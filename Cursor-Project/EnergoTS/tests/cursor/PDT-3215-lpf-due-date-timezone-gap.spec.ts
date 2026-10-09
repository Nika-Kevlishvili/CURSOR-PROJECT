/**
 * PDT-3215 — EasyPay LPF vs 3-hour Sofia/UTC gap (00:00–02:59 Europe/Sofia).
 *
 * Bug/standalone automation (no new Backend TC .md). Environment: any (set BASE_URL).
 *
 * Question: if due date is a calendar day and the customer pays in the Sofia morning
 * gap (00:00–02:59), can they deserve an LPF (already the next Sofia day) while the
 * system does not assign it because UTC is still the previous calendar day?
 *
 * Afternoon-runnable DATE (do not wait for midnight; do not use a future DATE):
 * confirm DATE = today Sofia 00:15 (`yyyyMMdd` + `001500`) via sofiaYmd()+toEasyPayDate.
 * EasyPay init-pay has no DATE — LPF at init uses server LocalDate.now() (Sofia today).
 * Today 00:15 is in the past this afternoon; EasyPay accepts it (not future).
 * Do NOT use EASYPAY_STORY_DATE 20260801001500 (outside the live OPEN period).
 *
 * Test A — due = yesterday Sofia: init AMOUNT includes LPF. Confirm DATE=today 00:15
 * (= yesterday 21:15 UTC in summer). If confirm/LPF uses Sofia date of DATE → STATUS 00
 * + LPF assigned. If it uses UTC date of DATE (due date) → STATUS 96 amount mismatch
 * or LPF missing. Amount mismatch is the bug under test (hard-fail, never soft-skip).
 *
 * Test B — due = today Sofia: init AMOUNT = principal (no LPF). Confirm same DATE.
 * Expect STATUS 00 and no LPF.
 *
 * REG-1063 / PDT-3214 Test 2: combine=true + 2 overdueLiability + dailyInterestRate.
 * TOTAL = raw initBody.AMOUNT (do not Number()). Shared EasyPay ONLINE channel:
 * withEasyPayChannelLock; prefer --workers=1.
 *
 * Soft-skip ONLY env blockers (merchant not found / EasyPay channel missing /
 * billing-run lock / DATE still future before 00:15 Sofia / paymentDate outside AP).
 * Never skip LPF amount mismatch (that is the timezone-gap bug under test).
 *
 * Reference spec(s):
 * - tests/cursor/PDT-3215-accounting-period-dates-and-more.spec.ts
 * - tests/cursor/pdt-3215-accounting-period-dates-and-more.fixtures.ts
 * - tests/cursor/PDT-3214-easypay-liabilities-receivables-accounting-period-rules.spec.ts (Test 2 LPF)
 * - tests/cursor/pdt-3214-easypay-liabilities-receivables-accounting-period-rules.fixtures.ts
 * - tests/cursor/pdt-3171-easypay-proforma-due-date-validto.fixtures.ts (init/confirm checksum)
 *
 * /epay/* is not in Phoenix core swagger — contract from payment-api + PDT-3171 helpers.
 *
 * Swagger (all envs, update-swagger-specs.ps1 this session):
 * - POST /customer-liability — CustomerLiabilityRequest required: accountingPeriodId,
 *   currencyId, customerId, dueDate, initialAmount, occurrenceDate
 * - CustomerLiabilityResponse.fullOffsetDate, dueDate, currentAmount
 * - POST /interest-rate; GET /latePaymentFine/list (LatePaymentFineListingRequest:
 *   page, size, prompt, searchBy=CUSTOMER, type); POST /latePaymentFine/job → integer
 * - POST /payment, GET /payment/{id}
 */

import { test, expect } from './cursor-test.fixtures';
import { finalizeTestRunSummary } from './shared/manual-verification-links.fixtures';
import {
  addSofiaDays,
  calculateConfirmChecksum,
  calculateInitChecksum,
  callInitPay,
  createLegalCustomer,
  formatEasyPayStatusDetail,
  isoToDdMmYyyy,
  resolveEasyPayMerchantId,
  sofiaYmd,
  toEasyPayDate,
  uniqueTid,
} from './pdt-3215-accounting-period-dates-and-more.fixtures';
import {
  callConfirmPayForLpf,
  createDailyInterestRate,
  createOverdueLiabilityWithInterest,
  ensureEasyPayCombineLiabilities,
  findLatestOnlinePaymentForCustomer,
  getCustomerLiabilityFullDetail,
  toCoinAmount,
  waitForEasyPayLpfCustomerLiability,
  withEasyPayChannelLock,
  type Pdt3214Fx,
} from './pdt-3214-easypay-liabilities-receivables-accounting-period-rules.fixtures';

