import { spawn } from 'node:child_process';
import fs from 'node:fs';
import path from 'node:path';
import { randomUUID } from 'node:crypto';
import type { EnvironmentName } from '../../shared/types.js';
import { ENVIRONMENTS } from '../../shared/types.js';
import type { AppContext, CommandResult } from '../context.js';
import { readEnvironmentUrl } from '../config.js';
import { HttpError } from '../errors.js';
import { clip } from '../redact.js';
import { asArray, asRecord, asString } from '../parse/model.js';

const DISCLAIMER = 'A small number of repeated runs cannot prove that an intermittent test is permanently fixed. The counts below are the observed sample.';

export function spawnRunner(command: string, args: string[], options: { cwd: string; env: NodeJS.ProcessEnv; timeoutMs: number }): Promise<CommandResult> {
  return new Promise((resolve) => {
    let timedOut = false;
    let stdout = '';
    let stderr = '';
    const child = spawn(command, args, { cwd: options.cwd, env: options.env, shell: false });
    const timer = setTimeout(() => {
      timedOut = true;
      child.kill();
    }, options.timeoutMs);
    child.stdout?.setEncoding('utf8');
    child.stderr?.setEncoding('utf8');
    child.stdout?.on('data', (chunk: string) => {
      stdout = `${stdout}${chunk}`.slice(-100_000);
    });
    child.stderr?.on('data', (chunk: string) => {
      stderr = `${stderr}${chunk}`.slice(-40_000);
    });
    child.on('error', (error) => {
      clearTimeout(timer);
      resolve({ exitCode: null, stdout, stderr, timedOut, spawnError: error.message });
    });
    child.on('close', (code) => {
      clearTimeout(timer);
      resolve({ exitCode: code, stdout, stderr, timedOut, spawnError: null });
    });
  });
}

function escapeRegex(value: string): string {
  return value.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
}

type Counts = { passed: number; failed: number; skipped: number; interrupted: number; flaky: number; total: number };

function emptyCounts(): Counts {
  return { passed: 0, failed: 0, skipped: 0, interrupted: 0, flaky: 0, total: 0 };
}

function addCounts(left: Counts, right: Counts): Counts {
  return {
    passed: left.passed + right.passed,
    failed: left.failed + right.failed,
    skipped: left.skipped + right.skipped,
    interrupted: left.interrupted + right.interrupted,
    flaky: left.flaky + right.flaky,
    total: left.total + right.total,
  };
}

export function countJsonReport(value: unknown): Counts | null {
  const root = asRecord(value);
  if (!root || !Array.isArray(root.suites)) return null;
  const counts = emptyCounts();
  const walk = (suite: Record<string, unknown>) => {
    for (const spec of asArray(suite.specs)) {
      const record = asRecord(spec);
      if (!record) continue;
      counts.total += 1;
      const tests = asArray(record.tests).map((test) => asRecord(test) ?? {});
      const statuses = tests.map((test) => asString(test.status));
      const resultStatuses = tests.flatMap((test) => asArray(test.results).map((result) => asString(asRecord(result)?.status)));
      if (statuses.includes('skipped') || resultStatuses.every((status) => status === 'skipped')) counts.skipped += 1;
      else if (statuses.includes('flaky') || (record.ok === true && resultStatuses.includes('failed'))) counts.flaky += 1;
      else if (record.ok === false || resultStatuses.includes('interrupted') || statuses.includes('unexpected')) {
        if (resultStatuses.includes('interrupted')) counts.interrupted += 1;
        else counts.failed += 1;
      } else counts.passed += 1;
    }
    for (const child of asArray(suite.suites)) {
      const record = asRecord(child);
      if (record) walk(record);
    }
  };
  for (const suite of root.suites) {
    const record = asRecord(suite);
    if (record) walk(record);
  }
  return counts;
}

function extractJson(stdout: string): unknown | null {
  const start = stdout.indexOf('{');
  const end = stdout.lastIndexOf('}');
  if (start < 0 || end <= start) return null;
  try {
    return JSON.parse(stdout.slice(start, end + 1)) as unknown;
  } catch {
    return null;
  }
}

