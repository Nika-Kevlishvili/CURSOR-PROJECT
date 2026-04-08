# Git push + Windows checkout fix

**Date:** 2026-04-07

## Done

1. **Commit** `b4a402e` on `Do-not-touch`: `fix(ci): enable Git core.longpaths before checkout on Windows self-hosted`
2. **Push** `origin/Do-not-touch` (bdd697b..b4a402e)
3. **Merge** `Do-not-touch` → `main`, **push** `origin/main` (cdf8b92..94b2d72)

## CI change

- Added a step **before** `actions/checkout` on Windows: `git config --global core.longpaths true`
- Mitigates `Filename too long` / `git checkout` exit code 128 on self-hosted Windows runners when the repo has very deep paths.

## If checkout still fails on the runner

1. **Once on the runner machine (PowerShell as Administrator):** enable Windows long paths:
   `New-ItemProperty -Path "HKLM:\SYSTEM\CurrentControlSet\Control\FileSystem" -Name "LongPathsEnabled" -Value 1 -PropertyType DWORD -Force`
   (Reboot may be required.)
2. Ensure **Git for Windows** is current.
3. Consider a **shorter** runner work folder (GitHub runner `work` directory) if paths remain extreme.

## Not committed

- Local modifications under `Cursor-Project/EnergoTS` and `Cursor-Project/Phoenix/*` (nested repo / submodule-like `m` status, no `.gitmodules` mapping). Left untouched to avoid accidental pointer changes.
