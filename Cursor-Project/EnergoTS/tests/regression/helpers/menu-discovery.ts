import { Page } from '@playwright/test';
import * as fs from 'fs';
import * as path from 'path';
import { MenuSection, PhoenixMenuStructure, MENU_STRUCTURE_PATH } from './menu-structure';
import { PhoenixNavigation } from './navigation';

export class MenuDiscovery {
  constructor(private page: Page) {}

  async ensureSidebarOpen(): Promise<void> {
    const nav = new PhoenixNavigation(this.page);
    await nav.ensureSidebarOpen();
  }

  static loadStructure(inputPath: string = MENU_STRUCTURE_PATH): PhoenixMenuStructure {
    const fullPath = path.resolve(process.cwd(), inputPath);
    return JSON.parse(fs.readFileSync(fullPath, 'utf-8')) as PhoenixMenuStructure;
  }

  async saveStructure(structure: PhoenixMenuStructure, outputPath: string = MENU_STRUCTURE_PATH): Promise<string> {
    const fullPath = path.resolve(process.cwd(), outputPath);
    fs.mkdirSync(path.dirname(fullPath), { recursive: true });
    fs.writeFileSync(fullPath, JSON.stringify(structure, null, 2));
    return fullPath;
  }

  async clickSubmenuItem(sectionName: string, subItemName: string): Promise<void> {
    const nav = new PhoenixNavigation(this.page);
    await nav.clickSubmenuItem(sectionName, subItemName);
  }

  async discoverFullStructure(environment: string, baseUrl: string): Promise<PhoenixMenuStructure> {
    return MenuDiscovery.loadStructure();
  }

  getSections(): MenuSection[] {
    return MenuDiscovery.loadStructure().sections;
  }
}
