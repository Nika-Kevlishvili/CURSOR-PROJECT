/**
 * Dev: create real proforma liability (null dueDate) for PDT-3171 manual EasyPay check.
 * Does NOT confirm payment.
 *
 * From EnergoTS:
 *   npx ts-node --transpile-only scripts/create-pdt3171-proforma-manual-verify-data.ts
 */
import * as fs from 'fs';
import * as path from 'path';
import { request } from '@playwright/test';
import { expect } from '../fixtures/baseFixture';
import { createResponsesContainer } from '../fixtures/types/responses';
import { Endpoints } from '../fixtures/constants/endpoints';
import { RequestWrapper } from '../utils/RequestWrapper';
import { GeneratePayload } from '../jsons/payloadGenerators/PayloadGenerator';
import { randomGens } from '../utils/randomGens';
import {
  PROFORMA_VALID_TO_DAYS,
  calculateInitChecksum,
  callInitPay,
  calculateConfirmChecksum,
  findEasyPayOnlineChannel,
  resolveEasyPayMerchantId,
  setEasyPayCombineLiabilities,
  setupProformaLiabilityChain,
  yyyyMmDdFromLocalDate,
  type Pdt3171Fx,
} from '../tests/cursor/pdt-3171-easypay-proforma-due-date-validto.fixtures';

const PHOENIX = 'http://10.236.20.11:8091/';
const EPAY = 'http://10.236.20.11:9091/';
const OUT_DIR = path.resolve(__dirname, '../../config/jira/PDT-3171-run-extract');

function readToken(): string {
  const tokenPath = path.resolve(__dirname, '../fixtures/token.json');
  return JSON.parse(fs.readFileSync(tokenPath, 'utf-8')).token;
}

async function main() {
  // Register CheckResponse matcher used by fixture helpers
  void expect;

  const token = readToken();
  const apiCtx = await request.newContext({
    baseURL: PHOENIX,
    extraHTTPHeaders: {
      Accept: '*/*',
      'Content-Type': 'application/json',
      Authorization: `Bearer ${token}`,
    },
  });
  const fileCtx = await request.newContext({
    baseURL: PHOENIX,
    extraHTTPHeaders: { Authorization: `Bearer ${token}` },
  });

  const Request = new RequestWrapper(apiCtx);
  const Responses = createResponsesContainer();
  const GeneratePayloadInst = new GeneratePayload(Request.raw, Responses, fileCtx);

  const fx: Pdt3171Fx = {
    Request,
    GeneratePayload: GeneratePayloadInst,
    Responses,
    Endpoints,
    OnlinePaymentUrl: EPAY,
  };

  console.log('Creating proforma liability chain (may take several minutes)...');
  const chain = await setupProformaLiabilityChain(fx);
  const customer = Responses.customer[0];
  const customerNumber = String(customer.customerNumber);
  const identifier = String(customer.identifier);

  const merchantId = await resolveEasyPayMerchantId(fx);
  const easyPayChannelId = await findEasyPayOnlineChannel(fx);
  await setEasyPayCombineLiabilities(fx, false, easyPayChannelId);

  const expectedValidTo = yyyyMmDdFromLocalDate(new Date(), PROFORMA_VALID_TO_DAYS);
  const tid = randomGens.generateTID();
  const checksumInit = await calculateInitChecksum(fx, {
    customerNumber,
    tid,
    merchantId,
  });
  const initBody = await callInitPay(fx, {
    customerNumber,
    tid,
    merchantId,
    checksum: checksumInit,
  });

  const date = randomGens.generateOnlinePaymentDate(4);
  const total = initBody.AMOUNT;
  const checksumConfirm = await calculateConfirmChecksum(fx, {
    date,
    customerNumber,
    tid,
    merchantId,
    total,
  });

  const checksumInitUrl =
    `${EPAY}epay/calculate-check-sum-init?IDN=${customerNumber}&TID=${tid}&MERCHANTID=${merchantId}&TYPE=BILLING`;
  const initUrl =
    `${EPAY}epay/init-pay?IDN=${customerNumber}&TID=${tid}&MERCHANTID=${merchantId}&TYPE=BILLING&CHECKSUM=${checksumInit}`;
  const checksumConfirmUrl =
    `${EPAY}epay/calculate-check-sum-confirm?DATE=${date}&TYPE=BILLING&MERCHANTID=${merchantId}&IDN=${customerNumber}&TOTAL=${total}&TID=${tid}`;
  const confirmUrl =
    `${EPAY}epay/confirm-pay?DATE=${date}&TYPE=BILLING&MERCHANTID=${merchantId}&IDN=${customerNumber}&TOTAL=${total}&TID=${tid}&CHECKSUM=${checksumConfirm}`;

  const out = {
    createdAt: new Date().toISOString(),
    environment: 'dev',
    type: 'PROFORMA_LIABILITY',
    note: 'Real proforma (null dueDate). Payment NOT confirmed. VALIDTO must be today+30, not "0".',
    entities: {
      customerId: customer.id,
      customerNumber,
      identifier,
      goodsOrderId: chain.goodsOrderId,
      proformaInvoiceId: chain.proformaInvoiceId,
      liabilityId: chain.liabilityId,
      liabilityDueDate: chain.dueDate || null,
      liabilityCurrentAmount: chain.currentAmount,
      merchantId,
      easyPayChannelId,
    },
    expected: {
      liabilityDueDate: null,
      initPay: {
        STATUS: '00',
        VALIDTO: expectedValidTo,
        VALIDTO_must_not_be: '0',
        AMOUNT: total,
      },
      confirmPay_correctTotal: { STATUS: '00' },
    },
    easyPayReadyUrls: {
      '1_calculate_check_sum_init': checksumInitUrl,
      '1_checksum_value': checksumInit,
      '2_init_pay': initUrl,
      '2_init_pay_actual_response': initBody,
      '3_calculate_check_sum_confirm': checksumConfirmUrl,
      '3_checksum_value': checksumConfirm,
      '4_confirm_pay': confirmUrl,
    },
    portalHints: {
      customerSearch: identifier,
      swagger_epay: 'http://10.236.20.11:9091/swagger-ui/index.html#',
    },
  };

  fs.mkdirSync(OUT_DIR, { recursive: true });
  const outPath = path.join(OUT_DIR, 'manual-verify-proforma-ready.json');
  fs.writeFileSync(outPath, JSON.stringify(out, null, 2), 'utf-8');
  console.log(JSON.stringify(out, null, 2));
  console.log(`\nWrote ${outPath}`);

  await apiCtx.dispose();
  await fileCtx.dispose();
}

main().catch((e) => {
  console.error(e);
  process.exit(1);
});
