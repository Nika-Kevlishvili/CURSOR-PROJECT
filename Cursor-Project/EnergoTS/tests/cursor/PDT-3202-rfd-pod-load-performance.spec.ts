/**
 * PDT-3202 — RFD Load PODs performance (Dev2).
 *
 * Bug-only automation (no TC .md). Creates all data from scratch.
 *
 * Actual (Jira customfield_10103): ~5 minutes to load PODs (Reminder-2373, 476 customers).
 * Expected: Prod reminders with >3500 customers load PODs in under 4 seconds.
 *
 * Timed GET: disconnection-of-power-supply-requests/load-customer-for-disconnection-power-supply
 * with CUSTOMERS_UNDER_CONDITIONS + `$CUSTOMER_NUMBER$=$n$OR$CUSTOMER_NUMBER$=$n2$` (never ALL_CUSTOMERS
 * / allSelected true — PDT-3179/PDT-3421). Do not send listOfCustomer (Swagger maxLength 1024;
 * GET @Size min 1 — omit, do not send "").
 * Does not POST a request-for-disconnection object (Load PODs is before save).
 *
 * Why not `$PRODUCT$=${productId}`: reminder condition eval joins invoice → product_details
 * for prod_id (schema.sql ~11779-11780). This test creates overdue MANUAL billing-group
 * liabilities (no invoice) → prod_id null → totalElements=0 (Dev2 reminder 4080, 536ms GET
 * was not an SLO pass). `$CUSTOMER_NUMBER$` is cust_c.customer_number (schema.sql id 4).
 * Why not SQL `IN (...)`: POST reminder 400 `[condition] customer_under_conditions_is_not_valid;`
 * (validateCondition / RuleEvaluatorService). OR-equals is the BillingPayloads formula.
 *
 * Customer count: env PDT3202_CUSTOMER_COUNT, default 80 (30-minute wall-clock cap, not
 * ticket 476). 25-customer Dev2 run ~5 min ≈ 12s each → 80 × 12s ≈ 16 min setup. N>80 may
 * exceed 30 min. First customer: createSupplyChain + overdue billing-group liability.
 * Remaining N-1 reuse the same product via createAdditionalCustomerOnSharedProduct
 * (no new term/electricity/product). Sequential POSTs only (do not parallelize).
 *
 * Swagger: parent refreshed all env specs 2026-09-17 13:01 UTC. This agent re-ran
 * update-swagger-specs.ps1 — failed on macOS (curl.exe missing). Contract taken from
 * Cursor-Project/config/swagger/dev2/swagger-spec.json (mtime 2026-09-17 13:01 local).
 * CheckResponse accepts 206 PARTIAL_CONTENT (ok = 200–299). Do not assert status === 200.
 *
 * Reference spec(s):
 * - tests/cursor/pdt-2913-missing-email-object-reminder-for-disconnection.fixtures.ts
 * - tests/cursor/PDT-3421-rfd-highest-consumption-uncheck.spec.ts
 * - tests/cursor/pdt-3421-rfd-highest-consumption-uncheck.fixtures.ts
 *   (createSecondCustomerSupplyAndLiability ~443-511 copy source)
 * - tests/cursor/PDT-2861-rfd-pod-reconnection-fk.spec.ts
 * - tests/cursor/PDT-2529-rfd-data-model-happy-path.fixtures.ts
 */

import { test, expect } from './cursor-test.fixtures';
import { finalizeTestRunSummary } from './shared/manual-verification-links.fixtures';
import {
  PDT_3202_KEY,
  PDT_3202_LOAD_GET_TIMEOUT_MS,
  PDT_3202_PAGE_SIZE,
  PDT_3202_SLO_MS,
  PDT_3202_TITLE,
  buildPdt3202LoadQuery,
  createAdditionalCustomerOnSharedProduct,
  createOverdueManualLiabilityWithBillingGroup,
  createPdt3202ExecutedReminderUnderConditions,
  createSupplyChain,
  collectPdt3202CustomerNumbers,
  pageContent,
  pageTotalElements,
  parsePdt3202CustomerCount,
  pdt3202CustomerNumberInCondition,
  pdt3202RelevantKeys,
  pdt3202TestTimeoutMs,
  timeLoadCustomersForDps,
  type PageCustomersForDps,
  type Pdt3202Fx,
} from './pdt-3202-rfd-pod-load-performance.fixtures';

