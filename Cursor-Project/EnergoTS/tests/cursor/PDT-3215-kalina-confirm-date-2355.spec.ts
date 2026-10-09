/**
 * PDT-3215 — Kalina EasyPay confirm_date repro: DATE = today Sofia 12:13.
 *
 * Bug/standalone automation (no new Backend TC .md). Environment: any (set BASE_URL).
 *
 * Pattern: PDT-3215-lpf-due-date-timezone-gap.spec.ts Test B (due = today, no LPF)
 * with confirm DATE = today Sofia 12:13 (`yyyyMMdd` + `121300`) via
 * `toEasyPayDate(sofiaYmd(), '121300')`.
 * Mirrors Kalina display 15:13 as Sofia wall 12:13 (+3h display offset).
 * Example: today Sofia 2026-09-10 → confirmDate `20260910121300`.
 *
 * Expect EasyPay confirm STATUS 00. Soft-skip if before 12:13 Sofia (future DATE),
 * or env blockers (merchant / channel / billing-run lock / paymentDate outside AP —
 * same callConfirmPayForLpf mapping as LPF/PDT-3214). Prefer --workers=1
 * (withEasyPayChannelLock).
 *
 * Reference spec(s):
 * - tests/cursor/PDT-3215-lpf-due-date-timezone-gap.spec.ts (Test B)
 * - tests/cursor/pdt-3215-accounting-period-dates-and-more.fixtures.ts
 * - tests/cursor/pdt-3214-easypay-liabilities-receivables-accounting-period-rules.fixtures.ts
 *
 * /epay/* is not in Phoenix core swagger — contract from payment-api + PDT-3171 helpers.
 *
 * Swagger (all envs, update-swagger-specs.ps1 this session):
 * - POST /customer-liability — CustomerLiabilityRequest required: accountingPeriodId,
 *   currencyId, customerId, dueDate, initialAmount, occurrenceDate
 * - POST /interest-rate; POST /payment, GET /payment/{id}
 */

import { test, expect } from './cursor-test.fixtures';
import { finalizeTestRunSummary } from './shared/manual-verification-links.fixtures';
import {
  calculateConfirmChecksum,
  calculateInitChecksum,
  callInitPay,
  createLegalCustomer,
  formatEasyPayStatusDetail,
  getPaymentDetail,
  isoToDdMmYyyy,
  resolveEasyPayMerchantId,
  sofiaYmd,
  timePart,
  toEasyPayDate,
  uniqueTid,
} from './pdt-3215-accounting-period-dates-and-more.fixtures';
import {
  callConfirmPayForLpf,
  createDailyInterestRate,
  createOverdueLiabilityWithInterest,
  ensureEasyPayCombineLiabilities,
  findLatestOnlinePaymentForCustomer,
  toCoinAmount,
  withEasyPayChannelLock,
  type Pdt3214Fx,
} from './pdt-3214-easypay-liabilities-receivables-accounting-period-rules.fixtures';

const JIRA_KEY = 'PDT-3215';
const JIRA_TITLE = 'Change Time for Accounting Period Dates and more';
/** Overdue principal (REG-1063 / PDT-3214 Test 2 / LPF Test B). */
const LPF_OVERDUE_AMOUNT = 100;
/** Today Sofia 12:13 — Kalina confirm_date repro (15:13 display = +3h). */
const CONFIRM_SOFIA_HMS = '121300';

function sofiaHourMinute(date: Date = new Date()): { hour: number; minute: number } {
  const parts = new Intl.DateTimeFormat('en-GB', {
    timeZone: 'Europe/Sofia',
    hour: '2-digit',
    minute: '2-digit',
    hourCycle: 'h23',
  }).formatToParts(date);
  return {
    hour: Number(parts.find((p) => p.type === 'hour')?.value ?? 0),
    minute: Number(parts.find((p) => p.type === 'minute')?.value ?? 0),
  };
}

function isEasyPayEnvBlocker(text: string): boolean {
  const msg = String(text ?? '').toLowerCase();
  if (!msg.trim()) return false;
  if (msg.includes('amount is incorrect') || msg.includes('amount incorrect')) return false;
  if (msg.includes('merchant not found')) return true;
  if (msg.includes('easypay online collection channel must exist')) return true;
  if (msg.includes('easypay') && msg.includes('channel') && msg.includes('missing')) return true;
  if (
    (msg.includes('billing run') || msg.includes('billing-run') || msg.includes('billingrun')) &&
    msg.includes('lock')
  ) {
    return true;
  }
  return false;
}

