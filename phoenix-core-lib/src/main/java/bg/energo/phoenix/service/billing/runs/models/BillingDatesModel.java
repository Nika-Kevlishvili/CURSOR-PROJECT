package bg.energo.phoenix.service.billing.runs.models;

import bg.energo.phoenix.model.entity.pod.billingByProfile.BillingDataByProfile;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class BillingDatesModel {

    private LocalDateTime periodFrom;

    private LocalDateTime periodTo;

    private BigDecimal value;

    private Boolean isShiftedHour = false;

    private Long billingByProfileId;

    public BillingDatesModel(BillingDataByProfile billingDataByProfile) {
        this.periodFrom = billingDataByProfile.getPeriodFrom();
        this.periodTo = billingDataByProfile.getPeriodTo();
        this.value = billingDataByProfile.getValue();
        this.isShiftedHour = billingDataByProfile.getIsShiftedHour();
        this.billingByProfileId = billingDataByProfile.getBillingByProfileId();
    }
}
