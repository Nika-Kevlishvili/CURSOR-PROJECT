#!/usr/bin/env bash
# Show Graph RAG stack status and basic HTTP checks.
set -uo pipefail

COMPOSE_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$COMPOSE_DIR"

if [[ -f .env ]]; then
  docker compose --env-file .env ps
else
  docker compose ps
fi

check() {
  local name="$1" url="$2"
  if curl -fsS --max-time 5 "$url" >/dev/null 2>&1; then
    printf "%-16s OK  %s\n" "$name" "$url"
  else
    printf "%-16s FAIL %s\n" "$name" "$url"
  fi
}

echo
check "Neo4j" "http://127.0.0.1:7474"
check "Ollama" "http://127.0.0.1:11434/api/tags"
code="$(curl -s -o /dev/null -w "%{http_code}" --max-time 3 http://127.0.0.1:8100/sse || true)"
if [[ "$code" =~ ^(200|400|405|406)$ ]]; then
  printf "%-16s OK  %s (%s)\n" "GraphRAG SSE" "http://127.0.0.1:8100/sse" "$code"
else
  printf "%-16s FAIL %s (%s)\n" "GraphRAG SSE" "http://127.0.0.1:8100/sse" "$code"
fi
