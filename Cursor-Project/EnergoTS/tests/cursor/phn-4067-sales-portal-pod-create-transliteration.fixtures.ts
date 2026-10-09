/**
 * PHN-4067 — Sales Portal POST /pod/create must not require *Trsl fields.
 *
 * SPRequest pitfall (do not edit fixtures/baseFixture.ts):
 *   SPRequest baseURL is http://10.236.20.11:7092/sales-portal/
 *   Relative pod/create becomes /sales-portal/pod/create → HTTP 404.
 * Playwright ignores that prefix when the request URL is absolute, while the
 * Sales Portal Bearer token from SPRequest still applies. Do not hardcode tokens.
 *
 * Criterion-7 exception (not a hardcoded entity id): prompts such as "ЕРП Север",
 * "H02", "София", "1000" are ticket/search strings for system nomenclature.
 * Sales Portal cannot create GRID_OPERATORS / MEASUREMENT_TYPE / address
 * nomenclatures as preconditions. Numeric ids are resolved at runtime via
 * GET /nomenclature/{name}/filter (206) + GET-by-id on Dev2 Sales Portal
 * http://10.236.20.11:7092. Never copy numeric address ids from the Jira curl.
 *
 * Jira ENV is TEST2. Numeric address ids in customfield_10103 (populatedPlaceId
 * 1694, zipCodeId 1664, districtId 1210, residentialAreaId 1476, streetId 112651,
 * regionId 1000 Варна, municipalityId 1040, identifier 32Z4000010002000) are
 * TEST2 — invalid for Dev2 POD create. Do not treat them as Dev or Dev2 ids.
 * gridOperatorId 1001 / measurementTypeId 1001 / countryId 1027 are not denylisted
 * (Dev2 GET may return the same numbers; they must still come from Dev2 GET).
 *
 * POPULATED_PLACES prompt "София" with exactMatch=false contains-matches region /
 * municipality (same PHN-4066 issue) and returns hundreds of Столична villages
 * (e.g. БАЛША) that have no zip 1000. Live Dev2: exactMatch=true returns the
 * city whose own name is СОФИЯ. Optional DISTRICT/STREET/RA only use page 0.
 *
 * Reference spec(s):
 * - tests/contractsAndOrders/productContract.spec.ts
 * - tests/cursor/PHN-4066-sales-portal-populated-place-wrong-filter.spec.ts
 * - tests/cursor/PHN-4002-product-contract-sales-channel-filter.spec.ts
 */

import { test, expect } from './cursor-test.fixtures';
import type { baseFixture } from '../../fixtures/baseFixture';

export const PHN_4067_JIRA_KEY = 'PHN-4067';
export const PHN_4067_TITLE = 'Sales Portal API Transliteration values required';

/**
 * Origin of the Sales Portal host used by SPRequest (same host as baseFixture).
 * Absolute URLs avoid the /sales-portal/ path prefix.
 */
export const SALES_PORTAL_ORIGIN = 'http://10.236.20.11:7092';

export const POD_CREATE_URL = `${SALES_PORTAL_ORIGIN}/pod/create`;
export const POD_BY_IDENTIFIER_URL = `${SALES_PORTAL_ORIGIN}/pod/by-identifier`;
export const SALES_PORTAL_SWAGGER_UI_URL = `${SALES_PORTAL_ORIGIN}/swagger-ui/index.html`;

export const TRANSLITERATION_REQUIRED_FRAGMENT = 'Transliteration field must be filled';

/** Ticket / search strings used on Dev2 filters — not TEST2 entity ids. */
export const GRID_OPERATOR_PROMPT = 'ЕРП Север';
export const MEASUREMENT_TYPE_PROMPT = 'H02';
export const POPULATED_PLACE_PROMPT = 'София';
export const ZIP_CODE_PROMPT = '1000';
export const PREFERRED_PLACE_OWN_NAME = 'СОФИЯ';
export const PREFERRED_MUNICIPALITY_HINT = 'СТОЛИЧНА';
export const PREFERRED_REGION_HINT = 'СОФИЯ';
export const PREFERRED_GRID_OPERATOR_CODE = '3';

