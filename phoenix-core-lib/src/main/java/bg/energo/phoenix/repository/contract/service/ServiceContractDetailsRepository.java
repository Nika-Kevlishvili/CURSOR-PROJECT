package bg.energo.phoenix.repository.contract.service;

import bg.energo.phoenix.model.entity.EntityStatus;
import bg.energo.phoenix.model.entity.contract.service.ServiceContractDetails;
import bg.energo.phoenix.model.request.contract.service.CurrentServiceContractDetails;
import bg.energo.phoenix.model.response.contract.action.calculation.PenaltyCalculationServiceContractVariables;
import bg.energo.phoenix.model.response.contract.productContract.ContractDetailForOvertimeResponse;
import bg.energo.phoenix.model.response.contract.serviceContract.ContractDetailForPerPieceResponse;
import bg.energo.phoenix.model.response.contract.serviceContract.ServiceContractAdditionalParametersResponse;
import bg.energo.phoenix.model.response.customer.CustomerCommunicationDataResponse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

public interface ServiceContractDetailsRepository extends JpaRepository<ServiceContractDetails, Long> {

    List<ServiceContractDetails> findByContractId(Long id);

    List<ServiceContractDetails> findByContractIdOrderByStartDate(Long id);

    boolean existsByContractIdAndVersionId(Long id, Long versionId);

    @Query(value = """
            select scd.*
            from service_contract.contract_details scd
            where scd.contract_id = :serviceContractId
            and scd.start_date <= :date
            order by scd.start_date desc
            limit 1
            """, nativeQuery = true)
    Optional<ServiceContractDetails> findRespectiveServiceContractDetailsByServiceContractId(LocalDate date, Long serviceContractId);

    @Query(value = """

            select c.id from service_contract.contracts c
                                                    where c.contract_number = :contract_number
                                                    and c.status = 'ACTIVE'
                                                    and
                                                    c.contract_status in ('READY', 'SIGNED', 'ENTERED_INTO_FORCE', 'ACTIVE_IN_TERM','ACTIVE_IN_PERPETUITY')
                                                    
            """, nativeQuery = true)
    Long findByContractIdAndLatestDetail(@Param("contract_number") String contractNumber);

    @Query(value = """

            select c.id from service_contract.contracts c
                                                    where c.contract_number = :contract_number
                                                    and c.status = 'ACTIVE'
            """, nativeQuery = true)
    Long findByContractIdAndLatestDetailsWithoutStatuses(@Param("contract_number") String contractNumber);

    Optional<ServiceContractDetails> findByContractIdAndVersionId(Long id, Long versionId);

    @Query("""
            select max(scd.versionId) from ServiceContractDetails  scd
            where scd.contractId = :contractId
            """)
    Long findByContractIdAndVersionIdMax(@Param("contractId") Long contractId);

    Optional<ServiceContractDetails> findFirstByContractIdOrderByVersionIdDesc(Long id);


    @Query(
            value = """
                    select scd.id from ServiceContractDetails scd
                    join ServiceContracts sc on scd.contractId = sc.id
                        where scd.id in :ids
                        and sc.status = 'ACTIVE'
                    """
    )
    List<Long> findByIdIn(@Param("ids") List<Long> ids);

    @Query("""
            select max(scd.versionId) from ServiceContractDetails scd
            where scd.contractId = :contractId
            """)
    Long findMaxVersionId(@Param("contractId") Long contractId);

    boolean existsByContractIdAndStartDate(Long id, LocalDate startDate);


    List<ServiceContractDetails> findAllByContractIdOrderByStartDateDesc(Long id);


    @Query(
            value = """
                    select new bg.energo.phoenix.model.response.contract.serviceContract.ServiceContractAdditionalParametersResponse(
                        scd.directDebit,
                        am.id,
                        concat(am.displayName, ' (', am.userName, ')'),
                        b.id,
                        b.name,
                        b.bic,
                        scd.iban,
                        ir.id,
                        ir.name,
                        c.id,
                        c.name
                    )
                    from ServiceContractDetails scd
                    left join AccountManager am on scd.employeeId = am.id
                    left join Bank b on scd.bankId = b.id
                    join InterestRate ir on scd.applicableInterestRate = ir.id
                    left join Campaign c on scd.campaignId = c.id
                        where scd.id = :contractDetailId
                    """
    )
    ServiceContractAdditionalParametersResponse getAdditionalParametersByContractDetailId(
            @Param("contractDetailId") Long contractDetailId
    );


