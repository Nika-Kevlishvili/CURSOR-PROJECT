/**
 * DEV-DATA — FOR_VOLUMES (STANDARD_BILLING) with two government compensations; draft invoices only.
 *
 * Flow: billed customer → recipient customer (separate ACTIVE legal) → priceSettlement → terms →
 * pod_settlement → product → product_contract → POD activation → BBP (profile15minute + CET + MIN) →
 * two compensations (recipientId = second customer) → billing run FOR_VOLUMES →
 * start-billing only (no start-generating / start-accounting).
 *
 * Reference specs:
 * - tests/billing/forVolumes/forVolumes.spec.ts (REG-713 entity order; REG-962 15-min + MIN upload)
 * - tests/cursor/pdt-2937-invoice-detailed-data-same-pc-name.fixtures.ts (BBP + FOR_VOLUMES pattern)
 *
 * Swagger (dev2, refreshed 2026-09-28 from http://10.236.20.11:8092/v3/api-docs):
 * POST /customer CreateCustomerRequest.addressTransl (TransliteratedCustomerAddressRequest)
 * and communicationData[].addressTransliterated (TransliteratedCustomerCommAddressRequest).
 * Local transliteration strings are required at runtime when the source local address is filled
 * (country, region, municipality, populatedPlace, zipCode, district, residentialArea, street, number).
 * streetType / residentialAreaType enums: STREET|BOULEVARD, QUARTER|RESIDENTIAL_AREA.
 * Also: /price-components, /terms, /pod, /products, /product-contract,
 * /contract-pods/manual, /billing-by-profile, /government-compensations, /billing-run;
 * PATCH /billing-run/start-billing; GET /billing-run/draft-invoices.
 */

import { test, expect } from '../../fixtures/baseFixture';
import type { baseFixture } from '../../fixtures/baseFixture';
import { envVariables } from '../../fixtures/envCashed';
import { randomGens } from '../../utils/randomGens';
import { postPdt2915BillingByProfile } from './pdt-2915-invoice-correction-deleted-bbp.fixtures';
import {
  applyExpParityLocalAddress,
  postExpParityLegalCustomer,
  type ExpParityLocalAddressIds,
} from './shared/exp-parity-customer.fixtures';

/** Dev Phoenix UI base (no trailing slash) — same as PDT-3072 portal preview links. */
export const DEV_PORTAL_BASE = 'https://devapps.energo-pro.bg/app/phoenix1-dev';
export const DEV_DATA_KEY = 'DEV-DATA';

export type DevVolCompFx = Pick<
  baseFixture,
  'Request' | 'GeneratePayload' | 'Responses' | 'Endpoints'
>;

export type CompensationPayload = {
  number: string;
  date: string;
  volumes: number;
  price: number;
  reason: string;
  documentPeriod: string;
  documentAmount: number | string;
  customerId: string | number | null;
  podId: string | number | null;
  recipientId: number;
  documentCurrencyId: string | number;
};

export type DevVolCompPrechainResult = {
  customerId: number;
  /** Separate ACTIVE legal customer used as government compensation recipient (≠ customerId). */
  recipientCustomerId: number;
  podId: number;
  contractId: number;
  bbpId: number;
  periodFrom: string;
  periodTo: string;
  documentPeriod: string;
};

export type DevVolCompScenarioResult = DevVolCompPrechainResult & {
  compensationIds: number[];
  compensationNumbers: string[];
  billingRunId: number;
  draftInvoiceIds: number[];
  billingRunStatus: string;
};

function asEntityId(raw: unknown): number {
  if (typeof raw === 'number' && Number.isFinite(raw) && raw > 0) {
    return raw;
  }
  if (raw && typeof raw === 'object' && 'id' in raw) {
    const n = Number((raw as { id: unknown }).id);
    if (Number.isFinite(n) && n > 0) {
      return n;
    }
  }
  throw new Error(`Expected numeric entity id, got: ${JSON.stringify(raw)}`);
}

/** Uppercase Latin transliteration used when a local address id is set (Dev2 POST /customer). */
const LOCAL_ADDRESS_TRANSLATION = {
  country: 'STANDARD COUNTRY',
  region: 'STANDARD REGION',
  municipality: 'STANDARD MUNICIPALITY',
  populatedPlace: 'STANDARD POPULATED PLACE',
  zipCode: 'STANDARD ZIP CODE',
  district: 'STANDARD DISTRICT',
  residentialArea: 'STANDARD RESIDENTIAL AREA',
  residentialAreaType: 'QUARTER' as const,
  street: 'STANDARD STREET',
  streetType: 'STREET' as const,
};

