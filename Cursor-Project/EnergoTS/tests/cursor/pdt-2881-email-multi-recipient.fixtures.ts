/**

 * PDT-2881 — Single email to multiple recipients (Email Communication API).

 *

 * Reference spec(s):

 * - tests/cursor/PDT-2553-email-communication-multi-recipient.spec.ts (multi-recipient contact linkage)

 * - tests/cursor/pdt-2815-version-validity.fixtures.ts (customer create + nomenclatures)

 */



import { execSync } from 'child_process';

import path from 'path';

import { expect, test } from '../../fixtures/baseFixture';

import type { baseFixture } from '../../fixtures/baseFixture';

import type { nomenclatures } from '../../jsons/payloads/create/nomenclatures/nomenclatures';



type FixtureRequest = baseFixture['Request'];



export type EmailCommunicationView = {

  id?: number;

  emailCommunicationId?: number;

  communicationChannelType?: string;

  communicationType?: string;

  emailCommunicationType?: string;

  emailCommunicationStatus?: string;

  communicationStatus?: string;

  sentDate?: string | null;

  emailSubject?: string;

  emailBody?: string;

  customerEmailAddress?: string;

  emailCommunicationCustomerContacts?: unknown[];

  customerContacts?: unknown[];

  recipients?: unknown[];

  taskShortResponse?: unknown[];

};



export type Pdt2881NomenclatureContext = {

  communicationTopicId: number;

  emailBoxId: number;

};



export type Pdt2881CustomerContext = {

  customerId: number;

  customerDetailId: number;

  customerCommunicationId: number;

  setupEmail: string;

};



export type Pdt2881PreconditionContext = Pdt2881CustomerContext & Pdt2881NomenclatureContext;



export type EmailCommunicationSendPayload = {

  communicationAsAnInstitution: boolean;

  communicationTopicId: number;

  emailCommunicationType: 'OUTGOING';

  emailCreateType: 'SEND' | 'DRAFT';

  emailBoxId: number;

  customerEmailAddress: string;

  emailSubject: string;

  emailBody: string;

  customerDetailId: number;

  customerCommunicationId: number;

};



export type EmailContactSqlRow = {

  id: number;

  emailAddress: string;

  status: string | null;

  taskId: string | null;

};



const FAILURE_STATUSES = new Set(['FAILED', 'ERROR', 'BOUNCED', 'UNDELIVERED', 'SENT_FAILED']);



const PG_READONLY_SCRIPT = path.resolve(

  __dirname,

  '../../../config/database/pdt2881-postgres-readonly.ps1',

);



function stampEmail(tag: string): string {

  return `pdt2881-${tag}-${Date.now()}@automation.local`;

}



function withEmailOnCreateTemplate(

  template: Record<string, unknown>,

  emailAddress: string,

): Record<string, unknown> {

  const payload = JSON.parse(JSON.stringify(template)) as Record<string, unknown>;

  const commData = payload.communicationData as Record<string, unknown>[];

  for (const comm of commData ?? []) {

    const contacts = comm.communicationContacts as { contactType?: string; contactValue?: string }[];

    for (const contact of contacts ?? []) {

      if (contact.contactType === 'EMAIL') {

        contact.contactValue = emailAddress;

      }

    }

  }

  return payload;

}



export function isPdt2881DbAssertEnabled(): boolean {

  return (

    process.env.PDT2881_PG_ASSERT === 'true' &&

    !!process.env.PDT2881_PG_HOST &&

    !!process.env.PDT2881_PG_USER &&

    !!process.env.PDT2881_PG_PASSWORD &&

    !!process.env.PDT2881_PG_DATABASE

  );

}



function runPostgresReadonlyScript(args: string): string {

  const command = `powershell -ExecutionPolicy Bypass -File "${PG_READONLY_SCRIPT}" ${args}`;

  return execSync(command, { encoding: 'utf8', stdio: ['pipe', 'pipe', 'pipe'] }).trim();

}



