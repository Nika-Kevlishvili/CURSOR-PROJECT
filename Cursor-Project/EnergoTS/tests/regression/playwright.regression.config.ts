import { defineConfig, devices } from '@playwright/test';
import * as path from 'path';
import * as dotenv from 'dotenv';

dotenv.config({ path: path.resolve(__dirname, '../../.env') });

function getFrontendUrl(apiBaseUrl: string): string {
  const normalized = apiBaseUrl.replace(/\/$/, '');
  
  const mapping: Record<string, string> = {
    'http://10.236.20.11:8091': 'http://10.236.20.11:8080/',
    'http://10.236.20.31:8091': 'http://10.236.20.31:8080/',
    'http://10.236.20.81:8091': 'http://10.236.20.81:8080/',
    'http://10.236.20.81:8094': 'http://10.236.20.31:8082/',
    'https://testapps.energo-pro.bg/backend/phoenix-epres': 'https://testapps.energo-pro.bg/app/phoenix-epres/',
    'https://devapps.energo-pro.bg/backend/phoenix-dev2': 'https://devapps.energo-pro.bg/app/phoenix-dev2/',
  };
  
  return mapping[normalized] || normalized;
}

const apiBaseUrl = process.env.BASE_URL || 'http://10.236.20.11:8091';
const frontendUrl = getFrontendUrl(apiBaseUrl);
const authStatePath = path.resolve(__dirname, 'recorded/auth-state.json');

export default defineConfig({
  testDir: __dirname,
  timeout: 120000,
  retries: 0,
  
  reporter: [
    ['list'],
    ['json', { outputFile: path.resolve(__dirname, 'screenshots/regression-report.json') }],
  ],
  
  use: {
    baseURL: frontendUrl,
    storageState: authStatePath,

    headless: false,
    
    viewport: { width: 1920, height: 1080 },
    
    launchOptions: {
      slowMo: 500,
    },
    
    screenshot: 'only-on-failure',
    video: 'retain-on-failure',
    trace: 'retain-on-failure',
    
    actionTimeout: 30000,
    navigationTimeout: 60000,
  },
  
  globalSetup: require.resolve('./setup.ts'),

  /* One worker — shared session per describe; auth once in globalSetup */
  workers: 1,

  projects: [
    {
      name: 'visual-regression',
      use: {
        ...devices['Desktop Chrome'],
        channel: 'chrome',
        headless: false,
        launchOptions: {
          slowMo: 500,
        },
      },
    },
  ],
});
