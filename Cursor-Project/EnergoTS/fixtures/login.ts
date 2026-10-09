import { request } from '@playwright/test';
import * as dotenv from 'dotenv';
import * as path from 'path';

dotenv.config({ path: path.resolve(__dirname, '..', '.env') });

export async function tokenAuth() {
    // Get BASE_URL and normalize it (remove trailing slash for comparison)
    const baseUrl = (process.env.BASE_URL || 'http://10.236.20.11:8091/').replace(/\/$/, '');
    
    console.log(`BASE_URL detected: ${baseUrl}`);
    
    let authEnvironment;
    if (baseUrl.startsWith('http://10.236.20.11:8091')
        || baseUrl.startsWith('http://10.236.20.11:8092')
        || baseUrl.startsWith('https://devapps.energo-pro.bg/backend/phoenix1-dev') 
        || baseUrl.startsWith('http://10.236.20.81:8091')
        || baseUrl.startsWith('http://10.236.20.81:8094')
        || baseUrl.startsWith('https://devapps.energo-pro.bg/backend/phoenix2-dev')) {
        authEnvironment = process.env.DEVAUTHAPI;
        console.log('Selected DEVAUTHAPI');
    } else if (baseUrl.startsWith('https://testapps.energo-pro.bg/backend/phoenix-epres')
        || baseUrl.startsWith('http://10.236.20.81:8095')) {
        authEnvironment = process.env.TESTAUTHAPI;
        console.log('Selected TESTAUTHAPI');
    }

    if (!authEnvironment) {
        throw new Error(`No auth endpoint configured for BASE_URL: ${baseUrl}. Set DEVAUTHAPI or TESTAUTHAPI.`);
    }

    console.log(`Using auth endpoint: ${authEnvironment}`);

    const apiRequestContext = await request.newContext({
        extraHTTPHeaders: {
            'Accept': '*/*',
            'Content-Type': 'application/json',
        },
    });

    // POST directly to the full auth URL (not using baseURL since it's already complete)
    let response = await apiRequestContext.post(authEnvironment, {
        data: {
            user: process.env.PORTAL_USER,
            password: process.env.PASSWORD
        }
    });

    console.log(`Auth response status: ${response.status()}`);

    // Retry once on 401
    if(response.status() === 401){
        console.log('Got 401, retrying...');
        response = await apiRequestContext.post(authEnvironment, {
            data: {
                user: process.env.PORTAL_USER,
                password: process.env.PASSWORD
            }
        });
        console.log(`Retry status: ${response.status()}`);
    }

    const responseBody = await response.json();
    const token = responseBody.jwt;

    if (!token) {
        throw new Error(`Failed to get token from ${authEnvironment}. Status: ${response.status()}`);
    }

    console.log('✅ Authentication successful');
    return token;
}
