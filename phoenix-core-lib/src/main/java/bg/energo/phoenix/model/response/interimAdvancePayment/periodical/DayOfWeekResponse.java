package bg.energo.phoenix.model.response.interimAdvancePayment.periodical;

import bg.energo.phoenix.model.entity.product.iap.interimAdvancePayment.InterimAdvancePaymentDayWeekPeriodYear;
import bg.energo.phoenix.model.enums.product.iap.interimAdvancePayment.Day;
import bg.energo.phoenix.model.enums.product.iap.interimAdvancePayment.InterimAdvancePaymentSubObjectStatus;
import bg.energo.phoenix.model.enums.product.iap.interimAdvancePayment.Week;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DayOfWeekResponse {

    private Long id;
    private Week week;
    private List<Day> day;
    private InterimAdvancePaymentSubObjectStatus status;

    public DayOfWeekResponse(InterimAdvancePaymentDayWeekPeriodYear interimAdvancePaymentDayWeekPeriodYear){
        this.id = interimAdvancePaymentDayWeekPeriodYear.getId();
        this.week = interimAdvancePaymentDayWeekPeriodYear.getWeek();
        this.day = interimAdvancePaymentDayWeekPeriodYear.getDays();
        this.status = interimAdvancePaymentDayWeekPeriodYear.getStatus();
    }
}
