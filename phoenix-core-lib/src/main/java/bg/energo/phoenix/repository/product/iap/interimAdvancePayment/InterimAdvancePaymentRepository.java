package bg.energo.phoenix.repository.product.iap.interimAdvancePayment;

import bg.energo.phoenix.model.entity.product.iap.interimAdvancePayment.InterimAdvancePayment;
import bg.energo.phoenix.model.enums.product.iap.advancedPaymentGroup.AdvancedPaymentGroupStatus;
import bg.energo.phoenix.model.enums.product.iap.interimAdvancePayment.InterimAdvancePaymentStatus;
import bg.energo.phoenix.model.response.AdvancedPaymentGroup.InterimAdvancePaymentSearchListResponse;
import bg.energo.phoenix.model.response.copy.domain.CopyDomainListResponse;
import bg.energo.phoenix.model.response.interimAdvancePayment.InterimAdvancePaymentListResponse;
import bg.energo.phoenix.model.response.product.AvailableProductRelatedEntitiesResponse;
import bg.energo.phoenix.model.response.service.AvailableServiceRelatedEntitiyResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
public interface InterimAdvancePaymentRepository extends JpaRepository<InterimAdvancePayment, Long> {
    @Query("""
            select iap
            from AdvancedPaymentGroupDetails apgd
            join AdvancedPaymentGroup apg on apg.id = apgd.advancedPaymentGroupId
            join AdvancedPaymentGroupAdvancedPayments apgap on apgap.advancePaymentGroupDetailId = apgd.id
            join InterimAdvancePayment iap on iap.id = apgap.advancePaymentId
            where apg.id = :interimAdvancePaymentGroupId
            and apg.status in(:statuses)
            and apgap.status = 'ACTIVE'
            order by apgd.startDate desc
            """)
    Optional<InterimAdvancePayment> findRespectiveByInterimAdvancePaymentGroupId(Long interimAdvancePaymentGroupId,
                                                                                 List<AdvancedPaymentGroupStatus> statuses,
                                                                                 PageRequest limit);

    @Query("""
            select iap
            from InterimAdvancePayment iap
            join ProductInterimAndAdvancePayments piap on piap.interimAdvancePayment.id = iap.id
            join ProductDetails pd on pd.id = piap.productDetails.id
            where pd.id = :productDetailId
            and iap.status in(:statuses)
            and piap.productSubObjectStatus = 'ACTIVE'
            """)
    List<InterimAdvancePayment> findByProductDetailIdAndStatusIn(Long productDetailId,
                                                                 List<InterimAdvancePaymentStatus> statuses);

    @Query("""
            select iap
            from InterimAdvancePayment iap
            join ServiceInterimAndAdvancePayment siap on siap.interimAndAdvancePayment.id = iap.id
            join ServiceDetails sd on sd.id = siap.serviceDetails.id
            where sd.id = :serviceDetailId
            and iap.status in (:statuses)
            and siap.status = 'ACTIVE'
            """)
    List<InterimAdvancePayment> findByServiceDetailIdAndStatusIn(Long serviceDetailId,
                                                                 List<InterimAdvancePaymentStatus> statuses);

    Optional<InterimAdvancePayment> findByIdAndStatusIn(Long id, List<InterimAdvancePaymentStatus> statuses);

    List<InterimAdvancePayment> findByIdInAndStatusIn(List<Long> ids, List<InterimAdvancePaymentStatus> statuses);

    Optional<InterimAdvancePayment> findByIdAndStatus(Long id, InterimAdvancePaymentStatus statuses);