test.describe(`[${PDT_3202_KEY}]: ${PDT_3202_TITLE}`, {
  tag: ['@receivableManagement', '@pdt-3202', '@dev2'],
}, () => {
  test.describe.configure({ fullyParallel: false });

  test(`[${PDT_3202_KEY}]: ${PDT_3202_TITLE}`, async ({
    Request, GeneratePayload, Responses, Endpoints, TestRunSummary,
  }) => {
    const customerCount = parsePdt3202CustomerCount();
    test.setTimeout(pdt3202TestTimeoutMs(customerCount));
    const fx: Pdt3202Fx = { Request, GeneratePayload, Responses, Endpoints };

    await test.step('Precondition: first customer supply chain', async () => {
      await createSupplyChain(fx);
      expect(Responses.customer.length, 'first customer').toBe(1);
      expect(Responses.product.length, 'shared product').toBe(1);
      console.log(`[PDT-3202] created customer 1/${customerCount}`);
    });

    await test.step('Precondition: first customer overdue billing-group liability', async () => {
      await createOverdueManualLiabilityWithBillingGroup(fx);
    });

    await test.step(
      `Precondition: remaining ${customerCount - 1} customers on shared product (no new term/electricity/product)`,
      async () => {
        for (let i = 1; i < customerCount; i += 1) {
          const created = await createAdditionalCustomerOnSharedProduct(fx);
          expect(created.identifier.length, `customer ${i + 1} identifier`).toBeGreaterThan(0);
          console.log(`[PDT-3202] created customer ${i + 1}/${customerCount}`);
        }
      },
    );

    expect(Responses.customer.length, 'created customer count').toBe(customerCount);
    const customerNumbers = await test.step(
      'Precondition: collect numeric customerNumber for $CUSTOMER_NUMBER$=$n$ OR-chain',
      async () => {
        const numbers = await collectPdt3202CustomerNumbers(fx);
        expect(numbers.length, 'customerNumber count').toBe(customerCount);
        return numbers;
      },
    );
    const condition = pdt3202CustomerNumberInCondition(customerNumbers);
    TestRunSummary.registerPayload('condition', {
      condition,
      conditionType: 'CUSTOMERS_UNDER_CONDITIONS',
      customerNumbers,
      customerCount,
    });
    TestRunSummary.registerPayload('product', Responses.product[0]);

    const reminderId = await test.step(
      'Precondition: executed reminder CUSTOMERS_UNDER_CONDITIONS $CUSTOMER_NUMBER$ OR-equals (no /job, no listOfCustomer)',
      async () => {
        const created = await createPdt3202ExecutedReminderUnderConditions(fx, condition);
        TestRunSummary.registerPayload('reminder', {
          ...created.payload,
          reminderId: created.reminderId,
          conditionType: 'CUSTOMERS_UNDER_CONDITIONS',
          condition,
          customerNumbers,
        });
        expect(created.reminderId).toBeGreaterThan(0);
        expect(created.payload.conditionType).toBe('CUSTOMERS_UNDER_CONDITIONS');
        expect(created.payload.condition).toBe(condition);
        expect(created.payload.listOfCustomer, 'listOfCustomer must be omitted for UNDER_CONDITIONS').toBeUndefined();
        return created.reminderId;
      },
    );

    const query = buildPdt3202LoadQuery({ condition, reminderId });
    TestRunSummary.registerPayload('loadQuery', query);
    expect(
      Object.prototype.hasOwnProperty.call(query, 'listOfCustomer'),
      'GET params must omit listOfCustomer',
    ).toBe(false);

    const { elapsedMs, body, httpOk, status, totalElements, firstPageLength } =
      await test.step('Action: GET load-customer-for-disconnection-power-supply (timed)', async () => {
        const timed = await timeLoadCustomersForDps(fx, query);
        const loadOk = timed.response.ok();
        await expect(timed.response).CheckResponse();
        const json = (await timed.response.json()) as PageCustomersForDps;
        const content = pageContent(json);
        const total = pageTotalElements(json, content.length);
        return {
          elapsedMs: timed.elapsedMs,
          body: json,
          httpOk: loadOk,
          status: timed.response.status(),
          totalElements: total,
          firstPageLength: content.length,
        };
      });

    TestRunSummary.registerPayload('metrics', {
      customerCount,
      customerNumbers,
      condition,
      reminderId,
      elapsedMs,
      httpStatus: status,
    });

    const elapsedOk = elapsedMs < PDT_3202_SLO_MS;
    const contentIsArray = Array.isArray(body.content);
    const reportedTotalElements = Number(body.totalElements);
    const hasTotalElements = Number.isFinite(reportedTotalElements);
    const totalOk = hasTotalElements && reportedTotalElements >= customerCount;

    TestRunSummary.recordCheck({
      check: 'Load PODs GET HTTP success',
      expectedResult:
        'HTTP 2xx (Swagger 200; runtime 206 PARTIAL_CONTENT is ok). CheckResponse uses response.ok() (200–299).',
      actualResult: httpOk
        ? `As expected — HTTP ${status} in ${elapsedMs}ms (GET timeout ${PDT_3202_LOAD_GET_TIMEOUT_MS}ms).`
        : `Not as expected — HTTP ${status} in ${elapsedMs}ms.`,
      passed: httpOk,
    });

    TestRunSummary.recordCheck({
      check: 'Load PODs elapsed under 4 seconds',
      expectedResult: `elapsedMs < ${PDT_3202_SLO_MS} (PDT-3202 / Prod >3500 customers).`,
      actualResult: elapsedOk
        ? `As expected — elapsedMs=${elapsedMs}, totalElements=${totalElements}, firstPageLength=${firstPageLength}.`
        : `Not as expected — elapsedMs=${elapsedMs} (SLO ${PDT_3202_SLO_MS}ms); totalElements=${totalElements}; firstPageLength=${firstPageLength}.`,
      passed: elapsedOk,
    });

    TestRunSummary.recordCheck({
      check: 'Load PODs page covers created customers',
      expectedResult: `totalElements ≥ ${customerCount}; content is an array. First page may have only ${PDT_3202_PAGE_SIZE} rows when N>${PDT_3202_PAGE_SIZE}.`,
      actualResult: totalOk && contentIsArray
        ? `As expected — totalElements=${reportedTotalElements}, firstPageLength=${firstPageLength}, size=${PDT_3202_PAGE_SIZE}.`
        : `Not as expected — totalElements=${String(body.totalElements)}, contentIsArray=${contentIsArray}, firstPageLength=${firstPageLength}.`,
      passed: totalOk && contentIsArray,
    });

    expect(httpOk, `Load PODs GET HTTP ${status}`).toBe(true);
    expect(
      elapsedMs,
      `Load PODs SLO < ${PDT_3202_SLO_MS}ms (PDT-3202). elapsedMs=${elapsedMs}; totalElements=${totalElements}; firstPageLength=${firstPageLength}`,
    ).toBeLessThan(PDT_3202_SLO_MS);
    expect(contentIsArray, 'PageCustomersForDPSResponse.content is an array').toBe(true);
    expect(
      hasTotalElements,
      `PageCustomersForDPSResponse.totalElements must be present (got ${String(body.totalElements)})`,
    ).toBe(true);
    expect(
      reportedTotalElements,
      `totalElements must be ≥ created customer count ${customerCount} (first page length=${firstPageLength})`,
    ).toBeGreaterThanOrEqual(customerCount);
    expect(firstPageLength, 'first page length').toBeLessThanOrEqual(PDT_3202_PAGE_SIZE);
    if (reportedTotalElements <= PDT_3202_PAGE_SIZE) {
      expect(firstPageLength, 'first page should contain all rows when totalElements ≤ size').toBe(
        reportedTotalElements,
      );
    }

    await test.step('Attach test run summary', async () => {
      TestRunSummary.registerPayload('customer', Responses.customer[0]);
      if (Responses.reminderForDisconnection.length) {
        TestRunSummary.registerPayload(
          'reminderForDisconnection',
          Responses.reminderForDisconnection[Responses.reminderForDisconnection.length - 1],
        );
      }
      finalizeTestRunSummary(TestRunSummary, Responses, {
        jiraKey: PDT_3202_KEY,
        relevantEntityKeys: pdt3202RelevantKeys(),
        snapshot: {
          customerCount,
          customerNumbers,
          condition,
          reminderId,
          elapsedMs,
          totalElements,
          firstPageLength,
          httpStatus: status,
          getTimeoutMs: PDT_3202_LOAD_GET_TIMEOUT_MS,
        },
      });
    });
  });
});
