import { request } from 'playwright';
import fs from 'fs';
import path from 'path';
import { customerLegal } from '../../EnergoTS/jsons/payloads/create/customer/customerLegal';
import { collection_channel } from '../../EnergoTS/jsons/payloads/create/Receivables/collectionChannel';
import { payment_package } from '../../EnergoTS/jsons/payloads/create/Receivables/paymentPackage';
import { customer_liability } from '../../EnergoTS/jsons/payloads/create/Receivables/customerLiability';
import { payment } from '../../EnergoTS/jsons/payloads/create/Receivables/payment';

const energoRoot = path.resolve(__dirname, '../../EnergoTS');
const token = JSON.parse(fs.readFileSync(path.join(energoRoot, 'fixtures/token.json'), 'utf8')).token;
const baseURL = (process.env.BASE_URL || 'http://10.236.20.11:8091').replace(/\/?$/, '/');
const PAST_OP_DATE = process.env.PDT2962_PAST_OP_DATE ?? '2026-05-15';
const OPEN_MAY_PERIOD_ID = Number(process.env.PDT2962_OPEN_PERIOD_ID ?? 1041);

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
  pkgPayload.paymentDate = PAST_OP_DATE;
  pkgPayload.accountingPeriodId = OPEN_MAY_PERIOD_ID;
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
  paymentPayload.paymentDate = PAST_OP_DATE;
  paymentPayload.accountPeriodId = OPEN_MAY_PERIOD_ID;
  const pay = await post('payment', paymentPayload);
  const payId = typeof pay === 'number' ? pay : pay.id;
  const body = await (await ctx.get(`payment/${payId}`)).json();
  const receivable = body.offsettingResponseList?.find((x: { offsettingObjectType: string }) => x.offsettingObjectType === 'RECEIVABLE');
  console.log(JSON.stringify({
    scenario: 'PDT-2962 Step 1: past operation_date in OPEN accounting period',
    paymentId: payId,
    crtId: receivable?.id,
    operationDate: PAST_OP_DATE,
    expectedAccountPeriodId: OPEN_MAY_PERIOD_ID,
    offsetting: body.offsettingResponseList?.map((x: { id: number; offsettingObjectType: string }) => ({ crtId: x.id, type: x.offsettingObjectType })),
  }, null, 2));
  await ctx.dispose();
}
main().catch((e) => { console.error(e); process.exit(1); });