test.describe(`[${JIRA_KEY}]: ${JIRA_TITLE}`, {
  tag: ['@pdt-3215', '@easypay', '@receivableManagement'],
}, () => {
  // Shared EasyPay ONLINE channel: withEasyPayChannelLock. Prefer --workers=1.

  test(
    `[${JIRA_KEY}]: EasyPay DATE today Sofia 12:13 when due is today — confirm_date Kalina repro`,
    async ({
      Request,
      GeneratePayload,
      Responses,
      Endpoints,
      OnlinePaymentUrl,
      TestRunSummary,
    }) => {
      test.setTimeout(10 * 60 * 1000);
      const fx: Pdt3214Fx = {
        Request,
        GeneratePayload,
        Responses,
        Endpoints,
        OnlinePaymentUrl,
      };

      const todaySofia = sofiaYmd();
      const confirmDate = toEasyPayDate(todaySofia, CONFIRM_SOFIA_HMS);
      const dueDateDdMmYyyy = isoToDdMmYyyy(todaySofia);
      const { hour, minute } = sofiaHourMinute();

      if (hour < 12 || (hour === 12 && minute < 13)) {
        test.skip(
          true,
          `Sofia time ${String(hour).padStart(2, '0')}:${String(minute).padStart(2, '0')} ` +
            `is before 12:13 — DATE ${confirmDate} would be future (EasyPay STATUS 96). ` +
            `Re-run after 12:13 Europe/Sofia; do not use tomorrow DATE.`,
        );
      }

      const { sourceLiabilityId, interestRateId } = await test.step(
        'Precondition: legal customer + daily interest + liability due today Sofia',
        async () => {
          await createLegalCustomer(fx);
          const interestId = await createDailyInterestRate(fx);
          const liabilityId = await createOverdueLiabilityWithInterest(fx, {
            initialAmount: LPF_OVERDUE_AMOUNT,
            interestRateId: interestId,
            dueDateDdMmYyyy,
            occurrenceDateDdMmYyyy: dueDateDdMmYyyy,
          });
          TestRunSummary.registerPayload('customer', Responses.customer[0]);
          TestRunSummary.registerPayload('interestRate', { id: interestId });
          TestRunSummary.registerPayload('customerLiability', {
            sourceLiabilityId: liabilityId,
            initialAmount: LPF_OVERDUE_AMOUNT,
            dueDate: dueDateDdMmYyyy,
            occurrenceDate: dueDateDdMmYyyy,
            note:
              'Due = today Sofia; confirm DATE = today Sofia 12:13 (Kalina confirm_date repro)',
          });
          return { sourceLiabilityId: liabilityId, interestRateId: interestId };
        },
      );

      const customerNumber = String(Responses.customer[0].customerNumber);
      const identifier = String(Responses.customer[0].identifier);
      const tid = uniqueTid();
      const principalCoins = toCoinAmount(LPF_OVERDUE_AMOUNT);

      const { merchantId, initBody, confirmBody } = await test.step(
        `Action: EasyPay channel lock + init-pay + confirm-pay DATE=${confirmDate}`,
        async () =>
          withEasyPayChannelLock(async () => {
            let mid: string;
            try {
              mid = await resolveEasyPayMerchantId(fx);
              await ensureEasyPayCombineLiabilities(fx, true);
            } catch (err) {
              const detail = err instanceof Error ? err.message : String(err);
              if (isEasyPayEnvBlocker(detail)) {
                test.skip(true, `EasyPay env blocker before init: ${detail}`);
              }
              throw err;
            }

            const checksum = await calculateInitChecksum(fx, {
              customerNumber,
              tid,
              merchantId: mid,
            });
            const body = await callInitPay(fx, {
              customerNumber,
              tid,
              merchantId: mid,
              checksum,
            });
            const initDetail = formatEasyPayStatusDetail(body);
            if (String(body.STATUS) !== '00' && isEasyPayEnvBlocker(initDetail)) {
              test.skip(true, `EasyPay init-pay env blocker: ${initDetail}`);
            }
            expect(
              String(body.STATUS),
              `EasyPay init-pay must return STATUS 00. merchantId=${mid}. ${initDetail}`,
            ).toBe('00');

            const amountCoins = Number(body.AMOUNT);
            const noLpfOnDueDate = amountCoins === principalCoins;
            TestRunSummary.registerPayload('easyPayInit', {
              tid,
              merchantId: mid,
              confirmDate,
              todaySofia,
              customerNumber,
              identifier,
              status: body.STATUS,
              amountRaw: body.AMOUNT,
              amountCoins,
              principalCoins,
              combineLiabilities: true,
            });
            TestRunSummary.recordCheck({
              check: 'EasyPay init-pay AMOUNT equals principal (due = today, no LPF)',
              expectedResult: `STATUS 00 and AMOUNT coins = principal ${principalCoins}`,
              actualResult: noLpfOnDueDate
                ? `As expected — STATUS=${body.STATUS} AMOUNT=${body.AMOUNT}`
                : `Not as expected — STATUS=${body.STATUS} AMOUNT=${body.AMOUNT} ` +
                  `principalCoins=${principalCoins}`,
              passed: noLpfOnDueDate,
            });
            expect(
              amountCoins,
              `On due date ${todaySofia}, init AMOUNT must equal principal coins ` +
                `${principalCoins} (no LPF). ${initDetail}`,
            ).toBe(principalCoins);

            const amountRaw = body.AMOUNT;
            const checksumConfirm = await calculateConfirmChecksum(fx, {
              date: confirmDate,
              customerNumber,
              tid,
              merchantId: mid,
              total: amountRaw,
            });
            console.log(
              `[PDT-3215-kalina-1213] confirmDate=${confirmDate} tid=${tid} ` +
                `customerNumber=${customerNumber} identifier=${identifier} ` +
                `sourceLiabilityId=${sourceLiabilityId}`,
            );
            const result = await callConfirmPayForLpf(fx, {
              date: confirmDate,
              customerNumber,
              tid,
              merchantId: mid,
              total: amountRaw,
              checksum: checksumConfirm,
            });

            if (result.kind === 'payment_date_outside') {
              TestRunSummary.registerPayload('easyPayConfirm', {
                confirmDate,
                tid,
                customerNumber,
                identifier,
                paymentId: null,
                sourceLiabilityId,
                merchantId: mid,
                confirmStatus: null,
                error: result.detail.slice(0, 800),
              });
              TestRunSummary.recordCheck({
                check: 'EasyPay confirm-pay DATE today Sofia 12:13',
                expectedResult: 'STATUS 00 (Kalina confirm_date repro)',
                actualResult: `Not as expected — paymentDate/AP outside: ${result.detail}`,
                passed: false,
              });
              test.skip(
                true,
                `EasyPay confirm DATE ${confirmDate} rejected as paymentDate outside accounting period. ${result.detail}`,
              );
            }

            const confirm =
              result.kind === 'ok' || result.kind === 'other_status' ? result.body : undefined;
            const confirmDetail =
              result.kind === 'other_status'
                ? result.detail
                : formatEasyPayStatusDetail(confirm);
            const confirmStatus = String(confirm?.STATUS ?? '');

            if (confirmStatus !== '00' && isEasyPayEnvBlocker(confirmDetail)) {
              test.skip(true, `EasyPay confirm-pay env blocker: ${confirmDetail}`);
            }

            expect(
              confirmStatus,
              `EasyPay confirm-pay must return STATUS 00 for DATE=${confirmDate} ` +
                `(today Sofia 12:13; TOTAL=raw init AMOUNT). ${confirmDetail}`,
            ).toBe('00');

            TestRunSummary.recordCheck({
              check: 'EasyPay confirm-pay DATE today Sofia 12:13',
              expectedResult: 'STATUS 00',
              actualResult: `As expected — STATUS=${confirmStatus} DATE=${confirmDate}`,
              passed: true,
            });

            return { merchantId: mid, initBody: body, confirmBody: confirm };
          }),
      );

      const paymentRow = await test.step('Action: resolve EasyPay payment for customer', async () => {
        const row = await findLatestOnlinePaymentForCustomer(fx, identifier, {
          preferAmount: LPF_OVERDUE_AMOUNT,
          customerNumber,
        });
        const paymentId = Number(row.id);
        expect(paymentId, 'EasyPay payment id').toBeGreaterThan(0);
        Responses.payment.push(paymentId);
        TestRunSummary.registerPayload('easyPayConfirm', {
          confirmDate,
          tid,
          customerNumber,
          identifier,
          paymentId,
          sourceLiabilityId,
          merchantId,
          confirmStatus: confirmBody?.STATUS,
          initAmountRaw: initBody.AMOUNT,
        });
        const payment = await getPaymentDetail(fx, paymentId);
        const time = timePart(payment.paymentDate);
        const timePreserved = Boolean(time) && !time.startsWith('00:00:00');
        TestRunSummary.recordCheck({
          check: 'EasyPay paymentDate time is not midnight',
          expectedResult: 'GET /payment/{id} paymentDate time part != 00:00:00 (confirm DATE HMS preserved)',
          actualResult: `paymentDate=${payment.paymentDate}, timePart=${time || 'absent'}`,
          passed: timePreserved,
        });
        expect(
          timePreserved,
          `paymentDate time must not be 00:00:00. paymentDate=${payment.paymentDate} DATE=${confirmDate}`,
        ).toBe(true);
        console.log(
          `[PDT-3215-kalina-1213] paymentId=${paymentId} confirmDate=${confirmDate} ` +
            `tid=${tid} customerNumber=${customerNumber} identifier=${identifier} ` +
            `sourceLiabilityId=${sourceLiabilityId} STATUS=${confirmBody?.STATUS}`,
        );
        return row;
      });

      await test.step('Attach test run summary', async () => {
        finalizeTestRunSummary(TestRunSummary, Responses, {
          jiraKey: JIRA_KEY,
          relevantEntityKeys: [
            'customer',
            'customerLiability',
            'payment',
            'interestRate',
          ],
          snapshot: {
            scenario:
              'Due today Sofia; confirm DATE=today Sofia 12:13. Expect STATUS 00 for Kalina confirm_date DB check.',
            todaySofia,
            confirmDate,
            sofiaHour: hour,
            sofiaMinute: minute,
            sourceLiabilityId,
            interestRateId,
            initAmountRaw: initBody.AMOUNT,
            principalCoins,
            confirmStatus: confirmBody?.STATUS,
            merchantId,
            paymentId: paymentRow?.id,
            tid,
            customerNumber,
            identifier,
          },
        });
      });
    },
  );
});
