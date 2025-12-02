package bg.energo.phoenix.model.request.billing.ProcessPeriodicity;

import bg.energo.phoenix.model.enums.billing.processPeriodicity.ProcessPeriodicityChangeTo;
import bg.energo.phoenix.model.enums.billing.processPeriodicity.ProcessPeriodicityType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.hibernate.validator.constraints.Length;

import java.util.List;

@Data
public class ProcessPeriodicityRequest {

    @NotBlank(message = "name-name is required;")
    @Length(min = 1, max = 512, message = "name-length should be between {min} and {max};")
    private String name;

    private Boolean ignoreErrors;

    private Boolean ignoreWarnings;

    @NotNull(message = "processPeriodicityType-is required;")
    private ProcessPeriodicityType processPeriodicityType;

    private Long startAfterProcessId;

    private Long calendarId;

    private Boolean isWeekendsExcluded;

    private Boolean isHolidaysExcluded;

    private ProcessPeriodicityChangeTo changeTo;

    private List<Long> incompatibleProcesses;

    private OneTimeProcessPeriodicityDto oneTimeProcessPeriodicityDto;

}
