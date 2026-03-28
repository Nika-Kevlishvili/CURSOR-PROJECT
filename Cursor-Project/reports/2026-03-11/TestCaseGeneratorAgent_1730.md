# Test Case Generator Agent – Report

**Date:** 2026-03-11  
**Task:** Generate test cases for Put / Update existing POD (name only) – technical user story from #ai-report (Ani Giorganashvili).

## Scope

- **Source:** Confluence [Put Update existing POD](https://asterbit.atlassian.net/wiki/spaces/Phoenix/pages/740229121); Slack #ai-report messages and thread (Ani Giorganashvili).
- **Endpoint:** PUT /api/pod/{id} – update only the name of an existing POD on the current version (no new version).
- **Cross-dependency:** Run completed; output in `Cursor-Project/cross_dependencies/2026-03-11_put-update-pod-name-only.json`.

## Output

| Path | Description |
|------|-------------|
| **Cursor-Project/test_cases/Objects/POD/Update_name.md** | Full test case document: 10 TCs (positive and negative), following Test_case_template.md. |
| **Cursor-Project/test_cases/Objects/POD/README.md** | Folder README for POD entity. |
| **Cursor-Project/test_cases/Objects/README.md** | Updated to include POD in the entity table. |

## Test cases created (summary)

- **TC-1 (Positive):** Valid POD id, versionId, name → 200; name updated in DB.
- **TC-2 (Negative):** Name blank → 400 (name-Name can not be blank;).
- **TC-3 (Negative):** Name length > 1024 → 400 (name-Name size should be between 1 and 1024;).
- **TC-4 (Negative):** versionId missing → 400 (versionId-Version ID is mandatory;).
- **TC-5 (Negative):** versionId invalid (0 or negative) → 400 (versionId-Version ID must be at least 1;).
- **TC-6 (Negative):** POD id does not exist → 404.
- **TC-7 (Negative):** versionId does not exist for given POD → 404.
- **TC-8 (Positive):** Update does not create new version – only current version name changed.
- **TC-9 (Positive):** GET /pod/{id} returns updated name after PUT (regression from cross-deps).
- **TC-10 (Negative):** Invalid path id format → 400 or 404.

## Workflow applied

1. **Cross-dependency-finder** run first (Rule 35); no Jira key – scope: Put Update existing POD (name only); Confluence and entry points/downstream/what_could_break captured.
2. **Test case generation** used cross_dependency_data, Confluence content, and acceptance cases from Ani’s messages (including thread Part 2/2).
3. **Save location:** test_cases/Objects/POD/ per test_cases_structure.mdc (entity = POD, action = Update name).
4. **Template:** Structure and wording follow Cursor-Project/config/template/Test_case_template.md; all content in English.

## Agents involved

TestCaseGeneratorAgent, CrossDependencyFinderAgent.
