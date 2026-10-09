/**
 * PHN-4182 — Information of deactivation replaces covered POD termination actions
 * with one contract termination without notice action.
 *
 * Phoenix (origin/dev2, phoenix-core-lib e2c99819c):
 *   ActionSupplyDeactivationConversionService.convert
 *   SupplyActionDeactivationMassImportProcessService.processRow (column B = notice date)
 *
 * Swagger (dev2, cached): 
 *   POST /mass-import/{domainType}/files/upload domainType=SUPPLY_ACTION_DEACTIVATIONS
 *   GET  /actions/list
 *   GET  /actions/{id}
 *   POST /penalties PenaltyRequest.actionTypeList, penaltyApplicability
 *
 * Reference spec(s):
 * - tests/cursor/PHN-3998-actions-incorrect-action-type.spec.ts
 * - tests/cursor/phn-3998-actions-incorrect-action-type.fixtures.ts
 * - tests/cursor/pdt-2177-supply-action-deactivation-restricted-period.fixtures.ts
 *
 * Target env: Dev2 — BASE_URL=https://devapps.energo-pro.bg/backend/phoenix2-dev
 */

import * as ExcelJS from 'exceljs';
import { expect, test } from '../../fixtures/baseFixture';
import type { baseFixture } from '../../fixtures/baseFixture';
import { randomGens } from '../../utils/randomGens';
import {
  PDT_2177_TEMPLATE_URL,
  PDT_2177_UPLOAD_BASE,
  addDaysIso,
  entityId,
  isoToday,
  listSupplyActionDeactivationProcessIds,
  resolveSupplyActionDeactivationProcessId,
} from './pdt-2177-supply-action-deactivation-restricted-period.fixtures';
import {
  downloadMassImportErrorReport,
  loadMassImportErrorReportSnapshot,
  pollProcessUntilCompleted,
  type MassImportReportRow,
} from './PDT-2931-skip-risklist-product-contract-mass-import.fixtures';
import {
  createPenaltyWithActionTypes,
  createProductWithPenaltyIds,
  resolvePodIdentifierAt,
} from './phn-3998-actions-incorrect-action-type.fixtures';
import {
  buildContractNewVersionPayloadFromGet,
  latestLogicalVersionId,
  loadContractAtLatestVersion,
} from './pdt-3059-product-contract-new-version-individual-product.fixtures';
import {
  applyExpParityLocalAddress,
  postExpParityLegalCustomer,
} from './shared/exp-parity-customer.fixtures';

type FixtureRequest = baseFixture['Request'];
type FixtureFileUpload = baseFixture['FileUploadRequest'];
type Phn4182Fx = Pick<baseFixture, 'Request' | 'GeneratePayload' | 'Responses' | 'Endpoints'>;

export const PHN_4182_KEY = 'PHN-4182';
export const PHN_4182_TITLE = 'Change in actions for terminations and penalties process';

/** Dev2 nomenclature ids used by ActionTypeProperties in the PHN-4182 unit test. */
export const CONTRACT_TERMINATION_WITHOUT_NOTICE_ACTION_TYPE_ID = 2;
export const POD_TERMINATION_WITH_NOTICE_ACTION_TYPE_ID = 3;
export const POD_TERMINATION_WITHOUT_NOTICE_ACTION_TYPE_ID = 4;

export const CONTRACT_ACTION_REPORT_SNIPPET =
  'the additional contract type action was created';

export type TwoPodContractScenario = {
  customerId: number;
  customerIdentifier: string;
  contractId: number;
  podIds: number[];
  podIdentifiers: string[];
  activationDateIso: string;
  deactivationDateIso: string;
  noticeReceivingDateIso: string;
  termEndDateIso: string | null;
  podPenaltyId: number;
  contractPenaltyId: number | null;
};

export type ActionDetailSnapshot = {
  id: number;
  actionTypeId: number | null;
  actionTypeName: string;
  productContractId: number | null;
  penaltyId: number | null;
  executionDate: string | null;
  noticeReceivingDate: string | null;
  status: string | null;
  actionStatus: string | null;
  podCount: number | null;
  penaltyGenerated: boolean;
  withoutPenalty: boolean;
};

