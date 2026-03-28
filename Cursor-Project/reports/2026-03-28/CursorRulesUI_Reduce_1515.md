# Cursor Rules list — reduce noise (documentation update)

**Task:** User does not want unnecessary extra rules in Settings; repo-side guidance added (IDE toggle cannot be set from git alone without a documented `settings.json` key).

**Actions taken:**
- Updated `.cursor/README.md` with section **Fewer rules in Cursor Settings** (third-party toggle OFF, nested Phoenix rules explanation, Rule 0.8 constraint).
- Updated `.cursor/rules/README.md` with short cross-reference to the same.

**User action required:** Cursor **Settings → Plugins → Rules, Skills, Subagents** → turn **OFF** “Include third-party Plugins, Skills, and other configs.”

**Not done (policy):** No edits under `Cursor-Project/Phoenix/**` (Tier A).
