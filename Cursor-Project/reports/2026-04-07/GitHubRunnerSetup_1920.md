# GitHub self-hosted runner — download and extract

**Date:** 2026-04-07  
**Scope:** Prepare `D:\actions-runner` with actions-runner v2.333.1 (win-x64).

## Completed

- Created `D:\actions-runner`
- Downloaded `actions-runner-win-x64-2.333.1.zip` from GitHub releases
- SHA256 verified against published hash (match)
- Extracted archive; `config.cmd` and `run.cmd` present

## Pending (requires user)

- Registration token from GitHub: **Settings → Actions → Runners → New self-hosted runner** (one-time token)
- Run: `.\config.cmd --url <repo-url> --token <token>` from `D:\actions-runner`
- Start runner: `.\run.cmd` (or install as service per GitHub docs)

## Security

- Do not reuse tokens from screenshots; use a fresh token each registration session.
