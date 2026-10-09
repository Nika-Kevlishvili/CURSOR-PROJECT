import { request as playwrightRequest } from '@playwright/test';

export class JiraReporter {
    public async sendReport(statusMap: Record<string, 31 | 2>) {
        const apiRequestContext = await playwrightRequest.newContext({
            ignoreHTTPSErrors: true,
            extraHTTPHeaders: {
                'Content-Type': 'application/json',
                Authorization:
                    'Basic ' +
                    Buffer.from(
                        `${process.env.JIRA_EMAIL}:${process.env.JIRA_API_TOKEN}`
                    ).toString('base64'),
            }
        });

        try {
            for (const [key, value] of Object.entries(statusMap)) {
                const StatusBody = {
                    body: {
                        "transition": {
                            "id": value
                        }
                    }
                };
                const response = await apiRequestContext.post(`https://oppa-support.atlassian.net/rest/api/3/issue/${key}/transitions`, { data: StatusBody.body });
                if (response.ok()) {
                    console.log(`Report successfully posted to Jira issue ${key}`);
                } else {
                    console.error(`Failed to post to Jira: ${response.status()} ${response.statusText()}`);
                    const errorText = await response.text();
                    console.error(errorText);
                }

                await new Promise(resolve => setTimeout(resolve, 2000));
            }
        } catch (err) {
            console.error('Error sending report to Jira:', err);
        } finally {
            await apiRequestContext.dispose();
        }
    }
}
