import { defineConfig } from '@playwright/test';

const devUrl = 'https://devapps.energo-pro.bg/backend/phoenix2-dev'
const devFixUrl = 'http://10.236.20.81:8091'
const testUrl = 'https://testapps.energo-pro.bg/backend/phoenix-epres'
const dev2Url = 'https://devapps.energo-pro.bg/backend/phoenix2-dev'
const experimentUrl = 'http://10.236.20.81:8094'

export default defineConfig({
  testDir: './tests',
  /* Run tests in files in parallel */
  fullyParallel: true,
  /* Fail the build on CI if you accidentally left test.only in the source code. */
  forbidOnly: !!process.env.CI,
  /* Retry on CI only */
  retries: process.env.CI ? 1 : 0,
  /* Opt out of parallel tests on CI. */
  workers: process.env.CI ? 1 : undefined,
  /* Reporter to use. See https://playwright.dev/docs/test-reporters */
  reporter: [['json', { outputFile: 'playwright-report.json'}], ['html', { open: 'never' }]],

  /* Shared settings for all the projects below. See https://playwright.dev/docs/api/class-testoptions. */
  use: {
    /* Base URL to use in actions like `await page.goto('/')`. */
    baseURL: process.env.BASE_URL || devUrl,

    /* Collect trace when retrying the failed test. See https://playwright.dev/docs/trace-viewer */
    trace: 'on-first-retry',  
    headless: true,
    browserName: 'chromium',
    // headless: false, // Optional: set to true in CI
    screenshot: 'only-on-failure', // Take screenshot only on failures
    video: 'retain-on-failure', // Keep video for failed tests
    // viewport: { width: 1280, height: 720 },
  

  },

  /* Configure projects for major browsers */
  projects: [
    {
      name: 'setup',
      testMatch: /global-setup\.ts/,
      // No dependencies - runs independently
    },
    {
      name: 'main',
      // Removed dependencies: ['setup'] - now independent
      testMatch: /.*\.spec\.ts/,
    },
    {
      name: 'send report',
      testMatch: /global-teardown\.ts/,
    }
  ],

  /* Run your local dev server before starting the tests */
  // webServer: {
  //   command: 'npm run start',
  //   url: 'http://127.0.0.1:3000',
  //   reuseExistingServer: !process.env.CI,
  // },
});