#!/usr/bin/env node
/**
 * extract-failures.mjs
 *
 * Reads a Playwright "merged-reports" archive (the zip the Playwright Slack app
 * posts to the regression channel) and lists every failed case per environment,
 * with the full error text, the failing or hung step, and how the same case did
 * on the other environments.
 *
 * Usage:
 *   node extract-failures.mjs [<merged-reports.zip | merged-reports folder>]
 *        [--envs=dev,dev2,test] [--out=<file.json>] [--registry=<known-bugs.json>]
 *        [--extract-to=<empty dir>] [--max-error-chars=6000]
 *
 * With no input path, the newest "merged-reports*" zip or folder in ~/Downloads
 * is used and its path is printed.
 *
 * No dependencies. The archive is treated as untrusted data: entries are written
 * only inside the extraction folder, and nothing from the archive is executed.
 */

import {
  closeSync, existsSync, mkdirSync, mkdtempSync, openSync, readFileSync,
  readSync, readdirSync, statSync, writeFileSync,
} from 'node:fs';
import { homedir, tmpdir } from 'node:os';
import { basename, dirname, join, resolve, sep } from 'node:path';
import { inflateRawSync } from 'node:zlib';

const DEFAULT_ENVS = ['dev', 'dev2', 'test'];

// ── Arguments ────────────────────────────────────────────────────────────────

function fail(message) {
  console.error(`extract-failures: ${message}`);
  process.exit(1);
}

