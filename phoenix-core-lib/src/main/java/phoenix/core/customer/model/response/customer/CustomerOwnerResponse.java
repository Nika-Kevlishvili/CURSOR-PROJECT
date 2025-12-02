package phoenix.core.customer.model.response.customer;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import phoenix.core.customer.model.entity.customer.Customer;
import phoenix.core.customer.model.entity.customer.CustomerDetails;
import phoenix.core.customer.model.entity.customer.CustomerOwner;
import phoenix.core.customer.model.enums.customer.CustomerType;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CustomerOwnerResponse {
    protected Long id;
    protected String personalNumber;
    protected String name;

    public CustomerOwnerResponse(CustomerOwner owner, CustomerDetails details) {
        this.id = owner.getId();
        Customer ownerCustomer = owner.getOwnerCustomer();
        this.personalNumber =ownerCustomer.getIdentifier();
        if(ownerCustomer.getCustomerType().equals(CustomerType.LEGAL_ENTITY)){
            this.name = details.getName();
        }else {
            //Todo when private customer is added uncomment this
            this.name = String.format("%s %s %s",details.getName(), details.getMiddleName(), details.getLastName());
        }
    }
}