const JIRA_KEY = 'PDT-3215';
const JIRA_TITLE = 'Change Time for Accounting Period Dates and more';
/** Overdue principal per liability (REG-1063 / PDT-3214 Test 2). */
const LPF_OVERDUE_AMOUNT = 100;
const CONFIRM_SOFIA_HMS = '001500';

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

function utcGapAmountMismatchMessage(opts: {
  confirmDate: string;
  todaySofia: string;
  yesterdaySofia: string;
  detail: string;
}): string {
  return (
    `EasyPay confirm recalculated LPF using the UTC calendar date of DATE ${opts.confirmDate} ` +
    `(Sofia ${opts.todaySofia} 00:15 = previous UTC calendar day, which equals due date ` +
    `${opts.yesterdaySofia}) instead of the Sofia calendar date of DATE. ` +
    `STATUS 96 amount mismatch is the timezone-gap bug under test — not an env skip. ${opts.detail}`
  );
}

async function listLatePaymentFinesForCustomer(
  fx: Pdt3214Fx,
  customerIdentifier: string,
): Promise<{ totalElements: number; content: any[] }> {
  const listRes = await fx.Request.get(
    `${fx.Endpoints.latePaymentFine}/list?page=0&size=50` +
      `&prompt=${encodeURIComponent(customerIdentifier)}` +
      `&type=LATE_PAYMENT_FINE&searchBy=CUSTOMER`,
  );
  await expect(listRes).CheckResponse();
  const body = await listRes.json();
  return {
    totalElements: Number(body.totalElements ?? 0),
    content: (body.content ?? []) as any[],
  };
}

async function postLatePaymentFineJob(fx: Pdt3214Fx): Promise<void> {
  const res = await fx.Request.post(`${fx.Endpoints.latePaymentFine}/job`, {
    // LPF job may take longer in fresh data / CI; avoid 120s timeout flake.
    timeout: 5 * 60 * 1000,
  });
  if (res.status() >= 400) {
    const text = await res.text();
    if (isEasyPayEnvBlocker(`${res.status()} ${text}`)) {
      test.skip(
        true,
        `latePaymentFine/job env blocker HTTP ${res.status()}: ${text.slice(0, 400)}`,
      );
    }
  }
  await expect(res).CheckResponse();
}

