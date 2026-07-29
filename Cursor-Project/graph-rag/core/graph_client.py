"""
Neo4j driver wrapper — zone-aware CRUD, vector search, Cypher helpers.
"""

import os
from datetime import datetime, timezone
from typing import Any

import yaml
from neo4j import GraphDatabase

_CONFIG_PATH = os.path.join(os.path.dirname(__file__), "..", "config", "zones.yaml")


def _load_config() -> dict:
    with open(_CONFIG_PATH, encoding="utf-8") as f:
        return yaml.safe_load(f)


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
            "CREATE CONSTRAINT node_unique_id IF NOT EXISTS FOR (n:GraphNode) REQUIRE n.uid IS UNIQUE",
            "CREATE INDEX node_zone_idx IF NOT EXISTS FOR (n:GraphNode) ON (n.zone)",
            "CREATE INDEX node_name_idx IF NOT EXISTS FOR (n:GraphNode) ON (n.name)",
            "CREATE INDEX node_type_idx IF NOT EXISTS FOR (n:GraphNode) ON (n.node_type)",
        ]
        VECTOR_INDEX_CYPHER = (
            "CREATE VECTOR INDEX node_embeddings IF NOT EXISTS "
            "FOR (n:GraphNode) ON (n.embedding) "
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
        props = {
            "uid": uid,
            "node_type": node_type,
            "zone": zone,
            "name": name,
            "description": description,
            "last_updated": datetime.now(timezone.utc).isoformat(),
            **(properties or {}),
        }
        if embedding:
            props["embedding"] = embedding

        cypher = (
            "MERGE (n:GraphNode {uid: $uid}) "
            "SET n += $props "
            "SET n:%s" % node_type
        )
        self.run_query(cypher, {"uid": uid, "props": props})

    def upsert_edge(self, from_uid: str, to_uid: str, edge_type: str,
                    properties: dict | None = None) -> None:
        cypher = (
            "MATCH (a:GraphNode {uid: $from_uid}) "
            "MATCH (b:GraphNode {uid: $to_uid}) "
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
            "CALL db.index.vector.queryNodes('node_embeddings', $top_k, $embedding) "
            "YIELD node AS n, score "
            f"{zone_filter} "
            "RETURN n.uid AS uid, n.name AS name, n.description AS description, "
            "n.zone AS zone, n.node_type AS node_type, n.source_path AS source_path, "
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
            "MATCH (n:GraphNode) "
            f"WHERE (toLower(n.name) CONTAINS toLower($keyword) "
            f"OR toLower(n.description) CONTAINS toLower($keyword)) {zone_filter} "
            "RETURN n.uid AS uid, n.name AS name, n.description AS description, "
            "n.zone AS zone, n.node_type AS node_type, n.source_path AS source_path, "
            "n.source_hash AS source_hash, n.last_updated AS last_updated "
            "LIMIT $limit"
        )
        params: dict[str, Any] = {"keyword": keyword, "limit": limit}
        if zone:
            params["zone"] = zone
        return self.run_query(cypher, params)

    def get_neighbors(self, uid: str, max_depth: int = 2) -> list[dict]:
        cypher = (
            "MATCH (n:GraphNode {uid: $uid})-[r*1..%d]-(m:GraphNode) "
            "RETURN DISTINCT m.uid AS uid, m.name AS name, m.description AS description, "
            "m.zone AS zone, m.node_type AS node_type" % max_depth
        )
        return self.run_query(cypher, {"uid": uid})

    def get_zone_stats(self) -> list[dict]:
        cypher = (
            "MATCH (n:GraphNode) "
            "RETURN n.zone AS zone, n.node_type AS node_type, count(*) AS count "
            "ORDER BY zone, node_type"
        )
        return self.run_query(cypher)

    def get_node(self, uid: str) -> dict | None:
        results = self.run_query(
            "MATCH (n:GraphNode {uid: $uid}) "
            "RETURN n.uid AS uid, n.name AS name, n.description AS description, "
            "n.zone AS zone, n.node_type AS node_type, n.source_path AS source_path, "
            "n.source_hash AS source_hash, n.last_updated AS last_updated",
            {"uid": uid},
        )
        return results[0] if results else None

    def delete_node(self, uid: str) -> None:
        self.run_query("MATCH (n:GraphNode {uid: $uid}) DETACH DELETE n", {"uid": uid})

    def count_nodes(self, zone: str | None = None) -> int:
        if zone:
            result = self.run_query(
                "MATCH (n:GraphNode {zone: $zone}) RETURN count(n) AS cnt",
                {"zone": zone},
            )
        else:
            result = self.run_query("MATCH (n:GraphNode) RETURN count(n) AS cnt")
        return result[0]["cnt"] if result else 0
