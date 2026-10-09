import {test, expect} from '../../backend/fixtures/baseFixture';
import reportGenerator from '../../backend/utils/generateReport';

test.describe('[REG-53]: Product and Services - Terms', {tag: '@productsAndServices'}, () => {
  test.describe('[REG-94]: Terms', () => {
    test('[REG-249]: Create term', async ({ Request, GeneratePayload, Endpoints, Responses}) => {
      const term = await Request.post(Endpoints.terms, {data: GeneratePayload.productAndServices.term()})
      await expect(term).CheckResponse();
      Responses.terms.push(await term.json());

      test.info().attach('[REG-249] response', {
        body: JSON.stringify(reportGenerator.setLinksToResponses({Responses}), null, 2),
        contentType: 'application/json'
      });
    });
  });
});