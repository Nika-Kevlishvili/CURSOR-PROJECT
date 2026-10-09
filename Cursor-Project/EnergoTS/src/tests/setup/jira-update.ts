import { test as jiraUpdate, expect } from '@playwright/test';
import { StatusMapStore } from '../../backend/utils/reporters/StatusMapStore';
import { JiraReporter } from '../../backend/utils/reporters/JiraReporter';

/**
 * Runs as the "update jira" Playwright project (see playwright.config.ts).
 * Replaces the old standalone `npx ts-node updateJiraStatuses.ts` script so it
 * uses Playwright's bundled TS runtime instead of downloading ts-node at runtime.
 *
 * 1. Loads per-env status map JSON files saved by each regression pipeline
 * 2. Merges them — each ticket gets a failedOnEnvs[] array
 * 3. Sends a single batch of Jira transitions + Failed ENV field updates
 * 4. Clears processed status map files
 */
jiraUpdate('jira sync', async () => {
  jiraUpdate.setTimeout(3600000);

  console.log('=== Jira Status Update Pipeline ===\n');

  const envMaps = StatusMapStore.loadAllEnvStatusMaps();

  if (envMaps.length === 0) {
    console.log('No recent status maps found. Nothing to process.');
    return;
  }

  console.log(`\nLoaded status maps from ${envMaps.length} environment(s): ${envMaps.map(m => m.environment).join(', ')}`);

  const merged = StatusMapStore.mergeStatusMaps(envMaps);
  const totalTickets = Object.keys(merged).length;
  const failedCount = Object.values(merged).filter(t => t.status === 2).length;
  const passedCount = Object.values(merged).filter(t => t.status === 31).length;

  console.log(`\nMerged result: ${totalTickets} tickets (${passedCount} passed, ${failedCount} failed)\n`);

  const ticketsWithFailures = Object.entries(merged)
    .filter(([, t]) => t.failedOnEnvs.length > 0)
    .map(([id, t]) => `  ${id}: failed on [${t.failedOnEnvs.join(', ')}]`);

  if (ticketsWithFailures.length > 0) {
    console.log('Tickets with environment failures:');
    console.log(ticketsWithFailures.join('\n'));
    console.log();
  }

  StatusMapStore.saveMergedLog(merged, envMaps);

  expect(
    process.env.JIRA_EMAIL && process.env.JIRA_API_TOKEN,
    'Missing JIRA_EMAIL or JIRA_API_TOKEN — cannot update Jira'
  ).toBeTruthy();

  const jiraReporter = new JiraReporter();
  await jiraReporter.sendMergedReport(merged);

  StatusMapStore.clearStatusMaps();

  console.log('\n=== Jira Status Update Complete ===');
});
