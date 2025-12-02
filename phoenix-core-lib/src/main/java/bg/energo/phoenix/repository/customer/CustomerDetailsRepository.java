package bg.energo.phoenix.repository.customer;

import bg.energo.phoenix.model.CacheObject;
import bg.energo.phoenix.model.CacheObjectForCustomerDetails;
import bg.energo.phoenix.model.entity.customer.CustomerDetails;
import bg.energo.phoenix.model.enums.customer.CustomerDetailStatus;
import bg.energo.phoenix.model.enums.customer.CustomerStatus;
import bg.energo.phoenix.model.enums.customer.CustomerType;
import bg.energo.phoenix.model.request.customer.CustomerVersionsResponse;
import bg.energo.phoenix.model.response.contract.productContract.CustomerDetailsShortResponse;
import bg.energo.phoenix.model.response.customer.CustomerAddressResponse;
import bg.energo.phoenix.model.response.customer.CustomerShortResponse;
import bg.energo.phoenix.model.response.customer.CustomerVersionShortResponse;
import bg.energo.phoenix.model.response.customer.list.CustomerRelatedContractListResponse;
import bg.energo.phoenix.model.response.customer.list.CustomerRelatedOrderListResponse;
import bg.energo.phoenix.model.response.receivable.deposit.DepositCustomerResponse;
import bg.energo.phoenix.model.response.receivable.latePaymentFine.LatePaymentCustomerShortResponse;
import bg.energo.phoenix.model.response.shared.ShortResponse;
import jakarta.persistence.QueryHint;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface CustomerDetailsRepository extends JpaRepository<CustomerDetails, Long> {

    Optional<List<CustomerDetails>> findByCustomerIdAndStatusOrderByIdDesc(Long customerId, CustomerDetailStatus status);

    Optional<CustomerDetails> findFirstByCustomerId(@Param("customerId") Long customerId, Sort sort);

    Optional<CustomerDetails> findByCustomerId(Long customerId);

    Optional<CustomerDetails> findByCustomerIdAndVersionId(@Param("customerId") Long customerId, @Param("versionId") Long versionId);

    @Query("""
            select cd
            from CustomerDetails cd
            join Customer c on cd.customerId = c.id
            where cd.customerId = :customerId
            and cd.versionId = :versionId
            and c.status in (:statuses)
            """)
    Optional<CustomerDetails> findByCustomerIdAndVersionIdAndStatusIn(@Param("customerId") Long customerId, @Param("versionId") Long versionId, @Param("statuses") List<CustomerStatus> statuses);

    @Query(value = "SELECT max(c.versionId) FROM CustomerDetails c where c.customerId = :customerId")
    Optional<Long> findMaxVersionIdByCustomerId(Long customerId);


    @Query(
            value = """
                    select new bg.energo.phoenix.model.request.customer.CustomerVersionsResponse(
                        cd.versionId,
                        cd.createDate
                    )
                    from CustomerDetails cd
                        where cd.customerId = :customerId
                        and cd.status in (:statuses)
                        order by cd.versionId
                    """
    )
    List<CustomerVersionsResponse> getVersions(@Param("customerId") Long customerId, @Param("statuses") List<CustomerDetailStatus> statuses);


    @Query("""
                select new bg.energo.phoenix.model.response.customer.CustomerShortResponse(
                    c.id,
                    c.identifier,
                    c.customerType,
                    cd.businessActivity,
                    lf.name,
                    cd.name,
                    cd.middleName,
                    cd.lastName
                )
                from Customer c
                join CustomerDetails cd on c.id = cd.customerId
                left join LegalForm lf on lf.id = cd.legalFormId
                    where c.identifier = :identifier
                    and c.customerType in :types
                    and c.status in :statuses
                    order by cd.versionId desc
            """)
    Optional<CustomerShortResponse> findFirstByCustomerIdentifierAndStatus(
            @Param("identifier") String identifier,
            @Param("statuses") List<CustomerStatus> statuses,
            @Param("types") List<CustomerType> types,
            PageRequest pageRequest
    );


    @Query(
            value = """
                    select count(cam.id) > 0 from CustomerAccountManager cam
                    join AccountManager am on am.id = cam.managerId
                        where cam.customerDetail.id = :customerDetailId
                        and am.userName = :userName
                        and cam.status = 'ACTIVE'
                        and am.status = 'ACTIVE'
                    """
    )
    boolean isManagerInCustomerAccountManagers(
            @Param("userName") String userName,
            @Param("customerDetailId") Long customerDetailId
    );


    @Query("""
            select count(cd.id) > 0 from CustomerDetails cd
            join Customer c on cd.customerId=c.id
            where cd.id=:detailId
            and c.status in (:statuses)
            
            """)
    boolean existsByDetailIdAndCustomerStatus(Long detailId, List<CustomerStatus> statuses);


    @Query(
            value = """
                    select new bg.energo.phoenix.model.response.contract.productContract.CustomerDetailsShortResponse(cd.id,cd.versionId,cd.createDate,cd.name,cd.middleName,cd.lastName
                    )from CustomerDetails cd 
                    where cd.customerId = :customerId
                    order by cd.versionId desc"""
    )
    List<CustomerDetailsShortResponse> findCustomerDetailsForCustomer(@Param("customerId") Long customerId);

    @Query(
            value = """
                    select new bg.energo.phoenix.model.response.receivable.deposit.DepositCustomerResponse(cd.id,cd.customerId,cd.versionId,cd.createDate,cd.name,cd.middleName,cd.lastName, lf.name, c.identifier
                    )from CustomerDetails cd 
                    join Customer c on c.id = cd.customerId
                    left join LegalForm lf on lf.id = cd.legalFormId
                    where cd.customerId = :customerId
                    order by cd.versionId desc"""
    )
    List<DepositCustomerResponse> findCustomerDetailsForDeposit(@Param("customerId") Long customerId);

    @Query("""
            select new bg.energo.phoenix.model.CacheObjectForCustomerDetails(c.id,cd.versionId,cd.id,c.customerType,cd.businessActivity)
            from CustomerDetails cd 
            join Customer c on c.id=cd.customerId
            where c.identifier=:customerIdentifier
            and cd.versionId=:customerVersion
            """)
    @QueryHints(@QueryHint(name = "org.hibernate.cacheable", value = "true"))
    Optional<CacheObjectForCustomerDetails> findCacheObjectByIdentifier(String customerIdentifier, Long customerVersion);

    @Query(
            value = """
                            select new bg.energo.phoenix.model.CacheObject(
                                cc.id,
                                cc.contactTypeName
                            )
                            from CustomerCommContactPurposes cccp
                            join ContactPurpose cp on cccp.contactPurposeId = cp.id
                            join CustomerCommunications cc on cc.id=cccp.customerCommunicationsId
                            where cc.customerDetailsId = :customerDetailsId
                            and cp.id = :purposeId
                            and cp.status = 'ACTIVE'
                            and cc.status = 'ACTIVE'
                            and cccp.status = 'ACTIVE'
                            and exists(
                                select 1 from CustomerCommunicationContacts ccc
                                    where ccc.customerCommunicationsId = cc.id
                                    and ccc.status = 'ACTIVE'
                                    and ccc.contactType = 'EMAIL'
                            )
                            and exists(
                                select 1 from CustomerCommunicationContacts ccc
                                    where ccc.customerCommunicationsId = cc.id
                                    and ccc.status = 'ACTIVE'
                                    and ccc.contactType = 'MOBILE_NUMBER'
                            )
                            order by cc.createDate desc
                    """
    )
    @QueryHints(@QueryHint(name = "org.hibernate.cacheable", value = "true"))
    List<CacheObject> customerCommunicationDataCacheObjectList(
            @Param("customerDetailsId") Long customerDetailsId,
            @Param("purposeId") Long purposeId
    );

    @Query(value = """
            select coalesce(max('true'), 'false') have_contract_or_order
                from customer.customer_details cd
                where cd.id = :customerDetailId
                  and (exists(select 1
                              from goods_order.orders o
                              where o.customer_detail_id = cd.id
                                and o.status = 'ACTIVE'
                                and o.order_status in ('PAID', 'IN_EXECUTION'))
                    or
                       exists(select 1
                              from service_order.orders o
                              where o.customer_detail_id = cd.id
                                and o.status = 'ACTIVE'
                                and o.order_status in ('PAID', 'IN_EXECUTION'))
                    or
                       exists(select 1
                              from product_contract.contracts pc
                                       join product_contract.contract_details pcd
                                            on pcd.contract_id = pc.id
                                                and pcd.customer_detail_id = cd.id
                              where pc.status = 'ACTIVE'
                              and (pc.contract_status = 'SIGNED' and pc.contract_sub_status in ('SIGNED_BY_BOTH_SIDES','SPECIAL_PROCESSES')
                              or (pc.contract_status in ('ENTERED_INTO_FORCE',
                                                         'ACTIVE_IN_TERM',
                                                         'ACTIVE_IN_PERPETUITY')))
                                and pcd.start_date >= (select max(start_date)
                                                       from product_contract.contract_details cd3
                                                       where cd3.contract_id = pc.id
                                                         and cd3.start_date <= current_date))
                    or
                       exists(select 1
                              from service_contract.contracts sc
                                       join service_contract.contract_details scd
                                            on scd.contract_id = sc.id
                                                and scd.customer_detail_id = cd.id
                              where sc.status = 'ACTIVE'
                              and (sc.contract_status = 'SIGNED' and sc.contract_sub_status in ('SIGNED_BY_BOTH_SIDES','SPECIAL_PROCESSES')
                              or (sc.contract_status in ('ENTERED_INTO_FORCE',
                                                         'ACTIVE_IN_TERM',
                                                         'ACTIVE_IN_PERPETUITY')))
                                and scd.start_date >= (select max(start_date)
                                                       from service_contract.contract_details cd3
                                                       where cd3.contract_id = sc.id
                                                         and cd3.start_date <= current_date)))
            """, nativeQuery = true)
    boolean customerHasActiveContractsOrOrders(@Param("customerDetailId") Long customerDetailId);


//    @Query(nativeQuery = true,
//            value = """
//            select coalesce(max('true'),'false') have_contract_or_order from customer.customer_details cd
//            where cd.id = :customerDetailId
//            and
//            (exists (select 1 from goods_order.orders o where o.customer_detail_id = cd.id and o.status = 'ACTIVE')
//              or
//             exists (select 1 from service_order.orders o where o.customer_detail_id = cd.id and o.status = 'ACTIVE')
//              or
//             exists (select 1 from product_contract.contracts pc
//                      join product_contract.contract_details pcd
//                        on pcd.contract_id = pc.id
//                       and pcd.customer_detail_id = cd.id
//                      where pc.status = 'ACTIVE')
//              or
//             exists (select 1 from service_contract.contracts sc
//                      join service_contract.contract_details scd
//                        on scd.contract_id = sc.id
//                       and scd.customer_detail_id = cd.id
//                      where sc.status = 'ACTIVE')
//            );
//            """
//    )
//    boolean customerHasAtListOneContractorOrder(@Param("customerDetailId") Long customerDetailId);

    @Query(nativeQuery = true,
            value = """
                    select coalesce(max('true'),'false') have_contract_or_order from customer.customer_details cd
                    where cd.id = :customerDetailId
                    and
                    (exists (select 1 from goods_order.orders o where o.customer_detail_id = cd.id and o.status = 'ACTIVE' and o.order_status not in ('PAID', 'IN_EXECUTION'))
                      or
                     exists (select 1 from service_order.orders o where o.customer_detail_id = cd.id and o.status = 'ACTIVE' and o.order_status not in ('PAID', 'IN_EXECUTION'))
                      or
                     exists (select 1 from product_contract.contracts pc
                              join product_contract.contract_details pcd
                                on pcd.contract_id = pc.id
                               and pcd.customer_detail_id = cd.id
                              where pc.status = 'ACTIVE' and pc.contract_status <> 'TERMINATED')
                      or
                     exists (select 1 from service_contract.contracts sc
                              join service_contract.contract_details scd
                                on scd.contract_id = sc.id
                               and scd.customer_detail_id = cd.id
                              where sc.status = 'ACTIVE' and sc.contract_status <> 'TERMINATED')
                    );
                    """
    )
    boolean customerHasAtListOneContractorOrderThatIsNotTerminated(@Param("customerDetailId") Long customerDetailId);

    @Query(nativeQuery = true,
            value = """
                    select coalesce(max('true'),'false') have_contract_or_order from customer.customer_details cd
                    where cd.id = :customerDetailId
                    and
                    (exists (select 1 from product_contract.contracts pc
                              join product_contract.contract_details pcd
                                on pcd.contract_id = pc.id
                               and pcd.customer_detail_id = cd.id
                              where pc.status = 'ACTIVE' and pc.contract_status = 'TERMINATED')
                      or
                     exists (select 1 from service_contract.contracts sc
                              join service_contract.contract_details scd
                                on scd.contract_id = sc.id
                               and scd.customer_detail_id = cd.id
                              where sc.status = 'ACTIVE' and sc.contract_status = 'TERMINATED')
                    );
                    """
    )
    boolean customerHasAtListOneContractorOrderThatIsTerminated(@Param("customerDetailId") Long customerDetailId);


    @Query(
            nativeQuery = true,
            value = """
                    select * from (
                            select
                                concat(pc.contract_number,'/',date(pc.create_date)) as contractNumber,
                                pcd.version_id as version,
                                'PRODUCT_CONTRACT' as contractType,
                                pd.name as contractName,
                                pc.signing_date as dateOfSigning,
                                text(pc.contract_status) as contractStatus,
                                text(pc.contract_sub_status) as contractSubStatus,
                                pc.activation_date as activationDate,
                                pc.contract_term_end_date as contractTermEndDate,
                                pc.entry_into_force_date as entryIntoForceDate,
                                date(pc.create_date) as creationDate,
                                pc.create_date as createDateForSort,
                                pc.id as contractId,
                                pcd.id as contractDetailId,
                                text(pc.status) as status
                            from product_contract.contract_details pcd
                            join product_contract.contracts pc on pcd.contract_id = pc.id
                            join product.product_details pd on pcd.product_detail_id = pd.id
                                and pcd.start_date = (select max(start_date)
                                    from product_contract.contract_details cd1
                                    where cd1.contract_id = pc.id
                                    and cd1.start_date <= current_date)
                                and exists(
                                    select 1 from product_contract.contract_details cd6
                                    where cd6.contract_id = pc.id
                                    and cd6.customer_detail_id = :customerDetailId
                                )
                        union
                            select
                                concat(sc.contract_number,'/',date(sc.create_date)) as contractNumber,
                                scd.version_id as version,
                                'SERVICE_CONTRACT' as contractType,
                                sd.name as contractName,
                                sc.signing_date as dateOfSigning,
                                text(sc.contract_status) as contractStatus,
                                text(sc.contract_sub_status) as contractSubStatus,
                                null as activationDate,
                                sc.contract_term_end_date as contractTermEndDate,
                                sc.entry_into_force_date as entryIntoForceDate,
                                date(sc.create_date) as creationDate,
                                sc.create_date as createDateForSort,                              
                                sc.id as contractId,
                                scd.id as contractDetailId,
                                text(sc.status) as status
                            from service_contract.contract_details scd
                            join service_contract.contracts sc on scd.contract_id = sc.id
                            join service.service_details sd on scd.service_detail_id = sd.id
                                and scd.start_date = (select max(start_date)
                                    from service_contract.contract_details cd1
                                    where cd1.contract_id = sc.id
                                    and cd1.start_date <= current_date)
                                and exists(
                                    select 1 from service_contract.contract_details cd7
                                    where cd7.contract_id = sc.id
                                    and cd7.customer_detail_id = :customerDetailId
                                )                           
                    ) as pc
                        where pc.status in (:statuses)
                        and ((:contractStatuses) is null or pc.contractStatus in (:contractStatuses))
                        and ((:contractSubStatuses) is null or pc.contractSubStatus in (:contractSubStatuses))
                        and ((:contractTypes) is null or pc.contractType in (:contractTypes))
                        and (date(:signingDateFrom) is null or pc.dateOfSigning >= date(:signingDateFrom))
                        and (date(:signingDateTo) is null or pc.dateOfSigning <= date(:signingDateTo))
                        and (date(:activationDateFrom) is null or pc.activationDate >= date(:activationDateFrom))
                        and (date(:activationDateTo) is null or pc.activationDate <= date(:activationDateTo))
                        and (date(:contractTermEndDateFrom) is null or pc.contractTermEndDate >= date(:contractTermEndDateFrom))
                        and (date(:contractTermEndDateTo) is null or pc.contractTermEndDate <= date(:contractTermEndDateTo))
                        and (date(:entryIntoForceDateFrom) is null or pc.entryIntoForceDate >= date(:entryIntoForceDateFrom))
                        and (date(:entryIntoForceDateTo) is null or pc.entryIntoForceDate <= date(:entryIntoForceDateTo))
                        and (date(:creationDateFrom) is null or pc.creationDate >= date(:creationDateFrom))
                        and (date(:creationDateTo) is null or pc.creationDate <= date(:creationDateTo))
                        and (
                            :prompt is null or (
                                :searchBy = 'ALL' and (
                                    lower(pc.contractNumber) like :prompt
                                )
                            )
                            or (
                                (:searchBy = 'CONTRACT_NUMBER' and lower(pc.contractNumber) like :prompt)
                            )
                        )
                    """,
            countQuery = """
                    select count(1) from (
                            select
                                concat(pc.contract_number,'/',date(pc.create_date)) as contractNumber,
                                pcd.version_id as version,
                                'PRODUCT_CONTRACT' as contractType,
                                pd.name as contractName,
                                pc.signing_date as dateOfSigning,
                                text(pc.contract_status) as contractStatus,
                                text(pc.contract_sub_status) as contractSubStatus,
                                pc.activation_date as activationDate,
                                pc.contract_term_end_date as contractTermEndDate,
                                pc.entry_into_force_date as entryIntoForceDate,
                                date(pc.create_date) as creationDate,
                                pc.create_date as createDateForSort,
                                pc.id as contractId,
                                pcd.id as contractDetailId,
                                text(pc.status) as status
                            from product_contract.contract_details pcd
                            join product_contract.contracts pc on pcd.contract_id = pc.id
                            join product.product_details pd on pcd.product_detail_id = pd.id
                                and pcd.start_date = (select max(start_date)
                                    from product_contract.contract_details cd1
                                    where cd1.contract_id = pc.id
                                    and cd1.start_date <= current_date)
                                and exists(
                                    select 1 from product_contract.contract_details cd6
                                    where cd6.contract_id = pc.id
                                    and cd6.customer_detail_id = :customerDetailId
                                )
                        union
                            select
                                concat(sc.contract_number,'/',date(sc.create_date)) as contractNumber,
                                scd.version_id as version,
                                'SERVICE_CONTRACT' as contractType,
                                sd.name as contractName,
                                sc.signing_date as dateOfSigning,
                                text(sc.contract_status) as contractStatus,
                                text(sc.contract_sub_status) as contractSubStatus,
                                null as activationDate,
                                sc.contract_term_end_date as contractTermEndDate,
                                sc.entry_into_force_date as entryIntoForceDate,
                                date(sc.create_date) as creationDate,
                                sc.create_date as createDateForSort,
                                sc.id as contractId,
                                scd.id as contractDetailId,
                                text(sc.status) as status
                            from service_contract.contract_details scd
                            join service_contract.contracts sc on scd.contract_id = sc.id
                            join service.service_details sd on scd.service_detail_id = sd.id
                                and scd.start_date = (select max(start_date)
                                    from service_contract.contract_details cd1
                                    where cd1.contract_id = sc.id
                                    and cd1.start_date <= current_date)
                                and exists(
                                    select 1 from service_contract.contract_details cd7
                                    where cd7.contract_id = sc.id
                                    and cd7.customer_detail_id = :customerDetailId
                                )
                    ) as pc
                        where pc.status in (:statuses)
                        and ((:contractStatuses) is null or pc.contractStatus in (:contractStatuses))
                        and ((:contractSubStatuses) is null or pc.contractSubStatus in (:contractSubStatuses))
                        and ((:contractTypes) is null or pc.contractType in (:contractTypes))
                        and (date(:signingDateFrom) is null or pc.dateOfSigning >= date(:signingDateFrom))
                        and (date(:signingDateTo) is null or pc.dateOfSigning <= date(:signingDateTo))
                        and (date(:activationDateFrom) is null or pc.activationDate >= date(:activationDateFrom))
                        and (date(:activationDateTo) is null or pc.activationDate <= date(:activationDateTo))
                        and (date(:contractTermEndDateFrom) is null or pc.contractTermEndDate >= date(:contractTermEndDateFrom))
                        and (date(:contractTermEndDateTo) is null or pc.contractTermEndDate <= date(:contractTermEndDateTo))
                        and (date(:entryIntoForceDateFrom) is null or pc.entryIntoForceDate >= date(:entryIntoForceDateFrom))
                        and (date(:entryIntoForceDateTo) is null or pc.entryIntoForceDate <= date(:entryIntoForceDateTo))
                        and (date(:creationDateFrom) is null or pc.creationDate >= date(:creationDateFrom))
                        and (date(:creationDateTo) is null or pc.creationDate <= date(:creationDateTo))
                        and (
                            :prompt is null or (
                                :searchBy = 'ALL' and (
                                    lower(pc.contractNumber) like :prompt
                                )
                            )
                            or (
                                (:searchBy = 'CONTRACT_NUMBER' and lower(pc.contractNumber) like :prompt)
                            )
                        )
                    """
    )
    Page<CustomerRelatedContractListResponse> getCustomerRelatedContracts(
            @Param("customerDetailId") Long customerDetailId,
            @Param("statuses") List<String> statuses,
            @Param("prompt") String prompt,
            @Param("searchBy") String searchBy,
            @Param("contractTypes") List<String> contractTypes,
            @Param("contractStatuses") List<String> contractStatuses,
            @Param("contractSubStatuses") List<String> contractSubStatuses,
            @Param("signingDateFrom") LocalDate dateOfSigningFrom,
            @Param("signingDateTo") LocalDate dateOfSigningTo,
            @Param("activationDateFrom") LocalDate activationDateFrom,
            @Param("activationDateTo") LocalDate activationDateTo,
            @Param("contractTermEndDateFrom") LocalDate contractTermEndDateFrom,
            @Param("contractTermEndDateTo") LocalDate contractTermEndDateTo,
            @Param("entryIntoForceDateFrom") LocalDate entryIntoForceDateFrom,
            @Param("entryIntoForceDateTo") LocalDate entryIntoForceDateTo,
            @Param("creationDateFrom") LocalDate creationDateFrom,
            @Param("creationDateTo") LocalDate creationDateTo,
            Pageable pageable
    );


    @Query(
            nativeQuery = true,
            value = """
                    select * from (
                      with invoice_data as (select i.service_order_id,
                                                   max(i.payment_deadline)  as payment_deadline,
                                                   sum(case
                                                           when c.main_ccy_start_date <= current_date
                                                               and c.main_ccy = true
                                                               and c.status = 'ACTIVE'
                                                               and c.main_ccy_start_date = (select max(c2.main_ccy_start_date)
                                                                                            from nomenclature.currencies c2
                                                                                            where c2.main_ccy_start_date <= current_date
                                                                                              and c2.main_ccy = true
                                                                                              and c2.status = 'ACTIVE')
                                                               then i.total_amount_including_vat
                                                           else 0 end)     as total_amount_including_vat,
                                                   sum(case
                                                           when c.main_ccy_start_date <= current_date
                                                               and c.main_ccy = true
                                                               and c.status = 'ACTIVE'
                                                               and c.main_ccy_start_date = (select max(c2.main_ccy_start_date)
                                                                                            from nomenclature.currencies c2
                                                                                            where c2.main_ccy_start_date <= current_date
                                                                                              and c2.main_ccy = true
                                                                                              and c2.status = 'ACTIVE')
                                                               then 0
                                                           else i.total_amount_including_vat_in_other_currency end) as total_amount_including_vat_other_currency,
                                                   sum(case when l.current_amount <> 0 then 1 else 0 end)  as unpaid_count
                                            from invoice.invoices i
                                                     left join receivable.customer_liabilities l on l.invoice_id = i.id
                                                     left join nomenclature.currencies c on i.currency_id = c.id
                                                     inner join service_order.orders o on o.id = i.service_order_id
                                            where i.status = 'REAL'
                                              and i.type = 'STANDARD'
                                             and i.customer_detail_id = :customerDetailId
                                            group by i.service_order_id)
                        select
                            concat(o.order_number,'/',to_char(o.create_date,'dd.mm.yyyy')) as orderNumber,
                            'SERVICE_ORDER' as orderType,
                            text(o.order_status) as orderStatus,
                            date(o.create_date) as creationDate,
                            o.create_date as createDateForSort,
                             invd.payment_deadline as invoiceMaturityDate, --from billing module
                             case
                                 when invd.unpaid_count > 0 then false
                                 when invd.unpaid_count = 0 then true
                                 end        as invoicePaid, -- from billing module
                            case
                              when invd.total_amount_including_vat_other_currency > 0
                              then invd.total_amount_including_vat_other_currency
                              else invd.total_amount_including_vat
                              end as orderValue, -- will be implemented later
                            text(o.status) as status,
                            o.id as orderId,
                            case when o.invoice_payment_term_id is not null
                             then (select name from terms.invoice_payment_terms ipt where ipt.id = o.invoice_payment_term_id and ipt.status = 'ACTIVE')
                            else
                    		(select
                                            ipt.name
                                             from terms.terms t
                                             join terms.invoice_payment_terms ipt on ipt.term_id = t.id
                                             join terms.term_group_terms tgt on tgt.term_id = t.id
                                             join terms.term_group_details tgd on tgt.term_group_detail_id = tgd.id
                                             join terms.term_groups tg on tgd.group_id = tg.id
                                             where sd.term_group_id = tg.id
                                               and t.status = 'ACTIVE'
                                               and ipt.status = 'ACTIVE'
                                               and tgt.status = 'ACTIVE'
                                               and tg.status = 'ACTIVE'
                                               and tgd.start_date = (select max(start_date) from terms.term_group_details tgd3
                                                                     where tgd3.group_id = tg.id
                                                                       and tgd3.start_date <= current_date
                                               ) ) end invoicePaymentTerm
                        from customer.customer_details cd
                        join service_order.orders o on o.customer_detail_id = cd.id
                        join service.service_details sd on o.service_detail_id = sd.id
                        join service.services ss on sd.service_id = ss.id
                        left join invoice_data invd on invd.service_order_id = o.id
                            where cd.id = :customerDetailId
                    union
                        select
                            concat(o.order_number,'/',to_char(o.create_date,'dd.mm.yyyy')) as orderNumber,
                            'GOODS_ORDER' as orderType,
                            text(o.order_status) orderStatus,
                            date(o.create_date) as creationDate,
                            o.create_date as createDateForSort,
                            i.payment_deadline as invoiceMaturityDate, -- from billing module
                            case
                               when i.id is not null and l.id is not null
                                then (case when l.current_amount <> 0 then false else true end) end as invoicePaid, -- from billing module
                            (select sum(og.price * og.quantity)
                                from goods_order.order_goods og
                                where og.order_id = o.id) as orderValue,
                            text(o.status) as status,
                            o.id as orderId,
                            opt.name
                        from customer.customer_details cd
                        join goods_order.orders o on o.customer_detail_id = cd.id
                        left join invoice.invoices i on (i.goods_order_id = o.id and i.status = 'REAL' and i.type = 'STANDARD')
                        left join goods_order.order_payment_terms opt on opt.order_id = o.id and opt.status = 'ACTIVE'
                        left join receivable.customer_liabilities l on (l.invoice_id = i.id and l.status = 'ACTIVE')      
                            where cd.id = :customerDetailId
                    ) as pc
                        where pc.status in (:statuses)
                        and ((:orderStatuses) is null or pc.orderStatus in :orderStatuses)
                        and ((:orderTypes) is null or pc.orderType in :orderTypes)
                        and (:invoicePaid is null or :invoicePaid = 'ALL'
                           or (:invoicePaid = 'YES' and pc.invoicePaid = true)
                           or (:invoicePaid = 'NO' and pc.invoicePaid = false))
                        and (date(:invoiceMaturityDateFrom) is null or pc.invoiceMaturityDate >= date(:invoiceMaturityDateFrom))
                        and (date(:invoiceMaturityDateTo) is null or pc.invoiceMaturityDate <= date(:invoiceMaturityDateTo))
                        and (date(:creationDateFrom) is null or date(pc.creationDate) >= date(:creationDateFrom))
                        and (date(:creationDateTo) is null or date(pc.creationDate) <= date(:creationDateTo))
                        and (
                            :prompt is null or (
                                :searchBy = 'ALL' and (
                                    lower(pc.orderNumber) like :prompt
                                )
                            )
                            or (
                                (:searchBy = 'ORDER_NUMBER' and lower(pc.orderNumber) like :prompt)
                            )
                        )
                    """,
            countQuery = """
                    select count(1) from (
                      with invoice_data as (select i.service_order_id,
                                                   max(i.payment_deadline)  as payment_deadline,
                                                   sum(case
                                                           when c.main_ccy_start_date <= current_date
                                                               and c.main_ccy = true
                                                               and c.status = 'ACTIVE'
                                                               and c.main_ccy_start_date = (select max(c2.main_ccy_start_date)
                                                                                            from nomenclature.currencies c2
                                                                                            where c2.main_ccy_start_date <= current_date
                                                                                              and c2.main_ccy = true
                                                                                              and c2.status = 'ACTIVE')
                                                               then i.total_amount_including_vat
                                                           else 0 end)     as total_amount_including_vat,
                                                   sum(case
                                                           when c.main_ccy_start_date <= current_date
                                                               and c.main_ccy = true
                                                               and c.status = 'ACTIVE'
                                                               and c.main_ccy_start_date = (select max(c2.main_ccy_start_date)
                                                                                            from nomenclature.currencies c2
                                                                                            where c2.main_ccy_start_date <= current_date
                                                                                              and c2.main_ccy = true
                                                                                              and c2.status = 'ACTIVE')
                                                               then 0
                                                           else i.total_amount_including_vat_in_other_currency end) as total_amount_including_vat_other_currency,
                                                   sum(case when l.current_amount <> 0 then 1 else 0 end)  as unpaid_count
                                            from invoice.invoices i
                                                     left join receivable.customer_liabilities l on l.invoice_id = i.id
                                                     left join nomenclature.currencies c on i.currency_id = c.id
                                                     inner join service_order.orders o on o.id = i.service_order_id
                                            where i.status = 'REAL'
                                              and i.type = 'STANDARD'
                                             and i.customer_detail_id = :customerDetailId
                                            group by i.service_order_id)                      
                        select
                            concat(o.order_number,'/',to_char(o.create_date,'dd.mm.yyyy')) as orderNumber,
                            'SERVICE_ORDER' as orderType,
                            text(o.order_status) as orderStatus,
                            date(o.create_date) as creationDate,
                            o.create_date as createDateForSort,
                            invd.payment_deadline as invoiceMaturityDate, --from billing module
                             case
                                 when invd.unpaid_count > 0 then false
                                 when invd.unpaid_count = 0 then true
                                 end        as invoicePaid, -- from billing module
                            case
                              when invd.total_amount_including_vat_other_currency > 0
                              then invd.total_amount_including_vat_other_currency
                              else invd.total_amount_including_vat
                              end as orderValue, -- will be implemented later
                            text(o.status) as status,
                            o.id as orderId
                        from customer.customer_details cd
                        join service_order.orders o on o.customer_detail_id = cd.id
                        left join invoice_data invd on invd.service_order_id = o.id
                            where cd.id = :customerDetailId
                    union
                        select
                            concat(o.order_number,'/',to_char(o.create_date,'dd.mm.yyyy')) as orderNumber,
                            'GOODS_ORDER' as orderType,
                            text(o.order_status) orderStatus,
                            date(o.create_date) as creationDate,
                            o.create_date as createDateForSort,
                            inv.payment_deadline  as invoiceMaturityDate,--from billing module
                             case
                                when inv.id is not null and l.id is not null
                                  then (case when l.current_amount <> 0 then false else true end) end as invoicePaid, -- from billing module
                            (select sum(og.price * og.quantity)
                                from goods_order.order_goods og
                                where og.order_id = o.id) as orderValue,
                            text(o.status) as status,
                            o.id as orderId
                        from customer.customer_details cd
                        join goods_order.orders o on o.customer_detail_id = cd.id
                        left join invoice.invoices inv on o.id = inv.goods_order_id and inv.status = 'REAL' and inv.type = 'STANDARD'
                        left join receivable.customer_liabilities l on (l.invoice_id = inv.id and l.status = 'ACTIVE')
                        left join goods_order.order_payment_terms opt on opt.order_id = o.id and opt.status = 'ACTIVE'
                            where cd.id = :customerDetailId
                    ) as pc
                        where pc.status in (:statuses)
                        and ((:orderStatuses) is null or pc.orderStatus in :orderStatuses)
                        and ((:orderTypes) is null or pc.orderType in :orderTypes)
                        and (:invoicePaid is null or :invoicePaid = 'ALL'
                             or (:invoicePaid = 'YES' and pc.invoicePaid = true)
                             or (:invoicePaid = 'NO' and pc.invoicePaid = false))
                        and (date(:invoiceMaturityDateFrom) is null or pc.invoiceMaturityDate >= date(:invoiceMaturityDateFrom))
                        and (date(:invoiceMaturityDateTo) is null or pc.invoiceMaturityDate <= date(:invoiceMaturityDateTo))
                        and (date(:creationDateFrom) is null or date(pc.creationDate) >= date(:creationDateFrom))
                        and (date(:creationDateTo) is null or date(pc.creationDate) <= date(:creationDateTo))
                        and (
                            :prompt is null or (
                                :searchBy = 'ALL' and (
                                    lower(pc.orderNumber) like :prompt
                                )
                            )
                            or (
                                (:searchBy = 'ORDER_NUMBER' and lower(pc.orderNumber) like :prompt)
                            )
                        )
                    """
    )
    Page<CustomerRelatedOrderListResponse> getCustomerRelatedOrders(
            @Param("customerDetailId") Long customerDetailId,
            @Param("statuses") List<String> statuses,
            @Param("prompt") String prompt,
            @Param("searchBy") String searchBy,
            @Param("orderTypes") List<String> orderTypes,
            @Param("orderStatuses") List<String> orderStatuses,
            @Param("invoiceMaturityDateFrom") LocalDate invoiceMaturityDateFrom,
            @Param("invoiceMaturityDateTo") LocalDate invoiceMaturityDateTo,
            @Param("creationDateFrom") LocalDate creationDateFrom,
            @Param("creationDateTo") LocalDate creationDateTo,
            @Param("invoicePaid") String invoicePaid,
            Pageable pageable
    );

    @Query("""
            SELECT exists (
                            select 1
                            from ProductContractDetails pdd
                            join ProductContract pc on pc.id = pdd.contractId
                            where pdd.customerDetailId = cd.id
                            and pc.status = 'ACTIVE'
                        )
                    or exists (
                            select 1
                            from ServiceContractDetails scd
                            left join ServiceContracts sc on scd.contractId = sc.id
                            where scd.customerDetailId = cd.id
                            and sc.status = 'ACTIVE'
                        )
                    or exists (
                            select 1
                            from ServiceOrder so
                            where so.customerDetailId = cd.id
                            and so.status = 'ACTIVE'
                        )
                    or exists (
                            select 1
                            from GoodsOrder go
                            where go.customerDetailId = cd.id
                            and go.status = 'ACTIVE'
                        )
            from CustomerDetails cd
            where cd.id = :id
            """)
    @QueryHints(@QueryHint(name = "org.hibernate.cacheable", value = "true"))
    Boolean checkForBoundObjects(Long id);

    @Query("""
            select new bg.energo.phoenix.model.response.customer.CustomerVersionShortResponse(
                c.id,
                case when c.customerType = 'LEGAL_ENTITY' then concat(c.identifier, ' (', cd.name, ')')
                    else replace(concat(c.identifier , ' (',coalesce(cd.name, '') , ' ', coalesce(cd.middleName, '') , ' ', coalesce(cd.lastName, ''), ')'), '\\s+', ' ') end,
                cd.versionId
            )
            from Customer c
            join CustomerDetails cd on cd.customerId = c.id
            where cd.id = :id
            """)
    Optional<CustomerVersionShortResponse> findByCustomerDetailsId(Long id);

    @Query(value = """
            select legFor.name
            from customer.customer_details cusDet
                     join nomenclature.legal_forms legFor
                          on (cusDet.legal_form_id = legFor.id)
            where cusDet.id = :cusDetId""", nativeQuery = true)
    String getLegalFormName(Long cusDetId);

    @Query("""
            select new bg.energo.phoenix.model.response.shared.ShortResponse(
                c.id,
                case when c.customerType = 'LEGAL_ENTITY' then concat(c.identifier, ' (', cd.name, coalesce(lf.name, ''), ')')
                    else replace(concat(c.identifier , ' (',coalesce(cd.name, '') , ' ', coalesce(cd.middleName, '') , ' ', coalesce(cd.lastName, ''), ')'), '\\s+', ' ') end
            )
            from Customer c
            join CustomerDetails cd on cd.customerId = c.id
            left join LegalForm lf on lf.id = cd.legalFormId
            where cd.id = :id
            """)
    Optional<ShortResponse> findByCustomerDetailsIdTemp(Long id);

    boolean existsByIdAndCustomerId(Long customerDetailId, Long customerId);

    @Query("""
                    select cd from Customer c
                    join CustomerDetails cd on cd.customerId=c.id
                    where c.identifier=:customerIdentifier
                    and cd.versionId=:versionId
                    and c.status='ACTIVE'
            """)
    Optional<CustomerDetails> findByCustomerIdentifierAndVersionId(String customerIdentifier, Long versionId);

    @Query("""
                    select cd
                            from CustomerDetails cd
                            join Customer customer on cd.id = customer.lastCustomerDetailId
                            where customer.identifier = :customerIdentifier
                            and customer.status ='ACTIVE'
            """)
    Optional<CustomerDetails> findLastCustomerDetail(String customerIdentifier);

    @Query("""
                                select new bg.energo.phoenix.model.response.receivable.latePaymentFine.LatePaymentCustomerShortResponse(
                                    c.id,
                                    c.identifier,
                                    c.customerType,
                                    cd.name,
                                    cd.middleName,
                                    cd.lastName,
                                    lf.name
                                )
                                from CustomerDetails cd
                                join Customer c on c.id = cd.customerId
                                    and c.lastCustomerDetailId = cd.id
                                left join LegalForm lf on lf.id = cd.legalFormId
                                where cd.customerId = :customerId
            
            """)
    LatePaymentCustomerShortResponse findCustomerForLiabilityShortResponse(@Param("customerId") Long customerId);

    @Query("""
            SELECT c.lastCustomerDetailId
            FROM Customer c
            WHERE c.id = :customerId
            """)
    Long findLastCustomerDetailIdByCustomerId(@Param("customerId") Long customerId);

    @Query(value = """
            SELECT DISTINCT
                CASE
                    WHEN c.customer_type = 'PRIVATE_CUSTOMER'
                        THEN concat_ws(' ', cd.name, cd.middle_name, cd.last_name)
                    ELSE concat_ws(' ', cd.name, lf.name)
                    END as customer_name_comb,
                CASE
                    WHEN c.customer_type = 'PRIVATE_CUSTOMER'
                        THEN concat_ws(' ', cd.name_transl, cd.middle_name_transl, cd.last_name_transl)
                    ELSE concat_ws(' ', cd.name_transl, lf.name)
                    END as customer_name_comb_trsl,
                c.identifier as customer_identifier,
                c.customer_number as customer_number,
                CASE
                    WHEN cd.foreign_address IS TRUE THEN cd.populated_place_foreign
                    ELSE pp.name
                    END as populated_place,
                CASE
                    WHEN cd.foreign_address IS TRUE THEN cd.zip_code_foreign
                    ELSE zc.zip_code
                    END as zip_code,
                translation.translate_text(concat_ws(', ',
                                                     nullif(concat_ws(' ',
                                                                      CASE
                                                                          WHEN cd.foreign_address IS TRUE THEN cd.district_foreign
                                                                          ELSE d.name
                                                                          END,
                                                                      CASE
                                                                          WHEN cd.foreign_address IS TRUE THEN
                                                                              CASE
                                                                                  WHEN cd.foreign_residential_area_type IS NOT NULL
                                                                                      THEN concat(cd.foreign_residential_area_type, ' ', cd.residential_area_foreign)
                                                                                  ELSE cd.residential_area_foreign
                                                                                  END
                                                                          ELSE
                                                                              CASE
                                                                                  WHEN ra.type IS NOT NULL
                                                                                      THEN concat(ra.type, ' ', ra.name)
                                                                                  ELSE ra.name
                                                                                  END
                                                                          END
                                                            ), ''),
                                                     nullif(concat_ws(' ',
                                                                      CASE
                                                                          WHEN cd.foreign_address IS TRUE THEN cd.street_type
                                                                          ELSE s.type
                                                                          END,
                                                                      CASE
                                                                          WHEN cd.foreign_address IS TRUE THEN cd.street_foreign
                                                                          ELSE s.name
                                                                          END,
                                                                      cd.street_number
                                                            ), ''),
                                                     nullif(concat('бл. ', cd.block), 'бл. '),
                                                     nullif(concat('вх. ', cd.entrance), 'вх. '),
                                                     nullif(concat('ет. ', cd.floor), 'ет. '),
                                                     nullif(concat('ап. ', cd.apartment), 'ап. '),
                                                     nullif(cd.address_additional_info, '')
                                           ),text('BULGARIAN')) as address_comb
            FROM customer.customer_details cd
                     JOIN customer.customers c ON cd.customer_id = c.id
                     LEFT JOIN nomenclature.legal_forms lf ON cd.legal_form_id = lf.id
                     LEFT JOIN nomenclature.populated_places pp ON cd.populated_place_id = pp.id
                     LEFT JOIN nomenclature.zip_codes zc ON cd.zip_code_id = zc.id
                     LEFT JOIN nomenclature.districts d ON cd.district_id = d.id
                     LEFT JOIN nomenclature.streets s ON cd.street_id = s.id
                     LEFT JOIN nomenclature.residential_areas ra ON cd.residential_area_id = ra.id
            WHERE cd.id = :customerDetailId
            """, nativeQuery = true)
    Optional<CustomerAddressResponse> findCustomerAddressInfo(Long customerDetailId);

}
