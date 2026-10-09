/**
 * PDT-3326 — Concurrent CREATE of two Disconnection of Power Supply (DPS) objects
 * for the same Request for Disconnection (RFD) / Customer / POD.
 *
 * Product rule (Jira expected): the system must not allow a second disconnection for the same
 * RFD / Customer / POD / liabilities. After the fix, two overlapping POST
 * /disconnection-of-power-supply with the same RFD+POD must yield exactly one created
 * EXECUTED DPS; the other CREATE is rejected.
 *
 * Race launch: two independent Playwright APIRequestContext clients (separate TCP
 * connections) so the POSTs are not serialized on a single HTTP/1.1 connection.
 *
 * Swagger (dev): POST body DisconnectionOfPowerSupplyRequest —
 *   requestForDisconnectionId, saveType (DRAFT|EXECUTED), disconnectedRequest[]
 *   (customerId, podId, gridOperatorTaxesId, dateOfDisconnection, expressReconnection).
 * POST returns Long (new DPS id). GET /disconnection-of-power-supply/{id} →
 *   DisconnectionOfPowerSupplyResponse (disconnectionStatus, powerSupplyDisconnectionRequestId, …).
 * GET /disconnection-of-power-supply/table/executed?disconnectionId=&page=&size= →
 *   page of DisconnectionPowerSupplyTableList (podId, customerId, …).
 *
 * Reference spec(s):
 * - tests/receivableManagement/disconnectionOfPowerSupply.spec.ts
 * - tests/receivableManagement/ReconnectionOfPowerSupply/ReconnectionAfterPayment.spec.ts (expressReconnection)
 * - tests/cursor/PDT-2880-pod-disconnected-banner.spec.ts (cursor fixtures / summary patterns)
 */

import { test, expect } from './cursor-test.fixtures';
import { finalizeTestRunSummary } from './shared/manual-verification-links.fixtures';
import { getToken } from '../../fixtures/utils/auth';
import { configuredBaseURL } from '../../fixtures/utils/baseUrl';
import { RequestWrapper } from '../../utils/RequestWrapper';

const JIRA_KEY = 'PDT-3326';
const JIRA_TITLE =
  'Disconnection of power supply-2 Disconnections for one customer/POD/liability';
const TIMEOUT_MS = 15 * 60 * 1000;

type FixtureBundle = {
  Request: any;
  GeneratePayload: any;
  Responses: any;
  Endpoints: any;
};

function deepClone<T>(value: T): T {
  return JSON.parse(JSON.stringify(value)) as T;
}

async function createParallelDpsClients(playwright: {
  request: { newContext: (options: Record<string, unknown>) => Promise<{ dispose: () => Promise<void> }> };
}) {
  const options = {
    baseURL: configuredBaseURL,
    extraHTTPHeaders: {
      Accept: '*/*',
      'Content-Type': 'application/json',
      Authorization: `Bearer ${getToken()}`,
    },
  };
  const contextA = await playwright.request.newContext(options);
  const contextB = await playwright.request.newContext(options);
  return {
    wrapA: new RequestWrapper(contextA as never),
    wrapB: new RequestWrapper(contextB as never),
    dispose: async () => {
      await Promise.all([contextA.dispose(), contextB.dispose()]);
    },
  };
}

function asId(value: unknown): number {
  if (typeof value === 'number') {
    return value;
  }
  if (value && typeof value === 'object' && 'id' in (value as object)) {
    return Number((value as { id: number }).id);
  }
  return Number(value);
}

async function createLegalCustomer(fx: FixtureBundle): Promise<void> {
  const response = await fx.Request.post(fx.Endpoints.customer, {
    data: fx.GeneratePayload.customers.customer_legal(),
  });
  await expect(response).CheckResponse();
  fx.Responses.customer.push(await response.json());
}

async function createTerm(fx: FixtureBundle): Promise<void> {
  const response = await fx.Request.post(fx.Endpoints.terms, {
    data: fx.GeneratePayload.productAndServices.term(),
  });
  await expect(response).CheckResponse();
  fx.Responses.terms.push(await response.json());
}

async function createPod(fx: FixtureBundle): Promise<void> {
  const response = await fx.Request.post(fx.Endpoints.pod, {
    data: fx.GeneratePayload.pointsOfDelivery.pod_settlement(),
  });
  await expect(response).CheckResponse();
  fx.Responses.pod.push(await response.json());
}

async function createProduct(fx: FixtureBundle): Promise<void> {
  const response = await fx.Request.post(fx.Endpoints.product, {
    data: fx.GeneratePayload.productAndServices.product(),
  });
  await expect(response).CheckResponse();
  fx.Responses.product.push(await response.json());
}

async function createProductContract(fx: FixtureBundle): Promise<void> {
  const response = await fx.Request.post(fx.Endpoints.productContract, {
    data: await fx.GeneratePayload.contractsAndOrders.product_contract(),
  });
  await expect(response).CheckResponse();
  fx.Responses.productContract.push(await response.json());
}

