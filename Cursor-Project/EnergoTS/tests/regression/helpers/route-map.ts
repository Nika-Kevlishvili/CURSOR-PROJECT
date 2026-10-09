import * as fs from 'fs';
import * as path from 'path';

export interface RouteEntry {
  section: string;
  subItem: string;
  path: string;
  method: string;
  apiHint?: string;
}

export interface RouteMapFile {
  baseUrl: string;
  discoveredAt?: string;
  routes: Record<string, RouteEntry>;
}

export const ROUTE_MAP_PATH = path.resolve(__dirname, '../config/route-map.json');

export function routeKey(section: string, subItem: string): string {
  return `${section}::${subItem}`;
}

export function toSlug(name: string): string {
  return name.toLowerCase().replace(/[^a-z0-9]+/g, '-').replace(/^-|-$/g, '');
}

export function loadRouteMap(filePath: string = ROUTE_MAP_PATH): RouteMapFile {
  return JSON.parse(fs.readFileSync(filePath, 'utf-8')) as RouteMapFile;
}

export function saveRouteEntry(
  section: string,
  subItem: string,
  entry: Partial<RouteEntry> & { path: string; method: string },
  filePath: string = ROUTE_MAP_PATH,
): void {
  const map = loadRouteMap(filePath);
  const key = routeKey(section, subItem);
  map.routes[key] = {
    section,
    subItem,
    path: entry.path,
    method: entry.method,
    apiHint: entry.apiHint,
  };
  map.discoveredAt = new Date().toISOString();
  fs.writeFileSync(filePath, JSON.stringify(map, null, 2));
}

export function getKnownPath(section: string, subItem: string, map: RouteMapFile): string | null {
  const entry = map.routes[routeKey(section, subItem)];
  return entry?.path ?? null;
}

export function slugPathCandidates(subItem: string): string[] {
  const slug = toSlug(subItem);
  const singular = slug.endsWith('s') ? slug.slice(0, -1) : slug;
  return [...new Set([`/${slug}`, `/${singular}`])];
}
