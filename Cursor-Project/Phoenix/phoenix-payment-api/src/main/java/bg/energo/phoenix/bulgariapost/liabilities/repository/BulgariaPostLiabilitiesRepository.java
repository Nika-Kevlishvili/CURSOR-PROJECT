package bg.energo.phoenix.bulgariapost.liabilities.repository;

import bg.energo.phoenix.model.entity.customer.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface BulgariaPostLiabilitiesRepository extends JpaRepository<Customer, Long> {

    /**
     * GET /obligations response rows for Bulgarian Post (PHN-3641).
     * <p>
     * CustomerAddress is the POD address from the liability billing group (empty if 0 or more than one POD).
     * DocumentInfo is: Basis for issuing + ", " + POD identifier + ", " + meter period from + "-" + meter period to
     * (from Invoice; empty when those fields are absent; POD identifier empty when the invoice has multiple PODs).
     * When combine liabilities is on, DocumentInfo values are joined with "; ".
     */
    @Query(value = """
            WITH params AS (
                                            SELECT
                                                NULLIF(:customerNumber, '')::text AS p_customer_number,
                                                :collectionChannelId ::bigint AS p_collection_channel_id,
                                                CURRENT_DATE AS p_calc_date
                                        ),
                                        parsed AS (
                                            SELECT
                                                p.*,
                                                CASE WHEN p.p_customer_number ~ '^\\d{14}$' THEN left(p.p_customer_number, 10) END AS p_cn_10,
                                                CASE WHEN p.p_customer_number ~ '^\\d{14}$' THEN right(p.p_customer_number, 4) END AS p_bg_4
                                            FROM params p
                                        ),
                                        base_customer AS (
                                            SELECT
                                                c.id AS customer_id,
                                                c.customer_number::text AS customer_number_10,
                                                c.customer_type::text AS customer_type,
                                                c.identifier AS customer_identifier,
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
                                                cd.mailbox
                                            FROM customer.customers c
                                            JOIN customer.customer_details cd
                                              ON cd.id = c.last_customer_detail_id
                                            WHERE c.status::text = 'ACTIVE'
                                        ),
                                        projected_customer AS (
                                            SELECT
                                                b.customer_id,
                                                b.customer_number_10,
                                                b.customer_type,
                                                b.customer_identifier,
                                                b.customer_detail_id,
                                                CASE
                                                    WHEN b.customer_type = 'LEGAL_ENTITY'
                                                        THEN concat_ws(' ', NULLIF(b.name, ''), NULLIF(lf.name, ''))
                                                    ELSE concat_ws(' ', NULLIF(b.name, ''), NULLIF(b.middle_name, ''), NULLIF(b.last_name, ''))
                                                END AS customer_name,
                                                CASE
                                                    WHEN b.foreign_address = true THEN
                                                        concat_ws(', ',
                                                            NULLIF(ctry.name, ''), NULLIF(b.region_foreign, ''),
                                                            NULLIF(b.municipality_foreign, ''), NULLIF(b.populated_place_foreign, ''),
                                                            NULLIF(b.zip_code_foreign, ''), NULLIF(b.district_foreign, ''),
                                                            NULLIF(b.residential_area_foreign, ''), NULLIF(b.street_foreign, ''),
                                                            NULLIF(b.street_number, ''), NULLIF(b.block, ''),
                                                            NULLIF(b.entrance, ''), NULLIF(b.floor, ''),
                                                            NULLIF(b.apartment, ''), NULLIF(b.mailbox, '')
                                                        )
                                                    ELSE
                                                        concat_ws(', ',
                                                            NULLIF(ctry.name, ''), NULLIF(r.name, ''),
                                                            NULLIF(m.name, ''), NULLIF(pp.name, ''),
                                                            NULLIF(zc.zip_code, ''), NULLIF(d.name, ''),
                                                            NULLIF(ra.name, ''), NULLIF(s.name, ''),
                                                            NULLIF(b.street_number, ''), NULLIF(b.block, ''),
                                                            NULLIF(b.entrance, ''), NULLIF(b.floor, ''),
                                                            NULLIF(b.apartment, ''), NULLIF(b.mailbox, '')
                                                        )
                                                END AS customer_address
                                            FROM base_customer b
                                            LEFT JOIN nomenclature.legal_forms lf
                                              ON lf.id = b.legal_form_id
                                            LEFT JOIN nomenclature.countries ctry
                                              ON ctry.id = b.country_id
                                            LEFT JOIN nomenclature.populated_places pp
                                              ON pp.id = b.populated_place_id
                                            LEFT JOIN nomenclature.zip_codes zc
                                              ON zc.id = b.zip_code_id
                                            LEFT JOIN nomenclature.municipalities m
                                              ON m.id = pp.municipality_id
                                            LEFT JOIN nomenclature.regions r
                                              ON r.id = m.region_id
                                            LEFT JOIN nomenclature.districts d
                                              ON d.id = b.district_id
                                            LEFT JOIN nomenclature.residential_areas ra
                                              ON ra.id = b.residential_area_id
                                            LEFT JOIN nomenclature.streets s
                                              ON s.id = b.street_id
                                        ),
                                        channel_cfg AS (
                                            SELECT
                                                ch.id AS collection_channel_id,
                                                ch.combine_liabilities,
                                                ch.currency_id AS channel_currency_id,
                                                cur.name::text AS currency_name,
                                                ch.customer_condition_type::text AS cond_type,
                                                ch.customer_conditions,
                                                ch.list_of_customers,
                                                ch.exclude_liabilities_by_amount_less_than,
                                                ch.exclude_liabilities_by_amount_greater_than,
                                                mc.id AS main_currency_id
                                            FROM receivable.collection_channels ch
                                            JOIN nomenclature.currencies cur
                                              ON cur.id = ch.currency_id
                                            JOIN LATERAL (
                                                SELECT c0.id
                                                FROM nomenclature.currencies c0
                                                WHERE c0.main_ccy = true
                                                  AND c0.main_ccy_start_date <= CURRENT_DATE
                                                  AND c0.status::text = 'ACTIVE'
                                                ORDER BY c0.main_ccy_start_date DESC
                                                LIMIT 1
                                            ) mc ON true
                                            CROSS JOIN parsed pr
                                            WHERE ch.id = pr.p_collection_channel_id
                                              AND ch.status::text = 'ACTIVE'
                                        ),
                                        target_customer AS (
                                            SELECT pc.*
                                            FROM projected_customer pc
                                            CROSS JOIN parsed pr
                                            CROSS JOIN channel_cfg cc
                                            WHERE (
                                                    (pr.p_customer_number !~ '^\\d{14}$'
                                                     AND pc.customer_number_10 = pr.p_customer_number)
                                                 OR (pr.p_customer_number ~ '^\\d{14}$'
                                                     AND pc.customer_number_10 = pr.p_cn_10
                                                     AND EXISTS (
                                                         SELECT 1
                                                         FROM customer.customer_details cd_any
                                                         JOIN product_contract.contract_details pcd
                                                           ON pcd.customer_detail_id = cd_any.id
                                                          AND COALESCE(pcd.status::text, '') NOT IN ('DRAFT', 'CANCELLED')
                                                         JOIN product_contract.contracts pc2
                                                           ON pc2.id = pcd.contract_id
                                                          AND pc2.status::text = 'ACTIVE'
                                                         JOIN product_contract.contract_billing_groups cbg
                                                           ON cbg.contract_id = pc2.id
                                                          AND cbg.status::text = 'ACTIVE'
                                                         WHERE cd_any.customer_id = pc.customer_id
                                                           AND cbg.group_number = pr.p_bg_4
                                                     ))
                                                  )
                                              AND (
                                                  cc.cond_type = 'ALL_CUSTOMERS'
                                                  OR (cc.cond_type = 'LIST_OF_CUSTOMERS'
                                                      AND pc.customer_identifier = ANY(string_to_array(cc.list_of_customers, ',')))
                                                  OR (cc.cond_type = 'CUSTOMERS_UNDER_CONDITIONS'
                                                      AND (
                                                          receivable.customer_condition_eval(pc.customer_id, cc.customer_conditions) > 0
                                                          OR EXISTS (
                                                              SELECT 1
                                                              FROM receivable.customer_liabilities l0
                                                              WHERE l0.customer_id = pc.customer_id
                                                                AND l0.status::text = 'ACTIVE'
                                                                AND receivable.liability_condition_eval_exact2(l0.id, cc.customer_conditions) > 0
                                                          )
                                                      ))
                                              )
                                        ),
                                        candidate_liabilities AS (
                                            SELECT
                                                tc.customer_id,
                                                tc.customer_identifier,
                                                tc.customer_number_10,
                                                tc.customer_name,
                                                tc.customer_address,
                                                l.id AS liability_id,
                                                l.liability_number,
                                                l.currency_id,
                                                l.current_amount,
                                                CASE
                                                    WHEN l.currency_id = cc.channel_currency_id THEN l.current_amount
                                                    ELSE COALESCE(l.current_amount_in_other_ccy, l.current_amount)
                                                END AS principal,
                                                l.due_date,
                                                l.occurrence_date,
                                                l.invoice_id,
                                                l.outgoing_document_from_external_system,
                                                l.contract_billing_group_id,
                                                l.action_id,
                                                l.claimed_penalty_id,
                                                l.late_payment_fine_id,
                                                l.rescheduling_id,
                                                l.deposit_id
                                            FROM target_customer tc
                                            JOIN receivable.customer_liabilities l
                                              ON l.customer_id = tc.customer_id
                                             AND l.status::text = 'ACTIVE'
                                            CROSS JOIN parsed pr
                                            CROSS JOIN channel_cfg cc
                                            WHERE l.current_amount > 0
                                              AND receivable.check_liability_payment_allowed(l.id, pr.p_calc_date) = true
                                              AND NOT (
                                                  COALESCE(l.blocked_for_liabilities_offsetting, false)
                                                  AND pr.p_calc_date <@ daterange(
                                                      COALESCE(l.blocked_for_liabilities_offsetting_from_date, '-infinity'::date),
                                                      COALESCE(l.blocked_for_liabilities_offsetting_to_date, 'infinity'::date),
                                                      '[]'
                                                  )
                                              )
                                              AND NOT EXISTS (
                                                  SELECT 1
                                                  FROM receivable.mass_operation_for_blocking mofb
                                                  WHERE mofb.status::text <> 'DELETED'
                                                    AND mofb.mass_operation_blocking_status::text = 'EXECUTED'
                                                    AND 'CUSTOMER_LIABILITY' = ANY(mofb."type")
                                                    AND COALESCE(mofb.blocked_for_liabilities_offsetting, false)
                                                    AND pr.p_calc_date <@ daterange(
                                                        COALESCE(mofb.blocked_for_liabilities_offsetting_from_date, '-infinity'::date),
                                                        COALESCE(mofb.blocked_for_liabilities_offsetting_to_date, 'infinity'::date),
                                                        '[]'
                                                    )
                                                    AND (
                                                        mofb.customer_condition_type::text = 'ALL_CUSTOMERS'
                                                        OR (mofb.customer_condition_type::text = 'CUSTOMERS_UNDER_CONDITIONS'
                                                            AND receivable.customer_condition_eval(tc.customer_id, mofb.customer_conditions) > 0)
                                                        OR (mofb.customer_condition_type::text = 'LIST_OF_CUSTOMERS'
                                                            AND tc.customer_identifier = ANY(string_to_array(mofb.list_of_customers, ',')))
                                                    )
                                                    AND NOT EXISTS (
                                                        SELECT 1
                                                        FROM receivable.mass_operation_for_blocking_exclution_prefixes mofbep
                                                        JOIN nomenclature.prefixes p
                                                          ON p.id = mofbep.prefix_id
                                                        LEFT JOIN invoice.invoices i_mofb
                                                          ON i_mofb.id = l.invoice_id
                                                        WHERE mofbep.status::text <> 'DELETED'
                                                          AND p.status::text <> 'DELETED'
                                                          AND mofbep.mass_operation_for_blocking_id = mofb.id
                                                          AND COALESCE(i_mofb.invoice_number, '') LIKE ('%' || p.name || '-%')
                                                    )
                                                    AND (
                                                        mofb.exclusion_by_amount_less_than IS NULL
                                                        OR (mofb.currency_id = l.currency_id
                                                            AND mofb.exclusion_by_amount_less_than <= l.initial_amount)
                                                        OR (mofb.currency_id IS DISTINCT FROM l.currency_id
                                                            AND receivable.convert_to_currency(mofb.exclusion_by_amount_less_than, mofb.currency_id, 0)
                                                                <= receivable.convert_to_currency(l.initial_amount, l.currency_id, 0))
                                                    )
                                                    AND (
                                                        mofb.exclusion_by_amount_greater_than IS NULL
                                                        OR (mofb.currency_id = l.currency_id
                                                            AND mofb.exclusion_by_amount_greater_than >= l.initial_amount)
                                                        OR (mofb.currency_id IS DISTINCT FROM l.currency_id
                                                            AND receivable.convert_to_currency(mofb.exclusion_by_amount_greater_than, mofb.currency_id, 0)
                                                                >= receivable.convert_to_currency(l.initial_amount, l.currency_id, 0))
                                                    )
                                              )
                                              AND (
                                                  cc.cond_type <> 'CUSTOMERS_UNDER_CONDITIONS'
                                                  OR receivable.liability_condition_eval_exact2(l.id, cc.customer_conditions) > 0
                                              )
                                              AND (
                                                  pr.p_customer_number !~ '^\\d{14}$'
                                                  OR EXISTS (
                                                      SELECT 1
                                                      FROM product_contract.contract_billing_groups cbg
                                                      WHERE cbg.id = l.contract_billing_group_id
                                                        AND cbg.status::text = 'ACTIVE'
                                                        AND cbg.group_number = pr.p_bg_4
                                                  )
                                              )
                                        ),
                                        filtered_liabilities AS (
                                            SELECT
                                                cl.*,
                                                i.basis_for_issuing,
                                                i.meter_reading_period_from,
                                                i.meter_reading_period_to,
                                                COALESCE(
                                                    i.invoice_number,
                                                    act.action_number,
                                                    cp.prefix || '-' || lpad(cp.id::text, 10, '0'),
                                                    lpf.late_payment_number,
                                                    rsch.rescheduling_number,
                                                    dep.deposit_number
                                                ) AS outgoing_doc_number,
                                                split_part(
                                                    COALESCE(
                                                        i.invoice_number,
                                                        act.action_number,
                                                        cp.prefix || '-' || lpad(cp.id::text, 10, '0'),
                                                        lpf.late_payment_number,
                                                        rsch.rescheduling_number,
                                                        dep.deposit_number,
                                                        cl.outgoing_document_from_external_system,
                                                        ''
                                                    ),
                                                    '-',
                                                    1
                                                ) AS doc_prefix
                                            FROM candidate_liabilities cl
                                            CROSS JOIN parsed pr
                                            CROSS JOIN channel_cfg cc
                                            LEFT JOIN invoice.invoices i
                                              ON i.id = cl.invoice_id
                                            LEFT JOIN action.actions act
                                              ON act.id = cl.action_id
                                            LEFT JOIN action.claimed_penalty cp
                                              ON cp.id = cl.claimed_penalty_id
                                            LEFT JOIN receivable.late_payment_fines lpf
                                              ON lpf.id = cl.late_payment_fine_id
                                            LEFT JOIN receivable.reschedulings rsch
                                              ON rsch.id = cl.rescheduling_id
                                            LEFT JOIN receivable.customer_deposits dep
                                              ON dep.id = cl.deposit_id
                                            WHERE
                                                (
                                                    cc.exclude_liabilities_by_amount_less_than IS NULL
                                                    OR (
                                                        cl.currency_id = cc.channel_currency_id
                                                        AND cl.current_amount >= cc.exclude_liabilities_by_amount_less_than
                                                    )
                                                    OR (
                                                        cl.currency_id IS DISTINCT FROM cc.channel_currency_id
                                                        AND receivable.convert_to_currency(cl.current_amount, cl.currency_id, 0)
                                                            >= receivable.convert_to_currency(cc.exclude_liabilities_by_amount_less_than, cc.channel_currency_id, 0)
                                                    )
                                                )
                                              AND
                                                (
                                                    cc.exclude_liabilities_by_amount_greater_than IS NULL
                                                    OR (
                                                        cl.currency_id = cc.channel_currency_id
                                                        AND cl.current_amount <= cc.exclude_liabilities_by_amount_greater_than
                                                    )
                                                    OR (
                                                        cl.currency_id IS DISTINCT FROM cc.channel_currency_id
                                                        AND receivable.convert_to_currency(cl.current_amount, cl.currency_id, 0)
                                                            <= receivable.convert_to_currency(cc.exclude_liabilities_by_amount_greater_than, cc.channel_currency_id, 0)
                                                    )
                                                )
                                              AND NOT EXISTS (
                                                  SELECT 1
                                                  FROM receivable.collection_channel_exclude_liab_prefixes ex
                                                  JOIN nomenclature.prefixes pex
                                                    ON pex.id = ex.prefix_id
                                                   AND pex.status::text = 'ACTIVE'
                                                  WHERE ex.collection_channel_id = pr.p_collection_channel_id
                                                    AND ex.status::text = 'ACTIVE'
                                                    AND pex.name = split_part(
                                                        COALESCE(
                                                            i.invoice_number,
                                                            act.action_number,
                                                            cp.prefix || '-' || lpad(cp.id::text, 10, '0'),
                                                            lpf.late_payment_number,
                                                            rsch.rescheduling_number,
                                                            dep.deposit_number,
                                                            cl.outgoing_document_from_external_system,
                                                            ''
                                                        ),
                                                        '-',
                                                        1
                                                    )
                                              )
                                        ),
                                        bg_single AS (
                                            SELECT
                                                cp.contract_billing_group_id,
                                                MIN(p.id) AS pod_id
                                            FROM product_contract.contract_pods cp
                                            JOIN pod.pod_details pd
                                              ON pd.id = cp.pod_detail_id
                                            JOIN pod.pod p
                                              ON p.id = pd.pod_id
                                            WHERE cp.status::text = 'ACTIVE'
                                              AND cp.contract_billing_group_id IN (
                                                  SELECT fl.contract_billing_group_id
                                                  FROM filtered_liabilities fl
                                                  WHERE fl.contract_billing_group_id IS NOT NULL
                                              )
                                            GROUP BY cp.contract_billing_group_id
                                            HAVING COUNT(DISTINCT p.id) = 1
                                        ),
                                        bg_pod_address AS (
                                            SELECT
                                                bgs.contract_billing_group_id,
                                                CASE
                                                    WHEN COALESCE(f.foreign_address, false) = true THEN
                                                        concat_ws(', ',
                                                            NULLIF(ctry.name, ''), NULLIF(f.region_foreign, ''),
                                                            NULLIF(f.municipality_foreign, ''), NULLIF(f.populated_place_foreign, ''),
                                                            NULLIF(f.zip_code_foreign, ''), NULLIF(f.district_foreign, ''),
                                                            NULLIF(f.residential_area_foreign, ''), NULLIF(f.street_foreign, ''),
                                                            NULLIF(f.street_number, ''), NULLIF(f.block, ''),
                                                            NULLIF(f.entrance, ''), NULLIF(f.floor, ''),
                                                            NULLIF(f.apartment, ''), NULLIF(f.mailbox, ''),
                                                            NULLIF(f.address_additional_info, '')
                                                        )
                                                    ELSE
                                                        concat_ws(', ',
                                                            NULLIF(ctry.name, ''), NULLIF(r.name, ''),
                                                            NULLIF(m.name, ''), NULLIF(pp.name, ''),
                                                            NULLIF(zc.zip_code, ''), NULLIF(d.name, ''),
                                                            NULLIF(ra.name, ''), NULLIF(s.name, ''),
                                                            NULLIF(f.street_number, ''), NULLIF(f.block, ''),
                                                            NULLIF(f.entrance, ''), NULLIF(f.floor, ''),
                                                            NULLIF(f.apartment, ''), NULLIF(f.mailbox, ''),
                                                            NULLIF(f.address_additional_info, '')
                                                        )
                                                END AS pod_address
                                            FROM bg_single bgs
                                            JOIN pod.pod p
                                              ON p.id = bgs.pod_id
                                            JOIN pod.pod_details f
                                              ON f.id = p.last_pod_detail_id
                                            LEFT JOIN nomenclature.countries ctry
                                              ON ctry.id = f.country_id
                                            LEFT JOIN nomenclature.populated_places pp
                                              ON pp.id = f.populated_place_id
                                            LEFT JOIN nomenclature.zip_codes zc
                                              ON zc.id = f.zip_code_id
                                            LEFT JOIN nomenclature.municipalities m
                                              ON m.id = pp.municipality_id
                                            LEFT JOIN nomenclature.regions r
                                              ON r.id = m.region_id
                                            LEFT JOIN nomenclature.districts d
                                              ON d.id = f.district_id
                                            LEFT JOIN nomenclature.residential_areas ra
                                              ON ra.id = f.residential_area_id
                                            LEFT JOIN nomenclature.streets s
                                              ON s.id = f.street_id
                                        ),
                                        inv_pod_ids AS (
                                            SELECT isdd.invoice_id, isdd.pod_id
                                            FROM invoice.invoice_standard_detailed_data isdd
                                            WHERE isdd.invoice_id IN (
                                                SELECT fl.invoice_id
                                                FROM filtered_liabilities fl
                                                WHERE fl.invoice_id IS NOT NULL
                                            )
                                              AND isdd.pod_id IS NOT NULL
                                            UNION
                                            SELECT idd.invoice_id, idd.pod_id
                                            FROM invoice.invoice_detailed_data idd
                                            WHERE idd.invoice_id IN (
                                                SELECT fl.invoice_id
                                                FROM filtered_liabilities fl
                                                WHERE fl.invoice_id IS NOT NULL
                                            )
                                              AND idd.pod_id IS NOT NULL
                                            UNION
                                            SELECT i.id AS invoice_id, i.pod_id
                                            FROM invoice.invoices i
                                            WHERE i.id IN (
                                                SELECT fl.invoice_id
                                                FROM filtered_liabilities fl
                                                WHERE fl.invoice_id IS NOT NULL
                                            )
                                              AND i.pod_id IS NOT NULL
                                        ),
                                        inv_single_pod AS (
                                            SELECT
                                                ip.invoice_id,
                                                MIN(p.identifier) AS pod_identifier
                                            FROM inv_pod_ids ip
                                            JOIN pod.pod p
                                              ON p.id = ip.pod_id
                                            GROUP BY ip.invoice_id
                                            HAVING COUNT(DISTINCT ip.pod_id) = 1
                                        ),
                                        enriched_liabilities AS (
                                            SELECT
                                                fl.*,
                                                COALESCE(bpa.pod_address, '') AS pod_address,
                                                isp.pod_identifier AS invoice_pod_identifier
                                            FROM filtered_liabilities fl
                                            LEFT JOIN bg_pod_address bpa
                                              ON bpa.contract_billing_group_id = fl.contract_billing_group_id
                                            LEFT JOIN inv_single_pod isp
                                              ON isp.invoice_id = fl.invoice_id
                                        ),
                                        with_interest AS (
                                            SELECT
                                                fl.*,
                                                CASE
                                                    WHEN cc.main_currency_id = cc.channel_currency_id
                                                        THEN receivable.calculate_lfp(fl.liability_id, (SELECT p_calc_date FROM parsed))
                                                    ELSE receivable.convert_to_currency(
                                                        receivable.calculate_lfp(fl.liability_id, (SELECT p_calc_date FROM parsed)),
                                                        cc.main_currency_id,
                                                        cc.channel_currency_id,
                                                        2
                                                    )
                                                END AS interest
                                            FROM enriched_liabilities fl
                                            CROSS JOIN channel_cfg cc
                                        ),
                                        priority_flagged AS (
                                            SELECT
                                                wi.*,
                                                EXISTS (
                                                    SELECT 1
                                                    FROM receivable.collection_channel_priority_liab_prefixes pprio
                                                    JOIN nomenclature.prefixes pp
                                                      ON pp.id = pprio.prefix_id
                                                     AND pp.status::text = 'ACTIVE'
                                                    WHERE pprio.collection_channel_id = (SELECT p_collection_channel_id FROM params)
                                                      AND pprio.status::text = 'ACTIVE'
                                                      AND pp.name = wi.doc_prefix
                                                ) AS is_priority_match
                                            FROM with_interest wi
                                        ),
                                        priority_ranked AS (
                                            SELECT
                                                pf.*,
                                                ROW_NUMBER() OVER (
                                                    ORDER BY
                                                        CASE WHEN pf.is_priority_match THEN 0 ELSE 1 END,
                                                        pf.due_date NULLS LAST,
                                                        pf.liability_id
                                                ) AS rn_for_payment,
                                                COUNT(*) FILTER (WHERE pf.is_priority_match) OVER () AS matched_priority_count
                                            FROM priority_flagged pf
                                        ),
                                        has_priority AS (
                                            SELECT EXISTS (
                                                SELECT 1
                                                FROM receivable.collection_channel_priority_liab_prefixes p0
                                                WHERE p0.collection_channel_id = (SELECT p_collection_channel_id FROM params)
                                                  AND p0.status::text = 'ACTIVE'
                                            ) AS val
                                        ),
                                        non_combine_rows AS (
                                            SELECT
                                                CASE
                                                    WHEN pr.p_customer_number ~ '^\\d{14}$'
                                                        THEN prr.customer_number_10 || pr.p_bg_4
                                                    ELSE prr.customer_number_10
                                                END AS "CustomerNumber",
                                                prr.customer_name AS "CustomerName",
                                                COALESCE(prr.pod_address, '') AS "CustomerAddress",
                                                COALESCE(
                                                    NULLIF(split_part(NULLIF(prr.outgoing_doc_number, ''), '-', 2), ''),
                                                    NULLIF(prr.outgoing_doc_number, ''),
                                                    NULLIF(split_part(NULLIF(prr.outgoing_document_from_external_system, ''), '-', 2), ''),
                                                    NULLIF(prr.outgoing_document_from_external_system, ''),
                                                    NULLIF(prr.liability_number, ''),
                                                    prr.liability_number
                                                ) AS "DocumentNumber",
                                                to_char(prr.occurrence_date, 'DD.MM.YYYY') AS "DocumentDate",
                                                CASE
                                                    WHEN prr.basis_for_issuing IS NULL
                                                     AND prr.invoice_pod_identifier IS NULL
                                                     AND prr.meter_reading_period_from IS NULL
                                                     AND prr.meter_reading_period_to IS NULL
                                                        THEN ''
                                                    ELSE concat_ws(', ',
                                                        NULLIF(prr.basis_for_issuing, ''),
                                                        NULLIF(prr.invoice_pod_identifier, ''),
                                                        CASE
                                                            WHEN prr.meter_reading_period_from IS NOT NULL
                                                              OR prr.meter_reading_period_to IS NOT NULL
                                                                THEN concat(
                                                                    COALESCE(to_char(prr.meter_reading_period_from, 'DD.MM.YYYY'), ''),
                                                                    '-',
                                                                    COALESCE(to_char(prr.meter_reading_period_to, 'DD.MM.YYYY'), '')
                                                                )
                                                        END
                                                    )
                                                END AS "DocumentInfo",
                                                CASE
                                                    WHEN hp.val THEN
                                                        CASE
                                                            WHEN prr.matched_priority_count > 0 THEN prr.rn_for_payment = 1
                                                            ELSE true
                                                        END
                                                    ELSE true
                                                END AS "AllowedForPayment",
                                                false AS "AllowedPartialPayment",
                                                cc.currency_name AS "Currency",
                                                prr.principal AS "Principal",
                                                prr.interest AS "Interest",
                                                0.00 AS "Another"
                                            FROM priority_ranked prr
                                            CROSS JOIN parsed pr
                                            CROSS JOIN channel_cfg cc
                                            CROSS JOIN has_priority hp
                                            WHERE cc.combine_liabilities = false
                                        ),
                                        combine_row AS (
                                            SELECT
                                                CASE
                                                    WHEN pr.p_customer_number ~ '^\\d{14}$'
                                                        THEN min(prr.customer_number_10) || pr.p_bg_4
                                                    ELSE min(prr.customer_number_10)
                                                END AS "CustomerNumber",
                                                min(prr.customer_name) AS "CustomerName",
                                                CASE
                                                    WHEN COUNT(DISTINCT NULLIF(prr.pod_address, '')) = 1
                                                        THEN MAX(NULLIF(prr.pod_address, ''))
                                                    ELSE ''
                                                END AS "CustomerAddress",
                                                string_agg(
                                                    COALESCE(
                                                        NULLIF(split_part(NULLIF(prr.outgoing_doc_number, ''), '-', 2), ''),
                                                        NULLIF(prr.outgoing_doc_number, ''),
                                                        NULLIF(split_part(NULLIF(prr.outgoing_document_from_external_system, ''), '-', 2), ''),
                                                        NULLIF(prr.outgoing_document_from_external_system, ''),
                                                        NULLIF(prr.liability_number, ''),
                                                        prr.liability_number
                                                    ),
                                                    ', ' ORDER BY prr.due_date NULLS LAST, prr.liability_id
                                                ) AS "DocumentNumber",
                                                ''::text AS "DocumentDate",
                                                string_agg(
                                                    NULLIF(
                                                        CASE
                                                            WHEN prr.basis_for_issuing IS NULL
                                                             AND prr.invoice_pod_identifier IS NULL
                                                             AND prr.meter_reading_period_from IS NULL
                                                             AND prr.meter_reading_period_to IS NULL
                                                                THEN ''
                                                            ELSE concat_ws(', ',
                                                                NULLIF(prr.basis_for_issuing, ''),
                                                                NULLIF(prr.invoice_pod_identifier, ''),
                                                                CASE
                                                                    WHEN prr.meter_reading_period_from IS NOT NULL
                                                                      OR prr.meter_reading_period_to IS NOT NULL
                                                                        THEN concat(
                                                                            COALESCE(to_char(prr.meter_reading_period_from, 'DD.MM.YYYY'), ''),
                                                                            '-',
                                                                            COALESCE(to_char(prr.meter_reading_period_to, 'DD.MM.YYYY'), '')
                                                                        )
                                                                END
                                                            )
                                                        END,
                                                        ''
                                                    ),
                                                    '; ' ORDER BY prr.due_date NULLS LAST, prr.liability_id
                                                ) AS "DocumentInfo",
                                                TRUE AS "AllowedForPayment",
                                                FALSE AS "AllowedPartialPayment",
                                                cc.currency_name AS "Currency",
                                                SUM(prr.principal) AS "Principal",
                                                SUM(prr.interest) AS "Interest",
                                                0.00 AS "Another"
                                            FROM priority_ranked prr
                                            CROSS JOIN parsed pr
                                            CROSS JOIN channel_cfg cc
                                            WHERE cc.combine_liabilities = true
                                            GROUP BY cc.currency_name, pr.p_customer_number, pr.p_bg_4
                                        )
                                        SELECT * FROM non_combine_rows
                                        UNION ALL
                                        SELECT * FROM combine_row
            """, nativeQuery = true)
    List<BulgariaPostObligationsProjection> findObligations(
            @Param("customerNumber") String customerNumber,
            @Param("collectionChannelId") Long collectionChannelId
    );

    @Query(value = """
            WITH params AS (
                SELECT
                    :collectionChannelId ::bigint AS cc_id,
                    NULLIF(:customerNumber, '')::text AS p_customer_number
            ),
                 parsed AS (
                     SELECT
                         p.*,
                         CASE WHEN p.p_customer_number ~ '^\\d{10}$' THEN p.p_customer_number END AS p_cn_10,
                         CASE WHEN p.p_customer_number ~ '^\\d{14}$' THEN left(p.p_customer_number, 10) END AS p_cn_10_from_14,
                         CASE WHEN p.p_customer_number ~ '^\\d{14}$' THEN right(p.p_customer_number, 4) END AS p_bg_4
                     FROM params p
                 ),
                 cust AS (
                     SELECT
                         c.id AS customer_id,
                         c.customer_number::text AS customer_number_10,
                         c.customer_type::text AS customer_type,
                         cd.id AS customer_detail_id,
                         cd.name,
                         cd.middle_name,
                         cd.last_name,
                         cd.legal_form_id,
                         cd.vat_number,
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
                     FROM customer.customers c
                              JOIN customer.customer_details cd
                                   ON cd.id = c.last_customer_detail_id
                     WHERE c.status::text = 'ACTIVE'
                 ),
                 projected_customer AS (
                     SELECT
                         c.customer_id,
                         c.customer_detail_id,
                         c.customer_number_10,
                         c.customer_type,
                         CASE
                             WHEN c.customer_type = 'LEGAL_ENTITY'
                                 THEN concat_ws(' ', NULLIF(c.name, ''), NULLIF(lf.name, ''))
                             ELSE concat_ws(' ', NULLIF(c.name, ''), NULLIF(c.middle_name, ''), NULLIF(c.last_name, ''))
                             END AS customer_name,
                         CASE
                             WHEN c.foreign_address = true THEN
                                 concat_ws(
                                         ', ',
                                         NULLIF(ctry.name, ''),
                                         NULLIF(c.region_foreign, ''),
                                         NULLIF(c.municipality_foreign, ''),
                                         NULLIF(c.populated_place_foreign, ''),
                                         NULLIF(c.zip_code_foreign, ''),
                                         NULLIF(c.district_foreign, ''),
                                         NULLIF(c.residential_area_foreign, ''),
                                         NULLIF(c.street_foreign, ''),
                                         NULLIF(c.street_number, ''),
                                         NULLIF(c.block, ''),
                                         NULLIF(c.entrance, ''),
                                         NULLIF(c.floor, ''),
                                         NULLIF(c.apartment, ''),
                                         NULLIF(c.mailbox, ''),
                                         NULLIF(c.address_additional_info, '')
                                 )
                             ELSE
                                 concat_ws(
                                         ', ',
                                         NULLIF(ctry.name, ''),
                                         NULLIF(r.name, ''),
                                         NULLIF(m.name, ''),
                                         NULLIF(pp.name, ''),
                                         NULLIF(zc.zip_code, ''),
                                         NULLIF(d.name, ''),
                                         NULLIF(ra.name, ''),
                                         NULLIF(s.name, ''),
                                         NULLIF(c.street_number, ''),
                                         NULLIF(c.block, ''),
                                         NULLIF(c.entrance, ''),
                                         NULLIF(c.floor, ''),
                                         NULLIF(c.apartment, ''),
                                         NULLIF(c.mailbox, ''),
                                         NULLIF(c.address_additional_info, '')
                                 )
                             END AS customer_address
                     FROM cust c
                              LEFT JOIN nomenclature.legal_forms lf
                                        ON lf.id = c.legal_form_id
                              LEFT JOIN nomenclature.countries ctry
                                        ON ctry.id = c.country_id
                              LEFT JOIN nomenclature.populated_places pp
                                        ON pp.id = c.populated_place_id
                              LEFT JOIN nomenclature.zip_codes zc
                                        ON zc.id = c.zip_code_id
                              LEFT JOIN nomenclature.municipalities m
                                        ON m.id = pp.municipality_id
                              LEFT JOIN nomenclature.regions r
                                        ON r.id = m.region_id
                              LEFT JOIN nomenclature.districts d
                                        ON d.id = c.district_id
                              LEFT JOIN nomenclature.residential_areas ra
                                        ON ra.id = c.residential_area_id
                              LEFT JOIN nomenclature.streets s
                                        ON s.id = c.street_id
                 ),
                 matched_customer AS (
                     SELECT
                         pc.*
                     FROM projected_customer pc
                              CROSS JOIN parsed pr
                     WHERE
                         (
                             (pr.p_customer_number ~ '^\\d{10}$' AND pc.customer_number_10 = pr.p_cn_10)
                                 OR (pr.p_customer_number ~ '^\\d{14}$' AND pc.customer_number_10 = pr.p_cn_10_from_14)
                             )
                       AND (
                         pr.p_bg_4 IS NULL
                             OR EXISTS (
                             SELECT 1
                             FROM customer.customer_details cd_any
                                      JOIN product_contract.contract_details pcd
                                           ON pcd.customer_detail_id = cd_any.id
                                               AND COALESCE(pcd.status::text, '') NOT IN ('DRAFT', 'CANCELLED')
                                      JOIN product_contract.contracts pc0
                                           ON pc0.id = pcd.contract_id
                                               AND pc0.status::text = 'ACTIVE'
                                      JOIN product_contract.contract_billing_groups cbg
                                           ON cbg.contract_id = pc0.id
                                               AND cbg.status::text = 'ACTIVE'
                             WHERE cd_any.customer_id = pc.customer_id
                               AND cbg.group_number = pr.p_bg_4
                         )
                         )
                 ),
                 eligible_liabilities AS (
                     SELECT
                         cl.id AS liability_id
                     FROM receivable.customer_liabilities cl
                              CROSS JOIN parsed pr
                              JOIN matched_customer mc ON mc.customer_id = cl.customer_id
                              LEFT JOIN product_contract.contract_billing_groups cbg ON cbg.id = cl.contract_billing_group_id AND cbg.status = 'ACTIVE'
                              LEFT JOIN receivable.collection_channels cc ON cc.id = pr.cc_id AND cc.status = 'ACTIVE'
                     WHERE cl.status = 'ACTIVE'
                       AND cl.current_amount > 0
                       AND COALESCE(cl.blocked_for_payment, FALSE) = FALSE
                       AND COALESCE(cl.blocked_for_liabilities_offsetting, FALSE) = FALSE
                       AND (pr.p_bg_4 IS NULL OR cbg.group_number = pr.p_bg_4)
                       AND (
                         CASE
                             WHEN cc.customer_condition_type = 'LIST_OF_CUSTOMERS'
                                 THEN trim((SELECT c0.identifier FROM customer.customers c0 WHERE c0.id = cl.customer_id)) =
                                      ANY(ARRAY(SELECT trim(both ' ' from unnest(string_to_array(cc.list_of_customers, ',')))))
                             ELSE receivable.liability_condition_eval_exact(cl.id, cc.customer_conditions) > 0
                             END
                         )
                 ),
                 excluded AS (
                     SELECT DISTINCT cl.id AS excluded_liability_id
                     FROM receivable.customer_liabilities cl
                              CROSS JOIN parsed pr
                              LEFT JOIN invoice.invoices inv ON cl.invoice_id = inv.id AND inv.status = 'REAL'
                              LEFT JOIN product.product_details prod_d ON prod_d.id = inv.product_detail_id AND prod_d.status = 'ACTIVE'
                              LEFT JOIN service.service_details serv_d ON serv_d.id = inv.service_detail_id AND serv_d.status = 'ACTIVE'
                              LEFT JOIN product.product_details_collection_channels pdcc ON pdcc.product_details_id = prod_d.id AND pdcc.status = 'ACTIVE'
                              LEFT JOIN service.service_details_collection_channels sdcc ON sdcc.service_details_id = serv_d.id AND sdcc.status = 'ACTIVE'
                              LEFT JOIN receivable.collection_channels cc ON (cc.id = pdcc.collection_channel_id OR cc.id = sdcc.collection_channel_id) AND cc.status = 'ACTIVE'
                              LEFT JOIN receivable.collection_channel_exclude_liab_prefixes ccelp ON ccelp.collection_channel_id = cc.id AND ccelp.status = 'ACTIVE'
                              LEFT JOIN nomenclature.prefixes p ON p.id = ccelp.prefix_id AND p.status = 'ACTIVE'
                     WHERE (pdcc.collection_channel_id = pr.cc_id OR sdcc.collection_channel_id = pr.cc_id)
                       AND (ccelp.collection_channel_id = pr.cc_id)
                       AND (p.name = substring(cl.liability_number, 1, length(p.name)))
                 )
            SELECT
                cl.id AS liabilityId,
                cl.liability_number AS liabilityNumber,
                cl.occurrence_date AS occurrenceDate,
                cl.due_date AS dueDate,
                CASE
                    WHEN cl.currency_id = cc.currency_id THEN cl.current_amount
                    ELSE COALESCE(cl.current_amount_in_other_ccy, cl.current_amount)
                    END AS currentAmount,
                cl.currency_id AS currencyId,
                cl.outgoing_document_from_external_system AS outgoingDocumentFromExternalSystem,
                inv.invoice_number AS invoiceNumber,
                COALESCE(
                    inv.invoice_number,
                    act.action_number,
                    cp.prefix || '-' || lpad(cp.id::text, 10, '0'),
                    lpf.late_payment_number,
                    rsch.rescheduling_number,
                    dep.deposit_number
                ) AS outgoingDocumentNumber,
                inv.basis_for_issuing AS basisForIssuing,
                inv.meter_reading_period_from AS meterReadingPeriodFrom,
                inv.meter_reading_period_to AS meterReadingPeriodTo,
                mc.customer_name AS customerName,
                mc.customer_address AS customerAddress,
                CASE
                    WHEN pr.p_bg_4 IS NULL THEN mc.customer_number_10
                    ELSE mc.customer_number_10 || pr.p_bg_4
                    END AS customerNumber,
                pr.p_bg_4 AS billingGroup,
                curr.name AS Currency,
                cl.invoice_id AS invoiceId,
                text(cl.outgoing_document_type) AS outgoingDocumentType,
                cl.late_payment_fine_id AS latePaymentFineId,
                cl.action_id AS actionId,
                cl.claimed_penalty_id AS claimedPenaltyId,
                cl.rescheduling_id AS reschedulingId,
                cl.deposit_id AS depositId
            FROM receivable.customer_liabilities cl
                     CROSS JOIN parsed pr
                     JOIN eligible_liabilities el ON el.liability_id = cl.id
                     LEFT JOIN excluded ex ON ex.excluded_liability_id = cl.id
                     JOIN matched_customer mc ON mc.customer_id = cl.customer_id
                     LEFT JOIN invoice.invoices inv ON inv.id = cl.invoice_id AND inv.status = 'REAL'
                     LEFT JOIN action.actions act ON act.id = cl.action_id
                     LEFT JOIN action.claimed_penalty cp ON cp.id = cl.claimed_penalty_id
                     LEFT JOIN receivable.late_payment_fines lpf ON lpf.id = cl.late_payment_fine_id
                     LEFT JOIN receivable.reschedulings rsch ON rsch.id = cl.rescheduling_id
                     LEFT JOIN receivable.customer_deposits dep ON dep.id = cl.deposit_id
                     LEFT JOIN receivable.collection_channels cc ON cc.id = pr.cc_id AND cc.status = 'ACTIVE'
                     LEFT JOIN nomenclature.currencies curr ON curr.id = cc.currency_id AND curr.status = 'ACTIVE'
            WHERE ex.excluded_liability_id IS NULL
            ORDER BY cl.due_date NULLS LAST, cl.id
            """, nativeQuery = true)
    List<BulgariaPostLiabilitiesProjection> findEligibleLiabilities(
            @Param("collectionChannelId") Long collectionChannelId,
            @Param("customerNumber") String customerNumber
    );

    @Query(value = """
            WITH params AS (
                SELECT
                    NULLIF(:customerNumber, '')::text AS p_customer_number
            ),
                 parsed AS (
                     SELECT
                         p.*,
                         CASE WHEN p.p_customer_number ~ '^\\d{10}$' THEN p.p_customer_number END AS p_cn_10,
                         CASE WHEN p.p_customer_number ~ '^\\d{14}$' THEN left(p.p_customer_number, 10) END AS p_cn_10_from_14,
                         CASE WHEN p.p_customer_number ~ '^\\d{14}$' THEN right(p.p_customer_number, 4) END AS p_bg_4
                     FROM params p
                 )
            SELECT EXISTS(
                SELECT 1
                FROM customer.customers c
                         CROSS JOIN parsed pr
                WHERE c.status::text = 'ACTIVE'
                  AND (
                    (pr.p_customer_number ~ '^\\d{10}$' AND c.customer_number::text = pr.p_cn_10)
                        OR (pr.p_customer_number ~ '^\\d{14}$' AND c.customer_number::text = pr.p_cn_10_from_14)
                    )
                  AND (
                    pr.p_bg_4 IS NULL
                        OR EXISTS (
                        SELECT 1
                        FROM customer.customer_details cd_any
                                 JOIN product_contract.contract_details pcd
                                      ON pcd.customer_detail_id = cd_any.id
                                          AND COALESCE(pcd.status::text, '') NOT IN ('DRAFT', 'CANCELLED')
                                 JOIN product_contract.contracts pc0
                                      ON pc0.id = pcd.contract_id
                                          AND pc0.status::text = 'ACTIVE'
                                 JOIN product_contract.contract_billing_groups cbg
                                      ON cbg.contract_id = pc0.id
                                          AND cbg.status::text = 'ACTIVE'
                        WHERE cd_any.customer_id = c.id
                          AND cbg.group_number = pr.p_bg_4
                    )
                    )
            )
            """, nativeQuery = true)
    boolean existsValidCustomerNumber(@Param("customerNumber") String customerNumber);

    /**
     * Converts {@code amount} from {@code fromCurrencyId} to {@code toCurrencyId}
     * using {@code receivable.convert_to_currency}, rounded to {@code scale} decimals.
     * Same conversion used for obligations/receipt LPF when the LPF currency ≠ channel currency.
     * <p>
     * Currency ids are cast to {@code integer} because the DB function signature is
     * {@code (numeric, integer, integer, integer)} and JPA binds {@code Long} as {@code bigint}.
     */
    @Query(value = """
            SELECT receivable.convert_to_currency(
                :amount,
                CAST(:fromCurrencyId AS integer),
                CAST(:toCurrencyId AS integer),
                CAST(:scale AS integer)
            )
            """, nativeQuery = true)
    BigDecimal convertToCurrency(
            @Param("amount") BigDecimal amount,
            @Param("fromCurrencyId") Long fromCurrencyId,
            @Param("toCurrencyId") Long toCurrencyId,
            @Param("scale") Integer scale
    );

}

