package bg.energo.phoenix.repository.receivable.rescheduling;

import bg.energo.phoenix.model.entity.EntityStatus;
import bg.energo.phoenix.model.entity.receivable.rescheduling.Rescheduling;
import bg.energo.phoenix.model.entity.receivable.rescheduling.ReschedulingPlans;
import bg.energo.phoenix.model.enums.receivable.rescheduling.ReschedulingStatus;
import bg.energo.phoenix.model.response.receivable.rescheduling.ReschedulingAddressResponse;
import bg.energo.phoenix.model.response.receivable.rescheduling.ReschedulingListingResponse;
import bg.energo.phoenix.model.response.shared.ShortResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface ReschedulingRepository extends JpaRepository<Rescheduling, Long> {

    @Query(
            """
                    select new bg.energo.phoenix.model.response.shared.ShortResponse(resch.id, resch.reschedulingNumber)
                    from Rescheduling resch
                    where resch.reschedulingNumber = :reschedulingNumber
                    """
    )
    ShortResponse findByReschedulingNumber(String reschedulingNumber);

    Optional<Rescheduling> findByIdAndStatus(Long id, EntityStatus status);

    @Query(
            nativeQuery = true,
            value = """
                    
                                    select r.rescheduling_number       as reschedulingNumber,
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
                               end                        customer,
                           r.rescheduling_status       as reschedulingStatus,
                           r.number_of_installment     as numberOfInstallments,
                           r.amount_of_the_installment as amountOfTheInstallment,
                           r.installment_due_day       as installmentDueDateOfTheMonth,
                           date(r.create_date)         as creationDate,
                           r.status                    as status,
                           r.id                        as id,
                           r.reversed                  as reversed
                    from receivable.reschedulings r
                             join customer.customers c on r.customer_id = c.id
                             join customer.customer_details cd on c.last_customer_detail_id = cd.id
                             left join nomenclature.legal_forms lf on cd.legal_form_id = lf.id
                    where ((:statuses) is null or text(r.status) in :statuses)
                      and ((:reschedulingStatuses) is null or text(r.rescheduling_status) in :reschedulingStatuses)
                      and ((:reverseStatuses) is null or r.reversed in :reverseStatuses)
                      and (coalesce(:numberOfInstallmentFrom, '0') = '0' or r.number_of_installment >= :numberOfInstallmentFrom)
                      and (coalesce(:numberOfInstallmentTo, '0') = '0' or r.number_of_installment <= :numberOfInstallmentTo)
                      and (coalesce(:installmentDueDayFrom, '0') = '0' or r.installment_due_day >= :installmentDueDayFrom)
                      and (coalesce(:installmentDueDayTo, '0') = '0' or r.installment_due_day <= :installmentDueDayTo)
                      and (date(:createDateFrom) is null or date(r.create_date) >= date(:createDateFrom))
                      and (date(:createDateTo) is null or date(r.create_date) <= date(:createDateTo))
                      and (:prompt is null or
                           (:searchBy = 'ALL' and (lower(r.rescheduling_number) like :prompt or c.identifier like :prompt))
                        or (((:searchBy = 'CUSTOMER' and c.identifier like :prompt)
                            or (:searchBy = 'RESCHEDULING_NUMBER' and lower(r.rescheduling_number) like :prompt))))
                    """,
            countQuery = """
                    select count(r.id)
                    from receivable.reschedulings r
                             join customer.customers c on r.customer_id = c.id
                             join customer.customer_details cd on c.last_customer_detail_id = cd.id
                             left join nomenclature.legal_forms lf on cd.legal_form_id = lf.id
                    where ((:statuses) is null or text(r.status) in :statuses)
                      and ((:reschedulingStatuses) is null or text(r.rescheduling_status) in :reschedulingStatuses)
                      and ((:reverseStatuses) is null or r.reversed in :reverseStatuses)
                      and (coalesce(:numberOfInstallmentFrom, '0') = '0' or r.number_of_installment >= :numberOfInstallmentFrom)
                      and (coalesce(:numberOfInstallmentTo, '0') = '0' or r.number_of_installment <= :numberOfInstallmentTo)
                      and (coalesce(:installmentDueDayFrom, '0') = '0' or r.installment_due_day >= :installmentDueDayFrom)
                      and (coalesce(:installmentDueDayTo, '0') = '0' or r.installment_due_day <= :installmentDueDayTo)
                      and (date(:createDateFrom) is null or date(r.create_date) >= date(:createDateFrom))
                      and (date(:createDateTo) is null or date(r.create_date) <= date(:createDateTo))
                      and (:prompt is null or
                           (:searchBy = 'ALL' and (lower(r.rescheduling_number) like :prompt or c.identifier like :prompt))
                        or (((:searchBy = 'CUSTOMER' and c.identifier like :prompt)
                            or (:searchBy = 'RESCHEDULING_NUMBER' and lower(r.rescheduling_number) like :prompt))))
                    """
    )
    Page<ReschedulingListingResponse> filter(
            @Param("searchBy") String searchBy,
            @Param("prompt") String prompt,
            @Param("numberOfInstallmentFrom") BigDecimal numberOfInstallmentFrom,
            @Param("numberOfInstallmentTo") BigDecimal numberOfInstallmentTo,
            @Param("installmentDueDayFrom") BigDecimal installmentDueDayFrom,
            @Param("installmentDueDayTo") BigDecimal installmentDueDayTo,
            @Param("createDateFrom") LocalDate createDateFrom,
            @Param("createDateTo") LocalDate createDateTo,
            @Param("statuses") List<String> statuses,
            @Param("reschedulingStatuses") List<String> reschedulingStatuses,
            @Param("reverseStatuses") List<Boolean> reverseStatuses,
            Pageable pageable
    );

    Optional<Rescheduling> findByCustomerAssessmentIdAndReschedulingStatus(Long customerAssessmentId, ReschedulingStatus status);

    boolean existsByCustomerAssessmentIdAndStatus(Long customerAssessmentId, EntityStatus status);

    @Query("""
                   select r from Rescheduling r
                   where r.reschedulingNumber=:number
            """)
    Optional<Rescheduling> findByNumber(String number);

    @Query(value = """
            with default_nomenclature as (select c.print_name, c.abbreviation, c.full_name
                                          from nomenclature.currencies c
                                          where c.is_default = true),
                 installment_totals as (select rescheduling_id,
                                               sum(amount)           as total_amount,
                                               sum(principal_amount) as total_principal,
                                               sum(interest_amount)  as total_interest
                                        from receivable.rescheduling_plans
                                        where rescheduling_id = :reschedulingId
                                        group by rescheduling_id)
            select case
                       when c.customer_type = 'PRIVATE_CUSTOMER'
                           then concat(cd.name, ' ', cd.middle_name, ' ', cd.last_name)
                       else concat(cd.name, ' ', lf.name) end                                             as CustomerNameComb,
                   case
                       when c.customer_type = 'PRIVATE_CUSTOMER'
                           then concat(cd.name_transl, ' ', cd.middle_name_transl, ' ',
                                       cd.last_name_transl)
                       else concat(cd.name_transl, ' ', lft.name) end                                     as CustomerNameCombTrsl,
                   c.identifier                                                                           as CustomerIdentifier,
                   c.customer_number                                                                      as CustomerNumber,
                   case when cd.foreign_address is true then cd.populated_place_foreign else pp.name end  as CustomerPopulatedPlace,
                   case when cd.foreign_address is true then cd.zip_code_foreign else zc.zip_code end     as CustomerZip,
                   case when cd.foreign_address is true then cd.district_foreign else d.name end          as CustomerDistrict,
                   translation.translate_text(
                           case when cd.foreign_address is true then text(cd.street_type) else text(s.type) end,
                           text('BULGARIAN')
                   )                                                                                      as CustomerStrBlvdType,
                   case when cd.foreign_address is true then cd.street_foreign else s.name end            as CustomerStrBlvdName,
                   case when cd.foreign_address is true then cd.block else cd.block end                   as CustomerBlock,
                   cd.entrance                                                                            as CustomerEntrance,
                   cd.floor                                                                               as CustomerFloor,
                   cd.apartment                                                                           as CustomerApartment,
                   cd.address_additional_info                                                             as CustomerAdditionalInfo,
                   case when cd.foreign_address is true then cd.residential_area_foreign else ra.name end as CustomerQuarterRaName,
                   translation.translate_text(
                           case when cd.foreign_address is true then text(cd.residential_area_type) else text(ra.type) end,
                           text('BULGARIAN')
                   )                                                                                      as CustomerQuarterRaType,
                   cd.street_number                                                                       as CustomerStrBlvdNumber,
                   string_agg(distinct seg.name, ', ')                                                    as CustomerSegments,
                   translation.translate_text(concat_ws(', ',
                                                        nullif(concat_ws(', ',
                                                                         case
                                                                             when cd.foreign_address is true
                                                                                 then cd.district_foreign
                                                                             else d.name end,
                                                                         case
                                                                             when cd.foreign_address is true then
                                                                                 case
                                                                                     when cd.foreign_residential_area_type is not null
                                                                                         then concat(
                                                                                             cd.foreign_residential_area_type, ' ',
                                                                                             cd.residential_area_foreign)
                                                                                     else cd.residential_area_foreign
                                                                                     end
                                                                             else
                                                                                 case
                                                                                     when ra.type is not null
                                                                                         then concat(ra.type, ' ', ra.name)
                                                                                     else ra.name
                                                                                     end
                                                                             end
                                                               ), ''),
                                                        nullif(concat_ws(' ',
                                                                         case
                                                                             when cd.foreign_address is true then cd.street_type
                                                                             else s.type end,
                                                                         case
                                                                             when cd.foreign_address is true then cd.street_foreign
                                                                             else s.name end,
                                                                         cd.street_number
                                                               ), ''),
                                                        nullif(concat('бл. ', cd.block), 'бл. '),
                                                        nullif(concat('вх. ', cd.entrance), 'вх. '),
                                                        nullif(concat('ет. ', cd.floor), 'ет. '),
                                                        nullif(concat('ап. ', cd.apartment), 'ап. '),
                                                        nullif(cd.address_additional_info, '')
                                              ), text('BULGARIAN'))                                       as CustomerAddressComb,
                   default_nomenclature.print_name                                                        as CurrencyPrintName,
                   default_nomenclature.abbreviation                                                      as CurrencyAbr,
                   default_nomenclature.full_name                                                         as CurrencyFullName,
                   it.total_amount                                                                        as TotalInstallmentsAmount,
                   it.total_principal                                                                     as TotalInstallmentsPrinciple,
                   it.total_interest                                                                      as TotalInstallmentsInterests,
                   min(cl.create_date)                                                                    as LiabilitiesPeriodFrom,
                   max(cl.create_date)                                                                    as LiabilitiesPeriodTo
            from receivable.reschedulings r
                     join customer.customers c on r.customer_id = c.id
                     join customer.customer_details cd on cd.id = :customerDetailId
                     left join nomenclature.legal_forms lf on cd.legal_form_id = lf.id
                     left join nomenclature.legal_forms_transl lft on cd.legal_form_transl_id = lft.id
                     left join nomenclature.populated_places pp on cd.populated_place_id = pp.id
                     left join nomenclature.zip_codes zc on cd.zip_code_id = zc.id
                     left join nomenclature.districts d on cd.district_id = d.id
                     left join nomenclature.streets s on cd.street_id = s.id
                     left join nomenclature.residential_areas ra on cd.residential_area_id = ra.id
                     left join customer.customer_segments cs on cs.customer_detail_id = cd.id
                     left join nomenclature.segments seg on cs.segment_id = seg.id
                     left join default_nomenclature on true
                     left join installment_totals it on it.rescheduling_id = r.id
                     left join receivable.rescheduling_liabilities rl on rl.rescheduling_id = r.id
                     left join receivable.customer_liabilities cl on rl.customer_liabilitie_id = cl.id
            where r.id = :reschedulingId
            group by c.customer_type,
                     cd.name,
                     cd.middle_name,
                     cd.last_name,
                     cd.name_transl,
                     cd.middle_name_transl,
                     cd.last_name_transl,
                     lf.name,
                     c.identifier,
                     c.customer_number,
                     cd.foreign_address,
                     cd.populated_place_foreign,
                     pp.name,
                     cd.zip_code_foreign,
                     zc.zip_code,
                     cd.district_foreign,
                     d.name,
                     cd.street_type,
                     s.type,
                     cd.street_foreign,
                     s.name,
                     cd.block,
                     cd.entrance,
                     cd.floor,
                     cd.apartment,
                     cd.address_additional_info,
                     cd.foreign_residential_area_type,
                     cd.residential_area_foreign,
                     ra.type,
                     ra.name,
                     cd.street_number,
                     cd.residential_area_type,
                     default_nomenclature.print_name,
                     default_nomenclature.abbreviation,
                     default_nomenclature.full_name,
                     it.total_amount,
                     it.total_principal,
                     it.total_interest,
                     lft.name
            """, nativeQuery = true)
    Optional<ReschedulingAddressResponse> findCustomerAddressInfoForRescheduling(
            @Param("reschedulingId") Long reschedulingId,
            @Param("customerDetailId") Long customerDetailId
    );

    @Query("""
                    select ri from ReschedulingPlans ri
                    where ri.reschedulingId=:reschedulingId
            """)
    List<ReschedulingPlans> findInstallmentsByReschedulingId(@Param("reschedulingId") Long reschedulingId);

    @Query("""
                    select rl,cl,i,lpf from ReschedulingLiabilities rl
                    join CustomerLiability cl on rl.customerLiabilitieId=cl.id
                    join LatePaymentFine lpf on cl.childLatePaymentFineId=lpf.id
                    left join Invoice i on cl.invoiceId=i.id
                    where rl.reschedulingId=:reschedulingId
            """)
    List<Object[]> findReschedulingLiabilitiesByReschedulingId(@Param("reschedulingId") Long reschedulingId);
}
