import { Page } from '@playwright/test';
import { PhoenixNavigation } from './navigation';
import { ScreenshotManager } from './screenshot-manager';
import { MenuDiscovery } from './menu-discovery';
import { captureListingInteractions } from './listing-interactions';

export interface SubmenuFlowOptions {
  page: Page;
  nav: PhoenixNavigation;
  menu: MenuDiscovery;
  screenshots: ScreenshotManager;
  sectionName: string;
  subItemName: string;
  stepPrefix: string;
}

export async function runSubmenuVisualFlow(options: SubmenuFlowOptions): Promise<void> {
  const { page, nav, menu, screenshots, sectionName, subItemName, stepPrefix } = options;

  console.log(`\n📍 [${sectionName}] → ${subItemName}`);

  await menu.clickSubmenuItem(sectionName, subItemName);
  await nav.waitForNoLoadingSpinner();

  await screenshots.captureFullPage(
    'listing',
    `${stepPrefix}-list`,
    `${sectionName} / ${subItemName} — list view`,
    [
      'Page loaded (not blank)',
      'Listing or main content visible',
      'Table/grid or form area rendered',
      'Action buttons visible if applicable',
    ],
  );

  await screenshots.captureWithHorizontalScroll(
    'listing',
    `${stepPrefix}-list-scroll`,
    `${subItemName} — horizontal scroll`,
    ['Wide table/content captured left and right'],
  );

  await captureListingInteractions(page, screenshots, stepPrefix, subItemName);

  const openedPreview = await nav.openFirstListPreview();
  if (openedPreview) {
    await nav.waitForNoLoadingSpinner();

    await screenshots.captureFullPage(
      'object',
      `${stepPrefix}-preview`,
      `${subItemName} — preview/detail`,
      [
        'Detail/preview opened',
        'Header or title visible',
        'Main fields/sections visible',
        'Tabs or sub-panels if applicable',
        'Action buttons state',
      ],
    );

    await page.goBack({ waitUntil: 'networkidle' }).catch(() => undefined);
    await page.waitForTimeout(500);
  } else {
    await screenshots.captureViewport(
      'listing',
      `${stepPrefix}-no-preview`,
      `${subItemName} — no list row to open`,
      ['List empty or preview not applicable for this page'],
    );
  }
}

export function sanitizeStepPrefix(sectionIndex: number, subIndex: number, sectionName: string, subName: string): string {
  const slug = (value: string) => value.toLowerCase().replace(/[^a-z0-9]+/g, '-').replace(/^-|-$/g, '');
  return `${String(sectionIndex + 1).padStart(2, '0')}-${String(subIndex + 1).padStart(2, '0')}-${slug(sectionName)}-${slug(subName)}`;
}
