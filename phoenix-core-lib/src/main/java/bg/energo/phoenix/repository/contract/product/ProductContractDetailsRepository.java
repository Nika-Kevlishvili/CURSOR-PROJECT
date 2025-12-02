package bg.energo.phoenix.repository.contract.product;

import bg.energo.phoenix.model.entity.contract.product.ProductContract;
import bg.energo.phoenix.model.entity.contract.product.ProductContractDetails;
import bg.energo.phoenix.model.entity.contract.product.ProductContractResigningWithCustomerAndPointOfDeliveryIntersectionMiddleResponse;
import bg.energo.phoenix.model.enums.contract.express.ProductContractVersionStatus;
import bg.energo.phoenix.model.enums.contract.products.ProductContractStatus;
import bg.energo.phoenix.model.request.contract.product.CurrentProductContractDetails;
import bg.energo.phoenix.model.response.contract.action.calculation.PenaltyCalculationProductContractVariables;
import bg.energo.phoenix.model.response.contract.productContract.AdditionalParametersResponse;
import bg.energo.phoenix.model.response.contract.productContract.ProductContractVersionResponse;
import bg.energo.phoenix.model.response.contract.productContract.ProductContractVersionShortDto;
import bg.energo.phoenix.model.response.contract.productContract.ProductContractVersionWithStatusResponse;
import bg.energo.phoenix.model.response.customer.CustomerCommunicationDataResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

@Repository
public interface ProductContractDetailsRepository extends JpaRepository<ProductContractDetails, Long> {

    @Query(
            value = """
                    select new bg.energo.phoenix.model.response.contract.productContract.AdditionalParametersResponse(
                        pcd.dealNumber,
                        pcd.estimatedTotalConsumptionUnderContractKwh,
                        pcd.directDebit,
                        am.id,
                        concat(am.displayName, ' (', am.userName, ')'),
                        b.id,
                        b.name,
                        b.bic,
                        pcd.iban,
                        pcd.riskAssessment,
                        pcd.riskAssessmentAdditionalCondition,
                        ir.id,
                        ir.name,
                        c.id,
                        c.name
                    )
                    from ProductContractDetails pcd
                    left join AccountManager am on pcd.employeeId = am.id
                    left join Bank b on pcd.bankId = b.id
                    join InterestRate ir on pcd.applicableInterestRate = ir.id
                    left join Campaign c on pcd.campaignId = c.id
                        where pcd.id = :productContractId
                    """
    )
    AdditionalParametersResponse getAdditionalParametersByProductContractDetailId(@Param("productContractId") Long productContractId);


    Optional<ProductContractDetails> findByContractIdAndVersionId(Long contractId, Integer versionId);

    List<ProductContractDetails> findProductContractDetailsByContractId(Long contractId);

    List<ProductContractDetails> findProductContractDetailsByContractIdAndVersionStatus(Long contractId, ProductContractVersionStatus versionStatus);

    @Query("""
            select pcd
            from ProductContractDetails pcd
            where pcd.startDate < :startDate
            and pcd.contractId = :contractId
            and pcd.versionStatus = 'SIGNED'
            order by pcd.startDate desc
            """)
    Optional<ProductContractDetails> findPreviousProductContractDetailsDependingOnStartDate(Long contractId,
                                                                                            LocalDate startDate,
                                                                                            PageRequest pageRequest);

    @Query("""
            select pcd
            from ProductContractDetails pcd
            where pcd.startDate > :startDate
            and pcd.contractId = :contractId
            and pcd.versionStatus = 'SIGNED'            
            order by pcd.startDate
            """)
    Optional<ProductContractDetails> findNextProductContractDetailsDependingOnStartDate(Long contractId,
                                                                                        LocalDate startDate,
                                                                                        PageRequest pageRequest);

    Optional<ProductContractDetails> findFirstByContractIdOrderByStartDateDesc(Long contractId);

    Optional<ProductContractDetails> findFirstByDealNumberOrderByStartDateDesc(String dealNumber);

    @Query("""
            select new bg.energo.phoenix.model.response.contract.productContract.ProductContractVersionResponse(
                pcd.id,
                pcd.contractId,
                pcd.versionId,
                pcd.startDate,
                pcd.endDate
            )
            from ProductContractDetails pcd
            where pcd.contractId = :contractId
            and pcd.versionStatus = 'SIGNED'
            order by pcd.startDate
            """)
    List<ProductContractVersionResponse> findProductContractVersionsOrdered(@Param("contractId") Long contractId);

    //TODO HOLA
    @Query("""
            select new bg.energo.phoenix.model.response.contract.productContract.ProductContractVersionResponse(
                pcd.id,
                pcd.contractId,
                pcd.versionId,
                pcd.startDate,
                pcd.endDate
            )
            from ProductContractDetails pcd 
            where pcd.contractId = :contractId
            and pcd.startDate >= :startDate
            order by pcd.startDate
            """)
    List<ProductContractVersionResponse> findProductContractVersionsOrderedByStartDate(@Param("contractId") Long contractId, @Param("startDate") LocalDate startDate);

