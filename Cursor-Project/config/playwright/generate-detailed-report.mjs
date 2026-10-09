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

function decodeJsonAttachmentBody(body) {
  if (!body) return null;
  try {
    const decoded = Buffer.from(String(body), 'base64').toString('utf-8');
    return JSON.parse(decoded);
  } catch {
    return null;
  }
}

function decodeTextAttachmentBody(body) {
  if (!body) return '';
  try {
    return Buffer.from(String(body), 'base64').toString('utf-8');
  } catch {
    return '';
  }
}

function collectLinksFromObject(obj) {
  if (!obj || typeof obj !== 'object' || Array.isArray(obj)) return [];
  const out = [];
  for (const [entity, rawUrls] of Object.entries(obj)) {
    if (!Array.isArray(rawUrls)) continue;
    const entityName = String(entity || 'entity');
    for (const rawUrl of rawUrls) {
      if (typeof rawUrl !== 'string') continue;
      const url = rawUrl.trim();
      if (!/^https?:\/\//i.test(url)) continue;
      out.push({ entity: entityName, url });
    }
  }
  return out;
}

function collectCreatedEntityLinks(test) {
  const out = [];
  const seen = new Set();
  for (const r of test.results || []) {
    for (const a of r.attachments || []) {
      let candidates = [];
      if (a?.contentType === 'application/json') {
        const parsed = decodeJsonAttachmentBody(a?.body);
        // Legacy shape: { links: { entity: [url] } }
        candidates = [...candidates, ...collectLinksFromObject(parsed?.links)];
        // New shape used by tests: { entity: [url] } on "portal URLs (JSON)" attachments.
        candidates = [...candidates, ...collectLinksFromObject(parsed)];
      }
      if (String(a?.contentType || '').startsWith('text/plain')) {
        // Fallback for plain-text attachments containing direct URLs.
        const text = decodeTextAttachmentBody(a?.body);
        const urls = text.match(/https?:\/\/[^\s)]+/gi) || [];
        candidates = [...candidates, ...urls.map((url) => ({ entity: 'link', url: url.trim() }))];
      }

      for (const item of candidates) {
        const key = `${item.entity}::${item.url}`;
        if (seen.has(key)) continue;
        seen.add(key);
        out.push(item);
      }
    }
  }
  // Remove generic "link" entries when the same URL exists under a concrete entity.
  const concreteByUrl = new Set(
    out
      .filter((x) => String(x.entity).toLowerCase() !== 'link')
      .map((x) => x.url),
  );
  return out.filter((x) => {
    if (String(x.entity).toLowerCase() !== 'link') return true;
    return !concreteByUrl.has(x.url);
  });
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

function toStatusLabel(status) {
  const s = String(status || '').toLowerCase();
  if (s === 'passed') return 'PASSED';
  if (s === 'skipped') return 'SKIPPED';
  if (s === 'timedout') return 'FAILED';
  if (s === 'failed') return 'FAILED';
  return String(status || 'UNKNOWN').toUpperCase();
}

function collectAllTests(report) {
  const rows = [];
  walkSuites(report.suites || [], [], (spec, describePath, suiteFile) => {
    const file = spec.file || suiteFile || '';
    if (shouldSkipFile(file)) return;
    const title = spec.title || '(untitled)';
    const line = spec.line != null ? spec.line : '';
    const location = line !== '' ? `${file}:${line}` : file;
    const describeNorm = normalizeDescribePath(describePath);
    for (const test of spec.tests || []) {
      rows.push({
        title,
        test,
        location,
        describeNorm,
        tc: parseTestCaseFromTitle(title),
        status: lastResultStatus(test),
        duration: formatDurationMs(test),
        entityLinks: collectCreatedEntityLinks(test),
        err: collectErrorSnippet(test),
      });
    }
  });
  return rows;
}

function buildMarkdown(report, jsonPath, cwdForDisplay) {
  const stats = report.stats || {};
  const sections = [];
  const allTests = collectAllTests(report);
  const runDate = stats.startTime ? new Date(stats.startTime) : new Date();
  const dateStr = Number.isNaN(runDate.getTime()) ? new Date().toISOString().slice(0, 10) : runDate.toISOString().slice(0, 10);
  const jiraSet = new Set(allTests.map((t) => t.tc.jira).filter((x) => x && x !== '—'));
  const jiraKey = jiraSet.size === 1 ? Array.from(jiraSet)[0] : 'N/A';
  const tcIds = allTests
    .map((t) => t.tc.tcId)
    .filter((x) => /^TC-(BE|FE)-\d+$/i.test(String(x)))
    .map((x) => String(x).toUpperCase());
  const specSet = new Set(allTests.map((t) => t.location.split(':')[0].replace(/\\/g, '/')).filter(Boolean));
  const jsonLabel = path.relative(cwdForDisplay, jsonPath) || path.basename(jsonPath);

  const passed = allTests.filter((t) => String(t.status).toLowerCase() === 'passed').length;
  const failed = allTests.filter((t) => ['failed', 'timedout'].includes(String(t.status).toLowerCase())).length;
  const skipped = allTests.filter((t) => String(t.status).toLowerCase() === 'skipped').length;

  const reportTitle = jiraKey !== 'N/A'
    ? `# ${jiraKey} Detailed Test Report`
    : '# Detailed Test Report';

  sections.push(reportTitle);
  sections.push('');
  sections.push('Run context');
  sections.push(`- Jira key: ${jiraKey}`);
  sections.push(`- Date: ${dateStr}`);
  sections.push('- Branch: cursor');
  if (specSet.size === 1) {
    sections.push(`- Spec path: \`${escapeMd(Array.from(specSet)[0])}\``);
  } else {
    sections.push('- Spec path(s):');
    for (const specPath of specSet) sections.push(`  - \`${escapeMd(specPath)}\``);
  }
  sections.push(`- Source JSON: \`${escapeMd(jsonLabel)}\``);
  sections.push('- Command: N/A (read from generated Playwright JSON report)');
  sections.push('');
  sections.push('Summary');
  sections.push(`- Total in scope: ${allTests.length}`);
  sections.push(`- Passed: ${passed}`);
  sections.push(`- Failed: ${failed}`);
  sections.push(`- Skipped: ${skipped}`);
  if (stats.duration != null) {
    sections.push(`- Duration: ~${(stats.duration / 1000).toFixed(1)}s`);
  }
  if (tcIds.length) {
    sections.push(`- Test case IDs: ${tcIds.join(', ')}`);
  }
  sections.push('');
  sections.push('Test cases');
  sections.push('');

  allTests.forEach((row, idx) => {
    const tcLabel = /^TC-(BE|FE)-\d+$/i.test(String(row.tc.tcId))
      ? String(row.tc.tcId).toUpperCase()
      : `Test-${idx + 1}`;
    sections.push(tcLabel);
    sections.push(`- Status: ${toStatusLabel(row.status)}`);
    sections.push(`- Expected Result: ${row.tc.scenario && row.tc.scenario !== '—' ? row.tc.scenario : row.title}`);
    if (String(row.status).toLowerCase() === 'passed') {
      sections.push('- Actual Result: Test passed with expected behavior.');
    } else if (String(row.status).toLowerCase() === 'skipped') {
      sections.push('- Actual Result: Test was skipped.');
    } else {
      sections.push(`- Actual Result: Test failed. ${row.err || 'See Playwright report for details.'}`);
    }
    sections.push('- Portal data links:');
    if (row.entityLinks.length) {
      row.entityLinks.forEach((x) => {
        sections.push(`  - ${x.entity}: ${x.url}`);
      });
    } else {
      sections.push('  - not available');
    }
    sections.push('');
  });

  if (!allTests.length) sections.push('_No test entries (empty run or all filtered out)._\n');

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
