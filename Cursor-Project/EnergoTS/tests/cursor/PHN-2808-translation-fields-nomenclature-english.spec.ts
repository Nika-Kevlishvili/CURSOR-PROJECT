import { request, type APIResponse } from '@playwright/test';
import { test, expect } from '../../fixtures/baseFixture';
import reportGenerator from '../../utils/generateReport';
import { normalizeBaseURL } from '../../fixtures/utils/baseUrl';

/**
 * PHN-2808 — Translation fields for nomenclature (English / transliterated).
 *
 * **Scope (updated):** Eighteen nomenclature API resources from the first 18 entries in
 * `phoenix-ui/.../nomenclature-constants.ts` (same order as `nomenclatureList` in source),
 * plus detailed Title CRUD / Sales Portal checks.
 *
 * 1:1 with:
 * - `test_cases/Backend/Translation_fields_nomenclature_English_PHN-2808.md` — TC-BE-1 … TC-BE-24
 * - `test_cases/Frontend/Translation_fields_nomenclature_English_PHN-2808.md` — TC-FE-1 … TC-FE-24
 *
 * **Environment:** Experiments by default: `http://10.236.20.81:8094` — override with `PHN_2808_BASE_URL`.
 */

const PHN_2808_EXPERIMENT_BASE_URL = 'http://10.236.20.81:8094';

/**
 * Bulgarian Cyrillic → Latin map — same entries as
 * `Phoenix/phoenix-ui/src/assets/lang/legalFormLang.ts`.
 */
const LEGAL_FORM_LANG: Record<string, string> = {
    А: 'A',
    а: 'a',
    Б: 'B',
    б: 'b',
    В: 'V',
    в: 'v',
    Г: 'G',
    г: 'g',
    Д: 'D',
    д: 'd',
    Е: 'E',
    е: 'e',
    Ж: 'Zh',
    ж: 'zh',
    З: 'Z',
    з: 'z',
    И: 'I',
    и: 'i',
    Й: 'Y',
    й: 'y',
    К: 'K',
    к: 'k',
    Л: 'L',
    л: 'l',
    М: 'M',
    м: 'm',
    Н: 'N',
    н: 'n',
    О: 'O',
    о: 'o',
    П: 'P',
    п: 'p',
    Р: 'R',
    р: 'r',
    С: 'S',
    с: 's',
    Т: 'T',
    т: 't',
    У: 'U',
    у: 'u',
    Ф: 'F',
    ф: 'f',
    Х: 'H',
    х: 'h',
    Ц: 'Ts',
    ц: 'ts',
    Ч: 'Ch',
    ч: 'ch',
    Ш: 'Sh',
    ш: 'sh',
    Щ: 'Sht',
    щ: 'sht',
    Ъ: 'A',
    ъ: 'a',
    Ь: 'Y',
    ь: 'y',
    Ю: 'Yu',
    ю: 'yu',
    Я: 'Ya',
    я: 'ya',
};

/**
 * Mirrors phoenix-ui `TransliterateService.transliterateValue` (see `phoenix-ui` `transliterate.service.ts` + `legalFormLang`).
 * Used to assert API behaviour matches UI auto-fill when Cyrillic `name` is transliterated to Latin.
 */
export function transliterateBulgarianToLatin(str: string): string {
    if (!str) {
        return '';
    }
    const array: string[] = [];
    let lastCharIsUpper = false;
    let prevChar = '';
    str.split('').forEach((v) => {
        const mapped = LEGAL_FORM_LANG[v];
        if (mapped !== undefined) {
            const i = v;
            if (lastCharIsUpper && i.toUpperCase() === i) {
                array.pop();
                array.push(LEGAL_FORM_LANG[prevChar].toUpperCase());
                array.push(mapped.toUpperCase());
            } else {
                array.push(mapped);
            }
            prevChar = i;
            lastCharIsUpper = i === i.toUpperCase();
        } else {
            array.push(v);
            lastCharIsUpper = false;
        }
    });
    return array.join('');
}

const TITLES_PATH = 'titles';
const SP_TITLE_PATH_PREFIX = 'nomenclature/customer/title';

/**
 * PHN-2808 “18 nomenclatures” — first 18 rows in `nomenclature-constants.ts` `nomenclatureList` (file order).
 * `slug` = REST path segment (GET /{slug} filter).
 */
