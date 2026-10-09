import { test, expect } from "../../fixtures/baseFixture";
import priceParameterMP from '../../mass-imports/generators/domains/priceParameterMP';

test.describe('Price Parameter Mass Import Generation', {tag: '@@massImport'}, () => { 
    test('Generate 15-minute interval Excel file', async ({GeneratePayload}) => {
        const startDate = '2026-01-01 00:00:00';
        const endDate = '2026-01-31 23:00:00';
        
        const generator = new priceParameterMP([
            { startDate, endDate, amount: 100 }
        ]);
        
        await generator.generateFifteenMinutes();
    });
    
    test('Generate daily interval Excel file', async () => {
       const startDate = '2026-01-01 00:00:00';
        const endDate = '2026-01-31 23:00:00';
        
        const generator = new priceParameterMP([
            { startDate, endDate, amount: 200 }
        ]);
        
        await generator.generateDays();
    });
    
    test('Generate hourly interval Excel file', async () => {
        const startDate = '2026-01-01 00:00:00';
        const endDate = '2026-01-31 23:00:00';
        
        const generator = new priceParameterMP([
            { startDate, endDate, amount: 150 }
        ]);
        
        await generator.generateHours();
    });
});
