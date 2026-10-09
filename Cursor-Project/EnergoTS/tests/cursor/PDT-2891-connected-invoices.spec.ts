/**
 * PDT-2891 — Connected invoices (Story V0.2 Backend).
 *
 * Maps 1:1 to: Cursor-Project/test_cases/Backend/Invoice_debit_credit_note_parent_connection_PDT_2891.md
 *
 * Reference spec(s):
 * - tests/cursor/pdt-2891-connected-invoices.fixtures.ts
 * - tests/cursor/pdt-2872-minimal-interim-payment.fixtures.ts
 * - tests/cursor/rps-pod-invoice-due-date.fixtures.ts
 * - tests/billing/forVolumes/deduction.spec.ts (REG-718)
 * - PDT-3045 regression: correction on deducted volume host (same spec)
 */
import { test, expect, finalizeTestRunSummary } from './cursor-test.fixtures';
import {
  JIRA_KEY,
  type Pdt2891Fx,
  readConnectedInvoiceIds,
  assertConnectedContains,
  assertConnectedExcludes,
  assertConnectedLength,
  assertBidirectionalConnected,
  getInvoiceBody,
  runStandardVolumeInvoice,
  runStandardBillingInvoice,
  runTwoStandardBillingParents,
  runInterimDeductionLink,
  assertInterimDeductionConnected,
  runManualInterimThenDeduction,
  runMixedManualAndStandardInterimDeduction,
  createManualCreditOrDebitNote,
  startManualNoteAndGetCdId,
  runInvoiceCorrection,
  runInvoiceReversal,
  PDT_3045_JIRA_KEY,
  runDeductedVolumeCorrectionConnectedFlow,
  assertDeductedVolumeCorrectionConnections,
} from './pdt-2891-connected-invoices.fixtures';
import {
  pollInterimInvoicesPositive,
  startBillingRun,
  createManualInterimBillingRun,
  preconditionProductContractForManual,
  resolvePdt2872Amounts,
} from './pdt-2872-minimal-interim-payment.fixtures';

const JIRA_TITLE = 'Missing connection between Invoice and Debit/Credit note';

function attachSummary(
  TestRunSummary: Parameters<typeof finalizeTestRunSummary>[0],
  Responses: Parameters<typeof finalizeTestRunSummary>[1],
  tcId: string,
  snapshot: Record<string, unknown>,
  passed: boolean,
  check: string,
  expectedResult: string,
  actualResult: string,
  jiraKey: string = JIRA_KEY,
): void {
  TestRunSummary.recordCheck({
    check,
    expectedResult,
    actualResult,
    passed,
  });
  finalizeTestRunSummary(TestRunSummary, Responses, {
    jiraKey,
    relevantEntityKeys: ['customer', 'pod', 'productContract', 'billingRun', 'invoice'],
    snapshot: { tcId, ...snapshot },
  });
}

