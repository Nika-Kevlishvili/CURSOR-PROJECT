# Rules Loading System - CRITICAL

## Overview

This system ensures that ALL rules from `.cursor/rules/` directory are ALWAYS read and applied BEFORE any response or action is taken.

## Problem Solved

Previously, Cursor AI could respond to queries without first reading the rules, leading to violations of critical constraints (e.g., attempting to modify Confluence when it's READ-ONLY).

## Solution

The `RulesLoader` service automatically loads all rules at the start of every conversation, ensuring they are always applied.

## Rule 0.0: MANDATORY Rules Reading

**ABSOLUTE FIRST PRIORITY**: Before responding to ANY query, performing ANY action, or making ANY tool call, Cursor AI MUST FIRST read ALL rules from `.cursor/rules/` directory.

### Workflow

1. **FIRST**: Call `load_rules_at_start()` function
2. **SECOND**: Read and understand ALL rules
3. **THEN**: Proceed with response/action

### Implementation

```python
from agents.rules_loader import load_rules_at_start

# At the START of EVERY conversation/response:
rules_content = load_rules_at_start()

# Now proceed with understanding rules and responding
```

## How It Works

1. **RulesLoader** scans `.cursor/rules/` directory for `.mdc` files
2. Checks each file for `alwaysApply: true` in frontmatter
3. Loads all rules, prioritizing `alwaysApply: true` rules
4. Returns combined rules content for AI to read

## Rule Files

- Files with `alwaysApply: true` are automatically applied in EVERY session
- Files without `alwaysApply: true` are loaded but may be applied conditionally

## Current Rules

- `phoenix.mdc` - Contains all Phoenix project rules with `alwaysApply: true`

## Critical Rules Summary

From `phoenix.mdc`:

1. **Rule 0.0**: MUST read rules FIRST before any action
2. **Rule 1**: Confluence and GitLab are READ-ONLY (no modifications)
3. **Rule 0.2**: Phoenix questions MUST be answered by PhoenixExpert
4. **Rule 0.6**: MUST generate reports after every task

## Usage Example

```python
# CORRECT workflow:
from agents.rules_loader import load_rules_at_start

# Step 1: Load rules FIRST
rules = load_rules_at_start()

# Step 2: Understand rules
# (AI reads rules_content and applies constraints)

# Step 3: Now respond/act according to rules
# ... rest of the code
```

## Violation Consequences

Violating Rule 0.0 (not reading rules first) is a **CRITICAL SYSTEM ERROR** because:
- AI operates without proper constraints
- Can violate security rules (e.g., modifying READ-ONLY systems)
- Can bypass mandatory workflows (e.g., PhoenixExpert consultation)
- Can skip required reporting

## Testing

To verify rules are loaded:

```python
from agents.rules_loader import get_rules_loader

loader = get_rules_loader()
summary = loader.get_rules_summary()
print(summary)
```

## Maintenance

- Add new rule files to `.cursor/rules/` directory
- Set `alwaysApply: true` in frontmatter for rules that must always apply
- Rules are automatically discovered and loaded

