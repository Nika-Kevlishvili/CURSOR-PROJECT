package bg.energo.phoenix.repository.product.term.terms;

import bg.energo.phoenix.billingRun.model.BillingInvoicePaymentTerm;
import bg.energo.phoenix.model.customAnotations.contract.service.ServiceContractInvoicePaymentTermsResponse;
import bg.energo.phoenix.model.entity.product.term.terms.InvoicePaymentTerms;
import bg.energo.phoenix.model.enums.product.term.terms.PaymentTermStatus;
import bg.energo.phoenix.model.response.terms.InvoicePaymentTermsResponse;
import bg.energo.phoenix.model.response.terms.ServiceOrderPaymentTermResponse;
import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface InvoicePaymentTermsRepository extends JpaRepository<InvoicePaymentTerms, Long> {

    @Query(value = """
        select new bg.energo.phoenix.model.response.terms.InvoicePaymentTermsResponse(
           ipt,
           c
       )
       from InvoicePaymentTerms ipt
       join Calendar c on c.id = ipt.calendarId
           where ipt.termId = :termId
           and ipt.status in (:statuses)
           order by ipt.createDate
    """)
    List<InvoicePaymentTermsResponse> findDetailedByTermIdAndStatusIn(
            @Param("termId") Long termId,
            @Param("statuses") List<PaymentTermStatus> statuses
    );
    @Query(value = """
        select new bg.energo.phoenix.model.customAnotations.contract.service.ServiceContractInvoicePaymentTermsResponse(
           ipt,
           c
       )
       from InvoicePaymentTerms ipt
       join Calendar c on c.id = ipt.calendarId
           where ipt.termId = :termId
           and ipt.status in (:statuses)
           order by ipt.createDate
    """)
    List<ServiceContractInvoicePaymentTermsResponse> findDetailedByTermIdAndStatusInForServiceContract(
            @Param("termId") Long termId,
            @Param("statuses") List<PaymentTermStatus> statuses
    );

    Optional<List<InvoicePaymentTerms>> findByTermIdAndStatusIn(Long id, List<PaymentTermStatus> statuses);


    @Query(
            value = """
                    select ipt from InvoicePaymentTerms ipt
                    where ipt.termId = :termId
                    and ipt.status in (:statuses)
                    """
    )
    List<InvoicePaymentTerms> findInvoicePaymentTermsByTermIdAndStatusIn(
            @Param("termId") Long termId,
            @Param("statuses") List<PaymentTermStatus> statuses
    );
    @Query(nativeQuery = true,
    value = """
            select coalesce(ipt.id, ipt2.id) as id,
                   case
                       when ipt.id is not null then coalesce(so.invoice_payment_term_value, ipt.value)
                       else ipt2.value end   as orderTermValue,
                   case
                       when ipt.id is not null then ipt.exclude_weekends
                       else ipt2.exclude_weekends
                       end                   as excludeWeekends,
                   case
                       when ipt.id is not null then ipt.exclude_holidays
                       else ipt2.exclude_holidays
                       end                   as excludeHolidays,
                   case
                       when ipt.id is not null then ipt.due_date_change
                       else ipt2.due_date_change
                       end                   as dueDateChange,
                   case
                       when ipt.id is not null then ipt.type
                       else ipt2.type
                       end                   as calendarType,
                   case
                       when ipt.id is not null then ipt.calendar_id
                       else ipt2.calendar_id
                       end                   as calendarId,
                   case
                       when so.invoice_payment_term_id is not null then t2.no_interest_on_overdue_debts
                       else t.no_interest_on_overdue_debts
                       end                   as noInterestOnOverdueDebts
            from service_order.orders so
                     join service.service_details sd
                          on so.service_detail_id = sd.id
                     left join terms.invoice_payment_terms ipt
                               on so.invoice_payment_term_id = ipt.id
                                   and ipt.status = 'ACTIVE'
                     left join terms.terms t2 on ipt.term_id = t2.id
                     left join terms.term_groups tg
                               on (sd.term_group_id = tg.id and tg.status = 'ACTIVE')
                     left join terms.term_group_details tgd
                               on (tgd.group_id = tg.id
                                   and tgd.start_date = (select max(start_date)
                                                         from terms.term_group_details dt
                                                         where dt.group_id = tg.id
                                                           and dt.start_date <= now()))
                     left join terms.term_group_terms tgt
                               on (tgt.term_group_detail_id = tgd.id and tgt.status = 'ACTIVE')
                     left join terms.terms t
                               on (t.id = tgt.term_id and tgt.term_id = t.id and t.status = 'ACTIVE')
                     left join terms.invoice_payment_terms ipt2
                               on (ipt2.term_id = t.id and ipt2.status = 'ACTIVE')
            where so.id = :orderId
              and (ipt.id is not null or ipt2.id is not null)
            order by case when so.invoice_payment_term_id is not null then 0 else 1 end
            limit 1
            """)
    Optional<ServiceOrderPaymentTermResponse> findInvoiceTermForServiceOrder(@Param("orderId") Long orderId);


    @Query(value = """
                  select p.id as id,
                     pd.invoicePaymentTermValue as value,
                     p.excludeWeekends as excludeWeekends,
                     p.excludeHolidays as excludeHolidays,
                     p.dueDateChange as dueDateChange,
                     p.calendarId as calendarId,
                     p.calendarType as calendarType
                     from InvoicePaymentTerms p
                    inner join ProductContractDetails pd on p.id = pd.invoicePaymentTermId
                    where pd.id =:productContractDetailId
""")
    BillingInvoicePaymentTerm findByProductContractDetailId(Long productContractDetailId);

    @Query(value = """
                    select p.id as id,
                     pd.invoicePaymentTermValue as value,
                     p.excludeWeekends as excludeWeekends,
                     p.excludeHolidays as excludeHolidays,
                     p.dueDateChange as dueDateChange,
                     p.calendarId as calendarId,
                     p.calendarType as calendarType
                     from InvoicePaymentTerms p
                    inner join ServiceContractDetails pd on p.id = pd.invoicePaymentTermId
                    where pd.id =:serviceContractDetailId
""")
    BillingInvoicePaymentTerm findByServiceContractDetailId(Long serviceContractDetailId);
}
