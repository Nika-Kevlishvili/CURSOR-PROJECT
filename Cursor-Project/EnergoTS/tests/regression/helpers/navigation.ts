import { Page, Locator } from '@playwright/test';
import { NavigationResolver } from './navigation-resolver';

/**
 * Phoenix navigation facade.
 * UI two-level menu is attempted first; NavigationResolver handles fallbacks.
 */
export class PhoenixNavigation {
  private resolver: NavigationResolver;

  constructor(private page: Page) {
    this.resolver = new NavigationResolver(page);
  }

  private sidebarSection(sectionName: string): Locator {
    return this.page.locator('main article li > p').getByText(sectionName, { exact: true });
  }

  async isSidebarExpanded(): Promise<boolean> {
    return (await this.page.locator('main article li > p').count()) > 0;
  }

  /** Single burger click only — no toggle loops. */
  async ensureSidebarOpen(): Promise<void> {
    if (await this.isSidebarExpanded()) {
      console.log('  ✓ Sidebar already open');
      return;
    }

    const burger = this.page.locator('div.burger');
    if (await burger.isVisible({ timeout: 3000 }).catch(() => false)) {
      console.log('  → Opening sidebar (div.burger)...');
      await burger.click();
      await this.page.waitForTimeout(1500);
    }

    if (await this.isSidebarExpanded()) {
      console.log('  ✓ Sidebar opened');
      return;
    }

    console.log('  ⚠ Sidebar not open — will use direct URL fallback in navigateToSubmenu');
  }

  async expandSection(sectionName: string): Promise<void> {
    if (sectionName === 'Shortcuts') return;
    await this.ensureSidebarOpen();
    const section = this.sidebarSection(sectionName);
    if (await section.isVisible({ timeout: 3000 }).catch(() => false)) {
      await section.click({ force: true });
      await this.page.waitForTimeout(600);
    }
  }

  async clickSubmenuItem(sectionName: string, subItemName: string): Promise<void> {
    await this.resolver.navigateToListing(sectionName, subItemName);
  }

  async navigateToSubmenu(sectionName: string, subItemName: string): Promise<void> {
    await this.clickSubmenuItem(sectionName, subItemName);
  }

  async clickButton(buttonText: string): Promise<void> {
    await this.page.getByRole('button', { name: buttonText }).first().click();
  }

  async clickTabSafe(tabName: string): Promise<boolean> {
    const tab = this.page.getByRole('tab', { name: tabName }).or(
      this.page.locator(`[role="tab"]:has-text("${tabName}")`),
    );
    if (await tab.first().isVisible({ timeout: 2000 }).catch(() => false)) {
      await tab.first().click();
      await this.page.waitForTimeout(500);
      return true;
    }
    return false;
  }

  async waitForNoLoadingSpinner(timeout = 30000): Promise<void> {
    const spinnerSelectors = [
      '.loading', '.spinner', '[class*="loading"]', '[class*="spinner"]',
      '.ant-spin', '.mat-spinner', '[role="progressbar"]',
    ];

    for (const selector of spinnerSelectors) {
      const spinner = this.page.locator(selector);
      if (await spinner.isVisible({ timeout: 1000 }).catch(() => false)) {
        await spinner.waitFor({ state: 'hidden', timeout });
      }
    }
  }

  async openFirstListPreview(): Promise<boolean> {
    const rowSelectors = [
      'tbody tr',
      '.ag-center-cols-container .ag-row',
      '.ag-row',
      '[role="row"]:not([role="columnheader"])',
    ];

    for (const selector of rowSelectors) {
      const row = this.page.locator(selector).first();
      if (await row.isVisible({ timeout: 3000 }).catch(() => false)) {
        await row.dblclick().catch(async () => row.click());
        await this.page.waitForLoadState('networkidle');
        await this.page.waitForTimeout(1200);
        return true;
      }
    }

    return false;
  }
}
