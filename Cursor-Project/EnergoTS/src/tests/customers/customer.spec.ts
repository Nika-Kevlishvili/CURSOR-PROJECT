import { test, expect } from "../../backend/fixtures/baseFixture";
import reportGenerator from '../../backend/utils/generateReport';


test.describe('[REG-1]: Customer', {tag: '@customer'}, () => {
    let numberUIC: string | undefined;

    test.beforeEach(async ({ GeneratePayload }) => {
        const payload = GeneratePayload.customers.customer_private_business();
        numberUIC = payload.customerIdentifier;
    });
    
    test.describe('[REG-2]: Customer', () => {
        test.describe('[REG-3]: Create - Customer', () => {
            test('[REG-157]: Create Customer | Private Customer + Business Activity | Happy path - Only Mandatory fields', async({Request, GeneratePayload, Endpoints, Responses}) => {
                const customerData = GeneratePayload.customers.customer_private_business()
                customerData.customerIdentifier = numberUIC!
                
                customerData.foreign = false

                customerData.address.foreignAddressData = null
                customerData.bankingDetails = null
                customerData.relatedCustomers = null
                customerData.communicationData = null;
                customerData.accountManagers = null
                customerData.customerAdditionalInformation = null
                customerData.managers = null
                customerData.owner = null
                customerData.economicBranchNCEAId = null
                customerData.businessCustomerDetails.legalFormId = null
                customerData.businessCustomerDetails.legalFormTransId = null

                customerData.address.localAddressData.districtId = null
                customerData.address.localAddressData.residentialAreaId = null
                customerData.address.localAddressData.streetId = null
                customerData.addressTransl.localAddressData.district = null
                customerData.addressTransl.localAddressData.residentialArea = null
                customerData.addressTransl.localAddressData.street = null

                customerData.bankingDetails = null

                const customer = await Request.post(Endpoints.customer, { data: customerData })
                await expect(customer).CheckResponse();
                Responses.customer.push(await customer.json());

                test.info().attach('[REG-157] response', {
                    body: JSON.stringify(reportGenerator.setLinksToResponses(Responses), null, 2),
                    contentType: 'application/json'
                });
                
            })
            test('[REG-158]: Create Customer | Private Customer + Business Activity | Full data - All fields/Sub objects with max data', async({Request, GeneratePayload, Endpoints}) => {

                const customerData = GeneratePayload.customers.customer_private_business()
                customerData.customerIdentifier = numberUIC!
                customerData.foreign = false

                customerData.marketingConsent = true
                customerData.preferCommunicationInEnglish = true
                customerData.oldCustomerNumber = '1111111111'
                customerData.vatNumber = 'BG' + numberUIC

                customerData.privateCustomerDetails.middleName = 'MIDDLENAME'
                customerData.privateCustomerDetails.middleNameTranslated = 'MIDDLENAMETRANS'

                customerData.address.foreignAddressData = null
                customerData.address.localAddressData.residentialAreaType = 'QUARTER'
                customerData.address.localAddressData.streetType = 'STREET'


                customerData.address.number = '1'
                customerData.address.additionalInformation = 'ADDINFO'
                customerData.address.block = '2'
                customerData.address.entrance = '3'
                customerData.address.floor = '4'
                customerData.address.apartment = '5'
                customerData.address.mailbox = '6'
                customerData.addressTransl.number = '1'
                customerData.addressTransl.additionalInformation = 'ADDINFO'
                customerData.addressTransl.block = '2'
                customerData.addressTransl.entrance = '3'
                customerData.addressTransl.floor = '4'
                customerData.addressTransl.apartment = '5'
                customerData.addressTransl.mailbox = '6'

                customerData.relatedCustomers = null

                customerData.accountManagers = null
                customerData.customerAdditionalInformation = null
                customerData.managers = null
                customerData.owner = null
                customerData.economicBranchNCEAId = null

                customerData.address.localAddressData.districtId = null
                customerData.address.localAddressData.residentialAreaId = null
                customerData.address.localAddressData.streetId = null
                customerData.addressTransl.localAddressData.district = null
                customerData.addressTransl.localAddressData.residentialArea = null
                customerData.addressTransl.localAddressData.street = null

                const customer = await Request.post(Endpoints.customer, { data: customerData });
                await expect(customer).CheckResponse();


                test.info().attach('[REG-158] response', {
                    body: JSON.stringify(reportGenerator.setLinksToResponses({}), null, 2),
                    contentType: 'application/json'
                });
            })
        })
    });

    test.describe('[REG-12]: Groups of connected customers', () => {
        test.describe('[REG-35]: Create - Groups of connected customers', () => {
            test('[REG-37]: CCG create',  async({Request, GeneratePayload, Responses, Endpoints, frontend}) => {
                test.setTimeout(120000);
                await test.step('Create 2 legal customers', async () => {
                    const customer1 = await Request.post(Endpoints.customer, {data: GeneratePayload.customers.customer_legal()})
                    const customer2 = await Request.post(Endpoints.customer, {data: GeneratePayload.customers.customer_legal()})

                    await expect(customer1).CheckResponse();
                    await expect(customer2).CheckResponse();

                    Responses.customer.push(await customer1.json())
                    Responses.customer.push(await customer2.json())
                })

                await test.step('Create group of connected customers with the 2 legal customers', async () => {
                    const gcc = await Request.post(Endpoints.groupsOfConnectedCustomers, {data: GeneratePayload.customers.groupsOfConnectedCustomers()})
                    await expect(gcc).CheckResponse();
                    Responses.groupsOfConnectedCustomers.push(await gcc.json());
                })

                // await test.step('UI: Open CCG in frontend', async () => {
                //     await frontend.openEntity(Responses, 'groupsOfConnectedCustomers', 0);
                // });

                // //open first customer:
                // await frontend.openEntity(Responses, 'customer', 0);

                // // Second customer:
                // await frontend.openEntity(Responses, 'customer', 1);

                // await frontend.openEntity(Responses, 'groupsOfConnectedCustomers', 0);

                test.info().attach('[REG-35] response', {
                    body: JSON.stringify(reportGenerator.setLinksToResponses(Responses), null, 2),
                    contentType: 'application/json'
                });
            })
        })
    })
});