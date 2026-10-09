import * as fs from 'fs';
import * as path from 'path';

const envVariablesPath = path.resolve(__dirname, '../fixtures/envVariables.json');

// Handle case where envVariables.json doesn't exist (e.g., when setup fails)
let envVariables: any = {};
if (fs.existsSync(envVariablesPath)) {
  envVariables = JSON.parse(fs.readFileSync(envVariablesPath, 'utf-8'));
} else {
  console.warn('envVariables.json not found - setup may have failed or not run yet');
}

export { envVariables };