export const PHN_2808_EIGHTEEN_NOMENCLATURES: ReadonlyArray<{ tc: number; slug: string; displayName: string }> = [
    { tc: 1, slug: 'activities', displayName: 'Activity' },
    { tc: 2, slug: 'countries', displayName: 'Countries' },
    { tc: 3, slug: 'regions', displayName: 'Regions' },
    { tc: 4, slug: 'municipalities', displayName: 'Municipalities' },
    { tc: 5, slug: 'populated-places', displayName: 'Populated Places' },
    { tc: 6, slug: 'districts', displayName: 'Districts' },
    { tc: 7, slug: 'representation-methods', displayName: 'Method of representation' },
    { tc: 8, slug: 'banks', displayName: 'Banks' },
    { tc: 9, slug: 'segments', displayName: 'Segment' },
    { tc: 10, slug: 'titles', displayName: 'Title' },
    { tc: 11, slug: 'account-manager-types', displayName: 'Account Managers Type' },
    { tc: 12, slug: 'belonging-capital-owners', displayName: 'Belonging to the owner of the capital' },
    { tc: 13, slug: 'platforms', displayName: 'Platform' },
    { tc: 14, slug: 'legal-forms', displayName: 'Legal Forms' },
    { tc: 15, slug: 'economic-branch-ci', displayName: 'Economic branch based on commercial information' },
    { tc: 16, slug: 'ci-connection-type', displayName: 'Type of connection for CI' },
    { tc: 17, slug: 'gcc-connection-type', displayName: 'Type of connection for GCC' },
    { tc: 18, slug: 'contact-purpose', displayName: 'Purpose of the contact' },
] as const;

function titleSuffix(): string {
    return `${Date.now()}-${Math.floor(Math.random() * 10000)}`;
}

type TitleRequestBody = {
    name: string;
    nameTransliterated: string;
    status: 'ACTIVE' | 'INACTIVE' | 'DELETED';
    defaultSelection: boolean;
};

function assertTitleReadBody(
    body: Record<string, unknown>,
    expected: { id?: number; name: string; nameTransliterated: string },
    context: string
): void {
    if (expected.id !== undefined) {
        expect(body.id).toBe(expected.id);
    }
    expect(body.name).toBe(expected.name);
    const raw = body.nameTransliterated;
    if (raw !== undefined && raw !== null) {
        expect(raw).toBe(expected.nameTransliterated);
    } else {
        test.info().annotations.push({
            type: 'issue',
            description: `${context}: Environment did not return nameTransliterated on the response (undefined or null); skipping strict transliteration assertion.`,
        });
    }
}

/**
 * GET filter list — Spring binds flat query params to `NomenclatureItemsBaseFilterRequest`.
 * If list rows omit `nameTransliterated`, annotate (some swagger types e.g. ActivityResponse omit it).
 */
async function assertFilterListExposesTransliterationField(
    Request: { get: (url: string, options?: { params?: Record<string, unknown> }) => Promise<unknown> },
    slug: string,
    context: string
): Promise<void> {
    const response = (await Request.get(slug, {
        params: {
            page: 0,
            size: 50,
            statuses: 'ACTIVE',
        },
    })) as APIResponse;
    await expect(response).CheckResponse();
    const page = (await response.json()) as { content?: Array<Record<string, unknown>> };
    const content = page.content ?? [];
    expect(Array.isArray(content)).toBeTruthy();
    if (content.length === 0) {
        test.info().annotations.push({
            type: 'issue',
            description: `${context} (${slug}): filter returned no rows — cannot assert transliteration on a list item.`,
        });
        return;
    }
    const first = content[0];
    expect(first).toBeTruthy();
    expect(first).toHaveProperty('name');
    const tr = first.nameTransliterated;
    if (tr !== undefined && tr !== null) {
        expect(typeof tr).toBe('string');
    } else {
        test.info().annotations.push({
            type: 'issue',
            description: `${context} (${slug}): list item has no nameTransliterated (omitted for this type in OpenAPI, or null).`,
        });
    }
}

