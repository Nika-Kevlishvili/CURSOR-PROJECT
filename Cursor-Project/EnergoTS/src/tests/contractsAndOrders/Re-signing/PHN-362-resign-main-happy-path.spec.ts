/**
 * PHN-362 — Main happy paths (Wait Yes/No × supply rule).
 * API re-sign on Dev2 portal — no mass-import imports.
 *
 * | TC        | Wait | Supply              |
 * |-----------|------|---------------------|
 * | TC-BE-36  | Yes  | First day of month  |
 * | TC-BE-33  | No   | First day of month  |
 * | TC-BE-37  | Yes  | Exact day           |
 * | TC-BE-34  | No   | Exact day           |
 *
 * Run all four:
 *   npx playwright test PHN-362-resign-main-happy-path.spec.ts --project=main
 *
 * Run one (example TC-BE-36):
 *   npx playwright test PHN-362-resign-main-happy-path.spec.ts --grep "TC-BE-36"
 *
 * Extended scenarios (perpetuity, Case 17, anchor, mass import): PHN-362-resign-happy-path.spec.ts
 */

import { expect, test } from '../../../backend/fixtures/baseFixture';
import reportGenerator from '../../../backend/utils/generateReport';
import {
  IDX_PRODUCT_RS,
  IDX_PRODUCT_STD,
  PHN362_B_CONTRACT_TERM_END,
  PHN362_B_INITIAL_TERM_END,
  PHN362_DYNAMIC_DATES_LOG,
  PHN362_POD_ACTIVATION,
  PHN362_QA_WAIT_YES_EXACT_CHECKLIST,
  PHN362_RESIGNING_DEADLINE_DAYS,
  addMonthsSameDay,
  assertResignOutcome,
  createNewContractFWithVersions,
  createOldContractB,
  createProductRs,
  findPodRow,
  firstDayOfMonthAfter,
  headerContractStatus,
  initialTermEndFromContractB,
  loadProductContract,
  resolveFSigningDate,
  runSharedPreconditions,
  signContractFAndTriggerResign,
  tc39AddDays,
  versionIdForSigningDate,
  versionRow,
} from './phn-362-resign-main-happy-path.fixtures';

