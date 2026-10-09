---
name: sdlc-secure
description: Phase 8 — build the AI security system for the product and its agent: threat model, controls mapped to each threat, prompt-injection and data-exfiltration defences, secrets, supply chain (plugins, skills, MCP servers), memory poisoning, and drills that prove each control fires. Use before release, when adding tools, MCP servers or plugins, or when the owner asks about AI security.
---
# Phase 8 — AI security

Read `sdlc/design/ai-security.md` first — it holds the layer model and the threat catalogue.

## Steps
1. Threat model, written so the owner can read it: assets, entry points, trust boundaries, attacker goals. Walk the catalogue in the design doc (prompt injection, sensitive data disclosure, supply chain, excessive agency, memory poisoning, unbounded cost, and the rest).
2. For each threat choose a control — preventive, detective or corrective — and where it is enforced: permission rule, sandbox, hook, code, CI. Deterministic controls first, model-based second.
3. Implement the controls.
4. Review the diff for security — the `auditor` (gate: security), plus Cursor's own security review if your Cursor offers one — and run a secret scan (`gitleaks git --redact`) and a dependency scan; fix HIGH and CRITICAL findings.
5. Drill every control: a clean control passes, a seeded attack is stopped. Record both outputs.
6. Record the security posture as an ADR; `auditor` (gate: security — review loop per AGENTS.md, loop id `gate-secure`); owner approval → phase 8 `approved` + a gate-log line.

## Done when
Every catalogued threat has a control with a passing drill, or an explicit risk acceptance signed by the owner, and the scans show no open HIGH or CRITICAL finding.
