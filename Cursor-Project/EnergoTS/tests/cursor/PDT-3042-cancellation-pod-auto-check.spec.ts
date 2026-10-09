/**
 * PDT-3042 — Cancellation of disconnection CREATE table: POD auto-checked and
 * locked when remaining reminder-liability principal is strictly below RFD
 * liabilityAmountFrom (or remaining == 0).
 *
 * Bug-only / standalone (no test_cases/*.md). Exact Jira title in each test().
 *
 * Target env: Dev2
 * Canonical OpenAPI: Cursor-Project/config/swagger/dev2/swagger-spec.json
 * Runtime BASE_URL: http://10.236.20.11:8092
 *   (also public https://devapps.energo-pro.bg/backend/phoenix-dev2)
 * Playwright default config is NOT Dev2 — run with:
 *   BASE_URL=http://10.236.20.11:8092 npx playwright test tests/cursor/PDT-3042-cancellation-pod-auto-check.spec.ts
 *
 * Swagger refresh this session: update-swagger-specs.ps1 needs curl.exe (Windows);
 * macOS /usr/bin/curl fallback downloaded all envs including Dev2 (ok).
 *
 * Chain: two manual invoices 50+100 → assert each liability billingGroupResponse.id /
 * contractBillingGroupId > 0 → overdue ALL open liabilities for the customer →
 * reminder payload (customerList + listOfCustomer + confirm true) → POST /job
 * (13 min HTTP, 12 min EXECUTED poll) → GET second-tab fail-fast → POST EXECUTED RFD
 * (13 min HTTP = eligibility check). Do not poll reminders-list-for-disconnection-request.
 *
 * Swagger DTO fields used:
 * - PowerSupplyDcnCancellationTableResponse.checked
 * - PowerSupplyDcnCancellationTableResponse.unableToCheck
 * - CancellationOfThePowerSupplyRequest.saveAs enum DRAFT | EXECUTED
 * - CancellationPodRequest: customerId, podId, requestForDisconnectionOfPowerSupplyId, cancellationReasonId
 * - DPSRequestsBaseRequest.liabilityAmountFrom
 * - PowerSupplyDisconnectionReminderBaseRequest: communicationChannels, conditionType,
 *   customerSendToDateAndTime, disconnectionDate, liabilitiesMaxDueDate, listOfCustomer, customerList, confirm
 *
 * Confluence (parent already read):
 * - Cancellation of a disconnection of the power supply - Create (page 73990412)
 * - Edit (73990422)
 * - Phase 2 Create (585698120) — ticket Additional Logic for Phase2: No; same auto-check rule documented
 * - Phase 2 Edit (585698133)
 *
 * Reference spec(s):
 * - tests/receivableManagement/requestForDisconnection.spec.ts (REG-1045)
 * - tests/receivableManagement/cancelationOfRequestForDisconnection.spec.ts (REG-1161 chain)
 * - tests/cursor/PDT-2971-rfd-shared-pod-multi-customer-merge.spec.ts
 * - tests/cursor/pdt-2971-rfd-shared-pod-multi-customer-merge.fixtures.ts
 * - tests/cursor/pdt-3179-reconnection-invoice-emails.fixtures.ts
 * - tests/cursor/PDT-2880-pod-disconnected-banner.spec.ts
 * - tests/receivableManagement/Payment/offlinePaymentCreateAndOffsetting.spec.ts (REG-1005)
 * - jsons/payloadGenerators/domains/ReceivablesManagementPayloads.ts cancellationOfRequestForDisconnection()
 */

import { test, expect } from './cursor-test.fixtures';
import {
  finalizeTestRunSummary,
  buildProductContractTabLinks,
} from './shared/manual-verification-links.fixtures';
import {
  PDT_3042_KEY,
  PDT_3042_TITLE,
  PDT_3042_TEST_TIMEOUT_MS,
  PDT_3042_RFD_FROM,
  PDT_3042_REMAINING_AFTER_PAYMENTS,
  asNumber,
  buildCancellationDraftPayload,
  cancellationPodRow,
  cancellationPodRowOmittingLocked,
  entityId,
  errorHaystack,
  findPodRow,
  getCancellationCheckedPods,
  getCancellationTableContent,
  payTicketExampleAmounts,
  pdt3042RelevantKeys,
  postCancellationDraft,
  requireReasonForCancellation,
  setupPdt3042TicketChain,
  sumLiabilityCurrentAmounts,
  swaggerChecked,
  swaggerUnableToCheck,
  type Pdt3042Fx,
} from './pdt-3042-cancellation-pod-auto-check.fixtures';

