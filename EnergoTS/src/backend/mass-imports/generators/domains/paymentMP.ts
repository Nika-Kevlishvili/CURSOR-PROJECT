import * as fs from 'fs';
import * as path from 'path';
import { format } from 'date-fns';
import * as iconv from 'iconv-lite';

const FILE_ENCODING = 'win1251';
export type PaymentPartnerRow = {
    customerNumber: string | number;
    customerIdentifier: string;
    billingGroupNumber?: string;
    invoicePrefix: string;
    invoiceNumber: string;
    amount: number | string;
};

export type BankPartnerPaymentRow = {
    paymentDate: Date | string;
    amount: number | string;
    reference?: string;
    customerNumber: string | number;
    invoicePrefix: string;
    invoiceNumber: string;
    billingGroupNumber?: string;
    customerName?: string;
    customerIban?: string;
};

export type BankPartnerHeader = {
    iban: string;
    statementNumber?: string;
    currency: 'BGN' | 'EUR';
    openingBalanceDate: Date | string;
    openingBalance: number | string;
    debitCreditIndicator?: 'C' | 'D';
    /** Payment date for :20: tag; defaults to openingBalanceDate */
    paymentDate?: Date | string;
};

export type PaymentMassImportOptions = {
    outputPath?: string;
    outputDir?: string;
};

/** Phoenix PaymentPartnerMapper expects rows of at least 70 characters. */
export const PAYMENT_PARTNER_ROW_LENGTH = 70;
export const PAYMENT_PARTNER_PREFIX_LENGTH = 3;
export const PAYMENT_PARTNER_INVOICE_NUMBER_LENGTH = 10;
export const PAYMENT_PARTNER_AMOUNT_LENGTH = 10;

const PAYMENT_PARTNER_FIELDS = [
    { key: 'customerNumber', length: 16 },
    { key: 'customerIdentifier', length: 10 },
    { key: 'billingGroupNumber', length: 20 },
    { key: 'invoicePrefix', length: PAYMENT_PARTNER_PREFIX_LENGTH },
    { key: 'invoiceNumber', length: PAYMENT_PARTNER_INVOICE_NUMBER_LENGTH },
    { key: 'amount', length: PAYMENT_PARTNER_AMOUNT_LENGTH },
] as const;

const DEFAULT_BANK_IBAN = 'BG20STSA93000024288530';
const DEFAULT_STATEMENT_NUMBER = '038/01';
const DEFAULT_BILLING_GROUP = '0000000000';

export class PaymentMassImportGenerator {
    private options: PaymentMassImportOptions;

    constructor(options: PaymentMassImportOptions = {}) {
        this.options = options;
    }

    private padRight(value: string, length: number): string {
        return PaymentMassImportGenerator.truncateField(value, length).padEnd(length, ' ');
    }

    public static truncateField(value: string | number, length: number): string {
        return String(value ?? '').slice(0, length);
    }

    public static formatInvoicePrefix(prefix: string): string {
        return PaymentMassImportGenerator.truncateField(prefix.trim(), PAYMENT_PARTNER_PREFIX_LENGTH).padEnd(
            PAYMENT_PARTNER_PREFIX_LENGTH,
            ' ',
        );
    }

    public static formatInvoiceNumberPart(invoiceNumber: string | number): string {
        const digits = String(invoiceNumber).replace(/\D/g, '');
        return digits.padStart(PAYMENT_PARTNER_INVOICE_NUMBER_LENGTH, '0').slice(-PAYMENT_PARTNER_INVOICE_NUMBER_LENGTH);
    }

    private formatAmountWithDot(amount: number | string, length: number): string {
        const numeric = typeof amount === 'number' ? amount : Number(String(amount).replace(',', '.'));
        const formatted = Number.isFinite(numeric) ? numeric.toFixed(2) : String(amount);
        return this.padRight(formatted, length);
    }

    private formatAmountWithComma(amount: number | string): string {
        const numeric = typeof amount === 'number' ? amount : Number(String(amount).replace(',', '.'));
        if (!Number.isFinite(numeric)) {
            return String(amount).replace('.', ',');
        }
        return numeric.toFixed(2).replace('.', ',');
    }

    private toYyMmDd(value: Date | string): string {
        if (value instanceof Date) {
            return format(value, 'yyMMdd');
        }

        const trimmed = String(value).trim();
        if (/^\d{6}$/.test(trimmed)) {
            return trimmed;
        }

        if (/^\d{8}$/.test(trimmed)) {
            return trimmed.slice(2);
        }

        const parsed = new Date(trimmed);
        if (!Number.isNaN(parsed.getTime())) {
            return format(parsed, 'yyMMdd');
        }

        throw new Error(`Unable to parse payment date: ${value}`);
    }

