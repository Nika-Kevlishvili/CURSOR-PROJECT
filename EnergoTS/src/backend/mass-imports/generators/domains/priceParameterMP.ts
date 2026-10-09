import * as ExcelJS from 'exceljs';
import { format, add, isBefore, parseISO } from 'date-fns';

/** Optional shifted slot — `period` without `*`; never auto-generated. */
export type ShiftedHourEntry = {
    period: string;
    amount?: number;
};

export type massImportValues = {
    startDate: string;
    endDate: string;
    amount: number;
    /** Optional explicit shifted rows for this date range only. */
    shiftedHours?: ShiftedHourEntry[];
};

export type PriceParameterMPOptions = {
    /** Override default output file name (`${freq}.xlsx` in cwd). */
    outputPath?: string;
    /**
     * EET SLP price series: add the October fallback hour and drop the March
     * spring-forward hour so billing_run price-count validation accepts the file
     * in every month. Off by default so profile imports stay unchanged.
     */
    applySlpDstRules?: boolean;
};

class priceParameterMP {
    private dateRanges: massImportValues[];
    private options: PriceParameterMPOptions;

    constructor(massImportvalues: massImportValues[], options: PriceParameterMPOptions = {}) {
        this.dateRanges = massImportvalues;
        this.options = options;
    }

    private parsePeriodString(period: string, freq: string): Date {
        if (freq === '1D') {
            const [day, month, year] = period.split('.').map(Number);
            return new Date(year, month - 1, day);
        }
        const [datePart, timePart] = period.trim().split(/\s+/);
        const [day, month, year] = datePart.split('.').map(Number);
        const [hour, minute] = timePart.split(':').map(Number);
        return new Date(year, month - 1, day, hour, minute);
    }

    private buildRowsForRange(freq: string, range: massImportValues) {
        const strftime = freq === '1D' ? 'dd.MM.yyyy' : 'dd.MM.yyyy HH:mm';
        const startTime = parseISO(range.startDate);
        const endTime = parseISO(range.endDate);
        const rows: { period: string; amount: number; shifted: boolean; sortKey: number }[] = [];

        for (const ts of this.getTimestamps(freq, startTime, endTime)) {
            if (this.options.applySlpDstRules && this.isEetSpringForwardSlot(ts, freq)) {
                continue;
            }
            rows.push({
                period: format(ts, strftime),
                amount: range.amount,
                shifted: false,
                sortKey: ts.getTime(),
            });
        }

        for (const entry of range.shiftedHours ?? []) {
            const ts = this.parsePeriodString(entry.period, freq);
            rows.push({
                period: `${format(ts, strftime)}*`,
                amount: entry.amount ?? range.amount,
                shifted: true,
                sortKey: ts.getTime(),
            });
        }

        rows.sort((a, b) => {
            const byTime = a.sortKey - b.sortKey;
            if (byTime !== 0) return byTime;
            return (a.shifted ? 1 : 0) - (b.shifted ? 1 : 0);
        });

        if (this.options.applySlpDstRules) {
            this.appendOctoberFallbackRows(freq, strftime, startTime, endTime, range.amount, rows);
        }

        return rows;
    }

    /** Last Sunday of a local calendar month. monthIndex is 0-based. */
    private lastSunday(year: number, monthIndex: number): Date {
        const last = new Date(year, monthIndex + 1, 0);
        last.setHours(0, 0, 0, 0);
        last.setDate(last.getDate() - last.getDay());
        return last;
    }

    /**
     * EET spring-forward slots from billing_run.get_march_leap_hour:
     * last Sunday of March, 03:00 (and 03:15/03:30/03:45 for 15 minutes).
     * Those timestamps must be absent or the SLP price series is invalid.
     */
    private isEetSpringForwardSlot(ts: Date, freq: string): boolean {
        if (freq !== '15min' && freq !== '1h') {
            return false;
        }
        if (ts.getMonth() !== 2 || ts.getHours() !== 3) {
            return false;
        }
        const sunday = this.lastSunday(ts.getFullYear(), 2);
        if (ts.getDate() !== sunday.getDate()) {
            return false;
        }
        if (freq === '1h') {
            return ts.getMinutes() === 0;
        }
        return ts.getMinutes() === 0 || ts.getMinutes() === 15 || ts.getMinutes() === 30 || ts.getMinutes() === 45;
    }

