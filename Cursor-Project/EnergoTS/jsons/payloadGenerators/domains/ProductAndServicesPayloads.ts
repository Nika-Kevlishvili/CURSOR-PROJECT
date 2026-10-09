import { APIRequestContext } from "playwright";
import { baseFixture } from "../../../fixtures/baseFixture";
import { product_create, ProductPayload } from '../../payloads/create/productAndServices/product';
import { service } from '../../payloads/create/productAndServices/service';
import { Goods } from '../../payloads/create/productAndServices/goodsCreate';
import { priceParameter } from '../../payloads/create/productAndServices/priceParameter';
import { PriceSettlement } from '../../payloads/create/productAndServices/priceSettlement';
import { periodicalComponent } from '../../payloads/create/productAndServices/periodical';
import { priceCompByScales } from '../../payloads/create/productAndServices/componentByscales';
import { perPiece } from '../../payloads/create/productAndServices/perPiece';
import { electricity } from '../../payloads/create/productAndServices/electricity';
import { oneTime } from '../../payloads/create/productAndServices/oneTime';
import { groupComponent } from '../../payloads/create/productAndServices/groupComponent';
import { terms } from '../../payloads/create/productAndServices/terms';
import { groupOfTerms } from '../../payloads/create/productAndServices/groupOfTerms';
import { interim } from '../../payloads/create/productAndServices/interimExactAmount';
import { group_interim } from '../../payloads/create/productAndServices/groupInterims';
import { termination_payload } from '../../payloads/create/productAndServices/termination';
import { group_termination } from '../../payloads/create/productAndServices/groupTermination';
import { penalty } from '../../payloads/create/productAndServices/penalty';
import { groupPenalty } from '../../payloads/create/productAndServices/groupPenalty';
import { specialOffersTab_create } from '../../payloads/create/productAndServices/product';
import excelGens from '../../../mass-imports/generators/domains/priceParameterMP';
import fs from 'fs';

type priceParametertypes = 'FIFTEEN_MINUTES' | 'ONE_HOUR' | 'ONE_DAY' | 'ONE_MONTH';
type terminationTypes = 'EXPIRATION_OF_THE_NOTICE' | 'DEACTIVATION_OF_POINTS_OF_DELIVERY' | 'EXPIRATION_OF_THE_CONTRACT_TERM';
type priceDateRange = {
    startDate: string;
    endDate: string;
    amount: number;
};

export class ProductAndServicesPayloads {
    private Request: APIRequestContext;
    private responses: baseFixture['Responses'];
    private fileUploadRequest: APIRequestContext;

    constructor(apiRequestContext: APIRequestContext, responses: baseFixture['Responses'], fileUploadRequest: APIRequestContext) {
        this.Request = apiRequestContext;
        this.responses = responses;
        this.fileUploadRequest = fileUploadRequest;
    }

    /*     * Uses existing terms, interims, price components, penalties, and terminations from responses
     */
    public product(termIndex: number = 0) {
        const productPayload = product_create();

        if (this.responses.terms) {
            productPayload.termId = this.responses.terms[termIndex].id;
        } else {
            productPayload.termGroupId = this.responses.termsGroup[0].id;
        }

        if (this.responses.interim) {
            for (const i of this.responses.interim) {
                productPayload.interimAdvancePayments.push(i);
            }
        }

        if (this.responses.groupOfInterims) {
            for (const i of this.responses.groupOfInterims) {
                productPayload.interimAdvancePaymentGroups.push(i.id);
            }
        }

        if (this.responses.priceComponent) {
            for (const i of this.responses.priceComponent) {
                productPayload.priceComponentIds.push(i);
            }
        }
        
        if (this.responses.groupOfPriceComponents) {
            for (const i of this.responses.groupOfPriceComponents) {
                productPayload.priceComponentGroupIds.push(i.id);
            }
        }

        if (this.responses.penalty) {
            for (const i of this.responses.penalty) {
                productPayload.penaltyIds.push(i.id);
            }
        }

        if (this.responses.groupOfPenalties) {
            for (const i of this.responses.groupOfPenalties) {
                productPayload.penaltyGroupIds.push(i.id);
            }
        }

        if (this.responses.termination) {
            for (const i of this.responses.termination) {
                productPayload.terminationIds.push(i.id);
            }
        }

        if (this.responses.groupOfTerminations) {
            for (const i of this.responses.groupOfTerminations) {
                productPayload.terminationGroupIds.push(i.id);
            }
        }

        return productPayload;
    }

