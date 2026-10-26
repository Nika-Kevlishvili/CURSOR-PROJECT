import fs from 'node:fs';
import { DatabaseSync, type SqlValue } from 'node:sqlite';
import type { AppConfig } from './config.js';

export class AppDb {
  constructor(readonly raw: DatabaseSync) {}

  exec(sql: string): void {
    this.raw.exec(sql);
  }

  run(sql: string, ...params: SqlValue[]): void {
    this.raw.prepare(sql).run(...params);
  }

  get<T>(sql: string, ...params: SqlValue[]): T | undefined {
    return this.raw.prepare(sql).get(...params) as T | undefined;
  }

  all<T>(sql: string, ...params: SqlValue[]): T[] {
    return this.raw.prepare(sql).all(...params) as T[];
  }

  transaction<T>(fn: () => T): T {
    this.exec('BEGIN IMMEDIATE');
    try {
      const result = fn();
      this.exec('COMMIT');
      return result;
    } catch (error) {
      try {
        this.exec('ROLLBACK');
      } catch {
        // The original error is the one the caller needs.
      }
      throw error;
    }
  }

  close(): void {
    this.raw.close();
  }
}

const SCHEMA = `
CREATE TABLE IF NOT EXISTS meta (
  key TEXT PRIMARY KEY,
  value TEXT NOT NULL
);
CREATE TABLE IF NOT EXISTS reports (
  id TEXT PRIMARY KEY,
  content_hash TEXT NOT NULL UNIQUE,
  environment TEXT NOT NULL,
  label TEXT,
  executed_at TEXT,
  imported_at TEXT NOT NULL,
  source_filename TEXT NOT NULL,
  parser TEXT NOT NULL,
  playwright_version TEXT,
  archive_bytes INTEGER NOT NULL,
  test_count INTEGER NOT NULL,
  failed_count INTEGER NOT NULL,
  storage_dir TEXT,
  attachments_available INTEGER NOT NULL DEFAULT 1,
  warnings_json TEXT NOT NULL,
  report_path TEXT
);
CREATE TABLE IF NOT EXISTS test_cases (
  id TEXT PRIMARY KEY,
  file_path TEXT,
  full_title TEXT NOT NULL,
  title TEXT NOT NULL,
  project_name TEXT,
  tags_json TEXT NOT NULL,
  classification TEXT NOT NULL,
  confidence TEXT NOT NULL,
  primary_cause TEXT,
  causes_json TEXT NOT NULL,
  secondary_json TEXT NOT NULL,
  regressed INTEGER NOT NULL DEFAULT 0,
  updated_at TEXT NOT NULL
);
CREATE TABLE IF NOT EXISTS results (
  id TEXT PRIMARY KEY,
  report_id TEXT NOT NULL,
  test_id TEXT NOT NULL,
  line INTEGER,
  column_no INTEGER,
  outcome TEXT,
  expected_status TEXT,
  status TEXT NOT NULL,
  duration_ms INTEGER,
  error_summary TEXT,
  raw_error TEXT,
  signature TEXT,
  selector TEXT,
  endpoint TEXT,
  source_ref TEXT,
  regressed INTEGER NOT NULL DEFAULT 0,
  UNIQUE (report_id, test_id),
  FOREIGN KEY (report_id) REFERENCES reports(id),
  FOREIGN KEY (test_id) REFERENCES test_cases(id)
);
CREATE TABLE IF NOT EXISTS attempts (
  id TEXT PRIMARY KEY,
  result_id TEXT NOT NULL,
  retry_index INTEGER,
  status TEXT NOT NULL,
  duration_ms INTEGER,
  start_time TEXT,
  worker_index INTEGER,
  error_message TEXT,
  error_stack TEXT,
  attachments_json TEXT NOT NULL,
  FOREIGN KEY (result_id) REFERENCES results(id)
);
CREATE TABLE IF NOT EXISTS assessments (
  test_id TEXT PRIMARY KEY,
  classification TEXT NOT NULL,
  confidence TEXT NOT NULL,
  categories_json TEXT NOT NULL,
  evidence_json TEXT NOT NULL,
  missing_json TEXT NOT NULL,
  contradicts_json TEXT NOT NULL,
  source_files_json TEXT NOT NULL,
  next_action TEXT NOT NULL,
  excerpt TEXT,
  frequency_json TEXT NOT NULL,
  recovery_json TEXT NOT NULL,
  updated_at TEXT NOT NULL,
  FOREIGN KEY (test_id) REFERENCES test_cases(id)
);
CREATE TABLE IF NOT EXISTS issues (
  id TEXT PRIMARY KEY,
  signature TEXT NOT NULL,
  file_path TEXT,
  primary_cause TEXT NOT NULL,
  classification TEXT NOT NULL,
  summary TEXT NOT NULL,
  confidence TEXT NOT NULL,
  updated_at TEXT NOT NULL
);
CREATE TABLE IF NOT EXISTS issue_tests (
  issue_id TEXT NOT NULL,
  test_id TEXT NOT NULL,
  PRIMARY KEY (issue_id, test_id)
);
CREATE TABLE IF NOT EXISTS proposals (
  id TEXT PRIMARY KEY,
  issue_id TEXT,
  dedupe_key TEXT NOT NULL,
  version INTEGER NOT NULL,
  kind TEXT NOT NULL,
  state TEXT NOT NULL,
  summary TEXT NOT NULL,
  rationale TEXT NOT NULL,
  impact TEXT NOT NULL,
  risks TEXT NOT NULL,
  confidence TEXT NOT NULL,
  validation_plan TEXT NOT NULL,
  why TEXT NOT NULL,
  files_json TEXT NOT NULL,
  diff_text TEXT NOT NULL,
  root_cause_json TEXT NOT NULL,
  report_ids_json TEXT NOT NULL,
  created_at TEXT NOT NULL,
  approved_at TEXT,
  applied_at TEXT,
  rejected_at TEXT,
  applied_diff TEXT,
  ai_note TEXT,
  block_reason TEXT
);
CREATE TABLE IF NOT EXISTS proposal_tests (
  proposal_id TEXT NOT NULL,
  test_id TEXT NOT NULL,
  PRIMARY KEY (proposal_id, test_id)
);
CREATE TABLE IF NOT EXISTS approvals (
  id TEXT PRIMARY KEY,
  proposal_id TEXT NOT NULL,
  action TEXT NOT NULL,
  note TEXT,
  created_at TEXT NOT NULL
);
CREATE TABLE IF NOT EXISTS file_backups (
  proposal_id TEXT NOT NULL,
  path TEXT NOT NULL,
  content TEXT NOT NULL,
  base_hash TEXT NOT NULL,
  applied_hash TEXT,
  PRIMARY KEY (proposal_id, path)
);
CREATE TABLE IF NOT EXISTS validations (
  id TEXT PRIMARY KEY,
  proposal_id TEXT NOT NULL,
  command TEXT NOT NULL,
  environment TEXT NOT NULL,
  selected_tests TEXT NOT NULL,
  started_at TEXT NOT NULL,
  ended_at TEXT,
  exit_code INTEGER,
  passed INTEGER,
  failed INTEGER,
  skipped INTEGER,
  interrupted INTEGER,
  flaky INTEGER,
  repetitions INTEGER NOT NULL,
  observed_failures INTEGER,
  report_path TEXT,
  supports_fix INTEGER,
  status TEXT NOT NULL,
  log_excerpt TEXT,
  disclaimer TEXT NOT NULL
);
CREATE INDEX IF NOT EXISTS idx_results_report ON results(report_id);
CREATE INDEX IF NOT EXISTS idx_results_test ON results(test_id);
CREATE INDEX IF NOT EXISTS idx_results_signature ON results(signature);
CREATE INDEX IF NOT EXISTS idx_proposals_state ON proposals(state);
`;

export function openDatabase(config: Pick<AppConfig, 'dbPath'>): AppDb {
  fs.mkdirSync(pathDir(config.dbPath), { recursive: true });
  const raw = new DatabaseSync(config.dbPath);
  const db = new AppDb(raw);
  db.exec('PRAGMA foreign_keys = ON');
  db.exec('PRAGMA busy_timeout = 5000');
  db.exec(SCHEMA);
  db.run(
    `INSERT INTO meta (key, value) VALUES ('schema_version', '1')
     ON CONFLICT(key) DO UPDATE SET value = excluded.value`,
  );
  return db;
}

function pathDir(file: string): string {
  const index = Math.max(file.lastIndexOf('/'), file.lastIndexOf('\\'));
  return index <= 0 ? '.' : file.slice(0, index);
}
