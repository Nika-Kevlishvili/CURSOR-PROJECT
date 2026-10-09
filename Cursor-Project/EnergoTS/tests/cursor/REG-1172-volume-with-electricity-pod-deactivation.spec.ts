/**
 * REG-1172 — FOR_VOLUMES + WITH_ELECTRICITY_INVOICE: deactivated POD before meter reading "to"
 * must not receive electricity price component lines on the invoice.
 *
 * Precondition/billing flow aligned with [REG-991] electricity happy pass:
 * `tests/billing/electricity/withElectricity(Product).spec.ts`
 *
 * REG-1172 delta: 4-POD deactivation matrix + per-POD detailed-data electricity assert.
 *
 * Target environment: **dev2**
 *   $env:BASE_URL='https://devapps.energo-pro.bg/backend/phoenix-dev2'
 *   npx playwright test --project=setup
 *   npx playwright test tests/cursor/REG-1172-volume-with-electricity-pod-deactivation.spec.ts --project=main
 */

import { test, expect, finalizeTestRunSummary } from './cursor-test.fixtures';
import { envVariables } from '../../fixtures/envCashed';
import {
  REG_1172_POD_COUNT,
  applyPdt2376PodActivationMatrix,
  assertReg1172PodElectricityMatrix,
  buildReg1172ElectricityPriceComponentPayload,
  buildReg1172PodMatrix,
  buildReg1172ProductContractPayload,
  isReg1172Dev2Target,
  postReg1172BillingByProfileAllPods,
  postReg1172VolumeElectricityBillingRun,
  prepareReg1172LegalCustomerPayload,
  REG_1172_DEV2_BASE_URL,
  resolvePdt2376BillingAnchor,
  validateReg1172InvoiceTabs,
  type Pdt2376BillingAnchor,
  type Pdt2376PodMatrixRow,
  type Reg1172PerPodElectricityResult,
} from './reg-1172-volume-with-electricity.fixtures';

const JIRA_KEY = 'REG-1172';
const JIRA_TITLE =
  'Feedback - For volumes with electricity, electricity Deactivated POD should not be included invoice ';

function attachJson(title: string, payload: unknown): void {
  test.info().attach(title, {
    body: JSON.stringify(payload, null, 2),
    contentType: 'application/json',
  });
}

