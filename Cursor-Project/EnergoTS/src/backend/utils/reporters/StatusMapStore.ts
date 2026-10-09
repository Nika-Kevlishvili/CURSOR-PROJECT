import * as fs from 'fs';
import * as os from 'os';
import * as path from 'path';

type RegressionEnvKey = 'dev' | 'dev2' | 'test';

export interface EnvStatusMap {
    environment: RegressionEnvKey;
    timestamp: string;
    statusMap: Record<string, 31 | 2>;
}

export interface MergedTicket {
    failedOnEnvs: RegressionEnvKey[];
    status: 31 | 2;
}

export type MergedStatusMap = Record<string, MergedTicket>;

const STATUS_MAPS_DIR = path.join(os.homedir(), 'merged-reports', 'status-maps');
const MAX_AGE_MS = 12 * 60 * 60 * 1000;

const ENV_JIRA_OPTIONS: Record<RegressionEnvKey, { label: string; optionId: string }> = {
    dev:  { label: 'DEV',  optionId: '10895' },
    dev2: { label: 'DEV2', optionId: '10896' },
    test: { label: 'TEST', optionId: '10897' },
};

export class StatusMapStore {

    static get statusMapsDir(): string {
        return STATUS_MAPS_DIR;
    }

    static get envJiraOptions(): Record<RegressionEnvKey, { label: string; optionId: string }> {
        return ENV_JIRA_OPTIONS;
    }

    static saveEnvStatusMap(env: RegressionEnvKey, statusMap: Record<string, 31 | 2>): string {
        fs.mkdirSync(STATUS_MAPS_DIR, { recursive: true });
        const filePath = path.join(STATUS_MAPS_DIR, `${env}.json`);
        const data: EnvStatusMap = {
            environment: env,
            timestamp: new Date().toISOString(),
            statusMap,
        };
        fs.writeFileSync(filePath, JSON.stringify(data, null, 2));
        console.log(`[StatusMapStore] Saved ${Object.keys(statusMap).length} entries → ${filePath}`);
        return filePath;
    }

    static loadAllEnvStatusMaps(): EnvStatusMap[] {
        if (!fs.existsSync(STATUS_MAPS_DIR)) {
            console.warn(`[StatusMapStore] Directory not found: ${STATUS_MAPS_DIR}`);
            return [];
        }

        const files = fs.readdirSync(STATUS_MAPS_DIR).filter(f => f.endsWith('.json'));
        const maps: EnvStatusMap[] = [];

        for (const file of files) {
            const filePath = path.join(STATUS_MAPS_DIR, file);
            try {
                const data: EnvStatusMap = JSON.parse(fs.readFileSync(filePath, 'utf-8'));
                const ageMs = Date.now() - new Date(data.timestamp).getTime();

                if (ageMs > MAX_AGE_MS) {
                    console.warn(
                        `[StatusMapStore] Skipping stale ${file} ` +
                        `(${(ageMs / 3600000).toFixed(1)}h old, max ${MAX_AGE_MS / 3600000}h)`
                    );
                    continue;
                }

                console.log(
                    `[StatusMapStore] Loaded ${file}: ${Object.keys(data.statusMap).length} tickets, ` +
                    `${(ageMs / 3600000).toFixed(1)}h old`
                );
                maps.push(data);
            } catch (e) {
                console.warn(`[StatusMapStore] Failed to read ${file}:`, e);
            }
        }

        return maps;
    }

    static mergeStatusMaps(envMaps: EnvStatusMap[]): MergedStatusMap {
        const merged: MergedStatusMap = {};

        for (const envMap of envMaps) {
            for (const [ticketId, status] of Object.entries(envMap.statusMap)) {
                if (!merged[ticketId]) {
                    merged[ticketId] = { failedOnEnvs: [], status: 31 };
                }

                if (status === 2) {
                    if (!merged[ticketId].failedOnEnvs.includes(envMap.environment)) {
                        merged[ticketId].failedOnEnvs.push(envMap.environment);
                    }
                }
            }
        }

        for (const ticket of Object.values(merged)) {
            ticket.status = ticket.failedOnEnvs.length > 0 ? 2 : 31;
        }

        return merged;
    }

    static clearStatusMaps(): void {
        if (!fs.existsSync(STATUS_MAPS_DIR)) return;
        const files = fs.readdirSync(STATUS_MAPS_DIR).filter(f => f.endsWith('.json'));
        for (const file of files) {
            fs.unlinkSync(path.join(STATUS_MAPS_DIR, file));
        }
        console.log(`[StatusMapStore] Cleared ${files.length} status map file(s)`);
    }

    static saveMergedLog(merged: MergedStatusMap, envMaps: EnvStatusMap[]): void {
        const logPath = path.join(os.homedir(), 'merged-reports', 'jira-update-log.json');
        const log = {
            processedAt: new Date().toISOString(),
            environments: envMaps.map(m => ({ env: m.environment, timestamp: m.timestamp })),
            totalTickets: Object.keys(merged).length,
            passed: Object.values(merged).filter(t => t.status === 31).length,
            failed: Object.values(merged).filter(t => t.status === 2).length,
            tickets: merged,
        };
        fs.writeFileSync(logPath, JSON.stringify(log, null, 2));
        console.log(`[StatusMapStore] Saved update log → ${logPath}`);
    }
}
