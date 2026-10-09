import { test } from '@playwright/test';
import { setupAuthenticatedPage } from '../helpers';

/**
 * Reference flow — two-level Phoenix menu:
 *   Level 1: sidebar section (`main article li > p`) — expands flyout
 *   Level 2: flyout item (`cdk-drop-list`) — opens object listing
 */
test.describe('Reference recording pattern', () => {
  test.skip('Customers — list and preview (reference only)', async ({ page, baseURL }) => {
    await setupAuthenticatedPage(page);
    await page.goto(baseURL || '');
    await page.waitForLoadState('networkidle');

    const sidebarMarker = page.locator('main article li > p').getByText('Customers', { exact: true });
    if (!(await sidebarMarker.isVisible({ timeout: 2000 }).catch(() => false))) {
      await page.mouse.click(35, 35);
      await page.waitForTimeout(800);
    }

    // Level 1 — expand Customers section
    await page.locator('main article li > p').getByText('Customers', { exact: true }).click();
    await page.waitForTimeout(600);

    // Level 2 — flyout "Customers" → real listing
    await page.locator('[id^="cdk-drop-list"]').getByText('Customers', { exact: true }).click();
    await page.waitForLoadState('networkidle');
    await page.waitForTimeout(1500);

    const firstRow = page.locator('tbody tr, .ag-row, [role="row"]').first();
    await firstRow.dblclick();
    await page.waitForLoadState('networkidle');
  });
});
