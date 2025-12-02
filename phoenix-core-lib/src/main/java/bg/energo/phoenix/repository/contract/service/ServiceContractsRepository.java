package bg.energo.phoenix.repository.contract.service;

import bg.energo.phoenix.model.documentModels.contract.response.ContractMainResponse;
import bg.energo.phoenix.model.documentModels.termination.TerminationEmailDocumentResponse;
import bg.energo.phoenix.model.entity.EntityStatus;
import bg.energo.phoenix.model.entity.contract.service.ServiceContractDetails;
import bg.energo.phoenix.model.entity.contract.service.ServiceContracts;
import bg.energo.phoenix.model.entity.product.service.ServiceContractTerm;
import bg.energo.phoenix.model.enums.contract.service.ServiceContractContractType;
import bg.energo.phoenix.model.enums.contract.service.ServiceContractDetailStatus;
import bg.energo.phoenix.model.enums.contract.service.ServiceContractDetailsSubStatus;
import bg.energo.phoenix.model.response.billing.billingRun.manualInvoice.ContractOrderShortResponse;
import bg.energo.phoenix.model.response.contract.priceComponent.PriceComponentForContractResponse;
import bg.energo.phoenix.model.response.contract.productContract.ContractWithStatusShortResponse;
import bg.energo.phoenix.model.response.contract.productContract.priceComponentFormula.IapResponseFromNativeQuery;
import bg.energo.phoenix.model.response.contract.productContract.terminations.serviceContract.ServiceContractTerminationByTermsResponse;
import bg.energo.phoenix.model.response.contract.productContract.terminations.serviceContract.ServiceContractTerminationWithActionsResponse;
import bg.energo.phoenix.model.response.contract.productContract.terminations.serviceContract.ServiceContractTerminationWithContractTermsResponse;
import bg.energo.phoenix.model.response.contract.serviceContract.ServiceContractListingResponse;
import bg.energo.phoenix.model.response.crm.emailCommunication.MassCommunicationFileProcessedResultProjection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface ServiceContractsRepository extends JpaRepository<ServiceContracts, Long> {

    @Query(value = "select  nextval('service_contract.contract_number_seq')", nativeQuery = true)
    String getNextSequenceValue();

    Optional<ServiceContracts> findByIdAndStatusIn(Long id, List<EntityStatus> statuses);

    Optional<ServiceContracts> findByContractNumberAndStatus(String contractNumber, EntityStatus statuses);

    Optional<ServiceContracts> findByIdAndStatusAndContractStatusIsNotIn(Long id, EntityStatus status, List<ServiceContractDetailStatus> contractStatuses);

    boolean existsByIdAndStatusAndContractStatusNotIn(Long id, EntityStatus status, List<ServiceContractDetailStatus> contractStatuses);

    @Query(
            value = """
                    select count(sc.id) > 0 from ServiceContracts sc
                    left join ServiceContractActivity sca on sc.id = sca.contractId
                    join SystemActivity sa on sa.id = sca.systemActivityId
                        where sc.id = :id
                        and sc.status = 'ACTIVE'
                        and sca.status = 'ACTIVE'
                        and sa.status = 'ACTIVE'
                    """
    )
    boolean hasConnectionToActivity(Long id);

    @Query(value = """
            select new bg.energo.phoenix.model.response.contract.serviceContract.ServiceContractListingResponse(
                                                             sc.id,
                                                             sc.contractNumber,
                                                             c.customerType,
                                                             c.identifier,
                                                             cd.name,
                                                             cd.middleName,
                                                             cd.lastName,
                                                             lf.name,
                                                             sd.name,
                                                             sd.serviceType,
                                                             sc.signingDate,
                                                             sc.contractStatus,
                                                             sc.subStatus,
                                                             sc.contractTermEndDate,
                                                             sc.perpetuityDate,
                                                             sc.status,
                                                             sc.createDate,
                                                             scd.type,
                                                             scd.agreementSuffix,
                                                             (case when exists (select 1 from Invoice inv where inv.serviceContractId = sc.id and inv.invoiceStatus = 'REAL') then true else false end)
                                                             )
                                                         from ServiceContracts sc
                                                         join ServiceContractDetails scd on scd.contractId = sc.id
                                                         join CustomerDetails cd on cd.id = scd.customerDetailId
                                                         join Customer c on cd.customerId = c.id
                                                         left join LegalForm lf on lf.id = cd.legalFormId
                                                         join ServiceDetails sd on scd.serviceDetailId = sd.id
                                                         and sc.id in(
                                                             select innerSC.id
                                                             from ServiceContracts innerSC
                                                             join ServiceContractDetails innerSCD on innerSCD.contractId = innerSC.id
                                                             where (
                                                                 (:contractStatus) is null
                                                                 or innerSC.contractStatus in :contractStatus
                                                             )
                                                             and (
                                                                 (:contractSubStatus) is null
                                                                 or innerSC.subStatus in :contractSubStatus
                                                             )
                                                             and (
                                                                 (:types) is null
                                                                 or innerSCD.type in :types
                                                                 )
                                                             and (
                                                                 (
                                                                     cast(:terminationDateFrom as date) is null
                                                                     and cast(:terminationDateTo as date) is null
                                                                 )
                                                                 or (
                                                                     cast(:terminationDateFrom as date) is not null
                                                                     and cast(:terminationDateTo as date) is not null
                                                                     and innerSC.terminationDate between :terminationDateFrom and :terminationDateTo
                                                                 )
                                                                 or (
                                                                     cast(:terminationDateFrom as date) is not null
                                                                     and cast(:terminationDateTo as date) is null
                                                                     and innerSC.terminationDate >= :terminationDateFrom
                                                                 )
                                                                 or (
                                                                     cast(:terminationDateFrom as date) is null
                                                                     and cast(:terminationDateTo as date) is not null
                                                                     and innerSC.terminationDate <= :terminationDateTo
                                                                 )
                                                                 )
                                                             and (
                                                                 (
                                                                     cast(:perpetuityDateFrom as date) is null
                                                                     and cast(:perpetuityDateTo as date) is null
                                                                 )
                                                                 or (
                                                                     cast(:perpetuityDateFrom as date) is not null
                                                                     and cast(:perpetuityDateTo as date) is not null
                                                                     and innerSC.perpetuityDate between :perpetuityDateFrom and :perpetuityDateTo
                                                                 )
                                                                 or (
                                                                     cast(:perpetuityDateFrom as date) is not null
                                                                     and cast(:perpetuityDateTo as date) is null
                                                                     and innerSC.perpetuityDate >= :perpetuityDateFrom
                                                                 )
                                                                 or (
                                                                     cast(:perpetuityDateFrom as date) is null
                                                                     and cast(:perpetuityDateTo as date) is not null
                                                                     and innerSC.perpetuityDate <= :perpetuityDateTo
                                                                 )
                                                                 )
                                                             and (
                                                                   (:serviceIds) is null
                                                                    or (innerSCD.serviceDetailId in :serviceIds)
                                                                  )
                                                             and (
                                                                  innerSC.status in(:statuses)
                                                                  )
                                                             and(
                                                                 (
                                                                     (:accountManagerIds) is null
                                                                      or exists
                                                                         (
                                                                             select 1 from CustomerAccountManager cam
                                                                             where cam.customerDetail.id = innerSCD.customerDetailId
                                                                             and cam.status = 'ACTIVE'
                                                                             and cam.managerId in (:accountManagerIds)
                                                                         )
                                                                 )
                                                             )
                                                             and (coalesce(:excludeVersions, '0') = '0' or
                                                                                   (:excludeVersions = 'OLDVERSION' and
                                                                                    innerSCD.startDate >=
                                                                                    (select max(scd2.startDate)
                                                                                     from ServiceContractDetails scd2
                                                                                     where scd2.contractId = innerSC.id
                                                                                       and scd2.startDate <= current_date)
                                                                                       )
                                                                                or
                                                                                   (:excludeVersions = 'FUTUREVERSION' and
                                                                                    innerSCD.startDate <=
                                                                                    (select max(scd2.startDate)
                                                                                     from ServiceContractDetails scd2
                                                                                     where scd2.contractId = innerSC.id
                                                                                       and scd2.startDate <= current_date)
                                                                                       )
                                                                                or
                                                                                   (:excludeVersions = 'OLDANDFUTUREVERSION' and
                                                                                    innerSCD.startDate =
                                                                                    (select max(scd2.startDate)
                                                                                     from ServiceContractDetails scd2
                                                                                     where scd2.contractId = innerSC.id
                                                                                       and scd2.startDate <= current_date)
                                                                                       )
                                                                                )
                                                             and (
                                                                 :prompt is null
                                                                 or (
                                                                     :searchBy = 'ALL'
                                                                     and (
                                                                         lower(innerSC.contractNumber) like :prompt
                                                                         or exists (
                                                                             select 1 from Customer innerCustomer
                                                                             join CustomerDetails innerCustomerDetails on innerCustomerDetails.customerId = innerCustomer.id
                                                                             where innerSCD.customerDetailId = innerCustomerDetails.id
                                                                             and (
                                                                                 lower(innerCustomerDetails.name) like :prompt
                                                                                 or innerCustomer.identifier like :prompt
                                                                             )
                                                                         )
                                                                     )
                                                                 )
                                                                 or (
                                                                         (
                                                                             :searchBy = 'CONTRACT_NUMBER'
                                                                             and (
                                                                                 lower(innerSC.contractNumber) like :prompt
                                                                             )
                                                                         )
                                                                         or (
                                                                             :searchBy = 'CUSTOMER_NAME'
                                                                             and exists (
                                                                                 select 1 from Customer innerCustomer
                                                                                 join CustomerDetails innerCustomerDetails on innerCustomerDetails.customerId = innerCustomer.id
                                                                                 where innerSCD.customerDetailId = innerCustomerDetails.id
                                                                                 and (
                                                                                     lower(innerCustomerDetails.name) like :prompt
                                                                                 )
                                                                             )
                                                                         )
                                                                         or (
                                                                             :searchBy = 'CUSTOMER_UIC_OR_PERSONAL_NUMBER'
                                                                             and exists (
                                                                                 select 1 from Customer innerCustomer
                                                                                 join CustomerDetails innerCustomerDetails on innerCustomerDetails.customerId = innerCustomer.id
                                                                                 where innerSCD.customerDetailId = innerCustomerDetails.id
                                                                                 and (
                                                                                     innerCustomer.identifier like :prompt
                                                                                 )
                                                                             )
                                                                         )
                                                                 )
                                                             )
                                                         )
                                                         where scd.startDate = (
                                                             select max(innerSCD.startDate)
                                                             from ServiceContractDetails innerSCD
                                                             where innerSCD.contractId = sc.id
                                                             and innerSCD.startDate <= current_date())
            """,
            countQuery =
                    """
                             select count(sc.id)
                                                         from ServiceContracts sc
                                                         join ServiceContractDetails scd on scd.contractId = sc.id
                                                         join CustomerDetails cd on cd.id = scd.customerDetailId
                                                         join Customer c on cd.customerId = c.id
                                                         left join LegalForm lf on lf.id = cd.legalFormId
                                                         join ServiceDetails sd on scd.serviceDetailId = sd.id
                                                         and sc.id in(
                                                             select innerSC.id
                                                             from ServiceContracts innerSC
                                                             join ServiceContractDetails innerSCD on innerSCD.contractId = innerSC.id
                                                             where (
                                                                 (:contractStatus) is null
                                                                 or innerSC.contractStatus in :contractStatus
                                                             )
                                                             and (
                                                                 (:contractSubStatus) is null
                                                                 or innerSC.subStatus in :contractSubStatus
                                                             )
                                                             and (
                                                                 (:types) is null
                                                                 or innerSCD.type in :types
                                                                 )
                                                             and (
                                                                 (
                                                                     cast(:terminationDateFrom as date) is null
                                                                     and cast(:terminationDateTo as date) is null
                                                                 )
                                                                 or (
                                                                     cast(:terminationDateFrom as date) is not null
                                                                     and cast(:terminationDateTo as date) is not null
                                                                     and innerSC.terminationDate between :terminationDateFrom and :terminationDateTo
                                                                 )
                                                                 or (
                                                                     cast(:terminationDateFrom as date) is not null
                                                                     and cast(:terminationDateTo as date) is null
                                                                     and innerSC.terminationDate >= :terminationDateFrom
                                                                 )
                                                                 or (
                                                                     cast(:terminationDateFrom as date) is null
                                                                     and cast(:terminationDateTo as date) is not null
                                                                     and innerSC.terminationDate <= :terminationDateTo
                                                                 )
                                                                 )
                                                             and (
                                                                 (
                                                                     cast(:perpetuityDateFrom as date) is null
                                                                     and cast(:perpetuityDateTo as date) is null
                                                                 )
                                                                 or (
                                                                     cast(:perpetuityDateFrom as date) is not null
                                                                     and cast(:perpetuityDateTo as date) is not null
                                                                     and innerSC.perpetuityDate between :perpetuityDateFrom and :perpetuityDateTo
                                                                 )
                                                                 or (
                                                                     cast(:perpetuityDateFrom as date) is not null
                                                                     and cast(:perpetuityDateTo as date) is null
                                                                     and innerSC.perpetuityDate >= :perpetuityDateFrom
                                                                 )
                                                                 or (
                                                                     cast(:perpetuityDateFrom as date) is null
                                                                     and cast(:perpetuityDateTo as date) is not null
                                                                     and innerSC.perpetuityDate <= :perpetuityDateTo
                                                                 )
                                                                 )
                                                             and (
                                                                   (:serviceIds) is null
                                                                    or (innerSCD.serviceDetailId in :serviceIds)
                                                                  )
                                                             and (
                                                                  innerSC.status in(:statuses)
                                                                  )
                                                             and(
                                                                 (
                                                                     (:accountManagerIds) is null
                                                                      or exists
                                                                         (
                                                                             select 1 from CustomerAccountManager cam
                                                                             where cam.customerDetail.id = innerSCD.customerDetailId
                                                                             and cam.status = 'ACTIVE'
                                                                             and cam.managerId in (:accountManagerIds)
                                                                         )
                                                                 )
                                                             )
                                                             and (coalesce(:excludeVersions, '0') = '0' or
                                                                                   (:excludeVersions = 'OLDVERSION' and
                                                                                    innerSCD.startDate >=
                                                                                    (select max(scd2.startDate)
                                                                                     from ServiceContractDetails scd2
                                                                                     where scd2.contractId = innerSC.id
                                                                                       and scd2.startDate <= current_date)
                                                                                       )
                                                                                or
                                                                                   (:excludeVersions = 'FUTUREVERSION' and
                                                                                    innerSCD.startDate <=
                                                                                    (select max(scd2.startDate)
                                                                                     from ServiceContractDetails scd2
                                                                                     where scd2.contractId = innerSC.id
                                                                                       and scd2.startDate <= current_date)
                                                                                       )
                                                                                or
                                                                                   (:excludeVersions = 'OLDANDFUTUREVERSION' and
                                                                                    innerSCD.startDate =
                                                                                    (select max(scd2.startDate)
                                                                                     from ServiceContractDetails scd2
                                                                                     where scd2.contractId = innerSC.id
                                                                                       and scd2.startDate <= current_date)
                                                                                       )
                                                                                )
                                                             and (
                                                                 :prompt is null
                                                                 or (
                                                                     :searchBy = 'ALL'
                                                                     and (
                                                                         lower(innerSC.contractNumber) like :prompt
                                                                         or exists (
                                                                             select 1 from Customer innerCustomer
                                                                             join CustomerDetails innerCustomerDetails on innerCustomerDetails.customerId = innerCustomer.id
                                                                             where innerSCD.customerDetailId = innerCustomerDetails.id
                                                                             and (
                                                                                 lower(innerCustomerDetails.name) like :prompt
                                                                                 or innerCustomer.identifier like :prompt
                                                                             )
                                                                         )
                                                                     )
                                                                 )
                                                                 or (
                                                                         (
                                                                             :searchBy = 'CONTRACT_NUMBER'
                                                                             and (
                                                                                 lower(innerSC.contractNumber) like :prompt
                                                                             )
                                                                         )
                                                                         or (
                                                                             :searchBy = 'CUSTOMER_NAME'
                                                                             and exists (
                                                                                 select 1 from Customer innerCustomer
                                                                                 join CustomerDetails innerCustomerDetails on innerCustomerDetails.customerId = innerCustomer.id
                                                                                 where innerSCD.customerDetailId = innerCustomerDetails.id
                                                                                 and (
                                                                                     lower(innerCustomerDetails.name) like :prompt
                                                                                 )
                                                                             )
                                                                         )
                                                                         or (
                                                                             :searchBy = 'CUSTOMER_UIC_OR_PERSONAL_NUMBER'
                                                                             and exists (
                                                                                 select 1 from Customer innerCustomer
                                                                                 join CustomerDetails innerCustomerDetails on innerCustomerDetails.customerId = innerCustomer.id
                                                                                 where innerSCD.customerDetailId = innerCustomerDetails.id
                                                                                 and (
                                                                                     innerCustomer.identifier like :prompt
                                                                                 )
                                                                             )
                                                                         )
                                                                 )
                                                             )
                                                         )
                                                         where scd.startDate = (
                                                             select max(innerSCD.startDate)
                                                             from ServiceContractDetails innerSCD
                                                             where innerSCD.contractId = sc.id
                                                             and innerSCD.startDate <= current_date())
                            """)
    Page<ServiceContractListingResponse> filter(
            @Param("prompt") String prompt,
            @Param("searchBy") String searchBy,
            @Param("terminationDateFrom") LocalDate dateOfTerminationFrom,
            @Param("terminationDateTo") LocalDate dateOfTerminationTo,
            @Param("perpetuityDateFrom") LocalDate perpetuityDateFrom,
            @Param("perpetuityDateTo") LocalDate perpetuityDateTo,
            @Param("contractStatus") List<ServiceContractDetailStatus> contractStatus,
            @Param("contractSubStatus") List<ServiceContractDetailsSubStatus> contractSubStatus,
            @Param("types") List<ServiceContractContractType> types,
            @Param("serviceIds") List<Long> serviceIds,
            @Param("accountManagerIds") List<Long> accountManagerIds,
            @Param("statuses") List<EntityStatus> statuses,
            @Param("excludeVersions") String excludeVersions,
            Pageable pageable
    );


    @Query("select sc.id from ServiceContracts sc where sc.status in (:statuses) and sc.id in (:ids)")
    List<Long> findByIdInAndStatusIn(List<Long> ids, List<EntityStatus> statuses);

    boolean existsByIdAndStatusIn(Long serviceContractId, List<EntityStatus> active);

    @Query("""
            select sc from ServiceContracts sc
            where sc.id in(:serviceContractIds)
            and sc.status in(:statuses)
            """)
    List<ServiceContracts> findAllByServiceContractIdsAndStatusIn(List<Long> serviceContractIds, List<EntityStatus> statuses);

    @Query("""
            select case when count(sc) > 0 then true else false end from ServiceContracts sc
            join ServiceContractDetails scd on scd.contractId = sc.id
            join ServiceDetails cd on cd.id = scd.serviceDetailId
            join EPService s on s.id = :id
            and sc.status = 'ACTIVE'
            """)
    boolean existsByServiceId(Long id);

    @Query("""
            select case when count(sc) > 0 then true else false end from ServiceContracts sc
            join ServiceContractDetails scd on scd.contractId = sc.id
            join ServiceDetails sd on sd.id = scd.serviceDetailId
            join EPService s on s.id = sd.service.id
            where s.id = :serviceId
            and scd.id<>:serviceContractDetailsId
            and sc.status = 'ACTIVE'
            """)
    boolean existsByServiceIdAndContractIdNotEquals(Long serviceId, Long serviceContractDetailsId);

    @Query(
            value = """
                    SELECT iap.id as id,
                            iap.name as name,
                            iap.value_type as valueType,
                            iap.value as value,
                            iap.value_from as valueFrom,
                            iap.value_to as valueTo,
                            iap.payment_type as paymentType,
                            iap.date_of_issue_type as dateOfissueType,
                            iap.date_of_issue_value as dateOfissueValue,
                            iap.date_of_issue_value_from as dateOfIssueValueFrom,
                            iap.date_of_issue_value_to as dateOfIssueValueTo,
                            iap.issuing_for_the_month_to_current as issuingForTheMonthToCurrent,
                            iap.deduction_from as deductionFrom,
                            iap.match_term_of_standard_invoice as matchTermOfStandardInvoice,
                            iap.no_interest_on_overdue_debts as noInterestOnOverdueDebts,
                            iap.status as status,
                            iap.iap_group_detail_id as groupDetailId
                           FROM service.service_interim_advance_payment_groups piapg
                           join
                           interim_advance_payment.interim_advance_payment_groups iapg
                           on
                           piapg.interim_advance_payment_group_id = iapg.id
                           join
                           interim_advance_payment.interim_advance_payment_group_details iapgd
                           on
                           iapgd.interim_advance_payment_group_id = iapg.id
                           join
                           interim_advance_payment.interim_advance_payments iap
                           on
                           iap.iap_group_detail_id =  iapgd.id
                           where piapg.service_detail_id = :id
                           and piapg.status = 'ACTIVE'
                           and iapg.status = 'ACTIVE'
                           and iap.status = 'ACTIVE'
                           and iapgd.start_date =
                           (select max(start_date) from interim_advance_payment.interim_advance_payment_group_details tt where tt.interim_advance_payment_group_id
                            = iapgd.interim_advance_payment_group_id
                           and start_date < now())
                        order by iap.id
                    """, nativeQuery = true
    )
    List<IapResponseFromNativeQuery> getIapsByServiceDetailIdAndCurrentDate(Long id);


    @Query(value = """
            SELECT
                pc.id as id,
                pc.name as name
            FROM service.service_price_component_groups ppcg
                     join
                 price_component.price_component_groups pcg
                 on
                         ppcg.price_component_group_id = pcg.id
                     join
                 price_component.price_component_group_details pcgd
                 on
                         pcgd.price_component_group_id = pcg.id
                     join
                 price_component.price_components  pc
                 on
                         pc.price_component_group_detail_id =  pcgd.id
            where
                    ppcg.service_detail_id = :id
              and
                    ppcg.status = 'ACTIVE'
              and pcg.status = 'ACTIVE'
              and pc.status = 'ACTIVE'
              and pcgd.start_date =
                  (select max(start_date) from price_component.price_component_group_details tt where tt.price_component_group_id
                      = pcgd.price_component_group_id and start_date < now())
            order by pc.id
            """, nativeQuery = true)
    List<PriceComponentForContractResponse> getPriceComponentFromServicePriceComponentGroups(Long id);

    @Query("""
            select count(pcrsc.id) > 0
            from ProductContractRelatedServiceContract pcrsc
            join ProductContract pc on (pc.id = pcrsc.productContractId and pcrsc.serviceContractId = :id)
            where pc.status = 'ACTIVE'
            and pcrsc.status = 'ACTIVE'
            """)
    boolean hasConnectionToProductContract(Long id);

    @Query("""
            select count(scrsc.id) > 0 from ServiceContractRelatedServiceContract scrsc
                where (
                        (scrsc.serviceContractId = :id or scrsc.relatedServiceContractId = :id)
                        and scrsc.status = 'ACTIVE'
                    )
                and exists (
                    select 1
                    from ServiceContracts as s1
                    where s1.id = scrsc.serviceContractId and s1.status = 'ACTIVE'
                )
                and exists (
                    select 1
                    from ServiceContracts as s2
                    where s2.id = scrsc.relatedServiceContractId and s2.status = 'ACTIVE'
                )
                    """)
    boolean hasConnectionToServiceContract(Long id);

    @Query("""
            select count(scrso.id) > 0
            from ServiceContractRelatedServiceOrder scrso
            join ServiceOrder so on (so.id = scrso.serviceOrderId and scrso.serviceContractId = :id)
            where scrso.status = 'ACTIVE'
            and so.status = 'ACTIVE'
            """)
    boolean hasConnectionToServiceOrder(Long id);

    @Query("""
            select count(solsc.id) > 0
            from ServiceOrderLinkedServiceContract solsc
            join ServiceOrder so on (so.id = solsc.orderId and solsc.contractId = :id)
            where solsc.status = 'ACTIVE'
            and so.status = 'ACTIVE'
            """)
    boolean isLinkedToServiceOrder(Long id);

    @Query("""
            select count(scrgo.id) > 0
            from ServiceContractRelatedGoodsOrder scrgo
            join GoodsOrder go on (go.id = scrgo.goodsOrderId and scrgo.serviceContractId = :id)
            where scrgo.status = 'ACTIVE'
            and go.status = 'ACTIVE'
            """)
    boolean hasConnectionToGoodsOrder(Long id);

    @Query("""
            select count(sct.id) > 0
            from ServiceContractTask sct
            join Task t on t.id = sct.taskId
            where sct.contractId = :id
            and sct.status = 'ACTIVE'
            and t.status = 'ACTIVE'
            """)
    boolean hasConnectionToTask(Long id);

    @Query("""
            select count(a.id) > 0
            from Action a
            where a.serviceContractId = :id
            and a.status = 'ACTIVE'
            """)
    boolean hasConnectionToAction(Long id);


    @Query(
            nativeQuery = true,
            value = """
                    select
                        tbl.contract_id as contractId,
                        tbl.action_id as actionId,
                        tbl.termination_id as terminationId,
                        tbl.notice_due as noticeDue,
                        tbl.action_execution_date as actionExecutionDate,
                        tbl.penalty_payer as actionPenaltyPayer,
                        tbl.action_type_id as actionTypeId,
                        tbl.auto_termination_from as autoTerminationFrom,
                        tbl.date_of_termination as contractTerminationDate
                    from (
                        select
                            c.id as contract_id,
                            ac.id as action_id,
                            t.id as termination_id,
                            t.notice_due,
                            ac.execution_date as action_execution_date,
                            ac.penalty_payer,
                            t.auto_termination_from,
                            ac.action_type_id,
                            case
                               when t.auto_termination_from = 'EVENT_DATE'
                                   then ac.execution_date
                               when t.auto_termination_from = 'FIRST_DAY_OF_MONTH_FOLLOWING_EVENT_DATE'
                                   then date_trunc('month', ac.execution_date +interval '1 month') end date_of_termination,
                            row_number() over (
                                partition by ac.service_contract_id order by
                                    case
                                        when t.auto_termination_from = 'EVENT_DATE'
                                            then ac.execution_date
                                        when t.auto_termination_from = 'FIRST_DAY_OF_MONTH_FOLLOWING_EVENT_DATE'
                                            then date_trunc('month', ac.execution_date + interval '1 month') end asc, ac.create_date
                            ) as priority
                        from service_contract.contracts c
                        join action.actions ac on ac.service_contract_id = c.id
                        join product.terminations t on ac.termination_id = t.id
                            where c.status = 'ACTIVE'
                            and c.contract_status in ('ENTERED_INTO_FORCE', 'ACTIVE_IN_TERM', 'ACTIVE_IN_PERPETUITY')
                            and ac.status = 'ACTIVE'
                            and t.status = 'ACTIVE'
                            and (
                                t.auto_termination_from = 'EVENT_DATE' and ac.execution_date <= current_date
                                or t.auto_termination_from = 'FIRST_DAY_OF_MONTH_FOLLOWING_EVENT_DATE' and
                                    date_trunc('month', ac.execution_date + interval '1 month') <= current_date
                            )
                    ) as tbl
                    where priority = 1
                    and ((:contractIdsToExclude) is null or tbl.contract_id not in :contractIdsToExclude)
                    limit :limit
                    """
    )
    List<ServiceContractTerminationWithActionsResponse> getEligibleServiceContractsForTerminationWithAction(
            @Param("limit") Integer limit,
            @Param("contractIdsToExclude") List<Long> contractIdsToExclude
    );


    @Query(nativeQuery = true,
            value = """
                    select c.id as contractId,
                           cd.id as detailId,
                           sct.automatic_renewal as automaticRenewal,
                           sct.perpetuity_clause as perpetuityCause,
                           sct.number_of_renewals as numberOfRenewals,
                           sct.renewal_period_type as renewalPeriodType,
                           sct.renewal_period_value as renewalValue,
                           sct.value as termValue,
                           sct.id as termId,
                           sct.contract_term_type termType,
                           -- sct.perpetuity_cause,
                           (select termination_id
                            from (select t.create_date crdate,
                                         st.termination_id as       termination_Id
                                  from service.service_terminations st
                                           join
                                       product.terminations t
                                       on st.termination_id = t.id
                                           join
                                       service.service_details sd2
                                       on st.service_detail_id = sd2.id
                                           join service_contract.contract_details cd3
                                                on cd3.service_detail_id = sd2.id
                                                    and cd3.contract_id = c.id
                                                    and
                                                   current_date between coalesce(sd2.available_From, current_date) and coalesce(sd2.available_To, current_date)
                                  where st.status = 'ACTIVE'
                                    and t.status = 'ACTIVE'
                                    and t.auto_termination = 'true'
                                    and t.event = 'EXPIRATION_OF_THE_CONTRACT_TERM'
                                  union
                                  select t2.create_date crdate, tgt.termination_id
                                  from service.service_termination_groups stg
                                           join
                                       product.termination_groups tg
                                       on stg.termination_group_id = tg.id
                                           join
                                       product.termination_group_details tgd
                                       on tgd.termination_group_id = tg.id
                                           and
                                          tgd.start_date = (select max(start_date)
                                                            from product.termination_group_details tgd3
                                                            where tgd3.termination_group_id = tg.id
                                                              and tgd3.start_date <= current_date)
                                           join
                                       product.termination_group_terminations tgt
                                       on tgt.termination_group_detail_id = tgd.id
                                           join
                                       product.terminations t2
                                       on tgt.termination_id = t2.id
                                           join
                                       service.service_details sd3
                                       on stg.service_detail_id = sd3.id
                                           join service_contract.contract_details cd3
                                                on cd3.service_detail_id = sd3.id
                                                    and cd3.contract_id = c.id
                                                 --   and
                                                 --  current_date between coalesce(sd3.available_From, current_date) and coalesce(sd3.available_To, current_date)
                                  where stg.status = 'ACTIVE'
                                    and tg.status = 'ACTIVE'
                                    and tgt.status = 'ACTIVE'
                                    and t2.status = 'ACTIVE'
                                    and t2.auto_termination = 'true'
                                    and t2.event = 'EXPIRATION_OF_THE_CONTRACT_TERM'
                                  order by crdate desc
                                  limit 1) as event) as terminationId
                    from service_contract.contracts c
                             join service_contract.contract_details cd
                                  on cd.contract_id = c.id
                             join
                         service.service_contract_terms sct--pct
                         on cd.service_contract_term_id = sct.id
                    where contract_status in ('ENTERED_INTO_FORCE', 'ACTIVE_IN_TERM')
                      and c.status = 'ACTIVE'
                      and cd.start_date = (select max(start_date)
                                           from service_contract.contract_details cd1
                                           where cd1.contract_id = c.id
                                             and cd1.start_date <= current_date)
                      and (c.contract_term_end_date is null or c.contract_term_end_date <= current_date)
                      and (not exists(select 1 from action.actions ac where ac.service_contract_id = c.id and ac.status = 'ACTIVE')
                        or
                           exists
                               (select 1
                                from action.actions ac
                                         join
                                     product.terminations t
                                     on ac.termination_id = t.id
                                where ac.service_contract_id = c.id
                                  and ac.status = 'ACTIVE'
                                  and t.status = 'ACTIVE'
                                  and (ac.execution_date > current_date
                                    or
                                       (ac.execution_date <= current_date
                                           and T.auto_termination_from = 'FIRST_DAY_OF_MONTH_FOLLOWING_EVENT_DATE')
                                           and date_trunc('month', ac.execution_date + interval '1 month') > current_date
                                    )
                               ))
                      and ((:contractIds) is null or contract_id not in :contractIds)
                    limit :limit
                    """)
    List<ServiceContractTerminationWithContractTermsResponse> getServiceContractsForTermDeactivation(
            @Param("limit") Integer limit,
            @Param("contractIds") List<Long> contractIds
    );


    @Query(value = """
            select new bg.energo.phoenix.model.response.contract.productContract.terminations.serviceContract.ServiceContractTerminationByTermsResponse(sc.id)
            from ServiceContracts sc
            join ServiceContractDetails scd on sc.id = scd.contractId
            where sc.status = 'ACTIVE'
            and sc.contractStatus = 'ENTERED_INTO_FORCE'
            and (:contractIds is null or sc.id not in (:contractIds))
            """)
    List<ServiceContractTerminationByTermsResponse> getEligibleProductContractsForTerminationByTerms(
            @Param("contractIds") List<Long> contractIds,
            PageRequest pageable
    );

    @Query("""
            select sc from ServiceContracts as sc
            where ((sc.contractStatus = 'SIGNED' and sc.subStatus in ('SIGNED_BY_BOTH_SIDES','SPECIAL_PROCESSES'))
                    or (sc.contractStatus = 'ENTERED_INTO_FORCE' and sc.subStatus = 'AWAITING_ACTIVATION'))
            and sc.status = 'ACTIVE'
            and (sc.entryIntoForceDate is not null and (sc.entryIntoForceDate < :date or sc.entryIntoForceDate = :date))
            """)
    List<ServiceContracts> getServiceContractsForStatusUpdateFromJob(@Param("date") LocalDate nowDate);

    @Query("""
            select sct
            from ServiceContracts s
                     join ServiceContractDetails sd
                          on sd.contractId = s.id
                              and s.status = 'ACTIVE'
                              and s.id = :contractId
                              and sd.id = :contractDetailId
                     join ServiceContractTerm sct
                          on sd.serviceContractTermId =  sct.id
                              and sct.status = 'ACTIVE'
                              and sct.perpetuityClause = true
            """)
    Optional<ServiceContractTerm> getServiceContractTermByContractIdAndDetailId(@Param("contractId") Long contractId,
                                                                                @Param("contractDetailId") Long contractDetailId);

    @Query("""
            select new bg.energo.phoenix.model.response.billing.billingRun.manualInvoice.ContractOrderShortResponse(sc.id, sc.contractNumber)
            from ServiceContracts sc
            where sc.id = :id
            and sc.status = :status
            """)
    Optional<ContractOrderShortResponse> findByIdAndStatus(@Param("id") Long id,
                                                           @Param("status") EntityStatus status);

    @Query(value = """
            SELECT
                pc.id as id,
                pc.name as name
            FROM service.service_price_component_groups ppcg
                     join
                 price_component.price_component_groups pcg
                 on
                         ppcg.price_component_group_id = pcg.id
                     join
                 price_component.price_component_group_details pcgd
                 on
                         pcgd.price_component_group_id = pcg.id
                     join
                 price_component.price_components  pc
                 on
                         pc.price_component_group_detail_id =  pcgd.id
            where
                    ppcg.service_detail_id = :id
              and
                    ppcg.status = 'ACTIVE'
              and pcg.status = 'ACTIVE'
              and pc.status = 'ACTIVE'
              and pcgd.start_date >=
                  (select max(start_date) from price_component.price_component_group_details tt where tt.price_component_group_id
                      = pcgd.price_component_group_id and start_date <= current_date)
            order by pc.id
            """, nativeQuery = true)
    List<PriceComponentForContractResponse> getPriceComponentFromServicePriceComponentCurrentAndFutureGroups(
            @Param("id") Long id
    );


    @Query("""
                select new bg.energo.phoenix.model.response.contract.productContract.ContractWithStatusShortResponse
                (sc.id, cast(sc.contractStatus as string), cast(sc.subStatus as string))
                from ServiceContracts sc
                where sc.id = :id
                and sc.status = 'ACTIVE'
            """)
    Optional<ContractWithStatusShortResponse> getServiceContractWithStatus(@Param("id") Long id);

    @Query("""
            select count(lsc.id) > 0
                 from ContractLinkedServiceContract as lsc
                          join ServiceContracts as con
                               on con.id = lsc.contractId
                 where lsc.linkedServiceContractId = :id
                   and lsc.status = 'ACTIVE'
                   and con.status ='ACTIVE'
                    """)
    boolean isLinkedToServiceContract(Long id);

    @Query(value = """
            select count(pricComAppMode.id) > 0
                                       from service_contract.contract_details serCondet
                                                join service.service_details serDet
                                                     on serDet.id = serCondet.service_detail_id
                                                join service.service_price_components serPriceCom
                                                     on serDet.id = serPriceCom.service_detail_id
                                                join price_component.price_components priceCom
                                                     on priceCom.id = serPriceCom.price_component_id
                                                join price_component.application_models pricComAppMode
                                                     on priceCom.id = pricComAppMode.price_component_id
                                       where serCondet.id = :serviceContractDetailId
                                         and pricComAppMode.application_model_type = 'PRICE_AM_PER_PIECE'
                """, nativeQuery = true)
    boolean isServiceFromContractMappedToPerPiecePriceComponent(Long serviceContractDetailId);

    @Query("""
            select  
            customer.identifier as customerIdentifier,
            customerDetails.versionId as customerVersionId,
            contractDetails.id as serviceContractDetailId
            from ServiceContracts contract
                    join ServiceContractDetails contractDetails on contract.id = contractDetails.contractId
                    join CustomerDetails customerDetails on contractDetails.customerDetailId = customerDetails.id
                    join Customer customer on customerDetails.customerId = customer.id
            where contract.contractNumber = (:contractNumber) and contractDetails.versionId = (:contractVersionId)
            """
    )
    MassCommunicationFileProcessedResultProjection findCustomerIdentifierAndVersionIdByContractNumberAndContractVersionId(
            @Param("contractNumber") String contractNumber,
            @Param("contractVersionId") Long contractVersionId
    );

    @Query("""
            select pcd.versionId from ServiceContractDetails pcd
            join ServiceContracts serviceContract on serviceContract.contractNumber = :contractNumber
            where pcd.startDate = (
                select max(innerPCD.startDate) from ServiceContractDetails innerPCD
                where innerPCD.contractId = serviceContract.id
            )
            and pcd.contractId = serviceContract.id
            """
    )
    Long findLatestServiceContractDetailVersionId(String contractNumber);

    @Query("""
            select scd
            from ServiceContractDetails scd
            where scd.contractId = :contractId
            """)
    List<ServiceContractDetails> findServiceContractDetailsByContractId(Long contractId);

    @Query(nativeQuery = true, value = """
            with cc_address_formatter as (select cc.id,
                                                 case
                                                     when cc.foreign_address = true then cc.populated_place_foreign
                                                     else pp.name end                                           as populated_place,
                                                 case
                                                     when cc.foreign_address = true then cc.zip_code_foreign
                                                     else zc.zip_code end                                       as zip_code,
                                                 case
                                                     when cc.foreign_address = true then cc.district_foreign
                                                     else distr.name end                                        as district,
                                                 case
                                                     when cc.foreign_address = true then
                                                         replace(text(cc.foreign_residential_area_type), '_', ' ')
                                                     else replace(text(cc.residential_area_type), '_', ' ') end as ra_type,
                                                 case
                                                     when cc.foreign_address = true then cc.residential_area_foreign
                                                     else ra.name end                                           as ra_name,
                                                 case
                                                     when cc.foreign_address = true then
                                                         cc.foreign_street_type
                                                     else cc.street_type end                                    as street_type,
                                                 case
                                                     when cc.foreign_address = true then cc.street_foreign
                                                     else str.name end                                          as street,
                                                 cc.street_number,
                                                 cc.block,
                                                 cc.entrance,
                                                 cc.floor,
                                                 cc.apartment,
                                                 cc.address_additional_info,
                                                 case
                                                     when cc.foreign_address = false then
                                                         concat_ws(', ',
                                                                   nullif(distr.name, ''),
                                                                   nullif(concat_ws(' ',
                                                                                    replace(text(cc.residential_area_type), '_', ' '),
                                                                                    ra.name), ''),
                                                                   nullif(
                                                                           concat_ws(' ', cc.street_type, str.name, cc.street_number),
                                                                           ''),
                                                                   nullif(concat('бл. ', cc.block), 'бл. '),
                                                                   nullif(concat('вх. ', cc.entrance), 'вх. '),
                                                                   nullif(concat('ет. ', cc.floor), 'ет. '),
                                                                   nullif(concat('ап. ', cc.apartment), 'ап. '),
                                                                   cc.address_additional_info
                                                         )
                                                     else
                                                         concat_ws(', ',
                                                                   nullif(cc.district_foreign, ''),
                                                                   nullif(concat_ws(' ',
                                                                                    replace(text(cc.foreign_residential_area_type), '_', ' '),
                                                                                    cc.residential_area_foreign), ''),
                                                                   nullif(
                                                                           concat_ws(' ', cc.street_type, cc.street_foreign, cc.street_number),
                                                                           ''),
                                                                   nullif(concat('бл. ', cc.block), 'бл. '),
                                                                   nullif(concat('вх. ', cc.entrance), 'вх. '),
                                                                   nullif(concat('ет. ', cc.floor), 'ет. '),
                                                                   nullif(concat('ап. ', cc.apartment), 'ап. '),
                                                                   cc.address_additional_info
                                                         )
                                                     end                                                        as formatted_address
                                          from customer.customer_communications cc
                                                   left join nomenclature.districts distr on cc.district_id = distr.id
                                                   left join nomenclature.zip_codes zc on cc.zip_code_id = zc.id
                                                   left join nomenclature.residential_areas ra on cc.residential_area_id = ra.id
                                                   left join nomenclature.streets str on cc.street_id = str.id
                                                   left join nomenclature.populated_places pp on cc.populated_place_id = pp.id
                                                   join service_contract.contract_details scd
                                                        on scd.customer_detail_id = cc.customer_detail_id
                                          where scd.contract_id = :id
                                            and scd.version_id = :versionId),
                 contact_aggregator as (select cc.customer_communication_id,
                                               string_agg(distinct cc.contact_value, ', ')
                                               filter (where cc.contact_type = 'EMAIL')          as email_comb,
                                               string_agg(distinct cc.contact_value, ', ')
                                               filter (where cc.contact_type = 'MOBILE_NUMBER')  as mobile_comb,
                                               string_agg(distinct cc.contact_value, ', ')
                                               filter (where cc.contact_type = 'LANDLINE_PHONE') as phone_comb,
                                               array_agg(distinct cc.contact_value)
                                               filter (where cc.contact_type = 'EMAIL')          as email_array,
                                               array_agg(distinct cc.contact_value)
                                               filter (where cc.contact_type = 'MOBILE_NUMBER')  as mobile_array,
                                               array_agg(distinct cc.contact_value)
                                               filter (where cc.contact_type = 'LANDLINE_PHONE') as phone_array
                                        from customer.customer_communication_contacts cc
                                                 join customer.customer_communications ccc on cc.customer_communication_id = ccc.id
                                                 join service_contract.contract_details scd
                                                      on scd.customer_detail_id = ccc.customer_detail_id
                                        where scd.contract_id = :id
                                          and scd.version_id = :versionId
                                          and cc.status = 'ACTIVE'
                                        group by cc.customer_communication_id),
                 additional_params as (select ap.service_detail_id,
                                              coalesce(csap.label, ap.label) as label,
                                              coalesce(csap.value, ap.value) as value,
                                              ap.ordering_id
                                       from service.service_additional_params ap
                                                join service_contract.contract_details scd
                                                     on scd.service_detail_id = ap.service_detail_id
                                                left join service_contract.contract_service_additional_params csap
                                                          on ap.id = csap.service_additional_param_id and
                                                             csap.contract_detail_id = scd.id
                                       where scd.contract_id = :id
                                         and scd.version_id = :versionId),
                 banks as (select b.id, b.name, b.bic
                           from nomenclature.banks b)
            select cd.customer_name_comb                                                   as CustomerNameComb,
                   cd.customer_name_comb_trsl                                              as CustomerNameCombTrsl,
                   cd.customer_identifier                                                  as CustomerIdentifier,
                   cd.customer_number                                                      as CustomerNumber,
                   cd.customer_type                                                        as CustomerType,
                   translation.translate_text(
                           customer.formatted_address,
                           text('BULGARIAN')
                   )                                                                       as HeadquarterAddressComb,
                   customer.populated_place                                                as HeadquarterPopulatedPlace,
                   customer.zip_code                                                       as HeadquarterZip,
                   customer.district                                                       as HeadquarterDistrict,
                   translation.translate_text(customer.ra_type, text('BULGARIAN'))         as HeadquarterQuarterRaType,
                   customer.ra_type                                                        as HeadquarterQuarterRaTypeTrsl,
                   customer.ra_name                                                        as HeadquarterQuarterRaName,
                   translation.translate_text(text(customer.street_type),
                                              text('BULGARIAN'))                           as HeadquarterStrBlvdType,
                   customer.street_type                                                    as HeadquarterStrBlvdTypeTrsl,
                   customer.street                                                         as HeadquarterStrBlvdName,
                   customer.street_number                                                  as HeadquarterStrBlvdNumber,
                   customer.block                                                          as HeadquarterBlock,
                   customer.entrance                                                       as HeadquarterEntrance,
                   customer.floor                                                          as HeadquarterFloor,
                   customer.apartment                                                      as HeadquarterApartment,
                   customer.address_additional_info                                        as HeadquarterAdditionalInfo,

                   translation.translate_text(contr_cc.formatted_address,
                                              text('BULGARIAN'))                           as CommunicationAddressComb,
                   contr_cc.populated_place                                                as CommunicationPopulatedPlace,
                   contr_cc.zip_code                                                       as CommunicationZip,
                   contr_cc.district                                                       as CommunicationDistrict,
                   translation.translate_text(contr_cc.ra_type, text('BULGARIAN'))         as CommunicationQuarterRaType,
                   contr_cc.ra_type                                                        as CommunicationQuarterRaTypeTrsl,
                   contr_cc.ra_name                                                        as CommunicationQuarterRaName,
                   translation.translate_text(text(contr_cc.street_type),
                                              text('BULGARIAN'))                           as CommunicationStrBlvdType,
                   contr_cc.street_type                                                    as CommunicationStrBlvdTypeTrsl,
                   contr_cc.street                                                         as CommunicationStrBlvdName,
                   contr_cc.street_number                                                  as CommunicationStrBlvdNumber,
                   contr_cc.block                                                          as CommunicationBlock,
                   contr_cc.entrance                                                       as CommunicationEntrance,
                   contr_cc.floor                                                          as CommunicationFloor,
                   contr_cc.apartment                                                      as CommunicationApartment,
                   contr_cc.address_additional_info                                        as CommunicationAdditionalInfo,

                   contr_contacts.email_comb                                               as CommunicationEmailComb,
                   contr_contacts.mobile_comb                                              as CommunicationMobileComb,
                   contr_contacts.phone_comb                                               as CommunicationPhoneComb,
                   contr_contacts.email_array                                              as CommunicationEmail,
                   contr_contacts.mobile_array                                             as CommunicationMobile,
                   contr_contacts.phone_array                                              as CommunicationPhone,

                   translation.translate_text(bil_cc.formatted_address,
                                              text('BULGARIAN'))                           as BillingAddressComb,

                   bil_cc.populated_place                                                  as BillingPopulatedPlace,
                   bil_cc.zip_code                                                         as BillingZip,
                   bil_cc.district                                                         as BillingDistrict,
                   translation.translate_text(bil_cc.ra_type, text('BULGARIAN'))           as BillingQuarterRaType,
                   bil_cc.ra_type                                                          as BillingQuarterRaTypeTrsl,
                   bil_cc.ra_name                                                          as BillingQuarterRaName,
                   translation.translate_text(text(bil_cc.street_type),
                                              text('BULGARIAN'))                           as BillingStrBlvdType,
                   bil_cc.street_type                                                      as BillingStrBlvdTypeTrsl,
                   bil_cc.street                                                           as BillingStrBlvdName,
                   bil_cc.street_number                                                    as BillingStrBlvdNumber,
                   bil_cc.block                                                            as BillingBlock,
                   bil_cc.entrance                                                         as BillingEntrance,
                   bil_cc.floor                                                            as BillingFloor,
                   bil_cc.apartment                                                        as BillingApartment,
                   bil_cc.address_additional_info                                          as BillingAdditionalInfo,

                   bil_contacts.email_comb                                                 as BillingEmailComb,
                   bil_contacts.mobile_comb                                                as BillingMobileComb,
                   bil_contacts.phone_comb                                                 as BillingPhoneComb,
                   bil_contacts.email_array                                                as BillingEmail,
                   bil_contacts.mobile_array                                               as BillingMobile,
                   bil_contacts.phone_array                                                as BillingPhone,

                   seg.customer_segments                                                   as CustomerSegments,

                   service_det.name                                                        as ProductName,
                   service_det.name_transl                                                 as ProductNameTrsl,
                   service_det.printing_name                                               as ProductPrintName,
                   service_det.printing_name_transl                                        as ProductPrintNameTrsl,
                   service_det.invoice_and_templates_text                                  as TextInvoicesTemplates,
                   service_det.invoice_and_templates_text_transl                           as TextInvoicesTemplatesTrsl,
                   translation.translate_text(replace(text(scd.payment_guarantee), '_', ' '),
                                              text('BULGARIAN'))                           as PaymentGuaranteeType,

                   case
                       when text(scd.payment_guarantee) in ('CASH_DEPOSIT', 'CASH_DEPOSIT_AND_BANK') then
                           scd.cash_deposit_amount end                                     as DepositAmount,
                   case
                       when text(scd.payment_guarantee) in ('CASH_DEPOSIT', 'CASH_DEPOSIT_AND_BANK') then
                           deposit_curr.print_name end                                     as DepositCurrencyPrintName,
                   case
                       when text(scd.payment_guarantee) in ('CASH_DEPOSIT', 'CASH_DEPOSIT_AND_BANK') then
                           deposit_curr.abbreviation end                                   as DepositCurrencyAbr,
                   case
                       when text(scd.payment_guarantee) in ('CASH_DEPOSIT', 'CASH_DEPOSIT_AND_BANK') then
                           deposit_curr.full_name end                                      as DepositCurrencyFullName,

                   case
                       when text(scd.payment_guarantee) in ('BANK', 'CASH_DEPOSIT_AND_BANK') then
                           scd.bank_guarantee_amount end                                   as BankGuaranteeAmount,
                   case
                       when text(scd.payment_guarantee) in ('BANK', 'CASH_DEPOSIT_AND_BANK') then
                           b_g_curr.print_name end                                         as BankGuaranteeCurrencyPrintName,
                   case
                       when text(scd.payment_guarantee) in ('BANK', 'CASH_DEPOSIT_AND_BANK') then
                           b_g_curr.abbreviation end                                       as BankGuaranteeCurrencyAbr,
                   case
                       when text(scd.payment_guarantee) in ('BANK', 'CASH_DEPOSIT_AND_BANK') then
                           b_g_curr.full_name end                                          as BankGuaranteeCurrencyFullName,

                   service_det.additional_info1                                            as AdditionalField1,
                   service_det.additional_info2                                            as AdditionalField2,
                   service_det.additional_info3                                            as AdditionalField3,
                   service_det.additional_info4                                            as AdditionalField4,
                   service_det.additional_info5                                            as AdditionalField5,
                   service_det.additional_info6                                            as AdditionalField6,
                   service_det.additional_info7                                            as AdditionalField7,
                   service_det.additional_info8                                            as AdditionalField8,
                   service_det.additional_info9                                            as AdditionalField9,
                   service_det.additional_info10                                           as AdditionalField10,

                   ap_1.label                                                              as AdditionalParametersLabel1,
                   ap_1.value                                                              as AdditionalParametersValue1,
                   ap_2.label                                                              as AdditionalParametersLabel2,
                   ap_2.value                                                              as AdditionalParametersValue2,
                   ap_3.label                                                              as AdditionalParametersLabel3,
                   ap_3.value                                                              as AdditionalParametersValue3,
                   ap_4.label                                                              as AdditionalParametersLabel4,
                   ap_4.value                                                              as AdditionalParametersValue4,
                   ap_5.label                                                              as AdditionalParametersLabel5,
                   ap_5.value                                                              as AdditionalParametersValue5,
                   ap_6.label                                                              as AdditionalParametersLabel6,
                   ap_6.value                                                              as AdditionalParametersValue6,
                   ap_7.label                                                              as AdditionalParametersLabel7,
                   ap_7.value                                                              as AdditionalParametersValue7,
                   ap_8.label                                                              as AdditionalParametersLabel8,
                   ap_8.value                                                              as AdditionalParametersValue8,
                   ap_9.label                                                              as AdditionalParametersLabel9,
                   ap_9.value                                                              as AdditionalParametersValue9,
                   ap_10.label                                                             as AdditionalParametersLabel10,
                   ap_10.value                                                             as AdditionalParametersValue10,
                   ap_11.label                                                             as AdditionalParametersLabel11,
                   ap_11.value                                                             as AdditionalParametersValue11,
                   ap_12.label                                                             as AdditionalParametersLabel12,
                   ap_12.value                                                             as AdditionalParametersValue12,

                   replace(text(scd.type), '_', ' ')                                       as ContractDocumentType,

                   cvt.agg_names                                                           as ContractVersionType,

                   sc.contract_number                                                      as ContractNumber,
                   case
                       when scd.additional_agreement_suffix is not null then
                           '#' || text(scd.additional_agreement_suffix) end                as AdditionalSuffix,
                   cast(sc.create_date as date)                                            as CreationDate,
                   sc.signing_date                                                         as SigningDate,
                   sc.entry_into_force_date                                                as EntryForceDate,
                   sc.initial_term_start_date                                              as ContractTermStartDate,
                   scd.start_date                                                          as VersionStartDate,
                   ir.name                                                                 as ApplicableInterestRate,
                   camp.name                                                               as Campaign,
                   b.name                                                                  as ContractBank,
                   b.bic                                                                   as ContractBIC,
                   scd.iban                                                                as ContractIBAN,
                   cust_bank.name                                                          as CustomerBank,
                   cust_bank.bic                                                           as CustomerBIC,
                   cd.iban                                                                 as CustomerIBAN,

                   employee.display_name || ' (' || employee.user_name || ')'              as Employee,
                   assistant.name                                                          as AssistingEmployee,
                   intermed.name                                                           as InternalIntermediary,
                   ext.name                                                                as ExternalIntermediary,

                   replace(text(contr_terms.contract_term_type), '_', '/')                 as ContractTermType,
                   contr_terms.value                                                       as ContractTermValue,
                   replace(text(contr_terms.contract_term_period_type), '_', ' ')          as ContractTermValueType,
                   case when contr_terms.perpetuity_clause = true then 'YES' else 'NO' end as ContractTermPerpetuity,
                   text(contr_terms.number_of_renewals)                                    as ContractTermRenewal,
                   text(contr_terms.renewal_period_value)                                  as ContractTermRenewalValue,
                   replace(text(contr_terms.renewal_period_type), '_', '/')                as ContractTermRenewalType,
                   replace(text(payment_terms.type), '_', ' ')                             as PaymentTermType,
                   text(scd.invoice_payment_term_value)                                    as PaymentTermValue,
                   text(terms.contract_delivery_activation_value)                          as TermActivationDeliveryValue,
                   replace(text(terms.contract_delivery_activation_type), '_', ' ')        as TermActivationDeliveryType,
                   text(terms.resigning_deadline_value)                                    as DeadlineEarlyResigningValue,
                   replace(text(terms.resigning_deadline_type), '_', ' ')                  as DeadlineEarlyResigningType,
                   text(terms.general_notice_period_value)                                 as GeneralNoticePeriodValue,
                   replace(text(terms.general_notice_period_type), '_', ' ')               as GeneralNoticePeriodType,
                   text(terms.notice_term_period_value)                                    as NoticeTermValue,
                   replace(text(terms.notice_term_period_type), '_', ' ')                  as NoticeTermType,
                   text(terms.notice_term_disconnection_period_value)                      as NoticeTermDisconnectionValue,
                   replace(text(terms.notice_term_disconnection_period_type), '_',
                           ' ')                                                            as NoticeTermDisconnectionType,
                   replace(text(scd.entry_into_force), '_', ' ')                           as EntryIntoForceType,
                   text(sc.entry_into_force_date)                                          as EntryIntoForceValue,
                   replace(text(scd.start_initial_term), '_', ' ')                         as StartInitialTermType,
                   text(sc.initial_term_start_date)                                        as StartInitialTermValue,
                   iap.yn                                                                  as InterimAdvancePaymentsYN,
                   iap.interims                                                            as InterimAdvancePaymentsList,
                   pods.pods                                                               as ServicePODs,
                   rsc.contracts                                                           as ServiceContracts,
                   scd.quantity                                                            as Quantity
            from service_contract.contract_details scd
                     join service_contract.contracts sc on scd.contract_id = sc.id
                     join lateral (select replace(text(c.customer_type), '_', ' '),
                                          cd.*,
                                          lf.name                                       as legal_form_name,
                                          case
                                              when c.customer_type = 'PRIVATE_CUSTOMER'
                                                  then concat(cd.name, ' ', cd.middle_name, ' ', cd.last_name)
                                              else concat(cd.name, ' ', lf.name)
                                              end                                       as customer_name_comb,
                                          case
                                              when c.customer_type = 'PRIVATE_CUSTOMER'
                                                  then concat(cd.name_transl, ' ', cd.middle_name_transl, ' ', cd.last_name_transl)
                                              else concat(cd.name_transl, ' ', lf.name)
                                              end                                       as customer_name_comb_trsl,
                                          c.identifier                                  as customer_identifier,
                                          c.customer_number,
                                          translation.translate_text(replace(text(c.customer_type), '_', ' '),
                                                                     text('BULGARIAN')) as customer_type
                                   from customer.customer_details cd
                                            join customer.customers c on cd.customer_id = c.id
                                            left join nomenclature.legal_forms lf on cd.legal_form_id = lf.id
                                   where scd.customer_detail_id = cd.id) cd on true
                     join service.service_details service_det on scd.service_detail_id = service_det.id
                     join service.services p on service_det.service_id = p.id
                     left join lateral (select cd.id,
                                               case
                                                   when cd.foreign_address = true then cd.populated_place_foreign
                                                   else pp.name end                                          as populated_place,
                                               case
                                                   when cd.foreign_address = true then cd.zip_code_foreign
                                                   else zc.zip_code end                                      as zip_code,
                                               case
                                                   when cd.foreign_address = true then cd.district_foreign
                                                   else distr.name end                                       as district,
                                               case
                                                   when cd.foreign_address = true
                                                       then replace(text(cd.foreign_residential_area_type), '_', ' ')
                                                   else
                                                       replace(text(cd.residential_area_type), '_', ' ') end as ra_type,
                                               case
                                                   when cd.foreign_address = true then cd.residential_area_foreign
                                                   else ra.name end                                          as ra_name,
                                               case
                                                   when cd.foreign_address = true then cd.foreign_street_type
                                                   else cd.street_type end                                   as street_type,
                                               case
                                                   when cd.foreign_address = true then cd.street_foreign
                                                   else str.name end                                         as street,
                                               cd.street_number,
                                               cd.block,
                                               cd.entrance,
                                               cd.floor,
                                               cd.apartment,
                                               cd.address_additional_info,
                                               case
                                                   when cd.foreign_address = false then
                                                       concat_ws(', ',
                                                                 nullif(distr.name, ''),
                                                                 nullif(concat_ws(' ',
                                                                                  replace(text(cd.residential_area_type), '_', ' '),
                                                                                  ra.name), ''),
                                                                 nullif(
                                                                         concat_ws(' ', cd.street_type, str.name, cd.street_number),
                                                                         ''),
                                                                 nullif(concat('бл. ', cd.block), 'бл. '),
                                                                 nullif(concat('вх. ', cd.entrance), 'вх. '),
                                                                 nullif(concat('ет. ', cd.floor), 'ет. '),
                                                                 nullif(concat('ап. ', cd.apartment), 'ап. '),
                                                                 cd.address_additional_info
                                                       )
                                                   else
                                                       concat_ws(', ',
                                                                 nullif(cd.district_foreign, ''),
                                                                 nullif(concat_ws(' ',
                                                                                  replace(text(cd.foreign_residential_area_type), '_', ' '),
                                                                                  cd.residential_area_foreign), ''),
                                                                 nullif(
                                                                         concat_ws(' ', cd.foreign_street_type, cd.street_foreign,
                                                                                   cd.street_number),
                                                                         ''),
                                                                 nullif(concat('бл. ', cd.block), 'бл. '),
                                                                 nullif(concat('вх. ', cd.entrance), 'вх. '),
                                                                 nullif(concat('ет. ', cd.floor), 'ет. '),
                                                                 nullif(concat('ап. ', cd.apartment), 'ап. '),
                                                                 cd.address_additional_info
                                                       )
                                                   end                                                       as formatted_address
                                        from customer.customer_details cd
                                                 left join nomenclature.districts distr on cd.district_id = distr.id
                                                 left join nomenclature.zip_codes zc on cd.zip_code_id = zc.id
                                                 left join nomenclature.residential_areas ra on cd.residential_area_id = ra.id
                                                 left join nomenclature.streets str on cd.street_id = str.id
                                                 left join nomenclature.populated_places pp on cd.populated_place_id = pp.id
                                        where cd.id = scd.customer_detail_id) customer on true
                     left join cc_address_formatter contr_cc on contr_cc.id = scd.customer_communication_id_for_contract
                     left join cc_address_formatter bil_cc on bil_cc.id = scd.customer_communication_id_for_billing
                     left join contact_aggregator contr_contacts on contr_contacts.customer_communication_id = contr_cc.id
                     left join contact_aggregator bil_contacts on bil_contacts.customer_communication_id = bil_cc.id
                     left join lateral (select array_agg(distinct seg.name) filter (where seg.id is not null) as customer_segments
                                        from customer.customer_details cd
                                                 left join customer.customer_segments cs
                                                           on cd.id = cs.customer_detail_id and cs.status = 'ACTIVE'
                                                 join nomenclature.segments seg on cs.segment_id = seg.id
                                        where scd.customer_detail_id = cd.id) seg on true
                     left join nomenclature.currencies deposit_curr on scd.cash_deposit_currency_id = deposit_curr.id
                     left join nomenclature.currencies b_g_curr on scd.bank_guarantee_currency_id = b_g_curr.id
                     left join additional_params ap_1 on ap_1.ordering_id = 0
                     left join additional_params ap_2 on ap_2.ordering_id = 1
                     left join additional_params ap_3 on ap_3.ordering_id = 2
                     left join additional_params ap_4 on ap_4.ordering_id = 3
                     left join additional_params ap_5 on ap_5.ordering_id = 4
                     left join additional_params ap_6 on ap_6.ordering_id = 5
                     left join additional_params ap_7 on ap_7.ordering_id = 6
                     left join additional_params ap_8 on ap_8.ordering_id = 7
                     left join additional_params ap_9 on ap_9.ordering_id = 8
                     left join additional_params ap_10 on ap_10.ordering_id = 9
                     left join additional_params ap_11 on ap_11.ordering_id = 10
                     left join additional_params ap_12 on ap_12.ordering_id = 11
                     left join lateral (select string_agg(vt.name, ', ') as agg_names
                                        from service_contract.contract_version_types cvt
                                                 join nomenclature.contract_version_types vt
                                                      on cvt.contract_version_type_id = vt.id
                                        where cvt.status = 'ACTIVE'
                                          and cvt.contract_detail_id = scd.id) cvt on true
                     left join banks b on scd.bank_id = b.id
                     left join banks cust_bank on cd.bank_id = cust_bank.id
                     left join interest_rate.interest_rates ir on scd.applicable_interest_rate = ir.id
                     left join nomenclature.campaigns camp on scd.campaign_id = camp.id
                     left join customer.account_managers employee on scd.employee_id = employee.id
                     left join lateral (select string_agg(am.display_name || ' (' || am.user_name || ')', ';') as name
                                        from service_contract.contract_assisting_employees ae
                                                 join customer.account_managers am
                                                      on am.id = ae.account_manager_id
                                        where ae.status = 'ACTIVE'
                                          and ae.contract_detail_id = scd.id) assistant on true
                     left join lateral ( select string_agg(am.display_name || ' (' || am.user_name || ')', ';') as name
                                         from service_contract.contract_internal_intermediaries ae
                                                  join customer.account_managers am
                                                       on am.id = ae.account_manager_id
                                         where ae.status = 'ACTIVE'
                                           and ae.contract_detail_id = scd.id) intermed on true
                     left join lateral ( select array_agg(ei.name) as name
                                         from service_contract.contract_external_intermediaries ex
                                                  join nomenclature.external_intermediaries ei
                                                       on ex.external_intermediary_id = ei.id
                                         where ex.status = 'ACTIVE'
                                           and ex.contract_detail_id = scd.id) ext on true
                     left join service.service_contract_terms contr_terms on scd.service_contract_term_id = contr_terms.id
                     left join terms.invoice_payment_terms payment_terms on scd.invoice_payment_term_id = payment_terms.id
                     left join terms.terms terms on payment_terms.term_id = terms.id
                     left join lateral (select case when count(1) > 0 then 'YES' else 'NO' end as yn,
                                               array_agg(interim order by priority)            as interims
                                        from (select iap.name as interim,
                                                     1        as priority
                                              from service_contract.contract_interim_advance_payments ciap
                                                       join interim_advance_payment.interim_advance_payments iap
                                                            on ciap.interim_advance_payment_id = iap.id
                                              where ciap.status = 'ACTIVE'
                                                and ciap.contract_detail_id = scd.id
                                              union
                                              select iap.name as interim,
                                                     2        as priority
                                              from service.service_interim_advance_payment_groups piapg
                                                       join
                                                   interim_advance_payment.interim_advance_payment_groups iapg
                                                   on
                                                       piapg.interim_advance_payment_group_id = iapg.id
                                                       join
                                                   interim_advance_payment.interim_advance_payment_group_details iapgd
                                                   on
                                                       iapgd.interim_advance_payment_group_id = iapg.id
                                                       join
                                                   interim_advance_payment.interim_advance_payments iap
                                                   on
                                                       iap.iap_group_detail_id = iapgd.id
                                                       join nomenclature.currencies c on iap.currency_id = c.id
                                                       left join interim_advance_payment.interim_advance_payment_terms iapt
                                                                 on iap.id = iapt.interim_advance_payment_id and iapt.status = 'ACTIVE'
                                                       join service_contract.contract_details cd
                                                            on cd.service_detail_id = piapg.service_detail_id
                                                       join service_contract.contracts contr on cd.contract_id = contr.id
                                              where piapg.status = 'ACTIVE'
                                                and iapg.status = 'ACTIVE'
                                                and iap.status = 'ACTIVE'
                                                and iapgd.start_date =
                                                    (select max(start_date)
                                                     from interim_advance_payment.interim_advance_payment_group_details tt
                                                     where tt.interim_advance_payment_group_id
                                                         = iapgd.interim_advance_payment_group_id
                                                       and start_date < now())
                                                and contr.id = :id
                                                and cd.version_id = :versionId) as tbl) iap on true
                     left join lateral (select string_agg(ident, ', ') as pods
                                        from (select pod.identifier as ident
                                              from service_contract.contract_pods cp
                                                       join pod.pod pod on cp.pod_id = pod.id
                                                       join service_contract.contract_details cd on cp.contract_detail_id = cd.id
                                                       join service_contract.contracts c on cd.contract_id = c.id
                                              where cp.status = 'ACTIVE'
                                                and cp.contract_detail_id = scd.id
                                              union
                                              select cp.pod_identifier as ident
                                              from service_contract.contract_unrecognized_pods cp
                                                       join service_contract.contract_details cd on cp.contract_detail_id = cd.id
                                                       join service_contract.contracts c on cd.contract_id = c.id
                                              where cp.status = 'ACTIVE'
                                                and cp.contract_detail_id = scd.id) as tbl) pods on true
                     left join lateral ( select string_agg(sc_inner.contract_number || '/' ||
                                                           to_char(sc_inner.create_date, 'DD.MM.YYYY'),
                                                           ', ') as contracts
                                         from service_contract.contract_related_service_contracts rsc
                                                  join service_contract.contracts sc_inner
                                                       on rsc.contract_id = sc_inner.id
                                         where rsc.status = 'ACTIVE'
                                           and rsc.contract_id = sc.id) rsc on true
            where sc.id = :id
              and scd.version_id = :versionId
            """)
    ContractMainResponse fetchContractInfoForDocument(Long id, Long versionId);

    @Query("""
                    select count(scd.id)>0  from ServiceContracts sc
                    join ServiceContractDetails scd on scd.contractId=sc.id
                    join CustomerDetails cd on scd.customerDetailId=cd.id
                    join Customer c on cd.customerId=c.id
                    where c.id= :customerId
                    and sc.id= :contractId
                    and c.status='ACTIVE'
            """)
    boolean isContractAttachedToCustomer(@Param("customerId") Long customerId,
                                         @Param("contractId") Long contractId);

    @Query("""
                    select sc.id  from ServiceContracts sc
                    join ServiceContractDetails scd on scd.contractId=sc.id
                    join CustomerDetails cd on scd.customerDetailId=cd.id
                    join Customer c on cd.customerId=c.id
                    where c.id= :customerId
                    and scd.id= :contractDetailId
                    and c.status='ACTIVE'
            """)
    Long checkContractAttachedToCustomer(@Param("customerId") Long customerId,
                                         @Param("contractDetailId") Long contractDetailId);

    @Query("""
                    select sc from ServiceContracts sc
                    join ServiceContractDetails scd on scd.contractId=sc.id
                    join CustomerDetails cd on scd.customerDetailId=cd.id
                    join Customer c on cd.customerId=c.id
                    where c.id= :customerId
                    and scd.id= :contractDetailId
                    and c.status='ACTIVE'
            """)
    Optional<ServiceContracts> checkContractAttachedToCustomerAndFetchContractNumber(@Param("customerId") Long customerId,
                                                                                     @Param("contractDetailId") Long contractDetailId);

    @Query("""
                    select sc.id,sc.contractNumber from ServiceContracts sc
                    join ServiceContractDetails scd on scd.contractId=sc.id
                    where scd.id=:id
            """)
    List<Object[]> fetchServiceContractNumberById(Long id);

    @Query(nativeQuery = true, value = """
            with cc_address_formatter as (select cc.id,
                                                 case
                                                     when cc.foreign_address = true then cc.populated_place_foreign
                                                     else pp.name end                                           as populated_place,
                                                 case
                                                     when cc.foreign_address = true then cc.zip_code_foreign
                                                     else zc.zip_code end                                       as zip_code,
                                                 case
                                                     when cc.foreign_address = true then cc.district_foreign
                                                     else distr.name end                                        as district,
                                                 case
                                                     when cc.foreign_address = true then
                                                         replace(text(cc.foreign_residential_area_type), '_', ' ')
                                                     else replace(text(cc.residential_area_type), '_', ' ') end as ra_type,
                                                 case
                                                     when cc.foreign_address = true then cc.residential_area_foreign
                                                     else ra.name end                                           as ra_name,
                                                 case
                                                     when cc.foreign_address = true then
                                                         cc.foreign_street_type
                                                     else cc.street_type end                                    as street_type,
                                                 case
                                                     when cc.foreign_address = true then cc.street_foreign
                                                     else str.name end                                          as street,
                                                 cc.street_number,
                                                 cc.block,
                                                 cc.entrance,
                                                 cc.floor,
                                                 cc.apartment,
                                                 cc.address_additional_info,
                                                 case
                                                     when cc.foreign_address = false then
                                                         concat_ws(', ',
                                                                   nullif(distr.name, ''),
                                                                   nullif(concat_ws(' ',
                                                                                    replace(text(cc.residential_area_type), '_', ' '),
                                                                                    ra.name), ''),
                                                                   nullif(
                                                                           concat_ws(' ', cc.street_type, str.name, cc.street_number),
                                                                           ''),
                                                                   nullif(concat('бл. ', cc.block), 'бл. '),
                                                                   nullif(concat('вх. ', cc.entrance), 'вх. '),
                                                                   nullif(concat('ет. ', cc.floor), 'ет. '),
                                                                   nullif(concat('ап. ', cc.apartment), 'ап. '),
                                                                   cc.address_additional_info
                                                         )
                                                     else
                                                         concat_ws(', ',
                                                                   nullif(cc.district_foreign, ''),
                                                                   nullif(concat_ws(' ',
                                                                                    replace(text(cc.foreign_residential_area_type), '_', ' '),
                                                                                    cc.residential_area_foreign), ''),
                                                                   nullif(
                                                                           concat_ws(' ', cc.street_type, cc.street_foreign, cc.street_number),
                                                                           ''),
                                                                   nullif(concat('бл. ', cc.block), 'бл. '),
                                                                   nullif(concat('вх. ', cc.entrance), 'вх. '),
                                                                   nullif(concat('ет. ', cc.floor), 'ет. '),
                                                                   nullif(concat('ап. ', cc.apartment), 'ап. '),
                                                                   cc.address_additional_info
                                                         )
                                                     end                                                        as formatted_address
                                          from customer.customer_communications cc
                                                   left join nomenclature.districts distr on cc.district_id = distr.id
                                                   left join nomenclature.zip_codes zc on cc.zip_code_id = zc.id
                                                   left join nomenclature.residential_areas ra on cc.residential_area_id = ra.id
                                                   left join nomenclature.streets str on cc.street_id = str.id
                                                   left join nomenclature.populated_places pp on cc.populated_place_id = pp.id),
                 segment_info as (select cd.id                                                          as customer_detail_id,
                                         array_agg(distinct seg.name) filter (where seg.id is not null) as customer_segments
                                  from customer.customer_details cd
                                           left join customer.customer_segments cs
                                                     on cd.id = cs.customer_detail_id and cs.status = 'ACTIVE'
                                           join nomenclature.segments seg on cs.segment_id = seg.id
                                  group by cd.id),
                 cd_address_formatter as (select cd.id,
                                                 case
                                                     when cd.foreign_address = true then cd.populated_place_foreign
                                                     else pp.name end                                          as populated_place,
                                                 case
                                                     when cd.foreign_address = true then cd.zip_code_foreign
                                                     else zc.zip_code end                                      as zip_code,
                                                 case
                                                     when cd.foreign_address = true then cd.district_foreign
                                                     else distr.name end                                       as district,
                                                 case
                                                     when cd.foreign_address = true
                                                         then replace(text(cd.foreign_residential_area_type), '_', ' ')
                                                     else
                                                         replace(text(cd.residential_area_type), '_', ' ') end as ra_type,
                                                 case
                                                     when cd.foreign_address = true then cd.residential_area_foreign
                                                     else ra.name end                                          as ra_name,
                                                 case
                                                     when cd.foreign_address = true then cd.foreign_street_type
                                                     else cd.street_type end                                   as street_type,
                                                 case
                                                     when cd.foreign_address = true then cd.street_foreign
                                                     else str.name end                                         as street,
                                                 cd.street_number,
                                                 cd.block,
                                                 cd.entrance,
                                                 cd.floor,
                                                 cd.apartment,
                                                 cd.address_additional_info,
                                                 case
                                                     when cd.foreign_address = false then
                                                         concat_ws(', ',
                                                                   nullif(distr.name, ''),
                                                                   nullif(concat_ws(' ',
                                                                                    replace(text(cd.residential_area_type), '_', ' '),
                                                                                    ra.name), ''),
                                                                   nullif(
                                                                           concat_ws(' ', cd.street_type, str.name, cd.street_number),
                                                                           ''),
                                                                   nullif(concat('бл. ', cd.block), 'бл. '),
                                                                   nullif(concat('вх. ', cd.entrance), 'вх. '),
                                                                   nullif(concat('ет. ', cd.floor), 'ет. '),
                                                                   nullif(concat('ап. ', cd.apartment), 'ап. '),
                                                                   cd.address_additional_info
                                                         )
                                                     else
                                                         concat_ws(', ',
                                                                   nullif(cd.district_foreign, ''),
                                                                   nullif(concat_ws(' ',
                                                                                    replace(text(cd.foreign_residential_area_type), '_', ' '),
                                                                                    cd.residential_area_foreign), ''),
                                                                   nullif(
                                                                           concat_ws(' ', cd.foreign_street_type, cd.street_foreign,
                                                                                     cd.street_number),
                                                                           ''),
                                                                   nullif(concat('бл. ', cd.block), 'бл. '),
                                                                   nullif(concat('вх. ', cd.entrance), 'вх. '),
                                                                   nullif(concat('ет. ', cd.floor), 'ет. '),
                                                                   nullif(concat('ап. ', cd.apartment), 'ап. '),
                                                                   cd.address_additional_info
                                                         )
                                                     end                                                       as formatted_address
                                          from customer.customer_details cd
                                                   left join nomenclature.districts distr on cd.district_id = distr.id
                                                   left join nomenclature.zip_codes zc on cd.zip_code_id = zc.id
                                                   left join nomenclature.residential_areas ra on cd.residential_area_id = ra.id
                                                   left join nomenclature.streets str on cd.street_id = str.id
                                                   left join nomenclature.populated_places pp on cd.populated_place_id = pp.id),
                 customer_base as (select replace(text(c.customer_type), '_', ' '),
                                          cd.*,
                                          lf.name      as legal_form_name,
                                          case
                                              when c.customer_type = 'PRIVATE_CUSTOMER'
                                                  then concat(cd.name, ' ', cd.middle_name, ' ', cd.last_name)
                                              else concat(cd.name, ' ', lf.name)
                                              end      as customer_name_comb,
                                          case
                                              when c.customer_type = 'PRIVATE_CUSTOMER'
                                                  then concat(cd.name_transl, ' ', cd.middle_name_transl, ' ', cd.last_name_transl)
                                              else concat(cd.name_transl, ' ', lf.name)
                                              end      as customer_name_comb_trsl,
                                          c.identifier as customer_identifier,
                                          c.customer_number
                                   from customer.customer_details cd
                                            join customer.customers c on cd.customer_id = c.id
                                            left join nomenclature.legal_forms lf on cd.legal_form_id = lf.id)
            select cb.customer_name_comb                  as CustomerNameComb,
                   cb.customer_name_comb_trsl             as CustomerNameCombTrsl,
                   cb.customer_identifier                 as CustomerIdentifier,
                   cb.customer_number                     as CustomerNumber,
                   translation.translate_text(customer.formatted_address,text('BULGARIAN'))             as HeadquarterAddressComb,
                   customer.populated_place               as HeadquarterPopulatedPlace,
                   customer.zip_code                      as HeadquarterZip,
                   customer.district                      as HeadquarterDistrict,
                   translation.translate_text(customer.ra_type,text('BULGARIAN'))   as HeadquarterQuarterRaType,
                   customer.ra_name                       as HeadquarterQuarterRaName,
                   translation.translate_text(text(customer.street_type ),text('BULGARIAN'))                  as HeadquarterStrBlvdType,
                   customer.street                        as HeadquarterStrBlvdName,
                   customer.street_number                 as HeadquarterStrBlvdNumber,
                   customer.block                         as HeadquarterBlock,
                   customer.entrance                      as HeadquarterEntrance,
                   customer.floor                         as HeadquarterFloor,
                   customer.apartment                     as HeadquarterApartment,
                   customer.address_additional_info       as HeadquarterAdditionalInfo,
                        
                   translation.translate_text(contr_cc.formatted_address,text('BULGARIAN'))             as CommunicationAddressComb,
                   contr_cc.populated_place               as CommunicationPopulatedPlace,
                   contr_cc.zip_code                      as CommunicationZip,
                   contr_cc.district                      as CommunicationDistrict,
                   translation.translate_text(contr_cc.ra_type,text('BULGARIAN'))                      as CommunicationQuarterRaType,
                   contr_cc.ra_name                       as CommunicationQuarterRaName,
                   translation.translate_text(text(contr_cc.street_type ),text('BULGARIAN'))                  as CommunicationStrBlvdType,
                   contr_cc.street                        as CommunicationStrBlvdName,
                   contr_cc.street_number                 as CommunicationStrBlvdNumber,
                   contr_cc.block                         as CommunicationBlock,
                   contr_cc.entrance                      as CommunicationEntrance,
                   contr_cc.floor                         as CommunicationFloor,
                   contr_cc.apartment                     as CommunicationApartment,
                   contr_cc.address_additional_info       as CommunicationAdditionalInfo,
                   si.customer_segments                   as CustomerSegments,
                   sc.contract_number                     as ContractNumber,
                   cast(sc.create_date as date)           as ContractDate,
                   sd.printing_name                       as ContractProductName,
                   text(scd.type)                         as ContractType,
                   sc.termination_date                    as ContractTerminationDate,
                   sc.termination_date + interval '1 day' as ContractTerminationDatePlus1,
                   sc.termination_date                    as CalculatedTerminationDate,
                   sc.termination_date + interval '1 day' as CalculatedTerminationDatePlus1,
                   text(t.event)                          as EventType,
                   cb.id                                  as CustomerDetailId
            from service_contract.contract_details scd
                     join service_contract.contracts sc on scd.contract_id = sc.id
                     join service.service_details sd on scd.service_detail_id = sd.id
                     left join customer_base cb
                               on cb.id = scd.customer_detail_id
                     left join segment_info si on si.customer_detail_id = cb.id
                     left join cc_address_formatter contr_cc on contr_cc.id = scd.customer_communication_id_for_contract
                     left join cd_address_formatter customer on customer.id = cb.id
                     left join product.terminations t on t.id = :terminationId
            where scd.id = :contractDetailId
            """)
    TerminationEmailDocumentResponse fetchTerminationEmailResponse(Long contractDetailId, Long terminationId);
}
