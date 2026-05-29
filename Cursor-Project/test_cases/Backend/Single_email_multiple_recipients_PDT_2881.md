# Single email to multiple recipients — Email Communication (PDT-2881)

**Jira:** [PDT-2881](https://oppa-support.atlassian.net/browse/PDT-2881)  
**Type:** Task (Backend)  
**Summary:** BACKEND — Sending single email to multiple recipients  
**Scope:** Outbound Email Communication must persist one row per recipient in `crm.email_communication_customer_contacts` but call the mass-communication mail client **once** per customer send. Pass/fail uses API responses plus read-only SQL on `task_id` (one distinct non-null `task_id` per send).  
**Linked issue:** PDT-2553 (relates to)  
**Target environment:** Dev (`send.email.enabled=true` required for send assertions)

**Regression risks (cross-dependency):** (1) Mass-communication client called once per customer send — duplicate `task_id` values would break billing/CRM audit. (2) `EmailCommunicationStatusUpdateJobService` assumes one `task_id` per contact batch — multiple tasks per recipient would desync statuses. (3) Invoice/document flows that join emails with `;` must not regress to N separate sends.

---

## Test data (preconditions)

1. Obtain Bearer token (Dev portal user with `EMAIL_COMMUNICATION_CREATE_AND_SEND`, `EMAIL_COMMUNICATION_VIEW_SEND`, `EMAIL_COMMUNICATION_RESEND`). Expect HTTP `200` from a lightweight authenticated `GET` (e.g. `GET /topic-of-communication` filter page size 1).
2. `POST /topic-of-communication` — body per Swagger `TopicOfCommunicationRequest`: `name` (unique string), `status`: `ACTIVE`. Expect HTTP `201`/`200`; save `communicationTopicId` from response `id`.
3. `POST /email-mailboxes` — body per Swagger `EmailMailboxesRequest`: valid `emailAddress`, `status`: `ACTIVE`. Expect HTTP `201`/`200`; save `emailBoxId` from response `id`.
4. `POST /customer` — body per Swagger `CustomerCreateRequest`: type `PRIVATE`, status `ACTIVE`, required identifiers, plus communication data with at least one **email** `contactValue` (e.g. `setup.pdt2881@example.com`) and ACTIVE contact purpose that allows email. Expect HTTP `201`; save `customerId`, latest `customerDetailId`, and the email `customerCommunicationId` from the response.
5. `GET /customer-communications/{customerCommunicationId}` — expect HTTP `200`, communication status ACTIVE, at least one email contact row present.

**Standard SEND payload skeleton** (replace placeholders per TC):

```json
{
  "communicationAsAnInstitution": false,
  "communicationTopicId": "<communicationTopicId>",
  "emailCommunicationType": "OUTGOING",
  "emailCreateType": "SEND",
  "emailBoxId": "<emailBoxId>",
  "customerEmailAddress": "<see TC Delta>",
  "emailSubject": "<see TC>",
  "emailBody": "<p>PDT-2881</p>",
  "customerDetailId": "<customerDetailId>",
  "customerCommunicationId": "<customerCommunicationId>"
}
```

**SQL helper** (read-only, after send):

```sql
SELECT ecc.id, ecc.email_address, ecc.status, ecc.task_id
FROM crm.email_communication_customer_contacts ecc
JOIN crm.email_communication_customers ec ON ec.id = ecc.email_communication_customer_id
WHERE ec.email_communication_id = :emailCommunicationId
ORDER BY ecc.id;
```

**Validation error contract** (Dev — `GlobalExceptionHandler` + `EmailCommunicationRequestValidator`; cite: `phoenix-core-lib/.../GlobalExceptionHandler.java`, `EmailCommunicationRequestValidator.java`):

- HTTP status: **400** `Bad Request`
- Response body (`RestError` JSON):
  - `errorCode`: **`ILLEGAL_ARGUMENTS_PROVIDED`**
  - `message`: semicolon-separated validation fragments; failing field identified by prefix **`customerEmailAddress`**

**Prove no row persisted** (after failed `POST`):

1. `GET /email-communication/list?page=0&size=20&prompt=<emailSubject used in TC>&searchBy=EMAIL_SUBJECT` — expect `totalElements` = **0** (or `content` empty).
2. Read-only SQL: `SELECT COUNT(*) FROM crm.email_communications WHERE email_subject = '<emailSubject>';` — expect **0**.

---

## Backend Test Cases

### TC-BE-1 (Positive): Two recipients — one shared `task_id`

**Description:** Semicolon-separated `customerEmailAddress` with two valid emails creates two contact rows and one mass-comm `task_id`. Covers regression risk **#1**.

**Preconditions:**
1. Apply Test data steps 1–5.
2. Delta: none (baseline two-recipient send).

**Test steps:**
1. `POST /email-communication` with skeleton payload where `customerEmailAddress` = `pdt2881.a.test@example.com;pdt2881.b.test@example.com`, `emailSubject` = `PDT-2881 TC-BE-1`.
2. Expect HTTP `201`; save `emailCommunicationId` (response body `Long`).
3. `GET /email-communication/{emailCommunicationId}?type=EMAIL`.
4. Run SQL helper with `:emailCommunicationId`.

**Expected test case results:**
- Step 1: HTTP `201`; body is numeric id > 0.
- Step 3: HTTP `200`; `emailCommunicationType` = `OUTGOING`; `emailCommunicationStatus` ∈ {`IN_PROGRESS`, `SENT_SUCCESSFULLY`} (Dev mass-comm); `customerEmailAddress` contains both addresses separated by `;`; `sentDate` not null when status is `SENT_SUCCESSFULLY`.
- Step 4: Exactly **2** rows; `email_address` in (`pdt2881.a.test@example.com`, `pdt2881.b.test@example.com`); `COUNT(DISTINCT task_id) FILTER (WHERE task_id IS NOT NULL)` = **1**; all non-error rows share the same `task_id`; `status` not `ERROR` when mail client accepted the job.

---

### TC-BE-2 (Positive): Three recipients — one `task_id`

**Description:** Three-address list uses multi-recipient branch in `EmailSenderService` (`addToRecepients` when `recipientEmailAddresses.size() > 1`). Covers regression risk **#1**.

**Preconditions:**
1. Apply Test data steps 1–5.
2. Delta: `customerEmailAddress` = `pdt2881.r1@example.com;pdt2881.r2@example.com;pdt2881.r3@example.com` (three distinct addresses).

**Test steps:**
1. `POST /email-communication` with full skeleton payload (all fields from Test data), Delta `customerEmailAddress`, `emailSubject` = `PDT-2881 TC-BE-2`, `emailCreateType` = `SEND`.
2. Assert HTTP `201`; save `emailCommunicationId` from response body (`Long` > 0).
3. `GET /email-communication/{emailCommunicationId}?type=EMAIL`.
4. Run SQL helper with `:emailCommunicationId`.

**Expected test case results:**
- Step 1: HTTP `201`; `emailCommunicationId` > 0.
- Step 3: HTTP `200`; `communicationChannelType` = `EMAIL`; `emailCommunicationType` = `OUTGOING`; `emailCommunicationStatus` ∈ {`IN_PROGRESS`, `SENT_SUCCESSFULLY`}; `emailSubject` = `PDT-2881 TC-BE-2`; `customerEmailAddress` contains all three addresses separated by `;`.
- Step 4: **3** rows; `email_address` ∈ {`pdt2881.r1@example.com`, `pdt2881.r2@example.com`, `pdt2881.r3@example.com`}; `COUNT(DISTINCT task_id) FILTER (WHERE task_id IS NOT NULL)` = **1** (regression risk #1).

---

### TC-BE-3 (Positive): Duplicate semicolon entries — one distinct outbound recipient set

**Description:** Duplicate segments in `customerEmailAddress` are deduplicated in `collectRecipientAddresses` (`.distinct()`); regression risk **#1** — only one mass-comm task per send even when input repeats an address.

**Preconditions:**
1. Apply Test data steps 1–5.
2. Delta: `customerEmailAddress` = `pdt2881.dup@example.com; pdt2881.dup@example.com` (same address twice).

**Test steps:**
1. `POST /email-communication` with skeleton payload, Delta `customerEmailAddress`, `emailSubject` = `PDT-2881 TC-BE-3`, `emailCreateType` = `SEND`.
2. Assert HTTP `201`; save `emailCommunicationId`.
3. `GET /email-communication/{emailCommunicationId}?type=EMAIL` — assert `emailCommunicationStatus` ∈ {`IN_PROGRESS`, `SENT_SUCCESSFULLY`}.
4. Run SQL helper for `emailCommunicationId`.

**Expected test case results:**
- Step 1: HTTP `201`; `emailCommunicationId` > 0.
- Step 3: HTTP `200`; status not `DRAFT`.
- Step 4: `COUNT(DISTINCT email_address)` = **1**; `COUNT(DISTINCT task_id) FILTER (WHERE task_id IS NOT NULL)` = **1** (single physical send — regression risk #1).

---

### TC-BE-4 (Positive): Single recipient regression

**Description:** Single address without `;` still sends successfully (`addToRecepient` single-recipient path). Covers regression risk **#1** (must not regress to broken single-send).

**Preconditions:**
1. Apply Test data steps 1–5.
2. Delta: `customerEmailAddress` = `pdt2881.single@example.com` (no semicolon).

**Test steps:**
1. `POST /email-communication` with full skeleton payload, Delta `customerEmailAddress`, `emailSubject` = `PDT-2881 TC-BE-4`, `emailCreateType` = `SEND`.
2. Assert HTTP `201`; save `emailCommunicationId`.
3. `GET /email-communication/{emailCommunicationId}?type=EMAIL`.
4. Run SQL helper.

**Expected test case results:**
- Step 1: HTTP `201`; `emailCommunicationId` > 0.
- Step 3: HTTP `200`; `emailSubject` = `PDT-2881 TC-BE-4`; `emailBody` contains `PDT-2881`; `emailCommunicationType` = `OUTGOING`; `emailCommunicationStatus` ∈ {`IN_PROGRESS`, `SENT_SUCCESSFULLY`}; `customerEmailAddress` = `pdt2881.single@example.com` (no `;`).
- Step 4: **1** row with `email_address` = `pdt2881.single@example.com`; exactly **1** non-null `task_id`; contact `status` ≠ `ERROR` when `send.email.enabled=true`.

---

### TC-BE-5 (Negative): Invalid email segment in list

**Description:** Class-level validator rejects the request when any semicolon-separated segment fails the email regex. Covers regression risk **#3** (invalid input must not partially create contacts/send).

**Preconditions:**
1. Apply Test data steps 1–5.
2. Delta: `customerEmailAddress` = `valid@example.com;not-an-email` (second segment invalid).

**Test steps:**
1. `POST /email-communication` with full skeleton payload, Delta `customerEmailAddress`, `emailSubject` = `PDT-2881 TC-BE-5`, `emailCreateType` = `SEND`.
2. Assert HTTP **400** and parse `RestError` body.
3. `GET /email-communication/list?page=0&size=20&prompt=PDT-2881 TC-BE-5&searchBy=EMAIL_SUBJECT`.
4. Run read-only SQL: `SELECT COUNT(*) FROM crm.email_communications WHERE email_subject = 'PDT-2881 TC-BE-5';`.

**Expected test case results:**
- Step 1: HTTP **400**; response body is **not** a numeric id.
- Step 2: JSON `errorCode` = **`ILLEGAL_ARGUMENTS_PROVIDED`**; `message` contains **`customerEmailAddress`** and **`invalid email format`** (validator message from `EmailCommunicationRequestValidator.validateEmailAddress`).
- Step 3: HTTP `200`; `totalElements` = **0** (Page `content` length = 0).
- Step 4: Count = **0**.

---

### TC-BE-6 (Negative): Trailing semicolon produces empty segment

**Description:** `good@example.com;` produces an empty segment after `split(";")`; empty segment fails regex. Covers regression risk **#3**.

**Preconditions:**
1. Apply Test data steps 1–5.
2. Delta: `customerEmailAddress` = `good@example.com;` (trailing semicolon → empty second segment).

**Test steps:**
1. `POST /email-communication` with full skeleton payload, Delta `customerEmailAddress`, `emailSubject` = `PDT-2881 TC-BE-6`, `emailCreateType` = `SEND`.
2. Assert HTTP **400**; parse `RestError` JSON.
3. `GET /email-communication/list?page=0&size=20&prompt=PDT-2881 TC-BE-6&searchBy=EMAIL_SUBJECT`.
4. `SELECT COUNT(*) FROM crm.email_communications WHERE email_subject = 'PDT-2881 TC-BE-6';` (read-only).

**Expected test case results:**
- Step 1: HTTP **400**; body is not a created id.
- Step 2: `errorCode` = **`ILLEGAL_ARGUMENTS_PROVIDED`**; `message` contains **`customerEmailAddress`** and **`invalid email format`** (empty segment fails `Pattern.matcher`).
- Step 3: `totalElements` = **0**.
- Step 4: SQL count = **0**.

---

### TC-BE-7 (Positive): Whitespace around semicolons trimmed

**Description:** `splitEmail` / `collectRecipientAddresses` trim each segment so spaces around `;` do not break multi-recipient send. Covers regression risk **#1**.

**Preconditions:**
1. Apply Test data steps 1–5.
2. Delta: `customerEmailAddress` = `pdt2881.ws1@example.com ; pdt2881.ws2@example.com` (spaces around separator).

**Test steps:**
1. `POST /email-communication` with full skeleton payload, Delta `customerEmailAddress`, `emailSubject` = `PDT-2881 TC-BE-7`, `emailCreateType` = `SEND`.
2. Assert HTTP `201`; save `emailCommunicationId`.
3. `GET /email-communication/{emailCommunicationId}?type=EMAIL`.
4. Run SQL helper.

**Expected test case results:**
- Step 1: HTTP `201`; `emailCommunicationId` > 0.
- Step 3: HTTP `200`; `emailCommunicationStatus` ∈ {`IN_PROGRESS`, `SENT_SUCCESSFULLY`}; `customerEmailAddress` contains both trimmed addresses (may still show spaces in stored string — SQL is authoritative for persisted contact rows).
- Step 4: **2** rows; `email_address` exactly `pdt2881.ws1@example.com` and `pdt2881.ws2@example.com`; `COUNT(DISTINCT task_id) FILTER (WHERE task_id IS NOT NULL)` = **1**.

---

### TC-BE-8 (Positive): Resend — new communication, still one `task_id` per send

**Description:** `GET /email-communication/{id}/resend` creates a new communication; its send must also collapse recipients to one physical email.

**Preconditions:**
1. Apply Test data steps 1–5.
2. Delta (baseline send): `POST /email-communication` with skeleton fields, `customerEmailAddress` = `pdt2881.res.a@example.com;pdt2881.res.b@example.com`, `emailCreateType` = `SEND`, `emailSubject` = `PDT-2881 TC-BE-8-base` — expect HTTP **201** and body `originalId` > 0; SQL on `originalId` must show **2** contacts and `COUNT(DISTINCT task_id) FILTER (WHERE task_id IS NOT NULL)` = **1** (covers regression risk #1).
3. Delta (resend): permission `EMAIL_COMMUNICATION_RESEND` on the test user.

**Test steps:**
1. `GET /email-communication/{originalId}/resend`.
2. Assert HTTP **200**; parse response body as `Long` → `resendId`.
3. Assert `resendId` > 0 and `resendId` ≠ `originalId`.
4. `GET /email-communication/{resendId}?type=EMAIL` — read `emailCommunicationStatus` into `status`.
5. If `status` = `DRAFT`, wait **5 seconds** (wall clock).
6. Repeat step 4; update `status` (attempt counter ≤ 24; if still `DRAFT` after 24 attempts, fail the TC).
7. Assert `status` ∈ {`IN_PROGRESS`, `SENT_SUCCESSFULLY`, `SENT_FAILED`}.
8. Run SQL helper with `:emailCommunicationId` = `resendId`.

**Expected test case results:**
- Precondition step 2: HTTP `201`; `originalId` > 0; SQL shows 2 contacts + one distinct non-null `task_id`.
- Steps 1–3: HTTP `200`; `resendId` is a new positive id different from `originalId`.
- Step 7: HTTP `200`; `communicationChannelType` = `EMAIL`; `emailCommunicationType` = `OUTGOING`; `emailCommunicationStatus` matches final `status`; `customerEmailAddress` contains both addresses; when `status` = `SENT_SUCCESSFULLY`, `sentDate` is not null.
- Step 8: Exactly **2** rows with `email_address` in (`pdt2881.res.a@example.com`, `pdt2881.res.b@example.com`); `COUNT(DISTINCT task_id) FILTER (WHERE task_id IS NOT NULL)` = **1** for `resendId` (regression risk #2 — status job must not see multiple task ids per recipient set).

---

### TC-BE-9 (Negative): Segment longer than 128 characters

**Description:** Validator enforces max **128** characters per semicolon-separated address (`@Size` / custom check in `EmailCommunicationRequestValidator`). Covers regression risk **#3**.

**Preconditions:**
1. Apply Test data steps 1–5.
2. Delta: `customerEmailAddress` = `short@example.com;` + `a`×125 + `@example.com` (local part 125 chars + `@example.com` → segment length > 128).

**Test steps:**
1. `POST /email-communication` with full skeleton payload, Delta `customerEmailAddress`, `emailSubject` = `PDT-2881 TC-BE-9`, `emailCreateType` = `SEND`.
2. Assert HTTP **400**; parse `RestError`.
3. `GET /email-communication/list?page=0&size=20&prompt=PDT-2881 TC-BE-9&searchBy=EMAIL_SUBJECT`.
4. `SELECT COUNT(*) FROM crm.email_communications WHERE email_subject = 'PDT-2881 TC-BE-9';`.

**Expected test case results:**
- Step 1: HTTP **400**.
- Step 2: `errorCode` = **`ILLEGAL_ARGUMENTS_PROVIDED`**; `message` contains **`customerEmailAddress`** and **`max symbols for each email address is 128`**.
- Step 3: `totalElements` = **0**.
- Step 4: SQL count = **0**.

---

### TC-BE-10 (Positive — variant): `emailCreateType` DRAFT — no `task_id` until send

**Description:** Draft create must not invoke outbound send; multiple recipient rows may exist but all `task_id` remain null until send. Covers regression risk **#2** (status job must not run on unsent drafts).

**Preconditions:**
1. Apply Test data steps 1–5.
2. Delta: `emailCreateType` = `DRAFT` (not `SEND`); two addresses in `customerEmailAddress`.

**Test steps:**
1. `POST /email-communication` with skeleton payload except `emailCreateType`: `DRAFT`, `customerEmailAddress` = `pdt2881.draft1@example.com;pdt2881.draft2@example.com`, `emailSubject` = `PDT-2881 TC-BE-10`.
2. HTTP `201`; save `draftId`.
3. `GET /email-communication/{draftId}?type=EMAIL`.
4. SQL helper for `draftId`.

**Expected test case results:**
- Step 1: HTTP `201`.
- Step 3: HTTP `200`; `emailCommunicationStatus` = **`DRAFT`**; `sentDate` is **null**.
- Step 4: Two contact rows may exist for the two addresses; **all** `task_id` values are **null** (no outbound send yet).

---

## References

| Source | Location |
|--------|----------|
| Jira PDT-2881 | https://oppa-support.atlassian.net/browse/PDT-2881 |
| Sender service | `phoenix-core-lib/.../EmailCommunicationSenderService.java` |
| Mail client | `phoenix-core-lib/.../EmailSenderService.java` |
| Validator | `phoenix-core-lib/.../EmailCommunicationRequestValidator.java` |
| Controller | `phoenix-core/.../EmailCommunicationController.java` |
| Status enum | `EmailCommunicationStatus`: `DRAFT`, `IN_PROGRESS`, `SENT_SUCCESSFULLY`, `SENT_FAILED`, … |
