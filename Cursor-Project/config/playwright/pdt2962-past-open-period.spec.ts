import { test, expect } from '../../EnergoTS/fixtures/baseFixture';

const PAST_OP_DATE = '2026-05-15';
const OPEN_MAY_PERIOD_ID = 1041; // ACCOUNTING202605 — past month, still OPEN on Dev

test('PDT-2962 Step1: past operation_date in OPEN accounting period', async ({
  Request,
  GeneratePayload,
  Responses,
  Endpoints,
}) => {
  await test.step('customer + collection + package', async () => {
    const customer = await Request.post(Endpoints.customer, {
      data: GeneratePayload.customers.customer_legal(),
    });
    await expect(customer).CheckResponse();
    Responses.customer.push(await customer.json());

    const channel = await Request.post(Endpoints.collectionChannel, {
      data: GeneratePayload.receivablesManagement.collection_channel(),
    });
    await expect(channel).CheckResponse();
    Responses.collectionChannel.push(await channel.json());

    const pkgPayload = GeneratePayload.receivablesManagement.payment_package();
    pkgPayload.paymentDate = PAST_OP_DATE;
    pkgPayload.accountingPeriodId = OPEN_MAY_PERIOD_ID;
    const pkg = await Request.post(Endpoints.paymentPackage, { data: pkgPayload });
    await expect(pkg).CheckResponse();
    Responses.paymentPackage.push(await pkg.json());
  });

  await test.step('liability + payment (past date, May OPEN period)', async () => {
    const liabilityPayload = GeneratePayload.receivablesManagement.customer_liability();
    liabilityPayload.initialAmount = 100;
    const liability = await Request.post(Endpoints.customerLiability, { data: liabilityPayload });
    await expect(liability).CheckResponse();
    Responses.customerLiability.push(await liability.json());

    const paymentPayload = await GeneratePayload.receivablesManagement.payment();
    paymentPayload.initialAmount = 200;
    paymentPayload.paymentDate = PAST_OP_DATE;
    paymentPayload.accountPeriodId = OPEN_MAY_PERIOD_ID;
    const payment = await Request.post(Endpoints.payment, { data: paymentPayload });
    await expect(payment).CheckResponse();
    Responses.payment.push(await payment.json());
  });

  const paymentId = Responses.payment[0];
  const paymentGet = await Request.get(`${Endpoints.payment}/${paymentId}`);
  await expect(paymentGet).CheckResponse();
  const body = await paymentGet.json();
  const receivable = body.offsettingResponseList?.find(
    (item: { offsettingObjectType: string }) => item.offsettingObjectType === 'RECEIVABLE',
  );
  expect(receivable).toBeTruthy();
  console.log('CRT receivable id:', receivable.id, 'paymentDate:', PAST_OP_DATE, 'period:', OPEN_MAY_PERIOD_ID);
});
