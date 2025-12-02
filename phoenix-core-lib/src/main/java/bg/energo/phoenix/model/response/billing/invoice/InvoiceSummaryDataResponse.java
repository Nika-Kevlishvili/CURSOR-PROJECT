package bg.energo.phoenix.model.response.billing.invoice;

import bg.energo.phoenix.model.enums.billing.invoice.SummaryDataType;

import java.math.BigDecimal;

public record InvoiceSummaryDataResponse(
        String priceComponent,
        BigDecimal totalVolumes,
        String unitOfMeasure,
        BigDecimal unitPrice,
        String unitOfMeasureForUnitPrice,
        BigDecimal value,
        String unitOfMeasureForValue,
        String incomeAccount,
        String costCenter,
        BigDecimal vatRatePercent,
        SummaryDataType type
) {
    public InvoiceSummaryDataResponse(InvoiceSummaryDataMiddleResponse response) {
        this(response.getName(),
                response.getTotalVolumes(),
                response.getMeasureUnitForTotalVolumes(),
                response.getUnitPrice(),
                response.getMeasureUnitForUnitPrice(),
                response.getValue(),
                response.getMeasureUnitForValue(),
                response.getIncomeAccountNumber(),
                response.getCostCenterControllingOrder(),
                response.getVatRate(),
                SummaryDataType.valueOf(response.getType())
        );
    }

    public InvoiceSummaryDataResponse(String priceComponent, BigDecimal totalVolumes, String unitOfMeasure, BigDecimal unitPrice, String unitOfMeasureForUnitPrice, BigDecimal value, String unitOfMeasureForValue, String incomeAccount, String costCenter, BigDecimal vatRatePercent, String type) {
        this(priceComponent, totalVolumes, unitOfMeasure, unitPrice, unitOfMeasureForUnitPrice, value, unitOfMeasureForValue, incomeAccount, costCenter, vatRatePercent, SummaryDataType.valueOf(type));
    }
    public InvoiceSummaryDataResponse(String priceComponent, BigDecimal value, String unitOfMeasureForValue, String incomeAccount, String costCenter, BigDecimal vatRatePercent, String type) {
        this(priceComponent, null, null, null, null, value, unitOfMeasureForValue, incomeAccount, costCenter, vatRatePercent, SummaryDataType.valueOf(type));
    }
}
