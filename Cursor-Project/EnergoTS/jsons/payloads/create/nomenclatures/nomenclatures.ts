import { baseNomenclature } from "./baseNomenclature";
import { APIRequestContext } from "@playwright/test";
import { randomGens } from "../../../../utils/randomGens";

export class nomenclatures extends baseNomenclature {
    constructor(request: APIRequestContext) {
        super(request);
    }

    public async Account_manager_types(prompt: string = '') {
        this.currentNomenclature = 'account_manager_types';
        this.currentNomenclatureUrlGet = `account-manager-types?statuses=ACTIVE&page=0&size=1${prompt ? `&prompt=${prompt}` : ''}`;
        this.currentNomenclatureUrlPost = 'account-manager-types';
        this.currentPayload = {
            "name": prompt ? prompt : `Account Manager Type ${this.generatedName}`,
            "status": "ACTIVE",
            "defaultSelection": false
        }

        const response = await this.checkNomenclature()
        this.nomenclaturesData[this.currentNomenclature] = response['id'];
        return response['id'];
    }

    public async action_type(prompt: string = '') {
        this.currentNomenclature = 'action_type';
        this.currentNomenclatureUrlGet = `action-types?statuses=ACTIVE&page=0&size=1${prompt ? `&prompt=${prompt}` : ''}`;
        this.currentNomenclatureUrlPost = 'action-types';
        this.currentPayload = {
            "name": prompt ? prompt : `action_type ${this.generatedName}`,
            "status": "ACTIVE",
            "defaultSelection": false
        }

        const response = await this.checkNomenclature()
        this.nomenclaturesData[this.currentNomenclature] = response.id;
        return response.id;
    }

    public async activity(prompt: string = '') {
        this.currentNomenclature = 'activity';
        this.currentNomenclatureUrlGet = `nomenclature/activities/filter?statuses=ACTIVE&page=0&size=1${prompt ? `&prompt=${prompt}` : ''}`;
        this.currentNomenclatureUrlPost = 'activities';
        this.currentPayload = {
            "name": prompt ? prompt : `activity ${this.generatedName}`,
            "status": "ACTIVE",
            "defaultSelection": false
        }

        const response = await this.checkNomenclature()
        this.nomenclaturesData[this.currentNomenclature] = response['id'];
        return response['id'];
    }

    public async banks(prompt: string = '') {
        
        this.currentNomenclature = 'banks';
        this.currentNomenclatureUrlGet = `banks?statuses=ACTIVE&page=0&size=1${prompt ? `&prompt=${prompt}` : ''}`;
        this.currentNomenclatureUrlPost = 'banks';
        this.currentPayload = {
            "name": prompt ? prompt : `bank ${this.generatedName}`,
            "bic": `${this.generatedName}`,
            "status": "ACTIVE",
            "defaultSelection": false
        }

        const response = await this.checkNomenclature()
        this.nomenclaturesData[this.currentNomenclature] = response['id'];
        return response['id'];
    }

    public async base_interest_rate(prompt: string = '') {
        this.currentNomenclature = 'base_interest_rate';
        this.currentNomenclatureUrlGet = `base-interest-rates?statuses=ACTIVE&page=0&size=1${prompt ? `&prompt=${prompt}` : ''}`;
        this.currentNomenclatureUrlPost = 'base-interest-rates';
        this.currentPayload = {
            "name": prompt ? prompt : `base interest rate ${this.generatedName}`,
            "status": "ACTIVE",
            "defaultSelection": false,
            "percentageRate": 100,
            "dateFrom": randomGens.generateTodaysDate('yyyy-mm-dd'),
        }

        const response = await this.checkNomenclature()
        this.nomenclaturesData[this.currentNomenclature] = response.id;
        return response.id;
    }

    public async belonging_capital_owners(prompt: string = '') {
        this.currentNomenclature = 'belonging_capital_owners';
        this.currentNomenclatureUrlGet = `belonging-capital-owners?statuses=ACTIVE&page=0&size=1${prompt ? `&prompt=${prompt}` : ''}`;
        this.currentNomenclatureUrlPost = 'belonging-capital-owners';
        this.currentPayload = {
            "name": prompt ? prompt : `belonging capital owners ${this.generatedName}`,
            "status": "ACTIVE",
            "defaultSelection": false
        }

        const response = await this.checkNomenclature()
        this.nomenclaturesData[this.currentNomenclature] = response['id'];
        return response['id'];
    }

    public async calendar(prompt: string = '') {
        this.currentNomenclature = 'calendar';
        this.currentNomenclatureUrlGet = `calendar?statuses=ACTIVE&page=0&size=1${prompt ? `&prompt=${prompt}` : ''}`;
        this.currentNomenclatureUrlPost = 'calendar';
        this.currentPayload = {
            "name": prompt ? prompt : `calendar ${this.generatedName}`,
            "weekends": [
                "SATURDAY",
                "SUNDAY"
            ],
            "holidays": [],
            "defaultSelection": false,
            "status": "ACTIVE"
        }

        const calendarKey = this.currentNomenclature;
        const response = await this.checkNomenclature()
        // Capture key before await to avoid race with concurrent checkNomenclature restorations
        this.nomenclaturesData[calendarKey] = {'id': response['id'], 'orderingId': response['orderingId']};
        return response['id'];
    }

    public async sub_activity(prompt: string = '') {
        this.currentNomenclature = 'sub_activity';
        this.currentNomenclatureUrlGet = `nomenclature/sub-activities/filter?statuses=ACTIVE&page=0&size=1${prompt ? `&prompt=${prompt}` : ''}`
        this.currentNomenclatureUrlPost = 'sub-activities';
        this.currentPayload = {
            "name": prompt ? prompt : `subActivity ${this.generatedName}`,
            "activityId": this.nomenclaturesData['activity'],
            "fields": [
                {
                    "title": "r",
                    "fieldType": "TEXT_AREA",
                    "maxLength": 2,
                    "mandatory": false,
                    "ordering": 3,
                    "regexp": [
                        "ALL"
                    ]
                }
            ],
            "status": "ACTIVE",
            "defaultSelection": false
        }

        const response = await this.checkNomenclature()
        this.nomenclaturesData[this.currentNomenclature] = response['id'];
        return response['id'];
    }

    public async campaign(prompt: string = '') {
        this.currentNomenclature = 'campaign';
        this.currentNomenclatureUrlGet = `campaigns?statuses=ACTIVE&page=0&size=1${prompt ? `&prompt=${prompt}` : ''}`
        this.currentNomenclatureUrlPost = 'campaigns';
        this.currentPayload = {
            "name": prompt ? prompt : `campaign ${this.generatedName}`,
            "status": "ACTIVE",
            "defaultSelection": false
        }

        const response = await this.checkNomenclature()
        this.nomenclaturesData[this.currentNomenclature] = response['id'];
        return response['id'];
    }

    public async contract_version_types(prompt: string = '') {
        this.currentNomenclature = 'contract_version_types';
        this.currentNomenclatureUrlGet = `contract-version-types?statuses=ACTIVE&page=0&size=1${prompt ? `&prompt=${prompt}` : ''}`
        this.currentNomenclatureUrlPost = 'contract-version-types';
        this.currentPayload = {
            "name": prompt ? prompt : `contract version types ${this.generatedName}`,
            "status": "ACTIVE",
            "defaultSelection": false
        }


        const response = await this.checkNomenclature()
        this.nomenclaturesData[this.currentNomenclature] = response['id'];
        return response['id'];
    }

    public async sales_area(prompt: string = '') {
        this.currentNomenclature = 'sales_area';
        this.currentNomenclatureUrlGet = `nomenclature/sales-areas/filter?statuses=ACTIVE&page=0&size=1${prompt ? `&prompt=${prompt}` : ''}`
        this.currentNomenclatureUrlPost = 'sales-areas';
        this.currentPayload = {
            "name": prompt ? prompt : `sales area ${this.generatedName}`,
            "status": "ACTIVE",
            "defaultSelection": false
        }

        const response = await this.checkNomenclature()
        this.nomenclaturesData[this.currentNomenclature] = response['id'];
        return response['id'];
    }

