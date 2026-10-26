import type { AttemptStatus } from '../parse/model.js';
import type { Classification, Confidence } from '../../shared/types.js';

export type HistoryAttempt = {
  retryIndex: number | null;
  status: AttemptStatus;
};

export type Observation = {
  environment: string;
  reportId: string;
  attempts: HistoryAttempt[];
};

export type Verdict = 'pass' | 'fail' | 'intermittent' | 'unknown';

export type ClassificationResult = {
  classification: Classification;
  secondary: Classification[];
  confidence: Confidence;
  notes: string[];
};

export function executionVerdict(attempts: HistoryAttempt[]): Verdict {
  const meaningful = attempts.filter((attempt) => attempt.status === 'passed' || attempt.status === 'failed' || attempt.status === 'timedOut' || attempt.status === 'interrupted');
  if (meaningful.length === 0) return 'unknown';
  const anyPass = meaningful.some((attempt) => attempt.status === 'passed');
  const anyFail = meaningful.some((attempt) => attempt.status !== 'passed');
  if (anyPass && anyFail) return 'intermittent';
  if (anyPass) return 'pass';
  return 'fail';
}

export function classifyHistory(observations: Observation[], defectSignal: boolean): ClassificationResult {
  const conclusive = observations
    .map((observation) => ({ ...observation, verdict: executionVerdict(observation.attempts) }))
    .filter((observation) => observation.verdict !== 'unknown');

  if (conclusive.length === 0) {
    return {
      classification: 'insufficient_evidence',
      secondary: [],
      confidence: 'low',
      notes: ['No pass or fail attempts were found. Skipped or missing attempts are not treated as passes.'],
    };
  }

  const passes = conclusive.filter((item) => item.verdict === 'pass');
  const fails = conclusive.filter((item) => item.verdict === 'fail');
  const intermittent = conclusive.filter((item) => item.verdict === 'intermittent');
  const notes: string[] = [];

  if (passes.length === conclusive.length) {
    return { classification: 'stable_pass', secondary: [], confidence: 'high', notes: ['Every observed execution passed without a failed attempt.'] };
  }

  const byEnv = new Map<string, Verdict[]>();
  for (const item of conclusive) {
    const list = byEnv.get(item.environment) ?? [];
    list.push(item.verdict);
    byEnv.set(item.environment, list);
  }
  const cleanEnvs = [...byEnv.entries()].filter(([, verdicts]) => verdicts.every((verdict) => verdict === 'pass'));
  const badEnvs = [...byEnv.entries()].filter(([, verdicts]) => verdicts.some((verdict) => verdict !== 'pass'));
  if (badEnvs.length === 1 && cleanEnvs.length >= 1) {
    const secondary: Classification[] = badEnvs[0]?.[1].some((verdict) => verdict === 'intermittent') ? ['confirmed_intermittent'] : [];
    const brokenCount = badEnvs[0]?.[1].length ?? 0;
    notes.push(`Failures are confined to ${badEnvs[0]?.[0]}. ${cleanEnvs.map(([name]) => name).join(', ')} has only passing results. Missing environments are not counted as passes.`);
    return {
      classification: 'environment_specific',
      secondary,
      confidence: brokenCount >= 2 ? 'medium' : 'low',
      notes,
    };
  }

  if (fails.length >= 2 && passes.length === 0 && intermittent.length === 0) {
    if (defectSignal) {
      return {
        classification: 'probable_defect',
        secondary: ['consistent_failure'],
        confidence: 'medium',
        notes: ['The test failed in every observed execution with an assertion or response mismatch. That is not evidence the test is flaky.'],
      };
    }
    return {
      classification: 'consistent_failure',
      secondary: [],
      confidence: 'medium',
      notes: [`The test failed in ${fails.length} executions and has no passing result.`],
    };
  }

  if (intermittent.length > 0 || (passes.length > 0 && fails.length > 0 && conclusive.length >= 3)) {
    notes.push(intermittent.length > 0
      ? 'At least one execution failed and then passed on a later attempt.'
      : 'Pass and fail outcomes both appear across three or more executions.');
    return { classification: 'confirmed_intermittent', secondary: [], confidence: intermittent.length > 0 ? 'medium' : 'medium', notes };
  }

  if (passes.length > 0 && fails.length > 0) {
    return {
      classification: 'suspected_flaky',
      secondary: [],
      confidence: 'low',
      notes: ['The test both passed and failed, but only two conclusive executions were found. That is not enough to call the intermittency confirmed.'],
    };
  }

  return {
    classification: 'insufficient_evidence',
    secondary: [],
    confidence: 'low',
    notes: ['A single failure does not show whether the test is flaky, environment-specific, or a consistent defect.'],
  };
}
