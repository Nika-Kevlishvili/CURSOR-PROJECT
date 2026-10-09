/**
 * PDT-3379 helpers — reconnection invoice / automatic liability must use the
 * newest Product contract billing group for the RFD customer + POD.
 *
 * Dual-contract is not limited to re-sign: second SIGNED contract, same customer,
 * same POD, after OLD Contract POD is deactivated.
 *
 * RFD: reuse PDT-3179 LIST_OF_CUSTOMERS + allSelected false + this test's POD.
 * Do not use ALL_CUSTOMERS, allSelected true, Load PODs GET, or reminder /job.
 * Display-column tokens are computed by Phoenix; drive “latest liability” by
 * creating liabilities in order. Empty-column TCs omit unpaid liabilities.
 *
 * InvoiceResponse (dev swagger): contractBillingGroup / productContract are
 * ShortResponse — assert nestedId(...). No contractBillingGroupId / productContractId.
 * GET RFD has no taxCalculated — assert disconnectionRequestsStatus === FEE_CHARGED.
 *
 * PUT /billing-group/{id} cannot change contractId.
 *
 * Reference spec(s):
 * - tests/cursor/PDT-3179-reconnection-invoice-emails.spec.ts
 * - tests/cursor/pdt-3179-reconnection-invoice-emails.fixtures.ts
 * - tests/cursor/PDT-2861-rfd-pod-reconnection-fk.spec.ts
 * - tests/cursor/PDT-3409-reconnection-draft-disappearing-pods.spec.ts
 * - tests/cursor/PDT-2880-pod-disconnected-banner.spec.ts
 * - tests/receivableManagement/requestForDisconnection.spec.ts (manualInvoice billing run)
 */

import { expect } from '../../fixtures/baseFixture';
import { envVariables } from '../../fixtures/envCashed';
import { randomGens } from '../../utils/randomGens';
import {
  PDT_3179_MANUAL_LIABILITY_AMOUNT,
  PDT_3179_RFD_POST_TIMEOUT_MS,
  asNumber,
  calculateTax,
  collectCustomerInvoices,
  createExecutedReminder,
  customerIdentifier,
  entityId,
  findLiabilityByInvoiceId,
  getLiability,
  getRfd,
  invoicesOfType,
  listingContent,
  nestedId,
  resolveContractBillingGroupId,
  todayYmd,
  createExecutedRfd,
  type Pdt3179Fx,
} from './pdt-3179-reconnection-invoice-emails.fixtures';

export { createExecutedRfd };

export const PDT_3379_KEY = 'PDT-3379';
export const PDT_3379_TITLE =
  'Reconnection taxes should take the billing group of the newest contract';

/** Before every story POD activationDate (2024-01-xx). ProductContract template signingDate is 2025-01-01. */
const PDT_3379_CONTRACT_SIGNING_DATE = '2023-01-01';
const PDT_3379_EMPTY_LIABILITY_TOKEN = '0.00-BGN-0';
const PDT_3379_REMINDER_EXECUTED_WAIT_MS = 90_000;
const PDT_3379_REMINDER_POLL_MS = 2_000;
/** getRemindersForDPSRequestList SQL is slow; poll this family, not GET /list?statuses=EXECUTED. */
const PDT_3379_DPS_REMINDER_LIST_WAIT_MS = 180_000;
const PDT_3379_DPS_REMINDER_LIST_POLL_MS = 25_000;
const PDT_3379_DPS_REMINDER_LIST_GET_TIMEOUT_MS = 240_000;

export type Pdt3379Fx = Pdt3179Fx;

export type Pdt3379DualDates = {
  oldActivationDate: string;
  oldDeactivationDate: string | null;
  newActivationDate: string;
  newDeactivationDate?: string | null;
};

export type Pdt3379DualOpts = Pdt3379DualDates & {
  emails?: string[];
  independentSecondProduct?: boolean;
  skipOldLiability?: boolean;
  skipReminder?: boolean;
  skipNewContract?: boolean;
  skipOldActivation?: boolean;
  skipNewActivation?: boolean;
};

export type Pdt3379DualResult = {
  oldContractId: number;
  newContractId: number | null;
  oldBillingGroupId: number;
  newBillingGroupId: number | null;
  oldLiabilityId: number | null;
  oldContractIndex: number;
  newContractIndex: number | null;
  customerId: number;
  podId: number;
};

export type Pdt3379NewestExpected = {
  billingGroupId: number;
  productContractId: number;
};

export function pdt3379RelevantKeys(): string[] {
  return ['customer', 'productContract', 'pod', 'requestForDisconnection', 'invoice', 'customerLiability'];
}

export async function getProductContract(
  fx: Pdt3379Fx,
  contractIndex: number,
): Promise<Record<string, unknown>> {
  const contractId = entityId(fx.Responses.productContract[contractIndex]);
  const res = await fx.Request.get(`${fx.Endpoints.productContract}/${contractId}?version=1`);
  await expect(res).CheckResponse();
  return (await res.json()) as Record<string, unknown>;
}

