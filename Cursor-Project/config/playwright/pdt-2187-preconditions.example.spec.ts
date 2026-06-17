/**
 * PDT-2187 — example: one isolated precondition per test (your own assertions go here).
 * Run: npx playwright test config/playwright/pdt-2187-preconditions.example.spec.ts -c config/playwright/playwright.config.ts
 */
import { test } from '../../EnergoTS/fixtures/baseFixture';
import {
  preconditionExternalOutgoing,
  preconditionInvoice,
  preconditionLpf,
  preconditionReschedulingShort,
  readNewestExportFile,
  resetPdt2187BillingCache,
  triggerPdt2187ExportJob,
} from './pdt-2187-preconditions';

test.describe('[PDT-2187] isolated preconditions (no assertions)', { tag: ['@pdt-2187', '@dev'] }, () => {
  test.beforeEach(() => resetPdt2187BillingCache());

  test('external outgoing — own channel', async ({ Request, GeneratePayload, Responses, Endpoints }) => {
    const fx = { Request, GeneratePayload, Responses, Endpoints };
    const pack = await preconditionExternalOutgoing(fx);
    await triggerPdt2187ExportJob(fx);
    const { filePath, lines } = readNewestExportFile(pack.channel.exportDir, '.txt');
    console.log(JSON.stringify({ pack, exportFile: filePath, lineCount: lines.length }, null, 2));
  });

  test('invoice — own channel', async ({ Request, GeneratePayload, Responses, Endpoints }) => {
    const fx = { Request, GeneratePayload, Responses, Endpoints };
    const pack = await preconditionInvoice(fx);
    await triggerPdt2187ExportJob(fx);
    console.log(JSON.stringify(pack, null, 2));
  });

  test('LPF — own channel', async ({ Request, GeneratePayload, Responses, Endpoints }) => {
    test.setTimeout(25 * 60 * 1000);
    const fx = { Request, GeneratePayload, Responses, Endpoints };
    const pack = await preconditionLpf(fx);
    await triggerPdt2187ExportJob(fx);
    console.log(JSON.stringify(pack, null, 2));
  });

  test('rescheduling short — own channel', async ({ Request, GeneratePayload, Responses, Endpoints }) => {
    test.setTimeout(20 * 60 * 1000);
    const fx = { Request, GeneratePayload, Responses, Endpoints };
    const pack = await preconditionReschedulingShort(fx);
    await triggerPdt2187ExportJob(fx);
    console.log(JSON.stringify(pack, null, 2));
  });
});
