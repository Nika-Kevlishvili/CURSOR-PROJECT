/**
 * PHN-362 mass-import resign — upload Excel + sign F (cases 07, 10, 11, 12).
 */

import fs from 'fs';
import { expect, test } from '../../fixtures/baseFixture';
import type { baseFixture } from '../../fixtures/baseFixture';
import {
  MSG_RESIGNING_FAILED_ALL_PODS,
  MSG_RESIGNING_FAILED_ALL_PODS_FULL,
  MSG_VERSION_NOT_EXIST,
  addDaysIso,
  assertPodNotResignedOnOldContract,
  assertPodResignedOnOldContract,
  buildEditProductContractPayload,
  finalizeResignTest,
  firstDayOfMonthAfter,
  getContractPods,
  signContractExpectError,
  signContractF,
} from './contract-resign-new-flow.fixtures';
import { runMassImportOldContractPrep } from './phn-362-mass-import-old-contract.fixtures';
import { runMassImportPodCasePrep } from './phn-362-mass-import-pod-case.fixtures';
import {
  PREP_B_INITIAL_TERM_SIGN_VALUE,
  PREP_EXPECTED_AFTER_RESIGN,
  PREP_RUNTIME_TODAY,
} from './phn-362-resign-two-pods-precondition.fixtures';
import { loadProductContract } from './pdt-2815-version-validity.fixtures';
import {
  assertCase14PodSubrangesOnF,
  assertPodPresentOnAllFVersions,
} from './phn-362-resign-case-14-bugfix.fixtures';

type MiResignCtx = Pick<
  baseFixture,
  'Request' | 'GeneratePayload' | 'Responses' | 'Endpoints' | 'FileUploadRequest'
>;

async function findProductContractIdByNumber(
  Request: baseFixture['Request'],
  contractNumber: string,
): Promise<number> {
  const list = await Request.post('product-contract/list', {
    data: { page: 0, size: 10, prompt: contractNumber, types: ['CONTRACT'] },
  });
  await expect(list).CheckResponse();
  const body = (await list.json()) as { content?: { id?: number; contractNumber?: string }[] };
  const hit = (body.content ?? []).find((r) => r.contractNumber === contractNumber);
  if (!hit?.id) {
    throw new Error(`Contract ${contractNumber} not found after mass import`);
  }
  return Number(hit.id);
}

async function waitForMassImportNotification(Request: baseFixture['Request']): Promise<void> {
  const pattern = /PRODUCT_CONTRACTS?_MASS_IMPORT_COMPLETED/i;
  for (let attempt = 0; attempt < 30; attempt++) {
    const res = await Request.get('notifications?size=10&page=0');
    await expect(res).CheckResponse();
    const body = (await res.json()) as { content?: { notificationType?: string }[] };
    const notifications = Array.isArray(body?.content) ? body.content : [];
    if (notifications.some((n) => n.notificationType && pattern.test(String(n.notificationType)))) {
      return;
    }
    await new Promise((r) => setTimeout(r, 1000));
  }
  throw new Error('Mass import completion notification not received within 30s');
}

export async function uploadProductContractMassImportExcel(
  Request: baseFixture['Request'],
  FileUploadRequest: baseFixture['FileUploadRequest'],
  excelPath: string,
): Promise<void> {
  const buffer = fs.readFileSync(excelPath);
  const upload = await FileUploadRequest.post('product-contract/import', {
    multipart: {
      file: {
        name: excelPath.split(/[/\\]/).pop() ?? 'import.xlsx',
        mimeType: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
        buffer,
      },
    },
  });
  if (!upload.ok()) {
    const text = await upload.text();
    throw new Error(`Mass import upload failed ${upload.status()}: ${text.slice(0, 500)}`);
  }
  await waitForMassImportNotification(Request);
}

export type MassImportResignPrep = Awaited<ReturnType<typeof runMassImportOldContractPrep>>;

const POD_MASS_IMPORT_CASE_IDS = new Set(['07', '10', '11', '12', '14']);

