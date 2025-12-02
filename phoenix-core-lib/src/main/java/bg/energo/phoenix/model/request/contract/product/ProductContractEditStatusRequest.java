package bg.energo.phoenix.model.request.contract.product;

import bg.energo.phoenix.model.enums.contract.express.ProductContractVersionStatus;
import bg.energo.phoenix.model.enums.contract.products.ContractDetailsStatus;
import bg.energo.phoenix.model.enums.contract.products.ContractDetailsSubStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ProductContractEditStatusRequest {

    @NotNull(message = "contractStatus-contractStatus is mandatory;")
    private ContractDetailsStatus contractStatus;
    @NotNull(message = "contractSubStatus-contractSubStatus is mandatory;")
    private ContractDetailsSubStatus contractSubStatus;
    @NotNull(message = "contractVersionStatus-contractVersionStatus is mandatory;")
    private ProductContractVersionStatus contractVersionStatus;
}
