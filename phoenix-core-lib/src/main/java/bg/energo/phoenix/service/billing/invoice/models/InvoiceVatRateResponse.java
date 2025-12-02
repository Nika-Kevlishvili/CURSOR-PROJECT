package bg.energo.phoenix.service.billing.invoice.models;

import bg.energo.phoenix.util.epb.EPBDecimalUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;

public record InvoiceVatRateResponse(
        BigDecimal vatRatePercent,
        BigDecimal amountExcludingVat,
        BigDecimal valueOfVat
) {
    public InvoiceVatRateResponse(BigDecimal vatRatePercent, BigDecimal amount) {
        this(
                EPBDecimalUtils.convertToCurrencyScale(vatRatePercent),
                EPBDecimalUtils.convertToCurrencyScale(amount),
                EPBDecimalUtils.convertToCurrencyScale(calculateValueOfVat(vatRatePercent, amount))
        );
    }

    public InvoiceVatRateResponse(InvoiceDetailedDataAmountModel invoiceDetailedDataAmountModel) {
        this(
                EPBDecimalUtils.convertToCurrencyScale(invoiceDetailedDataAmountModel.vatRatePercent()),
                EPBDecimalUtils.convertToCurrencyScale(invoiceDetailedDataAmountModel.pureAmount()),
                EPBDecimalUtils.convertToCurrencyScale(
                        calculateValueOfVat(
                                invoiceDetailedDataAmountModel.pureAmount(),
                                invoiceDetailedDataAmountModel.vatRatePercent()
                        )
                )
        );
    }

    private static BigDecimal calculateValueOfVat(BigDecimal netAmount, BigDecimal vatRate) {
        return netAmount.multiply(vatRate).divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
    }

    @Override
    public String toString() {
        return "{" +
                "vatRatePercent=" + vatRatePercent +
                ", amountExcludingVat=" + amountExcludingVat +
                ", valueOfVat=" + valueOfVat +
                '}';
    }
}
