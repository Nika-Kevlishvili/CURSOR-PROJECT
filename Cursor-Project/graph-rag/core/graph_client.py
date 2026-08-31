"""
Neo4j driver wrapper — zone-aware CRUD, vector search, Cypher helpers.
"""

import os
import re
from datetime import datetime, timezone
from typing import Any

import yaml
from neo4j import GraphDatabase

_CONFIG_PATH = os.path.join(os.path.dirname(__file__), "..", "config", "zones.yaml")

# Neo4j Browser colors by the FIRST label. Color* sorts before GraphIdx.
_LAYER_LABELS = (
    "YellowAPI", "BlueBusiness", "RedTests", "GreenPlaywright",
    "ColorBusiness", "ColorApi", "ColorTests", "ColorPlaywright",
    "LayerDomain", "LayerApi", "LayerTestCases", "LayerPlaywright",
)

# Neo4j Browser colors by the FIRST label. GraphIdx must not be first.
_TYPE_LABELS = (
    "Domain", "BusinessProcess", "Entity", "Validation", "Scheduler", "Service",
    "Endpoint", "DTO", "EnumType", "Repository", "Module",
    "TestCase", "TestStep", "Precondition", "ExpectedResult", "CoveredEndpoint",
    "Spec", "Fixture", "WritingRule", "Pattern", "Helper",
)


def _keyword_tokens(keyword: str) -> list[str]:
    stop = {
        "the", "a", "an", "is", "on", "in", "of", "to", "for", "and", "or",
        "where", "what", "how", "does", "do", "this", "that", "with", "from",
        "when", "are", "was", "can", "which", "who",
    }
    words = re.findall(r"[a-z0-9]+", (keyword or "").lower())
    tokens = [w for w in words if len(w) >= 3 and w not in stop]
    if tokens:
        return tokens[:8]
    short = [w for w in words if len(w) >= 2]
    return short[:5] or ["__none__"]


def _load_config() -> dict:
    with open(_CONFIG_PATH, encoding="utf-8") as f:
        return yaml.safe_load(f)


def _safe_label(value: str) -> str:
    if not re.match(r"^[A-Za-z][A-Za-z0-9_]*$", value or ""):
        return "GraphIdx"
    return value