export type ImportFlowResult = {
  processId: number;
  processStatus: string;
  uploadStatus: number;
  reportRows: MassImportReportRow[];
  reportText: string;
};

function dateOnly(value: unknown): string | null {
  if (value == null || value === '') {
    return null;
  }
  const text = String(value).slice(0, 10);
  return /^\d{4}-\d{2}-\d{2}$/.test(text) ? text : null;
}

export function toDdMmYyyy(isoDate: string): string {
  const [year, month, day] = isoDate.split('-');
  return `${day}.${month}.${year}`;
}

export async function createTwoPodContractScenario(
  fx: Phn4182Fx,
  options?: { includeContractPenalty?: boolean; termEndDateIso?: string },
): Promise<TwoPodContractScenario> {
  const includeContractPenalty = options?.includeContractPenalty !== false;
  const { Request, GeneratePayload, Responses, Endpoints } = fx;
  const activationDateIso = randomGens.generateMonthStartDate('yyyy-mm-dd');
  const noticeReceivingDateIso = isoToday();
  const deactivationDateIso = options?.termEndDateIso ?? addDaysIso(noticeReceivingDateIso, 14);

  const addressIds = await test.step('Precondition: customer', async () => {
    const created = await postExpParityLegalCustomer({
      Request,
      GeneratePayload,
      Responses,
      Endpoints,
    });
    return created.addressIds;
  });

  await test.step('Precondition: price component', async () => {
    const price = await Request.post(Endpoints.priceComponent, {
      data: GeneratePayload.productAndServices.priceSettlement(),
    });
    await expect(price).CheckResponse();
    Responses.priceComponent.push(await price.json());
  });

  await test.step('Precondition: term', async () => {
    const term = await Request.post(Endpoints.terms, {
      data: GeneratePayload.productAndServices.term(),
    });
    await expect(term).CheckResponse();
    Responses.terms.push(await term.json());
  });

  const podPenaltyId = await test.step(
    'Precondition: POD penalty for termination without notice (action type 4)',
    async () =>
      createPenaltyWithActionTypes(fx, [
        POD_TERMINATION_WITHOUT_NOTICE_ACTION_TYPE_ID,
        POD_TERMINATION_WITH_NOTICE_ACTION_TYPE_ID,
      ]),
  );

  const contractPenaltyId = includeContractPenalty
    ? await test.step(
        'Precondition: contract penalty for termination without notice (action type 2)',
        async () => {
          const payload = GeneratePayload.productAndServices.penalty() as {
            actionTypeList: number[];
            penaltyApplicability: string;
          };
          payload.actionTypeList = [CONTRACT_TERMINATION_WITHOUT_NOTICE_ACTION_TYPE_ID];
          payload.penaltyApplicability = 'CONTRACT';
          const res = await Request.post(Endpoints.penalty, { data: payload });
          await expect(res).CheckResponse();
          const body = await res.json();
          Responses.penalty.push(body);
          return entityId(body);
        },
      )
    : null;

  await test.step('Precondition: two PODs', async () => {
    for (let i = 0; i < 2; i++) {
      const podPayload = GeneratePayload.pointsOfDelivery.pod_settlement() as Record<string, unknown>;
      applyExpParityLocalAddress(podPayload, addressIds);
      const pod = await Request.post(Endpoints.pod, { data: podPayload });
      await expect(pod).CheckResponse();
      Responses.pod.push(await pod.json());
    }
  });

  await test.step('Precondition: product linked to the penalties for this case', async () => {
    if (options?.termEndDateIso) {
      const products = GeneratePayload.productAndServices;
      const originalProduct = products.product.bind(products);
      products.product = ((termIndex?: number) => {
        const payload = originalProduct(termIndex) as {
          productTerms?: Array<{ typeOfTerms: string; value: string | null; periodType: string | null }>;
        };
        if (payload.productTerms?.[0]) {
          payload.productTerms[0].typeOfTerms = 'CERTAIN_DATE';
          payload.productTerms[0].value = null;
          payload.productTerms[0].periodType = null;
        }
        return payload;
      }) as typeof products.product;
    }
    const penaltyIds = contractPenaltyId == null ? [podPenaltyId] : [podPenaltyId, contractPenaltyId];
    await createProductWithPenaltyIds(fx, penaltyIds);
  });

  await test.step('Precondition: one signed product contract with both PODs', async () => {
    const contractPayload = (await GeneratePayload.contractsAndOrders.product_contract(0, 0)) as {
      basicParameters: { contractTermEndDate: string | null };
      productParameters: { contractTermEndDate: string | null };
    };
    if (options?.termEndDateIso) {
      contractPayload.basicParameters.contractTermEndDate = options.termEndDateIso;
      contractPayload.productParameters.contractTermEndDate = options.termEndDateIso;
    }
    const contract = await Request.post(Endpoints.productContract, {
      data: contractPayload,
    });
    await expect(contract).CheckResponse();
    Responses.productContract.push(await contract.json());
  });

  await test.step('Precondition: activate both PODs on that contract', async () => {
    for (const podIndex of [0, 1]) {
      const podActivation = await Request.post('/contract-pods/manual', {
        data: await GeneratePayload.pointsOfDelivery.pod_activation(
          podIndex,
          activationDateIso,
          undefined,
          0,
        ),
      });
      await expect(podActivation).CheckResponse();
    }
  });

  const podIdentifiers = [
    await resolvePodIdentifierAt(Request, Endpoints, Responses, 0),
    await resolvePodIdentifierAt(Request, Endpoints, Responses, 1),
  ];

  const customerBody = Responses.customer[0] as {
    identifier?: string;
    customerIdentifier?: string;
  };
  let customerIdentifier = String(
    customerBody.identifier ?? customerBody.customerIdentifier ?? '',
  ).trim();
  if (!customerIdentifier) {
    const customerGet = await Request.get(`${Endpoints.customer}/${entityId(Responses.customer[0])}`);
    await expect(customerGet).CheckResponse();
    const detail = (await customerGet.json()) as {
      identifier?: string;
      customerIdentifier?: string;
    };
    customerIdentifier = String(detail.identifier ?? detail.customerIdentifier ?? '').trim();
  }
  expect(customerIdentifier, 'customer identifier for actions/list').toBeTruthy();

  const contractId = entityId(Responses.productContract[0]);
  const termEndDateIso = await readContractTermEndDate(Request, contractId);
  const podIds = [0, 1].map((index) => entityId(Responses.pod[index]));

  return {
    customerId: entityId(Responses.customer[0]),
    customerIdentifier,
    contractId,
    podIds,
    podIdentifiers,
    activationDateIso,
    deactivationDateIso,
    noticeReceivingDateIso,
    termEndDateIso,
    podPenaltyId,
    contractPenaltyId,
  };
}

