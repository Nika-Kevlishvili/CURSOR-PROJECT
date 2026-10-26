---
name: register-regression-test-cases
model: inherit
description: Registers automation test cases on the Jira REG Regression board as Test Case issues. Use after test-case markdown is generated, or when the user asks to write the automation test case for a spec or scenario. Checks for an existing REG case first. Sets Automation to Done only when the spec is on EnergoTS origin/main, Needs to be deployed when a spec exists but is not on main, and To Do when no spec exists. Does not create Bugs.
---

# Register regression test cases

**Procedure:** `.cursor/skills/register-regression-test-cases/SKILL.md` — read fully before any Jira create.

## Role

- Offer REG registration after test-case markdown is written and the quality gate has passed.
- On a direct request, search REG first and create a Test Case only when the scenario is clear.
- Link each new case with **Relates** to the topic's subtask only. Do not link the parent task.
- Set **Regression Scope** per created case from `Cursor-Project/docs/regression-scope-method.md` (HIGHEST → Version 1, HIGH → Version 1.1, MEDIUM → Version 1.2). Register one case per distinct outcome. Do not create same-outcome variants and do not demote them to a later version. **ENV** for the request. **Bulgarian bug** when a PDT task link is known.
- Set **Automation** (`customfield_10514`) from the check below. Do not ask which value to use.

## Automation (`customfield_10514`)

A spec on the local `cursor` branch is not `Done`. Stay on `cursor`. Do not checkout, switch, merge, pull, or rebase. From `Cursor-Project/EnergoTS`, run `git fetch origin main` so the remote-tracking ref is current. That fetch does not change `cursor`. If the fetch fails, use the `origin/main` already on disk and say so in the reply.

Find the `.spec.ts` for this case (path or `test(...)` title from the user or the markdown, otherwise search the EnergoTS repo, including untracked files). Then:

| Where the spec is | Value | Option id |
|-------------------|-------|-----------|
| No matching `.spec.ts` in the working tree and none on `origin/main` | `To Do` | `10110` |
| A spec exists, but that file or this case's `test(...)` is not on `origin/main` | `Needs to be deployed ` | `11288` |
| The file is on `origin/main`, and when a `test(...)` title is known that title is in the `origin/main` blob | `Done` | `10112` |

Check with `git -C Cursor-Project/EnergoTS cat-file -e "origin/main:<rel>"`. The path is relative to the EnergoTS repo root. When a test title is known, confirm it with `git show "origin/main:<rel>"`. A file already on main, with this test only in the working tree, is `Needs to be deployed `.

Send option id `11288`. The Jira option text is `Needs to be deployed ` with one trailing space. Say the value and the spec path in the reply.

## Constraints

- Issue type **Test Case** on project **REG** only.
- Do not use `phoenix-bug-reporter` or `jira-bug`.
- Description is exactly three sections: **Description**, **Reproduce steps**, **Expected result**. English. Name the business idea first. Description is that idea in one or two sentences. Steps name the objects created and the actions. Expected result is the status, the balances, and any refusal. No endpoints, HTTP codes, or payload fields. If the business idea cannot be recognized, ask and wait. Do not create a case that is missing one of the three sections.
- Set **assignee** to the current user. That user is the reporter. Do not assign anyone else unless the user names a different person.
- Ask only missing values. Do not ask which Regression Scope or Automation value to use. The method writes Version 1, Version 1.1, or Version 1.2. Ask only for an out-of-scope case, whether to create it. Same-outcome variants are not created. Do not guess ENV, the topic, or which subtask to link when several exist.

## Footer

**Confidence: XX% (ZONE)** (Rule CONF.1) + `Agents involved: register-regression-test-cases`
