package bg.energo.phoenix.model.response.contract.productContract;

import java.util.List;

public record ProductContractDataModificationResponse(
        Long id,
        List<String> resigningMessages
) {
}
