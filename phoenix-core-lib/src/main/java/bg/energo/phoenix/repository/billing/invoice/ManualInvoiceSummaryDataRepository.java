package bg.energo.phoenix.repository.billing.invoice;

import bg.energo.phoenix.model.entity.billing.invoice.ManualInvoiceSummaryData;
import bg.energo.phoenix.model.response.billing.invoice.InvoiceSummaryDataResponse;
import bg.energo.phoenix.service.billing.invoice.models.InvoiceDetailedDataAmountModel;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ManualInvoiceSummaryDataRepository extends JpaRepository<ManualInvoiceSummaryData, Long> {
    @Query(value = """
                    select new bg.energo.phoenix.service.billing.invoice.models.InvoiceDetailedDataAmountModel(
                           misd.vatRatePercent,
                           misd.value,
                           c.mainCurrency,
                           c.altCurrencyExchangeRate
                    )
                    from ManualInvoiceSummaryData misd
                    join Currency c on c.id = misd.valueCurrencyId
                    where misd.invoiceId = :invoiceId
            """)
    List<InvoiceDetailedDataAmountModel> findManualInvoiceAmountDataByInvoiceId(Long invoiceId);

    @Query("""
            select new bg.energo.phoenix.model.response.billing.invoice.InvoiceSummaryDataResponse(
                misd.priceComponentOrPriceComponentGroups,
                misd.totalVolumes,
                misd.measuresUnitForTotalVolumes,
                misd.unitPrice,
                misd.measureForUnitPrice,
                misd.value,
                misd.valueCurrencyName,
                misd.incomeAccountNumber,
                misd.costCenter,
                misd.vatRatePercent,
                'DIRECT'
            )
            from ManualInvoiceSummaryData misd
            where misd.invoiceId = :invoiceId
            order by misd.createDate
            """)
    Page<InvoiceSummaryDataResponse> findManualInvoiceSummaryDataByInvoiceId(Long invoiceId,
                                                                             PageRequest pageRequest);

    List<ManualInvoiceSummaryData> findAllByInvoiceId(Long invoiceId);
}