    @Query(
            nativeQuery = true,
            value = """
                    select id from(
                    select cd.id,
                           cd.start_date,
                           coalesce(lead(cd.start_date, 1) OVER (order by cd.start_date), date '9999-12-31')-1 as next_date
                    from service_contract.contracts c
                    join
                    service_contract.contract_details cd
                    on cd.contract_id = c.id and c.id =:contractId
                    and c.status = 'ACTIVE') as tbl
                    where :executionDate between tbl.start_date and tbl.next_date;
                    """
    )
    Long getContractDetailIdByExecutionDate(
            @Param("contractId") Long contractId,
            @Param("executionDate") LocalDate executionDate
    );


    @Query(
            value = """
                    select count (scd.id) > 0
                        from ServiceContractDetails scd
                        join CustomerDetails cd on cd.id = scd.customerDetailId
                        join Customer c on c.id = cd.customerId
                            where c.status = 'ACTIVE'
                            and c.id = :customerId
                            and scd.id = :contractDetailId
                    """
    )
    boolean isCustomerAttachedToContractDetail(
            @Param("customerId") Long customerId,
            @Param("contractDetailId") Long contractDetailId
    );

    @Query("""
            select max(cd.agreementSuffix) from ServiceContractDetails  cd
            where cd.contractId = :contractId
            """)
    Optional<Integer> findContractAgreementSuffixValue(Long contractId);

    @Query("""
            select pcd from ServiceContractDetails pcd
            where pcd.startDate = (
                select max(innerPCD.startDate) from ServiceContractDetails innerPCD
                where innerPCD.contractId = :serviceContractId
            )
            and pcd.contractId = :serviceContractId
            """)
    Optional<ServiceContractDetails> findLatestServiceContractDetails(Long serviceContractId);


    @Query(
            nativeQuery = true,
            value = """
                    select
                         cd.id as contractDetailId,
                         c.id as contractId,
                         c.contract_term_end_date as contractTermEndDate,
                         case
                            when :terminationId is null then :executionDate
                            else (select case
                                when t.auto_termination_from = 'EVENT_DATE' then :executionDate
                                when t.auto_termination_from = 'FIRST_DAY_OF_MONTH_FOLLOWING_EVENT_DATE'
                                   then date(date_trunc('month', current_date +interval '1 month'))
                                   end from product.terminations t where t.id = :terminationId)
                            end as realTerminationDate
                        from service_contract.contracts c
                        join service_contract.contract_details cd on cd.contract_id = c.id
                            and c.id = :contractId
                            and c.status = 'ACTIVE'
                            and (:executionDate >= cd.start_date and :executionDate < coalesce((
                                select min(start_date) from service_contract.contract_details cd1
                                where cd1.contract_id = cd.contract_id
                                and cd1.start_date > cd.start_date), date(:executionDate) + 1))
                    """
    )
    PenaltyCalculationServiceContractVariables getPenaltyCalculationVariablesForServiceContract(
            @Param("contractId") Long serviceContractId,
            @Param("executionDate") LocalDate executionDate,
            @Param("terminationId") Long terminationId
    );

    @Query("""
            select scd.customerDetailId from ServiceContractDetails scd
            where scd.contractId = :contractId
            """)
    List<Long> getCustomerIdsToChangeStatusWithContractId(@Param("contractId") Long contractId);

    @Query("""
            select sc
            from ServiceContractDetails sc
            join CustomerDetails cd on cd.id = sc.customerDetailId
            where cd.customerId = :customerId
            and sc.contractId in :contractIds
            and sc.startDate > current_date
            """)
    Stream<ServiceContractDetails> getServiceContractsByCustomerDetailsId(
            @Param("contractIds") List<Long> contractIds,
            @Param("customerId") Long customerId
    );