async function activatePod(fx: FixtureBundle): Promise<void> {
  const response = await fx.Request.post('/contract-pods/manual', {
    data: await fx.GeneratePayload.pointsOfDelivery.pod_activation(),
  });
  await expect(response).CheckResponse();
}

async function createManualInvoiceBillingRun(fx: FixtureBundle): Promise<void> {
  const payload = await fx.GeneratePayload.billing.manualInvoice();
  const response = await fx.Request.post(fx.Endpoints.billingRun, { data: payload });
  await expect(response).CheckResponse();
  fx.Responses.billingRun.push(await response.json());
}

async function createReminderForDisconnection(fx: FixtureBundle): Promise<void> {
  const payload = fx.GeneratePayload.receivablesManagement.reminderForDisconnection();
  const response = await fx.Request.post(fx.Endpoints.reminderForDisconnection, { data: payload });
  await expect(response).CheckResponse();
  fx.Responses.reminderForDisconnection.push(await response.json());
  await fx.GeneratePayload.receivablesManagement.offsetReminderForDisconnectionTime();
}

async function createRequestForDisconnection(fx: FixtureBundle): Promise<void> {
  const payload = fx.GeneratePayload.receivablesManagement.requestForDisconnection();
  const response = await fx.Request.post(fx.Endpoints.requestForDisconnection, { data: payload });
  await expect(response).CheckResponse();
  fx.Responses.requestForDisconnection.push(await response.json());
}

async function chargeFeeOnRequestForDisconnection(fx: FixtureBundle): Promise<void> {
  const rfdId = asId(fx.Responses.requestForDisconnection[0]);
  const response = await fx.Request.post(
    `disconnection-of-power-supply-requests/calculate-tax/${rfdId}`,
  );
  await expect(response).CheckResponse();
}

async function fetchDpsView(fx: FixtureBundle, dpsId: number) {
  const response = await fx.Request.get(`${fx.Endpoints.disconnectionOfPowerSupply}/${dpsId}`);
  await expect(response).CheckResponse();
  return response.json();
}

async function fetchExecutedPodsForDps(fx: FixtureBundle, dpsId: number) {
  // Swagger/dev: GET /disconnection-of-power-supply/table/executed
  // query: disconnectionId (required), page, size — returns Page<DisconnectionPowerSupplyTableList>
  const response = await fx.Request.get(
    `${fx.Endpoints.disconnectionOfPowerSupply}/table/executed`,
    {
      params: {
        disconnectionId: dpsId,
        page: 0,
        size: 25,
      },
    },
  );
  await expect(response).CheckResponse();
  const body = await response.json();
  return (body.content ?? body) as Array<{ podId?: number; customerId?: number }>;
}

