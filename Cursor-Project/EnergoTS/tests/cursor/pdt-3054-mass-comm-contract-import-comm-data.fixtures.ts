/**
 * PDT-3054 — Mass Email / Mass SMS: contract-import recipient resolution from
 * contract-level communication data (billing purpose 84 / contract purpose 83).
 *
 * Reference helper sources:
 * - tests/cursor/pdt-2815-version-validity.fixtures.ts (mass import + product contract sign chain)
 * - tests/cursor/pdt-2599-service-contract.fixtures.ts (signed service contract chain)
 * - Swagger MassEmailCreateRequest / MassSmsCreateRequest (createType / saveAs enums)
 */

import * as ExcelJS from 'exceljs';
import fs from 'fs';
import path from 'path';
import { expect } from './cursor-test.fixtures';
import type { baseFixture } from '../../fixtures/baseFixture';
import { envVariables } from '../../fixtures/envCashed';
import {
  ANCHOR_DATE,
  MASS_EMAIL_CONTRACT_IMPORT_TYPE,
  MASS_SMS_CONTRACT_IMPORT_TYPE,
  addDaysIso,
  buildEditProductContractPayload,
  getEmailCommunicationPreview,
  getSmsCommunicationPreview,
  loadProductContract,
  productContractStatusUpdate,
  putProductContractNewVersion,
  resolveMassEmailCustomerPreview,
  resolveMassSmsCustomerPreview,
  uploadMassEmailContractImport,
  uploadMassSmsContractParse,
  versionDetailId,
  waitForMassEmailIndividualEmailRow,
  type EmailCommunicationPreview,
  type MassCommImportBody,
  type MassEmailImportBody,
  type MassSmsImportBody,
  type SmsCommunicationPreview,
} from './pdt-2815-version-validity.fixtures';
import {
  asPriceComponentId,
  asServiceId,
  resolvePdt2599ContractFormulaForPost,
} from './pdt-2599-service-contract.fixtures';

type FixtureRequest = baseFixture['Request'];
type FixtureFileUpload = baseFixture['FileUploadRequest'];
type Fx = {
  Request: FixtureRequest;
  FileUploadRequest: FixtureFileUpload;
  GeneratePayload: baseFixture['GeneratePayload'];
  Responses: baseFixture['Responses'];
  Endpoints: baseFixture['Endpoints'];
};

export const PDT_3054_PURPOSE_BILLING = Number(envVariables.billing_purpose ?? 84);
export const PDT_3054_PURPOSE_CONTRACT = Number(envVariables.contact_purpose ?? 83);

export const EMAIL_BILLING = 'billing.contract.test@example.com';
export const EMAIL_CONTRACT = 'contract.comm.test@example.com';
export const EMAIL_CUSTOMER_BILLING = 'billing.customer.test@example.com';
export const EMAIL_BILLING_V1 = 'old.billing@example.com';
export const EMAIL_BILLING_V2 = 'new.billing@example.com';
export const MOBILE_BILLING = '+359881000001';
export const MOBILE_CONTRACT = '+359882000002';
export const MOBILE_CUSTOMER = '+359883000003';
export const MOBILE_NO_EMAIL = '+359884000004';
export const MOBILE_PLACEHOLDER_BILLING = '+359885000005';
export const MOBILE_PLACEHOLDER_CONTRACT = '+359886000006';
export const MOBILE_PLACEHOLDER_CUSTOMER = '+359887000007';
export const MOBILE_PLACEHOLDER_V1 = '+359888000008';
export const MOBILE_PLACEHOLDER_V2 = '+359889000009';
export const EMAIL_NO_MOBILE = 'no-mobile-billing@example.com';
export const EMAIL_PLACEHOLDER_SMS_BILLING = 'sms.billing.placeholder@example.com';
export const EMAIL_PLACEHOLDER_SMS_CONTRACT = 'sms.contract.placeholder@example.com';
export const EMAIL_PLACEHOLDER_SMS_CUSTOMER = 'sms.customer.placeholder@example.com';
export const EMAIL_PLACEHOLDER_STRIP = 'strip.later.billing@example.com';

export const ERR_CONTRACT_NO_COMM_PURPOSE_84 =
  'Contract does not have communication data for purpose id 84';
export const ERR_COMM_NO_VALID_EMAIL_PREFIX =
  'Communication data (id:';
export const ERR_COMM_NO_VALID_EMAIL_SUFFIX =
  'for purpose id 84 has no valid email address';
export const ERR_COMM_NO_VALID_EMAIL_SUFFIX_83 =
  'for purpose id 83 has no valid email address';
export const ERR_COMM_NO_VALID_MOBILE_SUFFIX =
  'for purpose id 84 has no valid mobile number with SMS enabled';

export type CommContactSpec = {
  contactType: 'EMAIL' | 'MOBILE_NUMBER';
  contactValue: string;
  sendSms?: boolean;
  status?: 'ACTIVE' | 'DELETED';
  id?: number;
};

export type CommEntrySpec = {
  tag: string;
  purposeIds: number[];
  contacts: CommContactSpec[];
  /** Preserve existing communication row id on customer PUT (keeps contract FKs stable). */
  id?: number;
  status?: 'ACTIVE' | 'DELETED';
};

/**
 * Dev product/service contract create requires both EMAIL and MOBILE_NUMBER on
 * billing and contract communication data (`checkForEmailAndNumber`).
 * Email TCs keep distinctive emails; SMS TCs keep distinctive mobiles.
 */
export function contactsWithEmailAndMobile(
  email: string,
  mobile: string,
  sendSms = true,
): CommContactSpec[] {
  return [
    { contactType: 'EMAIL', contactValue: email },
    { contactType: 'MOBILE_NUMBER', contactValue: mobile, sendSms },
  ];
}

export type Pdt3054ProductScenario = {
  customerId: number;
  customerIdentifier: string;
  customerVersion: number;
  contractId: number;
  contractNumber: string;
  contractVersionId: number;
  productContractDetailId: number;
  commIds: Record<string, number>;
  /** TC-BE-7: exact error fragment after Dev-compatible billing clear / strip. */
  purpose84ExpectedError?: string;
  /** TC-BE-7: true when product-contract PUT cleared billing to null. */
  billingCleared?: boolean;
  billingClearRejectedBody?: string;
};

export type Pdt3054ServiceScenario = {
  customerId: number;
  customerIdentifier: string;
  customerVersion: number;
  contractId: number;
  contractNumber: string;
  contractVersionId: number;
  serviceContractDetailId: number;
  commIds: Record<string, number>;
};

function cloneCommAddress(from: Record<string, unknown>): Record<string, unknown> {
  return JSON.parse(JSON.stringify(from));
}

function buildCommEntry(
  baseAddress: Record<string, unknown>,
  spec: CommEntrySpec,
): Record<string, unknown> {
  const entry: Record<string, unknown> = {
    status: spec.status ?? 'ACTIVE',
    contactTypeName: `PDT-3054 ${spec.tag}`,
    contactPurposes: spec.purposeIds.map((contactPurposeId) => ({
      contactPurposeId,
      status: 'ACTIVE',
    })),
    address: cloneCommAddress(baseAddress),
    communicationContacts: spec.contacts.map((c) => {
      const contact: Record<string, unknown> = {
        sendSms: c.contactType === 'MOBILE_NUMBER' ? c.sendSms !== false : false,
        platformId: null,
        status: c.status ?? 'ACTIVE',
        contactType: c.contactType,
        contactValue: c.contactValue,
      };
      if (c.id != null) contact.id = c.id;
      return contact;
    }),
    contactPersons: [],
  };
  if (spec.id != null) entry.id = spec.id;
  return entry;
}