export async function readContractTermEndDate(
  Request: FixtureRequest,
  contractId: number,
): Promise<string | null> {
  const res = await Request.get(`product-contract/${contractId}`);
  await expect(res).CheckResponse();
  const body = (await res.json()) as unknown;
  const fromDetail = findDateField(body, 'contractTermEndDate') ?? findDateField(body, 'contractTermDate');
  if (fromDetail) {
    return fromDetail;
  }
  const contractNumber = findStringField(body, 'contractNumber');
  if (!contractNumber) {
    return null;
  }
  const listed = await Request.post('product-contract/list', {
    data: {
      page: 0,
      size: 5,
      prompt: contractNumber,
      searchBy: 'CONTRACT_NUMBER',
      exactMatch: true,
    },
  });
  if (!listed.ok()) {
    return null;
  }
  const listedBody = await listed.json();
  return findDateField(listedBody, 'contractTermDate') ?? findDateField(listedBody, 'contractTermEndDate');
}

function findStringField(value: unknown, fieldName: string): string | null {
  if (value == null || typeof value !== 'object') {
    return null;
  }
  if (Array.isArray(value)) {
    for (const item of value) {
      const found = findStringField(item, fieldName);
      if (found) {
        return found;
      }
    }
    return null;
  }
  const record = value as Record<string, unknown>;
  const direct = record[fieldName];
  if (typeof direct === 'string' && direct.trim() !== '') {
    return direct.trim();
  }
  for (const child of Object.values(record)) {
    const found = findStringField(child, fieldName);
    if (found) {
      return found;
    }
  }
  return null;
}

