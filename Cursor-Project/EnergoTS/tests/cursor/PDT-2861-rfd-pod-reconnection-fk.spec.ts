import { test, expect } from '../../fixtures/baseFixture';
import reportGenerator from '../../utils/generateReport';
import { envVariables } from '../../fixtures/envCashed';

type CustomersForDPSQuery = {
  page: number;
  size: number;
  conditionType: 'LIST_OF_CUSTOMERS';
  listOfCustomer: string;
  powerSupplyDisconnectionReminderId: number;
  gridOperatorId: number;
  searchBy?: string;
  prompt?: string;
};

function reminderIdFromResponses(Responses: { reminderForDisconnection: unknown[] }): number {
  const r = Responses.reminderForDisconnection[0] as number | { id: number };
  return typeof r === 'number' ? r : r.id;
}

function rfdPayloadTemplate(GeneratePayload: any, Responses: any): Record<string, unknown> {
  const payload = GeneratePayload.receivablesManagement.requestForDisconnection() as Record<string, unknown>;
  payload.reminderForDisconnectionId = reminderIdFromResponses(Responses);
  return payload;
}

async function createRequestCDraftListCustomers(
  Request: any,
  GeneratePayload: any,
  Responses: any,
  Endpoints: any,
): Promise<number> {
  const payload = rfdPayloadTemplate(GeneratePayload, Responses);
  payload.disconnectionRequestsStatus = 'DRAFT';
  payload.conditionType = 'LIST_OF_CUSTOMERS';
  payload.listOfCustomer = Responses.customer[0].identifier as string;
  payload.allSelected = true;
  payload.pods = [];
  payload.podWithHighestConsumption = false;
  const res = await Request.post(Endpoints.requestForDisconnection, { data: payload });
  await (expect(res) as any).CheckResponse();
  return Number(await res.json());
}

/** Full grid row from Load PODs / customers-for-DPS — required by POST validate when pods[] is non-empty. */
async function fetchPodRowFromLoadCustomersTab(
  Request: any,
  baseQuery: CustomersForDPSQuery,
  podIdentifier: string,
): Promise<Record<string, unknown>> {
  const params = {
    ...baseQuery,
    searchBy: 'POD_IDENTIFIER',
    prompt: podIdentifier,
  };
  const res = await Request.get('disconnection-of-power-supply-requests/load-customer-for-disconnection-power-supply', {
    params: params as Record<string, string | number>,
  });
  await (expect(res) as any).CheckResponse();
  const body = await res.json();
  const row = body.content?.find((r: { podIdentifier?: string }) => r.podIdentifier === podIdentifier);
  expect(row, `Expected POD row on load-customer-for-disconnection for identifier ${podIdentifier}`).toBeTruthy();
  return { ...(row as Record<string, unknown>), isChecked: true };
}

async function createRequestAExecutedWithPod(
  Request: any,
  GeneratePayload: any,
  Responses: any,
  Endpoints: any,
  podRow: Record<string, unknown>,
): Promise<number> {
  const payload = rfdPayloadTemplate(GeneratePayload, Responses);
  payload.disconnectionRequestsStatus = 'EXECUTED';
  payload.conditionType = 'LIST_OF_CUSTOMERS';
  payload.listOfCustomer = Responses.customer[0].identifier as string;
  payload.allSelected = false;
  payload.podWithHighestConsumption = false;
  payload.pods = [podRow];
  const res = await Request.post(Endpoints.requestForDisconnection, { data: payload });
  await (expect(res) as any).CheckResponse();
  return Number(await res.json());
}

async function loadViewPodTab(
  Request: any,
  requestDraftId: number,
  query: CustomersForDPSQuery,
): Promise<any> {
  const res = await Request.get(`disconnection-of-power-supply-requests/view-pod-tab/${requestDraftId}`, {
    params: query as Record<string, string | number>,
  });
  await (expect(res) as any).CheckResponse();
  return res.json();
}

function findPodInViewPodTabContent(pageBody: { content?: Array<{ podIdentifier?: string }> }, podIdentifier: string) {
  return pageBody.content?.find((row) => row.podIdentifier === podIdentifier);
}

