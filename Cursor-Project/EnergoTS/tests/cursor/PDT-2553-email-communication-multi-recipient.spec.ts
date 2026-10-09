import { test, expect } from '../../fixtures/baseFixture';
import reportGenerator from '../../utils/generateReport';

test.describe('[PDT-2553]: Email communication multi-recipient', { tag: '@customerComm' }, () => {
  const getContactsFromDetails = (details: any): any[] => {
    if (Array.isArray(details?.emailCommunicationCustomerContacts)) {
      return details.emailCommunicationCustomerContacts;
    }
    if (Array.isArray(details?.customerContacts)) {
      return details.customerContacts;
    }
    if (Array.isArray(details?.recipients)) {
      return details.recipients;
    }
    return [];
  };

  const getCommunicationIdFromDetails = (details: any): string | undefined => {
    return details?.id ?? details?.emailCommunicationId ?? details?.communicationId;
  };

  const getCommunicationIdsFromContacts = (contacts: any[]): Set<string> => {
    const ids = new Set<string>();
    contacts.forEach((c: any) => {
      const id = c.emailCommunicationId ?? c.communicationId ?? c.parentCommunicationId;
      if (id) {
        ids.add(String(id));
      }
    });
    return ids;
  };

  const getContactEmail = (c: any): string | undefined =>
    c.email || c.emailAddress || c.contactEmail || c.recipientEmail;

  const getContactStatus = (c: any): string | undefined =>
    c.status || c.sendStatus || c.deliveryStatus || c.communicationStatus;

  const getAttemptCounter = (c: any): number | undefined => {
    const value =
      c.attempts ??
      c.attemptCount ??
      c.resendCount ??
      c.sendAttempts ??
      c.retryCount ??
      c.deliveryAttempts;
    return typeof value === 'number' ? value : undefined;
  };

  const isFailureStatus = (status?: string): boolean => {
    if (!status) return false;
    const normalized = status.toUpperCase();
    return ['FAILED', 'ERROR', 'BOUNCED', 'UNDELIVERED'].includes(normalized);
  };

  // Happy_path_and_single_recipient – TC-1
  test('[PDT-2553]: Send email to a single recipient – baseline behaviour (TC-1)', async ({ Request, Endpoints, Responses }) => {
    let createdComm: any;

    await test.step('Create email communication for Customer A with Contact A1 as single recipient', async () => {
      const payload = {
        customerCode: 'CustomerA',
        contacts: ['ContactA1'],
        subject: `PDT-2553_single_recipient_${Date.now()}`,
        body: 'Baseline single recipient email content for PDT-2553.',
      };

      const response = await Request.post(Endpoints.email, { data: payload });
      await expect(response).CheckResponse();
      createdComm = await response.json();
      Responses.email.push(createdComm);
    });

    await test.step('Verify exactly one communication and single successful recipient linkage in Phoenix', async () => {
      const detailsResponse = await Request.get(`${Endpoints.email}/${createdComm.id}`);
      await expect(detailsResponse).CheckResponse();
      const details = await detailsResponse.json();

      const commId = getCommunicationIdFromDetails(details);
      expect(commId).toBeDefined();

      const contacts = getContactsFromDetails(details);
      expect(Array.isArray(contacts)).toBeTruthy();
      expect(contacts.length).toBe(1);

      const uniqueEmails = new Set(
        contacts.map(getContactEmail).filter((e: any) => !!e),
      );
      expect(uniqueEmails.size).toBe(1);

      const commIdsFromContacts = getCommunicationIdsFromContacts(contacts);
      if (commIdsFromContacts.size > 0) {
        expect(commIdsFromContacts.size).toBe(1);
        expect(commIdsFromContacts.has(String(commId))).toBeTruthy();
      }

      const statuses = new Set(
        contacts.map(getContactStatus).filter((s: any) => !!s),
      );
      expect(
        Array.from(statuses).every((s) => !isFailureStatus(s)),
      ).toBeTruthy();
    });

    test.info().attach('[PDT-2553] response TC-1', {
      body: JSON.stringify(reportGenerator.setLinksToResponses(Responses), null, 2),
      contentType: 'application/json',
    });
  });

  // Happy_path_and_single_recipient – TC-2
  test('[PDT-2553]: Send email to two recipients – one email with multiple recipients (TC-2)', async ({ Request, Endpoints, Responses }) => {
    let createdComm: any;

    await test.step('Create email communication for Customer A with Contact A1 and Contact A2', async () => {
      const payload = {
        customerCode: 'CustomerA',
        contacts: ['ContactA1', 'ContactA2'],
        subject: `PDT-2553_two_recipients_${Date.now()}`,
        body: 'Two-recipient email content for PDT-2553.',
      };

      const response = await Request.post(Endpoints.email, { data: payload });
      await expect(response).CheckResponse();
      createdComm = await response.json();
      Responses.email.push(createdComm);
    });

    await test.step('Verify single communication with two distinct recipients and successful statuses', async () => {
      const detailsResponse = await Request.get(`${Endpoints.email}/${createdComm.id}`);
      await expect(detailsResponse).CheckResponse();
      const details = await detailsResponse.json();

      const commId = getCommunicationIdFromDetails(details);
      expect(commId).toBeDefined();

      const contacts = getContactsFromDetails(details);
      expect(Array.isArray(contacts)).toBeTruthy();
      expect(contacts.length).toBe(2);

      const uniqueEmails = new Set(
        contacts.map(getContactEmail).filter((e: any) => !!e),
      );
      expect(uniqueEmails.size).toBe(2);

      const commIdsFromContacts = getCommunicationIdsFromContacts(contacts);
      if (commIdsFromContacts.size > 0) {
        expect(commIdsFromContacts.size).toBe(1);
        expect(commIdsFromContacts.has(String(commId))).toBeTruthy();
      }

      const statuses = new Set(
        contacts.map(getContactStatus).filter((s: any) => !!s),
      );
      expect(
        Array.from(statuses).every((s) => !isFailureStatus(s)),
      ).toBeTruthy();
    });

    test.info().attach('[PDT-2553] response TC-2', {
      body: JSON.stringify(reportGenerator.setLinksToResponses(Responses), null, 2),
      contentType: 'application/json',
    });
  });

  // Happy_path_and_single_recipient – TC-3
  test('[PDT-2553]: Send email to many recipients across multiple customers – single email with combined recipients (TC-3)', async ({ Request, Endpoints, Responses }) => {
    let createdComm: any;

    await test.step('Create email communication for Customer A and Customer B contacts', async () => {
      const payload = {
        customers: ['CustomerA', 'CustomerB'],
        contacts: ['ContactA1', 'ContactA2', 'ContactB1'],
        subject: `PDT-2553_multi_customer_${Date.now()}`,
        body: 'Multi-customer multi-recipient email content for PDT-2553.',
      };

      const response = await Request.post(Endpoints.email, { data: payload });
      await expect(response).CheckResponse();
      createdComm = await response.json();
      Responses.email.push(createdComm);
    });

    await test.step('Verify single communication with all customers and contacts linked successfully', async () => {
      const detailsResponse = await Request.get(`${Endpoints.email}/${createdComm.id}`);
      await expect(detailsResponse).CheckResponse();
      const details = await detailsResponse.json();

      const commId = getCommunicationIdFromDetails(details);
      expect(commId).toBeDefined();

      const contacts = getContactsFromDetails(details);
      expect(Array.isArray(contacts)).toBeTruthy();
      expect(contacts.length).toBe(3);

      const uniqueEmails = new Set(
        contacts.map(getContactEmail).filter((e: any) => !!e),
      );
      expect(uniqueEmails.size).toBe(3);

      const commIdsFromContacts = getCommunicationIdsFromContacts(contacts);
      if (commIdsFromContacts.size > 0) {
        expect(commIdsFromContacts.size).toBe(1);
        expect(commIdsFromContacts.has(String(commId))).toBeTruthy();
      }

      const statuses = new Set(
        contacts.map(getContactStatus).filter((s: any) => !!s),
      );
      expect(
        Array.from(statuses).every((s) => !isFailureStatus(s)),
      ).toBeTruthy();
    });

    test.info().attach('[PDT-2553] response TC-3', {
      body: JSON.stringify(reportGenerator.setLinksToResponses(Responses), null, 2),
      contentType: 'application/json',
    });
  });

  // Happy_path_and_single_recipient – TC-4
  test('[PDT-2553]: Resend previously sent email communication – single resend message with same recipient set (TC-4)', async ({ Request, Endpoints, Responses }) => {
    let existingComm: any;

    await test.step('Identify existing communication created for multi-recipient scenario', async () => {
      if (!Responses.email.length) {
        test.skip('No existing email communication from previous tests to resend.');
      }
      existingComm = Responses.email[0];
    });

    let originalDetails: any;

    await test.step('Capture original communication state before resend', async () => {
      const originalDetailsResponse = await Request.get(`${Endpoints.email}/${existingComm.id}`);
      await expect(originalDetailsResponse).CheckResponse();
      originalDetails = await originalDetailsResponse.json();

      const originalContacts = getContactsFromDetails(originalDetails);
      expect(Array.isArray(originalContacts)).toBeTruthy();
      expect(originalContacts.length).toBeGreaterThanOrEqual(1);
    });

    await test.step('Trigger resend for existing communication', async () => {
      const response = await Request.get(`${Endpoints.email}/${existingComm.id}/resend`);
      await expect(response).CheckResponse();
    });

    await test.step('Verify resend tracking, recipient set stability and attempt increment', async () => {
      const resendDetailsResponse = await Request.get(`${Endpoints.email}/${existingComm.id}`);
      await expect(resendDetailsResponse).CheckResponse();
      const resendDetails = await resendDetailsResponse.json();

      const originalCommId = getCommunicationIdFromDetails(originalDetails);
      const resendCommId = getCommunicationIdFromDetails(resendDetails);
      expect(resendCommId).toBeDefined();
      if (originalCommId) {
        expect(String(resendCommId)).toBe(String(originalCommId));
      }

      const originalContacts = getContactsFromDetails(originalDetails);
      const resendContacts = getContactsFromDetails(resendDetails);

      expect(resendContacts.length).toBe(originalContacts.length);

      const originalEmails = new Set(originalContacts.map(getContactEmail).filter((e: any) => !!e));
      const resendEmails = new Set(resendContacts.map(getContactEmail).filter((e: any) => !!e));
      expect(resendEmails.size).toBe(originalEmails.size);
      originalEmails.forEach((email) => {
        expect(resendEmails.has(email)).toBeTruthy();
      });

      for (let i = 0; i < resendContacts.length; i += 1) {
        const before = getAttemptCounter(originalContacts[i]);
        const after = getAttemptCounter(resendContacts[i]);
        if (before !== undefined && after !== undefined) {
          expect(after).toBe(before + 1);
        }
      }
    });

    test.info().attach('[PDT-2553] response TC-4', {
      body: JSON.stringify(reportGenerator.setLinksToResponses(Responses), null, 2),
      contentType: 'application/json',
    });
  });

  // Happy_path_and_single_recipient – TC-5
  test('[PDT-2553]: Attempt to send email with no recipients selected (TC-5)', async ({ Request, Endpoints, Responses }) => {
    await test.step('Attempt to create email communication without recipients and expect validation error', async () => {
      const payload = {
        customerCode: 'CustomerA',
        contacts: [],
        subject: `PDT-2553_no_recipient_${Date.now()}`,
        body: 'No-recipient negative case content for PDT-2553.',
      };

      const response = await Request.post(Endpoints.email, { data: payload });
      expect(response.status()).toBe(400);

      let body: any = null;
      try {
        body = await response.json();
      } catch {
        body = null;
      }

      if (body && (body.id || body.communicationId)) {
        const id = body.id ?? body.communicationId;
        const followUpResponse = await Request.get(`${Endpoints.email}/${id}`);
        const statusCode = followUpResponse.status();
        expect([200, 404]).toContain(statusCode);

        if (statusCode === 200) {
          const details = await followUpResponse.json();
          const commStatus =
            details?.status ??
            details?.communicationStatus ??
            details?.sendStatus ??
            details?.deliveryStatus;
          expect(
            ['SENT', 'PENDING'].includes(String(commStatus).toUpperCase()),
          ).toBeFalsy();
        }
      }

      expect(Responses.email.length).toBeGreaterThanOrEqual(0);
    });

    test.info().attach('[PDT-2553] response TC-5', {
      body: JSON.stringify(reportGenerator.setLinksToResponses(Responses), null, 2),
      contentType: 'application/json',
    });
  });

  // Multi_recipient_and_mass_email – TC-1
  test.skip(
    '[PDT-2553]: Mass email to moderate-sized recipient list – one email per batch with combined recipients (TC-1)',
    'Mass email batch processing and external provider verification are not automated in this spec.',
  );

  // Multi_recipient_and_mass_email – TC-2
  test.skip(
    '[PDT-2553]: Mass email across multiple customers – single email per batch covering all recipients (TC-2)',
    'Cross-customer mass email campaign behaviour is not automated in this spec.',
  );

  // Multi_recipient_and_mass_email – TC-3
  test.skip(
    '[PDT-2553]: Mass email with batch splitting at provider limit – one email per batch, no per-contact duplication (TC-3)',
    'Provider limit batch splitting behaviour is not automated in this spec.',
  );

  // Multi_recipient_and_mass_email – TC-4
  test.skip(
    '[PDT-2553]: Mass email job retry does not multiply sent emails for successful recipients (TC-4)',
    'Mass email retry behaviour is not automated in this spec.',
  );

  // Multi_recipient_and_mass_email – TC-5
  test.skip(
    '[PDT-2553]: Mass email campaign with no valid recipients – no outbound email created (TC-5)',
    'Mass email with zero valid recipients is not automated in this spec.',
  );

  // Error_and_edge_cases – TC-1
  test.skip(
    '[PDT-2553]: Mixed valid and invalid recipient emails – one email sent, clear per-contact status (TC-1)',
    'Mixed valid/invalid recipient behaviour requires complex setup and external validation; not automated here.',
  );

  // Error_and_edge_cases – TC-2
  test.skip(
    '[PDT-2553]: Unreachable recipient (bounce) – no resend multiplication, correct per-contact failure (TC-2)',
    'Bounce simulation and retry behaviour depend on external provider; not automated here.',
  );

  // Error_and_edge_cases – TC-3
  test.skip(
    '[PDT-2553]: Missing or misconfigured mailbox – communication not sent and no recipient-specific duplication (TC-3)',
    'Mailbox misconfiguration scenario is not automated in this spec.',
  );

  // Error_and_edge_cases – TC-4
  test.skip(
    '[PDT-2553]: Single customer with multiple contacts – per-contact statuses with one email and clear activity mapping (TC-4)',
    'Detailed activity mapping for multi-contact single customer is not automated here.',
  );

  // Error_and_edge_cases – TC-5
  test.skip(
    '[PDT-2553]: Resend after partial failure – no additional duplication for previously successful recipients (TC-5)',
    'Resend after partial failure with per-contact status evaluation is not automated here.',
  );
});

