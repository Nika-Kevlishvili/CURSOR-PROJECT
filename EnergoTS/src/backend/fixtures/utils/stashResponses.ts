import fs from 'fs';
import path from 'path';
import { ResponsesContainer } from '../types/responses';

/**
 * Writes the ResponsesContainer data to a JSON file in the fixtures folder.
 * If the file already exists, appends the new test case data as a separate object in an array.
 * Only stores keys with non-empty arrays to keep the file clean.
 * File structure: [{ test case 1 data }, { test case 2 data }, ...]
 * 
 * @param data - The ResponsesContainer object to save
 * @param fileName - Output file name (default: 'responses.json')
 * @param testCaseId - Optional stable identifier (e.g. 'REG-978') stored alongside the
 *                     entry so tests can look it up by id instead of array position
 */
export function saveResponsesToFile(data: ResponsesContainer, fileName: string, testCaseId?: string): void {
  const filePath = path.resolve(__dirname, '../stashedResponses', fileName.endsWith('.json') ? fileName : `${fileName}.json`);
  
  let testCasesData: any[] = [];
  
  // Check if file already exists
  if (fs.existsSync(filePath)) {
    try {
      // Read existing data
      const existingContent = fs.readFileSync(filePath, 'utf-8');
      const existingData = JSON.parse(existingContent);
      
      // Handle both old format (single object) and new format (array)
      if (Array.isArray(existingData)) {
        testCasesData = existingData;
      } else {
        // Convert old format to new format
        testCasesData = [existingData];
      }
    } catch (error) {
      console.warn(`Warning: Could not read existing file ${filePath}. Creating new file.`, error);
    }
  }
  
  // Filter out empty arrays from the new data
  const filteredData: any = {};
  for (const key of Object.keys(data) as Array<keyof ResponsesContainer>) {
    const value = data[key];
    if (Array.isArray(value) && value.length > 0) {
      filteredData[key] = value;
    }
  }
  
  // Tag the entry with a stable id so it can be looked up by id instead of position
  if (testCaseId) {
    filteredData.testCaseId = testCaseId;
  }

  // Append new test case data (only non-empty arrays)
  testCasesData.push(filteredData);
  
  const dir = path.dirname(filePath);
  if (!fs.existsSync(dir)) {
    fs.mkdirSync(dir, { recursive: true });
  }

  fs.writeFileSync(filePath, JSON.stringify(testCasesData, null, 2), 'utf-8');
}

/**
 * Reads a responses JSON file from the fixtures/stashedResponses folder and returns it as a JS object.
 * Throws an error if the file doesn't exist.
 * 
 * @param fileName - File name to load (default: 'responses.json')
 * @returns The ResponsesContainer object or array of ResponsesContainer objects from the file
 */
export function loadResponsesFromFile(fileName: string = 'responses.json'): any {
  const filePath = path.resolve(__dirname, '../stashedResponses', fileName.endsWith('.json') ? fileName : `${fileName}.json`);
  
  if (!fs.existsSync(filePath)) {
    const dir = path.dirname(filePath);
    if (!fs.existsSync(dir)) {
      fs.mkdirSync(dir, { recursive: true });
    }
    fs.writeFileSync(filePath, '[]', 'utf-8');
    return [];
  }
  
  const fileContent = fs.readFileSync(filePath, 'utf-8');
  return JSON.parse(fileContent);
}

/**
 * Loads a stashed responses file and returns the entry tagged with the given testCaseId.
 * This decouples tests from the positional order of entries in the file, so a missing or
 * failed prep for one case can no longer shift the indices of the others.
 *
 * @param fileName - File name to load
 * @param testCaseId - The stable id the entry was saved with (e.g. 'REG-978')
 * @returns The matching entry, or an empty object if no entry with that id exists
 */
export function loadResponseById(fileName: string, testCaseId: string): any {
  const data = loadResponsesFromFile(fileName);
  const entries = Array.isArray(data) ? data : [data];
  return entries.find((entry) => entry && entry.testCaseId === testCaseId) ?? {};
}

export function updateFileWithInvoiceIds(fileName: string, invoicesByTestCase: Map<number, number[]>): void {
  const filePath = path.resolve(__dirname, '../stashedResponses', fileName.endsWith('.json') ? fileName : `${fileName}.json`);

  if (!fs.existsSync(filePath)) {
    console.warn(`Warning: File ${filePath} does not exist, cannot update invoice IDs.`);
    return;
  }

  const data: any[] = JSON.parse(fs.readFileSync(filePath, 'utf-8'));

  for (const [testCaseIndex, invoiceIds] of invoicesByTestCase) {
    if (testCaseIndex < data.length) {
      if (!data[testCaseIndex].invoice) {
        data[testCaseIndex].invoice = [];
      }
      data[testCaseIndex].invoice.push(...invoiceIds);
    }
  }

  fs.writeFileSync(filePath, JSON.stringify(data, null, 2), 'utf-8');
}

export function clearStashedResponses(fileName: string): void {
  const filePath = path.resolve(__dirname, '../stashedResponses', fileName.endsWith('.json') ? fileName : `${fileName}.json`);
  
  if (fs.existsSync(filePath)) {
    fs.writeFileSync(filePath, '[]', 'utf-8'); // Clear the file by writing an empty array
  }
}
