import { request as playwrightRequest } from '@playwright/test';
import * as fs from 'fs';
import * as path from 'path';
import { SlackReporter } from './reporters/SlackReporter';



export async function OnlinePaymentUrl(): Promise<string> {
    const baseUrl = new SlackReporter
    let currentUrl = await baseUrl.resolveBaseURL();

    const normalizedBaseUrl = currentUrl.replace(/\/$/, '');

    if (normalizedBaseUrl.includes('10.236.20.11') || normalizedBaseUrl.includes('10.236.20.11:8091')) {
        currentUrl = 'http://10.236.20.11:9091/';
    } else if (normalizedBaseUrl.includes('10.236.20.81') || normalizedBaseUrl.includes('10.236.20.81:8091')) {
        currentUrl = 'http://10.236.20.81:9091/';
    } else if (normalizedBaseUrl.includes('phoenix-dev2') || normalizedBaseUrl.includes('devapps.energo-pro.bg')) {
        currentUrl = 'http://10.236.20.11:9092/';
    } else if (normalizedBaseUrl.includes('phoenix-epres') || normalizedBaseUrl.includes('testapps.energo-pro.bg')) {
        currentUrl = 'http://10.236.20.31:9091/';
    }
    return currentUrl;
}