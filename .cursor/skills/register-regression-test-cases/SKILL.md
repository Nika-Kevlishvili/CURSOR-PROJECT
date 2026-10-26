---
name: register-regression-test-cases
description: Registers automation test cases on the Jira REG (Regression) board as issue type Test Case. Use after test-case markdown is written, or when the user asks to write the automation test case for a spec, title, or scenario. Checks for an existing REG case first. Does not create Bugs and does not use phoenix-bug-reporter.
---

# Register regression test cases

Create **Test Case** issues on the Jira **REG** project (`oppa-support.atlassian.net`). One issue per distinct outcome in `Cursor-Project/docs/regression-scope-method.md` (**What is created on the REG board**). Same-outcome variants stay in the source markdown. Each created issue **Relates** to that topic's **subtask** only, and has its own **Regression Scope**. Do not link the parent task.

Do **not** use `phoenix-bug-reporter` or `jira-bug`. REG is denied for **Bug** creation. This skill creates issue type **Test Case** only.

**Cloud:** call `getAccessibleAtlassianResources` once per session. Use the resource whose URL is `https://oppa-support.atlassian.net`. Pass that `cloudId` on every Jira call.

## Description template

Every REG Test Case `description` uses exactly these three sections, in this order. Do not add other headings. Write them in English.

Before writing, name the **business idea**: what money, status, or refusal this case protects for the user. Take that idea from the spec, the source ticket, or the documented change. If the idea cannot be recognized, ask and wait. Do not register a guessed case.

Keep the text short enough to read in one pass. Do not describe endpoints, HTTP codes, or payload field names. Status names that a user sees (Real, Cancelled, Invoiced, Uninvoiced) stay.

```markdown
**Description:**

{One or two sentences. The business outcome: which debt, compensation, or refusal changes.}

**Reproduce steps:**

1. {Objects created, in plain words: customer, recipient, point of delivery, contract, price, compensation, and so on.}
2. {The action taken on those objects: bill, cancel, offset, reschedule, try again.}

**Expected result:**

{The observable business outcome. Statuses, which balances are zero and kept as history, and what is refused and why.}
```

**Description** is the business idea only. **Reproduce steps** list which objects are created and what is done with them. **Expected result** is the outcome a business reader can check. Fill them from the spec or the source case. If any of the three is missing or unclear, ask and wait. Do not create that case.

Worked example (REG-1385, one positive government compensation on a Real debit note):

**Description:** Cancelling a Real debit-note invoice with one positive government compensation closes the customer's remaining debt and both sides of the compensation, and sets the compensation back to Uninvoiced.

**Reproduce steps:** create the legal customer, a different legal recipient, the electricity point of delivery, terms, the volume price, the product and contract; add profile data and one compensation of 20.00; run volume billing and finish accounting; cancel the invoice; try to regenerate compensations; try to cancel the same invoice again.

**Expected result:** the invoice is Cancelled; the remaining customer debt, the government liability, and the customer receivable are zero and remain as history; the compensation is Uninvoiced and no longer linked to the invoice. Regeneration is refused because the invoice is no longer Real. A second cancellation is refused because the invoice is already cancelled. The compensation stays Uninvoiced.

## When to use

**After generation.** Rule 35 quality gate has passed and markdown files are saved. Ask once, for the whole batch: register these test cases on the REG board? **No** stops. **Yes** continues. This offer is not part of Rule 35 itself.

**Direct request.** The user asks to write the automation test case (a spec path, a `test(...)` title, or a scenario). That request is consent to create. Do not ask a second "register?" question. There is no need to wait for a markdown file.

## Direct request

1. Identify the automation: spec path, test title, or the scenario named in the message.
2. Existence search (below). If a REG Test Case already covers it, return that key and stop.
3. Read the spec or the user's scenario. Continue only when the description, reproduce steps, and expected result are all clear.
4. If any of those is unclear, ask that question and wait. After the answer, continue. Do not create a guessed case.

## Shared steps (both entries)

Ask only what is still unknown. Do not re-ask a value already in the chat, the spec, the markdown header, or the source ticket.

1. **Existence.** For each case run:

   `project = REG AND issuetype = "Test Case" AND (summary ~ "<title>" OR description ~ "<spec path>")`

   On a match, report the key and skip that case.

