package bg.energo.phoenix.model.request.receivable.customerAssessment;

import bg.energo.phoenix.model.customAnotations.DuplicatedValuesValidator;
import bg.energo.phoenix.model.enums.receivable.customerAssessment.Assessment;
import bg.energo.phoenix.model.enums.receivable.customerAssessment.AssessmentStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class CustomerAssessmentBaseRequest {

    @NotNull(message = "finalAssessment-finalAssessment can not be null;")
    private Assessment finalAssessment;

    @NotNull(message = "typeId-typeId can not be null;")
    private Long typeId;

    private List<Long> additionalConditions;

    @NotNull(message = "parameters-parameters can not be null;")
    @Size(min = 9, max = 9, message = "parameters-parameters must have exactly 9 elements;")
    private @Valid List<CustomerAssessmentParametersRequest> parameters;

    @Size(min = 1, max = 2048, message = "newComment-new comment must be between {min} and {max} characters;")
    private String newComment;

    @NotNull(message = "customerId-customer can not be null;")
    private Long customerId;

    @DuplicatedValuesValidator(fieldPath = "files")
    private List<Long> files;

    @NotNull(message = "status-status can not be null;")
    private AssessmentStatus status;

    public List<Long> taskIds;
}
