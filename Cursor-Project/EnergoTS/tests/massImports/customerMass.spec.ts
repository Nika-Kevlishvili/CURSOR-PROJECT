import { test, expect } from "../../fixtures/baseFixture";
import reportGenerator from '../../utils/generateReport';

// test.describe('[REG-946]: Customer Mass Import', {tag: ['@massImport','@customer']}, () => {
//     test.describe('[REG-947]: Generate and upload Customer Mass Import Excel',
//         {tag: ['@customer', '@massImport']}, () => {
//         test('Customer mass import', async ({ MassImportGenerator }) => {
//             const payload = MassImportGenerator.CustomerMassImportGenerator.legalCustomerMP();
//             const res = await MassImportGenerator.generateAndUpload(payload);
//             expect(res.success, res.message ?? undefined).toBe(true);

//             test.info().attach('[REG-947] response', {
//                 body: JSON.stringify(reportGenerator.setLinksToResponses({}), null, 2),
//                 contentType: 'application/json'
//             });
//         });
//     })
// })