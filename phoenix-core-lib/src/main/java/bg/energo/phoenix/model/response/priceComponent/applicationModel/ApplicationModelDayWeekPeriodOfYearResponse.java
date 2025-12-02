package bg.energo.phoenix.model.response.priceComponent.applicationModel;

import lombok.Data;

import java.util.List;
@Data
public class ApplicationModelDayWeekPeriodOfYearResponse {
    private Boolean yearRound;
    private List<IssuingPeriodsResponse> periodsOfYear;
    private List<ApplicationModelDayOfWeekResponse> dayOfWeek;
}
