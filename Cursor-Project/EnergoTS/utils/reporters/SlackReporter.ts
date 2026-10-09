import { request as playwrightRequest } from '@playwright/test';
import * as fs from 'fs';
import * as path from 'path';
import * as os from 'os';
import { randomGens } from '../randomGens';

export class SlackReporter {
    public async resolveBaseURL(): Promise<string> {
        // Try to get BASE_URL from available sources
        let baseUrl = process.env.BASE_URL;

        // Try reading from playwright config as fallback
        if (!baseUrl) {
            try {
                const configPath = path.resolve(__dirname, '../../playwright.config.ts');
                if (fs.existsSync(configPath)) {
                    const configContent = fs.readFileSync(configPath, 'utf-8');

                    // Extract the default URL from config (matches: baseURL: process.env.BASE_URL || dev2Url)
                    const dev2UrlMatch = configContent.match(/const dev2Url = ['"](.+?)['"]/);
                    const devUrlMatch = configContent.match(/const devUrl = ['"](.+?)['"]/);

                    // Check which URL is used as default in the config
                    if (configContent.includes('process.env.BASE_URL || dev2Url')) {
                        baseUrl = dev2UrlMatch ? dev2UrlMatch[1] : undefined;
                        console.log(`Loaded BASE_URL from playwright.config.ts (dev2): ${baseUrl}`);
                    } else if (configContent.includes('process.env.BASE_URL || devUrl')) {
                        baseUrl = devUrlMatch ? devUrlMatch[1] : undefined;
                        console.log(`Loaded BASE_URL from playwright.config.ts (dev): ${baseUrl}`);
                    }
                }
            } catch (error) {
                console.warn('Failed to read BASE_URL from config:', error);
            }
        }

        // Final fallback to dev URL
        if (!baseUrl) {
            baseUrl = 'http://10.236.20.11:8091/';
            console.warn('No BASE_URL found, falling back to dev URL');
        }

        return baseUrl;
    }

    private selectChannel(): string {
        const workflowRef = process.env.GITHUB_WORKFLOW_REF ?? '';
        // GITHUB_WORKFLOW_REF format: "owner/repo/.github/workflows/main.yml@refs/heads/branch"
        // Must strip the @refs/... suffix before splitting by '/' to get the filename
        const workflowPath = workflowRef.split('@')[0];
        const workflowFile = workflowPath.split('/').pop() ?? '';

        if (workflowFile === 'main.yml') {
            return 'C0AN5UQ6TQW'; // Pipeline channel
        }

        return 'C0ANBH2FERJ'; // Regression channel (default for all *-regression.yml and others)
    }

    private buildSummaryMessage(testStats: { passed: number; failed: number; total: number; setupFailed?: boolean }): string {
        // Check for setup failure first
        if (testStats.setupFailed) {
            return `❌ Global setup failed or timed out\nEnvironment is unreachable or setup exceeded timeout`;
        }

        const isRegression = process.env.REGRESSION === 'true';
        const { passed, failed, total } = testStats;

        return isRegression
            ? (failed === 0
                ? `Regression run\n✅ All tests passed: ${total}/${total}`
                : `Regression run\n⚠️ Tests completed: ${passed} passed, ${failed} failed - ${total} tests`)
            : (failed === 0
                ? `✅ All tests passed: ${total}/${total}`
                : `⚠️ Tests completed: ${passed} passed, ${failed} failed - ${total} tests`);
    }

    private async uploadFileToSlack(
        apiContext: any,
        filePath: string,
        fileName: string,
        channelId: string,
        initialComment?: string
    ): Promise<void> {
        if (!fs.existsSync(filePath)) {
            throw new Error(`File not found at ${filePath}`);
        }

        const fileSizeInBytes = fs.statSync(filePath).size;
        const encodedFileName = encodeURIComponent(path.basename(filePath));

        const fileUploadUrl = await apiContext.get(`https://slack.com/api/files.getUploadURLExternal?filename=${encodedFileName}&length=${fileSizeInBytes}&pretty=1`);
        if (!fileUploadUrl.ok()) {
            throw new Error(`Failed to get upload URL: ${fileUploadUrl.status()} ${fileUploadUrl.statusText()}`);
        }

        const fileUploadData = await fileUploadUrl.json();
        if (!fileUploadData || !fileUploadData.file_id || !fileUploadData.upload_url) {
            throw new Error(`Invalid response from Slack API: ${JSON.stringify(fileUploadData)}`);
        }

        const file_id = fileUploadData.file_id;
        const uploadUrl = fileUploadData.upload_url;
        const fileStream = fs.createReadStream(filePath);

        const uploadFile = await apiContext.post(uploadUrl, {
            multipart: {
                file: fileStream
            }
        });

        if (!uploadFile.ok()) {
            throw new Error(`Failed to upload file: ${uploadFile.status()} ${uploadFile.statusText()}`);
        }

        const comment = initialComment ? `&initial_comment=${encodeURIComponent(initialComment)}` : '';
        const finishUpload = await apiContext.post(`https://slack.com/api/files.completeUploadExternal?files=[{\"id\":\"${file_id}\", \"title\":\"${fileName}\"}]&channel_id=${channelId}${comment}&pretty=1`, {});
        
        if (!finishUpload.ok()) {
            throw new Error(`Failed to complete upload: ${finishUpload.status()} ${finishUpload.statusText()}`);
        }

        console.log(`File ${fileName} successfully uploaded to Slack`);
    }

    public async sendReport(
        statusMap: Record<string, 31 | 2>, 
        testStats?: { passed: number; failed: number; total: number; setupFailed?: boolean },
    ) {
        const dateStr = randomGens.generateTodaysDate('dd.mm.yyyy');
        const htmlFileName = `report(${dateStr})`;
        const pdfFileName = `report(${dateStr})`;

        if (!process.env.SLACK_API_TOKEN) {
            throw new Error('SLACK_API_TOKEN is not defined in environment variables.');
        }

        // Use test stats if provided, otherwise calculate from Jira IDs (fallback)
        let stats: { passed: number; failed: number; total: number; setupFailed?: boolean };

        if (testStats) {
            stats = testStats;
        } else {
            // Fallback: use Jira ID counts (includes suites)
            const totalJiraIds = Object.keys(statusMap).length;
            const passed = Object.values(statusMap).filter(status => status === 31).length;
            const failed = Object.values(statusMap).filter(status => status === 2).length;
            stats = { passed, failed, total: totalJiraIds };
        }

        const summaryMessage = this.buildSummaryMessage(stats);
        const baseUrl = await this.resolveBaseURL();
        const channelId = this.selectChannel();

        console.log(`sendReportToSlack: resolved baseUrl='${baseUrl}', selected channel='${channelId}'`);

        const apiRequestContext = await playwrightRequest.newContext({
            extraHTTPHeaders: {
                'Authorization': `Bearer ${process.env.SLACK_API_TOKEN}`
            }
        });

        try {
            const mergedZipPath = path.resolve(__dirname, '../../playwright-report/merged-reports.zip');
            const indexHtmlPath = path.resolve(__dirname, '../../playwright-report/index.html');
            const statsJsonPath = path.join(os.homedir(), 'merged-reports', 'stats.json');

            const isDev2       = (process.env.BASE_URL ?? '').includes('phoenix-dev2');
            const isIndividual = (process.env.INDIVIDUAL ?? 'false') === 'true';

            // Grouped mode, non-dev2: don't send to Slack — dev2 will send the merged ZIP
            if (!isIndividual && !isDev2) {
                console.log('Grouped mode — Slack report will be sent by the dev2 run. Skipping upload.');
                return;
            }

            let uploadPath: string;
            let uploadedName: string;
            let slackMessage: string;

            if (isIndividual) {
                // Individual mode: send this env's own report
                uploadPath   = indexHtmlPath;
                uploadedName = htmlFileName;
                slackMessage = summaryMessage;
                console.log(`Individual mode — uploading ${uploadPath}`);
            } else {
                // Grouped mode, dev2: send merged ZIP with combined stats from all envs
                uploadPath   = mergedZipPath;
                uploadedName = `merged-reports(${dateStr})`;

                const ENV_ORDER: Record<string, string> = {
                    dev: 'Dev', dev2: 'Dev2', 'dev-fix': 'Dev-Fix', test: 'Test',
                };
                let allStatsData: Record<string, { passed: number; failed: number; total: number }> = {};
                if (fs.existsSync(statsJsonPath)) {
                    try { allStatsData = JSON.parse(fs.readFileSync(statsJsonPath, 'utf-8')); } catch (_) {}
                }
                const lines = Object.entries(ENV_ORDER).map(([id, label]) => {
                    const s = allStatsData[id];
                    if (!s) return `${label} — ⏳ not run`;
                    return s.failed > 0
                        ? `${label} — ✅ ${s.passed} passed / ⚠️ ${s.failed} failed`
                        : `${label} — ✅ ${s.passed} passed`;
                });
                slackMessage = `Regression run\n${lines.join('\n')}`;
                console.log(`Grouped mode — uploading merged ZIP with stats:\n${lines.join('\n')}`);
            }

            await this.uploadFileToSlack(apiRequestContext, uploadPath, uploadedName, channelId, slackMessage);
            console.log('Report successfully uploaded to Slack.');
        } catch (error) {
            console.error('Error sending report to Slack:', error);
        } finally {
            if (apiRequestContext) {
                await apiRequestContext.dispose();
            }
        }
    }
}
