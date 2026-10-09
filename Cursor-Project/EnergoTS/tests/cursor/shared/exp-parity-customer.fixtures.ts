/**
 * EXP-PARITY legal-customer + POD address — consistent local nomenclature chain.
 *
 * `customer_legal()` / `pod_settlement()` stamp independent `envVariables` ids
 * (`countries` vs `population_places` vs `streets` / `zip_codes`). Experiment
 * rejects mixed trees (`Populated place is not in entered country`,
 * `Street not found in entered populated place`).
 *
 * Creates a live ACTIVE chain the same way as
 * `tests/cursor/PHN-2865-manager-phones-emails.spec.ts` and
 * `tests/cursor/CONFLUENCE-838795267-Put-update-customer-private-no-BA.spec.ts`.
 *
 * Swagger (`Cursor-Project/config/swagger/dev2/swagger-spec.json`):
 * - Address nomenclatures require `nameTransliterated` (CountryRequest, RegionRequest,
 *   MunicipalityRequest, PopulatedPlaceRequest, ZipCodeRequest maxLength 32,
 *   DistrictRequest, ResidentialAreaRequest, StreetsRequest).
 * - `POST /customer` → `CreateCustomerRequest.addressTransl` and
 *   `communicationData[].addressTransliterated` (uppercase strings when the source field is filled).
 * - `POST /pod` → `addressRequest.localAddressData.*Trsl` paired with each filled source id.
 *
 * Reference spec(s):
 * - tests/cursor/PHN-2865-manager-phones-emails.spec.ts
 * - tests/cursor/CONFLUENCE-838795267-Put-update-customer-private-no-BA.spec.ts
 */

import { expect } from '../cursor-test.fixtures';
import type { baseFixture } from '../cursor-test.fixtures';

export type ExpParityLocalAddressIds = {
  countryId: number;
  regionId: number;
  municipalityId: number;
  populatedPlaceId: number;
  zipCodeId: number;
  districtId: number;
  residentialAreaId: number;
  streetId: number;
};

export type ExpParityCustomerFx = Pick<
  baseFixture,
  'Request' | 'GeneratePayload' | 'Responses' | 'Endpoints'
>;

function asEntityId(raw: unknown): number {
  if (typeof raw === 'number' && Number.isFinite(raw) && raw > 0) {
    return raw;
  }
  if (raw && typeof raw === 'object' && 'id' in raw) {
    const value = Number((raw as { id?: unknown }).id);
    if (Number.isFinite(value) && value > 0) {
      return value;
    }
  }
  throw new Error(`Unable to read an entity id from ${JSON.stringify(raw)}`);
}

/** Uppercase Latin labels. Dev2 rejects blank or non-uppercase transliteration strings. */
const LOCAL_ADDRESS_TRANSLATION = {
  country: 'STANDARD COUNTRY',
  region: 'STANDARD REGION',
  municipality: 'STANDARD MUNICIPALITY',
  populatedPlace: 'STANDARD POPULATED PLACE',
  zipCode: 'STANDARD ZIP CODE',
  district: 'STANDARD DISTRICT',
  residentialArea: 'STANDARD RESIDENTIAL AREA',
  street: 'STANDARD STREET',
} as const;

function stampLocalAddressData(
  local: Record<string, unknown> | undefined,
  ids: ExpParityLocalAddressIds,
): void {
  if (!local) {
    return;
  }
  local.countryId = ids.countryId;
  local.regionId = ids.regionId;
  local.municipalityId = ids.municipalityId;
  local.populatedPlaceId = ids.populatedPlaceId;
  local.zipCodeId = ids.zipCodeId;
  local.districtId = ids.districtId;
  local.residentialAreaId = ids.residentialAreaId;
  local.streetId = ids.streetId;
  local.streetType = 'STREET';
  local.residentialAreaType = 'QUARTER';
}

function uppercaseFilled(value: unknown): string | undefined {
  if (typeof value !== 'string' || value.trim() === '') {
    return undefined;
  }
  return value.toUpperCase();
}

/** Customer `addressTransl.localAddressData`: strings, only for filled source ids. */
function customerTransliteratedLocal(local: Record<string, unknown> | undefined): Record<string, string> {
  const src = local ?? {};
  const transl: Record<string, string> = {};
  const pairs: Array<[keyof typeof LOCAL_ADDRESS_TRANSLATION, string]> = [
    ['country', 'countryId'],
    ['region', 'regionId'],
    ['municipality', 'municipalityId'],
    ['populatedPlace', 'populatedPlaceId'],
    ['zipCode', 'zipCodeId'],
    ['district', 'districtId'],
    ['residentialArea', 'residentialAreaId'],
    ['street', 'streetId'],
  ];
  for (const [labelKey, idKey] of pairs) {
    if (src[idKey] != null) {
      transl[labelKey] = LOCAL_ADDRESS_TRANSLATION[labelKey];
    }
  }
  if (typeof src.streetType === 'string' && src.streetType.trim() !== '') {
    transl.streetType = src.streetType;
  }
  if (typeof src.residentialAreaType === 'string' && src.residentialAreaType.trim() !== '') {
    transl.residentialAreaType = src.residentialAreaType;
  }
  return transl;
}