test.describe(
  `[${JIRA_KEY}]: ${JIRA_TITLE}`,
  { tag: ['@billing', '@pdt-2891', '@pdt-3045', '@dev', '@connected-invoices'] },
  () => {
    test.describe.configure({ mode: 'parallel' });

    test(
      `[${JIRA_KEY}] TC-BE-1: Interim deduction — interim ↔ volume host Connected invoices`,
      async ({ Request, GeneratePayload, Responses, Endpoints, TestRunSummary }) => {
        test.setTimeout(25 * 60 * 1000);
        const fx: Pdt2891Fx = { Request, GeneratePayload, Responses, Endpoints };

        const outcome = await test.step('Precondition: interim + volume deduction chain', async () =>
          runInterimDeductionLink(fx),
        );

        const { interimInvoiceId, volumeHostInvoiceId, volumeDocumentType } = outcome;

        await test.step('Assert interim ↔ volume host (2 invoices)', async () => {
          await assertInterimDeductionConnected(
            Request,
            [interimInvoiceId],
            volumeHostInvoiceId,
            'TC-BE-1',
          );
        });

        await test.step('Attach test run summary', async () => {
          attachSummary(
            TestRunSummary,
            Responses,
            'TC-BE-1',
            { interimInvoiceId, volumeHostInvoiceId, volumeDocumentType },
            true,
            '§3.1 interim ↔ volume host Connected invoices after deduction',
            'interim lists volume host; volume host lists interim',
            `interim=${interimInvoiceId} volumeHost=${volumeHostInvoiceId} (${volumeDocumentType})`,
          );
        });
      },
    );

    test(
      `[${JIRA_KEY}] TC-BE-2: Multiple interims — each connected to single volume host`,
      async ({ Request, GeneratePayload, Responses, Endpoints, TestRunSummary }) => {
        test.setTimeout(35 * 60 * 1000);
        const fx: Pdt2891Fx = { Request, GeneratePayload, Responses, Endpoints };
        const amounts = await resolvePdt2872Amounts(Request);

        const outcome = await test.step('Precondition: two interims + volume deduction (REG-718)', async () =>
          runInterimDeductionLink(fx, [amounts.exclFor500, amounts.exclFor500]),
        );

        const interimIds = outcome.interimInvoiceIds;
        expect(interimIds.length, 'two interim invoices').toBeGreaterThanOrEqual(2);

        await test.step('Assert each interim ↔ single volume host', async () => {
          await assertInterimDeductionConnected(
            Request,
            interimIds,
            outcome.volumeHostInvoiceId,
            'TC-BE-2',
          );
        });

        const volumeConnected = readConnectedInvoiceIds(
          await getInvoiceBody(Request, outcome.volumeHostInvoiceId),
        );

        await test.step('Attach test run summary', async () => {
          attachSummary(
            TestRunSummary,
            Responses,
            'TC-BE-2',
            { interimIds, ...outcome },
            volumeConnected.length >= interimIds.length,
            '§3.1 volume host lists all interims; each interim lists volume host',
            'volume host connected includes every interim id',
            `volumeHost connected=[${volumeConnected.join(',')}] (${outcome.volumeDocumentType})`,
          );
        });
      },
    );

    test(
      `[${JIRA_KEY}] TC-BE-3: Standard billing without interim deduction — no Connected invoices`,
      async ({ Request, GeneratePayload, Responses, Endpoints, TestRunSummary }) => {
        test.setTimeout(18 * 60 * 1000);
        const fx: Pdt2891Fx = { Request, GeneratePayload, Responses, Endpoints };

        const volumeInvoiceId = await runStandardVolumeInvoice(fx);
        const body = await getInvoiceBody(Request, volumeInvoiceId);
        assertConnectedLength(body, 0, 'volume-only invoice');

        await test.step('Attach test run summary', async () => {
          attachSummary(
            TestRunSummary,
            Responses,
            'TC-BE-3',
            { volumeInvoiceId, connectedCount: 0 },
            readConnectedInvoiceIds(body).length === 0,
            '§3.1 no deduction → empty Connected invoices',
            'Connected invoices length = 0',
            `length=${readConnectedInvoiceIds(body).length}`,
          );
        });
      },
    );

    test(
      `[${JIRA_KEY}] TC-BE-4: Manual interim at creation — Connected invoices empty`,
      async ({ Request, GeneratePayload, Responses, Endpoints, TestRunSummary }) => {
        test.setTimeout(18 * 60 * 1000);
        const fx: Pdt2891Fx = { Request, GeneratePayload, Responses, Endpoints };
        const amounts = await resolvePdt2872Amounts(Request);

        await preconditionProductContractForManual(
          fx as Parameters<typeof preconditionProductContractForManual>[0],
        );
        const manualBrId = await createManualInterimBillingRun(
          fx as Parameters<typeof createManualInterimBillingRun>[0],
          amounts.exclFor500,
        );
        await startBillingRun(Request, manualBrId);
        const poll = await pollInterimInvoicesPositive(Request, manualBrId, 1);
        const body = await getInvoiceBody(Request, poll.invoiceId);
        assertConnectedLength(body, 0, 'manual interim at creation');

        await test.step('Attach test run summary', async () => {
          attachSummary(
            TestRunSummary,
            Responses,
            'TC-BE-4',
            { manualInterimId: poll.invoiceId },
            true,
            '§3.2 manual interim has no links at creation',
            'Connected invoices length = 0',
            `length=${readConnectedInvoiceIds(body).length}`,
          );
        });
      },
    );

    test(
      `[${JIRA_KEY}] TC-BE-5: Manual interim after standard deduction — interim ↔ volume host`,
      async ({ Request, GeneratePayload, Responses, Endpoints, TestRunSummary }) => {
        test.setTimeout(25 * 60 * 1000);
        const fx: Pdt2891Fx = { Request, GeneratePayload, Responses, Endpoints };

        const outcome = await runManualInterimThenDeduction(fx);
        const { interimInvoiceId, volumeHostInvoiceId, volumeDocumentType } = outcome;

        await test.step('Assert interim ↔ volume host', async () => {
          await assertInterimDeductionConnected(
            Request,
            [interimInvoiceId],
            volumeHostInvoiceId,
            'TC-BE-5',
          );
        });

        await test.step('Attach test run summary', async () => {
          attachSummary(
            TestRunSummary,
            Responses,
            'TC-BE-5',
            outcome,
            true,
            '§3.2 manual interim post-deduction same as §3.1 (2 invoices)',
            'interim ↔ volume host',
            `ids: ${interimInvoiceId}, ${volumeHostInvoiceId} (${volumeDocumentType})`,
          );
        });
      },
    );

    test(
      `[${JIRA_KEY}] TC-BE-17: Mixed manual + standard interim — both Connected to volume host`,
      async ({ Request, GeneratePayload, Responses, Endpoints, TestRunSummary }) => {
        test.setTimeout(40 * 60 * 1000);
        const fx: Pdt2891Fx = { Request, GeneratePayload, Responses, Endpoints };

        const outcome = await test.step(
          'Precondition: standard + manual interim + single FOR_VOLUMES deduction',
          async () => runMixedManualAndStandardInterimDeduction(fx),
        );

        const { standardInterimId, manualInterimId, interimInvoiceIds, volumeHostInvoiceId } =
          outcome;
        expect(interimInvoiceIds.length, 'two interim invoices deducted').toBe(2);

        await test.step('Assert each interim ↔ volume host (mixed sources)', async () => {
          await assertInterimDeductionConnected(
            Request,
            interimInvoiceIds,
            volumeHostInvoiceId,
            'TC-BE-17',
          );
        });

        const volumeConnected = readConnectedInvoiceIds(
          await getInvoiceBody(Request, volumeHostInvoiceId),
        );

        await test.step('Attach test run summary', async () => {
          attachSummary(
            TestRunSummary,
            Responses,
            'TC-BE-17',
            { standardInterimId, manualInterimId, ...outcome },
            volumeConnected.length >= 2,
            '§3.1+§3.2 volume host lists standard + manual interim; each interim lists volume host',
            'both interim types connected after one FOR_VOLUMES deduction',
            `standard=${standardInterimId} manual=${manualInterimId} volumeHost=${volumeHostInvoiceId} connected=[${volumeConnected.join(',')}]`,
          );
        });
      },
    );

    test(
      `[${JIRA_KEY}] TC-BE-6: Manual credit note — bidirectional Connected invoices (single parent)`,
      async ({ Request, GeneratePayload, Responses, Endpoints, TestRunSummary }) => {
        test.setTimeout(40 * 60 * 1000);
        const fx: Pdt2891Fx = { Request, GeneratePayload, Responses, Endpoints };

        const parentInvoiceId = await runStandardVolumeInvoice(fx);
        const { billingRunId } = await createManualCreditOrDebitNote(fx, [parentInvoiceId], 'CREDIT_NOTE');
        const cdNoteId = await startManualNoteAndGetCdId(fx, billingRunId, 'CREDIT_NOTE');

        await assertBidirectionalConnected(Request, parentInvoiceId, cdNoteId, 'TC-BE-6');

        await test.step('Attach test run summary', async () => {
          attachSummary(
            TestRunSummary,
            Responses,
            'TC-BE-6',
            { parentInvoiceId, cdNoteId },
            true,
            '§3.3 manual CREDIT_NOTE bidirectional link',
            'parent ↔ C/D in Connected invoices',
            `parent=${parentInvoiceId} cd=${cdNoteId}`,
          );
        });
      },
    );

    test(
      `[${JIRA_KEY}] TC-BE-7: Manual debit note — multiple parent invoices on one C/D note`,
      async ({ Request, GeneratePayload, Responses, Endpoints, TestRunSummary }) => {
        test.setTimeout(35 * 60 * 1000);
        const fx: Pdt2891Fx = { Request, GeneratePayload, Responses, Endpoints };

        const { parentInvoiceId1: parent1, parentInvoiceId2: parent2 } =
          await runTwoStandardBillingParents(fx);

        const { billingRunId } = await createManualCreditOrDebitNote(
          fx,
          [parent1, parent2],
          'DEBIT_NOTE',
        );
        const cdNoteId = await startManualNoteAndGetCdId(fx, billingRunId, 'DEBIT_NOTE');

        const cdBody = await getInvoiceBody(Request, cdNoteId);
        assertConnectedContains(cdBody, [parent1, parent2], 'multi-parent C/D');
        assertConnectedContains(await getInvoiceBody(Request, parent1), [cdNoteId], 'parent1');
        assertConnectedContains(await getInvoiceBody(Request, parent2), [cdNoteId], 'parent2');

        await test.step('Attach test run summary', async () => {
          attachSummary(
            TestRunSummary,
            Responses,
            'TC-BE-7',
            { parent1, parent2, cdNoteId },
            true,
            '§3.3 DEBIT_NOTE lists both parents; each parent lists C/D',
            'C/D connected to parent1 and parent2',
            `cd connected=[${readConnectedInvoiceIds(cdBody).join(',')}]`,
          );
        });
      },
    );

    test(
      `[${JIRA_KEY}] TC-BE-11: Correction — reversal invoice Connected to original standard invoice`,
      async ({ Request, GeneratePayload, Responses, Endpoints, TestRunSummary }) => {
        test.setTimeout(25 * 60 * 1000);
        const fx: Pdt2891Fx = { Request, GeneratePayload, Responses, Endpoints };

        const originalStandardId = await runStandardVolumeInvoice(fx);
        const { reversalInvoiceId } = await runInvoiceCorrection(fx, originalStandardId);
        expect(reversalInvoiceId, 'reversal invoice from correction').toBeTruthy();

        await assertBidirectionalConnected(
          Request,
          reversalInvoiceId!,
          originalStandardId,
          'TC-BE-11',
        );

        await test.step('Attach test run summary', async () => {
          attachSummary(
            TestRunSummary,
            Responses,
            'TC-BE-11',
            { originalStandardId, reversalInvoiceId },
            true,
            '§3.4 reversal ↔ original standard',
            'bidirectional Connected invoices',
            `original=${originalStandardId} reversal=${reversalInvoiceId}`,
          );
        });
      },
    );

    test(
      `[${JIRA_KEY}] TC-BE-12: Correction — new correction C/D Connected to original (not reversal)`,
      async ({ Request, GeneratePayload, Responses, Endpoints, TestRunSummary }) => {
        test.setTimeout(25 * 60 * 1000);
        const fx: Pdt2891Fx = { Request, GeneratePayload, Responses, Endpoints };

        const originalStandardId = await runStandardVolumeInvoice(fx);
        const { reversalInvoiceId, correctionCdId } = await runInvoiceCorrection(fx, originalStandardId);
        expect(correctionCdId, 'correction C/D id').toBeTruthy();

        const cdBody = await getInvoiceBody(Request, correctionCdId!);
        assertConnectedContains(cdBody, [originalStandardId], 'correction C/D');
        assertConnectedContains(
          await getInvoiceBody(Request, originalStandardId),
          [correctionCdId!],
          'original includes correction C/D',
        );
        if (reversalInvoiceId) {
          assertConnectedExcludes(cdBody, [reversalInvoiceId], 'correction C/D must not link reversal');
        }

        await test.step('Attach test run summary', async () => {
          attachSummary(
            TestRunSummary,
            Responses,
            'TC-BE-12',
            { originalStandardId, correctionCdId, reversalInvoiceId },
            true,
            '§3.4 correction C/D → original only',
            'Connected to originalStandardId; not reversal',
            `cd connected=[${readConnectedInvoiceIds(cdBody).join(',')}]`,
          );
        });
      },
    );

    test(
      `[${JIRA_KEY}] TC-BE-13: Correction after manual credit — old C/D Connected to original (not correction C/D)`,
      async ({ Request, GeneratePayload, Responses, Endpoints, TestRunSummary }) => {
        test.setTimeout(30 * 60 * 1000);
        const fx: Pdt2891Fx = { Request, GeneratePayload, Responses, Endpoints };

        const originalStandardId = await runStandardVolumeInvoice(fx);
        const manual = await createManualCreditOrDebitNote(fx, [originalStandardId], 'CREDIT_NOTE');
        const oldCdId = await startManualNoteAndGetCdId(fx, manual.billingRunId, 'CREDIT_NOTE');

        const { correctionCdId } = await runInvoiceCorrection(fx, originalStandardId);
        const oldCdBody = await getInvoiceBody(Request, oldCdId);
        assertConnectedContains(oldCdBody, [originalStandardId], 'old manual C/D after correction');
        if (correctionCdId) {
          assertConnectedExcludes(oldCdBody, [correctionCdId], 'no C/D ↔ C/D');
        }

        await test.step('Attach test run summary', async () => {
          attachSummary(
            TestRunSummary,
            Responses,
            'TC-BE-13',
            { originalStandardId, oldCdId, correctionCdId },
            true,
            '§3.4 manual credit then correction — old C/D → original',
            'old manual C/D Connected to original; not new correction C/D',
            `oldCd connected=[${readConnectedInvoiceIds(oldCdBody).join(',')}]`,
          );
        });
      },
    );

    test(
      `[${JIRA_KEY}] TC-BE-14: Invoice reversal — cancellation invoice Connected to original`,
      async ({ Request, GeneratePayload, Responses, Endpoints, TestRunSummary }) => {
        test.setTimeout(22 * 60 * 1000);
        const fx: Pdt2891Fx = { Request, GeneratePayload, Responses, Endpoints };

        const originalInvoiceId = await runStandardVolumeInvoice(fx);
        const { cancellationInvoiceId } = await runInvoiceReversal(fx, originalInvoiceId);

        await assertBidirectionalConnected(
          Request,
          cancellationInvoiceId,
          originalInvoiceId,
          'TC-BE-14',
        );

        await test.step('Attach test run summary', async () => {
          attachSummary(
            TestRunSummary,
            Responses,
            'TC-BE-14',
            { originalInvoiceId, cancellationInvoiceId },
            true,
            '§3.5 cancellation ↔ original',
            'bidirectional Connected invoices',
            `original=${originalInvoiceId} cancellation=${cancellationInvoiceId}`,
          );
        });
      },
    );

    test(
      `[${JIRA_KEY}] TC-BE-15: Invoice reversal after manual credit — prior links preserved on original`,
      async ({ Request, GeneratePayload, Responses, Endpoints, TestRunSummary }) => {
        test.setTimeout(28 * 60 * 1000);
        const fx: Pdt2891Fx = { Request, GeneratePayload, Responses, Endpoints };

        const originalInvoiceId = await runStandardVolumeInvoice(fx);
        const manual = await createManualCreditOrDebitNote(fx, [originalInvoiceId], 'CREDIT_NOTE');
        const priorCdId = await startManualNoteAndGetCdId(fx, manual.billingRunId, 'CREDIT_NOTE');

        const baseline = new Set(readConnectedInvoiceIds(await getInvoiceBody(Request, originalInvoiceId)));
        expect(baseline.has(priorCdId), 'baseline includes prior C/D').toBeTruthy();

        const { cancellationInvoiceId } = await runInvoiceReversal(fx, originalInvoiceId);
        const afterBody = await getInvoiceBody(Request, originalInvoiceId);
        const afterIds = new Set(readConnectedInvoiceIds(afterBody));

        for (const id of Array.from(baseline)) {
          expect(afterIds.has(id), `prior link ${id} preserved`).toBeTruthy();
        }
        expect(afterIds.has(cancellationInvoiceId), 'cancellation added').toBeTruthy();

        await test.step('Attach test run summary', async () => {
          attachSummary(
            TestRunSummary,
            Responses,
            'TC-BE-15',
            { originalInvoiceId, priorCdId, cancellationInvoiceId },
            true,
            '§3.5 manual credit then reversal — prior links + cancellation',
            'baseline S ∪ {cancellationInvoiceId}',
            `after=[${Array.from(afterIds).join(',')}]`,
          );
        });
      },
    );

    test(
      `[${PDT_3045_JIRA_KEY}] ${PDT_3045_JIRA_KEY}: Correction on deducted volume invoice — Connected invoices`,
      async ({ Request, GeneratePayload, Responses, Endpoints, TestRunSummary }) => {
        test.setTimeout(40 * 60 * 1000);
        const fx: Pdt2891Fx = { Request, GeneratePayload, Responses, Endpoints };

        const outcome = await test.step(
          'Precondition: interim deduction then INVOICE_CORRECTION on volume host (TC-BE-1 chain)',
          async () => runDeductedVolumeCorrectionConnectedFlow(fx),
        );

        const {
          interimInvoiceId,
          deductedVolumeInvoiceId,
          volumeDocumentType,
          correctionCreditInvoiceId,
          correctionDebitInvoiceId,
        } = outcome;

        await test.step('Assert baseline interim ↔ deducted volume', async () => {
          await assertInterimDeductionConnected(
            Request,
            [interimInvoiceId],
            deductedVolumeInvoiceId,
            'PDT-3045',
          );
        });

        await test.step('Assert PDT-3045 Connected invoices after volume correction', async () => {
          await assertDeductedVolumeCorrectionConnections(
            Request,
            {
              interimInvoiceId,
              deductedVolumeInvoiceId,
              correctionCreditInvoiceId,
              correctionDebitInvoiceId,
            },
            'PDT-3045',
          );
        });

        await test.step('Attach test run summary', async () => {
          attachSummary(
            TestRunSummary,
            Responses,
            'PDT-3045',
            outcome,
            volumeDocumentType === 'DEBIT_NOTE' || volumeDocumentType === 'CREDIT_NOTE',
            'Connected invoices after INVOICE_CORRECTION on deducted volume host',
            'Interim lists correction credit, correction debit, deducted volume; deducted volume and both correction legs list interim',
            `interim=${interimInvoiceId} volumeHost=${deductedVolumeInvoiceId} (${volumeDocumentType}) correctionCredit=${correctionCreditInvoiceId} correctionDebit=${correctionDebitInvoiceId}`,
            PDT_3045_JIRA_KEY,
          );
        });
      },
    );

  },
);