function findDateField(value: unknown, fieldName: string): string | null {
  if (value == null || typeof value !== 'object') {
    return null;
  }
  if (Array.isArray(value)) {
    for (const item of value) {
      const found = findDateField(item, fieldName);
      if (found) {
        return found;
      }
    }
    return null;
  }
  const record = value as Record<string, unknown>;
  if (fieldName in record) {
    const direct = dateOnly(record[fieldName]);
    if (direct) {
      return direct;
    }
  }
  for (const child of Object.values(record)) {
    const found = findDateField(child, fieldName);
    if (found) {
      return found;
    }
  }
  return null;
}

export async function buildSupplyActionDeactivationWorkbook(
  Request: FixtureRequest,
  rows: Array<{ podIdentifier: string; noticeReceivingDateIso?: string | null }>,
): Promise<Buffer> {
  expect(rows.length, 'at least one POD row').toBeGreaterThan(0);
  const templateRes = await Request.get(PDT_2177_TEMPLATE_URL);
  await expect(templateRes).CheckResponse();
  const templateBuffer = await templateRes.body();

  const workbook = new ExcelJS.Workbook();
  await workbook.xlsx.load(templateBuffer as unknown as ExcelJS.Buffer);
  const worksheet = workbook.worksheets[0];
  expect(worksheet, 'SUPPLY_ACTION_DEACTIVATIONS template worksheet').toBeTruthy();

  for (let rowIndex = 2; rowIndex <= 30; rowIndex++) {
    worksheet!.getCell(`A${rowIndex}`).value = null;
    worksheet!.getCell(`B${rowIndex}`).value = null;
  }
  rows.forEach((row, index) => {
    const excelRow = index + 2;
    worksheet!.getCell(`A${excelRow}`).value = row.podIdentifier;
    if (row.noticeReceivingDateIso) {
      worksheet!.getCell(`B${excelRow}`).value = toDdMmYyyy(row.noticeReceivingDateIso);
    }
  });

  const out = await workbook.xlsx.writeBuffer();
  return Buffer.from(out);
}

export async function runSupplyActionDeactivationImport(
  Request: FixtureRequest,
  FileUploadRequest: FixtureFileUpload,
  rows: Array<{ podIdentifier: string; noticeReceivingDateIso?: string | null }>,
  deactivationDateIso: string,
): Promise<ImportFlowResult> {
  const processIdsBefore = new Set(await listSupplyActionDeactivationProcessIds(Request));
  const fileBuffer = await buildSupplyActionDeactivationWorkbook(Request, rows);
  const uploadUrl = `${PDT_2177_UPLOAD_BASE}?date=${encodeURIComponent(deactivationDateIso)}`;
  const upload = await FileUploadRequest.post(uploadUrl, {
    multipart: {
      file: {
        name: `phn-4182-supply-action-deactivation-${randomGens.generateCurrentTimeStamp()}.xlsx`,
        mimeType: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
        buffer: fileBuffer,
      },
    },
  });
  const bodyText = await upload.text();
  expect(
    upload.status(),
    `SUPPLY_ACTION_DEACTIVATIONS upload accepted (body=${bodyText.slice(0, 300)})`,
  ).toBeGreaterThanOrEqual(200);
  expect(upload.status()).toBeLessThan(300);

  const processId = await resolveSupplyActionDeactivationProcessId(Request, processIdsBefore);
  const process = await pollProcessUntilCompleted(Request, processId);

  let reportRows: MassImportReportRow[] = [];
  try {
    const reportBuffer = await downloadMassImportErrorReport(Request, processId);
    const snapshot = await loadMassImportErrorReportSnapshot(reportBuffer);
    reportRows = snapshot.failedRows;
  } catch (error) {
    const message = error instanceof Error ? error.message : String(error);
    await test.info().attach(`PHN-4182 process ${processId} report unavailable`, {
      body: message,
      contentType: 'text/plain',
    });
  }

  const reportText = reportRows.map((row) => row.errors ?? '').join('\n');
  await test.info().attach(`PHN-4182 process ${processId} report rows`, {
    body: reportText || '(no report rows)',
    contentType: 'text/plain',
  });

  return {
    processId,
    processStatus: process.status,
    uploadStatus: upload.status(),
    reportRows,
    reportText,
  };
}

type ActionListRow = {
  id?: number;
  actionTypeName?: string;
  executionDate?: string;
  status?: string;
};

