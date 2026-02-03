---
name: production-data-reader
description: Routes production database queries through ProductionDataReaderAgent, analyzes ANY production data (liability, receivable, payment, deposit, invoice, contract, etc.), and provides step-by-step explanations of data creation. Use when handling ANY production data questions, entity analysis, relationship analysis, or when the user asks "როგორ შეიქმნა" (how was created) for ANY production entity.
---

# Production Data Reader Skill

## When to Use

Use this skill when:
- User asks about ANY production database data (liability, receivable, payment, deposit, invoice, contract, customer, etc.)
- User asks "როგორ შეიქმნა" (how was created) for ANY production entity
- User asks about entity relationships, dependencies, or traceability
- User asks about offsets and their sequence (for any entity type)
- User asks about entity history or reversal history
- User asks "რითია დაოფსეტებული" (what is offset with)
- User asks to query or analyze ANY production database table
- User mentions "Prod გარემო" (Prod environment) with data questions

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

- "Prod გარემოზე მაქვს ლაიაბილითი რომელიც დაოფსეტებულია და მინდა მითხრა რითია დაოფსეტებული და რა თანმიმდევრობით, ლაიაბილითის აიდია - 45319"
- "როგორ შეიქმნა Receivable-11925?"
- "როგორ შეიქმნა Payment-30362?"
- "როგორ შეიქმნა Deposit-12345?"
- "როგორ შეიქმნა Invoice-67890?"
- "როგორ შეიქმნა Contract-11111?"
- "რა თანმიმდევრობით არის ოფსეტები ლაიაბილითი 45319-ისთვის?"
- "რა დატა აქვს Customer-123-ს?"
- "რა კავშირები აქვს Invoice-67890-ს?"

## Security

- **READ-ONLY ONLY:** Always use readonly_user credentials
- **NO MODIFICATIONS:** Never modify production data
- **NO WRITES:** Only SELECT queries allowed

## Integration

- Consults with PhoenixExpert for business logic understanding
- Uses IntegrationService before tasks
- Generates reports via ReportingService
