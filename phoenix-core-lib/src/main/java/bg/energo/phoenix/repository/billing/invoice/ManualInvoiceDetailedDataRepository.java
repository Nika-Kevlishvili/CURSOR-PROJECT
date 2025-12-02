package bg.energo.phoenix.repository.billing.invoice;

import bg.energo.phoenix.model.entity.billing.invoice.ManualInvoiceDetailedData;
import bg.energo.phoenix.model.response.billing.invoice.InvoiceDetailedDataResponse;
import bg.energo.phoenix.service.billing.invoice.models.InvoiceDetailedDataAmountModel;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ManualInvoiceDetailedDataRepository extends JpaRepository<ManualInvoiceDetailedData, Long> {
    @Query(value = """
                    select new bg.energo.phoenix.service.billing.invoice.models.InvoiceDetailedDataAmountModel(
                        midd.vatRatePercent,
                        midd.value,
                        c.mainCurrency,
                        c.altCurrencyExchangeRate
                    )
                    from ManualInvoiceDetailedData midd
                    join Currency c on midd.valueCurrencyId = c.id
                    where midd.invoiceId = :invoiceId
            """)
    List<InvoiceDetailedDataAmountModel> findManualInvoiceAmountDataByInvoiceId(Long invoiceId);



    @Query("""
            select new bg.energo.phoenix.model.response.billing.invoice.InvoiceDetailedDataResponse(
                midd.priceComponentOrPriceComponentGroups,
                midd.pod,
                midd.periodFrom,
                midd.periodTo,
                midd.meter,
                midd.newMeterReading,
                midd.oldMeterReading,
                midd.unitPrice,
                midd.measureForUnitPrice,
                midd.value,
                midd.valueCurrencyName,
                midd.incomeAccountNumber,
                midd.costCenter,
                midd.vatRatePercent,
                midd.deducted,
                midd.multiplier,
                midd.differences,
                midd.correction,
                midd.totalVolumes,
                midd.measuresUnitForTotalVolumes
            )
            from ManualInvoiceDetailedData midd
            where midd.invoiceId = :invoiceId
            order by midd.createDate
            """)
    Page<InvoiceDetailedDataResponse> findManualInvoiceDetailedDataByInvoiceId(Long invoiceId,
                                                                               PageRequest pageRequest);

    List<ManualInvoiceDetailedData> findAllByInvoiceId(Long invoiceId);
}