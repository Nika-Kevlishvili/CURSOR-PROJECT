/**
 * PDT-3054 — Mass Email / Mass SMS | Use contract-level communication data
 * when recipients are imported by Contract.
 *
 * Backend TC: Cursor-Project/test_cases/Backend/Mass_communication_contract_import_comm_data.md
 * Environment: Dev (purpose billing=84, contract=83 via envVariables).
 *
 * Reference spec(s):
 * - tests/cursor/PDT-2815-version-validity-three-processes.spec.ts
 * - tests/cursor/PDT-2815-mass-email-individual-customer-version.spec.ts
 * - tests/cursor/PDT-2815-mass-sms-individual-customer-version.spec.ts
 * - tests/cursor/PDT-3035-contract-mass-import.spec.ts (cursor-test.fixtures + multi-TC titles)
 */

import { test, expect } from './cursor-test.fixtures';
import {
  buildProductContractTabLinks,
  finalizeTestRunSummary,
} from './shared/manual-verification-links.fixtures';
import {
  EMAIL_BILLING,
  EMAIL_BILLING_V1,
  EMAIL_BILLING_V2,
  EMAIL_CONTRACT,
  EMAIL_CUSTOMER_BILLING,
  ERR_COMM_NO_VALID_EMAIL_PREFIX,
  ERR_COMM_NO_VALID_EMAIL_SUFFIX,
  ERR_COMM_NO_VALID_EMAIL_SUFFIX_83,
  ERR_COMM_NO_VALID_MOBILE_SUFFIX,
  ERR_CONTRACT_NO_COMM_PURPOSE_84,
  MOBILE_BILLING,
  MOBILE_CONTRACT,
  MOBILE_CUSTOMER,
  MOBILE_PLACEHOLDER_CUSTOMER,
  PDT_3054_PURPOSE_BILLING,
  PDT_3054_PURPOSE_CONTRACT,
  assertImportRowOk,
  buildBlankVersionProductScenario,
  buildProductContractCommScenario,
  buildServiceContractCommScenario,
  collectMassEmailCustomerPreviews,
  collectMassSmsCustomerPreviews,
  createCustomerWithComms,
  massCustomerFromImportRow,
  normalizeEmail,
  normalizeMobileDigits,
  postMassEmail,
  postMassSms,
  uploadMassEmailContractImport,
  uploadMassEmailCustomerImport,
  uploadMassSmsContractParse,
  waitForResolvedMassEmailCustomerPreview,
  waitForResolvedMassSmsCustomerPreview,
  type Pdt3054ProductScenario,
} from './pdt-3054-mass-comm-contract-import-comm-data.fixtures';

const JIRA_TITLE =
  'Mass Email / Mass SM | Use contract-level communication data when recipients are imported by Contract';