    List<ProductContractDetails> findByContractId(Long id);

    @Query(value = """
            select c.id
            from product_contract.contracts c
            where c.contract_number = :contract_number
            and c.status = 'ACTIVE'
            and c.contract_status in ('READY', 'SIGNED', 'ENTERED_INTO_FORCE', 'ACTIVE_IN_TERM','ACTIVE_IN_PERPETUITY')
            """, nativeQuery = true)
    Long findByContractIdAndLatestDetail(@Param("contract_number") String contractNumber);

    @Query(value = """
            select c.id
            from product_contract.contracts c
            where c.contract_number = :contract_number
            and c.status = 'ACTIVE'
            """, nativeQuery = true)
    Long findByContractIdAndLatestDetailWithoutStatuses(@Param("contract_number") String contractNumber);

    @Query("""
            select max(pd.versionId) from ProductContractDetails  pd
            where pd.contractId = :contractId
            """)
    Integer findMaxVersionId(@Param("contractId") Long contractId);

    @Query("""
            select  cd1.id
            from
             ProductContractDetails cd1
            where cd1.contractId in (
            select cd.contractId
             from ProductContractDetails cd
             where cd.id = :contractDetailId)
            and
            cd1.startDate > (
            select cd.startDate
             from ProductContractDetails cd
             where cd.id = :contractDetailId)
            and not exists (select 1
                             from ContractPods cp
                                   join
                                  PointOfDeliveryDetails pd
                                  on cp.podDetailId = pd.id
                            where cp.contractDetailId = cd1.id
                              and pd.podId in(select pd.podId
                                                from PointOfDeliveryDetails pd
                                                where pd.id = :podDetailId)
                              and cp.status = 'ACTIVE')
                                """)
    List<Long> getContractDetailIdsToAddPod(Long contractDetailId, Long podDetailId);

    @Query("""
            select pcd from ProductContractDetails pcd
            where pcd.contractId = :productContractId
            and pcd.versionId = :productContractVersionId
            """)
    Optional<ProductContractDetails> findByProductContractIdAndVersionId(Long productContractId, Integer productContractVersionId);

    @Query("""
            select pcd from ProductContractDetails pcd
            where pcd.contractId = :productContractId
            and pcd.startDate > :sourceDate
            order by pcd.startDate
            """)
    Optional<ProductContractDetails> findProductContractNextVersion(Long productContractId, LocalDate sourceDate, PageRequest pageRequest);

    @Query("""
            select pcd from ProductContractDetails pcd
            where pcd.contractId = :productContractId
            and pcd.startDate < :sourceDate
            order by pcd.startDate desc
            """)
    Optional<ProductContractDetails> findProductContractPreviousVersion(Long productContractId, LocalDate sourceDate, PageRequest pageRequest);

    @Query(
            nativeQuery = true,
            value = """
                    select id from(
                    select cd.id,
                           cd.start_date,
                           coalesce(lead(cd.start_date, 1) OVER (order by cd.start_date), date '9999-12-31')-1 as next_date
                    from product_contract.contracts c
                    join
                    product_contract.contract_details cd
                    on cd.contract_id = c.id and c.id =:contractId
                    and c.status = 'ACTIVE'
                    and cd.status = 'SIGNED') as tbl
                    where :executionDate between tbl.start_date and tbl.next_date;
                    """
    )
    Long getContractDetailIdByExecutionDate(
            @Param("contractId") Long contractId,
            @Param("executionDate") LocalDate executionDate
    );


    @Query(
            value = """
                    select count (pcd.id) > 0
                        from ProductContractDetails pcd
                        join CustomerDetails cd on cd.id = pcd.customerDetailId
                        join Customer c on c.id = cd.customerId
                            where c.status = 'ACTIVE'
                            and c.id = :customerId
                            and pcd.id = :contractDetailId
                    """
    )
    boolean isCustomerAttachedToContractDetail(
            @Param("customerId") Long customerId,
            @Param("contractDetailId") Long contractDetailId
    );

    @Query("""
            select pcd from ProductContractDetails pcd
            where pcd.startDate = (
                select max(innerPCD.startDate) from ProductContractDetails innerPCD
                where innerPCD.contractId = :productContractId
            )
            and pcd.contractId = :productContractId
            """)
    Optional<ProductContractDetails> findLatestProductContractDetails(Long productContractId);

    @Query("""
            select max(cd.agreementSuffix) from ProductContractDetails cd
            where cd.contractId = :contractId
            """)
    Optional<Integer> findContractAgreementSuffixValue(Long contractId);

