#!/usr/bin/env node
'use strict';

/**
 * mergeReports.js
 *
 * ZIP-based multi-environment report merger.
 *
 * Each regression workflow calls:
 *   node src/backend/scripts/mergeReports.js <env>
 *
 * What it does:
 *   1. Copies playwright-report/index.html  → ~/merged-reports/<env>/index.html
 *      Copies playwright-report/data/       → ~/merged-reports/<env>/data/  (if exists)
 *   2. Updates ~/merged-reports/merged.html (the nav dashboard) with badge + timestamp
 *   3. ZIPs the entire ~/merged-reports/ folder → playwright-report/merged-reports.zip
 *      so SlackReporter can upload it.
 *
 * User workflow:
 *   Download merged-reports.zip → unzip → open merged.html → click env tab
 *   → "Open Report" opens <env>/index.html in a new tab (real file:// URL, works perfectly)
 *
 * Valid envs: dev | dev2 | test | custom
 */

const fs            = require('fs');
const path          = require('path');
const os            = require('os');
const { execSync }  = require('child_process');

// ── Config ────────────────────────────────────────────────────────────────────

const VALID_ENVS = ['dev', 'dev2', 'test', 'custom'];

const ROOT          = path.resolve(__dirname, '..');
const REPORT_DIR    = path.join(ROOT, 'playwright-report');
const INDEX_HTML    = path.join(REPORT_DIR, 'index.html');
const REPORT_DATA   = path.join(REPORT_DIR, 'data');
const TEMPLATE      = path.join(__dirname, 'merged-template.html');
const SHARED_DIR    = path.join(os.homedir(), 'merged-reports');
const SHARED_HTML   = path.join(SHARED_DIR, 'merged.html');
const OUTPUT_ZIP    = path.join(REPORT_DIR, 'merged-reports.zip');

// ── Helpers ───────────────────────────────────────────────────────────────────

function replaceBetween(html, marker, env, newContent) {
  const startTag = `<!--${marker}:${env}:START-->`;
  const endTag   = `<!--${marker}:${env}:END-->`;
  const startIdx = html.indexOf(startTag);
  const endIdx   = html.indexOf(endTag);
  if (startIdx === -1 || endIdx === -1) {
    console.warn(`  ⚠  Marker not found: ${startTag} / ${endTag} — skipping.`);
    return html;
  }
  return html.slice(0, startIdx + startTag.length) + newContent + html.slice(endIdx);
}

function isSkippedSpec(spec) {
  const tests = spec.tests || [];
  if (tests.length === 0) return false;
  return tests.every((test) => {
    if (test.status === 'skipped' || test.expectedStatus === 'skipped') return true;
    const results = test.results || [];
    return results.length > 0 && results.every((result) => result.status === 'skipped');
  });
}

function utcTimestamp() {
  return new Date().toLocaleString('en-GB', {
    timeZone: 'UTC', day: '2-digit', month: '2-digit',
    year: 'numeric', hour: '2-digit', minute: '2-digit',
  }) + ' UTC';
}

function copyDirSync(src, dest) {
  if (!fs.existsSync(src)) return;
  fs.mkdirSync(dest, { recursive: true });
  for (const entry of fs.readdirSync(src, { withFileTypes: true })) {
    const srcPath  = path.join(src, entry.name);
    const destPath = path.join(dest, entry.name);
    if (entry.isDirectory()) {
      copyDirSync(srcPath, destPath);
    } else {
      fs.copyFileSync(srcPath, destPath);
    }
  }
}

// ── Main ──────────────────────────────────────────────────────────────────────