/** Build customer create payload with one or more purpose-scoped communication data rows. */
export function buildPrivateCustomerWithComms(
  GeneratePayload: baseFixture['GeneratePayload'],
  entries: CommEntrySpec[],
): Record<string, unknown> {
  const template = GeneratePayload.customers.customer_private() as Record<string, unknown>;
  const defaultComm = (template.communicationData as Record<string, unknown>[])?.[0];
  const baseAddress =
    (defaultComm?.address as Record<string, unknown>) ??
    (template.address as Record<string, unknown>);
  template.communicationData = entries.map((e) => buildCommEntry(baseAddress, e));
  return template;
}

export async function getCustomerJson(
  Request: FixtureRequest,
  Endpoints: baseFixture['Endpoints'],
  customerId: number,
  version = 1,
): Promise<Record<string, unknown>> {
  const res = await Request.get(`${Endpoints.customer}/${customerId}?version=${version}`);
  await expect(res).CheckResponse();
  return (await res.json()) as Record<string, unknown>;
}

export function mapCommIdsByTag(
  customerJson: Record<string, unknown>,
  expectedTags: string[],
): Record<string, number> {
  const comms = (customerJson.communicationData as { id?: number; contactTypeName?: string }[]) ?? [];
  const out: Record<string, number> = {};
  for (const tag of expectedTags) {
    const row = comms.find((c) => String(c.contactTypeName ?? '').includes(tag));
    expect(row?.id, `communication data tagged ${tag} must exist`).toBeTruthy();
    out[tag] = Number(row!.id);
  }
  return out;
}

export async function createCustomerWithComms(
  fx: Fx,
  entries: CommEntrySpec[],
): Promise<{
  customerId: number;
  customerIdentifier: string;
  customerVersion: number;
  customerJson: Record<string, unknown>;
  createPayload: Record<string, unknown>;
  commIds: Record<string, number>;
}> {
  const { Request, GeneratePayload, Responses, Endpoints } = fx;
  const payload = buildPrivateCustomerWithComms(GeneratePayload, entries);
  const res = await Request.post(Endpoints.customer, { data: payload });
  await expect(res).CheckResponse();
  const created = await res.json();
  Responses.customer.push(created);
  const customerId = Number(created.id);
  const customerJson = await getCustomerJson(Request, Endpoints, customerId, 1);
  const customerIdentifier = String(
    customerJson.customerIdentifier ?? payload.customerIdentifier ?? created.identifier ?? '',
  );
  const customerVersion = Number(
    customerJson.customerVersionId ?? customerJson.versionId ?? 1,
  );
  const commIds = mapCommIdsByTag(
    customerJson,
    entries.map((e) => e.tag),
  );
  return {
    customerId,
    customerIdentifier,
    customerVersion,
    customerJson,
    createPayload: payload,
    commIds,
  };
}

export async function putCustomerReplaceComms(
  fx: Fx,
  customerId: number,
  fromVersion: number,
  createTemplate: Record<string, unknown>,
  entries: CommEntrySpec[],
  updateExistingVersion = true,
): Promise<{ customerJson: Record<string, unknown>; commIds: Record<string, number> }> {
  const { Request, Endpoints } = fx;
  const baseAddress =
    ((createTemplate.communicationData as Record<string, unknown>[])?.[0]
      ?.address as Record<string, unknown>) ??
    (createTemplate.address as Record<string, unknown>);

  const editPayload = {
    customerDetailsVersion: fromVersion,
    updateExistingVersion,
    customerType: createTemplate.customerType,
    customerIdentifier: createTemplate.customerIdentifier,
    foreign: createTemplate.foreign,
    marketingConsent: createTemplate.marketingConsent,
    customerDetailStatus: 'ACTIVE',
    businessActivity: createTemplate.businessActivity,
    preferCommunicationInEnglish: createTemplate.preferCommunicationInEnglish ?? false,
    privateCustomerDetails: createTemplate.privateCustomerDetails,
    segmentIds: createTemplate.segmentIds,
    address: createTemplate.address,
    bankingDetails: createTemplate.bankingDetails,
    communicationData: entries.map((e) => buildCommEntry(baseAddress, e)),
    accountManagers: [],
    managers: [],
    relatedCustomers: [],
    owner: [],
    customerEditContractRequests: [],
  };

  const put = await Request.put(`${Endpoints.customer}/${customerId}`, { data: editPayload });
  await expect(put).CheckResponse();
  const version = updateExistingVersion ? fromVersion : fromVersion + 1;
  const customerJson = await getCustomerJson(Request, Endpoints, customerId, version);
  const commIds = mapCommIdsByTag(
    customerJson,
    entries.map((e) => e.tag),
  );
  return { customerJson, commIds };
}

export async function createCatalogChain(fx: Fx): Promise<void> {
  const { Request, GeneratePayload, Responses, Endpoints } = fx;

  const term = await Request.post(Endpoints.terms, {
    data: GeneratePayload.productAndServices.term(),
  });
  await expect(term).CheckResponse();
  Responses.terms.push(await term.json());

  const price = await Request.post(Endpoints.priceComponent, {
    data: GeneratePayload.productAndServices.electricity(),
  });
  await expect(price).CheckResponse();
  Responses.priceComponent.push(await price.json());

  const productPayload = GeneratePayload.productAndServices.product();
  productPayload.contractTypes = ['SUPPLY_ONLY'];
  productPayload.paymentGuarantees = ['NO'];
  const product = await Request.post(Endpoints.product, { data: productPayload });
  await expect(product).CheckResponse();
  Responses.product.push(await product.json());

  const pod = await Request.post(Endpoints.pod, {
    data: GeneratePayload.pointsOfDelivery.pod_settlement(),
  });
  await expect(pod).CheckResponse();
  Responses.pod.push(await pod.json());
}

export async function createSignedProductContractWithComms(
  fx: Fx,
  opts: {
    billingCommId: number;
    contractCommId: number;
  },
): Promise<{
  contractId: number;
  contractNumber: string;
  contractVersionId: number;
  productContractDetailId: number;
  contractPayload: Record<string, unknown>;
}> {
  const { Request, GeneratePayload, Responses, Endpoints } = fx;
  const productIndex = Math.max(0, Responses.product.length - 1);
  const contractPayload = (await GeneratePayload.contractsAndOrders.product_contract(
    0,
    productIndex,
  )) as Record<string, unknown>;
  const bp = contractPayload.basicParameters as Record<string, unknown>;
  bp.status = 'DRAFT';
  bp.subStatus = 'DRAFT';
  bp.signingDate = null;
  // Dev requires communicationDataBillingId (Swagger required + Email+Mobile on both comm rows).
  bp.communicationDataBillingId = opts.billingCommId;
  bp.communicationDataContractId = opts.contractCommId;
  (contractPayload.productParameters as Record<string, unknown>).contractType = 'SUPPLY_ONLY';

  const res = await Request.post(Endpoints.productContract, { data: contractPayload });
  await expect(res).CheckResponse();
  const body = await res.json();
  Responses.productContract.push(body);
  const contractId = Number(body.id);

  const ready = await productContractStatusUpdate(Request, contractId, 'READY', 'READY', 1);
  await expect(ready).CheckResponse();
  const signed = await productContractStatusUpdate(
    Request,
    contractId,
    'SIGNED',
    'SIGNED_BY_BOTH_SIDES',
    1,
  );
  await expect(signed).CheckResponse();

  const editPayload = await buildEditProductContractPayload(Request, contractId, contractPayload, {
    savingAsNewVersion: false,
    preserveSigningDate: false,
  });
  const editBp = editPayload.basicParameters as Record<string, unknown>;
  editBp.communicationDataBillingId = opts.billingCommId;
  editBp.communicationDataContractId = opts.contractCommId;

  const put = await Request.put(
    `${Endpoints.productContract}/${contractId}?versionId=1&changeFutureVersionsPods=false`,
    { data: editPayload },
  );
  await expect(put).CheckResponse();

  const loaded = await loadProductContract(Request, contractId);
  const contractNumber = String(
    (loaded.basicParameters as { contractNumber?: string })?.contractNumber ?? '',
  );
  const productContractDetailId = versionDetailId(loaded, 1);
  expect(productContractDetailId, 'signed v1 productContractDetailId').toBeTruthy();

  return {
    contractId,
    contractNumber,
    contractVersionId: 1,
    productContractDetailId: productContractDetailId!,
    contractPayload,
  };
}

