package bg.energo.phoenix.model.response.customer;

import bg.energo.phoenix.model.enums.customer.CustomerStatus;
import bg.energo.phoenix.model.enums.customer.CustomerType;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

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
    private Long version;
    public CustomerResponse(Long id) {
        this.id = id;
    }
}
