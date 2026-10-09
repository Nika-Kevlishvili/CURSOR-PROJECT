/**

 * PDT-3454 — Open-end product contract +1 version on product apply (bug path helpers).

 *

 * Slimmed Playwright spec keeps only TC-BE-2: the REAL bug trigger is PERIOD

 * recalculation from an old initialTermDate/signingDate during async CREATE.

 *

 * CustomerMapperService.mapProductContractUpdateRequest does NOT copy

 * basicParameters.contractTermEndDate (stays null). Then

 * ProductContractDateService.setContractTermEndDate recalculates:

 *   termEnd = initialTermDate + PERIOD − 1  (for PERIOD terms)

 * ProductContractProductParametersService.validateContractTermEndDate fails when

 * that recalculated termEnd < activationDate — async edit throws before

 * adjustPodActivation; no +1 version.

 *

 * Fresh contracts with initialTerm ≈ today do NOT hit the bug (89424 / 89328-style):

 * recalc termEnd ≈ today+11 ≥ activation → CREATE succeeds. Do NOT use

 * forceAndAssertTermEndStrictlyBeforeD (D > header T) as the primary gate.

 *

 * Bug setup:

 * - Keep PERIOD product (12 DAY_DAYS from createPdt3330BaseChain).

 * - Pin version startDate + signingDate + startOfInitialTerm far in the past.

 * - Activate POD with activationDate = today (>> initialTerm + period).

 * - Header contractTermEndDate before apply must be ≥ activation (long/null)

 *   so setup PUTs succeed — NOT T < D.

 * - Apply with newContractsVersionsStartDate = D (today+10).

 *

 * Reuses PDT-3330 chain/apply helpers. Each run POSTs a new term, price component,

 * product, customer, and POD, then POSTs a signed contract with

 * createSignedProductContractOnIndices on those just-created indices.

 *

 * Reference spec(s):

 * - tests/cursor/PDT-3330-product-new-version-apply-two-stage-contract-update.spec.ts

 * - tests/cursor/pdt-3330-product-new-version-apply-two-stage-contract-update.fixtures.ts

 * - tests/cursor/pdt-2815-version-validity.fixtures.ts (activatePodOnContract)

 * - tests/cursor/pdt-2906-product-new-version-contract-update.fixtures.ts (past startDate)

 *

 * Swagger (dev):

 * - PUT /products/{id} ProductEditRequest

 * - POST /products/validate-product-related-contract-update

 * - GET /product-contract/{id}?versionId=

 * - PUT /product-contract/{id}?versionId=&changeFutureVersionsPods=

 * - PUT /product-contract/status-update/{id}?versionId= (READY→SIGNED after pin PUT)

 * - POST /contract-pods/manual PodManualActivationRequest

 * - BaseProductTermsRequest.typeOfTerms PERIOD|WITHOUT_TERM; periodType DAY_DAYS; value 1..9999

 */



import { expect as baseExpect } from '../../fixtures/baseFixture';

import type { baseFixture } from '../../fixtures/baseFixture';

import type { ProductPayload } from '../../jsons/payloads/create/productAndServices/product';

import {

  activatePodOnContract,

  anchorDateIso,

  buildEditProductContractPayload,

  headerContractStatus,

  productContractStatusUpdate,

} from './pdt-2815-version-validity.fixtures';

import {

  addDaysIso,

  applyFixedParameterProductFields,

  applyFixedParameterTermFields,

  contractVersionRows,

  createFollowingContractVersion,

  createPdt3330BaseChain,

  createSignedProductContractOnIndices,

  entityId,

  ensureVersionStartsBefore,

  findVersionByStartDate,

  loadContractVersionProductRef,

  loadProductContract,

  loadProductDetailSnapshot,

  loadVersionProductDetailId,

  pdt3330DateAliases,

  pollUntilContractVersionStartDate,

  putProductEdit,

  validateProductRelatedContractUpdate,

  buildPdt3330ProductEditPayload,

  type BaseChainResult,

} from './pdt-3330-product-new-version-apply-two-stage-contract-update.fixtures';



type FixtureRequest = baseFixture['Request'];



