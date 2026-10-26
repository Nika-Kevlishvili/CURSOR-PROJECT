import { createAdvisor } from './ai/advisor.js';
import { createApp } from './app.js';
import { assertLoopbackHost, ensureDataDirs, loadConfig } from './config.js';
import { openDatabase } from './db.js';
import { applyRetention } from './services/importService.js';

const config = loadConfig();
assertLoopbackHost(config.host);
ensureDataDirs(config);
const db = openDatabase(config);
const removed = applyRetention({ config, db, advisor: { enabled: false, async explain() { return null; } } });
if (removed > 0) {
  console.log(`Retention removed attachment files for ${removed} imported report(s). Result history was kept.`);
}
if (process.env.FLAKY_ANALYZER_AI_ENABLED === 'true' && !(process.env.FLAKY_ANALYZER_AI_BASE_URL && process.env.FLAKY_ANALYZER_AI_API_KEY && process.env.FLAKY_ANALYZER_AI_MODEL)) {
  console.log('AI notes are off. Set FLAKY_ANALYZER_AI_BASE_URL, FLAKY_ANALYZER_AI_API_KEY, and FLAKY_ANALYZER_AI_MODEL together. Report text is not sent.');
}
const advisor = createAdvisor(config);
const app = createApp({ config, db, advisor });
app.listen(config.port, config.host, () => {
  console.log(`Flaky Test Analyzer listening at http://${config.host}:${config.port}`);
});
