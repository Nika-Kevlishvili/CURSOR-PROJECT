package bg.energo.phoenix.model.request.crm.emailCommunication;

import bg.energo.phoenix.model.response.crm.smsCommunication.ActiveContractsAndAssociatedCustomersForMassCommunicationProjection;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class MassCommunicationCustomerRequest {

    @NotBlank(message = "customerIdentifier-[customerIdentifier] customer identifier is mandatory!;")
    private String customerIdentifier;

    private Long version;

    private Long serviceContractDetailId;

    private Long productContractDetailId;

    @JsonIgnore
    private List<Long> serviceContractDetailIds;

    @JsonIgnore
    private List<Long> productContractDetailIds;

    public MassCommunicationCustomerRequest(ActiveContractsAndAssociatedCustomersForMassCommunicationProjection projection) {
        this.productContractDetailIds = projection.getProductContractDetailIds();
        this.serviceContractDetailIds = projection.getServiceContractDetailIds();
        this.customerIdentifier = projection.getCustomerIdentifier();
        this.version = projection.getCustomerVersion();
    }
}