test.describe('[PDT-2861]: Request for disconnection — Load PODs / executed RFD interaction', { tag: '@receivableManagement' }, () => {
  test('[PDT-2861] POD missing from Load PODs — open executed RFD without reconnection (Prod-1048 symptom)', async ({
    Request,
    GeneratePayload,
    Responses,
    Endpoints,
  }) => {
    test.setTimeout(15 * 60 * 1000);

    let requestCDraftId = 0;
    let requestAExecutedId = 0;
    let resolvedPodIdentifier = '';

    await test.step('Precondition: customer', async () => {
      const customer = await Request.post(Endpoints.customer, { data: GeneratePayload.customers.customer_legal() });
      await expect(customer).CheckResponse();
      Responses.customer.push(await customer.json());
    });

    await test.step('Precondition: term', async () => {
      const term = await Request.post(Endpoints.terms, { data: GeneratePayload.productAndServices.term() });
      await expect(term).CheckResponse();
      Responses.terms.push(await term.json());
    });

    await test.step('Precondition: POD', async () => {
      const podSettlement = await Request.post(Endpoints.pod, { data: GeneratePayload.pointsOfDelivery.pod_settlement() });
      await expect(podSettlement).CheckResponse();
      Responses.pod.push(await podSettlement.json());
    });

    await test.step('Precondition: product', async () => {
      const product = await Request.post(Endpoints.product, { data: GeneratePayload.productAndServices.product() });
      await expect(product).CheckResponse();
      Responses.product.push(await product.json());
    });

    await test.step('Precondition: product contract', async () => {
      const contract = await Request.post(Endpoints.productContract, {
        data: await GeneratePayload.contractsAndOrders.product_contract(),
      });
      await expect(contract).CheckResponse();
      Responses.productContract.push(await contract.json());
    });

    await test.step('Precondition: activate POD', async () => {
      const podActivation = await Request.post('/contract-pods/manual', {
        data: await GeneratePayload.pointsOfDelivery.pod_activation(),
      });
      await expect(podActivation).CheckResponse();
    });

    await test.step('Precondition: resolve POD identifier (create response may omit identifier)', async () => {
      const podGet = await Request.get(`${Endpoints.pod}/${Responses.pod[0].id}`);
      await expect(podGet).CheckResponse();
      const body = await podGet.json();
      resolvedPodIdentifier = String(body.identifier);
      expect(resolvedPodIdentifier.length).toBeGreaterThan(0);
    });

    await test.step('Precondition: manual billing run', async () => {
      const payload = await GeneratePayload.billing.manualInvoice();
      const createManualInvoice = await Request.post(Endpoints.billingRun, { data: payload });
      await expect(createManualInvoice).CheckResponse();
      Responses.billingRun.push(await createManualInvoice.json());
    });

    await test.step('Precondition: wait for invoice generation', async () => {
      await GeneratePayload.billing.waitForInvoiceGeneration();
    });

    await test.step('Precondition: reminder for disconnection + offset time + wait EXECUTED', async () => {
      const reminderPayload = GeneratePayload.receivablesManagement.reminderForDisconnection();
      const reminderRes = await Request.post(Endpoints.reminderForDisconnection, { data: reminderPayload });
      await expect(reminderRes).CheckResponse();
      Responses.reminderForDisconnection.push(await reminderRes.json());
      await GeneratePayload.receivablesManagement.offsetReminderForDisconnectionTime();
    });

    const listQuery: CustomersForDPSQuery = {
      page: 0,
      size: 50,
      conditionType: 'LIST_OF_CUSTOMERS',
      listOfCustomer: Responses.customer[0].identifier as string,
      powerSupplyDisconnectionReminderId: reminderIdFromResponses(Responses),
      gridOperatorId: envVariables.grid_operator,
    };

    let podRowForExecutedRequest: Record<string, unknown>;

    await test.step('Precondition: resolve full POD grid row for executed request payload', async () => {
      podRowForExecutedRequest = await fetchPodRowFromLoadCustomersTab(Request, listQuery, resolvedPodIdentifier);
    });

    await test.step('Action: create Request-C as DRAFT (LIST_OF_CUSTOMERS, allSelected, empty pods)', async () => {
      requestCDraftId = await createRequestCDraftListCustomers(Request, GeneratePayload, Responses, Endpoints);
      Responses.requestForDisconnection.push(requestCDraftId);
    });

    await test.step('Action: create Request-A as EXECUTED (single POD only)', async () => {
      requestAExecutedId = await createRequestAExecutedWithPod(
        Request,
        GeneratePayload,
        Responses,
        Endpoints,
        podRowForExecutedRequest,
      );
      Responses.requestForDisconnection.push(requestAExecutedId);
    });

    await test.step('Post-process: charge fee for executed Request-A', async () => {
      const calc = await Request.post(`disconnection-of-power-supply-requests/calculate-tax/${requestAExecutedId}`);
      await expect(calc).CheckResponse();
    });

    await test.step('Optional: load customers tab — capture response (LIST_OF_CUSTOMERS may be empty post-scenario)', async () => {
      const loadCustomers = await Request.get('disconnection-of-power-supply-requests/load-customer-for-disconnection-power-supply', {
        params: listQuery as Record<string, string | number>,
      });
      await expect(loadCustomers).CheckResponse();
      const body = await loadCustomers.json();
      test.info().attach('[PDT-2861] load-customer-for-disconnection-power-supply', {
        body: JSON.stringify(body, null, 2),
        contentType: 'application/json',
      });
    });

    await test.step('Assert: view-pod-tab for Request-C does not list target POD identifier', async () => {
      const pageBody = await loadViewPodTab(Request, requestCDraftId, listQuery);
      const podRow = findPodInViewPodTabContent(pageBody, resolvedPodIdentifier);
      expect(podRow).toBeFalsy();
    });

    test.info().attach('[PDT-2861] response', {
      body: JSON.stringify(
        {
          ...reportGenerator.setLinksToResponses(Responses),
          requestCDraftId,
          requestAExecutedId,
        },
        null,
        2,
      ),
      contentType: 'application/json',
    });
  });
});