    @Query(value = """
            select new bg.energo.phoenix.model.response.contract.productContract.ProductContractVersionShortDto(pcd.contractId,pcd.id,pcd.versionId,pcd.startDate)
            from ProductContractDetails pcd 
            where pcd.contractId = :contractId
            and pcd.startDate < :startDate
            and pcd.versionStatus = 'SIGNED'
            order by pcd.startDate desc 
            """)
    Page<ProductContractVersionShortDto> findVersionBefore(Long contractId, LocalDate startDate, Pageable pageable);


    @Query(
            nativeQuery = true,
            value = """
                    select distinct tbl.contract_id                                        as contractId,
                                    tbl.contract_detail_id                                 as contractDetailId,
                                    tbl.estimated_total_consumption_under_contract_kwh     as estimatedTotalConsumptionUnderContract,
                                    tbl.avg_hourly_load_profiles                           as avgHourlyLoadProfiles,
                                    tbl.initial_term_start_date                            as contractInitialTermStartDate,
                                    tbl.contract_term_end_date                             as contractTermEndDate,
                                    tbl.real_termination_date                              as realTerminationDate,
                                    (select sum(pd.estimated_monthly_avg_consumption)
                                     from product_contract.contract_pods cp
                                              join pod.pod_details pd on pd.id = cp.pod_detail_id
                                              join pod.pod p on pd.pod_id = p.id and p.status = 'ACTIVE'
                                     where (
                                         (:actionPods) is null or
                                         p.id in (:actionPods))
                                       and cp.status = 'ACTIVE'
                                       and cp.contract_detail_id = tbl.contract_detail_id) as summedEstimatedMonthlyAvgConsumptionForPods,
                                    tbl.total_actual_consumption                           as ActualTotalConsumptionUnderContract,
                                    tbl.total_actual_consumption_for_pod                   as ActualTotalConsumptionUnderContractForPod,
                                    tbl.avg_month_act_cons                                 as AvgMonthlyActualConsumptionUnderContract,
                                    tbl.avg_month_act_cons_pod                             as AvgMonthlyActualConsumptionUnderContractForPod,
                                    tbl.avg_day_act_cons                                   as AvgDailyActualConsumptionUnderContract,
                                    tbl.avg_day_act_cons_pod                               as AvgDailyActualConsumptionUnderContractForPod,
                                    tbl.price_last_inv                                     as PriceFromLastInvoiceForActiveEnergy,
                                    tbl.actual_pav                                         as ActualPav
                    from (select c.id                                                               as contract_id,
                                 cd.id                                                              as contract_detail_id,
                                 cd.estimated_total_consumption_under_contract_kwh,
                                 cd.avg_hourly_load_profiles,
                                 c.initial_term_start_date,
                                 c.contract_term_end_date,
                                 case
                                     when :terminationId is null then :executionDate
                                     else (select case
                                                      when t.auto_termination_from = 'EVENT_DATE' then :executionDate
                                                      when t.auto_termination_from = 'FIRST_DAY_OF_MONTH_FOLLOWING_EVENT_DATE'
                                                          then date(date_trunc('month', current_date + interval '1 month'))
                                                      end
                                           from product.terminations t
                                           where t.id = :terminationId) end                            real_termination_date,
                                 coalesce(eval_cont.totActCons, 0)                                  as total_actual_consumption,
                                 coalesce(eval_cont_pod.totActConsPod, 0)                           as total_actual_consumption_for_pod,
                                 coalesce(eval_cont.totActCons / eval_cont.months_diff, 0)          as avg_month_act_cons,
                                 coalesce(eval_cont_pod.totActConsPod / eval_cont.months_diff, 0)   as avg_month_act_cons_pod,
                                 coalesce(eval_cont.totActCons / eval_cont.days_diff, 0)            as avg_day_act_cons,
                                 coalesce(eval_cont_pod.totActConsPod / eval_cont_pod.days_diff, 0) as avg_day_act_cons_pod,
                                 coalesce(eval_cont.price_last_inv, 0)                              as price_last_inv,
                                 coalesce(eval_actual_pav.actual_pav, 0)                            as actual_pav
                          from product_contract.contracts c
                                   join product_contract.contract_details cd on cd.contract_id = c.id
                              and c.id = :contractId and c.status = 'ACTIVE'
                              and (:executionDate >= cd.start_date and :executionDate < coalesce((select min(start_date)
                                                                                                  from product_contract.contract_details cd1
                                                                                                  where cd1.contract_id = cd.contract_id
                                                                                                    and cd1.start_date > cd.start_date),
                                                                                                 date(:executionDate) + 1))
                                   left join lateral (
                              select array_agg(p.id) as pod_id
                              from product_contract.contract_pods cp
                                       join pod.pod_details pd on cp.pod_detail_id = pd.id
                                       join pod.pod p on pd.pod_id = p.id
                              where cp.contract_detail_id = cd.id
                                  and
                                    :actionPods is null
                                 or p.id in (:actionPods)
                              ) pods on true

                                   left join lateral (
                              with invoice_summary as (select sum(i.total_actual_consumption)      as totActCons,
                                                              greatest(1,
                                                                       (date_part('year', max(i.meter_reading_period_to)) -
                                                                        date_part('year', min(i.meter_reading_period_from))) * 12 +
                                                                       (date_part('month', max(i.meter_reading_period_to)) -
                                                                        date_part('month', min(i.meter_reading_period_from))) + 1
                                                              )                                    as months_diff,
                                                              max(i.meter_reading_period_to) -
                                                              min(i.meter_reading_period_from) + 1 as days_diff
                                                       from product_contract.contract_details cont
                                                                join invoice.invoices i
                                                                     on cont.id = i.product_contract_detail_id
                                                                         and i.status = 'REAL'
                                                                         and i.type = 'STANDARD'
                                                                         and i.total_actual_consumption is not null
                                                       where cont.id = cd.id),
                                   last_invoice as (select i.total_actual_consumption_amount / i.total_actual_consumption as price_last
                                                    from product_contract.contract_details cont
                                                             join invoice.invoices i
                                                                  on cont.id = i.product_contract_detail_id
                                                                      and i.status = 'REAL'
                                                                      and i.type = 'STANDARD'
                                                                      and i.total_actual_consumption is not null
                                                    where cont.id = cd.id
                                                    order by i.invoice_date desc, i.id desc
                                                    limit 1)
                              select invoice_summary.totActCons,
                                     invoice_summary.months_diff,
                                     invoice_summary.days_diff,
                                     last_invoice.price_last as price_last_inv
                              from invoice_summary
                                       cross join last_invoice
                              ) eval_cont on true
                                   left join lateral (select sum(iptac.total_actual_consumption)  as totActConsPod,
                                                             greatest(1,
                                                                      (date_part('year', max(i.meter_reading_period_to)) -
                                                                       date_part('year', min(i.meter_reading_period_from))) * 12 +
                                                                      (date_part('month', max(i.meter_reading_period_to)) -
                                                                       date_part('month', min(i.meter_reading_period_from))) + 1
                                                             )                                    as months_diff,
                                                             max(i.meter_reading_period_to) -
                                                             min(i.meter_reading_period_from) + 1 as days_diff
                                                      from product_contract.contract_details cont
                                                               join invoice.invoices i
                                                                    on cont.id = i.product_contract_detail_id
                                                                        and i.status = 'REAL'
                                                                        and i.type = 'STANDARD'
                                                                        and i.total_actual_consumption is not null
                                                               join invoice.invoice_total_actual_consumption iptac
                                                                    on i.id = iptac.invoice_id and iptac.pod_id = any (pods.pod_id)
                                                      where cont.id = cd.id
                              ) eval_cont_pod on true
                                   left join lateral (
                              with pod_data as (select sum(case when pcc.type = '1' then pcc.total_volume else 0 end) as scaleSum,
                                                       sum(case when pcc.type = '2' then pcc.total_volume else 0 end) as profileSum,
                                                       array_agg(pcc.pod_id)                                          as podIds
                                                from product_contract.contract_pods cp
                                                         join pod.pod_details pd on cp.pod_detail_id = pd.id
                                                         join pod.pod pod on pd.pod_id = pod.id
                                                         join product_contract.contract_details cd
                                                              on cp.contract_detail_id = cd.id and cd.contract_id = c.id
                                                         join reporting.pod_consumption_cach pcc on pod.id = pcc.pod_id
                                                    and ((cp.activation_date <= pcc.period_to)
                                                        and
                                                         (cp.deactivation_date is null or cp.deactivation_date >= pcc.period_from))
                                                    and (:actionPods is null or pod.id in (:actionPods)))
                              select case
                                         when cd.contract_type = 'COMBINED'
                                             then
                                             pdpd.scaleSum /
                                             ((max(i.meter_reading_period_to) -
                                               min(i.meter_reading_period_from) + 1) * 24)
                                         else pdpd.profileSum /
                                              ((max(i.meter_reading_period_to) -
                                                min(i.meter_reading_period_from) + 1) * 24)
                                         end as actual_pav
                              from product_contract.contracts cont
                                       join invoice.invoices i
                                            on cont.id = i.product_contract_id
                                                and i.status = 'REAL'
                                                and i.type = 'STANDARD'
                                                and i.invoice_date >= c.activation_date
                                       join invoice.invoice_standard_detailed_data isdd on i.id = isdd.invoice_id
                                       join pod_data pdpd on isdd.pod_id = any (pdpd.podIds)
                              where cont.id = c.id
                                and cd.contract_type <> 'WITHOUT_SUPPLY'
                              group by pdpd.scaleSum, pdpd.profileSum
                              ) eval_actual_pav on true) as tbl
                    """
    )
    PenaltyCalculationProductContractVariables getPenaltyCalculationVariablesForProductContract(
            @Param("contractId") Long contractId,
            @Param("executionDate") LocalDate executionDate,
            @Param("terminationId") Long terminationId,
            @Param("actionPods") List<Long> actionPods // these are the IDs of pods
    );

