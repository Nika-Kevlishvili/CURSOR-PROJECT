package bg.energo.phoenix.repository.receivable.latePaymentFine;

import bg.energo.phoenix.model.documentModels.latePaymentFine.LatePaymentFineCustomerInfoResponse;
import bg.energo.phoenix.model.documentModels.latePaymentFine.LatePaymentFineInterestsMiddleResponse;
import bg.energo.phoenix.model.documentModels.latePaymentFine.LatePaymentFineOutDocInfoResponse;
import bg.energo.phoenix.model.entity.receivable.latePaymentFine.LatePaymentFine;
import bg.energo.phoenix.model.response.receivable.latePaymentFine.LatePaymentFineListingMiddleResponse;
import bg.energo.phoenix.model.response.receivable.latePaymentFine.LatePaymentFineShortResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.query.Procedure;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface LatePaymentFineRepository extends JpaRepository<LatePaymentFine, Long> {

    Optional<LatePaymentFine> findByIdAndCustomerId(Long id, Long customerId);

    Optional<LatePaymentFine> findByLatePaymentNumber(String latePaymentNumber);

    @Query(nativeQuery = true,
            value = """
                    select lf.id from receivable.late_payment_fines lf
                    where date(lf.create_date) = date(:date)
                      and lf.type = 'LATE_PAYMENT_FINE'
                      and (lf.outgoing_doc_type = 'LATE_PAYMENT_FINE_JOB' or lf.outgoing_doc_type = 'ONLINE_PAYMENT')
                    """
    )
    List<Long> getLatePaymentFinesByCreateDate(LocalDate date);

    @Query(value = """
                        select new bg.energo.phoenix.model.response.receivable.latePaymentFine.LatePaymentFineShortResponse(
                        lpf
                        )
                        from LatePaymentFine lpf
                        where lpf.customerId = :customerId
                        and lpf.latePaymentNumber = COALESCE(:prompt, lpf.latePaymentNumber)
            """)
    Page<LatePaymentFineShortResponse> findAllByCustomerId(@Param("customerId") Long customerId,
                                                           @Param("prompt") String prompt,
                                                           Pageable pageable);

    @Query(nativeQuery = true,
            value = """
                    
                                      WITH TotalAmounts AS (SELECT late_payment_fine_id,
                                                 SUM(total_amount) AS totalAmount
                                          FROM receivable.late_payment_fine_invoices
                                          GROUP BY late_payment_fine_id)
                    select tbl.*
                    from (select lpf.late_payment_number  as                                                 number,
                                 date(lpf.create_date)    as                                                 createDate,
                                 date(lpf.due_date)       as                                                 dueDate,
                                 lpf.type                 as                                                 type,
                                 c.identifier             as                                                 customerIdentifier,
                                 case lpf.reversed when false then 'NO' else 'YES' end                       reversed,
                                 (select cbg.group_number
                                  from product_contract.contract_billing_groups cbg
                                  where lpf.contract_billing_group_id = cbg.id)                              billingGroup,
                                 ROUND(ta.totalAmount, 2) AS                                                 totalAmount,
                                 (select c.name from nomenclature.currencies c where lpf.currency_id = c.id) currency,
                                 lpf.id                   as                                                 id,
                                 date(lpf.logical_date)   as                                                 logicalDate,
                                 case
                                     when c.customer_type = 'PRIVATE_CUSTOMER' then
                                         concat(
                                                 c.identifier, ' (',
                                                 cd.name,
                                                 case when cd.middle_name is not null then concat(' ', cd.middle_name) else '' end,
                                                 case when cd.last_name is not null then concat(' ', cd.last_name) else '' end,
                                                 ')'
                                         )
                                     when c.customer_type = 'LEGAL_ENTITY' then
                                         concat(c.identifier, ' (', cd.name, ' ', lf.name, ')')
                                     end                                                                     customer
                          from receivable.late_payment_fines lpf
                                   join customer.customers c on lpf.customer_id = c.id
                                   join customer.customer_details cd on c.last_customer_detail_id = cd.id
                                   left join TotalAmounts ta on ta.late_payment_fine_id = lpf.id
                                   left join nomenclature.legal_forms lf on cd.legal_form_id = lf.id
                          where (date(:createDateFrom) is null or date(lpf.logical_date) >= date(:createDateFrom))
                            and (date(:createDateTo) is null or date(lpf.logical_date) <= date(:createDateTo))
                            and (date(:dueDateFrom) is null or lpf.due_date >= date(:dueDateFrom))
                            and (date(:dueDateTo) is null or lpf.due_date <= date(:dueDateTo))
                            and ((:type) is null or text(lpf.type) in (:type))
                            and ((:currencyIds) is null or lpf.currency_id in (:currencyIds))
                            and ((:reversed) is null or text(lpf.reversed) in (:reversed))
                            and (:prompt is null or
                                 (:searchBy = 'ALL' and (lower(lpf.late_payment_number) like :prompt or c.identifier like :prompt))
                              or ((:searchBy = 'LATE_PAYMENT_NUMBER' and lower(lpf.late_payment_number) like :prompt)
                                  or (:searchBy = 'CUSTOMER' and lower(C.identifier) like :prompt))
                              )) as tbl
                    where (coalesce(:totalAmountFrom, '0') = '0' or tbl.totalAmount >= :totalAmountFrom)
                      and (coalesce(:totalAmountTo, '0') = '0' or tbl.totalAmount <= :totalAmountTo)
                    """,
            countQuery = """
                    WITH TotalAmounts AS (SELECT late_payment_fine_id,
                                                 SUM(total_amount) AS totalAmount
                                          FROM receivable.late_payment_fine_invoices
                                          GROUP BY late_payment_fine_id)
                    select count(*)
                    from (select lpf.late_payment_number  as                                                 number,
                                 date(lpf.create_date)    as                                                 createDate,
                                 date(lpf.due_date)       as                                                 dueDate,
                                 lpf.type                 as                                                 type,
                                 c.identifier             as                                                 customerIdentifier,
                                 case lpf.reversed when false then 'NO' else 'YES' end                       reversed,
                                 (select cbg.group_number
                                  from product_contract.contract_billing_groups cbg
                                  where lpf.contract_billing_group_id = cbg.id)                              billingGroup,
                                 ROUND(ta.totalAmount, 2) AS                                                 totalAmount,
                                 (select c.name from nomenclature.currencies c where lpf.currency_id = c.id) currency,
                                 lpf.id                   as                                                 id,
                                 date(lpf.logical_date)   as                                                 logicalDate,
                                 case
                                     when c.customer_type = 'PRIVATE_CUSTOMER' then
                                         concat(
                                                 c.identifier, ' (',
                                                 cd.name,
                                                 case when cd.middle_name is not null then concat(' ', cd.middle_name) else '' end,
                                                 case when cd.last_name is not null then concat(' ', cd.last_name) else '' end,
                                                 ')'
                                         )
                                     when c.customer_type = 'LEGAL_ENTITY' then
                                         concat(c.identifier, ' (', cd.name, ' ', lf.name, ')')
                                     end                                                                     customer
                          from receivable.late_payment_fines lpf
                                   join customer.customers c on lpf.customer_id = c.id
                                   join customer.customer_details cd on c.last_customer_detail_id = cd.id
                                   left join TotalAmounts ta on ta.late_payment_fine_id = lpf.id
                                   left join nomenclature.legal_forms lf on cd.legal_form_id = lf.id
                          where (date(:createDateFrom) is null or date(lpf.logical_date) >= date(:createDateFrom))
                            and (date(:createDateTo) is null or date(lpf.logical_date) <= date(:createDateTo))
                            and (date(:dueDateFrom) is null or lpf.due_date >= date(:dueDateFrom))
                            and (date(:dueDateTo) is null or lpf.due_date <= date(:dueDateTo))
                            and ((:type) is null or text(lpf.type) in (:type))
                            and ((:currencyIds) is null or lpf.currency_id in (:currencyIds))
                            and ((:reversed) is null or text(lpf.reversed) in (:reversed))
                            and (:prompt is null or
                                 (:searchBy = 'ALL' and (lower(lpf.late_payment_number) like :prompt or c.identifier like :prompt))
                              or ((:searchBy = 'LATE_PAYMENT_NUMBER' and lower(lpf.late_payment_number) like :prompt)
                                  or (:searchBy = 'CUSTOMER' and lower(C.identifier) like :prompt))
                              )) as tbl
                    where (coalesce(:totalAmountFrom, '0') = '0' or tbl.totalAmount >= :totalAmountFrom)
                      and (coalesce(:totalAmountTo, '0') = '0' or tbl.totalAmount <= :totalAmountTo)
                    """
    )
    Page<LatePaymentFineListingMiddleResponse> filter(
            @Param("createDateFrom") LocalDateTime createDateFrom,
            @Param("createDateTo") LocalDateTime createDateTo,
            @Param("dueDateFrom") LocalDate dueDateFrom,
            @Param("dueDateTo") LocalDate dueDateTo,
            @Param("type") List<String> type,
            @Param("currencyIds") List<Long> currencyIds,
            @Param("reversed") List<String> reversed,
            @Param("searchBy") String searchBy,
            @Param("totalAmountFrom") BigDecimal totalAmountFrom,
            @Param("totalAmountTo") BigDecimal totalAmountTo,
            @Param("prompt") String prompt,
            Pageable pageable

    );

    @Query(value = "select nextval('receivable.late_payment_fines_id_seq')", nativeQuery = true)
    Long getNextLatePaymentSequence();


    @Query(value = """
               SELECT
                                        invoicePaymentTerm
                                    FROM (
                                             SELECT distinct
                                                 ipt.id as invoicePaymentTerm
                                             FROM invoice.invoices i
                                                      JOIN product_contract.contract_details pcd ON pcd.id = i.product_contract_detail_id
                                                      JOIN terms.invoice_payment_terms ipt ON pcd.invoice_payment_term_id = ipt.id
                                                      JOIN nomenclature.calendars c ON ipt.calendar_id = c.id
                                             WHERE i.id = :invoiceId
                                                and ipt.status = 'ACTIVE'
            
                                             UNION ALL
            
                                             SELECT distinct
                                                 ipt.id as invoicePaymentTerm
                                             FROM invoice.invoices i
                                                       JOIN service_contract.contract_details scd ON scd.id = i.service_contract_detail_id
                                                       JOIN terms.invoice_payment_terms ipt ON scd.invoice_payment_term_id = ipt.id
                                                       JOIN nomenclature.calendars c ON ipt.calendar_id = c.id
                                             WHERE i.id = :invoiceId
                                               and ipt.status = 'ACTIVE'
                                         ) combined_data
            """,
            nativeQuery = true)
    Long findPaymentTermIdByInvoiceId(@Param("invoiceId") Long invoiceId);

    @Query(value = """
                   select cd.id
                   from receivable.late_payment_fines lpf
                   left join product_contract.contract_billing_groups cbg on lpf.contract_billing_group_id = cbg.id
                   left join product_contract.contracts c on cbg.contract_id = c.id
                   left join product_contract.contract_details cd on c.id = cd.contract_id
                   where lpf.id = :latePaymentId
            """, nativeQuery = true)
    Optional<List<Long>> getCustomerDetailId(@Param("latePaymentId") Long latePaymentId);

    @Query(value = """
                               SELECT
                                   COALESCE(
                                           NULLIF(regexp_replace(late_payment_number, '^.*?-', '', 'g'), ''),
                                           late_payment_number
                                   ) AS numeric_part
                               FROM receivable.late_payment_fines
                               WHERE late_payment_number ~ '(^[А-Яа-я]{3}-)?[0-9]+$'\s
                               ORDER BY
                                   CASE
                                       WHEN late_payment_number ~ '^.*-([0-9]+)$' THEN CAST(regexp_replace(late_payment_number, '^.*-', '', 'g') AS BIGINT)
                                       WHEN late_payment_number ~ '^[0-9]+$' THEN CAST(late_payment_number AS BIGINT)
                                       ELSE 0
                                       END DESC
                               LIMIT 1
            """, nativeQuery = true)
    Optional<String> getLatestNumberOfTheLatePaymentNumber();

    @Procedure(name = "receivable.direct_liability_offsetting")
    String directLiabilityOffsetting(
            @Param("sourceType") String sourceType,
            @Param("sourceId") Long sourceId,
            @Param("liabilityId") Long liabilityId,
            @Param("userId") String userId,
            @Param("modifyUserId") String modifyUserId
    );

    @Query(
            nativeQuery = true,
            value = """
                    SELECT DISTINCT CONCAT(
                                            cd.name,
                                            CASE
                                                WHEN text(c.customer_type) = 'PRIVATE_CUSTOMER'
                                                    THEN CONCAT(' ', cd.middle_name, ' ', cd.last_name)
                                                ELSE ''
                                                END
                                    )                         AS CustomerNameComb,
                                    CONCAT(
                                            cd.name_transl,
                                            CASE
                                                WHEN text(c.customer_type) = 'PRIVATE_CUSTOMER'
                                                    THEN CONCAT(' ', cd.middle_name_transl, ' ', cd.last_name_transl)
                                                WHEN text(c.customer_type) = 'LEGAL_ENTITY' THEN CONCAT(' ', lf.name)
                                                ELSE ''
                                                END
                                    )                         AS CustomerNameCombTrsl,
                                    c.identifier                  as CustomerIdentifier,
                                    cd.vat_number                 as CustomerVat,
                                    c.customer_number             as CustomerNumber,
                                    translation.translate_text(CONCAT(
                                            CASE
                                                WHEN cd.foreign_address
                                                    THEN CONCAT(
                                                        cd.district_foreign, ', ',
                                                        CASE
                                                            WHEN cd.residential_area_foreign IS NULL THEN ''
                                                            ELSE CONCAT(cd.foreign_residential_area_type, ', ', cd.residential_area_foreign)
                                                            END,
                                                        CASE
                                                            WHEN cd.street_foreign IS NULL THEN ''
                                                            ELSE CONCAT(', ', cd.foreign_street_type, ', ', cd.street_foreign)
                                                            END
                                                         )
                                                ELSE CONCAT(
                                                        dist.name, ', ',
                                                        CASE
                                                            WHEN resarea.name IS NULL THEN ''
                                                            ELSE CONCAT(resarea.type, ', ', resarea.name)
                                                            END,
                                                        CASE
                                                            WHEN strt.name IS NULL THEN ''
                                                            ELSE CONCAT(', ', strt.type, ', ', strt.name)
                                                            END
                                                     )
                                                END, ', ',
                                            cd.street_number, ', ',
                                            CASE WHEN cd.block IS NOT NULL THEN CONCAT('бл. ', cd.block, ', ') ELSE '' END,
                                            CASE WHEN cd.entrance IS NOT NULL THEN CONCAT('вх. ', cd.entrance, ', ') ELSE '' END,
                                            CASE WHEN cd.floor IS NOT NULL THEN CONCAT('ет. ', cd.floor, ', ') ELSE '' END,
                                            CASE WHEN cd.apartment IS NOT NULL THEN CONCAT('ап. ', cd.apartment, ', ') ELSE '' END,
                                            cd.address_additional_info
                                                               ),text('BULGARIAN'))                         AS CustomerAddressComb,
                                    CASE
                                        WHEN cd.foreign_address THEN cd.populated_place_foreign
                                        ELSE popp.name
                                        END                       AS CustomerPopulatedPlace,
                                    CASE
                                        WHEN cd.foreign_address THEN cd.zip_code_foreign
                                        ELSE zipc.zip_code
                                        END                       AS CustomerZip,
                                    CASE
                                        WHEN cd.foreign_address THEN cd.district_foreign
                                        ELSE dist.name
                                        END                       AS CustomerDistrict,
                                    translation.translate_text(CASE
                                                                   WHEN cd.foreign_address THEN text(cd.foreign_residential_area_type)
                                                                   ELSE text(resarea.type)
                                        END                      ,text('BULGARIAN')) AS CustomerQuarterRaType,
                                    CASE
                                        WHEN cd.foreign_address THEN cd.residential_area_foreign
                                        ELSE resarea.name
                                        END                       AS CustomerQuarterRaName,
                                    translation.translate_text(CASE
                                                                   WHEN cd.foreign_address THEN text(cd.foreign_street_type)
                                                                   ELSE text(strt.type)
                                        END ,text('BULGARIAN'))
                                        AS CustomerStrBlvdType,
                                    CASE
                                        WHEN cd.foreign_address THEN cd.street_foreign
                                        ELSE strt.name
                                        END                       AS CustomerStrBlvdName,
                                    cd.street_number              AS CustomerStrBlvdNumber,
                                    cd.block                      AS CustomerBlock,
                                    cd.entrance                   AS CustomerEntrance,
                                    cd.floor                      AS CustomerFloor,
                                    cd.apartment                  AS CustomerApartment,
                                    cd.address_additional_info    AS CustomerAdditionalInfo,
                                    ARRAY_AGG(DISTINCT segm.name) AS CustomerSegments
                    FROM receivable.late_payment_fines lpf
                             JOIN customer.customers c ON lpf.customer_id = c.id
                             JOIN customer.customer_details cd ON c.last_customer_detail_id = cd.id
                             LEFT JOIN nomenclature.districts dist ON cd.district_id = dist.id
                             LEFT JOIN nomenclature.residential_areas resarea ON cd.residential_area_id = resarea.id
                             LEFT JOIN nomenclature.streets strt ON cd.street_id = strt.id
                             LEFT JOIN nomenclature.legal_forms lf ON cd.legal_form_id = lf.id
                             LEFT JOIN nomenclature.populated_places popp ON cd.populated_place_id = popp.id
                             LEFT JOIN nomenclature.zip_codes zipc ON cd.zip_code_id = zipc.id
                             LEFT JOIN customer.customer_segments cussegm ON cd.id = cussegm.customer_detail_id
                             LEFT JOIN nomenclature.segments segm ON cussegm.segment_id = segm.id
                    WHERE lpf.id = :latePayemntFineId
                    GROUP BY CustomerNameComb, CustomerNameCombTrsl, CustomerIdentifier, CustomerVat, CustomerNumber,
                             CustomerAddressComb, CustomerPopulatedPlace, CustomerZip, CustomerDistrict, CustomerQuarterRaType,
                             CustomerQuarterRaName,
                             CustomerStrBlvdType, CustomerStrBlvdName, CustomerStrBlvdNumber, CustomerBlock, CustomerEntrance,
                             CustomerFloor, CustomerApartment,
                             CustomerAdditionalInfo
                    """
    )
    LatePaymentFineCustomerInfoResponse getCustomerInfoResponse(Long latePayemntFineId);

    @Query(
            nativeQuery = true,
            value = """
                    select cl.full_offset_date                    as FullPaymentDate,
                           translation.translate_text(text(inv.type),text('BULGARIAN'))                             as OverdueDocumentType,
                           inv.invoice_number                     as OverdueDocumentNumber,
                           split_part(inv.invoice_number, '-', 1) as OverdueDocumentPrefix,
                           inv.invoice_date                       as OverdueDocumentDate,
                           cl.initial_amount                      as LiabilityInitialAmount
                    from receivable.late_payment_fines lpf
                             join receivable.customer_liabilities cl on lpf.id = cl.child_late_payment_fine_id
                             left join invoice.invoices inv on cl.invoice_id = inv.id
                    where lpf.id = :latePaymentFineId;
                    """
    )
    LatePaymentFineOutDocInfoResponse getOutDocInfo(Long latePaymentFineId);

    @Query(
            nativeQuery = true,
            value = """
                    select lpinv.total_amount                     as InterestAmount,
                           lpinv.percentage                       as InterestRate,
                           lpinv.number_of_days                   as NumberDays,
                           lpinv.late_paid_amount                 as OverdueAmount,
                           inv.invoice_number                     as OverdueDocumentNumber,
                           lpinv.overdue_end_date                 as OverdueEndDate,
                           lpinv.overdue_start_date               as OverdueStartDate,
                           split_part(inv.invoice_number, '-', 1) as OverdueDocumentPrefix
                    from receivable.late_payment_fine_invoices lpinv
                             left join invoice.invoices inv on lpinv.invoice_id = inv.id
                    where lpinv.late_payment_fine_id = :latePaymentFineId
                    """
    )
    List<LatePaymentFineInterestsMiddleResponse> getInterestsInfo(Long latePaymentFineId);

    @Query("""
                    select lpf from LatePaymentFine lpf
                    where lpf.latePaymentNumber=:number
            """)
    Optional<LatePaymentFine> findByNumber(String number);

    boolean existsByIdAndReversed(Long id, boolean reversed);

}
