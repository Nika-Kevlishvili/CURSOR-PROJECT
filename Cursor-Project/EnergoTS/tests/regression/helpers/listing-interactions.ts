import { Page, Locator } from '@playwright/test';
import { ScreenshotManager } from './screenshot-manager';
import {
  analyzeSortOrder,
  findColumnForFilter,
  findSortColumn,
  getAllVisibleRowTexts,
  getTableColumnValues,
  getTableHeaderLabels,
  verifyRowsContainSearchToken,
  verifyValuesMatchFilter,
} from './table-utils';

function slugLabel(label: string): string {
  return label.replace(/[^a-z0-9]+/gi, '-').toLowerCase().replace(/^-|-$/g, '').slice(0, 48);
}

async function waitForTableSettle(page: Page): Promise<void> {
  await page.waitForTimeout(1200);
  await page
    .locator('.p-progress-spinner, .loading, [class*="spinner"]')
    .first()
    .waitFor({ state: 'hidden', timeout: 15000 })
    .catch(() => undefined);
}

async function resetListingState(page: Page): Promise<void> {
  const clearSearch = page.locator('button.circle.clear, button[title*="Clear" i], .search-clear').first();
  if (await clearSearch.isVisible({ timeout: 1000 }).catch(() => false)) {
    await clearSearch.click().catch(() => undefined);
    await waitForTableSettle(page);
  }

  const resetFilters = page
    .locator('button.circle.reset, button[title*="Reset" i], button.filter-reset')
    .first();
  if (await resetFilters.isVisible({ timeout: 1000 }).catch(() => false)) {
    await resetFilters.click().catch(() => undefined);
    await waitForTableSettle(page);
  }
}

function isLikelyOpaqueId(token: string): boolean {
  const compact = token.replace(/\s+/g, '');
  return /^[A-Z0-9_-]{6,}$/i.test(compact) && !/[а-яА-ЯёЁ]/.test(token);
}

async function pickFilterOption(page: Page): Promise<string | null> {
  await page.waitForTimeout(600);

  const overlayRoots = [
    page.locator('.cdk-overlay-container .cdk-overlay-pane:visible').last(),
    page.locator('.p-multiselect-panel:visible').last(),
    page.locator('.p-overlaypanel:visible').last(),
    page.locator('[role="listbox"]:visible').last(),
  ];

  for (const root of overlayRoots) {
    if (!(await root.isVisible({ timeout: 2000 }).catch(() => false))) continue;

    const labels = root.locator('label');
    const labelCount = await labels.count();
    for (let j = 0; j < labelCount; j++) {
      const lbl = labels.nth(j);
      const text = (await lbl.innerText().catch(() => '')).replace(/\s+/g, ' ').trim();
      if (!text || /^select all$/i.test(text) || /^всички$/i.test(text)) continue;
      await lbl.click({ timeout: 3000 });
      return text;
    }

    const items = root.locator('li.p-multiselect-item, li[role="option"], li.p-element, li');
    const itemCount = await items.count();
    for (let j = 0; j < Math.min(itemCount, 15); j++) {
      const item = items.nth(j);
      const text = (await item.innerText().catch(() => '')).replace(/\s+/g, ' ').trim();
      if (!text || /^select all$/i.test(text) || /^всички$/i.test(text)) continue;
      await item.click({ timeout: 3000 });
      return text;
    }

    const checkbox = root.locator('input[type="checkbox"]').nth(1);
    if (await checkbox.isVisible({ timeout: 1000 }).catch(() => false)) {
      const text = await checkbox
        .evaluate((el) => {
          const label = el.closest('label') ?? el.parentElement?.querySelector('label');
          return (label?.textContent || '').replace(/\s+/g, ' ').trim();
        })
        .catch(() => '');
      await checkbox.click({ force: true });
      if (text && !/^select all$/i.test(text)) return text;
    }
  }

  return null;
}