    /**
     * EET October fallback hour. Billing counts days×96+4 (15 minutes) or days×24+1 (hourly).
     * The extra rows are the repeated 03:00 hour on the last Sunday, marked shifted (`*`).
     */
    private appendOctoberFallbackRows(
        freq: string,
        strftime: string,
        startTime: Date,
        endTime: Date,
        amount: number,
        rows: { period: string; amount: number; shifted: boolean; sortKey: number }[],
    ) {
        if (freq !== '15min' && freq !== '1h') {
            return;
        }
        const minutes = freq === '15min' ? [0, 15, 30, 45] : [0];
        const coveredDays = new Set<string>();
        for (const ts of this.getTimestamps(freq, startTime, endTime)) {
            if (ts.getMonth() === 9) {
                coveredDays.add(`${ts.getFullYear()}-${ts.getMonth()}-${ts.getDate()}`);
            }
        }
        const years = new Set([...coveredDays].map((day) => Number(day.slice(0, 4))));
        for (const year of years) {
            const sunday = this.lastSunday(year, 9);
            const sundayKey = `${sunday.getFullYear()}-${sunday.getMonth()}-${sunday.getDate()}`;
            if (!coveredDays.has(sundayKey)) {
                continue;
            }
            for (const minute of minutes) {
                const slot = new Date(sunday.getFullYear(), sunday.getMonth(), sunday.getDate(), 3, minute, 0, 0);
                const label = format(slot, strftime);
                const alreadyPresent = rows.some((row) => row.shifted && row.period.replace(/\*$/, '') === label);
                if (alreadyPresent) {
                    continue;
                }
                rows.push({
                    period: `${label}*`,
                    amount,
                    shifted: true,
                    sortKey: slot.getTime(),
                });
            }
        }
        rows.sort((a, b) => {
            const byTime = a.sortKey - b.sortKey;
            if (byTime !== 0) return byTime;
            return (a.shifted ? 1 : 0) - (b.shifted ? 1 : 0);
        });
    }

    private async generateSheet(freq: string) {
        let period = '';
        if (freq === '15min') {
            period = 'PERIOD \nDD.MM.YYYY HH:MM';
        } else if (freq === '1h') {
            period = 'PERIOD\nDD.MM.YYYY HH:MM';
        } else if (freq === '1D') {
            period = 'PERIOD\nDD.MM.YYYY';
        }

        const workbook = new ExcelJS.Workbook();
        const worksheet = workbook.addWorksheet('Sheet 1');

        worksheet.getCell('A1').value = period;
        worksheet.getCell('A1').numFmt = '@';

        worksheet.getCell('B1').value = 'VALUE\n(use D for delete)';
        worksheet.getCell('B1').numFmt = 'yyyy-mm-dd hh:mm';

        let rowIndex = 2;
        for (const range of this.dateRanges) {
            for (const row of this.buildRowsForRange(freq, range)) {
                worksheet.getCell(`A${rowIndex}`).value = row.period;
                worksheet.getCell(`B${rowIndex}`).value = row.amount;
                rowIndex++;
            }
        }

        const output = this.options.outputPath ?? `${freq}.xlsx`;
        await workbook.xlsx.writeFile(output);
    }

    private getTimestamps(freq: string, startTime: Date, endTime: Date): Date[] {
        const timestamps: Date[] = [];
        let current = new Date(startTime);

        while (!isBefore(endTime, current)) {
            timestamps.push(new Date(current));
            current = add(current, freq === '1D' ? { days: 1 } : freq === '15min' ? { minutes: 15 } : { hours: 1 });
        }

        return timestamps;
    }

    public async generateFifteenMinutes() {
        await this.generateSheet('15min');
    }

    public async generateDays() {
        await this.generateSheet('1D');
    }

    public async generateHours() {
        await this.generateSheet('1h');
    }

    public static async getFileStream(profileType: string) {
        const fs = await import('fs');
        let filename = '';

        if (profileType === 'ONE_DAY' || profileType === 'DAY') {
            filename = '1D.xlsx';
        } else if (profileType === 'ONE_HOUR' || profileType === 'HOUR') {
            filename = '1h.xlsx';
        } else if (profileType === 'FIFTEEN_MINUTES' || profileType === 'MIN') {
            filename = '15min.xlsx';
        }

        return fs.createReadStream(filename);
    }
}

export default priceParameterMP;
