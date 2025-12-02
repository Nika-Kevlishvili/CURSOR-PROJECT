package bg.energo.phoenix.model.response.contract.productContract;

import bg.energo.phoenix.model.response.customer.CustomerShortResponse;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class CustomerVersionedResponse {
    private CustomerShortResponse customer;
    private List<CustomerDetailsShortResponse> details;
}
