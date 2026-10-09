/**

 * DEV-DATA — Product contract mass import CREATE with minimal Excel fields (Dev).

 *

 * Purpose: create a product contract via mass import using the Confluence CREATE rule

 * (empty Contract_number + empty Contract_version + empty Contract_create_edit)

 * and generator-default POD consumption (not Risk List max).

 *

 * Confluence: Mass import of product contract (page ID 9568493)

 * https://asterbit.atlassian.net/wiki/spaces/Phoenix/pages/9568493/Mass+import+of+product+contract

 *

 * Reference spec(s):

 * - tests/cursor/PDT-2931-skip-risklist-product-contract-mass-import.spec.ts

 * - tests/cursor/PDT-2931-skip-risklist-product-contract-mass-import.fixtures.ts

 * - tests/cursor/dev-volume-invoice-custom-template.spec.ts (DEV-DATA naming)

 * - mass-imports/massImportPayloads/productContractMpPayload.ts

 */



import * as ExcelJS from 'exceljs';

import { expect, test } from '../../fixtures/baseFixture';

import type { baseFixture } from '../../fixtures/baseFixture';

import { entityId } from './pdt-2906-product-new-version-contract-update.fixtures';

import {

  buildProductContractMassImportWorkbook,

  downloadMassImportErrorReport,

  formatMassImportErrorReportForAttach,

  listProductContractMassImportProcessIds,

  loadMassImportErrorReportSnapshot,

  pollProcessUntilCompleted,

  resolveProductContractMassImportProcessId,

  uploadProductContractMassImportFile,

  type MassImportErrorReportSnapshot,

  type MassImportReportRow,

  type ProductContractMiRow,

} from './PDT-2931-skip-risklist-product-contract-mass-import.fixtures';



type FixtureRequest = baseFixture['Request'];

type FixtureFileUpload = baseFixture['FileUploadRequest'];



export type CreatedProductContractRef = {

  id: number;

  contractNumber: string;

  detail: Record<string, unknown>;

};



const CREATE_ROW_DUMP_COLUMNS = [

  'A',

  'C',

  'E',

  'F',

  'G',

  'H',

  'I',

  'J',

  'K',

  'L',

  'BJ',

  'BL',

  'BN',

  'BY',

] as const;



function sleep(ms: number): Promise<void> {

  return new Promise((resolve) => setTimeout(resolve, ms));

}



function cellText(value: ExcelJS.CellValue | null | undefined): string {

  if (value == null || value === '') {

    return '';

  }

  if (typeof value === 'object' && 'text' in value && value.text != null) {

    return String(value.text).trim();

  }

  return String(value).trim();

}



async function attachCreateRow2CellDump(fileBuffer: Buffer): Promise<void> {

  const workbook = new ExcelJS.Workbook();

  await workbook.xlsx.load(fileBuffer);

  const worksheet = workbook.getWorksheet(1);

  if (!worksheet) {

    throw new Error('Mass import workbook has no worksheet for CREATE row 2 cell dump');

  }

  const lines = CREATE_ROW_DUMP_COLUMNS.map((column) => {

    const cell = worksheet.getCell(`${column}2`);

    return `${column}=${cellText(cell.value)}`;

  });

  await test.info().attach('mass-import-create-row-2-cell-dump', {

    body: lines.join('\n'),

    contentType: 'text/plain',

  });

}



type ProductContractListRow = {

  id?: number;

  contractNumber?: string;

  customerId?: number;

  customerNumber?: string | number;

};



function matchesCustomerProductContractRow(

  row: ProductContractListRow,

  customerId: number,

  customerNumber?: string,

): boolean {

  if (Number(row.customerId) === customerId) {

    return true;

  }

  if (customerNumber && String(row.customerNumber ?? '') === String(customerNumber)) {

    return true;

  }

  return false;

}



/**

 * CONSUMER POD for minimal mass-import CREATE.

 * Leaves `estimatedMonthlyAvgConsumption` at the generator default (`pod_settlement` → `"1"`).

 * Do not call PDT-2931 `createConsumerPod` — that sets 99_999_999 (Risk List trigger).

 */

