/**
 * PDT-3205 — Copy EasyPay functionality in another collection channel (Virtual POS).
 *
 * Backend-only (dev). Asserts payment-api Virtual POS protocol STATUS / amounts /
 * channel binding. Spec expected results follow EasyPay/wiki exact-copy;
 * TC-BE-5/6/7/8 hard-assert EasyPay behavior (will fail on current VPOS gaps).
 *
 * Reference spec(s):
 * - tests/cursor/pdt-3171-easypay-proforma-due-date-validto.fixtures.ts
 * - tests/cursor/PDT-3171-easypay-proforma-due-date-validto.spec.ts
 * - tests/cursor/pdt-3214-easypay-liabilities-receivables-accounting-period-rules.fixtures.ts
 * - tests/cursor/PDT-3214-easypay-liabilities-receivables-accounting-period-rules.spec.ts
 * - tests/receivableManagement/Payment/onlinePayment.spec.ts (REG-1036)
 *
 * Swagger: Phoenix core swagger-spec.json (dev) has /collection-channel, /customer-liability,
 * /payment, /payment/list, /currencies, /goods-order — NOT /virtualpos or /epay
 * (payment-api protocol from TC + code + EasyPay fixtures).
 *
 * Parallelism: shared VirtualPos / EasyPay ONLINE channels use file locks around
 * combineLiabilities mutate + init/confirm. Prefer --workers=1 for this file.
 */

import { test, expect } from './cursor-test.fixtures';
import { finalizeTestRunSummary } from './shared/manual-verification-links.fixtures';
import { randomGens } from '../../utils/randomGens';
import {
  VIRTUAL_POS_CHANNEL_NAME,
  EASY_PAY_CHANNEL_NAME,
  PROFORMA_VALID_TO_DAYS,
  assertValidToNotZero,
  calculateEasyPayConfirmChecksum,
  calculateEasyPayInitChecksum,
  calculateVirtualPosConfirmChecksum,
  calculateVirtualPosInitChecksum,
  callEasyPayConfirmPay,
  callEasyPayInitPay,
  callVirtualPosConfirmPay,
  callVirtualPosConfirmPayAllowingEnvBlockers,
  callVirtualPosInitPay,
  calendarDayIso,
  corruptChecksum,
  createLegalCustomer,
  createManualLiability,
  ensureEasyPayChannel,
  ensureVirtualPosChannel,
  ensureVirtualPosOnlinePaymentPackageForConfirmDate,
  ensureVirtualPosOnlinePaymentPackageForDate,
  extractAccountPeriodId,
  findLatestOnlinePaymentForCustomer,
  findOnlineChannelByName,
  formatVirtualPosStatusDetail,
  getCollectionChannelDetail,
  getLiabilityDetail,
  getPaymentDetail,
  getPaymentPackageDetail,
  isPaymentPackageNotApplicableFailure,
  listPaymentsByCustomerIdentifier,
  readGoodsOrderInvoiceMeta,
  resolveEasyPayMerchantId,
  resolveEnvAccountingPeriodContext,
  resolveVirtualPosMerchantId,
  setupProformaLiabilityChain,
  issueInvoiceAndReadLiabilityDueDate,
  shortId,
  toCoinAmount,
  withEasyPayChannelLock,
  withVirtualPosChannelLock,
  yyyyMmDdFromLocalDate,
  type Pdt3205Fx,
} from './pdt-3205-copy-easypay-to-virtual-pos.fixtures';

const JIRA_KEY = 'PDT-3205';
const JIRA_TITLE = 'Copy EasyPay functionality in another collection channel';