    public async sales_channel(prompt: string = '') {
        this.currentNomenclature = 'sales_channel';
        this.currentNomenclatureUrlGet = `nomenclature/sales-channels/filter?statuses=ACTIVE&page=0&size=1${prompt ? `&prompt=${prompt}` : ''}`
        this.currentNomenclatureUrlPost = 'sales-channels';
        this.currentPayload = {
            "name": prompt ? prompt : `sales channel ${this.generatedName}`,
            "status": "ACTIVE",
            "defaultSelection": false
        }

        const response = await this.checkNomenclature()
        this.nomenclaturesData[this.currentNomenclature] = response['id'];
        return response['id'];
    }

    public async balancing_group_coordinator(prompt: string = '') {
        this.currentNomenclature = 'balancing_group_coordinator';
        this.currentNomenclatureUrlGet = `balancing-group-coordinators?statuses=ACTIVE&page=0&size=1${prompt ? `&prompt=${prompt}` : ''}`
        this.currentNomenclatureUrlPost = 'balancing-group-coordinators';
        this.currentPayload = {
            "name": prompt ? prompt : `balancing group coordinator ${this.generatedName}`,
            "status": "ACTIVE",
            "fullName": prompt ? prompt : `balancing group coordinator ${this.generatedName}`,
            "defaultSelection": false
        }

        const response = await this.checkNomenclature()
        this.nomenclaturesData[this.currentNomenclature] = response['id'];
        return response['id'];
    }


    public async countries(prompt: string = 'standard') {
        this.currentNomenclature = 'countries';
        this.currentNomenclatureUrlGet = `nomenclature/countries/filter?statuses=ACTIVE&statuses=INACTIVE&page=0&size=1&prompt=${prompt}`
        this.currentNomenclatureUrlPost = 'countries';
        this.currentPayload = {
            "name": prompt === 'standard' ? `standard country` : prompt,
            "nameTransliterated": prompt === 'standard' ? `standard country` : prompt,
            "status": "ACTIVE",
            "defaultSelection": false
        }

        const response = await this.checkNomenclature()
        this.nomenclaturesData[this.currentNomenclature] = response['id'];
        return response['id'];
    }

    public async regions(prompt: string = 'standard') {
        this.currentNomenclature = 'regions';
        this.currentNomenclatureUrlGet = `nomenclature/regions/filter?statuses=ACTIVE&statuses=INACTIVE&page=0&size=1&prompt=${prompt}`
        this.currentNomenclatureUrlPost = 'regions';
        this.currentPayload = {
            "name": prompt === 'standard' ? `standard region` : prompt,
            "nameTransliterated": prompt === 'standard' ? `standard region` : prompt,
            "countryId": this.nomenclaturesData['countries'],
            "status": "ACTIVE",
            "defaultSelection": false
        }

        const response = await this.checkNomenclature()
        this.nomenclaturesData[this.currentNomenclature] = response['id'];
        return response['id'];
    }

    public async municipalities(prompt: string = 'standard') {
        this.currentNomenclature = 'municipalities';
        this.currentNomenclatureUrlGet = `nomenclature/municipalities/filter?statuses=ACTIVE&statuses=INACTIVE&page=0&size=1&prompt=${prompt}`
        this.currentNomenclatureUrlPost = 'municipalities';
        this.currentPayload = {
            "name": prompt === 'standard' ? `standard municipality` : prompt,
            "nameTransliterated": prompt === 'standard' ? `standard municipality` : prompt,
            "regionId": this.nomenclaturesData['regions'],
            "status": "ACTIVE",
            "defaultSelection": false
        }

        const response = await this.checkNomenclature()
        this.nomenclaturesData[this.currentNomenclature] = response['id'];
        return response['id'];
    }

    public async populated_places(prompt: string = 'standard') {
        this.currentNomenclature = 'population_places';
        this.currentNomenclatureUrlGet = `nomenclature/populated-places/filter?statuses=ACTIVE&statuses=INACTIVE&page=0&size=25&prompt=${prompt}`
        this.currentNomenclatureUrlPost = 'populated-places';
        this.currentPayload = {
            "name": prompt === 'standard' ? `standard populated place` : prompt,
            "nameTransliterated": prompt === 'standard' ? `standard populated place` : prompt,
            "municipalityId": this.nomenclaturesData['municipalities'],
            "status": "ACTIVE",
            "defaultSelection": false
        }

        const response = await this.checkNomenclature()
        this.nomenclaturesData[this.currentNomenclature] = response['id'];
        return response['id'];
    }

    public async zip_codes(prompt: string = 'standard') {
        this.currentNomenclature = 'zip_codes';
        this.currentNomenclatureUrlGet = `nomenclature/zip-codes/filter?statuses=ACTIVE&statuses=INACTIVE&page=0&size=1&prompt=${prompt}`
        this.currentNomenclatureUrlPost = 'zip-codes';
        this.currentPayload = {
            "name": prompt === 'standard' ? `standard zip code` : prompt,
            "nameTransliterated": prompt === 'standard' ? `standard zip code` : prompt,
            "populatedPlaceId": this.nomenclaturesData['population_places'],
            "status": "ACTIVE",
            "defaultSelection": false
        }

        const response = await this.checkNomenclature()
        this.nomenclaturesData[this.currentNomenclature] = response['id'];
        return response['id'];
    }

    public async districts(prompt: string = 'standard') {
        this.currentNomenclature = 'districts';
        this.currentNomenclatureUrlGet = `nomenclature/districts/filter?statuses=ACTIVE&statuses=INACTIVE&page=0&size=1&prompt=${prompt}`
        this.currentNomenclatureUrlPost = 'districts';
        this.currentPayload = {
            "name": prompt === 'standard' ? `standard district` : prompt,
            "nameTransliterated": prompt === 'standard' ? `standard district` : prompt,
            "populatedPlaceId": this.nomenclaturesData['population_places'],
            "status": "ACTIVE",
            "defaultSelection": false
        }

        const response = await this.checkNomenclature()
        this.nomenclaturesData[this.currentNomenclature] = response['id'];
        return response['id'];
    }

    public async document_expirationf_period(prompt: string = ''){
        this.currentNomenclature = 'document_expiration_period';
        this.currentNomenclatureUrlGet = `expiration-periods?statuses=ACTIVE&page=0&size=1${prompt ? `&prompt=${prompt}` : ''}`
        this.currentNomenclatureUrlPost = 'expiration-periods';
        this.currentPayload = {
            "name": prompt ? prompt : `document expiration period ${this.generatedName}`,
            "status": "ACTIVE",
            "defaultSelection": false
        }

        const response = await this.checkNomenclature()
        this.nomenclaturesData[this.currentNomenclature] = response['id'];
        return response['id'];
    }

    public async residential_areas(prompt: string = 'standard') {
        this.currentNomenclature = 'residential_areas';
        this.currentNomenclatureUrlGet = `nomenclature/residential-areas/filter?statuses=ACTIVE&page=0&size=1&prompt=${prompt}`
        this.currentNomenclatureUrlPost = 'residential-areas';
        this.currentPayload = {
            "name": prompt === 'standard' ? `residential area ${this.generatedName}` : prompt,
            "nameTransliterated": prompt === 'standard' ? `standard residential area` : prompt,
            "type": "QUARTER",
            "populatedPlaceId": this.nomenclaturesData['population_places'],
            "status": "ACTIVE",
            "defaultSelection": false
        }

        const response = await this.checkNomenclature()
        this.nomenclaturesData[this.currentNomenclature] = response['id'];
        return response['id'];
    }

    public async streets(prompt: string = 'standard') {
        this.currentNomenclature = 'streets';
        this.currentNomenclatureUrlGet = `nomenclature/streets/filter?statuses=ACTIVE&page=0&size=1&prompt=${prompt}`
        this.currentNomenclatureUrlPost = 'streets';
        this.currentPayload = {
            "name": prompt === 'standard' ? `street ${this.generatedName}` : prompt,
            "nameTransliterated": prompt === 'standard' ? `standard street` : prompt,
            "type": "STREET",
            "populatedPlaceId": this.nomenclaturesData['population_places'],
            "status": "ACTIVE",
            "defaultSelection": false
        }

        const response = await this.checkNomenclature()
        this.nomenclaturesData[this.currentNomenclature] = response['id'];
        return response['id'];
    }

