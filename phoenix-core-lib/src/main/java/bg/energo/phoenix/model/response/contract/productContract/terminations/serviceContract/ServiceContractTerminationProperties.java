package bg.energo.phoenix.model.response.contract.productContract.terminations.serviceContract;

import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class ServiceContractTerminationProperties {
    private final Integer batchSize;
    private final Integer queryBatchSize;
    private final Integer numberOfThreads;
}