export {

  addDaysIso,

  buildPdt3330ProductEditPayload,

  contractVersionRows,

  createFollowingContractVersion,

  createPdt3330BaseChain,

  createSignedProductContractOnIndices,

  ensureVersionStartsBefore,

  findVersionByStartDate,

  loadContractVersionProductRef,

  loadProductContract,

  loadProductDetailSnapshot,

  loadVersionProductDetailId,

  pollUntilContractVersionStartDate,

  putProductEdit,

  validateProductRelatedContractUpdate,

};



export const JIRA_TITLE = 'Creating New Version +1 Case';



/** Matches createPdt3330BaseChain / applyFixedParameterProductFields PERIOD length. */

export const PDT_3454_PERIOD_DAY_DAYS = 12;



export function pdt3454TestTitle(tcId: string, scenario: string): string {

  return `[PDT-3454]: ${JIRA_TITLE} — ${tcId} ${scenario}`;

}



export const PDT_3454_RELEVANT_ENTITIES = ['customer', 'product', 'productContract'] as const;



/**

 * Indices of the customer / product / POD just POSTed by createPdt3330BaseChain

 * in this test. Capture immediately after the chain — do not default to 0 from

 * another test's leftover Responses, and never query an existing catalog row.

 */

export function justCreatedChainIndices(

  Responses: baseFixture['Responses'],

  productIndex: number,

): { customerIndex: number; productIndex: number; podIndex: number } {

  const customerIndex = Responses.customer.length - 1;

  const podIndex = Responses.pod.length - 1;

  if (customerIndex < 0 || podIndex < 0 || productIndex < 0) {

    throw new Error(

      'PDT-3454: createPdt3330BaseChain must POST a new customer, product, and POD before the contract',

    );

  }

  return { customerIndex, productIndex, podIndex };

}



export function entityIdFromResponses(

  row: { id?: number } | undefined,

  label: string,

): number {

  const id = Number(row?.id);

  if (!Number.isFinite(id) || id <= 0) {

    throw new Error(`PDT-3454: missing just-created ${label} id`);

  }

  return id;

}



export type Pdt3454DateAliases = ReturnType<typeof pdt3330DateAliases> & {

  DMinus1: string;

  /** Far-past initial term / version start / signing — PERIOD recalc lands before today. */

  pastInitialTerm: string;

  /** initialTerm + PERIOD − 1 (DAY_DAYS) — must be < activation for bug. */

  expectedPeriodRecalcTermEnd: string;

};



export function pdt3454DateAliases(reference = new Date()): Pdt3454DateAliases {

  const dates = pdt3330DateAliases(reference);

  const pastInitialTerm = addDaysIso(dates.todayDate, -400);

  return {

    ...dates,

    DMinus1: addDaysIso(dates.D, -1),

    pastInitialTerm,

    expectedPeriodRecalcTermEnd: addDaysIso(pastInitialTerm, PDT_3454_PERIOD_DAY_DAYS - 1),

  };

}



export type HeaderDates = {

  /** Raw basicParameters.activationDate only — not supplyActivationDate. */

  activationDate: string | null;

  contractTermEndDate: string | null;

  startOfInitialTerm: string | null;

  signingDate: string | null;

};



/**

 * Header dates for PDT-3454 assertions.

 * POD activated? → basicParameters.activationDate only.

 * supplyActivationDate (FIRST_DAY_OF_MONTH → 1st of next month) is NOT POD activation.

 */

export function headerDatesFromBody(body: Record<string, unknown>): HeaderDates {

  const bp = (body.basicParameters ?? {}) as {

    activationDate?: string | null;

    contractTermEndDate?: string | null;

    startOfInitialTerm?: string | null;

    signingDate?: string | null;

  };

  const activationRaw = bp.activationDate ?? null;

  return {

    activationDate:

      activationRaw != null && activationRaw !== '' ? String(activationRaw).slice(0, 10) : null,

    contractTermEndDate:

      bp.contractTermEndDate != null && bp.contractTermEndDate !== ''

        ? String(bp.contractTermEndDate).slice(0, 10)

        : null,

    startOfInitialTerm:

      bp.startOfInitialTerm != null && bp.startOfInitialTerm !== ''

        ? String(bp.startOfInitialTerm).slice(0, 10)

        : null,

    signingDate:

      bp.signingDate != null && bp.signingDate !== '' ? String(bp.signingDate).slice(0, 10) : null,

  };

}