    @Query("""
             select scd
             from ServiceContractDetails scd
             join ServiceContracts sc on sc.id = scd.contractId
             where scd.contractId = :contractId
             and sc.status = 'ACTIVE'
             and scd.startDate = (
                 select max(innerScd.startDate)
                 from ServiceContractDetails innerScd
                 where innerScd.customerDetailId = :customerDetailsId
                 and innerScd.contractId = :contractId
                 )
            """)
    Optional<ServiceContractDetails> findByContractIdAndLatestDetailByCustomerDetailsId(
            @Param("contractId") Long contractId,
            @Param("customerDetailsId") Long customerDetailsId);

    @Query("""
             select new bg.energo.phoenix.model.response.customer.CustomerCommunicationDataResponse(
                         cc.id,
                         cc.contactTypeName,
                         cc.createDate
                     )
             from ServiceContractDetails scd
             join ServiceContracts sc on sc.id = scd.contractId
             join CustomerCommunications cc on cc.id = scd.customerCommunicationIdForBilling
             where scd.contractId = :contractId
             and sc.status = 'ACTIVE'
             and cc.status = 'ACTIVE'
             and scd.startDate = (
                 select max(innerSCD.startDate)
                 from ServiceContractDetails innerSCD
                 where innerSCD.customerDetailId = :customerDetailsId
                 and innerSCD.contractId = :contractId
                 )
            """)
    Optional<CustomerCommunicationDataResponse> findCommunicationDataByContractIdAndCustomerDetailId(
            @Param("contractId") Long contractId,
            @Param("customerDetailsId") Long customerDetailsId);

    @Query("""
            select new bg.energo.phoenix.model.request.contract.service.CurrentServiceContractDetails(
            cd.id,
            cd.startDate,
            cd.contractId,
            cd.versionId
            )
             from ServiceContracts c
              join
             ServiceContractDetails cd
             on cd.contractId = c.id
             and c.id in :contractIds
              join
             CustomerDetails cd2
              on cd.customerDetailId = cd2.id
              and cd2.customerId = :customerId
             and cd.startDate = (select max(cd2.startDate)
                                  from ServiceContractDetails cd2
                                  where cd2.contractId = c.id
                                    and cd2.startDate <= current_date)
             """)
    Stream<CurrentServiceContractDetails> findCurrentServiceContractDetailsByContractIds(
            @Param("contractIds") List<Long> contractIds,
            @Param("customerId") Long customerId
    );

    @Query("""
             select new bg.energo.phoenix.model.response.customer.CustomerCommunicationDataResponse(
                         cc.id,
                         cc.contactTypeName,
                         cc.createDate
                     )
             from ServiceContractDetails scd
             join ServiceContracts sc on sc.id = scd.contractId
             join CustomerCommunications cc on cc.id = scd.customerCommunicationIdForBilling
             where scd.contractId = :contractId
             and cc.id = :id
             and sc.status = 'ACTIVE'
             and scd.startDate = (
                 select max(innerSCD.startDate)
                 from ServiceContractDetails innerSCD
                 where innerSCD.customerDetailId = :customerDetailsId
                 and innerSCD.contractId = :contractId
                 )
            """)
    Optional<CustomerCommunicationDataResponse> findCommunicationDataByIdAndContractIdAndCustomerDetailId(@Param("id") Long customerCommunicationId,
                                                                                                          @Param("contractId") Long contractId,
                                                                                                          @Param("customerDetailsId") Long customerDetailsId);

    @Query("""
            select scd
            from ServiceContractDetails scd
            join ServiceContracts sc on scd.contractId = sc.id
            where sc.status = 'ACTIVE'
            and cast(sc.contractStatus as string) not in('TERMINATED', 'CANCELLED')
            and scd.serviceDetailId in(:updatedServiceDetailsWithRelatedContracts)
            """)
    List<ServiceContractDetails> findAllActiveServiceContractByServiceDetailIds(List<Long> updatedServiceDetailsWithRelatedContracts);

