/**
 * PDT-3206 — Changes in JSON for contract template.
 *
 * Maps Backend TC-BE-1 … TC-BE-17 1:1.
 * Inspect API: GET …/document-json-test?versionId= (ContractDocumentModel PascalCase).
 * Create proxy: ProxyEditRequest camelCase on basicParameters.proxy.
 *
 * Reference spec(s):
 * - tests/cursor/PDT-3179-reconnection-invoice-emails.spec.ts
 * - tests/cursor/PHN-2130-sales-portal-contract-update.spec.ts (buildProxyForCreate ~1998)
 * - tests/cursor/pdt-2599-service-contract.fixtures.ts
 * - jsons/payloadGenerators/domains/ContractsAndOrdersPayloads.ts (product_contract, serviceContract, edit_ProductContract)
 * - jsons/payloadGenerators/domains/CustomerPayloads.ts
 * - jsons/payloads/create/contractOrders/productContract.ts
 *
 * Swagger (dev) gaps: POST /product-contract and /service-contract document 200, runtime 201;
 * GET generate-popup documents 200, runtime 206 PARTIAL_CONTENT; document-json-test has no 4xx/5xx.
 */

import { test, expect } from './cursor-test.fixtures';
import type { TestRunSummaryCollector } from './shared/test-run-summary.fixtures';
import {
  finalizeTestRunSummary,
  buildProductContractTabLinks,
} from './shared/manual-verification-links.fixtures';
import {
  PDT_3206_KEY,
  PDT_3206_TITLE,
  PDT_3206_TEST_TIMEOUT_MS,
  PDT_3206_AA_TIMEOUT_MS,
  addDaysIso,
  assertPdt3206EightKeys,
  assertPdt3206ProxyPrivateBeforeManagers,
  buildPdt3206Proxy,
  createPdt3206BaCustomer,
  createPdt3206LegalCustomer,
  createPdt3206PrivateCustomer,
  errorHaystack,
  findProxyNameInManagers,
  getPdt3206DocumentJson,
  getPdt3206ProductContract,
  getPdt3206ServiceContract,
  latestLogicalVersionId,
  managersList,
  pdt3206RelevantKeys,
  pdt3206ValidEgn,
  postPdt3206ProductContract,
  postPdt3206ServiceContract,
  proxyPrivateList,
  putPdt3206ProductContract,
  setupPdt3206ProductBase,
  setupPdt3206ServiceBase,
  toProxyEditRequest,
  tryPdt3206Generate,
  type Pdt3206Fx,
} from './pdt-3206-contract-template-proxy-private-json.fixtures';

function titleFor(tc: string, scenario: string): string {
  return `[${PDT_3206_KEY}]: ${PDT_3206_TITLE} | ${tc} – ${scenario}`;
}

async function attachSummary(
  TestRunSummary: TestRunSummaryCollector,
  Responses: Pdt3206Fx['Responses'],
  opts: {
    kind: 'product' | 'service' | 'none';
    snapshot: Record<string, unknown>;
    contractId?: number | null;
  },
): Promise<void> {
  await test.step('Attach test run summary', async () => {
    if (Responses.customer[0]) TestRunSummary.registerPayload('customer', Responses.customer[0]);
    if (Responses.productContract[0]) {
      TestRunSummary.registerPayload('productContract', Responses.productContract[0]);
    }
    if (Responses.serviceContract[0]) {
      TestRunSummary.registerPayload('serviceContract', Responses.serviceContract[0]);
    }
    let extra: Record<string, string[]> | undefined;
    if (opts.kind === 'product' && opts.contractId) {
      extra = buildProductContractTabLinks(opts.contractId);
    }
    finalizeTestRunSummary(TestRunSummary, Responses, {
      jiraKey: PDT_3206_KEY,
      relevantEntityKeys: pdt3206RelevantKeys(opts.kind),
      extraLinks: extra && Object.keys(extra).length ? extra : undefined,
      snapshot: opts.snapshot,
    });
  });
}

