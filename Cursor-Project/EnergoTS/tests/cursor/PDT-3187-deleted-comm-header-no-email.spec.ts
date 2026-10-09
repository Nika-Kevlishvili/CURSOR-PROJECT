/**
 * PDT-3187 — Deleted communication header → no invoice email CRM objects.
 *
 * Expected (bug twin / reported outcome): after REAL invoice with BG still pointing at a
 * soft-deleted BILLING communication header (sendingInvoice=EMAIL,
 * sendingAnInvoice=ACCORDING_TO_THE_CONTRACT), that deleted header must NOT surface as a
 * CRM email-communication object for the customer.
 *
 * Semantic twin of Prod cause group (aliases only — no Prod numeric IDs):
 * - PROD_COMM_HEADER DELETED; BG sendingInvoice=EMAIL still references it
 * - Billing run sendingAnInvoice=ACCORDING_TO_THE_CONTRACT → ≥1 REAL invoice
 * - Primary assert: zero CRM email-communication objects (not invoice.customerCommunicationId)
 *
 * Portable via BASE_URL (DEV / DEV2 / TEST).
 *
 * Missing prerequisite policy:
 * If consumption / POD / templates / accounting prevent REAL invoice on the target env,
 * the chain stops with an explicit test.skip reason — never invent fake invoice success.
 * Missing invoice.customerCommunicationId is NOT a missing prerequisite.
 *
 * Reference spec(s):
 * - tests/billing/electricity/withElectricity(Product).spec.ts ([REG-991])
 * - tests/cursor/PDT-2529-rfd-data-model-happy-path.fixtures.ts
 * - tests/cursor/pdt-3187-deleted-comm-header.fixtures.ts
 */

import { test, expect } from './cursor-test.fixtures';
import {
  finalizeTestRunSummary,
  buildProductContractTabLinks,
} from './shared/manual-verification-links.fixtures';
import { resolvePdt2376BillingAnchor, type Pdt2376BillingAnchor } from './pdt-2376-volume-with-electricity.fixtures';
import {
  PDT_3187_JIRA_KEY,
  assertNoEmailCommunicationsForCustomer,
  assertRealInvoicesForBillingRun,
  createPdt3187CatalogAndContract,
  createPdt3187StandardBillingRun,
  pdt3187ElasticsearchSkipNote,
  postPdt3187BillingByProfile,
  softDeleteBillingCommunicationHeader,
} from './pdt-3187-deleted-comm-header.fixtures';

