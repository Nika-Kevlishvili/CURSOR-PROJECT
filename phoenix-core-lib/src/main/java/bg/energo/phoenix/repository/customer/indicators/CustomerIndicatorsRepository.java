package bg.energo.phoenix.repository.customer.indicators;

import bg.energo.phoenix.model.entity.customer.indicators.CustomerIndicators;
import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.math.BigDecimal;

public interface CustomerIndicatorsRepository extends JpaRepository<CustomerIndicators, Long> {

    @Query(nativeQuery = true, value = """
        Select sum(current_amount) current_amount from
            (
                Select a.customer_id,  round(sum(receivable.convert_to_currency(a.current_amount,a.currency_id,0)),2) current_amount
                from receivable.customer_liabilities a
                         left join invoice.invoices b on a.invoice_id =b.id and b.status = 'REAL'
                where a.status = 'ACTIVE'
                  and full_offset_date is null
                  and (b.document_type is null or b.document_type <> 'PROFORMA_INVOICE')
                  and (a.outgoing_document_from_external_system is null or a.outgoing_document_from_external_system not like '%Deposit%')
                  and a.customer_id = :customerId
                group by a.customer_id
        
                union all
        
                Select a.customer_id, -round(sum(receivable.convert_to_currency(a.current_amount,a.currency_id,0)),2) current_amount
                from receivable.customer_receivables a
                         left join invoice.invoices b on a.invoice_id =b.id and b.status = 'REAL'
                where a.status = 'ACTIVE'
                  and full_offset_date is null
                  and a.customer_id = :customerId
                group by a.customer_id)a
        group by customer_id
    """)
    BigDecimal getCurrentLiabilities(@Param("customerId") Long customerId);
}
