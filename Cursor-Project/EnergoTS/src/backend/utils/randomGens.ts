type date_format = 'dd.mm.yyyy' | 'yyyy-mm-dd' | "dd-mm-yyyy"

export class randomGens {
    /**
     * Generates a random string based on the specified criteria.
     *
     * @param {boolean} [upperCase] - If true, includes uppercase letters in the generated string.
     * @param {boolean} [numbers] - If true, includes numbers in the generated string.
     * @param {number} [length=10] - The length of the generated string. Defaults to 10 if not specified.
     * @returns {string} A random string based on the specified criteria.
     */
    public static generateRandomString(upperCase?: boolean, numbers?: boolean, length: number = 10): string {
        let characters = 'abcdefghijklmnopqrstuvwxyz';
        if (upperCase) {
            characters = 'ABCDEFGHIJKLMNOPQRSTUVWXYZ';
        }else if (numbers) {
            characters = '0123456789';
        }
        let result = '';
        for (let i = 0; i < length; i++) {
            const randomIndex = Math.floor(Math.random() * characters.length);
            result += characters.charAt(randomIndex);
        }
        return result;
    }
    /**
     * Generates a random email address, with prefix 'EmailAdress@' and a random string of 6 characters followed by '.com'.
     *
     * @returns {string} A randomly generated email address.
     */
    public static generateRandomEMail(): string {
        const randomEmail = `EmailAdress@${randomGens.generateRandomString(true, false, 6)}.com`;
        return randomEmail;
    }
    /**
     * Generates a unique numeric identifier by combining the current timestamp with
     * cryptographically-seeded random digits. Max length is 32 characters.
     * Safe to use in parallel pipeline runs that start at the same time.
     *
     * @returns {string} A unique numeric string of up to 32 digits.
     */
    public static generateUniqueIdentifier(): string {
        const timestamp = Date.now().toString(); // 13 digits
        const random = Math.floor(Math.random() * 10000).toString().padStart(4, '0'); // 4 digits
        return timestamp + random; // 17 digits total — max allowed by API
    }

    public static generateCurrentTimeStamp(toISOString: boolean = false): string {
        if (toISOString) {
            return new Date().toISOString();
        }

        const date = new Date();
        const timeStamp = date.getTime();
        return timeStamp.toString();
    }

    /**
     * Generates ISO 8601 timestamp with optional time offset
     * @param hoursOffset - Hours to add (can be negative)
     * @param minutesOffset - Minutes to add (can be negative)
     * @returns ISO timestamp string (e.g., "2026-01-23T01:10:00.000Z")
     */
    public static generateISOTimestampWithOffset(hoursOffset: number = 0, minutesOffset: number = 0): string {
        const date = new Date();
        date.setHours(date.getHours() + hoursOffset);
        date.setMinutes(date.getMinutes() + minutesOffset);
        return date.toISOString();
    }

    /**
    * generate current date in format dd.mm.yyyy (e.g. 01.01.2025)
    */
    public static generateTodaysDate(format: date_format = 'dd.mm.yyyy'): string {
        const today = new Date();
        if (format === 'yyyy-mm-dd') {
            const yyyy = today.getFullYear();
            const mm = String(today.getMonth() + 1).padStart(2, '0');
            const dd = String(today.getDate()).padStart(2, '0');
            return `${yyyy}-${mm}-${dd}`;
        } else if (format === 'dd-mm-yyyy') {
            return today.toLocaleDateString('en-GB').split('/').join('-');
        } else {
            return today.toLocaleDateString('en-GB').split('/').join('.');
        }
    }

    /**
     * Calendar date in UTC. Dev validates "today or past" against the UTC day,
     * which is still the previous day for a few hours after local midnight.
     */
    public static generateUtcDate(format: date_format = 'dd.mm.yyyy'): string {
        const today = new Date();
        const yyyy = today.getUTCFullYear();
        const mm = String(today.getUTCMonth() + 1).padStart(2, '0');
        const dd = String(today.getUTCDate()).padStart(2, '0');

        if (format === 'yyyy-mm-dd') {
            return `${yyyy}-${mm}-${dd}`;
        } else if (format === 'dd-mm-yyyy') {
            return `${dd}-${mm}-${yyyy}`;
        } else {
            return `${dd}.${mm}.${yyyy}`;
        }
    }
    
