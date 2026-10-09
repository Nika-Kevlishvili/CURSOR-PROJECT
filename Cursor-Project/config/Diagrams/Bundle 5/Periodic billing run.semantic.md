# Periodic Billing Run

**Domain:** Billing / Invoicing
**Source:** Periodic billing run.drawio.svg
**Extracted:** 2026-06-30

## Overview

| Metric | Count |
|--------|-------|
| Decision Points | 3 |
| User Actions | 0 |
| Process Steps | 11 |
| Save Operations | 5 |
| Error States | 0 |

## Decision Points (Business Logic)

These are the branching points in the process. Each decision leads to different outcomes.

### 1. Check what is the type of Periodic object

- **then** --> (continues...)

### 2. Check what is the selected option

- **then** --> (continues...)

### 3. Check change to settings

- **then** --> (continues...)

## Process Steps

- System should check if one-time billing run is already created according to this Periodic Billing Run
- JOB should run every day at 11:59 p.m.
- Firstly, should check what are the Exclude options in the Process Periodicity object
- X
- For each Periodic Billing Run object system should check each Process Periodicity object connected to it
- Next working day
- process should be stopped
- If it is One-time
- To check if in process periodic object tomorrow is defined in the configuration
- Previous working day
- If it is Periodical

## Save Operations (Outcomes)

These are the possible end states where data is persisted:

- System should create one-time billing run for tomorrow
- Shouldn't create billing run
- system should create one-time billing run
- If Exclude options are not selected and it should be created for tomorrow, then system should create one-time billing run
- System should ignore such Process Periodicity object and shouldn't create one-time billing run

## Flow Connections

Direct relationships between steps:

- JOB should run every day at 11:59 p.m. --> For each Periodic Billing Run object system should check each Process Periodicity object connected to it
- For each Periodic Billing Run object system should check each Process Periodicity object connected to it --> System should check if one-time billing run is already created according to this Periodic Billing Run
- If it is One-time --> Check what is the selected option
- System should ignore such Process Periodicity object and shouldn't create one-time billing run --> X
- System should create one-time billing run for tomorrow --> X
- If Exclude options are not selected and it should be created for tomorrow, then system should create one-time billing run --> X
- process should be stopped --> X

---

## How to Use This for Test Cases

1. **Happy Path**: Follow decisions with 'Yes' conditions to Save Operations
2. **Alternative Paths**: Follow 'No' branches to see different outcomes
3. **Error Scenarios**: Check Error States section for exception cases
4. **Preconditions**: User Actions show what triggers this process

*This is supplementary evidence - always verify against code and Confluence.*