export const TICKET_POD_NAME = 'ТО от бърз договор - API';
export const TICKET_ADDITIONAL_INFORMATION = 'НЯМА АСАНСЬОР';
export const TICKET_ENTRANCE = 'Б';
export const TICKET_NUMBER = '7';
export const TICKET_BLOCK = '62';
export const TICKET_FLOOR = '2';
export const TICKET_APARTMENT = '4';
export const TICKET_MAILBOX = '4';

/**
 * Address (and POD identifier) values from Jira customfield_10103.
 * These belong to TEST2 — forbidden as request ids on Dev2 Sales Portal.
 * Do not include gridOperatorId 1001, measurementTypeId 1001, or countryId 1027.
 */
export const TEST2_TICKET_ADDRESS_IDS = Object.freeze({
  populatedPlaceId: 1694,
  zipCodeId: 1664,
  districtId: 1210,
  residentialAreaId: 1476,
  streetId: 112651,
  regionId: 1000,
  municipalityId: 1040,
  identifier: '32Z4000010002000',
});

const FORBIDDEN_IDENTIFIERS = new Set([
  TEST2_TICKET_ADDRESS_IDS.identifier,
  '32Z4255642504607',
]);

const FILTER_PAGE_SIZE = 30;
const MAX_FILTER_PAGES = 10;
/** Optional DISTRICTS / STREETS / RESIDENTIAL_AREAS: page 0 only (prompt София explodes). */
const OPTIONAL_CHILD_FILTER_SIZE = 50;

export type Phn4067SpRequest = baseFixture['SPRequest'];

export type NomenclatureFilterParams = {
  statuses: string;
  prompt: string;
  page: number;
  size: number;
  /** OpenAPI NomenclatureItemsBaseFilterRequest.exactMatch (optional boolean). */
  exactMatch: boolean;
};

/** Live filter body is a page (PHN-4066); OpenAPI 206 $ref is a single NomenclatureResponse. */
export type NomenclatureFilterPage = {
  totalElements?: number;
  totalPages?: number;
  numberOfElements?: number;
  last?: boolean;
  size?: number;
  number?: number;
  content?: Array<{
    id?: number;
    name?: string;
    status?: string;
    orderingId?: number;
  }>;
};

export type GridOperatorDetail = {
  id?: number;
  name?: string;
  status?: string;
  gridOperatorCode?: string;
};

export type MeasurementTypeDetail = {
  id?: number;
  name?: string;
  status?: string;
  gridOperatorId?: number;
};

export type AddressNomenclatureDetail = {
  id?: number;
  name?: string;
  status?: string;
  populatedPlaceName?: string;
  municipalityId?: number;
  regionId?: number;
  countryId?: number;
  populatedPlaceId?: number;
  type?: string;
};

export type ResolvedAddressChain = {
  populatedPlaceId: number;
  populatedPlaceName: string;
  municipalityId: number;
  municipalityName: string;
  regionId: number;
  regionName: string;
  countryId: number;
  countryName: string;
};

export type OptionalStreetNomenclature = {
  districtId?: number;
  residentialAreaId?: number;
  residentialAreaType?: 'QUARTER' | 'RESIDENTIAL_AREA';
  streetId?: number;
  streetType?: 'STREET' | 'BOULEVARD';
};

export type PodCreatePayload = {
  identifier: string;
  gridOperatorId: number;
  systemSource: 'SALES_PORTAL';
  name: string;
  type: 'CONSUMER';
  estimatedMonthlyAvgConsumption: number;
  consumptionPurpose: 'HOUSEHOLD';
  voltageLevel: 'LOW';
  slp: true;
  measurementTypeId: number;
  addressRequest: {
    foreign: false;
    localAddressData: Record<string, unknown>;
    number: string;
    additionalInformation: string;
    block: string;
    entrance: string;
    floor: string;
    apartment: string;
    mailbox: string;
  };
};

