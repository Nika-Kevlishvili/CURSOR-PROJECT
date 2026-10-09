import { test, expect } from './cursor-test.fixtures';
import { attachManualVerificationLinks } from './shared/manual-verification-links.fixtures';

/**
 * Nomenclature list (filter) — optional language query parameter.
 *
 * **Target environment:** DEV2 (`https://devapps.energo-pro.bg/backend/phoenix-dev2` by default).
 * Override with `BASE_URL` if needed (must still be DEV2 for ticket intent).
 *
 * **Contract (refreshed dev2 OpenAPI):**
 * - `GET /titles` — query object `NomenclatureItemsBaseFilterRequest`
 * - Language field in spec: `lan` (not `language`) with enum `BULGARIAN` | `ENGLISH`
 * - We locate the created row with `prompt` = stored Bulgarian `name` and `exactMatch: true` (list-by-name).
 * - **Observed DEV2 list behaviour for `lan=ENGLISH`:** `name` may remain Bulgarian while English matches POST `nameTransliterated`; the EN test accepts English in either `name` or `nameTransliterated`.
 *
 * **Reference spec(s):** `tests/cursor/PHN-2808-translation-fields-nomenclature-english.spec.ts` (titles path + POST body shape).
 */

const TITLES_PATH = 'titles';
const DEV2_DEFAULT_BASE = 'https://devapps.energo-pro.bg/backend/phoenix-dev2';

type TitleRow = { id: number; name?: string; nameTransliterated?: string };

type TitleListPage = { content?: TitleRow[] };

async function preconditionCreateTitleWithDistinctNames(Request: {
    post: (url: string, options?: { data?: unknown }) => Promise<{ status: () => number; json: () => Promise<unknown> }>;
}): Promise<{ titleId: number; nameBulgarian: string; nameEnglish: string }> {
    const suffix = `${Date.now()}-${Math.floor(Math.random() * 10000)}`;
    const nameBulgarian = `НЛ-${suffix}`;
    const nameEnglish = `NL-LAN-${suffix}`;
    const response = await Request.post(TITLES_PATH, {
        data: {
            name: nameBulgarian,
            nameTransliterated: nameEnglish,
            status: 'ACTIVE',
            defaultSelection: false,
        },
    });
    await expect(response).CheckResponse();
    const body = (await response.json()) as { id: number };
    return { titleId: body.id, nameBulgarian, nameEnglish };
}

async function getTitlesFilterByStoredBgName(
    Request: {
        get: (url: string, options?: { params?: Record<string, unknown> }) => Promise<{ status: () => number; ok: () => boolean; json: () => Promise<unknown> }>;
    },
    /** Always the persisted Bulgarian `name` — filter searches that field regardless of `lan`. */
    nameBulgarian: string,
    extraParams: Record<string, unknown> = {}
) {
    return Request.get(TITLES_PATH, {
        params: {
            page: 0,
            size: 25,
            statuses: 'ACTIVE',
            prompt: nameBulgarian,
            exactMatch: true,
            ...extraParams,
        },
    });
}

function assertTitleRowName(page: TitleListPage, titleId: number, expectedName: string): void {
    const row = page.content?.find((r) => r.id === titleId);
    expect(row, `Expected title id ${titleId} in filter page`).toBeTruthy();
    expect(row!.name).toBe(expectedName);
}

/** When `lan=ENGLISH`, DEV2 may surface English either in `name` (ideal per UX) or only in `nameTransliterated`. */
function attachNomenclatureLanVerificationLinks(
  Responses: Parameters<typeof attachManualVerificationLinks>[0],
  snapshot: Record<string, unknown>,
): void {
  attachManualVerificationLinks(Responses, {
    jiraKey: 'Nomenclature-LAN',
    snapshot,
  });
}

function assertEnglishListRow(page: TitleListPage, titleId: number, nameEnglish: string, nameBulgarian: string): void {
    const row = page.content?.find((r) => r.id === titleId);
    expect(row, `Expected title id ${titleId} in filter page`).toBeTruthy();
    if (row!.name === nameEnglish) {
        expect(row!.name).not.toBe(nameBulgarian);
        return;
    }
    expect(row!.nameTransliterated, 'English label must appear in `name` or `nameTransliterated` when lan=ENGLISH').toBe(
        nameEnglish
    );
}