    @Query(nativeQuery = true,
            value = """
                    select
                     tbl.id as id,
                     tbl.name as name,
                     tbl.value_type as valueType,
                     tbl.deduction_from as deductionFrom,
                     coalesce(tbl.group_product_service_name, 'Available') as available,
                     tbl.create_date as createDate,
                     tbl.status as status
                    from
                    (select iap.id,
                           iap.name,
                           iap.value_type,
                           iap.deduction_from,
                           (select iapgd.name from interim_advance_payment.interim_advance_payment_group_details iapgd
                             join interim_advance_payment.interim_advance_payment_groups iapg
                               on iapgd.interim_advance_payment_group_id = iapg.id
                               and iapg.status = 'ACTIVE'
                             join interim_advance_payment.iap_group_iaps igi
                               on igi.interim_advance_payment_group_detail_id = iapgd.id
                              where igi.interim_advance_payment_id = iap.id
                                and igi.status = 'ACTIVE'
                               union
                                select pd.name from product.product_interim_advance_payments piap
                                join
                                product.product_details pd on piap.product_detail_id = pd.id
                                 and piap.interim_advance_payment_id = iap.id
                                 and piap.status = 'ACTIVE'
                                join product.products p on pd.product_id = p.id
                                 and p.status = 'ACTIVE'
                                  union
                                select sd.name from service.service_interim_advance_payments siap
                                join
                                service.service_details sd
                                on siap.service_detail_id = sd.id
                                 and siap.interim_advance_payment_id = iap.id
                                 and siap.status = 'ACTIVE'
                                join service.services s on sd.service_id = s.id
                                 and s.status = 'ACTIVE' limit 1
                                ) as group_product_service_name,
                           iap.create_date,
                           iap.status
                      from interim_advance_payment.interim_advance_payments iap
                        where text(iap.status) in (:statuses)
                          and (:columnValue is null
                           or (:columnName = 'NAME' and lower(iap.name) like :columnValue)
                           or (:columnName = 'ALL' and (lower(iap.name) like :columnValue)))
                          and ((:valueTypes)  is null or text(iap.value_type) in (:valueTypes))
                          and (:deductionFrom is null or text(iap.deduction_from) = text(:deductionFrom))) as tbl
                     where  (coalesce(:available, '0') = '0' or :available = 'YES' and tbl.group_product_service_name is null or
                           :available = 'NO' and tbl.group_product_service_name is not null)
                    """,
            countQuery = """
                     select
                     count(tbl.id)
                    from
                    (select iap.id,
                           iap.name,
                           iap.value_type,
                           iap.deduction_from,
                           (select iapgd.name from interim_advance_payment.interim_advance_payment_group_details iapgd
                             join interim_advance_payment.interim_advance_payment_groups iapg
                               on iapgd.interim_advance_payment_group_id = iapg.id
                               and iapg.status = 'ACTIVE'
                             join interim_advance_payment.iap_group_iaps igi
                               on igi.interim_advance_payment_group_detail_id = iapgd.id
                              where igi.interim_advance_payment_id = iap.id
                                and igi.status = 'ACTIVE'
                               union
                                select pd.name from product.product_interim_advance_payments piap
                                join
                                product.product_details pd on piap.product_detail_id = pd.id
                                 and piap.interim_advance_payment_id = iap.id
                                 and piap.status = 'ACTIVE'
                                join product.products p on pd.product_id = p.id
                                 and p.status = 'ACTIVE'
                                  union
                                select sd.name from service.service_interim_advance_payments siap
                                join
                                service.service_details sd
                                on siap.service_detail_id = sd.id
                                 and siap.interim_advance_payment_id = iap.id
                                 and siap.status = 'ACTIVE'
                                join service.services s on sd.service_id = s.id
                                 and s.status = 'ACTIVE' limit 1
                                ) as group_product_service_name,
                           iap.create_date,
                           iap.status
                      from interim_advance_payment.interim_advance_payments iap
                        where text(iap.status) in (:statuses)
                          and (:columnValue is null
                           or (:columnName = 'NAME' and lower(iap.name) like :columnValue)
                           or (:columnName = 'ALL' and (lower(iap.name) like :columnValue)))
                          and ((:valueTypes)  is null or text(iap.value_type) in (:valueTypes))
                          and (:deductionFrom is null or text(iap.deduction_from) = text(:deductionFrom))) as tbl
                     where  (coalesce(:available, '0') = '0' or :available = 'YES' and tbl.group_product_service_name is null or
                           :available = 'NO' and tbl.group_product_service_name is not null)
                     """)
    Page<InterimAdvancePaymentListResponse> filter(
            @Param("columnValue") String prompt,
            @Param("columnName") String searchField,
            @Param("statuses") List<String> statuses,
            @Param("valueTypes") List<String> valueTypes,
            @Param("deductionFrom") String deductionFrom,
            @Param("available") String available,
            Pageable pageable
    );

