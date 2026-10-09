import playwrightRequest from "@playwright/test"
import { test as base, request, APIRequestContext, expect as baseExpect} from '@playwright/test';
import { nomenclatures } from '../jsons/payloads/create/nomenclatures/nomenclatures';
import { GeneratePayload } from '../jsons/payloadGenerators/PayloadGenerator';
import { massImportGenerator } from '../mass-imports/generators/massImportGenerator';
import { RequestWrapper} from '../utils/RequestWrapper';
import { OnlinePaymentUrl } from '../utils/onlinePaymentUrls';
import { getToken, getSPToken} from './utils/auth';
import { normalizeBaseURL } from './utils/baseUrl';
import { ResponsesContainer, createResponsesContainer } from './types/responses';
import { checkResponseMatcher } from './matchers/checkResponse';
import { Endpoints as EndpointsConst, EndpointsType } from './constants/endpoints';
import {saveResponsesToFile, loadResponsesFromFile, clearStashedResponses} from './utils/stashResponses';
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
  clearStashedResponses: typeof clearStashedResponses;
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
    // const normalizedURL = normalizeBaseURL(baseURL).replace(/\/$/, '');

    const salesPortalBaseURL = 'http://10.236.20.11:7092/sales-portal/';
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

  // MassImportGenerator: async ({ Request, FileUploadRequest, Responses }, use) => {
  //   await use(new massImportGenerator(Request.raw, FileUploadRequest, Responses));
  // },

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

  clearStashedResponses: async ({}, use) => {
    await use(clearStashedResponses);
  },
  validateInvoice: async ({ Request, Responses }, use) => {
    await use(new billingValidator(Request.raw, Responses));
  },
  receivableValidations: async ({ Request, Responses }, use) => {
    await use(new receivableValidations(Request.raw, Responses));
  }
  
});

/**
 * Extended expect with custom CheckResponse matcher.
 * Import this instead of @playwright/test expect in your test files.
 */
export const expect = baseExpect.extend(checkResponseMatcher);
