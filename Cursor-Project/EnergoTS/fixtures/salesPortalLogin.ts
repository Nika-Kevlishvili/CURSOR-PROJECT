import {APIRequestContext} from '@playwright/test';
import * as dotenv from 'dotenv';
import { normalizeBaseURL } from './utils/baseUrl';

dotenv.config();

export default async function salesPortalTokenAuth(context: APIRequestContext) {
    const normalizedURL = normalizeBaseURL().replace(/\/$/, '');
    let salesPortalBaseURL = null
    if (normalizedURL.includes('phoenix2-dev')) {
      salesPortalBaseURL = 'http://10.236.20.11:7092/'
    }else {
      salesPortalBaseURL = 'http://10.236.20.81:7095/'
    }


    const Request = context
    const grant_type = process.env.GRANT_TYPE ?? ''
    const client_id = process.env.CLIENT_ID ?? ''
    const client_secret = process.env.CLIENT_SECRET ?? ''
    const addr = `${salesPortalBaseURL}oauth2/token?grant_type=${grant_type}&client_id=${encodeURIComponent(client_id)}&client_secret=${encodeURIComponent(client_secret)}`
    const tokenResponse = await Request.post(addr);
    const token = (await tokenResponse.json()).access_token;

    console.log(`Sales Portal url: ${salesPortalBaseURL}`);
    return token
}