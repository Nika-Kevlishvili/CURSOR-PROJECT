package bg.energo.phoenix.model.request.billing.ProcessPeriodicity.baseDTO;

import bg.energo.phoenix.model.customAnotations.product.applicationModel.RRuleValidator;
import bg.energo.phoenix.model.enums.billing.processPeriodicity.ProcessPeriodicityPeriodType;
import lombok.Data;

import java.util.List;

@Data
public class BaseProcessPeriodicityPeriodOptionsDto {

    private ProcessPeriodicityPeriodType processPeriodicityPeriodType;

    private BaseDayOfWeekAndPeriodOfYearDto baseDayOfWeekAndPeriodOfYearDto;

    private List<BaseDateOfMonthDto> dateOfMonths;

    @RRuleValidator(message = "baseProcessPeriodicityPeriodOptionsDto.RRUle")
    private String RRUle;

}
