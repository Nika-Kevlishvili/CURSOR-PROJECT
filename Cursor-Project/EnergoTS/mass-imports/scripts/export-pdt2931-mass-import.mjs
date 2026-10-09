/**
 * Export PDT-2931 product contract mass import Excel files for manual Dev upload.
 * Usage (from EnergoTS): node mass-imports/scripts/export-pdt2931-mass-import.mjs
 */
import fs from 'fs';
import path from 'path';
import { fileURLToPath } from 'url';
import ExcelJS from 'exceljs';
import { request } from '@playwright/test';

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const outputDir = path.resolve(__dirname, '../output');
const baseURL = (process.env.BASE_URL ?? 'http://10.236.20.11:8091/').replace(/\/?$/, '/');
const TEMPLATE_URL = 'mass-import/PRODUCT_CONTRACTS/template/download';
const ROUTING_COLS = ['A', 'B', 'C', 'D'];

function loadToken() {
  const tokenPath = path.resolve(__dirname, '../../fixtures/token.json');
  const raw = JSON.parse(fs.readFileSync(tokenPath, 'utf-8'));
  return typeof raw === 'string' ? raw : raw.token ?? raw.access_token;
}

function loadPayloadFromTsModule() {
  const tsPath = path.resolve(__dirname, '../massImportPayloads/productContractMpPayload.ts');
  const src = fs.readFileSync(tsPath, 'utf-8');
  const fnBody = src.replace(/^export function productContractMassPayload\(\) \{\s*return /, '').replace(/\s*}\s*$/, '');
  return new Function(`return ${fnBody}`)();
}

function clearWorksheetCells(worksheet, excelRow, columns) {
  for (const col of columns) {
    worksheet.getCell(`${col}${excelRow}`).value = null;
  }
}

function applyPayloadToWorksheetRow(worksheet, payload, excelRow, { clearRoutingColumns = false } = {}) {
  if (clearRoutingColumns) {
    clearWorksheetCells(worksheet, excelRow, ROUTING_COLS);
  }
  const numericColumns = new Set(['F', 'G', 'H', 'B', 'BS', 'BR', 'BQ']);
  const dateColumns = new Set(['D', 'N', 'O', 'P', 'Q', 'R', 'S']);
  for (const field of Object.values(payload)) {
    if (!field?.cellNumber) continue;
    const col = field.cellNumber.replace(/\d+$/, '');
    const value = field.value;
    if (value === undefined || value === null || value === '') continue;
    const cell = worksheet.getCell(`${col}${excelRow}`);
    if (typeof value === 'number') {
      cell.value = value;
      continue;
    }
    const asString = String(value);
    if (dateColumns.has(col) && /^\d{4}-\d{2}-\d{2}$/.test(asString)) {
      cell.value = new Date(`${asString}T12:00:00.000Z`);
    } else if (numericColumns.has(col) && /^-?\d+(\.\d+)?$/.test(asString)) {
      cell.value = Number(asString);
    } else {
      cell.value = value;
    }
  }
}

function buildCreatePayload(base, row) {
  const p = JSON.parse(JSON.stringify(base));
  p.contract_customer_identifier.value = row.customerIdentifier;
  p.contract_customer_version.value = row.customerVersion;
  p.contract_product.value = row.productId;
  p.contract_product_version.value = row.productVersion;
  p.contract_pods.value = row.podIdentifier;
  p.contract_applicable_interest_rate.value = row.interestRateName;
  p.contract_number.value = '';
  p.contract_version.value = '';
  p.contract_create_edit_c_or_e.value = '';
  p.contract_start_date.value = '';
  p.contract_status.value = 'DRAFT';
  p.contract_sub_status.value = 'DRAFT';
  p.contract_type.value = 'CONTRACT';
  p.contract_version_status.value = 'SIGNED';
  p.contract_entering_into_force.value = 'SIGNING';
  p.contract_start_of_term.value = 'SIGNING';
  p.contract_supply_activation_after_contract_resigning.value = 'FIRST_DAY_OF_MONTH';
  for (const k of [
    'contract_entering_into_force_value',
    'contract_start_of_term_value',
    'contract_supply_activation_after_contract_resigning_value',
    'contract_entry_into_force_date',
    'contract_term_start_date',
  ]) {
    if (p[k]) p[k].value = '';
  }
  return p;
}