    public async credit_rating(prompt: string = '') {
        this.currentNomenclature = 'credit_rating';
        this.currentNomenclatureUrlGet = `credit-rating?statuses=ACTIVE&page=0&size=1${prompt ? `&prompt=${prompt}` : ''}`
        this.currentNomenclatureUrlPost = 'credit-rating';
        this.currentPayload = {
            "name": prompt ? prompt : `credit rating ${this.generatedName}`,
            "status": "ACTIVE",
            "defaultSelection": false
        }

        const response = await this.checkNomenclature()
        this.nomenclaturesData[this.currentNomenclature] = response['id'];
        return response['id'];
    }

    public async currency(prompt: string = '') {
        this.currentNomenclature = 'currency';
        const urlGet = `currencies?statuses=ACTIVE&page=0&size=1${prompt ? `&prompt=${prompt}` : ''}`;
        this.currentNomenclatureUrlGet = urlGet;

        let response = await this.request.get(urlGet);
        let responseJson = await response.json();

        this.nomenclaturesData['currency'] = responseJson['content'][0].id;
        return responseJson['content'][0].id;
    }

    public async deactivation_reason(prompt: string = '') {
        this.currentNomenclature = 'deactivation_reason';
        this.currentNomenclatureUrlGet = `deactivation-purpose?statuses=ACTIVE&page=0&size=1${prompt ? `&prompt=${prompt}` : ''}`
        this.currentNomenclatureUrlPost = 'deactivation-purpose';
        this.currentPayload = {
            "name": prompt ? prompt : `deactivation purpose ${this.generatedName}`,
            "status": "ACTIVE",
            "defaultSelection": false
        }

        const response = await this.checkNomenclature()
        this.nomenclaturesData[this.currentNomenclature] = response['id'];
        return response['id'];
    }

    public async reason_for_disconnection(prompt: string = '') {
        this.currentNomenclature = 'reason_for_disconnection';
        this.currentNomenclatureUrlGet = `nomenclature/reason-for-disconnection/filter?statuses=ACTIVE&statuses=INACTIVE&page=0&size=1${prompt ? `&prompt=${prompt}` : ''}`
        this.currentNomenclatureUrlPost = 'reason-for-disconnection';
        this.currentPayload = {
            "name": prompt ? prompt : `Reason for disconnection ${this.generatedName}`,
            "status": "ACTIVE",
            "defaultSelection": false
        }

        const response = await this.checkNomenclature()
        this.nomenclaturesData[this.currentNomenclature] = response['id'];
        return response['id'];
    }


    public async taxes_for_grid_operator(prompt: string = 'ЕСО ЕАД', documentTemplateId: number, emailTemplateId: number) {
        this.currentNomenclature = 'taxes_for_grid_operator';
        this.currentNomenclatureUrlGet = `tax-for-the-grid-operator?statuses=ACTIVE&page=0&size=1${prompt ? `&prompt=${prompt}` : ''}`
        this.currentNomenclatureUrlPost = 'tax-for-the-grid-operator';

        this.currentPayload = {
            "disconnectionType": prompt ? prompt : `${this.generatedName}`,
            "gridOperator": this.nomenclaturesData['grid_operator'],
            "documentTemplateId": documentTemplateId,
            "emailTemplateId": emailTemplateId,
            "numberOfIncomeAccount": "r",
            "basisForIssuing": null,
            "supplierType": "CURRENT",
            "costCenterControllingOrder": "g",
            "priceComponentOrPriceComponentGroupOrItem": null,
            "taxForReconnection": "1",
            "taxForExpressReconnection": "2",
            "currency": this.nomenclaturesData['currency'],
            "removeTaxInCancel": false,
            "defaultForPodWithMeasurementTypeSlp": false,
            "defaultForPodWithMeasurementTypeBySettlementPeriod": true,
            "inactiveCheckbox": false,
            "status": "ACTIVE",
            "defaultSelection": false
        }

        const response = await this.checkNomenclature()
        this.nomenclaturesData[this.currentNomenclature] = response.id;
        return response.id;
    }

    public async risk_assesment(prompt: string = '') {
        this.currentNomenclature = 'risk_assesment';
        this.currentNomenclatureUrlGet = `nomenclature/risk-assessment/filter?statuses=ACTIVE&page=0&size=1${prompt ? `&prompt=${prompt}` : ''}`
        this.currentNomenclatureUrlPost = 'risk-assessment';
        this.currentPayload = {
            "name": prompt ? prompt : `risk assessment ${this.generatedName}`,
            "status": "ACTIVE",
            "defaultSelection": false
        }

        const response = await this.checkNomenclature()
        this.nomenclaturesData[this.currentNomenclature] = response['id'];
        return response['id'];
    }

    public async email_mailboxes(prompt: string = '') {
        this.currentNomenclature = 'email_mailboxes';
        this.currentNomenclatureUrlGet = `email-mailboxes?statuses=ACTIVE&page=0&size=1${prompt ? `&prompt=${prompt}` : ''}`
        this.currentNomenclatureUrlPost = 'email-mailboxes';
        this.currentPayload = {
            "name": prompt ? prompt : `email mailbox ${this.generatedName}`,
            "status": "ACTIVE",
            "defaultSelection": false,
            "emailAddress": randomGens.generateRandomEMail(),
            "emailForSendingInvoices": false,
            "emailForGridOperator": false,
            "communicationForContract": false
        }

        const response = await this.checkNomenclature()
        this.nomenclaturesData[this.currentNomenclature] = response['id'];
        return response['id'];
    }

    public async pod_additional_parameters(prompt: string = '') {
        this.currentNomenclature = 'pod_additional_parameters';
        this.currentNomenclatureUrlGet = `nomenclature/pod-additional-parameters/filter?statuses=ACTIVE&statuses=INACTIVE&page=0&size=1${prompt ? `&prompt=${prompt}` : ''}`
        this.currentNomenclatureUrlPost = 'pod-additional-params';
        this.currentPayload = {
            "name": prompt ? prompt : `pod additional parameter ${this.generatedName}`,
            "status": "ACTIVE",
            "defaultSelection": false
        }

        const response = await this.checkNomenclature()
        this.nomenclaturesData[this.currentNomenclature] = response['id'];
        return response['id'];
    }



    public async economic_branch_according_to_national_classification(prompt: string = '') {
        this.currentNomenclature = 'economic_branch_according_to_national_classification';
        this.currentNomenclatureUrlGet = `economic-branch-ncea?statuses=ACTIVE&page=0&size=1${prompt ? `&prompt=${prompt}` : ''}`
        this.currentNomenclatureUrlPost = 'economic-branch-ncea';
        this.currentPayload = {
            "name": prompt ? prompt : `economic branch ${this.generatedName}`,
            "status": "ACTIVE",
            "defaultSelection": false
        }

        const response = await this.checkNomenclature()
        this.nomenclaturesData[this.currentNomenclature] = response['id'];
        return response['id'];
    }

    public async Economic_branch_based_on_commercial_information(prompt: string = '') {
        this.currentNomenclature = 'economic_branch_based_on_commercial_information';
        this.currentNomenclatureUrlGet = `economic-branch-ci?statuses=ACTIVE&page=0&size=1${prompt ? `&prompt=${prompt}` : ''}`
        this.currentNomenclatureUrlPost = 'economic-branch-ci';
        this.currentPayload = {
            "name": prompt ? prompt : `economic branch ${this.generatedName}`,
            "status": "ACTIVE",
            "defaultSelection": false
        }

        const response = await this.checkNomenclature()
        this.nomenclaturesData[this.currentNomenclature] = response['id'];
        return response['id'];
    }

    public async external_intermediary(prompt: string = '') {
        this.currentNomenclature = 'external_intermediary';
        this.currentNomenclatureUrlGet = `external-intermediaries?statuses=ACTIVE&page=0&size=1${prompt ? `&prompt=${prompt}` : ''}`
        this.currentNomenclatureUrlPost = 'external-intermediaries';
        this.currentPayload = {
            "name": prompt ? prompt : `external intermediary ${this.generatedName}`,
            "identifier": prompt ? prompt : `${this.generatedName}`,
            "status": "ACTIVE",
            "defaultSelection": false
        }

        const response = await this.checkNomenclature()
        this.nomenclaturesData[this.currentNomenclature] = response['id'];
        return response['id'];
    }

