/**
 * PHN-3998 / PDT-2337 — Information-of-deactivation mass import must create
 * actions with action type POD termination without notice (nomenclature id 4 on Dev2),
 * even when a contract product also has a with-notice penalty (config id 3).
 *
 * Phoenix (origin/dev2): ActionSupplyActivationService.onComplete
 *   - findPenaltyIdsForProductWithContractDetailId(..., [podTerminationWithoutNoticeId])
 *   - createActionRequestForPods always setActionTypeId(podTerminationWithoutNoticeId)
 *
 * Swagger (dev2):
 *   POST /mass-import/{domainType}/files/upload domainType=SUPPLY_ACTION_DEACTIVATIONS
 *   GET  /actions/list → PageActionListResponse / ActionListResponse.actionTypeName
 *   GET  /actions/{id} → ActionResponse.actionTypeId
 *   POST /penalties → PenaltyRequest.actionTypeList (int64[]), penaltyApplicability POD|CONTRACT|EVENT
 *
 * Reference spec(s):
 * - tests/cursor/PDT-2177-supply-action-deactivation-restricted-period.spec.ts
 * - tests/cursor/pdt-2177-supply-action-deactivation-restricted-period.fixtures.ts
 * - tests/cursor/PDT-2931-skip-risklist-product-contract-mass-import.fixtures.ts
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
  entityId,
  isoToday,
  listSupplyActionDeactivationProcessIds,
  resolveSupplyActionDeactivationProcessId,
} from './pdt-2177-supply-action-deactivation-restricted-period.fixtures';
import {
  downloadMassImportErrorReport,
  formatMassImportErrorReportForAttach,
  loadMassImportErrorReportSnapshot,
  pollProcessUntilCompleted,
  type MassImportErrorReportSnapshot,
} from './PDT-2931-skip-risklist-product-contract-mass-import.fixtures';

type FixtureRequest = baseFixture['Request'];
type FixtureFileUpload = baseFixture['FileUploadRequest'];
type Phn3998Fx = Pick<baseFixture, 'Request' | 'GeneratePayload' | 'Responses' | 'Endpoints'>;

/** Matches phoenix-mass-import application-*.properties action.type.pod-termination.without-notice */
export const PHN_3998_KEY = 'PHN-3998';
export const PHN_3998_TITLE = 'CLONE - Actions - Incorrect Action Type';

export const POD_TERMINATION_WITHOUT_NOTICE_ACTION_TYPE_ID = 4;
/** Matches action.type.pod-termination.with-notice (Dev2 nomenclature id 3). */
export const POD_TERMINATION_WITH_NOTICE_ACTION_TYPE_ID = 3;

/** Exact Dev2 nomenclature.action_types.name for id 4 (ticket expected type). */
export const POD_TERMINATION_WITHOUT_NOTICE_ACTION_TYPE_NAME =
  'ПРЕКРАТЯВАНЕ НА ТО БЕЗ ПРЕДИЗВЕСТИЕ';

export type ActionDetailSnapshot = {
  id: number;
  actionTypeId: number | null;
  actionTypeName: string;
  productContractId: number | null;
  penaltyId: number | null;
  executionDate: string | null;
};

export function buildActionPreviewPath(actionId: number): string {
  return `actions-for-terminations-and-penalties/preview?id=${actionId}`;
}

function sleep(ms: number): Promise<void> {
  return new Promise((resolve) => setTimeout(resolve, ms));
}

function dateOnly(value: unknown): string | null {
  if (value == null || value === '') {
    return null;
  }
  const text = String(value).slice(0, 10);
  return /^\d{4}-\d{2}-\d{2}$/.test(text) ? text : null;
}

export async function resolvePodIdentifierAt(
  Request: FixtureRequest,
  Endpoints: baseFixture['Endpoints'],
  Responses: baseFixture['Responses'],
  podIndex: number,
): Promise<string> {
  const podBody = Responses.pod[podIndex] as { id?: number; identifier?: string } | undefined;
  const fromCreate = String(podBody?.identifier ?? '').trim();
  if (fromCreate) {
    return fromCreate;
  }
  const podGet = await Request.get(`${Endpoints.pod}/${entityId(podBody)}`);
  await expect(podGet).CheckResponse();
  const detail = (await podGet.json()) as { identifier?: string };
  const identifier = String(detail.identifier ?? '').trim();
  expect(
    identifier,
    `POD identifier at index ${podIndex} required for SUPPLY_ACTION_DEACTIVATIONS upload`,
  ).toBeTruthy();
  return identifier;
}

