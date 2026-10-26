import { createTwoFilesPatch } from 'diff';
import type { Classification, Confidence, RootCause } from '../../shared/types.js';
import { looksLikeProductDefect } from './rootCause.js';
import { assertPatchPolicy } from '../patch/policy.js';

export type PatchFile = {
  path: string;
  baseHash: string;
  oldText: string;
  newText: string;
};

export type ProposalDraft = {
  kind: 'patch' | 'diagnostic';
  summary: string;
  rationale: string;
  impact: string;
  risks: string;
  why: string;
  confidence: Confidence;
  validationPlan: string;
  files: PatchFile[];
  diff: string;
};

function uniqueSlice(full: string, lines: string[], index: number, replacement: string, newline: string): { oldText: string; newText: string } | null {
  let radius = 0;
  while (radius <= 6) {
    const start = Math.max(0, index - radius);
    const end = Math.min(lines.length, index + radius + 1);
    const oldText = lines.slice(start, end).join(newline);
    const nextLines = lines.slice(start, end).map((line, offset) => (start + offset === index ? replacement : line));
    const newText = nextLines.join(newline);
    const first = full.indexOf(oldText);
    if (first >= 0 && full.indexOf(oldText, first + oldText.length) < 0 && oldText !== newText) {
      return { oldText, newText };
    }
    radius += 1;
  }
  return null;
}

