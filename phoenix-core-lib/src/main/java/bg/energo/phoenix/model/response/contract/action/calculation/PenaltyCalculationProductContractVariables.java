package bg.energo.phoenix.model.response.contract.action.calculation;

import java.math.BigDecimal;
import java.time.LocalDate;

public interface PenaltyCalculationProductContractVariables {

    Long getContractId();

    Long getContractDetailId();

    BigDecimal getEstimatedTotalConsumptionUnderContract();

    BigDecimal getAvgHourlyLoadProfiles();

    LocalDate getContractInitialTermStartDate();

    LocalDate getContractTermEndDate();

    LocalDate getRealTerminationDate();

    BigDecimal getSummedEstimatedMonthlyAvgConsumptionForPods();

    BigDecimal getActualTotalConsumptionUnderContract();

    BigDecimal getActualTotalConsumptionUnderContractForPod();

    BigDecimal getAvgMonthlyActualConsumptionUnderContract();

    BigDecimal getAvgMonthlyActualConsumptionUnderContractForPod();

    BigDecimal getAvgDailyActualConsumptionUnderContract();

    BigDecimal getAvgDailyActualConsumptionUnderContractForPod();
    BigDecimal getPriceFromLastInvoiceForActiveEnergy();

    BigDecimal getActualPav();
}