    private toMmDd(value: Date | string): string {
        if (value instanceof Date) {
            return format(value, 'MMdd');
        }

        const yyMmDd = this.toYyMmDd(value);
        return yyMmDd.slice(2);
    }

    private ensureOutputDirectory(outputDir: string) {
        if (!fs.existsSync(outputDir)) {
            fs.mkdirSync(outputDir, { recursive: true });
        }
    }

    private resolveOutputPath(fileName: string): string {
        if (this.options.outputPath) {
            const dir = path.dirname(this.options.outputPath);
            this.ensureOutputDirectory(dir);
            return this.options.outputPath;
        }

        const outputDir = this.options.outputDir ?? path.resolve(__dirname, '../../output');
        this.ensureOutputDirectory(outputDir);
        return path.join(outputDir, fileName);
    }

    private buildPaymentPartnerLine(row: PaymentPartnerRow): string {
        const line = PAYMENT_PARTNER_FIELDS.map((field) => {
            if (field.key === 'amount') {
                return this.formatAmountWithDot(row.amount, field.length);
            }

            if (field.key === 'invoicePrefix') {
                return PaymentMassImportGenerator.formatInvoicePrefix(row.invoicePrefix);
            }

            if (field.key === 'invoiceNumber') {
                return PaymentMassImportGenerator.formatInvoiceNumberPart(row.invoiceNumber);
            }

            const values: Record<string, string> = {
                customerNumber: String(row.customerNumber),
                customerIdentifier: row.customerIdentifier,
                billingGroupNumber: row.billingGroupNumber ?? '',
            };

            return this.padRight(values[field.key], field.length);
        }).join('');

        if (line.length > PAYMENT_PARTNER_ROW_LENGTH) {
            throw new Error(
                `Payment partner row exceeds ${PAYMENT_PARTNER_ROW_LENGTH} characters, got ${line.length}`,
            );
        }

        // Phoenix requires length >= 70; fixed fields sum to 69, so pad one trailing space.
        return line.padEnd(PAYMENT_PARTNER_ROW_LENGTH, ' ');
    }

    private buildBankPartnerPaymentBlock(payment: BankPartnerPaymentRow, index: number): string[] {
        const paymentDate = this.toYyMmDd(payment.paymentDate);
        const valueDate = this.toMmDd(payment.paymentDate);
        const amount = this.formatAmountWithComma(payment.amount);
        const reference = payment.reference ?? `02200000${index + 1}//29729700992222${index}`;
        const customerNumber = String(payment.customerNumber).padStart(10, '0').slice(-10);
        const invoicePrefix = payment.invoicePrefix.trim();
        const invoiceNumber = PaymentMassImportGenerator.formatInvoiceNumberPart(payment.invoiceNumber);
        const invoiceDocRef = invoiceNumber.replace(/^0+/, '').slice(0, 7) || invoiceNumber.slice(-7);
        const billingGroup = String(payment.billingGroupNumber ?? DEFAULT_BILLING_GROUP)
            .padStart(10, '0')
            .slice(-10);
        const customerName = payment.customerName ?? 'AUTOMATION CUSTOMER';
        const customerIban = payment.customerIban ?? 'BG02IORT73771003825000';

        return [
            `:61:${paymentDate}${valueDate}C${amount}N${reference}`,
            'TC14-TSC30',
            ':NS:191200+',
            ':86:020+14+',
            `20ЕЛ. ЕНЕРГИЯ ${invoicePrefix}${invoiceNumber}+`,
            `21${invoiceDocRef}, ${customerNumber},${billingGroup}+`,
            '30IORT7377+',
            `31${customerIban}+`,
            `32${customerName}+`,
            '33',
        ];
    }

    /**
     * Generates a fixed-width payment partner txt file.
     * Each row is 70 characters based on the Payment Mass Import specification.
     */
    public generatePaymentPartnerFile(rows: PaymentPartnerRow[]): string {
        if (!rows.length) {
            throw new Error('At least one payment partner row is required');
        }

        const content = rows.map((row) => this.buildPaymentPartnerLine(row)).join('\n');
        const outputPath = this.resolveOutputPath('paymentPartner.txt');
        fs.writeFileSync(outputPath, iconv.encode(content, FILE_ENCODING));
        return outputPath;
    }

