import fs from 'fs';
import path from 'path';

/**
 * Reads the authentication token from the token.json file.
 * This token is generated during global setup and used for all API requests.
 * 
 * @returns The JWT Bearer token string
 */
export function getToken(): string {
  const tokenPath = path.resolve(__dirname, '../token.json');
  const tokenData = JSON.parse(fs.readFileSync(tokenPath, 'utf-8'));
  return tokenData.token;
}

export function getSPToken(): string {
  const tokenPath = path.resolve(__dirname, '../salesPortalToken.json');
  const tokenData = JSON.parse(fs.readFileSync(tokenPath, 'utf-8'));
  return tokenData;
}