/**
 * After contract is signed: remove EMAIL / MOBILE / SMS flag on a tagged communication
 * row (TC-BE-8/9 Comm-A; TC-BE-16 Comm-B). Contract FKs keep the same communication id.
 */
export async function stripCommContactTypeAfterSign(
  fx: Fx,
  opts: {
    customerId: number;
    customerVersion: number;
    createTemplate: Record<string, unknown>;
    allEntries: CommEntrySpec[];
    commIds: Record<string, number>;
    tag: string;
    strip: 'EMAIL' | 'MOBILE_NUMBER' | 'SMS_FLAG';
  },
): Promise<{ customerVersion: number; commIds: Record<string, number> }> {
  const targetId = opts.commIds[opts.tag];
  expect(targetId, `${opts.tag} id before strip`).toBeTruthy();

  const mutatedEntries: CommEntrySpec[] = opts.allEntries.map((e) => {
    const id = opts.commIds[e.tag];
    if (e.tag !== opts.tag) {
      return {
        ...e,
        id,
        contacts: e.contacts.map((c) => ({ ...c })),
      };
    }
    if (opts.strip === 'EMAIL') {
      return {
        ...e,
        id,
        contacts: e.contacts
          .filter((c) => c.contactType !== 'EMAIL')
          .map((c) => ({ ...c })),
      };
    }
    if (opts.strip === 'MOBILE_NUMBER') {
      return {
        ...e,
        id,
        contacts: e.contacts
          .filter((c) => c.contactType !== 'MOBILE_NUMBER')
          .map((c) => ({ ...c })),
      };
    }
    // SMS_FLAG: keep mobile but disable sendSms so mass SMS treats it as invalid.
    return {
      ...e,
      id,
      contacts: e.contacts.map((c) =>
        c.contactType === 'MOBILE_NUMBER' ? { ...c, sendSms: false } : { ...c },
      ),
    };
  });

  const updated = await putCustomerReplaceComms(
    fx,
    opts.customerId,
    opts.customerVersion,
    opts.createTemplate,
    mutatedEntries,
    true,
  );
  // Contract FK still points at original id — keep that id even if customer PUT renamed tags.
  const commIds = { ...updated.commIds, [opts.tag]: targetId };
  return {
    customerVersion: opts.customerVersion,
    commIds,
  };
}

/** @deprecated Prefer stripCommContactTypeAfterSign with tag 'Comm-A'. */
export async function stripBillingContactTypeAfterSign(
  fx: Fx,
  opts: {
    customerId: number;
    customerVersion: number;
    createTemplate: Record<string, unknown>;
    allEntries: CommEntrySpec[];
    commIds: Record<string, number>;
    strip: 'EMAIL' | 'MOBILE_NUMBER' | 'SMS_FLAG';
  },
): Promise<{ customerVersion: number; commIds: Record<string, number> }> {
  return stripCommContactTypeAfterSign(fx, { ...opts, tag: 'Comm-A' });
}

/**
 * TC-BE-7 Dev constraint: `communicationDataBillingId` is required on create/update (Swagger)
 * and `product_contract.contract_details.customer_communication_id_for_billing` is NOT NULL.
 * Flow: create+sign with valid billing Email+Mobile, then try PUT billing=null.
 * If PUT rejects (expected on Dev), remove Comm-A from the customer and soft-delete any
 * remaining billing contacts so the contract FK points at a non-ACTIVE / purpose-less row.
 * Exact null-FK path for `ERR_CONTRACT_NO_COMM_PURPOSE_84` is only available when PUT succeeds.
 * When PUT fails, expected mass-email error is no-valid-email for the still-linked billing id
 * (still proves no silent fallback to customer-level Comm-C).
 */
export async function clearProductContractBillingCommAfterSign(
  fx: Fx,
  opts: {
    contractId: number;
    contractPayload: Record<string, unknown>;
    contractCommId: number;
    billingCommId: number;
    customerId: number;
    customerVersion: number;
    createTemplate: Record<string, unknown>;
    entries: CommEntrySpec[];
    commIds: Record<string, number>;
  },
): Promise<{
  billingCleared: boolean;
  clearRejectedBody?: string;
  purpose84ExpectedError: string;
  commIds: Record<string, number>;
}> {
  const editPayload = await buildEditProductContractPayload(
    fx.Request,
    opts.contractId,
    opts.contractPayload,
    { savingAsNewVersion: false, preserveSigningDate: true },
  );
  const editBp = editPayload.basicParameters as Record<string, unknown>;
  editBp.communicationDataBillingId = null;
  editBp.communicationDataContractId = opts.contractCommId;

  const put = await fx.Request.put(
    `${fx.Endpoints.productContract}/${opts.contractId}?versionId=1&changeFutureVersionsPods=false`,
    { data: editPayload },
  );
  if (put.ok()) {
    return {
      billingCleared: true,
      purpose84ExpectedError: ERR_CONTRACT_NO_COMM_PURPOSE_84,
      commIds: opts.commIds,
    };
  }
  const clearRejectedBody = await put.text();

  // Remove Comm-A from customer payload (treat as deleted) while keeping Comm-B / Comm-C.
  // Contract still stores billingCommId FK → resolveEmail finds no ACTIVE email on that id.
  const keepActive = opts.entries
    .filter((e) => e.tag !== 'Comm-A')
    .map((e) => ({
      ...e,
      id: opts.commIds[e.tag],
      contacts: e.contacts.map((c) => ({ ...c })),
    }));
  const updated = await putCustomerReplaceComms(
    fx,
    opts.customerId,
    opts.customerVersion,
    opts.createTemplate,
    keepActive,
    true,
  );
  const mergedIds = { ...opts.commIds, ...updated.commIds };
  const purpose84ExpectedError = `${ERR_COMM_NO_VALID_EMAIL_PREFIX} ${opts.billingCommId}) ${ERR_COMM_NO_VALID_EMAIL_SUFFIX}`;
  return {
    billingCleared: false,
    clearRejectedBody,
    purpose84ExpectedError,
    // Prefer exact purpose-missing when resolver somehow nulls FK; fixtures expose both fragments.
    // Spec asserts purpose84ExpectedError — includes no-valid-email when PUT clear is rejected.
    commIds: mergedIds,
  };
}

