package bg.energo.phoenix.repository.receivable.manualLiabilityOffsetting;

import bg.energo.phoenix.model.entity.receivable.manualLiabilityOffsetting.ManualLiabilityOffsetting;
import bg.energo.phoenix.model.response.receivable.manualLiabilityOffsetting.LiabilitiesMiddleResponse;
import bg.energo.phoenix.model.response.receivable.manualLiabilityOffsetting.MLOCustomerResultForLiability;
import bg.energo.phoenix.model.response.receivable.manualLiabilityOffsetting.ManualLIabilityOffsettingListingMiddleResponse;
import bg.energo.phoenix.model.response.receivable.manualLiabilityOffsetting.MloCustomerMiddleResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.sql.Date;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface ManualLiabilityOffsettingRepository extends JpaRepository<ManualLiabilityOffsetting, Long> {


    @Query(nativeQuery = true,
            value = """
                    select
                        cls.id as id,
                        CASE
                            WHEN cls.outgoing_document_from_external_system IS NOT NULL
                            THEN concat(cls.outgoing_document_from_external_system, '/', 
                                 coalesce(to_char(cls.occurrence_date, 'DD.MM.YYYY'), 'N/A'), ' | ', 
                                 to_char(cls.due_date, 'DD.MM.YYYY'), ' | ', 
                                 round(cls.current_amount::numeric, 2), ' ', c.name)
                            ELSE concat(cls.liability_number, '/', 
                                 coalesce(to_char(cls.occurrence_date, 'DD.MM.YYYY'), 'N/A'), ' | ', 
                                 to_char(cls.due_date, 'DD.MM.YYYY'), ' | ', 
                                 round(cls.current_amount::numeric, 2), ' ', c.name)
                        END as currentAmount,
                        cls.customer_id as customerId,
                        cls.current_amount as amount,
                        cls.currency_id as currencyId
                    from receivable.customer_liabilities cls
                    join nomenclature.currencies c on cls.currency_id = c.id
                    where cls.customer_id = :customerId
                    and cls.current_amount > 0
                    and cls.status = 'ACTIVE'
                    and (cls.occurrence_date is null or cls.occurrence_date <= :date)
                    and receivable.check_liability_offestting_allowed(cls.id, :date)
                    order by cls.due_date , cls.id
                    """)
    List<LiabilitiesMiddleResponse> getCustomerLiabilities(Long customerId, @Param("date") Date date);

    @Query(nativeQuery = true,
            value = """
                            select
                                  cl.id as id
                                  from
                                  receivable.customer_liabilities cl
                                  where cl.customer_id = :customerId
                                  and cl.current_amount > 0
                                  and cl.status = 'ACTIVE'
                    """)
    List<Long> getCustomerLiabilitiesIds(Long customerId);

    @Query(nativeQuery = true,
            value = """
                            select
                                  cl.id as id
                                  from receivable.late_payment_fines lpf
                                  join receivable.customer_liabilities cl
                                  on cl.late_payment_fine_id  = lpf.id
                                  where lpf.customer_id = :customerId
                                  and cl.current_amount > 0
                                  and cl.status = 'ACTIVE'
                    """)
    List<Long> getLatePaymentFinesIds(Long customerId);

    @Query(nativeQuery = true,
            value = """
                    select 
                        crv.id as id,
                        CASE
                            WHEN crv.outgoing_document_from_external_system IS NOT NULL
                            THEN concat(crv.outgoing_document_from_external_system, '/', 
                                 coalesce(to_char(crv.occurrence_date, 'DD.MM.YYYY'), 'N/A'), ' | ', 
                                 coalesce(to_char(crv.occurrence_date, 'DD.MM.YYYY'), 'N/A'), ' | -', 
                                 round(abs(crv.current_amount)::numeric, 2), ' ', c.name)
                            ELSE concat(crv.receivable_number, '/', 
                                 coalesce(to_char(crv.occurrence_date, 'DD.MM.YYYY'), 'N/A'), ' | ', 
                                 to_char(crv.due_date, 'DD.MM.YYYY'), ' | -', 
                                 round(abs(crv.current_amount)::numeric, 2), ' ', c.name)
                        END as currentAmount,
                        crv.customer_id as customerId,
                        -abs(crv.current_amount) as amount,
                        crv.currency_id as currencyId
                    from receivable.customer_receivables crv
                    join nomenclature.currencies c on crv.currency_id = c.id
                    where crv.customer_id = :customerId
                    and crv.current_amount > 0
                    and crv.status = 'ACTIVE'
                    and (crv.occurrence_date is null or crv.occurrence_date <= :date)
                    and receivable.check_receivable_offestting_allowed(crv.id, :date)
                    order by crv.occurrence_date , crv.id
                    """)
    List<LiabilitiesMiddleResponse> getReceivables(Long customerId, @Param("date") Date date);

    @Query(nativeQuery = true,
            value = """
                            select
                             cr.id as id
                            from
                            receivable.customer_receivables cr
                            where cr.customer_id = :customerId
                            and cr.current_amount > 0
                            and cr.status = 'ACTIVE'
                    """)
    List<Long> getReceivablesIds(Long customerId);

    @Query(nativeQuery = true,
            value = """
                            select concat(cd.deposit_number, '/',
                                          date(cd.create_date), ' | ', date(cd.create_date),
                                          ' | -', round(cd.current_amount::numeric, 2), ' ', curr.abbreviation
                                   )                 as currentAmount,
                                   cd.customer_id    as customerId,
                                   cd.id             as id,
                                   cd.current_amount as amount,
                                   cd.currency_id    as currencyId
                            from receivable.customer_deposits cd
                                     join nomenclature.currencies curr on cd.currency_id = curr.id
                            where cd.customer_id = :customerId
                              and cd.current_amount > 0
                              and cd.status = 'ACTIVE'
                              and date(cd.create_date) <= :date
                    """)
    List<LiabilitiesMiddleResponse> getDeposits(Long customerId, Date date);

    @Query(nativeQuery = true,
            value = """
                    select 
                        p.id as id,
                        concat(p.payment_number, '/', 
                               to_char(p.payment_date, 'DD.MM.YYYY'), ' | ', 
                               to_char(p.payment_date, 'DD.MM.YYYY'), ' | ', 
                               round(p.current_amount::numeric, 2), ' ', c.name) as currentAmount,
                        p.customer_id as customerId,
                        p.current_amount as amount,
                        c.id as currencyId
                    from receivable.customer_payments p
                    join nomenclature.currencies c on p.currency_id = c.id
                    where p.customer_id = :customerId
                    and p.current_amount < 0
                    and p.payment_date <= :date
                    and receivable.check_payment_offestting_allowed(p.id, :date)
                    order by p.payment_date , p.id
                    """)
    List<LiabilitiesMiddleResponse> getPayments(Long customerId, @Param("date") Date date);

    @Query(nativeQuery = true,
            value = """
                            select
                             cd.id as id
                            from
                            receivable.customer_deposits cd
                            where cd.customer_id = :customerId
                            and cd.current_amount > 0
                            and cd.status = 'ACTIVE'
                    """)
    List<Long> getDepositsIds(Long customerId);

    @Query(
            nativeQuery = true,
            value = """
                    select mlo.id                     as id,
                           mlo.manual_liabilitie_date as manualLiabilityDate,
                           case
                               when c.customer_type = 'PRIVATE_CUSTOMER' then
                                   concat(
                                           c.identifier, ' (',
                                           cd.name,
                                           case when cd.middle_name is not null then concat(' ', cd.middle_name) else '' end,
                                           case when cd.last_name is not null then concat(' ', cd.last_name) else '' end,
                                           ')'
                                   )
                               when c.customer_type = 'LEGAL_ENTITY' then concat(c.identifier, ' (', cd.name, ' ', lf.name, ')')
                               end                       customer,
                           mlo.reversed               as reversed
                    from receivable.manual_liabilitie_offsettings mlo
                             join customer.customers c on mlo.customer_id = c.id and c.status = 'ACTIVE'
                             join customer.customer_details cd on c.last_customer_detail_id = cd.id
                             left join nomenclature.legal_forms lf on cd.legal_form_id = lf.id
                    where (date(:liabilitieDateFrom) is null or mlo.manual_liabilitie_date >= date(:liabilitieDateFrom))
                      and (date(:liabilitieDateTo) is null or mlo.manual_liabilitie_date <= date(:liabilitieDateTo))
                      and ((:reversed) is null or text(mlo.reversed) in :reversed)
                      and (:prompt is null or (:searchBy = 'ALL' and (
                        text(mlo.id) like :prompt or
                        lower(c.identifier) like :prompt))
                        or ((:searchBy = 'NUMBER' and text(mlo.id) like :prompt)
                            or (:searchBy = 'CUSTOMER' and lower(c.identifier) like :prompt)))
                    """
    )
    Page<ManualLIabilityOffsettingListingMiddleResponse> filter(
            @Param("prompt") String prompt,
            @Param("liabilitieDateFrom") LocalDate liabilitieDateFrom,
            @Param("liabilitieDateTo") LocalDate liabilitieDateTo,
            @Param("reversed") List<String> reversed,
            @Param("searchBy") String searchBy,
            Pageable pageable
    );

    @Query(nativeQuery = true,
            value = """
                    select 
                    clpbd.customer_deposit_id as depositId,
                    receivable.convert_to_currency(clpbd.amount,clpbd.currency_id,cd.currency_id) as offsetAmountInDepositCurrency
                    from receivable.mlo_customer_results mcr
                             inner join receivable.customer_liabilitie_paid_by_deposits clpbd  on mcr.customer_liabilitie_paid_by_deposit_id =clpbd.id
                             inner join receivable.customer_deposits cd  on cd.id =clpbd.customer_deposit_id
                    where mcr.manual_liabilitie_offsetting_id = :mloId and clpbd.status ='ACTIVE'  and cd.status='ACTIVE';
                    """
    )
    List<MLOCustomerResultForLiability> getMloCustomerResultForLiability(Long mloId);

    @Query(value = "SELECT receivable.check_receivable_offestting_allowed(:receivableId, :offsettingDate)", nativeQuery = true)
    Boolean checkIfReceivableIsAllowedForOffsetting(@Param("receivableId") Long receivableId, @Param("offsettingDate") Date offsettingDate);

    @Query(value = "SELECT receivable.check_liability_offestting_allowed(:liabilityId, :offsettingDate)", nativeQuery = true)
    Boolean checkIfLiabilityIsAllowedForOffsetting(@Param("liabilityId") Long liabilityId, @Param("offsettingDate") Date offsettingDate);

    @Query(nativeQuery = true, value = """
                   with default_nomenclature as (
                       select c.print_name,c.abbreviation,c.full_name from nomenclature.currencies c
                       where c.is_default=true
                   )
                   select
                       case
                           when c.customer_type = 'PRIVATE_CUSTOMER'
                               then concat(cd.name, ' ', cd.middle_name, ' ', cd.last_name)
                           else concat(cd.name, ' ', lf.name) end        as CustomerNameComb,
                       case
                           when c.customer_type = 'PRIVATE_CUSTOMER'
                               then concat(cd.name_transl, ' ', cd.middle_name_transl, ' ',
                                           cd.last_name_transl)
                           else concat(cd.name_transl, ' ', lft.name) end as CustomerNameCombTrsl,
                       c.identifier as CustomerIdentifier,
                       c.customer_number as CustomerNumber,
                       case when cd.foreign_address is true then cd.populated_place_foreign else pp.name end as CustomerPopulatedPlace,
                       case when cd.foreign_address is true then cd.zip_code_foreign else zc.zip_code end as CustomerZip,
                       case when cd.foreign_address is true then cd.district_foreign else d.name end as CustomerDistrict,
                       translation.translate_text(
                               case when cd.foreign_address is true then text(cd.street_type) else text(s.type) end,text('BULGARIAN')
                       )as CustomerStrBlvdType,
                       case when cd.foreign_address is true then cd.street_foreign else s.name end as CustomerStrBlvdName,
                       case when cd.foreign_address is true then cd.block else cd.block end as CustomerBlock,
                       cd.entrance as CustomerEntrance,
                       cd.floor as CustomerFloor,
                       cd.apartment as CustomerApartment,
                       cd.address_additional_info as CustomerAdditionalInfo,
                       case when cd.foreign_address is true then cd.residential_area_foreign else ra.name end as CustomerQuarterRaName,
                       translation.translate_text(
                               case when cd.foreign_address is true then text(cd.residential_area_type) else text(ra.type) end,
                       text('BULGARIAN')
                       ) as CustomerQuarterRaType,
                       cd.street_number as CustomerStrBlvdNumber,
                       string_agg(distinct seg.name, ', ') as CustomerSegments,
                       translation.translate_text(
                               concat_ws(', ',
                                         nullif(concat_ws(', ',
                                                          case when cd.foreign_address is true then cd.district_foreign else d.name end,
                                                          case
                                                              when cd.foreign_address is true then
                                                                  case when cd.foreign_residential_area_type is not null
                                                                           then concat(cd.foreign_residential_area_type, ' ', cd.residential_area_foreign)
                                                                       else cd.residential_area_foreign
                                                                      end
                                                              else
                                                                  case when ra.type is not null
                                                                           then concat(ra.type, ' ', ra.name)
                                                                       else ra.name
                                                                      end
                                                              end
                                                ), ''),
                                         nullif(concat_ws(' ',
                                                          case when cd.foreign_address is true then cd.street_type else s.type end,
                                                          case when cd.foreign_address is true then cd.street_foreign else s.name end,
                                                          cd.street_number
                                                ), ''),
                                         nullif(concat('бл. ', cd.block), 'бл. '),
                                         nullif(concat('вх. ', cd.entrance), 'вх. '),
                                         nullif(concat('ет. ', cd.floor), 'ет. '),
                                         nullif(concat('ап. ', cd.apartment), 'ап. '),
                                         nullif(cd.address_additional_info, '')
                               ),text('BULGARIAN')
                       )as CustomerAddressComb,
                       mlo.manual_liabilitie_date as OffsettingDate,
                       sum(mlocl.before_current_amount) as LiabilitiesAmountBefore,
                       sum(mlocl.after_current_amount) as LiabilitiesAmountAfter,
                       sum(mlocr.before_current_amount) as ReceivablesAmountBefore,
                       sum(mlocr.after_current_amount) as ReceivablesAmountAfter,
                       default_nomenclature.print_name as CurrencyPrintName,
                       default_nomenclature.abbreviation as CurrencyAbr,
                       default_nomenclature.full_name as CurrencyFullName
                   from receivable.manual_liabilitie_offsettings mlo
                            join customer.customer_details cd on mlo.customer_detail_id=cd.id
                            join customer.customers c on cd.customer_id=c.id
                            left join nomenclature.legal_forms lf on cd.legal_form_id = lf.id
                            left join nomenclature.legal_forms_transl lft on cd.legal_form_transl_id=lft.id
                            left join nomenclature.populated_places pp on cd.populated_place_id=pp.id
                            left join nomenclature.zip_codes zc on cd.zip_code_id = zc.id
                            left join nomenclature.districts d on cd.district_id = d.id
                            left join nomenclature.streets s on cd.street_id = s.id
                            left join nomenclature.residential_areas ra on cd.residential_area_id=ra.id
                            left join customer.customer_segments cs on cs.customer_detail_id=cd.id
                            left join nomenclature.segments seg on cs.segment_id= seg.id
                            left join receivable.mlo_customer_liabilities mlocl on mlocl.manual_liabilitie_offsetting_id=mlo.id
                            left join receivable.mlo_customer_receivables mlocr on mlocr.manual_liabilitie_offsetting_id = mlo.id
                            left join default_nomenclature on true
                   where mlo.id=:mloId
                   group by
                       c.customer_type,
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
                       cd.residential_area_type, mlo.manual_liabilitie_date, default_nomenclature.print_name, default_nomenclature.abbreviation, default_nomenclature.full_name,
                       lft.name
            """)
    Optional<MloCustomerMiddleResponse> getMloInfo(Long mloId);

    @Query("""
                     select cl,mlocl from
                     ManualLiabilityOffsetting mlo
                     join MLOCustomerLiabilities mlocl on mlocl.manualLiabilityOffsettingId=mlo.id
                     join CustomerLiability cl on mlocl.customerLiabilitiesId=cl.id
                     where mlo.id=:mloId
            """)
    List<Object[]> findLiabilities(Long mloId);

    @Query("""
                    select cr,mlocr from
                    ManualLiabilityOffsetting mlo
                    join MLOCustomerReceivables mlocr on mlocr.manualLiabilityOffsettingId=:mloId
                    join CustomerReceivable cr on mlocr.customerReceivablesId=cr.id
                    where mlo.id=:mloId
            """)
    List<Object[]> findReceivables(Long mloId);

    @Query("""
                    select distinct mlo.id from ManualLiabilityOffsetting mlo
                    join MLOCustomerLiabilities mlocl on mlocl.manualLiabilityOffsettingId=mlo.id
                    where mlo.reversed=false and mlocl.customerLiabilitiesId in (:liabilityIds)
            """)
    List<Long> findMlosConnectedToLiabilities(List<Long> liabilityIds);

    List<ManualLiabilityOffsetting> findManualLiabilityOffsettingsByIdAndReversedIsFalse(Long customerReceivablesId);

}