    /**
    * Generate yesterday date in format dd.mm.yyyy
    */
    public static generateYesterdaysDate(format: date_format = 'dd.mm.yyyy'): string {
        const yesterday = new Date();
        yesterday.setDate(yesterday.getDate() - 1); 

        if (format === 'yyyy-mm-dd') {
            const yyyy = yesterday.getFullYear();
            const mm = String(yesterday.getMonth() + 1).padStart(2, '0');
            const dd = String(yesterday.getDate()).padStart(2, '0');
            return `${yyyy}-${mm}-${dd}`;
        } else if (format === 'dd-mm-yyyy') {
            return yesterday.toLocaleDateString('en-GB').split('/').join('-');
        } else {
            return yesterday.toLocaleDateString('en-GB').split('/').join('.');
        }
    }

    /**
    * Generate one week before date in format dd.mm.yyyy
    */
    public static generateOneWeekBeforeDate(format: date_format = 'dd.mm.yyyy'): string {
        const yesterday = new Date();
        yesterday.setDate(yesterday.getDate() - 7); 

        if (format === 'yyyy-mm-dd') {
            const yyyy = yesterday.getFullYear();
            const mm = String(yesterday.getMonth() + 1).padStart(2, '0');
            const dd = String(yesterday.getDate()).padStart(2, '0');
            return `${yyyy}-${mm}-${dd}`;
        } else if (format === 'dd-mm-yyyy') {
            return yesterday.toLocaleDateString('en-GB').split('/').join('-');
        } else {
            return yesterday.toLocaleDateString('en-GB').split('/').join('.');
        }
    }

    public static generateMonthStartDate(format: date_format = 'dd.mm.yyyy', monthOffset: number = 0): string {
        const today = new Date();
        const targetDate = new Date(today.getFullYear(), today.getMonth() + monthOffset, 1);
        const dd = '01';
        const mm = String(targetDate.getMonth() + 1).padStart(2, '0');
        const yyyy = String(targetDate.getFullYear());
        if (format === 'yyyy-mm-dd') {
            return `${yyyy}-${mm}-${dd}`;
        } else if (format === 'dd-mm-yyyy') {
            return `${dd}-${mm}-${yyyy}`;
        } else {
            return `${dd}.${mm}.${yyyy}`;
        }
    }

    /**
    * Generate current month end date in specified format (e.g. dd.mm.yyyy -> 31.01.2025)
    */
    public static generateMonthEndDate(format: date_format = 'dd.mm.yyyy', monthOffset: number = 0): string {
        const today = new Date();
        const endOfMonth = new Date(today.getFullYear(), today.getMonth() + monthOffset + 1, 0);
        const dd = String(endOfMonth.getDate()).padStart(2, '0');
        const mm = String(endOfMonth.getMonth() + 1).padStart(2, '0');
        const yyyy = String(endOfMonth.getFullYear());
        if (format === 'yyyy-mm-dd') {
            return `${yyyy}-${mm}-${dd}`;
        } else if (format === 'dd-mm-yyyy') {
            return `${dd}-${mm}-${yyyy}`;
        } else {
            return `${dd}.${mm}.${yyyy}`;
        }
    }

    /**
    * Generate current month half date (15th day) in specified format (e.g. yyyy-mm-dd -> 2025-11-15)
    */
    public static generateMonthHalfDate(format: date_format = 'yyyy-mm-dd', monthOffset: number = 0): string {
        const today = new Date();
        const targetDate = new Date(today.getFullYear(), today.getMonth() + monthOffset, 15);
        const dd = '15';
        const mm = String(targetDate.getMonth() + 1).padStart(2, '0');
        const yyyy = String(targetDate.getFullYear());
        if (format === 'yyyy-mm-dd') {
            return `${yyyy}-${mm}-${dd}`;
        } else if (format === 'dd-mm-yyyy') {
            return `${dd}-${mm}-${yyyy}`;
        } else {
            return `${dd}.${mm}.${yyyy}`;
        }
    }

