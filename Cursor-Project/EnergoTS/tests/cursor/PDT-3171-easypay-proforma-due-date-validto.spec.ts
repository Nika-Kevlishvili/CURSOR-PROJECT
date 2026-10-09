/**
 * PDT-3171 — EasyPay change in due date of proforma invoices (VALIDTO).
 *
 * Backend-only (dev). Asserts EasyPay body STATUS / VALIDTO / AMOUNT.
 * Runtime on origin/dev: null dueDate → VALIDTO = today+30 yyyyMMdd (not "0");
 * real dueDate → that date; combined confirm mismatch → STATUS 96.
 *
 * Reference spec(s):
 * - tests/receivableManagement/Payment/onlinePayment.spec.ts (REG-1036 EasyPay init/confirm)
 * - tests/cursor/PDT-2960-proforma-liability-due-date.spec.ts (proforma accounting chain)
 * - tests/contractsAndOrders/goodsOrder.spec.ts (issue-invoice after proforma)
 *
 * /epay/* is not in Phoenix core swagger — contract from payment-api + REG-1036 helpers.
 */

import { test, expect } from './cursor-test.fixtures';
import { finalizeTestRunSummary } from './shared/manual-verification-links.fixtures';
import { randomGens } from '../../utils/randomGens';
import {
  assertValidToNotZero,
  calculateConfirmChecksum,
  calculateInitChecksum,
  callConfirmPay,
  callInitPay,
  createCollectionChannelAndPaymentPackage,
  createLegalCustomer,
  createManualLiability,
  findEasyPayOnlineChannel,
  getLiabilityDetail,
  issueInvoiceAndReadLiabilityDueDate,
  listOpenLiabilitiesForCustomer,
  listPaymentsByCustomerIdentifier,
  PROFORMA_VALID_TO_DAYS,
  resolveEasyPayMerchantId,
  setEasyPayCombineLiabilities,
  setupProformaLiabilityChain,
  toCoinAmount,
  dueDateToYyyyMmDd,
  yyyyMmDdFromLocalDate,
  type Pdt3171Fx,
} from './pdt-3171-easypay-proforma-due-date-validto.fixtures';

const JIRA_KEY = 'PDT-3171';
const JIRA_TITLE = 'EasyPay - change in due date of proforma invoices';

