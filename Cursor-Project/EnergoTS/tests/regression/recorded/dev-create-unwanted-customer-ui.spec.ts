import { test, expect } from '@playwright/test';
import { setupAuthenticatedPage } from '../helpers';

/**
 * DEV-DATA — UI create Unwanted Customer on Dev (frontend automation).
 *
 * Portal (Dev): https://devapps.energo-pro.bg/app/phoenix1-dev
 *   (direct http://10.236.20.11:8080 serves broken JS MIME — do not use)
 *
 * Routes (phoenix-ui unwanted-customers-routing):
 *   list   → /unwanted-customers
 *   create → /unwanted-customers/create
 *
 * Swagger (dev): POST /unwanted-customer → UnwantedCustomerCreateRequest
 *   required: identificationNumber, name, unwantedCustomerReasonId,
 *             contractCreateRestriction, orderCreateRestriction
 *
 * Reference:
 *   - tests/regression/recorded/reference-customers-flow.spec.ts (auth + menu)
 *   - tests/customers/unwantedCustomer.spec.ts (API create happy path)
 *   - phoenix-ui create-unwanted-customer.component.html (stable element ids)
 */

const DEV_DATA_KEY = 'DEV-DATA';
const DEV_PORTAL_BASE = 'https://devapps.energo-pro.bg/app/phoenix1-dev';

function uniqueIdentificationNumber(): string {
  // Pattern: CAPITAL_LETTERS_AND_NUMBERS; digits-only (9 chars).
  const suffix = Date.now().toString().slice(-8);
  return `9${suffix}`;
}

function uniqueName(): string {
  // Swagger/UI name pattern: uppercase Latin/Cyrillic + digits + limited symbols.
  return `AUTO UNWANTED ${Date.now()}`;
}

function portalUrl(path: string): string {
  const base = DEV_PORTAL_BASE.replace(/\/$/, '');
  const suffix = path.startsWith('/') ? path : `/${path}`;
  return `${base}${suffix}`;
}

test.describe(`[${DEV_DATA_KEY}]: Unwanted Customer UI create`, () => {
  // Tall viewport helps Reason nomenclature open downward (not under app header).
  test.use({ viewport: { width: 1920, height: 1400 } });

  test(`[${DEV_DATA_KEY}]: Create Unwanted Customer via Phoenix UI (Dev)`, async ({ page }) => {
    const identificationNumber = uniqueIdentificationNumber();
    const name = uniqueName();
    let createResponseStatus: number | undefined;
    let createdId: number | undefined;

    await test.step('Precondition: inject JWT into browser storage', async () => {
      await setupAuthenticatedPage(page);
    });

    await test.step('Open Unwanted Customers listing', async () => {
      await page.goto(portalUrl('/unwanted-customers'), {
        waitUntil: 'domcontentloaded',
        timeout: 90000,
      });
      await page.waitForLoadState('networkidle').catch(() => undefined);
      // Host + inner button share the same id — target the clickable button.
      await expect(page.locator('button#unWantedCustomerListCreateButton')).toBeVisible({
        timeout: 90000,
      });
    });

    await test.step('Open create form', async () => {
      await page.locator('button#unWantedCustomerListCreateButton').click();
      await page.waitForURL(/\/unwanted-customers\/create/, { timeout: 30000 });
      await expect(
        page.locator('input#unWantedCustomerCreateUicPersonalNumberInput'),
      ).toBeVisible({
        timeout: 30000,
      });
    });

    await test.step('Fill mandatory fields', async () => {
      const uic = page.locator('input#unWantedCustomerCreateUicPersonalNumberInput');
      await uic.click();
      await uic.fill(identificationNumber);

      const nameInput = page.locator('input#unWantedCustomerCreatePageNameInput');
      await nameInput.click();
      await nameInput.fill(name);

      // Force dropdown to open DOWNWARD: keep Reason near top of viewport with space below
      // (openDropdownOptionTop is true when field bottom + panel height >= window.innerHeight).
      await page.evaluate(() => {
        const el = document.querySelector(
          '#unWantedCustomerCreatePageReasonDropdownDiv',
        ) as HTMLElement | null;
        if (!el) return;
        const top = el.getBoundingClientRect().top + window.scrollY;
        window.scrollTo({ top: Math.max(0, top - 140), behavior: 'instant' as ScrollBehavior });
      });
      await page.waitForTimeout(400);
      const reasonHost = page.locator('#unWantedCustomerCreatePageReasonDropdownDiv');
      await reasonHost.click();
      const firstOption = page.locator('#unWantedCustomerCreatePageReasonDropdownOption0');
      await expect(firstOption).toBeVisible({ timeout: 20000 });
      await firstOption.click({ timeout: 15000 });
      await expect(firstOption).toBeHidden({ timeout: 10000 });

      await page
        .locator('#unWantedCustomerCreatePageRestrictedToCreateContractCheckBox')
        .first()
        .click();
      await page
        .locator('#unWantedCustomerCreatePageRestrictedToCreateOrderCheckBox')
        .first()
        .click();
    });

    await test.step('Save and assert POST /unwanted-customer succeeds', async () => {
      const createResponsePromise = page.waitForResponse(
        (res) =>
          res.url().includes('/unwanted-customer') &&
          res.request().method() === 'POST' &&
          !res.url().includes('filter') &&
          !res.url().includes('check'),
        { timeout: 60000 },
      );

      await page.locator('#unWantedCustomerCreatePageSaveButton').first().click();
      const createResponse = await createResponsePromise;
      createResponseStatus = createResponse.status();
      expect(createResponseStatus, `POST /unwanted-customer status`).toBeLessThan(300);

      try {
        const body = await createResponse.json();
        createdId = body?.id;
      } catch {
        createdId = undefined;
      }

      await page.waitForURL(/\/unwanted-customers\/?(\?.*)?$/, { timeout: 60000 });
    });

    await test.step('Verify created row appears in listing search', async () => {
      const search = page.locator('input#unWantedCustomerListFilterSearchInput');
      await expect(search).toBeVisible({ timeout: 30000 });
      await search.click();
      await search.fill(identificationNumber);
      await page.locator('button#unWantedCustomerListFilterSearchButton, #unWantedCustomerListFilterSearchButton button').first().click();
      await page.waitForLoadState('networkidle').catch(() => undefined);
      await page.waitForTimeout(1500);

      const rowText = page.locator('tbody tr, .ag-row').filter({ hasText: identificationNumber });
      await expect(rowText.first()).toBeVisible({ timeout: 30000 });
    });

    console.log(
      `[${DEV_DATA_KEY}] Unwanted Customer UI create OK — identificationNumber=${identificationNumber}, name=${name}, id=${createdId ?? 'n/a'}, status=${createResponseStatus}`,
    );
  });
});
