# Test Case Generation – Process Timing & Tracking (PDT-2553)

**Ticket:** [PDT-2553](https://oppa-support.atlassian.net/browse/PDT-2553)  
**Date:** 2026-03-30  
**Goal:** Instrument and improve **TestCaseGeneratorAgent** / `test-case-generate` workflow with phase-level duration and step-level detail.

---

## 1. Executive timing summary (this run)

| Phase | Description | Est. duration (this session) | Notes |
|--------|-------------|------------------------------|--------|
| **P1 – Rules & skills** | Load Rule 35/35a, read `test-case-generator` + `cross-dependency-finder` skills + template | **~2 min** | User target example aligned (~2m). |
| **P2 – Cross-dependency** | Rule 35a merge lookup; structured dependency payload; optional JSON reuse | **~4–6 min** | User example ~5m. Included git grep PDT-2553 + validation of existing `cross_dependencies` file. |
| **P3 – Confluence** | Search related pages for email comm / mass comm | **~0 min (skipped)** | Confluence MCP not present in this Cursor workspace; **recommend enabling** for production runs. |
| **P4 – Codebase** | Phoenix read-only: `EmailCommunicationSenderService`, `EmailSenderServiceInterface`, mocks | **~3–4 min** | Grep + full file read for citations. |
| **P5 – Test case authoring** | Write `Flows/Email_communication/*.md`, README, update `Flows/README.md` | **~6–8 min** | Exhaustive TCs per Rule 35 + template. |
| **P6 – Artefacts & report** | Cross-dependency file already existed; timing report + Rule 0.6 summary | **~2 min** | |
| **Total (approx.)** | End-to-end | **~17–22 min** | Varies with ticket size and Confluence depth. |

> **Important:** Durations above are **engineering estimates for this session**, not automated timestamps. For agent improvement, capture **real** phase times (see §3).

---

## 2. Detailed steps per phase

### P1 – Rules & skills (~2m)

1. Acknowledge Rule 35: cross-dependency-finder before test-case-generator.
2. Acknowledge Rule 35a: Jira key → merge lookup → conditional sync if merge exists → `technical_details`.
3. Read `.cursor/skills/test-case-generator/SKILL.md`.
4. Read `.cursor/skills/cross-dependency-finder/SKILL.md`.
5. Read `Cursor-Project/config/template/Test_case_template.md` for TC structure.

### P2 – Cross-dependency (~5m)

1. Parse Jira key `PDT-2553` from URL.
2. **Merge lookup:** `grep PDT-2553` under `Cursor-Project/Phoenix/**` (no hits this run).
3. Load or produce structured JSON: reused `Cursor-Project/cross_dependencies/2026-03-30_PDT-2553-email-multi-recipient.json`.
4. Confirm `entry_points`, `what_could_break`, `technical_details.merge_lookup` / `conditional_sync`.
5. *(If merge existed:* targeted `git fetch` / branch sync per `git_sync_workflow.mdc` — **not executed**; none found.)

### P3 – Confluence (~0m here)

1. *(Intended)* cloudId → search “email communication”, “mass comm”, “PDT-2553”.
2. *(Intended)* Store page IDs/titles in `cross_dependency_data` or report.
3. **Blocked this run:** no Confluence MCP in `mcps/` discovery.

### P4 – Codebase – PhoenixExpert evidence (~3–4m)

1. `grep EmailCommunicationSenderService` → locate `phoenix-core-lib/.../EmailCommunicationSenderService.java`.
2. Read implementation: `sendBatch`, `sendSingleInternal`, `sendSingleMass` loops calling `emailSenderService.sendEmail` per contact.
3. Read `EmailSenderServiceInterface.sendEmail` signature (single `recipientEmailAddress`).
4. Note `MockEmailSenderService` for regression tests.

### P5 – Test case authoring (~6–8m)

1. Choose flow folder: `test_cases/Flows/Email_communication/` (new).
2. `README.md` for flow index.
3. `Multi_recipient_single_message.md`: positive multi-recipient, single-recipient, send-disabled, validation negative.
4. `Status_polling_and_task_mapping.md`: taskId, polling, partial failure, Bcc, mock.
5. Update `test_cases/Flows/README.md` table row.

### P6 – Reporting (~2m)

1. This timing document.
2. `Summary_1828.md` (Rule 0.6).

---

## 3. Recommendations to improve agent timing (instrumentation)

1. **Structured phase timer (pseudo-code for agent wrapper):**
   - At start of each phase: `phase_start[name] = utcnow()`.
   - At end: `duration_ms = (utcnow() - phase_start[name]).total_seconds() * 1000`.
   - Append JSON line to `Cursor-Project/reports/YYYY-MM-DD/test_generate_timing.jsonl`.

2. **Mandatory fields per run:**
   - `jira_key`, `timestamp_start`, `timestamp_end`, `phases[]` with `{ "id", "substeps_done[], "duration_sec", "blocked_reason" }`.

3. **Parallelism (careful):**
   - Confluence search + git merge lookup can run in parallel after Jira key is known.
   - Do **not** parallelise “cross-dependency output → test cases” (strict order).

4. **Cache reuse:**
   - If `cross_dependencies/YYYY-MM-DD_<key>_<slug>.json` exists and is fresh (same commit SHA), skip full rediscovery; only validate merge tip (fast git rev-parse).

5. **Close gap:**
   - Enable Confluence MCP in Cursor for this project to avoid P3 skip (~2–5m lost quality, not necessarily wall time).

---

## 4. Agents involved

TestCaseGeneratorAgent, CrossDependencyFinderAgent, PhoenixExpert (codebase consultation).

---

## 5. Deliverables

| Artefact | Path |
|----------|------|
| Cross-dependency (input) | `Cursor-Project/cross_dependencies/2026-03-30_PDT-2553-email-multi-recipient.json` |
| Test cases | `Cursor-Project/test_cases/Flows/Email_communication/` |
| Flow index update | `Cursor-Project/test_cases/Flows/README.md` |
| This report | `Cursor-Project/reports/2026-03-30/TestCaseGeneration_ProcessTiming_PDT-2553.md` |

---

## 6. Run 2026-03-30 (18:32) – structured tracking

**Purpose:** Same phase labels as §1–2, plus **machine-readable** log for tuning the orchestrator.

| Phase | Example budget (your target) | This run – substeps | Measured micro-ops (indicative) |
|--------|------------------------------|---------------------|----------------------------------|
| **P1 – Rules & skills** | ~2 min | Skills + template + Rule 35/35a loaded | *(not wall-timed; host should time)* |
| **P2 – Cross-dependency** | ~5 min | Key parse → grep Phoenix for merge → reuse `cross_dependencies/*.json` | **~1.78 s** full-tree `Select-String` on `Phoenix/**/*.java` (Windows; some long-path read errors); **~0.002 s** read JSON |
| **P3 – Jira** | ~0.5–1 min | `getAccessibleAtlassianResources` → `getJiraIssue` PDT-2553 | *(MCP latency only in host logs)* |
| **P4 – Rovo search** | ~0.5–1 min | `searchAtlassian` (related issues; not ticket body — description null in API) | *(MCP)* |
| **P5 – Codebase** | ~3–4 min | Read `EmailCommunicationSenderService` batch loop | **~0.001 s** read first 180 lines (disk cache) |
| **P6 – Authoring** | variable | New `Shared_email_client_and_notifications.md`; README + cross_dep `jira_snapshot`; fix `Status_polling` reference | *(human/agent composition)* |
| **P7 – Reporting** | ~2 min | This §6 + `test_generate_timing_PDT-2553.jsonl` + Rule 0.6 agent/summary | *(file I/O)* |

**Jira (API snapshot):** Summary — *When send email to more than one recipient, to send one email with multiple recipients not many separate emails*; **Status:** Done on Dev; [PDT-2553](https://oppa-support.atlassian.net/browse/PDT-2553).

**Code anchor (pre-change N sends):** Per-contact loop calling `emailSenderService.sendEmail` in batch processing:

```109:124:c:\Users\N.kevlishvili\Cursor\Cursor-Project\Phoenix\phoenix-core-lib\src\main\java\bg\energo\phoenix\service\crm\emailCommunication\EmailCommunicationSenderService.java
                for (EmailCommunicationCustomerContact currentContact : emailCommunicationCustomerContacts) {
                    MDC.put("contactId", String.valueOf(currentContact.getId()));
                    MDC.put("contactEmail", currentContact.getEmailAddress());
                    try {
                        Optional<EmailMailboxes> mailbox = emailMailboxesRepository.findById(emailCommunication.getEmailBoxId());
                        String senderEmail = mailbox.map(EmailMailboxes::getEmailAddress).orElse(null);

                        log.info("[EMAIL SENDER] Sending email to: {} from: {}", currentContact.getEmailAddress(), senderEmail);

                        Optional<SendEmailResponse> sendEmailResponseOptional = emailSenderService.sendEmail(
                                currentContact.getEmailAddress(),
                                subject,
                                body,
                                attachments,
                                senderEmail
                        );
```

**Machine-readable log:** `Cursor-Project/reports/2026-03-30/test_generate_timing_PDT-2553.jsonl` (one JSON object per line; append future runs).

**Orchestrator note:** Set `duration_sec` on each phase in the host (Cursor task runner or CI) using `Stopwatch`; keep this jsonl as the **audit** stream. The **~2 m / ~5 m** targets map to P1 and P2 in §1.