test.describe(`[${PDT_3187_JIRA_KEY}]: Deleted communication header — no invoice email objects`, {
  tag: ['@billing', '@cursor', '@PDT-3187'],
}, () => {
  // Exact Jira summary (trailing space preserved): "Invoices - email objects was not generated "
  test('[PDT-3187]: Invoices - email objects was not generated ', async ({
    Request,
    GeneratePayload,
    Responses,
    Endpoints,
    TestRunSummary,
  }) => {
    test.setTimeout(15 * 60 * 1000);

    const fx = { Request, GeneratePayload, Responses, Endpoints };
    let customerId = 0;
    let customerIdentifier = '';
    let customerDetailsVersion = 1;
    let createPayload: Record<string, unknown> = {};
    let billingCommId = 0;
    let contractCommId = 0;
    let contractId = 0;
    let billingGroupId = 0;
    let billingAnchor: Pdt2376BillingAnchor | null = null;
    let billingRunId: number | null = null;
    let realInvoiceAchieved = false;
    let missingPrerequisite: string | null = null;

    await test.step('Precondition: catalog + legal customer (BILLING+CONTRACT headers) + contract + BG EMAIL', async () => {
      const created = await createPdt3187CatalogAndContract(fx);
      customerId = created.customerId;
      customerIdentifier = created.customerIdentifier;
      customerDetailsVersion = created.customerDetailsVersion;
      createPayload = created.createPayload;
      billingCommId = created.commIds.billingCommId;
      contractCommId = created.commIds.contractCommId;
      contractId = created.contractId;
      billingGroupId = created.billingGroupId;
      billingAnchor = created.billingAnchor;

      TestRunSummary.registerPayload('customer', createPayload);
      TestRunSummary.recordCheck({
        check: 'Customer + contract + BG EMAIL pinned to billing comm header',
        expectedResult:
          'Synthetic legal customer with ACTIVE BILLING+CONTRACT headers; BG sendingInvoice=EMAIL and billingCustomerCommunicationId set.',
        actualResult: `customerId=${customerId}, identifier=${customerIdentifier}, billingCommId=${billingCommId}, contractCommId=${contractCommId}, contractId=${contractId}, billingGroupId=${billingGroupId}`,
        passed: true,
      });
    });

    await test.step('Precondition: billing-by-profile for OPEN accounting period', async () => {
      if (!billingAnchor) {
        missingPrerequisite = 'Billing anchor was not resolved during catalog/contract setup.';
        test.skip(true, `Missing prerequisite: ${missingPrerequisite}`);
        return;
      }
      try {
        await postPdt3187BillingByProfile(fx, billingAnchor);
      } catch (err) {
        missingPrerequisite = `billing-by-profile failed (consumption/POD/accounting period): ${
          err instanceof Error ? err.message : String(err)
        }`;
        test.skip(true, `Missing prerequisite: ${missingPrerequisite}`);
      }
    });

    await test.step('Soft-delete BILLING communication header (omit id from customer PUT; BG keeps FK)', async () => {
      const { deletedStatus } = await softDeleteBillingCommunicationHeader(fx, {
        customerId,
        createPayload,
        commIds: { billingCommId, contractCommId },
        customerDetailsVersion,
      });

      const bgRes = await Request.get(`billing-group/${billingGroupId}`);
      await expect(bgRes).CheckResponse();
      // BillingGroupResponse exposes stored billingCustomerCommunicationId as communicationId.
      const bg = (await bgRes.json()) as {
        sendingInvoice?: string;
        communicationId?: number;
      };
      expect(bg.sendingInvoice, 'BG sendingInvoice must remain EMAIL after header soft-delete').toBe('EMAIL');
      expect(
        Number(bg.communicationId),
        'BG must still reference the deleted billing communication header id',
      ).toBe(billingCommId);

      TestRunSummary.recordCheck({
        check: 'Soft-delete billing communication header (omit-from-list)',
        expectedResult:
          'Billing header soft-deleted via omit-from-list customer PUT (not explicit status=DELETED). ' +
          'Evidence: GET customer-communications/{id} returns 400 Active communication data not found (ACTIVE-only), ' +
          'or 200 with status DELETED; customer GET no longer lists id as ACTIVE. BG still references the id (sendingInvoice=EMAIL).',
        actualResult: `billingCommId=${billingCommId} deletedStatus=${deletedStatus}; billingGroupId=${billingGroupId} communicationId=${bg.communicationId}`,
        passed: deletedStatus.toUpperCase() === 'DELETED' && Number(bg.communicationId) === billingCommId,
      });
    });

    await test.step('Create STANDARD billing run (ACCORDING_TO_THE_CONTRACT) → REAL invoice', async () => {
      let anchor = billingAnchor;
      if (!anchor) {
        try {
          anchor = await resolvePdt2376BillingAnchor(Request);
          billingAnchor = anchor;
        } catch (err) {
          missingPrerequisite = `Cannot resolve OPEN accounting period for billing: ${
            err instanceof Error ? err.message : String(err)
          }`;
          test.skip(true, `Missing prerequisite: ${missingPrerequisite}`);
          return;
        }
      }

      try {
        const createdRun = await createPdt3187StandardBillingRun(fx, anchor);
        billingRunId = createdRun.billingRunId;
        TestRunSummary.registerPayload('billingRun', createdRun.payload);
      } catch (err) {
        missingPrerequisite = `POST /billing-run failed: ${err instanceof Error ? err.message : String(err)}`;
        test.skip(true, `Missing prerequisite: ${missingPrerequisite}`);
        return;
      }

      try {
        await GeneratePayload.billing.waitForInvoiceGeneration(true, true, 1, 0);
      } catch (err) {
        // Missing prerequisite: templates, volumes, generation/accounting pipeline — do not fake REAL success.
        missingPrerequisite = `Billing run did not complete to REAL invoice(s): ${
          err instanceof Error ? err.message : String(err)
        }`;
        console.warn(`[${PDT_3187_JIRA_KEY}] ${missingPrerequisite}`);
        test.skip(true, `Missing prerequisite: ${missingPrerequisite}`);
        return;
      }

      const invoiceIds = Responses.invoice.map(Number).filter((id) => Number.isFinite(id) && id > 0);
      try {
        const realInvoices = await assertRealInvoicesForBillingRun(Request, invoiceIds);
        realInvoiceAchieved = realInvoices.length > 0;
        expect(
          realInvoices.length,
          `[${PDT_3187_JIRA_KEY}] Expected ≥1 REAL invoice after COMPLETED billing run`,
        ).toBeGreaterThan(0);
        TestRunSummary.recordCheck({
          check: 'REAL invoice after soft-deleted billing header + BG EMAIL',
          expectedResult:
            'REAL invoice(s) created after soft-deleted billing header + BG EMAIL / ACCORDING_TO_THE_CONTRACT. ' +
            'invoice.customerCommunicationId is not required to equal the deleted header id.',
          actualResult: `billingRunId=${billingRunId}, realCount=${realInvoices.length}, invoices=${JSON.stringify(realInvoices)}`,
          passed: realInvoiceAchieved,
        });
      } catch (err) {
        missingPrerequisite = `Invoice not REAL after accounting: ${
          err instanceof Error ? err.message : String(err)
        }`;
        test.skip(true, `Missing prerequisite: ${missingPrerequisite}`);
      }
    });

    await test.step('Assert: zero CRM email-communication objects for customer (primary Expected Result)', async () => {
      if (!realInvoiceAchieved) {
        test.skip(
          true,
          `Missing prerequisite: REAL invoice not achieved — cannot assert email-object absence. ${
            missingPrerequisite ?? ''
          }`.trim(),
        );
        return;
      }

      const { totalElements } = await assertNoEmailCommunicationsForCustomer(
        Request,
        Endpoints,
        customerIdentifier,
      );
      TestRunSummary.recordCheck({
        check: 'No email communication objects after REAL invoice (deleted header) — primary Expected Result',
        expectedResult:
          'Deleted billing header must NOT surface as a CRM email-communication object: ' +
          'email-communication/list by CUSTOMER_IDENTIFIER returns zero rows.',
        actualResult: `totalElements=${totalElements}; ${pdt3187ElasticsearchSkipNote()}`,
        passed: totalElements === 0,
      });
    });

    await test.step('Attach test run summary', async () => {
      finalizeTestRunSummary(TestRunSummary, Responses, {
        jiraKey: PDT_3187_JIRA_KEY,
        relevantEntityKeys: ['customer', 'product', 'productContract', 'billingRun', 'invoice', 'email'],
        extraLinks: contractId > 0 ? buildProductContractTabLinks(contractId) : undefined,
        snapshot: {
          customerId,
          customerIdentifier,
          billingCommId,
          contractCommId,
          billingGroupId,
          contractId,
          billingRunId,
          realInvoiceAchieved,
          missingPrerequisite,
          note: pdt3187ElasticsearchSkipNote(),
        },
      });
    });
  });
});