export async function createPenaltyWithActionTypes(
  fx: Phn3998Fx,
  actionTypeIds: number[],
): Promise<number> {
  const { Request, GeneratePayload, Responses, Endpoints } = fx;
  const payload = GeneratePayload.productAndServices.penalty() as {
    actionTypeList: number[];
    penaltyApplicability: string;
  };
  payload.actionTypeList = [...actionTypeIds];
  payload.penaltyApplicability = 'POD';
  const res = await Request.post(Endpoints.penalty, { data: payload });
  await expect(res).CheckResponse();
  const body = await res.json();
  Responses.penalty.push(body);
  return entityId(body);
}

export async function createProductWithPenaltyIds(
  fx: Phn3998Fx,
  penaltyIds: number[],
  termIndex = 0,
  priceComponentIds?: number[],
): Promise<number> {
  const { Request, GeneratePayload, Responses, Endpoints } = fx;
  const payload = GeneratePayload.productAndServices.product(termIndex) as {
    penaltyIds: number[];
    penaltyGroupIds: number[];
    priceComponentIds: number[];
  };
  payload.penaltyIds = [...penaltyIds];
  payload.penaltyGroupIds = [];
  if (priceComponentIds) {
    payload.priceComponentIds = [...priceComponentIds];
  }
  const res = await Request.post(Endpoints.product, { data: payload });
  await expect(res).CheckResponse();
  const body = await res.json();
  // product_contract interpolates Responses.product[i] into GET /products/{id}
  Responses.product.push(entityId(body));
  return entityId(body);
}