    @Query("""
            Select distinct iap
            from InterimAdvancePayment iap
                     left join InterimAdvancePaymentTerms pt on iap.id = pt.interimAdvancePayment.id
            where (iap.groupDetailId is null or iap.groupDetailId = :groupId)
              and not exists (select 1
                              from ServiceInterimAndAdvancePayment siap
                              where siap.interimAndAdvancePayment.id = iap.id
                                and siap.serviceDetails.service.status = 'ACTIVE'
                                and siap.status = 'ACTIVE')
              and not exists (select 1
                              from ProductInterimAndAdvancePayments piap
                              where piap.interimAdvancePayment.id = iap.id
                                and piap.productDetails.product.productStatus = 'ACTIVE'
                                and piap.productSubObjectStatus = 'ACTIVE')
              and (
                (iap.valueType = 'EXACT_AMOUNT'
                    and iap.value is not null
                    and iap.valueFrom is null
                    and iap.valueTo is null)
                    or
                (iap.valueType = 'PERCENT_FROM_PREVIOUS_INVOICE_AMOUNT'
                    and iap.value is not null
                    and iap.valueFrom is null
                    and iap.valueTo is null)
                    or
                (iap.valueType = 'PRICE_COMPONENT'
                    and not exists(select 1
                                   from PriceComponentFormulaVariable pcfv
                                   where pcfv.priceComponent.id = iap.priceComponent.id
                                     and cast(pcfv.variable as string) like 'X%'
                                     and (pcfv.value is null
                                       or pcfv.valueFrom is not null
                                       or pcfv.valueTo is not null
                                       ))
                    )
                )
              and (iap.dateOfIssueType = 'MATCH_THE_INVOICE_DATE'
                or
                   (iap.dateOfIssueType = 'DATE_OF_THE_MONTH'
                       and iap.dateOfIssueValue is not null
                       and iap.dateOfIssueValueFrom is null
                       and iap.dateOfIssueValueTo is null)
                or (iap.dateOfIssueType = 'WORKING_DAYS_AFTER_INVOICE_DATE'
                    and iap.dateOfIssueValue is not null
                    and iap.dateOfIssueValueFrom is null
                    and iap.dateOfIssueValueTo is null
                       ) or iap.dateOfIssueType = 'PERIODICAL')
              and (
                pt is null or
                (pt.value is not null
                    and pt.valueFrom is null
                    and pt.valueTo is null)
                )
              and iap.paymentType = 'OBLIGATORY'
              and iap.id in :advancePaymentIds
            """
    )
    List<InterimAdvancePayment> getAvailableAdvancePaymentForGroupDetail(List<Long> advancePaymentIds, Long groupId);

    @Query("""
            select distinct iap
            from InterimAdvancePayment iap
                     left join InterimAdvancePaymentTerms pt on iap.id = pt.interimAdvancePayment.id
            where iap.status = 'ACTIVE'
              and (
                (iap.valueType = 'EXACT_AMOUNT'
                    and iap.value is not null
                    and iap.valueFrom is null
                    and iap.valueTo is null)
                    or
                (iap.valueType = 'PERCENT_FROM_PREVIOUS_INVOICE_AMOUNT'
                    and iap.value is not null
                    and iap.valueFrom is null
                    and iap.valueTo is null)
                    or
                (iap.valueType = 'PRICE_COMPONENT'
                    and not exists(select 1
                                   from PriceComponentFormulaVariable pcfv
                                   where pcfv.priceComponent.id = iap.priceComponent.id
                                     and cast(pcfv.variable as string) like 'X%'
                                     and (pcfv.value is null
                                       or pcfv.valueFrom is not null
                                       or pcfv.valueTo is not null
                                       ))
                    ))
              and (iap.dateOfIssueType = 'MATCH_THE_INVOICE_DATE'
                or
                   (iap.dateOfIssueType = 'DATE_OF_THE_MONTH'
                       and iap.dateOfIssueValue is not null
                       and iap.dateOfIssueValueFrom is null
                       and iap.dateOfIssueValueTo is null)
                or (iap.dateOfIssueType = 'WORKING_DAYS_AFTER_INVOICE_DATE'
                    and iap.dateOfIssueValue is not null
                    and iap.dateOfIssueValueFrom is null
                    and iap.dateOfIssueValueTo is null
                       ) or iap.dateOfIssueType = 'PERIODICAL')
              and (
                pt is null or
                (pt.value is not null
                    and pt.valueFrom is null
                    and pt.valueTo is null)
                )
              and iap.paymentType = 'OBLIGATORY'
              and not exists (select 1
                              from ServiceInterimAndAdvancePayment siap
                              where siap.interimAndAdvancePayment.id = iap.id
                                and siap.serviceDetails.service.status = 'ACTIVE'
                                and siap.status = 'ACTIVE')
              and not exists (select 1
                              from ProductInterimAndAdvancePayments piap
                              where piap.interimAdvancePayment.id = iap.id
                                and piap.productDetails.product.productStatus = 'ACTIVE'
                                and piap.productSubObjectStatus = 'ACTIVE')
            """)
    List<InterimAdvancePayment> getAvailableAdvancePayments(List<Long> advancedPaymentsToCreate);

