package phoenix.core.customer.model.request.relatedCustomer;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import phoenix.core.customer.model.enums.customer.Status;

import javax.validation.constraints.NotNull;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class BaseRelatedCustomerRequest {

    @NotNull(message = "relatedCustomerId: must not be null.")
    private Long relatedCustomerId;

    @NotNull(message = "connectionType: must not be null.")
    private Long ciConnectionTypeId;

    @NotNull(message = "status: must not be null.")
    private Status status;

}
