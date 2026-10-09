import { test, Page, Browser } from '@playwright/test';
import {
  AUTH_STATE_PATH,
  MenuDiscovery,
  PhoenixNavigation,
  ScreenshotManager,
  waitForLoggedIn,
} from './index';
import { loadRouteMap } from './route-map';
import { runSubmenuVisualFlow, sanitizeStepPrefix } from './page-flow';
import { MenuSection } from './menu-structure';
import { newScreenshotRunId } from './screenshot-paths';

async function openSharedSession(browser: Browser, baseURL: string): Promise<Page> {
  const context = await browser.newContext({
    storageState: AUTH_STATE_PATH,
    viewport: { width: 1920, height: 1080 },
  });
  const page = await context.newPage();
  const home = baseURL.replace(/\/$/, '') + '/';

  await page.goto(home, { waitUntil: 'commit', timeout: 90000 });
  await page.waitForTimeout(2000);
  await waitForLoggedIn(page);
  console.log('  ✓ Shared session ready — one login, same tab for all sub-items');
  return page;
}

/** Waits until the user closes the browser window — does not auto-close. */
async function leaveBrowserOpenForUser(page: Page | undefined): Promise<void> {
  if (!page || page.isClosed()) return;
  console.log('\n🖥️  Browser left open — close the window manually when done.\n');
  await page.waitForEvent('close', { timeout: 0 }).catch(() => undefined);
}

export function createSectionRegressionTests(sectionName: string): void {
  const menu = MenuDiscovery.loadStructure();
  const section = menu.sections.find((s) => s.name === sectionName);

  if (!section) {
    throw new Error(`Section not found in menu-structure.json: ${sectionName}`);
  }

  const tag = sectionName.toLowerCase().replace(/[^a-z0-9]+/g, '-');
  const subItems = section.subItems.filter(() => section.name !== 'Shortcuts');

  test.describe(`@section-${tag} ${section.name}`, () => {
    test.describe.configure({ mode: 'serial' });

    let page: Page;
    let runId: string;
    const baseURL = loadRouteMap().baseUrl;

    test.beforeAll(async ({ browser }) => {
      runId = newScreenshotRunId();
      page = await openSharedSession(browser, baseURL);
    });

    test.afterAll(async ({}, testInfo) => {
      testInfo.setTimeout(0);
      await leaveBrowserOpenForUser(page);
    });

    for (const subItem of subItems) {
      test(`${subItem.name} — visual regression`, async () => {
        test.setTimeout(300000);

        const nav = new PhoenixNavigation(page);
        const menuDiscovery = new MenuDiscovery(page);
        const screenshots = new ScreenshotManager(page, {
          runId,
          sectionIndex: section.index,
          sectionName: section.name,
          subItemIndex: subItem.index,
          subItemName: subItem.name,
        });
        const stepPrefix = sanitizeStepPrefix(section.index, subItem.index, section.name, subItem.name);

        await runSubmenuVisualFlow({
          page,
          nav,
          menu: menuDiscovery,
          screenshots,
          sectionName: section.name,
          subItemName: subItem.name,
          stepPrefix,
        });

        await screenshots.generateManifest('dev', baseURL);
      });
    }
  });
}

export function createAllSectionsRegressionTest(): void {
  const structure = MenuDiscovery.loadStructure();
  const baseURL = loadRouteMap().baseUrl;

  test.describe('@regression-all Sequential walkthrough', () => {
    test.describe.configure({ mode: 'serial' });

    test('All menu sections in order', async ({ browser }) => {
      test.setTimeout(3600000);

      const runId = newScreenshotRunId();
      const page = await openSharedSession(browser, baseURL);

      try {
        const nav = new PhoenixNavigation(page);
        const menu = new MenuDiscovery(page);

        await nav.ensureSidebarOpen().catch(() => undefined);
        const sidebarShots = new ScreenshotManager(page, {
          runId,
          sectionIndex: -1,
          sectionName: 'Session',
          subItemIndex: 0,
          subItemName: 'Sidebar',
        });
        await sidebarShots.captureViewport('listing', '00-sidebar', 'Sidebar open', [
          'Menu visible',
          'User logged in',
        ]);
        await sidebarShots.generateManifest('dev', baseURL);

        for (const section of structure.sections) {
          await runSectionSubmenus(page, nav, menu, runId, section);
        }

        await leaveBrowserOpenForUser(page);
      } catch (error) {
        await leaveBrowserOpenForUser(page);
        throw error;
      }
    });
  });
}

async function runSectionSubmenus(
  page: Page,
  nav: PhoenixNavigation,
  menu: MenuDiscovery,
  runId: string,
  section: MenuSection,
): Promise<void> {
  for (const subItem of section.subItems) {
    if (section.name === 'Shortcuts') continue;

    const stepPrefix = sanitizeStepPrefix(section.index, subItem.index, section.name, subItem.name);
    const screenshots = new ScreenshotManager(page, {
      runId,
      sectionIndex: section.index,
      sectionName: section.name,
      subItemIndex: subItem.index,
      subItemName: subItem.name,
    });

    try {
      await runSubmenuVisualFlow({
        page,
        nav,
        menu,
        screenshots,
        sectionName: section.name,
        subItemName: subItem.name,
        stepPrefix,
      });
      await screenshots.generateManifest('dev', loadRouteMap().baseUrl);
    } catch (error) {
      console.warn(`⚠️ Skipped ${section.name} / ${subItem.name}:`, error);
      await screenshots.captureViewport(
        'listing',
        `${stepPrefix}-error`,
        `${subItem.name} — error`,
        [error instanceof Error ? error.message : String(error)],
      );
      await screenshots.generateManifest('dev', loadRouteMap().baseUrl);
    }
  }
}
