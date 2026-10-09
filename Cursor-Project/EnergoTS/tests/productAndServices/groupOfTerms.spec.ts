import {test, expect} from '../../fixtures/baseFixture';
import reportGenerator from '../../utils/generateReport';

test.describe('[REG-53]: Product and Services - Group of Terms', {tag: '@productsAndServices'}, () => {
  test.describe('[REG-95]: Group of Terms', () => {
    test("[REG-262]: Create group of terms", async ({Request, GeneratePayload, Responses, Endpoints}) => {
      let term;
      let groupOfTerms;
      let groupOfTermsResponse;

      await test.step('Create term', async () => {
        term = await Request.post(Endpoints.terms, {data: GeneratePayload.productAndServices.term()})
        Responses.terms.push(await term.json())

        await expect(term).CheckResponse();
      });

      await test.step('Create group of terms and validate', async () => {
        groupOfTerms = await Request.post(Endpoints.termsGroup, {data: GeneratePayload.productAndServices.groupOfTerm()})
        await expect(groupOfTerms).CheckResponse();

        Responses.termsGroup.push(await groupOfTerms.json());
      });

      test.info().attach('[REG-262] response', {
        body: JSON.stringify(reportGenerator.setLinksToResponses(Responses), null, 2),
        contentType: 'application/json'
      });
    });
  });
});