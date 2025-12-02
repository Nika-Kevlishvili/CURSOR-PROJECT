package bg.energo.phoenix.repository.contract.order.goods;

import bg.energo.phoenix.model.entity.EntityStatus;
import bg.energo.phoenix.model.entity.contract.order.goods.GoodsOrder;
import bg.energo.phoenix.model.enums.contract.order.goods.GoodsOrderStatus;
import bg.energo.phoenix.model.response.billing.billingRun.manualInvoice.ContractOrderShortResponse;
import bg.energo.phoenix.model.response.contract.order.goods.GoodsOrderForInvoiceResponse;
import bg.energo.phoenix.model.response.contract.order.goods.GoodsOrderListingMiddleResponse;
import bg.energo.phoenix.model.response.contract.productContract.FilteredContractOrderEntityResponse;
import bg.energo.phoenix.model.response.customer.CustomerCommunicationDataResponse;
import bg.energo.phoenix.model.response.customer.CustomerContractOrderResponse;
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
public interface GoodsOrderRepository extends JpaRepository<GoodsOrder, Long> {

    @Query("select go.id from GoodsOrder go where go.id in :ids and go.status in :statuses")
    List<Long> findByIdInAndStatusIn(List<Long> ids, List<EntityStatus> statuses);

    List<GoodsOrder> findAllByIdInAndStatusIn(List<Long> goodsOrderIds, List<EntityStatus> statuses);

    Optional<GoodsOrder> findByIdAndStatusIn(Long id, List<EntityStatus> statuses);


    @Query(
            nativeQuery = true,
            value = """
                select *
                from (
                    select
                        order_number as number,
                        case
                            when c.customer_type = 'PRIVATE_CUSTOMER' then
                                case
                                    when cd.middle_name is not null and cd.middle_name <> ''
                                        then cd.name||' '||cd.middle_name||' '||cd.last_name ||' ('||c.identifier||')'
                                    else cd.name || ' ' || cd.last_name || ' (' || c.identifier || ')'
                                end
                            when c.customer_type = 'LEGAL_ENTITY'     then cd.name||' ('||c.identifier||')' end customerName,
                        'GOODS_ORDER' as type,
                        o.id
                    from goods_order.orders o
                    join customer.customer_details cd on o.customer_detail_id = cd.id
                    join customer.customers c on cd.customer_id = c.id
                    where (:prompt is null or lower(o.order_number) like :prompt)
                      and o.status ='ACTIVE'
                    union all
                    select
                        order_number as number,
                        case
                            when c.customer_type = 'PRIVATE_CUSTOMER' then
                                case
                                    when cd.middle_name is not null and cd.middle_name <> ''
                                        then cd.name||' '||cd.middle_name||' '||cd.last_name ||' ('||c.identifier||')'
                                    else cd.name || ' ' || cd.last_name || ' (' || c.identifier || ')'
                                end
                            when c.customer_type = 'LEGAL_ENTITY'     then cd.name||' ('||c.identifier||')' end customerName,
                        'SERVICE_ORDER' as type,
                        o.id
                    from service_order.orders o
                    join customer.customer_details cd on o.customer_detail_id = cd.id
                    join customer.customers c on cd.customer_id = c.id
                    where (:prompt is null or lower(o.order_number) like :prompt)
                      and o.status ='ACTIVE'
                ) as union_query
                order by customerName
                """,
            countQuery = """
                select count(*)
                from (
                    select
                        order_number as number
                    from goods_order.orders o
                    join customer.customer_details cd on o.customer_detail_id = cd.id
                    join customer.customers c on cd.customer_id = c.id
                    where (:prompt is null or lower(o.order_number) like :prompt)
                      and o.status ='ACTIVE'
                    union all
                    select
                        order_number as number
                    from service_order.orders o
                    join customer.customer_details cd on o.customer_detail_id = cd.id
                    join customer.customers c on cd.customer_id = c.id
                    where (:prompt is null or lower(o.order_number) like :prompt)
                      and o.status ='ACTIVE'
                ) as count_query
                """
    )
    Page<FilteredContractOrderEntityResponse> filterOrders(@Param("prompt") String prompt, Pageable pageable);