test.describe(`[${JIRA_KEY}]: Volume + WITH_ELECTRICITY — deactivated POD exclusion`, {
  tag: ['@billing', '@reg-1172', '@dev2'],
}, () => {
  test.describe.configure({ mode: 'serial' });

  test(`[${JIRA_KEY}]: ${JIRA_TITLE}`, async ({
    Request,
    GeneratePayload,
    Responses,
    Endpoints,
    validateInvoice,
    TestRunSummary,
  }) => {
    test.setTimeout(35 * 60 * 1000);

    let billingAnchor: Pdt2376BillingAnchor;
    let matrix: Pdt2376PodMatrixRow[] = [];
    let perPodResults: Reg1172PerPodElectricityResult[] = [];

    await test.step('Precondition: billing anchor (OPEN accounting period)', async () => {
      if (!isReg1172Dev2Target()) {
        test.info().annotations.push({
          type: 'environment',
          description: `Expected dev2 BASE_URL (${REG_1172_DEV2_BASE_URL}); got ${process.env.BASE_URL ?? 'default dev'}`,
        });
      }
      billingAnchor = await resolvePdt2376BillingAnchor(Request);
      matrix = buildReg1172PodMatrix(billingAnchor.profileEnd);
      attachJson(`[${JIRA_KEY}] billing anchor + POD matrix`, {
        anchor: billingAnchor,
        meterReadingTo: billingAnchor.profileEnd,
        matrix,
      });
      TestRunSummary.registerPayload('reg1172Matrix', { meterReadingTo: billingAnchor.profileEnd, matrix });
    });

    await test.step('generate customer', async () => {
      const customerPayload = prepareReg1172LegalCustomerPayload(
        GeneratePayload.customers.customer_legal() as Record<string, unknown>,
      );
      TestRunSummary.registerPayload('customer', customerPayload);
      const customer = await Request.post(Endpoints.customer, { data: customerPayload });
      await expect(customer).CheckResponse();
      Responses.customer.push(await customer.json());
    });

    await test.step('generate price component (volume)', async () => {
      const payload = GeneratePayload.productAndServices.priceSettlement();
      const price = await Request.post(Endpoints.priceComponent, { data: payload });
      await expect(price).CheckResponse();
      Responses.priceComponent.push(await price.json());
    });

    await test.step('generate electricity price component', async () => {
      const payload = buildReg1172ElectricityPriceComponentPayload(GeneratePayload);
      const priceElectricity = await Request.post(Endpoints.priceComponent, { data: payload });
      await expect(priceElectricity).CheckResponse();
      Responses.priceComponent.push(await priceElectricity.json());
    });

    await test.step('generate term', async () => {
      const term = await Request.post(Endpoints.terms, {
        data: GeneratePayload.productAndServices.term(),
      });
      await expect(term).CheckResponse();
      Responses.terms.push(await term.json());
    });

    await test.step(`generate ${REG_1172_POD_COUNT} PODs`, async () => {
      for (let i = 0; i < REG_1172_POD_COUNT; i++) {
        const podSettlement = await Request.post(Endpoints.pod, {
          data: GeneratePayload.pointsOfDelivery.pod_settlement(),
        });
        await expect(podSettlement).CheckResponse();
        Responses.pod.push(await podSettlement.json());
      }
    });

    await test.step('generate product', async () => {
      const product = await Request.post(Endpoints.product, {
        data: GeneratePayload.productAndServices.product(),
      });
      await expect(product).CheckResponse();
      Responses.product.push(await product.json());
    });

    await test.step('generate contract', async () => {
      const contractPayload = await buildReg1172ProductContractPayload(GeneratePayload, Request, Responses);
      const contract = await Request.post(Endpoints.productContract, { data: contractPayload });
      await expect(contract).CheckResponse();
      const contractBody = await contract.json();
      Responses.productContract.push(contractBody);
      TestRunSummary.registerPayload('productContract', contractBody);
    });

    await test.step('Activate PODs (REG-1172 deactivation matrix)', async () => {
      await applyPdt2376PodActivationMatrix({
        Request,
        contractId: Responses.productContract[0].id,
        pods: Responses.pod.map((p) => ({ id: p.id })),
        matrix,
        deactivationPurposeId: envVariables.deactivation_reason,
      });
    });

    await test.step('Data by profiles', async () => {
      await postReg1172BillingByProfileAllPods({ Request, GeneratePayload, Responses }, billingAnchor!);
    });

    await test.step('Billing run', async () => {
      await postReg1172VolumeElectricityBillingRun(
        { Request, GeneratePayload, Responses, Endpoints },
        billingAnchor!,
      );
      TestRunSummary.registerPayload('billingRun', {
        models: ['FOR_VOLUMES', 'WITH_ELECTRICITY_INVOICE'],
        accountingPeriodId: billingAnchor!.accountingPeriodId,
        invoiceDate: billingAnchor!.invoiceDate,
      });
    });

    await test.step('invoice generation', async () => {
      await GeneratePayload.billing.waitForInvoiceGeneration(true, true, 1, 0);
    });

    await test.step('validate invoice', async () => {
      await validateReg1172InvoiceTabs(validateInvoice);
    });

    await test.step(
      'Assert: REG-1172 — deactivated POD before meter reading to excluded from electricity detailed-data',
      async () => {
        const outcome = await assertReg1172PodElectricityMatrix({
          Request,
          Responses,
          matrix,
          billingAnchor: billingAnchor!,
          jiraKey: JIRA_KEY,
        });
        perPodResults = outcome.perPodResults;
        attachJson(`[${JIRA_KEY}] detailed-data aggregation`, {
          invoiceIdsScanned: outcome.invoiceIds,
          electricityPriceComponentId: outcome.electricityCtx.electricityPriceComponentId ?? null,
        });
        attachJson(`[${JIRA_KEY}] per-POD electricity verdicts`, outcome.perPodAttachment);
        attachJson(`[${JIRA_KEY}] verdict matrix summary`, {
          meterReadingTo: billingAnchor!.profileEnd,
          rows: outcome.summaryRows,
        });
      },
    );

    await test.step('Attach test run summary', async () => {
      const allPassed = perPodResults.every((r) => r.expected === r.actual);
      TestRunSummary.recordCheck({
        check: 'Deactivated POD before meter reading to excluded from WITH_ELECTRICITY lines',
        expectedResult:
          'POD1: no electricity; POD2/POD3/POD4: electricity present (per deactivation vs meterReadingTo)',
        actualResult: perPodResults
          .map((r) => `${r.label}(${r.identifier}): expected=${r.expected} actual=${r.actual}`)
          .join('; '),
        passed: allPassed,
      });
      finalizeTestRunSummary(TestRunSummary, Responses, {
        jiraKey: JIRA_KEY,
        relevantEntityKeys: [
          'customer',
          'priceComponent',
          'product',
          'pod',
          'productContract',
          'billingRun',
          'invoice',
        ],
        snapshot: {
          meterReadingTo: billingAnchor!.profileEnd,
          matrix,
          perPodResults,
        },
      });
    });
  });
});
