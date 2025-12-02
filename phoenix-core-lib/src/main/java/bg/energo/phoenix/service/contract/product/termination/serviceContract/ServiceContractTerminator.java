package bg.energo.phoenix.service.contract.product.termination.serviceContract;

import bg.energo.phoenix.model.response.contract.productContract.terminations.serviceContract.ServiceContractTerminationGenericModel;
import bg.energo.phoenix.model.response.contract.productContract.terminations.serviceContract.ServiceContractTerminationProperties;

import java.util.List;

public interface ServiceContractTerminator {

    ServiceContractTerminationProperties getProperties();

    List<ServiceContractTerminationGenericModel> getContractData(Integer size, List<Long> contractIdsToExclude);

    void terminate(ServiceContractTerminationGenericModel model);

}
