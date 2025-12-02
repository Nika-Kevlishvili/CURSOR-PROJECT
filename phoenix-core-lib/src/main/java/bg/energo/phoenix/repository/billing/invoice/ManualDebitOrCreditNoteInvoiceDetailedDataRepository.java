package bg.energo.phoenix.repository.billing.invoice;

import bg.energo.phoenix.model.entity.billing.invoice.ManualDebitOrCreditNoteInvoiceDetailedData;
import bg.energo.phoenix.model.response.billing.invoice.InvoiceDetailedDataResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface ManualDebitOrCreditNoteInvoiceDetailedDataRepository extends JpaRepository<ManualDebitOrCreditNoteInvoiceDetailedData, Long> {
    @Query("""
            select new bg.energo.phoenix.model.response.billing.invoice.InvoiceDetailedDataResponse(
                mdcnidd.priceComponentOrPriceComponentGroups,
                mdcnidd.pod,
                mdcnidd.periodFrom,
                mdcnidd.periodTo,
                mdcnidd.meter,
                mdcnidd.newMeterReading,
                mdcnidd.oldMeterReading,
                mdcnidd.unitPrice,
                mdcnidd.measureForUnitPrice,
                mdcnidd.value,
                mdcnidd.valueCurrencyName,
                mdcnidd.incomeAccountNumber,
                mdcnidd.costCenter,
                mdcnidd.vatRatePercent,
                mdcnidd.deducted,
                mdcnidd.multiplier,
                mdcnidd.differences,
                mdcnidd.correction,
                mdcnidd.totalVolumes,
                mdcnidd.measuresUnitForTotalVolumes
            )
            from ManualDebitOrCreditNoteInvoiceDetailedData mdcnidd
            where mdcnidd.invoiceId = :invoiceId
            order by mdcnidd.createDate
            """)
    Page<InvoiceDetailedDataResponse> findManualDebitOrCreditNoteInvoiceDetailedDataByInvoiceId(Long invoiceId, Pageable pageable);

    List<ManualDebitOrCreditNoteInvoiceDetailedData> findAllByInvoiceId(Long invoiceId);

}