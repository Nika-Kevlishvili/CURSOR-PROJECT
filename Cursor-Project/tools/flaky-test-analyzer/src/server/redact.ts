const ANSI = /\u001b\[[0-9;]*m/g;

export function redact(text: string | null | undefined): string | null {
  if (text == null) return null;
  return text
    .replace(ANSI, '')
    .replace(/eyJ[a-zA-Z0-9_-]{8,}\.[a-zA-Z0-9_-]{8,}\.[a-zA-Z0-9_-]{4,}/g, '[redacted-jwt]')
    .replace(/bearer\s+[a-z0-9\-._~+/]+=*/gi, 'bearer [redacted]')
    .replace(/(password|passwd|secret|token|jwt|authorization)\s*[:=]\s*["']?[^"'\s,}]+/gi, '$1=[redacted]')
    .replace(/[A-Za-z]:\\Users\\[^\\\s]+/g, 'C:\\\\Users\\\\[user]')
    .replace(/\/Users\/[^/\s]+/g, '/Users/[user]');
}

export function clip(text: string | null | undefined, max: number): string | null {
  const clean = redact(text);
  if (clean == null) return null;
  if (clean.length <= max) return clean;
  return `${clean.slice(0, max)}\n…[truncated]`;
}
