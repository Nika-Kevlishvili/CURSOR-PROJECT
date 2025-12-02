package phoenix.core.customer.apis.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CustomerCheckResponse {
    List<ApisCustomer> customers;
}