export async function getContractBillingIds(
  fx: Pdt3379Fx,
  contractIndex: number,
): Promise<{
  contractId: number;
  billingGroupId: number;
  contractPodId: number;
  contractDetailId: number;
  body: Record<string, unknown>;
}> {
  const body = await getProductContract(fx, contractIndex);
  const contractId = entityId(fx.Responses.productContract[contractIndex]);
  const billingGroupId = resolveContractBillingGroupId(body);
  const pods = Array.isArray(body.contractPodsResponses)
    ? (body.contractPodsResponses as Record<string, unknown>[])
    : [];
  const contractPodId = asNumber(pods[0]?.id) || nestedId(pods[0]) || 0;
  const versions = Array.isArray(body.versions) ? (body.versions as Record<string, unknown>[]) : [];
  const contractDetailId =
    nestedId(versions[0]) ?? asNumber(versions[0]?.id) ?? asNumber(body.contractDetailId);
  return { contractId, billingGroupId, contractPodId, contractDetailId, body };
}

export async function activateContractPod(
  fx: Pdt3379Fx,
  opts: {
    podIndex?: number;
    contractIndex?: number;
    activationDate: string;
    deactivationDate?: string | null;
  },
): Promise<void> {
  const podIndex = opts.podIndex ?? 0;
  const contractIndex = opts.contractIndex ?? 0;
  const payload = await fx.GeneratePayload.pointsOfDelivery.pod_activation(
    podIndex,
    opts.activationDate,
    opts.deactivationDate ?? undefined,
    contractIndex,
  );
  if (opts.deactivationDate) {
    payload.deactivationDate = opts.deactivationDate;
    payload.deactivationPurposeId = envVariables.deactivation_reason;
  } else {
    payload.deactivationDate = null;
  }
  const res = await fx.Request.post('/contract-pods/manual', { data: payload });
  await expect(res).CheckResponse();
}

export async function createOverdueManualLiabilityOnContract(
  fx: Pdt3379Fx,
  contractIndex = 0,
  opts?: { interestRateId?: number },
): Promise<{ liabilityId: number; billingGroupId: number }> {
  const { Request, GeneratePayload, Responses, Endpoints } = fx;
  const ids = await getContractBillingIds(fx, contractIndex);
  expect(ids.billingGroupId, 'contract billingGroupId').toBeGreaterThan(0);

  const yesterday = randomGens.generateYesterdaysDate('dd-mm-yyyy');
  const payload = GeneratePayload.receivablesManagement.customer_liability();
  payload.billingGroupId = ids.billingGroupId;
  payload.initialAmount = PDT_3179_MANUAL_LIABILITY_AMOUNT;
  payload.dueDate = yesterday;
  payload.occurrenceDate = yesterday;
  if (opts?.interestRateId) {
    payload.applicableInterestRateId = opts.interestRateId;
  }

  const post = await Request.post(Endpoints.customerLiability, { data: payload });
  await expect(post).CheckResponse();
  const liabilityId = entityId(await post.json());
  Responses.customerLiability.push(liabilityId);

  const liab = await getLiability(fx, liabilityId);
  expect(asNumber(liab.currentAmount), 'manual liability currentAmount > 0').toBeGreaterThan(0);
  const gotBillingGroupId = nestedId(liab.billingGroupResponse);
  expect(gotBillingGroupId, 'GET liability billingGroupResponse.id').toBe(ids.billingGroupId);

  return { liabilityId, billingGroupId: ids.billingGroupId };
}

export async function createExtraProduct(fx: Pdt3379Fx): Promise<number> {
  const term = await fx.Request.post(fx.Endpoints.terms, { data: fx.GeneratePayload.productAndServices.term() });
  await expect(term).CheckResponse();
  fx.Responses.terms.push(await term.json());
  const termIndex = fx.Responses.terms.length - 1;
  const electricity = await fx.Request.post(fx.Endpoints.priceComponent, {
    data: fx.GeneratePayload.productAndServices.electricity(),
  });
  await expect(electricity).CheckResponse();
  fx.Responses.priceComponent.push(await electricity.json());
  const pcId = entityId(fx.Responses.priceComponent[fx.Responses.priceComponent.length - 1]);
  const productPayload = fx.GeneratePayload.productAndServices.product(termIndex) as Record<string, unknown>;
  productPayload.contractTypes = ['SUPPLY_ONLY'];
  productPayload.paymentGuarantees = ['NO'];
  productPayload.priceComponentIds = [pcId];
  productPayload.interimAdvancePayments = [];
  productPayload.interimAdvancePaymentGroups = [];
  const product = await fx.Request.post(fx.Endpoints.product, { data: productPayload });
  await expect(product).CheckResponse();
  fx.Responses.product.push(await product.json());
  return entityId(fx.Responses.product[fx.Responses.product.length - 1]);
}

export async function createExtraPod(fx: Pdt3379Fx): Promise<number> {
  const pod = await fx.Request.post(
    fx.Endpoints.pod,
    { data: fx.GeneratePayload.pointsOfDelivery.pod_settlement() },
  );
  await expect(pod).CheckResponse();
  const body = await pod.json();
  fx.Responses.pod.push(body);
  return entityId(body);
}

export async function createExtraCustomer(
  fx: Pdt3379Fx,
  emails?: string[],
): Promise<Record<string, unknown>> {
  const payload = fx.GeneratePayload.customers.customer_legal() as Record<string, unknown>;
  if (emails?.length) {
    const comm = (payload.communicationData as Array<Record<string, unknown>>)?.[0];
    if (comm) {
      comm.communicationContacts = [
        {
          sendSms: true,
          platformId: envVariables.platform,
          status: 'ACTIVE',
          contactType: 'MOBILE_NUMBER',
          contactValue: '555125531',
        },
        ...emails.map((contactValue) => ({
          sendSms: false,
          platformId: envVariables.platform,
          status: 'ACTIVE',
          contactType: 'EMAIL',
          contactValue,
        })),
      ];
    }
  }
  const res = await fx.Request.post(fx.Endpoints.customer, { data: payload });
  await expect(res).CheckResponse();
  const body = (await res.json()) as Record<string, unknown>;
  fx.Responses.customer.push(body);
  return body;
}

