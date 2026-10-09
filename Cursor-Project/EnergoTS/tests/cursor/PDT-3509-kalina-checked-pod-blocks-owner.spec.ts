/**
 * PDT-3509 Kalina, 23 Sep 2026 on Dev.
 * POD 32XRGEPFXSRBZ4068038365 is already checked on Request-1422 for customer
 * 6045351. A new highest-consumption execute for the contract owner 6045350
 * must return HTTP 400 naming that POD and Request-1422.
 *
 * Does not create a POD, does not edit Request-1422, does not rewrite customerId.
 * The other PDT-3509 spec is left unchanged.
 *
 * Run: $env:BASE_URL='http://10.236.20.11:8091'
 */

import { test, expect } from './cursor-test.fixtures';
import { finalizeTestRunSummary } from './shared/manual-verification-links.fixtures';
import { DEV_PORTAL_BASE } from './dev-volume-billing-two-compensations.fixtures';
import {
  PDT_3509_KEY,
  PDT_3509_TITLE,
  buildRfdPayload,
  createReminderIncludedForCustomer,
  getRfdView,
  postDraftRfd,
  putRfd,
  waitForCreatedReminderExecuted,
  type Pdt3509Fx,
  type Pdt3509HttpResult,
} from './pdt-3509-rfd-pod-blocked-by-other-customer.fixtures';

const BLOCKING_REQUEST_ID = 1422;
const BLOCKING_REQUEST_NUMBER = 'Request-1422';
const POD_ID = 297042;
const POD_IDENTIFIER = '32XRGEPFXSRBZ4068038365';
const ALT_CUSTOMER_ID = 6045351;
const OWNER_IDENTIFIER = '1771233149145';
const GRID_OPERATOR_ID = 1009;
const DEV_REMINDER_EMAIL_TEMPLATE_ID = 1020;
const DEV_REMINDER_SMS_TEMPLATE_ID = 1086;

function describesOperationNotAllowed(result: Pdt3509HttpResult): boolean {
  return (
    result.exceptionId.includes('OperationNotAllowedException') ||
    /OperationNotAllowedException/.test(result.haystack)
  );
}

test(`[${PDT_3509_KEY}]: ${PDT_3509_TITLE} | Kalina owner execute blocked by Request-1422`, async ({
  Request, GeneratePayload, Responses, Endpoints, TestRunSummary,
}) => {
  test.setTimeout(20 * 60 * 1000);
  const fx: Pdt3509Fx = { Request, GeneratePayload, Responses, Endpoints };
  let reminderId = 0;
  let requestId = 0;
  let executeResult: Pdt3509HttpResult | null = null;

  const blocking = await test.step('Read: Request-1422 is FEE_CHARGED and the POD is checked for the alt recipient', async () => {
    const view = await getRfdView(fx, BLOCKING_REQUEST_ID);
    const status = String(view.disconnectionRequestsStatus ?? view.status ?? '');
    const requestNumber = String(view.requestNumber ?? '');
    expect(status, 'Request-1422 status').toBe('FEE_CHARGED');
    expect(requestNumber, 'Request-1422 number').toBe(BLOCKING_REQUEST_NUMBER);
    return view;
  });

  reminderId = await test.step('Precondition: reminder INCLUDED for the POD owner only', async () => {
    const id = await createReminderIncludedForCustomer(fx, OWNER_IDENTIFIER, {
      emailTemplateId: DEV_REMINDER_EMAIL_TEMPLATE_ID,
      smsTemplateId: DEV_REMINDER_SMS_TEMPLATE_ID,
    });
    const status = await waitForCreatedReminderExecuted(fx, id);
    expect(status, 'owner reminder status').toBe('EXECUTED');
    return id;
  });

  const draftPayload = buildRfdPayload(fx, {
    reminderId,
    listOfCustomer: OWNER_IDENTIFIER,
    disconnectionRequestsStatus: 'DRAFT',
    pods: [],
    podWithHighestConsumption: true,
  });
  draftPayload.gridOperatorId = GRID_OPERATOR_ID;

  requestId = await test.step('Precondition: draft request, highest consumption, no explicit pods', async () =>
    postDraftRfd(fx, draftPayload),
  );

  executeResult = await test.step('Action: Save And Execute', async () =>
    putRfd(fx, requestId, {
      ...draftPayload,
      disconnectionRequestsStatus: 'EXECUTED',
    }),
  );

  await test.step('Summary: expected 400 naming the POD and Request-1422', async () => {
    const messageText = executeResult?.message || executeResult?.haystack || '';
    const passed =
      executeResult?.status === 400
      && messageText.includes(POD_IDENTIFIER)
      && messageText.includes(BLOCKING_REQUEST_NUMBER);
    TestRunSummary.recordOutcome(
      'Owner highest-consumption execute is blocked by Request-1422',
      `HTTP 400 OperationNotAllowedException naming ${POD_IDENTIFIER} and ${BLOCKING_REQUEST_NUMBER}`,
      executeResult
        ? `HTTP ${executeResult.status} ${messageText.slice(0, 400)}`
        : 'no response',
      passed,
    );
    finalizeTestRunSummary(TestRunSummary, Responses, {
      jiraKey: PDT_3509_KEY,
      relevantEntityKeys: ['reminderForDisconnection', 'requestForDisconnection'],
      extraLinks: {
        requestForDisconnection: [
          `${DEV_PORTAL_BASE}/request-for-disconnection/preview?id=${BLOCKING_REQUEST_ID}`,
          `${DEV_PORTAL_BASE}/request-for-disconnection/preview?id=${requestId}`,
        ],
        customer: [
          `${DEV_PORTAL_BASE}/customers/preview/basic?id=6045350`,
          `${DEV_PORTAL_BASE}/customers/preview/basic?id=${ALT_CUSTOMER_ID}`,
        ],
        pod: [`${DEV_PORTAL_BASE}/points-of-delivery/preview?id=${POD_ID}`],
      },
      snapshot: {
        blockingRequestId: BLOCKING_REQUEST_ID,
        blockingStatus: blocking.disconnectionRequestsStatus,
        reminderId,
        requestId,
        podId: POD_ID,
      },
    });
  });

  const messageText = executeResult.message || executeResult.haystack;
  expect(executeResult.status, 'Save And Execute HTTP status').toBe(400);
  expect(describesOperationNotAllowed(executeResult), executeResult.haystack.slice(0, 500)).toBe(true);
  expect(messageText, 'error names the POD').toContain(POD_IDENTIFIER);
  expect(messageText, 'error names Request-1422').toContain(BLOCKING_REQUEST_NUMBER);
});
