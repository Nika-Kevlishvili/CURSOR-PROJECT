package bg.energo.phoenix.repository.receivable.payment;

import bg.energo.phoenix.model.entity.EntityStatus;
import bg.energo.phoenix.model.entity.receivable.customerLiability.CustomerLiabilityPaidByPayment;
import bg.energo.phoenix.model.entity.receivable.payment.Payment;
import bg.energo.phoenix.model.entity.receivable.payment.PaymentReceivableOffsetting;
import bg.energo.phoenix.model.response.receivable.payment.PaymentListMiddleResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    Optional<Payment> findByIdAndStatusIn(Long id, List<EntityStatus> entityStatuses);

    Optional<Payment> findByPaymentNumber(String paymentNumber);

    @Query(value = "select  nextval('receivable.customer_payments_id_seq')", nativeQuery = true)
    String getNextSequenceValue();

    @Query(value = """
            select
            case when exists (select 1 from
                               receivable.customer_liabilitie_paid_by_payments clpbp
                              where clpbp.customer_payment_id = cp.id
                                and clpbp.status = 'ACTIVE'
                                and clpbp.customer_liabilitie_id in(select customer_liabilitie_id from receivable.customer_liabilitie_paid_by_receivables clpbr where clpbr.status = 'ACTIVE')) then 'CANNOT_DELETE'
            when exists (select 1 from
                               receivable.customer_liabilitie_paid_by_payments clpbp
                              where clpbp.customer_payment_id = cp.id
                                and clpbp.status = 'ACTIVE') then 'CAN_DELETE_WITH_MESSAGE'
            else 'CAN_DELETE'
                                end can_delete
            from receivable.customer_payments cp
            where cp.id =  :customerPaymentId
            """, nativeQuery = true)
    String getPaymentDeleteStatus(@Param("customerPaymentId") Long paymentId);

    @Query(value = """
            select cp.payment_number                                                    as paymentNumber,
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
                   case cp.outgoing_document_type
                       when 'INVOICE'
                           then (select invoice_number from invoice.invoices i where cp.invoice_id = i.id)
                       when 'LATE_PAYMENT_FINE'
                           then (select late_payment_number
                                 from receivable.late_payment_fines lpf
                                 where cp.late_payment_fine_id = lpf.id)
                       when 'DEPOSIT'
                           then (select deposit_number from receivable.customer_deposits cd where cp.customer_deposit_id = cd.id)
                       when 'PENALTY'
                           then (select text(id) from terms.penalties p where cp.penalty_id = p.id)
                       end                                                              as outgoingDocumentType,
                   ccd.name                                                             as paymentChannel,
                   pp.id                                                                as paymentPackage,
                   cp.payment_date                                                      as paymentDate,
                   receivable.convert_to_currency(cp.initial_amount, cp.currency_id, 0) as initialAmount,
                   receivable.convert_to_currency(cp.current_amount, cp.currency_id, 0) as currentAmount,
                   cp.id                                                                as customerPaymentId,
                   cp.status                                                            as status
            from receivable.customer_payments cp
                     join customer.customers c on cp.customer_id = c.id and c.status = 'ACTIVE'
                     join customer.customer_details cd on c.last_customer_detail_id = cd.id
                     left join product_contract.contract_billing_groups cbg
                               on cp.contract_billing_group_id = cbg.id and cbg.status = 'ACTIVE'
                     join receivable.collection_channels ccd on cp.collection_channel_id = ccd.id
                     join receivable.payment_packages pp on cp.payment_package_id = pp.id
                     left join nomenclature.legal_forms lf on cd.legal_form_id = lf.id
            where ((:status) is null or text(cp.status) in :status)
              and (coalesce(:initialAmountFrom, '0') = '0' or
                   receivable.convert_to_currency(cp.initial_amount, cp.currency_id, 0) >= :initialAmountFrom)
              and (coalesce(:initialAmountTo, '0') = '0' or
                   receivable.convert_to_currency(cp.initial_amount, cp.currency_id, 0) <= :initialAmountTo)
              and (coalesce(:currentAmountFrom, '0') = '0' or
                   receivable.convert_to_currency(cp.current_amount, cp.currency_id, 0) >= :currentAmountFrom)
              and (coalesce(:currentAmountTo, '0') = '0' or
                   receivable.convert_to_currency(cp.current_amount, cp.currency_id, 0) <= :currentAmountTo)
              and ((:collectionChannelIds) is null or cp.collection_channel_id in (:collectionChannelIds))
              and (:blockedForOffsetting is null or coalesce(cp.blocked_for_offsetting, false) = :blockedForOffsetting)
              and (date(:PaymentDateFrom) is null or cp.payment_date >= date(:PaymentDateFrom))
              and (date(:PaymentDateTo) is null or cp.payment_date <= date(:PaymentDateTo))
              and (coalesce(:prompt, '') = '' or (:searchBy = 'ALL' and coalesce(:prompt, '') <> '' and (
                lower(cp.payment_number) = lower(:prompt)
                    or c.identifier = :prompt
                    or text(c.customer_number) = :prompt
                    or concat(c.customer_number, cbg.group_number) = :prompt
                    or text(pp.id) = :prompt
                    or (:prompt =
                        case cp.outgoing_document_type
                            when 'INVOICE'
                                then (select invoice_number from invoice.invoices i where cp.invoice_id = i.id)
                            when 'LATE_PAYMENT_FINE'
                                then (select late_payment_number
                                      from receivable.late_payment_fines lpf
                                      where cp.late_payment_fine_id = lpf.id)
                            when 'DEPOSIT'
                                then (select deposit_number
                                      from receivable.customer_deposits cd
                                      where cp.customer_deposit_id = cd.id)
                            when 'PENALTY'
                                then (select text(id) from terms.penalties p where cp.penalty_id = p.id)
                            end)
                ))
                or ((:searchBy = 'PAYMENT_NUMBER' and lower(cp.payment_number) = lower(:prompt))
                    or (:searchBy = 'CUSTOMER_NUMBER' and
                        (text(c.customer_number) = :prompt or concat(c.customer_number, cbg.group_number) = :prompt))
                    or (:searchBy = 'CUSTOMER_IDENTIFIER') and c.identifier = :prompt
                    or (:searchBy = 'PAYMENT_PACKAGE' and text(pp.id) = :prompt)
                    or (:searchBy = 'OUTGOING_DOCUMENT' and (:prompt =
                                                             case cp.outgoing_document_type
                                                                 when 'INVOICE' then (select invoice_number
                                                                                      from invoice.invoices i
                                                                                      where cp.invoice_id = i.id)
                                                                 when 'LATE_PAYMENT_FINE' then (select late_payment_number
                                                                                                from receivable.late_payment_fines lpf
                                                                                                where cp.late_payment_fine_id = lpf.id)
                                                                 when 'DEPOSIT' then (select deposit_number
                                                                                      from receivable.customer_deposits cd
                                                                                      where cp.customer_deposit_id = cd.id)
                                                                 when 'PENALTY' then (select text(id)
                                                                                      from terms.penalties p
                                                                                      where cp.penalty_id = p.id)
                                                                 end))))
            """,
            countQuery = """
                    select count(*)
                    from receivable.customer_payments cp
                             join customer.customers c
                                  on cp.customer_id = c.id
                                      and c.status = 'ACTIVE'
                             join customer.customer_details cd
                                  on c.last_customer_detail_id = cd.id
                             left join product_contract.contract_billing_groups cbg
                                       on cp.contract_billing_group_id = cbg.id
                                           and cbg.status = 'ACTIVE'
                             join receivable.collection_channels ccd
                                  on cp.collection_channel_id = ccd.id
                             join receivable.payment_packages pp
                                  on cp.payment_package_id = pp.id
                             left join nomenclature.legal_forms lf on cd.legal_form_id = lf.id
                    where ((:status) is null or text(cp.status) in :status)
                      and (coalesce(:initialAmountFrom, '0') = '0' or
                           receivable.convert_to_currency(cp.initial_amount, cp.currency_id, 0) >= :initialAmountFrom)
                      and (coalesce(:initialAmountTo, '0') = '0' or
                           receivable.convert_to_currency(cp.initial_amount, cp.currency_id, 0) <= :initialAmountTo)
                      and (coalesce(:currentAmountFrom, '0') = '0' or
                           receivable.convert_to_currency(cp.current_amount, cp.currency_id, 0) >= :currentAmountFrom)
                      and (coalesce(:currentAmountTo, '0') = '0' or
                           receivable.convert_to_currency(cp.current_amount, cp.currency_id, 0) <= :currentAmountTo)
                      and ((:collectionChannelIds) is null or cp.collection_channel_id in (:collectionChannelIds))
                      and (:blockedForOffsetting is null or coalesce(cp.blocked_for_offsetting, false) = :blockedForOffsetting)
                      and (date(:PaymentDateFrom) is null or cp.payment_date >= date(:PaymentDateFrom))
                      and (date(:PaymentDateTo) is null or cp.payment_date <= date(:PaymentDateTo))
                      and (coalesce(:prompt, '') = '' or (:searchBy = 'ALL' and coalesce(:prompt, '') <> '' and (
                        lower(cp.payment_number) = lower(:prompt)
                            or c.identifier = :prompt
                            or text(c.customer_number) = :prompt
                            or concat(c.customer_number, cbg.group_number) = :prompt
                            or text(pp.id) = :prompt
                            or (:prompt =
                                case cp.outgoing_document_type
                                    when 'INVOICE'
                                        then (select invoice_number from invoice.invoices i where cp.invoice_id = i.id)
                                    when 'LATE_PAYMENT_FINE'
                                        then (select late_payment_number
                                              from receivable.late_payment_fines lpf
                                              where cp.late_payment_fine_id = lpf.id)
                                    when 'DEPOSIT'
                                        then (select deposit_number
                                              from receivable.customer_deposits cd
                                              where cp.customer_deposit_id = cd.id)
                                    when 'PENALTY'
                                        then (select text(id) from terms.penalties p where cp.penalty_id = p.id)
                                    end)
                        ))
                        or ((:searchBy = 'PAYMENT_NUMBER' and lower(cp.payment_number) = lower(:prompt))
                            or (:searchBy = 'CUSTOMER_NUMBER' and
                                (text(c.customer_number) = :prompt or concat(c.customer_number, cbg.group_number) = :prompt))
                            or (:searchBy = 'CUSTOMER_IDENTIFIER') and c.identifier = :prompt
                            or (:searchBy = 'PAYMENT_PACKAGE' and text(pp.id) = :prompt)
                            or (:searchBy = 'OUTGOING_DOCUMENT' and (:prompt =
                                                                     case cp.outgoing_document_type
                                                                         when 'INVOICE' then (select invoice_number
                                                                                              from invoice.invoices i
                                                                                              where cp.invoice_id = i.id)
                                                                         when 'LATE_PAYMENT_FINE' then (select late_payment_number
                                                                                                        from receivable.late_payment_fines lpf
                                                                                                        where cp.late_payment_fine_id = lpf.id)
                                                                         when 'DEPOSIT' then (select deposit_number
                                                                                              from receivable.customer_deposits cd
                                                                                              where cp.customer_deposit_id = cd.id)
                                                                         when 'PENALTY' then (select text(id)
                                                                                              from terms.penalties p
                                                                                              where cp.penalty_id = p.id)
                                                                         end))))
                    """,
            nativeQuery = true
    )
    Page<PaymentListMiddleResponse> filter(
            @Param("searchBy") String searchBy,
            @Param("prompt") String prompt,
            @Param("status") List<String> status,
            @Param("initialAmountFrom") BigDecimal initialAmountFrom,
            @Param("initialAmountTo") BigDecimal initialAmountTo,
            @Param("currentAmountFrom") BigDecimal currentAmountFrom,
            @Param("currentAmountTo") BigDecimal currentAmountTo,
            @Param("collectionChannelIds") List<Long> collectionChannelIds,
            @Param("blockedForOffsetting") Boolean blockedForOffsetting,
            @Param("PaymentDateFrom") LocalDate paymentDateFrom,
            @Param("PaymentDateTo") LocalDate paymentDateTo,
            Pageable pageable
    );

    boolean existsByPaymentPackageIdAndStatus(Long paymentPackageId, EntityStatus status);

    @Query(value = """
            select distinct
                subquery.id,
                subquery.customer,
                subquery.billing_group,
                subquery.outgoing_document_type,
                subquery.payment_channel,
                subquery.payment_package,
                subquery.payment_date,
                subquery.initial_amount,
                subquery.current_amount,
                subquery.customer_payment_id,
                subquery.payment_number,
                subquery.status
            from (
                select distinct
                    cp.id as id,
                    case when c.customer_type = 'PRIVATE_CUSTOMER'
                             then concat(c.identifier,concat(' (',cd.name),case when cd.middle_name is not null then cd.middle_name  end,case when cd.last_name is not null then cd.last_name  end,')' )
                         when c.customer_type = 'LEGAL_ENTITY' then concat(c.identifier,' (',cd.name,')')
                        end customer,
                    cbg.group_number as billing_group,
                    case cp.outgoing_document_type
                        when 'INVOICE'
                            then (select invoice_number from invoice.invoices i where cp.invoice_id = i.id)
                        when 'LATE_PAYMENT_FINE'
                            then (select late_payment_number from receivable.late_payment_fines lpf where cp.late_payment_fine_id = lpf.id)
                        when 'DEPOSIT'
                            then (select deposit_number from receivable.customer_deposits cd where cp.customer_deposit_id  = cd.id)
                        when 'PENALTY'
                            then (select text(id) from terms.penalties p where cp.penalty_id = p.id)
                        end outgoing_document_type,
                    ccd.name as payment_channel,
                    pp.id as payment_package,
                    cp.payment_date as payment_date,
                    receivable.convert_to_currency(cp.initial_amount, cp.currency_id, 0) as initial_amount,
                    receivable.convert_to_currency(cp.current_amount, cp.currency_id, 0) as current_amount,
                    cp.id as customer_payment_id,
                    cp.payment_number as payment_number,
                    cp.status as status
                from
                    receivable.customer_payments cp
                        join  customer.customers c
                              on cp.customer_id =  c.id
                                  and c.status = 'ACTIVE'
                        join customer.customer_details cd
                             on cd.customer_id = c.id
                             and c.id = :customerId
                        left join product_contract.contract_billing_groups cbg
                                  on cp.contract_billing_group_id = cbg.id
                                      and cbg.status = 'ACTIVE'
                        join receivable.collection_channels ccd
                             on cp.collection_channel_id = ccd.id
                        join receivable.payment_packages pp
                             on cp.payment_package_id  = pp.id
                where
                    ((:status) is null or text(cp.status) in (:status))
                    and
                    (receivable.convert_to_currency(cp.initial_amount, cp.currency_id, 0)  >= :initialAmountFrom or :initialAmountFrom is null)
                  and
                    (receivable.convert_to_currency(cp.initial_amount, cp.currency_id, 0) <= :initialAmountTo or :initialAmountTo is null)
                  and
                    (receivable.convert_to_currency(cp.current_amount, cp.currency_id, 0) >= :currentAmountFrom or :currentAmountFrom is null)
                  and
                    (receivable.convert_to_currency(cp.current_amount, cp.currency_id, 0) <= :currentAmountTo or :currentAmountTo is null)
                  and
                    ((:collectionChannelIds) is null or cp.collection_channel_id in (:collectionChannelIds))
                  and
                    (:blockedForOffsetting is null or coalesce(cp.blocked_for_offsetting,false) = :blockedForOffsetting)
                  and
                    (date(:PaymentDateFrom) is null or cp.payment_date >= date(:PaymentDateFrom))
                  and
                    (date(:PaymentDateTo) is null or cp.payment_date <= date(:PaymentDateTo))
                  and (coalesce(:prompt,'') = '' or (:searchBy = 'ALL' and coalesce(:prompt,'') <> '' and (
                        lower(cp.payment_number) like lower(:prompt)
                        or
                            c.identifier like :prompt
                        or
                            text(c.customer_number) like :prompt
                        or
                            text(cbg.group_number) like :prompt
                        or
                            text(pp.id) like :prompt
                        or
                            (:prompt =
                             case cp.outgoing_document_type
                                 when 'INVOICE'
                                     then (select invoice_number from invoice.invoices i where cp.invoice_id = i.id)
                                 when 'LATE_PAYMENT_FINE'
                                     then (select late_payment_number from receivable.late_payment_fines lpf where cp.late_payment_fine_id = lpf.id)
                                 when 'DEPOSIT'
                                     then (select deposit_number from receivable.customer_deposits cd where cp.customer_deposit_id  = cd.id)
                                 when 'PENALTY'
                                     then (select text(id) from terms.penalties p where cp.penalty_id = p.id)
                                 end)
                    )
                    )
                                            or (
                                       (text(:searchBy) = 'PAYMENT_NUMBER' and text(lower(cp.payment_number)) like text(lower(:prompt)))
                                       or
                                       (:searchBy = 'CUSTOMER_NUMBER' and (text(c.customer_number)  like :prompt or concat(c.customer_number,cbg.group_number) = :prompt) )
                                       or
                                       (:searchBy = 'PAYMENT_PACKAGE' and text(pp.id) like :prompt)
                                       or
                                       (:searchBy = 'BILLING_GROUP' and text(cbg.group_number) like :prompt)
                                       or
                                       (:searchBy = 'OUTGOING_DOCUMENT' and (:prompt =
                                                                             case cp.outgoing_document_type
                                                                                 when 'INVOICE'
                                                                                     then (select invoice_number from invoice.invoices i where cp.invoice_id = i.id)
                                                                                 when 'LATE_PAYMENT_FINE'
                                                                                     then (select late_payment_number from receivable.late_payment_fines lpf where cp.late_payment_fine_id = lpf.id)
                                                                                 when 'DEPOSIT'
                                                                                     then (select deposit_number from receivable.customer_deposits cd where cp.customer_deposit_id  = cd.id)
                                                                                 when 'PENALTY'
                                                                                     then (select text(id) from terms.penalties p where cp.penalty_id = p.id)
                                                                                 end))
                                   )
                            )
            ) as subquery
            """,
            countQuery = """
                    select
                        count(distinct cp.id)
                    from
                        receivable.customer_payments cp
                            join  customer.customers c
                                  on cp.customer_id =  c.id
                                      and c.status = 'ACTIVE'
                            join customer.customer_details cd
                                 on cd.customer_id = c.id
                                 and c.id = :customerId
                            left join product_contract.contract_billing_groups cbg
                                      on cp.contract_billing_group_id = cbg.id
                                          and cbg.status = 'ACTIVE'
                            join receivable.collection_channels ccd
                                 on cp.collection_channel_id = ccd.id
                            join receivable.payment_packages pp
                                 on cp.payment_package_id  = pp.id
                    where
                        ((:status) is null or text(cp.status) in (:status))
                        and
                        (receivable.convert_to_currency(cp.initial_amount, cp.currency_id, 0)  >= :initialAmountFrom or :initialAmountFrom is null)
                      and
                        (receivable.convert_to_currency(cp.initial_amount, cp.currency_id, 0) <= :initialAmountTo or :initialAmountTo is null)
                      and
                        (receivable.convert_to_currency(cp.current_amount, cp.currency_id, 0) >= :currentAmountFrom or :currentAmountFrom is null)
                      and
                        (receivable.convert_to_currency(cp.current_amount, cp.currency_id, 0) <= :currentAmountTo or :currentAmountTo is null)
                      and
                        ((:collectionChannelIds) is null or cp.collection_channel_id in (:collectionChannelIds))
                      and
                        (:blockedForOffsetting is null or coalesce(cp.blocked_for_offsetting,false) = :blockedForOffsetting)
                      and
                        (date(:PaymentDateFrom) is null or cp.payment_date >= date(:PaymentDateFrom))
                      and
                        (date(:PaymentDateTo) is null or cp.payment_date <= date(:PaymentDateTo))
                      and (coalesce(:prompt,'') = '' or (:searchBy = 'ALL' and coalesce(:prompt,'') <> '' and (
                                lower(cp.payment_number) like lower(:prompt)
                            or
                                c.identifier like :prompt
                            or
                                text(c.customer_number) like :prompt
                            or
                                text(cbg.group_number) like :prompt
                            or
                                text(pp.id) like :prompt
                            or
                                (:prompt =
                                 case cp.outgoing_document_type
                                     when 'INVOICE'
                                         then (select invoice_number from invoice.invoices i where cp.invoice_id = i.id)
                                     when 'LATE_PAYMENT_FINE'
                                         then (select late_payment_number from receivable.late_payment_fines lpf where cp.late_payment_fine_id = lpf.id)
                                     when 'DEPOSIT'
                                         then (select deposit_number from receivable.customer_deposits cd where cp.customer_deposit_id  = cd.id)
                                     when 'PENALTY'
                                         then (select text(id) from terms.penalties p where cp.penalty_id = p.id)
                                     end)
                        )
                        )
                        or (
                                   (text(:searchBy) = 'PAYMENT_NUMBER' and text(lower(cp.payment_number)) like text(lower(:prompt)))
                                   or
                                   (:searchBy = 'CUSTOMER_NUMBER' and (text(c.customer_number)  like :prompt or concat(c.customer_number,cbg.group_number) = :prompt) )
                                   or
                                   (:searchBy = 'PAYMENT_PACKAGE' and text(pp.id) like :prompt)
                                   or
                                   (:searchBy = 'BILLING_GROUP' and text(cbg.group_number) like :prompt)
                                   or
                                   (:searchBy = 'OUTGOING_DOCUMENT' and (:prompt =
                                                                         case cp.outgoing_document_type
                                                                             when 'INVOICE'
                                                                                 then (select invoice_number from invoice.invoices i where cp.invoice_id = i.id)
                                                                             when 'LATE_PAYMENT_FINE'
                                                                                 then (select late_payment_number from receivable.late_payment_fines lpf where cp.late_payment_fine_id = lpf.id)
                                                                             when 'DEPOSIT'
                                                                                 then (select deposit_number from receivable.customer_deposits cd where cp.customer_deposit_id  = cd.id)
                                                                             when 'PENALTY'
                                                                                 then (select text(id) from terms.penalties p where cp.penalty_id = p.id)
                                                                             end))
                               )
                        )
                    """, nativeQuery = true)
    Page<PaymentListMiddleResponse> getCustomerRelatedPayment(
            @Param("customerId") Long customerId,
            @Param("searchBy") String searchBy,
            @Param("prompt") String prompt,
            @Param("status") List<String> status,
            @Param("initialAmountFrom") BigDecimal initialAmountFrom,
            @Param("initialAmountTo") BigDecimal initialAmountTo,
            @Param("currentAmountFrom") BigDecimal currentAmountFrom,
            @Param("currentAmountTo") BigDecimal currentAmountTo,
            @Param("collectionChannelIds") List<Long> collectionChannelIds,
            @Param("blockedForOffsetting") Boolean blockedForOffsetting,
            @Param("PaymentDateFrom") LocalDate paymentDateFrom,
            @Param("PaymentDateTo") LocalDate paymentDateTo,
            Pageable pageable
    );

    @Query(value = """
            select pros
            from PaymentReceivableOffsetting pros
            where pros.customerPaymentId = :paymentId
            and pros.status = 'ACTIVE'
            """)
    List<PaymentReceivableOffsetting> getPaymentReceivableOffsetting(
            @Param("paymentId") Long paymentId
    );

    @Query(value = """
            select clpbp
            from CustomerLiabilityPaidByPayment clpbp
            where clpbp.customerPaymentId = :paymentId
            and clpbp.status = 'ACTIVE'
            """)
    List<CustomerLiabilityPaidByPayment> getPaymentLiabilityOffsetting(
            @Param("paymentId") Long paymentId
    );

    @Query("""
            select (count(*) > 0) as is_online_payment
            from Payment payment
                     join PaymentPackage package on package.id = payment.paymentPackageId
            where payment.id = :paymentId
              and package.type = 'ONLINE'
            """
    )
    boolean isOnlinePayment(@Param("paymentId") Long paymentId);

    @Query("""
                    select p from Payment p
                    where p.status='ACTIVE'
                    and p.currentAmount>0
            """)
    List<Payment> findPaymentsWithCurrentAmountMoreThanZero();
}