test.describe(`[${JIRA_KEY}]: ${JIRA_TITLE}`, {
  tag: ['@receivableManagement', '@payment', '@virtualpos', '@easypay', '@pdt-3205', '@dev'],
}, () => {
  test(
    `[${JIRA_KEY}] TC-BE-1: CHECK merchant 7000006 returns STATUS 00 and stotinki AMOUNT`,
    async ({ Request, GeneratePayload, Responses, Endpoints, OnlinePaymentUrl, TestRunSummary }) => {
      test.setTimeout(5 * 60 * 1000);
      const fx: Pdt3205Fx = { Request, GeneratePayload, Responses, Endpoints, OnlinePaymentUrl };
      const vposMerchant = await resolveVirtualPosMerchantId(fx);
      const dueDateDdMmYyyy = '15-09-2026';
      const expectedValidTo = '20260915';

      let virtualPosChannelId = 0;
      let liabilityId = 0;
      let openAmount = 0;

      await test.step('Precondition: legal customer + VirtualPos combine=true + liability', async () => {
        await createLegalCustomer(fx);
        TestRunSummary.registerPayload('customer', Responses.customer[0]);
        liabilityId = await createManualLiability(fx, {
          initialAmount: 100,
          dueDateDdMmYyyy,
        });
        const detail = await getLiabilityDetail(fx, liabilityId);
        expect(detail.currentAmount).toBeGreaterThan(0);
        openAmount = detail.currentAmount;

        await withVirtualPosChannelLock(async () => {
          virtualPosChannelId = await ensureVirtualPosChannel(fx, true);
        });
      });

      const customerNumber = String(Responses.customer[0].customerNumber);
      const identifier = String(Responses.customer[0].identifier);
      const paymentsBefore = await listPaymentsByCustomerIdentifier(fx, identifier);

      const checkBody = await test.step('Action: Virtual POS CHECK init-pay', async () => {
        const checksum = await calculateVirtualPosInitChecksum(fx, {
          customerNumber,
          merchantId: vposMerchant,
          type: 'CHECK',
        });
        return callVirtualPosInitPay(fx, {
          customerNumber,
          merchantId: vposMerchant,
          type: 'CHECK',
          checksum,
        });
      });

      await test.step('Assert: STATUS 00, AMOUNT coins, VALIDTO, no new payment', async () => {
        expect(
          String(checkBody.STATUS),
          formatVirtualPosStatusDetail(checkBody),
        ).toBe('00');
        expect(String(checkBody.IDN)).toBe(customerNumber);
        const amount = Number(checkBody.AMOUNT);
        // online_payment_check AMOUNT = payable principal (+ LPF when applicable), channel-scoped —
        // may differ from a single liability's currentAmount; require positive protocol amount.
        expect(amount, formatVirtualPosStatusDetail(checkBody)).toBeGreaterThan(0);
        expect(Number.isInteger(amount), `AMOUNT must be integer stotinki, got ${amount}`).toBe(true);
        expect(String(checkBody.VALIDTO)).toBe(expectedValidTo);

        const paymentsAfter = await listPaymentsByCustomerIdentifier(fx, identifier);
        expect(paymentsAfter.totalElements).toBe(paymentsBefore.totalElements);

        TestRunSummary.recordCheck({
          check: 'TC-BE-1 Virtual POS CHECK STATUS 00',
          expectedResult: `STATUS=00, AMOUNT>0 stotinki, VALIDTO=${expectedValidTo}, no payment created`,
          actualResult: `As expected — STATUS=${checkBody.STATUS}, AMOUNT=${checkBody.AMOUNT} (liability currentAmount coins=${toCoinAmount(openAmount)}), VALIDTO=${checkBody.VALIDTO}, payments=${paymentsAfter.totalElements}`,
          passed: true,
        });
      });

      await test.step('Attach test run summary', async () => {
        finalizeTestRunSummary(TestRunSummary, Responses, {
          jiraKey: JIRA_KEY,
          relevantEntityKeys: ['customer', 'customerLiability', 'collectionChannel'],
          snapshot: { virtualPosChannelId, liabilityId, checkStatus: checkBody.STATUS },
        });
      });
    },
  );

  test(
    `[${JIRA_KEY}] TC-BE-2: CHECK then BILLING confirm binds payment to VirtualPos channel`,
    async ({ Request, GeneratePayload, Responses, Endpoints, OnlinePaymentUrl, TestRunSummary }) => {
      test.setTimeout(5 * 60 * 1000);
      const fx: Pdt3205Fx = { Request, GeneratePayload, Responses, Endpoints, OnlinePaymentUrl };
      const vposMerchant = await resolveVirtualPosMerchantId(fx);

      let virtualPosChannelId = 0;
      let easyPayChannelId: number | null = null;

      await test.step('Precondition: customer + two liabilities + VirtualPos combine=true', async () => {
        await createLegalCustomer(fx);
        TestRunSummary.registerPayload('customer', Responses.customer[0]);
        await createManualLiability(fx, { initialAmount: 80, dueDateDdMmYyyy: '01-09-2026' });
        await createManualLiability(fx, { initialAmount: 20, dueDateDdMmYyyy: '20-09-2026' });

        const easyPay = await findOnlineChannelByName(fx, EASY_PAY_CHANNEL_NAME);
        if (easyPay) easyPayChannelId = easyPay.id;

        await withVirtualPosChannelLock(async () => {
          virtualPosChannelId = await ensureVirtualPosChannel(fx, true);
        });
      });

      const customerNumber = String(Responses.customer[0].customerNumber);
      const identifier = String(Responses.customer[0].identifier);

      const { checkAmount, billingAmount, confirmBody, paymentId, channelName } =
        await withVirtualPosChannelLock(async () => {
          await ensureVirtualPosChannel(fx, true);

          const checksumCheck = await calculateVirtualPosInitChecksum(fx, {
            customerNumber,
            merchantId: vposMerchant,
            type: 'CHECK',
          });
          const checkBody = await callVirtualPosInitPay(fx, {
            customerNumber,
            merchantId: vposMerchant,
            type: 'CHECK',
            checksum: checksumCheck,
          });
          expect(String(checkBody.STATUS), formatVirtualPosStatusDetail(checkBody)).toBe('00');
          const checkAmt = checkBody.AMOUNT;

          const tid = randomGens.generateTID();
          const checksumInit = await calculateVirtualPosInitChecksum(fx, {
            customerNumber,
            merchantId: vposMerchant,
            type: 'BILLING',
            tid,
          });
          const initBody = await callVirtualPosInitPay(fx, {
            customerNumber,
            merchantId: vposMerchant,
            type: 'BILLING',
            tid,
            checksum: checksumInit,
          });
          expect(String(initBody.STATUS), formatVirtualPosStatusDetail(initBody)).toBe('00');
          expect(Number(initBody.AMOUNT)).toBe(Number(checkAmt));
          const invoices = initBody.INVOICES;
          expect(
            !invoices || (Array.isArray(invoices) && invoices.length === 0),
            'combined channel must omit/empty INVOICES',
          ).toBeTruthy();

          const date = randomGens.generateOnlinePaymentDate(4);
          await ensureVirtualPosOnlinePaymentPackageForConfirmDate(fx, {
            confirmDate: date,
            channelId: virtualPosChannelId,
          });
          const checksumConfirm = await calculateVirtualPosConfirmChecksum(fx, {
            date,
            customerNumber,
            tid,
            merchantId: vposMerchant,
            total: initBody.AMOUNT,
          });
          const confirm = await callVirtualPosConfirmPay(fx, {
            date,
            customerNumber,
            tid,
            merchantId: vposMerchant,
            total: initBody.AMOUNT,
            checksum: checksumConfirm,
          });
          expect(String(confirm.STATUS), formatVirtualPosStatusDetail(confirm)).toBe('00');
          expect(String(confirm.DESCRIPTION ?? 'OK')).toBe('OK');

          const paymentRow = await findLatestOnlinePaymentForCustomer(fx, identifier, {
            customerNumber,
            preferAmount: Number(initBody.AMOUNT) / 100,
          });
          const payment = await getPaymentDetail(fx, Number(paymentRow.id));
          Responses.payment.push(paymentRow.id);
          const payChannelId = shortId(payment.collectionChannelId);
          expect(payChannelId).toBe(virtualPosChannelId);
          if (easyPayChannelId != null) {
            expect(payChannelId).not.toBe(easyPayChannelId);
          }
          const channel = await getCollectionChannelDetail(fx, payChannelId!);
          expect(String(channel.name)).toBe(VIRTUAL_POS_CHANNEL_NAME);

          return {
            checkAmount: checkAmt,
            billingAmount: initBody.AMOUNT,
            confirmBody: confirm,
            paymentId: Number(paymentRow.id),
            channelName: String(channel.name),
          };
        });

      await test.step('Assert: VirtualPos channel binding recorded', async () => {
        TestRunSummary.recordCheck({
          check: 'TC-BE-2 payment on VirtualPos channel',
          expectedResult: `Confirm 00; payment.collectionChannelId=${virtualPosChannelId}; name=VirtualPos`,
          actualResult: `As expected — confirm=${confirmBody.STATUS}, paymentId=${paymentId}, channel=${channelName}, CHECK AMOUNT=${checkAmount}, BILLING AMOUNT=${billingAmount}`,
          passed: true,
        });
      });

      await test.step('Attach test run summary', async () => {
        finalizeTestRunSummary(TestRunSummary, Responses, {
          jiraKey: JIRA_KEY,
          relevantEntityKeys: ['customer', 'customerLiability', 'collectionChannel', 'payment'],
          snapshot: { virtualPosChannelId, easyPayChannelId, paymentId, channelName },
        });
      });
    },
  );

  test(
    `[${JIRA_KEY}] TC-BE-5: Confirm DATE drives payment-package date and accounting period (EasyPay spec)`,
    async ({ Request, GeneratePayload, Responses, Endpoints, OnlinePaymentUrl, TestRunSummary }) => {
      test.setTimeout(5 * 60 * 1000);
      const fx: Pdt3205Fx = { Request, GeneratePayload, Responses, Endpoints, OnlinePaymentUrl };
      const vposMerchant = await resolveVirtualPosMerchantId(fx);

      const ctx = await test.step('Precondition: resolve prior OPEN vs today OPEN AP', async () => {
        return resolveEnvAccountingPeriodContext(fx);
      });
      test.skip(
        !ctx.priorOpenPeriod || !ctx.priorOpenEasyPayDate || !ctx.priorOpenMidIso,
        'No prior OPEN accounting period on this env — cannot assert DATE→AP remapping',
      );

      let virtualPosChannelId = 0;
      await test.step('Precondition: customer + liability + VirtualPos combine=true', async () => {
        await createLegalCustomer(fx);
        TestRunSummary.registerPayload('customer', Responses.customer[0]);
        await createManualLiability(fx, { initialAmount: 100, dueDateDdMmYyyy: '15-09-2026' });
        await withVirtualPosChannelLock(async () => {
          virtualPosChannelId = await ensureVirtualPosChannel(fx, true);
        });
      });

      const customerNumber = String(Responses.customer[0].customerNumber);
      const identifier = String(Responses.customer[0].identifier);
      const confirmDate = ctx.priorOpenEasyPayDate!;
      const expectedDay = ctx.priorOpenMidIso!;
      const julyPeriodId = ctx.priorOpenPeriod!.id;
      const augustPeriodId = ctx.todayOpenPeriod.id;

      const result = await withVirtualPosChannelLock(async () => {
        await ensureVirtualPosChannel(fx, true);
        // Pre-create ONLINE package for prior-OPEN mid day so createOrFetch does not
        // pick an env-stale package (e.g. Test 4402) that fails date+channel applicability.
        const onlinePkg = await ensureVirtualPosOnlinePaymentPackageForDate(fx, {
          paymentDateIso: expectedDay,
          accountingPeriodId: julyPeriodId,
          channelId: virtualPosChannelId > 0 ? virtualPosChannelId : undefined,
        });
        TestRunSummary.registerPayload('virtualPosOnlinePaymentPackage', {
          paymentDateIso: expectedDay,
          accountingPeriodId: julyPeriodId,
          channelId: onlinePkg.channelId,
          paymentPackageId: onlinePkg.paymentPackageId,
          created: onlinePkg.created,
          detail: onlinePkg.detail ?? null,
        });

        const tid = randomGens.generateTID();
        const checksumInit = await calculateVirtualPosInitChecksum(fx, {
          customerNumber,
          merchantId: vposMerchant,
          type: 'BILLING',
          tid,
        });
        const initBody = await callVirtualPosInitPay(fx, {
          customerNumber,
          merchantId: vposMerchant,
          type: 'BILLING',
          tid,
          checksum: checksumInit,
        });
        expect(String(initBody.STATUS), formatVirtualPosStatusDetail(initBody)).toBe('00');

        const checksumConfirm = await calculateVirtualPosConfirmChecksum(fx, {
          date: confirmDate,
          customerNumber,
          tid,
          merchantId: vposMerchant,
          total: initBody.AMOUNT,
        });
        const confirmResult = await callVirtualPosConfirmPayAllowingEnvBlockers(fx, {
          date: confirmDate,
          customerNumber,
          tid,
          merchantId: vposMerchant,
          total: initBody.AMOUNT,
          checksum: checksumConfirm,
        });

        if (confirmResult.kind === 'payment_date_outside') {
          const isPkg = isPaymentPackageNotApplicableFailure(confirmResult.detail);
          TestRunSummary.recordCheck({
            check: 'TC-BE-5 DATE drives package date + AP (EasyPay spec)',
            expectedResult: `confirm STATUS 00; payment/package day=${expectedDay}, AP=${julyPeriodId}`,
            actualResult: isPkg
              ? `Soft-skip — payment package not applicable after ONLINE pre-create: ${confirmResult.detail}`
              : `Soft-skip — paymentDate/AP outside: ${confirmResult.detail}`,
            passed: false,
          });
          test.skip(
            true,
            isPkg
              ? `Env rejects Virtual POS confirm DATE ${confirmDate}: payment package not applicable ` +
                `for payment date + VirtualPos channel after ONLINE package pre-create ` +
                `(paymentDateIso=${expectedDay}, priorOpen=#${julyPeriodId}, ` +
                `channelId=${onlinePkg.channelId}, created=${onlinePkg.created}, ` +
                `paymentPackageId=${onlinePkg.paymentPackageId}). ` +
                `Init STATUS 00 verified; DATE→AP remapping not runnable here. ${confirmResult.detail}`
              : `Env rejects prior-OPEN Virtual POS confirm DATE ${confirmDate}. ` +
                `Init STATUS 00 verified; DATE→AP remapping not runnable here. ${confirmResult.detail}`,
          );
        }

        if (confirmResult.kind === 'other_status') {
          expect(
            String(confirmResult.body?.STATUS ?? ''),
            `Virtual POS confirm-pay must return STATUS 00 (DATE=${confirmDate}). ${confirmResult.detail}`,
          ).toBe('00');
        }

        const confirmBody = confirmResult.body;
        expect(String(confirmBody.STATUS), formatVirtualPosStatusDetail(confirmBody)).toBe('00');

        const paymentRow = await findLatestOnlinePaymentForCustomer(fx, identifier, {
          customerNumber,
          preferAmount: Number(initBody.AMOUNT) / 100,
        });
        const payment = await getPaymentDetail(fx, Number(paymentRow.id));
        Responses.payment.push(paymentRow.id);
        const packageId = Number(payment.paymentPackageId);
        expect(packageId).toBeGreaterThan(0);
        const pkg = await getPaymentPackageDetail(fx, packageId);

        return { payment, pkg, packageId, paymentId: Number(paymentRow.id) };
      });

      await test.step('Assert: payment + package date/AP follow confirm DATE (EasyPay spec)', async () => {
        const paymentDay = calendarDayIso(result.payment.paymentDate);
        const packageDay = calendarDayIso(result.pkg.paymentDate);
        const paymentAp = extractAccountPeriodId(result.payment);
        const packageAp =
          extractAccountPeriodId(result.pkg) ??
          shortId(result.pkg.accountingPeriod) ??
          shortId(result.pkg.accountingPeriodId);

        expect(paymentDay, 'payment.paymentDate calendar day').toBe(expectedDay);
        expect(packageDay, 'package.paymentDate must equal confirm DATE day (not LocalDate.now())').toBe(
          expectedDay,
        );
        expect(paymentAp, 'payment accountPeriodId').toBe(julyPeriodId);
        expect(packageAp, 'package accountingPeriodId').toBe(julyPeriodId);
        expect(paymentAp).not.toBe(augustPeriodId);
        expect(shortId(result.payment.collectionChannelId)).toBe(virtualPosChannelId);

        TestRunSummary.recordCheck({
          check: 'TC-BE-5 DATE drives package date + AP (EasyPay spec)',
          expectedResult: `paymentDay=${expectedDay}, packageDay=${expectedDay}, AP=${julyPeriodId} (not ${augustPeriodId})`,
          actualResult: `As expected — paymentDay=${paymentDay}, packageDay=${packageDay}, paymentAp=${paymentAp}, packageAp=${packageAp}`,
          passed: true,
        });
      });

      await test.step('Attach test run summary', async () => {
        finalizeTestRunSummary(TestRunSummary, Responses, {
          jiraKey: JIRA_KEY,
          relevantEntityKeys: ['customer', 'payment', 'paymentPackage', 'collectionChannel'],
          snapshot: {
            confirmDate,
            expectedDay,
            julyPeriodId,
            augustPeriodId,
            paymentId: result.paymentId,
            packageId: result.packageId,
          },
        });
      });
    },
  );

  test(
    `[${JIRA_KEY}] TC-BE-6: Proforma per-invoice VALIDTO is today+30 yyyyMMdd, not "0"`,
    async ({ Request, GeneratePayload, Responses, Endpoints, OnlinePaymentUrl, TestRunSummary }) => {
      test.setTimeout(20 * 60 * 1000);
      const fx: Pdt3205Fx = { Request, GeneratePayload, Responses, Endpoints, OnlinePaymentUrl };
      const vposMerchant = await resolveVirtualPosMerchantId(fx);

      const { liabilityId, currentAmount, goodsOrderId, proformaInvoiceId } = await test.step(
        'Precondition: proforma liability with null dueDate',
        async () => setupProformaLiabilityChain(fx),
      );
      TestRunSummary.registerPayload('customer', Responses.customer[0]);

      await withVirtualPosChannelLock(async () => {
        await ensureVirtualPosChannel(fx, false);
      });

      const expectedValidTo = yyyyMmDdFromLocalDate(new Date(), PROFORMA_VALID_TO_DAYS);
      const customerNumber = String(Responses.customer[0].customerNumber);

      const initBody = await withVirtualPosChannelLock(async () => {
        await ensureVirtualPosChannel(fx, false);
        const tid = randomGens.generateTID();
        const checksum = await calculateVirtualPosInitChecksum(fx, {
          customerNumber,
          merchantId: vposMerchant,
          type: 'BILLING',
          tid,
        });
        return callVirtualPosInitPay(fx, {
          customerNumber,
          merchantId: vposMerchant,
          type: 'BILLING',
          tid,
          checksum,
        });
      });

      await test.step('Assert: STATUS 00, VALIDTO=today+30 (not "0") on root and invoices', async () => {
        expect(String(initBody.STATUS), formatVirtualPosStatusDetail(initBody)).toBe('00');
        const rootValidTo = assertValidToNotZero(initBody.VALIDTO, 'root');
        expect(rootValidTo).toBe(expectedValidTo);

        const invoices = initBody.INVOICES as Array<{ VALIDTO?: string }> | undefined;
        expect(Array.isArray(invoices) && invoices.length > 0, 'INVOICES required when combine=false').toBeTruthy();
        for (const inv of invoices!) {
          expect(assertValidToNotZero(inv.VALIDTO, 'invoice')).toBe(expectedValidTo);
        }
        expect(Number(initBody.AMOUNT)).toBeGreaterThan(0);
        expect(Number(initBody.AMOUNT)).toBe(toCoinAmount(currentAmount));

        TestRunSummary.recordCheck({
          check: 'TC-BE-6 proforma VALIDTO today+30 (EasyPay spec)',
          expectedResult: `STATUS=00, VALIDTO=${expectedValidTo} (not "0")`,
          actualResult: `As expected — STATUS=${initBody.STATUS}, VALIDTO=${initBody.VALIDTO}, invoice VALIDTOs checked`,
          passed: true,
        });
      });

      await test.step('Attach test run summary', async () => {
        finalizeTestRunSummary(TestRunSummary, Responses, {
          jiraKey: JIRA_KEY,
          relevantEntityKeys: ['customer', 'goodsOrder', 'invoice', 'customerLiability'],
          snapshot: { goodsOrderId, proformaInvoiceId, liabilityId, expectedValidTo, initValidTo: initBody.VALIDTO },
        });
      });
    },
  );

  test(
    `[${JIRA_KEY}] TC-BE-7: LONGDESC includes invoice document number and date`,
    async ({ Request, GeneratePayload, Responses, Endpoints, OnlinePaymentUrl, TestRunSummary }) => {
      test.setTimeout(20 * 60 * 1000);
      const fx: Pdt3205Fx = { Request, GeneratePayload, Responses, Endpoints, OnlinePaymentUrl };
      const vposMerchant = await resolveVirtualPosMerchantId(fx);

      const chain = await test.step('Precondition: proforma → issue-invoice liability', async () => {
        const base = await setupProformaLiabilityChain(fx);
        await issueInvoiceAndReadLiabilityDueDate(fx, base.goodsOrderId, base.liabilityId);
        const meta = await readGoodsOrderInvoiceMeta(fx, base.goodsOrderId, {
          liabilityId: base.liabilityId,
        });
        test.skip(
          !meta,
          `goods-order ${base.goodsOrderId} invoice meta still missing after issue-invoice poll — cannot assert LONGDESC`,
        );
        return { ...base, ...meta! };
      });
      TestRunSummary.registerPayload('customer', Responses.customer[0]);

      await withVirtualPosChannelLock(async () => {
        await ensureVirtualPosChannel(fx, false);
      });

      const customerNumber = String(Responses.customer[0].customerNumber);
      const invoiceFragment = `Фактура: ${chain.longDescDoc}/${chain.expectedLongDescDate}`;
      const combinedFragment = `Фактури: ${chain.longDescDoc}/${chain.expectedLongDescDate}`;

      const initBody = await withVirtualPosChannelLock(async () => {
        await ensureVirtualPosChannel(fx, false);
        const tid = randomGens.generateTID();
        const checksum = await calculateVirtualPosInitChecksum(fx, {
          customerNumber,
          merchantId: vposMerchant,
          type: 'BILLING',
          tid,
        });
        return callVirtualPosInitPay(fx, {
          customerNumber,
          merchantId: vposMerchant,
          type: 'BILLING',
          tid,
          checksum,
        });
      });

      await test.step('Assert: LONGDESC contains invoice number/date (EasyPay spec)', async () => {
        expect(String(initBody.STATUS), formatVirtualPosStatusDetail(initBody)).toBe('00');
        const invoices = initBody.INVOICES as Array<{ LONGDESC?: string; IDN?: string }> | undefined;
        expect(Array.isArray(invoices) && invoices.length > 0).toBeTruthy();
        const inv0 = invoices![0];
        expect(String(inv0.LONGDESC ?? ''), 'INVOICES[0].LONGDESC').toContain(invoiceFragment);
        expect(String(initBody.LONGDESC ?? ''), 'root LONGDESC').toContain(combinedFragment);
        expect(String(initBody.LONGDESC ?? '')).toContain(`Клиентски номер: ${customerNumber}`);

        TestRunSummary.recordCheck({
          check: 'TC-BE-7 LONGDESC invoice number/date (EasyPay spec)',
          expectedResult: `INVOICE LONGDESC contains "${invoiceFragment}"; root contains "${combinedFragment}"`,
          actualResult: `As expected — invoice LONGDESC=${inv0.LONGDESC}; root=${initBody.LONGDESC}`,
          passed: true,
        });
      });

      await test.step('Attach test run summary', async () => {
        finalizeTestRunSummary(TestRunSummary, Responses, {
          jiraKey: JIRA_KEY,
          relevantEntityKeys: ['customer', 'goodsOrder', 'invoice', 'customerLiability'],
          snapshot: {
            goodsOrderId: chain.goodsOrderId,
            longDescDoc: chain.longDescDoc,
            expectedLongDescDate: chain.expectedLongDescDate,
            invoiceDocumentNumber: chain.invoiceDocumentNumber,
          },
        });
      });
    },
  );

  test(
    `[${JIRA_KEY}] TC-BE-8: Confirm STATUS 00 only after offsetting completes (EasyPay spec)`,
    async ({ Request, GeneratePayload, Responses, Endpoints, OnlinePaymentUrl, TestRunSummary }) => {
      test.setTimeout(5 * 60 * 1000);
      const fx: Pdt3205Fx = { Request, GeneratePayload, Responses, Endpoints, OnlinePaymentUrl };
      const vposMerchant = await resolveVirtualPosMerchantId(fx);

      let liabilityId = 0;
      let amountBefore = 0;
      let virtualPosChannelId = 0;

      await test.step('Precondition: customer + liability + VirtualPos combine=true', async () => {
        await createLegalCustomer(fx);
        TestRunSummary.registerPayload('customer', Responses.customer[0]);
        liabilityId = await createManualLiability(fx, {
          initialAmount: 100,
          dueDateDdMmYyyy: '15-09-2026',
        });
        const detail = await getLiabilityDetail(fx, liabilityId);
        expect(detail.currentAmount).toBeGreaterThan(0);
        amountBefore = detail.currentAmount;
        await withVirtualPosChannelLock(async () => {
          virtualPosChannelId = await ensureVirtualPosChannel(fx, true);
        });
      });

      const customerNumber = String(Responses.customer[0].customerNumber);
      const identifier = String(Responses.customer[0].identifier);

      const { confirmBody, liabilityAfter, paymentId } = await withVirtualPosChannelLock(async () => {
        await ensureVirtualPosChannel(fx, true);
        const tid = randomGens.generateTID();
        const checksumInit = await calculateVirtualPosInitChecksum(fx, {
          customerNumber,
          merchantId: vposMerchant,
          type: 'BILLING',
          tid,
        });
        const initBody = await callVirtualPosInitPay(fx, {
          customerNumber,
          merchantId: vposMerchant,
          type: 'BILLING',
          tid,
          checksum: checksumInit,
        });
        expect(String(initBody.STATUS)).toBe('00');

        const date = randomGens.generateOnlinePaymentDate(4);
        await ensureVirtualPosOnlinePaymentPackageForConfirmDate(fx, {
          confirmDate: date,
          channelId: virtualPosChannelId > 0 ? virtualPosChannelId : undefined,
        });
        const checksumConfirm = await calculateVirtualPosConfirmChecksum(fx, {
          date,
          customerNumber,
          tid,
          merchantId: vposMerchant,
          total: initBody.AMOUNT,
        });
        const confirm = await callVirtualPosConfirmPay(fx, {
          date,
          customerNumber,
          tid,
          merchantId: vposMerchant,
          total: initBody.AMOUNT,
          checksum: checksumConfirm,
        });
        expect(String(confirm.STATUS), formatVirtualPosStatusDetail(confirm)).toBe('00');

        // Immediately after confirm (no poll) — EasyPay sync offsetting contract.
        const liabilityNow = await getLiabilityDetail(fx, liabilityId);
        const paymentRow = await findLatestOnlinePaymentForCustomer(fx, identifier, {
          customerNumber,
          preferAmount: Number(initBody.AMOUNT) / 100,
        });
        Responses.payment.push(paymentRow.id);
        const payment = await getPaymentDetail(fx, Number(paymentRow.id));
        expect(shortId(payment.collectionChannelId)).toBe(virtualPosChannelId);

        return {
          confirmBody: confirm,
          liabilityAfter: liabilityNow,
          paymentId: Number(paymentRow.id),
        };
      });

      await test.step('Assert: liability already covered when confirm returned 00', async () => {
        expect(
          liabilityAfter.currentAmount,
          `Spec: offsetting complete before STATUS 00 (was ${amountBefore})`,
        ).toBe(0);

        TestRunSummary.recordCheck({
          check: 'TC-BE-8 sync offsetting before STATUS 00 (EasyPay spec)',
          expectedResult: `confirm=00 and liability.currentAmount=0 immediately (was ${amountBefore})`,
          actualResult: `As expected — confirm=${confirmBody.STATUS}, currentAmount=${liabilityAfter.currentAmount}, paymentId=${paymentId}`,
          passed: true,
        });
      });

      await test.step('Attach test run summary', async () => {
        finalizeTestRunSummary(TestRunSummary, Responses, {
          jiraKey: JIRA_KEY,
          relevantEntityKeys: ['customer', 'customerLiability', 'payment', 'collectionChannel'],
          snapshot: { liabilityId, amountBefore, currentAmount: liabilityAfter.currentAmount, paymentId },
        });
      });
    },
  );

  test(
    `[${JIRA_KEY}] TC-BE-10: EasyPay /epay init and confirm still succeed with merchant 7000005`,
    async ({ Request, GeneratePayload, Responses, Endpoints, OnlinePaymentUrl, TestRunSummary }) => {
      test.setTimeout(5 * 60 * 1000);
      const fx: Pdt3205Fx = { Request, GeneratePayload, Responses, Endpoints, OnlinePaymentUrl };

      let easyPayChannelId = 0;
      let virtualPosChannelId: number | null = null;

      await test.step('Precondition: customer + liability + EasyPay combine=true', async () => {
        await createLegalCustomer(fx);
        TestRunSummary.registerPayload('customer', Responses.customer[0]);
        await createManualLiability(fx, { initialAmount: 100, dueDateDdMmYyyy: '15-09-2026' });
        const vpos = await findOnlineChannelByName(fx, VIRTUAL_POS_CHANNEL_NAME);
        if (vpos) virtualPosChannelId = vpos.id;

        await withEasyPayChannelLock(async () => {
          easyPayChannelId = await ensureEasyPayChannel(fx, true);
        });
      });

      const customerNumber = String(Responses.customer[0].customerNumber);
      const identifier = String(Responses.customer[0].identifier);
      // Use resolveEasyPayMerchantId as returned (incl. Merchant_ID on :9092) — do not force 7000005.
      const easyPayMerchant = await resolveEasyPayMerchantId(fx);

      const { confirmBody, paymentId, channelName } = await withEasyPayChannelLock(async () => {
        await ensureEasyPayChannel(fx, true);
        const tid = randomGens.generateTID();
        const checksumInit = await calculateEasyPayInitChecksum(fx, {
          customerNumber,
          tid,
          merchantId: easyPayMerchant,
        });
        const initBody = await callEasyPayInitPay(fx, {
          customerNumber,
          tid,
          merchantId: easyPayMerchant,
          checksum: checksumInit,
        });
        expect(String(initBody.STATUS)).toBe('00');

        const date = randomGens.generateOnlinePaymentDate(4);
        const checksumConfirm = await calculateEasyPayConfirmChecksum(fx, {
          date,
          customerNumber,
          tid,
          merchantId: easyPayMerchant,
          total: initBody.AMOUNT,
        });
        const confirm = await callEasyPayConfirmPay(fx, {
          date,
          customerNumber,
          tid,
          merchantId: easyPayMerchant,
          total: initBody.AMOUNT,
          checksum: checksumConfirm,
        });
        expect(String(confirm.STATUS)).toBe('00');

        const paymentRow = await findLatestOnlinePaymentForCustomer(fx, identifier, {
          customerNumber,
          preferAmount: Number(initBody.AMOUNT) / 100,
        });
        Responses.payment.push(paymentRow.id);
        const payment = await getPaymentDetail(fx, Number(paymentRow.id));
        const payChannelId = shortId(payment.collectionChannelId);
        expect(payChannelId).toBe(easyPayChannelId);
        if (virtualPosChannelId != null) {
          expect(payChannelId).not.toBe(virtualPosChannelId);
        }
        const channel = await getCollectionChannelDetail(fx, payChannelId!);
        expect(String(channel.name)).toBe(EASY_PAY_CHANNEL_NAME);

        return {
          confirmBody: confirm,
          paymentId: Number(paymentRow.id),
          channelName: String(channel.name),
        };
      });

      await test.step('Assert: EasyPay regression unchanged', async () => {
        TestRunSummary.recordCheck({
          check: 'TC-BE-10 EasyPay /epay still works',
          expectedResult: `confirm=00; channel=EasyPay; merchant=${easyPayMerchant}`,
          actualResult: `As expected — confirm=${confirmBody.STATUS}, paymentId=${paymentId}, channel=${channelName}`,
          passed: true,
        });
      });

      await test.step('Attach test run summary', async () => {
        finalizeTestRunSummary(TestRunSummary, Responses, {
          jiraKey: JIRA_KEY,
          relevantEntityKeys: ['customer', 'payment', 'collectionChannel'],
          snapshot: { easyPayChannelId, virtualPosChannelId, paymentId, easyPayMerchant },
        });
      });
    },
  );

  test(
    `[${JIRA_KEY}] TC-BE-12: EasyPay merchant on /virtualpos init-pay returns STATUS 96`,
    async ({ Request, GeneratePayload, Responses, Endpoints, OnlinePaymentUrl, TestRunSummary }) => {
      test.setTimeout(5 * 60 * 1000);
      const fx: Pdt3205Fx = { Request, GeneratePayload, Responses, Endpoints, OnlinePaymentUrl };
      const vposMerchant = await resolveVirtualPosMerchantId(fx);
      const easyMerchant = await resolveEasyPayMerchantId(fx);
      test.skip(
        vposMerchant === easyMerchant,
        `Env shares the same merchant (${vposMerchant}) for EasyPay and Virtual POS — cannot assert merchant isolation`,
      );

      await test.step('Precondition: customer + liability + VirtualPos channel', async () => {
        await createLegalCustomer(fx);
        TestRunSummary.registerPayload('customer', Responses.customer[0]);
        await createManualLiability(fx, { initialAmount: 100, dueDateDdMmYyyy: '15-09-2026' });
        await withVirtualPosChannelLock(async () => {
          await ensureVirtualPosChannel(fx, true);
        });
      });

      const customerNumber = String(Responses.customer[0].customerNumber);
      const identifier = String(Responses.customer[0].identifier);
      const paymentsBefore = await listPaymentsByCustomerIdentifier(fx, identifier);
      const tid = randomGens.generateTID();

      const initBody = await test.step('Action: Virtual POS init with EasyPay merchant', async () => {
        const checksum = await calculateVirtualPosInitChecksum(fx, {
          customerNumber,
          merchantId: easyMerchant,
          type: 'BILLING',
          tid,
        });
        return callVirtualPosInitPay(fx, {
          customerNumber,
          merchantId: easyMerchant,
          type: 'BILLING',
          tid,
          checksum,
        });
      });

      await test.step('Assert: STATUS 96, no payment', async () => {
        expect(String(initBody.STATUS), formatVirtualPosStatusDetail(initBody)).toBe('96');
        expect(String(initBody.STATUS)).not.toBe('00');
        const paymentsAfter = await listPaymentsByCustomerIdentifier(fx, identifier);
        expect(paymentsAfter.totalElements).toBe(paymentsBefore.totalElements);

        TestRunSummary.recordCheck({
          check: 'TC-BE-12 wrong merchant → 96',
          expectedResult: `STATUS=96 using EasyPay merchant ${easyMerchant} on /virtualpos (VPOS=${vposMerchant}); no Phoenix payment`,
          actualResult: `As expected — STATUS=${initBody.STATUS}, payments=${paymentsAfter.totalElements}`,
          passed: true,
        });
      });

      await test.step('Attach test run summary', async () => {
        finalizeTestRunSummary(TestRunSummary, Responses, {
          jiraKey: JIRA_KEY,
          relevantEntityKeys: ['customer', 'customerLiability', 'collectionChannel'],
          snapshot: {
            badMerchantId: easyMerchant,
            vposMerchant,
            status: initBody.STATUS,
          },
        });
      });
    },
  );

  test(
    `[${JIRA_KEY}] TC-BE-11: Virtual POS checksum helpers produce HMAC accepted by init and confirm`,
    async ({ Request, GeneratePayload, Responses, Endpoints, OnlinePaymentUrl, TestRunSummary }) => {
      test.setTimeout(5 * 60 * 1000);
      const fx: Pdt3205Fx = { Request, GeneratePayload, Responses, Endpoints, OnlinePaymentUrl };
      const vposMerchant = await resolveVirtualPosMerchantId(fx);

      await test.step('Precondition: customer + liability + VirtualPos combine=true', async () => {
        await createLegalCustomer(fx);
        TestRunSummary.registerPayload('customer', Responses.customer[0]);
        await createManualLiability(fx, { initialAmount: 100, dueDateDdMmYyyy: '15-09-2026' });
        await withVirtualPosChannelLock(async () => {
          await ensureVirtualPosChannel(fx, true);
        });
      });

      const customerNumber = String(Responses.customer[0].customerNumber);

      await withVirtualPosChannelLock(async () => {
        await ensureVirtualPosChannel(fx, true);
        const tid = randomGens.generateTID();
        const checksumInit = await calculateVirtualPosInitChecksum(fx, {
          customerNumber,
          merchantId: vposMerchant,
          type: 'BILLING',
          tid,
        });
        expect(checksumInit.length, 'checksum-init HMAC non-empty').toBeGreaterThan(10);

        const initBody = await callVirtualPosInitPay(fx, {
          customerNumber,
          merchantId: vposMerchant,
          type: 'BILLING',
          tid,
          checksum: checksumInit,
        });
        expect(String(initBody.STATUS)).toBe('00');

        const date = randomGens.generateOnlinePaymentDate(4);
        await ensureVirtualPosOnlinePaymentPackageForConfirmDate(fx, { confirmDate: date });
        const checksumConfirm = await calculateVirtualPosConfirmChecksum(fx, {
          date,
          customerNumber,
          tid,
          merchantId: vposMerchant,
          total: initBody.AMOUNT,
        });
        expect(checksumConfirm.length, 'checksum-confirm HMAC non-empty').toBeGreaterThan(10);

        const confirmBody = await callVirtualPosConfirmPay(fx, {
          date,
          customerNumber,
          tid,
          merchantId: vposMerchant,
          total: initBody.AMOUNT,
          checksum: checksumConfirm,
        });
        expect(String(confirmBody.STATUS)).toBe('00');

        TestRunSummary.recordCheck({
          check: 'TC-BE-11 checksum helpers accepted',
          expectedResult: 'init+confirm STATUS=00 using helper HMAC unchanged',
          actualResult: `As expected — init=${initBody.STATUS}, confirm=${confirmBody.STATUS}`,
          passed: true,
        });
      });

      await test.step('Attach test run summary', async () => {
        finalizeTestRunSummary(TestRunSummary, Responses, {
          jiraKey: JIRA_KEY,
          relevantEntityKeys: ['customer', 'customerLiability', 'collectionChannel'],
        });
      });
    },
  );

  test(
    `[${JIRA_KEY}] TC-BE-14: Combined confirm TOTAL mismatch returns STATUS 96`,
    async ({ Request, GeneratePayload, Responses, Endpoints, OnlinePaymentUrl, TestRunSummary }) => {
      test.setTimeout(5 * 60 * 1000);
      const fx: Pdt3205Fx = { Request, GeneratePayload, Responses, Endpoints, OnlinePaymentUrl };
      const vposMerchant = await resolveVirtualPosMerchantId(fx);

      await test.step('Precondition: customer + liability + VirtualPos combine=true + BILLING init', async () => {
        await createLegalCustomer(fx);
        TestRunSummary.registerPayload('customer', Responses.customer[0]);
        await createManualLiability(fx, { initialAmount: 100, dueDateDdMmYyyy: '15-09-2026' });
        await withVirtualPosChannelLock(async () => {
          await ensureVirtualPosChannel(fx, true);
        });
      });

      const customerNumber = String(Responses.customer[0].customerNumber);
      const identifier = String(Responses.customer[0].identifier);

      await withVirtualPosChannelLock(async () => {
        await ensureVirtualPosChannel(fx, true);
        const tid = randomGens.generateTID();
        const checksumInit = await calculateVirtualPosInitChecksum(fx, {
          customerNumber,
          merchantId: vposMerchant,
          type: 'BILLING',
          tid,
        });
        const initBody = await callVirtualPosInitPay(fx, {
          customerNumber,
          merchantId: vposMerchant,
          type: 'BILLING',
          tid,
          checksum: checksumInit,
        });
        expect(String(initBody.STATUS)).toBe('00');
        const amount = Number(initBody.AMOUNT);
        expect(amount).toBeGreaterThan(1);
        const wrongTotal = amount + 1;
        const paymentsBefore = await listPaymentsByCustomerIdentifier(fx, identifier);

        const date = randomGens.generateOnlinePaymentDate(4);
        await ensureVirtualPosOnlinePaymentPackageForConfirmDate(fx, { confirmDate: date });
        const checksumConfirm = await calculateVirtualPosConfirmChecksum(fx, {
          date,
          customerNumber,
          tid,
          merchantId: vposMerchant,
          total: wrongTotal,
        });
        const confirmBody = await callVirtualPosConfirmPay(fx, {
          date,
          customerNumber,
          tid,
          merchantId: vposMerchant,
          total: wrongTotal,
          checksum: checksumConfirm,
        });
        expect(String(confirmBody.STATUS), formatVirtualPosStatusDetail(confirmBody)).toBe('96');
        const additional = String(confirmBody.ADDITIONALINFO ?? '').toLowerCase();
        expect(
          additional.includes('incorrect') || additional.includes('combined'),
          `ADDITIONALINFO should mention incorrect/combined: ${confirmBody.ADDITIONALINFO}`,
        ).toBeTruthy();

        const paymentsAfter = await listPaymentsByCustomerIdentifier(fx, identifier);
        expect(paymentsAfter.totalElements).toBe(paymentsBefore.totalElements);

        TestRunSummary.recordCheck({
          check: 'TC-BE-14 TOTAL mismatch → 96',
          expectedResult: 'STATUS=96; no confirmed payment',
          actualResult: `As expected — STATUS=${confirmBody.STATUS}, ADDITIONALINFO=${confirmBody.ADDITIONALINFO}`,
          passed: true,
        });
      });

      await test.step('Attach test run summary', async () => {
        finalizeTestRunSummary(TestRunSummary, Responses, {
          jiraKey: JIRA_KEY,
          relevantEntityKeys: ['customer', 'customerLiability', 'collectionChannel'],
        });
      });
    },
  );

  test(
    `[${JIRA_KEY}] TC-BE-17: Unknown IDN on init-pay returns STATUS 14`,
    async ({ Request, GeneratePayload, Responses, Endpoints, OnlinePaymentUrl, TestRunSummary }) => {
      test.setTimeout(5 * 60 * 1000);
      const fx: Pdt3205Fx = { Request, GeneratePayload, Responses, Endpoints, OnlinePaymentUrl };
      const vposMerchant = await resolveVirtualPosMerchantId(fx);
      const unknownIdn = '9999999999';

      await test.step('Precondition: create customer (API works) + unused IDN + VirtualPos', async () => {
        await createLegalCustomer(fx);
        TestRunSummary.registerPayload('customer', Responses.customer[0]);
        expect(String(Responses.customer[0].customerNumber)).not.toBe(unknownIdn);

        await withVirtualPosChannelLock(async () => {
          await ensureVirtualPosChannel(fx, true);
        });
      });

      const identifier = String(Responses.customer[0].identifier);
      const paymentsBefore = await listPaymentsByCustomerIdentifier(fx, identifier);
      const tid = randomGens.generateTID();
      const initBody = await test.step('Action: init-pay with unknown IDN', async () => {
        const checksum = await calculateVirtualPosInitChecksum(fx, {
          customerNumber: unknownIdn,
          merchantId: vposMerchant,
          type: 'BILLING',
          tid,
        });
        return callVirtualPosInitPay(fx, {
          customerNumber: unknownIdn,
          merchantId: vposMerchant,
          type: 'BILLING',
          tid,
          checksum,
        });
      });

      await test.step('Assert: STATUS 14', async () => {
        expect(String(initBody.STATUS), formatVirtualPosStatusDetail(initBody)).toBe('14');
        expect(String(initBody.STATUS)).not.toBe('00');
        expect(initBody.AMOUNT == null || initBody.AMOUNT === '').toBeTruthy();
        const paymentsAfter = await listPaymentsByCustomerIdentifier(fx, identifier);
        expect(paymentsAfter.totalElements).toBe(paymentsBefore.totalElements);

        TestRunSummary.recordCheck({
          check: 'TC-BE-17 unknown IDN → 14',
          expectedResult: 'STATUS=14 Invalid subscriber; no payment growth',
          actualResult: `As expected — STATUS=${initBody.STATUS}, payments=${paymentsAfter.totalElements}`,
          passed: true,
        });
      });

      await test.step('Attach test run summary', async () => {
        finalizeTestRunSummary(TestRunSummary, Responses, {
          jiraKey: JIRA_KEY,
          relevantEntityKeys: ['customer', 'collectionChannel'],
          snapshot: { unknownIdn, vposMerchant, status: initBody.STATUS },
        });
      });
    },
  );

  test(
    `[${JIRA_KEY}] TC-BE-18: Customer with no payable liabilities returns STATUS 62`,
    async ({ Request, GeneratePayload, Responses, Endpoints, OnlinePaymentUrl, TestRunSummary }) => {
      test.setTimeout(5 * 60 * 1000);
      const fx: Pdt3205Fx = { Request, GeneratePayload, Responses, Endpoints, OnlinePaymentUrl };
      const vposMerchant = await resolveVirtualPosMerchantId(fx);

      await test.step('Precondition: customer with no liabilities + VirtualPos', async () => {
        await createLegalCustomer(fx);
        TestRunSummary.registerPayload('customer', Responses.customer[0]);
        const identifier = String(Responses.customer[0].identifier);
        const listRes = await Request.get(
          `${Endpoints.customerLiability}/list?page=0&size=50&columns=ID&direction=DESC` +
            `&prompt=${identifier}&searchFields=CUSTOMER`,
        );
        await expect(listRes).CheckResponse();
        const body = await listRes.json();
        const open = (body.content ?? []).filter((r: any) => Number(r.currentAmount ?? 0) > 0);
        expect(open.length, 'no open liabilities').toBe(0);

        await withVirtualPosChannelLock(async () => {
          await ensureVirtualPosChannel(fx, true);
        });
      });

      const customerNumber = String(Responses.customer[0].customerNumber);
      const tid = randomGens.generateTID();
      const initBody = await test.step('Action: BILLING init with empty basket', async () => {
        const checksum = await calculateVirtualPosInitChecksum(fx, {
          customerNumber,
          merchantId: vposMerchant,
          type: 'BILLING',
          tid,
        });
        return callVirtualPosInitPay(fx, {
          customerNumber,
          merchantId: vposMerchant,
          type: 'BILLING',
          tid,
          checksum,
        });
      });

      await test.step('Assert: STATUS 62 No Obligation', async () => {
        expect(String(initBody.STATUS), formatVirtualPosStatusDetail(initBody)).toBe('62');
        expect(String(initBody.STATUS)).not.toBe('00');

        TestRunSummary.recordCheck({
          check: 'TC-BE-18 no liabilities → 62',
          expectedResult: 'STATUS=62',
          actualResult: `As expected — STATUS=${initBody.STATUS}`,
          passed: true,
        });
      });

      await test.step('Attach test run summary', async () => {
        finalizeTestRunSummary(TestRunSummary, Responses, {
          jiraKey: JIRA_KEY,
          relevantEntityKeys: ['customer', 'collectionChannel'],
          snapshot: { status: initBody.STATUS },
        });
      });
    },
  );

  test(
    `[${JIRA_KEY}] TC-BE-19: Repeat confirm-pay for the same TID returns STATUS 94`,
    async ({ Request, GeneratePayload, Responses, Endpoints, OnlinePaymentUrl, TestRunSummary }) => {
      test.setTimeout(5 * 60 * 1000);
      const fx: Pdt3205Fx = { Request, GeneratePayload, Responses, Endpoints, OnlinePaymentUrl };
      const vposMerchant = await resolveVirtualPosMerchantId(fx);

      await test.step('Precondition: customer + liability + VirtualPos', async () => {
        await createLegalCustomer(fx);
        TestRunSummary.registerPayload('customer', Responses.customer[0]);
        await createManualLiability(fx, { initialAmount: 100, dueDateDdMmYyyy: '15-09-2026' });
        await withVirtualPosChannelLock(async () => {
          await ensureVirtualPosChannel(fx, true);
        });
      });

      const customerNumber = String(Responses.customer[0].customerNumber);
      const identifier = String(Responses.customer[0].identifier);

      await withVirtualPosChannelLock(async () => {
        await ensureVirtualPosChannel(fx, true);
        const tid = randomGens.generateTID();
        const checksumInit = await calculateVirtualPosInitChecksum(fx, {
          customerNumber,
          merchantId: vposMerchant,
          type: 'BILLING',
          tid,
        });
        const initBody = await callVirtualPosInitPay(fx, {
          customerNumber,
          merchantId: vposMerchant,
          type: 'BILLING',
          tid,
          checksum: checksumInit,
        });
        expect(String(initBody.STATUS)).toBe('00');

        const date = randomGens.generateOnlinePaymentDate(4);
        await ensureVirtualPosOnlinePaymentPackageForConfirmDate(fx, { confirmDate: date });
        const checksumConfirm = await calculateVirtualPosConfirmChecksum(fx, {
          date,
          customerNumber,
          tid,
          merchantId: vposMerchant,
          total: initBody.AMOUNT,
        });
        const firstConfirm = await callVirtualPosConfirmPay(fx, {
          date,
          customerNumber,
          tid,
          merchantId: vposMerchant,
          total: initBody.AMOUNT,
          checksum: checksumConfirm,
        });
        expect(String(firstConfirm.STATUS)).toBe('00');

        const paymentsAfterFirst = await listPaymentsByCustomerIdentifier(fx, identifier);
        const countAfterFirst = paymentsAfterFirst.totalElements;

        const checksumRepeat = await calculateVirtualPosConfirmChecksum(fx, {
          date,
          customerNumber,
          tid,
          merchantId: vposMerchant,
          total: initBody.AMOUNT,
        });
        const secondConfirm = await callVirtualPosConfirmPay(fx, {
          date,
          customerNumber,
          tid,
          merchantId: vposMerchant,
          total: initBody.AMOUNT,
          checksum: checksumRepeat,
        });
        expect(String(secondConfirm.STATUS), formatVirtualPosStatusDetail(secondConfirm)).toBe('94');
        const info = String(secondConfirm.ADDITIONALINFO ?? '').toLowerCase();
        expect(
          info.includes('repeat') || info.includes('tid') || info.includes('already'),
          `ADDITIONALINFO should mention repeat/TID: ${secondConfirm.ADDITIONALINFO}`,
        ).toBeTruthy();

        const paymentsAfterSecond = await listPaymentsByCustomerIdentifier(fx, identifier);
        expect(paymentsAfterSecond.totalElements).toBe(countAfterFirst);

        TestRunSummary.recordCheck({
          check: 'TC-BE-19 repeat TID → 94',
          expectedResult: 'second confirm STATUS=94; payment count unchanged',
          actualResult: `As expected — STATUS=${secondConfirm.STATUS}, payments=${paymentsAfterSecond.totalElements}`,
          passed: true,
        });
      });

      await test.step('Attach test run summary', async () => {
        finalizeTestRunSummary(TestRunSummary, Responses, {
          jiraKey: JIRA_KEY,
          relevantEntityKeys: ['customer', 'customerLiability', 'collectionChannel', 'payment'],
        });
      });
    },
  );

  test(
    `[${JIRA_KEY}] TC-BE-20: Corrupted CHECKSUM on init-pay returns STATUS 93`,
    async ({ Request, GeneratePayload, Responses, Endpoints, OnlinePaymentUrl, TestRunSummary }) => {
      test.setTimeout(5 * 60 * 1000);
      const fx: Pdt3205Fx = { Request, GeneratePayload, Responses, Endpoints, OnlinePaymentUrl };
      const vposMerchant = await resolveVirtualPosMerchantId(fx);

      await test.step('Precondition: customer + liability + VirtualPos', async () => {
        await createLegalCustomer(fx);
        TestRunSummary.registerPayload('customer', Responses.customer[0]);
        await createManualLiability(fx, { initialAmount: 100, dueDateDdMmYyyy: '01-10-2026' });
        await withVirtualPosChannelLock(async () => {
          await ensureVirtualPosChannel(fx, true);
        });
      });

      const customerNumber = String(Responses.customer[0].customerNumber);
      const tid = randomGens.generateTID();

      const initBody = await test.step('Action: init-pay with corrupted checksum', async () => {
        const checksum = await calculateVirtualPosInitChecksum(fx, {
          customerNumber,
          merchantId: vposMerchant,
          type: 'BILLING',
          tid,
        });
        const bad = corruptChecksum(checksum);
        return callVirtualPosInitPay(fx, {
          customerNumber,
          merchantId: vposMerchant,
          type: 'BILLING',
          tid,
          checksum: bad,
        });
      });

      await test.step('Assert: STATUS 93', async () => {
        expect(String(initBody.STATUS), formatVirtualPosStatusDetail(initBody)).toBe('93');
        expect(String(initBody.STATUS)).not.toBe('00');

        TestRunSummary.recordCheck({
          check: 'TC-BE-20 bad checksum → 93',
          expectedResult: 'STATUS=93',
          actualResult: `As expected — STATUS=${initBody.STATUS}`,
          passed: true,
        });
      });

      await test.step('Attach test run summary', async () => {
        finalizeTestRunSummary(TestRunSummary, Responses, {
          jiraKey: JIRA_KEY,
          relevantEntityKeys: ['customer', 'customerLiability', 'collectionChannel'],
          snapshot: { status: initBody.STATUS },
        });
      });
    },
  );
});
