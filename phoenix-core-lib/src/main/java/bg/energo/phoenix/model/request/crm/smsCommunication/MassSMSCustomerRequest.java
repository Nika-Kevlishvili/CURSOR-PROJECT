package bg.energo.phoenix.model.request.crm.smsCommunication;

import bg.energo.phoenix.model.response.crm.smsCommunication.ActiveContractsAndAssociatedCustomersForMassCommunicationProjection;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class MassSMSCustomerRequest {
    @NotBlank(message = "customerIdentifier-[customerIdentifier] customer identifier is mandatory!;")
    private String customerIdentifier;
    private Long version;
    private Long serviceContractDetailId;
    private Long productContractDetailId;

    @JsonIgnore
    private List<Long> serviceContractDetailIds;
    @JsonIgnore
    private List<Long> productContractDetailIds;

    public MassSMSCustomerRequest(ActiveContractsAndAssociatedCustomersForMassCommunicationProjection customers) {
        this.customerIdentifier = customers.getCustomerIdentifier();
        this.version = customers.getCustomerVersion();
        this.serviceContractDetailIds=customers.getServiceContractDetailIds();
        this.productContractDetailIds = customers.getProductContractDetailIds();
    }
}
