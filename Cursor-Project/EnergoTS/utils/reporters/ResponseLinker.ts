const { configuredBaseURL } = require('../../fixtures/baseFixture');

export class ResponseLinker {
    private static readonly ENDPOINTS: Record<string, string> = {
        nomenclature: '/nomenclature',
        customer: '/customers',
        unwantedCustomer: '/unwanted-customers',
        groupsOfConnectedCustomers: '/groups-of-connected-customers',
        priceComponent: '/price-components',
        groupOfPriceComponents: '/groups-of-price-components',
        termsGroup: '/groups-of-terms',
        processes: '/processes',
        systemMessages: '/system-messages',
        qesDocuments: '/documents-for-signing-with-qualified-signature',
        product: '/energy-products',
        productContract: '/energy-product-contracts',
        service: '/services',
        priceParameters: '/price-parameters',
        terms: '/terms',
        goods: '/goods',
        termination: '/terminations',
        groupOfTerminations: '/groups-of-terminations',
        interim: '/interim-and-advance-payments',
        groupOfInterims: '/groups-of-interim-and-advance-payment',
        penalty: '/penalties',
        groupOfPenalties: '/groups-of-penalties',
        discount: '/discount-from-duty-to-society',
        pod: '/points-of-delivery',
        meters: '/meters',
        compensation: '/compensation',
        dataByProfiles: '/data-by-profiles',
        dataByScales: '/data-by-scales',
        supplyActivation: '/supply-activation',
        supplyDeactivation: '/supply-deactivation',
        templateConverter: '/template-converter',
        task: '/tasks',
        activity: '/activities',
        interestRate: '/interest-rates',
        goodsOrder: '/goods-order',
        serviceContract: '/service-contracts',
        claimedPenalty: '/claimed-penalty',
        serviceOrder: '/service-orders',
        expressContract: '/express-contract',
        action: '/actions-for-terminations-and-penalties',
        companyDetails: '/company-details',
        accountingPeriods: '/accounting-periods',
        processPeriodicity: '/process-periodicity',
        billingRun: '/billing-run',
        deposit: '/deposits',
        invoiceCancellation: '/invoice-cancellation',
        invoice: '/invoices',
        customerLiability: '/customer-liabilities',
        customerAssessment: '/customer-assessment',
        customerReceivable: '/customer-receivables',
        payment: '/payments',
        massOperationForBlocking: '/mass-operation-for-blocking',
        paymentPackage: '/payment-package',
        collectionChannel: '/collection-channel',
        reminder: '/reminder',
        email: '/email',
        rescheduling: '/rescheduling',
        changeOfCoordinatorObjection: '/change-of-coordinator-objection',
        manualLiabilityOffsetting: '/manual-liability-offsetting',
        latePaymentFine: '/late-payment-fine',
        defaultInterest: '/default-interest-calculation',
        disconnectionOfPowerSupply: '/disconnection-of-power-supply',
        cancellationOfRequestOfDisconnection: '/cancellation-of-request-for-disconnection',
        massSms: '/mass-sms',
        requestForDisconnection: '/request-for-disconnection',
        sms: '/sms',
        massEmailCommunication: '/mass-email-communication',
        massEmail: '/mass-email',
        onPaper: '/on-paper',
        objectionWithdrawal: '/objection-withdrawal',
        reconnectionOfPowerSupply: '/reconnection-of-power-supply',
        reminderForDisconnection: '/reminder-for-disconnection',
        template: '/templates',
    };

    private static getFrontendUrl(baseUrl: string): string | null {
        // Normalize baseUrl by removing trailing slash for comparison
        const normalizedBaseUrl = baseUrl.replace(/\/$/, '');

        if (normalizedBaseUrl === 'http://10.236.20.11:8091') {
            return 'http://10.236.20.11:8080/';
        } else if (normalizedBaseUrl === 'http://10.236.20.31:8091') {
            return 'http://10.236.20.31:8080/';
        } else if (normalizedBaseUrl === 'http://10.236.20.81:8091') {
            return 'http://10.236.20.81:8080/';
        } else if (normalizedBaseUrl === 'http://10.236.20.81:8094') {
            return 'http://10.236.20.31:8082/';
        } else if (normalizedBaseUrl === 'https://testapps.energo-pro.bg/backend/phoenix-epres') {
            return 'https://testapps.energo-pro.bg/app/phoenix-epres/';
        } else if (normalizedBaseUrl === 'https://devapps.energo-pro.bg/backend/phoenix2-dev') {
            return 'https://devapps.energo-pro.bg/app/phoenix2-dev/';
        }else if (normalizedBaseUrl === 'http://10.236.20.81:8095') {
            return 'https://testapps.energo-pro.bg/app/phoenix-test2/';
        }

        return null;
    }

    private static buildLink(frontEndUrl: string, key: string, item: any): string | null {
        let itemId: number | undefined;

        // Handle two cases: item is a number (just the ID), or item is an object with an id property
        if (typeof item === 'number') {
            itemId = item;
        } else if (item && typeof item === 'object' && item.id) {
            itemId = item.id;
        }

        if (itemId === undefined) {
            return null;
        }

        // Special case for customer endpoint
        if (key === 'customer') {
            return `${frontEndUrl}${this.ENDPOINTS[key]}/preview/basic?id=${itemId}`;
        }
        // Special case for dataByProfiles - needs periodFrom, periodTo, and periodType
        else if (key === 'dataByProfiles' && typeof item === 'object' && item.periodFrom && item.periodTo && item.periodType) {
            return `${frontEndUrl}${this.ENDPOINTS[key]}/preview?id=${itemId}&periodFrom=${item.periodFrom.split('T')[0]}&periodTo=${item.periodTo.split('T')[0]}&periodType=${item.periodType}`;
        }
        else {
            return `${frontEndUrl}${this.ENDPOINTS[key]}/preview?id=${itemId}`;
        }
    }

    public static setLinksToResponses(responses: any): Record<string, string[]> {
        const baseUrl = configuredBaseURL || process.env.BASE_URL || 'http://10.236.20.11:8091/';
        const frontEndUrl = this.getFrontendUrl(baseUrl);

        if (!frontEndUrl) {
            console.warn('Frontend URL not configured for current BASE_URL:', baseUrl);
            return {};
        }

        // Traverse through responses and map IDs to links
        const linkedResponses: Record<string, string[]> = {};

        for (const [key, value] of Object.entries(responses)) {
            if (Array.isArray(value) && value.length > 0 && this.ENDPOINTS[key]) {
                const links: string[] = [];
                for (const item of value) {
                    const link = this.buildLink(frontEndUrl, key, item);
                    if (link) {
                        links.push(link);
                    }
                }
                if (links.length > 0) {
                    linkedResponses[key] = links;
                }
            }
        }

        if (process.env.SAVE_OBJECT_LINKS === 'true') {
            console.log("linkedResponses:", linkedResponses);
        }

        return linkedResponses;
    }
}
