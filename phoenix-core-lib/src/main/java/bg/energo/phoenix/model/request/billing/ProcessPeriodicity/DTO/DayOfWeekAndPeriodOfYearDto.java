package bg.energo.phoenix.model.request.billing.ProcessPeriodicity.DTO;

import lombok.Data;

import java.util.List;

@Data
public class DayOfWeekAndPeriodOfYearDto {
    private List<DayOfWeekDto> daysOfWeek;
    private Boolean yearAround;
    private List<PeriodOfYearDto> periodsOfYear;
}
