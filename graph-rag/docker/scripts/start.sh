#!/usr/bin/env bash
# Start portable Graph RAG Docker stack (Neo4j + Ollama + MCP SSE).
set -euo pipefail

COMPOSE_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$COMPOSE_DIR"

if [[ ! -f .env ]]; then
  if [[ ! -f .env.example ]]; then
    echo ".env.example not found in $COMPOSE_DIR" >&2
    exit 1
  fi
  cp .env.example .env
  echo "Created .env from .env.example — edit NEO4J_PASSWORD and GRAPH_RAG_WORKSPACE_HOST, then re-run."
  exit 2
fi

OLLAMA_MODEL="$(grep -E '^\s*OLLAMA_MODEL\s*=' .env | tail -n1 | cut -d= -f2- | tr -d '[:space:]"'\' || true)"
OLLAMA_MODEL="${OLLAMA_MODEL:-qwen2.5:3b}"

echo "Building / starting Graph RAG stack in $COMPOSE_DIR ..."
docker compose --env-file .env up -d --build

echo "Waiting for Ollama..."
deadline=$((SECONDS + 300))
until docker compose exec -T ollama ollama list >/dev/null 2>&1; do
  if (( SECONDS >= deadline )); then
    echo "Ollama did not become ready in time" >&2
    exit 1
  fi
  sleep 3
done

echo "Pulling model: $OLLAMA_MODEL (first run may take a while)..."
docker compose exec -T ollama ollama pull "$OLLAMA_MODEL"

echo
echo "Stack started."
echo "  Neo4j Browser : http://localhost:7474"
echo "  Ollama API    : http://localhost:11434"
echo "  GraphRAG MCP  : http://localhost:8100/sse"
echo
echo "Cursor mcp.json (this machine):"
echo '  "GraphRAG": { "type": "http", "url": "http://127.0.0.1:8100/sse" }'
echo "Other PCs: use http://<SERVER_LAN_IP>:8100/sse"