export async function createSignedContract(
  fx: Pdt3379Fx,
  opts: {
    customerIndex?: number;
    productIndex?: number;
    podIndex?: number;
    allPods?: boolean;
    versionStatus?: 'SIGNED' | 'DRAFT';
  } = {},
): Promise<number> {
  const customerIndex = opts.customerIndex ?? 0;
  const productIndex = opts.productIndex ?? 0;
  const payload = opts.allPods
    ? await fx.GeneratePayload.contractsAndOrders.product_contract(customerIndex, productIndex)
    : await fx.GeneratePayload.contractsAndOrders.product_contract(
        customerIndex,
        productIndex,
        opts.podIndex ?? 0,
      );
  (payload.productParameters as Record<string, unknown>).contractType = 'SUPPLY_ONLY';
  const basic = payload.basicParameters as Record<string, unknown>;
  // product_contract() GETs customer[customerIndex] for communication ids but hardcodes
  // basicParameters.customerId = customer[0] (ContractsAndOrdersPayloads.ts). Overwrite here.
  const expectedCustomerId = entityId(fx.Responses.customer[customerIndex]);
  basic.customerId = expectedCustomerId;
  if (opts.versionStatus === 'DRAFT') {
    basic.versionStatus = 'DRAFT';
  } else {
    basic.status = 'SIGNED';
    basic.subStatus = 'SIGNED_BY_BOTH_SIDES';
    basic.versionStatus = 'SIGNED';
    basic.signingDate = PDT_3379_CONTRACT_SIGNING_DATE;
    if (basic.statusModifyDate !== undefined) {
      basic.statusModifyDate = PDT_3379_CONTRACT_SIGNING_DATE;
    }
  }
  const contract = await fx.Request.post(fx.Endpoints.productContract, { data: payload });
  await expect(contract).CheckResponse();
  const contractBody = await contract.json();
  fx.Responses.productContract.push(contractBody);
  if (opts.versionStatus !== 'DRAFT') {
    const verify = await getProductContract(fx, fx.Responses.productContract.length - 1);
    const got = (verify.basicParameters ?? {}) as Record<string, unknown>;
    expect(
      String(got.status),
      `SIGNED contract must be SIGNED as of ${PDT_3379_CONTRACT_SIGNING_DATE}; got status=${got.status} subStatus=${got.subStatus} versionStatus=${got.versionStatus}`,
    ).toBe('SIGNED');
    // GET BasicParametersResponse.customerId is integer (dev swagger); nestedId also accepts ShortResponse.id.
    const gotCustomerId = nestedId(got.customerId);
    expect(
      gotCustomerId,
      `GET product-contract basicParameters.customerId must belong to customerIndex=${customerIndex} (id=${expectedCustomerId}); got ${gotCustomerId}. Silent attach to customer[0] is a test setup defect.`,
    ).toBe(expectedCustomerId);
  }
  return entityId(contractBody);
}

/**
 * Terms → product → customer → POD → Contract-OLD (no POST /contract-pods/manual).
 * Caller activates with explicit dates (story newest-contract criterion).
 * Copied from PDT-3179 createSupplyChain minus the month-start activation.
 */
export async function createPdt3379SupplyChain(
  fx: Pdt3379Fx,
  opts: { emails?: string[] } = {},
): Promise<{ customerId: number; podId: number; contractId: number }> {
  const { Request, GeneratePayload, Responses, Endpoints } = fx;

  const term = await Request.post(Endpoints.terms, { data: GeneratePayload.productAndServices.term() });
  await expect(term).CheckResponse();
  Responses.terms.push(await term.json());

  const electricityPayload = GeneratePayload.productAndServices.electricity();
  const electricity = await Request.post(Endpoints.priceComponent, { data: electricityPayload });
  await expect(electricity).CheckResponse();
  Responses.priceComponent.push(await electricity.json());

  const productPayload = GeneratePayload.productAndServices.product() as Record<string, unknown>;
  productPayload.contractTypes = ['SUPPLY_ONLY'];
  productPayload.paymentGuarantees = ['NO'];
  productPayload.priceComponentIds = [entityId(Responses.priceComponent[0])];
  productPayload.interimAdvancePayments = [];
  productPayload.interimAdvancePaymentGroups = [];
  const product = await Request.post(Endpoints.product, { data: productPayload });
  await expect(product).CheckResponse();
  Responses.product.push(await product.json());

  const customerPayload = GeneratePayload.customers.customer_legal() as Record<string, unknown>;
  if (opts.emails?.length) {
    const comm = (customerPayload.communicationData as Array<Record<string, unknown>>)?.[0];
    if (comm) {
      comm.communicationContacts = [
        {
          sendSms: true,
          platformId: envVariables.platform,
          status: 'ACTIVE',
          contactType: 'MOBILE_NUMBER',
          contactValue: '555125531',
        },
        ...opts.emails.map((contactValue) => ({
          sendSms: false,
          platformId: envVariables.platform,
          status: 'ACTIVE',
          contactType: 'EMAIL',
          contactValue,
        })),
      ];
    }
  }
  const customer = await Request.post(Endpoints.customer, { data: customerPayload });
  await expect(customer).CheckResponse();
  const customerBody = await customer.json();
  Responses.customer.push(customerBody);

  const pod = await Request.post(Endpoints.pod, { data: GeneratePayload.pointsOfDelivery.pod_settlement() });
  await expect(pod).CheckResponse();
  const podBody = await pod.json();
  Responses.pod.push(podBody);

  const contractId = await createSignedContract(fx, { customerIndex: 0, productIndex: 0, podIndex: 0 });

  return {
    customerId: entityId(customerBody),
    podId: entityId(podBody),
    contractId,
  };
}