    @Query("""
            select pcd.customerDetailId from ProductContractDetails pcd
            where pcd.contractId = :contractId
            """)
    List<Long> getCustomerIdsToChangeStatusWithContractId(@Param("contractId") Long contractId);

    @Query(value = """
            select pcd
            from ProductContractDetails pcd
            where pcd.contractId = :contractId
            and pcd.startDate <= :signingDate
            and pcd.versionStatus = 'SIGNED'
            order by pcd.startDate desc
            """)
    List<ProductContractDetails> findVersionBeforeSigned(Long contractId, LocalDate signingDate, Pageable pageable);

    @Query(value = """
            select
              c.id as contractId,
              cd.id as contractDetailId,
              cp.id as contractPointOfDeliveryId,
              cp.pod_detail_id as pointOfDeliveryDetailId,
              p.id as podId
            from
              product_contract.contracts c
              join product_contract.contract_details cd on cd.contract_id = c.id
              join product_contract.contract_pods cp on cp.contract_detail_id = cd.id
              and cp.status = 'ACTIVE'
              and cd.status = 'SIGNED'
              join pod.pod_details pd on cp.pod_detail_id = pd.id
              join pod.pod p on pd.pod_id = p.id
              and p.status = 'ACTIVE'
            where
              cd.id in (
                select
                  id
                from
                  (
                    select
                      cd.id,
                      c.id as contract_id,
                      cd.start_date,
                      coalesce(
                        lead(cd.start_date) over (
                          partition by cd.contract_id
                          order by
                            cd.start_date
                        ) - 1,
                        date('2090-12-31')
                      ) as end_date,
                      cd.customer_detail_id,
                      array(
                        select
                          distinct p.id
                        from
                          product_contract.contract_pods cp
                          join pod.pod_details pd on cp.pod_detail_id = pd.id
                          join pod.pod p on pd.pod_id = p.id
                          and p.status = 'ACTIVE'
                        where
                          cp.contract_detail_id in(
                            select
                              cd2.id
                            from
                              product_contract.contract_details cd2
                            where
                              cd2.contract_id = c.id
                          )
                          and cp.activation_date is not null
                          and cp.status = 'ACTIVE'
                      ) as searched_pods,
                      cast(
                        string_to_array(:podIds, ',') as bigint[]
                      ) as parameter_pods
                    from
                      product_contract.contract_details cd
                      join product_contract.contracts c on cd.contract_id = c.id
                    where
                      c.resign_to_contract_id is null
                      and c.status = 'ACTIVE'
                      and c.contract_status in (
                        'ACTIVE_IN_TERM', 'ACTIVE_IN_PERPETUITY'
                      )
                      and cd.contract_id <> :contractId
                      and cd.customer_detail_id in (
                        select
                          id
                        from
                          customer.customer_details cdt
                        where
                          cdt.customer_id = :customerId
                      )
                  ) as tbl
                where
                  :signingDate between tbl.start_date
                  and tbl.end_date
                  and (
                    tbl.searched_pods <@ parameter_pods
                    and tbl.searched_pods <> '{}'
                  )
              )
            """, nativeQuery = true)
    List<ProductContractResigningWithCustomerAndPointOfDeliveryIntersectionMiddleResponse> findProductContractDetailsWithCustomerAndPointOfDeliveryIntersection(@Param("contractId") Long contractId,
                                                                                                                                                                @Param("customerId") Long customerId,
                                                                                                                                                                @Param("signingDate") LocalDate signingDate,
                                                                                                                                                                @Param("podIds") String podIds);


