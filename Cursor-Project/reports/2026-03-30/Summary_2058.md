## Task
Validate subagent and rule link integrity, and run virtual smoke tests.

## What was checked
- Enumerated rule files under `.cursor/rules/`.
- Enumerated subagent files under `.cursor/agents/`.
- Enumerated skill files under `.cursor/skills/`.
- Verified referenced command/template/document paths exist (including `.cursor/commands/hands-off.md`, `.cursor/commands/energo-ts-run.md`, `Cursor-Project/config/template/Test_case_template.md`, `Cursor-Project/config/template/Slack_report_template.md`, and related docs).
- Ran subagent smoke tests in readonly mode.

## Virtual test results
- `explore` subagent: PASS (`OK-explore`)
- `cross-dependency-finder` subagent: PASS (`OK-cross-dependency-finder`)
- `shell` subagent: FAIL with internal error: `Required tool READ not found in allTools` (reproduced on retry)

## Conclusion
Rule and subagent path links checked in this validation are resolved correctly.  
One runtime issue was detected in `shell` subagent initialization and should be fixed in tool wiring/configuration.
