package bg.energo.phoenix.service.billing.runs.models;

import bg.energo.phoenix.model.enums.time.PeriodType;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
public class BillingDataShortModelProfile extends BillingDataShortModel {
    private Long billingByProfileId;
    private Long profileId;
    private PeriodType periodType;

    public BillingDataShortModelProfile( Long podId, LocalDateTime billingFrom, LocalDateTime billingTo, LocalDateTime createDate, Long billingByProfileId, Long profileId,PeriodType periodType) {
        super(podId, billingFrom.toLocalDate(), billingTo.toLocalDate(), createDate);
        this.billingByProfileId = billingByProfileId;
        this.profileId=profileId;
        this.periodType=periodType;
    }
}