    @Query("""
            select iap from InterimAdvancePayment iap
            join AdvancedPaymentGroupDetails apgd on apgd.advancedPaymentGroupId = :id
            join AdvancedPaymentGroupAdvancedPayments apgap on apgap.advancePaymentGroupDetailId = apgd.id
            where iap.status = 'ACTIVE'
            """)
    List<InterimAdvancePayment> findAllActiveByAdvancedPaymentGroup(Long id);

    List<InterimAdvancePayment> findAllByGroupDetailIdAndStatusIn(Long groupDetailId, List<InterimAdvancePaymentStatus> statuses);

    @Query("""
            select distinct new bg.energo.phoenix.model.response.AdvancedPaymentGroup.InterimAdvancePaymentSearchListResponse(
                                                                                                                       iap.id,
                                                                                                                       iap.name,
                                                                                                                       iap.createDate
                                                                                                                   )
            from InterimAdvancePayment iap
                     left join InterimAdvancePaymentTerms pt on iap.id = pt.interimAdvancePayment.id
            where iap.status = 'ACTIVE'
              and (
                (iap.valueType = 'EXACT_AMOUNT'
                    and iap.value is not null
                    and iap.valueFrom is null
                    and iap.valueTo is null)
                    or
                (iap.valueType = 'PERCENT_FROM_PREVIOUS_INVOICE_AMOUNT'
                    and iap.value is not null
                    and iap.valueFrom is null
                    and iap.valueTo is null)
                    or
                (iap.valueType = 'PRICE_COMPONENT'
                    and not exists(select 1
                                   from PriceComponentFormulaVariable pcfv
                                   where pcfv.priceComponent.id = iap.priceComponent.id
                                     and cast(pcfv.variable as string) like 'X%'
                                     and (pcfv.value is null
                                       or pcfv.valueFrom is not null
                                       or pcfv.valueTo is not null
                                       ))
                    )
                )
              and (iap.dateOfIssueType = 'MATCH_THE_INVOICE_DATE'
                or
                   (iap.dateOfIssueType = 'DATE_OF_THE_MONTH'
                       and iap.dateOfIssueValue is not null
                       and iap.dateOfIssueValueFrom is null
                       and iap.dateOfIssueValueTo is null)
                or (iap.dateOfIssueType = 'WORKING_DAYS_AFTER_INVOICE_DATE'
                    and iap.dateOfIssueValue is not null
                    and iap.dateOfIssueValueFrom is null
                    and iap.dateOfIssueValueTo is null
                       ) or iap.dateOfIssueType = 'PERIODICAL')
              and (
                pt is null or
                (pt.value is not null
                    and pt.valueFrom is null
                    and pt.valueTo is null)
                )
              and iap.paymentType = 'OBLIGATORY'
              and (lower(iap.name) like lower(:columnValue) or (cast(iap.id as string) like lower(:columnValue)))
              and not exists (select 1
                              from AdvancedPaymentGroupAdvancedPayments igi
                                       join AdvancedPaymentGroupDetails apgd on apgd.id = igi.advancePaymentGroupDetailId
                                       join AdvancedPaymentGroup apg on apg.id = apgd.advancedPaymentGroupId
                              where igi.advancePaymentId = iap.id
                                and apg.status = 'ACTIVE'
                                and igi.status = 'ACTIVE')
              and not exists (select 1
                              from ServiceInterimAndAdvancePayment siap
                              where siap.interimAndAdvancePayment.id = iap.id
                                and siap.serviceDetails.service.status = 'ACTIVE'
                                and siap.status = 'ACTIVE')
              and not exists (select 1
                              from ProductInterimAndAdvancePayments piap
                              where piap.interimAdvancePayment.id = iap.id
                                and piap.productDetails.product.productStatus = 'ACTIVE'
                                and piap.productSubObjectStatus = 'ACTIVE')
            order by iap.createDate desc
            """)
    Page<InterimAdvancePaymentSearchListResponse> findAvailableInterimAndAdvancePayments(
            @Param("columnValue") String prompt,
            Pageable pageable
    );