/** Same as headerDatesFromBody.activationDate — explicit name for setup gates. */

export function basicParametersActivationDate(body: Record<string, unknown>): string | null {

  return headerDatesFromBody(body).activationDate;

}



/**

 * Effective initial-term date used for PERIOD recalc when product term is SIGNING:

 * prefer header startOfInitialTerm, else signingDate (ProductContractDateService).

 */

export function effectiveInitialTermDate(body: Record<string, unknown>): string | null {

  const h = headerDatesFromBody(body);

  return h.startOfInitialTerm ?? h.signingDate;

}



function stripAddressCoords(addr: unknown): void {

  if (!addr || typeof addr !== 'object') return;

  const row = addr as Record<string, unknown>;

  delete row.latitude;

  delete row.longitude;

}



function stripCustomerCoords(payload: Record<string, unknown>): void {

  stripAddressCoords(payload.address);

  const comm = payload.communicationData;

  if (!Array.isArray(comm)) return;

  for (const entry of comm) {

    if (entry && typeof entry === 'object') {

      stripAddressCoords((entry as Record<string, unknown>).address);

    }

  }

}



/**

 * Override pdt-2906 12 DAY_DAYS terms so async fillContractTermEndDate does not

 * pin header end ≈ initialTerm+11 near D. Swagger: BaseProductTermsRequest value 1..9999.

 * Used only by optional createPdt3454BaseChain (long period / happy-path helpers).

 */

export function applyPdt3454LongProductTerms(payload: ProductPayload): void {

  payload.productTerms = [

    {

      typeOfTerms: 'PERIOD',

      value: '3650',

      periodType: 'DAY_DAYS',

      renewalPeriodValue: null,

      renewalPeriodType: null,

      perpetuityCause: false,

      automaticRenewal: null,

      numberOfRenewals: null,

      name: '3650 Day/Days Period',

      id: null,

    },

  ];

}



/**

 * Same entity order as createPdt3330BaseChain, but productTerms PERIOD 3650 DAY_DAYS

 * after applyFixedParameterProductFields (do not edit pdt-2906). Optional happy-path helper.

 */

export async function createPdt3454BaseChain(

  Request: FixtureRequest,

  GeneratePayload: baseFixture['GeneratePayload'],

  Responses: baseFixture['Responses'],

  Endpoints: baseFixture['Endpoints'],

  TestRunSummary?: { registerPayload: (key: string, payload: unknown) => void },

): Promise<BaseChainResult> {

  const termPayload = GeneratePayload.productAndServices.term();

  applyFixedParameterTermFields(termPayload);

  TestRunSummary?.registerPayload('terms', termPayload);

  const termRes = await Request.post(Endpoints.terms, { data: termPayload });

  await baseExpect(termRes).CheckResponse();

  Responses.terms.push(await termRes.json());



  const pricePayload = GeneratePayload.productAndServices.electricity();

  TestRunSummary?.registerPayload('priceComponent', pricePayload);

  const priceRes = await Request.post(Endpoints.priceComponent, { data: pricePayload });

  await baseExpect(priceRes).CheckResponse();

  Responses.priceComponent.push(await priceRes.json());



  const productPayload = GeneratePayload.productAndServices.product();

  applyFixedParameterProductFields(productPayload);

  applyPdt3454LongProductTerms(productPayload);

  TestRunSummary?.registerPayload('product', productPayload);

  const productRes = await Request.post(Endpoints.product, { data: productPayload });

  await baseExpect(productRes).CheckResponse();

  Responses.product.push(await productRes.json());

  const productId = entityId(Responses.product[Responses.product.length - 1]);

  const productIndex = Responses.product.length - 1;



  const snap = await loadProductDetailSnapshot(Request, Endpoints, productId, 1);



  const customerPayload = GeneratePayload.customers.customer_legal() as Record<string, unknown>;

  stripCustomerCoords(customerPayload);

  TestRunSummary?.registerPayload('customer', customerPayload);

  const customerRes = await Request.post(Endpoints.customer, { data: customerPayload });

  await baseExpect(customerRes).CheckResponse();

  Responses.customer.push(await customerRes.json());



  const podPayload = GeneratePayload.pointsOfDelivery.pod_settlement();

  podPayload.type = 'CONSUMER';

  podPayload.consumptionPurpose = 'NON_HOUSEHOLD';

  podPayload.voltageLevel = 'LOW';

  TestRunSummary?.registerPayload('pod', podPayload);

  const podRes = await Request.post(Endpoints.pod, { data: podPayload });

  await baseExpect(podRes).CheckResponse();

  Responses.pod.push(await podRes.json());



  return { productId, productDetailV1Id: snap.detailId, productIndex };

}



