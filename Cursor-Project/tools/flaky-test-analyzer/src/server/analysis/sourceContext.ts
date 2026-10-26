import fs from 'node:fs';
import path from 'node:path';
import { clip } from '../redact.js';

const ALLOWED_PREFIXES = ['tests/', 'fixtures/', 'utils/'];

export function toRepoRelative(automationRoot: string, filePath: string | null): string | null {
  if (!filePath) return null;
  const posix = filePath.replace(/\\/g, '/').replace(/^\.\//, '');
  if (posix.includes('\0') || posix.split('/').includes('..')) return null;
  const absolute = path.isAbsolute(filePath) ? filePath : path.resolve(automationRoot, ...posix.split('/'));
  const relative = path.relative(automationRoot, absolute).replace(/\\/g, '/');
  if (!relative || relative.startsWith('..') || path.isAbsolute(relative)) return null;
  return relative;
}

export function candidateSourcePaths(filePath: string | null, stack: string | null): string[] {
  const found: string[] = [];
  const add = (value: string | null | undefined) => {
    if (!value) return;
    const posix = value.replace(/\\/g, '/').replace(/^\.\//, '');
    if (!posix || posix.split('/').includes('..')) return;
    found.push(posix);
    if (posix.startsWith('src/')) found.push(posix.slice(4));
    if (!posix.startsWith('tests/') && !posix.startsWith('src/')) found.push(`tests/${posix}`);
  };
  add(filePath);
  for (const match of stack?.matchAll(/((?:src\/)?tests\/[^\s:'"]+\.(?:spec|test)\.ts)/g) ?? []) {
    add(match[1]);
  }
  return [...new Set(found)];
}

export function resolveExistingSource(automationRoot: string, filePath: string | null, stack: string | null): string | null {
  for (const candidate of candidateSourcePaths(filePath, stack)) {
    const relative = toRepoRelative(automationRoot, candidate);
    if (!relative) continue;
    const absolute = path.resolve(automationRoot, ...relative.split('/'));
    if (fs.existsSync(absolute) && fs.statSync(absolute).isFile()) return relative;
  }
  return toRepoRelative(automationRoot, filePath);
}

export function lineForFailure(source: string | null, reportedLine: number | null, errorText: string): number | null {
  if (!source) return reportedLine;
  const lines = source.split(/\r?\n/);
  const title = errorText.match(/\[REG-\d+\]:[^'\n]+/)?.[0]?.trim();
  if (title) {
    const titleIndex = lines.findIndex((line) => line.includes(title));
    if (titleIndex >= 0) {
      const window = lines.slice(titleIndex, Math.min(lines.length, titleIndex + 30));
      const assertion = window.findIndex((line) => /expect\s*\(/.test(line));
      if (assertion >= 0) return titleIndex + assertion + 1;
      return titleIndex + 1;
    }
  }
  return reportedLine;
}

export function isPatchablePath(relativePath: string): boolean {
  const posix = relativePath.replace(/\\/g, '/');
  if (!posix.endsWith('.ts') && !posix.endsWith('.tsx')) return false;
  if (posix.split('/').includes('..') || posix.includes(':')) return false;
  if (posix.includes('.env') || posix.endsWith('token.json')) return false;
  return ALLOWED_PREFIXES.some((prefix) => posix.startsWith(prefix));
}

export function readSource(automationRoot: string, relativePath: string | null): {
  relativePath: string | null;
  text: string | null;
  missingReason: string | null;
} {
  if (!relativePath) {
    return { relativePath: null, text: null, missingReason: 'The report has no test file path.' };
  }
  if (!isPatchablePath(relativePath) && !relativePath.endsWith('.ts') && !relativePath.endsWith('.tsx')) {
    return { relativePath, text: null, missingReason: `${relativePath} is outside the test, fixture, and helper sources that this tool reads.` };
  }
  const absolute = path.resolve(automationRoot, ...relativePath.split('/'));
  const check = path.relative(automationRoot, absolute);
  if (check.startsWith('..') || path.isAbsolute(check)) {
    return { relativePath, text: null, missingReason: 'The file path points outside the automation repository.' };
  }
  if (!fs.existsSync(absolute) || !fs.statSync(absolute).isFile()) {
    return {
      relativePath,
      text: null,
      missingReason: `${relativePath} was named by the report but was not found under the automation root. Search tests for the full title.`,
    };
  }
  return { relativePath, text: fs.readFileSync(absolute, 'utf8'), missingReason: null };
}

export function excerptAround(text: string, line: number | null, radius = 8): string | null {
  if (!line || line < 1) return null;
  const lines = text.split(/\r?\n/);
  const start = Math.max(0, line - 1 - radius);
  const end = Math.min(lines.length, line - 1 + radius + 1);
  return lines.slice(start, end).map((row, index) => `${start + index + 1}: ${row}`).join('\n');
}

export function lineFromStack(stack: string | null, filePath: string | null): number | null {
  if (!stack || !filePath) return null;
  const base = filePath.split('/').pop();
  if (!base) return null;
  const escaped = base.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
  const match = stack.match(new RegExp(`${escaped}:(\\d+):\\d+`));
  return match?.[1] ? Number(match[1]) : null;
}

export function safeExcerpt(text: string | null, line: number | null): string | null {
  return clip(excerptAround(text ?? '', line), 4000);
}
