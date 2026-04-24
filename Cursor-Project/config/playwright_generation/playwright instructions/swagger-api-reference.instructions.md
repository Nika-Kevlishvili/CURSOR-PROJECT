---
description: "MANDATORY reference for using OpenAPI/Swagger specs when generating Playwright API tests. Ensures payloads, endpoints, field types, and enums match the real API contract."
applyTo: "**/*.spec.ts"
priority: 2
---

# Swagger / OpenAPI Spec Reference (MANDATORY)

## Core Principle

**Always consult the OpenAPI spec before constructing payloads or using endpoints.** The spec is the **single source of truth** for the API contract — field names, enum values, types, and required fields. **NEVER trust test case documents, Jira descriptions, or assumptions for field names/enum values — ALWAYS verify against the refreshed spec.**

## STEP ZERO: Refresh Specs (Rule SWAGGER.0 — NEVER SKIP)

**Before using ANY spec data, you MUST refresh all specs by running:**

```powershell
powershell -ExecutionPolicy Bypass -File ".cursor/commands/update-swagger-specs.ps1"
```

Cached specs become stale as the backend evolves. New endpoints, renamed fields, and changed enum values will NOT appear in cached specs. **Real example of damage caused by skipping this step:**
- Field `title` was used instead of correct `titleId`
- Source value `Sales_Portal` was used instead of correct `SALES_PORTAL`
- Field `birthdate` was used instead of correct `birthDate`
- All of these caused **every test to fail at runtime**

If the refresh fails (network/VPN), warn the user and continue with cached specs. See `.cursor/rules/integrations/swagger_refresh_mandatory.mdc` for the full enforcement checklist.

## Spec Location

Specs are stored per environment under:

```
Cursor-Project/config/swagger/{env}/swagger-spec.json
```

Available environments: `dev`, `test`, `dev2`, `experiment`, `prod`.
Manifest with URLs: `Cursor-Project/config/swagger/environments.json`.

## Environment Selection

1. **Default**: Use `dev` (`config/swagger/dev/swagger-spec.json`).
2. **If the test targets a specific environment** (stated in Jira, user request, or `BASE_URL` config): use the matching env's spec.
3. **If `preprod`**: no spec available yet -- fall back to `dev`.

## How to Search the Spec (Files Are ~1.3 MB)

**NEVER read the entire `swagger-spec.json` file.** It is too large. Use targeted searches instead.

### Finding an Endpoint

Use `Grep` to locate the endpoint path inside the spec:

```
Grep pattern: "/<endpoint-path>" in Cursor-Project/config/swagger/{env}/swagger-spec.json
```

Examples:
- `"/customer"` -- finds customer CRUD endpoints
- `"/billing-run"` -- finds billing run endpoints
- `"/product-contract"` -- finds product contract endpoints
- `"/pod"` -- finds POD endpoints

Once you find the line with the endpoint path, read ~100-200 lines around it to capture the full operation definition (parameters, requestBody `$ref`, responses).

### Resolving Schema References (`$ref`)

OpenAPI specs use `$ref` pointers like `"$ref": "#/components/schemas/CustomerCreateDTO"`. To resolve:

1. Grep for the schema name: `"CustomerCreateDTO"` (or whatever the DTO name is).
2. Read the schema definition to get: `properties`, `required` array, `type`, `enum` values.
3. For nested `$ref` inside properties, repeat the lookup.

### Extracting Enum Values

Grep for the field name or DTO name and look for `"enum"` arrays nearby:

```
Grep pattern: "enum" in the spec file, near the field of interest
```

## What to Extract

For each endpoint involved in a test, extract:

| Data Point | Where in Spec | Use In Test |
|------------|---------------|-------------|
| HTTP method | Key under the path (`get`, `post`, `put`, `delete`) | `Request.post(...)`, `Request.get(...)` |
| Request body schema | `requestBody.content.application/json.schema.$ref` → resolve DTO | Payload construction: field names, types, required fields |
| Required fields | `required` array in the schema | Ensure payload includes all mandatory fields |
| Field types and formats | `type`, `format` (e.g., `string`/`date-time`, `integer`/`int64`) | Correct data types in payload |
| Enum values | `enum` arrays on string properties | Use valid values (e.g., status, type fields) |
| Path parameters | `parameters` with `in: "path"` | URL construction |
| Query parameters | `parameters` with `in: "query"` | Request options |
| Response schema | `responses.200.content.application/json.schema` | Assertion structure (optional, for detailed checks) |

## How to Apply

### Payload Construction

1. Start with the existing `GeneratePayload` method if one exists for the domain.
2. Cross-check the generated payload against the spec schema:
   - Are all `required` fields present?
   - Are field names spelled correctly (camelCase as in spec)?
   - Are enum fields using values from the spec's `enum` array?
   - Are date fields in the correct format (`yyyy-MM-dd`, ISO 8601, etc.)?
   - Are numeric fields using the correct type (`integer` vs `number`)?
3. If the `GeneratePayload` method does not exist for this endpoint, construct the payload manually using the spec schema as the blueprint.

### Endpoint Verification

- Compare the path from the spec with the `Endpoints` constant in `fixtures/constants/endpoints.ts`.
- If the endpoint is not in `Endpoints`, use the path string directly (strip leading `/` per `RequestWrapper` convention).
- If the `Endpoints` constant differs from the spec, trust the spec and use the correct path.

### Negative Test Cases

The spec helps identify what to break for negative tests:
- Omit a `required` field to trigger validation errors.
- Send an invalid enum value.
- Send wrong type (string where integer expected).
- Send null for a non-nullable field.

## When a Deep Spec Dive Is Optional (Refresh Is NEVER Optional)

A deep schema extraction is not required when ALL of these are true:
- The endpoint is well-covered by an existing `GeneratePayload` method.
- The `Endpoints` constant matches the expected path.
- There is no ambiguity about field types, enums, or required fields.
- The test is copying/adapting an existing test that already works.

**However, the spec refresh (Step Zero) is ALWAYS required — even in these cases.** And at minimum, a **quick verification grep** (confirming the endpoint exists and field names match) MUST be done.

## Mandatory Cross-Validation Checklist

Before writing ANY payload in a `.spec.ts` file, verify each of these against the refreshed spec:

| Check | What to Verify | Common Mistakes |
|-------|---------------|-----------------|
| Field names | Exact camelCase from spec | `title` vs `titleId`, `birthdate` vs `birthDate` |
| Required fields | All fields in `required` array present | Missing mandatory fields → 400 errors |
| Enum values | Exact values from spec `enum` array | `Sales_Portal` vs `SALES_PORTAL` |
| Field types | Correct types (number/string/boolean/array) | String where integer expected |
| Date formats | Correct format string | `yyyy-MM-dd` vs ISO 8601 |
| Path parameters | Correct types and constraints | `string` vs `integer` |

## Workflow Summary

```
0. REFRESH specs: run update-swagger-specs.ps1 (NEVER SKIP)
1. Identify endpoints needed for the test
2. Grep refreshed spec for each endpoint path
3. Read the operation definition (~100-200 lines)
4. Resolve request body $ref → read schema
5. Note required fields, types, enums
6. Cross-validate EVERY field name and enum value against the spec
7. Cross-check with GeneratePayload / Endpoints
8. Use ONLY spec-validated data in payload construction and assertions
```
