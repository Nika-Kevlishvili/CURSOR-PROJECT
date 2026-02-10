## PhoenixExpert Agent Report - 2026-02-10 15:00

### Task Context

- **User language:** Georgian
- **Topic:** Meaning and usage of "restriction" ("რესთრიქშენი") in Phoenix price components and how it affects billing.
- **Goal:** Explain conceptually and technically how restrictions work, how they are modeled, and how billing run applies them, with concrete configuration examples.

### User Question (Reconstructed)

The user asked what the "restriction" in Phoenix price components means (specifically in the context of tariffs/price components) and how to use it in billings, including practical configuration scenarios.

### PhoenixExpert Actions

- **Conceptual explanation:**
  - Clarified that a *restriction* on a price component is a business rule that limits how much of that component can be applied in a billing period.
  - Distinguished between **volume-based** restrictions (e.g., capped kWh) and **value-based** restrictions (e.g., capped CCY/GEL amount).
  - Explained how restrictions interact with other tariff elements (base prices, time-of-use components, and subsidies).
- **Technical explanation:**
  - Described how restrictions are represented on the Phoenix application model for price components:
    - Fields storing:
      - Restriction **type** (volume-based vs value-based).
      - Restriction **unit** (e.g., kWh, percentage, CCY).
      - Optional **ranges** (e.g., kWh intervals) that a restriction applies to.
      - Optional **percentage** fields for proportional caps.
      - Optional **absolute caps** in CCY.
  - Explained how the billing run reads these fields for each price component and applies them:
    - **Volume-based:** billing accumulates consumed quantity (e.g., kWh) and applies the component only up to the restricted volume range.
    - **Value-based:** billing accumulates calculated monetary value and caps it when the configured maximum CCY is reached.
    - **Percentage-based:** billing may compute the component as a percentage of a base amount but still respect any configured max value or volume cap.
- **Billing flow description:**
  - Outlined, step by step, how the billing run:
    1. Loads price components for the contract/POD and period.
    2. For each component, reads its restriction configuration from the model.
    3. Aggregates consumption and/or monetary value for that component.
    4. Applies the restriction logic (volume limit, value limit, or both) before finalizing the line amount.
    5. Produces billing lines that already respect all configured restrictions.

### Configuration Examples Given

- **Example 1: Night tariff capped at 300 kWh**
  - Business requirement: Apply a discounted night tariff only up to **300 kWh** per billing period; any additional night consumption should be billed by another component (e.g., base tariff).
  - PhoenixExpert described a configuration where:
    - The night tariff price component has:
      - Restriction **type:** volume-based.
      - **Unit:** kWh.
      - **Range:** 0–300 kWh (or an equivalent max-volume parameter).
    - During billing:
      - System calculates night consumption.
      - Applies the night tariff price only up to 300 kWh.
      - Remaining kWh (if any) are **not** priced by this component and can be priced by another component.

- **Example 2: Subsidy capped at 50 GEL**
  - Business requirement: Provide a subsidy (discount) on the bill but cap the subsidy total at **50 GEL** per billing period.
  - PhoenixExpert described a configuration where:
    - The subsidy price component has:
      - Restriction **type:** value-based.
      - **Unit:** CCY (e.g., GEL).
      - **Max value:** 50 GEL.
    - During billing:
      - System computes the subsidy as per the component formula (possibly a percentage of some base charge).
      - If computed subsidy exceeds 50 GEL, the applied subsidy is capped at 50 GEL.

- **Example 3 (implicit): Percentage + range**
  - PhoenixExpert indicated that some components can use **percentage-based** calculations within a restricted range:
    - For instance, a discount equal to 20% of energy charge but only for the first N kWh or up to a fixed CCY cap.
  - Billing run:
    - First computes the base amount (e.g., energy charge).
    - Applies the relevant percentage.
    - Then enforces configured volume and/or value caps.

### How to Use Restrictions in Billing Configuration

- **Volume-based restriction usage:**
  - Use when business rule is "apply this price only for the first X units" (e.g., *first 300 kWh at discount*, *first 100 kWh free*).
  - Configure:
    - Type: volume-based.
    - Quantity unit (kWh or another unit relevant to the price component).
    - Volume range or maximum volume.

- **Value-based restriction usage:**
  - Use when business rule is "do not exceed Y CCY for this component" (e.g., *subsidy not more than 50 GEL*).
  - Configure:
    - Type: value-based.
    - Currency and max amount (e.g., 50 GEL).

- **Combined/percentage restrictions:**
  - Use when component is defined as a percentage of another charge but must still be limited:
    - Configure calculation as percentage.
    - Add a value cap (CCY) and/or a volume range if the rule requires.

### Outcome

- The user now has:
  - A conceptual understanding of what *restriction* means in Phoenix price components.
  - A technical view of how restrictions are stored on the model and evaluated during billing runs.
  - Concrete examples (night tariff cap and subsidy cap) they can use as templates for real configurations.

