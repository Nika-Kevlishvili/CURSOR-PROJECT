## Summary Report - 2026-02-10 15:00

### Task Overview

- **User request:** Clarify what the "restriction" ("რესთრიქშენი") in Phoenix price components means and how to use it within billings.
- **Context:** Question was asked in Georgian; answer had to be conceptually clear and technically precise for Phoenix billing configuration.

### Agents and Roles

- **PhoenixExpert**
  - Interpreted the user’s question and identified that it concerns Phoenix pricing configuration and billing behavior.
  - Provided both **business-level** and **technical** explanations of restrictions on price components.

### Key Points Explained

- **Concept of restriction in price components:**
  - A restriction is a rule that limits how much of a given price component can be applied in a billing period.
  - Two main types were clarified:
    - **Volume-based restrictions** (limit in units such as kWh).
    - **Value-based restrictions** (limit in monetary amount, e.g., GEL).

- **Technical modeling in Phoenix:**
  - Restrictions are stored on the application model alongside price components.
  - Configuration fields include:
    - Restriction **type** (volume vs value).
    - Measurement **unit** (kWh, percentage, CCY).
    - Optional **ranges** (e.g., kWh intervals) and **caps** (max CCY).
  - The billing run:
    - Loads components and restrictions.
    - Computes quantities and monetary amounts.
    - Enforces configured caps before finalizing billing line amounts.

- **Concrete configuration examples:**
  - **Night tariff capped at 300 kWh:**
    - Volume-based restriction; night tariff applied only for first 300 kWh, rest billed by other components.
  - **Subsidy capped at 50 GEL:**
    - Value-based restriction; subsidy calculated by formula but capped at 50 GEL per period.
  - Indicated patterns for combining percentage-based calculations with volume and/or value caps.

### Outcome

- The user now has:
  - A clear mental model of how restrictions work in Phoenix price components.
  - Practical examples they can mirror when configuring capped tariffs and subsidies.
  - An understanding of how billing runs interpret and apply these restrictions so that configured caps are respected on generated bills.

