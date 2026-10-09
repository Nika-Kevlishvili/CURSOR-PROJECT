import { test } from '@playwright/test';
import { setupAuthenticatedPage, PhoenixNavigation, ScreenshotManager } from '../../helpers';

test.describe('Product Contracts - Visual Regression', () => {
  let nav: PhoenixNavigation;
  let screenshots: ScreenshotManager;

  test.beforeEach(async ({ page }) => {
    await setupAuthenticatedPage(page);
    nav = new PhoenixNavigation(page);
    screenshots = new ScreenshotManager(page, {
      sectionIndex: 4,
      sectionName: 'Contracts and Orders',
      subItemIndex: 1,
      subItemName: 'Energy Product Contracts',
    });
  });

  test('List View - Full Visual Regression', async ({ page, baseURL }) => {
    test.setTimeout(300000);

    await page.goto(baseURL || '');
    await page.waitForLoadState('networkidle');
    await page.waitForTimeout(2000);

    await screenshots.captureViewport('listing', '00-home', 'Home page after login', [
      'User logged in (name visible)',
      'Main menu visible on left',
      'Language selector visible',
    ]);

    console.log('📍 Step 1: Expanding Contracts and Orders menu...');
    await nav.expandMenu('Contracts and Orders');
    await page.waitForTimeout(1000);

    await screenshots.captureViewport('listing', '01-menu-expanded', 'Contracts and Orders menu expanded', [
      'Submenu items visible',
      'Product Contracts option visible',
      'Service Contracts option visible',
    ]);

    console.log('📍 Step 2: Clicking Product Contracts...');
    await nav.clickSubmenuItem('Product Contracts');
    await page.waitForTimeout(2000);
    await nav.waitForNoLoadingSpinner();

    await screenshots.captureFullPage('listing', '02-list-view', 'Product Contracts list view', [
      'Table loaded with data',
      'Column headers visible',
      'Create/Add button visible',
      'Search/Filter options',
      'Pagination if applicable',
    ]);

    await screenshots.captureWithHorizontalScroll(
      'listing',
      '03-table-columns',
      'All table columns (horizontal scroll)',
      ['All columns visible', 'Data aligned', 'Status badges'],
    );

    console.log('📍 Step 3: Opening first contract preview...');
    const firstRow = page.locator('tbody tr').first();
    if (await firstRow.isVisible({ timeout: 5000 }).catch(() => false)) {
      await firstRow.dblclick();
      await page.waitForTimeout(2000);
      await nav.waitForNoLoadingSpinner();

      await screenshots.captureFullPage('object', '04-contract-detail', 'Contract detail/preview view', [
        'Contract number in header',
        'Status badge',
        'Customer info',
        'Contract dates',
        'Tabs visible (General, PODs, etc.)',
        'Action buttons',
      ]);

      const tabs = ['PODs', 'Documents', 'History'];
      for (const tabName of tabs) {
        const success = await nav.clickTabSafe(tabName);
        if (success) {
          await page.waitForTimeout(1000);
          await screenshots.captureViewport(
            'object',
            `05-tab-${tabName.toLowerCase()}`,
            `${tabName} tab content`,
            [`${tabName} tab active`, `${tabName} content loaded`],
          );
        }
      }

      await page.goBack();
      await page.waitForTimeout(1000);
    }

    console.log('📍 Step 4: Opening Create form...');
    const createBtn = page
      .locator('button:has-text("Create"), button:has-text("Add"), button:has-text("New")')
      .first();
    if (await createBtn.isVisible({ timeout: 3000 }).catch(() => false)) {
      await createBtn.click();
      await page.waitForTimeout(2000);

      await screenshots.captureFullPage('object', '06-create-form', 'Create Product Contract form', [
        'Form title',
        'Required fields with *',
        'Customer field',
        'Product field',
        'Dates fields',
        'Save/Cancel buttons',
      ]);

      const cancelBtn = page.locator('button:has-text("Cancel"), button:has-text("Close")').first();
      if (await cancelBtn.isVisible({ timeout: 2000 }).catch(() => false)) {
        await cancelBtn.click();
      }
    }

    await screenshots.generateManifest('dev', baseURL || '');
  });

  test.afterEach(async () => {
    console.log(`\n✅ Test completed. Screenshots: ${screenshots.getScreenshotDir()}`);
  });
});
