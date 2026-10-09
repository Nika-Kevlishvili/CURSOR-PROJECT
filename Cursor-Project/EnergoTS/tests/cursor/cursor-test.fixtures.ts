/**

 * Shared test entry for new tests/cursor/*.spec.ts files.

 * Re-exports baseFixture + TestRunSummary + unified log after each test.

 */

import { test as baseTest, expect } from '../../fixtures/baseFixture';

import reportGenerator from '../../utils/generateReport';

import {
  TestRunSummaryCollector,
  rewritePortalLinksForDisplay,
} from './shared/test-run-summary.fixtures';



export { expect };

export type { baseFixture } from '../../fixtures/baseFixture';

export {
  TestRunSummaryCollector,
  finalizeTestRunSummary,
  rewritePortalLinksForDisplay,
} from './shared/test-run-summary.fixtures';

export type { ExpectedActualOutcome, FinalizeTestRunSummaryOptions, VerificationCheck } from './shared/test-run-summary.fixtures';



export const test = baseTest.extend<{ TestRunSummary: TestRunSummaryCollector }>({

  TestRunSummary: async ({}, use, testInfo) => {

    await use(new TestRunSummaryCollector(testInfo.title));

  },

});



test.afterEach(({ Responses, TestRunSummary }) => {

  if (TestRunSummary.isFinalized()) {

    return;

  }

  const links = rewritePortalLinksForDisplay(reportGenerator.setLinksToResponses(Responses));

  console.log(`\nTEST: ${TestRunSummary.testTitle}\n[API responses]`, JSON.stringify(links, null, 2));

  test.info().attach('[API responses]', {

    body: JSON.stringify({ testTitle: TestRunSummary.testTitle, links, responses: Responses }, null, 2),

    contentType: 'application/json',

  });

});
