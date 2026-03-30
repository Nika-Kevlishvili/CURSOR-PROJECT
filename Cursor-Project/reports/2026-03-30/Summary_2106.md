## Task
Targeted diagnosis for `shell` subagent failure.

## Reproduction
- Tried `Subagent` with `subagent_type: "shell"` in readonly mode.
- Retried in non-readonly mode.
- Both attempts failed with the same internal error:
  - `Required tool READ not found in allTools`

## Findings
- Local rule/agent/skill links are valid and resolvable.
- `.cursor/agents/` does not contain a configurable `shell` agent file, so this is not a workspace markdown-link issue.
- The failure happens before task logic runs, indicating subagent runtime/tool registration mismatch.
- Most likely root cause: the shell subagent runtime expects a tool named `READ` that is not present in its provided tool set.

## Impact
- `shell` subagent cannot be used in this environment.
- Other tested subagents (`explore`, `cross-dependency-finder`) initialize and run normally.

## Recommended Fix
1. Check subagent runtime/tool registry for `shell` and ensure expected read tool is registered (or mapped to available read tool).
2. Verify alias compatibility (`READ` vs currently exposed read tool names).
3. Re-run smoke test after registry fix using a minimal prompt.
