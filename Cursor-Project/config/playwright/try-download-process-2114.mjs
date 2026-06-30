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

async function tryDownload(label, authUrl, apiBase, processId) {
  const login = await fetch(authUrl, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ user: process.env.PORTAL_USER, password: process.env.PASSWORD }),
  });
  console.log(`${label} auth`, login.status);
  if (!login.ok) return;
  const { jwt } = await login.json();
  const url = `${apiBase.replace(/\/$/, '')}/process/download-mass-import-file/${processId}`;
  const res = await fetch(url, { headers: { Authorization: `Bearer ${jwt}` } });
  const buf = Buffer.from(await res.arrayBuffer());
  console.log(`${label} download`, res.status, buf.byteLength);
  if (res.ok && buf.byteLength > 1000) {
    const out = path.resolve(__dirname, 'pdt-3035/prod-process-2114-product-contract-mass-import.xlsx');
    fs.mkdirSync(path.dirname(out), { recursive: true });
    fs.writeFileSync(out, buf);
    console.log('saved', out);
  }
}

await tryDownload('test', process.env.TESTAUTHAPI, 'https://testapps.energo-pro.bg/backend/phoenix-epres', 2114);
await tryDownload('test-ip', process.env.TESTAUTHAPI, 'http://10.236.20.31:8091', 2114);
await tryDownload('dev', process.env.DEVAUTHAPI, 'http://10.236.20.11:8091', 2114);
