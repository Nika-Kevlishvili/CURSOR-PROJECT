/**
 * PDT-3215 — Change Time for Accounting Period Dates and more.
 *
 * Backend TCs: Cursor-Project/test_cases/Backend/Timezone_accounting_period_payment_package.md
 * Environment: any (set BASE_URL). Live OPEN period covering Sofia today — not
 * hardcoded ACCOUNTING202608. Story PDF August dates stay in fixtures as examples.
 *
 * Reference spec(s):
 * - tests/cursor/PDT-3171-easypay-proforma-due-date-validto.spec.ts
 * - tests/cursor/pdt-2960-proforma-liability-due-date.fixtures.ts
 * - tests/cursor/pdt-3214-easypay-liabilities-receivables-accounting-period-rules.fixtures.ts
 * - tests/cursor/pdt-3215-accounting-period-dates-and-more.fixtures.ts
 *
 * /epay/* is not in Phoenix core swagger.
 *
 * Jira source: REST fallback (MCP unavailable; get-jira-issue-rest.ps1).
 * Swagger refresh: update-swagger-specs.ps1 succeeded this session (all envs).
 *
 * TC-BE-6/10/12: EasyPay ONLINE package is a plain PaymentPackageCreateRequest
 * (not GeneratePayload.payment_package) + withEasyPayChannelLock around confirm.
 * Confirm DATE is Sofia today 00:15 (equals first BG day on the 1st). origin/test
 * PaymentService uses LocalDate.now() for online package applicability, so a
 * first-of-month DATE cannot match createOrFetch's package.
 *
 * TC-BE-10: EasyPay DATE = first Sofia day 00:15 of the live OPEN period.
 * TC-BE-12: EasyPay confirm_date time component must not be stored as 00:00.
 */

import { test, expect } from './cursor-test.fixtures';
import { finalizeTestRunSummary } from './shared/manual-verification-links.fixtures';
import {
  CLOSE_TOO_EARLY_FRAGMENT,
  EASYPAY_FUTURE_ADDITIONALINFO,
  VALIDATE_OUTSIDE_MESSAGE,
  addSofiaDays,
  callConfirmPay,
  calculateConfirmChecksum,
  calculateInitChecksum,
  callInitPay,
  createLegalCustomer,
  createOfflineChannelAndPackage,
  createOfflinePayment,
  createOnlinePackageOnChannel,
  createReceivableOnPeriodDate,
  currentOpenPeriodForNow,
  datePart,
  extractPaymentPeriodId,
  extractPaymentPeriodName,
  findEasyPayOnlineChannel,
  planEasyPayOnlineConfirm,
  formatEasyPayStatusDetail,
  getAccountingPeriodById,
  getLiabilityAccountingPeriod,
  getPaymentDetail,
  getPaymentPackageDetail,
  listPaymentsByCustomerIdentifier,
  resolveEasyPayMerchantId,
  resolveOpenPeriodForSofiaDate,
  runPackageLockJob,
  setEasyPayCombineLiabilities,
  setupProformaLiabilityChain,
  sofiaNowIsBeforeEasyPayDate,
  sofiaYmd,
  timePart,
  toCoinAmount,
  toEasyPayDate,
  toIsoDate,
  toPeriodCalendar,
  uniqueTid,
  validatePaymentDate,
  withEasyPayChannelLock,
  type Pdt3215Fx,
  type PeriodCalendar,
} from './pdt-3215-accounting-period-dates-and-more.fixtures';

const JIRA_KEY = 'PDT-3215';
const JIRA_TITLE = 'Change Time for Accounting Period Dates and more';

async function loadLivePeriod(fx: Pdt3215Fx): Promise<PeriodCalendar> {
  const today = sofiaYmd();
  const period = await resolveOpenPeriodForSofiaDate(fx, today);
  if (!period) {
    test.skip(true, `No OPEN accounting period covering Sofia today ${today}`);
  }
  return toPeriodCalendar(period!);
}

function skipIfEasyPayDateFuture(easyPayDate: string): void {
  if (sofiaNowIsBeforeEasyPayDate(easyPayDate)) {
    test.skip(
      true,
      `EasyPay DATE ${easyPayDate} is still in the future vs Sofia now`,
    );
  }
}

