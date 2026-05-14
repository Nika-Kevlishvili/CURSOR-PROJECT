# QA Cursor Toolkit

A self-contained, **project-agnostic** toolkit for QA engineers using **Cursor IDE**. Includes Atlassian REST scripts, universal Cursor rules, agents, skills, commands, test case templates, and database workflow configuration.

**No project-specific logic** — works with any QA project out of the box.

## Folder Structure

```
qa-cursor-toolkit/
├── .env.example                          # Template — copy to .env, fill in credentials
├── .env                                  # YOUR credentials (never commit!)
├── README.md                             # This file
│
├── install.ps1                           # One-command installer
├── .gitignore                            # Protects .env from commits
├── hooks.json                            # Cursor hooks config
│
├── scripts/                              # REST scripts (standalone)
│   ├── jira/
│   │   ├── fetch-issue.ps1               # Fetch full Jira issue as JSON
│   │   └── download-attachments.ps1      # Download attachments + DOCX text extraction
│   ├── confluence/
│   │   ├── get-page.ps1                  # Fetch Confluence page by numeric ID
│   │   └── cql-search.ps1               # Search Confluence via CQL query
│   └── slack/
│       ├── send-message.ps1              # Send message to Slack channel
│       └── upload-file.ps1              # Upload file to Slack channel
│
├── hooks/                                # Safety hook scripts
│   ├── block-credential-commit.ps1       # Blocks .env / credential file commits
│   └── block-destructive-git.ps1        # Blocks force-push / direct push to main
│
├── config/                               # Project config templates
│   └── swagger/
│       ├── README.md                     # Swagger spec management guide
│       ├── environments.json             # Maps envs to API doc URLs (you create)
│       └── update-swagger-specs.ps1      # Downloads specs for all environments
│
├── agents/                               # Cursor subagent definitions → .cursor/agents/
│   ├── README.md                         # Agent map and instructions
│   ├── qa-workflow.md                    # Orchestrator — sequences multi-agent pipelines
│   ├── bug-validator.md                  # Bug validation (5-verdict matrix)
│   ├── cross-dependency-finder.md        # Cross-dependency analysis
│   ├── test-case-generator.md            # Test case generation (Backend/Frontend)
│   ├── test-case-quality-validator.md    # TC quality scoring (6 axes, 8/12 threshold)
│   ├── environment-resolver.md           # Resolves target environment
│   ├── database-query.md                # PostgreSQL via MCP (environment-aware)
│   ├── shell.md                          # CLI/terminal delegation
│   └── report-generator.md              # Markdown report saving
│
├── skills/                               # Cursor skills (SKILL.md) → .cursor/skills/
│   ├── bug-validation/SKILL.md           # Bug validation workflow
│   ├── cross-dependency-finder/SKILL.md  # Cross-dep discovery
│   ├── test-case-generator/SKILL.md      # TC generation workflow
│   ├── database-workflow/SKILL.md        # DB connect-first workflow
│   └── jira-ticket-analysis/SKILL.md     # Full Jira ticket analysis
│
├── commands/                             # Cursor slash commands → .cursor/commands/
│   ├── test-case-quality.md              # /test-case-quality <Topic>
│   ├── report.md                         # /report — save analysis to disk
│   └── feedback.md                       # /feedback — save feedback to disk
│
├── .gitmodules.example                   # Git submodules template for your repos
│
├── rules/                                # Cursor rules (.mdc) → .cursor/rules/
│   ├── main/
│   │   ├── core_qa_rules.mdc             # Agent disclosure, confidence, English artifacts
│   │   ├── clarification_and_confidence.mdc  # Confidence gate — ask before guessing
│   │   └── evidence_only_answers.mdc     # Evidence-based answers, Jira completeness
│   ├── safety/
│   │   └── safety_rules.mdc             # Read-only external systems, no credential logging
│   ├── integrations/
│   │   ├── jira_rest_fallback.mdc        # MCP primary → REST fallback for Jira reads
│   │   ├── confluence_rest_fallback.mdc  # MCP primary → REST fallback for Confluence reads
│   │   └── database_workflow.mdc         # DB environment mapping, connect-first, security
│   └── workspace/
│       └── test_cases_structure.mdc      # Backend/Frontend two-folder TC layout
│
└── templates/
    ├── Test_case_template.md             # Canonical test case structure + DRY preconditions
    └── test_case_quality_rubric.md       # 6-axis scoring rubric (8/12 threshold)
```

