import reportGenerator from '../../../backend/utils/generateReport';
import { randomGens } from '../../../backend/utils/randomGens';
import {test, expect, expectBulgarianPostSuccess, findBulgarianPostCollectionChannel, resolveChannelExchangeRate, BULGARIAN_POST_CHANNEL_NAME, coerceMoney, resolveChannelCurrencyMeta, expectMoney2} from '../../../backend/fixtures/bulgarianPost.fixtures';

const round2 = (n: number) => Math.round((n + Number.EPSILON) * 100) / 100;

type ObligationRow = {
  CustomerNumber: number;
  CustomerName: string;
  CustomerAddress: string;
  DocumentDate: string;
  DocumentInfo: string;
  DocumentNumber: any;
  AllowedForPayment: boolean;
  AllowedPartialPayment: boolean;
  Another: 0;
  Principal: number;
  Interest: number;
  Currency: string;
};

test.describe('[REG-56]: Receivables Management - Payments', { tag: '@receivableManagement' }, () => {
  test.describe('[REG-115]: Payments', () => {
    test.describe('[REG-874]: Create', () => {
      test('[REG-1174]: Online payment - Bulgarian post integration: happy pass on one customer, liability from manual invoice, combined is unchecked on collection channel', async ({Request,GeneratePayload,Responses,Endpoints,BpClient,BulgarianPostCredentials,receivableValidations}) => {
        test.setTimeout(5 * 60 * 1000);

        let customerNumber10: string;
        let obligationRow: ObligationRow;
        let exchangeRate: number;
        let tid: string;
        let paidSum: number;
        let invoiceNumber: string;
        let currency: string;
        let liabilityAmount: number;
        let liabilityCurrency: string;

        await test.step('Precondition: create customer', async () => {
          const customer = await Request.post(Endpoints.customer, {data: GeneratePayload.customers.customer_legal()});
          await expect(customer).CheckResponse();
          Responses.customer.push(await customer.json());
          customerNumber10 = String(Responses.customer[0].customerNumber);
        });

        await test.step('Precondition: term, POD, product, contract, activate POD', async () => {
          const term = await Request.post(Endpoints.terms, {data: GeneratePayload.productAndServices.term()});
          await expect(term).CheckResponse();
          Responses.terms.push(await term.json());

          const pod = await Request.post(Endpoints.pod, {data: GeneratePayload.pointsOfDelivery.pod_settlement()});
          await expect(pod).CheckResponse();
          Responses.pod.push(await pod.json());

          const product = await Request.post(Endpoints.product, {data: GeneratePayload.productAndServices.product()});
          await expect(product).CheckResponse();
          Responses.product.push(await product.json());

          const contract = await Request.post(Endpoints.productContract, {data: await GeneratePayload.contractsAndOrders.product_contract()});
          await expect(contract).CheckResponse();
          Responses.productContract.push(await contract.json());

          const podActivation = await Request.post('/contract-pods/manual', {data: await GeneratePayload.pointsOfDelivery.pod_activation()});
          await expect(podActivation).CheckResponse();
        });

        await test.step('Precondition: manual invoice billing run', async () => {
          const payload = await GeneratePayload.billing.manualInvoice();
          const createManualInvoice = await Request.post(Endpoints.billingRun, {data: payload});
          await expect(createManualInvoice).CheckResponse();
          Responses.billingRun.push(await createManualInvoice.json());
        });

        await test.step('make the invoice real', async () => {
          await GeneratePayload.billing.waitForInvoiceGeneration()
        })

        await test.step('Precondition: wait for liability', async () => {
          const invoiceId = Responses.invoice[0];
          const findLiability = await Request.get(`${Endpoints.customerLiability}/list?page=0&size=25&columns=ID&direction=DESC&prompt=${Responses.customer[0].identifier}&searchFields=CUSTOMER`);
          await expect(findLiability).CheckResponse();
          const listBody = await findLiability.json();
          expect(listBody.content ?? []).toHaveLength(1);

          const liabilityId = listBody.content[0].id;
          const liabilityGet = await Request.get(`${Endpoints.customerLiability}/${liabilityId}`);
          await expect(liabilityGet).CheckResponse();
          const liability = await liabilityGet.json();
          liabilityAmount = liability.initialAmount;
          liabilityCurrency = liability.currencyResponse.name
          invoiceNumber = String(liability.invoiceResponse?.invoiceNumber ?? '').split('-').at(-1)!;

          expect(liability.invoiceResponse.id).toEqual(invoiceId);
          Responses.customerLiability.push(liabilityId);
        });

        await test.step('modify BulgarianPost collection channel and get exchange rate', async () => {
          const { id, body } = await findBulgarianPostCollectionChannel(Request, Endpoints);
          Responses.collectionChannel.push(id);

          await GeneratePayload.receivablesManagement.modifyChannel(Responses.collectionChannel[0], false);
          const channelGet = await Request.get(`${Endpoints.collectionChannel}/${id}`);
          currency = (await channelGet.json()).currencyId.name
          exchangeRate = await resolveChannelExchangeRate(Request, Endpoints, body);
        });

        await test.step('Bulgarian Post: GET obligations (new token)', async () => {
          const response = await BpClient.getObligations(customerNumber10);
          const body = await expectBulgarianPostSuccess(response);
          expect(body.Results?.length).toBeGreaterThan(0);
          const rows = (body.Results ?? []) as ObligationRow[];

          const payable = rows.find((x) => x.AllowedForPayment === true);
          expect(payable, 'Expected at least one obligation with AllowedForPayment=true').toBeDefined();
          obligationRow = payable!;

          expect(rows[0]?.DocumentNumber).toBe(invoiceNumber);
          const DocumentInfo = rows.find((x) => x.DocumentInfo === "MANUAL INVOICE");
          expect(DocumentInfo, 'documment type is not right').toBeTruthy();
          expect(rows[0]?.DocumentDate).not.toBe(null);
          expect(rows[0]?.Currency).toBe(currency);

          if(liabilityCurrency != currency) {
            expect(rows[0]?.Principal).toBe(round2(liabilityAmount / exchangeRate))
          }
          else{
            expect(rows[0]?.Principal).toBe(round2(liabilityAmount))
          }
        });

        await test.step('Bulgarian Post: GET receipt (new token)', async () => {
          tid = randomGens.generateBulgarianPostTid();
          const roundExchange = (rate: number) => Math.round(rate * 100_000) / 100_000;
          const principal = Number(obligationRow.Principal);
          const interest = Number(obligationRow.Interest ?? 0);
          paidSum = principal + interest;

          const response = await BpClient.getReceipt({
            CustomerNumber: customerNumber10,
            DocumentNumber: obligationRow.DocumentNumber,
            Currency: obligationRow.Currency,
            Exchange: roundExchange(exchangeRate),
            PaidSum: paidSum,
            TID: tid,
            PostCode: BulgarianPostCredentials.PostCode
          });
          const body = await expectBulgarianPostSuccess(response);
          expect(Number(body.Principal)).toBe(principal);
          expect(Number(body.Interest)).toBe(interest);
        });

        await test.step('Bulgarian Post: GET pay (new token)', async () => {
          const response = await BpClient.pay(tid);
          await expectBulgarianPostSuccess(response);
        });

        await test.step('Verify BulgarianPost payment exists in Phoenix', async () => {
          const bulgarianPostChannelId = Responses.collectionChannel[0] as number;
          const paymentList = await Request.post(`${Endpoints.payment}/list`, {
            data: {
              page: 0,
              size: 25,
              prompt: `${Responses.customer[0].identifier}`,
              searchFields: 'CUSTOMER_IDENTIFIER',
              collectionChannelIds: [bulgarianPostChannelId]
            }
          });
          await expect(paymentList).CheckResponse();
          const listBody = await paymentList.json();
          expect(listBody.totalElements).toBeGreaterThan(0);

          type PaymentListRow = { id: number; collectionChannel?: string };
          const bulgarianPostPayment = (listBody.content as PaymentListRow[]).find((payment) => payment.collectionChannel === BULGARIAN_POST_CHANNEL_NAME);
          expect(bulgarianPostPayment, `Expected payment with collection channel ${BULGARIAN_POST_CHANNEL_NAME}`).toBeDefined();
          Responses.payment.push(bulgarianPostPayment!.id);
        });

        await test.step('Validate BulgarianPost payment', async () => {
          await receivableValidations.paymentValidation(Responses.payment[0]);
        });

        await test.step('Verify payment is fully offset', async () => {
          await expect
            .poll(
              async () => {
                const paymentGet = await Request.get(`${Endpoints.payment}/${Responses.payment[0]}`);
                await expect(paymentGet).CheckResponse();
                const paymentBody = await paymentGet.json();
                return paymentBody.currentAmount as number;
              },
              {
                message: `Payment id=${Responses.payment[0]} should be fully offset`,
                timeout: 30_000,
                intervals: [1000]
              }
            )
            .toBe(0);
        });

        await test.step('Verify liability is fully offset', async () => {
          const liabilityGet = await Request.get(`${Endpoints.customerLiability}/${Responses.customerLiability[0]}`);
          await expect(liabilityGet).CheckResponse();
          const liabilityBody = await liabilityGet.json();
          expect(liabilityBody.currentAmount).toBe(0);
        });

        test.info().attach('[REG-1174]: Online payment - Bulgarian post integration: happy pass on one customer, liability from manual invoice, combined is unchecked on collection channel', {
          body: JSON.stringify(reportGenerator.setLinksToResponses(Responses), null, 2),
          contentType: 'application/json'
        })

      });

      test('[REG-1212]: Online payment - Bulgarian post integration: happy pass on one customer, multiple liability, with combined channel', async ({Request,GeneratePayload,Responses,Endpoints,BpClient,BulgarianPostCredentials,receivableValidations}) => {
        test.setTimeout(5 * 60 * 1000);

        let customerNumber10: string;
        let customerDetails: any;
        let obligationRow: ObligationRow;
        let exchangeRate: number;
        let tid: string;
        let paidSum: number;
        let invoiceNumber: string;
        let currency: string;
        let invoiceLiabilityAmount: number;
        let invoiceLiabilityCurrency: string;
        let interestPercent: any;
        let manualLiabilityAmount:any;
        let interestAmount: number;
        let junkAmount: number;
        let lpfLiabilityAmount: number;
        let lpfNumber: any;
        let depositLiabilityAmount: number;
        let depositNumber: string;
        let externalNumber: string;
        let bulgarianPostChannelId:number;
        let podDetails:any;
        let principal: any;
        let interest: any;

        await test.step('create customer', async() => {
          const customer = await Request.post(Endpoints.customer, {data: GeneratePayload.customers.customer_legal()});
          await expect(customer).CheckResponse();
          Responses.customer.push(await customer.json());
          customerNumber10 = String(Responses.customer[0].customerNumber);

          const customerGet = await Request.get(`${Endpoints.customer}/${Responses.customer[0].id}`);
          customerDetails = await customerGet.json();
        })

        await test.step('generate interest rate', async() => {
          const payload = GeneratePayload.receivablesManagement.dailyInterestRate();
          interestPercent = payload.interestRatePeriods[0].amountInPercent
          const interestRate = await Request.post(Endpoints.interestRate, {data: payload});
          expect(interestRate).CheckResponse();
          Responses.interestRate.push(await interestRate.json());
        })

        /**LPF liability generation ⬇️*/
        await test.step('create liability', async() => {
          const liabilityPayload = GeneratePayload.receivablesManagement.customer_liability()
          junkAmount = randomGens.getRandomNumber();
          liabilityPayload.initialAmount = junkAmount
          liabilityPayload.applicableInterestRateId = Responses.interestRate[0];
          liabilityPayload.dueDate = randomGens.generateYesterdaysDate("dd-mm-yyyy")
          liabilityPayload.occurrenceDate = randomGens.generateOneWeekBeforeDate("dd-mm-yyyy")
          const liabilityPost = await Request.post(`${Endpoints.customerLiability}`, {data: liabilityPayload});

          await expect(liabilityPost).CheckResponse();
          Responses.customerLiability.push(await liabilityPost.json());
        });

        await test.step('create collection', async() => {
          const collectionChannel = await Request.post(Endpoints.collectionChannel, {data: GeneratePayload.receivablesManagement.collection_channel()});
          await expect(collectionChannel).CheckResponse();
          const connectionChannelData = await collectionChannel.json();
          Responses.collectionChannel.push(connectionChannelData);
        });

        await test.step('create payment package', async() => {
          const paymentPackage = await Request.post(Endpoints.paymentPackage, {data: GeneratePayload.receivablesManagement.payment_package()});
          await expect(paymentPackage).CheckResponse();
          const paymentPackageData = await paymentPackage.json();
          Responses.paymentPackage.push(paymentPackageData);
        });

        await test.step('create payment', async() => {
          const newPayload = await GeneratePayload.receivablesManagement.payment(false, false);
          newPayload.initialAmount = junkAmount;
          const payment = await Request.post(Endpoints.payment, { data: newPayload });
          await expect(payment).CheckResponse();
          const paymentData = await payment.json();
          Responses.payment.push(paymentData);
        })

        await test.step('check if LPFs are generated', async() => {
          const LPFs = await GeneratePayload.receivablesManagement.waitForLPFGeneration(false);
          expect(LPFs.content).toBeDefined();
          expect(LPFs.content??[]).toHaveLength(1);
          Responses.latePaymentFine.push(LPFs.content[0].id);
        })

        await test.step('check declared currencies and exchange rate', async() => {
          exchangeRate = await GeneratePayload.receivablesManagement.exchangeRateForMainCurrency();
        })

        await test.step('validate LPF', async() => {
          const LPFID = await Responses.latePaymentFine[0];
          await receivableValidations.LatePaymentFineValidation(LPFID);
        })

        await test.step('check LPF generated liability', async() => {
          const liability = await Request.get(`${Endpoints.customerLiability}/list?page=0&size=25&prompt=${Responses.customer[0].identifier}&searchFields=CUSTOMER&initialAmountTo=${junkAmount*(interestPercent/100)}`);
          expect(liability).CheckResponse();
          const responseBody = await liability.json();

          const liabilityId = responseBody.content[0].id;
          const liabilityGet = await Request.get(`${Endpoints.customerLiability}/${liabilityId}`);
          expect(liabilityGet).CheckResponse();
          const liabilityResponse = await liabilityGet.json();
          expect(liabilityResponse.latePaymentFineShortResponse.id).toBeTruthy();
          expect(liabilityResponse.latePaymentFineShortResponse.name).toContain('Fine');
          lpfLiabilityAmount = liabilityResponse.initialAmount
          lpfNumber = String(liabilityResponse.latePaymentFineShortResponse?.name ?? '').split('-').at(-1)!;

          Responses.customerLiability.push(liabilityId)
        })
        /**LPF liability generation ⬆️*/

        /**Liability from invoice ⬇️*/
        await test.step('Precondition: term, POD, product, contract, activate POD', async () => {
          const term = await Request.post(Endpoints.terms, {data: GeneratePayload.productAndServices.term()});
          await expect(term).CheckResponse();
          Responses.terms.push(await term.json());

          const pod = await Request.post(Endpoints.pod, {data: GeneratePayload.pointsOfDelivery.pod_settlement()});
          await expect(pod).CheckResponse();
          Responses.pod.push(await pod.json());

          const podGet = await Request.get(`${Endpoints.pod}/${Responses.pod[0].id}`);
          await expect(podGet).CheckResponse();
          podDetails = await podGet.json();

          const product = await Request.post(Endpoints.product, {data: GeneratePayload.productAndServices.product()});
          await expect(product).CheckResponse();
          Responses.product.push(await product.json());

          const contract = await Request.post(Endpoints.productContract, {data: await GeneratePayload.contractsAndOrders.product_contract()});
          await expect(contract).CheckResponse();
          Responses.productContract.push(await contract.json());

          const podActivation = await Request.post('/contract-pods/manual', {data: await GeneratePayload.pointsOfDelivery.pod_activation()});
          await expect(podActivation).CheckResponse();
        });

        await test.step('Precondition: manual invoice billing run', async () => {
          const payload = await GeneratePayload.billing.manualInvoice();
          const createManualInvoice = await Request.post(Endpoints.billingRun, {data: payload});
          await expect(createManualInvoice).CheckResponse();
          Responses.billingRun.push(await createManualInvoice.json());
        });

        await test.step('make the invoice real', async () => {
          await GeneratePayload.billing.waitForInvoiceGeneration()
        })

        await test.step('Precondition: wait for liability', async () => {
          const invoiceId = Responses.invoice[0];
          const findLiability = await Request.get(`${Endpoints.customerLiability}/list?page=0&size=25&columns=ID&direction=DESC&prompt=${Responses.customer[0].identifier}&searchFields=CUSTOMER`);
          await expect(findLiability).CheckResponse();
          const listBody = await findLiability.json();
          expect(listBody.content ?? []).toHaveLength(3);

          const liabilityId = listBody.content[0].id;
          const liabilityGet = await Request.get(`${Endpoints.customerLiability}/${liabilityId}`);
          await expect(liabilityGet).CheckResponse();
          const liability = await liabilityGet.json();
          invoiceLiabilityAmount = liability.initialAmount;
          invoiceLiabilityCurrency = liability.currencyResponse.name
          invoiceNumber = String(liability.invoiceResponse?.invoiceNumber ?? '').split('-').at(-1)!;

          expect(liability.invoiceResponse.id).toEqual(invoiceId);
          Responses.customerLiability.push(liabilityId);
        });
        /**Liability from invoice ⬆️*/

        await test.step('create liability(overdue for interest)', async() => {
          const liabilityPayload = GeneratePayload.receivablesManagement.customer_liability()
          manualLiabilityAmount = randomGens.getRandomNumber();
          liabilityPayload.initialAmount = manualLiabilityAmount
          liabilityPayload.applicableInterestRateId = Responses.interestRate[0];
          externalNumber = randomGens.generateExternalOutgoingDocumentNumber();
          liabilityPayload.outgoingDocumentFromExternalSystem = externalNumber
          liabilityPayload.dueDate = randomGens.generateYesterdaysDate("dd-mm-yyyy")
          liabilityPayload.occurrenceDate = randomGens.generateOneWeekBeforeDate("dd-mm-yyyy")
          const liabilityPost = await Request.post(`${Endpoints.customerLiability}`, {data: liabilityPayload});

          await expect(liabilityPost).CheckResponse();
          Responses.customerLiability.push(await liabilityPost.json());

          interestAmount = (manualLiabilityAmount/100)*interestPercent;
        })

        /**Deposit liability ⬇️*/
        await test.step('create deposit', async() =>{
          const depositPayload = GeneratePayload.receivablesManagement.deposit();
          depositLiabilityAmount = randomGens.getRandomNumber()
          depositPayload.initialAmount = depositLiabilityAmount;
          const deposit = await Request.post("/deposit", {data: depositPayload});
          await expect(deposit).CheckResponse();
          Responses.deposit.push(await deposit.json());
        });

        await test.step('Get deposit liability', async() => {
          const deposit_liability = await Request.get(`${Endpoints.customerLiability}/list?page=0&size=25&columns=ID&direction=DESC&prompt=${Responses.customer[0].identifier}&searchFields=CUSTOMER&initialAmountFrom=${depositLiabilityAmount}`);
          await expect(deposit_liability).CheckResponse();
          const listing = await deposit_liability.json();
          const liabilityId = listing.content[0].id;

          const liabilityGet = await Request.get(`${Endpoints.customerLiability}/${liabilityId}`);
          expect(liabilityGet).CheckResponse();
          const getResponseBody = await liabilityGet.json();
          
          expect(getResponseBody.depositShortResponse.id).toBeTruthy();
          expect(getResponseBody.depositShortResponse.name).toContain('ДЕ');
          expect(getResponseBody.currentAmount).toBe(depositLiabilityAmount)

          Responses.customerLiability.push(liabilityId);
          depositNumber = String(getResponseBody.depositShortResponse?.name ?? '').split('-').at(-1)!;
        });
        /**Deposit liability⬆️*/

        await test.step('modify BulgarianPost collection channel and get exchange rate', async () => {
          const { id, body } = await findBulgarianPostCollectionChannel(Request, Endpoints);
          bulgarianPostChannelId = id;
          Responses.collectionChannel.push(id);

          await GeneratePayload.receivablesManagement.modifyChannel(bulgarianPostChannelId, true);/**checking combine on channel*/
          const channelGet = await Request.get(`${Endpoints.collectionChannel}/${id}`);
          currency = (await channelGet.json()).currencyId.name
          const channelMeta = await resolveChannelCurrencyMeta(Request, Endpoints, body);
          currency = channelMeta.currencyName;
          exchangeRate = channelMeta.exchangeRate;
        });

        await test.step('Bulgarian Post: GET obligations (new token)', async () => {
          const perItemPrincipal = round2(
            [lpfLiabilityAmount, manualLiabilityAmount, depositLiabilityAmount, invoiceLiabilityAmount].reduce((sum, amount) => sum + round2(amount / exchangeRate), 0)
          );
          const perItemInterest = round2(round2(interestAmount) / exchangeRate);

          const response = await BpClient.getObligations(customerNumber10);
          const body = await expectBulgarianPostSuccess(response);
          expect(body.Results?.length).toBeGreaterThan(0);
          const rows = (body.Results ?? []) as ObligationRow[];

          const payable = rows.find((x) => x.AllowedForPayment === true);
          expect(payable, 'Expected at least one obligation with AllowedForPayment=true').toBeDefined();
          obligationRow = payable!;

          expect(rows[0]?.DocumentNumber).toContain(invoiceNumber);
          expect(rows[0]?.DocumentNumber).toContain(externalNumber);
          expect(rows[0]?.DocumentNumber).toContain(lpfNumber);
          expect(rows[0]?.DocumentNumber).toContain(depositNumber);
          const DocumentInfo = rows.find((x) => x.DocumentInfo === "MANUAL INVOICE");
          expect(DocumentInfo, 'documment type is not right').toBeTruthy();
          expect(rows[0]?.DocumentDate).toBe("");
          expect(rows[0]?.Currency).toBe(currency);

          const sameCurrencyPrincipal = round2([lpfLiabilityAmount, manualLiabilityAmount, depositLiabilityAmount, invoiceLiabilityAmount]
          .reduce((sum, amount) => sum + round2(amount), 0));

          if(invoiceLiabilityCurrency != currency) {
            expect(rows[0]?.Principal).toBe(round2(perItemPrincipal));
            expect(rows[0]?.Interest).toBe(perItemInterest);
            principal = perItemPrincipal;
            interest = perItemInterest;
          }
          else{
            expect(rows[0]?.Principal).toBe(sameCurrencyPrincipal);
            expect(rows[0]?.Interest).toBe(round2(interestAmount));
            principal = sameCurrencyPrincipal;
            interest = round2(interestAmount);
          }

          paidSum = round2(principal + interest);
          principal = Number(rows[0]?.Principal);
          interest = Number(rows[0]?.Interest);

          const expectedName = `${customerDetails.name} ${customerDetails.legalForm?.name ?? ''}`.replace(/\s+/g, ' ').trim();
          expect(String(rows[0]?.CustomerName).replace(/\s+/g, ' ').trim()).toBe(expectedName);
          expect(String(rows[0]?.CustomerNumber)).toBe(customerNumber10);
          expect(rows[0]?.CustomerAddress).toContain(podDetails.populatedPlace.name);
          expect(rows[0]?.CustomerAddress).toContain(podDetails.country.name);
          expect(rows[0]?.CustomerAddress).toContain(podDetails.municipality.name);
          expect(rows[0]?.CustomerAddress).toContain(podDetails.region.name);
          expect(rows[0]?.CustomerAddress).toContain(podDetails.zipCode.name);
          /**commenting extra address checks, we are not waiting cust.address anymore, here pod address returns */
          // expect(rows[0]?.CustomerAddress).toContain(customerDetails.districtId.name);
          // expect(rows[0]?.CustomerAddress).toContain(customerDetails.residentialAreaId.name); 
          // expect(rows[0]?.CustomerAddress).toContain(customerDetails.streetId.name);
        });

        await test.step('Bulgarian Post: GET receipt (new token)', async () => {
          const roundExchange = (rate: number) => Math.round(rate * 100_000) / 100_000;
          tid = randomGens.generateBulgarianPostTid();

          const response = await BpClient.getReceipt({
            CustomerNumber: customerNumber10,
            DocumentNumber: obligationRow.DocumentNumber,
            Currency: obligationRow.Currency,
            Exchange: roundExchange(exchangeRate),
            PaidSum: paidSum,
            TID: tid,
            PostCode: BulgarianPostCredentials.PostCode,
          });

          const body = await expectBulgarianPostSuccess(response);
          expectMoney2(
            coerceMoney(body.Principal) + coerceMoney(body.Interest ?? 0),
            paidSum,
            'PaidSum equals receipt Principal+Interest',
          );
          expect(Number(body.Principal)).toBe(principal);
          expect(Number(body.Interest)).toBe(interest);
        });

        await test.step('Bulgarian Post: GET pay (new token)', async () => {
          const response = await BpClient.pay(tid);
          await expectBulgarianPostSuccess(response);
        });

        await test.step('Verify BulgarianPost payment exists in Phoenix', async () => {
          const paymentList = await Request.post(`${Endpoints.payment}/list`, {
            data: {
              page: 0,
              size: 25,
              prompt: `${Responses.customer[0].identifier}`,
              searchFields: 'CUSTOMER_IDENTIFIER',
              collectionChannelIds: [bulgarianPostChannelId],
            },
          });
          await expect(paymentList).CheckResponse();
          const listBody = await paymentList.json();
          expect(listBody.totalElements).toBeGreaterThan(0);
          const paymentId = listBody.content[0].id;

          const getPayment = await Request.get(`${Endpoints.payment}/${paymentId}`);
          expect(getPayment).CheckResponse();
          const paymetnDetails = await getPayment.json();
          expect(paymetnDetails.initialAmount).toBe(paidSum);
          
          await expect.poll(
              async () => {
                const paymentGet = await Request.get(`${Endpoints.payment}/${paymentId}`);
                await expect(paymentGet).CheckResponse();
                const paymentBody = await paymentGet.json();
                return paymentBody.currentAmount as number;
              },
              {
                message: `Payment id=${paymentId} should be fully offset`,
                timeout: 30_000,
                intervals: [1000]
              }
          ).toBe(0);
          expect(paymetnDetails.customerId.personalNumber).toBe(customerDetails.identifier);
        });

        await test.step('check that all liabilities were covered', async() => {
          const liability1 = await Request.get(`${Endpoints.customerLiability}/${Responses.customerLiability[0]}`);
          expect(liability1).CheckResponse();
          const details1 = await liability1.json();
          expect(details1.currentAmount).toBe(0);

          const liability2 = await Request.get(`${Endpoints.customerLiability}/${Responses.customerLiability[1]}`);
          expect(liability2).CheckResponse();
          const details2 = await liability2.json();
          expect(details2.currentAmount).toBe(0);

          const liability3 = await Request.get(`${Endpoints.customerLiability}/${Responses.customerLiability[2]}`);
          expect(liability3).CheckResponse();
          const details3 = await liability3.json();
          expect(details3.currentAmount).toBe(0);

          const liability4 = await Request.get(`${Endpoints.customerLiability}/${Responses.customerLiability[3]}`);
          expect(liability4).CheckResponse();
          const details4 = await liability4.json();
          expect(details4.currentAmount).toBe(0);
        })

        test.info().attach('[REG-1212]: Online payment - Bulgarian post integration: happy pass on one customer, liability from manual invoice, combined is unchecked on collection channel', {
          body: JSON.stringify(reportGenerator.setLinksToResponses(Responses), null, 2),
          contentType: 'application/json'
        })
      });
    });
  });
});