test.describe(`[${JIRA_KEY}]: ${JIRA_TITLE}`, {
  tag: ['@receivableManagement', '@payment', '@easypay', '@pdt-3215'],
}, () => {
  test(
    `[${JIRA_KEY}] TC-BE-1: Accounting Period Start/End are UTC of 00:00 BG day 1 and 23:59 BG last day`,
    async ({ Request, GeneratePayload, Responses, Endpoints, OnlinePaymentUrl, TestRunSummary }) => {
      const fx: Pdt3215Fx = { Request, GeneratePayload, Responses, Endpoints, OnlinePaymentUrl };

      const cal = await test.step('Precondition: OPEN accounting period covering Sofia today', async () => {
        return loadLivePeriod(fx);
      });

      await test.step('Assert: Start/End are UTC of Bulgarian month bounds', async () => {
        const expectedName = `ACCOUNTING${cal.firstIso.slice(0, 7).replace('-', '')}`;
        TestRunSummary.registerPayload('accountingPeriod', {
          id: cal.period.id,
          name: cal.period.name,
          firstIso: cal.firstIso,
          lastIso: cal.lastIso,
        });
        expect(cal.period.name).toBe(expectedName);
        expect(String(cal.period.startDate).startsWith(cal.startUtcPrefix)).toBeTruthy();
        expect(String(cal.period.endDate).startsWith(cal.endUtcPrefix)).toBeTruthy();
        TestRunSummary.recordCheck({
          check: 'TC-BE-1 live period UTC bounds',
          expectedResult: `name=${expectedName}, startDate=${cal.startUtcPrefix}, endDate=${cal.endUtcPrefix}`,
          actualResult: `name=${cal.period.name}, startDate=${cal.period.startDate}, endDate=${cal.period.endDate}`,
          passed:
            cal.period.name === expectedName &&
            String(cal.period.startDate).startsWith(cal.startUtcPrefix) &&
            String(cal.period.endDate).startsWith(cal.endUtcPrefix),
        });
      });

      await test.step('Attach test run summary', async () => {
        finalizeTestRunSummary(TestRunSummary, Responses, {
          jiraKey: JIRA_KEY,
          relevantEntityKeys: [],
          snapshot: {
            periodId: cal.period.id,
            name: cal.period.name,
            firstIso: cal.firstIso,
            lastIso: cal.lastIso,
            startDate: cal.period.startDate,
            endDate: cal.period.endDate,
          },
        });
      });
    },
  );

  test(
    `[${JIRA_KEY}] TC-BE-2: Payment lookup — first BG day of the live OPEN period belongs to that period`,
    async ({ Request, GeneratePayload, Responses, Endpoints, OnlinePaymentUrl, TestRunSummary }) => {
      const fx: Pdt3215Fx = { Request, GeneratePayload, Responses, Endpoints, OnlinePaymentUrl };

      const cal = await test.step('Precondition: resolve live OPEN accounting period', async () => {
        return loadLivePeriod(fx);
      });

      const result = await test.step(
        `Action: GET validate-payment-date-and-accounting-period paymentDate=${cal.firstIso}`,
        async () => validatePaymentDate(fx, cal.period.id, cal.firstIso),
      );

      await test.step('Assert: valid=true (HTTP 200)', async () => {
        expect(result.status).toBe(200);
        expect(result.valid).toBe(true);
        expect(result.message == null || result.message === '').toBeTruthy();
        TestRunSummary.recordCheck({
          check: 'TC-BE-2 first BG day belongs to live period',
          expectedResult: 'HTTP 200, valid=true, message empty',
          actualResult: `HTTP ${result.status}, valid=${result.valid}, message=${result.message}`,
          passed: result.status === 200 && result.valid === true,
        });
      });

      await test.step('Attach test run summary', async () => {
        finalizeTestRunSummary(TestRunSummary, Responses, {
          jiraKey: JIRA_KEY,
          relevantEntityKeys: [],
          snapshot: { periodId: cal.period.id, firstIso: cal.firstIso, valid: result.valid },
        });
      });
    },
  );

  test(
    `[${JIRA_KEY}] TC-BE-3: Payment lookup — day before the live OPEN period does not belong to it`,
    async ({ Request, GeneratePayload, Responses, Endpoints, OnlinePaymentUrl, TestRunSummary }) => {
      const fx: Pdt3215Fx = { Request, GeneratePayload, Responses, Endpoints, OnlinePaymentUrl };

      const cal = await test.step('Precondition: resolve live OPEN accounting period', async () => {
        return loadLivePeriod(fx);
      });

      const result = await test.step(
        `Action: GET validate-payment-date-and-accounting-period paymentDate=${cal.dayBeforeIso}`,
        async () => validatePaymentDate(fx, cal.period.id, cal.dayBeforeIso),
      );

      await test.step('Assert: HTTP 200, valid=false, outside message', async () => {
        expect(result.status).toBe(200);
        expect(result.valid).toBe(false);
        expect(String(result.message)).toBe(VALIDATE_OUTSIDE_MESSAGE);
        TestRunSummary.recordCheck({
          check: 'TC-BE-3 day before live period is outside',
          expectedResult: `HTTP 200, valid=false, message=${VALIDATE_OUTSIDE_MESSAGE}`,
          actualResult: `HTTP ${result.status}, valid=${result.valid}, message=${result.message}`,
          passed:
            result.status === 200 &&
            result.valid === false &&
            String(result.message) === VALIDATE_OUTSIDE_MESSAGE,
        });
      });

      await test.step('Attach test run summary', async () => {
        finalizeTestRunSummary(TestRunSummary, Responses, {
          jiraKey: JIRA_KEY,
          relevantEntityKeys: [],
          snapshot: { periodId: cal.period.id, dayBeforeIso: cal.dayBeforeIso, valid: result.valid },
        });
      });
    },
  );

  test(
    `[${JIRA_KEY}] TC-BE-4: Receivable on occurrenceDate first BG day stores the live OPEN Accounting Period`,
    async ({ Request, GeneratePayload, Responses, Endpoints, OnlinePaymentUrl, TestRunSummary }) => {
      const fx: Pdt3215Fx = { Request, GeneratePayload, Responses, Endpoints, OnlinePaymentUrl };

      await test.step('Precondition: legal customer', async () => {
        await createLegalCustomer(fx);
        TestRunSummary.registerPayload('customer', Responses.customer[0]);
      });

      const cal = await test.step('Precondition: resolve live OPEN accounting period', async () => {
        return loadLivePeriod(fx);
      });

      const dueIso = cal.firstIso === cal.lastIso ? cal.firstIso : addSofiaDays(cal.firstIso, 14);
      const { receivableId, body } = await test.step(
        `Action: POST /customer-receivable occurrenceDate=${cal.firstIso}`,
        async () =>
          createReceivableOnPeriodDate(fx, {
            accountingPeriodId: cal.period.id,
            occurrenceIso: cal.firstIso,
            dueIso,
          }),
      );
      TestRunSummary.registerPayload('customerReceivable', {
        receivableId,
        occurrenceDate: cal.firstIso,
      });

      await test.step('Assert: accountingPeriodResponse is the live period', async () => {
        const periodId = Number(body.accountingPeriodResponse?.id ?? body.accountingPeriodId);
        const periodName = String(
          body.accountingPeriodResponse?.name ?? body.accountingPeriodName ?? '',
        );
        expect(periodId).toBe(cal.period.id);
        expect(periodName).toBe(cal.period.name);
        expect(toIsoDate(body.occurrenceDate)).toBe(cal.firstIso);
        TestRunSummary.recordCheck({
          check: 'TC-BE-4 receivable live AP',
          expectedResult: `accountingPeriodResponse.id=${cal.period.id}, name=${cal.period.name}, occurrenceDate=${cal.firstIso}`,
          actualResult: `id=${periodId}, name=${periodName}, occurrenceDate=${body.occurrenceDate}`,
          passed: periodId === cal.period.id && periodName === cal.period.name,
        });
      });

      await test.step('Attach test run summary', async () => {
        finalizeTestRunSummary(TestRunSummary, Responses, {
          jiraKey: JIRA_KEY,
          relevantEntityKeys: ['customer', 'customerReceivable'],
          snapshot: { receivableId, periodId: cal.period.id },
        });
      });
    },
  );

  test(
    `[${JIRA_KEY}] TC-BE-5: Close live OPEN Accounting Period is rejected while now is still before End`,
    async ({ Request, GeneratePayload, Responses, Endpoints, OnlinePaymentUrl, TestRunSummary }) => {
      const fx: Pdt3215Fx = { Request, GeneratePayload, Responses, Endpoints, OnlinePaymentUrl };

      const cal = await test.step('Precondition: GET live OPEN period with End in the future', async () => {
        const live = await loadLivePeriod(fx);
        expect(live.period.status).toBe('OPEN');
        if (new Date(live.period.endDate).getTime() <= Date.now()) {
          test.skip(
            true,
            `Period ${live.period.name} end ${live.period.endDate} is not in the future — close-too-early does not apply`,
          );
        }
        return live;
      });

      const putRes = await test.step('Action: PUT /accounting-period/{id} status=CLOSED', async () => {
        return fx.Request.put(`accounting-period/${cal.period.id}`, {
          data: {
            name: cal.period.name,
            startDate: cal.period.startDate,
            endDate: cal.period.endDate,
            status: 'CLOSED',
            modifyDate: cal.period.modifyDate ?? cal.period.startDate,
          },
        });
      });

      await test.step('Assert: HTTP 400 close-too-early; GET still OPEN', async () => {
        const putBody = await putRes.text();
        const skipReason =
          putBody.includes('because of its status in billing run')
            ? 'Live period is blocked by a billing run — not this TC'
            : putBody.includes('file generation is in progress')
              ? 'Live period file generation is in progress — not this TC'
              : putBody.includes('already closed')
                ? 'Live period is already CLOSED — not this TC'
                : null;
        if (skipReason) {
          test.skip(true, skipReason);
        }
        expect(putRes.status(), putBody.slice(0, 400)).toBe(400);
        expect(putBody).toContain(CLOSE_TOO_EARLY_FRAGMENT);
        const after = await getAccountingPeriodById(fx, cal.period.id);
        expect(after.status).toBe('OPEN');
        TestRunSummary.recordCheck({
          check: 'TC-BE-5 close blocked before End',
          expectedResult: `HTTP 400 contains ${CLOSE_TOO_EARLY_FRAGMENT}; status still OPEN`,
          actualResult: `HTTP ${putRes.status()} body=${putBody.slice(0, 300)}; GET status=${after.status}`,
          passed: putRes.status() === 400 && after.status === 'OPEN',
        });
      });

      await test.step('Attach test run summary', async () => {
        finalizeTestRunSummary(TestRunSummary, Responses, {
          jiraKey: JIRA_KEY,
          relevantEntityKeys: [],
          snapshot: { periodId: cal.period.id, putStatus: putRes.status() },
        });
      });
    },
  );

  test(
    `[${JIRA_KEY}] TC-BE-6: EasyPay DATE first BG day 00:15 assigns the live OPEN Accounting Period`,
    async ({ Request, GeneratePayload, Responses, Endpoints, OnlinePaymentUrl, TestRunSummary }) => {
      test.setTimeout(20 * 60 * 1000);
      const fx: Pdt3215Fx = { Request, GeneratePayload, Responses, Endpoints, OnlinePaymentUrl };

      const { liabilityId, currentAmount } = await test.step(
        'Precondition: legal customer + goods-order proforma accounting',
        async () => setupProformaLiabilityChain(fx),
      );
      TestRunSummary.registerPayload('customer', Responses.customer[0]);
      expect(currentAmount).toBeGreaterThan(0);

      const cal = await test.step('Precondition: resolve live OPEN accounting period', async () => {
        return loadLivePeriod(fx);
      });
      const confirmPlan = planEasyPayOnlineConfirm(cal);
      skipIfEasyPayDateFuture(confirmPlan.easyPayDate);

      let easyPayChannelId: number;
      const merchantId = await test.step('Precondition: EasyPay merchant + combine=false', async () => {
        const mid = await resolveEasyPayMerchantId(fx);
        easyPayChannelId = await findEasyPayOnlineChannel(fx);
        await setEasyPayCombineLiabilities(fx, false, easyPayChannelId);
        return mid;
      });

      const customerNumber = String(Responses.customer[0].customerNumber);
      const identifier = String(Responses.customer[0].identifier);
      const total = toCoinAmount(currentAmount);

      const before = await test.step('Precondition: count ACTIVE payments for this customer', async () => {
        return listPaymentsByCustomerIdentifier(fx, identifier);
      });

      const confirmBody = await withEasyPayChannelLock(async () => {
        await test.step(
          `Precondition: ONLINE payment package paymentDate=${confirmPlan.paymentDateIso} on EasyPay ONLINE`,
          async () =>
            createOnlinePackageOnChannel(fx, {
              channelId: easyPayChannelId,
              paymentDateIso: confirmPlan.paymentDateIso,
              accountingPeriodId: cal.period.id,
            }),
        );
        return test.step(
          `Action: EasyPay confirm-pay DATE=${confirmPlan.easyPayDate}`,
          async () => {
            const tid = uniqueTid();
            const initChecksum = await calculateInitChecksum(fx, { customerNumber, tid, merchantId });
            await callInitPay(fx, { customerNumber, tid, merchantId, checksum: initChecksum });
            const confirmChecksum = await calculateConfirmChecksum(fx, {
              date: confirmPlan.easyPayDate,
              customerNumber,
              tid,
              merchantId,
              total,
            });
            return callConfirmPay(fx, {
              date: confirmPlan.easyPayDate,
              customerNumber,
              tid,
              merchantId,
              total,
              checksum: confirmChecksum,
            });
          },
        );
      });

      await test.step('Assert: STATUS 00, payment live AP, package first BG day', async () => {
        expect(
          String(confirmBody.STATUS),
          formatEasyPayStatusDetail(confirmBody),
        ).toBe('00');

        const after = await listPaymentsByCustomerIdentifier(fx, identifier);
        const beforeIds = new Set((before.content ?? []).map((row: { id?: number }) => Number(row.id)));
        const created = (after.content ?? []).find(
          (row: { id?: number; status?: string }) =>
            !beforeIds.has(Number(row.id)) && String(row.status) === 'ACTIVE',
        );
        expect(created, 'new ACTIVE payment after confirm-pay').toBeTruthy();
        const paymentId = Number(created.id);
        const payment = await getPaymentDetail(fx, paymentId);
        const periodId = extractPaymentPeriodId(payment);
        const periodName = extractPaymentPeriodName(payment);
        expect(periodId).toBe(cal.period.id);
        expect(periodName).toBe(cal.period.name);

        const pkg = await getPaymentPackageDetail(fx, Number(payment.paymentPackageId));
        expect(datePart(pkg.paymentDate)).toBe(confirmPlan.paymentDateIso);

        TestRunSummary.recordCheck({
          check: 'TC-BE-6 EasyPay first BG day → live period',
          expectedResult: `STATUS=00, accountPeriodId=${cal.period.id} ${cal.period.name}, package paymentDate=${confirmPlan.paymentDateIso}`,
          actualResult: `STATUS=${confirmBody.STATUS}, periodId=${periodId} ${periodName}, packageDate=${pkg.paymentDate}`,
          passed:
            String(confirmBody.STATUS) === '00' &&
            periodId === cal.period.id &&
            periodName === cal.period.name,
        });
      });

      await test.step('Attach test run summary', async () => {
        finalizeTestRunSummary(TestRunSummary, Responses, {
          jiraKey: JIRA_KEY,
          relevantEntityKeys: ['customer', 'goodsOrder', 'invoice', 'customerLiability', 'payment'],
          snapshot: { liabilityId, confirmStatus: confirmBody.STATUS, periodName: cal.period.name },
        });
      });
    },
  );

  test(
    `[${JIRA_KEY}] TC-BE-7: Offline (non-EasyPay) payment stores time 00:00 and the live OPEN period`,
    async ({ Request, GeneratePayload, Responses, Endpoints, OnlinePaymentUrl, TestRunSummary }) => {
      const fx: Pdt3215Fx = { Request, GeneratePayload, Responses, Endpoints, OnlinePaymentUrl };

      await test.step('Precondition: legal customer', async () => {
        await createLegalCustomer(fx);
        TestRunSummary.registerPayload('customer', Responses.customer[0]);
      });

      const cal = await test.step('Precondition: resolve live OPEN period', async () => {
        const live = await loadLivePeriod(fx);
        expect(live.period.status).toBe('OPEN');
        return live;
      });

      const { collectionChannelId, paymentPackageId } = await test.step(
        `Precondition: OFFLINE channel + UNLOCKED package paymentDate=${cal.firstIso}`,
        async () =>
          createOfflineChannelAndPackage(fx, {
            paymentDateIso: cal.firstIso,
            accountingPeriodId: cal.period.id,
            type: 'OFFLINE',
          }),
      );

      const paymentId = await test.step(`Action: POST /payment paymentDate=${cal.firstIso}`, async () => {
        return createOfflinePayment(fx, {
          paymentDateIso: cal.firstIso,
          accountingPeriodId: cal.period.id,
          collectionChannelId,
          paymentPackageId,
          initialAmount: 10,
        });
      });

      await test.step('Assert: live AP, isOnlinePayment=false, time 00:00 when present', async () => {
        const payment = await getPaymentDetail(fx, paymentId);
        expect(payment.isOnlinePayment).toBe(false);
        expect(extractPaymentPeriodId(payment)).toBe(cal.period.id);
        expect(extractPaymentPeriodName(payment)).toBe(cal.period.name);
        expect(datePart(payment.paymentDate)).toBe(cal.firstIso);
        const time = timePart(payment.paymentDate);
        if (time) {
          expect(time.startsWith('00:00:00')).toBeTruthy();
        }
        TestRunSummary.recordCheck({
          check: 'TC-BE-7 offline 00:00 live period',
          expectedResult: `isOnlinePayment=false, AP=${cal.period.name}, paymentDate=${cal.firstIso}T00:00:00`,
          actualResult: `isOnlinePayment=${payment.isOnlinePayment}, AP=${extractPaymentPeriodName(payment)}, paymentDate=${payment.paymentDate}`,
          passed:
            payment.isOnlinePayment === false &&
            extractPaymentPeriodId(payment) === cal.period.id,
        });
      });

      await test.step('Attach test run summary', async () => {
        finalizeTestRunSummary(TestRunSummary, Responses, {
          jiraKey: JIRA_KEY,
          relevantEntityKeys: ['customer', 'payment', 'paymentPackage'],
          snapshot: { paymentId, periodId: cal.period.id },
        });
      });
    },
  );

  test(
    `[${JIRA_KEY}] TC-BE-8: EasyPay confirm with a future DATE is rejected and creates no payment`,
    async ({ Request, GeneratePayload, Responses, Endpoints, OnlinePaymentUrl, TestRunSummary }) => {
      test.setTimeout(20 * 60 * 1000);
      const fx: Pdt3215Fx = { Request, GeneratePayload, Responses, Endpoints, OnlinePaymentUrl };

      const { liabilityId, currentAmount } = await test.step(
        'Precondition: legal customer + goods-order proforma accounting',
        async () => setupProformaLiabilityChain(fx),
      );
      TestRunSummary.registerPayload('customer', Responses.customer[0]);
      const openAmount = currentAmount;
      expect(openAmount).toBeGreaterThan(0);

      let easyPayChannelId: number;
      const merchantId = await test.step('Precondition: EasyPay merchant + combine=false', async () => {
        const mid = await resolveEasyPayMerchantId(fx);
        easyPayChannelId = await findEasyPayOnlineChannel(fx);
        await setEasyPayCombineLiabilities(fx, false, easyPayChannelId);
        return mid;
      });

      const futureDate = toEasyPayDate(addSofiaDays(sofiaYmd(), 1), '001500');
      const customerNumber = String(Responses.customer[0].customerNumber);
      const identifier = String(Responses.customer[0].identifier);
      const total = toCoinAmount(openAmount);

      const before = await test.step('Precondition: count ACTIVE payments', async () => {
        return listPaymentsByCustomerIdentifier(fx, identifier);
      });

      const confirmBody = await test.step(`Action: EasyPay confirm-pay DATE=${futureDate}`, async () => {
        const tid = uniqueTid();
        const initChecksum = await calculateInitChecksum(fx, { customerNumber, tid, merchantId });
        await callInitPay(fx, { customerNumber, tid, merchantId, checksum: initChecksum });
        const confirmChecksum = await calculateConfirmChecksum(fx, {
          date: futureDate,
          customerNumber,
          tid,
          merchantId,
          total,
        });
        return callConfirmPay(fx, {
          date: futureDate,
          customerNumber,
          tid,
          merchantId,
          total,
          checksum: confirmChecksum,
        });
      });

      await test.step('Assert: STATUS 96, no new payment, liability amount unchanged', async () => {
        expect(confirmBody.STATUS).toBeDefined();
        expect(String(confirmBody.STATUS)).toBe('96');
        expect(String(confirmBody.DESCRIPTION)).toBe('General error');
        expect(String(confirmBody.ADDITIONALINFO)).toBe(EASYPAY_FUTURE_ADDITIONALINFO);

        const after = await listPaymentsByCustomerIdentifier(fx, identifier);
        const beforeActive = (before.content ?? []).filter((r: { status?: string }) => r.status === 'ACTIVE').length;
        const afterActive = (after.content ?? []).filter((r: { status?: string }) => r.status === 'ACTIVE').length;
        expect(afterActive).toBe(beforeActive);

        const liability = await getLiabilityAccountingPeriod(fx, liabilityId);
        expect(liability.currentAmount).toBe(openAmount);

        TestRunSummary.recordCheck({
          check: 'TC-BE-8 future DATE rejected',
          expectedResult: `STATUS=96, DESCRIPTION=General error, ADDITIONALINFO=${EASYPAY_FUTURE_ADDITIONALINFO}, no new payment`,
          actualResult: formatEasyPayStatusDetail(confirmBody),
          passed: String(confirmBody.STATUS) === '96' && afterActive === beforeActive,
        });
      });

      await test.step('Attach test run summary', async () => {
        finalizeTestRunSummary(TestRunSummary, Responses, {
          jiraKey: JIRA_KEY,
          relevantEntityKeys: ['customer', 'goodsOrder', 'customerLiability'],
          snapshot: { liabilityId, futureDate, confirmStatus: confirmBody.STATUS },
        });
      });
    },
  );

  test(
    `[${JIRA_KEY}] TC-BE-9: Payment Package lock at Bulgarian midnight locks packages of the BG day that just ended`,
    async ({ Request, GeneratePayload, Responses, Endpoints, OnlinePaymentUrl, TestRunSummary }) => {
      const fx: Pdt3215Fx = { Request, GeneratePayload, Responses, Endpoints, OnlinePaymentUrl };

      const channelId = await test.step('Precondition: EasyPay ONLINE channel', async () => {
        return findEasyPayOnlineChannel(fx);
      });

      const todayBg = sofiaYmd();
      const yesterdayBg = addSofiaDays(todayBg, -1);

      const todayPeriod = await test.step('Precondition: OPEN accounting period for Sofia today', async () => {
        return currentOpenPeriodForNow(fx);
      });

      const yesterdayPeriod = await test.step(
        'Precondition: OPEN accounting period for Sofia yesterday',
        async () => {
          const row = await resolveOpenPeriodForSofiaDate(fx, yesterdayBg);
          if (!row) {
            test.skip(
              true,
              `No OPEN accounting period covering Sofia yesterday ${yesterdayBg} (month boundary / closed)`,
            );
          }
          return row!;
        },
      );

      const yesterdayPackageId = await test.step(
        `Precondition: ONLINE package paymentDate=${yesterdayBg} UNLOCKED`,
        async () =>
          createOnlinePackageOnChannel(fx, {
            channelId,
            paymentDateIso: yesterdayBg,
            accountingPeriodId: yesterdayPeriod.id,
          }),
      );

      const todayPackageId = await test.step(
        `Precondition: ONLINE package paymentDate=${todayBg} UNLOCKED`,
        async () =>
          createOnlinePackageOnChannel(fx, {
            channelId,
            paymentDateIso: todayBg,
            accountingPeriodId: todayPeriod.id,
          }),
      );

      await test.step('Action: POST /payment-package/job-test', async () => {
        await runPackageLockJob(fx);
      });

      await test.step('Assert: yesterday LOCKED, today UNLOCKED (PDF)', async () => {
        const yesterday = await getPaymentPackageDetail(fx, yesterdayPackageId);
        const today = await getPaymentPackageDetail(fx, todayPackageId);
        expect(yesterday.lockStatus).toBe('LOCKED');
        expect(today.lockStatus).toBe('UNLOCKED');
        TestRunSummary.recordCheck({
          check: 'TC-BE-9 package lock yesterday vs today',
          expectedResult: 'yesterday lockStatus=LOCKED, today lockStatus=UNLOCKED (PDF)',
          actualResult: `yesterday=${yesterday.lockStatus}, today=${today.lockStatus}`,
          passed: yesterday.lockStatus === 'LOCKED' && today.lockStatus === 'UNLOCKED',
        });
      });

      await test.step('Attach test run summary', async () => {
        finalizeTestRunSummary(TestRunSummary, Responses, {
          jiraKey: JIRA_KEY,
          relevantEntityKeys: ['paymentPackage'],
          snapshot: { yesterdayPackageId, todayPackageId, yesterdayBg, todayBg },
        });
      });
    },
  );

  test(
    `[${JIRA_KEY}] TC-BE-10: EasyPay payment DATE=first BG day 00:15 assigns the live OPEN period (§1.5)`,
    async ({ Request, GeneratePayload, Responses, Endpoints, OnlinePaymentUrl, TestRunSummary }) => {
      test.setTimeout(20 * 60 * 1000);
      const fx: Pdt3215Fx = { Request, GeneratePayload, Responses, Endpoints, OnlinePaymentUrl };

      const { liabilityId, currentAmount } = await test.step(
        'Precondition: legal customer + goods-order proforma accounting',
        async () => setupProformaLiabilityChain(fx),
      );
      TestRunSummary.registerPayload('customer', Responses.customer[0]);
      expect(currentAmount).toBeGreaterThan(0);

      const cal = await test.step('Precondition: resolve live OPEN accounting period', async () => {
        return loadLivePeriod(fx);
      });
      const confirmPlan = planEasyPayOnlineConfirm(cal);
      skipIfEasyPayDateFuture(confirmPlan.easyPayDate);

      let easyPayChannelId: number;
      const merchantId = await test.step('Precondition: EasyPay merchant + combine=false', async () => {
        const mid = await resolveEasyPayMerchantId(fx);
        easyPayChannelId = await findEasyPayOnlineChannel(fx);
        await setEasyPayCombineLiabilities(fx, false, easyPayChannelId);
        return mid;
      });

      const customerNumber = String(Responses.customer[0].customerNumber);
      const identifier = String(Responses.customer[0].identifier);
      const total = toCoinAmount(currentAmount);

      const before = await test.step('Precondition: count ACTIVE payments for this customer', async () => {
        return listPaymentsByCustomerIdentifier(fx, identifier);
      });

      const confirmBody = await withEasyPayChannelLock(async () => {
        await test.step(
          `Precondition: ONLINE payment package paymentDate=${confirmPlan.paymentDateIso} on EasyPay ONLINE`,
          async () =>
            createOnlinePackageOnChannel(fx, {
              channelId: easyPayChannelId,
              paymentDateIso: confirmPlan.paymentDateIso,
              accountingPeriodId: cal.period.id,
            }),
        );
        return test.step(
          `Action: EasyPay confirm-pay DATE=${confirmPlan.easyPayDate}`,
          async () => {
            const tid = uniqueTid();
            const initChecksum = await calculateInitChecksum(fx, { customerNumber, tid, merchantId });
            await callInitPay(fx, { customerNumber, tid, merchantId, checksum: initChecksum });
            const confirmChecksum = await calculateConfirmChecksum(fx, {
              date: confirmPlan.easyPayDate,
              customerNumber,
              tid,
              merchantId,
              total,
            });
            return callConfirmPay(fx, {
              date: confirmPlan.easyPayDate,
              customerNumber,
              tid,
              merchantId,
              total,
              checksum: confirmChecksum,
            });
          },
        );
      });

      await test.step('Assert: STATUS 00, Payment accountPeriodId = live period (§1.5)', async () => {
        expect(
          String(confirmBody.STATUS),
          formatEasyPayStatusDetail(confirmBody),
        ).toBe('00');

        const after = await listPaymentsByCustomerIdentifier(fx, identifier);
        const beforeIds = new Set((before.content ?? []).map((row: { id?: number }) => Number(row.id)));
        const created = (after.content ?? []).find(
          (row: { id?: number; status?: string }) =>
            !beforeIds.has(Number(row.id)) && String(row.status) === 'ACTIVE',
        );
        expect(created, 'new ACTIVE payment after confirm-pay').toBeTruthy();
        const paymentId = Number(created.id);
        const payment = await getPaymentDetail(fx, paymentId);
        const periodId = extractPaymentPeriodId(payment);
        const periodName = extractPaymentPeriodName(payment);
        expect(periodId).toBe(cal.period.id);
        expect(periodName).toBe(cal.period.name);

        const pkg = await getPaymentPackageDetail(fx, Number(payment.paymentPackageId));
        expect(datePart(pkg.paymentDate)).toBe(confirmPlan.paymentDateIso);

        TestRunSummary.recordCheck({
          check: 'TC-BE-10 §1.5 Payment AP = live period',
          expectedResult: `STATUS=00, accountPeriodId=${cal.period.id} ${cal.period.name}, package paymentDate=${confirmPlan.paymentDateIso}`,
          actualResult: `STATUS=${confirmBody.STATUS}, periodId=${periodId} ${periodName}, packageDate=${pkg.paymentDate}`,
          passed:
            String(confirmBody.STATUS) === '00' &&
            periodId === cal.period.id &&
            periodName === cal.period.name,
        });
      });

      await test.step('Attach test run summary', async () => {
        finalizeTestRunSummary(TestRunSummary, Responses, {
          jiraKey: JIRA_KEY,
          relevantEntityKeys: ['customer', 'goodsOrder', 'invoice', 'customerLiability', 'payment'],
          snapshot: { liabilityId, confirmStatus: confirmBody.STATUS, periodId: cal.period.id },
        });
      });
    },
  );

  test(
    `[${JIRA_KEY}] TC-BE-11: New goods-order liability is assigned the OPEN Accounting Period that contains now(UTC)`,
    async ({ Request, GeneratePayload, Responses, Endpoints, OnlinePaymentUrl, TestRunSummary }) => {
      test.setTimeout(20 * 60 * 1000);
      const fx: Pdt3215Fx = { Request, GeneratePayload, Responses, Endpoints, OnlinePaymentUrl };

      const currentOpen = await test.step('Precondition: current OPEN period for Sofia today', async () => {
        return currentOpenPeriodForNow(fx);
      });

      const { liabilityId } = await test.step(
        'Action: goods-order proforma + generating + accounting',
        async () => setupProformaLiabilityChain(fx),
      );
      TestRunSummary.registerPayload('customer', Responses.customer[0]);

      await test.step('Assert: liability accountPeriodId + accountingPeriodName + currentAmount>0', async () => {
        const detail = await getLiabilityAccountingPeriod(fx, liabilityId);
        expect(detail.currentAmount).toBeGreaterThan(0);
        expect(detail.accountPeriodId).toBe(currentOpen.id);
        expect(detail.accountingPeriodName).toBe(currentOpen.name);
        TestRunSummary.recordCheck({
          check: 'TC-BE-11 liability current OPEN period',
          expectedResult: `accountPeriodId=${currentOpen.id}, accountingPeriodName=${currentOpen.name}, currentAmount>0`,
          actualResult: `accountPeriodId=${detail.accountPeriodId}, accountingPeriodName=${detail.accountingPeriodName}, currentAmount=${detail.currentAmount}`,
          passed:
            detail.accountPeriodId === currentOpen.id &&
            detail.accountingPeriodName === currentOpen.name &&
            detail.currentAmount > 0,
        });
      });

      await test.step('Attach test run summary', async () => {
        finalizeTestRunSummary(TestRunSummary, Responses, {
          jiraKey: JIRA_KEY,
          relevantEntityKeys: ['customer', 'goodsOrder', 'invoice', 'customerLiability'],
          snapshot: {
            liabilityId,
            currentOpenId: currentOpen.id,
            currentOpenName: currentOpen.name,
          },
        });
      });
    },
  );

  test(
    `[${JIRA_KEY}] TC-BE-12: EasyPay DATE=first BG day 00:15 — paymentDate time part is not 00:00 after §3.1 fix`,
    async ({ Request, GeneratePayload, Responses, Endpoints, OnlinePaymentUrl, TestRunSummary }) => {
      test.setTimeout(20 * 60 * 1000);
      const fx: Pdt3215Fx = { Request, GeneratePayload, Responses, Endpoints, OnlinePaymentUrl };

      const { liabilityId, currentAmount } = await test.step(
        'Precondition: legal customer + goods-order proforma accounting',
        async () => setupProformaLiabilityChain(fx),
      );
      TestRunSummary.registerPayload('customer', Responses.customer[0]);
      expect(currentAmount).toBeGreaterThan(0);

      const cal = await test.step('Precondition: resolve live OPEN accounting period', async () => {
        return loadLivePeriod(fx);
      });
      const confirmPlan = planEasyPayOnlineConfirm(cal);
      skipIfEasyPayDateFuture(confirmPlan.easyPayDate);

      let easyPayChannelId: number;
      const merchantId = await test.step('Precondition: EasyPay merchant + combine=false', async () => {
        const mid = await resolveEasyPayMerchantId(fx);
        easyPayChannelId = await findEasyPayOnlineChannel(fx);
        await setEasyPayCombineLiabilities(fx, false, easyPayChannelId);
        return mid;
      });

      const customerNumber = String(Responses.customer[0].customerNumber);
      const identifier = String(Responses.customer[0].identifier);
      const total = toCoinAmount(currentAmount);

      const before = await test.step('Precondition: count ACTIVE payments', async () => {
        return listPaymentsByCustomerIdentifier(fx, identifier);
      });

      const confirmBody = await withEasyPayChannelLock(async () => {
        await test.step(
          `Precondition: ONLINE payment package paymentDate=${confirmPlan.paymentDateIso} on EasyPay ONLINE`,
          async () =>
            createOnlinePackageOnChannel(fx, {
              channelId: easyPayChannelId,
              paymentDateIso: confirmPlan.paymentDateIso,
              accountingPeriodId: cal.period.id,
            }),
        );
        return test.step(
          `Action: EasyPay confirm-pay DATE=${confirmPlan.easyPayDate}`,
          async () => {
            const tid = uniqueTid();
            const initChecksum = await calculateInitChecksum(fx, { customerNumber, tid, merchantId });
            await callInitPay(fx, { customerNumber, tid, merchantId, checksum: initChecksum });
            const confirmChecksum = await calculateConfirmChecksum(fx, {
              date: confirmPlan.easyPayDate,
              customerNumber,
              tid,
              merchantId,
              total,
            });
            return callConfirmPay(fx, {
              date: confirmPlan.easyPayDate,
              customerNumber,
              tid,
              merchantId,
              total,
              checksum: confirmChecksum,
            });
          },
        );
      });

      await test.step('Assert: STATUS 00; paymentDate time part is not 00:00 (§3.1)', async () => {
        expect(
          String(confirmBody.STATUS),
          formatEasyPayStatusDetail(confirmBody),
        ).toBe('00');

        const after = await listPaymentsByCustomerIdentifier(fx, identifier);
        const beforeIds = new Set((before.content ?? []).map((row: { id?: number }) => Number(row.id)));
        const created = (after.content ?? []).find(
          (row: { id?: number; status?: string }) =>
            !beforeIds.has(Number(row.id)) && String(row.status) === 'ACTIVE',
        );
        expect(created, 'new ACTIVE payment after confirm-pay').toBeTruthy();
        const paymentId = Number(created.id);
        const payment = await getPaymentDetail(fx, paymentId);
        const time = timePart(payment.paymentDate);

        const timePreserved = Boolean(time) && !time.startsWith('00:00:00');
        TestRunSummary.recordCheck({
          check: 'TC-BE-12 §3.1 EasyPay confirm_date time preserved',
          expectedResult: 'paymentDate time part != 00:00:00 (§3.1). Expected 00:15:00 (BG) or 21:15:00 (UTC).',
          actualResult: `paymentDate=${payment.paymentDate}, timePart=${time || 'absent'}, timePreserved=${timePreserved}`,
          passed: timePreserved,
        });
        expect(
          timePreserved,
          `paymentDate time must not be 00:00:00 after §3.1. paymentDate=${payment.paymentDate}`,
        ).toBe(true);
      });

      await test.step('Attach test run summary', async () => {
        finalizeTestRunSummary(TestRunSummary, Responses, {
          jiraKey: JIRA_KEY,
          relevantEntityKeys: ['customer', 'goodsOrder', 'invoice', 'customerLiability', 'payment'],
          snapshot: { liabilityId, confirmStatus: confirmBody.STATUS },
        });
      });
    },
  );
});
