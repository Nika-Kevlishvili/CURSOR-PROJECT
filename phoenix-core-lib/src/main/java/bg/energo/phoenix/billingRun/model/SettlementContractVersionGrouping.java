package bg.energo.phoenix.billingRun.model;

import bg.energo.phoenix.billingRun.model.entity.BillingRunSettlementPeriod;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SettlementContractVersionGrouping {
    private Long bgInvoiceSlotId;
    private Long contractDetailId;
    private Long priceComponentId;

    private Long podId;
    private LocalDateTime dateFrom;
    private LocalDateTime dateTo;


    public SettlementContractVersionGrouping(BillingRunSettlementPeriod settlementPeriod) {
        this.bgInvoiceSlotId = settlementPeriod.getBgInvoiceSlotId();
        this.contractDetailId = settlementPeriod.getContractDetailId();
        this.podId = settlementPeriod.getPodId();
        this.priceComponentId = settlementPeriod.getPriceComponentId();
        this.dateFrom = settlementPeriod.getDateFrom();
        this.dateTo = settlementPeriod.getDateTo();
    }
}
