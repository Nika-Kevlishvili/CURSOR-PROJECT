import { request as playwrightRequest, APIRequestContext } from '@playwright/test';
import * as fs from 'fs';
import * as os from 'os';
import * as path from 'path';
import { StatusMapStore, MergedStatusMap } from './StatusMapStore';

type RegressionEnvKey = 'dev' | 'dev2' | 'test';

const FAILED_ENV_FIELD = 'customfield_12125';

const ENV_JIRA_OPTIONS: Record<RegressionEnvKey, { label: string; optionId: string }> = {
    dev:  { label: 'DEV',  optionId: '10895' },
    dev2: { label: 'DEV2', optionId: '10896' },
    test: { label: 'TEST', optionId: '10897' },
};

const WORKFLOW_ENV_MAP: Record<string, RegressionEnvKey> = {
    'dev-regression.yml':  'dev',
    'dev2-regression.yml': 'dev2',
    'test-regression.yml': 'test',
    'jiraregression2.yml': 'dev2',
};

export class JiraReporter {

    /** Strip wrapping/trailing quotes that often leak into GitHub secrets and dotenv files. */
    private jiraBasicAuthHeader(): string {
        const email = (process.env.JIRA_EMAIL ?? '').trim().replace(/^['"]+|['"]+$/g, '');
        const token = (process.env.JIRA_API_TOKEN ?? '').trim().replace(/^['"]+|['"]+$/g, '');
        return 'Basic ' + Buffer.from(`${email}:${token}`).toString('base64');
    }

    private isMissingIssue(status: number, body: string): boolean {
        if (status === 404) return true;
        return status !== 401 && status !== 403
            && /does not exist or you do not have permission/i.test(body);
    }

    private isAuthFailure(status: number): boolean {
        return status === 401 || status === 403;
    }

    private envFromWorkflowRef(): RegressionEnvKey | undefined {
        const ref = process.env.GITHUB_WORKFLOW_REF ?? '';
        const file = ref.split('@')[0].split('/').pop()?.toLowerCase() ?? '';
        return WORKFLOW_ENV_MAP[file];
    }

    private envFromStatsJson(): RegressionEnvKey | undefined {
        const statsPath = path.join(os.homedir(), 'merged-reports', 'stats.json');
        if (!fs.existsSync(statsPath)) return undefined;

        try {
            const stats = JSON.parse(fs.readFileSync(statsPath, 'utf-8')) as Record<
                string,
                { timestamp?: string }
            >;
            let best: { env: RegressionEnvKey; time: number } | undefined;

            for (const [key, val] of Object.entries(stats)) {
                if (!val?.timestamp || !ENV_JIRA_OPTIONS[key as RegressionEnvKey]) continue;
                const t = Date.parse(val.timestamp.replace(' UTC', ' UTC'));
                if (Number.isNaN(t)) continue;
                if (!best || t > best.time) best = { env: key as RegressionEnvKey, time: t };
            }

            if (best && Date.now() - best.time < 2 * 60 * 60 * 1000) return best.env;
        } catch {
            /* ignore */
        }
        return undefined;
    }

    private async resolveBaseURL(): Promise<string> {
        if (process.env.BASE_URL) return process.env.BASE_URL;

        const ctxPath = path.resolve(__dirname, '../../fixtures/.run-context.json');
        if (fs.existsSync(ctxPath)) {
            try {
                const ctx = JSON.parse(fs.readFileSync(ctxPath, 'utf-8'));
                if (ctx.baseURL) return ctx.baseURL;
            } catch {
                /* ignore */
            }
        }

        try {
            const configPath = path.resolve(__dirname, '../../playwright.config.ts');
            if (fs.existsSync(configPath)) {
                const content = fs.readFileSync(configPath, 'utf-8');
                const dev2Url = content.match(/const dev2Url\s*=\s*['"](.+?)['"]/)?.[1];
                const devUrl = content.match(/const devUrl\s*=\s*['"](.+?)['"]/)?.[1];
                const testUrl = content.match(/const testUrl\s*=\s*['"](.+?)['"]/)?.[1];

                if (content.includes('process.env.BASE_URL || dev2Url') && dev2Url) return dev2Url;
                if (content.includes('process.env.BASE_URL || devUrl') && devUrl) return devUrl;
                if (testUrl) return testUrl;
            }
        } catch (error) {
            console.warn('Failed to read playwright.config.ts:', error);
        }

        return 'http://10.236.20.11:8091/';
    }

    private envFromBaseUrl(baseUrl: string): RegressionEnvKey | undefined {
        const url = baseUrl.toLowerCase();
        if (url.includes('phoenix2-dev')) return 'dev2';
        if (url.includes('phoenix1-dev') || url.includes('10.236.20.11')) return 'dev';
        if (url.includes('phoenix-epres') || url.includes('testapps')) return 'test';
        return undefined;
    }

    private async resolveEnvironment(): Promise<
        | {
              env: RegressionEnvKey;
              jiraOptionId: string;
              label: string;
              source: string;
          }
        | undefined
    > {
        const fromWorkflow = this.envFromWorkflowRef();
        if (fromWorkflow) {
            const cfg = ENV_JIRA_OPTIONS[fromWorkflow];
            console.log(`[JiraReporter] Env=${fromWorkflow} (source: GITHUB_WORKFLOW_REF)`);
            return {
                env: fromWorkflow,
                jiraOptionId: cfg.optionId,
                label: cfg.label,
                source: 'workflow',
            };
        }

        const fromStats = this.envFromStatsJson();
        if (fromStats) {
            const cfg = ENV_JIRA_OPTIONS[fromStats];
            console.log(`[JiraReporter] Env=${fromStats} (source: stats.json)`);
            return {
                env: fromStats,
                jiraOptionId: cfg.optionId,
                label: cfg.label,
                source: 'stats.json',
            };
        }

        const baseUrl = (await this.resolveBaseURL()).toLowerCase();
        console.log(`[JiraReporter] Detected BASE_URL: ${baseUrl}`);
        const fromUrl = this.envFromBaseUrl(baseUrl);
        if (fromUrl) {
            const cfg = ENV_JIRA_OPTIONS[fromUrl];
            console.log(`[JiraReporter] Env=${fromUrl} (source: BASE_URL)`);
            return {
                env: fromUrl,
                jiraOptionId: cfg.optionId,
                label: cfg.label,
                source: 'baseURL',
            };
        }

        console.warn('[JiraReporter] Could not resolve environment — Failed ENV skipped');
        return undefined;
    }

    private async getFailedEnvIds(api: APIRequestContext, issueKey: string): Promise<string[]> {
        const getRes = await api.get(
            `https://oppa-support.atlassian.net/rest/api/3/issue/${issueKey}?fields=${FAILED_ENV_FIELD}`
        );
        if (!getRes.ok()) {
            console.error(`[${issueKey}] Failed to read Failed ENV field`);
            return [];
        }

        const issue = await getRes.json();
        return issue.fields[FAILED_ENV_FIELD]?.map((e: { id: string }) => e.id) ?? [];
    }

    private async mergeFailedEnv(
        api: APIRequestContext,
        issueKey: string,
        optionId: string,
        action: 'add' | 'remove'
    ): Promise<boolean> {
        const currentIds = await this.getFailedEnvIds(api, issueKey);

        const updatedIds =
            action === 'add'
                ? [...new Set([...currentIds, optionId])]
                : currentIds.filter(id => id !== optionId);

        const putRes = await api.put(
            `https://oppa-support.atlassian.net/rest/api/3/issue/${issueKey}`,
            { data: { fields: { [FAILED_ENV_FIELD]: updatedIds.map(id => ({ id })) } } }
        );

        if (!putRes.ok()) {
            console.error(`[${issueKey}] Failed ENV update error:`, await putRes.text());
            return false;
        }
        return true;
    }

    private async postTransition(
        api: APIRequestContext,
        issueKey: string,
        transitionId: 31 | 2
    ): Promise<boolean> {
        const res = await api.post(
            `https://oppa-support.atlassian.net/rest/api/3/issue/${issueKey}/transitions`,
            { data: { transition: { id: transitionId } } }
        );

        if (!res.ok()) {
            console.error(`Failed to transition ${issueKey}:`, await res.text());
            return false;
        }

        console.log(`Transition OK: ${issueKey} → ${transitionId === 31 ? 'Passed' : 'Failed'}`);
        return true;
    }

    public async sendReport(statusMap: Record<string, 31 | 2>) {
        const envCtx = await this.resolveEnvironment();

        const apiRequestContext = await playwrightRequest.newContext({
            ignoreHTTPSErrors: true,
            extraHTTPHeaders: {
                'Content-Type': 'application/json',
                Authorization: this.jiraBasicAuthHeader(),
            },
        });

        try {
            for (const [key, reportStatus] of Object.entries(statusMap)) {
                if (!envCtx) {
                    await this.postTransition(apiRequestContext, key, reportStatus);
                    await new Promise(resolve => setTimeout(resolve, 2000));
                    continue;
                }

                if (reportStatus === 2) {
                    const fieldOk = await this.mergeFailedEnv(
                        apiRequestContext,
                        key,
                        envCtx.jiraOptionId,
                        'add'
                    );
                    if (fieldOk) {
                        console.log(`${key}: +${envCtx.label} in Failed ENV`);
                    }
                    await this.postTransition(apiRequestContext, key, 2);
                } else if (reportStatus === 31) {
                    const fieldOk = await this.mergeFailedEnv(
                        apiRequestContext,
                        key,
                        envCtx.jiraOptionId,
                        'remove'
                    );
                    if (fieldOk) {
                        console.log(`${key}: -${envCtx.label} from Failed ENV`);
                    }

                    const remaining = await this.getFailedEnvIds(apiRequestContext, key);

                    if (remaining.length > 0) {
                        console.log(
                            `${key}: PASS on ${envCtx.label}, but still failing on other env(s) ` +
                                `[${remaining.join(', ')}] → status stays Failed`
                        );
                        await this.postTransition(apiRequestContext, key, 2);
                    } else {
                        console.log(
                            `${key}: PASS on ${envCtx.label}, all envs clear → status=Passed`
                        );
                        await this.postTransition(apiRequestContext, key, 31);
                    }
                }

                await new Promise(resolve => setTimeout(resolve, 2000));
            }
        } catch (error) {
            console.error('Error sending report to Jira:', error);
        } finally {
            await apiRequestContext.dispose();
        }
    }

    /**
     * Sends merged status map to Jira. Used by the dedicated Jira update pipeline.
     * Sets Failed ENV field directly based on all env results rather than
     * reading/merging the existing field per-env.
     */
    public async sendMergedReport(mergedMap: MergedStatusMap) {
        const apiRequestContext = await playwrightRequest.newContext({
            ignoreHTTPSErrors: true,
            extraHTTPHeaders: {
                'Content-Type': 'application/json',
                Authorization: this.jiraBasicAuthHeader(),
            },
        });

        const envOptions = StatusMapStore.envJiraOptions;
        let processed = 0;
        let errors = 0;
        let consecutiveAuthFailures = 0;
        const skippedMissing: string[] = [];

        try {
            const myself = await apiRequestContext.get(
                'https://oppa-support.atlassian.net/rest/api/3/myself'
            );
            if (!myself.ok()) {
                const body = await myself.text();
                throw new Error(
                    `Jira /myself failed HTTP ${myself.status()}: ${body}. ` +
                    `GitHub secrets JIRA_EMAIL / JIRA_API_TOKEN are invalid or not for oppa-support.atlassian.net.`
                );
            }
            const me = await myself.json() as { emailAddress?: string; displayName?: string };
            console.log(`[JiraReporter] Authenticated as ${me.displayName ?? me.emailAddress ?? '(unknown)'}`);

            for (const [ticketId, ticket] of Object.entries(mergedMap)) {
                const failedEnvOptionIds = ticket.failedOnEnvs
                    .filter((env): env is RegressionEnvKey => env in envOptions)
                    .map(env => envOptions[env].optionId);

                const putRes = await apiRequestContext.put(
                    `https://oppa-support.atlassian.net/rest/api/3/issue/${ticketId}`,
                    {
                        data: {
                            fields: {
                                [FAILED_ENV_FIELD]: failedEnvOptionIds.map(id => ({ id })),
                            },
                        },
                    }
                );

                if (!putRes.ok()) {
                    const body = await putRes.text();
                    console.error(`[${ticketId}] Failed ENV update error:`, body);

                    if (this.isMissingIssue(putRes.status(), body)) {
                        skippedMissing.push(ticketId);
                        console.warn(
                            `[${ticketId}] skipped — Jira issue does not exist. Remaining tickets continue.`
                        );
                        processed++;
                        consecutiveAuthFailures = 0;
                        continue;
                    }

                    errors++;

                    if (this.isAuthFailure(putRes.status())) {
                        consecutiveAuthFailures++;
                        if (consecutiveAuthFailures >= 3) {
                            throw new Error(
                                `Jira rejected ${consecutiveAuthFailures} tickets in a row ` +
                                `(HTTP ${putRes.status()}: authentication/permission). ` +
                                `Check GitHub secrets JIRA_EMAIL / JIRA_API_TOKEN for oppa-support.atlassian.net ` +
                                `and REG project browse+transition rights. Aborting remaining ${
                                    Object.keys(mergedMap).length - processed
                                } tickets.`
                            );
                        }
                        processed++;
                        continue;
                    }
                    consecutiveAuthFailures = 0;
                } else {
                    consecutiveAuthFailures = 0;
                    const envLabels = ticket.failedOnEnvs.length > 0
                        ? ticket.failedOnEnvs.map(e => envOptions[e]?.label ?? e).join(', ')
                        : '(cleared)';
                    console.log(`${ticketId}: Failed ENV → [${envLabels}]`);
                }

                const transitioned = await this.postTransition(
                    apiRequestContext,
                    ticketId,
                    ticket.status
                );
                if (!transitioned) errors++;

                processed++;
                await new Promise(resolve => setTimeout(resolve, 2000));
            }
        } catch (error) {
            console.error('Error sending merged report to Jira:', error);
            throw error;
        } finally {
            await apiRequestContext.dispose();
        }

        if (skippedMissing.length > 0) {
            console.warn(
                `\n[JiraReporter] Skipped ${skippedMissing.length} missing Jira issue(s) ` +
                `(tests referenced keys that do not exist): ${skippedMissing.join(', ')}`
            );
        }

        console.log(
            `\n[JiraReporter] Merged update complete: ${processed} tickets processed, ` +
            `${errors} errors, ${skippedMissing.length} skipped (missing)`
        );
    }
}