    @Query("""
            select count(scd) > 0
            from ServiceContractDetails scd
            join CustomerDetails cd on scd.customerDetailId = cd.id
            join ServiceContracts sc on scd.contractId = sc.id
            where sc.id = :contractId
            and cd.customerId = :customerId
            and sc.status = :status
            """)
    boolean existsByCustomerIdAndContractId(@Param("contractId") Long contractId,
                                            @Param("customerId") Long customerId,
                                            @Param("status") EntityStatus status);
    @Query("""
        select scd.contractId from ServiceContractDetails scd
        join CustomerDetails cd on scd.customerDetailId = cd.id
        join ServiceContracts sc on scd.contractId = sc.id
        where sc.id in :contractIds
        and cd.customerId=:customerId
        and sc.status=:status
""")
    Set<Long> checkForIds(Set<Long> contractIds,Long customerId,EntityStatus status);

    @Query(value = """
            select tbl.contract_id                     as contractId,
                   tbl.contract_detail_id              as contractDetailId,
                   tbl.invoice_payment_term_value      as invoicePaymentTermValue,
                   tbl.applicable_interest_rate        as applicableInterestRate,
                   tbl.customer_detail_id              as customerDetailId,
                   tbl.billing_group_direct_debit      as billingGroupDirectDebit,
                   tbl.billing_group_bank_id           as billingGroupBankId,
                   tbl.billing_group_iban              as billingGroupIban,
                   tbl.contract_direct_debit           as contractDirectDebit,
                   tbl.contract_bank_id                as contractBankId,
                   tbl.contract_iban                   as contractIban,
                   tbl.customer_direct_debit           as customerDirectDebit,
                   tbl.customer_bank_id                as customerBankId,
                   tbl.customer_iban                   as customerIban,
                   tbl.detail_id                       as serviceOrProductDetailId,
                   tbl.income_account_number           as incomeAccountNumber,
                   tbl.cost_center_controlling_order   as costCenterControllingOrder,
                   tbl.communication_id_for_billing    as communicationIdForBilling,
                   tbl.bg_communication_id_for_billing as billingGroupCommunicationIdForBilling,
                   tbl.contract_type                   as contractType
            from (select c.id                                      as contract_id,
                         cd.id                                     as contract_detail_id,
                         cd.invoice_payment_term_value,
                         cd.applicable_interest_rate,
                         cd.customer_detail_id,
                         cbg.direct_debit                          as billing_group_direct_debit,
                         cbg.bank_id                               as billing_group_bank_id,
                         cbg.iban                                  as billing_group_iban,
                         cd.direct_debit                           as contract_direct_debit,
                         cd.bank_id                                as contract_bank_id,
                         cd.iban                                   as contract_iban,
                         cdet.direct_debit                         as customer_direct_debit,
                         cdet.bank_id                              as customer_bank_id,
                         cdet.iban                                 as customer_iban,
                         cd.product_detail_id                      as detail_id,
                         pd.income_account_number,
                         pd.cost_center_controlling_order,
                         cd.customer_communication_id_for_billing  as communication_id_for_billing,
                         cbg.customer_communication_id_for_billing as bg_communication_id_for_billing,
                         'PRODUCT_CONTRACT'                        as contract_type
                  from product_contract.contracts c
                           join
                       product_contract.contract_details cd
                       on cd.contract_id = c.id
                           join product.product_details pd
                                on cd.product_detail_id = pd.id
                           join customer.customer_details cdet
                                on cdet.id = cd.customer_detail_id
                           join product_contract.contract_billing_groups cbg
                                on cbg.contract_id = c.id
                           left join customer.customer_communications cc on cc.id = cbg.customer_communication_id_for_billing
                  where c.id = :contractId
                    and c.status = 'ACTIVE'
                    and cd.start_date =
                        (select max(start_date)
                         from product_contract.contract_details cd2
                         where cd2.contract_id
                             = c.id
                           and cd2.start_date <= current_date)
                  union
                  select c.id                                     as contract_id,
                         cd.id                                    as contract_detail_id,
                         cd.invoice_payment_term_value,
                         cd.applicable_interest_rate,
                         cd.customer_detail_id,
                         null                                     as billing_group_direct_debit,
                         null                                     as billing_group_bank_id,
                         null                                     as billing_group_iban,
                         cd.direct_debit                          as contract_direct_debit,
                         cd.bank_id                               as contract_bank_id,
                         cd.iban                                  as contract_iban,
                         cdet.direct_debit                        as customer_direct_debit,
                         cdet.bank_id                             as customer_bank_id,
                         cdet.iban                                as customer_iban,
                         cd.service_detail_id                     as detail_id,
                         sd.income_account_number,
                         sd.cost_center_controlling_order,
                         cd.customer_communication_id_for_billing as communication_id_for_billing,
                         null                                     as bg_communication_id_for_billing,
                         'SERVICE_CONTRACT'                       as contract_type
                  from service_contract.contracts c
                           join
                       service_contract.contract_details cd
                       on cd.contract_id = c.id
                           join service.service_details sd
                                on sd.id = cd.service_detail_id
                           join customer.customer_details cdet
                                on cdet.id = cd.customer_detail_id
                  where c.id = :contractId
                    and c.status = 'ACTIVE'
                    and cd.start_date =
                        (select max(start_date)
                         from service_contract.contract_details cd2
                         where cd2.contract_id
                             = c.id
                           and cd2.start_date <= current_date)) as tbl
            where tbl.contract_type = :contractType
            """, nativeQuery = true)
    Optional<ContractDetailForOvertimeResponse> getContractDetailForOvertime(@Param("contractId") Long contractId,
                                                                             @Param("contractType") String contractType);

