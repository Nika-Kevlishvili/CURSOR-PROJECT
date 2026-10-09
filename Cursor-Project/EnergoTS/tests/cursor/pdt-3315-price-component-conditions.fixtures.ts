/**
 * PDT-3315 — Price component conditions with OR / in(...) on months-difference.
 *
 * Bug: after `$VAR$` replace, months replacement_text lacks a trailing space → SQL
 * becomes `=6OR` / `diffin(` → `pc_condition_passed=-3` (PC dropped from invoice).
 *
 * Conditions (exact token format from Test PCs 12648/12662):
 * - Control: months `=6` only (baseline — should pass)
 * - OR / IN: same contract/voltage filters + months 6/12/18(/24)
 *
 * Months-diff calendar (Dev formula: same-month→1 else AGE months+1):
 * activation = first day of (billing maxEndDate month − 5 months) so months-diff = 6.
 * Billing-by-profile (ONE_MONTH): profileStart/profileEnd = first→last day of the
 * invoice / maxEndDate month (same year+month; periodFrom = 1st). Billing-run
 * maxEndDate / invoicePeriodTo stay the OPEN period end.
 *
 * Environment: Dev (OPEN accounting period resolved at runtime)
 * Swagger: Cursor-Project/config/swagger/dev/swagger-spec.json
 *   POST /price-components → PriceComponentRequest.formulaRequest.condition (string)
 *
 * Reference:
 * - tests/cursor/pdt-3113-incorrect-interim-generation.fixtures.ts
 * - tests/cursor/rps-pod-invoice-due-date.fixtures.ts
 * - tests/cursor/pdt-2599-service-contract.fixtures.ts (invoice/detailed-data)
 */

import { test, expect } from './cursor-test.fixtures';
import type { baseFixture } from './cursor-test.fixtures';
import {
  applyVolumeBillingAnchor,
  profileDateRangeFromAnchor,
} from './PDT-2529-rfd-data-model-happy-path.fixtures';
import type { Pdt2376BillingAnchor } from './pdt-2376-volume-with-electricity.fixtures';
import { asBillingRunId } from './pdt-2599-service-contract.fixtures';
import {
  fetchPdt2599InvoiceDetailedRows,
  type Pdt2599InvoiceDetailedDataRow,
} from './pdt-2599-service-contract.fixtures';

export const PDT_3315_JIRA_KEY = 'PDT-3315';
/** Exact Jira summary. */
export const PDT_3315_JIRA_TITLE = 'Price component conditions problem';

/** Distinctive expressions so invoice lines are identifiable even if displayName truncates. */
export const PDT_3315_EXPR_CONTROL = 10;
export const PDT_3315_EXPR_OR = 1111;
export const PDT_3315_EXPR_IN = 2222;

export const PDT_3315_VOLUME = 100;
export const PDT_3315_TARGET_MONTHS_DIFF = 6;

const MONTHS_TOKEN =
  '$MONTHS_DIFFERENCE_BETWEEN_MAX_DATE_OF_BILLING_RUN_CONTRACT_ACTIVATION_DATE$';

/** Exact OR condition from Test PC 12648 (no spaces around OR operands). */
export const PDT_3315_CONDITION_OR =
  `$CONTRACT_TYPE$=$COMBINED$AND$POD_VOLTAGE_LEVEL$<>$HIGH$AND${MONTHS_TOKEN}=6OR${MONTHS_TOKEN}=12OR${MONTHS_TOKEN}=18`;

/** Exact IN condition from Test PC 12662. */
export const PDT_3315_CONDITION_IN =
  `$CONTRACT_TYPE$=$COMBINED$AND$POD_VOLTAGE_LEVEL$<>$HIGH$AND${MONTHS_TOKEN}in(6,12,18,24)`;

/** Control: same filters + single months equality (parent Dev proof → pc_condition_passed=1). */
export const PDT_3315_CONDITION_CONTROL =
  `$CONTRACT_TYPE$=$COMBINED$AND$POD_VOLTAGE_LEVEL$<>$HIGH$AND${MONTHS_TOKEN}=6`;

