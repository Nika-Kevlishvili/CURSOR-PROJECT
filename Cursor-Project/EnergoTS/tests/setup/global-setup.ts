import { test as setup, request as playwrightRequest, APIRequestContext } from '@playwright/test';
import * as fs from 'fs';
import * as path from 'path';
import { nomenclatures } from '../../jsons/payloads/create/nomenclatures/nomenclatures';
import {tokenAuth} from '../../fixtures/login';
import { templates } from '../../jsons/payloads/create/nomenclatures/templates';
import salesPortalTokenAuth from '../../fixtures/salesPortalLogin';

const projectRoot = path.resolve(__dirname, '..', '..');
const fixturesDir = path.join(projectRoot, 'fixtures');
const envFilePath = path.join(fixturesDir, 'envVariables.json');
const tokenFilePath = path.join(fixturesDir, 'token.json');
const salesPortalDataPath = path.join(fixturesDir, 'salesPortalToken.json');


setup('global setup', async ({ baseURL }) => {
    console.log('Global setup executed');
    
    // Set BASE_URL in process.env so tokenAuth() can access it
    if (!process.env.BASE_URL && baseURL) {
        process.env.BASE_URL = baseURL;
        console.log(`Using BASE_URL from config: ${baseURL}`);
    }
    
    const token = await tokenAuth();
    fs.writeFileSync(tokenFilePath, JSON.stringify({ token }, null, 2));
    
    let actualBaseURL = process.env.BASE_URL || baseURL || 'http://10.236.20.11:8091/';

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