    /**
     * Generates a bank partner txt file using SWIFT-style tags (:20:, :25:, :60F:, :61:, :86:).
     * Structure follows the production example and Confluence specification.
     */
    public generateBankPartnerFile(header: BankPartnerHeader, payments: BankPartnerPaymentRow[]): string {
        if (!payments.length) {
            throw new Error('At least one bank partner payment row is required');
        }

        const tag20Date = this.toYyMmDd(header.paymentDate ?? header.openingBalanceDate);
        const openingDate = this.toYyMmDd(header.openingBalanceDate);
        const openingBalance = this.formatAmountWithComma(header.openingBalance);
        const debitCredit = header.debitCreditIndicator ?? 'C';
        const iban = header.iban || DEFAULT_BANK_IBAN;
        const statementNumber = header.statementNumber ?? DEFAULT_STATEMENT_NUMBER;

        const lines: string[] = [
            `:20:${tag20Date}`,
            `:25:${iban}`,
            `:28C:${statementNumber}`,
            `:60F:${debitCredit}${openingDate}${header.currency}${openingBalance}`,
        ];

        payments.forEach((payment, index) => {
            lines.push(...this.buildBankPartnerPaymentBlock(payment, index));
        });

        const content = lines.join('\n');
        const outputPath = this.resolveOutputPath('bank.txt');
        fs.writeFileSync(outputPath, iconv.encode(content, FILE_ENCODING));
        return outputPath;
    }

    /**
     * Builds payment partner rows from created billing entities.
     */
    public static buildPaymentPartnerRowsFromEntities(params: {
        customerNumber: string | number;
        customerIdentifier: string;
        invoicePrefix: string;
        invoiceNumber: string;
        amount: number | string;
        billingGroupNumber?: string;
    }): PaymentPartnerRow {
        return {
            customerNumber: params.customerNumber,
            customerIdentifier: params.customerIdentifier,
            billingGroupNumber: params.billingGroupNumber,
            invoicePrefix: PaymentMassImportGenerator.formatInvoicePrefix(params.invoicePrefix),
            invoiceNumber: PaymentMassImportGenerator.formatInvoiceNumberPart(params.invoiceNumber),
            amount: params.amount,
        };
    }

    /**
     * Builds bank partner payment rows from created billing entities.
     */
    public static buildBankPartnerRowsFromEntities(params: {
        paymentDate: Date | string;
        customerNumber: string | number;
        invoicePrefix: string;
        invoiceNumber: string;
        amount: number | string;
        billingGroupNumber?: string;
        reference?: string;
        customerName?: string;
        customerIban?: string;
    }): BankPartnerPaymentRow {
        return {
            paymentDate: params.paymentDate,
            customerNumber: params.customerNumber,
            invoicePrefix: params.invoicePrefix.trim(),
            invoiceNumber: PaymentMassImportGenerator.formatInvoiceNumberPart(params.invoiceNumber),
            billingGroupNumber: params.billingGroupNumber,
            amount: params.amount,
            reference: params.reference,
            customerName: params.customerName,
            customerIban: params.customerIban,
        };
    }

    /**
     * Resolves invoice prefix and 10-digit number for mass import files.
     * Prefer explicit API fields (invoice.prefix + invoice.invoiceNumber) over combined strings.
     */
    public static resolveInvoiceParts(options: {
        prefix?: string | null;
        invoiceNumber?: string | null;
        fullInvoiceNumber?: string | null;
    }): { invoicePrefix: string; invoiceNumber: string } {
        if (options.prefix?.trim()) {
            const numericPart = String(options.invoiceNumber ?? '').trim();
            const number = numericPart.includes('-') ? numericPart.split('-').pop()! : numericPart;

            return {
                invoicePrefix: PaymentMassImportGenerator.formatInvoicePrefix(options.prefix),
                invoiceNumber: PaymentMassImportGenerator.formatInvoiceNumberPart(number),
            };
        }

        const full = (options.fullInvoiceNumber ?? options.invoiceNumber ?? '').trim();
        if (!full) {
            throw new Error('Invoice prefix and number are required for payment mass import');
        }

        if (full.includes('-')) {
            const separatorIndex = full.indexOf('-');
            const invoicePrefix = full.substring(0, separatorIndex).trim();
            const invoiceNumber = full.substring(separatorIndex + 1).trim();

            if (!invoicePrefix || !invoiceNumber) {
                throw new Error(`Invoice number "${full}" must be in PREFIX-NUMBER format`);
            }

            return {
                invoicePrefix: PaymentMassImportGenerator.formatInvoicePrefix(invoicePrefix),
                invoiceNumber: PaymentMassImportGenerator.formatInvoiceNumberPart(invoiceNumber),
            };
        }

        throw new Error(
            `Invoice number "${full}" has no prefix. Use invoice.prefix from the invoice API response.`,
        );
    }

    public static splitInvoiceNumber(
        fullInvoiceNumber: string,
        fallbackPrefix?: string,
    ): { invoicePrefix: string; invoiceNumber: string } {
        if (fallbackPrefix) {
            return PaymentMassImportGenerator.resolveInvoiceParts({
                prefix: fallbackPrefix,
                invoiceNumber: fullInvoiceNumber,
            });
        }

        return PaymentMassImportGenerator.resolveInvoiceParts({ fullInvoiceNumber });
    }
}
