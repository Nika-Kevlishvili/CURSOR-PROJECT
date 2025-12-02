package bg.energo.phoenix.model.request.receivable.customerAssessment;

import bg.energo.phoenix.model.enums.receivable.customerAssessment.Assessment;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CustomerAssessmentParametersRequest {

    @NotNull(message = "parameterConditionId-parameterConditionId can not be null;")
    private Long parameterCriteriaId;

    @Size(min = 1, max = 16, message = "parameterValue-value must be between {min} and {max} characters;")
    private String parameterValue;

    @NotNull(message = "parameterAssessment-parameterAssessment can not be null;")
    private Assessment parameterAssessment;

    @NotNull(message = "parameterFinalAssessment-parameterFinalAssessment can not be null;")
    private Assessment parameterFinalAssessment;

}
