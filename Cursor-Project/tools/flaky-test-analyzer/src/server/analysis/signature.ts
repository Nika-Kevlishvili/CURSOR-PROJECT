import { createHash } from 'node:crypto';

const ANSI = /\u001b\[[0-9;]*m/g;

export function normalizeErrorSignature(message: string): string {
  return message
    .replace(ANSI, '')
    .replace(/eyJ[a-zA-Z0-9_-]{8,}\.[a-zA-Z0-9_-]{8,}\.[a-zA-Z0-9_-]{4,}/g, '<jwt>')
    .replace(/\b[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}\b/gi, '<uuid>')
    .replace(/\b\d{4}-\d{2}-\d{2}[T ]\d{2}:\d{2}:\d{2}(?:\.\d+)?(?:Z|[+-]\d{2}:?\d{2})?\b/g, '<timestamp>')
    .replace(/\b\d{13,}\b/g, '<id>')
    .replace(/https?:\/\/[^\s)'"]+/gi, '<url>')
    .replace(/\s+/g, ' ')
    .trim()
    .toLowerCase()
    .slice(0, 500);
}

export function testIdentity(parts: {
  filePath: string | null;
  projectName: string | null;
  fullTitle: string;
  repeatEachIndex: number | null;
}): string {
  const raw = [
    parts.filePath ?? '<unknown-file>',
    parts.projectName ?? '<unknown-project>',
    parts.fullTitle,
    parts.repeatEachIndex === null ? '' : `repeat=${parts.repeatEachIndex}`,
  ].join('\n');
  return createHash('sha256').update(raw).digest('hex').slice(0, 24);
}

export function sha256(value: string | Uint8Array): string {
  const hash = createHash('sha256');
  if (typeof value === 'string') hash.update(value);
  else hash.update(value);
  return hash.digest('hex');
}

export function extractSelector(text: string): string | null {
  const match = text.match(/(?:locator|getBy(?:Role|Text|Label|Placeholder|TestId|AltText|Title))\((?:[^)(]|\([^)]*\)){0,4}\)/);
  return match ? match[0].slice(0, 180) : null;
}

export function extractEndpoint(text: string): string | null {
  const url = text.match(/https?:\/\/[^\s)'"]+/i);
  if (url) return url[0].slice(0, 200);
  const verb = text.match(/\b(?:GET|POST|PUT|PATCH|DELETE)\s+(\/\S+)/);
  if (verb?.[1]) return verb[1].slice(0, 200);
  const path = text.match(/["'](\/(?:api|backend)\/[^"']+)["']/);
  return path?.[1]?.slice(0, 200) ?? null;
}
