import * as path from 'path';

export type ScreenshotKind = 'listing' | 'object';

export function slugMenuName(value: string): string {
  return value.toLowerCase().replace(/[^a-z0-9]+/g, '-').replace(/^-|-$/g, '');
}

export function newScreenshotRunId(): string {
  return new Date().toISOString().replace(/[:.]/g, '-').slice(0, 19);
}

export interface MenuScreenshotPathInput {
  screenshotsRoot: string;
  runId: string;
  sectionIndex: number;
  sectionName: string;
  subItemIndex: number;
  subItemName: string;
}

export function buildMenuScreenshotDirs(input: MenuScreenshotPathInput): {
  baseDir: string;
  listingDir: string;
  objectDir: string;
  sectionDir: string;
  subItemDir: string;
} {
  const sectionDir =
    input.sectionIndex < 0
      ? '00-session'
      : `${String(input.sectionIndex + 1).padStart(2, '0')}-${slugMenuName(input.sectionName)}`;
  const subItemDir = `${String(input.subItemIndex + 1).padStart(2, '0')}-${slugMenuName(input.subItemName)}`;
  const baseDir = path.join(input.screenshotsRoot, input.runId, sectionDir, subItemDir);

  return {
    baseDir,
    listingDir: path.join(baseDir, 'listing'),
    objectDir: path.join(baseDir, 'object'),
    sectionDir,
    subItemDir,
  };
}

export function dirForKind(
  dirs: Pick<ReturnType<typeof buildMenuScreenshotDirs>, 'listingDir' | 'objectDir'>,
  kind: ScreenshotKind,
): string {
  return kind === 'listing' ? dirs.listingDir : dirs.objectDir;
}
