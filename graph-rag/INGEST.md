# Graph RAG ingest playbook

**Rule:** GRAPH.1 — `.cursor/rules/integrations/graph_rag_ingest.mdc`

This graph indexes the **engine** (rules, skills, agents, memory, SDLC docs). It is not evidence about Phoenix or EnergoTS. Do not answer product questions from graph nodes.

`graph-rag/` lives at the repository root. Workspace root is the parent of `graph-rag/` (one level up, not two).

## Zones

| Zone | Paths | Node types |
|------|--------|------------|
| `rules` | `.cursor/rules` | Rule |
| `skills_and_agents` | `.cursor/skills`, `.cursor/agents` | Skill, Agent |
| `memory_and_handoff` | `memory/` | MemoryNote |
| `sdlc_docs` | `docs/`, `sdlc/design` | Document |

The default pipeline is `ingest/pipeline.py`. It reads only those paths.

## What the default pipeline does not do

- It does not index `Phoenix/`, `EnergoTS/`, or Swagger.
- It does not call `ingest/extractors/swagger_extractor.py` or `ingest/extractors/testcase_extractor.py`. Those files stay in the tree.
- It does not run `ingest/ingest_zip_codes.py`.

## How to fill

1. Set `NEO4J_PASSWORD` in the environment. Do not put the password in git.
2. Run `python3 graph-rag/ingest/pipeline.py` from the repository root only after the user agrees (GRAPH.0). This change does not start Docker or install packages.
3. Upsert is by `uid`. `source_hash` marks staleness when a file changes.
4. `graph_index_topic` uses the same engine pipeline. It does not switch to zip-code or Swagger ingest.

## What not to ingest

- Phoenix or EnergoTS source trees
- Swagger specs
- Secrets, `.env`, API keys, database passwords
- Whole Confluence, or chat text that was not checked against an engine file

## Query

`graph_query` returns `source_path` pointers inside the engine. Open those files before citing them. If the question is about Phoenix or EnergoTS, read those trees (or Confluence) directly. Do not treat graph text as product evidence.

Neo4j Browser, when a local server is already running: `http://localhost:7474`, Bolt `bolt://localhost:7687`, user `neo4j`, password from `NEO4J_PASSWORD`.
