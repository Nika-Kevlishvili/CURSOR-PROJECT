package bg.energo.phoenix.repository.contract.product;

import bg.energo.phoenix.model.CacheObject;
import bg.energo.phoenix.model.CacheObjectForDetails;
import bg.energo.phoenix.model.CacheObjectForLocalDate;
import bg.energo.phoenix.model.documentModels.contract.response.ContractMainResponse;
import bg.energo.phoenix.model.documentModels.termination.TerminationEmailDocumentResponse;
import bg.energo.phoenix.model.entity.contract.product.ProductContract;
import bg.energo.phoenix.model.entity.product.product.ProductContractTerms;
import bg.energo.phoenix.model.enums.contract.products.ContractDetailType;
import bg.energo.phoenix.model.enums.contract.products.ContractDetailsStatus;
import bg.energo.phoenix.model.enums.contract.products.ContractDetailsSubStatus;
import bg.energo.phoenix.model.enums.contract.products.ProductContractStatus;
import bg.energo.phoenix.model.request.contract.pod.ActivationFilteredModel;
import bg.energo.phoenix.model.request.contract.pod.ContractModelMassImport;
import bg.energo.phoenix.model.response.billing.billingRun.manualInvoice.ContractOrderShortResponse;
import bg.energo.phoenix.model.response.contract.action.ActionContractResponse;
import bg.energo.phoenix.model.response.contract.priceComponent.PriceComponentForContractResponse;
import bg.energo.phoenix.model.response.contract.productContract.ContractWithStatusShortResponse;
import bg.energo.phoenix.model.response.contract.productContract.FilteredContractOrderEntityResponse;
import bg.energo.phoenix.model.response.contract.productContract.ProductContractListingResponse;
import bg.energo.phoenix.model.response.contract.productContract.priceComponentFormula.IapResponseFromNativeQuery;
import bg.energo.phoenix.model.response.contract.productContract.terminations.ProductContractTerminationByTermsResponse;
import bg.energo.phoenix.model.response.contract.productContract.terminations.ProductContractTerminationWithActionsResponse;
import bg.energo.phoenix.model.response.contract.productContract.terminations.ProductContractTerminationWithContractTermsResponse;
import bg.energo.phoenix.model.response.contract.productContract.terminations.ProductContractTerminationWithPodsResponse;
import bg.energo.phoenix.model.response.crm.emailCommunication.MassCommunicationFileProcessedResultProjection;
import bg.energo.phoenix.model.response.customer.CustomerActiveContractResponse;
import bg.energo.phoenix.model.response.customer.CustomerContractOrderResponse;
import bg.energo.phoenix.service.billing.runs.models.BillingDataShortModelForScale;
import bg.energo.phoenix.service.billing.runs.models.BillingDataShortModelProfile;
import bg.energo.phoenix.service.billing.runs.models.BillingRunForVolumesModel;
import jakarta.persistence.QueryHint;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface ProductContractRepository extends JpaRepository<ProductContract, Long> {

    @Query(value = "select nextval('product_contract.contract_number_seq')", nativeQuery = true)
    String getNextSequenceValue();

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
                           FROM product.product_interim_advance_payment_groups piapg
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
                           where piapg.product_detail_id = :productDetailId
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
    List<IapResponseFromNativeQuery> getIapSByProductDetailIdAndCurrentDate(@Param("productDetailId") Long productDetailId);


    @Query(value = """
            select
            pc.id as id,
            pc.name as name
            from product.product_price_component_groups ppcg
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
                    ppcg.product_detail_id = :productDetailId
              and
                    ppcg.status = 'ACTIVE'
              and pcg.status = 'ACTIVE'
              and pc.status = 'ACTIVE'
              and pcgd.start_date =
                  (select max(start_date) from price_component.price_component_group_details tt where tt.price_component_group_id
                      = pcgd.price_component_group_id and start_date < now())
            order by pc.id
            """, nativeQuery = true)
    List<PriceComponentForContractResponse> getPriceComponentFromProductPriceComponentGroups(@Param("productDetailId") Long productDetailId);


    boolean existsByIdAndStatusIn(Long id, List<ProductContractStatus> statuses);

    boolean existsByIdAndStatusAndContractStatusNotIn(Long id, ProductContractStatus status, List<ContractDetailsStatus> contractStatuses);

    Optional<ProductContract> findByIdAndStatusIn(Long id, List<ProductContractStatus> statuses);

    Optional<ProductContract> findByIdAndStatusAndContractStatusIsNotIn(Long id, ProductContractStatus status, List<ContractDetailsStatus> contractStatus);

    Optional<ProductContract> findByContractNumberAndStatus(String contractNumber, ProductContractStatus status);


    @Query(
            value = """
                    select count(pc.id) > 0 from ProductContract pc
                    left join ProductContractActivity pca on pca.contractId = pc.id
                    join SystemActivity sa on sa.id = pca.systemActivityId
                        where pc.id = :id
                        and pc.status = 'ACTIVE'
                        and pca.status = 'ACTIVE'
                        and sa.status = 'ACTIVE'
                    """
    )
    boolean hasConnectionToActivity(Long id);

    @Query(value = """
            select new bg.energo.phoenix.model.response.contract.productContract.ProductContractListingResponse(
                            pc.id,
                            pc.locked,
                            pc.contractNumber,
                            (
                                CASE
                                    WHEN (c.customerType = 'LEGAL_ENTITY')
                                    THEN CONCAT(cd.name, ' ', lf.name, ' (', c.identifier, ')')
                                    ELSE CONCAT(cd.name, ' ',
                                                CASE WHEN cd.middleName IS NOT NULL THEN CONCAT(cd.middleName, ' ') ELSE '' END,
                                                cd.lastName, ' (', c.identifier, ')')
                                END
                            ),
                            pd.name,
                            pd.productType,
                            pc.signingDate,
                            pc.contractStatus,
                            pc.subStatus,
                            pc.activationDate,
                            pc.contractTermEndDate,
                            pc.perpetuityDate,
                            pc.status,
                            pc.createDate,
                            pcd.type,
                            pcd.agreementSuffix,
                            (case when exists (select 1 from Invoice inv where inv.productContractId = pc.id and inv.invoiceStatus = 'REAL') then true else false end)
                        )
            from ProductContract pc
                     join ProductContractDetails pcd on pcd.contractId = pc.id
                     join CustomerDetails cd on cd.id = pcd.customerDetailId
                     join Customer c on cd.customerId = c.id
                     LEFT JOIN LegalForm lf ON lf.id = cd.legalFormId
                     join ProductDetails pd on pcd.productDetailId = pd.id
                and pc.id in (select innerPC.id
                               from ProductContract innerPC
                                        join ProductContractDetails innerPCD on innerPCD.contractId = innerPC.id
                               where (
                                   (:contractStatus) is null
                                       or innerPC.contractStatus in :contractStatus
                                   )
                                 and (
                                   (:contractSubStatus) is null
                                       or innerPC.subStatus in :contractSubStatus
                                   )
                                 and (
                                   innerPC.status in (:statuses)
                                   )
                                 and (
                                   (:types) is null
                                       or innerPCD.type in :types
                                   )
                                 and (cast(:activationDateFrom as date) is null or innerPC.activationDate >= :activationDateFrom)
                                 and (cast(:activationDateTo as date) is null or innerPC.activationDate <= :activationDateTo)
                                 and (cast(:dateOfEntryIntoPerpetuityFrom as date) is null or innerPC.perpetuityDate >= :dateOfEntryIntoPerpetuityFrom)
                                 and (cast(:dateOfEntryIntoPerpetuityTo as date) is null or innerPC.perpetuityDate <= :dateOfEntryIntoPerpetuityTo)
                                 and (cast(:dateOfTerminationFrom as date) is null or innerPC.terminationDate >= :dateOfTerminationFrom)
                                 and (cast(:dateOfTerminationTo as date) is null or innerPC.terminationDate <= :dateOfTerminationTo)
                                 and (
                                   (
                                       (:productIds) is null
                                           or (innerPCD.productDetailId in (:productIds))
                                       )
                                   )
                                 and (
                                   (
                                       (:accountManagerIds) is null
                                           or exists
                                           (select 1
                                            from CustomerAccountManager cam
                                            where cam.customerDetail.id = innerPCD.customerDetailId
                                              and cam.status = 'ACTIVE'
                                              and cam.managerId in (:accountManagerIds))
                                       )
                                   )
                                 and (coalesce(:excludeVersions, '0') = '0' or
                                      (:excludeVersions = 'OLDVERSION' and
                                       innerPCD.startDate >=
                                       (select max(pcd2.startDate)
                                        from ProductContractDetails pcd2
                                        where pcd2.contractId = innerPC.id
                                          and pcd2.startDate <= current_date)
                                          )
                                   or
                                      (:excludeVersions = 'FUTUREVERSION' and
                                       innerPCD.startDate <=
                                       (select max(pcd2.startDate)
                                        from ProductContractDetails pcd2
                                        where pcd2.contractId = innerPC.id
                                          and pcd2.startDate <= current_date)
                                          )
                                   or
                                      (:excludeVersions = 'OLDANDFUTUREVERSION' and
                                       innerPCD.startDate =
                                       (select max(pcd2.startDate)
                                        from ProductContractDetails pcd2
                                        where pcd2.contractId = innerPC.id
                                          and pcd2.startDate <= current_date)
                                          )
                                   )
                                 and (
                                   coalesce(:prompt, '') = ''
                                       or (
                                       :searchBy = 'ALL'
                                           and (
                                           lower(innerPC.contractNumber) like :prompt
                                               or exists (select 1
                                                          from Customer innerCustomer
                                                                   join CustomerDetails innerCustomerDetails
                                                                        on innerCustomerDetails.customerId = innerCustomer.id
                                                          where innerPCD.customerDetailId = innerCustomerDetails.id
                                                            and (
                                                              lower(innerCustomerDetails.name) like :prompt
                                                                  or lower(innerCustomer.identifier) like :prompt
                                                              ))
                                           )
                                       )
                                       or (
                                       (
                                           :searchBy = 'CONTRACT_NUMBER'
                                               and (
                                               lower(innerPC.contractNumber) like :prompt
                                               )
                                           )
                                           or (
                                           :searchBy = 'CUSTOMER_NAME'
                                               and exists (select 1
                                                           from Customer innerCustomer
                                                                    join CustomerDetails innerCustomerDetails
                                                                         on innerCustomerDetails.customerId = innerCustomer.id
                                                           where innerPCD.customerDetailId = innerCustomerDetails.id
                                                             and (
                                                               lower(innerCustomerDetails.name) like :prompt
                                                               ))
                                           )
                                           or (
                                           :searchBy = 'CUSTOMER_UIC_OR_PERSONAL_NUMBER'
                                               and exists (select 1
                                                           from Customer innerCustomer
                                                                    join CustomerDetails innerCustomerDetails
                                                                         on innerCustomerDetails.customerId = innerCustomer.id
                                                           where innerPCD.customerDetailId = innerCustomerDetails.id
                                                             and (
                                                               lower(innerCustomerDetails.name) like :prompt
                                                                   or lower(innerCustomer.identifier) like :prompt
                                                               ))
                                           )
                                       )
                                   ))
            where pcd.startDate = (select max(innerPCD.startDate)
                                   from ProductContractDetails innerPCD
                                   where innerPCD.contractId = pc.id
                                     and innerPCD.startDate <= current_date())
            """,
            countQuery =
                    """
                                            select count(pc.id)
                            from ProductContract pc
                                     join ProductContractDetails pcd on pcd.contractId = pc.id
                                     join CustomerDetails cd on cd.id = pcd.customerDetailId
                                     join Customer c on cd.customerId = c.id
                                     LEFT JOIN LegalForm lf ON lf.id = cd.legalFormId
                                     join ProductDetails pd on pcd.productDetailId = pd.id
                                and pc.id in (select innerPC.id
                                               from ProductContract innerPC
                                                        join ProductContractDetails innerPCD on innerPCD.contractId = innerPC.id
                                               where (
                                                   (:contractStatus) is null
                                                       or innerPC.contractStatus in :contractStatus
                                                   )
                                                 and (
                                                   (:contractSubStatus) is null
                                                       or innerPC.subStatus in :contractSubStatus
                                                   )
                                                 and (
                                                   innerPC.status in (:statuses)
                                                   )
                                                 and (
                                                   (:types) is null
                                                       or innerPCD.type in :types
                                                   )
                                                 and (cast(:activationDateFrom as date) is null or innerPC.activationDate >= :activationDateFrom)
                                                 and (cast(:activationDateTo as date) is null or innerPC.activationDate <= :activationDateTo)
                                                 and (cast(:dateOfEntryIntoPerpetuityFrom as date) is null or innerPC.perpetuityDate >= :dateOfEntryIntoPerpetuityFrom)
                                                 and (cast(:dateOfEntryIntoPerpetuityTo as date) is null or innerPC.perpetuityDate <= :dateOfEntryIntoPerpetuityTo)
                                                 and (cast(:dateOfTerminationFrom as date) is null or innerPC.terminationDate >= :dateOfTerminationFrom)
                                                 and (cast(:dateOfTerminationTo as date) is null or innerPC.terminationDate <= :dateOfTerminationTo)
                                                 and (
                                                   (
                                                       (:productIds) is null
                                                           or (innerPCD.productDetailId in (:productIds))
                                                       )
                                                   )
                                                 and (
                                                   (
                                                       (:accountManagerIds) is null
                                                           or exists
                                                           (select 1
                                                            from CustomerAccountManager cam
                                                            where cam.customerDetail.id = innerPCD.customerDetailId
                                                              and cam.status = 'ACTIVE'
                                                              and cam.managerId in (:accountManagerIds))
                                                       )
                                                   )
                                                 and (coalesce(:excludeVersions, '0') = '0' or
                                                      (:excludeVersions = 'OLDVERSION' and
                                                       innerPCD.startDate >=
                                                       (select max(pcd2.startDate)
                                                        from ProductContractDetails pcd2
                                                        where pcd2.contractId = innerPC.id
                                                          and pcd2.startDate <= current_date)
                                                          )
                                                   or
                                                      (:excludeVersions = 'FUTUREVERSION' and
                                                       innerPCD.startDate <=
                                                       (select max(pcd2.startDate)
                                                        from ProductContractDetails pcd2
                                                        where pcd2.contractId = innerPC.id
                                                          and pcd2.startDate <= current_date)
                                                          )
                                                   or
                                                      (:excludeVersions = 'OLDANDFUTUREVERSION' and
                                                       innerPCD.startDate =
                                                       (select max(pcd2.startDate)
                                                        from ProductContractDetails pcd2
                                                        where pcd2.contractId = innerPC.id
                                                          and pcd2.startDate <= current_date)
                                                          )
                                                   )
                                                 and (
                                                   coalesce(:prompt, '') = ''
                                                       or (
                                                       :searchBy = 'ALL'
                                                           and (
                                                           lower(innerPC.contractNumber) like :prompt
                                                               or exists (select 1
                                                                          from Customer innerCustomer
                                                                                   join CustomerDetails innerCustomerDetails
                                                                                        on innerCustomerDetails.customerId = innerCustomer.id
                                                                          where innerPCD.customerDetailId = innerCustomerDetails.id
                                                                            and (
                                                                              lower(innerCustomerDetails.name) like :prompt
                                                                                  or lower(innerCustomer.identifier) like :prompt
                                                                              ))
                                                           )
                                                       )
                                                       or (
                                                       (
                                                           :searchBy = 'CONTRACT_NUMBER'
                                                               and (
                                                               lower(innerPC.contractNumber) like :prompt
                                                               )
                                                           )
                                                           or (
                                                           :searchBy = 'CUSTOMER_NAME'
                                                               and exists (select 1
                                                                           from Customer innerCustomer
                                                                                    join CustomerDetails innerCustomerDetails
                                                                                         on innerCustomerDetails.customerId = innerCustomer.id
                                                                           where innerPCD.customerDetailId = innerCustomerDetails.id
                                                                             and (
                                                                               lower(innerCustomerDetails.name) like :prompt
                                                                               ))
                                                           )
                                                           or (
                                                           :searchBy = 'CUSTOMER_UIC_OR_PERSONAL_NUMBER'
                                                               and exists (select 1
                                                                           from Customer innerCustomer
                                                                                    join CustomerDetails innerCustomerDetails
                                                                                         on innerCustomerDetails.customerId = innerCustomer.id
                                                                           where innerPCD.customerDetailId = innerCustomerDetails.id
                                                                             and (
                                                                               lower(innerCustomerDetails.name) like :prompt
                                                                                   or lower(innerCustomer.identifier) like :prompt
                                                                               ))
                                                           )
                                                       )
                                                   ))
                            where pcd.startDate = (select max(innerPCD.startDate)
                                                   from ProductContractDetails innerPCD
                                                   where innerPCD.contractId = pc.id
                                                     and innerPCD.startDate <= current_date())
                                                """)
    Page<ProductContractListingResponse> filter(
            @Param("prompt") String prompt,
            @Param("searchBy") String searchBy,
            @Param("activationDateFrom") LocalDate activationDateFrom,
            @Param("activationDateTo") LocalDate activationDateTo,
            @Param("dateOfEntryIntoPerpetuityFrom") LocalDate dateOfEntryIntoPerpetuityFrom,
            @Param("dateOfEntryIntoPerpetuityTo") LocalDate dateOfEntryIntoPerpetuityTo,
            @Param("dateOfTerminationFrom") LocalDate dateOfTerminationFrom,
            @Param("dateOfTerminationTo") LocalDate dateOfTerminationTo,
            @Param("contractStatus") List<ContractDetailsStatus> contractStatus,
            @Param("contractSubStatus") List<ContractDetailsSubStatus> contractSubStatus,
            @Param("types") List<ContractDetailType> types,
            @Param("productIds") List<Long> productIds, // these are product detail ids
            @Param("accountManagerIds") List<Long> accountManagerIds,
            @Param("excludeVersions") String excludeVersions,
            @Param("statuses") List<ProductContractStatus> statuses,
            Pageable pageable
    );


    @Query("select pc.id from ProductContract pc where pc.status in :statuses and pc.id in :ids")
    List<Long> findByIdInAndStatusIn(List<Long> ids, List<ProductContractStatus> statuses);

    @Query(
            value = """
                    select pc.id from ProductContract pc
                        where pc.id in (:productContractIds)
                        and pc.status in (:statuses)
                    """
    )
    List<Long> findAllByIdInAndStatusIn(
            @Param("productContractIds") List<Long> productContractIds,
            @Param("statuses") List<ProductContractStatus> statuses
    );


    @Query(
            nativeQuery = true,
            value = """
                    select *
                    from (
                        select
                            c.contract_number as number,
                            case
                                when c1.customer_type = 'PRIVATE_CUSTOMER' then
                                    case
                                        when cd1.middle_name IS NOT NULL AND cd1.middle_name <> ''
                                            then cd1.name || ' ' || cd1.middle_name || ' ' || cd1.last_name || ' (' || c1.identifier || ')'
                                        else cd1.name || ' ' || cd1.last_name || ' (' || c1.identifier || ')'
                                    end
                                when c1.customer_type = 'LEGAL_ENTITY' then cd1.name || ' (' || c1.identifier || ')'
                            end as customerName,
                            'PRODUCT_CONTRACT' as type,
                            c.id
                        from product_contract.contracts c
                        join product_contract.contract_details cd on cd.contract_id = c.id
                        join customer.customer_details cd1 on cd.customer_detail_id = cd1.id
                        join customer.customers c1 on cd1.customer_id = c1.id
                        where (:prompt is null or lower(c.contract_number) like :prompt)
                          and c.status = 'ACTIVE'
                          and cd.start_date = (
                              select max(start_date)
                              from product_contract.contract_details cd3
                              where cd3.contract_id = c.id
                                and cd3.start_date <= current_date
                          )
                        union all
                        select
                            c.contract_number as number,
                            case
                                when c1.customer_type = 'PRIVATE_CUSTOMER' then
                                    case
                                        when cd1.middle_name IS NOT NULL AND cd1.middle_name <> ''
                                            then cd1.name || ' ' || cd1.middle_name || ' ' || cd1.last_name || ' (' || c1.identifier || ')'
                                        else cd1.name || ' ' || cd1.last_name || ' (' || c1.identifier || ')'
                                    end
                                when c1.customer_type = 'LEGAL_ENTITY' then cd1.name || ' (' || c1.identifier || ')'
                            end as customerName,
                            'SERVICE_CONTRACT' as type,
                            c.id
                        from service_contract.contracts c
                        join service_contract.contract_details cd on cd.contract_id = c.id
                        join customer.customer_details cd1 on cd.customer_detail_id = cd1.id
                        join customer.customers c1 on cd1.customer_id = c1.id
                        where (:prompt is null or lower(c.contract_number) like :prompt)
                          and c.status = 'ACTIVE'
                          and cd.start_date = (
                              select max(start_date)
                              from service_contract.contract_details cd3
                              where cd3.contract_id = c.id
                                and cd3.start_date <= current_date
                          )
                    ) as combined_result
                    order by customerName
                    """,
            countQuery = """
                    select count(*)
                    from (
                        select c.contract_number
                        from product_contract.contracts c
                        join product_contract.contract_details cd on cd.contract_id = c.id
                        join customer.customer_details cd1 on cd.customer_detail_id = cd1.id
                        join customer.customers c1 on cd1.customer_id = c1.id
                        where (:prompt is null or lower(c.contract_number) like :prompt)
                          and c.status = 'ACTIVE'
                          and cd.start_date = (
                              select max(start_date)
                              from product_contract.contract_details cd3
                              where cd3.contract_id = c.id
                                and cd3.start_date <= current_date
                          )
                        union all
                        select c.contract_number
                        from service_contract.contracts c
                        join service_contract.contract_details cd on cd.contract_id = c.id
                        join customer.customer_details cd1 on cd.customer_detail_id = cd1.id
                        join customer.customers c1 on cd1.customer_id = c1.id
                        where (:prompt is null or lower(c.contract_number) like :prompt)
                          and c.status = 'ACTIVE'
                          and cd.start_date = (
                              select max(start_date)
                              from service_contract.contract_details cd3
                              where cd3.contract_id = c.id
                                and cd3.start_date <= current_date
                          )
                    ) as count_result
                    """
    )
    Page<FilteredContractOrderEntityResponse> filterContracts(
            @Param("prompt") String prompt,
            Pageable pageable
    );

    @Query("""
            select case when count(pc) > 0 then true else false end from ProductContract pc
            join ProductContractDetails pcd on pcd.contractId = pc.id
            join ProductDetails  pd on pd.id=pcd.productDetailId
            join Product p on p.id=pd.product.id
            where p.id=:productId
            and pc.status = 'ACTIVE'
            """)
    boolean existsByProductId(Long productId);

    @Query("""
            select case when count(pc.id) > 0 then true else false end from ProductContract pc
            join ProductContractDetails pcd on pcd.contractId = pc.id
            join ProductDetails  pd on pd.id=pcd.productDetailId
            join Product p on p.id=pd.product.id
            where p.id=:productId
            and pcd.id<>:contractDetailsId
            and pc.status = 'ACTIVE'
            """)
    boolean existsByProductIdAndContractIdNotEquals(Long productId, Long contractDetailsId);


    @Query("""
            select
            new bg.energo.phoenix.model.request.contract.pod.ContractModelMassImport(c.id,c.signingDate,cd.startDate,cp.activationDate)
             from
            ProductContractDetails cd
            join ProductContract c on cd.contractId = c.id
            join ContractPods cp on cp.contractDetailId = cd.id
            join PointOfDeliveryDetails pd on cp.podDetailId = pd.id
            join PointOfDelivery p on pd.podId = p.id
            where c.status = 'ACTIVE'
              and cp.status = 'ACTIVE'
              and p.status = 'ACTIVE'
              and (c.contractStatus = 'SIGNED' and c.subStatus in ('SIGNED_BY_BOTH_SIDES','SPECIAL_PROCESSES')
                    or
                   c.contractStatus in ('ENTERED_INTO_FORCE','ACTIVE_IN_TERM','ACTIVE_IN_PERPETUITY')
                  )
              and p.id = :podId
              and cd.versionStatus = 'SIGNED'
            order by cd.startDate
                        """)
    List<ContractModelMassImport> getContractsForPodForActivation(Long podId);

    //    cd.id as contract_detail_id,
//    cd.start_date,
//    cp.activation_date,
//    cp.deactivation_date,
//    cp.pod_detail_id,
//    cd.contract_id,
//    p.id as podid
    @Query("""
                select
            new bg.energo.phoenix.model.request.contract.pod.ActivationFilteredModel(cd.id,c.id,cd.startDate,cp)
                 from
                ProductContractDetails cd
                join
                ProductContract c
                on cd.contractId = c.id
                join ContractPods cp on cp.contractDetailId = cd.id
                join PointOfDeliveryDetails pd on cp.podDetailId = pd.id
                join PointOfDelivery p on pd.podId = p.id
                where c.status = 'ACTIVE'
                  and cp.status = 'ACTIVE'
                  and p.status = 'ACTIVE'
                  and
                  (cd.startDate =
                   (select max(cd3.startDate) from ProductContractDetails cd3
                               where cd3.contractId = c.id
                        and cd3.startDate <= :startDate
                        and cd3.versionStatus = 'SIGNED')
                    or cd.startDate >= :startDate   
                  )
                  and c.id in (:contractId)
                  and p.id = :podId
                  and cd.versionStatus = 'SIGNED'
                order by cd.startDate
                                        """)
    List<ActivationFilteredModel> filterOtherVersionsForPod(Long podId, List<Long> contractId, LocalDate startDate);


    @Query(
            nativeQuery = true,
            value = """
                    select
                        distinct c.id,
                        c.contract_number as formattedNumber,
                        'PRODUCT_CONTRACT' as type
                    from product_contract.contracts c
                    join product_contract.contract_details cd on cd.contract_id =  c.id
                    join customer.customer_details cd1 on cd.customer_detail_id = cd1.id
                    join customer.customers c1 on cd1.customer_id = c1.id
                        where (:prompt is null or lower(c.contract_number) like :prompt)
                        and c.status = 'ACTIVE'
                        and c.contract_status in ('ENTERED_INTO_FORCE', 'ACTIVE_IN_TERM', 'ACTIVE_IN_PERPETUITY', 'TERMINATED')
                        and c1.id = :customerId
                        and c1.status = 'ACTIVE'
                    union
                    select
                        distinct c.id,
                        c.contract_number as formattedNumber,
                        'SERVICE_CONTRACT' as type
                    from service_contract.contracts c
                    join service_contract.contract_details cd on cd.contract_id = c.id
                    join customer.customer_details cd1 on cd.customer_detail_id = cd1.id
                    join customer.customers c1 on cd1.customer_id = c1.id
                        where (:prompt is null or lower(c.contract_number) like :prompt)
                        and c.status = 'ACTIVE'
                        and c.contract_status in ('ENTERED_INTO_FORCE', 'ACTIVE_IN_TERM', 'ACTIVE_IN_PERPETUITY', 'TERMINATED')
                        and c1.id = :customerId
                        and c1.status = 'ACTIVE'
                    """,
            countQuery = """
                    select count(1) from (
                        select
                            distinct c.id,
                            c.contract_number as formattedNumber,
                            'PRODUCT_CONTRACT' as type
                        from product_contract.contracts c
                        join product_contract.contract_details cd on cd.contract_id =  c.id
                        join customer.customer_details cd1 on cd.customer_detail_id = cd1.id
                        join customer.customers c1 on cd1.customer_id = c1.id
                            where (:prompt is null or lower(c.contract_number) like :prompt)
                            and c.status = 'ACTIVE'
                            and c.contract_status in ('ENTERED_INTO_FORCE', 'ACTIVE_IN_TERM', 'ACTIVE_IN_PERPETUITY', 'TERMINATED')
                            and c1.id = :customerId
                            and c1.status = 'ACTIVE'
                        union
                        select
                            distinct c.id,
                            c.contract_number as formattedNumber,
                            'SERVICE_CONTRACT' as type
                        from service_contract.contracts c
                        join service_contract.contract_details cd on cd.contract_id = c.id
                        join customer.customer_details cd1 on cd.customer_detail_id = cd1.id
                        join customer.customers c1 on cd1.customer_id = c1.id
                            where (:prompt is null or lower(c.contract_number) like :prompt)
                            and c.status = 'ACTIVE'
                            and c.contract_status in ('ENTERED_INTO_FORCE', 'ACTIVE_IN_TERM', 'ACTIVE_IN_PERPETUITY', 'TERMINATED')
                            and c1.id = :customerId
                            and c1.status = 'ACTIVE'
                    ) as count
                    """
    )
    Page<ActionContractResponse> filterContractsForAction(
            @Param("prompt") String prompt,
            @Param("customerId") Long customerId,
            Pageable pageable
    );

    @Query("""
            select count(c.id) > 0
            from ProductContract c
            where c.id = :contractId
            and c.status='ACTIVE'
            and (
                c.contractStatus = 'SIGNED'
                and c.subStatus in ('SIGNED_BY_BOTH_SIDES','SPECIAL_PROCESSES')
                or c.contractStatus in ('ENTERED_INTO_FORCE','ACTIVE_IN_TERM','ACTIVE_IN_PERPETUITY')
            )
            and (
                (cast(:activationDate as date) is null )
                or c.signingDate <= :activationDate
            )
            """)
    boolean existsForActivation(@Param("contractId") Long contractId,
                                @Param("activationDate") LocalDate activationDate);

    @Query(value = """
            select new bg.energo.phoenix.model.response.contract.productContract.terminations.ProductContractTerminationByTermsResponse(pc.id)
            from ProductContract pc
            join ProductContractDetails pcd on pc.id = pcd.contractId
            where pc.status = 'ACTIVE'
            and pc.contractStatus = 'ENTERED_INTO_FORCE'
            """)
    Page<ProductContractTerminationByTermsResponse> getEligibleProductContractsForTerminationByTerms(
            PageRequest pageable
    );

    @Query("""
            select count(pod.id) from PointOfDelivery pod
            join PointOfDeliveryDetails podd on podd.podId = pod.id
            join ContractPods cp on cp.contractDetailId = podd.id
            join ProductContractDetails pcd on pcd.id = cp.contractDetailId
            join ProductContract pc on pc.id = pcd.contractId
            where cp.activationDate is not null
            and pod.status = 'ACTIVE'
            and cp.status = 'ACTIVE'
            and pc.id = :productContractId
            """)
    long countProductContractDetailsPointOfDeliveriesWithActivationDate(Long productContractId);

    @Query(value = """
            select * from(
                  select c.id as contractId,
                         cd.id as detailId,
                         pct.automatic_renewal as automaticRenewal,
                         coalesce(pct.perpetuity_cause,false) as perpetuityCause,
                         perpetuity_cause,
                         pct.number_of_renewals as numberOfRenewals,
                       pct.renewal_period_type as renewalPeriodType,
                       pct.renewal_period_value as renewalValue,  pct.value as termValue,
                         pct.id as termId,
                         pct.contract_term_type termType,
                   case when coalesce(pct.perpetuity_cause,false) = false then(select termination_id from (select
                    t.create_date crdate,t.id as termination_id
                    from product.product_terminations pt
                   join product.terminations t on pt.termination_id = t.id
                    join product.product_details pd2 on pt.product_detail_id = pd2.id
                    join product_contract.contract_details cd3 on cd3.product_detail_id = pd2.id
                     and cd3.contract_id = c.id
                   where pt.status = 'ACTIVE'
                     and t.status = 'ACTIVE'
                     and t.event = 'EXPIRATION_OF_THE_CONTRACT_TERM'
                   union
                   select t2.create_date crdate,t2.id
                   from product.product_termination_groups ptg
                   join product.termination_groups tg on ptg.termination_group_id = tg.id
                   join product.termination_group_details tgd on tgd.termination_group_id = tg.id
                   and
                   tgd.start_date = (select max(start_date) from product.termination_group_details tgd3
                                  where tgd3.termination_group_id = tg.id
                         and tgd3.start_date <= current_date)
                   join product.termination_group_terminations tgt on tgt.termination_group_detail_id = tgd.id
                   join product.terminations t2 on tgt.termination_id = t2.id
                   join product.product_details pd3 on ptg.product_detail_id = pd3.id
                   join product_contract.contract_details cd3 on cd3.product_detail_id = pd3.id
                   and cd3.contract_id = c.id
                   where ptg.status =  'ACTIVE'
                     and tg.status =  'ACTIVE'
                     and tgt.status = 'ACTIVE'
                     and t2.status = 'ACTIVE'
                     and t2.event = 'EXPIRATION_OF_THE_CONTRACT_TERM'
                     order by crdate desc limit 1) as event) end terminationId
                    from product_contract.contracts c
                   join product_contract.contract_details cd on cd.contract_id =  c.id
                   join product.product_contract_terms pct on cd.product_contract_term_id = pct.id
                   where
                   contract_status in ('ENTERED_INTO_FORCE','ACTIVE_IN_TERM')
                   and c.status = 'ACTIVE'
                   and cd.start_date = (select max(start_date) from product_contract.contract_details cd1
                                          where cd1.contract_id = c.id
                                         and cd1.start_date <= current_date)
                   and (c.contract_term_end_date is null or c.contract_term_end_date <= current_date)
                  ) as tbl
                  where  tbl.terminationId is not null or tbl.perpetuity_cause = true
                        
            """,
            countQuery = """
                    select count(1) from(
                                      select c.id as contractId,
                                             cd.id as detailId,
                                             pct.automatic_renewal as automaticRenewal,
                                             coalesce(pct.perpetuity_cause,false) as perpetuityCause,
                                             perpetuity_cause,
                                             pct.number_of_renewals as numberOfRenewals,
                                           pct.renewal_period_type as renewalPeriodType,
                                           pct.renewal_period_value as renewalValue,  pct.value as termValue,
                                             pct.id as termId,
                                             pct.contract_term_type termType,
                                       case when coalesce(pct.perpetuity_cause,false) = false then(select termination_id from (select
                                        t.create_date crdate,t.id as termination_id
                                        from product.product_terminations pt
                                       join product.terminations t on pt.termination_id = t.id
                                        join product.product_details pd2 on pt.product_detail_id = pd2.id
                                        join product_contract.contract_details cd3 on cd3.product_detail_id = pd2.id
                                         and cd3.contract_id = c.id
                                       where pt.status = 'ACTIVE'
                                         and t.status = 'ACTIVE'
                                         and t.event = 'EXPIRATION_OF_THE_CONTRACT_TERM'
                                       union
                                       select t2.create_date crdate,t2.id
                                       from product.product_termination_groups ptg
                                       join product.termination_groups tg on ptg.termination_group_id = tg.id
                                       join product.termination_group_details tgd on tgd.termination_group_id = tg.id
                                       and
                                       tgd.start_date = (select max(start_date) from product.termination_group_details tgd3
                                                      where tgd3.termination_group_id = tg.id
                                             and tgd3.start_date <= current_date)
                                       join product.termination_group_terminations tgt on tgt.termination_group_detail_id = tgd.id
                                       join product.terminations t2 on tgt.termination_id = t2.id
                                       join product.product_details pd3 on ptg.product_detail_id = pd3.id
                                       join product_contract.contract_details cd3 on cd3.product_detail_id = pd3.id
                                       and cd3.contract_id = c.id
                                       where ptg.status =  'ACTIVE'
                                         and tg.status =  'ACTIVE'
                                         and tgt.status = 'ACTIVE'
                                         and t2.status = 'ACTIVE'
                                         and t2.event = 'EXPIRATION_OF_THE_CONTRACT_TERM'
                                         order by crdate desc limit 1) as event) end terminationId
                                        from product_contract.contracts c
                                       join product_contract.contract_details cd on cd.contract_id =  c.id
                                       join product.product_contract_terms pct on cd.product_contract_term_id = pct.id
                                       where
                                       contract_status in ('ENTERED_INTO_FORCE','ACTIVE_IN_TERM')
                                       and c.status = 'ACTIVE'
                                       and cd.start_date = (select max(start_date) from product_contract.contract_details cd1
                                                              where cd1.contract_id = c.id
                                                             and cd1.start_date <= current_date)
                                       and (c.contract_term_end_date is null or c.contract_term_end_date <= current_date)
                                      ) as tbl
                                      where  tbl.terminationId is not null or tbl.perpetuity_cause = true
                    """,
            nativeQuery = true
    )
    Page<ProductContractTerminationWithContractTermsResponse> getProductContractsForTermDeactivation(
            Pageable pageable
    );


    @Query(
            nativeQuery = true,
            value = """
                    with termination_data as (select t.auto_termination_from,
                                                     t.create_date      crdate,
                                                     cd3.contract_id as contrid,
                                                     t.id            as terminationId,
                                                     t.notice_due    as noticeDue
                                              from product.product_terminations pt
                                                       join
                                                   product.terminations t
                                                   on pt.termination_id = t.id
                                                       join
                                                   product.product_details pd2
                                                   on pt.product_detail_id = pd2.id
                                                       join product_contract.contract_details cd3
                                                            on cd3.product_detail_id = pd2.id
                                              where pt.status = 'ACTIVE'
                                                and t.status = 'ACTIVE'
                                                and t.auto_termination = 'true'
                                                and t.event = 'DEACTIVATION_OF_POINTS_OF_DELIVERY'
                                              union
                                              select t2.auto_termination_from,
                                                     t2.create_date     crdate,
                                                     cd3.contract_id as contrid,
                                                     t2.id           as terminationId,
                                                     t2.notice_due   as noticeDue
                                              from product.product_termination_groups ptg
                                                       join
                                                   product.termination_groups tg
                                                   on ptg.termination_group_id = tg.id
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
                                                   product.product_details pd3
                                                   on ptg.product_detail_id = pd3.id
                                                       join product_contract.contract_details cd3
                                                            on cd3.product_detail_id = pd3.id
                                              where ptg.status = 'ACTIVE'
                                                and tg.status = 'ACTIVE'
                                                and tgt.status = 'ACTIVE'
                                                and t2.status = 'ACTIVE'
                                                and t2.auto_termination = 'true'
                                                and t2.event = 'DEACTIVATION_OF_POINTS_OF_DELIVERY')
                    select *
                    from (select c.id,
                                 (select cp.deactivation_date
                                  from product_contract.contract_details cd
                                           join
                                       product_contract.contract_pods cp
                                       on cp.contract_detail_id = cd.id
                                           and cd.contract_id = c.id
                                  where cp.status = 'ACTIVE'
                                    and cp.deactivation_date < current_date
                                  order by cp.deactivation_date desc
                                  limit 1) as deactivationDate,
                                 (select td.auto_termination_from
                                  from termination_data td
                                  where td.contrid = c.id
                                  order by td.crdate desc
                                  limit 1) as autoTerminationFrom,
                                 (select td.terminationId
                                  from termination_data td
                                  where td.contrid = c.id
                                  order by td.crdate desc
                                  limit 1) as terminationId,
                                 (select td.noticeDue
                                  from termination_data td
                                  where td.contrid = c.id
                                  order by td.crdate desc
                                  limit 1) as noticeDue
                          from product_contract.contracts c
                          where c.status = 'ACTIVE'
                            and c.contract_status in ('ACTIVE_IN_TERM', 'ACTIVE_IN_PERPETUITY')
                            and not exists
                              (select 1
                               from product_contract.contract_details cd
                                        join
                                    product_contract.contract_pods cp
                                    on cp.contract_detail_id = cd.id
                                        and cp.status = 'ACTIVE'
                                        and cp.activation_date is null
                                        and cp.deactivation_date is null
                               where cd.contract_id = c.id
                                 and cd.start_date >= (select max(start_date)
                                                       from product_contract.contract_details cd1
                                                       where cd1.contract_id = c.id
                                                         and cd1.start_date <= current_date))
                            and exists(select 1
                                       from product_contract.contract_details cd2
                                                join
                                            product.product_details pd
                                            on cd2.product_detail_id = pd.id
                                       where cd2.contract_id = c.id
                                         and pd.status = 'ACTIVE'
                                         and current_date between coalesce(pd.available_From, current_date) and coalesce(pd.available_To, current_date))) as tbl
                    where tbl.deactivationDate is not null
                      and tbl.autoTerminationFrom is not null
                    """,
            countQuery = """
                    with termination_data as (select t.auto_termination_from,
                                                     t.create_date      crdate,
                                                     cd3.contract_id as contrid,
                                                     t.id            as terminationId,
                                                     t.notice_due    as noticeDue
                                              from product.product_terminations pt
                                                       join
                                                   product.terminations t
                                                   on pt.termination_id = t.id
                                                       join
                                                   product.product_details pd2
                                                   on pt.product_detail_id = pd2.id
                                                       join product_contract.contract_details cd3
                                                            on cd3.product_detail_id = pd2.id
                                              where pt.status = 'ACTIVE'
                                                and t.status = 'ACTIVE'
                                                and t.auto_termination = 'true'
                                                and t.event = 'DEACTIVATION_OF_POINTS_OF_DELIVERY'
                                              union
                                              select t2.auto_termination_from,
                                                     t2.create_date     crdate,
                                                     cd3.contract_id as contrid,
                                                     t2.id           as terminationId,
                                                     t2.notice_due   as noticeDue
                                              from product.product_termination_groups ptg
                                                       join
                                                   product.termination_groups tg
                                                   on ptg.termination_group_id = tg.id
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
                                                   product.product_details pd3
                                                   on ptg.product_detail_id = pd3.id
                                                       join product_contract.contract_details cd3
                                                            on cd3.product_detail_id = pd3.id
                                              where ptg.status = 'ACTIVE'
                                                and tg.status = 'ACTIVE'
                                                and tgt.status = 'ACTIVE'
                                                and t2.status = 'ACTIVE'
                                                and t2.auto_termination = 'true'
                                                and t2.event = 'DEACTIVATION_OF_POINTS_OF_DELIVERY')
                    select count(1)
                    from (select c.id,
                                 (select cp.deactivation_date
                                  from product_contract.contract_details cd
                                           join
                                       product_contract.contract_pods cp
                                       on cp.contract_detail_id = cd.id
                                           and cd.contract_id = c.id
                                  where cp.status = 'ACTIVE'
                                    and cp.deactivation_date < current_date
                                  order by cp.deactivation_date desc
                                  limit 1) as deactivationDate,
                                 (select td.auto_termination_from
                                  from termination_data td
                                  where td.contrid = c.id
                                  order by td.crdate desc
                                  limit 1) as autoTerminationFrom,
                                 (select td.terminationId
                                  from termination_data td
                                  where td.contrid = c.id
                                  order by td.crdate desc
                                  limit 1) as terminationId,
                                 (select td.noticeDue
                                  from termination_data td
                                  where td.contrid = c.id
                                  order by td.crdate desc
                                  limit 1) as noticeDue
                          from product_contract.contracts c
                          where c.status = 'ACTIVE'
                            and c.contract_status in ('ACTIVE_IN_TERM', 'ACTIVE_IN_PERPETUITY')
                            and not exists
                              (select 1
                               from product_contract.contract_details cd
                                        join
                                    product_contract.contract_pods cp
                                    on cp.contract_detail_id = cd.id
                                        and cp.status = 'ACTIVE'
                                        and cp.activation_date is null
                                        and cp.deactivation_date is null
                               where cd.contract_id = c.id
                                 and cd.start_date >= (select max(start_date)
                                                       from product_contract.contract_details cd1
                                                       where cd1.contract_id = c.id
                                                         and cd1.start_date <= current_date))
                            and exists(select 1
                                       from product_contract.contract_details cd2
                                                join
                                            product.product_details pd
                                            on cd2.product_detail_id = pd.id
                                       where cd2.contract_id = c.id
                                         and pd.status = 'ACTIVE'
                                         and current_date between coalesce(pd.available_From, current_date) and coalesce(pd.available_To, current_date))) as tbl
                    where tbl.deactivationDate is not null
                      and tbl.autoTerminationFrom is not null
                    """
    )
    Page<ProductContractTerminationWithPodsResponse> getProductContractsToTerminationWithPodsDeactivation(
            Pageable pageable
    );


    @Query(
            nativeQuery = true,
            value = """
                    select tbl.contract_id           as contractId,
                           tbl.action_id             as actionId,
                           tbl.termination_id        as terminationId,
                           tbl.notice_due            as noticeDue,
                           tbl.action_execution_date as actionExecutionDate,
                           tbl.penalty_payer         as actionPenaltyPayer,
                           tbl.action_type_id        as actionTypeId,
                           tbl.auto_termination_from as autoTerminationFrom,
                           tbl.date_of_termination   as contractTerminationDate
                    from (select c.id              as                                                         contract_id,
                                 ac.id             as                                                         action_id,
                                 t.id              as                                                         termination_id,
                                 t.notice_due,
                                 ac.execution_date as                                                         action_execution_date,
                                 ac.penalty_payer,
                                 t.auto_termination_from,
                                 ac.action_type_id,
                                 case
                                     when t.auto_termination_from = 'EVENT_DATE'
                                         then ac.execution_date
                                     when t.auto_termination_from = 'FIRST_DAY_OF_MONTH_FOLLOWING_EVENT_DATE'
                                         then date_trunc('month', ac.execution_date + interval '1 month') end date_of_termination,
                                 row_number() over (
                                     partition by ac.product_contract_id order by
                                         case
                                             when t.auto_termination_from = 'EVENT_DATE'
                                                 then ac.execution_date
                                             when t.auto_termination_from = 'FIRST_DAY_OF_MONTH_FOLLOWING_EVENT_DATE'
                                                 then date_trunc('month', ac.execution_date + interval '1 month') end asc, ac.create_date
                                     )             as                                                         priority
                          from product_contract.contracts c
                                   join action.actions ac on ac.product_contract_id = c.id
                                   join product.terminations t on ac.termination_id = t.id
                          where c.status = 'ACTIVE'
                            and contract_status in ('ENTERED_INTO_FORCE', 'ACTIVE_IN_TERM', 'ACTIVE_IN_PERPETUITY')
                            and ac.status = 'ACTIVE'
                            and t.status = 'ACTIVE'
                            and (
                              t.auto_termination_from = 'EVENT_DATE' and ac.execution_date <= current_date
                                  or t.auto_termination_from = 'FIRST_DAY_OF_MONTH_FOLLOWING_EVENT_DATE' and
                                     date_trunc('month', ac.execution_date + interval '1 month') <= current_date
                              )) as tbl
                    where priority = 1
                                        """,
            countQuery = """
                    select count(1)
                    from (select c.id              as                                                         contract_id,
                                 ac.id             as                                                         action_id,
                                 t.id              as                                                         termination_id,
                                 t.notice_due,
                                 ac.execution_date as                                                         action_execution_date,
                                 ac.penalty_payer,
                                 t.auto_termination_from,
                                 ac.action_type_id,
                                 case
                                     when t.auto_termination_from = 'EVENT_DATE'
                                         then ac.execution_date
                                     when t.auto_termination_from = 'FIRST_DAY_OF_MONTH_FOLLOWING_EVENT_DATE'
                                         then date_trunc('month', ac.execution_date + interval '1 month') end date_of_termination,
                                 row_number() over (
                                     partition by ac.product_contract_id order by
                                         case
                                             when t.auto_termination_from = 'EVENT_DATE'
                                                 then ac.execution_date
                                             when t.auto_termination_from = 'FIRST_DAY_OF_MONTH_FOLLOWING_EVENT_DATE'
                                                 then date_trunc('month', ac.execution_date + interval '1 month') end asc, ac.create_date
                                     )             as                                                         priority
                          from product_contract.contracts c
                                   join action.actions ac on ac.product_contract_id = c.id
                                   join product.terminations t on ac.termination_id = t.id
                          where c.status = 'ACTIVE'
                            and contract_status in ('ENTERED_INTO_FORCE', 'ACTIVE_IN_TERM', 'ACTIVE_IN_PERPETUITY')
                            and ac.status = 'ACTIVE'
                            and t.status = 'ACTIVE'
                            and (
                              t.auto_termination_from = 'EVENT_DATE' and ac.execution_date <= current_date
                                  or t.auto_termination_from = 'FIRST_DAY_OF_MONTH_FOLLOWING_EVENT_DATE' and
                                     date_trunc('month', ac.execution_date + interval '1 month') <= current_date
                              )) as tbl
                    where priority = 1
                    """
    )
    Page<ProductContractTerminationWithActionsResponse> getEligibleProductContractsForTerminationWithAction(
            Pageable pageable
    );

    @Query("""
            select count(pcrpc.id) > 0 from ProductContractRelatedProductContract pcrpc
               where (
                          (pcrpc.productContractId = :id or pcrpc.relatedProductContractId = :id)
                          and pcrpc.status = 'ACTIVE'
                      )
               and exists (
                      select 1
                      from ProductContract as c1
                      where c1.id = pcrpc.productContractId and c1.status = 'ACTIVE'
                  )
               and exists (
                      select 1
                      from ProductContract as c2
                      where c2.id = pcrpc.relatedProductContractId and c2.status = 'ACTIVE'
                  )
             """)
    boolean hasConnectionToProductContract(Long id);

    @Query("""
            select count(pcrsc.id) > 0
            from ProductContractRelatedServiceContract pcrsc
            join ServiceContracts sc on (pcrsc.serviceContractId = sc.id and pcrsc.productContractId = :id)
            where sc.status = 'ACTIVE'
            and pcrsc.status = 'ACTIVE'
            """)
    boolean hasConnectionToServiceContract(Long id);

    @Query("""
            select count(pcrso.id) > 0
            from ProductContractRelatedServiceOrder pcrso
            join ServiceOrder so on (so.id = pcrso.serviceOrderId and pcrso.productContractId = :id)
            and pcrso.status = 'ACTIVE'
            and so.status = 'ACTIVE'
            """)
    boolean hasConnectionToServiceOrder(Long id);

    @Query("""
            select count(solpc.id) > 0
            from ServiceOrderLinkedProductContract solpc
            join ServiceOrder so on (so.id = solpc.orderId and solpc.contractId = :id)
            where solpc.status = 'ACTIVE'
            and so.status = 'ACTIVE'
            """)
    boolean isLinkedToServiceOrder(Long id);

    @Query("""
            select count(pcrgo.id) > 0
            from ProductContractRelatedGoodsOrder pcrgo
            join GoodsOrder go on (go.id = pcrgo.goodsOrderId and pcrgo.productContractId = :id)
            and pcrgo.status = 'ACTIVE'
            and go.status = 'ACTIVE'
            """)
    boolean hasConnectionToGoodsOrder(Long id);

    @Query("""
            select count(pct.id) > 0
            from ProductContractTask pct
            join Task t on pct.taskId = t.id and pct.contractId = :id
            where t.status = 'ACTIVE'
            and pct.status = 'ACTIVE'
            """)
    boolean hasConnectionToTask(Long id);

    @Query("""
            select count(a.id) > 0
            from Action a
            where a.productContractId = :id
            and a.status = 'ACTIVE'
            """)
    boolean hasConnectionToAction(Long id);

    @Query(nativeQuery = true, value = "SELECT setval('product_contract.contract_number_seq', 1, true)")
    void resetContractNumberSequence();


    @Query(
            value = """
                    select count(pct.id)
                         from ProductContract c
                                  join ProductContractDetails cd
                                       on cd.contractId = c.id
                                           and c.status = 'ACTIVE'
                                           and c.id = :contractId
                                  join ProductDetails pd
                                       on cd.productDetailId = pd.id
                                  join Product p
                                       on pd.product.id = p.id
                                           and p.productStatus = 'ACTIVE'
                                           and pd.productDetailStatus = 'ACTIVE'
                                  join ProductContractTerms pct
                                       on pct.productDetailsId =  pd.id
                                           and pct.status = 'ACTIVE'
                                           and pct.perpetuityCause = true
                         where
                                 cd.startDate = (select max(cd2.startDate) from ProductContractDetails cd2
                                                  where cd2.contractId = c.id
                                                    and cd2.startDate <= :date)
                        """
    )
    Long getProductContractTermsCountByContractId(@Param("contractId") Long contractId, @Param("date") LocalDate date);

    @Query("""
            select pct
            from ProductContract c
                     join ProductContractDetails cd
                          on cd.contractId = c.id
                              and c.status = 'ACTIVE'
                              and c.id = :contractId
                              and cd.id = :contractDetailId
                     join ProductContractTerms pct
                          on cd.productContractTermId =  pct.id
                              and pct.status = 'ACTIVE'
                              and pct.perpetuityCause = true
            """)
    Optional<ProductContractTerms> getProductContractTermByContractIdAndDetailId(@Param("contractId") Long contractId,
                                                                                 @Param("contractDetailId") Long contractDetailId);

    @Query("""
            select pc from ProductContract as pc
            where ((pc.contractStatus = 'SIGNED' and pc.subStatus in ('SIGNED_BY_BOTH_SIDES','SPECIAL_PROCESSES'))
                    or (pc.contractStatus = 'ENTERED_INTO_FORCE' and pc.subStatus = 'AWAITING_ACTIVATION'))
            and pc.status = 'ACTIVE'
            and (pc.activationDate is null or pc.activationDate < :date or pc.activationDate = :date)
            """)
    List<ProductContract> getProductContractsForStatusUpdateFromJob(@Param("date") LocalDate date);

    @Query("""
            select new bg.energo.phoenix.model.CacheObject(c.id,c.contractNumber)
            from ProductContract c 
            where c.contractNumber=:contractNumber
            and c.status = 'ACTIVE'
                """)
    Optional<CacheObject> getContractCacheObjectByNumber(String contractNumber);

    @Query("""
            select new bg.energo.phoenix.model.CacheObjectForDetails(c.id,cd.versionId,cd.id)
            from ProductContract c
            join ProductContractDetails cd on cd.contractId=c.id
            where c.contractNumber=:contractNumber
            and c.status = 'ACTIVE'
            and cd.versionId=:versionId
                """)
    @QueryHints(@QueryHint(name = "org.hibernate.cacheable", value = "true"))
    Optional<CacheObjectForDetails> getContractCacheObjectByNumberAndVersion(String contractNumber, Integer versionId);

    @Query("""
            select new bg.energo.phoenix.model.CacheObjectForDetails(c.id,cd.versionId,cd.id)
            from ProductContract c
            join ProductContractDetails cd on cd.contractId=c.id
            where c.contractNumber=:contractNumber
            and c.status = 'ACTIVE'
            and cd.versionId = (
            select max(cd2.versionId) from ProductContractDetails cd2
            where cd2.contractId = c.id)
                """)
    @QueryHints(@QueryHint(name = "org.hibernate.cacheable", value = "true"))
    Optional<CacheObjectForDetails> getLatestContractCacheObject(String contractNumber);

    @Query("""
            select new bg.energo.phoenix.model.CacheObjectForLocalDate(pc.contractTermEndDate)
            from ProductContract pc
            where pc.id=:contractId
            """)
        //Todo this is removed for testing purposes should be added in future.
//    @QueryHints(@QueryHint(name = "org.hibernate.cacheable", value = "true"))

    Optional<CacheObjectForLocalDate> findContractTermEndDate(Long contractId);


    @Query("""
            select new bg.energo.phoenix.service.billing.runs.models.BillingRunForVolumesModel(pc.id,pcd.id,pcd.contractType,pc.contractStatus,pc.terminationDate,cbg.id,cbg.groupNumber,cbg.separateInvoiceForEachPod,pod.id,podd.id,podd.measurementType,pod.identifier,cp.activationDate,cp.deactivationDate)
            from ProductContract pc
            join ProductContractDetails pcd on pcd.contractId=pc.id
            join ContractPods cp on cp.contractDetailId=pcd.id
            join ContractBillingGroup cbg on cbg.id=cp.billingGroupId
            join PointOfDeliveryDetails podd on podd.id=cp.podDetailId
            join PointOfDelivery pod on pod.id=podd.podId
            where pc.id=:contractId
            and cp.status='ACTIVE'
            and (pc.terminationDate is null  or pc.terminationDate>cp.activationDate)
                        
            """)
    List<BillingRunForVolumesModel> getBillingRunForVolumesModelsByContractId(Long contractId);


//    @Query(nativeQuery = true, value = """
//                   with podId as (select p.id,p.identifier
//                                  from product_contract.contracts pc
//                                  join product_contract.contract_details pcd on pc.id = pcd.contract_id
//                                  join product_contract.contract_pods cp on pcd.id=cp.contract_detail_id
//                                  join pod.pod_details podd on podd.id=cp.pod_detail_id
//                                  join pod.pod p on p.id = podd.pod_id
//                                  where pc.id=:id)
//                   select -1 as billingByProfileId,
//                          bs.id as billingByScalesId,
//                          podId.identifier as podIdentifier,
//                          podId.id as podId,
//                          bs.date_from as billingFrom,
//                          bs.date_to as billingTo
//                    from podId,pod.billing_by_scale bs
//                   where bs.pod_id = podId.id
//                   union all
//                   select bp.id as billingByProfileId,
//                          -1 as billingByScalesId,
//                          podId.identifier as podIdentifier,
//                          podId.id as podId,
//                          bp.period_from as billingFrom,
//                          bp.period_to as billingTo
//                    from podId, pod.billing_by_profile bp
//                   where bp.pod_id= podId.id
//
//
//            """)
//    List<BillingDataShortModelProfile> getBillingDataShortModelForContractId(Long contractId);

    @Query(value = """
            select new bg.energo.phoenix.service.billing.runs.models.BillingDataShortModelProfile(p.id,bp.periodFrom,bp.periodTo,bp.createDate,bp.id,bp.profileId,bp.periodType) 
            from 
            ProductContract pc
            join ProductContractDetails  pcd on pc.id=pcd.contractId
            join ContractPods  cp on pcd.id=cp.contractDetailId
            join PointOfDeliveryDetails podd on podd.id=cp.podDetailId
            join PointOfDelivery p on p.id=podd.podId
            join BillingByProfile bp on bp.podId=p.id
            where pc.id=:contractId
            and bp.status='ACTIVE'
            and (select count(bdbp.id) from BillingDataByProfile bdbp where bdbp.billingByProfileId = bp.id) > 0
            """)
    List<BillingDataShortModelProfile> getBillingDataShortModelProfileForContractId(Long contractId);

    @Query(value = """
            select new bg.energo.phoenix.service.billing.runs.models.BillingDataShortModelForScale(p.identifier,p.id,bs.dateFrom,bs.dateTo,bs.createDate,bs.id) from 
            ProductContract pc
            join ProductContractDetails  pcd on pc.id=pcd.contractId
            join ContractPods  cp on pcd.id=cp.contractDetailId
            join PointOfDeliveryDetails podd on podd.id=cp.podDetailId
            join PointOfDelivery p on p.id=podd.podId
            join BillingByScale bs on bs.podId=p.id
            where pc.id=:contractId
            and bs.status='ACTIVE'
            and (select count(bdbp.id) from BillingDataByScale bdbp where bdbp.billingByScaleId = bs.id) > 0
            """)
    List<BillingDataShortModelForScale> getBillingDataShortModelScaleForContractId(Long contractId);


    @Query(nativeQuery = true,
            value = """
                            select distinct c.id as contractId, c.contract_number as contractNumber, c.create_date as creationDate, 'PRODUCT_CONTRACT' as contractType
                            from product_contract.contracts c
                             join product_contract.contract_details cd
                            on cd.contract_id = c.id
                            and  c.status = 'ACTIVE'
                            and c.contract_status in ('ACTIVE_IN_TERM', 'ACTIVE_IN_PERPETUITY')join customer.customer_details cd2
                                            on cd.customer_detail_id  = cd2.id
                                            join customer.customers c2
                                            on cd2.customer_id = c2.id
                                            and c2.id = :customerId
                                            and c2.status = 'ACTIVE'
                            union
                            select distinct c.id, c.contract_number, c.create_date ,'SERVICE_CONTRACT'
                            from service_contract.contracts c
                             join service_contract.contract_details cd
                            on cd.contract_id = c.id
                            
                            and c.status = 'ACTIVE'
                            and c.contract_status in ('ACTIVE_IN_TERM', 'ACTIVE_IN_PERPETUITY')
                            join customer.customer_details cd2
                                            on cd.customer_detail_id  = cd2.id
                                            join customer.customers c2
                                            on cd2.customer_id = c2.id
                                            and c2.id = :customerId
                                            and c2.status = 'ACTIVE'
                    """,
            countQuery = """
                    select
                           count(contractId)
                       from (
                           select distinct c.id as contractId, c.contract_number as contractNumber, c.create_date as creationDate, 'PRODUCT_CONTRACT' as contractType
                           from product_contract.contracts c
                            join product_contract.contract_details cd
                           on cd.contract_id = c.id
                           and  c.status = 'ACTIVE'
                           and c.contract_status in ('ACTIVE_IN_TERM', 'ACTIVE_IN_PERPETUITY')join customer.customer_details cd2
                                    on cd.customer_detail_id  = cd2.id
                                    join customer.customers c2
                                    on cd2.customer_id = c2.id
                                    and c2.id = :customerId
                                    and c2.status = 'ACTIVE'
                           union
                           select distinct c.id, c.contract_number, c.create_date, 'SERVICE_CONTRACT'
                           from service_contract.contracts c
                            join service_contract.contract_details cd
                           on cd.contract_id = c.id
                           
                           and c.status = 'ACTIVE'
                           and c.contract_status in ('ACTIVE_IN_TERM', 'ACTIVE_IN_PERPETUITY')
                       join customer.customer_details cd2
                                    on cd.customer_detail_id  = cd2.id
                                    join customer.customers c2
                                    on cd2.customer_id = c2.id
                                    and c2.id = :customerId
                                    and c2.status = 'ACTIVE') as count_query
                    """)
    Page<CustomerActiveContractResponse> findByCustomerId(
            @Param("customerId") Long customerId,
            Pageable pageable
    );

    @Query(nativeQuery = true,
            value = """
                    select distinct c.id as id, c.contract_number as number, c.create_date as creationDate, 'PRODUCT_CONTRACT' as type
                    from product_contract.contracts c
                    join product_contract.contract_details cd
                    on cd.contract_id = c.id
                    and cd.customer_detail_id = :customerDetailsId
                    and c.status = 'ACTIVE'
                    and (:prompt is null
                    or lower(c.contract_number) like :prompt)
                    union
                    select distinct c.id, c.contract_number, c.create_date ,'SERVICE_CONTRACT'
                    from service_contract.contracts c
                    join service_contract.contract_details cd
                    on cd.contract_id = c.id
                    and cd.customer_detail_id = :customerDetailsId
                    and c.status = 'ACTIVE'
                    and (:prompt is null
                    or lower(c.contract_number) like :prompt)
                    """,
            countQuery = """
                    select count(id)
                    from (
                        select distinct c.id as id, c.contract_number as number, c.create_date as creationDate, 'PRODUCT_CONTRACT' as type                   from product_contract.contracts c
                        join product_contract.contract_details cd
                        on cd.contract_id = c.id
                        and cd.customer_detail_id = :customerDetailsId
                        and c.status = 'ACTIVE'
                        and (:prompt is null
                        or lower(c.contract_number) like :prompt)
                        union
                        select distinct c.id, c.contract_number, c.create_date, 'SERVICE_CONTRACT'
                        from service_contract.contracts c
                        join service_contract.contract_details cd
                        on cd.contract_id = c.id
                        and cd.customer_detail_id = :customerDetailsId
                        and c.status = 'ACTIVE'
                        and (:prompt is null
                        or lower(c.contract_number) like :prompt)
                       ) as count_query
                    """)
    Page<CustomerContractOrderResponse> findByCustomerDetailsIdAndPrompt(
            @Param("customerDetailsId") Long customerDetailsId,
            @Param("prompt") String prompt,
            Pageable pageable
    );

    @Query("""
            select new bg.energo.phoenix.model.response.billing.billingRun.manualInvoice.ContractOrderShortResponse(pc.id, pc.contractNumber)
            from ProductContract pc
            where pc.id = :id
            and pc.status = :status
            """)
    Optional<ContractOrderShortResponse> findByIdAndStatus(@Param("id") Long id,
                                                           @Param("status") ProductContractStatus status);


    @Query("""
                select new bg.energo.phoenix.model.response.contract.productContract.ContractWithStatusShortResponse
                (pc.id, cast( pc.contractStatus as string), cast(pc.subStatus as string))
                from ProductContract pc
                where pc.id = :id
                and pc.status = 'ACTIVE'
            """)
    Optional<ContractWithStatusShortResponse> getProductContractWithStatus(@Param("id") Long id);

    @Query("""
                      select count(lpc.id) > 0
                      from ContractLinkedProductContract as lpc
                               join ServiceContracts as con
                                    on con.id = lpc.contractId
                      where lpc.linkedProductContractId = :id
                        and lpc.status = 'ACTIVE'
                        and con.status = 'ACTIVE'
            """)
    boolean isLinkedToServiceContract(Long id);

    @Query("""
            select  
            customer.identifier as customerIdentifier,
            customerDetails.versionId as customerVersionId,
            contractDetails.id as productContractDetailId
            from Contract contract
                    join ProductContractDetails contractDetails on contract.id = contractDetails.contractId
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
            select pcd.versionId from ProductContractDetails pcd
            join ProductContract productContract on productContract.contractNumber = :contractNumber
            where pcd.startDate = (
                select max(innerPCD.startDate) from ProductContractDetails innerPCD
                where innerPCD.contractId = productContract.id
            )
            and pcd.contractId = productContract.id
            """
    )
    Long findLatestProductContractDetailVersionId(String contractNumber);

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
                                                   join product_contract.contract_details pcd
                                                        on pcd.customer_detail_id = cc.customer_detail_id
                                          where pcd.contract_id = :id
                                            and pcd.version_id = :versionId),
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
                                                 join product_contract.contract_details pcd
                                                      on pcd.customer_detail_id = ccc.customer_detail_id
                                        where pcd.contract_id = :id
                                          and pcd.version_id = :versionId
                                          and cc.status = 'ACTIVE'
                                        group by cc.customer_communication_id),
                 additional_params as (select ap.product_detail_id,
                                              coalesce(cpap.label, ap.label) as label,
                                              coalesce(cpap.value, ap.value) as value,
                                              ap.ordering_id
                                       from product.product_additional_params ap
                                                join product_contract.contract_details pcd
                                                     on pcd.product_detail_id = ap.product_detail_id
                                                left join product_contract.contract_product_additional_params cpap
                                                          on ap.id = cpap.product_additional_param_id and
                                                             cpap.contract_detail_id = pcd.id
                                       where pcd.contract_id = :id
                                         and pcd.version_id = :versionId),
                 banks as (select b.id, b.name, b.bic
                           from nomenclature.banks b)
            select cd.customer_name_comb                                                  as CustomerNameComb,
                   cd.customer_name_comb_trsl                                             as CustomerNameCombTrsl,
                   cd.customer_identifier                                                 as CustomerIdentifier,
                   cd.customer_number                                                     as CustomerNumber,
                   cd.customer_type                                                       as CustomerType,
                   translation.translate_text(
                           customer.formatted_address,
                           text('BULGARIAN')
                   )                                                                      as HeadquarterAddressComb,
                   customer.populated_place                                               as HeadquarterPopulatedPlace,
                   customer.zip_code                                                      as HeadquarterZip,
                   customer.district                                                      as HeadquarterDistrict,
                   translation.translate_text(customer.ra_type, text('BULGARIAN'))        as HeadquarterQuarterRaType,
                   customer.ra_type                                                       as HeadquarterQuarterRaTypeTrsl,
                   customer.ra_name                                                       as HeadquarterQuarterRaName,
                   translation.translate_text(text(customer.street_type),
                                              text('BULGARIAN'))                          as HeadquarterStrBlvdType,
                   customer.street_type                                                   as HeadquarterStrBlvdTypeTrsl,
                   customer.street                                                        as HeadquarterStrBlvdName,
                   customer.street_number                                                 as HeadquarterStrBlvdNumber,
                   customer.block                                                         as HeadquarterBlock,
                   customer.entrance                                                      as HeadquarterEntrance,
                   customer.floor                                                         as HeadquarterFloor,
                   customer.apartment                                                     as HeadquarterApartment,
                   customer.address_additional_info                                       as HeadquarterAdditionalInfo,

                   translation.translate_text(contr_cc.formatted_address,
                                              text('BULGARIAN'))                          as CommunicationAddressComb,
                   contr_cc.populated_place                                               as CommunicationPopulatedPlace,
                   contr_cc.zip_code                                                      as CommunicationZip,
                   contr_cc.district                                                      as CommunicationDistrict,
                   translation.translate_text(contr_cc.ra_type, text('BULGARIAN'))        as CommunicationQuarterRaType,
                   contr_cc.ra_type                                                       as CommunicationQuarterRaTypeTrsl,
                   contr_cc.ra_name                                                       as CommunicationQuarterRaName,
                   translation.translate_text(text(contr_cc.street_type),
                                              text('BULGARIAN'))                          as CommunicationStrBlvdType,
                   contr_cc.street_type                                                   as CommunicationStrBlvdTypeTrsl,
                   contr_cc.street                                                        as CommunicationStrBlvdName,
                   contr_cc.street_number                                                 as CommunicationStrBlvdNumber,
                   contr_cc.block                                                         as CommunicationBlock,
                   contr_cc.entrance                                                      as CommunicationEntrance,
                   contr_cc.floor                                                         as CommunicationFloor,
                   contr_cc.apartment                                                     as CommunicationApartment,
                   contr_cc.address_additional_info                                       as CommunicationAdditionalInfo,

                   contr_contacts.email_comb                                              as CommunicationEmailComb,
                   contr_contacts.mobile_comb                                             as CommunicationMobileComb,
                   contr_contacts.phone_comb                                              as CommunicationPhoneComb,
                   contr_contacts.email_array                                             as CommunicationEmail,
                   contr_contacts.mobile_array                                            as CommunicationMobile,
                   contr_contacts.phone_array                                             as CommunicationPhone,

                   translation.translate_text(bil_cc.formatted_address,
                                              text('BULGARIAN'))                          as BillingAddressComb,
                   bil_cc.populated_place                                                 as BillingPopulatedPlace,
                   bil_cc.zip_code                                                        as BillingZip,
                   bil_cc.district                                                        as BillingDistrict,
                   translation.translate_text(bil_cc.ra_type, text('BULGARIAN'))          as BillingQuarterRaType,
                   bil_cc.ra_type                                                         as BillingQuarterRaTypeTrsl,
                   bil_cc.ra_name                                                         as BillingQuarterRaName,
                   translation.translate_text(text(bil_cc.street_type),
                                              text('BULGARIAN'))                          as BillingStrBlvdType,
                   bil_cc.street_type                                                     as BillingStrBlvdTypeTrsl,
                   bil_cc.street                                                          as BillingStrBlvdName,
                   bil_cc.street_number                                                   as BillingStrBlvdNumber,
                   bil_cc.block                                                           as BillingBlock,
                   bil_cc.entrance                                                        as BillingEntrance,
                   bil_cc.floor                                                           as BillingFloor,
                   bil_cc.apartment                                                       as BillingApartment,
                   bil_cc.address_additional_info                                         as BillingAdditionalInfo,

                   bil_contacts.email_comb                                                as BillingEmailComb,
                   bil_contacts.mobile_comb                                               as BillingMobileComb,
                   bil_contacts.phone_comb                                                as BillingPhoneComb,
                   bil_contacts.email_array                                               as BillingEmail,
                   bil_contacts.mobile_array                                              as BillingMobile,
                   bil_contacts.phone_array                                               as BillingPhone,

                   seg.customer_segments                                                  as CustomerSegments,

                   pd.name                                                                as ProductName,
                   pd.name_transl                                                         as ProductNameTrsl,
                   pd.printing_name                                                       as ProductPrintName,
                   pd.printing_name_transl                                                as ProductPrintNameTrsl,
                   pd.invoice_and_templates_text                                          as TextInvoicesTemplates,
                   pd.invoice_and_templates_text_transl                                   as TextInvoicesTemplatesTrsl,
                   replace(text(pcd.contract_type), '_', ' ')                             as ContractType,
                   translation.translate_text(replace(text(pcd.payment_guarantee), '_', ' '),
                                              text('BULGARIAN'))                          as PaymentGuaranteeType,
                   case
                       when text(pcd.payment_guarantee) in ('CASH_DEPOSIT', 'CASH_DEPOSIT_AND_BANK') then
                           pcd.cash_deposit_amount end                                    as DepositAmount,
                   case
                       when text(pcd.payment_guarantee) in ('CASH_DEPOSIT', 'CASH_DEPOSIT_AND_BANK') then
                           deposit_curr.print_name end                                    as DepositCurrencyPrintName,
                   case
                       when text(pcd.payment_guarantee) in ('CASH_DEPOSIT', 'CASH_DEPOSIT_AND_BANK') then
                           deposit_curr.abbreviation end                                  as DepositCurrencyAbr,
                   case
                       when text(pcd.payment_guarantee) in ('CASH_DEPOSIT', 'CASH_DEPOSIT_AND_BANK') then
                           deposit_curr.full_name end                                     as DepositCurrencyFullName,

                   case
                       when text(pcd.payment_guarantee) in ('BANK', 'CASH_DEPOSIT_AND_BANK') then
                           pcd.bank_guarantee_amount end                                  as BankGuaranteeAmount,
                   case
                       when text(pcd.payment_guarantee) in ('BANK', 'CASH_DEPOSIT_AND_BANK') then
                           b_g_curr.print_name end                                        as BankGuaranteeCurrencyPrintName,
                   case
                       when text(pcd.payment_guarantee) in ('BANK', 'CASH_DEPOSIT_AND_BANK') then
                           b_g_curr.abbreviation end                                      as BankGuaranteeCurrencyAbr,
                   case
                       when text(pcd.payment_guarantee) in ('BANK', 'CASH_DEPOSIT_AND_BANK') then
                           b_g_curr.full_name end                                         as BankGuaranteeCurrencyFullName,

                   replace(text(pd.schedule_registration), '_', ' ')                      as RegistrationSchedules,
                   replace(text(pd.forecasting), '_', ' ')                                as Forecasting,
                   replace(text(pd.taking_over_balancing_cost), '_', ' ')                 as TakingBalancingCosts,

                   pd.additional_info1                                                    as AdditionalField1,
                   pd.additional_info2                                                    as AdditionalField2,
                   pd.additional_info3                                                    as AdditionalField3,
                   pd.additional_info4                                                    as AdditionalField4,
                   pd.additional_info5                                                    as AdditionalField5,
                   pd.additional_info6                                                    as AdditionalField6,
                   pd.additional_info7                                                    as AdditionalField7,
                   pd.additional_info8                                                    as AdditionalField8,
                   pd.additional_info9                                                    as AdditionalField9,
                   pd.additional_info10                                                   as AdditionalField10,

                   ap_1.label                                                             as AdditionalParametersLabel1,
                   ap_1.value                                                             as AdditionalParametersValue1,
                   ap_2.label                                                             as AdditionalParametersLabel2,
                   ap_2.value                                                             as AdditionalParametersValue2,
                   ap_3.label                                                             as AdditionalParametersLabel3,
                   ap_3.value                                                             as AdditionalParametersValue3,
                   ap_4.label                                                             as AdditionalParametersLabel4,
                   ap_4.value                                                             as AdditionalParametersValue4,
                   ap_5.label                                                             as AdditionalParametersLabel5,
                   ap_5.value                                                             as AdditionalParametersValue5,
                   ap_6.label                                                             as AdditionalParametersLabel6,
                   ap_6.value                                                             as AdditionalParametersValue6,
                   ap_7.label                                                             as AdditionalParametersLabel7,
                   ap_7.value                                                             as AdditionalParametersValue7,
                   ap_8.label                                                             as AdditionalParametersLabel8,
                   ap_8.value                                                             as AdditionalParametersValue8,
                   ap_9.label                                                             as AdditionalParametersLabel9,
                   ap_9.value                                                             as AdditionalParametersValue9,
                   ap_10.label                                                            as AdditionalParametersLabel10,
                   ap_10.value                                                            as AdditionalParametersValue10,
                   ap_11.label                                                            as AdditionalParametersLabel11,
                   ap_11.value                                                            as AdditionalParametersValue11,
                   ap_12.label                                                            as AdditionalParametersLabel12,
                   ap_12.value                                                            as AdditionalParametersValue12,

                   replace(text(pcd.type), '_', ' ')                                      as ContractDocumentType,

                   cvt.agg_names                                                          as ContractVersionType,

                   pc.contract_number                                                     as ContractNumber,
                   case
                       when pcd.additional_agreement_suffix is not null then
                           '#' || text(pcd.additional_agreement_suffix) end               as AdditionalSuffix,
                   cast(pc.create_date as date)                                           as CreationDate,
                   pc.signing_date                                                        as SigningDate,
                   pc.entry_into_force_date                                               as EntryForceDate,
                   pc.initial_term_start_date                                             as ContractTermStartDate,
                   pcd.start_date                                                         as VersionStartDate,
                   ir.name                                                                as ApplicableInterestRate,
                   camp.name                                                              as Campaign,
                   b.name                                                                 as ContractBank,
                   b.bic                                                                  as ContractBIC,
                   pcd.iban                                                               as ContractIBAN,
                   cust_bank.name                                                         as CustomerBank,
                   cust_bank.bic                                                          as CustomerBIC,
                   cd.iban                                                                as CustomerIBAN,
                   text(pcd.estimated_total_consumption_under_contract_kwh)               as EstimatedContractConsumption,

                   employee.display_name || ' (' || employee.user_name || ')'             as Employee,
                   assistant.name                                                         as AssistingEmployee,
                   intermed.name                                                          as InternalIntermediary,
                   ext.name                                                               as ExternalIntermediary,

                   replace(text(contr_terms.contract_term_type), '_', '/')                as ContractTermType,
                   contr_terms.value                                                      as ContractTermValue,
                   replace(text(contr_terms.contract_term_period_type), '_', ' ')         as ContractTermValueType,
                   case when contr_terms.perpetuity_cause = true then 'YES' else 'NO' end as ContractTermPerpetuity,
                   text(contr_terms.number_of_renewals)                                   as ContractTermRenewal,
                   text(contr_terms.renewal_period_value)                                 as ContractTermRenewalValue,
                   replace(text(contr_terms.renewal_period_type), '_', '/')               as ContractTermRenewalType,
                   replace(text(payment_terms.type), '_', ' ')                            as PaymentTermType,
                   text(pcd.invoice_payment_term_value)                                   as PaymentTermValue,
                   text(terms.contract_delivery_activation_value)                         as TermActivationDeliveryValue,
                   replace(text(terms.contract_delivery_activation_type), '_', ' ')       as TermActivationDeliveryType,
                   text(terms.resigning_deadline_value)                                   as DeadlineEarlyResigningValue,
                   replace(text(terms.resigning_deadline_type), '_', ' ')                 as DeadlineEarlyResigningType,
                   text(terms.general_notice_period_value)                                as GeneralNoticePeriodValue,
                   replace(text(terms.general_notice_period_type), '_', ' ')              as GeneralNoticePeriodType,
                   text(terms.notice_term_period_value)                                   as NoticeTermValue,
                   replace(text(terms.notice_term_period_type), '_', ' ')                 as NoticeTermType,
                   text(terms.notice_term_disconnection_period_value)                     as NoticeTermDisconnectionValue,
                   replace(text(terms.notice_term_disconnection_period_type), '_',
                           ' ')                                                           as NoticeTermDisconnectionType,
                   replace(text(pcd.supply_activation_after_contract_resigning), '_',
                           ' ')                                                           as SupplyActivationType,
                   text(pcd.supply_activation_value)                                      as SupplyActivationValue,
                   replace(text(pcd.wait_for_old_contract_term_to_expire), '_', ' ')      as WaitOldContractToExpire,
                   replace(text(pcd.entry_into_force), '_', ' ')                          as EntryIntoForceType,
                   text(pc.entry_into_force_date)                                         as EntryIntoForceValue,
                   replace(text(pcd.start_initial_term), '_', ' ')                        as StartInitialTermType,
                   text(pc.initial_term_start_date)                                       as StartInitialTermValue,
                   iap.yn                                                                 as InterimAdvancePaymentsYN,
                   iap.interims                                                           as InterimAdvancePaymentsList,
                   pcd.marginal_price                                                     as MarginalPrice,
                   pcd.marginal_price_validity                                            as MarginalPriceValidity,
                   pcd.avg_hourly_load_profiles                                           as Pav,
                   pcd.procurement_price                                                  as ProcurementPrice,
                   pcd.cost_price_increase_from_imbalances                                as ImbalancesPrice,
                   pcd.set_margin                                                         as SetMargin,
                   sc.contracts                                                           as ServiceContracts
            from product_contract.contract_details pcd
                     join product_contract.contracts pc on pcd.contract_id = pc.id
                     join lateral (select replace(text(c.customer_type), '_', ' '),
                                          cd.*,
                                          lf.name                                 as legal_form_name,
                                          case
                                              when c.customer_type = 'PRIVATE_CUSTOMER'
                                                  then concat(cd.name, ' ', cd.middle_name, ' ', cd.last_name)
                                              else concat(cd.name, ' ', lf.name)
                                              end                                 as customer_name_comb,
                                          case
                                              when c.customer_type = 'PRIVATE_CUSTOMER'
                                                  then concat(cd.name_transl, ' ', cd.middle_name_transl, ' ', cd.last_name_transl)
                                              else concat(cd.name_transl, ' ', lf.name)
                                              end                                 as customer_name_comb_trsl,
                                          c.identifier                            as customer_identifier,
                                          c.customer_number,
                                          translation.translate_text(replace(text(c.customer_type), '_', ' '),
                                                                     'BULGARIAN') as customer_type
                                   from customer.customer_details cd
                                            join customer.customers c on cd.customer_id = c.id
                                            left join nomenclature.legal_forms lf on cd.legal_form_id = lf.id
                                   where pcd.customer_detail_id = cd.id) cd on true
                     join product.product_details pd on pcd.product_detail_id = pd.id
                     join product.products p on pd.product_id = p.id
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
                                        where pcd.customer_detail_id = cd.id) customer on true
                     left join cc_address_formatter contr_cc on contr_cc.id = pcd.customer_communication_id_for_contract
                     left join cc_address_formatter bil_cc on bil_cc.id = pcd.customer_communication_id_for_billing
                     left join contact_aggregator contr_contacts on contr_contacts.customer_communication_id = contr_cc.id
                     left join contact_aggregator bil_contacts on bil_contacts.customer_communication_id = bil_cc.id
                     left join lateral (select array_agg(distinct seg.name) filter (where seg.id is not null) as customer_segments
                                        from customer.customer_details cd
                                                 left join customer.customer_segments cs
                                                           on cd.id = cs.customer_detail_id and cs.status = 'ACTIVE'
                                                 join nomenclature.segments seg on cs.segment_id = seg.id
                                        where pcd.customer_detail_id = cd.id) seg on true
                     left join nomenclature.currencies deposit_curr on pcd.cash_deposit_currency_id = deposit_curr.id
                     left join nomenclature.currencies b_g_curr on pcd.bank_guarantee_currency_id = b_g_curr.id
                     left join additional_params ap_1 on pd.id = ap_1.product_detail_id and ap_1.ordering_id = 0
                     left join additional_params ap_2 on pd.id = ap_2.product_detail_id and ap_2.ordering_id = 1
                     left join additional_params ap_3 on pd.id = ap_3.product_detail_id and ap_3.ordering_id = 2
                     left join additional_params ap_4 on pd.id = ap_4.product_detail_id and ap_4.ordering_id = 3
                     left join additional_params ap_5 on pd.id = ap_5.product_detail_id and ap_5.ordering_id = 4
                     left join additional_params ap_6 on pd.id = ap_6.product_detail_id and ap_6.ordering_id = 5
                     left join additional_params ap_7 on pd.id = ap_7.product_detail_id and ap_7.ordering_id = 6
                     left join additional_params ap_8 on pd.id = ap_8.product_detail_id and ap_8.ordering_id = 7
                     left join additional_params ap_9 on pd.id = ap_9.product_detail_id and ap_9.ordering_id = 8
                     left join additional_params ap_10 on pd.id = ap_10.product_detail_id and ap_10.ordering_id = 9
                     left join additional_params ap_11 on pd.id = ap_11.product_detail_id and ap_11.ordering_id = 10
                     left join additional_params ap_12 on pd.id = ap_12.product_detail_id and ap_12.ordering_id = 11
                     left join lateral (select string_agg(vt.name, ', ') as agg_names
                                        from product_contract.contract_version_types cvt
                                                 join nomenclature.contract_version_types vt
                                                      on cvt.contract_version_type_id = vt.id
                                        where cvt.status = 'ACTIVE'
                                          and cvt.contract_detail_id = pcd.id) cvt on true
                     left join banks b on pcd.bank_id = b.id
                     left join banks cust_bank on cd.bank_id = cust_bank.id
                     left join interest_rate.interest_rates ir on pcd.applicable_interest_rate = ir.id
                     left join nomenclature.campaigns camp on pcd.campaign_id = camp.id
                     left join customer.account_managers employee on pcd.employee_id = employee.id
                     left join lateral (select string_agg(am.display_name || ' (' || am.user_name || ')', ';') as name
                                        from product_contract.contract_assisting_employees ae
                                                 join customer.account_managers am
                                                      on am.id = ae.account_manager_id
                                        where ae.status = 'ACTIVE'
                                          and ae.contract_detail_id = pcd.id) assistant on true
                     left join lateral ( select string_agg(am.display_name || ' (' || am.user_name || ')', ';') as name
                                         from product_contract.contract_internal_intermediaries ae
                                                  join customer.account_managers am
                                                       on am.id = ae.account_manager_id
                                         where ae.status = 'ACTIVE'
                                           and ae.contract_detail_id = pcd.id) intermed on true
                     left join lateral (select array_agg(ei.name) as name
                                        from product_contract.contract_external_intermediaries ex
                                                 join nomenclature.external_intermediaries ei
                                                      on ex.external_intermediary_id = ei.id
                                        where ex.status = 'ACTIVE'
                                          and ex.contract_detail_id = pcd.id) ext on true
                     left join product.product_contract_terms contr_terms on pcd.product_contract_term_id = contr_terms.id
                     left join terms.invoice_payment_terms payment_terms on pcd.invoice_payment_term_id = payment_terms.id
                     left join terms.terms terms on payment_terms.term_id = terms.id
                     left join lateral (select case when count(1) > 0 then 'YES' else 'NO' end as yn,
                                               array_agg(interim order by priority)            as interims
                                        from (select iap.name as interim,
                                                     1        as priority
                                              from product_contract.contract_interim_advance_payments ciap
                                                       join interim_advance_payment.interim_advance_payments iap
                                                            on ciap.interim_advance_payment_id = iap.id
                                              where ciap.status = 'ACTIVE'
                                                and ciap.contract_detail_id = pcd.id
                                              union
                                              select iap.name as interim,
                                                     2        as priority
                                              from product.product_interim_advance_payment_groups piapg
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
                                                       join product_contract.contract_details cd
                                                            on cd.product_detail_id = piapg.product_detail_id
                                                       join product_contract.contracts contr on cd.contract_id = contr.id
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
                     left join lateral (select string_agg(sc.contract_number || '/' || to_char(sc.create_date, 'DD.MM.YYYY'),
                                                          ', ') as contracts
                                        from product_contract.contract_related_service_contracts rsc
                                                 join service_contract.contracts sc on rsc.related_service_contract_id = sc.id
                                        where rsc.status = 'ACTIVE'
                                          and rsc.contract_id = :id) sc on true
            where pc.id = :id
              and pcd.version_id = :versionId
            """)
    ContractMainResponse fetchContractInfoForDocument(Long id, Integer versionId);

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
                   pc.contract_number                     as ContractNumber,
                   cast(pc.create_date as date)           as ContractDate,
                   pd.printing_name                       as ContractProductName,
                   text(pcd.contract_type)                as ContractType,
                   pc.termination_date                    as ContractTerminationDate,
                   pc.termination_date + interval '1 day' as ContractTerminationDatePlus1,
                   pc.termination_date                    as CalculatedTerminationDate,
                   pc.termination_date + interval '1 day' as CalculatedTerminationDatePlus1,
                   text(t.event)                          as EventType,
                   cb.id                                  as CustomerDetailId
            from product_contract.contract_details pcd
                     join product_contract.contracts pc on pcd.contract_id = pc.id
                     join product.product_details pd on pcd.product_detail_id = pd.id
                     left join customer_base cb
                               on cb.id = pcd.customer_detail_id
                     left join segment_info si on si.customer_detail_id = cb.id
                     left join cc_address_formatter contr_cc on contr_cc.id = pcd.customer_communication_id_for_contract
                     left join cd_address_formatter customer on customer.id = cb.id
                     left join product.terminations t on t.id = :terminationId
            where pcd.id = :contractDetailId
            """)
    TerminationEmailDocumentResponse fetchTerminationEmailResponse(Long contractDetailId, Long terminationId);

    @Query(
            value =
                    """
                            select max(max_date) from (
                                select max(coalesce(i.meter_reading_period_to,i.invoice_date)) max_date from invoice.invoices i
                                where  i.type in('STANDARD','INTERIM_AND_ADVANCE_PAYMENT') and i.product_contract_id  =:_contract_id
                                        union
                                select max(coalesce( b.max_end_date , b.invoice_date)) max_date from billing.billings b inner join
                                billing_run.run_contracts rc on rc.run_id = b.id and rc.contract_type ='PRODUCT_CONTRACT'
                                where rc.contract_id  =:_contract_id
                             	) as max_date_subquery""",
            nativeQuery = true
    )
    LocalDate getContractLockDate(@Param("_contract_id") Long productContractId);
}
