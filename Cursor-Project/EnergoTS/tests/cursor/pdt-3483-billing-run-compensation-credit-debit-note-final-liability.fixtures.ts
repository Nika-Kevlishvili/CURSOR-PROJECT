/**
 * PDT-3483 — Billing Run Compensation FinalLiabilityAmount on Credit Note and Debit Note.
 *
 * FLA is read from GET billing-run/generate-invoice-data (PascalCase FinalLiabilityAmount).
 * IAP leftover auto-offset is NOT asserted. Compensation is POSTed after draft (T known)
 * and before start-generating. One signed documentAmount per Excel case.
 *
 * Credit Note: REAL IAP deduction larger than the period invoice (DEV profile
 * totals were ~34.5M with PC expression 10; IAP 200 produced DEBIT_NOTE).
 * Debit case: FOR_VOLUMES with no IAP (or IAP smaller than volumes) — the
 * document is INVOICE or DEBIT_NOTE and generates a liability. No correction.
 *
 * Excel Case 2 = -10 (not Word story -40). T=50 worked example; otherwise scale C.
 *
 * Backend TC: Cursor-Project/test_cases/Backend/PDT-3483_billing_run_compensation_credit_debit_note_final_liability.md
 *
 * Swagger: DEV refresh this session (Cursor-Project/config/swagger/dev/swagger-spec.json).
 *
 * Reference spec(s):
 * - tests/cursor/PDT-3470-standard-credit-note-government-compensation-receivable.spec.ts
 * - tests/cursor/pdt-3470-standard-credit-note-government-compensation-receivable.fixtures.ts
 * - tests/cursor/pdt-3087-government-compensation-defer-to-pdf.fixtures.ts
 * - tests/cursor/dev-volume-billing-two-compensations.fixtures.ts
 * - tests/cursor/cursor-test.fixtures.ts
 */

import { expect, test } from './cursor-test.fixtures';
import {
  createStandardBillingRun,
  startBillingAndObserveDraft,
  type ExpParity03Fx,
} from './exp-parity-03-interim-deduction-and-issue-date.fixtures';
import {
  createForVolumesDraftBillingRun,
  getCompensation,
  getInvoice,
  isNullishEntityRef,
  runDevVolCompContractAndBbpPrechain,
  startGeneratingAndWaitGenerated,
  type CompensationView,
  type InvoiceView,
  type Pdt3087Fx,
} from './pdt-3087-government-compensation-defer-to-pdf.fixtures';
import type { DevVolCompPrechainResult } from './dev-volume-billing-two-compensations.fixtures';
import {
  asEntityId,
  buildPdt3470IapRealChain as buildPdt3470IapRealChainBase,
  createPdt3470Compensation,
  generatePdt3470VolumeRun,
  listCompensationsByNumber,
  nestedId,
  pdt3470PortalExtraLinks,
  postZeroAmountCompensationExpect400,
  uniqueCompensationNumber,
  type Pdt3470Fx,
  type Pdt3470IapReadyChain,
} from './pdt-3470-standard-credit-note-government-compensation-receivable.fixtures';

export {
  asEntityId,
  createPdt3470Compensation,
  generatePdt3470VolumeRun,
  getCompensation,
  getInvoice,
  isNullishEntityRef,
  listCompensationsByNumber,
  nestedId,
  pdt3470PortalExtraLinks,
  postZeroAmountCompensationExpect400,
  startGeneratingAndWaitGenerated,
};

export type Pdt3483Fx = Pdt3470Fx;

export const PDT_3483_KEY = 'PDT-3483';
export const PDT_3483_TITLE = 'Billing Run Compensation - Credit Note and Debit Note';
export const PDT_3483_TEST_TIMEOUT_MS = 55 * 60 * 1000;
export const PDT_3483_BE12_TIMEOUT_MS = 10 * 60 * 1000;
export const PDT_3483_EXCEL_T = 50;
/** Must exceed FOR_VOLUMES totalAmountIncludingVat on current DEV billing-by-profile (~34.5M). */
export const PDT_3483_IAP_EXACT_AMOUNT = 80_000_000;
export const PDT_3483_ZERO_AMOUNT_MESSAGE = 'documentAmount-[documentAmount] must not be zero;';

export async function buildPdt3470IapRealChain(
  fx: Pdt3483Fx,
  opts: { includeUnrelatedPod?: boolean } = {},
): Promise<Pdt3470IapReadyChain> {
  return buildPdt3470IapRealChainBase(fx, {
    ...opts,
    iapExactAmount: PDT_3483_IAP_EXACT_AMOUNT,
  });
}

export const PDT_3483_RELEVANT_ENTITY_KEYS = [
  'customer',
  'pod',
  'product',
  'productContract',
  'billingRun',
  'invoice',
  'compensation',
] as const;