export async function setupPdt3379DualContract(
  fx: Pdt3379Fx,
  opts: Pdt3379DualOpts,
): Promise<Pdt3379DualResult> {
  await createPdt3379SupplyChain(fx, { emails: opts.emails });
  const oldContractIndex = 0;
  const oldIds = await getContractBillingIds(fx, oldContractIndex);

  if (!opts.skipOldActivation) {
    await activateContractPod(fx, {
      contractIndex: oldContractIndex,
      activationDate: opts.oldActivationDate,
      deactivationDate: null,
    });
  }

  let oldLiabilityId: number | null = null;
  if (!opts.skipOldLiability) {
    const liab = await createOverdueManualLiabilityOnContract(fx, oldContractIndex);
    oldLiabilityId = liab.liabilityId;
  }

  if (opts.oldDeactivationDate) {
    await activateContractPod(fx, {
      contractIndex: oldContractIndex,
      activationDate: opts.oldActivationDate,
      deactivationDate: opts.oldDeactivationDate,
    });
  }

  let newContractIndex: number | null = null;
  let newContractId: number | null = null;
  let newBillingGroupId: number | null = null;

  if (!opts.skipNewContract) {
    if (opts.independentSecondProduct) {
      await createExtraProduct(fx);
    }
    const productIndex = opts.independentSecondProduct ? 1 : 0;
    await createSignedContract(fx, {
      customerIndex: 0,
      productIndex,
      podIndex: 0,
      versionStatus: 'SIGNED',
    });
    newContractIndex = fx.Responses.productContract.length - 1;
    const newIds = await getContractBillingIds(fx, newContractIndex);
    newContractId = newIds.contractId;
    newBillingGroupId = newIds.billingGroupId;
    expect(newBillingGroupId, 'NEW billing group must differ from OLD').not.toBe(oldIds.billingGroupId);

    if (!opts.skipNewActivation) {
      await activateContractPod(fx, {
        contractIndex: newContractIndex,
        podIndex: 0,
        activationDate: opts.newActivationDate,
        deactivationDate: opts.newDeactivationDate ?? null,
      });
    }
  }

  if (!opts.skipReminder) {
    await createExecutedReminder(fx);
  }

  return {
    oldContractId: oldIds.contractId,
    newContractId,
    oldBillingGroupId: oldIds.billingGroupId,
    newBillingGroupId,
    oldLiabilityId,
    oldContractIndex,
    newContractIndex,
    customerId: entityId(fx.Responses.customer[0]),
    podId: entityId(fx.Responses.pod[0]),
  };
}

/**
 * TC-BE-3: Contract-NEW created first (earlier create date, later closed period).
 * Responses.productContract[0] = NEW, [1] = OLD.
 * Phoenix /contract-pods/manual forbids overlapping periods and "deactivation is not set"
 * when a later currently-active period is posted after an earlier open period.
 */
export async function setupPdt3379NewCreatedFirst(
  fx: Pdt3379Fx,
  opts: Pdt3379DualDates & { emails?: string[] },
): Promise<Pdt3379DualResult> {
  await createPdt3379SupplyChain(fx, { emails: opts.emails });
  const newIds = await getContractBillingIds(fx, 0);

  await createSignedContract(fx, { customerIndex: 0, productIndex: 0, podIndex: 0 });
  const oldContractIndex = 1;
  await activateContractPod(fx, {
    contractIndex: oldContractIndex,
    activationDate: opts.oldActivationDate,
    deactivationDate: opts.oldDeactivationDate,
  });
  const oldLiab = await createOverdueManualLiabilityOnContract(fx, oldContractIndex);

  expect(opts.newDeactivationDate, 'NEW period must be closed (no overlap / no open deactivation)').toBeTruthy();
  await activateContractPod(fx, {
    contractIndex: 0,
    activationDate: opts.newActivationDate,
    deactivationDate: opts.newDeactivationDate,
  });
  const oldIds = await getContractBillingIds(fx, oldContractIndex);
  expect(newIds.billingGroupId).not.toBe(oldIds.billingGroupId);

  await createExecutedReminder(fx);

  return {
    oldContractId: oldIds.contractId,
    newContractId: newIds.contractId,
    oldBillingGroupId: oldIds.billingGroupId,
    newBillingGroupId: newIds.billingGroupId,
    oldLiabilityId: oldLiab.liabilityId,
    oldContractIndex,
    newContractIndex: 0,
    customerId: entityId(fx.Responses.customer[0]),
    podId: entityId(fx.Responses.pod[0]),
  };
}

