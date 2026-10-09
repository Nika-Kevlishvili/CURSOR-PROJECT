/**
 * PDT-3063 — Invoice Communication Data from alternative recipient
 * (Manual invoice + Manual interim).
 *
 * Maps 1:1 to test_cases/Backend/PDT-3063_Invoice_communication_data_alternative_recipient.md
 *
 * Reference spec(s):
 * - tests/cursor/PDT-2960-proforma-liability-due-date.spec.ts
 * - tests/cursor/PDT-2872-minimal-interim-payment.spec.ts
 * - tests/cursor/pdt-2872-minimal-interim-payment.fixtures.ts
 * - jsons/payloads/create/billing/manualInvoice.ts
 */
import { test, expect } from './cursor-test.fixtures';
import { finalizeTestRunSummary, buildProductContractTabLinks } from './shared/manual-verification-links.fixtures';
import {
  addPdt3063SecondBillingGroupWithPod,
  buildManualInterimPayload,
  buildManualInvoicePayload,
  createPdt3063ProductContractWithAltRecipient,
  createPdt3063ServiceContractContext,
  expectHttp400WithMessage,
  getBillingRun,
  interimIcd,
  listManualInvoiceCommunicationData,
  miIcd,
  parseIcdListOk,
  pushBillingRun,
  putBillingGroupAlt,
  type Pdt3063Fx,
  type Pdt3063IcdItem,
} from './pdt-3063-invoice-communication-data.fixtures';

const JIRA_KEY = 'PDT-3063';
const JIRA_TITLE = 'Billing run - Manual invoice - Incorrect Invoice communication data';
const MSG_MI_INVALID =
  'manualInvoiceBasicDataParameters.invoiceCommunicationDataId-communications data is invalid;';
const MSG_MI_MANDATORY =
  'manualInvoiceBasicDataParameters.invoiceCommunicationDataId-[invoiceCommunicationDataId] invoice communication data id is mandatory;';
const MSG_INTERIM_INVALID =
  'interimAndAdvancePaymentParameters.invoiceCommunicationDataId-communications data is invalid;';
const MSG_LIST_CUSTOMER_NULL = 'customerDetailsId-[customerDetailsId] should not be null;';

const TIMEOUT_MS = 12 * 60 * 1000;

function idsOf(rows: Pdt3063IcdItem[]): number[] {
  return rows.map((r) => Number(r.id)).filter((n) => Number.isFinite(n));
}

function defaultSelectedIds(rows: Pdt3063IcdItem[]): number[] {
  return rows.filter((r) => r.defaultSelected === true).map((r) => Number(r.id));
}

async function attachPdt3063Summary(
  TestRunSummary: Parameters<typeof finalizeTestRunSummary>[0],
  Responses: Pdt3063Fx['Responses'],
  snapshot: Record<string, unknown>,
  extraKeys: string[] = [],
): Promise<void> {
  await test.step('Attach test run summary', async () => {
    const contractId = Number(snapshot.productContractId ?? snapshot.serviceContractId);
    finalizeTestRunSummary(TestRunSummary, Responses, {
      jiraKey: JIRA_KEY,
      relevantEntityKeys: ['customer', 'pod', 'product', 'productContract', 'serviceContract', 'billingRun', ...extraKeys],
      extraLinks: Number.isFinite(contractId) ? buildProductContractTabLinks(contractId) : undefined,
      snapshot,
    });
  });
}

