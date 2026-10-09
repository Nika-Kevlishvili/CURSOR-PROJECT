#!/usr/bin/env node
/**
 * Validates that a detailed Playwright report follows the canonical style.
 * Usage:
 *   node ../config/playwright/validate-detailed-report-format.mjs [path/to/report.md]
 * Default:
 *   cwd/playwright-report-detailed.md
 */

import * as fs from 'fs';
import * as path from 'path';

const cwd = process.cwd();
const reportArg = process.argv[2] || path.join(cwd, 'playwright-report-detailed.md');
const reportPath = path.resolve(reportArg);

if (!fs.existsSync(reportPath)) {
  console.error(`[report-validate] File not found: ${reportPath}`);
  process.exit(1);
}

const content = fs.readFileSync(reportPath, 'utf-8');
const errors = [];

function assertContains(label, test) {
  if (!test(content)) errors.push(`Missing required section: ${label}`);
}

assertContains('title', (s) => /^# .+ Detailed Test Report/m.test(s));
assertContains('Run context', (s) => /^Run context$/m.test(s));
assertContains('Summary', (s) => /^Summary$/m.test(s));
assertContains('Test cases', (s) => /^Test cases$/m.test(s));

const caseBlockRegex =
  /^(TC-(?:BE|FE)-\d+|Test-\d+)\n- Status: (?:PASSED|FAILED|SKIPPED|UNKNOWN)\n- Expected Result: .+\n- Actual Result: .+\n- Portal data links:\n(?:  - .+\n)+/gm;
const blocks = content.match(caseBlockRegex) || [];
if (!blocks.length) {
  errors.push('No valid test case blocks found in canonical format.');
}

if (errors.length) {
  console.error('[report-validate] FAILED');
  for (const err of errors) console.error(`- ${err}`);
  process.exit(2);
}

console.log(`[report-validate] OK: ${path.relative(cwd, reportPath)}`);
