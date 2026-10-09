# Start Already Created Billing Run

**Domain:** Billing / Invoicing
**Source:** Start Already created billing run.drawio.svg
**Extracted:** 2026-06-30

## Overview

| Metric | Count |
|--------|-------|
| Decision Points | 0 |
| User Actions | 0 |
| Process Steps | 10 |
| Save Operations | 0 |
| Error States | 0 |

## Process Steps

- X
- System should start billing run process
- System should check what is the status for the selected incompatible processes seperately
- System should check if there are any Incompatible Processes selected in the Process Periodicity object
- System should wait until the process is completed and shouldn't start billing run
- Process should be continued
- After relevant one-time billing runs are selected, system should check respective Process Periodicity object on which bases one-time billing run is created - check if Start After Process section is defined
- JOB should run every minute to check which one-time billing run should be started
- Process should be stopped
- System should check if the selected process exists (is created according to the selected periodic billing run or not for current day) or not

## Flow Connections

Direct relationships between steps:

- Process should be continued --> System should check if there are any Incompatible Processes selected in the Process Periodicity object
- Process should be stopped --> X
- System should wait until the process is completed and shouldn't start billing run --> X
- System should start billing run process --> X

---

## How to Use This for Test Cases

1. **Happy Path**: Follow decisions with 'Yes' conditions to Save Operations
2. **Alternative Paths**: Follow 'No' branches to see different outcomes
3. **Error Scenarios**: Check Error States section for exception cases
4. **Preconditions**: User Actions show what triggers this process

*This is supplementary evidence - always verify against code and Confluence.*

