import { APIRequestContext } from "playwright";
import {expect} from '../../../fixtures/baseFixture';
import { baseFixture } from "../../../fixtures/baseFixture";
import { expressContract } from '../../payloads/contractOrders/expressContract';
import { ProductContract, ProductContractPayload } from '../../payloads/contractOrders/productContract';
import { Service_contract } from '../../payloads/contractOrders/serviceContract';
import { action } from '../../payloads/contractOrders/action';
import { serviceOrder } from '../../payloads/contractOrders/serviceOrder';
import { goodsOrder } from '../../payloads/contractOrders/goodsOrder';
import { billingGroup } from "../../payloads/contractOrders/billingGroup";
import { claimedPenalty } from '../../payloads/contractOrders/claimedPenalty';

export class ContractsAndOrdersPayloads {
    private Request: APIRequestContext;
    private responses: baseFixture['Responses'];

    constructor(apiRequestContext: APIRequestContext, responses: baseFixture['Responses']) {
        this.Request = apiRequestContext;
        this.responses = responses;
    }

    public async expressContract() {
        const payload = expressContract();
        const productget = await this.Request.get(`products/${this.responses.product[0]}?`);
        const productgetjson = await productget.json();

        for (const i of this.responses.pod) {
            payload.podDetailIds.push(i.podDetailId);
        }

        payload.expressContractParameters.productId = this.responses.product[0];
        payload.productParameters.productContractTermId = productgetjson.productContractTerms[0].id;
        payload.productParameters.invoicePaymentTermId = this.responses.terms[0].invoicePaymentTerms[0].id;

        return payload;
       
    }
    

    /**
     * 
     * @param customerIndex  - customer index in Responses.customer array to be added to the contract
     * @param productIndex - product index in Responses.product array to be added to the contract
     * @returns - Product Contract payload
     */
    public async product_contract(customerIndex: number = 0, productIndex: number = 0, podIndex?: number) {
        const productContractPayload = ProductContract();

        const getCustomer = await this.Request.get(`customer/${this.responses.customer[customerIndex].id}?version=1`);
        if (!getCustomer.ok()) {
            const errorText = await getCustomer.text();
            throw new Error(`Failed to get customer (${getCustomer.status()}): ${errorText.substring(0, 200)}`);
        }
        const getCustomerResponse = await getCustomer.json();

        const getProduct = await this.Request.get(`products/${this.responses.product[productIndex]}?version=1`);
        if (!getProduct.ok()) {
            const errorText = await getProduct.text();
            throw new Error(`Failed to get product (${getProduct.status()}): ${errorText.substring(0, 200)}`);
        }
        const getProductResponse = await getProduct.json();

        const termsId = getProductResponse.terms.id 
        const getTerms = await this.Request.get(`terms?id=${termsId}`);

        if (!getTerms.ok()) {
            const errorText = await getTerms.text();
            throw new Error(`Failed to get terms (${getTerms.status()}): ${errorText.substring(0, 200)}`);
        }
        const getTermsResponse = await getTerms.json();

        if (this.responses.interim) {
            for (const i of this.responses.interim) {
                const interimGet = await this.Request.get(`iap/${i}?version=1`);
                const interimJson = await interimGet.json();
                const interimDetails = {
                    "issueDate": null,
                    "value": await interimJson.value,
                    "interimAdvancePaymentId": await interimJson.id,
                    "termValue":  await interimJson.interimAdvancePaymentTerm.value,
                    "contractFormulas": []
                }
                productContractPayload.productParameters.interimAdvancePayments.push(interimDetails);
            }
        }

        productContractPayload.basicParameters.customerId = this.responses.customer[0].id;

        productContractPayload.basicParameters.communicationDataBillingId = getCustomerResponse.communicationData?.[0]?.id;
        productContractPayload.basicParameters.communicationDataContractId = getCustomerResponse.communicationData?.[0]?.id;

        productContractPayload.productParameters.invoicePaymentTermId = getTermsResponse.invoicePaymentTerms?.[0]?.id;
        productContractPayload.productParameters.productContractTermId = getProductResponse.productContractTerms?.[0]?.id;

        productContractPayload.basicParameters.productId = getProductResponse.id;
        productContractPayload.basicParameters.productVersionId = getProductResponse.version;
      
        // Determine which PODs will be added to this contract
        const podsToAdd = podIndex !== undefined 
            ? [this.responses.pod[podIndex]] 
            : this.responses.pod;

        // Calculate estimated consumption only for PODs being added to this contract
        let estimatedConsumption = 0;
        if(podsToAdd && podsToAdd.length > 0){
            let sum = 0;
            for(const i of podsToAdd){
                const podGet = await this.Request.get(`pod/${i.id}?version=1`);
                const podJson = await podGet.json();
                if(podJson.type === 'CONSUMER'){
                    sum += podJson.estimatedMonthlyAvgConsumption;
                }
            }
            estimatedConsumption = (sum * 12 / 1000);
        }

        productContractPayload.additionalParameters.estimatedTotalConsumptionUnderContractKwh = estimatedConsumption;

        // Add POD details to contract
        for (const i of podsToAdd) {
            const poddetail = {
                "pointOfDeliveryDetailId": i.podDetailId,
                "dealNumber": null
            };
            productContractPayload.productContractPointOfDeliveries.push(poddetail);
        }

        return productContractPayload;
    }

