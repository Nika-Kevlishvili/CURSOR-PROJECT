package bg.energo.phoenix.model.response.interimAdvancePayment.periodical;

import bg.energo.phoenix.model.entity.product.iap.interimAdvancePayment.InterimAdvancePaymentIssuingPeriod;
import bg.energo.phoenix.model.enums.product.iap.interimAdvancePayment.InterimAdvancePaymentSubObjectStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PeriodOfYearResponse {
    private Long id;
    private String startDate;
    private String endDate;
    private InterimAdvancePaymentSubObjectStatus status;

    public PeriodOfYearResponse(InterimAdvancePaymentIssuingPeriod interimAdvancePaymentIssuingPeriod){
        this.id = interimAdvancePaymentIssuingPeriod.getId();
        this.startDate = interimAdvancePaymentIssuingPeriod.getPeriodFrom();
        this.endDate = interimAdvancePaymentIssuingPeriod.getPeriodTo();
        this.status = interimAdvancePaymentIssuingPeriod.getStatus();
    }
}