export async function listActionsByCustomer(
  Request: FixtureRequest,
  customerIdentifier: string,
): Promise<ActionListRow[]> {
  const res = await Request.get('actions/list', {
    params: {
      page: 0,
      size: 50,
      sortBy: 'ID',
      sortDirection: 'DESC',
      searchBy: 'CUSTOMER_IDENTIFIER',
      prompt: customerIdentifier,
      exactMatch: true,
    },
  });
  await expect(res).CheckResponse();
  const body = (await res.json()) as { content?: ActionListRow[] };
  return body.content ?? [];
}

export async function loadActionDetail(
  Request: FixtureRequest,
  actionId: number,
): Promise<ActionDetailSnapshot> {
  const res = await Request.get(`actions/${actionId}`);
  await expect(res).CheckResponse();
  const body = (await res.json()) as Record<string, unknown>;
  const pods = body.pods ?? body.actionPods ?? body.podResponses;
  return {
    id: entityId(body),
    actionTypeId: body.actionTypeId == null ? null : Number(body.actionTypeId),
    actionTypeName: String(body.actionTypeName ?? ''),
    productContractId: body.productContractId == null ? null : Number(body.productContractId),
    penaltyId: body.penaltyId == null ? null : Number(body.penaltyId),
    executionDate: dateOnly(body.executionDate),
    noticeReceivingDate: dateOnly(body.noticeReceivingDate),
    status: body.status == null ? null : String(body.status),
    actionStatus: body.actionStatus == null ? null : String(body.actionStatus),
    podCount: Array.isArray(pods) ? pods.length : null,
    penaltyGenerated: body.penaltyGenerated === true || body.claimedPenalty != null,
    withoutPenalty: body.withoutPenalty === true,
  };
}

function sleep(ms: number): Promise<void> {
  return new Promise((resolve) => setTimeout(resolve, ms));
}

export async function waitForCustomerActions(
  Request: FixtureRequest,
  customerIdentifier: string,
  ready: (actions: ActionDetailSnapshot[]) => boolean,
  attempts = 10,
): Promise<ActionDetailSnapshot[]> {
  let last: ActionDetailSnapshot[] = [];
  for (let attempt = 0; attempt < attempts; attempt++) {
    last = await loadCustomerActions(Request, customerIdentifier);
    if (ready(last)) {
      return last;
    }
    await sleep(2000);
  }
  return last;
}

export async function loadCustomerActions(
  Request: FixtureRequest,
  customerIdentifier: string,
): Promise<ActionDetailSnapshot[]> {
  const rows = await listActionsByCustomer(Request, customerIdentifier);
  const ids = [
    ...new Set(
      rows.map((row) => Number(row.id)).filter((id) => Number.isFinite(id) && id > 0),
    ),
  ];
  const details: ActionDetailSnapshot[] = [];
  for (const id of ids) {
    details.push(await loadActionDetail(Request, id));
  }
  return details;
}

export function isActiveRecord(action: ActionDetailSnapshot): boolean {
  return action.status == null || action.status === 'ACTIVE';
}