    public async form_of_ownership(prompt: string = '') {
        this.currentNomenclature = 'form_of_ownership';
        this.currentNomenclatureUrlGet = `ownership-form?statuses=ACTIVE&page=0&size=1${prompt ? `&prompt=${prompt}` : ''}`
        this.currentNomenclatureUrlPost = 'ownership-form';
        this.currentPayload = {
            "name": prompt ? prompt : `form of ownership ${this.generatedName}`,
            "status": "ACTIVE",
            "defaultSelection": false
        }

        const response = await this.checkNomenclature()
        this.nomenclaturesData[this.currentNomenclature] = response['id'];
        return response['id'];
    }

    public async goods_group(prompt: string = '') {
        this.currentNomenclature = 'goods_group';
        this.currentNomenclatureUrlGet = `goods-groups?statuses=ACTIVE&page=0&size=1${prompt ? `&prompt=${prompt}` : ''}`
        this.currentNomenclatureUrlPost = 'goods-groups';
        this.currentPayload = {
            "name": prompt ? prompt : `goods group ${this.generatedName}`,
            "nameTransliterated": prompt ? prompt : `goods group ${this.generatedName}`,
            "status": "ACTIVE",
            "defaultSelection": false
        }

        const response = await this.checkNomenclature()
        this.nomenclaturesData[this.currentNomenclature] = response['id'];
        return response['id'];
    }

    public async goods_supplier(prompt: string = '') {
        this.currentNomenclature = 'goods_supplier';
        this.currentNomenclatureUrlGet = `goods-suppliers?statuses=ACTIVE&page=0&size=1${prompt ? `&prompt=${prompt}` : ''}`
        this.currentNomenclatureUrlPost = 'goods-suppliers';
        this.currentPayload = {
            "name": prompt ? prompt : "string",
            "identifier": prompt ? prompt : `${randomGens.generateRandomString(false, true, 8)}`,
            "status": "ACTIVE",
            "defaultSelection": true
        }

        const response = await this.checkNomenclature()
        this.nomenclaturesData[this.currentNomenclature] = response.id;
        return response.id;
    }

    public async goods_unit(prompt: string = '') {
        this.currentNomenclature = 'goods_unit';
        this.currentNomenclatureUrlGet = `goods-units?statuses=ACTIVE&page=0&size=1${prompt ? `&prompt=${prompt}` : ''}`
        this.currentNomenclatureUrlPost = 'goods-units';
        this.currentPayload = {
            "name": prompt ? prompt : `goods unit ${this.generatedName}`,
            "status": "ACTIVE",
            "defaultSelection": false
        }

        const response = await this.checkNomenclature()
        this.nomenclaturesData[this.currentNomenclature] = response['id'];
        return response['id'];
    }

    public async grid_operator(prompt: string = 'ЕСО ЕАД') {
        this.currentNomenclature = 'grid_operator';
        this.currentNomenclatureUrlGet = `nomenclature/grid-operators/filter?statuses=ACTIVE&statuses=INACTIVE&page=0&size=1&${prompt ? `prompt=${prompt}` : ''}`;
        this.currentNomenclatureUrlPost = 'grid-operators';
        this.currentPayload = {
            "name": prompt? prompt:`grid operator ${this.generatedName}`,
            "status": "ACTIVE",
            "defaultSelection": false,
            "fullName": "asf",
        }

        const response = await this.checkNomenclature()
        this.nomenclaturesData[this.currentNomenclature] = response['id'];
        return response['id'];
    }

    public async income_account_name(prompt: string = '') {
        this.currentNomenclature = 'income_account_number';
        this.currentNomenclatureUrlGet = `nomenclature/income-account-number/filter?statuses=ACTIVE&statuses=INACTIVE&page=0&size=1${prompt ? `&prompt=${prompt}` : ''}`
        this.currentNomenclatureUrlPost = 'income-account-number';
        this.currentPayload = {
            "name": prompt ? prompt : `income account name ${this.generatedName}`,
            "number": prompt ? prompt : `income account number ${this.generatedName}`,
            "status": "ACTIVE",
            "defaultAssignmentType": []
        }

        const response = await this.checkNomenclature()
        this.nomenclaturesData[this.currentNomenclature] = response['id'];
        return response['id'];
    }

    //todo - add perfomer id get request
    public async task_type(prompt: string = '') {
        const performer = await this.request.get('manager-tags/all?page=0&size=25&withGroupPrefix=true&excludedItemId=0&prompt=Phoenix.Test&statuses=ACTIVE')
        const performerId = (await performer.json())['content'][0]['id'];
        const performerType = (await performer.json())['content'][0]['performerType'];

        this.currentNomenclature = 'task_type';
        this.currentNomenclatureUrlGet = `nomenclature/task-types/filter?statuses=ACTIVE&page=0&size=1${prompt ? `&prompt=${prompt}` : ''}`
        this.currentNomenclatureUrlPost = 'task-types';
        this.currentPayload = {
            "name": prompt ? prompt : `task type ${this.generatedName}`,
            "calendarId": this.nomenclaturesData['calendar'],
            "taskTypeStages": [
                {
                    "stage": 1,
                    "term": null,
                    "termType": "CALENDAR_DAYS",
                    "performerId": performerId,
                    "performerType": performerType
                }
            ],
            "status": "ACTIVE",
            "defaultSelection": false
        }

        const response = await this.checkNomenclature()
        this.nomenclaturesData[this.currentNomenclature] = response.id;
        return response.id;
    }

    public async preferences(prompt: string = '') {
        this.currentNomenclature = 'preferences';
        this.currentNomenclatureUrlGet = `nomenclature/preferences/filter?statuses=ACTIVE&statuses=INACTIVE&page=0&size=1${prompt ? `&prompt=${prompt}` : ''}`
        this.currentNomenclatureUrlPost = 'preferences';
        this.currentPayload = {
            "name": prompt ? prompt : `preference ${this.generatedName}`,
            "status": "ACTIVE",
            "defaultSelection": false
        }

        const response = await this.checkNomenclature()
        this.nomenclaturesData[this.currentNomenclature] = response['id'];
        return response['id'];
    }

    public async prefixes() {
        this.currentNomenclatureUrlGet = 'prefixes?statuses=ACTIVE&page=0&size=100'
        let response = await this.request.get(this.currentNomenclatureUrlGet)
        let responseJson = await response.json();
        const reponseContent = await responseJson['content'];

        this.nomenclaturesData['prefix_late_payment_fine'] = reponseContent[0]['id'];
        this.nomenclaturesData['prefix_service_goods_order'] = reponseContent[1]['id'];
        this.nomenclaturesData['prefix_product_credit_note'] = reponseContent[2]['id'];
        this.nomenclaturesData['prefix_service_interim_advance_payment_invoice'] = reponseContent[3]['id'];
        this.nomenclaturesData['prefix_service_goods_credit_note'] = reponseContent[4]['id'];
        this.nomenclaturesData['prefix_service_goods_invoice'] = reponseContent[5]['id'];
        this.nomenclaturesData['prefix_product_interim_advance_payment_invoice'] = reponseContent[6]['id'];
        this.nomenclaturesData['prefix_product_debit_note'] = reponseContent[7]['id'];
        this.nomenclaturesData['prefix_product_invoice'] = reponseContent[8]['id'];
        this.nomenclaturesData['prefix_invoice_cancellation_protocol'] = reponseContent[9]['id'];
        this.nomenclaturesData['prefix_action'] = reponseContent[10]['id'];
        this.nomenclaturesData['prefix_deposit'] = reponseContent[11]['id'];
    }

    public async platform(prompt: string = '') {
        this.currentNomenclature = 'platform';
        this.currentNomenclatureUrlGet = `nomenclature/platforms/filter?statuses=ACTIVE&statuses=INACTIVE&page=0&size=1${prompt ? `&prompt=${prompt}` : ''}`
        this.currentNomenclatureUrlPost = 'platforms';
        this.currentPayload = {
            "name": prompt ? prompt : `platform ${this.generatedName}`,
            "status": "ACTIVE",
            "defaultSelection": false
        }

        const response = await this.checkNomenclature()
        this.nomenclaturesData[this.currentNomenclature] = response['id'];
        return response['id'];
    }

