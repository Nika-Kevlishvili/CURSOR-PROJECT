---
name: test-case-generator
description: Generate Backend/Frontend test case markdown (Rule 35).
---

# Test case generator

1. Ensure environment named (TC-ENV-ASK.0).
2. Ensure Backend vs Backend+Frontend answered.
3. Require `cross_dependency_data` from cross-dependency-finder.
4. Use `config/templates/Test_Case_Template.md`.
5. Write `test_cases/Backend/<Topic>.md`; Frontend only if requested.
6. Expected results follow documented behavior; note Findings if code differs.
7. Run quality validator next.