export async function saveNextVersionWithOnePod(
  Request: FixtureRequest,
  contractId: number,
  keepPodId: number,
  deactivationDateIso: string,
): Promise<{ startDateIso: string; versionId: number }> {
  const contract = await loadContractAtLatestVersion(Request, contractId);
  const versionId = latestLogicalVersionId(contract);
  const versions = (contract.versions ?? []) as Array<{ versionId?: number; startDate?: string }>;
  const current = versions.find((version) => Number(version.versionId) === versionId) ?? versions[versions.length - 1];
  const currentStart = String(current?.startDate ?? '').slice(0, 10);
  expect(currentStart, 'current contract version start date').toMatch(/^\d{4}-\d{2}-\d{2}$/);
  const startDateIso = addDaysIso(currentStart, 1);
  expect(
    startDateIso <= deactivationDateIso,
    `new version start ${startDateIso} must be on or before deactivation ${deactivationDateIso}`,
  ).toBeTruthy();

  const pods = (contract.contractPodsResponses ?? contract.contractPods ?? []) as Array<Record<string, unknown>>;
  const kept = pods.find((pod) => Number(pod.podId) === keepPodId);
  const keptDetailId = Number(kept?.podDetailId ?? kept?.pointOfDeliveryDetailId);
  expect(keptDetailId, `POD ${keepPodId} detail id on the current version`).toBeGreaterThan(0);

  const { payload } = buildContractNewVersionPayloadFromGet(contract, startDateIso);
  const groups = (payload.podRequests ?? []) as Array<{
    billingGroupId: number;
    productContractPointOfDeliveries: Array<{ pointOfDeliveryDetailId: number; dealNumber: unknown }>;
  }>;
  payload.podRequests = groups
    .map((group) => ({
      ...group,
      productContractPointOfDeliveries: group.productContractPointOfDeliveries.filter(
        (pod) => Number(pod.pointOfDeliveryDetailId) === keptDetailId,
      ),
    }))
    .filter((group) => group.productContractPointOfDeliveries.length > 0);
  payload.savingAsNewVersion = true;
  payload.startDate = startDateIso;

  const basicParameters = (payload.basicParameters ?? {}) as Record<string, unknown>;
  const entryInForceDate = basicParameters.entryInForceDate ?? basicParameters.entryIntoForceDate ?? startDateIso;
  basicParameters.entryInForceDate = entryInForceDate;
  basicParameters.entryIntoForceDate = entryInForceDate;
  payload.basicParameters = basicParameters;

  const podGet = await Request.get(`pod/${keepPodId}?version=1`);
  await expect(podGet).CheckResponse();
  const podJson = (await podGet.json()) as { type?: string; estimatedMonthlyAvgConsumption?: number };
  const monthly = Number(podJson.estimatedMonthlyAvgConsumption ?? 0);
  const estimatedKwh = podJson.type === 'CONSUMER' ? (monthly * 12) / 1000 : 0;
  const additionalParameters = (payload.additionalParameters ?? {}) as Record<string, unknown>;
  additionalParameters.estimatedTotalConsumptionUnderContractKwh = estimatedKwh;
  payload.additionalParameters = additionalParameters;

  const putRes = await Request.put(
    `product-contract/${contractId}?versionId=${versionId}&changeFutureVersionsPods=false`,
    { data: payload },
  );
  const bodyText = await putRes.text();
  expect(putRes.ok(), `new contract version HTTP ${putRes.status()} body=${bodyText.slice(0, 800)}`).toBeTruthy();

  const after = await loadContractAtLatestVersion(Request, contractId);
  return { startDateIso, versionId: latestLogicalVersionId(after) };
}

export function activeOnDate(
  actions: ActionDetailSnapshot[],
  executionDateIso: string,
  actionTypeId?: number,
): ActionDetailSnapshot[] {
  return actions.filter(
    (action) =>
      action.executionDate === executionDateIso &&
      isActiveRecord(action) &&
      (actionTypeId == null || action.actionTypeId === actionTypeId),
  );
}

export async function createManualAction(
  Request: FixtureRequest,
  input: {
    actionTypeId: number;
    customerId: number;
    contractId: number;
    executionDateIso: string;
    noticeReceivingDateIso: string;
    penaltyId: number | null;
    podIds: number[];
  },
): Promise<number> {
  const res = await Request.post('actions', {
    data: {
      actionTypeId: input.actionTypeId,
      noticeReceivingDate: input.noticeReceivingDateIso,
      executionDate: input.executionDateIso,
      penaltyPayer: 'CUSTOMER',
      dontAllowAutomaticPenaltyClaim: true,
      penaltyId: input.penaltyId,
      withoutPenalty: input.penaltyId == null,
      customerId: input.customerId,
      contractId: input.contractId,
      contractType: 'PRODUCT_CONTRACT',
      pods: input.podIds,
      ...(input.actionTypeId === CONTRACT_TERMINATION_WITHOUT_NOTICE_ACTION_TYPE_ID
        ? { withoutAutomaticTermination: true }
        : {}),
    },
  });
  const bodyText = await res.text();
  expect(res.ok(), `POST /actions failed HTTP ${res.status()} body=${bodyText.slice(0, 800)}`).toBeTruthy();
  return entityId(bodyText ? JSON.parse(bodyText) : null);
}

