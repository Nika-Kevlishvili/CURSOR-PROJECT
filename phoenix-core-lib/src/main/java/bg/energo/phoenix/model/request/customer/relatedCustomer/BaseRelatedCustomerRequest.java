package bg.energo.phoenix.model.request.customer.relatedCustomer;

import bg.energo.phoenix.model.enums.customer.Status;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class BaseRelatedCustomerRequest {

    @NotNull(message = "relatedCustomers.relatedCustomerId-relatedCustomerId must not be null;")
    private Long relatedCustomerId;

    @NotNull(message = "relatedCustomers.ciConnectionTypeId-connectionType must not be null;")
    private Long ciConnectionTypeId;

    @NotNull(message = "relatedCustomers.status-related customer request status must not be null;")
    private Status status;

}