export async function validateProposal(ctx: AppContext, id: string, input: { environment: string; repetitions?: number; confirm?: boolean; proposalId: string }) {
  if (input.proposalId !== id) throw new HttpError(400, 'Confirm the proposal id in the request body before validation starts.', 'PROPOSAL_MISMATCH');
  if (input.confirm !== true) throw new HttpError(400, 'Set confirm=true to start a Playwright validation run.', 'CONFIRMATION_REQUIRED');
  if (!(ENVIRONMENTS as readonly string[]).includes(input.environment)) {
    throw new HttpError(400, 'Validation environment must be dev, dev2, or test. Production is not available.', 'BAD_ENVIRONMENT');
  }
  const environment = input.environment as EnvironmentName;
  const repetitions = input.repetitions ?? 1;
  if (!Number.isInteger(repetitions) || repetitions < 1 || repetitions > ctx.config.maxValidationRepeats) {
    throw new HttpError(400, `Repetitions must be an integer from 1 to ${ctx.config.maxValidationRepeats}.`, 'BAD_REPEATS');
  }
  const proposal = ctx.db.get<{ id: string; state: string; kind: string }>(`SELECT id, state, kind FROM proposals WHERE id = ?`, id);
  if (!proposal) throw new HttpError(404, 'Proposal was not found.', 'NOT_FOUND');
  if (proposal.kind !== 'patch' || !['APPLIED', 'BLOCKED', 'VALIDATION_FAILED'].includes(proposal.state)) {
    throw new HttpError(409, 'Validation runs only after this proposal’s patch has been applied.', 'NOT_APPLIED');
  }
  const tests = ctx.db.all<{ full_title: string; file_path: string | null }>(
    `SELECT tc.full_title, tc.file_path FROM proposal_tests pt JOIN test_cases tc ON tc.id = pt.test_id WHERE pt.proposal_id = ?`,
    id,
  );
  const runnable = tests.filter((test) => test.file_path);
  if (runnable.length === 0) {
    return recordValidation(ctx, id, {
      command: '',
      environment,
      selected: [],
      startedAt: new Date().toISOString(),
      endedAt: new Date().toISOString(),
      exitCode: null,
      counts: emptyCounts(),
      repetitions,
      reportPath: null,
      status: 'BLOCKED',
      log: 'No test file path is stored for this proposal.',
      supports: false,
    });
  }
  let baseUrl: string;
  try {
    baseUrl = readEnvironmentUrl(ctx.config.automationRoot, environment);
  } catch (error) {
    throw new HttpError(400, error instanceof Error ? error.message : 'Environment URL is not configured.', 'ENV_URL_MISSING');
  }
  const cli = path.join(ctx.config.automationRoot, 'node_modules', '@playwright', 'test', 'cli.js');
  const byFile = new Map<string, string[]>();
  for (const test of runnable.slice(0, 5)) {
    if (!test.file_path) continue;
    byFile.set(test.file_path, [...(byFile.get(test.file_path) ?? []), test.full_title]);
  }
  const commands: string[][] = [...byFile.entries()].map(([file, titles]) => [
    cli,
    'test',
    file,
    '-g',
    titles.map(escapeRegex).join('|'),
    '--reporter=json',
  ]);
  const commandText = commands.map((args) => `${process.execPath} ${args.join(' ')}`).join(' && ');
  const validationId = `val_${randomUUID()}`;
  const reportDir = path.join(ctx.config.storageDir, 'validations', validationId);
  fs.mkdirSync(reportDir, { recursive: true });
  const startedAt = new Date().toISOString();
  if (!fs.existsSync(cli)) {
    return recordValidation(ctx, id, {
      command: commandText,
      environment,
      selected: runnable.map((test) => test.full_title),
      startedAt,
      endedAt: new Date().toISOString(),
      exitCode: null,
      counts: emptyCounts(),
      repetitions,
      reportPath: null,
      status: 'BLOCKED',
      log: 'Playwright is not installed in the automation repository (node_modules/@playwright/test is missing). No tests were executed.',
      supports: false,
    });
  }
  const runner = ctx.runner ?? spawnRunner;
  let counts = emptyCounts();
  let exitCode: number | null = 0;
  let log = '';
  let blocked = false;
  let parsed = true;
  let observedFailures = 0;
  for (let repeat = 0; repeat < repetitions; repeat += 1) {
    for (const args of commands) {
      const result = await runner(process.execPath, args, {
        cwd: ctx.config.automationRoot,
        env: { ...process.env, BASE_URL: baseUrl },
        timeoutMs: ctx.config.validationTimeoutMs,
      });
      log += `\n[repeat ${repeat + 1}] exit=${result.exitCode ?? 'null'} timedOut=${result.timedOut}\n${result.stderr}\n`;
      const json = extractJson(result.stdout);
      const reportFile = path.join(reportDir, `repeat-${repeat + 1}-${path.basename(args[2] ?? 'test')}.json`);
      if (json) fs.writeFileSync(reportFile, JSON.stringify(json));
      else if (result.stdout) fs.writeFileSync(reportFile, result.stdout);
      const nextCounts = json ? countJsonReport(json) : null;
      if (!nextCounts) parsed = false;
      else counts = addCounts(counts, nextCounts);
      if (result.spawnError || result.timedOut) blocked = true;
      if (result.exitCode !== 0) {
        exitCode = result.exitCode;
        observedFailures += 1;
      }
      const blob = `${result.stdout}\n${result.stderr}`;
      if (/No auth endpoint|Failed to get token|Cannot find module/i.test(blob) && (nextCounts?.total ?? 0) === 0) blocked = true;
    }
  }
  const supports = !blocked && parsed && exitCode === 0 && counts.failed === 0 && counts.interrupted === 0 && counts.flaky === 0 && counts.total > 0;
  let status: 'VALIDATED' | 'VALIDATION_FAILED' | 'BLOCKED' = 'VALIDATION_FAILED';
  if (blocked && counts.total === 0) status = 'BLOCKED';
  else if (supports) status = 'VALIDATED';
  if (!parsed && status === 'VALIDATED') status = 'BLOCKED';
  return recordValidation(ctx, id, {
    command: `BASE_URL=${baseUrl} ${commandText}`,
    environment,
    selected: runnable.map((test) => test.full_title),
    startedAt,
    endedAt: new Date().toISOString(),
    exitCode,
    counts,
    repetitions,
    reportPath: reportDir,
    status,
    log: clip(log, 8000) ?? '',
    supports,
    observedFailures,
  });
}

