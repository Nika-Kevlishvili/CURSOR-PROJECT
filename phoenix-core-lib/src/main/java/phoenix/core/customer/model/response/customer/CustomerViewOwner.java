package phoenix.core.customer.model.response.customer;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import phoenix.core.customer.model.enums.customer.Status;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CustomerViewOwner {
    private Long id;
    private BelongingCapitalOwnerResponse belongingCapitalOwner;
    private CustomerResponse customer;
    private CustomerResponse ownerCustomer;
    private String additionalInfo;
    private Status status;
}
