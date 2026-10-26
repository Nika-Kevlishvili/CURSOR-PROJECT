import { execFileSync } from 'node:child_process';
import fs from 'node:fs';
import path from 'node:path';
import { createTwoFilesPatch } from 'diff';
import { HttpError } from '../errors.js';
import { assertPatchPolicy } from './policy.js';
import { isPatchablePath, toRepoRelative } from '../analysis/sourceContext.js';
import { sha256 } from '../analysis/signature.js';
import type { PatchFile } from '../analysis/propose.js';

export type FileIo = {
  read(absolute: string): string;
  write(absolute: string, content: string): void;
  afterWrite?: (absolute: string) => void;
};

const fsIo: FileIo = {
  read: (absolute) => fs.readFileSync(absolute, 'utf8'),
  write: (absolute, content) => fs.writeFileSync(absolute, content, 'utf8'),
};

export function replaceOnce(content: string, oldText: string, newText: string): string {
  assertPatchPolicy(oldText, newText);
  const first = content.indexOf(oldText);
  if (first < 0) {
    throw new HttpError(409, 'The source no longer contains the approved code. The patch is stale.', 'STALE_PATCH');
  }
  if (content.indexOf(oldText, first + oldText.length) >= 0) {
    throw new HttpError(409, 'The approved snippet appears more than once. The patch was not applied.', 'AMBIGUOUS_PATCH');
  }
  return content.slice(0, first) + newText + content.slice(first + oldText.length);
}

export function gitPorcelain(automationRoot: string, relativePath: string): string {
  try {
    return execFileSync('git', ['status', '--porcelain', '--', relativePath], {
      cwd: automationRoot,
      encoding: 'utf8',
      stdio: ['ignore', 'pipe', 'pipe'],
    }).trim();
  } catch {
    throw new HttpError(409, `Git status failed for ${relativePath}. The patch was not applied.`, 'GIT_STATUS_FAILED');
  }
}

export function assertCurrentSource(automationRoot: string, file: PatchFile): { absolute: string; content: string } {
  const relative = toRepoRelative(automationRoot, file.path);
  if (!relative || relative !== file.path.replace(/\\/g, '/') || !isPatchablePath(relative)) {
    throw new HttpError(403, `Refusing to modify ${file.path}. Approved patches can only change tests, fixtures, or helpers.`, 'PATH_FORBIDDEN');
  }
  const absolute = path.resolve(automationRoot, ...relative.split('/'));
  if (!fs.existsSync(absolute)) {
    throw new HttpError(409, `${relative} no longer exists. The patch is stale.`, 'STALE_PATCH');
  }
  const content = fs.readFileSync(absolute, 'utf8');
  if (sha256(content) !== file.baseHash) {
    throw new HttpError(409, `${relative} changed after the diff was created. Approve a new diff before applying it.`, 'STALE_PATCH');
  }
  gitPorcelain(automationRoot, relative);
  return { absolute, content };
}

export function applyTransaction(entries: { absolute: string; next: string }[], io: FileIo = fsIo): void {
  const backups = entries.map((entry) => ({ absolute: entry.absolute, content: io.read(entry.absolute) }));
  try {
    for (const entry of entries) {
      io.write(entry.absolute, entry.next);
      io.afterWrite?.(entry.absolute);
    }
  } catch (error) {
    for (const backup of backups) {
      try {
        io.write(backup.absolute, backup.content);
      } catch {
        // Preserve the original apply error.
      }
    }
    throw error;
  }
}

export function resultingDiff(relativePath: string, before: string, after: string): string {
  return createTwoFilesPatch(relativePath, relativePath, before, after, '', '', { context: 3 });
}
