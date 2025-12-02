package bg.energo.phoenix.repository.billing.invoice;

import bg.energo.phoenix.model.CacheObject;
import bg.energo.phoenix.model.entity.billing.invoice.Invoice;
import bg.energo.phoenix.model.entity.documents.Document;
import bg.energo.phoenix.model.enums.billing.billings.PrefixType;
import bg.energo.phoenix.model.enums.billing.invoice.InvoiceDocumentType;
import bg.energo.phoenix.model.enums.billing.invoice.InvoiceStatus;
import bg.energo.phoenix.model.response.billing.billingRun.manualCreditOrDebitNote.BillingRunInvoiceResponse;
import bg.energo.phoenix.model.response.billing.invoice.*;
import bg.energo.phoenix.model.response.contract.order.goods.OrderInvoiceViewResponse;
import bg.energo.phoenix.model.response.customer.CustomerInvoicesResponseModel;
import bg.energo.phoenix.model.response.shared.ShortResponse;
import bg.energo.phoenix.service.billing.invoice.numberGeneration.InvoiceNumberDto;
import bg.energo.phoenix.service.billing.invoice.reversal.InvoiceReversalModel;
import bg.energo.phoenix.service.billing.invoice.reversal.ReversalValidationObject;
import bg.energo.phoenix.service.billing.model.persistance.BillingRunDocumentModel;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface InvoiceRepository extends JpaRepository<Invoice, Long> {
    @Query(value = """
            select new bg.energo.phoenix.model.response.billing.invoice.InvoiceListingResponse(
                inv.id,
                inv.invoiceNumber,
                inv.invoiceDocumentType,
                inv.invoiceDate,
                CASE
                    WHEN (c.customerType = 'LEGAL_ENTITY')
                    THEN CONCAT(cd.name, ' ', lf.name, ' (', c.identifier, ')')
                    ELSE CONCAT(cd.name, ' ',
                                CASE WHEN cd.middleName IS NOT NULL THEN CONCAT(cd.middleName, ' ') ELSE '' END,
                                cd.lastName, ' (', c.identifier, ')')
                END,
                ap.name,
                coalesce(bil.billingNumber, ''),
                inv.basisForIssuing,
                inv.meterReadingPeriodFrom,
                inv.meterReadingPeriodTo,
                inv.totalAmountExcludingVat,
                inv.invoiceStatus
            )
            from Invoice inv
            left join BillingRun bil on bil.id = inv.billingId
            left join AccountingPeriods ap on ap.id = inv.accountPeriodId
            join CustomerDetails cd on cd.id = inv.customerDetailId
            join Customer c on c.id = cd.customerId
            left join LegalForm lf on lf.id = cd.legalFormId
            where (
            inv.invoiceStatus <> 'DRAFT'
            and inv.invoiceStatus <> 'DRAFT_GENERATED'
            and (coalesce(:invoiceStatuses, '0') = '0' or inv.invoiceStatus in (:invoiceStatuses))
            and (coalesce(:invoiceDocumentTypes, '0') = '0' or inv.invoiceDocumentType in (:invoiceDocumentTypes))
            and (coalesce(:accountingPeriodId, '0') = '0' or inv.accountPeriodId in (:accountingPeriodId))
            and (coalesce(:billingRun, '0') = '0' or inv.billingId is null or (bil.billingNumber is not null and lower(bil.billingNumber) like (:billingRun)))
            and (cast(:dateOfInvoiceFrom as date) is null or cast(inv.invoiceDate as date) >= :dateOfInvoiceFrom)
            and (cast(:dateOfInvoiceTo as date) is null or cast(inv.invoiceDate as date) <= :dateOfInvoiceTo)
            and (:totalAmountFrom is null or inv.totalAmountExcludingVat >= :totalAmountFrom)
            and (:totalAmountTo is null or inv.totalAmountExcludingVat <= :totalAmountTo)
            and (cast(:meterReadingPeriodFrom as date) is null or (inv.meterReadingPeriodFrom is not null and cast(inv.meterReadingPeriodFrom as date) >= :meterReadingPeriodFrom))
            and (cast(:meterReadingPeriodTo as date) is null or (inv.meterReadingPeriodTo is not null and cast(inv.meterReadingPeriodTo as date) <= :meterReadingPeriodTo))
            and (:prompt is null
              or (:searchBy = 'ALL'
                  and (lower(text(inv.invoiceDocumentType)) like :prompt
                      or lower(ap.name) like :prompt
                      or (inv.billingId is null or (bil.billingNumber is not null and lower(bil.billingNumber) like :prompt))
                      or lower(inv.invoiceNumber) like :prompt
                      or text(inv.meterReadingPeriodFrom) like :prompt
                      or text(inv.meterReadingPeriodTo) like :prompt))
              or ((:searchBy = 'ACCOUNTING_PERIOD'
                      and lower(ap.name) like :prompt)
              or (:searchBy = 'BILLING_RUN'
                  and (lower(bil.billingNumber) like :prompt))
              or (:searchBy = 'INVOICE_NUMBER'
                  and lower(inv.invoiceNumber) like :prompt)
                 )
              )
            )
            """
    )
    Page<InvoiceListingResponse> filter(
            @Param("prompt") String prompt,
            @Param("invoiceDocumentTypes") List<InvoiceDocumentType> invoiceDocumentTypes,
            @Param("accountingPeriodId") List<Long> accountingPeriodId,
            @Param("billingRun") String billingRun,
            @Param("dateOfInvoiceFrom") LocalDate dateOfInvoiceFrom,
            @Param("dateOfInvoiceTo") LocalDate dateOfInvoiceTo,
            @Param("totalAmountFrom") BigDecimal totalAmountFrom,
            @Param("totalAmountTo") BigDecimal totalAmountTo,
            @Param("meterReadingPeriodFrom") LocalDate meterReadingPeriodFrom,
            @Param("meterReadingPeriodTo") LocalDate meterReadingPeriodTo,
            @Param("invoiceStatuses") List<InvoiceStatus> invoiceStatuses,
            @Param("searchBy") String searchBy,
            Pageable pageRequest
    );

    @Query(nativeQuery = true,
            value = """
                    select inv1.id as invoiceId,
                           inv1.invoice_number as invoiceNumber,
                           case when inv1.product_contract_id is not null or inv1.service_contract_id is not null then true else false end as canSelectAccordingToContract,
                           coalesce(inv1.product_contract_id,inv1.service_contract_id,inv1.service_order_id,inv1.goods_order_id, -1) <> -1 canSelectManual
                    from invoice.invoices inv1
                             join customer.customer_details cd1 on cd1.id=inv1.customer_detail_id
                    where
                        (:invoiceNumber is null
                            or (
                                   (((select inv.product_contract_id from invoice.invoices inv where inv.invoice_number = :invoiceNumber) is not null and (select inv.product_contract_id from invoice.invoices inv where inv.invoice_number = :invoiceNumber)=inv1.product_contract_id) or
                                    ((select inv.service_contract_id from invoice.invoices inv where inv.invoice_number = :invoiceNumber) is not null and (select inv.service_contract_id from invoice.invoices inv where inv.invoice_number = :invoiceNumber)=inv1.service_contract_id) or
                                    ((select inv.goods_order_id from invoice.invoices inv where inv.invoice_number = :invoiceNumber) is not null and (select inv.goods_order_id from invoice.invoices inv where inv.invoice_number = :invoiceNumber)=inv1.goods_order_id) or
                                    ((select inv.service_order_id from invoice.invoices inv where inv.invoice_number = :invoiceNumber) is not null and (select inv.service_order_id from invoice.invoices inv where inv.invoice_number = :invoiceNumber)=inv1.service_order_id))
                                   )
                             and (select cd.customer_id from invoice.invoices inv
                                                                 join customer.customer_details cd on cd.id=inv.customer_detail_id
                                  where inv.invoice_number=:invoiceNumber) = cd1.customer_id
                             and (inv1.contract_billing_group_id is null or (select inv.contract_billing_group_id from invoice.invoices inv where inv.invoice_number = :invoiceNumber)=inv1.contract_billing_group_id)
                            )
                      and (:prompt is null or lower(inv1.invoice_number) LIKE :prompt)
                      and inv1.status = 'REAL'
                      and inv1.document_type = 'INVOICE'
                    """
    )
    Page<BillingRunInvoiceResponse> filterInvoiceNumbers(
            @Param("prompt") String prompt,
            @Param("invoiceNumber") String invoiceNumber,
            PageRequest pageRequest
    );

    @Query(value = """
            
                           select inv1.id
                           from invoice.invoices inv1
                           where
              (
                   ((select inv.product_contract_id from invoice.invoices inv where inv.id = :invoiceId)=inv1.product_contract_id) or
                   ( (select inv.service_contract_id from invoice.invoices inv where inv.id = :invoiceId)=inv1.service_contract_id) or
                   ( (select inv.goods_order_id from invoice.invoices inv where inv.id = :invoiceId)=inv1.goods_order_id) or
                   ( (select inv.service_order_id from invoice.invoices inv where inv.id = :invoiceId)=inv1.service_order_id)
                   )
            
            
            and inv1.status = 'REAL'
            and inv1.document_type = 'INVOICE'
            """, nativeQuery = true)
    Set<Long> availableInvoicesForBilling(Long invoiceId);

    Optional<List<Invoice>> findAllByInvoiceNumberLike(String invoiceNumber);

    List<Invoice> findAllByServiceOrderIdIn(List<Long> serviceOrderIds);

    @Query(nativeQuery = true, value = """
            select string_agg(substring(i.invoice_number from '-(.*)'), ',')
            from service_order.orders o
                     join invoice.invoices i on o.id = i.service_order_id and i.status = 'REAL' and i.type='STANDARD'
            where o.id = :serviceOrderId
            """)
    Optional<String> getInvoiceNumbersToCancelByServiceOrderId(Long serviceOrderId);

    @Query("""
            select new bg.energo.phoenix.model.response.billing.invoice.InvoiceShortResponse(
            i.id,
            i.invoiceNumber
            )
            from Invoice i
            where i.id in :invoiceIds
            """)
    List<InvoiceShortResponse> getInvoiceShortResponsesByIds(@Param("invoiceIds") List<Long> invoiceIds);

    @Query(value = """
            select coalesce(max('true'), 'false')
            from invoice.invoices i
            where case :contractType
                      when 'SERVICE_CONTRACT'
                          then i.service_contract_id = :contractId
                      when 'PRODUCT_CONTRACT'
                          then i.product_contract_id = :contractId
                end
              and i.status <> 'CANCELLED'
              and exists (select 1
                          from invoice.invoice_detailed_data idd
                          where idd.invoice_id = i.id
                            and idd.price_component_id = :priceComponentId)
            """, nativeQuery = true)
    boolean existsByContractIdAndPriceComponentId(@Param("contractId") Long contractId,
                                                  @Param("contractType") String contractType,
                                                  @Param("priceComponentId") Long priceComponentId);

    Optional<Invoice> findByIdAndCustomerDetailIdAndInvoiceStatusIn(Long id, Long customerDetailsId, List<InvoiceStatus> statuses);

    Optional<Invoice> findByIdAndInvoiceStatusIn(Long id, List<InvoiceStatus> statuses);

    Optional<Invoice> findByIdAndCustomerDetailIdAndContractBillingGroupIdAndInvoiceStatusIn(Long id, Long customerDetailsId, Long contractBillingGroupId, List<InvoiceStatus> statuses);

    @Query(value = """
            select new bg.energo.phoenix.model.response.billing.invoice.InvoiceShortResponse(
                i.id,
                i.invoiceNumber
            )
            from Invoice i
            join CustomerDetails cd on i.customerDetailId = cd.id
            join Customer c on cd.customerId = c.id
            where c.id = :customerId
              and (:contractBillingGroupId is null or i.contractBillingGroupId = :contractBillingGroupId)
              and i.invoiceStatus = 'REAL'
              and i.invoiceNumber = COALESCE(:prompt, i.invoiceNumber)
            """)
    Page<InvoiceShortResponse> findByIdAndCustomerDetailIdAndContractBillingGroupId(@Param("customerId") Long customerId,
                                                                                    @Param("contractBillingGroupId") Long contractBillingGroupId,
                                                                                    @Param("prompt") String prompt,
                                                                                    Pageable pageable);


    @Query(value = "select nextval('invoice.draft_invoice_number_seq')", nativeQuery = true)
    Long getDraftNextSequenceValue();

    @Query(value = "select nextval('invoice.real_invoice_number_seq')", nativeQuery = true)
    Long getRealNextSequenceValue();

    @Query(value = "select nextval('invoice.draft_proforma_invoice_number_seq')", nativeQuery = true)
    Long getDraftProformaNextSequenceValue();

    @Query(value = "select nextval('invoice.real_proforma_invoice_number_seq')", nativeQuery = true)
    Long getRealProformaNextSequenceValue();

    @Query("""
            select i
            from Invoice i
            where i.goodsOrderId = :goodsOrderId
            and i.invoiceStatus = :invoiceStatus
            and i.invoiceDocumentType in (:invoiceDocumentType)
            """)
    Optional<Invoice> findGoodsOrderInvoiceByStatusAndDocumentTypeIn(@Param("goodsOrderId") Long id,
                                                                     @Param("invoiceStatus") InvoiceStatus invoiceStatus,
                                                                     @Param("invoiceDocumentType") List<InvoiceDocumentType> invoiceDocumentType);

    @Query("""
            select i, go
            from Invoice i
            join GoodsOrder go on go.id = i.goodsOrderId
            where go.id = :goodsOrderId
            and go.status = 'ACTIVE'
            and ((go.orderStatus = 'CONFIRMED' and i.invoiceStatus = 'DRAFT') or (go.orderStatus = 'AWAITING_PAYMENT' and i.invoiceStatus = 'REAL'))
            and i.invoiceDocumentType = :invoiceDocumentType
            """)
    List<Object[]> findGoodsOrderInvoiceByStatusAndDocumentTypeToDelete(@Param("goodsOrderId") Long id,
                                                                        @Param("invoiceDocumentType") InvoiceDocumentType invoiceDocumentType);

    @Query(nativeQuery = true, value = """
            select invoice_tbl.id                  as id,
                   invoice_tbl.invoicenumber       as invoiceNumber,
                   invoice_tbl.invoicedocumenttype as invoiceDocumenttype,
                   invoice_tbl.invoicetype         as invoiceType,
                   invoice_tbl.invoicedate         as invoiceDate,
                   invoice_tbl.customer            as customer,
                   invoice_tbl.accountingperiod    as accountingPeriod,
                   invoice_tbl.basisforissuing     as basisForIssuing,
                   invoice_tbl.totalamount         as totalAmount
            from (select i.id                         as id,
                         i.invoice_number             as invoiceNumber,
                         i.document_type              as invoicedocumenttype,
                         i.type                       as invoicetype,
                         i.invoice_date               as invoicedate,
                         (
                             case
                                 when c.customer_type = 'LEGAL_ENTITY' then concat(c.identifier, ' (', cd.name, ' ', lf.name, ')')
                                 else concat(c.identifier, ' (', cd.name, ' ',
                                             case when cd.middle_name is not null then concat(cd.middle_name, ' ') else '' end,
                                             cd.last_name, ')')
                                 end
                             )                        as customer,
                         ap.name                      as accountingperiod,
                         i.basis_for_issuing          as basisforissuing,
                         i.total_amount_including_vat as totalamount
                  from invoice.invoices i
                           left join service_order.orders so on i.service_order_id = so.id
                           left join goods_order.orders go on i.goods_order_id = go.id
                           left join billing.account_periods ap
                                     on i.account_period_id = ap.id
                           join customer.customer_details cd on cd.id = i.customer_detail_id
                           join customer.customers c on cd.customer_id = c.id
                           left join nomenclature.legal_forms lf on lf.id = cd.legal_form_id
                  where ((:orderType = 'SERVICE_ORDER' and so.id = :id)
                      or (:orderType = 'GOODS_ORDER') and go.id = :id)
                    and i.document_type = 'PROFORMA_INVOICE'
                    and (case when :draftsTab then i.status = 'DRAFT' else text(i.status) in ('DRAFT_GENERATED', 'REAL') end)
                    and (:prompt is null or lower(i.invoice_number) like :prompt)) as invoice_tbl
            """, countQuery = """
            select count(id)
            from (select i.id as id
                  from invoice.invoices i
                           left join service_order.orders so on i.service_order_id = so.id
                           left join goods_order.orders go on i.goods_order_id = go.id
                           left join billing.account_periods ap
                                     on i.account_period_id = ap.id
                           join customer.customer_details cd on cd.id = i.customer_detail_id
                           join customer.customers c on cd.customer_id = c.id
                           left join nomenclature.legal_forms lf on lf.id = cd.legal_form_id
                  where ((:orderType = 'SERVICE_ORDER' and so.id = :id)
                      or (:orderType = 'GOODS_ORDER') and go.id = :id)
                    and i.document_type = 'PROFORMA_INVOICE'
                    and (case when :draftsTab then text(i.status) = 'DRAFT' else text(i.status) = 'DRAFT_GENERATED' end)
                    and (:prompt is null or lower(i.invoice_number) like :prompt)) as invoice_tbl
            """)
    Page<OrderInvoiceViewResponse> getInvoiceByOrderId(@Param("id") Long id,
                                                       @Param("orderType") String orderType,
                                                       @Param("prompt") String prompt,
                                                       @Param("draftsTab") Boolean draftsTab,
                                                       Pageable pageable);

    @Query("""
                select i
                from Invoice i
                where i.serviceOrderId = :orderId
                and (((:forDelete = true and (text(i.invoiceStatus) in ('DRAFT', 'DRAFT_GENERATED')))
                        or (:forDelete = false and i.invoiceStatus = 'DRAFT_GENERATED') and :orderStatus = 'CONFIRMED')
                    or
                    (i.invoiceStatus = 'REAL' and :orderStatus = 'AWAITING_PAYMENT'))
                and i.invoiceDocumentType = 'PROFORMA_INVOICE'
                and i.invoiceType = 'STANDARD'
            """)
    List<Invoice> findServiceOrderInvoicesWithStatus(@Param("orderId") Long id,
                                                     @Param("orderStatus") String orderStatus,
                                                     @Param("forDelete") boolean forDelete);

    @Query("""
                select i
                from Invoice i
                where i.serviceOrderId = :orderId
                and i.billingId is null
                and i.invoiceStatus = 'DRAFT'
                and i.invoiceDocumentType = 'PROFORMA_INVOICE'
            """)
    List<Invoice> findServiceOrderInvoicesForGeneration(@Param("orderId") Long id);

    @Query("""
            select new bg.energo.phoenix.model.response.billing.invoice.InvoiceResponseExport
            (
                inv.id,
                inv.invoiceNumber,
                inv.createDate,
                inv.invoiceStatus,
                inv.currentStatusChangeDate,
                inv.invoiceDocumentType,
                c.identifier,
                (case when c.customerType = 'LEGAL_ENTITY' then concat(c.identifier, ' (', cd.name, ')')
                        else replace(concat(c.identifier , ' (',coalesce(cd.name, '') , ' ', coalesce(cd.middleName, '') , ' ', coalesce(cd.lastName, ''), ')'), '\\s+', ' ') end),
                inv.taxEventDate,
                inv.paymentDeadline,
                inv.invoiceType,
                (case when inv.serviceContractId is not null then (select concat(sc.contractNumber, '/', cast(sc.createDate as date)) from ServiceContracts sc where sc.id = inv.serviceContractId)
                      when inv.productContractId is not null then (select concat(pc.contractNumber, '/', cast(pc.createDate as date)) from ProductContract pc where pc.id=inv.productContractId)
                      when inv.goodsOrderId is not null then (select concat(go.orderNumber, '/', cast(go.createDate as date)) from GoodsOrder go where go.id=inv.goodsOrderId)
                      when inv.serviceOrderId is not null then (select concat(so.orderNumber, '/', cast(so.createDate as date)) from ServiceOrder so where so.id=inv.serviceOrderId)
                      else ''
                end ),
                cbg.groupNumber,
                altC.identifier,
                 (case when altC.customerType = 'LEGAL_ENTITY' then concat(altC.identifier, ' (', altCd.name, ')')
                        else replace(concat(altC.identifier , ' (',coalesce(altCd.name, '') , ' ', coalesce(altCd.middleName, '') , ' ', coalesce(altCd.lastName, ''), ')'), '\\s+', ' ') end),
                cc.contactTypeName,
                (case when inv.serviceDetailId is not null then (select s.name from ServiceDetails s where s.id = inv.serviceDetailId)
                      when inv.productDetailId is not null then (select p.name from ProductDetails p where p.id=inv.productDetailId)
                      else ''
                end ),
                inv.meterReadingPeriodFrom,
                inv.meterReadingPeriodTo,
                inv.incomeAccountNumber,
                inv.basisForIssuing,
                inv.costCenterControllingOrder,
                ir.name,
                inv.directDebit,
                bk.name,
                bk.bic,
                inv.iban,
                inv.totalAmountIncludingVat,
                inv.totalAmountIncludingVatInOtherCurrency,
                (select c.name from Currency c where c.id=inv.currencyId),
                (select ap.name from AccountingPeriods ap where ap.id=inv.accountPeriodId),
                (select tp.name from ContractTemplateDetail tp where tp.id = inv.templateDetailId),
                (select string_agg(cast(comp.id as string ),',' ) from Compensations comp where comp.invoiceId=inv.id),
                (select string_agg(inv2.invoiceNumber,',' ) from BillingRunInvoices bi
                 join Invoice inv2 on inv2.billingId=bi.billingId
                 join BillingRun br on br.id=bi.billingId
                 where bi.invoiceId=inv.id
                 and br.type='MANUAL_CREDIT_OR_DEBIT_NOTE')
            ) from Invoice  inv
            left join CustomerDetails cd on cd.id =inv.customerDetailId
            left join Customer c on c.id=cd.customerId
            left join CustomerDetails  altCd on altCd.id =inv.alternativeRecipientCustomerDetailId
            left join Customer  altC on altC.id= altCd.customerId
            left join ContractBillingGroup  cbg on cbg.id=inv.contractBillingGroupId
            left join InterestRate  ir on ir.id=inv.interestRateId
            left join Bank bk on bk.id=inv.bankId
            left join CustomerCommunications cc on cc.id=inv.customerCommunicationId
            where inv.billingId=:billingId
            and inv.invoiceStatus = :invoiceStatus
            """)
    List<InvoiceResponseExport> findAllByBillingId(Long billingId, InvoiceStatus invoiceStatus);

    @Query(value = """
            select customer_invoices.invoiceId                            AS invoiceId,
                   customer_invoices.invoiceNumber                        AS invoiceNumber,
                   customer_invoices.documentType                         AS documentType,
                   customer_invoices.invoiceDate                          AS invoiceDate,
                   customer_invoices.accountingPeriod                     AS accountingPeriod,
                   customer_invoices.billingNumber                        AS billingNumber,
                   customer_invoices.basisForIssuing                      AS basisForIssuing,
                   customer_invoices.meterReadingPeriodFrom               AS meterReadingPeriodFrom,
                   customer_invoices.meterReadingPeriodTo                 AS meterReadingPeriodTo,
                   coalesce(customer_invoices.totalAmountExcludingVat, 0) AS totalAmountExcludingVat,
                   customer_invoices.status                               AS status,
                   customer_invoices.actions                              AS actions
            from (SELECT inv.id                         AS invoiceId,
                         inv.invoice_number             AS invoiceNumber,
                         inv.document_type              AS documentType,
                         inv.invoice_date               AS invoiceDate,
                         ap.name                        AS accountingPeriod,
                         b.billing_number               AS billingNumber,
                         b.basic_for_issuing            AS basisForIssuing,
                         inv.meter_reading_period_from  as meterReadingPeriodFrom,
                         inv.meter_reading_period_to    as meterReadingPeriodTo,
                         inv.total_amount_excluding_vat as totalAmountExcludingVat,
                         inv.status                     AS status,
                         ''                             AS actions
                  FROM invoice.invoices inv
                           LEFT JOIN billing.account_periods ap ON ap.id = inv.account_period_id
                           LEFT JOIN billing.billings b ON b.id = inv.billing_id
                  WHERE inv.customer_detail_id = :customerDetailId
                    AND text(inv.document_type) <> 'PROFORMA_INVOICE'
                    AND (coalesce(:documentTypes, '0') = '0' or text(inv.document_type) in (:documentTypes))
                    AND (coalesce(:accountPeriodIds, '0') = '0' or inv.account_period_id in (:accountPeriodIds))
                    AND (coalesce(:billingRun, '0') = '0' or lower(coalesce(b.billing_number, '')) like (:billingRun))
                    AND (inv.invoice_date between coalesce(:invoiceDateFrom, to_date('1990-01-01', 'yyyy-MM-dd')) and coalesce(:invoiceDateTo, to_date('2090-12-31', 'yyyy-MM-dd')))
                    AND (coalesce(inv.total_amount_excluding_vat, 0) between coalesce(cast(:totalAmountFrom as numeric), 0) and coalesce(cast(:totalAmountTo as numeric), 999999999))
                    AND (text(inv.status) <> 'DRAFT')
                    AND (coalesce(:statuses, '0') = '0' or text(inv.status) in (:statuses))
                    AND (:prompt is null or coalesce(:searchBy, '0') = '0' or (
                      (
                          :searchBy = 'ACCOUNTING_PERIOD'
                              and lower(ap.name) like (:prompt)
                          )
                          or (
                          :searchBy = 'BILLING_RUN'
                              and lower(b.billing_number) like (:prompt)
                          )
                          or (
                          :searchBy = 'INVOICE_NUMBER'
                              and lower(inv.invoice_number) like (:prompt)
                          )
                      ))) as customer_invoices""",
            countQuery = """
                       select count(customer_invoices.invoiceId)
                       from (SELECT inv.id                         AS invoiceId
                             FROM invoice.invoices inv
                             LEFT JOIN billing.account_periods ap ON ap.id = inv.account_period_id
                             LEFT JOIN billing.billings b ON b.id = inv.billing_id
                             WHERE inv.customer_detail_id = :customerDetailId
                               AND text(inv.document_type) <> 'PROFORMA_INVOICE'
                               AND (coalesce(:documentTypes, '0') = '0' or text(inv.document_type) in ((:documentTypes)))
                               AND (coalesce(:accountPeriodIds, '0') = '0' or inv.account_period_id in (:accountPeriodIds))
                               AND (coalesce(:billingRun, '0') = '0' or lower(coalesce(b.billing_number, '')) like (:billingRun))
                               AND (inv.invoice_date between coalesce(:invoiceDateFrom, to_date('1990-01-01', 'yyyy-MM-dd')) and coalesce(:invoiceDateTo, to_date('2090-12-31', 'yyyy-MM-dd')))
                               AND (coalesce(inv.total_amount_excluding_vat, 0) between coalesce(cast(:totalAmountFrom as numeric), 0) and coalesce(cast(:totalAmountTo as numeric), 999999999))
                               AND (text(inv.status) <> 'DRAFT')
                               AND (coalesce(:statuses, '0') = '0' or text(inv.status) in (:statuses))
                               AND (:prompt is null or coalesce(:searchBy, '0') = '0' or (
                                 (
                                     :searchBy = 'ACCOUNTING_PERIOD'
                                         and lower(ap.name) like (:prompt)
                                     )
                                     or (
                                     :searchBy = 'BILLING_RUN'
                                         and lower(b.billing_number) like (:prompt)
                                     )
                                     or (
                                     :searchBy = 'INVOICE_NUMBER'
                                         and lower(inv.invoice_number) like (:prompt)
                                     )
                                 ))) as customer_invoices
                    """, nativeQuery = true)
    Page<CustomerInvoicesResponseModel> getCustomerInvoicesByCustomerDetailId(String prompt,
                                                                              Long customerDetailId,
                                                                              List<String> documentTypes,
                                                                              List<Long> accountPeriodIds,
                                                                              String billingRun,
                                                                              LocalDate invoiceDateFrom,
                                                                              LocalDate invoiceDateTo,
                                                                              BigDecimal totalAmountFrom,
                                                                              BigDecimal totalAmountTo,
                                                                              List<String> statuses,
                                                                              String searchBy,
                                                                              Pageable pageable);

    @Query("""
            select inv
            from Invoice inv
            left join BillingRunDraftInvoicesMark mark on (mark.invoice = inv.id and mark.billingRun = :billingRunId)
            where inv.billingId = :billingRunId
            and text(inv.invoiceStatus) = 'DRAFT'
            and mark.id is null
            """)
    Page<Invoice> findInvoicesForDocumentGeneration(Long billingRunId,
                                                    Pageable pageable);

    @Query("""
            select count(inv.id)
            from Invoice inv
            left join BillingRunDraftInvoicesMark mark on (mark.invoice = inv.id and mark.billingRun = :billingRunId)
            where inv.billingId = :billingRunId
            and text(inv.invoiceStatus) = 'DRAFT'
            and mark.id is null
            """)
    long countInvoicesForDocumentGeneration(Long billingRunId);

    @Query("""
            select inv
            from Invoice inv
            where inv.billingId = :billingRunId
            and inv.invoiceStatus = :invoiceStatus
            """)
    List<Invoice> findByBillingRunAndInvoiceStatus(Long billingRunId, InvoiceStatus invoiceStatus);

    @Query("""
            select inv
            from Invoice inv
            where inv.billingId = :billingRunId
            """)
    List<Invoice> findInvoicesByBillingId(Long billingRunId);

    @Query("""
            select inv
            from Invoice inv
            where inv.billingId = :billingRunId
            and inv.invoiceStatus = 'DRAFT'
            and not exists (
                select 1
                from BillingRunDraftInvoicesMark mark
                where mark.billingRun = inv.billingId
                and mark.invoice = inv.id
            )
            """)
    List<Invoice> findAllValidInvoicesForGeneratingPDFDocument(Long billingRunId);

    @Query("""
            select inv
            from Invoice inv
            join Invoice rinv on rinv.id = inv.reversalCreatedFromId
            where inv.billingId = :billingRunId
            and inv.invoiceStatus = 'DRAFT'
            and not exists (
                select 1
                from BillingRunDraftInvoicesMark mark
                where mark.billingRun = inv.billingId
                and mark.invoice = inv.id
            )and ((rinv.standardInvoiceId is null or  rinv.standardInvoiceId not  in (:invalidIds)) and rinv.id not in (:invalidIds))
            """)
    List<Invoice> findAllValidInvoicesForGeneratingPDFReversalDocument(Long billingRunId, List<Long> invalidIds);

    @Query("""
            select inv
            from Invoice inv
            where inv.billingId = :billingRunId
            and inv.invoiceStatus = 'DRAFT_GENERATED'
            and not exists (
                select 1
                from BillingRunDraftPdfInvoicesMark mark
                where mark.billingRun = inv.billingId
                and mark.invoice = inv.id
            )
            """)
    List<Invoice> findAllValidInvoicesForAccounting(Long billingRunId);

    @Query("""
            select inv
            from Invoice inv
            join Invoice rinv on rinv.id = inv.reversalCreatedFromId
            where inv.billingId = :billingRunId
            and inv.invoiceStatus = 'DRAFT_GENERATED'
            and not exists (
                select 1
                from BillingRunDraftPdfInvoicesMark mark
                where mark.billingRun = inv.billingId
                and mark.invoice = inv.id
            )and ((rinv.standardInvoiceId is null or  rinv.standardInvoiceId not  in (:invalidIds)) and rinv.id not in (:invalidIds))
            """)
    List<Invoice> findAllValidInvoicesForReversalAccounting(Long billingRunId, List<Long> invalidIds);

    @Query("""
                       select new bg.energo.phoenix.model.CacheObject(i.id,i.invoiceNumber)
                       from Invoice i
                        where i.invoiceNumber=:invoiceNumber
            """)
    Optional<CacheObject> findByInvoiceNumber(String invoiceNumber);

    @Query("""
                       select i
                       from Invoice i
                        where i.invoiceNumber=:invoiceNumber
            """)
    Optional<Invoice> findInvoiceByInvoiceNumber(String invoiceNumber);

    @Query("""
                  select inv.invoiceNumber from Invoice inv
                  where inv.invoiceStatus='CANCELLED' and inv.invoiceDocumentType <> 'PROFORMA_INVOICE'
                  and not exists (select inv2 from Invoice inv2
                  where inv2.invoiceNumber like concat('%',substring(inv.invoiceNumber,length(inv.invoiceNumber)-9),'%') and (inv2.invoiceStatus='DRAFT' or inv2.invoiceStatus='DRAFT_GENERATED') and inv2.invoiceDocumentType <> 'PROFORMA_INVOICE')
            """)
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    List<String> findAvailableDraftInvoiceNumbers();

    @Query("""
                  select inv.invoiceNumber from Invoice inv
                  where inv.invoiceStatus='CANCELLED' and inv.invoiceDocumentType = 'PROFORMA_INVOICE'
                  and not exists (select inv2 from Invoice inv2
                  where inv2.invoiceNumber like concat('%',substring(inv.invoiceNumber,length(inv.invoiceNumber)-9),'%') and (inv2.invoiceStatus='DRAFT' or inv2.invoiceStatus='DRAFT_GENERATED') and inv2.invoiceDocumentType = 'PROFORMA_INVOICE')
            """)
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    List<String> findAvailableDraftProformaInvoiceNumbers();

    @Query("""
            select new bg.energo.phoenix.model.response.shared.ShortResponse(
            i.id,
            i.invoiceNumber
            )
            from Invoice i
            where ((:orderType = 'SERVICE_ORDER' and i.serviceOrderId = :orderId)
                or (:orderType = 'GOODS_ORDER' and i.goodsOrderId = :orderId))
              and i.invoiceStatus = :invoiceStatus
              and text(i.invoiceDocumentType) in ('PROFORMA_INVOICE', 'INVOICE')
            """)
    List<ShortResponse> findOrderInvoices(@Param("orderId") Long orderId,
                                          @Param("invoiceStatus") InvoiceStatus invoiceStatus,
                                          @Param("orderType") String orderType);


    @Query("""
                    select inv from Invoice inv
                    where inv.invoiceType='CORRECTION'
                    and inv.standardInvoiceId=:invoiceId
                    and inv.invoiceStatus='REAL'
            """)
    List<Invoice> findCorrectionsForStandard(Long invoiceId);


    @Query("""
            select inv from Invoice inv
            where (inv.invoiceType='STANDARD' and inv.id=:invoiceId) 
            or (inv.invoiceType='CORRECTION' and inv.standardInvoiceId=:invoiceId)
            and inv.invoiceStatus= 'REAL'
            """)
    List<Invoice> findAllForInterim(Long invoiceId);

    @Query("""
            select new bg.energo.phoenix.service.billing.invoice.reversal.InvoiceReversalModel(inv.id,inv.invoiceDocumentType,inv.invoiceType,inv.standardInvoiceId,substring(inv.invoiceNumber,length(inv.invoiceNumber)-9)) from Invoice inv
            where substring(inv.invoiceNumber,length(inv.invoiceNumber)-9) in :numbers
            and inv.invoiceStatus='REAL'
            and inv.invoiceDocumentType='INVOICE'
            and date(inv.createDate) = date((select max(inv2.createDate) from Invoice  inv2 where inv2.id=inv.id))
            and not exists (select inv3.id from Invoice inv3 where inv3.reversalCreatedFromId =inv.id and inv3.invoiceStatus<>'CANCELLED')
            """)
    List<InvoiceReversalModel> findAllByInvoiceNumberAndNewest(Collection<String> numbers);

    @Query("""
            select new bg.energo.phoenix.service.billing.invoice.reversal.InvoiceReversalModel(inv.id,inv.invoiceDocumentType,inv.invoiceType,inv.standardInvoiceId,substring(inv.invoiceNumber,length(inv.invoiceNumber)-9)) from Invoice inv
            where substring(inv.invoiceNumber,length(inv.invoiceNumber)-9) in :numbers
            and inv.invoiceStatus='REAL'
            and date(inv.createDate) = date((select max(inv2.createDate) from Invoice  inv2 where inv2.id=inv.id))
            
            """)
    List<InvoiceReversalModel> findAllByNumberForCancellation(Collection<String> numbers);

    @Query("""
            select new bg.energo.phoenix.service.billing.invoice.reversal.InvoiceReversalModel(inv.id,inv.invoiceDocumentType,inv.invoiceType,inv.standardInvoiceId,substring(inv.invoiceNumber,length(inv.invoiceNumber)-9)) from Invoice inv
            where inv.invoiceStatus='REAL'
            and substring(inv.invoiceNumber,length(inv.invoiceNumber)-9)=:number
            and date(inv.createDate) = :createDate
            and inv.invoiceDocumentType='INVOICE'
             and not exists (select inv3.id from Invoice inv3 where inv3.reversalCreatedFromId =inv.id and inv3.invoiceStatus <> 'CANCELLED')
            """)
    Optional<InvoiceReversalModel> findAllByInvoiceNumberAndDate(String number, LocalDate createDate);

    @Query("""
                    select inv from Invoice inv
                    where inv.standardInvoiceId=:standardId
                    and inv.invoiceType='INTERIM_AND_ADVANCE_PAYMENT'
            """)
    List<Invoice> findAllInterimByStandardId(Long standardId);

    @Query("""
                select i, b.prefixType
                from Invoice i
                left join BillingRun b
                on b.id = i.billingId
                where i.id in (:invoiceIds)
            """)
    List<Object[]> findAllWithPrefixByInvoiceIds(Set<Long> invoiceIds);


    @Query(value = """
                select rinv3.invoice_number as invoiceNumber ,
                       rinv2.id reversalId,
                       inv2.id as invoiceId from invoice.invoices inv2
                join invoice.invoices cor1 on cor1.standard_invoice_id=inv2.id
                join invoice.invoices rinv3 on rinv3.reversal_created_from_id=cor1.id
                left join invoice.invoices rinv2 on rinv2.reversal_created_from_id = cor1.id and not exists (
                    select 1
                    from billing.billing_run_draft_invoices_marks mark
                    where mark.billing_run_id = rinv2.billing_id
                      and mark.invoice_id = rinv2.id
                )
                where inv2.id in (select rinv1.reversal_created_from_id from invoice.invoices rinv1
                                                                                 join invoice.invoices inv1 on inv1.id=rinv1.reversal_created_from_id
                                  where rinv1.billing_id = :billingRunId
                                    and rinv1.status = 'DRAFT'
                                    and inv1.standard_invoice_id is null)
            
            """, nativeQuery = true)
    List<ReversalValidationObject> findAllValidObjectForDraft(Long billingRunId);

    @Query(value = """
                select rinv3.invoice_number as invoiceNumber ,
                       rinv2.id reversalId,
                       inv2.id as invoiceId from invoice.invoices inv2
                join invoice.invoices cor1 on cor1.standard_invoice_id=inv2.id
                join invoice.invoices rinv3 on rinv3.reversal_created_from_id=cor1.id
                left join invoice.invoices rinv2 on rinv2.reversal_created_from_id = cor1.id and not exists (
                    select 1
                    from billing.billing_run_draft_pdf_invoices_marks mark
                    where mark.billing_run_id = rinv2.billing_id
                      and mark.invoice_id = rinv2.id
                )
                where inv2.id in (select rinv1.reversal_created_from_id from invoice.invoices rinv1
                                                                                 join invoice.invoices inv1 on inv1.id=rinv1.reversal_created_from_id
                                  where rinv1.billing_id = :billingRunId
                                    and rinv1.status = 'DRAFT_GENERATED'
                                    and inv1.standard_invoice_id is null)
            
            """, nativeQuery = true)
    List<ReversalValidationObject> findAllValidObjectForReal(Long billingRunId);

    @Query("""
            select new  bg.energo.phoenix.model.response.billing.invoice.InvoiceTypesDto(inv.invoiceType,inv.invoiceDocumentType) from Invoice inv 
            where inv.id=:invoiceId
            """)
    InvoiceTypesDto findInvoiceTypeById(Long invoiceId);


    @Query(value = """
                select i from Invoice i
                where i.invoiceStatus = 'REAL'
                                and :contractId = case when :contractType = 'PRODUCT_CONTRACT' then  i.productContractId  else i.serviceContractId end
                                and coalesce(i.contractBillingGroupId,-1) = coalesce(:billingGroupId,-1)
                                and coalesce(i.podId,-1) = coalesce(:podId,-1)
                                and i.isDeducted = false
                                and text(i.contractType) = :contractType
                                and i.deductedForInvoiceId is null
                                and i.invoiceSlot =:invoiceSlot
                                and i.invoiceType = 'INTERIM_AND_ADVANCE_PAYMENT'
                                and i.issuingForTheMonth = :checkDate
                                and i.deductionFromType = 'FIRST_INVOICE_FOR_SAME_PERIOD'
                                and (select count(1) from DeductionInterimInvoice dii where dii.interimInvoiceId=i.id and dii.billingId=:billingRunId)<=0
                 union
                 (select i from Invoice i
                 where i.invoiceStatus = 'REAL'
                                and i.productContractId  is null 
                                and i.serviceContractId  is null
                                and i.contractBillingGroupId is null 
                                and i.podId is null
                                and i.isDeducted = false
                                and text(i.contractType) = :contractType
                                and i.deductedForInvoiceId is null
                                and i.invoiceSlot =:invoiceSlot
                                and i.invoiceType = 'INTERIM_AND_ADVANCE_PAYMENT'
                                and i.issuingForTheMonth = :checkDate
                                and i.deductionFromType = 'FIRST_INVOICE_FOR_SAME_PERIOD'
                                 and i.customerId = :customerId
                                 and (select count(1) from DeductionInterimInvoice dii where dii.interimInvoiceId=i.id and dii.billingId=:billingRunId)<=0)
            
            """)
    List<Invoice> getAllNotDeductedInterimSamePeriod(Long contractId,
                                                     Long billingGroupId,
                                                     Long podId,
                                                     String contractType,
                                                     String invoiceSlot,
                                                     LocalDate checkDate,
                                                     Long customerId,
                                                     Long billingRunId);

    @Query(value = """
                select i from Invoice i
                where i.invoiceStatus = 'REAL'
                                and :contractId = case when :contractType = 'PRODUCT_CONTRACT' then  i.productContractId  else i.serviceContractId end
                                and coalesce(i.contractBillingGroupId,-1) = coalesce(:billingGroupId,-1)
                                and coalesce(i.podId,-1) = coalesce(:podId,-1)
                                and i.isDeducted = false
                                and i.deductedForInvoiceId is null
                                and text(i.contractType) =:contractType
                                and i.invoiceSlot =:invoiceSlot
                                and i.issuingForPaymentTermDate < :checkDate
                                and i.invoiceType = 'INTERIM_AND_ADVANCE_PAYMENT'
                                and i.deductionFromType = 'FIRST_INVOICE_WITH_LONGER_PAYMENT_TERM'
                                and (select count(1) from DeductionInterimInvoice dii where dii.interimInvoiceId=i.id and dii.billingId=:billingRunId)<=0
                 union 
                 (select i from Invoice i
                where i.invoiceStatus = 'REAL'
                                and i.productContractId  is null 
                                and i.serviceContractId  is null
                                and i.contractBillingGroupId is null 
                                and i.podId is null
                                and i.isDeducted = false
                                and i.deductedForInvoiceId is null
                                and text(i.contractType) =:contractType
                                and i.invoiceSlot =:invoiceSlot
                                and i.issuingForPaymentTermDate < :checkDate
                                and i.invoiceType = 'INTERIM_AND_ADVANCE_PAYMENT'
                                and i.deductionFromType = 'FIRST_INVOICE_WITH_LONGER_PAYMENT_TERM'
                                 and i.customerId = :customerId
                                 and (select count(1) from DeductionInterimInvoice dii where dii.interimInvoiceId=i.id and dii.billingId=:billingRunId)<=0)
            """)
    List<Invoice> getAllNotDeductedInterimLongPaymentTerm(Long contractId,
                                                          Long billingGroupId,
                                                          Long podId,
                                                          String contractType,
                                                          String invoiceSlot,
                                                          LocalDate checkDate,
                                                          Long customerId,
                                                          Long billingRunId);

    @Query(value = """
            select new bg.energo.phoenix.model.response.billing.invoice.InvoiceSummaryDataResponse(
            i.basisForIssuing,
            i.totalAmountExcludingVat,
            (select c.name from Currency c where c.id = i.currencyId),
            i.incomeAccountNumber,
            i.costCenterControllingOrder,
            (select vr.vatRatePercent from InvoiceVatRateValue vr where vr.invoiceId = i.id),
            'DIRECT'
            )
            from Invoice i
            where i.id = :id
            """, countQuery = """
            select count(i.id)
             from Invoice i
             where i.id = :id
            """)
    Page<InvoiceSummaryDataResponse> findManualInterimSummaryDataByInvoiceId(Long id, PageRequest pageRequest);

    List<Invoice> findAllByGoodsOrderIdIn(List<Long> orderIds);

    @Query(value = """
            select inv.id as invoiceId,
                    r.receivable_number as number
            from billing.billings b
            join invoice.invoices inv on inv.billing_id = b.id
            join receivable.customer_receivables r on r.invoice_id = inv.id
            where b.id = :billingRunId
            union
            select inv.id as invoiceId,
                    l.liability_number as number
            from billing.billings b
            join invoice.invoices inv on inv.billing_id = b.id
            join receivable.customer_liabilities l on l.invoice_id = inv.id
            where b.id = :billingRunId
            """, nativeQuery = true)
    List<InvoiceLiabilitiesAndReceivablesExportModel> findCustomerLiabilitiesAndReceivablesForInvoice(Long billingRunId);

    @Query(value = """
            select case
                       when
                           exists(select idd.id
                                  from invoice.invoice_standard_detailed_data idd
                                  where idd.invoice_id = :id
                                    and (text(idd.detail_type) in ('DISCOUNT', 'SETTLEMENT', 'SCALE')
                                      or (
                                             text(idd.detail_type) in ('OVER_TIME_ONE_TIME', 'OVER_TIME_PERIODICAL')
                                                 and idd.pod_id is not null
                                             )))
                               or exists(select d.id
                                         from invoice.manual_debit_or_credit_note_invoice_detailed_data d
                                         where d.invoice_id = :id)
                               or exists(select m.id
                                         from invoice.manual_invoice_detailed_data m
                                         where m.invoice_id = :id)
                               or exists(select idd.id
                                         from invoice.invoice_detailed_data idd
                                         where idd.invoice_id = :id
                                           and (idd.pod_id is not null or idd.unrecognized_pod is not null))
                           then true
                       else false end
            """, nativeQuery = true)
    boolean hasThirdTab(Long id);

    @Query(value = """
            
             with customer_invoice as (select *
                                      from invoice.invoices
                                      where id = :invoiceId),
                 invoice_type as (select case
                                             when ci.product_contract_id is not null then 'PRODUCT_CONTRACT'
                                             when ci.service_contract_id is not null then 'SERVICE_CONTRACT'
                                             when ci.goods_order_id is not null then 'GOODS_ORDER'
                                             else 'SERVICE_ORDER' end as type
                                  from customer_invoice ci),
                 invoice_customer as (select c.id                                                                   as customer_id,
                                             cd.id                                                                  as customer_detail_id,
                                             case
                                                 when c.customer_type = 'PRIVATE_CUSTOMER'
                                                     then concat(cd.name, ' ', cd.middle_name, ' ', cd.last_name)
                                                 else concat(cd.name, ' ', lf.name) end                             as customer_name,
                                             case
                                                 when c.customer_type = 'PRIVATE_CUSTOMER'
                                                     then concat(cd.name_transl, ' ', cd.middle_name_transl, ' ',
                                                                 cd.last_name_transl)
                                                 else concat(cd.name_transl, ' ', lf.name) end                      as customer_name_trns,
                                             cd.vat_number                                                          as vat_number,
                                             c.identifier                                                           as identifier,
                                             c.customer_number                                                      as customer_number,
                                             case
                                                 when cd.foreign_address = false then
                                                     concat_ws(', ',
                                                               nullif(distr.name, ''),
                                                               nullif(concat_ws(' ',
                                                                                case when ra.name is null then null else replace(text(cd.residential_area_type), '_', ' ') end ,
                                                                                ra.name), ''),
                                                               nullif(
                                                                       concat_ws(' ', case when str.name is null then null else cd.street_type end, str.name, case when str.name is null then null else cd.street_number end),
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
                                                                                case when cd.residential_area_foreign is null then null else  replace(text(cd.foreign_residential_area_type), '_', ' ') end,
                                                                                cd.residential_area_foreign), ''),
                                                               nullif(
                                                                       concat_ws(' ', case when cd.street_foreign is null then null else cd.foreign_street_type end, cd.street_foreign,case when cd.street_foreign is null then null else cd.street_number end),
                                                                       ''),
                                                               nullif(concat('бл. ', cd.block), 'бл. '),
                                                               nullif(concat('вх. ', cd.entrance), 'вх. '),
                                                               nullif(concat('ет. ', cd.floor), 'ет. '),
                                                               nullif(concat('ап. ', cd.apartment), 'ап. '),
                                                               cd.address_additional_info
                                                     )
                                                 end                                                                as customer_addr_comb,
                                             case
                                                 when cd.foreign_address = false then pp.name
                                                 else cd.populated_place_foreign end                                as populated_place,
                                             case
                                                 when cd.foreign_address = false then zc.zip_code
                                                 else cd.zip_code_foreign end                                       as zip_code,
                                             case
                                                 when cd.foreign_address = false then distr.name
                                                 else cd.district_foreign end                                       as district,
                                             case
                                                 when cd.foreign_address = false
                                                     then replace(text(cd.residential_area_type), '_', ' ')
                                                 else replace(text(cd.foreign_residential_area_type), '_', ' ') end as quarter_ra_type,
                                             case
                                                 when cd.foreign_address = false then ra.name
                                                 else cd.residential_area_foreign end                               as quarter_ra_name,
                                             case
                                                 when cd.foreign_address = false then cd.street_type
                                                 else cd.foreign_street_type end                                    as blvd_str_type,
                                             case
                                                 when cd.foreign_address = false
                                                     then str.name
                                                 else cd.street_foreign end                                         as str_blvd_name,
                                             cd.street_number                                                       as str_blvd_number,
                                             cd.block                                                               as block,
                                             cd.entrance                                                            as entrance,
                                             cd.floor                                                               as floor,
                                             cd.apartment                                                           as apartment,
                                             cd.address_additional_info                                             as additional_info
                                      from customer.customer_details cd
                                               join customer.customers c
                                                    on cd.customer_id = c.id
                                               left join nomenclature.legal_forms lf on cd.legal_form_id = lf.id
                                               left join nomenclature.districts distr on cd.district_id = distr.id
                                               left join nomenclature.zip_codes zc on cd.zip_code_id = zc.id
                                               left join nomenclature.residential_areas ra on cd.residential_area_id = ra.id
                                               left join nomenclature.streets str on cd.street_id = str.id
                                               left join nomenclature.populated_places pp on cd.populated_place_id = pp.id
                                      where cd.id = (select customer_detail_id from customer_invoice)),
                 customer_segments as (select s.name as segment
                                       from customer.customer_segments cs
                                                join nomenclature.segments s on cs.segment_id = s.id
                                       where cs.customer_detail_id = (select customer_detail_id from customer_invoice)
                                         and cs.status = 'ACTIVE'),
                 invoice_billing_group as (select cbg.group_number as group_number
                                           from product_contract.contract_billing_groups cbg
                                           where cbg.id =
                                                 (select customer_invoice.contract_billing_group_id
                                                  from customer_invoice)),
                 invoice_contract as (select case
                                                 when it.type like 'PRODUCT_CONTRACT' then (select pc.contract_number
                                                                                            from product_contract.contracts pc
                                                                                            where pc.id = (select ci.product_contract_id from customer_invoice ci))
                                                 when it.type like 'SERVICE_CONTRACT' then (select sc.contract_number
                                                                                            from service_contract.contracts sc
                                                                                            where sc.id = (select ci.service_contract_id from customer_invoice ci))
                                                 when it.type like 'GOODS_ORDER' then (select go.order_number
                                                                                       from goods_order.orders go
                                                                                       where go.id = (select ci.goods_order_id from customer_invoice ci))
                                                 else (select se.order_number
                                                       from service_order.orders se
                                                       where se.id = (select ci.service_order_id from customer_invoice ci)) end as contract
                                      from invoice_type it),
                 invoice_currency as (select ic.*
                                      from nomenclature.currencies ic
                                      where id = (select ci.currency_id from customer_invoice ci)),
                 invoice_other_currency as (select ic.*
                                            from nomenclature.currencies ic
                                            where id = (select ci.currency_id_in_other_currency from customer_invoice ci)),
                 billing_run as (
                     select b.billing_number from billing.billings b
                                                      join customer_invoice ci on ci.billing_id=b.id
                 )
            select (select ic.customer_name from invoice_customer ic)                                as CustomerNameComb,
                   (select ic.customer_name_trns from invoice_customer ic)                           as CustomerNameCombTrsl,
                   (select ic.identifier from invoice_customer ic)                                   as CustomerIdentifer,
                   (select ic.vat_number from invoice_customer ic)                                   as CustomerVat,
                   (select ic.customer_number from invoice_customer ic)                              as CustomerNumber,
                   translation.translate_text((select ic.customer_addr_comb from invoice_customer ic) ,text('BULGARIAN'))                          as CustomerAddressComb,
                   (select ic.populated_place from invoice_customer ic)                              as CustomerPopulatedPlace,
                   (select ic.zip_code from invoice_customer ic)                                     as CustomerZip,
                   (select ic.district from invoice_customer ic)                                     as CustomerDistrict,
                   translation.translate_text((select ic.quarter_ra_type from invoice_customer ic)  , text('BULGARIAN'))                            as CustomerQuarterRaType,
                   (select ic.quarter_ra_name from invoice_customer ic)                              as CustomerQuarterRaName,
                   translation.translate_text(text((select ic.blvd_str_type from invoice_customer ic)),text('BULGARIAN'))       as CustomerStrBlvdType,
                   (select ic.str_blvd_name from invoice_customer ic)                                as CustomerStrBlvdName,
                   (select ic.str_blvd_number from invoice_customer ic)                              as CustomerStrBlvdNumber,
                   (select ic.block from invoice_customer ic)                                        as CustomerBlock,
                   (select ic.entrance from invoice_customer ic)                                     as CustomerEntrance,
                   (select ic.floor from invoice_customer ic)                                        as CustomerFloor,
                   (select ic.apartment from invoice_customer ic)                                    as CustomerApartment,
                   (select ic.additional_info from invoice_customer ic)                              as CustomerAdditionalInfo,
                   (select array_agg(cs.segment) from customer_segments cs)                          as CustomerSegments,
                   (select ibg.group_number from invoice_billing_group ibg)                          as BillingGroup,
                   (select ic.contract from invoice_contract ic)                                     as ContractNumber,
                   translation.translate_text(replace(text((select ci.document_type from customer_invoice ci)), '_', ' '),text('BULGARIAN'))    as DocumentType,
                   (select substring(ci.invoice_number from '-(.*)') from customer_invoice ci)       as DocumentNumber,
                   (select substring(ci.invoice_number from '^[^-]+')
                    from customer_invoice ci)                                                        as DocumentPrefix,
                   (select ci.invoice_date from customer_invoice ci)                                 as DocumentDate,
                   (select ci.tax_event_date from customer_invoice ci)                               as TaxEventDate,
                   (select ci.meter_reading_period_from from customer_invoice ci)                    as MeterReadingFrom,
                   (select ci.meter_reading_period_to from customer_invoice ci)                      as MeterReadingTo,
                   (select case
                               when ci.meter_reading_period_to is not null then concat(
                                       extract(month from ci.meter_reading_period_to), '-',
                                       extract(year from ci.meter_reading_period_to))
                               end
                    from customer_invoice ci)                                                        as InvoicedMonth,
                   (select ci.payment_deadline from customer_invoice ci)                             as PaymentDeadline,
                   (select ci.basis_for_issuing from customer_invoice ci)                            as BasisForIssuing,
                   (select ic.print_name from invoice_currency ic)                                   as CurrencyPrintName,
                   (select ic.abbreviation from invoice_currency ic)                                 as CurrencyAbr,
                   (select ic.full_name from invoice_currency ic)                                    as CurrencyFullName,
                   (select iot.print_name from invoice_other_currency iot)                           as OtherCurrencyPrintName,
                   (select iot.abbreviation from invoice_other_currency iot)                         as OtherCurrencyAbr,
                   (select iot.full_name from invoice_other_currency iot)                            as OtherCurrencyFullName,
                   (select ci.total_amount_excluding_vat from customer_invoice ci)                   as TotalExclVat,
                   (select ci.total_amount_of_vat from customer_invoice ci)                          as TotalVat,
                   (select ci.total_amount_including_vat from customer_invoice ci)                   as TotalInclVat,
                   (select ci.total_amount_including_vat_in_other_currency from customer_invoice ci) as TotalInclVatOtherCurrency,
                   (select ci.total_amount_including_vat from customer_invoice ci)                   as FinalLiabilityAmount,
                   (select bi.billing_number from billing_run bi)                   as billingNumber
            from customer_invoice inv
            """, nativeQuery = true)
    BillingRunDocumentModel getInvoiceDocumentModel(Long invoiceId);

    @Query("""
            select distinct i,
                   go,
                   so
            from Invoice i
            join CustomerLiability cl on cl.invoiceId = i.id
            left join GoodsOrder go on i.goodsOrderId = go.id 
            left join ServiceOrder so on i.serviceOrderId = so.id
            where coalesce(i.serviceOrderId, i.goodsOrderId, 0) <> 0
            and i.invoiceStatus = 'REAL'
            and text(i.invoiceDocumentType) in ('PROFORMA_INVOICE', 'INVOICE')
            and i.invoiceType = 'STANDARD'
            and i.billingId is null
            and cl.currentAmount = 0
            and cl.status = 'ACTIVE'
            and ((go.id is not null and go.orderStatus= 'AWAITING_PAYMENT' and go.status = 'ACTIVE' ) or 
                (so.id is not null and so.status = 'ACTIVE' and so.orderStatus = 'AWAITING_PAYMENT'))
            """)
    List<Object[]> findByIdAndLiabilityAmountZero();

    @Query(value = """
            select doc from InvoiceDocumentFile idoc
            join Document doc on idoc.documentId=doc.id
            where idoc.invoiceId=:id
            and idoc.createDate=(select max(idoc2.createDate) from InvoiceDocumentFile idoc2 where idoc2.invoiceId=:id)
            """)
    Optional<Document> findLatestInvoiceDocument(Long id);

    @Query("""
            select coalesce(
                        run.emailTemplateId,
                        case
                              when inv.productDetailId is not null
                                then (select pt.templateId from ProductTemplate pt where pt.productDetailId = pd.id and pt.status = 'ACTIVE' and pt.type = 'EMAIL_TEMPLATE')
                              when inv.serviceDetailId is not null
                                then (select st.templateId from ServiceTemplate st where st.serviceDetailId = sd.id and st.status = 'ACTIVE' and st.type = 'EMAIL_TEMPLATE')
                              when inv.goodsOrderId is not null
                                then go.emailTemplateId
                              else so.emailTemplateId
                        end
            )
            from Invoice inv
            join BillingRun run on run.id = inv.billingId
            left join ProductDetails pd on pd.id = inv.productDetailId
            left join ServiceDetails sd on sd.id = inv.serviceDetailId
            left join GoodsOrder go on go.id = inv.goodsOrderId
            left join ServiceOrder so on so.id = inv.serviceOrderId
            where inv.id = :invoiceId
            """)
    Optional<Long> findValidEmailTemplateIdByInvoiceId(Long invoiceId);

    @Query("""
                select inv.id from Invoice inv
                where inv.billingId=:billingId
                and inv.contractBillingGroupId is null
            """)
    List<Long> findInvoiceIdsWithoutBillingGroup(Long billingId);

    @Query("""
                            select  b.prefixType
                from Invoice i
                left join BillingRun b
                on b.id = i.billingId
                where i.id = :invoiceIds
            """)
    Optional<PrefixType> findPrefixTypeForInvoice(Long invoiceIds);


    @Query(value = """
                 with firstInvoice as  (select (coalesce((select cast(substring(inv.invoice_number,length(inv.invoice_number)-8) as bigint) invNumber
                                                          from invoice.invoices inv
                                                          where inv.invoice_number_modify_date>:modifyDate and inv.document_type<>'PROFORMA_INVOICE' and (inv.status='REAL' or inv.status='DRAFT_GENERATED') order by inv.invoice_number_modify_date
                                                          limit 1),(select cast(substring(inv.invoice_number,length(inv.invoice_number)-8) as bigint) invNumber from invoice.invoices inv
                                                                    where inv.document_type<>'PROFORMA_INVOICE' and (inv.status='REAL' or inv.status='DRAFT_GENERATED')
                                                                    order by inv.id desc limit 1))) as invNumber  ),
                      lastInvoice as (select seq.last_value as invNumber from invoice.real_invoice_number_seq seq),
                      numbers as (SELECT lpad(series::text, 10, '0') AS seq
                                  FROM generate_series((select firstInvoice.invNumber from firstInvoice),(select lastInvoice.invNumber from lastInvoice)) AS series)
                 select nb.seq,cast('REAL' as invoice.invoice_number_type) from numbers nb
                                                                                    left join invoice.invoices inv on substring(inv.invoice_number,length(inv.invoice_number)-9) = nb.seq and (inv.status='REAL' or inv.status='DRAFT_GENERATED') and inv.document_type<>'PROFORMA_INVOICE' and inv.invoice_number_modify_date>:modifyDate
                 where inv IS NULL
                 union all
                 (
            
                     with firstInvoice as  (select (coalesce((select cast(substring(inv.invoice_number,length(inv.invoice_number)-8) as bigint) invNumber
                                                              from invoice.invoices inv
                                                              where inv.invoice_number_modify_date>:modifyDate and inv.document_type='PROFORMA_INVOICE' and (inv.status='REAL' or inv.status='DRAFT_GENERATED') order by inv.invoice_number_modify_date
                                                              limit 1),(select cast(substring(inv.invoice_number,length(inv.invoice_number)-8) as bigint) invNumber from invoice.invoices inv
                                                                        where inv.document_type='PROFORMA_INVOICE' and (inv.status='REAL' or inv.status='DRAFT_GENERATED')
                                                                        order by inv.id desc limit 1))) as invNumber ),
                          lastInvoice as (select seq.last_value as invNumber from invoice.real_proforma_invoice_number_seq seq),
                          numbers as (SELECT lpad(series::text, 10, '0') AS seq
                                      FROM generate_series((select firstInvoice.invNumber from firstInvoice),(select lastInvoice.invNumber from lastInvoice)) AS series)
                     select nb.seq,cast('REAL_PROFORMA' as invoice.invoice_number_type) as numberType from numbers nb
                                                                                                               left join invoice.invoices inv on substring(inv.invoice_number,length(inv.invoice_number)-9) = nb.seq and (inv.status='REAL' or inv.status='DRAFT_GENERATED') and inv.document_type='PROFORMA_INVOICE' and inv.invoice_number_modify_date>:modifyDate
                     where inv IS NULL
                 )
                 union all
                 (
            
                     with firstInvoice as  (select (coalesce((select cast(substring(inv.invoice_number,length(inv.invoice_number)-8) as bigint) invNumber
                                                              from invoice.invoices inv
                                                              where inv.invoice_number_modify_date>:modifyDate and inv.document_type<>'PROFORMA_INVOICE' and (inv.status='DRAFT') order by inv.invoice_number_modify_date
                                                              limit 1),(select cast(substring(inv.invoice_number,length(inv.invoice_number)-8) as bigint) invNumber from invoice.invoices inv
                                                                        where inv.document_type<>'PROFORMA_INVOICE' and (inv.status='DRAFT')
                                                                        order by inv.id desc limit 1))) as invNumber ),
                          lastInvoice as (select seq.last_value as invNumber from invoice.draft_invoice_number_seq seq),
                          numbers as (SELECT lpad(series::text, 9, '0') AS seq
                                      FROM generate_series((select firstInvoice.invNumber from firstInvoice),(select lastInvoice.invNumber from lastInvoice)) AS series)
                     select nb.seq,cast('DRAFT_INVOICE' as invoice.invoice_number_type) as numberType from numbers nb
                                                                                                               left join invoice.invoices inv on substring(inv.invoice_number,length(inv.invoice_number)-8) = nb.seq and inv.status='DRAFT' and inv.document_type<>'PROFORMA_INVOICE' and inv.invoice_number_modify_date>:modifyDate
                     where inv IS NULL
                 )
                 union all
                 (
            
                     with firstInvoice as  (select (coalesce((select cast(substring(inv.invoice_number,length(inv.invoice_number)-8) as bigint) invNumber
                                                              from invoice.invoices inv
                                                              where inv.invoice_number_modify_date>:modifyDate and inv.document_type='PROFORMA_INVOICE' and (inv.status='DRAFT') order by inv.invoice_number_modify_date
                                                              limit 1),(select cast(substring(inv.invoice_number,length(inv.invoice_number)-8) as bigint) invNumber from invoice.invoices inv
                                                                        where inv.document_type='PROFORMA_INVOICE' and (inv.status='DRAFT')
                                                                        order by inv.id desc limit 1))) as invNumber  ),
                          lastInvoice as (select seq.last_value as invNumber from invoice.draft_proforma_invoice_number_seq seq),
                          numbers as (SELECT lpad(series::text, 9, '0') AS seq
                                      FROM generate_series((select firstInvoice.invNumber from firstInvoice),(select lastInvoice.invNumber from lastInvoice)) AS series)
                     select nb.seq,cast('DRAFT_PROFORMA' as invoice.invoice_number_type) as numberType from numbers nb
                                                                                                                left join invoice.invoices inv on substring(inv.invoice_number,length(inv.invoice_number)-8) = nb.seq and inv.status='DRAFT' and inv.document_type='PROFORMA_INVOICE' and inv.invoice_number_modify_date>:modifyDate
                     where inv IS NULL
                 )
            
            """, nativeQuery = true)
    List<InvoiceNumberDto> getMissedInvoiceNumbers(LocalDateTime modifyDate);

    @Query("""
            select new bg.energo.phoenix.model.response.shared.ShortResponse(
                inv.id,
                inv.invoiceNumber
            )
            from Invoice inv
            where inv.parentInvoiceId = :invoiceId and inv.invoiceStatus = 'REAL'
            """)
    List<ShortResponse> findInvoiceDebitCreditNotes(Long invoiceId);

    boolean existsInvoiceByInvoiceStatusAndProductContractId(InvoiceStatus invoiceStatus, Long productContractId);

    boolean existsInvoiceByInvoiceStatusAndServiceContractId(InvoiceStatus invoiceStatus, Long serviceContractId);

    boolean existsInvoiceByInvoiceStatusAndGoodsOrderId(InvoiceStatus invoiceStatus, Long goodsOrderId);

    boolean existsInvoiceByInvoiceStatusAndServiceOrderId(InvoiceStatus invoiceStatus, Long serviceOrderId);

    @Query("""
            select conInv
            from Invoice  inv 
            join BillingRunInvoices bri on bri.billingId=inv.billingId
            join Invoice conInv on conInv.id=bri.invoiceId
            where inv.id=:invoiceId
            """)
    List<Invoice> findConnectedCreditDebitNotes(Long invoiceId);

    @Query("""
            select pc.contractNumber
            from Invoice inv
            join ProductContract pc on pc.id=inv.productContractId
            where inv.id=:id
            """)
    Optional<String> getInterimErrorObject(Long id);

    @Query("""
            select inv from DeductionInterimInvoice dii
            join Invoice inv on dii.interimInvoiceId=inv.id
            where dii.invoiceId=:standardInvoiceId
            """)
    List<Invoice> findDeductibleInvoicesForRealStandardInvoice(Long standardInvoiceId);
}