test.describe(`[${JIRA_KEY}]: ${JIRA_TITLE}`, { tag: ['@receivableManagement', '@pdt-3326', '@dev'] }, () => {
  test(`[${JIRA_KEY}]: ${JIRA_TITLE}`, async ({
    Request,
    GeneratePayload,
    Responses,
    Endpoints,
    TestRunSummary,
    playwright,
  }) => {
    test.setTimeout(TIMEOUT_MS);
    const fx: FixtureBundle = { Request, GeneratePayload, Responses, Endpoints };

    await test.step('Precondition: Create legal customer', async () => {
      await createLegalCustomer(fx);
    });

    await test.step('Precondition: Create term', async () => {
      await createTerm(fx);
    });

    await test.step('Precondition: Create POD', async () => {
      await createPod(fx);
    });

    await test.step('Precondition: Create product', async () => {
      await createProduct(fx);
    });

    await test.step('Precondition: Create product contract', async () => {
      await createProductContract(fx);
    });

    await test.step('Precondition: Activate POD', async () => {
      await activatePod(fx);
    });

    await test.step('Precondition: Create billing run for manual invoice', async () => {
      await createManualInvoiceBillingRun(fx);
    });

    await test.step('Precondition: Wait for invoice generation', async () => {
      await GeneratePayload.billing.waitForInvoiceGeneration();
    });

    await test.step('Precondition: Generate reminder for disconnection', async () => {
      await createReminderForDisconnection(fx);
    });

    await test.step('Precondition: Generate request for disconnection', async () => {
      await createRequestForDisconnection(fx);
    });

    await test.step('Precondition: Charge fee (calculate-tax on RFD)', async () => {
      await chargeFeeOnRequestForDisconnection(fx);
    });

    const customerId = Number(Responses.customer[0].id);
    const podId = Number(Responses.pod[0].id);
    const rfdId = asId(Responses.requestForDisconnection[0]);

    let createdDpsId = 0;
    let rejectedStatus = 0;
    let rejectedBody: unknown = null;
    let racePayload: Record<string, unknown> = {};

    await test.step('Race: two concurrent POST /disconnection-of-power-supply (same RFD+POD)', async () => {
      // Build ONE payload via generator, then deep-clone so both requests share RFD/customer/POD.
      const payload = GeneratePayload.receivablesManagement.disconnectionOfPowerSupply() as {
        requestForDisconnectionId: number | null;
        saveType: string;
        disconnectedRequest: Array<{
          customerId: number | null;
          podId: number | null;
          gridOperatorTaxesId: number;
          expressReconnection: boolean;
          dateOfDisconnection: string;
        }>;
      };
      payload.saveType = 'EXECUTED';
      payload.disconnectedRequest[0].expressReconnection = true;

      const payloadA = deepClone(payload);
      const payloadB = deepClone(payload);
      racePayload = payloadA as unknown as Record<string, unknown>;

      TestRunSummary.registerPayload('disconnectionOfPowerSupplyRaceA', payloadA);
      TestRunSummary.registerPayload('disconnectionOfPowerSupplyRaceB', payloadB);

      expect(payloadA.requestForDisconnectionId).toBe(rfdId);
      expect(payloadB.requestForDisconnectionId).toBe(rfdId);
      expect(payloadA.disconnectedRequest[0].customerId).toBe(customerId);
      expect(payloadB.disconnectedRequest[0].customerId).toBe(customerId);
      expect(payloadA.disconnectedRequest[0].podId).toBe(podId);
      expect(payloadB.disconnectedRequest[0].podId).toBe(podId);

      const parallel = await createParallelDpsClients(playwright);
      try {
        // Start both POSTs before awaiting either — separate HTTP clients / TCP connections.
        const pendingA = parallel.wrapA.post(Endpoints.disconnectionOfPowerSupply, { data: payloadA });
        const pendingB = parallel.wrapB.post(Endpoints.disconnectionOfPowerSupply, { data: payloadB });
        const [responseA, responseB] = await Promise.all([pendingA, pendingB]);

        const pair = [
          { label: 'A', response: responseA },
          { label: 'B', response: responseB },
        ];
        const succeeded = pair.filter((item) => item.response.ok());
        const rejected = pair.filter((item) => !item.response.ok());

        expect(
          succeeded.length,
          `exactly one concurrent DPS CREATE must succeed (statuses A=${responseA.status()} B=${responseB.status()})`,
        ).toBe(1);
        expect(
          rejected.length,
          `exactly one concurrent DPS CREATE must be rejected (statuses A=${responseA.status()} B=${responseB.status()})`,
        ).toBe(1);

        await expect(succeeded[0].response).CheckResponse();
        createdDpsId = asId(await succeeded[0].response.json());
        Responses.disconnectionOfPowerSupply.push(createdDpsId);
        expect(createdDpsId).toBeGreaterThan(0);

        rejectedStatus = rejected[0].response.status();
        expect(rejectedStatus).toBeGreaterThanOrEqual(400);
        try {
          rejectedBody = await rejected[0].response.json();
        } catch {
          rejectedBody = '(non-JSON body)';
        }

        TestRunSummary.recordCheck({
          check: 'Concurrent DPS CREATE: one success, one reject',
          expectedResult:
            'Exactly one POST /disconnection-of-power-supply with the same RFD+POD succeeds and returns a DPS id; the other CREATE is rejected (HTTP >= 400).',
          actualResult: `As expected — winner=${succeeded[0].label} dpsId=${createdDpsId}; rejected=${rejected[0].label} status=${rejectedStatus} body=${JSON.stringify(rejectedBody)}; rfdId=${rfdId}, podId=${podId}, customerId=${customerId}.`,
          passed: true,
        });
      } finally {
        await parallel.dispose();
      }
    });

    await test.step('Verify: only the successful DPS is EXECUTED and includes the podId', async () => {
      const view = await fetchDpsView(fx, createdDpsId);

      expect(view.disconnectionStatus).toBe('EXECUTED');
      expect(asId(view.powerSupplyDisconnectionRequestId)).toBe(rfdId);

      const pods = await fetchExecutedPodsForDps(fx, createdDpsId);
      const podIds = pods.map((row) => Number(row.podId)).filter((id) => Number.isFinite(id));

      expect(podIds).toContain(podId);

      TestRunSummary.recordCheck({
        check: 'Single EXECUTED DPS owns the POD',
        expectedResult:
          'GET view of the successful DPS is EXECUTED for the same RFD; GET table/executed includes the POD. The rejected CREATE has no DPS id.',
        actualResult: `As expected — dpsId=${createdDpsId} status=${view.disconnectionStatus} pods=${JSON.stringify(podIds)}; rejectedStatus=${rejectedStatus}.`,
        passed: true,
      });
    });

    await test.step('Attach test run summary', async () => {
      TestRunSummary.registerPayload('disconnectionOfPowerSupply', racePayload);
      finalizeTestRunSummary(TestRunSummary, Responses, {
        jiraKey: JIRA_KEY,
        relevantEntityKeys: [
          'customer',
          'pod',
          'product',
          'productContract',
          'billingRun',
          'reminderForDisconnection',
          'requestForDisconnection',
          'disconnectionOfPowerSupply',
        ],
        snapshot: {
          customerId,
          podId,
          rfdId,
          createdDpsId,
          rejectedStatus,
          rejectedBody,
          raceNote:
            'Concurrent EXECUTED CREATE: exactly one DPS is created; the other POST is rejected.',
        },
      });
    });
  });
});
