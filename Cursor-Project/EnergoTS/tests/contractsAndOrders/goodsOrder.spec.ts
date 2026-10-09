import {test, expect} from '../../fixtures/baseFixture';
import reportGenerator from '../../utils/generateReport';

test.describe('[REG-52]: Contracts and Orders - Product Contract', {tag: '@contractsAndOrders'}, () => {  
    test.describe('[REG-87]: Goods order', async () => {
        test.describe('[REG-508]: Create Goods order', async () => {
            test(' [REG-945]: Goods order - Happy path - Invoice generation', {tag: '@customer'}, async ({Request, GeneratePayload, Responses, Endpoints}) =>{
                await test.step('Create Customer', async () => {
                    const customer = await Request.post(Endpoints.customer, {data: GeneratePayload.customers.customer_legal()});
                    await expect(customer).CheckResponse();
                    Responses.customer.push(await customer.json());
                });

                await test.step('Create Goods', async () => {
                    const goods = await Request.post(Endpoints.goods, {data: GeneratePayload.productAndServices.goods()});
                    await expect(goods).CheckResponse();
                    Responses.goods.push(await goods.json());
                });

                 await test.step('Create Goods', async () => {
                    const goods = await Request.post(Endpoints.goods, {data: GeneratePayload.productAndServices.goods()});
                
                    await expect(goods).CheckResponse();
                    Responses.goods.push(await goods.json());
                });

                await test.step('Create Goods order', async () => {
                    const goodsOrder = await Request.post(Endpoints.goodsOrder, {data: await GeneratePayload.contractsAndOrders.goodsOrder()});
                    Responses.goodsOrder.push(await goodsOrder.json());
                    await expect(goodsOrder).CheckResponse();
                });
                
                await test.step('Create proforma invoice', async () => {
                    const proforma = await Request.post(`goods-order/${await Responses.goodsOrder[0]}/issue-proforma-invoice`);    
                    await expect(proforma).CheckResponse();        
                });

                await test.step('Start generating', async () => {
                    const startGenerating = await Request.patch(`goods-order/${await Responses.goodsOrder[0]}/start-generating`);   
                    await expect(startGenerating).CheckResponse();        
                });

                await test.step('Start accounting', async () => {
                    const startAccounting = await Request.patch(`goods-order/${await Responses.goodsOrder[0]}/start-accounting`);    
                    await expect(startAccounting).CheckResponse();
                })

                await test.step('Start issuing', async () => {
                    const startIssuing = await Request.patch(`goods-order/${await Responses.goodsOrder[0]}/issue-invoice`);    
                    await expect(startIssuing).CheckResponse();
                })

                await test.step('compearing amounts', async () => {  
                    const getGoodsOrder = await Request.get(`goods-order/${await Responses.goodsOrder[0]}?version=1`);
                    const goodsOrderjson = await getGoodsOrder.json();

                    let sum = 0;
                    for (const goods of goodsOrderjson.goodsParametersResponse.goods) {
                    const price = parseFloat(goods.price);     
                    const quantity = parseInt(goods.quantity);  
                    const vatRate = parseFloat(goodsOrderjson.goodsParametersResponse.vatRate.valueInPercent);

                    if (isNaN(price) || isNaN(quantity)) {
                    
                        continue; // skip this good
                    }
                    sum += price * quantity + (price * quantity * vatRate / 100);
                    sum = Math.round(sum * 100) / 100; // round to 2 decimal places
                    }
                    
                    const goodsinvoice = await goodsOrderjson.basicParametersResponse.invoice.id;
                    const getInvoice = await Request.get(`invoice?id=${goodsinvoice}`);
                    expect(sum).toBe(parseFloat((await getInvoice.json()).totalAmountIncludingVat));
                  
                })

                test.info().attach('[REG-945] response', {
                  body: JSON.stringify(reportGenerator.setLinksToResponses(Responses), null, 2),
                  contentType: 'application/json'
                });
            });
        });
    });
});