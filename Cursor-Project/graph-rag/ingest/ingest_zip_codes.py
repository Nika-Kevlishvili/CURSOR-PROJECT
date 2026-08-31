"""Ingest only zip-code API nodes from dev Swagger into Neo4j."""

import os
import sys

_root = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
sys.path.insert(0, _root)

from core.graph_client import GraphClient
from core.llm_client import LLMClient, preload_embed_model
from core.staleness import compute_content_hash, compute_file_hash
from ingest.extractors.swagger_extractor import extract_swagger
from ingest.pipeline import _upsert_result

API_LAYER = "api_and_repo_layout"
DOMAIN = "zip-codes"

# Human captions for Neo4j (title on the node, description on hover / inspector)
ZIP_COPY = {
    "GET /zip-codes": (
        "List zip codes",
        "Opens the paged list of zip codes. Search filters (page, size, statuses, place) are on the Zip code search filter type.",
    ),
    "POST /zip-codes": (
        "Create a zip code",
        "Creates a new zip code. Send the Zip code request body (name, populated place, status, default selection).",
    ),
    "GET /zip-codes/{id}": (
        "View one zip code",
        "Returns a single zip code by its id.",
    ),
    "PUT /zip-codes/{id}": (
        "Edit a zip code",
        "Updates an existing zip code. Send the Zip code request body.",
    ),
    "GET /zip-codes/{id}/tree": (
        "Zip code location tree",
        "Shows country, region, municipality, and populated place for this zip code.",
    ),
    "GET /zip-codes/{id}/detailed": (
        "Zip code details",
        "Returns the zip code plus the full populated-place name.",
    ),
    "ZipCodeRequest": (
        "Zip code request body",
        "Body you send to create or edit a zip code. Must include name, populated place id, status, and default selection.",
    ),
    "ZipCodeResponse": (
        "Zip code API result",
        "What the API returns for a zip code: id, name, status, populated place, ordering, and related ids.",
    ),
    "ZipCodeFilterRequest": (
        "Zip code search filter",
        "Paging and filters for the list: page, size, statuses, prompt, populated place, include/exclude ids.",
    ),
    "PageZipCodeResponse": (
        "Zip code list page",
        "A paged list of zip codes: the content array plus total pages and total elements.",
    ),
    "ZipCodeTreeResponse": (
        "Zip code place tree",
        "Place names around a zip code: country, region, municipality, and populated place.",
    ),
    "ZipCodeDetailedResponse": (
        "Zip code with place name",
        "Zip code fields plus the full populated-place name.",
    ),
    "ZipCodeRequest.status": (
        "Status on zip code request",
        "Allowed values when creating or editing: ACTIVE, INACTIVE, DELETED.",
    ),
    "ZipCodeResponse.status": (
        "Status on zip code result",
        "Allowed values on a returned zip code: ACTIVE, INACTIVE, DELETED.",
    ),
    "ZipCodeDetailedResponse.status": (
        "Status on zip code details",
        "Allowed values on the detailed result: ACTIVE, INACTIVE, DELETED.",
    ),
}


def _is_zip_node(node: dict) -> bool:
    props = node.get("properties") or {}
    path = str(props.get("path", "")).lower()
    name = str(node.get("name", "")).lower().replace("_", "")
    uid = str(node.get("uid", "")).lower().replace("_", "")
    if path.startswith("/zip-codes"):
        return True
    compact = name.replace("-", "")
    uid_c = uid.replace("-", "")
    return "zipcode" in compact or "zipcode" in uid_c


def _apply_zip_display(node: dict) -> None:
    node["zone"] = API_LAYER
    copy = ZIP_COPY.get(node.get("name", ""))
    if copy:
        node["title"], node["description"] = copy
    props = dict(node.get("properties") or {})
    props["domain"] = DOMAIN
    if copy:
        props["title"] = copy[0]
    node["properties"] = props


