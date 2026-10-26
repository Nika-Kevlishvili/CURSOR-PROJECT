import { zipSync, strToU8 } from 'fflate';
import { describe, expect, it } from 'vitest';
import { HttpError } from '../errors.js';
import { safeRelativePath, unzipSafely } from '../zip/safeZip.js';
import type { ArchiveLimits } from '../config.js';

const limits: ArchiveLimits = {
  maxArchiveBytes: 1024 * 1024,
  maxExpandedBytes: 1024 * 1024,
  maxFiles: 10,
  maxFileBytes: 1024 * 1024,
  maxProcessingMs: 5000,
};

describe('zip safety', () => {
  it('rejects parent traversal before extraction', () => {
    expect(() => safeRelativePath('../evil.txt')).toThrow(HttpError);
    expect(() => safeRelativePath('nested/../../evil.txt')).toThrow(HttpError);
    const archive = zipSync({ '../evil.txt': strToU8('no'), 'ok.txt': strToU8('yes') });
    expect(() => unzipSafely(archive, limits, Date.now())).toThrow(/traverses|not allowed|unsafe/i);
  });

  it('rejects a malformed archive', () => {
    expect(() => unzipSafely(new Uint8Array([1, 2, 3, 4, 5]), limits, Date.now())).toThrow(HttpError);
  });

  it('rejects archives over the size limit', () => {
    const archive = zipSync({ 'a.txt': strToU8('hello') });
    expect(() => unzipSafely(archive, { ...limits, maxArchiveBytes: 8 }, Date.now())).toThrow(/limit/i);
  });

  it('rejects too many files', () => {
    const archive = zipSync({
      'a.txt': strToU8('a'),
      'b.txt': strToU8('b'),
      'c.txt': strToU8('c'),
    });
    expect(() => unzipSafely(archive, { ...limits, maxFiles: 2 }, Date.now())).toThrow(/more than 2/);
  });

  it('rejects an expanded payload over the limit', () => {
    const archive = zipSync({ 'big.txt': strToU8('x'.repeat(5000)) });
    expect(() => unzipSafely(archive, { ...limits, maxExpandedBytes: 100 }, Date.now())).toThrow(/Expanded/);
  });

  it('rejects processing that exceeds the time limit', () => {
    const archive = zipSync({ 'a.txt': strToU8('a') });
    expect(() => unzipSafely(archive, { ...limits, maxProcessingMs: 0 }, Date.now() - 5)).toThrow(/time limit/i);
  });
});
