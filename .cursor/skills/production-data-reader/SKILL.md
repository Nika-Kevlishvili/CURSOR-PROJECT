---
name: production-data-reader
description: Production database analysis via PostgreSQLProd MCP (readonly): liabilities, receivables, payments, contracts, offsets, traceability. Use for ANY production data questions (Rule PDR.0).
---

# Production Data Reader Skill

## When to Use

- User asks about **production** DB data, offsets, how an entity was created, relationships, or traceability.
- User mentions Prod with data questions.

## Workflow (Cursor — no Python agent)

1. **Role:** ProductionDataReader / follow `.cursor/rules/integrations/production_data_reader.mdc`.
2. **Connect:** PostgreSQLProd MCP with **readonly_user** only (see `database_workflow.mdc` / MCP for connection pattern).
3. **Query:** Run SELECTs via MCP `query`; iterate related tables; build chronology and relationships manually (no `analyze_entity()` helper in this workspace).
4. **Report:** Answer in chat; save markdown under **Chat reports** per **`Cursor-Project/reports/README.md`** only if the user asks for a file (Rule 0.6).

## Security

- READ-ONLY; never write to production.

## Integration

- May consult PhoenixExpert for business logic.
- **Rule 0.3 / 0.6:** no Python IntegrationService or ReportingService — MCP + chat answer; markdown on disk only if the user asks.
