import { PolicyError } from '../errors.js';

function count(text: string, pattern: RegExp): number {
  return text.match(pattern)?.length ?? 0;
}

export function assertPatchPolicy(oldText: string, newText: string): void {
  if (oldText === newText) throw new PolicyError('The patch does not change the file.');
  if (count(newText, /expect\s*\(/g) < count(oldText, /expect\s*\(/g)) {
    throw new PolicyError('The patch removes an assertion.');
  }
  if (/\b(?:test|describe)\.(?:skip|fixme)\b/.test(newText) && !/\b(?:test|describe)\.(?:skip|fixme)\b/.test(oldText)) {
    throw new PolicyError('The patch skips or disables a test.');
  }
  if (/waitForTimeout\s*\(/.test(newText) && count(newText, /waitForTimeout\s*\(/g) > count(oldText, /waitForTimeout\s*\(/g)) {
    throw new PolicyError('The patch adds an arbitrary sleep.');
  }
  const oldRetries = Number(oldText.match(/retries\s*:\s*(\d+)/)?.[1] ?? 0);
  const newRetries = Number(newText.match(/retries\s*:\s*(\d+)/)?.[1] ?? 0);
  if (newRetries > oldRetries) throw new PolicyError('The patch adds retries.');
  const oldTimeout = Number(oldText.match(/timeout\s*:\s*(\d+)/)?.[1] ?? 0);
  const newTimeout = Number(newText.match(/timeout\s*:\s*(\d+)/)?.[1] ?? 0);
  if (newTimeout > oldTimeout) throw new PolicyError('The patch increases a timeout without a separate evidence review.');
  if (/\.catch\(\s*(?:\(\s*\)\s*=>)?\s*\{\s*\}\s*\)/.test(newText) && !/\.catch\(\s*(?:\(\s*\)\s*=>)?\s*\{\s*\}\s*\)/.test(oldText)) {
    throw new PolicyError('The patch swallows an error.');
  }
}