    @Query("""
            select iap
            from InterimAdvancePayment iap
                     left join InterimAdvancePaymentTerms pt on iap.id = pt.interimAdvancePayment.id
            where iap.status = 'ACTIVE'
              and (
                (iap.valueType = 'EXACT_AMOUNT'
                    and iap.value is not null
                    and iap.valueFrom is null
                    and iap.valueTo is null)
                    or
                (iap.valueType = 'PERCENT_FROM_PREVIOUS_INVOICE_AMOUNT'
                    and iap.value is not null
                    and iap.valueFrom is null
                    and iap.valueTo is null)
                    or
                (iap.valueType = 'PRICE_COMPONENT'
                    and not exists(select 1
                                   from PriceComponentFormulaVariable pcfv
                                   where pcfv.priceComponent.id = iap.priceComponent.id
                                     and cast(pcfv.variable as string) like 'X%'
                                     and (pcfv.value is null
                                       or pcfv.valueFrom is not null
                                       or pcfv.valueTo is not null
                                       ))
                    )
                )
              and (iap.dateOfIssueType = 'MATCH_THE_INVOICE_DATE'
                or
                   (iap.dateOfIssueType = 'DATE_OF_THE_MONTH'
                       and iap.dateOfIssueValue is not null
                       and iap.dateOfIssueValueFrom is null
                       and iap.dateOfIssueValueTo is null)
                or (iap.dateOfIssueType = 'WORKING_DAYS_AFTER_INVOICE_DATE'
                    and iap.dateOfIssueValue is not null
                    and iap.dateOfIssueValueFrom is null
                    and iap.dateOfIssueValueTo is null
                       )
                or iap.dateOfIssueType = 'PERIODICAL')
              and (
                pt is null or
                (pt.value is not null
                    and pt.valueFrom is null
                    and pt.valueTo is null)
                )
              and iap.paymentType = 'OBLIGATORY'
              and not exists (select 1
                              from AdvancedPaymentGroupAdvancedPayments igi
                                       join AdvancedPaymentGroupDetails apgd on apgd.id = igi.advancePaymentGroupDetailId
                                       join AdvancedPaymentGroup apg on apg.id = apgd.advancedPaymentGroupId
                              where igi.advancePaymentId = iap.id
                                and apg.status = 'ACTIVE'
                                and igi.status = 'ACTIVE')
              and not exists (select 1
                              from ServiceInterimAndAdvancePayment siap
                              where siap.interimAndAdvancePayment.id = iap.id
                                and siap.serviceDetails.service.status = 'ACTIVE'
                                and siap.status = 'ACTIVE')
              and not exists (select 1
                              from ProductInterimAndAdvancePayments piap
                              where piap.interimAdvancePayment.id = iap.id
                                and piap.productDetails.product.productStatus = 'ACTIVE'
                                and piap.productSubObjectStatus = 'ACTIVE')
              and iap.id = :id
            order by iap.createDate desc
            """)
    Optional<InterimAdvancePayment> checkAvailableIap(Long id);

    @Query("""
             SELECT iap
             FROM InterimAdvancePayment iap
             LEFT JOIN AdvancedPaymentGroupAdvancedPayments iaps ON iaps.advancePaymentId = iap.id
             LEFT JOIN ProductInterimAndAdvancePayments  piap ON piap.interimAdvancePayment.id = iap.id
             LEFT JOIN ServiceInterimAndAdvancePayment siap ON siap.interimAndAdvancePayment.id = iap.id
             WHERE iap.id = :id
             AND iap.status = 'ACTIVE'
             AND ((iaps.status = 'ACTIVE' AND  iaps.advancePaymentId IS NOT NULL)
             OR (piap.productSubObjectStatus = 'ACTIVE' AND piap.interimAdvancePayment.id IS NOT NULL)
             OR (siap.status = 'ACTIVE' AND siap.interimAndAdvancePayment.id IS NOT NULL))
            """)
    Optional<InterimAdvancePayment> canDelete(Long id);

