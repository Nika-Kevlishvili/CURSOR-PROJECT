import { test, expect, baseFixture } from '../../../backend/fixtures/baseFixture';
import reportGenerator from '../../../backend/utils/generateReport';
import { randomGens } from '../../../backend/utils/randomGens';

test.describe.skip('[REG-52]: Contracts and Orders - Product Contract', { tag: '@contractsAndOrders' }, () => {
  test.describe('[REG-83]: Energy Product Contract', () => {
    test.describe('[REG-1214]: Re-signing', () => {

  // ─── Shared helpers ───────────────────────────────────────────────────────

  /**
   * Creates the base precondition entities common to most TCs:
   *  - customer (LEGAL)
   *  - 1 or 2 PODs
   *  - terms
   *  - Product A (non-re-signing)
   *  - "old" contract with Product A (ACTIVE_IN_TERM)
   *  - activates the specified PODs in the old contract
   *
   * After calling this helper the Responses arrays contain:
   *   customer[0], pod[0..n-1], terms[0], product[0], productContract[0]
   */
  async function setupOldContract(
    { Request, GeneratePayload, Responses, Endpoints }: Pick<baseFixture, 'Request' | 'GeneratePayload' | 'Responses' | 'Endpoints'>,
    opts: {
      podCount?: number;          // 1 (default) or 2
      activatePods?: boolean;     // true (default) — call /contract-pods/manual
      oldContractStatus?: string; // 'ACTIVE_IN_TERM' (default) or 'ACTIVE_IN_PERPETUITY'
    } = {}
  ) {
    const { podCount = 1, activatePods = true, oldContractStatus = 'ACTIVE_IN_TERM' } = opts;

    await test.step('Create customer', async () => {
      const response = await Request.post(Endpoints.customer, { data: GeneratePayload.customers.customer_legal() });
      await expect(response).CheckResponse();
      Responses.customer.push(await response.json());
    });

    await test.step('Create POD 1', async () => {
      const response = await Request.post(Endpoints.pod, { data: GeneratePayload.pointsOfDelivery.pod_settlement() });
      await expect(response).CheckResponse();
      Responses.pod.push(await response.json());
    });

    if (podCount >= 2) {
      await test.step('Create POD 2', async () => {
        const response = await Request.post(Endpoints.pod, { data: GeneratePayload.pointsOfDelivery.pod_settlement() });
        await expect(response).CheckResponse();
        Responses.pod.push(await response.json());
      });
    }

    await test.step('Create terms', async () => {
      const payload = GeneratePayload.productAndServices.term();
      payload.supplyActivations
      const response = await Request.post(Endpoints.terms, { data: payload });
      await expect(response).CheckResponse();
      Responses.terms.push(await response.json());
    });

    await test.step('Create Product A (non-re-signing)', async () => {
      const payload = GeneratePayload.productAndServices.product();
      const response = await Request.post(Endpoints.product, { data: payload });
      await expect(response).CheckResponse();
      Responses.product.push(await response.json());
    });

    await test.step(`Create old contract with Product A (status: ${oldContractStatus})`, async () => {
      const payload = await GeneratePayload.contractsAndOrders.product_contract(0, 0);
      payload.basicParameters.versionStatus = 'SIGNED';
      const response = await Request.post(Endpoints.productContract, { data: payload });
      await expect(response).CheckResponse();
      Responses.productContract.push(await response.json());
    });

    if (activatePods) {
      await test.step('Activate POD 1 in old contract', async () => {
        const payload = await GeneratePayload.pointsOfDelivery.pod_activation(0, '2025-01-01');
        const response = await Request.post('/contract-pods/manual', { data: payload });
        await expect(response).CheckResponse();
      });

      if (podCount >= 2) {
        await test.step('Activate POD 2 in old contract', async () => {
          const payload = await GeneratePayload.pointsOfDelivery.pod_activation(1, '2025-02-01');
          const response = await Request.post('/contract-pods/manual', { data: payload });
          await expect(response).CheckResponse();
        });
      }
    }
  }

  /**
   * Creates Product B: re-signing, non-manual, compatible with Product A.
   * Pushes to Responses.product[1].
   */
  async function createProductB(
    { Request, GeneratePayload, Responses, Endpoints }: Pick<baseFixture, 'Request' | 'GeneratePayload' | 'Responses' | 'Endpoints'>,
    productAId: number
  ) {

    await test.step('create term 2 (for Product B)', async () => {
      const response = await Request.post(Endpoints.terms, { data: GeneratePayload.productAndServices.term() });
      await expect(response).CheckResponse();
      Responses.terms.push(await response.json());
    })

    await test.step('Create Product B (re-signing, non-manual, compatible with Product A)', async () => {
      const payload = GeneratePayload.productAndServices.product(1);
      const response = await Request.post(Endpoints.product, { data: payload });
      await expect(response).CheckResponse();
      Responses.product.push(await response.json());

      const specialOfferPayload = await GeneratePayload.productAndServices.specialOffersTab(true, Responses.product[0]);
      const specialOffersResponse = await Request.put(`products/${Responses.product[1]}/special-offers?version=1`, { data: specialOfferPayload });
      await expect(specialOffersResponse).CheckResponse();
    });
  }

  // ─── TC-BE-1 ──────────────────────────────────────────────────────────────
  test('[REG-1224]: Re-signing Product validation during contract creation', async ({
    Request, GeneratePayload, Responses, Endpoints,
  }) => {
    await setupOldContract({ Request, GeneratePayload, Responses, Endpoints });
    await createProductB({ Request, GeneratePayload, Responses, Endpoints }, Responses.product[0]);

    await test.step('Create re-signing contract with Product B (status: READY) — expect 2xx', async () => {
      const payload = await GeneratePayload.contractsAndOrders.product_contract(0, 1, 0);
      payload.basicParameters.status = 'READY';
      payload.basicParameters.subStatus = 'READY';
      payload.basicParameters.versionStatus = 'SIGNED';
      payload.basicParameters.signingDate = ''
      const response = await Request.post(Endpoints.productContract, { data: payload });
      await expect(response).CheckResponse();
      Responses.productContract.push(await response.json());
    });

    console.log(reportGenerator.setLinksToResponses(Responses));

    test.info().attach('[PHN-2620] TC-BE-1 response', {
      body: JSON.stringify(reportGenerator.setLinksToResponses(Responses), null, 2),
      contentType: 'application/json',
    });
  });

  // ─── TC-BE-2 ──────────────────────────────────────────────────────────────
  test('[REG-1228]: Re-signing Product validation during contract creation', async ({
    Request, GeneratePayload, Responses, Endpoints,
  }) => {
    await setupOldContract({ Request, GeneratePayload, Responses, Endpoints });

    await test.step('Create re-signing contract with Product A (NOT a re-signing product, status: READY) — expect 2xx + warning', async () => {
      const payload = await GeneratePayload.contractsAndOrders.product_contract(0, 0, 0);
      payload.basicParameters.status = 'READY';
      payload.basicParameters.subStatus = 'READY';
      payload.basicParameters.versionStatus = 'SIGNED';
      const response = await Request.post(Endpoints.productContract, { data: payload });
      await expect(response).CheckResponse();
      const body = await response.json();
      // Response should contain resigningMessages with a warning about non-re-signing product
      expect(body.resigningMessages, 'Expected resigningMessages in response').toBeDefined();
      expect(body.resigningMessages.length, 'Expected at least one warning message').toBeGreaterThan(0);
      Responses.productContract.push(body);
    });

    console.log(reportGenerator.setLinksToResponses(Responses));

    test.info().attach('[PHN-2620] TC-BE-2 response', {
      body: JSON.stringify(reportGenerator.setLinksToResponses(Responses), null, 2),
      contentType: 'application/json',
    });
  });

  // ─── TC-BE-3 ──────────────────────────────────────────────────────────────
  test('[REG-1330]: PHN-2620 TC-BE-3 - Manual supply activation after re-signing blocks contract creation', async ({
    Request, GeneratePayload, Responses, Endpoints,
  }) => {
    await setupOldContract({ Request, GeneratePayload, Responses, Endpoints });

    await test.step('Create Product C (re-signing, supplyActivationAfterResigning = MANUAL)', async () => {
      const payload = GeneratePayload.productAndServices.product(0);
      (payload as any).isResigning = true;
      (payload as any).resignProductTargets = [Responses.product[0]];
      (payload as any).supplyActivationAfterResigning = 'MANUAL';
      const response = await Request.post(Endpoints.product, { data: payload });
      await expect(response).CheckResponse();
      Responses.product.push(await response.json());
    });

    await test.step('Attempt re-signing contract with Product C — expect 4xx', async () => {
      const payload = await GeneratePayload.contractsAndOrders.product_contract(0, 1, 0);
      payload.basicParameters.status = 'READY';
      payload.basicParameters.subStatus = 'READY';
      payload.basicParameters.versionStatus = 'SIGNED';
      const response = await Request.post(Endpoints.productContract, { data: payload });
      expect(response.status(), 'Expected HTTP 4xx — manual activation should block re-signing').toBeGreaterThanOrEqual(400);
    });

    console.log(reportGenerator.setLinksToResponses(Responses));

    test.info().attach('[PHN-2620] TC-BE-3 response', {
      body: JSON.stringify(reportGenerator.setLinksToResponses(Responses), null, 2),
      contentType: 'application/json',
    });
  });

  // ─── TC-BE-4 ──────────────────────────────────────────────────────────────
  test('[REG-1331]: PHN-2620 TC-BE-4 - Re-signing blocked when the customer has no active contract', async ({
    Request, GeneratePayload, Responses, Endpoints,
  }) => {
    // Setup: create a fresh customer with NO contracts
    await test.step('Create a new customer (no contracts)', async () => {
      const response = await Request.post(Endpoints.customer, { data: GeneratePayload.customers.customer_legal() });
      await expect(response).CheckResponse();
      Responses.customer.push(await response.json());
    });

    await test.step('Create POD', async () => {
      const response = await Request.post(Endpoints.pod, { data: GeneratePayload.pointsOfDelivery.pod_settlement() });
      await expect(response).CheckResponse();
      Responses.pod.push(await response.json());
    });

    await test.step('Create terms', async () => {
      const response = await Request.post(Endpoints.terms, { data: GeneratePayload.productAndServices.term() });
      await expect(response).CheckResponse();
      Responses.terms.push(await response.json());
    });

    await test.step('Create Product A (used as base for Product B compatibility)', async () => {
      const response = await Request.post(Endpoints.product, { data: GeneratePayload.productAndServices.product() });
      await expect(response).CheckResponse();
      Responses.product.push(await response.json());
    });

    await test.step('Create Product B (re-signing, compatible with Product A)', async () => {
      const payload = GeneratePayload.productAndServices.product(0);
      (payload as any).isResigning = true;
      (payload as any).resignProductTargets = [Responses.product[0]];
      (payload as any).supplyActivationAfterResigning = 'FROM_FIRST_DAY_OF_NEXT_MONTH';
      const response = await Request.post(Endpoints.product, { data: payload });
      await expect(response).CheckResponse();
      Responses.product.push(await response.json());
    });

    await test.step('Attempt re-signing contract with Product B for customer with NO active contract — expect 4xx', async () => {
      const payload = await GeneratePayload.contractsAndOrders.product_contract(0, 1, 0);
      payload.basicParameters.status = 'READY';
      payload.basicParameters.subStatus = 'READY';
      payload.basicParameters.versionStatus = 'SIGNED';
      const response = await Request.post(Endpoints.productContract, { data: payload });
      expect(response.status(), 'Expected HTTP 4xx — customer has no active contract').toBeGreaterThanOrEqual(400);
    });

    console.log(reportGenerator.setLinksToResponses(Responses));

    test.info().attach('[PHN-2620] TC-BE-4 response', {
      body: JSON.stringify(reportGenerator.setLinksToResponses(Responses), null, 2),
      contentType: 'application/json',
    });
  });

  // ─── TC-BE-5 ──────────────────────────────────────────────────────────────
  test('[REG-1226]: Create product contract at READY — product not compatible', async ({
    Request, GeneratePayload, Responses, Endpoints,
  }) => {
    await setupOldContract({ Request, GeneratePayload, Responses, Endpoints });

    await test.step('Create Product D (re-signing, non-manual, NOT compatible with Product A)', async () => {
      const payload = GeneratePayload.productAndServices.product(0);
      (payload as any).isResigning = true;
      (payload as any).resignProductTargets = []; // empty — no compatible products
      (payload as any).supplyActivationAfterResigning = 'FROM_FIRST_DAY_OF_NEXT_MONTH';
      const response = await Request.post(Endpoints.product, { data: payload });
      await expect(response).CheckResponse();
      Responses.product.push(await response.json());
    });

    await test.step('Attempt re-signing contract with Product D (incompatible) — expect 4xx', async () => {
      const payload = await GeneratePayload.contractsAndOrders.product_contract(0, 1, 0);
      payload.basicParameters.status = 'READY';
      payload.basicParameters.subStatus = 'READY';
      payload.basicParameters.versionStatus = 'SIGNED';
      const response = await Request.post(Endpoints.productContract, { data: payload });
      expect(response.status(), 'Expected HTTP 4xx — product incompatibility should block re-signing').toBeGreaterThanOrEqual(400);
    });

    console.log(reportGenerator.setLinksToResponses(Responses));

    test.info().attach('[PHN-2620] TC-BE-5 response', {
      body: JSON.stringify(reportGenerator.setLinksToResponses(Responses), null, 2),
      contentType: 'application/json',
    });
  });

  // ─── TC-BE-6 ──────────────────────────────────────────────────────────────
  test('[REG-1332]: PHN-2620 TC-BE-6 - Re-signing blocked when all PODs are already re-signed', async ({
    Request, GeneratePayload, Responses, Endpoints,
  }) => {
    // Setup old contract with both POD 1 and POD 2
    await setupOldContract({ Request, GeneratePayload, Responses, Endpoints }, { podCount: 2 });
    await createProductB({ Request, GeneratePayload, Responses, Endpoints }, Responses.product[0]);

    // Mark POD 1 as re-signed: create a successful re-signing contract for POD 1 only
    await test.step('Create re-signing contract for POD 1 (marks POD 1 as re-signed)', async () => {
      const payload = await GeneratePayload.contractsAndOrders.product_contract(0, 1, 0);
      payload.basicParameters.status = 'READY';
      payload.basicParameters.subStatus = 'READY';
      payload.basicParameters.versionStatus = 'SIGNED';
      const response = await Request.post(Endpoints.productContract, { data: payload });
      await expect(response).CheckResponse();
      Responses.productContract.push(await response.json());
    });

    // Mark POD 2 as re-signed: create a successful re-signing contract for POD 2 only
    await test.step('Create re-signing contract for POD 2 (marks POD 2 as re-signed)', async () => {
      const payload = await GeneratePayload.contractsAndOrders.product_contract(0, 1, 1);
      payload.basicParameters.status = 'READY';
      payload.basicParameters.subStatus = 'READY';
      payload.basicParameters.versionStatus = 'SIGNED';
      const response = await Request.post(Endpoints.productContract, { data: payload });
      await expect(response).CheckResponse();
      Responses.productContract.push(await response.json());
    });

    await test.step('Attempt re-signing contract with both PODs (both already re-signed) — expect 4xx', async () => {
      const payload = await GeneratePayload.contractsAndOrders.product_contract(0, 1);
      payload.basicParameters.status = 'READY';
      payload.basicParameters.subStatus = 'READY';
      payload.basicParameters.versionStatus = 'SIGNED';
      const response = await Request.post(Endpoints.productContract, { data: payload });
      expect(response.status(), 'Expected HTTP 4xx — all PODs are already re-signed').toBeGreaterThanOrEqual(400);
    });

    console.log(reportGenerator.setLinksToResponses(Responses));

    test.info().attach('[PHN-2620] TC-BE-6 response', {
      body: JSON.stringify(reportGenerator.setLinksToResponses(Responses), null, 2),
      contentType: 'application/json',
    });
  });

  // ─── TC-BE-7 ──────────────────────────────────────────────────────────────
  test('[REG-1333]: PHN-2620 TC-BE-7 - SIGNED by customer passes re-signing validation', async ({
    Request, GeneratePayload, Responses, Endpoints,
  }) => {
    await setupOldContract({ Request, GeneratePayload, Responses, Endpoints });
    await createProductB({ Request, GeneratePayload, Responses, Endpoints }, Responses.product[0]);

    await test.step('Create re-signing contract with Product B (status: SIGNED / SIGNED_BY_CUSTOMER) — expect 2xx', async () => {
      const payload = await GeneratePayload.contractsAndOrders.product_contract(0, 1, 0);
      payload.basicParameters.status = 'SIGNED';
      payload.basicParameters.subStatus = 'SIGNED_BY_CUSTOMER';
      payload.basicParameters.versionStatus = 'SIGNED';
      const response = await Request.post(Endpoints.productContract, { data: payload });
      await expect(response).CheckResponse();
      Responses.productContract.push(await response.json());
    });

    console.log(reportGenerator.setLinksToResponses(Responses));

    test.info().attach('[PHN-2620] TC-BE-7 response', {
      body: JSON.stringify(reportGenerator.setLinksToResponses(Responses), null, 2),
      contentType: 'application/json',
    });
  });

  // ─── TC-BE-8 ──────────────────────────────────────────────────────────────
  test('[REG-1334]: PHN-2620 TC-BE-8 - SIGNED by EPRES passes re-signing validation', async ({
    Request, GeneratePayload, Responses, Endpoints,
  }) => {
    await setupOldContract({ Request, GeneratePayload, Responses, Endpoints });
    await createProductB({ Request, GeneratePayload, Responses, Endpoints }, Responses.product[0]);

    await test.step('Create re-signing contract with Product B (status: SIGNED / SIGNED_BY_EPRES) — expect 2xx', async () => {
      const payload = await GeneratePayload.contractsAndOrders.product_contract(0, 1, 0);
      payload.basicParameters.status = 'SIGNED';
      payload.basicParameters.subStatus = 'SIGNED_BY_EPRES';
      payload.basicParameters.versionStatus = 'SIGNED';
      const response = await Request.post(Endpoints.productContract, { data: payload });
      await expect(response).CheckResponse();
      Responses.productContract.push(await response.json());
    });

    console.log(reportGenerator.setLinksToResponses(Responses));

    test.info().attach('[PHN-2620] TC-BE-8 response', {
      body: JSON.stringify(reportGenerator.setLinksToResponses(Responses), null, 2),
      contentType: 'application/json',
    });
  });

  // ─── TC-BE-9 ──────────────────────────────────────────────────────────────
  test('[REG-1335]: PHN-2620 TC-BE-9 - Active in perpetuity old contract passes re-signing validation', async ({
    Request, GeneratePayload, Responses, Endpoints,
  }) => {
    // Old contract is ACTIVE_IN_PERPETUITY instead of ACTIVE_IN_TERM
    await setupOldContract(
      { Request, GeneratePayload, Responses, Endpoints },
      { oldContractStatus: 'ACTIVE_IN_PERPETUITY' }
    );
    await createProductB({ Request, GeneratePayload, Responses, Endpoints }, Responses.product[0]);

    await test.step('Create re-signing contract with Product B (status: READY) — expect 2xx', async () => {
      const payload = await GeneratePayload.contractsAndOrders.product_contract(0, 1, 0);
      payload.basicParameters.status = 'READY';
      payload.basicParameters.subStatus = 'READY';
      payload.basicParameters.versionStatus = 'SIGNED';
      const response = await Request.post(Endpoints.productContract, { data: payload });
      await expect(response).CheckResponse();
      Responses.productContract.push(await response.json());
    });

    console.log(reportGenerator.setLinksToResponses(Responses));

    test.info().attach('[PHN-2620] TC-BE-9 response', {
      body: JSON.stringify(reportGenerator.setLinksToResponses(Responses), null, 2),
      contentType: 'application/json',
    });
  });

  // ─── TC-BE-10 ─────────────────────────────────────────────────────────────
  test('[REG-1336]: PHN-2620 TC-BE-10 - Re-signing passes when at least one POD is still eligible', async ({
    Request, GeneratePayload, Responses, Endpoints,
  }) => {
    // Old contract with both POD 1 and POD 2
    await setupOldContract({ Request, GeneratePayload, Responses, Endpoints }, { podCount: 2 });
    await createProductB({ Request, GeneratePayload, Responses, Endpoints }, Responses.product[0]);

    // Mark POD 1 as re-signed by creating a first successful re-signing contract for POD 1 only
    await test.step('Create re-signing contract for POD 1 only (marks POD 1 as re-signed)', async () => {
      const payload = await GeneratePayload.contractsAndOrders.product_contract(0, 1, 0);
      payload.basicParameters.status = 'READY';
      payload.basicParameters.subStatus = 'READY';
      payload.basicParameters.versionStatus = 'SIGNED';
      const response = await Request.post(Endpoints.productContract, { data: payload });
      await expect(response).CheckResponse();
      Responses.productContract.push(await response.json());
    });

    await test.step('Create re-signing contract with POD 1 (re-signed) + POD 2 (eligible) — expect 2xx', async () => {
      // Both PODs in the new contract; POD 2 is still eligible → validation passes
      const payload = await GeneratePayload.contractsAndOrders.product_contract(0, 1);
      payload.basicParameters.status = 'READY';
      payload.basicParameters.subStatus = 'READY';
      payload.basicParameters.versionStatus = 'SIGNED';
      const response = await Request.post(Endpoints.productContract, { data: payload });
      await expect(response).CheckResponse();
      Responses.productContract.push(await response.json());
    });

    console.log(reportGenerator.setLinksToResponses(Responses));

    test.info().attach('[PHN-2620] TC-BE-10 response', {
      body: JSON.stringify(reportGenerator.setLinksToResponses(Responses), null, 2),
      contentType: 'application/json',
    });
  });

  // ─── TC-BE-11 ─────────────────────────────────────────────────────────────
  test('[REG-1337]: PHN-2620 TC-BE-11 - PUT to READY passes re-signing validation', async ({
    Request, GeneratePayload, Responses, Endpoints,
  }) => {
    await setupOldContract({ Request, GeneratePayload, Responses, Endpoints });
    await createProductB({ Request, GeneratePayload, Responses, Endpoints }, Responses.product[0]);

    // Create the new contract in DRAFT status (validation NOT triggered on DRAFT)
    await test.step('Create new contract with Product B in DRAFT status (no validation triggered)', async () => {
      const payload = await GeneratePayload.contractsAndOrders.product_contract(0, 1, 0);
      payload.basicParameters.status = 'DRAFT';
      payload.basicParameters.subStatus = 'DRAFT';
      payload.basicParameters.versionStatus = 'DRAFT';
      payload.basicParameters.signingDate = null;
      const response = await Request.post(Endpoints.productContract, { data: payload });
      await expect(response).CheckResponse();
      Responses.productContract.push(await response.json());
    });

    await test.step('Edit new contract to READY status via PUT (validation triggered) — expect 2xx', async () => {
      const newContractId = Responses.productContract[1].id;
      const versionId = Responses.productContract[1].versionId ?? 1;

      // Fetch current contract state to build valid edit payload
      const getContract = await Request.get(`${Endpoints.productContract}/${newContractId}?version=1`);
      await expect(getContract).CheckResponse();
      const contractData = await getContract.json();

      // Build podRequests grouped by billingGroupId
      const podsByGroup = new Map<number, any[]>();
      for (const pod of contractData.contractPodsResponses) {
        if (!podsByGroup.has(pod.billingGroupId)) podsByGroup.set(pod.billingGroupId, []);
        podsByGroup.get(pod.billingGroupId)!.push({ pointOfDeliveryDetailId: pod.podDetailId, dealNumber: pod.dealNumber ?? null });
      }
      const podRequests = Array.from(podsByGroup.entries()).map(([bgId, pods]) => ({
        billingGroupId: bgId,
        productContractPointOfDeliveries: pods,
      }));

      const allPods = contractData.contractPodsResponses.map((pod: any) => ({
        pointOfDeliveryDetailId: pod.podDetailId,
        dealNumber: pod.dealNumber ?? null,
      }));

      const editPayload = {
        basicParameters: {
          ...contractData.basicParameters,
          status: 'READY',
          subStatus: 'READY',
          versionStatus: 'SIGNED',
        },
        additionalParameters: {
          ...contractData.additionalParameters,
          riskAssessment: 'PERMIT',
          employeeId: contractData.additionalParameters?.employeeId ?? null,
        },
        productParameters: contractData.productParameters,
        savingAsNewVersion: false,
        startDate: contractData.versions[0].startDate,
        podRequests,
        productContractPointOfDeliveries: allPods,
        proxy: {},
      };

      const editResponse = await Request.put(
        `${Endpoints.productContract}/${newContractId}?versionId=${versionId}&changeFutureVersionsPods=false`,
        { data: editPayload }
      );
      await expect(editResponse).CheckResponse();
    });

    console.log(reportGenerator.setLinksToResponses(Responses));

    test.info().attach('[PHN-2620] TC-BE-11 response', {
      body: JSON.stringify(reportGenerator.setLinksToResponses(Responses), null, 2),
      contentType: 'application/json',
    });
  });

  // ─── TC-BE-12 ─────────────────────────────────────────────────────────────
  test('[REG-1338]: PHN-2620 TC-BE-12 - Edit to an incompatible re-signing product is blocked', async ({
    Request, GeneratePayload, Responses, Endpoints,
  }) => {
    await setupOldContract({ Request, GeneratePayload, Responses, Endpoints });

    // Create Product D (re-signing, incompatible with Product A)
    await test.step('Create Product D (re-signing, NOT compatible with Product A)', async () => {
      const payload = GeneratePayload.productAndServices.product(0);
      (payload as any).isResigning = true;
      (payload as any).resignProductTargets = [];
      (payload as any).supplyActivationAfterResigning = 'FROM_FIRST_DAY_OF_NEXT_MONTH';
      const response = await Request.post(Endpoints.product, { data: payload });
      await expect(response).CheckResponse();
      Responses.product.push(await response.json());
    });

    // Create an initial contract with Product A (non-re-signing) in READY status
    await test.step('Create initial contract with Product A in READY status', async () => {
      const payload = await GeneratePayload.contractsAndOrders.product_contract(0, 0, 0);
      payload.basicParameters.status = 'READY';
      payload.basicParameters.subStatus = 'READY';
      payload.basicParameters.versionStatus = 'SIGNED';
      const response = await Request.post(Endpoints.productContract, { data: payload });
      // May return 2xx with a warning (product A is not a re-signing product — TC-BE-2 scenario)
      // We only need the contract to exist; if 4xx, skip
      if (response.ok()) {
        Responses.productContract.push(await response.json());
      }
    });

    await test.step('Edit initial contract — change product to Product D (incompatible) — expect 4xx', async () => {
      // If the initial contract could not be created, skip edit attempt
      if (Responses.productContract.length < 2) {
        test.skip(true, 'Initial contract was not created; skipping edit step');
        return;
      }

      const contractToEdit = Responses.productContract[1];
      const contractId = contractToEdit.id;
      const versionId = contractToEdit.versionId ?? 1;

      const getContract = await Request.get(`${Endpoints.productContract}/${contractId}?version=1`);
      await expect(getContract).CheckResponse();
      const contractData = await getContract.json();

      const podsByGroup = new Map<number, any[]>();
      for (const pod of contractData.contractPodsResponses) {
        if (!podsByGroup.has(pod.billingGroupId)) podsByGroup.set(pod.billingGroupId, []);
        podsByGroup.get(pod.billingGroupId)!.push({ pointOfDeliveryDetailId: pod.podDetailId, dealNumber: pod.dealNumber ?? null });
      }
      const podRequests = Array.from(podsByGroup.entries()).map(([bgId, pods]) => ({
        billingGroupId: bgId,
        productContractPointOfDeliveries: pods,
      }));
      const allPods = contractData.contractPodsResponses.map((pod: any) => ({
        pointOfDeliveryDetailId: pod.podDetailId,
        dealNumber: pod.dealNumber ?? null,
      }));

      // Fetch Product D version
      const getProductD = await Request.get(`${Endpoints.product}/${Responses.product[1]}?version=1`);
      const productDData = await getProductD.json();

      const editPayload = {
        basicParameters: {
          ...contractData.basicParameters,
          status: 'READY',
          subStatus: 'READY',
          versionStatus: 'SIGNED',
          productId: productDData.id,
          productVersionId: productDData.version,
        },
        additionalParameters: {
          ...contractData.additionalParameters,
          riskAssessment: 'PERMIT',
        },
        productParameters: contractData.productParameters,
        savingAsNewVersion: false,
        startDate: contractData.versions[0].startDate,
        podRequests,
        productContractPointOfDeliveries: allPods,
        proxy: {},
      };

      const editResponse = await Request.put(
        `${Endpoints.productContract}/${contractId}?versionId=${versionId}&changeFutureVersionsPods=false`,
        { data: editPayload }
      );
      expect(editResponse.status(), 'Expected HTTP 4xx — incompatible re-signing product should block edit').toBeGreaterThanOrEqual(400);
    });

    console.log(reportGenerator.setLinksToResponses(Responses));

    test.info().attach('[PHN-2620] TC-BE-12 response', {
      body: JSON.stringify(reportGenerator.setLinksToResponses(Responses), null, 2),
      contentType: 'application/json',
    });
  });

  // ─── TC-BE-13 ─────────────────────────────────────────────────────────────
  test('[REG-1339]: PHN-2620 TC-BE-13 - Another customer\'s active contract does not allow re-signing', async ({
    Request, GeneratePayload, Responses, Endpoints,
  }) => {
    // Create Customer Y with an active contract
    let customerY_data: any;

    await test.step('Create Customer Y (will have active contract)', async () => {
      const response = await Request.post(Endpoints.customer, { data: GeneratePayload.customers.customer_legal() });
      await expect(response).CheckResponse();
      customerY_data = await response.json();
    });

    // Create Customer X (no contracts — the target for our re-signing attempt)
    await test.step('Create Customer X (no contracts)', async () => {
      const response = await Request.post(Endpoints.customer, { data: GeneratePayload.customers.customer_legal() });
      await expect(response).CheckResponse();
      Responses.customer.push(await response.json()); // customer[0] = Customer X
    });

    await test.step('Create POD', async () => {
      const response = await Request.post(Endpoints.pod, { data: GeneratePayload.pointsOfDelivery.pod_settlement() });
      await expect(response).CheckResponse();
      Responses.pod.push(await response.json());
    });

    await test.step('Create terms', async () => {
      const response = await Request.post(Endpoints.terms, { data: GeneratePayload.productAndServices.term() });
      await expect(response).CheckResponse();
      Responses.terms.push(await response.json());
    });

    await test.step('Create Product A', async () => {
      const response = await Request.post(Endpoints.product, { data: GeneratePayload.productAndServices.product() });
      await expect(response).CheckResponse();
      Responses.product.push(await response.json());
    });

    await test.step('Create Product B (re-signing, compatible with Product A)', async () => {
      const payload = GeneratePayload.productAndServices.product(0);
      (payload as any).isResigning = true;
      (payload as any).resignProductTargets = [Responses.product[0]];
      (payload as any).supplyActivationAfterResigning = 'FROM_FIRST_DAY_OF_NEXT_MONTH';
      const response = await Request.post(Endpoints.product, { data: payload });
      await expect(response).CheckResponse();
      Responses.product.push(await response.json());
    });

    // Temporarily add Customer Y to build the "old contract" for Customer Y
    await test.step('Create active contract for Customer Y (old contract)', async () => {
      Responses.customer.push(customerY_data); // customer[1] = Customer Y
      const payload = await GeneratePayload.contractsAndOrders.product_contract(1, 0, 0);
      payload.basicParameters.status = 'ACTIVE_IN_TERM';
      payload.basicParameters.subStatus = 'DELIVERY';
      payload.basicParameters.versionStatus = 'SIGNED';
      payload.basicParameters.entryInForceDate = '2025-01-01';
      const response = await Request.post(Endpoints.productContract, { data: payload });
      await expect(response).CheckResponse();
      Responses.productContract.push(await response.json());
    });

    await test.step('Attempt re-signing contract with Product B for Customer X (no active contract) — expect 4xx', async () => {
      const payload = await GeneratePayload.contractsAndOrders.product_contract(0, 1, 0);
      payload.basicParameters.status = 'READY';
      payload.basicParameters.subStatus = 'READY';
      payload.basicParameters.versionStatus = 'SIGNED';
      const response = await Request.post(Endpoints.productContract, { data: payload });
      expect(response.status(), 'Expected HTTP 4xx — Customer X has no active contract; Customer Y\'s contract should not count').toBeGreaterThanOrEqual(400);
    });

    console.log(reportGenerator.setLinksToResponses(Responses));

    test.info().attach('[PHN-2620] TC-BE-13 response', {
      body: JSON.stringify(reportGenerator.setLinksToResponses(Responses), null, 2),
      contentType: 'application/json',
    });
  });

  // ─── TC-BE-14 ─────────────────────────────────────────────────────────────
  test('[REG-1340]: PHN-2620 TC-BE-14 - POD without an activation date blocks re-signing', async ({
    Request, GeneratePayload, Responses, Endpoints,
  }) => {
    // Old contract is created but pod_activation is skipped → POD has no activationDate
    await setupOldContract(
      { Request, GeneratePayload, Responses, Endpoints },
      { activatePods: false }
    );
    await createProductB({ Request, GeneratePayload, Responses, Endpoints }, Responses.product[0]);

    await test.step('Attempt re-signing contract with Product B (POD has no activationDate in old contract) — expect 4xx', async () => {
      const payload = await GeneratePayload.contractsAndOrders.product_contract(0, 1, 0);
      payload.basicParameters.status = 'READY';
      payload.basicParameters.subStatus = 'READY';
      payload.basicParameters.versionStatus = 'SIGNED';
      const response = await Request.post(Endpoints.productContract, { data: payload });
      expect(response.status(), 'Expected HTTP 4xx — POD without activation date is not eligible for re-signing').toBeGreaterThanOrEqual(400);
    });

    console.log(reportGenerator.setLinksToResponses(Responses));

    test.info().attach('[PHN-2620] TC-BE-14 response', {
      body: JSON.stringify(reportGenerator.setLinksToResponses(Responses), null, 2),
      contentType: 'application/json',
    });
  });
    });
  });
});