function transliteratedLocalAddress(sourceLocal: Record<string, unknown> | undefined) {
  const local = sourceLocal ?? {};
  const transl: Record<string, string> = {
    country: LOCAL_ADDRESS_TRANSLATION.country,
    region: LOCAL_ADDRESS_TRANSLATION.region,
    municipality: LOCAL_ADDRESS_TRANSLATION.municipality,
    populatedPlace: LOCAL_ADDRESS_TRANSLATION.populatedPlace,
    zipCode: LOCAL_ADDRESS_TRANSLATION.zipCode,
    street: LOCAL_ADDRESS_TRANSLATION.street,
    streetType:
      typeof local.streetType === 'string' ? local.streetType : LOCAL_ADDRESS_TRANSLATION.streetType,
  };
  if (local.districtId != null) {
    transl.district = LOCAL_ADDRESS_TRANSLATION.district;
  }
  if (local.residentialAreaId != null) {
    transl.residentialArea = LOCAL_ADDRESS_TRANSLATION.residentialArea;
    transl.residentialAreaType =
      typeof local.residentialAreaType === 'string'
        ? local.residentialAreaType
        : LOCAL_ADDRESS_TRANSLATION.residentialAreaType;
  }
  return transl;
}

/** Dev2 requires addressTransl / addressTransliterated when the source address fields are filled. */
function legalCustomerWithAddressTransliteration(
  payload: Record<string, any>,
): Record<string, any> {
  const address = payload.address ?? {};
  payload.addressTransl = {
    foreign: false,
    localAddressData: transliteratedLocalAddress(address.localAddressData),
    number: address.number ?? '122',
  };
  if (Array.isArray(payload.communicationData)) {
    for (const entry of payload.communicationData) {
      if (!entry?.address || entry.addressTransliterated != null) continue;
      entry.addressTransliterated = {
        foreign: false,
        localAddressData: transliteratedLocalAddress(entry.address.localAddressData),
        number: entry.address.number ?? '122',
      };
    }
  }
  return payload;
}

export function monthStartFromPeriod(periodFrom: string): string {
  const day = periodFrom.includes('T') ? periodFrom.split('T')[0] : periodFrom.slice(0, 10);
  if (!/^\d{4}-\d{2}-\d{2}$/.test(day)) {
    return randomGens.generateMonthStartDate('yyyy-mm-dd');
  }
  return `${day.slice(0, 8)}01`;
}

/**
 * Clone compensation template twice with distinct `number` values.
 * `GeneratePayload.energyData.compensation()` mutates a shared singleton — never POST it twice as-is.
 * `recipientId` must be an ACTIVE customer id that is NOT the billed `customerId`.
 */
export function buildTwoDistinctCompensationPayloads(
  GeneratePayload: DevVolCompFx['GeneratePayload'],
  documentPeriod: string,
  recipientCustomerId: number,
): [CompensationPayload, CompensationPayload] {
  const template = GeneratePayload.energyData.compensation() as CompensationPayload;
  const customerId = Number(template.customerId);
  if (!recipientCustomerId || recipientCustomerId <= 0) {
    throw new Error('recipientCustomerId is required (create a second legal customer first)');
  }
  if (recipientCustomerId === customerId) {
    throw new Error(
      `recipientId must not equal customerId (${customerId}) — CompensationService rejects same customer as recipient`,
    );
  }

  const suffix = randomGens.generateRandomString(true, true, 8);
  const shared: CompensationPayload = {
    ...template,
    documentPeriod,
    documentAmount:
      typeof template.documentAmount === 'string'
        ? Number(template.documentAmount)
        : template.documentAmount,
    customerId,
    podId: Number(template.podId),
    recipientId: recipientCustomerId,
    documentCurrencyId: template.documentCurrencyId ?? envVariables.currency,
  };

  return [
    { ...shared, number: `DEV-VOL-COMP-A-${suffix}` },
    { ...shared, number: `DEV-VOL-COMP-B-${suffix}` },
  ];
}

export function buildDevInvoicePreviewLink(invoiceId: number): string {
  return `${DEV_PORTAL_BASE}/billing-run/invoices/preview/basic-parameters?id=${invoiceId}`;
}

export function buildDevBillingRunPreviewLink(billingRunId: number): string {
  return `${DEV_PORTAL_BASE}/billing-run/preview/basic-parameters?id=${billingRunId}`;
}

