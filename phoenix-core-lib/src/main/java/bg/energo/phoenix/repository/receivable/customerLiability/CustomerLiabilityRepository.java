package bg.energo.phoenix.repository.receivable.customerLiability;

import bg.energo.phoenix.model.entity.EntityStatus;
import bg.energo.phoenix.model.entity.billing.invoice.Invoice;
import bg.energo.phoenix.model.entity.receivable.customerLiability.CustomerLiability;
import bg.energo.phoenix.model.response.billing.invoice.InvoiceConnectionsShortResponse;
import bg.energo.phoenix.model.response.billing.invoice.InvoiceLiabilitiesReceivableResponse;
import bg.energo.phoenix.model.response.receivable.customerLiability.CustomerLiabilityAndReceivableListingMiddleResponse;
import bg.energo.phoenix.model.response.receivable.customerLiability.CustomerLiabilityListingMiddleResponse;
import bg.energo.phoenix.model.response.receivable.customerLiability.CustomerLiabilityTransactionsResponse;
import bg.energo.phoenix.model.response.receivable.defaultInterestCalculation.DefaultInterestCalculationPreviewResponse;
import bg.energo.phoenix.model.response.receivable.rescheduling.ReschedulingLiabilityMiddleResponse;
import bg.energo.phoenix.model.response.shared.ShortResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
public interface CustomerLiabilityRepository extends JpaRepository<CustomerLiability, Long> {

    Optional<CustomerLiability> findByLatePaymentFineId(Long latePaymentFineId);

    Optional<CustomerLiability> findByIdAndStatus(Long id, EntityStatus status);

    Optional<CustomerLiability> findByLiabilityNumberAndStatus(String liabilityNumber, EntityStatus status);

    @Query(nativeQuery = true,
            value = """
                    select customerId,
                           liabilityNumber,
                           customer,
                           billingGroup,
                           alternativeRecipientOfAnInvoice,
                           initialAmount,
                           currentAmount,
                           id,
                           currencyId,
                           currencyName,
                           status,
                           creationType,
                           accountingPeriodStatus,
                           occurrenceDate,
                           dueDate,
                           invoiceId,
                           pods
                    from (select c.id                                                                 as customerId,
                                 liability_number                                                     as liabilityNumber,
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
                                         concat(c.identifier, ' (', cd.name, ' ', lf.name, ')')
                                     end                                                              as customer,
                                 cbg.group_number                                                     as billingGroup,
                                 (select case
                                             when c1.customer_type = 'PRIVATE_CUSTOMER' then
                                                 concat(
                                                         c1.identifier, ' (',
                                                         cd1.name,
                                                         case
                                                             when cd1.middle_name is not null then concat(' ', cd1.middle_name)
                                                             else '' end,
                                                         case when cd1.last_name is not null then concat(' ', cd1.last_name) else '' end,
                                                         ')'
                                                 )
                                             when c1.customer_type = 'LEGAL_ENTITY' then
                                                 concat(c1.identifier, ' (', cd1.name, ' ', lf1.name, ')')
                                             end
                                  from customer.customer_details cd1
                                           left join nomenclature.legal_forms lf1 on lf1.id = cd1.legal_form_id
                                           join customer.customers c1 on cd1.id = c1.last_customer_detail_id
                                      and c1.id = cl.alt_invoice_recipient_customer_id
                                      and c1.status = 'ACTIVE')                                       as alternativeRecipientOfAnInvoice,
                                 receivable.convert_to_currency(cl.initial_amount, cl.currency_id, 0) as initialAmount,
                                 receivable.convert_to_currency(cl.current_amount, cl.currency_id, 0) as currentAmount,
                                 cl.id                                                                as id,
                                 cc.id                                                                as currencyId,
                                 cc.name                                                              as currencyName,
                                 cl.status                                                            as status,
                                 cl.creation_type                                                     as creationType,
                                 ap.status                                                            as accountingPeriodStatus,
                                 cl.occurrence_date                                                   as occurrenceDate,
                                 cl.due_date                                                          as dueDate,
                                 cl.invoice_id                                                        as invoiceId,
                                 (select string_agg(distinct p.id::text || ',' || p.identifier, '~') as pod_details
                                  from invoice.invoice_standard_detailed_data isdd
                                           join pod.pod p
                                                on isdd.pod_id = p.id
                                                    and isdd.invoice_id = cl.invoice_id
                                  group by isdd.invoice_id)                                           as pods
                          from receivable.customer_liabilities cl
                                   join customer.customers c on cl.customer_id = c.id
                                   join customer.customer_details cd on c.last_customer_detail_id = cd.id
                                   left join product_contract.contract_billing_groups cbg
                                             on cl.contract_billing_group_id = cbg.id and cbg.status = 'ACTIVE'
                                   join billing.account_periods ap on cl.account_period_id = ap.id
                                   join nomenclature.currencies cc on cl.currency_id = cc.id
                                   left join nomenclature.legal_forms lf on lf.id = cd.legal_form_id
                          where ((:statuses) is null or text(cl.status) in (:statuses))
                            and (date(:dueDateFrom) is null or cl.due_date >= date(:dueDateFrom))
                            and (date(:dueDateTo) is null or cl.due_date <= date(:dueDateTo))
                            and (coalesce(:initialAmountFrom, '0') = '0' or
                                 receivable.convert_to_currency(cl.initial_amount, cl.currency_id, 0) >= :initialAmountFrom)
                            and (coalesce(:initialAmountTo, '0') = '0' or
                                 receivable.convert_to_currency(cl.initial_amount, cl.currency_id, 0) <= :initialAmountTo)
                            and (coalesce(:currentAmountFrom, '0') = '0' or
                                 receivable.convert_to_currency(cl.current_amount, cl.currency_id, 0) >= :currentAmountFrom)
                            and (coalesce(:currentAmountTo, '0') = '0' or
                                 receivable.convert_to_currency(cl.current_amount, cl.currency_id, 0) <= :currentAmountTo)
                            and (date(:occurrenceDateFrom) is null or cl.occurrence_date >= :occurrenceDateFrom)
                            and (date(:occurrenceDateTo) is null or cl.occurrence_date <= :occurrenceDateTo)
                            and (:blockedForPayment is null or cl.blocked_for_payment = :blockedForPayment)
                            and (:blockedForReminderLetters is null or cl.blocked_for_reminder_letters = :blockedForReminderLetters)
                            and (:blockedForCalculationOfInterests is null or
                                 cl.blocked_for_calculation_of_late_payment = :blockedForCalculationOfInterests)
                            and (:blockedForLiabilitiesOffsetting is null or
                                 cl.blocked_for_liabilities_offsetting = :blockedForLiabilitiesOffsetting)
                            and (:blockedForSupplyTermination is null or cl.blocked_for_supply_termination = :blockedForSupplyTermination)
                            and ((:currencyIds) is null or cl.currency_id in (:currencyIds))
                            and (coalesce(:billingGroup, '0') = '0' or concat(c.customer_number, cbg.group_number) = :billingGroup)
                            and (:prompt is null or (:searchBy = 'ALL' and (
                              lower(cl.liability_number) like :prompt
                                  or
                              lower(c.identifier) like :prompt
                                  or
                              lower(cbg.group_number) like :prompt
                              )
                              )
                              or (
                                     (:searchBy = 'ID' and lower(cl.liability_number) like :prompt)
                                         or
                                     (:searchBy = 'CUSTOMER' and lower(c.identifier) like :prompt)
                                         or
                                     (:searchBy = 'BILLINGGROUP' and lower(cbg.group_number) like :prompt)
                                     )
                              )) as subquery
                    """,
            countQuery = """
                        select count(cl.id)
                        from receivable.customer_liabilities cl
                                 join
                             customer.customers c
                             on cl.customer_id = c.id
                                 join customer.customer_details cd
                                      on c.last_customer_detail_id = cd.id
                                 left join product_contract.contract_billing_groups cbg
                                           on cl.contract_billing_group_id = cbg.id
                                               and cbg.status = 'ACTIVE'
                                 join billing.account_periods ap on cl.account_period_id = ap.id
                                 join nomenclature.currencies cc on cl.currency_id = cc.id
                                 left join nomenclature.legal_forms lf on lf.id = cd.legal_form_id
                        where ((:statuses) is null or text(cl.status) in (:statuses))
                          and (date(:dueDateFrom) is null or cl.due_date >= date(:dueDateFrom))
                          and (date(:dueDateTo) is null or cl.due_date <= date(:dueDateTo))
                          and (coalesce(:initialAmountFrom, '0') = '0' or
                               receivable.convert_to_currency(cl.initial_amount, cl.currency_id, 0) >= :initialAmountFrom)
                          and (coalesce(:initialAmountTo, '0') = '0' or
                               receivable.convert_to_currency(cl.initial_amount, cl.currency_id, 0) <= :initialAmountTo)
                          and (coalesce(:currentAmountFrom, '0') = '0' or
                               receivable.convert_to_currency(cl.current_amount, cl.currency_id, 0) >= :currentAmountFrom)
                          and (coalesce(:currentAmountTo, '0') = '0' or
                               receivable.convert_to_currency(cl.current_amount, cl.currency_id, 0) <= :currentAmountTo)
                          and (date(:occurrenceDateFrom) is null or cl.occurrence_date >= :occurrenceDateFrom)
                          and (date(:occurrenceDateTo) is null or cl.occurrence_date <= :occurrenceDateTo)
                          and (:blockedForPayment is null or cl.blocked_for_payment = :blockedForPayment)
                          and (:blockedForReminderLetters is null or cl.blocked_for_reminder_letters = :blockedForReminderLetters)
                          and (:blockedForCalculationOfInterests is null or
                               cl.blocked_for_calculation_of_late_payment = :blockedForCalculationOfInterests)
                          and (:blockedForLiabilitiesOffsetting is null or
                               cl.blocked_for_liabilities_offsetting = :blockedForLiabilitiesOffsetting)
                          and (:blockedForSupplyTermination is null or cl.blocked_for_supply_termination = :blockedForSupplyTermination)
                          and ((:currencyIds) is null or cl.currency_id in (:currencyIds))
                          and (coalesce(:billingGroup, '0') = '0' or concat(c.customer_number, cbg.group_number) = :billingGroup)
                          and (:prompt is null or (:searchBy = 'ALL' and (
                            lower(cl.liability_number) like :prompt
                                or
                            lower(c.identifier) like :prompt
                                or
                            lower(cbg.group_number) like :prompt
                            )
                            )
                            or (
                                   (:searchBy = 'ID' and lower(cl.liability_number) like :prompt)
                                       or
                                   (:searchBy = 'CUSTOMER' and lower(c.identifier) like :prompt)
                                       or
                                   (:searchBy = 'BILLINGGROUP' and lower(cbg.group_number) like :prompt)
                                   )
                            )
                    """
    )
    Page<CustomerLiabilityListingMiddleResponse> filter(
            @Param("searchBy") String searchBy,
            @Param("prompt") String prompt,
            @Param("dueDateFrom") LocalDate dueDateFrom,
            @Param("dueDateTo") LocalDate dueDateTo,
            @Param("initialAmountFrom") BigDecimal initialAmountFrom,
            @Param("initialAmountTo") BigDecimal initialAmountTo,
            @Param("currentAmountFrom") BigDecimal currentAmountFrom,
            @Param("currentAmountTo") BigDecimal currentAmountTo,
            @Param("blockedForPayment") Boolean blockedForPayment,
            @Param("blockedForReminderLetters") Boolean blockedForReminderLetters,
            @Param("blockedForCalculationOfInterests") Boolean blockedForCalculationOfInterests,
            @Param("blockedForLiabilitiesOffsetting") Boolean blockedForLiabilitiesOffsetting,
            @Param("blockedForSupplyTermination") Boolean blockedForSupplyTermination,
            @Param("currencyIds") List<Long> currencyIds,
            @Param("billingGroup") String billingGroup,
            @Param("statuses") List<String> statuses,
            @Param("occurrenceDateFrom") LocalDate occurrenceDateFrom,
            @Param("occurrenceDateTo") LocalDate occurrenceDateTo,
            Pageable pageable
    );

