/**
 * PDT-3169 — Cancellation of request for disc - Error.
 *
 * Bug-only / standalone (no test_cases/*.md). Exact Jira title in test().
 *
 * Target env: Dev2
 * Canonical OpenAPI: Cursor-Project/config/swagger/dev2/swagger-spec.json
 * Runtime BASE_URL: http://10.236.20.11:8092
 *   (also public https://devapps.energo-pro.bg/backend/phoenix-dev2)
 * Playwright default config is NOT Dev2 — run with:
 *   BASE_URL=http://10.236.20.11:8092 npx playwright test tests/cursor/PDT-3169-cancellation-pagination-current-page-payload.spec.ts
 *
 * Swagger refresh: parent session already ran update-swagger-specs.ps1 for ALL envs
 * (Dev2 from http://10.236.20.11:8092/v3/api-docs). Do not re-download here.
 *
 * Jira reproduce (API equivalent of UI pagination + UIC filter):
 * Create Cancellation; table page size 1 so auto-checked POD A is on another page;
 * filter CUSTOMER_IDENTIFIER = Customer B; POST table with only the visible (B) row.
 * Actual (old FE): HTTP 400 APPLICATION_ERROR / ClientException: locked POD A
 *   "can't be unchecked".
 * Expected (complete payload / FE fix): POST table = [locked POD A + selected POD B]
 *   saveAs=DRAFT → HTTP 2xx.
 *
 * Do NOT hardcode TEST Request-1329 / UIC 148143678 / Dev2 Request-1825.
 *
 * Swagger DTO fields used:
 * - DisconnectionOfPowerSupplyGetDraftTableRequest: page, size, requestForDisconnectionId,
 *   searchFields (ALL | CUSTOMER_IDENTIFIER | CUSTOMER_NUMBER | POD_IDENTIFIER), prompt
 * - PagePowerSupplyDcnCancellationTableResponse.totalElements, content
 * - PowerSupplyDcnCancellationTableResponse.checked, unableToCheck
 * - CancellationOfThePowerSupplyRequest.saveAs enum DRAFT | EXECUTED
 * - CancellationPodRequest: customerId, podId, requestForDisconnectionOfPowerSupplyId,
 *   cancellationReasonId
 * - DPSRequestsBaseRequest.liabilityAmountFrom + currencyId, validityPeriodFrom/To,
 *   disconnectionRequestsStatus EXECUTED, conditionType LIST_OF_CUSTOMERS
 *
 * Confluence (product rule, not TC): Cancellation Create page 73990412 —
 * default 25/page; auto-check when remaining 0 or remaining < liability amount from;
 * cannot uncheck.
 *
 * Reference spec(s):
 * - tests/cursor/PDT-3042-cancellation-pod-auto-check.spec.ts
 * - tests/cursor/pdt-3042-cancellation-pod-auto-check.fixtures.ts
 * - tests/cursor/PDT-2971-rfd-shared-pod-multi-customer-merge.spec.ts
 * - tests/cursor/pdt-2971-rfd-shared-pod-multi-customer-merge.fixtures.ts
 * - tests/cursor/pdt-2913-missing-email-object-reminder-for-disconnection.fixtures.ts
 * - tests/receivableManagement/cancelationOfRequestForDisconnection.spec.ts (REG-1161)
 * - tests/receivableManagement/requestForDisconnection.spec.ts (REG-1045)
 */

import { test, expect } from './cursor-test.fixtures';
import {
  finalizeTestRunSummary,
  buildProductContractTabLinks,
} from './shared/manual-verification-links.fixtures';
import {
  findPodRow,
  swaggerChecked,
  swaggerUnableToCheck,
  type Pdt3042Fx,
} from './pdt-3042-cancellation-pod-auto-check.fixtures';
import {
  PDT_3169_KEY,
  PDT_3169_RFD_FROM,
  PDT_3169_TEST_TIMEOUT_MS,
  PDT_3169_TITLE,
  asNumber,
  buildPdt3169CancellationDraftPayload,
  cancellationPodRowFor,
  entityId,
  errorHaystack,
  findCustomerPodRow,
  getPdt3169CancellationTableContent,
  payPdt3169CustomerALiabilitiesFully,
  pdt3169RelevantKeys,
  postCancellationDraft,
  restErrorFields,
  rowCancellationReasonId,
  setupPdt3169TwoPodCancellationChain,
  type Pdt3169Fx,
} from './pdt-3169-cancellation-pagination-current-page-payload.fixtures';

const JIRA_TITLE = PDT_3169_TITLE;