export function buildDevCompensationPreviewLink(compensationId: number): string {
  return `${DEV_PORTAL_BASE}/government-compensations/preview?id=${compensationId}`;
}

/**
 * REG-713-style contract chain + pdt-2937 BBP (15-minute + CET + MIN upload).
 * `governmentAltRecipient` adds a second price component the way Kalina's PC 10265 is stored:
 * doNotIncludeVatBase true and customerDetailId = the second customer's detail
 * (JSON name for alternativeRecipientCustomerDetailId). The first component stays on the
 * invoice so billing can emit the owner's invoice. Existing callers stay unchanged.
 */
export async function runDevVolCompContractAndBbpPrechain(
  ctx: DevVolCompFx,
  opts?: {
    governmentAltRecipient?: boolean;
    /** yyyy-mm-dd. Defaults to the current month start inside pod_activation(). */
    podActivationDate?: string;
    /** Excel rows for billing-by-profile. When set, also pass profilePeriodFrom/To for the API body. */
    profileDateRanges?: Array<{ startDate: string; endDate: string; amount: number }>;
    profilePeriodFrom?: string;
    profilePeriodTo?: string;
  },
): Promise<DevVolCompPrechainResult> {
  const { Request, GeneratePayload, Responses, Endpoints } = ctx;
  let localAddressIds: ExpParityLocalAddressIds | null = null;
  let volumeProfileId = Number(envVariables.profiles);

  await test.step('Precondition: billed customer', async () => {
    const customer = await Request.post(Endpoints.customer, {
      data: GeneratePayload.customers.customer_legal(),
    });
    await expect(customer).CheckResponse();
    Responses.customer.push(await customer.json());
  });

  await test.step('Precondition: recipient customer (compensation recipientId)', async () => {
    const recipient = await Request.post(Endpoints.customer, {
      data: GeneratePayload.customers.customer_legal(),
    });
    await expect(recipient).CheckResponse();
    Responses.customer.push(await recipient.json());
  });

  await test.step('Precondition: price component (priceSettlement, 15-min profile)', async () => {
    const payload = GeneratePayload.productAndServices.priceSettlement();
    payload.formulaRequest.expression = '100';
    let profileId = Number(envVariables.profiles);
    if (opts?.governmentAltRecipient) {
      const listed = await Request.get('profiles', {
        params: { statuses: 'ACTIVE', page: 0, size: 50 },
      });
      await expect(listed).CheckResponse();
      const page = (await listed.json()) as {
        content?: Array<{ id?: number; timeZone?: string }>;
      };
      const cet = (page.content ?? []).find(
        (row) => row.timeZone === 'CET' && Number(row.id) > 0,
      );
      profileId = Number(cet?.id ?? 0);
      expect(
        profileId,
        'ACTIVE CET profile — billing joins profile prices in the profile timezone',
      ).toBeGreaterThan(0);
      volumeProfileId = profileId;
    }
    payload.applicationModelRequest.settlementPeriodsRequest.profiles = [
      { profileId, percentage: 100 },
    ];
    // Owner line must stay in the invoice. A VAT-base-only product leaves invoice
    // details empty and generateInvoice throws "No value present".
    const ownerPrice = await Request.post(Endpoints.priceComponent, { data: payload });
    await expect(ownerPrice).CheckResponse();
    Responses.priceComponent.push(await ownerPrice.json());

    if (opts?.governmentAltRecipient) {
      const recipient = Responses.customer[Responses.customer.length - 1] as {
        lastCustomerDetailId?: number;
        customerDetailsId?: number;
      };
      const detailId = Number(recipient.lastCustomerDetailId ?? recipient.customerDetailsId ?? 0);
      expect(detailId, 'recipient customerDetailId for VAT-base alt recipient').toBeGreaterThan(0);
      const vatBasePayload = structuredClone(payload) as {
        doNotIncludeVatBase?: boolean;
        customerDetailId?: number;
      };
      vatBasePayload.doNotIncludeVatBase = true;
      vatBasePayload.customerDetailId = detailId;
      const vatBasePrice = await Request.post(Endpoints.priceComponent, { data: vatBasePayload });
      await expect(vatBasePrice).CheckResponse();
      Responses.priceComponent.push(await vatBasePrice.json());
    }
  });

  await test.step('Precondition: terms', async () => {
    const termPayload = GeneratePayload.productAndServices.term();
    if (opts?.governmentAltRecipient) {
      const paymentTerms = termPayload.invoicePaymentTerms as Array<{ value?: number; name?: string }>;
      if (paymentTerms[0]) {
        paymentTerms[0].value = 1;
        paymentTerms[0].name = '1 WORKING_DAYS ( )';
      }
    }
    const term = await Request.post(Endpoints.terms, {
      data: termPayload,
    });
    await expect(term).CheckResponse();
    Responses.terms.push(await term.json());
  });

  await test.step('Precondition: POD settlement', async () => {
    const podPayload = GeneratePayload.pointsOfDelivery.pod_settlement() as Record<string, unknown>;
    if (localAddressIds) {
      applyExpParityLocalAddress(podPayload, localAddressIds);
    }
    const pod = await Request.post(Endpoints.pod, {
      data: podPayload,
    });
    await expect(pod).CheckResponse();
    Responses.pod.push(await pod.json());
  });

  await test.step('Precondition: product', async () => {
    const productPayload = GeneratePayload.productAndServices.product() as {
      productGroupId?: number;
      templateIds?: Array<{ templateId: number; templateType: string }>;
    };
    if (opts?.governmentAltRecipient) {
      const listed = await Request.get('product-groups', {
        params: { statuses: 'ACTIVE', page: 0, size: 1 },
      });
      await expect(listed).CheckResponse();
      const page = (await listed.json()) as { content?: Array<{ id?: number }> };
      const groupId = Number(page.content?.[0]?.id ?? 0);
      expect(groupId, 'ACTIVE product group on this environment').toBeGreaterThan(0);
      productPayload.productGroupId = groupId;
      // Dev ACTIVE templates: INVOICE/EMAIL, INVOICE/DOCUMENT, PRODUCT/DOCUMENT.
      // envVariables template ids are from another environment and are rejected here.
      productPayload.templateIds = [
        { templateId: 1001, templateType: 'EMAIL_TEMPLATE' },
        { templateId: 1000, templateType: 'INVOICE_TEMPLATE' },
        { templateId: 1003, templateType: 'CONTRACT_TEMPLATE' },
      ];
    }
    const product = await Request.post(Endpoints.product, {
      data: productPayload,
    });
    await expect(product).CheckResponse();
    Responses.product.push(await product.json());
  });

  let contractId = 0;
  await test.step('Precondition: product contract', async () => {
    const contract = await Request.post(Endpoints.productContract, {
      data: await GeneratePayload.contractsAndOrders.product_contract(),
    });
    await expect(contract).CheckResponse();
    const body = await contract.json();
    Responses.productContract.push(body);
    contractId = asEntityId(body);
  });

  await test.step('Precondition: activate POD on contract', async () => {
    const podActivation = await Request.post('/contract-pods/manual', {
      data: await GeneratePayload.pointsOfDelivery.pod_activation(0, opts?.podActivationDate),
    });
    await expect(podActivation).CheckResponse();
  });

  let bbpId = 0;
  let periodFrom = '';
  let periodTo = '';
  await test.step('Precondition: billing-by-profile (15-minute + CET + MIN upload)', async () => {
    const payload = await GeneratePayload.energyData.profile15minute(0, opts?.profileDateRanges);
    payload.timeZone = 'CET';
    payload.profileId = opts?.governmentAltRecipient ? volumeProfileId : envVariables.profiles;
    if (opts?.profilePeriodFrom) {
      payload.periodFrom = opts.profilePeriodFrom;
    }
    if (opts?.profilePeriodTo) {
      payload.periodTo = opts.profilePeriodTo;
    }
    const bbp = await postPdt2915BillingByProfile(Request, payload, 'DEV-VOL-COMP BBP');
    await GeneratePayload.energyData.uploadDataByProfileFile(bbp.id, 'MIN');
    bbpId = bbp.id;
    periodFrom = String(payload.periodFrom);
    periodTo = String(payload.periodTo);
    Responses.dataByProfiles.push({
      id: bbp.id,
      periodFrom,
      periodTo,
      periodType: payload.periodType,
    });
  });

  const customerId = asEntityId(Responses.customer[0]);
  const recipientCustomerId = asEntityId(Responses.customer[1]);
  expect(recipientCustomerId, 'recipient customer must differ from billed customer').not.toBe(
    customerId,
  );
  const podId = asEntityId(Responses.pod[0]);
  const documentPeriod = monthStartFromPeriod(periodFrom);

  return {
    customerId,
    recipientCustomerId,
    podId,
    contractId,
    bbpId,
    periodFrom,
    periodTo,
    documentPeriod,
  };
}

