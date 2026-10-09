import { defineConfig } from '@playwright/test';

const devUrl = 'http://10.236.20.81:8094'
const devFixUrl = 'http://10.236.20.81:8091'
const testUrl = 'https://testapps.energo-pro.bg/backend/phoenix-epres'
const dev2Url = 'https://devapps.energo-pro.bg/backend/phoenix2-dev'
const experimentUrl = 'http://10.236.20.81:8094'

export default defineConfig({
  testDir: './src/tests',
  /* Run tests in files in parallel */
  fullyParallel: true,
  /* Fail the build on CI if you accidentally left test.only in the source code. */
  forbidOnly: !!process.env.CI,
  /* A failed test is not run again. */
  retries: 0,
  /* Opt out of parallel tests on CI. */
  workers: process.env.CI ? 1 : undefined,
  /* Reporter to use. See https://playwright.dev/docs/test-reporters */
  reporter: [
    ['json', { outputFile: 'src/backend/playwright-report.json' }],
    ['html', { open: 'never', outputFolder: 'src/backend/playwright-report' }],
  ],

  /* Shared settings for all the projects below. See https://playwright.dev/docs/api/class-testoptions. */
  use: {
    /* Base URL to use in actions like `await page.goto('/')`. */
    baseURL: process.env.BASE_URL || devUrl,

    /* Keep a trace for a failed test. Failed tests are not retried. */
    trace: 'retain-on-failure',  
    headless: false,
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
      // SLP, pulling, and sales portal run in their own projects after main finishes
      testIgnore: [
        /(SLP|pulling)\.spec\.ts/,
        /salesPortal[\\/].*\.spec\.ts/,
      ],
    },
    {
      name: 'additional billing tests',
      testMatch: /(SLP|pulling)\.spec\.ts/,
      // No dependencies — CI runs this as a separate step after main
    },
    {
      name: 'sales portal',
      testMatch: /salesPortal[\\/].*\.spec\.ts/,
      // Token lives for 1 hour. This project re-runs setup so a late pipeline step gets a fresh token.
      dependencies: ['setup'],
    },
    {
      name: 'send report',
      testMatch: /global-teardown\.ts/,
    },
    {
      name: 'update jira',
      testMatch: /jira-update\.ts/,
      // Standalone project: run via `npx playwright test --project="update jira"`
    },
    {
      name: 'frontend',
      testDir: './src/frontend',
      testMatch: /.*\.spec\.ts/,
      dependencies: ['setup'],
    }
  ],

  /* Run your local dev server before starting the tests */
  // webServer: {
  //   command: 'npm run start',
  //   url: 'http://127.0.0.1:3000',
  //   reuseExistingServer: !process.env.CI,
  // },
});