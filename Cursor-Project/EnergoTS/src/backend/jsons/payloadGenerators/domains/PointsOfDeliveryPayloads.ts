import { APIRequestContext } from "playwright";
import { baseFixture } from "../../../fixtures/baseFixture";
import { pod_settlement } from '../../payloads/pod/podSettlement';
import { pod_slp } from '../../payloads/pod/podSlp';
import { meters } from '../../payloads/pod/meters';
import { pod_activation } from '../../payloads/contractOrders/podActivation';
import {expect} from '../../../fixtures/baseFixture';

export class PointsOfDeliveryPayloads {
    private Request: APIRequestContext;
    private responses: baseFixture['Responses'];

    constructor(apiRequestContext: APIRequestContext, responses: baseFixture['Responses']) {
        this.Request = apiRequestContext;
        this.responses = responses;
    }

    public pod_settlement() {
        const payload = pod_settlement();
        return payload;
    }

    public pod_slp() {
        const payload = pod_slp();
        return payload;
    }

    public meters() {
        const payload = meters();
        return payload;
    }

    public async pod_activation(podIndex: number = 0, activationDate?: string, deactivationDate?: string, contract: number = 0) {
        const podActivationPayload = pod_activation()
        if (activationDate) {
            podActivationPayload.activationDate = activationDate;
        }
        if (deactivationDate) {
            podActivationPayload.deactivationDate = deactivationDate;
        }

        const getContract = await this.Request.get(`product-contract/${this.responses.productContract[contract].id}`);
        const getContractResponse = await getContract.json();

        for (const i of getContractResponse.contractPodsResponses) {
            if (i.podId === this.responses.pod[podIndex].id) {
                podActivationPayload.identifier = i.identifier;
                podActivationPayload.podDetailId = i.podDetailId;
                break;
            }
        }

        podActivationPayload.contractDetailId = getContractResponse.versions[0].id;

        return podActivationPayload;
    }

    public async podStatusCheck(podStatus: "POTENTIAL" | "ACTIVE" | "DEACTIVATED" | "NON_ACTIVATED" | "AWAITING_ACTIVATION" | "IN_PROCESS_OF_CONTRACTING")  {
        let matched: any;
        await expect.poll(async () => {
            const customerPodGet = await this.Request.get(`customer/pod-tab?page=0&size=25&customerId=${this.responses.customer[0].id}`);
            await expect(customerPodGet).CheckResponse();
            const customerPodGetResponse = await customerPodGet.json();
            matched = customerPodGetResponse.content.find((X: any) => X.podId === this.responses.pod[0].id);
            return matched?.customerPodStatus;
        }, { timeout: 30_000, intervals: [1_000, 2_000, 5_000] }).toBe(podStatus);

        expect(matched, "❌ POD not found on customer").toBeDefined();
        return matched;
    }
}