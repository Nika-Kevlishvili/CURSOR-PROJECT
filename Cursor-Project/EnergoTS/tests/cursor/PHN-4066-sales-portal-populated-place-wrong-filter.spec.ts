/**
 * PHN-4066 — Sales Portal API Populated place wrong filter.
 *
 * Bug-only automation (no TC .md). Jira: PHN-4066. Reproduce lives in customfield_10103.
 *
 * Criterion-7 exception (not a hardcoded entity id): prompt "Варна" is the ticket
 * reproduce string for system nomenclature POPULATED_PLACES. Confluence dump API is
 * GET /nomenclature/{name} (full list for dropdowns) — this test cannot create city
 * Варна as a precondition. No numeric ids are hardcoded.
 *
 * Expected (assert this — test is RED on current Dev2 until the bug is fixed):
 * 1. OpenAPI NomenclatureItemsBaseFilterRequest.exactMatch: "When true, prompt must
 *    match exactly." (not %prompt% contains). Live Sales Portal OpenAPI: GET
 *    /nomenclature/{nomenclature}/filter operationId filterNomenclature; path enum
 *    includes POPULATED_PLACES; required statuses, page, size; optional prompt,
 *    exactMatch, lan, excludedItemId, includedItemIds; success 206.
 * 2. Reporter: one result — the city Варна. Exact match on the populated place’s own
 *    name (first segment before " - "), case-insensitive trim — not region /
 *    municipality / country (that would still return ~188 Dev2 places under region Варна).
 * 3. Wiki does NOT define /filter or exactMatch (do not claim Confluence mandates it):
 *    - GET list by nomenclature name (page 711229442)
 *    - GET Nomenclature list (page 713195521)
 *    - GET Detail by nomenclature name+ID (page 715849729)
 *    CQL exactMatch AND nomenclature → 0 pages.
 *
 * Runtime today (not asserted as expected): PopulatedPlaceService.filterNomenclature
 * always uses fromPromptToQueryParameter (%…%) and JPQL ORs place/municipality/region/
 * country; exactMatch is ignored. Live Dev2: exactMatch true/false/omitted all return
 * totalElements=209 (e.g. БЕНКОВСКИ - АВРЕН - ВАРНА - …).
 *
 * SPRequest: use absolute URL http://10.236.20.11:7092/nomenclature/POPULATED_PLACES/filter
 * (see fixtures). Relative nomenclature/... 404s under /sales-portal/.
 *
 * Target env: Dev2.
 *
 * Reference spec(s):
 * - tests/contractsAndOrders/productContract.spec.ts
 * - tests/cursor/PHN-4002-product-contract-sales-channel-filter.spec.ts
 */

import { test, expect } from './cursor-test.fixtures';
import { finalizeTestRunSummary } from './shared/manual-verification-links.fixtures';
import {
  AVREN_VILLAGE_EXAMPLE,
  EXPECTED_EXACT_PLACE_NAME,
  PHN_4066_JIRA_KEY,
  PHN_4066_TITLE,
  SALES_PORTAL_SWAGGER_UI_URL,
  TICKET_PROMPT,
  firstPageContainsAvrenVillage,
  firstPageNames,
  getPopulatedPlacesFilter,
  namesEqualIgnoreCaseTrim,
  populatedPlaceOwnName,
  readTotalElements,
} from './phn-4066-sales-portal-populated-place-wrong-filter.fixtures';

