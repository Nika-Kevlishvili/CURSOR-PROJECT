package bg.energo.phoenix.service.billing.model.persistance.projection;

import java.math.BigDecimal;
import java.time.LocalDate;

public interface BillingRunDocumentDetailedDataProjection {
    String getDetailType();

    String getPod();
    Long getPodId();

    String getPodAdditionalIdentifier();

    String getPodName();
    String getMeteringType();
    String getMeasurementType();

    String getPodAddressComb();

    String getPodPlace();

    String getPodZip();

    LocalDate getPeriodFrom();

    LocalDate getPeriodTo();

    String getMeter();

    String getPriceComponent();

    BigDecimal getValue();

    String getMeasureUnitOfValue();

    BigDecimal getTotalVolumes();

    String getMeasureUnitForTotalVolumes();

    BigDecimal getPrice();

    BigDecimal getPriceInOtherCurrency();

    String getMeasureUnitOfPrice();

    String getMeasureUnitOfPriceInOtherCurrency();

    BigDecimal getVatRatePercent();

    BigDecimal getNewMeterReading();

    BigDecimal getOldMeterReading();

    String getProductName();

    BigDecimal getDifference();

    BigDecimal getMultiplier();

    BigDecimal getCorrection();

    BigDecimal getDeducted();

    String getScaleCode();

    Boolean getDoNotIncludeVatBase();
}
