import { test as baseTest, expect } from './baseFixture';
import type { baseFixture } from './baseFixture';

export { expect };

export function reminderId(raw: unknown): number | undefined {
  if (raw == null) return undefined;
  if (typeof raw === 'number' && Number.isFinite(raw)) return raw;
  if (typeof raw === 'object' && raw !== null && 'id' in raw) {
    const id = Number((raw as { id: number }).id);
    return Number.isFinite(id) ? id : undefined;
  }
  return undefined;
}

/**
 * DELETE for every reminder recorded in Responses.
 * Runs safely after pass or fail; treats 404 as already deleted. Does not assert.
 */
export async function deleteAllReminders(
  Request: baseFixture['Request'],
  Endpoints: baseFixture['Endpoints'],
  reminders: unknown[],
): Promise<void> {
  const seen = new Set<number>();

  for (const raw of reminders) {
    const id = reminderId(raw);
    if (id == null || seen.has(id)) continue;
    seen.add(id);

    const deletion = await Request.delete(`${Endpoints.reminder}/${id}`);
    const status = deletion.status();
    if (deletion.ok() || status === 404) continue;

    console.warn(`[reminder cleanup] DELETE reminder/${id} failed with status ${status}`);
  }
}

export const test = baseTest;

test.afterEach(async ({ Request, Responses, Endpoints }) => {
  await deleteAllReminders(Request, Endpoints, Responses.reminder);
});