export function nomenclatureFilterUrl(nomenclature: string): string {
  return `${SALES_PORTAL_ORIGIN}/nomenclature/${nomenclature}/filter`;
}

export function gridOperatorUrl(id: number): string {
  return `${SALES_PORTAL_ORIGIN}/nomenclature/product/grid-operator/${id}`;
}

export function measurementTypeUrl(id: number): string {
  return `${SALES_PORTAL_ORIGIN}/nomenclature/pod/measurement-type/${id}`;
}

export function populatedPlaceUrl(id: number): string {
  return `${SALES_PORTAL_ORIGIN}/nomenclature/address/populated-place/${id}`;
}

export function municipalityUrl(id: number): string {
  return `${SALES_PORTAL_ORIGIN}/nomenclature/address/municipality/${id}`;
}

export function regionUrl(id: number): string {
  return `${SALES_PORTAL_ORIGIN}/nomenclature/address/region/${id}`;
}

export function countryUrl(id: number): string {
  return `${SALES_PORTAL_ORIGIN}/nomenclature/address/country/${id}`;
}

export function zipCodeUrl(id: number): string {
  return `${SALES_PORTAL_ORIGIN}/nomenclature/address/zip-code/${id}`;
}

export function districtUrl(id: number): string {
  return `${SALES_PORTAL_ORIGIN}/nomenclature/address/district/${id}`;
}

export function residentialAreaUrl(id: number): string {
  return `${SALES_PORTAL_ORIGIN}/nomenclature/address/residential-area/${id}`;
}

export function streetUrl(id: number): string {
  return `${SALES_PORTAL_ORIGIN}/nomenclature/address/street/${id}`;
}

export function isActiveStatus(status: unknown): boolean {
  return String(status ?? '').toUpperCase() === 'ACTIVE';
}

export function readPositiveId(value: unknown): number | undefined {
  const id = Number(value);
  return Number.isFinite(id) && id > 0 ? id : undefined;
}

export function populatedPlaceOwnName(displayName: string): string {
  return String(displayName ?? '').split(' - ')[0]?.trim() ?? '';
}

export function namesEqualIgnoreCaseTrim(left: string, right: string): boolean {
  return left.trim().toLocaleLowerCase('bg') === right.trim().toLocaleLowerCase('bg');
}

export function collectKeysContainingTrsl(value: unknown, path = ''): string[] {
  if (value == null || typeof value !== 'object') {
    return [];
  }
  const found: string[] = [];
  if (Array.isArray(value)) {
    value.forEach((item, index) => {
      found.push(...collectKeysContainingTrsl(item, `${path}[${index}]`));
    });
    return found;
  }
  for (const [key, nested] of Object.entries(value as Record<string, unknown>)) {
    const next = path ? `${path}.${key}` : key;
    if (key.toLowerCase().includes('trsl')) {
      found.push(next);
    }
    found.push(...collectKeysContainingTrsl(nested, next));
  }
  return found;
}

export function bodyTextContainsTransliterationError(value: unknown): boolean {
  try {
    return JSON.stringify(value).includes(TRANSLITERATION_REQUIRED_FRAGMENT);
  } catch {
    return String(value).includes(TRANSLITERATION_REQUIRED_FRAGMENT);
  }
}

export type ResolvedAddressIdsForTest2Guard = {
  populatedPlaceId: number;
  zipCodeId: number;
  regionId: number;
  municipalityId: number;
  districtId?: number;
  residentialAreaId?: number;
  streetId?: number;
  identifier?: string;
};

