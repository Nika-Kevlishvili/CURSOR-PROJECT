#!/usr/bin/env npx ts-node
/**
 * updateJiraStatuses.ts
 *
 * Standalone script run by the update-jira pipeline (6 AM Georgian time).
 *
 * 1. Loads per-env status map JSON files saved by each regression pipeline
 * 2. Merges them — each ticket gets a failedOnEnvs[] array
 * 3. Sends a single batch of Jira transitions + Failed ENV field updates
 * 4. Clears processed status map files
 */

import * as dotenv from 'dotenv';
import { StatusMapStore } from '../utils/reporters/StatusMapStore';
import { JiraReporter } from '../utils/reporters/JiraReporter';

dotenv.config();

async function main() {
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

    if (!process.env.JIRA_EMAIL || !process.env.JIRA_API_TOKEN) {
        console.error('Missing JIRA_EMAIL or JIRA_API_TOKEN — skipping Jira update');
        process.exit(1);
    }

    const jiraReporter = new JiraReporter();
    await jiraReporter.sendMergedReport(merged);

    StatusMapStore.clearStatusMaps();

    console.log('\n=== Jira Status Update Complete ===');
}

main().catch(err => {
    console.error('Fatal error:', err);
    process.exit(1);
});