export async function createTwoGovernmentCompensations(
  ctx: DevVolCompFx,
  documentPeriod: string,
  recipientCustomerId: number,
): Promise<{ ids: number[]; numbers: string[]; payloads: CompensationPayload[] }> {
  const { Request, GeneratePayload, Responses, Endpoints } = ctx;
  const payloads = buildTwoDistinctCompensationPayloads(
    GeneratePayload,
    documentPeriod,
    recipientCustomerId,
  );
  const ids: number[] = [];
  const numbers: string[] = [];

  for (let i = 0; i < payloads.length; i++) {
    const payload = payloads[i];
    expect(payload.recipientId, 'compensation recipientId must be second customer').toBe(
      recipientCustomerId,
    );
    expect(payload.recipientId, 'recipientId must not equal billed customerId').not.toBe(
      Number(payload.customerId),
    );
    const res = await Request.post(Endpoints.compensation, { data: payload });
    await expect(res).CheckResponse();
    const raw = await res.json();
    const id = asEntityId(raw);
    ids.push(id);
    numbers.push(payload.number);
    Responses.compensation.push({ id, number: payload.number, ...payload });
  }

  return { ids, numbers, payloads };
}

/**
 * Create FOR_VOLUMES billing run and run start-billing only
 * (`waitForInvoiceGeneration(true, false, …)` — skips start-generating and start-accounting).
 */