async function getSearchToken(page: Page): Promise<string> {
  const headers = await getTableHeaderLabels(page);
  const nameIndex = headers.findIndex((h) => /^name$/i.test(h) || /group name/i.test(h));
  if (nameIndex >= 0) {
    const nameValues = await getTableColumnValues(page, nameIndex, 5);
    for (const name of nameValues) {
      if (!name || name.length < 3) continue;
      if (!isLikelyOpaqueId(name) && name.length >= 4) return name.slice(0, 32);

      const words = name
        .split(/\s+/)
        .map((w) => w.trim())
        .filter((w) => w.length >= 4 && !isLikelyOpaqueId(w) && !/^\d+$/.test(w));
      if (words[0]) return words[0].slice(0, 32);
    }
  }

  const firstRowText = await page.locator('tbody tr').first().innerText().catch(() => '');
  const fallback =
    firstRowText
      .split(/\s+/)
      .map((w) => w.trim())
      .find(
        (w) =>
          w.length >= 4 &&
          !/^\d+$/.test(w) &&
          !isLikelyOpaqueId(w) &&
          !/^(active|new|legal entity|no|yes)$/i.test(w),
      ) || 'test';
  return fallback;
}

async function clearFilterDropdown(page: Page, dropdown: Locator): Promise<void> {
  await dropdown.click({ timeout: 3000 }).catch(() => undefined);
  await page.waitForTimeout(400);
  const selectAll = page
    .locator('.p-multiselect-panel:visible label, .p-overlaypanel:visible label')
    .filter({ hasText: /select all|всички/i })
    .first();
  if (await selectAll.isVisible({ timeout: 1500 }).catch(() => false)) {
    const checked = await selectAll
      .evaluate((el) => {
        const input = el.parentElement?.querySelector('input[type="checkbox"]') as HTMLInputElement | null;
        return input?.checked ?? false;
      })
      .catch(() => false);
    if (!checked) await selectAll.click();
    if (checked) await selectAll.click();
  }
  await page.keyboard.press('Escape');
  await page.waitForTimeout(400);
}

/** Apply filters and verify column cell values match selected option (not row count). */
async function captureAppliedFilters(
  page: Page,
  screenshots: ScreenshotManager,
  stepPrefix: string,
  subItemName: string,
): Promise<void> {
  const multiselects = page.locator('phx-filters-multiselect-dropdown');
  const count = await multiselects.count();
  const max = Math.min(count, 2);
  if (max === 0) {
    console.log('  · No multiselect filters on listing — skip');
    return;
  }

  const headers = await getTableHeaderLabels(page);

  for (let i = 0; i < max; i++) {
    const dropdown = multiselects.nth(i);
    const filterLabel =
      (await dropdown
        .evaluate((el) => (el.textContent || '').split('\n')[0]?.trim())
        .catch(() => '')) || `filter-${i + 1}`;

    try {
      await dropdown.click({ timeout: 5000 });
      await page.waitForTimeout(500);
      const selectedOption = await pickFilterOption(page);
      if (!selectedOption) {
        console.warn(`  ⚠ Could not pick filter option for ${filterLabel}`);
        await page.keyboard.press('Escape');
        continue;
      }

      await page.keyboard.press('Escape').catch(() => undefined);
      await waitForTableSettle(page);

      const column = findColumnForFilter(filterLabel, headers);
      let checkItems: string[];
      let verified = false;

      if (!column) {
        console.warn(`  ⚠ No table column mapped for filter "${filterLabel}"`);
        checkItems = [
          `Filter "${filterLabel}" option selected: "${selectedOption}"`,
          'Column mapping not found — content check skipped',
        ];
      } else {
        const cellValues = await getTableColumnValues(page, column.index, 25);
        const result = verifyValuesMatchFilter(cellValues, selectedOption);
        verified = result.verified;

        console.log(
          `  ${verified ? '✓' : '⚠'} Filter "${filterLabel}" → column "${column.label}": ${result.matched}/${result.total} rows match "${selectedOption}"`,
        );
        if (!verified && result.mismatches.length) {
          console.log(`    Mismatches: ${result.mismatches.join(' | ')}`);
        }

        checkItems = [
          `Filter "${filterLabel}" option: "${selectedOption}"`,
          `Mapped column: "${column.label}"`,
          verified
            ? `Content verified: ${result.matched}/${result.total} rows match filter`
            : `Content mismatch: ${result.matched}/${result.total} match — samples: ${result.mismatches.join('; ') || 'n/a'}`,
        ];
      }

      await screenshots.captureViewport(
        'listing',
        `${stepPrefix}-filter-${slugLabel(filterLabel)}-${verified ? 'verified' : 'check'}`,
        `${subItemName} — filter ${filterLabel} = "${selectedOption}"`,
        checkItems,
      );

      await clearFilterDropdown(page, dropdown);
      await waitForTableSettle(page);
    } catch (error) {
      console.warn(`  ⚠ Filter apply skipped (${filterLabel}):`, error);
      await page.keyboard.press('Escape').catch(() => undefined);
    }
  }
}

