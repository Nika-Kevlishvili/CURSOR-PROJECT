package bg.energo.migration.customer.mapper.accountmanager;

import bg.energo.migration.customer.entity.accountmanager.CustomerAccountManagerView;
import bg.energo.migration.integration.phoenix.customer.model.request.accountmanagers.CreateCustomerAccountManagerRequest;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CustomerAccountManagerRequestMapper {

    public List<CreateCustomerAccountManagerRequest> map(
            List<CustomerAccountManagerView> customerAccountManagerViewList
    ) {
        return customerAccountManagerViewList
                .stream()
                .map(this::mapToCustomerAccountManager)
                .toList();
    }

    private CreateCustomerAccountManagerRequest mapToCustomerAccountManager(
            CustomerAccountManagerView customerAccountManagerView
    ) {
        return CreateCustomerAccountManagerRequest
                .builder()
                .accountManagerId(customerAccountManagerView.getAccountManagerId())
                .accountManagerTypeId(customerAccountManagerView.getAccountManagerTypeId())
                .build();
    }
}
