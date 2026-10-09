import { test as base, Page } from '@playwright/test';
import { ResponsesContainer, createResponsesContainer } from '../../backend/fixtures/types/responses';
import { getToken } from '../../backend/fixtures/utils/auth';
import { OpenMainPage } from '../pom/MainPage';
import { BurgerMenu } from '../utils/BurgerMenu';
import { dropdownSelector } from '../utils/dropdownSelector';
import { PageObjects } from '../pom/PageObjects';

export type ENV_VARIABLES = ResponsesContainer;



type frontEndFixture = {
    MainPage: Page;
    BurgerMenu: BurgerMenu;
    DropdownSelector: dropdownSelector;
    POM: PageObjects;
    Responses: ResponsesContainer;
}

export const test = base.extend<frontEndFixture>({

    MainPage: async ({ page }, use) => {
        const devPage = await OpenMainPage(page, getToken());
        await use(devPage);
    },

    DropdownSelector: async ({ MainPage }, use) => {
        await use(new dropdownSelector(MainPage));
    },

    BurgerMenu: async ({ MainPage }, use) => {
        await use(new BurgerMenu(MainPage));
    },

    Responses: async ({}, use) => {
        await use(createResponsesContainer());
    },

    POM: async ({ MainPage, Responses, DropdownSelector }, use) => {
        await use(new PageObjects(MainPage, Responses, DropdownSelector));
    },
});

export { expect } from '@playwright/test';