function attachCustomerAddressTransliteration(payload: Record<string, unknown>): void {
  const address = payload.address;
  if (address && typeof address === 'object') {
    const addr = address as Record<string, unknown>;
    const transl: Record<string, unknown> = {
      foreign: addr.foreign === true,
      localAddressData: customerTransliteratedLocal(
        addr.localAddressData as Record<string, unknown> | undefined,
      ),
    };
    const number = uppercaseFilled(addr.number);
    if (number) {
      transl.number = number;
    }
    payload.addressTransl = transl;
  }
  if (!Array.isArray(payload.communicationData)) {
    return;
  }
  for (const row of payload.communicationData) {
    if (!row || typeof row !== 'object') {
      continue;
    }
    const entry = row as { address?: Record<string, unknown>; addressTransliterated?: unknown };
    if (!entry.address || entry.addressTransliterated != null) {
      continue;
    }
    const transl: Record<string, unknown> = {
      foreign: entry.address.foreign === true,
      localAddressData: customerTransliteratedLocal(
        entry.address.localAddressData as Record<string, unknown> | undefined,
      ),
    };
    const number = uppercaseFilled(entry.address.number);
    if (number) {
      transl.number = number;
    }
    entry.addressTransliterated = transl;
  }
}

/** POD `*Trsl` must be filled exactly when the matching source field is filled. */
function stampPodLocalTrsl(local: Record<string, unknown>): void {
  const pairs: Array<[string, keyof typeof LOCAL_ADDRESS_TRANSLATION]> = [
    ['countryId', 'country'],
    ['regionId', 'region'],
    ['municipalityId', 'municipality'],
    ['populatedPlaceId', 'populatedPlace'],
    ['zipCodeId', 'zipCode'],
    ['districtId', 'district'],
    ['residentialAreaId', 'residentialArea'],
    ['streetId', 'street'],
  ];
  for (const [idKey, labelKey] of pairs) {
    const trslKey = `${labelKey}Trsl`;
    if (local[idKey] != null) {
      local[trslKey] = LOCAL_ADDRESS_TRANSLATION[labelKey];
    }
  }
  if (local.streetType != null) {
    local.streetTypeTrsl = String(local.streetType);
  }
  if (local.residentialAreaType != null) {
    local.residentialAreaTypeTrsl = String(local.residentialAreaType);
  }
}

function stampAddressContainer(addr: unknown, ids: ExpParityLocalAddressIds): void {
  if (!addr || typeof addr !== 'object') {
    return;
  }
  stampLocalAddressData(
    (addr as { localAddressData?: Record<string, unknown> }).localAddressData,
    ids,
  );
}

/** Overlay a consistent local address on customer or POD create/edit payloads. */
export function applyExpParityLocalAddress(
  payload: Record<string, unknown>,
  ids: ExpParityLocalAddressIds,
): void {
  stampAddressContainer(payload.address, ids);
  stampAddressContainer(payload.addressRequest, ids);
  if (payload.addressRequest && typeof payload.addressRequest === 'object') {
    const addressRequest = payload.addressRequest as Record<string, unknown>;
    const local = addressRequest.localAddressData;
    if (local && typeof local === 'object') {
      stampPodLocalTrsl(local as Record<string, unknown>);
    }
    const number = uppercaseFilled(addressRequest.number);
    if (number) {
      addressRequest.numberTrsl = number;
    }
  }
  if (Array.isArray(payload.communicationData)) {
    for (const row of payload.communicationData) {
      if (row && typeof row === 'object') {
        stampAddressContainer((row as { address?: unknown }).address, ids);
      }
    }
  }
  attachCustomerAddressTransliteration(payload);
}

function sleep(ms: number): Promise<void> {
  return new Promise((resolve) => setTimeout(resolve, ms));
}

/** ZipCodeRequest.name maxLength 32. Keep `Zip ${suffix}` under that cap. */
function uniqueNomenclatureSuffix(): string {
  return `pw-${Date.now().toString(36)}-${process.pid.toString(36).slice(-3)}-${Math.random()
    .toString(36)
    .slice(2, 6)}`;
}

function isDuplicateKeyBody(body: unknown): boolean {
  return /DuplicateKeyException|duplicate key value violates unique constraint/i.test(
    JSON.stringify(body ?? {}),
  );
}

/**
 * POST address nomenclatures retry DuplicateKeyException.
 * CountryRequest (dev swagger) has no orderingId — the DB assigns ordering_id
 * with max+1, so parallel workers collide on countries_ordering_uk / regions_ordering_uk.
 */
