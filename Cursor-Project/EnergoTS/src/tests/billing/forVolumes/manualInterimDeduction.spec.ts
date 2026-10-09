/**
 * [REG-1297]: Manual interim billing run - standard invoice deduction (from same period)
 * Linked to REG-562 (Interim and advance payments).
 *
 * Flow (Dev):
 * 1) Fresh customer → contract → BBP (current month)
 * 2) MANUAL_INTERIM_AND_ADVANCE_PAYMENT (amountExcludingVat=100, FIRST_INVOICE_FOR_SAME_PERIOD, ZERO)
 *    → complete to REAL interim
 * 3) FOR_VOLUMES on same contract → STANDARD invoice
 * 4) Assert STANDARD summary INTERIM row = −100 (runtime deduction path)
 *
 * Not REG-718 (product IAP). Staging branch per explicit user permission (overrides ENERGOTS.0).
 */
import { test, expect } from '../../../backend/fixtures/baseFixture';
import reportGenerator from '../../../backend/utils/generateReport';
import {
  BILLING_TIMEOUT_MS,
  JIRA_KEY,
  JIRA_TITLE,
  MANUAL_INTERIM_AMOUNT_EXCL_VAT,
  assertStandardInvoiceDeductsManualInterim,
  createAndCompleteForVolumesOnSameContract,
  createAndCompleteManualInterim,
  runManualInterimDeductionPrechain,
} from './manualInterimDeduction.fixtures';

test.describe(`[${JIRA_KEY}]: ${JIRA_TITLE}`, {
  tag: [`@${JIRA_KEY}`, '@billing', '@manual-interim', '@deduction'],
}, () => {
  test.describe.configure({ mode: 'serial' });

  test(
    `[${JIRA_KEY}]: Standard invoice is deducted by manual interim billing run amount`,
    { tag: [`@${JIRA_KEY}`, '@billing', '@manual-interim', '@deduction'] },
    async ({ Request, GeneratePayload, Responses, Endpoints, FileUploadRequest }) => {
      test.setTimeout(BILLING_TIMEOUT_MS);
      const fx = { Request, GeneratePayload, Responses, Endpoints, FileUploadRequest };

      const chain = await runManualInterimDeductionPrechain(fx);

      const { interimInvoiceId, interimExclVat } = await test.step(
        'Create and complete MANUAL_INTERIM_AND_ADVANCE_PAYMENT',
        async () =>
          createAndCompleteManualInterim(fx, {
            customerDetailId: chain.customerDetailId,
            communicationDataId: chain.communicationDataId,
            contractId: chain.contractId,
            billingGroupId: chain.billingGroupId,
          }),
      );

      expect(interimExclVat).toBe(MANUAL_INTERIM_AMOUNT_EXCL_VAT);

      const { standardInvoiceId } = await test.step(
        'Create and complete FOR_VOLUMES on same contract',
        async () => createAndCompleteForVolumesOnSameContract(fx),
      );

      await test.step('Assert STANDARD invoice deducts manual interim (summary INTERIM row)', async () => {
        await assertStandardInvoiceDeductsManualInterim(Request, Endpoints, {
          interimInvoiceId,
          standardInvoiceId,
          interimExclVat,
          bbpPeriodFrom: chain.bbpPeriodFrom,
          bbpPeriodTo: chain.bbpPeriodTo,
        });
      });

      test.info().attach(`[${JIRA_KEY}] response`, {
        body: JSON.stringify(reportGenerator.setLinksToResponses(Responses), null, 2),
        contentType: 'application/json',
      });
    },
  );
});