/** Full product-contract scenario used by most TC-BE email/SMS cases. */
export async function buildProductContractCommScenario(
  fx: Fx,
  variant:
    | 'email-three-comms'
    | 'email-two-comms'
    | 'sms-three-comms'
    | 'no-billing-comm'
    | 'billing-no-email'
    | 'billing-no-mobile'
    | 'contract-no-email',
): Promise<Pdt3054ProductScenario> {
  await createCatalogChain(fx);

  let entries: CommEntrySpec[];
  switch (variant) {
    case 'email-three-comms':
      entries = [
        {
          tag: 'Comm-A',
          purposeIds: [PDT_3054_PURPOSE_BILLING],
          contacts: contactsWithEmailAndMobile(EMAIL_BILLING, MOBILE_PLACEHOLDER_BILLING),
        },
        {
          tag: 'Comm-B',
          purposeIds: [PDT_3054_PURPOSE_CONTRACT],
          contacts: contactsWithEmailAndMobile(EMAIL_CONTRACT, MOBILE_PLACEHOLDER_CONTRACT),
        },
        {
          tag: 'Comm-C',
          purposeIds: [PDT_3054_PURPOSE_BILLING],
          contacts: contactsWithEmailAndMobile(EMAIL_CUSTOMER_BILLING, MOBILE_PLACEHOLDER_CUSTOMER),
        },
      ];
      break;
    case 'email-two-comms':
      entries = [
        {
          tag: 'Comm-A',
          purposeIds: [PDT_3054_PURPOSE_BILLING],
          contacts: contactsWithEmailAndMobile(EMAIL_BILLING, MOBILE_PLACEHOLDER_BILLING),
        },
        {
          tag: 'Comm-B',
          purposeIds: [PDT_3054_PURPOSE_CONTRACT],
          contacts: contactsWithEmailAndMobile(EMAIL_CONTRACT, MOBILE_PLACEHOLDER_CONTRACT),
        },
      ];
      break;
    case 'sms-three-comms':
      entries = [
        {
          tag: 'Comm-A',
          purposeIds: [PDT_3054_PURPOSE_BILLING],
          contacts: contactsWithEmailAndMobile(
            EMAIL_PLACEHOLDER_SMS_BILLING,
            MOBILE_BILLING,
            true,
          ),
        },
        {
          tag: 'Comm-B',
          purposeIds: [PDT_3054_PURPOSE_CONTRACT],
          contacts: contactsWithEmailAndMobile(
            EMAIL_PLACEHOLDER_SMS_CONTRACT,
            MOBILE_CONTRACT,
            true,
          ),
        },
        {
          tag: 'Comm-C',
          purposeIds: [PDT_3054_PURPOSE_BILLING],
          contacts: contactsWithEmailAndMobile(
            EMAIL_PLACEHOLDER_SMS_CUSTOMER,
            MOBILE_CUSTOMER,
            true,
          ),
        },
      ];
      break;
    case 'no-billing-comm':
      // Temporary Comm-A billing (Email+Mobile) so product-contract create/sign succeeds on Dev;
      // cleared or removed after sign (see clearProductContractBillingCommAfterSign).
      entries = [
        {
          tag: 'Comm-A',
          purposeIds: [PDT_3054_PURPOSE_BILLING],
          contacts: contactsWithEmailAndMobile(EMAIL_PLACEHOLDER_STRIP, MOBILE_PLACEHOLDER_BILLING),
        },
        {
          tag: 'Comm-B',
          purposeIds: [PDT_3054_PURPOSE_CONTRACT],
          contacts: contactsWithEmailAndMobile(EMAIL_CONTRACT, MOBILE_PLACEHOLDER_CONTRACT),
        },
        {
          tag: 'Comm-C',
          purposeIds: [PDT_3054_PURPOSE_BILLING],
          contacts: contactsWithEmailAndMobile(EMAIL_CUSTOMER_BILLING, MOBILE_PLACEHOLDER_CUSTOMER),
        },
      ];
      break;
    case 'billing-no-email':
      // Create with dual contacts, then strip EMAIL after sign (contract create requires both).
      entries = [
        {
          tag: 'Comm-A',
          purposeIds: [PDT_3054_PURPOSE_BILLING],
          contacts: contactsWithEmailAndMobile(EMAIL_PLACEHOLDER_STRIP, MOBILE_NO_EMAIL, true),
        },
        {
          tag: 'Comm-B',
          purposeIds: [PDT_3054_PURPOSE_CONTRACT],
          contacts: contactsWithEmailAndMobile(EMAIL_CONTRACT, MOBILE_PLACEHOLDER_CONTRACT),
        },
      ];
      break;
    case 'billing-no-mobile':
      // Create with dual contacts, then strip MOBILE after sign.
      entries = [
        {
          tag: 'Comm-A',
          purposeIds: [PDT_3054_PURPOSE_BILLING],
          contacts: contactsWithEmailAndMobile(EMAIL_NO_MOBILE, MOBILE_PLACEHOLDER_BILLING, true),
        },
        {
          tag: 'Comm-B',
          purposeIds: [PDT_3054_PURPOSE_CONTRACT],
          contacts: contactsWithEmailAndMobile(
            EMAIL_PLACEHOLDER_SMS_CONTRACT,
            MOBILE_CONTRACT,
            true,
          ),
        },
      ];
      break;
    case 'contract-no-email':
      // TC-BE-16: dual contacts on Comm-A+Comm-B, then strip EMAIL from Comm-B after sign.
      entries = [
        {
          tag: 'Comm-A',
          purposeIds: [PDT_3054_PURPOSE_BILLING],
          contacts: contactsWithEmailAndMobile(EMAIL_BILLING, MOBILE_PLACEHOLDER_BILLING),
        },
        {
          tag: 'Comm-B',
          purposeIds: [PDT_3054_PURPOSE_CONTRACT],
          contacts: contactsWithEmailAndMobile(EMAIL_CONTRACT, MOBILE_PLACEHOLDER_CONTRACT),
        },
      ];
      break;
  }

  const customer = await createCustomerWithComms(fx, entries);
  const billingCommId = customer.commIds['Comm-A'];
  const contractCommId = customer.commIds['Comm-B'];
  expect(billingCommId, 'billing communication data id').toBeTruthy();
  expect(contractCommId, 'contract communication data id').toBeTruthy();

  const contract = await createSignedProductContractWithComms(fx, {
    billingCommId,
    contractCommId,
  });

  let commIds = customer.commIds;
  let purpose84ExpectedError: string | undefined;
  let billingCleared: boolean | undefined;
  let billingClearRejectedBody: string | undefined;

  if (variant === 'no-billing-comm') {
    const cleared = await clearProductContractBillingCommAfterSign(fx, {
      contractId: contract.contractId,
      contractPayload: contract.contractPayload,
      contractCommId,
      billingCommId,
      customerId: customer.customerId,
      customerVersion: customer.customerVersion,
      createTemplate: customer.createPayload,
      entries,
      commIds,
    });
    commIds = cleared.commIds;
    purpose84ExpectedError = cleared.purpose84ExpectedError;
    billingCleared = cleared.billingCleared;
    billingClearRejectedBody = cleared.clearRejectedBody;
  } else if (variant === 'billing-no-email') {
    const stripped = await stripCommContactTypeAfterSign(fx, {
      customerId: customer.customerId,
      customerVersion: customer.customerVersion,
      createTemplate: customer.createPayload,
      allEntries: entries,
      commIds,
      tag: 'Comm-A',
      strip: 'EMAIL',
    });
    commIds = stripped.commIds;
  } else if (variant === 'billing-no-mobile') {
    const stripped = await stripCommContactTypeAfterSign(fx, {
      customerId: customer.customerId,
      customerVersion: customer.customerVersion,
      createTemplate: customer.createPayload,
      allEntries: entries,
      commIds,
      tag: 'Comm-A',
      strip: 'MOBILE_NUMBER',
    });
    commIds = stripped.commIds;
  } else if (variant === 'contract-no-email') {
    const stripped = await stripCommContactTypeAfterSign(fx, {
      customerId: customer.customerId,
      customerVersion: customer.customerVersion,
      createTemplate: customer.createPayload,
      allEntries: entries,
      commIds,
      tag: 'Comm-B',
      strip: 'EMAIL',
    });
    commIds = stripped.commIds;
  }

  return {
    customerId: customer.customerId,
    customerIdentifier: customer.customerIdentifier,
    customerVersion: customer.customerVersion,
    contractId: contract.contractId,
    contractNumber: contract.contractNumber,
    contractVersionId: contract.contractVersionId,
    productContractDetailId: contract.productContractDetailId,
    commIds,
    purpose84ExpectedError,
    billingCleared,
    billingClearRejectedBody,
  };
}