const SIGNED_SUBSTATUSES_FOR_POD_ACTIVATION = new Set(['SIGNED_BY_BOTH_SIDES', 'SPECIAL_PROCESSES']);



function isHeaderEnteredIntoForceFamily(headerStatus: string | undefined): boolean {

  return (

    headerStatus === 'ENTERED_INTO_FORCE' ||

    headerStatus === 'ACTIVE_IN_TERM' ||

    headerStatus === 'ACTIVE_IN_PERPETUITY'

  );

}



function signedSubStatusAllowsPodActivation(subStatus: string | undefined): boolean {

  return SIGNED_SUBSTATUSES_FOR_POD_ACTIVATION.has(String(subStatus ?? ''));

}



function isHeaderEligibleForPodOrCreate(

  headerStatus: string | undefined,

  subStatus: string | undefined,

): boolean {

  if (isHeaderEnteredIntoForceFamily(headerStatus)) return true;

  return headerStatus === 'SIGNED' && signedSubStatusAllowsPodActivation(subStatus);

}



function isoDateOrNull(value: unknown): string | null {

  if (value == null || value === '') return null;

  return String(value).slice(0, 10);

}



function maxIsoDate(...dates: string[]): string {

  return dates.map((d) => d.slice(0, 10)).reduce((a, b) => (a >= b ? a : b));

}



function headerNeedsEntryInForceDate(headerStatus: string | undefined): boolean {

  return isHeaderEnteredIntoForceFamily(headerStatus);

}



/**

 * READY→SIGNED (or SIGNED→SIGNED_BY_BOTH_SIDES). Never demote ENTERED_INTO_FORCE family.

 */

async function ensureHeaderEligibleForPodActivation(

  Request: FixtureRequest,

  contractId: number,

  versionId: number,

): Promise<void> {

  const body = await loadProductContract(Request, contractId);

  const headerStatus = headerContractStatus(body);

  const subStatus = (body.basicParameters as { subStatus?: string } | undefined)?.subStatus;



  if (isHeaderEnteredIntoForceFamily(headerStatus)) {

    return;

  }

  if (headerStatus === 'SIGNED' && signedSubStatusAllowsPodActivation(subStatus)) {

    return;

  }

  if (headerStatus === 'SIGNED') {

    const signed = await productContractStatusUpdate(

      Request,

      contractId,

      'SIGNED',

      'SIGNED_BY_BOTH_SIDES',

      versionId,

    );

    await baseExpect(signed).CheckResponse();

    return;

  }

  if (headerStatus === 'DRAFT' || headerStatus === 'READY') {

    if (headerStatus !== 'READY') {

      const ready = await productContractStatusUpdate(Request, contractId, 'READY', 'READY', versionId);

      await baseExpect(ready).CheckResponse();

    }

    const signed = await productContractStatusUpdate(

      Request,

      contractId,

      'SIGNED',

      'SIGNED_BY_BOTH_SIDES',

      versionId,

    );

    await baseExpect(signed).CheckResponse();

    return;

  }



  throw new Error(

    `PDT-3454 setup: header status ${headerStatus ?? '(missing)'} is not eligible for ` +

      `POD activation / CREATE (need SIGNED(SIGNED_BY_BOTH_SIDES|SPECIAL_PROCESSES) / ` +

      `ENTERED_INTO_FORCE / ACTIVE_IN_TERM / ACTIVE_IN_PERPETUITY). Do not demote ENTERED_INTO_FORCE.`,

  );

}



