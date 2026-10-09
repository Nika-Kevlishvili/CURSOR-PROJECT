/**
 * Contract Resign New Flow — Dev2 API suite (Excel Backend scenarios).
 *
 * - API/sign assertions aligned with Phoenix `ProductContractResignService` (dev2).
 * - Excel Frontend sheet is out of scope here (portal UI needs a separate spec if required later).
 *
 * Requires EnergoTS `.env` with Dev2 BASE_URL / auth (run `npx playwright test --project=setup` first).
 *
 * Reference: tests/cursor/PDT-2750-missing-interim-invoice.spec.ts, contract-resign-new-flow.fixtures.ts
 */

import { test, expect } from '../../fixtures/baseFixture';
import {
    TERM_END_DATE,
    SIGNING_DATE_IN_WINDOW,
    SIGNING_DATE_BEFORE_WINDOW,
    SIGNING_DATE_AFTER_TERM_END,
    SIGNING_DATE_AT_EARLY_DEADLINE,
    WAIT_YES_FIRST_DAY_OF_NEXT_MONTH,
    EXACT_WAIT_YES_AFTER_TERM_END,
    CASE_13_F_SIGNING_DATE,
    CASE_13_EXPECTED_F_ACTIVATION,
    CASE_13_EXPECTED_B_DEACTIVATION,
    CASE_13_INITIAL_TERM_END,
    runCase13ResignChain,
    assertPodDeactivationDate,
    createSignedSupplyContractManual,
    productContractIndex,
    MSG_NOT_FOR_RESIGNING,
    MSG_MANUAL_ACTIVATION,
    MSG_ONLY_FOR_RESIGNING,
    MSG_ONLY_FOR_RESIGNING_FULL,
    MSG_INCOMPATIBLE,
    MSG_INCOMPATIBLE_FULL,
    MSG_VERSION_NOT_EXIST,
    activatePodOnContract,
    addDaysIso,
    addMonthsSameDay,
    maxIsoDate,
    todayIso,
    assertBothPodsResignedOnOld,
    assertExcelCase16ResignBlocked,
    setupExcelCase16ResignChain,
    finalizeResignTest,
    assertInformationalResignOnly,
    assertPodActivationDate,
    assertPodNotResignedOnOldContract,
    assertPodResignedOnOldContract,
    buildSignedPutPayload,
    createAndSignBaseContractB,
    createDraftContractF,
    MSG_RESIGNING_FAILED_ALL_PODS,
    MSG_RESIGNING_FAILED_ALL_PODS_FULL,
    createPastSupplyGapThenReactivateOnBase,
    entityId,
    getContractPods,
    runStandardResignChain,
    setupContractFVersionsStartingAfter,
    runSuccessfulResign,
    setupAc4ResignChain,
    sharedBaseProduct,
    sharedCustomer,
    sharedNonResignProduct,
    sharedPod,
    sharedPrice,
    sharedResignProduct,
    sharedSecondPod,
    sharedTermBase,
    sharedTermResign,
    signContractExpectError,
    signContractF,
    signContractWithEmptyPodRequests,
    createAndSignBaseContractTwoPods,
    createAndSignBaseContractPerpetuity,
    createDraftContractDifferentCustomer,
    createSecondResignDraft,
    putProductContractWithoutAuth,
    updateResignDraftBeforeSign,
    type FixtureCtx,
} from './contract-resign-new-flow.fixtures';
import { buildEditProductContractPayload } from './pdt-2815-version-validity.fixtures';

const TAG = '[CONTRACT-RESIGN-NEW-FLOW]';

