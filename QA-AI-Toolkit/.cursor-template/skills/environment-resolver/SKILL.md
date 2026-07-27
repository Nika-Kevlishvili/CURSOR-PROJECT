---
name: environment-resolver
description: Resolve environment from user/ticket against project-config.json.
---

# Environment resolver

1. Read `.cursor/project-config.json` → `environments[]`.
2. Match user text or ticket fields to `id`/`label`.
3. If ambiguous or missing → ask user to choose from the configured list.
4. Never invent environments not in the config.