function recordValidation(ctx: AppContext, proposalId: string, input: {
  command: string;
  environment: string;
  selected: string[];
  startedAt: string;
  endedAt: string;
  exitCode: number | null;
  counts: Counts;
  repetitions: number;
  reportPath: string | null;
  status: 'VALIDATED' | 'VALIDATION_FAILED' | 'BLOCKED';
  log: string;
  supports: boolean;
  observedFailures?: number;
}) {
  const id = `val_${randomUUID()}`;
  const observed = input.observedFailures ?? input.counts.failed + input.counts.flaky + input.counts.interrupted;
  ctx.db.transaction(() => {
    ctx.db.run(
      `INSERT INTO validations (
        id, proposal_id, command, environment, selected_tests, started_at, ended_at, exit_code,
        passed, failed, skipped, interrupted, flaky, repetitions, observed_failures, report_path,
        supports_fix, status, log_excerpt, disclaimer
      ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)`,
      id,
      proposalId,
      input.command,
      input.environment,
      JSON.stringify(input.selected),
      input.startedAt,
      input.endedAt,
      input.exitCode,
      input.counts.passed,
      input.counts.failed,
      input.counts.skipped,
      input.counts.interrupted,
      input.counts.flaky,
      input.repetitions,
      observed,
      input.reportPath,
      input.supports ? 1 : 0,
      input.status,
      input.log,
      DISCLAIMER,
    );
    ctx.db.run(
      `UPDATE proposals SET state = ?, block_reason = ? WHERE id = ?`,
      input.status,
      input.status === 'VALIDATED' ? null : input.log.slice(0, 500),
      proposalId,
    );
  });
  return ctx.db.get(`SELECT * FROM validations WHERE id = ?`, id);
}

export function validationResult(ctx: AppContext, id: string) {
  const row = ctx.db.get(`SELECT * FROM validations WHERE id = ?`, id);
  if (!row) throw new HttpError(404, 'Validation run was not found.', 'NOT_FOUND');
  return row;
}