def _domain_layer(zip_nodes: list[dict]) -> tuple[list[dict], list[dict]]:
    """Domain pointers: code files + Confluence page URLs. No fake TC/Playwright."""
    domain_uid = "domain:zip-codes"
    entity_uid = "entity:zip-codes:ZipCode"
    place_uid = "entity:zip-codes:PopulatedPlace"
    comm_uid = "process:zip-codes:local-address"
    cust_uid = "process:zip-codes:customer-details-address"
    pod_uid = "process:zip-codes:pod-address"
    default_proc_uid = "process:zip-codes:default-selection"
    validation_uid = "validation:zip-codes:active-in-populated-place"
    inuse_uid = "validation:zip-codes:in-use-active-objects"
    default_val_uid = "validation:zip-codes:default-is-global"
    service_uid = "service:zip-codes:ZipCodeService"
    wiki_nom = "confluence:zip-codes:197112"
    wiki_change = "confluence:zip-codes:845119490"
    wiki_default = "confluence:zip-codes:845283330"
    java_zip = (
        "Cursor-Project/Phoenix/phoenix-core-lib/src/main/java/"
        "bg/energo/phoenix/model/entity/nomenclature/address/ZipCode.java"
    )
    java_comm = (
        "Cursor-Project/Phoenix/phoenix-core-lib/src/main/java/bg/energo/"
        "phoenix/service/customer/customerCommunications/"
        "CustomerCommunicationsService.java"
    )
    java_cust = (
        "Cursor-Project/Phoenix/phoenix-core-lib/src/main/java/"
        "bg/energo/phoenix/service/customer/CustomerDetailsService.java"
    )
    java_pod = (
        "Cursor-Project/Phoenix/phoenix-core-lib/src/main/java/"
        "bg/energo/phoenix/service/pod/pod/PointOfDeliveryService.java"
    )
    java_svc = (
        "Cursor-Project/Phoenix/phoenix-core-lib/src/main/java/"
        "bg/energo/phoenix/service/nomenclature/address/ZipCodeService.java"
    )
    java_repo = (
        "Cursor-Project/Phoenix/phoenix-core-lib/src/main/java/"
        "bg/energo/phoenix/repository/nomenclature/address/ZipCodeRepository.java"
    )
    wiki_base = "https://asterbit.atlassian.net/wiki/spaces/Phoenix/pages"
    nodes = [
        {
            "uid": domain_uid,
            "node_type": "Domain",
            "zone": "phoenix_domain",
            "name": "zip-codes",
            "title": "Zip codes",
            "description": (
                "Geographic master data: a postal code that belongs to a populated place. "
                "Part of address nomenclature (country → region → municipality → "
                "populated place → zip code). Stored in nomenclature.zip_codes."
            ),
            "source_path": java_zip,
            "properties": {"domain": DOMAIN},
        },
        {
            "uid": entity_uid,
            "node_type": "Entity",
            "zone": "phoenix_domain",
            "name": "ZipCode",
            "title": "Zip code record",
            "description": (
                "Table nomenclature.zip_codes. Fields: id, populated place (required), "
                "name (the postal code), status (ACTIVE / INACTIVE / DELETED), "
                "default selection, ordering id."
            ),
            "source_path": java_zip,
            "properties": {"domain": DOMAIN},
        },
        {
            "uid": place_uid,
            "node_type": "Entity",
            "zone": "phoenix_domain",
            "name": "PopulatedPlace",
            "title": "Populated place",
            "description": (
                "Settlement that owns zip codes. ZipCode.populatedPlace is required "
                "(column nomenclature.zip_codes.populated_place_id)."
            ),
            "source_path": java_zip,
            "properties": {"domain": DOMAIN},
        },
        {
            "uid": comm_uid,
            "node_type": "BusinessProcess",
            "zone": "phoenix_domain",
            "name": "local-address-zip-code",
            "title": "Zip code on customer communication address",
            "description": (
                "Customer communications local address: zipCodeId is mandatory and must "
                "be ACTIVE for that populated place."
            ),
            "source_path": java_comm,
            "properties": {"domain": DOMAIN},
        },
        {
            "uid": cust_uid,
            "node_type": "BusinessProcess",
            "zone": "phoenix_domain",
            "name": "customer-details-zip-code",
            "title": "Zip code on customer details address",
            "description": (
                "Customer details store zip_code_id (required FK to ZipCode). "
                "Save path validates zip against populated place in CustomerDetailsService."
            ),
            "source_path": java_cust,
            "properties": {"domain": DOMAIN},
        },
        {
            "uid": pod_uid,
            "node_type": "BusinessProcess",
            "zone": "phoenix_domain",
            "name": "pod-address-zip-code",
            "title": "Zip code on POD address",
            "description": (
                "POD details store zip_code_id on the local address. "
                "PointOfDeliveryService checks zip exists for the populated place."
            ),
            "source_path": java_pod,
            "properties": {"domain": DOMAIN},
        },
        {
            "uid": default_proc_uid,
            "node_type": "BusinessProcess",
            "zone": "phoenix_domain",
            "name": "zip-code-default-selection",
            "title": "Default zip code flag",
            "description": (
                "Runtime: only one default zip in the whole table "
                "(findByDefaultSelectionTrue). Product wiki asks for one default "
                "per populated place and auto-fill on address forms."
            ),
            "source_path": java_svc,
            "properties": {"domain": DOMAIN},
        },
        {
            "uid": service_uid,
            "node_type": "Service",
            "zone": "phoenix_domain",
            "name": "ZipCodeService",
            "title": "Zip code CRUD service",
            "description": (
                "Create, edit, list, tree, default flag, unique name per place, "
                "no DELETED on add/edit."
            ),
            "source_path": java_svc,
            "properties": {"domain": DOMAIN},
        },
        {
            "uid": validation_uid,
            "node_type": "Validation",
            "zone": "phoenix_domain",
            "name": "zip-code-active-in-place",
            "title": "Zip code must be active in the populated place",
            "description": (
                "Lookup by id + populatedPlaceId + ACTIVE (communications, "
                "customer details, POD)."
            ),
            "source_path": java_comm,
            "properties": {"domain": DOMAIN},
        },
        {
            "uid": inuse_uid,
            "node_type": "Validation",
            "zone": "phoenix_domain",
            "name": "zip-code-in-use",
            "title": "Zip in use by customer, communication, or POD",
            "description": (
                "getActiveConnectionsCount: ACTIVE CustomerDetails, "
                "CustomerCommunications, or PointOfDelivery. Blocks delete / parent change."
            ),
            "source_path": java_repo,
            "properties": {"domain": DOMAIN},
        },
        {
            "uid": default_val_uid,
            "node_type": "Validation",
            "zone": "phoenix_domain",
            "name": "zip-code-default-is-global",
            "title": "Default ZIP is global in code, per place in wiki",
            "description": (
                "Code: findByDefaultSelectionTrue — one is_default for the system. "
                "Confluence 845119490 and 845283330: one default per populated place "
                "and auto-fill on Customer / POD / communication / express contract."
            ),
            "source_path": java_svc,
            "properties": {"domain": DOMAIN},
        },
        {
            "uid": wiki_nom,
            "node_type": "BusinessProcess",
            "zone": "phoenix_domain",
            "name": "wiki-nomenclatures-zip",
            "title": "Wiki: Nomenclatures (ZIP)",
            "description": (
                "Confluence Nomenclatures: ZIP is non-standard nomenclature linked to "
                "Customer and Point of delivery. Page ID 197112."
            ),
            "source_path": f"{wiki_base}/197112/Nomenclatures",
            "properties": {
                "domain": DOMAIN,
                "confluence_page_id": "197112",
            },
        },
        {
            "uid": wiki_change,
            "node_type": "BusinessProcess",
            "zone": "phoenix_domain",
            "name": "wiki-zip-default-per-place",
            "title": "Wiki: default ZIP per populated place",
            "description": (
                "To-be: checkbox Default selection for populated place; only one "
                "default per place; new default clears the previous in that place. "
                "Page ID 845119490."
            ),
            "source_path": (
                f"{wiki_base}/845119490/changes+in+ZIP+Code+nomeclature"
            ),
            "properties": {
                "domain": DOMAIN,
                "confluence_page_id": "845119490",
            },
        },
        {
            "uid": wiki_default,
            "node_type": "BusinessProcess",
            "zone": "phoenix_domain",
            "name": "wiki-zip-autofill",
            "title": "Wiki: auto-fill default ZIP on address forms",
            "description": (
                "To-be: choosing a populated place auto-fills ZIP if that place has "
                "a default. Applies to Customer address, POD, communication data, "
                "express contract. Page ID 845283330."
            ),
            "source_path": (
                f"{wiki_base}/845283330/Default+ZIP+code+selection+for+populated+places"
            ),
            "properties": {
                "domain": DOMAIN,
                "confluence_page_id": "845283330",
            },
        },
    ]
    edges = [
        {"from_uid": domain_uid, "to_uid": entity_uid, "edge_type": "CONTAINS"},
        {"from_uid": domain_uid, "to_uid": place_uid, "edge_type": "CONTAINS"},
        {"from_uid": entity_uid, "to_uid": place_uid, "edge_type": "BELONGS_TO"},
        {"from_uid": domain_uid, "to_uid": comm_uid, "edge_type": "HAS_PROCESS"},
        {"from_uid": domain_uid, "to_uid": cust_uid, "edge_type": "HAS_PROCESS"},
        {"from_uid": domain_uid, "to_uid": pod_uid, "edge_type": "HAS_PROCESS"},
        {"from_uid": domain_uid, "to_uid": default_proc_uid, "edge_type": "HAS_PROCESS"},
        {"from_uid": domain_uid, "to_uid": wiki_nom, "edge_type": "HAS_PROCESS"},
        {"from_uid": domain_uid, "to_uid": wiki_change, "edge_type": "HAS_PROCESS"},
        {"from_uid": domain_uid, "to_uid": wiki_default, "edge_type": "HAS_PROCESS"},
        {"from_uid": comm_uid, "to_uid": validation_uid, "edge_type": "VALIDATES_WITH"},
        {"from_uid": cust_uid, "to_uid": validation_uid, "edge_type": "VALIDATES_WITH"},
        {"from_uid": pod_uid, "to_uid": validation_uid, "edge_type": "VALIDATES_WITH"},
        {"from_uid": default_proc_uid, "to_uid": default_val_uid, "edge_type": "VALIDATES_WITH"},
        {"from_uid": default_proc_uid, "to_uid": service_uid, "edge_type": "USES_SERVICE"},
        {"from_uid": default_val_uid, "to_uid": wiki_change, "edge_type": "DOCUMENTED_IN"},
        {"from_uid": default_val_uid, "to_uid": wiki_default, "edge_type": "DOCUMENTED_IN"},
        {"from_uid": default_proc_uid, "to_uid": inuse_uid, "edge_type": "VALIDATES_WITH"},
    ]
    for n in zip_nodes:
        if n.get("node_type") == "Endpoint":
            edges.append(
                {"from_uid": domain_uid, "to_uid": n["uid"], "edge_type": "HAS_ENDPOINT"}
            )
    return nodes, edges