    @Query(value = """
            select c.id                                     as contractId,
                   cd.id                                    as contractDetailId,
                   cd.invoice_payment_term_value            as invoicePaymentTermValue,
                   cd.applicable_interest_rate              as applicableInterestRate,
                   cd.customer_detail_id                    as customerDetailId,
                   cd.direct_debit                          as contractDirectDebit,
                   cd.bank_id                               as contractBankId,
                   cd.iban                                  as contractIban,
                   cdet.direct_debit                        as customerDirectDebit,
                   cdet.bank_id                             as customerBankId,
                   cdet.iban                                as customerIban,
                   cd.service_detail_id                     as detailId,
                   sd.income_account_number                 as incomeAccountNumber,
                   sd.cost_center_controlling_order         as costCenterControllingOrder,
                   cd.customer_communication_id_for_billing as communicationIdForBilling,
                   cd.quantity                              as quantity
            from service_contract.contracts c
                     join
                 service_contract.contract_details cd
                 on cd.contract_id = c.id
                     join service.service_details sd
                          on sd.id = cd.service_detail_id
                     join customer.customer_details cdet
                          on cdet.id = cd.customer_detail_id
            where c.id = :contractId
              and c.status = 'ACTIVE'
              and cd.start_date =
                  (select max(start_date)
                   from service_contract.contract_details cd2
                   where cd2.contract_id
                       = c.id
                     and cd2.start_date <= :startDate)
            """, nativeQuery = true)
    Optional<ContractDetailForPerPieceResponse> getContractDetailForPerPiece(@Param("contractId") Long contractId,
                                                                             @Param("startDate") LocalDate startDate);

    @Query(value = """
            select true from service_contract.contract_details det
           where det.id = :serviceContractDetailId and
               det.start_date =
               (select max(serDet.start_date)
                from service_contract.contract_details serDet
                where serDet.contract_id = :serviceContractId
                  and serDet.start_date <= current_date)
            """, nativeQuery = true)
    Boolean isServiceContractCurrent(Long serviceContractDetailId, Long serviceContractId);

    @Query("""
        select scd from ServiceContractDetails scd
        where scd.startDate = (
            select max(innerSCD.startDate)
            from ProductContractDetails innerSCD
            where innerSCD.contractId = :serviceContractId
            and innerSCD.startDate <= CURRENT_DATE
        )
        and scd.contractId = :serviceContractId
        """)
    Optional<ServiceContractDetails> findCurrentServiceContractDetails(Long serviceContractId);
}
