package bg.energo.phoenix.repository.contract.order.service;

import bg.energo.phoenix.model.entity.EntityStatus;
import bg.energo.phoenix.model.entity.contract.order.service.ServiceOrder;
import bg.energo.phoenix.model.enums.contract.order.service.ServiceOrderStatus;
import bg.energo.phoenix.model.response.billing.billingRun.manualInvoice.ContractOrderShortResponse;
import bg.energo.phoenix.model.response.contract.order.service.ServiceOrderBasicParametersResponse;
import bg.energo.phoenix.model.response.contract.order.service.ServiceOrderForInvoiceResponse;
import bg.energo.phoenix.model.response.contract.order.service.ServiceOrderListResponse;
import bg.energo.phoenix.model.response.customer.CustomerCommunicationDataResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface ServiceOrderRepository extends JpaRepository<ServiceOrder, Long> {

    @Query(value = "select nextval('service_order.order_number_seq')", nativeQuery = true)
    Long getNextOrderNumber();


    Optional<ServiceOrder> findByIdAndStatusIn(Long id, List<EntityStatus> statuses);


    @Query(
            value = """
                    select new bg.energo.phoenix.model.response.contract.order.service.ServiceOrderBasicParametersResponse(
                        so,
                        sd,
                        ir,
                        c,
                        b,
                        cd,
                        cc,
                        cu,
                        lf.name
                    )
                    from ServiceOrder so
                    join ServiceDetails sd on so.serviceDetailId = sd.id
                    left join InterestRate ir on so.applicableInterestRateId = ir.id
                    left join Campaign c on so.campaignId = c.id
                    left join Bank b on so.bankId = b.id
                    join CustomerDetails cd on so.customerDetailId = cd.id
                    join CustomerCommunications cc on so.customerCommunicationIdForBilling = cc.id
                    join Customer cu on cd.customerId = cu.id
                    left join LegalForm lf on lf.id = cd.legalFormId
                        where so.id = :orderId
                    """
    )
    ServiceOrderBasicParametersResponse getBasicParametersByServiceOrder(@Param("orderId") Long orderId);


    @Query("select so.id from ServiceOrder so where so.id in :ids and so.status in :statuses")
    List<Long> findByIdInAndStatusIn(List<Long> ids, List<EntityStatus> statuses);

    @Query(
            nativeQuery = true,
            value = """
                    with invoice_data as (select i.service_order_id,
                                                 count(i.id)                                                               as invoice_count,
                                                 max(case when i.type = 'STANDARD' then i.payment_deadline end)            as payment_deadline,
                                                 sum(case
                                                         when i.type = 'STANDARD'
                                                             then i.total_amount_including_vat end)                        as total_amount_including_vat,
                                                 sum(case
                                                         when i.type = 'STANDARD'
                                                             then i.total_amount_including_vat_in_other_currency end)      as total_amount_including_vat_other_currency,
                                                 sum(case
                                                         when i.type = 'STANDARD'
                                                             then (case when l.current_amount <> 0 then 1 else 0 end) end) as unpaid_count
                                          from invoice.invoices i
                                                   left join receivable.customer_liabilities l on l.invoice_id = i.id
                                                   left join nomenclature.currencies c on i.currency_id = c.id
                                          where i.status = 'REAL'
                                          group by i.service_order_id)
                    select so.id                                                                    as id,
                           so.order_number || '/' || to_char(so.create_date, 'dd.mm.yyyy')          as orderNumber,
                           case
                               when c.customer_type = 'PRIVATE_CUSTOMER' then
                                   case
                                       when cd.middle_name is not null and cd.middle_name <> ''
                                           then cd.name || ' ' || cd.middle_name || ' ' || cd.last_name || ' (' || c.identifier || ')'
                                       else cd.name || ' ' || cd.last_name || ' (' || c.identifier || ')'
                                       end
                               when c.customer_type = 'LEGAL_ENTITY'
                                   then cd.name || ' ' || (select name from nomenclature.legal_forms lf where cd.legal_form_id = lf.id) ||
                                        ' (' || c.identifier || ')'
                               end                                                                  as customerName,
                           sd.name                                                                  as serviceName,
                           so.create_date                                                           as createDate,
                           invd.payment_deadline                                                    as invoiceMaturityDate,
                           case
                               when invd.unpaid_count > 0 then false
                               when invd.unpaid_count = 0 then true
                               end                                                                  as invoicePaid,
                           case
                               when :accountManagerSortDirection = 'ASC'
                                   then vwc.display_name
                               when :accountManagerSortDirection = 'DESC'
                                   then vwc.display_name_desc
                               else vwc.display_name
                               end                                                                  as accountManager,
                           case
                               when invd.total_amount_including_vat_other_currency > 0
                                   then invd.total_amount_including_vat_other_currency
                               else invd.total_amount_including_vat
                               end                                                                  as valueOfTheOrder,
                           so.status                                                                as status,
                           so.order_status                                                          as orderStatus,
                           (case when coalesce(invd.invoice_count, 0) = 0 then false else true end) as isLockedByInvoice
                    from service_order.orders so
                             join customer.customer_details cd on so.customer_detail_id = cd.id
                             join customer.customers c on cd.customer_id = c.id
                             join service.service_details sd on so.service_detail_id = sd.id
                             left join service_order.vw_order_customer_account_managers vwc on vwc.customer_detail_id = cd.id
                             left join invoice_data invd on invd.service_order_id = so.id
                    where text(so.status) in :statuses
                      and ((:serviceDetailIds) is null or so.service_detail_id in :serviceDetailIds)
                      and (date(:dateOfCreationFrom) is null or so.create_date >= date(:dateOfCreationFrom))
                      and (date(:dateOfCreationTo) is null or so.create_date <= date(:dateOfCreationTo))
                      and (:directDebit is null or so.direct_debit = :directDebit)
                      and ((:accountManagers) is null or exists(select 1
                                                                from customer.customer_account_managers cam
                                                                where cam.customer_detail_id = cd.id
                                                                  and cam.account_manager_id in (:accountManagers)
                                                                  and cam.status = 'ACTIVE'))
                      and (cast(:invoiceMaturityDateFrom as date) is null or
                           (cast(:invoiceMaturityDateFrom as date) <= invd.payment_deadline))
                      and (cast(:invoiceMaturityDateTo as date) is null or (cast(:invoiceMaturityDateTo as date) >= invd.payment_deadline))
                      and (:isInvoicePaid is null or (:isInvoicePaid = 'YES' and invd.unpaid_count = 0)
                        or (:isInvoicePaid = 'NO' and invd.unpaid_count > 0)
                        or (:isInvoicePaid = 'ALL' and (invd.unpaid_count >= 0)
                               ))
                      and (
                        :searchBy is null
                            or (
                            :searchBy = 'ALL' and
                            (lower(so.order_number) like :prompt or lower(cd.name) like :prompt or lower(c.identifier) like :prompt)
                            )
                            or (
                            (:searchBy = 'ORDER_NUMBER' and lower(so.order_number) like :prompt)
                                or (:searchBy = 'CUSTOMER_NAME' and lower(cd.name) like :prompt)
                                or (:searchBy = 'CUSTOMER_UIC_OR_PERSONAL_NUMBER' and lower(c.identifier) like :prompt)
                            )
                        )                   
                    """,
            countQuery = """
                    with invoice_data as (select i.service_order_id,
                                                 count(i.id)                                                               as invoice_count,
                                                 max(case when i.type = 'STANDARD' then i.payment_deadline end)            as payment_deadline,
                                                 sum(case
                                                         when i.type = 'STANDARD'
                                                             then i.total_amount_including_vat end)                        as total_amount_including_vat,
                                                 sum(case
                                                         when i.type = 'STANDARD'
                                                             then i.total_amount_including_vat_in_other_currency end)      as total_amount_including_vat_other_currency,
                                                 sum(case
                                                         when i.type = 'STANDARD'
                                                             then (case when l.current_amount <> 0 then 1 else 0 end) end) as unpaid_count
                                          from invoice.invoices i
                                                   left join receivable.customer_liabilities l on l.invoice_id = i.id
                                                   left join nomenclature.currencies c on i.currency_id = c.id
                                          where i.status = 'REAL'
                                          group by i.service_order_id)
                    select count(1)
                    from service_order.orders so
                             join customer.customer_details cd on so.customer_detail_id = cd.id
                             join customer.customers c on cd.customer_id = c.id
                             join service.service_details sd on so.service_detail_id = sd.id
                             left join service_order.vw_order_customer_account_managers vwc on vwc.customer_detail_id = cd.id
                             left join invoice_data invd on invd.service_order_id = so.id
                    where text(so.status) in :statuses
                      and ((:serviceDetailIds) is null or so.service_detail_id in :serviceDetailIds)
                      and (date(:dateOfCreationFrom) is null or so.create_date >= date(:dateOfCreationFrom))
                      and (date(:dateOfCreationTo) is null or so.create_date <= date(:dateOfCreationTo))
                      and (:directDebit is null or so.direct_debit = :directDebit)
                      and ((:accountManagers) is null or exists(select 1
                                                                from customer.customer_account_managers cam
                                                                where cam.customer_detail_id = cd.id
                                                                  and cam.account_manager_id in (:accountManagers)
                                                                  and cam.status = 'ACTIVE'))
                      and (cast(:invoiceMaturityDateFrom as date) is null or
                           (cast(:invoiceMaturityDateFrom as date) <= invd.payment_deadline))
                      and (cast(:invoiceMaturityDateTo as date) is null or (cast(:invoiceMaturityDateTo as date) >= invd.payment_deadline))
                      and (:isInvoicePaid is null or (:isInvoicePaid = 'YES' and invd.unpaid_count = 0)
                        or (:isInvoicePaid = 'NO' and invd.unpaid_count > 0)
                        or (:isInvoicePaid = 'ALL' and (invd.unpaid_count >= 0)
                               ))
                      and (
                        :searchBy is null
                            or (
                            :searchBy = 'ALL' and
                            (lower(so.order_number) like :prompt or lower(cd.name) like :prompt or lower(c.identifier) like :prompt)
                            )
                            or (
                            (:searchBy = 'ORDER_NUMBER' and lower(so.order_number) like :prompt)
                                or (:searchBy = 'CUSTOMER_NAME' and lower(cd.name) like :prompt)
                                or (:searchBy = 'CUSTOMER_UIC_OR_PERSONAL_NUMBER' and lower(c.identifier) like :prompt)
                            )
                        )
                    """
    )
    Page<ServiceOrderListResponse> list(
            @Param("prompt") String prompt,
            @Param("searchBy") String searchBy,
            @Param("accountManagerSortDirection") String accountManagerSortDirection,
            @Param("serviceDetailIds") List<Long> serviceDetailIds,
            @Param("dateOfCreationFrom") LocalDate dateOfCreationFrom,
            @Param("dateOfCreationTo") LocalDate dateOfCreationTo,
            @Param("invoiceMaturityDateFrom") LocalDate invoiceMaturityDateFrom,
            @Param("invoiceMaturityDateTo") LocalDate invoiceMaturityDateTo,
            @Param("isInvoicePaid") String isInvoicePaid,
            @Param("directDebit") Boolean directDebit,
            @Param("accountManagers") List<Long> accountManagers,
            @Param("statuses") List<String> statuses,
            Pageable pageable
    );

    @Query("select so from ServiceOrder so where so.id in :ids and so.status in :statuses")
    List<ServiceOrder> findAllByIdInAndStatusIn(List<Long> ids, List<EntityStatus> statuses);


    boolean existsByIdAndStatusIn(Long id, List<EntityStatus> statuses);
    boolean existsByIdAndOrderStatus(Long id, ServiceOrderStatus orderStatus);

    Optional<ServiceOrder> findByOrderNumberAndStatus(String orderNumber, EntityStatus status);

    @Query("""
            select count(sorpc.id) > 0
            from ProductContractRelatedServiceOrder sorpc
            join ProductContract pc on (sorpc.productContractId = pc.id and sorpc.serviceOrderId = :id)
            where sorpc.status = 'ACTIVE'
            and pc.status = 'ACTIVE'
            """)
    boolean hasConnectionToProductContract(Long id);

    @Query("""
            select count(scrso.id) > 0
            from ServiceContractRelatedServiceOrder scrso
            join ServiceContracts sc on (sc.id = scrso.serviceContractId and scrso.serviceOrderId = :id)
            where scrso.status = 'ACTIVE'
            and sc.status = 'ACTIVE'
            """)
    boolean hasConnectionToServiceContract(Long id);

    @Query("""
            select count(sorso.id) > 0
            from ServiceOrderRelatedServiceOrder sorso
            join ServiceOrder soSource on (soSource.id = sorso.serviceOrderId)
            join ServiceOrder soTarget on (soTarget.id = sorso.relatedServiceOrderId)
            where (sorso.serviceOrderId = :id or sorso.relatedServiceOrderId = :id)
            and sorso.status = 'ACTIVE'
            and soSource.status = 'ACTIVE'
            and soTarget.status = 'ACTIVE'
            """)
    boolean hasConnectionToServiceOrder(Long id);

    @Query("""
            select count(sorgo.id) > 0
            from ServiceOrderRelatedGoodsOrder sorgo
            join GoodsOrder go on (sorgo.goodsOrderId = go.id and sorgo.serviceOrderId = :id)
            where go.status = 'ACTIVE'
            and sorgo.status = 'ACTIVE'
            """)
    boolean hasConnectionToGoodsOrder(Long id);

    @Query("""
            select count(sot.id) > 0
            from ServiceOrderTask sot
            join Task t on t.id = sot.taskId
            where sot.orderId = :id
            and sot.status = 'ACTIVE'
            and t.status = 'ACTIVE'
            """)
    boolean hasConnectionToTask(Long id);

    @Query("""
            select count(soa.id) > 0
            from ServiceOrderActivity soa
            join SystemActivity sa on soa.systemActivityId = sa.id
            where soa.orderId = :id
            and soa.status = 'ACTIVE'
            and sa.status = 'ACTIVE'
            """)
    boolean hasConnectionToActivity(Long id);

    @Query(nativeQuery = true, value = "SELECT setval('service_order.order_number_seq', 1, true)")
    void resetOrderNumberSequence();

    @Query("""
             select new bg.energo.phoenix.model.response.customer.CustomerCommunicationDataResponse(
                         cc.id,
                         cc.contactTypeName,
                         cc.createDate
                     )
             from ServiceOrder so
             join CustomerCommunications cc on cc.id = so.customerCommunicationIdForBilling
             where so.id = :orderId
             and so.customerDetailId = :customerDetailsId
             and so.status = 'ACTIVE'
             and cc.status = 'ACTIVE'
            """)
    Optional<CustomerCommunicationDataResponse> findCommunicationDataByOrderIdAndCustomerDetailId(
            @Param("orderId") Long orderId,
            @Param("customerDetailsId") Long customerDetailsId);

    @Query("""
            select new bg.energo.phoenix.model.response.billing.billingRun.manualInvoice.ContractOrderShortResponse(so.id, so.orderNumber)
            from ServiceOrder so
            where so.id = :id
            and so.status = :status
            """)
    Optional<ContractOrderShortResponse> findByIdAndStatus(@Param("id") Long serviceOrderId,
                                                           @Param("status") EntityStatus entityStatus);
    @Query("""
             select new bg.energo.phoenix.model.response.customer.CustomerCommunicationDataResponse(
                         cc.id,
                         cc.contactTypeName,
                         cc.createDate
                     )
             from ServiceOrder so
             join CustomerCommunications cc on cc.id = so.customerCommunicationIdForBilling
             where so.id = :orderId
             and cc.id = :id
             and so.customerDetailId = :customerDetailsId
             and so.status = 'ACTIVE'
            """)
    Optional<CustomerCommunicationDataResponse> findCommunicationDataByIdAndOrderIdAndCustomerDetailId(@Param("id") Long customerCommunicationId,
                                                                                                       @Param("orderId") Long serviceOrderId,
                                                                                                       @Param("customerDetailsId") Long customerDetailId);
    @Query("""
            select count(so.id) > 0
            from ServiceOrder so
            join CustomerDetails cu on so.customerDetailId = cu.id
            where so.id = :orderId
            and cu.customerId = :customerId
            and so.status = 'ACTIVE'
        """)
    boolean existsByOrderIdAndCustomerId(Long orderId, Long customerId);

    @Query(nativeQuery = true, value = """
            with pod_count as (select o.id                         as order_id,
                                      (select count(*) from service_order.order_pods where order_id = o.id and status = 'ACTIVE') +
                                      (select count(*)
                                       from service_order.order_unrecognized_pods
                                       where order_id = o.id
                                         and status = 'ACTIVE')    as count,
                                      (select string_agg(distinct text(op.pod_id), ';')
                                       from service_order.order_pods op
                                       where op.order_id = o.id
                                         and op.status = 'ACTIVE') as podIds,
                                      (select string_agg(distinct op.pod_identifier, ';')
                                       from service_order.order_unrecognized_pods op
                                       where op.order_id = o.id
                                         and op.status = 'ACTIVE') as unrecognizedPods
                               from service_order.orders o)
            select so.id                                                             as id,
                   sd.id                                                             as serviceDetailId,
                   sd.income_account_number                                          as incomeAccountNumber,
                   sd.cost_center_controlling_order                                  as costCenterControllingOrder,
                   so.applicable_interest_rate_id                                    as applicableInterestRateId,
                   case
                       when so.direct_debit = true then so.direct_debit
                       else cd.direct_debit end                                      as directDebit,
                   case
                       when so.direct_debit = true then so.bank_id
                       else case when cd.direct_debit = true then cd.bank_id end end as bankId,
                   case
                       when so.direct_debit = true then so.iban
                       else case when cd.direct_debit = true then cd.iban end end    as iban,
                   so.customer_detail_id                                             as customerDetailId,
                   so.customer_communication_id_for_billing                          as customerCommunicationId,
                   cd.customer_id                                                       as customerId,
                   vr.id                                                             as vatRateId,
                   vr.value_in_percent                                               as vatRatePercent,
                   sd.currency_id                                                    as currencyId,
                   so.quantity                                                       as quantity,
                   so.invoice_payment_term_value                                     as paymentTermValue,
                   coalesce((select td.id
                             from template.template_details td
                             where td.template_id = so.invoice_template_id
                               and td.start_date = (select max(innerTD.start_date)
                                                    from template.template_details innerTD
                                                    where innerTD.template_id = so.invoice_template_id
                                                      and innerTD.start_date <= current_date)),
                            (select td.id
                             from template.template_details td
                                      join service.service_templates st
                                           on (st.service_detail_id = sd.id and st.service_template_type = 'INVOICE_TEMPLATE' and
                                               st.status = 'ACTIVE')
                             where td.template_id = st.template_id
                               and td.start_date = (select max(innerTD.start_date)
                                                    from template.template_details innerTD
                                                    where innerTD.template_id = st.template_id
                                                      and innerTD.start_date <= current_date))
                   )                                                                 as templateDetailId,
                   pods.count                                                        as podQuantity,
                   pods.podIds                                                       as recognizedPodIds,
                   pods.unrecognizedPods                                             as unrecognizedPods
            from service_order.orders so
                     join service.service_details sd on so.service_detail_id = sd.id
                     join nomenclature.vat_rates vr
                          on ((sd.global_vat_rate = false and sd.vat_rate_id = vr.id)
                              or (sd.global_vat_rate = true and vr.status = 'ACTIVE' and vr.start_date =
                                                                                         (select max(innerVr.start_date)
                                                                                          from nomenclature.vat_rates innerVr
                                                                                          where innerVr.start_date <= current_date
                                                                                            and innerVr.status = 'ACTIVE'
                                                                                            and innerVr.global_vat_rate = true)))
                     join customer.customer_details cd on cd.id = so.customer_detail_id
                     left join template.template_details ctd1 on ctd1.template_id = so.invoice_template_id
                     left join pod_count pods on pods.order_id = so.id
            where so.id = :id
              and so.status = 'ACTIVE'
              and so.order_status = 'CONFIRMED'
              and not exists (select 1
                              from invoice.invoices i
                              where i.service_order_id = :id
                                and i.status <> 'CANCELLED'
                                and i.type = 'STANDARD')
            """)
    Optional<ServiceOrderForInvoiceResponse> findByOrderIdAndOrderStatusAndNonExistentInvoice(@Param("id") Long id);

    @Query("""
                select so
                from ServiceOrder so
                where so.status = 'ACTIVE'
                and so.id = :id
                and so.orderStatus in (:statuses)
            """)
    Optional<ServiceOrder> findByIdAndOrderStatus(@Param("id") Long id,
                                                  @Param("statuses")List<ServiceOrderStatus> orderStatuses);

    @Query(value = """
            select so.*
            from service_order.orders so
            join invoice.invoices i on so.id = i.service_order_id
            where so.status = 'ACTIVE'
            and so.order_status = 'AWAITING_PAYMENT'
            and so.prepayment_term_in_calendar_days < extract(day from current_date)
            and i.invoice_date + interval '1 day' * so.prepayment_term_in_calendar_days < current_date
            and i.status = 'REAL'
            and i.document_type = 'PROFORMA_INVOICE'
            and not exists(select 1
                               from invoice.invoices inv
                                        join receivable.customer_liabilities cl on cl.invoice_id = inv.id
                               where inv.service_order_id = so.id
                                 and inv.billing_id is null
                                 and cl.current_amount = 0)
              """, nativeQuery = true)
    List<ServiceOrder> findOrdersWithOverdueInvoices();
    @Query(value = """
            select not exists(select 1
                              from invoice.invoices inv
                                       join receivable.customer_liabilities cl on cl.invoice_id = inv.id
                              where inv.service_order_id = :id
                                and inv.billing_id is null
                                and inv.status <> 'CANCELLED'
                                and cl.current_amount > 0)
            """, nativeQuery = true)
    boolean allInvoicesPaid(Long id);
    @Query(value = """
            select exists(select 1
                          from invoice.invoices inv
                                   join receivable.customer_liabilities cl on cl.invoice_id = inv.id
                          where inv.service_order_id = :id
                            and inv.billing_id is null
                            and inv.status <> 'CANCELLED'
                            and cl.current_amount = 0)
            """, nativeQuery = true)
    boolean anyInvoicePaid(Long id);

}
