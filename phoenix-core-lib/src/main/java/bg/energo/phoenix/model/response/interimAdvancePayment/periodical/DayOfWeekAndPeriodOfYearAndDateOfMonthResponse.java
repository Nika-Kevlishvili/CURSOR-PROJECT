package bg.energo.phoenix.model.response.interimAdvancePayment.periodical;

import bg.energo.phoenix.model.enums.product.iap.interimAdvancePayment.PeriodType;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class DayOfWeekAndPeriodOfYearAndDateOfMonthResponse {

    private PeriodType periodType;

    private DayOfWeekAndPeriodOfYearResponse dayOfWeekAndPeriodOfYear;

    private List<DateOfMonthResponse> dateOfMonths;

    public DayOfWeekAndPeriodOfYearAndDateOfMonthResponse(
            PeriodType periodType,
            List<DayOfWeekResponse> daysOfWeek,
            Boolean yearRound,
            List<PeriodOfYearResponse> periodsOfYear,
            List<DateOfMonthResponse> dateOfMonths){
        dayOfWeekAndPeriodOfYear = new DayOfWeekAndPeriodOfYearResponse(daysOfWeek, yearRound, periodsOfYear);
        this.dateOfMonths = dateOfMonths;
        this.periodType = periodType;
    }

}