async function captureSearch(
  page: Page,
  screenshots: ScreenshotManager,
  stepPrefix: string,
  subItemName: string,
): Promise<void> {
  const searchInput = page
    .locator('phx-input[formcontrolname="search"] input')
    .or(page.getByPlaceholder(/search/i))
    .first();

  if (!(await searchInput.isVisible({ timeout: 2500 }).catch(() => false))) {
    console.log('  · No search field on listing — skip');
    return;
  }

  const token = await getSearchToken(page);

  try {
    await searchInput.click({ timeout: 5000 });
    await page.waitForTimeout(250);
    await searchInput.fill(token, { timeout: 8000 });
    await waitForTableSettle(page);

    const rowTexts = await getAllVisibleRowTexts(page, 25);
    const result = verifyRowsContainSearchToken(rowTexts, token);

    console.log(
      `  ${result.verified ? '✓' : '⚠'} Search "${token}": ${result.matched}/${result.total} rows contain token`,
    );

    await screenshots.captureViewport(
      'listing',
      `${stepPrefix}-search-${result.verified ? 'verified' : 'check'}`,
      `${subItemName} — search "${token}"`,
      [
        `Search token: "${token}"`,
        result.verified
          ? `Content verified: all ${result.total} visible rows contain "${token}"`
          : `Content mismatch: ${result.matched}/${result.total} rows contain token`,
        result.mismatches.length ? `Non-matching rows (sample): ${result.mismatches.join(' | ')}` : 'All rows match',
      ],
    );

    const clearBtn = page.locator('button.circle.clear, button[title*="Clear" i]').first();
    if (await clearBtn.isVisible({ timeout: 1500 }).catch(() => false)) {
      await clearBtn.click();
    } else {
      await searchInput.clear();
    }
    await waitForTableSettle(page);
  } catch (error) {
    console.warn('  ⚠ Search skipped:', error);
  }
}

async function captureVerifiedSort(
  page: Page,
  screenshots: ScreenshotManager,
  stepPrefix: string,
  subItemName: string,
): Promise<void> {
  const sortCol = await findSortColumn(page, [
    /type of connection/i,
    /date of creation/i,
    /^name$/i,
    /group name/i,
    /^status$/i,
    /^type$/i,
    /number of connected/i,
    /reason/i,
  ]);

  if (!sortCol) {
    console.log('  · No sortable table column found — skip');
    return;
  }

  const headers = page.locator('thead th');
  const th = headers.nth(sortCol.index);
  const before = await getTableColumnValues(page, sortCol.index, 10);

  let after = [...before];
  let clicks = 0;
  let direction: 'asc' | 'desc' | 'unchanged' = 'unchanged';

  for (clicks = 0; clicks < 3; clicks++) {
    await th.click();
    await waitForTableSettle(page);
    after = await getTableColumnValues(page, sortCol.index, 10);
    const analysis = analyzeSortOrder(after);

    if (analysis === 'asc' || analysis === 'desc') {
      direction = analysis;
      break;
    }
  }

  const verified = direction === 'asc' || direction === 'desc';
  const sample = after.slice(0, 5).join(' | ');

  console.log(
    `  ${verified ? '✓' : '⚠'} Sort "${sortCol.label}": ${verified ? direction : 'not verified'} — [${sample}]`,
  );

  await screenshots.captureViewport(
    'listing',
    `${stepPrefix}-sort-${slugLabel(sortCol.label)}-${verified ? direction : 'unverified'}`,
    `${subItemName} — sort on "${sortCol.label}"`,
    [
      `Column "${sortCol.label}" sorted ${verified ? direction.toUpperCase() : 'NOT verified'}`,
      `Values check: ${after.join(' → ')}`,
      verified ? 'Column values are in correct sort order' : 'Sort order could not be confirmed from cell values',
    ],
  );
}

export async function captureListingInteractions(
  page: Page,
  screenshots: ScreenshotManager,
  stepPrefix: string,
  subItemName: string,
): Promise<void> {
  console.log('  → Listing interactions: filter/search/sort content checks');
  await resetListingState(page);
  await captureAppliedFilters(page, screenshots, stepPrefix, subItemName);
  await captureSearch(page, screenshots, stepPrefix, subItemName);
  await captureVerifiedSort(page, screenshots, stepPrefix, subItemName);
}
