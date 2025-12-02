package bg.energo.phoenix.model.request.billing.ProcessPeriodicity;

import bg.energo.phoenix.model.enums.billing.processPeriodicity.ProcessPeriodicityBillingProcessStart;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalTime;

@Data
public class OneTimeProcessPeriodicityDto {

    private ProcessPeriodicityBillingProcessStart processPeriodicityBillingProcessStart;

    private LocalDate oneTimeStartDate;

    private LocalTime oneTimeStartTime;
}