/** Fail if a Dev2-resolved id accidentally equals a TEST2 ticket address id. */
export function assertResolvedAddressIdsAreNotTest2(resolved: ResolvedAddressIdsForTest2Guard): void {
  const numericPairs: Array<{ field: string; value?: number; forbidden: number }> = [
    {
      field: 'populatedPlaceId',
      value: resolved.populatedPlaceId,
      forbidden: TEST2_TICKET_ADDRESS_IDS.populatedPlaceId,
    },
    { field: 'zipCodeId', value: resolved.zipCodeId, forbidden: TEST2_TICKET_ADDRESS_IDS.zipCodeId },
    { field: 'regionId', value: resolved.regionId, forbidden: TEST2_TICKET_ADDRESS_IDS.regionId },
    {
      field: 'municipalityId',
      value: resolved.municipalityId,
      forbidden: TEST2_TICKET_ADDRESS_IDS.municipalityId,
    },
    { field: 'districtId', value: resolved.districtId, forbidden: TEST2_TICKET_ADDRESS_IDS.districtId },
    {
      field: 'residentialAreaId',
      value: resolved.residentialAreaId,
      forbidden: TEST2_TICKET_ADDRESS_IDS.residentialAreaId,
    },
    { field: 'streetId', value: resolved.streetId, forbidden: TEST2_TICKET_ADDRESS_IDS.streetId },
  ];
  for (const row of numericPairs) {
    if (row.value == null) {
      continue;
    }
    expect(
      row.value,
      `Resolved Dev2 ${row.field} must not be TEST2 ticket id ${row.forbidden}`,
    ).not.toBe(row.forbidden);
  }
  if (resolved.identifier != null) {
    expect(
      resolved.identifier,
      `Resolved Dev2 identifier must not be TEST2 ticket id ${TEST2_TICKET_ADDRESS_IDS.identifier}`,
    ).not.toBe(TEST2_TICKET_ADDRESS_IDS.identifier);
  }
}

function uniqueDigitTail(length: number): string {
  const stamp = Date.now().toString();
  const rand = Math.floor(Math.random() * 1_000_000).toString().padStart(6, '0');
  const digits = `${stamp}${rand}`;
  return digits.slice(-length).padStart(length, '0');
}

/**
 * Identifier rules from PointOfDeliveryService.validateGridOperator
 * (prefix match is case-insensitive; OpenAPI allows A-Z and 0-9, max 33).
 */
export function buildPodIdentifier(gridOperatorCode: string): string {
  const code = String(gridOperatorCode ?? '').trim();
  const build = (): string => {
    switch (code) {
      case '1':
        return `32Z1${uniqueDigitTail(12)}`;
      case '3':
        return `32Z4${uniqueDigitTail(12)}`;
      case '4':
        return `32Z5${uniqueDigitTail(12)}`;
      case '0':
        return `32X${uniqueDigitTail(13)}`;
      case '2':
        return `EVN${uniqueDigitTail(7)}`;
      default:
        throw new Error(
          `Unsupported gridOperatorCode "${code}" — cannot build a POD identifier ` +
            '(expected 0, 1, 2, 3, or 4 from GET /nomenclature/product/grid-operator/{id}).',
        );
    }
  };

  let identifier = build();
  if (FORBIDDEN_IDENTIFIERS.has(identifier) || !/^[A-Z0-9]+$/.test(identifier)) {
    identifier = build();
  }
  if (FORBIDDEN_IDENTIFIERS.has(identifier)) {
    throw new Error('Generated a forbidden ticket identifier — retry the test.');
  }
  assertIdentifierMatchesGridOperatorCode(identifier, code);
  return identifier;
}

export function assertIdentifierMatchesGridOperatorCode(identifier: string, code: string): void {
  const lowered = identifier.toLowerCase();
  const ok = (() => {
    switch (String(code).trim()) {
      case '0':
        return lowered.startsWith('32x') || lowered.startsWith('tso');
      case '1':
        return lowered.startsWith('32z1') && identifier.length === 16;
      case '2':
        return (lowered.startsWith('evn') && identifier.length === 10) ||
          (lowered.startsWith('bg') && identifier.length === 33);
      case '3':
        return lowered.startsWith('32z4') && identifier.length === 16;
      case '4':
        return lowered.startsWith('32z5') && identifier.length === 16;
      default:
        return false;
    }
  })();
  if (!ok) {
    throw new Error(`Identifier "${identifier}" does not match gridOperatorCode "${code}".`);
  }
}