test.describe(`[${PDT_3169_KEY}]: ${JIRA_TITLE}`, {
  tag: ['@dev2', '@receivableManagement', '@pdt-3169'],
}, () => {
  // Reminder POST /job is process-wide — do not run this chain in parallel.
  test.describe.configure({ mode: 'serial' });

  test(`[${PDT_3169_KEY}]: ${JIRA_TITLE}`, async ({
    Request, GeneratePayload, Responses, Endpoints, TestRunSummary,
  }) => {
    test.setTimeout(PDT_3169_TEST_TIMEOUT_MS);
    const fx: Pdt3169Fx = { Request, GeneratePayload, Responses, Endpoints };
    const pdt3042Fx: Pdt3042Fx = fx;

    const chain = await test.step(
      'Precondition: customer A+B, POD A+B, invoices, reminder job, EXECUTED RFD from=20 both PODs',
      async () => {
        const created = await setupPdt3169TwoPodCancellationChain(fx, {
          liabilityAmountFrom: PDT_3169_RFD_FROM,
        });
        TestRunSummary.registerPayload('customer', {
          customerAId: created.customerAId,
          customerAIdentifier: created.customerAIdentifier,
          customerBId: created.customerBId,
          customerBIdentifier: created.customerBIdentifier,
        });
        TestRunSummary.registerPayload('reminderForDisconnection', {
          reminderId: created.reminderId,
        });
        TestRunSummary.registerPayload('requestForDisconnection', {
          rfdId: created.rfdId,
          liabilityAmountFrom: PDT_3169_RFD_FROM,
          podAId: created.podAId,
          podBId: created.podBId,
        });
        return created;
      },
    );

    await test.step(
      'Pay Customer A liabilities fully (remaining 0 → auto-check). Leave Customer B unpaid',
      async () => {
        const paid = await payPdt3169CustomerALiabilitiesFully(fx, chain.liabilityIdsA);
        TestRunSummary.registerPayload('payment', {
          paymentIds: paid.paymentIds,
          customerARemaining: paid.remaining,
        });
      },
    );

    await test.step(
      'A: GET table-content size=1 page=0 → totalElements >= 2 (more than one table page)',
      async () => {
        const table = await getPdt3169CancellationTableContent(fx, {
          page: 0,
          size: 1,
          requestForDisconnectionId: chain.rfdId,
        });
        const enoughPages = table.totalElements >= 2;
        TestRunSummary.recordCheck({
          check: 'Pagination — size=1 yields more than one table page',
          expectedResult:
            'GET /cancellation-of-disconnection-of-the-power-supply/table-content ' +
            'DisconnectionOfPowerSupplyGetDraftTableRequest page=0 size=1 requestForDisconnectionId ' +
            'returns PagePowerSupplyDcnCancellationTableResponse.totalElements >= 2.',
          actualResult: enoughPages
            ? `As expected — totalElements=${table.totalElements}, contentRows=${table.rows.length}.`
            : `Not as expected — totalElements=${table.totalElements}, contentRows=${table.rows.length}, body=${JSON.stringify(table.body).slice(0, 800)}`,
          passed: enoughPages,
        });
        expect(
          table.totalElements,
          `size=1 page=0 must report at least two POD rows for RFD ${chain.rfdId}`,
        ).toBeGreaterThanOrEqual(2);
      },
    );

    const fullTable = await test.step(
      'B: GET table-content size=25 — POD A locked; POD B selectable',
      async () => {
        const table = await getPdt3169CancellationTableContent(fx, {
          page: 0,
          size: 25,
          requestForDisconnectionId: chain.rfdId,
        });
        const rowA = findCustomerPodRow(table.rows, chain.customerAId, chain.podAId);
        const rowB = findCustomerPodRow(table.rows, chain.customerBId, chain.podBId);
        expect(
          rowA,
          `GET table-content must include Customer A POD ${chain.podAId} (auto-checked)`,
        ).toBeTruthy();
        expect(
          rowB,
          `GET table-content must include Customer B POD ${chain.podBId} (selectable)`,
        ).toBeTruthy();
        const aChecked = swaggerChecked(rowA);
        const aUnable = swaggerUnableToCheck(rowA);
        const bChecked = swaggerChecked(rowB);
        const bUnable = swaggerUnableToCheck(rowB);
        const passed = aChecked === true && aUnable === true && bChecked === false && bUnable === false;
        TestRunSummary.recordCheck({
          check: 'Lock vs select — paid POD A auto-checked; unpaid POD B not locked',
          expectedResult:
            'Swagger PowerSupplyDcnCancellationTableResponse: POD A checked=true AND unableToCheck=true ' +
            '(Customer A remaining 0). POD B checked=false AND unableToCheck=false ' +
            `(Customer B remaining >= liabilityAmountFrom ${PDT_3169_RFD_FROM}).`,
          actualResult: passed
            ? `As expected — A checked=${aChecked} unableToCheck=${aUnable}; B checked=${bChecked} unableToCheck=${bUnable}.`
            : `Not as expected — A ${JSON.stringify(rowA)}; B ${JSON.stringify(rowB)}`,
          passed,
        });
        expect(aChecked, 'POD A Swagger checked (paid remaining 0)').toBe(true);
        expect(aUnable, 'POD A Swagger unableToCheck').toBe(true);
        expect(bChecked, 'POD B Swagger checked (unpaid remaining >= from)').toBe(false);
        expect(bUnable, 'POD B Swagger unableToCheck').toBe(false);
        return table;
      },
    );

    await test.step(
      'C: GET table-content searchFields=CUSTOMER_IDENTIFIER prompt=Customer B — selectable POD only',
      async () => {
        const table = await getPdt3169CancellationTableContent(fx, {
          page: 0,
          size: 25,
          requestForDisconnectionId: chain.rfdId,
          searchFields: 'CUSTOMER_IDENTIFIER',
          prompt: chain.customerBIdentifier,
        });
        const rowA = findCustomerPodRow(table.rows, chain.customerAId, chain.podAId);
        const rowB = findCustomerPodRow(table.rows, chain.customerBId, chain.podBId);
        const onlySelectableB = Boolean(rowB) && !rowA;
        TestRunSummary.recordCheck({
          check: 'Ticket filter — CUSTOMER_IDENTIFIER = Customer B shows selectable POD only',
          expectedResult:
            'GET table-content searchFields=CUSTOMER_IDENTIFIER prompt=Customer B identifier ' +
            'returns the selectable POD B row and omits auto-checked POD A (other page / other UIC).',
          actualResult: onlySelectableB
            ? `As expected — rows=${table.rows.length}, totalElements=${table.totalElements}, POD B present, POD A absent.`
            : `Not as expected — rows=${JSON.stringify(table.rows).slice(0, 1200)}`,
          passed: onlySelectableB,
        });
        expect(rowB, 'filtered table must include Customer B POD').toBeTruthy();
        expect(rowA, 'filtered table must omit auto-checked Customer A POD').toBeFalsy();
        expect(swaggerChecked(rowB), 'filtered POD B remains selectable (checked=false)').toBe(false);
        expect(swaggerUnableToCheck(rowB), 'filtered POD B unableToCheck=false').toBe(false);
      },
    );

    await test.step(
      "D: Reproduce bug (old FE) — POST DRAFT table=[Customer B only] → 400 can't be unchecked",
      async () => {
        const rowB = findCustomerPodRow(fullTable.rows, chain.customerBId, chain.podBId);
        const reasonB = rowCancellationReasonId(rowB, 'Customer B selected row');
        const payload = buildPdt3169CancellationDraftPayload(fx, chain.rfdId, [
          cancellationPodRowFor({
            customerId: chain.customerBId,
            podId: chain.podBId,
            rfdId: chain.rfdId,
            cancellationReasonId: reasonB,
          }),
        ]);
        TestRunSummary.registerPayload('cancellationCurrentPageOnly', payload);
        const created = await postCancellationDraft(pdt3042Fx, payload);
        const err = restErrorFields(created.status, created.text, created.json);
        const hasLockMessage = /can't be unchecked/i.test(err.haystack);
        const hasCustomerA = err.haystack.includes(String(chain.customerAId));
        const hasPodA = err.haystack.includes(String(chain.podAId));
        const hasAppError =
          err.errorCode === 'APPLICATION_ERROR' || /APPLICATION_ERROR/i.test(err.haystack);
        const hasClientException =
          err.exceptionId === 'ClientException' || /ClientException/i.test(err.haystack);
        const passed =
          created.status === 400 &&
          hasLockMessage &&
          hasCustomerA &&
          hasPodA &&
          hasAppError &&
          hasClientException;
        TestRunSummary.recordCheck({
          check: 'Old FE current-page payload omits locked POD A',
          expectedResult:
            'POST /cancellation-of-disconnection-of-the-power-supply saveAs=DRAFT with table = ' +
            'only Customer B (CancellationPodRequest) must return HTTP 400, errorCode APPLICATION_ERROR, ' +
            `exceptionId ClientException, message contains can't be unchecked and Customer A ` +
            `customerId=${chain.customerAId} podId=${chain.podAId} (auto-checked POD on another page).`,
          actualResult: passed
            ? `As expected — HTTP ${created.status}, errorCode=${err.errorCode}, exceptionId=${err.exceptionId}, lock message matched Customer A ids.`
            : `Not as expected — HTTP ${created.status}: ${err.haystack.slice(0, 800)}`,
          passed,
        });
        expect(created.status, 'omitting locked POD A from non-empty table must be HTTP 400').toBe(400);
        expect(err.haystack, 'errorCode APPLICATION_ERROR').toMatch(/APPLICATION_ERROR/i);
        expect(err.haystack, 'exceptionId ClientException').toMatch(/ClientException/i);
        expect(err.haystack, "error must mention can't be unchecked").toMatch(/can't be unchecked/i);
        expect(
          err.haystack,
          `error must mention Customer A customerId ${chain.customerAId}`,
        ).toContain(String(chain.customerAId));
        expect(
          err.haystack,
          `error must mention Customer A podId ${chain.podAId}`,
        ).toContain(String(chain.podAId));
      },
    );

    await test.step(
      'E: Expected product result — POST DRAFT table=[POD A locked + POD B selected] → 2xx',
      async () => {
        const rowA = findCustomerPodRow(fullTable.rows, chain.customerAId, chain.podAId)
          ?? findPodRow(fullTable.rows, chain.podAId);
        const rowB = findCustomerPodRow(fullTable.rows, chain.customerBId, chain.podBId)
          ?? findPodRow(fullTable.rows, chain.podBId);
        const reasonA = rowCancellationReasonId(rowA, 'auto-checked POD A');
        const reasonB = rowCancellationReasonId(rowB, 'selected POD B');
        const payload = buildPdt3169CancellationDraftPayload(fx, chain.rfdId, [
          cancellationPodRowFor({
            customerId: chain.customerAId,
            podId: chain.podAId,
            rfdId: chain.rfdId,
            cancellationReasonId: reasonA,
          }),
          cancellationPodRowFor({
            customerId: chain.customerBId,
            podId: chain.podBId,
            rfdId: chain.rfdId,
            cancellationReasonId: reasonB,
          }),
        ]);
        TestRunSummary.registerPayload('cancellationCompleteTable', payload);
        const created = await postCancellationDraft(pdt3042Fx, payload);
        const ok = created.status >= 200 && created.status < 300 && asNumber(created.id) > 0;
        TestRunSummary.recordCheck({
          check: 'Complete payload includes locked POD A plus selected POD B',
          expectedResult:
            'POST same endpoint saveAs=DRAFT with table = [auto-checked POD A + selected POD B] ' +
            '→ HTTP 2xx and created cancellation id (Jira expected result once all locked PODs are in the payload).',
          actualResult: ok
            ? `As expected — HTTP ${created.status}, id=${created.id}.`
            : `Not as expected — HTTP ${created.status}: ${errorHaystack(created.status, created.text, created.json).slice(0, 800)}`,
          passed: ok,
        });
        expect(created.status, 'POST cancellation DRAFT complete table').toBeGreaterThanOrEqual(200);
        expect(created.status, 'POST cancellation DRAFT complete table').toBeLessThan(300);
        expect(created.id, 'created cancellation id').toBeGreaterThan(0);
      },
    );

    await test.step('Attach test run summary', async () => {
      const extra: Record<string, string[]> = {};
      try {
        const linksA = buildProductContractTabLinks(entityId(Responses.productContract[0]));
        const linksB = buildProductContractTabLinks(entityId(Responses.productContract[1]));
        extra.productContract = [
          ...(linksA.productContract ?? []),
          ...(linksB.productContract ?? []),
        ];
      } catch {
        /* portal base URL may be absent */
      }
      finalizeTestRunSummary(TestRunSummary, Responses, {
        jiraKey: PDT_3169_KEY,
        relevantEntityKeys: pdt3169RelevantKeys(),
        extraLinks: extra.productContract?.length ? extra : undefined,
        snapshot: {
          rfdId: chain.rfdId,
          customerAId: chain.customerAId,
          customerBId: chain.customerBId,
          customerAIdentifier: chain.customerAIdentifier,
          customerBIdentifier: chain.customerBIdentifier,
          podAId: chain.podAId,
          podBId: chain.podBId,
          liabilityAmountFrom: PDT_3169_RFD_FROM,
          liabilityIdsA: chain.liabilityIdsA,
          liabilityIdsB: chain.liabilityIdsB,
        },
      });
    });
  });
});
