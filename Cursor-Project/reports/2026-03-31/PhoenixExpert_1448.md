# PhoenixExpert — update-swagger-specs command doc

**Date:** 2026-03-31  
**Task:** Align Cursor slash command docs with Experiment Swagger.

## Changes

- **`.cursor/commands/update-swagger-specs.md`:** Added **experiment** row to canonical URL table; replaced hardcoded `d:\Cursor\cursor-project\...` full-path example with a placeholder under the user’s workspace pattern.
- **`.cursor/commands/update-swagger-specs.ps1`:** Comment only — notes that the environment list comes from `environments.json` (includes experiment, extensible for PreProd).

## Note

The script already consumed `experiment` from `environments.json` before this edit; the markdown table was the missing piece for humans reading the command.
