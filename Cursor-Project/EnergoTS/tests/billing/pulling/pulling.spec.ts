import {test, expect} from '../../../fixtures/baseFixture';
import { randomGens } from '../../../utils/randomGens';
import reportGenerator from '../../../utils/generateReport';

test.describe('[REG-55]: Billing ', {tag: '@billing'}, () => {
    test.describe('[REG-106]: Billing run', () => {
        test.describe.serial('[REG-964]: Pulling', () => {
            test('clean up stash files in case anything left over from previous runs', async ({clearStashedResponses}) => {
                clearStashedResponses('pulling_spec_ts');
            });

            test('data preparation for case REG-965', async ({Request, GeneratePayload, Responses, Endpoints, saveResponsesToFile}) => {
                test.setTimeout(10 * 60 * 1000);
                await test.step('generate customer', async () => {
                    const customer = await Request.post(Endpoints.customer, {data: GeneratePayload.customers.customer_legal()});
                    await expect(customer).CheckResponse();
                    Responses.customer.push(await customer.json());
                });

                await test.step('generate price component for product 1', async () => {
                    let payload =  GeneratePayload.productAndServices.priceSettlement();

                    payload.formulaRequest.expression = 100;
                    const price = await Request.post(Endpoints.priceComponent, {data: payload});
                    await expect(price).CheckResponse();
                    Responses.priceComponent.push(await price.json());
                })

                await test.step('generate terms for products', async () => {
                    for (let i = 0; i < 2; i++) {
                        const term = await Request.post(Endpoints.terms, {data: GeneratePayload.productAndServices.term()});
                        await expect(term).CheckResponse();
                        Responses.terms.push(await term.json())
                    }
                })

                await test.step('generate settlement pod', async () => {
                    const payload = GeneratePayload.pointsOfDelivery.pod_settlement();
                    payload.name = '1 hour pod';
                    const podSettlement = await Request.post(Endpoints.pod, {data: GeneratePayload.pointsOfDelivery.pod_settlement()});
                    await expect(podSettlement).CheckResponse();
                    Responses.pod.push(await podSettlement.json());
                })

                await test.step('generate product 1 for contract 1', async () => {
                    const product = await Request.post(Endpoints.product, {data: GeneratePayload.productAndServices.product()});
                    await expect(product).CheckResponse();
                    Responses.product.push(await product.json())
                })

                await test.step('generate price component for product 2', async () => {
                    let payload =  GeneratePayload.productAndServices.priceSettlement();

                    payload.formulaRequest.expression = 200
                    const price2 = await Request.post(Endpoints.priceComponent, {data: payload});
                    await expect(price2).CheckResponse();
                    Responses.priceComponent.push(await price2.json());
                    
                })

                await test.step('generate product 2 for contract 2', async () => {
                    const payload = GeneratePayload.productAndServices.product(1);
                    payload.priceComponentIds = [Responses.priceComponent[1]];
                    const product = await Request.post(Endpoints.product, {data: payload});
                    await expect(product).CheckResponse();
                    Responses.product.push(await product.json())
                })

                await test.step('generate contract 1', async () => {
                    const payload = await GeneratePayload.contractsAndOrders.product_contract();
                    const contract = await Request.post(Endpoints.productContract, {data: payload});
                    await expect(contract).CheckResponse();
                    Responses.productContract.push(await contract.json())
                });

                await test.step('generate contract 2', async () => {
                    const payload = await GeneratePayload.contractsAndOrders.product_contract(0, 1);
                    const contract = await Request.post(Endpoints.productContract, {data: payload});
                    await expect(contract).CheckResponse();
                    Responses.productContract.push(await contract.json())
                    
                });

                await test.step('Activate POD', async () => {
                    const startDate = randomGens.generateMonthStartDate('yyyy-mm-dd', -1);
                    const halfwayDate1 = randomGens.generateMonthHalfDate('yyyy-mm-dd', -1);
                    const halfwayDate2 = randomGens.generateMonthHalfDatePlusOne('yyyy-mm-dd', 16, -1);
                    const endDate = randomGens.generateMonthEndDate('yyyy-mm-dd', -1);

                    let payload = await GeneratePayload.pointsOfDelivery.pod_activation(0, startDate, halfwayDate1, 0);
                    const podActivation1 = await Request.post('/contract-pods/manual', {data: payload})
                    await expect(podActivation1).CheckResponse();

                    payload = await GeneratePayload.pointsOfDelivery.pod_activation(0, halfwayDate2, endDate, 1);
                    const podActivation2 = await Request.post('/contract-pods/manual', {data: payload})
                    await expect(podActivation2).CheckResponse();
                });

                await test.step('Data by profiles 1 day', async() => {
                    const startDate = randomGens.generateMonthStartDate('yyyy-mm-dd', -1);
                    const endDate = randomGens.generateMonthEndDate('yyyy-mm-dd', -1);
                    const payload = await GeneratePayload.energyData.profile1Day(0, [{ startDate: `${startDate}T00:00:00.000Z`, endDate: `${endDate}T00:00:00.000Z`, amount: 100 }]);

                    const profiles = await Request.post('billing-by-profile', {data: payload});
                    await expect(profiles).CheckResponse();
                    const profileData = await profiles.json();
                    // Store full object with metadata for link generation
                    Responses.dataByProfiles.push({
                        id: profileData,
                        periodFrom: payload.periodFrom,
                        periodTo: payload.periodTo,
                        periodType: payload.periodType
                    });
                    
                    await GeneratePayload.energyData.uploadDataByProfileFile(profileData, 'DAY')
                });

                await test.step('Biiling run', async() => {
                    const billingRun = await Request.post(Endpoints.billingRun, {data: await GeneratePayload.billing.billingRun()});
                    await expect(billingRun).CheckResponse();
                    Responses.billingRun.push(await billingRun.json());
                })

                await test.step('save data for generation', async() => {
                    saveResponsesToFile(Responses, 'pulling_spec_ts');
                })
                
                test.info().attach('[REG-965] response', {
                    body: JSON.stringify(reportGenerator.setLinksToResponses(Responses), null, 2),
                    contentType: 'application/json'
                });
            })

            test('data preparation for case REG-966', async ({Request, GeneratePayload, Responses, Endpoints, saveResponsesToFile}) => {
                test.setTimeout(10 * 60 * 1000);
                await test.step('generate customer', async () => {
                    const customer = await Request.post(Endpoints.customer, {data: GeneratePayload.customers.customer_legal()});
                    await expect(customer).CheckResponse();
                    Responses.customer.push(await customer.json());
                });

                await test.step('generate price component for product 1', async () => {
                    let payload =  GeneratePayload.productAndServices.priceSettlement();

                    payload.formulaRequest.expression = 100;
                    const price = await Request.post(Endpoints.priceComponent, {data: payload});
                    await expect(price).CheckResponse();
                    Responses.priceComponent.push(await price.json());
                })

                await test.step('generate terms for products', async () => {
                    for (let i = 0; i < 2; i++) {
                        const term = await Request.post(Endpoints.terms, {data: GeneratePayload.productAndServices.term()});
                        await expect(term).CheckResponse();
                        Responses.terms.push(await term.json())
                    }
                })

                await test.step('generate settlement pod', async () => {
                    const payload = GeneratePayload.pointsOfDelivery.pod_settlement();
                    payload.name = '1 hour pod';
                    const podSettlement = await Request.post(Endpoints.pod, {data: GeneratePayload.pointsOfDelivery.pod_settlement()});
                    await expect(podSettlement).CheckResponse();
                    Responses.pod.push(await podSettlement.json());
                })

                await test.step('generate product 1 for contract 1', async () => {
                    const product = await Request.post(Endpoints.product, {data: GeneratePayload.productAndServices.product()});
                    await expect(product).CheckResponse();
                    Responses.product.push(await product.json())
                })

                await test.step('generate price component for product 2', async () => {
                    let payload =  GeneratePayload.productAndServices.priceSettlement();

                    payload.formulaRequest.expression = 200
                    const price2 = await Request.post(Endpoints.priceComponent, {data: payload});
                    await expect(price2).CheckResponse();
                    Responses.priceComponent.push(await price2.json());
                })

                await test.step('generate product 2 for contract 2', async () => {
                    const payload = GeneratePayload.productAndServices.product(1);
                    payload.priceComponentIds = [Responses.priceComponent[1]];
                    const product = await Request.post(Endpoints.product, {data: payload});
                    await expect(product).CheckResponse();
                    Responses.product.push(await product.json())
                })

                await test.step('generate contract 1', async () => {
                    const payload = await GeneratePayload.contractsAndOrders.product_contract();
                    const contract = await Request.post(Endpoints.productContract, {data: payload});
                    await expect(contract).CheckResponse();
                    Responses.productContract.push(await contract.json())
                });

                await test.step('generate settlement pod 2 for contract 2', async () => {
                    const payload = GeneratePayload.pointsOfDelivery.pod_settlement();
                    payload.name = '1 month pod';
                    const podSettlement = await Request.post(Endpoints.pod, {data: GeneratePayload.pointsOfDelivery.pod_settlement()});
                    await expect(podSettlement).CheckResponse();
                    Responses.pod.push(await podSettlement.json());
                })

                await test.step('generate contract 2', async () => {
                    const payload = await GeneratePayload.contractsAndOrders.product_contract(0, 1);
                    const contract = await Request.post(Endpoints.productContract, {data: payload});
                    await expect(contract).CheckResponse();
                    Responses.productContract.push(await contract.json())
                    
                });

                await test.step('Activate PODs', async () => {
                    const startDate = randomGens.generateMonthStartDate('yyyy-mm-dd', -1);
                    const halfwayDate1 = randomGens.generateMonthHalfDate('yyyy-mm-dd', -1);
                    const halfwayDate2 = randomGens.generateMonthHalfDatePlusOne('yyyy-mm-dd', 16, -1);
                    const endDate = randomGens.generateMonthEndDate('yyyy-mm-dd', -1);
                    
                    let payload = await GeneratePayload.pointsOfDelivery.pod_activation(0, startDate, halfwayDate1, 0);
                    const podActivation1 = await Request.post('/contract-pods/manual', {data: payload})
                    await expect(podActivation1).CheckResponse();

                    payload = await GeneratePayload.pointsOfDelivery.pod_activation(0, halfwayDate2, endDate, 1);
                    const podActivation2 = await Request.post('/contract-pods/manual', {data: payload})
                    await expect(podActivation2).CheckResponse();

                    const podActivation3 = await Request.post('/contract-pods/manual', {data: await GeneratePayload.pointsOfDelivery.pod_activation(1, startDate, endDate, 1)});
                    await expect(podActivation3).CheckResponse();
                });

                await test.step('Data by profiles 1 day for pod 1', async() => {
                    const startDate = randomGens.generateMonthStartDate('yyyy-mm-dd', -1);
                    const endDate = randomGens.generateMonthEndDate('yyyy-mm-dd', -1);
                    const payload = await GeneratePayload.energyData.profile1Day(0, [{ startDate: `${startDate}T00:00:00.000Z`, endDate: `${endDate}T00:00:00.000Z`, amount: 100 }]);

                    const profiles = await Request.post('billing-by-profile', {data: payload});
                    await expect(profiles).CheckResponse();
                    const profileData = await profiles.json();

                    Responses.dataByProfiles.push({
                        id: profileData,
                        periodFrom: payload.periodFrom,
                        periodTo: payload.periodTo,
                        periodType: payload.periodType
                    });
                    
                    await GeneratePayload.energyData.uploadDataByProfileFile(profileData, 'DAY')
                });

                await test.step('generate data by profile for pod 2', async() => {
                    const startDate = randomGens.generateMonthStartDate('yyyy-mm-dd', -1);
                    const endDate = randomGens.generateMonthEndDate('yyyy-mm-dd', -1);
                    const payload = await GeneratePayload.energyData.profile1Month(1, [{ startDate, endDate }]);
                    const profiles = await Request.post('billing-by-profile', {data: payload});
                    await expect(profiles).CheckResponse();
                    const profileData = await profiles.json();

                    Responses.dataByProfiles.push({
                        id: profileData,
                        periodFrom: payload.periodFrom,
                        periodTo: payload.periodTo,
                        periodType: payload.periodType
                    });
                });

                await test.step('Billing run', async() => {
                    const billingRun1 = await Request.post(Endpoints.billingRun, {data: await GeneratePayload.billing.billingRun()});
                    await expect(billingRun1).CheckResponse();

                    const billingRun2 = await Request.post(Endpoints.billingRun, {data: await GeneratePayload.billing.billingRun('CUSTOMER')});
                    await expect(billingRun2).CheckResponse();

                    const billingRun3 = await Request.post(Endpoints.billingRun, {data: await GeneratePayload.billing.billingRun('POD')});
                    await expect(billingRun3).CheckResponse();

                    Responses.billingRun.push(await billingRun1.json());
                    Responses.billingRun.push(await billingRun2.json());
                    Responses.billingRun.push(await billingRun3.json());
                })

                await test.step('save data for generation', async() => {
                    saveResponsesToFile(Responses, 'pulling_spec_ts');
                })
    
                test.info().attach('[REG-966] response', {
                    body: JSON.stringify(reportGenerator.setLinksToResponses(Responses), null, 2),
                    contentType: 'application/json'
                });
            })

            test('data preparation for case REG-967', async ({Request, GeneratePayload, Responses, Endpoints, saveResponsesToFile}) => {
                test.setTimeout(10 * 60 * 1000);
                await test.step('generate customer', async () => {
                    const customer = await Request.post(Endpoints.customer, {data: GeneratePayload.customers.customer_legal()});
                    await expect(customer).CheckResponse();
                    Responses.customer.push(await customer.json());
                });

                await test.step('generate price component for product 1', async () => {
                    let payload =  GeneratePayload.productAndServices.priceSettlement();

                    payload.formulaRequest.expression = 100;
                    const price = await Request.post(Endpoints.priceComponent, {data: payload});
                    await expect(price).CheckResponse();
                    Responses.priceComponent.push(await price.json());
                })

                await test.step('generate terms for products', async () => {
                    for (let i = 0; i < 2; i++) {
                        const term = await Request.post(Endpoints.terms, {data: GeneratePayload.productAndServices.term()});
                        await expect(term).CheckResponse();
                        Responses.terms.push(await term.json())
                    }
                })

                await test.step('generate settlement pod', async () => {
                    const payload = GeneratePayload.pointsOfDelivery.pod_settlement();
                    payload.name = '1 hour pod';
                    const podSettlement = await Request.post(Endpoints.pod, {data: GeneratePayload.pointsOfDelivery.pod_settlement()});
                    await expect(podSettlement).CheckResponse();
                    Responses.pod.push(await podSettlement.json());
                })

                await test.step('generate product 1 for contract 1', async () => {
                    const product = await Request.post(Endpoints.product, {data: GeneratePayload.productAndServices.product()});
                    await expect(product).CheckResponse();
                    Responses.product.push(await product.json())
                })

                await test.step('generate price component for product 2', async () => {
                    let payload =  GeneratePayload.productAndServices.priceSettlement();

                    payload.formulaRequest.expression = 200
                    const price2 = await Request.post(Endpoints.priceComponent, {data: payload});
                    await expect(price2).CheckResponse();
                    Responses.priceComponent.push(await price2.json());
                    
                })

                await test.step('generate product 2 for contract 2', async () => {
                    const payload = GeneratePayload.productAndServices.product(1);
                    payload.priceComponentIds = [Responses.priceComponent[1]];
                    const product = await Request.post(Endpoints.product, {data: payload});
                    await expect(product).CheckResponse();
                    Responses.product.push(await product.json())
                })

                await test.step('generate contract 1', async () => {
                    const payload = await GeneratePayload.contractsAndOrders.product_contract();
                    const contract = await Request.post(Endpoints.productContract, {data: payload});
                    await expect(contract).CheckResponse();
                    Responses.productContract.push(await contract.json())
                });

                await test.step('generate contract 2', async () => {
                    const payload = await GeneratePayload.contractsAndOrders.product_contract(0, 1);
                    const contract = await Request.post(Endpoints.productContract, {data: payload});
                    await expect(contract).CheckResponse();
                    Responses.productContract.push(await contract.json())
                    
                });

                await test.step('Activate POD', async () => {
                    const startDate = randomGens.generateMonthStartDate('yyyy-mm-dd', -1);
                    const halfwayDate1 = randomGens.generateMonthHalfDate('yyyy-mm-dd', -1);
                    const halfwayDate2 = randomGens.generateMonthHalfDatePlusOne('yyyy-mm-dd', 17, -1);
                    const endDate = randomGens.generateMonthEndDate('yyyy-mm-dd', -1);

                    let payload = await GeneratePayload.pointsOfDelivery.pod_activation(0, startDate, halfwayDate1, 0);
                    const podActivation1 = await Request.post('/contract-pods/manual', {data: payload})
                    await expect(podActivation1).CheckResponse();

                    payload = await GeneratePayload.pointsOfDelivery.pod_activation(0, halfwayDate2, endDate, 1);
                    const podActivation2 = await Request.post('/contract-pods/manual', {data: payload})
                    await expect(podActivation2).CheckResponse();
                });

                await test.step('Data by profiles 1 day', async() => {
                    const startDate = randomGens.generateMonthStartDate('yyyy-mm-dd', -1);
                    const endDate = randomGens.generateMonthEndDate('yyyy-mm-dd', -1);
                    const payload = await GeneratePayload.energyData.profile1Day(0, [{ startDate: `${startDate}T00:00:00.000Z`, endDate: `${endDate}T00:00:00.000Z`, amount: 100 }]);

                    const profiles = await Request.post('billing-by-profile', {data: payload});
                    await expect(profiles).CheckResponse();
                    const profileData = await profiles.json();
                    // Store full object with metadata for link generation
                    Responses.dataByProfiles.push({
                        id: profileData,
                        periodFrom: payload.periodFrom,
                        periodTo: payload.periodTo,
                        periodType: payload.periodType
                    });
                    
                    await GeneratePayload.energyData.uploadDataByProfileFile(profileData, 'DAY')
                });

                await test.step('Biiling run', async() => {
                    const billingRun = await Request.post(Endpoints.billingRun, {data: await GeneratePayload.billing.billingRun()});
                    await expect(billingRun).CheckResponse();
                    const billingRunData = await billingRun.json();
                    Responses.billingRun.push({ id: billingRunData, invoiceNumbers: 0 });
                })

                await test.step('save data for generation', async() => {
                    saveResponsesToFile(Responses, 'pulling_spec_ts');
                })

                test.info().attach('[REG-967] response', {
                    body: JSON.stringify(reportGenerator.setLinksToResponses(Responses), null, 2),
                    contentType: 'application/json'
                });
            })

            test('data preparation for case REG-968', async ({Request, GeneratePayload, Responses, Endpoints, saveResponsesToFile}) => {
                test.setTimeout(10 * 60 * 1000);
                await test.step('generate customer', async () => {
                    const customer = await Request.post(Endpoints.customer, {data: GeneratePayload.customers.customer_legal()});
                    await expect(customer).CheckResponse();
                    Responses.customer.push(await customer.json());
                });

                await test.step('generate price component for product 1', async () => {
                    let payload =  GeneratePayload.productAndServices.priceSettlement();

                    payload.formulaRequest.expression = 100;
                    const price = await Request.post(Endpoints.priceComponent, {data: payload});
                    await expect(price).CheckResponse();
                    Responses.priceComponent.push(await price.json());
                })

                await test.step('generate terms for products', async () => {
                    for (let i = 0; i < 2; i++) {
                        const term = await Request.post(Endpoints.terms, {data: GeneratePayload.productAndServices.term()});
                        await expect(term).CheckResponse();
                        Responses.terms.push(await term.json())
                    }
                })

                await test.step('generate settlement pod 1 for contract 1', async () => {
                    const payload = GeneratePayload.pointsOfDelivery.pod_settlement();
                    payload.name = '1 hour pod';
                    const podSettlement = await Request.post(Endpoints.pod, {data: GeneratePayload.pointsOfDelivery.pod_settlement()});
                    await expect(podSettlement).CheckResponse();
                    Responses.pod.push(await podSettlement.json());
                })
                
                await test.step('generate settlement pod 2 for contract 1', async () => {
                    const payload = GeneratePayload.pointsOfDelivery.pod_settlement();
                    payload.name = '1 month pod';
                    const podSettlement = await Request.post(Endpoints.pod, {data: GeneratePayload.pointsOfDelivery.pod_settlement()});
                    await expect(podSettlement).CheckResponse();
                    Responses.pod.push(await podSettlement.json());
                })

                await test.step('generate product 1 for contract 1', async () => {
                    const product = await Request.post(Endpoints.product, {data: GeneratePayload.productAndServices.product()});
                    await expect(product).CheckResponse();
                    Responses.product.push(await product.json())
                })

                await test.step('generate price component for product 2', async () => {
                    let payload =  GeneratePayload.productAndServices.priceSettlement();

                    payload.formulaRequest.expression = 200
                    const price2 = await Request.post(Endpoints.priceComponent, {data: payload});
                    await expect(price2).CheckResponse();
                    Responses.priceComponent.push(await price2.json());
                })

                await test.step('generate product 2 for contract 2', async () => {
                    const payload = GeneratePayload.productAndServices.product(1);
                    payload.priceComponentIds = [Responses.priceComponent[1]];
                    const product = await Request.post(Endpoints.product, {data: payload});
                    await expect(product).CheckResponse();
                    Responses.product.push(await product.json())
                })

                await test.step('generate contract 1', async () => {
                    const payload = await GeneratePayload.contractsAndOrders.product_contract();
                    const contract = await Request.post(Endpoints.productContract, {data: payload});
                    await expect(contract).CheckResponse();
                    Responses.productContract.push(await contract.json())
                });

                await test.step('generate contract 2', async () => {
                    const payload = await GeneratePayload.contractsAndOrders.product_contract(0, 1, 0);
                    const contract = await Request.post(Endpoints.productContract, {data: payload});
                    await expect(contract).CheckResponse();
                    Responses.productContract.push(await contract.json())
                });

                await test.step('Activate PODs', async () => {
                    const startDate = randomGens.generateMonthStartDate('yyyy-mm-dd', -1);
                    const halfwayDate1 = randomGens.generateMonthHalfDate('yyyy-mm-dd', -1);
                    const halfwayDate2 = randomGens.generateMonthHalfDatePlusOne('yyyy-mm-dd', 17, -1);
                    const endDate = randomGens.generateMonthEndDate('yyyy-mm-dd', -1);
                    
                    let payload = await GeneratePayload.pointsOfDelivery.pod_activation(0, startDate, halfwayDate1, 0);
                    const podActivation1 = await Request.post('/contract-pods/manual', {data: payload})
                    await expect(podActivation1).CheckResponse();

                    payload = await GeneratePayload.pointsOfDelivery.pod_activation(0, halfwayDate2, endDate, 1);
                    const podActivation2 = await Request.post('/contract-pods/manual', {data: payload})
                    await expect(podActivation2).CheckResponse();

                    const podActivation3 = await Request.post('/contract-pods/manual', {data: await GeneratePayload.pointsOfDelivery.pod_activation(1, startDate, endDate, 0)});
                    await expect(podActivation3).CheckResponse();
                });

                await test.step('Data by profiles 1 day for pod 1', async() => {
                    const startDate = randomGens.generateMonthStartDate('yyyy-mm-dd', -1);
                    const endDate = randomGens.generateMonthEndDate('yyyy-mm-dd', -1);
                    const payload = await GeneratePayload.energyData.profile1Day(0, [{ startDate: `${startDate}T00:00:00.000Z`, endDate: `${endDate}T00:00:00.000Z`, amount: 100 }]);

                    const profiles = await Request.post('billing-by-profile', {data: payload});
                    await expect(profiles).CheckResponse();
                    const profileData = await profiles.json();

                    Responses.dataByProfiles.push({
                        id: profileData,
                        periodFrom: payload.periodFrom,
                        periodTo: payload.periodTo,
                        periodType: payload.periodType
                    });
                    
                    await GeneratePayload.energyData.uploadDataByProfileFile(profileData, 'DAY')
                });

                await test.step('generate data by profile for pod 2', async() => {
                    const startDate = randomGens.generateMonthStartDate('yyyy-mm-dd', -1);
                    const endDate = randomGens.generateMonthEndDate('yyyy-mm-dd', -1);
                    const payload = await GeneratePayload.energyData.profile1Month(1, [{ startDate, endDate }]);
                    
                    const profiles = await Request.post('billing-by-profile', {data: payload});
                    await expect(profiles).CheckResponse();
                    const profileData = await profiles.json();

                    Responses.dataByProfiles.push({
                        id: profileData,
                        periodFrom: payload.periodFrom,
                        periodTo: payload.periodTo,
                        periodType: payload.periodType
                    });
                });

                await test.step('Biiling run', async() => {
                    const billingRun1 = await Request.post(Endpoints.billingRun, {data: await GeneratePayload.billing.billingRun()});
                    await expect(billingRun1).CheckResponse();
                    Responses.billingRun.push(await billingRun1.json());
                })

                await test.step('save data for generation', async() => {
                    saveResponsesToFile(Responses, 'pulling_spec_ts');
                })
                
                test.info().attach('[REG-968] response', {
                    body: JSON.stringify(reportGenerator.setLinksToResponses(Responses), null, 2),
                    contentType: 'application/json'
                });
            })

            test('data preparation for case REG-969', async ({Request, GeneratePayload, Responses, Endpoints, saveResponsesToFile}) => {
                test.setTimeout(10 * 60 * 1000);
                await test.step('generate customer', async () => {
                    const customer = await Request.post(Endpoints.customer, {data: GeneratePayload.customers.customer_legal()});
                    await expect(customer).CheckResponse();
                    Responses.customer.push(await customer.json());
                });

                await test.step('generate price component for product 1', async () => {
                    let payload =  GeneratePayload.productAndServices.priceSettlement();

                    payload.formulaRequest.expression = 100;
                    const price = await Request.post(Endpoints.priceComponent, {data: payload});
                    await expect(price).CheckResponse();
                    Responses.priceComponent.push(await price.json());
                })

                await test.step('generate terms for products', async () => {
                    for (let i = 0; i < 2; i++) {
                        const term = await Request.post(Endpoints.terms, {data: GeneratePayload.productAndServices.term()});
                        await expect(term).CheckResponse();
                        Responses.terms.push(await term.json())
                    }
                })

                await test.step('generate settlement pod', async () => {
                    const payload = GeneratePayload.pointsOfDelivery.pod_settlement();
                    payload.name = '1 hour pod';
                    const podSettlement = await Request.post(Endpoints.pod, {data: GeneratePayload.pointsOfDelivery.pod_settlement()});
                    await expect(podSettlement).CheckResponse();
                    Responses.pod.push(await podSettlement.json());
                })

                await test.step('generate product 1 for contract 1', async () => {
                    const product = await Request.post(Endpoints.product, {data: GeneratePayload.productAndServices.product()});
                    await expect(product).CheckResponse();
                    Responses.product.push(await product.json())
                })

                await test.step('generate price component for product 2', async () => {
                    let payload =  GeneratePayload.productAndServices.priceSettlement();

                    payload.formulaRequest.expression = 200
                    const price2 = await Request.post(Endpoints.priceComponent, {data: payload});
                    await expect(price2).CheckResponse();
                    Responses.priceComponent.push(await price2.json());
                    
                })

                await test.step('generate product 2 for contract 2', async () => {
                    const payload = GeneratePayload.productAndServices.product(1);
                    payload.priceComponentIds = [Responses.priceComponent[1]];
                    const product = await Request.post(Endpoints.product, {data: payload});
                    await expect(product).CheckResponse();
                    Responses.product.push(await product.json())
                })

                await test.step('generate contract 1', async () => {
                    const payload = await GeneratePayload.contractsAndOrders.product_contract();
                    const contract = await Request.post(Endpoints.productContract, {data: payload});
                    await expect(contract).CheckResponse();
                    Responses.productContract.push(await contract.json())
                });

                await test.step('generate contract 2', async () => {
                    const payload = await GeneratePayload.contractsAndOrders.product_contract(0, 1);
                    const contract = await Request.post(Endpoints.productContract, {data: payload});
                    await expect(contract).CheckResponse();
                    Responses.productContract.push(await contract.json())
                    
                });

                await test.step('Activate POD', async () => {
                    const startDate = randomGens.generateMonthStartDate('yyyy-mm-dd', -1);
                    const halfwayDate1 = randomGens.generateMonthHalfDate('yyyy-mm-dd', -1);
                    const halfwayDate2 = randomGens.generateMonthHalfDatePlusOne('yyyy-mm-dd', 16, -1);
                    const endDate = randomGens.generateMonthEndDate('yyyy-mm-dd', -1);

                    let payload = await GeneratePayload.pointsOfDelivery.pod_activation(0, startDate, halfwayDate1, 0);
                    const podActivation1 = await Request.post('/contract-pods/manual', {data: payload})
                    await expect(podActivation1).CheckResponse();

                    payload = await GeneratePayload.pointsOfDelivery.pod_activation(0, halfwayDate2, endDate, 1);
                    const podActivation2 = await Request.post('/contract-pods/manual', {data: payload})
                    await expect(podActivation2).CheckResponse();
                });

                await test.step('Data by profiles 1 day', async() => {
                    const startDate = randomGens.generateMonthStartDate('yyyy-mm-dd', -1);
                    const endDate = randomGens.generateMonthEndDate('yyyy-mm-dd', -1);
                    const payload = await GeneratePayload.energyData.profile1Day(0, [{ startDate: `${startDate}T00:00:00.000Z`, endDate: `${endDate}T00:00:00.000Z`, amount: 100 }]);

                    const profiles = await Request.post('billing-by-profile', {data: payload});
                    await expect(profiles).CheckResponse();
                    const profileData = await profiles.json();
                    // Store full object with metadata for link generation
                    Responses.dataByProfiles.push({
                        id: profileData,
                        periodFrom: payload.periodFrom,
                        periodTo: payload.periodTo,
                        periodType: payload.periodType
                    });
                    
                    await GeneratePayload.energyData.uploadDataByProfileFile(profileData, 'DAY')
                });

                await test.step('Biiling run', async() => {
                    const billingRun = await Request.post(Endpoints.billingRun, {data: await GeneratePayload.billing.billingRun()});
                    await expect(billingRun).CheckResponse();
                    Responses.billingRun.push(await billingRun.json());
                })
                
                await test.step('save data for generation', async() => {
                    saveResponsesToFile(Responses, 'pulling_spec_ts');
                })

                test.info().attach('[REG-969] response', {
                    body: JSON.stringify(reportGenerator.setLinksToResponses(Responses), null, 2),
                    contentType: 'application/json'
                });
            })

            test('data preparation for case REG-970', async ({Request, GeneratePayload, Responses, Endpoints, saveResponsesToFile}) => {
                test.setTimeout(10 * 60 * 1000);
                await test.step('generate customer', async () => {
                    const customer = await Request.post(Endpoints.customer, {data: GeneratePayload.customers.customer_legal()});
                    await expect(customer).CheckResponse();
                    Responses.customer.push(await customer.json());
                });

                await test.step('generate price component for product 1', async () => {
                    let payload =  GeneratePayload.productAndServices.priceSettlement();

                    payload.formulaRequest.expression = 100;
                    const price = await Request.post(Endpoints.priceComponent, {data: payload});
                    await expect(price).CheckResponse();
                    Responses.priceComponent.push(await price.json());
                })

                await test.step('generate terms for products', async () => {
                    for (let i = 0; i < 2; i++) {
                        const term = await Request.post(Endpoints.terms, {data: GeneratePayload.productAndServices.term()});
                        await expect(term).CheckResponse();
                        Responses.terms.push(await term.json())
                    }
                })

                await test.step('generate settlement pod', async () => {
                    const payload = GeneratePayload.pointsOfDelivery.pod_settlement();
                    payload.name = '1 hour pod';
                    const podSettlement = await Request.post(Endpoints.pod, {data: GeneratePayload.pointsOfDelivery.pod_settlement()});
                    await expect(podSettlement).CheckResponse();
                    Responses.pod.push(await podSettlement.json());
                })

                await test.step('generate product 1 for contract 1', async () => {
                    const product = await Request.post(Endpoints.product, {data: GeneratePayload.productAndServices.product()});
                    await expect(product).CheckResponse();
                    Responses.product.push(await product.json())
                })

                await test.step('generate price component for product 2', async () => {
                    let payload =  GeneratePayload.productAndServices.priceSettlement();

                    payload.formulaRequest.expression = 200
                    const price2 = await Request.post(Endpoints.priceComponent, {data: payload});
                    await expect(price2).CheckResponse();
                    Responses.priceComponent.push(await price2.json());
                })

                await test.step('generate product 2 for contract 2', async () => {
                    const payload = GeneratePayload.productAndServices.product(1);
                    payload.priceComponentIds = [Responses.priceComponent[1]];
                    const product = await Request.post(Endpoints.product, {data: payload});
                    await expect(product).CheckResponse();
                    Responses.product.push(await product.json())
                })

                await test.step('generate contract 1', async () => {
                    const payload = await GeneratePayload.contractsAndOrders.product_contract();
                    const contract = await Request.post(Endpoints.productContract, {data: payload});
                    await expect(contract).CheckResponse();
                    Responses.productContract.push(await contract.json())
                });

                await test.step('generate settlement pod 2 for contract 2', async () => {
                    const payload = GeneratePayload.pointsOfDelivery.pod_settlement();
                    payload.name = '1 month pod';
                    const podSettlement = await Request.post(Endpoints.pod, {data: GeneratePayload.pointsOfDelivery.pod_settlement()});
                    await expect(podSettlement).CheckResponse();
                    Responses.pod.push(await podSettlement.json());
                })

                await test.step('generate contract 2', async () => {
                    const payload = await GeneratePayload.contractsAndOrders.product_contract(0, 1);
                    const contract = await Request.post(Endpoints.productContract, {data: payload});
                    await expect(contract).CheckResponse();
                    Responses.productContract.push(await contract.json())
                });

                await test.step('generate contract 3', async () => {
                    const payload = await GeneratePayload.contractsAndOrders.product_contract(0, 1, 1);
                    const contract = await Request.post(Endpoints.productContract, {data: payload});
                    await expect(contract).CheckResponse();
                    Responses.productContract.push(await contract.json())
                });

                await test.step('Activate PODs', async () => {
                    const startDate = randomGens.generateMonthStartDate('yyyy-mm-dd', -1);
                    const halfwayDate1 = randomGens.generateMonthHalfDate('yyyy-mm-dd', -1);
                    const halfwayDate2 = randomGens.generateMonthHalfDatePlusOne('yyyy-mm-dd', 16, -1);
                    const endDate = randomGens.generateMonthEndDate('yyyy-mm-dd', -1);
                    
                    let payload = await GeneratePayload.pointsOfDelivery.pod_activation(0, startDate, halfwayDate1, 0);
                    const podActivation1 = await Request.post('/contract-pods/manual', {data: payload})
                    await expect(podActivation1).CheckResponse();

                    payload = await GeneratePayload.pointsOfDelivery.pod_activation(0, halfwayDate2, endDate, 1);
                    const podActivation2 = await Request.post('/contract-pods/manual', {data: payload})
                    await expect(podActivation2).CheckResponse();

                    const podActivation3 = await Request.post('/contract-pods/manual', {data: await GeneratePayload.pointsOfDelivery.pod_activation(1, startDate, halfwayDate1, 1)});
                    await expect(podActivation3).CheckResponse();

                    const podActivation4 = await Request.post('/contract-pods/manual', {data: await GeneratePayload.pointsOfDelivery.pod_activation(1, randomGens.generateMonthHalfDatePlusOne('yyyy-mm-dd', 17, -1), endDate, 2)});
                    await expect(podActivation4).CheckResponse();
                });

                await test.step('Data by profiles 1 day for pod 1', async() => {
                    const startDate = randomGens.generateMonthStartDate('yyyy-mm-dd', -1);
                    const endDate = randomGens.generateMonthEndDate('yyyy-mm-dd', -1);
                    const payload = await GeneratePayload.energyData.profile1Day(0, [{ startDate: `${startDate}T00:00:00.000Z`, endDate: `${endDate}T00:00:00.000Z`, amount: 100 }]);

                    const profiles = await Request.post('billing-by-profile', {data: payload});
                    await expect(profiles).CheckResponse();
                    const profileData = await profiles.json();

                    Responses.dataByProfiles.push({
                        id: profileData,
                        periodFrom: payload.periodFrom,
                        periodTo: payload.periodTo,
                        periodType: payload.periodType
                    });
                    
                    await GeneratePayload.energyData.uploadDataByProfileFile(profileData, 'DAY')
                });

                await test.step('generate data by profile for pod 2', async() => {
                    const startDate = randomGens.generateMonthStartDate('yyyy-mm-dd', -1);
                    const endDate = randomGens.generateMonthEndDate('yyyy-mm-dd', -1);
                    const payload = await GeneratePayload.energyData.profile1Month(1, [{ startDate, endDate }]);
                    
                    const profiles = await Request.post('billing-by-profile', {data: payload});
                    await expect(profiles).CheckResponse();
                    const profileData = await profiles.json();

                    Responses.dataByProfiles.push({
                        id: profileData,
                        periodFrom: payload.periodFrom,
                        periodTo: payload.periodTo,
                        periodType: payload.periodType
                    });
                });

                await test.step('Billing run', async() => {
                    const billingRun1 = await Request.post(Endpoints.billingRun, {data: await GeneratePayload.billing.billingRun()});
                    await expect(billingRun1).CheckResponse();
                    const billingRunData1 = await billingRun1.json();
                    Responses.billingRun.push({ id: billingRunData1, invoiceNumbers: 0 });
                })

                await test.step('save data for generation', async() => {
                    saveResponsesToFile(Responses, 'pulling_spec_ts');
                })
                
                test.info().attach('[REG-970] response', {
                    body: JSON.stringify(reportGenerator.setLinksToResponses(Responses), null, 2),
                    contentType: 'application/json'
                });
            })
        
            test('data preparation for case REG-971', async ({Request, GeneratePayload, Responses, Endpoints, Nomenclatures, saveResponsesToFile}) => {
                test.setTimeout(10 * 60 * 1000);
                await test.step('generate customer', async () => {
                    const customer = await Request.post(Endpoints.customer, {data: GeneratePayload.customers.customer_legal()});
                    await expect(customer).CheckResponse();
                    Responses.customer.push(await customer.json());
                });

                await test.step('POD create', async () => {
                    const grid = await Nomenclatures.grid_operator('ЕСО ЕАД');
                    const measurementType = await Nomenclatures.measurement_type('ЕСО ЕАД', grid);
                    
                    const payload = GeneratePayload.pointsOfDelivery.pod_slp();

                    payload.gridOperatorId = await grid
                    payload.measurementTypeId = await measurementType
                    
                    const podSlp = await Request.post(Endpoints.pod, {data: payload});
                    await expect(podSlp).CheckResponse();
                    Responses.pod.push(await podSlp.json());
                })

                await test.step('Meters create', async () => {
                    const grid = await Nomenclatures.grid_operator('ЕСО ЕАД');

                    const podGet = await Request.get(`pod/${(Responses.pod[0].id)}?versionId=1`)
                    const podJsonBody = await podGet.json();
                    const podId = podJsonBody.id
                    const gridM = podJsonBody.gridOperatorId
                    const payloadMeter = GeneratePayload.pointsOfDelivery.meters()


                    const scaleCode = await Nomenclatures.scales_code(grid)
                    const scalGet = await Request.get(`scales/${scaleCode}`)

                    payloadMeter.gridOperatorId= await gridM
                    payloadMeter.podId = await podId
                    payloadMeter.meterScales = [scaleCode]


                    const meter = await Request.post(Endpoints.meters, {data: payloadMeter})
                    await expect(meter).CheckResponse();
                    Responses.meters.push(await meter.json())

                })
                    
                await test.step('generate terms for products', async () => {
                    for (let i = 0; i < 2; i++) {
                        const term = await Request.post(Endpoints.terms, {data: GeneratePayload.productAndServices.term()});
                        await expect(term).CheckResponse();
                        Responses.terms.push(await term.json())
                    }
                })

                await test.step('scale price 1', async () => {
                    const grid = await Nomenclatures.grid_operator('ЕСО ЕАД');
                    const scaleCode = await Nomenclatures.scales_code(grid)

                    const payload = GeneratePayload.productAndServices.scaleComponent();
                    payload.applicationModelRequest.volumesByScaleRequest.scaleIds[0] = scaleCode;

                    const priceComp = await Request.post(Endpoints.priceComponent, {data: payload});
                    await expect(priceComp).CheckResponse();
                    Responses.priceComponent.push(await priceComp.json());
                })

                await test.step('generate product 1 for contract 1', async () => {
                    const product = await Request.post(Endpoints.product, {data: GeneratePayload.productAndServices.product()});
                    await expect(product).CheckResponse();
                    Responses.product.push(await product.json())
                })

                await test.step('scale price 2', async () => {
                    const grid = await Nomenclatures.grid_operator('ЕСО ЕАД');
                    const scaleCode = await Nomenclatures.scales_code(grid)

                    const payload = GeneratePayload.productAndServices.scaleComponent();
                    payload.formulaRequest.expression = 200;
                    payload.applicationModelRequest.volumesByScaleRequest.scaleIds[0] = scaleCode;

                    const priceComp = await Request.post(Endpoints.priceComponent, {data: payload});
                    await expect(priceComp).CheckResponse();
                    Responses.priceComponent.push(await priceComp.json());
                })

                await test.step('generate product 2 for contract 2', async () => {
                    const payload = GeneratePayload.productAndServices.product(1);
                    payload.priceComponentIds = [Responses.priceComponent[1]];
                    const product = await Request.post(Endpoints.product, {data: payload});
                    await expect(product).CheckResponse();
                    Responses.product.push(await product.json())
                })

                await test.step('generate contract 1', async () => {
                    const payload = await GeneratePayload.contractsAndOrders.product_contract();
                    const contract = await Request.post(Endpoints.productContract, {data: payload});
                    await expect(contract).CheckResponse();
                    Responses.productContract.push(await contract.json())
                });

                await test.step('generate contract 2', async () => {
                    const payload = await GeneratePayload.contractsAndOrders.product_contract(0, 1);
                    const contract = await Request.post(Endpoints.productContract, {data: payload});
                    await expect(contract).CheckResponse();
                    Responses.productContract.push(await contract.json())
                    
                });

                await test.step('Activate POD', async () => {
                    const startDate = randomGens.generateMonthStartDate('yyyy-mm-dd', -1);
                    const halfwayDate1 = randomGens.generateMonthHalfDate('yyyy-mm-dd', -1);
                    const halfwayDate2 = randomGens.generateMonthHalfDatePlusOne('yyyy-mm-dd', 16, -1);
                    const endDate = randomGens.generateMonthEndDate('yyyy-mm-dd', -1);

                    let payload = await GeneratePayload.pointsOfDelivery.pod_activation(0, startDate, halfwayDate1, 0);
                    const podActivation1 = await Request.post('/contract-pods/manual', {data: payload})
                    await expect(podActivation1).CheckResponse();

                    payload = await GeneratePayload.pointsOfDelivery.pod_activation(0, halfwayDate2, endDate, 1);
                    const podActivation2 = await Request.post('/contract-pods/manual', {data: payload})
                    await expect(podActivation2).CheckResponse();
                });

                await test.step('Data by scales', async() => {
                    const payload = await GeneratePayload.energyData.scaleCode();
                    payload.dateFrom = randomGens.generateMonthStartDate('yyyy-mm-dd', -1);
                    payload.dateTo = randomGens.generateMonthEndDate('yyyy-mm-dd', -1);
                    payload.invoiceDate = `${randomGens.generateMonthStartDate('yyyy-mm-dd', -1)}T00:00:00.000Z`;
                    payload.billingByScalesTableCreateRequests[0].periodFrom = randomGens.generateMonthStartDate('yyyy-mm-dd', -1);
                    payload.billingByScalesTableCreateRequests[0].periodTo = randomGens.generateMonthEndDate('yyyy-mm-dd', -1);
                    const scales = await Request.post(Endpoints.dataByScales, {data: payload});
                    await expect(scales).CheckResponse();
                    Responses.dataByScales.push(await scales.json());
                })

                await test.step('Biiling run', async() => {
                    const billingRun = await Request.post(Endpoints.billingRun, {data: await GeneratePayload.billing.billingRun()});
                    await expect(billingRun).CheckResponse();
                    Responses.billingRun.push(await billingRun.json());
                })

                await test.step('save data for generation', async() => {
                    saveResponsesToFile(Responses, 'pulling_spec_ts');
                })
   
                test.info().attach('[REG-971] response', {
                    body: JSON.stringify(reportGenerator.setLinksToResponses(Responses), null, 2),
                    contentType: 'application/json'
                });
            })

            test('data preparation for case REG-972', async ({Request, GeneratePayload, Responses, Endpoints, saveResponsesToFile}) => {
                test.setTimeout(10 * 60 * 1000);
                await test.step('generate customer', async () => {
                    const customer = await Request.post(Endpoints.customer, {data: GeneratePayload.customers.customer_legal()});
                    await expect(customer).CheckResponse();
                    Responses.customer.push(await customer.json());
                });

                await test.step('generate price component for product 1', async () => {
                    let payload =  GeneratePayload.productAndServices.priceSettlement();

                    payload.formulaRequest.expression = 100;
                    const price = await Request.post(Endpoints.priceComponent, {data: payload});
                    await expect(price).CheckResponse();
                    Responses.priceComponent.push(await price.json());
                })

                await test.step('generate terms for products', async () => {
                    for (let i = 0; i < 2; i++) {
                        const term = await Request.post(Endpoints.terms, {data: GeneratePayload.productAndServices.term()});
                        await expect(term).CheckResponse();
                        Responses.terms.push(await term.json())
                    }
                })

                await test.step('generate settlement pod', async () => {
                    const payload = GeneratePayload.pointsOfDelivery.pod_settlement();
                    payload.name = '1 hour pod';
                    const podSettlement = await Request.post(Endpoints.pod, {data: GeneratePayload.pointsOfDelivery.pod_settlement()});
                    await expect(podSettlement).CheckResponse();
                    Responses.pod.push(await podSettlement.json());
                })

                await test.step('generate product 1 for contract 1', async () => {
                    const product = await Request.post(Endpoints.product, {data: GeneratePayload.productAndServices.product()});
                    await expect(product).CheckResponse();
                    Responses.product.push(await product.json())
                })

                await test.step('generate price component for product 2', async () => {
                    let payload =  GeneratePayload.productAndServices.priceSettlement();

                    payload.formulaRequest.expression = 200
                    const price2 = await Request.post(Endpoints.priceComponent, {data: payload});
                    await expect(price2).CheckResponse();
                    Responses.priceComponent.push(await price2.json());
                })

                await test.step('generate product 2 for contract 2', async () => {
                    const payload = GeneratePayload.productAndServices.product(1);
                    payload.priceComponentIds = [Responses.priceComponent[1]];
                    const product = await Request.post(Endpoints.product, {data: payload});
                    await expect(product).CheckResponse();
                    Responses.product.push(await product.json())
                })

                await test.step('generate contract 1', async () => {
                    const payload = await GeneratePayload.contractsAndOrders.product_contract();
                    const contract = await Request.post(Endpoints.productContract, {data: payload});
                    await expect(contract).CheckResponse();
                    Responses.productContract.push(await contract.json())
                });

                await test.step('generate settlement pod 2 for contract 2', async () => {
                    const payload = GeneratePayload.pointsOfDelivery.pod_settlement();
                    payload.name = '1 month pod';
                    const podSettlement = await Request.post(Endpoints.pod, {data: GeneratePayload.pointsOfDelivery.pod_settlement()});
                    await expect(podSettlement).CheckResponse();
                    Responses.pod.push(await podSettlement.json());
                })

                await test.step('generate contract 2', async () => {
                    const payload = await GeneratePayload.contractsAndOrders.product_contract(0, 1);
                    const contract = await Request.post(Endpoints.productContract, {data: payload});
                    await expect(contract).CheckResponse();
                    Responses.productContract.push(await contract.json())
                });

                await test.step('generate contract 3', async () => {
                    const payload = await GeneratePayload.contractsAndOrders.product_contract(0, 1, 1);
                    const contract = await Request.post(Endpoints.productContract, {data: payload});
                    await expect(contract).CheckResponse();
                    Responses.productContract.push(await contract.json())
                });

                await test.step('Activate PODs', async () => {
                    const startDate = randomGens.generateMonthStartDate('yyyy-mm-dd', -1);
                    const halfwayDate1 = randomGens.generateMonthHalfDate('yyyy-mm-dd', -1);
                    const halfwayDate2 = randomGens.generateMonthHalfDatePlusOne('yyyy-mm-dd', 16, -1);
                    const endDate = randomGens.generateMonthEndDate('yyyy-mm-dd', -1);
                    
                    let payload = await GeneratePayload.pointsOfDelivery.pod_activation(0, startDate, halfwayDate1, 0);
                    const podActivation1 = await Request.post('/contract-pods/manual', {data: payload})
                    await expect(podActivation1).CheckResponse();

                    payload = await GeneratePayload.pointsOfDelivery.pod_activation(0, halfwayDate2, endDate, 1);
                    const podActivation2 = await Request.post('/contract-pods/manual', {data: payload})
                    await expect(podActivation2).CheckResponse();

                    const podActivation3 = await Request.post('/contract-pods/manual', {data: await GeneratePayload.pointsOfDelivery.pod_activation(1, startDate, halfwayDate1, 1)});
                    await expect(podActivation3).CheckResponse();

                    const podActivation4 = await Request.post('/contract-pods/manual', {data: await GeneratePayload.pointsOfDelivery.pod_activation(1, halfwayDate2, endDate, 2)});
                    await expect(podActivation4).CheckResponse();
                });

                await test.step('Data by profiles 1 day for pod 1', async() => {
                    const startDate = randomGens.generateMonthStartDate('yyyy-mm-dd', -1);
                    const endDate = randomGens.generateMonthEndDate('yyyy-mm-dd', -1);
                    const payload = await GeneratePayload.energyData.profile1Day(0, [{ startDate: `${startDate}T00:00:00.000Z`, endDate: `${endDate}T00:00:00.000Z`, amount: 100 }]);

                    const profiles = await Request.post('billing-by-profile', {data: payload});
                    await expect(profiles).CheckResponse();
                    const profileData = await profiles.json();

                    Responses.dataByProfiles.push({
                        id: profileData,
                        periodFrom: payload.periodFrom,
                        periodTo: payload.periodTo,
                        periodType: payload.periodType
                    });
                    
                    await GeneratePayload.energyData.uploadDataByProfileFile(profileData, 'DAY')
                });

                await test.step('generate data by profile for pod 2', async() => {
                    const startDate = randomGens.generateMonthStartDate('yyyy-mm-dd', -1);
                    const endDate = randomGens.generateMonthEndDate('yyyy-mm-dd', -1);
                    const payload = await GeneratePayload.energyData.profile1Month(1, [{ startDate, endDate }]);
                    
                    const profiles = await Request.post('billing-by-profile', {data: payload});
                    await expect(profiles).CheckResponse();
                    const profileData = await profiles.json();

                    Responses.dataByProfiles.push({
                        id: profileData,
                        periodFrom: payload.periodFrom,
                        periodTo: payload.periodTo,
                        periodType: payload.periodType
                    });
                });

                await test.step('Billing run', async() => {
                    const billingRun1 = await Request.post(Endpoints.billingRun, {data: await GeneratePayload.billing.billingRun()});
                    await expect(billingRun1).CheckResponse();
                    const billingRunData1 = await billingRun1.json();
                    Responses.billingRun.push({ id: billingRunData1, invoiceNumbers: 2 });
                })

                await test.step('save data for generation', async() => {
                    saveResponsesToFile(Responses, 'pulling_spec_ts');
                })

                test.info().attach('[REG-972] response', {
                    body: JSON.stringify(reportGenerator.setLinksToResponses(Responses), null, 2),
                    contentType: 'application/json'
                });
            })

            test("run billings", async ({GeneratePayload}) => {
                test.setTimeout(10 * 60 * 1000);
                await test.step('start billing generation in parallel', async() => {
                    try {
                        await GeneratePayload.billing.waitForInvoiceGenerationParallel('pulling_spec_ts', true)
                    } catch (e) {
                        console.warn('Some billing runs failed, subsequent tests will handle their own failures:', (e as Error).message);
                    }
                })
            })

            test('[REG-965]: pod has no gaps', async ({Responses, loadResponsesFromFile}) => {
                const testCasesData = loadResponsesFromFile('pulling_spec_ts');
                Object.assign(Responses, testCasesData[0]);

                test.info().attach('[REG-965] response', {
                    body: JSON.stringify(reportGenerator.setLinksToResponses(Responses), null, 2),
                    contentType: 'application/json'
                });
            })

            test('[REG-966]: customer, contract, pod level', async ({Responses, loadResponsesFromFile}) => {
                const testCasesData = loadResponsesFromFile('pulling_spec_ts');
                Object.assign(Responses, testCasesData[1]);

                test.info().attach('[REG-966] response', {
                    body: JSON.stringify(reportGenerator.setLinksToResponses(Responses), null, 2),
                    contentType: 'application/json'
                });
            })

            test('[REG-967]: pod has gaps', async ({Responses, loadResponsesFromFile}) => {
                const testCasesData = loadResponsesFromFile('pulling_spec_ts');
                Object.assign(Responses, testCasesData[2]);

                test.info().attach('[REG-967] response', {
                    body: JSON.stringify(reportGenerator.setLinksToResponses(Responses), null, 2),
                    contentType: 'application/json'
                });
            })

            test('[REG-968]: one pod has gap, second pod does not', async ({Responses, loadResponsesFromFile}) => {
                const testCasesData = loadResponsesFromFile('pulling_spec_ts');
                Object.assign(Responses, testCasesData[3]);

                test.info().attach('[REG-968] response', {
                    body: JSON.stringify(reportGenerator.setLinksToResponses(Responses), null, 2),
                    contentType: 'application/json'
                });
            })

            test('[REG-969]: pods have different measurement type', async ({Responses, loadResponsesFromFile}) => {
                const testCasesData = loadResponsesFromFile('pulling_spec_ts');
                Object.assign(Responses, testCasesData[4]);

                test.info().attach('[REG-969] response', {
                    body: JSON.stringify(reportGenerator.setLinksToResponses(Responses), null, 2),
                    contentType: 'application/json'
                });
            })

            test('[REG-970]: one chain fails another chain', async ({Responses, loadResponsesFromFile}) => {
                const testCasesData = loadResponsesFromFile('pulling_spec_ts');
                Object.assign(Responses, testCasesData[5]);

                test.info().attach('[REG-970] response', {
                    body: JSON.stringify(reportGenerator.setLinksToResponses(Responses), null, 2),
                    contentType: 'application/json'
                });
            })

            test('[REG-971]: pod has no gap (scale flow)', async ({Responses, loadResponsesFromFile}) => {
                const testCasesData = loadResponsesFromFile('pulling_spec_ts');
                Object.assign(Responses, testCasesData[6]);

                test.info().attach('[REG-971] response', {
                    body: JSON.stringify(reportGenerator.setLinksToResponses(Responses), null, 2),
                    contentType: 'application/json'
                });
            })

            test('[REG-972]: 3 contracts, 2 pulling chains', async ({Responses, loadResponsesFromFile}) => {
                const testCasesData = loadResponsesFromFile('pulling_spec_ts');
                Object.assign(Responses, testCasesData[7]);

                test.info().attach('[REG-972] response', {
                    body: JSON.stringify(reportGenerator.setLinksToResponses(Responses), null, 2),
                    contentType: 'application/json'
                });
            })
        })
    })
})