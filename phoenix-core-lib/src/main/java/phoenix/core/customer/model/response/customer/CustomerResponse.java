package phoenix.core.customer.model.response.customer;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import phoenix.core.customer.model.enums.customer.CustomerStatus;
import phoenix.core.customer.model.enums.customer.CustomerType;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CustomerResponse {
    private Long id;
    private Long customerNumber;
    private String identifier;
    private CustomerType customerType;
    private List<CustomerViewOwner> customerOwners;
    private Long lastCustomerDetailId;
    private CustomerStatus isDeleted;
    public CustomerResponse(Long id) {
        this.id = id;
    }
}