    @Query(nativeQuery = true,
            value = """
                    with euro as (
                        select id from nomenclature.currencies
                                  where name='EUR'
                    ),
                        leva as (
                            select id from nomenclature.currencies
                                      where name = 'лева'
                        )
                    
                    
                    select
                        cl.id as liabilityId,
                        concat(cl.liability_number,'(',cl.outgoing_document_from_external_system,'/',date(cl.create_date),')') as liability,
                        receivable.convert_to_currency(cl.initial_amount,cl.currency_id,euro.id,2) as initialLiabilityAmountInEuro,
                        receivable.convert_to_currency(cl.initial_amount,cl.currency_id,leva.id,2) as initialLiabilityAmountInLeva,
                        cl.due_date as dueDate,
                        receivable.convert_to_currency(cl.current_amount,cl.currency_id,euro.id,2) as currentLiabilityAmountInEuro,
                        receivable.convert_to_currency(cl.current_amount,cl.currency_id,leva.id,2) as currentLiabilityAmountInLeva,
                        cl.applicable_interest_rate_date_from as interestsFromDate,
                        case when cl.outgoing_document_type='INVOICE' then cl.invoice_id
                             when cl.outgoing_document_type='ACTION' then cl.action_id
                             when cl.outgoing_document_type='LATE_PAYMENT_FINE' then cl.late_payment_fine_id
                            end as outgoingDocumentId,
                        cl.outgoing_document_type as outgoingDocumentType,
                        cl.currency_id currencyId,
                        curr.name as currencyName,
                        cl.current_amount as originalCurrentAmount,
                        cl.initial_amount as originalInitialAmount
                    from
                        receivable.customer_liabilities cl
                            left join invoice.invoices inv
                                      on cl.invoice_id = inv.id and inv.document_type = 'PROFORMA_INVOICE'
                            join nomenclature.currencies curr on cl.currency_id = curr.id
                            cross join euro
                            cross join leva
                    where cl.customer_id = :customerId
                      and ((:specificLiabilityId is null and cl.current_amount > 0) or (:specificLiabilityId is not null and cl.id = :specificLiabilityId))
                      and cl.status = 'ACTIVE'
                      and (cl.outgoing_document_type is null or cl.outgoing_document_type not in ('RESCHEDULING', 'DEPOSIT'))
                      and inv.id is null
                    """)
    List<ReschedulingLiabilityMiddleResponse> getReschedulingLiabilitiesByCustomerId(
            @Param("customerId") Long customerId,
            @Param("specificLiabilityId") Long specificLiabilityId
    );

    @Query(value = """
                    with euro as (
                                    select id from nomenclature.currencies
                                              where name='EUR'
                                ),
                                    leva as (
                                        select id from nomenclature.currencies
                                                  where name = 'лева'
                                    )
            
            
                                select
                                    cl.id as liabilityId,
                                    concat(cl.liability_number,'(',cl.outgoing_document_from_external_system,'/',date(cl.create_date),')') as liability,
                                    receivable.convert_to_currency(cl.initial_amount,cl.currency_id,euro.id,2) as initialLiabilityAmountInEuro,
                                    receivable.convert_to_currency(cl.initial_amount,cl.currency_id,leva.id,2) as initialLiabilityAmountInLeva,
                                    cl.due_date as dueDate,
                                    receivable.convert_to_currency(cl.current_amount,cl.currency_id,euro.id,2) as currentLiabilityAmountInEuro,
                                    receivable.convert_to_currency(cl.current_amount,cl.currency_id,leva.id,2) as currentLiabilityAmountInLeva,
                                    cl.applicable_interest_rate_date_from as interestsFromDate,
                                    case when cl.outgoing_document_type='INVOICE' then cl.invoice_id
                                         when cl.outgoing_document_type='ACTION' then cl.action_id
                                         when cl.outgoing_document_type='LATE_PAYMENT_FINE' then cl.late_payment_fine_id
                                        end as outgoingDocumentId,
                                    cl.outgoing_document_type as outgoingDocumentType,
                                    cl.currency_id currencyId,
                                    curr.name as currencyName,
                                    cl.current_amount as originalCurrentAmount,
                                    cl.initial_amount as originalInitialAmount
                                from
                                    receivable.customer_liabilities cl
                                        left join invoice.invoices inv
                                                  on cl.invoice_id = inv.id and inv.document_type = 'PROFORMA_INVOICE'
                                        join nomenclature.currencies curr on cl.currency_id = curr.id
                                        cross join euro
                                        cross join leva
                                where cl.id in (:liabilityIds)
            """, nativeQuery = true)
    List<ReschedulingLiabilityMiddleResponse> getReschedulingLiabilitiesByLiabilityIds(List<Long> liabilityIds);

    @Query(nativeQuery = true,
            value = """
                            select exists (select 1
                                   from receivable.customer_liabilities cl
                                            left join invoice.invoices inv
                                                      on cl.invoice_id = inv.id and inv.document_type = 'PROFORMA_INVOICE'
                                   where (cl.customer_id = :customerId
                                       and cl.status = 'ACTIVE'
                                       and (cl.outgoing_document_type = 'RESCHEDULING' or cl.outgoing_document_type = 'DEPOSIT' or
                                            inv.id is not null)
                                       and cl.id in :specificLiabilityId)
                                      or (cl.customer_id != :customerId and cl.id in :specificLiabilityId))
                    """)
    boolean existsReschedulingLiabilityByCustomerIdAndLiabilityId(
            @Param("customerId") Long customerId,
            @Param("specificLiabilityId") List<Long> specificLiabilityId
    );

    List<CustomerLiability> findAllByInvoiceIdIn(List<Long> invoiceIds);


    @Query("""
            select new bg.energo.phoenix.model.response.billing.invoice.InvoiceConnectionsShortResponse(
                        cl.id,
                        cl.initialAmount,
                        cl.currentAmount)
                        from CustomerLiability cl where cl.invoiceId = :invoiceId and cl.status = 'ACTIVE'
            """
    )
    List<InvoiceConnectionsShortResponse> findAllLiabilityByInvoiceId(Long invoiceId);

    @Query("""
            select new bg.energo.phoenix.model.response.billing.invoice.InvoiceLiabilitiesReceivableResponse(
            l.id,
            l.liabilityNumber,
            'LIABILITY'
            )
            from CustomerLiability l
            where l.invoiceId = :id
                and l.status = 'ACTIVE'
            """)
    List<InvoiceLiabilitiesReceivableResponse> findAllByInvoiceId(@Param("id") Long id);

    boolean existsByActionIdAndStatus(Long id, EntityStatus status);

    @Query("""
            select new bg.energo.phoenix.model.response.shared.ShortResponse(
            l.id,
            l.liabilityNumber
            )
            from CustomerLiability l
            where l.actionId = :actionId
                and l.status = 'ACTIVE'
            """)
    ShortResponse getActionLiability(@Param("actionId") Long id);

   /* @Query(value = """
     select l.* from receivable.customer_liabilities l
     where l.current_amount = 0 and l.late_payment_fine_id is null and l.status = 'ACTIVE'
     """,nativeQuery = true)
    List<CustomerLiability> getLiabilitiesToFine();*/

    @Query(value = """
            with mofb_eval as (select mofb.id mofb_id, eval.liability_id
                               from receivable.mass_operation_for_blocking mofb
                                        inner join lateral receivable.liability_condition_eval(mofb.customer_conditions) eval
                                                   on true
                               where mofb.status != 'DELETED'
                                 and mofb.mass_operation_blocking_status = 'EXECUTED'
                                 and 'CUSTOMER_LIABILITY' = any (mofb."type")
                                 and mofb.blocked_for_calculation_of_late_payment
                                 and mofb.customer_condition_type = 'CUSTOMERS_UNDER_CONDITIONS'),
                 mofb_arr as (select liability_id, array_agg(mofb_id) m_arr
                              from mofb_eval
                              group by liability_id)
            select cl.id,
                   cast(receivable.calculate_lfp_bulk_json(
                           cl.id,
                           :date,
                           mofb_arr.m_arr) as text) lfp
            from receivable.customer_liabilities cl
                     left join mofb_arr on cl.id = mofb_arr.liability_id
                     left join receivable.late_payment_fines lf on cl.child_late_payment_fine_id = lf.id
            where cl.current_amount = 0
              and cl.late_payment_fine_id is null
              and cl.status = 'ACTIVE'
              and cl.due_date < cl.full_offset_date
              and (cl.child_late_payment_fine_id is null or lf.reversed)
            order by 1 desc
            """, nativeQuery = true)
    List<Object[]> getLiabilitiesToFine(LocalDate date);

    @Query(value = """
                    SELECT receivable.calculate_lfp(:id,:date)
            """, nativeQuery = true)
    BigDecimal calculateSingleLatePaymentFine(Long id, LocalDate date);

    @Query(nativeQuery = true,
            value = """
                    with CTE AS(Select distinct cl.id as liabilityId,
                                    cl.customer_id,
                                    c.identifier,
                                    (string_to_array(list_of_customers, ',')) as sta,
                                    receivable.liability_condition_eval_exact(cl.id,cc.customer_conditions) as eval,
                                    cc.customer_condition_type,
                                    cc.list_of_customers,
                                    pdcc.collection_channel_id as pdcc_collection_channel_id,
                                    sdcc.collection_channel_id as sdcc_collection_channel_id
                    from receivable.customer_liabilities cl
                    left join customer.customers c on cl.customer_id = c.id
                    left join invoice.invoices inv on cl.invoice_id =inv.id and inv.status = 'REAL'
                    left join product.product_details prod_d  on prod_d.id =inv.product_detail_id and prod_d.status = 'ACTIVE'
                    left join service.service_details serv_d on serv_d.id=inv.service_detail_id and serv_d.status = 'ACTIVE'
                    left join product.product_details_collection_channels pdcc on pdcc.product_details_id = prod_d.id and pdcc.status = 'ACTIVE'
                    left join service.service_details_collection_channels sdcc on sdcc.service_details_id = serv_d.id and sdcc.status = 'ACTIVE'
                    left join receivable.collection_channels cc on cc.id = pdcc.collection_channel_id or cc.id = sdcc.collection_channel_id and cc.status = 'ACTIVE'
                    left join receivable.collection_channel_exclude_liab_prefixes ccelp on ccelp.collection_channel_id = cc.id and ccelp.status = 'ACTIVE'
                    left join nomenclature.prefixes p on p.id = ccelp.prefix_id and p.status = 'ACTIVE'),
                    
                    exclude as (
                        Select distinct cl.id as excluded_liabilityId
                    from receivable.customer_liabilities cl
                    left join invoice.invoices inv on cl.invoice_id =inv.id and inv.status = 'REAL'
                    left join product.product_details prod_d  on prod_d.id =inv.product_detail_id and prod_d.status = 'ACTIVE'
                    left join service.service_details serv_d on serv_d.id=inv.service_detail_id and serv_d.status = 'ACTIVE'
                    left join product.product_details_collection_channels pdcc on pdcc.product_details_id = prod_d.id and pdcc.status = 'ACTIVE'
                    left join service.service_details_collection_channels sdcc on sdcc.service_details_id = serv_d.id and sdcc.status = 'ACTIVE'
                    left join receivable.collection_channels cc on cc.id = pdcc.collection_channel_id or cc.id = sdcc.collection_channel_id and cc.status = 'ACTIVE'
                    left join receivable.collection_channel_exclude_liab_prefixes ccelp on ccelp.collection_channel_id = cc.id and ccelp.status = 'ACTIVE'
                    left join nomenclature.prefixes p on p.id = ccelp.prefix_id and p.status = 'ACTIVE'
                    where
                        CASE
                            WHEN (exclude_liabilities_by_amount_less_than IS NULL or exclude_liabilities_by_amount_less_than = 0) AND (exclude_liabilities_by_amount_greater_than IS NULL or exclude_liabilities_by_amount_greater_than = 0) THEN TRUE
                            WHEN exclude_liabilities_by_amount_less_than IS NULL THEN cl.current_amount <= exclude_liabilities_by_amount_greater_than
                            WHEN exclude_liabilities_by_amount_greater_than IS NULL THEN cl.current_amount >= exclude_liabilities_by_amount_less_than
                            ELSE cl.current_amount not between exclude_liabilities_by_amount_greater_than and exclude_liabilities_by_amount_less_than END
                    and CASE WHEN inv.invoice_number is not null then substring(inv.invoice_number FROM '^[^-]+') = p.name
                        else 1 = 1 end
                    and (pdcc.collection_channel_id = :collectionChannelId
                        or sdcc.collection_channel_id = :collectionChannelId)
                    and cl.status = 'ACTIVE'
                    and case when cc.file_type = 'BANK_PARTNER' and cl.end_date_of_waiting_payment is not null then cl.end_date_of_waiting_payment < :date else 1=1 end
                    )
                    
                    
                    Select distinct a.liabilityId
                    from CTE a
                    left join exclude b on a.liabilityId = b.excluded_liabilityId
                    where case when customer_condition_type = 'LIST_OF_CUSTOMERS'
                                    then trim(identifier) = ANY(ARRAY(SELECT trim(both ' ' from unnest(string_to_array(list_of_customers, ',')))))
                               else eval > 0
                          end
                    and (pdcc_collection_channel_id = :collectionChannelId
                        or sdcc_collection_channel_id = :collectionChannelId)
                    and b.excluded_liabilityId is null
                    """)
    List<Long> findLiabilitiesByCollectionChannelId(Long collectionChannelId, LocalDate date);

