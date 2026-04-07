# GitHub self-hosted runner — registration

**Date:** 2026-04-07  
**Repo:** `Nika-Kevlishvili/CURSOR-PROJECT`  
**Path:** `D:\actions-runner`

## Result

- `config.cmd` completed with `--unattended --replace`
- Output: Connected to GitHub; runner successfully added; settings saved

## Next steps (operator)

- Start interactively: `cd D:\actions-runner` then `.\run.cmd` (keeps a terminal open), or install/run as a Windows service per GitHub documentation for this runner version
- In workflows use `runs-on: self-hosted` (and labels if configured)

## Security note

- Registration tokens are one-time; do not commit `.runner` secrets or re-share tokens in chat.
