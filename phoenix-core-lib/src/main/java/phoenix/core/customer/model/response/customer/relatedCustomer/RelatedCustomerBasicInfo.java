package phoenix.core.customer.model.response.customer.relatedCustomer;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class RelatedCustomerBasicInfo {
    private Long id;
    private String name;
}