    @Query("""
            select pc
            from ProductContract pc
            join ProductContractDetails pcd on pcd.contractId = pc.id
            where (:contractId is null or pc.id <> :contractId)
            and pc.status = 'ACTIVE'
            and pcd.dealNumber = :dealNumber
            """)
    List<ProductContract> findProductContractDetailsWithPresentedDealNumberExcludingContractId(@Param("contractId") Long contractId,
                                                                                               @Param("dealNumber") String dealNumber);


    List<ProductContractDetails> findAllByDealNumber(String dealNumber);

    @Query("""
            select count(pcd) > 0
            from ProductContractDetails pcd
            join ProductContract pc on pc.id = pcd.contractId
            where pcd.id = :contractDetailId
            and pc.status = :status
            """)
    boolean existsByIdAndContractStatus(
            @Param("contractDetailId") Long contractDetailId,
            @Param("status") ProductContractStatus status);

    @Query("""
             select pcd
             from ProductContractDetails pcd
             join ProductContract pc on pc.id = pcd.contractId
             where pcd.contractId = :contractId
             and pc.status = 'ACTIVE'
             and pcd.startDate = (
                 select max(innerPcd.startDate)
                 from ProductContractDetails innerPcd
                 where innerPcd.customerDetailId = :customerDetailsId
                 and innerPcd.contractId = :contractId
                 )
            """)
    Optional<ProductContractDetails> findLatestDetailByContractIdAndByCustomerDetailsId(
            @Param("contractId") Long contractId,
            @Param("customerDetailsId") Long customerDetailsId);


