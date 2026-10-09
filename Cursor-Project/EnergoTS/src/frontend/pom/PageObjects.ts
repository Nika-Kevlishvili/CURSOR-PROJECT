import { Page } from '@playwright/test';
import { ENV_VARIABLES } from '../fixtures/FrontEndFixture';
import { dropdownSelector } from '../utils/dropdownSelector';
import { CustomerCreateBasicPage } from './createPOM/CustomerCreateBasicPage';

export class PageObjects {
    readonly customerCreateBasic: CustomerCreateBasicPage;

    constructor(page: Page, envVariables: ENV_VARIABLES, dropdown: dropdownSelector) {
        this.customerCreateBasic = new CustomerCreateBasicPage(page, dropdown);
    }
}
