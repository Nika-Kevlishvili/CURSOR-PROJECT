/**
 * PDT-3445 helpers — Create listing preselect rule, data created in the test.
 *
 * No environment ids. The chain uses whatever BASE_URL setup wrote into
 * envVariables (grid operator, tax). Same steps on dev, dev2, test, preprod,
 * prod, and experiments.
 *
 * Product rule (create SQL findTableByGridOperatorId):
 *   checked and unableToUncheck are true only when liability_amount = 0
 *   AND disconnected (pod.disconnected AND the RFD pod is checked).
 * The listing itself also requires an executed disconnection date after the
 * last executed reconnection. A paid POD that was never disconnected therefore
 * must not come back preselected. After executed DPS the same paid POD must
 * be preselected and locked.
 *
 * Swagger (test spec, ReconnectionTableListingRequest / CreateReconnectionTableResponse):
 * - GET /reconnection-of-the-power-supply/table
 *   required: gridOperatorId
 *   searchBy: CUSTOMER_IDENTIFIER | CUSTOMER_NUMBER | POD_IDENTIFIER |
 *             DISCONNECTION_REQUEST_NUMBER | ALL
 *   row fields used: podId, podIdentifier, checked, unableToUncheck
 *
 * Reference spec(s):
 * - tests/cursor/PDT-3179-reconnection-invoice-emails.spec.ts
 * - tests/cursor/pdt-3179-reconnection-invoice-emails.fixtures.ts
 * - tests/cursor/PDT-3409-reconnection-draft-disappearing-pods.spec.ts
 * - tests/cursor/pdt-3042-cancellation-pod-auto-check.fixtures.ts
 */

import { expect } from './cursor-test.fixtures';
import { envVariables } from '../../fixtures/envCashed';
import { payPdt3042Amount } from './pdt-3042-cancellation-pod-auto-check.fixtures';
import {
  asNumber,
  createExecutedDps,
  createExecutedRfd,
  customerIdentifier,
  entityId,
  getLiability,
  listLiabilities,
  nestedId,
  runPdt3179ReceivableChain,
  type Pdt3179Fx,
} from './pdt-3179-reconnection-invoice-emails.fixtures';
import {
  findPodTableRow,
  getReconnectionTable,
  listingRows,
  resolveGridOperatorIdForPod,
  resolvePodIdentifierForPod,
  rowChecked,
} from './pdt-3409-reconnection-draft-disappearing-pods.fixtures';

export const PDT_3445_KEY = 'PDT-3445';
/** Exact Jira summary. */
export const PDT_3445_TITLE =
  '[Backend] - Reconnection of the power supply - After Draft save, more PODs are checked than the user selected';

export type Pdt3445Fx = Pdt3179Fx;

export type Pdt3445CreatedPod = {
  podId: number;
  podIdentifier: string;
  customerId: number;
  customerIdentifier: string;
  gridOperatorId: number;
  rfdId: number;
  liabilityId: number;
  remainingAfterPay: number;
};

export function pdt3445RelevantKeys(): Array<
  | 'customer'
  | 'pod'
  | 'product'
  | 'productContract'
  | 'customerLiability'
  | 'requestForDisconnection'
  | 'disconnectionOfPowerSupply'
  | 'payment'
> {
  return [
    'customer',
    'pod',
    'product',
    'productContract',
    'customerLiability',
    'requestForDisconnection',
    'disconnectionOfPowerSupply',
    'payment',
  ];
}

export function createListingQuery(
  created: Pick<Pdt3445CreatedPod, 'gridOperatorId' | 'podIdentifier'>,
): Record<string, string | number> {
  return {
    gridOperatorId: created.gridOperatorId,
    page: 0,
    pageSize: 25,
    searchBy: 'POD_IDENTIFIER',
    prompt: created.podIdentifier,
  };
}

/**
 * Customer, contract, POD (created disconnected=false), overdue liability,
 * executed reminder, executed RFD, then pay that liability to 0.
 * Does not execute disconnection of power supply.
 */
export async function createPaidNotDisconnectedRfdPod(fx: Pdt3445Fx): Promise<Pdt3445CreatedPod> {
  await runPdt3179ReceivableChain(fx);
  const rfdId = await createExecutedRfd(fx, { status: 'EXECUTED' });

  const customer = fx.Responses.customer[0];
  const pod = fx.Responses.pod[0];
  const podId = entityId(pod);
  const customerId = entityId(customer);
  const liabilityId = entityId(fx.Responses.customerLiability[0]);
  const podIdentifier = await resolvePodIdentifierForPod(fx, pod);
  const gridOperatorId = await resolveGridOperatorIdForPod(fx, pod);
  const ident = customerIdentifier(fx.Responses);
  expect(ident.length, 'created customer identifier').toBeGreaterThan(0);

  const remainingAfterPay = await payLiabilityFully(fx, liabilityId);

  return {
    podId,
    podIdentifier,
    customerId,
    customerIdentifier: ident,
    gridOperatorId,
    rfdId,
    liabilityId,
    remainingAfterPay,
  };
}

