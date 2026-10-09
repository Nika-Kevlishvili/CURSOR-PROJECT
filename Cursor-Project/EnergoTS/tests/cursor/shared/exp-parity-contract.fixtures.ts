/**
 * EXP-PARITY golden path — SIGNED COMBINED product contract.
 *
 * Match working volumes specs (REG-974 `combined.spec.ts`, `SLP.spec.ts`):
 * - POD is created as CONSUMER (stock `pod_settlement` / `pod_slp`).
 * - `estimatedMonthlyAvgConsumption` stays the template default (1).
 * - `product_contract()` sets `estimatedTotalConsumptionUnderContractKwh`
 *   = sum(CONSUMER monthly) * 12 / 1000 (same formula as Phoenix
 *   `processEstimatedTotalConsumption` / PDT-2931).
 * - Do not stamp `riskAssessment` on create — Phoenix overwrites it from SUK
 *   (`validateCustomerInRiskListAPI`). SUK query is `consumption` + customer
 *   name. High monthly (PDT-2931 max 99_999_999) → DENY; low (1) → typically
 *   PERMIT when the API is up. Stamping PERMIT does not skip the HTTP call.
 *
 * POST already SIGNED with `signingDate` on/before the billing window.
 * Phoenix `existsForActivation` requires header SIGNED +
 * `signingDate <= activationDate`. DRAFT create forces dates empty
 * (`ProductContractDateService`); hops to SIGNED leave `signingDate` null,
 * so past POD activation (`2026-08-01`) returns 400. SIGNED create requires
 * `signingDate` and allows past dates (future dates rejected). Same path as
 * REG-974. Do not PUT the contract after create (re-enters Risk List).
 *
 * No GENERATOR flip. PUT /pod after SIGNED is not part of the working specs.
 *
 * Swagger: `Cursor-Project/config/swagger/experiment/swagger-spec.json` (Rule 41).
 * `ProductContractBasicParametersCreateRequest.status` includes SIGNED;
 * `subStatus` includes SIGNED_BY_BOTH_SIDES; `signingDate` is date.
 * Reference spec(s):
 * - `tests/billing/forVolumes/combined.spec.ts` (REG-974)
 * - `tests/billing/forVolumes/SLP.spec.ts`
 * - `tests/cursor/pdt-2815-version-validity.fixtures.ts`
 *   (`existsForActivation` / `activationDateOnOrAfterSigning`)
 */

import { expect } from '../cursor-test.fixtures';

const PRODUCT_CONTRACT_ROOT = 'product-contract';
const EXP_PARITY_CONTRACT_TYPE = 'COMBINED' as const;
/** On/before EXP-PARITY billing windows; matches `productContract.ts` template. */
const EXP_PARITY_SIGNING_DATE = '2025-01-01';

type ProductContractCreatePayload = {
  basicParameters?: Record<string, unknown>;
  additionalParameters?: Record<string, unknown> | null;
  productParameters?: Record<string, unknown>;
};

type ExpParityRequest = {
  post: (url: string, options?: { data?: unknown }) => Promise<{ json: () => Promise<unknown> }>;
};

function entityId(raw: unknown): number {
  if (typeof raw === 'number' && Number.isFinite(raw)) {
    return raw;
  }
  if (raw && typeof raw === 'object' && 'id' in raw) {
    const id = Number((raw as { id?: unknown }).id);
    if (Number.isFinite(id) && id > 0) {
      return id;
    }
  }
  throw new Error(`product-contract create did not return an id: ${JSON.stringify(raw)}`);
}

/** Product catalog must allow COMBINED (volumes billing). */
export function applyExpParityCombinedProduct<T extends { contractTypes?: string[] }>(payload: T): T {
  payload.contractTypes = [EXP_PARITY_CONTRACT_TYPE];
  return payload;
}

/** Overlay SIGNED COMBINED + signingDate so POD activation can use past windows. */
export function applyExpParitySignedCombinedContract<T extends ProductContractCreatePayload>(
  payload: T,
): T {
  const basic = (payload.basicParameters ?? {}) as Record<string, unknown>;
  basic.status = 'SIGNED';
  basic.subStatus = 'SIGNED_BY_BOTH_SIDES';
  basic.versionStatus = 'SIGNED';
  basic.signingDate = EXP_PARITY_SIGNING_DATE;
  basic.entryInForceDate = null;
  basic.startOfInitialTerm = null;
  payload.basicParameters = basic;

  const product = (payload.productParameters ?? {}) as Record<string, unknown>;
  product.contractType = EXP_PARITY_CONTRACT_TYPE;
  payload.productParameters = product;

  return payload;
}

/** POST SIGNED COMBINED with signingDate (REG-974). POD stays CONSUMER. */
export async function postExpParitySignedProductContract<T extends ProductContractCreatePayload>(
  Request: ExpParityRequest,
  payload: T,
): Promise<unknown> {
  applyExpParitySignedCombinedContract(payload);
  const created = await Request.post(PRODUCT_CONTRACT_ROOT, { data: payload });
  await expect(created).CheckResponse();
  const raw = await created.json();
  entityId(raw);
  return raw;
}
