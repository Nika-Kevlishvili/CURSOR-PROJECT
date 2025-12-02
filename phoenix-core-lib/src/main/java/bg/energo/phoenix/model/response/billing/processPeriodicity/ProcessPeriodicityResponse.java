package bg.energo.phoenix.model.response.billing.processPeriodicity;

import bg.energo.phoenix.model.entity.EntityStatus;
import bg.energo.phoenix.model.enums.billing.processPeriodicity.ProcessPeriodicityChangeTo;
import bg.energo.phoenix.model.enums.billing.processPeriodicity.ProcessPeriodicityType;
import bg.energo.phoenix.model.request.billing.ProcessPeriodicity.DTO.ProcessPeriodicityPeriodOptionsDto;
import bg.energo.phoenix.model.request.billing.ProcessPeriodicity.DTO.ProcessPeriodicityTimeIntervalDto;
import bg.energo.phoenix.model.request.billing.ProcessPeriodicity.OneTimeProcessPeriodicityDto;
import bg.energo.phoenix.model.response.nomenclature.terms.CalendarShortResponse;
import lombok.Data;

import java.util.List;

@Data
public class ProcessPeriodicityResponse {
    private Long id;
    private String name;
    private Boolean ignoreErrors;
    private Boolean ignoreWarnings;
    private ProcessPeriodicityType processPeriodicityType;
    private List<ProcessPeriodicityTimeIntervalDto> startTimeIntervals;
    private ProcessPeriodicityBillingProcessResponse startAfterProcess;

    private CalendarShortResponse calendar;
    private Boolean isWeekendsExcluded;
    private Boolean isHolidaysExcluded;
    private ProcessPeriodicityChangeTo changeTo;
    private ProcessPeriodicityPeriodOptionsDto processPeriodicityPeriodOptionsDto;

    private List<ProcessPeriodicityBillingProcessResponse> incompatibleProcesses;
    private OneTimeProcessPeriodicityDto oneTimeProcessPeriodicityDto;

    private EntityStatus status;

}
