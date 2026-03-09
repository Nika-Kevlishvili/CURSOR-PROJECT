# NT-1 – Agent questions (after HandsOff report)

**Jira:** NT-1  
**Date:** 2026-03-09

---

[CrossDependencyFinder]: Should cancellation be tested when the payment package is explicitly LOCKED (negative case), or is the current UNLOCKED happy-path regression sufficient for this ticket?

[TestCaseGenerator]: Should we add a test case for "invoice cancellation when payment package is LOCKED" (expected: error or specific behaviour) to cover the bug scenario described in NT-1?

[EnergoTSTestAgent]: The spec currently has two tests (create request + payment cancel UNLOCKED). Should a third test be added that locks the payment package and attempts cancellation to assert the error message or 4xx response?
