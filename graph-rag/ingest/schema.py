"""
Graph schema definitions — node types, edge types, zone labels, and Neo4j constraints.
"""

ZONE_LABELS = [
    "rules",
    "skills_and_agents",
    "memory_and_handoff",
    "sdlc_docs",
]

NODE_TYPES = {
    "rules": ["Rule"],
    "skills_and_agents": ["Skill", "Agent"],
    "memory_and_handoff": ["MemoryNote"],
    "sdlc_docs": ["Document"],
}

EDGE_TYPES_INTRA = {
    "rules": [],
    "skills_and_agents": [],
    "memory_and_handoff": [],
    "sdlc_docs": [],
}

EDGE_TYPES_BRIDGE = []

COMMON_PROPERTIES = {
    "source_path": "str",
    "source_hash": "str",
    "last_updated": "datetime",
    "zone": "str",
    "name": "str",
    "description": "str",
    "embedding": "list[float]",
}

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
