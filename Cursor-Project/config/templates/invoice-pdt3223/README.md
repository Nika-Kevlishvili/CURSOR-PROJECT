# PDT-3223 invoice document template — ListPC.PC tag

## Correct Templater tag

```text
[[DD.TablePCScales.ListPC.PC]]
```

This maps to invoice document JSON path `DD[].TablePCScales[].ListPC[].PC`  
(`invoice_and_template_text` of the price component). Related examples from Phoenix formatters:

- `[[DD.TablePCScales.ListPC.Value]]`
- `[[DD!SumAdd(TablePCProfiles.Price, TablePCScales.ListPC.Value)]]`

## Why a fix was needed

Dev template `Шаблон_Фактура_Бъг.docx` had the tag **split across 3 Word runs**:
`[[` + `DD.TablePCScales.ListPC.PC` + `]][[`.  
NGS Templater needs a contiguous tag. The fixed file merges it into one `w:t`:
`[[DD.TablePCScales.ListPC.PC]]`.

Volume-sum template already had the tag in one run; copied as-is.

## Files

| File | Source |
|------|--------|
| `PDT-3223-invoice-ListPC-PC.docx` | Fixed from Dev volume-sum invoice template (file id 2422) |
| `PDT-3223-invoice-ListPC-PC-from-bug.docx` | Fixed from Dev bug template `Шаблон_Фактура_Бъг` (file id 2428) |
| `fix_listpc_tag.py` | Rebuild script |

## How to use on Dev

1. Portal → Templates → upload one of the fixed DOCX files as **INVOICE / DOCUMENT**.
2. Assign that template to the product as `INVOICE_TEMPLATE`.
3. After billing, check generated document / `generate-invoice-data` for `ListPC.PC` order (Name ASC, then Id ASC).
