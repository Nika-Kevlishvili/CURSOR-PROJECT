---
description: Attach the verification Word report to its Jira ticket comment. Create the report first when the file is not ready yet.
---

# Attach report to Jira

User-triggered only. Run this when the user asks to attach the verification report to the Jira ticket, including `/attach-report-to-jira`, “მიაბა რეპორტი ჯირას”, or the same request in English.

This posts a comment on an **existing** issue and uploads the Word file into that comment. It does not create a Jira ticket. Rule JIRA.0 still forbids creating bugs in Phoenix delivery.

## Comment text

The script writes this English sentence **before** the file:

`These are the main cases that were run and verified for this bug.`

or `this task` when the issue type is a task. If the type is neither, it says `this bug or task`.

Do not rewrite that sentence.

## Steps

1. Resolve the issue key, in this order:
   - key the user named (`PDT-3672`, `PHN-1234`)
   - key in the report path they named
   - key in the Word report just created in this chat
   - if several reports could match and the user did not pick one, ask which ticket. Do not guess.
2. From the workspace root, run:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File ".cursor\commands\attach-report-to-jira.ps1" -IssueKey "<KEY>"
```

If the user named a file, add `-ReportPath "<absolute path>"`.

3. Read the exit code.

| Code | Action |
|------|--------|
| **0** | The comment is posted. Give the user `COMMENT_URL`, the file name, and `COMMENT_TEXT`. |
| **3** | No `.docx` yet. Create it, then attach. See below. |
| **1** or **2** | Stop. Report the script error. Do not invent a second upload. If `ATTACHMENT_ID` is present, say the file is on the ticket and the comment failed. |
| **4** | Ask for the issue key. |

4. Do not print `JIRA_API_TOKEN` or `.env` contents.

## When the report is not ready (exit 3)

1. Read `.cursor/skills/word-report/SKILL.md` and create the Word report from verified cases already in this chat (results, live amounts, portal links). Follow Rule WR.0, including the canvas.
2. Do not invent cases, amounts, statuses, or links. If this chat has no verified cases, stop and ask the user. Do not post a Jira comment.
3. Save the `.docx` under `Cursor-Project/reports/Chat reports/YYYY/<english-month>/DD/`. The file name must start with the issue key, for example `PDT-3672_Topic_Dev_Report.docx`.
4. Run the script again with `-IssueKey` and `-ReportPath` set to that new file.
5. Only the `.docx` is attached. Do not attach the canvas or a markdown companion.

## When the report is ready

Attach immediately. Use the newest `*_Report.docx` under `Cursor-Project/reports` whose name contains that issue key. Do not ask for an extra confirmation.

## Out of scope

- Do not run this as part of `/report` unless the user also asks to put the report on the Jira ticket.
- Do not edit GitLab or Confluence.
- Do not create or rewrite the Jira issue.
