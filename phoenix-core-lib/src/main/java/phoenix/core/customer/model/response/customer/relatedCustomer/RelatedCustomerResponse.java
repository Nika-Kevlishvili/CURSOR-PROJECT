package phoenix.core.customer.model.response.customer.relatedCustomer;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;
import phoenix.core.customer.model.entity.customer.Customer;
import phoenix.core.customer.model.entity.customer.CustomerDetails;
import phoenix.core.customer.model.entity.customer.RelatedCustomer;
import phoenix.core.customer.model.enums.customer.Status;

@Data
@AllArgsConstructor
public class RelatedCustomerResponse {
    private Long id;
    private String name;
    private String identifier;
    private Long typeOfConnectionId;
    private String typeOfConnectionName;
    private Status status;

    public RelatedCustomerResponse(RelatedCustomer relatedCustomer,
                                   Customer customer,
                                   CustomerDetails customerDetails) {
        this.id = relatedCustomer.getId();
        this.name = customerDetails.getName()
                .concat(StringUtils.isEmpty(customerDetails.getMiddleName()) ? "" : " " + customerDetails.getMiddleName())
                .concat(" ")
                .concat(customerDetails.getLastName());
        this.identifier = customer.getIdentifier();
        this.typeOfConnectionId = relatedCustomer.getCiConnectionType().getId();
        this.typeOfConnectionName = relatedCustomer.getCiConnectionType().getName();
        this.status = relatedCustomer.getStatus();
    }
}
