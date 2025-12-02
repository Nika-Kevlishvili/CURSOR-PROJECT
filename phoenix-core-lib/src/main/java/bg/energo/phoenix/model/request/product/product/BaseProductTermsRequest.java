package bg.energo.phoenix.model.request.product.product;

import bg.energo.phoenix.model.customAnotations.product.product.ProductContractTermValidator;
import bg.energo.phoenix.model.enums.product.product.ProductContractTermRenewalType;
import bg.energo.phoenix.model.enums.product.product.ProductTermPeriodType;
import bg.energo.phoenix.model.enums.product.product.ProductTermType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@ProductContractTermValidator
@AllArgsConstructor
@NoArgsConstructor
public class BaseProductTermsRequest {
    private Long id;

    @NotNull(message = "basicSettings.productTerms.name-name can not be null;")
    private String name;

    private boolean perpetuityCause;

    // TODO: 9/15/23 typeOfTerms and periodType fields should switch names

    @NotNull(message = "basicSettings.productTerms.typeOfTerms-typeOfTerms can not be null;")
    private ProductTermPeriodType typeOfTerms;

    private ProductTermType periodType;

    @Min(value = 1, message = "basicSettings.productTerms.value-value should be between 1-9999;")
    @Max(value = 9999, message = "basicSettings.productTerms.value-value should be between 1-9999;")
    private Integer value;

    private Boolean automaticRenewal;

    @Min(value = 1, message = "basicSettings.productTerms.numberOfRenewals-Number of renewals should be between 1-999")
    @Max(value = 9999, message = "basicSettings.productTerms.numberOfRenewals-Number of renewals should be between 1-999")
    private Integer numberOfRenewals;

    @Min(value = 1, message = "basicSettings.productTerms.numberOfRenewals-Number of renewals period should be between 1-9999;")
    @Max(value = 9999, message = "basicSettings.productTerms.numberOfRenewals-Number of renewals period should be between 1-9999;")
    private Integer renewalPeriodValue;

    private ProductContractTermRenewalType renewalPeriodType;
}
