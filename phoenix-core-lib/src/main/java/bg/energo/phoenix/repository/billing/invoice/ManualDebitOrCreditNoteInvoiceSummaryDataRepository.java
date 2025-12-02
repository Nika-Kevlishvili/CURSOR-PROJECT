package bg.energo.phoenix.repository.billing.invoice;

import bg.energo.phoenix.model.entity.billing.invoice.ManualDebitOrCreditNoteInvoiceSummaryData;
import bg.energo.phoenix.model.response.billing.invoice.InvoiceSummaryDataResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface ManualDebitOrCreditNoteInvoiceSummaryDataRepository extends JpaRepository<ManualDebitOrCreditNoteInvoiceSummaryData, Long> {
    @Query("""
            select new bg.energo.phoenix.model.response.billing.invoice.InvoiceSummaryDataResponse(
                mdcnisd.priceComponentOrPriceComponentGroups,
                mdcnisd.totalVolumes,
                mdcnisd.measuresUnitForTotalVolumes,
                mdcnisd.unitPrice,
                mdcnisd.measureForUnitPrice,
                mdcnisd.value,
                mdcnisd.valueCurrencyName,
                mdcnisd.incomeAccountNumber,
                mdcnisd.costCenter,
                mdcnisd.vatRatePercent,
                'DIRECT'
            )
            from ManualDebitOrCreditNoteInvoiceSummaryData mdcnisd
            where mdcnisd.invoiceId = :invoiceId
            order by mdcnisd.createDate
            """)
    Page<InvoiceSummaryDataResponse> findManualDebitOrCreditNoteInvoiceSummaryDataByInvoiceId(Long invoiceId, Pageable pageable);

    List<ManualDebitOrCreditNoteInvoiceSummaryData> findAllByInvoiceId(Long invoiceId);
}
