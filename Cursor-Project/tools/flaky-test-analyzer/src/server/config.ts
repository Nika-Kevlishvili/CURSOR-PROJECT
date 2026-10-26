import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';
import type { EnvironmentName } from '../shared/types.js';

export type ArchiveLimits = {
  maxArchiveBytes: number;
  maxExpandedBytes: number;
  maxFiles: number;
  maxFileBytes: number;
  maxProcessingMs: number;
};

export type AiConfig = {
  enabled: boolean;
  baseUrl: string | null;
  apiKey: string | null;
  model: string | null;
};

export type AppConfig = {
  host: string;
  port: number;
  appRoot: string;
  automationRoot: string;
  dataDir: string;
  storageDir: string;
  dbPath: string;
  allowedOrigins: string[];
  limits: ArchiveLimits;
  retentionDays: number;
  validationTimeoutMs: number;
  maxValidationRepeats: number;
  ai: AiConfig;
};

function intEnv(env: NodeJS.ProcessEnv, key: string, fallback: number): number {
  const raw = env[key];
  if (!raw) return fallback;
  const value = Number(raw);
  if (!Number.isFinite(value) || value < 0) return fallback;
  return value;
}

export function appRootFromMeta(metaUrl: string): string {
  return path.resolve(path.dirname(fileURLToPath(metaUrl)), '..', '..');
}

export function loadConfig(env: NodeJS.ProcessEnv = process.env, metaUrl = import.meta.url): AppConfig {
  const appRoot = env.FLAKY_ANALYZER_APP_ROOT
    ? path.resolve(env.FLAKY_ANALYZER_APP_ROOT)
    : appRootFromMeta(metaUrl);
  const automationRoot = path.resolve(env.FLAKY_ANALYZER_AUTOMATION_ROOT ?? path.join(appRoot, '..', '..', 'EnergoTS'));
  const dataDir = path.resolve(env.FLAKY_ANALYZER_DATA_DIR ?? path.join(appRoot, 'data'));
  const host = env.FLAKY_ANALYZER_HOST ?? '127.0.0.1';
  const aiRequested = env.FLAKY_ANALYZER_AI_ENABLED === 'true';
  const baseUrl = env.FLAKY_ANALYZER_AI_BASE_URL?.replace(/\/$/, '') ?? null;
  const apiKey = env.FLAKY_ANALYZER_AI_API_KEY ?? null;
  const model = env.FLAKY_ANALYZER_AI_MODEL ?? null;
  const aiReady = Boolean(aiRequested && baseUrl && apiKey && model);

  return {
    host,
    port: intEnv(env, 'FLAKY_ANALYZER_PORT', 4178),
    appRoot,
    automationRoot,
    dataDir,
    storageDir: path.join(dataDir, 'storage'),
    dbPath: path.join(dataDir, 'analyzer.sqlite'),
    allowedOrigins: (env.FLAKY_ANALYZER_ORIGINS ?? 'http://127.0.0.1:5173,http://localhost:5173')
      .split(',')
      .map((item) => item.trim())
      .filter(Boolean),
    limits: {
      maxArchiveBytes: intEnv(env, 'FLAKY_ANALYZER_MAX_ARCHIVE_BYTES', 80 * 1024 * 1024),
      maxExpandedBytes: intEnv(env, 'FLAKY_ANALYZER_MAX_EXPANDED_BYTES', 200 * 1024 * 1024),
      maxFiles: intEnv(env, 'FLAKY_ANALYZER_MAX_FILES', 4000),
      maxFileBytes: intEnv(env, 'FLAKY_ANALYZER_MAX_FILE_BYTES', 40 * 1024 * 1024),
      maxProcessingMs: intEnv(env, 'FLAKY_ANALYZER_MAX_PROCESSING_MS', 60_000),
    },
    retentionDays: intEnv(env, 'FLAKY_ANALYZER_RETENTION_DAYS', 90),
    validationTimeoutMs: intEnv(env, 'FLAKY_ANALYZER_VALIDATION_TIMEOUT_MS', 15 * 60 * 1000),
    maxValidationRepeats: Math.min(intEnv(env, 'FLAKY_ANALYZER_MAX_REPEATS', 5), 10),
    ai: {
      enabled: aiReady,
      baseUrl,
      apiKey,
      model,
    },
  };
}

export function assertLoopbackHost(host: string, env: NodeJS.ProcessEnv = process.env): void {
  const loopback = host === '127.0.0.1' || host === 'localhost' || host === '::1';
  if (!loopback && env.FLAKY_ANALYZER_ALLOW_NON_LOOPBACK !== 'true') {
    throw new Error(`Refusing to bind ${host}. The analyzer listens on 127.0.0.1 unless FLAKY_ANALYZER_ALLOW_NON_LOOPBACK=true.`);
  }
}

export function ensureDataDirs(config: AppConfig): void {
  fs.mkdirSync(config.dataDir, { recursive: true });
  fs.mkdirSync(config.storageDir, { recursive: true });
}

export function readEnvironmentUrl(automationRoot: string, environment: EnvironmentName): string {
  const file = path.join(automationRoot, 'playwright-environments.json');
  if (!fs.existsSync(file)) {
    throw new Error(`Missing ${file}. Validation needs the repository environment map.`);
  }
  const parsed = JSON.parse(fs.readFileSync(file, 'utf8')) as {
    environments?: Record<string, { url?: string }>;
  };
  const url = parsed.environments?.[environment]?.url;
  if (!url || typeof url !== 'string') {
    throw new Error(`playwright-environments.json has no URL for ${environment}.`);
  }
  const lower = url.toLowerCase();
  if (lower.includes('prodapps') || /\/\/[^/]*prod[^/]*\./.test(lower)) {
    throw new Error('Refusing to use a production URL.');
  }
  return url;
}