    boolean existsByIdAndStatusIn(Long id, List<EntityStatus> statuses);

    @Query(nativeQuery = true,
            value = """
                    with main_curr as (select id, alt_ccy_exchange_rate
                                       from nomenclature.currencies
                                       where main_ccy_start_date <= current_date
                                         and main_ccy = true
                                         and status = 'ACTIVE'
                                       order by main_ccy_start_date desc
                                       limit 1)
                    select o.id                                                                        as id,
                           o.status                                                                    as status,
                           o.order_status                                                              as orderStatus,
                           concat(order_number, '/', to_char(o.create_date, 'dd.mm.yyyy'))             as orderNumber,
                           case
                               when c.customer_type = 'PRIVATE_CUSTOMER'
                                   then
                                   concat(cd.name, ' ', case when cd.middle_name is not null then cd.middle_name end, ' ',
                                          case when cd.last_name is not null then cd.last_name end, ' (' || c.identifier || ')')
                               when c.customer_type = 'LEGAL_ENTITY' then concat(cd.name, ' ', (select name
                                                                                                from nomenclature.legal_forms lf
                                                                                                where cd.legal_form_id = lf.id),
                                                                                 ' (',
                                                                                 c.identifier,
                                                                                 ')') end                 customer,
                           (case
                                when :goodsNameDirection = 'ASC' then vog.goods_name
                                when :goodsNameDirection = 'DESC' then vog.goods_name_Desc
                                else vog.goods_name end)                                               as goods,
                           (case
                                when :goodsSupplierDirection = 'ASC' then vogs.goods_supplier
                                when :goodsSupplierDirection = 'DESC' then vogs.goods_supplier_desc
                                else vogs.goods_supplier end)                                          as goodsSupplier,
                           o.create_date                                                               as orderCreationDate,
                           opt.name                                                                    as paymentTerm,
                           (CASE WHEN COALESCE(inv.id, 0) = 0 THEN FALSE ELSE TRUE END)                as isLockedByInvoice,
                           i.payment_deadline                                                          as invoiceMaturityDate,--from billing module
                           case
                               when i.id is not null and l.id is not null
                                   then (case when l.current_amount <> 0 then false else true end) end as invoicePaid,--from billing module
                           case
                               when i.id is null then (select sum(
                                                                      case
                                                                          when mc.id = coalesce(og.currency_id, gd2.currency_id)
                                                                              then
                                                                              coalesce(og.price, gd2.price) * og.quantity
                                                                          else
                                                                              coalesce(og.price, gd2.price) *
                                                                              (select c1.alt_ccy_exchange_rate
                                                                               from nomenclature.currencies c1
                                                                               where c1.id = coalesce(og.currency_id, gd2.currency_id)) *
                                                                              og.quantity
                                                                          end)
                                                       from goods_order.order_goods og
                                                                left join
                                                            goods.goods_details gd2
                                                            on og.goods_details_id = gd2.id
                                                       where og.order_id = o.id)
                               else
                                   (case
                                        when i.currency_id = mc.id then i.total_amount_including_vat
                                        else i.total_amount_including_vat_in_other_currency end) end
                                                                                                       as orderValue
                    from goods_order.orders o
                             join customer.customer_details cd on o.customer_detail_id = cd.id
                             join customer.customers c on cd.customer_id = c.id
                             left join goods_order.order_payment_terms opt on opt.order_id = o.id and opt.status = 'ACTIVE'
                             left join goods_order.vw_order_goods vog on vog.order_id = o.id
                             left join goods_order.vw_order_goods_suppliers vogs on vogs.order_id = o.id
                             left join invoice.invoices i on (i.goods_order_id = o.id and i.status = 'REAL' and i.type = 'STANDARD')
                             left join invoice.invoices inv on o.id = inv.goods_order_id and inv.status = 'REAL'
                             left join receivable.customer_liabilities l on (l.invoice_id = i.id and l.status = 'ACTIVE')
                             cross join main_curr mc
                    where cast(o.status as text) in (:statuses)
                      and (coalesce(:goodsName, '0') = '0'
                        or lower(vog.goods_name) like (:goodsName)
                        )
                      and ((:goodsIds) is null
                        or exists(select 1
                                  from goods.goods_details gd
                                           join goods_order.order_goods og2
                                                on og2.goods_details_id = gd.id
                                  where og2.order_id = o.id
                                    and gd.status = 'ACTIVE'
                                    and gd.goods_id in (:goodsIds))
                        )
                      and ((:goodsSupplierIds) is null
                        or exists(select 1
                                  from goods.goods_details gd
                                           join goods_order.order_goods og2
                                                on og2.goods_details_id = gd.id
                                  where og2.order_id = o.id
                                    and gd.status = 'ACTIVE'
                                    and gd.goods_suppliers_id in (:goodsSupplierIds))
                        )
                      and (
                        (cast(:dateOfOrderCreationFrom as date) is null and cast(:dateOfOrderCreationTo as date) is null)
                            or
                        (cast(:dateOfOrderCreationFrom as date) is not null and cast(:dateOfOrderCreationTo as date) is not null and
                         date(o.create_date) between cast(:dateOfOrderCreationFrom as date) and cast(:dateOfOrderCreationTo as date))
                            or
                        (cast(:dateOfOrderCreationFrom as date) is not null and cast(:dateOfOrderCreationTo as date) is null and
                         date(o.create_date) >= cast(:dateOfOrderCreationFrom as date))
                            or
                        (cast(:dateOfOrderCreationFrom as date) is null and cast(:dateOfOrderCreationTo as date) is not null and
                         date(o.create_date) <= cast(:dateOfOrderCreationTo as date))
                        )
                      and (coalesce(:directDebit, '0') = '0' or o.direct_debit = :directDebit)
                      and (:invoicePaid is null or (:invoicePaid = 'YES' and l.id is not null and l.current_amount = 0) or
                           (:invoicePaid = 'NO' and l.id is not null and l.current_amount <> 0)
                        or (:invoicePaid = 'ALL' and (l.id is not null and l.current_amount = 0)))
                      and (cast(:invoiceMaturityDateFrom as date) is null or
                           (i.id is not null and cast(i.payment_deadline as date) >= cast(:invoiceMaturityDateFrom as date)))
                      and (cast(:invoiceMaturityDateTo as date) is null or
                           (i.id is not null and cast(i.payment_deadline as date) <= cast(:invoiceMaturityDateTo as date)))
                      and ((:accountManagerIds) is null
                        or exists(select 1
                                  from customer.customer_account_managers cam
                                  where cam.customer_detail_id = cd.id
                                    and cam.account_manager_id in (:accountManagerIds)
                                    and cam.status = 'ACTIVE')
                        )
                      and (:prompt is null
                        or (:searchBy = 'ALL'
                            and
                            (
                                lower(o.order_number) like :prompt
                                    or lower(cd.name) like :prompt
                                    or c.identifier like :prompt
                                )
                               )
                        or (
                               (:searchBy = 'ORDER_NUMBER' and lower(o.order_number) like :prompt)
                                   or (:searchBy = 'CUSTOMER_NAME' and lower(cd.name) like :prompt)
                                   or (:searchBy = 'CUSTOMER_UIC_OR_PERSONAL_NUMBER' and lower(c.identifier) like :prompt)
                               )
                        )
                    """,
            countQuery = """
                    with main_curr as (select id, alt_ccy_exchange_rate
                                       from nomenclature.currencies
                                       where main_ccy_start_date <= current_date
                                         and main_ccy = true
                                         and status = 'ACTIVE'
                                       order by main_ccy_start_date desc
                                       limit 1)
                    select count(1)
                    from goods_order.orders o
                             join customer.customer_details cd on o.customer_detail_id = cd.id
                             join customer.customers c on cd.customer_id = c.id
                             left join goods_order.order_payment_terms opt on opt.order_id = o.id and opt.status = 'ACTIVE'
                             left join goods_order.vw_order_goods vog on vog.order_id = o.id
                             left join goods_order.vw_order_goods_suppliers vogs on vogs.order_id = o.id
                             left join invoice.invoices i on (i.goods_order_id = o.id and i.status = 'REAL' and i.type = 'STANDARD')
                             left join invoice.invoices inv on o.id = inv.goods_order_id and inv.status = 'REAL'
                             left join receivable.customer_liabilities l on (l.invoice_id = i.id and l.status = 'ACTIVE')
                             cross join main_curr mc
                    where cast(o.status as text) in (:statuses)
                      and (coalesce(:goodsName, '0') = '0'
                        or lower(vog.goods_name) like (:goodsName)
                        )
                      and ((:goodsIds) is null
                        or exists(select 1
                                  from goods.goods_details gd
                                           join goods_order.order_goods og2
                                                on og2.goods_details_id = gd.id
                                  where og2.order_id = o.id
                                    and gd.status = 'ACTIVE'
                                    and gd.goods_id in (:goodsIds))
                        )
                      and ((:goodsSupplierIds) is null
                        or exists(select 1
                                  from goods.goods_details gd
                                           join goods_order.order_goods og2
                                                on og2.goods_details_id = gd.id
                                  where og2.order_id = o.id
                                    and gd.status = 'ACTIVE'
                                    and gd.goods_suppliers_id in (:goodsSupplierIds))
                        )
                      and (
                        (cast(:dateOfOrderCreationFrom as date) is null and cast(:dateOfOrderCreationTo as date) is null)
                            or
                        (cast(:dateOfOrderCreationFrom as date) is not null and cast(:dateOfOrderCreationTo as date) is not null and
                         date(o.create_date) between cast(:dateOfOrderCreationFrom as date) and cast(:dateOfOrderCreationTo as date))
                            or
                        (cast(:dateOfOrderCreationFrom as date) is not null and cast(:dateOfOrderCreationTo as date) is null and
                         date(o.create_date) >= cast(:dateOfOrderCreationFrom as date))
                            or
                        (cast(:dateOfOrderCreationFrom as date) is null and cast(:dateOfOrderCreationTo as date) is not null and
                         date(o.create_date) <= cast(:dateOfOrderCreationTo as date))
                        )
                      and (coalesce(:directDebit, '0') = '0' or o.direct_debit = :directDebit)
                      and (:invoicePaid is null or (:invoicePaid = 'YES' and l.id is not null and l.current_amount = 0) or
                           (:invoicePaid = 'NO' and l.id is not null and l.current_amount <> 0)
                        or (:invoicePaid = 'ALL' and (l.id is not null and l.current_amount = 0)))
                      and (cast(:invoiceMaturityDateFrom as date) is null or
                           (i.id is not null and cast(i.payment_deadline as date) >= cast(:invoiceMaturityDateFrom as date)))
                      and (cast(:invoiceMaturityDateTo as date) is null or
                           (i.id is not null and cast(i.payment_deadline as date) <= cast(:invoiceMaturityDateTo as date)))
                      and ((:accountManagerIds) is null
                        or exists(select 1
                                  from customer.customer_account_managers cam
                                  where cam.customer_detail_id = cd.id
                                    and cam.account_manager_id in (:accountManagerIds)
                                    and cam.status = 'ACTIVE')
                        )
                      and (:prompt is null
                        or (:searchBy = 'ALL'
                            and
                            (
                                lower(o.order_number) like :prompt
                                    or lower(cd.name) like :prompt
                                    or c.identifier like :prompt
                                )
                               )
                        or (
                               (:searchBy = 'ORDER_NUMBER' and lower(o.order_number) like :prompt)
                                   or (:searchBy = 'CUSTOMER_NAME' and lower(cd.name) like :prompt)
                                   or (:searchBy = 'CUSTOMER_UIC_OR_PERSONAL_NUMBER' and lower(c.identifier) like :prompt)
                               )
                        )
                    """)
    Page<GoodsOrderListingMiddleResponse> filter(
            @Param("prompt") String prompt,
            @Param("goodsName") String goodsName,
            @Param("goodsIds") List<Long> goodsIds,
            @Param("goodsSupplierIds") List<Long> goodsSupplierIds,
            @Param("dateOfOrderCreationFrom") LocalDate dateOfOrderCreationFrom,
            @Param("dateOfOrderCreationTo") LocalDate dateOfOrderCreationTo,
            @Param("invoiceMaturityDateFrom") LocalDate invoiceMaturityDateFrom,
            @Param("invoiceMaturityDateTo") LocalDate invoiceMaturityDateTo,
            @Param("invoicePaid") String invoicePaid,
            @Param("directDebit") Boolean directDebit,
            @Param("accountManagerIds") List<Long> accountManagerIds,
            @Param("searchBy") String searchBy,
            @Param("goodsNameDirection") String goodsNameDirection, // TODO: 11.10.23 some silent exception handling here, need to fix
            @Param("goodsSupplierDirection") String goodsSupplierDirection, // TODO: 11.10.23 some silent exception handling here, need to fix
            @Param("statuses") List<String> statuses,
            Pageable pageable
    );

