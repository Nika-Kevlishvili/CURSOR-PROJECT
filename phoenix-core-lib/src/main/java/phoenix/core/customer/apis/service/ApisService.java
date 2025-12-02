package phoenix.core.customer.apis.service;

import phoenix.core.customer.apis.model.CustomerCheckRequest;
import phoenix.core.customer.apis.model.CustomerCheckResponse;

public interface ApisService {
    CustomerCheckResponse checkApisCustomersInfo(CustomerCheckRequest customerCheckRequest);
    CustomerCheckResponse checkApisCustomerInfoWithSingleIdentificationNumber(String numbers);
}