    @Query("""
             select new bg.energo.phoenix.model.response.customer.CustomerCommunicationDataResponse(
                         cc.id,
                         cc.contactTypeName,
                         cc.createDate
                     )
             from ProductContractDetails pcd
             join ProductContract pc on pc.id = pcd.contractId
             join CustomerCommunications cc on cc.id = pcd.customerCommunicationIdForBilling
             where pcd.contractId = :contractId
             and pc.status = 'ACTIVE'
             and cc.status = 'ACTIVE'
             and pcd.startDate = (
                 select max(innerPcd.startDate)
                 from ProductContractDetails innerPcd
                 where innerPcd.customerDetailId = :customerDetailsId
                and innerPcd.contractId = :contractId
                 )
            """)
    Optional<CustomerCommunicationDataResponse> findCommunicationDataByContractIdAndCustomerDetailId(
            @Param("contractId") Long contractId,
            @Param("customerDetailsId") Long customerDetailsId);

    @Query("""
            select pc
            from ProductContractDetails pc
            join CustomerDetails cd on cd.id = pc.customerDetailId
            where cd.customerId = :customerId
            and pc.contractId in :contractIds
            and pc.startDate > current_date
            """)
    Stream<ProductContractDetails> getProductContractsByCustomerDetailsId(
            @Param("contractIds") List<Long> contractIds,
            @Param("customerId") Long customerId
    );

    @Query("""
            select new bg.energo.phoenix.model.request.contract.product.CurrentProductContractDetails(
            cd.id,
            cd.startDate,
            cd.contractId,
            cd.versionId
            )
             from ProductContract c
              join
             ProductContractDetails cd
             on cd.contractId = c.id
             and c.id in :contractIds
              join
             CustomerDetails cd2
              on cd.customerDetailId = cd2.id
              and cd2.customerId = :customerId
             and cd.startDate = (select max(cd2.startDate)
                                  from ProductContractDetails cd2
                                  where cd2.contractId = c.id
                                    and cd2.startDate <= current_date)
             """)
    Stream<CurrentProductContractDetails> findCurrentProductContractDetailsByContractIds(
            @Param("contractIds") List<Long> contractIds,
            @Param("customerId") Long customerId
    );

    @Query("""
             select new bg.energo.phoenix.model.response.customer.CustomerCommunicationDataResponse(
                         cc.id,
                         cc.contactTypeName,
                         cc.createDate
                     )
             from ProductContractDetails pcd
             join ProductContract pc on pc.id = pcd.contractId
             join CustomerCommunications cc on cc.id = pcd.customerCommunicationIdForBilling
             where pcd.contractId = :contractId
             and cc.id = :id
             and pc.status = 'ACTIVE'
             and pcd.startDate = (
                 select max(innerPcd.startDate)
                 from ProductContractDetails innerPcd
                 where innerPcd.customerDetailId = :customerDetailsId
                 and innerPcd.contractId = :contractId
                 )
            """)
    Optional<CustomerCommunicationDataResponse> findCommunicationDataByIdAndContractIdAndCustomerDetailId(@Param("id") Long id,
                                                                                                          @Param("contractId") Long productContractId,
                                                                                                          @Param("customerDetailsId") Long customerDetailId);

    @Query("""
            select pcd
            from ProductContractDetails pcd
            join ProductContract pc on pcd.contractId = pc.id
            where pcd.productDetailId = :productDetailId
            and cast(pc.contractStatus as string) not in ('TERMINATED', 'CANCELLED')
            and pc.status = 'ACTIVE'
            """)
    List<ProductContractDetails> findAllbyProductDetailId(Long productDetailId);

    @Query("""
            select pcd.productDetailId
            from ProductContractDetails pcd
            where pcd.id=:contractdetailid
             """)
    Long getProductDetailIdByContractDetailId(
            @Param("contractdetailid") Long contractDetailId
    );

    @Query("""
            select count(pcd) > 0
            from ProductContractDetails pcd
            join CustomerDetails cd on cd.id = pcd.customerDetailId
            join ProductContract pc on pcd.contractId = pc.id
            where pc.id = :contractId
            and pc.status = :status
            and cd.customerId = :customerId
            """)
    boolean existsByCustomerIdAndContractId(@Param("customerId") Long customerId,
                                            @Param("contractId") Long contractId,
                                            @Param("status") ProductContractStatus status);

