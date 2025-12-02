package bg.energo.phoenix.model.request.contract.product;

import bg.energo.phoenix.model.customAnotations.contract.products.ProductContractPointOfDeliveriesValidator;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ContractPodRequest {
    @NotNull(message = "podRequests.billingGroupId-Pods must not be empty")
    private Long billingGroupId;

    @ProductContractPointOfDeliveriesValidator
    @NotEmpty(message = "podRequests.productContractPointOfDeliveries-Point Of Deliveries must not be empty")
    private List<@Valid ProductContractPointOfDeliveryRequest> productContractPointOfDeliveries;
}
