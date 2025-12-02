package bg.energo.phoenix.service.billing.model.persistance.dao;

import bg.energo.phoenix.service.billing.model.persistance.projection.BillingRunDocumentDetailedDataProjection;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@NoArgsConstructor
public class BillingRunDocumentDetailedDataDAO {
    private String detailType;
    private String pod;
    private String podAdditionalID;
    private String podName;
    private String meteringType;
    private String measurementType;
    private String podAddressComb;
    private String podPlace;
    private String podZip;
    private LocalDate periodFrom;
    private LocalDate periodTo;
    private String meter;
    private String priceComponent;
    private String scaleCode;
    private BigDecimal value;
    private String measureUnitOfValue;
    private BigDecimal totalVolumes;
    private String measureUnitForTotalVolumes;
    private BigDecimal price;
    private BigDecimal priceInOtherCurrency;
    private String measureUnitOfPrice;
    private String measureUnitOfPriceInOtherCurrency;
    private BigDecimal vatRatePercent;
    private BigDecimal newMeterReading;
    private BigDecimal oldMeterReading;
    private String productName;
    private BigDecimal difference;
    private Boolean doNotIncludeVatBase;

    public BillingRunDocumentDetailedDataDAO(String priceComponent,
                                             BigDecimal value,
                                             String measureUnitOfValue,
                                             BigDecimal totalVolumes,
                                             String measureUnitForTotalVolumes,
                                             BigDecimal price,
                                             BigDecimal priceInOtherCurrency,
                                             String measureUnitOfPrice,
                                             BigDecimal vatRatePercent,
                                             BigDecimal newMeterReading,
                                             BigDecimal oldMeterReading,
                                             BigDecimal difference,
                                             BigDecimal correction,
                                             BigDecimal deducted,
                                             BigDecimal multiplier,
                                             LocalDate periodFrom,
                                             LocalDate periodTo) {
        this.priceComponent = priceComponent;
        this.value = value;
        this.measureUnitOfValue = measureUnitOfValue;
        this.totalVolumes = totalVolumes;
        this.measureUnitForTotalVolumes = measureUnitForTotalVolumes;
        this.price = price;
        this.priceInOtherCurrency = priceInOtherCurrency;
        this.measureUnitOfPrice = measureUnitOfPrice;
        this.vatRatePercent = vatRatePercent;
        this.newMeterReading = newMeterReading;
        this.oldMeterReading = oldMeterReading;
        this.difference = difference;
        this.correction = correction;
        this.deducted = deducted;
        this.multiplier = multiplier;
        this.detailType = "FOR_MANUAL";
        this.periodFrom = periodFrom;
        this.periodTo = periodTo;
    }

    private BigDecimal correction;
    private BigDecimal deducted;
    private BigDecimal multiplier;

    public BillingRunDocumentDetailedDataDAO(BillingRunDocumentDetailedDataProjection projection) {
        this.detailType = projection.getDetailType();
        this.pod = projection.getPod();
        this.podAdditionalID = projection.getPodAdditionalIdentifier();
        this.podName = projection.getPodName();
        this.podAddressComb = projection.getPodAddressComb();
        this.podPlace = projection.getPodPlace();
        this.podZip = projection.getPodZip();
        this.periodFrom = projection.getPeriodFrom();
        this.periodTo = projection.getPeriodTo();
        this.meter = projection.getMeter();
        this.priceComponent = projection.getPriceComponent();
        this.value = projection.getValue();
        this.measureUnitOfValue = projection.getMeasureUnitOfValue();
        this.totalVolumes = projection.getTotalVolumes();
        this.measureUnitForTotalVolumes = projection.getMeasureUnitForTotalVolumes();
        this.price = projection.getPrice();
        this.priceInOtherCurrency = projection.getPriceInOtherCurrency();
        this.measureUnitOfPrice = projection.getMeasureUnitOfPrice();
        this.measureUnitOfPriceInOtherCurrency = projection.getMeasureUnitOfPriceInOtherCurrency();
        this.vatRatePercent = projection.getVatRatePercent();
        this.newMeterReading = projection.getNewMeterReading();
        this.oldMeterReading = projection.getOldMeterReading();
        this.productName = projection.getProductName();
        this.difference = projection.getDifference();
        this.correction = projection.getCorrection();
        this.deducted = projection.getDeducted();
        this.multiplier = projection.getMultiplier();
        this.meteringType=projection.getMeteringType();
        this.measurementType=projection.getMeasurementType();
        this.scaleCode=projection.getScaleCode();
        this.doNotIncludeVatBase=projection.getDoNotIncludeVatBase();
    }
}