    public async contact_purpose(prompt: string = 'Комуникация по договор') {
        this.currentNomenclature = 'contact_purpose';
        this.currentNomenclatureUrlGet = `contact-purpose?statuses=ACTIVE&page=0&size=1&prompt=${prompt}`
        this.currentNomenclatureUrlPost = 'contact-purpose';
        this.currentPayload = {
            "name": prompt ? prompt : `contact purpose ${this.generatedName}`,
            "status": "ACTIVE",
            "defaultSelection": false
        }
        

        const response = await this.checkNomenclature()
        this.nomenclaturesData[this.currentNomenclature] = response['id'];
        return response['id'];
    }

     public async billing_purpose(prompt: string = 'Фактуриране и плащане') {
        this.currentNomenclature = 'billing_purpose';
        this.currentNomenclatureUrlGet = `contact-purpose?statuses=ACTIVE&page=0&size=1&prompt=${prompt}`

        const response = await this.checkNomenclature()
        this.nomenclaturesData[this.currentNomenclature] = response['id'];
        return response['id'];
    }

    public async segment(prompt: string = '') {
        this.currentNomenclature = 'segment';
        this.currentNomenclatureUrlGet = `nomenclature/segments/filter?statuses=ACTIVE&page=0&size=1${prompt ? `&prompt=${prompt}` : ''}`
        this.currentNomenclatureUrlPost = 'segments';
        this.currentPayload = {
            "name": prompt ? prompt : `segment ${this.generatedName}`,
            "status": "ACTIVE",
            "defaultSelection": false
        }

        const response = await this.checkNomenclature()
        this.nomenclaturesData[this.currentNomenclature] = response['id'];
        return response['id'];
    }

    public async legal_forms(prompt: string = '') {
        this.currentNomenclature = 'legal_forms';
        this.currentNomenclatureUrlGet = `legal-forms?statuses=ACTIVE&page=0&size=1${prompt ? `&prompt=${prompt}` : ''}`
        this.currentNomenclatureUrlPost = 'legal-forms';
        this.currentPayload = {
            "name": prompt ? prompt : `legal form ${this.generatedName}`,
            "description": "Description",
            "legalFormsTransliterated": [
                {
                    "name": "Transliterated name",
                    "description": "Transliterated description"
                }
            ],
            "status": "ACTIVE",
            "defaultSelection": false
        }

        const response = await this.checkNomenclature()
        this.nomenclaturesData[this.currentNomenclature] = response['id'];
        return response['id'];
    }

    public async title(prompt: string = '') {
        this.currentNomenclature = 'title';
        this.currentNomenclatureUrlGet = `nomenclature/titles/filter?statuses=ACTIVE&page=0&size=1${prompt ? `&prompt=${prompt}` : ''}`
        this.currentNomenclatureUrlPost = 'titles';
        this.currentPayload = {
            "name": prompt ? prompt : `title ${this.generatedName}`,
            "status": "ACTIVE",
            "defaultSelection": false
        }

        const response = await this.checkNomenclature()
        this.nomenclaturesData[this.currentNomenclature] = response['id'];
        return response['id'];
    }

    public async method_of_representation(prompt: string = '') {
        this.currentNomenclature = 'method_of_representation';
        this.currentNomenclatureUrlGet = `nomenclature/representation-methods/filter?statuses=ACTIVE&page=0&size=1${prompt ? `&prompt=${prompt}` : ''}`
        this.currentNomenclatureUrlPost = 'representation-methods';
        this.currentPayload = {
            "name": prompt ? prompt : `method of representation ${this.generatedName}`,
            "status": "ACTIVE",
            "defaultSelection": false
        }

        const response = await this.checkNomenclature()
        this.nomenclaturesData[this.currentNomenclature] = response['id'];
        return response['id'];
    }

    public async missing_customer(prompt: string = '') {
        this.currentNomenclature = 'missing_customer';
        this.currentNomenclatureUrlGet = `missing-customer?statuses=ACTIVE&statuses=INACTIVE&page=0&size=1${prompt ? `&prompt=${prompt}` : ''}`
        this.currentNomenclatureUrlPost = 'missing-customer';
        this.currentPayload = {
            "uic": `${randomGens.generateRandomString(true, false, 10)}`,
            "name": prompt ? prompt : `MISSING CUSTOMER ${randomGens.generateRandomString(false, true, 5)}`,
            "nameTransliterated": prompt ? prompt : `MISSING CUSTOMER ${randomGens.generateRandomString(false, true, 5)}`,
            "legalForm": "LK",
            "legalFormTransliterated": "LK",
            "defaultSelection": false,
            "status": "ACTIVE"
        }

        const response = await this.checkNomenclature()
        this.nomenclaturesData[this.currentNomenclature] = response.id;
        return response.id;
    }

    public async price_component_price_type(prompt: string = '') {
        this.currentNomenclature = 'price_component_price_type';
        this.currentNomenclatureUrlGet = `price-component-price-type?statuses=ACTIVE&page=0&size=1${prompt ? `&prompt=${prompt}` : ''}`
        this.currentNomenclatureUrlPost = 'price-component-price-type';
        this.currentPayload = {
            "name": prompt ? prompt : `price component price type ${this.generatedName}`,
            "status": "ACTIVE",
            "defaultSelection": false
        }

        const response = await this.checkNomenclature()
        this.nomenclaturesData[this.currentNomenclature] = response['id'];
        return response['id'];
    }

    public async price_component_value_type(prompt: string = '') {
        this.currentNomenclature = 'price_component_value_type';
        this.currentNomenclatureUrlGet = `nomenclature/price-component-value-type/filter?statuses=ACTIVE&page=0&size=1${prompt ? `&prompt=${prompt}` : ''}`
        this.currentNomenclatureUrlPost = 'price-component-value-type';
        this.currentPayload = {
            "name": prompt ? prompt : `price component value type ${this.generatedName}`,
            "status": "ACTIVE",
            "defaultSelection": false
        }

        const response = await this.checkNomenclature()
        this.nomenclaturesData[this.currentNomenclature] = response['id'];
        return response['id'];
    }

    public async profiles(prompt: string = 'PLAYWRIGHT') {
        this.currentNomenclature = 'profiles';
        this.currentNomenclatureUrlGet = `profiles?statuses=ACTIVE&page=0&size=1${prompt ? `&prompt=${prompt}` : ''}`
        this.currentNomenclatureUrlPost = 'profiles';
        this.currentPayload = {
            "name": prompt ? prompt : `profile ${this.generatedName}`,
            "timeZone": "CET",
            "status": "ACTIVE",
            "defaultSelection": false
        }

        const response = await this.checkNomenclature()
        this.nomenclaturesData[this.currentNomenclature] = response['id'];
        return response['id'];
    }

    public async unwanted_customer_reasons(prompt: string = '') {
        this.currentNomenclature = 'unwanted_customer_reasons';
        this.currentNomenclatureUrlGet = `nomenclature/unwanted-customer-reason/filter?statuses=ACTIVE&page=0&size=1${prompt ? `&prompt=${prompt}` : ''}`
        this.currentNomenclatureUrlPost = 'unwanted-customer-reason';
        this.currentPayload = {
            "name": prompt ? prompt : `unwanted customer reason ${this.generatedName}`,
            "status": "ACTIVE",
            "defaultSelection": false
        }

        const response = await this.checkNomenclature()
        this.nomenclaturesData[this.currentNomenclature] = response['id'];
        return response['id'];
    }

    public async reason_for_cancellation(prompt: string = '') {
        this.currentNomenclature = 'reason_for_cancellation';
        this.currentNomenclatureUrlGet = `nomenclature/reason-for-cancellation/filter?statuses=ACTIVE&statuses=INACTIVE&page=0&size=1${prompt ? `&prompt=${prompt}` : ''}`
        this.currentNomenclatureUrlPost = 'reason-for-cancellation';
        this.currentPayload = {
            "name": prompt ? prompt : `reason for cancellation ${this.generatedName}`,
            "status": "ACTIVE",
            "defaultSelection": false
        }

        const response = await this.checkNomenclature()
        this.nomenclaturesData[this.currentNomenclature] = response['id'];
        return response['id'];
    }

