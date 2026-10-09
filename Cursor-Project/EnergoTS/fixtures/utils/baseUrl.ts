/**
 * Store for the configured base URL, used in error messages and request contexts.
 * Exported as a mutable variable to allow updates during test execution.
 */
export let configuredBaseURL: string;

/**
 * Normalizes the base URL by ensuring it ends with a trailing slash.
 * This is necessary for proper path resolution in API requests.
 * 
 * @param baseURL - The base URL from environment or config
 * @returns Normalized URL with trailing slash
 */
export function normalizeBaseURL(baseURL?: string): string {
  configuredBaseURL = process.env.BASE_URL || baseURL || 'http://10.236.20.11:8091/';
  
  if (!configuredBaseURL.endsWith('/')) {
    configuredBaseURL += '/';
  }
  
  return configuredBaseURL;
}