    Optional<GoodsOrder> findByOrderNumberAndStatus(String orderNumber, EntityStatus entityStatus);

    @Query("""
            select count(pcrgo.id) > 0
            from ProductContractRelatedGoodsOrder pcrgo
            join ProductContract pc on (pc.id = pcrgo.productContractId and pcrgo.goodsOrderId = :id)
            where pc.status = 'ACTIVE'
            and pcrgo.status = 'ACTIVE'
            """)
    boolean hasConnectionToProductContract(Long id);

    @Query("""
            select count(scrgo.id) > 0
            from ServiceContractRelatedGoodsOrder scrgo
            join ServiceContracts sc on (sc.id = scrgo.serviceContractId and scrgo.goodsOrderId = :id)
            where scrgo.status = 'ACTIVE'
            and sc.status = 'ACTIVE'
            """)
    boolean hasConnectionToServiceContract(Long id);

    @Query("""
            select count(sorgo.id) > 0
            from ServiceOrderRelatedGoodsOrder sorgo
            join ServiceOrder so on (so.id = sorgo.serviceOrderId and sorgo.goodsOrderId = :id)
            where sorgo.status = 'ACTIVE'
            and so.status = 'ACTIVE'
            """)
    boolean hasConnectionToServiceOrder(Long id);

    @Query("""
            select count(gorgo.id) > 0
            from GoodsOrderRelatedGoodsOrder gorgo
            join GoodsOrder goSource on (gorgo.goodsOrderId = goSource.id)
            join GoodsOrder goTarget on (gorgo.relatedGoodsOrderId = goTarget.id)
            where (gorgo.goodsOrderId = :id and gorgo.relatedGoodsOrderId = :id)
            and gorgo.status = 'ACTIVE'
            and (goSource.status = 'ACTIVE' and goTarget.status = 'ACTIVE')
            """)
    boolean hasConnectionToGoodsOrder(Long id);