2. **ENV** (`customfield_11252`), one value for the request. If Dev or Dev2 is not already named, ask. Dev → `DEV`. Dev2 → `DEV2`. Do not use **Failed ENV**.

3. **Bulgarian bug** (`customfield_11185`, string). If a PDT key is already in the markdown header or the user message, write `https://oppa-support.atlassian.net/browse/PDT-xxxx` and do not ask. If the source looks like a PDT task but the key is missing, ask for the link and wait. If there is no PDT task, omit the field.

4. **Regression Scope** (`customfield_11285`) on **each** case that will be created. Before assigning any value, read **`Cursor-Project/docs/regression-scope-method.md`** in full, including **What is created on the REG board**, the same-area table, and the rule that a bare create on an HIGHEST process stays in that process. The visual is `Cursor-Project/docs/regression-scope-pattern.html`. Read the summary and the expected result. Name the outcome the steps assert. The first yes fixes the rank. If the summary and the steps protect different outcomes, rank the outcome the steps assert and say so in the reply. A rank already chosen at test-case generation is reused only when it matches this read. If it does not, the method wins.

   Rank every scenario. Register one case per distinct outcome inside that rank. The section **What is created on the REG board** defines a same outcome: the same wrong balance, billed party, or supply flag, with only a count, a period, a second object of the same kind, or an extra error text changing. Do not create REG issues for those variants. Leave them in the source markdown. Fold a check that is one more call on the same setup into the representative case. Do not move a HIGHEST variant to Version 1.1 or Version 1.2 to shrink the first wave. Do not invent LOW. HIGHEST is Version 1, and that case runs in the first wave.

   | Rank from the method | Jira value | Option id |
   |----------------------|------------|-----------|
   | HIGHEST | `Version 1` | `10454` |
   | HIGH | `Version 1.1` | `10455` |
   | MEDIUM | `Version 1.2` | `10456` |
   | Out of scope | omit the field | — |

   Do not ask which Version to use. HIGHEST, HIGH, and MEDIUM are written from the method with no scope question. Out of scope means listing, sorting, paging, a screen, a field format, or a group that does not block contract create and does not post money. For each out-of-scope case, ask once whether to create it. **No** skips that case. **Yes** creates it and leaves **Regression Scope** empty. Same-outcome variants are not created and are not asked. Do not copy the topic ticket's scope. Do not invent a version.

5. **Topic subtask**, once per request. Test Case is not a subtask. Link with **Relates** to the topic's **subtask** only. Do not link the parent task, and do not set the parent field.
   - Determine the subject first. Use a REG key the user already named. Otherwise search `project = REG AND issuetype != "Test Case" AND summary ~ "<topic>"`. One clear match: use it. Zero or several matches: ask for the REG topic key and wait.
   - If that issue is already a **Subtask**, it is the link target.
   - If it is a parent (Task or other), read its subtasks. Link the subtask that holds the test cases for that subject. One such subtask: use it. Several: ask which subtask and wait. None: ask and wait. Do not fall back to the parent task.
   - On the generation path, name that subtask key in the consent question.
   - Example: Invoice Cancellation is REG-109. Its test-case subtask is REG-1059. A cancellation case Relates only to REG-1059. REG-1068 is the same pattern.

6. **Automation** (`customfield_10514`) for each case, from the EnergoTS `main` check below. Do not ask which value to use. Say the value and the evidence (spec path, and whether `origin/main` contains that test) in the reply.

7. **Create** only after every gap above is answered and the description has all three sections. One `createJiraIssue` per case. If a create fails mid-batch, report keys already created and stop.

8. **Link.** `createJiraIssueLink` with `linkType` `Relates`, `inwardIssue` = new key, `outwardIssue` = the **subtask** key from step 5. Do not also link the parent task. If that call fails, `POST {JIRA_BASE_URL}/rest/api/3/issueLink` with the existing Jira credentials, only for this approved create. Body: `{ "type": { "name": "Relates" }, "inwardIssue": { "key": "<new>" }, "outwardIssue": { "key": "<subtask>" } }`.