    public async edit_ProductContract(generatedPayload: ProductContractPayload) {
        const contractget = await this.Request.get(`product-contract/${this.responses.productContract[0].id}?version=1`);
        const contractgetjson = await contractget.json();

        // Group PODs by billingGroupId
        const podsByBillingGroup = new Map<number, any[]>();
        
        for(const pod of contractgetjson.contractPodsResponses) {
            const billingGroupId = pod.billingGroupId;
            if (!podsByBillingGroup.has(billingGroupId)) {
                podsByBillingGroup.set(billingGroupId, []);
            }
            podsByBillingGroup.get(billingGroupId)!.push({
                "pointOfDeliveryDetailId": pod.podDetailId,
                "dealNumber": pod.dealNumber
            });
        }

        // Convert grouped PODs to podRequests format
        const podRequests = Array.from(podsByBillingGroup.entries()).map(([billingGroupId, pods]) => ({
            "billingGroupId": billingGroupId,
            "productContractPointOfDeliveries": pods
        }));

        // Add all PODs to productContractPointOfDeliveries
        const allPods = contractgetjson.contractPodsResponses.map((pod: any) => ({
            "pointOfDeliveryDetailId": pod.podDetailId,
            "dealNumber": pod.dealNumber
        }));

        const editPayload = {
            ...generatedPayload,
            basicParameters: {
                ...generatedPayload.basicParameters,
                status: contractgetjson.basicParameters.status,
                subStatus: contractgetjson.basicParameters.subStatus,
            },

            productContractPointOfDeliveries: allPods,
            savingAsNewVersion: false,
            startDate: contractgetjson.versions[0].startDate,
            podRequests: podRequests
        };

        if (editPayload.additionalParameters) {
            editPayload.additionalParameters.riskAssessment = "PERMIT";
            editPayload.additionalParameters.employeeId = 154;
        }

        return editPayload;
    }

    public async addBillingGroup(contractIndex: number = 0) {
        const payload = billingGroup();
        const contractget = await this.Request.get(`product-contract/${this.responses.productContract[0].id}?version=1`);
        const contractgetjson = await contractget.json();
        const getBillingGroup = await this.Request.get(`billing-group/${contractgetjson.billingGroups[0].id}`);
        const getBillingGroupJson = await getBillingGroup.json();

        payload.contractId = this.responses.productContract[contractIndex].id;
        payload.sendingInvoice = getBillingGroupJson.sendingInvoice
        return payload;
    }

    public async editBillingGroup(billingGroupIndex: number = 0) {
        const contractget = await this.Request.get(`product-contract/${this.responses.productContract[0].id}?version=1`);
        const contractgetjson = await contractget.json();

        const getBillingGroup = await this.Request.get(`billing-group/${contractgetjson.billingGroups[billingGroupIndex].id}`);
        const getBillingGroupJson = await getBillingGroup.json();

        const billingGroupNumber = contractgetjson.billingGroups[billingGroupIndex].groupNumber
        const billingGroupId = contractgetjson.billingGroups[billingGroupIndex].id
        const payload = billingGroup();

        payload.contractId = this.responses.productContract[0].id;
        payload.id = billingGroupId;
        payload.groupNumber = billingGroupNumber;
        payload.sendingInvoice = getBillingGroupJson.sendingInvoice

        return payload;
    }

