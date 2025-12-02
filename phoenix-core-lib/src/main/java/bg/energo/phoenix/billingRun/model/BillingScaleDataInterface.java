package bg.energo.phoenix.billingRun.model;

import java.math.BigDecimal;
import java.time.LocalDate;

public interface BillingScaleDataInterface {
    Long getBdbsSplitIoId();

    Long getBgInvoiceSlotId();

    Long getContractDetailId();

    Long getPcId();

    Long getPodId();

    Long getPodDetailId();

    BigDecimal getTotalVolumesKwh();

    BigDecimal getVolumes();

    Long getContractId();

    Long getRunContractId();

    Integer getDays();

    Integer getTotalDays();

    LocalDate getDateFrom();

    LocalDate getDateTo();

    Boolean getTariff();

    String getPriceFormula();

    Long getCustomerId();

    Long getCustomerDetailId();

    Long getProductDetailId();

    Long getPcGroupId();

    BigDecimal getNewMeterReading();

    BigDecimal getOldMeterReading();

    BigDecimal getDifference();

    BigDecimal getMultiplier();

    BigDecimal getCorrection();

    BigDecimal getDeducted();

    Long getMeterId();
}