class GraphClient:

    def __init__(self, uri: str | None = None, user: str | None = None,
                 password: str | None = None, database: str | None = None):
        cfg = _load_config().get("neo4j", {})
        self._uri = uri or os.environ.get("NEO4J_URI", cfg.get("uri", "bolt://localhost:7687"))
        self._user = user or os.environ.get("NEO4J_USER", cfg.get("user", "neo4j"))
        self._password = password or os.environ.get("NEO4J_PASSWORD", cfg.get("password", "graphrag"))
        self._database = database or os.environ.get("NEO4J_DATABASE", cfg.get("database", "neo4j"))
        self._driver = GraphDatabase.driver(self._uri, auth=(self._user, self._password))

    def close(self):
        self._driver.close()

    def run_query(self, cypher: str, params: dict | None = None) -> list[dict]:
        with self._driver.session(database=self._database) as session:
            result = session.run(cypher, params or {})
            return [record.data() for record in result]

    def setup_schema(self):
        SETUP_CYPHER = [
            "CREATE CONSTRAINT graphidx_uid IF NOT EXISTS FOR (n:GraphIdx) REQUIRE n.uid IS UNIQUE",
            "CREATE INDEX graphidx_zone_idx IF NOT EXISTS FOR (n:GraphIdx) ON (n.zone)",
            "CREATE INDEX graphidx_name_idx IF NOT EXISTS FOR (n:GraphIdx) ON (n.name)",
            "CREATE INDEX graphidx_type_idx IF NOT EXISTS FOR (n:GraphIdx) ON (n.node_type)",
        ]
        VECTOR_INDEX_CYPHER = (
            "CREATE VECTOR INDEX node_embeddings_graphidx IF NOT EXISTS "
            "FOR (n:GraphIdx) ON (n.embedding) "
            "OPTIONS {indexConfig: {"
            " `vector.dimensions`: 384,"
            " `vector.similarity_function`: 'cosine'"
            "}}"
        )
        for cypher in SETUP_CYPHER:
            self.run_query(cypher)
        try:
            self.run_query(VECTOR_INDEX_CYPHER)
        except Exception:
            pass  # vector index may already exist or Neo4j version may not support it

    def upsert_node(self, uid: str, node_type: str, zone: str,
                    name: str, description: str, properties: dict | None = None,
                    embedding: list[float] | None = None) -> None:
        from core.display import enrich_display, neo4j_layer_label

        enriched = enrich_display({
            "zone": zone,
            "name": name,
            "title": (properties or {}).get("title"),
            "description": description,
            "properties": dict(properties or {}),
        })
        zone = enriched["zone"]
        title = enriched["title"]
        description = enriched["description"]
        properties = enriched["properties"]

        # Browser default caption is `name`. Keep the technical id for search.
        props = {
            "uid": uid,
            "node_type": node_type,
            "zone": zone,
            "technical_name": name,
            "name": title,
            "title": title,
            "description": description,
            "last_updated": datetime.now(timezone.utc).isoformat(),
            **properties,
        }
        props["title"] = title
        props["name"] = title
        props["technical_name"] = name
        if embedding:
            props["embedding"] = embedding

        layer_label = _safe_label(neo4j_layer_label(zone))
        # Only Color* + GraphIdx. Type labels (Endpoint/DTO) are older in this DB
        # and would sort first — Browser would paint all endpoints the same color.
        self.run_query("MATCH (n {uid: $uid}) DETACH DELETE n", {"uid": uid})
        self.run_query(
            f"CREATE (n:{layer_label}:GraphIdx) SET n += $props",
            {"props": props},
        )

    def upsert_edge(self, from_uid: str, to_uid: str, edge_type: str,
                    properties: dict | None = None) -> None:
        cypher = (
            "MATCH (a:GraphIdx {uid: $from_uid}) "
            "MATCH (b:GraphIdx {uid: $to_uid}) "
            "MERGE (a)-[r:%s]->(b) "
            "SET r += $props" % edge_type
        )
        props = {
            "last_updated": datetime.now(timezone.utc).isoformat(),
            **(properties or {}),
        }
        self.run_query(cypher, {"from_uid": from_uid, "to_uid": to_uid, "props": props})

    def vector_search(self, embedding: list[float], zone: str | None = None,
                      top_k: int = 10) -> list[dict]:
        zone_filter = "WHERE n.zone = $zone" if zone else ""
        cypher = (
            "CALL db.index.vector.queryNodes('node_embeddings_graphidx', $top_k, $embedding) "
            "YIELD node AS n, score "
            f"{zone_filter} "
            "RETURN n.uid AS uid, n.name AS name, n.description AS description, "
            "n.zone AS zone, n.node_type AS node_type, n.source_path AS source_path, "
            "n.confluence_page_id AS confluence_page_id, "
            "n.source_hash AS source_hash, n.last_updated AS last_updated, score "
            "ORDER BY score DESC"
        )
        params: dict[str, Any] = {"top_k": top_k, "embedding": embedding}
        if zone:
            params["zone"] = zone
        return self.run_query(cypher, params)

    def keyword_search(self, keyword: str, zone: str | None = None,
                       limit: int = 10) -> list[dict]:
        zone_filter = "AND n.zone = $zone" if zone else ""
        cypher = (
            "MATCH (n:GraphIdx) "
            "WHERE ANY(t IN $tokens WHERE "
            "  toLower(n.name) CONTAINS t "
            "  OR toLower(coalesce(n.title, '')) CONTAINS t "
            "  OR toLower(n.description) CONTAINS t "
            "  OR toLower(coalesce(n.source_path, '')) CONTAINS t "
            "  OR toLower(coalesce(n.technical_name, '')) CONTAINS t "
            "  OR toLower(coalesce(n.confluence_page_id, '')) CONTAINS t"
            f") {zone_filter} "
            "RETURN n.uid AS uid, n.name AS name, n.description AS description, "
            "n.zone AS zone, n.node_type AS node_type, n.source_path AS source_path, "
            "n.confluence_page_id AS confluence_page_id, "
            "n.source_hash AS source_hash, n.last_updated AS last_updated "
            "LIMIT $limit"
        )
        params: dict[str, Any] = {
            "tokens": _keyword_tokens(keyword),
            "limit": limit,
        }
        if zone:
            params["zone"] = zone
        return self.run_query(cypher, params)

    def get_neighbors(self, uid: str, max_depth: int = 2) -> list[dict]:
        cypher = (
            "MATCH (n:GraphIdx {uid: $uid})-[r*1..%d]-(m:GraphIdx) "
            "RETURN DISTINCT m.uid AS uid, m.name AS name, m.description AS description, "
            "m.zone AS zone, m.node_type AS node_type, m.source_path AS source_path, "
            "m.confluence_page_id AS confluence_page_id" % max_depth
        )
        return self.run_query(cypher, {"uid": uid})

    def get_zone_stats(self) -> list[dict]:
        cypher = (
            "MATCH (n:GraphIdx) "
            "RETURN n.zone AS zone, n.node_type AS node_type, count(*) AS count "
            "ORDER BY zone, node_type"
        )
        return self.run_query(cypher)

    def get_node(self, uid: str) -> dict | None:
        results = self.run_query(
            "MATCH (n:GraphIdx {uid: $uid}) "
            "RETURN n.uid AS uid, n.name AS name, n.description AS description, "
            "n.zone AS zone, n.node_type AS node_type, n.source_path AS source_path, "
            "n.confluence_page_id AS confluence_page_id, "
            "n.source_hash AS source_hash, n.last_updated AS last_updated",
            {"uid": uid},
        )
        return results[0] if results else None

    def delete_node(self, uid: str) -> None:
        self.run_query("MATCH (n:GraphIdx {uid: $uid}) DETACH DELETE n", {"uid": uid})

    def count_nodes(self, zone: str | None = None) -> int:
        if zone:
            result = self.run_query(
                "MATCH (n:GraphIdx {zone: $zone}) RETURN count(n) AS cnt",
                {"zone": zone},
            )
        else:
            result = self.run_query("MATCH (n:GraphIdx) RETURN count(n) AS cnt")
        return result[0]["cnt"] if result else 0