test.describe(`[${PHN_4066_JIRA_KEY}]: ${PHN_4066_TITLE}`, {
  tag: ['@phn-4066', '@salesPortal', '@nomenclature'],
}, () => {
  test(`[${PHN_4066_JIRA_KEY}]: ${PHN_4066_TITLE}`, async ({
    SPRequest,
    TestRunSummary,
    Responses,
  }) => {
    const exactTrue = await test.step(
      'GET POPULATED_PLACES/filter exactMatch=true (ticket query)',
      async () => {
        const result = await getPopulatedPlacesFilter(SPRequest, true);
        TestRunSummary.registerPayload('exactMatchTrueQuery', result.params);
        return result;
      },
    );

    const exactFalse = await test.step(
      'GET POPULATED_PLACES/filter exactMatch=false (contrast counts only)',
      async () => {
        const result = await getPopulatedPlacesFilter(SPRequest, false);
        TestRunSummary.registerPayload('exactMatchFalseQuery', result.params);
        return result;
      },
    );

    const exactTrueTotal = readTotalElements(exactTrue.body);
    const exactFalseTotal = readTotalElements(exactFalse.body);
    const firstNamesTrue = firstPageNames(exactTrue.body);
    const firstNamesFalse = firstPageNames(exactFalse.body);
    const singleOwnName = populatedPlaceOwnName(firstNamesTrue[0] ?? '');
    const ownNameIsVarna = namesEqualIgnoreCaseTrim(singleOwnName, EXPECTED_EXACT_PLACE_NAME);
    const avrenOnFirstPage = firstPageContainsAvrenVillage(firstNamesTrue);
    const exactMatchHonored = exactTrueTotal === 1;
    const onlyCityVarna =
      exactTrueTotal === 1 && firstNamesTrue.length === 1 && ownNameIsVarna && !avrenOnFirstPage;

    await test.step('Record OpenAPI, reporter, and wiki checks', async () => {
      TestRunSummary.recordCheck({
        check: 'exactMatch=true honors OpenAPI exact prompt match',
        expectedResult:
          'OpenAPI NomenclatureItemsBaseFilterRequest.exactMatch: When true, prompt must match exactly ' +
          `(not %prompt% contains). GET filter?prompt=${TICKET_PROMPT}&exactMatch=true must not return contains-matches.`,
        actualResult: exactMatchHonored
          ? `As expected — HTTP ${exactTrue.status}; totalElements=${exactTrueTotal}.`
          : `Not as expected — HTTP ${exactTrue.status}; totalElements=${exactTrueTotal} ` +
            `(runtime ignores exactMatch / wraps %${TICKET_PROMPT}%; OpenAPI requires exact match).`,
        passed: exactMatchHonored,
      });

      TestRunSummary.recordCheck({
        check: 'exactMatch=true returns only the city Варна (place-name segment)',
        expectedResult:
          'Reporter: one result — the city Варна. Exact match on the populated place’s own name ' +
          '(segment before " - "), case-insensitive trim. First page must not list Avren villages ' +
          `such as ${AVREN_VILLAGE_EXAMPLE}.`,
        actualResult: onlyCityVarna
          ? `As expected — totalElements=1; ownName=${singleOwnName}.`
          : `Not as expected — totalElements=${exactTrueTotal}; firstPageCount=${firstNamesTrue.length}; ` +
            `firstOwnName=${singleOwnName || '(empty)'}; avrenOnFirstPage=${avrenOnFirstPage}; ` +
            `firstNames=${firstNamesTrue.slice(0, 5).join(' | ')}.`,
        passed: onlyCityVarna,
      });

      TestRunSummary.recordCheck({
        check: 'Confluence has no /filter or exactMatch rule',
        expectedResult:
          'Wiki dump APIs do not define /filter or exactMatch: GET list by nomenclature name (711229442), ' +
          'GET Nomenclature list (713195521), GET Detail by nomenclature name+ID (715849729). ' +
          'CQL exactMatch AND nomenclature returned 0 pages. Do not claim Confluence mandates exactMatch.',
        actualResult:
          'As documented — wiki is silent on /filter; this test asserts OpenAPI + reporter only.',
        passed: true,
      });
    });

    await test.step('Attach test run summary', async () => {
      finalizeTestRunSummary(TestRunSummary, Responses, {
        jiraKey: PHN_4066_JIRA_KEY,
        relevantEntityKeys: [],
        extraLinks: {
          swaggerUi: [SALES_PORTAL_SWAGGER_UI_URL],
        },
        snapshot: {
          exactTrueTotal,
          exactFalseTotal,
          firstNamesTrue,
          exactFalseFirstNames: firstNamesFalse.slice(0, 5),
        },
      });
    });

    await test.step('Assert expected exactMatch=true (OpenAPI + reporter; RED until fixed)', async () => {
      expect(
        exactTrueTotal,
        `exactMatch=true must return totalElements=1 (city ${EXPECTED_EXACT_PLACE_NAME}), got ${exactTrueTotal}. ` +
          `Contrast exactMatch=false totalElements=${exactFalseTotal}. First page: ${firstNamesTrue.slice(0, 5).join(' | ')}`,
      ).toBe(1);

      expect(
        firstNamesTrue.length,
        `exactMatch=true first page must contain exactly one row when totalElements=1, got ${firstNamesTrue.length}`,
      ).toBe(1);

      expect(
        ownNameIsVarna,
        `Single row place segment (before " - ") must equal ${EXPECTED_EXACT_PLACE_NAME} case-insensitive, got "${singleOwnName}" from "${firstNamesTrue[0] ?? ''}"`,
      ).toBe(true);

      expect(
        avrenOnFirstPage,
        `First page must not contain Avren villages such as ${AVREN_VILLAGE_EXAMPLE}. Names: ${firstNamesTrue.join(' | ')}`,
      ).toBe(false);
    });
  });
});
