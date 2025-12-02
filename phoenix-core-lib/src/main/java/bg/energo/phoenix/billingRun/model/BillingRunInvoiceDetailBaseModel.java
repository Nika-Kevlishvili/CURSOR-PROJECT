package bg.energo.phoenix.billingRun.model;

import bg.energo.phoenix.model.enums.billing.invoice.InvoiceStandardDetailType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class BillingRunInvoiceDetailBaseModel extends Value implements Cloneable  {
    private InvoiceStandardDetailType type;
    protected Long bgInvoiceSlotId;
    protected Long podId;
    protected Long podDetailId;
    protected Long pcId;
    protected Long pcGroupId;
    protected BigDecimal calculatedVolumes;
    protected BigDecimal calculatedPrice;
    protected BigDecimal kwhPrice;
    protected LocalDateTime dateTo;
    private Long customerId;
    private Long customerDetailId;
    private Long contractDetailId;
    private Long productDetailId;
    private Long serviceDetailId;

    // restricted values
    private BigDecimal volumesOfPercentageRestriction;
    private BigDecimal amountOfPercentageRestriction;
    private BigDecimal volumesOfKwhRestriction;
    private BigDecimal amountOfKwhRestriction;
    private BigDecimal volumesOfCcyRestriction;
    private BigDecimal amountOfCcyRestriction;
    // calculated total amount for model
    private BigDecimal totalAmount;
    // calculated total percent restriction for whole priceComponent
    private BigDecimal totalPercentRestrictionByPriceComponent;
    //final prioritized restriction values
    private BigDecimal finalRestrictionVolume;
    private BigDecimal finalRestrictionAmount;

    //DISCOUNT
    private BigDecimal discountAmount;
    private BigDecimal discountPerKWH;
    private Long discountId;

    private BigDecimal finalPrice;

    //Invoice Amounts
    private BigDecimal invoiceMainCurrencyAmountWithoutVat;
    private BigDecimal invoiceMainCurrencyAmountWithVat;
    private BigDecimal invoiceMainCurrencyVatAmount;
    private Long invoiceMainCurrencyId;

    private BigDecimal invoiceVatPercent;
    private Long invoiceVatId;

    private BigDecimal invoiceAltCurrencyAmountWithoutVat;
    private BigDecimal invoiceAltCurrencyAmountWithVat;
    private BigDecimal invoiceAltCurrencyVatAmount;
    private Long invoiceAltCurrencyId;

    private BigDecimal invoiceOriginalCurrencyAmountWithoutVat;
    private BigDecimal invoiceOriginalCurrencyAmountWithVat;
    private BigDecimal invoiceOriginalCurrencyVatAmount;
    private Long invoiceOriginalCurrencyId;

    private BigDecimal invoiceTotalConsumption;


    private Long meterId;
    private BigDecimal newMeterReading;
    private BigDecimal oldMeterReading;
    private BigDecimal difference;
    private BigDecimal multiplier;
    private BigDecimal correction;
    private BigDecimal deducted;
    private Integer measuresUnitForTotalVolumes;
    private Integer measureUnitForUnitPrice;
    private String incomeAccountNumber;
    private String costCenterControllingOrder;
    private boolean isTariff;

    private Long beforeSplitDayCount;
    private BigDecimal beforeSplitVolumes;
    //private BigDecimal beforeSplitPrice;
    private Integer splitUniqueIndex;

    private Boolean calcIsOldMeterReadingSplit;
    private Boolean calcIsMeterReadingNewSplit;
    private Boolean calcIsCorrectionSplit;
    private Boolean calcIsDeductSplit;
    private Boolean calcIsDifferenceSplit;
    private Boolean hasCorrection;
    private Long[]  billingDataProfileIds;
    private Long[]  billingDataScaleIds;
    private Long priceComponentPriceTypeId;
    private Boolean discounted;
    private Boolean restricted;
    private String unrecognizedPod;
    private Integer scaleId;
    public BillingRunInvoiceDetailBaseModel(InvoiceStandardDetailType type, Long bgInvoiceSlotId, Long contractDetailId, BigDecimal finalPrice, Long pcId) {
        this.type = type;
        this.bgInvoiceSlotId = bgInvoiceSlotId;
        this.contractDetailId = contractDetailId;
        this.finalPrice = finalPrice;
        this.pcId = pcId;
        this.invoiceTotalConsumption = BigDecimal.ONE;
    }


    @Override
    public BillingRunInvoiceDetailBaseModel clone() {
        try {
            // Perform shallow copy
            BillingRunInvoiceDetailBaseModel cloned = (BillingRunInvoiceDetailBaseModel) super.clone();
            // Deep copy of the list
            if (this.billingDataScaleIds != null) {
                cloned.billingDataScaleIds = this.billingDataScaleIds.clone();
            }
            // Deep copy of the list
            if (this.billingDataProfileIds != null) {
                cloned.billingDataProfileIds = this.billingDataProfileIds.clone();
            }

            // Deep copy mutable fields manually
            if (calculatedVolumes != null) cloned.calculatedVolumes = new BigDecimal(calculatedVolumes.toString());
            if (calculatedPrice != null) cloned.calculatedPrice = new BigDecimal(calculatedPrice.toString());
            if (kwhPrice != null) cloned.kwhPrice = new BigDecimal(kwhPrice.toString());
            if (volumesOfPercentageRestriction != null)
                cloned.volumesOfPercentageRestriction = new BigDecimal(volumesOfPercentageRestriction.toString());
            if (amountOfPercentageRestriction != null)
                cloned.amountOfPercentageRestriction = new BigDecimal(amountOfPercentageRestriction.toString());
            if (volumesOfKwhRestriction != null)
                cloned.volumesOfKwhRestriction = new BigDecimal(volumesOfKwhRestriction.toString());
            if (amountOfKwhRestriction != null)
                cloned.amountOfKwhRestriction = new BigDecimal(amountOfKwhRestriction.toString());
            if (volumesOfCcyRestriction != null)
                cloned.volumesOfCcyRestriction = new BigDecimal(volumesOfCcyRestriction.toString());
            if (amountOfCcyRestriction != null)
                cloned.amountOfCcyRestriction = new BigDecimal(amountOfCcyRestriction.toString());
            if (totalAmount != null) cloned.totalAmount = new BigDecimal(totalAmount.toString());
            if (totalPercentRestrictionByPriceComponent != null)
                cloned.totalPercentRestrictionByPriceComponent = new BigDecimal(totalPercentRestrictionByPriceComponent.toString());
            if (finalRestrictionVolume != null)
                cloned.finalRestrictionVolume = new BigDecimal(finalRestrictionVolume.toString());
            if (finalRestrictionAmount != null)
                cloned.finalRestrictionAmount = new BigDecimal(finalRestrictionAmount.toString());
            if (discountAmount != null) cloned.discountAmount = new BigDecimal(discountAmount.toString());
            if (discountPerKWH != null) cloned.discountPerKWH = new BigDecimal(discountPerKWH.toString());
            if (finalPrice != null) cloned.finalPrice = new BigDecimal(finalPrice.toString());
            if (invoiceMainCurrencyAmountWithoutVat != null)
                cloned.invoiceMainCurrencyAmountWithoutVat = new BigDecimal(invoiceMainCurrencyAmountWithoutVat.toString());
            if (invoiceMainCurrencyAmountWithVat != null)
                cloned.invoiceMainCurrencyAmountWithVat = new BigDecimal(invoiceMainCurrencyAmountWithVat.toString());
            if (invoiceMainCurrencyVatAmount != null)
                cloned.invoiceMainCurrencyVatAmount = new BigDecimal(invoiceMainCurrencyVatAmount.toString());
            if (invoiceAltCurrencyAmountWithoutVat != null)
                cloned.invoiceAltCurrencyAmountWithoutVat = new BigDecimal(invoiceAltCurrencyAmountWithoutVat.toString());
            if (invoiceAltCurrencyAmountWithVat != null)
                cloned.invoiceAltCurrencyAmountWithVat = new BigDecimal(invoiceAltCurrencyAmountWithVat.toString());
            if (invoiceAltCurrencyVatAmount != null)
                cloned.invoiceAltCurrencyVatAmount = new BigDecimal(invoiceAltCurrencyVatAmount.toString());
            if (invoiceOriginalCurrencyAmountWithoutVat != null)
                cloned.invoiceOriginalCurrencyAmountWithoutVat = new BigDecimal(invoiceOriginalCurrencyAmountWithoutVat.toString());
            if (invoiceOriginalCurrencyAmountWithVat != null)
                cloned.invoiceOriginalCurrencyAmountWithVat = new BigDecimal(invoiceOriginalCurrencyAmountWithVat.toString());
            if (invoiceOriginalCurrencyVatAmount != null)
                cloned.invoiceOriginalCurrencyVatAmount = new BigDecimal(invoiceOriginalCurrencyVatAmount.toString());
            if (invoiceTotalConsumption != null)
                cloned.invoiceTotalConsumption = new BigDecimal(invoiceTotalConsumption.toString());
            if (newMeterReading != null) cloned.newMeterReading = new BigDecimal(newMeterReading.toString());
            if (oldMeterReading != null) cloned.oldMeterReading = new BigDecimal(oldMeterReading.toString());
            if (difference != null) cloned.difference = new BigDecimal(difference.toString());
            if (multiplier != null) cloned.multiplier = new BigDecimal(multiplier.toString());
            if (correction != null) cloned.correction = new BigDecimal(correction.toString());
            if (deducted != null) cloned.deducted = new BigDecimal(deducted.toString());
            if (beforeSplitVolumes != null) cloned.beforeSplitVolumes = new BigDecimal(beforeSplitVolumes.toString());

            return cloned;
        } catch (CloneNotSupportedException e) {
            throw new AssertionError("Cloning failed!", e);
        }
    }

}