    @Query(
            value = """
                    select count (pd.id) > 0 from ProductDetails pd
                    join ProductInterimAndAdvancePayments piap on piap.productDetails.id = pd.id
                        where pd.product.productStatus = 'ACTIVE'
                        and piap.productDetails.productDetailStatus = 'ACTIVE'
                        and piap.interimAdvancePayment.id = :id
                    """
    )
    boolean hasConnectionToProduct(@Param("id") Long id);


    @Query(
            value = """
                    select count (sd.id) > 0 from ServiceDetails sd
                    join ServiceInterimAndAdvancePayment siap on siap.serviceDetails.id = sd.id
                        where sd.service.status = 'ACTIVE'
                        and siap.serviceDetails.status = 'ACTIVE'
                        and siap.interimAndAdvancePayment.id = :id
                    """
    )
    boolean hasConnectionToService(@Param("id") Long id);


    @Query("""
            select new bg.energo.phoenix.model.response.product.AvailableProductRelatedEntitiesResponse(iap.id,iap.name) from InterimAdvancePayment iap
            where iap.groupDetailId is null
            and iap.status = 'ACTIVE'
            and not exists (
                select 1 from Product p
                join ProductDetails pd on pd.product.id = p.id
                join ProductInterimAndAdvancePayments piap on 
                piap.productDetails.id = pd.id
                and 
                piap.interimAdvancePayment.id = iap.id
                    where p.productStatus = 'ACTIVE'
                    and piap.productSubObjectStatus = 'ACTIVE'
            )
            and not exists (
                select 1 from EPService s
                join ServiceDetails sd on sd.service.id = s.id
                join ServiceInterimAndAdvancePayment siap on
                 siap.serviceDetails.id = sd.id
                 and
                 siap.interimAndAdvancePayment.id = iap.id
                    where s.status = 'ACTIVE'
                    and siap.status = 'ACTIVE'
            )
            and (:prompt is null or (lower(iap.name) like concat('%',lower(:prompt),'%') or text(iap.id)=:prompt))
            order by iap.createDate desc
            """)
    Page<AvailableProductRelatedEntitiesResponse> findAvailableAdvancePaymentsForProduct(@Param("prompt") String prompt, PageRequest pageRequest);


    @Query(
            value = """
                    select new bg.energo.phoenix.model.response.service.AvailableServiceRelatedEntitiyResponse(
                        iap.id,
                        iap.name
                    )
                    from InterimAdvancePayment iap
                        where iap.groupDetailId is null
                        and iap.status = 'ACTIVE'
                        and not exists (
                            select 1 from Product p
                            join ProductDetails pd on pd.product.id = p.id
                            join ProductInterimAndAdvancePayments piap on 
                            piap.productDetails.id = pd.id
                            and 
                            piap.interimAdvancePayment.id = iap.id
                                where p.productStatus = 'ACTIVE'
                                and piap.productSubObjectStatus = 'ACTIVE'
                        )
                        and not exists (
                            select 1 from EPService s
                            join ServiceDetails sd on sd.service.id = s.id
                            join ServiceInterimAndAdvancePayment siap on
                             siap.serviceDetails.id = sd.id
                             and
                             siap.interimAndAdvancePayment.id = iap.id
                                where s.status = 'ACTIVE'
                                and siap.status = 'ACTIVE'
                        )
                        and (:prompt is null or concat(lower(iap.name),iap.id) like :prompt)
                    order by iap.createDate desc
                    """
    )
    Page<AvailableServiceRelatedEntitiyResponse> findAvailableInterimAdvancePaymentsForService(
            @Param("prompt") String prompt,
            Pageable pageable
    );