    boolean existsByContractIdAndVersionId(Long contractId, Integer versionId);

    @Query("""
            select distinct pcd
            from ProductContractDetails pcd
            join ProductDetails pd on pd.id = pcd.productDetailId
            join ContractPods cp on cp.contractDetailId = pcd.id
            where cp.customModifyDate between :start and :end
            and cp.activationDate is not null
            and (
               (pd.productBalancingIdForConsumer.id is not null and coalesce(pcd.dealNumber, '') = '')
               or
               (pd.productBalancingIdForGenerator.id is not null and coalesce(cp.dealNumber, '') = '')
            )
            """)
    List<ProductContractDetails> findProductContractDetailsForDealCreation(@Param("start") LocalDateTime start,
                                                                           @Param("end") LocalDateTime end,
                                                                           PageRequest pageRequest);

    @Query("""
            select count(distinct pcd.id)
            from ProductContractDetails pcd
            join ProductDetails pd on pd.id = pcd.productDetailId
            join ContractPods cp on cp.contractDetailId = pcd.id
            and cp.customModifyDate between :start and :end
            and cp.activationDate is not null
            and (
               (pd.productBalancingIdForConsumer.id is not null and coalesce(pcd.dealNumber, '') = '')
               or
               (pd.productBalancingIdForGenerator.id is not null and coalesce(cp.dealNumber, '') = '')
            )
            """)
    long countProductContractDetailsForDealCreation(@Param("start") LocalDateTime start,
                                                    @Param("end") LocalDateTime end);

    @Query("""
                    select pcd.contractId from ProductContractDetails pcd
                    join CustomerDetails cd on pcd.customerDetailId = cd.id
                    join ProductContract pc on pcd.contractId = pc.id
                    where pc.id in :contractIds
                    and cd.customerId=:customerId
                    and pc.status=:status
            """)
    Set<Long> checkForIds(Set<Long> contractIds, Long customerId, ProductContractStatus status);

    @Query(value = """
            select pcd.*
            from product_contract.contracts pc
            join product_contract.contract_details pcd on pcd.contract_id = pc.id
            where pcd.start_date <= :date
            and pc.id = :productContractId
            order by pcd.start_date desc
            limit 1
            """, nativeQuery = true)
    Optional<ProductContractDetails> findRespectiveProductContractDetailsByProductContractId(LocalDate date,
                                                                                             Long productContractId);

    @Query(value = """
            select true from product_contract.contract_details det
            where det.id = :productContractDetailId and
                det.start_date =
                (select max(prodDet.start_date)
                 from product_contract.contract_details prodDet
                 where prodDet.contract_id = :productContractId
                   and prodDet.start_date <= current_date)
            """, nativeQuery = true)
    Boolean isProductContractCurrent(Long productContractDetailId, Long productContractId);

    @Query(value = """
            select min(conPod.activation_date) from product_contract.contract_details conDet
             join product_contract.contract_pods conPod
             on conDet.id = conPod.contract_detail_id
             where conDet.contract_id = :contractId
             and conPod.status = 'ACTIVE'
            """, nativeQuery = true)
    LocalDate minPodActivationDateForContractId(Long contractId);

    @Query("""
            select pcd from ProductContractDetails pcd
            where pcd.startDate = (
                select max(innerPCD.startDate)
                from ProductContractDetails innerPCD
                where innerPCD.contractId = :productContractId
                and innerPCD.startDate <= CURRENT_DATE
            )
            and pcd.contractId = :productContractId
            """)
    Optional<ProductContractDetails> findCurrentProductContractDetails(Long productContractId);

    @Query(
            value = """
                    select pc
                        from ProductContractDetails pcd
                         join ProductContract pc on pcd.contractId=pc.id
                        join CustomerDetails cd on cd.id = pcd.customerDetailId
                        join Customer c on c.id = cd.customerId
                            where c.status = 'ACTIVE'
                            and c.id = :customerId
                            and pcd.id = :contractDetailId
                    """
    )
    Optional<ProductContract> checkCustomerAndFetchContractNumber(
            @Param("customerId") Long customerId,
            @Param("contractDetailId") Long contractDetailId
    );

    @Query("""
                    select pc.id,pc.contractNumber from ProductContract pc
                    join ProductContractDetails pcd on pcd.contractId = pc.id
                    where pcd.id=:id
            """)
    List<Object[]> fetchProductContractNumberByDetailId(Long id);

    @Query("""
            select pcd from ProductContractDetails pcd
            where pcd.contractId = :productContractId
            and pcd.startDate > :sourceDate
            and pcd.versionStatus = 'SIGNED'
            order by pcd.startDate
            """)
    List<ProductContractDetails> findAllSignedProductContractNextVersion(Long productContractId, LocalDate sourceDate);

