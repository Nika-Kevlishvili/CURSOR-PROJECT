package phoenix.core.customer;

import org.springframework.data.domain.Page;
import phoenix.core.customer.apis.model.CustomerCheckRequest;
import phoenix.core.customer.apis.model.CustomerCheckResponse;
import phoenix.core.customer.model.request.CreateCustomerRequest;
import phoenix.core.customer.model.request.CustomerListingResponse;
import phoenix.core.customer.model.request.EditCustomerRequest;
import phoenix.core.customer.model.request.GetCustomersListRequest;
import phoenix.core.customer.model.response.customer.CustomerResponse;
import phoenix.core.customer.model.response.customer.CustomerViewResponse;
import phoenix.core.exception.CustomerCreateException;

public interface CustomerFacade {
    CustomerResponse create(CreateCustomerRequest request, String systemUserId) throws CustomerCreateException;

    CustomerResponse delete(Long id, String systemUserId);

    CustomerCheckResponse checkCustomer(CustomerCheckRequest customerCheckRequest);

    CustomerResponse update(Long id, EditCustomerRequest request, String systemUserId);

    Page<CustomerListingResponse> list(GetCustomersListRequest request);

    CustomerViewResponse view(Long id, Long version);

}