test.describe(`[${JIRA_KEY}]: ${JIRA_TITLE}`, {
  tag: ['@receivableManagement', '@payment', '@easypay', '@pdt-3171', '@dev'],
}, () => {
  test.describe.configure({ mode: 'serial' });

  test(
    `[${JIRA_KEY}] TC-BE-1: Proforma-only liability — init-pay VALIDTO is today+30 yyyyMMdd, not "0"`,
    async ({ Request, GeneratePayload, Responses, Endpoints, OnlinePaymentUrl, TestRunSummary }) => {
      test.setTimeout(20 * 60 * 1000);
      const fx: Pdt3171Fx = { Request, GeneratePayload, Responses, Endpoints, OnlinePaymentUrl };

      const { liabilityId, currentAmount, proformaInvoiceId, goodsOrderId } = await test.step(
        'Precondition: legal customer + goods order proforma accounting (null dueDate)',
        async () => setupProformaLiabilityChain(fx),
      );

      TestRunSummary.registerPayload('customer', Responses.customer[0]);

      const merchantId = await test.step('Precondition: EasyPay merchant + channel (combine=false)', async () => {
        const mid = await resolveEasyPayMerchantId(fx);
        const easyPayChannelId = await findEasyPayOnlineChannel(fx);
        await setEasyPayCombineLiabilities(fx, false, easyPayChannelId);
        return mid;
      });

      const expectedValidTo = yyyyMmDdFromLocalDate(new Date(), PROFORMA_VALID_TO_DAYS);
      const customerNumber = String(Responses.customer[0].customerNumber);

      const initBody = await test.step('Action: EasyPay init-pay after valid checksum', async () => {
        const tid = randomGens.generateTID();
        const checksum = await calculateInitChecksum(fx, {
          customerNumber,
          tid,
          merchantId,
        });
        return callInitPay(fx, { customerNumber, tid, merchantId, checksum });
      });

      await test.step('Assert: STATUS 00, VALIDTO=today+30, not "0", AMOUNT>0', async () => {
        expect(String(initBody.STATUS)).toBe('00');
        const rootValidTo = assertValidToNotZero(initBody.VALIDTO, 'root');
        expect(rootValidTo).toBe(expectedValidTo);

        const invoices = initBody.INVOICES as Array<{ VALIDTO?: string }> | undefined;
        if (Array.isArray(invoices)) {
          for (const inv of invoices) {
            expect(assertValidToNotZero(inv.VALIDTO, 'invoice')).toBe(expectedValidTo);
          }
        }

        const amount = Number(initBody.AMOUNT);
        expect(amount).toBeGreaterThan(0);
        expect(amount).toBe(toCoinAmount(currentAmount));

        TestRunSummary.recordCheck({
          check: 'TC-BE-1 proforma VALIDTO today+30',
          expectedResult: `STATUS=00, VALIDTO=${expectedValidTo} (not "0"), AMOUNT=${toCoinAmount(currentAmount)}`,
          actualResult: `As expected — STATUS=${initBody.STATUS}, VALIDTO=${initBody.VALIDTO}, AMOUNT=${initBody.AMOUNT}`,
          passed: true,
        });
      });

      await test.step('Attach test run summary', async () => {
        finalizeTestRunSummary(TestRunSummary, Responses, {
          jiraKey: JIRA_KEY,
          relevantEntityKeys: ['customer', 'goodsOrder', 'invoice', 'customerLiability'],
          snapshot: {
            goodsOrderId,
            proformaInvoiceId,
            liabilityId,
            expectedValidTo,
            initValidTo: initBody.VALIDTO,
            initAmount: initBody.AMOUNT,
          },
        });
      });
    },
  );

  test(
    `[${JIRA_KEY}] TC-BE-2: Liability with real dueDate — init-pay VALIDTO equals that date`,
    async ({ Request, GeneratePayload, Responses, Endpoints, OnlinePaymentUrl, TestRunSummary }) => {
      test.setTimeout(5 * 60 * 1000);
      const fx: Pdt3171Fx = { Request, GeneratePayload, Responses, Endpoints, OnlinePaymentUrl };
      const dueDateDdMmYyyy = '15-09-2026';
      const expectedValidTo = dueDateToYyyyMmDd(dueDateDdMmYyyy);

      await test.step('Precondition: legal customer + channel/package + liability with dueDate', async () => {
        await createLegalCustomer(fx);
        await createCollectionChannelAndPaymentPackage(fx);
        const liabilityId = await createManualLiability(fx, {
          initialAmount: 100,
          dueDateDdMmYyyy,
        });
        const detail = await getLiabilityDetail(fx, liabilityId);
        expect(detail.currentAmount).toBeGreaterThan(0);
        expect(dueDateToYyyyMmDd(detail.dueDate || dueDateDdMmYyyy)).toBe(expectedValidTo);
        TestRunSummary.registerPayload('customer', Responses.customer[0]);
      });

      const merchantId = await test.step('Precondition: EasyPay merchant + combine=false', async () => {
        const mid = await resolveEasyPayMerchantId(fx);
        const easyPayChannelId = await findEasyPayOnlineChannel(fx);
        await setEasyPayCombineLiabilities(fx, false, easyPayChannelId);
        return mid;
      });

      const customerNumber = String(Responses.customer[0].customerNumber);
      const initBody = await test.step('Action: EasyPay init-pay', async () => {
        const tid = randomGens.generateTID();
        const checksum = await calculateInitChecksum(fx, { customerNumber, tid, merchantId });
        return callInitPay(fx, { customerNumber, tid, merchantId, checksum });
      });

      await test.step('Assert: VALIDTO equals liability dueDate yyyyMMdd', async () => {
        expect(String(initBody.STATUS)).toBe('00');
        expect(assertValidToNotZero(initBody.VALIDTO, 'root')).toBe(expectedValidTo);
        const invoices = initBody.INVOICES as Array<{ VALIDTO?: string }> | undefined;
        if (Array.isArray(invoices) && invoices.length > 0) {
          expect(assertValidToNotZero(invoices[0].VALIDTO, 'invoice')).toBe(expectedValidTo);
        }
        TestRunSummary.recordCheck({
          check: 'TC-BE-2 real dueDate VALIDTO',
          expectedResult: `STATUS=00, VALIDTO=${expectedValidTo}`,
          actualResult: `As expected — STATUS=${initBody.STATUS}, VALIDTO=${initBody.VALIDTO}`,
          passed: true,
        });
      });

      await test.step('Attach test run summary', async () => {
        finalizeTestRunSummary(TestRunSummary, Responses, {
          jiraKey: JIRA_KEY,
          relevantEntityKeys: ['customer', 'customerLiability', 'collectionChannel', 'paymentPackage'],
          snapshot: { expectedValidTo, initValidTo: initBody.VALIDTO },
        });
      });
    },
  );

  test(
    `[${JIRA_KEY}] TC-BE-3: Mixed basket — root VALIDTO uses nearest non-null dueDate`,
    async ({ Request, GeneratePayload, Responses, Endpoints, OnlinePaymentUrl, TestRunSummary }) => {
      test.setTimeout(20 * 60 * 1000);
      const fx: Pdt3171Fx = { Request, GeneratePayload, Responses, Endpoints, OnlinePaymentUrl };
      const manualDueDateDdMmYyyy = '20-08-2026';
      const expectedRootValidTo = dueDateToYyyyMmDd(manualDueDateDdMmYyyy);
      const expectedProformaValidTo = yyyyMmDdFromLocalDate(new Date(), PROFORMA_VALID_TO_DAYS);

      const proforma = await test.step(
        'Precondition: proforma liability (null dueDate) via goods-order chain',
        async () => setupProformaLiabilityChain(fx),
      );

      await test.step('Precondition: second manual liability with nearer dueDate', async () => {
        await createManualLiability(fx, {
          initialAmount: 50,
          dueDateDdMmYyyy: manualDueDateDdMmYyyy,
        });
        TestRunSummary.registerPayload('customer', Responses.customer[0]);
      });

      const merchantId = await test.step('Precondition: EasyPay merchant + combine=false', async () => {
        const mid = await resolveEasyPayMerchantId(fx);
        const easyPayChannelId = await findEasyPayOnlineChannel(fx);
        await setEasyPayCombineLiabilities(fx, false, easyPayChannelId);
        return mid;
      });

      const customerNumber = String(Responses.customer[0].customerNumber);
      const initBody = await test.step('Action: EasyPay init-pay for mixed basket', async () => {
        const tid = randomGens.generateTID();
        const checksum = await calculateInitChecksum(fx, { customerNumber, tid, merchantId });
        return callInitPay(fx, { customerNumber, tid, merchantId, checksum });
      });

      await test.step('Assert: root VALIDTO = nearest non-null; proforma invoice = today+30', async () => {
        expect(String(initBody.STATUS)).toBe('00');
        expect(assertValidToNotZero(initBody.VALIDTO, 'root')).toBe(expectedRootValidTo);

        const invoices = (initBody.INVOICES ?? []) as Array<{ VALIDTO?: string; AMOUNT?: number }>;
        expect(
          invoices.length,
          'combineLiabilities=false should return INVOICES for mixed basket',
        ).toBeGreaterThanOrEqual(2);
        const validTos = invoices.map((i) => assertValidToNotZero(i.VALIDTO, 'invoice'));
        expect(validTos).toContain(expectedRootValidTo);
        expect(validTos).toContain(expectedProformaValidTo);

        TestRunSummary.recordCheck({
          check: 'TC-BE-3 mixed basket nearest VALIDTO',
          expectedResult: `Root VALIDTO=${expectedRootValidTo}; proforma entry ${expectedProformaValidTo}`,
          actualResult: `As expected — root=${initBody.VALIDTO}, INVOICES=${JSON.stringify(initBody.INVOICES ?? [])}`,
          passed: true,
        });
      });

      await test.step('Attach test run summary', async () => {
        finalizeTestRunSummary(TestRunSummary, Responses, {
          jiraKey: JIRA_KEY,
          relevantEntityKeys: ['customer', 'goodsOrder', 'customerLiability'],
          snapshot: {
            proformaLiabilityId: proforma.liabilityId,
            expectedRootValidTo,
            expectedProformaValidTo,
            initValidTo: initBody.VALIDTO,
          },
        });
      });
    },
  );

  test(
    `[${JIRA_KEY}] TC-BE-4: Invalid CHECKSUM on init-pay returns STATUS 93`,
    async ({ Request, GeneratePayload, Responses, Endpoints, OnlinePaymentUrl, TestRunSummary }) => {
      test.setTimeout(5 * 60 * 1000);
      const fx: Pdt3171Fx = { Request, GeneratePayload, Responses, Endpoints, OnlinePaymentUrl };

      await test.step('Precondition: customer + payable liability', async () => {
        await createLegalCustomer(fx);
        await createCollectionChannelAndPaymentPackage(fx);
        await createManualLiability(fx, { initialAmount: 100, dueDateDdMmYyyy: '01-10-2026' });
        TestRunSummary.registerPayload('customer', Responses.customer[0]);
      });

      const merchantId = await resolveEasyPayMerchantId(fx);
      const customerNumber = String(Responses.customer[0].customerNumber);

      const initBody = await test.step('Action: init-pay with invalid CHECKSUM', async () => {
        const tid = randomGens.generateTID();
        const invalidChecksum = '0000000000000000000000000000000000000000';
        return callInitPay(fx, {
          customerNumber,
          tid,
          merchantId,
          checksum: invalidChecksum,
        });
      });

      await test.step('Assert: STATUS 93 Invalid checksum', async () => {
        expect(String(initBody.STATUS)).toBe('93');
        TestRunSummary.recordCheck({
          check: 'TC-BE-4 invalid checksum',
          expectedResult: 'STATUS=93',
          actualResult: `As expected — STATUS=${initBody.STATUS}`,
          passed: true,
        });
      });

      await test.step('Attach test run summary', async () => {
        finalizeTestRunSummary(TestRunSummary, Responses, {
          jiraKey: JIRA_KEY,
          relevantEntityKeys: ['customer', 'customerLiability'],
          snapshot: { status: initBody.STATUS },
        });
      });
    },
  );

  test(
    `[${JIRA_KEY}] TC-BE-5: Unknown MERCHANTID on init-pay returns STATUS 96`,
    async ({ Request, GeneratePayload, Responses, Endpoints, OnlinePaymentUrl, TestRunSummary }) => {
      test.setTimeout(5 * 60 * 1000);
      const fx: Pdt3171Fx = { Request, GeneratePayload, Responses, Endpoints, OnlinePaymentUrl };
      const badMerchantId = '9999999';

      await test.step('Precondition: customer + payable liability', async () => {
        await createLegalCustomer(fx);
        await createCollectionChannelAndPaymentPackage(fx);
        await createManualLiability(fx, { initialAmount: 100, dueDateDdMmYyyy: '01-10-2026' });
        TestRunSummary.registerPayload('customer', Responses.customer[0]);
      });

      await test.step('Precondition: resolve real merchant (contrast only)', async () => {
        const real = await resolveEasyPayMerchantId(fx);
        expect(real).not.toBe(badMerchantId);
      });

      const customerNumber = String(Responses.customer[0].customerNumber);
      const initBody = await test.step('Action: init-pay with bad MERCHANTID + matching checksum', async () => {
        const tid = randomGens.generateTID();
        const checksum = await calculateInitChecksum(fx, {
          customerNumber,
          tid,
          merchantId: badMerchantId,
        });
        return callInitPay(fx, {
          customerNumber,
          tid,
          merchantId: badMerchantId,
          checksum,
        });
      });

      await test.step('Assert: STATUS 96 General error', async () => {
        expect(String(initBody.STATUS)).toBe('96');
        TestRunSummary.recordCheck({
          check: 'TC-BE-5 unknown merchant',
          expectedResult: 'STATUS=96',
          actualResult: `As expected — STATUS=${initBody.STATUS}`,
          passed: true,
        });
      });

      await test.step('Attach test run summary', async () => {
        finalizeTestRunSummary(TestRunSummary, Responses, {
          jiraKey: JIRA_KEY,
          relevantEntityKeys: ['customer', 'customerLiability'],
          snapshot: { status: initBody.STATUS, badMerchantId },
        });
      });
    },
  );

  test(
    `[${JIRA_KEY}] TC-BE-6: Customer with no payable liabilities — init-pay STATUS 62`,
    async ({ Request, GeneratePayload, Responses, Endpoints, OnlinePaymentUrl, TestRunSummary }) => {
      test.setTimeout(5 * 60 * 1000);
      const fx: Pdt3171Fx = { Request, GeneratePayload, Responses, Endpoints, OnlinePaymentUrl };

      await test.step('Precondition: legal customer with no liabilities', async () => {
        await createLegalCustomer(fx);
        TestRunSummary.registerPayload('customer', Responses.customer[0]);
        const open = await listOpenLiabilitiesForCustomer(
          fx,
          String(Responses.customer[0].identifier),
        );
        expect(open, 'customer must have no open liabilities').toHaveLength(0);
      });

      const merchantId = await resolveEasyPayMerchantId(fx);
      const customerNumber = String(Responses.customer[0].customerNumber);

      const initBody = await test.step('Action: init-pay with valid checksum', async () => {
        const tid = randomGens.generateTID();
        const checksum = await calculateInitChecksum(fx, { customerNumber, tid, merchantId });
        return callInitPay(fx, { customerNumber, tid, merchantId, checksum });
      });

      await test.step('Assert: STATUS 62 No Obligation', async () => {
        expect(String(initBody.STATUS)).toBe('62');
        TestRunSummary.recordCheck({
          check: 'TC-BE-6 no obligation',
          expectedResult: 'STATUS=62',
          actualResult: `As expected — STATUS=${initBody.STATUS}`,
          passed: true,
        });
      });

      await test.step('Attach test run summary', async () => {
        finalizeTestRunSummary(TestRunSummary, Responses, {
          jiraKey: JIRA_KEY,
          relevantEntityKeys: ['customer'],
          snapshot: { status: initBody.STATUS },
        });
      });
    },
  );

  test(
    `[${JIRA_KEY}] TC-BE-7: confirm-pay succeeds after successful init when TOTAL matches AMOUNT`,
    async ({
      Request,
      GeneratePayload,
      Responses,
      Endpoints,
      OnlinePaymentUrl,
      receivableValidations,
      TestRunSummary,
    }) => {
      test.setTimeout(5 * 60 * 1000);
      const fx: Pdt3171Fx = {
        Request,
        GeneratePayload,
        Responses,
        Endpoints,
        OnlinePaymentUrl,
        receivableValidations,
      };

      await test.step('Precondition: customer + two liabilities + combine=true', async () => {
        await createLegalCustomer(fx);
        await createCollectionChannelAndPaymentPackage(fx);
        await createManualLiability(fx, { initialAmount: 100, dueDateDdMmYyyy: '01-09-2026' });
        await createManualLiability(fx, { initialAmount: 100, dueDateDdMmYyyy: '15-09-2026' });
        const easyPayChannelId = await findEasyPayOnlineChannel(fx);
        await setEasyPayCombineLiabilities(fx, true, easyPayChannelId);
        TestRunSummary.registerPayload('customer', Responses.customer[0]);
      });

      const merchantId = await resolveEasyPayMerchantId(fx);
      const customerNumber = String(Responses.customer[0].customerNumber);
      const tid = randomGens.generateTID();

      const initBody = await test.step('Action: init-pay (combined)', async () => {
        const checksum = await calculateInitChecksum(fx, { customerNumber, tid, merchantId });
        const body = await callInitPay(fx, { customerNumber, tid, merchantId, checksum });
        expect(String(body.STATUS)).toBe('00');
        expect(Number(body.AMOUNT)).toBeGreaterThan(0);
        assertValidToNotZero(body.VALIDTO, 'root');
        return body;
      });

      const amount = Number(initBody.AMOUNT);
      const confirmBody = await test.step('Action: confirm-pay with matching TOTAL', async () => {
        const date = randomGens.generateOnlinePaymentDate(4);
        const checksumConfirm = await calculateConfirmChecksum(fx, {
          date,
          customerNumber,
          tid,
          merchantId,
          total: amount,
        });
        return callConfirmPay(fx, {
          date,
          customerNumber,
          tid,
          merchantId,
          total: amount,
          checksum: checksumConfirm,
        });
      });

      await test.step('Assert: confirm STATUS 00 and payment created', async () => {
        expect(String(confirmBody.STATUS)).toBe('00');
        const payments = await listPaymentsByCustomerIdentifier(
          fx,
          String(Responses.customer[0].identifier),
        );
        expect(payments.totalElements).toBeGreaterThan(0);
        expect(payments.content[0]).toBeDefined();
        Responses.payment.push(payments.content[0].id);
        if (receivableValidations) {
          await receivableValidations.paymentValidation(Responses.payment[0]);
        }
        TestRunSummary.recordCheck({
          check: 'TC-BE-7 confirm match happy path',
          expectedResult: 'Init VALIDTO real yyyyMMdd; confirm STATUS=00; payment created',
          actualResult: `As expected — init VALIDTO=${initBody.VALIDTO}, confirm STATUS=${confirmBody.STATUS}, paymentId=${Responses.payment[0]}`,
          passed: true,
        });
      });

      await test.step('Attach test run summary', async () => {
        finalizeTestRunSummary(TestRunSummary, Responses, {
          jiraKey: JIRA_KEY,
          relevantEntityKeys: ['customer', 'customerLiability', 'payment'],
          snapshot: {
            initAmount: amount,
            initValidTo: initBody.VALIDTO,
            confirmStatus: confirmBody.STATUS,
          },
        });
      });
    },
  );

  test(
    `[${JIRA_KEY}] TC-BE-8: After issue-invoice from proforma — VALIDTO uses real payment deadline / dueDate`,
    async ({ Request, GeneratePayload, Responses, Endpoints, OnlinePaymentUrl, TestRunSummary }) => {
      test.setTimeout(20 * 60 * 1000);
      const fx: Pdt3171Fx = { Request, GeneratePayload, Responses, Endpoints, OnlinePaymentUrl };

      const { goodsOrderId, liabilityId, proformaInvoiceId } = await test.step(
        'Precondition: proforma accounting with null dueDate',
        async () => setupProformaLiabilityChain(fx),
      );

      const afterIssue = await test.step(
        'Precondition: PATCH issue-invoice and read real dueDate',
        async () => issueInvoiceAndReadLiabilityDueDate(fx, goodsOrderId, liabilityId),
      );

      const expectedValidTo = dueDateToYyyyMmDd(afterIssue.dueDate);
      const todayPlus30 = yyyyMmDdFromLocalDate(new Date(), PROFORMA_VALID_TO_DAYS);
      TestRunSummary.registerPayload('customer', Responses.customer[0]);

      const merchantId = await test.step('Precondition: EasyPay merchant + combine=false', async () => {
        const mid = await resolveEasyPayMerchantId(fx);
        const easyPayChannelId = await findEasyPayOnlineChannel(fx);
        await setEasyPayCombineLiabilities(fx, false, easyPayChannelId);
        return mid;
      });

      const customerNumber = String(Responses.customer[0].customerNumber);
      const initBody = await test.step('Action: init-pay after issue-invoice', async () => {
        const tid = randomGens.generateTID();
        const checksum = await calculateInitChecksum(fx, { customerNumber, tid, merchantId });
        return callInitPay(fx, { customerNumber, tid, merchantId, checksum });
      });

      await test.step('Assert: VALIDTO equals real dueDate, not today+30 fallback', async () => {
        expect(String(initBody.STATUS)).toBe('00');
        const rootValidTo = assertValidToNotZero(initBody.VALIDTO, 'root');
        expect(rootValidTo).toBe(expectedValidTo);
        if (expectedValidTo !== todayPlus30) {
          expect(rootValidTo).not.toBe(todayPlus30);
        }
        const invoices = initBody.INVOICES as Array<{ VALIDTO?: string }> | undefined;
        if (Array.isArray(invoices)) {
          for (const inv of invoices) {
            expect(assertValidToNotZero(inv.VALIDTO, 'invoice')).toBe(expectedValidTo);
          }
        }
        const amount = Number(initBody.AMOUNT);
        expect(amount).toBeGreaterThan(0);
        expect(amount).toBe(toCoinAmount(afterIssue.currentAmount));

        TestRunSummary.recordCheck({
          check: 'TC-BE-8 post issue-invoice VALIDTO',
          expectedResult: `STATUS=00, VALIDTO=${expectedValidTo} (liability dueDate), not "0"`,
          actualResult: `As expected — STATUS=${initBody.STATUS}, VALIDTO=${initBody.VALIDTO}, AMOUNT=${initBody.AMOUNT}`,
          passed: true,
        });
      });

      await test.step('Attach test run summary', async () => {
        finalizeTestRunSummary(TestRunSummary, Responses, {
          jiraKey: JIRA_KEY,
          relevantEntityKeys: ['customer', 'goodsOrder', 'invoice', 'customerLiability'],
          snapshot: {
            goodsOrderId,
            proformaInvoiceId,
            liabilityId,
            realDueDate: afterIssue.dueDate,
            expectedValidTo,
            initValidTo: initBody.VALIDTO,
          },
        });
      });
    },
  );

  test(
    `[${JIRA_KEY}] TC-BE-9: confirm-pay mismatched TOTAL on combined channel returns STATUS 96`,
    async ({ Request, GeneratePayload, Responses, Endpoints, OnlinePaymentUrl, TestRunSummary }) => {
      test.setTimeout(5 * 60 * 1000);
      const fx: Pdt3171Fx = { Request, GeneratePayload, Responses, Endpoints, OnlinePaymentUrl };

      await test.step('Precondition: customer + liability + combine=true + successful init', async () => {
        await createLegalCustomer(fx);
        await createCollectionChannelAndPaymentPackage(fx);
        await createManualLiability(fx, { initialAmount: 100, dueDateDdMmYyyy: '01-09-2026' });
        const easyPayChannelId = await findEasyPayOnlineChannel(fx);
        await setEasyPayCombineLiabilities(fx, true, easyPayChannelId);
        TestRunSummary.registerPayload('customer', Responses.customer[0]);
      });

      const merchantId = await resolveEasyPayMerchantId(fx);
      const customerNumber = String(Responses.customer[0].customerNumber);
      const tid = randomGens.generateTID();
      const identifier = String(Responses.customer[0].identifier);

      const initBody = await test.step('Precondition: successful init-pay', async () => {
        const checksum = await calculateInitChecksum(fx, { customerNumber, tid, merchantId });
        const body = await callInitPay(fx, { customerNumber, tid, merchantId, checksum });
        expect(String(body.STATUS)).toBe('00');
        expect(Number(body.AMOUNT)).toBeGreaterThan(0);
        return body;
      });

      const amount = Number(initBody.AMOUNT);
      const wrongTotal = amount > 1 ? amount - 1 : amount + 1;

      const confirmBody = await test.step('Action: confirm-pay with mismatched TOTAL', async () => {
        const date = randomGens.generateOnlinePaymentDate(4);
        const checksumConfirm = await calculateConfirmChecksum(fx, {
          date,
          customerNumber,
          tid,
          merchantId,
          total: wrongTotal,
        });
        return callConfirmPay(fx, {
          date,
          customerNumber,
          tid,
          merchantId,
          total: wrongTotal,
          checksum: checksumConfirm,
        });
      });

      await test.step('Assert: STATUS 96 and no successful payment for wrong TOTAL', async () => {
        expect(String(confirmBody.STATUS)).toBe('96');
        const statusText = String(
          confirmBody.ADDITIONALINFO ??
            confirmBody.DESCRIPTION ??
            confirmBody.STATUSDESC ??
            confirmBody.message ??
            '',
        ).toLowerCase();
        expect(statusText.length, 'confirm ADDITIONALINFO/DESCRIPTION should be present').toBeGreaterThan(0);
        expect(
          statusText.includes('incorrect') || statusText.includes('combined'),
          `expected incorrect/combined message, got: ${statusText}`,
        ).toBeTruthy();

        const payments = await listPaymentsByCustomerIdentifier(fx, identifier);
        const wrongPayment = (payments.content ?? []).find((p: any) => {
          const pAmount = Number(p.initialAmount ?? p.amount ?? 0);
          return Math.abs(toCoinAmount(pAmount) - wrongTotal) < 1 || pAmount === wrongTotal;
        });
        expect(wrongPayment, 'no payment should accept mismatched TOTAL').toBeFalsy();

        TestRunSummary.recordCheck({
          check: 'TC-BE-9 combined confirm mismatch STATUS 96',
          expectedResult: 'STATUS=96; payment not accepted for wrong TOTAL',
          actualResult: `As expected — STATUS=${confirmBody.STATUS}, wrongTotal=${wrongTotal}, payments=${payments.totalElements}`,
          passed: true,
        });
      });

      await test.step('Attach test run summary', async () => {
        finalizeTestRunSummary(TestRunSummary, Responses, {
          jiraKey: JIRA_KEY,
          relevantEntityKeys: ['customer', 'customerLiability', 'payment'],
          snapshot: {
            initAmount: amount,
            wrongTotal,
            confirmStatus: confirmBody.STATUS,
          },
        });
      });
    },
  );
});