export type Pdt3315Fx = Pick<
  baseFixture,
  'Request' | 'GeneratePayload' | 'Responses' | 'Endpoints'
>;

export type Pdt3315OpenPeriod = {
  id: number;
  startDate: string;
  endDate: string;
};

export type Pdt3315Calendar = {
  anchor: Pdt2376BillingAnchor;
  /** POD / contract activation so months-diff vs maxEndDate = 6. */
  activationDate: string;
  openPeriod: Pdt3315OpenPeriod;
};

export type Pdt3315CreatedPc = {
  role: 'control' | 'or' | 'in';
  id: number;
  name: string;
  displayName: string;
  expression: number;
  condition: string;
};

export type Pdt3315ScenarioResult = {
  calendar: Pdt3315Calendar;
  isolationToken: string;
  priceComponents: Pdt3315CreatedPc[];
  contractId: number;
  productId: number;
  podId: number;
  billingRunId: number;
  invoiceId: number;
  detailedRows: Pdt2599InvoiceDetailedDataRow[];
};

type OpenApRow = {
  id?: number;
  startDate?: string;
  endDate?: string;
  status?: string;
};

function ymd(iso?: string): string {
  return (iso ?? '').slice(0, 10);
}

function addUtcMonths(dateYmd: string, months: number): string {
  const d = new Date(`${dateYmd}T00:00:00.000Z`);
  d.setUTCMonth(d.getUTCMonth() + months);
  return d.toISOString().slice(0, 10);
}

function firstOfMonth(dateYmd: string): string {
  return `${dateYmd.slice(0, 7)}-01`;
}

/** Last calendar day of the month that contains `dateYmd` (UTC). */
function lastOfMonth(dateYmd: string): string {
  const d = new Date(`${firstOfMonth(dateYmd)}T00:00:00.000Z`);
  d.setUTCMonth(d.getUTCMonth() + 1, 0);
  return d.toISOString().slice(0, 10);
}

/**
 * ONE_MONTH billing-by-profile window: same year+month, periodFrom = 1st.
 * Prefer the month of `endDateYmd` (invoice / maxEndDate). Use period end as
 * periodTo when it is already the last day of that month.
 * Example: OPEN 2026-08-31→2026-09-30 → 2026-09-01→2026-09-30.
 */
function billingByProfileWindowFromPeriodEnd(endDateYmd: string): {
  profileStart: string;
  profileEnd: string;
} {
  const profileStart = firstOfMonth(endDateYmd);
  const lastOfProfileMonth = lastOfMonth(profileStart);
  const end = ymd(endDateYmd);
  const profileEnd =
    end.slice(0, 7) === profileStart.slice(0, 7) && end === lastOfProfileMonth
      ? end
      : lastOfProfileMonth;
  return { profileStart, profileEnd };
}

export function entityId(entry: unknown): number {
  if (typeof entry === 'number' && Number.isFinite(entry) && entry > 0) return entry;
  if (entry !== null && typeof entry === 'object') {
    if ('id' in entry) {
      const id = Number((entry as { id: unknown }).id);
      if (Number.isFinite(id) && id > 0) return id;
    }
    const bpId = Number((entry as { basicParameters?: { id?: unknown } }).basicParameters?.id);
    if (Number.isFinite(bpId) && bpId > 0) return bpId;
  }
  throw new Error(`[PDT-3315] Cannot resolve entity id from: ${JSON.stringify(entry).slice(0, 200)}`);
}

export function buildPdt3315IsolationToken(): string {
  return Math.random().toString(36).slice(2, 8).toUpperCase();
}

/**
 * AGE(max, activation) months + 1 = target when not same month.
 * activation = first day of (maxEndDate month − (target − 1) months).
 * Example: max 2026-09-01, target 6 → activation 2026-04-01 (parent Dev proof).
 */
export function activationDateForMonthsDiff(
  maxEndDateYmd: string,
  targetMonths = PDT_3315_TARGET_MONTHS_DIFF,
): string {
  const monthsBack = targetMonths - 1;
  return firstOfMonth(addUtcMonths(firstOfMonth(ymd(maxEndDateYmd)), -monthsBack));
}