async function putContractHeaderDates(

  Request: FixtureRequest,

  Endpoints: baseFixture['Endpoints'],

  contractId: number,

  contractPayload: Record<string, unknown>,

  versionId: number,

  startDate: string,

  patch: {

    contractTermEndDate?: string | null;

    activationDate?: string | null;

    signingDate?: string | null;

    startOfInitialTerm?: string | null;

    entryInForceDate?: string | null;

  },

): Promise<void> {

  const editPayload = await buildEditProductContractPayload(Request, contractId, contractPayload, {

    startDate: startDate.slice(0, 10),

    savingAsNewVersion: false,

    preserveSigningDate: true,

  });

  const bp = editPayload.basicParameters as Record<string, unknown>;



  const currentRes = await Request.get(`${Endpoints.productContract}/${contractId}?versionId=${versionId}`);

  await baseExpect(currentRes).CheckResponse();

  const current = (await currentRes.json()) as Record<string, unknown>;

  const currentBp = (current.basicParameters ?? {}) as {

    entryInForceDate?: string | null;

    startOfInitialTerm?: string | null;

    signingDate?: string | null;

  };

  const headerSt = headerContractStatus(current);

  const fromGetEntry = isoDateOrNull(currentBp.entryInForceDate);

  const fromGetStartTerm = isoDateOrNull(currentBp.startOfInitialTerm);



  if (headerNeedsEntryInForceDate(headerSt)) {

    const entryInForce = fromGetEntry ?? anchorDateIso();

    bp.entryInForceDate = entryInForce;

    bp.startOfInitialTerm = fromGetStartTerm ?? entryInForce;

  } else if (fromGetEntry != null) {

    bp.entryInForceDate = fromGetEntry;

    if (fromGetStartTerm != null) bp.startOfInitialTerm = fromGetStartTerm;

  }



  if (patch.contractTermEndDate !== undefined) {

    bp.contractTermEndDate = patch.contractTermEndDate;

  }

  if (patch.activationDate !== undefined) {

    bp.activationDate = patch.activationDate;

  }

  if (patch.signingDate !== undefined) {

    bp.signingDate = patch.signingDate;

  }

  if (patch.startOfInitialTerm !== undefined) {

    bp.startOfInitialTerm = patch.startOfInitialTerm;

  }

  if (patch.entryInForceDate !== undefined) {

    bp.entryInForceDate = patch.entryInForceDate;

  }



  const put = await Request.put(

    `${Endpoints.productContract}/${contractId}?versionId=${versionId}&changeFutureVersionsPods=false`,

    { data: editPayload },

  );

  await baseExpect(put).CheckResponse();

}



/**

 * Ensure header eligible for POD activate WITHOUT forcing signingDate to today.

 * Past signingDate is required so SIGNING → initialTermDate stays in the past

 * (PERIOD recalc then lands before today's activation).

 */

async function ensureHeaderEligibleKeepingSigningDate(args: {

  Request: FixtureRequest;

  contractId: number;

  versionId: number;

}): Promise<void> {

  const { Request, contractId, versionId } = args;

  const body = await loadProductContract(Request, contractId);

  const headerStatus = headerContractStatus(body);

  const subStatus = (body.basicParameters as { subStatus?: string } | undefined)?.subStatus;

  if (isHeaderEligibleForPodOrCreate(headerStatus, subStatus)) {

    return;

  }

  await ensureHeaderEligibleForPodActivation(Request, contractId, versionId);

}



/**

 * Pin version start + signing + startOfInitialTerm to pastInitialTerm, and ensure

 * header contractTermEndDate is null or ≥ safeTermEndMin (activation / today).

 * Does NOT force T < D (that was the wrong 89424-style gate).

 */

