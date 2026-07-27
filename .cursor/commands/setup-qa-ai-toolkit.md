# setup-qa-ai-toolkit

Onboard any project with the portable **QA AI Toolkit** (generic rules/skills/agents/hooks + integrations).

## Manual steps before first run

1. Ensure this workspace contains the `QA-AI-Toolkit/` folder.
2. Install **Node.js LTS + npm** (PostgreSQL MCP uses `npx`).
3. Have GitLab / Jira / Confluence tokens ready to paste into `.env` when the wizard pauses.

## Run

```powershell
.\QA-AI-Toolkit\scripts\install-qa-ai-toolkit.ps1
.\QA-AI-Toolkit\scripts\install-qa-ai-toolkit.ps1 -TargetPath D:\Path\To\Project
.\QA-AI-Toolkit\scripts\install-qa-ai-toolkit.ps1 -FromStep 6
.\QA-AI-Toolkit\scripts\install-qa-ai-toolkit.ps1 -VerifyOnly -TargetPath D:\Path\To\Project
.\QA-AI-Toolkit\scripts\upgrade-qa-ai-toolkit.ps1 -TargetPath D:\Path\To\Project
```

## Wizard flow

Preconditions → copy `.cursor` → project name → environments → write `.env` → **fill `.env` (hard gate)** → GitLab select/clone → DB MCP prompts → merge `mcp.json` → extensions → verify.

After each step: `Proceed to next step? [Y/n/q]`.

Full docs: [QA-AI-Toolkit/README.md](../../QA-AI-Toolkit/README.md).
