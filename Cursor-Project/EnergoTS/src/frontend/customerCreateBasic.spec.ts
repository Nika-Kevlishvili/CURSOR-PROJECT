import { Page } from '@playwright/test';
import { randomGens } from '../backend/utils/randomGens';
import { test, expect } from './fixtures/FrontEndFixture';
import { LegalCustomerCreateArguments } from './pom/createPOM/CustomerCreateBasicPage';

type CreatedCustomer = {
    id: number;
    customerNumber: number;
    identifier: string;
    lastCustomerDetailId: number;
};

async function openCustomerCreateBasic(page: Page): Promise<void> {
    await page.locator('.burger').waitFor({ state: 'visible', timeout: 60_000 });
    await page.waitForURL((url) => !url.searchParams.has('jwt'), { timeout: 60_000 });
    const current = new URL(page.url());
    const appRoot = current.pathname.endsWith('/') ? current.pathname : `${current.pathname}/`;
    const target = new URL(`${appRoot}customers/create/basic`, current.origin);
    await page.evaluate((path) => {
        window.history.pushState({}, '', path);
        window.dispatchEvent(new PopStateEvent('popstate'));
    }, target.pathname);
    await page.waitForURL(target.pathname);
}

test.skip('Customer create basic creates a legal customer', async ({ POM, MainPage }) => {
    test.setTimeout(120_000);
    const customer = POM.customerCreateBasic;
    const identifier = randomGens.generateUniqueIdentifier();
    const createArguments: LegalCustomerCreateArguments = {
        identifier,
        name: `AUTOMATION ${identifier}`,
        nameTransliterated: `AUTOMATION ${identifier}`,
        legalForm: 'ЕООД',
        mainSubjectOfActivity: 'Automated frontend customer creation',
        segment: 'Неуточнен',
        populatedPlace: 'БЕНКОВСКИ - АВРЕН - ВАРНА - БЪЛГАРИЯ',
        zipCode: '9134',
        unrecognizedIdentifier: true,
    };

    await test.step('Precondition: open customer create basic', async () => {
        await openCustomerCreateBasic(MainPage);
        await customer.createCustomerButton.waitFor({ state: 'visible', timeout: 60_000 });
    });

    await test.step('Create a legal customer through the page object', async () => {
        const createResponsePromise = MainPage.waitForResponse((response) => {
            const request = response.request();
            return request.method() === 'POST' && new URL(response.url()).pathname.endsWith('/customer');
        });

        const submittedIdentifier = await customer.createLegalCustomer(createArguments);
        const createResponse = await createResponsePromise;
        const createdCustomer = (await createResponse.json()) as CreatedCustomer;

        expect(createResponse.status(), JSON.stringify(createdCustomer)).toBe(200);
        expect(submittedIdentifier).toBe(identifier);
        expect(createdCustomer.identifier).toBe(identifier);
        expect(createdCustomer.id).toBeGreaterThan(0);
        expect(createdCustomer.customerNumber).toBeGreaterThan(0);
        expect(createdCustomer.lastCustomerDetailId).toBeGreaterThan(0);

        await expect
            .poll(() => new URL(MainPage.url()).pathname)
            .not.toContain('/customers/create/basic');
        await expect(customer.heading).toContainText(createArguments.name);
    });
});
