# Bug Validation

Validate bug reports using BugFinderAgent (Rule 32 - MANDATORY workflow).

## ALWAYS Use BugFinderAgent:
ALL bug validation requests MUST be handled by BugFinderAgent - NO EXCEPTIONS.

## Mandatory 5-Step Workflow:

### Step 1: Extract Expected Behavior
- Extract bug's expected result from ticket description  
- Identify specific behavior claimed by bug reporter
- Document expected behavior clearly

### Step 2: Confluence Validation (Evidence Strength)
- Search Confluence using MCP tools FIRST
- Assess evidence strength: exact match / contextual match / no match / contradicts / search failed
- Report: "Confluence validation: [evidence strength] - [explanation]"

### Step 3: Code Validation (Behavior Analysis)
- Search codebase using code search tools
- Analyze actual implementation behavior
- Check if code matches faulty behavior described in bug
- Report: "Code validation: [matches reported behavior/does not match/could not verify] - [explanation]"

### Step 4: Apply 5-Verdict Decision Matrix
- Use evidence + behavior to determine verdict
- Apply one of 5 verdicts: VALID / NEEDS CLARIFICATION / NEEDS APPROVAL / NOT VALID / INSUFFICIENT EVIDENCE

### Step 5: Deliver verdict (chat; file only on request)
- Combine all findings with clear verdict and reasoning in the **chat reply** (full structured markdown).
- Include actionable next steps based on verdict.
- **Disk:** Save `BugValidation_[DescriptiveName].md` under **Chat reports** only if the user runs **`/report`** or explicitly asks to save; otherwise no file (Rule 0.6).

## Response Structure:

```markdown
## Bug Validation Analysis

### 1. Expected Behavior
**Bug Claims:** [What the bug report says should happen]
**Context:** [Relevant business context or user scenario]

### 2. Confluence Validation  
**Evidence Strength:** [exact match / contextual match / no match / contradicts / search failed]
**Explanation:** [Detailed explanation of Confluence findings]
**Sources:** [List of Confluence pages found with page IDs, titles, URLs]

### 3. Code Analysis
**Behavior Match:** [matches reported behavior / does not match reported behavior / could not verify]
**Explanation:** [Detailed explanation of actual code behavior]
**Code References:**
- File: [path/to/file.java]
- Lines: [123-145]
- Implementation: [Description of what code actually does]

### 4. Final Verdict
**Verdict:** [VALID / NEEDS CLARIFICATION / NEEDS APPROVAL / NOT VALID / INSUFFICIENT EVIDENCE]
**Reasoning:** [Why this verdict was chosen based on evidence matrix]
**Next Steps:** [What should be done next based on verdict]
```

## Workflow Requirements:
- **Rule 0.3** — No Python `IntegrationService` here; follow MCP/Jira when needed
- Consult PhoenixExpert for context
- Use READ-ONLY mode (no code modifications)
- Do **not** save a BugValidation file unless the user runs **`/report`** or explicitly requests a saved report under **Chat reports** (per **`Cursor-Project/reports/README.md`**). No Summary/agent files unless explicitly requested.

## 5-Verdict Decision Matrix:

- **VALID**: Exact Confluence match + code confirms reported faulty behavior → Fix the bug
- **NEEDS CLARIFICATION**: Contextual Confluence match + code confirms reported behavior → Get product clarification  
- **NEEDS APPROVAL**: No Confluence match + code confirms reported behavior → Get product approval
- **NOT VALID**: Confluence contradicts expected behavior + code follows Confluence → Close as "working as designed"
- **INSUFFICIENT EVIDENCE**: Cannot access Confluence/code or evidence too weak → Resolve technical issues

## Response Must End With:
"Agents involved: BugFinderAgent, PhoenixExpert"
