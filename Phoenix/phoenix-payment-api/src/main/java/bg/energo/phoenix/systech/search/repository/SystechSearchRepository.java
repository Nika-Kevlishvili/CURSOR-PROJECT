package bg.energo.phoenix.systech.search.repository;

import java.util.List;

import bg.energo.phoenix.model.entity.customer.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface SystechSearchRepository extends JpaRepository<Customer, Long> {

    @Query(value = """
                                                WITH params AS (
                SELECT
                    NULLIF(btrim(:customerNumber),  '')::text AS p_customer_number,
                    NULLIF(btrim(:customerName),    '')::text AS p_customer_name,
                    NULLIF(btrim(:customerAddress), '')::text AS p_customer_address,
                    NULLIF(btrim(:customerPin),     '')::text AS p_customer_pin,
                    NULLIF(btrim(:customerPhone),   '')::text AS p_customer_phone,
                    NULLIF(btrim(:customerPod),     '')::text AS p_customer_pod
            ),
                 parsed AS (
                     SELECT
                         p.*,
                         regexp_replace(p.p_customer_phone, '[^0-9]', '', 'g') AS p_customer_phone_digits,
                         CASE WHEN p.p_customer_number ~ '^[0-9]{14}$' THEN left(p.p_customer_number, 10) END AS p_cn_10,
                         CASE WHEN p.p_customer_number ~ '^[0-9]{14}$' THEN right(p.p_customer_number, 4) END AS p_bg_4
                     FROM params p
                 ),
                 phone_customers AS (
                     SELECT DISTINCT cd_ph.customer_id
                     FROM parsed pr
                              JOIN customer.customer_communication_contacts ccc_ph
                                   ON pr.p_customer_phone_digits IS NOT NULL
                                       AND ccc_ph.status = 'ACTIVE'
                                       AND ccc_ph.contact_type = 'MOBILE_NUMBER'
                                       AND ccc_ph.contact_value IS NOT NULL
                                       AND regexp_replace(ccc_ph.contact_value, '[^0-9]', '', 'g') LIKE '%' || pr.p_customer_phone_digits || '%'
                              JOIN customer.customer_communications cc_ph
                                   ON cc_ph.id = ccc_ph.customer_communication_id
                                       AND cc_ph.status = 'ACTIVE'
                              JOIN customer.customer_comm_contact_purposes cp_ph
                                   ON cp_ph.customer_communication_id = cc_ph.id
                                       AND cp_ph.status = 'ACTIVE'
                                       AND cp_ph.contact_purpose_id = 83
                              JOIN customer.customer_details cd_ph
                                   ON cd_ph.id = cc_ph.customer_detail_id
                 ),
                 address_terms AS (
                     SELECT btrim(term) AS term
                     FROM parsed pr
                              CROSS JOIN LATERAL unnest(string_to_array(pr.p_customer_address, ',')) AS t(term)
                     WHERE pr.p_customer_address IS NOT NULL
                       AND btrim(term) <> ''
                 ),
                 active_customers AS MATERIALIZED (
                     SELECT
                         c.id,
                         c.customer_number,
                         c.customer_type,
                         c.identifier,
                         c.last_customer_detail_id
                     FROM customer.customers c
                              CROSS JOIN parsed pr
                     WHERE c.status::text = 'ACTIVE'
                       AND (
                         pr.p_customer_number IS NULL
                             OR (
                             pr.p_customer_number ~ '^[0-9]+$'
                                 AND length(pr.p_customer_number) <> 14
                                 AND c.customer_number::text = pr.p_customer_number
                             )
                             OR (
                             pr.p_customer_number ~ '^[0-9]{14}$'
                                 AND c.customer_number::text = pr.p_cn_10
                                 AND EXISTS (
                                 SELECT 1
                                 FROM customer.customer_details cd_any
                                          JOIN product_contract.contract_details pcd
                                               ON pcd.customer_detail_id = cd_any.id
                                                   AND COALESCE(pcd.status::text, '') NOT IN ('DRAFT', 'CANCELLED')
                                          JOIN product_contract.contracts pc
                                               ON pc.id = pcd.contract_id
                                                   AND pc.status::text = 'ACTIVE'
                                          JOIN product_contract.contract_billing_groups cbg
                                               ON cbg.contract_id = pc.id
                                                   AND cbg.status::text = 'ACTIVE'
                                 WHERE cd_any.customer_id = c.id
                                   AND cbg.group_number = pr.p_bg_4
                             )
                             )
                         )
                       AND (pr.p_customer_pin IS NULL OR c.identifier::text = pr.p_customer_pin)
                       AND (
                         pr.p_customer_phone IS NULL
                             OR EXISTS (
                             SELECT 1
                             FROM phone_customers pcust
                             WHERE pcust.customer_id = c.id
                         )
                         )
                 ),
                 base AS MATERIALIZED (
                     SELECT
                         c.id AS customer_id,
                         c.customer_number::text AS customer_number_10,
                         c.customer_type::text AS customer_type,
                         c.identifier::text AS customer_pin,
                         cd.id AS customer_detail_id,
                         cd.name,
                         cd.middle_name,
                         cd.last_name,
                         cd.legal_form_id,
                         cd.foreign_address,
                         cd.country_id,
                         cd.populated_place_id,
                         cd.zip_code_id,
                         cd.district_id,
                         cd.residential_area_id,
                         cd.street_id,
                         cd.region_foreign,
                         cd.municipality_foreign,
                         cd.populated_place_foreign,
                         cd.zip_code_foreign,
                         cd.district_foreign,
                         cd.residential_area_foreign,
                         cd.street_foreign,
                         cd.street_number,
                         cd.block,
                         cd.entrance,
                         cd.floor,
                         cd.apartment,
                         cd.mailbox,
                         cd.address_additional_info
                     FROM active_customers c
                              JOIN customer.customer_details cd
                                   ON cd.id = c.last_customer_detail_id
                 ),
                 projected AS (
                     SELECT
                         b.customer_id,
                         b.customer_detail_id,
                         b.customer_number_10,
                         b.customer_type,
                         b.address_additional_info, -- ADDED THIS FIELD FOR ILIKE MATCHING
                         CASE
                             WHEN b.customer_type = 'LEGAL_ENTITY'
                                 THEN concat_ws(' ', NULLIF(b.name, ''), NULLIF(lf.name, ''))
                             ELSE concat_ws(' ', NULLIF(b.name, ''), NULLIF(b.middle_name, ''), NULLIF(b.last_name, ''))
                             END AS customer_name,
                         CASE
                             WHEN b.foreign_address = true THEN
                                 concat_ws(', ',
                                           NULLIF(ctry.name,''), NULLIF(b.region_foreign,''),
                                           NULLIF(b.municipality_foreign,''), NULLIF(b.populated_place_foreign,''),
                                           NULLIF(b.zip_code_foreign,''), NULLIF(b.district_foreign,''),
                                           NULLIF(b.residential_area_foreign,''), NULLIF(b.street_foreign,''),
                                           NULLIF(b.street_number,''), NULLIF(b.block,''),
                                           NULLIF(b.entrance,''), NULLIF(b.floor,''),
                                           NULLIF(b.apartment,''), NULLIF(b.mailbox,''),
                                           NULLIF(b.address_additional_info,'')
                                 )
                             ELSE
                                 concat_ws(', ',
                                           NULLIF(ctry.name,''), NULLIF(r.name,''),
                                           NULLIF(m.name,''), NULLIF(pp.name,''),
                                           NULLIF(zc.zip_code,''), NULLIF(d.name,''),
                                           NULLIF(ra.name,''), NULLIF(s.name,''),
                                           NULLIF(b.street_number,''), NULLIF(b.block,''),
                                           NULLIF(b.entrance,''), NULLIF(b.floor,''),
                                           NULLIF(b.apartment,''), NULLIF(b.mailbox,''),
                                           NULLIF(b.address_additional_info,'')
                                 )
                             END AS customer_address,
                         b.customer_pin
                     FROM base b
                              LEFT JOIN nomenclature.legal_forms lf        ON lf.id = b.legal_form_id
                              LEFT JOIN nomenclature.countries ctry        ON ctry.id = b.country_id
                              LEFT JOIN nomenclature.populated_places pp   ON pp.id = b.populated_place_id
                              LEFT JOIN nomenclature.zip_codes zc          ON zc.id = b.zip_code_id
                              LEFT JOIN nomenclature.municipalities m      ON m.id = pp.municipality_id
                              LEFT JOIN nomenclature.regions r             ON r.id = m.region_id
                              LEFT JOIN nomenclature.districts d           ON d.id = b.district_id
                              LEFT JOIN nomenclature.residential_areas ra  ON ra.id = b.residential_area_id
                              LEFT JOIN nomenclature.streets s             ON s.id = b.street_id
                 ),
                 filtered AS (
                     SELECT p.*
                     FROM projected p
                              CROSS JOIN parsed pr
                     WHERE
                         (pr.p_customer_name IS NULL OR EXISTS (
                             SELECT 1
                             FROM customer.customer_details cd_any
                                      LEFT JOIN nomenclature.legal_forms lf_any
                                                ON lf_any.id = cd_any.legal_form_id
                             WHERE cd_any.customer_id = p.customer_id
                               AND (
                                       CASE
                                           WHEN p.customer_type = 'LEGAL_ENTITY'
                                               THEN concat_ws(' ', NULLIF(cd_any.name, ''), NULLIF(lf_any.name, ''))
                                           ELSE concat_ws(' ', NULLIF(cd_any.name, ''), NULLIF(cd_any.middle_name, ''), NULLIF(cd_any.last_name, ''))
                                           END
                                       ) ILIKE '%' || pr.p_customer_name || '%'
                         ))
                       -- MODIFIED LOGIC: Exact match for all parts OR ILIKE match for address_additional_info
                       AND (
                         NOT EXISTS (SELECT 1 FROM address_terms)
                             OR NOT EXISTS (
                             SELECT 1
                             FROM address_terms at
                             WHERE NOT (
                                 COALESCE(p.address_additional_info, '') ILIKE '%' || at.term || '%'
                                     OR EXISTS (
                                     SELECT 1
                                     FROM unnest(string_to_array(p.customer_address, ',')) AS ap(addr_part)
                                     WHERE lower(btrim(ap.addr_part)) = lower(at.term)
                                 )
                                 )
                         )
                         )
                       AND (pr.p_customer_pod IS NULL OR EXISTS (
                         SELECT 1
                         FROM customer.customer_details cd_any
                                  JOIN product_contract.contract_details pcd
                                       ON pcd.customer_detail_id = cd_any.id
                                           AND COALESCE(pcd.status::text, '') NOT IN ('DRAFT', 'CANCELLED')
                                  JOIN product_contract.contracts pc
                                       ON pc.id = pcd.contract_id
                                           AND pc.status::text = 'ACTIVE'
                                           AND pc.contract_status::text NOT IN ('DRAFT', 'READY', 'CANCELLED')
                                  JOIN product_contract.contract_pods cp
                                       ON cp.contract_detail_id = pcd.id
                                           AND cp.status::text = 'ACTIVE'
                                           AND cp.activation_date IS NOT NULL
                                  JOIN pod.pod_details pd ON pd.id = cp.pod_detail_id
                                  JOIN pod.pod p0
                                       ON p0.id = pd.pod_id
                                           AND p0.status::text = 'ACTIVE'
                         WHERE cd_any.customer_id = p.customer_id
                           AND p0.identifier = pr.p_customer_pod
                           AND (
                             pr.p_bg_4 IS NULL
                                 OR EXISTS (
                                 SELECT 1
                                 FROM product_contract.contract_billing_groups cbg
                                 WHERE cbg.id = cp.contract_billing_group_id
                                   AND cbg.group_number = pr.p_bg_4
                             )
                             )
                     ))
                 )
            SELECT
                CASE
                    WHEN pr.p_customer_number ~ '^[0-9]{14}$' THEN f.customer_number_10 || pr.p_bg_4
                    ELSE f.customer_number_10
                    END AS CustomerNumber,
                f.customer_name    AS CustomerName,
                f.customer_address AS CustomerAddress,
                f.customer_pin     AS CustomerPIN,
                pbd.customer_phone AS CustomerPhone,
                podbc.customer_pod AS CustomerPOD
            FROM filtered f
                     CROSS JOIN parsed pr
                     LEFT JOIN LATERAL (
                SELECT string_agg(DISTINCT ccc.contact_value, ', ' ORDER BY ccc.contact_value) AS customer_phone
                FROM customer.customer_communications cc
                         JOIN customer.customer_communication_contacts ccc
                              ON ccc.customer_communication_id = cc.id
                                  AND ccc.status = 'ACTIVE'
                                  AND ccc.contact_type = 'MOBILE_NUMBER'
                                  AND ccc.contact_value IS NOT NULL
                                  AND btrim(ccc.contact_value) <> ''
                WHERE cc.customer_detail_id = f.customer_detail_id
                  AND cc.status = 'ACTIVE'
                  AND EXISTS (
                    SELECT 1
                    FROM customer.customer_comm_contact_purposes cp
                    WHERE cp.customer_communication_id = cc.id
                      AND cp.status = 'ACTIVE'
                      AND cp.contact_purpose_id = 83
                )
                ) pbd ON true
                     LEFT JOIN LATERAL (
                SELECT string_agg(x.pod_identifier, ', ' ORDER BY x.pod_identifier) AS customer_pod
                FROM (
                         SELECT DISTINCT p.identifier AS pod_identifier
                         FROM product_contract.contract_pods cp
                                  JOIN pod.pod_details pd ON pd.id = cp.pod_detail_id
                                  JOIN pod.pod p
                                       ON p.id = pd.pod_id
                                           AND p.status::text = 'ACTIVE'
                                  JOIN product_contract.contract_details pcd
                                       ON pcd.id = cp.contract_detail_id
                                           AND COALESCE(pcd.status::text, '') NOT IN ('DRAFT', 'CANCELLED')
                                  JOIN product_contract.contracts pc
                                       ON pc.id = pcd.contract_id
                                           AND pc.status::text = 'ACTIVE'
                                           AND pc.contract_status::text NOT IN ('DRAFT', 'READY', 'CANCELLED')
                                  JOIN customer.customer_details cd_pod
                                       ON cd_pod.id = pcd.customer_detail_id
                         WHERE cd_pod.customer_id = f.customer_id
                           AND cp.status::text = 'ACTIVE'
                           AND cp.activation_date IS NOT NULL
                           AND p.identifier IS NOT NULL
                           AND btrim(p.identifier) <> ''
                     ) x
                ) podbc ON true
            LIMIT 101
            """, nativeQuery = true)
    List<CustomerSearchProjection> searchCustomers(
            @Param("customerNumber") String customerNumber,
            @Param("customerName") String customerName,
            @Param("customerAddress") String customerAddress,
            @Param("customerPin") String customerPin,
            @Param("customerPhone") String customerPhone,
            @Param("customerPod") String customerPod
    );
}
