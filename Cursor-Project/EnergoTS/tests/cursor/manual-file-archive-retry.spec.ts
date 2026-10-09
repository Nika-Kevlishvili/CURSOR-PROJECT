import { test, expect } from '../../fixtures/baseFixture';

test.describe('Manual: trigger file archive retry job', { tag: '@maintenance' }, () => {
    test('PATCH /file-archive-retry/retry', async ({ Request }) => {
        const resp = await Request.patch('/file-archive-retry/retry');
        // Endpoint is marked deprecated but should still return 200 in Dev2.
        expect(resp.status()).toBe(200);
    });
});

