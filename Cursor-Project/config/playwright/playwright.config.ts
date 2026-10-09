import { defineConfig } from '@playwright/test';
import path from 'path';

const energoRoot = path.resolve(__dirname, '../../EnergoTS');

export default defineConfig({
  testDir: __dirname,
  use: {
    baseURL: process.env.BASE_URL || 'http://10.236.20.11:8091',
  },
  projects: [{ name: 'main', testMatch: /pdt2962-past-open-period\.spec\.ts/ }],
});
