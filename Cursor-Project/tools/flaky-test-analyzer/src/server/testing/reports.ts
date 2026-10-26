import { Buffer } from 'node:buffer';
import { zipSync, strToU8 } from 'fflate';

export function zipBytes(entries: Record<string, string | Uint8Array>): Uint8Array {
  const files: Record<string, Uint8Array> = {};
  for (const [name, value] of Object.entries(entries)) {
    files[name] = typeof value === 'string' ? strToU8(value) : value;
  }
  return zipSync(files);
}

export type HtmlAttempt = {
  retry: number;
  status: string;
  message?: string;
  asString?: boolean;
  workerIndex?: number;
};

export function htmlReportZip(options: {
  htmlPath?: string;
  fileName?: string;
  title?: string;
  line?: number;
  outcome?: string;
  suite?: string[];
  attempts: HtmlAttempt[];
  extra?: Record<string, string>;
}): Uint8Array {
  const fileName = options.fileName ?? 'tests/customer.spec.ts';
  const title = options.title ?? 'creates a customer';
  const tests = [{
    testId: 't1',
    title,
    projectName: 'main',
    location: { file: fileName, line: options.line ?? 4, column: 1 },
    duration: 1000,
    annotations: [{ type: 'tag-note', description: 'fixture' }],
    tags: ['@customer'],
    outcome: options.outcome ?? 'unexpected',
    path: options.suite ?? ['Customer'],
    ok: options.outcome === 'expected' || options.outcome === 'flaky',
    results: options.attempts.map((attempt) => ({
      duration: 100,
      startTime: '2026-03-01T00:00:00.000Z',
      retry: attempt.retry,
      steps: [],
      errors: attempt.message ? (attempt.asString ? [attempt.message] : [{ message: attempt.message }]) : [],
      status: attempt.status,
      annotations: [],
      attachments: [{ name: 'screenshot', contentType: 'image/png', path: 'data/shot.png' }],
    })),
  }];
  const inner = zipBytes({
    'report.json': JSON.stringify({
      startTime: Date.parse('2026-03-01T00:00:00.000Z'),
      duration: 1500,
      files: [{ fileId: 'file1', fileName, tests: [] }],
      projectNames: ['main'],
      stats: { total: 1 },
      errors: [],
    }),
    'file1.json': JSON.stringify({ fileId: 'file1', fileName, tests }),
  });
  const html = `<!doctype html><html><head><title>Playwright Test Report</title></head><body><script id="playwrightReportBase64" type="application/zip">data:application/zip;base64,${Buffer.from(inner).toString('base64')}</script><script>throw new Error('report script must not execute');</script></body></html>`;
  return zipBytes({
    [options.htmlPath ?? 'nested/playwright-report/index.html']: html,
    ...(options.extra ?? {}),
  });
}

export function jsonReporterZip(title = 'json reporter test'): Uint8Array {
  const report = {
    config: { version: '1.56.0' },
    suites: [{
      title: 'tests/json.spec.ts',
      file: 'tests/json.spec.ts',
      suites: [{
        title: 'Billing',
        specs: [{
          title,
          ok: false,
          tags: ['@billing'],
          file: 'tests/json.spec.ts',
          line: 12,
          column: 3,
          tests: [{
            expectedStatus: 'passed',
            projectName: 'main',
            status: 'unexpected',
            results: [{
              workerIndex: 4,
              status: 'failed',
              duration: 80,
              retry: 0,
              startTime: '2026-03-02T00:00:00.000Z',
              error: { message: 'bearer super-secret-token failed expected 200 received 500', stack: 'at tests/json.spec.ts:12:3' },
              errors: [],
              attachments: [],
            }],
          }],
        }],
      }],
    }],
  };
  return zipBytes({ 'out/playwright-report.json': JSON.stringify(report) });
}
