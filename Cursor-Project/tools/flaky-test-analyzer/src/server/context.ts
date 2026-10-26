import type { AnalysisAdvisor } from './ai/advisor.js';
import type { AppConfig } from './config.js';
import type { AppDb } from './db.js';

export type CommandResult = {
  exitCode: number | null;
  stdout: string;
  stderr: string;
  timedOut: boolean;
  spawnError: string | null;
};

export type CommandRunner = (
  command: string,
  args: string[],
  options: { cwd: string; env: NodeJS.ProcessEnv; timeoutMs: number },
) => Promise<CommandResult>;

export type AppContext = {
  config: AppConfig;
  db: AppDb;
  advisor: AnalysisAdvisor;
  runner?: CommandRunner;
};
