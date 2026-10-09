import { Page } from "@playwright/test";

function getFrontendUrl(): string {
    const backendUrl = (process.env.BASE_URL || 'https://devapps.energo-pro.bg/backend/phoenix1-dev/').replace(/\/$/, '');

    if (backendUrl.startsWith('https://devapps.energo-pro.bg/backend/phoenix1-dev'))    return 'https://devapps.energo-pro.bg/app/phoenix1-dev/';
    if (backendUrl.startsWith('http://10.236.20.81:8091'))                              return 'http://10.236.20.81:8080/';
    if (backendUrl.startsWith('https://devapps.energo-pro.bg/backend/phoenix2-dev'))    return 'https://devapps.energo-pro.bg/app/phoenix2-dev/';
    if (backendUrl.startsWith('https://testapps.energo-pro.bg/backend/phoenix-epres'))  return 'https://testapps.energo-pro.bg/app/phoenix-epres/';
    if (backendUrl.startsWith('http://10.236.20.31:'))                                  return 'http://10.236.20.31:8082/';
    if (backendUrl.startsWith('http://10.236.20.81:8094')) return 'http://10.236.20.31:8082/';
    // Explicit override still supported via DEVENV
    if (process.env.DEVENV) return process.env.DEVENV;

    throw new Error(`No frontend URL configured for BASE_URL: ${backendUrl}. Set DEVENV or add a mapping in MainPage.ts.`);
}

export async function OpenMainPage(page: Page, token: string): Promise<Page> {
    const frontendUrl = getFrontendUrl();
    await page.goto(`${frontendUrl}?jwt=${token}`, { waitUntil: 'commit' });
    return page;
}