export async function fetchPdt3379PodRow(
  fx: Pdt3379Fx,
  podIndex = 0,
  contractIndex = 0,
): Promise<Record<string, unknown>> {
  const pod = fx.Responses.pod[podIndex] as Record<string, unknown>;
  const customer = fx.Responses.customer[0] as Record<string, unknown>;
  const podId = entityId(pod);
  const customerId = entityId(customer);
  const identifier = String(customer.identifier ?? '');

  const getRes = await fx.Request.get(`${fx.Endpoints.pod}/${podId}?version=1`);
  await expect(getRes).CheckResponse();
  const body = (await getRes.json()) as Record<string, unknown>;
  const contractBody = await getProductContract(fx, contractIndex);
  const contractPods = Array.isArray(contractBody.contractPodsResponses)
    ? (contractBody.contractPodsResponses as Record<string, unknown>[])
    : [];
  const matchingPod = contractPods.find((row) => nestedId(row.podId) === podId) ?? contractPods[0];
  const versions = Array.isArray(body.versions) ? (body.versions as Record<string, unknown>[]) : [];
  const podDetailId =
    nestedId(pod.podDetailId) ??
    nestedId(body.podDetailId) ??
    nestedId(body.lastPodDetailId) ??
    nestedId(versions[0]?.podDetailId) ??
    nestedId(matchingPod?.podDetailId) ??
    nestedId(matchingPod?.pointOfDeliveryDetailId);
  expect(podDetailId, 'podDetailId for RFD pods[]').toBeGreaterThan(0);

  const bp = (contractBody.basicParameters ?? {}) as Record<string, unknown>;
  const contractId = entityId(fx.Responses.productContract[contractIndex]);
  const contractNumber = String(bp.contractNumber ?? contractId);
  const bg = Array.isArray(contractBody.billingGroups)
    ? (contractBody.billingGroups[0] as Record<string, unknown>)
    : undefined;
  const billingGroupLabel = String(bg?.groupNumber ?? bg?.number ?? nestedId(bg) ?? '');
  const billingGroupId = resolveContractBillingGroupId(contractBody);

  let liabilityToken = PDT_3379_EMPTY_LIABILITY_TOKEN;
  let liabilityAmount = 0;
  const liabilityIds = fx.Responses.customerLiability ?? [];
  if (liabilityIds.length > 0) {
    let chosen: Record<string, unknown> | null = null;
    let fallback: Record<string, unknown> | null = null;
    for (let i = liabilityIds.length - 1; i >= 0; i--) {
      const liab = await getLiability(fx, entityId(liabilityIds[i]));
      if (!fallback) fallback = liab;
      if (nestedId(liab.billingGroupResponse) === billingGroupId) {
        chosen = liab;
        break;
      }
    }
    chosen = chosen ?? fallback;
    const liabilityNumber = String(chosen?.number ?? '');
    if (liabilityNumber.length > 0) {
      liabilityAmount = asNumber(chosen?.currentAmount) || PDT_3179_MANUAL_LIABILITY_AMOUNT;
      const currencyName = String(
        (chosen?.currencyResponse as { name?: unknown } | undefined)?.name ?? 'BGN',
      ).replace(/-/g, ' ');
      liabilityToken = `${liabilityAmount}-${currencyName}-${liabilityNumber}`;
    }
  }

  return {
    podId,
    customerId,
    podIdentifier: String(body.identifier ?? pod.identifier ?? ''),
    isChecked: true,
    isHighestConsumption: false,
    existingCustomerReceivables: true,
    gridOperatorId: nestedId(body.gridOperatorId) ?? envVariables.grid_operator,
    podDetailId,
    customerNumber: identifier,
    customers: identifier,
    contracts: contractNumber,
    billingGroups: billingGroupLabel || contractNumber,
    liabilitiesInBillingGroup: liabilityToken,
    liabilitiesInPod: liabilityToken,
    liabilityAmountCustomer: liabilityAmount,
  };
}

export async function createPdt3379Rfd(
  fx: Pdt3379Fx,
  opts: {
    status?: 'DRAFT' | 'EXECUTED';
    podIndexes?: number[];
    contractIndex?: number;
    podContractIndexes?: number[];
  } = {},
): Promise<number> {
  const { Request, GeneratePayload, Responses, Endpoints } = fx;
  const identifier = customerIdentifier(Responses);
  expect(identifier.length, 'RFD listOfCustomer requires this test customer identifier').toBeGreaterThan(0);
  const podIndexes = opts.podIndexes ?? [0];
  const contractIndex = opts.contractIndex ?? 0;
  const pods = [];
  for (let i = 0; i < podIndexes.length; i++) {
    const rowContractIndex = opts.podContractIndexes?.[i] ?? contractIndex;
    pods.push(await fetchPdt3379PodRow(fx, podIndexes[i], rowContractIndex));
  }
  const payload = GeneratePayload.receivablesManagement.requestForDisconnection();
  payload.reminderForDisconnectionId = await resolvePdt3379ReminderIdForRfd(fx);
  payload.gridOpRequestRegDate = todayYmd();
  payload.disconnectionRequestsStatus = opts.status ?? 'EXECUTED';
  payload.supplierType = 'CURRENT';
  payload.conditionType = 'LIST_OF_CUSTOMERS';
  payload.listOfCustomer = identifier;
  payload.allSelected = false;
  payload.pods = pods;
  payload.podWithHighestConsumption = false;
  payload.templateIds = [];
  const res = await Request.post(Endpoints.requestForDisconnection, {
    data: payload,
    timeout: PDT_3179_RFD_POST_TIMEOUT_MS,
  });
  await expect(res).CheckResponse();
  const requestId = entityId(await res.json());
  Responses.requestForDisconnection.push(requestId);
  return requestId;
}