test.describe(`[${JIRA_KEY}]: ${JIRA_TITLE}`, {
  tag: ['@receivableManagement', '@payment', '@easypay', '@pdt-3215', '@lpf'],
}, () => {
  // Independent tests. Shared EasyPay ONLINE channel: withEasyPayChannelLock.
  // Prefer --workers=1 for this file.

  test(
    `[${JIRA_KEY}] EasyPay DATE today 00:15 Sofia when due was yesterday — LPF assigned (Sofia calendar not UTC previous day)`,
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
      const yesterdaySofia = addSofiaDays(todaySofia, -1);
      const confirmDate = toEasyPayDate(todaySofia, CONFIRM_SOFIA_HMS);
      const dueDateDdMmYyyy = isoToDdMmYyyy(yesterdaySofia);
      const { hour, minute } = sofiaHourMinute();

      if (hour === 0 && minute < 15) {
        test.skip(
          true,
          `Sofia time ${String(hour).padStart(2, '0')}:${String(minute).padStart(2, '0')} ` +
            `is before 00:15 — DATE ${confirmDate} would be future (EasyPay STATUS 96). ` +
            `Re-run after 00:15 Europe/Sofia; do not use tomorrow DATE.`,
        );
      }

      const { sourceLiabilityId, secondLiabilityId, interestRateId } = await test.step(
        'Precondition: legal customer + daily interest + 2 overdueLiability due yesterday (REG-1063)',
        async () => {
          await createLegalCustomer(fx);
          const interestId = await createDailyInterestRate(fx);
          const postOverdue = async (): Promise<number> =>
            createOverdueLiabilityWithInterest(fx, {
              initialAmount: LPF_OVERDUE_AMOUNT,
              interestRateId: interestId,
              dueDateDdMmYyyy,
              occurrenceDateDdMmYyyy: dueDateDdMmYyyy,
            });
          const liabilityId1 = await postOverdue();
          const liabilityId2 = await postOverdue();
          TestRunSummary.registerPayload('customer', Responses.customer[0]);
          TestRunSummary.registerPayload('interestRate', { id: interestId });
          TestRunSummary.registerPayload('customerLiability', {
            sourceLiabilityId: liabilityId1,
            secondLiabilityId: liabilityId2,
            initialAmount: LPF_OVERDUE_AMOUNT,
            dueDate: dueDateDdMmYyyy,
            occurrenceDate: dueDateDdMmYyyy,
            count: 2,
            note:
              'Due = yesterday Sofia; confirm DATE = today Sofia 00:15 (UTC previous evening)',
          });
          return {
            sourceLiabilityId: liabilityId1,
            secondLiabilityId: liabilityId2,
            interestRateId: interestId,
          };
        },
      );

      const customerNumber = String(Responses.customer[0].customerNumber);
      const identifier = String(Responses.customer[0].identifier);
      const tid = uniqueTid();
      const principalMajor = LPF_OVERDUE_AMOUNT * 2;
      const principalCoins = toCoinAmount(principalMajor);

      const { merchantId, initBody, confirmBody } = await test.step(
        `Action: EasyPay combine=true + init-pay (no DATE) + confirm-pay DATE=${confirmDate}`,
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
              `EasyPay init-pay must return STATUS 00 (combine=true / REG-1063). ` +
                `merchantId=${mid} OnlinePaymentUrl=${OnlinePaymentUrl}. ${initDetail}`,
            ).toBe('00');

            const amountCoins = Number(body.AMOUNT);
            const invoices = Array.isArray(body.INVOICES) ? body.INVOICES : [];
            const invoiceExtra =
              invoices.length > 2 ||
              invoices.some(
                (inv: { AMOUNT?: unknown; amount?: unknown }) =>
                  Number(inv?.AMOUNT ?? inv?.amount ?? 0) > 0 &&
                  Number(inv?.AMOUNT ?? inv?.amount ?? 0) !== toCoinAmount(LPF_OVERDUE_AMOUNT),
              );
            const initIncludesLpf =
              amountCoins > principalCoins || invoiceExtra;
            TestRunSummary.registerPayload('easyPayInit', {
              tid,
              merchantId: mid,
              onlinePaymentUrl: OnlinePaymentUrl,
              confirmDate,
              todaySofia,
              yesterdaySofia,
              status: body.STATUS,
              amountRaw: body.AMOUNT,
              amountCoins,
              principalMajor,
              principalCoins,
              invoiceCount: invoices.length,
              combineLiabilities: true,
            });
            TestRunSummary.recordCheck({
              check: 'EasyPay init-pay AMOUNT includes LPF (Sofia today vs due yesterday)',
              expectedResult:
                `STATUS 00 and AMOUNT coins > principal ${principalCoins} ` +
                `(2 x ${LPF_OVERDUE_AMOUNT}) or INVOICES show extra vs currentAmount`,
              actualResult: initIncludesLpf
                ? `As expected — STATUS=${body.STATUS} AMOUNT=${body.AMOUNT} ` +
                  `principalCoins=${principalCoins} invoices=${invoices.length}`
                : `Not as expected — STATUS=${body.STATUS} AMOUNT=${body.AMOUNT} ` +
                  `principalCoins=${principalCoins} invoices=${JSON.stringify(invoices).slice(0, 400)}`,
              passed: initIncludesLpf,
            });
            expect(
              initIncludesLpf,
              `Init AMOUNT must include LPF (overdue as of Sofia today ${todaySofia}, ` +
                `due=${yesterdaySofia}). AMOUNT=${body.AMOUNT} principalCoins=${principalCoins}. ` +
                initDetail,
            ).toBeTruthy();

            const amountRaw = body.AMOUNT;
            const checksumConfirm = await calculateConfirmChecksum(fx, {
              date: confirmDate,
              customerNumber,
              tid,
              merchantId: mid,
              total: amountRaw,
            });
            const result = await callConfirmPayForLpf(fx, {
              date: confirmDate,
              customerNumber,
              tid,
              merchantId: mid,
              total: amountRaw,
              checksum: checksumConfirm,
            });

            if (result.kind === 'payment_date_outside') {
              TestRunSummary.recordCheck({
                check: 'EasyPay confirm-pay DATE today 00:15 Sofia',
                expectedResult: 'STATUS 00 (Sofia calendar of DATE, not UTC previous day)',
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
            const combined =
              `${confirm?.ADDITIONALINFO ?? ''} ${confirm?.DESCRIPTION ?? ''} ${confirmDetail}`;
            const amountIncorrect =
              combined.toLowerCase().includes('amount is incorrect') ||
              combined.toLowerCase().includes('amount incorrect');

            if (confirmStatus !== '00' && isEasyPayEnvBlocker(confirmDetail)) {
              test.skip(true, `EasyPay confirm-pay env blocker: ${confirmDetail}`);
            }

            if (confirmStatus !== '00' && amountIncorrect) {
              TestRunSummary.recordCheck({
                check: 'EasyPay confirm-pay DATE today 00:15 Sofia (LPF timezone gap)',
                expectedResult:
                  'STATUS 00 — confirm uses Sofia calendar date of DATE (today), matching init LPF',
                actualResult: `Not as expected — ${confirmDetail}`,
                passed: false,
              });
              expect(
                confirmStatus,
                utcGapAmountMismatchMessage({
                  confirmDate,
                  todaySofia,
                  yesterdaySofia,
                  detail: confirmDetail,
                }),
              ).toBe('00');
            }

            expect(
              confirmStatus,
              `EasyPay confirm-pay must return STATUS 00 for DATE=${confirmDate} ` +
                `(today Sofia 00:15; TOTAL=raw init AMOUNT). ${confirmDetail}`,
            ).toBe('00');

            TestRunSummary.recordCheck({
              check: 'EasyPay confirm-pay DATE today 00:15 Sofia',
              expectedResult:
                'STATUS 00 — LPF days from Sofia calendar of DATE, matching init AMOUNT',
              actualResult: `As expected — STATUS=${confirmStatus} DATE=${confirmDate} TOTAL=${amountRaw}`,
              passed: true,
            });

            return { merchantId: mid, initBody: body, confirmBody: confirm };
          }),
      );

      const { lpfId, lpfAmount, sourceAfter } = await test.step(
        'Assert: LPF LATE_PAYMENT_FINE exists; source liability currentAmount 0',
        async () => {
          let listed = await listLatePaymentFinesForCustomer(fx, identifier);
          if (listed.totalElements === 0) {
            await postLatePaymentFineJob(fx);
            listed = await listLatePaymentFinesForCustomer(fx, identifier);
          }

          const hit = await waitForEasyPayLpfCustomerLiability(fx, {
            customerIdentifier: identifier,
            sourceLiabilityIds: [sourceLiabilityId, secondLiabilityId],
          });
          expect(hit.lpfAmount, 'LPF amount must be positive').toBeGreaterThan(0);
          expect(
            String(hit.liabilityDetail.outgoingDocumentType ?? '').toUpperCase(),
            'LPF liability outgoingDocumentType',
          ).toBe('LATE_PAYMENT_FINE');

          const sourceAfterPay = await getCustomerLiabilityFullDetail(fx, sourceLiabilityId);
          expect(
            Number(sourceAfterPay.currentAmount),
            `Source liability ${sourceLiabilityId} currentAmount after confirm`,
          ).toBe(0);

          TestRunSummary.registerPayload('latePaymentFine', {
            lpfId: hit.lpfId,
            lpfAmount: hit.lpfAmount,
            type: 'LATE_PAYMENT_FINE',
            listTotalElements: listed.totalElements,
          });
          TestRunSummary.recordCheck({
            check: 'LPF assigned for overdue paid at Sofia 00:15 (not UTC previous day)',
            expectedResult:
              'LATE_PAYMENT_FINE exists for this customer; source liability currentAmount=0',
            actualResult:
              `As expected — lpfId=${hit.lpfId} amount=${hit.lpfAmount} ` +
              `source.currentAmount=${sourceAfterPay.currentAmount} ` +
              `fullOffsetDate=${sourceAfterPay.fullOffsetDate} dueDate=${sourceAfterPay.dueDate}`,
            passed: true,
          });

          return {
            lpfId: hit.lpfId,
            lpfAmount: hit.lpfAmount,
            sourceAfter: sourceAfterPay,
          };
        },
      );

      const paymentRow = await test.step('Action: resolve EasyPay payment for customer', async () => {
        const row = await findLatestOnlinePaymentForCustomer(fx, identifier, {
          preferAmount: Number(initBody.AMOUNT) / 100,
          customerNumber,
        });
        const paymentId = Number(row.id);
        expect(paymentId, 'EasyPay payment id').toBeGreaterThan(0);
        Responses.payment.push(paymentId);
        return row;
      });

      await test.step('Attach test run summary', async () => {
        finalizeTestRunSummary(TestRunSummary, Responses, {
          jiraKey: JIRA_KEY,
          relevantEntityKeys: [
            'customer',
            'customerLiability',
            'payment',
            'latePaymentFine',
            'interestRate',
          ],
          snapshot: {
            scenario:
              'Due yesterday Sofia; confirm DATE=today 00:15 Sofia (UTC previous evening). ' +
              'Expect LPF from Sofia calendar, not UTC date of DATE.',
            todaySofia,
            yesterdaySofia,
            confirmDate,
            sofiaHour: hour,
            sofiaMinute: minute,
            sourceLiabilityId,
            secondLiabilityId,
            interestRateId,
            lpfId,
            lpfAmount,
            sourceCurrentAmount: sourceAfter.currentAmount,
            sourceFullOffsetDate: sourceAfter.fullOffsetDate,
            sourceDueDate: sourceAfter.dueDate,
            initAmountRaw: initBody.AMOUNT,
            principalCoins,
            confirmStatus: confirmBody.STATUS,
            merchantId,
            paymentId: paymentRow?.id,
            tid,
          },
        });
      });
    },
  );

  test(
    `[${JIRA_KEY}] EasyPay DATE today 00:15 Sofia when due is today — no LPF`,
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

      if (hour === 0 && minute < 15) {
        test.skip(
          true,
          `Sofia time ${String(hour).padStart(2, '0')}:${String(minute).padStart(2, '0')} ` +
            `is before 00:15 — DATE ${confirmDate} would be future. Re-run after 00:15 Europe/Sofia.`,
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
            note: 'Due = today Sofia; confirm DATE = today Sofia 00:15 — no LPF',
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
            const result = await callConfirmPayForLpf(fx, {
              date: confirmDate,
              customerNumber,
              tid,
              merchantId: mid,
              total: amountRaw,
              checksum: checksumConfirm,
            });

            if (result.kind === 'payment_date_outside') {
              TestRunSummary.recordCheck({
                check: 'EasyPay confirm-pay DATE today 00:15 when due is today',
                expectedResult: 'STATUS 00 (due today; no LPF in TOTAL)',
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
                `(due today; TOTAL=raw init AMOUNT). ${confirmDetail}`,
            ).toBe('00');

            TestRunSummary.recordCheck({
              check: 'EasyPay confirm-pay DATE today 00:15 when due is today',
              expectedResult: 'STATUS 00',
              actualResult: `As expected — STATUS=${confirmStatus} DATE=${confirmDate}`,
              passed: true,
            });

            return { merchantId: mid, initBody: body, confirmBody: confirm };
          }),
      );

      await test.step('Assert: latePaymentFine/job + list — no LPF for this customer', async () => {
        await postLatePaymentFineJob(fx);
        const listed = await listLatePaymentFinesForCustomer(fx, identifier);
        const noLpf = listed.totalElements === 0;
        TestRunSummary.recordCheck({
          check: 'No LPF when due is today and DATE is Sofia 00:15',
          expectedResult: 'latePaymentFine/list totalElements=0 (type LATE_PAYMENT_FINE)',
          actualResult: noLpf
            ? `As expected — totalElements=${listed.totalElements}`
            : `Not as expected — totalElements=${listed.totalElements} ` +
              `content=${JSON.stringify(listed.content).slice(0, 400)}`,
          passed: noLpf,
        });
        expect(
          listed.totalElements,
          `No LATE_PAYMENT_FINE expected when due=${todaySofia} and DATE=${confirmDate}. ` +
            `totalElements=${listed.totalElements}`,
        ).toBe(0);
      });

      const paymentRow = await test.step('Action: resolve EasyPay payment for customer', async () => {
        const row = await findLatestOnlinePaymentForCustomer(fx, identifier, {
          preferAmount: LPF_OVERDUE_AMOUNT,
          customerNumber,
        });
        const paymentId = Number(row.id);
        expect(paymentId, 'EasyPay payment id').toBeGreaterThan(0);
        Responses.payment.push(paymentId);
        return row;
      });

      await test.step('Attach test run summary', async () => {
        finalizeTestRunSummary(TestRunSummary, Responses, {
          jiraKey: JIRA_KEY,
          relevantEntityKeys: [
            'customer',
            'customerLiability',
            'payment',
            'latePaymentFine',
            'interestRate',
          ],
          snapshot: {
            scenario:
              'Due today Sofia; confirm DATE=today 00:15 Sofia. Expect no LPF (on due date).',
            todaySofia,
            confirmDate,
            sofiaHour: hour,
            sofiaMinute: minute,
            sourceLiabilityId,
            interestRateId,
            initAmountRaw: initBody.AMOUNT,
            principalCoins,
            confirmStatus: confirmBody.STATUS,
            merchantId,
            paymentId: paymentRow?.id,
            tid,
          },
        });
      });
    },
  );
});