test.describe(`[${JIRA_KEY}]: ${JIRA_TITLE}`, { tag: ['@billing', '@pdt-3063', '@dev'] }, () => {
  test(`[${JIRA_KEY}]: ${JIRA_TITLE} | TC-BE-1: List ICD limited to alt recipient, defaultSelected KOMM2`, async ({
    Request,
    GeneratePayload,
    Responses,
    Endpoints,
    TestRunSummary,
  }) => {
    test.setTimeout(TIMEOUT_MS);
    const fx = { Request, GeneratePayload, Responses, Endpoints };
    const chain = await test.step('Precondition: signed product contract with alt recipient KOMM2', async () =>
      createPdt3063ProductContractWithAltRecipient(fx),
    );
    TestRunSummary.registerPayload('productContract', chain.productContractPayload);

    const withGroup = await test.step('GET ICD list with billingGroupIds (alt recipient)', async () => {
      const res = await listManualInvoiceCommunicationData(fx, {
        customerDetailsId: chain.customerCDetailsId,
        contractOrderType: 'PRODUCT_CONTRACT',
        contractOrderId: chain.productContractId,
        billingGroupIds: [chain.billingGroupId],
      });
      return parseIcdListOk(res);
    });

    const withoutGroup = await test.step('GET ICD list without billingGroupIds (baseline)', async () => {
      const res = await listManualInvoiceCommunicationData(fx, {
        customerDetailsId: chain.customerCDetailsId,
        contractOrderType: 'PRODUCT_CONTRACT',
        contractOrderId: chain.productContractId,
      });
      return parseIcdListOk(res);
    });

    await test.step('Assert list is limited to alternative recipient KOMM2', async () => {
      expect(withGroup.length).toBeGreaterThan(0);
      for (const item of withGroup) {
        expect(item.customerId, 'list customerId must be alternative recipient').toBe(chain.customerRId);
        expect(item.customerId).not.toBe(chain.customerCId);
      }
      const komm2 = withGroup.find((r) => r.id === chain.komm2Id);
      expect(komm2, 'KOMM2 present').toBeTruthy();
      expect(komm2?.name).toBe('KOMM2');
      expect(komm2?.defaultSelected).toBe(true);
      expect(idsOf(withGroup)).not.toContain(chain.kommContractId);
      expect(idsOf(withoutGroup), 'baseline without groups still includes contract comm').toContain(
        chain.kommContractId,
      );
      TestRunSummary.recordCheck({
        check: 'TC-BE-1 list limited to alt recipient',
        expectedResult: 'Only R / KOMM2 with defaultSelected=true; no KOMM_CONTRACT. Baseline includes KOMM_CONTRACT.',
        actualResult: `As expected — withGroup ids=${idsOf(withGroup).join(',')}; baseline includes ${chain.kommContractId}.`,
        passed: true,
      });
    });

    await attachPdt3063Summary(TestRunSummary, Responses, {
      productContractId: chain.productContractId,
      billingGroupId: chain.billingGroupId,
      komm2Id: chain.komm2Id,
    });
  });

  test(`[${JIRA_KEY}] TC-BE-2: Create MANUAL_INVOICE persists KOMM2`, async ({
    Request,
    GeneratePayload,
    Responses,
    Endpoints,
    TestRunSummary,
  }) => {
    test.setTimeout(TIMEOUT_MS);
    const fx = { Request, GeneratePayload, Responses, Endpoints };
    const chain = await test.step('Precondition: signed product contract with alt recipient', async () =>
      createPdt3063ProductContractWithAltRecipient(fx),
    );
    const payload = await buildManualInvoicePayload(fx, {
      customerDetailsId: chain.customerCDetailsId,
      productContractId: chain.productContractId,
      billingGroupId: chain.billingGroupId,
      invoiceCommunicationDataId: chain.komm2Id,
    });
    TestRunSummary.registerPayload('billingRun', payload);

    const billingRunId = await test.step('POST /billing-run MANUAL_INVOICE with komm2Id', async () => {
      const res = await Request.post(Endpoints.billingRun, { data: payload });
      await expect(res).CheckResponse();
      return pushBillingRun(Responses, await res.json(), 'MANUAL_INVOICE');
    });

    await test.step('GET /billing-run/{id} stores KOMM2 on alternative recipient', async () => {
      const body = await getBillingRun(fx, billingRunId);
      const again = await getBillingRun(fx, billingRunId);
      const icd = miIcd(body);
      const billingType = String(
        body.billingType ??
          (body.commonParameters as Record<string, unknown> | undefined)?.billingType ??
          '',
      );
      expect(
        billingType === 'MANUAL_INVOICE' || Boolean(body.manualInvoiceBillingRunParameters),
        'GET billingType MANUAL_INVOICE or manualInvoiceBillingRunParameters present',
      ).toBe(true);
      expect(icd?.id).toBe(chain.komm2Id);
      expect(icd?.id).not.toBe(chain.kommContractId);
      expect(miIcd(again)?.id).toBe(chain.komm2Id);
      const getCustomerId = icd?.customerId ?? null;
      const getCustomerVersion = icd?.customerVersion ?? null;
      TestRunSummary.recordCheck({
        check: 'TC-BE-2 persist KOMM2',
        expectedResult:
          'POST 200; GET invoiceCommunicationData.id = komm2Id (not kommContractId). Nested customerId/customerVersion may be null on GET (3-arg CustomerCommunicationDataResponse constructor).',
        actualResult:
          getCustomerId == null
            ? `As expected — icd.id=${icd?.id} persisted. GET nested customerId=${getCustomerId}, customerVersion=${getCustomerVersion} (3-arg constructor; not treated as product defect).`
            : `As expected — icd.id=${icd?.id}, customerId=${getCustomerId}, customerVersion=${getCustomerVersion}.`,
        passed: icd?.id === chain.komm2Id,
      });
    });

    await attachPdt3063Summary(TestRunSummary, Responses, {
      productContractId: chain.productContractId,
      billingRunId,
      komm2Id: chain.komm2Id,
    });
  });

  test(`[${JIRA_KEY}] TC-BE-4: Manual interim list+create+GET same alt rule`, async ({
    Request,
    GeneratePayload,
    Responses,
    Endpoints,
    TestRunSummary,
  }) => {
    test.setTimeout(TIMEOUT_MS);
    const fx = { Request, GeneratePayload, Responses, Endpoints };
    const chain = await test.step('Precondition: alt recipient billing group', async () =>
      createPdt3063ProductContractWithAltRecipient(fx),
    );

    const list = await test.step('GET ICD list for interim (shared list API)', async () => {
      const res = await listManualInvoiceCommunicationData(fx, {
        customerDetailsId: chain.customerCDetailsId,
        contractOrderType: 'PRODUCT_CONTRACT',
        contractOrderId: chain.productContractId,
        billingGroupIds: [chain.billingGroupId],
      });
      return parseIcdListOk(res);
    });

    const payload = await buildManualInterimPayload(fx, {
      customerDetailsId: chain.customerCDetailsId,
      productContractId: chain.productContractId,
      billingGroupIds: [chain.billingGroupId],
      invoiceCommunicationDataId: chain.komm2Id,
    });
    TestRunSummary.registerPayload('billingRun', payload);

    const billingRunId = await test.step('POST MANUAL_INTERIM_AND_ADVANCE_PAYMENT', async () => {
      const res = await Request.post(Endpoints.billingRun, { data: payload });
      await expect(res).CheckResponse();
      return pushBillingRun(Responses, await res.json(), 'MANUAL_INTERIM_AND_ADVANCE_PAYMENT');
    });

    await test.step('Assert list + GET persist KOMM2', async () => {
      const komm2 = list.find((r) => r.id === chain.komm2Id);
      expect(komm2?.defaultSelected).toBe(true);
      expect(komm2?.customerId).toBe(chain.customerRId);
      expect(idsOf(list)).not.toContain(chain.kommContractId);
      const icd = interimIcd(await getBillingRun(fx, billingRunId));
      expect(icd?.id).toBe(chain.komm2Id);
      TestRunSummary.recordCheck({
        check: 'TC-BE-4 interim alt recipient',
        expectedResult:
          'List defaultSelected KOMM2 with list customerId = R; GET invoiceCommunicationData.id = komm2Id. Nested GET customerId may be null (3-arg constructor).',
        actualResult: `As expected — GET id=${icd?.id}, nested customerId=${icd?.customerId ?? null}.`,
        passed: icd?.id === chain.komm2Id,
      });
    });

    await attachPdt3063Summary(TestRunSummary, Responses, {
      productContractId: chain.productContractId,
      billingRunId,
    });
  });

  test(`[${JIRA_KEY}] TC-BE-5: Override to second comm of same alt (KOMM2B)`, async ({
    Request,
    GeneratePayload,
    Responses,
    Endpoints,
    TestRunSummary,
  }) => {
    test.setTimeout(TIMEOUT_MS);
    const fx = { Request, GeneratePayload, Responses, Endpoints };
    const chain = await test.step('Precondition: alt recipient with KOMM2 and KOMM2B', async () =>
      createPdt3063ProductContractWithAltRecipient(fx, { extraBillingCommOnAlt: true }),
    );

    const list = await parseIcdListOk(
      await listManualInvoiceCommunicationData(fx, {
        customerDetailsId: chain.customerCDetailsId,
        contractOrderType: 'PRODUCT_CONTRACT',
        contractOrderId: chain.productContractId,
        billingGroupIds: [chain.billingGroupId],
      }),
    );
    expect(idsOf(list)).toEqual(expect.arrayContaining([chain.komm2Id, chain.komm2bId]));
    expect(defaultSelectedIds(list)).toEqual([chain.komm2Id]);
    expect(idsOf(list)).not.toContain(chain.kommContractId);

    const payload = await buildManualInvoicePayload(fx, {
      customerDetailsId: chain.customerCDetailsId,
      productContractId: chain.productContractId,
      billingGroupId: chain.billingGroupId,
      invoiceCommunicationDataId: chain.komm2bId,
    });
    TestRunSummary.registerPayload('billingRun', payload);
    const res = await Request.post(Endpoints.billingRun, { data: payload });
    await expect(res).CheckResponse();
    const billingRunId = pushBillingRun(Responses, await res.json(), 'MANUAL_INVOICE');
    const icd = miIcd(await getBillingRun(fx, billingRunId));
    expect(icd?.id).toBe(chain.komm2bId);
    TestRunSummary.recordCheck({
      check: 'TC-BE-5 override KOMM2B',
      expectedResult:
        'List has KOMM2 defaultSelected; save KOMM2B persists as GET id. Nested GET customerId may be null (3-arg constructor).',
      actualResult: `As expected — stored ${icd?.id}, nested customerId=${icd?.customerId ?? null}.`,
      passed: icd?.id === chain.komm2bId,
    });

    await attachPdt3063Summary(TestRunSummary, Responses, {
      productContractId: chain.productContractId,
      billingRunId,
      komm2bId: chain.komm2bId,
    });
  });

  test(`[${JIRA_KEY}] TC-BE-6: No alt recipient → contract KOMM_CONTRACT`, async ({
    Request,
    GeneratePayload,
    Responses,
    Endpoints,
    TestRunSummary,
  }) => {
    test.setTimeout(TIMEOUT_MS);
    const fx = { Request, GeneratePayload, Responses, Endpoints };
    const chain = await test.step('Precondition: billing group without alternative recipient', async () =>
      createPdt3063ProductContractWithAltRecipient(fx, { attachAlt: false, createUnattachedAlt: true }),
    );
    const bg = await Request.get(`${Endpoints.billingGroup}/${chain.billingGroupId}`);
    await expect(bg).CheckResponse();
    const bgJson = (await bg.json()) as Record<string, unknown>;
    expect(bgJson.alternativeRecipientCustomerDetailId ?? null).toBeNull();

    const list = await parseIcdListOk(
      await listManualInvoiceCommunicationData(fx, {
        customerDetailsId: chain.customerCDetailsId,
        contractOrderType: 'PRODUCT_CONTRACT',
        contractOrderId: chain.productContractId,
        billingGroupIds: [chain.billingGroupId],
      }),
    );
    expect(idsOf(list)).toContain(chain.kommContractId);
    expect(idsOf(list)).not.toContain(chain.komm2Id);

    const payload = await buildManualInvoicePayload(fx, {
      customerDetailsId: chain.customerCDetailsId,
      productContractId: chain.productContractId,
      billingGroupId: chain.billingGroupId,
      invoiceCommunicationDataId: chain.kommContractId,
    });
    TestRunSummary.registerPayload('billingRun', payload);
    const createRes = await Request.post(Endpoints.billingRun, { data: payload });
    await expect(createRes).CheckResponse();
    const billingRunId = pushBillingRun(Responses, await createRes.json(), 'MANUAL_INVOICE');
    expect(miIcd(await getBillingRun(fx, billingRunId))?.id).toBe(chain.kommContractId);
    TestRunSummary.recordCheck({
      check: 'TC-BE-6 contract comm when no alt',
      expectedResult: 'List/save use KOMM_CONTRACT; KOMM2 absent.',
      actualResult: 'As expected — contract communication data stored.',
      passed: true,
    });
    await attachPdt3063Summary(TestRunSummary, Responses, {
      productContractId: chain.productContractId,
      billingRunId,
    });
  });

  test(`[${JIRA_KEY}] TC-BE-7: Switch group A→B re-derives list to B`, async ({
    Request,
    GeneratePayload,
    Responses,
    Endpoints,
    TestRunSummary,
  }) => {
    test.setTimeout(TIMEOUT_MS);
    const fx = { Request, GeneratePayload, Responses, Endpoints };
    const chain = await test.step('Precondition: two groups with alt recipients A and B', async () =>
      createPdt3063ProductContractWithAltRecipient(fx, { twoAltRecipients: true }),
    );
    const second = await addPdt3063SecondBillingGroupWithPod(fx, chain);
    await putBillingGroupAlt(fx, chain.billingGroupId, {
      contractId: chain.productContractId,
      alternativeRecipientCustomerDetailId: chain.customerADetailsId,
      billingCustomerCommunicationId: chain.komm2aId,
    });
    await putBillingGroupAlt(fx, second.billingGroupId2, {
      contractId: chain.productContractId,
      alternativeRecipientCustomerDetailId: chain.customerBDetailsId,
      billingCustomerCommunicationId: chain.komm2bRecipientId,
    });

    const listA = await parseIcdListOk(
      await listManualInvoiceCommunicationData(fx, {
        customerDetailsId: chain.customerCDetailsId,
        contractOrderType: 'PRODUCT_CONTRACT',
        contractOrderId: chain.productContractId,
        billingGroupIds: [chain.billingGroupId],
      }),
    );
    const listB = await parseIcdListOk(
      await listManualInvoiceCommunicationData(fx, {
        customerDetailsId: chain.customerCDetailsId,
        contractOrderType: 'PRODUCT_CONTRACT',
        contractOrderId: chain.productContractId,
        billingGroupIds: [second.billingGroupId2],
      }),
    );
    expect(idsOf(listA)).toContain(chain.komm2aId);
    expect(defaultSelectedIds(listA)).toEqual([chain.komm2aId]);
    expect(idsOf(listA)).not.toContain(chain.komm2bRecipientId);
    expect(idsOf(listA)).not.toContain(chain.kommContractId);
    expect(idsOf(listB)).toContain(chain.komm2bRecipientId);
    expect(defaultSelectedIds(listB)).toEqual([chain.komm2bRecipientId]);
    expect(idsOf(listB)).not.toContain(chain.komm2aId);
    expect(idsOf(listB)).not.toContain(chain.kommContractId);

    const payload = await buildManualInvoicePayload(fx, {
      customerDetailsId: chain.customerCDetailsId,
      productContractId: chain.productContractId,
      billingGroupId: second.billingGroupId2,
      invoiceCommunicationDataId: chain.komm2bRecipientId,
    });
    TestRunSummary.registerPayload('billingRun', payload);
    const createRes = await Request.post(Endpoints.billingRun, { data: payload });
    await expect(createRes).CheckResponse();
    const billingRunId = pushBillingRun(Responses, await createRes.json(), 'MANUAL_INVOICE');
    expect(miIcd(await getBillingRun(fx, billingRunId))?.id).toBe(chain.komm2bRecipientId);
    TestRunSummary.recordCheck({
      check: 'TC-BE-7 switch group A to B',
      expectedResult: 'List A = KOMM2_A; list B = KOMM2_B; save B persists KOMM2_B.',
      actualResult: 'As expected — group B derivation and persist.',
      passed: true,
    });
    await attachPdt3063Summary(TestRunSummary, Responses, {
      productContractId: chain.productContractId,
      billingRunId,
      billingGroupAId: chain.billingGroupId,
      billingGroupBId: second.billingGroupId2,
    });
  });

  test(`[${JIRA_KEY}] TC-BE-8: Alt group vs plain group restores contract rules`, async ({
    Request,
    GeneratePayload,
    Responses,
    Endpoints,
    TestRunSummary,
  }) => {
    test.setTimeout(TIMEOUT_MS);
    const fx = { Request, GeneratePayload, Responses, Endpoints };
    const chain = await test.step('Precondition: alt group + plain group', async () =>
      createPdt3063ProductContractWithAltRecipient(fx),
    );
    const second = await addPdt3063SecondBillingGroupWithPod(fx, chain);
    const plain = await Request.get(`${Endpoints.billingGroup}/${second.billingGroupId2}`);
    await expect(plain).CheckResponse();
    const plainJson = (await plain.json()) as Record<string, unknown>;
    expect(plainJson.alternativeRecipientCustomerDetailId ?? null).toBeNull();

    const listAlt = await parseIcdListOk(
      await listManualInvoiceCommunicationData(fx, {
        customerDetailsId: chain.customerCDetailsId,
        contractOrderType: 'PRODUCT_CONTRACT',
        contractOrderId: chain.productContractId,
        billingGroupIds: [chain.billingGroupId],
      }),
    );
    const listPlain = await parseIcdListOk(
      await listManualInvoiceCommunicationData(fx, {
        customerDetailsId: chain.customerCDetailsId,
        contractOrderType: 'PRODUCT_CONTRACT',
        contractOrderId: chain.productContractId,
        billingGroupIds: [second.billingGroupId2],
      }),
    );
    expect(idsOf(listAlt)).toContain(chain.komm2Id);
    expect(idsOf(listAlt)).not.toContain(chain.kommContractId);
    expect(idsOf(listPlain)).toContain(chain.kommContractId);
    expect(idsOf(listPlain)).not.toContain(chain.komm2Id);

    const payload = await buildManualInvoicePayload(fx, {
      customerDetailsId: chain.customerCDetailsId,
      productContractId: chain.productContractId,
      billingGroupId: second.billingGroupId2,
      invoiceCommunicationDataId: chain.kommContractId,
    });
    TestRunSummary.registerPayload('billingRun', payload);
    const createRes = await Request.post(Endpoints.billingRun, { data: payload });
    await expect(createRes).CheckResponse();
    const billingRunId = pushBillingRun(Responses, await createRes.json(), 'MANUAL_INVOICE');
    expect(miIcd(await getBillingRun(fx, billingRunId))?.id).toBe(chain.kommContractId);
    TestRunSummary.recordCheck({
      check: 'TC-BE-8 alt vs plain group',
      expectedResult: 'Alt list KOMM2; plain list KOMM_CONTRACT; save plain persists contract comm.',
      actualResult: 'As expected.',
      passed: true,
    });
    await attachPdt3063Summary(TestRunSummary, Responses, {
      productContractId: chain.productContractId,
      billingRunId,
    });
  });

  test(`[${JIRA_KEY}] TC-BE-9: Existing run ICD not rewritten after later alt on group (AC-1a)`, async ({
    Request,
    GeneratePayload,
    Responses,
    Endpoints,
    TestRunSummary,
  }) => {
    test.setTimeout(TIMEOUT_MS);
    const fx = { Request, GeneratePayload, Responses, Endpoints };
    const chain = await test.step('Precondition: no alt, then MANUAL_INVOICE with KOMM_CONTRACT', async () =>
      createPdt3063ProductContractWithAltRecipient(fx, { attachAlt: false, createUnattachedAlt: true }),
    );
    const payload = await buildManualInvoicePayload(fx, {
      customerDetailsId: chain.customerCDetailsId,
      productContractId: chain.productContractId,
      billingGroupId: chain.billingGroupId,
      invoiceCommunicationDataId: chain.kommContractId,
    });
    const createRes = await Request.post(Endpoints.billingRun, { data: payload });
    await expect(createRes).CheckResponse();
    const billingRunId = pushBillingRun(Responses, await createRes.json(), 'MANUAL_INVOICE');
    TestRunSummary.registerPayload('billingRun', payload);

    await putBillingGroupAlt(fx, chain.billingGroupId, {
      contractId: chain.productContractId,
      alternativeRecipientCustomerDetailId: chain.customerRDetailsId,
      billingCustomerCommunicationId: chain.komm2Id,
    });

    const stored = miIcd(await getBillingRun(fx, billingRunId));
    expect(stored?.id).toBe(chain.kommContractId);
    const newList = await parseIcdListOk(
      await listManualInvoiceCommunicationData(fx, {
        customerDetailsId: chain.customerCDetailsId,
        contractOrderType: 'PRODUCT_CONTRACT',
        contractOrderId: chain.productContractId,
        billingGroupIds: [chain.billingGroupId],
      }),
    );
    expect(idsOf(newList)).toContain(chain.komm2Id);
    expect(defaultSelectedIds(newList)).toContain(chain.komm2Id as number);
    TestRunSummary.recordCheck({
      check: 'TC-BE-9 no migration of existing run',
      expectedResult: 'Saved run keeps KOMM_CONTRACT; new list defaultSelected KOMM2.',
      actualResult: `As expected — stored ${stored?.id}; list default ${defaultSelectedIds(newList).join(',')}.`,
      passed: true,
    });
    await attachPdt3063Summary(TestRunSummary, Responses, {
      productContractId: chain.productContractId,
      billingRunId,
    });
  });

  test(`[${JIRA_KEY}] TC-BE-10: STANDARD_BILLING has no this ICD rule`, async ({
    Request,
    GeneratePayload,
    Responses,
    Endpoints,
    TestRunSummary,
  }) => {
    test.setTimeout(TIMEOUT_MS);
    const fx = { Request, GeneratePayload, Responses, Endpoints };
    const chain = await test.step('Precondition: alt recipient + energy data + STANDARD_BILLING payload', async () =>
      createPdt3063ProductContractWithAltRecipient(fx),
    );
    const profilePayload = await GeneratePayload.energyData.profile1Month();
    const profiles = await Request.post(Endpoints.profilesByMonth, { data: profilePayload });
    await expect(profiles).CheckResponse();
    const billingPayload = await GeneratePayload.billing.billingRun('CONTRACT', ['FOR_VOLUMES']);
    expect(
      (billingPayload as { manualInvoiceParameters?: unknown }).manualInvoiceParameters,
    ).toBeFalsy();
    TestRunSummary.registerPayload('billingRun', billingPayload);
    const createRes = await Request.post(Endpoints.billingRun, { data: billingPayload });
    await expect(createRes).CheckResponse();
    const billingRunId = pushBillingRun(Responses, await createRes.json(), 'STANDARD_BILLING');
    const body = await getBillingRun(fx, billingRunId);
    const billingType = String(
      body.billingType ?? (body.commonParameters as Record<string, unknown>)?.billingType ?? '',
    );
    expect(
      billingType === 'STANDARD_BILLING' || Boolean(body.standardBillingRunParameters),
      'STANDARD_BILLING type or standardBillingRunParameters',
    ).toBe(true);
    expect(body.standardBillingRunParameters).toBeTruthy();
    expect(body.manualInvoiceBillingRunParameters ?? null).toBeFalsy();
    expect(miIcd(body)?.id ?? null).not.toBe(chain.komm2Id);
    TestRunSummary.recordCheck({
      check: 'TC-BE-10 STANDARD_BILLING out of ICD rule',
      expectedResult: 'STANDARD_BILLING has standardBillingRunParameters; no MI ICD = komm2Id.',
      actualResult: `As expected — billingType=${billingType || '(nested)'}, no MI ICD ${chain.komm2Id}.`,
      passed: true,
    });
    await attachPdt3063Summary(TestRunSummary, Responses, {
      productContractId: chain.productContractId,
      billingRunId,
      billingType,
    });
  });

  test(`[${JIRA_KEY}] TC-BE-11: SERVICE_CONTRACT no billing group → not alt list`, async ({
    Request,
    GeneratePayload,
    Responses,
    Endpoints,
    TestRunSummary,
  }) => {
    test.setTimeout(TIMEOUT_MS);
    const fx = { Request, GeneratePayload, Responses, Endpoints };
    const ctx = await test.step('Precondition: signed service contract + unattached alt customer', async () =>
      createPdt3063ServiceContractContext(fx),
    );
    const list = await parseIcdListOk(
      await listManualInvoiceCommunicationData(fx, {
        customerDetailsId: ctx.customerCDetailsId,
        contractOrderType: 'SERVICE_CONTRACT',
        contractOrderId: ctx.serviceContractId,
      }),
    );
    expect(idsOf(list)).toContain(ctx.kommServiceId);
    expect(idsOf(list)).not.toContain(ctx.komm2Id);

    const payload = await buildManualInvoicePayload(fx, {
      customerDetailsId: ctx.customerCDetailsId,
      productContractId: ctx.serviceContractId,
      billingGroupId: null,
      invoiceCommunicationDataId: ctx.kommServiceId,
      contractOrderType: 'SERVICE_CONTRACT',
      contractOrderId: ctx.serviceContractId,
    });
    TestRunSummary.registerPayload('billingRun', payload);
    const createRes = await Request.post(Endpoints.billingRun, { data: payload });
    await expect(createRes).CheckResponse();
    const billingRunId = pushBillingRun(Responses, await createRes.json(), 'MANUAL_INVOICE');
    expect(miIcd(await getBillingRun(fx, billingRunId))?.id).toBe(ctx.kommServiceId);
    TestRunSummary.recordCheck({
      check: 'TC-BE-11 service contract without billing group',
      expectedResult: 'List/save use service customer comm, not unattached KOMM2.',
      actualResult: 'As expected.',
      passed: true,
    });
    await attachPdt3063Summary(TestRunSummary, Responses, {
      serviceContractId: ctx.serviceContractId,
      billingRunId,
    });
  });

  test(`[${JIRA_KEY}] TC-BE-12: Negative: reject contract comm while alt group selected`, async ({
    Request,
    GeneratePayload,
    Responses,
    Endpoints,
    TestRunSummary,
  }) => {
    test.setTimeout(TIMEOUT_MS);
    const fx = { Request, GeneratePayload, Responses, Endpoints };
    const chain = await test.step('Precondition: alt recipient group', async () =>
      createPdt3063ProductContractWithAltRecipient(fx),
    );
    const payload = await buildManualInvoicePayload(fx, {
      customerDetailsId: chain.customerCDetailsId,
      productContractId: chain.productContractId,
      billingGroupId: chain.billingGroupId,
      invoiceCommunicationDataId: chain.kommContractId,
    });
    TestRunSummary.registerPayload('billingRun', payload);
    const res = await Request.post(Endpoints.billingRun, { data: payload });
    await expectHttp400WithMessage(res, MSG_MI_INVALID);
    const list = await parseIcdListOk(
      await listManualInvoiceCommunicationData(fx, {
        customerDetailsId: chain.customerCDetailsId,
        contractOrderType: 'PRODUCT_CONTRACT',
        contractOrderId: chain.productContractId,
        billingGroupIds: [chain.billingGroupId],
      }),
    );
    expect(idsOf(list)).toContain(chain.komm2Id);
    TestRunSummary.recordCheck({
      check: 'TC-BE-12 reject contract comm',
      expectedResult: `HTTP 400 with ${MSG_MI_INVALID}`,
      actualResult: 'As expected — no billing run created.',
      passed: true,
    });
    await attachPdt3063Summary(TestRunSummary, Responses, {
      productContractId: chain.productContractId,
    });
  });

  test(`[${JIRA_KEY}] TC-BE-13: Negative: null ICD id mandatory message`, async ({
    Request,
    GeneratePayload,
    Responses,
    Endpoints,
    TestRunSummary,
  }) => {
    test.setTimeout(TIMEOUT_MS);
    const fx = { Request, GeneratePayload, Responses, Endpoints };
    const chain = await test.step('Precondition: alt recipient group', async () =>
      createPdt3063ProductContractWithAltRecipient(fx),
    );
    const payload = await buildManualInvoicePayload(fx, {
      customerDetailsId: chain.customerCDetailsId,
      productContractId: chain.productContractId,
      billingGroupId: chain.billingGroupId,
      invoiceCommunicationDataId: null,
      omitInvoiceCommunicationDataId: true,
    });
    TestRunSummary.registerPayload('billingRun', payload);
    const res = await Request.post(Endpoints.billingRun, { data: payload });
    await expectHttp400WithMessage(res, MSG_MI_MANDATORY);
    TestRunSummary.recordCheck({
      check: 'TC-BE-13 ICD mandatory',
      expectedResult: `HTTP 400 with ${MSG_MI_MANDATORY}`,
      actualResult: 'As expected.',
      passed: true,
    });
    await attachPdt3063Summary(TestRunSummary, Responses, {
      productContractId: chain.productContractId,
    });
  });

  test(`[${JIRA_KEY}] TC-BE-14: Negative: reject other alt recipient's comm`, async ({
    Request,
    GeneratePayload,
    Responses,
    Endpoints,
    TestRunSummary,
  }) => {
    test.setTimeout(TIMEOUT_MS);
    const fx = { Request, GeneratePayload, Responses, Endpoints };
    const chain = await test.step('Precondition: group attached to A; B unattached', async () =>
      createPdt3063ProductContractWithAltRecipient(fx, { twoAltRecipients: true }),
    );
    const payload = await buildManualInvoicePayload(fx, {
      customerDetailsId: chain.customerCDetailsId,
      productContractId: chain.productContractId,
      billingGroupId: chain.billingGroupId,
      invoiceCommunicationDataId: chain.komm2bRecipientId,
    });
    TestRunSummary.registerPayload('billingRun', payload);
    const res = await Request.post(Endpoints.billingRun, { data: payload });
    await expectHttp400WithMessage(res, MSG_MI_INVALID);
    const list = await parseIcdListOk(
      await listManualInvoiceCommunicationData(fx, {
        customerDetailsId: chain.customerCDetailsId,
        contractOrderType: 'PRODUCT_CONTRACT',
        contractOrderId: chain.productContractId,
        billingGroupIds: [chain.billingGroupId],
      }),
    );
    expect(idsOf(list)).toContain(chain.komm2aId);
    expect(idsOf(list)).not.toContain(chain.komm2bRecipientId);
    TestRunSummary.recordCheck({
      check: 'TC-BE-14 reject other alt comm',
      expectedResult: 'HTTP 400 invalid; list has A not B.',
      actualResult: 'As expected.',
      passed: true,
    });
    await attachPdt3063Summary(TestRunSummary, Responses, {
      productContractId: chain.productContractId,
    });
  });

  test(`[${JIRA_KEY}] TC-BE-15: Negative: list missing customerDetailsId`, async ({
    Request,
    GeneratePayload,
    Responses,
    Endpoints,
    TestRunSummary,
  }) => {
    test.setTimeout(TIMEOUT_MS);
    const fx = { Request, GeneratePayload, Responses, Endpoints };
    const chain = await test.step('Precondition: alt recipient group', async () =>
      createPdt3063ProductContractWithAltRecipient(fx),
    );
    TestRunSummary.registerPayload('productContract', chain.productContractPayload);
    const missing = await listManualInvoiceCommunicationData(fx, {
      omitCustomerDetailsId: true,
      contractOrderType: 'PRODUCT_CONTRACT',
      contractOrderId: chain.productContractId,
      billingGroupIds: [chain.billingGroupId],
    });
    await expectHttp400WithMessage(missing, MSG_LIST_CUSTOMER_NULL);
    const okList = await parseIcdListOk(
      await listManualInvoiceCommunicationData(fx, {
        customerDetailsId: chain.customerCDetailsId,
        contractOrderType: 'PRODUCT_CONTRACT',
        contractOrderId: chain.productContractId,
        billingGroupIds: [chain.billingGroupId],
      }),
    );
    expect(idsOf(okList)).toContain(chain.komm2Id);
    TestRunSummary.recordCheck({
      check: 'TC-BE-15 list customerDetailsId required',
      expectedResult: `HTTP 400 ${MSG_LIST_CUSTOMER_NULL}; control list 200 with KOMM2.`,
      actualResult: 'As expected.',
      passed: true,
    });
    await attachPdt3063Summary(TestRunSummary, Responses, {
      productContractId: chain.productContractId,
    });
  });

  test(`[${JIRA_KEY}] TC-BE-16: Negative: PUT update reject contract comm; GET still komm2Id`, async ({
    Request,
    GeneratePayload,
    Responses,
    Endpoints,
    TestRunSummary,
  }) => {
    test.setTimeout(TIMEOUT_MS);
    const fx = { Request, GeneratePayload, Responses, Endpoints };
    const chain = await test.step('Precondition: MANUAL_INVOICE saved with KOMM2', async () =>
      createPdt3063ProductContractWithAltRecipient(fx),
    );
    const createPayload = await buildManualInvoicePayload(fx, {
      customerDetailsId: chain.customerCDetailsId,
      productContractId: chain.productContractId,
      billingGroupId: chain.billingGroupId,
      invoiceCommunicationDataId: chain.komm2Id,
    });
    const createRes = await Request.post(Endpoints.billingRun, { data: createPayload });
    await expect(createRes).CheckResponse();
    const billingRunId = pushBillingRun(Responses, await createRes.json(), 'MANUAL_INVOICE');
    const updatePayload = await buildManualInvoicePayload(fx, {
      customerDetailsId: chain.customerCDetailsId,
      productContractId: chain.productContractId,
      billingGroupId: chain.billingGroupId,
      invoiceCommunicationDataId: chain.kommContractId,
    });
    TestRunSummary.registerPayload('billingRun', updatePayload);
    const putRes = await Request.put(`${Endpoints.billingRun}/${billingRunId}`, { data: updatePayload });
    await expectHttp400WithMessage(putRes, MSG_MI_INVALID);
    expect(miIcd(await getBillingRun(fx, billingRunId))?.id).toBe(chain.komm2Id);
    TestRunSummary.recordCheck({
      check: 'TC-BE-16 PUT rejects contract comm',
      expectedResult: 'HTTP 400 invalid; GET still komm2Id.',
      actualResult: 'As expected — stored value unchanged.',
      passed: true,
    });
    await attachPdt3063Summary(TestRunSummary, Responses, {
      productContractId: chain.productContractId,
      billingRunId,
    });
  });

  test(`[${JIRA_KEY}] TC-BE-17: Negative: interim reject contract comm`, async ({
    Request,
    GeneratePayload,
    Responses,
    Endpoints,
    TestRunSummary,
  }) => {
    test.setTimeout(TIMEOUT_MS);
    const fx = { Request, GeneratePayload, Responses, Endpoints };
    const chain = await test.step('Precondition: alt recipient group', async () =>
      createPdt3063ProductContractWithAltRecipient(fx),
    );
    const payload = await buildManualInterimPayload(fx, {
      customerDetailsId: chain.customerCDetailsId,
      productContractId: chain.productContractId,
      billingGroupIds: [chain.billingGroupId],
      invoiceCommunicationDataId: chain.kommContractId,
    });
    TestRunSummary.registerPayload('billingRun', payload);
    const res = await Request.post(Endpoints.billingRun, { data: payload });
    await expectHttp400WithMessage(res, MSG_INTERIM_INVALID);
    TestRunSummary.recordCheck({
      check: 'TC-BE-17 interim reject contract comm',
      expectedResult: `HTTP 400 with ${MSG_INTERIM_INVALID}`,
      actualResult: 'As expected.',
      passed: true,
    });
    await attachPdt3063Summary(TestRunSummary, Responses, {
      productContractId: chain.productContractId,
    });
  });

  test(`[${JIRA_KEY}] TC-BE-18: Two interim groups same alt+same comm → one defaultSelected`, async ({
    Request,
    GeneratePayload,
    Responses,
    Endpoints,
    TestRunSummary,
  }) => {
    test.setTimeout(TIMEOUT_MS);
    const fx = { Request, GeneratePayload, Responses, Endpoints };
    const chain = await test.step('Precondition: two groups same alt KOMM2', async () =>
      createPdt3063ProductContractWithAltRecipient(fx),
    );
    const second = await addPdt3063SecondBillingGroupWithPod(fx, chain);
    await putBillingGroupAlt(fx, second.billingGroupId2, {
      contractId: chain.productContractId,
      alternativeRecipientCustomerDetailId: chain.customerRDetailsId,
      billingCustomerCommunicationId: chain.komm2Id,
    });
    const list = await parseIcdListOk(
      await listManualInvoiceCommunicationData(fx, {
        customerDetailsId: chain.customerCDetailsId,
        contractOrderType: 'PRODUCT_CONTRACT',
        contractOrderId: chain.productContractId,
        billingGroupIds: [chain.billingGroupId, second.billingGroupId2],
      }),
    );
    for (const item of list) {
      expect(item.customerId).toBe(chain.customerRId);
    }
    expect(list.filter((r) => r.id === chain.komm2Id && r.defaultSelected === true)).toHaveLength(1);

    const payload = await buildManualInterimPayload(fx, {
      customerDetailsId: chain.customerCDetailsId,
      productContractId: chain.productContractId,
      billingGroupIds: [chain.billingGroupId, second.billingGroupId2],
      invoiceCommunicationDataId: chain.komm2Id,
    });
    TestRunSummary.registerPayload('billingRun', payload);
    const createRes = await Request.post(Endpoints.billingRun, { data: payload });
    await expect(createRes).CheckResponse();
    const billingRunId = pushBillingRun(Responses, await createRes.json(), 'MANUAL_INTERIM_AND_ADVANCE_PAYMENT');
    expect(interimIcd(await getBillingRun(fx, billingRunId))?.id).toBe(chain.komm2Id);
    TestRunSummary.recordCheck({
      check: 'TC-BE-18 same alt two groups',
      expectedResult: 'Exactly one defaultSelected KOMM2; save persists komm2Id.',
      actualResult: 'As expected.',
      passed: true,
    });
    await attachPdt3063Summary(TestRunSummary, Responses, {
      productContractId: chain.productContractId,
      billingRunId,
    });
  });

  test(`[${JIRA_KEY}] TC-BE-19: Two groups different alts → defaultSelected unset; save komm2aId`, async ({
    Request,
    GeneratePayload,
    Responses,
    Endpoints,
    TestRunSummary,
  }) => {
    test.setTimeout(TIMEOUT_MS);
    const fx = { Request, GeneratePayload, Responses, Endpoints };
    const chain = await test.step('Precondition: two groups different alt recipients', async () =>
      createPdt3063ProductContractWithAltRecipient(fx, { twoAltRecipients: true }),
    );
    const second = await addPdt3063SecondBillingGroupWithPod(fx, chain);
    await putBillingGroupAlt(fx, second.billingGroupId2, {
      contractId: chain.productContractId,
      alternativeRecipientCustomerDetailId: chain.customerBDetailsId,
      billingCustomerCommunicationId: chain.komm2bRecipientId,
    });
    const list = await parseIcdListOk(
      await listManualInvoiceCommunicationData(fx, {
        customerDetailsId: chain.customerCDetailsId,
        contractOrderType: 'PRODUCT_CONTRACT',
        contractOrderId: chain.productContractId,
        billingGroupIds: [chain.billingGroupId, second.billingGroupId2],
      }),
    );
    expect(idsOf(list)).toEqual(expect.arrayContaining([chain.komm2aId, chain.komm2bRecipientId]));
    expect(idsOf(list)).not.toContain(chain.kommContractId);
    for (const item of list) {
      expect(item.defaultSelected === true, 'defaultSelected must be unset when alts differ').toBeFalsy();
    }
    const payload = await buildManualInterimPayload(fx, {
      customerDetailsId: chain.customerCDetailsId,
      productContractId: chain.productContractId,
      billingGroupIds: [chain.billingGroupId, second.billingGroupId2],
      invoiceCommunicationDataId: chain.komm2aId,
    });
    TestRunSummary.registerPayload('billingRun', payload);
    const createRes = await Request.post(Endpoints.billingRun, { data: payload });
    await expect(createRes).CheckResponse();
    const billingRunId = pushBillingRun(Responses, await createRes.json(), 'MANUAL_INTERIM_AND_ADVANCE_PAYMENT');
    expect(interimIcd(await getBillingRun(fx, billingRunId))?.id).toBe(chain.komm2aId);
    TestRunSummary.recordCheck({
      check: 'TC-BE-19 different alts no default',
      expectedResult: 'Union of A+B without defaultSelected; save komm2aId.',
      actualResult: 'As expected.',
      passed: true,
    });
    await attachPdt3063Summary(TestRunSummary, Responses, {
      productContractId: chain.productContractId,
      billingRunId,
    });
  });
});