/** TC-BE-6: v1 billing V1 email, v2 billing V2 email; blank version import → latest. */
export async function buildBlankVersionProductScenario(fx: Fx): Promise<
  Pdt3054ProductScenario & {
    productContractDetailIdV1: number;
    productContractDetailIdV2: number;
    versionIdV2: number;
  }
> {
  await createCatalogChain(fx);

  const entriesV1: CommEntrySpec[] = [
    {
      tag: 'Comm-B',
      purposeIds: [PDT_3054_PURPOSE_CONTRACT],
      contacts: contactsWithEmailAndMobile(EMAIL_CONTRACT, MOBILE_PLACEHOLDER_CONTRACT),
    },
    {
      tag: 'Comm-A-v1',
      purposeIds: [PDT_3054_PURPOSE_BILLING],
      contacts: contactsWithEmailAndMobile(EMAIL_BILLING_V1, MOBILE_PLACEHOLDER_V1),
    },
  ];
  const customer = await createCustomerWithComms(fx, entriesV1);
  const contract = await createSignedProductContractWithComms(fx, {
    billingCommId: customer.commIds['Comm-A-v1'],
    contractCommId: customer.commIds['Comm-B'],
  });
  const productContractDetailIdV1 = contract.productContractDetailId;

  const template = customer.createPayload;
  const updated = await putCustomerReplaceComms(
    fx,
    customer.customerId,
    customer.customerVersion,
    template,
    [
      {
        ...entriesV1[0],
        id: customer.commIds['Comm-B'],
      },
      {
        ...entriesV1[1],
        id: customer.commIds['Comm-A-v1'],
      },
      {
        tag: 'Comm-A-v2',
        purposeIds: [PDT_3054_PURPOSE_BILLING],
        contacts: contactsWithEmailAndMobile(EMAIL_BILLING_V2, MOBILE_PLACEHOLDER_V2),
      },
    ],
    true,
  );

  await putProductContractNewVersion(
    fx.Request,
    fx.Endpoints,
    contract.contractId,
    contract.contractPayload,
    1,
    {
      startDate: addDaysIso(ANCHOR_DATE, 1),
      savingAsNewVersion: true,
      versionStatus: 'SIGNED',
    },
  );

  // Re-apply billing comm on latest version after sign.
  const loadedAfter = await loadProductContract(fx.Request, contract.contractId);
  const versions = (loadedAfter.versions ?? []) as { versionId?: number; startDate?: string }[];
  const versionIdV2 = Math.max(...versions.map((v) => Number(v.versionId ?? 0)));
  const v2StartDate = String(
    versions.find((v) => Number(v.versionId) === versionIdV2)?.startDate ??
      addDaysIso(ANCHOR_DATE, 1),
  );
  const editPayload = await buildEditProductContractPayload(
    fx.Request,
    contract.contractId,
    contract.contractPayload,
    { savingAsNewVersion: false, preserveSigningDate: true, startDate: v2StartDate },
  );
  const editBp = editPayload.basicParameters as Record<string, unknown>;
  editBp.communicationDataBillingId = updated.commIds['Comm-A-v2'];
  editBp.communicationDataContractId = updated.commIds['Comm-B'];
  const put = await fx.Request.put(
    `${fx.Endpoints.productContract}/${contract.contractId}?versionId=${versionIdV2}&changeFutureVersionsPods=false`,
    { data: editPayload },
  );
  await expect(put).CheckResponse();

  const finalBody = await loadProductContract(fx.Request, contract.contractId);
  const productContractDetailIdV2 = versionDetailId(finalBody, versionIdV2);
  expect(productContractDetailIdV2, 'v2 productContractDetailId').toBeTruthy();

  return {
    customerId: customer.customerId,
    customerIdentifier: customer.customerIdentifier,
    customerVersion: Number(
      (await getCustomerJson(fx.Request, fx.Endpoints, customer.customerId)).customerVersionId ??
        customer.customerVersion,
    ),
    contractId: contract.contractId,
    contractNumber: contract.contractNumber,
    contractVersionId: versionIdV2,
    productContractDetailId: productContractDetailIdV2!,
    productContractDetailIdV1,
    productContractDetailIdV2: productContractDetailIdV2!,
    versionIdV2,
    commIds: updated.commIds,
  };
}

export async function buildServiceContractCommScenario(
  fx: Fx,
  variant: 'email-two-comms' | 'email-three-comms' | 'sms-three-comms' = 'email-two-comms',
): Promise<Pdt3054ServiceScenario> {
  let entries: CommEntrySpec[];
  switch (variant) {
    case 'email-three-comms':
      entries = [
        {
          tag: 'Comm-A',
          purposeIds: [PDT_3054_PURPOSE_BILLING],
          contacts: contactsWithEmailAndMobile(EMAIL_BILLING, MOBILE_PLACEHOLDER_BILLING),
        },
        {
          tag: 'Comm-B',
          purposeIds: [PDT_3054_PURPOSE_CONTRACT],
          contacts: contactsWithEmailAndMobile(EMAIL_CONTRACT, MOBILE_PLACEHOLDER_CONTRACT),
        },
        {
          tag: 'Comm-C',
          purposeIds: [PDT_3054_PURPOSE_BILLING],
          contacts: contactsWithEmailAndMobile(EMAIL_CUSTOMER_BILLING, MOBILE_PLACEHOLDER_CUSTOMER),
        },
      ];
      break;
    case 'sms-three-comms':
      entries = [
        {
          tag: 'Comm-A',
          purposeIds: [PDT_3054_PURPOSE_BILLING],
          contacts: contactsWithEmailAndMobile(
            EMAIL_PLACEHOLDER_SMS_BILLING,
            MOBILE_BILLING,
            true,
          ),
        },
        {
          tag: 'Comm-B',
          purposeIds: [PDT_3054_PURPOSE_CONTRACT],
          contacts: contactsWithEmailAndMobile(
            EMAIL_PLACEHOLDER_SMS_CONTRACT,
            MOBILE_CONTRACT,
            true,
          ),
        },
        {
          tag: 'Comm-C',
          purposeIds: [PDT_3054_PURPOSE_BILLING],
          contacts: contactsWithEmailAndMobile(
            EMAIL_PLACEHOLDER_SMS_CUSTOMER,
            MOBILE_CUSTOMER,
            true,
          ),
        },
      ];
      break;
    case 'email-two-comms':
    default:
      entries = [
        {
          tag: 'Comm-A',
          purposeIds: [PDT_3054_PURPOSE_BILLING],
          contacts: contactsWithEmailAndMobile(EMAIL_BILLING, MOBILE_PLACEHOLDER_BILLING),
        },
        {
          tag: 'Comm-B',
          purposeIds: [PDT_3054_PURPOSE_CONTRACT],
          contacts: contactsWithEmailAndMobile(EMAIL_CONTRACT, MOBILE_PLACEHOLDER_CONTRACT),
        },
      ];
      break;
  }
  const customer = await createCustomerWithComms(fx, entries);
  return buildServiceContractForExistingCustomer(fx, customer);
}

