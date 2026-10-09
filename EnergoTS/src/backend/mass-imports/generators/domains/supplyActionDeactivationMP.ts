import { APIRequestContext } from "playwright";
import { baseFixture } from '../../../fixtures/baseFixture';
import { supplyActionDeactivationMassPayload } from '../../massImportPayloads/supplyActionDeactivationMpPayload';
import { randomGens } from '../../../utils/randomGens';

export class SupplyActionDeactivationMassImportGenerator {
    private Request: APIRequestContext;
    private responses: baseFixture['Responses'];
    private templateName = 'SUPPLY_ACTION_DEACTIVATIONS';
    readonly downloadUrl = 'mass-import/SUPPLY_ACTION_DEACTIVATIONS/template/download';

    constructor(apiRequestContext: APIRequestContext, responses: baseFixture['Responses']) {
        this.Request = apiRequestContext;
        this.responses = responses;
    }

    /**
     * Builds the supply-deactivation workbook from PODs already stored on Responses.
     * Column B is the notice receiving date (dd.MM.yyyy), at least 6 working days
     * before the deactivation date. That date is sent as the upload query param `date`.
     *
     * When `podIndexes` is omitted, every POD in Responses.pod is included.
     * When it is provided, only those indexes are included.
     */
    public async supplyDeactivationMP(deactivationDate?: string, podIndexes?: number[]) {
        const pods = this.selectPods(podIndexes);
        if (pods.length === 0) {
            throw new Error('Supply deactivation mass import needs at least one POD in Responses.pod');
        }

        const date = deactivationDate || randomGens.generateTodaysDate('yyyy-mm-dd');
        const noticeReceivingDate = this.noticeReceivingDate(date);
        const rows = [];

        for (const pod of pods) {
            const podId = typeof pod === 'number' ? pod : pod?.id;
            if (podId == null) {
                throw new Error('POD in Responses.pod has no id');
            }

            const podResponse = await this.Request.get(`pod/${podId}`);
            if (!podResponse.ok()) {
                throw new Error(`Failed to load POD ${podId}: ${podResponse.status()} ${podResponse.statusText()}`);
            }

            const podBody = await podResponse.json();
            const identifier = podBody?.identifier;
            if (!identifier) {
                throw new Error(`POD ${podId} has no identifier`);
            }

            rows.push({ identifier: String(identifier), noticeReceivingDate });
        }

        return {
            payload: supplyActionDeactivationMassPayload(rows),
            templateName: this.templateName,
            downloadUrl: this.downloadUrl,
            uploadUrl: `mass-import/SUPPLY_ACTION_DEACTIVATIONS/files/upload?date=${date}`,
        };
    }

    private selectPods(podIndexes?: number[]) {
        const allPods = this.responses.pod ?? [];
        if (podIndexes === undefined) {
            return allPods;
        }

        return podIndexes.map((index) => {
            if (!Number.isInteger(index) || index < 0 || index >= allPods.length) {
                throw new Error(`POD index ${index} is out of range in Responses.pod (length ${allPods.length})`);
            }
            return allPods[index];
        });
    }

    /** dd.MM.yyyy, 10 working days before the deactivation date. */
    private noticeReceivingDate(deactivationDate: string): string {
        const [year, month, day] = deactivationDate.split('-').map(Number);
        const date = new Date(year, month - 1, day);
        let remaining = 10;
        while (remaining > 0) {
            date.setDate(date.getDate() - 1);
            const weekday = date.getDay();
            if (weekday !== 0 && weekday !== 6) {
                remaining--;
            }
        }
        const dd = String(date.getDate()).padStart(2, '0');
        const mm = String(date.getMonth() + 1).padStart(2, '0');
        const yyyy = String(date.getFullYear());
        return `${dd}.${mm}.${yyyy}`;
    }
}
