export interface SubMenuItem {
  name: string;
  index: number;
}

export interface MenuSection {
  name: string;
  index: number;
  subItems: SubMenuItem[];
}

export interface PhoenixMenuStructure {
  discoveredAt: string;
  environment: string;
  baseUrl: string;
  sections: MenuSection[];
}

export const MENU_STRUCTURE_PATH = 'tests/regression/config/menu-structure.json';