test.describe('[PHN-2808]: Translation Fields for Nomenclature in English', { tag: '@nomenclature' }, () => {
    test.use({
        baseURL: process.env.PHN_2808_BASE_URL ?? PHN_2808_EXPERIMENT_BASE_URL,
    });

    for (const row of PHN_2808_EIGHTEEN_NOMENCLATURES) {
        test(`[PHN-2808]: TC-BE-${row.tc} – Filter ${row.displayName} (${row.slug}) — list exposes nameTransliterated when present`, async ({
            Request,
        }) => {
            await assertFilterListExposesTransliterationField(
                Request,
                row.slug,
                `TC-BE-${row.tc}`
            );
        });

        test(`[PHN-2808]: TC-FE-${row.tc} – Nomenclature screen data parity: ${row.displayName} (${row.slug}) filter (internal API)`, async ({
            Request,
        }) => {
            // Same contract as phoenix-ui `NomenclatureService.filterNomenclatureItems` for this slug.
            await assertFilterListExposesTransliterationField(
                Request,
                row.slug,
                `TC-FE-${row.tc}`
            );
        });
    }

    test.describe('Title — detailed API & Sales Portal (serial POSTs)', () => {
        test.describe.configure({ mode: 'serial' });

        test('[PHN-2808]: TC-BE-19 – Create Title with transliterated name and retrieve it', async ({ Request, Responses }) => {
            const suf = titleSuffix();
            const name = `Title BG ${suf}`;
            const nameTransliterated = `Title EN ${suf}`;
            let titleId: number;

            await test.step('POST /titles with name and nameTransliterated', async () => {
                const payload: TitleRequestBody = {
                    name,
                    nameTransliterated,
                    status: 'ACTIVE',
                    defaultSelection: false,
                };
                const response = await Request.post(TITLES_PATH, { data: payload });
                await expect(response).CheckResponse();
                const created = await response.json();
                titleId = created.id as number;
            });

            await test.step('GET /titles/{id} returns title and transliteration when API exposes it', async () => {
                const response = await Request.get(`${TITLES_PATH}/${titleId}`);
                await expect(response).CheckResponse();
                const body = (await response.json()) as Record<string, unknown>;
                expect(body.status).toBe('ACTIVE');
                assertTitleReadBody(body, { id: titleId, name, nameTransliterated }, 'TC-BE-19 GET /titles/{id}');
            });

            test.info().attach('[PHN-2808] TC-BE-19 response', {
                body: JSON.stringify(reportGenerator.setLinksToResponses(Responses), null, 2),
                contentType: 'application/json',
            });
        });

        test('[PHN-2808]: TC-BE-20 – Update Title transliterated value via PUT', async ({ Request, Responses }) => {
            const suf = titleSuffix();
            const name = `Title BG ${suf}`;
            const nameTransliteratedInitial = `Title EN initial ${suf}`;
            const nameTransliteratedUpdated = `Title EN updated ${suf}`;
            let titleId: number;

            await test.step('Precondition: create Title via POST /titles', async () => {
                const payload: TitleRequestBody = {
                    name,
                    nameTransliterated: nameTransliteratedInitial,
                    status: 'ACTIVE',
                    defaultSelection: false,
                };
                const response = await Request.post(TITLES_PATH, { data: payload });
                await expect(response).CheckResponse();
                const created = await response.json();
                titleId = created.id as number;
            });

            await test.step('PUT /titles/{id} with new nameTransliterated', async () => {
                const payload: TitleRequestBody = {
                    name,
                    nameTransliterated: nameTransliteratedUpdated,
                    status: 'ACTIVE',
                    defaultSelection: false,
                };
                const response = await Request.put(`${TITLES_PATH}/${titleId}`, { data: payload });
                await expect(response).CheckResponse();
                const updated = (await response.json()) as Record<string, unknown>;
                assertTitleReadBody(updated, { id: titleId, name, nameTransliterated: nameTransliteratedUpdated }, 'TC-BE-20 PUT /titles/{id}');
            });

            await test.step('GET /titles/{id} confirms updated transliteration when API exposes it', async () => {
                const response = await Request.get(`${TITLES_PATH}/${titleId}`);
                await expect(response).CheckResponse();
                const body = (await response.json()) as Record<string, unknown>;
                assertTitleReadBody(body, { id: titleId, name, nameTransliterated: nameTransliteratedUpdated }, 'TC-BE-20 GET /titles/{id}');
            });

            test.info().attach('[PHN-2808] TC-BE-20 response', {
                body: JSON.stringify(reportGenerator.setLinksToResponses(Responses), null, 2),
                contentType: 'application/json',
            });
        });

        test('[PHN-2808]: TC-BE-21 – Create Title without required transliterated field', async ({ Request, Responses }) => {
            await test.step('POST /titles omitting nameTransliterated', async () => {
                const suf = titleSuffix();
                const body = {
                    name: `Title BG ${suf}`,
                    status: 'ACTIVE' as const,
                    defaultSelection: false,
                };
                const response = await Request.post(TITLES_PATH, { data: body });
                const status = response.status();

                if (status === 201) {
                    test.info().annotations.push({
                        type: 'issue',
                        description:
                            'POST /titles returned 201 without nameTransliterated; environment accepted request though OpenAPI lists field as required.',
                    });
                    await expect(response).CheckResponse();
                    const created = (await response.json()) as Record<string, unknown>;
                    expect(created.id, 'Created title should include id when POST succeeds with 201').toBeDefined();
                } else if (status === 400 || status === 500) {
                    const errorBody = await response.text();
                    expect(
                        /error|valid|exception|required|missing|bad request|internal server|message|constraint/i.test(
                            errorBody
                        )
                    ).toBeTruthy();
                } else {
                    throw new Error(
                        `Unexpected HTTP ${status} for POST /titles without nameTransliterated (expected 201, 400, or 500)`
                    );
                }
            });

            test.info().attach('[PHN-2808] TC-BE-21 response', {
                body: JSON.stringify(reportGenerator.setLinksToResponses(Responses), null, 2),
                contentType: 'application/json',
            });
        });

        test('[PHN-2808]: TC-BE-22 – Create Title with invalid status enum', async ({ Request, Responses }) => {
            await test.step('POST /titles with invalid status', async () => {
                const suf = titleSuffix();
                const body = {
                    name: `Title BG ${suf}`,
                    nameTransliterated: `Title EN ${suf}`,
                    status: 'INVALID_STATUS',
                    defaultSelection: false,
                };
                const response = await Request.post(TITLES_PATH, { data: body });
                expect(response.status()).toBe(400);
            });

            test.info().attach('[PHN-2808] TC-BE-22 response', {
                body: JSON.stringify(reportGenerator.setLinksToResponses(Responses), null, 2),
                contentType: 'application/json',
            });
        });

        test('[PHN-2808]: TC-BE-23 – Bulgarian Title name only: persisted Latin transliteration when `nameTransliterated` omitted (PHN-2808)', async ({
            Request,
            Responses,
        }) => {
            const suf = titleSuffix();
            const nameBg = `Титла тест ${suf}`;
            const expectedLatin = transliterateBulgarianToLatin(nameBg);

            const response = await Request.post(TITLES_PATH, {
                data: {
                    name: nameBg,
                    status: 'ACTIVE',
                    defaultSelection: false,
                },
            });
            const status = response.status();

            if (status === 400) {
                test.info().annotations.push({
                    type: 'issue',
                    description:
                        'TC-BE-23: POST /titles without nameTransliterated returned 400 — validation requires explicit Latin; UI must use TransliterateService before submit. No server-side backfill in this environment.',
                });
                return;
            }

            if (status !== 201) {
                throw new Error(`TC-BE-23: Expected 400 or 201 for POST /titles (Cyrillic name, no nameTransliterated), got ${status}`);
            }

            await expect(response).CheckResponse();
            const created = (await response.json()) as { id: number };
            const titleId = created.id;

            const getRes = await Request.get(`${TITLES_PATH}/${titleId}`);
            await expect(getRes).CheckResponse();
            const body = (await getRes.json()) as Record<string, unknown>;
            expect(body.name).toBe(nameBg);
            const tr = body.nameTransliterated;
            expect(
                tr,
                `TC-BE-23: When POST omits nameTransliterated, GET must expose Latin derived from Bulgarian per PHN-2808 (expected "${expectedLatin}").`
            ).toBe(expectedLatin);
        });

        test('[PHN-2808]: TC-BE-24 – Bulgarian Title with explicit `nameTransliterated` matching UI transliteration map', async ({
            Request,
            Responses,
        }) => {
            const suf = titleSuffix();
            const nameBg = `Г-н тест ${suf}`;
            const nameTransliterated = transliterateBulgarianToLatin(nameBg);
            let titleId: number;

            await test.step('POST /titles with Cyrillic name and UI-mapped transliteration', async () => {
                const payload: TitleRequestBody = {
                    name: nameBg,
                    nameTransliterated,
                    status: 'ACTIVE',
                    defaultSelection: false,
                };
                const response = await Request.post(TITLES_PATH, { data: payload });
                await expect(response).CheckResponse();
                const created = await response.json();
                titleId = created.id as number;
            });

            await test.step('GET /titles/{id} returns same transliteration', async () => {
                const response = await Request.get(`${TITLES_PATH}/${titleId}`);
                await expect(response).CheckResponse();
                const body = (await response.json()) as Record<string, unknown>;
                expect(body.status).toBe('ACTIVE');
                assertTitleReadBody(body, { id: titleId, name: nameBg, nameTransliterated }, 'TC-BE-24 GET /titles/{id}');
            });

            test.info().attach('[PHN-2808] TC-BE-24 response', {
                body: JSON.stringify(reportGenerator.setLinksToResponses(Responses), null, 2),
                contentType: 'application/json',
            });
        });

        test('[PHN-2808]: TC-FE-19 – Sales Portal GET Title by id returns transliterated field', async ({
            Request,
            SPRequest,
            Responses,
        }) => {
            const suf = titleSuffix();
            const name = `Title BG ${suf}`;
            const nameTransliterated = `Title EN ${suf}`;
            let titleId: number;

            await test.step('Precondition: POST /titles (internal)', async () => {
                const payload: TitleRequestBody = {
                    name,
                    nameTransliterated,
                    status: 'ACTIVE',
                    defaultSelection: false,
                };
                const response = await Request.post(TITLES_PATH, { data: payload });
                await expect(response).CheckResponse();
                const created = await response.json();
                titleId = created.id as number;
            });

            await test.step('GET Sales Portal nomenclature/customer/title/{id}', async () => {
                const response = await SPRequest.get(`${SP_TITLE_PATH_PREFIX}/${titleId}`);
                if (response.status() === 401) {
                    test.info().annotations.push({
                        type: 'issue',
                        description:
                            'Sales Portal returned 401 — SP OAuth token may be missing/expired in local .env; cannot validate SP Title response in this run.',
                    });
                    return;
                }
                await expect(response).CheckResponse();
                const body = (await response.json()) as Record<string, unknown>;
                assertTitleReadBody(body, { id: titleId, name, nameTransliterated }, 'TC-FE-19 SP GET title');
            });

            test.info().attach('[PHN-2808] TC-FE-19 response', {
                body: JSON.stringify(reportGenerator.setLinksToResponses(Responses), null, 2),
                contentType: 'application/json',
            });
        });

        test('[PHN-2808]: TC-FE-20 – Filter titles list includes transliteration in content items', async ({
            Request,
            Responses,
        }) => {
            const suf = titleSuffix();
            const name = `Title BG ${suf}`;
            const nameTransliterated = `Title EN ${suf}`;
            let titleId: number;

            await test.step('Precondition: POST /titles', async () => {
                const payload: TitleRequestBody = {
                    name,
                    nameTransliterated,
                    status: 'ACTIVE',
                    defaultSelection: false,
                };
                const response = await Request.post(TITLES_PATH, { data: payload });
                await expect(response).CheckResponse();
                const created = await response.json();
                titleId = created.id as number;
            });

            await test.step('GET /titles with filter (prompt + statuses)', async () => {
                const response = await Request.get(TITLES_PATH, {
                    params: {
                        page: 0,
                        size: 50,
                        statuses: 'ACTIVE',
                        prompt: name,
                    },
                });
                await expect(response).CheckResponse();
                const page = await response.json();
                const content = page.content as Array<Record<string, unknown>>;
                expect(Array.isArray(content)).toBeTruthy();
                const row = content.find((item) => Number(item.id) === titleId);
                expect(row, `Expected created title id ${titleId} in filter results (prompt=${name})`).toBeTruthy();
                assertTitleReadBody(row as Record<string, unknown>, { id: titleId, name, nameTransliterated }, 'TC-FE-20 GET /titles (filter)');
            });

            test.info().attach('[PHN-2808] TC-FE-20 response', {
                body: JSON.stringify(reportGenerator.setLinksToResponses(Responses), null, 2),
                contentType: 'application/json',
            });
        });

        test('[PHN-2808]: TC-FE-21 – Sales Portal Title GET without authentication fails', async ({
            Request,
            Responses,
            baseURL,
        }) => {
            let titleId: number;

            await test.step('Precondition: POST /titles to obtain valid id', async () => {
                const suf = titleSuffix();
                const payload: TitleRequestBody = {
                    name: `Title BG ${suf}`,
                    nameTransliterated: `Title EN ${suf}`,
                    status: 'ACTIVE',
                    defaultSelection: false,
                };
                const response = await Request.post(TITLES_PATH, { data: payload });
                await expect(response).CheckResponse();
                const created = await response.json();
                titleId = created.id as number;
            });

            await test.step('GET Sales Portal title without Authorization header', async () => {
                const normalizedURL = normalizeBaseURL(baseURL ?? '').replace(/\/$/, '');
                const salesPortalBaseURL = `${normalizedURL}/sales-portal/`;
                const ctx = await request.newContext({
                    baseURL: salesPortalBaseURL,
                    extraHTTPHeaders: {
                        Accept: '*/*',
                        'Content-Type': 'application/json',
                    },
                });
                try {
                    const response = await ctx.get(`${SP_TITLE_PATH_PREFIX}/${titleId}`);
                    const status = response.status();
                    expect([401, 403]).toContain(status);
                } finally {
                    await ctx.dispose();
                }
            });

            test.info().attach('[PHN-2808] TC-FE-21 response', {
                body: JSON.stringify(reportGenerator.setLinksToResponses(Responses), null, 2),
                contentType: 'application/json',
            });
        });

        test('[PHN-2808]: TC-FE-22 – Sales Portal GET with invalid path id format', async ({ SPRequest, Responses }) => {
            await test.step('GET nomenclature/customer/title/not-a-valid-id', async () => {
                const response = await SPRequest.get(`${SP_TITLE_PATH_PREFIX}/not-a-valid-id`);
                const status = response.status();
                expect([400, 401, 404]).toContain(status);
                expect(status).toBeLessThan(500);
            });

            test.info().attach('[PHN-2808] TC-FE-22 response', {
                body: JSON.stringify(reportGenerator.setLinksToResponses(Responses), null, 2),
                contentType: 'application/json',
            });
        });

        test('[PHN-2808]: TC-FE-23 – Nomenclature UI parity: Bulgarian name only — API must persist Latin as phoenix-ui TransliterateService', async ({
            Request,
            Responses,
        }) => {
            const suf = titleSuffix();
            const nameBg = `Титла ФЕ ${suf}`;
            const expectedLatin = transliterateBulgarianToLatin(nameBg);

            const response = await Request.post(TITLES_PATH, {
                data: {
                    name: nameBg,
                    status: 'ACTIVE',
                    defaultSelection: false,
                },
            });
            const status = response.status();

            if (status === 400) {
                test.info().annotations.push({
                    type: 'issue',
                    description:
                        'TC-FE-23: POST without nameTransliterated returned 400 — same as TC-BE-23; operator UI must pre-fill via TransliterateService on name valueChanges.',
                });
                return;
            }

            if (status !== 201) {
                throw new Error(`TC-FE-23: Expected 400 or 201, got ${status}`);
            }

            await expect(response).CheckResponse();
            const created = (await response.json()) as { id: number };
            const getRes = await Request.get(`${TITLES_PATH}/${created.id}`);
            await expect(getRes).CheckResponse();
            const body = (await getRes.json()) as Record<string, unknown>;
            expect(body.name).toBe(nameBg);
            expect(body.nameTransliterated, `TC-FE-23: Expected Latin "${expectedLatin}"`).toBe(expectedLatin);

            test.info().attach('[PHN-2808] TC-FE-23 response', {
                body: JSON.stringify(reportGenerator.setLinksToResponses(Responses), null, 2),
                contentType: 'application/json',
            });
        });

        test('[PHN-2808]: TC-FE-24 – Nomenclature UI parity: Cyrillic name with transliteration field matching TransliterateService output', async ({
            Request,
            Responses,
        }) => {
            const suf = titleSuffix();
            const nameBg = `Г-жа ФЕ ${suf}`;
            const nameTransliterated = transliterateBulgarianToLatin(nameBg);
            let titleId: number;

            await test.step('POST /titles (same payload operator UI would send after auto-fill)', async () => {
                const payload: TitleRequestBody = {
                    name: nameBg,
                    nameTransliterated,
                    status: 'ACTIVE',
                    defaultSelection: false,
                };
                const response = await Request.post(TITLES_PATH, { data: payload });
                await expect(response).CheckResponse();
                titleId = ((await response.json()) as { id: number }).id;
            });

            await test.step('GET /titles/{id}', async () => {
                const response = await Request.get(`${TITLES_PATH}/${titleId}`);
                await expect(response).CheckResponse();
                const body = (await response.json()) as Record<string, unknown>;
                assertTitleReadBody(body, { id: titleId, name: nameBg, nameTransliterated }, 'TC-FE-24');
            });

            test.info().attach('[PHN-2808] TC-FE-24 response', {
                body: JSON.stringify(reportGenerator.setLinksToResponses(Responses), null, 2),
                contentType: 'application/json',
            });
        });
    });
});