9. **Write-back.** When a source markdown file exists, append each new key under that file's **References** as `[REG-xxxx](https://oppa-support.atlassian.net/browse/REG-xxxx)` next to the matching TC heading. Do not create a markdown file only to store the key.

## Automation status (EnergoTS main)

`Automation` is required. The field has no Yes option. Set it from where the Playwright spec for **this** case actually is. A spec on the local `cursor` branch is not enough for `Done`.

Stay on `cursor`. Do not checkout, switch, merge, pull, or rebase. From `Cursor-Project/EnergoTS`, run `git fetch origin main` so `origin/main` is current. That fetch updates the remote-tracking ref only. If the fetch fails, use the `origin/main` already on disk and say so in the reply.

1. Find the spec. Use the path or `test(...)` title the user named, or the spec named in the markdown. If neither is named, search the EnergoTS repo for a `.spec.ts` whose path or `test(...)` title matches this case (PDT key, case title). Count untracked and uncommitted files. A file that exists only in the working tree still counts as automation that exists.
2. Decide:

   | Where the spec is | Automation value | Option id |
   |-------------------|------------------|-----------|
   | No matching `.spec.ts` in the working tree and none on `origin/main` | `To Do` | `10110` |
   | A matching spec exists (working tree or `cursor`) but that file, or the `test(...)` for this case, is not on `origin/main` | `Needs to be deployed ` | `11288` |
   | The file is on `origin/main`, and when a `test(...)` title is known that title is in the `origin/main` blob | `Done` | `10112` |

3. Check `origin/main` without leaving `cursor`. Path is relative to the EnergoTS repo root (`tests/...`, not `Cursor-Project/EnergoTS/tests/...`).

   `git -C Cursor-Project/EnergoTS cat-file -e "origin/main:<rel>"`

   Exit 0 means the file is on main. When a test title is known, read the blob with `git show "origin/main:<rel>"` and confirm that title is in it. A file that is already on main, with this test added only in the working tree, is `Needs to be deployed `.

4. Send option id `11288` for the third value. The Jira option text is `Needs to be deployed ` with one trailing space. The id avoids dropping that space. Field key is `customfield_10514`.

## Create payload

`createJiraIssue`:

| Argument | Value |
|----------|--------|
| `projectKey` | `REG` |
| `issueType` | `Test Case` |
| `summary` | Case title |
| `description` | Filled from the description template section. Markdown. |
| `labels` | `Backend_Automation` for `TC-BE-*` or a backend spec. `Frontend_Automation` for `TC-FE-*` or a frontend spec. If the layer is unclear, ask. |
| `assignee` | Account id of the current user (`atlassianUserInfo`). That user is the reporter. Do not assign anyone else unless the user names a different person. |

`additional_fields` (names; option ids are the fallback if name resolution fails):

| Field | Value |
|-------|--------|
| ENV | `DEV` or `DEV2` (`10420` / `10421`) |
| Regression Scope | Rank from `regression-scope-method.md`: HIGHEST → `Version 1` (`10454`), HIGH → `Version 1.1` (`10455`), MEDIUM → `Version 1.2` (`10456`). Do not ask. Out of scope: ask whether to create; on yes, omit this field. |
| Automation layer | `Backend Automation` (`10933`) and/or `Frontend Automation` (`10932`). Array. |
| Regression | `Yes` (`10283`). Required. |
| Automation | From **Automation status (EnergoTS main)**. `Done` (`10112`) only when this case's spec is on EnergoTS `origin/main`. `Needs to be deployed ` (`11288`, trailing space; send the id) when a spec exists but is not on `origin/main`. `To Do` (`10110`) when no spec exists. Do not ask. |
| Bulgarian bug | PDT browse URL, or omit |

Set **assignee** to the current user. Jira sets that same user as **reporter**. Do not set priority. Do not set **Failed ENV**. If the user names a different assignee, use that person instead.

## Reply

List created keys as browse links, the Automation value for each created case and why (spec path, on `origin/main` or not), skipped existing keys, cases not created because they repeat an outcome already registered, and any case not created because a gap is still open.

**Confidence: XX% (ZONE)** from the Jira evidence you actually used (createmeta values above are verified for REG Test Case `10405`).  
`Agents involved: register-regression-test-cases`