export async function runMassImportPrepAndUpload(
  ctx: MiResignCtx,
  caseId: string,
): Promise<MassImportResignPrep & { contractFId: number }> {
  const prep = POD_MASS_IMPORT_CASE_IDS.has(caseId)
    ? await runMassImportPodCasePrep(caseId, ctx)
    : await runMassImportOldContractPrep(
        ctx.Request,
        ctx.GeneratePayload,
        ctx.Responses,
        ctx.Endpoints,
        ctx.FileUploadRequest,
        { caseId },
      );

  await test.step(`Mass import Excel case ${caseId}`, async () => {
    await uploadProductContractMassImportExcel(ctx.Request, ctx.FileUploadRequest, prep.excelPath);
  });

  const contractFId = await findProductContractIdByNumber(ctx.Request, prep.fContractNumber);
  return { ...prep, contractFId };
}

function expectedWaitYesFirstDayActivation(): { fActivation: string; bDeactivation: string } {
  const fActivation = firstDayOfMonthAfter(PREP_B_INITIAL_TERM_SIGN_VALUE);
  return { fActivation, bDeactivation: addDaysIso(fActivation, -1) };
}

/** Case 07 — AC4 warning; sign without removeFuturePods. */
export async function runPhn362MassImportCase07Resign(ctx: MiResignCtx) {
  const prep = await runMassImportPrepAndUpload(ctx, '07');
  const expected = expectedWaitYesFirstDayActivation();

  await signContractExpectError(
    { ...ctx },
    prep.contractFId,
    {
      signingDate: PREP_RUNTIME_TODAY,
      removeFuturePods: false,
      waitExpire: 'YES',
      supplyActivation: 'FIRST_DAY_OF_MONTH',
      productIndex: 1,
    },
    [MSG_RESIGNING_FAILED_ALL_PODS, MSG_RESIGNING_FAILED_ALL_PODS_FULL],
  );

  await assertPodNotResignedOnOldContract(ctx.Request, prep.contractBId, prep.podId);
  await finalizeResignTest(
    { ...ctx },
    {
      label: 'PHN-362 Case 07 (mass import)',
      oldContractId: prep.contractBId,
      newContractId: prep.contractFId,
      expectResignLinks: false,
    },
  );

  test.info().annotations.push({
    type: 'expected-activation',
    description: `calculated F activation ${expected.fActivation} outside B POD window ending ${expected.bDeactivation}`,
  });
}

/** Case 10 — POD not activatable in new contract version. */
export async function runPhn362MassImportCase10Resign(ctx: MiResignCtx) {
  const prep = await runMassImportPrepAndUpload(ctx, '10');

  await test.step('Precondition: future F version — POD not activatable at signing', async () => {
    const generated = (await ctx.GeneratePayload.contractsAndOrders.product_contract(0, 1)) as Record<
      string,
      unknown
    >;
    const signingDate = PREP_RUNTIME_TODAY;
    const editPayload = await buildEditProductContractPayload(ctx.Request, prep.contractFId, generated, {
      forDraftV1: false,
      preserveSigningDate: false,
    });
    editPayload.savingAsNewVersion = true;
    editPayload.startDate = addDaysIso(signingDate, 45);
    const put = await ctx.Request.put(
      `${ctx.Endpoints.productContract}/${prep.contractFId}?versionId=1&changeFutureVersionsPods=true`,
      { data: editPayload },
    );
    await expect(put).CheckResponse();
  });

  await signContractExpectError(
    { ...ctx },
    prep.contractFId,
    { productIndex: 1, waitExpire: 'YES', supplyActivation: 'FIRST_DAY_OF_MONTH' },
    [MSG_RESIGNING_FAILED_ALL_PODS, MSG_RESIGNING_FAILED_ALL_PODS_FULL, MSG_VERSION_NOT_EXIST],
  );

  await assertPodNotResignedOnOldContract(ctx.Request, prep.contractBId, prep.podId);
  await finalizeResignTest(
    { ...ctx },
    {
      label: 'PHN-362 Case 10 (mass import)',
      oldContractId: prep.contractBId,
      newContractId: prep.contractFId,
      expectResignLinks: false,
    },
  );
}