test.describe(`[${PDT_3206_KEY}]: ${PDT_3206_TITLE}`, {
  tag: ['@contractsAndOrders', '@pdt-3206', '@dev'],
}, () => {
  test(titleFor('TC-BE-1', 'Product PRIVATE one ACTIVE proxy — ProxyPrivate length 1'), async ({
    Request, GeneratePayload, Responses, Endpoints, TestRunSummary,
  }) => {
    test.setTimeout(PDT_3206_TEST_TIMEOUT_MS);
    const fx: Pdt3206Fx = { Request, GeneratePayload, Responses, Endpoints };
    await test.step('Precondition: terms, price, product, POD', async () => {
      await setupPdt3206ProductBase(fx);
    });
    await test.step('Precondition: private customer', async () => {
      await createPdt3206PrivateCustomer(fx);
    });
    const proxyName = 'Ivan Petrov Proxy';
    const poa = 'POAMAIN001';
    const created = await test.step('Precondition: product contract with one private proxy', async () => {
      const proxy = buildPdt3206Proxy(GeneratePayload, {
        proxyName,
        proxyPowerOfAttorneyNumber: poa,
        notaryPublic: 'Notary Main',
        registrationNumber: 'REGMAIN111',
        areaOfOperation: 'Sofia City',
        proxyData: '2026-01-15',
      });
      TestRunSummary.registerPayload('proxy', proxy);
      return postPdt3206ProductContract(fx, { proxy: [proxy] });
    });
    const contractId = created.id!;
    await test.step('Precondition: GET product-contract version 1 — proxy ACTIVE', async () => {
      const detail = await getPdt3206ProductContract(fx, contractId, 1);
      const proxies = ((detail.basicParameters as Record<string, unknown>)?.proxy ?? []) as Array<{
        status?: string;
      }>;
      expect(proxies[0]?.status).toBe('ACTIVE');
    });

    const doc = await test.step('Action: GET document-json-test versionId=1', async () =>
      getPdt3206DocumentJson(fx, 'product', contractId, 1));
    expect(doc.status).toBe(200);
    const priv = proxyPrivateList(doc.json);
    const mgrs = managersList(doc.json);
    expect(priv).toHaveLength(1);
    assertPdt3206EightKeys(priv[0]);
    expect(priv[0].ProxyName).toBe(proxyName);
    expect(priv[0].PowerAttroneyNumber).toBe(poa);
    expect(mgrs).toEqual([]);
    assertPdt3206ProxyPrivateBeforeManagers(doc.rawText);

    const gen = await test.step('Additional: generate-popup / generate (skip if no template)', async () =>
      tryPdt3206Generate(fx, 'product', contractId, 1));
    expect([206, 200]).toContain(gen.popupStatus);
    if (!gen.skipped && gen.generateStatus != null) {
      expect(gen.generateStatus).toBe(200);
    }

    const passed = doc.status === 200 && priv.length === 1 && mgrs.length === 0;
    TestRunSummary.recordCheck({
      check: 'TC-BE-1 ProxyPrivate length 1, Managers [], key order',
      expectedResult:
        'HTTP 200; ProxyPrivate[0].ProxyName Ivan Petrov Proxy; PowerAttroneyNumber POAMAIN001; Managers []; ProxyPrivate before Managers.',
      actualResult: passed
        ? `As expected — ProxyPrivate=${priv.length}, Managers=${mgrs.length}, ProxyName=${String(priv[0]?.ProxyName)}.`
        : `Not as expected — status=${doc.status}, ProxyPrivate=${priv.length}.`,
      passed,
    });
    await attachSummary(TestRunSummary, Responses, {
      kind: 'product',
      contractId,
      snapshot: { contractId, proxyName },
    });
  });

  test(titleFor('TC-BE-2', 'Service PRIVATE one ACTIVE proxy — same JSON split'), async ({
    Request, GeneratePayload, Responses, Endpoints, TestRunSummary,
  }) => {
    test.setTimeout(PDT_3206_TEST_TIMEOUT_MS);
    const fx: Pdt3206Fx = { Request, GeneratePayload, Responses, Endpoints };
    await test.step('Precondition: terms, price, service, POD', async () => {
      await setupPdt3206ServiceBase(fx);
    });
    await test.step('Precondition: private customer', async () => {
      await createPdt3206PrivateCustomer(fx);
    });
    const proxyName = 'Service Private Proxy';
    const poa = 'POASVC001';
    const created = await test.step('Precondition: service contract with one private proxy', async () => {
      const proxy = buildPdt3206Proxy(GeneratePayload, {
        proxyName,
        proxyPowerOfAttorneyNumber: poa,
        notaryPublic: 'Notary Service',
        registrationNumber: 'REGSVC111',
        areaOfOperation: 'Varna City',
        proxyData: '2026-02-01',
        proxyCustomerIdentifier: pdt3206ValidEgn(GeneratePayload),
      });
      TestRunSummary.registerPayload('proxy', proxy);
      return postPdt3206ServiceContract(fx, { proxy: [proxy] });
    });
    const contractId = created.id!;
    await test.step('Precondition: GET service-contract version 1', async () => {
      await getPdt3206ServiceContract(fx, contractId, 1);
    });

    const doc = await test.step('Action: GET service document-json-test versionId=1', async () =>
      getPdt3206DocumentJson(fx, 'service', contractId, 1));
    expect(doc.status).toBe(200);
    const priv = proxyPrivateList(doc.json);
    const mgrs = managersList(doc.json);
    expect(priv).toHaveLength(1);
    assertPdt3206EightKeys(priv[0]);
    expect(priv[0].ProxyName).toBe(proxyName);
    expect(priv[0].PowerAttroneyNumber).toBe(poa);
    expect(mgrs).toEqual([]);
    assertPdt3206ProxyPrivateBeforeManagers(doc.rawText);

    const gen = await test.step('Additional: generate-popup / generate (skip if no template)', async () =>
      tryPdt3206Generate(fx, 'service', contractId, 1));
    expect([206, 200]).toContain(gen.popupStatus);

    const passed = doc.status === 200 && priv.length === 1 && String(priv[0].ProxyName) === proxyName;
    TestRunSummary.recordCheck({
      check: 'TC-BE-2 service ProxyPrivate length 1',
      expectedResult: 'HTTP 200; ProxyPrivate[0].ProxyName Service Private Proxy; Managers []; order.',
      actualResult: passed
        ? `As expected — ProxyName=${String(priv[0]?.ProxyName)}.`
        : `Not as expected — status=${doc.status}.`,
      passed,
    });
    await attachSummary(TestRunSummary, Responses, {
      kind: 'service',
      contractId,
      snapshot: { serviceContractId: contractId },
    });
  });

  test(titleFor('TC-BE-3', 'Product LEGAL manager proxy — ProxyPrivate [] Managers filled'), async ({
    Request, GeneratePayload, Responses, Endpoints, TestRunSummary,
  }) => {
    test.setTimeout(PDT_3206_TEST_TIMEOUT_MS);
    const fx: Pdt3206Fx = { Request, GeneratePayload, Responses, Endpoints };
    await test.step('Precondition: terms, price, product, POD', async () => {
      await setupPdt3206ProductBase(fx);
    });
    const { managerId } = await test.step('Precondition: legal customer with manager', async () =>
      createPdt3206LegalCustomer(fx, 'LEGALMGR', 'ONE'));
    const proxyName = 'Legal Manager Proxy';
    const poa = 'POALEG001';
    const created = await test.step('Precondition: product contract with managerIds', async () => {
      const proxy = buildPdt3206Proxy(GeneratePayload, {
        proxyName,
        proxyPowerOfAttorneyNumber: poa,
        notaryPublic: 'Notary Legal',
        registrationNumber: 'REGLEG111',
        areaOfOperation: 'Burgas Area',
        proxyData: '2026-03-01',
        proxyCustomerIdentifier: pdt3206ValidEgn(GeneratePayload),
        managerIds: [managerId],
      });
      TestRunSummary.registerPayload('proxy', proxy);
      return postPdt3206ProductContract(fx, { proxy: [proxy] });
    });
    const contractId = created.id!;

    const doc = await test.step('Action: GET document-json-test', async () =>
      getPdt3206DocumentJson(fx, 'product', contractId, 1));
    expect(doc.status).toBe(200);
    const priv = proxyPrivateList(doc.json);
    const mgrs = managersList(doc.json);
    expect(priv).toEqual([]);
    expect(mgrs.length).toBeGreaterThan(0);
    expect(findProxyNameInManagers(mgrs, proxyName), 'Managers[].ProxyList must contain Legal Manager Proxy').toBe(
      true,
    );
    const managerNames = mgrs.map((m) => String(m.Name ?? ''));
    expect(managerNames.some((n) => n.includes('LEGALMGR'))).toBe(true);
    assertPdt3206ProxyPrivateBeforeManagers(doc.rawText);

    const passed = priv.length === 0 && mgrs.length > 0 && findProxyNameInManagers(mgrs, proxyName);
    TestRunSummary.recordCheck({
      check: 'TC-BE-3 legal ProxyPrivate empty, nested ProxyList contains name',
      expectedResult: 'ProxyPrivate []; Managers non-empty; ProxyList contains Legal Manager Proxy.',
      actualResult: passed
        ? `As expected — Managers=${mgrs.length}.`
        : `Not as expected — ProxyPrivate=${priv.length}, nested=${findProxyNameInManagers(mgrs, proxyName)}.`,
      passed,
    });
    await attachSummary(TestRunSummary, Responses, {
      kind: 'product',
      contractId,
      snapshot: { contractId, managerId },
    });
  });

  test(titleFor('TC-BE-4', 'Service LEGAL manager proxy — ProxyPrivate [] Managers filled'), async ({
    Request, GeneratePayload, Responses, Endpoints, TestRunSummary,
  }) => {
    test.setTimeout(PDT_3206_TEST_TIMEOUT_MS);
    const fx: Pdt3206Fx = { Request, GeneratePayload, Responses, Endpoints };
    await test.step('Precondition: terms, price, service, POD', async () => {
      await setupPdt3206ServiceBase(fx);
    });
    const { managerId } = await test.step('Precondition: legal customer with manager', async () =>
      createPdt3206LegalCustomer(fx, 'SVCLEGALMGR', 'ONE'));
    const proxyName = 'Svc Legal Proxy';
    const created = await test.step('Precondition: service contract with managerIds', async () => {
      const proxy = buildPdt3206Proxy(GeneratePayload, {
        proxyName,
        proxyPowerOfAttorneyNumber: 'POASLEG01',
        notaryPublic: 'Notary SvcLegal',
        areaOfOperation: 'Ruse Area',
        proxyData: '2026-03-15',
        proxyCustomerIdentifier: pdt3206ValidEgn(GeneratePayload),
        managerIds: [managerId],
      });
      return postPdt3206ServiceContract(fx, { proxy: [proxy] });
    });
    const contractId = created.id!;

    const doc = await test.step('Action: GET service document-json-test', async () =>
      getPdt3206DocumentJson(fx, 'service', contractId, 1));
    expect(doc.status).toBe(200);
    const priv = proxyPrivateList(doc.json);
    const mgrs = managersList(doc.json);
    expect(priv).toEqual([]);
    expect(mgrs.length).toBeGreaterThan(0);
    expect(findProxyNameInManagers(mgrs, proxyName), 'Managers[].ProxyList must contain Svc Legal Proxy').toBe(true);
    const managerNames = mgrs.map((m) => String(m.Name ?? ''));
    expect(managerNames.some((n) => n.includes('SVCLEGALMGR'))).toBe(true);
    assertPdt3206ProxyPrivateBeforeManagers(doc.rawText);

    const passed = priv.length === 0 && mgrs.length > 0 && findProxyNameInManagers(mgrs, proxyName);
    TestRunSummary.recordCheck({
      check: 'TC-BE-4 service legal split',
      expectedResult: 'ProxyPrivate []; Managers non-empty; nested ProxyList contains Svc Legal Proxy.',
      actualResult: passed ? 'As expected.' : `Not as expected — ProxyPrivate=${priv.length}.`,
      passed,
    });
    await attachSummary(TestRunSummary, Responses, {
      kind: 'service',
      contractId,
      snapshot: { serviceContractId: contractId, managerId },
    });
  });

  test(titleFor('TC-BE-5', 'Product PRIVATE no proxy — both lists []'), async ({
    Request, GeneratePayload, Responses, Endpoints, TestRunSummary,
  }) => {
    test.setTimeout(PDT_3206_TEST_TIMEOUT_MS);
    const fx: Pdt3206Fx = { Request, GeneratePayload, Responses, Endpoints };
    await test.step('Precondition: terms, price, product, POD', async () => {
      await setupPdt3206ProductBase(fx);
    });
    await test.step('Precondition: private customer', async () => {
      await createPdt3206PrivateCustomer(fx);
    });
    const created = await test.step('Precondition: product contract with proxy []', async () =>
      postPdt3206ProductContract(fx, { proxy: [] }));
    const contractId = created.id!;

    const doc = await test.step('Action: GET document-json-test', async () =>
      getPdt3206DocumentJson(fx, 'product', contractId, 1));
    expect(doc.status).toBe(200);
    expect(doc.json).toHaveProperty('ProxyPrivate');
    expect(doc.json).toHaveProperty('Managers');
    expect(proxyPrivateList(doc.json)).toEqual([]);
    expect(managersList(doc.json)).toEqual([]);
    assertPdt3206ProxyPrivateBeforeManagers(doc.rawText);

    TestRunSummary.recordCheck({
      check: 'TC-BE-5 both lists present as empty arrays',
      expectedResult: 'HTTP 200; ProxyPrivate [] and Managers [] keys present; order.',
      actualResult: 'As expected — both keys present and empty.',
      passed: true,
    });
    await attachSummary(TestRunSummary, Responses, {
      kind: 'product',
      contractId,
      snapshot: { contractId },
    });
  });

  test(titleFor('TC-BE-6', 'PowerAttroneyNumber key equals POASPELL99'), async ({
    Request, GeneratePayload, Responses, Endpoints, TestRunSummary,
  }) => {
    test.setTimeout(PDT_3206_TEST_TIMEOUT_MS);
    const fx: Pdt3206Fx = { Request, GeneratePayload, Responses, Endpoints };
    await test.step('Precondition: terms, price, product, POD', async () => {
      await setupPdt3206ProductBase(fx);
    });
    await test.step('Precondition: private customer', async () => {
      await createPdt3206PrivateCustomer(fx);
    });
    const poa = 'POASPELL99';
    const created = await test.step('Precondition: product contract with POASPELL99', async () => {
      const proxy = buildPdt3206Proxy(GeneratePayload, {
        proxyName: 'Spelling Proxy',
        proxyPowerOfAttorneyNumber: poa,
      });
      return postPdt3206ProductContract(fx, { proxy: [proxy] });
    });
    const contractId = created.id!;

    const doc = await test.step('Action: GET document-json-test', async () =>
      getPdt3206DocumentJson(fx, 'product', contractId, 1));
    expect(doc.status).toBe(200);
    const row = proxyPrivateList(doc.json)[0];
    expect(row).toBeTruthy();
    expect(row).toHaveProperty('PowerAttroneyNumber', poa);
    expect(Object.prototype.hasOwnProperty.call(row, 'PowerOfAttorneyNumber')).toBe(false);

    TestRunSummary.recordCheck({
      check: 'TC-BE-6 misspelled PowerAttroneyNumber key',
      expectedResult: 'ProxyPrivate[0].PowerAttroneyNumber = POASPELL99; no PowerOfAttorneyNumber key.',
      actualResult: `As expected — PowerAttroneyNumber=${String(row?.PowerAttroneyNumber)}.`,
      passed: true,
    });
    await attachSummary(TestRunSummary, Responses, {
      kind: 'product',
      contractId,
      snapshot: { contractId, poa },
    });
  });

  test(titleFor('TC-BE-7', 'Authorized-proxy coalesce — RegistrationNumber from MAIN'), async ({
    Request, GeneratePayload, Responses, Endpoints, TestRunSummary,
  }) => {
    test.setTimeout(PDT_3206_TEST_TIMEOUT_MS);
    const fx: Pdt3206Fx = { Request, GeneratePayload, Responses, Endpoints };
    await test.step('Precondition: terms, price, product, POD', async () => {
      await setupPdt3206ProductBase(fx);
    });
    await test.step('Precondition: private customer', async () => {
      await createPdt3206PrivateCustomer(fx);
    });
    const created = await test.step('Precondition: product contract with main + authorized proxy fields', async () => {
      const proxy = buildPdt3206Proxy(GeneratePayload, {
        proxyName: 'Main Proxy Name',
        proxyPowerOfAttorneyNumber: 'POAMAIN777',
        notaryPublic: 'Notary MainSide',
        registrationNumber: 'REGMAIN777',
        areaOfOperation: 'Main Operation Area',
        proxyData: '2026-04-01',
        proxyCustomerIdentifier: pdt3206ValidEgn(GeneratePayload),
        proxyAuthorizedByProxy: 'Auth Proxy Name',
        authorizedProxyPowerOfAttorneyNumber: 'POAAUTH888',
        authorizedProxyNotaryPublic: 'Notary AuthSide',
        authorizedProxyRegistrationNumber: 'REGAUTH888',
        authorizedProxyAreaOfOperation: 'Auth Operation Area',
      });
      TestRunSummary.registerPayload('proxy', proxy);
      return postPdt3206ProductContract(fx, { proxy: [proxy] });
    });
    const contractId = created.id!;

    const doc = await test.step('Action: GET document-json-test', async () =>
      getPdt3206DocumentJson(fx, 'product', contractId, 1));
    expect(doc.status).toBe(200);
    const priv = proxyPrivateList(doc.json);
    expect(priv).toHaveLength(1);
    const row = priv[0];
    expect(row.ProxyName).toBe('Auth Proxy Name');
    expect(row.PowerAttroneyNumber).toBe('POAAUTH888');
    expect(row.NotaryPublic).toBe('Notary AuthSide');
    expect(row.OperationArea).toBe('Auth Operation Area');
    expect(row.RegistrationNumber).toBe('REGMAIN777');
    expect(row.RegistrationNumber).not.toBe('REGAUTH888');
    expect(managersList(doc.json)).toEqual([]);

    TestRunSummary.recordCheck({
      check: 'TC-BE-7 coalesce Auth name/POA/notary/area; RegistrationNumber from main',
      expectedResult:
        'ProxyName Auth Proxy Name; PowerAttroneyNumber POAAUTH888; RegistrationNumber REGMAIN777 not REGAUTH888.',
      actualResult: `As expected — RegistrationNumber=${String(row.RegistrationNumber)}.`,
      passed: true,
    });
    await attachSummary(TestRunSummary, Responses, {
      kind: 'product',
      contractId,
      snapshot: { contractId, registrationNumber: row.RegistrationNumber },
    });
  });

  test(titleFor('TC-BE-8', 'Latin source — Trsl keys exist'), async ({
    Request, GeneratePayload, Responses, Endpoints, TestRunSummary,
  }) => {
    test.setTimeout(PDT_3206_TEST_TIMEOUT_MS);
    const fx: Pdt3206Fx = { Request, GeneratePayload, Responses, Endpoints };
    await test.step('Precondition: terms, price, product, POD', async () => {
      await setupPdt3206ProductBase(fx);
    });
    await test.step('Precondition: private customer', async () => {
      await createPdt3206PrivateCustomer(fx);
    });
    const created = await test.step('Precondition: product contract with Latin proxy fields for Trsl keys', async () => {
      const proxy = buildPdt3206Proxy(GeneratePayload, {
        proxyName: 'Ivan Petrov Trsl',
        notaryPublic: 'Notary Sofia',
        areaOfOperation: 'Sofia City',
        proxyPowerOfAttorneyNumber: 'POATRSL01',
        proxyData: '2026-05-01',
        proxyCustomerIdentifier: pdt3206ValidEgn(GeneratePayload),
      });
      return postPdt3206ProductContract(fx, { proxy: [proxy] });
    });
    const contractId = created.id!;

    const doc = await test.step('Action: GET document-json-test', async () =>
      getPdt3206DocumentJson(fx, 'product', contractId, 1));
    expect(doc.status).toBe(200);
    const row = proxyPrivateList(doc.json)[0];
    expect(row).toHaveProperty('ProxyNameTrsl');
    expect(row).toHaveProperty('NotaryPublicTrsl');
    expect(row).toHaveProperty('OperationAreaTrsl');
    expect(typeof row.ProxyNameTrsl).toBe('string');
    expect(typeof row.NotaryPublicTrsl).toBe('string');
    expect(typeof row.OperationAreaTrsl).toBe('string');

    TestRunSummary.recordCheck({
      check: 'TC-BE-8 Trsl keys present (values may equal source)',
      expectedResult: 'ProxyNameTrsl, NotaryPublicTrsl, OperationAreaTrsl exist as strings.',
      actualResult: `As expected — Trsl keys present (ProxyNameTrsl=${String(row.ProxyNameTrsl)}).`,
      passed: true,
    });
    await attachSummary(TestRunSummary, Responses, {
      kind: 'product',
      contractId,
      snapshot: { contractId },
    });
  });

  test(titleFor('TC-BE-9', 'Additional agreement version uses same ProxyPrivate split'), async ({
    Request, GeneratePayload, Responses, Endpoints, TestRunSummary,
  }) => {
    test.setTimeout(PDT_3206_AA_TIMEOUT_MS);
    const fx: Pdt3206Fx = { Request, GeneratePayload, Responses, Endpoints };
    await test.step('Precondition: terms, price, product, POD', async () => {
      await setupPdt3206ProductBase(fx);
    });
    await test.step('Precondition: private customer', async () => {
      await createPdt3206PrivateCustomer(fx);
    });
    const proxyName = 'Addendum Proxy';
    const created = await test.step('Precondition: product contract v1 with private proxy', async () => {
      const proxy = buildPdt3206Proxy(GeneratePayload, {
        proxyName,
        proxyPowerOfAttorneyNumber: 'POAADD001',
      });
      return postPdt3206ProductContract(fx, { proxy: [proxy] });
    });
    const contractId = created.id!;

    let newVersionId = 2;
    await test.step('Action: PUT savingAsNewVersion ADDITIONAL_AGREEMENT', async () => {
      const v1 = await getPdt3206ProductContract(fx, contractId, 1);
      const versions = (v1.versions ?? []) as Array<{ startDate?: string }>;
      const start = String(versions[0]?.startDate ?? (v1.basicParameters as { signingDate?: string })?.signingDate);
      const copied = (((v1.basicParameters as Record<string, unknown>)?.proxy ?? []) as Record<string, unknown>[]).map(
        toProxyEditRequest,
      );
      await putPdt3206ProductContract(fx, {
        versionId: 1,
        savingAsNewVersion: true,
        type: 'ADDITIONAL_AGREEMENT',
        startDate: addDaysIso(start, 1),
        proxy: copied,
      });
      const latest = await getPdt3206ProductContract(fx, contractId);
      newVersionId = latestLogicalVersionId(latest);
      expect(newVersionId).toBeGreaterThan(1);
      const aa = await getPdt3206ProductContract(fx, contractId, newVersionId);
      expect(String((aa.basicParameters as { type?: string }).type)).toBe('ADDITIONAL_AGREEMENT');
    });

    const doc = await test.step('Action: GET document-json-test on new versionId', async () =>
      getPdt3206DocumentJson(fx, 'product', contractId, newVersionId));
    expect(doc.status).toBe(200);
    const priv = proxyPrivateList(doc.json);
    expect(priv).toHaveLength(1);
    expect(priv[0].ProxyName).toBe(proxyName);
    expect(managersList(doc.json)).toEqual([]);
    assertPdt3206ProxyPrivateBeforeManagers(doc.rawText);

    TestRunSummary.recordCheck({
      check: 'TC-BE-9 additional-agreement document JSON split',
      expectedResult: `document-json-test on versionId=${newVersionId}: ProxyPrivate Addendum Proxy; Managers [].`,
      actualResult: `As expected — versionId=${newVersionId}, ProxyName=${String(priv[0]?.ProxyName)}.`,
      passed: true,
    });
    await attachSummary(TestRunSummary, Responses, {
      kind: 'product',
      contractId,
      snapshot: { contractId, newVersionId },
    });
  });

  test(titleFor('TC-BE-10', 'Product PRIVATE two proxies — 400 not legal entity'), async ({
    Request, GeneratePayload, Responses, Endpoints, TestRunSummary,
  }) => {
    test.setTimeout(PDT_3206_TEST_TIMEOUT_MS);
    const fx: Pdt3206Fx = { Request, GeneratePayload, Responses, Endpoints };
    await test.step('Precondition: terms, price, product, POD', async () => {
      await setupPdt3206ProductBase(fx);
    });
    await test.step('Precondition: private customer', async () => {
      await createPdt3206PrivateCustomer(fx);
    });
    const fragment = 'you cant add multiple proxy when customer type is not legal entity';
    const posted = await test.step('Action: POST product-contract with two proxies', async () => {
      const a = buildPdt3206Proxy(GeneratePayload, {
        proxyName: 'First Multi Proxy',
        proxyCustomerIdentifier: pdt3206ValidEgn(GeneratePayload),
      });
      const b = buildPdt3206Proxy(GeneratePayload, {
        proxyName: 'Second Multi Proxy',
        proxyCustomerIdentifier: pdt3206ValidEgn(GeneratePayload),
        proxyPowerOfAttorneyNumber: 'POAMULTI2',
      });
      return postPdt3206ProductContract(fx, { proxy: [a, b], expectSuccess: false });
    });
    expect(posted.status).toBe(400);
    expect(posted.status).not.toBe(201);
    const hay = errorHaystack(posted.status, posted.text, posted.json);
    expect(hay).toContain(fragment);

    TestRunSummary.recordCheck({
      check: 'TC-BE-10 two private proxies rejected',
      expectedResult: `HTTP 400; message contains "${fragment}".`,
      actualResult: `As expected — status=${posted.status}.`,
      passed: posted.status === 400 && hay.includes(fragment),
    });
    await attachSummary(TestRunSummary, Responses, {
      kind: 'product',
      snapshot: { status: posted.status },
    });
  });

  test(titleFor('TC-BE-11', 'PUT proxy [] deletes ACTIVE proxy from ProxyPrivate'), async ({
    Request, GeneratePayload, Responses, Endpoints, TestRunSummary,
  }) => {
    test.setTimeout(PDT_3206_AA_TIMEOUT_MS);
    const fx: Pdt3206Fx = { Request, GeneratePayload, Responses, Endpoints };
    await test.step('Precondition: terms, price, product, POD', async () => {
      await setupPdt3206ProductBase(fx);
    });
    await test.step('Precondition: private customer', async () => {
      await createPdt3206PrivateCustomer(fx);
    });
    const proxyName = 'Soon Deleted Proxy';
    const poa = 'POADEL001';
    const created = await test.step('Precondition: product contract with Soon Deleted Proxy', async () => {
      const proxy = buildPdt3206Proxy(GeneratePayload, {
        proxyName,
        proxyPowerOfAttorneyNumber: poa,
      });
      return postPdt3206ProductContract(fx, { proxy: [proxy] });
    });
    const contractId = created.id!;

    await test.step('Action: PUT savingAsNewVersion false, proxy []', async () => {
      await putPdt3206ProductContract(fx, {
        versionId: 1,
        savingAsNewVersion: false,
        proxy: [],
      });
    });

    const doc = await test.step('Action: GET document-json-test', async () =>
      getPdt3206DocumentJson(fx, 'product', contractId, 1));
    expect(doc.status).toBe(200);
    const priv = proxyPrivateList(doc.json);
    const names = priv.map((p) => String(p.ProxyName ?? ''));
    const poas = priv.map((p) => String(p.PowerAttroneyNumber ?? ''));
    expect(names).not.toContain(proxyName);
    expect(poas).not.toContain(poa);
    expect(doc.json).toHaveProperty('ProxyPrivate');
    expect(doc.json).toHaveProperty('Managers');
    expect(managersList(doc.json)).toEqual([]);

    TestRunSummary.recordCheck({
      check: 'TC-BE-11 deleted proxy absent from ProxyPrivate',
      expectedResult: 'HTTP 200; ProxyPrivate does not contain Soon Deleted Proxy / POADEL001.',
      actualResult: `As expected — ProxyPrivate names=${JSON.stringify(names)}.`,
      passed: true,
    });
    await attachSummary(TestRunSummary, Responses, {
      kind: 'product',
      contractId,
      snapshot: { contractId },
    });
  });

  test(titleFor('TC-BE-12', 'LEGAL canary name must not leak into ProxyPrivate'), async ({
    Request, GeneratePayload, Responses, Endpoints, TestRunSummary,
  }) => {
    test.setTimeout(PDT_3206_TEST_TIMEOUT_MS);
    const fx: Pdt3206Fx = { Request, GeneratePayload, Responses, Endpoints };
    await test.step('Precondition: terms, price, product, POD', async () => {
      await setupPdt3206ProductBase(fx);
    });
    const { managerId } = await test.step('Precondition: legal customer', async () =>
      createPdt3206LegalCustomer(fx));
    const canary = 'MUST-NOT-LEAK-PROXY-NAME';
    const created = await test.step('Precondition: product contract with canary proxy + managerIds', async () => {
      const proxy = buildPdt3206Proxy(GeneratePayload, {
        proxyName: canary,
        proxyPowerOfAttorneyNumber: 'POALEAK01',
        managerIds: [managerId],
      });
      return postPdt3206ProductContract(fx, { proxy: [proxy] });
    });
    const contractId = created.id!;

    const doc = await test.step('Action: GET document-json-test', async () =>
      getPdt3206DocumentJson(fx, 'product', contractId, 1));
    expect(doc.status).toBe(200);
    const priv = proxyPrivateList(doc.json);
    expect(priv).toEqual([]);
    expect(priv.some((p) => String(p.ProxyName) === canary)).toBe(false);
    expect(managersList(doc.json).length).toBeGreaterThan(0);

    TestRunSummary.recordCheck({
      check: 'TC-BE-12 canary not in top-level ProxyPrivate',
      expectedResult: 'ProxyPrivate []; MUST-NOT-LEAK-PROXY-NAME not in ProxyPrivate.',
      actualResult: 'As expected — canary absent from ProxyPrivate.',
      passed: true,
    });
    await attachSummary(TestRunSummary, Responses, {
      kind: 'product',
      contractId,
      snapshot: { contractId, canary },
    });
  });

  test(titleFor('TC-BE-13', 'BA-private (businessActivity true) still fills ProxyPrivate (DB type PRIVATE_CUSTOMER)'), async ({
    Request, GeneratePayload, Responses, Endpoints, TestRunSummary,
  }) => {
    test.setTimeout(PDT_3206_TEST_TIMEOUT_MS);
    const fx: Pdt3206Fx = { Request, GeneratePayload, Responses, Endpoints };
    await test.step('Precondition: terms, price, product, POD', async () => {
      await setupPdt3206ProductBase(fx);
    });
    const { managerId, view } = await test.step('Precondition: BA-private customer with manager', async () =>
      createPdt3206BaCustomer(fx));
    expect(String(view.customerType)).toBe('PRIVATE_CUSTOMER');
    expect(view.businessActivity).toBe(true);
    const proxyName = 'BA Private Proxy';
    const created = await test.step('Precondition: product contract with managerIds', async () => {
      const proxy = buildPdt3206Proxy(GeneratePayload, {
        proxyName,
        managerIds: [managerId],
      });
      return postPdt3206ProductContract(fx, { proxy: [proxy] });
    });
    const contractId = created.id!;

    const doc = await test.step('Action: GET document-json-test', async () =>
      getPdt3206DocumentJson(fx, 'product', contractId, 1));
    expect(doc.status).toBe(200);
    const priv = proxyPrivateList(doc.json);
    const mgrs = managersList(doc.json);
    expect(priv).toHaveLength(1);
    expect(priv[0].ProxyName).toBe(proxyName);
    expect(mgrs).toEqual([]);

    const passed =
      doc.status === 200 &&
      priv.length === 1 &&
      String(priv[0]?.ProxyName) === proxyName &&
      mgrs.length === 0;
    TestRunSummary.recordCheck({
      check: 'TC-BE-13 BA-private fills ProxyPrivate (PRIVATE_CUSTOMER DB type)',
      expectedResult:
        'HTTP 200; customerType PRIVATE_CUSTOMER + businessActivity true; ProxyPrivate length 1; ProxyName BA Private Proxy; Managers [].',
      actualResult: passed
        ? `As expected — ProxyPrivate=${priv.length}, ProxyName=${String(priv[0]?.ProxyName)}, Managers=${mgrs.length}.`
        : `Not as expected — status=${doc.status}, ProxyPrivate=${priv.length}, Managers=${mgrs.length}.`,
      passed,
    });
    await attachSummary(TestRunSummary, Responses, {
      kind: 'product',
      contractId,
      snapshot: {
        contractId,
        customerType: view.customerType,
        businessActivity: view.businessActivity,
      },
    });
  });

  test(titleFor('TC-BE-14', 'GET document-json-test without versionId — 400'), async ({
    Request, GeneratePayload, Responses, Endpoints, TestRunSummary,
  }) => {
    test.setTimeout(PDT_3206_TEST_TIMEOUT_MS);
    const fx: Pdt3206Fx = { Request, GeneratePayload, Responses, Endpoints };
    await test.step('Precondition: terms, price, product, POD', async () => {
      await setupPdt3206ProductBase(fx);
    });
    await test.step('Precondition: private customer', async () => {
      await createPdt3206PrivateCustomer(fx);
    });
    const created = await test.step('Precondition: product contract', async () => {
      const proxy = buildPdt3206Proxy(GeneratePayload);
      return postPdt3206ProductContract(fx, { proxy: [proxy] });
    });
    const contractId = created.id!;

    const doc = await test.step('Action: GET document-json-test without versionId', async () =>
      getPdt3206DocumentJson(fx, 'product', contractId));
    expect(doc.status).toBe(400);
    expect(doc.status).not.toBe(200);
    const hay = errorHaystack(doc.status, doc.rawText, doc.json);
    expect(hay.toLowerCase()).toContain('versionid');
    expect(doc.json?.ProxyPrivate).toBeUndefined();

    TestRunSummary.recordCheck({
      check: 'TC-BE-14 missing versionId',
      expectedResult: 'HTTP 400; body mentions versionId; not a ContractDocumentModel.',
      actualResult: `As expected — status=${doc.status}.`,
      passed: doc.status === 400,
    });
    await attachSummary(TestRunSummary, Responses, {
      kind: 'product',
      contractId,
      snapshot: { contractId, status: doc.status },
    });
  });

  test(titleFor('TC-BE-15', 'GET document-json-test missing contract — not 200 not 403'), async ({
    Request, GeneratePayload, Responses, Endpoints, TestRunSummary,
  }) => {
    test.setTimeout(PDT_3206_TEST_TIMEOUT_MS);
    const fx: Pdt3206Fx = { Request, GeneratePayload, Responses, Endpoints };
    await test.step('Precondition: terms, price, product, POD', async () => {
      await setupPdt3206ProductBase(fx);
    });
    await test.step('Precondition: private customer', async () => {
      await createPdt3206PrivateCustomer(fx);
    });
    const created = await test.step('Precondition: one valid product contract', async () => {
      const proxy = buildPdt3206Proxy(GeneratePayload);
      return postPdt3206ProductContract(fx, { proxy: [proxy] });
    });
    const existingId = created.id!;
    const missingId = existingId + 999_999_991;

    await test.step('Precondition: missing id is not a real contract', async () => {
      const probe = await Request.get(`${Endpoints.productContract}/${missingId}`);
      expect(probe.status()).not.toBe(200);
    });

    const doc = await test.step('Action: GET document-json-test for missing id', async () =>
      getPdt3206DocumentJson(fx, 'product', missingId, 1));
    expect(doc.status).not.toBe(200);
    expect(doc.status).not.toBe(403);
    const hay = errorHaystack(doc.status, doc.rawText, doc.json);
    const code = String((doc.json as { errorCode?: string } | null)?.errorCode ?? '');
    const ok400 = doc.status === 400;
    const ok500 = doc.status === 500 && (code === 'APPLICATION_ERROR' || hay.includes('APPLICATION_ERROR'));
    expect(ok400 || ok500).toBe(true);
    expect(doc.json?.ProxyPrivate).toBeUndefined();

    TestRunSummary.recordCheck({
      check: 'TC-BE-15 missing contract id',
      expectedResult: 'Not 200, not 403; 400 or 500 APPLICATION_ERROR without document model.',
      actualResult: `As expected — status=${doc.status} errorCode=${code}.`,
      passed: true,
    });
    await attachSummary(TestRunSummary, Responses, {
      kind: 'product',
      contractId: existingId,
      snapshot: { existingId, missingId, status: doc.status },
    });
  });

  test(titleFor('TC-BE-16', 'PRIVATE proxy with managerIds — spec 400 (F4 may 201 until fix)'), async ({
    Request, GeneratePayload, Responses, Endpoints, TestRunSummary,
  }) => {
    test.setTimeout(PDT_3206_TEST_TIMEOUT_MS);
    const fx: Pdt3206Fx = { Request, GeneratePayload, Responses, Endpoints };
    await test.step('Precondition: terms, price, product, POD', async () => {
      await setupPdt3206ProductBase(fx);
    });
    await test.step('Precondition: private customer', async () => {
      await createPdt3206PrivateCustomer(fx);
    });
    const fragment = 'managerIds-[managerIds] should not be present when customer is PRIVATE_CUSTOMER';
    const posted = await test.step('Action: POST product-contract PRIVATE proxy with managerIds', async () => {
      const proxy = buildPdt3206Proxy(GeneratePayload, {
        managerIds: [999999991],
      });
      TestRunSummary.registerPayload('proxy', proxy);
      return postPdt3206ProductContract(fx, { proxy: [proxy], expectSuccess: false });
    });
    // F4: origin/dev may return 201 because getManagers checks an empty local list. Spec is 400.
    expect(posted.status, 'F4: 201 means managerIds check never fired — fail until code fix').toBe(400);
    const hay = errorHaystack(posted.status, posted.text, posted.json);
    expect(hay).toContain(fragment);

    TestRunSummary.recordCheck({
      check: 'TC-BE-16 PRIVATE managerIds rejected (spec, not F4 201)',
      expectedResult: `HTTP 400; message contains "${fragment}".`,
      actualResult:
        posted.status === 400
          ? `As expected — status=400.`
          : `Not as expected — status=${posted.status} (F4 runtime may 201).`,
      passed: posted.status === 400 && hay.includes(fragment),
    });
    await attachSummary(TestRunSummary, Responses, {
      kind: 'product',
      snapshot: { status: posted.status },
    });
  });

  test(titleFor('TC-BE-17', 'Service PRIVATE two proxies — service create message'), async ({
    Request, GeneratePayload, Responses, Endpoints, TestRunSummary,
  }) => {
    test.setTimeout(PDT_3206_TEST_TIMEOUT_MS);
    const fx: Pdt3206Fx = { Request, GeneratePayload, Responses, Endpoints };
    await test.step('Precondition: terms, price, service, POD', async () => {
      await setupPdt3206ServiceBase(fx);
    });
    await test.step('Precondition: private customer', async () => {
      await createPdt3206PrivateCustomer(fx);
    });
    const fragment = "can't have more then one proxy items when customer type is LEGAL_ENTITY";
    const posted = await test.step('Action: POST service-contract with two proxies', async () => {
      const a = buildPdt3206Proxy(GeneratePayload, {
        proxyName: 'Svc Multi A',
        proxyCustomerIdentifier: pdt3206ValidEgn(GeneratePayload),
      });
      const b = buildPdt3206Proxy(GeneratePayload, {
        proxyName: 'Svc Multi B',
        proxyCustomerIdentifier: pdt3206ValidEgn(GeneratePayload),
        proxyPowerOfAttorneyNumber: 'POASVCM2',
      });
      return postPdt3206ServiceContract(fx, { proxy: [a, b], expectSuccess: false });
    });
    expect(posted.status).toBe(400);
    expect(posted.status).not.toBe(201);
    const hay = errorHaystack(posted.status, posted.text, posted.json);
    expect(hay).toContain(fragment);
    expect(hay).not.toContain('you cant add multiple proxy when customer type is not legal entity');

    TestRunSummary.recordCheck({
      check: 'TC-BE-17 service create two-proxy message (not product fragment)',
      expectedResult: `HTTP 400; message contains "${fragment}".`,
      actualResult: `As expected — status=${posted.status}.`,
      passed: posted.status === 400 && hay.includes(fragment),
    });
    await attachSummary(TestRunSummary, Responses, {
      kind: 'service',
      snapshot: { status: posted.status },
    });
  });
});
