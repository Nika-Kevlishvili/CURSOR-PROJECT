package bg.energo.phoenix.repository.receivable.disconnectionPowerSupply;

import bg.energo.phoenix.model.entity.EntityStatus;
import bg.energo.phoenix.model.entity.receivable.disconnectionPowerSupply.DisconnectionOfPowerSupply;
import bg.energo.phoenix.model.response.receivable.disconnectionPowerSupply.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface DisconnectionPowerSupplyRepository extends JpaRepository<DisconnectionOfPowerSupply, Long> {

    @Query(value = "select nextval('receivable.power_supply_disconnections_id_seq')", nativeQuery = true)
    Long getNextSequenceValue();

    @Query(value = """
            select
            case when c.customer_type = 'PRIVATE_CUSTOMER'
                  then concat(c.identifier,concat(' (',cd.name),
                       case when cd.middle_name is not null then concat(' ',cd.middle_name)  end,
                       case when cd.last_name is not null then concat(' ',cd.last_name)  end,
                       case when cd.legal_form_id is not null then concat(' ',lf.name) end, ')' )
                  when c.customer_type = 'LEGAL_ENTITY' then concat(c.identifier,' (',cd.name,case when cd.legal_form_id is not null then concat(' ',lf.name) end,')')
                  end customer,
            p.identifier as podIdentifier,
            got.disconnection_type as disconnectionType,
            psdp.disconnection_date as disconnectionDate,
            psdp.is_checked as isChecked,
            psdp.express_reconnection as expressReconnection,
            c.id as customerId,
            p.id as podId,
            psdp.id as psdpId,
            psd.id  as disconnectionId,
            psdp.grid_operator_tax_id as gridOperatorTaxId,
            psdp.power_supply_disconnection_id as powerSupplyDisconnectionId
            from
            receivable.power_supply_disconnection_request_pods psdrp
             join
            receivable.power_supply_disconnection_requests psdr
            on psdrp.power_supply_disconnection_request_id = psdr.id
            and psdrp.power_supply_disconnection_request_id = :powerSupplyDisconnectionRequestId
            and psdrp.is_checked = true
            and psdr.disconnection_request_status = 'EXECUTED'
            join
            pod.pod p
            on psdrp.pod_id = p.id
            and coalesce(p.disconnected,false) = false
            and p.impossibility_disconnection = false
            join customer.customers c
            on psdrp.customer_id = c.id
            join
            customer.customer_details cd
            on c.last_customer_detail_id = cd.id
            left join
            nomenclature.legal_forms lf
            on cd.legal_form_id = lf.id
            left join
            receivable.power_supply_disconnections psd
            on psd.power_supply_disconnection_request_id = psdr.id
            and psd.id = :powerSupplyDisconnectionId
            left join
            receivable.power_supply_disconnection_pods psdp
            on psdp.power_supply_disconnection_id =  psd.id
            and psdrp.pod_id = psdp.pod_id
            left join nomenclature.grid_operator_taxes got
            on psdp.grid_operator_tax_id =  got.id
            where case when psd.disconnection_status =  'EXECUTED' then psdrp.is_checked = true else 1=1 end
            and not exists
            (select * from receivable.power_supply_disconnections psd2
              join
              receivable.power_supply_disconnection_pods psdp2
              on psdp2.power_supply_disconnection_id = psd2.id
              and psd2.power_supply_disconnection_request_id = :powerSupplyDisconnectionRequestId
              and psdp2.pod_id = psdrp.pod_id
              and psd2.disconnection_status = 'EXECUTED'
              and psd2.status = 'ACTIVE')
            and
             (:prompt is null or (:searchBy = 'ALL' and (lower(c.identifier) like :prompt
                                                          or
                                                         text(c.customer_number) like :prompt
                                                          or
                                                         lower(p.identifier) like :prompt
                                            )
                                        )
                                        or ((:searchBy = 'CUSTOMER_IDENTIFIER' and lower(c.identifier) like :prompt)
                                             or
                                            (:searchBy = 'CUSTOMER_NUMBER' and text(c.customer_number) like :prompt)
                                             or
                                            (:searchBy = 'POD_IDENTIFIER' and lower(p.identifier) like :prompt)
                                        )
                                    )
            and p.id not in(
                select distinct b.pod_id
                    from receivable.power_supply_dcn_cancellations a
                    left join receivable.power_supply_dcn_cancellation_pods b on a.id = b.power_supply_dcn_cancellation_id
                    where cancellation_status = 'EXECUTED'
                    and status = 'ACTIVE'
                    and b.id is not null
                )
            order by psdrp.is_checked desc
            """,
            countQuery = """
                                select
                                count(*)
                                from
                                receivable.power_supply_disconnection_request_pods psdrp
                                 join
                                receivable.power_supply_disconnection_requests psdr
                                on psdrp.power_supply_disconnection_request_id = psdr.id
                                and psdrp.power_supply_disconnection_request_id = :powerSupplyDisconnectionRequestId
                                and psdrp.is_checked = true
                                and psdr.disconnection_request_status = 'EXECUTED'
                                join
                                pod.pod p
                                on psdrp.pod_id = p.id
                                and coalesce(p.disconnected,false) = false
                                and p.impossibility_disconnection = false
                                join customer.customers c
                                on psdrp.customer_id = c.id
                                join
                                customer.customer_details cd
                                on c.last_customer_detail_id = cd.id
                                left join
                                nomenclature.legal_forms lf
                                on cd.legal_form_id = lf.id
                                left join
                                receivable.power_supply_disconnections psd
                                on psd.power_supply_disconnection_request_id = psdr.id
                                and psd.id = :powerSupplyDisconnectionId
                                left join
                                receivable.power_supply_disconnection_pods psdp
                                on psdp.power_supply_disconnection_id =  psd.id
                                and psdrp.pod_id = psdp.pod_id
                                left join nomenclature.grid_operator_taxes got
                                on psdp.grid_operator_tax_id =  got.id
                                where case when psd.disconnection_status =  'EXECUTED' then psdrp.is_checked = true else 1=1 end
                                and not exists
                                (select * from receivable.power_supply_disconnections psd2
                                  join
                                  receivable.power_supply_disconnection_pods psdp2
                                  on psdp2.power_supply_disconnection_id = psd2.id
                                  and psd2.power_supply_disconnection_request_id = :powerSupplyDisconnectionRequestId
                                  and psdp2.pod_id = psdrp.pod_id
                                  and psd2.disconnection_status = 'EXECUTED'
                                  and psd2.status = 'ACTIVE')
                                and
                                 (:prompt is null or (:searchBy = 'ALL' and (lower(c.identifier) like :prompt
                                                                              or
                                                                             text(c.customer_number) like :prompt
                                                                              or
                                                                             lower(p.identifier) like :prompt
                                                                )
                                                            )
                                                            or ((:searchBy = 'CUSTOMER_IDENTIFIER' and lower(c.identifier) like :prompt)
                                                                 or
                                                                (:searchBy = 'CUSTOMER_NUMBER' and text(c.customer_number) like :prompt)
                                                                 or
                                                                (:searchBy = 'POD_IDENTIFIER' and lower(p.identifier) like :prompt)
                                                            )
                                                        )
                    and p.id not in(
                    select distinct b.pod_id
                        from receivable.power_supply_dcn_cancellations a
                        left join receivable.power_supply_dcn_cancellation_pods b on a.id = b.power_supply_dcn_cancellation_id
                        where cancellation_status = 'EXECUTED'
                        and status = 'ACTIVE'
                        and b.id is not null
                    )
                    """, nativeQuery = true)
    Page<DisconnectionPowerSupplyPodsMiddleResponse> getDraftTable(
            @Param("powerSupplyDisconnectionRequestId") Long powerSupplyDisconnectionRequestId,
            @Param("powerSupplyDisconnectionId") Long powerSupplyDisconnectionId,
            @Param("prompt") String prompt,
            @Param("searchBy") String searchBy,
            Pageable pageable
    );

    @Query(value = """
             select
             case when c.customer_type = 'PRIVATE_CUSTOMER'
                  then concat(c.identifier,concat(' (',cd.name),
                       case when cd.middle_name is not null then concat(' ',cd.middle_name)  end,
                       case when cd.last_name is not null then concat(' ',cd.last_name)  end,
                       case when cd.legal_form_id is not null then concat(' ',lf.name) end, ')' )
                  when c.customer_type = 'LEGAL_ENTITY' then concat(c.identifier,' (',cd.name,case when cd.legal_form_id is not null then concat(' ',lf.name) end,')')
                  end customer,
             p.identifier as podIdentifier,
             got.disconnection_type as disconnectionType,
             psdp.disconnection_date as disconnectionDate,
             psdp.is_checked as isChecked,
             psdp.express_reconnection as expressReconnection,
             c.id    as customerId,
             p.id    as podId,
             psdp.id as pspdId,
             psd.id  as disconnectionId,
             psdp.grid_operator_tax_id as gridOperatorTaxId,
             psdp.power_supply_disconnection_id as powerSupplyDisconnectionId
            from
            receivable.power_supply_disconnections psd
            join
            receivable.power_supply_disconnection_pods psdp
            on
            psdp.power_supply_disconnection_id = psd.id
            and psd.id = :powerSupplyDisconnectionId
            join
            customer.customers c
            on psdp.customer_id = c.id
            join
            customer.customer_details cd
            on c.last_customer_detail_id = cd.id
            join
            nomenclature.grid_operator_taxes got
            on psdp.grid_operator_tax_id = got.id
            join
            pod.pod p
            on psdp.pod_id = p.id
            left join
            nomenclature.legal_forms lf
            on cd.legal_form_id = lf.id
            and
             (:prompt is null or (:searchBy = 'ALL' and (lower(c.identifier) like :prompt
                                                          or
                                                         text(c.customer_number) like :prompt
                                                          or
                                                         lower(p.identifier) like :prompt
                                            )
                                        )
                                        or ((:searchBy = 'CUSTOMER_IDENTIFIER' and lower(c.identifier) like :prompt)
                                             or
                                            (:searchBy = 'CUSTOMER_NUMBER' and text(c.customer_number) like :prompt)
                                             or
                                            (:searchBy = 'POD_IDENTIFIER' and lower(p.identifier) like :prompt)
                                        )
                                    )
            """, countQuery = """
                    select
                    count(*)
                    from
                    receivable.power_supply_disconnections psd
                    join
                    receivable.power_supply_disconnection_pods psdp
                    on
                    psdp.power_supply_disconnection_id = psd.id
                    and psd.id = :powerSupplyDisconnectionId
                    join
                    customer.customers c
                    on psdp.customer_id = c.id
                    join
                    customer.customer_details cd
                    on c.last_customer_detail_id = cd.id
                    join
                    nomenclature.grid_operator_taxes got
                    on psdp.grid_operator_tax_id = got.id
                    join
                    pod.pod p
                    on psdp.pod_id = p.id
                    left join
                    nomenclature.legal_forms lf
                    on cd.legal_form_id = lf.id
                    and
                     (:prompt is null or (:searchBy = 'ALL' and (lower(c.identifier) like :prompt
                                                                  or
                                                                 text(c.customer_number) like :prompt
                                                                  or
                                                                 lower(p.identifier) like :prompt
                                                    )
                                                )
                                                or ((:searchBy = 'CUSTOMER_IDENTIFIER' and lower(c.identifier) like :prompt)
                                                     or
                                                    (:searchBy = 'CUSTOMER_NUMBER' and text(c.customer_number) like :prompt)
                                                     or
                                                    (:searchBy = 'POD_IDENTIFIER' and lower(p.identifier) like :prompt)
                                                )
                                            )
            """, nativeQuery = true)
    Page<DisconnectionPowerSupplyPodsMiddleResponse> getExecutedTable(
            @Param("powerSupplyDisconnectionId") Long powerSupplyDisconnectionId,
            @Param("prompt") String prompt,
            @Param("searchBy") String searchBy,
            Pageable pageable
    );

    @Query("SELECT new bg.energo.phoenix.model.response.receivable.disconnectionPowerSupply.PodAndGotMappedResponse(p.id, p.identifier, gt.id, gt.disconnectionType) " +
           "FROM PointOfDelivery p " +
           "LEFT JOIN TaxesForTheGridOperator gt ON p.gridOperatorId = gt.gridOperator AND gt.disconnectionType = :disconnectionType " +
           "WHERE p.identifier = :identifier " +
           "  AND (gt.id IS NULL OR gt.id = (SELECT MAX(gt2.id) " +
           "                                 FROM TaxesForTheGridOperator gt2 " +
           "                                 WHERE gt2.gridOperator = gt.gridOperator " +
           "                                   AND gt2.disconnectionType = :disconnectionType))")
    PodAndGotMappedResponse findPodAndGridOperatorTax(
            @Param("identifier") String identifier,
            @Param("disconnectionType") String disconnectionType
    );

    @Query("SELECT new bg.energo.phoenix.model.response.receivable.disconnectionPowerSupply.PodAndGotMappedResponse(" +
           "p.id, p.identifier, gt.id, gt.disconnectionType) " +
           "FROM PointOfDelivery p " +
           "LEFT JOIN TaxesForTheGridOperator gt ON p.gridOperatorId = gt.gridOperator " +
           "WHERE p.id = :podId " +
           "AND (gt.id = :gridOperatorTaxId OR gt.id IS NULL)")
    PodAndGotMappedResponse findPodAndGridOperatorTaxById(
            @Param("podId") Long podId,
            @Param("gridOperatorTaxId") Long gridOperatorTaxId);


    @Query(nativeQuery = true,
            value = """
                    WITH disconnection_pod_counts AS (
                    SELECT 
                        power_supply_disconnection_id, 
                        COUNT(DISTINCT pod_id) AS numberOfPods
                    FROM receivable.power_supply_disconnection_pods
                    GROUP BY power_supply_disconnection_id
                )
                SELECT 
                    psd.disconnection_number AS disconnectionNumber,
                    date(psd.create_date) AS createDate,
                    psd.disconnection_status AS disconnectionStatus,
                    psdr.request_number AS requestForDisconnectionNumber,
                    COALESCE(dpc.numberOfPods, 0) AS numberOfPods,
                    psd.id AS id,
                    psd.status AS status
                FROM receivable.power_supply_disconnections psd
                JOIN receivable.power_supply_disconnection_requests psdr 
                    ON psd.power_supply_disconnection_request_id = psdr.id
                LEFT JOIN disconnection_pod_counts dpc
                    ON psd.id = dpc.power_supply_disconnection_id
                LEFT JOIN receivable.power_supply_disconnection_pods psdp 
                    ON psd.id = psdp.power_supply_disconnection_id
                LEFT JOIN customer.customers c 
                    ON c.id = psdp.customer_id
                LEFT JOIN customer.customer_details cd 
                    ON cd.customer_id = c.id
                LEFT JOIN pod.pod p 
                    ON p.id = psdp.pod_id
                WHERE 
                    (COALESCE(:createDateFrom, NULL) IS NULL OR psd.create_date >= date(:createDateFrom))
                    AND (COALESCE(:createDateTo, NULL) IS NULL OR psd.create_date <= date(:createDateTo))
                    AND (COALESCE(:disconnectionStatus, NULL) IS NULL OR text(psd.disconnection_status) in(:disconnectionStatus))
                    AND (COALESCE(:status, NULL) IS NULL OR text(psd.status) in(:status))
                    AND (COALESCE(:gridOperatorIds, NULL) IS NULL OR psdr.grid_operator_id in(:gridOperatorIds))
                    AND (
                        :prompt IS NULL OR (
                            :searchBy = 'ALL' AND (
                                lower(psd.disconnection_number) ILIKE :prompt
                                OR lower(psdr.request_number) ILIKE :prompt
                                OR c.identifier ILIKE :prompt
                                    OR cd.name ILIKE :prompt
                                    OR CONCAT(cd.name, ' ', cd.middle_name) ILIKE :prompt
                                    OR CONCAT(cd.name, ' ', cd.last_name) ILIKE :prompt
                                    OR CONCAT(cd.middle_name, ' ', cd.last_name) ILIKE :prompt
                                    OR CONCAT(cd.name, ' ', middle_name, ' ', cd.last_name) ILIKE :prompt || '%'
                                )
                            ) OR (
                                :searchBy = 'NUMBER' AND lower(psd.disconnection_number) ILIKE :prompt
                            ) OR (
                                :searchBy = 'DISCONNECTION_REQUEST_NUMBER' AND lower(psdr.request_number) ILIKE :prompt
                            ) OR (
                                :searchBy = 'CUSTOMER_IDENTIFIER' AND c.identifier ILIKE :prompt
                            ) OR (
                                :searchBy = 'POD_IDENTIFIER' AND lower(p.identifier) ILIKE :prompt
                            ) OR (
                                :searchBy = 'CUSTOMER_NAME' and (
                                   cd.name ILIKE :prompt
                                   OR CONCAT(cd.name, ' ', cd.middle_name) ILIKE :prompt
                                   OR CONCAT(cd.name, ' ', cd.last_name) ILIKE :prompt
                                   OR CONCAT(cd.middle_name, ' ', cd.last_name) ILIKE :prompt
                                   OR CONCAT(cd.name, ' ', middle_name, ' ', cd.last_name) ILIKE :prompt
                                   )
                            )
                        )
                        AND (COALESCE(:numberOfPodsFrom, NULL) IS NULL OR dpc.numberOfPods >= :numberOfPodsFrom)
                        AND (COALESCE(:numberOfPodsTo, NULL) IS NULL OR dpc.numberOfPods <= :numberOfPodsTo)
                GROUP BY 
                    psd.id, psd.disconnection_number, psd.create_date, psd.disconnection_status, 
                    psdr.request_number, dpc.numberOfPods, psd.status
                    """,
            countQuery = """
                    WITH disconnection_pod_counts AS (
                        SELECT 
                            power_supply_disconnection_id, 
                            COUNT(DISTINCT pod_id) AS numberOfPods
                        FROM receivable.power_supply_disconnection_pods
                        GROUP BY power_supply_disconnection_id
                    )
                    SELECT COUNT(DISTINCT psd.id)
                    FROM receivable.power_supply_disconnections psd
                    JOIN receivable.power_supply_disconnection_requests psdr 
                        ON psd.power_supply_disconnection_request_id = psdr.id
                    LEFT JOIN disconnection_pod_counts dpc
                        ON psd.id = dpc.power_supply_disconnection_id
                    LEFT JOIN receivable.power_supply_disconnection_pods psdp 
                        ON psd.id = psdp.power_supply_disconnection_id
                    LEFT JOIN customer.customers c 
                        ON c.id = psdp.customer_id
                    LEFT JOIN customer.customer_details cd 
                        ON cd.customer_id = c.id
                    LEFT JOIN pod.pod p 
                        ON p.id = psdp.pod_id
                    WHERE 
                        (COALESCE(:createDateFrom, NULL) IS NULL OR psd.create_date >= date(:createDateFrom))
                        AND (COALESCE(:createDateTo, NULL) IS NULL OR psd.create_date <= date(:createDateTo))
                        AND (COALESCE(:disconnectionStatus, NULL) IS NULL OR text(psd.disconnection_status) in(:disconnectionStatus))
                        AND (COALESCE(:status, NULL) IS NULL OR text(psd.status) in(:status))
                        AND (COALESCE(:gridOperatorIds, NULL) IS NULL OR psdr.grid_operator_id in(:gridOperatorIds))
                        AND (
                            :prompt IS NULL OR (
                                :searchBy = 'ALL' AND (
                                    lower(psd.disconnection_number) ILIKE :prompt
                                    OR lower(psdr.request_number) ILIKE :prompt
                                    OR c.identifier ILIKE :prompt
                                    OR lower(p.identifier) ILIKE :prompt
                                    OR cd.name ILIKE :prompt
                                    OR CONCAT(cd.name, ' ', cd.middle_name) ILIKE :prompt
                                    OR CONCAT(cd.name, ' ', cd.last_name) ILIKE :prompt
                                    OR CONCAT(cd.middle_name, ' ', cd.last_name) ILIKE :prompt
                                    OR CONCAT(cd.name, ' ', middle_name, ' ', cd.last_name) ILIKE :prompt || '%'
                                )
                            ) OR (
                                :searchBy = 'NUMBER' AND lower(psd.disconnection_number) ILIKE :prompt
                            ) OR (
                                :searchBy = 'DISCONNECTION_REQUEST_NUMBER' AND lower(psdr.request_number) ILIKE :prompt
                            ) OR (
                                :searchBy = 'CUSTOMER_IDENTIFIER' AND c.identifier ILIKE :prompt
                            ) OR (
                                :searchBy = 'POD_IDENTIFIER' AND lower(p.identifier) ILIKE :prompt
                            ) OR (
                                :searchBy = 'CUSTOMER_NAME' and (
                                   cd.name ILIKE :prompt
                                   OR CONCAT(cd.name, ' ', cd.middle_name) ILIKE :prompt
                                   OR CONCAT(cd.name, ' ', cd.last_name) ILIKE :prompt
                                   OR CONCAT(cd.middle_name, ' ', cd.last_name) ILIKE :prompt
                                   OR CONCAT(cd.name, ' ', middle_name, ' ', cd.last_name) ILIKE :prompt
                                   )
                            )
                        )
                        AND (COALESCE(:numberOfPodsFrom, NULL) IS NULL OR dpc.numberOfPods >= :numberOfPodsFrom)
                        AND (COALESCE(:numberOfPodsTo, NULL) IS NULL OR dpc.numberOfPods <= :numberOfPodsTo)
                    """)
    Page<DisconnectionPowerSupplyListingMiddleResponse> filter(
            @Param("prompt") String prompt,
            @Param("searchBy") String searchBy,
            @Param("createDateFrom") LocalDate createDateFrom,
            @Param("createDateTo") LocalDate createDateTo,
            @Param("disconnectionStatus") List<String> disconnectionStatus,
            @Param("numberOfPodsFrom") Long numberOfPodsFrom,
            @Param("numberOfPodsTo") Long numberOfPodsTo,
            @Param("status") List<String> status,
            @Param("gridOperatorIds") List<Long> gridOperatorIds,
            Pageable pageable
    );


    @Query(value = """
                SELECT
                p.id as podId,
                p.identifier AS podIdentifier,
                psdrp.is_checked AS isChecked
            FROM
                receivable.power_supply_disconnection_request_pods psdrp
            JOIN
                receivable.power_supply_disconnection_requests psdr
                ON psdrp.power_supply_disconnection_request_id = psdr.id
                AND psdrp.power_supply_disconnection_request_id = :powerSupplyDisconnectionRequestId
                AND psdrp.is_checked = true
                AND psdr.disconnection_request_status = 'EXECUTED'
            JOIN
                pod.pod p
                ON psdrp.pod_id = p.id
                AND COALESCE(p.disconnected, false) = false
                AND p.impossibility_disconnection = false
            LEFT JOIN
                receivable.power_supply_disconnections psd
                ON psd.power_supply_disconnection_request_id = psdr.id
                AND psd.id = :powerSupplyDisconnectionId
            LEFT JOIN
                receivable.power_supply_disconnection_pods psdp
                ON psdp.power_supply_disconnection_id = psd.id
            WHERE
                CASE
                    WHEN psd.disconnection_status = 'EXECUTED' THEN psdrp.is_checked = true
                    ELSE 1 = 1
                END
                AND NOT EXISTS (
                    SELECT 1
                    FROM receivable.power_supply_disconnections psd2
                    JOIN receivable.power_supply_disconnection_pods psdp2
                    ON psdp2.power_supply_disconnection_id = psd2.id
                    AND psd2.power_supply_disconnection_request_id = :powerSupplyDisconnectionRequestId
                    AND psdp2.pod_id = psdrp.pod_id
                    AND psd2.disconnection_status = 'EXECUTED'
                    AND psd2.status = 'ACTIVE'
                )
            ORDER BY
                psdrp.is_checked DESC;
            """, nativeQuery = true)
    List<DisconnectionPowerSupplyPodsMiddleResponse> getTable(
            @Param("powerSupplyDisconnectionRequestId") Long powerSupplyDisconnectionRequestId,
            @Param("powerSupplyDisconnectionId") Long powerSupplyDisconnectionId
    );

    @Query(value = """
                SELECT
                p.id as podId,
                p.identifier AS podIdentifier,
                psdrp.is_checked AS isChecked
            FROM
                receivable.power_supply_disconnection_request_pods psdrp
            JOIN
                receivable.power_supply_disconnection_requests psdr
                ON psdrp.power_supply_disconnection_request_id = psdr.id
                AND psdrp.power_supply_disconnection_request_id = :powerSupplyDisconnectionRequestId
                AND psdrp.is_checked = true
                AND psdr.disconnection_request_status = 'EXECUTED'
            JOIN
                pod.pod p
                ON psdrp.pod_id = p.id
            LEFT JOIN
                receivable.power_supply_disconnections psd
                ON psd.power_supply_disconnection_request_id = psdr.id
                    AND psd.id = :powerSupplyDisconnectionId
            ORDER BY
                psdrp.is_checked DESC;
            """, nativeQuery = true)
    List<DisconnectionPowerSupplyPodsMiddleResponse> getExecutedTable(
            @Param("powerSupplyDisconnectionRequestId") Long powerSupplyDisconnectionRequestId,
            @Param("powerSupplyDisconnectionId") Long powerSupplyDisconnectionId
    );

    @Query(value = """
            SELECT DISTINCT new bg.energo.phoenix.model.response.receivable.disconnectionPowerSupply.DisconnectionRequestList(psdr.id, psdr.requestNumber)
                       FROM DisconnectionPowerSupplyRequests psdr
                       JOIN DisconnectionPowerSupplyRequestsPods psdrp ON psdr.id = psdrp.powerSupplyDisconnectionRequestId
                       LEFT JOIN DisconnectionOfPowerSupply psd ON psdr.id = psd.powerSupplyDisconnectionRequestId
                       LEFT JOIN CancellationOfDisconnectionOfThePowerSupply psdc ON psdr.id = psdc.requestForDisconnectionOfThePowerSupplyId
                       WHERE psdr.disconnectionRequestsStatus = 'EXECUTED'
                       AND psdrp.isChecked = TRUE
                       AND (psd.disconnectionStatus = 'EXECUTED' or psd.disconnectionStatus is null)
                       AND (psd.status IS NULL OR psd.status = 'ACTIVE')
                       AND psdrp.podId not in (
                                   Select distinct psdcp.podId
                                   from  DisconnectionPowerSupplyRequests psdr1
                                             JOIN DisconnectionPowerSupplyRequestsPods psdrp1 ON psdr1.id = psdrp1.powerSupplyDisconnectionRequestId
                                             LEFT JOIN CancellationOfDisconnectionOfThePowerSupply psdc ON psdr1.id = psdc.requestForDisconnectionOfThePowerSupplyId
                                             LEFT JOIN PowerSupplyDcnCancellationPods psdcp ON psdc.id = psdcp.powerSupplyDcnCancellationId
                                   where psdr1.id = psdr.id
                                     and psdc.cancellationStatus = 'EXECUTED'
                                     and psdcp.isChecked = true
                                   )
                       AND (psdc.entityStatus IS NULL OR psdc.entityStatus = 'ACTIVE')
                       AND LOWER(psdr.requestNumber) LIKE LOWER(COALESCE(:prompt, ''))
            """)
    List<DisconnectionRequestList> findDisconnectionRequests(@Param("prompt") String prompt);

    @Query("SELECT NEW bg.energo.phoenix.model.response.receivable.disconnectionPowerSupply.TypeOfDisconnectionList(got.id, got.disconnectionType) " +
           "FROM PointOfDelivery pod " +
           "JOIN TaxesForTheGridOperator got ON pod.gridOperatorId = got.gridOperator " +
           "WHERE pod.id = :podId")
    List<TypeOfDisconnectionList> findDisconnectionType(Long podId);

    Optional<DisconnectionOfPowerSupply> findByIdAndStatus(Long id, EntityStatus status);

    @Query(value = """
        select
            cl.id                        as                      liabilityId,
               pod.id                       as                      podId,
               cl.customer_id               as                      customerId,
               cl.contract_billing_group_id as                      billingGroupId,
               cl.invoice_id                as                      invoiceId,
               tax.email_template_id        as                        emailTemplateId,
               tax.document_template_id     as                        documentTemplateId,
               tax.number_of_income_account   as                      numberOfIncomeAccount,
               tax.basis_for_issuing            as                    basisForIssuing,
               tax.cost_center_controlling_order  as                  costCenterControllingOrder,
               tax.tax_for_express_reconnection     as                taxForExpressReconnection, 
               tax.price_component_or_price_component_group_or_item as priceComponent,
               tax.currency_id                    as                  currencyId,
               tax.email_template_id as emailTemplateId,
               null                               as                  savedInvoiceId
        from receivable.power_supply_disconnections psd
                    join receivable.power_supply_disconnection_requests dps on psd.power_supply_disconnection_request_id = dps.id
                    join receivable.power_supply_disconnection_pods psdp on psd.id = psdp.power_supply_disconnection_id
                    join receivable.power_supply_disconnection_request_results dpsr on dpsr.pod_id = psdp.pod_id and dpsr.power_supply_disconnection_request_id=dps.id
                    join pod.pod pod on dpsr.pod_id=pod.id
                  join nomenclature.grid_operator_taxes tax on (tax.supplier_type::text) = dps.supplier_type::text
            and tax.grid_operator_id = dps.grid_operator_id
            and (
                                                                  (SELECT got.disconnection_type
                                                                   FROM pod.pod pod
                                                                            JOIN nomenclature.grid_operator_taxes got ON pod.grid_operator_id = got.grid_operator_id
                                                                   WHERE pod.id = psdp.pod_id
                                                                  order by pod.create_date desc
                                                             limit 1) = tax.disconnection_type
                                                                  )
                  join receivable.customer_liabilities cl on cl.liability_number = (select cl1.liability_number
                                                                                   from receivable.customer_liabilities cl1
                                                                                            join (select substring(liability_item from '[^-]+-[^-]+-(.+)') as extracted_liability
                                                                                                  from unnest(string_to_array(dpsr.liabilities_in_pod, ',')) as liability_item) extracted_liabilities
                                                                                                 on cl1.liability_number = extracted_liabilities.extracted_liability
                                                                                   order by cl1.create_date desc
                                                                                   limit 1)
        
        where
            dpsr.is_checked is true
          and tax.status = 'ACTIVE' and
           psdp.express_reconnection is true
          and psd.id = :powerSupplyDisconnectionId
    """, nativeQuery = true)
    List<TaxCalculationExpressReconnectionResponse> fetchLiabilities(@Param("powerSupplyDisconnectionId") Long powerSupplyDisconnectionId);

    @Query(value = """
        select cl.id                                                as                      liabilityId,
               pod.id                                               as                      podId,
               cl.customer_id                                       as                      customerId,
               cl.contract_billing_group_id                         as                      billingGroupId,
               cl.invoice_id                                        as                      invoiceId,
               tax.email_template_id                                as                      emailTemplateId,
               tax.document_template_id                             as                      documentTemplateId,
               tax.number_of_income_account                         as                      numberOfIncomeAccount,
               tax.basis_for_issuing                                as                      basisForIssuing,
               tax.cost_center_controlling_order                    as                      costCterControllingOrder,
               tax.tax_for_reconnection                             as                      taxForExpressReconnection,
               tax.price_component_or_price_component_group_or_item as                      priceComponent,
               tax.currency_id                                      as                      currencyId,
               dpsr.saved_invoice_id                                as                      savedInvoiceId
        from receivable.power_supply_disconnections psd
                 join receivable.power_supply_disconnection_requests dps on psd.power_supply_disconnection_request_id = dps.id
                 join receivable.power_supply_disconnection_pods psdp on psd.id = psdp.power_supply_disconnection_id
                 left join receivable.power_supply_disconnection_request_results dpsr on dpsr.pod_id = psdp.pod_id and dpsr.power_supply_disconnection_request_id=dps.id
                 join pod.pod pod on dpsr.pod_id=pod.id
                 join nomenclature.grid_operator_taxes tax on (tax.supplier_type::text) = dps.supplier_type::text
                    and tax.grid_operator_id = dps.grid_operator_id
                    and exists (
                       select 1
                       from pod.pod p
                       join nomenclature.grid_operator_taxes got on p.grid_operator_id = got.grid_operator_id
                       where p.id = psdp.pod_id
                         and got.disconnection_type = tax.disconnection_type
                    )
                 join receivable.customer_liabilities cl on cl.liability_number = (
                    select cl1.liability_number
                    from receivable.customer_liabilities cl1
                    join (select substring(liability_item from '[^-]+-[^-]+-(.+)') as extracted_liability
                          from unnest(string_to_array(dpsr.liabilities_in_pod, ',')) as liability_item) extracted_liabilities
                         on cl1.liability_number = extracted_liabilities.extracted_liability
                    order by cl1.create_date desc
                    limit 1)
        where dpsr.is_checked = true
          and tax.status = 'ACTIVE'
          and psd.id = :disconnectionId
    """, nativeQuery = true)
    List<TaxCalculationExpressReconnectionResponse> fetchLiabilitiesForTaxProcess(@Param("disconnectionId") Long disconnectionId);


}
