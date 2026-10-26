export const ENVIRONMENTS = ['dev', 'dev2', 'test'] as const;
export type EnvironmentName = (typeof ENVIRONMENTS)[number];

export const FIX_STATES = [
  'PROPOSED',
  'APPROVED',
  'APPLIED',
  'VALIDATED',
  'REJECTED',
  'BLOCKED',
  'VALIDATION_FAILED',
  'STALE',
] as const;
export type FixState = (typeof FIX_STATES)[number];

export const CLASSIFICATIONS = [
  'confirmed_intermittent',
  'suspected_flaky',
  'consistent_failure',
  'environment_specific',
  'probable_defect',
  'insufficient_evidence',
  'stable_pass',
] as const;
export type Classification = (typeof CLASSIFICATIONS)[number];

export const ROOT_CAUSES = [
  'timing_or_synchronization',
  'unstable_locator',
  'race_condition',
  'shared_state',
  'fixture_or_cleanup',
  'api_or_network',
  'environment_or_infrastructure',
  'authentication',
  'incorrect_assertion',
  'application_defect',
  'test_order_dependency',
  'resource_contention',
  'unknown',
] as const;
export type RootCause = (typeof ROOT_CAUSES)[number];

export type Confidence = 'high' | 'medium' | 'low';

export type DashboardFilters = {
  environment?: EnvironmentName;
  from?: string;
  to?: string;
  status?: string;
  rootCause?: string;
  fixStatus?: string;
};

export type DashboardCounts = {
  importedReports: number;
  testsAnalyzed: number;
  failedTests: number;
  suspectedFlaky: number;
  confirmedIntermittent: number;
  environmentSpecific: number;
  probableDefects: number;
  insufficientEvidence: number;
  fixesAwaitingApproval: number;
  approvedAwaitingValidation: number;
  regressedAfterFix: number;
  diagnosticRecommendations: number;
};

export type Ratio = {
  numerator: number;
  denominator: number;
  rate: number | null;
};
