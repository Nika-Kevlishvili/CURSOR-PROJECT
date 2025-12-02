package bg.energo.phoenix.model.request.contract.express;

import bg.energo.phoenix.model.customAnotations.contract.express.ExpressContractParametersValidator;
import bg.energo.phoenix.model.customAnotations.contract.express.ExpressContractTypeValidation;
import bg.energo.phoenix.model.enums.contract.express.ExpressContractType;
import bg.energo.phoenix.model.request.contract.ProxyEditRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
@ExpressContractTypeValidation
@ExpressContractParametersValidator
public class ExpressContractRequest {
    @Valid
    @NotNull(message = "productBasicParameters-Basic parameters can not be null!;")
    private ExpressContractParameters expressContractParameters;

    @Valid
    private ExpressContractCustomerRequest customer;

    @NotNull(message = "expressContractType-expressContractType is mandatory;")
    private ExpressContractType expressContractType;

    @Valid
    private ExpressContractProductParametersRequest productParameters;

    @Valid
    private ExpressContractServiceParametersRequest serviceParameters;

    private List<Long> podDetailIds;

    @Valid
    private List<@Valid ProxyEditRequest> proxyRequest;
    //Todo service contract parameters should be added and checked in @ExpressContractTypeValidation
}
