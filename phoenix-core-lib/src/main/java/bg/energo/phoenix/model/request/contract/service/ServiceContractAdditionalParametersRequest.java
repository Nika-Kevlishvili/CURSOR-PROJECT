package bg.energo.phoenix.model.request.contract.service;

import bg.energo.phoenix.model.customAnotations.DuplicatedValuesValidator;
import bg.energo.phoenix.model.customAnotations.contract.service.ServiceContractBankDetailsValidator;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ServiceContractAdditionalParametersRequest {

    @Valid
    @ServiceContractBankDetailsValidator
    private ServiceContractBankingDetails bankingDetails;

    @NotNull(message = "additionalParameters.interestRateId-Interest Rate is mandatory;")
    private Long interestRateId;

   // @NotNull(message = "additionalParameters.campaignId-Campaign is mandatory;")
    private Long campaignId;

    @DuplicatedValuesValidator(fieldPath = "additionalParameters.assistingEmployees")
    private List<Long> assistingEmployees;

    @DuplicatedValuesValidator(fieldPath = "additionalParameters.internalIntermediaries")
    private List<Long> internalIntermediaries;

    @DuplicatedValuesValidator(fieldPath = "additionalParameters.externalIntermediaries")
    private List<Long> externalIntermediaries;

    private Long employeeId;

}
