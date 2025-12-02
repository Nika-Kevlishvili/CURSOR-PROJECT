package bg.energo.phoenix.model.response.customer.owner;

import bg.energo.phoenix.model.entity.customer.Customer;
import bg.energo.phoenix.model.entity.customer.CustomerDetails;
import bg.energo.phoenix.model.entity.customer.CustomerOwner;
import bg.energo.phoenix.model.enums.customer.CustomerType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CustomerOwnerResponse {
    protected Long id;
    protected String personalNumber;
    protected String name;
    protected CustomerType ownerType;

    public CustomerOwnerResponse(CustomerOwner owner, CustomerDetails details) {
        this.id = owner.getId();
        Customer ownerCustomer = owner.getOwnerCustomer();
        this.personalNumber =ownerCustomer.getIdentifier();
        if(ownerCustomer.getCustomerType().equals(CustomerType.LEGAL_ENTITY)){
            this.name = details.getName();
        }else {
            //Todo when private customer is added uncomment this
            //this.name = String.format("%s %s %s",details.getName(), details.getMiddleName(), details.getLastName());
            this.name = String.format("%s %s %s", details.getName(), details.getMiddleName() != null ? details.getMiddleName() : "", details.getLastName() != null ? details.getLastName() : "").trim();
        }
        this.ownerType = ownerCustomer.getCustomerType();
    }
}