/** Case 11 — supply straddles two new versions (E row in Excel performs sign). */
export async function runPhn362MassImportCase11Resign(ctx: MiResignCtx) {
  const prep = await runMassImportPrepAndUpload(ctx, '11');
  const fBody = await loadProductContract(ctx.Request, prep.contractFId);
  const versions = (fBody.versions ?? []) as { versionId?: number }[];
  expect(versions.length).toBeGreaterThanOrEqual(2);

  const bp = fBody.basicParameters as { status?: string; resignedTo?: unknown[] };
  expect(['ACTIVE_IN_TERM', 'ENTERED_INTO_FORCE', 'ACTIVE_IN_PERPETUITY', 'SIGNED']).toContain(
    String(bp.status ?? ''),
  );

  await assertPodResignedOnOldContract(ctx.Request, prep.contractBId, prep.podId);
  await finalizeResignTest(
    { ...ctx },
    {
      label: 'PHN-362 Case 11 (mass import)',
      oldContractId: prep.contractBId,
      newContractId: prep.contractFId,
      expectResignLinks: true,
    },
  );
}

/** Case 12 — open-ended old POD; new inherits open-ended deactivation. */
export async function runPhn362MassImportCase12Resign(ctx: MiResignCtx) {
  const prep = await runMassImportPrepAndUpload(ctx, '12');

  const body = await signContractF(
    { ...ctx },
    prep.contractFId,
    {
      waitExpire: 'YES',
      supplyActivation: 'FIRST_DAY_OF_MONTH',
      productIndex: 1,
    },
  );
  expect(body.id).toBeTruthy();

  const newPods = await getContractPods(ctx.Request, prep.contractFId);
  const newRow = newPods.find((p) => Number(p.podId) === prep.podId);
  expect(newRow?.activationDate).toBeTruthy();
  expect(newRow?.deactivationDate == null || newRow?.deactivationDate === '').toBeTruthy();

  const oldPods = await getContractPods(ctx.Request, prep.contractBId);
  const oldRow = oldPods.find((p) => Number(p.podId) === prep.podId);
  expect(oldRow?.deactivationDate).toBe(addDaysIso(String(newRow?.activationDate), -1));

  await finalizeResignTest(
    { ...ctx },
    {
      label: 'PHN-362 Case 12 (mass import)',
      oldContractId: prep.contractBId,
      newContractId: prep.contractFId,
      expectResignLinks: true,
    },
  );

  test.info().annotations.push({
    type: 'open-ended',
    description: `F POD open-ended; B deactivated ${oldRow?.deactivationDate}`,
  });
}

/** Case 14 — mass import multi-version F (1/2/4/3) + E-row sign; per-version POD sub-ranges. */
export async function runPhn362MassImportCase14Resign(ctx: MiResignCtx) {
  const prep = await runMassImportPrepAndUpload(ctx, '14');

  const fBody = await loadProductContract(ctx.Request, prep.contractFId);
  const bp = fBody.basicParameters as { status?: string };
  expect(['ACTIVE_IN_TERM', 'ENTERED_INTO_FORCE', 'ACTIVE_IN_PERPETUITY', 'SIGNED']).toContain(
    String(bp.status ?? ''),
  );

  await assertCase14PodSubrangesOnF(ctx.Request, prep.contractFId, prep.podId);
  await assertPodResignedOnOldContract(ctx.Request, prep.contractBId, prep.podId);

  await finalizeResignTest(
    { ...ctx },
    {
      label: 'PHN-362 Case 14 (mass import)',
      oldContractId: prep.contractBId,
      newContractId: prep.contractFId,
      expectResignLinks: true,
    },
  );
}

export { PREP_EXPECTED_AFTER_RESIGN };