    @Query(nativeQuery = true,
            value = """
                    select c.id as customerId,
                           liability_number as liabilityNumber,
                           case when c.customer_type = 'PRIVATE_CUSTOMER'
                                    then concat(c.identifier,concat(' (',cd.name),case when cd.middle_name is not null then cd.middle_name  end,case when cd.last_name is not null then cd.last_name  end,')' )
                                when c.customer_type = 'LEGAL_ENTITY' then concat(c.identifier,' (',cd.name,')')
                               end customer,
                           cbg.group_number as billingGroup,
                           (select
                                case when c1.customer_type = 'PRIVATE_CUSTOMER'
                                         then concat(c1.identifier,concat(' (',cd1.name),case when cd1.middle_name is not null then cd1.middle_name  end,case when cd1.last_name is not null then cd1.last_name  end,')' )
                                     when c1.customer_type = 'LEGAL_ENTITY' then concat(c1.identifier,' (',cd1.name,')')
                                    end
                            from customer.customer_details cd1
                                     join customer.customers c1
                                          on cd1.id = c1.last_customer_detail_id
                                              and c1.id = cl.alt_invoice_recipient_customer_id
                                              and c1.status = 'ACTIVE') as alternativeRecipientOfAnInvoice,
                           cl.initial_amount as initialAmount,
                           cl.current_amount as currentAmount,
                           cl.id as id,
                           cl.currency_id as currencyId,
                           cl.status as status,
                           cl.creation_type as creationType
                    from
                        receivable.customer_liabilities cl
                            join
                        customer.customers c
                        on cl.customer_id =  c.id
                            join customer.customer_details cd
                                 on c.last_customer_detail_id = cd.id
                                 and cd.id = :customerDetailId
                            left join product_contract.contract_billing_groups cbg
                                      on cl.contract_billing_group_id = cbg.id
                                          and cbg.status = 'ACTIVE'
                    where
                        ((:statuses) is null or text(cl.status) in :statuses)
                      and
                        (date(:dueDateFrom) is null or cl.due_date >= date(:dueDateFrom))
                      and
                        (date(:dueDateTo) is null or cl.due_date <= date(:dueDateTo))
                      and
                        ((:initialAmountFrom) is null or cl.initial_amount >= :initialAmountFrom)
                      and
                        ((:initialAmountTo) is null or cl.initial_amount <= :initialAmountTo)
                      and
                        ((:currentAmountFrom) is null or cl.current_amount >= :currentAmountFrom)
                      and
                        ((:currentAmountTo) is null or cl.current_amount <= :currentAmountTo)
                      and
                        ((:blockedForPayment) is null  or cl.blocked_for_payment = :blockedForPayment)
                      and
                        ((:blockedForReminderLetters) is null  or cl.blocked_for_reminder_letters = :blockedForReminderLetters)
                      and
                        ((:blockedForCalculationOfInterests) is null  or cl.blocked_for_calculation_of_late_payment = :blockedForCalculationOfInterests)
                      and
                        ((:blockedForLiabilitiesOffsetting) is null  or cl.blocked_for_liabilities_offsetting  = :blockedForLiabilitiesOffsetting)
                      and
                        ((:blockedForSupplyTermination) is null  or cl.blocked_for_supply_termination  = :blockedForSupplyTermination)
                      and
                        ((:currencyIds) is null or cl.currency_id in :currencyIds)
                      and
                        ((:billingGroup) is null or concat(c.customer_number,cbg.group_number) = :billingGroup)
                      and
                        (:prompt is null or (:searchBy = 'ALL' and (
                            lower(cl.liability_number) like :prompt
                                or
                            lower(c.identifier) like :prompt
                                or
                            lower(cbg.group_number) like :prompt
                            )
                            )
                            or (
                             (:searchBy = 'ID' and lower(cl.liability_number) like :prompt)
                                 or
                             (:searchBy = 'CUSTOMER' and lower(c.identifier) like :prompt)
                                 or
                             (:searchBy = 'BILLINGGROUP' and lower(cbg.group_number) like :prompt)
                             )
                            )
                    """,
            countQuery = """
                        select count(cl.id)
                    from
                        receivable.customer_liabilities cl
                            join
                        customer.customers c
                        on cl.customer_id =  c.id
                            join customer.customer_details cd
                                 on c.last_customer_detail_id = cd.id
                                 and cd.id = :customerDetailId
                            left join product_contract.contract_billing_groups cbg
                                      on cl.contract_billing_group_id = cbg.id
                                          and cbg.status = 'ACTIVE'
                    where
                        ((:statuses) is null or text(cl.status) in :statuses)
                      and
                        (date(:dueDateFrom) is null or cl.due_date >= date(:dueDateFrom))
                      and
                        (date(:dueDateTo) is null or cl.due_date <= date(:dueDateTo))
                      and
                        ((:initialAmountFrom) is null or cl.initial_amount >= :initialAmountFrom)
                      and
                        ((:initialAmountTo) is null or cl.initial_amount <= :initialAmountTo)
                      and
                        ((:currentAmountFrom) is null or cl.current_amount >= :currentAmountFrom)
                      and
                        ((:currentAmountTo) is null or cl.current_amount <= :currentAmountTo)
                      and
                        ((:blockedForPayment) is null  or cl.blocked_for_payment = :blockedForPayment)
                      and
                        ((:blockedForReminderLetters) is null  or cl.blocked_for_reminder_letters = :blockedForReminderLetters)
                      and
                        ((:blockedForCalculationOfInterests) is null  or cl.blocked_for_calculation_of_late_payment = :blockedForCalculationOfInterests)
                      and
                        ((:blockedForLiabilitiesOffsetting) is null  or cl.blocked_for_liabilities_offsetting  = :blockedForLiabilitiesOffsetting)
                      and
                        ((:blockedForSupplyTermination) is null  or cl.blocked_for_supply_termination  = :blockedForSupplyTermination)
                      and
                        ((:currencyIds) is null or cl.currency_id in :currencyIds)
                      and
                        ((:billingGroup) is null or concat(c.customer_number,cbg.group_number) = :billingGroup)
                      and
                        (:prompt is null or (:searchBy = 'ALL' and (
                            lower(cl.liability_number) like :prompt
                                or
                            lower(c.identifier) like :prompt
                                or
                            lower(cbg.group_number) like :prompt
                            )
                            )
                            or (
                             (:searchBy = 'ID' and lower(cl.liability_number) like :prompt)
                                 or
                             (:searchBy = 'CUSTOMER' and lower(c.identifier) like :prompt)
                                 or
                             (:searchBy = 'BILLINGGROUP' and lower(cbg.group_number) like :prompt)
                             )
                            )
                    """)
    Page<CustomerLiabilityListingMiddleResponse> getCustomerRelatedLiabilities(
            @Param("customerDetailId") Long customerDetailId,
            @Param("searchBy") String searchBy,
            @Param("prompt") String prompt,
            @Param("dueDateFrom") LocalDate dueDateFrom,
            @Param("dueDateTo") LocalDate dueDateTo,
            @Param("initialAmountFrom") BigDecimal initialAmountFrom,
            @Param("initialAmountTo") BigDecimal initialAmountTo,
            @Param("currentAmountFrom") BigDecimal currentAmountFrom,
            @Param("currentAmountTo") BigDecimal currentAmountTo,
            @Param("blockedForPayment") Boolean blockedForPayment,
            @Param("blockedForReminderLetters") Boolean blockedForReminderLetters,
            @Param("blockedForCalculationOfInterests") Boolean blockedForCalculationOfInterests,
            @Param("blockedForLiabilitiesOffsetting") Boolean blockedForLiabilitiesOffsetting,
            @Param("blockedForSupplyTermination") Boolean blockedForSupplyTermination,
            @Param("currencyIds") List<Long> currencyIds,
            @Param("billingGroup") String billingGroup,
            @Param("statuses") List<String> statuses,
            Pageable pageable
    );

    @Query("""
            select new bg.energo.phoenix.model.response.receivable.defaultInterestCalculation.DefaultInterestCalculationPreviewResponse(
                    cl.id,
                    cl.liabilityNumber,
                    case
                        when cl.outgoingDocumentType = 'INVOICE' then concat(i.invoiceNumber, '/', function('to_char', i.createDate, 'YYYY-MM-DD'))
                        when cl.outgoingDocumentType = 'LATE_PAYMENT_FINE' then concat(lpf.latePaymentNumber, '/', function('to_char', lpf.createDate, 'YYYY-MM-DD'))
                        when cl.outgoingDocumentType = 'DEPOSIT' then concat(d.depositNumber, '/', function('to_char', d.createDate, 'YYYY-MM-DD'))
                    end,
                    cl.initialAmount,
                    cl.dueDate,
                    cl.currentAmount,
                    case
                        when cl.outgoingDocumentType = 'INVOICE' then i.id
                        when cl.outgoingDocumentType = 'LATE_PAYMENT_FINE' then lpf.id
                        when cl.outgoingDocumentType = 'DEPOSIT' then d.id
                    end,
                    cl.outgoingDocumentType
                    )
                    from CustomerLiability cl
                    left join Invoice i on cl.invoiceId = i.id and cl.outgoingDocumentType = 'INVOICE'
                    left join LatePaymentFine lpf on cl.latePaymentFineId = lpf.id and cl.outgoingDocumentType = 'LATE_PAYMENT_FINE'
                    left join Deposit d on cl.outgoingDocumentFromExternalSystem = d.depositNumber and cl.outgoingDocumentType = 'DEPOSIT'
                    where cl.customerId = :customerId
                    and cl.currentAmount > 0
                    order by cl.dueDate desc, cl.id asc
            """)
    Page<DefaultInterestCalculationPreviewResponse> getDefaultInterestCalculationPreview(
            @Param("customerId") Long customerId,
            Pageable pageable
    );

    @Query(value = """
                    with   mofb_eval as ( select mofb.id mofb_id, eval.liability_id from receivable.mass_operation_for_blocking mofb
            inner join lateral receivable.liability_condition_eval(mofb.customer_conditions) eval on true
             where mofb.status!='DELETED'
            		and mofb.mass_operation_blocking_status ='EXECUTED' and   'CUSTOMER_LIABILITY'= any(mofb."type" )
            		and mofb.blocked_for_calculation_of_late_payment
            		and mofb.customer_condition_type ='CUSTOMERS_UNDER_CONDITIONS'
            		),
                    mofb_arr as (
            		 	select liability_id ,array_agg(mofb_id)  m_arr
            		from mofb_eval
            		group by liability_id)
            select cl.id,
                   cast(receivable.calculate_lfp_bulk_json(
                   cl.id,
                   :date,
                   mofb_arr.m_arr) as text) lfp
            from receivable.customer_liabilities cl
                left join mofb_arr on cl.id=mofb_arr.liability_id and cl.late_payment_fine_id is null and cl.status = 'ACTIVE'
            where cl.id in :liabilityIds
            order by 1 desc;
            """, nativeQuery = true)
    List<Object[]> calculateDefaultInterest(List<Long> liabilityIds, LocalDate date);