    public async specialOffersTab(resign: boolean = false, resignProductId: number = 0, customer: boolean = true, contract: boolean = true) {
        const payload = specialOffersTab_create();

        if (resign) {
            payload.resignable = true
            payload.resigningProducts.push(
                {
                    "productId": resignProductId,
                    "versionIds": [1]
                }
            )
        }

        if(customer && this.responses.customer.length > 0) {
            payload.applyToSpecificCustomers = true;
            payload.customers[0].customerId = this.responses.customer[0].id;
            payload.customers[0].customerDetailId = this.responses.customer[0].lastCustomerDetailId
        }

        if(contract && this.responses.productContract.length > 0) {
            const getContract = await this.Request.get(`product-contract/${this.responses.productContract[0].id}`)
            const getContractJson = await getContract.json();
            const productContractDetailId = getContractJson.versions[0].id

            payload.contracts[0].contractId = this.responses.productContract[0].id;
            payload.contracts[0].contractDetailId = productContractDetailId;
        }

        return payload;
    }
        

    public async edit_Product(generatedPayload: ProductPayload, productIndex: number = 0) {
        const productGet = await this.Request.get(`products/${this.responses.product[productIndex]}?version=1`);
        const productGetJson = await productGet.json();

        // Convert productTerms values from string to number and add fromGroup flag
        const updatedProductTerms = generatedPayload.productTerms.map(term => ({
            ...term,
            value: typeof term.value === 'string' ? parseInt(term.value) : term.value,
            fromGroup: false
        }));

        // If there are existing productTerms in the response, preserve their IDs
        if (productGetJson.productContractTerms && productGetJson.productContractTerms.length > 0) {
            updatedProductTerms.forEach((term, index) => {
                if (productGetJson.productContractTerms[index]) {
                    term.id = productGetJson.productContractTerms[index].id;
                }
            });
        }

        const editPayload = {
            ...generatedPayload,
            productTerms: updatedProductTerms,
            collectionChannelIds: generatedPayload.collectionChannelIds || [],
            version: productGetJson.version || 1,
            updateExistingVersion: true
        };

        return editPayload;
    }

    public service(termIndex: number = 0) { 
        const payload = service();
    
        if (this.responses.terms) {
            payload.term = this.responses.terms[termIndex].id;
        } else {
            payload.termGroup = this.responses.termsGroup[0].id;
        }

        if (this.responses.interim) {
            for (const i of this.responses.interim) {
                payload.interimAdvancePayments.push(i);
            }
        }

        if (this.responses.groupOfInterims) {
            for (const i of this.responses.groupOfInterims) {
                payload.interimAdvancePaymentGroups.push(i.id);
            }
        }

        if (this.responses.priceComponent) {
            for (const i of this.responses.priceComponent) {
                payload.priceComponents.push(i);
            }
        }
        if (this.responses.groupOfPriceComponents) {
            for (const i of this.responses.groupOfPriceComponents) {
                payload.priceComponentGroups.push(i.id);
            }
        }

        if (this.responses.penalty) {
            for (const i of this.responses.penalty) {
                payload.penalties.push(i.id);
            }
        }

        if (this.responses.groupOfPenalties) {
            for (const i of this.responses.groupOfPenalties) {
                payload.penaltyGroups.push(i.id);
            }
        }

        if (this.responses.termination) {
            for (const i of this.responses.termination) {
                payload.terminations.push(i.id);
            }
        }

        if (this.responses.groupOfTerminations) {
            for (const i of this.responses.groupOfTerminations) {
                payload.terminationGroups.push(i.id);
            }
        }
        return payload;
    }

    public goods() {
        let payload = Goods();
        return payload;
    }

