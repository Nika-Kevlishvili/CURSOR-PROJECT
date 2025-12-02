package bg.energo.phoenix.model.response.contract.productContract.terminations;

import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class ProductContractTerminationProperties {
    private final Integer batchSize;
    private final Integer queryBatchSize;
    private final Integer numberOfThreads;
}
