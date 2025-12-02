package bg.energo.phoenix.model.request.contract.product;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record ProductContractPointOfDeliveryRequest(
        @NotNull
        Long pointOfDeliveryDetailId,

        @Pattern(regexp = "^[0-9]{1,32}$|^$", message = "dealNumber-Invalid deal number pattern, allowed symbols: [0-9], max length: 32 digits;")
        String dealNumber
) {
}