---

## Quick Start

### Step 1: Set Up Credentials

```powershell
cd qa-cursor-toolkit
Copy-Item .env.example .env
```

Edit `.env` and fill in your real values:

```env
JIRA_EMAIL=your-email@company.com
JIRA_API_TOKEN=your-api-token
JIRA_BASE_URL=https://your-site.atlassian.net
```

**Get an API token:** [id.atlassian.com/manage-profile/security/api-tokens](https://id.atlassian.com/manage-profile/security/api-tokens)

### Step 2: Install Everything (one command)

```powershell
powershell -ExecutionPolicy Bypass -File "path\to\qa-cursor-toolkit\install.ps1" -TargetProject "C:\Users\you\YourProject"
```

This copies **all** toolkit components into your project:
- **Agents:** qa-workflow orchestrator, bug-validator, cross-dep, test-case-generator, TC quality, environment-resolver, DB query, shell, report-generator
- **Skills:** Routing guides for bug validation, cross-deps, TC generation, DB workflow, Jira analysis
- **Commands:** `/test-case-quality`, `/report`, `/feedback`
- **Rules:** Confidence gate, evidence-based answers, REST fallback, read-only safety, TC structure
- **Hooks:** Block credential commits + block force-push
- **Templates:** Test case template + quality rubric
- **Scripts:** Jira, Confluence, Slack REST scripts
- **Config:** Swagger spec management

### Manual installation (alternative)

If you prefer to copy manually, see the folder structure above and copy each folder's contents into the corresponding `.cursor/` subdirectory.

### Step 4: Use Scripts

All scripts run from the `scripts/` directory with PowerShell 5.1+ (built into Windows):

```powershell
cd qa-cursor-toolkit
```

---

## Scripts Usage

### Jira: Fetch a Full Issue

```powershell
powershell -ExecutionPolicy Bypass -File scripts\jira\fetch-issue.ps1 -IssueKey 'PROJ-123'
```

**Output:** `output\PROJ-123\issue-rest.json` — all fields, custom fields, links, attachments metadata, changelog peek.

### Jira: Download Attachments

```powershell
powershell -ExecutionPolicy Bypass -File scripts\jira\download-attachments.ps1 -IssueKey 'PROJ-123'
```

**Output:** `output\PROJ-123\attachments\` + `manifest.json`. Auto-extracts text from `.docx` files.

### Confluence: Fetch a Page

```powershell
# v1 API (default — full expand)
powershell -ExecutionPolicy Bypass -File scripts\confluence\get-page.ps1 -PageId '12345678'

# v2 API
powershell -ExecutionPolicy Bypass -File scripts\confluence\get-page.ps1 -PageId '12345678' -Api v2

# Save to file
powershell -ExecutionPolicy Bypass -File scripts\confluence\get-page.ps1 -PageId '12345678' -OutFile page.json
```

### Confluence: CQL Search

```powershell
powershell -ExecutionPolicy Bypass -File scripts\confluence\cql-search.ps1 -Cql 'type=page AND title~"API docs"'
powershell -ExecutionPolicy Bypass -File scripts\confluence\cql-search.ps1 -Cql 'space=MYSPACE AND label="release"' -Limit 25
```

---

## Cursor Components Overview

### Agents

| Agent | Purpose |
|-------|---------|
| **qa-workflow** | **Orchestrator** — sequences multi-agent pipelines (env → cross-dep → TC → quality → report) |
| **environment-resolver** | Resolves target environment (dev/test/prod/...) — never silently defaults |
| **bug-validator** | 5-verdict bug validation: Confluence → Swagger → codebase → verdict. READ-ONLY. |
| **cross-dependency-finder** | Find upstream/downstream dependencies and what could break. Feeds test-case-generator. |
| **test-case-generator** | Generate Backend/Frontend test cases from bugs/tasks with cross-dep data. |
| **test-case-quality-validator** | Score TCs on 6 axes (0–2 each). Pass threshold: 8/12. READ-ONLY. |
| **database-query** | PostgreSQL queries via MCP; environment-aware; connect-first. |
| **shell** | CLI/terminal task delegation. |
| **report-generator** | Save `.md` reports only on `/report`, `/feedback`, or explicit save. |

### Skills

| Skill | Purpose |
|-------|---------|
| **bug-validation** | Structured bug validation workflow (extract → Confluence → Swagger → code → verdict). |
| **cross-dependency-finder** | Cross-dep discovery workflow (scope → codebase + Jira + shallow Confluence → JSON output). |
| **test-case-generator** | TC generation workflow (cross-dep first → Confluence → codebase → DRY preconditions → quality). |
| **database-workflow** | DB connect-first MCP workflow with environment selection. |
| **jira-ticket-analysis** | Full Jira ticket analysis (complete payload + custom fields + attachments + linked Confluence). |

### Commands

| Command | Purpose |
|---------|---------|
| `/test-case-quality <Topic>` | Ad-hoc quality check on existing test case files. Scores 6 axes, 8/12 threshold. |
| `/report` | Save current analysis to disk as markdown. |
| `/feedback` | Save feedback to disk under Feedback area. |

### Rules

| Rule file | Purpose |
|-----------|---------|
| **core_qa_rules.mdc** | "Agents involved" footer, confidence score, English artifacts, chat-first reporting |
| **clarification_and_confidence.mdc** | Stop and ask when scope/environment/target is unclear — never guess |
| **evidence_only_answers.mdc** | Answers must cite code, Confluence, Swagger, or DB — no improvisation |
| **safety_rules.mdc** | Confluence/GitLab read-only, no credential logging, retry discipline |
| **jira_rest_fallback.mdc** | When Jira MCP fails → use REST API v3 with retry/backoff |
| **confluence_rest_fallback.mdc** | When Confluence MCP fails → use REST with wiki base URL resolution |
| **database_workflow.mdc** | Environment mapping (Dev/Test/Prod), connect-first, query best practices |
| **test_cases_structure.mdc** | Backend/Frontend two-folder layout, TC-BE-N / TC-FE-N numbering |

### How Cursor components work

- **Rules** with `alwaysApply: true` are automatically loaded in every chat session. Rules with `alwaysApply: false` are loaded only when matching files are opened (based on `globs` patterns).
- **Agents** are loaded from `.cursor/agents/` and Cursor delegates tasks to them when appropriate.
- **Skills** are loaded from `.cursor/skills/` and provide step-by-step workflows for specific task types.
- **Commands** are loaded from `.cursor/commands/` and respond to slash-command triggers (e.g. `/report`).

---

## Test Cases

The toolkit enforces a structured test case format:

```
test_cases/
├── Backend/
│   ├── User_registration.md      # TC-BE-1, TC-BE-2, ...
│   └── Order_cancellation.md
├── Frontend/
│   ├── User_registration.md      # TC-FE-1, TC-FE-2, ...
│   └── Order_cancellation.md
└── README.md
```

**Key rules:**
- Each topic → two files (Backend + Frontend), same name
- Each file → at least one Positive and one Negative test case
- Preconditions must describe HOW to create entities (not just "entity X exists")
- DRY: shared setup in `## Test data`, per-TC only lists deltas

See `templates/Test_case_template.md` for the full template with examples.

---

## Customization Guide

### For your project

1. **Jira custom fields:** Edit `rules/main/evidence_only_answers.mdc` → update the custom field mapping table with your Jira instance's field IDs (use `expand=names` to discover them).
2. **Database schemas:** Edit `rules/integrations/database_workflow.mdc` → replace placeholder SQL with your project's schemas and common queries.
3. **MCP server names:** Edit `rules/integrations/database_workflow.mdc` → update the environment → MCP server mapping table.
4. **Test case data layers:** Edit `templates/Test_case_template.md` → update the "Data layers" table with your project's domain entities.
5. **Confluence site:** If your Confluence is on a different Atlassian site than Jira, set `CONFLUENCE_WIKI_BASE` in `.env`.

---

## Credential Resolution

All scripts resolve credentials in this order:

1. **System environment variables** (highest priority)
2. **`.env` file** in the toolkit root directory

| Variable | Required | Used By | Description |
|----------|----------|---------|-------------|
| `JIRA_EMAIL` | **Yes** | All scripts | Your Atlassian account email |
| `JIRA_API_TOKEN` | **Yes** | All scripts | API token from Atlassian |
| `JIRA_BASE_URL` | **Yes** | Jira scripts | e.g. `https://your-site.atlassian.net` |
| `CONFLUENCE_EMAIL` | No | Confluence scripts | Overrides JIRA_EMAIL |
| `CONFLUENCE_API_TOKEN` | No | Confluence scripts | Overrides JIRA_API_TOKEN |
| `CONFLUENCE_WIKI_BASE` | No | Confluence scripts | e.g. `https://other-site.atlassian.net/wiki` |

---

## Troubleshooting

| Error | Fix |
|-------|-----|
| `JIRA_EMAIL and JIRA_API_TOKEN must be set` | Create `.env` from `.env.example` and fill credentials |
| `401 Unauthorized` | Check email + API token; token may have expired |
| `403 Forbidden` | Account lacks permission for this resource |
| `404 Not Found` | Wrong issue key, page ID, or base URL |
| `Missing wiki base URL` | Set `CONFLUENCE_WIKI_BASE` or `CONFLUENCE_URL` in `.env` |
| `Execution policy` error | Run with `powershell -ExecutionPolicy Bypass -File ...` |
| Rules not loading in Cursor | Ensure `.mdc` files are in `.cursor/rules/` and match `alwaysApply` / `globs` |

---

## Git Submodules (Project Repositories)

The toolkit includes a `.gitmodules.example` template for linking your project's GitLab/GitHub repositories as git submodules. This lets Cursor AI read and analyze your codebase directly.

### Setup

```powershell
# 1. Copy template to your project root
Copy-Item "path\to\qa-cursor-toolkit\.gitmodules.example" "your-project-root\.gitmodules"

# 2. Edit .gitmodules — replace placeholder URLs with your actual repos
notepad .gitmodules

# 3. Initialize submodules
cd your-project-root
git submodule init
git submodule update
```

### Structure

Organize repos by layer:

```
your-project/
├── .gitmodules
├── Backend/                  # Backend service repos (git submodules)
│   ├── service-core/
│   ├── service-api/
│   └── service-scheduler/
├── Frontend/                 # Frontend repos (git submodules)
│   └── webapp/
├── TestAutomation/           # Test automation repo (git submodule)
│   └── e2e-tests/
└── .cursor/                  # Cursor config (from this toolkit)
    ├── agents/
    ├── skills/
    ├── commands/
    └── rules/
```

### Adding more repos

```bash
git submodule add https://gitlab.your-company.com/project/new-service.git Backend/new-service
```

### Tips

- **Branch tracking:** `git submodule update --remote` pulls the latest from tracked branches.
- **Cursor reads these:** Agents like bug-validator, cross-dependency-finder, and test-case-generator will search and analyze code in submodule directories.
- **Read-only:** Cursor AI will only read these repos, never push to them (enforced by safety rules).

---

## Security

- **NEVER commit `.env`** to version control
- **NEVER share your API token** — each person creates their own
- All operations are **READ-ONLY** — scripts cannot modify Jira or Confluence
- API tokens can be revoked at [Atlassian settings](https://id.atlassian.com/manage-profile/security/api-tokens)
- Database credentials stay in Cursor MCP config, not in workspace files
