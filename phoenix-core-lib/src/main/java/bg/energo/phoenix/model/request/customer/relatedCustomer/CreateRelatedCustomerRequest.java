package bg.energo.phoenix.model.request.customer.relatedCustomer;

import bg.energo.phoenix.model.enums.customer.Status;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = false)
@AllArgsConstructor
public class CreateRelatedCustomerRequest extends BaseRelatedCustomerRequest {

    public CreateRelatedCustomerRequest(EditRelatedCustomerRequest request) {
        super(request.getRelatedCustomerId(), request.getCiConnectionTypeId(), request.getStatus());
    }

    public CreateRelatedCustomerRequest(Long relatedCustomerId, Long ciConnectionTypeId, Status status) {
        super(relatedCustomerId, ciConnectionTypeId, status);
    }

}
