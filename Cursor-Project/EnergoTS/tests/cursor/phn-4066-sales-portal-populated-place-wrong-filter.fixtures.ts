/**
 * PHN-4066 — Sales Portal GET /nomenclature/POPULATED_PLACES/filter helpers.
 *
 * SPRequest pitfall (do not edit fixtures/baseFixture.ts):
 *   SPRequest baseURL is http://10.236.20.11:7092/sales-portal/
 *   Relative nomenclature/... becomes /sales-portal/nomenclature/... → HTTP 404.
 * Playwright ignores that prefix when the request URL is absolute, while the
 * Sales Portal Bearer token from SPRequest still applies. Do not hardcode tokens.
 *
 * Ticket prompt "Варна" is system nomenclature (bug reproduce string), not a
 * hardcoded entity id. Confluence dump API is GET /nomenclature/{name} — tests
 * cannot create city Варна as a precondition (criterion-7 exception).
 *
 * Reference spec(s):
 * - tests/contractsAndOrders/productContract.spec.ts
 * - tests/cursor/PHN-4002-product-contract-sales-channel-filter.spec.ts
 */

import { expect } from '../../fixtures/baseFixture';
import type { baseFixture } from '../../fixtures/baseFixture';

export const PHN_4066_JIRA_KEY = 'PHN-4066';
export const PHN_4066_TITLE = 'Sales Portal API Populated place wrong filter';

/** Jira customfield_10103 reproduce prompt (populated-place own name, not an entity id). */
export const TICKET_PROMPT = 'Варна';

/** Reporter: exact match on the place’s own name (segment before " - "). */
export const EXPECTED_EXACT_PLACE_NAME = 'Варна';

/**
 * Known-wrong first-page row on current Dev2 when exactMatch is ignored
 * (Avren municipality under region Варна).
 */
export const AVREN_VILLAGE_EXAMPLE = 'БЕНКОВСКИ - АВРЕН - ВАРНА';

/**
 * Origin of the Sales Portal host used by SPRequest (same host as baseFixture).
 * Absolute filter URL avoids the /sales-portal/ path prefix.
 */
export const SALES_PORTAL_ORIGIN = 'http://10.236.20.11:7092';

export const POPULATED_PLACES_NOMENCLATURE = 'POPULATED_PLACES';

export const POPULATED_PLACES_FILTER_URL =
  `${SALES_PORTAL_ORIGIN}/nomenclature/${POPULATED_PLACES_NOMENCLATURE}/filter`;

export const SALES_PORTAL_SWAGGER_UI_URL = `${SALES_PORTAL_ORIGIN}/swagger-ui/index.html`;

export type Phn4066SpRequest = baseFixture['SPRequest'];

/** OpenAPI NomenclatureItemsBaseFilterRequest query used by filterNomenclature. */
export type Phn4066FilterParams = {
  statuses: string;
  prompt: string;
  page: number;
  size: number;
  exactMatch: boolean;
};

/** OpenAPI PageNomenclatureResponse (content items are NomenclatureResponse). */
export type NomenclatureFilterPage = {
  totalElements?: number;
  totalPages?: number;
  numberOfElements?: number;
  size?: number;
  number?: number;
  content?: Array<{
    id?: number;
    name?: string;
    status?: string;
    orderingId?: number;
  }>;
};

export function buildPopulatedPlaceFilterParams(exactMatch: boolean): Phn4066FilterParams {
  return {
    statuses: 'ACTIVE',
    prompt: TICKET_PROMPT,
    page: 0,
    size: 30,
    exactMatch,
  };
}

/**
 * Place’s own name: first segment before " - " in concatenated display name
 * (place - municipality - region - country).
 */
export function populatedPlaceOwnName(displayName: string): string {
  return String(displayName ?? '').split(' - ')[0]?.trim() ?? '';
}

export function namesEqualIgnoreCaseTrim(left: string, right: string): boolean {
  return left.trim().toLocaleLowerCase('bg') === right.trim().toLocaleLowerCase('bg');
}

export function firstPageNames(body: NomenclatureFilterPage): string[] {
  return (body.content ?? []).map((row) => String(row.name ?? ''));
}

export function readTotalElements(body: NomenclatureFilterPage): number {
  const total = Number(body.totalElements);
  return Number.isFinite(total) ? total : -1;
}

/** True when the first page still lists Avren-municipality towns (contains-match bug). */
export function firstPageContainsAvrenVillage(names: string[]): boolean {
  return names.some(
    (name) => name.includes(AVREN_VILLAGE_EXAMPLE) || name.includes(' - АВРЕН - '),
  );
}

export async function getPopulatedPlacesFilter(
  SPRequest: Phn4066SpRequest,
  exactMatch: boolean,
): Promise<{ status: number; body: NomenclatureFilterPage; params: Phn4066FilterParams }> {
  const params = buildPopulatedPlaceFilterParams(exactMatch);
  const response = await SPRequest.get(POPULATED_PLACES_FILTER_URL, { params });
  await expect(response).CheckResponse();
  const body = (await response.json()) as NomenclatureFilterPage;
  return { status: response.status(), body, params };
}