export type GenerateInvoiceDataBody = {
  FinalLiabilityAmount?: number;
  FinalLiabilityAmountWithWords?: string;
  TotalInclVat?: number;
  SDCompensations?: unknown[];
  [key: string]: unknown;
};

export type Pdt3483VolumeCreditNote = {
  volumeBillingRunId: number;
  creditNoteInvoiceId: number;
  T: number;
};

export type Pdt3483DebitNoteDraft = {
  chain: DevVolCompPrechainResult;
  volumeBillingRunId: number;
  debitInvoiceId: number;
  T: number;
  invoiceDocumentType: string;
};

/**
 * Java-style HALF_UP to scale 2 (ties away from zero), matching
 * EPBDecimalUtils.convertToCurrencyScale.
 */
export function roundHalfUpScale2(value: number): number {
  if (!Number.isFinite(value)) {
    throw new Error(`Expected finite number for money HALF_UP, got ${JSON.stringify(value)}`);
  }
  const sign = value < 0 ? -1 : 1;
  const absStr = absNumberToDecimalString(Math.abs(value));
  const [wholeRaw, fracRaw = ''] = absStr.split('.');
  const whole = Number(wholeRaw);
  const centsPart = (fracRaw + '00').slice(0, 2);
  const rest = fracRaw.slice(2);
  let cents = whole * 100 + Number(centsPart);
  const third = rest.length ? Number(rest[0]) : 0;
  if (third >= 5) {
    cents += 1;
  }
  return (sign * cents) / 100;
}

function absNumberToDecimalString(abs: number): string {
  const s = abs.toString();
  if (s.includes('e') || s.includes('E')) {
    return abs.toFixed(12).replace(/0+$/, '').replace(/\.$/, '');
  }
  return s;
}

export function money(raw: unknown): number {
  const n = Number(raw);
  if (!Number.isFinite(n)) {
    throw new Error(`Expected numeric amount, got: ${JSON.stringify(raw)}`);
  }
  return roundHalfUpScale2(n);
}

/** Excel identity: CN → round(-T - C, 2); otherwise round(T - C, 2). */
export function expectedFla(docType: string, T: number, C: number): number {
  const signedBase = docType === 'CREDIT_NOTE' ? -Number(T) : Number(T);
  return roundHalfUpScale2(signedBase - Number(C));
}

export function scaleC(T: number, excelC: number, excelT = PDT_3483_EXCEL_T): number {
  return roundHalfUpScale2((Number(T) * excelC) / excelT);
}

export function assertFlaEquals(actual: unknown, expected: number, message: string): void {
  expect(
    actual === null || actual === undefined,
    `${message}: FinalLiabilityAmount must be present (not null/omitted)`,
  ).toBe(false);
  const actualNum = typeof actual === 'number' ? actual : Number(actual);
  expect(
    Number.isFinite(actualNum),
    `${message}: FinalLiabilityAmount must be numeric, got ${JSON.stringify(actual)}`,
  ).toBe(true);
  expect(money(actualNum), message).toBe(money(expected));
}

export function uniquePdt3483CompensationNumber(tcTag: string): string {
  return uniqueCompensationNumber(tcTag).replace(/^PDT3470-/, 'PDT3483-');
}

export async function getGenerateInvoiceData(
  Request: Pdt3483Fx['Request'],
  invoiceId: number,
): Promise<GenerateInvoiceDataBody> {
  const res = await Request.get('billing-run/generate-invoice-data', {
    params: { invoiceId },
  });
  await expect(res).CheckResponse();
  const body = (await res.json()) as GenerateInvoiceDataBody;
  expect(
    Object.prototype.hasOwnProperty.call(body, 'FinalLiabilityAmount'),
    'OpenAPI BillingRunDocumentModelImpl.FinalLiabilityAmount (PascalCase) is present',
  ).toBe(true);
  return body;
}

/**
 * TC-BE-1 extras on generate-invoice-data: TotalInclVat present, words from abs(FLA),
 * SDCompensations non-empty. Optional id/number match is recorded, not a hard fail
 * when the item uses other OpenAPI keys (DocumentNumber / Amount / …).
 */
