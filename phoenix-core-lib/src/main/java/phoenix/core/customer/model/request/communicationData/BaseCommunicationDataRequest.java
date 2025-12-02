package phoenix.core.customer.model.request.communicationData;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import phoenix.core.customer.model.enums.customer.Status;
import phoenix.core.customer.model.request.CustomerAddressRequest;

import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class BaseCommunicationDataRequest {

    @NotEmpty
    private String contactTypeName;

    @Valid
    @NotNull(message = "Customer address is required; ")
    private CustomerAddressRequest address;

    @NotNull
    private Status status;

}
