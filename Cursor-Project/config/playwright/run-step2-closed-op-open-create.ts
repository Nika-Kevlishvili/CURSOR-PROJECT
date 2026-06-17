import { request } from 'playwright';
import fs from 'fs';
import path from 'path';
import { customerLegal } from '../../EnergoTS/jsons/payloads/create/customer/customerLegal';
import { collection_channel } from '../../EnergoTS/jsons/payloads/create/Receivables/collectionChannel';
import { payment_package } from '../../EnergoTS/jsons/payloads/create/Receivables/paymentPackage';
import { customer_liability } from '../../EnergoTS/jsons/payloads/create/Receivables/customerLiability';
import { payment } from '../../EnergoTS/jsons/payloads/create/Receivables/payment';

const energoRoot = path.resolve(__dirname, '../../EnergoTS');
const env = JSON.parse(fs.readFileSync(path.join(energoRoot, 'fixtures/envVariables.json'), 'utf8'));
const token = JSON.parse(fs.readFileSync(path.join(energoRoot, 'fixtures/token.json'), 'utf8')).token;
const baseURL = (process.env.BASE_URL || 'http://10.236.20.11:8091').replace(/\/?$/, '/');

// Step 2: op on CLOSED period (Jan 2026), create NOW on OPEN period (Jun 2026)
const CLOSED_OP_DATE = '2026-01-15';
const OPEN_CREATE_PERIOD_ID = env.accounting_period; // current OPEN (e.g. 1042 ACCOUNTING202606)

async function main() {
  const ctx = await request.newContext({
    baseURL,
    extraHTTPHeaders: { Authorization: `Bearer ${token}`, Accept: '*/*', 'Content-Type': 'application/json' },
  });
  const post = async (ep: string, data: unknown) => {
    const res = await ctx.post(ep, { data });
    const text = await res.text();
    if (!res.ok()) throw new Error(`${ep} ${res.status()}: ${text}`);
    return JSON.parse(text);
  };

  const customerRes = await post('customer', customerLegal());
  const customerId = typeof customerRes === 'number' ? customerRes : customerRes.id;
  const channelRes = await post('collection-channel', collection_channel());
  const channelId = typeof channelRes === 'number' ? channelRes : channelRes.id;

  const pkgPayload = payment_package();
  pkgPayload.channelId = channelId;
  pkgPayload.paymentDate = CLOSED_OP_DATE;
  pkgPayload.accountingPeriodId = OPEN_CREATE_PERIOD_ID;
  const pkg = await post('payment-package', pkgPayload);
  const pkgId = typeof pkg === 'number' ? pkg : pkg.id;

  const liabilityPayload = customer_liability();
  liabilityPayload.customerId = customerId;
  liabilityPayload.initialAmount = 100;
  await post('customer-liability', liabilityPayload);

  const paymentPayload = payment();
  paymentPayload.customerId = customerId;
  paymentPayload.collectionChannelId = channelId;
  paymentPayload.paymentPackageId = pkgId;
  paymentPayload.initialAmount = 200;
  paymentPayload.paymentDate = CLOSED_OP_DATE;
  paymentPayload.accountPeriodId = OPEN_CREATE_PERIOD_ID;
  const pay = await post('payment', paymentPayload);
  const payId = typeof pay === 'number' ? pay : pay.id;

  const body = await (await ctx.get(`payment/${payId}`)).json();
  const receivableOffset = body.offsettingResponseList?.find(
    (x: { offsettingObjectType: string }) => x.offsettingObjectType === 'RECEIVABLE',
  );
  const liabilityOffset = body.offsettingResponseList?.find(
    (x: { offsettingObjectType: string }) => x.offsettingObjectType === 'LIABILITY',
  );

  console.log(
    JSON.stringify(
      {
        scenario: 'PDT-2962 Step 2: closed op period + open create period',
        paymentId: payId,
        operationDate: CLOSED_OP_DATE,
        closedOpPeriodHint: 'ACCOUNTING202601 (1037)',
        openCreatePeriodId: OPEN_CREATE_PERIOD_ID,
        expectedCrtAccountPeriodId: OPEN_CREATE_PERIOD_ID,
        offsetting: body.offsettingResponseList?.map((x: { id: number; offsettingObjectType: string }) => ({
          crtId: x.id,
          type: x.offsettingObjectType,
        })),
        receivableDestId: receivableOffset?.destObjectId ?? receivableOffset?.id,
        liabilityCrtId: liabilityOffset?.id,
      },
      null,
      2,
    ),
  );

  await ctx.dispose();
}

main().catch((e) => {
  console.error(e);
  process.exit(1);
});
