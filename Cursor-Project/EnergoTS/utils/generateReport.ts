import * as dotenv from 'dotenv';
import { ReportParser } from './reporters/ReportParser';
import { SlackReporter } from './reporters/SlackReporter';
import { JiraReporter } from './reporters/JiraReporter';
import { ResponseLinker } from './reporters/ResponseLinker';

dotenv.config();

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

    public getTestStats(suites: any[]): { passed: number; failed: number; total: number } {
        return this.parser.getTestStats(suites);
    }

    public async sendReportToSlack(
        statusMap: Record<string, 31 | 2>, 
        testStats?: { passed: number; failed: number; total: number; setupFailed?: boolean },
    ) {
        return this.slackReporter.sendReport(statusMap, testStats);
    }

    public async sendReportToJira(statusMap: Record<string, 31 | 2>) {
        return this.jiraReporter.sendReport(statusMap);
    }

    public static setLinksToResponses(responses: any): Record<string, string[]> {
        return ResponseLinker.setLinksToResponses(responses);
    }
}
