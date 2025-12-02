package bg.energo.phoenix.model.response.contract.action.calculation;

import lombok.Data;

@Data
public class PenaltyCalculationContractVariables {
    private PenaltyCalculationProductContractVariables productContractVariables;
    private PenaltyCalculationServiceContractVariables serviceContractVariables;

    public PenaltyCalculationContractVariables(PenaltyCalculationProductContractVariables productContractVariables) {
        this.productContractVariables = productContractVariables;
    }

    public PenaltyCalculationContractVariables(PenaltyCalculationServiceContractVariables serviceContractVariables) {
        this.serviceContractVariables = serviceContractVariables;
    }

}