function buildEditBase(base, row) {
  const p = JSON.parse(JSON.stringify(base));
  p.contract_customer_identifier.value = row.customerIdentifier;
  p.contract_customer_version.value = row.customerVersion;
  p.contract_product.value = row.productId;
  p.contract_product_version.value = row.productVersion;
  p.contract_applicable_interest_rate.value = row.interestRateName;
  p.contract_number.value = row.contractNumber;
  p.contract_version.value = row.version;
  p.contract_type.value = row.contractDetailType;
  p.contract_pods.value = '';
  for (const k of [
    'contract_status',
    'contract_sub_status',
    'contract_version_type',
    'contract_version_status',
    'contract_entering_into_force',
    'contract_start_of_term',
    'contract_supply_activation_after_contract_resigning',
    'contract_entering_into_force_value',
    'contract_start_of_term_value',
    'contract_supply_activation_after_contract_resigning_value',
  ]) {
    if (p[k]) p[k].value = '';
  }
  return p;
}

function buildEditSamePayload(base, row) {
  const p = buildEditBase(base, row);
  p.contract_create_edit_c_or_e.value = 'E';
  p.contract_start_date.value = '';
  return p;
}

function buildEditNewVersionPayload(base, row) {
  const p = buildEditBase(base, row);
  p.contract_create_edit_c_or_e.value = 'C';
  p.contract_start_date.value = row.startDate;
  p.contract_version_status.value = 'SIGNED';
  return p;
}

function addHeaderComments(worksheet) {
  const notes = {
    A1: 'CREATE: leave A empty. EDIT: existing contract number.',
    B1: 'CREATE: leave B empty. EDIT: version number (e.g. 1).',
    C1: 'CREATE: leave C empty. EDIT: E = same version, C = new version.',
    D1: 'CREATE: leave D empty. EDIT new version (C): start date yyyy-MM-dd.',
  };
  for (const [addr, text] of Object.entries(notes)) {
    const cell = worksheet.getCell(addr);
    cell.note = text;
  }
}

function addInstructionsSheet(workbook) {
  const ws = workbook.addWorksheet('HOW_TO_READ', { properties: { tabColor: { argb: 'FFFFCC00' } } });
  const lines = [
    ['PDT-2931 — Product contract mass import (Dev)'],
    [''],
    ['Row type is NOT a separate column. Phoenix reads columns A, B, C:'],
    [''],
    ['CREATE new contract (row 2 in REFERENCE file)'],
    ['  A Contract_number     — EMPTY'],
    ['  B Contract_version    — EMPTY'],
    ['  C create_edit         — EMPTY'],
    ['  D start_date          — EMPTY'],
    ['  E customer UIC/PN     — required'],
    ['  BY POD identifier     — required'],
    [''],
    ['EDIT same version (row 3)'],
    ['  A — contract number (e.g. EP-1234567890)'],
    ['  B — version (e.g. 1)'],
    ['  C — E'],
    ['  D — empty'],
    [''],
    ['EDIT new version (row 4)'],
    ['  A — contract number'],
    ['  B — version'],
    ['  C — C'],
    ['  D — new version start date'],
    [''],
    ['Do NOT type CREATE/EDIT in column A — empty A+B+C means create.'],
    ['Evidence: ProductContractMassImportProcessService.java lines 74-77'],
    [''],
    ['pdt-2931-last-upload.xlsx from Playwright is often CREATE-only (1 row) — that is why A/B/C look empty.'],
  ];
  lines.forEach((row, i) => {
    ws.getCell(`A${i + 1}`).value = row[0] ?? '';
  });
  ws.getColumn(1).width = 90;
}