async function buildServiceContractForExistingCustomer(
  fx: Fx,
  customer: Awaited<ReturnType<typeof createCustomerWithComms>>,
): Promise<Pdt3054ServiceScenario> {
  const { Request, GeneratePayload, Responses, Endpoints } = fx;

  // Ensure POD for this customer context exists (createCatalog not required for service).
  if (Responses.pod.length === 0) {
    const pod = await Request.post(Endpoints.pod, {
      data: GeneratePayload.pointsOfDelivery.pod_settlement(),
    });
    await expect(pod).CheckResponse();
    Responses.pod.push(await pod.json());
  }

  if (Responses.priceComponent.length === 0) {
    const price = await Request.post(Endpoints.priceComponent, {
      data: GeneratePayload.productAndServices.perPiece(),
    });
    await expect(price).CheckResponse();
    Responses.priceComponent.push(asPriceComponentId(await price.json()));
  }

  if (Responses.terms.length === 0) {
    const term = await Request.post(Endpoints.terms, {
      data: GeneratePayload.productAndServices.term(),
    });
    await expect(term).CheckResponse();
    Responses.terms.push(await term.json());
  }

  let serviceId: number;
  if (Responses.service.length === 0) {
    const servicePayload = GeneratePayload.productAndServices.service();
    servicePayload.penalties = [];
    servicePayload.penaltyGroups = [];
    servicePayload.terminations = [];
    servicePayload.terminationGroups = [];
    const serviceRes = await Request.post(Endpoints.service, { data: servicePayload });
    await expect(serviceRes).CheckResponse();
    Responses.service.push(await serviceRes.json());
  }
  serviceId = asServiceId(Responses.service[0]);

  const serviceViewRes = await Request.get(`${Endpoints.service}/${serviceId}?version=1`);
  await expect(serviceViewRes).CheckResponse();
  const serviceView = await serviceViewRes.json();
  const activeVersion = (serviceView.versions ?? []).find(
    (v: { status?: string }) => v.status === 'ACTIVE',
  );
  expect(activeVersion?.id).toBeTruthy();
  const serviceVersionId = activeVersion!.id as number;
  const serviceDetailId = activeVersion!.detailId as number;

  const thirdRes = await Request.get(`${Endpoints.serviceContract}/third-tab-fields`, {
    params: { serviceDetailId },
  });
  await expect(thirdRes).CheckResponse();
  const third = (await thirdRes.json()) as Record<string, unknown>;
  const termsCatalog = third.serviceContractTerms as { id: number }[];
  const invoiceTermsCatalog = third.invoicePaymentTerms as { id: number; value: number }[];
  expect(termsCatalog?.length).toBeGreaterThan(0);

  let formulaSource: unknown = null;
  const pcId = asPriceComponentId(Responses.priceComponent[Responses.priceComponent.length - 1]);
  const pcRes = await Request.get(`${Endpoints.priceComponent}/${pcId}`);
  if (pcRes.ok()) formulaSource = await pcRes.json();
  const formula = await resolvePdt2599ContractFormulaForPost(
    Request,
    Endpoints,
    third,
    formulaSource,
    [],
  );

  // Ensure Responses.customer[0] is our customer for serviceContract payload generator.
  const customerIndex = Responses.customer.findIndex(
    (c: { id?: number }) => Number(c.id) === customer.customerId,
  );
  expect(customerIndex, 'created customer must be in Responses').toBeGreaterThanOrEqual(0);

  const payload = await GeneratePayload.contractsAndOrders.serviceContract();
  const custJson = customer.customerJson;
  payload.basicParameters.customerId = custJson.customerId ?? customer.customerId;
  payload.basicParameters.customerVersionId =
    custJson.customerVersionId ?? custJson.versionId ?? customer.customerVersion;
  payload.basicParameters.communicationDataForBilling = customer.commIds['Comm-A'];
  payload.basicParameters.communicationDataForContract = customer.commIds['Comm-B'];
  payload.basicParameters.serviceVersionId = serviceVersionId;
  payload.basicParameters.contractStatus = 'DRAFT';
  payload.basicParameters.detailsSubStatus = 'DRAFT';
  payload.basicParameters.contractVersionStatus = 'SIGNED';
  payload.basicParameters.signInDate = null as unknown as string;
  payload.basicParameters.entryIntoForceDate = null;
  payload.serviceParameters.contractTermId = termsCatalog[0].id;
  payload.serviceParameters.invoicePaymentTermId = invoiceTermsCatalog[0].id;
  payload.serviceParameters.invoicePaymentTerm = invoiceTermsCatalog[0].value;
  payload.serviceParameters.contractFormulas = formula
    ? [{ formulaVariableId: formula.formulaVariableId, value: formula.value }]
    : [];
  payload.serviceParameters.podIds = [Number(Responses.pod[Responses.pod.length - 1].id)];
  payload.serviceParameters.entryIntoForceDate = null as unknown as string;
  payload.serviceParameters.startOfContractInitialTermDate = null as unknown as string;

  const createRes = await Request.post(Endpoints.serviceContract, { data: payload });
  await expect(createRes).CheckResponse();
  const contractId = Number(await createRes.json());
  Responses.serviceContract.push({ id: contractId });

  const ready = await Request.put(
    `service-contract/status-update/${contractId}?versionId=1`,
    {
      data: {
        contractStatus: 'READY',
        contractSubStatus: 'READY',
        contractVersionStatus: 'SIGNED',
      },
    },
  );
  await expect(ready).CheckResponse();
  const signed = await Request.put(
    `service-contract/status-update/${contractId}?versionId=1`,
    {
      data: {
        contractStatus: 'SIGNED',
        contractSubStatus: 'SIGNED_BY_BOTH_SIDES',
        contractVersionStatus: 'SIGNED',
      },
    },
  );
  await expect(signed).CheckResponse();

  const getRes = await Request.get(`${Endpoints.serviceContract}/${contractId}`);
  await expect(getRes).CheckResponse();
  const body = (await getRes.json()) as Record<string, unknown>;
  const bp = (body.basicParameters ?? {}) as {
    contractNumber?: string;
    serviceContractNumber?: string;
  };
  const versions = (body.versions ?? []) as { versionId?: number; id?: number }[];
  const v1 = versions.find((v) => Number(v.versionId) === 1) ?? versions[0];
  const serviceContractDetailId = Number(v1?.id);
  expect(serviceContractDetailId, 'serviceContractDetailId').toBeTruthy();

  return {
    customerId: customer.customerId,
    customerIdentifier: customer.customerIdentifier,
    customerVersion: customer.customerVersion,
    contractId,
    contractNumber: String(bp.contractNumber ?? bp.serviceContractNumber ?? ''),
    contractVersionId: Number(v1?.versionId ?? 1),
    serviceContractDetailId,
    commIds: customer.commIds,
  };
}

export async function uploadMassEmailCustomerImport(
  Request: FixtureRequest,
  FileUploadRequest: FixtureFileUpload,
  Endpoints: baseFixture['Endpoints'],
  customerIdentifier: string,
): Promise<MassEmailImportBody> {
  const importType = 'MASS_IMPORT_OF_CUSTOMERS';
  const templateRes = await Request.get(
    `${Endpoints.email}/mass-import-template/${importType}`,
  );
  await expect(templateRes).CheckResponse();
  const templateBuffer = await templateRes.body();

  const outputDir = path.resolve(__dirname, '../../mass-imports/output');
  if (!fs.existsSync(outputDir)) fs.mkdirSync(outputDir, { recursive: true });
  const stamp = new Date().toISOString().replace(/[:.]/g, '-');
  const filePath = path.join(outputDir, `pdt-3054-email-customer-${stamp}.xlsx`);
  fs.writeFileSync(filePath, templateBuffer);

  const workbook = new ExcelJS.Workbook();
  await workbook.xlsx.readFile(filePath);
  const worksheet = workbook.getWorksheet(1);
  if (!worksheet) throw new Error('Mass email customers template has no worksheet');
  worksheet.getCell('A2').value = customerIdentifier;
  await workbook.xlsx.writeFile(filePath);
  const fileBuffer = fs.readFileSync(filePath);

  const upload = await FileUploadRequest.post(
    `${Endpoints.email}/customers-import?massEmailCommunicationImportType=${importType}`,
    {
      multipart: {
        file: {
          name: path.basename(filePath),
          mimeType: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
          buffer: fileBuffer,
        },
      },
    },
  );
  await expect(upload).CheckResponse();
  const body = (await upload.json()) as MassEmailImportBody;
  try {
    fs.unlinkSync(filePath);
  } catch {
    /* ignore */
  }
  return body;
}

export { uploadMassEmailContractImport, uploadMassSmsContractParse };

