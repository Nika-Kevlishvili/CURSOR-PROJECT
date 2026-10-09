import { test as setup, request as playwrightRequest, APIRequestContext } from '@playwright/test';
import * as fs from 'fs';
import * as path from 'path';
import { nomenclatures } from '../../backend/jsons/payloads/nomenclatures/nomenclatures';
import {tokenAuth} from '../../backend/fixtures/login';
import { templates } from '../../backend/jsons/payloads/nomenclatures/templates';
import salesPortalTokenAuth from '../../backend/fixtures/salesPortalLogin';

const envFilePath = path.resolve('src/backend/fixtures', 'envVariables.json');
const tokenFilePath = path.resolve('src/backend/fixtures', 'token.json');
const salesPortalDataPath = path.resolve('src/backend/fixtures', 'salesPortalToken.json');


setup('global setup', async ({ baseURL }) => {
    console.log('Global setup executed');
    
    // Set BASE_URL in process.env so tokenAuth() can access it
    if (!process.env.BASE_URL && baseURL) {
        process.env.BASE_URL = baseURL;
        console.log(`Using BASE_URL from config: ${baseURL}`);
    }
    
    const token = await tokenAuth();
    fs.writeFileSync(tokenFilePath, JSON.stringify({ token }, null, 2));
    
    let actualBaseURL = process.env.BASE_URL || baseURL || 'https://devapps.energo-pro.bg/backend/phoenix1-dev/';

    const headersForNomenclatures = {
        'Accept': '*/*',
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${token}`,
    };

    const headersForTemplates = {
        'Authorization': `Bearer ${token}`,
    };
    
    // Ensure baseURL ends with trailing slash for proper path resolution
    if (!actualBaseURL.endsWith('/')) {
        actualBaseURL += '/';
    }

    const apiRequestContextNomenclatures = await playwrightRequest.newContext({
        baseURL: actualBaseURL,
        extraHTTPHeaders: headersForNomenclatures,
    });

    const apiRequestContextTemplates = await playwrightRequest.newContext({
        baseURL: actualBaseURL,
        extraHTTPHeaders: headersForTemplates,
    });

    try {
        const salesPortalAuth = await salesPortalTokenAuth(apiRequestContextNomenclatures);
        fs.writeFileSync(salesPortalDataPath, JSON.stringify(salesPortalAuth, null, 2));
    } catch (e) {
        console.warn(`⚠️ Sales portal authentication failed: ${e instanceof Error ? e.message : e}. Continuing setup.`);
    }

    const nom = new nomenclatures(apiRequestContextNomenclatures);
    const template = new templates(apiRequestContextTemplates, apiRequestContextNomenclatures);

    const templateData = await template.generateEveryTemplate();
    const nomenclaturedata = await nom.generateNomenclatures(templateData);
    
    const variables = {
        ...nomenclaturedata,
        ...templateData
    };

    fs.writeFileSync(envFilePath, JSON.stringify(variables, null, 4), 'utf-8');
    console.log('env file ready');
    
    await apiRequestContextNomenclatures.dispose();
    await apiRequestContextTemplates.dispose();
});