async function resolveInterestRateName(api) {
  let interestRateId = process.env.INTEREST_RATE_ID;
  if (!interestRateId) {
    const envPath = path.resolve(__dirname, '../../fixtures/envVariables.json');
    if (fs.existsSync(envPath)) {
      interestRateId = JSON.parse(fs.readFileSync(envPath, 'utf-8')).interest_rate;
    }
  }
  if (!interestRateId) return '<<INTEREST_RATE_NAME>>';
  const res = await api.get(`interest-rate/${interestRateId}`);
  if (!res.ok()) return '<<INTEREST_RATE_NAME>>';
  const body = await res.json();
  return body.name ?? body.interestRateName ?? '<<INTEREST_RATE_NAME>>';
}

function futureStartDateIso() {
  const d = new Date();
  d.setUTCMonth(d.getUTCMonth() + 1);
  d.setUTCDate(1);
  return d.toISOString().split('T')[0];
}

async function main() {
  fs.mkdirSync(outputDir, { recursive: true });
  const token = loadToken();
  const api = await request.newContext({
    baseURL,
    extraHTTPHeaders: { Authorization: `Bearer ${token}`, Accept: '*/*' },
  });

  const templateRes = await api.get(TEMPLATE_URL);
  if (!templateRes.ok()) throw new Error(`Template download failed: ${templateRes.status()}`);
  const templateBuffer = await templateRes.body();

  const templatePath = path.join(outputDir, 'PDT-2931-PRODUCT_CONTRACTS-template-Dev.xlsx');
  fs.writeFileSync(templatePath, templateBuffer);

  const interestRateName = await resolveInterestRateName(api);
  const basePayload = loadPayloadFromTsModule();

  const placeholders = {
    customerIdentifier: '<<CUSTOMER_UIC>>',
    customerVersion: 1,
    productId: '<<PRODUCT_ID>>',
    productVersion: 1,
    podIdentifier: '<<POD_IDENTIFIER>>',
    interestRateName,
    contractNumber: '<<EXISTING_CONTRACT_NUMBER>>',
    version: 1,
    contractDetailType: 'CONTRACT',
    startDate: futureStartDateIso(),
  };

  const refWb = new ExcelJS.Workbook();
  await refWb.xlsx.load(templateBuffer);
  const refWs = refWb.getWorksheet(1);
  if (!refWs) throw new Error('No worksheet');

  addHeaderComments(refWs);
  applyPayloadToWorksheetRow(refWs, buildCreatePayload(basePayload, placeholders), 2, {
    clearRoutingColumns: true,
  });
  applyPayloadToWorksheetRow(refWs, buildEditSamePayload(basePayload, placeholders), 3);
  applyPayloadToWorksheetRow(refWs, buildEditNewVersionPayload(basePayload, placeholders), 4);

  // Visible row labels in column Z (not read by Phoenix — beyond BY)
  refWs.getCell('Z2').value = 'ROW TYPE: CREATE (A,B,C,D empty)';
  refWs.getCell('Z3').value = 'ROW TYPE: EDIT same version (C=E)';
  refWs.getCell('Z4').value = 'ROW TYPE: EDIT new version (C=C, D=date)';
  refWs.getCell('Z1').value = 'Human hint only — do not use in upload';

  addInstructionsSheet(refWb);
  const referencePath = path.join(outputDir, 'PDT-2931-mass-import-REFERENCE-3rows-plus-guide.xlsx');
  await refWb.xlsx.writeFile(referencePath);

  await api.dispose();
  console.log('Written:');
  console.log(' ', templatePath);
  console.log(' ', referencePath);
  console.log('');
  console.log('Open REFERENCE file: sheet HOW_TO_READ + rows 2-4 on Sheet1. Column Z is hint only.');
}

main().catch((e) => {
  console.error(e);
  process.exit(1);
});
