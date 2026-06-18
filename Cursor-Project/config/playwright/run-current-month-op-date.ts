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

const OP_DATE = '2026-06-17';
const OPEN_PERIOD_ID = env.accounting_period;

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
  const idOf = (r: number | { id: number }) => (typeof r === 'number' ? r : r.id);

  const customerId = idOf(await post('customer', customerLegal()));
  const channelId = idOf(await post('collection-channel', collection_channel()));
  const pkgPayload = payment_package();
  pkgPayload.channelId = channelId;
  pkgPayload.paymentDate = OP_DATE;
  pkgPayload.accountingPeriodId = OPEN_PERIOD_ID;
  const pkgId = idOf(await post('payment-package', pkgPayload));

  const liabilityPayload = customer_liability();
  liabilityPayload.customerId = customerId;
  liabilityPayload.initialAmount = 100;
  await post('customer-liability', liabilityPayload);

  const paymentPayload = payment();
  paymentPayload.customerId = customerId;
  paymentPayload.collectionChannelId = channelId;
  paymentPayload.paymentPackageId = pkgId;
  paymentPayload.initialAmount = 200;
  paymentPayload.paymentDate = OP_DATE;
  paymentPayload.accountPeriodId = OPEN_PERIOD_ID;
  const payId = idOf(await post('payment', paymentPayload));

  const body = await (await ctx.get(`payment/${payId}`)).json();
  const offsets = body.offsettingResponseList?.map((x: { id: number; offsettingObjectType: string }) => ({
    crtId: x.id,
    type: x.offsettingObjectType,
  }));

  console.log(
    JSON.stringify(
      {
        paymentId: payId,
        operationDate: OP_DATE,
        accountPeriodId: OPEN_PERIOD_ID,
        offsetting: offsets,
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
