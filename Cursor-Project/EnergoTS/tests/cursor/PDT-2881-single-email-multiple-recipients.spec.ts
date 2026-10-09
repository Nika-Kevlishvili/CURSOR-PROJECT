import { test, expect } from '../../fixtures/baseFixture';

import reportGenerator from '../../utils/generateReport';

import {

  annotateDbAssertSkipped,

  assertEmailContactsFromDb,

  assertEmailSubjectCountFromDb,

  assertEmailValidationError,

  assertMultiRecipientEmailCommunication,

  assertNoEmailPersistedBySubject,

  assertOutgoingEmailView,

  buildEmailCommunicationPayload,

  buildLongEmailSegmentOver128,

  getEmailCommunicationView,

  postEmailCommunication,

  resendEmailCommunication,

  runPdt2881SharedPreconditions,

  waitForEmailStatusLeavingDraft,

} from './pdt-2881-email-multi-recipient.fixtures';



test.describe(

  '[PDT-2881]: BACKEND - Sending single email to multiple recipients',

  { tag: '@customerComm' },

  () => {

    test('[PDT-2881] TC-BE-1: Two recipients — one shared task_id', async ({

      Request,

      GeneratePayload,

      Endpoints,

      Responses,

      Nomenclatures,

    }) => {

      let emailCommunicationId: number;



      const pre = await test.step('Precondition: token, topic, mailbox, customer', async () =>

        runPdt2881SharedPreconditions(

          Request,

          GeneratePayload,

          Endpoints,

          Responses,

          Nomenclatures,

          'tc-be-1',

        ));



      await test.step('Precondition delta: two-recipient SEND payload', async () => {

        const payload = buildEmailCommunicationPayload(pre, {

          customerEmailAddress: 'pdt2881.a.test@example.com;pdt2881.b.test@example.com',

          emailSubject: 'PDT-2881 TC-BE-1',

          emailCreateType: 'SEND',

        });

        emailCommunicationId = await postEmailCommunication(

          Request,

          Endpoints,

          payload,

          Responses,

        );

      });



      await test.step('GET /email-communication/{id}?type=EMAIL — outgoing multi-recipient view', async () => {

        const details = await getEmailCommunicationView(Request, Endpoints, emailCommunicationId);

        assertOutgoingEmailView(details);

        expect(details.emailSubject).toBe('PDT-2881 TC-BE-1');

        expect(details.customerEmailAddress).toContain('pdt2881.a.test@example.com');

        expect(details.customerEmailAddress).toContain('pdt2881.b.test@example.com');



        assertMultiRecipientEmailCommunication(details, emailCommunicationId, {

          expectedRecipientCount: 2,

          expectedEmails: ['pdt2881.a.test@example.com', 'pdt2881.b.test@example.com'],

          requireSentDateWhenSuccessful: true,

        });

      });



      await test.step('SQL: email_communication_customer_contacts — one shared task_id', async () => {

        annotateDbAssertSkipped();

        await assertEmailContactsFromDb(emailCommunicationId, {

          expectedRowCount: 2,

          expectedEmails: ['pdt2881.a.test@example.com', 'pdt2881.b.test@example.com'],

          distinctNonNullTaskIdCount: 1,

        });

      });



      test.info().attach('[PDT-2881] TC-BE-1 response', {

        body: JSON.stringify(reportGenerator.setLinksToResponses(Responses), null, 2),

        contentType: 'application/json',

      });

    });



    test('[PDT-2881] TC-BE-2: Three recipients — one task_id', async ({

      Request,

      GeneratePayload,

      Endpoints,

      Responses,

      Nomenclatures,

    }) => {

      let emailCommunicationId: number;



      const pre = await test.step('Precondition: token, topic, mailbox, customer', async () =>

        runPdt2881SharedPreconditions(

          Request,

          GeneratePayload,

          Endpoints,

          Responses,

          Nomenclatures,

          'tc-be-2',

        ));



      await test.step('Precondition delta: three-recipient SEND payload', async () => {

        const payload = buildEmailCommunicationPayload(pre, {

          customerEmailAddress:

            'pdt2881.r1@example.com;pdt2881.r2@example.com;pdt2881.r3@example.com',

          emailSubject: 'PDT-2881 TC-BE-2',

          emailCreateType: 'SEND',

        });

        emailCommunicationId = await postEmailCommunication(

          Request,

          Endpoints,

          payload,

          Responses,

        );

      });



      await test.step('GET view and assert three distinct recipients on one communication', async () => {

        const details = await getEmailCommunicationView(Request, Endpoints, emailCommunicationId);

        assertOutgoingEmailView(details);

        expect(details.emailSubject).toBe('PDT-2881 TC-BE-2');

        assertMultiRecipientEmailCommunication(details, emailCommunicationId, {

          expectedRecipientCount: 3,

          expectedEmails: [

            'pdt2881.r1@example.com',

            'pdt2881.r2@example.com',

            'pdt2881.r3@example.com',

          ],

        });

      });



      await test.step('SQL: three contact rows — one distinct task_id', async () => {

        annotateDbAssertSkipped();

        await assertEmailContactsFromDb(emailCommunicationId, {

          expectedRowCount: 3,

          expectedEmails: [

            'pdt2881.r1@example.com',

            'pdt2881.r2@example.com',

            'pdt2881.r3@example.com',

          ],

          distinctNonNullTaskIdCount: 1,

        });

      });



      test.info().attach('[PDT-2881] TC-BE-2 response', {

        body: JSON.stringify(reportGenerator.setLinksToResponses(Responses), null, 2),

        contentType: 'application/json',

      });

    });



    test('[PDT-2881] TC-BE-3: Duplicate semicolon entries — one distinct recipient', async ({

      Request,

      GeneratePayload,

      Endpoints,

      Responses,

      Nomenclatures,

    }) => {

      let emailCommunicationId: number;



      const pre = await test.step('Precondition: token, topic, mailbox, customer', async () =>

        runPdt2881SharedPreconditions(

          Request,

          GeneratePayload,

          Endpoints,

          Responses,

          Nomenclatures,

          'tc-be-3',

        ));



      await test.step('Precondition delta: duplicate address segments in customerEmailAddress', async () => {

        const payload = buildEmailCommunicationPayload(pre, {

          customerEmailAddress: 'pdt2881.dup@example.com; pdt2881.dup@example.com',

          emailSubject: 'PDT-2881 TC-BE-3',

          emailCreateType: 'SEND',

        });

        emailCommunicationId = await postEmailCommunication(

          Request,

          Endpoints,

          payload,

          Responses,

        );

      });



      await test.step('GET view — deduplicated single distinct recipient', async () => {

        const details = await getEmailCommunicationView(Request, Endpoints, emailCommunicationId);

        assertOutgoingEmailView(details);

        assertMultiRecipientEmailCommunication(details, emailCommunicationId, {

          expectedRecipientCount: 1,

          expectedEmails: ['pdt2881.dup@example.com'],

        });

      });



      await test.step('SQL: one distinct email_address and one task_id', async () => {

        annotateDbAssertSkipped();

        await assertEmailContactsFromDb(emailCommunicationId, {

          expectedRowCount: 1,

          expectedEmails: ['pdt2881.dup@example.com'],

          distinctNonNullTaskIdCount: 1,

        });

      });



      test.info().attach('[PDT-2881] TC-BE-3 response', {

        body: JSON.stringify(reportGenerator.setLinksToResponses(Responses), null, 2),

        contentType: 'application/json',

      });

    });



    test('[PDT-2881] TC-BE-4: Single recipient regression', async ({

      Request,

      GeneratePayload,

      Endpoints,

      Responses,

      Nomenclatures,

    }) => {

      let emailCommunicationId: number;



      const pre = await test.step('Precondition: token, topic, mailbox, customer', async () =>

        runPdt2881SharedPreconditions(

          Request,

          GeneratePayload,

          Endpoints,

          Responses,

          Nomenclatures,

          'tc-be-4',

        ));



      await test.step('Precondition delta: single recipient without semicolon', async () => {

        const payload = buildEmailCommunicationPayload(pre, {

          customerEmailAddress: 'pdt2881.single@example.com',

          emailSubject: 'PDT-2881 TC-BE-4',

          emailCreateType: 'SEND',

        });

        emailCommunicationId = await postEmailCommunication(

          Request,

          Endpoints,

          payload,

          Responses,

        );

      });



      await test.step('GET view — single-recipient regression', async () => {

        const details = await getEmailCommunicationView(Request, Endpoints, emailCommunicationId);

        assertOutgoingEmailView(details);

        expect(details.emailSubject).toBe('PDT-2881 TC-BE-4');

        expect(String(details.emailBody ?? '')).toContain('PDT-2881');

        expect(details.customerEmailAddress).toBe('pdt2881.single@example.com');

        assertMultiRecipientEmailCommunication(details, emailCommunicationId, {

          expectedRecipientCount: 1,

          expectedEmails: ['pdt2881.single@example.com'],

        });

      });



      await test.step('SQL: one contact row with one task_id', async () => {

        annotateDbAssertSkipped();

        await assertEmailContactsFromDb(emailCommunicationId, {

          expectedRowCount: 1,

          expectedEmails: ['pdt2881.single@example.com'],

          distinctNonNullTaskIdCount: 1,

        });

      });



      test.info().attach('[PDT-2881] TC-BE-4 response', {

        body: JSON.stringify(reportGenerator.setLinksToResponses(Responses), null, 2),

        contentType: 'application/json',

      });

    });



    test('[PDT-2881] TC-BE-5: Invalid email segment in list', async ({

      Request,

      GeneratePayload,

      Endpoints,

      Responses,

      Nomenclatures,

    }) => {

      const pre = await test.step('Precondition: token, topic, mailbox, customer', async () =>

        runPdt2881SharedPreconditions(

          Request,

          GeneratePayload,

          Endpoints,

          Responses,

          Nomenclatures,

          'tc-be-5',

        ));



      await test.step('POST /email-communication — invalid email segment in list', async () => {

        const payload = buildEmailCommunicationPayload(pre, {

          customerEmailAddress: 'valid@example.com;not-an-email',

          emailSubject: 'PDT-2881 TC-BE-5',

          emailCreateType: 'SEND',

        });

        const response = await Request.post(Endpoints.email, { data: payload });

        await assertEmailValidationError(response, ['invalid email format']);

      });



      await test.step('Prove no row persisted via list search', async () => {

        await assertNoEmailPersistedBySubject(Request, Endpoints, 'PDT-2881 TC-BE-5');

      });



      await test.step('SQL: no email_communications row for subject', async () => {

        annotateDbAssertSkipped();

        await assertEmailSubjectCountFromDb('PDT-2881 TC-BE-5', 0);

      });



      test.info().attach('[PDT-2881] TC-BE-5 response', {

        body: JSON.stringify(reportGenerator.setLinksToResponses(Responses), null, 2),

        contentType: 'application/json',

      });

    });



    test('[PDT-2881] TC-BE-6: Trailing semicolon empty segment', async ({

      Request,

      GeneratePayload,

      Endpoints,

      Responses,

      Nomenclatures,

    }) => {

      const pre = await test.step('Precondition: token, topic, mailbox, customer', async () =>

        runPdt2881SharedPreconditions(

          Request,

          GeneratePayload,

          Endpoints,

          Responses,

          Nomenclatures,

          'tc-be-6',

        ));



      await test.step('POST /email-communication — trailing semicolon empty segment', async () => {

        const payload = buildEmailCommunicationPayload(pre, {

          customerEmailAddress: 'good@example.com;',

          emailSubject: 'PDT-2881 TC-BE-6',

          emailCreateType: 'SEND',

        });

        const response = await Request.post(Endpoints.email, { data: payload });

        await assertEmailValidationError(response, ['invalid email format']);

      });



      await test.step('Prove no row persisted via list search', async () => {

        await assertNoEmailPersistedBySubject(Request, Endpoints, 'PDT-2881 TC-BE-6');

      });



      await test.step('SQL: no email_communications row for subject', async () => {

        annotateDbAssertSkipped();

        await assertEmailSubjectCountFromDb('PDT-2881 TC-BE-6', 0);

      });



      test.info().attach('[PDT-2881] TC-BE-6 response', {

        body: JSON.stringify(reportGenerator.setLinksToResponses(Responses), null, 2),

        contentType: 'application/json',

      });

    });



    test('[PDT-2881] TC-BE-7: Whitespace around semicolons trimmed', async ({

      Request,

      GeneratePayload,

      Endpoints,

      Responses,

      Nomenclatures,

    }) => {

      let emailCommunicationId: number;



      const pre = await test.step('Precondition: token, topic, mailbox, customer', async () =>

        runPdt2881SharedPreconditions(

          Request,

          GeneratePayload,

          Endpoints,

          Responses,

          Nomenclatures,

          'tc-be-7',

        ));



      await test.step('Precondition delta: whitespace around semicolons', async () => {

        const payload = buildEmailCommunicationPayload(pre, {

          customerEmailAddress: 'pdt2881.ws1@example.com ; pdt2881.ws2@example.com',

          emailSubject: 'PDT-2881 TC-BE-7',

          emailCreateType: 'SEND',

        });

        emailCommunicationId = await postEmailCommunication(

          Request,

          Endpoints,

          payload,

          Responses,

        );

      });



      await test.step('GET view — trimmed recipient addresses', async () => {

        const details = await getEmailCommunicationView(Request, Endpoints, emailCommunicationId);

        assertOutgoingEmailView(details);

        assertMultiRecipientEmailCommunication(details, emailCommunicationId, {

          expectedRecipientCount: 2,

          expectedEmails: ['pdt2881.ws1@example.com', 'pdt2881.ws2@example.com'],

        });

      });



      await test.step('SQL: trimmed email_address values — one task_id', async () => {

        annotateDbAssertSkipped();

        await assertEmailContactsFromDb(emailCommunicationId, {

          expectedRowCount: 2,

          expectedEmails: ['pdt2881.ws1@example.com', 'pdt2881.ws2@example.com'],

          distinctNonNullTaskIdCount: 1,

        });

      });



      test.info().attach('[PDT-2881] TC-BE-7 response', {

        body: JSON.stringify(reportGenerator.setLinksToResponses(Responses), null, 2),

        contentType: 'application/json',

      });

    });



    test('[PDT-2881] TC-BE-8: Resend — new communication, one task_id per send', async ({

      Request,

      GeneratePayload,

      Endpoints,

      Responses,

      Nomenclatures,

    }) => {

      let originalId: number;

      let resendId: number;



      const pre = await test.step('Precondition: token, topic, mailbox, customer', async () =>

        runPdt2881SharedPreconditions(

          Request,

          GeneratePayload,

          Endpoints,

          Responses,

          Nomenclatures,

          'tc-be-8',

        ));



      await test.step('Precondition delta: baseline two-recipient SEND', async () => {

        const payload = buildEmailCommunicationPayload(pre, {

          customerEmailAddress: 'pdt2881.res.a@example.com;pdt2881.res.b@example.com',

          emailSubject: 'PDT-2881 TC-BE-8-base',

          emailCreateType: 'SEND',

        });

        originalId = await postEmailCommunication(Request, Endpoints, payload, Responses);



        const baseline = await getEmailCommunicationView(Request, Endpoints, originalId);

        assertOutgoingEmailView(baseline);

        assertMultiRecipientEmailCommunication(baseline, originalId, {

          expectedRecipientCount: 2,

          expectedEmails: ['pdt2881.res.a@example.com', 'pdt2881.res.b@example.com'],

        });

      });



      await test.step('SQL: baseline — two contacts, one task_id', async () => {

        annotateDbAssertSkipped();

        await assertEmailContactsFromDb(originalId, {

          expectedRowCount: 2,

          expectedEmails: ['pdt2881.res.a@example.com', 'pdt2881.res.b@example.com'],

          distinctNonNullTaskIdCount: 1,

        });

      });



      await test.step('GET /email-communication/{originalId}/resend — new communication id', async () => {

        resendId = await resendEmailCommunication(Request, Endpoints, originalId);

        expect(resendId).toBeGreaterThan(0);

        expect(resendId).not.toBe(originalId);

      });



      await test.step('Poll resend status until no longer DRAFT', async () => {

        const finalStatus = await waitForEmailStatusLeavingDraft(

          Request,

          Endpoints,

          resendId,

        );

        expect(['IN_PROGRESS', 'SENT_SUCCESSFULLY', 'SENT_FAILED'].includes(finalStatus)).toBeTruthy();

      });



      await test.step('GET resend view — same recipient set on new communication', async () => {

        const details = await getEmailCommunicationView(Request, Endpoints, resendId);

        assertOutgoingEmailView(details);

        expect(details.customerEmailAddress).toContain('pdt2881.res.a@example.com');

        expect(details.customerEmailAddress).toContain('pdt2881.res.b@example.com');



        if (details.emailCommunicationStatus === 'SENT_SUCCESSFULLY') {

          expect(details.sentDate).toBeTruthy();

        }



        assertMultiRecipientEmailCommunication(details, resendId, {

          expectedRecipientCount: 2,

          expectedEmails: ['pdt2881.res.a@example.com', 'pdt2881.res.b@example.com'],

        });

      });



      await test.step('SQL: resend — two contacts, one task_id', async () => {

        annotateDbAssertSkipped();

        await assertEmailContactsFromDb(resendId, {

          expectedRowCount: 2,

          expectedEmails: ['pdt2881.res.a@example.com', 'pdt2881.res.b@example.com'],

          distinctNonNullTaskIdCount: 1,

        });

      });



      test.info().attach('[PDT-2881] TC-BE-8 response', {

        body: JSON.stringify(reportGenerator.setLinksToResponses(Responses), null, 2),

        contentType: 'application/json',

      });

    });



    test('[PDT-2881] TC-BE-9: Segment longer than 128 characters', async ({

      Request,

      GeneratePayload,

      Endpoints,

      Responses,

      Nomenclatures,

    }) => {

      const pre = await test.step('Precondition: token, topic, mailbox, customer', async () =>

        runPdt2881SharedPreconditions(

          Request,

          GeneratePayload,

          Endpoints,

          Responses,

          Nomenclatures,

          'tc-be-9',

        ));



      await test.step('POST /email-communication — segment longer than 128 characters', async () => {

        const payload = buildEmailCommunicationPayload(pre, {

          customerEmailAddress: `short@example.com;${buildLongEmailSegmentOver128()}`,

          emailSubject: 'PDT-2881 TC-BE-9',

          emailCreateType: 'SEND',

        });

        const response = await Request.post(Endpoints.email, { data: payload });

        await assertEmailValidationError(response, [

          'max symbols for each email address is 128',

        ]);

      });



      await test.step('Prove no row persisted via list search', async () => {

        await assertNoEmailPersistedBySubject(Request, Endpoints, 'PDT-2881 TC-BE-9');

      });



      await test.step('SQL: no email_communications row for subject', async () => {

        annotateDbAssertSkipped();

        await assertEmailSubjectCountFromDb('PDT-2881 TC-BE-9', 0);

      });



      test.info().attach('[PDT-2881] TC-BE-9 response', {

        body: JSON.stringify(reportGenerator.setLinksToResponses(Responses), null, 2),

        contentType: 'application/json',

      });

    });



    test('[PDT-2881] TC-BE-10: DRAFT — no task_id until send', async ({

      Request,

      GeneratePayload,

      Endpoints,

      Responses,

      Nomenclatures,

    }) => {

      let draftId: number;



      const pre = await test.step('Precondition: token, topic, mailbox, customer', async () =>

        runPdt2881SharedPreconditions(

          Request,

          GeneratePayload,

          Endpoints,

          Responses,

          Nomenclatures,

          'tc-be-10',

        ));



      await test.step('Precondition delta: DRAFT with two recipient addresses', async () => {

        const payload = buildEmailCommunicationPayload(pre, {

          customerEmailAddress: 'pdt2881.draft1@example.com;pdt2881.draft2@example.com',

          emailSubject: 'PDT-2881 TC-BE-10',

          emailCreateType: 'DRAFT',

        });

        draftId = await postEmailCommunication(Request, Endpoints, payload, Responses);

      });



      await test.step('GET view — DRAFT status, no sentDate, no outbound task', async () => {

        const details = await getEmailCommunicationView(Request, Endpoints, draftId);

        assertMultiRecipientEmailCommunication(details, draftId, {

          expectedRecipientCount: 2,

          expectedEmails: ['pdt2881.draft1@example.com', 'pdt2881.draft2@example.com'],

          allowDraft: true,

          requireAllTaskIdsNull: true,

        });

      });



      await test.step('SQL: all contact task_id values are null', async () => {

        annotateDbAssertSkipped();

        await assertEmailContactsFromDb(draftId, {

          expectedRowCount: 2,

          expectedEmails: ['pdt2881.draft1@example.com', 'pdt2881.draft2@example.com'],

          allTaskIdsNull: true,

        });

      });



      test.info().attach('[PDT-2881] TC-BE-10 response', {

        body: JSON.stringify(reportGenerator.setLinksToResponses(Responses), null, 2),

        contentType: 'application/json',

      });

    });

  },

);