export async function filterNomenclature(
  SPRequest: Phn4067SpRequest,
  nomenclature: string,
  prompt: string,
  page = 0,
  size = FILTER_PAGE_SIZE,
  exactMatch = false,
): Promise<{ status: number; body: NomenclatureFilterPage; params: NomenclatureFilterParams }> {
  if (!prompt.trim()) {
    throw new Error(`Filter prompt for ${nomenclature} must be non-empty (empty prompt returns HTTP 400).`);
  }
  const params: NomenclatureFilterParams = {
    statuses: 'ACTIVE',
    prompt,
    page,
    size,
    exactMatch,
  };
  return test.step(
    `GET /nomenclature/${nomenclature}/filter prompt=${prompt} exactMatch=${exactMatch} page=${page}`,
    async () => {
      const response = await SPRequest.get(nomenclatureFilterUrl(nomenclature), { params });
      await expect(response).CheckResponse();
      const body = (await response.json()) as NomenclatureFilterPage;
      return { status: response.status(), body, params };
    },
  );
}

export async function collectFilterRows(
  SPRequest: Phn4067SpRequest,
  nomenclature: string,
  prompt: string,
  options?: { exactMatch?: boolean; maxPages?: number; size?: number },
): Promise<{ rows: NonNullable<NomenclatureFilterPage['content']>; firstParams: NomenclatureFilterParams }> {
  const exactMatch = options?.exactMatch ?? false;
  const maxPages = options?.maxPages ?? MAX_FILTER_PAGES;
  const size = options?.size ?? FILTER_PAGE_SIZE;
  const rows: NonNullable<NomenclatureFilterPage['content']> = [];
  let firstParams: NomenclatureFilterParams | undefined;
  for (let page = 0; page < maxPages; page += 1) {
    const result = await filterNomenclature(SPRequest, nomenclature, prompt, page, size, exactMatch);
    if (!firstParams) {
      firstParams = result.params;
    }
    const pageRows = result.body.content ?? [];
    rows.push(...pageRows);
    const totalPages = Number(result.body.totalPages);
    const isLast =
      result.body.last === true ||
      pageRows.length === 0 ||
      (Number.isFinite(totalPages) && totalPages > 0 && page + 1 >= totalPages);
    if (isLast) {
      break;
    }
  }
  if (!firstParams) {
    throw new Error(`No filter response for ${nomenclature} prompt=${prompt}`);
  }
  return { rows, firstParams };
}

async function getJsonById<T extends object>(
  SPRequest: Phn4067SpRequest,
  url: string,
  label: string,
): Promise<T> {
  return test.step(`GET ${label}`, async () => {
    const response = await SPRequest.get(url);
    await expect(response).CheckResponse();
    return (await response.json()) as T;
  });
}

export async function resolveGridOperator(
  SPRequest: Phn4067SpRequest,
): Promise<{ detail: GridOperatorDetail; filterParams: NomenclatureFilterParams }> {
  const { rows, firstParams } = await collectFilterRows(SPRequest, 'GRID_OPERATORS', GRID_OPERATOR_PROMPT);
  const active: GridOperatorDetail[] = [];
  for (const row of rows) {
    const id = readPositiveId(row.id);
    if (!id) {
      continue;
    }
    const detail = await getJsonById<GridOperatorDetail>(
      SPRequest,
      gridOperatorUrl(id),
      `/nomenclature/product/grid-operator/${id}`,
    );
    if (isActiveStatus(detail.status) && readPositiveId(detail.id)) {
      active.push(detail);
    }
  }
  const preferred =
    active.find((item) => String(item.gridOperatorCode ?? '').trim() === PREFERRED_GRID_OPERATOR_CODE) ??
    active[0];
  if (!preferred || !readPositiveId(preferred.id) || !String(preferred.gridOperatorCode ?? '').trim()) {
    throw new Error(
      `No ACTIVE GRID_OPERATORS for prompt "${GRID_OPERATOR_PROMPT}" with a gridOperatorCode ` +
        `(rows=${rows.length}, active=${active.length}).`,
    );
  }
  return { detail: preferred, filterParams: firstParams };
}