    public async addPodToBillingGroup(billingGroupIndex: number = 0, poddetailId: number) {
        const contractget = await this.Request.get(`product-contract/${this.responses.productContract[0].id}?version=1`);
        const contractgetjson = await contractget.json();
        const billingGroupId = contractgetjson.billingGroups[billingGroupIndex].id

        const addPod = {
            "billingGroupId": billingGroupId,
            "productContractPointOfDeliveries": [
                {
                    "pointOfDeliveryDetailId": poddetailId,
                    "dealNumber": null
                }
            ]
        }

        const podsToAdd = this.responses.pod;

        // Calculate estimated consumption only for PODs being added to this contract
        let estimatedConsumption = 0;
        if(podsToAdd && podsToAdd.length > 0){
            let sum = 0;
            for(const i of podsToAdd){
                const podGet = await this.Request.get(`pod/${i.id}?version=1`);
                const podJson = await podGet.json();
                if(podJson.type === 'CONSUMER'){
                    sum += podJson.estimatedMonthlyAvgConsumption;
                }
            }
            estimatedConsumption = (sum * 12 / 1000);
        }

        return {addPod, estimatedConsumption};

    }
    
    public async serviceContract() {
        const payload = Service_contract();
        const serviceget = await this.Request.get(`services/${this.responses.service[0]}?`);
        const serviceId = (await serviceget.json()).id;
        const serviceTermid = (await serviceget.json()).contractTerms[0].id;
        const priceGet = await this.Request.get(`price-components/${this.responses.priceComponent[0]}?`);
        const priceGetJson = await priceGet.json();

        const getTerms = await this.Request.get(`terms?id=${this.responses.terms[0].id}`);
        if (!getTerms.ok()) {
            const errorText = await getTerms.text();
            throw new Error(`Failed to get terms (${getTerms.status()}): ${errorText.substring(0, 200)}`);
        }
        const getTermsResponse = await getTerms.json();

        const getCustomer = await this.Request.get(`customer/${this.responses.customer[0].id}?version=1`);
        const getCustomerResponse = await getCustomer.json();

        payload.basicParameters.serviceId = serviceId;
        payload.basicParameters.customerId = getCustomerResponse.customerId;
        payload.basicParameters.communicationDataForBilling = getCustomerResponse.communicationData[0].id;
        payload.basicParameters.communicationDataForContract = getCustomerResponse.communicationData[0].id;

        payload.serviceParameters.invoicePaymentTermId = getTermsResponse.invoicePaymentTerms[0].id;
        payload.serviceParameters.contractTermId = serviceTermid;

        if (this.responses.pod) {
            for (const i of this.responses.pod) {
                payload.serviceParameters.podIds.push(i.id);
            }
        }

        if (this.responses.interim) {
            for (const i of this.responses.interim) {
                const interimGet = await this.Request.get(`iap/${i}?version=1`);
                const interimJson = await interimGet.json();
                const interimDetails = {
                    "issueDate": null,
                    "value": await interimJson.value,
                    "interimAdvancePaymentId": await interimJson.id,
                    "termValue":  await interimJson.interimAdvancePaymentTerm.value,
                    "contractFormulas": []
                }
                payload.serviceParameters.interimAdvancePaymentsRequests.push(interimDetails);
            }
        }
    
        if (priceGetJson.applicationModelResponse?.perPieceResponse?.ranges[0]){
            payload.serviceParameters.quantity = priceGetJson.applicationModelResponse.perPieceResponse.ranges[0].to;
        }
        return payload;
    }
    
    public async action() {
        const actionPayload = action();

        const getCustomer = await this.Request.get(`customer/${this.responses.customer[0].id}?version=1`);
        const getCustomerResponse = await getCustomer.json();
        
        actionPayload.contractId = this.responses.productContract[0].id;
        actionPayload.customerId = getCustomerResponse.customerId;
        actionPayload.penaltyId = this.responses.penalty[0].id;
        actionPayload.terminationId = this.responses.termination[0].id;
        return actionPayload;
    }

