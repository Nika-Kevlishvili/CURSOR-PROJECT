import { APIRequestContext } from "playwright";
import { baseFixture } from "../../fixtures/baseFixture";
import { CustomerPayloads } from './domains/CustomerPayloads';
import { PointsOfDeliveryPayloads } from './domains/PointsOfDeliveryPayloads';
import { CustomerCommunicationPayloads } from './domains/CustomerCommunicationPayloads';
import { ContractsAndOrdersPayloads } from './domains/ContractsAndOrdersPayloads';
import { ProductAndServicesPayloads } from './domains/ProductAndServicesPayloads';
import { EnergyDataPayloads } from './domains/EnergyDataPayloads';
import { BillingPayloads } from './domains/BillingPayloads';
import { ReceivablesManagementPayloads } from './domains/ReceivablesManagementPayloads';
import { OperationsManagementPayloads } from './domains/OperationsManagementPayloads';
import { MasterDataPayloads } from './domains/MasterDataPayloads';
import {salesPortalPayloads} from './domains/salesPortalPayloads';

export class GeneratePayload {
    public readonly customers: CustomerPayloads;
    public readonly pointsOfDelivery: PointsOfDeliveryPayloads;
    public readonly customerCommunication: CustomerCommunicationPayloads;
    public readonly contractsAndOrders: ContractsAndOrdersPayloads;
    public readonly productAndServices: ProductAndServicesPayloads;
    public readonly energyData: EnergyDataPayloads;
    public readonly billing: BillingPayloads;
    public readonly receivablesManagement: ReceivablesManagementPayloads;
    public readonly operationsManagement: OperationsManagementPayloads;
    public readonly masterData: MasterDataPayloads;
    public readonly salesPortal: salesPortalPayloads;

    constructor(apiRequestContext: APIRequestContext, responses: baseFixture['Responses'], fileUploadRequest: APIRequestContext) {
        this.customers = new CustomerPayloads(apiRequestContext, responses);
        this.pointsOfDelivery = new PointsOfDeliveryPayloads(apiRequestContext, responses);
        this.customerCommunication = new CustomerCommunicationPayloads(apiRequestContext, responses);
        this.contractsAndOrders = new ContractsAndOrdersPayloads(apiRequestContext, responses);
        this.productAndServices = new ProductAndServicesPayloads(apiRequestContext, responses, fileUploadRequest);
        this.energyData = new EnergyDataPayloads(apiRequestContext, responses, fileUploadRequest);
        this.billing = new BillingPayloads(apiRequestContext, responses);
        this.receivablesManagement = new ReceivablesManagementPayloads(apiRequestContext, responses, fileUploadRequest);
        this.operationsManagement = new OperationsManagementPayloads(apiRequestContext, responses);
        this.masterData = new MasterDataPayloads(apiRequestContext, responses);
        this.salesPortal = new salesPortalPayloads(apiRequestContext, responses);
    }
}