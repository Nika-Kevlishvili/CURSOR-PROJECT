package bg.energo.phoenix.model.request.contract.order.service;

import bg.energo.phoenix.model.customAnotations.DuplicatedValuesValidator;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
public class ServiceOrderServiceParametersRequest {

    private Long contractTermId;

    private LocalDate contractTermCertainDateValue;

    //@NotNull(message = "serviceParameters.invoicePaymentTermId-Invoice payment term is mandatory;")
    private Long invoicePaymentTermId;

    //@NotNull(message = "serviceParameters.invoicePaymentTermValue-Invoice payment term value is mandatory;")
    private Integer invoicePaymentTermValue;

    private List<@Valid ServiceOrderFormulaVariableRequest> formulaVariables;

    @Min(value = 1, message = "serviceParameters.quantity-Value must be between 1 and 9999 characters;")
    @Max(value = 9999, message = "serviceParameters.quantity-Value must be between 1 and 9999 characters;")
    private Integer quantity;

    @DuplicatedValuesValidator(fieldPath = "serviceParameters.pods")
    private List<Long> pods;

    @DuplicatedValuesValidator(fieldPath = "serviceParameters.unrecognizedPods")
    private List<String> unrecognizedPods;

    private List<@Valid ServiceOrderLinkedContractRequest> linkedContracts;

}
