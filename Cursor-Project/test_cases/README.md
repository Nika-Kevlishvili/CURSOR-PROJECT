# Test Cases

Test case files are organized into **two sub-folders** by testing layer:

- **`Backend/`** — Backend (API) test cases (`TC-BE-N`). One `.md` file per topic.
- **`Frontend/`** — Frontend (UI) test cases (`TC-FE-N`). One `.md` file per topic.

Each topic produces two files with the same name — one in each folder.

## Template

Every `.md` file MUST follow **`Cursor-Project/config/template/Test_case_template.md`**.

## Index

| Topic | Backend | Frontend | Jira |
|-------|---------|----------|------|
| Zero-amount prevention across all liability and receivable generation flows | `Backend/Zero_amount_liability_receivable.md` | `Frontend/Zero_amount_liability_receivable.md` | PDT-2474 |

## Layout reference

See `.cursor/rules/workspace/test_cases_structure.mdc` for the full two-folder layout rules.
