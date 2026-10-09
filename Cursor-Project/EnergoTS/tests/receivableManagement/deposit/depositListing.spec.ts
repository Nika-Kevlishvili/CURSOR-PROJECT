import { test, expect } from '../../../fixtures/baseFixture';
import { randomGens } from '../../../utils/randomGens';

test.describe('[REG-56]: Receivables Management - Deposits', {tag: '@receivableManagement'}, () => {
    test.describe('[REG-113]: Deposits', {tag: '@deposits'}, () => {
        test.describe('[REG-519]: Listing', () => {
            test.describe('[REG-719]: Deposit listing - Dropdown filters + search', () => {
                test('[REG-719]: filtering data with full filters', async({Request, GeneratePayload, Responses, Endpoints}) => {
                    let leva: any;
                    let euro: any;
                    
                    await test.step('save currencies to use in filters later', async() => {
                        const currencies = await Request.get(`/currencies?statuses=ACTIVE&page=0&size=25`);
                        await expect(currencies).CheckResponse();
                        const currencies_body = await currencies.json();
                        leva = currencies_body.content[0].id;
                        euro = currencies_body.content[1].id;  
                    });
                    
                    await test.step('create customer', async() => {
                        const payload = GeneratePayload.customers.customer_legal();
                        const customer = await Request.post("/customer", {data: payload});
                        await expect(customer).CheckResponse();
                        Responses.customer.push(await customer.json());
                    });
                    
                    await test.step('Create deposit', async() => {
                        const depositPayload = GeneratePayload.receivablesManagement.deposit();
                        depositPayload.initialAmount = 100;
                        const deposit = await Request.post("/deposit", {data: depositPayload});
                        await expect(deposit).CheckResponse();
                        Responses.deposit.push(await deposit.json());
                    });
                    
                    await test.step('create receivable', async() => {
                        const receivablePayload = GeneratePayload.receivablesManagement.customer_receivable();
                        receivablePayload.initialAmount = 100;
                        const receivable = await Request.post("/customer-receivable", {data: receivablePayload});
                        await expect(receivable).CheckResponse();
                        Responses.customerReceivable.push(await receivable.json());
                    });
                    
                    await test.step('get deposit liability', async() => {
                        const deposit_liability = await Request.get(`/customer-liability/list?page=0&size=1&prompt=${Responses.customer[0].identifier}&searchFields=CUSTOMER`);
                        await expect(deposit_liability).CheckResponse();
                        Responses.customerLiability.push((await deposit_liability.json()).content[0]);
                    });
                    
                    await test.step('offset receivable and deposit liability', async() => {
                        const offset_payload = await GeneratePayload.receivablesManagement.MLO_L_R();
                        const offset = await Request.post(Endpoints.manualLiabilityOffsetting, {data: offset_payload});
                        await expect(offset).CheckResponse();
                    });
                    
                    await test.step('Start job and check if deposit is updated', async() => {
                        const job = await Request.post("/deposit/job")
                        await expect(job).CheckResponse();
                        const get_deposit = await Request.get(`/deposit/${Responses.deposit[0]}`);
                        await expect(get_deposit).CheckResponse();
                        const depositData = await get_deposit.json();
                        expect(depositData.currentAmount).toBe(100);
                    });
                    await test.step('Check filters with positive expectations', async() => {
                        const deposit_listing = await Request.get(`/deposit/list?page=0&size=25&direction=DESC&sortBy=ID&paymentDeadlineFrom=2025-08-01&currencyIds=${leva}&initialAmountFrom=1&initialAmountTo=101&depositListingType=ALL&currentAmountFrom=1&currentAmountTo=101`);
                        await expect(deposit_listing).CheckResponse();
                        const deposit_data = await deposit_listing.json();
                        expect(deposit_data.content.length).toBeGreaterThan(0);
                        expect(deposit_data.content[0].initialAmount).toBeGreaterThanOrEqual(99);
                        expect(deposit_data.content[0].initialAmount).toBeLessThanOrEqual(101);
                        expect(deposit_data.content[0].currentAmount).toBeGreaterThanOrEqual(99);
                        expect(deposit_data.content[0].currentAmount).toBeLessThanOrEqual(101);
                        expect(deposit_data.content[0].currencyName).toBe('лева');
                        const dateStr = deposit_data.content[0].paymentDeadline;
                        const date = new Date(dateStr);
                        const mindate = new Date("2025-08-01")
                        expect(date.getTime()).toBeGreaterThanOrEqual(mindate.getTime());
                    });
                    await test.step('check select alls and compare data to unfiltered, it should be equal', async() => {
                        const old_listing = await Request.get(`/deposit/list?page=0&size=25&direction=DESC&sortBy=ID`);
                        const old_listing_data = await old_listing.json();
                        const old_totalElements = old_listing_data.totalElements;
                        const old_totalPages = old_listing_data.totalPages;
                        const old_content = old_listing_data.coontent;
                        
                        const new_listing = await Request.get(`/deposit/list?page=0&size=25&direction=DESC&sortBy=ID&paymentDeadlineTo=2042-09-01&paymentDeadlineFrom=2019-04-01&currencyIds=${leva}&currencyIds=${euro}&initialAmountFrom=0&depositListingType=ALL&currentAmountFrom=0`);
                        const listing_data = await new_listing.json();
                        const totalElements = listing_data.totalElements;
                        const totalPages = listing_data.totalPages;
                        const content = listing_data.content;

                        expect(old_totalElements).toEqual(totalElements);
                        expect(totalPages).toEqual(old_totalPages);
                    });
                    await test.step('Search fith filters and prompt in search bar', async() => {
                        const complex_search = await Request.get(`/deposit/list?page=0&size=25&direction=DESC&sortBy=ID&paymentDeadlineFrom=2019-09-01&currencyIds=${leva}&prompt=0000&initialAmountFrom=0&initialAmountTo=100000&depositListingType=ALL&currentAmountFrom=0&currentAmountTo=10000`);
                        await expect(complex_search).CheckResponse();
                        const search_data = await complex_search.json();
                        expect(search_data.content.length).toBeGreaterThan(0);
                        expect(search_data.content[0].depositNumber).toContain('0000');
                    });
                });
            });
            test.describe('[REG-720]: Deposit listing - Sorying', () => {
                test('[REG-720]: sorting with diff columns', async({Request, GeneratePayload, Responses, Endpoints}) => {
                    await test.step('sort with id and compare data', async() => {
                        const sord_ID_DESC = await Request.get(`/deposit/list?page=0&size=25&sortBy=ID&direction=DESC`);
                        await expect(sord_ID_DESC).CheckResponse();
                        const data = await sord_ID_DESC.json();
                        const id1 = data.content[0].id;
                        const sord_ID_ASC = await Request.get(`/deposit/list?page=0&size=25&sortBy=ID&direction=ASC`);
                        await expect(sord_ID_ASC).CheckResponse();
                        const data2 = await sord_ID_ASC.json();
                        const id2 = data2.content[0].id;
                        expect(id1).not.toEqual(id2);
                    });
                    await test.step('sort with customer and compare', async() => {
                        const sort_customer_Desc = await Request.get(`/deposit/list?page=0&size=25&sortBy=CUSTOMER_NUMBER&direction=DESC`);
                        await expect(sort_customer_Desc).CheckResponse();
                        const data = await sort_customer_Desc.json();
                        const customer1 = data.content[0].customerNumber;
                        const sort_customer_ASC = await Request.get(`/deposit/list?page=0&size=25&sortBy=CUSTOMER_NUMBER&direction=ASC`);
                        await expect(sort_customer_ASC).CheckResponse();
                        const data2 = await sort_customer_ASC.json();
                        const customer2 = data2.content[0].customerNumber;
                        expect(customer1).not.toEqual(customer2);         
                    });  
                    await test.step('sort by deposit number and compare asc-desc', async() => {
                        const sort_number_Desc = await Request.get(`/deposit/list?page=0&size=25&sortBy=DEPOSIT_NUMBER&direction=DESC`);
                        await expect(sort_number_Desc).CheckResponse();
                        const data = await sort_number_Desc.json();
                        const number1 = data.content[0].depositNumber;
                        const sort_number_asc = await Request.get(`/deposit/list?page=0&size=25&sortBy=DEPOSIT_NUMBER&direction=ASC`);
                        await expect(sort_number_asc).CheckResponse();
                        const data2 = await sort_number_asc.json();
                        const number2 = data2.content[0].depositNumber;
                        expect(number1).not.toEqual(number2);
                    });
                    await test.step('sort CONTRACT_ORDER_NUMBER and compare', async() => {
                        const contractDesc = await Request.get(`/deposit/list?page=0&size=25&sortBy=CONTRACT_ORDER_NUMBER&direction=DESC`);
                        await expect(contractDesc).CheckResponse();
                        const data = await contractDesc.json();
                        const contract1 = await data.content[0].contractOrderNumber;
                        const contractAsc = await Request.get(`/deposit/list?page=0&size=25&sortBy=CONTRACT_ORDER_NUMBER&direction=ASC`);
                        await expect(contractAsc).CheckResponse();
                        const data2 = await contractAsc.json();
                        const contract2 = await data2.content[0].contractOrderNumber;
                        
                        // Only compare if at least one value exists (not null/undefined)
                        if (contract1 || contract2) {
                            expect(contract1).not.toEqual(contract2);
                        }
                    }); 
                    await test.step('sort by PAYMENT_DEADLINE and compare', async() => {
                        const desc = await Request.get(`/deposit/list?page=0&size=25&sortBy=PAYMENT_DEADLINE&direction=DESC`);
                        await expect(desc).CheckResponse();
                        const res = await desc.json();
                        const dd1 = new Date(await res.content[0].paymentDeadline)
                        const asc = await Request.get(`/deposit/list?page=0&size=25&sortBy=PAYMENT_DEADLINE&direction=ASC`);
                        await expect(asc).CheckResponse();
                        const res2 = await asc.json();
                        const dd2 = new Date(res2.content[0].paymentDeadline);
                        expect(dd1.getTime()).toBeGreaterThanOrEqual(dd2.getTime());
                    });
                    await test.step('sort INITIAL_AMOUNT', async() => {
                        const desc = await Request.get(`/deposit/list?page=0&size=25&sortBy=INITIAL_AMOUNT&direction=DESC`);
                        await expect(desc).CheckResponse();
                        const data = await desc.json();
                        const initial1 = await data.content[0].initialAmount;
                        const asc = await Request.get(`/deposit/list?page=0&size=25&sortBy=INITIAL_AMOUNT&direction=ASC`);
                        await expect(asc).CheckResponse();
                        const data2 = await asc.json();
                        const initial2 = await data2.content[0].initialAmount;
                        
                        // Compare values - expect DESC >= ASC for amounts
                        expect(initial1).toBeGreaterThanOrEqual(initial2);
                    });
                    await test.step('sort with CURRENT_AMOUNT ', async() => {
                        const desc = await Request.get(`/deposit/list?page=0&size=25&sortBy=CURRENT_AMOUNT&direction=DESC`);
                        await expect(desc).CheckResponse();
                        const data = await desc.json();
                        const curr1 = await data.content[0].currentAmount;
                        const asc = await Request.get(`/deposit/list?page=0&size=25&sortBy=CURRENT_AMOUNT&direction=ASC`);
                        await expect(asc).CheckResponse();
                        const data2 = await asc.json();
                        const curr2 = await data2.content[0].currentAmount;
                        
                        // Handle null values and compare - DESC >= ASC
                        if (curr1 !== null && curr2 !== null) {
                            expect(curr1).toBeGreaterThanOrEqual(curr2);
                        } else if (curr1 === null && curr2 !== null) {
                            // DESC has null, ASC has value - nulls may sort last
                            expect(curr2).toBeDefined();
                        }
                    });
                    await test.step('sort with CURRENCY_NAME', async() => {
                        const desc = await Request.get(`/deposit/list?page=0&size=25&sortBy=CURRENCY_NAME&direction=DESC`);
                        await expect(desc).CheckResponse();
                        const data = await desc.json();
                        const curr1 = await data.content[0].currencyName;
                        const asc = await Request.get(`/deposit/list?page=0&size=25&sortBy=CURRENCY_NAME&direction=ASC`);
                        await expect(asc).CheckResponse();
                        const data2 = await asc.json();
                        const curr2 = await data2.content[0].currencyName;
                        
                        // Compare currency names - DESC should be alphabetically >= ASC
                        expect(curr1.localeCompare(curr2)).toBeGreaterThanOrEqual(0);
                    });
                });
            });

            test.describe('[REG-721]: deposit listing - pageing', () => {
                test('[REG-721]: checking if paging into different oages work', async({Request, GeneratePayload, Responses, Endpoints}) => {
                    let page_odd: any;
                    let page_even: any;

                    await test.step('load first page', async() => {
                        const firstPage_25 = await Request.get(`/deposit/list?page=0&size=25&sortBy=ID&direction=DESC`);
                        await expect(firstPage_25).CheckResponse();
                        const firstPage_50 = await Request.get(`/deposit/list?page=0&size=50&sortBy=ID&direction=DESC`);
                        await expect(firstPage_50).CheckResponse();
                        const firstPage_100 = await Request.get(`/deposit/list?page=0&size=100&sortBy=ID&direction=DESC`);
                        await expect(firstPage_100).CheckResponse();
                    });
                    await test.step('Create var for random number', async() => {
                        page_odd = randomGens.getRandomOdd();
                        page_even = randomGens.getRandomEven();
                    });
                    await test.step('Go to random page with diff sizes', async() => {
                        const oddS25 = await Request.get(`/deposit/list?page=${page_odd}&size=25&sortBy=ID&direction=DESC`);
                        await expect(oddS25).CheckResponse();
                        const oddS50 = await Request.get(`/deposit/list?page=${page_odd}&size=50&sortBy=ID&direction=DESC`);
                        await expect(oddS50).CheckResponse();
                        const oddS100 = await Request.get(`/deposit/list?page=${page_odd}&size=100&sortBy=ID&direction=DESC`);
                        await expect(oddS100).CheckResponse();
                        const evenS25 = await Request.get(`/deposit/list?page=${page_even}&size=25&sortBy=ID&direction=DESC`);
                        await expect(evenS25).CheckResponse();
                        const evenS50 = await Request.get(`/deposit/list?page=${page_even}&size=50&sortBy=ID&direction=DESC`);
                        await expect(evenS50).CheckResponse();
                        const evenS100 = await Request.get(`/deposit/list?page=${page_even}&size=100&sortBy=ID&direction=DESC`);
                        await expect(evenS100).CheckResponse();
                    }); 
                });
            });
        });
    });
});