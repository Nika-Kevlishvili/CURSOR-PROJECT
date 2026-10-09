import { chromium } from '@playwright/test';
import fs from 'fs';
import path from 'path';
import { fileURLToPath } from 'url';
import dotenv from 'dotenv';

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const projectRoot = path.resolve(__dirname, '../..');
dotenv.config({ path: path.join(projectRoot, '.env') });

const FRONTEND = 'http://10.236.20.11:8080';
const CUSTOMERS_URL = `${FRONTEND}/customers`;
const AUTH_STATE_PATH = path.resolve(__dirname, 'recorded/auth-state.json');
const SHOT_BASE = path.resolve(__dirname, 'screenshots/manual-check/02-customers/01-customers');
const LISTING_DIR = path.join(SHOT_BASE, 'listing');
const OBJECT_DIR = path.join(SHOT_BASE, 'object');
const REPORT_PATH = path.join(SHOT_BASE, 'exploration-report.json');

async function refreshToken() {
  const res = await fetch(process.env.DEVAUTHAPI, {
    method: 'POST',
    headers: { Accept: '*/*', 'Content-Type': 'application/json' },
    body: JSON.stringify({ user: process.env.PORTAL_USER, password: process.env.PASSWORD }),
  });
  const body = await res.json();
  if (!body.jwt) throw new Error(`Auth failed status ${res.status}`);
  return body.jwt;
}

function updateAuthState(token) {
  const auth = JSON.parse(fs.readFileSync(AUTH_STATE_PATH, 'utf-8'));
  for (const item of auth.origins[0].localStorage) {
    if (item.name === 'token' || item.name === 'jwt') item.value = token;
  }
  fs.writeFileSync(AUTH_STATE_PATH, JSON.stringify(auth, null, 2));
}

async function shot(page, name, fullPage = false, kind = 'listing') {
  const dir = kind === 'object' ? OBJECT_DIR : LISTING_DIR;
  fs.mkdirSync(dir, { recursive: true });
  const file = path.join(dir, `${name}.png`);
  await page.screenshot({ path: file, fullPage });
  return file;
}

async function waitReady(page) {
  await page.waitForLoadState('domcontentloaded');
  await page.waitForTimeout(8000);
  await page.locator('phx-customers-list h2:has-text("Customers")').waitFor({ state: 'visible', timeout: 60000 });
}

const report = {
  url: CUSTOMERS_URL,
  timestamp: new Date().toISOString(),
  auth: { refreshed: false, method: 'recorded/auth-state.json + DEVAUTHAPI' },
  screenshots: [],
  filters: [],
  search: {},
  sort: {},
  createButton: {},
  createForm: { fieldLabels: [], mandatoryFields: [], validationMessages: [] },
  errors: [],
};

try {
  updateAuthState(await refreshToken());
  report.auth.refreshed = true;
} catch (e) {
  report.errors.push(`Token refresh: ${e.message}`);
}

const browser = await chromium.launch({ headless: true });
const page = await (await browser.newContext({ storageState: AUTH_STATE_PATH, viewport: { width: 1920, height: 1080 } })).newPage();

const collectForm = async () =>
  page.evaluate(() => {
    const fieldLabels = [];
    const mandatory = [];
    const validation = [];
    const labelMandatory = (text, input) => {
      const reasons = [];
      if (/\*/.test(text)) reasons.push('asterisk-in-label');
      if (input?.getAttribute('aria-required') === 'true') reasons.push('aria-required');
      if (input?.hasAttribute('required')) reasons.push('required');
      return reasons;
    };
    document.querySelectorAll('label').forEach((lbl) => {
      const text = lbl.textContent?.replace(/\s+/g, ' ').trim();
      if (!text || text.length > 150) return;
      const forId = lbl.getAttribute('for');
      const input = forId ? document.getElementById(forId) : lbl.parentElement?.querySelector('input, textarea, select');
      fieldLabels.push(text);
      const reasons = labelMandatory(text, input);
      if (reasons.length) mandatory.push({ label: text, reasons });
    });
    document.querySelectorAll('.text-red, .p-error, small[class*="error"], [class*="error-message"], .invalid-feedback').forEach((el) => {
      const msg = el.textContent?.replace(/\s+/g, ' ').trim();
      if (msg && msg.length < 200) validation.push(msg);
    });
    return {
      fieldLabels: [...new Set(fieldLabels)],
      mandatoryFields: mandatory,
      validationMessages: [...new Set(validation)],
      heading: document.querySelector('h1,h2')?.textContent?.replace(/\s+/g, ' ').trim(),
      url: location.href,
    };
  });