export async function pinPastInitialTermOpenEndVersion(args: {

  Request: FixtureRequest;

  Endpoints: baseFixture['Endpoints'];

  contractId: number;

  contractPayload: Record<string, unknown>;

  openEndVersionId: number;

  pastInitialTerm: string;

  D: string;

  /** Minimum allowed header term-end (usually today / activation). */

  safeTermEndMin: string;

}): Promise<{ startDate: string; signingDate: string; startOfInitialTerm: string | null }> {

  const {

    Request,

    Endpoints,

    contractId,

    contractPayload,

    openEndVersionId,

    pastInitialTerm,

    D,

    safeTermEndMin,

  } = args;

  const past = pastInitialTerm.slice(0, 10);

  const beforeD = D.slice(0, 10);

  const termMin = safeTermEndMin.slice(0, 10);



  if (!(past < beforeD)) {

    throw new Error(

      `PDT-3454 bug setup: pastInitialTerm ${past} must be < D=${beforeD}`,

    );

  }



  await ensureHeaderEligibleKeepingSigningDate({

    Request,

    contractId,

    versionId: openEndVersionId,

  });



  const termEndSafe = addDaysIso(termMin, 365);

  await putContractHeaderDates(

    Request,

    Endpoints,

    contractId,

    contractPayload,

    openEndVersionId,

    past,

    {

      signingDate: past,

      startOfInitialTerm: past,

      entryInForceDate: null,

      contractTermEndDate: termEndSafe,

    },

  );



  const after = await loadProductContract(Request, contractId);

  const open = contractVersionRows(after).find((r) => r.versionId === openEndVersionId);

  if (!open || open.endDate != null) {

    throw new Error(

      `PDT-3454 bug setup: version must stay Open End; got endDate=${open?.endDate}`,

    );

  }

  if (open.startDate.slice(0, 10) !== past) {

    throw new Error(

      `PDT-3454 bug setup: version startDate must be ${past}; got ${open.startDate}`,

    );

  }

  if (!(open.startDate < beforeD)) {

    throw new Error(

      `PDT-3454 bug setup: version startDate ${open.startDate} must be < D=${beforeD}`,

    );

  }



  const header = headerDatesFromBody(after);

  if (header.signingDate !== past) {

    throw new Error(

      `PDT-3454 bug setup: signingDate must stay ${past} (SIGNING→initialTerm); got ${header.signingDate}`,

    );

  }

  const effectiveInitial = effectiveInitialTermDate(after);

  if (effectiveInitial !== past) {

    throw new Error(

      `PDT-3454 bug setup: effective initialTerm (startOfInitialTerm|signingDate) must be ${past}; ` +

        `got startOfInitialTerm=${header.startOfInitialTerm}, signingDate=${header.signingDate}`,

    );

  }

  if (!(header.contractTermEndDate == null || header.contractTermEndDate >= termMin)) {

    throw new Error(

      `PDT-3454 bug setup: header contractTermEndDate must be null or ≥ ${termMin} ` +

        `(setup must not use T < activation); got ${header.contractTermEndDate}`,

    );

  }



  return {

    startDate: open.startDate.slice(0, 10),

    signingDate: header.signingDate!,

    startOfInitialTerm: header.startOfInitialTerm,

  };

}



/**

 * After POD activate: keep header term-end ≥ activation (long/null). Do NOT force T < D.

 */

export async function ensureHeaderTermEndAtLeastActivation(args: {

  Request: FixtureRequest;

  Endpoints: baseFixture['Endpoints'];

  contractId: number;

  contractPayload: Record<string, unknown>;

  openEndVersionId: number;

  startDate: string;

}): Promise<HeaderDates> {

  const { Request, Endpoints, contractId, contractPayload, openEndVersionId, startDate } = args;



  const body0 = await loadProductContract(Request, contractId);

  const header0 = headerDatesFromBody(body0);

  if (header0.activationDate == null) {

    throw new Error(

      `PDT-3454 bug setup: activationDate must be set after POD activate before term-end safety PUT`,

    );

  }

  const activation = header0.activationDate;

  const needsSafeTerm =

    header0.contractTermEndDate != null && header0.contractTermEndDate < activation;



  if (needsSafeTerm || header0.contractTermEndDate == null) {

    const termEndSafe = addDaysIso(activation, 365);

    await putContractHeaderDates(

      Request,

      Endpoints,

      contractId,

      contractPayload,

      openEndVersionId,

      startDate.slice(0, 10),

      { contractTermEndDate: termEndSafe },

    );

  }



  const after = await loadProductContract(Request, contractId);

  const header = headerDatesFromBody(after);

  if (header.activationDate == null) {

    throw new Error(

      `PDT-3454 bug setup: activationDate became null while ensuring safe term-end`,

    );

  }

  if (header.contractTermEndDate != null && header.contractTermEndDate < header.activationDate) {

    throw new Error(

      `PDT-3454 bug setup: contractTermEndDate ${header.contractTermEndDate} must stay ≥ ` +

        `activation ${header.activationDate} so setup PUTs succeed (bug is PERIOD recalc, not T < activation)`,

    );

  }

  return header;

}



