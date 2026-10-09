import { request as playwrightRequest } from '@playwright/test';
import type { APIRequestContext, APIResponse } from '@playwright/test';
import * as dotenv from 'dotenv';
import * as path from 'path';
import { test as baseTest, expect } from './baseFixture';
import type { baseFixture } from './baseFixture';

dotenv.config({ path: path.resolve(__dirname, '../../../.env') });

export { expect };

/** Bulgaria Post auth request — PascalCase keys per payment-api contract. */
export type BulgarianPostCredentials = {
  UserName: string;
  UserPassword: string;
  PostCode: string;
};

export type BulgarianPostApiBody = {
  ErrorCode: number;
  ErrorMessage?: string;
  Token?: string;
  Results?: unknown[];
  [key: string]: unknown;
};

export const BULGARIAN_POST_CHANNEL_NAME = 'BulgarianPost';

export function loadBulgarianPostCredentials(): BulgarianPostCredentials {
  const UserName =
    process.env.BULGARIAPOST_USERNAME?.trim() || process.env.BG_USER_NAME?.trim();
  const UserPassword =
    process.env.BULGARIAPOST_PASSWORD?.trim() || process.env.BG_USER_PASSWORD?.trim();
  const PostCode =
    process.env.BULGARIAPOST_POST_CODE?.trim() || process.env.BG_USER_POSTCODE?.trim();

  if (!UserName || !UserPassword || !PostCode) {
    throw new Error(
      'Missing Bulgarian Post credentials in Cursor-Project/EnergoTS/.env. ' +
        'Set BULGARIAPOST_USERNAME, BULGARIAPOST_PASSWORD, BULGARIAPOST_POST_CODE ' +
        '(or BG_USER_NAME, BG_USER_PASSWORD, BG_USER_POSTCODE).',
    );
  }

  return { UserName, UserPassword, PostCode };
}

export async function issueBulgarianPostToken(
  context: APIRequestContext,
  creds: BulgarianPostCredentials,
): Promise<string> {
  const response = await context.post('bulgarian-post/auth', {
    data: creds,
    headers: {
      Accept: 'application/json',
      'Content-Type': 'application/json',
    },
  });

  const body = (await response.json()) as BulgarianPostApiBody;
  if (body.ErrorCode !== 0 || !body.Token) {
    throw new Error(
      `Bulgarian Post auth failed (HTTP ${response.status()}): ErrorCode=${body.ErrorCode}, ErrorMessage=${body.ErrorMessage ?? ''}`,
    );
  }

  return body.Token;
}

/**
 * Payment-api client: issues a fresh one-time token before every protected call.
 * Tokens are consumed on first use (Redis) — never reuse across requests.
 */
export class BpClient {
  constructor(
    private readonly context: APIRequestContext,
    private readonly creds: BulgarianPostCredentials,
  ) {}

  private async authorizedGet(relativePath: string): Promise<APIResponse> {
    const token = await issueBulgarianPostToken(this.context, this.creds);
    return this.context.get(relativePath, {
      headers: {
        Accept: 'application/json',
        Authorization: `Bearer ${token}`,
      },
    });
  }

  async getObligations(customerNumber: string): Promise<APIResponse> {
    return this.authorizedGet(`bulgarian-post/obligations/${encodeURIComponent(customerNumber)}`);
  }

  async getReceipt(params: Record<string, string | number>): Promise<APIResponse> {
    const query = new URLSearchParams();
    for (const [key, value] of Object.entries(params)) {
      query.set(key, String(value));
    }
    return this.authorizedGet(`bulgarian-post/receipt?${query.toString()}`);
  }

  async pay(tid: string): Promise<APIResponse> {
    return this.authorizedGet(`bulgarian-post/pay/${encodeURIComponent(tid)}`);
  }

  async getTest(): Promise<APIResponse> {
    return this.authorizedGet('bulgarian-post/test');
  }

  /** Smoke helper — no Bearer header (expect HTTP 401 when auth filter is active). */
  async getTestWithoutAuth(): Promise<APIResponse> {
    return this.context.get('bulgarian-post/test', {
      headers: { Accept: 'application/json' },
    });
  }
}

export async function parseBulgarianPostJson(response: APIResponse): Promise<BulgarianPostApiBody> {
  return (await response.json()) as BulgarianPostApiBody;
}

/** Assert payment-api HTTP success and business ErrorCode === 0. */
export async function expectBulgarianPostSuccess(response: APIResponse): Promise<BulgarianPostApiBody> {
  expect(response.status(), 'Bulgarian Post HTTP status').toBe(200);
  const body = await parseBulgarianPostJson(response);
  expect(body.ErrorCode, `Bulgarian Post ErrorMessage=${body.ErrorMessage ?? ''}`).toBe(0);
  return body;
}