export async function assertReconnectionUsesNewest(
  fx: Pdt3379Fx,
  expected: Pdt3379NewestExpected,
): Promise<{
  invoice: Record<string, unknown>;
  liability: Record<string, unknown> | null;
  billingGroupId: number | undefined;
  productContractId: number | undefined;
}> {
  const invoices = await collectCustomerInvoices(fx);
  const reconnection = invoicesOfType(invoices, 'RECONNECTION').sort(
    (a, b) => entityId(a) - entityId(b),
  );
  expect(reconnection.length, 'at least one RECONNECTION invoice').toBeGreaterThanOrEqual(1);
  const invoice = reconnection[reconnection.length - 1];
  const billingGroupId = nestedId(invoice.contractBillingGroup);
  const productContractId = nestedId(invoice.productContract);
  expect(billingGroupId, 'invoice.contractBillingGroup.id').toBe(expected.billingGroupId);
  expect(productContractId, 'invoice.productContract.id').toBe(expected.productContractId);
  expect(String(invoice.invoiceType)).toBe('RECONNECTION');
  expect(String(invoice.invoiceStatus)).toBe('REAL');

  const recLiab = await findLiabilityByInvoiceId(fx, entityId(invoice));
  if (recLiab) {
    expect(nestedId(recLiab.billingGroupResponse), 'reconnection liability billingGroupResponse.id').toBe(
      expected.billingGroupId,
    );
  }
  return { invoice, liability: recLiab, billingGroupId, productContractId };
}

export async function chargeFeeExpectNewest(
  fx: Pdt3379Fx,
  requestId: number,
  expected: Pdt3379NewestExpected,
): Promise<{
  rfd: Record<string, unknown>;
  invoice: Record<string, unknown>;
  liability: Record<string, unknown> | null;
}> {
  const calcRes = await calculateTax(fx, requestId);
  await expect(calcRes).CheckResponse();
  const rfd = await getRfd(fx, requestId);
  expect(String(rfd.disconnectionRequestsStatus)).toBe('FEE_CHARGED');
  const asserted = await assertReconnectionUsesNewest(fx, expected);
  return { rfd, invoice: asserted.invoice, liability: asserted.liability };
}

export async function getBillingGroup(
  fx: Pdt3379Fx,
  billingGroupId: number,
): Promise<Record<string, unknown>> {
  const res = await fx.Request.get(`${fx.Endpoints.billingGroup}/${billingGroupId}`);
  await expect(res).CheckResponse();
  return (await res.json()) as Record<string, unknown>;
}

export async function putBillingGroup(
  fx: Pdt3379Fx,
  billingGroupId: number,
  overrides: Record<string, unknown>,
): Promise<void> {
  const current = await getBillingGroup(fx, billingGroupId);
  const contractIdFromGet =
    nestedId(current.contractId) ?? asNumber(current.contractId);
  const payload = {
    groupNumber: current.groupNumber,
    sendingInvoice: current.sendingInvoice,
    separateInvoiceForEachPod: Boolean(current.separateInvoiceForEachPod),
    directDebit: Boolean(current.directDebit),
    bankId: nestedId(current.bankId) ?? current.bankId ?? null,
    iban: current.iban ?? null,
    alternativeRecipientCustomerDetailId:
      nestedId(current.alternativeRecipientCustomerDetailId) ??
      current.alternativeRecipientCustomerDetailId ??
      null,
    billingCustomerCommunicationId:
      nestedId(current.billingCustomerCommunicationId) ??
      current.billingCustomerCommunicationId ??
      null,
    ...overrides,
    // API rejects "You can not change contract!;" — always send GET contractId.
    contractId: contractIdFromGet,
  };
  const res = await fx.Request.put(`${fx.Endpoints.billingGroup}/${billingGroupId}`, { data: payload });
  await expect(res).CheckResponse();
}

export async function createOverdueInvoiceOnContract(
  fx: Pdt3379Fx,
  contractIndex = 0,
): Promise<{ invoiceId: number; liabilityId: number }> {
  const { Request, GeneratePayload, Responses, Endpoints } = fx;
  const payload = (await GeneratePayload.billing.manualInvoice()) as Record<string, unknown>;
  if (contractIndex !== 0) {
    const contractBody = await getProductContract(fx, contractIndex);
    const mip = payload.manualInvoiceParameters as Record<string, unknown>;
    const basic = (mip.manualInvoiceBasicDataParameters ?? {}) as Record<string, unknown>;
    basic.billingGroupId = resolveContractBillingGroupId(contractBody);
    basic.contractOrderId = nestedId(
      (contractBody.basicParameters as Record<string, unknown> | undefined)?.id,
    ) ?? entityId(Responses.productContract[contractIndex]);
    basic.contractOrderType = 'PRODUCT_CONTRACT';
    mip.manualInvoiceBasicDataParameters = basic;
    payload.manualInvoiceParameters = mip;
  }
  const res = await Request.post(Endpoints.billingRun, { data: payload });
  await expect(res).CheckResponse();
  const billingRunId = entityId(await res.json());
  Responses.billingRun.push(billingRunId);
  await GeneratePayload.billing.waitForInvoiceGeneration(true, true, 1, Responses.billingRun.length - 1);

  const invoices = await collectCustomerInvoices(fx);
  expect(invoices.length, 'manual invoice created').toBeGreaterThan(0);
  const invoice = invoices[invoices.length - 1];
  const invoiceId = entityId(invoice);
  Responses.invoice.push(invoiceId);
  const liability = await findLiabilityByInvoiceId(fx, invoiceId);
  expect(liability, 'invoice-sourced customer liability').toBeTruthy();
  const liabilityId = entityId(liability);
  if (!Responses.customerLiability.includes(liabilityId)) {
    Responses.customerLiability.push(liabilityId);
  }
  return { invoiceId, liabilityId };
}

