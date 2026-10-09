import { Page } from '@playwright/test';
import * as fs from 'fs';
import * as path from 'path';

const tokenFilePath = path.resolve(__dirname, '../../../fixtures/token.json');
export const AUTH_STATE_PATH = path.resolve(__dirname, '../recorded/auth-state.json');

/** @deprecated Use storageState from globalSetup — no per-test init needed. */
export async function setupAuthenticatedPage(page: Page): Promise<string> {
  if (!fs.existsSync(tokenFilePath)) {
    throw new Error(`Token file not found: ${tokenFilePath}. Run global-setup first.`);
  }

  const tokenData = JSON.parse(fs.readFileSync(tokenFilePath, 'utf-8'));
  const token = tokenData.token;

  if (!token) {
    throw new Error('Token is empty in token.json');
  }

  await page.addInitScript((jwt: string) => {
    localStorage.setItem('token', jwt);
    localStorage.setItem('jwt', jwt);
  }, token);

  return token;
}

export function writeAuthState(token: string, authStatePath: string = AUTH_STATE_PATH): void {
  if (!fs.existsSync(authStatePath)) {
    throw new Error(`Auth state template not found: ${authStatePath}`);
  }

  const auth = JSON.parse(fs.readFileSync(authStatePath, 'utf-8')) as {
    origins: Array<{ origin: string; localStorage: Array<{ name: string; value: string }> }>;
  };

  for (const item of auth.origins[0].localStorage) {
    if (item.name === 'token' || item.name === 'jwt') {
      item.value = token;
    }
  }

  fs.writeFileSync(authStatePath, JSON.stringify(auth, null, 2));
  fs.writeFileSync(tokenFilePath, JSON.stringify({ token }, null, 2));
}

export async function getAuthToken(): Promise<string> {
  if (!fs.existsSync(tokenFilePath)) {
    throw new Error(`Token file not found: ${tokenFilePath}`);
  }

  const tokenData = JSON.parse(fs.readFileSync(tokenFilePath, 'utf-8'));
  return tokenData.token;
}

export async function waitForAuthReady(page: Page, timeout = 10000): Promise<void> {
  await page.waitForFunction(
    () => {
      const token = localStorage.getItem('token') || localStorage.getItem('jwt');
      return !!token;
    },
    { timeout },
  );
}

export async function waitForLoggedIn(page: Page, timeout = 20000): Promise<void> {
  await page.getByText('Nika Kevlishvili').waitFor({ state: 'visible', timeout }).catch(() => undefined);
}
