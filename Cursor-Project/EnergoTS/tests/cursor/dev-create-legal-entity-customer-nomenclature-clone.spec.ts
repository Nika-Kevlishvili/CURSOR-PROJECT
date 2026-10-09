/**
 * DEV-DATA — Create LEGAL_ENTITY customer cloning nomenclature IDs from a successful
 * Dev POST /customer (2026-08-04 ES/DB logs). Only customerIdentifier changes per run.
 *
 * Swagger: POST /customer → CreateCustomerRequest (dev swagger-spec.json).
 * Original logged identifier 8347236252 is never reused.
 *
 * Reference spec(s):
 * - tests/cursor/PDT-2906-valeri-async-apply.spec.ts (cursor-test.fixtures + customer_legal POST)
 * - tests/cursor/PHN-2865-manager-phones-emails.spec.ts (POST /customer + GET /customer/{id})
 * - tests/cursor/dev-volume-invoice-custom-template.spec.ts (DEV-DATA naming + finalizeTestRunSummary)
 * - jsons/payloads/create/customer/customerLegal.ts (LEGAL_ENTITY payload shape)
 */

import { test, expect } from './cursor-test.fixtures';
import { finalizeTestRunSummary } from './shared/manual-verification-links.fixtures';
import { randomGens } from '../../utils/randomGens';

const DEV_DATA_KEY = 'DEV-DATA';

/** Nomenclature / field values from successful Dev create (2026-08-04). */
const DEV_CLONE = {
  name: '12',
  legalFormId: 1000,
  legalFormTransId: 1000,
  ownershipFormId: 1006,
  economicBranchId: 1006,
  segmentIds: [1013] as number[],
  mainSubjectOfActivity: 'FSA',
  address: {
    countryId: 1027,
    regionId: 1023,
    municipalityId: 1053,
    populatedPlaceId: 6667,
    zipCodeId: 6623,
  },
  contactPurposeIds: [83, 84] as number[],
  mobile: '11',
  email: 'FASf@dgj.dfgh',
} as const;

/**
 * Unique LEGAL_ENTITY identifier each run (timestamp + random digits).
 * Never reuses the original logged UIC 8347236252.
 */
function generateUniqueLegalEntityIdentifier(): string {
  const id = randomGens.generateUniqueIdentifier();
  if (id === '8347236252') {
    return `${id}${Math.floor(Math.random() * 90 + 10)}`;
  }
  return id;
}

/** Local address: only Dev-logged IDs; street/district/residential omitted (null in source create). */
function buildDevCloneLocalAddressData() {
  return {
    countryId: DEV_CLONE.address.countryId,
    regionId: DEV_CLONE.address.regionId,
    municipalityId: DEV_CLONE.address.municipalityId,
    populatedPlaceId: DEV_CLONE.address.populatedPlaceId,
    zipCodeId: DEV_CLONE.address.zipCodeId,
  };
}

function buildDevCloneAddress() {
  return {
    foreign: false,
    localAddressData: buildDevCloneLocalAddressData(),
    number: null,
    additionalInformation: null,
    block: null,
    entrance: null,
    floor: null,
    apartment: null,
    mailbox: null,
  };
}

/**
 * Start from GeneratePayload.customers.customer_legal(), then override to match Dev clone.
 * Swagger CreateCustomerRequest required: customerType, customerIdentifier, foreign,
 * marketingConsent, customerDetailStatus.
 */
function buildDevNomenclatureClonePayload(GeneratePayload: {
  customers: { customer_legal: () => Record<string, any> };
}): { payload: Record<string, any>; customerIdentifier: string } {
  const payload = GeneratePayload.customers.customer_legal() as Record<string, any>;
  const customerIdentifier = generateUniqueLegalEntityIdentifier();

  payload.customerType = 'LEGAL_ENTITY';
  payload.customerIdentifier = customerIdentifier;
  payload.foreign = true;
  payload.marketingConsent = false;
  payload.preferCommunicationInEnglish = false;
  payload.businessActivity = false;
  payload.oldCustomerNumber = null;
  payload.vatNumber = null;
  payload.customerDetailStatus = 'NEW';

  payload.businessCustomerDetails = {
    procurementLaw: false,
    name: DEV_CLONE.name,
    nameTranslated: DEV_CLONE.name,
    legalFormId: DEV_CLONE.legalFormId,
    legalFormTransId: DEV_CLONE.legalFormTransId,
  };

  payload.ownershipFormId = DEV_CLONE.ownershipFormId;
  payload.economicBranchId = DEV_CLONE.economicBranchId;
  payload.economicBranchNCEAId = null;
  payload.mainSubjectOfActivity = DEV_CLONE.mainSubjectOfActivity;
  payload.segmentIds = [...DEV_CLONE.segmentIds];

  const address = buildDevCloneAddress();
  payload.address = address;

  payload.bankingDetails = {
    directDebit: false,
    bankId: null,
    bic: null,
    iban: null,
    declaredConsumption: null,
    preferenceIds: [],
    creditRatingId: null,
  };

  // Managers were not present in the reconstructed Dev create payload.
  payload.managers = [];
  payload.relatedCustomers = null;
  payload.owner = null;
  payload.accountManagers = null;
  // Not on CreateCustomerRequest (Swagger) — drop generator leftover
  delete payload.customerEditContractRequests;

  payload.communicationData = [
    {
      status: 'ACTIVE',
      contactTypeName: 'MAIL',
      contactPurposes: DEV_CLONE.contactPurposeIds.map((contactPurposeId) => ({
        contactPurposeId,
        status: 'ACTIVE',
      })),
      address: buildDevCloneAddress(),
      communicationContacts: [
        {
          sendSms: false,
          platformId: null,
          status: 'ACTIVE',
          contactType: 'MOBILE_NUMBER',
          contactValue: DEV_CLONE.mobile,
        },
        {
          sendSms: false,
          platformId: null,
          status: 'ACTIVE',
          contactType: 'EMAIL',
          contactValue: DEV_CLONE.email,
        },
      ],
      contactPersons: [],
    },
  ];

  return { payload, customerIdentifier };
}

