import { test } from '@playwright/test';
import { setupAuthenticatedPage, MenuDiscovery, MENU_STRUCTURE_PATH } from '../helpers';

test.describe('Phoenix Menu Discovery', () => {
  test('Discover and save sidebar menu structure', async ({ page, baseURL }) => {
    test.setTimeout(300000);

    await setupAuthenticatedPage(page);
    await page.goto(baseURL || '');
    await page.waitForLoadState('networkidle');
    await page.waitForTimeout(2000);

    const menu = new MenuDiscovery(page);
    const structure = await menu.discoverFullStructure('dev', baseURL || '');
    await menu.saveStructure(structure, MENU_STRUCTURE_PATH);

    console.log('\n✅ Menu discovery complete');
    for (const section of structure.sections) {
      console.log(`  ${section.name}: ${section.subItems.map((s) => s.name).join(' | ') || '(no sub-items)'}`);
    }
  });
});