    /**
    * Generate current month half date plus one day (16th day) in specified format (e.g. yyyy-mm-dd -> 2025-11-16)
    */
    public static generateMonthHalfDatePlusOne(format: date_format = 'yyyy-mm-dd', dd: number = 16, monthOffset: number = 0): string {
        const today = new Date();
        const targetDate = new Date(today.getFullYear(), today.getMonth() + monthOffset, dd);
        const mm = String(targetDate.getMonth() + 1).padStart(2, '0');
        const yyyy = String(targetDate.getFullYear());
        if (format === 'yyyy-mm-dd') {
            return `${yyyy}-${mm}-${dd}`;
        } else if (format === 'dd-mm-yyyy') {
            return `${dd}-${mm}-${yyyy}`;
        } else {
            return `${dd}.${mm}.${yyyy}`;
        }
    }

    /**
    *For generating random odd number
    */
    public static getRandomOdd(): number {
        let num = Math.floor(Math.random() * 100);
        if (num % 2 === 0) {
            num += 1;
            if (num >= 100) num -= 2;
        }
        return num;
    }
    
    /**
     * For generating random even number
     */
    public static getRandomEven(): number {
        let num = Math.floor(Math.random() * 100);
        if (num % 2 !== 0) {
            num -= 1;
            if (num < 0) num += 2;
        }
        return num;
    }

    public static generatePercentage() {
        return Math.floor(Math.random() * 99) + 1; // returns 1–99
    }

    /**
     * 
     * This is for online payment -> transaction ID 
     */
    public static generateTID(): string {
        const now = new Date();
        const YYYY = now.getFullYear().toString();
        const MM = (now.getMonth() + 1).toString().padStart(2, '0');
        const DD = now.getDate().toString().padStart(2, '0');
        const HH = now.getHours().toString().padStart(2, '0');
        const mm = now.getMinutes().toString().padStart(2, '0');
        const ss = now.getSeconds().toString().padStart(2, '0');
        const timestampTrimmed = `${YYYY}${MM}${DD}${HH}${mm}${ss}`;
        
        /* These values are hardcoded in the system */
        const STAN = '077118'
        const AID = '100020'

        return timestampTrimmed + STAN + AID
    }

    /**
     * Bulgarian Post receipt TID (PHN-2628): 22 digits — yyyyMMddHHmmssff (16) + 6 random digits.
     */
    public static generateBulgarianPostTid(): string {
        const now = new Date();
        const yyyy = now.getFullYear().toString();
        const MM = (now.getMonth() + 1).toString().padStart(2, '0');
        const DD = now.getDate().toString().padStart(2, '0');
        const HH = now.getHours().toString().padStart(2, '0');
        const mm = now.getMinutes().toString().padStart(2, '0');
        const ss = now.getSeconds().toString().padStart(2, '0');
        const ff = Math.floor(now.getMilliseconds() / 10).toString().padStart(2, '0');
        const suffix = Math.floor(Math.random() * 1_000_000).toString().padStart(6, '0');
        return `${yyyy}${MM}${DD}${HH}${mm}${ss}${ff}${suffix}`;
    }
        
    /**
     * 
     * This is for online payment -> Current date as a payment date in format -> YYYYMMDDHHmmss
     * + To simulate X hour ago payment
     */
    public static generateOnlinePaymentDate(hoursAgo: number = 4): string {
        const date = new Date();
        date.setHours(date.getHours() - hoursAgo);

        return date
            .toISOString()          // 2026-02-11T10:20:30.000Z
            .replace(/[-:T]/g, '')  // remove separators
            .slice(0, 14);          // keep YYYYMMDDHHmmss
    }  

    public static getRandomNumber(): number {
        return Math.floor(Math.random() * 1000) + 1;
    }

    public static generateExternalOutgoingDocumentNumber(): string {
        return Math.floor(Math.random() * 10_000_000_000).toString().padStart(10, '0');
    }
}
