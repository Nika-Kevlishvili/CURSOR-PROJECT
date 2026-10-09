import * as fs from 'fs';
import * as path from 'path';

export class ReportParser {
    private reportPath: string;

    constructor() {
        this.reportPath = path.resolve(__dirname, '../../playwright-report.json');
    }

    public async readReport() {
        if (!fs.existsSync(this.reportPath)) {
            console.warn('No report found to send to Jira.');
            return null;
        }

        const report = JSON.parse(fs.readFileSync(this.reportPath, 'utf-8'));
        return report;
    }

    private extractJiraId(title: string): string | null {
        const match = title.match(/\[(REG-\d+)\]/);
        return match ? match[1] : null;
    }

    private evaluateSuite(suite: any, statusMap: Record<string, 31 | 2>): boolean {
        let isOk = true;

        // Check direct specs
        for (const spec of suite.specs || []) {
            const specId = this.extractJiraId(spec.title);
            const passed = !!spec.ok;

            if (specId) {
                statusMap[specId] = passed ? 31 : 2;
            }

            if (!passed) {
                isOk = false;
            }
        }

        // Recursively check child suites
        for (const childSuite of suite.suites || []) {
            const childOk = this.evaluateSuite(childSuite, statusMap);
            if (!childOk) {
                isOk = false;
            }
        }

        // If any child spec or suite failed, parent suite is 'fail'
        const suiteId = this.extractJiraId(suite.title);
        if (suiteId) {
            statusMap[suiteId] = isOk ? 31 : 2;
        }

        return isOk;
    }

    /**
     * returns evaluated report file for example: {  'REG-94': 31, 'REG-262': 2,}
     * 
     * 31 indicates success, 2 indicates failure (Jira transition IDs)
     *
     * @param report - report file to be evaluated
     * @returns returns an object mapping Jira IDs to status codes
     *          where 31 indicates success and 2 indicates failure.
     */
    public buildStatusMap(report: any): Record<string, 31 | 2> {
        const statusMap: Record<string, 31 | 2> = {};
        if (!report || !Array.isArray(report.suites)) {
            console.warn('Report JSON is missing or malformed. Skipping Jira status map generation.');
            return statusMap;
        }
        for (const suite of report.suites) {
            this.evaluateSuite(suite, statusMap);
        }

        // Count only actual test specs (not suite hierarchies) for summary
        const actualTestCount = this.countActualTests(report.suites);
        console.log(`Generated status map: ${Object.keys(statusMap).length} Jira IDs (${actualTestCount} actual tests)`);
        console.log('Status map:', statusMap);
        return statusMap;
    }

    /**
     * Counts the actual number of test specs (not suites)
     */
    public countActualTests(suites: any[]): number {
        let count = 0;
        for (const suite of suites) {
            // Count specs in this suite
            count += (suite.specs || []).length;
            // Recursively count specs in child suites
            if (suite.suites && suite.suites.length > 0) {
                count += this.countActualTests(suite.suites);
            }
        }
        return count;
    }

    /**
     * Gets actual test statistics (passed/failed counts from specs only)
     */
    public getTestStats(suites: any[]): { passed: number; failed: number; total: number } {
        let passed = 0;
        let failed = 0;

        for (const suite of suites) {
            // Count specs in this suite
            for (const spec of suite.specs || []) {
                if (spec.ok) {
                    passed++;
                } else {
                    failed++;
                }
            }
            // Recursively count specs in child suites
            if (suite.suites && suite.suites.length > 0) {
                const childStats = this.getTestStats(suite.suites);
                passed += childStats.passed;
                failed += childStats.failed;
            }
        }

        return { passed, failed, total: passed + failed };
    }
}
