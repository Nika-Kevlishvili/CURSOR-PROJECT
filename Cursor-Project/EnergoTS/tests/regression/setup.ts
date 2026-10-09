import { FullConfig } from '@playwright/test';
import * as fs from 'fs';
import { tokenAuth } from '../../fixtures/login';
import { AUTH_STATE_PATH, writeAuthState } from './helpers/auth';

async function globalSetup(config: FullConfig): Promise<void> {
  console.log('🔐 Regression auth — single login for entire run');

  const apiBaseUrl = process.env.BASE_URL || 'http://10.236.20.11:8091';
  process.env.BASE_URL = apiBaseUrl;

  const frontendUrl = config.projects[0]?.use?.baseURL;
  console.log(`📍 API: ${apiBaseUrl}`);
  console.log(`📍 Frontend: ${frontendUrl}`);

  try {
    const token = await tokenAuth();
    writeAuthState(token, AUTH_STATE_PATH);
    console.log('✅ Session saved — all tests reuse this auth state');
  } catch (error) {
    if (fs.existsSync(AUTH_STATE_PATH)) {
      console.warn('⚠️ Login API unavailable — reusing existing auth-state.json');
      console.warn(`   ${error instanceof Error ? error.message : String(error)}`);
    } else {
      throw error;
    }
  }
}

export default globalSetup;
