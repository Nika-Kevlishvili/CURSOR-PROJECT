package phoenix.core.customer.model.response.customer;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import phoenix.core.customer.model.entity.customer.Customer;
import phoenix.core.customer.model.entity.customer.CustomerDetails;
import phoenix.core.customer.model.entity.customer.CustomerOwner;
import phoenix.core.customer.model.enums.customer.CustomerType;

@Data
@EqualsAndHashCode(callSuper = true)
@AllArgsConstructor
@NoArgsConstructor
public class CustomerOwnerDetailResponse extends CustomerOwnerResponse {



    private String additionalInformation;
    private Long belongingOwnerCapitalId;


    public CustomerOwnerDetailResponse(CustomerOwner owner, CustomerDetails details) {
        this.id = owner.getId();
        Customer ownerCustomer = owner.getOwnerCustomer();
        this.personalNumber =ownerCustomer.getIdentifier();
        if(ownerCustomer.getCustomerType().equals(CustomerType.LEGAL_ENTITY)){
            this.name = details.getName();
        }else {
            this.name = String.format("%s %s %s",details.getName(), details.getMiddleName(), details.getLastName());
        }
        this.additionalInformation = owner.getAdditionalInfo();
        this.belongingOwnerCapitalId = owner.getBelongingCapitalOwner().getId();
    }
}
