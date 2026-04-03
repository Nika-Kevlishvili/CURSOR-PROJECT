# PhoenixExpert Report — 2026-04-03 16:05

## Request

User asked (Georgian): what **Prefix** should be for the **self-invoiced** invoice type per **Confluence**.

## Confluence (MCP)

- `searchAtlassian` was used with multiple queries; all results returned `metadata.searchScope: "jira-only"`.
- `getAccessibleAtlassianResources` shows scopes: `read:jira-work`, `write:jira-work` — **no Confluence read scope** on the connected token.
- **Confluence pages could not be retrieved** for this question via the available integration.

## Phoenix codebase (read-only) — `InvoiceType` technical prefixes

Source: `phoenix-core-lib/.../InvoiceType.java`

| Enum constant | Prefix string in code |
|---------------|------------------------|
| MANUAL | M |
| STANDARD | S |
| INTERIM_AND_ADVANCE_PAYMENT | INT |
| CORRECTION | C |
| REVERSAL | R |
| RECONNECTION | REC |

**Note:** There is **no** enum value named "self invoiced" / "self-billed" in this `InvoiceType` definition. Mapping a business term from Confluence to `InvoiceType` must be done from official documentation or product analysts.

**Note:** Invoice numbers in Phoenix combine a **nomenclature prefix** (from `Prefix` / `InvoicePrefixService`) with sequence; that is separate from the `InvoiceType` enum’s `prefix` field.

## Conclusion

Answer could not be confirmed from Confluence in this session due to token scope. Code reference above lists all `InvoiceType` prefixes present in Phoenix today.