    @Query(value = """
                SELECT receivable.calculate_lfp_bulk_json(
                :liabilityId,
                :date,
                NULL
            ) AS result
            """,
            nativeQuery = true
    )
    String calculateLatePaymentAndInterestRateOnlinePayment(Long liabilityId, LocalDate date);

    @Query(value = """
                SELECT receivable.calculate_lfp_json(
                :liabilityId,
                :date
            ) AS result
            """,
            nativeQuery = true
    )
    String calculateLatePaymentAndInterestRateForOnlinePayment(Long liabilityId, LocalDate date);

    @Query("""
            select inv
            from CustomerLiability cl
            join Invoice inv on inv.id = cl.invoiceId
            """)
    Optional<Invoice> findInvoiceByLiabilityId(@Param("liabilityId") Long liabilityId);

    List<CustomerLiability> findByDepositIdOrderByCreateDateDesc(Long depositId);

    @Query("SELECT COUNT(l) = :count FROM CustomerLiability l JOIN Customer c on l.customerId = c.id WHERE l.id IN (:liabilityIds) AND c.id = :customerId")
    boolean existsAllByLiabilityIdsAndCustomerId(@Param("count") Integer count, @Param("liabilityIds") List<Long> liabilityIds, @Param("customerId") Long customerId);

    @Query(value = """
                    SELECT
                        COUNT(*) > 0
                    FROM (
                             SELECT cld.customer_liabilitie_id
                             FROM receivable.customer_liabilitie_paid_by_deposits cld
                             WHERE cld.customer_liabilitie_id = :liabilityId
            
                             UNION
            
                             SELECT clp.customer_liabilitie_id
                             FROM receivable.customer_liabilitie_paid_by_payments clp
                             WHERE clp.customer_liabilitie_id = :liabilityId
            
                             UNION
            
                             SELECT clr.customer_liabilitie_id
                             FROM receivable.customer_liabilitie_paid_by_receivables clr
                             WHERE clr.customer_liabilitie_id = :liabilityId
            
                             UNION
            
                             SELECT clrr.customer_liabilitie_id
                             FROM receivable.customer_liabilitie_paid_by_rescheduling clrr
                             WHERE clrr.customer_liabilitie_id = :liabilityId
            
                         ) combined;
            """, nativeQuery = true)
    boolean hasParticipatedInOffsetting(@Param("liabilityId") Long liabilityId);