    @Query("""
            select count(got.id) > 0
            from GoodsOrderTask got
            join Task t on got.taskId = t.id
            where got.orderId = :id
            and got.status = 'ACTIVE'
            and t.status = 'ACTIVE'
            """)
    boolean hasConnectionToTask(Long id);

    @Query("""
            select count(goa.id) > 0
            from GoodsOrderActivity goa
            join SystemActivity sa on sa.id = goa.systemActivityId
            where goa.orderId = :id
            and sa.status = 'ACTIVE'
            and goa.status = 'ACTIVE'
            """)
    boolean hasConnectionToActivity(Long id);

    @Query("""
             select new bg.energo.phoenix.model.response.customer.CustomerCommunicationDataResponse(
                         cc.id,
                         cc.contactTypeName,
                         cc.createDate
                     )
             from GoodsOrder go
             join CustomerCommunications cc on cc.id = go.customerCommunicationIdForBilling
             where go.id = :orderId
             and go.customerDetailId = :customerDetailsId
             and go.status = 'ACTIVE'
             and cc.status = 'ACTIVE'
            """)
    Optional<CustomerCommunicationDataResponse> findCommunicationDataByOrderIdAndCustomerDetailId(
            @Param("orderId") Long orderId,
            @Param("customerDetailsId") Long customerDetailsId);

