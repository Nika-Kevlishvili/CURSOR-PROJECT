import { describe, expect, it } from 'vitest';
import { HttpError } from '../errors.js';
import { parseArchive } from '../parse/parseArchive.js';
import { htmlReportZip, jsonReporterZip, zipBytes } from '../testing/reports.js';
import type { ArchiveLimits } from '../config.js';
import { unzipSafely } from '../zip/safeZip.js';

const limits: ArchiveLimits = {
  maxArchiveBytes: 5 * 1024 * 1024,
  maxExpandedBytes: 5 * 1024 * 1024,
  maxFiles: 20,
  maxFileBytes: 5 * 1024 * 1024,
  maxProcessingMs: 5000,
};

function parsed(bytes: Uint8Array) {
  return parseArchive(unzipSafely(bytes, limits, Date.now()), limits, Date.now());
}

describe('report parsing', () => {
  it('reads a Playwright HTML report nested in a directory and does not execute its script', () => {
    const report = parsed(htmlReportZip({
      attempts: [{ retry: 0, status: 'failed', message: 'Expected: 200 Received: 500' }],
    }));
    expect(report.parser).toBe('html-embedded');
    expect(report.playwrightVersion).toBeNull();
    expect(report.sourceReportPath).toContain('nested/');
    expect(report.tests).toHaveLength(1);
    expect(report.tests[0]?.fullTitle).toBe('Customer > creates a customer');
    expect(report.tests[0]?.attempts[0]?.workerIndex).toBeNull();
    expect(report.tests[0]?.expectedStatus).toBeNull();
  });

  it('reads the JSON reporter when the HTML report has no embedded data', () => {
    const report = parsed(jsonReporterZip());
    expect(report.parser).toBe('json-reporter');
    expect(report.playwrightVersion).toBe('1.56.0');
    expect(report.tests[0]?.expectedStatus).toBe('passed');
    expect(report.tests[0]?.attempts[0]?.workerIndex).toBe(4);
    expect(report.tests[0]?.fullTitle).toBe('Billing > json reporter test');
  });

  it('uses a JSON block inside HTML as the fallback', () => {
    const html = `<html><script type="application/json">${JSON.stringify({
      config: { version: '1.56.0' },
      suites: [{
        title: 'Fallback',
        specs: [{
          title: 'fallback test',
          ok: false,
          tags: [],
          file: 'tests/fallback.spec.ts',
          line: 2,
          column: 1,
          tests: [{ expectedStatus: 'passed', projectName: 'main', results: [{ status: 'failed', retry: 0, errors: [{ message: 'boom' }] }] }],
        }],
      }],
    })}</script></html>`;
    const report = parsed(zipBytes({ 'reports/index.html': html }));
    expect(report.parser).toBe('html-fallback');
    expect(report.tests[0]?.title).toBe('fallback test');
  });

  it('rejects an archive with no report', () => {
    expect(() => parsed(zipBytes({ 'readme.txt': 'hello' }))).toThrow(HttpError);
  });

  it('rejects an HTML file that is not a Playwright report', () => {
    const html = '<html><body><h1>Not a report</h1></body></html>';
    expect(() => parsed(zipBytes({ 'index.html': html }))).toThrow(/no readable Playwright report data/i);
  });

  it('rejects a broken embedded report payload', () => {
    const html = '<html><script id="playwrightReportBase64" type="application/zip">data:application/zip;base64,@@@@</script></html>';
    expect(() => parsed(zipBytes({ 'index.html': html }))).toThrow(/could not be decoded/i);
  });

  it('keeps assertion text when the HTML report stores errors as strings', () => {
    const report = parsed(htmlReportZip({
      attempts: [{ retry: 0, status: 'failed', asString: true, message: 'Error: Expected: > 0\nReceived: 0' }],
    }));
    expect(report.tests[0]?.attempts[0]?.errorMessage).toContain('Received: 0');
  });
});
