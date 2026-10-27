/**
 * PDT-3631 — Billing group appears in the government receivable.
 *
 * Bug/standalone automation (no Backend TC .md).
 * Environment: dev.
 * Run with: $env:BASE_URL='https://devapps.energo-pro.bg/backend/phoenix1-dev'
 * Also accepted: http://10.236.20.11:8091 (selects DEVAUTHAPI in fixtures/login.ts).
 *
 * Assertion follows Confluence "Customer receivable automatic creation (Government Compensation)"
 * page id 1077608452 (https://asterbit.atlassian.net/wiki/spaces/Phoenix/pages/1077608452/Customer+receivable+automatic+creation+Government+Compensation):
 * the receivable whose customer is the government institution has Billing group = billing group from the invoice.
 * This does not assert the reporter's empty-group claim.
 *
 * Second test uses documentAmount 20 (positive compensation) and receivableForCustomer
 * (the billed-customer receivable). It does not use receivableForRecipient.
 *
 * Reference spec(s):
 * - Cursor-Project/EnergoTS/tests/cursor/PDT-3368-invoice-cancellation-government-compensations.spec.ts
 * - Cursor-Project/EnergoTS/tests/cursor/pdt-3368-invoice-cancellation-government-compensations.fixtures.ts
 * - Cursor-Project/EnergoTS/tests/cursor/GC-REG-government-compensation-process.spec.ts
 * - Cursor-Project/EnergoTS/tests/cursor/gc-reg-government-compensation-regression.fixtures.ts
 *
 * swagger_refresh: succeeded (update-swagger-specs.ps1 exit 0). Dev spec:
 * Cursor-Project/config/swagger/dev/swagger-spec.json
 * GET /invoice InvoiceResponse.contractBillingGroup ShortResponse { id, name }
 * GET /customer-receivable/{id} CustomerReceivableResponse.billingGroupResponse
 *   BillingGroupListingResponse { id, groupNumber }
 * POST /invoice-cancellation CreateInvoiceCancellationRequest (required templateId)
 *   InvoiceCancellationResponse { invoiceCancellationId, processId }
 */

import { test, expect } from './cursor-test.fixtures';
import { finalizeTestRunSummary } from './shared/manual-verification-links.fixtures';
import { assertPositiveApplyAttachments } from './pdt-3368-invoice-cancellation-government-compensations.fixtures';
import {
  BILLING_TIMEOUT_MS,
  PDT_3631_KEY,
  PDT_3631_TITLE,
  accountGcsOnRealDebit,
  assertNegativeApplyAttachments,
  devPortalLinks,
  expectGovernmentReceivableBillingGroup,
  extractPartyCustomerId,
  formatBillingGroupId,
  getCustomerReceivable,
  getInvoice,
  postCancelByInvoiceNumber,
  readContractBillingGroup,
  readInvoiceContractBillingGroup,
  readReceivableBillingGroup,
  resolveOpenTaxEventDate,
  runDevVolCompContractAndBbpPrechain,
  shortId,
  toAmountNumber,
  waitInvoiceCancelled,
  type BillingGroupSnapshot,
  type Pdt3631Fx,
} from './pdt-3631-government-receivable-billing-group.fixtures';

