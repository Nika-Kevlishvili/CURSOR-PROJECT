import { randomGens } from "../../../utils/randomGens";
import { APIRequestContext } from "@playwright/test";
import {expect} from '../../../fixtures/baseFixture';

export class baseNomenclature {
    protected request: APIRequestContext;
    protected nomenclaturesData: any;

    protected currentNomenclature: string;
    protected currentNomenclatureUrlGet: string;
    protected currentNomenclatureUrlPost: string;

    protected currentPayload: any
    protected generatedName: string;

    constructor(request: APIRequestContext) {
        this.request = request;
        this.nomenclaturesData = {};
        this.currentNomenclature = '';
        this.currentNomenclatureUrlGet = '';
        this.currentNomenclatureUrlPost = '';

        this.generatedName = randomGens.generateRandomString(true, false, 10);
        this.currentPayload = {};
    }

    protected async checkNomenclature() {
        // Snapshot instance vars before the first await so concurrent calls don't overwrite each other
        const urlGet = this.currentNomenclatureUrlGet;
        const urlPost = this.currentNomenclatureUrlPost;
        const payload = this.currentPayload;
        const nomenclature = this.currentNomenclature;

        let response = await this.request.get(urlGet);
        let responseJson = await response.json();

        if (!responseJson['content'] || !Array.isArray(responseJson['content']) || responseJson['content'].length === 0) {
            const temp = await this.request.post(urlPost, { data: payload });
            expect(temp).CheckResponse();

            let createdName = (payload as any)['name'];
            if (nomenclature === 'taxes_for_grid_operator') {
                createdName = (payload as any)['gridOperator'];
            }

            const baseUrl = urlGet.split('&prompt=')[0].split('?prompt=')[0];
            const updatedUrl = `${baseUrl}${baseUrl.includes('?') ? '&' : '?'}prompt=${encodeURIComponent(createdName)}`;

            response = await this.request.get(updatedUrl);
            responseJson = await response.json();
        }

        if (!responseJson['content'] || !Array.isArray(responseJson['content']) || responseJson['content'].length === 0) {
            throw new Error(`Failed to find or create nomenclature. Response: ${JSON.stringify(responseJson)}`);
        }

        // Write under the captured local key — immune to concurrent mutation of this.currentNomenclature.
        // Calling methods' post-await writes are now redundant but harmless (same value).
        this.nomenclaturesData[nomenclature] = responseJson['content'][0]['id'];

        // Restore the snapshotted key so the calling method's synchronous store (this.nomenclaturesData[this.currentNomenclature])
        // uses the correct key despite concurrent parallel calls mutating this.currentNomenclature.
        this.currentNomenclature = nomenclature;

        return responseJson['content'][0];
    }

}