    @Query(value = """
                  SELECT *
                  FROM (SELECT DISTINCT ON (id) id,
                                    id_raw,
                                    outgoing_document,
                                    create_date,
                                    create_date_raw,
                                    due_date,
                                    due_date_raw,
                                    billingGroup,
                                    Contract_Order,
                                    initial_amount,
                                    current_amount,
                                    pods,
                                    address,
                                    offseting,
                                    object,
                                    outgoing_document_type,
                                    outgoing_document_id,
                                    separate_invoice_for_each_pod
            FROM (WITH combined_liabilitie AS (SELECT a.customer_liabilitie_id,
                                                      a.customer_deposit_id,
                                                      a.create_date,
                                                      ROUND(a.amount, 2) AS amount,
                                                      b.print_name,
                                                      'DEPOSIT:'         as outgoing_document_type
                                               FROM receivable.customer_liabilitie_paid_by_deposits a
                                                        LEFT JOIN nomenclature.currencies b ON a.currency_id = b.id
                                               where a.status = 'ACTIVE'
                                               UNION ALL
                                               SELECT a.customer_liabilitie_id,
                                                      a.customer_payment_id,
                                                      a.create_date,
                                                      ROUND(a.amount, 2) AS amount,
                                                      b.print_name,
                                                      'PAYMENT:'
                                               FROM receivable.customer_liabilitie_paid_by_payments a
                                                        LEFT JOIN nomenclature.currencies b ON a.currency_id = b.id
                                               where a.status = 'ACTIVE'
                                               UNION ALL
                                               SELECT a.customer_liabilitie_id,
                                                      a.customer_receivable_id,
                                                      a.create_date,
                                                      ROUND(a.amount, 2) AS amount,
                                                      b.print_name,
                                                      'RECEIVABLE:'
                                               FROM receivable.customer_liabilitie_paid_by_receivables a
                                                        LEFT JOIN nomenclature.currencies b ON a.currency_id = b.id
                                               where a.status = 'ACTIVE'
                                               UNION ALL
                                               SELECT a.customer_liabilitie_id,
                                                      a.customer_rescheduling_id,
                                                      a.create_date,
                                                      ROUND(a.amount, 2) AS amount,
                                                      b.print_name,
                                                      'RESHEDULING:'
                                               FROM receivable.customer_liabilitie_paid_by_rescheduling a
                                                        LEFT JOIN nomenclature.currencies b ON a.currency_id = b.id
                                               where a.status = 'ACTIVE'),
                       cte as (SELECT customer_liabilitie_id,
                                      STRING_AGG((outgoing_document_type || customer_deposit_id || '-' || create_date::date ||
                                                  '-' ||
                                                  amount || '-' ||
                                                  print_name)::TEXT,
                                                 ', ') AS offseting
                               FROM combined_liabilitie
                               GROUP BY customer_liabilitie_id),
                       combined_receivables AS
                           (select a.customer_receivable_id,
                                   customer_payment_id,
                                   a.create_date,
                                   ROUND(a.amount, 2) AS amount,
                                   b.print_name,
                                   'PAYMENT:'         as outgoing_document_type
                            from receivable.customer_payment_receivable_offsettings a
                                     LEFT JOIN nomenclature.currencies b ON a.currency_id = b.id
                            where a.status = 'ACTIVE'
                            UNION ALL
                            select a.customer_receivable_id,
                                   customer_liabilitie_id as customer_receivable_id2,
                                   a.create_date,
                                   ROUND(a.amount, 2)     AS amount,
                                   b.print_name,
                                   'LIABILITY:'
                            from receivable.customer_liabilitie_paid_by_receivables a
                                     LEFT JOIN nomenclature.currencies b ON a.currency_id = b.id
                            where a.status = 'ACTIVE'),
                       cte2 as (SELECT customer_receivable_id,
                                       STRING_AGG(
                                               (outgoing_document_type || customer_payment_id || '-' || create_date::date ||
                                                '-' ||
                                                amount || '-' ||
                                                print_name)::TEXT,
                                               ', ') AS offseting
                                FROM combined_receivables
                                GROUP BY customer_receivable_id),
                       cte3 as (select a.customer_deposit_id,
                                       STRING_AGG(('DEPOSIT:' || customer_deposit_id || '-' || a.create_date::date || '-' ||
                                                   amount ||
                                                   '-' ||
                                                   print_name)::TEXT,
                                                  ', ') AS offseting
                                from receivable.customer_liabilitie_paid_by_deposits a
                                         LEFT JOIN nomenclature.currencies b ON a.currency_id = b.id
                                where a.status = 'ACTIVE'
                                group by customer_deposit_id)
                  Select id,
                         id_raw,
                         outgoing_document,
                         create_date,
                         create_date_raw,
                         due_date,
                         due_date_raw,
                         billingGroup,
                         Contract_Order,
                         initial_amount,
                         current_amount,
                         pods,
                         address,
                         offseting,
                         object,
                         outgoing_document_type,
                         outgoing_document_id,
                         separate_invoice_for_each_pod
                  from (Select a.liability_number                              as                        id
                             , a.id                                            as                        id_raw
                             , j.identifier
                             , g.group_number
                             , a.outgoing_document_from_external_system        as                        outgoing_document
                             , TO_CHAR(a.create_date::timestamp, 'DD-MM-YYYY') AS                        create_date
                             , a.create_date                                   as                        create_date_raw
                             , TO_CHAR(a.due_date::timestamp, 'DD-MM-YYYY')    AS                        due_date
                             , a.due_date                                      AS                        due_date_raw
                             , g.group_number                                  as                        billingGroup
                             , case
                                   when b.goods_order_id is not null then
                                       concat('[{"ID": ', b.goods_order_id, ', "TYPE": "GOODS_ORDER", "NUMBER": "',
                                              k.order_number, '"}]')
                                   when b.service_order_id is not null then
                                       concat('[{"ID": ', b.service_order_id, ', "TYPE": "SERVICE_ORDER", "NUMBER": "',
                                              l.order_number, '"}]')
                                   when b.product_contract_id is not null then
                                       concat('[{"ID": ', b.product_contract_id, ', "TYPE": "PRODUCT_CONTRACT", "NUMBER": "',
                                              m.contract_number, '"}]')
                                   when b.service_contract_id is not null then
                                       concat('[{"ID": ', b.service_contract_id, ', "TYPE": "SERVICE_CONTRACT", "NUMBER": "',
                                              n.contract_number, '"}]')
                                   when a.contract_billing_group_id is not null then
                                       (select concat('[{"ID": ', g.contract_id, ', "TYPE": "PRODUCT_CONTRACT", "NUMBER": "',
                                                      cc.contract_number, '"}]')
                                        from product_contract.contracts cc
                                        where cc.id = g.contract_id)
                                   else null
                          end                                                  as                        Contract_Order
                             , receivable.convert_to_currency(a.initial_amount, a.currency_id, 0)        initial_amount
                             , receivable.convert_to_currency(a.current_amount, a.currency_id, 0)        current_amount
                             , case when separate_invoice_for_each_pod = true then pods else null end    pods
                             , case when separate_invoice_for_each_pod = true then address else null end address
                             , h.offseting
                             , pr_cont_pods.blocked_for_disconnection
                             , 'liability'                                     as                        object
                             , text(a.outgoing_document_type)                  as                        outgoing_document_type
                             , coalesce(a.invoice_id, a.action_id, a.late_payment_fine_id, a.rescheduling_id,
                                        a.deposit_id)                          as                        outgoing_document_id
                             , g.separate_invoice_for_each_pod
                        from receivable.customer_liabilities a
                                 left join customer.customers j on a.customer_id = j.id
                                 left join invoice.invoices b on a.invoice_id = b.id and b.status = 'REAL'
                                 left join goods_order.orders k on k.id = b.goods_order_id
                                 left join service_order.orders l on l.id = b.service_order_id
                                 left join product_contract.contracts m on m.id = b.product_contract_id
                                 left join service_contract.contracts n on n.id = b.service_contract_id
                                 left join cte h on h.customer_liabilitie_id = a.id
                                 left join (Select invoice_id,
                                                   e.blocked_for_disconnection,
                                                   e.identifier  as pods,
                                                   NULLIF(CONCAT_WS(', ',
                                                                    NULLIF(cou.name, ''),
                                                                    NULLIF(NULLIF(rr.name, f.region_foreign), ''),
                                                                    NULLIF(NULLIF(m.name, f.municipality_foreign), ''),
                                                                    NULLIF(NULLIF(pp.name, f.populated_place_foreign), ''),
                                                                    NULLIF(NULLIF(zc.zip_code, f.zip_code_foreign), ''),
                                                                    NULLIF(NULLIF(b.name, f.district_foreign), ''),
                                                                    NULLIF(text(f.foreign_residential_area_type), ''),
                                                                    NULLIF(NULLIF(c.name, f.residential_area_foreign), ''),
                                                                    NULLIF(text(f.foreign_street_type), ''),
                                                                    NULLIF(NULLIF(d.name, f.street_foreign), ''),
                                                                    NULLIF(f.street_number, ''),
                                                                    NULLIF(f.address_additional_info, ''),
                                                                    NULLIF(CASE
                                                                               WHEN f.block IS NOT NULL THEN 'бл. ' || f.block
                                                                               ELSE '' END, ''),
                                                                    NULLIF(CASE
                                                                               WHEN f.entrance IS NOT NULL
                                                                                   THEN 'вх. ' || f.entrance
                                                                               ELSE '' END, ''),
                                                                    NULLIF(CASE
                                                                               WHEN f.floor IS NOT NULL THEN 'ет. ' || f.floor
                                                                               ELSE '' END, ''),
                                                                    NULLIF(CASE
                                                                               WHEN f.apartment IS NOT NULL
                                                                                   THEN 'ап. ' || f.apartment
                                                                               ELSE '' END, ''),
                                                                    NULLIF(f.mailbox, '')
                                                          ), '') AS address
                                            from invoice.invoice_standard_detailed_data a
                                                     left join pod.pod e on a.pod_id = e.id
                                                     left join pod.pod_details f on e.last_pod_detail_id = f.id
                                                     left join nomenclature.districts b on f.district_id = b.id
                                                     left join nomenclature.residential_areas c on c.id = f.residential_area_id
                                                     left join nomenclature.streets d on f.street_id = d.id
                                                     left join nomenclature.countries cou on f.country_id = cou.id
                                                     left join nomenclature.populated_places pp on f.populated_place_id = pp.id
                                                     left join nomenclature.municipalities m on pp.municipality_id = m.id
                                                     left join nomenclature.regions rr on m.region_id = rr.id
                                                     left join nomenclature.zip_codes zc on f.zip_code_id = zc.id) pr_cont_pods
                                           on pr_cont_pods.invoice_id = a.invoice_id
                                 join customer.customers e on a.customer_id = e.id
                            and e.id = :customerId
                                 left join product_contract.contract_billing_groups g
                                           on a.contract_billing_group_id = g.id and g.status = 'ACTIVE'
                        where a.status = 'ACTIVE'
                          and (a.outgoing_document_type <> 'DEPOSIT' or a.outgoing_document_type is null)
                          and case
                                  when :showLiabilitiesAndReceivables = true then a.current_amount >= 0
                                  else a.current_amount > 0 end
                          and (b.document_type is null or b.document_type <> 'PROFORMA_INVOICE')
                          and (coalesce(a.blocked_for_payment, false) = coalesce(:blockedForPayment, false))
                          and (coalesce(a.blocked_for_liabilities_offsetting, false) =
                               coalesce(:blockedForLiabilitiesOffsetting, false))
                          and (coalesce(a.blocked_for_reminder_letters, false) = coalesce(:blockedForReminderLetters, false))
                          and (coalesce(a.blocked_for_calculation_of_late_payment, false) =
                               coalesce(:blockedForCalculationOfLatePayment, false))
                          and (coalesce(a.blocked_for_supply_termination, false) =
                               coalesce(:blockedForDisconnection, false))) a
                  where (:prompt is null or (:searchBy = 'ALL' and (
                      lower(a.id) like :prompt
                          or
                      lower(a.outgoing_document) like :prompt
                          or
                      lower(a.create_date) like :prompt
                          or
                      lower(a.due_date) like :prompt
                          or
                      lower(a.billingGroup) like :prompt
                          or
                      lower(a.pods) like :prompt
                          or
                      lower(a.address) like :prompt
                          or
                      lower(a.Contract_Order) like :prompt
                          or
                      lower(a.offseting) like :prompt
                          or
                      lower(a.object) like :prompt
                          or
                      lower(text(a.initial_amount)) = :prompt
                          or
                      lower(text(a.current_amount)) = :prompt)
                      )
                      or (
                             (:searchBy = 'ID' and lower(a.id) like :prompt)
                                 or
                             (:searchBy = 'OUTGOIGDOCUMENT' and lower(a.outgoing_document) like :prompt)
                                 or
                             (:searchBy = 'CONTRACTORDER' and lower(a.Contract_Order) like :prompt)
                                 or
                             (:searchBy = 'PODIDENTIFIER' and lower(a.pods) like :prompt)
                                 or
                             (:searchBy = 'PODADDRESS' and lower(a.address) like :prompt)))
                  UNION ALL
                  Select id,
                         id_raw,
                         outgoing_document,
                         create_date,
                         create_date_raw,
                         due_date,
                         due_date_raw,
                         billingGroup,
                         Contract_Order,
                         initial_amount,
                         current_amount,
                         case when separate_invoice_for_each_pod = true then pods else null end    pods,
                         case when separate_invoice_for_each_pod = true then address else null end address,
                         offseting,
                         object,
                         text(outgoing_document_type) as                                           outgoing_document_type,
                         outgoing_document_id,
                         separate_invoice_for_each_pod
                  from (Select a.receivable_number                             as                  id
                             , a.id                                            as                  id_raw
                             , j.identifier
                             , g.group_number
                             , a.outgoing_document_from_external_system        as                  outgoing_document
                             , TO_CHAR(a.create_date::timestamp, 'DD-MM-YYYY') AS                  create_date
                             , a.create_date                                   as                  create_date_raw
                             , TO_CHAR(a.due_date::timestamp, 'DD-MM-YYYY')    AS                  due_date
                             , a.due_date                                      AS                  due_date_raw
                             , g.group_number                                  as                  billingGroup
                             , case
                                   when b.goods_order_id is not null then
                                       concat('[{"ID": ', b.goods_order_id, ', "TYPE": "GOODS_ORDER", "NUMBER": "',
                                              k.order_number, '"}]')
                                   when b.service_order_id is not null then
                                       concat('[{"ID": ', b.service_order_id, ', "TYPE": "SERVICE_ORDER", "NUMBER": "',
                                              l.order_number, '"}]')
                                   when b.product_contract_id is not null then
                                       concat('[{"ID": ', b.product_contract_id, ', "TYPE": "PRODUCT_CONTRACT", "NUMBER": "',
                                              m.contract_number, '"}]')
                                   when b.service_contract_id is not null then
                                       concat('[{"ID": ', b.service_contract_id, ', "TYPE": "SERVICE_CONTRACT", "NUMBER": "',
                                              n.contract_number, '"}]')
                                   when a.contract_billing_group_id is not null then
                                       (select concat('[{"ID": ', g.contract_id, ', "TYPE": "PRODUCT_CONTRACT", "NUMBER": "',
                                                      cc.contract_number, '"}]')
                                        from product_contract.contracts cc
                                        where cc.id = g.contract_id)
                                   else null
                          end                                                  as                  Contract_Order
                             , -receivable.convert_to_currency(a.initial_amount, a.currency_id, 0) initial_amount
                             , -receivable.convert_to_currency(a.current_amount, a.currency_id, 0) current_amount
                             , i.pods
                             , i.address
                             , h.offseting
                             , i.blocked_for_disconnection
                             , 'receivable'                                    as                  object
                             , a.outgoing_document_type
                             , coalesce(a.invoice_id,
                                        a.action_id,
                                        a.late_payment_fine_id,
                                        regexp_replace(a.outgoing_document_from_external_system, '\\D', '',
                                                       'g')::bigint)           as                  outgoing_document_id
                             , g.separate_invoice_for_each_pod
                        from receivable.customer_receivables a
                                 left join customer.customers j on a.customer_id = j.id
                                 left join invoice.invoices b on a.invoice_id = b.id and b.status = 'REAL'
                                 left join goods_order.orders k on k.id = b.goods_order_id
                                 left join service_order.orders l on l.id = b.service_order_id
                                 left join product_contract.contracts m on m.id = b.product_contract_id
                                 left join service_contract.contracts n on n.id = b.service_contract_id
                                 left join cte2 h on h.customer_receivable_id = a.id
                                 left join (Select invoice_id,
                                                   e.blocked_for_disconnection,
                                                   e.identifier  as pods,
                                                   NULLIF(CONCAT_WS(', ',
                                                                    NULLIF(cou.name, ''),
                                                                    NULLIF(NULLIF(rr.name, f.region_foreign), ''),
                                                                    NULLIF(NULLIF(m.name, f.municipality_foreign), ''),
                                                                    NULLIF(NULLIF(pp.name, f.populated_place_foreign), ''),
                                                                    NULLIF(NULLIF(zc.zip_code, f.zip_code_foreign), ''),
                                                                    NULLIF(NULLIF(b.name, f.district_foreign), ''),
                                                                    NULLIF(text(f.foreign_residential_area_type), ''),
                                                                    NULLIF(NULLIF(c.name, f.residential_area_foreign), ''),
                                                                    NULLIF(text(f.foreign_street_type), ''),
                                                                    NULLIF(NULLIF(d.name, f.street_foreign), ''),
                                                                    NULLIF(f.street_number, ''),
                                                                    NULLIF(f.address_additional_info, ''),
                                                                    NULLIF(CASE
                                                                               WHEN f.block IS NOT NULL THEN 'бл. ' || f.block
                                                                               ELSE '' END, ''),
                                                                    NULLIF(CASE
                                                                               WHEN f.entrance IS NOT NULL
                                                                                   THEN 'вх. ' || f.entrance
                                                                               ELSE '' END, ''),
                                                                    NULLIF(CASE
                                                                               WHEN f.floor IS NOT NULL THEN 'ет. ' || f.floor
                                                                               ELSE '' END, ''),
                                                                    NULLIF(CASE
                                                                               WHEN f.apartment IS NOT NULL
                                                                                   THEN 'ап. ' || f.apartment
                                                                               ELSE '' END, ''),
                                                                    NULLIF(f.mailbox, '')
                                                          ), '') AS address
                                            from invoice.invoice_standard_detailed_data a
                                                     left join pod.pod e on a.pod_id = e.id
                                                     left join pod.pod_details f on e.last_pod_detail_id = f.id
                                                     left join nomenclature.districts b on f.district_id = b.id
                                                     left join nomenclature.residential_areas c on c.id = f.residential_area_id
                                                     left join nomenclature.streets d on f.street_id = d.id
                                                     left join nomenclature.countries cou on f.country_id = cou.id
                                                     left join nomenclature.populated_places pp on f.populated_place_id = pp.id
                                                     left join nomenclature.municipalities m on pp.municipality_id = m.id
                                                     left join nomenclature.regions rr on m.region_id = rr.id
                                                     left join nomenclature.zip_codes zc on f.zip_code_id = zc.id) i
                                           on i.invoice_id = a.invoice_id
                                 join customer.customers e on a.customer_id = e.id
                            and e.id = :customerId
                                 left join product_contract.contract_billing_groups g
                                           on a.contract_billing_group_id = g.id and g.status = 'ACTIVE'
                        where a.status = 'ACTIVE'
                          and case
                                  when :showLiabilitiesAndReceivables = true then a.current_amount >= 0
                                  else a.current_amount > 0 end
                          and (b.document_type is null or b.document_type <> 'PROFORMA_INVOICE')
                          and (a.outgoing_document_type <> 'DEPOSIT' or a.outgoing_document_type is null)
                          and (coalesce(a.blocked_for_payment, false) = coalesce(:blockedForLiabilitiesOffsetting, false))
                          and (false = coalesce(:blockedForPayment, false))
                          and (false = coalesce(:blockedForReminderLetters, false))
                          and (false = coalesce(:blockedForCalculationOfLatePayment, false))
                          and (false = coalesce(:blockedForDisconnection, false))) a
                  where (:prompt is null or (:searchBy = 'ALL' and (
                      lower(a.id) like :prompt
                          or
                      lower(a.outgoing_document) like :prompt
                          or
                      lower(a.create_date) like :prompt
                          or
                      lower(a.due_date) like :prompt
                          or
                      lower(a.billingGroup) like :prompt
                          or
                      lower(a.pods) like :prompt
                          or
                      lower(a.address) like :prompt
                          or
                      lower(a.Contract_Order) like :prompt
                          or
                      lower(a.offseting) like :prompt
                          or
                      lower(a.object) like :prompt
                          or
                      lower(text(a.initial_amount)) = :prompt
                          or
                      lower(text(a.current_amount)) = :prompt
                      )
                      )
                      or (
                             (:searchBy = 'ID' and lower(a.id) like :prompt)
                                 or
                             (:searchBy = 'OUTGOIGDOCUMENT' and lower(a.outgoing_document) like :prompt)
                                 or
                             (:searchBy = 'CONTRACTORDER' and lower(a.Contract_Order) like :prompt)
                                 or
                             (:searchBy = 'PODIDENTIFIER' and lower(a.pods) like :prompt)
                                 or
                             (:searchBy = 'PODADDRESS' and lower(a.address) like :prompt)
                             )
                            )
                  UNION ALL
                  Select id,
                         id_raw,
                         outgoing_document,
                         create_date,
                         create_date_raw,
                         due_date,
                         due_date_raw,
                         billingGroup,
                         Contract_Order,
                         initial_amount,
                         current_amount,
                         null as pods,
                         null as address,
                         a.offseting,
                         object,
                         null,
                         null,
                         null
                  from (Select a.deposit_number                                          as        id
                             , a.id                                                      as        id_raw
                             , j.identifier
                             , null                                                      as        outgoing_document
                             , null                                                      AS        create_date
                             , date(null)                                                as        create_date_raw
                             , null                                                      AS        due_date
                             , date(null)                                                AS        due_date_raw
                             , null                                                      as        billingGroup
                             , (SELECT jsonb_agg(jsonb_build_object(
                                                         'NUMBER', contract_number,
                                                         'ID', contract_id,
                                                         'TYPE', contract_type
                                                 ) ORDER BY contract_number)::TEXT
                                FROM (SELECT pc.contract_number, k.contract_id, 'PRODUCT_CONTRACT' AS contract_type
                                      FROM receivable.customer_deposit_product_contracts k
                                               JOIN product_contract.contracts pc ON pc.id = k.contract_id
                                      WHERE k.customer_deposit_id = a.id
                                      UNION ALL
                                      SELECT sc.contract_number, ks.contract_id, 'SERVICE_CONTRACT'
                                      FROM receivable.customer_deposit_service_contracts ks
                                               JOIN service_contract.contracts sc ON sc.id = ks.contract_id
                                      WHERE ks.customer_deposit_id = a.id
                                      UNION ALL
                                      SELECT ord.order_number, so.order_id, 'SERVICE_ORDER'
                                      FROM receivable.customer_deposit_service_orders so
                                               JOIN service_order.orders ord ON ord.id = so.order_id
                                      WHERE so.customer_deposit_id = a.id
                                      UNION ALL
                                      SELECT ord.order_number, g.order_id, 'GOODS_ORDER'
                                      FROM receivable.customer_deposit_goods_orders g
                                               JOIN goods_order.orders ord ON ord.id = g.order_id
                                      WHERE g.customer_deposit_id = a.id) all_contracts) as        Contract_Order
                             , -receivable.convert_to_currency(a.initial_amount, a.currency_id, 0) initial_amount
                             , -receivable.convert_to_currency(a.current_amount, a.currency_id, 0) current_amount
                             , null
                             , null
                             , l.offseting
                             , 'deposit'                                                 as        object
                             , null                                                      as        outgoing_document_number
                        from receivable.customer_deposits a
                                 left join customer.customers j on a.customer_id = j.id
                                 join customer.customers e on a.customer_id = e.id
                            and e.id = :customerId
                                 left join cte3 l on l.customer_deposit_id = a.id
                        where a.status = 'ACTIVE'
                          and ((:showDeposits) is null or :showDeposits = true)) a
                  where (:prompt is null or (:searchBy = 'ALL' and (
                      lower(a.id) like :prompt
                          or
                      lower(a.outgoing_document) like :prompt
                          or
                      lower(a.create_date) like :prompt
                          or
                      lower(a.due_date) like :prompt
                          or
                      lower(a.billingGroup) like :prompt
                          or
                      lower(a.Contract_Order) like :prompt
                          or
                      lower(a.offseting) like :prompt
                          or
                      lower(a.object) like :prompt
                          or
                      lower(text(a.initial_amount)) = :prompt
                          or
                      lower(text(a.current_amount)) = :prompt
                      )
                      )
                      or (
                             (:searchBy = 'ID' and lower(a.id) like :prompt)
                                 or
                             (:searchBy = 'OUTGOIGDOCUMENT' and lower(a.outgoing_document) like :prompt)
                                 or
                             (:searchBy = 'CONTRACTORDER' and lower(a.Contract_Order) like :prompt)
                             )
                            )) a) sorted_distinct_rows
            """,
            countQuery = """
                                  SELECT count(*)
                                  FROM (SELECT DISTINCT ON (id) id
                    FROM (WITH combined_liabilitie AS (SELECT a.customer_liabilitie_id,
                                                              a.customer_deposit_id,
                                                              a.create_date,
                                                              ROUND(a.amount, 2) AS amount,
                                                              b.print_name,
                                                              'DEPOSIT:'         as outgoing_document_type
                                                       FROM receivable.customer_liabilitie_paid_by_deposits a
                                                                LEFT JOIN nomenclature.currencies b ON a.currency_id = b.id
                                                       where a.status = 'ACTIVE'
                                                       UNION ALL
                                                       SELECT a.customer_liabilitie_id,
                                                              a.customer_payment_id,
                                                              a.create_date,
                                                              ROUND(a.amount, 2) AS amount,
                                                              b.print_name,
                                                              'PAYMENT:'
                                                       FROM receivable.customer_liabilitie_paid_by_payments a
                                                                LEFT JOIN nomenclature.currencies b ON a.currency_id = b.id
                                                       where a.status = 'ACTIVE'
                                                       UNION ALL
                                                       SELECT a.customer_liabilitie_id,
                                                              a.customer_receivable_id,
                                                              a.create_date,
                                                              ROUND(a.amount, 2) AS amount,
                                                              b.print_name,
                                                              'RECEIVABLE:'
                                                       FROM receivable.customer_liabilitie_paid_by_receivables a
                                                                LEFT JOIN nomenclature.currencies b ON a.currency_id = b.id
                                                       where a.status = 'ACTIVE'
                                                       UNION ALL
                                                       SELECT a.customer_liabilitie_id,
                                                              a.customer_rescheduling_id,
                                                              a.create_date,
                                                              ROUND(a.amount, 2) AS amount,
                                                              b.print_name,
                                                              'RESHEDULING:'
                                                       FROM receivable.customer_liabilitie_paid_by_rescheduling a
                                                                LEFT JOIN nomenclature.currencies b ON a.currency_id = b.id
                                                       where a.status = 'ACTIVE'),
                               cte as (SELECT customer_liabilitie_id,
                                              STRING_AGG((outgoing_document_type || customer_deposit_id || '-' || create_date::date ||
                                                          '-' ||
                                                          amount || '-' ||
                                                          print_name)::TEXT,
                                                         ', ') AS offseting
                                       FROM combined_liabilitie
                                       GROUP BY customer_liabilitie_id),
                               combined_receivables AS
                                   (select a.customer_receivable_id,
                                           customer_payment_id,
                                           a.create_date,
                                           ROUND(a.amount, 2) AS amount,
                                           b.print_name,
                                           'PAYMENT:'         as outgoing_document_type
                                    from receivable.customer_payment_receivable_offsettings a
                                             LEFT JOIN nomenclature.currencies b ON a.currency_id = b.id
                                    where a.status = 'ACTIVE'
                                    UNION ALL
                                    select a.customer_receivable_id,
                                           customer_liabilitie_id as customer_receivable_id2,
                                           a.create_date,
                                           ROUND(a.amount, 2)     AS amount,
                                           b.print_name,
                                           'LIABILITY:'
                                    from receivable.customer_liabilitie_paid_by_receivables a
                                             LEFT JOIN nomenclature.currencies b ON a.currency_id = b.id
                                    where a.status = 'ACTIVE'),
                               cte2 as (SELECT customer_receivable_id,
                                               STRING_AGG(
                                                       (outgoing_document_type || customer_payment_id || '-' || create_date::date ||
                                                        '-' ||
                                                        amount || '-' ||
                                                        print_name)::TEXT,
                                                       ', ') AS offseting
                                        FROM combined_receivables
                                        GROUP BY customer_receivable_id),
                               cte3 as (select a.customer_deposit_id,
                                               STRING_AGG(('DEPOSIT:' || customer_deposit_id || '-' || a.create_date::date || '-' ||
                                                           amount ||
                                                           '-' ||
                                                           print_name)::TEXT,
                                                          ', ') AS offseting
                                        from receivable.customer_liabilitie_paid_by_deposits a
                                                 LEFT JOIN nomenclature.currencies b ON a.currency_id = b.id
                                        where a.status = 'ACTIVE'
                                        group by customer_deposit_id)
                          Select id,
                                 id_raw,
                                 outgoing_document,
                                 create_date,
                                 create_date_raw,
                                 due_date,
                                 due_date_raw,
                                 billingGroup,
                                 Contract_Order,
                                 initial_amount,
                                 current_amount,
                                 pods,
                                 address,
                                 offseting,
                                 object,
                                 outgoing_document_type,
                                 outgoing_document_id,
                                 separate_invoice_for_each_pod
                          from (Select a.liability_number                              as                        id
                                     , a.id                                            as                        id_raw
                                     , j.identifier
                                     , g.group_number
                                     , a.outgoing_document_from_external_system        as                        outgoing_document
                                     , TO_CHAR(a.create_date::timestamp, 'DD-MM-YYYY') AS                        create_date
                                     , a.create_date                                   as                        create_date_raw
                                     , TO_CHAR(a.due_date::timestamp, 'DD-MM-YYYY')    AS                        due_date
                                     , a.due_date                                      AS                        due_date_raw
                                     , g.group_number                                  as                        billingGroup
                                     , case
                                           when b.goods_order_id is not null then
                                               concat('[{"ID": ', b.goods_order_id, ', "TYPE": "GOODS_ORDER", "NUMBER": "',
                                                      k.order_number, '"}]')
                                           when b.service_order_id is not null then
                                               concat('[{"ID": ', b.service_order_id, ', "TYPE": "SERVICE_ORDER", "NUMBER": "',
                                                      l.order_number, '"}]')
                                           when b.product_contract_id is not null then
                                               concat('[{"ID": ', b.product_contract_id, ', "TYPE": "PRODUCT_CONTRACT", "NUMBER": "',
                                                      m.contract_number, '"}]')
                                           when b.service_contract_id is not null then
                                               concat('[{"ID": ', b.service_contract_id, ', "TYPE": "SERVICE_CONTRACT", "NUMBER": "',
                                                      n.contract_number, '"}]')
                                           when a.contract_billing_group_id is not null then
                                               (select concat('[{"ID": ', g.contract_id, ', "TYPE": "PRODUCT_CONTRACT", "NUMBER": "',
                                                              cc.contract_number, '"}]')
                                                from product_contract.contracts cc
                                                where cc.id = g.contract_id)
                                           else null
                                  end                                                  as                        Contract_Order
                                     , receivable.convert_to_currency(a.initial_amount, a.currency_id, 0)        initial_amount
                                     , receivable.convert_to_currency(a.current_amount, a.currency_id, 0)        current_amount
                                     , case when separate_invoice_for_each_pod = true then pods else null end    pods
                                     , case when separate_invoice_for_each_pod = true then address else null end address
                                     , h.offseting
                                     , pr_cont_pods.blocked_for_disconnection
                                     , 'liability'                                     as                        object
                                     , text(a.outgoing_document_type)                  as                        outgoing_document_type
                                     , coalesce(a.invoice_id, a.action_id, a.late_payment_fine_id, a.rescheduling_id,
                                                a.deposit_id)                          as                        outgoing_document_id
                                     , g.separate_invoice_for_each_pod
                                from receivable.customer_liabilities a
                                         left join customer.customers j on a.customer_id = j.id
                                         left join invoice.invoices b on a.invoice_id = b.id and b.status = 'REAL'
                                         left join goods_order.orders k on k.id = b.goods_order_id
                                         left join service_order.orders l on l.id = b.service_order_id
                                         left join product_contract.contracts m on m.id = b.product_contract_id
                                         left join service_contract.contracts n on n.id = b.service_contract_id
                                         left join cte h on h.customer_liabilitie_id = a.id
                                         left join (Select invoice_id,
                                                           e.blocked_for_disconnection,
                                                           e.identifier  as pods,
                                                           NULLIF(CONCAT_WS(', ',
                                                                            NULLIF(cou.name, ''),
                                                                            NULLIF(NULLIF(rr.name, f.region_foreign), ''),
                                                                            NULLIF(NULLIF(m.name, f.municipality_foreign), ''),
                                                                            NULLIF(NULLIF(pp.name, f.populated_place_foreign), ''),
                                                                            NULLIF(NULLIF(zc.zip_code, f.zip_code_foreign), ''),
                                                                            NULLIF(NULLIF(b.name, f.district_foreign), ''),
                                                                            NULLIF(text(f.foreign_residential_area_type), ''),
                                                                            NULLIF(NULLIF(c.name, f.residential_area_foreign), ''),
                                                                            NULLIF(text(f.foreign_street_type), ''),
                                                                            NULLIF(NULLIF(d.name, f.street_foreign), ''),
                                                                            NULLIF(f.street_number, ''),
                                                                            NULLIF(f.address_additional_info, ''),
                                                                            NULLIF(CASE
                                                                                       WHEN f.block IS NOT NULL THEN 'бл. ' || f.block
                                                                                       ELSE '' END, ''),
                                                                            NULLIF(CASE
                                                                                       WHEN f.entrance IS NOT NULL
                                                                                           THEN 'вх. ' || f.entrance
                                                                                       ELSE '' END, ''),
                                                                            NULLIF(CASE
                                                                                       WHEN f.floor IS NOT NULL THEN 'ет. ' || f.floor
                                                                                       ELSE '' END, ''),
                                                                            NULLIF(CASE
                                                                                       WHEN f.apartment IS NOT NULL
                                                                                           THEN 'ап. ' || f.apartment
                                                                                       ELSE '' END, ''),
                                                                            NULLIF(f.mailbox, '')
                                                                  ), '') AS address
                                                    from invoice.invoice_standard_detailed_data a
                                                             left join pod.pod e on a.pod_id = e.id
                                                             left join pod.pod_details f on e.last_pod_detail_id = f.id
                                                             left join nomenclature.districts b on f.district_id = b.id
                                                             left join nomenclature.residential_areas c on c.id = f.residential_area_id
                                                             left join nomenclature.streets d on f.street_id = d.id
                                                             left join nomenclature.countries cou on f.country_id = cou.id
                                                             left join nomenclature.populated_places pp on f.populated_place_id = pp.id
                                                             left join nomenclature.municipalities m on pp.municipality_id = m.id
                                                             left join nomenclature.regions rr on m.region_id = rr.id
                                                             left join nomenclature.zip_codes zc on f.zip_code_id = zc.id) pr_cont_pods
                                                   on pr_cont_pods.invoice_id = a.invoice_id
                                         join customer.customers e on a.customer_id = e.id
                                    and e.id = :customerId
                                         left join product_contract.contract_billing_groups g
                                                   on a.contract_billing_group_id = g.id and g.status = 'ACTIVE'
                                where a.status = 'ACTIVE'
                                  and (a.outgoing_document_type <> 'DEPOSIT' or a.outgoing_document_type is null)
                                  and case
                                          when :showLiabilitiesAndReceivables = true then a.current_amount >= 0
                                          else a.current_amount > 0 end
                                  and (b.document_type is null or b.document_type <> 'PROFORMA_INVOICE')
                                  and (coalesce(a.blocked_for_payment, false) = coalesce(:blockedForPayment, false))
                                  and (coalesce(a.blocked_for_liabilities_offsetting, false) =
                                       coalesce(:blockedForLiabilitiesOffsetting, false))
                                  and (coalesce(a.blocked_for_reminder_letters, false) = coalesce(:blockedForReminderLetters, false))
                                  and (coalesce(a.blocked_for_calculation_of_late_payment, false) =
                                       coalesce(:blockedForCalculationOfLatePayment, false))
                                  and (coalesce(a.blocked_for_supply_termination, false) =
                                       coalesce(:blockedForDisconnection, false))) a
                          where (:prompt is null or (:searchBy = 'ALL' and (
                              lower(a.id) like :prompt
                                  or
                              lower(a.outgoing_document) like :prompt
                                  or
                              lower(a.create_date) like :prompt
                                  or
                              lower(a.due_date) like :prompt
                                  or
                              lower(a.billingGroup) like :prompt
                                  or
                              lower(a.pods) like :prompt
                                  or
                              lower(a.address) like :prompt
                                  or
                              lower(a.Contract_Order) like :prompt
                                  or
                              lower(a.offseting) like :prompt
                                  or
                              lower(a.object) like :prompt
                                  or
                              lower(text(a.initial_amount)) = :prompt
                                  or
                              lower(text(a.current_amount)) = :prompt)
                              )
                              or (
                                     (:searchBy = 'ID' and lower(a.id) like :prompt)
                                         or
                                     (:searchBy = 'OUTGOIGDOCUMENT' and lower(a.outgoing_document) like :prompt)
                                         or
                                     (:searchBy = 'CONTRACTORDER' and lower(a.Contract_Order) like :prompt)
                                         or
                                     (:searchBy = 'PODIDENTIFIER' and lower(a.pods) like :prompt)
                                         or
                                     (:searchBy = 'PODADDRESS' and lower(a.address) like :prompt)))
                          UNION ALL
                          Select id,
                                 id_raw,
                                 outgoing_document,
                                 create_date,
                                 create_date_raw,
                                 due_date,
                                 due_date_raw,
                                 billingGroup,
                                 Contract_Order,
                                 initial_amount,
                                 current_amount,
                                 case when separate_invoice_for_each_pod = true then pods else null end    pods,
                                 case when separate_invoice_for_each_pod = true then address else null end address,
                                 offseting,
                                 object,
                                 text(outgoing_document_type) as                                           outgoing_document_type,
                                 outgoing_document_id,
                                 separate_invoice_for_each_pod
                          from (Select a.receivable_number                             as                  id
                                     , a.id                                            as                  id_raw
                                     , j.identifier
                                     , g.group_number
                                     , a.outgoing_document_from_external_system        as                  outgoing_document
                                     , TO_CHAR(a.create_date::timestamp, 'DD-MM-YYYY') AS                  create_date
                                     , a.create_date                                   as                  create_date_raw
                                     , TO_CHAR(a.due_date::timestamp, 'DD-MM-YYYY')    AS                  due_date
                                     , a.due_date                                      AS                  due_date_raw
                                     , g.group_number                                  as                  billingGroup
                                     , case
                                           when b.goods_order_id is not null then
                                               concat('[{"ID": ', b.goods_order_id, ', "TYPE": "GOODS_ORDER", "NUMBER": "',
                                                      k.order_number, '"}]')
                                           when b.service_order_id is not null then
                                               concat('[{"ID": ', b.service_order_id, ', "TYPE": "SERVICE_ORDER", "NUMBER": "',
                                                      l.order_number, '"}]')
                                           when b.product_contract_id is not null then
                                               concat('[{"ID": ', b.product_contract_id, ', "TYPE": "PRODUCT_CONTRACT", "NUMBER": "',
                                                      m.contract_number, '"}]')
                                           when b.service_contract_id is not null then
                                               concat('[{"ID": ', b.service_contract_id, ', "TYPE": "SERVICE_CONTRACT", "NUMBER": "',
                                                      n.contract_number, '"}]')
                                           when a.contract_billing_group_id is not null then
                                               (select concat('[{"ID": ', g.contract_id, ', "TYPE": "PRODUCT_CONTRACT", "NUMBER": "',
                                                              cc.contract_number, '"}]')
                                                from product_contract.contracts cc
                                                where cc.id = g.contract_id)
                                           else null
                                  end                                                  as                  Contract_Order
                                     , -receivable.convert_to_currency(a.initial_amount, a.currency_id, 0) initial_amount
                                     , -receivable.convert_to_currency(a.current_amount, a.currency_id, 0) current_amount
                                     , i.pods
                                     , i.address
                                     , h.offseting
                                     , i.blocked_for_disconnection
                                     , 'receivable'                                    as                  object
                                     , a.outgoing_document_type
                                     , coalesce(a.invoice_id,
                                                a.action_id,
                                                a.late_payment_fine_id,
                                                regexp_replace(a.outgoing_document_from_external_system, '\\D', '',
                                                               'g')::bigint)           as                  outgoing_document_id
                                     , g.separate_invoice_for_each_pod
                                from receivable.customer_receivables a
                                         left join customer.customers j on a.customer_id = j.id
                                         left join invoice.invoices b on a.invoice_id = b.id and b.status = 'REAL'
                                         left join goods_order.orders k on k.id = b.goods_order_id
                                         left join service_order.orders l on l.id = b.service_order_id
                                         left join product_contract.contracts m on m.id = b.product_contract_id
                                         left join service_contract.contracts n on n.id = b.service_contract_id
                                         left join cte2 h on h.customer_receivable_id = a.id
                                         left join (Select invoice_id,
                                                           e.blocked_for_disconnection,
                                                           e.identifier  as pods,
                                                           NULLIF(CONCAT_WS(', ',
                                                                            NULLIF(cou.name, ''),
                                                                            NULLIF(NULLIF(rr.name, f.region_foreign), ''),
                                                                            NULLIF(NULLIF(m.name, f.municipality_foreign), ''),
                                                                            NULLIF(NULLIF(pp.name, f.populated_place_foreign), ''),
                                                                            NULLIF(NULLIF(zc.zip_code, f.zip_code_foreign), ''),
                                                                            NULLIF(NULLIF(b.name, f.district_foreign), ''),
                                                                            NULLIF(text(f.foreign_residential_area_type), ''),
                                                                            NULLIF(NULLIF(c.name, f.residential_area_foreign), ''),
                                                                            NULLIF(text(f.foreign_street_type), ''),
                                                                            NULLIF(NULLIF(d.name, f.street_foreign), ''),
                                                                            NULLIF(f.street_number, ''),
                                                                            NULLIF(f.address_additional_info, ''),
                                                                            NULLIF(CASE
                                                                                       WHEN f.block IS NOT NULL THEN 'бл. ' || f.block
                                                                                       ELSE '' END, ''),
                                                                            NULLIF(CASE
                                                                                       WHEN f.entrance IS NOT NULL
                                                                                           THEN 'вх. ' || f.entrance
                                                                                       ELSE '' END, ''),
                                                                            NULLIF(CASE
                                                                                       WHEN f.floor IS NOT NULL THEN 'ет. ' || f.floor
                                                                                       ELSE '' END, ''),
                                                                            NULLIF(CASE
                                                                                       WHEN f.apartment IS NOT NULL
                                                                                           THEN 'ап. ' || f.apartment
                                                                                       ELSE '' END, ''),
                                                                            NULLIF(f.mailbox, '')
                                                                  ), '') AS address
                                                    from invoice.invoice_standard_detailed_data a
                                                             left join pod.pod e on a.pod_id = e.id
                                                             left join pod.pod_details f on e.last_pod_detail_id = f.id
                                                             left join nomenclature.districts b on f.district_id = b.id
                                                             left join nomenclature.residential_areas c on c.id = f.residential_area_id
                                                             left join nomenclature.streets d on f.street_id = d.id
                                                             left join nomenclature.countries cou on f.country_id = cou.id
                                                             left join nomenclature.populated_places pp on f.populated_place_id = pp.id
                                                             left join nomenclature.municipalities m on pp.municipality_id = m.id
                                                             left join nomenclature.regions rr on m.region_id = rr.id
                                                             left join nomenclature.zip_codes zc on f.zip_code_id = zc.id) i
                                                   on i.invoice_id = a.invoice_id
                                         join customer.customers e on a.customer_id = e.id
                                    and e.id = :customerId
                                         left join product_contract.contract_billing_groups g
                                                   on a.contract_billing_group_id = g.id and g.status = 'ACTIVE'
                                where a.status = 'ACTIVE'
                                  and case
                                          when :showLiabilitiesAndReceivables = true then a.current_amount >= 0
                                          else a.current_amount > 0 end
                                  and (b.document_type is null or b.document_type <> 'PROFORMA_INVOICE')
                                  and (a.outgoing_document_type <> 'DEPOSIT' or a.outgoing_document_type is null)
                                  and (coalesce(a.blocked_for_payment, false) = coalesce(:blockedForLiabilitiesOffsetting, false))
                                  and (false = coalesce(:blockedForPayment, false))
                                  and (false = coalesce(:blockedForReminderLetters, false))
                                  and (false = coalesce(:blockedForCalculationOfLatePayment, false))
                                  and (false = coalesce(:blockedForDisconnection, false))) a
                          where (:prompt is null or (:searchBy = 'ALL' and (
                              lower(a.id) like :prompt
                                  or
                              lower(a.outgoing_document) like :prompt
                                  or
                              lower(a.create_date) like :prompt
                                  or
                              lower(a.due_date) like :prompt
                                  or
                              lower(a.billingGroup) like :prompt
                                  or
                              lower(a.pods) like :prompt
                                  or
                              lower(a.address) like :prompt
                                  or
                              lower(a.Contract_Order) like :prompt
                                  or
                              lower(a.offseting) like :prompt
                                  or
                              lower(a.object) like :prompt
                                  or
                              lower(text(a.initial_amount)) = :prompt
                                  or
                              lower(text(a.current_amount)) = :prompt
                              )
                              )
                              or (
                                     (:searchBy = 'ID' and lower(a.id) like :prompt)
                                         or
                                     (:searchBy = 'OUTGOIGDOCUMENT' and lower(a.outgoing_document) like :prompt)
                                         or
                                     (:searchBy = 'CONTRACTORDER' and lower(a.Contract_Order) like :prompt)
                                         or
                                     (:searchBy = 'PODIDENTIFIER' and lower(a.pods) like :prompt)
                                         or
                                     (:searchBy = 'PODADDRESS' and lower(a.address) like :prompt)
                                     )
                                    )
                          UNION ALL
                          Select id,
                                 id_raw,
                                 outgoing_document,
                                 create_date,
                                 create_date_raw,
                                 due_date,
                                 due_date_raw,
                                 billingGroup,
                                 Contract_Order,
                                 initial_amount,
                                 current_amount,
                                 null as pods,
                                 null as address,
                                 a.offseting,
                                 object,
                                 null,
                                 null,
                                 null
                          from (Select a.deposit_number                                          as        id
                                     , a.id                                                      as        id_raw
                                     , j.identifier
                                     , null                                                      as        outgoing_document
                                     , null                                                      AS        create_date
                                     , date(null)                                                as        create_date_raw
                                     , null                                                      AS        due_date
                                     , date(null)                                                AS        due_date_raw
                                     , null                                                      as        billingGroup
                                     , (SELECT jsonb_agg(jsonb_build_object(
                                                                 'NUMBER', contract_number,
                                                                 'ID', contract_id,
                                                                 'TYPE', contract_type
                                                         ) ORDER BY contract_number)::TEXT
                                        FROM (SELECT pc.contract_number, k.contract_id, 'PRODUCT_CONTRACT' AS contract_type
                                              FROM receivable.customer_deposit_product_contracts k
                                                       JOIN product_contract.contracts pc ON pc.id = k.contract_id
                                              WHERE k.customer_deposit_id = a.id
                                              UNION ALL
                                              SELECT sc.contract_number, ks.contract_id, 'SERVICE_CONTRACT'
                                              FROM receivable.customer_deposit_service_contracts ks
                                                       JOIN service_contract.contracts sc ON sc.id = ks.contract_id
                                              WHERE ks.customer_deposit_id = a.id
                                              UNION ALL
                                              SELECT ord.order_number, so.order_id, 'SERVICE_ORDER'
                                              FROM receivable.customer_deposit_service_orders so
                                                       JOIN service_order.orders ord ON ord.id = so.order_id
                                              WHERE so.customer_deposit_id = a.id
                                              UNION ALL
                                              SELECT ord.order_number, g.order_id, 'GOODS_ORDER'
                                              FROM receivable.customer_deposit_goods_orders g
                                                       JOIN goods_order.orders ord ON ord.id = g.order_id
                                              WHERE g.customer_deposit_id = a.id) all_contracts) as        Contract_Order
                                     , -receivable.convert_to_currency(a.initial_amount, a.currency_id, 0) initial_amount
                                     , -receivable.convert_to_currency(a.current_amount, a.currency_id, 0) current_amount
                                     , null
                                     , null
                                     , l.offseting
                                     , 'deposit'                                                 as        object
                                     , null                                                      as        outgoing_document_number
                                from receivable.customer_deposits a
                                         left join customer.customers j on a.customer_id = j.id
                                         join customer.customers e on a.customer_id = e.id
                                    and e.id = :customerId
                                         left join cte3 l on l.customer_deposit_id = a.id
                                where a.status = 'ACTIVE'
                                  and ((:showDeposits) is null or :showDeposits = true)) a
                          where (:prompt is null or (:searchBy = 'ALL' and (
                              lower(a.id) like :prompt
                                  or
                              lower(a.outgoing_document) like :prompt
                                  or
                              lower(a.create_date) like :prompt
                                  or
                              lower(a.due_date) like :prompt
                                  or
                              lower(a.billingGroup) like :prompt
                                  or
                              lower(a.Contract_Order) like :prompt
                                  or
                              lower(a.offseting) like :prompt
                                  or
                              lower(a.object) like :prompt
                                  or
                              lower(text(a.initial_amount)) = :prompt
                                  or
                              lower(text(a.current_amount)) = :prompt
                              )
                              )
                              or (
                                     (:searchBy = 'ID' and lower(a.id) like :prompt)
                                         or
                                     (:searchBy = 'OUTGOIGDOCUMENT' and lower(a.outgoing_document) like :prompt)
                                         or
                                     (:searchBy = 'CONTRACTORDER' and lower(a.Contract_Order) like :prompt)
                                     )
                                    )) a) sorted_distinct_rows
                    """, nativeQuery = true)
    Page<CustomerLiabilityAndReceivableListingMiddleResponse> getCustomerLiabilityAndReceivableListingMiddleResponse(
            @Param("customerId") Long customerId,
            @Param("blockedForPayment") Boolean blockedForPayment,
            @Param("blockedForLiabilitiesOffsetting") Boolean blockedForLiabilitiesOffsetting,
            @Param("blockedForReminderLetters") Boolean blockedForReminderLetters,
            @Param("blockedForCalculationOfLatePayment") Boolean blockedForCalculationOfLatePayment,
            @Param("showDeposits") Boolean showDeposits,
            @Param("blockedForDisconnection") Boolean blockedForDisconnection,
            @Param("showLiabilitiesAndReceivables") Boolean showLiabilitiesAndReceivables,
            @Param("prompt") String prompt,
            @Param("searchBy") String searchBy,
            Pageable pageable
    );