try {
  await page.goto(CUSTOMERS_URL, { waitUntil: 'domcontentloaded', timeout: 90000 });
  await waitReady(page);
  report.screenshots.push(await shot(page, '01-listing-initial'));

  report.filters = await page.locator('phx-customers-list-filters').evaluate((root) => {
    const out = [];
    root.querySelectorAll('phx-filters-multiselect-dropdown').forEach((el, i) => {
      const label = (el.innerText || '').split('\n')[0]?.trim() || `Multiselect ${i + 1}`;
      out.push({ label, selector: `phx-filters-multiselect-dropdown:nth-of-type(${i + 1})`, type: 'phx-filters-multiselect-dropdown' });
    });
    const allDd = root.querySelector('phx-base-dropdown[formcontrolname="all"]');
    if (allDd) out.push({ label: 'All (UIC filter)', selector: 'phx-base-dropdown[formcontrolname="all"]', type: 'phx-base-dropdown', placeholder: 'All' });
    const pp = root.querySelector('phx-input[formcontrolname="populatedPlace"]');
    if (pp) out.push({ label: 'Populated place', selector: 'phx-input[formcontrolname="populatedPlace"] input', type: 'text-input', placeholder: 'Populated place' });
    const excl = root.querySelector('phx-checkbox');
    if (excl) out.push({ label: 'Exclude old versions', selector: 'phx-customers-list-filters phx-checkbox', type: 'checkbox' });
    return out;
  });

  const multiselects = page.locator('phx-customers-list-filters phx-filters-multiselect-dropdown');
  for (let i = 0; i < await multiselects.count(); i++) {
    const label = report.filters[i]?.label || `filter-${i + 1}`;
    try {
      await multiselects.nth(i).click({ timeout: 5000 });
      await page.waitForTimeout(700);
      report.screenshots.push(await shot(page, `02-filter-${String(i + 1).padStart(2, '0')}-${label.replace(/[^a-z0-9]+/gi, '-').toLowerCase()}`));
      await page.keyboard.press('Escape');
      await page.waitForTimeout(400);
    } catch (e) {
      report.errors.push(`Filter open ${label}: ${e.message}`);
    }
  }

  try {
    const searchInput = page.locator('phx-input[formcontrolname="search"] input').first();
    report.search.selector = 'phx-input[formcontrolname="search"] input';
    const rowsBefore = await page.locator('tbody tr').count();
    await searchInput.fill('AUTOMATION');
    await page.waitForTimeout(2000);
    report.search.typed = 'AUTOMATION';
    report.search.rowCountBefore = rowsBefore;
    report.search.rowCountAfter = await page.locator('tbody tr').count();
    report.screenshots.push(await shot(page, '03-after-search'));
    await searchInput.clear();
    await page.waitForTimeout(1000);
  } catch (e) {
    report.search.error = e.message;
    report.errors.push(`Search: ${e.message}`);
  }

  try {
    const nameHeader = page.locator('thead th').filter({ hasText: /^Name$/ }).first();
    const headers = await page.locator('thead th').allInnerTexts();
    const firstBefore = await page.locator('tbody tr').first().innerText().catch(() => '');
    await nameHeader.click();
    await page.waitForTimeout(1500);
    const firstAfter = await page.locator('tbody tr').first().innerText().catch(() => '');
    report.sort = {
      availableHeaders: headers.map((h) => h.replace(/\s+/g, ' ').trim()).filter(Boolean),
      clickedColumn: 'Name',
      ariaSortAfterClick: await nameHeader.getAttribute('aria-sort'),
      firstRowChanged: firstBefore !== firstAfter,
    };
    report.screenshots.push(await shot(page, '04-after-sort'));
  } catch (e) {
    report.sort.error = e.message;
    report.errors.push(`Sort: ${e.message}`);
  }

  try {
    const createBtn = page.locator('phx-customers-list-filters phx-button button.circle.add, phx-customers-list-filters button.circle.add').first();
    await createBtn.waitFor({ state: 'visible', timeout: 10000 });
    report.createButton = {
      text: ((await createBtn.getAttribute('title')) || (await createBtn.getAttribute('aria-label')) || 'Add').trim(),
      selector: 'phx-customers-list-filters button.circle.add',
      className: await createBtn.getAttribute('class'),
      visibleText: (await createBtn.innerText()).trim(),
    };
    await createBtn.click();
    await page.waitForTimeout(6000);
    report.screenshots.push(await shot(page, '05-after-create-click', false, 'object'));
  } catch (e) {
    report.createButton = { fallbackNavigation: `${FRONTEND}/customers/create`, clickError: e.message };
    await page.goto(`${FRONTEND}/customers/create`, { waitUntil: 'domcontentloaded' });
    await page.waitForTimeout(8000);
    report.screenshots.push(await shot(page, '05-create-via-url', false, 'object'));
    report.errors.push(`Create click: ${e.message}`);
  }

  report.createForm = await collectForm();
  report.screenshots.push(await shot(page, '06-create-form-full', true, 'object'));

  try {
    const saveBtn = page
      .getByRole('button', { name: /Create Customer/i })
      .first();
    if (await saveBtn.isVisible({ timeout: 5000 })) {
      await saveBtn.click();
      await page.waitForTimeout(2500);
      const afterSave = await collectForm();
      report.createForm.validationMessages = afterSave.validationMessages;
      report.createForm.mandatoryFieldsAfterSaveAttempt = afterSave.mandatoryFields;
      report.screenshots.push(await shot(page, '07-after-save-validation', true, 'object'));
    } else {
      report.errors.push('Save validation probe: Create Customer button not visible');
    }
  } catch (e) {
    report.errors.push(`Save validation probe: ${e.message}`);
  }

  try {
    report.createForm.orangeBorderMandatory = await page.evaluate(() => {
      const out = [];
      document.querySelectorAll('input, textarea, select').forEach((input) => {
        const style = window.getComputedStyle(input);
        const bc = style.borderColor || '';
        const isOrange =
          bc.includes('255, 152') ||
          bc.includes('255, 165') ||
          input.className?.toString?.().includes('ng-invalid');
        if (!isOrange) return;
        const label =
          input.closest('phx-input, phx-base-dropdown, .field')?.querySelector('label')?.textContent?.trim() ||
          input.getAttribute('placeholder') ||
          input.getAttribute('name') ||
          'unknown';
        out.push(label.replace(/\s+/g, ' ').slice(0, 80));
      });
      return [...new Set(out)];
    });
    report.screenshots.push(await shot(page, '08-mandatory-orange-borders', true, 'object'));
  } catch (e) {
    report.errors.push(`Orange mandatory scan: ${e.message}`);
  }
} catch (e) {
  report.errors.push(`Run error: ${e.message}`);
  try {
    report.screenshots.push(await shot(page, '99-error-state', true, 'listing'));
  } catch {}
}

fs.writeFileSync(REPORT_PATH, JSON.stringify(report, null, 2));
console.log(JSON.stringify(report, null, 2));
await browser.close();