const JIRA_TITLE = PDT_3042_TITLE;

test.describe(`[${PDT_3042_KEY}]: ${JIRA_TITLE}`, {
  tag: ['@dev2', '@receivableManagement', '@pdt-3042'],
}, () => {
  // Reminder POST /job is process-wide — do not run these chains in parallel.
  test.describe.configure({ mode: 'serial' });

  test.describe('ticket example remaining 15 < from 20', () => {
    test(`[${PDT_3042_KEY}]: ${JIRA_TITLE}`, async ({
      Request, GeneratePayload, Responses, Endpoints, TestRunSummary,
    }) => {
      test.setTimeout(PDT_3042_TEST_TIMEOUT_MS);
      const fx: Pdt3042Fx = { Request, GeneratePayload, Responses, Endpoints };

      const chain = await test.step(
        'Precondition: customer, POD, two invoices 50+100, reminder job, EXECUTED RFD from=20',
        async () => {
          const created = await setupPdt3042TicketChain(fx, {
            liabilityAmountFrom: PDT_3042_RFD_FROM,
          });
          TestRunSummary.registerPayload('customer', Responses.customer[0]);
          TestRunSummary.registerPayload('reminderForDisconnection', {
            reminderId: created.reminderId,
          });
          TestRunSummary.registerPayload('requestForDisconnection', {
            rfdId: created.rfdId,
            liabilityAmountFrom: PDT_3042_RFD_FROM,
          });
          return created;
        },
      );

      const paid = await test.step(
        'Pay first liability fully then second down so remaining principal ≈ 15 (no calculate-tax)',
        async () => {
          const result = await payTicketExampleAmounts(fx, chain.liabilityIds);
          TestRunSummary.registerPayload('payment', {
            paymentIds: result.paymentIds,
            remaining: result.remaining,
          });
          return result;
        },
      );

      const tableRow = await test.step(
        'GET table-content: POD checked=true and unableToCheck=true (15 < 20)',
        async () => {
          const table = await getCancellationTableContent(fx, chain.rfdId);
          const row = findPodRow(table.rows, chain.podId);
          expect(
            row,
            `GET table-content must include POD ${chain.podId} for RFD ${chain.rfdId}`,
          ).toBeTruthy();
          const checked = swaggerChecked(row);
          const unableToCheck = swaggerUnableToCheck(row);
          TestRunSummary.recordCheck({
            check: 'Ticket example — remaining 15 < liabilityAmountFrom 20 auto-check lock',
            expectedResult:
              'GET /cancellation-of-disconnection-of-the-power-supply/table-content: ' +
              'checked=true AND unableToCheck=true for the unpaid-below-from POD (principal remaining 15, from 20).',
            actualResult: checked && unableToCheck
              ? `As expected — remaining=${paid.remaining}, checked=${checked}, unableToCheck=${unableToCheck}, cancellationReasonId=${row!.cancellationReasonId}.`
              : `Not as expected — remaining=${paid.remaining}, checked=${checked}, unableToCheck=${unableToCheck}, row=${JSON.stringify(row)}`,
            passed: checked && unableToCheck,
          });
          expect(checked, 'Swagger checked').toBe(true);
          expect(unableToCheck, 'Swagger unableToCheck').toBe(true);
          return row!;
        },
      );

      await test.step(
        'GET get-checked-pods/{rfdId}/0 (create uses cancellationId=0; empty list is known Phoenix behavior)',
        async () => {
          const checkedPods = await getCancellationCheckedPods(fx, chain.rfdId, 0);
          const row = findPodRow(checkedPods.rows, chain.podId);
          if (!row) {
            // Dev2 getCheckedPods(cancellationId=0) returns [] — no saved cancellation POD rows yet.
            // UI seeds checked PODs from table-content instead (regression Finding).
            TestRunSummary.recordCheck({
              check: 'GET get-checked-pods/{rfdId}/0 at create time',
              expectedResult:
                'POD may appear with checked=true and unableToCheck=true, or the list may be empty ' +
                '(Phoenix getCheckedPods returns [] when cancellationId=0; UI seeds from table-content).',
              actualResult:
                `As expected — empty create-time get-checked-pods is known Phoenix behavior ` +
                `(rows=${checkedPods.rows.length}, POD ${chain.podId} missing). ` +
                `table-content already proved auto-check.`,
              passed: true,
            });
            return;
          }
          expect(swaggerChecked(row), 'get-checked-pods checked').toBe(true);
          expect(swaggerUnableToCheck(row), 'get-checked-pods unableToCheck').toBe(true);
          TestRunSummary.recordCheck({
            check: 'GET get-checked-pods/{rfdId}/0 at create time',
            expectedResult:
              'If the POD is present, checked=true AND unableToCheck=true (same as table-content).',
            actualResult:
              `As expected — POD ${chain.podId} present with checked=${row.checked}, unableToCheck=${row.unableToCheck}.`,
            passed: true,
          });
        },
      );

      await test.step('POST cancellation DRAFT including the auto-checked POD → 2xx', async () => {
        const reasonId =
          asNumber(tableRow.cancellationReasonId) || requireReasonForCancellation('create DRAFT');
        const payload = buildCancellationDraftPayload(fx, chain, [
          cancellationPodRow(chain, reasonId),
        ]);
        TestRunSummary.registerPayload('cancellationOfRequestOfDisconnection', payload);
        const created = await postCancellationDraft(fx, payload);
        TestRunSummary.recordCheck({
          check: 'POST cancellation DRAFT with auto-checked POD in table',
          expectedResult: 'HTTP 2xx; CancellationOfThePowerSupplyRequest.saveAs=DRAFT accepted.',
          actualResult: created.status >= 200 && created.status < 300
            ? `As expected — HTTP ${created.status}, id=${created.id}.`
            : `Not as expected — HTTP ${created.status}: ${errorHaystack(created.status, created.text, created.json)}`,
          passed: created.status >= 200 && created.status < 300,
        });
        expect(created.status, 'POST cancellation DRAFT').toBeGreaterThanOrEqual(200);
        expect(created.status, 'POST cancellation DRAFT').toBeLessThan(300);
        expect(created.id, 'created cancellation id').toBeGreaterThan(0);
      });

      await test.step('Attach test run summary', async () => {
        let extra: Record<string, string[]> | undefined;
        try {
          extra = buildProductContractTabLinks(entityId(Responses.productContract[0]));
        } catch {
          extra = undefined;
        }
        finalizeTestRunSummary(TestRunSummary, Responses, {
          jiraKey: PDT_3042_KEY,
          relevantEntityKeys: pdt3042RelevantKeys({
            includePayment: true,
            includeCancellation: true,
          }),
          extraLinks: extra && Object.keys(extra).length ? extra : undefined,
          snapshot: {
            rfdId: chain.rfdId,
            podId: chain.podId,
            remaining: paid.remaining,
            liabilityAmountFrom: PDT_3042_RFD_FROM,
            liabilityIds: chain.liabilityIds,
          },
        });
      });
    });
  });

  test.describe('boundary remaining >= from 20 not auto-checked', () => {
    test(`[${PDT_3042_KEY}]: ${JIRA_TITLE}`, async ({
      Request, GeneratePayload, Responses, Endpoints, TestRunSummary,
    }) => {
      test.setTimeout(PDT_3042_TEST_TIMEOUT_MS);
      const fx: Pdt3042Fx = { Request, GeneratePayload, Responses, Endpoints };

      const chain = await test.step(
        'Precondition: two invoices 50+100, reminder job, EXECUTED RFD from=20 (no payments)',
        async () => {
          const created = await setupPdt3042TicketChain(fx, {
            liabilityAmountFrom: PDT_3042_RFD_FROM,
          });
          TestRunSummary.registerPayload('requestForDisconnection', {
            rfdId: created.rfdId,
            liabilityAmountFrom: PDT_3042_RFD_FROM,
          });
          return created;
        },
      );

      const remaining = await test.step(
        'Confirm remaining principal is the sum of both invoice liabilities (>= 20)',
        async () => {
          const sum = await sumLiabilityCurrentAmounts(fx, chain.liabilityIds);
          expect(
            sum,
            `unpaid remaining ${sum} must be >= liabilityAmountFrom ${PDT_3042_RFD_FROM}`,
          ).toBeGreaterThanOrEqual(PDT_3042_RFD_FROM);
          return sum;
        },
      );

      await test.step(
        'GET table-content: remaining >= from 20 → checked=false and unableToCheck=false',
        async () => {
          const table = await getCancellationTableContent(fx, chain.rfdId);
          const row = findPodRow(table.rows, chain.podId);
          expect(
            row,
            `GET table-content must include POD ${chain.podId} (manual select allowed)`,
          ).toBeTruthy();
          const checked = swaggerChecked(row);
          const unableToCheck = swaggerUnableToCheck(row);
          const notLocked = checked === false && unableToCheck === false;
          TestRunSummary.recordCheck({
            check: 'Boundary — remaining >= liabilityAmountFrom is NOT auto-checked (strict <)',
            expectedResult:
              'No payments: remaining = sum of both invoice liability currentAmounts, and remaining >= from 20 → ' +
              'checked=false AND unableToCheck=false. Equal remaining==from is also NOT auto-checked (SQL uses remaining < from).',
            actualResult: notLocked
              ? `As expected — remaining=${remaining}, checked=${checked}, unableToCheck=${unableToCheck}.`
              : `Not as expected — remaining=${remaining}, checked=${checked}, unableToCheck=${unableToCheck}, row=${JSON.stringify(row)}`,
            passed: notLocked,
          });
          expect(checked, 'Swagger checked must be false when remaining >= from').toBe(false);
          expect(
            unableToCheck,
            'Swagger unableToCheck must be false when remaining >= from',
          ).toBe(false);
        },
      );

      await test.step('Attach test run summary', async () => {
        finalizeTestRunSummary(TestRunSummary, Responses, {
          jiraKey: PDT_3042_KEY,
          relevantEntityKeys: pdt3042RelevantKeys(),
          snapshot: {
            rfdId: chain.rfdId,
            podId: chain.podId,
            remaining,
            liabilityAmountFrom: PDT_3042_RFD_FROM,
          },
        });
      });
    });
  });

  test.describe('lock omit auto-checked POD from non-empty table returns 400', () => {
    test(`[${PDT_3042_KEY}]: ${JIRA_TITLE}`, async ({
      Request, GeneratePayload, Responses, Endpoints, TestRunSummary,
    }) => {
      test.setTimeout(PDT_3042_TEST_TIMEOUT_MS);
      const fx: Pdt3042Fx = { Request, GeneratePayload, Responses, Endpoints };

      const chain = await test.step(
        'Precondition: ticket example chain + payments so POD is auto-checked locked',
        async () => {
          const created = await setupPdt3042TicketChain(fx, {
            liabilityAmountFrom: PDT_3042_RFD_FROM,
          });
          await payTicketExampleAmounts(fx, created.liabilityIds);
          return created;
        },
      );

      await test.step('GET table-content confirms lock before negative POST', async () => {
        const table = await getCancellationTableContent(fx, chain.rfdId);
        const row = findPodRow(table.rows, chain.podId);
        expect(row, 'locked POD must appear on table-content').toBeTruthy();
        expect(swaggerChecked(row)).toBe(true);
        expect(swaggerUnableToCheck(row)).toBe(true);
        TestRunSummary.registerPayload('lockedPod', {
          podId: chain.podId,
          checked: row!.checked,
          unableToCheck: row!.unableToCheck,
        });
      });

      await test.step(
        "POST / with non-empty table omitting auto-checked locked POD → 400 containing can't be unchecked",
        async () => {
          // Do NOT use table=[] — CancellationOfPowerSupplyRequestValidator rejects empty
          // table first ("table-table must not be empty.") before validateTable lock check.
          // Non-empty table that omits the locked POD (different podId) hits:
          // CancellationOfDisconnectionOfThePowerSupplyService.validateTable → "can't be unchecked".
          const reasonId = requireReasonForCancellation('omit locked POD negative POST');
          const payload = buildCancellationDraftPayload(fx, chain, [
            cancellationPodRowOmittingLocked(chain, reasonId),
          ]);
          TestRunSummary.registerPayload('cancellationOfRequestOfDisconnection', payload);
          const created = await postCancellationDraft(fx, payload);
          const haystack = errorHaystack(created.status, created.text, created.json);
          const hasLockMessage = /can't be unchecked/i.test(haystack);
          const passed = created.status === 400 && hasLockMessage;
          TestRunSummary.recordCheck({
            check: 'Cannot omit auto-locked POD from non-empty create table',
            expectedResult:
              'HTTP 400; body contains can\'t be unchecked ' +
              '(validateTable: checked POD from findForCheck missing from request.table; ' +
              'not empty-table bean validation).',
            actualResult: passed
              ? `As expected — HTTP ${created.status}, lock message matched.`
              : `Not as expected — HTTP ${created.status}: ${haystack.slice(0, 800)}`,
            passed,
          });
          expect(created.status, 'omitting locked POD must be 400').toBe(400);
          expect(haystack, 'error must mention cannot uncheck').toMatch(/can't be unchecked/i);
        },
      );

      await test.step('Attach test run summary', async () => {
        finalizeTestRunSummary(TestRunSummary, Responses, {
          jiraKey: PDT_3042_KEY,
          relevantEntityKeys: pdt3042RelevantKeys({ includePayment: true }),
          snapshot: {
            rfdId: chain.rfdId,
            podId: chain.podId,
            remaining: PDT_3042_REMAINING_AFTER_PAYMENTS,
          },
        });
      });
    });
  });
});