function main() {
  const env = process.argv[2];

  if (!env) {
    console.error('Error: environment name is required.');
    console.error(`Usage: node src/backend/scripts/mergeReports.js <${VALID_ENVS.join('|')}>`);
    process.exit(1);
  }
  if (!VALID_ENVS.includes(env)) {
    console.error(`Error: unknown environment "${env}". Valid: ${VALID_ENVS.join(', ')}`);
    process.exit(1);
  }
  if (!fs.existsSync(INDEX_HTML)) {
    console.error(`Error: Playwright report not found at ${INDEX_HTML}`);
    process.exit(1);
  }

  // ── Safety guard ──────────────────────────────────────────────────────────
  // The script recursively clears ~/merged-reports/<env> on each run. Abort if
  // the home dir or SHARED_DIR ever resolves unexpectedly (e.g. empty/misconfigured
  // HOME/USERPROFILE), so a recursive delete can never target the wrong location.
  const home = os.homedir();
  const expectedSharedDir = path.join(home, 'merged-reports');
  if (!home || path.resolve(SHARED_DIR) !== path.resolve(expectedSharedDir)) {
    console.error('Refusing to run: merged-reports directory resolved unexpectedly.');
    console.error(`  homedir()  = "${home}"`);
    console.error(`  SHARED_DIR = "${SHARED_DIR}"`);
    process.exit(1);
  }

  // ── 1. Copy report files into env sub-directory ───────────────────────────
  const envDir = path.join(SHARED_DIR, env);
  if (fs.existsSync(envDir)) {
    fs.rmSync(envDir, { recursive: true, force: true });
    console.log(`Cleared previous report for env "${env}"`);
  }
  fs.mkdirSync(envDir, { recursive: true });

  fs.copyFileSync(INDEX_HTML, path.join(envDir, 'index.html'));
  console.log(`Copied index.html → ${envDir}/index.html`);

  if (fs.existsSync(REPORT_DATA)) {
    copyDirSync(REPORT_DATA, path.join(envDir, 'data'));
    console.log(`Copied data/ → ${envDir}/data/`);
  }

  // ── 2. Load / initialise merged.html dashboard ───────────────────────────
  const MAX_AGE_MS = 24 * 60 * 60 * 1000;
  let merged;

  // Template version marker — bump the number in merged-template.html whenever the
  // layout changes (tabs added/removed) so existing merged.html files get regenerated.
  const templateVersionTag = (fs.readFileSync(TEMPLATE, 'utf-8').match(/<!--TEMPLATE-VERSION:\d+-->/) || [null])[0];

  if (fs.existsSync(SHARED_HTML)) {
    const ageMs = Date.now() - fs.statSync(SHARED_HTML).mtimeMs;
    const candidate = fs.readFileSync(SHARED_HTML, 'utf-8');
    const isStale = ageMs >= MAX_AGE_MS
      || (templateVersionTag && !candidate.includes(templateVersionTag))
      || !candidate.includes('<!--CONTENT:dev:START-->')
      || !candidate.includes('<!--FAILURES:dev:START-->');
    if (isStale) {
      const reason = ageMs >= MAX_AGE_MS ? `${(ageMs / 3600000).toFixed(1)}h old` : 'outdated template version/markers';
      console.log(`merged.html has ${reason} — regenerating from template.`);
      merged = fs.readFileSync(TEMPLATE, 'utf-8');
    } else {
      merged = candidate;
      console.log(`Loaded existing merged.html (${(ageMs / 3600000).toFixed(1)}h old)`);
    }
  } else {
    merged = fs.readFileSync(TEMPLATE, 'utf-8');
    console.log('Initialised merged.html from template');
  }

  // ── 3. Inject "Open Report" card into the env panel ──────────────────────
  const reportJson = path.join(ROOT, 'playwright-report.json');
  let statusClass = 'badge-passed';
  let statusLabel = 'Passed';
  let passed = 0, failed = 0, skipped = 0, total = 0;
  const failedTests = [];
  const seenFailures = new Set();

  if (fs.existsSync(reportJson)) {
    try {
      const json = JSON.parse(fs.readFileSync(reportJson, 'utf-8'));

      function walkSuites(items, parentTitle) {
        for (const item of (items || [])) {
          const suiteTitle = parentTitle ? `${parentTitle} > ${item.title || ''}` : (item.title || '');
          for (const spec of (item.specs || [])) {
            const fullTitle = [suiteTitle, spec.title || ''].filter(Boolean).join(' > ');
            // Use the spec's OWN [REG-XXX] id (from the test title), NOT the first
            // match in the full title — that would be the suite's id (e.g. REG-55)
            // and would collapse all failures of a suite into a single summary row.
            const specReg = (spec.title || '').match(/REG-\d+/i);
            const regId   = specReg ? specReg[0].toUpperCase() : null;
            total++;
            // test.skip() still sets spec.ok, so it must not be counted as passed.
            if (isSkippedSpec(spec)) {
              skipped++;
            } else if (spec.ok) {
              passed++;
            } else {
              failed++;
              const key = regId || fullTitle;
              if (!seenFailures.has(key)) {
                seenFailures.add(key);
                failedTests.push({ regId, title: fullTitle });
              }
            }
          }
          walkSuites(item.suites, suiteTitle);
        }
      }
      walkSuites(json.suites, '');

      if (failed > 0) { statusClass = 'badge-failed'; statusLabel = `Failed (${failed})`; }
    } catch (e) {
      console.warn(`  ⚠  Could not parse playwright-report.json: ${e.message}`);
    }
  } else {
    statusClass = 'badge-failed';
    statusLabel = 'Setup failed';
  }

  const ran = passed + failed;
  const statsLine = total > 0
    ? `<div class="stats"><span class="stat-passed">✓ ${passed} passed</span>${failed > 0 ? `<span class="stat-failed">✗ ${failed} failed</span>` : ''}<span class="stat-skipped">⏭ ${skipped} skipped</span><span class="stat-total">${ran} ran</span></div>`
    : '';

  const card = `
<div class="report-card">
  ${statsLine}
  <button class="open-btn" onclick="window.open('${env}/index.html')">Open ${env.toUpperCase()} Report ↗</button>
</div>`;

  merged = replaceBetween(merged, 'CONTENT', env, card);

  // ── 4. Update failures data ───────────────────────────────────────────────
  merged = replaceBetween(merged, 'FAILURES', env, JSON.stringify(failedTests));

  // ── 5. Update badge + timestamp ───────────────────────────────────────────
  merged = replaceBetween(merged, 'TIMESTAMP', env, utcTimestamp());
  const badgeHtml = `<span class="badge ${statusClass}" data-badge="${env}">${statusLabel}</span>`;
  merged = replaceBetween(merged, 'BADGE', env, badgeHtml);

  // ── 5. Save merged.html ───────────────────────────────────────────────────
  fs.writeFileSync(SHARED_HTML, merged);
  console.log(`Saved merged.html → ${SHARED_HTML}`);

  // ── 5b. Persist per-env stats for combined Slack message ─────────────────
  const statsFile = path.join(SHARED_DIR, 'stats.json');
  let allStats = {};
  if (fs.existsSync(statsFile)) {
    try { allStats = JSON.parse(fs.readFileSync(statsFile, 'utf-8')); } catch (_) {}
  }
  // Purge stale entries for envs that are no longer part of the regression pipeline
  for (const staleEnv of Object.keys(allStats)) {
    if (!VALID_ENVS.includes(staleEnv)) {
      delete allStats[staleEnv];
      console.log(`Removed stale stats.json entry for env "${staleEnv}"`);
    }
  }
  allStats[env] = { passed, failed, skipped, total, timestamp: utcTimestamp() };
  fs.writeFileSync(statsFile, JSON.stringify(allStats, null, 2));
  console.log(`Updated stats.json for ${env}: ${passed} passed, ${failed} failed, ${skipped} skipped, ${passed + failed} ran`);

  // ── 6. ZIP the entire shared dir → playwright-report/merged-reports.zip ──
  // Windows 10/11: tar (bsdtar) natively supports ZIP via the -a flag.
  // Linux/macOS:   zip is standard.
  if (fs.existsSync(OUTPUT_ZIP)) fs.unlinkSync(OUTPUT_ZIP);

  try {
    // Windows: bsdtar (Git for Windows) misinterprets drive letters as remote
    // hosts, so use PowerShell's Compress-Archive which handles Windows paths natively.
    // Linux/macOS: zip is standard.
    const cmd = process.platform === 'win32'
      ? `powershell.exe -NoProfile -Command "Compress-Archive -Path '${SHARED_DIR}\\*' -DestinationPath '${OUTPUT_ZIP}' -Force"`
      : `cd "${SHARED_DIR}" && zip -r "${OUTPUT_ZIP}" .`;
    execSync(cmd, { stdio: 'pipe' });
    const sizeMB = (fs.statSync(OUTPUT_ZIP).size / 1024 / 1024).toFixed(1);
    console.log(`Created merged-reports.zip (${sizeMB} MB) → ${OUTPUT_ZIP}`);
  } catch (e) {
    console.error('Failed to create zip:', e.message);
    process.exit(1);
  }
}

main();