/**

 * TC-BE-2 setup — past initialTerm + PERIOD 12 DAY_DAYS + POD activated today.

 *

 * Order:

 * 1. Pin version startDate / signingDate / startOfInitialTerm = pastInitialTerm (< D).

 * 2. Header eligible SIGNED without rewriting signing to today.

 * 3. POST /contract-pods/manual with activationDate = todayDate (>> pastInitial + period).

 * 4. Ensure header contractTermEndDate ≥ activation (long term-end) — NOT T < D.

 * 5. Assert: pastInitial + 12 − 1 < activation; Open End; start < D; POD activated.

 *

 * During CREATE, mapper drops term-end → PERIOD recalc from past initial → termEnd < activation

 * → validateContractTermEndDate throws → no +1 version on current Dev.

 */

export async function applyActivatedPodWithPastInitialTermPeriodBug(args: {

  Request: FixtureRequest;

  Endpoints: baseFixture['Endpoints'];

  Responses: baseFixture['Responses'];

  contractId: number;

  contractPayload: Record<string, unknown>;

  openEndVersionId: number;

  todayDate: string;

  D: string;

  pastInitialTerm: string;

  expectedPeriodRecalcTermEnd: string;

  podIndex?: number;

}): Promise<HeaderDates & { versionStartDate: string; effectiveInitialTerm: string }> {

  const {

    Request,

    Endpoints,

    Responses,

    contractId,

    contractPayload,

    openEndVersionId,

    todayDate,

    D,

    pastInitialTerm,

    expectedPeriodRecalcTermEnd,

    podIndex = 0,

  } = args;



  const today = todayDate.slice(0, 10);

  const beforeD = D.slice(0, 10);

  const past = pastInitialTerm.slice(0, 10);

  const recalcEnd = expectedPeriodRecalcTermEnd.slice(0, 10);



  if (!(recalcEnd < today)) {

    throw new Error(

      `PDT-3454 bug setup: expected PERIOD recalc termEnd ${recalcEnd} ` +

        `(pastInitial ${past} + ${PDT_3454_PERIOD_DAY_DAYS} − 1) must be < today/activation ${today}`,

    );

  }



  const beforeActivate = await loadProductContract(Request, contractId);

  if (!contractVersionRows(beforeActivate).some((r) => r.versionId === openEndVersionId)) {

    throw new Error(`Open-end version ${openEndVersionId} not found on contract ${contractId}`);

  }



  const pinned = await pinPastInitialTermOpenEndVersion({

    Request,

    Endpoints,

    contractId,

    contractPayload,

    openEndVersionId,

    pastInitialTerm: past,

    D: beforeD,

    safeTermEndMin: today,

  });



  // Activate on today — must stay < D; past signing ≤ today satisfies existsForActivation.

  if (!(today < beforeD)) {

    throw new Error(

      `PDT-3454 bug setup: today=${today} must be < D=${beforeD} for POD activate before apply`,

    );

  }



  try {

    await activatePodOnContract(Request, Responses, contractId, openEndVersionId, today, podIndex);

  } catch (err) {

    const snap = await loadProductContract(Request, contractId);

    const snapBp = (snap.basicParameters ?? {}) as {

      subStatus?: string;

      signingDate?: string | null;

      startOfInitialTerm?: string | null;

    };

    const snapRow = contractVersionRows(snap).find((r) => r.versionId === openEndVersionId);

    const msg = err instanceof Error ? err.message : String(err);

    throw new Error(

      `PDT-3454 POD activate failed for contract ${contractId} version ${openEndVersionId}. ` +

        `activationDate attempted=${today}. GET snapshot: headerStatus=${headerContractStatus(snap) ?? '(missing)'}; ` +

        `subStatus=${snapBp.subStatus ?? '(missing)'}; signingDate=${isoDateOrNull(snapBp.signingDate) ?? '(null)'}; ` +

        `startOfInitialTerm=${isoDateOrNull(snapBp.startOfInitialTerm) ?? '(null)'}; ` +

        `versionStart=${snapRow?.startDate ?? '(missing)'}. Cause: ${msg}`,

    );

  }



  const header = await ensureHeaderTermEndAtLeastActivation({

    Request,

    Endpoints,

    contractId,

    contractPayload,

    openEndVersionId,

    startDate: pinned.startDate,

  });



  const after = await loadProductContract(Request, contractId);

  const openAfter = contractVersionRows(after).find((r) => r.versionId === openEndVersionId);

  if (!openAfter || openAfter.endDate != null) {

    throw new Error(

      `PDT-3454 bug setup must keep version Open End; got endDate=${openAfter?.endDate}`,

    );

  }

  if (!(openAfter.startDate < beforeD)) {

    throw new Error(

      `PDT-3454 bug setup needs version startDate < D=${beforeD}; got ${openAfter.startDate}`,

    );

  }



  const effectiveInitial = effectiveInitialTermDate(after);

  if (effectiveInitial == null || effectiveInitial !== past) {

    throw new Error(

      `PDT-3454 bug setup: effective initialTerm must remain ${past} after activate; got ${effectiveInitial}`,

    );

  }

  if (header.activationDate == null) {

    const pods = (after.contractPodsResponses ?? []) as { activationDate?: string | null }[];

    const podActivationSet = pods.some((p) => p.activationDate != null && String(p.activationDate) !== '');

    if (!podActivationSet) {

      throw new Error(

        `PDT-3454 bug setup needs POD/header activationDate not null after POST /contract-pods/manual`,

      );

    }

  }

  const activation = header.activationDate ?? today;

  const periodRecalcWouldBe = addDaysIso(effectiveInitial, PDT_3454_PERIOD_DAY_DAYS - 1);

  if (!(periodRecalcWouldBe < activation)) {

    throw new Error(

      `PDT-3454 bug setup FAILED: PERIOD recalc ${periodRecalcWouldBe} ` +

        `(initialTerm ${effectiveInitial} + ${PDT_3454_PERIOD_DAY_DAYS} − 1) must be < activation ${activation}. ` +

        `Without this inequality CREATE will succeed (89424-style) — abort.`,

    );

  }

  if (header.contractTermEndDate != null && header.contractTermEndDate < activation) {

    throw new Error(

      `PDT-3454 bug setup: header termEnd ${header.contractTermEndDate} < activation ${activation} — ` +

        `setup PUT path is wrong; keep termEnd ≥ activation (bug is async PERIOD recalc, not header T < activation)`,

    );

  }



  return {

    ...header,

    activationDate: activation,

    versionStartDate: openAfter.startDate.slice(0, 10),

    effectiveInitialTerm: effectiveInitial,

  };

}



