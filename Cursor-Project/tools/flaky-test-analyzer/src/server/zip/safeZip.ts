import path from 'node:path';
import { Unzip, UnzipInflate } from 'fflate';
import { HttpError } from '../errors.js';
import type { ArchiveLimits } from '../config.js';

export type ArchiveFile = {
  path: string;
  bytes: Uint8Array;
};

export function assertProcessingTime(startedAt: number, limits: ArchiveLimits): void {
  if (Date.now() - startedAt > limits.maxProcessingMs) {
    throw new HttpError(408, 'Report processing exceeded the configured time limit.', 'PROCESSING_TIMEOUT');
  }
}

export function safeRelativePath(name: string): string {
  if (!name || name.includes('\0') || name.includes(':')) {
    throw new HttpError(400, 'Archive entry name is not allowed.', 'UNSAFE_PATH');
  }
  const slash = name.replace(/\\/g, '/');
  if (slash.startsWith('/') || slash.startsWith('//')) {
    throw new HttpError(400, `Archive entry is absolute: ${slash}`, 'UNSAFE_PATH');
  }
  const parts = slash.split('/');
  if (parts.some((part) => part === '..')) {
    throw new HttpError(400, `Archive entry traverses outside the archive: ${slash}`, 'UNSAFE_PATH');
  }
  const normalized = parts.filter((part) => part !== '' && part !== '.').join('/');
  if (!normalized || normalized.startsWith('..')) {
    throw new HttpError(400, `Archive entry path is unsafe: ${slash}`, 'UNSAFE_PATH');
  }
  return normalized;
}

export function resolveInside(root: string, relativePath: string): string {
  const destination = path.resolve(root, ...relativePath.split('/'));
  const relative = path.relative(root, destination);
  if (relative.startsWith('..') || path.isAbsolute(relative)) {
    throw new HttpError(400, 'Extraction path escaped the working directory.', 'UNSAFE_PATH');
  }
  return destination;
}

function concat(chunks: Uint8Array[]): Uint8Array {
  const size = chunks.reduce((sum, chunk) => sum + chunk.length, 0);
  const out = new Uint8Array(size);
  let offset = 0;
  for (const chunk of chunks) {
    out.set(chunk, offset);
    offset += chunk.length;
  }
  return out;
}

export function unzipSafely(archive: Uint8Array, limits: ArchiveLimits, startedAt: number): ArchiveFile[] {
  if (archive.byteLength < 4 || archive[0] !== 0x50 || archive[1] !== 0x4b) {
    throw new HttpError(400, 'The ZIP archive is malformed and could not be read.', 'MALFORMED_ZIP');
  }
  if (archive.byteLength > limits.maxArchiveBytes) {
    throw new HttpError(
      413,
      `Archive is ${archive.byteLength} bytes, above the limit of ${limits.maxArchiveBytes}.`,
      'ARCHIVE_TOO_LARGE',
    );
  }
  assertProcessingTime(startedAt, limits);

  const files: ArchiveFile[] = [];
  let expanded = 0;
  let seen = 0;
  let failure: Error | null = null;
  const unzipper = new Unzip();
  unzipper.register(UnzipInflate);
  unzipper.onfile = (file) => {
    if (failure) {
      file.terminate();
      return;
    }
    if (file.name.endsWith('/')) {
      file.start();
      return;
    }
    let safeName: string;
    try {
      safeName = safeRelativePath(file.name);
    } catch (error) {
      failure = error instanceof Error ? error : new Error('Unsafe archive entry');
      file.terminate();
      return;
    }
    seen += 1;
    if (seen > limits.maxFiles) {
      failure = new HttpError(413, `Archive contains more than ${limits.maxFiles} files.`, 'TOO_MANY_FILES');
      file.terminate();
      return;
    }
    const chunks: Uint8Array[] = [];
    let fileSize = 0;
    file.ondata = (error, chunk, final) => {
      if (failure) return;
      if (error) {
        failure = new HttpError(400, 'The ZIP archive is malformed or uses an unsupported compression method.', 'MALFORMED_ZIP');
        file.terminate();
        return;
      }
      fileSize += chunk.byteLength;
      expanded += chunk.byteLength;
      if (fileSize > limits.maxFileBytes) {
        failure = new HttpError(413, `An archive entry exceeds the per-file limit (${file.name}).`, 'FILE_TOO_LARGE');
        file.terminate();
        return;
      }
      if (expanded > limits.maxExpandedBytes) {
        failure = new HttpError(413, `Expanded archive exceeds ${limits.maxExpandedBytes} bytes.`, 'EXPANDED_TOO_LARGE');
        file.terminate();
        return;
      }
      if (chunk.byteLength > 0) chunks.push(chunk);
      if (final) files.push({ path: safeName, bytes: concat(chunks) });
    };
    file.start();
  };

  try {
    unzipper.push(archive, true);
  } catch {
    throw new HttpError(400, 'The ZIP archive is malformed and could not be read.', 'MALFORMED_ZIP');
  }
  if (failure) throw failure;
  assertProcessingTime(startedAt, limits);
  return files;
}
