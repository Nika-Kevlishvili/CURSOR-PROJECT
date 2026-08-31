# Graph RAG portable Docker stack

**How to fill the graph (four layers):** `../INGEST.md` (Rule GRAPH.1). This file is only the Docker stack.

Run Neo4j, Ollama (LLM), and the GraphRAG MCP server (SSE) as one Compose stack.
Configure once, copy the repo to any machine with Docker, and start with a single script.

## Architecture

| Service | Port | Role |
|---------|------|------|
| `neo4j` | 7474 (HTTP), 7687 (Bolt) | Knowledge graph DB |
| `ollama` | 11434 | Local LLM (`qwen2.5:3b` by default) |
| `graph-rag` | 8100 | MCP SSE endpoint for Cursor |

Clients do **not** run `mcp_server.py` locally. Cursor connects to:

```json
"GraphRAG": {
  "url": "http://SERVER_IP:8100/sse"
}
```

## Prerequisites

- **Docker Desktop** (Windows/macOS) or Docker Engine + Compose plugin (Linux) — required before `start.ps1` / `start.sh`
- Enough disk for the first Ollama model pull (~2 GB+)
- Optional GPU: NVIDIA driver + nvidia-container-toolkit, then use `docker-compose.gpu.yml`

## First-time setup (home / any host)

1. Copy env file and edit values:

```powershell
cd Cursor-Project/graph-rag/docker
Copy-Item .env.example .env
# Set NEO4J_PASSWORD and GRAPH_RAG_WORKSPACE_HOST (host path to workspace root)
```

`GRAPH_RAG_WORKSPACE_HOST` must point at the workspace root that contains `Cursor-Project/` (used for ingest mounts).

Example Windows: `D:/Cursor/cursor-project`  
Example Linux: `/home/you/cursor-project`

2. Start:

```powershell
.\scripts\start.ps1
```

Linux/macOS:

```bash
chmod +x scripts/*.sh
./scripts/start.sh
```

3. Check:

```powershell
.\scripts\status.ps1
```

4. Point Cursor MCP at the stack (this machine):

```json
"GraphRAG": {
  "type": "http",
  "url": "http://127.0.0.1:8100/sse"
}
```

Reload MCP / restart Cursor, then call `graph_status`.

## Move to another PC

1. Copy or clone this repository (needs `Cursor-Project/graph-rag/` for image build).
2. Install Docker.
3. Create `.env` from `.env.example` (set password + local workspace path).
4. Run `scripts/start.ps1` or `scripts/start.sh`.
5. On each Cursor client, set:

```json
"GraphRAG": {
  "type": "http",
  "url": "http://SERVER_LAN_IP:8100/sse"
}
```

Allow inbound TCP **8100** on the host firewall (LAN/VPN only). Prefer not exposing Neo4j/Ollama to the public internet.

## GPU (optional)

```powershell
docker compose -f docker-compose.yml -f docker-compose.gpu.yml --env-file .env up -d --build
docker compose exec -T ollama ollama pull qwen2.5:3b
```

Or extend `start.ps1` to pass both compose files when needed.

## Stop / data

```powershell
.\scripts\stop.ps1
```

Named volumes (`neo4j_data`, `ollama_data`) are kept so graph data and models survive restarts.

## Local stdio mode (dev without Docker)

Still supported:

```powershell
$env:MCP_TRANSPORT = "stdio"
python Cursor-Project/graph-rag/server/mcp_server.py
```

With local Neo4j/LM Studio, use the classic `command` / `args` / `env` block in `.cursor/mcp.json`.

## Environment variables (container)

| Variable | Default | Meaning |
|----------|---------|---------|
| `NEO4J_URI` | `bolt://neo4j:7687` | Bolt URI inside Compose network |
| `NEO4J_PASSWORD` | from `.env` | Neo4j password |
| `NEO4J_BROWSER_URL` | `http://localhost:7474` | Shown in `graph_status` |
| `LLM_BASE_URL` | `http://ollama:11434/v1` | OpenAI-compatible Ollama API |
| `LLM_MODEL` | `qwen2.5:3b` | Ollama model tag |
| `MCP_TRANSPORT` | `sse` | `stdio` / `sse` / `streamable-http` |
| `MCP_HOST` | `0.0.0.0` | Bind address |
| `MCP_PORT` | `8100` | MCP listen port |
| `GRAPH_RAG_WORKSPACE` | `/workspace` | Ingest root inside container |

## Smoke checklist

- [ ] `docker compose ps` — services up
- [ ] Neo4j Browser opens
- [ ] `curl http://127.0.0.1:11434/api/tags` lists the model
- [ ] Cursor GraphRAG URL mode → `graph_status` succeeds
- [ ] Restart host → `start` again → data still present
- [ ] Neo4j Browser styles: paste `../config/neo4j-browser.grass` via `:style` so layers have colors and `{title}` captions
