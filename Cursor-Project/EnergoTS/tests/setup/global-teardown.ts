import { test as teardown} from '@playwright/test';
import reportGenerator from '../../utils/generateReport';

teardown('send report to slack', async () => {
  teardown.setTimeout(3600000); 
  console.log('Running global teardown...');

  const report = new reportGenerator();
  const reportFile = await report.readReport();

  // Check if setup failed by looking at the report
  let setupFailed = false;
  if (reportFile && reportFile.suites) {
    // Look through all suites to find setup-related tests
    for (const suite of reportFile.suites) {
      // Check if this is the global-setup file (direct file property)
      if (suite.file?.includes('global-setup.ts') && suite.specs) {
        console.log(`Found setup suite: ${suite.file}`);
        
        // Check all specs for failures or timeouts
        for (const spec of suite.specs) {
          if (spec.tests) {
            for (const test of spec.tests) {
              if (test.results) {
                for (const result of test.results) {
                  console.log(`Setup test status: ${result.status}`);
                  if (result.status === 'timedOut' || result.status === 'failed') {
                    setupFailed = true;
                    break;
                  }
                }
              }
              if (setupFailed) break;
            }
          }
          if (setupFailed) break;
        }
      }
      if (setupFailed) break;
    }
    
    if (setupFailed) {
      console.error('❌ Global setup failed or timed out');
      console.error('Environment is unreachable or setup exceeded timeout');
      
      // Send setup failure notification
      await report.sendReportToSlack(
        { 'SETUP-FAILED': 31 },
        { passed: 0, failed: 1, total: 1, setupFailed: true }
      );
      return;
    } else {
      console.log('Setup check: No setup failures detected');
    }
  }

  // If JSON report is missing or invalid, skip Jira but still try Slack HTML upload
  if (!reportFile) {
    console.warn('Report JSON is missing. Skipping Jira status updates. Attempting Slack upload if HTML exists...');
    await report.sendReportToSlack({}, { passed: 0, failed: 0, total: 0 });
    return;
  }

  const statusMap = report.buildStatusMap(reportFile);
  
  // Get actual test statistics from the report
  const testStats = report.getTestStats(reportFile.suites);
  console.log(`Test results: ${testStats.passed} passed, ${testStats.failed} failed, ${testStats.total} total`);

  await report.sendReportToSlack(statusMap, testStats);
  await report.sendReportToJira(statusMap);
})