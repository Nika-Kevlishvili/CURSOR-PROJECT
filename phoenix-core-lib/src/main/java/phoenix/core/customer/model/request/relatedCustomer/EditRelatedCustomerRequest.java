package phoenix.core.customer.model.request.relatedCustomer;

import lombok.Data;
import lombok.EqualsAndHashCode;
import phoenix.core.customer.model.enums.customer.Status;

import java.util.ArrayList;
import java.util.List;

@Data
@EqualsAndHashCode(callSuper = false)
public class EditRelatedCustomerRequest extends BaseRelatedCustomerRequest {

    private Long id;

    public EditRelatedCustomerRequest(Long id, Long relatedCustomerId, Long ciConnectionTypeId, Status status) {
        super(relatedCustomerId, ciConnectionTypeId, status);
        this.id = id;
    }

    public static List<CreateRelatedCustomerRequest> getRelatedCustomersAddRequest(List<EditRelatedCustomerRequest> requestList){
        List<CreateRelatedCustomerRequest> createRelatedCustomerRequests = new ArrayList<>();
        for (int i = 0; i < requestList.size(); i++) {
            createRelatedCustomerRequests.add(new CreateRelatedCustomerRequest(requestList.get(i)));
        }
        return createRelatedCustomerRequests;
    }
}
