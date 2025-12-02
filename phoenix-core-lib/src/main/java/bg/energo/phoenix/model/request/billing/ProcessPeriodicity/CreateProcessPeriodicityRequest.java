package bg.energo.phoenix.model.request.billing.ProcessPeriodicity;

import bg.energo.phoenix.model.customAnotations.billing.processPeriodicity.ProcessPeriodicityValidator;
import bg.energo.phoenix.model.request.billing.ProcessPeriodicity.baseDTO.BaseProcessPeriodicityPeriodOptionsDto;
import bg.energo.phoenix.model.request.billing.ProcessPeriodicity.baseDTO.BaseProcessPeriodicityTimeIntervalDto;
import jakarta.validation.Valid;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@Data
@ProcessPeriodicityValidator
@EqualsAndHashCode(callSuper = false)
public class CreateProcessPeriodicityRequest extends ProcessPeriodicityRequest {

    private List<BaseProcessPeriodicityTimeIntervalDto> startTimeIntervals;

    @Valid
    private BaseProcessPeriodicityPeriodOptionsDto baseProcessPeriodicityPeriodOptionsDto;

}
