import {test, expect} from '../../../backend/fixtures/baseFixture';
import { randomGens } from '../../../backend/utils/randomGens';
import reportGenerator from '../../../backend/utils/generateReport';

test.describe('[REG-52]: Contracts and Orders - Claimed penalty', {tag: '@contractsAndOrders'}, () => {
  test.describe('[REG-1210]: Claimed penalty', () => {
    test.describe('[REG-1211]: Create Claimed penalty', () => {
      test('[REG-1197]: Create Claimed penalty Reversal ', {tag: '@customer'}, async ({Request, Responses, GeneratePayload, Endpoints}) => {
        await test.step('generate objects', async () => {
          const term = await Request.post(Endpoints.terms, {data: GeneratePayload.productAndServices.term()});
          await expect(term).CheckResponse();
          Responses.terms.push(await term.json())

          const customer = await Request.post(Endpoints.customer, {data: GeneratePayload.customers.customer_private()});
          await expect(customer).CheckResponse();
          Responses.customer.push(await customer.json());

          const penalty = await Request.post(Endpoints.penalty, {data: GeneratePayload.productAndServices.penalty()});
          await expect(penalty).CheckResponse();
          Responses.penalty.push(await penalty.json());

          const termination = await Request.post(Endpoints.termination, {data: GeneratePayload.productAndServices.termination('EXPIRATION_OF_THE_NOTICE')});
          await expect(termination).CheckResponse();
          Responses.termination.push(await termination.json());

          const podSettlement = await Request.post(Endpoints.pod, {data: GeneratePayload.pointsOfDelivery.pod_settlement()});
          await expect(podSettlement).CheckResponse();
          Responses.pod.push(await podSettlement.json());

          const product = await Request.post(Endpoints.product, {data: GeneratePayload.productAndServices.product()});
          await expect(product).CheckResponse();
          Responses.product.push(await product.json())

          const contractpayload = await GeneratePayload.contractsAndOrders.product_contract();
          contractpayload.basicParameters.status = 'ENTERED_INTO_FORCE'
          contractpayload.basicParameters.subStatus = 'AWAITING_ACTIVATION'
          contractpayload.basicParameters.entryInForceDate = randomGens.generateUtcDate('yyyy-mm-dd');
          const contract = await Request.post(Endpoints.productContract, {data: contractpayload});

          await expect(contract).CheckResponse();
          Responses.productContract.push(await contract.json())
        });

        await test.step('Create action', async () => {
          const action = await Request.post(Endpoints.action, {data: await GeneratePayload.contractsAndOrders.action()});
          console.log('action', await action.json())
          Responses.action.push(await action.json());
          await expect(action).CheckResponse();
        });

        await test.step('Validating claimed penalty', async () => {
          const action = await Request.get(`${Endpoints.action}/${Responses.action[0].id}`);
          const actionResponse = await action.json();
          await expect(action).CheckResponse();

          const claimedPenalty = await Request.get(`${Endpoints.claimedPenalty}/${actionResponse.claimedPenalty.id}`);
          const claimedPenaltyResponse = await claimedPenalty.json();
          Responses.claimedPenalty.push(await claimedPenaltyResponse);
          await expect(claimedPenalty).CheckResponse();

          expect(actionResponse.actionNumber).toBe(claimedPenaltyResponse.outgoingDocument.name);
          expect(actionResponse.penaltyClaimAmount).toBe(claimedPenaltyResponse.amount);
        });

        await test.step('Reverse Penalty', async () => {
          const claimedPenaltyId = Responses.claimedPenalty[0].id;
          const reverse = await Request.post(`${Endpoints.claimedPenalty}/${claimedPenaltyId}/reverse`);
          const reversedClaimedPenaltyId = await reverse.json();
          console.log('reversedClaimedPenaltyId', reversedClaimedPenaltyId);
          await expect(reverse).CheckResponse();

          const reversedCp = await Request.get(`${Endpoints.claimedPenalty}/${reversedClaimedPenaltyId}`);
          await expect(reversedCp).CheckResponse();
          const reversedCpResponse = await reversedCp.json();
          Responses.claimedPenalty.push(reversedCpResponse);
          console.log('reversedClaimedPenalty', reversedCpResponse);
        });

        await test.step('Validate original is marked as reversed', async () => {
          const originalRefresh = await Request.get(`${Endpoints.claimedPenalty}/${Responses.claimedPenalty[0].id}`);
          await expect(originalRefresh).CheckResponse();
          const originalRefreshResponse = await originalRefresh.json();
          expect(originalRefreshResponse.reversed).toBe(true);
        });

        await test.step('Validate reversed claimed penalty', async () => {
          const original = Responses.claimedPenalty[0];
          const reversed = Responses.claimedPenalty[1];

          expect(reversed.type).toBe('REVERSAL');
          expect(reversed.number).toHaveLength(10);
          expect(reversed.prefix).toBeDefined();
          expect(reversed.creationDate).toBe(randomGens.generateUtcDate('yyyy-mm-dd'));
          expect(reversed.dueDate).toBe(reversed.creationDate);

          expect(reversed.amount).toBe(-original.amount);
          if (original.amountOtherCurrency !== null) {
            expect(reversed.amountOtherCurrency).toBe(-original.amountOtherCurrency);
          }
          expect(reversed.currency.id).toBe(original.currency.id);

          expect(reversed.incomeAccountNumber).toBe(original.incomeAccountNumber);
          expect(reversed.costCenter).toBe(original.costCenter);
          expect(reversed.customer.id).toBe(original.customer.id);
          expect(reversed.billingGroup.id).toBe(original.billingGroup.id);

          expect(reversed.documentTemplate).toBeDefined();
          expect(reversed.emailTemplate).toBeDefined();

          expect(reversed.outgoingDocument).toBeDefined();
          expect(reversed.outgoingDocument.id).toBe(original.id);

          expect(reversed.liabilityReceivable).toBeDefined();
        });

        await test.step('Validate receivable from reversal', async () => {
          const original = Responses.claimedPenalty[0];
          const reversed = Responses.claimedPenalty[1];

          const receivable = await Request.get(`customer-receivable/${reversed.liabilityReceivable.id}`);
          await expect(receivable).CheckResponse();
          const receivableResponse = await receivable.json();
          Responses.customerReceivable.push(receivableResponse);
          console.log('receivableFromReversal', receivableResponse);

          expect(receivableResponse.status).toBe('ACTIVE');
          expect(receivableResponse.customerResponse.id).toBe(original.customer.id);
          expect(receivableResponse.initialAmount).toBe(Math.abs(reversed.amount));
          expect(receivableResponse.currencyResponse.id).toBe(original.currency.id);
          expect(receivableResponse.numberOfIncomeAccount).toBe(original.incomeAccountNumber);
          expect(receivableResponse.costCenterControllingOrder).toBe(original.costCenter);
          expect(receivableResponse.outgoingDocumentType).toBe('CLAIMED_PENALTY');
          expect(receivableResponse.claimedPenaltyShortResponse.id).toBe(reversed.id);
        });

        test.info().attach('[REG-1197] response', {
          body: JSON.stringify(reportGenerator.setLinksToResponses(Responses), null, 2),
          contentType: 'application/json'
        });

        console.log(reportGenerator.setLinksToResponses(Responses));
      });
    });
  });
});