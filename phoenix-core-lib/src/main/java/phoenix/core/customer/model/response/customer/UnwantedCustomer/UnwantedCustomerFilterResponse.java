package phoenix.core.customer.model.response.customer.UnwantedCustomer;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Page;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UnwantedCustomerFilterResponse {
    Page<UnwantedCustomerResponse> unwantedCustomerResponse;
    Long count;
}
