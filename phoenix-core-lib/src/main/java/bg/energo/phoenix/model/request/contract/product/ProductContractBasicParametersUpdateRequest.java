package bg.energo.phoenix.model.request.contract.product;

import bg.energo.phoenix.model.customAnotations.contract.products.ProductContractTerminationDateValidator;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;
@Data
@EqualsAndHashCode(callSuper = true)
@ProductContractTerminationDateValidator
public class ProductContractBasicParametersUpdateRequest extends ProductContractBasicParametersCreateRequest{

    private LocalDate terminationDate;
    private LocalDate perpetuityDate;
    private LocalDate contractTermEndDate;

}