    @Query("""
            select pcd from ProductContractDetails pcd
            where pcd.contractId = :productContractId
            and pcd.startDate > :sourceDate
            and pcd.versionStatus = 'SIGNED'
            order by pcd.startDate
            """)
    Optional<ProductContractDetails> findSignedProductContractNextVersion(Long productContractId, LocalDate sourceDate, PageRequest pageRequest);

    @Query("""
            select pcd from ProductContractDetails pcd
            where pcd.contractId = :productContractId
            and pcd.versionStatus = 'SIGNED'
            order by pcd.startDate DESC
            """)
    Optional<ProductContractDetails> findLastSignedProductContractVersion(Long productContractId, PageRequest pageRequest);

    @Query("""
            select pcd from ProductContractDetails pcd
            where pcd.contractId = :productContractId
            and pcd.startDate < :sourceDate
            and pcd.versionStatus = 'SIGNED'
            order by pcd.startDate desc
            """)
    Optional<ProductContractDetails> findSignedProductContractPreviousVersion(Long productContractId, LocalDate sourceDate, PageRequest pageRequest);

    @Query("""
            select new bg.energo.phoenix.model.response.contract.productContract.ProductContractVersionWithStatusResponse(
                pcd.id,
                pcd.contractId,
                pcd.versionId,
                pcd.startDate,
                pcd.endDate,
                case
                        when pcd.versionStatus = 'SIGNED' then 'Valid'
                        else 'Not Valid'
                    end
            )
            from ProductContractDetails pcd
            where pcd.contractId = :contractId
            order by 
                case when pcd.versionStatus = 'SIGNED' then 0 else 1 end,
                pcd.startDate
            """)
    List<ProductContractVersionWithStatusResponse> findProductContractVersionsOrderedByStatusAndStartDate(
            @Param("contractId") Long contractId);

    @Query(value = """
            SELECT CASE
                       WHEN cl.creation_type = 'MANUAL' AND cl.contract_billing_group_id IS NOT NULL
                           THEN (SELECT MAX(cd.customer_communication_id_for_billing)
                                 FROM product_contract.contracts c
                                          JOIN product_contract.contract_details cd ON c.id = cd.contract_id
                                          JOIN product_contract.contract_billing_groups cbg ON c.id = cbg.contract_id
                                 WHERE cbg.id = cl.contract_billing_group_id)
                        
                       WHEN cl.outgoing_document_type = 'INVOICE' THEN (SELECT COALESCE(MAX(i.customer_communication_id),
                                                                                        MAX(i.contract_communication_id))
                                                                        FROM invoice.invoices i
                                                                        WHERE i.id = cl.invoice_id)
                        
                       WHEN cl.outgoing_document_type = 'LATE_PAYMENT_FINE' AND lpf.parent_liability_id IS NOT NULL
                           THEN (SELECT COALESCE(MAX(i.customer_communication_id), MAX(i.contract_communication_id))
                                 FROM invoice.invoices i
                                          JOIN receivable.customer_liabilities parent_cl ON parent_cl.id = lpf.parent_liability_id
                                 WHERE parent_cl.creation_type = 'AUTOMATIC'
                                   AND parent_cl.invoice_id IS NOT NULL
                                   AND parent_cl.invoice_id = i.id)
                        
                       WHEN cl.outgoing_document_type = 'LATE_PAYMENT_FINE' AND lpf.parent_liability_id IS NOT NULL
                           THEN (SELECT MAX(cd.customer_communication_id_for_billing)
                                 FROM product_contract.contracts c
                                          JOIN product_contract.contract_details cd ON c.id = cd.contract_id
                                          JOIN product_contract.contract_billing_groups cbg ON c.id = cbg.contract_id
                                          JOIN receivable.customer_liabilities parent_cl ON parent_cl.id = lpf.parent_liability_id
                                 WHERE parent_cl.creation_type = 'MANUAL'
                                   AND parent_cl.contract_billing_group_id IS NOT NULL
                                   AND parent_cl.contract_billing_group_id = cbg.id)
                        
                       WHEN cl.outgoing_document_type = 'RESCHEDULING' THEN (SELECT MAX(r.customer_communication_id)
                                                                             FROM receivable.reschedulings r
                                                                             WHERE r.id = cl.rescheduling_id)
                       END AS customer_communication_id
            FROM receivable.customer_liabilities cl
                     LEFT JOIN receivable.late_payment_fines lpf ON cl.late_payment_fine_id = lpf.id
            WHERE cl.id = :customerLiabilityId
            """, nativeQuery = true)
    Long findCustomerCommunicationIdForBillingByLiabilityId(Long customerLiabilityId);

}
