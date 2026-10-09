/**
 * PHN-4048 — Regression of Billing data by scales create and table-only edit on Dev2
 * after the create-path performance optimization. Behaviour must stay the same.
 *
 * Maps 1:1 to Cursor-Project/test_cases/Backend/Billing_data_by_scales_create_and_edit.md
 * (TC-BE-1 … TC-BE-25). No Frontend TCs. No scales mass-import URL.
 *
 * Swagger: Cursor-Project/config/swagger/dev2/swagger-spec.json (refreshed 2026-08-20).
 * Runtime statuses: POST 201, PUT 200, GET 200, list 206, DELETE 200.
 *
 * Reference spec(s):
 * - tests/cursor/pdt-3223-invoice-json-tablepcscales-listpc-order.fixtures.ts
 * - tests/cursor/PHN-4002-product-contract-sales-channel-filter.spec.ts
 * - tests/cursor/PHN-3992-invoice-cancel-closed-accounting-period.spec.ts
 * - tests/cursor/cursor-test.fixtures.ts
 * - tests/billing/forVolumes/forVolumes.spec.ts
 * - fixtures/constants/endpoints.ts
 * - jsons/payloads/create/energyData/dataByScales(ScaleCode).ts
 */
import { test, expect } from './cursor-test.fixtures';
import { finalizeTestRunSummary } from './shared/manual-verification-links.fixtures';
import {
  PHN_4048_JIRA_TITLE,
  PHN_4048_KEY,
  DATE_FROM,
  DATE_TO,
  acquireDataByScalesLock,
  asNumber,
  assertWithCheck,
  createBillingByScalesPrereqs,
  createHeader,
  deleteBillingByScales,
  editTablePayload,
  expectHttp400Containing,
  expectNoListRowForIdentifier,
  getBillingByScales,
  listByPodIdentifier,
  postBillingByScales,
  postBillingByScalesExpecting400,
  putBillingByScales,
  scaleCodeRow,
  tableRows,
  tariffRow,
  uniquePodIdentifier,
  ymd,
  type Phn4048Fx,
} from './phn-4048-billing-data-by-scales-create-and-edit.fixtures';

const TAGS = ['@phn-4048', '@billingByScales', '@dev2'];

function fxFrom(args: {
  Request: Phn4048Fx['Request'];
  GeneratePayload: Phn4048Fx['GeneratePayload'];
  Responses: Phn4048Fx['Responses'];
  Endpoints: Phn4048Fx['Endpoints'];
  Nomenclatures: Phn4048Fx['Nomenclatures'];
}): Phn4048Fx {
  return args;
}

function normalizeMessage(message: string): string {
  return message.replace(/[\u2018\u2019\u0060]/g, "'");
}

async function attachSummary(
  TestRunSummary: Parameters<typeof finalizeTestRunSummary>[0],
  Responses: Phn4048Fx['Responses'],
  snapshot: Record<string, unknown>,
): Promise<void> {
  await test.step('Attach test run summary', async () => {
    finalizeTestRunSummary(TestRunSummary, Responses, {
      jiraKey: PHN_4048_KEY,
      relevantEntityKeys: ['pod', 'meters', 'dataByScales'],
      snapshot,
    });
  });
}