export function queryEmailContactsFromDb(

  emailCommunicationId: number,

): EmailContactSqlRow[] {

  const raw = runPostgresReadonlyScript(

    `-Mode contacts -EmailCommunicationId ${emailCommunicationId}`,

  );

  const parsed = JSON.parse(raw) as EmailContactSqlRow | EmailContactSqlRow[];

  return Array.isArray(parsed) ? parsed : [parsed];

}



export function queryEmailSubjectCountFromDb(emailSubject: string): number {

  const escaped = emailSubject.replace(/"/g, '\\"');

  const raw = runPostgresReadonlyScript(

    `-Mode countBySubject -EmailSubject "${escaped}"`,

  );

  const parsed = JSON.parse(raw) as { count: number };

  return Number(parsed.count);

}



export function parseRecipientEmails(raw: string): string[] {

  return raw

    .split(';')

    .map((segment) => segment.trim())

    .filter((segment) => segment.length > 0);

}



export function buildLongEmailSegmentOver128(): string {

  return `${'a'.repeat(125)}@example.com`;

}



const getContactsFromDetails = (details: EmailCommunicationView): Record<string, unknown>[] => {

  if (Array.isArray(details.emailCommunicationCustomerContacts)) {

    return details.emailCommunicationCustomerContacts as Record<string, unknown>[];

  }

  if (Array.isArray(details.customerContacts)) {

    return details.customerContacts as Record<string, unknown>[];

  }

  if (Array.isArray(details.recipients)) {

    return details.recipients as Record<string, unknown>[];

  }

  return [];

};



const getContactEmail = (contact: Record<string, unknown>): string | undefined =>

  (contact.email ??

    contact.emailAddress ??

    contact.contactEmail ??

    contact.recipientEmail) as string | undefined;



const getContactStatus = (contact: Record<string, unknown>): string | undefined =>

  (contact.status ??

    contact.sendStatus ??

    contact.deliveryStatus ??

    contact.communicationStatus) as string | undefined;



const getCommunicationIdsFromContacts = (contacts: Record<string, unknown>[]): Set<string> => {

  const ids = new Set<string>();

  contacts.forEach((contact) => {

    const id =

      contact.emailCommunicationId ?? contact.communicationId ?? contact.parentCommunicationId;

    if (id != null) {

      ids.add(String(id));

    }

  });

  return ids;

};



const isFailureStatus = (status?: string): boolean => {

  if (!status) return false;

  return FAILURE_STATUSES.has(String(status).toUpperCase());

};



export function assertOutgoingEmailView(details: EmailCommunicationView): void {

  expect(details.communicationChannelType ?? 'EMAIL').toBe('EMAIL');

  expect(details.communicationType ?? details.emailCommunicationType).toBe('OUTGOING');

}



export async function verifyAuthenticatedSession(Request: FixtureRequest): Promise<void> {

  const response = await Request.get('topic-of-communication', {

    params: { statuses: 'ACTIVE', page: 0, size: 1 },

  });

  await expect(response).CheckResponse();

}



export async function createPdt2881Nomenclatures(

  Nomenclatures: nomenclatures,

  tag: string,

): Promise<Pdt2881NomenclatureContext> {

  const prompt = `PDT-2881 ${tag}`;

  const communicationTopicId = await Nomenclatures.topic_of_communication(prompt);

  const emailBoxId = await Nomenclatures.email_mailboxes(prompt);

  expect(communicationTopicId).toBeGreaterThan(0);

  expect(emailBoxId).toBeGreaterThan(0);

  return { communicationTopicId, emailBoxId };

}



export async function createPdt2881Customer(

  Request: FixtureRequest,

  GeneratePayload: baseFixture['GeneratePayload'],

  Endpoints: baseFixture['Endpoints'],

  Responses: baseFixture['Responses'],

  emailTag = 'setup',

): Promise<Pdt2881CustomerContext> {

  const setupEmail = stampEmail(emailTag);

  const payload = withEmailOnCreateTemplate(

    GeneratePayload.customers.customer_private() as Record<string, unknown>,

    setupEmail,

  );



  const response = await Request.post(Endpoints.customer, { data: payload });

  await expect(response).CheckResponse();

  const body = (await response.json()) as Record<string, unknown>;

  Responses.customer.push(body);



  const customerId = Number(body.id ?? body.customerId);

  const customerDetailId = Number(

    body.lastCustomerDetailId ??

      (body.customerDetail as { id?: number } | undefined)?.id ??

      body.customerDetailId,

  );



  const communicationData = (body.communicationData ?? []) as { id?: number }[];

  let customerCommunicationId = Number(communicationData[0]?.id);



  if (!customerCommunicationId) {

    const customerGet = await Request.get(`${Endpoints.customer}/${customerId}`);

    await expect(customerGet).CheckResponse();

    const customerBody = (await customerGet.json()) as {

      communicationData?: { id?: number }[];

    };

    customerCommunicationId = Number(customerBody.communicationData?.[0]?.id);

  }



  expect(customerId).toBeGreaterThan(0);

  expect(customerDetailId).toBeGreaterThan(0);

  expect(customerCommunicationId).toBeGreaterThan(0);



  return {

    customerId,

    customerDetailId,

    customerCommunicationId,

    setupEmail,

  };

}



export async function verifyCustomerCommunicationActive(

  Request: FixtureRequest,

  customerCommunicationId: number,

): Promise<void> {

  const response = await Request.get(`customer-communications/${customerCommunicationId}`);

  await expect(response).CheckResponse();

  const body = (await response.json()) as {

    status?: string;

    communicationContacts?: { contactType?: string }[];

    contacts?: { contactType?: string }[];

  };

  expect(String(body.status ?? 'ACTIVE').toUpperCase()).toBe('ACTIVE');



  const contacts = body.communicationContacts ?? body.contacts ?? [];

  expect(

    contacts.some((contact) => String(contact.contactType).toUpperCase() === 'EMAIL'),

  ).toBeTruthy();

}



export function buildEmailCommunicationPayload(

  pre: Pdt2881PreconditionContext,

  options: {

    customerEmailAddress: string;

    emailSubject: string;

    emailCreateType?: 'SEND' | 'DRAFT';

    emailBody?: string;

  },

): EmailCommunicationSendPayload {

  return {

    communicationAsAnInstitution: false,

    communicationTopicId: pre.communicationTopicId,

    emailCommunicationType: 'OUTGOING',

    emailCreateType: options.emailCreateType ?? 'SEND',

    emailBoxId: pre.emailBoxId,

    customerEmailAddress: options.customerEmailAddress,

    emailSubject: options.emailSubject,

    emailBody: options.emailBody ?? '<p>PDT-2881</p>',

    customerDetailId: pre.customerDetailId,

    customerCommunicationId: pre.customerCommunicationId,

  };

}



export async function parseEmailCommunicationId(response: {

  json: () => Promise<unknown>;

}): Promise<number> {

  const body = await response.json();

  const id = typeof body === 'number' ? body : Number(body);

  expect(Number.isFinite(id)).toBeTruthy();

  expect(id).toBeGreaterThan(0);

  return id;

}



export async function postEmailCommunication(

  Request: FixtureRequest,

  Endpoints: baseFixture['Endpoints'],

  payload: EmailCommunicationSendPayload,

  Responses: baseFixture['Responses'],

): Promise<number> {

  const response = await Request.post(Endpoints.email, { data: payload });

  expect(response.status()).toBe(201);

  await expect(response).CheckResponse();

  const emailCommunicationId = await parseEmailCommunicationId(response);

  Responses.email.push({ id: emailCommunicationId, subject: payload.emailSubject });

  return emailCommunicationId;

}



export async function getEmailCommunicationView(

  Request: FixtureRequest,

  Endpoints: baseFixture['Endpoints'],

  emailCommunicationId: number,

): Promise<EmailCommunicationView> {

  const response = await Request.get(`${Endpoints.email}/${emailCommunicationId}`, {

    params: { type: 'EMAIL' },

  });

  await expect(response).CheckResponse();

  return (await response.json()) as EmailCommunicationView;

}



export function assertEmailContactsSql(

  rows: EmailContactSqlRow[],

  options: {

    expectedRowCount: number;

    expectedEmails?: string[];

    distinctNonNullTaskIdCount?: number;

    allTaskIdsNull?: boolean;

  },

): void {

  expect(rows.length).toBe(options.expectedRowCount);



  const emails = rows.map((row) => row.emailAddress.toLowerCase());

  if (options.expectedEmails) {

    options.expectedEmails.forEach((expected) => {

      expect(emails).toContain(expected.toLowerCase());

    });

  }



  const distinctEmails = new Set(emails);

  expect(distinctEmails.size).toBe(options.expectedRowCount);



  rows.forEach((row) => {

    if (row.status) {

      expect(isFailureStatus(row.status)).toBeFalsy();

    }

  });



  const nonNullTaskIds = rows

    .map((row) => row.taskId)

    .filter((taskId): taskId is string => taskId != null && taskId !== '');



  if (options.allTaskIdsNull) {

    expect(nonNullTaskIds.length).toBe(0);

    return;

  }



  if (options.distinctNonNullTaskIdCount != null) {

    const distinctTaskIds = new Set(nonNullTaskIds);

    expect(distinctTaskIds.size).toBe(options.distinctNonNullTaskIdCount);

  }

}



export async function assertEmailContactsFromDb(

  emailCommunicationId: number,

  options: Parameters<typeof assertEmailContactsSql>[1],

): Promise<void> {

  if (!isPdt2881DbAssertEnabled()) {

    return;

  }

  const rows = queryEmailContactsFromDb(emailCommunicationId);

  assertEmailContactsSql(rows, options);

}



export async function assertEmailSubjectCountFromDb(

  emailSubject: string,

  expectedCount: number,

): Promise<void> {

  if (!isPdt2881DbAssertEnabled()) {

    return;

  }

  const count = queryEmailSubjectCountFromDb(emailSubject);

  expect(count).toBe(expectedCount);

}



export function assertMultiRecipientEmailCommunication(

  details: EmailCommunicationView,

  emailCommunicationId: number,

  options: {

    expectedRecipientCount: number;

    expectedEmails?: string[];

    allowDraft?: boolean;

    requireSentDateWhenSuccessful?: boolean;

    requireAllTaskIdsNull?: boolean;

  },

): void {

  const commId = Number(details.id ?? details.emailCommunicationId ?? emailCommunicationId);

  expect(commId).toBe(emailCommunicationId);



  const contacts = getContactsFromDetails(details);

  let recipientEmails: string[];



  if (contacts.length > 0) {

    expect(contacts.length).toBe(options.expectedRecipientCount);

    recipientEmails = contacts

      .map(getContactEmail)

      .filter((email): email is string => !!email);

    expect(recipientEmails.length).toBe(options.expectedRecipientCount);



    const commIdsFromContacts = getCommunicationIdsFromContacts(contacts);

    if (commIdsFromContacts.size > 0) {

      expect(commIdsFromContacts.size).toBe(1);

      expect(commIdsFromContacts.has(String(emailCommunicationId))).toBeTruthy();

    }



    contacts.forEach((contact) => {

      const status = getContactStatus(contact);

      if (status) {

        expect(isFailureStatus(status)).toBeFalsy();

      }

    });

  } else {

    recipientEmails = parseRecipientEmails(details.customerEmailAddress ?? '');

    expect(recipientEmails.length).toBe(options.expectedRecipientCount);

  }



  const distinctEmails = new Set(recipientEmails.map((email) => email.toLowerCase()));

  expect(distinctEmails.size).toBe(options.expectedRecipientCount);



  if (options.expectedEmails) {

    options.expectedEmails.forEach((expectedEmail) => {

      expect(distinctEmails.has(expectedEmail.toLowerCase())).toBeTruthy();

    });

  }



  const status = String(

    details.emailCommunicationStatus ?? details.communicationStatus ?? '',

  ).toUpperCase();



  if (options.allowDraft) {

    expect(status).toBe('DRAFT');

    expect(details.sentDate ?? null).toBeNull();

    if (options.requireAllTaskIdsNull) {

      expect(details.taskShortResponse ?? []).toHaveLength(0);

    }

    return;

  }



  expect(status).not.toBe('DRAFT');

  expect(['IN_PROGRESS', 'SENT_SUCCESSFULLY', 'SENT'].includes(status)).toBeTruthy();



  if (

    options.requireSentDateWhenSuccessful &&

    status === 'SENT_SUCCESSFULLY'

  ) {

    expect(details.sentDate).toBeTruthy();

  }

}



export async function assertEmailValidationError(

  response: { status: () => number; json: () => Promise<unknown> },

  messageFragments: string[],

): Promise<void> {

  expect(response.status()).toBe(400);

  const body = (await response.json()) as { errorCode?: string; message?: string };

  expect(typeof body).not.toBe('number');

  expect(body.errorCode).toBe('ILLEGAL_ARGUMENTS_PROVIDED');

  const message = String(body.message ?? '').toLowerCase();

  expect(message).toContain('customeremailaddress');

  messageFragments.forEach((fragment) => {

    expect(message).toContain(fragment.toLowerCase());

  });

}



export async function assertNoEmailPersistedBySubject(

  Request: FixtureRequest,

  Endpoints: baseFixture['Endpoints'],

  emailSubject: string,

): Promise<void> {

  const listResponse = await Request.get(`${Endpoints.email}/list`, {

    params: {

      page: 0,

      size: 20,

      prompt: emailSubject,

      searchBy: 'EMAIL_SUBJECT',

    },

  });

  await expect(listResponse).CheckResponse();

  const listBody = (await listResponse.json()) as {

    totalElements?: number;

    content?: unknown[];

  };

  expect(listBody.totalElements ?? listBody.content?.length ?? 0).toBe(0);

}



export async function resendEmailCommunication(

  Request: FixtureRequest,

  Endpoints: baseFixture['Endpoints'],

  originalId: number,

): Promise<number> {

  const response = await Request.get(`${Endpoints.email}/${originalId}/resend`);

  expect(response.status()).toBe(200);

  await expect(response).CheckResponse();

  return parseEmailCommunicationId(response);

}



export async function waitForEmailStatusLeavingDraft(

  Request: FixtureRequest,

  Endpoints: baseFixture['Endpoints'],

  emailCommunicationId: number,

  maxAttempts = 24,

): Promise<string> {

  let status = 'DRAFT';



  for (let attempt = 1; attempt <= maxAttempts; attempt += 1) {

    const details = await getEmailCommunicationView(Request, Endpoints, emailCommunicationId);

    status = String(

      details.emailCommunicationStatus ?? details.communicationStatus ?? '',

    ).toUpperCase();



    if (status !== 'DRAFT') {

      return status;

    }



    if (attempt < maxAttempts) {

      await new Promise((resolve) => setTimeout(resolve, 5000));

    }

  }



  throw new Error(

    `Email communication ${emailCommunicationId} remained DRAFT after ${maxAttempts} polling attempts`,

  );

}



export async function runPdt2881SharedPreconditions(

  Request: FixtureRequest,

  GeneratePayload: baseFixture['GeneratePayload'],

  Endpoints: baseFixture['Endpoints'],

  Responses: baseFixture['Responses'],

  Nomenclatures: nomenclatures,

  emailTag: string,

): Promise<Pdt2881PreconditionContext> {

  await verifyAuthenticatedSession(Request);

  const nomenclaturesCtx = await createPdt2881Nomenclatures(Nomenclatures, emailTag);

  const customer = await createPdt2881Customer(

    Request,

    GeneratePayload,

    Endpoints,

    Responses,

    emailTag,

  );

  await verifyCustomerCommunicationActive(Request, customer.customerCommunicationId);

  return { ...customer, ...nomenclaturesCtx };

}



export function annotateDbAssertSkipped(): void {

  if (!isPdt2881DbAssertEnabled()) {

    test.info().annotations.push({

      type: 'note',

      description:

        'SQL assertions skipped — set PDT2881_PG_ASSERT=true and PDT2881_PG_HOST/USER/PASSWORD/DATABASE (requires psql in PATH).',

    });

  }

}


