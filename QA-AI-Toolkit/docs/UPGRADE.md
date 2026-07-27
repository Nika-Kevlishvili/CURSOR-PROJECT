# Upgrade

```powershell
.\QA-AI-Toolkit\scripts\upgrade-qa-ai-toolkit.ps1 -TargetPath D:\MyProject
```

## What upgrade does

- Refreshes `.cursor/rules`, `skills`, `agents`, `hooks` from the toolkit template
- Bumps `toolkitVersion` in `project-config.json`
- Refreshes `config/scripts` and `config/templates`

## What upgrade does NOT do

- Does not delete or re-clone repositories under `codeRoot`
- Does not manage a fixed repo inventory (clone again with installer `-FromStep 6` when you need new repos)
- Does not overwrite `.env`
