package bg.energo.phoenix.model.response.contract.order.service;

import bg.energo.phoenix.model.response.contract.serviceContract.ServiceContractTermsResponse;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ServiceOrderServiceParametersResponse {

    private ServiceOrderServiceParametersFields serviceParametersFields;
    private ServiceContractTermsResponse contractTerm;
    private LocalDate contractTermCertainDateValue;

    private Long invoicePaymentTermId;
    private Integer invoicePaymentTermValue;
    private String invoicePaymentTermName;
    private List<ServiceOrderFormulaVariableResponse> formulaVariables;

    private Integer quantity;

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private List<ServiceOrderLinkedContractShortResponse> linkedContracts;

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private List<ServiceOrderSubObjectShortResponse> pods;

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private List<ServiceOrderSubObjectShortResponse> unrecognizedPods;

}
