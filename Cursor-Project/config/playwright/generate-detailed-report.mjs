#!/usr/bin/env node
/**
 * Builds a human-readable detailed Markdown report from Playwright JSON output.
 * Usage (from EnergoTS root): node ../config/playwright/generate-detailed-report.mjs
 * Default JSON: cwd/playwright-report.json | Default out: cwd/playwright-report-detailed.md (next to JSON)
 * Overrides: node generate-detailed-report.mjs <path/to.json> [path/to/out.md]
 * See README-detailed-reporting.md and Rule DPR.0.
 */

import * as fs from 'fs';
import * as path from 'path';
import { fileURLToPath } from 'url';

const __dirname = path.dirname(fileURLToPath(import.meta.url));

const skipSetup = process.env.DETAILED_REPORT_SKIP_SETUP !== '0';

function shouldSkipFile(filePath) {
  if (!skipSetup || !filePath) return false;
  const f = filePath.replace(/\\/g, '/').toLowerCase();
  return (
    f.includes('global-setup') ||
    f.includes('global-teardown') ||
    f.includes('global_setup') ||
    f.includes('global_teardown')
  );
}

function lastResultStatus(test) {
  const results = test.results || [];
  if (!results.length) return 'unknown';
  return results[results.length - 1].status || 'unknown';
}

function formatDurationMs(test) {
  let total = 0;
  for (const r of test.results || []) total += r.duration || 0;
  return total;
}

function collectStepTitles(test) {
  const titles = [];
  for (const r of test.results || []) {
    for (const step of r.steps || []) {
      if (step.title) titles.push(step.title);
    }
  }
  return titles;
}

function collectErrorSnippet(test) {
  for (const r of test.results || []) {
    if (r.error?.message) return String(r.error.message).split('\n')[0].slice(0, 2000);
    if (r.errors?.[0]?.message) return String(r.errors[0].message).split('\n')[0].slice(0, 2000);
  }
  return '';
}

function annotationsBlock(test) {
  const ann = test.annotations || [];
  if (!ann.length)
    return '_No annotations (optional: use `test.info().annotate(\'tc_objective\', …)` for a short objective beyond the title)._';
  return ann
    .map((a) => `- **${a.type || 'annotation'}:** ${a.description || ''}`)
    .join('\n');
}

function walkSuites(suites, ancestorTitles, visitSpec) {
  for (const suite of suites || []) {
    const chain = [...ancestorTitles, suite.title].filter(Boolean);
    const suiteFile = suite.file || '';
    for (const spec of suite.specs || []) visitSpec(spec, chain, suiteFile);
    walkSuites(suite.suites || [], chain, visitSpec);
  }
}

function escapeMd(s) {
  return String(s).replace(/\|/g, '\\|');
}

/**
 * Parse Jira key + TC-BE-N / TC-FE-N + scenario from common Playwright title patterns.
 * Examples: "[PDT-2599] TC-BE-1: Something" or "[REG-1] TC-FE-2 — UI check"
 */
function parseTestCaseFromTitle(title) {
  const raw = String(title || '').trim();
  if (!raw) {
    return { jira: '—', tcId: '—', scenario: '—', matched: false };
  }
  const m = raw.match(
    /^\[([A-Z][A-Z0-9]*-\d+)\]\s*(TC-(?:BE|FE)-\d+)\s*[:—\-]\s*(.+)$/i,
  );
  if (m) {
    const jira = m[1].toUpperCase();
    const tcId = m[2].toUpperCase();
    const scenario = m[3].trim();
    return { jira, tcId, scenario, matched: true };
  }
  const jm = raw.match(/^\[([A-Z][A-Z0-9]*-\d+)\]\s*(.+)$/i);
  if (jm) {
    return {
      jira: jm[1].toUpperCase(),
      tcId: 'Unmapped (no TC-BE/TC-FE in title)',
      scenario: jm[2].trim(),
      matched: false,
    };
  }
  return { jira: '—', tcId: 'Unmapped', scenario: raw, matched: false };
}

function normalizeDescribePath(describePath) {
  const s = describePath.filter(Boolean).join(' › ');
  return s.replace(/\\/g, '/');
}

