import { unzipSafely, type ArchiveFile } from '../zip/safeZip.js';
import type { ArchiveLimits } from '../config.js';
import { HttpError } from '../errors.js';
import { clip } from '../redact.js';
import {
  asArray,
  asNumber,
  asRecord,
  asString,
  attemptStatus,
  fullTitle,
  isFileSuiteTitle,
  posixPath,
  type NormalizedAttempt,
  type NormalizedResult,
  type ParsedReport,
} from './model.js';

function textOf(bytes: Uint8Array): string {
  return new TextDecoder('utf-8', { fatal: false }).decode(bytes);
}

export function extractEmbeddedZip(html: string): Uint8Array | 'missing' | 'malformed' {
  const script = html.match(/id=["']playwrightReportBase64["'][^>]*>([\s\S]*?)<\/script>/i);
  const assigned = html.match(/playwrightReportBase64\s*=\s*["'](data:application\/zip;base64,[^"']+)["']/i);
  const payload = script?.[1] ?? assigned?.[1];
  if (!payload || !payload.includes('base64,')) {
    if (script || assigned || html.includes('playwrightReportBase64')) return 'malformed';
    return 'missing';
  }
  const b64 = payload.slice(payload.indexOf('base64,') + 'base64,'.length).replace(/\s+/g, '');
  if (!b64 || !/^[A-Za-z0-9+/=]+$/.test(b64)) return 'malformed';
  const bytes = Buffer.from(b64, 'base64');
  if (bytes.byteLength < 4 || bytes[0] !== 0x50 || bytes[1] !== 0x4b) return 'malformed';
  return new Uint8Array(bytes);
}

function errorParts(errors: unknown[]): { message: string; stack: string } {
  const texts = errors.map((error) => {
    if (typeof error === 'string') return error;
    const record = asRecord(error);
    if (!record) return '';
    return [asString(record.message), asString(record.stack), asString(record.codeframe)].filter((item): item is string => Boolean(item)).join('\n');
  }).filter((item) => item.length > 0);
  const message = texts.join('\n\n');
  return { message, stack: message };
}

function attachmentFrom(value: unknown): NormalizedAttempt['attachments'][number] | null {
  const record = asRecord(value);
  if (!record) return null;
  const name = asString(record.name) ?? 'attachment';
  const body = asString(record.body);
  return {
    name,
    contentType: asString(record.contentType),
    path: asString(record.path),
    text: body ? clip(body, 2000) : null,
  };
}

function attemptsFromHtml(results: unknown[]): NormalizedAttempt[] {
  return results.map((result) => {
    const record = asRecord(result) ?? {};
    const { message, stack } = errorParts(asArray(record.errors));
    return {
      retryIndex: asNumber(record.retry),
      status: attemptStatus(record.status),
      durationMs: asNumber(record.duration),
      startTime: asString(record.startTime),
      workerIndex: null,
      errorMessage: clip(message || null, 8000),
      errorStack: clip(stack || null, 8000),
      attachments: asArray(record.attachments).map(attachmentFrom).filter((item): item is NonNullable<typeof item> => Boolean(item)),
    };
  });
}

function parseHtmlTests(files: ArchiveFile[], sourceReportPath: string): NormalizedResult[] {
  const tests: NormalizedResult[] = [];
  for (const file of files) {
    if (!file.path.endsWith('.json') || file.path.endsWith('report.json')) continue;
    let parsed: unknown;
    try {
      parsed = JSON.parse(textOf(file.bytes));
    } catch {
      continue;
    }
    const record = asRecord(parsed);
    const fileName = posixPath(asString(record?.fileName));
    for (const test of asArray(record?.tests)) {
      const item = asRecord(test);
      if (!item) continue;
      const location = asRecord(item.location);
      const title = asString(item.title) ?? '(untitled test)';
      const suitePath = asArray(item.path).map((part) => asString(part)).filter((part): part is string => Boolean(part));
      const filePath = posixPath(asString(location?.file)) ?? fileName;
      tests.push({
        title,
        suitePath,
        fullTitle: fullTitle(suitePath, title),
        filePath,
        line: asNumber(location?.line),
        column: asNumber(location?.column),
        projectName: asString(item.projectName),
        tags: asArray(item.tags).map((tag) => asString(tag)).filter((tag): tag is string => Boolean(tag)),
        annotations: asArray(item.annotations).map((annotation) => {
          const row = asRecord(annotation);
          return { type: asString(row?.type) ?? 'annotation', description: asString(row?.description) };
        }),
        expectedStatus: null,
        outcome: asString(item.outcome),
        repeatEachIndex: null,
        attempts: attemptsFromHtml(asArray(item.results)),
        sourceRef: sourceReportPath,
      });
    }
  }
  return tests;
}

export function parseHtmlEmbedded(htmlPath: string, html: string, limits: ArchiveLimits, startedAt: number): ParsedReport | null {
  const embedded = extractEmbeddedZip(html);
  if (embedded === 'missing') return null;
  if (embedded === 'malformed') {
    throw new HttpError(400, `${htmlPath} contains a Playwright report marker that could not be decoded.`, 'MALFORMED_REPORT_DATA');
  }
  let inner: ArchiveFile[];
  try {
    inner = unzipSafely(embedded, limits, startedAt);
  } catch (error) {
    if (error instanceof HttpError && error.code === 'MALFORMED_ZIP') {
      throw new HttpError(400, `${htmlPath} contains a Playwright data ZIP that could not be read.`, 'MALFORMED_REPORT_DATA');
    }
    throw error;
  }
  const reportJson = inner.find((file) => file.path === 'report.json' || file.path.endsWith('/report.json'));
  let startedAtIso: string | null = null;
  let durationMs: number | null = null;
  if (reportJson) {
    try {
      const report = asRecord(JSON.parse(textOf(reportJson.bytes)));
      const start = asNumber(report?.startTime);
      startedAtIso = start === null ? null : new Date(start).toISOString();
      durationMs = asNumber(report?.duration);
    } catch {
      throw new HttpError(400, `${htmlPath} contains report.json that is not valid JSON.`, 'MALFORMED_REPORT_DATA');
    }
  }
  const tests = parseHtmlTests(inner, htmlPath);
  if (tests.length === 0 && !reportJson) return null;
  return {
    parser: 'html-embedded',
    playwrightVersion: null,
    startedAt: startedAtIso,
    durationMs,
    warnings: tests.length === 0 ? ['The embedded Playwright report did not contain test records.'] : [],
    tests,
    sourceReportPath: htmlPath,
  };
}

function walkJsonSuite(
  suite: Record<string, unknown>,
  suitePath: string[],
  fileHint: string | null,
  sourceRef: string,
  tests: NormalizedResult[],
): void {
  const filePath = posixPath(asString(suite.file)) ?? fileHint;
  const title = asString(suite.title);
  const nextPath = title && !isFileSuiteTitle(title, filePath) ? [...suitePath, title] : suitePath;
  for (const spec of asArray(suite.specs)) {
    const specRecord = asRecord(spec);
    if (!specRecord) continue;
    const specTitle = asString(specRecord.title) ?? '(untitled test)';
    const specFile = posixPath(asString(specRecord.file)) ?? filePath;
    for (const test of asArray(specRecord.tests)) {
      const testRecord = asRecord(test);
      if (!testRecord) continue;
      const attempts = asArray(testRecord.results).map((result) => {
        const row = asRecord(result) ?? {};
        const { message, stack } = errorParts([row.error, ...asArray(row.errors)].filter((item) => item !== undefined));
        return {
          retryIndex: asNumber(row.retry),
          status: attemptStatus(row.status),
          durationMs: asNumber(row.duration),
          startTime: asString(row.startTime),
          workerIndex: asNumber(row.workerIndex),
          errorMessage: clip(message || null, 8000),
          errorStack: clip(stack || null, 8000),
          attachments: asArray(row.attachments).map(attachmentFrom).filter((item): item is NonNullable<typeof item> => Boolean(item)),
        };
      });
      tests.push({
        title: specTitle,
        suitePath: nextPath,
        fullTitle: fullTitle(nextPath, specTitle),
        filePath: specFile,
        line: asNumber(specRecord.line),
        column: asNumber(specRecord.column),
        projectName: asString(testRecord.projectName) ?? asString(asRecord(testRecord.project)?.name),
        tags: asArray(specRecord.tags).map((tag) => asString(tag)).filter((item): item is string => Boolean(item)),
        annotations: asArray(testRecord.annotations).map((annotation) => {
          const row = asRecord(annotation);
          return { type: asString(row?.type) ?? 'annotation', description: asString(row?.description) };
        }),
        expectedStatus: asString(testRecord.expectedStatus),
        outcome: asString(testRecord.status),
        repeatEachIndex: asNumber(testRecord.repeatEachIndex),
        attempts,
        sourceRef,
      });
    }
  }
  for (const child of asArray(suite.suites)) {
    const childRecord = asRecord(child);
    if (childRecord) walkJsonSuite(childRecord, nextPath, filePath, sourceRef, tests);
  }
}

export function parseJsonReporter(jsonPath: string, raw: unknown): ParsedReport | null {
  const root = asRecord(raw);
  if (!root || !Array.isArray(root.suites)) return null;
  const tests: NormalizedResult[] = [];
  for (const suite of root.suites) {
    const record = asRecord(suite);
    if (record) walkJsonSuite(record, [], null, jsonPath, tests);
  }
  const config = asRecord(root.config);
  return {
    parser: 'json-reporter',
    playwrightVersion: asString(config?.version),
    startedAt: null,
    durationMs: null,
    warnings: [],
    tests,
    sourceReportPath: jsonPath,
  };
}

export function parseHtmlFallback(htmlPath: string, html: string): ParsedReport | null {
  const scripts = [...html.matchAll(/<script[^>]*type=["']application\/json["'][^>]*>([\s\S]*?)<\/script>/gi)];
  for (const match of scripts) {
    try {
      const parsed = parseJsonReporter(htmlPath, JSON.parse(match[1] ?? ''));
      if (parsed && parsed.tests.length > 0) {
        return { ...parsed, parser: 'html-fallback', warnings: ['Test data was recovered from a JSON block inside the HTML report.'] };
      }
    } catch {
      // Try the next block. The HTML itself is never executed.
    }
  }
  return null;
}

function score(report: ParsedReport): number {
  return report.tests.length;
}

export function parseArchive(files: ArchiveFile[], limits: ArchiveLimits, startedAt: number): ParsedReport {
  const htmlFiles = files.filter((file) => file.path.toLowerCase().endsWith('.html'));
  const embedded: ParsedReport[] = [];
  for (const file of htmlFiles) {
    const report = parseHtmlEmbedded(file.path, textOf(file.bytes), limits, startedAt);
    if (report) embedded.push(report);
  }
  if (embedded.length > 0) {
    embedded.sort((a, b) => score(b) - score(a));
    const chosen = embedded[0];
    if (!chosen || chosen.tests.length === 0) {
      throw new HttpError(422, 'The HTML report was recognized, but it did not contain test records.', 'NO_TESTS');
    }
    if (embedded.length > 1) {
      chosen.warnings.push(`Multiple HTML reports were found. Used ${chosen.sourceReportPath} (${chosen.tests.length} tests).`);
    }
    const jsonSibling = files.some((file) => file.path.endsWith('.json'));
    if (jsonSibling) chosen.warnings.push('A JSON file was also present. The embedded HTML report was used.');
    return chosen;
  }

  const jsonCandidates = files.filter((file) => file.path.toLowerCase().endsWith('.json'));
  const jsonReports: ParsedReport[] = [];
  for (const file of jsonCandidates) {
    try {
      const parsed = parseJsonReporter(file.path, JSON.parse(textOf(file.bytes)));
      if (parsed && parsed.tests.length > 0) jsonReports.push(parsed);
    } catch {
      // Not a Playwright JSON report.
    }
  }
  if (jsonReports.length > 0) {
    jsonReports.sort((a, b) => score(b) - score(a));
    const chosen = jsonReports[0];
    if (!chosen) throw new HttpError(422, 'No Playwright report data was found in the archive.', 'NO_REPORT_DATA');
    return chosen;
  }

  for (const file of htmlFiles) {
    const fallback = parseHtmlFallback(file.path, textOf(file.bytes));
    if (fallback) return fallback;
  }

  if (htmlFiles.length === 0) {
    throw new HttpError(422, 'The archive does not contain an HTML report or a Playwright JSON report.', 'NO_REPORT_DATA');
  }
  throw new HttpError(
    422,
    'An HTML file was found, but it has no readable Playwright report data. Export the HTML report folder produced by Playwright 1.56 (index.html with embedded report data) or include playwright-report.json.',
    'UNSUPPORTED_REPORT',
  );
}