/**
 * Latest OPEN accounting period from available-list → billing anchor + activation for months=6.
 */
export async function resolvePdt3315BillingCalendar(
  Request: Pdt3315Fx['Request'],
): Promise<Pdt3315Calendar> {
  const listRes = await Request.get(
    'billing-run/accounting-period-available-list?page=0&size=50&direction=DESC',
  );
  await expect(listRes).CheckResponse();
  const page = (await listRes.json()) as { content?: OpenApRow[] };
  const open = (page.content ?? [])
    .filter(
      (r) =>
        String(r.status).toUpperCase() === 'OPEN' &&
        r.id != null &&
        ymd(r.startDate) &&
        ymd(r.endDate),
    )
    .sort((a, b) => ymd(a.startDate).localeCompare(ymd(b.startDate)));

  const period = open.at(-1);
  if (!period) {
    throw new Error(
      '[PDT-3315] Need at least one OPEN accounting period. OPEN now: (none). Open a period on Dev, then rerun.',
    );
  }

  const start = ymd(period.startDate);
  const end = ymd(period.endDate);
  const id = Number(period.id);
  // maxEndDate / invoicePeriodTo = period end (applyVolumeBillingAnchor → basicParameters.maxEndDate).
  // Activation stays first-of-month of (maxEnd month − 5) so AGE months+1 = 6.
  const maxForDiff = end;
  const activationDate = activationDateForMonthsDiff(maxForDiff, PDT_3315_TARGET_MONTHS_DIFF);
  // profileDateRangeFromAnchor → POST /billing-by-profile periodFrom/periodTo.
  // Do not use OPEN start/end across months (e.g. 2026-08-31→2026-09-30 → 400).
  const { profileStart, profileEnd } = billingByProfileWindowFromPeriodEnd(end);

  if (activationDate > start) {
    throw new Error(
      `[PDT-3315] Computed activation ${activationDate} is after period start ${start}. ` +
        `Need an OPEN period at least ${PDT_3315_TARGET_MONTHS_DIFF} months after a valid activation window.`,
    );
  }

  return {
    openPeriod: { id, startDate: start, endDate: end },
    activationDate,
    anchor: {
      invoicePeriodTo: end,
      taxEventDate: end,
      invoiceDate: end,
      accountingPeriodId: id,
      profileStart,
      profileEnd,
      commentary:
        `OPEN ap=${id} ${start}→${end}; profile=${profileStart}→${profileEnd}; ` +
        `activation=${activationDate} for months-diff=${PDT_3315_TARGET_MONTHS_DIFF} ` +
        `(AGE months+1 vs maxEndDate=${end}).`,
    },
  };
}

function buildVolumePcPayload(
  GeneratePayload: Pdt3315Fx['GeneratePayload'],
  opts: {
    name: string;
    displayName: string;
    expression: number;
    condition: string;
  },
) {
  const payload = GeneratePayload.productAndServices.priceSettlement();
  payload.name = opts.name;
  payload.displayName = opts.displayName;
  payload.formulaRequest.expression = String(opts.expression);
  payload.formulaRequest.condition = opts.condition;
  payload.formulaRequest.issuedSeparateInvoice = 'INVOICE_ONE';
  return payload;
}

export async function createPdt3315VolumePriceComponents(
  fx: Pdt3315Fx,
  isolationToken: string,
): Promise<Pdt3315CreatedPc[]> {
  const { Request, GeneratePayload, Responses, Endpoints } = fx;
  const specs: Array<Omit<Pdt3315CreatedPc, 'id'>> = [
    {
      role: 'control',
      name: `CTRL-PDT3315-${isolationToken}`,
      displayName: `CTRL-PDT3315-${isolationToken}`,
      expression: PDT_3315_EXPR_CONTROL,
      condition: PDT_3315_CONDITION_CONTROL,
    },
    {
      role: 'or',
      name: `OR-PDT3315-${isolationToken}`,
      displayName: `OR-PDT3315-${isolationToken}`,
      expression: PDT_3315_EXPR_OR,
      condition: PDT_3315_CONDITION_OR,
    },
    {
      role: 'in',
      name: `IN-PDT3315-${isolationToken}`,
      displayName: `IN-PDT3315-${isolationToken}`,
      expression: PDT_3315_EXPR_IN,
      condition: PDT_3315_CONDITION_IN,
    },
  ];

  const created: Pdt3315CreatedPc[] = [];
  for (const spec of specs) {
    const payload = buildVolumePcPayload(GeneratePayload, spec);
    const res = await Request.post(Endpoints.priceComponent, { data: payload });
    await expect(res).CheckResponse();
    const body = await res.json();
    Responses.priceComponent.push(body);
    created.push({ ...spec, id: entityId(body) });
  }
  return created;
}

