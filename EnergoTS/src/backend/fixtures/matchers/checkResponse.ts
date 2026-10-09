import { test as base } from '@playwright/test';
import { ExtendedAPIResponse } from '../../utils/RequestWrapper';
import { configuredBaseURL } from '../utils/baseUrl';

/**
 * Custom Playwright expect matcher for API response validation.
 * 
 * Usage: await expect(response).CheckResponse();
 * 
 * Features:
 * - Auto-extracts [REG-XXX] test ID from test title
 * - Formats errors with endpoint, method, payload, and response details
 * - Handles response body consumption (only readable once)
 * - Works with RequestWrapper's ExtendedAPIResponse metadata
 */
export const checkResponseMatcher = {
  async CheckResponse(
    response: ExtendedAPIResponse,
    testName?: string,
    endpoint?: string,
    method?: string,
    payload?: any
  ) {
    const actualStatus = response.status();
    const pass = response.ok();

    // Auto-extract test name from test.info() if not provided
    let finalTestName = testName;
    if (!finalTestName) {
      try {
        const info = base.info();
        const titleMatch = info.title.match(/\[(REG-\d+)\]/);
        finalTestName = titleMatch ? titleMatch[1] : info.title;
      } catch {
        finalTestName = 'unknown test';
      }
    }

    // Use provided values or fall back to metadata
    const finalEndpoint = endpoint || response._requestMetadata?.endpoint || 'unknown';
    const finalMethod = method || response._requestMetadata?.method || 'unknown';
    const finalPayload = payload !== undefined ? payload : response._requestMetadata?.payload;

    if (pass) {
      return {
        pass: true,
        message: () => `✅ ${finalTestName} - ok status ${actualStatus} received`,
      };
    } else {
      const responseBody = await response.json().catch(() => response.text());
      return {
        pass: false,
        message: () => `
        ${finalTestName} ❌ Failed 
        Endpoint: ${configuredBaseURL}${finalEndpoint}
        Method: ${finalMethod}
        Payload: ${JSON.stringify(finalPayload, null, 2)}
        Status: ${response.status()}
        Response: ${JSON.stringify(responseBody, null, 2)}`,
      };
    }
  },
};