/**

 * @deprecated Wrong bug gate (D > header T → 89424 happy path). Prefer

 * applyActivatedPodWithPastInitialTermPeriodBug. Kept only for reference; do not call.

 */

export async function forceAndAssertTermEndStrictlyBeforeD(args: {

  Request: FixtureRequest;

  Endpoints: baseFixture['Endpoints'];

  contractId: number;

  contractPayload: Record<string, unknown>;

  openEndVersionId: number;

  todayDate: string;

  D: string;

  preferredTermEnd?: string;

}): Promise<HeaderDates> {

  throw new Error(

    `PDT-3454: forceAndAssertTermEndStrictlyBeforeD is deprecated — D > header T is NOT the bug ` +

      `(creates 89424-style PERIOD recalc ≥ activation). Use applyActivatedPodWithPastInitialTermPeriodBug. ` +

      `Args: D=${args.D}, today=${args.todayDate}`,

  );

}



/**

 * @deprecated Use applyActivatedPodWithPastInitialTermPeriodBug.

 */

export async function applyActivatedPodWithTermEndBeforeD(

  args: Parameters<typeof applyActivatedPodWithPastInitialTermPeriodBug>[0],

): Promise<HeaderDates & { versionStartDate: string; effectiveInitialTerm: string }> {

  return applyActivatedPodWithPastInitialTermPeriodBug(args);

}



export async function waitMs(ms: number): Promise<void> {

  await new Promise((resolve) => setTimeout(resolve, ms));

}



