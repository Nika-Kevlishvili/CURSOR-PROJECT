# setup-qa-ai-toolkit

Install the **QA AI Toolkit** into a target project using an interactive step wizard.

## Run

```powershell
.\QA-AI-Toolkit\scripts\install-qa-ai-toolkit.ps1
.\QA-AI-Toolkit\scripts\install-qa-ai-toolkit.ps1 -TargetPath D:\MyProject
.\QA-AI-Toolkit\scripts\install-qa-ai-toolkit.ps1 -FromStep 6
.\QA-AI-Toolkit\scripts\install-qa-ai-toolkit.ps1 -VerifyOnly -TargetPath D:\MyProject
```

## Wizard reminders

- Answer prompts when asked (project name, environments, GitLab selection, DB fields).
- After each phase, confirm before continuing (`Y/n/q`).
- Fill `.env` **before** GitLab clone (hard gate).
- Repos are selected dynamically; re-run from step 6 to clone more later.

See [QA-AI-Toolkit/README.md](../../QA-AI-Toolkit/README.md).