export function massCustomerFromImportRow(
  row: NonNullable<MassCommImportBody['results']>[number],
): {
  customerIdentifier: string;
  version?: number;
  productContractDetailId?: number;
  serviceContractDetailId?: number;
} {
  return {
    customerIdentifier: String(row.customerIdentifier ?? ''),
    version:
      row.customerVersionId != null ? Number(row.customerVersionId) : undefined,
    productContractDetailId:
      row.productContractDetailId != null
        ? Number(row.productContractDetailId)
        : undefined,
    serviceContractDetailId:
      row.serviceContractDetailId != null
        ? Number(row.serviceContractDetailId)
        : undefined,
  };
}

export async function postMassEmail(
  Request: FixtureRequest,
  Endpoints: baseFixture['Endpoints'],
  Responses: baseFixture['Responses'],
  opts: {
    subject: string;
    contactPurposeIds: number[];
    createType: 'DRAFT' | 'SEND';
    customers: {
      customerIdentifier: string;
      version?: number;
      productContractDetailId?: number;
      serviceContractDetailId?: number;
    }[];
  },
): Promise<{ status: number; bodyText: string; massEmailId?: number }> {
  const payload = {
    allCustomersWithActiveContract: false,
    communicationAsInstitution: false,
    emailBoxId: envVariables.email_mailboxes,
    topicOfCommunicationId: envVariables.topic_of_communication,
    contactPurposeIds: opts.contactPurposeIds,
    subject: opts.subject,
    emailBody: opts.subject,
    createType: opts.createType,
    customers: opts.customers,
  };
  const res = await Request.post(`${Endpoints.email}/mass`, { data: payload });
  const bodyText = await res.text();
  const status = res.status();
  if (status >= 200 && status < 300) {
    const massEmailId = Number(JSON.parse(bodyText));
    Responses.email = Responses.email ?? [];
    Responses.email.push({ id: massEmailId, kind: 'massEmailDraft' });
    return { status, bodyText, massEmailId };
  }
  return { status, bodyText };
}

export async function postMassSms(
  Request: FixtureRequest,
  Endpoints: baseFixture['Endpoints'],
  Responses: baseFixture['Responses'],
  opts: {
    smsBody: string;
    contactPurposeIds: number[];
    saveAs: 'DRAFT' | 'SEND';
    customers: {
      customerIdentifier: string;
      version?: number;
      productContractDetailId?: number;
      serviceContractDetailId?: number;
    }[];
  },
): Promise<{ status: number; bodyText: string; smsCommunicationId?: number }> {
  const payload = {
    allCustomersWithActiveContract: false,
    communicationAsInstitution: false,
    topicOfCommunicationId: envVariables.topic_of_communication,
    exchangeCodeId: envVariables.sms_sending_numbers,
    contactPurposeIds: opts.contactPurposeIds,
    smsBody: opts.smsBody,
    saveAs: opts.saveAs,
    customers: opts.customers,
  };
  const res = await Request.post(`${Endpoints.sms}/mass`, { data: payload });
  const bodyText = await res.text();
  const status = res.status();
  if (status >= 200 && status < 300) {
    const smsCommunicationId = Number(JSON.parse(bodyText));
    Responses.sms = Responses.sms ?? [];
    Responses.sms.push({ id: smsCommunicationId, kind: 'massSmsDraft' });
    return { status, bodyText, smsCommunicationId };
  }
  return { status, bodyText };
}

export async function getCustomerCommunications(
  Request: FixtureRequest,
  commId: number,
): Promise<Record<string, unknown>> {
  const res = await Request.get(`customer-communications/${commId}`);
  await expect(res).CheckResponse();
  return (await res.json()) as Record<string, unknown>;
}

export function extractEmailsFromCommView(body: Record<string, unknown>): string[] {
  // GET /customer-communications/{id} returns CommunicationDataResponse.contacts[].name
  // (Swagger ContactBasicInfo), not communicationContacts[].contactValue.
  const basicContacts =
    (body.contacts as { contactType?: string; name?: string }[] | undefined) ?? [];
  if (basicContacts.length > 0) {
    const emails = basicContacts
      .filter((c) => c.contactType === 'EMAIL')
      .map((c) => String(c.name ?? '').toLowerCase())
      .filter(Boolean);
    if (emails.length > 0) return emails;
  }
  const contacts =
    (body.communicationContacts as { contactType?: string; contactValue?: string }[]) ?? [];
  return contacts
    .filter((c) => c.contactType === 'EMAIL')
    .map((c) => String(c.contactValue ?? '').toLowerCase());
}

export function extractMobilesFromCommView(body: Record<string, unknown>): string[] {
  const basicContacts =
    (body.contacts as { contactType?: string; name?: string }[] | undefined) ?? [];
  if (basicContacts.length > 0) {
    const mobiles = basicContacts
      .filter((c) => c.contactType === 'MOBILE_NUMBER')
      .map((c) => String(c.name ?? '').replace(/\s/g, ''))
      .filter(Boolean);
    if (mobiles.length > 0) return mobiles;
  }
  const contacts =
    (body.communicationContacts as { contactType?: string; contactValue?: string }[]) ?? [];
  return contacts
    .filter((c) => c.contactType === 'MOBILE_NUMBER')
    .map((c) => String(c.contactValue ?? '').replace(/\s/g, ''));
}

export async function downloadMassEmailReportText(
  Request: FixtureRequest,
  Endpoints: baseFixture['Endpoints'],
  massEmailId: number,
): Promise<string> {
  const res = await Request.get(`${Endpoints.email}/download-report/${massEmailId}`);
  await expect(res).CheckResponse();
  const buf = await res.body();
  return buf.toString('utf8');
}

export async function getMassEmailView(
  Request: FixtureRequest,
  Endpoints: baseFixture['Endpoints'],
  massEmailId: number,
): Promise<Record<string, unknown>> {
  const res = await Request.get(`${Endpoints.email}/mass/${massEmailId}`);
  await expect(res).CheckResponse();
  return (await res.json()) as Record<string, unknown>;
}

export async function getMassSmsView(
  Request: FixtureRequest,
  Endpoints: baseFixture['Endpoints'],
  smsCommunicationId: number,
): Promise<Record<string, unknown>> {
  const res = await Request.get(`${Endpoints.sms}/mass/${smsCommunicationId}`);
  await expect(res).CheckResponse();
  return (await res.json()) as Record<string, unknown>;
}

export function assertImportRowOk(
  importBody: MassCommImportBody,
  customerIdentifier: string,
): NonNullable<MassCommImportBody['results']>[number] {
  const popup = importBody.popupMessage;
  expect(popup == null || String(popup).trim() === '', `popupMessage=${popup}`).toBeTruthy();
  const row = (importBody.results ?? []).find(
    (r) => r.customerIdentifier === customerIdentifier,
  );
  expect(row, `import results must include ${customerIdentifier}`).toBeTruthy();
  return row!;
}

export type { MassEmailImportBody, MassSmsImportBody, EmailCommunicationPreview, SmsCommunicationPreview };
export {
  MASS_EMAIL_CONTRACT_IMPORT_TYPE,
  MASS_SMS_CONTRACT_IMPORT_TYPE,
  resolveMassEmailCustomerPreview,
  resolveMassSmsCustomerPreview,
  waitForMassEmailIndividualEmailRow,
  getEmailCommunicationPreview,
  getSmsCommunicationPreview,
};

export function normalizeEmail(value?: string): string {
  return String(value ?? '')
    .trim()
    .toLowerCase();
}

export function normalizeMobileDigits(value?: string): string {
  return String(value ?? '').replace(/\D/g, '');
}