export async function createMinimalConsumerPod(

  Request: FixtureRequest,

  GeneratePayload: baseFixture['GeneratePayload'],

  Responses: baseFixture['Responses'],

  Endpoints: baseFixture['Endpoints'],

): Promise<{ podId: number; podIdentifier: string }> {

  const podPayload = GeneratePayload.pointsOfDelivery.pod_settlement();

  podPayload.type = 'CONSUMER';

  podPayload.consumptionPurpose = 'NON_HOUSEHOLD';

  podPayload.voltageLevel = 'LOW';

  const pod = await Request.post(Endpoints.pod, { data: podPayload });

  await expect(pod).CheckResponse();

  const podBody = await pod.json();

  Responses.pod.push(podBody);

  const podId = entityId(podBody);

  let identifier = String(podBody.identifier ?? '');

  if (!identifier) {

    const podGet = await Request.get(`${Endpoints.pod}/${podId}`);

    await expect(podGet).CheckResponse();

    const podDetail = await podGet.json();

    identifier = String(podDetail.identifier);

  }

  return { podId, podIdentifier: identifier };

}



/**

 * Poll product-contract/list until a row matches the test customer by customerId (or customerNumber),

 * then GET detail. Never treats unmatched totalElements as a hit (avoids PDT-2931 false positive).

 */

export async function waitAndLoadCreatedProductContractForCustomer(

  Request: FixtureRequest,

  options: {

    customerId: number;

    customerIdentifier: string;

    customerNumber?: string;

  },

  timeoutMs = 120_000,

): Promise<CreatedProductContractRef> {

  const { customerId, customerIdentifier, customerNumber } = options;

  const prompts = [customerIdentifier, customerNumber ? String(customerNumber) : ''].filter(Boolean);

  const searchModes: Array<{ searchBy: string }> = [

    { searchBy: 'CUSTOMER_UIC_OR_PERSONAL_NUMBER' },

    { searchBy: 'ALL' },

  ];

  const triedAttempts: string[] = [];

  let lastContentLength = 0;

  const deadline = Date.now() + timeoutMs;



  while (Date.now() < deadline) {

    for (const prompt of prompts) {

      for (const mode of searchModes) {

        const res = await Request.post('product-contract/list', {

          data: { page: 0, size: 100, prompt, ...mode },

        });

        if (res.status() !== 200) {

          triedAttempts.push(

            `prompt=${prompt} searchBy=${mode.searchBy} status=${res.status()}`,

          );

          continue;

        }

        await expect(res).CheckResponse();

        const body = (await res.json()) as {

          content?: ProductContractListRow[];

          totalElements?: number;

        };

        const content = body.content ?? [];

        lastContentLength = content.length;

        triedAttempts.push(

          `prompt=${prompt} searchBy=${mode.searchBy} contentLength=${content.length} totalElements=${body.totalElements ?? 'n/a'}`,

        );



        const row = content.find((item) =>

          matchesCustomerProductContractRow(item, customerId, customerNumber),

        );

        if (!row) {

          continue;

        }

        const id = Number(row.id);

        const contractNumber = String(row.contractNumber ?? '');

        expect(id, 'created product contract id from list').toBeGreaterThan(0);

        expect(contractNumber, 'created product contract contractNumber from list').toBeTruthy();



        const detailRes = await Request.get(`product-contract/${id}?versionId=1`);

        await expect(detailRes).CheckResponse();

        const detail = (await detailRes.json()) as Record<string, unknown>;

        return { id, contractNumber, detail };

      }

    }

    await sleep(2000);

  }



  throw new Error(

    `No product contract list row found for customerId=${customerId} identifier=${customerIdentifier} after ${timeoutMs}ms. ` +

      `Tried: ${triedAttempts.join('; ')}. Last content length=${lastContentLength}`,

  );

}



/**

 * POST product-contract/list (Swagger: ProductContractListingRequest — required page, size;

 * searchBy CUSTOMER_UIC_OR_PERSONAL_NUMBER | ALL) and return the created row.

 * Prefer {@link waitAndLoadCreatedProductContractForCustomer} — this helper does not poll and

 * does not verify customerId match on list rows.

 */

