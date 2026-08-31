# Graph RAG ingest playbook

**Rule:** GRAPH.1 — `.cursor/rules/integrations/graph_rag_ingest.mdc`  
**Query (agents):** GRAPH.0 — `.cursor/rules/integrations/graph_rag_integration.mdc`

This file is the **fill** playbook. Docker start/stop is in `docker/README.md`.

## Target shape (optional layers)

```
phoenix_domain              what the business object means
        | HAS_ENDPOINT
api_and_repo_layout         Swagger endpoints / DTOs / enums
        | COVERS                 (only if test-case nodes exist)
test_cases                  markdown test cases
        | IMPLEMENTS             (only if Playwright nodes exist)
playwright_automation       Playwright spec / fixtures / writing rules
```

`n.zone` must be one of those four strings. Optional `domain` property (e.g. `zip-codes`) is the business tag, not a fifth layer.

A topic is **valid** with only the layers that have sources (often domain + API). Do not create placeholder nodes for missing test cases or Playwright. Add those layers later when markdown TCs or EnergoTS specs exist.

## Fill order

Agents do **not** start this list on their own. GRAPH.0 **Ask before fill**: if the graph is empty, the topic is missing, or nodes are stale / disagree with live code or Confluence, the agent must ask the user with **what / from what / what would be written**, then wait for yes.

1. Start the stack (`docker/scripts/start.ps1`) — Neo4j + MCP + Ollama.
2. Refresh Swagger if the API slice must be current (`update-swagger-specs` / project script).
3. Ingest **API** for the topic (parser on `config/swagger/dev/swagger-spec.json`). Filter by path prefix (e.g. `/zip-codes`) so unrelated customer/POD nodes are not pulled in via DTO edges.
4. Ingest **test cases** only if markdown TCs for the topic exist. Link each TestCase to Endpoint with `COVERS` when the path/operation is known. Skip this step if there are no files.
5. Ingest **Playwright** only if EnergoTS specs for those TCs exist. Link Spec `IMPLEMENTS` TestCase. Skip if there are no specs.
6. Ingest **domain** from linked Confluence or verified Phoenix code. Link Domain `HAS_ENDPOINT` Endpoint.
7. Embed with MiniLM; upsert by stable `uid`; set `source_path` + `source_hash`.
8. Check in Neo4j Browser (`http://localhost:7474`, Bolt `bolt://localhost:7687`, user `neo4j`, password from `docker/.env`).

Do **not** use the local chat LLM (Qwen) as the main extractor. It synthesizes answers at **query** time.

## What not to ingest

- Whole Confluence / whole Phoenix source tree
- All four Swagger environment files as duplicate APIs
- Unverified chat (`graph_update` only after user yes + live evidence; GRAPH.0 Ask before fill)
- Secrets and `.env`

## Zip-codes (worked example)

Scoped API ingest: `ingest/ingest_zip_codes.py` (run inside `graph-rag-mcp` with `PYTHONPATH` / workspace mount).

That script loads **API from Swagger** plus **domain pointers**: ZipCode / PopulatedPlace, customer-details + POD + communications address processes, ZipCodeService / ZipCodeRepository (in-use check), the **global vs per-place default mismatch**, and Confluence URLs for pages **197112**, **845119490**, **845283330**. `graph_query` must surface those `source_path` values so agents open the files and wiki pages. **Test cases** and **Playwright** stay empty until markdown TCs and EnergoTS specs exist. Fake “not ingested yet” markers are not stored. List endpoint `GET /zip-codes` must `ACCEPTS` `ZipCodeFilterRequest` (query `$ref`, not only JSON body).

Neo4j check (API slice):

```cypher
MATCH (n)
WHERE toLower(n.name) CONTAINS 'zip' OR toLower(n.title) CONTAINS 'zip'
OPTIONAL MATCH (n)-[r]-(m)
WHERE toLower(m.name) CONTAINS 'zip' OR toLower(m.title) CONTAINS 'zip'
RETURN n, r, m
```

Four-layer check (when layers exist):

```cypher
MATCH (n)
WHERE n.zone IN ['phoenix_domain','api_and_repo_layout','test_cases','playwright_automation']
RETURN n.zone AS layer, n.node_type AS type, count(*) AS n
ORDER BY layer, type
```

## Implementation vs this playbook

| Layer | Extractor today |
|-------|-----------------|
| phoenix_domain | Zip-codes: `ingest_zip_codes.py` — Phoenix code **and** Confluence page URLs (197112, 845119490, 845283330). |
| api_and_repo_layout | `swagger_extractor.py` — `ACCEPTS` from JSON body **and** query/path `$ref` |
| test_cases | `testcase_extractor.py` — no `COVERS` to real Endpoint uids; do not run until a topic has TCs |
| playwright_automation | Missing — omit until EnergoTS specs are ingested for a topic |

`pipeline.py` still ingests **all** Swagger (and all test-case folders if that zone is requested). `graph_index_topic("zip-codes")` uses the scoped zip ingest. Other topics still run unfiltered API ingest.

Until extractors exist for a layer, **omit that layer**. Say so in chat if the user expected it; do not call a two-layer topic graph incomplete.

`graph_query` searches GRAPH.1 layers (`phoenix_domain`, `api_and_repo_layout`, plus test/Playwright when asked). Legacy names such as `reference_data` are mapped to domain + API. Nodes must keep `n.zone` as those layer strings so retrieval can return `source_path`.

## Visualization (colors + human titles)

Four layers use **high-contrast** colors (`config/layer_display.yaml`). Neo4j Browser **does not** read `n.color` until GraSS is applied (or you click each label chip).

| Layer | Label | Color |
|-------|--------|--------|
| Business meaning | `BlueBusiness` | blue `#0047FF` |
| API | `YellowAPI` | yellow `#FFD000` |
| Test cases | `RedTests` | red `#FF0000` |
| Playwright tests | `GreenPlaywright` | green `#00C853` |

Bubble text starts with `🔵 BUSINESS` / `🟡 API` / `🔴 Test cases` / `🟢 PLAYWRIGHT`. Technical path is `technical_name`.

**See colors in Neo4j Browser** (`http://localhost:7474`):

```cypher
MATCH (n)
WHERE n.domain = 'zip-codes'
OPTIONAL MATCH (n)-[r]-(m)
WHERE m.domain = 'zip-codes'
RETURN n, r, m
```

Neo4j Browser auto-picks similar pink/blue/purple tones. To get the four sharp colors:

1. Run the query above (new result frame — do not reuse the old pink graph).
2. **Fast:** above the graph click `YellowAPI` → yellow, `BlueBusiness` → blue, `RedTests` → red, `GreenPlaywright` → green. Do not color `GraphIdx`.
3. **Exact hex:** `:style reset`, then `:style`, paste `config/neo4j-browser.grass`.
4. Caption: color chip → **Caption** → `title` if the bubble still shows a technical path.
