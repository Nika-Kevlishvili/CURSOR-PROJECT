import { test as base, request, APIRequestContext, expect as baseExpect, Page } from '@playwright/test';
import { OpenMainPage } from '../../frontend/pom/MainPage';
import { BurgerMenu } from '../../frontend/utils/BurgerMenu';
import { dropdownSelector } from '../../frontend/utils/dropdownSelector';
import { PageObjects } from '../../frontend/pom/PageObjects';
import { nomenclatures } from '../jsons/payloads/nomenclatures/nomenclatures';
import { GeneratePayload } from '../jsons/payloadGenerators/PayloadGenerator';
import { massImportGenerator } from '../mass-imports/generators/massImportGenerator';
import { RequestWrapper} from '../utils/RequestWrapper';
import { OnlinePaymentUrl } from '../utils/onlinePaymentUrls';
import { getToken, getSPToken} from './utils/auth';
import { normalizeBaseURL } from './utils/baseUrl';
import { ResponsesContainer, createResponsesContainer } from './types/responses';
import { checkResponseMatcher } from './matchers/checkResponse';
import { ResponseLinker } from '../utils/reporters/ResponseLinker';
import { Endpoints as EndpointsConst, EndpointsType } from './constants/endpoints';
import {saveResponsesToFile, loadResponsesFromFile, loadResponseById, clearStashedResponses} from './utils/stashResponses';
import { billingValidator } from "../validations/billingValidator";
import {receivableValidations} from "../validations/receivableValidations";

/**
 * Base fixture type definition for Playwright test context.
 * Provides access to API client, payload generators, response storage, and utilities.
 */
export type baseFixture = {
  Request: RequestWrapper;
  SPRequest: RequestWrapper;
  FileUploadRequest: APIRequestContext;
  Nomenclatures: nomenclatures;
  Responses: ResponsesContainer;
  GeneratePayload: GeneratePayload;
  MassImportGenerator: massImportGenerator;
  OnlinePaymentUrl: string;
  Endpoints: EndpointsType;
  validateInvoice: billingValidator;
  receivableValidations: receivableValidations;
  saveResponsesToFile: typeof saveResponsesToFile;
  loadResponsesFromFile: typeof loadResponsesFromFile;
  loadResponseById: typeof loadResponseById;
  clearStashedResponses: typeof clearStashedResponses;
  frontend: FrontendContext;
};

export type FrontendContext = {
  page: Page;
  burgerMenu: BurgerMenu;
  dropdownSelector: dropdownSelector;
  pom: PageObjects;
  openEntity: (responses: ResponsesContainer, entityType: keyof ResponsesContainer, index?: number) => Promise<void>;
};

/**
 * Extended Playwright test with custom fixtures.
 * Import this instead of @playwright/test in your test files.
 */
export const test = base.extend<baseFixture>({
  Request: async ({ baseURL }, use) => {
    const normalizedURL = normalizeBaseURL(baseURL);
    
    const context = await request.newContext({
      baseURL: normalizedURL,
      extraHTTPHeaders: {
        'Accept': '*/*',
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${getToken()}`,
      },
    });

    const wrapper = new RequestWrapper(context);
    await use(wrapper);
  },

  SPRequest: async ({ baseURL }, use) => {
    const normalizedURL = normalizeBaseURL(baseURL).replace(/\/$/, '');
    let salesPortalBaseURL = null
    if (normalizedURL.includes('phoenix2-dev')) {
      salesPortalBaseURL = 'http://10.236.20.11:7092/'
    }else {
      salesPortalBaseURL = 'http://10.236.20.81:7095/'
    }

    const context = await request.newContext({
      baseURL: salesPortalBaseURL,
      extraHTTPHeaders: {
        'Accept': '*/*',
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${getSPToken()}`,
      },
    });
    const wrapper = new RequestWrapper(context);
    await use(wrapper);
  },

  FileUploadRequest: async ({ baseURL }, use) => {
    const normalizedURL = normalizeBaseURL(baseURL);
    
    const context = await request.newContext({
      baseURL: normalizedURL,
      extraHTTPHeaders: {
        'Authorization': `Bearer ${getToken()}`,
      },
    });

    await use(context);
  },

  Nomenclatures: async ({ Request }, use) => {
    await use(new nomenclatures(Request.raw));
  },

  Responses: async ({}, use) => {
    await use(createResponsesContainer());
  },

  GeneratePayload: async ({ Request, Responses, FileUploadRequest }, use) => {
    await use(new GeneratePayload(Request.raw, Responses, FileUploadRequest));
  },

  MassImportGenerator: async ({ Request, FileUploadRequest, Responses }, use) => {
    await use(new massImportGenerator(Request.raw, FileUploadRequest, Responses));
  },

  Endpoints: async ({}, use) => {
    await use(EndpointsConst);
  },

  OnlinePaymentUrl: async ({}, use) => {
    const url = await OnlinePaymentUrl();
    await use(url);
  },

  saveResponsesToFile: async ({}, use) => {
    await use(saveResponsesToFile);
  },

  loadResponsesFromFile: async ({}, use) => {
    await use(loadResponsesFromFile);
  },

  loadResponseById: async ({}, use) => {
    await use(loadResponseById);
  },

  clearStashedResponses: async ({}, use) => {
    await use(clearStashedResponses);
  },
  validateInvoice: async ({ Request, Responses, GeneratePayload }, use) => {
    await use(new billingValidator(Request.raw, Responses, GeneratePayload));
  },
  receivableValidations: async ({ Request, Responses }, use) => {
    await use(new receivableValidations(Request.raw, Responses));
  },

  frontend: async ({ page, Responses }, use) => {
    let initialized = false;
    const token = getToken();

    const ensureInit = async () => {
      if (!initialized) {
        await OpenMainPage(page, token);
        await page.waitForSelector('.burger');
        initialized = true;
      }
    };

    const wrapLazy = <T extends object>(target: T): T =>
      new Proxy(target, {
        get(obj, prop) {
          const val = (obj as any)[prop];
          if (typeof val === 'function') {
            return async (...args: any[]) => {
              await ensureInit();
              return val.apply(obj, args);
            };
          }
          return val;
        },
      });

    const dropdown = new dropdownSelector(page);

    await use({
      page,
      burgerMenu: wrapLazy(new BurgerMenu(page)),
      dropdownSelector: wrapLazy(dropdown),
      pom: wrapLazy(new PageObjects(page, Responses, dropdown)),
      openEntity: async (responses, entityType, index = 0) => {
        const url = ResponseLinker.getEntityUrl(responses, entityType as string, index);
        if (!url) throw new Error(`No frontend URL found for entity '${entityType}' at index ${index}`);
        await ensureInit();
        try {
          await page.goto(url, { waitUntil: 'commit' });
        } catch (e: any) {
          if (!e.message?.includes('ERR_ABORTED')) throw e;
        }
      },
    });
  },

});

/**
 * Extended expect with custom CheckResponse matcher.
 * Import this instead of @playwright/test expect in your test files.
 */
export const expect = baseExpect.extend(checkResponseMatcher);