export async function resolveMeasurementType(
  SPRequest: Phn4067SpRequest,
  gridOperatorId: number,
): Promise<{ detail: MeasurementTypeDetail; filterParams: NomenclatureFilterParams }> {
  const { rows, firstParams } = await collectFilterRows(SPRequest, 'MEASUREMENT_TYPE', MEASUREMENT_TYPE_PROMPT);
  for (const row of rows) {
    const id = readPositiveId(row.id);
    if (!id) {
      continue;
    }
    const detail = await getJsonById<MeasurementTypeDetail>(
      SPRequest,
      measurementTypeUrl(id),
      `/nomenclature/pod/measurement-type/${id}`,
    );
    if (isActiveStatus(detail.status) && Number(detail.gridOperatorId) === gridOperatorId && readPositiveId(detail.id)) {
      return { detail, filterParams: firstParams };
    }
  }
  throw new Error(
    `No ACTIVE MEASUREMENT_TYPE for prompt "${MEASUREMENT_TYPE_PROMPT}" with gridOperatorId=${gridOperatorId} ` +
      `(filter rows=${rows.length}).`,
  );
}

function scoreSofiaChain(placeName: string, municipalityName: string, regionName: string): number {
  let score = 0;
  const own = populatedPlaceOwnName(placeName);
  if (namesEqualIgnoreCaseTrim(own, PREFERRED_PLACE_OWN_NAME)) {
    score += 100;
  }
  if (municipalityName.toLocaleUpperCase('bg').includes(PREFERRED_MUNICIPALITY_HINT)) {
    score += 20;
  }
  if (regionName.toLocaleUpperCase('bg').includes(PREFERRED_REGION_HINT)) {
    score += 10;
  }
  return score;
}

/**
 * City София only: exactMatch=true so we do not page hundreds of Столична villages
 * (БАЛША etc.) that share municipality/region contains-match and have no zip 1000.
 */
export async function resolveSofiaAddressChain(
  SPRequest: Phn4067SpRequest,
): Promise<{ chain: ResolvedAddressChain; filterParams: NomenclatureFilterParams }> {
  const filter = await filterNomenclature(
    SPRequest,
    'POPULATED_PLACES',
    POPULATED_PLACE_PROMPT,
    0,
    FILTER_PAGE_SIZE,
    true,
  );
  const rows = filter.body.content ?? [];
  const preferredFirst = [...rows].sort((left, right) => {
    const leftOwn = namesEqualIgnoreCaseTrim(populatedPlaceOwnName(String(left.name ?? '')), PREFERRED_PLACE_OWN_NAME);
    const rightOwn = namesEqualIgnoreCaseTrim(populatedPlaceOwnName(String(right.name ?? '')), PREFERRED_PLACE_OWN_NAME);
    if (leftOwn === rightOwn) {
      return 0;
    }
    return leftOwn ? -1 : 1;
  });

  let best: { chain: ResolvedAddressChain; score: number } | undefined;
  for (const row of preferredFirst) {
    const placeId = readPositiveId(row.id);
    if (!placeId) {
      continue;
    }
    const place = await getJsonById<AddressNomenclatureDetail>(
      SPRequest,
      populatedPlaceUrl(placeId),
      `/nomenclature/address/populated-place/${placeId}`,
    );
    const municipalityId = readPositiveId(place.municipalityId);
    if (!isActiveStatus(place.status) || !municipalityId) {
      continue;
    }
    const municipality = await getJsonById<AddressNomenclatureDetail>(
      SPRequest,
      municipalityUrl(municipalityId),
      `/nomenclature/address/municipality/${municipalityId}`,
    );
    const regionId = readPositiveId(municipality.regionId);
    if (!isActiveStatus(municipality.status) || !regionId) {
      continue;
    }
    const region = await getJsonById<AddressNomenclatureDetail>(
      SPRequest,
      regionUrl(regionId),
      `/nomenclature/address/region/${regionId}`,
    );
    const countryId = readPositiveId(region.countryId);
    if (!isActiveStatus(region.status) || !countryId) {
      continue;
    }
    const country = await getJsonById<AddressNomenclatureDetail>(
      SPRequest,
      countryUrl(countryId),
      `/nomenclature/address/country/${countryId}`,
    );
    if (!isActiveStatus(country.status)) {
      continue;
    }
    const displayName = String(place.populatedPlaceName || place.name || row.name || '');
    const chain: ResolvedAddressChain = {
      populatedPlaceId: placeId,
      populatedPlaceName: displayName,
      municipalityId,
      municipalityName: String(municipality.name ?? ''),
      regionId,
      regionName: String(region.name ?? ''),
      countryId,
      countryName: String(country.name ?? ''),
    };
    const score = scoreSofiaChain(chain.populatedPlaceName, chain.municipalityName, chain.regionName);
    if (!best || score > best.score) {
      best = { chain, score };
    }
    if (namesEqualIgnoreCaseTrim(populatedPlaceOwnName(displayName), PREFERRED_PLACE_OWN_NAME) && score >= 100) {
      return { chain, filterParams: filter.params };
    }
  }
  if (!best) {
    throw new Error(
      `No POPULATED_PLACES for prompt "${POPULATED_PLACE_PROMPT}" exactMatch=true with ACTIVE place + ` +
        `municipality + region + country (do not use TEST2 ticket regionId ${TEST2_TICKET_ADDRESS_IDS.regionId} / Варна; ` +
        `avoid Столична villages ` +
        `such as БАЛША that have no zip ${ZIP_CODE_PROMPT}). Filter rows=${rows.length}.`,
    );
  }
  return { chain: best.chain, filterParams: filter.params };
}

