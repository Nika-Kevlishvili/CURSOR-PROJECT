/**
 * PHN-4067 — Sales Portal API Transliteration values required.
 *
 * Bug-only automation (no TC .md). Jira: PHN-4067. Reproduce lives in customfield_10103.
 *
 * Expected: Sales Portal POST /pod/create must not require transliteration (*Trsl)
 * fields, especially for ID-backed address fields. Cyrillic free-text (name,
 * additionalInformation, entrance, etc.) must not trigger
 * "Transliteration field must be filled when source field is filled".
 * Success: HTTP 201 PodResponse { id, podDetailId, versionId }.
 *
 * Original actual: ILLEGAL_ARGUMENTS_PROVIDED listing many *Trsl pairing errors.
 * Jira ENV is TEST2. Numeric address ids in customfield_10103 are TEST2 (not Dev
 * and not Dev2) — invalid for Dev2 POD create. This test targets Dev2 Sales Portal
 * http://10.236.20.11:7092 and resolves nomenclature ids at runtime only.
 *
 * Criterion-7 exception: system nomenclature resolved at runtime on Dev2 (see fixtures).
 * Do not hardcode numeric ids (not TEST2 ticket ids, not Dev2 ids). Do not reuse
 * identifier 32Z4000010002000 (TEST2 ticket) or 32Z4255642504607. Do not send any *Trsl keys.
 * POPULATED_PLACES prompt София uses exactMatch=true (city own-name СОФИЯ), not
 * contains-match villages such as БАЛША that have no zip 1000.
 *
 * SPRequest: use absolute URLs (see fixtures). Relative pod/create 404s under /sales-portal/.
 *
 * Target env: Dev2.
 *
 * Reference spec(s):
 * - tests/contractsAndOrders/productContract.spec.ts
 * - tests/cursor/PHN-4066-sales-portal-populated-place-wrong-filter.spec.ts
 * - tests/cursor/PHN-4002-product-contract-sales-channel-filter.spec.ts
 */

import { test, expect } from './cursor-test.fixtures';
import { finalizeTestRunSummary } from './shared/manual-verification-links.fixtures';
import {
  PHN_4067_JIRA_KEY,
  PHN_4067_TITLE,
  POD_BY_IDENTIFIER_URL,
  POD_CREATE_URL,
  SALES_PORTAL_SWAGGER_UI_URL,
  TRANSLITERATION_REQUIRED_FRAGMENT,
  bodyTextContainsTransliterationError,
  buildPodCreatePayloadWithoutTrsl,
  buildPodIdentifier,
  collectKeysContainingTrsl,
  resolveGridOperator,
  resolveMeasurementType,
  resolveOptionalStreetNomenclature,
  resolveSofiaAddressChain,
  resolveZipCodeForPlace,
  assertResolvedAddressIdsAreNotTest2,
} from './phn-4067-sales-portal-pod-create-transliteration.fixtures';