export async function createDailyInterestAndLpf(
  fx: Pdt3379Fx,
  contractIndex = 0,
): Promise<{ lpfId: number; lpfLiabilityId: number }> {
  const interestRes = await fx.Request.post(fx.Endpoints.interestRate, {
    data: fx.GeneratePayload.receivablesManagement.dailyInterestRate(),
  });
  await expect(interestRes).CheckResponse();
  const interestId = entityId(await interestRes.json());
  fx.Responses.interestRate.push(interestId);
  // Unpaid manual liability is not LPF-eligible. REG-1049 / PDT-2187: pay in full, then due-date-change.
  const { liabilityId } = await createOverdueManualLiabilityOnContract(fx, contractIndex, {
    interestRateId: interestId,
  });
  if (!(fx.Responses.paymentPackage ?? []).length) {
    if (!(fx.Responses.collectionChannel ?? []).length) {
      const channelRes = await fx.Request.post(fx.Endpoints.collectionChannel, {
        data: fx.GeneratePayload.receivablesManagement.collection_channel(),
      });
      await expect(channelRes).CheckResponse();
      fx.Responses.collectionChannel.push(await channelRes.json());
    }
    const packagePayload = fx.GeneratePayload.receivablesManagement.payment_package();
    packagePayload.channelId = entityId(fx.Responses.collectionChannel[0]);
    const pkgRes = await fx.Request.post(fx.Endpoints.paymentPackage, { data: packagePayload });
    await expect(pkgRes).CheckResponse();
    fx.Responses.paymentPackage.push(await pkgRes.json());
  }
  const paymentPayload = await fx.GeneratePayload.receivablesManagement.payment();
  paymentPayload.initialAmount = PDT_3179_MANUAL_LIABILITY_AMOUNT;
  const paymentRes = await fx.Request.post(fx.Endpoints.payment, { data: paymentPayload });
  await expect(paymentRes).CheckResponse();
  fx.Responses.payment.push(await paymentRes.json());
  await expect
    .poll(
      async () => asNumber((await getLiability(fx, liabilityId)).currentAmount),
      { message: 'LPF source liability not fully offset before job', timeout: 90_000, intervals: [2000, 5000] },
    )
    .toBe(0);
  const dueChange = await fx.Request.put(
    `${fx.Endpoints.customerLiability}/${liabilityId}/due-date-change?dueDate=${randomGens.generateYesterdaysDate('yyyy-mm-dd')}`,
  );
  await expect(dueChange).CheckResponse();

  const listBefore = await fx.Request.get(
    `${fx.Endpoints.latePaymentFine}/list?page=0&size=25&prompt=${encodeURIComponent(customerIdentifier(fx.Responses))}&type=LATE_PAYMENT_FINE&searchBy=CUSTOMER`,
  );
  await expect(listBefore).CheckResponse();
  const beforeCount = asNumber((await listBefore.json()).totalElements);

  const lpfBody = await fx.GeneratePayload.receivablesManagement.waitForLPFGeneration(false);
  const rows = listingContent(lpfBody);
  expect(asNumber(lpfBody?.totalElements) || rows.length, 'LPF generated').toBeGreaterThan(beforeCount);

  const lpfRow = rows[0] ?? {};
  const lpfId = entityId(lpfRow);
  fx.Responses.latePaymentFine.push(lpfRow);

  const liabList = await fx.Request.get(
    `${fx.Endpoints.customerLiability}/list?page=0&size=50&columns=ID&direction=DESC&prompt=${encodeURIComponent(customerIdentifier(fx.Responses))}&searchFields=CUSTOMER`,
  );
  await expect(liabList).CheckResponse();
  let lpfLiabilityId = 0;
  for (const row of listingContent(await liabList.json())) {
    const id = entityId(row);
    const detail = await getLiability(fx, id);
    const lpfRef = nestedId(detail.latePaymentFineShortResponse) ?? asNumber(detail.latePaymentFineId);
    if (lpfRef === lpfId || asNumber(detail.latePaymentFineId) > 0) {
      lpfLiabilityId = id;
      break;
    }
  }
  expect(lpfLiabilityId, 'LPF customer liability').toBeGreaterThan(0);
  fx.Responses.customerLiability.push(lpfLiabilityId);
  return { lpfId, lpfLiabilityId };
}

export async function createReschedulingInstallment(
  fx: Pdt3379Fx,
): Promise<{ reschedulingId: number; installmentLiabilityId: number }> {
  const interestRes = await fx.Request.post(fx.Endpoints.interestRate, {
    data: fx.GeneratePayload.receivablesManagement.dailyInterestRate(),
  });
  await expect(interestRes).CheckResponse();
  fx.Responses.interestRate.push(await interestRes.json());

  const assessmentPayload = fx.GeneratePayload.receivablesManagement.customer_assessment();
  const assessmentRes = await fx.Request.post(fx.Endpoints.customerAssessment, { data: assessmentPayload });
  await expect(assessmentRes).CheckResponse();
  fx.Responses.customerAssessment.push(await assessmentRes.json());

  const payload = await fx.GeneratePayload.receivablesManagement.rescheduling(
    '3',
    'INTEREST_WITH_THE_FIRST_INSTALLMENT',
  );
  payload.saveAndExecute = true;
  payload.reschedulingStatus = 'EXECUTED';
  const res = await fx.Request.post(fx.Endpoints.rescheduling, { data: payload });
  await expect(res).CheckResponse();
  const reschedulingId = entityId(await res.json());
  fx.Responses.rescheduling.push(reschedulingId);

  const liabList = await fx.Request.get(
    `${fx.Endpoints.customerLiability}/list?page=0&size=50&columns=ID&direction=DESC&prompt=${encodeURIComponent(customerIdentifier(fx.Responses))}&searchFields=CUSTOMER`,
  );
  await expect(liabList).CheckResponse();
  let installmentLiabilityId = 0;
  for (const row of listingContent(await liabList.json())) {
    const id = entityId(row);
    const detail = await getLiability(fx, id);
    const rsId = nestedId(detail.reschedulingShortResponse);
    if (rsId === reschedulingId || rsId > 0) {
      installmentLiabilityId = id;
      break;
    }
  }
  expect(installmentLiabilityId, 'rescheduling installment liability').toBeGreaterThan(0);
  fx.Responses.customerLiability.push(installmentLiabilityId);
  return { reschedulingId, installmentLiabilityId };
}