    @Query("""
            select iap.id from InterimAdvancePayment iap
                        where iap.groupDetailId is null
                        and iap.status = 'ACTIVE'
                        and not exists (
                            select 1 from Product p
                            join ProductDetails pd on pd.product.id = p.id
                            join ProductInterimAndAdvancePayments piap on 
                            piap.productDetails.id = pd.id
                            and 
                            piap.interimAdvancePayment.id = iap.id
                                where p.productStatus = 'ACTIVE'
                                and piap.productSubObjectStatus = 'ACTIVE'
                        )
                        and not exists (
                            select 1 from EPService s
                            join ServiceDetails sd on sd.service.id = s.id
                            join ServiceInterimAndAdvancePayment siap on
                             siap.serviceDetails.id = sd.id
                             and
                             siap.interimAndAdvancePayment.id = iap.id
                                where s.status = 'ACTIVE'
                                and siap.status = 'ACTIVE'
                        )
                        and iap.id in (:ids)
            """)
    List<Long> findAvailableAdvancePaymentIdsForProduct(Collection<Long> ids);


    @Query(
            value = """
                    select iap.id from InterimAdvancePayment iap
                        where iap.groupDetailId is null
                        and iap.status = 'ACTIVE'
                        and not exists (
                            select 1 from Product p
                            join ProductDetails pd on pd.product.id = p.id
                            join ProductInterimAndAdvancePayments piap on 
                            piap.productDetails.id = pd.id
                            and 
                            piap.interimAdvancePayment.id = iap.id
                                where p.productStatus = 'ACTIVE'
                                and piap.productSubObjectStatus = 'ACTIVE'
                        )
                        and not exists (
                            select 1 from EPService s
                            join ServiceDetails sd on sd.service.id = s.id
                            join ServiceInterimAndAdvancePayment siap on
                             siap.serviceDetails.id = sd.id
                             and
                             siap.interimAndAdvancePayment.id = iap.id
                                where s.status = 'ACTIVE'
                                and siap.status = 'ACTIVE'
                        )
                        and iap.id in (:ids)
                    """
    )
    List<Long> findAvailableAdvancePaymentIdsForService(Collection<Long> ids);


    @Query("""
            select new bg.energo.phoenix.model.response.copy.domain.CopyDomainListResponse(iap.id, concat(iap.name, ' (', iap.id, ')'))
                from InterimAdvancePayment iap
                where (:prompt is null
                or lower(iap.name) like :prompt
                or concat('%', cast(iap.id as string), '%') = :prompt)
                and iap.status in (:statuses)
                order by iap.id DESC
            """)
    Page<CopyDomainListResponse> filterForCopy(@Param("prompt") String prompt,
                                               @Param("statuses") List<InterimAdvancePaymentStatus> statuses,
                                               Pageable pageable);

    List<InterimAdvancePayment> findAllByIdIn(Set<Long> ids);

    @Query("""
            select iap
            from InterimAdvancePayment iap
            where iap.priceComponent.id = :priceComponentId
            and iap.groupDetailId is not null
             """)
    Optional<InterimAdvancePayment> findByPriceComponentId(@Param("priceComponentId") Long id);

