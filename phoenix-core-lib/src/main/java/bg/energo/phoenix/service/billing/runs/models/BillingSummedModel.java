package bg.energo.phoenix.service.billing.runs.models;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter

public class BillingSummedModel extends BillingRunSettlementCalculatedModel{

    private BigDecimal totalVolumes;
    private BigDecimal summedAmounts;
    private BigDecimal averagePrice;
    private BigDecimal finalAmount;

    public BillingSummedModel(BillingRunSettlementCalculatedModel model, BigDecimal totalVolumes, BigDecimal summedAmounts, BigDecimal averagePrice, BigDecimal finalAmount) {
        super(model);
        this.totalVolumes = totalVolumes;
        this.summedAmounts = summedAmounts;
        this.averagePrice = averagePrice;
        this.finalAmount = finalAmount;
    }

    @Override
    public String toString() {
        return "BillingSummedModel{" +
               "totalVolumes=" + totalVolumes +
               ", summedAmounts=" + summedAmounts +
               ", averagePrice=" + averagePrice +
               ", finalAmount=" + finalAmount +
               ", invoiceNumber=" + getInvoiceNumber() +
               ", contractDetailId=" + getContractDetailId() +
               ", billingGroupId=" + getBillingGroupId() +
               ", separateInvoice=" + getSeparateInvoice() +
               ", podDetailId=" + getPodDetailId() +
               ", podId=" + getPodId() +
               ", measurementType=" + getMeasurementType() +
               ", billingByProfileId=" + getBillingByProfileId() +
               ", profileId=" + getProfileId() +
               ", calculatedFrom=" + getCalculatedFrom() +
               ", calculatedTo=" + getCalculatedTo() +
               ", priceComponentId=" + getPriceComponentId() +
               ", applicationModelId=" + getApplicationModelId() +
               ", periodType=" + getPeriodType() +
               '}';
    }
}
