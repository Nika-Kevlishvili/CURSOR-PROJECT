import {APIRequestContext} from '@playwright/test';
import * as dotenv from 'dotenv';

dotenv.config();

export default async function salesPortalTokenAuth(context: APIRequestContext) {
    const Request = context
    const grant_type = process.env.GRANT_TYPE ?? ''
    const client_id = process.env.CLIENT_ID ?? ''
    const client_secret = process.env.CLIENT_SECRET ?? ''
    const addr = `http://10.236.20.11:7092/sales-portal/oauth2/token?grant_type=${grant_type}&client_id=${encodeURIComponent(client_id)}&client_secret=${encodeURIComponent(client_secret)}`
    const tokenResponse = await Request.post(addr);
    const token = (await tokenResponse.json()).access_token;

    return token
}