    @Query(
            nativeQuery = true,
            value =
                    """

                                                      select coalesce(max('true'),'false') as is_connected from interim_advance_payment.interim_advance_payments iap
                            where iap.id = :interimadvancepaymentid and
                             (exists (select 1
                                      from product.products p
                                           join product.product_details pd
                                             on pd.product_id = p.id
                                            and p.status = 'ACTIVE'
                                           join product.product_interim_advance_payments piap
                                             on piap.product_detail_id = pd.id
                                            and piap.interim_advance_payment_id = iap.id
                                            and piap.status = 'ACTIVE'
                                           join product_contract.contract_details cd
                                             on cd.product_detail_id =  pd.id
                                           join product_contract.contracts c
                                             on cd.contract_id =  c.id
                                            and c.status = 'ACTIVE')
                              or
                              exists
                                      (select 1
                                                  from product.products p
                                                       join product.product_details pd
                                                         on pd.product_id = p.id
                                                        and p.status = 'ACTIVE'
                                                       join product.product_interim_advance_payment_groups piapg
                                                         on piapg.product_detail_id = pd.id
                                                        and piapg.status = 'ACTIVE'
                                                       join interim_advance_payment.interim_advance_payment_groups iapg
                                                         on piapg.interim_advance_payment_group_id = iapg.id
                                                        and iapg.status = 'ACTIVE'
                                                       join interim_advance_payment.interim_advance_payment_group_details iapgd
                                                         on iapgd.interim_advance_payment_group_id = iapg.id
                                                       join interim_advance_payment.iap_group_iaps igi
                                                         on igi.interim_advance_payment_group_detail_id = iapgd.id
                                                        and igi.interim_advance_payment_id =  iap.id
                                                        and igi.status = 'ACTIVE'
                                                       join product_contract.contract_details cd
                                                         on cd.product_detail_id =  pd.id
                                                       join product_contract.contracts c
                                                         on cd.contract_id =  c.id
                                                        and c.status = 'ACTIVE')
                              or
                              exists (select 1
                                          from service.services s
                                               join service.service_details sd
                                                 on sd.service_id = s.id
                                                and s.status = 'ACTIVE'
                                               join service.service_interim_advance_payments siap
                                                 on siap.service_detail_id = sd.id
                                                and siap.interim_advance_payment_id = iap.id
                                                and siap.status = 'ACTIVE'                     
                                               join service_contract.contract_details cd
                                                 on cd.service_detail_id =  sd.id
                                               join service_contract.contracts c
                                                 on cd.contract_id =  c.id
                                                and c.status = 'ACTIVE')
                              or
                              exists (select 1
                                          from service.services s
                                               join service.service_details sd
                                                 on sd.service_id = s.id
                                                and s.status = 'ACTIVE'
                                               join service.service_interim_advance_payments siap
                                                 on siap.service_detail_id = sd.id
                                                and siap.interim_advance_payment_id = iap.id
                                                and siap.status = 'ACTIVE'
                                               join service_order.orders o
                                                 on o.service_detail_id =  sd.id
                                                and o.status = 'ACTIVE'
                                             )
                              or
                              exists
                                      (select 1
                                                  from service.services s
                                                       join service.service_details sd
                                                         on sd.service_id = s.id
                                                        and s.status = 'ACTIVE'
                                                       join service.service_interim_advance_payment_groups siapg
                                                         on siapg.service_detail_id = sd.id
                                                        and siapg.status = 'ACTIVE'
                                                       join interim_advance_payment.interim_advance_payment_groups iapg 
                                                         on siapg.interim_advance_payment_group_id = iapg.id
                                                        and iapg.status = 'ACTIVE'
                                                       join interim_advance_payment.interim_advance_payment_group_details iapgd
                                                         on iapgd.interim_advance_payment_group_id = iapg.id
                                                       join interim_advance_payment.iap_group_iaps igi
                                                         on igi.interim_advance_payment_group_detail_id = iapgd.id
                                                        and igi.interim_advance_payment_id =  iap.id
                                                        and igi.status = 'ACTIVE'                      
                                                       join service_contract.contract_details cd
                                                         on cd.service_detail_id =  sd.id
                                                       join service_contract.contracts c
                                                         on cd.contract_id = c.id
                                                        and c.status = 'ACTIVE'
                                                        )
                             or
                              exists
                                      (select 1
                                                  from service.services s
                                                       join service.service_details sd
                                                         on sd.service_id = s.id
                                                        and s.status = 'ACTIVE'
                                                       join service.service_interim_advance_payment_groups siapg
                                                         on siapg.service_detail_id = sd.id
                                                        and siapg.status = 'ACTIVE'
                                                       join interim_advance_payment.interim_advance_payment_groups iapg
                                                         on siapg.interim_advance_payment_group_id = iapg.id
                                                        and iapg.status = 'ACTIVE'
                                                       join interim_advance_payment.interim_advance_payment_group_details iapgd
                                                         on iapgd.interim_advance_payment_group_id = iapg.id
                                                       join interim_advance_payment.iap_group_iaps igi
                                                         on igi.interim_advance_payment_group_detail_id = iapgd.id
                                                        and igi.interim_advance_payment_id =  iap.id
                                                        and igi.status = 'ACTIVE'
                                                       join service_order.orders o
                                                         on o.service_detail_id =  sd.id
                                                        and o.status = 'ACTIVE'
                                                        )   
                                                       
                             )                                         
                                                                  """
    )
    boolean hasLockedConnection(
            @Param("interimadvancepaymentid") Long id
    );
}