export async function deleteRfd(fx: Pdt3379Fx, requestId: number): Promise<void> {
  const res = await fx.Request.delete(`${fx.Endpoints.requestForDisconnection}/${requestId}`);
  await expect(res).CheckResponse();
}

export async function deleteProductContract(fx: Pdt3379Fx, contractId: number): Promise<void> {
  const res = await fx.Request.delete(`${fx.Endpoints.productContract}/${contractId}`);
  await expect(res).CheckResponse();
}

/** SIGNED / ACTIVE_IN_TERM cannot be DELETE'd. PDT-2880 status-update after all PODs deactivated. */
export async function terminateProductContract(fx: Pdt3379Fx, contractId: number): Promise<void> {
  const res = await fx.Request.put(
    `${fx.Endpoints.productContract}/status-update/${contractId}?versionId=1`,
    {
      data: {
        contractStatus: 'TERMINATED',
        contractSubStatus: 'ALL_PODS_ARE_DEACTIVATED',
        contractVersionStatus: 'SIGNED',
      },
    },
  );
  await expect(res).CheckResponse();
}

/**
 * Poll this-test reminder until GET 200 + EXECUTED. Does not throw — CREATE RFD
 * validates against getRemindersForDPSRequestList, not GET /list?statuses=EXECUTED.
 */
export async function waitForThisTestReminderExecuted(fx: Pdt3379Fx): Promise<number> {
  if (!fx.Responses.reminderForDisconnection.length) return 0;
  const reminderId = entityId(fx.Responses.reminderForDisconnection[0]);
  const deadline = Date.now() + PDT_3379_REMINDER_EXECUTED_WAIT_MS;
  while (Date.now() < deadline) {
    const getRes = await fx.Request.get(`power-supply-disconnection-reminder/${reminderId}`, {
      timeout: 5_000,
    });
    if (getRes.ok()) {
      const body = (await getRes.json()) as { reminderStatus?: string };
      if (String(body.reminderStatus) === 'EXECUTED') {
        return reminderId;
      }
    }
    await new Promise((r) => setTimeout(r, PDT_3379_REMINDER_POLL_MS));
  }
  return 0;
}

/**
 * RFD CREATE always checks getRemindersForDPSRequestList("").
 * Same SQL family: GET .../reminders-list-for-disconnection-request (omit empty prompt).
 * Do not use GET /list?statuses=EXECUTED — those ids 400 as "Reminder … not found".
 */
export async function resolvePdt3379ReminderIdForRfd(fx: Pdt3379Fx): Promise<number> {
  await waitForThisTestReminderExecuted(fx);
  const createdId = fx.Responses.reminderForDisconnection.length
    ? entityId(fx.Responses.reminderForDisconnection[0])
    : 0;
  const deadline = Date.now() + PDT_3379_DPS_REMINDER_LIST_WAIT_MS;
  let lastRows: Record<string, unknown>[] = [];
  while (Date.now() < deadline) {
    try {
      const listRes = await fx.Request.get(
        `${fx.Endpoints.reminderForDisconnection}/reminders-list-for-disconnection-request?page=0&size=50`,
        { timeout: PDT_3379_DPS_REMINDER_LIST_GET_TIMEOUT_MS },
      );
      if (listRes.ok()) {
        await expect(listRes).CheckResponse();
        lastRows = listingContent(await listRes.json());
        const listedIds = lastRows.map((row) => entityId(row)).filter((id) => id > 0);
        if (createdId > 0 && listedIds.includes(createdId)) return createdId;
        if (listedIds.length > 0) return listedIds[0];
      }
    } catch {
      // GET timeout on slow DPS-list SQL — poll again until deadline.
    }
    await new Promise((r) => setTimeout(r, PDT_3379_DPS_REMINDER_LIST_POLL_MS));
  }
  throw new Error(
    `reminders-list-for-disconnection-request stayed empty after ${PDT_3379_DPS_REMINDER_LIST_WAIT_MS / 1000}s (this-test=${createdId}, lastRows=${lastRows.length}). RFD CREATE cannot use GET /list?statuses=EXECUTED.`,
  );
}

export function newestExpected(dual: Pdt3379DualResult): Pdt3379NewestExpected {
  expect(dual.newBillingGroupId, 'NEW billing group').toBeTruthy();
  expect(dual.newContractId, 'NEW product contract').toBeTruthy();
  return {
    billingGroupId: dual.newBillingGroupId as number,
    productContractId: dual.newContractId as number,
  };
}
