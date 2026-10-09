import { APIRequestContext, APIResponse } from '@playwright/test';

/**
 * Extended API Response that includes request metadata for customCheck
 */
export interface ExtendedAPIResponse extends APIResponse {
    _requestMetadata?: {
        endpoint: string;
        method: string;
        payload?: any;
    };
}

/**
 * Wrapper around APIRequestContext that captures request metadata
 * and attaches it to responses for automatic use in customCheck
 */
export class RequestWrapper {
    constructor(private context: APIRequestContext) {}

    /**
     * Normalize URL by removing leading slash to ensure proper baseURL resolution
     * When baseURL contains a path (e.g., /backend/phoenix-epres), a leading /
     * would replace it entirely instead of appending to it
     */
    private normalizeUrl(url: string): string {
        return url.startsWith('/') ? url.substring(1) : url;
    }

    async post(url: string, options?: { data?: any; [key: string]: any }): Promise<ExtendedAPIResponse> {
        const normalizedUrl = this.normalizeUrl(url);
        const response = await this.context.post(normalizedUrl, options) as ExtendedAPIResponse;
        
        // Attach metadata to response
        response._requestMetadata = {
            endpoint: normalizedUrl,
            method: 'POST',
            payload: options?.data
        };
        
        return response;
    }

    async get(url: string, options?: any): Promise<ExtendedAPIResponse> {
        const normalizedUrl = this.normalizeUrl(url);
        const response = await this.context.get(normalizedUrl, options) as ExtendedAPIResponse;
        
        response._requestMetadata = {
            endpoint: normalizedUrl,
            method: 'GET'
        };
        
        return response;
    }

    async put(url: string, options?: { data?: any; [key: string]: any }): Promise<ExtendedAPIResponse> {
        const normalizedUrl = this.normalizeUrl(url);
        const response = await this.context.put(normalizedUrl, options) as ExtendedAPIResponse;
        
        response._requestMetadata = {
            endpoint: normalizedUrl,
            method: 'PUT',
            payload: options?.data
        };
        
        return response;
    }

    async patch(url: string, options?: { data?: any; [key: string]: any }): Promise<ExtendedAPIResponse> {
        const normalizedUrl = this.normalizeUrl(url);
        const response = await this.context.patch(normalizedUrl, options) as ExtendedAPIResponse;
        
        response._requestMetadata = {
            endpoint: normalizedUrl,
            method: 'PATCH',
            payload: options?.data
        };
        
        return response;
    }

    async delete(url: string, options?: any): Promise<ExtendedAPIResponse> {
        const normalizedUrl = this.normalizeUrl(url);
        const response = await this.context.delete(normalizedUrl, options) as ExtendedAPIResponse;
        
        response._requestMetadata = {
            endpoint: normalizedUrl,
            method: 'DELETE'
        };
        
        return response;
    }

    // Expose the underlying context for any other methods
    get raw(): APIRequestContext {
        return this.context;
    }
}
