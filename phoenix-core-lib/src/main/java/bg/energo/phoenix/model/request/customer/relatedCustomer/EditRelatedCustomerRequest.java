package bg.energo.phoenix.model.request.customer.relatedCustomer;

import bg.energo.phoenix.model.enums.customer.Status;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@EqualsAndHashCode(callSuper = false)
@NoArgsConstructor
public class EditRelatedCustomerRequest extends BaseRelatedCustomerRequest {

    private Long id;

    public EditRelatedCustomerRequest(Long id, Long relatedCustomerId, Long ciConnectionTypeId, Status status) {
        super(relatedCustomerId, ciConnectionTypeId, status);
        this.id = id;
    }

    public static List<CreateRelatedCustomerRequest> getRelatedCustomersAddRequest(List<EditRelatedCustomerRequest> requestList){
        List<CreateRelatedCustomerRequest> createRelatedCustomerRequests = new ArrayList<>();
        if(requestList == null) return null;
        for (EditRelatedCustomerRequest request : requestList) {
            createRelatedCustomerRequests.add(new CreateRelatedCustomerRequest(request));
        }
        return createRelatedCustomerRequests;
    }
}