function resolveCustomerDetail(view: Record<string, any>): Record<string, any> | null {
  if (view.activeCustomerDetail) return view.activeCustomerDetail;
  if (view.customerDetail) return view.customerDetail;
  if (Array.isArray(view.customerDetails) && view.customerDetails.length > 0) {
    const list = view.customerDetails as Record<string, any>[];
    const active = list.find((x) => x.active === true || x.current === true);
    if (active) return active;
    return list.reduce((a, b) => ((b.versionId ?? 0) > (a.versionId ?? 0) ? b : a));
  }
  return null;
}

test.describe('[DEV-DATA]: Create LEGAL_ENTITY customer (Dev nomenclature clone)', {
  tag: ['@customers', '@dev-data'],
}, () => {
  test(
    '[DEV-DATA]: Create LEGAL_ENTITY customer with Dev nomenclature clone (unique identifier)',
    async ({ Request, GeneratePayload, Responses, Endpoints, TestRunSummary }) => {
      let customerIdentifier = '';
      let customerId = 0;

      await test.step('POST /customer with Dev nomenclature clone (unique identifier)', async () => {
        const built = buildDevNomenclatureClonePayload(GeneratePayload);
        customerIdentifier = built.customerIdentifier;
        expect(customerIdentifier).not.toBe('8347236252');

        TestRunSummary.registerPayload('customer', built.payload);

        const response = await Request.post(Endpoints.customer, { data: built.payload });
        await expect(response).CheckResponse();
        const created = await response.json();
        Responses.customer.push(created);
        customerId = created.id ?? created.customerId;

        expect(customerId, 'created customer id').toBeGreaterThan(0);
        expect(
          created.identifier ?? created.customerIdentifier,
          'create response identifier',
        ).toBe(customerIdentifier);

        TestRunSummary.recordCheck({
          check: 'POST /customer LEGAL_ENTITY Dev nomenclature clone',
          expectedResult:
            '200/OK via CheckResponse; customer created with unique identifier (not 8347236252) and Dev nomenclature IDs.',
          actualResult: `As expected — customerId=${customerId}; identifier=${customerIdentifier}; status=${response.status()}.`,
          passed: true,
        });
      });

      await test.step('GET /customer/{id} and assert identifier + key nomenclature fields', async () => {
        const getResponse = await Request.get(`${Endpoints.customer}/${customerId}`);
        await expect(getResponse).CheckResponse();
        const body = (await getResponse.json()) as Record<string, any>;
        const detail = resolveCustomerDetail(body) ?? body;

        const gotIdentifier =
          body.identifier ?? body.customerIdentifier ?? detail.identifier ?? detail.customerIdentifier;
        expect(gotIdentifier).toBe(customerIdentifier);

        const customerType = body.customerType ?? detail.customerType;
        expect(customerType).toBe('LEGAL_ENTITY');

        const foreign = body.foreign ?? detail.foreign;
        expect(foreign).toBe(true);

        const biz = detail.businessCustomerDetails ?? body.businessCustomerDetails ?? {};
        expect(biz.name ?? detail.name).toBe(DEV_CLONE.name);
        expect(biz.nameTranslated ?? detail.nameTranslated).toBe(DEV_CLONE.name);
        expect(biz.legalFormId ?? detail.legalFormId).toBe(DEV_CLONE.legalFormId);
        expect(biz.legalFormTransId ?? detail.legalFormTransId).toBe(DEV_CLONE.legalFormTransId);

        const ownershipFormId = detail.ownershipFormId ?? body.ownershipFormId;
        expect(ownershipFormId).toBe(DEV_CLONE.ownershipFormId);

        const economicBranchId = detail.economicBranchId ?? body.economicBranchId;
        expect(economicBranchId).toBe(DEV_CLONE.economicBranchId);

        const segmentIds = detail.segmentIds ?? body.segmentIds;
        if (Array.isArray(segmentIds)) {
          expect(segmentIds).toEqual(expect.arrayContaining(DEV_CLONE.segmentIds));
        }

        const mainSubject =
          detail.mainSubjectOfActivity ?? body.mainSubjectOfActivity;
        if (mainSubject != null) {
          expect(mainSubject).toBe(DEV_CLONE.mainSubjectOfActivity);
        }

        TestRunSummary.recordCheck({
          check: 'GET /customer/{id} nomenclature clone fields',
          expectedResult:
            'Identifier matches create; LEGAL_ENTITY; foreign=true; name/legalForm/ownership/economicBranch/segment match Dev clone.',
          actualResult: `As expected — identifier=${gotIdentifier}; type=${customerType}; foreign=${foreign}; legalFormId=${biz.legalFormId ?? detail.legalFormId}; ownershipFormId=${ownershipFormId}; economicBranchId=${economicBranchId}.`,
          passed: true,
        });
      });

      await test.step('Attach test run summary', async () => {
        finalizeTestRunSummary(TestRunSummary, Responses, {
          jiraKey: DEV_DATA_KEY,
          relevantEntityKeys: ['customer'],
          snapshot: {
            customerId,
            customerIdentifier,
            legalFormId: DEV_CLONE.legalFormId,
            ownershipFormId: DEV_CLONE.ownershipFormId,
            economicBranchId: DEV_CLONE.economicBranchId,
            segmentIds: DEV_CLONE.segmentIds,
          },
        });
      });
    },
  );
});