    public async gcc_connection_type(prompt: string = '') {
        this.currentNomenclature = 'gcc_connection_type';
        this.currentNomenclatureUrlGet = `nomenclature/gcc-connection-type?statuses=ACTIVE&page=0&size=1${prompt ? `&prompt=${prompt}` : ''}`
        this.currentNomenclatureUrlPost = 'gcc-connection-type';
        this.currentPayload = {
            "name": prompt ? prompt : `gcc connection type ${this.generatedName}`,
            "status": "ACTIVE",
            "defaultSelection": false
        }

        const response = await this.checkNomenclature()
        this.nomenclaturesData[this.currentNomenclature] = response['id'];
        return response['id'];
    }
    
    public async scales_tariff(prompt: string = 'ЕСО ЕАД') {
        this.currentNomenclature = 'scales_tariff';
        this.currentNomenclatureUrlGet = `nomenclature/scales/filter?statuses=ACTIVE&page=0&size=1${prompt ? `&prompt=${prompt}` : ''}`
        this.currentNomenclatureUrlPost = 'scales';
        this.currentPayload = {
            "name": prompt ? prompt : `scale ${randomGens.generateRandomString(true, false, 6)}`,
            "scaleType": "222",
            "scaleCode": null,
            "tariffOrScale": prompt ? prompt : `scale ${this.generatedName}`,
            "status": "ACTIVE",
            "gridOperatorId": this.nomenclaturesData['grid_operator'],
            "defaultSelection": false,
            "calculationForNumberOfDays": false,
            "scaleForActiveElectricity": true
        }

        const response = await this.checkNomenclature()
        this.nomenclaturesData[this.currentNomenclature] = response['id'];
        return response['id'];
    }

    public async scales_code(prompt: string = 'codePlaywright') {
        this.currentNomenclature = 'scales_code';
        this.currentNomenclatureUrlGet = `nomenclature/scales/filter?statuses=ACTIVE&page=0&size=1${prompt ? `&prompt=${prompt}` : ''}`
        this.currentNomenclatureUrlPost = 'scales';
        this.currentPayload = {
            "name": prompt ? prompt : `scale ${this.generatedName}`,
            "scaleType": "codePlaywright",
            "scaleCode": prompt ? prompt : `scale ${this.generatedName}`,
            "tariffOrScale": null,
            "status": "ACTIVE",
            "gridOperatorId": this.nomenclaturesData['grid_operator'],
            "defaultSelection": false,
            "calculationForNumberOfDays": false,
            "scaleForActiveElectricity": true
        }

        const response = await this.checkNomenclature()
        this.nomenclaturesData[this.currentNomenclature] = response['id'];
        return response['id'];
    }

    public async scales_code_withoutCheckbox(prompt: string = '') {
        this.currentNomenclature = 'scales_code';
        this.currentNomenclatureUrlGet = `nomenclature/scales/filter?statuses=ACTIVE&page=0&size=1${prompt ? `&prompt=${prompt}` : ''}`
        this.currentNomenclatureUrlPost = 'scales';
        this.currentPayload = {
            "name": prompt ? prompt : `scale ${this.generatedName}`,
            "scaleType": "222",
            "scaleCode": prompt ? prompt : `scale ${this.generatedName}`,
            "tariffOrScale": null,
            "status": "ACTIVE",
            "gridOperatorId": this.nomenclaturesData['grid_operator'],
            "defaultSelection": false,
            "calculationForNumberOfDays": false,
            "scaleForActiveElectricity": false
        }

        const response = await this.checkNomenclature()
        this.nomenclaturesData[this.currentNomenclature] = response['id'];
        return response['id'];
    }

    public async scales_tariff_withoutCheckbox(prompt: string = 'ЕСО ЕАД') {
        this.currentNomenclature = 'scales_tariff';
        this.currentNomenclatureUrlGet = `nomenclature/scales/filter?statuses=ACTIVE&page=0&size=1${prompt ? `&prompt=${prompt}` : ''}`
        this.currentNomenclatureUrlPost = 'scales';
        this.currentPayload = {
            "name": prompt ? prompt : `scale ${randomGens.generateRandomString(true, false, 6)}`,
            "scaleType": "222",
            "scaleCode": null,
            "tariffOrScale": prompt ? prompt : `scale ${this.generatedName}`,
            "status": "ACTIVE",
            "gridOperatorId": this.nomenclaturesData['grid_operator'],
            "defaultSelection": false,
            "calculationForNumberOfDays": false,
            "scaleForActiveElectricity": false
        }

        const response = await this.checkNomenclature()
        this.nomenclaturesData[this.currentNomenclature] = response['id'];
        return response['id'];
    }


public async scales_tariff_numberOfDays(prompt: string = '') {
        this.currentNomenclature = 'scales_tariff';
        this.currentNomenclatureUrlGet = `nomenclature/scales/filter?statuses=ACTIVE&page=0&size=1${prompt ? `&prompt=${prompt}` : ''}`
        this.currentNomenclatureUrlPost = 'scales';
        this.currentPayload = {
            "name": prompt ? prompt : `scale ${randomGens.generateRandomString(true, false, 6)}`,
            "scaleType": "222",
            "scaleCode": null,
            "tariffOrScale": prompt ? prompt : `scale ${this.generatedName}`,
            "status": "ACTIVE",
            "gridOperatorId": this.nomenclaturesData['grid_operator'],
            "defaultSelection": false,
            "calculationForNumberOfDays": true,
            "scaleForActiveElectricity": false
        }

        const response = await this.checkNomenclature()
        this.nomenclaturesData[this.currentNomenclature] = response['id'];
        return response['id'];
    }

    public async scales_code_numberOfDays(prompt: string = '') {
        this.currentNomenclature = 'scales_code';
        this.currentNomenclatureUrlGet = `nomenclature/scales/filter?statuses=ACTIVE&page=0&size=1${prompt ? `&prompt=${prompt}` : ''}`
        this.currentNomenclatureUrlPost = 'scales';
        this.currentPayload = {
            "name": prompt ? prompt : `scale ${this.generatedName}`,
            "scaleType": "222",
            "scaleCode": prompt ? prompt : `scale ${this.generatedName}`,
            "tariffOrScale": null,
            "status": "ACTIVE",
            "gridOperatorId": this.nomenclaturesData['grid_operator'],
            "defaultSelection": false,
            "calculationForNumberOfDays": true,
            "scaleForActiveElectricity": false
        }

        const response = await this.checkNomenclature()
        this.nomenclaturesData[this.currentNomenclature] = response['id'];
        return response['id'];
    }
    public async product_group(prompt: string = '') {
        this.currentNomenclature = 'product_group';
        this.currentNomenclatureUrlGet = `nomenclature/product-groups/filter?statuses=ACTIVE&page=0&size=1${prompt ? `&prompt=${prompt}` : ''}`
        this.currentNomenclatureUrlPost = 'product-groups';
        this.currentPayload = {
            "name": prompt ? prompt : `product group ${this.generatedName}`,
            "nameTransliterated": prompt ? prompt : `product group ${this.generatedName}`,
            "status": "ACTIVE",
            "defaultSelection": false
        }

        const response = await this.checkNomenclature()
        this.nomenclaturesData[this.currentNomenclature] = response['id'];
        return response['id'];
    }

    public async product_type(prompt: string = '') {
        this.currentNomenclature = 'product_type';
        this.currentNomenclatureUrlGet = `nomenclature/product-types/filter?statuses=ACTIVE&page=0&size=1${prompt ? `&prompt=${prompt}` : ''}`
        this.currentNomenclatureUrlPost = 'product-types';
        this.currentPayload = {
            "name": prompt ? prompt : `product type ${this.generatedName}`,
            "status": "ACTIVE",
            "defaultSelection": false
        }

        const response = await this.checkNomenclature()
        this.nomenclaturesData[this.currentNomenclature] = response['id'];
        return response['id'];
    }

    public async price_type_for_electricity(prompt: string = '') {
        this.currentNomenclature = 'price_type_for_electricity';
        this.currentNomenclatureUrlGet = `nomenclature/electricity-price-type/filter?statuses=ACTIVE&page=0&size=1${prompt ? `&prompt=${prompt}` : ''}`
        this.currentNomenclatureUrlPost = 'electricity-price-type';
        this.currentPayload = {
            "name": prompt ? prompt : `electricity price type ${this.generatedName}`,
            "status": "ACTIVE",
            "defaultSelection": false
        }

        const response = await this.checkNomenclature()
        this.nomenclaturesData[this.currentNomenclature] = response['id'];
        return response['id'];
    }

