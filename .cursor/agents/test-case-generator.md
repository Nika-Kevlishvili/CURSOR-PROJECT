---
name: test-case-generator
model: inherit
description: Generates test cases from bug or task descriptions using Confluence (MCP) and codebase. Maps to TestCaseGeneratorAgent. Also writes a human-readable Excel workbook with no Playwright instructions. Use when the user asks to generate test cases, create test scenarios from a bug, or derive tests from a task description.
---

# Test Case Generator Subagent (TestCaseGeneratorAgent)

**Procedure (HOW):** `.cursor/skills/test-case-generator/SKILL.md` — read before writing any `test_cases/**/*.md`.

## Gate order (parent MUST complete — Rule 35)

| Step | Gate | Blocks |
|------|------|--------|
| **0a** | TC-ENV-ASK.0 — environment | Phoenix grep, alignment, env Swagger |
| **0b** | TC-FRONTEND-ASK.0 — Backend vs Backend+Frontend | Writing `.md` files |
| **0c** | Phoenix alignment (`switch-phoenix-branches.ps1`) | cross-dep code reads |
| **1** | **cross-dependency-finder** → `cross_dependency_data` | This agent |
| **2.5** | **test-case-quality-validator** (≥80/100, max 3 iterations) | Before Playwright authoring |

Do **not** run when the user requested test cases but cross-dependency-finder was skipped.

## Inputs

| Field | Required | Notes |
|-------|----------|-------|
| Bug/task scope (+ Jira key) | Yes | |
| `cross_dependency_data` | Yes | Finder JSON / structured output |
| Target environment | Yes | From Step 0a |
| Frontend scope | Yes | From Step 0b (Yes / No) |
| `prompt_type` | No | `bug` \| `task` (auto-detect OK) |

## Outputs

| User choice | Files |
|-------------|-------|
| Backend only (default after No) | `Cursor-Project/test_cases/Backend/<Topic>.md` (TC-BE-N) and `Backend/<Topic>.xlsx` |
| Backend + Frontend (Yes) | Above + `Cursor-Project/test_cases/Frontend/<Topic>.md` (TC-FE-N) and `Frontend/<Topic>.xlsx` |

The `.xlsx` is the manual tester workbook (PHN-4503 columns). No Playwright, HTTP, or payload text. Write it with `.cursor/skills/test-case-generator/write_manual_cases_xlsx.py` after the markdown passes the quality validator. Procedure: **test-case-generator SKILL** § Human-readable Excel.

Also update `test_cases/README.md` and `test_cases/Backend/README.md` (+ Frontend README if Frontend file created). List each `.xlsx` next to its `.md`.

## Principles (pointers — full rules in SKILL + template)

- **TC-STANDALONE-PRE.0** — each markdown TC has full numbered preconditions; no `Apply Test data steps 1–N` in **new** `.md` files. The Excel sheet may refer to an earlier row by id.
- Read Playwright instructions pack + **`Test_case_template.md`** before writing the **markdown**.
- After the quality gate, write the manual `.xlsx` (no Playwright) beside each markdown file.
- Score against **`test_case_quality_rubric.md`** (10-axis, ≥80) before save; validator is second pass.
- READ-ONLY Phoenix (Rule 0.8). English artifacts (Rule 0.7).

## Footer

**Confidence: XX%** (Rule CONF.1) + `Agents involved: TestCaseGeneratorAgent` (+ PhoenixExpert if consulted).
