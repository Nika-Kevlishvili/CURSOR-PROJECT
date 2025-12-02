package bg.energo.phoenix.model.response.customer.relatedCustomer;

import bg.energo.phoenix.model.entity.customer.Customer;
import bg.energo.phoenix.model.entity.customer.CustomerDetails;
import bg.energo.phoenix.model.entity.customer.RelatedCustomer;
import bg.energo.phoenix.model.enums.customer.Status;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;

@Data
@AllArgsConstructor
public class RelatedCustomerResponse {
    private Long id;
    private String name;
    private String identifier;
    private Long customerId;
    private Long relatedCustomerId;
    private Long ciConnectionTypeId;
    private String ciConnectionTypeName;
    private Status status;

    public RelatedCustomerResponse(RelatedCustomer relatedCustomer,
                                   Customer customer,
                                   CustomerDetails customerDetails) {
        this.id = relatedCustomer.getId();
        this.name = customerDetails.getName()
                .concat(StringUtils.isEmpty(customerDetails.getMiddleName()) ? "" : " " + customerDetails.getMiddleName())
                .concat(" ")
                .concat(customerDetails.getLastName());
        this.customerId = relatedCustomer.getCustomerId();
        this.relatedCustomerId = relatedCustomer.getRelatedCustomerId();
        this.identifier = customer.getIdentifier();
        this.ciConnectionTypeId = relatedCustomer.getCiConnectionType().getId();
        this.ciConnectionTypeName = relatedCustomer.getCiConnectionType().getName();
        this.status = relatedCustomer.getStatus();
    }
}
