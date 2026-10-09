/**
 * One-shot Dev data setup for PDT-3171 manual EasyPay verification.
 * Creates: legal customer + open liability (known dueDate). Does NOT confirm payment.
 *
 * From EnergoTS:
 *   npx ts-node --transpile-only scripts/create-pdt3171-manual-verify-data.ts
 */
import * as fs from 'fs';
import * as path from 'path';
import { request } from '@playwright/test';
import { customerLegal } from '../jsons/payloads/create/customer/customerLegal';
import { customer_liability } from '../jsons/payloads/create/Receivables/customerLiability';
import { randomGens } from '../utils/randomGens';

const PHOENIX = 'http://10.236.20.11:8091/';
const EPAY = 'http://10.236.20.11:9091/';
const MERCHANT_ID = '7000005';
const DUE_DATE = '15-09-2026'; // → VALIDTO 20260915
const AMOUNT = 100;
const OUT_DIR = path.resolve(
  __dirname,
  '../../config/jira/PDT-3171-run-extract',
);

function getToken(): string {
  const tokenPath = path.resolve(__dirname, '../fixtures/token.json');
  return JSON.parse(fs.readFileSync(tokenPath, 'utf-8')).token;
}

async function main() {
  const token = getToken();
  const api = await request.newContext({
    baseURL: PHOENIX,
    extraHTTPHeaders: {
      Accept: '*/*',
      'Content-Type': 'application/json',
      Authorization: `Bearer ${token}`,
    },
  });
  const epay = await request.newContext({
    baseURL: EPAY,
    extraHTTPHeaders: { Accept: '*/*' },
  });

  const customerPayload = customerLegal();
  const custRes = await api.post('customer', { data: customerPayload });
  if (!custRes.ok()) {
    throw new Error(`customer create failed: ${custRes.status()} ${await custRes.text()}`);
  }
  const customer = await custRes.json();
  const customerId = customer.id;
  const customerNumber = String(customer.customerNumber);
  const identifier = String(customer.identifier);

  const liabilityPayload = customer_liability();
  liabilityPayload.customerId = customerId;
  liabilityPayload.initialAmount = AMOUNT;
  liabilityPayload.dueDate = DUE_DATE;
  liabilityPayload.occurrenceDate = randomGens.generateTodaysDate('dd-mm-yyyy');

  const liabRes = await api.post('customer-liability', { data: liabilityPayload });
  if (!liabRes.ok()) {
    throw new Error(`liability create failed: ${liabRes.status()} ${await liabRes.text()}`);
  }
  const liabilityId = await liabRes.json();

  const detailRes = await api.get(`customer-liability/${liabilityId}`);
  const detail = await detailRes.json();

  const tid = randomGens.generateTID();
  const checksumInitUrl =
    `${EPAY}epay/calculate-check-sum-init?IDN=${customerNumber}&TID=${tid}&MERCHANTID=${MERCHANT_ID}&TYPE=BILLING`;
  const checksumInitRes = await epay.get(
    `epay/calculate-check-sum-init?IDN=${customerNumber}&TID=${tid}&MERCHANTID=${MERCHANT_ID}&TYPE=BILLING`,
  );
  const checksumInit = (await checksumInitRes.text()).trim();

  const initUrl =
    `${EPAY}epay/init-pay?IDN=${customerNumber}&TID=${tid}&MERCHANTID=${MERCHANT_ID}&TYPE=BILLING&CHECKSUM=${checksumInit}`;
  const initRes = await epay.get(
    `epay/init-pay?IDN=${customerNumber}&TID=${tid}&MERCHANTID=${MERCHANT_ID}&TYPE=BILLING&CHECKSUM=${checksumInit}`,
  );
  const initBody = await initRes.json();

  const date = randomGens.generateOnlinePaymentDate(4);
  const total = initBody.AMOUNT ?? AMOUNT * 100;
  const checksumConfirmUrl =
    `${EPAY}epay/calculate-check-sum-confirm?DATE=${date}&TYPE=BILLING&MERCHANTID=${MERCHANT_ID}&IDN=${customerNumber}&TOTAL=${total}&TID=${tid}`;
  const checksumConfirmRes = await epay.get(
    `epay/calculate-check-sum-confirm?DATE=${date}&TYPE=BILLING&MERCHANTID=${MERCHANT_ID}&IDN=${customerNumber}&TOTAL=${total}&TID=${tid}`,
  );
  const checksumConfirm = (await checksumConfirmRes.text()).trim();

  const confirmUrl =
    `${EPAY}epay/confirm-pay?DATE=${date}&TYPE=BILLING&MERCHANTID=${MERCHANT_ID}&IDN=${customerNumber}&TOTAL=${total}&TID=${tid}&CHECKSUM=${checksumConfirm}`;

  const tidBad = randomGens.generateTID();
  const badCsRes = await epay.get(
    `epay/calculate-check-sum-init?IDN=${customerNumber}&TID=${tidBad}&MERCHANTID=${MERCHANT_ID}&TYPE=BILLING`,
  );
  const badCs = (await badCsRes.text()).trim();
  const badChecksumInitPayUrl =
    `${EPAY}epay/init-pay?IDN=${customerNumber}&TID=${tidBad}&MERCHANTID=${MERCHANT_ID}&TYPE=BILLING&CHECKSUM=DEADBEEF`;
  const badMerchantInitUrl =
    `${EPAY}epay/init-pay?IDN=${customerNumber}&TID=${tidBad}&MERCHANTID=9999999&TYPE=BILLING&CHECKSUM=${badCs}`;

  const out = {
    createdAt: new Date().toISOString(),
    environment: 'dev',
    note: 'Payment NOT confirmed — liability still open. Run confirm only if you want to complete payment.',
    entities: {
      customerId,
      customerNumber,
      identifier,
      liabilityId,
      liabilityDueDate: detail.dueDate,
      liabilityCurrentAmount: detail.currentAmount,
      initialAmount: AMOUNT,
    },
    expected: {
      initPay: {
        STATUS: '00',
        VALIDTO: '20260915',
        VALIDTO_must_not_be: '0',
        AMOUNT: total,
      },
      confirmPay_correctTotal: { STATUS: '00' },
      confirmPay_wrongTotal: { STATUS: '96' },
      initPay_badChecksum: { STATUS: '93' },
      initPay_badMerchant: { STATUS: '96' },
    },
    phoenixPayloads: {
      customer: { method: 'POST', url: `${PHOENIX}customer`, body: customerPayload },
      customerLiability: {
        method: 'POST',
        url: `${PHOENIX}customer-liability`,
        body: liabilityPayload,
      },
    },
    easyPayReadyUrls: {
      '1_calculate_check_sum_init': checksumInitUrl,
      '1_checksum_value': checksumInit,
      '2_init_pay': initUrl,
      '2_init_pay_actual_response': initBody,
      '3_calculate_check_sum_confirm': checksumConfirmUrl,
      '3_checksum_value': checksumConfirm,
      '4_confirm_pay': confirmUrl,
      'neg_init_bad_checksum': badChecksumInitPayUrl,
      'neg_init_bad_merchant': badMerchantInitUrl,
      'neg_confirm_wrong_total_needs_fresh_checksum':
        'Recompute checksum-confirm with TOTAL=AMOUNT+1, then call confirm-pay — expect STATUS 96',
    },
    portalHints: {
      customerSearch: identifier,
      swagger_phoenix: 'http://10.236.20.11:8091/swagger-ui/index.html#',
      swagger_epay: 'http://10.236.20.11:9091/swagger-ui/index.html#',
    },
  };

  fs.mkdirSync(OUT_DIR, { recursive: true });
  const outPath = path.join(OUT_DIR, 'manual-verify-ready.json');
  fs.writeFileSync(outPath, JSON.stringify(out, null, 2), 'utf-8');
  console.log(JSON.stringify(out, null, 2));
  console.log(`\nWrote ${outPath}`);

  await api.dispose();
  await epay.dispose();
}

main().catch((e) => {
  console.error(e);
  process.exit(1);
});
