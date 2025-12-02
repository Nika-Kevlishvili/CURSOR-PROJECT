package bg.energo.phoenix.model.request.product.service.subObject.contractTerm;

import bg.energo.phoenix.model.customAnotations.product.service.ValidServiceContractTerm;
import bg.energo.phoenix.model.enums.product.service.ServiceContractTermPeriodType;
import bg.energo.phoenix.model.enums.product.service.ServiceContractTermRenewalType;
import bg.energo.phoenix.model.enums.product.service.ServiceContractTermType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ValidServiceContractTerm
public class BaseServiceContractTermRequest {

    @NotNull(message = "contractTerms.name-name can not be null;")
    private String name;

    private Boolean perpetuityCause;

    @NotNull(message = "contractTerms.periodType-Period type can not be null;")
    private ServiceContractTermPeriodType periodType;

    private ServiceContractTermType termType;

    @Min(value = 1, message = "contractTerms.value-value should be between 1-9999;")
    @Max(value = 9999, message = "contractTerms.value-value should be between 1-9999;")
    private Integer value;

    private Boolean automaticRenewal;

    @Min(value = 1, message = "contractTerms.numberOfRenewals-Number of renewals should be between 1-999;")
    @Max(value = 9999, message = "contractTerms.numberOfRenewals-Number of renewals should be between 1-999;")
    private Integer numberOfRenewals;

    @Min(value = 1, message = "contractTerms.renewalPeriodValue-Number of renewals period should be between 1-9999;")
    @Max(value = 9999, message = "contractTerms.renewalPeriodValue-Number of renewals period should be between 1-9999;")
    private Integer renewalPeriodValue;

    private ServiceContractTermRenewalType renewalPeriodType;

}
