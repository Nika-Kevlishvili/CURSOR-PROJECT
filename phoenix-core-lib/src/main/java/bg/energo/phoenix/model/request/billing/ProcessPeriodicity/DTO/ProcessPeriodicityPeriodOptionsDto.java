package bg.energo.phoenix.model.request.billing.ProcessPeriodicity.DTO;

import bg.energo.phoenix.model.customAnotations.product.applicationModel.RRuleValidator;
import bg.energo.phoenix.model.enums.billing.processPeriodicity.ProcessPeriodicityPeriodType;
import lombok.Data;

import java.util.List;

@Data
public class ProcessPeriodicityPeriodOptionsDto {
    private ProcessPeriodicityPeriodType processPeriodicityPeriodType;
    private DayOfWeekAndPeriodOfYearDto dayOfWeekAndPeriodOfYearDto;
    private List<DateOfMonthDto> dateOfMonths;
    @RRuleValidator(message = "processPeriodicityPeriodOptionsDto.RRUle")
    private String RRUle;


}
