import type { HistoryAttempt } from './classify.js';
import type { Ratio } from '../../shared/types.js';

const FAILED = new Set(['failed', 'timedOut', 'interrupted']);

export function failureFrequency(attempts: HistoryAttempt[]): Ratio {
  const counted = attempts.filter((attempt) => attempt.status === 'passed' || FAILED.has(attempt.status));
  const numerator = counted.filter((attempt) => FAILED.has(attempt.status)).length;
  return {
    numerator,
    denominator: counted.length,
    rate: counted.length === 0 ? null : numerator / counted.length,
  };
}

export function retryRecovery(executions: { attempts: HistoryAttempt[] }[]): Ratio {
  let numerator = 0;
  let denominator = 0;
  for (const execution of executions) {
    const ordered = [...execution.attempts].sort((a, b) => (a.retryIndex ?? 0) - (b.retryIndex ?? 0));
    const first = ordered[0];
    if (!first || !FAILED.has(first.status) || ordered.length < 2) continue;
    denominator += 1;
    if (ordered.slice(1).some((attempt) => attempt.status === 'passed')) numerator += 1;
  }
  return {
    numerator,
    denominator,
    rate: denominator === 0 ? null : numerator / denominator,
  };
}

export function formatRatio(ratio: Ratio): string {
  if (ratio.denominator === 0) return 'unavailable (0 observed attempts)';
  const percent = ratio.rate === null ? 'n/a' : `${Math.round(ratio.rate * 1000) / 10}%`;
  return `${percent} (${ratio.numerator}/${ratio.denominator})`;
}