export async function createForVolumesDraftBillingRun(
  ctx: DevVolCompFx,
): Promise<{ billingRunId: number; draftInvoiceIds: number[]; billingRunStatus: string }> {
  const { Request, GeneratePayload, Responses, Endpoints } = ctx;

  const billingPayload = await GeneratePayload.billing.billingRun('CONTRACT', ['FOR_VOLUMES']);
  const billingRun = await Request.post(Endpoints.billingRun, { data: billingPayload });
  await expect(billingRun).CheckResponse();
  const billingRaw = await billingRun.json();
  const billingRunId = asEntityId(billingRaw);
  Responses.billingRun.push(
    typeof billingRaw === 'object' && billingRaw !== null ? billingRaw : { id: billingRunId },
  );

  await GeneratePayload.billing.waitForInvoiceGeneration(true, false, 1, 0);

  const statusRes = await Request.get(`${Endpoints.billingRun}/${billingRunId}`);
  await expect(statusRes).CheckResponse();
  const statusBody = (await statusRes.json()) as {
    commonParameters?: { status?: string };
  };
  const billingRunStatus = String(statusBody.commonParameters?.status ?? '');

  const draftRes = await Request.get(
    `billing-run/draft-invoices?id=${billingRunId}&page=0&size=25`,
  );
  await expect(draftRes).CheckResponse();
  const draftBody = (await draftRes.json()) as { content?: Array<{ id?: number }> };
  const draftInvoiceIds = (draftBody.content ?? [])
    .map((row) => Number(row.id))
    .filter((id) => Number.isFinite(id) && id > 0);

  if (Responses.invoice.length === 0) {
    for (const id of draftInvoiceIds) {
      Responses.invoice.push(id);
    }
  }

  return { billingRunId, draftInvoiceIds, billingRunStatus };
}

export function logDevVolCompCreatedEntities(result: DevVolCompScenarioResult): void {
  console.log('\n========== DEV-DATA: volume billing + two compensations (draft only) ==========');
  console.log(
    JSON.stringify(
      {
        customerId: result.customerId,
        recipientCustomerId: result.recipientCustomerId,
        podId: result.podId,
        contractId: result.contractId,
        bbpId: result.bbpId,
        documentPeriod: result.documentPeriod,
        compensationIds: result.compensationIds,
        compensationNumbers: result.compensationNumbers,
        billingRunId: result.billingRunId,
        billingRunStatus: result.billingRunStatus,
        draftInvoiceIds: result.draftInvoiceIds,
        portal: {
          billingRun: buildDevBillingRunPreviewLink(result.billingRunId),
          invoices: result.draftInvoiceIds.map(buildDevInvoicePreviewLink),
          compensations: result.compensationIds.map(buildDevCompensationPreviewLink),
          contract: `${DEV_PORTAL_BASE}/energy-product-contracts/preview/basic-parameters?id=${result.contractId}`,
        },
      },
      null,
      2,
    ),
  );
}
