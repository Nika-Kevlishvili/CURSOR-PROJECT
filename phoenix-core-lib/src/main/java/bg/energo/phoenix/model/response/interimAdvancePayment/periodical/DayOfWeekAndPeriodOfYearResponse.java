package bg.energo.phoenix.model.response.interimAdvancePayment.periodical;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DayOfWeekAndPeriodOfYearResponse {

    private List<DayOfWeekResponse> daysOfWeek;

    private Boolean yearRound;

    private List<PeriodOfYearResponse> periodsOfYear;

}
