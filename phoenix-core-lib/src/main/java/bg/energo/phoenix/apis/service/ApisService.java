package bg.energo.phoenix.apis.service;

import bg.energo.phoenix.apis.model.CustomerCheckRequest;
import bg.energo.phoenix.apis.model.CustomerCheckResponse;

public interface ApisService {
    CustomerCheckResponse checkApisCustomersInfo(CustomerCheckRequest customerCheckRequest);
    CustomerCheckResponse checkApisCustomerInfoWithSingleIdentificationNumber(String numbers);
}