export async function loadCreatedProductContractForCustomer(

  Request: FixtureRequest,

  customerIdentifier: string,

  customerNumber?: string,

): Promise<CreatedProductContractRef> {

  const prompts = [customerIdentifier, customerNumber ? String(customerNumber) : ''].filter(Boolean);

  const searchModes: Array<{ searchBy?: string }> = [

    { searchBy: 'CUSTOMER_UIC_OR_PERSONAL_NUMBER' },

    { searchBy: 'ALL' },

  ];



  for (const prompt of prompts) {

    for (const mode of searchModes) {

      const res = await Request.post('product-contract/list', {

        data: { page: 0, size: 100, prompt, ...mode },

      });

      if (res.status() !== 200) {

        continue;

      }

      await expect(res).CheckResponse();

      const body = (await res.json()) as {

        content?: Array<{ id?: number; contractNumber?: string }>;

      };

      const row = (body.content ?? []).find((item) => Number(item.id) > 0);

      if (!row) {

        continue;

      }

      const id = Number(row.id);

      const contractNumber = String(row.contractNumber ?? '');

      expect(id, 'created product contract id from list').toBeGreaterThan(0);

      expect(contractNumber, 'created product contract contractNumber from list').toBeTruthy();



      const detailRes = await Request.get(`product-contract/${id}?versionId=1`);

      await expect(detailRes).CheckResponse();

      const detail = (await detailRes.json()) as Record<string, unknown>;

      return { id, contractNumber, detail };

    }

  }



  throw new Error(`No product contract list row found for customer ${customerIdentifier}`);

}



const LEFTOVER_TEMPLATE_ROW_NULL_DATE_SNIPPET =

  'EntryIntoForce, StartOfContractInitialTerm and supply activation can not be null';



function parseReportExcelRowNumber(rowNumber: MassImportReportRow['rowNumber']): number | null {

  const parsed = Number(rowNumber);

  return Number.isFinite(parsed) ? parsed : null;

}



function isLeftoverTemplateRowNullDateFailure(row: MassImportReportRow, lastDataRow: number): boolean {

  const excelRow = parseReportExcelRowNumber(row.rowNumber);

  if (excelRow == null || excelRow <= lastDataRow) {

    return false;

  }

  return String(row.errors ?? '').includes(LEFTOVER_TEMPLATE_ROW_NULL_DATE_SNIPPET);

}



export async function runMinimalProductContractMassImportFlow(

  Request: FixtureRequest,

  FileUploadRequest: FixtureFileUpload,

  rows: ProductContractMiRow[],

): Promise<{

  processId: number;

  reportRows: MassImportReportRow[];

  reportSnapshot: MassImportErrorReportSnapshot;

  uploadStatus: number;

}> {

  const lastDataRow = 1 + rows.length;

  const fileBuffer = await buildProductContractMassImportWorkbook(Request, rows);

  await attachCreateRow2CellDump(fileBuffer);

  const processIdsBeforeUpload = new Set(await listProductContractMassImportProcessIds(Request));

  const upload = await uploadProductContractMassImportFile(

    FileUploadRequest,

    fileBuffer,

    'dev-pc-mi-minimal.xlsx',

  );

  if (upload.status !== 202) {

    await test.info().attach('mass-import-upload-error', {

      body: String(upload.bodyText ?? '').slice(0, 4000),

      contentType: 'text/plain',

    });

  }

  expect(upload.status, 'mass import upload must be accepted').toBe(202);



  const processId = await resolveProductContractMassImportProcessId(Request, processIdsBeforeUpload);

  const process = await pollProcessUntilCompleted(Request, processId);

  expect(process.status).toBe('COMPLETED');



  const reportBuffer = await downloadMassImportErrorReport(Request, processId);

  const reportSnapshot = await loadMassImportErrorReportSnapshot(reportBuffer);



  const leftoverTemplateFailures = reportSnapshot.failedRows.filter((row) =>

    isLeftoverTemplateRowNullDateFailure(row, lastDataRow),

  );

  const blockingFailures = reportSnapshot.failedRows.filter(

    (row) => !isLeftoverTemplateRowNullDateFailure(row, lastDataRow),

  );



  await test.info().attach('mass-import-report-failure-split', {

    body: `leftoverTemplateFailedRows=${leftoverTemplateFailures.length} blockingFailures=${blockingFailures.length}`,

    contentType: 'text/plain',

  });



  if (blockingFailures.length > 0) {

    const errorTexts = blockingFailures

      .map((row) => `row ${row.rowNumber}: ${String(row.errors ?? '')}`)

      .join('; ');

    expect(blockingFailures, `Mass import blocking failures: ${errorTexts}`).toHaveLength(0);

  }



  test.info().attach(`[DEV-DATA] process ${processId} — MASS_IMPORT_ERROR_REPORT content`, {

    body: formatMassImportErrorReportForAttach(reportSnapshot),

    contentType: 'text/plain',

  });



  return {

    processId,

    reportRows: blockingFailures,

    reportSnapshot,

    uploadStatus: upload.status,

  };

}