PLACEHOLDER_UIDS = (
    "testcase:zip-codes:placeholder",
    "spec:zip-codes:placeholder",
)


def main() -> None:
    workspace = os.environ.get("GRAPH_RAG_WORKSPACE") or os.path.abspath(
        os.path.join(_root, "..", "..")
    )
    swagger = "Cursor-Project/config/swagger/dev/swagger-spec.json"
    print(f"Workspace: {workspace}")
    print("Extracting Swagger...")
    results = extract_swagger(swagger, workspace)

    all_nodes, all_edges = [], []
    for r in results:
        all_nodes.extend(r.get("nodes", []))
        all_edges.extend(r.get("edges", []))

    zip_nodes = [n for n in all_nodes if _is_zip_node(n)]
    for n in zip_nodes:
        _apply_zip_display(n)
    keep = {n["uid"] for n in zip_nodes}
    zip_edges = [
        e for e in all_edges if e["from_uid"] in keep and e["to_uid"] in keep
    ]
    marker_nodes, marker_edges = _domain_layer(zip_nodes)
    zip_nodes.extend(marker_nodes)
    zip_edges.extend(marker_edges)
    for n in zip_nodes:
        path = n.get("source_path") or ""
        if not path or n.get("source_hash"):
            continue
        if path.startswith("http://") or path.startswith("https://"):
            n["source_hash"] = compute_content_hash(path)
        else:
            hashed = compute_file_hash(os.path.join(workspace, path))
            if hashed:
                n["source_hash"] = hashed

    print(f"Zip-code nodes: {len(zip_nodes)}, edges: {len(zip_edges)}")
    for n in zip_nodes:
        print(f"  {n['node_type']:12} {n.get('title') or n['name']}")

    print("Preloading embeddings...")
    preload_embed_model()
    graph = GraphClient()
    llm = LLMClient()
    summary = {"nodes_created": 0, "edges_created": 0, "errors": []}
    try:
        graph.setup_schema()
        _upsert_result(
            graph,
            llm,
            {"nodes": zip_nodes, "edges": zip_edges},
            summary,
            verbose=True,
        )
        for uid in PLACEHOLDER_UIDS:
            graph.run_query("MATCH (n {uid: $uid}) DETACH DELETE n", {"uid": uid})
    finally:
        graph.close()

    print(
        f"Done. Upserted nodes={summary['nodes_created']} "
        f"edges={summary['edges_created']} errors={len(summary['errors'])}"
    )
    if summary["errors"]:
        for err in summary["errors"][:5]:
            print(f"  ERR {err}")


if __name__ == "__main__":
    main()
