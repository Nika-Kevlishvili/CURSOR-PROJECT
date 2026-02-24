# Test Cases – Hierarchical Output Format (Human-Readable)

## Purpose

Generated test cases MUST be **maximally understandable for humans** and saved in a **hierarchical folder structure** so that navigation and intent are clear at a glance.

## Root folder

- **Path:** `Cursor-Project/generated_test_cases/`
- All generated test-case trees live under this folder. Use this folder (not the flat `test_cases/` legacy path) for the new format.

## Top-level categories

Two main branches (extend only if the project clearly needs more):

| Category | Meaning | Examples under it |
|----------|---------|--------------------|
| **Object** | Domain entities / objects and actions on them | customer, contract, POD, invoice, … |
| **Flows** | Business or technical flows and their variants | Billing, Payment, Registration, … |

## Structure rules

1. **Folders = hierarchy levels.** Use folders only; no category names as files until the leaf level.
2. **Leaf level = test case files.** At the leaf of the tree, use one `.md` file per “bucket” of test cases (e.g. one file per action or per subflow variant). File name = human-readable label (e.g. `Create.md`, `Edit.md`, `Standard.md`, `For_volumes.md`, `Profile.md`). Use underscores for multi-word names (e.g. `For_volumes`, `Scale`).
3. **Object branch:** `Object / <entity> / <action or sub-area> / … / <leaf>.md`  
   - Entity: customer, contract, POD, etc.  
   - Then actions or sub-areas: Create, Edit, Delete, View, Validation, etc.  
   - Deeper levels only if needed (e.g. Object/customer/Edit/Identifier_validation.md).
4. **Flows branch:** `Flows / <flow name> / <variant or subflow> / … / <leaf>.md`  
   - Flow: Billing, Payment, etc.  
   - Then variants: Standard, interim, etc.  
   - Then subflows: e.g. For_volumes → scale, Profile, etc.  
   - Deeper levels as needed; leaf file = one coherent set of test cases (e.g. one file for “Profile”, one for “scale” under For_volumes).
5. **Human-readable names:** Prefer clear English words (Create, Edit, Billing, Standard, For_volumes, Profile, interim). No opaque IDs in folder or file names.

## Example tree (reference)

```text
Cursor-Project/generated_test_cases/
  Object/
    customer/
      Create.md
      Edit.md
      Delete.md
      View.md
      Validation.md
    contract/
      Create.md
      Edit.md
      ...
    ...
  Flows/
    Billing/
      Standard/
        For_volumes/
          scale.md
          Profile.md
        interim.md
        ...
      ...
    Payment/
      ...
    ...
```

## Content of leaf files (e.g. `Create.md`, `Profile.md`)

- Each leaf `.md` file contains **one or more test cases** for that bucket.
- Format inside the file: **clear, human-oriented** (short title, steps, expected result, optional preconditions).
- Prefer:
  - Short descriptive titles.
  - Numbered steps.
  - One “Expected result” per case.
  - Optional: “Preconditions”, “Data”, “Confluence/Code refs” if useful.

Example for `Object/customer/Create.md`:

```markdown
# Customer – Create

## TC-1: Create customer with valid identifier
- **Steps:** 1. Open customer form. 2. Enter valid identifier. 3. Save.
- **Expected:** Customer is created and visible in list.

## TC-2: Create customer – identifier too long
- **Steps:** 1. Open customer form. 2. Enter identifier longer than max length. 3. Save.
- **Expected:** Validation error; customer not created.
```

## Mapping from generator output to this structure

- **Entities / resources** (customer, contract, POD, …) → under `Object/<entity>/`.
- **Actions** (Create, Edit, Delete, View, …) → next level (folder or leaf file name).
- **Business flows** (Billing, Payment, …) → under `Flows/<flow>/`.
- **Flow variants** (Standard, interim, For_volumes, …) → next levels until a clear bucket; then one leaf `.md` per bucket (e.g. `Profile.md`, `scale.md`).
- **Regression / impact** cases (from cross-dependency “what_could_break”) → place under the most relevant Object or Flow path; add a leaf file or section (e.g. `Regression.md` or a dedicated subfolder) so humans can find them easily.

## Summary

- **Root:** `Cursor-Project/generated_test_cases/`
- **Top levels:** `Object/`, `Flows/`
- **Then:** entity or flow → action or variant → … → **leaf `.md`** with human-readable test cases.
- **Goal:** Maximally understandable for humans; structure is the same idea you described (Object → customer → Create/Edit/…; Flows → Billing → Standard → For_volumes → scale/Profile, interim, …).