    @Query(nativeQuery = true,
            value = """
                    select distinct o.id as id, o.order_number as number, o.create_date as creationDate, 'GOODS_ORDER' as type
                    from goods_order.orders o
                    where o.status = 'ACTIVE'
                    and o.customer_detail_id = :customerDetailsId
                    and (:prompt is null
                    or lower(o.order_number) like :prompt)
                    union
                    select distinct o.id, o.order_number, o.create_date, 'SERVICE_ORDER'
                    from service_order.orders o
                    where o.status = 'ACTIVE'
                    and o.customer_detail_id = :customerDetailsId
                    and (:prompt is null
                    or lower(o.order_number) like :prompt)
                    """,
            countQuery = """
                    select count(id)
                    from (
                        select distinct o.id as id, o.order_number as number, o.create_date as creationDate, 'GOODS_ORDER' as type
                        from goods_order.orders o
                        where o.status = 'ACTIVE'
                        and o.customer_detail_id = :customerDetailsId
                        and (:prompt is null
                        or lower(o.order_number) like :prompt)
                        union
                        select distinct o.id, o.order_number, o.create_date, 'SERVICE_ORDER'
                        from service_order.orders o
                        where o.status = 'ACTIVE'
                        and o.customer_detail_id = :customerDetailsId
                        and (:prompt is null
                        or lower(o.order_number) like :prompt)
                    ) as count_query
                    """)
    Page<CustomerContractOrderResponse> findByCustomerDetailsIdAndPrompt(
            @Param("customerDetailsId") Long customerDetailsId,
            @Param("prompt") String prompt,
            Pageable pageable
    );

