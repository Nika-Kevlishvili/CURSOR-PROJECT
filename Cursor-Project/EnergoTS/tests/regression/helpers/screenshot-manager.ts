import { Page } from '@playwright/test';
import * as fs from 'fs';
import * as path from 'path';
import {
  buildMenuScreenshotDirs,
  dirForKind,
  newScreenshotRunId,
  type ScreenshotKind,
} from './screenshot-paths';

export interface ScreenshotInfo {
  filename: string;
  relativePath: string;
  kind: ScreenshotKind;
  stepName: string;
  description: string;
  checkItems: string[];
  timestamp: string;
  scrollPosition?: string;
  confluenceRef?: string;
}

export interface ScreenshotMenuContext {
  runId?: string;
  sectionIndex: number;
  sectionName: string;
  subItemIndex: number;
  subItemName: string;
}

export interface ManifestData {
  runId: string;
  environment: string;
  baseUrl: string;
  section: string;
  subItem: string;
  sectionIndex: number;
  subItemIndex: number;
  startTime: string;
  listing: ScreenshotInfo[];
  object: ScreenshotInfo[];
  /** Flat list (listing + object) for backward-compatible tooling */
  screenshots: ScreenshotInfo[];
}

export class ScreenshotManager {
  private readonly baseDir: string;
  private readonly listingDir: string;
  private readonly objectDir: string;
  private readonly listingScreenshots: ScreenshotInfo[] = [];
  private readonly objectScreenshots: ScreenshotInfo[] = [];
  private readonly runId: string;
  private readonly context: ScreenshotMenuContext;
  private readonly counters: Record<ScreenshotKind, number> = { listing: 0, object: 0 };

  constructor(
    private page: Page,
    context: ScreenshotMenuContext,
  ) {
    this.context = context;
    this.runId = context.runId ?? newScreenshotRunId();

    const screenshotsRoot = path.resolve(__dirname, '../screenshots');
    const dirs = buildMenuScreenshotDirs({
      screenshotsRoot,
      runId: this.runId,
      sectionIndex: context.sectionIndex,
      sectionName: context.sectionName,
      subItemIndex: context.subItemIndex,
      subItemName: context.subItemName,
    });

    this.baseDir = dirs.baseDir;
    this.listingDir = dirs.listingDir;
    this.objectDir = dirs.objectDir;
    fs.mkdirSync(this.listingDir, { recursive: true });
    fs.mkdirSync(this.objectDir, { recursive: true });
  }

  private getNextFilename(kind: ScreenshotKind, stepName: string, suffix: string): string {
    this.counters[kind]++;
    const paddedCounter = String(this.counters[kind]).padStart(2, '0');
    return `${paddedCounter}-${stepName}${suffix}.png`;
  }

  private pushScreenshot(kind: ScreenshotKind, info: Omit<ScreenshotInfo, 'kind' | 'relativePath'>): void {
    const relativePath = path.join(kind, info.filename).replace(/\\/g, '/');
    const entry: ScreenshotInfo = { ...info, kind, relativePath };
    if (kind === 'listing') {
      this.listingScreenshots.push(entry);
    } else {
      this.objectScreenshots.push(entry);
    }
  }

  private targetDir(kind: ScreenshotKind): string {
    return dirForKind({ listingDir: this.listingDir, objectDir: this.objectDir }, kind);
  }

  async captureFullPage(
    kind: ScreenshotKind,
    stepName: string,
    description: string,
    checkItems: string[],
    confluenceRef?: string,
  ): Promise<void> {
    await this.page.waitForTimeout(500);

    const filename = this.getNextFilename(kind, stepName, '-full');
    await this.page.screenshot({
      path: path.join(this.targetDir(kind), filename),
      fullPage: true,
    });

    this.pushScreenshot(kind, {
      filename,
      stepName,
      description,
      checkItems,
      timestamp: new Date().toISOString(),
      scrollPosition: 'full',
      confluenceRef,
    });
  }

  async captureViewport(
    kind: ScreenshotKind,
    stepName: string,
    description: string,
    checkItems: string[],
    confluenceRef?: string,
  ): Promise<void> {
    await this.page.waitForTimeout(300);

    const filename = this.getNextFilename(kind, stepName, '-viewport');
    await this.page.screenshot({
      path: path.join(this.targetDir(kind), filename),
      fullPage: false,
    });

    this.pushScreenshot(kind, {
      filename,
      stepName,
      description,
      checkItems,
      timestamp: new Date().toISOString(),
      scrollPosition: 'viewport',
      confluenceRef,
    });
  }

  async captureWithHorizontalScroll(
    kind: ScreenshotKind,
    stepName: string,
    description: string,
    checkItems: string[],
    scrollContainerSelector?: string,
  ): Promise<void> {
    await this.page.waitForTimeout(300);

    if (scrollContainerSelector) {
      await this.page.evaluate((sel: string) => {
        const el = document.querySelector(sel);
        if (el) el.scrollLeft = 0;
      }, scrollContainerSelector);
    } else {
      await this.page.evaluate(() => window.scrollTo(0, 0));
    }
    await this.page.waitForTimeout(300);

    const leftFilename = this.getNextFilename(kind, stepName, '-left');
    await this.page.screenshot({ path: path.join(this.targetDir(kind), leftFilename) });

    if (scrollContainerSelector) {
      await this.page.evaluate((sel: string) => {
        const el = document.querySelector(sel);
        if (el) el.scrollLeft = el.scrollWidth;
      }, scrollContainerSelector);
    } else {
      await this.page.evaluate(() => window.scrollTo(document.body.scrollWidth, 0));
    }
    await this.page.waitForTimeout(300);

    const rightFilename = this.getNextFilename(kind, stepName, '-right');
    await this.page.screenshot({ path: path.join(this.targetDir(kind), rightFilename) });

    this.pushScreenshot(kind, {
      filename: leftFilename,
      stepName: `${stepName} (left)`,
      description,
      checkItems,
      timestamp: new Date().toISOString(),
      scrollPosition: 'left',
    });
    this.pushScreenshot(kind, {
      filename: rightFilename,
      stepName: `${stepName} (right)`,
      description,
      checkItems: [],
      timestamp: new Date().toISOString(),
      scrollPosition: 'right',
    });
  }

