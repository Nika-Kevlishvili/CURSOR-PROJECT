package bg.energo.phoenix.model.response.billing.invoice;

import bg.energo.phoenix.util.epb.EPBDecimalUtils;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;

@Data
@AllArgsConstructor
public class InvoiceDetailedDataResponse {
    private String priceComponent;
    private String pointOfDelivery;
    private LocalDate periodFrom;
    private LocalDate periodTo;
    private String meter;
    private BigDecimal newMeterReading;
    private BigDecimal oldMeterReading;
    private BigDecimal unitPrice;
    private String unitOfMeasureForUnitPrice;
    private BigDecimal value;
    private String unitOfMeasureForValue;
    private String incomeAccount;
    private String costCenter;
    private BigDecimal vatRatePercent;
    private BigDecimal deducted;
    private BigDecimal multiplier;
    private BigDecimal difference;
    private BigDecimal correction;
    private BigDecimal totalVolumes;
    private String unitOfMeasureForTotalVolumes;

    public InvoiceDetailedDataResponse(InvoiceDetailedDataProjection projection) {
        this(
                projection.getPriceComponentName(),
                projection.getPodIdentifier(),
                projection.getPeriodFrom(),
                projection.getPeriodTo(),
                projection.getMeterNumber(),
                projection.getNewMeterReading(),
                projection.getOldMeterReading(),
                projection.getUnitPrice(),
                projection.getMeasureOfUnitPrice(),
                projection.getValue(),
                projection.getMeasureOfValue(),
                projection.getIncomeAccountNumber(),
                projection.getCostCenterControllingOrder(),
                projection.getVatRatePercent(),
                projection.getDeducted(),
                projection.getMultiplier(),
                projection.getDifference(),
                projection.getCorrection(),
                projection.getTotalVolumes(),
                projection.getMeasureOfTotalVolumes()
        );
    }

    public InvoiceDetailedDataResponse(List<InvoiceDetailedDataProjection> projections) {
        InvoiceDetailedDataProjection first = projections.stream().findFirst().get();

        BigDecimal valueSum = EPBDecimalUtils.calculateSummary(
                projections
                        .stream()
                        .map(InvoiceDetailedDataProjection::getValue)
                        .toList()
        );

        BigDecimal totalVolumeSum = EPBDecimalUtils.calculateSummary(
                projections
                        .stream()
                        .map(InvoiceDetailedDataProjection::getTotalVolumes)
                        .toList()
        );

        BigDecimal price = valueSum.divide(totalVolumeSum, 12, RoundingMode.HALF_UP).stripTrailingZeros();

        this.priceComponent = first.getPriceComponentName();
        this.pointOfDelivery = first.getPodIdentifier();
        this.periodFrom = first.getPeriodFrom();
        this.periodTo = first.getPeriodTo();
        this.meter = first.getMeterNumber();
        this.newMeterReading = first.getNewMeterReading();
        this.oldMeterReading = first.getOldMeterReading();
        this.unitPrice = price;
        this.unitOfMeasureForUnitPrice = first.getMeasureOfUnitPrice();
        this.value = valueSum;
        this.unitOfMeasureForValue = first.getMeasureOfValue();
        this.incomeAccount = first.getIncomeAccountNumber();
        this.costCenter = first.getCostCenterControllingOrder();
        this.vatRatePercent = first.getVatRatePercent();
        this.deducted = first.getDeducted();
        this.multiplier = first.getMultiplier();
        this.difference = first.getDifference();
        this.correction = first.getCorrection();
        this.totalVolumes = totalVolumeSum;
        this.unitOfMeasureForTotalVolumes = first.getMeasureOfTotalVolumes();
    }
}
