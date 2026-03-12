# POD (Point of Delivery) – Entity-based test cases

This folder contains test cases **grouped by the POD entity** (Point of Delivery).

| File | Content |
|------|--------|
| **Update_name.md** | PUT /api/pod/{id} – update only the name of an existing POD on the current version (no new version). Covers valid request, validation errors (blank name, name length, versionId), 404 (POD or version not found), and regression (GET returns updated name). |

Source: Confluence [Put Update existing POD](https://asterbit.atlassian.net/wiki/spaces/Phoenix/pages/740229121); technical user story from #ai-report (Ani Giorganashvili).