    @Query("""
                    select sum(c.initialAmount) from CustomerLiability c
                    where c.id in (:liabilityIds)
            """)
    BigDecimal sumLiabilityAmounts(List<Long> liabilityIds);

    @Query("""
                    select cl from CustomerLiability cl 
                    where cl.status = 'ACTIVE'
                    and cl.reschedulingId=:reschedulingId
                    order by cl.dueDate
            """)
    List<CustomerLiability> findInstallments(@Param("reschedulingId") Long reschedulingId);

    @Query("""
                    select cl.latePaymentFineId from CustomerLiability cl
                    where cl.status='ACTIVE'
                    and cl.childLatePaymentFineId is not null
                    and cl.id in (:liabilities)
            """)
    List<Long> findLpfsConnectedToLiabilities(List<Long> liabilities);

    @Query("""
                    select cl from CustomerLiability cl
                    where cl.id in :liabilityIds
            """)
    List<CustomerLiability> getLiabilities(Long[] liabilityIds);

    @Query(nativeQuery = true,
            value = """
                    select crt.id as id , crt.source_object_id as sourceId
                    from receivable.customer_receivable_transactions crt
                    where crt.dest_object_type = 'LIABILITY'
                      and crt.dest_object_id = :liabilityId
                      and text(crt.source_object_type) = :sourceObjectType
                      and text(crt.operation_context) = :context
                    """
    )
    List<CustomerLiabilityTransactionsResponse> findTransactionAndDestIdByLiabilityId(Long liabilityId, String sourceObjectType, String context);

