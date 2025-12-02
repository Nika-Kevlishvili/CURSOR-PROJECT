package bg.energo.phoenix.service.riskList;

import bg.energo.phoenix.model.entity.customer.Customer;
import bg.energo.phoenix.model.entity.customer.CustomerDetails;
import bg.energo.phoenix.service.riskList.model.RiskListBasicInfoResponse;
import bg.energo.phoenix.service.riskList.model.RiskListFullInfoResponse;
import bg.energo.phoenix.service.riskList.model.RiskListRequest;

public interface RiskListService {

    RiskListBasicInfoResponse evaluateBasicCustomerRisk(RiskListRequest riskListRequest);

    RiskListBasicInfoResponse evaluateBasicCustomerRisk(RiskListRequest riskListRequest, Customer customer, CustomerDetails customerDetails);

    RiskListFullInfoResponse evaluateFullCustomerRisk(String identifier);

}
