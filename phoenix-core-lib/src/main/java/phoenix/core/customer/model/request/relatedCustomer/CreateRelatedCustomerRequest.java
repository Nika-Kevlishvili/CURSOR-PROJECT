package phoenix.core.customer.model.request.relatedCustomer;

import lombok.Data;
import lombok.EqualsAndHashCode;
import phoenix.core.customer.model.enums.customer.Status;

@Data
@EqualsAndHashCode(callSuper = false)
public class CreateRelatedCustomerRequest extends BaseRelatedCustomerRequest {

    public CreateRelatedCustomerRequest(EditRelatedCustomerRequest request) {
        super(request.getRelatedCustomerId(), request.getCiConnectionTypeId(), request.getStatus());
    }

    public CreateRelatedCustomerRequest(Long relatedCustomerId, Long ciConnectionTypeId, Status status) {
        super(relatedCustomerId, ciConnectionTypeId, status);
    }

}