async function postNomenclatureUntilCommitted(
  Request: ExpParityCustomerFx['Request'],
  path: string,
  data: Record<string, unknown>,
): Promise<number> {
  const baseName = typeof data.name === 'string' ? data.name : 'Name';
  const baseTransliterated =
    typeof data.nameTransliterated === 'string' ? data.nameTransliterated : undefined;
  const maxAttempts = 8;
  for (let attempt = 1; attempt <= maxAttempts; attempt++) {
    const payload = { ...data };
    if (attempt > 1) {
      const bump = `r${attempt}${Math.random().toString(36).slice(2, 5)}`;
      const nameCap = path === 'zip-codes' ? 32 : 512;
      payload.name = `${baseName}-${bump}`.slice(0, nameCap);
      if (baseTransliterated) {
        payload.nameTransliterated = `${baseTransliterated}-${bump}`.slice(0, nameCap);
      }
    }
    const res = await Request.post(path, { data: payload });
    if (res.ok()) {
      await expect(res).CheckResponse();
      return asEntityId(await res.json());
    }
    const body = await res.json().catch(() => ({}));
    if (!isDuplicateKeyBody(body) || attempt === maxAttempts) {
      throw new Error(
        `POST ${path} failed HTTP ${res.status()} attempt ${attempt}/${maxAttempts}: ${JSON.stringify(body)}`,
      );
    }
    await sleep(120 * attempt + Math.floor(Math.random() * 400));
  }
  throw new Error(`POST ${path} exhausted DuplicateKey retries`);
}

/**
 * One ACTIVE country → street tree. Called per test (no describe-level ids, Rule 40).
 */
export async function createExpParityLocalAddressIds(
  Request: ExpParityCustomerFx['Request'],
): Promise<ExpParityLocalAddressIds> {
  await sleep(40 + Math.floor(Math.random() * 350));
  const unique = uniqueNomenclatureSuffix();
  const mkActive = (label: string, maxLen = 512) => {
    const name = `${label} ${unique}`.slice(0, maxLen);
    return {
      name,
      nameTransliterated: name.slice(0, maxLen),
      status: 'ACTIVE' as const,
      defaultSelection: false,
    };
  };

  const countryId = await postNomenclatureUntilCommitted(Request, 'countries', mkActive('Country'));
  const regionId = await postNomenclatureUntilCommitted(Request, 'regions', {
    ...mkActive('Region'),
    countryId,
  });
  const municipalityId = await postNomenclatureUntilCommitted(Request, 'municipalities', {
    ...mkActive('Municipality'),
    regionId,
  });
  const populatedPlaceId = await postNomenclatureUntilCommitted(Request, 'populated-places', {
    ...mkActive('Populated place'),
    municipalityId,
  });
  const zipCodeId = await postNomenclatureUntilCommitted(Request, 'zip-codes', {
    ...mkActive('Zip', 32),
    populatedPlaceId,
  });
  const districtId = await postNomenclatureUntilCommitted(Request, 'districts', {
    ...mkActive('District'),
    populatedPlaceId,
  });
  const residentialAreaId = await postNomenclatureUntilCommitted(Request, 'residential-areas', {
    ...mkActive('Residential area'),
    type: 'QUARTER',
    populatedPlaceId,
  });
  const streetId = await postNomenclatureUntilCommitted(Request, 'streets', {
    ...mkActive('Street'),
    type: 'STREET',
    populatedPlaceId,
  });

  return {
    countryId,
    regionId,
    municipalityId,
    populatedPlaceId,
    zipCodeId,
    districtId,
    residentialAreaId,
    streetId,
  };
}

export async function postExpParityLegalCustomer(
  fx: ExpParityCustomerFx,
  addressIds?: ExpParityLocalAddressIds,
): Promise<{ id: number; identifier: string; addressIds: ExpParityLocalAddressIds; body: unknown }> {
  const { Request, GeneratePayload, Responses, Endpoints } = fx;
  const ids = addressIds ?? (await createExpParityLocalAddressIds(Request));
  const payload = GeneratePayload.customers.customer_legal() as Record<string, unknown> & {
    customerIdentifier?: string;
  };
  applyExpParityLocalAddress(payload, ids);
  const res = await Request.post(Endpoints.customer, { data: payload });
  await expect(res).CheckResponse();
  const body = await res.json();
  const id = asEntityId(body);
  const identifier = String(
    (body as { identifier?: string }).identifier ?? payload.customerIdentifier ?? '',
  );
  Responses.customer.push(
    body && typeof body === 'object'
      ? { ...(body as object), id, identifier, customerIdentifier: identifier }
      : { id, identifier, customerIdentifier: identifier },
  );
  return { id, identifier, addressIds: ids, body };
}
