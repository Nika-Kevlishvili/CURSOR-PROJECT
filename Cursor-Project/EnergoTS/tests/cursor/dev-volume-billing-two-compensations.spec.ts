/**
 * DEV-DATA — Create FOR_VOLUMES billing run with two government compensations; draft invoices only.
 *
 * Does NOT call start-generating or start-accounting (billing run stays DRAFT).
 *
 * Environment: Dev (default BASE_URL http://10.236.20.11:8091). Portal: http://10.236.20.11:8080
 *
 * Reference spec(s):
 * - tests/billing/forVolumes/forVolumes.spec.ts (REG-713 / REG-962)
 * - tests/cursor/pdt-2937-invoice-detailed-data-same-pc-name.fixtures.ts
 * - tests/cursor/dev-volume-billing-two-compensations.fixtures.ts
 */

import { test, expect } from './cursor-test.fixtures';
import {
  finalizeTestRunSummary,
  buildProductContractTabLinks,
} from './shared/manual-verification-links.fixtures';
import {
  DEV_DATA_KEY,
  DEV_PORTAL_BASE,
  buildDevBillingRunPreviewLink,
  buildDevCompensationPreviewLink,
  buildDevInvoicePreviewLink,
  createForVolumesDraftBillingRun,
  createTwoGovernmentCompensations,
  logDevVolCompCreatedEntities,
  runDevVolCompContractAndBbpPrechain,
  type DevVolCompScenarioResult,
} from './dev-volume-billing-two-compensations.fixtures';

test.describe('[DEV-DATA]: Volume billing with two compensations (draft only)', {
  tag: ['@billing', '@dev-data', '@compensations'],
}, () => {
  test(
    '[DEV-DATA]: Volume billing with two compensations — draft invoice only (no generate/accounting)',
    async ({ Request, GeneratePayload, Responses, Endpoints, TestRunSummary }) => {
      test.setTimeout(20 * 60 * 1000);
      const fx = { Request, GeneratePayload, Responses, Endpoints };

      const pre = await test.step(
        'Precondition: billed + recipient customers → PC → terms → POD → product → contract → activate → BBP',
        async () => runDevVolCompContractAndBbpPrechain(fx),
      );

      TestRunSummary.registerPayload('prechain', {
        customerId: pre.customerId,
        recipientCustomerId: pre.recipientCustomerId,
        podId: pre.podId,
        contractId: pre.contractId,
        bbpId: pre.bbpId,
        documentPeriod: pre.documentPeriod,
      });

      const compensations = await test.step(
        'Create two government compensations (distinct numbers, recipient = second customer)',
        async () => {
          expect(pre.recipientCustomerId, 'recipient must differ from billed customer').not.toBe(
            pre.customerId,
          );
          const created = await createTwoGovernmentCompensations(
            fx,
            pre.documentPeriod,
            pre.recipientCustomerId,
          );
          expect(created.ids.length, 'expected two compensation ids').toBe(2);
          expect(new Set(created.numbers).size, 'compensation numbers must be distinct').toBe(2);
          expect(created.payloads[0].recipientId).toBe(pre.recipientCustomerId);
          expect(created.payloads[1].recipientId).toBe(pre.recipientCustomerId);
          TestRunSummary.registerPayload('compensation1', created.payloads[0]);
          TestRunSummary.registerPayload('compensation2', created.payloads[1]);
          return created;
        },
      );

      const billing = await test.step(
        'Create FOR_VOLUMES billing run and start-billing only (no generate/accounting)',
        async () => createForVolumesDraftBillingRun(fx),
      );

      await test.step('Assert: billing run DRAFT with ≥1 draft invoice', async () => {
        expect(billing.billingRunStatus, 'billing run must remain DRAFT').toBe('DRAFT');
        expect(
          billing.draftInvoiceIds.length,
          'draft-invoices must return at least one invoice',
        ).toBeGreaterThanOrEqual(1);

        TestRunSummary.recordCheck({
          check: 'FOR_VOLUMES draft billing with two compensations',
          expectedResult:
            'Billing run status DRAFT after start-billing only; draft-invoices has ≥1 invoice; two compensations created.',
          actualResult: `As expected — status=${billing.billingRunStatus}, draftInvoices=${billing.draftInvoiceIds.length}, compensations=${compensations.ids.join(',')}`,
          passed: true,
        });
      });

      const result: DevVolCompScenarioResult = {
        ...pre,
        compensationIds: compensations.ids,
        compensationNumbers: compensations.numbers,
        billingRunId: billing.billingRunId,
        draftInvoiceIds: billing.draftInvoiceIds,
        billingRunStatus: billing.billingRunStatus,
      };

      await test.step('Log created entity ids and portal preview links', async () => {
        logDevVolCompCreatedEntities(result);
        test.info().attach('[DEV-DATA] created entities', {
          body: JSON.stringify(result, null, 2),
          contentType: 'application/json',
        });
      });

      await test.step('Attach test run summary', async () => {
        const extraLinks: Record<string, string[]> = {
          ...buildProductContractTabLinks(pre.contractId, DEV_PORTAL_BASE),
          billingRun: [buildDevBillingRunPreviewLink(billing.billingRunId)],
          invoice: billing.draftInvoiceIds.map(buildDevInvoicePreviewLink),
          compensation: compensations.ids.map(buildDevCompensationPreviewLink),
        };

        finalizeTestRunSummary(TestRunSummary, Responses, {
          jiraKey: DEV_DATA_KEY,
          relevantEntityKeys: ['customer', 'productContract', 'billingRun', 'invoice'],
          extraLinks,
          snapshot: {
            customerId: pre.customerId,
            recipientCustomerId: pre.recipientCustomerId,
            podId: pre.podId,
            contractId: pre.contractId,
            bbpId: pre.bbpId,
            compensationIds: compensations.ids,
            compensationNumbers: compensations.numbers,
            billingRunId: billing.billingRunId,
            billingRunStatus: billing.billingRunStatus,
            draftInvoiceIds: billing.draftInvoiceIds,
            portalBase: DEV_PORTAL_BASE,
          },
        });
      });
    },
  );
});