export async function resolveZipCodeForPlace(
  SPRequest: Phn4067SpRequest,
  populatedPlaceId: number,
  populatedPlaceName: string,
): Promise<{ zipCodeId: number; name: string; filterParams: NomenclatureFilterParams }> {
  const { rows, firstParams } = await collectFilterRows(SPRequest, 'ZIP_CODES', ZIP_CODE_PROMPT, {
    exactMatch: false,
    maxPages: 1,
  });
  const inspected: string[] = [];
  for (const row of rows) {
    const id = readPositiveId(row.id);
    if (!id) {
      continue;
    }
    const zip = await getJsonById<AddressNomenclatureDetail>(
      SPRequest,
      zipCodeUrl(id),
      `/nomenclature/address/zip-code/${id}`,
    );
    inspected.push(
      `zipId=${zip.id ?? id} status=${zip.status ?? '?'} populatedPlaceId=${zip.populatedPlaceId ?? 'n/a'} name=${zip.name ?? row.name ?? ''}`,
    );
    if (
      isActiveStatus(zip.status) &&
      Number(zip.populatedPlaceId) === populatedPlaceId &&
      readPositiveId(zip.id)
    ) {
      return { zipCodeId: Number(zip.id), name: String(zip.name ?? row.name ?? ''), filterParams: firstParams };
    }
  }
  throw new Error(
    `No ACTIVE ZIP_CODES for prompt "${ZIP_CODE_PROMPT}" belonging to populatedPlaceId=${populatedPlaceId} ` +
      `(${populatedPlaceName}). Zip is required by assignLocalAddressData. Inspected: ${inspected.join(' | ') || 'none'}.`,
  );
}