export async function payLiabilityFully(fx: Pdt3445Fx, liabilityId: number): Promise<number> {
  const before = await getLiability(fx, liabilityId);
  const current = asNumber(before.currentAmount);
  expect(current, `liability ${liabilityId} currentAmount before payment`).toBeGreaterThan(0);
  const invoiceResponse = before.invoiceResponse as { id?: number } | null | undefined;
  const invoiceId = invoiceResponse?.id != null ? Number(invoiceResponse.id) : undefined;
  await payPdt3042Amount(fx, current, invoiceId && invoiceId > 0 ? invoiceId : undefined);
  const after = await getLiability(fx, liabilityId);
  const remaining = asNumber(after.currentAmount);
  expect(remaining, `liability ${liabilityId} currentAmount after payment`).toBeCloseTo(0, 2);
  return remaining;
}

/**
 * Disconnection tax inserts another liability on the same RFD pod
 * (DisconnectionOfPowerSupplyService links it before the invoice is issued).
 * Create listing sums those current amounts, so the POD stays unchecked until
 * every linked liability is 0.
 */
export async function payOpenCustomerLiabilities(
  fx: Pdt3445Fx,
  customerIdentifierValue: string,
): Promise<number[]> {
  const rows = await listLiabilities(fx, customerIdentifierValue);
  const paid: number[] = [];
  for (const row of rows) {
    const id = nestedId(row.id) ?? asNumber(row.id);
    if (!id) continue;
    const full = await getLiability(fx, id);
    if (asNumber(full.currentAmount) <= 0) continue;
    await payLiabilityFully(fx, id);
    paid.push(id);
  }
  return paid;
}

/** Executed DPS sets pod.disconnected and writes the disconnection date the listing requires. */
export async function executeDisconnectionForCreatedPod(
  fx: Pdt3445Fx,
  rfdId: number,
): Promise<{ dpsId: number; status: number }> {
  const taxId = asNumber(envVariables.taxes_for_grid_operator);
  expect(
    taxId,
    'envVariables.taxes_for_grid_operator is required (written by setup for the current BASE_URL)',
  ).toBeGreaterThan(0);
  const dps = await createExecutedDps(fx, { taxId, express: false, requestId: rfdId });
  expect(
    dps.status,
    `EXECUTED disconnection of power supply HTTP ${dps.status} ${dps.text.slice(0, 400)}`,
  ).toBeGreaterThanOrEqual(200);
  expect(dps.status).toBeLessThan(300);
  expect(dps.id, 'disconnection of power supply id').toBeTruthy();
  return { dpsId: dps.id as number, status: dps.status };
}

export async function loadCreateListingRow(
  fx: Pdt3445Fx,
  tablePath: string,
  created: Pdt3445CreatedPod,
): Promise<{
  query: Record<string, string | number>;
  status: number;
  row: Record<string, unknown> | undefined;
  rowCount: number;
}> {
  const query = createListingQuery(created);
  const listing = await getReconnectionTable(fx, tablePath, query);
  const rows = listingRows(listing.body);
  return {
    query,
    status: listing.status,
    rowCount: rows.length,
    row: findPodTableRow(rows, { podId: created.podId, podIdentifier: created.podIdentifier }),
  };
}

/**
 * Paid POD, no executed disconnection. Missing row is success: the listing
 * requires a disconnection date, so the POD is not preselected. A present row
 * with checked or unableToUncheck is the bug.
 */
export function notDisconnectedPaidMustNotBePreselected(row: Record<string, unknown> | undefined): {
  passed: boolean;
  checked: boolean | undefined;
  actualResult: string;
} {
  const checked = rowChecked(row);
  if (!row) {
    return {
      passed: true,
      checked,
      actualResult:
        'As expected — paid POD with no executed disconnection is not on the Create listing, so it is not preselected.',
    };
  }
  const locked = row.unableToUncheck === true;
  if (checked === true || locked) {
    return {
      passed: false,
      checked,
      actualResult:
        `Not as expected — POD is not disconnected but Create listing returned checked=${String(checked)}, ` +
        `unableToUncheck=${String(row.unableToUncheck)}.`,
    };
  }
  return {
    passed: true,
    checked,
    actualResult:
      `As expected — POD is on the Create listing and is not preselected ` +
      `(checked=${String(checked)}, unableToUncheck=${String(row.unableToUncheck)}).`,
  };
}

/** After executed DPS and liability 0, the same POD must be checked and locked. */
export function disconnectedPaidMustBePreselected(row: Record<string, unknown> | undefined): {
  passed: boolean;
  checked: boolean | undefined;
  actualResult: string;
} {
  const checked = rowChecked(row);
  if (!row) {
    return {
      passed: false,
      checked,
      actualResult:
        'Not as expected — disconnected paid POD is missing from the Create listing.',
    };
  }
  const locked = row.unableToUncheck === true;
  if (checked === true && locked) {
    return {
      passed: true,
      checked,
      actualResult: 'As expected — checked=true and unableToUncheck=true.',
    };
  }
  return {
    passed: false,
    checked,
    actualResult:
      `Not as expected — checked=${String(checked)}, unableToUncheck=${String(row.unableToUncheck)}. ` +
      'A disconnected POD with liability 0 must be preselected and locked.',
  };
}

export function rowSnapshot(row: Record<string, unknown> | undefined): Record<string, unknown> | null {
  if (!row) return null;
  return {
    podId: row.podId,
    podIdentifier: row.podIdentifier,
    checked: row.checked,
    unableToUncheck: row.unableToUncheck,
    requestForDisconnectionId: nestedId(row.requestForDisconnectionId) ?? row.requestForDisconnectionId,
  };
}