test.describe(`${TAG}: Contract Resign New Flow`, { tag: ['@contract-resign', '@dev2'] }, () => {
    test.describe('Backend scenario cases', () => {
        test(`${TAG}: Case 1 - Non-resign product saves with informational message`, async ({ Request, GeneratePayload, Responses, Endpoints }) => {
            const c = { Request, GeneratePayload, Responses, Endpoints } satisfies FixtureCtx;
            await test.step('Precondition: chain without resign product flag', async () => {
                await sharedCustomer(c);
                await sharedPrice(c);
                await sharedTermBase(c);
                await sharedTermResign(c);
                await sharedPod(c);
                await sharedBaseProduct(c, 0);
                await createAndSignBaseContractB(c);
                await sharedNonResignProduct(c, 1);
            });
            let contractId: number;
            await test.step('Precondition delta: draft F uses non-resign catalog product', async () => {
                contractId = await createDraftContractF(c, { productIndex: 1 });
            });
            const baseContractId = c.Responses.productContract[0] as number;
            const podId = entityId(c.Responses.pod[0]);
            await test.step('Sign F - informational message, no POD re-sign side effects', async () => {
                const body = await signContractF(c, contractId, {
                    signingDate: SIGNING_DATE_IN_WINDOW,
                    waitExpire: 'NO',
                    supplyActivation: 'FIRST_DAY_OF_MONTH',
                    productIndex: 1,
                });
                await assertInformationalResignOnly(c, body, baseContractId, podId, MSG_NOT_FOR_RESIGNING);
            });
            await finalizeResignTest(c, { label: `${TAG}: Case 1`, oldContractId: baseContractId, newContractId: contractId, expectResignLinks: false });
        });

        test(`${TAG}: Case 2 - Manual activation blocks re-signing`, async ({ Request, GeneratePayload, Responses, Endpoints }) => {
            const c = { Request, GeneratePayload, Responses, Endpoints } satisfies FixtureCtx;
            const chain = await test.step('Precondition: resign chain', async () => runStandardResignChain(c));
            await test.step('Sign F with MANUAL supply activation — expect ClientException', async () => {
                await signContractExpectError(
                    c,
                    chain.newContractId,
                    {
                        signingDate: SIGNING_DATE_IN_WINDOW,
                        supplyActivation: 'MANUAL',
                        waitExpire: 'NO',
                        productIndex: 1,
                    },
                    MSG_MANUAL_ACTIVATION,
                );
                await assertPodNotResignedOnOldContract(c.Request, chain.baseContractId, chain.podId);
            });
            await finalizeResignTest(c, { label: `${TAG}: Case 2`, chain, expectResignLinks: false });
        });

        test(`${TAG}: Case 3 - No active predecessor contract for same product`, async ({ Request, GeneratePayload, Responses, Endpoints }) => {
            const c = { Request, GeneratePayload, Responses, Endpoints } satisfies FixtureCtx;
            await test.step('Precondition: customer + resign product only (no base contract B)', async () => {
                await sharedCustomer(c);
                await sharedPrice(c);
                await sharedTermBase(c);
                await sharedTermResign(c);
                await sharedPod(c);
                const baseId = await sharedBaseProduct(c, 0);
                await sharedResignProduct(c, baseId, { termIndex: 1 });
            });
            const contractId = await test.step('Precondition delta: draft F without signed predecessor', async () => {
                return createDraftContractF(c, { productIndex: 1 });
            });
            await test.step('Sign F - expect only-for-re-signing error', async () => {
                await signContractExpectError(
                    c,
                    contractId,
                    { signingDate: SIGNING_DATE_IN_WINDOW, productIndex: 1 },
                    [MSG_ONLY_FOR_RESIGNING, MSG_ONLY_FOR_RESIGNING_FULL],
                );
            });
            await finalizeResignTest(c, { label: `${TAG}: Case 3`, newContractId: contractId, expectResignLinks: false });
        });

        test(`${TAG}: Case 4 - Incompatible re-sign product`, async ({ Request, GeneratePayload, Responses, Endpoints }) => {
            const c = { Request, GeneratePayload, Responses, Endpoints } satisfies FixtureCtx;
            await test.step('Precondition: base contract B + incompatible resign target', async () => {
                await sharedCustomer(c);
                await sharedPrice(c);
                await sharedTermBase(c);
                await sharedTermResign(c);
                await sharedPod(c);
                const baseA = await sharedBaseProduct(c, 0);
                await createAndSignBaseContractB(c, { productIndex: 0 });
                const baseB = await sharedBaseProduct(c, 0);
                await sharedResignProduct(c, baseB, { termIndex: 1, linkTarget: true });
            });
            const contractId = await createDraftContractF(c, { productIndex: 2 });
            await test.step('Sign F - expect incompatibility error', async () => {
                await signContractExpectError(
                    c,
                    contractId,
                    { signingDate: SIGNING_DATE_IN_WINDOW, productIndex: 2 },
                    [MSG_INCOMPATIBLE, MSG_INCOMPATIBLE_FULL],
                );
            });
            await finalizeResignTest(c, { label: `${TAG}: Case 4`, newContractId: contractId, expectResignLinks: false });
        });

        test(`${TAG}: Case 5 - Signing date before re-sign window`, async ({ Request, GeneratePayload, Responses, Endpoints }) => {
            const c = { Request, GeneratePayload, Responses, Endpoints } satisfies FixtureCtx;
            const chain = await test.step('Precondition: standard resign chain', async () => runStandardResignChain(c));
            await test.step('Sign F before window — expect error', async () => {
                await signContractExpectError(
                    c,
                    chain.newContractId,
                    { signingDate: SIGNING_DATE_BEFORE_WINDOW, productIndex: 1 },
                    [MSG_ONLY_FOR_RESIGNING, MSG_ONLY_FOR_RESIGNING_FULL, MSG_RESIGNING_FAILED_ALL_PODS],
                );
                await assertPodNotResignedOnOldContract(c.Request, chain.baseContractId, chain.podId);
            });
            await finalizeResignTest(c, { label: `${TAG}: Case 5`, chain, expectResignLinks: false });
        });

        test(`${TAG}: Case 6 - Signing date after old contract term end`, async ({ Request, GeneratePayload, Responses, Endpoints }) => {
            const c = { Request, GeneratePayload, Responses, Endpoints } satisfies FixtureCtx;
            const chain = await test.step('Precondition: standard resign chain', async () => runStandardResignChain(c));
            await test.step('Sign F after term end — expect error', async () => {
                await signContractExpectError(
                    c,
                    chain.newContractId,
                    { signingDate: SIGNING_DATE_AFTER_TERM_END, productIndex: 1 },
                    [MSG_ONLY_FOR_RESIGNING, MSG_ONLY_FOR_RESIGNING_FULL, MSG_RESIGNING_FAILED_ALL_PODS],
                );
                await assertPodNotResignedOnOldContract(c.Request, chain.baseContractId, chain.podId);
            });
            await finalizeResignTest(c, { label: `${TAG}: Case 6`, chain, expectResignLinks: false });
        });

        test(`${TAG}: Case 7 - Signing date equals early re-sign deadline (inclusive)`, async ({ Request, GeneratePayload, Responses, Endpoints }) => {
            const c = { Request, GeneratePayload, Responses, Endpoints } satisfies FixtureCtx;
            const chain = await test.step('Precondition: standard resign chain', async () => runStandardResignChain(c));
            await test.step('Sign F at early deadline — expect success', async () => {
                const body = await signContractF(c, chain.newContractId, {
                    signingDate: SIGNING_DATE_AT_EARLY_DEADLINE,
                    waitExpire: 'NO',
                    supplyActivation: 'FIRST_DAY_OF_MONTH',
                    productIndex: 1,
                });
                expect(body.id).toBeTruthy();
            });
            await finalizeResignTest(c, { label: `${TAG}: Case 7`, chain, expectResignLinks: true });
        });

        test(`${TAG}: Case 8 - No eligible POD on old contract`, async ({ Request, GeneratePayload, Responses, Endpoints }) => {
            const c = { Request, GeneratePayload, Responses, Endpoints } satisfies FixtureCtx;
            await test.step('Precondition: base B without POD activation', async () => {
                await sharedCustomer(c);
                await sharedPrice(c);
                await sharedTermBase(c);
                await sharedTermResign(c);
                await sharedPod(c);
                await sharedBaseProduct(c, 0);
                await createAndSignBaseContractB(c, { activatePod: false });
                const baseId = entityId(c.Responses.product[0]);
                await sharedResignProduct(c, baseId, { termIndex: 1 });
            });
            const contractId = await createDraftContractF(c, { productIndex: 1 });
            await test.step('Sign F - expect no eligible POD / only-for-re-signing failure', async () => {
                await signContractExpectError(
                    c,
                    contractId,
                    { signingDate: SIGNING_DATE_IN_WINDOW, productIndex: 1 },
                    [MSG_ONLY_FOR_RESIGNING, MSG_ONLY_FOR_RESIGNING_FULL, MSG_RESIGNING_FAILED_ALL_PODS],
                );
            });
            await finalizeResignTest(c, { label: `${TAG}: Case 8`, newContractId: contractId, expectResignLinks: false });
        });

        test(`${TAG}: Case 9 - POD already re-signed in same version (AC6)`, async ({ Request, GeneratePayload, Responses, Endpoints }) => {
            const c = { Request, GeneratePayload, Responses, Endpoints } satisfies FixtureCtx;
            const chain = await test.step('Precondition: first successful re-sign', async () => {
                const ch = await runStandardResignChain(c);
                await runSuccessfulResign(c, ch);
                await assertPodResignedOnOldContract(c.Request, ch.baseContractId, ch.podId);
                return ch;
            });
            const secondId = await createSecondResignDraft(c, 1);
            await test.step('Second sign — AC6: no eligible POD / resign failed for all pods', async () => {
                await signContractExpectError(
                    c,
                    secondId,
                    { signingDate: SIGNING_DATE_IN_WINDOW, productIndex: 1 },
                    [MSG_RESIGNING_FAILED_ALL_PODS, MSG_RESIGNING_FAILED_ALL_PODS_FULL],
                );
                await assertPodResignedOnOldContract(c.Request, chain.baseContractId, chain.podId);
            });
            await finalizeResignTest(c, { label: `${TAG}: Case 9`, chain, expectResignLinks: true });
        });

        test(`${TAG}: Case 10 - AC7 lifecycle re-sign from contract C into D`, async ({ Request, GeneratePayload, Responses, Endpoints }) => {
            const c = { Request, GeneratePayload, Responses, Endpoints } satisfies FixtureCtx;
            test.setTimeout(20 * 60 * 1000);
            let chain = await test.step('Precondition: re-sign B â†’ F1', async () => {
                const ch = await runStandardResignChain(c);
                await runSuccessfulResign(c, ch);
                return ch;
            });
            await test.step('Precondition: deactivate POD on F1 (first new contract)', async () => {
                const deactDate = addDaysIso(SIGNING_DATE_IN_WINDOW, 5);
                await activatePodOnContract(c, productContractIndex(c, chain.newContractId), deactDate, 0, deactDate);
            });
            const contractCId = await test.step('Precondition: contract C — same customer, base product, POD active', async () => {
                const signC = addDaysIso(SIGNING_DATE_IN_WINDOW, 10);
                return createSignedSupplyContractManual(c, {
                    signingDate: signC,
                    activationDate: signC,
                    productIndex: 0,
                });
            });
            let draftD = 0;
            await test.step('Precondition: resign product + draft D from C', async () => {
                const baseId = entityId(c.Responses.product[0]);
                await sharedResignProduct(c, baseId, { termIndex: 1 });
                draftD = await createDraftContractF(c, { productIndex: 1 });
                const body = await signContractF(c, draftD, {
                    signingDate: SIGNING_DATE_IN_WINDOW,
                    waitExpire: 'NO',
                    supplyActivation: 'FIRST_DAY_OF_MONTH',
                    productIndex: 1,
                });
                expect(body.id).toBeTruthy();
                await assertPodResignedOnOldContract(c.Request, contractCId, chain.podId);
            });
            await finalizeResignTest(c, {
                label: `${TAG}: Case 10`,
                oldContractId: contractCId,
                newContractId: draftD,
                expectResignLinks: true,
                extraContractIds: [chain.baseContractId, chain.newContractId],
            });
        });

        test(`${TAG}: Case 11 - Wait Yes + first day of next month activation`, async ({ Request, GeneratePayload, Responses, Endpoints }) => {
            const c = { Request, GeneratePayload, Responses, Endpoints } satisfies FixtureCtx;
            const chain = await test.step('Precondition: resign chain', async () => runStandardResignChain(c));
            await test.step('Sign with Wait=Yes and FIRST_DAY_OF_MONTH', async () => {
                await signContractF(c, chain.newContractId, {
                    signingDate: SIGNING_DATE_IN_WINDOW,
                    waitExpire: 'YES',
                    supplyActivation: 'FIRST_DAY_OF_MONTH',
                    productIndex: 1,
                });
            });
            await test.step('Assert new POD activation first day of next month', async () => {
                await assertPodActivationDate(
                    c.Request,
                    chain.newContractId,
                    chain.podId,
                    WAIT_YES_FIRST_DAY_OF_NEXT_MONTH,
                );
            });
            await finalizeResignTest(c, { label: `${TAG}: Case 11`, chain, expectResignLinks: true });
        });

        test(`${TAG}: Case 12 - Wait Yes + exact day activation (day after term end)`, async ({ Request, GeneratePayload, Responses, Endpoints }) => {
            const c = { Request, GeneratePayload, Responses, Endpoints } satisfies FixtureCtx;
            const chain = await test.step('Precondition: resign chain', async () => runStandardResignChain(c));
            await test.step('Sign with Wait=Yes and EXACT_DATE rule', async () => {
                await signContractF(c, chain.newContractId, {
                    signingDate: SIGNING_DATE_IN_WINDOW,
                    waitExpire: 'YES',
                    supplyActivation: 'EXACT_DATE',
                    productIndex: 1,
                });
            });
            await test.step('Assert activation day after term end', async () => {
                await assertPodActivationDate(
                    c.Request,
                    chain.newContractId,
                    chain.podId,
                    EXACT_WAIT_YES_AFTER_TERM_END,
                );
            });
            await finalizeResignTest(c, { label: `${TAG}: Case 12`, chain, expectResignLinks: true });
        });

        test(`${TAG}: Case 13 - Wait Yes + first day of next month (term end ${CASE_13_INITIAL_TERM_END})`, async ({
            Request,
            GeneratePayload,
            Responses,
            Endpoints,
        }) => {
            const c = { Request, GeneratePayload, Responses, Endpoints } satisfies FixtureCtx;
            const chain = await test.step('Precondition: Case 13 chain (initial term end 2026-03-25)', async () =>
                runCase13ResignChain(c),
            );
            await test.step('Sign F with Wait=Yes and FIRST_DAY_OF_MONTH [TC-BE-25]', async () => {
                await signContractF(c, chain.newContractId, {
                    signingDate: CASE_13_F_SIGNING_DATE,
                    waitExpire: 'YES',
                    supplyActivation: 'FIRST_DAY_OF_MONTH',
                    productIndex: 1,
                });
            });
            await test.step('Assert F activation first day of month after initial term end', async () => {
                await assertPodActivationDate(
                    c.Request,
                    chain.newContractId,
                    chain.podId,
                    CASE_13_EXPECTED_F_ACTIVATION,
                );
                await assertPodDeactivationDate(
                    c.Request,
                    chain.baseContractId,
                    chain.podId,
                    CASE_13_EXPECTED_B_DEACTIVATION,
                );
            });
            await finalizeResignTest(c, { label: `${TAG}: Case 13`, chain, expectResignLinks: true });
        });

        test(`${TAG}: Case 14 - Wait No + exact (month after signing same day)`, async ({ Request, GeneratePayload, Responses, Endpoints }) => {
            const c = { Request, GeneratePayload, Responses, Endpoints } satisfies FixtureCtx;
            const chain = await test.step('Precondition: resign chain', async () => runStandardResignChain(c));
            await signContractF(c, chain.newContractId, {
                signingDate: SIGNING_DATE_IN_WINDOW,
                waitExpire: 'NO',
                supplyActivation: 'EXACT_DATE',
                productIndex: 1,
            });
            await assertPodActivationDate(
                c.Request,
                chain.newContractId,
                chain.podId,
                addMonthsSameDay(SIGNING_DATE_IN_WINDOW, 1),
            );
            await finalizeResignTest(c, { label: `${TAG}: Case 14`, chain, expectResignLinks: true });
        });

        test(`${TAG}: Case 15 - Perpetuity date before signing skips contract`, async ({ Request, GeneratePayload, Responses, Endpoints }) => {
            const c = { Request, GeneratePayload, Responses, Endpoints } satisfies FixtureCtx;
            await sharedCustomer(c);
            await sharedPrice(c);
            await sharedTermBase(c);
            await sharedTermResign(c);
            await sharedPod(c);
            const baseId = await sharedBaseProduct(c, 0);
            await createAndSignBaseContractPerpetuity(c, {
                perpetuityDate: '2025-01-01',
                signingDate: SIGNING_DATE_IN_WINDOW,
            });
            await sharedResignProduct(c, baseId, { termIndex: 1 });
            const contractId = await createDraftContractF(c, { productIndex: 1 });
            await signContractExpectError(
                c,
                contractId,
                { signingDate: SIGNING_DATE_IN_WINDOW, waitExpire: 'NO', productIndex: 1 },
                [MSG_RESIGNING_FAILED_ALL_PODS, MSG_RESIGNING_FAILED_ALL_PODS_FULL],
            );
            await finalizeResignTest(c, { label: `${TAG}: Case 15`, newContractId: contractId, expectResignLinks: false });
        });

        test(`${TAG}: Case 16 — resigning period cannot be calculated (happy-path preconditions; old B without term/dates)`, async ({
            Request,
            GeneratePayload,
            Responses,
            Endpoints,
        }) => {
            const c = { Request, GeneratePayload, Responses, Endpoints } satisfies FixtureCtx;
            const chain = await test.step(
                'Precondition: same as happy-path resign chain; contract B omits term end and initial-term dates',
                async () => setupExcelCase16ResignChain(c),
            );
            await test.step('Sign F — engine cannot derive re-signing period; no POD transfer', async () => {
                await assertExcelCase16ResignBlocked(c, chain, 'Case 16');
            });
            await finalizeResignTest(c, {
                label: `${TAG}: Case 16`,
                chain,
                expectResignLinks: false,
            });
        });

        test(`${TAG}: Case 17 - Future activation conflict without confirmation (AC4)`, async ({ Request, GeneratePayload, Responses, Endpoints }) => {
            const c = { Request, GeneratePayload, Responses, Endpoints } satisfies FixtureCtx;
            const chain = await setupAc4ResignChain(c);
            await signContractExpectError(
                c,
                chain.newContractId,
                { signingDate: SIGNING_DATE_IN_WINDOW, removeFuturePods: false, productIndex: 1 },
                [MSG_RESIGNING_FAILED_ALL_PODS, MSG_RESIGNING_FAILED_ALL_PODS_FULL],
            );
            await assertPodNotResignedOnOldContract(c.Request, chain.baseContractId, chain.podId);
            await finalizeResignTest(c, { label: `${TAG}: Case 17`, chain, expectResignLinks: false });
        });

        test(`${TAG}: Case 18 - AC4 confirm removeFuturePods=true`, async ({ Request, GeneratePayload, Responses, Endpoints }) => {
            const c = { Request, GeneratePayload, Responses, Endpoints } satisfies FixtureCtx;
            const chain = await setupAc4ResignChain(c);
            await signContractF(c, chain.newContractId, {
                signingDate: SIGNING_DATE_IN_WINDOW,
                removeFuturePods: true,
                productIndex: 1,
            });
            await assertPodResignedOnOldContract(c.Request, chain.baseContractId, chain.podId);
            await finalizeResignTest(c, { label: `${TAG}: Case 18`, chain, expectResignLinks: true });
        });

        test(`${TAG}: Case 19 - AC4 cancel without confirmation`, async ({ Request, GeneratePayload, Responses, Endpoints }) => {
            const c = { Request, GeneratePayload, Responses, Endpoints } satisfies FixtureCtx;
            const chain = await setupAc4ResignChain(c);
            await signContractExpectError(
                c,
                chain.newContractId,
                { signingDate: SIGNING_DATE_IN_WINDOW, removeFuturePods: false, productIndex: 1 },
                [MSG_RESIGNING_FAILED_ALL_PODS, MSG_RESIGNING_FAILED_ALL_PODS_FULL],
            );
            await assertPodNotResignedOnOldContract(c.Request, chain.baseContractId, chain.podId);
            await finalizeResignTest(c, { label: `${TAG}: Case 19`, chain, expectResignLinks: false });
        });

        test(`${TAG}: Case 20 - Gap in future version of old contract`, async ({ Request, GeneratePayload, Responses, Endpoints }) => {
            const c = { Request, GeneratePayload, Responses, Endpoints } satisfies FixtureCtx;
            const chain = await runStandardResignChain(c);
            const generated = (await c.GeneratePayload.contractsAndOrders.product_contract(0, 1)) as Record<string, unknown>;
            const editPayload = await buildEditProductContractPayload(c.Request, chain.baseContractId, generated, {
                forDraftV1: false,
                preserveSigningDate: true,
            });
            editPayload.savingAsNewVersion = true;
            editPayload.startDate = addDaysIso(SIGNING_DATE_IN_WINDOW, 40);
            const put = await c.Request.put(
                `${c.Endpoints.productContract}/${chain.baseContractId}?versionId=1&changeFutureVersionsPods=true`,
                { data: editPayload },
            );
            await expect(put).CheckResponse();
            await signContractExpectError(c, chain.newContractId, {
                signingDate: SIGNING_DATE_IN_WINDOW,
                productIndex: 1,
            }, MSG_RESIGNING_FAILED_ALL_PODS);
            await assertPodNotResignedOnOldContract(c.Request, chain.baseContractId, chain.podId);
            await finalizeResignTest(c, { label: `${TAG}: Case 20`, chain, expectResignLinks: false });
        });

        test(`${TAG}: Case 21 - Gap only in past version (should succeed)`, async ({ Request, GeneratePayload, Responses, Endpoints }) => {
            const c = { Request, GeneratePayload, Responses, Endpoints } satisfies FixtureCtx;
            await sharedCustomer(c);
            await sharedPrice(c);
            await sharedTermBase(c);
            await sharedTermResign(c);
            await sharedPod(c);
            const baseId = await sharedBaseProduct(c, 0);
            const baseContractId = await createAndSignBaseContractB(c);
            const contractIndex = c.Responses.productContract.length - 1;
            await createPastSupplyGapThenReactivateOnBase(c, contractIndex, 0);
            await sharedResignProduct(c, baseId, { termIndex: 1 });
            const newContractId = await createDraftContractF(c, { productIndex: 1 });
            const podId = entityId(c.Responses.pod[0]);
            const body = await signContractF(c, newContractId, {
                signingDate: SIGNING_DATE_IN_WINDOW,
                waitExpire: 'NO',
                supplyActivation: 'FIRST_DAY_OF_MONTH',
                productIndex: 1,
            });
            expect(body.id).toBeTruthy();
            await assertPodResignedOnOldContract(c.Request, baseContractId, podId);
            await finalizeResignTest(c, { label: `${TAG}: Case 21`, oldContractId: baseContractId, newContractId: newContractId, expectResignLinks: true });
        });

        test(`${TAG}: Case 22 - POD not activatable in new contract versions`, async ({ Request, GeneratePayload, Responses, Endpoints }) => {
            const c = { Request, GeneratePayload, Responses, Endpoints } satisfies FixtureCtx;
            const chain = await runStandardResignChain(c);
            await test.step('Precondition: future version on F — POD not activatable at signing date', async () => {
                const generated = (await c.GeneratePayload.contractsAndOrders.product_contract(0, 1)) as Record<
                    string,
                    unknown
                >;
                const editPayload = await buildEditProductContractPayload(c.Request, chain.newContractId, generated, {
                    forDraftV1: false,
                    preserveSigningDate: false,
                });
                editPayload.savingAsNewVersion = true;
                editPayload.startDate = addDaysIso(SIGNING_DATE_IN_WINDOW, 45);
                const put = await c.Request.put(
                    `${c.Endpoints.productContract}/${chain.newContractId}?versionId=1&changeFutureVersionsPods=true`,
                    { data: editPayload },
                );
                await expect(put).CheckResponse();
            });
            await test.step('Sign F - resign fails for all pods (no activatable POD in new versions)', async () => {
                await signContractExpectError(
                    c,
                    chain.newContractId,
                    { signingDate: SIGNING_DATE_IN_WINDOW, productIndex: 1 },
                    [MSG_RESIGNING_FAILED_ALL_PODS, MSG_RESIGNING_FAILED_ALL_PODS_FULL, MSG_VERSION_NOT_EXIST],
                );
                await assertPodNotResignedOnOldContract(c.Request, chain.baseContractId, chain.podId);
            });
            await finalizeResignTest(c, { label: `${TAG}: Case 22`, chain, expectResignLinks: false });
        });

        test(`${TAG}: Case 23 - Activation spans multiple new contract versions`, async ({ Request, GeneratePayload, Responses, Endpoints }) => {
            const c = { Request, GeneratePayload, Responses, Endpoints } satisfies FixtureCtx;
            const chain = await runStandardResignChain(c);
            const generated = (await c.GeneratePayload.contractsAndOrders.product_contract(0, 1)) as Record<string, unknown>;
            const editPayload = await buildEditProductContractPayload(c.Request, chain.newContractId, generated, {
                forDraftV1: false,
                preserveSigningDate: false,
            });
            editPayload.savingAsNewVersion = true;
            editPayload.startDate = addDaysIso(SIGNING_DATE_IN_WINDOW, -5);
            const put = await c.Request.put(
                `${c.Endpoints.productContract}/${chain.newContractId}?versionId=1&changeFutureVersionsPods=true`,
                { data: editPayload },
            );
            await expect(put).CheckResponse();
            const body = await signContractF(c, chain.newContractId, {
                signingDate: SIGNING_DATE_IN_WINDOW,
                waitExpire: 'NO',
                productIndex: 1,
            });
            expect(body.id).toBeTruthy();
            const getRes = await c.Request.get(`product-contract/${chain.newContractId}`);
            await expect(getRes).CheckResponse();
            const versions = ((await getRes.json()) as { versions?: { versionId?: number }[] }).versions ?? [];
            expect(versions.length).toBeGreaterThanOrEqual(2);
            await finalizeResignTest(c, { label: `${TAG}: Case 23`, chain, expectResignLinks: true });
        });

        test(`${TAG}: Case 24 - Open-ended old POD deactivation inheritance`, async ({ Request, GeneratePayload, Responses, Endpoints }) => {
            const c = { Request, GeneratePayload, Responses, Endpoints } satisfies FixtureCtx;
            const chain = await runStandardResignChain(c);
            await runSuccessfulResign(c, chain);
            const newPods = await getContractPods(c.Request, chain.newContractId);
            const newRow = newPods.find((p) => Number(p.podId) === chain.podId);
            expect(newRow?.activationDate).toBeTruthy();
            const oldPods = await getContractPods(c.Request, chain.baseContractId);
            const oldRow = oldPods.find((p) => Number(p.podId) === chain.podId);
            expect(oldRow?.deactivationDate).toBe(addDaysIso(String(newRow?.activationDate), -1));
            await finalizeResignTest(c, { label: `${TAG}: Case 24`, chain, expectResignLinks: true });
        });

        test(`${TAG}: Case 25 - Old POD scheduled deactivation inherited`, async ({ Request, GeneratePayload, Responses, Endpoints }) => {
            const c = { Request, GeneratePayload, Responses, Endpoints } satisfies FixtureCtx;
            await sharedCustomer(c);
            await sharedPrice(c);
            await sharedTermBase(c);
            await sharedTermResign(c);
            await sharedPod(c);
            const baseId = await sharedBaseProduct(c, 0);
            const baseId_contract = await createAndSignBaseContractB(c);
            const activationStart = maxIsoDate(addDaysIso(SIGNING_DATE_IN_WINDOW, -5), todayIso());
            const futureDeact = addDaysIso(activationStart, 60);
            await activatePodOnContract(
                c,
                c.Responses.productContract.length - 1,
                activationStart,
                0,
                futureDeact,
            );
            await sharedResignProduct(c, baseId, { termIndex: 1 });
            const newId = await createDraftContractF(c, { productIndex: 1 });
            await signContractF(c, newId, {
                signingDate: SIGNING_DATE_IN_WINDOW,
                waitExpire: 'NO',
                productIndex: 1,
            });
            const pods = await getContractPods(c.Request, baseId_contract);
            const row = pods.find((p) => Number(p.podId) === entityId(c.Responses.pod[0]));
            expect(row?.deactivationDate).toBeTruthy();
            await finalizeResignTest(c, {
                label: `${TAG}: Case 25`,
                oldContractId: baseId_contract,
                newContractId: newId,
                expectResignLinks: true,
            });
        });

        test(`${TAG}: Case 26 - Two eligible PODs transferred atomically`, async ({ Request, GeneratePayload, Responses, Endpoints }) => {
            const c = { Request, GeneratePayload, Responses, Endpoints } satisfies FixtureCtx;
            await sharedCustomer(c);
            await sharedPrice(c);
            await sharedTermBase(c);
            await sharedTermResign(c);
            await sharedPod(c);
            await sharedSecondPod(c);
            const baseId = await sharedBaseProduct(c, 0);
            await createAndSignBaseContractTwoPods(c);
            await sharedResignProduct(c, baseId, { termIndex: 1 });
            const newId = await createDraftContractF(c, { productIndex: 1, podIndices: [0, 1] });
            const baseContractId = c.Responses.productContract[0] as number;
            const podIds = [entityId(c.Responses.pod[0]), entityId(c.Responses.pod[1])];
            const body = await signContractF(c, newId, {
                signingDate: SIGNING_DATE_IN_WINDOW,
                productIndex: 1,
            });
            expect(body.id).toBeTruthy();
            await assertBothPodsResignedOnOld(c.Request, baseContractId, podIds);
            await finalizeResignTest(c, {
                label: `${TAG}: Case 26`,
                oldContractId: baseContractId,
                newContractId: newId,
                expectResignLinks: true,
            });
        });

        test(`${TAG}: Case 27 - Second resign draft fails after first F signed (AC6)`, async ({ Request, GeneratePayload, Responses, Endpoints }) => {
            const c = { Request, GeneratePayload, Responses, Endpoints } satisfies FixtureCtx;
            await sharedCustomer(c);
            await sharedPrice(c);
            await sharedTermBase(c);
            await sharedTermResign(c);
            await sharedPod(c);
            await sharedSecondPod(c);
            const baseContractId = await createAndSignBaseContractTwoPods(c);
            const baseId = entityId(c.Responses.product[0]);
            await sharedResignProduct(c, baseId, { termIndex: 1 });
            const podIds = [entityId(c.Responses.pod[0]), entityId(c.Responses.pod[1])];
            const firstId = await createDraftContractF(c, { productIndex: 1, podIndices: [0, 1] });
            await signContractF(c, firstId, { signingDate: SIGNING_DATE_IN_WINDOW, productIndex: 1 });
            await assertBothPodsResignedOnOld(c.Request, baseContractId, podIds);
            const secondId = await createSecondResignDraft(c, 1, [0, 1]);
            await signContractExpectError(c, secondId, {
                signingDate: SIGNING_DATE_IN_WINDOW,
                productIndex: 1,
            }, MSG_RESIGNING_FAILED_ALL_PODS);
            await assertBothPodsResignedOnOld(c.Request, baseContractId, podIds);
            await finalizeResignTest(c, { label: `${TAG}: Case 27`, oldContractId: (c.Responses.productContract[0] as number), newContractId: firstId, expectResignLinks: true, extraContractIds: [secondId] });
        });

        test(`${TAG}: Case 28 - Old contract shows Re-signed marker after success`, async ({ Request, GeneratePayload, Responses, Endpoints }) => {
            const c = { Request, GeneratePayload, Responses, Endpoints } satisfies FixtureCtx;
            const chain = await test.step('Precondition: successful re-sign', async () => {
                const ch = await runStandardResignChain(c);
                await updateResignDraftBeforeSign(c, ch.newContractId, 1);
                await signContractF(c, ch.newContractId, {
                    signingDate: SIGNING_DATE_IN_WINDOW,
                    waitExpire: 'NO',
                    supplyActivation: 'FIRST_DAY_OF_MONTH',
                    productIndex: 1,
                });
                return ch;
            });
            await test.step('GET old contract — POD has Re-signed marker', async () => {
                await assertPodResignedOnOldContract(c.Request, chain.baseContractId, chain.podId);
            });
            await finalizeResignTest(c, { label: `${TAG}: Case 28`, chain, expectResignLinks: true });
        });

        test(`${TAG}: Case 29 - Empty podRequests rejected on sign PUT (maps Excel selectedPodIds validation)`, async ({ Request, GeneratePayload, Responses, Endpoints }) => {
            const c = { Request, GeneratePayload, Responses, Endpoints } satisfies FixtureCtx;
            const chain = await runStandardResignChain(c);
            const result = await signContractWithEmptyPodRequests(c, chain.newContractId, {
                signingDate: SIGNING_DATE_IN_WINDOW,
                productIndex: 1,
            });
            expect([400, 422]).toContain(result.status);
            await assertPodNotResignedOnOldContract(c.Request, chain.baseContractId, chain.podId);
            await finalizeResignTest(c, { label: `${TAG}: Case 29`, chain, expectResignLinks: false });
        });

        test(`${TAG}: Case 30 - Invalid pod detail id on sign PUT`, async ({ Request, GeneratePayload, Responses, Endpoints }) => {
            const c = { Request, GeneratePayload, Responses, Endpoints } satisfies FixtureCtx;
            const chain = await runStandardResignChain(c);
            const signedPayload = await buildSignedPutPayload(c, chain.newContractId, {
                signingDate: SIGNING_DATE_IN_WINDOW,
                productIndex: 1,
            });
            signedPayload.podRequests = [
                {
                    billingGroupId: 1,
                    productContractPointOfDeliveries: [{ pointOfDeliveryDetailId: 999999999, dealNumber: null }],
                },
            ];
            const response = await c.Request.put(
                `${c.Endpoints.productContract}/${chain.newContractId}?versionId=1&changeFutureVersionsPods=false`,
                { data: signedPayload },
            );
            expect([400, 404, 422]).toContain(response.status());
            await assertPodNotResignedOnOldContract(c.Request, chain.baseContractId, chain.podId);
            await finalizeResignTest(c, { label: `${TAG}: Case 30`, chain, expectResignLinks: false });
        });

        test(`${TAG}: Case 31 - Token without contract-edit permission (401/403)`, async ({ Request, GeneratePayload, Responses, Endpoints }) => {
            const c = { Request, GeneratePayload, Responses, Endpoints } satisfies FixtureCtx;
            const chain = await runStandardResignChain(c);
            const payload = await buildSignedPutPayload(c, chain.newContractId, {
                signingDate: SIGNING_DATE_IN_WINDOW,
                productIndex: 1,
            });
            const anon = await putProductContractWithoutAuth(chain.newContractId, payload);
            expect([401, 403]).toContain(anon.status);
            await finalizeResignTest(c, { label: `${TAG}: Case 31`, chain, expectResignLinks: false });
        });

        test(`${TAG}: Case 32 - Re-signing across different customers`, async ({ Request, GeneratePayload, Responses, Endpoints }) => {
            const c = { Request, GeneratePayload, Responses, Endpoints } satisfies FixtureCtx;
            const chain = await runStandardResignChain(c);
            const otherCustomerDraft = await createDraftContractDifferentCustomer(c);
            await signContractExpectError(c, otherCustomerDraft, {
                signingDate: SIGNING_DATE_IN_WINDOW,
                productIndex: 1,
                customerIndex: 1,
            }, MSG_ONLY_FOR_RESIGNING);
            await finalizeResignTest(c, {
                label: `${TAG}: Case 32`,
                oldContractId: c.Responses.productContract[0] as number,
                newContractId: otherCustomerDraft,
                expectResignLinks: false,
            });
        });

        test(`${TAG}: Case 33 - READY status blocks re-sign (TC-BE-2)`, async ({ Request, GeneratePayload, Responses, Endpoints }) => {
            const c = { Request, GeneratePayload, Responses, Endpoints } satisfies FixtureCtx;
            const chain = await runStandardResignChain(c);
            const draftId = chain.newContractId;
            await signContractExpectError(
                c,
                draftId,
                {
                    signingDate: SIGNING_DATE_IN_WINDOW,
                    status: 'READY',
                    subStatus: 'READY',
                    productIndex: 1,
                },
                MSG_ONLY_FOR_RESIGNING,
            );
            await assertPodNotResignedOnOldContract(c.Request, chain.baseContractId, chain.podId);
            await finalizeResignTest(c, { label: `${TAG}: Case 33`, chain, expectResignLinks: false });
        });

        test(`${TAG}: Case 36 - Signing date outside all F version ranges (Step 2 / TC-BE-4)`, async ({
            Request,
            GeneratePayload,
            Responses,
            Endpoints,
        }) => {
            const c = { Request, GeneratePayload, Responses, Endpoints } satisfies FixtureCtx;
            const chain = await test.step('Precondition: standard resign chain', async () => runStandardResignChain(c));
            await test.step('Precondition: F V1–V3 all start after signing date S', async () => {
                await setupContractFVersionsStartingAfter(c, chain.newContractId);
            });
            await test.step('GET F — at least three versions', async () => {
                const getRes = await c.Request.get(`product-contract/${chain.newContractId}`);
                await expect(getRes).CheckResponse();
                const versions = ((await getRes.json()) as { versions?: unknown[] }).versions ?? [];
                expect(versions.length).toBeGreaterThanOrEqual(3);
            });
            await test.step('Sign F with S in window — version missing, no POD re-sign', async () => {
                const body = await signContractF(c, chain.newContractId, {
                    signingDate: SIGNING_DATE_IN_WINDOW,
                    waitExpire: 'NO',
                    supplyActivation: 'FIRST_DAY_OF_MONTH',
                    productIndex: 1,
                });
                await assertInformationalResignOnly(c, body, chain.baseContractId, chain.podId, MSG_VERSION_NOT_EXIST);
            });
            await finalizeResignTest(c, { label: `${TAG}: Case 36`, chain, expectResignLinks: false });
        });

    });
});
