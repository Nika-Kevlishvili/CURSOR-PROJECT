/**
 * Download Prod process 2114 mass-import file for PDT-3035 Dev replay.
 * Usage (from repo root):
 *   node Cursor-Project/config/playwright/download-pdt-3035-prod-process-file.mjs
 */
import fs from 'fs';
import path from 'path';
import { fileURLToPath } from 'url';

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const energotsEnv = path.resolve(__dirname, '../../EnergoTS/.env');

function loadEnvFile(filePath) {
  if (!fs.existsSync(filePath)) return;
  for (const line of fs.readFileSync(filePath, 'utf8').split(/\r?\n/)) {
    const trimmed = line.trim();
    if (!trimmed || trimmed.startsWith('#')) continue;
    const eq = trimmed.indexOf('=');
    if (eq === -1) continue;
    const key = trimmed.slice(0, eq).trim();
    let value = trimmed.slice(eq + 1).trim();
    if (
      (value.startsWith('"') && value.endsWith('"')) ||
      (value.startsWith("'") && value.endsWith("'"))
    ) {
      value = value.slice(1, -1);
    }
    if (process.env[key] == null) process.env[key] = value;
  }
}

loadEnvFile(energotsEnv);

const PROCESS_ID = 2114;
const OUT_DIR = path.resolve(__dirname, 'pdt-3035');
const OUT_FILE = path.join(
  OUT_DIR,
  'prod-process-2114-product-contract-mass-import.xlsx',
);

const PROD_AUTH_CANDIDATES = [
  process.env.PRODAUTHAPI,
  process.env.TESTAUTHAPI,
  process.env.DEVAUTHAPI,
  'https://testapps.energo-pro.bg/backend/portal/rest/v2/login',
].filter(Boolean);

const PROD_API_BASES = [
  process.env.PROD_BASE_URL,
  'https://apps.energo-pro.bg/backend/phoenix-epres',
  'http://10.236.20.66:8090',
].filter(Boolean);

async function login(authUrl) {
  const res = await fetch(authUrl, {
    method: 'POST',
    headers: { Accept: '*/*', 'Content-Type': 'application/json' },
    body: JSON.stringify({
      user: process.env.PORTAL_USER,
      password: process.env.PASSWORD,
    }),
  });
  if (!res.ok) {
    throw new Error(`Auth ${authUrl} failed: HTTP ${res.status}`);
  }
  const body = await res.json();
  if (!body.jwt) {
    throw new Error(`Auth ${authUrl} returned no jwt`);
  }
  return body.jwt;
}

async function downloadFile(apiBase, token) {
  const base = apiBase.replace(/\/$/, '');
  const url = `${base}/process/download-mass-import-file/${PROCESS_ID}`;
  const res = await fetch(url, {
    headers: { Authorization: `Bearer ${token}`, Accept: '*/*' },
  });
  if (!res.ok) {
    const text = await res.text();
    throw new Error(`Download ${url} failed: HTTP ${res.status} — ${text.slice(0, 200)}`);
  }
  const buf = Buffer.from(await res.arrayBuffer());
  if (buf.byteLength < 1000) {
    throw new Error(`Download too small (${buf.byteLength} bytes) — likely not a valid xlsx`);
  }
  return buf;
}

async function main() {
  if (!process.env.PORTAL_USER || !process.env.PASSWORD) {
    throw new Error('PORTAL_USER and PASSWORD required in EnergoTS/.env');
  }

  let lastError;
  for (const authUrl of PROD_AUTH_CANDIDATES) {
    for (const apiBase of PROD_API_BASES) {
      try {
        console.log(`Trying auth=${authUrl} api=${apiBase}`);
        const token = await login(authUrl);
        const buf = await downloadFile(apiBase, token);
        fs.mkdirSync(OUT_DIR, { recursive: true });
        fs.writeFileSync(OUT_FILE, buf);
        console.log(`Saved ${buf.byteLength} bytes -> ${OUT_FILE}`);
        return;
      } catch (e) {
        lastError = e;
        console.warn(String(e.message || e));
      }
    }
  }
  throw lastError ?? new Error('All Prod download attempts failed');
}

main().catch((e) => {
  console.error(e);
  process.exit(1);
});
