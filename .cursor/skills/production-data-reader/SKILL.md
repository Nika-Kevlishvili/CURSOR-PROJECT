---
name: production-data-reader
description: Production database analysis via PostgreSQLProd MCP (readonly): liabilities, receivables, payments, contracts, offsets, traceability. Use for ANY production data questions (Rule PDR.0).
---

# Production Data Reader Skill

## When to Use

- User asks about **production** DB data, offsets, how an entity was created, relationships, or traceability.
- User mentions Prod with data questions (liability, receivable, payment, deposit, invoice, contract, customer, etc.).

## Workflow (Cursor — no Python agent)

1. **Role:** ProductionDataReader / **Rule PDR.0** (`.cursor/rules/integrations/production_data_reader.mdc`).
2. **Connect:** PostgreSQLProd MCP — **`mcp_PostgreSQLProd_connect_db`** per MCP schema (**Rule DB.1**); READ-ONLY only.
3. **Parse entity** — extract ID and type from user query.
4. **Query:** SELECT-only via MCP `query`; traverse relationships, offset tables (`customer_liabilitie_paid_by_*`, `customer_payment_*`, etc.); build chronology manually (no Python helpers in this workspace).
5. **Analyze** — event sequence, reversals, dependencies.
6. **Report** — chat-first; file under **Chat reports** per **`Cursor-Project/reports/README.md`** only if user asks (Rule 0.6).

## Output format

Provide for the entity type in scope:

- **Entity details:** ID, number, amounts, dates, currency, status
- **Relationships:** linked entities and connections
- **Event sequence:** chronological offsets, reversals, modifications (ACTIVE/REVERSED)
- **Step-by-step explanation:** how entity was created and modified
- **Reversal history** when applicable
- **Dependencies / dependents**

## Example queries

- "On Prod liability id 45319 — offset sequence and what it is offset with"
- "How was Receivable-11925 / Payment-30362 / Invoice-67890 created?"
- "What relationships does Contract-11111 have?"

## Security

- READ-ONLY; never write to production. Never log credentials.

## Integration

- May consult **PhoenixExpert** for business logic.
- **Rule 0.3 / 0.6:** no Python IntegrationService or ReportingService — MCP + chat; disk only on request.

## Confidence Score (Rule CONF.1 — Three-Zone) [MANDATORY]

Final output: `**Confidence: XX% (ZONE)**` with evidence factors. Compute from evidence: base 40, +points for each evidence source gathered, -points for gaps. Zones: **GO** (≥ 85%), **CAUTION** (55–84% + assumptions + verify list), **STOP** (< 55% — do not deliver conclusions, ask user). See `.cursor/rules/scoring/confidence_scoring_matrix.mdc`.

## Footer

`Agents involved: ProductionDataReaderAgent`