function buildMarkdown(report, jsonPath, cwdForDisplay) {
  const stats = report.stats || {};
  const sections = [];
  const jsonLabel = path.relative(cwdForDisplay, jsonPath) || path.basename(jsonPath);

  sections.push(`# Playwright detailed report`);
  sections.push('');
  sections.push(`- **Generated:** ${new Date().toISOString()}`);
  sections.push(`- **Source:** \`${escapeMd(jsonLabel)}\` (same folder as default output when run from EnergoTS)`);
  sections.push(`- **Run start (from JSON):** ${stats.startTime || ''}`);
  if (stats.duration != null) sections.push(`- **Total run duration (from JSON):** ${Math.round(stats.duration)} ms`);
  sections.push(
    `- **Summary (from JSON stats):** expected=${stats.expected ?? 0}, unexpected=${stats.unexpected ?? 0}, skipped=${stats.skipped ?? 0}, flaky=${stats.flaky ?? 0}`,
  );
  sections.push(`- **Skip setup/teardown files in this report:** ${skipSetup ? 'yes' : 'no'}`);
  sections.push('');
  sections.push(
    'Each section lists **which test case the automation title maps to** (Jira + TC id when present) and **what scenario the title describes**. Map to `test_cases/Backend|Frontend/*.md` when you need full preconditions and expected results.',
  );
  sections.push('');

  let index = 0;
  walkSuites(report.suites || [], [], (spec, describePath, suiteFile) => {
    const file = spec.file || suiteFile || '';
    if (shouldSkipFile(file)) return;
    const title = spec.title || '(untitled)';
    const line = spec.line != null ? spec.line : '';
    const location = line !== '' ? `${file}:${line}` : file;
    const describeNorm = normalizeDescribePath(describePath);

    for (const test of spec.tests || []) {
      index += 1;
      const status = lastResultStatus(test);
      const duration = formatDurationMs(test);
      const steps = collectStepTitles(test);
      const err =
        status === 'failed' || status === 'timedOut' ? collectErrorSnippet(test) : '';
      const tc = parseTestCaseFromTitle(title);

      sections.push(`## ${index}. ${escapeMd(title)}`);
      sections.push('');
      sections.push(`| Field | Value |`);
      sections.push(`| --- | --- |`);
      sections.push(`| **Jira (from title)** | ${escapeMd(tc.jira)} |`);
      sections.push(`| **Test case id (from title)** | ${escapeMd(tc.tcId)} |`);
      sections.push(`| **Scenario / objective (from title)** | ${escapeMd(tc.scenario)} |`);
      sections.push(`| **Playwright \`test()\` title** | ${escapeMd(title)} |`);
      sections.push(`| **Spec location** | \`${escapeMd(location.replace(/\\/g, '/'))}\` |`);
      sections.push(`| **Describe path** | ${escapeMd(describeNorm || '—')} |`);
      sections.push(`| **Result** | **${escapeMd(status)}** |`);
      sections.push(`| **Duration (sum of results)** | ${duration} ms |`);
      sections.push('');
      if (!tc.matched && tc.tcId.startsWith('Unmapped')) {
        sections.push(
          '> **Tip:** Name each Playwright test title like **`[JIRA-KEY] TC-BE-n: Short scenario`** (or **TC-FE-n**) so this report shows the exact TC row the test covers.',
        );
        sections.push('');
      }
      sections.push(`### Annotations`);
      sections.push('');
      sections.push(annotationsBlock(test));
      sections.push('');
      sections.push(`### Steps (\`test.step\` titles)`);
      sections.push('');
      sections.push(steps.length ? steps.map((t) => `- ${escapeMd(t)}`).join('\n') : '_None._');
      sections.push('');
      if (err) {
        sections.push(`### Failure (first line)`);
        sections.push('');
        sections.push('```');
        sections.push(err);
        sections.push('```');
        sections.push('');
      }
      sections.push('---');
      sections.push('');
    }
  });

  if (index === 0) sections.push('_No test entries (empty run or all filtered out)._\n');

  return sections.join('\n');
}

const cwd = process.cwd();
const jsonArg = process.argv[2] || path.join(cwd, 'playwright-report.json');
const outArg = process.argv[3] || path.join(cwd, 'playwright-report-detailed.md');
const jsonPath = path.resolve(jsonArg);
const outPath = path.resolve(outArg);

if (!fs.existsSync(jsonPath)) {
  console.error(`[detailed-report] JSON not found: ${jsonPath}`);
  process.exit(1);
}

let report;
try {
  report = JSON.parse(fs.readFileSync(jsonPath, 'utf-8'));
} catch (e) {
  console.error('[detailed-report] Failed to parse JSON:', e);
  process.exit(1);
}

fs.writeFileSync(outPath, buildMarkdown(report, jsonPath, cwd), 'utf-8');
console.log(`[detailed-report] Wrote: ${outPath}`);