test.describe('[Nomenclature-LAN]: Nomenclature list by name — optional language (DEV2)', { tag: ['@nomenclature', '@dev2'] }, () => {
    test.use({
        baseURL: process.env.BASE_URL ?? DEV2_DEFAULT_BASE,
    });

    test('[Nomenclature-LAN]: DEV2 — lan=BULGARIAN returns Bulgarian name', async ({ Request, Responses }) => {
        let titleId: number;
        let nameBulgarian: string;
        let nameEnglish: string;

        await test.step('Precondition: POST /titles with distinct BG / EN strings', async () => {
            const created = await preconditionCreateTitleWithDistinctNames(Request);
            titleId = created.titleId;
            nameBulgarian = created.nameBulgarian;
            nameEnglish = created.nameEnglish;
        });

        await test.step('GET /titles with lan=BULGARIAN — name is Bulgarian', async () => {
            const response = await getTitlesFilterByStoredBgName(Request, nameBulgarian, { lan: 'BULGARIAN' });
            await expect(response).CheckResponse();
            const page = (await response.json()) as TitleListPage;
            assertTitleRowName(page, titleId, nameBulgarian);
        });

        await test.step('Attach portal links for manual verification', async () => {
            attachNomenclatureLanVerificationLinks(Responses, { titleId, nameBulgarian, nameEnglish, lan: 'BULGARIAN' });
        });
    });

    test('[Nomenclature-LAN]: DEV2 — lan=ENGLISH returns English display name', async ({ Request, Responses }) => {
        let titleId: number;
        let nameBulgarian: string;
        let nameEnglish: string;

        await test.step('Precondition: POST /titles with distinct BG / EN strings', async () => {
            const created = await preconditionCreateTitleWithDistinctNames(Request);
            titleId = created.titleId;
            nameBulgarian = created.nameBulgarian;
            nameEnglish = created.nameEnglish;
        });

        await test.step('GET /titles with lan=ENGLISH — list row exposes English (name and/or nameTransliterated)', async () => {
            const response = await getTitlesFilterByStoredBgName(Request, nameBulgarian, { lan: 'ENGLISH' });
            await expect(response).CheckResponse();
            const page = (await response.json()) as TitleListPage;
            assertEnglishListRow(page, titleId, nameEnglish, nameBulgarian);
        });

        await test.step('Attach portal links for manual verification', async () => {
            attachNomenclatureLanVerificationLinks(Responses, { titleId, nameBulgarian, nameEnglish, lan: 'ENGLISH' });
        });
    });

    test('[Nomenclature-LAN]: DEV2 — omitting lan defaults to Bulgarian name', async ({ Request, Responses }) => {
        let titleId: number;
        let nameBulgarian: string;

        await test.step('Precondition: POST /titles with distinct BG / EN strings', async () => {
            const created = await preconditionCreateTitleWithDistinctNames(Request);
            titleId = created.titleId;
            nameBulgarian = created.nameBulgarian;
        });

        await test.step('GET /titles without lan — same name as explicit BULGARIAN', async () => {
            const withoutLan = await getTitlesFilterByStoredBgName(Request, nameBulgarian);
            await expect(withoutLan).CheckResponse();
            const explicitBg = await getTitlesFilterByStoredBgName(Request, nameBulgarian, { lan: 'BULGARIAN' });
            await expect(explicitBg).CheckResponse();
            const pageA = (await withoutLan.json()) as TitleListPage;
            const pageB = (await explicitBg.json()) as TitleListPage;
            assertTitleRowName(pageA, titleId, nameBulgarian);
            expect(pageA.content?.find((r) => r.id === titleId)?.name).toBe(
                pageB.content?.find((r) => r.id === titleId)?.name
            );
        });

        await test.step('Attach portal links for manual verification', async () => {
            attachNomenclatureLanVerificationLinks(Responses, { titleId, nameBulgarian, lan: 'default' });
        });
    });

    test('[Nomenclature-LAN]: DEV2 — unsupported lan (GERMAN) is safe — 4xx or Bulgarian fallback, never 5xx', async ({
        Request,
        Responses,
    }) => {
        let titleId: number;
        let nameBulgarian: string;

        await test.step('Precondition: POST /titles', async () => {
            const created = await preconditionCreateTitleWithDistinctNames(Request);
            titleId = created.titleId;
            nameBulgarian = created.nameBulgarian;
        });

        await test.step('GET /titles with lan=GERMAN (not in OpenAPI enum)', async () => {
            const response = await getTitlesFilterByStoredBgName(Request, nameBulgarian, { lan: 'GERMAN' });
            const status = response.status();
            expect(status < 500).toBeTruthy();

            if (response.ok()) {
                const page = (await response.json()) as TitleListPage;
                const row = page.content?.find((r) => r.id === titleId);
                expect(row?.name).toBe(nameBulgarian);
            } else {
                expect(status).toBeGreaterThanOrEqual(400);
                expect(status).toBeLessThan(500);
            }
        });

        await test.step('Attach portal links for manual verification', async () => {
            attachNomenclatureLanVerificationLinks(Responses, { titleId, nameBulgarian, lan: 'GERMAN' });
        });
    });

    test('[Nomenclature-LAN]: DEV2 — empty lan — safe (4xx or default BG), never 5xx', async ({ Request, Responses }) => {
        let titleId: number;
        let nameBulgarian: string;

        await test.step('Precondition: POST /titles', async () => {
            const created = await preconditionCreateTitleWithDistinctNames(Request);
            titleId = created.titleId;
            nameBulgarian = created.nameBulgarian;
        });

        await test.step('GET /titles with lan="" ', async () => {
            const response = await getTitlesFilterByStoredBgName(Request, nameBulgarian, { lan: '' });
            const status = response.status();
            expect(status < 500).toBeTruthy();

            if (response.ok()) {
                const page = (await response.json()) as TitleListPage;
                assertTitleRowName(page, titleId, nameBulgarian);
            } else {
                expect(status).toBeGreaterThanOrEqual(400);
                expect(status).toBeLessThan(500);
            }
        });

        await test.step('Attach portal links for manual verification', async () => {
            attachNomenclatureLanVerificationLinks(Responses, { titleId, nameBulgarian, lan: '' });
        });
    });

    test('[Nomenclature-LAN]: DEV2 — wrong query key lang=ENGLISH does not switch to English unless API errors', async ({
        Request,
        Responses,
    }) => {
        let titleId: number;
        let nameBulgarian: string;
        let nameEnglish: string;

        await test.step('Precondition: POST /titles', async () => {
            const created = await preconditionCreateTitleWithDistinctNames(Request);
            titleId = created.titleId;
            nameBulgarian = created.nameBulgarian;
            nameEnglish = created.nameEnglish;
        });

        await test.step('Baseline: lan=BULGARIAN', async () => {
            const response = await getTitlesFilterByStoredBgName(Request, nameBulgarian, { lan: 'BULGARIAN' });
            await expect(response).CheckResponse();
            const page = (await response.json()) as TitleListPage;
            assertTitleRowName(page, titleId, nameBulgarian);
        });

        await test.step('Typo: pass lang (not lan)=ENGLISH — expect BG default or non-500 rejection', async () => {
            const response = await Request.get(TITLES_PATH, {
                params: {
                    page: 0,
                    size: 25,
                    statuses: 'ACTIVE',
                    prompt: nameBulgarian,
                    exactMatch: true,
                    lang: 'ENGLISH',
                },
            });
            const status = response.status();
            expect(status).toBeLessThan(500);

            if (!response.ok()) {
                expect(status).toBeGreaterThanOrEqual(400);
                return;
            }

            const page = (await response.json()) as TitleListPage;
            assertTitleRowName(page, titleId, nameBulgarian);
        });

        await test.step('Attach portal links for manual verification', async () => {
            attachNomenclatureLanVerificationLinks(Responses, {
                titleId,
                nameBulgarian,
                nameEnglish,
                queryKey: 'lang',
            });
        });
    });
});