    public async purpose_of_the_contact(prompt: string = '') {
        this.currentNomenclature = 'purpose_of_the_contract';
        this.currentNomenclatureUrlGet = `nomenclature/contact-purpose?statuses=ACTIVE&page=0&size=1${prompt ? `&prompt=${prompt}` : ''}`
        this.currentNomenclatureUrlPost = 'contact-purpose';
        this.currentPayload = {
            "name": prompt ? prompt : `purpose of the contract ${this.generatedName}`,
            "status": "ACTIVE",
            "defaultSelection": false
        }

        const response = await this.checkNomenclature()
        this.nomenclaturesData[this.currentNomenclature] = response['id'];
        return response['id'];
    }

    public async measurement_type (prompt: string = 'ЕСО ЕАД', gridOperator: string | null = null) {
        this.currentNomenclature = 'measurement_type';
        this.currentNomenclatureUrlGet = `nomenclature/measurement-type/filter?statuses=ACTIVE&statuses=INACTIVE&page=0&size=1&${prompt ? `prompt=${prompt}` : ''}`;
        this.currentNomenclatureUrlPost = 'measurement-type';
        this.currentPayload = {
            "name": prompt? prompt:`measurement type ${this.generatedName}`,
            "gridOperatorId": gridOperator ? gridOperator : this.nomenclaturesData['grid_operator'],
            "status": "ACTIVE",
            "defaultSelection": false
        }

        const response = await this.checkNomenclature()
        this.nomenclaturesData[this.currentNomenclature] = response['id'];
        return response['id'];
    }

    public async type_of_user(prompt: string = '') {
        this.currentNomenclature = 'type_of_user';
        this.currentNomenclatureUrlGet = `nomenclature/user-types/filter?statuses=ACTIVE&page=0&size=1${prompt ? `&prompt=${prompt}` : ''}`
        this.currentNomenclatureUrlPost = 'user-types';
        this.currentPayload = {
            "name": prompt ? prompt : `type of user ${this.generatedName}`,
            "status": "ACTIVE",
            "defaultSelection": false
        }

        const response = await this.checkNomenclature()
        this.nomenclaturesData[this.currentNomenclature] = response['id'];
        return response['id'];
    }

    public async service_group(prompt: string = '') {
        this.currentNomenclature = 'service_group';
        this.currentNomenclatureUrlGet = `nomenclature/service-groups/filter?statuses=ACTIVE&page=0&size=1${prompt ? `&prompt=${prompt}` : ''}`
        this.currentNomenclatureUrlPost = 'service-groups';
        this.currentPayload = {
            "name": prompt ? prompt : `service group ${this.generatedName}`,
            "nameTransliterated": prompt ? prompt : `service group ${this.generatedName}`,
            "status": "ACTIVE",
            "defaultSelection": false
        }

        const response = await this.checkNomenclature()
        this.nomenclaturesData[this.currentNomenclature] = response['id'];
        return response['id'];
    }

    public async service_type(prompt: string = '') {
        this.currentNomenclature = 'service_type';
        this.currentNomenclatureUrlGet = `nomenclature/service-type/filter?statuses=ACTIVE&page=0&size=1${prompt ? `&prompt=${prompt}` : ''}`
        this.currentNomenclatureUrlPost = 'service-type';
        this.currentPayload = {
            "name": prompt ? prompt : `service type ${this.generatedName}`,
            "status": "ACTIVE",
            "defaultSelection": false
        }

        const response = await this.checkNomenclature()
        this.nomenclaturesData[this.currentNomenclature] = response['id'];
        return response['id'];
    }

    public async service_unit(prompt: string = '') {
        this.currentNomenclature = 'service_unit';
        this.currentNomenclatureUrlGet = `nomenclature/service-units/filter?statuses=ACTIVE&page=0&size=1${prompt ? `&prompt=${prompt}` : ''}`
        this.currentNomenclatureUrlPost = 'service-units';
        this.currentPayload = {
            "name": prompt ? prompt : `service unit ${this.generatedName}`,
            "status": "ACTIVE",
            "defaultSelection": false
        }

        const response = await this.checkNomenclature()
        this.nomenclaturesData[this.currentNomenclature] = response['id'];
        return response['id'];
    }

    public async additional_conditions(prompt: string = '') {
        this.currentNomenclature = 'additional_conditions';
        this.currentNomenclatureUrlGet = `nomenclature/additional-conditions/filter?statuses=ACTIVE&statuses=INACTIVE&page=0&size=1${prompt ? `&prompt=${prompt}` : ''}`
        this.currentNomenclatureUrlPost = 'additional-conditions';
        this.currentPayload = {
            "customerAssessmentTypeId": this.nomenclaturesData['customer_assessment_criteria'],
            "name": prompt ? prompt : `additional conditions ${this.generatedName}`,
            "status": "ACTIVE",
            "defaultSelection": false
        }

        const response = await this.checkNomenclature()
        this.nomenclaturesData[this.currentNomenclature] = response['id'];
        return response['id'];
    }

    public async sms_sending_numbers(prompt: string = '') {
        this.currentNomenclature = 'sms_sending_numbers';
        this.currentNomenclatureUrlGet = `sms-sending-numbers?statuses=ACTIVE&page=0&size=1${prompt ? `&prompt=${prompt}` : ''}`
        this.currentNomenclatureUrlPost = 'sms-sending-numbers';
        this.currentPayload = {
            "name": prompt ? prompt : `sms sending number ${this.generatedName}`,
            "smsNumber": `${randomGens.generateRandomString(false,true,8)}`,
            "status": "ACTIVE",
            "defaultSelection": false
        }

        const response = await this.checkNomenclature()
        this.nomenclaturesData[this.currentNomenclature] = response['id'];
        return response['id'];
    }

    public async grounds_for_objection_withdrawal_to_change_of_a_CBG(prompt: string = '') {
        this.currentNomenclature = 'nomenclature/grounds_for_objection_withdrawal_to_change_of_a_CBG';
        this.currentNomenclatureUrlGet = `nomenclature/ground-for-objection-withdrawal-to-change-of-a-cbg/filter?statuses=ACTIVE&statuses=INACTIVE&page=0&size=1${prompt ? `&prompt=${prompt}` : ''}`
        this.currentNomenclatureUrlPost = 'ground-for-objection-withdrawal-to-change-of-a-cbg';
        this.currentPayload = {
            "name": prompt ? prompt : `grounds for objection withdrawal to change of a CBG ${this.generatedName}`,
            "status": "ACTIVE",
            "defaultSelection": false
        }

        const response = await this.checkNomenclature()
        this.nomenclaturesData[this.currentNomenclature] = response['id'];
        return response['id'];
    }

    public async balancing_group_coordinator_grounds(prompt: string = '') {
        this.currentNomenclature = 'balancing_group_coordinator_grounds';
        this.currentNomenclatureUrlGet = `nomenclature/balancing-group-coordinator-ground/filter?statuses=ACTIVE&statuses=INACTIVE&page=0&size=1${prompt ? `&prompt=${prompt}` : ''}`
        this.currentNomenclatureUrlPost = 'balancing-group-coordinator-ground';
        this.currentPayload = {
            "name": prompt ? prompt : `balancing group coordinator grounds ${this.generatedName}`,
            "status": "ACTIVE",
            "defaultSelection": false
        }

        const response = await this.checkNomenclature()
        this.nomenclaturesData[this.currentNomenclature] = response['id'];
        return response['id'];
    }

    public async blocking_reason(prompt: string = 'all reasons') {
        this.currentNomenclature = 'blocking_reason';
        this.currentNomenclatureUrlGet = `nomenclature/blocking-reason/filter?statuses=ACTIVE&statuses=INACTIVE&page=0&size=1${prompt ? `&prompt=${prompt}` : ''}`
        this.currentNomenclatureUrlPost = 'blocking-reason';
        this.currentPayload = {
            "name": prompt ? prompt : `blocking reason ${this.generatedName}`,
              "reasonTypes": [
                "BLOCKED_FOR_PAYMENT",
                "BLOCKED_FOR_REMINDER_LETTERS",
                "BLOCKED_FOR_CALC_LATE_PAYMENT_FINES_INTERESTS",
                "BLOCKED_FOR_LIABILITIES_OFFSETTING",
                "BLOCKED_FOR_SUPPLY_TERMINATION"
            ],
            "status": "ACTIVE",
            "defaultSelection": false
        }

        const response = await this.checkNomenclature()
        this.nomenclaturesData[this.currentNomenclature] = response['id'];
        return response['id'];
    }