/** Listing rows linked to a mass email (may be one MASS_EMAIL customer row per purpose). */
export async function listMassEmailCustomerPreviewIds(
  Request: FixtureRequest,
  Endpoints: baseFixture['Endpoints'],
  massEmailId: number,
  customerIdentifier: string,
): Promise<{ previewId: number; channel: 'EMAIL' | 'MASS_EMAIL' }[]> {
  const listRes = await Request.get(`${Endpoints.email}/list`, {
    params: {
      page: 0,
      size: 50,
      prompt: customerIdentifier,
      searchBy: 'CUSTOMER_IDENTIFIER',
      kindOfCommunication: ['EMAIL', 'MASS_EMAIL'],
      sortColumn: 'ID',
      sortDirection: 'DESC',
    },
  });
  await expect(listRes).CheckResponse();
  const body = (await listRes.json()) as {
    content?: {
      massOrIndemailCommunicationId?: number;
      linkedEmailCommunicationId?: number;
      communicationChannel?: string;
    }[];
  };
  const rows = body.content ?? [];
  const linked = rows.filter((r) => r.linkedEmailCommunicationId === massEmailId);
  const out: { previewId: number; channel: 'EMAIL' | 'MASS_EMAIL' }[] = [];
  const seen = new Set<number>();
  for (const row of linked) {
    const previewId = Number(row.massOrIndemailCommunicationId);
    if (!previewId || seen.has(previewId)) continue;
    seen.add(previewId);
    const channel: 'EMAIL' | 'MASS_EMAIL' =
      String(row.communicationChannel ?? '').toUpperCase() === 'EMAIL' ? 'EMAIL' : 'MASS_EMAIL';
    out.push({ previewId, channel });
  }
  return out;
}

/**
 * After mass `createType: SEND` with multiple purposes, collect every linked customer
 * preview (email + communication data id) for the customer.
 */
export async function collectMassEmailCustomerPreviews(
  Request: FixtureRequest,
  Endpoints: baseFixture['Endpoints'],
  massEmailId: number,
  customerIdentifier: string,
  maxAttempts = 12,
): Promise<
  {
    previewId: number;
    channel: 'EMAIL' | 'MASS_EMAIL';
    preview: EmailCommunicationPreview;
  }[]
> {
  let last: { previewId: number; channel: 'EMAIL' | 'MASS_EMAIL' }[] = [];
  for (let attempt = 1; attempt <= maxAttempts; attempt += 1) {
    last = await listMassEmailCustomerPreviewIds(
      Request,
      Endpoints,
      massEmailId,
      customerIdentifier,
    );
    if (last.length > 0) break;
    if (attempt < maxAttempts) {
      await new Promise((resolve) => setTimeout(resolve, 2500));
    }
  }
  expect(
    last.length,
    `expected ≥1 listing row linked to massEmailId=${massEmailId} for ${customerIdentifier}`,
  ).toBeGreaterThan(0);

  const previews: {
    previewId: number;
    channel: 'EMAIL' | 'MASS_EMAIL';
    preview: EmailCommunicationPreview;
  }[] = [];
  for (const row of last) {
    previews.push({
      ...row,
      preview: await getEmailCommunicationPreview(Request, Endpoints, row.previewId, row.channel),
    });
  }
  return previews;
}

/** Poll until mass email customer preview is resolvable after SEND. */
export async function waitForResolvedMassEmailCustomerPreview(
  Request: FixtureRequest,
  Endpoints: baseFixture['Endpoints'],
  massEmailId: number,
  customerIdentifier: string,
  context: string,
  maxAttempts = 12,
): Promise<{ preview: EmailCommunicationPreview; channel: 'EMAIL' | 'MASS_EMAIL'; previewId: number }> {
  let lastError: unknown;
  for (let attempt = 1; attempt <= maxAttempts; attempt += 1) {
    try {
      return await resolveMassEmailCustomerPreview(
        Request,
        Endpoints,
        massEmailId,
        customerIdentifier,
      );
    } catch (err) {
      lastError = err;
      if (attempt < maxAttempts) {
        await new Promise((resolve) => setTimeout(resolve, 2500));
      }
    }
  }
  throw new Error(
    `${context}: could not resolve mass email customer preview for massEmailId=${massEmailId}, customer=${customerIdentifier} after ${maxAttempts} attempts; last=${String(lastError)}`,
  );
}

/** Poll until mass SMS customer preview is resolvable after SEND. */
export async function waitForResolvedMassSmsCustomerPreview(
  Request: FixtureRequest,
  Endpoints: baseFixture['Endpoints'],
  massSmsId: number,
  customerIdentifier: string,
  context: string,
  maxAttempts = 12,
): Promise<{ preview: SmsCommunicationPreview; previewId: number }> {
  let lastError: unknown;
  for (let attempt = 1; attempt <= maxAttempts; attempt += 1) {
    try {
      return await resolveMassSmsCustomerPreview(Request, Endpoints, massSmsId, customerIdentifier);
    } catch (err) {
      lastError = err;
      if (attempt < maxAttempts) {
        await new Promise((resolve) => setTimeout(resolve, 2500));
      }
    }
  }
  throw new Error(
    `${context}: could not resolve mass SMS customer preview for massSmsId=${massSmsId}, customer=${customerIdentifier} after ${maxAttempts} attempts; last=${String(lastError)}`,
  );
}

/** Listing rows linked to a mass SMS (may be one Individual SMS customer row per purpose). */
export async function listMassSmsCustomerPreviewIds(
  Request: FixtureRequest,
  Endpoints: baseFixture['Endpoints'],
  massSmsId: number,
  customerIdentifier: string,
): Promise<number[]> {
  const listRes = await Request.get(`${Endpoints.sms}/list`, {
    params: {
      page: 0,
      size: 50,
      prompt: customerIdentifier,
      smsCommunicationSearchBy: 'CUSTOMER_IDENTIFIER',
      kindOfCommunications: ['SMS', 'MASS_SMS'],
    },
  });
  await expect(listRes).CheckResponse();
  const body = (await listRes.json()) as {
    content?: {
      massOrIndSmsCommunicationId?: number;
      linkedMassSmsId?: number;
      communicationChannel?: string;
    }[];
  };
  const rows = body.content ?? [];
  const linked = rows.filter(
    (r) =>
      r.linkedMassSmsId === massSmsId &&
      String(r.communicationChannel ?? '').toLowerCase() === 'individual',
  );
  const out: number[] = [];
  const seen = new Set<number>();
  for (const row of linked) {
    const previewId = Number(row.massOrIndSmsCommunicationId);
    if (!previewId || seen.has(previewId)) continue;
    seen.add(previewId);
    out.push(previewId);
  }
  return out;
}

/**
 * After mass SMS `saveAs: SEND` with multiple purposes, collect every linked customer
 * preview (phone + communication data id) for the customer.
 */
export async function collectMassSmsCustomerPreviews(
  Request: FixtureRequest,
  Endpoints: baseFixture['Endpoints'],
  massSmsId: number,
  customerIdentifier: string,
  maxAttempts = 12,
): Promise<{ previewId: number; preview: SmsCommunicationPreview }[]> {
  let last: number[] = [];
  for (let attempt = 1; attempt <= maxAttempts; attempt += 1) {
    last = await listMassSmsCustomerPreviewIds(
      Request,
      Endpoints,
      massSmsId,
      customerIdentifier,
    );
    if (last.length > 0) break;
    if (attempt < maxAttempts) {
      await new Promise((resolve) => setTimeout(resolve, 2500));
    }
  }
  expect(
    last.length,
    `expected ≥1 listing row linked to massSmsId=${massSmsId} for ${customerIdentifier}`,
  ).toBeGreaterThan(0);

  const previews: { previewId: number; preview: SmsCommunicationPreview }[] = [];
  for (const previewId of last) {
    previews.push({
      previewId,
      preview: await getSmsCommunicationPreview(Request, Endpoints, previewId),
    });
  }
  return previews;
}
