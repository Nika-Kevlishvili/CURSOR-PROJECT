export const CLASS_LABEL: Record<string, string> = {
  confirmed_intermittent: 'Intermittent',
  suspected_flaky: 'Suspected flaky',
  consistent_failure: 'Fails every time',
  environment_specific: 'One environment',
  probable_defect: 'Likely product defect',
  insufficient_evidence: 'Needs more runs',
  stable_pass: 'Passing',
};

export const CAUSE_LABEL: Record<string, string> = {
  timing_or_synchronization: 'Timing or synchronization',
  unstable_locator: 'Unstable locator',
  race_condition: 'Race condition',
  shared_state: 'Shared test data',
  fixture_or_cleanup: 'Fixture or cleanup',
  api_or_network: 'API or network',
  environment_or_infrastructure: 'Environment or infrastructure',
  authentication: 'Authentication',
  incorrect_assertion: 'Incorrect assertion',
  application_defect: 'Application defect',
  test_order_dependency: 'Test order',
  resource_contention: 'Browser or worker',
  unknown: 'Not enough to name a cause',
};

export function labelOf(map: Record<string, string>, value: string | null | undefined): string {
  if (!value) return 'Unavailable';
  return map[value] ?? value.replaceAll('_', ' ');
}

export function statusTone(status: string): string {
  if (status === 'passed' || status === 'VALIDATED') return 'good';
  if (status === 'flaky' || status === 'PROPOSED' || status === 'suspected_flaky' || status === 'confirmed_intermittent') return 'warn';
  if (status === 'failed' || status === 'timedOut' || status === 'interrupted' || status === 'VALIDATION_FAILED' || status === 'probable_defect') return 'bad';
  return 'neutral';
}
