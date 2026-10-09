package bg.energo.migration.integration.phoenix.customer.model.request.relatedcustomers;

import bg.energo.migration.integration.phoenix.customer.model.enums.Status;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateRelatedCustomerRequest {
    private Long relatedCustomerId;
    private Long ciConnectionTypeId;
    private Status status;
}
