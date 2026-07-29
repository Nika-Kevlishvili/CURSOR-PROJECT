"""
Graph schema definitions — node types, edge types, zone labels, and Neo4j constraints.
"""

ZONE_LABELS = [
    "phoenix_domain",
    "api_and_repo_layout",
    "test_cases",
    "playwright_automation",
]

NODE_TYPES = {
    "phoenix_domain": [
        "Domain", "BusinessProcess", "Entity", "Validation", "Scheduler", "Service",
    ],
    "api_and_repo_layout": [
        "Endpoint", "DTO", "EnumType", "Repository", "Module",
    ],
    "test_cases": [
        "TestCase", "TestStep", "Precondition", "ExpectedResult", "CoveredEndpoint",
    ],
    "playwright_automation": [
        "Spec", "Fixture", "WritingRule", "Pattern", "Helper",
    ],
}

EDGE_TYPES_INTRA = {
    "phoenix_domain": [
        ("Domain", "CONTAINS", "Entity"),
        ("Domain", "HAS_PROCESS", "BusinessProcess"),
        ("BusinessProcess", "VALIDATES_WITH", "Validation"),
        ("BusinessProcess", "USES_SERVICE", "Service"),
        ("Service", "TRIGGERS", "Scheduler"),
    ],
    "api_and_repo_layout": [
        ("Module", "EXPOSES", "Endpoint"),
        ("Endpoint", "ACCEPTS", "DTO"),
        ("Endpoint", "RETURNS", "DTO"),
        ("DTO", "HAS_ENUM", "EnumType"),
        ("Endpoint", "LIVES_IN", "Repository"),
    ],
    "test_cases": [
        ("TestCase", "HAS_STEP", "TestStep"),
        ("TestCase", "HAS_PRECONDITION", "Precondition"),
        ("TestStep", "EXPECTS", "ExpectedResult"),
        ("TestCase", "COVERS_ENDPOINT", "CoveredEndpoint"),
    ],
    "playwright_automation": [
        ("Spec", "USES_FIXTURE", "Fixture"),
        ("Spec", "FOLLOWS_RULE", "WritingRule"),
        ("Spec", "APPLIES_PATTERN", "Pattern"),
        ("Spec", "CALLS_HELPER", "Helper"),
    ],
}

EDGE_TYPES_BRIDGE = [
    ("TestCase", "COVERS", "Endpoint", "test_cases", "api_and_repo_layout"),
    ("Spec", "IMPLEMENTS", "TestCase", "playwright_automation", "test_cases"),
    ("Domain", "HAS_ENDPOINT", "Endpoint", "phoenix_domain", "api_and_repo_layout"),
]

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