    @Query("""
            select new bg.energo.phoenix.model.response.billing.billingRun.manualInvoice.ContractOrderShortResponse(go.id, go.orderNumber)
            from GoodsOrder go
            where go.id = :id
            and go.status = :status
            """)
    Optional<ContractOrderShortResponse> findByIdAndStatus(@Param("id") Long goodsOrderId,
                                                           @Param("status") EntityStatus entityStatus);

    @Query("""
             select new bg.energo.phoenix.model.response.customer.CustomerCommunicationDataResponse(
                         cc.id,
                         cc.contactTypeName,
                         cc.createDate
                     )
             from GoodsOrder go
             join CustomerCommunications cc on cc.id = go.customerCommunicationIdForBilling
             where go.id = :orderId
             and cc.id = :id
             and go.customerDetailId = :customerDetailsId
             and go.status = 'ACTIVE'
             and cc.status = 'ACTIVE'
            """)
    Optional<CustomerCommunicationDataResponse> findCommunicationDataByIdAndOrderIdAndCustomerDetailId(@Param("id") Long customerCommunicationId,
                                                                                                       @Param("orderId") Long goodsOrderId,
                                                                                                       @Param("customerDetailsId") Long customerDetailId);

    @Query("""
                 select count(go.id) > 0
                 from GoodsOrder go
                 join CustomerDetails cd on cd.id = go.customerDetailId
                 where go.id = :orderId
                 and cd.customerId = :customerId
                 and go.status = 'ACTIVE'
            """)
    boolean existsByOrderIdAndCustomerId(Long orderId, Long customerId);

