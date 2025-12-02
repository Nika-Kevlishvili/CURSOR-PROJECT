package bg.energo.phoenix.model.response.contract.action.calculation;

import java.time.LocalDate;

public interface PenaltyCalculationServiceContractVariables {

    Long getContractId();

    Long getContractDetailId();

    LocalDate getContractTermEndDate();

    LocalDate getRealTerminationDate();

}
