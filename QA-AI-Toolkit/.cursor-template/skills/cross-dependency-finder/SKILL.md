---
name: cross-dependency-finder
description: Map cross-dependencies for a scope before test cases.
---

# Cross-dependency finder

1. Load ticket/scope (Jira MCP → REST).
2. Deep Confluence exploration for related pages.
3. Search code under `codeRoot` for entry points, callers, consumers.
4. Output structured: upstream, downstream, what_could_break, Findings if code↔doc conflict.
5. Hand off to test-case-generator.
