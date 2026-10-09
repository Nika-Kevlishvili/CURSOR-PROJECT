import * as ExcelJS from 'exceljs';
import { format, add, isBefore, parseISO } from 'date-fns';

type massImportValues = {startDate: string, endDate: string, amount: number}[]

class priceParameterMP {
    private dateRanges: massImportValues;

    constructor(massImportvalues: massImportValues) {
        this.dateRanges = massImportvalues;
    }

    private async generateSheet(freq: string) {
        const strftime = freq === '1D' ? 'dd.MM.yyyy' : 'dd.MM.yyyy HH:mm';
        let period = '';
        if(freq === '15min') {
            period  = 'PERIOD \nDD.MM.YYYY HH:MM';
        }else if(freq === '1h') {
            period = 'PERIOD\nDD.MM.YYYY HH:MM';
        }else if(freq === '1D') {
            period = 'PERIOD\nDD.MM.YYYY'
        }

        const workbook = new ExcelJS.Workbook();
        const worksheet = workbook.addWorksheet('Sheet 1');

        worksheet.getCell('A1').value = period;
        worksheet.getCell('A1').numFmt = '@';

        worksheet.getCell('B1').value = 'VALUE\n(use D for delete)';
        worksheet.getCell('B1').numFmt = 'yyyy-mm-dd hh:mm';

        let rowIndex = 2;
        for (const range of this.dateRanges) {
            const timestamps = this.getTimestamps(freq, parseISO(range.startDate), parseISO(range.endDate));
            const formattedTimestamps = timestamps.map(ts => format(ts, strftime));

            formattedTimestamps.forEach((timestamp) => {
                worksheet.getCell(`A${rowIndex}`).value = timestamp;
                worksheet.getCell(`B${rowIndex}`).value = range.amount;
                rowIndex++;
            });
        }

        await workbook.xlsx.writeFile(`${freq}.xlsx`);
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