export function assertFlaTemplaterExtras(
  flaBody: GenerateInvoiceDataBody,
  opts: {
    signedFla: unknown;
    compensationId?: number;
    compensationNumber?: string;
  },
): { sdFirstKeys: string[]; sdMatchNote: string } {
  expect(flaBody.TotalInclVat).toBeDefined();
  const wordsRaw = flaBody.FinalLiabilityAmountWithWords;
  expect(typeof wordsRaw, 'FinalLiabilityAmountWithWords is string').toBe('string');
  const words = String(wordsRaw).trim();
  expect(
    words.length,
    'FinalLiabilityAmountWithWords is non-empty (abs amount in words; do not hardcode Bulgarian)',
  ).toBeGreaterThan(0);
  const signed = Number(opts.signedFla);
  if (Number.isFinite(signed) && signed < 0) {
    expect(
      words.startsWith('-'),
      'FinalLiabilityAmountWithWords is from abs(FLA) and must not start with -',
    ).toBe(false);
  }

  expect(Array.isArray(flaBody.SDCompensations), 'SDCompensations is array').toBe(true);
  const sd = (flaBody.SDCompensations ?? []) as unknown[];
  expect(
    sd.length,
    'SDCompensations contains the linked compensation (government is not a second documentAmount addend)',
  ).toBeGreaterThan(0);

  const first = sd[0];
  let sdFirstKeys: string[] = [];
  let sdMatchNote = 'SDCompensations[0] is not an object; length>0 only';
  if (first && typeof first === 'object') {
    sdFirstKeys = Object.keys(first as object);
    expect(sdFirstKeys.length, 'SDCompensations[0] has at least one key').toBeGreaterThan(0);
    sdMatchNote = `keys=${sdFirstKeys.join(',')}`;
    const rec = first as Record<string, unknown>;
    const idCandidate = rec.id ?? rec.Id ?? rec.compensationId;
    const numberCandidate = rec.DocumentNumber ?? rec.number ?? rec.Number;
    if (opts.compensationId && idCandidate != null && Number(idCandidate) > 0) {
      sdMatchNote +=
        Number(idCandidate) === opts.compensationId
          ? '; id matched'
          : `; id present ${idCandidate} (not failing on schema)`;
    }
    if (
      opts.compensationNumber &&
      numberCandidate != null &&
      String(numberCandidate).trim() !== ''
    ) {
      sdMatchNote +=
        String(numberCandidate) === opts.compensationNumber
          ? '; number matched'
          : `; number/DocumentNumber present ${String(numberCandidate)} (not failing on schema)`;
    }
  }
  return { sdFirstKeys, sdMatchNote };
}

/**
 * FOR_VOLUMES draft + CREDIT_NOTE gate. Returns T without comparing T to C.
 * Do not call startPdt3470VolumeCreditNoteDraft for zero-FLA TCs (it asserts cnTotal !== compensationAmount).
 */
export async function startPdt3483VolumeCreditNoteDraft(
  fx: Pdt3483Fx,
  chain: Pdt3470IapReadyChain,
): Promise<Pdt3483VolumeCreditNote> {
  const { Request, Responses } = fx;

  const volumeBillingRunId = await test.step(
    'Action: FOR_VOLUMES billing run (maxEndDate present)',
    async () => {
      const id = await createStandardBillingRun(fx as ExpParity03Fx, ['FOR_VOLUMES'], chain.anchor);
      Responses.billingRun[Responses.billingRun.length - 1] = {
        id,
        invoiceNumbers: 1,
      };
      return id;
    },
  );

  const observation = await startBillingAndObserveDraft(Request, volumeBillingRunId, ['FOR_VOLUMES']);
  expect(observation.draftInvoiceIds.length, 'volume run produced a draft invoice').toBeGreaterThan(
    0,
  );
  const creditNoteInvoiceId = observation.draftInvoiceIds[0];
  if (!Responses.invoice.includes(creditNoteInvoiceId as never)) {
    Responses.invoice.push(creditNoteInvoiceId);
  }

  const invoice = await getInvoice(Request, creditNoteInvoiceId);
  expect(invoice.invoiceType, 'volume invoiceType is STANDARD').toBe('STANDARD');
  expect(
    invoice.invoiceDocumentType,
    'IAP must exceed the period invoice so the document is CREDIT_NOTE — reduce volumes or increase IAP',
  ).toBe('CREDIT_NOTE');
  expect(
    nestedId((invoice as InvoiceView & { customer?: unknown }).customer),
    'Credit Note customer is the billed customer',
  ).toBe(chain.billedCustomerId);

  const T = money(invoice.totalAmountIncludingVat);
  return { volumeBillingRunId, creditNoteInvoiceId, T };
}