export async function createTwoContractDeactivationScenario(fx: Phn3998Fx): Promise<{
  customerId: number;
  customerIdentifier: string;
  contractIds: number[];
  podIdentifiers: string[];
  activationDateIso: string;
  deactivationDateIso: string;
  withoutNoticePenaltyIdA: number;
  withoutNoticePenaltyIdB: number;
  withNoticePenaltyId: number;
}> {
  const { Request, GeneratePayload, Responses, Endpoints } = fx;
  const activationDateIso = randomGens.generateMonthStartDate('yyyy-mm-dd');
  const deactivationDateIso = activationDateIso;

  await test.step('Precondition: customer', async () => {
    const customer = await Request.post(Endpoints.customer, {
      data: GeneratePayload.customers.customer_legal(),
    });
    await expect(customer).CheckResponse();
    Responses.customer.push(await customer.json());
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

  const withoutNoticePenaltyIdA = await test.step(
    'Precondition: penalty A — POD termination without notice (action type 4)',
    async () =>
      createPenaltyWithActionTypes(fx, [POD_TERMINATION_WITHOUT_NOTICE_ACTION_TYPE_ID]),
  );

  await test.step('Precondition: POD 1 (contract A)', async () => {
    const pod = await Request.post(Endpoints.pod, {
      data: GeneratePayload.pointsOfDelivery.pod_settlement(),
    });
    await expect(pod).CheckResponse();
    Responses.pod.push(await pod.json());
  });

  await test.step('Precondition: product A (without-notice penalty only)', async () => {
    await createProductWithPenaltyIds(fx, [withoutNoticePenaltyIdA]);
  });

  await test.step('Precondition: product contract A', async () => {
    const contract = await Request.post(Endpoints.productContract, {
      data: await GeneratePayload.contractsAndOrders.product_contract(0, 0, 0),
    });
    await expect(contract).CheckResponse();
    Responses.productContract.push(await contract.json());
  });

  const withNoticePenaltyId = await test.step(
    'Precondition: penalty POD termination with notice (action type 3)',
    async () => createPenaltyWithActionTypes(fx, [POD_TERMINATION_WITH_NOTICE_ACTION_TYPE_ID]),
  );

  await test.step('Precondition: POD 2 (contract B)', async () => {
    const pod = await Request.post(Endpoints.pod, {
      data: GeneratePayload.pointsOfDelivery.pod_settlement(),
    });
    await expect(pod).CheckResponse();
    Responses.pod.push(await pod.json());
  });

  await test.step('Precondition: term B (product terms are exclusive per product)', async () => {
    const term = await Request.post(Endpoints.terms, {
      data: GeneratePayload.productAndServices.term(),
    });
    await expect(term).CheckResponse();
    Responses.terms.push(await term.json());
  });

  await test.step('Precondition: price component B (price components are exclusive per product)', async () => {
    const price = await Request.post(Endpoints.priceComponent, {
      data: GeneratePayload.productAndServices.priceSettlement(),
    });
    await expect(price).CheckResponse();
    Responses.priceComponent.push(await price.json());
  });

  const withoutNoticePenaltyIdB = await test.step(
    'Precondition: penalty B — POD termination without notice (action type 4)',
    async () =>
      createPenaltyWithActionTypes(fx, [POD_TERMINATION_WITHOUT_NOTICE_ACTION_TYPE_ID]),
  );

  await test.step('Precondition: product B (with-notice + without-notice penalties)', async () => {
    await createProductWithPenaltyIds(
      fx,
      [withoutNoticePenaltyIdB, withNoticePenaltyId],
      1,
      [entityId(Responses.priceComponent[1])],
    );
  });

  await test.step('Precondition: product contract B', async () => {
    const contract = await Request.post(Endpoints.productContract, {
      data: await GeneratePayload.contractsAndOrders.product_contract(0, 1, 1),
    });
    await expect(contract).CheckResponse();
    Responses.productContract.push(await contract.json());
  });

  await test.step('Precondition: activate POD on contract A', async () => {
    const podActivation = await Request.post('/contract-pods/manual', {
      data: await GeneratePayload.pointsOfDelivery.pod_activation(0, activationDateIso, undefined, 0),
    });
    await expect(podActivation).CheckResponse();
  });

  await test.step('Precondition: activate POD on contract B', async () => {
    const podActivation = await Request.post('/contract-pods/manual', {
      data: await GeneratePayload.pointsOfDelivery.pod_activation(1, activationDateIso, undefined, 1),
    });
    await expect(podActivation).CheckResponse();
  });

  const podIdentifiers = [
    await resolvePodIdentifierAt(Request, Endpoints, Responses, 0),
    await resolvePodIdentifierAt(Request, Endpoints, Responses, 1),
  ];

  const customerBody = Responses.customer[0] as {
    id?: number;
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

  return {
    customerId: entityId(Responses.customer[0]),
    customerIdentifier,
    contractIds: [
      entityId(Responses.productContract[0]),
      entityId(Responses.productContract[1]),
    ],
    podIdentifiers,
    activationDateIso,
    deactivationDateIso,
    withoutNoticePenaltyIdA,
    withoutNoticePenaltyIdB,
    withNoticePenaltyId,
  };
}

export async function buildSupplyActionDeactivationWorkbookForPods(
  Request: FixtureRequest,
  podIdentifiers: string[],
): Promise<Buffer> {
  expect(podIdentifiers.length, 'at least one POD identifier for MI workbook').toBeGreaterThan(0);
  const templateRes = await Request.get(PDT_2177_TEMPLATE_URL);
  await expect(templateRes).CheckResponse();
  const templateBuffer = await templateRes.body();

  const workbook = new ExcelJS.Workbook();
  await workbook.xlsx.load(templateBuffer as unknown as ExcelJS.Buffer);
  const worksheet = workbook.worksheets[0];
  expect(worksheet, 'SUPPLY_ACTION_DEACTIVATIONS template worksheet').toBeTruthy();
  podIdentifiers.forEach((identifier, index) => {
    worksheet!.getCell(`A${index + 2}`).value = identifier;
  });

  const out = await workbook.xlsx.writeBuffer();
  return Buffer.from(out);
}

export async function uploadSupplyActionDeactivationFileForPods(
  FileUploadRequest: FixtureFileUpload,
  fileBuffer: Buffer,
  deactivationDateIso: string,
): Promise<{ status: number; bodyText: string }> {
  const uploadUrl = `${PDT_2177_UPLOAD_BASE}?date=${encodeURIComponent(deactivationDateIso)}`;
  const upload = await FileUploadRequest.post(uploadUrl, {
    multipart: {
      file: {
        name: `phn-3998-supply-action-deactivation-${randomGens.generateCurrentTimeStamp()}.xlsx`,
        mimeType: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
        buffer: fileBuffer,
      },
    },
  });
  const bodyText = await upload.text();
  return { status: upload.status(), bodyText };
}

export async function runMultiPodSupplyActionDeactivationMassImport(
  Request: FixtureRequest,
  FileUploadRequest: FixtureFileUpload,
  podIdentifiers: string[],
  deactivationDateIso: string,
): Promise<{
  processId: number;
  processStatus: string;
  uploadStatus: number;
  errorReportSummary: string;
  failedRowCount: number;
}> {
  const processIdsBefore = new Set(await listSupplyActionDeactivationProcessIds(Request));
  const fileBuffer = await buildSupplyActionDeactivationWorkbookForPods(Request, podIdentifiers);
  const upload = await uploadSupplyActionDeactivationFileForPods(
    FileUploadRequest,
    fileBuffer,
    deactivationDateIso,
  );
  expect(
    upload.status,
    `SUPPLY_ACTION_DEACTIVATIONS upload accepted (body=${upload.bodyText.slice(0, 300)})`,
  ).toBeGreaterThanOrEqual(200);
  expect(upload.status).toBeLessThan(300);

  const processId = await resolveSupplyActionDeactivationProcessId(Request, processIdsBefore);
  const process = await pollProcessUntilCompleted(Request, processId);

  let errorReport: MassImportErrorReportSnapshot | null = null;
  try {
    const reportBuffer = await downloadMassImportErrorReport(Request, processId);
    errorReport = await loadMassImportErrorReportSnapshot(reportBuffer);
    await test.info().attach(`[PHN-3998] process ${processId} — MASS_IMPORT_ERROR_REPORT`, {
      body: formatMassImportErrorReportForAttach(errorReport),
      contentType: 'text/plain; charset=utf-8',
    });
  } catch (error) {
    const message = error instanceof Error ? error.message : String(error);
    await test.info().attach(`[PHN-3998] process ${processId} — error report unavailable`, {
      body: message,
      contentType: 'text/plain; charset=utf-8',
    });
  }

  const failedRowCount = errorReport?.failedRows.length ?? 0;
  expect(
    failedRowCount,
    `Information of deactivation must succeed for both PODs (error report: ${errorReport?.summary ?? 'unavailable'})`,
  ).toBe(0);

  return {
    processId,
    processStatus: process.status,
    uploadStatus: upload.status,
    errorReportSummary: errorReport?.summary ?? 'error report not downloaded',
    failedRowCount,
  };
}

type ActionListRow = {
  id?: number;
  actionTypeName?: string;
  podIdentifiers?: string;
  contractNumber?: string;
  executionDate?: string;
};

export async function listActionsBySearch(
  Request: FixtureRequest,
  searchBy: 'CUSTOMER_IDENTIFIER' | 'POD_IDENTIFIER',
  prompt: string,
): Promise<ActionListRow[]> {
  const res = await Request.get('actions/list', {
    params: {
      page: 0,
      size: 50,
      sortBy: 'ID',
      sortDirection: 'DESC',
      searchBy,
      prompt,
      exactMatch: true,
    },
  });
  await expect(res).CheckResponse();
  const body = (await res.json()) as { content?: ActionListRow[] };
  return body.content ?? [];
}

export async function listActionsByCustomerIdentifier(
  Request: FixtureRequest,
  customerIdentifier: string,
): Promise<ActionListRow[]> {
  return listActionsBySearch(Request, 'CUSTOMER_IDENTIFIER', customerIdentifier);
}

export async function loadActionDetail(
  Request: FixtureRequest,
  actionId: number,
): Promise<ActionDetailSnapshot> {
  const res = await Request.get(`actions/${actionId}`);
  await expect(res).CheckResponse();
  const body = (await res.json()) as {
    id?: number;
    actionTypeId?: number;
    actionTypeName?: string;
    productContractId?: number;
    penaltyId?: number;
    executionDate?: string;
  };
  return {
    id: entityId(body),
    actionTypeId: body.actionTypeId == null ? null : Number(body.actionTypeId),
    actionTypeName: String(body.actionTypeName ?? ''),
    productContractId: body.productContractId == null ? null : Number(body.productContractId),
    penaltyId: body.penaltyId == null ? null : Number(body.penaltyId),
    executionDate: dateOnly(body.executionDate),
  };
}

export async function waitAndLoadCreatedActions(
  Request: FixtureRequest,
  customerIdentifier: string,
  podIdentifiers: string[],
  deactivationDateIso: string,
  expectedMinCount = 2,
): Promise<ActionDetailSnapshot[]> {
  const pods = podIdentifiers.map((p) => p.trim()).filter(Boolean);
  let lastIds: number[] = [];
  for (let attempt = 0; attempt < 20; attempt++) {
    const rowsByPod: ActionListRow[] = [];
    for (const pod of pods) {
      rowsByPod.push(...(await listActionsBySearch(Request, 'POD_IDENTIFIER', pod)));
    }
    const rowsByCustomer = await listActionsByCustomerIdentifier(Request, customerIdentifier);
    const rows = [...rowsByPod, ...rowsByCustomer];
    const matching = rows.filter((row) => {
      const podsCell = String(row.podIdentifiers ?? '');
      const exec = dateOnly(row.executionDate);
      const podHit = pods.some((pod) => podsCell.includes(pod));
      const dateHit = !exec || exec === deactivationDateIso;
      return podHit && dateHit;
    });
    lastIds = [
      ...new Set(
        matching
          .map((row) => Number(row.id))
          .filter((id) => Number.isFinite(id) && id > 0),
      ),
    ];
    if (lastIds.length >= expectedMinCount) {
      break;
    }
    await sleep(2000);
  }
  expect(
    lastIds.length,
    `expected at least ${expectedMinCount} actions for PODs ${podIdentifiers.join(', ')} after Information of deactivation (found ${lastIds.length})`,
  ).toBeGreaterThanOrEqual(expectedMinCount);

  const details: ActionDetailSnapshot[] = [];
  for (const id of lastIds) {
    details.push(await loadActionDetail(Request, id));
  }
  return details;
}

export function assertAllActionsWithoutNoticeType(
  actions: ActionDetailSnapshot[],
  expectedContractIds: number[],
  withoutNoticePenaltyByContract: Record<number, number>,
  withNoticePenaltyId: number,
): {
  passed: boolean;
  expectedResult: string;
  actualResult: string;
} {
  const expectedResult =
    `Two actions (one per product contract) with actionTypeId=${POD_TERMINATION_WITHOUT_NOTICE_ACTION_TYPE_ID} ` +
    `(${POD_TERMINATION_WITHOUT_NOTICE_ACTION_TYPE_NAME}); none with with-notice id ${POD_TERMINATION_WITH_NOTICE_ACTION_TYPE_ID}; ` +
    `each action uses that contract's without-notice penalty, not penalty ${withNoticePenaltyId}.`;
  const summary = actions
    .map(
      (a) =>
        `id=${a.id} typeId=${a.actionTypeId} name=${a.actionTypeName} contract=${a.productContractId} penalty=${a.penaltyId}`,
    )
    .join(' | ');

  expect(actions.length, `expected 2 actions (one per contract); got: ${summary}`).toBe(2);
  const wrongType = actions.filter(
    (a) => a.actionTypeId !== POD_TERMINATION_WITHOUT_NOTICE_ACTION_TYPE_ID,
  );
  expect(
    wrongType.length,
    `actions must all be without-notice type ${POD_TERMINATION_WITHOUT_NOTICE_ACTION_TYPE_ID}; got: ${summary}`,
  ).toBe(0);
  for (const action of actions) {
    expect(action.actionTypeName).toBe(POD_TERMINATION_WITHOUT_NOTICE_ACTION_TYPE_NAME);
    expect(action.actionTypeId).not.toBe(POD_TERMINATION_WITH_NOTICE_ACTION_TYPE_ID);
    expect(action.penaltyId, `action ${action.id} must not claim the with-notice penalty`).not.toBe(
      withNoticePenaltyId,
    );
    const contractId = action.productContractId;
    expect(contractId, `action ${action.id} must have productContractId`).toBeTruthy();
    const expectedPenalty = withoutNoticePenaltyByContract[Number(contractId)];
    expect(
      action.penaltyId,
      `action ${action.id} on contract ${contractId} must use without-notice penalty ${expectedPenalty}`,
    ).toBe(expectedPenalty);
  }
  const contractIds = new Set(actions.map((a) => a.productContractId).filter((id) => id != null));
  for (const contractId of expectedContractIds) {
    expect(
      contractIds.has(contractId),
      `missing action for product contract ${contractId}; got: ${summary}`,
    ).toBe(true);
  }
  return {
    passed: true,
    expectedResult,
    actualResult: summary,
  };
}

export { isoToday };
