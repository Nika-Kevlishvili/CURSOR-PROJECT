package phoenix.core.customer.model.request;


import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = false)
public class CustomerOwnerEditRequest extends CustomerOwnerRequest {

    private Long id;

    public static List<CustomerOwnerRequest> getCustomerOwnerAddRequest(List<CustomerOwnerEditRequest> requests){
        return requests !=null ? requests.stream().map(CustomerOwnerRequest::new).toList() : null;
    }
}
