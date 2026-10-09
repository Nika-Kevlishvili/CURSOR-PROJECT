export type SupplyActionDeactivationRow = {
    identifier: string;
    /** Notice receiving date, dd.MM.yyyy. */
    noticeReceivingDate: string;
};

/**
 * One Excel row per POD.
 * Column A is the POD identifier, column B is the notice receiving date (dd.MM.yyyy).
 * The deactivation date itself is sent on the upload URL as ?date=yyyy-MM-dd.
 */
export function supplyActionDeactivationMassPayload(rows: SupplyActionDeactivationRow[]) {
    const payload: Record<string, { value: string; cellNumber: string }> = {};

    rows.forEach((row, index) => {
        const excelRow = index + 2;
        payload[`identifier_${index}`] = {
            value: row.identifier,
            cellNumber: `A${excelRow}`,
        };
        payload[`noticeReceivingDate_${index}`] = {
            value: row.noticeReceivingDate,
            cellNumber: `B${excelRow}`,
        };
    });

    return payload;
}
