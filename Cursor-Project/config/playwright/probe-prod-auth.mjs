import fs from 'fs';
import path from 'path';
import { fileURLToPath } from 'url';

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const energotsEnv = path.resolve(__dirname, '../../EnergoTS/.env');

function loadEnvFile(filePath) {
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
    process.env[key] = value;
  }
}

loadEnvFile(energotsEnv);

const authUrls = [
  'https://apps.energo-pro.bg/backend/portal/rest/v2/login',
  'https://apps.energo-pro.bg/app/portal/rest/v2/login',
  'https://apps.energo-pro.bg/portal/rest/v2/login',
  'https://apps.energo-pro.bg/backend/phoenix-epres/portal/rest/v2/login',
  'https://portal.energo-pro.bg/rest/v2/login',
  'https://portal.energo-pro.bg/backend/portal/rest/v2/login',
];

const body = JSON.stringify({
  user: process.env.PORTAL_USER,
  password: process.env.PASSWORD,
});

for (const url of authUrls) {
  try {
    const res = await fetch(url, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json', Accept: '*/*' },
      body,
    });
    const text = await res.text();
    console.log(url, res.status, text.slice(0, 80).replace(/\s+/g, ' '));
  } catch (e) {
    console.log(url, 'ERR', String(e.message || e));
  }
}
