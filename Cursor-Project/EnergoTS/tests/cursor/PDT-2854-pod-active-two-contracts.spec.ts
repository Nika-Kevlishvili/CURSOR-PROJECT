import { test, expect } from '../../fixtures/baseFixture';
import reportGenerator from '../../utils/generateReport';
import {
  attachPortalLinks,
  assertPodActiveOnContract,
  assertPodNotActiveOnContract,
  buildEditProductContractPayload,
  buildManualActivationPayload,
  isoToday,
  loadContractSnapshot,
  portalLinksForPdt2854,
  postManualActivation,
  productContractStatusUpdate,
  runSupplyAutomaticActivationMassImport,
} from './pdt-2854-pod-active-two-contracts.fixtures';

test.describe('[PDT-2854]: POD active in two contracts', { tag: '@pointOfDelivery' }, () => {
  test('[PDT-2854] TC-BE-1: manual on contract A then mass activation must not activate POD on contract B', async ({
    Request,
    GeneratePayload,
    Responses,
    Endpoints,
    FileUploadRequest,
  }) => {
    test.setTimeout(25 * 60 * 1000);

    const CONTRACT_A = 0;
    const CONTRACT_B = 1;
    const activationDate = isoToday();
    let podIdentifier = '';

    try {
      await test.step('Precondition: term', async () => {
        const term = await Request.post(Endpoints.terms, { data: GeneratePayload.productAndServices.term() });
        await expect(term).CheckResponse();
        Responses.terms.push(await term.json());
      });

      await test.step('Precondition: POD', async () => {
        const pod = await Request.post(Endpoints.pod, { data: GeneratePayload.pointsOfDelivery.pod_settlement() });
        await expect(pod).CheckResponse();
        Responses.pod.push(await pod.json());
        const podGet = await Request.get(`pod/${Responses.pod[0].id}`);
        await expect(podGet).CheckResponse();
        podIdentifier = (await podGet.json()).identifier;
      });

      await test.step('Precondition: product', async () => {
        const product = await Request.post(Endpoints.product, {
          data: GeneratePayload.productAndServices.product(),
        });
        await expect(product).CheckResponse();
        Responses.product.push(await product.json());
      });

      await test.step('Precondition: customer', async () => {
        const customer = await Request.post(Endpoints.customer, {
          data: GeneratePayload.customers.customer_legal(),
        });
        await expect(customer).CheckResponse();
        Responses.customer.push(await customer.json());
      });

      let contractPayloadA: Awaited<ReturnType<typeof GeneratePayload.contractsAndOrders.product_contract>>;

      await test.step('Precondition: product contract A', async () => {
        contractPayloadA = await GeneratePayload.contractsAndOrders.product_contract();
        contractPayloadA.basicParameters.status = 'DRAFT';
        contractPayloadA.basicParameters.subStatus = 'DRAFT';
        contractPayloadA.basicParameters.signingDate = null;
        const res = await Request.post(Endpoints.productContract, { data: contractPayloadA });
        await expect(res).CheckResponse();
        Responses.productContract.push(await res.json());
      });

      await test.step('Contract A: READY → SIGNED', async () => {
        await GeneratePayload.contractsAndOrders.contractStatusChange('READY', 'READY', 'SIGNED', 1);
        await GeneratePayload.contractsAndOrders.contractStatusChange(
          'SIGNED',
          'SIGNED_BY_BOTH_SIDES',
          'SIGNED',
          1,
        );
      });

      await test.step('Contract A: edit (attach POD)', async () => {
        const payload = await buildEditProductContractPayload(
          Request,
          Responses.productContract[CONTRACT_A].id,
          contractPayloadA,
        );
        const put = await Request.put(
          `${Endpoints.productContract}/${Responses.productContract[CONTRACT_A].id}?versionId=1&changeFutureVersionsPods=false`,
          { data: payload },
        );
        await expect(put).CheckResponse();
      });

      await test.step('Contract A: manual activate POD', async () => {
        const activation = await Request.post('/contract-pods/manual', {
          data: await GeneratePayload.pointsOfDelivery.pod_activation(0, activationDate, undefined, CONTRACT_A),
        });
        await expect(activation).CheckResponse();
      });

      let contractPayloadB: Awaited<ReturnType<typeof GeneratePayload.contractsAndOrders.product_contract>>;

      await test.step('Precondition: product contract B (same POD)', async () => {
        contractPayloadB = await GeneratePayload.contractsAndOrders.product_contract();
        contractPayloadB.basicParameters.status = 'DRAFT';
        contractPayloadB.basicParameters.subStatus = 'DRAFT';
        contractPayloadB.basicParameters.signingDate = null;
        const res = await Request.post(Endpoints.productContract, { data: contractPayloadB });
        await expect(res).CheckResponse();
        Responses.productContract.push(await res.json());
      });

      await test.step('Contract B: READY → SIGNED', async () => {
        const contractBId = Responses.productContract[CONTRACT_B].id;
        const ready = await productContractStatusUpdate(Request, contractBId, 'READY', 'READY');
        await expect(ready).CheckResponse();
        const signed = await productContractStatusUpdate(
          Request,
          contractBId,
          'SIGNED',
          'SIGNED_BY_BOTH_SIDES',
        );
        await expect(signed).CheckResponse();
      });

      await test.step('Contract B: edit (attach same POD)', async () => {
        const payloadBEdit = await buildEditProductContractPayload(
          Request,
          Responses.productContract[CONTRACT_B].id,
          contractPayloadB,
        );
        const put = await Request.put(
          `${Endpoints.productContract}/${Responses.productContract[CONTRACT_B].id}?versionId=1&changeFutureVersionsPods=false`,
          { data: payloadBEdit },
        );
        await expect(put).CheckResponse();
      });

      const contractAId = Responses.productContract[CONTRACT_A].id;
      const contractBId = Responses.productContract[CONTRACT_B].id;

      await test.step('Attach portal links for created contracts', async () => {
        await attachPortalLinks(
          portalLinksForPdt2854({
            contractAId,
            contractBId,
            podIdentifier,
            podId: Responses.pod[0].id,
          }),
        );
      });

      await test.step('Assert: POD active on contract A only after manual activation', async () => {
        await assertPodActiveOnContract(
          Request,
          contractAId,
          podIdentifier,
          'After manual — contract A',
        );
        await assertPodNotActiveOnContract(
          Request,
          contractBId,
          podIdentifier,
          'After manual — contract B',
        );
      });

      await test.step('Step: mass activation import for POD (must not activate contract B)', async () => {
        const mi = await runSupplyAutomaticActivationMassImport(
          Request,
          FileUploadRequest,
          Responses,
          podIdentifier,
          activationDate,
        );
        console.log('[PDT-2854] Mass import result:', JSON.stringify(mi.result));
        if (mi.result.success) {
          console.warn(
            '[PDT-2854] Mass import completed without row error report — contract B must still stay inactive',
          );
        } else {
          console.log('[PDT-2854] Mass import reported failure (acceptable):', mi.result.message);
        }

        await assertPodActiveOnContract(
          Request,
          contractAId,
          podIdentifier,
          'After mass import — contract A still active',
        );
        await assertPodNotActiveOnContract(
          Request,
          contractBId,
          podIdentifier,
          'After mass import — contract B (E.R.)',
        );
      });
    } finally {
      reportGenerator.setLinksToResponses(Responses);
    }
  });

  test('[PDT-2854] TC-BE-2: first contract TERMINATED with POD still active — activation on second contract must be rejected', async ({
    Request,
    GeneratePayload,
    Responses,
    Endpoints,
  }) => {
    test.setTimeout(25 * 60 * 1000);

    const CONTRACT_A = 0;
    const CONTRACT_B = 1;
    const activationDate = isoToday();
    let podIdentifier = '';

    try {
      await test.step('Precondition: term', async () => {
        const term = await Request.post(Endpoints.terms, { data: GeneratePayload.productAndServices.term() });
        await expect(term).CheckResponse();
        Responses.terms.push(await term.json());
      });

      await test.step('Precondition: POD', async () => {
        const pod = await Request.post(Endpoints.pod, { data: GeneratePayload.pointsOfDelivery.pod_settlement() });
        await expect(pod).CheckResponse();
        Responses.pod.push(await pod.json());
        const podGet = await Request.get(`pod/${Responses.pod[0].id}`);
        await expect(podGet).CheckResponse();
        podIdentifier = (await podGet.json()).identifier;
      });

      await test.step('Precondition: product', async () => {
        const product = await Request.post(Endpoints.product, {
          data: GeneratePayload.productAndServices.product(),
        });
        await expect(product).CheckResponse();
        Responses.product.push(await product.json());
      });

      await test.step('Precondition: customer', async () => {
        const customer = await Request.post(Endpoints.customer, {
          data: GeneratePayload.customers.customer_legal(),
        });
        await expect(customer).CheckResponse();
        Responses.customer.push(await customer.json());
      });

      let contractPayloadA: Awaited<ReturnType<typeof GeneratePayload.contractsAndOrders.product_contract>>;

      await test.step('Precondition: product contract A', async () => {
        contractPayloadA = await GeneratePayload.contractsAndOrders.product_contract();
        contractPayloadA.basicParameters.status = 'DRAFT';
        contractPayloadA.basicParameters.subStatus = 'DRAFT';
        contractPayloadA.basicParameters.signingDate = null;
        const res = await Request.post(Endpoints.productContract, { data: contractPayloadA });
        await expect(res).CheckResponse();
        Responses.productContract.push(await res.json());
      });

      await test.step('Contract A: READY → SIGNED', async () => {
        await GeneratePayload.contractsAndOrders.contractStatusChange('READY', 'READY', 'SIGNED', 1);
        await GeneratePayload.contractsAndOrders.contractStatusChange(
          'SIGNED',
          'SIGNED_BY_BOTH_SIDES',
          'SIGNED',
          1,
        );
      });

      await test.step('Contract A: edit (attach POD)', async () => {
        const payload = await buildEditProductContractPayload(
          Request,
          Responses.productContract[CONTRACT_A].id,
          contractPayloadA,
        );
        const put = await Request.put(
          `${Endpoints.productContract}/${Responses.productContract[CONTRACT_A].id}?versionId=1&changeFutureVersionsPods=false`,
          { data: payload },
        );
        await expect(put).CheckResponse();
      });

      await test.step('Contract A: activate POD', async () => {
        const activation = await Request.post('/contract-pods/manual', {
          data: await GeneratePayload.pointsOfDelivery.pod_activation(0, activationDate, undefined, CONTRACT_A),
        });
        await expect(activation).CheckResponse();
      });

      let contractPayloadB: Awaited<ReturnType<typeof GeneratePayload.contractsAndOrders.product_contract>>;

      await test.step('Precondition: product contract B (same POD)', async () => {
        contractPayloadB = await GeneratePayload.contractsAndOrders.product_contract();
        contractPayloadB.basicParameters.status = 'DRAFT';
        contractPayloadB.basicParameters.subStatus = 'DRAFT';
        contractPayloadB.basicParameters.signingDate = null;
        const res = await Request.post(Endpoints.productContract, { data: contractPayloadB });
        await expect(res).CheckResponse();
        Responses.productContract.push(await res.json());
      });

      await test.step('Contract B: READY → SIGNED', async () => {
        const contractBId = Responses.productContract[CONTRACT_B].id;
        const ready = await productContractStatusUpdate(Request, contractBId, 'READY', 'READY');
        await expect(ready).CheckResponse();
        const signed = await productContractStatusUpdate(
          Request,
          contractBId,
          'SIGNED',
          'SIGNED_BY_BOTH_SIDES',
        );
        await expect(signed).CheckResponse();
      });

      await test.step('Contract B: edit (attach same POD)', async () => {
        const payloadBEdit = await buildEditProductContractPayload(
          Request,
          Responses.productContract[CONTRACT_B].id,
          contractPayloadB,
        );
        const put = await Request.put(
          `${Endpoints.productContract}/${Responses.productContract[CONTRACT_B].id}?versionId=1&changeFutureVersionsPods=false`,
          { data: payloadBEdit },
        );
        await expect(put).CheckResponse();
      });

      const contractAId = Responses.productContract[CONTRACT_A].id;
      const contractBId = Responses.productContract[CONTRACT_B].id;

      await test.step('Attach portal links for created contracts', async () => {
        await attachPortalLinks(
          portalLinksForPdt2854({
            contractAId,
            contractBId,
            podIdentifier,
            podId: Responses.pod[0].id,
          }),
        );
      });

      await test.step('Contract A: set TERMINATED (POD row left active — prod-style inconsistency)', async () => {
        const terminated = await productContractStatusUpdate(
          Request,
          contractAId,
          'TERMINATED',
          'BY_MUTUAL_AGREEMENT',
        );
        await expect(terminated).CheckResponse();
      });

      await test.step('Assert: contract A TERMINATED and POD still active on A', async () => {
        const snapA = await loadContractSnapshot(Request, contractAId, podIdentifier);
        expect(snapA.contractStatus, 'contract A status').toBe('TERMINATED');
        await assertPodActiveOnContract(
          Request,
          contractAId,
          podIdentifier,
          'Terminated contract A — POD row',
        );
        await assertPodNotActiveOnContract(
          Request,
          contractBId,
          podIdentifier,
          'Contract B before activation attempt',
        );
      });

      await test.step('Step: manual activation on contract B must be rejected', async () => {
        const snapB = await loadContractSnapshot(Request, contractBId, podIdentifier);
        const payload = buildManualActivationPayload(snapB, activationDate);
        const res = await postManualActivation(Request, payload);
        const body = await res.text();
        console.log(`[PDT-2854] Manual on B status=${res.status()} body=${body.slice(0, 500)}`);
        expect(
          res.ok(),
          `Manual activation on contract B must fail while POD active on terminated contract A; status=${res.status()}`,
        ).toBeFalsy();
        await assertPodNotActiveOnContract(
          Request,
          contractBId,
          podIdentifier,
          'After manual attempt — contract B (E.R.)',
        );
      });
    } finally {
      reportGenerator.setLinksToResponses(Responses);
    }
  });
});
