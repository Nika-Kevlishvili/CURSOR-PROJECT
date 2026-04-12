# Production Data Reader Query

Route ALL production database data questions to ProductionDataReaderAgent (Rule PDR.0).

## Mandatory Workflow:

1. **Rule 0.3** — No Python `IntegrationService` here; follow MCP/Jira when needed.
2. **Connect to Production Database** - Use PostgreSQLProd MCP with readonly_user credentials
3. **Parse Entity** - Extract entity ID and type from query (liability, receivable, payment, deposit, invoice, contract, etc.)
4. **Analyze Data** - Use universal method:
   - `analyze_entity(entity_type, entity_id)` for any entity type
   - `query_table(table_name, schema, filters)` for generic table queries
   - `analyze_relationships(entity_type, entity_id)` for relationship analysis
5. **Generate Report** - Format step-by-step explanation

## Database Connection:

- **MCP Server:** PostgreSQLProd
- **Host:** 10.236.20.78
- **Port:** 5000
- **User:** readonly_user
- **Password:** U$CM)*qr&Zb4JfU@
- **Database:** phoenix

## Security:

- **READ-ONLY ONLY:** Always use readonly_user credentials
- **NO MODIFICATIONS:** Never modify production data
- **NO WRITES:** Only SELECT queries allowed

## Response Requirements:

- State "**Expert:** ProductionDataReaderAgent" at beginning
- Provide step-by-step explanation of data creation
- Include offset sequence with dates and amounts
- Include reversal history if applicable
- End with: "Agents involved: ProductionDataReaderAgent"

## Reports (Rule 0.6 — optional):

- **Default:** full analysis in chat only.
- **On request:** save to **Chat reports** per **`Cursor-Project/reports/README.md`** if the user asks for a persisted report.

## Example Queries:

- "On Prod I have a liability that is offset and I want to know what it is offset with and in what sequence; liability id is 45319"
- "How was Receivable-11925 created?"
- "How was Payment-30362 created?"
- "How was Deposit-12345 created?"
- "How was Invoice-67890 created?"
- "How was Contract-11111 created?"
- "In what sequence are the offsets for liability 45319?"
- "What data does Customer-123 have?"
- "What relationships does Invoice-67890 have?"
