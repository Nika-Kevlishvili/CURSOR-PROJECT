package bg.energo.phoenix.model.request.billing.communicationData;

import bg.energo.phoenix.model.customAnotations.billing.communicationData.BillingCommunicationDataValidator;
import bg.energo.phoenix.model.enums.billing.billings.ContractOrderType;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@BillingCommunicationDataValidator
public class BillingCommunicationDataListRequest {

    @NotNull(message = "customerDetailsId-[customerDetailsId] should not be null;")
    private Long customerDetailsId;

    private ContractOrderType contractOrderType;

    private Long contractOrderId;

}