test.describe(`[${PHN_4048_KEY}]: ${PHN_4048_JIRA_TITLE}`, { tag: TAGS }, () => {
  test(`[${PHN_4048_KEY}] TC-BE-1: Create scale-code row for a full month matching the header`, async ({
    Request,
    GeneratePayload,
    Responses,
    Endpoints,
    Nomenclatures,
    TestRunSummary,
  }) => {
    test.setTimeout(10 * 60 * 1000);
    const fx = fxFrom({ Request, GeneratePayload, Responses, Endpoints, Nomenclatures });
    const chain = await test.step(
      'Precondition: create grid operator, user type, country, scale, POD, meter',
      async () => createBillingByScalesPrereqs(fx, { tag: 'BE1' }),
    );

    const payload = createHeader(chain.identifier, 'INVBE1', [scaleCodeRow(chain)]);
    TestRunSummary.registerPayload('billingByScales', payload);

    const id = await test.step('POST /billing-by-scales scaleCode full month', async () =>
      postBillingByScales(fx, payload),
    );
    const view = await test.step('GET /billing-by-scales/{id}', async () => getBillingByScales(fx, id));
    const row = tableRows(view)[0] ?? {};

    assertWithCheck(
      TestRunSummary,
      'Create scale-code row covering the header month',
      'POST 201 with numeric id; GET 200; header dates 2026-02-01/2026-02-28; ACTIVE; scaleCode 1; readings 0→100; isLocked false.',
      `As expected — id=${id} status=${String(view.status)} scaleCode=${String(row.scaleCode)} new=${String(row.newMeterReading)} isLocked=${String(view.isLocked)}.`,
      () => {
        expect(id).toBeGreaterThan(0);
        expect(String(view.identifier)).toBe(chain.identifier);
        expect(ymd(view.dateFrom)).toBe(DATE_FROM);
        expect(ymd(view.dateTo)).toBe(DATE_TO);
        expect(asNumber(view.billingPowerInKw)).toBe(10);
        expect(String(view.invoiceNumber)).toBe('INVBE1');
        expect(view.correction).toBe(false);
        expect(view.override).toBe(false);
        expect(String(view.status)).toBe('ACTIVE');
        expect(tableRows(view)).toHaveLength(1);
        expect(ymd(row.periodFrom)).toBe(DATE_FROM);
        expect(ymd(row.periodTo)).toBe(DATE_TO);
        expect(String(row.meterNumber)).toBe(chain.meterNumber);
        expect(asNumber(row.newMeterReading)).toBe(100);
        expect(asNumber(row.oldMeterReading)).toBe(0);
        expect(asNumber(row.difference)).toBe(100);
        expect(asNumber(row.multiplier)).toBe(1);
        expect(asNumber(row.totalVolumes)).toBe(100);
        expect(asNumber(row.index)).toBe(0);
        expect(String(row.scaleCode)).toBe(chain.scaleCode);
        expect(view.isLocked).toBe(false);
      },
    );

    await attachSummary(TestRunSummary, Responses, { id, identifier: chain.identifier });
  });

  test(`[${PHN_4048_KEY}] TC-BE-2: Create tariff-scale row with volumes, unitPrice, and totalValue`, async ({
    Request,
    GeneratePayload,
    Responses,
    Endpoints,
    Nomenclatures,
    TestRunSummary,
  }) => {
    test.setTimeout(10 * 60 * 1000);
    const fx = fxFrom({ Request, GeneratePayload, Responses, Endpoints, Nomenclatures });
    const chain = await test.step(
      'Precondition: create grid operator, user type, country, scale, POD, meter',
      async () => createBillingByScalesPrereqs(fx, { tag: 'BE2' }),
    );
    const payload = createHeader(chain.identifier, 'INVBE2', [tariffRow(chain)]);
    TestRunSummary.registerPayload('billingByScales', payload);
    const id = await test.step('POST /billing-by-scales tariffScale row', async () =>
      postBillingByScales(fx, payload),
    );
    const view = await test.step('GET /billing-by-scales/{id}', async () => getBillingByScales(fx, id));
    const row = tableRows(view)[0] ?? {};

    assertWithCheck(
      TestRunSummary,
      'Create tariffScale row without scaleCode/scaleNumber',
      'POST 201; GET table volumes/unitPrice/totalValue=100; tariffScale TARIFF1; scaleCode null; ACTIVE.',
      `As expected — id=${id} tariffScale=${String(row.tariffScale)} scaleCode=${String(row.scaleCode)} volumes=${String(row.volumes)}.`,
      () => {
        expect(tableRows(view)).toHaveLength(1);
        expect(asNumber(row.volumes)).toBe(100);
        expect(asNumber(row.unitPrice)).toBe(1);
        expect(asNumber(row.totalValue)).toBe(100);
        expect(String(row.meterNumber)).toBe(chain.meterNumber);
        expect(String(row.tariffScale)).toBe(chain.tariffScale);
        expect(row.scaleCode == null || row.scaleCode === '').toBe(true);
        expect(ymd(view.dateFrom)).toBe(DATE_FROM);
        expect(ymd(view.dateTo)).toBe(DATE_TO);
        expect(String(view.status)).toBe('ACTIVE');
      },
    );
    await attachSummary(TestRunSummary, Responses, { id, identifier: chain.identifier });
  });

  test(`[${PHN_4048_KEY}] TC-BE-3: Create parent-only record with an empty table`, async ({
    Request,
    GeneratePayload,
    Responses,
    Endpoints,
    Nomenclatures,
    TestRunSummary,
  }) => {
    test.setTimeout(10 * 60 * 1000);
    const fx = fxFrom({ Request, GeneratePayload, Responses, Endpoints, Nomenclatures });
    const chain = await test.step(
      'Precondition: create grid operator, user type, country, scale, POD, meter',
      async () => createBillingByScalesPrereqs(fx, { tag: 'BE3' }),
    );
    const payload = createHeader(chain.identifier, 'INVBE3', []);
    TestRunSummary.registerPayload('billingByScales', payload);
    const id = await test.step('POST /billing-by-scales empty table', async () =>
      postBillingByScales(fx, payload),
    );
    const view = await test.step('GET /billing-by-scales/{id}', async () => getBillingByScales(fx, id));

    assertWithCheck(
      TestRunSummary,
      'Create parent-only with billingByScalesTableCreateRequests []',
      'POST 201; GET 200; empty table; header INVBE3; ACTIVE.',
      `As expected — id=${id} tableLength=${tableRows(view).length} invoiceNumber=${String(view.invoiceNumber)}.`,
      () => {
        expect(String(view.identifier)).toBe(chain.identifier);
        expect(ymd(view.dateFrom)).toBe(DATE_FROM);
        expect(ymd(view.dateTo)).toBe(DATE_TO);
        expect(asNumber(view.billingPowerInKw)).toBe(10);
        expect(String(view.invoiceNumber)).toBe('INVBE3');
        expect(view.correction).toBe(false);
        expect(view.override).toBe(false);
        expect(String(view.status)).toBe('ACTIVE');
        expect(tableRows(view)).toHaveLength(0);
      },
    );
    await attachSummary(TestRunSummary, Responses, { id, identifier: chain.identifier });
  });

  test(`[${PHN_4048_KEY}] TC-BE-4: Create correction=true when an original record matches the header dates`, async ({
    Request,
    GeneratePayload,
    Responses,
    Endpoints,
    Nomenclatures,
    TestRunSummary,
  }) => {
    test.setTimeout(10 * 60 * 1000);
    const fx = fxFrom({ Request, GeneratePayload, Responses, Endpoints, Nomenclatures });
    const chain = await test.step(
      'Precondition: create nomenclature, POD, meter, and original billing-by-scales',
      async () => {
        const created = await createBillingByScalesPrereqs(fx, { tag: 'BE4' });
        const originalPayload = createHeader(created.identifier, 'INVBE4A', []);
        TestRunSummary.registerPayload('originalBillingByScales', originalPayload);
        const originalId = await postBillingByScales(fx, originalPayload);
        return { ...created, originalId };
      },
    );
    const correctionPayload = createHeader(chain.identifier, 'INVBE4B', [], {
      correction: true,
      invoiceCorrection: 'CORR1',
    });
    TestRunSummary.registerPayload('billingByScales', correctionPayload);
    const correctionId = await test.step('POST /billing-by-scales correction=true', async () =>
      postBillingByScales(fx, correctionPayload),
    );
    const correctionView = await getBillingByScales(fx, correctionId);
    const originalView = await getBillingByScales(fx, chain.originalId);

    assertWithCheck(
      TestRunSummary,
      'Correction create when original matches header dates',
      'POST 201; correctionId ≠ originalId; correction true/CORR1 ACTIVE; original correction false ACTIVE.',
      `As expected — originalId=${chain.originalId} correctionId=${correctionId}.`,
      () => {
        expect(correctionId).not.toBe(chain.originalId);
        expect(correctionView.correction).toBe(true);
        expect(String(correctionView.invoiceCorrection)).toBe('CORR1');
        expect(ymd(correctionView.dateFrom)).toBe(DATE_FROM);
        expect(ymd(correctionView.dateTo)).toBe(DATE_TO);
        expect(String(correctionView.status)).toBe('ACTIVE');
        expect(originalView.correction).toBe(false);
        expect(String(originalView.status)).toBe('ACTIVE');
      },
    );
    await attachSummary(TestRunSummary, Responses, {
      originalId: chain.originalId,
      correctionId,
      identifier: chain.identifier,
    });
  });

  test(`[${PHN_4048_KEY}] TC-BE-5: Create with override=true and correction=true when an original exists`, async ({
    Request,
    GeneratePayload,
    Responses,
    Endpoints,
    Nomenclatures,
    TestRunSummary,
  }) => {
    test.setTimeout(10 * 60 * 1000);
    const fx = fxFrom({ Request, GeneratePayload, Responses, Endpoints, Nomenclatures });
    const chain = await test.step(
      'Precondition: create nomenclature, POD, meter, and original billing-by-scales',
      async () => {
        const created = await createBillingByScalesPrereqs(fx, { tag: 'BE5' });
        await postBillingByScales(fx, createHeader(created.identifier, 'INVBE5A', []));
        return created;
      },
    );
    const payload = createHeader(chain.identifier, 'INVBE5B', [], {
      correction: true,
      override: true,
      invoiceCorrection: 'CORR2',
    });
    TestRunSummary.registerPayload('billingByScales', payload);
    const id = await test.step('POST /billing-by-scales override+correction', async () =>
      postBillingByScales(fx, payload),
    );
    const view = await getBillingByScales(fx, id);

    assertWithCheck(
      TestRunSummary,
      'Create with override=true and correction=true',
      'POST 201; GET correction true, override true, invoiceCorrection CORR2, ACTIVE, header dates unchanged.',
      `As expected — id=${id} correction=${String(view.correction)} override=${String(view.override)}.`,
      () => {
        expect(view.correction).toBe(true);
        expect(view.override).toBe(true);
        expect(String(view.invoiceCorrection)).toBe('CORR2');
        expect(String(view.status)).toBe('ACTIVE');
        expect(ymd(view.dateFrom)).toBe(DATE_FROM);
        expect(ymd(view.dateTo)).toBe(DATE_TO);
      },
    );
    await attachSummary(TestRunSummary, Responses, { id, identifier: chain.identifier });
  });

  test(`[${PHN_4048_KEY}] TC-BE-6: Create with an intermediate-period gap when saveRecordForIntermediatePeriod=true`, async ({
    Request,
    GeneratePayload,
    Responses,
    Endpoints,
    Nomenclatures,
    TestRunSummary,
  }) => {
    test.setTimeout(10 * 60 * 1000);
    const fx = fxFrom({ Request, GeneratePayload, Responses, Endpoints, Nomenclatures });
    const chain = await test.step(
      'Precondition: create grid operator, user type, country, scale, POD, meter',
      async () => createBillingByScalesPrereqs(fx, { tag: 'BE6' }),
    );
    const payload = createHeader(chain.identifier, 'INVBE6', [
      scaleCodeRow(chain, { periodTo: '2026-02-14' }),
    ]);
    TestRunSummary.registerPayload('billingByScales', payload);
    const id = await test.step('POST /billing-by-scales partial-month row, flag true', async () =>
      postBillingByScales(fx, payload),
    );
    const view = await getBillingByScales(fx, id);
    const row = tableRows(view)[0] ?? {};

    assertWithCheck(
      TestRunSummary,
      'Intermediate gap accepted when saveRecordForIntermediatePeriod=true',
      'POST 201; GET table periodTo 2026-02-14; header dateTo 2026-02-28; ACTIVE.',
      `As expected — id=${id} rowTo=${ymd(row.periodTo)} headerTo=${ymd(view.dateTo)}.`,
      () => {
        expect(tableRows(view)).toHaveLength(1);
        expect(ymd(row.periodTo)).toBe('2026-02-14');
        expect(ymd(view.dateTo)).toBe(DATE_TO);
        expect(String(view.status)).toBe('ACTIVE');
      },
    );
    await attachSummary(TestRunSummary, Responses, { id, identifier: chain.identifier });
  });

  test(`[${PHN_4048_KEY}] TC-BE-7: Create with consecutive reading mismatch when saveRecordForMeterReadings=true`, async ({
    Request,
    GeneratePayload,
    Responses,
    Endpoints,
    Nomenclatures,
    TestRunSummary,
  }) => {
    test.setTimeout(10 * 60 * 1000);
    const fx = fxFrom({ Request, GeneratePayload, Responses, Endpoints, Nomenclatures });
    const chain = await test.step(
      'Precondition: create grid operator, user type, country, scale, POD, meter',
      async () => createBillingByScalesPrereqs(fx, { tag: 'BE7' }),
    );
    const payload = createHeader(chain.identifier, 'INVBE7', [
      scaleCodeRow(chain, {
        periodFrom: DATE_FROM,
        periodTo: '2026-02-14',
        oldMeterReading: 0,
        newMeterReading: 100,
        difference: 100,
        totalVolumes: 100,
        index: 0,
      }),
      scaleCodeRow(chain, {
        periodFrom: '2026-02-16',
        periodTo: DATE_TO,
        oldMeterReading: 150,
        newMeterReading: 200,
        difference: 50,
        totalVolumes: 50,
        index: 1,
      }),
    ]);
    TestRunSummary.registerPayload('billingByScales', payload);
    const id = await test.step('POST /billing-by-scales mismatched consecutive readings, flag true', async () =>
      postBillingByScales(fx, payload),
    );
    const view = await getBillingByScales(fx, id);
    const rows = tableRows(view);

    assertWithCheck(
      TestRunSummary,
      'Reading mismatch stored when saveRecordForMeterReadings=true',
      'POST 201; GET two rows; index0 new=100; index1 old=150; ACTIVE.',
      `As expected — id=${id} rows=${rows.length}.`,
      () => {
        expect(rows).toHaveLength(2);
        expect(asNumber(rows[0].newMeterReading)).toBe(100);
        expect(asNumber(rows[1].oldMeterReading)).toBe(150);
        expect(String(view.status)).toBe('ACTIVE');
      },
    );
    await attachSummary(TestRunSummary, Responses, { id, identifier: chain.identifier });
  });

  test(`[${PHN_4048_KEY}] TC-BE-8: Edit table-only readings and volumes on a non-invoiced record`, async ({
    Request,
    GeneratePayload,
    Responses,
    Endpoints,
    Nomenclatures,
    TestRunSummary,
  }) => {
    test.setTimeout(10 * 60 * 1000);
    const fx = fxFrom({ Request, GeneratePayload, Responses, Endpoints, Nomenclatures });
    const chain = await test.step(
      'Precondition: create nomenclature, POD, meter, and billing-by-scales row',
      async () => {
        const created = await createBillingByScalesPrereqs(fx, { tag: 'BE8' });
        const createPayload = createHeader(created.identifier, 'INVBE8', [
          scaleCodeRow(created),
        ]);
        TestRunSummary.registerPayload('billingByScales', createPayload);
        const billingByScaleId = await postBillingByScales(fx, createPayload);
        const before = await getBillingByScales(fx, billingByScaleId);
        expect(before.isLocked).toBe(false);
        return { ...created, billingByScaleId };
      },
    );

    await test.step('POST /locks/acquire entityType=data-by-scales', async () => {
      await acquireDataByScalesLock(fx, chain.billingByScaleId);
    });
    const editPayload = editTablePayload(chain.billingByScaleId, chain, {
      newMeterReading: 150,
      oldMeterReading: 0,
      difference: 150,
      totalVolumes: 150,
    });
    TestRunSummary.registerPayload('billingByScalesEdit', editPayload);
    const putResult = await test.step('PUT /billing-by-scales table-only', async () =>
      putBillingByScales(fx, editPayload),
    );
    const view = await getBillingByScales(fx, chain.billingByScaleId);
    const row = tableRows(view)[0] ?? {};

    assertWithCheck(
      TestRunSummary,
      'Table-only PUT does not change header dates',
      'Lock 200; PUT 200 body=id; GET dates still Feb 2026; INVBE8; readings 150; ACTIVE.',
      `As expected — putStatus=${putResult.status} dates=${ymd(view.dateFrom)}/${ymd(view.dateTo)} new=${String(row.newMeterReading)}.`,
      () => {
        expect(putResult.status).toBe(200);
        expect(asNumber(putResult.body)).toBe(chain.billingByScaleId);
        expect(ymd(view.dateFrom)).toBe(DATE_FROM);
        expect(ymd(view.dateTo)).toBe(DATE_TO);
        expect(String(view.invoiceNumber)).toBe('INVBE8');
        expect(asNumber(row.newMeterReading)).toBe(150);
        expect(asNumber(row.difference)).toBe(150);
        expect(asNumber(row.totalVolumes)).toBe(150);
        expect(asNumber(row.oldMeterReading)).toBe(0);
        expect(String(view.status)).toBe('ACTIVE');
      },
    );
    await attachSummary(TestRunSummary, Responses, {
      id: chain.billingByScaleId,
      identifier: chain.identifier,
    });
  });

  test(`[${PHN_4048_KEY}] TC-BE-9: Listing finds the created record by POD identifier`, async ({
    Request,
    GeneratePayload,
    Responses,
    Endpoints,
    Nomenclatures,
    TestRunSummary,
  }) => {
    test.setTimeout(10 * 60 * 1000);
    const fx = fxFrom({ Request, GeneratePayload, Responses, Endpoints, Nomenclatures });
    const chain = await test.step(
      'Precondition: create nomenclature, POD, meter, and empty-table billing-by-scales',
      async () => {
        const created = await createBillingByScalesPrereqs(fx, { tag: 'BE9' });
        const payload = createHeader(created.identifier, 'INVBE9', []);
        TestRunSummary.registerPayload('billingByScales', payload);
        const billingByScaleId = await postBillingByScales(fx, payload);
        return { ...created, billingByScaleId };
      },
    );
    const listed = await test.step('GET /billing-by-scales/list by POD_IDENTIFIER', async () =>
      listByPodIdentifier(fx, chain.identifier),
    );
    const match = listed.content.find((row) => asNumber(row.id) === chain.billingByScaleId);

    assertWithCheck(
      TestRunSummary,
      'List by POD identifier includes the created record',
      'HTTP 206; content has id, identifier, Feb dates, ACTIVE; invoiced NO/false.',
      `As expected — listStatus=${listed.status} found=${Boolean(match)} invoiced=${String(match?.invoiced)}.`,
      () => {
        expect(listed.status).toBe(206);
        expect(match, 'list must include created id').toBeTruthy();
        expect(String(match?.identifier)).toBe(chain.identifier);
        expect(ymd(match?.dateFrom)).toBe(DATE_FROM);
        expect(ymd(match?.dateTo)).toBe(DATE_TO);
        expect(String(match?.status)).toBe('ACTIVE');
        const invoiced = match?.invoiced;
        const invoicedIsNo =
          invoiced === 'NO' || invoiced === false || invoiced === 'false' || invoiced == null;
        expect(invoicedIsNo, `invoiced should be NO/false, got ${String(invoiced)}`).toBe(true);
      },
    );
    await attachSummary(TestRunSummary, Responses, {
      id: chain.billingByScaleId,
      identifier: chain.identifier,
    });
  });

  test(`[${PHN_4048_KEY}] TC-BE-10: Reject create when the POD identifier is not an active POD`, async ({
    Request,
    GeneratePayload,
    Responses,
    Endpoints,
    Nomenclatures,
    TestRunSummary,
  }) => {
    test.setTimeout(10 * 60 * 1000);
    const fx = fxFrom({ Request, GeneratePayload, Responses, Endpoints, Nomenclatures });
    const identifier = uniquePodIdentifier('10');
    const payload = createHeader(identifier, 'INVBE10', []);
    TestRunSummary.registerPayload('billingByScales', payload);
    const failed = await test.step('POST /billing-by-scales unknown identifier', async () =>
      postBillingByScalesExpecting400(fx, payload, "Can't find active pod with identifier"),
    );
    await test.step('GET /billing-by-scales/list confirms no persist', async () =>
      expectNoListRowForIdentifier(fx, identifier),
    );

    assertWithCheck(
      TestRunSummary,
      'Unknown POD identifier is rejected',
      'HTTP 400; message contains Can’t find active pod with identifier; no list row.',
      `As expected — HTTP ${failed.status}; ${failed.message}`,
      () => {
        expect(failed.status).toBe(400);
      },
    );
    await attachSummary(TestRunSummary, Responses, { identifier });
  });

  test(`[${PHN_4048_KEY}] TC-BE-11: Reject standard create that overlaps the same POD and period`, async ({
    Request,
    GeneratePayload,
    Responses,
    Endpoints,
    Nomenclatures,
    TestRunSummary,
  }) => {
    test.setTimeout(10 * 60 * 1000);
    const fx = fxFrom({ Request, GeneratePayload, Responses, Endpoints, Nomenclatures });
    const chain = await test.step(
      'Precondition: create original non-correction billing-by-scales',
      async () => {
        const created = await createBillingByScalesPrereqs(fx, { tag: 'BE11' });
        const firstPayload = createHeader(created.identifier, 'INVBE11A', []);
        TestRunSummary.registerPayload('originalBillingByScales', firstPayload);
        const firstId = await postBillingByScales(fx, firstPayload);
        return { ...created, firstId };
      },
    );
    const secondPayload = createHeader(chain.identifier, 'INVBE11B', []);
    TestRunSummary.registerPayload('billingByScales', secondPayload);
    const failed = await test.step('POST overlapping non-correction create', async () =>
      postBillingByScalesExpecting400(fx, secondPayload, 'already exist billing data by scale'),
    );
    const firstView = await getBillingByScales(fx, chain.firstId);

    assertWithCheck(
      TestRunSummary,
      'Second overlapping standard create is rejected',
      'HTTP 400 already exist billing data by scale; first record ACTIVE INVBE11A.',
      `As expected — HTTP ${failed.status}; first status=${String(firstView.status)} invoice=${String(firstView.invoiceNumber)}.`,
      () => {
        expect(String(firstView.status)).toBe('ACTIVE');
        expect(String(firstView.invoiceNumber)).toBe('INVBE11A');
      },
    );
    await attachSummary(TestRunSummary, Responses, {
      firstId: chain.firstId,
      identifier: chain.identifier,
    });
  });

  test(`[${PHN_4048_KEY}] TC-BE-12: Reject correction=true when no original matches the header dates`, async ({
    Request,
    GeneratePayload,
    Responses,
    Endpoints,
    Nomenclatures,
    TestRunSummary,
  }) => {
    test.setTimeout(10 * 60 * 1000);
    const fx = fxFrom({ Request, GeneratePayload, Responses, Endpoints, Nomenclatures });
    const chain = await test.step(
      'Precondition: create POD chain without original billing-by-scales',
      async () => createBillingByScalesPrereqs(fx, { tag: 'BE12' }),
    );
    const payload = createHeader(chain.identifier, 'INVBE12', [], {
      correction: true,
      invoiceCorrection: 'CORR12',
    });
    TestRunSummary.registerPayload('billingByScales', payload);
    const failed = await test.step('POST correction without original', async () =>
      postBillingByScalesExpecting400(fx, payload, 'original scales dates'),
    );
    await expectNoListRowForIdentifier(fx, chain.identifier);

    assertWithCheck(
      TestRunSummary,
      'Correction without matching original is rejected',
      'HTTP 400; message contains original scales dates; no list row.',
      `As expected — HTTP ${failed.status}; ${failed.message}`,
      () => {
        expect(failed.status).toBe(400);
      },
    );
    await attachSummary(TestRunSummary, Responses, { identifier: chain.identifier });
  });

  test(`[${PHN_4048_KEY}] TC-BE-13: Reject a table row whose period is outside the header range`, async ({
    Request,
    GeneratePayload,
    Responses,
    Endpoints,
    Nomenclatures,
    TestRunSummary,
  }) => {
    test.setTimeout(10 * 60 * 1000);
    const fx = fxFrom({ Request, GeneratePayload, Responses, Endpoints, Nomenclatures });
    const chain = await test.step(
      'Precondition: create grid operator, user type, country, scale, POD, meter',
      async () => createBillingByScalesPrereqs(fx, { tag: 'BE13' }),
    );
    const payload = createHeader(chain.identifier, 'INVBE13', [
      scaleCodeRow(chain, { periodFrom: '2026-01-15', periodTo: '2026-02-10' }),
    ]);
    TestRunSummary.registerPayload('billingByScales', payload);
    const failed = await test.step('POST row periodFrom before header dateFrom', async () =>
      postBillingByScalesExpecting400(fx, payload, 'is not in range'),
    );
    await expectNoListRowForIdentifier(fx, chain.identifier);

    assertWithCheck(
      TestRunSummary,
      'Row outside header range is rolled back',
      'HTTP 400 is not in range; no ACTIVE list row.',
      `As expected — HTTP ${failed.status}; ${failed.message}`,
      () => {
        expect(failed.status).toBe(400);
      },
    );
    await attachSummary(TestRunSummary, Responses, { identifier: chain.identifier });
  });

  test(`[${PHN_4048_KEY}] TC-BE-14: Reject a row that fills both scaleCode and tariffScale`, async ({
    Request,
    GeneratePayload,
    Responses,
    Endpoints,
    Nomenclatures,
    TestRunSummary,
  }) => {
    test.setTimeout(10 * 60 * 1000);
    const fx = fxFrom({ Request, GeneratePayload, Responses, Endpoints, Nomenclatures });
    const chain = await test.step(
      'Precondition: create grid operator, user type, country, scale, POD, meter',
      async () => createBillingByScalesPrereqs(fx, { tag: 'BE14' }),
    );
    const bothRow = {
      ...scaleCodeRow(chain),
      tariffScale: chain.tariffScale,
      volumes: 100,
      unitPrice: 1,
      totalValue: 100,
    };
    const payload = createHeader(chain.identifier, 'INVBE14', [bothRow]);
    TestRunSummary.registerPayload('billingByScales', payload);
    const failed = await test.step('POST row with both scaleCode and tariffScale', async () =>
      postBillingByScalesExpecting400(
        fx,
        payload,
        "tariffScale-[TariffScale] can't be filled in while scaleCode is active",
      ),
    );
    await expectNoListRowForIdentifier(fx, chain.identifier);

    assertWithCheck(
      TestRunSummary,
      'Exclusive scaleCode XOR tariffScale',
      'HTTP 400; tariffScale cannot be filled while scaleCode is active; no list row.',
      `As expected — HTTP ${failed.status}; ${failed.message}`,
      () => {
        expect(failed.status).toBe(400);
      },
    );
    await attachSummary(TestRunSummary, Responses, { identifier: chain.identifier });
  });

  test(`[${PHN_4048_KEY}] TC-BE-15: Reject create when meterNumber does not exist`, async ({
    Request,
    GeneratePayload,
    Responses,
    Endpoints,
    Nomenclatures,
    TestRunSummary,
  }) => {
    test.setTimeout(10 * 60 * 1000);
    const fx = fxFrom({ Request, GeneratePayload, Responses, Endpoints, Nomenclatures });
    const chain = await test.step(
      'Precondition: create grid operator, user type, country, scale, POD, meter',
      async () => createBillingByScalesPrereqs(fx, { tag: 'BE15' }),
    );
    const unknownMeter = `9${uniquePodIdentifier('15')}`.replace(/[^0-9A-Za-z]/g, '').slice(0, 16);
    const payload = createHeader(chain.identifier, 'INVBE15', [
      scaleCodeRow(chain, { meterNumber: unknownMeter }),
    ]);
    TestRunSummary.registerPayload('billingByScales', payload);
    const failed = await test.step('POST unknown meterNumber', async () =>
      postBillingByScalesExpecting400(fx, payload, "Can't find Meter By meter number"),
    );
    await expectNoListRowForIdentifier(fx, chain.identifier);

    assertWithCheck(
      TestRunSummary,
      'Unknown meterNumber is rejected',
      'HTTP 400 Can’t find Meter By meter number; no list row.',
      `As expected — HTTP ${failed.status}; ${failed.message}`,
      () => {
        expect(failed.status).toBe(400);
      },
    );
    await attachSummary(TestRunSummary, Responses, { identifier: chain.identifier, unknownMeter });
  });

  test(`[${PHN_4048_KEY}] TC-BE-16: Reject create when the meter is not installed in the row period`, async ({
    Request,
    GeneratePayload,
    Responses,
    Endpoints,
    Nomenclatures,
    TestRunSummary,
  }) => {
    test.setTimeout(10 * 60 * 1000);
    const fx = fxFrom({ Request, GeneratePayload, Responses, Endpoints, Nomenclatures });
    const chain = await test.step(
      'Precondition: meter installment starts 2026-06-01 (after February header)',
      async () =>
        createBillingByScalesPrereqs(fx, {
          tag: 'BE16',
          installmentDate: '2026-06-01',
          removeDate: '2026-12-31',
        }),
    );
    const payload = createHeader(chain.identifier, 'INVBE16', [scaleCodeRow(chain)]);
    TestRunSummary.registerPayload('billingByScales', payload);
    const failed = await test.step('POST February row for June-installed meter', async () =>
      postBillingByScalesExpecting400(fx, payload, 'installed in this POD'),
    );
    await expectNoListRowForIdentifier(fx, chain.identifier);

    assertWithCheck(
      TestRunSummary,
      'Meter not installed in row period',
      'HTTP 400; message contains installed in this POD; no list row.',
      `As expected — HTTP ${failed.status}; ${failed.message}`,
      () => {
        expect(failed.status).toBe(400);
      },
    );
    await attachSummary(TestRunSummary, Responses, { identifier: chain.identifier });
  });

  test(`[${PHN_4048_KEY}] TC-BE-17: Reject create when scaleCode is unknown for the grid operator`, async ({
    Request,
    GeneratePayload,
    Responses,
    Endpoints,
    Nomenclatures,
    TestRunSummary,
  }) => {
    test.setTimeout(10 * 60 * 1000);
    const fx = fxFrom({ Request, GeneratePayload, Responses, Endpoints, Nomenclatures });
    const chain = await test.step(
      'Precondition: create grid operator, user type, country, scale, POD, meter',
      async () => createBillingByScalesPrereqs(fx, { tag: 'BE17' }),
    );
    const payload = createHeader(chain.identifier, 'INVBE17', [
      scaleCodeRow(chain, { scaleCode: '99' }),
    ]);
    TestRunSummary.registerPayload('billingByScales', payload);
    const failed = await test.step('POST unknown scaleCode 99', async () =>
      postBillingByScalesExpecting400(fx, payload, "Can't find active scales nomenclature"),
    );
    expect(normalizeMessage(failed.message)).toContain('scaleCode');
    await expectNoListRowForIdentifier(fx, chain.identifier);

    assertWithCheck(
      TestRunSummary,
      'Unknown scaleCode for grid operator',
      'HTTP 400; Can’t find active scales nomenclature and scaleCode; no list row.',
      `As expected — HTTP ${failed.status}; ${failed.message}`,
      () => {
        expect(failed.status).toBe(400);
      },
    );
    await attachSummary(TestRunSummary, Responses, { identifier: chain.identifier });
  });

  test(`[${PHN_4048_KEY}] TC-BE-18: Reject intermediate-period gap when saveRecordForIntermediatePeriod=false`, async ({
    Request,
    GeneratePayload,
    Responses,
    Endpoints,
    Nomenclatures,
    TestRunSummary,
  }) => {
    test.setTimeout(10 * 60 * 1000);
    const fx = fxFrom({ Request, GeneratePayload, Responses, Endpoints, Nomenclatures });
    const chain = await test.step(
      'Precondition: create grid operator, user type, country, scale, POD, meter',
      async () => createBillingByScalesPrereqs(fx, { tag: 'BE18' }),
    );
    const payload = createHeader(
      chain.identifier,
      'INVBE18',
      [scaleCodeRow(chain, { periodFrom: DATE_FROM, periodTo: '2026-02-14' })],
      { saveRecordForIntermediatePeriod: false, saveRecordForMeterReadings: true },
    );
    TestRunSummary.registerPayload('billingByScales', payload);
    const failed = await test.step('POST partial month with intermediate flag false', async () =>
      postBillingByScalesExpecting400(fx, payload, 'intermediate period'),
    );
    await expectNoListRowForIdentifier(fx, chain.identifier);

    assertWithCheck(
      TestRunSummary,
      'Intermediate gap rejected when flag is false',
      'HTTP 400 intermediate period; no parent persisted.',
      `As expected — HTTP ${failed.status}; ${failed.message}`,
      () => {
        expect(failed.status).toBe(400);
      },
    );
    await attachSummary(TestRunSummary, Responses, { identifier: chain.identifier });
  });

  test(`[${PHN_4048_KEY}] TC-BE-19: Reject consecutive reading mismatch when saveRecordForMeterReadings=false`, async ({
    Request,
    GeneratePayload,
    Responses,
    Endpoints,
    Nomenclatures,
    TestRunSummary,
  }) => {
    test.setTimeout(10 * 60 * 1000);
    const fx = fxFrom({ Request, GeneratePayload, Responses, Endpoints, Nomenclatures });
    const chain = await test.step(
      'Precondition: create grid operator, user type, country, scale, POD, meter',
      async () => createBillingByScalesPrereqs(fx, { tag: 'BE19' }),
    );
    const payload = createHeader(
      chain.identifier,
      'INVBE19',
      [
        scaleCodeRow(chain, {
          periodFrom: DATE_FROM,
          periodTo: '2026-02-14',
          oldMeterReading: 0,
          newMeterReading: 100,
          difference: 100,
          totalVolumes: 100,
          index: 0,
        }),
        scaleCodeRow(chain, {
          periodFrom: '2026-02-16',
          periodTo: DATE_TO,
          oldMeterReading: 150,
          newMeterReading: 200,
          difference: 50,
          totalVolumes: 50,
          index: 1,
        }),
      ],
      { saveRecordForIntermediatePeriod: true, saveRecordForMeterReadings: false },
    );
    TestRunSummary.registerPayload('billingByScales', payload);
    const failed = await test.step('POST mismatched readings with flag false', async () =>
      postBillingByScalesExpecting400(fx, payload, 'should be equal to new Meter reading'),
    );
    await expectNoListRowForIdentifier(fx, chain.identifier);

    assertWithCheck(
      TestRunSummary,
      'Reading mismatch rejected when saveRecordForMeterReadings=false',
      'HTTP 400 should be equal to new Meter reading; no list row.',
      `As expected — HTTP ${failed.status}; ${failed.message}`,
      () => {
        expect(failed.status).toBe(400);
      },
    );
    await attachSummary(TestRunSummary, Responses, { identifier: chain.identifier });
  });

  test(`[${PHN_4048_KEY}] TC-BE-20: Reject two same-scale rows with equal periodFrom`, async ({
    Request,
    GeneratePayload,
    Responses,
    Endpoints,
    Nomenclatures,
    TestRunSummary,
  }) => {
    test.setTimeout(10 * 60 * 1000);
    const fx = fxFrom({ Request, GeneratePayload, Responses, Endpoints, Nomenclatures });
    const chain = await test.step(
      'Precondition: create grid operator, user type, country, scale, POD, meter',
      async () => createBillingByScalesPrereqs(fx, { tag: 'BE20' }),
    );
    const payload = createHeader(chain.identifier, 'INVBE20', [
      scaleCodeRow(chain, {
        periodFrom: DATE_FROM,
        periodTo: '2026-02-14',
        index: 0,
      }),
      scaleCodeRow(chain, {
        periodFrom: DATE_FROM,
        periodTo: DATE_TO,
        index: 1,
      }),
    ]);
    TestRunSummary.registerPayload('billingByScales', payload);
    const failed = await test.step('POST two rows with equal periodFrom', async () =>
      postBillingByScalesExpecting400(fx, payload, 'is equal to'),
    );
    await expectNoListRowForIdentifier(fx, chain.identifier);

    assertWithCheck(
      TestRunSummary,
      'Equal periodFrom on same scale is rejected',
      'HTTP 400 is equal to; no list row.',
      `As expected — HTTP ${failed.status}; ${failed.message}`,
      () => {
        expect(failed.status).toBe(400);
      },
    );
    await attachSummary(TestRunSummary, Responses, { identifier: chain.identifier });
  });

  test(`[${PHN_4048_KEY}] TC-BE-21: Reject two same-scale rows with overlapping sub-periods`, async ({
    Request,
    GeneratePayload,
    Responses,
    Endpoints,
    Nomenclatures,
    TestRunSummary,
  }) => {
    test.setTimeout(10 * 60 * 1000);
    const fx = fxFrom({ Request, GeneratePayload, Responses, Endpoints, Nomenclatures });
    const chain = await test.step(
      'Precondition: create grid operator, user type, country, scale, POD, meter',
      async () => createBillingByScalesPrereqs(fx, { tag: 'BE21' }),
    );
    const payload = createHeader(chain.identifier, 'INVBE21', [
      scaleCodeRow(chain, {
        periodFrom: DATE_FROM,
        periodTo: '2026-02-20',
        index: 0,
      }),
      scaleCodeRow(chain, {
        periodFrom: '2026-02-10',
        periodTo: DATE_TO,
        index: 1,
      }),
    ]);
    TestRunSummary.registerPayload('billingByScales', payload);
    const failed = await test.step('POST overlapping same-scale rows', async () =>
      postBillingByScalesExpecting400(fx, payload, 'has overlap'),
    );
    await expectNoListRowForIdentifier(fx, chain.identifier);

    assertWithCheck(
      TestRunSummary,
      'Overlapping same-scale sub-periods',
      'HTTP 400 has overlap; no list row.',
      `As expected — HTTP ${failed.status}; ${failed.message}`,
      () => {
        expect(failed.status).toBe(400);
      },
    );
    await attachSummary(TestRunSummary, Responses, { identifier: chain.identifier });
  });

  test(`[${PHN_4048_KEY}] TC-BE-22: Reject header period longer than one year`, async ({
    Request,
    GeneratePayload,
    Responses,
    Endpoints,
    Nomenclatures,
    TestRunSummary,
  }) => {
    test.setTimeout(10 * 60 * 1000);
    const fx = fxFrom({ Request, GeneratePayload, Responses, Endpoints, Nomenclatures });
    const chain = await test.step(
      'Precondition: meter window covers 2024-01-01 to 2026-12-31',
      async () =>
        createBillingByScalesPrereqs(fx, {
          tag: 'BE22',
          installmentDate: '2024-01-01',
          removeDate: '2026-12-31',
        }),
    );
    const payload = createHeader(chain.identifier, 'INVBE22', [], {
      dateFrom: '2024-01-01',
      dateTo: '2026-01-02',
      invoiceDate: '2024-06-01T00:00:00.000Z',
    });
    TestRunSummary.registerPayload('billingByScales', payload);
    const failed = await test.step('POST header longer than one year, empty table', async () =>
      postBillingByScalesExpecting400(fx, payload, 'Period should be limited to one-year time interval'),
    );
    await expectNoListRowForIdentifier(fx, chain.identifier);

    assertWithCheck(
      TestRunSummary,
      'Header period > one year',
      'HTTP 400 Period should be limited to one-year time interval; no list row.',
      `As expected — HTTP ${failed.status}; ${failed.message}`,
      () => {
        expect(failed.status).toBe(400);
      },
    );
    await attachSummary(TestRunSummary, Responses, { identifier: chain.identifier });
  });

  test(`[${PHN_4048_KEY}] TC-BE-23: Reject negative totalVolumes when correction is false`, async ({
    Request,
    GeneratePayload,
    Responses,
    Endpoints,
    Nomenclatures,
    TestRunSummary,
  }) => {
    test.setTimeout(10 * 60 * 1000);
    const fx = fxFrom({ Request, GeneratePayload, Responses, Endpoints, Nomenclatures });
    const chain = await test.step(
      'Precondition: create grid operator, user type, country, scale, POD, meter',
      async () => createBillingByScalesPrereqs(fx, { tag: 'BE23' }),
    );
    const payload = createHeader(
      chain.identifier,
      'INVBE23',
      [scaleCodeRow(chain, { totalVolumes: -1, difference: 100 })],
      { correction: false },
    );
    TestRunSummary.registerPayload('billingByScales', payload);
    const failed = await test.step('POST negative totalVolumes without correction', async () =>
      postBillingByScalesExpecting400(
        fx,
        payload,
        'totalVolumes-[TotalVolumes] must be within the range 0',
      ),
    );
    await expectNoListRowForIdentifier(fx, chain.identifier);

    assertWithCheck(
      TestRunSummary,
      'Negative totalVolumes without correction',
      'HTTP 400 totalVolumes-[TotalVolumes] must be within the range 0; no list row.',
      `As expected — HTTP ${failed.status}; ${failed.message}`,
      () => {
        expect(failed.status).toBe(400);
      },
    );
    await attachSummary(TestRunSummary, Responses, { identifier: chain.identifier });
  });

  test(`[${PHN_4048_KEY}] TC-BE-24: Reject PUT on a deleted billing-by-scales record`, async ({
    Request,
    GeneratePayload,
    Responses,
    Endpoints,
    Nomenclatures,
    TestRunSummary,
  }) => {
    test.setTimeout(10 * 60 * 1000);
    const fx = fxFrom({ Request, GeneratePayload, Responses, Endpoints, Nomenclatures });
    const chain = await test.step(
      'Precondition: create, GET ACTIVE, DELETE without lock',
      async () => {
        const created = await createBillingByScalesPrereqs(fx, { tag: 'BE24' });
        const createPayload = createHeader(created.identifier, 'INVBE24', [
          scaleCodeRow(created),
        ]);
        TestRunSummary.registerPayload('billingByScales', createPayload);
        const billingByScaleId = await postBillingByScales(fx, createPayload);
        const beforeDelete = await getBillingByScales(fx, billingByScaleId);
        expect(String(beforeDelete.status)).toBe('ACTIVE');
        expect(asNumber(tableRows(beforeDelete)[0]?.newMeterReading)).toBe(100);
        expect(asNumber(tableRows(beforeDelete)[0]?.totalVolumes)).toBe(100);
        await deleteBillingByScales(fx, billingByScaleId);
        return { ...created, billingByScaleId };
      },
    );

    const afterDelete = await test.step('GET deleted record', async () =>
      getBillingByScales(fx, chain.billingByScaleId),
    );
    await test.step('POST /locks/acquire for PUT', async () => {
      await acquireDataByScalesLock(fx, chain.billingByScaleId);
    });
    const editPayload = editTablePayload(chain.billingByScaleId, chain, {
      newMeterReading: 999,
      oldMeterReading: 0,
      difference: 999,
      totalVolumes: 999,
    });
    TestRunSummary.registerPayload('billingByScalesEdit', editPayload);
    const putRes = await test.step('PUT deleted billing-by-scales', async () =>
      putBillingByScales(fx, editPayload),
    );
    const putFailed = await expectHttp400Containing(
      {
        status: () => putRes.status,
        text: async () => putRes.rawText || JSON.stringify(putRes.body ?? {}),
      },
      "Can't edit deleted billing by scale",
    );
    const afterPut = await getBillingByScales(fx, chain.billingByScaleId);
    const row = tableRows(afterPut)[0] ?? {};

    assertWithCheck(
      TestRunSummary,
      'PUT on deleted record is rejected and readings stay at create values',
      'GET DELETED; lock 200; PUT 400 Can’t edit deleted billing by scale; GET still DELETED, newMeterReading 100, INVBE24.',
      `As expected — afterDelete=${String(afterDelete.status)} put=${putFailed.status} afterPut=${String(afterPut.status)} new=${String(row.newMeterReading)}.`,
      () => {
        expect(String(afterDelete.status)).toBe('DELETED');
        expect(putFailed.status).toBe(400);
        expect(String(afterPut.status)).toBe('DELETED');
        expect(tableRows(afterPut)).toHaveLength(1);
        expect(asNumber(row.newMeterReading)).toBe(100);
        expect(asNumber(row.totalVolumes)).toBe(100);
        expect(String(afterPut.invoiceNumber)).toBe('INVBE24');
      },
    );
    await attachSummary(TestRunSummary, Responses, {
      id: chain.billingByScaleId,
      identifier: chain.identifier,
    });
  });

  test(`[${PHN_4048_KEY}] TC-BE-25: Reject create when tariffScale is unknown`, async ({
    Request,
    GeneratePayload,
    Responses,
    Endpoints,
    Nomenclatures,
    TestRunSummary,
  }) => {
    test.setTimeout(10 * 60 * 1000);
    const fx = fxFrom({ Request, GeneratePayload, Responses, Endpoints, Nomenclatures });
    const chain = await test.step(
      'Precondition: create grid operator, user type, country, scale, POD, meter',
      async () => createBillingByScalesPrereqs(fx, { tag: 'BE25' }),
    );
    const payload = createHeader(chain.identifier, 'INVBE25', [
      tariffRow(chain, { tariffScale: 'NOPE' }),
    ]);
    TestRunSummary.registerPayload('billingByScales', payload);
    const failed = await test.step('POST unknown tariffScale NOPE', async () =>
      postBillingByScalesExpecting400(fx, payload, 'TariffScale'),
    );
    await expectNoListRowForIdentifier(fx, chain.identifier);

    assertWithCheck(
      TestRunSummary,
      'Unknown tariffScale is rejected',
      'HTTP 400; message contains TariffScale; no list row.',
      `As expected — HTTP ${failed.status}; ${failed.message}`,
      () => {
        expect(failed.status).toBe(400);
      },
    );
    await attachSummary(TestRunSummary, Responses, { identifier: chain.identifier });
  });
});