export async function postPdt3315BillingByProfile(
  fx: Pdt3315Fx,
  podIndex: number,
  anchor: Pdt2376BillingAnchor,
  volumeValue: number,
): Promise<void> {
  const { Request, GeneratePayload, Responses } = fx;
  const dateRanges = profileDateRangeFromAnchor(anchor);
  const payload = await GeneratePayload.energyData.profile1Month(podIndex, dateRanges);
  payload.timeZone = 'CET';
  payload.entries[0].value = volumeValue;
  const profiles = await Request.post('billing-by-profile', { data: payload });
  await expect(profiles).CheckResponse();
  const profileData = await profiles.json();
  Responses.dataByProfiles.push({
    id: profileData,
    periodFrom: payload.periodFrom,
    periodTo: payload.periodTo,
    periodType: payload.periodType,
  });
}

/**
 * Full prechain: term → 3 conditioned volume PCs → COMBINED product → customer → LOW POD →
 * COMBINED contract → activate (months=6) → BBP → FOR_VOLUMES to DRAFT → detailed-data.
 */
export async function runPdt3315ConditionReproScenario(
  fx: Pdt3315Fx,
): Promise<Pdt3315ScenarioResult> {
  const { Request, GeneratePayload, Responses, Endpoints } = fx;
  const isolationToken = buildPdt3315IsolationToken();

  const calendar = await test.step(
    'Precondition: resolve OPEN accounting period + activation for months-diff=6',
    async () => resolvePdt3315BillingCalendar(Request),
  );
  console.log(`[PDT-3315] ${calendar.anchor.commentary}`);

  await test.step('Precondition: term', async () => {
    const term = await Request.post(Endpoints.terms, {
      data: GeneratePayload.productAndServices.term(),
    });
    await expect(term).CheckResponse();
    Responses.terms.push(await term.json());
  });

  const priceComponents = await test.step(
    'Precondition: 3 volume PCs (control =6, OR, IN) with exact condition tokens',
    async () => createPdt3315VolumePriceComponents(fx, isolationToken),
  );

  let productId = 0;
  await test.step('Precondition: product (COMBINED, voltage LOW, all 3 PCs)', async () => {
    const payload = GeneratePayload.productAndServices.product() as Record<string, unknown>;
    payload.contractTypes = ['COMBINED'];
    payload.paymentGuarantees = ['NO'];
    payload.voltageLevels = ['LOW'];
    payload.typePointsOfDelivery = ['CONSUMER'];
    payload.purposeOfConsumptions = ['NON_HOUSEHOLD'];
    payload.meteringTypeOfThePointOfDeliveries = ['SETTLEMENT_PERIOD'];
    payload.priceComponentIds = priceComponents.map((pc) => pc.id);
    payload.interimAdvancePayments = [];
    payload.interimAdvancePaymentGroups = [];
    const product = await Request.post(Endpoints.product, { data: payload });
    await expect(product).CheckResponse();
    const body = await product.json();
    Responses.product.push(body);
    productId = entityId(body);
  });

  await test.step('Precondition: customer', async () => {
    const customer = await Request.post(Endpoints.customer, {
      data: GeneratePayload.customers.customer_legal(),
    });
    await expect(customer).CheckResponse();
    Responses.customer.push(await customer.json());
  });

  let podId = 0;
  await test.step('Precondition: POD (voltage LOW — not HIGH)', async () => {
    const podPayload = GeneratePayload.pointsOfDelivery.pod_settlement() as Record<string, unknown>;
    podPayload.voltageLevel = 'LOW';
    const pod = await Request.post(Endpoints.pod, { data: podPayload });
    await expect(pod).CheckResponse();
    const body = await pod.json();
    Responses.pod.push(body);
    podId = entityId(body);
  });

  let contractId = 0;
  await test.step('Precondition: product contract COMBINED', async () => {
    const contractPayload = await GeneratePayload.contractsAndOrders.product_contract(0, 0, 0);
    (contractPayload.productParameters as Record<string, unknown>).contractType = 'COMBINED';
    const contract = await Request.post(Endpoints.productContract, { data: contractPayload });
    await expect(contract).CheckResponse();
    const body = await contract.json();
    Responses.productContract.push(body);
    contractId = entityId(body);
  });

  await test.step(
    `Precondition: activate POD (${calendar.activationDate}) for months-diff=6`,
    async () => {
      const act = await Request.post('/contract-pods/manual', {
        data: await GeneratePayload.pointsOfDelivery.pod_activation(
          0,
          calendar.activationDate,
          undefined,
          0,
        ),
      });
      await expect(act).CheckResponse();
    },
  );

  await test.step(
    `Precondition: billing-by-profile volume=${PDT_3315_VOLUME} (${calendar.anchor.profileStart}→${calendar.anchor.profileEnd})`,
    async () => postPdt3315BillingByProfile(fx, 0, calendar.anchor, PDT_3315_VOLUME),
  );

  let billingRunId = 0;
  let invoiceId = 0;
  await test.step('Action: FOR_VOLUMES STANDARD billing → DRAFT invoices', async () => {
    const billingPayload = await GeneratePayload.billing.billingRun(
      'CONTRACT',
      ['FOR_VOLUMES'],
      0,
      0,
    );
    applyVolumeBillingAnchor(billingPayload, calendar.anchor);
    const billingRun = await Request.post(Endpoints.billingRun, { data: billingPayload });
    await expect(billingRun).CheckResponse();
    billingRunId = asBillingRunId(await billingRun.json());
    Responses.billingRun.push({ id: billingRunId, invoiceNumbers: 1 });

    const billingIndex = Responses.billingRun.length - 1;
    // completeBillingRun=false → stop at DRAFT; draft invoice ids pushed to Responses.invoice
    await GeneratePayload.billing.waitForInvoiceGeneration(true, false, 1, billingIndex);

    invoiceId = Number(Responses.invoice[Responses.invoice.length - 1]);
    expect(invoiceId, 'Draft invoice id after FOR_VOLUMES').toBeGreaterThan(0);
  });

  const detailedRows = await test.step(
    'Action: GET invoice/detailed-data for draft invoice',
    async () => fetchPdt2599InvoiceDetailedRows(Request, invoiceId),
  );

  return {
    calendar,
    isolationToken,
    priceComponents,
    contractId,
    productId,
    podId,
    billingRunId,
    invoiceId,
    detailedRows,
  };
}

export function rowsMatchingPcLabel(
  rows: Pdt2599InvoiceDetailedDataRow[],
  label: string,
): Pdt2599InvoiceDetailedDataRow[] {
  const needle = label.toLowerCase();
  return rows.filter((row) => String(row.priceComponent ?? '').toLowerCase().includes(needle));
}

export function summarizePdt3315DetailedRows(
  rows: Pdt2599InvoiceDetailedDataRow[],
): Array<{ priceComponent?: string; value?: number; unitPrice?: number }> {
  return rows.map((row) => ({
    priceComponent: row.priceComponent == null ? undefined : String(row.priceComponent),
    value: row.value == null ? undefined : Number(row.value),
    unitPrice: row.unitPrice == null ? undefined : Number(row.unitPrice),
  }));
}

/** True when detailed-data has a row for the PC label (name/displayName). */
export function invoiceIncludesPriceComponent(
  rows: Pdt2599InvoiceDetailedDataRow[],
  pc: Pick<Pdt3315CreatedPc, 'name' | 'displayName'>,
): boolean {
  return (
    rowsMatchingPcLabel(rows, pc.displayName).length > 0 ||
    rowsMatchingPcLabel(rows, pc.name).length > 0
  );
}
