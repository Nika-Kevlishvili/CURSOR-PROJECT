package bg.energo.phoenix.model.response.billing.invoice;

import bg.energo.phoenix.model.enums.billing.invoice.InvoiceStandardDetailType;

import java.math.BigDecimal;
import java.time.LocalDate;

public interface InvoiceDetailedDataProjection {
    InvoiceStandardDetailType getDetailType();

    String getPriceComponentName();

    String getPodIdentifier();

    LocalDate getPeriodFrom();

    LocalDate getPeriodTo();

    String getMeterNumber();

    BigDecimal getNewMeterReading();

    BigDecimal getOldMeterReading();

    BigDecimal getDifference();

    BigDecimal getMultiplier();

    BigDecimal getCorrection();

    BigDecimal getDeducted();

    BigDecimal getTotalVolumes();

    String getMeasureOfTotalVolumes();

    BigDecimal getUnitPrice();

    String getMeasureOfUnitPrice();

    BigDecimal getValue();

    String getMeasureOfValue();

    String getIncomeAccountNumber();

    String getCostCenterControllingOrder();

    BigDecimal getVatRatePercent();
}