function missingAwait(line: string): string | null {
  const trimmed = line.trim();
  if (!trimmed || trimmed.startsWith('await ') || trimmed.startsWith('return ') || trimmed.startsWith('//')) return null;
  const assertion = /^expect\(/.test(trimmed) && /\.(to[A-Z]|not\.|CheckResponse)/.test(trimmed);
  const action = /\.(click|dblclick|fill|press|check|uncheck|selectOption|goto|hover|type|setInputFiles|tap)\s*\(/.test(trimmed);
  if (assertion || action) {
    const indent = line.match(/^\s*/)?.[0] ?? '';
    return `${indent}await ${trimmed}`;
  }
  return null;
}

function isSleep(line: string): boolean {
  return /waitForTimeout\s*\(/.test(line) || /new Promise\(\s*\(?\w*\)?\s*=>\s*setTimeout/.test(line);
}

function isAutoWait(line: string): boolean {
  return /expect\(|getBy(?:Role|Text|Label|Placeholder|TestId)|locator\(|waitFor\(/.test(line);
}

export function buildProposalDraft(input: {
  relativePath: string | null;
  sourceText: string | null;
  line: number | null;
  baseHash: string | null;
  errorText: string;
  classification: Classification;
  categories: RootCause[];
  missing: string[];
  nextAction: string;
}): ProposalDraft {
  const diagnostic = (summary: string, confidence: Confidence = 'low'): ProposalDraft => ({
    kind: 'diagnostic',
    summary,
    rationale: input.nextAction,
    impact: 'No source file is changed.',
    risks: 'Acting on a diagnostic as if it were a fix would hide the missing evidence.',
    why: input.missing.join(' ') || 'A reliable source change could not be derived from the report and the current file.',
    confidence,
    validationPlan: 'Import another execution or collect the missing file, error, and trace before proposing a patch.',
    files: [],
    diff: '',
  });

  if (!input.relativePath || !input.sourceText || !input.baseHash) {
    return diagnostic('No source patch. The failing test could not be mapped to a file under the automation root.');
  }
  if (input.classification === 'probable_defect' || input.categories.includes('application_defect')) {
    return diagnostic('No source patch. The failure looks like an application or assertion defect, so the test was not changed to match it.', 'medium');
  }
  if (input.classification === 'environment_specific') {
    return diagnostic('No source patch. The failure is confined to one environment that already has a passing counterpart.', 'medium');
  }
  if (input.classification === 'consistent_failure') {
    return diagnostic('No source patch. The test failed consistently, so a retry, timeout, or weaker assertion would hide the failure.', 'medium');
  }

  const newline = input.sourceText.includes('\r\n') ? '\r\n' : '\n';
  const lines = input.sourceText.split(/\r?\n/);
  const index = input.line && input.line >= 1 && input.line <= lines.length ? input.line - 1 : -1;
  const assertionRan = /received\s*:/i.test(input.errorText) && /expected\s*:/i.test(input.errorText);
  if (index >= 0 && !assertionRan) {
    const awaited = missingAwait(lines[index] ?? '');
    if (awaited) {
      const slice = uniqueSlice(input.sourceText, lines, index, awaited, newline);
      if (slice) {
        assertPatchPolicy(slice.oldText, slice.newText);
        const file: PatchFile = { path: input.relativePath, baseHash: input.baseHash, ...slice };
        return {
          kind: 'patch',
          summary: `Await the assertion in ${input.relativePath}.`,
          rationale: 'The failing line calls a Playwright assertion without await, so the test does not wait for that assertion.',
          impact: 'The test waits for the assertion on this line. The expected value is unchanged.',
          risks: 'If the line was intentionally left floating, the test will now fail when the assertion fails instead of continuing.',
          why: 'Playwright web-first assertions return promises. Without await, a failure or a pending condition is not part of the test result.',
          confidence: 'high',
          validationPlan: `Run this test in the environment that produced the report. One pass does not prove an intermittent failure is gone; use repeated validation if the history was intermittent.`,
          files: [file],
          diff: createTwoFilesPatch(input.relativePath, input.relativePath, input.sourceText, applySlice(input.sourceText, slice.oldText, slice.newText), '', '', { context: 3 }),
        };
      }
    }
  }

  const intermittent = input.classification === 'confirmed_intermittent' || input.classification === 'suspected_flaky';
  const timeout = /timeout|timed out/i.test(input.errorText);
  if (intermittent && timeout && index >= 0 && isAutoWait(lines[index] ?? '')) {
    for (let cursor = index - 1; cursor >= Math.max(0, index - 5); cursor -= 1) {
      const candidate = lines[cursor] ?? '';
      if (!candidate.trim()) continue;
      if (!isSleep(candidate)) break;
      const slice = removeIndexedLine(input.sourceText, lines, cursor, newline);
      if (!slice) break;
      try {
        assertPatchPolicy(slice.oldText, slice.newText);
      } catch {
        break;
      }
      const file: PatchFile = { path: input.relativePath, baseHash: input.baseHash, oldText: slice.oldText, newText: slice.newText };
      const nextSource = applySlice(input.sourceText, slice.oldText, slice.newText);
      return {
        kind: 'patch',
        summary: `Remove the fixed sleep before the auto-waiting call in ${input.relativePath}.`,
        rationale: 'The failing run already reached an auto-waiting Playwright call. A preceding waitForTimeout or setTimeout does not wait for the application state and showed intermittent timeout behavior.',
        impact: 'The fixed delay is removed. The following assertion or locator wait remains.',
        risks: 'If the delay was hiding a product race, the test can still fail. This change does not increase a timeout and does not add a retry.',
        why: 'The history shows inconsistent results, and the sleep is not tied to a response or UI state. The next call already waits.',
        confidence: 'medium',
        validationPlan: 'Run the affected test repeatedly in the same environment. Record the pass and fail counts. Do not treat a single pass as proof the race is gone.',
        files: [file],
        diff: createTwoFilesPatch(input.relativePath, input.relativePath, input.sourceText, nextSource, '', '', { context: 3 }),
      };
    }
  }

  if (input.classification === 'insufficient_evidence' && looksLikeProductDefect(input.errorText)) {
    return diagnostic('No test change. The assertion failed, so the value the test received does not match what it requires. Do not edit the assertion to force a pass.', 'medium');
  }
  if (input.classification === 'insufficient_evidence') {
    return diagnostic('No source patch. One failure is not enough to confirm a flaky test or a safe code change.');
  }
  return diagnostic('No source patch. The evidence does not identify a single safe edit.');
}

function applySlice(source: string, oldText: string, newText: string): string {
  const index = source.indexOf(oldText);
  if (index < 0) return source;
  return source.slice(0, index) + newText + source.slice(index + oldText.length);
}

function removeIndexedLine(full: string, lines: string[], index: number, newline: string): { oldText: string; newText: string } | null {
  let radius = 0;
  while (radius <= 6) {
    const start = Math.max(0, index - radius);
    const end = Math.min(lines.length, index + radius + 1);
    const oldLines = lines.slice(start, end);
    const newLines = oldLines.filter((_, offset) => start + offset !== index);
    const oldText = oldLines.join(newline);
    const newText = newLines.join(newline);
    const first = full.indexOf(oldText);
    if (oldText !== newText && first >= 0 && full.indexOf(oldText, first + oldText.length) < 0) {
      return { oldText, newText };
    }
    radius += 1;
  }
  return null;
}