test.describe(`[${PDT_3631_KEY}]: ${PDT_3631_TITLE}`, {
  tag: ['@billing', '@compensations', '@PDT-3631', '@dev'],
}, () => {
  test(`[${PDT_3631_KEY}]: ${PDT_3631_TITLE}`, async ({
    Request,
    GeneratePayload,
    Responses,
    Endpoints,
    TestRunSummary,
  }) => {
    test.setTimeout(BILLING_TIMEOUT_MS);
    const fx: Pdt3631Fx = { Request, GeneratePayload, Responses, Endpoints };

    const state: {
      contractId: number;
      contractBillingGroupId: number;
      contractGroupNumber: string | null;
      billedCustomerId: number;
      governmentCustomerId: number;
      compensationId: number;
      invoiceId: number;
      invoiceNumber: string;
      invoiceContractBillingGroupId: number;
      invoiceContractBillingGroupName: string | null;
      receivableId: number;
      before: BillingGroupSnapshot | null;
      after: BillingGroupSnapshot | null;
      currentAmountAfter: number | null;
      processId: number;
      invoiceContractBillingGroupIdAfterCancel: number | null;
    } = {
      contractId: 0,
      contractBillingGroupId: 0,
      contractGroupNumber: null,
      billedCustomerId: 0,
      governmentCustomerId: 0,
      compensationId: 0,
      invoiceId: 0,
      invoiceNumber: '',
      invoiceContractBillingGroupId: 0,
      invoiceContractBillingGroupName: null,
      receivableId: 0,
      before: null,
      after: null,
      currentAmountAfter: null,
      processId: 0,
      invoiceContractBillingGroupIdAfterCancel: null,
    };

    let summarized = false;
    const attachSummary = (): void => {
      if (summarized) {
        return;
      }
      summarized = true;
      finalizeTestRunSummary(TestRunSummary, Responses, {
        jiraKey: PDT_3631_KEY,
        relevantEntityKeys: ['customer', 'invoice', 'customerReceivable'],
        extraLinks: devPortalLinks({
          invoiceId: state.invoiceId,
          receivableId: state.receivableId,
          governmentCustomerId: state.governmentCustomerId,
          billedCustomerId: state.billedCustomerId,
        }),
        snapshot: {
          environment: 'dev',
          confluencePageId: 1077608452,
          contractId: state.contractId,
          contractBillingGroupId: state.contractBillingGroupId,
          contractGroupNumber: state.contractGroupNumber,
          invoiceId: state.invoiceId,
          invoiceNumber: state.invoiceNumber,
          invoiceContractBillingGroupIdBeforeCancel: state.invoiceContractBillingGroupId,
          invoiceContractBillingGroupName: state.invoiceContractBillingGroupName,
          invoiceContractBillingGroupIdAfterCancel: state.invoiceContractBillingGroupIdAfterCancel,
          governmentCustomerId: state.governmentCustomerId,
          billedCustomerId: state.billedCustomerId,
          compensationId: state.compensationId,
          governmentReceivableId: state.receivableId,
          receivableBillingGroupIdBeforeCancel: state.before?.id ?? null,
          receivableGroupNumberBeforeCancel: state.before?.groupNumber ?? null,
          receivableBillingGroupIdAfterCancel: state.after?.id ?? null,
          receivableGroupNumberAfterCancel: state.after?.groupNumber ?? null,
          currentAmountAfterCancel: state.currentAmountAfter,
          cancellationProcessId: state.processId,
        },
      });
    };

    try {
      const pre = await test.step(
        'Precondition: billed customer, government recipient, POD, contract, and billing profile',
        async () => runDevVolCompContractAndBbpPrechain(fx),
      );
      state.contractId = pre.contractId;
      state.billedCustomerId = pre.customerId;
      state.governmentCustomerId = pre.recipientCustomerId;

      const contractGroup = await test.step(
        'Precondition: read the contract billing group',
        async () => readContractBillingGroup(Request, pre.contractId),
      );
      state.contractBillingGroupId = contractGroup.id;
      state.contractGroupNumber = contractGroup.groupNumber;
      TestRunSummary.recordCheck({
        check: 'Contract billing group can be read back',
        expectedResult: 'GET product-contract billingGroups[0].id is a positive BillingGroupListingResponse id.',
        actualResult: `As expected — contractId=${pre.contractId} billingGroupId=${contractGroup.id} groupNumber=${contractGroup.groupNumber ?? 'null'}`,
        passed: true,
      });

      const billed = await test.step(
        'Precondition: negative government compensation invoiced through accounting',
        async () =>
          accountGcsOnRealDebit(fx, pre, [
            {
              documentAmount: -20,
              podId: pre.podId,
              reason: 'PDT-3631-negative-government-receivable',
            },
          ]),
      );
      const gc = billed.gcs[0];
      expect(gc, 'negative government compensation row').toBeTruthy();
      state.compensationId = gc.id;
      state.invoiceId = billed.invoiceId;
      state.invoiceNumber = billed.invoiceNumber;
      TestRunSummary.registerPayload('compensation', gc.payload);
      TestRunSummary.registerPayload('invoice', {
        invoiceId: billed.invoiceId,
        invoiceNumber: billed.invoiceNumber,
      });

      const invoiceGroup = await test.step(
        'Read invoice contract billing group',
        async () => {
          const invoice = await getInvoice(Request, billed.invoiceId);
          expect(invoice.invoiceStatus, 'invoice REAL').toBe('REAL');
          const group = readInvoiceContractBillingGroup(invoice as Record<string, unknown>);
          expect(
            group.id,
            `invoice.contractBillingGroup.id (${group.id}) must equal the contract billing group (${contractGroup.id}, groupNumber=${contractGroup.groupNumber ?? 'null'})`,
          ).toBe(contractGroup.id);
          return group;
        },
      );
      state.invoiceContractBillingGroupId = invoiceGroup.id;
      state.invoiceContractBillingGroupName = invoiceGroup.name;

      const before = await test.step(
        'Find the government-recipient customer receivable',
        async () => {
          assertNegativeApplyAttachments(gc.view, billed.invoiceId, 'PDT-3631');
          const id = shortId(gc.view, 'receivableForRecipient');
          expect(id, 'compensation receivableForRecipient id').toBeTruthy();
          const receivable = await getCustomerReceivable(Request, id as number);
          expect(
            extractPartyCustomerId(receivable),
            'receivable customer is the government compensation recipient',
          ).toBe(pre.recipientCustomerId);
          Responses.customerReceivable.push(receivable);
          return { id: id as number, receivable };
        },
      );
      state.receivableId = before.id;
      const beforeGroup = readReceivableBillingGroup(before.receivable);
      state.before = beforeGroup;

      const beforeMatch = beforeGroup.id === invoiceGroup.id;
      TestRunSummary.recordCheck({
        check: 'Government receivable billing group equals the invoice billing group',
        expectedResult: `billingGroupResponse.id equals invoice.contractBillingGroup.id (${invoiceGroup.id}). groupNumber recorded when the API returns it.`,
        actualResult: beforeMatch
          ? `As expected — receivableId=${before.id} billingGroupResponse.id=${formatBillingGroupId(beforeGroup.id)} groupNumber=${beforeGroup.groupNumber ?? 'null'}; invoice.contractBillingGroup.id=${invoiceGroup.id}`
          : `Not as expected — receivableId=${before.id} billingGroupResponse.id=${formatBillingGroupId(beforeGroup.id)} groupNumber=${beforeGroup.groupNumber ?? 'null'}; invoice.contractBillingGroup.id=${invoiceGroup.id}`,
        passed: beforeMatch,
      });
      expectGovernmentReceivableBillingGroup(
        'Before cancellation',
        before.id,
        beforeGroup.id,
        billed.invoiceId,
        invoiceGroup.id,
      );

      const cancel = await test.step('POST /invoice-cancellation for the real invoice', async () => {
        const taxEventDate = await resolveOpenTaxEventDate(Request);
        const posted = await postCancelByInvoiceNumber(Request, billed.invoiceNumber, taxEventDate);
        expect(
          posted.status,
          `POST /invoice-cancellation HTTP 201, body=${posted.rawText}`,
        ).toBe(201);
        expect(posted.processId, 'cancellation processId').toBeGreaterThan(0);
        Responses.invoiceCancellation.push(posted.body);
        TestRunSummary.registerPayload('invoiceCancellation', {
          taxEventDate,
          invoices: billed.invoiceNumber,
          processId: posted.processId,
          invoiceCancellationId: posted.cancellationId,
        });
        await waitInvoiceCancelled(Request, billed.invoiceId);
        return posted;
      });
      state.processId = cancel.processId;

      await test.step(
        'Re-read the government receivable after cancellation',
        async () => {
          const receivable = await getCustomerReceivable(Request, before.id);
          const afterGroup = readReceivableBillingGroup(receivable);
          const currentAmount = toAmountNumber(receivable.currentAmount);
          state.after = afterGroup;
          state.currentAmountAfter = currentAmount;

          const invoiceAfter = await getInvoice(Request, billed.invoiceId);
          const afterInvoiceGroup = readInvoiceContractBillingGroup(
            invoiceAfter as Record<string, unknown>,
          );
          state.invoiceContractBillingGroupIdAfterCancel = afterInvoiceGroup.id;

          const amountSettled = currentAmount === 0;
          const groupStillMatches = afterGroup.id === invoiceGroup.id;
          TestRunSummary.recordCheck({
            check: 'Cancellation settles the receivable and keeps the invoice billing group',
            expectedResult: `currentAmount is 0 and billingGroupResponse.id is still invoice.contractBillingGroup.id (${invoiceGroup.id}).`,
            actualResult:
              amountSettled && groupStillMatches
                ? `As expected — receivableId=${before.id} currentAmount=0 billingGroupResponse.id=${formatBillingGroupId(afterGroup.id)} groupNumber=${afterGroup.groupNumber ?? 'null'}`
                : `Not as expected — receivableId=${before.id} currentAmount=${currentAmount} billingGroupResponse.id=${formatBillingGroupId(afterGroup.id)} groupNumber=${afterGroup.groupNumber ?? 'null'}; invoice.contractBillingGroup.id=${invoiceGroup.id}`,
            passed: amountSettled && groupStillMatches,
          });

          expectGovernmentReceivableBillingGroup(
            'After cancellation',
            before.id,
            afterGroup.id,
            billed.invoiceId,
            invoiceGroup.id,
          );
          expect(
            currentAmount,
            `After cancellation government receivable ${before.id} currentAmount must be 0, got ${currentAmount}`,
          ).toBe(0);
        },
      );

      await test.step('Attach test run summary', async () => {
        attachSummary();
      });
    } finally {
      attachSummary();
    }
  });

  test('[PDT-3631]: Billing group appears in the government receivable — positive compensation', async ({
    Request,
    GeneratePayload,
    Responses,
    Endpoints,
    TestRunSummary,
  }) => {
    test.setTimeout(BILLING_TIMEOUT_MS);
    const fx: Pdt3631Fx = { Request, GeneratePayload, Responses, Endpoints };

    const state: {
      contractId: number;
      contractBillingGroupId: number;
      contractGroupNumber: string | null;
      billedCustomerId: number;
      governmentCustomerId: number;
      compensationId: number;
      invoiceId: number;
      invoiceNumber: string;
      invoiceContractBillingGroupId: number;
      invoiceContractBillingGroupName: string | null;
      receivableId: number;
      before: BillingGroupSnapshot | null;
      after: BillingGroupSnapshot | null;
      currentAmountAfter: number | null;
      processId: number;
      invoiceContractBillingGroupIdAfterCancel: number | null;
    } = {
      contractId: 0,
      contractBillingGroupId: 0,
      contractGroupNumber: null,
      billedCustomerId: 0,
      governmentCustomerId: 0,
      compensationId: 0,
      invoiceId: 0,
      invoiceNumber: '',
      invoiceContractBillingGroupId: 0,
      invoiceContractBillingGroupName: null,
      receivableId: 0,
      before: null,
      after: null,
      currentAmountAfter: null,
      processId: 0,
      invoiceContractBillingGroupIdAfterCancel: null,
    };

    let summarized = false;
    const attachSummary = (): void => {
      if (summarized) {
        return;
      }
      summarized = true;
      finalizeTestRunSummary(TestRunSummary, Responses, {
        jiraKey: PDT_3631_KEY,
        relevantEntityKeys: ['customer', 'invoice', 'customerReceivable'],
        extraLinks: devPortalLinks({
          invoiceId: state.invoiceId,
          receivableId: state.receivableId,
          governmentCustomerId: state.governmentCustomerId,
          billedCustomerId: state.billedCustomerId,
        }),
        snapshot: {
          environment: 'dev',
          compensationSign: 'positive',
          confluencePageId: 1077608452,
          contractId: state.contractId,
          contractBillingGroupId: state.contractBillingGroupId,
          contractGroupNumber: state.contractGroupNumber,
          invoiceId: state.invoiceId,
          invoiceNumber: state.invoiceNumber,
          invoiceContractBillingGroupIdBeforeCancel: state.invoiceContractBillingGroupId,
          invoiceContractBillingGroupName: state.invoiceContractBillingGroupName,
          invoiceContractBillingGroupIdAfterCancel: state.invoiceContractBillingGroupIdAfterCancel,
          governmentCustomerId: state.governmentCustomerId,
          billedCustomerId: state.billedCustomerId,
          compensationId: state.compensationId,
          receivableForCustomerId: state.receivableId,
          receivableBillingGroupIdBeforeCancel: state.before?.id ?? null,
          receivableGroupNumberBeforeCancel: state.before?.groupNumber ?? null,
          receivableBillingGroupIdAfterCancel: state.after?.id ?? null,
          receivableGroupNumberAfterCancel: state.after?.groupNumber ?? null,
          currentAmountAfterCancel: state.currentAmountAfter,
          cancellationProcessId: state.processId,
        },
      });
    };

    try {
      const pre = await test.step(
        'Precondition: billed customer, government recipient, POD, contract, and billing profile',
        async () => runDevVolCompContractAndBbpPrechain(fx),
      );
      state.contractId = pre.contractId;
      state.billedCustomerId = pre.customerId;
      state.governmentCustomerId = pre.recipientCustomerId;

      const contractGroup = await test.step(
        'Precondition: read the contract billing group',
        async () => readContractBillingGroup(Request, pre.contractId),
      );
      state.contractBillingGroupId = contractGroup.id;
      state.contractGroupNumber = contractGroup.groupNumber;
      TestRunSummary.recordCheck({
        check: 'Contract billing group can be read back',
        expectedResult: 'GET product-contract billingGroups[0].id is a positive BillingGroupListingResponse id.',
        actualResult: `As expected — contractId=${pre.contractId} billingGroupId=${contractGroup.id} groupNumber=${contractGroup.groupNumber ?? 'null'}`,
        passed: true,
      });

      const billed = await test.step(
        'Precondition: positive government compensation invoiced through accounting',
        async () =>
          accountGcsOnRealDebit(fx, pre, [
            {
              documentAmount: 20,
              podId: pre.podId,
              reason: 'PDT-3631-positive-customer-receivable',
            },
          ]),
      );
      const gc = billed.gcs[0];
      expect(gc, 'positive government compensation row').toBeTruthy();
      state.compensationId = gc.id;
      state.invoiceId = billed.invoiceId;
      state.invoiceNumber = billed.invoiceNumber;
      TestRunSummary.registerPayload('compensation', gc.payload);
      TestRunSummary.registerPayload('invoice', {
        invoiceId: billed.invoiceId,
        invoiceNumber: billed.invoiceNumber,
      });

      const invoiceGroup = await test.step(
        'Read invoice contract billing group',
        async () => {
          const invoice = await getInvoice(Request, billed.invoiceId);
          expect(invoice.invoiceStatus, 'invoice REAL').toBe('REAL');
          const group = readInvoiceContractBillingGroup(invoice as Record<string, unknown>);
          expect(
            group.id,
            `invoice.contractBillingGroup.id (${group.id}) must equal the contract billing group (${contractGroup.id}, groupNumber=${contractGroup.groupNumber ?? 'null'})`,
          ).toBe(contractGroup.id);
          return group;
        },
      );
      state.invoiceContractBillingGroupId = invoiceGroup.id;
      state.invoiceContractBillingGroupName = invoiceGroup.name;

      const before = await test.step(
        'Find the billed-customer receivable created by a positive compensation, not the government-recipient receivable',
        async () => {
          assertPositiveApplyAttachments(gc.view, billed.invoiceId, 'PDT-3631-positive');
          const id = shortId(gc.view, 'receivableForCustomer');
          expect(id, 'compensation receivableForCustomer id').toBeTruthy();
          const receivable = await getCustomerReceivable(Request, id as number);
          expect(
            extractPartyCustomerId(receivable),
            'receivable customer is the billed customer created by a positive compensation, not the government recipient',
          ).toBe(pre.customerId);
          Responses.customerReceivable.push(receivable);
          return { id: id as number, receivable };
        },
      );
      state.receivableId = before.id;
      const beforeGroup = readReceivableBillingGroup(before.receivable);
      state.before = beforeGroup;

      const beforeMatch = beforeGroup.id === invoiceGroup.id;
      TestRunSummary.recordCheck({
        check: 'Billed-customer receivable billing group equals the invoice billing group',
        expectedResult: `billingGroupResponse.id equals invoice.contractBillingGroup.id (${invoiceGroup.id}) on the billed-customer receivable created by a positive compensation, not the government-recipient receivable. groupNumber recorded when the API returns it.`,
        actualResult: beforeMatch
          ? `As expected — receivableForCustomer id=${before.id} billingGroupResponse.id=${formatBillingGroupId(beforeGroup.id)} groupNumber=${beforeGroup.groupNumber ?? 'null'}; invoice.contractBillingGroup.id=${invoiceGroup.id}`
          : `Not as expected — receivableForCustomer id=${before.id} billingGroupResponse.id=${formatBillingGroupId(beforeGroup.id)} groupNumber=${beforeGroup.groupNumber ?? 'null'}; invoice.contractBillingGroup.id=${invoiceGroup.id}`,
        passed: beforeMatch,
      });
      expectGovernmentReceivableBillingGroup(
        'Before cancellation',
        before.id,
        beforeGroup.id,
        billed.invoiceId,
        invoiceGroup.id,
      );

      const cancel = await test.step('POST /invoice-cancellation for the real invoice', async () => {
        const taxEventDate = await resolveOpenTaxEventDate(Request);
        const posted = await postCancelByInvoiceNumber(Request, billed.invoiceNumber, taxEventDate);
        expect(
          posted.status,
          `POST /invoice-cancellation HTTP 201, body=${posted.rawText}`,
        ).toBe(201);
        expect(posted.processId, 'cancellation processId').toBeGreaterThan(0);
        Responses.invoiceCancellation.push(posted.body);
        TestRunSummary.registerPayload('invoiceCancellation', {
          taxEventDate,
          invoices: billed.invoiceNumber,
          processId: posted.processId,
          invoiceCancellationId: posted.cancellationId,
        });
        await waitInvoiceCancelled(Request, billed.invoiceId);
        return posted;
      });
      state.processId = cancel.processId;

      await test.step(
        'Re-read the billed-customer receivable created by a positive compensation after cancellation',
        async () => {
          const receivable = await getCustomerReceivable(Request, before.id);
          const afterGroup = readReceivableBillingGroup(receivable);
          const currentAmount = toAmountNumber(receivable.currentAmount);
          state.after = afterGroup;
          state.currentAmountAfter = currentAmount;

          const invoiceAfter = await getInvoice(Request, billed.invoiceId);
          const afterInvoiceGroup = readInvoiceContractBillingGroup(
            invoiceAfter as Record<string, unknown>,
          );
          state.invoiceContractBillingGroupIdAfterCancel = afterInvoiceGroup.id;

          const amountSettled = currentAmount === 0;
          const groupStillMatches = afterGroup.id === invoiceGroup.id;
          TestRunSummary.recordCheck({
            check: 'Cancellation settles the billed-customer receivable and keeps the invoice billing group',
            expectedResult: `currentAmount is 0 and billingGroupResponse.id is still invoice.contractBillingGroup.id (${invoiceGroup.id}) on the billed-customer receivable created by a positive compensation, not the government-recipient receivable.`,
            actualResult:
              amountSettled && groupStillMatches
                ? `As expected — receivableForCustomer id=${before.id} currentAmount=0 billingGroupResponse.id=${formatBillingGroupId(afterGroup.id)} groupNumber=${afterGroup.groupNumber ?? 'null'}`
                : `Not as expected — receivableForCustomer id=${before.id} currentAmount=${currentAmount} billingGroupResponse.id=${formatBillingGroupId(afterGroup.id)} groupNumber=${afterGroup.groupNumber ?? 'null'}; invoice.contractBillingGroup.id=${invoiceGroup.id}`,
            passed: amountSettled && groupStillMatches,
          });

          expectGovernmentReceivableBillingGroup(
            'After cancellation',
            before.id,
            afterGroup.id,
            billed.invoiceId,
            invoiceGroup.id,
          );
          expect(
            currentAmount,
            `After cancellation billed-customer receivable ${before.id} currentAmount must be 0, got ${currentAmount}. billingGroupResponse.id=${formatBillingGroupId(afterGroup.id)} invoice.contractBillingGroup.id=${invoiceGroup.id}`,
          ).toBe(0);
        },
      );

      await test.step('Attach test run summary', async () => {
        attachSummary();
      });
    } finally {
      attachSummary();
    }
  });
});