test.describe(
  `[PDT-3054]: ${JIRA_TITLE}`,
  { tag: ['@pdt-3054', '@customerComm', '@massEmail', '@massSms', '@dev'] },
  () => {
    test(
      `[PDT-3054]: ${JIRA_TITLE} — TC-BE-1 product contract import billing purpose 84`,
      async ({
        Request,
        FileUploadRequest,
        GeneratePayload,
        Responses,
        Endpoints,
        TestRunSummary,
      }) => {
        test.setTimeout(15 * 60 * 1000);
        const fx = { Request, FileUploadRequest, GeneratePayload, Responses, Endpoints };

        let scenario: Pdt3054ProductScenario;
        await test.step(
          'Precondition: product contract with Comm-A(billing)/Comm-B(contract)/Comm-C(customer billing)',
          async () => {
            scenario = await buildProductContractCommScenario(fx, 'email-three-comms');
            TestRunSummary.registerPayload('scenario', scenario);
            expect(scenario.commIds['Comm-A']).toBeTruthy();
            expect(scenario.commIds['Comm-C']).toBeTruthy();
          },
        );

        let importRow: ReturnType<typeof massCustomerFromImportRow>;
        await test.step('POST email-communication/customers-import MASS_IMPORT_OF_CONTRACTS', async () => {
          const importBody = await uploadMassEmailContractImport(
            Request,
            FileUploadRequest,
            Endpoints,
            [{ contractNumber: scenario!.contractNumber, version: String(scenario!.contractVersionId) }],
          );
          const row = assertImportRowOk(importBody, scenario!.customerIdentifier);
          expect(row.productContractDetailId).toBeTruthy();
          importRow = massCustomerFromImportRow(row);
          TestRunSummary.registerPayload('importRow', importRow);
        });

        let massEmailId = 0;
        await test.step('POST email-communication/mass createType=SEND contactPurposeIds=[84]', async () => {
          const result = await postMassEmail(Request, Endpoints, Responses, {
            subject: 'PDT-3054 BE-1 billing purpose',
            contactPurposeIds: [PDT_3054_PURPOSE_BILLING],
            createType: 'SEND',
            customers: [importRow!],
          });
          expect(result.status, result.bodyText).toBe(200);
          expect(result.bodyText.toLowerCase()).not.toContain(
            'customer communication data not found for purpose id 84',
          );
          massEmailId = result.massEmailId!;
          expect(massEmailId).toBeGreaterThan(0);
          TestRunSummary.recordCheck({
            check: 'TC-BE-1 mass email SEND accepts contract billing purpose',
            expectedResult: 'HTTP 200 + massEmailId > 0; no purpose-84 not-found',
            actualResult: `As expected — status=${result.status} massEmailId=${massEmailId}`,
            passed: true,
          });
        });

        await test.step(
          'GET mass email customer preview — EMAIL_BILLING + Comm-A (not customer-level Comm-C)',
          async () => {
            const { preview, previewId, channel } = await waitForResolvedMassEmailCustomerPreview(
              Request,
              Endpoints,
              massEmailId,
              scenario!.customerIdentifier,
              'TC-BE-1',
            );
            const email = normalizeEmail(preview.customerEmailAddress);
            const commId = Number(preview.customerCommunicationDataShortResponse?.id);
            expect(email, `previewId=${previewId} channel=${channel}`).toContain(
              normalizeEmail(EMAIL_BILLING),
            );
            expect(email).not.toContain(normalizeEmail(EMAIL_CUSTOMER_BILLING));
            expect(commId).toBe(scenario!.commIds['Comm-A']);
            expect(commId).not.toBe(scenario!.commIds['Comm-C']);
            TestRunSummary.recordCheck({
              check: 'TC-BE-1 mass object selects contract billing email/comm',
              expectedResult: `customerEmailAddress=${EMAIL_BILLING}; commId=Comm-A(${scenario!.commIds['Comm-A']})`,
              actualResult: `As expected — email=${email}; commId=${commId}; previewId=${previewId}`,
              passed: true,
            });
          },
        );

        await test.step('Attach test run summary', async () => {
          finalizeTestRunSummary(TestRunSummary, Responses, {
            jiraKey: 'PDT-3054',
            relevantEntityKeys: ['customer', 'product', 'productContract'],
            extraLinks: buildProductContractTabLinks(scenario!.contractId),
            snapshot: { tc: 'TC-BE-1', massEmailId, ...scenario! },
          });
        });
      },
    );

    test(
      `[PDT-3054]: ${JIRA_TITLE} — TC-BE-2 product contract import contract purpose 83`,
      async ({
        Request,
        FileUploadRequest,
        GeneratePayload,
        Responses,
        Endpoints,
        TestRunSummary,
      }) => {
        test.setTimeout(15 * 60 * 1000);
        const fx = { Request, FileUploadRequest, GeneratePayload, Responses, Endpoints };

        let scenario: Pdt3054ProductScenario;
        await test.step('Precondition: product contract with Comm-A + Comm-B', async () => {
          scenario = await buildProductContractCommScenario(fx, 'email-two-comms');
          TestRunSummary.registerPayload('scenario', scenario);
        });

        let importRow: ReturnType<typeof massCustomerFromImportRow>;
        await test.step('POST customers-import MASS_IMPORT_OF_CONTRACTS', async () => {
          const importBody = await uploadMassEmailContractImport(
            Request,
            FileUploadRequest,
            Endpoints,
            [{ contractNumber: scenario!.contractNumber, version: String(scenario!.contractVersionId) }],
          );
          importRow = massCustomerFromImportRow(
            assertImportRowOk(importBody, scenario!.customerIdentifier),
          );
        });

        let massEmailId = 0;
        await test.step('POST mass email SEND contactPurposeIds=[83]', async () => {
          const result = await postMassEmail(Request, Endpoints, Responses, {
            subject: 'PDT-3054 BE-2 contract purpose',
            contactPurposeIds: [PDT_3054_PURPOSE_CONTRACT],
            createType: 'SEND',
            customers: [importRow!],
          });
          expect(result.status, result.bodyText).toBe(200);
          expect(result.bodyText.toLowerCase()).not.toContain(
            'customer communication data not found for purpose id 83',
          );
          massEmailId = result.massEmailId!;
          expect(massEmailId).toBeGreaterThan(0);
          TestRunSummary.recordCheck({
            check: 'TC-BE-2 mass email SEND with contract purpose 83',
            expectedResult: 'HTTP 200 + massEmailId; no purpose-83 not-found',
            actualResult: `As expected — status=${result.status} id=${massEmailId}`,
            passed: true,
          });
        });

        await test.step('GET mass email customer preview — EMAIL_CONTRACT + Comm-B', async () => {
          const { preview, previewId } = await waitForResolvedMassEmailCustomerPreview(
            Request,
            Endpoints,
            massEmailId,
            scenario!.customerIdentifier,
            'TC-BE-2',
          );
          const email = normalizeEmail(preview.customerEmailAddress);
          const commId = Number(preview.customerCommunicationDataShortResponse?.id);
          expect(email, `previewId=${previewId}`).toContain(normalizeEmail(EMAIL_CONTRACT));
          expect(commId).toBe(scenario!.commIds['Comm-B']);
          TestRunSummary.recordCheck({
            check: 'TC-BE-2 mass object selects contract-purpose email/comm',
            expectedResult: `customerEmailAddress=${EMAIL_CONTRACT}; commId=Comm-B(${scenario!.commIds['Comm-B']})`,
            actualResult: `As expected — email=${email}; commId=${commId}`,
            passed: true,
          });
        });

        await test.step('Attach test run summary', async () => {
          finalizeTestRunSummary(TestRunSummary, Responses, {
            jiraKey: 'PDT-3054',
            relevantEntityKeys: ['customer', 'product', 'productContract'],
            extraLinks: buildProductContractTabLinks(scenario!.contractId),
            snapshot: { tc: 'TC-BE-2', massEmailId, ...scenario! },
          });
        });
      },
    );

    test(
      `[PDT-3054]: ${JIRA_TITLE} — TC-BE-3 both purposes 84+83 report union`,
      async ({
        Request,
        FileUploadRequest,
        GeneratePayload,
        Responses,
        Endpoints,
        TestRunSummary,
      }) => {
        test.setTimeout(15 * 60 * 1000);
        const fx = { Request, FileUploadRequest, GeneratePayload, Responses, Endpoints };

        let scenario: Pdt3054ProductScenario;
        await test.step('Precondition: product contract Comm-A + Comm-B distinct emails', async () => {
          scenario = await buildProductContractCommScenario(fx, 'email-two-comms');
          TestRunSummary.registerPayload('scenario', scenario);
        });

        let importRow: ReturnType<typeof massCustomerFromImportRow>;
        await test.step('POST customers-import MASS_IMPORT_OF_CONTRACTS', async () => {
          const importBody = await uploadMassEmailContractImport(
            Request,
            FileUploadRequest,
            Endpoints,
            [{ contractNumber: scenario!.contractNumber, version: String(scenario!.contractVersionId) }],
          );
          importRow = massCustomerFromImportRow(
            assertImportRowOk(importBody, scenario!.customerIdentifier),
          );
        });

        let massEmailId = 0;
        await test.step('POST mass email createType=SEND contactPurposeIds=[84,83]', async () => {
          const result = await postMassEmail(Request, Endpoints, Responses, {
            subject: 'PDT-3054 BE-3 both purposes',
            contactPurposeIds: [PDT_3054_PURPOSE_BILLING, PDT_3054_PURPOSE_CONTRACT],
            createType: 'SEND',
            customers: [importRow!],
          });
          expect(result.status, result.bodyText).toBe(200);
          massEmailId = result.massEmailId!;
          expect(massEmailId).toBeGreaterThan(0);
        });

        await test.step(
          'GET all mass-linked customer previews — union EMAIL_BILLING + EMAIL_CONTRACT (Comm-A + Comm-B)',
          async () => {
            const previews = await collectMassEmailCustomerPreviews(
              Request,
              Endpoints,
              massEmailId,
              scenario!.customerIdentifier,
            );
            const emails = previews.map((p) => normalizeEmail(p.preview.customerEmailAddress));
            const commIds = previews
              .map((p) => Number(p.preview.customerCommunicationDataShortResponse?.id))
              .filter((id) => Number.isFinite(id) && id > 0);
            expect(
              emails.some((e) => e.includes(normalizeEmail(EMAIL_BILLING))),
              `emails=${JSON.stringify(emails)}`,
            ).toBeTruthy();
            expect(
              emails.some((e) => e.includes(normalizeEmail(EMAIL_CONTRACT))),
              `emails=${JSON.stringify(emails)}`,
            ).toBeTruthy();
            expect(commIds).toContain(scenario!.commIds['Comm-A']);
            expect(commIds).toContain(scenario!.commIds['Comm-B']);
            TestRunSummary.recordCheck({
              check: 'TC-BE-3 mass object resolves billing + contract recipient emails',
              expectedResult: `emails include ${EMAIL_BILLING} + ${EMAIL_CONTRACT}; commIds Comm-A+Comm-B`,
              actualResult: `As expected — emails=${emails.join('|')}; commIds=${commIds.join(',')}; rows=${previews.length}`,
              passed: true,
            });
          },
        );

        await test.step('Attach test run summary', async () => {
          finalizeTestRunSummary(TestRunSummary, Responses, {
            jiraKey: 'PDT-3054',
            relevantEntityKeys: ['customer', 'product', 'productContract'],
            extraLinks: buildProductContractTabLinks(scenario!.contractId),
            snapshot: { tc: 'TC-BE-3', massEmailId, ...scenario! },
          });
        });
      },
    );

    test(
      `[PDT-3054]: ${JIRA_TITLE} — TC-BE-4 Mass SMS contract import billing purpose 84`,
      async ({
        Request,
        FileUploadRequest,
        GeneratePayload,
        Responses,
        Endpoints,
        TestRunSummary,
      }) => {
        test.setTimeout(15 * 60 * 1000);
        const fx = { Request, FileUploadRequest, GeneratePayload, Responses, Endpoints };

        let scenario: Pdt3054ProductScenario;
        await test.step('Precondition: product contract SMS Comm-A/B/C', async () => {
          scenario = await buildProductContractCommScenario(fx, 'sms-three-comms');
          TestRunSummary.registerPayload('scenario', scenario);
        });

        let importRow: ReturnType<typeof massCustomerFromImportRow>;
        await test.step('POST sms-communication/parse MASS_SMS_CONTRACT', async () => {
          const importBody = await uploadMassSmsContractParse(
            Request,
            FileUploadRequest,
            Endpoints,
            [{ contractNumber: scenario!.contractNumber, version: String(scenario!.contractVersionId) }],
          );
          const row = assertImportRowOk(importBody, scenario!.customerIdentifier);
          expect(row.productContractDetailId).toBeTruthy();
          importRow = massCustomerFromImportRow(row);
        });

        let smsId = 0;
        await test.step('POST sms-communication/mass saveAs=SEND contactPurposeIds=[84]', async () => {
          const result = await postMassSms(Request, Endpoints, Responses, {
            smsBody: 'PDT-3054 BE-4 billing mobile test',
            contactPurposeIds: [PDT_3054_PURPOSE_BILLING],
            saveAs: 'SEND',
            customers: [importRow!],
          });
          expect(result.status, result.bodyText).toBe(200);
          expect(result.bodyText.toLowerCase()).not.toContain(
            'customer communication data not found',
          );
          smsId = result.smsCommunicationId!;
          expect(smsId).toBeGreaterThan(0);
        });

        await test.step(
          'GET mass SMS customer preview — MOBILE_BILLING + Comm-A (not customer-level mobile)',
          async () => {
            const { preview, previewId } = await waitForResolvedMassSmsCustomerPreview(
              Request,
              Endpoints,
              smsId,
              scenario!.customerIdentifier,
              'TC-BE-4',
            );
            const phone = normalizeMobileDigits(preview.phoneNumber);
            const billingDigits = normalizeMobileDigits(MOBILE_BILLING);
            const customerDigits = normalizeMobileDigits(MOBILE_CUSTOMER);
            const commId = Number(preview.customerCommunicationDataShortResponse?.id);
            expect(
              phone.endsWith(billingDigits.slice(-9)),
              `previewId=${previewId} phone=${preview.phoneNumber}`,
            ).toBeTruthy();
            expect(phone.endsWith(customerDigits.slice(-9))).toBeFalsy();
            expect(commId).toBe(scenario!.commIds['Comm-A']);
            TestRunSummary.recordCheck({
              check: 'TC-BE-4 mass SMS object selects contract billing mobile/comm',
              expectedResult: `phoneNumber≈${MOBILE_BILLING}; commId=Comm-A(${scenario!.commIds['Comm-A']})`,
              actualResult: `As expected — phone=${preview.phoneNumber}; commId=${commId}`,
              passed: true,
            });
          },
        );

        await test.step('Attach test run summary', async () => {
          finalizeTestRunSummary(TestRunSummary, Responses, {
            jiraKey: 'PDT-3054',
            relevantEntityKeys: ['customer', 'product', 'productContract'],
            extraLinks: buildProductContractTabLinks(scenario!.contractId),
            snapshot: { tc: 'TC-BE-4', smsId, ...scenario! },
          });
        });
      },
    );

    test(
      `[PDT-3054]: ${JIRA_TITLE} — TC-BE-5 service contract import purpose 83`,
      async ({
        Request,
        FileUploadRequest,
        GeneratePayload,
        Responses,
        Endpoints,
        TestRunSummary,
      }) => {
        test.setTimeout(15 * 60 * 1000);
        const fx = { Request, FileUploadRequest, GeneratePayload, Responses, Endpoints };

        let scenario: Awaited<ReturnType<typeof buildServiceContractCommScenario>>;
        await test.step('Precondition: signed service contract with Comm-A/Comm-B', async () => {
          scenario = await buildServiceContractCommScenario(fx);
          TestRunSummary.registerPayload('scenario', scenario);
          expect(scenario.serviceContractDetailId).toBeTruthy();
        });

        let importRow: ReturnType<typeof massCustomerFromImportRow>;
        await test.step('POST customers-import service contract row', async () => {
          const importBody = await uploadMassEmailContractImport(
            Request,
            FileUploadRequest,
            Endpoints,
            [
              {
                contractNumber: scenario!.contractNumber,
                version: String(scenario!.contractVersionId),
              },
            ],
          );
          const row = assertImportRowOk(importBody, scenario!.customerIdentifier);
          expect(row.serviceContractDetailId, 'serviceContractDetailId from import').toBeTruthy();
          importRow = massCustomerFromImportRow(row);
        });

        let massEmailId = 0;
        await test.step('POST mass email SEND contactPurposeIds=[83]', async () => {
          const result = await postMassEmail(Request, Endpoints, Responses, {
            subject: 'PDT-3054 BE-5 service contract purpose',
            contactPurposeIds: [PDT_3054_PURPOSE_CONTRACT],
            createType: 'SEND',
            customers: [importRow!],
          });
          expect(result.status, result.bodyText).toBe(200);
          massEmailId = result.massEmailId!;
          expect(massEmailId).toBeGreaterThan(0);
          TestRunSummary.recordCheck({
            check: 'TC-BE-5 service contract import mass email purpose 83',
            expectedResult: 'HTTP 200 massEmailId; no purpose-83 not-found',
            actualResult: `As expected — status=${result.status} id=${massEmailId}`,
            passed: true,
          });
        });

        await test.step('GET mass email customer preview — EMAIL_CONTRACT + Comm-B', async () => {
          const { preview, previewId } = await waitForResolvedMassEmailCustomerPreview(
            Request,
            Endpoints,
            massEmailId,
            scenario!.customerIdentifier,
            'TC-BE-5',
          );
          const email = normalizeEmail(preview.customerEmailAddress);
          const commId = Number(preview.customerCommunicationDataShortResponse?.id);
          expect(email, `previewId=${previewId}`).toContain(normalizeEmail(EMAIL_CONTRACT));
          expect(commId).toBe(scenario!.commIds['Comm-B']);
          TestRunSummary.recordCheck({
            check: 'TC-BE-5 mass object selects service-contract purpose email/comm',
            expectedResult: `customerEmailAddress=${EMAIL_CONTRACT}; commId=Comm-B(${scenario!.commIds['Comm-B']})`,
            actualResult: `As expected — email=${email}; commId=${commId}`,
            passed: true,
          });
        });

        await test.step('Attach test run summary', async () => {
          finalizeTestRunSummary(TestRunSummary, Responses, {
            jiraKey: 'PDT-3054',
            relevantEntityKeys: ['customer', 'service', 'serviceContract'],
            snapshot: { tc: 'TC-BE-5', massEmailId, ...scenario! },
          });
        });
      },
    );

    test(
      `[PDT-3054]: ${JIRA_TITLE} — TC-BE-6 blank version uses latest Valid/Signed billing`,
      async ({
        Request,
        FileUploadRequest,
        GeneratePayload,
        Responses,
        Endpoints,
        TestRunSummary,
      }) => {
        test.setTimeout(20 * 60 * 1000);
        const fx = { Request, FileUploadRequest, GeneratePayload, Responses, Endpoints };

        let scenario: Awaited<ReturnType<typeof buildBlankVersionProductScenario>>;
        await test.step('Precondition: product contract v1+v2 with distinct billing emails', async () => {
          scenario = await buildBlankVersionProductScenario(fx);
          TestRunSummary.registerPayload('scenario', {
            contractNumber: scenario.contractNumber,
            v1: scenario.productContractDetailIdV1,
            v2: scenario.productContractDetailIdV2,
            commIds: scenario.commIds,
          });
        });

        let importRow: ReturnType<typeof massCustomerFromImportRow>;
        await test.step('POST customers-import with blank version column', async () => {
          const importBody = await uploadMassEmailContractImport(
            Request,
            FileUploadRequest,
            Endpoints,
            [{ contractNumber: scenario!.contractNumber }],
          );
          const row = assertImportRowOk(importBody, scenario!.customerIdentifier);
          expect(row.productContractDetailId).toBe(scenario!.productContractDetailIdV2);
          expect(row.productContractDetailId).not.toBe(scenario!.productContractDetailIdV1);
          importRow = massCustomerFromImportRow(row);
          TestRunSummary.recordCheck({
            check: 'TC-BE-6 blank version resolves latest Valid/Signed detail',
            expectedResult: `productContractDetailId=${scenario!.productContractDetailIdV2}`,
            actualResult: `As expected — ${row.productContractDetailId}`,
            passed: true,
          });
        });

        let massEmailId = 0;
        await test.step('POST mass email createType=SEND purpose 84', async () => {
          const result = await postMassEmail(Request, Endpoints, Responses, {
            subject: 'PDT-3054 BE-6 blank version',
            contactPurposeIds: [PDT_3054_PURPOSE_BILLING],
            createType: 'SEND',
            customers: [importRow!],
          });
          expect(result.status, result.bodyText).toBe(200);
          massEmailId = result.massEmailId!;
          expect(massEmailId).toBeGreaterThan(0);
        });

        await test.step(
          'GET mass email customer preview — EMAIL_BILLING_V2 + Comm-A-v2 (not V1)',
          async () => {
            const { preview, previewId } = await waitForResolvedMassEmailCustomerPreview(
              Request,
              Endpoints,
              massEmailId,
              scenario!.customerIdentifier,
              'TC-BE-6',
            );
            const email = normalizeEmail(preview.customerEmailAddress);
            const commId = Number(preview.customerCommunicationDataShortResponse?.id);
            expect(email, `previewId=${previewId}`).toContain(normalizeEmail(EMAIL_BILLING_V2));
            expect(email).not.toContain(normalizeEmail(EMAIL_BILLING_V1));
            expect(commId).toBe(scenario!.commIds['Comm-A-v2']);
            expect(commId).not.toBe(scenario!.commIds['Comm-A-v1']);
            TestRunSummary.recordCheck({
              check: 'TC-BE-6 mass object selects latest V2 billing email/comm',
              expectedResult: `customerEmailAddress=${EMAIL_BILLING_V2}; commId=Comm-A-v2(${scenario!.commIds['Comm-A-v2']})`,
              actualResult: `As expected — email=${email}; commId=${commId}`,
              passed: true,
            });
          },
        );

        await test.step('Attach test run summary', async () => {
          finalizeTestRunSummary(TestRunSummary, Responses, {
            jiraKey: 'PDT-3054',
            relevantEntityKeys: ['customer', 'product', 'productContract'],
            extraLinks: buildProductContractTabLinks(scenario!.contractId),
            snapshot: { tc: 'TC-BE-6', massEmailId, ...scenario! },
          });
        });
      },
    );

    test(
      `[PDT-3054]: ${JIRA_TITLE} — TC-BE-7 negative no billing communication data`,
      async ({
        Request,
        FileUploadRequest,
        GeneratePayload,
        Responses,
        Endpoints,
        TestRunSummary,
      }) => {
        test.setTimeout(15 * 60 * 1000);
        const fx = { Request, FileUploadRequest, GeneratePayload, Responses, Endpoints };

        let scenario: Pdt3054ProductScenario;
        await test.step(
          'Precondition: create+sign with billing Email+Mobile then clear/remove billing for purpose 84',
          async () => {
            scenario = await buildProductContractCommScenario(fx, 'no-billing-comm');
            TestRunSummary.registerPayload('scenario', {
              ...scenario,
              billingCleared: scenario.billingCleared,
              purpose84ExpectedError: scenario.purpose84ExpectedError,
              billingClearRejectedBody: scenario.billingClearRejectedBody?.slice(0, 400),
            });
          },
        );

        let importRow: ReturnType<typeof massCustomerFromImportRow>;
        await test.step('POST customers-import', async () => {
          const importBody = await uploadMassEmailContractImport(
            Request,
            FileUploadRequest,
            Endpoints,
            [{ contractNumber: scenario!.contractNumber, version: String(scenario!.contractVersionId) }],
          );
          const row = assertImportRowOk(importBody, scenario!.customerIdentifier);
          expect(row.productContractDetailId).toBeTruthy();
          importRow = massCustomerFromImportRow(row);
        });

        await test.step('POST mass email purpose 84 — expect 400 (no contract billing / no valid email)', async () => {
          const result = await postMassEmail(Request, Endpoints, Responses, {
            subject: 'PDT-3054 BE-7 no billing comm',
            contactPurposeIds: [PDT_3054_PURPOSE_BILLING],
            createType: 'SEND',
            customers: [importRow!],
          });
          expect(result.status).toBe(400);
          const expected =
            scenario!.purpose84ExpectedError ?? ERR_CONTRACT_NO_COMM_PURPOSE_84;
          // Prefer exact purpose-missing when billing FK was cleared; otherwise Dev cannot null
          // billing — soft-delete of Comm-A yields no-valid-email for the still-linked id.
          // Also accept ERR_CONTRACT_NO_COMM_PURPOSE_84 if resolver treats inactive row as null.
          const body = result.bodyText;
          const matched =
            body.includes(expected) || body.includes(ERR_CONTRACT_NO_COMM_PURPOSE_84);
          expect(
            matched,
            `expected one of [${expected} | ${ERR_CONTRACT_NO_COMM_PURPOSE_84}]; body=${body.slice(0, 400)}`,
          ).toBeTruthy();
          expect(result.bodyText).not.toContain(EMAIL_CUSTOMER_BILLING);
          TestRunSummary.recordCheck({
            check: 'TC-BE-7 no silent fallback to customer-level billing email',
            expectedResult: `HTTP 400 containing purpose-84 missing or no-valid-email (billingCleared=${scenario!.billingCleared})`,
            actualResult: `As expected — status=${result.status}; body=${result.bodyText.slice(0, 300)}`,
            passed: true,
          });
        });

        await test.step('Attach test run summary', async () => {
          finalizeTestRunSummary(TestRunSummary, Responses, {
            jiraKey: 'PDT-3054',
            relevantEntityKeys: ['customer', 'product', 'productContract'],
            extraLinks: buildProductContractTabLinks(scenario!.contractId),
            snapshot: { tc: 'TC-BE-7', ...scenario! },
          });
        });
      },
    );

    test(
      `[PDT-3054]: ${JIRA_TITLE} — TC-BE-8 negative billing comm has no valid email`,
      async ({
        Request,
        FileUploadRequest,
        GeneratePayload,
        Responses,
        Endpoints,
        TestRunSummary,
      }) => {
        test.setTimeout(15 * 60 * 1000);
        const fx = { Request, FileUploadRequest, GeneratePayload, Responses, Endpoints };

        let scenario: Pdt3054ProductScenario;
        await test.step('Precondition: billing Comm-A dual contacts then strip EMAIL after sign', async () => {
          scenario = await buildProductContractCommScenario(fx, 'billing-no-email');
          TestRunSummary.registerPayload('scenario', scenario);
        });

        let importRow: ReturnType<typeof massCustomerFromImportRow>;
        await test.step('POST customers-import', async () => {
          const importBody = await uploadMassEmailContractImport(
            Request,
            FileUploadRequest,
            Endpoints,
            [{ contractNumber: scenario!.contractNumber, version: String(scenario!.contractVersionId) }],
          );
          importRow = massCustomerFromImportRow(
            assertImportRowOk(importBody, scenario!.customerIdentifier),
          );
        });

        await test.step('POST mass email purpose 84 — expect 400 no valid email', async () => {
          const commId = scenario!.commIds['Comm-A'];
          const result = await postMassEmail(Request, Endpoints, Responses, {
            subject: 'PDT-3054 BE-8 no email on billing comm',
            contactPurposeIds: [PDT_3054_PURPOSE_BILLING],
            createType: 'SEND',
            customers: [importRow!],
          });
          expect(result.status).toBe(400);
          expect(result.bodyText).toContain(
            `${ERR_COMM_NO_VALID_EMAIL_PREFIX} ${commId}) ${ERR_COMM_NO_VALID_EMAIL_SUFFIX}`,
          );
          expect(result.bodyText).not.toContain(EMAIL_CONTRACT);
          TestRunSummary.recordCheck({
            check: 'TC-BE-8 billing comm without email rejects mass email',
            expectedResult: `HTTP 400 with no-valid-email fragment for id ${commId}`,
            actualResult: `As expected — status=${result.status}; body=${result.bodyText.slice(0, 300)}`,
            passed: true,
          });
        });

        await test.step('Attach test run summary', async () => {
          finalizeTestRunSummary(TestRunSummary, Responses, {
            jiraKey: 'PDT-3054',
            relevantEntityKeys: ['customer', 'product', 'productContract'],
            extraLinks: buildProductContractTabLinks(scenario!.contractId),
            snapshot: { tc: 'TC-BE-8', ...scenario! },
          });
        });
      },
    );

    test(
      `[PDT-3054]: ${JIRA_TITLE} — TC-BE-9 negative billing comm has no valid mobile`,
      async ({
        Request,
        FileUploadRequest,
        GeneratePayload,
        Responses,
        Endpoints,
        TestRunSummary,
      }) => {
        test.setTimeout(15 * 60 * 1000);
        const fx = { Request, FileUploadRequest, GeneratePayload, Responses, Endpoints };

        let scenario: Pdt3054ProductScenario;
        await test.step('Precondition: billing Comm-A dual contacts then strip MOBILE after sign', async () => {
          scenario = await buildProductContractCommScenario(fx, 'billing-no-mobile');
          TestRunSummary.registerPayload('scenario', scenario);
        });

        let importRow: ReturnType<typeof massCustomerFromImportRow>;
        await test.step('POST sms-communication/parse MASS_SMS_CONTRACT', async () => {
          const importBody = await uploadMassSmsContractParse(
            Request,
            FileUploadRequest,
            Endpoints,
            [{ contractNumber: scenario!.contractNumber, version: String(scenario!.contractVersionId) }],
          );
          const row = assertImportRowOk(importBody, scenario!.customerIdentifier);
          expect(row.productContractDetailId).toBeTruthy();
          importRow = massCustomerFromImportRow(row);
        });

        await test.step('POST sms-communication/mass purpose 84 — expect 400 no valid mobile', async () => {
          const commId = scenario!.commIds['Comm-A'];
          const result = await postMassSms(Request, Endpoints, Responses, {
            smsBody: 'PDT-3054 BE-9 no mobile on billing comm',
            contactPurposeIds: [PDT_3054_PURPOSE_BILLING],
            saveAs: 'SEND',
            customers: [importRow!],
          });
          expect(result.status).toBe(400);
          expect(result.bodyText).toContain(
            `${ERR_COMM_NO_VALID_EMAIL_PREFIX} ${commId}) ${ERR_COMM_NO_VALID_MOBILE_SUFFIX}`,
          );
          expect(result.bodyText).not.toContain(MOBILE_CONTRACT);
          TestRunSummary.recordCheck({
            check: 'TC-BE-9 billing comm without mobile rejects mass SMS',
            expectedResult: `HTTP 400 with no-valid-mobile fragment for id ${commId}`,
            actualResult: `As expected — status=${result.status}; body=${result.bodyText.slice(0, 300)}`,
            passed: true,
          });
        });

        await test.step('Attach test run summary', async () => {
          finalizeTestRunSummary(TestRunSummary, Responses, {
            jiraKey: 'PDT-3054',
            relevantEntityKeys: ['customer', 'product', 'productContract'],
            extraLinks: buildProductContractTabLinks(scenario!.contractId),
            snapshot: { tc: 'TC-BE-9', ...scenario! },
          });
        });
      },
    );

    test(
      `[PDT-3054]: ${JIRA_TITLE} — TC-BE-10 customer import still uses customer-level comm`,
      async ({
        Request,
        FileUploadRequest,
        GeneratePayload,
        Responses,
        Endpoints,
        TestRunSummary,
      }) => {
        test.setTimeout(10 * 60 * 1000);
        const fx = { Request, FileUploadRequest, GeneratePayload, Responses, Endpoints };

        let customerId = 0;
        let customerIdentifier = '';
        let commCustomerOnlyId = 0;
        await test.step('Precondition: customer with Comm-D purpose 84 (Email+Mobile for create)', async () => {
          const created = await createCustomerWithComms(fx, [
            {
              tag: 'Comm-D',
              purposeIds: [PDT_3054_PURPOSE_BILLING],
              contacts: [
                { contactType: 'EMAIL', contactValue: EMAIL_CUSTOMER_BILLING },
                {
                  contactType: 'MOBILE_NUMBER',
                  contactValue: MOBILE_PLACEHOLDER_CUSTOMER,
                  sendSms: true,
                },
              ],
            },
          ]);
          customerId = created.customerId;
          customerIdentifier = created.customerIdentifier;
          commCustomerOnlyId = created.commIds['Comm-D'];
          TestRunSummary.registerPayload('customer', {
            customerId,
            customerIdentifier,
            commCustomerOnlyId,
          });
        });

        let importRow: ReturnType<typeof massCustomerFromImportRow>;
        await test.step('POST customers-import MASS_IMPORT_OF_CUSTOMERS', async () => {
          const importBody = await uploadMassEmailCustomerImport(
            Request,
            FileUploadRequest,
            Endpoints,
            customerIdentifier,
          );
          const row = assertImportRowOk(importBody, customerIdentifier);
          importRow = massCustomerFromImportRow(row);
        });

        let massEmailId = 0;
        await test.step('POST mass email SEND purpose 84', async () => {
          const result = await postMassEmail(Request, Endpoints, Responses, {
            subject: 'PDT-3054 BE-10 customer import',
            contactPurposeIds: [PDT_3054_PURPOSE_BILLING],
            createType: 'SEND',
            customers: [importRow!],
          });
          expect(result.status, result.bodyText).toBe(200);
          massEmailId = result.massEmailId!;
          expect(massEmailId).toBeGreaterThan(0);
          TestRunSummary.recordCheck({
            check: 'TC-BE-10 customer import mass email SEND succeeds',
            expectedResult: 'HTTP 200 massEmailId; customer-level purpose 84',
            actualResult: `As expected — status=${result.status} id=${massEmailId}`,
            passed: true,
          });
        });

        await test.step(
          'GET mass email customer preview — EMAIL_CUSTOMER_BILLING + Comm-D',
          async () => {
            const { preview, previewId } = await waitForResolvedMassEmailCustomerPreview(
              Request,
              Endpoints,
              massEmailId,
              customerIdentifier,
              'TC-BE-10',
            );
            const email = normalizeEmail(preview.customerEmailAddress);
            const commId = Number(preview.customerCommunicationDataShortResponse?.id);
            expect(email, `previewId=${previewId}`).toContain(
              normalizeEmail(EMAIL_CUSTOMER_BILLING),
            );
            expect(commId).toBe(commCustomerOnlyId);
            TestRunSummary.recordCheck({
              check: 'TC-BE-10 mass object selects customer-level billing email/comm',
              expectedResult: `customerEmailAddress=${EMAIL_CUSTOMER_BILLING}; commId=Comm-D(${commCustomerOnlyId})`,
              actualResult: `As expected — email=${email}; commId=${commId}`,
              passed: true,
            });
          },
        );

        await test.step('Attach test run summary', async () => {
          finalizeTestRunSummary(TestRunSummary, Responses, {
            jiraKey: 'PDT-3054',
            relevantEntityKeys: ['customer'],
            snapshot: {
              tc: 'TC-BE-10',
              massEmailId,
              customerId,
              customerIdentifier,
              commCustomerOnlyId,
            },
          });
        });
      },
    );

    test(
      `[PDT-3054]: ${JIRA_TITLE} — TC-BE-11 Mass SMS product contract purpose 83`,
      async ({
        Request,
        FileUploadRequest,
        GeneratePayload,
        Responses,
        Endpoints,
        TestRunSummary,
      }) => {
        test.setTimeout(15 * 60 * 1000);
        const fx = { Request, FileUploadRequest, GeneratePayload, Responses, Endpoints };

        let scenario: Pdt3054ProductScenario;
        await test.step('Precondition: product contract SMS Comm-A/B/C', async () => {
          scenario = await buildProductContractCommScenario(fx, 'sms-three-comms');
          TestRunSummary.registerPayload('scenario', scenario);
        });

        let importRow: ReturnType<typeof massCustomerFromImportRow>;
        await test.step('POST sms-communication/parse MASS_SMS_CONTRACT', async () => {
          const importBody = await uploadMassSmsContractParse(
            Request,
            FileUploadRequest,
            Endpoints,
            [{ contractNumber: scenario!.contractNumber, version: String(scenario!.contractVersionId) }],
          );
          const row = assertImportRowOk(importBody, scenario!.customerIdentifier);
          expect(row.productContractDetailId).toBeTruthy();
          importRow = massCustomerFromImportRow(row);
        });

        let smsId = 0;
        await test.step('POST sms-communication/mass saveAs=SEND contactPurposeIds=[83]', async () => {
          const result = await postMassSms(Request, Endpoints, Responses, {
            smsBody: 'PDT-3054 BE-11 contract mobile purpose 83',
            contactPurposeIds: [PDT_3054_PURPOSE_CONTRACT],
            saveAs: 'SEND',
            customers: [importRow!],
          });
          expect(result.status, result.bodyText).toBe(200);
          smsId = result.smsCommunicationId!;
          expect(smsId).toBeGreaterThan(0);
        });

        await test.step(
          'GET mass SMS customer preview — MOBILE_CONTRACT + Comm-B (not billing/customer mobiles)',
          async () => {
            const { preview, previewId } = await waitForResolvedMassSmsCustomerPreview(
              Request,
              Endpoints,
              smsId,
              scenario!.customerIdentifier,
              'TC-BE-11',
            );
            const phone = normalizeMobileDigits(preview.phoneNumber);
            const contractDigits = normalizeMobileDigits(MOBILE_CONTRACT);
            const billingDigits = normalizeMobileDigits(MOBILE_BILLING);
            const customerDigits = normalizeMobileDigits(MOBILE_CUSTOMER);
            const commId = Number(preview.customerCommunicationDataShortResponse?.id);
            expect(
              phone.endsWith(contractDigits.slice(-9)),
              `previewId=${previewId} phone=${preview.phoneNumber}`,
            ).toBeTruthy();
            expect(phone.endsWith(billingDigits.slice(-9))).toBeFalsy();
            expect(phone.endsWith(customerDigits.slice(-9))).toBeFalsy();
            expect(commId).toBe(scenario!.commIds['Comm-B']);
            TestRunSummary.recordCheck({
              check: 'TC-BE-11 mass SMS object selects contract-purpose mobile/comm',
              expectedResult: `phoneNumber≈${MOBILE_CONTRACT}; commId=Comm-B(${scenario!.commIds['Comm-B']})`,
              actualResult: `As expected — phone=${preview.phoneNumber}; commId=${commId}`,
              passed: true,
            });
          },
        );

        await test.step('Attach test run summary', async () => {
          finalizeTestRunSummary(TestRunSummary, Responses, {
            jiraKey: 'PDT-3054',
            relevantEntityKeys: ['customer', 'product', 'productContract'],
            extraLinks: buildProductContractTabLinks(scenario!.contractId),
            snapshot: { tc: 'TC-BE-11', smsId, ...scenario! },
          });
        });
      },
    );

    test(
      `[PDT-3054]: ${JIRA_TITLE} — TC-BE-12 Mass SMS both purposes 84+83`,
      async ({
        Request,
        FileUploadRequest,
        GeneratePayload,
        Responses,
        Endpoints,
        TestRunSummary,
      }) => {
        test.setTimeout(15 * 60 * 1000);
        const fx = { Request, FileUploadRequest, GeneratePayload, Responses, Endpoints };

        let scenario: Pdt3054ProductScenario;
        await test.step('Precondition: product contract SMS Comm-A + Comm-B distinct mobiles', async () => {
          scenario = await buildProductContractCommScenario(fx, 'sms-three-comms');
          TestRunSummary.registerPayload('scenario', scenario);
        });

        let importRow: ReturnType<typeof massCustomerFromImportRow>;
        await test.step('POST sms-communication/parse MASS_SMS_CONTRACT', async () => {
          const importBody = await uploadMassSmsContractParse(
            Request,
            FileUploadRequest,
            Endpoints,
            [{ contractNumber: scenario!.contractNumber, version: String(scenario!.contractVersionId) }],
          );
          importRow = massCustomerFromImportRow(
            assertImportRowOk(importBody, scenario!.customerIdentifier),
          );
        });

        let smsId = 0;
        await test.step('POST sms-communication/mass saveAs=SEND contactPurposeIds=[84,83]', async () => {
          const result = await postMassSms(Request, Endpoints, Responses, {
            smsBody: 'PDT-3054 BE-12 both purposes',
            contactPurposeIds: [PDT_3054_PURPOSE_BILLING, PDT_3054_PURPOSE_CONTRACT],
            saveAs: 'SEND',
            customers: [importRow!],
          });
          expect(result.status, result.bodyText).toBe(200);
          smsId = result.smsCommunicationId!;
          expect(smsId).toBeGreaterThan(0);
        });

        await test.step(
          'GET all mass-linked SMS customer previews — union MOBILE_BILLING + MOBILE_CONTRACT',
          async () => {
            const previews = await collectMassSmsCustomerPreviews(
              Request,
              Endpoints,
              smsId,
              scenario!.customerIdentifier,
            );
            const phones = previews.map((p) => normalizeMobileDigits(p.preview.phoneNumber));
            const billingDigits = normalizeMobileDigits(MOBILE_BILLING);
            const contractDigits = normalizeMobileDigits(MOBILE_CONTRACT);
            const commIds = previews
              .map((p) => Number(p.preview.customerCommunicationDataShortResponse?.id))
              .filter((id) => Number.isFinite(id) && id > 0);
            expect(
              phones.some((p) => p.endsWith(billingDigits.slice(-9))),
              `phones=${JSON.stringify(phones)}`,
            ).toBeTruthy();
            expect(
              phones.some((p) => p.endsWith(contractDigits.slice(-9))),
              `phones=${JSON.stringify(phones)}`,
            ).toBeTruthy();
            expect(commIds).toContain(scenario!.commIds['Comm-A']);
            expect(commIds).toContain(scenario!.commIds['Comm-B']);
            TestRunSummary.recordCheck({
              check: 'TC-BE-12 mass SMS object resolves billing + contract mobiles',
              expectedResult: `phones include ${MOBILE_BILLING} + ${MOBILE_CONTRACT}; commIds Comm-A+Comm-B`,
              actualResult: `As expected — phones=${phones.join('|')}; commIds=${commIds.join(',')}; rows=${previews.length}`,
              passed: true,
            });
          },
        );

        await test.step('Attach test run summary', async () => {
          finalizeTestRunSummary(TestRunSummary, Responses, {
            jiraKey: 'PDT-3054',
            relevantEntityKeys: ['customer', 'product', 'productContract'],
            extraLinks: buildProductContractTabLinks(scenario!.contractId),
            snapshot: { tc: 'TC-BE-12', smsId, ...scenario! },
          });
        });
      },
    );

    test(
      `[PDT-3054]: ${JIRA_TITLE} — TC-BE-13 Mass Email service contract purpose 84`,
      async ({
        Request,
        FileUploadRequest,
        GeneratePayload,
        Responses,
        Endpoints,
        TestRunSummary,
      }) => {
        test.setTimeout(15 * 60 * 1000);
        const fx = { Request, FileUploadRequest, GeneratePayload, Responses, Endpoints };

        let scenario: Awaited<ReturnType<typeof buildServiceContractCommScenario>>;
        await test.step(
          'Precondition: signed service contract with Comm-A/B + Comm-C customer billing trap',
          async () => {
            scenario = await buildServiceContractCommScenario(fx, 'email-three-comms');
            TestRunSummary.registerPayload('scenario', scenario);
            expect(scenario.serviceContractDetailId).toBeTruthy();
            expect(scenario.commIds['Comm-C']).toBeTruthy();
          },
        );

        let importRow: ReturnType<typeof massCustomerFromImportRow>;
        await test.step('POST customers-import service contract row', async () => {
          const importBody = await uploadMassEmailContractImport(
            Request,
            FileUploadRequest,
            Endpoints,
            [
              {
                contractNumber: scenario!.contractNumber,
                version: String(scenario!.contractVersionId),
              },
            ],
          );
          const row = assertImportRowOk(importBody, scenario!.customerIdentifier);
          expect(row.serviceContractDetailId, 'serviceContractDetailId from import').toBeTruthy();
          importRow = massCustomerFromImportRow(row);
        });

        let massEmailId = 0;
        await test.step('POST mass email SEND contactPurposeIds=[84]', async () => {
          const result = await postMassEmail(Request, Endpoints, Responses, {
            subject: 'PDT-3054 BE-13 service contract billing purpose',
            contactPurposeIds: [PDT_3054_PURPOSE_BILLING],
            createType: 'SEND',
            customers: [importRow!],
          });
          expect(result.status, result.bodyText).toBe(200);
          massEmailId = result.massEmailId!;
          expect(massEmailId).toBeGreaterThan(0);
        });

        await test.step(
          'GET mass email customer preview — EMAIL_BILLING + Comm-A (not customer-level Comm-C)',
          async () => {
            const { preview, previewId } = await waitForResolvedMassEmailCustomerPreview(
              Request,
              Endpoints,
              massEmailId,
              scenario!.customerIdentifier,
              'TC-BE-13',
            );
            const email = normalizeEmail(preview.customerEmailAddress);
            const commId = Number(preview.customerCommunicationDataShortResponse?.id);
            expect(email, `previewId=${previewId}`).toContain(normalizeEmail(EMAIL_BILLING));
            expect(email).not.toContain(normalizeEmail(EMAIL_CUSTOMER_BILLING));
            expect(commId).toBe(scenario!.commIds['Comm-A']);
            expect(commId).not.toBe(scenario!.commIds['Comm-C']);
            TestRunSummary.recordCheck({
              check: 'TC-BE-13 mass object selects service-contract billing email/comm',
              expectedResult: `customerEmailAddress=${EMAIL_BILLING}; commId=Comm-A(${scenario!.commIds['Comm-A']})`,
              actualResult: `As expected — email=${email}; commId=${commId}`,
              passed: true,
            });
          },
        );

        await test.step('Attach test run summary', async () => {
          finalizeTestRunSummary(TestRunSummary, Responses, {
            jiraKey: 'PDT-3054',
            relevantEntityKeys: ['customer', 'service', 'serviceContract'],
            snapshot: { tc: 'TC-BE-13', massEmailId, ...scenario! },
          });
        });
      },
    );

    test(
      `[PDT-3054]: ${JIRA_TITLE} — TC-BE-14 Mass SMS service contract purpose 84`,
      async ({
        Request,
        FileUploadRequest,
        GeneratePayload,
        Responses,
        Endpoints,
        TestRunSummary,
      }) => {
        test.setTimeout(15 * 60 * 1000);
        const fx = { Request, FileUploadRequest, GeneratePayload, Responses, Endpoints };

        let scenario: Awaited<ReturnType<typeof buildServiceContractCommScenario>>;
        await test.step('Precondition: signed service contract SMS Comm-A/B/C', async () => {
          scenario = await buildServiceContractCommScenario(fx, 'sms-three-comms');
          TestRunSummary.registerPayload('scenario', scenario);
          expect(scenario.serviceContractDetailId).toBeTruthy();
        });

        let importRow: ReturnType<typeof massCustomerFromImportRow>;
        await test.step('POST sms-communication/parse MASS_SMS_CONTRACT', async () => {
          const importBody = await uploadMassSmsContractParse(
            Request,
            FileUploadRequest,
            Endpoints,
            [
              {
                contractNumber: scenario!.contractNumber,
                version: String(scenario!.contractVersionId),
              },
            ],
          );
          const row = assertImportRowOk(importBody, scenario!.customerIdentifier);
          expect(row.serviceContractDetailId, 'serviceContractDetailId from SMS parse').toBeTruthy();
          importRow = massCustomerFromImportRow(row);
        });

        let smsId = 0;
        await test.step('POST sms-communication/mass saveAs=SEND contactPurposeIds=[84]', async () => {
          const result = await postMassSms(Request, Endpoints, Responses, {
            smsBody: 'PDT-3054 BE-14 service contract billing mobile',
            contactPurposeIds: [PDT_3054_PURPOSE_BILLING],
            saveAs: 'SEND',
            customers: [importRow!],
          });
          expect(result.status, result.bodyText).toBe(200);
          smsId = result.smsCommunicationId!;
          expect(smsId).toBeGreaterThan(0);
        });

        await test.step(
          'GET mass SMS customer preview — MOBILE_BILLING + Comm-A (not customer-level mobile)',
          async () => {
            const { preview, previewId } = await waitForResolvedMassSmsCustomerPreview(
              Request,
              Endpoints,
              smsId,
              scenario!.customerIdentifier,
              'TC-BE-14',
            );
            const phone = normalizeMobileDigits(preview.phoneNumber);
            const billingDigits = normalizeMobileDigits(MOBILE_BILLING);
            const customerDigits = normalizeMobileDigits(MOBILE_CUSTOMER);
            const commId = Number(preview.customerCommunicationDataShortResponse?.id);
            expect(
              phone.endsWith(billingDigits.slice(-9)),
              `previewId=${previewId} phone=${preview.phoneNumber}`,
            ).toBeTruthy();
            expect(phone.endsWith(customerDigits.slice(-9))).toBeFalsy();
            expect(commId).toBe(scenario!.commIds['Comm-A']);
            TestRunSummary.recordCheck({
              check: 'TC-BE-14 mass SMS object selects service-contract billing mobile/comm',
              expectedResult: `phoneNumber≈${MOBILE_BILLING}; commId=Comm-A(${scenario!.commIds['Comm-A']})`,
              actualResult: `As expected — phone=${preview.phoneNumber}; commId=${commId}`,
              passed: true,
            });
          },
        );

        await test.step('Attach test run summary', async () => {
          finalizeTestRunSummary(TestRunSummary, Responses, {
            jiraKey: 'PDT-3054',
            relevantEntityKeys: ['customer', 'service', 'serviceContract'],
            snapshot: { tc: 'TC-BE-14', smsId, ...scenario! },
          });
        });
      },
    );

    test(
      `[PDT-3054]: ${JIRA_TITLE} — TC-BE-15 Mass SMS negative no usable billing`,
      async ({
        Request,
        FileUploadRequest,
        GeneratePayload,
        Responses,
        Endpoints,
        TestRunSummary,
      }) => {
        test.setTimeout(15 * 60 * 1000);
        const fx = { Request, FileUploadRequest, GeneratePayload, Responses, Endpoints };

        let scenario: Pdt3054ProductScenario;
        await test.step(
          'Precondition: create+sign with billing Email+Mobile then clear/remove billing for purpose 84',
          async () => {
            scenario = await buildProductContractCommScenario(fx, 'no-billing-comm');
            TestRunSummary.registerPayload('scenario', {
              ...scenario,
              billingCleared: scenario.billingCleared,
              purpose84ExpectedError: scenario.purpose84ExpectedError,
              billingClearRejectedBody: scenario.billingClearRejectedBody?.slice(0, 400),
            });
          },
        );

        let importRow: ReturnType<typeof massCustomerFromImportRow>;
        await test.step('POST sms-communication/parse MASS_SMS_CONTRACT', async () => {
          const importBody = await uploadMassSmsContractParse(
            Request,
            FileUploadRequest,
            Endpoints,
            [{ contractNumber: scenario!.contractNumber, version: String(scenario!.contractVersionId) }],
          );
          const row = assertImportRowOk(importBody, scenario!.customerIdentifier);
          expect(row.productContractDetailId).toBeTruthy();
          importRow = massCustomerFromImportRow(row);
        });

        await test.step('POST mass SMS purpose 84 — expect 400 (no contract billing / no valid mobile)', async () => {
          const billingId = scenario!.commIds['Comm-A'];
          const result = await postMassSms(Request, Endpoints, Responses, {
            smsBody: 'PDT-3054 BE-15 no billing comm',
            contactPurposeIds: [PDT_3054_PURPOSE_BILLING],
            saveAs: 'SEND',
            customers: [importRow!],
          });
          expect(result.status).toBe(400);
          const noValidMobile =
            billingId != null
              ? `${ERR_COMM_NO_VALID_EMAIL_PREFIX} ${billingId}) ${ERR_COMM_NO_VALID_MOBILE_SUFFIX}`
              : '';
          const body = result.bodyText;
          const matched =
            body.includes(ERR_CONTRACT_NO_COMM_PURPOSE_84) ||
            (noValidMobile !== '' && body.includes(noValidMobile));
          expect(
            matched,
            `expected one of [${ERR_CONTRACT_NO_COMM_PURPOSE_84} | no-valid-mobile for Comm-A]; body=${body.slice(0, 400)}`,
          ).toBeTruthy();
          expect(body).not.toContain(MOBILE_CUSTOMER);
          expect(body).not.toContain(MOBILE_PLACEHOLDER_CUSTOMER);
          TestRunSummary.recordCheck({
            check: 'TC-BE-15 no silent fallback to customer-level billing mobile',
            expectedResult: `HTTP 400 containing purpose-84 missing or no-valid-mobile (billingCleared=${scenario!.billingCleared})`,
            actualResult: `As expected — status=${result.status}; body=${result.bodyText.slice(0, 300)}`,
            passed: true,
          });
        });

        await test.step('Attach test run summary', async () => {
          finalizeTestRunSummary(TestRunSummary, Responses, {
            jiraKey: 'PDT-3054',
            relevantEntityKeys: ['customer', 'product', 'productContract'],
            extraLinks: buildProductContractTabLinks(scenario!.contractId),
            snapshot: { tc: 'TC-BE-15', ...scenario! },
          });
        });
      },
    );

    test(
      `[PDT-3054]: ${JIRA_TITLE} — TC-BE-16 Mass Email negative purpose 83 contract comm has no email`,
      async ({
        Request,
        FileUploadRequest,
        GeneratePayload,
        Responses,
        Endpoints,
        TestRunSummary,
      }) => {
        test.setTimeout(15 * 60 * 1000);
        const fx = { Request, FileUploadRequest, GeneratePayload, Responses, Endpoints };

        let scenario: Pdt3054ProductScenario;
        await test.step('Precondition: Comm-A+Comm-B dual contacts then strip EMAIL from Comm-B', async () => {
          scenario = await buildProductContractCommScenario(fx, 'contract-no-email');
          TestRunSummary.registerPayload('scenario', scenario);
        });

        let importRow: ReturnType<typeof massCustomerFromImportRow>;
        await test.step('POST customers-import', async () => {
          const importBody = await uploadMassEmailContractImport(
            Request,
            FileUploadRequest,
            Endpoints,
            [{ contractNumber: scenario!.contractNumber, version: String(scenario!.contractVersionId) }],
          );
          importRow = massCustomerFromImportRow(
            assertImportRowOk(importBody, scenario!.customerIdentifier),
          );
        });

        await test.step('POST mass email purpose 83 — expect 400 no valid email on Comm-B', async () => {
          const commId = scenario!.commIds['Comm-B'];
          const result = await postMassEmail(Request, Endpoints, Responses, {
            subject: 'PDT-3054 BE-16 no email on contract comm',
            contactPurposeIds: [PDT_3054_PURPOSE_CONTRACT],
            createType: 'SEND',
            customers: [importRow!],
          });
          expect(result.status).toBe(400);
          expect(result.bodyText).toContain(
            `${ERR_COMM_NO_VALID_EMAIL_PREFIX} ${commId}) ${ERR_COMM_NO_VALID_EMAIL_SUFFIX_83}`,
          );
          expect(result.bodyText).not.toContain(EMAIL_BILLING);
          TestRunSummary.recordCheck({
            check: 'TC-BE-16 contract comm without email rejects mass email purpose 83',
            expectedResult: `HTTP 400 with no-valid-email fragment for Comm-B id ${commId} purpose 83`,
            actualResult: `As expected — status=${result.status}; body=${result.bodyText.slice(0, 300)}`,
            passed: true,
          });
        });

        await test.step('Attach test run summary', async () => {
          finalizeTestRunSummary(TestRunSummary, Responses, {
            jiraKey: 'PDT-3054',
            relevantEntityKeys: ['customer', 'product', 'productContract'],
            extraLinks: buildProductContractTabLinks(scenario!.contractId),
            snapshot: { tc: 'TC-BE-16', ...scenario! },
          });
        });
      },
    );
  },
);
