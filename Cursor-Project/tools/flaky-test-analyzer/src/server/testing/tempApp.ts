import fs from 'node:fs';
import os from 'node:os';
import path from 'node:path';
import { execFileSync } from 'node:child_process';
import { loadConfig, ensureDataDirs } from '../config.js';
import { openDatabase, type AppDb } from '../db.js';
import type { AppContext } from '../context.js';

export function tempAnalyzer(): { ctx: AppContext; root: string; cleanup: () => void } {
  const directory = fs.mkdtempSync(path.join(os.tmpdir(), 'flaky-analyzer-'));
  const root = path.join(directory, 'automation');
  fs.mkdirSync(path.join(root, 'tests'), { recursive: true });
  fs.writeFileSync(path.join(root, 'playwright-environments.json'), JSON.stringify({
    environments: {
      dev: { url: 'http://10.236.20.11:8091' },
      dev2: { url: 'https://devapps.energo-pro.bg/backend/phoenix-dev2' },
      test: { url: 'https://testapps.energo-pro.bg/backend/phoenix-epres' },
    },
  }));
  execFileSync('git', ['init'], { cwd: root, stdio: 'ignore' });
  const config = loadConfig({
    ...process.env,
    FLAKY_ANALYZER_APP_ROOT: directory,
    FLAKY_ANALYZER_AUTOMATION_ROOT: root,
    FLAKY_ANALYZER_DATA_DIR: path.join(directory, 'data'),
    FLAKY_ANALYZER_AI_ENABLED: 'false',
  });
  ensureDataDirs(config);
  const db: AppDb = openDatabase(config);
  const ctx: AppContext = {
    config,
    db,
    advisor: { enabled: false, async explain() { return null; } },
  };
  return {
    ctx,
    root,
    cleanup() {
      db.close();
      fs.rmSync(directory, { recursive: true, force: true });
    },
  };
}