/** Require a pre-seeded ONLINE collection channel named `BulgarianPost` (payment-api hardcodes this name). */
export async function findBulgarianPostCollectionChannel(Request: baseFixture['Request'], Endpoints: baseFixture['Endpoints']): Promise<{ id: number; body: Record<string, unknown> }> {

  const listing = await Request.get(`${Endpoints.collectionChannel}?page=0&size=25&searchBy=NAME&prompt=${BULGARIAN_POST_CHANNEL_NAME}&collectionChannelType=ONLINE`);
  await expect(listing).CheckResponse();
  const listBody = (await listing.json()) as { content?: Array<{ id: number }> };
  const first = listBody.content?.[0];

  if (!first?.id) {
    throw new Error(
      `Bulgarian Post precondition failed: ONLINE collection channel "${BULGARIAN_POST_CHANNEL_NAME}" was not found. ` +
        'Seed or configure this channel in the target environment before running Bulgarian Post tests.',
    );
  }

  const detail = await Request.get(`${Endpoints.collectionChannel}/${first.id}`);
  await expect(detail).CheckResponse();
  return { id: first.id, body: (await detail.json()) as Record<string, unknown> };
}

export async function resolveChannelExchangeRate(
  Request: baseFixture['Request'],
  Endpoints: baseFixture['Endpoints'],
  channelBody: Record<string, unknown>,
): Promise<number> {

  //get currency id
  const rawCurrencyId = channelBody.currencyId;
  let currencyId: number | undefined;
  if (typeof rawCurrencyId === 'number') {
    currencyId = rawCurrencyId;
  } else if (rawCurrencyId && typeof rawCurrencyId === 'object' && 'id' in rawCurrencyId) {
    currencyId = Number((rawCurrencyId as { id: number }).id);
  }

  if (currencyId == null || !Number.isFinite(currencyId)) {
    throw new Error('BulgarianPost collection channel has no currencyId');
  }

  //get exchange rate for this currency
  const currencyGet = await Request.get(`currencies/${currencyId}`);
  await expect(currencyGet).CheckResponse();
  const currency = (await currencyGet.json()) as { altCurrencyExchangeRate?: number; name?: string; abbreviation?: string };

  const exchange = currency.altCurrencyExchangeRate;
  if (exchange == null || exchange <= 0) {
    throw new Error(`Currency id=${currencyId} has no valid altCurrencyExchangeRate for receipt`);
  }

  return exchange;
}

export function coerceMoney(value: unknown): number {
  if (typeof value === 'number') {
    return Number.isFinite(value) ? value : 0;
  }

  if (typeof value === 'string') {
    const normalized = value.replace(/[^0-9.,-]/g, '').replace(/,/g, '.');
    const parsed = Number(normalized);
    return Number.isFinite(parsed) ? parsed : 0;
  }

  return 0;
}

export async function resolveChannelCurrencyMeta(
  Request: baseFixture['Request'],
  Endpoints: baseFixture['Endpoints'],
  channelBody: Record<string, unknown>,
): Promise<{ currencyName: string; exchangeRate: number; }> {
  const rawCurrencyId = channelBody.currencyId;
  let currencyId: number | undefined;
  if (typeof rawCurrencyId === 'number') {
    currencyId = rawCurrencyId;
  } else if (rawCurrencyId && typeof rawCurrencyId === 'object' && 'id' in rawCurrencyId) {
    currencyId = Number((rawCurrencyId as { id: number }).id);
  }

  if (currencyId == null || !Number.isFinite(currencyId)) {
    throw new Error('BulgarianPost collection channel has no currencyId');
  }

  const currencyGet = await Request.get(`currencies/${currencyId}`);
  await expect(currencyGet).CheckResponse();
  const currency = (await currencyGet.json()) as { altCurrencyExchangeRate?: number; name?: string; defaultSelection?: boolean };

  const exchange = currency.altCurrencyExchangeRate;
  if (exchange == null || exchange <= 0) {
    throw new Error(`Currency id=${currencyId} has no valid altCurrencyExchangeRate for receipt`);
  }

  return {
    currencyName: currency.name ?? '',
    exchangeRate: exchange
  };
}

export function expectMoney2(actual: unknown, expected: unknown, message?: string): void {
  expect(coerceMoney(actual)).toBeCloseTo(coerceMoney(expected), 2);
}

export type extendedPaymentFixture = baseFixture & {
  BpClient: BpClient;
  BulgarianPostCredentials: BulgarianPostCredentials;
};

export const test = baseTest.extend<{
  BpClient: BpClient;
  BulgarianPostCredentials: BulgarianPostCredentials;
}>({
  BulgarianPostCredentials: async ({}, use) => {
    await use(loadBulgarianPostCredentials());
  },

  BpClient: async ({ OnlinePaymentUrl, BulgarianPostCredentials }, use) => {
    const baseURL = OnlinePaymentUrl.replace(/\/?$/, '/');
    const context = await playwrightRequest.newContext({
      baseURL,
      extraHTTPHeaders: { Accept: 'application/json' },
    });
    const client = new BpClient(context, BulgarianPostCredentials);
    await use(client);
    await context.dispose();
  },
});