async function firstMatchingChildNomenclature(
  SPRequest: Phn4067SpRequest,
  nomenclature: string,
  prompt: string,
  populatedPlaceId: number,
  urlForId: (id: number) => string,
  pathLabel: string,
): Promise<AddressNomenclatureDetail | undefined> {
  const filter = await filterNomenclature(
    SPRequest,
    nomenclature,
    prompt,
    0,
    OPTIONAL_CHILD_FILTER_SIZE,
    false,
  );
  for (const row of filter.body.content ?? []) {
    const id = readPositiveId(row.id);
    if (!id) {
      continue;
    }
    const detail = await getJsonById<AddressNomenclatureDetail>(
      SPRequest,
      urlForId(id),
      `${pathLabel}/${id}`,
    );
    if (isActiveStatus(detail.status) && Number(detail.populatedPlaceId) === populatedPlaceId && readPositiveId(detail.id)) {
      return detail;
    }
  }
  return undefined;
}

export async function resolveOptionalStreetNomenclature(
  SPRequest: Phn4067SpRequest,
  populatedPlaceId: number,
): Promise<OptionalStreetNomenclature> {
  const optional: OptionalStreetNomenclature = {};
  const district = await firstMatchingChildNomenclature(
    SPRequest,
    'DISTRICTS',
    POPULATED_PLACE_PROMPT,
    populatedPlaceId,
    districtUrl,
    '/nomenclature/address/district',
  );
  if (district?.id) {
    optional.districtId = Number(district.id);
  }
  const residentialArea = await firstMatchingChildNomenclature(
    SPRequest,
    'RESIDENTIAL_AREAS',
    POPULATED_PLACE_PROMPT,
    populatedPlaceId,
    residentialAreaUrl,
    '/nomenclature/address/residential-area',
  );
  if (residentialArea?.id) {
    optional.residentialAreaId = Number(residentialArea.id);
    if (residentialArea.type === 'QUARTER' || residentialArea.type === 'RESIDENTIAL_AREA') {
      optional.residentialAreaType = residentialArea.type;
    }
  }
  const street = await firstMatchingChildNomenclature(
    SPRequest,
    'STREETS',
    POPULATED_PLACE_PROMPT,
    populatedPlaceId,
    streetUrl,
    '/nomenclature/address/street',
  );
  if (street?.id) {
    optional.streetId = Number(street.id);
    if (street.type === 'STREET' || street.type === 'BOULEVARD') {
      optional.streetType = street.type;
    }
  }
  return optional;
}

export function buildPodCreatePayloadWithoutTrsl(input: {
  identifier: string;
  gridOperatorId: number;
  measurementTypeId: number;
  chain: ResolvedAddressChain;
  zipCodeId: number;
  optional: OptionalStreetNomenclature;
}): PodCreatePayload {
  const localAddressData: Record<string, unknown> = {
    countryId: input.chain.countryId,
    regionId: input.chain.regionId,
    municipalityId: input.chain.municipalityId,
    populatedPlaceId: input.chain.populatedPlaceId,
    zipCodeId: input.zipCodeId,
  };
  if (input.optional.districtId) {
    localAddressData.districtId = input.optional.districtId;
  }
  if (input.optional.residentialAreaId) {
    localAddressData.residentialAreaId = input.optional.residentialAreaId;
  }
  if (input.optional.residentialAreaType) {
    localAddressData.residentialAreaType = input.optional.residentialAreaType;
  }
  if (input.optional.streetId) {
    localAddressData.streetId = input.optional.streetId;
  }
  if (input.optional.streetType) {
    localAddressData.streetType = input.optional.streetType;
  }

  return {
    identifier: input.identifier,
    gridOperatorId: input.gridOperatorId,
    systemSource: 'SALES_PORTAL',
    name: TICKET_POD_NAME,
    type: 'CONSUMER',
    estimatedMonthlyAvgConsumption: 100,
    consumptionPurpose: 'HOUSEHOLD',
    voltageLevel: 'LOW',
    slp: true,
    measurementTypeId: input.measurementTypeId,
    addressRequest: {
      foreign: false,
      localAddressData,
      number: TICKET_NUMBER,
      additionalInformation: TICKET_ADDITIONAL_INFORMATION,
      block: TICKET_BLOCK,
      entrance: TICKET_ENTRANCE,
      floor: TICKET_FLOOR,
      apartment: TICKET_APARTMENT,
      mailbox: TICKET_MAILBOX,
    },
  };
}
