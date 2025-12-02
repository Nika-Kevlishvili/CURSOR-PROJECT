package bg.energo.phoenix.repository.billing.invoice;

import bg.energo.phoenix.model.entity.billing.invoice.InvoiceVatRateValue;
import bg.energo.phoenix.model.enums.billing.invoice.InvoiceStatus;
import bg.energo.phoenix.model.response.billing.invoice.InvoiceDetailedExportModel;
import bg.energo.phoenix.service.billing.invoice.models.InvoiceVatRateResponse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface InvoiceVatRateValueRepository extends JpaRepository<InvoiceVatRateValue, Long> {
    @Query("""
            select new bg.energo.phoenix.service.billing.invoice.models.InvoiceVatRateResponse(
                ivrv.vatRatePercent,
                ivrv.amountExcludingVat,
                ivrv.valueOfVat
            )
            from InvoiceVatRateValue ivrv
            where ivrv.invoiceId = :invoiceId
            """)
    List<InvoiceVatRateResponse> findByInvoiceId(Long invoiceId);

    List<InvoiceVatRateValue> findAllByInvoiceIdIn(Collection<Long> invoiceIds);
    List<InvoiceVatRateValue> findAllByInvoiceId(Long invoiceId);

    @Query("""
            select new bg.energo.phoenix.model.response.billing.invoice.InvoiceDetailedExportModel(
                ivrv.vatRatePercent,
                ivrv.amountExcludingVat,
                ivrv.valueOfVat,
                ivrv.invoiceId
            )
            from InvoiceVatRateValue ivrv
            join Invoice inv on ivrv.invoiceId=inv.id
            where inv.billingId=:billingId
            and inv.invoiceStatus=:status
            """)
    List<InvoiceDetailedExportModel> findByBillingIdForExport(Long billingId, InvoiceStatus status);
}