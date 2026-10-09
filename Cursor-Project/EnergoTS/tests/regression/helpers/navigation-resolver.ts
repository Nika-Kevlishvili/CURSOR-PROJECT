import { Page } from '@playwright/test';
import {
  getKnownPath,
  loadRouteMap,
  saveRouteEntry,
  slugPathCandidates,
  toSlug,
} from './route-map';

export interface NavigationResult {
  method: string;
  url: string;
}

type Strategy = {
  id: string;
  run: () => Promise<boolean>;
};

/**
 * Tries navigation strategies once each (no loops).
 * Order: reference UI → known route map → slug URL → text-click fallback.
 * Persists newly working paths to route-map.json.
 */
export class NavigationResolver {
  private map = loadRouteMap();

  constructor(private page: Page) {}

  private get baseUrl(): string {
    return this.map.baseUrl.replace(/\/$/, '');
  }

  async hasListing(): Promise<boolean> {
    const row = this.page.locator('tbody tr, .ag-row, .ag-center-cols-container .ag-row').first();
    return row.isVisible({ timeout: 5000 }).catch(() => false);
  }

  private async waitForListingApi(timeoutMs = 8000): Promise<string | null> {
    return new Promise((resolve) => {
      const timer = setTimeout(() => {
        this.page.off('request', onRequest);
        resolve(null);
      }, timeoutMs);

      const onRequest = (req: { url: () => string }) => {
        const url = req.url();
        if (url.includes('/list') && (url.includes('page=0') || url.includes('size='))) {
          clearTimeout(timer);
          this.page.off('request', onRequest);
          resolve(url);
        }
      };

      this.page.on('request', onRequest);
    });
  }

  private async openSidebarOnce(): Promise<boolean> {
    const count = await this.page.locator('main article li > p').count();
    if (count > 0) return true;

    const burger = this.page.locator('div.burger');
    if (await burger.isVisible({ timeout: 2000 }).catch(() => false)) {
      await burger.click();
      await this.page.waitForTimeout(1500);
    }

    return (await this.page.locator('main article li > p').count()) > 0;
  }

  private async tryUiTwoLevel(sectionName: string, subItemName: string): Promise<boolean> {
    if (sectionName === 'Shortcuts') return false;

    console.log('  → Strategy: ui-two-level (reference flow)');
    const opened = await this.openSidebarOnce();
    if (!opened) {
      console.log('  ✗ Sidebar not available after single burger click');
      return false;
    }

    await this.page.locator('main article li > p').getByText(sectionName, { exact: true }).click();
    await this.page.waitForTimeout(600);

    const flyout = this.page.locator('[id^="cdk-drop-list"]').getByText(subItemName, { exact: true });
    if (!(await flyout.isVisible({ timeout: 3000 }).catch(() => false))) {
      console.log('  ✗ Flyout item not visible');
      return false;
    }

    await flyout.click();
    await this.page.waitForLoadState('networkidle').catch(() => undefined);
    await this.page.waitForTimeout(1500);

    return this.hasListing();
  }

  private async tryDirectPath(pathSegment: string): Promise<boolean> {
    const url = `${this.baseUrl}${pathSegment.startsWith('/') ? pathSegment : `/${pathSegment}`}`;
    console.log(`  → Strategy: direct-url ${url}`);

    const apiPromise = this.waitForListingApi();
    await this.page.goto(url, { waitUntil: 'domcontentloaded', timeout: 45000 });
    await this.page.waitForTimeout(2500);

    const listing = await this.hasListing();
    const api = await apiPromise;
    if (listing) {
      if (api) console.log(`  ✓ Listing API: ${api.split('?')[0]}`);
      return true;
    }
    return false;
  }

  private async tryTextClickFallback(sectionName: string, subItemName: string): Promise<boolean> {
    console.log('  → Strategy: text-click-fallback (single pass)');
    await this.page.goto(`${this.baseUrl}/`, { waitUntil: 'domcontentloaded', timeout: 45000 });
    await this.page.waitForTimeout(2000);
    await this.openSidebarOnce();

    const matches = this.page.locator('main').getByText(subItemName, { exact: true });
    const count = await matches.count();
    for (let i = 0; i < count; i++) {
      await matches.nth(i).click();
      await this.page.waitForTimeout(2000);
      if (await this.hasListing()) return true;
    }
    return false;
  }

  async navigateToListing(sectionName: string, subItemName: string): Promise<NavigationResult> {
    console.log(`\n🧭 Navigate: [${sectionName}] → ${subItemName}`);

    const strategies: Strategy[] = [
      {
        id: 'ui-two-level',
        run: () => this.tryUiTwoLevel(sectionName, subItemName),
      },
    ];

    const knownPath = getKnownPath(sectionName, subItemName, this.map);
    if (knownPath) {
      strategies.push({
        id: 'route-map',
        run: () => this.tryDirectPath(knownPath),
      });
    }

    for (const slug of slugPathCandidates(subItemName)) {
      if (slug === knownPath) continue;
      strategies.push({
        id: `slug-url:${slug}`,
        run: () => this.tryDirectPath(slug),
      });
    }

    strategies.push({
      id: 'text-click-fallback',
      run: () => this.tryTextClickFallback(sectionName, subItemName),
    });

    for (const strategy of strategies) {
      try {
        const ok = await strategy.run();
        if (ok) {
          const url = this.page.url();
          const pathOnly = url.replace(this.baseUrl, '') || `/${toSlug(subItemName)}`;
          saveRouteEntry(sectionName, subItemName, {
            path: pathOnly.startsWith('/') ? pathOnly : `/${pathOnly}`,
            method: strategy.id.split(':')[0],
          });
          console.log(`  ✅ Reached listing via ${strategy.id}: ${url}`);
          return { method: strategy.id, url };
        }
      } catch (error) {
        console.log(`  ✗ ${strategy.id}: ${error instanceof Error ? error.message : String(error)}`);
      }
    }

    throw new Error(`Could not reach listing for ${sectionName} / ${subItemName} (all strategies exhausted)`);
  }
}
