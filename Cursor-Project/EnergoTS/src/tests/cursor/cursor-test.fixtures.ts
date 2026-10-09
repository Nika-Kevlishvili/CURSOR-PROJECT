/**
 * Shared entry point for new tests/cursor specs.
 * Re-exports baseFixture, adds TestRunSummary, and logs API entity links after each test.
 */
import { test as base, expect } from '../../backend/fixtures/baseFixture';
import type { baseFixture } from '../../backend/fixtures/baseFixture';
import reportGenerator from '../../backend/utils/generateReport';
import { createTestRunSummary, type TestRunSummary } from './shared/test-run-summary.fixtures';

export type { baseFixture };
export type { TestRunSummary };

type CursorTestFixtures = {
  TestRunSummary: TestRunSummary;
};

export const test = base.extend<CursorTestFixtures>({
  TestRunSummary: async ({}, use) => {
    await use(createTestRunSummary());
  },
});

test.afterEach(async ({ Responses }, testInfo) => {
  const links = reportGenerator.setLinksToResponses(Responses);
  console.log('[API responses]', links);
  await testInfo.attach('[API responses]', {
    body: JSON.stringify(links, null, 2),
    contentType: 'application/json',
  });
});

export { expect };
