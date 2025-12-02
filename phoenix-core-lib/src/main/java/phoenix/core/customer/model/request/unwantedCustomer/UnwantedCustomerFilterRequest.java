package phoenix.core.customer.model.request.unwantedCustomer;

import lombok.Data;
import org.springframework.data.domain.Sort;
import phoenix.core.customer.model.enums.customer.unwantedCustomer.UnwantedCustomerFilterField;
import phoenix.core.customer.model.enums.customer.unwantedCustomer.UnwantedCustomerSortField;

import javax.validation.constraints.NotNull;
import java.util.List;

@Data
public class UnwantedCustomerFilterRequest {
    @NotNull
    Integer page;
    @NotNull
    Integer size;
    String prompt;
    UnwantedCustomerFilterField filterField;
    List<Long> reasonId;
    UnwantedCustomerSortField sortField;
    Sort.Direction direction;
}