  async captureSection(
    kind: ScreenshotKind,
    stepName: string,
    sectionSelector: string,
    description: string,
    checkItems: string[],
    confluenceRef?: string,
  ): Promise<void> {
    const section = this.page.locator(sectionSelector);

    try {
      await section.scrollIntoViewIfNeeded();
      await this.page.waitForTimeout(300);

      const filename = this.getNextFilename(kind, stepName, '-section');
      await section.screenshot({ path: path.join(this.targetDir(kind), filename) });

      this.pushScreenshot(kind, {
        filename,
        stepName,
        description,
        checkItems,
        timestamp: new Date().toISOString(),
        scrollPosition: 'section',
        confluenceRef,
      });
    } catch (error) {
      console.warn(`Could not capture section ${sectionSelector}: ${error}`);
      await this.captureViewport(kind, stepName, `${description} (fallback viewport)`, checkItems, confluenceRef);
    }
  }

  async captureVerticalSections(
    kind: ScreenshotKind,
    stepName: string,
    description: string,
    checkItems: string[],
  ): Promise<void> {
    await this.page.evaluate(() => window.scrollTo(0, 0));
    await this.page.waitForTimeout(300);
    const topFile = this.getNextFilename(kind, stepName, '-top');
    await this.page.screenshot({ path: path.join(this.targetDir(kind), topFile) });

    await this.page.evaluate(() => window.scrollTo(0, document.body.scrollHeight / 2));
    await this.page.waitForTimeout(300);
    const midFile = this.getNextFilename(kind, stepName, '-middle');
    await this.page.screenshot({ path: path.join(this.targetDir(kind), midFile) });

    await this.page.evaluate(() => window.scrollTo(0, document.body.scrollHeight));
    await this.page.waitForTimeout(300);
    const bottomFile = this.getNextFilename(kind, stepName, '-bottom');
    await this.page.screenshot({ path: path.join(this.targetDir(kind), bottomFile) });

    this.pushScreenshot(kind, {
      filename: topFile,
      stepName: `${stepName} (top)`,
      description,
      checkItems,
      timestamp: new Date().toISOString(),
      scrollPosition: 'top',
    });
    this.pushScreenshot(kind, {
      filename: midFile,
      stepName: `${stepName} (middle)`,
      description,
      checkItems: [],
      timestamp: new Date().toISOString(),
      scrollPosition: 'middle',
    });
    this.pushScreenshot(kind, {
      filename: bottomFile,
      stepName: `${stepName} (bottom)`,
      description,
      checkItems: [],
      timestamp: new Date().toISOString(),
      scrollPosition: 'bottom',
    });
  }

  async captureAllTabs(
    kind: ScreenshotKind,
    stepName: string,
    tabNames: string[],
    description: string,
    checkItemsPerTab: Record<string, string[]>,
  ): Promise<void> {
    for (const tabName of tabNames) {
      const tab = this.page
        .getByRole('tab', { name: tabName })
        .or(this.page.locator(`[role="tab"]:has-text("${tabName}")`));

      try {
        await tab.first().click();
        await this.page.waitForLoadState('networkidle');
        await this.page.waitForTimeout(500);

        const tabStepName = `${stepName}-${tabName.toLowerCase().replace(/\s+/g, '-')}`;
        const tabCheckItems = checkItemsPerTab[tabName] || [];

        await this.captureViewport(kind, tabStepName, `${description} - ${tabName} tab`, tabCheckItems);
      } catch (error) {
        console.warn(`Could not capture tab ${tabName}: ${error}`);
      }
    }
  }

  async generateManifest(environment: string, baseUrl: string): Promise<string> {
    const manifest: ManifestData = {
      runId: this.runId,
      environment,
      baseUrl,
      section: this.context.sectionName,
      subItem: this.context.subItemName,
      sectionIndex: this.context.sectionIndex,
      subItemIndex: this.context.subItemIndex,
      startTime: new Date().toISOString(),
      listing: this.listingScreenshots,
      object: this.objectScreenshots,
      screenshots: [...this.listingScreenshots, ...this.objectScreenshots],
    };

    const manifestPath = path.join(this.baseDir, 'manifest.json');
    fs.writeFileSync(manifestPath, JSON.stringify(manifest, null, 2));

    console.log(`\n📸 Screenshots saved to: ${this.baseDir}`);
    console.log(`   listing/ → ${this.listingScreenshots.length} file(s)`);
    console.log(`   object/  → ${this.objectScreenshots.length} file(s)`);
    console.log(`📋 Manifest: ${manifestPath}`);

    return manifestPath;
  }

  getScreenshots(): ScreenshotInfo[] {
    return [...this.listingScreenshots, ...this.objectScreenshots];
  }

  getScreenshotDir(): string {
    return this.baseDir;
  }

  getRunId(): string {
    return this.runId;
  }
}
