import { test } from '@playwright/test';
import { setupAuthenticatedPage, MenuDiscovery } from '../helpers';

test('debug submenu DOM after expand', async ({ page, baseURL }) => {
  await setupAuthenticatedPage(page);
  await page.goto(baseURL || '');
  await page.waitForLoadState('networkidle');
  await page.waitForTimeout(2000);

  const menu = new MenuDiscovery(page);
  await menu.ensureSidebarOpen();
  await menu.expandSection('Customers');

  const debug = await page.evaluate(() => {
    const article = document.querySelector('main article, article');
    const rootList = article?.querySelector('ul, ol') || article?.firstElementChild;
    if (!rootList) return { error: 'no list' };

    const customersLi = Array.from(rootList.children).find((li) =>
      (li.textContent || '').startsWith('Customers')
    );

    if (!customersLi) return { error: 'no customers li' };

    return {
      outerText: (customersLi.textContent || '').trim(),
      childNodes: Array.from(customersLi.children).map((c) => ({
        tag: c.tagName,
        className: c.className,
        text: (c.textContent || '').trim().slice(0, 200),
        childCount: c.children.length,
        children: Array.from(c.children).map((cc) => ({
          tag: cc.tagName,
          className: cc.className,
          text: (cc.textContent || '').trim().slice(0, 100),
        })),
      })),
    };
  });

  console.log(JSON.stringify(debug, null, 2));
});
