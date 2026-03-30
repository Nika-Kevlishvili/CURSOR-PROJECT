# Test case generate – Target vs Current timing

**Workflow:** `test-case-generate` (Rule 35 + test-case authoring)  
**Purpose:** Track phase budgets vs observed duration to close gaps.

---

## Target vs Current

| Phase | Target | Current | Gap |
|------|------:|------:|------:|
| Rules and context alignment | 2 m | 3 m | +1 m |
| Cross dependency analysis | 5 m | 7 m | +2 m |
| Test design modeling | 10 m | 12 m | +2 m |
| Authoring markdown test cases | 12 m | 14 m | +2 m |
| File organization and README update | 4 m | 5 m | +1 m |
| Final validation and delivery packaging | 3 m | 3 m | 0 m |
| **Total** | **36 m** | **44 m** | **+8 m** |

---

## Notes

- **Gap focus:** Largest overruns are cross dependency (+2 m), test design (+2 m), and authoring (+2 m); align with parallelizing Jira + merge grep after key is known, and caching `cross_dependencies/*` when SHA unchanged (see `test_generate_timing_PDT-2553.jsonl` recommendations).
- **Update:** Replace **Current** after each run from orchestrator `Stopwatch` or manual log; keep **Target** as SLA unless Leadership changes budgets.

---

## Agents involved

None (documentation only); aligns with TestCaseGeneratorAgent / CrossDependencyFinderAgent measurement goals.