export async function startPdt3483DebitNoteDraft(
  fx: Pdt3483Fx,
  opts?: {
    accountingPeriodId?: number;
    invoiceDate?: string;
    taxEventDate?: string;
    podActivationDate?: string;
    profileDateRanges?: Array<{ startDate: string; endDate: string; amount: number }>;
    profilePeriodFrom?: string;
    profilePeriodTo?: string;
  },
): Promise<Pdt3483DebitNoteDraft> {
  const chain = await test.step(
    'Precondition: FOR_VOLUMES contract + BBP without IAP (no interim deduction)',
    async () =>
      runDevVolCompContractAndBbpPrechain(fx as Pdt3087Fx, {
        podActivationDate: opts?.podActivationDate,
        profileDateRanges: opts?.profileDateRanges,
        profilePeriodFrom: opts?.profilePeriodFrom,
        profilePeriodTo: opts?.profilePeriodTo,
      }),
  );

  const volume = await test.step(
    'Action: FOR_VOLUMES billing run to DRAFT',
    async () =>
      createForVolumesDraftBillingRun(fx as Pdt3087Fx, {
        accountingPeriodId: opts?.accountingPeriodId,
        invoiceDate: opts?.invoiceDate,
        taxEventDate: opts?.taxEventDate,
      }),
  );
  expect(volume.draftInvoiceIds.length, 'volume run produced a draft invoice').toBeGreaterThan(0);
  const debitInvoiceId = volume.draftInvoiceIds[0];
  if (!fx.Responses.invoice.includes(debitInvoiceId as never)) {
    fx.Responses.invoice.push(debitInvoiceId);
  }

  const invoice = await getInvoice(fx.Request, debitInvoiceId);
  expect(invoice.invoiceType, 'volume invoiceType is STANDARD').toBe('STANDARD');
  expect(invoice.invoiceType, 'do not use MANUAL_CREDIT_OR_DEBIT_NOTE for debit FLA').not.toBe(
    'MANUAL',
  );
  const invoiceDocumentType = String(invoice.invoiceDocumentType ?? '');
  expect(
    invoiceDocumentType,
    'without IAP deduction the period document must not be CREDIT_NOTE; INVOICE (liability) or DEBIT_NOTE is the debit case',
  ).not.toBe('CREDIT_NOTE');
  expect(
    ['INVOICE', 'DEBIT_NOTE'].includes(invoiceDocumentType),
    `debit case invoiceDocumentType must be INVOICE or DEBIT_NOTE, got ${invoiceDocumentType}`,
  ).toBe(true);
  expect(
    nestedId((invoice as InvoiceView & { customer?: unknown }).customer),
    'debit-case invoice customer is the billed customer',
  ).toBe(chain.customerId);

  const T = money(invoice.totalAmountIncludingVat);
  expect(T, 'debit-case totalAmountIncludingVat').toBeGreaterThan(0);
  return {
    chain,
    volumeBillingRunId: volume.billingRunId,
    debitInvoiceId,
    T,
    invoiceDocumentType,
  };
}

export async function assertLinkedInvoiceIsDebitNote(
  Request: Pdt3483Fx['Request'],
  compensationId: number,
): Promise<{ linkedInvoiceId: number; invoice: InvoiceView; T: number; compensation: CompensationView }> {
  const compensation = await getCompensation(Request, compensationId);
  const linkedInvoiceId = nestedId(compensation.invoice);
  expect(linkedInvoiceId, 'compensation.invoice.id after PDF').toBeGreaterThan(0);
  const invoice = await getInvoice(Request, linkedInvoiceId as number);
  const docType = String(invoice.invoiceDocumentType ?? '');
  expect(
    docType,
    `debit case: linked invoice ${linkedInvoiceId} invoiceDocumentType=${docType}; expected INVOICE (liability, no IAP) or DEBIT_NOTE, not CREDIT_NOTE. Do not use MANUAL_CREDIT_OR_DEBIT_NOTE.`,
  ).not.toBe('CREDIT_NOTE');
  expect(
    ['INVOICE', 'DEBIT_NOTE'].includes(docType),
    `debit case linked invoiceDocumentType must be INVOICE or DEBIT_NOTE, got ${docType}`,
  ).toBe(true);
  return {
    linkedInvoiceId: linkedInvoiceId as number,
    invoice,
    T: money(invoice.totalAmountIncludingVat),
    compensation,
  };
}

export async function postPdt3483SignedCompensation(
  fx: Pdt3483Fx,
  opts: {
    customerId: number;
    podId: number;
    recipientId: number;
    documentPeriod: string;
    documentAmount: number;
    number: string;
    reason: string;
  },
): Promise<{ id: number; number: string }> {
  return createPdt3470Compensation(fx, {
    customerId: opts.customerId,
    podId: opts.podId,
    recipientId: opts.recipientId,
    documentPeriod: opts.documentPeriod,
    documentAmount: opts.documentAmount,
    volumes: Math.abs(opts.documentAmount),
    price: 1,
    number: opts.number,
    reason: opts.reason,
  });
}

export function assertFlaNotForbidden(
  actual: unknown,
  forbidden: number[],
  message: string,
  expected?: number,
): void {
  const actualNum = money(actual);
  for (const bad of forbidden) {
    if (expected !== undefined && money(bad) === money(expected)) {
      continue;
    }
    expect(actualNum, `${message}: forbidden FinalLiabilityAmount ${money(bad)}`).not.toBe(
      money(bad),
    );
  }
}
