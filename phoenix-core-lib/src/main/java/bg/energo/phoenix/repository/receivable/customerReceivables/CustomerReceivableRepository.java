package bg.energo.phoenix.repository.receivable.customerReceivables;

import bg.energo.phoenix.model.entity.EntityStatus;
import bg.energo.phoenix.model.entity.receivable.CustomerReceivable;
import bg.energo.phoenix.model.entity.receivable.customerLiability.CustomerLiabilityPaidByReceivable;
import bg.energo.phoenix.model.entity.receivable.payment.PaymentReceivableOffsetting;
import bg.energo.phoenix.model.response.billing.invoice.InvoiceConnectionsShortResponse;
import bg.energo.phoenix.model.response.billing.invoice.InvoiceLiabilitiesReceivableResponse;
import bg.energo.phoenix.model.response.receivable.customerLiability.CustomerLiabilityTransactionsResponse;
import bg.energo.phoenix.model.response.receivable.customerReceivable.CustomerReceivableMiddleResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface CustomerReceivableRepository extends JpaRepository<CustomerReceivable, Long> {

    Optional<CustomerReceivable> findByIdAndStatus(Long id, EntityStatus status);

    @Query(
            nativeQuery = true,
            value = """
                    
                                    select receivable_number                                                    as receivableNumber,
                           case
                               when c.customer_type = 'PRIVATE_CUSTOMER' then
                                   concat(c.identifier, ' (', cd.name,
                                          case
                                              when cd.middle_name is not null then concat(' ', cd.middle_name)
                                              else ''
                                              end,
                                          case
                                              when cd.last_name is not null then concat(' ', cd.last_name)
                                              else ''
                                              end,
                                          ')'
                                   )
                               when c.customer_type = 'LEGAL_ENTITY' then
                                   concat(c.identifier, ' (', cd.name, ' ', lf.name, ')')
                               end                                                              as customer,
                           cbg.group_number                                                     as billingGroup,
                           (select case
                                       when c1.customer_type = 'PRIVATE_CUSTOMER' then
                                           concat(c1.identifier, ' (', cd1.name,
                                                  case
                                                      when cd1.middle_name is not null then concat(' ', cd1.middle_name)
                                                      else ''
                                                      end,
                                                  case
                                                      when cd1.last_name is not null then concat(' ', cd1.last_name)
                                                      else ''
                                                      end,
                                                  ')'
                                           )
                                       when c1.customer_type = 'LEGAL_ENTITY' then
                                           concat(c1.identifier, ' (', cd1.name, ' ', lf1.name, ')')
                                       end
                            from customer.customer_details cd1
                                     left join nomenclature.legal_forms lf1 on lf1.id = cd1.legal_form_id
                                     join customer.customers c1 on cd1.id = c1.last_customer_detail_id
                                and c1.id = cr.alt_invoice_recipient_customer_id)               as alternativeRecipient,
                           receivable.convert_to_currency(cr.initial_amount, cr.currency_id, 0) as initialAmount,
                           receivable.convert_to_currency(cr.current_amount, cr.currency_id, 0) as currentAmount,
                           cr.id                                                                as id,
                           cr.status                                                            as status,
                           accounting_period.status                                             as accountingPeriodStatus,
                           cr.creation_type                                                     as creationType,
                           cr.occurrence_date                                                   as occurrenceDate,
                           cr.due_date                                                          as dueDate
                    from receivable.customer_receivables cr
                             join customer.customers c on cr.customer_id = c.id
                             join customer.customer_details cd on c.last_customer_detail_id = cd.id
                             left join nomenclature.legal_forms lf on lf.id = cd.legal_form_id
                             left join product_contract.contract_billing_groups cbg on cr.contract_billing_group_id = cbg.id
                        and cbg.status = 'ACTIVE'
                             join billing.account_periods as accounting_period on accounting_period.id = cr.account_period_id
                    where ((:statuses) is null or text(cr.status) in :statuses)
                      and (coalesce(:initialAmountFrom, '0') = '0' or
                           receivable.convert_to_currency(cr.initial_amount, cr.currency_id, 0) >= :initialAmountFrom)
                      and (coalesce(:initialAmountTo, '0') = '0' or
                           receivable.convert_to_currency(cr.initial_amount, cr.currency_id, 0) <= :initialAmountTo)
                      and (coalesce(:currentAmountFrom, '0') = '0' or
                           receivable.convert_to_currency(cr.current_amount, cr.currency_id, 0) >= :currentAmountFrom)
                      and (coalesce(:currentAmountTo, '0') = '0' or
                           receivable.convert_to_currency(cr.current_amount, cr.currency_id, 0) <= :currentAmountTo)
                      and ((:currencyIds) is null or cr.currency_id in (:currencyIds))
                      and (:blockedForPayment is null or cr.blocked_for_payment = :blockedForPayment)
                      and (coalesce(:billingGroup, '0') = '0' or concat(c.id, cbg.group_number) = :billingGroup)
                      and (date(:occurrenceDateFrom) is null or date(cr.occurrence_date) >= date(:occurrenceDateFrom))
                      and (date(:occurrenceDateTo) is null or date(cr.occurrence_date) <= date(:occurrenceDateTo))
                      and (date(:dueDateFrom) is null or date(cr.due_date) >= date(:dueDateFrom))
                      and (date(:dueDateTo) is null or date(cr.due_date) <= date(:dueDateTo))
                      and (:prompt is null or (:searchBy = 'ALL' and (lower(cr.receivable_number) like :prompt
                        or lower(c.identifier) like :prompt
                        or lower(cbg.group_number) like :prompt))
                        or ((:searchBy = 'ID' and lower(cr.receivable_number) like :prompt)
                            or (:searchBy = 'CUSTOMER' and lower(c.identifier) like :prompt)
                            or (:searchBy = 'BILLINGGROUP' and lower(cbg.group_number) like :prompt))
                        )
                    """,
            countQuery = """
                    select count(cr.id)
                                        from receivable.customer_receivables cr
                                                 join
                                             customer.customers c
                                             on cr.customer_id = c.id
                                                 join customer.customer_details cd
                                                      on c.last_customer_detail_id = cd.id
                                                 left join nomenclature.legal_forms lf
                                                      on cd.legal_form_id = lf.id
                                                 left join product_contract.contract_billing_groups cbg
                                                           on cr.contract_billing_group_id = cbg.id
                                                               and cbg.status = 'ACTIVE'
                                        where ((:statuses) is null or text(cr.status) in :statuses)
                                          and (coalesce(:initialAmountFrom, '0') = '0' or cr.initial_amount >= :initialAmountFrom)
                                          and (coalesce(:initialAmountTo, '0') = '0' or cr.initial_amount <= :initialAmountTo)
                                          and (coalesce(:currentAmountFrom, '0') = '0' or cr.current_amount >= :currentAmountFrom)
                                          and (coalesce(:currentAmountTo, '0') = '0' or cr.current_amount <= :currentAmountTo)
                                          and ((:currencyIds) is null or cr.currency_id in (:currencyIds))
                                          and (:blockedForPayment is null or cr.blocked_for_payment = :blockedForPayment)
                                          and (coalesce(:billingGroup, '0') = '0' or concat(c.id, cbg.group_number) = :billingGroup)
                                          and (date(:occurrenceDateFrom) is null or date(cr.occurrence_date) >= date(:occurrenceDateFrom))
                                          and (date(:occurrenceDateTo) is null or date(cr.occurrence_date) <= date(:occurrenceDateTo))
                                          and (date(:dueDateFrom) is null or date(cr.due_date) >= date(:dueDateFrom))
                                          and (date(:dueDateTo) is null or date(cr.due_date) <= date(:dueDateTo))
                                          and (:prompt is null or (:searchBy = 'ALL' and (
                                            lower(cr.receivable_number) like :prompt
                                                or
                                            lower(c.identifier) like :prompt
                                                or
                                            lower(cbg.group_number) like :prompt
                                            )
                                            )
                                            or (
                                                   (:searchBy = 'ID' and lower(cr.receivable_number) like :prompt)
                                                       or
                                                   (:searchBy = 'CUSTOMER' and lower(c.identifier) like :prompt)
                                                       or
                                                   (:searchBy = 'BILLINGGROUP' and lower(cbg.group_number) like :prompt)
                                                   )
                                            )
                    """
    )
    Page<CustomerReceivableMiddleResponse> listing(
            @Param("initialAmountFrom") BigDecimal initialAmountFrom,
            @Param("initialAmountTo") BigDecimal initialAmountTo,
            @Param("currentAmountFrom") BigDecimal currentAmountFrom,
            @Param("currentAmountTo") BigDecimal currentAmountTo,
            @Param("blockedForPayment") Boolean blockedForPayment,
            @Param("prompt") String prompt,
            @Param("searchBy") String searchBy,
            @Param("currencyIds") List<Long> currencyIds,
            @Param("statuses") List<String> statuses,
            @Param("billingGroup") String billingGroup,
            @Param("occurrenceDateFrom") LocalDate occurrenceDateFrom,
            @Param("occurrenceDateTo") LocalDate occurrenceDateTo,
            @Param("dueDateFrom") LocalDate dueDateFrom,
            @Param("dueDateTo") LocalDate dueDateTo,
            Pageable pageable);

    @Query("""
                    select cr from CustomerReceivable cr
                    where cr.receivableNumber=:receivableNumber
                    and cr.status=:status
            """)
    Optional<CustomerReceivable> findByReceivableNumberAndStatus(String receivableNumber, EntityStatus status);

    @Query(
            nativeQuery = true,
            value = """
                    select receivable_number                                                    as receivableNumber,
                           case
                               when c.customer_type = 'PRIVATE_CUSTOMER' then
                                   concat(
                                           c.identifier, ' (',
                                           cd.name,
                                           case when cd.middle_name is not null then concat(' ', cd.middle_name) else '' end,
                                           case when cd.last_name is not null then concat(' ', cd.last_name) else '' end,
                                           ')'
                                   )
                               when c.customer_type = 'LEGAL_ENTITY' then
                                   concat(c.identifier, ' (', cd.name, ')')
                               end                                                                 customer,
                           cbg.group_number                                                     as billingGroup,
                           (select case
                                       when c1.customer_type = 'PRIVATE_CUSTOMER' then
                                           concat(
                                                   c1.identifier, ' (',
                                                   cd1.name,
                                                   case when cd1.middle_name is not null then concat(' ', cd1.middle_name) else '' end,
                                                   case when cd1.last_name is not null then concat(' ', cd1.last_name) else '' end,
                                                   ')'
                                           )
                                       when c1.customer_type = 'LEGAL_ENTITY' then
                                           concat(c1.identifier, ' (', cd1.name, ')')
                                       end
                            from customer.customer_details cd1
                                     join customer.customers c1
                                          on cd1.id = c1.last_customer_detail_id
                                              and c1.id = cr.alt_invoice_recipient_customer_id) as alternativeRecipient,
                           cr.initial_amount                                                    as initialAmount,
                           cr.current_amount                                                    as currentAmount,
                           cr.id                                                                as id,
                           cr.status                                                            as status,
                           accounting_period.status                                             as accountingPeriodStatus,
                           cr.creation_type                                                     as creationType
                    from receivable.customer_receivables cr
                             join customer.customers c on cr.customer_id = c.id
                             join customer.customer_details cd on c.last_customer_detail_id = cd.id and cd.id = :customerDetailId
                             left join product_contract.contract_billing_groups cbg
                                       on cr.contract_billing_group_id = cbg.id and cbg.status = 'ACTIVE'
                             join billing.account_periods as accounting_period on accounting_period.id = cr.account_period_id
                    where ((:statuses) is null or text(cr.status) in :statuses)
                      and (coalesce(:initialAmountFrom, '0') = '0' or cr.initial_amount >= :initialAmountFrom)
                      and (coalesce(:initialAmountTo, '0') = '0' or cr.initial_amount <= :initialAmountTo)
                      and (coalesce(:currentAmountFrom, '0') = '0' or cr.current_amount >= :currentAmountFrom)
                      and (coalesce(:currentAmountTo, '0') = '0' or cr.current_amount <= :currentAmountTo)
                      and ((:currencyIds) is null or cr.currency_id in (:currencyIds))
                      and (:blockedForPayment is null or cr.blocked_for_payment = :blockedForPayment)
                      and (coalesce(:billingGroup, '0') = '0' or concat(c.id, cbg.group_number) = :billingGroup)
                      and (:prompt is null or (:searchBy = 'ALL' and (
                        lower(cr.receivable_number) like :prompt or
                        lower(c.identifier) like :prompt or
                        lower(cbg.group_number) like :prompt))
                        or ((:searchBy = 'ID' and lower(cr.receivable_number) like :prompt)
                            or (:searchBy = 'CUSTOMER' and lower(c.identifier) like :prompt)
                            or (:searchBy = 'BILLINGGROUP' and lower(cbg.group_number) like :prompt))
                        )
                    """,
            countQuery = """
                    select count(cr.id)
                    from receivable.customer_receivables cr
                             join customer.customers c on cr.customer_id = c.id
                             join customer.customer_details cd on c.last_customer_detail_id = cd.id and cd.id = :customerDetailId
                             left join product_contract.contract_billing_groups cbg
                                       on cr.contract_billing_group_id = cbg.id and cbg.status = 'ACTIVE'
                             join billing.account_periods as accounting_period on accounting_period.id = cr.account_period_id
                    where ((:statuses) is null or text(cr.status) in :statuses)
                      and (coalesce(:initialAmountFrom, '0') = '0' or cr.initial_amount >= :initialAmountFrom)
                      and (coalesce(:initialAmountTo, '0') = '0' or cr.initial_amount <= :initialAmountTo)
                      and (coalesce(:currentAmountFrom, '0') = '0' or cr.current_amount >= :currentAmountFrom)
                      and (coalesce(:currentAmountTo, '0') = '0' or cr.current_amount <= :currentAmountTo)
                      and ((:currencyIds) is null or cr.currency_id in (:currencyIds))
                      and (:blockedForPayment is null or cr.blocked_for_payment = :blockedForPayment)
                      and (coalesce(:billingGroup, '0') = '0' or concat(c.id, cbg.group_number) = :billingGroup)
                      and (:prompt is null or (:searchBy = 'ALL' and (
                        lower(cr.receivable_number) like :prompt or
                        lower(c.identifier) like :prompt or
                        lower(cbg.group_number) like :prompt))
                        or ((:searchBy = 'ID' and lower(cr.receivable_number) like :prompt)
                            or (:searchBy = 'CUSTOMER' and lower(c.identifier) like :prompt)
                            or (:searchBy = 'BILLINGGROUP' and lower(cbg.group_number) like :prompt))
                        )
                    """
    )
    Page<CustomerReceivableMiddleResponse> getCustomerRelatedReceivable(
            @Param("customerDetailId") Long customerDetailId,
            @Param("initialAmountFrom") BigDecimal initialAmountFrom,
            @Param("initialAmountTo") BigDecimal initialAmountTo,
            @Param("currentAmountFrom") BigDecimal currentAmountFrom,
            @Param("currentAmountTo") BigDecimal currentAmountTo,
            @Param("blockedForPayment") Boolean blockedForPayment,
            @Param("prompt") String prompt,
            @Param("searchBy") String searchBy,
            @Param("currencyIds") List<Long> currencyIds,
            @Param("statuses") List<String> statuses,
            @Param("billingGroup") String billingGroup,
            Pageable pageable
    );

    @Query("""
            select new bg.energo.phoenix.model.response.billing.invoice.InvoiceLiabilitiesReceivableResponse(
            r.id,
            r.receivableNumber,
            'RECEIVABLE'
            )
            from CustomerReceivable r
            where r.invoiceId = :id
                and r.status = 'ACTIVE'
            """)
    List<InvoiceLiabilitiesReceivableResponse> findShortResponseByInvoiceId(Long id);

    List<CustomerReceivable> findAllByInvoiceIdIn(List<Long> ids);

    @Query("""
            select pro
            from PaymentReceivableOffsetting pro
            where pro.customerReceivableId = :receivableId
            and pro.status = 'ACTIVE'
            """)
    List<PaymentReceivableOffsetting> findPaymentReceivableOffsetting(
            @Param("receivableId") Long receivableId
    );

    @Query("""
            select clpr
            from CustomerLiabilityPaidByReceivable clpr
            where clpr.customerReceivableId = :receivableId
            and clpr.status = 'ACTIVE'
            """)
    List<CustomerLiabilityPaidByReceivable> findLiabilityPaidByReceivable(
            @Param("receivableId") Long receivableId
    );

    @Query("""
            select new bg.energo.phoenix.model.response.billing.invoice.InvoiceConnectionsShortResponse(
                        cr.id,
                        cr.initialAmount,
                        cr.currentAmount)
                        from CustomerReceivable cr where cr.invoiceId = :invoiceId and cr.status = 'ACTIVE'
            """
    )
    List<InvoiceConnectionsShortResponse> findAllReceivableByInvoiceId(Long invoiceId);

    @Query(nativeQuery = true, value = """
                select mlo.id
                from receivable.mlo_customer_receivables mcr
                         inner join receivable.manual_liabilitie_offsettings mlo
                                    on mlo.id = mcr.manual_liabilitie_offsetting_id
                where mcr.customer_receivable_id = :receivableId
                  and not mlo.reversed
            """
    )
    List<Long> getConnectedMLOId(Long receivableId);

    @Query(nativeQuery = true,
            value = """
                    select crt.id as id , crt.dest_object_id as sourceId
                    from receivable.customer_receivable_transactions crt
                    where crt.dest_object_type = 'LIABILITY'
                      and crt.source_object_id = :receivableId
                      and text(crt.source_object_type) = :sourceObjectType
                      and text(crt.operation_context) = :context
                    """
    )
    List<CustomerLiabilityTransactionsResponse> findTransactionAndDestIdByReceivableId(Long receivableId, String sourceObjectType, String context);

    @Query("""
                    select cr from CustomerReceivable cr
                    where cr.id in :ids
            """)
    List<CustomerReceivable> findReceivablesById(Long[] ids);

    @Query("""
            select cr from CustomerReceivable cr
            where cr.status = 'ACTIVE'
            and cr.outgoingDocumentType = 'PAYMENT'
            and cr.outgoingDocumentFromExternalSystem = :paymentNumber
            """)
    List<CustomerReceivable> findCustomerReceivableCreatedByPayment(
            @Param("paymentNumber") String paymentNumber
    );
}
