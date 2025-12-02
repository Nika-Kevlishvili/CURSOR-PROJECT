package bg.energo.phoenix.service.contract.product.termination;

import bg.energo.phoenix.model.response.contract.productContract.terminations.ProductContractTerminationGenericModel;
import bg.energo.phoenix.model.response.contract.productContract.terminations.ProductContractTerminationProperties;

import java.util.List;

public interface ProductContractTerminator {

    ProductContractTerminationProperties getProperties();

    List<ProductContractTerminationGenericModel> getContractData(Integer size, Integer page);

    void terminate(ProductContractTerminationGenericModel model);

}