    @Query(nativeQuery = true, value = """
                select r.id
                from receivable.rescheduling_liabilities rl
                         join receivable.reschedulings r on rl.rescheduling_id = r.id
                where rl.customer_liabilitie_id = :liabilityId
                  and r.status = 'ACTIVE'
                  and r.rescheduling_status = 'EXECUTED'
                  and not r.reversed
            """
    )
    List<Long> getConnectedReschedulingId(Long liabilityId);

    @Query(nativeQuery = true, value = """
                select mlo.id
                from receivable.mlo_customer_liabilities mcl
                         inner join receivable.manual_liabilitie_offsettings mlo
                             on mlo.id = mcl.manual_liabilitie_offsetting_id
                where mcl.customer_liabilitie_id = :liabilityId
                  and not mlo.reversed
            """
    )
    List<Long> getConnectedMLOId(Long liabilityId);

    @Query(nativeQuery = true, value = """
                select lf.id from receivable.customer_liabilities cl
                join receivable.late_payment_fines lf on cl.child_late_payment_fine_id = lf.id
                and cl.id = :liabilityId and not lf.reversed
            """
    )
    List<Long> getConnectedLPFIds(Long liabilityId);

    @Query("""
                update CustomerLiability cl
                set cl.status = 'DELETED'
                where cl.id in (:liabilityIds)
            """)
    @Modifying
    void updateAllStatusByLiabilityIds(List<Long> liabilityIds);

