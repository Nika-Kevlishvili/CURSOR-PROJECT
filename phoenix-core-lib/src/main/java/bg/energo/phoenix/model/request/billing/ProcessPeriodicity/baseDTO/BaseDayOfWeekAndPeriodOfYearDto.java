package bg.energo.phoenix.model.request.billing.ProcessPeriodicity.baseDTO;

import lombok.Data;

import java.util.List;

@Data
public class BaseDayOfWeekAndPeriodOfYearDto {

    private List<BaseDayOfWeekDto> daysOfWeek;

    private Boolean yearAround;

    private List<BasePeriodOfYearDto> periodsOfYear;

}
