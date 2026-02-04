---
name: production-data-reader
description: Routes production database queries through ProductionDataReaderAgent, analyzes ANY production data (liability, receivable, payment, deposit, invoice, contract, etc.), and provides step-by-step explanations of data creation. Use when handling ANY production data questions, entity analysis, relationship analysis, or when the user asks how an entity was created for ANY production entity.
---

# Production Data Reader Skill

## When to Use

Use this skill when:
- User asks about ANY production database data (liability, receivable, payment, deposit, invoice, contract, customer, etc.)
- User asks how an entity was created for ANY production entity
- User asks about entity relationships, dependencies, or traceability
- User asks about offsets and their sequence (for any entity type)
- User asks about entity history or reversal history
- User asks what an entity is offset with
- User asks to query or analyze ANY production database table
- User mentions Prod environment with data questions

## Workflow

1. **Route to ProductionDataReaderAgent:**
   ```python
   from agents.Main import get_production_data_reader_agent
   agent = get_production_data_reader_agent()
   ```

2. **Connect to Production Database:**
   - Use PostgreSQLProd MCP server
   - Connect with readonly_user credentials
   - Host: 10.236.20.78, Port: 5000

3. **Analyze Entity:**
   - Parse entity ID and type from query
   - Use universal method: `analyze_entity(entity_type, entity_id)`
   - Supports: liability, receivable, payment, deposit, invoice, contract, and any other entity type
   - For generic queries: Use `query_table(table_name, schema, filters)`
   - For relationships: Use `analyze_relationships(entity_type, entity_id)`

4. **Generate Report:**
   - Format analysis as markdown report
   - Include step-by-step explanation
   - Save report following Rule 0.6

## Example Queries

- "On Prod I have a liability that is offset and I want to know what it is offset with and in what sequence; liability id is 45319"
- "How was Receivable-11925 created?"
- "How was Payment-30362 created?"
- "How was Deposit-12345 created?"
- "How was Invoice-67890 created?"
- "How was Contract-11111 created?"
- "In what sequence are the offsets for liability 45319?"
- "What data does Customer-123 have?"
- "What relationships does Invoice-67890 have?"

## Security

- **READ-ONLY ONLY:** Always use readonly_user credentials
- **NO MODIFICATIONS:** Never modify production data
- **NO WRITES:** Only SELECT queries allowed

## Integration

- Consults with PhoenixExpert for business logic understanding
- Uses IntegrationService before tasks
- Generates reports via ReportingService
