import * as dotenv from 'dotenv';
import { ReportParser } from './reporters/ReportParser';
import { SlackReporter } from './reporters/SlackReporter';
import { JiraReporter } from './reporters/JiraReporter';
import { ResponseLinker } from './reporters/ResponseLinker';
import { StatusMapStore, MergedStatusMap } from './reporters/StatusMapStore';

dotenv.config();

type RegressionEnvKey = 'dev' | 'dev2' | 'test';

/**
 * Main facade class for generating and sending test reports.
 * Delegates specific responsibilities to specialized reporter classes.
 */
export default class reportGenerator {
    private parser: ReportParser;
    private slackReporter: SlackReporter;
    private jiraReporter: JiraReporter;

    constructor() {
        this.parser = new ReportParser();
        this.slackReporter = new SlackReporter();
        this.jiraReporter = new JiraReporter();
    }

    public async readReport() {
        return this.parser.readReport();
    }

    public buildStatusMap(report: any): Record<string, 31 | 2> {
        return this.parser.buildStatusMap(report);
    }

    public countActualTests(suites: any[]): number {
        return this.parser.countActualTests(suites);
    }

    public getTestStats(suites: any[]): { passed: number; failed: number; skipped: number; total: number } {
        return this.parser.getTestStats(suites);
    }

    public async sendReportToSlack(
        statusMap: Record<string, 31 | 2>, 
        testStats?: { passed: number; failed: number; skipped?: number; total: number; setupFailed?: boolean },
    ) {
        return this.slackReporter.sendReport(statusMap, testStats);
    }

    public async sendReportToJira(statusMap: Record<string, 31 | 2>) {
        return this.jiraReporter.sendReport(statusMap);
    }

    public saveStatusMapForEnv(env: RegressionEnvKey, statusMap: Record<string, 31 | 2>): string {
        return StatusMapStore.saveEnvStatusMap(env, statusMap);
    }

    public async sendMergedReportToJira(mergedMap: MergedStatusMap) {
        return this.jiraReporter.sendMergedReport(mergedMap);
    }

    public static setLinksToResponses(responses: any): Record<string, string[]> {
        return ResponseLinker.setLinksToResponses(responses);
    }
}