const REAL_DEACTIVATION_DOMAIN = 'SUPPLY_AUTOMATIC_DEACTIVATIONS';

async function listProcessIds(
  Request: FixtureRequest,
  matches: (name: string, type: string) => boolean,
): Promise<number[]> {
  const res = await Request.get('process', {
    params: { page: 0, size: 50, sortBy: 'ID', sortDirection: 'DESC' },
  });
  if (!res.ok()) {
    return [];
  }
  const body = (await res.json()) as {
    content?: Array<{ id?: number; name?: string; processType?: string }>;
  };
  return (body.content ?? [])
    .filter((process) => matches(String(process.name ?? ''), String(process.processType ?? '')))
    .map((process) => Number(process.id))
    .filter((id) => Number.isFinite(id) && id > 0);
}

export async function runRealSupplyDeactivation(
  Request: FixtureRequest,
  FileUploadRequest: FixtureFileUpload,
  podIdentifiers: string[],
  deactivationDateIso: string,
): Promise<ImportFlowResult> {
  const processIdsBefore = new Set(
    await listProcessIds(
      Request,
      (name, type) =>
        `${name} ${type}`.toUpperCase().includes('SUPPLY_AUTOMATIC_DEACTIVATION') &&
        !`${name} ${type}`.toUpperCase().includes('ACTION'),
    ),
  );
  const templateRes = await Request.get(`mass-import/${REAL_DEACTIVATION_DOMAIN}/template/download`);
  await expect(templateRes).CheckResponse();
  const workbook = new ExcelJS.Workbook();
  await workbook.xlsx.load((await templateRes.body()) as unknown as ExcelJS.Buffer);
  const worksheet = workbook.worksheets[0];
  expect(worksheet, 'real deactivation template').toBeTruthy();
  podIdentifiers.forEach((identifier, index) => {
    worksheet!.getCell(`A${index + 2}`).value = identifier;
  });
  const fileBuffer = Buffer.from(await workbook.xlsx.writeBuffer());
  const upload = await FileUploadRequest.post(
    `mass-import/${REAL_DEACTIVATION_DOMAIN}/files/upload?date=${encodeURIComponent(deactivationDateIso)}`,
    {
      multipart: {
        file: {
          name: `phn-4182-real-deactivation-${randomGens.generateCurrentTimeStamp()}.xlsx`,
          mimeType: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
          buffer: fileBuffer,
        },
      },
    },
  );
  const bodyText = await upload.text();
  expect(upload.status(), `real deactivation upload (body=${bodyText.slice(0, 300)})`).toBeGreaterThanOrEqual(200);
  expect(upload.status()).toBeLessThan(300);

  let processId = 0;
  for (let attempt = 0; attempt < 60 && processId === 0; attempt++) {
    const ids = await listProcessIds(
      Request,
      (name, type) =>
        `${name} ${type}`.toUpperCase().includes('SUPPLY_AUTOMATIC_DEACTIVATION') &&
        !`${name} ${type}`.toUpperCase().includes('ACTION'),
    );
    const created = ids.filter((id) => !processIdsBefore.has(id));
    if (created.length > 0) {
      processId = Math.max(...created);
      break;
    }
    await sleep(2000);
  }
  expect(processId, 'real deactivation process id').toBeGreaterThan(0);
  const process = await pollProcessUntilCompleted(Request, processId);
  let reportRows: MassImportReportRow[] = [];
  try {
    const reportBuffer = await downloadMassImportErrorReport(Request, processId);
    reportRows = (await loadMassImportErrorReportSnapshot(reportBuffer)).failedRows;
  } catch {
    reportRows = [];
  }
  return {
    processId,
    processStatus: process.status,
    uploadStatus: upload.status(),
    reportRows,
    reportText: reportRows.map((row) => row.errors ?? '').join('\n'),
  };
}

export function summarizeActions(actions: ActionDetailSnapshot[]): string {
  if (actions.length === 0) {
    return 'no actions listed for the customer';
  }
  return actions
    .map(
      (action) =>
        `id=${action.id} typeId=${action.actionTypeId} status=${action.status} actionStatus=${action.actionStatus} exec=${action.executionDate} notice=${action.noticeReceivingDate} pods=${action.podCount} penalty=${action.penaltyId}`,
    )
    .join(' | ');
}
