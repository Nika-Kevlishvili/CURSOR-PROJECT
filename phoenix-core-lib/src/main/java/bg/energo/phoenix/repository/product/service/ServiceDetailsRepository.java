package bg.energo.phoenix.repository.product.service;

import bg.energo.phoenix.model.entity.product.service.EPService;
import bg.energo.phoenix.model.entity.product.service.ServiceDetails;
import bg.energo.phoenix.model.enums.product.service.ServiceDetailStatus;
import bg.energo.phoenix.model.response.contract.order.service.ServiceOrderServiceVersionResponse;
import bg.energo.phoenix.model.response.copy.group.CopyDomainWithVersionMiddleResponse;
import bg.energo.phoenix.model.response.product.CostCenterAndIncomeAccountResponse;
import bg.energo.phoenix.model.response.service.ContractServiceFilterResponse;
import bg.energo.phoenix.model.response.service.ServiceVersion;
import bg.energo.phoenix.model.response.service.ServiceVersionShortResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ServiceDetailsRepository extends JpaRepository<ServiceDetails, Long> {

    @Query(
            value = """
                        select count(sd.id) > 0 from ServiceDetails sd
                        join EPService s on sd.service.id = s.id
                            where sd.name = :name
                            and s.status = 'ACTIVE'
                            and (:serviceId is null or s.id <> :serviceId)
                    """
    )
    boolean existsByName(
            @Param("name") String name,
            @Param("serviceId") Long serviceId
    );


    @Query("""
            select new bg.energo.phoenix.model.response.service.ServiceVersion(sd.version,sd.id,sd.status,sd.createDate)
            from ServiceDetails sd
            where sd.service.id =:serviceId
            and sd.status in :statuses
            order by sd.version
              """)
    List<ServiceVersion> findAllServiceDetailsByServiceIdAndStatusIn(@Param("serviceId") Long serviceId,
                                                                     @Param("statuses") List<ServiceDetailStatus> serviceDetailStatuses);

    Optional<ServiceDetails> findByServiceIdAndVersionAndStatusIn(Long serviceId, Long version, List<ServiceDetailStatus> statuses);

    Optional<ServiceDetails> findFirstByServiceIdAndStatusInOrderByVersionDesc(Long serviceId, List<ServiceDetailStatus> statuses);

    @Query("""
            select sd from ServiceDetails sd
            where sd.service.id = :serviceId
            and sd.status in (:statuses)
            and sd.version = (select max(d.version) from ServiceDetails d where d.service.id = :serviceId and d.status in :statuses)
            """)
    Optional<ServiceDetails> findLastDetailByServiceId(
            @Param("serviceId") Long serviceId,
            @Param("statuses") List<ServiceDetailStatus> statuses,
            Sort sort
    );

    Optional<ServiceDetails> findByServiceIdAndVersion(Long serviceId, Long version);


    Optional<ServiceDetails> findByServiceAndVersion(EPService service, Long version);


    @Query(value = """
            select max(sd.version) from ServiceDetails sd
            where sd.service.id = :serviceId
            """
    )
    Long findLastDetailVersion(@Param("serviceId") Long serviceId);

    @Query("""
            select new bg.energo.phoenix.model.response.copy.group.CopyDomainWithVersionMiddleResponse(sd.id,sd.version,sd.createDate)
            from ServiceDetails sd
            where sd.service.id =:id
            order by sd.version ASC
            """)
    List<CopyDomainWithVersionMiddleResponse> findByCopyGroupBaseRequest(@Param("id") Long id);

    @Query(value = """
            select distinct new bg.energo.phoenix.model.response.service.ContractServiceFilterResponse(
                sd.id,
                sd.service.id,
                sd.name,
                sd.version
            )
            from ServiceDetails sd
            join EPService s on sd.service.id = s.id
            left join ServiceSegment sg on sg.serviceDetails.id = sd.id
                where text(sd.saleMethods) = '{CONTRACT}'
                  and sd.status = 'ACTIVE'
                  and s.status = 'ACTIVE'
                  and (sd.availableForSale = true or CURRENT_DATE BETWEEN sd.availableFrom AND sd.availableTo)
                  and (coalesce(:service_name, '0') = '0' or  sd.name = :service_name)
                  and (coalesce(:segment_ids, '0') = '0' or sg.segment.id in :segment_ids)
            """)
    Page<ContractServiceFilterResponse> findContractSales(
            @Param("service_name") String serviceName,
            @Param("segment_ids") List<Long> segmentIds,
            PageRequest page
    );


    @Query(
            value = """
                    select count(sd.id) > 0 from ServiceDetails sd
                    left join ServiceContractDetails scd on scd.serviceDetailId = sd.id
                    join ServiceContracts sc on sc.id = scd.contractId
                        where sd.id = :id
                        and sc.status = 'ACTIVE'
                    """
    )
    boolean hasActiveConnectionToContract(Long id);


    @Query(
            value = """
                    select count(sd.id) > 0 from ServiceDetails sd
                    left join ServiceContractDetails scd on scd.serviceDetailId = sd.id
                    join ServiceContracts sc on sc.id = scd.contractId
                        where sd.service.id = :serviceId
                        and sc.status = 'ACTIVE'
                        and sd.service.status = 'ACTIVE'
                    """
    )
    boolean hasServiceActiveConnectionToContract(Long serviceId);


    @Query(
            value = """
                    select sd
                    from ServiceDetails sd
                             join EPService s on s.id = sd.service.id
                    where sd.status = 'ACTIVE'
                      and s.status = 'ACTIVE'
                      and arrays_intersect(sd.saleMethods, '{ORDER}') = true
                      
                      and (
                        (
                            :customerDetailId is null
                                and (
                                (
                                    s.customerIdentifier is null
                                        and sd.availableForSale = true
                                        and current_timestamp between
                                        coalesce(sd.availableFrom, current_timestamp)
                                        and coalesce(sd.availableTo, current_timestamp)
                                    )
                                    or (s.customerIdentifier is not null)
                                )
                            )
                            or (
                            :customerDetailId is not null
                                and (
                                (
                                    s.customerIdentifier is null
                                        and sd.availableForSale = true
                                        and current_timestamp between
                                        coalesce(sd.availableFrom, current_timestamp)
                                        and coalesce(sd.availableTo, current_timestamp)
                                        and (
                                        sd.globalSegment = true
                                            or exists (select 1
                                                       from CustomerDetails cd
                                                                join Customer c on c.id = cd.customerId
                                                       where cd.id = :customerDetailId
                                                         and c.status = 'ACTIVE'
                                                         and exists(select 1
                                                                    from CustomerSegment cs
                                                                    where cs.customerDetail.id = cd.id
                                                                      and cs.status = 'ACTIVE'
                                                                      and exists(select 1
                                                                                 from ServiceSegment ss
                                                                                 where ss.serviceDetails.id = sd.id
                                                                                   and ss.segment.id = cs.segment.id
                                                                                   and ss.status = 'ACTIVE')))
                                        )
                                    )
                                    or (
                                    s.customerIdentifier is not null
                                        and s.customerIdentifier in (select c.identifier
                                                                     from Customer c
                                                                              join CustomerDetails cd on cd.customerId = c.id
                                                                         and cd.id = :customerDetailId
                                                                         and c.status = 'ACTIVE')
                                    )
                                )
                            )
                        )
                        and sd.id=:serviceDetailId
                    order by s.customerIdentifier, sd.name
                    """
    )
    Optional<ServiceDetails> getAvailableVersionForServiceOrdersAndIdIn(
            @Param("customerDetailId") Long customerDetailId,
            @Param("serviceDetailId") Long serviceDetailId
    );


    @Query(
            value = """
                    select new bg.energo.phoenix.model.response.contract.order.service.ServiceOrderServiceVersionResponse(
                        sd.id,
                            case
                                when s.customerIdentifier is not null then sd.name
                                else concat(sd.name, ' (Version ', sd.version,')') end,
                        sd.service.id,
                        sd.version
                    )
                    from ServiceDetails sd
                             join EPService s on s.id = sd.service.id
                    where sd.status = 'ACTIVE'
                      and s.status = 'ACTIVE'
                      and arrays_intersect(sd.saleMethods, '{ORDER}') = true
                      and (:prompt is null or lower(sd.name) like :prompt or text(s.id) like :prompt)
                      and (
                        (
                            :customerDetailId is null
                                and (
                                (
                                    s.customerIdentifier is null
                                        and sd.availableForSale = true
                                        and current_timestamp between
                                        coalesce(sd.availableFrom, current_timestamp)
                                        and coalesce(sd.availableTo, current_timestamp)
                                    )
                                    
                                )
                            )
                            or (
                            :customerDetailId is not null
                                and (
                                (
                                    s.customerIdentifier is null
                                        and sd.availableForSale = true
                                        and current_timestamp between
                                        coalesce(sd.availableFrom, current_timestamp)
                                        and coalesce(sd.availableTo, current_timestamp)
                                        and (
                                        sd.globalSegment = true
                                            or exists (select 1
                                                       from CustomerDetails cd
                                                                join Customer c on c.id = cd.customerId
                                                       where cd.id = :customerDetailId
                                                         and c.status = 'ACTIVE'
                                                         and exists(select 1
                                                                    from CustomerSegment cs
                                                                    where cs.customerDetail.id = cd.id
                                                                      and cs.status = 'ACTIVE'
                                                                      and exists(select 1
                                                                                 from ServiceSegment ss
                                                                                 where ss.serviceDetails.id = sd.id
                                                                                   and ss.segment.id = cs.segment.id
                                                                                   and ss.status = 'ACTIVE')))
                                        )
                                    )
                                    or (
                                    s.customerIdentifier is not null
                                                  and not exists(
                                                    select 1 from ServiceOrder o
                                                    where o.serviceDetailId = sd.id
                                                    and o.status = 'ACTIVE')
                                        and s.customerIdentifier in (select c.identifier
                                                                     from Customer c
                                                                              join CustomerDetails cd on cd.customerId = c.id
                                                                         and cd.id = :customerDetailId
                                                                         and c.status = 'ACTIVE')
                                    )
                                )
                            )
                        )
                        and
                        (:serviceDetailId is null or sd.id = :serviceDetailId)
                    order by s.customerIdentifier, sd.name
                                        """
    )
    Page<ServiceOrderServiceVersionResponse> getAvailableVersionsForServiceOrders(
            @Param("customerDetailId") Long customerDetailId,
            @Param("prompt") String prompt,
            @Param("serviceDetailId") Long serviceDetailId,
            Pageable pageable
    );

    @Query(
            value = """
                    select sd
                    from ServiceDetails sd
                    join EPService s on s.id = sd.service.id
                        where sd.status = 'ACTIVE'
                        and s.status = 'ACTIVE'
                        and arrays_intersect(sd.saleMethods, '{CONTRACT}') = true
                        and (:prompt is null or lower(sd.name) like :prompt)
                        and (
                            (
                                :customerDetailId is null
                                and (
                                        (
                                            s.customerIdentifier is null
                                            and sd.availableForSale = true
                                            and current_date between coalesce(sd.availableFrom, current_date)
                                            and coalesce(sd.availableTo, current_date)
                                        )
                                        or (s.customerIdentifier is not null)
                                )
                            )
                            or (
                                :customerDetailId is not null
                                and (
                                    (
                                        s.customerIdentifier is null
                                        and sd.availableForSale = true
                                        and current_date between coalesce(sd.availableFrom, current_date)
                                        and coalesce(sd.availableTo, current_date)
                                        and exists(
                                            select 1 from CustomerDetails cd
                                            join Customer c on c.id = cd.customerId
                                                where cd.id = :customerDetailId
                                                and c.status = 'ACTIVE'
                                                and exists(
                                                    select 1 from CustomerSegment cs
                                                    where cs.customerDetail.id = cd.id
                                                    and cs.status = 'ACTIVE'
                                                    and sd.globalSegment = true
                                                    or exists(
                                                        select 1 from ServiceSegment ss
                                                        where ss.serviceDetails.id = sd.id
                                                        and ss.segment.id = cs.segment.id
                                                        and ss.status = 'ACTIVE'
                                                    )
                                                )
                                        )
                                    )
                                    or (
                                        s.customerIdentifier is not null
                                          and not exists(
                                            select 1 from ServiceOrder o
                                            where o.serviceDetailId = sd.id
                                            and o.status = 'ACTIVE'
                                        )
                                        and s.customerIdentifier in (
                                            select c.identifier from Customer c
                                            join CustomerDetails cd on cd.customerId = c.id
                                            and cd.id = :customerDetailId
                                            and c.status = 'ACTIVE'
                                        )
                                    )
                                )
                            )
                        )
                        order by case when :customerDetailId is not null then s.customerIdentifier else sd.name end
                    """
    )
    Page<ServiceDetails> getAvailableVersionsForServiceContract(
            @Param("customerDetailId") Long customerDetailId,
            @Param("prompt") String prompt,
            Pageable pageable
    );

    @Query(
            value = """
                    select new bg.energo.phoenix.model.response.service.ServiceVersionShortResponse(
                        sd.id,
                        sd.name,
                        sd.version,
                        sd.service.customerIdentifier
                    )
                    from ServiceDetails sd
                    where exists (
                        select 1 from ServiceOrder so
                        where so.serviceDetailId = sd.id
                    )
                    and (:prompt is null or lower(sd.name) like :prompt)
                    order by sd.createDate desc
                    """
    )
    Page<ServiceVersionShortResponse> getServiceVersionsForServiceOrdersListing(
            @Param("prompt") String prompt,
            Pageable pageable
    );

    @Query(
            value = """
                    select new bg.energo.phoenix.model.response.service.ServiceVersionShortResponse(
                        sd.id,
                        sd.name,
                        sd.version,
                        sd.service.customerIdentifier
                    )
                    from ServiceDetails sd
                    where exists (
                        select 1 from ServiceContractDetails scd
                        where scd.serviceDetailId = sd.id
                    )
                    and (:prompt is null or lower(sd.name) like :prompt)
                    order by sd.createDate desc
                    """
    )
    Page<ServiceVersionShortResponse> getServiceVersionsForServiceContractsListing(
            @Param("prompt") String prompt,
            Pageable pageable
    );

    Optional<ServiceDetails> findByIdAndStatus(Long id, ServiceDetailStatus status);

    @Query(value = """
            select count(1) from
             service.services s
             join service.service_details sd
             on sd.service_id = s.id
              and sd.id = :serviceDetailId
              where
              s.status = 'ACTIVE'
              and sd.status = 'ACTIVE'
              and sd.available_For_Sale = true
              and current_date between coalesce(date(sd.available_From), current_date) and coalesce(date(sd.available_To), current_date)
              and s.customer_identifier is null
              and (:prompt is null or lower(sd.name) like :prompt)
              and 1 = (select count(1) from service.service_contract_terms sct where sct.service_details_id = sd.id and sct.status =  'ACTIVE')
              and 1 = (select count(1) from service.service_contract_terms sct where sct.service_details_id = sd.id and sct.status =  'ACTIVE' and sct.contract_term_period_type <> 'CERTAIN_DATE')
              and (sd.term_id is null or
              (1 = (select
              count(1)
               from terms.terms t
               join terms.invoice_payment_terms ipt on ipt.term_id = t.id
               where t.id = sd.term_id
                 and t.status = 'ACTIVE'
                 and ipt.status = 'ACTIVE') and 1 = (select count(1)
               from terms.terms t
               join terms.invoice_payment_terms ipt on ipt.term_id = t.id
               where t.id = sd.term_id
                 and ipt.value is not null
                 and t.status = 'ACTIVE'
                 and ipt.status = 'ACTIVE')
               and
               (exists
                  (select 1 from terms.terms trm
                    where trm.id = sd.term_id
                      and trm.status = 'ACTIVE'
                      and array_length(trm.contract_entry_into_force,1) = 1
                      and trm.contract_entry_into_force not in ('{EXACT_DAY}','{MANUAL}')
                      and array_length(trm.start_initial_term_of_contract,1) = 1
                      and trm.start_initial_term_of_contract not in ('{EXACT_DATE}','{MANUAL}')
                      and array_length(trm.supply_activation,1) = 1
                      and trm.supply_activation <> '{EXACT_DATE}'
                  )
                 )
                )
               )
               and array_length(sd.payment_guarantee, 1) = 1
               and (case when sd.payment_guarantee  = '{CASH_DEPOSIT}' then
               sd.cash_deposit_amount is not null and sd.cash_deposit_currency_id is not null else 1=1 end)
               and (case when sd.payment_guarantee  = '{BANK}' then
               sd.bank_guarantee_amount  is not null and sd.bank_guarantee_currency_id  is not null else 1=1 end)
               and (case when sd.payment_guarantee  = '{CASH_DEPOSIT_AND_BANK}' then
               sd.bank_guarantee_amount  is not null and sd.bank_guarantee_currency_id  is not null and
               sd.cash_deposit_amount is not null and
               sd.cash_deposit_currency_id is not null else 1=1 end)
               and(not exists (select 1 from service.service_interim_advance_payments siap where siap.service_detail_id = sd.id and siap.status = 'ACTIVE')
                  or
               1 = (select count(1) from service.service_interim_advance_payments siap
               join interim_advance_payment.interim_advance_payments iap on siap.interim_advance_payment_id = iap.id
                         where siap.service_detail_id = sd.id
                          and
                          iap.payment_type = 'OBLIGATORY'
                          and
                         (iap.match_term_of_standard_invoice = true
                           or
                           (
                           1= (select count(1) from interim_advance_payment.interim_advance_payment_terms iapt where iapt.interim_advance_payment_id = iap.id and iapt.status = 'ACTIVE')
                            and
                           1= (select count(1) from interim_advance_payment.interim_advance_payment_terms iapt where iapt.interim_advance_payment_id = iap.id and iapt.status = 'ACTIVE' and iapt.value is not null)
                           )
                         )
                         and
                         (iap.value_type = 'PRICE_COMPONENT' or ( (iap.value_type = 'PERCENT_FROM_PREVIOUS_INVOICE_AMOUNT' or iap.value_type = 'EXACT_AMOUNT') and iap.value is not null ))
                         and
                         (case
                                  when iap.date_of_issue_type in ('DATE_OF_THE_MONTH', 'WORKING_DAYS_AFTER_INVOICE_DATE')
                                   then iap.date_of_issue_value is not null
                                   else 1 = 1 end)
                         and iap.status = 'ACTIVE'
                         and siap.status  = 'ACTIVE')
                    )
               and
               (sd.equal_monthly_installments_activation = 'false' or (sd.installment_number is not null and sd.amount is not null))
               and
               (:customerDetailId is null or sd.global_segment = 'true'
                or
                exists
                (select 1
                from service.service_segments ss
                where ss.service_detail_id = sd.id
                and sd.status = 'ACTIVE'
                and exists (select 1 from customer.customer_segments cs
                where cs.customer_detail_id = :customerDetailId
                and cs.segment_id = ss.segment_id
                and cs.status = 'ACTIVE')))
            """, nativeQuery = true)
    Integer canCreateExpressContractForServiceDetails(@Param("serviceDetailId") Long serviceDetailId, @Param("customerDetailId") Long customerDetailId, @Param("prompt") String prompt);


    @Query("""
            SELECT  distinct CONCAT(
                                     CASE WHEN scd.id IS NOT NULL THEN ', SERVICE_CONTRACT' ELSE '' END,
                                     CASE WHEN so.id IS NOT NULL THEN ', SERVICE_ORDER' ELSE '' END
                                 ) AS contract_types
                             FROM
                                 ServiceDetails sd
                             LEFT JOIN ServiceContractDetails scd ON scd.serviceDetailId = sd.id
                             left join ServiceContracts        sc  on scd.contractId = sc.id
                              and sc.status = 'ACTIVE'
                             LEFT JOIN ServiceOrder so ON so.serviceDetailId = sd.id
                              and so.status =  'ACTIVE'
                             WHERE sd.id = :id
            """)
    List<String> checkForBoundObjects(Long id);

    @Query("""
            select distinct sd
            from ServiceDetails sd
            join ServiceContractDetails scd on scd.serviceDetailId = sd.id
            join ServiceContracts sc on sc.id = scd.contractId
            where sc.id = :contractId
            """)
    List<ServiceDetails> findForBillingRunOvertime(@Param("contractId") Long contractId);

    @Query("""
            select new bg.energo.phoenix.model.response.product.CostCenterAndIncomeAccountResponse(
            sd.id,
            sd.costCenterControllingOrder,
            sd.incomeAccountNumber
            )
            from ServiceDetails sd
            where sd.id in (:ids)
            """)
    List<CostCenterAndIncomeAccountResponse> getCostCenterAndIncomeAccountByDetailId(@Param("ids") List<Long> ids);
}
