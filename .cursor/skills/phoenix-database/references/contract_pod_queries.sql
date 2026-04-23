-- Phoenix DB - Standard query patterns (Rule DB.2)
-- Loaded on demand via the phoenix-database skill.
-- Connection details (host/port/user/password) live in MCP server config, never here.

-- =============================================================================
-- Find PRODUCT contracts by POD identifier
-- Schema chain: product_contract.contracts -> contract_details -> contract_pods
--               -> pod.pod_details -> pod.pod
-- =============================================================================
SELECT DISTINCT
    'PRODUCT_CONTRACT' AS contract_type,
    c.id                AS contract_id,
    c.contract_number,
    c.status,
    c.contract_status,
    c.create_date,
    c.entry_into_force_date,
    c.termination_date,
    p.identifier        AS pod_identifier,
    cp.id               AS contract_pod_id,
    cp.activation_date,
    cp.deactivation_date,
    cp.status           AS contract_pod_status
FROM product_contract.contracts c
JOIN product_contract.contract_details cd ON cd.contract_id      = c.id
JOIN product_contract.contract_pods    cp ON cp.contract_detail_id = cd.id
JOIN pod.pod_details                   pd ON cp.pod_detail_id    = pd.id
JOIN pod.pod                           p  ON pd.pod_id           = p.id
WHERE p.identifier = :pod_identifier
ORDER BY c.contract_number, c.id;

-- =============================================================================
-- Find SERVICE contracts by POD identifier
-- Schema chain: service_contract.contracts -> contract_details -> contract_pods
--               -> pod.pod (direct pod_id, no pod_details)
-- =============================================================================
SELECT DISTINCT
    'SERVICE_CONTRACT' AS contract_type,
    c.id                AS contract_id,
    c.contract_number,
    c.status,
    c.contract_status,
    c.create_date,
    c.entry_into_force_date,
    c.termination_date,
    p.identifier        AS pod_identifier,
    cp.id               AS contract_pod_id,
    cp.status           AS contract_pod_status
FROM service_contract.contracts c
JOIN service_contract.contract_details cd ON cd.contract_id       = c.id
JOIN service_contract.contract_pods    cp ON cp.contract_detail_id = cd.id
JOIN pod.pod                           p  ON cp.pod_id            = p.id
WHERE p.identifier = :pod_identifier
ORDER BY c.contract_number, c.id;

-- =============================================================================
-- Notes
-- - POD identifier is stored in pod.pod.identifier (varchar(33))
-- - Always use DISTINCT when joining multiple tables to avoid duplicate rows
-- - Include contract_type column to distinguish Product vs Service results
-- - Order by contract_number, contract_id for stable output
-- =============================================================================
