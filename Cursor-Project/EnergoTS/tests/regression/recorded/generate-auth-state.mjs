import { chromium } from '@playwright/test';
import fs from 'fs';
import path from 'path';
import { fileURLToPath } from 'url';

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const tokenPath = path.resolve(__dirname, '../../../fixtures/token.json');
const outputPath = path.resolve(__dirname, 'auth-state.json');
const frontendUrl = 'http://10.236.20.11:8080/';

const { token } = JSON.parse(fs.readFileSync(tokenPath, 'utf-8'));

const browser = await chromium.launch({ headless: true });
const context = await browser.newContext();
await context.addInitScript((jwt) => {
  localStorage.setItem('token', jwt);
  localStorage.setItem('jwt', jwt);
}, token);

const page = await context.newPage();
await page.goto(frontendUrl, { waitUntil: 'networkidle' });
await page.waitForTimeout(2000);

await context.storageState({ path: outputPath });
await browser.close();

console.log(`Auth state saved: ${outputPath}`);
