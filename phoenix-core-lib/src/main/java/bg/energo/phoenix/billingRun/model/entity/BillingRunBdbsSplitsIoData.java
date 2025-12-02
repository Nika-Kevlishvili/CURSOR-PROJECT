package bg.energo.phoenix.billingRun.model.entity;

import bg.energo.phoenix.billingRun.model.Value;
import io.hypersistence.utils.hibernate.type.array.LongArrayType;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.Type;

import java.math.BigDecimal;
import java.time.LocalDate;
@Entity
@Table(name = "bdbs_splits_io", schema = "billing_run")
@Data
public class BillingRunBdbsSplitsIoData extends Value implements Cloneable{

    @Id
    @Column(name = "bdbs_split_io_id")
    private Long bdsSplitIoId;

    @Column(name = "bdbs_id")
    private Long bdbsId;

    @Column(name = "calc_period_from")
    private LocalDate calcPeriodFrom;

    @Column(name = "calc_period_to")
    private LocalDate calcPeriodTo;

    @Column(name = "header_period_from")
    private LocalDate headerPeriodFrom;

    @Column(name = "header_period_to")
    private LocalDate headerPeriodTo;

    @Column(name = "calc_volume")
    private BigDecimal calcVolume;

    @Column(name = "calc_tariff_value")
    private BigDecimal calcTariffValue;

    @Column(name = "contract_id")
    private Long contractId;

    @Column(name = "meter_id")
    private Long meterId;

    @Column(name = "pod_detail_id")
    private Long podDetailId;

    @Column(name = "pc_group_id")
    private Long pcGroupId;

    @Column(name = "product_detail_id")
    private Long productDetailId;

    @Column(name = "run_contract_id")
    private Long runContractId;

    @Column(name = "is_tariff")
    private Boolean isTariff;

    @Column(name = "pod_id")
    private Long podId;

    @Column(name = "pc_id")
    private Long pcId;

    @Column(name = "contract_version_id")
    private Long contractDetailId;

    @Column(name = "bg_invoice_slot_id")
    private Long bgInvoiceSlotId;

    @Column(name = "customer_id", nullable = false)
    private Long customerId;

    @Column(name = "customer_detail_id", nullable = false)
    private Long customerDetailId;

    @Column(name = "calc_is_old_meter_reading_split")
    private Boolean calcIsOldMeterReadingSplit;

    @Column(name = "calc_is_meter_reading_new_split")
    private Boolean calcIsMeterReadingNewSplit;

    @Column(name = "calc_is_correction_split")
    private Boolean calcIsCorrectionSplit;

    @Column(name = "calc_is_deduct_split")
    private Boolean calcIsDeductSplit;

    @Column(name = "calc_is_difference_split")
    private Boolean calcIsDifferenceSplit;

    @Column(name = "calc_new_meter_readings")
    private BigDecimal calcNewMeterReadings;

    @Column(name = "calc_old_meter_reading")
    private BigDecimal calcOldMeterReading;

    @Column(name = "calc_difference")
    private BigDecimal calcDifference;

    @Column(name = "calc_deduction")
    private BigDecimal calcDeduction;

    @Column(name = "multiplier")
    private BigDecimal multiplier;

    @Column(name = "calc_correction")
    private BigDecimal calcCorrection;

    @Column(name = "price_formula")
    private String priceFormula;

    @Column(name = "has_correction")
    private Boolean hasCorrection;

    @Transient
    private BigDecimal runTotalPrice;
    @Transient
    private BigDecimal runTotalValue;
    @Transient
    private BigDecimal runKwhPrice;

    @Type(LongArrayType.class)
    @Column(name = "billing_data_scale_ids")
    private Long[] billingDataScaleIds;

    @Column(name = "price_component_price_type_id")
    private Long priceComponentPriceTypeId;

    @Column(name = "scale_id")
    private Integer scaleId;

    @Override
    public BillingRunBdbsSplitsIoData clone() {
        try {
            BillingRunBdbsSplitsIoData clone = (BillingRunBdbsSplitsIoData) super.clone();

            // Deep copy of the list
            if (this.billingDataScaleIds != null) {
                clone.billingDataScaleIds = this.billingDataScaleIds.clone();
            }
            // Copy mutable fields explicitly
            clone.calcPeriodFrom = (this.calcPeriodFrom != null) ? LocalDate.from(this.calcPeriodFrom) : null;
            clone.calcPeriodTo = (this.calcPeriodTo != null) ? LocalDate.from(this.calcPeriodTo) : null;
            clone.calcVolume = (this.calcVolume != null) ? new BigDecimal(this.calcVolume.toString()) : null;
            clone.calcTariffValue = (this.calcTariffValue != null) ? new BigDecimal(this.calcTariffValue.toString()) : null;
            clone.multiplier = (this.multiplier != null) ? new BigDecimal(this.multiplier.toString()) : null;
            clone.calcNewMeterReadings = (this.calcNewMeterReadings != null) ? new BigDecimal(this.calcNewMeterReadings.toString()) : null;
            clone.calcOldMeterReading = (this.calcOldMeterReading != null) ? new BigDecimal(this.calcOldMeterReading.toString()) : null;
            clone.calcDifference = (this.calcDifference != null) ? new BigDecimal(this.calcDifference.toString()) : null;
            clone.calcCorrection = (this.calcCorrection != null) ? new BigDecimal(this.calcCorrection.toString()) : null;
            return clone;
        } catch (CloneNotSupportedException e) {
            throw new AssertionError("Cloning not supported!", e); // This should never happen
        }
    }

}