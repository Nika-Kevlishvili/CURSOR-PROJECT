# Graph RAG MCP server (SSE) — portable image
FROM python:3.12-slim

WORKDIR /app

ENV PYTHONDONTWRITEBYTECODE=1 \
    PYTHONUNBUFFERED=1 \
    PIP_NO_CACHE_DIR=1 \
    MCP_TRANSPORT=sse \
    MCP_HOST=0.0.0.0 \
    MCP_PORT=8100 \
    GRAPH_RAG_WORKSPACE=/workspace

RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/*

COPY requirements.txt /app/requirements.txt
RUN pip install --upgrade pip \
    && pip install -r /app/requirements.txt

COPY . /app/graph-rag

ENV PYTHONPATH=/app/graph-rag

EXPOSE 8100

HEALTHCHECK --interval=30s --timeout=10s --start-period=120s --retries=5 \
  CMD curl -s -o /dev/null -w "%{http_code}" --max-time 3 "http://127.0.0.1:${MCP_PORT}/sse" | grep -Eq "200|400|405|406" || exit 1

CMD ["python", "/app/graph-rag/server/mcp_server.py"]
