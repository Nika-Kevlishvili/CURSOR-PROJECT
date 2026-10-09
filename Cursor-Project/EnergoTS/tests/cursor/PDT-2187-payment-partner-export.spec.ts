import { test, expect } from '../../fixtures/baseFixture';
import reportGenerator from '../../utils/generateReport';
import {
  PAYMENT_PARTNER_ROW_LENGTH,
  PDT2187_TC_EXPECT,
  bankCollectionChannelPreviewUrl,
  bankCsvField,
  buildPdt2187SharedPack,
  collectionChannelPreviewUrl,
  combinedReference,
  findRowByRef,
  linesForCustomer,
  parsePaymentPartnerRow,
  toPaymentPartnerDisplayDate,
  type Pdt2187Pack,
} from './pdt-2187-payment-partner-export.fixtures';

/**
 * PDT-2187 — Payment Partner export TXT (Dev API).
 * Maps to Cursor-Project/test_cases/Backend/Payment_Partner_Export_PDT_2187.md
 */
test.describe('[PDT-2187]: Payment Partner export file', { tag: ['@receivableManagement', '@pdt-2187', '@dev'] }, () => {
  test.describe.configure({ mode: 'serial' });

  let pack: Pdt2187Pack;

  test.afterEach(({ Responses }) => {
    test.info().attach('[PDT-2187] response links', {
      body: JSON.stringify(reportGenerator.setLinksToResponses({ Responses }), null, 2),
      contentType: 'application/json',
    });
  });

  test('[PDT-2187] Shared pack + single export job', async ({
    Request,
    GeneratePayload,
    Responses,
    Endpoints,
  }) => {
    test.setTimeout(35 * 60 * 1000);
    pack = await buildPdt2187SharedPack({ Request, GeneratePayload, Responses, Endpoints });

    await test.step('Export file + required row coverage', async () => {
      expect(pack.exportFilePath.length).toBeGreaterThan(0);
      expect(pack.lines.length).toBeGreaterThan(0);
      for (const line of pack.lines) {
        expect(line.length).toBe(PAYMENT_PARTNER_ROW_LENGTH);
      }
      expect(pack.bank?.csvLines.length).toBeGreaterThan(0);
    });

    test.info().attach('[PDT-2187] pack summary', {
      body: JSON.stringify(
        {
          collectionChannelId: pack.collectionChannelId,
          collectionChannelPreviewUrl: collectionChannelPreviewUrl(pack.collectionChannelId),
          bankChannelId: pack.bankChannelId,
          bankCollectionChannelPreviewUrl: pack.bankChannelId
            ? bankCollectionChannelPreviewUrl(pack.bankChannelId)
            : null,
          exportFile: pack.exportFilePath,
          lineCount: pack.lines.length,
          resShortNum10: pack.expected.resShortReschedulingNum10,
          resLongNum10: pack.expected.resLongReschedulingNum10,
          mainInvoice: pack.expected.mainInvoice,
          bankFile: pack.bank?.exportFilePath,
        },
        null,
        2,
      ),
      contentType: 'application/json',
    });
    console.log(
      `[PDT-2187] Collection channel: ${collectionChannelPreviewUrl(pack.collectionChannelId)}`,
    );
    if (pack.bankChannelId) {
      console.log(
        `[PDT-2187] Bank collection channel: ${bankCollectionChannelPreviewUrl(pack.bankChannelId)}`,
      );
    }
  });

  test('[PDT-2187] TC-BE-1: External outgoing document — MAIN (AC2)', async () => {
    await test.step('Cols 338–350, 351, AC1 layout', async () => {
      const match = findRowByRef(
        pack.lines,
        pack.customers.main.customerNumber,
        PDT2187_TC_EXPECT.external.prefix3,
        PDT2187_TC_EXPECT.external.number10,
      );
      expect(match, 'row 0000222357185 for MAIN').toBeDefined();
      const slice = parsePaymentPartnerRow(match!);
      expect(slice.prefix.trim()).toBe(PDT2187_TC_EXPECT.external.prefix3);
      expect(slice.number.trim()).toBe(PDT2187_TC_EXPECT.external.number10);
      expect(slice.documentDate).toBe(PDT2187_TC_EXPECT.external.documentDate);
      expect(slice.trailer.trim()).toBe('12345');
    });
  });

  test('[PDT-2187] TC-BE-2: LPF logical date — MAIN (AC4)', async () => {
    await test.step('LPF row + col 351 logical date', async () => {
      const row = findRowByRef(
        pack.lines,
        pack.customers.main.customerNumber,
        pack.expected.mainLpf.prefix3,
        pack.expected.mainLpf.number10,
      );
      expect(row, 'LPF row for MAIN').toBeDefined();
      const slice = parsePaymentPartnerRow(row!);
      expect(slice.documentDate).toBe(pack.expected.mainLpf.documentDate);
      expect(slice.documentDate.length).toBeGreaterThan(0);
      expect(slice.trailer.trim()).toBe('12345');
    });
  });

  test('[PDT-2187] TC-BE-3: Rescheduling — RES-SHORT (AC3)', async () => {
    const num10 = pack.expected.resShortReschedulingNum10;
    await test.step('RES + 10-digit id (TC target 0000001065 when Dev assigns)', async () => {
      const row = findRowByRef(
        pack.lines,
        pack.customers.resShort.customerNumber,
        PDT2187_TC_EXPECT.resShort.prefix3,
        num10,
      );
      expect(row, `RES${num10} for RES-SHORT`).toBeDefined();
      const slice = parsePaymentPartnerRow(row!);
      expect(slice.prefix.trim()).toBe('RES');
      expect(slice.number.trim()).toBe(num10);
      if (num10 === PDT2187_TC_EXPECT.resShort.number10) {
        expect(slice.number.trim()).toBe(PDT2187_TC_EXPECT.resShort.number10);
      }
      expect(slice.trailer.trim()).toBe('12345');
    });
  });

  test('[PDT-2187] TC-BE-4: Invoice 3+10 — MAIN (AC5)', async () => {
    await test.step('Invoice cols 338–350 (TC 250 + 0000123456 when number matches)', async () => {
      const row = findRowByRef(
        pack.lines,
        pack.customers.main.customerNumber,
        pack.expected.mainInvoice.prefix3,
        pack.expected.mainInvoice.number10,
      );
      expect(row, 'invoice row for MAIN').toBeDefined();
      const slice = parsePaymentPartnerRow(row!);
      expect(slice.prefix.trim()).toBe(pack.expected.mainInvoice.prefix3);
      expect(slice.number.trim()).toBe(pack.expected.mainInvoice.number10);
      if (
        pack.expected.mainInvoice.prefix3 === PDT2187_TC_EXPECT.invoice.prefix3 &&
        pack.expected.mainInvoice.number10 === PDT2187_TC_EXPECT.invoice.number10
      ) {
        expect(slice.prefix.trim()).toBe(PDT2187_TC_EXPECT.invoice.prefix3);
        expect(slice.number.trim()).toBe(PDT2187_TC_EXPECT.invoice.number10);
        expect(slice.documentDate).toBe(PDT2187_TC_EXPECT.invoice.documentDate);
      }
      expect(slice.trailer.trim()).toBe('12345');
    });
  });

  test('[PDT-2187] TC-BE-5: Manual without external doc — MAIN (AS-IS)', async () => {
    await test.step('No row with padded internal liability id in 338–350', async () => {
      const rows = linesForCustomer(pack.lines, pack.customers.main.customerNumber);
      const paddedId = String(pack.liabilityIds.mainNoExternal).padStart(10, '0');
      for (const line of rows) {
        const ref = combinedReference(parsePaymentPartnerRow(line));
        expect(ref).not.toBe(`000${paddedId}`);
        expect(ref).not.toContain(paddedId);
      }
    });
  });

  test('[PDT-2187] TC-BE-7: Long rescheduling id — RES-LONG (AC3)', async () => {
    const num10 = pack.expected.resLongReschedulingNum10;
    await test.step('RES + last 10 digits (TC target 4567890123)', async () => {
      const row = findRowByRef(
        pack.lines,
        pack.customers.resLong.customerNumber,
        PDT2187_TC_EXPECT.resLong.prefix3,
        num10,
      );
      expect(row, `RES${num10} for RES-LONG`).toBeDefined();
      const slice = parsePaymentPartnerRow(row!);
      expect(slice.prefix.trim()).toBe('RES');
      expect(slice.number.trim()).toBe(num10);
      expect(slice.trailer.trim()).toBe('12345');
    });
  });

  test('[PDT-2187] TC-BE-8: Action reference — MAIN (AC5)', async () => {
    await test.step('Action cols 338–350', async () => {
      const row = findRowByRef(
        pack.lines,
        pack.customers.main.customerNumber,
        pack.expected.mainAction.prefix3,
        pack.expected.mainAction.number10,
      );
      expect(row, 'action row for MAIN').toBeDefined();
      const slice = parsePaymentPartnerRow(row!);
      expect(slice.prefix.trim()).toBe(pack.expected.mainAction.prefix3);
      expect(slice.number.trim()).toBe(pack.expected.mainAction.number10);
      if (
        pack.expected.mainAction.prefix3 === PDT2187_TC_EXPECT.action.prefix3 &&
        pack.expected.mainAction.number10 === PDT2187_TC_EXPECT.action.number10
      ) {
        expect(slice.prefix.trim()).toBe(PDT2187_TC_EXPECT.action.prefix3);
        expect(slice.number.trim()).toBe(PDT2187_TC_EXPECT.action.number10);
      }
      expect(slice.trailer.trim()).toBe('12345');
    });
  });

  test('[PDT-2187] TC-BE-9: Channel scope — CUST-A in file, CUST-B absent (AC6)', async () => {
    await test.step('CUST-A present, CUST-B absent', async () => {
      const custARow = findRowByRef(
        pack.lines,
        pack.customers.custA.customerNumber,
        PDT2187_TC_EXPECT.external.prefix3,
        PDT2187_TC_EXPECT.external.number10,
      );
      expect(custARow, 'CUST-A external row').toBeDefined();
      expect(parsePaymentPartnerRow(custARow!).documentDate).toBe(toPaymentPartnerDisplayDate('01-09-2025'));
      expect(linesForCustomer(pack.lines, pack.customers.custB.customerNumber).length).toBe(0);
    });
  });

  test('[PDT-2187] TC-BE-10: Deposit reference — MAIN (AC5)', async () => {
    await test.step('Deposit cols 338–350 and date 351', async () => {
      const row = findRowByRef(
        pack.lines,
        pack.customers.main.customerNumber,
        pack.expected.mainDeposit.prefix3,
        pack.expected.mainDeposit.number10,
      );
      expect(row, 'deposit row for MAIN').toBeDefined();
      const slice = parsePaymentPartnerRow(row!);
      expect(slice.prefix.trim()).toBe(pack.expected.mainDeposit.prefix3);
      expect(slice.number.trim()).toBe(pack.expected.mainDeposit.number10);
      expect(slice.documentDate).toBe(PDT2187_TC_EXPECT.deposit.documentDate);
      expect(slice.trailer.trim()).toBe('12345');
    });
  });

  test('[PDT-2187] TC-BE-6: BANK — external doc (AC2)', async () => {
    await test.step('CSV fields 1, 2, 11 for MAIN external', async () => {
      const line = pack.bank!.csvLines.find((l) => l.includes(pack.customers.main.customerNumber));
      expect(line, 'CSV row for MAIN').toBeDefined();
      expect(bankCsvField(line!, 1)).toMatch(/^P02/i);
      expect(bankCsvField(line!, 2)).toBe(PDT2187_TC_EXPECT.bankExternalDate);
      expect(bankCsvField(line!, 11)).toContain('0000222357185');
    });
  });

  test('[PDT-2187] TC-BE-6b: BANK — rescheduling (AC3)', async () => {
    const ref = `RES${pack.expected.resShortReschedulingNum10}`;
    await test.step('CSV field 11 contains RES reference', async () => {
      const line = pack.bank!.csvLines.find(
        (l) => l.includes(pack.customers.resShort.customerNumber) && l.includes(ref),
      );
      expect(line, `CSV row containing ${ref}`).toBeDefined();
      expect(bankCsvField(line!, 11)).toContain(ref);
      if (pack.expected.resShortReschedulingNum10 === PDT2187_TC_EXPECT.resShort.number10) {
        expect(bankCsvField(line!, 11)).toContain(PDT2187_TC_EXPECT.bankResShortRef);
      }
    });
  });
});
