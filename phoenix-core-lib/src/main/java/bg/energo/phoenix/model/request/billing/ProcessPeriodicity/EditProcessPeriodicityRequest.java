package bg.energo.phoenix.model.request.billing.ProcessPeriodicity;

import bg.energo.phoenix.model.customAnotations.billing.processPeriodicity.EditProcessPeriodicityValidator;
import bg.energo.phoenix.model.request.billing.ProcessPeriodicity.DTO.ProcessPeriodicityPeriodOptionsDto;
import bg.energo.phoenix.model.request.billing.ProcessPeriodicity.DTO.ProcessPeriodicityTimeIntervalDto;
import jakarta.validation.Valid;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@Data
@EditProcessPeriodicityValidator
@EqualsAndHashCode(callSuper = false)
public class EditProcessPeriodicityRequest extends ProcessPeriodicityRequest {
    private Long id;
    private List<ProcessPeriodicityTimeIntervalDto> startTimeIntervals;
    @Valid
    private ProcessPeriodicityPeriodOptionsDto processPeriodicityPeriodOptionsDto;
}
