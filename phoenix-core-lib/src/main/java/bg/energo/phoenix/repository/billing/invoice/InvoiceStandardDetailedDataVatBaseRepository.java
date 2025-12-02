package bg.energo.phoenix.repository.billing.invoice;

import bg.energo.phoenix.model.entity.billing.invoice.InvoiceStandardDetailedDataVatBase;
import bg.energo.phoenix.model.entity.receivable.customerLiability.LiabilityVatBaseModel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InvoiceStandardDetailedDataVatBaseRepository extends JpaRepository<InvoiceStandardDetailedDataVatBase, Long> {
    @Query(value = """
            select c.id                                      as customerId,
                   vb.main_currency_id                       as mainCurrencyId,
                   vb.main_currency_total_amount_without_vat as totalAmountWithoutVatMainCurrency,
                   vb.alt_currency_total_amount_without_vat  as totalAmountWithoutVatAltCurrency
            from invoice.invoice_standard_detailed_data_vat_base vb
                     join customer.customer_details cd on cd.id = vb.alt_invoice_recipient_customer_detail_id
                     join customer.customers c on c.id = cd.customer_id
            where vb.invoice_id = :invoiceId
            """, nativeQuery = true)
    List<LiabilityVatBaseModel> findByInvoiceId(Long invoiceId);


    @Query(value = """
               select d from InvoiceStandardDetailedDataVatBase d 
               where d.invoiceId = :invoiceId and d.detailType <> 'INTERIM_DEDUCTION'
            """)
    List<InvoiceStandardDetailedDataVatBase> findAllByInvoiceIdForCorrection(Long invoiceId);
}