    public async collection_partner(prompt: string = '') {
        this.currentNomenclature = 'collection_partner';
        this.currentNomenclatureUrlGet = `nomenclature/collection-partner/filter?statuses=ACTIVE&statuses=INACTIVE&page=0&size=1${prompt ? `&prompt=${prompt}` : ''}`
        this.currentNomenclatureUrlPost = 'collection-partner';
        this.currentPayload = {
            "name": prompt ? prompt : `collection partner ${this.generatedName}`,
            "status": "ACTIVE",
            "defaultSelection": false
        }

        const response = await this.checkNomenclature()
        this.nomenclaturesData[this.currentNomenclature] = response['id'];
        return response['id'];
    }

    public async customer_assessment_criteria_type(prompt: string = '') {
        this.currentNomenclature = 'customer_assessment_criteria_type';
        this.currentNomenclatureUrlGet = `customer-assessment-criteria?statuses=ACTIVE&page=0&size=1`

        const response = await this.checkNomenclature()
        this.nomenclaturesData[this.currentNomenclature] = response['id'];
        return response['id'];
    }

    public async topic_of_communication(prompt: string = '') {
        this.currentNomenclature = 'topic_of_communication';
        this.currentNomenclatureUrlGet = `topic-of-communication?statuses=ACTIVE&page=0&size=1${prompt ? `&prompt=${prompt}` : ''}`
        this.currentNomenclatureUrlPost = 'topic-of-communication';
        this.currentPayload = {
            "name": prompt ? prompt : `topic of communication ${this.generatedName}`,
            "status": "ACTIVE",
            "defaultSelection": false
        }

        const response = await this.checkNomenclature()
        this.nomenclaturesData[this.currentNomenclature] = response['id'];
        return response['id'];
    }

    public async type_of_connection_for_ci(prompt: string = '') {
        this.currentNomenclature = 'type_of_connection_for_ci';
        this.currentNomenclatureUrlGet = `nomenclature/ci-connection-type/filter?statuses=ACTIVE&statuses=INACTIVE&page=0&size=1${prompt ? `&prompt=${prompt}` : ''}`
        this.currentNomenclatureUrlPost = 'ci-connection-type';
        this.currentPayload = {
            "name": prompt ? prompt : `type of connection for CI ${this.generatedName}`,
            "status": "ACTIVE",
            "defaultSelection": false
        }

        const response = await this.checkNomenclature()
        this.nomenclaturesData[this.currentNomenclature] = response['id'];
        return response['id'];
    }

    public async type_of_connection_for_gcc(prompt: string = '') {
        this.currentNomenclature = 'type_of_connection_for_gcc';
        this.currentNomenclatureUrlGet = `nomenclature/gcc-connection-type/filter?statuses=ACTIVE&statuses=INACTIVE&page=0&size=1${prompt ? `&prompt=${prompt}` : ''}`
        this.currentNomenclatureUrlPost = 'gcc-connection-type';
        this.currentPayload = {
            "name": prompt ? prompt : `type of connection for GCC ${this.generatedName}`,
            "status": "ACTIVE",
            "defaultSelection": false
        }

        const response = await this.checkNomenclature()
        this.nomenclaturesData[this.currentNomenclature] = response['id'];
        return response['id'];
    }

    public async vat_rate(prompt: string = '') {
        this.currentNomenclature = 'vat_rate';
        this.currentNomenclatureUrlGet = `nomenclature/vat-rates/filter?statuses=ACTIVE&statuses=INACTIVE&page=0&size=1${prompt ? `&prompt=${prompt}` : ''}`
        this.currentNomenclatureUrlPost = 'vat-rates';
        this.currentPayload = {
            "name": prompt ? prompt : `vat rate ${randomGens.generateRandomString(true, false, 6)}`,
            "status": "ACTIVE",
            "valueInPercent": 20,
            "globalVatRate": false,
            "startDate": ''
        }

        const response = await this.checkNomenclature()
        this.nomenclaturesData[this.currentNomenclature] = response['id'];
        return response['id'];
    }

    public async accoutingPeriod(prompt: string = '') {
        this.currentNomenclature = 'accounting_period';
        this.currentNomenclatureUrlGet = `billing-run/accounting-period-available-list?page=0&size=1&direction=DESC${prompt ? `&prompt=${prompt}` : ''}`

        const response = await this.checkNomenclature()
        this.nomenclaturesData[this.currentNomenclature] = response['id'];
        return response['id'];
    }

    public async interestRate(prompt: string = '') {
        this.currentNomenclature = 'interest_rate';
        this.currentNomenclatureUrlGet = `interest-rate/list?page=0&size=1&direction=DESC${prompt ? `&prompt=${prompt}` : ''}`

        const response = await this.checkNomenclature()
        this.nomenclaturesData[this.currentNomenclature] = response['id'];
        return response['id'];
    }

    public async generateNomenclatures(templateData: any) {
        // Wave 1: all nomenclatures with no inter-dependencies — run in parallel
        await Promise.all([
            this.currency(),
            this.Account_manager_types(),
            this.action_type(),
            this.banks(),
            this.base_interest_rate(),
            this.belonging_capital_owners(),
            this.calendar(),
            this.campaign(),
            this.contract_version_types(),
            this.balancing_group_coordinator(),
            this.credit_rating(),
            this.deactivation_reason(),
            this.countries(),
            this.email_mailboxes(),
            this.economic_branch_according_to_national_classification(),
            this.Economic_branch_based_on_commercial_information(),
            this.external_intermediary(),
            this.form_of_ownership(),
            this.goods_group(),
            this.goods_supplier(),
            this.goods_unit(),
            this.grid_operator(),
            this.income_account_name(),
            this.legal_forms(),
            this.method_of_representation(),
            this.missing_customer(),
            this.customer_assessment_criteria_type(),
            this.additional_conditions(),
            this.balancing_group_coordinator_grounds(),
            this.blocking_reason(),
            this.collection_partner(),
            this.reason_for_cancellation(),
            this.reason_for_disconnection(),
            this.grounds_for_objection_withdrawal_to_change_of_a_CBG(),
            this.platform(),
            this.pod_additional_parameters(),
            this.preferences(),
            this.price_component_price_type(),
            this.price_component_value_type(),
            this.price_type_for_electricity(),
            this.product_group(),
            this.product_type(),
            this.profiles(),
            this.risk_assesment(),
            this.sales_area(),
            this.sales_channel(),
            this.segment(),
            this.service_group(),
            this.service_type(),
            this.service_unit(),
            this.sms_sending_numbers(),
            this.activity(),
            this.title(),
            this.topic_of_communication(),
            this.type_of_connection_for_ci(),
            this.type_of_connection_for_gcc(),
            this.type_of_user(),
            this.unwanted_customer_reasons(),
            this.vat_rate(),
            this.accoutingPeriod(),
            this.interestRate(),
            this.contact_purpose(),
            this.billing_purpose(),
        ]);

        // Wave 2: depends on wave 1 results (grid_operator, currency, calendar, countries, activity)
        await Promise.all([
            this.regions(),                 // needs countries
            this.measurement_type(),        // needs grid_operator
            this.scales_code(),             // needs grid_operator
            this.scales_tariff(),           // needs grid_operator
            this.taxes_for_grid_operator('ЕСО ЕАД', templateData.email_document_template, templateData.email_template), // needs grid_operator + currency
            this.task_type(),               // needs calendar
            this.sub_activity(),            // needs activity
        ]);

        // Wave 3: needs regions
        await this.municipalities();

        // Wave 4: needs municipalities
        await this.populated_places();

        // Wave 5: all need populated_places
        await Promise.all([
            this.districts(),
            this.zip_codes(),
            this.residential_areas(),
            this.streets(),
        ]);

        return this.nomenclaturesData;
    }

}