    public async uploadPriceParameterFile(priceParameterId: number, priceType: string) {
        let filename = '';
        let periodTypeParam = '';
        
        if (priceType === 'MIN') {
            filename = '15min.xlsx';
            periodTypeParam = 'FIFTEEN_MINUTES';
        } else if (priceType === 'HOUR') {
            filename = '1h.xlsx';
            periodTypeParam = 'ONE_HOUR';
        } else if (priceType === 'DAY') {
            filename = '1D.xlsx';
            periodTypeParam = 'ONE_DAY';
        }else if (priceType === 'MONTH') {
            filename = '1M.xlsx';
            periodTypeParam = 'ONE_MONTH';
        }

        const fileBuffer = fs.readFileSync(filename);
        const fileUpload = await this.fileUploadRequest.post(`prices/import?periodType=${periodTypeParam}&priceParameterId=${priceParameterId}&priceParameterDetailsVersionId=1`, {
            multipart: {
                file: {
                    name: filename,
                    mimeType: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
                    buffer: fileBuffer
                }
            }
        });
        if (!fileUpload.ok()) {
            console.log('File upload failed:', fileUpload.status(), await fileUpload.text());
        }
    }

    private async generatePriceParameterWithExcel(
        type: priceParametertypes,
        dateRanges: priceDateRange[] | undefined,
        generatorMethod: 'generateFifteenMinutes' | 'generateHours' | 'generateDays',
        endTimeHours?: number,
        endTimeMinutes?: number
    ) {
        const priceParameterPayload = priceParameter();
        priceParameterPayload.periodType = type;
        
        // Use current month start and end dates
        const now = new Date();
        const monthStart = new Date(now.getFullYear(), now.getMonth(), 1);
        const monthEnd = new Date(now.getFullYear(), now.getMonth() + 1, 0);
        
        let startDate = monthStart.toISOString().split('T')[0];
        let endDate: string;
        
        if (endTimeHours !== undefined || endTimeMinutes !== undefined) {
            const endDateTime = new Date(monthEnd);
            endDateTime.setHours(endTimeHours ?? 23, endTimeMinutes ?? 0, 0, 0);
            endDate = endDateTime.toISOString();
        } else {
            endDate = `${monthEnd.toISOString().split('T')[0]}T23:59:59.999Z`;
        }
        
        const ranges = dateRanges && dateRanges.length > 0 
            ? dateRanges 
            : [{ startDate: `${startDate}T00:00:00.000Z`, endDate, amount: 100 }];
        
        const generator = new excelGens(ranges);
        await generator[generatorMethod]();
        
        return priceParameterPayload;
    }

    public async priceParameter15minute(dateRanges?: priceDateRange[]) {
        return this.generatePriceParameterWithExcel(
            'FIFTEEN_MINUTES',
            dateRanges,
            'generateFifteenMinutes',
            23,
            45
        );
    }

    public async priceParameter1Hour(dateRanges?: priceDateRange[]) {
        return this.generatePriceParameterWithExcel(
            'ONE_HOUR',
            dateRanges,
            'generateHours',
            23,
            0
        );
    }

    public async priceParameter1Day(dateRanges?: priceDateRange[]) {
        return this.generatePriceParameterWithExcel(
            'ONE_DAY',
            dateRanges,
            'generateDays'
        );
    }

    public  priceSettlement() {
        const payload = PriceSettlement();
        return payload;
    }

    public periodicalComponent() {
        const payload = periodicalComponent;
        return payload;
    }

    public scaleComponent() {
        const payload = priceCompByScales();
        return payload;
    }

    public perPiece() {
        const payload = perPiece;
        return payload;
    }

    public electricity() {
        const payload = electricity();
        payload.globalVatRate = false;
        payload.vatRateId = 1003;
        return payload;
    }

    public oneTime() {
        const payload = oneTime();
        return payload;
    }

    public groupComponent() {
        const payload = groupComponent;
        return payload;
    }

    public term() {
        const payload = terms();
        return payload;
    }

    public groupOfTerm() {
        const groupOfTermPayload = groupOfTerms();
        groupOfTermPayload.termsId = this.responses.terms[0].id;
        return groupOfTermPayload;
    }

    public interim() {
        const payload = interim();
        return payload;
    }

    public group_interim() {
        const payload = group_interim;
        return payload;
    }

    public termination(type: terminationTypes) {
        const payload = termination_payload();
        payload.event = type;
        return payload;
    }

    public group_termination() {
        const payload = group_termination;
        return payload;
    }

    public penalty() {
        const payload = penalty();
        return payload;
    }

    public groupPenalty() {
        const payload = groupPenalty;
        return payload;
    }
}