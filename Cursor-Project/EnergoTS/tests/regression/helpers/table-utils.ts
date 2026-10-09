import { Page } from '@playwright/test';

export async function getTableRowCount(page: Page): Promise<number> {
  return page.locator('tbody tr').count();
}

export async function getTableColumnValues(
  page: Page,
  columnIndex: number,
  maxRows = 8,
): Promise<string[]> {
  return page.evaluate(
    ({ col, max }) => {
      const rows = [...document.querySelectorAll('tbody tr')].slice(0, max);
      return rows
        .map((row) => {
          const cells = row.querySelectorAll('td');
          return (cells[col]?.textContent || '').replace(/\s+/g, ' ').trim();
        })
        .filter((v) => v.length > 0);
    },
    { col: columnIndex, max: maxRows },
  );
}

export async function getTableHeaderLabels(page: Page): Promise<string[]> {
  const headers = page.locator('thead th');
  const count = await headers.count();
  const labels: string[] = [];
  for (let i = 0; i < count; i++) {
    labels.push((await headers.nth(i).innerText()).replace(/\s+/g, ' ').trim());
  }
  return labels;
}

export async function findSortColumn(
  page: Page,
  preferredPatterns: RegExp[],
): Promise<{ index: number; label: string } | null> {
  const labels = await getTableHeaderLabels(page);
  for (const pattern of preferredPatterns) {
    for (let i = 0; i < labels.length; i++) {
      const label = labels[i];
      if (!label || /^actions$/i.test(label)) continue;
      if (pattern.test(label)) return { index: i, label };
    }
  }
  for (let i = 0; i < labels.length; i++) {
    const label = labels[i];
    if (label && !/^actions$/i.test(label)) return { index: i, label };
  }
  return null;
}

export type SortAnalysis = 'asc' | 'desc' | 'unchanged' | 'too_few_rows';

export function analyzeSortOrder(values: string[]): SortAnalysis {
  if (values.length < 2) return 'too_few_rows';
  const asc = [...values].sort((a, b) => a.localeCompare(b, undefined, { numeric: true, sensitivity: 'base' }));
  const desc = [...asc].reverse();
  if (values.every((v, i) => v === asc[i])) return 'asc';
  if (values.every((v, i) => v === desc[i])) return 'desc';
  return 'unchanged';
}

export function orderChanged(before: string[], after: string[]): boolean {
  if (before.length !== after.length) return true;
  return before.some((v, i) => v !== after[i]);
}

export function normalizeCellText(value: string): string {
  return value.replace(/\s+/g, ' ').trim().toLowerCase();
}

/** Map filter dropdown label to a table column header index. */
export function findColumnForFilter(
  filterLabel: string,
  headerLabels: string[],
): { index: number; label: string } | null {
  const pairs: Array<{ filter: RegExp; column: RegExp }> = [
    { filter: /^status$/i, column: /^status$/i },
    { filter: /^type$/i, column: /^type$/i },
    { filter: /type/i, column: /type of connection/i },
    { filter: /account manager/i, column: /account manager/i },
    { filter: /economic branch/i, column: /economic branch/i },
    { filter: /unwanted/i, column: /unwanted/i },
    { filter: /^reason$/i, column: /^reason$/i },
    { filter: /number of customer/i, column: /number of connected/i },
    { filter: /populated place/i, column: /populated place/i },
  ];

  for (const { filter, column } of pairs) {
    if (!filter.test(filterLabel)) continue;
    for (let i = 0; i < headerLabels.length; i++) {
      const h = headerLabels[i];
      if (h && column.test(h)) return { index: i, label: h };
    }
  }

  const filterWords = normalizeCellText(filterLabel).split(/\s+/).filter((w) => w.length > 2);
  let best: { index: number; label: string; score: number } | null = null;
  for (let i = 0; i < headerLabels.length; i++) {
    const h = headerLabels[i];
    if (!h || /^actions$/i.test(h)) continue;
    const headerNorm = normalizeCellText(h);
    const score = filterWords.filter((w) => headerNorm.includes(w)).length;
    if (score > 0 && (!best || score > best.score)) {
      best = { index: i, label: h, score };
    }
  }
  return best ? { index: best.index, label: best.label } : null;
}

export interface ContentMatchResult {
  matched: number;
  total: number;
  mismatches: string[];
  verified: boolean;
}

/** Cell value matches selected filter option (exact, contains, or YES/NO style). */
export function verifyValuesMatchFilter(
  cellValues: string[],
  filterOption: string,
): ContentMatchResult {
  if (cellValues.length === 0) {
    return { matched: 0, total: 0, mismatches: [], verified: true };
  }

  const option = normalizeCellText(filterOption);
  const mismatches: string[] = [];
  let matched = 0;

  for (const raw of cellValues) {
    const cell = normalizeCellText(raw);
    const ok =
      cell === option ||
      cell.includes(option) ||
      option.includes(cell) ||
      (option === 'yes' && cell === 'yes') ||
      (option === 'no' && cell === 'no');

    if (ok) matched++;
    else mismatches.push(raw);
  }

  return {
    matched,
    total: cellValues.length,
    mismatches: mismatches.slice(0, 5),
    verified: matched === cellValues.length,
  };
}

export function verifyRowsContainSearchToken(
  rowTexts: string[],
  token: string,
): ContentMatchResult {
  if (rowTexts.length === 0) {
    return { matched: 0, total: 0, mismatches: [], verified: true };
  }

  const needle = normalizeCellText(token);
  const mismatches: string[] = [];
  let matched = 0;

  for (const raw of rowTexts) {
    const row = normalizeCellText(raw);
    if (row.includes(needle)) matched++;
    else mismatches.push(raw.slice(0, 80));
  }

  return {
    matched,
    total: rowTexts.length,
    mismatches: mismatches.slice(0, 3),
    verified: matched === rowTexts.length,
  };
}

export async function getAllVisibleRowTexts(page: Page, maxRows = 25): Promise<string[]> {
  return page.evaluate((max) => {
    return [...document.querySelectorAll('tbody tr')]
      .slice(0, max)
      .map((row) => (row.textContent || '').replace(/\s+/g, ' ').trim())
      .filter((t) => t.length > 0);
  }, maxRows);
}
