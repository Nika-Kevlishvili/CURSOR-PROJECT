package phoenix.core.customer.model.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.data.domain.Sort;
import phoenix.core.customer.model.enums.customer.CustomerListColumns;
import phoenix.core.customer.model.enums.customer.CustomerSearchFields;
import phoenix.core.customer.model.enums.customer.CustomerStatus;
import phoenix.core.customer.model.enums.customer.CustomerType;
import phoenix.core.customer.model.enums.customer.unwantedCustomer.UnwantedCustomerStatus;

import javax.validation.constraints.NotNull;

@Data
@AllArgsConstructor
public class GetCustomersListRequest {
    @NotNull
    Integer page;
    @NotNull
    Integer size;
    String prompt;
    CustomerSearchFields searchFields;
    CustomerStatus customerStatusFilter;
    CustomerType customerTypeFilter;
    //ACCOUNTMANAGERS
    //ECONOMICBRANCH
    String populatedPlace;
    UnwantedCustomerStatus unwantedCustomerStatus;
    CustomerListColumns customerListColumns;
    Sort.Direction columnDirection;
}
