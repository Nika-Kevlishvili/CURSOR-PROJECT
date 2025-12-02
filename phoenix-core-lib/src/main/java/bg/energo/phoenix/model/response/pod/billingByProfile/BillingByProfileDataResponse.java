package bg.energo.phoenix.model.response.pod.billingByProfile;

import bg.energo.phoenix.model.entity.pod.billingByProfile.BillingDataByProfile;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class BillingByProfileDataResponse {

    private Long id;
    private BigDecimal value;
    private Boolean isShiftedHour;
    private LocalDateTime periodFrom;
    private LocalDateTime periodTo;
    private Integer periodFromYearsUnit;
    private Integer periodFromMonthsUnit;
    private Integer periodFromDaysUnit;
    private Integer periodFromHoursUnit;
    private Integer periodFromMinutesUnit;
    private Integer periodToYearsUnit;
    private Integer periodToMonthsUnit;
    private Integer periodToDaysUnit;
    private Integer periodToHoursUnit;
    private Integer periodToMinutesUnit;

    public BillingByProfileDataResponse(BillingDataByProfile billingDataByProfile) {
        this.id = billingDataByProfile.getId();
        this.value = billingDataByProfile.getValue();
        this.isShiftedHour = billingDataByProfile.getIsShiftedHour();
        this.periodFrom = billingDataByProfile.getPeriodFrom();
        this.periodTo = billingDataByProfile.getPeriodTo();
        this.periodFromYearsUnit = billingDataByProfile.getPeriodFrom().getYear();
        this.periodFromMonthsUnit = billingDataByProfile.getPeriodFrom().getMonth().getValue();
        this.periodFromDaysUnit = billingDataByProfile.getPeriodFrom().getDayOfMonth();
        this.periodFromHoursUnit = billingDataByProfile.getPeriodFrom().getHour();
        this.periodFromMinutesUnit = billingDataByProfile.getPeriodFrom().getMinute();
        this.periodToYearsUnit = billingDataByProfile.getPeriodTo().getYear();
        this.periodToMonthsUnit = billingDataByProfile.getPeriodTo().getMonth().getValue();
        this.periodToDaysUnit = billingDataByProfile.getPeriodTo().getDayOfMonth();
        this.periodToHoursUnit = billingDataByProfile.getPeriodTo().getHour();
        this.periodToMinutesUnit = billingDataByProfile.getPeriodTo().getMinute();
    }

}