    @Query(value = """
            select new bg.energo.phoenix.model.response.contract.order.goods.GoodsOrderForInvoiceResponse(
            go.id,
            go.incomeAccountNumber,
            go.costCenterControllingOrder,
            go.applicableInterestRateId,
            case when go.directDebit = true then go.directDebit else cd.directDebit end,
            case when go.directDebit = true then go.bankId else case when cd.directDebit = true then cd.bank.id end end,
            case when go.directDebit = true then go.iban else case when cd.directDebit = true then cd.iban end end,
            go.customerDetailId,
            cd.customerId,
            go.customerCommunicationIdForBilling,
            vr.id,
            vr.valueInPercent,
            go.noInterestOnOverdueDebts,
            case when go.invoiceTemplateId is not null then go.invoiceTemplateId else t.id end
            )
            from GoodsOrder go
                     join VatRate vr
                          on ((go.globalVatRate = false and go.vatRateId = vr.id)
                          or (go.globalVatRate = true and vr.status = 'ACTIVE' and vr.startDate = (select max(innerVr.startDate)
                          from VatRate innerVr
                          where innerVr.startDate <= current_date
                          and innerVr.status = 'ACTIVE'
                          and innerVr.globalVatRate = true)))
                     join CustomerDetails cd on cd.id = go.customerDetailId
                     left join ContractTemplate t on (t.defaultForGoodsOrderDocument = true and t.status = 'ACTIVE')
            where go.id = :id
              and go.status = 'ACTIVE'
              and go.orderStatus = 'CONFIRMED'
              and not exists (select 1
                              from Invoice i
                              where i.goodsOrderId = :id
                              and i.invoiceStatus <> 'CANCELLED'
                              and i.invoiceType = 'STANDARD')
            """)
    Optional<GoodsOrderForInvoiceResponse> findByOrderIdAndOrderStatusAndNonExistentInvoice(@Param("id") Long id);

    @Query("""
            select go
            from GoodsOrder go
            where go.id = :id
            and go.orderStatus = :goodsOrderStatus
            and go.status = 'ACTIVE'
            """)
    Optional<GoodsOrder> findByIdAndOrderStatus(@Param("id") Long id,
                                                @Param("goodsOrderStatus") GoodsOrderStatus goodsOrderStatus);

    @Query("""
            select go
            from GoodsOrder go
            where go.id = :id
              and go.orderStatus = :goodsOrderStatus
              and go.status = 'ACTIVE'
              and exists(select 1
                         from GoodsOrderPaymentTerm pt
                         where pt.orderId = :id
                           and pt.status = 'ACTIVE')
            """)
    Optional<GoodsOrder> findByIdAndOrderStatusAndPaymentTermFilled(@Param("id") Long id,
                                                                    @Param("goodsOrderStatus") GoodsOrderStatus goodsOrderStatus);

    @Query(value = """
            select go.*
            from goods_order.orders go
            join invoice.invoices i on go.id = i.goods_order_id
            join invoice.invoice_detailed_data idd on idd.invoice_id = i.id
            where go.status = 'ACTIVE'
            and go.order_status = 'AWAITING_PAYMENT'
            and go.payment_term_in_calendar_days < extract(day from current_date)
            and i.invoice_date + interval '1 day' * go.payment_term_in_calendar_days < current_date
            and i.status = 'REAL'
            and i.document_type = 'PROFORMA_INVOICE'
              """, nativeQuery = true)
    List<GoodsOrder> findOrdersWithOverdueInvoices();

}
