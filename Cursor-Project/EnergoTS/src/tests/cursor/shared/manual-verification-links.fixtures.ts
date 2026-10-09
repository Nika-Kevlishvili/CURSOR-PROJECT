/**
 * Portal link builders and unified test-run summary attach for cursor specs.
 */
import { test } from '../cursor-test.fixtures';
import type { TestRunSummary } from './test-run-summary.fixtures';
import reportGenerator from '../../../backend/utils/generateReport';
import { configuredBaseURL } from '../../../backend/fixtures/utils/baseUrl';

function resolveFrontendUrl(): string | null {
  const baseUrl = (configuredBaseURL || process.env.BASE_URL || '').replace(/\/$/, '');

  if (baseUrl === 'https://devapps.energo-pro.bg/backend/phoenix1-dev') {
    return 'https://devapps.energo-pro.bg/app/phoenix1-dev/';
  }
  if (baseUrl === 'http://10.236.20.31:8091') {
    return 'http://10.236.20.31:8080/';
  }
  if (baseUrl === 'http://10.236.20.81:8091') {
    return 'http://10.236.20.81:8080/';
  }
  if (baseUrl === 'http://10.236.20.81:8094') {
    return 'http://10.236.20.31:8082/';
  }
  if (baseUrl === 'https://testapps.energo-pro.bg/backend/phoenix-epres') {
    return 'https://testapps.energo-pro.bg/app/phoenix-epres/';
  }
  if (baseUrl === 'https://devapps.energo-pro.bg/backend/phoenix2-dev') {
    return 'https://devapps.energo-pro.bg/app/phoenix2-dev/';
  }
  if (baseUrl === 'http://10.236.20.81:8095') {
    return 'https://testapps.energo-pro.bg/app/phoenix-test2/';
  }
  return null;
}

export function buildProcessPreviewLink(processId: number | undefined): string | undefined {
  if (!processId) {
    return undefined;
  }
  const front = resolveFrontendUrl();
  if (!front) {
    return undefined;
  }
  return `${front.replace(/\/$/, '')}/processes/preview?id=${processId}`;
}

export function buildProductContractTabLinks(
  contractId: number | undefined,
): Record<string, string[]> {
  if (!contractId) {
    return {};
  }
  const front = resolveFrontendUrl();
  if (!front) {
    return {};
  }
  const prefix = `${front.replace(/\/$/, '')}/energy-product-contracts/preview`;
  return {
    productContractTabs: [
      `${prefix}/basic-parameters?id=${contractId}`,
      `${prefix}/additional-parameters?id=${contractId}`,
      `${prefix}/product-parameters?id=${contractId}`,
      `${prefix}/point-of-delivery?id=${contractId}`,
    ],
  };
}

export type FinalizeTestRunSummaryOptions = {
  jiraKey: string;
  relevantEntityKeys?: string[];
  extraLinks?: Record<string, string[]>;
  snapshot?: Record<string, unknown>;
};

export function finalizeTestRunSummary(
  summary: TestRunSummary,
  responses: unknown,
  options: FinalizeTestRunSummaryOptions,
): void {
  const allLinks = reportGenerator.setLinksToResponses(responses);
  const filtered: Record<string, string[]> = {};
  for (const key of options.relevantEntityKeys ?? []) {
    if (allLinks[key]?.length) {
      filtered[key] = allLinks[key];
    }
  }
  if (options.extraLinks) {
    for (const [key, urls] of Object.entries(options.extraLinks)) {
      if (urls?.length) {
        filtered[key] = [...(filtered[key] ?? []), ...urls];
      }
    }
  }

  const info = test.info();
  const lines: string[] = [
    `Test: ${info.title}`,
    `Jira: ${options.jiraKey}`,
    '',
    'Payloads:',
    JSON.stringify(summary.payloads, null, 2),
    '',
    'Checks:',
  ];
  for (const check of summary.checks) {
    lines.push(
      `- [${check.passed ? 'PASS' : 'FAIL'}] ${check.check}`,
      `  expected: ${check.expectedResult}`,
      `  actual: ${check.actualResult}`,
    );
  }
  lines.push('', 'Portal links:');
  for (const [key, urls] of Object.entries(filtered)) {
    lines.push(`${key}:`);
    for (const url of urls) {
      lines.push(`  ${url}`);
    }
  }
  if (options.snapshot) {
    lines.push('', 'Snapshot:', JSON.stringify(options.snapshot, null, 2));
  }

  const body = lines.join('\n');
  console.log(`[Test run summary]\n${body}`);
  void info.attach('test-run-summary', { body, contentType: 'text/plain' });
  void info.attach('test-run-summary.json', {
    body: JSON.stringify(
      {
        jiraKey: options.jiraKey,
        title: info.title,
        payloads: summary.payloads,
        checks: summary.checks,
        links: filtered,
        snapshot: options.snapshot ?? {},
      },
      null,
      2,
    ),
    contentType: 'application/json',
  });
}