    public async claimedPenalty(customerIndex: number = 0, contractIndex: number = 0) {
        const payload = claimedPenalty();

        payload.customerDetailId = this.responses.customer[customerIndex].lastCustomerDetailId;

        const contractGet = await this.Request.get(`product-contract/${this.responses.productContract[contractIndex].id}?version=1`);
        const contractJson = await contractGet.json();
        payload.billingGroupId = contractJson.contractPodsResponses?.[0]?.billingGroupId
            ?? contractJson.billingGroups?.[0]?.id;

        return payload;
    }

    public async serviceOrder() {
        const payload = serviceOrder();
        const getCustomer = await this.Request.get(`customer/${this.responses.customer[0].id}?version=1`);
        const serviceget = await this.Request.get(`services/${this.responses.service[0]}?version=1`);
    
        const getTerms = await this.Request.get(`terms?id=${this.responses.terms[0].id}`);
        if (!getTerms.ok()) {
            const errorText = await getTerms.text();
            throw new Error(`Failed to get terms (${getTerms.status()}): ${errorText.substring(0, 200)}`);
        }
        const getTermsResponse = await getTerms.json();

        payload.basicParameters.customerDetailId = this.responses.customer[0].lastCustomerDetailId;
        payload.basicParameters.serviceDetailId = (await serviceget.json()).versions[0].detailId;
        payload.basicParameters.customerCommunicationIdForBilling = (await getCustomer.json()).communicationData?.[0]?.id;
        payload.serviceParameters.invoicePaymentTermId = getTermsResponse.invoicePaymentTerms?.[0]?.id;
        payload.serviceParameters.invoicePaymentTermValue = getTermsResponse.invoicePaymentTerms?.[0]?.value;
        if (this.responses.pod) {
            for (const i of this.responses.pod) {
                payload.serviceParameters.pods.push(i.id);
            }
        }

        return payload;
    }
    
    public async goodsOrder() {
        const payload = goodsOrder();

        const getCustomer = await this.Request.get(`customer/${this.responses.customer[0].id}?version=1`);
        const customerjson = await getCustomer.json();
        if (this.responses.goods) {
            for (const [index, i] of this.responses.goods.entries()) {
                const goodsget = await this.Request.get(`goods/${this.responses.goods[index].goodsId}?`);
                const goodsgetjson = await goodsget.json();
                const goods =   
                {
                    "id": null,
                    "goodsDetailId": goodsgetjson.goodsDetailsResponse.id,
                    "name": 'GOODS_ORDER',
                    "codeForConnectionWithOtherSystem": goodsgetjson.goodsDetailsResponse.codeForConnectionWithOtherSystem,
                    "goodsUnitId": goodsgetjson.goodsDetailsResponse.goodsUnits.id,
                    "quantity": "1",
                    "price": goodsgetjson.goodsDetailsResponse.price,
                    "currencyId": goodsgetjson.goodsDetailsResponse.currency.id,
                    "numberOfIncomingAccount": goodsgetjson.goodsDetailsResponse.incomeAccountNumbers,
                    "costCenterOrControllingOrder": goodsgetjson.goodsDetailsResponse.controllingOrderId,
                    "inputDisabled": true
                }
                payload.goodsParameters.goods.push(goods);
            }
        }
        payload.basicParameters.customerDetailId = this.responses.customer[0].lastCustomerDetailId;
        payload.basicParameters.customerCommunicationIdForBilling = customerjson.communicationData[0].id;
        return payload;
    }

    public async contractStatusChange(
        status: "SIGNED" | "DRAFT" | "READY" | "ENTERED_INTO_FORCE" | "TERMINATED" | "CANCELLED" | "ACTIVE_IN_PERPETUITY", 
        subStatus: string,
        versionStatus: String, 
        versionId: number) {

        const payload = {
            "contractStatus": status,
            "contractSubStatus": subStatus,
            "contractVersionStatus": versionStatus
        }
        const put = await this.Request.put(`product-contract/status-update/${this.responses.productContract[0].id}?versionId=${versionId}`, {data: payload});
        expect(put).CheckResponse();
    }
}