test.describe(`[${PHN_4067_JIRA_KEY}]: ${PHN_4067_TITLE}`, {
  tag: ['@phn-4067', '@salesPortal'],
}, () => {
  test(`[${PHN_4067_JIRA_KEY}]: ${PHN_4067_TITLE}`, async ({
    SPRequest,
    TestRunSummary,
    Responses,
  }) => {
    test.setTimeout(3 * 60 * 1000);

    const gridOperator = await test.step(
      'Precondition: resolve ACTIVE grid operator (prompt ЕРП Север, prefer code 3)',
      async () => {
        const result = await resolveGridOperator(SPRequest);
        TestRunSummary.registerPayload('gridOperatorFilter', result.filterParams);
        expect(result.detail.id, 'Resolved grid operator id must be a positive number').toBeGreaterThan(0);
        return result.detail;
      },
    );

    const measurementType = await test.step(
      'Precondition: resolve ACTIVE measurement type H02 for the chosen grid operator',
      async () => {
        const result = await resolveMeasurementType(SPRequest, Number(gridOperator.id));
        TestRunSummary.registerPayload('measurementTypeFilter', result.filterParams);
        expect(result.detail.id, 'Resolved measurement type id must be a positive number').toBeGreaterThan(0);
        return result.detail;
      },
    );

    const addressChain = await test.step(
      'Precondition: resolve city София (POPULATED_PLACES exactMatch=true) with ACTIVE parents',
      async () => {
        const result = await resolveSofiaAddressChain(SPRequest);
        TestRunSummary.registerPayload('populatedPlaceFilter', result.filterParams);
        expect(result.chain.populatedPlaceId, 'populatedPlaceId must be resolved at runtime').toBeGreaterThan(0);
        expect(result.chain.municipalityId, 'municipalityId must be resolved at runtime').toBeGreaterThan(0);
        expect(result.chain.regionId, 'regionId must be resolved at runtime').toBeGreaterThan(0);
        expect(result.chain.countryId, 'countryId must be resolved at runtime').toBeGreaterThan(0);
        return result.chain;
      },
    );

    const zip = await test.step(
      'Precondition: resolve ACTIVE zip code 1000 for the chosen populated place',
      async () => {
        const result = await resolveZipCodeForPlace(
          SPRequest,
          addressChain.populatedPlaceId,
          addressChain.populatedPlaceName,
        );
        TestRunSummary.registerPayload('zipCodeFilter', result.filterParams);
        expect(result.zipCodeId).toBeGreaterThan(0);
        return result;
      },
    );

    const optionalStreet = await test.step(
      'Precondition: optionally resolve DISTRICT / RESIDENTIAL_AREA / STREET for the place',
      async () => {
        return resolveOptionalStreetNomenclature(SPRequest, addressChain.populatedPlaceId);
      },
    );

    const identifier = buildPodIdentifier(String(gridOperator.gridOperatorCode));

    await test.step('Assert resolved address ids are not TEST2 ticket ids', async () => {
      assertResolvedAddressIdsAreNotTest2({
        populatedPlaceId: addressChain.populatedPlaceId,
        zipCodeId: zip.zipCodeId,
        regionId: addressChain.regionId,
        municipalityId: addressChain.municipalityId,
        districtId: optionalStreet.districtId,
        residentialAreaId: optionalStreet.residentialAreaId,
        streetId: optionalStreet.streetId,
        identifier,
      });
      TestRunSummary.recordCheck({
        check: 'Resolved address ids are Dev2, not TEST2 ticket ids',
        expectedResult:
          'populatedPlaceId, zipCodeId, regionId, municipalityId, and any optional district / ' +
          'residentialArea / street ids (plus identifier) must not equal Jira TEST2 customfield_10103 values.',
        actualResult:
          'As expected — Dev2-resolved ids differ from TEST2 ticket address ids ' +
          `(place=${addressChain.populatedPlaceId}, zip=${zip.zipCodeId}, identifier=${identifier}).`,
        passed: true,
      });
    });
    const payload = buildPodCreatePayloadWithoutTrsl({
      identifier,
      gridOperatorId: Number(gridOperator.id),
      measurementTypeId: Number(measurementType.id),
      chain: addressChain,
      zipCodeId: zip.zipCodeId,
      optional: optionalStreet,
    });

    const trslKeys = collectKeysContainingTrsl(payload);
    TestRunSummary.registerPayload('podCreate', payload);

    await test.step('Assert create payload has zero keys containing Trsl', async () => {
      const passed = trslKeys.length === 0;
      TestRunSummary.recordCheck({
        check: 'Create payload has no *Trsl fields',
        expectedResult:
          'PodCreateRequest / PodAddressRequest / PODLocalAddressData must omit every *Trsl key ' +
          '(PHN-4067: transliteration is not required, including ID-backed address fields).',
        actualResult: passed
          ? 'As expected — payload contains zero keys whose name includes Trsl.'
          : `Not as expected — Trsl keys present: ${trslKeys.join(', ')}`,
        passed,
      });
      expect(trslKeys, `Payload must not contain *Trsl keys: ${trslKeys.join(', ')}`).toEqual([]);
    });

    const createResult = await test.step('POST /pod/create without *Trsl (absolute URL)', async () => {
      const response = await SPRequest.post(POD_CREATE_URL, { data: payload });
      await expect(response).CheckResponse();
      const status = response.status();
      const body = (await response.json()) as {
        id?: number;
        podDetailId?: number;
        versionId?: number;
        errorCode?: string;
        message?: string;
      };
      const bodyHasTrslError = bodyTextContainsTransliterationError(body);
      expect(
        status,
        `POST /pod/create must return 201, got ${status}. Body: ${JSON.stringify(body)}`,
      ).toBe(201);
      expect(
        bodyHasTrslError,
        `Create response must not contain "${TRANSLITERATION_REQUIRED_FRAGMENT}". Body: ${JSON.stringify(body)}`,
      ).toBe(false);
      const podId = Number(body.id);
      const podDetailId = body.podDetailId == null ? undefined : Number(body.podDetailId);
      const versionId = body.versionId == null ? undefined : Number(body.versionId);
      expect(podId, `Create body.id must be a number, got ${JSON.stringify(body)}`).toBeGreaterThan(0);
      if (body.podDetailId != null) {
        expect(
          podDetailId,
          `Create body.podDetailId must be numeric when present, got ${JSON.stringify(body)}`,
        ).toBeGreaterThan(0);
      }
      if (body.versionId != null) {
        expect(
          versionId,
          `Create body.versionId must be numeric when present, got ${JSON.stringify(body)}`,
        ).toBeGreaterThan(0);
      }
      Responses.pod.push({
        ...body,
        id: podId,
        identifier,
        podIdentifier: identifier,
      });
      return { status, body, podId, podDetailId, versionId, bodyHasTrslError };
    });

    TestRunSummary.recordCheck({
      check: 'POST /pod/create returns 201',
      expectedResult:
        'Sales Portal POST /pod/create (OpenAPI 201 PodResponse) succeeds without *Trsl fields. ' +
        'Non-transliteration 400s must fail the test (not treated as a pass).',
      actualResult:
        createResult.status === 201
          ? `As expected — HTTP 201; id=${createResult.podId}; podDetailId=${createResult.body.podDetailId ?? 'n/a'}.`
          : `Not as expected — HTTP ${createResult.status}; body=${JSON.stringify(createResult.body)}`,
      passed: createResult.status === 201,
    });

    TestRunSummary.recordCheck({
      check: 'Create response has no transliteration pairing error',
      expectedResult:
        `Response body must not contain "${TRANSLITERATION_REQUIRED_FRAGMENT}". ` +
        'Cyrillic free-text and ID-backed address fields must not require *Trsl.',
      actualResult: createResult.bodyHasTrslError
        ? `Not as expected — transliteration error present: ${JSON.stringify(createResult.body)}`
        : 'As expected — no transliteration required-field error in the create response.',
      passed: !createResult.bodyHasTrslError,
    });

    const getByIdentifier = await test.step('GET /pod/by-identifier for the created POD', async () => {
      const response = await SPRequest.get(POD_BY_IDENTIFIER_URL, {
        params: { identifier },
      });
      await expect(response).CheckResponse();
      const body = (await response.json()) as {
        podId?: number;
        podDetailId?: number;
        podIdentifier?: string;
        podStatus?: string;
        podName?: string;
      };
      const foundId = Number(body.podId);
      const identifierMatches = String(body.podIdentifier ?? '') === identifier;
      expect(
        identifierMatches,
        `GET /pod/by-identifier podIdentifier must equal ${identifier}. Body: ${JSON.stringify(body)}`,
      ).toBe(true);
      expect(
        foundId,
        `GET /pod/by-identifier podId must equal created id ${createResult.podId}. Body: ${JSON.stringify(body)}`,
      ).toBe(createResult.podId);
      expect(
        bodyTextContainsTransliterationError(body),
        `GET by-identifier must not contain "${TRANSLITERATION_REQUIRED_FRAGMENT}"`,
      ).toBe(false);
      return { body, foundId, identifierMatches };
    });

    TestRunSummary.recordCheck({
      check: 'GET /pod/by-identifier finds the created POD',
      expectedResult:
        'GET /pod/by-identifier?identifier=<new identifier> returns 200 SalesPortalPodResponse ' +
        'with podIdentifier / podId for the POD just created.',
      actualResult:
        getByIdentifier.identifierMatches && getByIdentifier.foundId === createResult.podId
          ? `As expected — podIdentifier=${getByIdentifier.body.podIdentifier}; podId=${getByIdentifier.body.podId}.`
          : `Not as expected — body=${JSON.stringify(getByIdentifier.body)}`,
      passed: getByIdentifier.identifierMatches && getByIdentifier.foundId === createResult.podId,
    });

    await test.step('Attach test run summary', async () => {
      finalizeTestRunSummary(TestRunSummary, Responses, {
        jiraKey: PHN_4067_JIRA_KEY,
        relevantEntityKeys: ['pod'],
        extraLinks: {
          swaggerUi: [SALES_PORTAL_SWAGGER_UI_URL],
        },
        snapshot: {
          identifier,
          podId: createResult.podId,
          podDetailId: createResult.body.podDetailId,
          gridOperatorId: gridOperator.id,
          gridOperatorCode: gridOperator.gridOperatorCode,
          measurementTypeId: measurementType.id,
          populatedPlaceId: addressChain.populatedPlaceId,
          populatedPlaceName: addressChain.populatedPlaceName,
          zipCodeId: zip.zipCodeId,
        },
      });
    });
  });
});
