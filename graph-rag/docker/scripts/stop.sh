#!/usr/bin/env bash
# Stop Graph RAG Docker stack (keeps named volumes / data).
set -euo pipefail

COMPOSE_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$COMPOSE_DIR"

echo "Stopping Graph RAG stack..."
if [[ -f .env ]]; then
  docker compose --env-file .env down
else
  docker compose down
fi
echo "Stopped. Named volumes retained (neo4j_data, ollama_data)."
