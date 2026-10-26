import fs from 'node:fs';
import http from 'node:http';
import path from 'node:path';
import { afterEach, describe, expect, it } from 'vitest';
import { createApp } from '../app.js';
import { sha256 } from '../analysis/signature.js';
import { applyTransaction } from '../patch/applyPatch.js';
import { decideProposal } from '../services/proposalService.js';
import { importReport } from '../services/importService.js';
import { compareTests, dashboard } from '../services/queryService.js';
import { validateProposal } from '../services/validationService.js';
import { htmlReportZip } from '../testing/reports.js';
import { tempAnalyzer } from '../testing/tempApp.js';

const source = [
  "import { test, expect } from '@playwright/test';",
  '',
  "test('saves the form', async ({ page }) => {",
  "  expect(page.getByRole('button', { name: 'Save' })).toBeVisible();",
  '});',
  '',
].join('\n');

describe('import, approval, and history', () => {
  const sessions: { cleanup: () => void }[] = [];
  afterEach(() => {
    for (const session of sessions.splice(0)) session.cleanup();
  });

  function open() {
    const session = tempAnalyzer();
    sessions.push(session);
    return session;
  }

  it('imports a report, keeps optional metadata empty, and rejects a duplicate archive', async () => {
    const { ctx, root } = open();
    fs.writeFileSync(path.join(root, 'tests', 'customer.spec.ts'), source);
    const bytes = htmlReportZip({
      fileName: 'tests/customer.spec.ts',
      title: 'saves the form',
      line: 4,
      attempts: [{ retry: 0, status: 'failed', message: 'bearer super-secret-token Expected: 200 Received: 500' }],
    });
    const first = await importReport(ctx, { bytes, filename: 'dev-report.zip', environment: 'dev', label: null, executedAt: null });
    expect(first.duplicate).toBe(false);
    expect(first.report.label).toBeNull();
    expect(first.report.executedAt).not.toBeNull();
    const stored = ctx.db.get<{ raw_error: string }>(`SELECT raw_error FROM results`);
    expect(stored?.raw_error).not.toContain('super-secret-token');
    expect(fs.readFileSync(path.join(root, 'tests', 'customer.spec.ts'), 'utf8')).toBe(source);
    const second = await importReport(ctx, { bytes, filename: 'copy.zip', environment: 'test', label: 'other', executedAt: null });
    expect(second.duplicate).toBe(true);
    expect(second.report.id).toBe(first.report.id);
    expect(ctx.db.get<{ c: number }>(`SELECT COUNT(*) AS c FROM reports`)?.c).toBe(1);
    const counts = dashboard(ctx, {}).counts;
    expect(counts.importedReports).toBe(1);
    expect(counts.confirmedIntermittent).toBe(0);
    const classification = ctx.db.get<{ classification: string }>(`SELECT classification FROM test_cases`);
    expect(classification?.classification).toBe('insufficient_evidence');
  });

  it('applies only an explicitly approved patch and restores files if a later write fails', async () => {
    const { ctx, root } = open();
    const spec = path.join(root, 'tests', 'customer.spec.ts');
    const sibling = path.join(root, 'tests', 'untouched.spec.ts');
    fs.writeFileSync(spec, source);
    fs.writeFileSync(sibling, 'export const keep = true;\n');
    const bytes = htmlReportZip({
      fileName: 'tests/customer.spec.ts',
      title: 'saves the form',
      line: 4,
      htmlPath: 'index.html',
      attempts: [{ retry: 0, status: 'timedOut', message: 'Timeout 5000ms exceeded waiting for getByRole' }],
    });
    await importReport(ctx, { bytes, filename: 'one.zip', environment: 'dev', label: 'build-1', executedAt: '2026-03-01T00:00:00.000Z' });
    expect(fs.readFileSync(spec, 'utf8')).toBe(source);
    const proposal = ctx.db.get<{ id: string; kind: string; state: string }>(`SELECT id, kind, state FROM proposals`);
    expect(proposal?.kind).toBe('patch');
    expect(proposal?.state).toBe('PROPOSED');
    expect(() => decideProposal(ctx, proposal!.id, 'approve_and_apply', 'wrong-id')).toThrow(/must match/i);
    expect(fs.readFileSync(spec, 'utf8')).toBe(source);
    const applied = await decideProposal(ctx, proposal!.id, 'approve_and_apply', proposal!.id);
    expect(applied.state).toBe('APPLIED');
    expect(applied.appliedDiff).toContain('await expect');
    expect(fs.readFileSync(spec, 'utf8')).toContain('await expect');
    expect(fs.readFileSync(sibling, 'utf8')).toBe('export const keep = true;\n');

    const rollback = path.join(root, 'tests', 'rollback.txt');
    fs.writeFileSync(path.join(root, 'tests', 'rollback-b.txt'), 'b');
    fs.writeFileSync(rollback, 'a');
    expect(() => applyTransaction([
      { absolute: rollback, next: 'A' },
      { absolute: path.join(root, 'tests', 'rollback-b.txt'), next: 'B' },
    ], {
      read: (file) => fs.readFileSync(file, 'utf8'),
      write: (file, content) => fs.writeFileSync(file, content),
      afterWrite(file) {
        if (file === rollback) throw new Error('disk full');
      },
    })).toThrow(/disk full/);
    expect(fs.readFileSync(rollback, 'utf8')).toBe('a');
  });

  it('rejects a stale patch and a path outside the test sources', async () => {
    const { ctx, root } = open();
    const spec = path.join(root, 'tests', 'customer.spec.ts');
    fs.writeFileSync(spec, source);
    const bytes = htmlReportZip({
      fileName: 'tests/customer.spec.ts',
      title: 'saves the form',
      line: 4,
      attempts: [{ retry: 0, status: 'failed', message: 'Timeout 5000ms exceeded' }],
    });
    await importReport(ctx, { bytes, filename: 'stale.zip', environment: 'dev', label: null, executedAt: null });
    fs.writeFileSync(spec, `${source}// local edit\n`);
    const proposal = ctx.db.get<{ id: string }>(`SELECT id FROM proposals`)!;
    expect(() => decideProposal(ctx, proposal.id, 'approve_and_apply', proposal.id)).toThrow(/changed after the diff/i);
    expect(fs.readFileSync(spec, 'utf8')).toContain('local edit');
    expect(ctx.db.get<{ state: string }>(`SELECT state FROM proposals WHERE id = ?`, proposal.id)?.state).toBe('STALE');

    const secret = path.join(root, 'secrets', 'token.ts');
    fs.mkdirSync(path.dirname(secret), { recursive: true });
    const secretText = 'export const token = "stay";\n';
    fs.writeFileSync(secret, secretText);
    ctx.db.run(
      `INSERT INTO proposals (
        id, issue_id, dedupe_key, version, kind, state, summary, rationale, impact, risks, confidence,
        validation_plan, why, files_json, diff_text, root_cause_json, report_ids_json, created_at, approved_at
      ) VALUES ('fix_evil', NULL, 'evil', 1, 'patch', 'PROPOSED', 'no', 'no', 'no', 'no', 'low', 'no', 'no', ?, '', '{}', '[]', ?, NULL)`,
      JSON.stringify([{ path: 'secrets/token.ts', baseHash: sha256(secretText), oldText: secretText, newText: 'export const token = "changed";\n' }]),
      new Date().toISOString(),
    );
    expect(() => decideProposal(ctx, 'fix_evil', 'approve_and_apply', 'fix_evil')).toThrow(/Refusing to modify/i);
    expect(fs.readFileSync(secret, 'utf8')).toBe(secretText);
  });

  it('rejects a proposal without changing the file', async () => {
    const { ctx, root } = open();
    fs.writeFileSync(path.join(root, 'tests', 'customer.spec.ts'), source);
    await importReport(ctx, {
      bytes: htmlReportZip({
        fileName: 'tests/customer.spec.ts',
        title: 'saves the form',
        line: 4,
        attempts: [{ retry: 0, status: 'failed', message: 'Timeout 5000ms exceeded' }],
      }),
      filename: 'reject.zip',
      environment: 'test',
      label: null,
      executedAt: null,
    });
    const proposal = ctx.db.get<{ id: string }>(`SELECT id FROM proposals`)!;
    const rejected = await decideProposal(ctx, proposal.id, 'reject', proposal.id, 'not this one');
    expect(rejected.state).toBe('REJECTED');
    expect(fs.readFileSync(path.join(root, 'tests', 'customer.spec.ts'), 'utf8')).toBe(source);
  });

  it('distinguishes a missing execution from a pass and blocks validation when Playwright is not installed', async () => {
    const { ctx, root } = open();
    fs.writeFileSync(path.join(root, 'tests', 'customer.spec.ts'), source);
    const passBytes = htmlReportZip({
      fileName: 'tests/customer.spec.ts',
      title: 'saves the form',
      line: 4,
      outcome: 'expected',
      attempts: [{ retry: 0, status: 'passed' }],
    });
    const otherBytes = htmlReportZip({
      fileName: 'tests/other.spec.ts',
      title: 'other test',
      line: 4,
      outcome: 'unexpected',
      attempts: [{ retry: 0, status: 'failed', message: 'Expected: 1 Received: 2' }],
    });
    await importReport(ctx, { bytes: passBytes, filename: 'dev.zip', environment: 'dev', label: 'dev', executedAt: '2026-03-01T00:00:00.000Z' });
    await importReport(ctx, { bytes: otherBytes, filename: 'test.zip', environment: 'test', label: 'test', executedAt: '2026-03-02T00:00:00.000Z' });
    const testId = ctx.db.get<{ id: string }>(`SELECT id FROM test_cases WHERE full_title LIKE '%saves the form%'`)?.id;
    expect(testId).toBeTruthy();
    const matrix = compareTests(ctx, testId);
    const saved = matrix.tests.find((test) => test.fullTitle.includes('saves the form'));
    expect(saved?.cells.map((cell) => cell.status)).toEqual(['passed', 'missing']);
    const flaky = htmlReportZip({
      fileName: 'tests/customer.spec.ts',
      title: 'saves the form',
      line: 5,
      outcome: 'flaky',
      attempts: [
        { retry: 0, status: 'timedOut', message: 'Timeout 5000ms exceeded waiting for getByRole' },
        { retry: 1, status: 'passed' },
      ],
    });
    fs.writeFileSync(path.join(root, 'tests', 'customer.spec.ts'), [
      "import { test, expect } from '@playwright/test';",
      '',
      "test('saves the form', async ({ page }) => {",
      '  await page.waitForTimeout(1000);',
      "  await expect(page.getByRole('button', { name: 'Save' })).toBeVisible();",
      '});',
      '',
    ].join('\n'));
    await importReport(ctx, { bytes: flaky, filename: 'retry.zip', environment: 'dev', label: 'retry', executedAt: '2026-03-03T00:00:00.000Z' });
    const classification = ctx.db.get<{ classification: string }>(
      `SELECT classification FROM test_cases WHERE full_title LIKE '%saves the form%'`,
    );
    expect(classification?.classification).toBe('confirmed_intermittent');
    const proposal = ctx.db.get<{ id: string; state: string }>(
      `SELECT id, state FROM proposals WHERE kind = 'patch' AND state = 'PROPOSED' ORDER BY created_at DESC LIMIT 1`,
    );
    expect(proposal).toBeTruthy();
    await decideProposal(ctx, proposal!.id, 'approve_and_apply', proposal!.id);
    const validation = await validateProposal(ctx, proposal!.id, {
      environment: 'dev',
      repetitions: 1,
      confirm: true,
      proposalId: proposal!.id,
    }) as { status: string; supports_fix: number };
    expect(validation.status).toBe('BLOCKED');
    expect(validation.supports_fix).toBe(0);
    expect(ctx.db.get<{ state: string }>(`SELECT state FROM proposals WHERE id = ?`, proposal!.id)?.state).toBe('BLOCKED');
  });

  it('refuses a mutating request without the local header', async () => {
    const { ctx } = open();
    const app = createApp(ctx);
    const server = http.createServer(app);
    await new Promise<void>((resolve) => server.listen(0, '127.0.0.1', () => resolve()));
    const address = server.address();
    const port = typeof address === 'object' && address ? address.port : 0;
    const request = (method: string, requestPath: string) => new Promise<{ status: number; body: string }>((resolve, reject) => {
      const req = http.request({ hostname: '127.0.0.1', port, path: requestPath, method }, (res) => {
        const chunks: Buffer[] = [];
        res.on('data', (chunk: Buffer) => chunks.push(chunk));
        res.on('end', () => resolve({ status: res.statusCode ?? 0, body: Buffer.concat(chunks).toString('utf8') }));
      });
      req.on('error', reject);
      req.end();
    });
    try {
      const denied = await request('POST', '/api/reports/import');
      expect(denied.status).toBe(403);
      const health = await request('GET', '/api/health');
      expect(health.status).toBe(200);
      expect(JSON.parse(health.body).ok).toBe(true);
    } finally {
      await new Promise<void>((resolve, reject) => server.close((error) => error ? reject(error) : resolve()));
    }
  });
});