function parseArgs(argv) {
  const args = { input: null, envs: DEFAULT_ENVS, out: null, registry: null, extractTo: null, maxErrorChars: 6000 };
  for (const a of argv) {
    if (a === '--help' || a === '-h') {
      console.log(readFileSync(new URL(import.meta.url), 'utf8').split('*/')[0].replace(/^#!.*\n\/\*\*\n?/, '').replace(/^ \* ?/gm, ''));
      process.exit(0);
    } else if (a.startsWith('--envs=')) {
      args.envs = a.slice(7).split(',').map((s) => s.trim()).filter(Boolean);
    } else if (a.startsWith('--out=')) {
      args.out = a.slice(6);
    } else if (a.startsWith('--registry=')) {
      args.registry = a.slice(11);
    } else if (a.startsWith('--extract-to=')) {
      args.extractTo = a.slice(13);
    } else if (a.startsWith('--max-error-chars=')) {
      args.maxErrorChars = Number(a.slice(18)) || 6000;
    } else if (a.startsWith('--')) {
      fail(`unknown option ${a}`);
    } else if (!args.input) {
      args.input = a;
    } else {
      fail(`unexpected argument ${a}`);
    }
  }
  return args;
}

// ── Zip reading (stored + deflate, no zip64) ─────────────────────────────────

function zipEntries(read, size) {
  const tailLen = Math.min(size, 65557);
  const tail = read(size - tailLen, tailLen);
  let eocd = -1;
  for (let i = tail.length - 22; i >= 0; i--) {
    if (tail.readUInt32LE(i) === 0x06054b50) { eocd = i; break; }
  }
  if (eocd < 0) throw new Error('not a zip archive (end record not found)');
  const count = tail.readUInt16LE(eocd + 10);
  const cdSize = tail.readUInt32LE(eocd + 12);
  const cdOffset = tail.readUInt32LE(eocd + 16);
  if (count === 0xffff || cdSize === 0xffffffff || cdOffset === 0xffffffff) {
    throw new Error('zip64 archives are not supported; unzip it yourself and pass the folder');
  }
  const cd = read(cdOffset, cdSize);
  const entries = [];
  let p = 0;
  for (let i = 0; i < count; i++) {
    if (cd.readUInt32LE(p) !== 0x02014b50) throw new Error('corrupt zip central directory');
    const method = cd.readUInt16LE(p + 10);
    const compressedSize = cd.readUInt32LE(p + 20);
    const nameLen = cd.readUInt16LE(p + 28);
    const extraLen = cd.readUInt16LE(p + 30);
    const commentLen = cd.readUInt16LE(p + 32);
    const externalAttrs = cd.readUInt32LE(p + 38);
    const localOffset = cd.readUInt32LE(p + 42);
    const name = cd.subarray(p + 46, p + 46 + nameLen).toString('utf8');
    entries.push({ name, method, compressedSize, externalAttrs, localOffset });
    p += 46 + nameLen + extraLen + commentLen;
  }
  return entries;
}

function zipEntryData(read, entry) {
  const local = read(entry.localOffset, 30);
  if (local.readUInt32LE(0) !== 0x04034b50) throw new Error(`corrupt zip entry ${entry.name}`);
  const dataStart = entry.localOffset + 30 + local.readUInt16LE(26) + local.readUInt16LE(28);
  const raw = read(dataStart, entry.compressedSize);
  if (entry.method === 0) return raw;
  if (entry.method === 8) return inflateRawSync(raw);
  throw new Error(`unsupported zip compression ${entry.method} for ${entry.name}`);
}

function bufferReader(buffer) {
  return (offset, length) => buffer.subarray(offset, offset + length);
}

/** Extracts a zip into destDir. Entries that would land outside destDir are skipped. */
function extractZipFile(zipPath, destDir) {
  const fd = openSync(zipPath, 'r');
  const size = statSync(zipPath).size;
  const read = (offset, length) => {
    const buf = Buffer.alloc(length);
    readSync(fd, buf, 0, length, offset);
    return buf;
  };
  const root = resolve(destDir);
  const skipped = [];
  let written = 0;
  try {
    for (const entry of zipEntries(read, size)) {
      // Windows-built archives use backslashes in entry names.
      const name = entry.name.replace(/\\/g, '/');
      const isSymlink = ((entry.externalAttrs >>> 16) & 0o170000) === 0o120000;
      const target = resolve(root, name);
      const unsafe = name.startsWith('/') || /^[a-zA-Z]:/.test(name) || name.split('/').includes('..')
        || (target !== root && !target.startsWith(root + sep));
      if (unsafe || isSymlink) { skipped.push(entry.name); continue; }
      if (name.endsWith('/')) { mkdirSync(target, { recursive: true }); continue; }
      mkdirSync(dirname(target), { recursive: true });
      writeFileSync(target, zipEntryData(read, entry));
      written++;
    }
  } finally {
    closeSync(fd);
  }
  return { written, skipped };
}

// ── Locating the report ──────────────────────────────────────────────────────

function isEnvDir(dir) {
  return existsSync(join(dir, 'index.html'));
}

function hasEnvDirs(dir) {
  try {
    return readdirSync(dir, { withFileTypes: true }).some((e) => e.isDirectory() && isEnvDir(join(dir, e.name)));
  } catch {
    return false;
  }
}

function newestInDownloads() {
  const dir = join(homedir(), 'Downloads');
  if (!existsSync(dir)) return null;
  const candidates = [];
  for (const e of readdirSync(dir, { withFileTypes: true })) {
    if (!/^merged-reports/i.test(e.name)) continue;
    const full = join(dir, e.name);
    const isZip = e.isFile() && /\.zip$/i.test(e.name);
    const isReportDir = e.isDirectory() && hasEnvDirs(full);
    if (isZip || isReportDir) candidates.push({ path: full, mtime: statSync(full).mtimeMs, isZip });
  }
  if (candidates.length === 0) return null;
  // Newest wins; on a tie the zip wins because the folder may be an older unpack.
  candidates.sort((a, b) => b.mtime - a.mtime || Number(b.isZip) - Number(a.isZip));
  const zips = candidates.filter((c) => c.isZip);
  return { chosen: (zips[0] ?? candidates[0]).path, candidates: candidates.map((c) => c.path) };
}

function resolveRoot(args) {
  let input = args.input;
  let note = null;
  if (!input) {
    const found = newestInDownloads();
    if (!found) fail('no path given and no merged-reports zip or folder found in ~/Downloads');
    input = found.chosen;
    note = `No path given. Using the newest zip in ~/Downloads: ${input}`;
    if (found.candidates.length > 1) note += ` (other candidates: ${found.candidates.filter((c) => c !== input).join(', ')})`;
  }
  const abs = resolve(input);
  if (!existsSync(abs)) fail(`path not found: ${abs}`);
  const st = statSync(abs);
  if (st.isDirectory()) {
    if (!hasEnvDirs(abs)) fail(`${abs} has no <env>/index.html folders`);
    return { source: abs, root: abs, note, extraction: null };
  }
  if (!/\.zip$/i.test(abs)) fail(`${abs} is neither a folder nor a .zip`);
  let dest;
  if (args.extractTo) {
    dest = resolve(args.extractTo);
    mkdirSync(dest, { recursive: true });
    if (readdirSync(dest).length > 0) fail(`--extract-to folder is not empty: ${dest}`);
  } else {
    dest = mkdtempSync(join(tmpdir(), 'merged-reports-'));
  }
  const extraction = extractZipFile(abs, dest);
  let root = dest;
  if (!hasEnvDirs(root)) {
    const kids = readdirSync(dest, { withFileTypes: true }).filter((e) => e.isDirectory());
    const inner = kids.find((k) => hasEnvDirs(join(dest, k.name)));
    if (!inner) fail(`${abs} has no <env>/index.html folders after extraction`);
    root = join(dest, inner.name);
  }
  return { source: abs, root, note, extraction };
}

// ── Report parsing ───────────────────────────────────────────────────────────

const ANSI = /\u001b\[[0-9;]*m/g;

function clean(text) {
  return String(text ?? '').replace(ANSI, '').replace(/\r\n/g, '\n');
}

function errorText(e) {
  if (typeof e === 'string') return clean(e);
  if (!e) return '';
  return clean([e.message, e.codeframe ?? e.snippet, e.stack && !String(e.stack).includes(String(e.message)) ? e.stack : ''].filter(Boolean).join('\n\n'));
}

function mask(text) {
  return String(text)
    .replace(/[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}/gi, '<uuid>')
    .replace(/\d{4}-\d{2}-\d{2}(T[\d:.]+Z?)?/g, '<date>')
    .replace(/\b\d+(\.\d+)?\b/g, '<n>')
    // Spec files differ in dash encoding; keep one signature for the same message.
    .replace(/[\u2010-\u2015\ufffd]/g, '-')
    .replace(/\s+/g, ' ')
    .trim();
}

/**
 * Failed `expect(response).CheckResponse()` calls carry the request and the
 * response in the error text. Returns null for any other error.
 */
function apiFailureOf(text) {
  const t = clean(text);
  const endpoint = t.match(/^\s*Endpoint: (.+)$/m)?.[1]?.trim();
  const status = t.match(/^\s*Status: (\d{3})\s*$/m)?.[1];
  if (!endpoint || !status) return null;
  const method = t.match(/^\s*Method: (.+)$/m)?.[1]?.trim() ?? null;
  const between = (from, to) => {
    const a = t.indexOf(from);
    if (a < 0) return null;
    const b = to ? t.indexOf(to, a) : -1;
    return t.slice(a + from.length, b < 0 ? undefined : b).trim();
  };
  const payload = between('Payload:', '\n        Status:');
  // The response ends where the code frame ("  207 | ...") or the stack starts.
  const responseRaw = (between('Response:', null) ?? '').split(/\n\n\s*\d+ \|/)[0].split(/\n\s+at /)[0].trim();
  let message = null;
  try {
    const parsed = JSON.parse(responseRaw);
    message = typeof parsed === 'string' ? parsed : parsed?.message ?? parsed?.error ?? null;
  } catch { /* not JSON */ }
  let path = endpoint;
  try { path = new URL(endpoint).pathname; } catch { /* relative */ }
  return {
    method,
    endpoint,
    path,
    status: Number(status),
    responseMessage: message ?? responseRaw.slice(0, 300),
    payload: payload && payload.length > 3000 ? `${payload.slice(0, 3000)}\n… (truncated)` : payload,
    response: responseRaw.length > 3000 ? `${responseRaw.slice(0, 3000)}\n… (truncated)` : responseRaw,
  };
}

/** One line that stays the same between runs: ids, numbers, dates and hosts are masked. */
function signatureOf(text, api) {
  if (api) {
    // Drop the per-environment prefix (/backend/phoenix1-dev/) so one bug groups across environments.
    const route = api.path.replace(/^\/backend\/[^/]+\//, '').replace(/^\//, '');
    // Validation responses list several messages in no fixed order.
    const message = String(api.responseMessage).split('\n')[0].split(';').map((m) => m.trim()).filter(Boolean).sort().join('; ');
    return `API ${api.method} ${mask(route)} -> ${api.status}: ${mask(message)}`.slice(0, 240);
  }
  const lines = clean(text).split('\n').map((l) => l.trim()).filter(Boolean);
  if (lines.length === 0) return '(no error text)';
  const head = [lines[0]];
  if (/^\w*Error:$/.test(lines[0]) && lines[1]) head.push(lines[1]);
  const matcher = lines.find((l) => /^expect\(/.test(l));
  if (matcher && !head.includes(matcher)) head.push(matcher);
  return mask(head.join(' | ')).slice(0, 240);
}

/** First stack frame inside the EnergoTS repository, as a repo-relative path. */
function errorLocationOf(text) {
  for (const m of clean(text).matchAll(/^\s+at (?:.*?\()?(.+?):(\d+):(\d+)\)?$/gm)) {
    const file = m[1].replace(/\\/g, '/');
    const i = file.lastIndexOf('/src/');
    if (i >= 0 && !file.includes('node_modules')) return `${file.slice(i + 1)}:${m[2]}`;
  }
  return null;
}

function regIdOf(title) {
  const m = String(title ?? '').match(/\[([A-Z][A-Z0-9]+-\d+)\]/);
  return m ? m[1] : null;
}

function describeSteps(steps) {
  const top = [];
  let hung = null;
  let failed = null;
  const walk = (list, trail, depth) => {
    for (const s of list ?? []) {
      const here = [...trail, s.title];
      if (depth === 0 && !/^(Before Hooks|After Hooks|Worker Cleanup|attach ")/.test(s.title)) {
        top.push({ title: s.title, durationMs: s.duration, ...(s.duration === -1 ? { unfinished: true } : {}), ...(s.error ? { error: true } : {}) });
      }
      if (s.duration === -1) hung = here;
      if (s.error) failed = here;
      walk(s.steps, here, depth + 1);
    }
  };
  walk(steps, [], 0);
  return { top, hungStep: hung ? hung.join(' > ') : null, failedStep: failed ? failed.join(' > ') : null };
}

function embeddedReport(envDir) {
  const html = readFileSync(join(envDir, 'index.html'), 'utf8');
  // The report data is a base64 zip. Older Playwright assigns it to window.playwrightReportBase64,
  // newer versions put it in a <script> element; both use the same data: prefix.
  const marker = 'data:application/zip;base64,';
  const start = html.lastIndexOf(marker);
  if (start < 0) throw new Error('index.html has no embedded Playwright report data');
  const from = start + marker.length;
  let end = from;
  while (end < html.length && /[A-Za-z0-9+/=]/.test(html[end])) end++;
  const zip = Buffer.from(html.slice(from, end), 'base64');
  const read = bufferReader(zip);
  const byName = new Map(zipEntries(read, zip.length).map((e) => [e.name, e]));
  const json = (name) => (byName.has(name) ? JSON.parse(zipEntryData(read, byName.get(name)).toString('utf8')) : null);
  return { json };
}

function annotationsOf(test) {
  return (test.annotations ?? []).map((a) => ({ type: a.type, ...(a.description ? { description: a.description } : {}) }));
}

function parseEnv(envName, envDir, maxErrorChars) {
  const { json } = embeddedReport(envDir);
  const report = json('report.json');
  if (!report) throw new Error('embedded report.json is missing');
  const failures = [];
  const knownBugs = [];
  const files = {};
  for (const file of report.files ?? []) {
    const detail = json(`${file.fileId}.json`);
    const detailById = new Map((detail?.tests ?? []).map((t) => [t.testId, t]));
    const fileName = file.fileName ?? file.tests?.[0]?.location?.file ?? file.fileId;
    const rows = [];
    for (const summary of file.tests ?? []) {
      const test = detailById.get(summary.testId) ?? summary;
      const result = (test.results ?? [])[test.results.length - 1] ?? {};
      const annotations = annotationsOf(test);
      const types = annotations.map((a) => a.type);
      const base = {
        env: envName,
        regId: regIdOf(test.title),
        title: test.title,
        suitePath: (test.path ?? []).map((s) => String(s).trim()),
        file: test.location?.file ?? fileName,
        line: test.location?.line ?? null,
        project: test.projectName ?? null,
        tags: test.tags ?? [],
        annotations,
        outcome: test.outcome,
        status: result.status ?? null,
        durationMs: test.duration ?? null,
        startTime: result.startTime ?? null,
      };
      rows.push({ line: base.line, title: base.title, regId: base.regId, outcome: base.outcome, status: base.status, durationMs: base.durationMs });
      if (test.outcome === 'expected' && types.includes('known-bug')) {
        knownBugs.push({ ...base, knownBug: annotations.find((a) => a.type === 'known-bug')?.description ?? null });
        continue;
      }
      if (test.outcome !== 'unexpected' && test.outcome !== 'flaky') continue;
      const errors = (result.errors ?? []).map(errorText).filter(Boolean);
      const fullError = errors.join('\n\n────────\n\n');
      const steps = describeSteps(result.steps);
      const attachments = (result.attachments ?? []).map((a) => {
        const out = { name: a.name, contentType: a.contentType };
        if (a.path) {
          const abs = join(envDir, a.path);
          out.path = existsSync(abs) ? abs : a.path;
        }
        if (typeof a.body === 'string') out.body = clean(a.body).slice(0, 4000);
        return out;
      });
      const api = apiFailureOf(errors[0] ?? '');
      failures.push({
        ...base,
        signature: signatureOf(errors[0] ?? (result.status === 'timedOut' ? 'Test timeout' : ''), api),
        errorLocation: errorLocationOf(errors[0] ?? ''),
        api,
        error: fullError.length > maxErrorChars ? `${fullError.slice(0, maxErrorChars)}\n… (truncated, ${fullError.length} chars)` : fullError,
        errorCount: errors.length,
        failedStep: steps.failedStep,
        hungStep: steps.hungStep,
        steps: steps.top,
        tracePath: attachments.find((a) => a.name === 'trace')?.path ?? null,
        attachments: attachments.filter((a) => a.name !== 'trace'),
        markState: types.includes('known-bug-fixed') ? 'fixed' : types.includes('known-bug-drift') ? 'drift' : null,
      });
    }
    files[fileName] = rows.sort((a, b) => (a.line ?? 0) - (b.line ?? 0));
  }
  const startMs = typeof report.startTime === 'number' ? report.startTime : Date.parse(report.startTime);
  return {
    name: envName,
    path: envDir,
    startTime: Number.isFinite(startMs) ? new Date(startMs).toISOString() : null,
    ageDays: Number.isFinite(startMs) ? Math.floor((Date.now() - startMs) / 86400000) : null,
    durationMinutes: report.duration ? Math.round(report.duration / 60000) : null,
    stats: report.stats ?? null,
    failures,
    knownBugs,
    // Every case of each file that has a failure, in source order: shows prep steps and cascades.
    filesWithFailures: Object.fromEntries(Object.entries(files).filter(([, rows]) => rows.some((r) => r.outcome === 'unexpected' || r.outcome === 'flaky'))),
  };
}

// ── Cross-environment views ──────────────────────────────────────────────────

function caseKey(t) {
  return t.regId ?? `${t.file}::${t.title}`;
}

function buildCrossEnv(environments) {
  const outcomeByEnv = {};
  for (const env of environments) {
    outcomeByEnv[env.name] = new Map();
    for (const rows of Object.values(env.allCases)) {
      for (const r of rows) outcomeByEnv[env.name].set(r.regId ?? `${r.file}::${r.title}`, r.outcome);
    }
  }
  const cases = new Map();
  for (const env of environments) {
    for (const f of env.failures) {
      const key = caseKey(f);
      if (!cases.has(key)) cases.set(key, { key, regId: f.regId, title: f.title, file: f.file, line: f.line, envs: {} });
      cases.get(key).envs[env.name] = { status: f.status, signature: f.signature };
    }
  }
  for (const c of cases.values()) {
    for (const env of environments) {
      if (!c.envs[env.name]) c.envs[env.name] = { status: outcomeByEnv[env.name].get(c.key) === 'expected' ? 'passed' : outcomeByEnv[env.name].get(c.key) ?? 'not in this run' };
    }
    const failed = Object.values(c.envs).filter((e) => e.signature);
    c.failedOn = Object.entries(c.envs).filter(([, e]) => e.signature).map(([n]) => n);
    c.sameErrorEverywhere = failed.length > 1 && failed.every((e) => e.signature === failed[0].signature);
  }
  return [...cases.values()].sort((a, b) => b.failedOn.length - a.failedOn.length || String(a.file).localeCompare(String(b.file)));
}

function buildSignatureGroups(environments) {
  const groups = new Map();
  for (const env of environments) {
    for (const f of env.failures) {
      if (!groups.has(f.signature)) groups.set(f.signature, { signature: f.signature, count: 0, envs: new Set(), cases: [] });
      const g = groups.get(f.signature);
      g.count++;
      g.envs.add(env.name);
      g.cases.push({ env: env.name, regId: f.regId, title: f.title, file: f.file, line: f.line });
    }
  }
  return [...groups.values()].map((g) => ({ ...g, envs: [...g.envs] })).sort((a, b) => b.count - a.count);
}

function applyRegistry(environments, registryPath) {
  if (!registryPath) return null;
  const abs = resolve(registryPath);
  if (!existsSync(abs)) return { path: abs, found: false, entries: 0 };
  const data = JSON.parse(readFileSync(abs, 'utf8'));
  const entries = Array.isArray(data) ? data : data.entries ?? [];
  const byCase = new Map(entries.map((e) => [e.case, e]));
  for (const env of environments) {
    for (const f of env.failures) {
      const entry = byCase.get(f.regId);
      if (!entry) continue;
      const inEnv = (entry.envs ?? []).includes(env.name);
      f.registry = { jira: entry.jira ?? null, envs: entry.envs ?? [], state: f.markState ?? (inEnv ? 'marked but still red' : 'marked for other environments only') };
    }
  }
  return { path: abs, found: true, entries: entries.length };
}

// ── Main ─────────────────────────────────────────────────────────────────────

function main() {
  const args = parseArgs(process.argv.slice(2));
  const { source, root, note, extraction } = resolveRoot(args);

  const present = readdirSync(root, { withFileTypes: true }).filter((e) => e.isDirectory() && isEnvDir(join(root, e.name))).map((e) => e.name);
  const environments = [];
  const problems = [];
  for (const name of args.envs) {
    if (!present.includes(name)) { problems.push(`${name}: no ${name}/index.html in the archive`); continue; }
    try {
      const env = parseEnv(name, join(root, name), args.maxErrorChars);
      environments.push(env);
    } catch (e) {
      problems.push(`${name}: ${e.message}`);
    }
  }
  // allCases is only needed to look a case up on another environment.
  for (const env of environments) {
    const { json } = embeddedReport(env.path);
    const report = json('report.json');
    env.allCases = {};
    for (const file of report.files ?? []) {
      env.allCases[file.fileName ?? file.fileId] = (file.tests ?? []).map((t) => ({
        regId: regIdOf(t.title),
        title: t.title,
        file: t.location?.file ?? file.fileName,
        // A known-bug case is an expected failure; it must not read as a pass on that environment.
        outcome: t.outcome === 'expected' && (t.annotations ?? []).some((a) => a.type === 'known-bug') ? 'known-bug' : t.outcome,
      }));
    }
  }
  const crossEnv = buildCrossEnv(environments);
  const signatureGroups = buildSignatureGroups(environments);
  const registry = applyRegistry(environments, args.registry);
  for (const env of environments) delete env.allCases;

  let statsJson = null;
  try { statsJson = JSON.parse(readFileSync(join(root, 'stats.json'), 'utf8')); } catch { /* optional */ }

  const result = {
    source,
    root,
    generatedAt: new Date().toISOString(),
    environmentsRequested: args.envs,
    environmentsInArchive: present,
    problems,
    extraction: extraction ? { filesWritten: extraction.written, entriesSkippedAsUnsafe: extraction.skipped } : null,
    statsJson,
    registry,
    environments,
    crossEnv,
    signatureGroups,
  };

  const outPath = resolve(args.out ?? join(root === source ? mkdtempSync(join(tmpdir(), 'regression-triage-')) : dirname(root) === tmpdir() ? root : dirname(root), 'failures.json'));
  mkdirSync(dirname(outPath), { recursive: true });
  writeFileSync(outPath, JSON.stringify(result, null, 2));

  const lines = [];
  if (note) lines.push(note);
  lines.push(`Source: ${source}`);
  if (extraction) lines.push(`Extracted to: ${root} (${extraction.written} files${extraction.skipped.length ? `, ${extraction.skipped.length} unsafe entries skipped` : ''})`);
  lines.push(`Details: ${outPath}`, '');
  lines.push('| Env | Run started (UTC) | Age | Passed | Failed | Timed out | Known bug | Skipped |');
  lines.push('|-----|-------------------|-----|--------|--------|-----------|-----------|---------|');
  for (const env of environments) {
    const timedOut = env.failures.filter((f) => f.status === 'timedOut').length;
    lines.push(`| ${env.name} | ${env.startTime?.slice(0, 16).replace('T', ' ') ?? '?'} | ${env.ageDays ?? '?'} d | ${(env.stats?.expected ?? 0) - env.knownBugs.length} | ${env.failures.length} | ${timedOut} | ${env.knownBugs.length} | ${env.stats?.skipped ?? '?'} |`);
  }
  if (problems.length) lines.push('', 'Problems:', ...problems.map((p) => `- ${p}`));
  const stale = environments.filter((e) => (e.ageDays ?? 0) > 2);
  if (stale.length) lines.push('', `WARNING: this report is ${Math.max(...stale.map((e) => e.ageDays))} days old. Confirm it is the run you mean before triaging.`);
  lines.push('', `Distinct failing cases: ${crossEnv.length} (${crossEnv.filter((c) => c.failedOn.length > 1).length} fail on more than one environment)`);
  lines.push('', 'Most common errors:');
  for (const g of signatureGroups.slice(0, 12)) lines.push(`- ${g.count}x [${g.envs.join(', ')}] ${g.signature}`);
  const flagged = environments.flatMap((e) => e.failures.filter((f) => f.registry || f.markState).map((f) => `- ${e.name} ${f.regId}: ${f.registry?.state ?? f.markState}`));
  if (flagged.length) lines.push('', 'Known-bug marks that need attention:', ...flagged);
  console.log(lines.join('\n'));
}

main();