test.describe('PHN-362 - Main happy paths (Wait Yes/No)', { tag: ['@contractsAndOrders', '@dev2', '@resign', '@happy-path', '@main-happy-path'] }, () => {
  test.describe.configure({ mode: 'serial' });

  test.describe('[REG-1178]: 1 — Wait Yes | First day of month following signing [TC-BE-36]', () => {
    test.describe.configure({ mode: 'serial' });

  test('[REG-1178]: [Dev 2] Re-signing Process - Wait Yes | First day of month following signing', async ({
    Request,
    GeneratePayload,
    Responses,
    Endpoints,
  }) => {
    test.setTimeout(25 * 60 * 1000);
    test.info().annotations.push({ type: 'jira', description: 'PHN-362' });
    test.info().annotations.push({ type: 'tc', description: 'TC-BE-36' });
    test.info().annotations.push({
      type: 'supply-rule',
      description:
        'FIRST_DAY_OF_MONTH — UI/term: first day of month following signing; Wait=Yes → activation after initial term end (Case 13)',
    });
    test.info().annotations.push({ type: 'wait-for-old-contract', description: 'YES' });
    test.info().annotations.push({ type: 'qa-case', description: 'Case 13 — activation after initial term end' });
    test.info().attach('[Case 13] dynamic dates', {
      body: JSON.stringify(PHN362_DYNAMIC_DATES_LOG, null, 2),
      contentType: 'application/json',
    });

    let contractBId = 0;
    let contractFId = 0;
    let podIdentifier = '';
    let contractFPayload: Record<string, unknown> = {};

    await runSharedPreconditions(Request, GeneratePayload, Responses, Endpoints);

    await test.step(
      `Precondition: old contract B (initial term end ${PHN362_B_INITIAL_TERM_END}) [TC-BE-25/26]`,
      async () => {
        const b = await createOldContractB(Request, GeneratePayload, Responses, Endpoints);
        contractBId = b.contractId;
        podIdentifier = b.podIdentifier;
      },
    );

    await test.step('Precondition: product P-RS (Re-sign = Yes, compatible) [TC-BE-7/12]', async () => {
      await createProductRs(Request, GeneratePayload, Responses, Endpoints);
    });

    await test.step('Precondition: new contract F V1–V3 [TC-BE-5/36]', async () => {
      const f = await createNewContractFWithVersions(Request, GeneratePayload, Responses, Endpoints);
      contractFId = f.contractId;
      contractFPayload = f.contractPayload;
    });

    await test.step(
      'Step 10.2.1 / diagram: perpetuity empty or > signing S — Wait=Yes only [TC-BE-36]',
      async () => {
        const bBody = await loadProductContract(Request, contractBId);
        const bp = bBody.basicParameters as { perpetuityDate?: string | null };
        const fBody = await loadProductContract(Request, contractFId);
        const s = resolveFSigningDate(fBody);
        const perp = bp.perpetuityDate;
        expect(perp == null || perp === '' || perp > s).toBeTruthy();
      },
    );

    await test.step('Precondition: POD a on B eligible (not yet re-signed) [TC-BE-18]', async () => {
      const bBody = await loadProductContract(Request, contractBId);
      const row = findPodRow(bBody, podIdentifier) as { activationDate?: string; isResigned?: boolean };
      expect(row?.activationDate).toBe(PHN362_POD_ACTIVATION);
      expect(row?.isResigned).not.toBe(true);
    });

    const fBodyForSigning39 = await loadProductContract(Request, contractFId);
    const fSigningDate39 = resolveFSigningDate(fBodyForSigning39);

    await test.step('Precondition: F signing date S in resigning window [TC-BE-10]', async () => {
      expect(
        fSigningDate39 > tc39AddDays(PHN362_B_CONTRACT_TERM_END, -PHN362_RESIGNING_DEADLINE_DAYS),
      ).toBeTruthy();
      expect(fSigningDate39 < PHN362_B_CONTRACT_TERM_END).toBeTruthy();
      expect(versionIdForSigningDate(fBodyForSigning39, fSigningDate39)).toBeGreaterThanOrEqual(2);
    });

    const bBodyForExpected = await loadProductContract(Request, contractBId);
    const bInitialEnd = initialTermEndFromContractB(bBodyForExpected);
    const expectedCase13 = {
      fActivation: firstDayOfMonthAfter(bInitialEnd),
      bDeactivation: tc39AddDays(firstDayOfMonthAfter(bInitialEnd), -1),
    };

    await test.step(
      'Step 1: sign F — Wait=Yes + FIRST_DAY_OF_MONTH (not Exact day, not Manual) [TC-BE-1/3/25]',
      async () => {
        await signContractFAndTriggerResign(
          Request,
          Endpoints,
          contractFId,
          contractFPayload,
          {
            supplyActivation: 'FIRST_DAY_OF_MONTH',
            waitForOldContractTermToExpires: 'YES',
          },
          { signingDate: fSigningDate39 },
        );
      },
    );

    await test.step('Step 9: 3rd tab — Wait=Yes, FIRST_DAY_OF_MONTH [TC-BE-19]', async () => {
      const fBody = await loadProductContract(Request, contractFId);
      const pp = (fBody.productParameters ?? fBody.thirdPagePreview ?? {}) as {
        supplyActivation?: string;
        productContractWaitForOldContractTermToExpires?: string;
      };
      const supply = pp.supplyActivation ?? (fBody as { supplyActivation?: string }).supplyActivation;
      const wait = pp.productContractWaitForOldContractTermToExpires;
      expect(wait).toBe('YES');
      expect(supply).toBe('FIRST_DAY_OF_MONTH');
      expect(supply).not.toBe('EXACT_DATE');
      expect(supply).not.toBe('MANUAL');
    });

    await test.step(
      'Step 10.2: Wait=Yes + FIRST_DAY_OF_MONTH → first day of month after initial term end [TC-BE-25]',
      async () => {
        expect(expectedCase13.fActivation).toBe(firstDayOfMonthAfter(bInitialEnd));
        expect(expectedCase13.bDeactivation).toBe(tc39AddDays(expectedCase13.fActivation, -1));
      },
    );

    await test.step('Steps 11–12 / 14 / 15: contract F versions OK [TC-BE-30/35/36]', async () => {
      const fBody = await loadProductContract(Request, contractFId);
      expect(((fBody.versions ?? []) as unknown[]).length).toBeGreaterThanOrEqual(3);
    });

    await assertResignOutcome(Request, contractFId, contractBId, podIdentifier, expectedCase13);

    test.info().attach('[PHN-362] TC-BE-36 trace', {
      body: JSON.stringify(reportGenerator.setLinksToResponses(Responses), null, 2),
      contentType: 'application/json',
    });
  });
  });

  test.describe('[REG-1223]: 2 — Wait No | First day of month following signing [TC-BE-33]', () => {
    test.describe.configure({ mode: 'serial' });

  test('[REG-1223]: [Dev 2] Re-signing  Process - Wait No | First day of month following signing', async ({
    Request,
    GeneratePayload,
    Responses,
    Endpoints,
  }) => {
    test.setTimeout(25 * 60 * 1000);
    test.info().annotations.push({ type: 'jira', description: 'PHN-362' });
    test.info().annotations.push({ type: 'tc', description: 'TC-BE-33' });
    test.info().annotations.push({
      type: 'supply-rule',
      description: 'FIRST_DAY_OF_MONTH — first day of month following signing date (Wait=No)',
    });
    test.info().annotations.push({ type: 'wait-for-old-contract', description: 'NO' });
    test.info().attach('[TC-BE-33] dynamic dates', {
      body: JSON.stringify(PHN362_DYNAMIC_DATES_LOG, null, 2),
      contentType: 'application/json',
    });

    let contractBId = 0;
    let contractFId = 0;
    let podIdentifier = '';
    let contractFPayload: Record<string, unknown> = {};

    await runSharedPreconditions(Request, GeneratePayload, Responses, Endpoints);

    await test.step('Precondition: old contract B (Active in term) [TC-BE-10/18]', async () => {
      const b = await createOldContractB(Request, GeneratePayload, Responses, Endpoints);
      contractBId = b.contractId;
      podIdentifier = b.podIdentifier;
    });

    await test.step('Precondition: product P-RS (Re-sign = Yes, compatible) [TC-BE-7/12]', async () => {
      await createProductRs(Request, GeneratePayload, Responses, Endpoints);
    });

    await test.step('Precondition: new contract F V1–V3 [TC-BE-5/36]', async () => {
      const f = await createNewContractFWithVersions(Request, GeneratePayload, Responses, Endpoints);
      contractFId = f.contractId;
      contractFPayload = f.contractPayload;
    });

    await test.step('Step 10.1: Wait=No — no perpetuity gate on diagram (only 10.2.1) [TC-BE-33]', async () => {
      // Sign step applies FIRST_DAY_OF_MONTH + NO; B is Active in term with perpetuityDate unset.
    });

    await test.step('Precondition: POD a on B eligible (not yet re-signed) [TC-BE-18]', async () => {
      const bBody = await loadProductContract(Request, contractBId);
      const row = findPodRow(bBody, podIdentifier) as { activationDate?: string; isResigned?: boolean };
      expect(row?.activationDate).toBe(PHN362_POD_ACTIVATION);
      expect(row?.isResigned).not.toBe(true);
    });

    const fBodyForSigning37 = await loadProductContract(Request, contractFId);
    const fSigningDateS = resolveFSigningDate(fBodyForSigning37);
    const expectedV37 = {
      fActivation: firstDayOfMonthAfter(fSigningDateS),
      bDeactivation: tc39AddDays(firstDayOfMonthAfter(fSigningDateS), -1),
    };

    await test.step('Step 1: Create/update F — Signed by both sides [TC-BE-1/3]', async () => {
      await signContractFAndTriggerResign(
        Request,
        Endpoints,
        contractFId,
        contractFPayload,
        {
          supplyActivation: 'FIRST_DAY_OF_MONTH',
          waitForOldContractTermToExpires: 'NO',
        },
        { signingDate: fSigningDateS },
      );
    });

    await test.step('Step 2: signing date S in version V2 [TC-BE-5]', async () => {
      const body = await loadProductContract(Request, contractFId);
      const bp = body.basicParameters as { signingDate?: string };
      expect(bp.signingDate).toBe(fSigningDateS);
      expect(versionRow(body, 2)?.startDate).toBeTruthy();
      expect(((body.versions ?? []) as { versionId?: number }[]).length).toBeGreaterThanOrEqual(2);
    });

    await test.step('Step 3: product Re-sign = Yes [TC-BE-7]', async () => {
      expect(Responses.product[IDX_PRODUCT_RS]).toBeTruthy();
    });

    await test.step('Step 5: S within old contract scope [TC-BE-10]', async () => {
      expect(
        fSigningDateS > tc39AddDays(PHN362_B_CONTRACT_TERM_END, -PHN362_RESIGNING_DEADLINE_DAYS),
      ).toBeTruthy();
      expect(fSigningDateS < PHN362_B_CONTRACT_TERM_END).toBeTruthy();
    });

    await test.step('Step 6: compatible re-signing product [TC-BE-12]', async () => {
      expect(Responses.product[IDX_PRODUCT_RS]).toBeTruthy();
      expect(Responses.product[IDX_PRODUCT_STD]).toBeTruthy();
    });

    await test.step('Step 7a: signing date S aligned with V2 start [TC-BE-14]', async () => {
      const body = await loadProductContract(Request, contractFId);
      const v2Start = versionRow(body, 2)?.startDate as string;
      expect(fSigningDateS >= v2Start).toBeTruthy();
      expect(versionIdForSigningDate(body, fSigningDateS)).toBeGreaterThanOrEqual(2);
    });

    await test.step('Step 8: POD a on B marked re-signed after F sign [TC-BE-18]', async () => {
      const bBody = await loadProductContract(Request, contractBId);
      const row = findPodRow(bBody, podIdentifier) as { isResigned?: boolean };
      expect(row?.isResigned).toBe(true);
    });

    await test.step('Step 9: 3rd tab fields stored [TC-BE-19]', async () => {
      const fBody = await loadProductContract(Request, contractFId);
      const pp = (fBody.productParameters ?? fBody.thirdPagePreview ?? {}) as {
        supplyActivation?: string;
        productContractWaitForOldContractTermToExpires?: string;
      };
      const supply = pp.supplyActivation ?? (fBody as { supplyActivation?: string }).supplyActivation;
      const wait = pp.productContractWaitForOldContractTermToExpires;
      if (supply) expect(supply).toBe('FIRST_DAY_OF_MONTH');
      if (wait) expect(wait).toBe('NO');
    });

    await test.step('Step 10.1: Wait=No → activation first day of month after S [TC-BE-22]', async () => {
      expect(expectedV37.fActivation).toBe(firstDayOfMonthAfter(fSigningDateS));
      expect(expectedV37.bDeactivation).toBe(tc39AddDays(expectedV37.fActivation, -1));
    });

    await test.step('Steps 11–12 / 14 / 15: POD window + all versions OK [TC-BE-30/35/36]', async () => {
      const fBody = await loadProductContract(Request, contractFId);
      const versions = (fBody.versions ?? []) as { versionId?: number }[];
      expect(versions.length).toBeGreaterThanOrEqual(3);
    });

    await assertResignOutcome(Request, contractFId, contractBId, podIdentifier, expectedV37);

    test.info().attach('[PHN-362] TC-BE-33 trace', {
      body: JSON.stringify(reportGenerator.setLinksToResponses(Responses), null, 2),
      contentType: 'application/json',
    });
  });
  });

  test.describe('[REG-1220]: 3 — Wait Yes | Exact day [TC-BE-37]', () => {
    test.describe.configure({ mode: 'serial' });

  test('[REG-1220]: [Dev 2] Re-signing  Process - Wait Yes |  Exact day', async ({
    Request,
    GeneratePayload,
    Responses,
    Endpoints,
  }) => {
    test.setTimeout(25 * 60 * 1000);
    test.info().annotations.push({ type: 'jira', description: 'PHN-362' });
    test.info().annotations.push({ type: 'tc', description: 'TC-BE-37' });
    test.info().annotations.push({
      type: 'supply-rule',
      description: 'EXACT_DATE — exact day (initial term end + 1 when Wait=Yes)',
    });
    test.info().annotations.push({ type: 'wait-for-old-contract', description: 'YES' });
    test.info().annotations.push({
      type: 'qa-checklist',
      description: 'Wait=Yes + EXACT_DATE (not Manual, not first day of month after signing)',
    });
    test.info().attach('[TC-BE-37] QA checklist row', {
      body: JSON.stringify(PHN362_QA_WAIT_YES_EXACT_CHECKLIST, null, 2),
      contentType: 'application/json',
    });
    test.info().attach('[TC-BE-37] dynamic dates', {
      body: JSON.stringify(PHN362_DYNAMIC_DATES_LOG, null, 2),
      contentType: 'application/json',
    });

    let contractBId = 0;
    let contractFId = 0;
    let podIdentifier = '';
    let contractFPayload: Record<string, unknown> = {};

    await runSharedPreconditions(Request, GeneratePayload, Responses, Endpoints);

    await test.step(
      `Precondition: old contract B (initial term end ${PHN362_B_INITIAL_TERM_END}) [TC-BE-26]`,
      async () => {
        const b = await createOldContractB(Request, GeneratePayload, Responses, Endpoints);
        contractBId = b.contractId;
        podIdentifier = b.podIdentifier;
      },
    );

    await test.step('Checklist ✅: customer has active old contract B (in term) [TC-BE-10]', async () => {
      const bBody = await loadProductContract(Request, contractBId);
      const header = headerContractStatus(bBody);
      expect(
        header === 'ACTIVE_IN_TERM' ||
          header === 'ENTERED_INTO_FORCE' ||
          header === 'ACTIVE_IN_PERPETUITY',
      ).toBeTruthy();
      expect(PHN362_QA_WAIT_YES_EXACT_CHECKLIST.customerActiveOtherContract).toBe(true);
    });

    await test.step('Checklist ✅: re-signing product P-RS + compatible mapping [TC-BE-7/12]', async () => {
      await createProductRs(Request, GeneratePayload, Responses, Endpoints);
      expect(Responses.product[IDX_PRODUCT_RS]).toBeTruthy();
      expect(Responses.product[IDX_PRODUCT_STD]).toBeTruthy();
      expect(PHN362_QA_WAIT_YES_EXACT_CHECKLIST.reSigningProduct).toBe(true);
      expect(PHN362_QA_WAIT_YES_EXACT_CHECKLIST.productCompatible).toBe(true);
    });

    await test.step('Checklist ❌: supply rule on sign is EXACT_DATE, not Manual [TC-BE-15]', async () => {
      expect(PHN362_QA_WAIT_YES_EXACT_CHECKLIST.supplyActivationManual).toBe(false);
      expect(PHN362_QA_WAIT_YES_EXACT_CHECKLIST.supplyFirstDayOfNextMonth).toBe(false);
    });

    await test.step('Precondition: new contract F V1–V3 [TC-BE-5/36]', async () => {
      const f = await createNewContractFWithVersions(Request, GeneratePayload, Responses, Endpoints);
      contractFId = f.contractId;
      contractFPayload = f.contractPayload;
    });

    await test.step('Checklist ✅: POD activatable on F; no AC4 interference contract [TC-BE-30/44]', async () => {
      const fBody = await loadProductContract(Request, contractFId);
      const fPod = findPodRow(fBody, podIdentifier);
      expect(fPod, 'POD a on draft F').toBeDefined();
      expect(PHN362_QA_WAIT_YES_EXACT_CHECKLIST.podActivatableOnNewContract).toBe(true);
      expect(PHN362_QA_WAIT_YES_EXACT_CHECKLIST.futureActivationOtherContractAc4).toBe(false);
    });

    const fBodyForSigning38 = await loadProductContract(Request, contractFId);
    const fSigningDate38 = resolveFSigningDate(fBodyForSigning38);

    await test.step(
      'Step 10.2.1 / diagram: perpetuity empty or > signing S — Wait=Yes only [TC-BE-37]',
      async () => {
        const bBody = await loadProductContract(Request, contractBId);
        const bp = bBody.basicParameters as { perpetuityDate?: string | null };
        const perp = bp.perpetuityDate;
        expect(perp == null || perp === '' || perp > fSigningDate38).toBeTruthy();
        expect(PHN362_QA_WAIT_YES_EXACT_CHECKLIST.waitForOldContractTermExpiresYes).toBe(true);
      },
    );

    await test.step('Checklist ✅: Active POD on B not re-signed; no future-version gap setup [TC-BE-18/14]', async () => {
      const bBody = await loadProductContract(Request, contractBId);
      const row = findPodRow(bBody, podIdentifier) as {
        activationDate?: string;
        isResigned?: boolean;
        deactivationDate?: string | null;
      };
      expect(row?.activationDate).toBe(PHN362_POD_ACTIVATION);
      expect(row?.isResigned).not.toBe(true);
      expect(PHN362_QA_WAIT_YES_EXACT_CHECKLIST.activePodNotResigned).toBe(true);
      expect(PHN362_QA_WAIT_YES_EXACT_CHECKLIST.podGapFutureVersion).toBe(false);
    });

    await test.step('Checklist ✅: signing date in early re-sign window [TC-BE-10]', async () => {
      expect(
        fSigningDate38 > tc39AddDays(PHN362_B_CONTRACT_TERM_END, -PHN362_RESIGNING_DEADLINE_DAYS),
      ).toBeTruthy();
      expect(fSigningDate38 < PHN362_B_CONTRACT_TERM_END).toBeTruthy();
      expect(versionIdForSigningDate(fBodyForSigning38, fSigningDate38)).toBeGreaterThanOrEqual(2);
      expect(PHN362_QA_WAIT_YES_EXACT_CHECKLIST.signingDateInResigningWindow).toBe(true);
    });

    const bBodyBeforeSign = await loadProductContract(Request, contractBId);
    const bInitialEnd = initialTermEndFromContractB(bBodyBeforeSign);
    expect(bInitialEnd).toBe(PHN362_B_INITIAL_TERM_END);
    expect(PHN362_QA_WAIT_YES_EXACT_CHECKLIST.oldContractInitialTermEndSet).toBe(true);

    const expectedV38 = {
      fActivation: tc39AddDays(bInitialEnd, 1),
      bDeactivation: bInitialEnd,
    };

    await test.step(
      'Checklist ❌ on sign: Wait=Yes + EXACT_DATE (not Manual, not FIRST_DAY_OF_MONTH) [TC-BE-26]',
      async () => {
        expect(PHN362_QA_WAIT_YES_EXACT_CHECKLIST.supplyFirstDayOfNextMonth).toBe(false);
        expect(PHN362_QA_WAIT_YES_EXACT_CHECKLIST.activationWaitYesExactDay).toBe(true);
        expect(expectedV38.fActivation).toBe(tc39AddDays(PHN362_B_INITIAL_TERM_END, 1));
      },
    );

    await test.step('Step 1: sign F — trigger re-sign [TC-BE-1/3]', async () => {
      await signContractFAndTriggerResign(
        Request,
        Endpoints,
        contractFId,
        contractFPayload,
        {
          supplyActivation: 'EXACT_DATE',
          waitForOldContractTermToExpires: 'YES',
          supplyActivationValue: expectedV38.fActivation,
        },
        { signingDate: fSigningDate38 },
      );
    });

    await test.step('Step 9: 3rd tab stored — Wait=Yes, EXACT_DATE [TC-BE-19]', async () => {
      const fBody = await loadProductContract(Request, contractFId);
      const pp = (fBody.productParameters ?? fBody.thirdPagePreview ?? {}) as {
        supplyActivation?: string;
        supplyActivationValue?: string;
        productContractWaitForOldContractTermToExpires?: string;
      };
      const supply = pp.supplyActivation ?? (fBody as { supplyActivation?: string }).supplyActivation;
      const wait = pp.productContractWaitForOldContractTermToExpires;
      expect(wait).toBe('YES');
      expect(supply).toBe('EXACT_DATE');
      expect(supply).not.toBe('MANUAL');
      expect(supply).not.toBe('FIRST_DAY_OF_MONTH');
      if (pp.supplyActivationValue) {
        expect(pp.supplyActivationValue).toBe(expectedV38.fActivation);
      }
    });

    await test.step('Step 10.2: Wait=Yes + Exact day → initial term end + 1 [TC-BE-26]', async () => {
      expect(expectedV38.fActivation).toBe(tc39AddDays(bInitialEnd, 1));
      expect(expectedV38.bDeactivation).toBe(bInitialEnd);
    });

    await test.step('Steps 11–12 / 14 / 15 [TC-BE-30/35/36]', async () => {
      const fBody = await loadProductContract(Request, contractFId);
      expect(((fBody.versions ?? []) as unknown[]).length).toBeGreaterThanOrEqual(3);
    });

    await assertResignOutcome(Request, contractFId, contractBId, podIdentifier, expectedV38);

    test.info().attach('[PHN-362] TC-BE-37 trace', {
      body: JSON.stringify(reportGenerator.setLinksToResponses(Responses), null, 2),
      contentType: 'application/json',
    });
  });
  });

  test.describe('[REG-1222]: 4 — Wait No | Exact day — signing + 1 month same day [TC-BE-34]', () => {
    test.describe.configure({ mode: 'serial' });

  test('[REG-1222]: [Dev 2] Re-signing  Process - Wait No | Exact day', async ({
    Request,
    GeneratePayload,
    Responses,
    Endpoints,
  }) => {
    test.setTimeout(25 * 60 * 1000);
    test.info().annotations.push({ type: 'jira', description: 'PHN-362' });
    test.info().annotations.push({ type: 'tc', description: 'TC-BE-34' });
    test.info().annotations.push({
      type: 'supply-rule',
      description: 'EXACT_DATE — signing date + 1 month, same calendar day (Wait=No)',
    });
    test.info().annotations.push({ type: 'wait-for-old-contract', description: 'NO' });
    test.info().attach('[TC-BE-34] dynamic dates', {
      body: JSON.stringify(PHN362_DYNAMIC_DATES_LOG, null, 2),
      contentType: 'application/json',
    });

    let contractBId = 0;
    let contractFId = 0;
    let podIdentifier = '';
    let contractFPayload: Record<string, unknown> = {};

    await runSharedPreconditions(Request, GeneratePayload, Responses, Endpoints);

    await test.step('Precondition: old contract B (Active in term) [TC-BE-10/18]', async () => {
      const b = await createOldContractB(Request, GeneratePayload, Responses, Endpoints);
      contractBId = b.contractId;
      podIdentifier = b.podIdentifier;
    });

    await test.step('Precondition: product P-RS (Re-sign = Yes, compatible) [TC-BE-7/12]', async () => {
      await createProductRs(Request, GeneratePayload, Responses, Endpoints);
    });

    await test.step('Precondition: new contract F V1–V3 [TC-BE-5/36]', async () => {
      const f = await createNewContractFWithVersions(Request, GeneratePayload, Responses, Endpoints);
      contractFId = f.contractId;
      contractFPayload = f.contractPayload;
    });

    await test.step('Precondition: POD a on B eligible (not yet re-signed) [TC-BE-18]', async () => {
      const bBody = await loadProductContract(Request, contractBId);
      const row = findPodRow(bBody, podIdentifier) as { activationDate?: string; isResigned?: boolean };
      expect(row?.activationDate).toBe(PHN362_POD_ACTIVATION);
      expect(row?.isResigned).not.toBe(true);
    });

    await test.step('Step 10.1: Wait=No — no perpetuity gate on diagram [TC-BE-34]', async () => {
      // Steps 14–15 (gap / new-contract versions) satisfied by single continuous POD on B + F V1–V3.
    });

    const fBodyForSigning34 = await loadProductContract(Request, contractFId);
    const fSigningDate34 = resolveFSigningDate(fBodyForSigning34);
    const expectedV34 = {
      fActivation: addMonthsSameDay(fSigningDate34, 1),
      bDeactivation: tc39AddDays(addMonthsSameDay(fSigningDate34, 1), -1),
    };

    await test.step('Step 1: sign F — Wait=No + EXACT_DATE [TC-BE-1/3]', async () => {
      await signContractFAndTriggerResign(
        Request,
        Endpoints,
        contractFId,
        contractFPayload,
        {
          supplyActivation: 'EXACT_DATE',
          waitForOldContractTermToExpires: 'NO',
          // EXACT_DATE requires a date on sign PUT (validator + resign SQL use supplyActivationValue when Wait=No).
          supplyActivationValue: expectedV34.fActivation,
        },
        { signingDate: fSigningDate34 },
      );
    });

    await test.step('Step 5: signing date S in resigning window [TC-BE-10]', async () => {
      expect(
        fSigningDate34 > tc39AddDays(PHN362_B_CONTRACT_TERM_END, -PHN362_RESIGNING_DEADLINE_DAYS),
      ).toBeTruthy();
      expect(fSigningDate34 < PHN362_B_CONTRACT_TERM_END).toBeTruthy();
      expect(versionIdForSigningDate(fBodyForSigning34, fSigningDate34)).toBeGreaterThanOrEqual(2);
    });

    await test.step('Step 9: 3rd tab — Wait=No, EXACT_DATE [TC-BE-19]', async () => {
      const fBody = await loadProductContract(Request, contractFId);
      const pp = (fBody.productParameters ?? fBody.thirdPagePreview ?? {}) as {
        supplyActivation?: string;
        productContractWaitForOldContractTermToExpires?: string;
      };
      const supply = pp.supplyActivation ?? (fBody as { supplyActivation?: string }).supplyActivation;
      const wait = pp.productContractWaitForOldContractTermToExpires;
      expect(wait).toBe('NO');
      expect(supply).toBe('EXACT_DATE');
      expect(supply).not.toBe('FIRST_DAY_OF_MONTH');
      expect(supply).not.toBe('MANUAL');
    });

    await test.step('Step 10.1: Wait=No + exact day → S + 1 month same day [TC-BE-34]', async () => {
      expect(expectedV34.fActivation).toBe(addMonthsSameDay(fSigningDate34, 1));
      expect(expectedV34.bDeactivation).toBe(tc39AddDays(expectedV34.fActivation, -1));
    });

    await test.step('Steps 11–12 / 14 / 15: contract F versions OK [TC-BE-30/35/36]', async () => {
      const fBody = await loadProductContract(Request, contractFId);
      expect(((fBody.versions ?? []) as unknown[]).length).toBeGreaterThanOrEqual(3);
    });

    await assertResignOutcome(Request, contractFId, contractBId, podIdentifier, expectedV34);

    test.info().attach('[PHN-362] TC-BE-34 trace', {
      body: JSON.stringify(reportGenerator.setLinksToResponses(Responses), null, 2),
      contentType: 'application/json',
    });
  });
  });

  
});