    @Query("""
                update CustomerLiability cl
                set cl.incomeAccountNumber = :incomeAccountNumber
                where cl.invoiceId = :invoiceId
            """)
    @Modifying
    void updateAllIncomeAccountNumberByLiabilityIds(Long invoiceId, String incomeAccountNumber);

    @Query("""
                select cl.id from CustomerLiability cl
                where cl.customerId=:customerId
                and cl.id in (:liabilityIds)
                and cl.status in (:statuses)
            """)
    Set<Long> findIdByCustomerIdAndIdInAndStatusIn(Long customerId, List<Long> liabilityIds, List<EntityStatus> statuses);

    @Query(
            nativeQuery = true, value = """
            SELECT
                CASE
                    WHEN EXISTS (
                        SELECT 1
                        FROM product_contract.contracts c
                                 JOIN product_contract.contract_details cd
                                      ON (current_date between cd.start_date and coalesce(cd.end_date, current_date))
                                          AND cd.contract_id = c.id
                                 JOIN product.product_details pd
                                      ON cd.product_detail_id = pd.id
                                 LEFT JOIN terms.terms terms
                                      ON pd.term_id = terms.id
                                 LEFT JOIN terms.term_groups tergr
                                      ON pd.term_group_id = tergr.id
                                 LEFT JOIN terms.term_group_details tergrdt
                                      ON tergr.last_group_detail_id = tergrdt.id
                                 LEFT JOIN terms.terms terms1
                                      ON terms1.group_detail_id = tergrdt.id
                        WHERE c.id = :productContractId
                          AND (terms.no_interest_on_overdue_debts = TRUE
                            OR terms1.no_interest_on_overdue_debts = TRUE)
                    ) THEN TRUE
                    ELSE FALSE
                    END AS has_no_interest_on_overdue_debts
            FROM product_contract.contracts c
            WHERE c.id = :productContractId
            """
    )
    boolean getIfHasNoInterestOnOverdueDebts(Long productContractId);

}
