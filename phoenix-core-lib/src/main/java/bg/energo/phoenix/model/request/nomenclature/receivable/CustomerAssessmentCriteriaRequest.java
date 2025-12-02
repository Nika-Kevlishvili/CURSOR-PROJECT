package bg.energo.phoenix.model.request.nomenclature.receivable;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CustomerAssessmentCriteriaRequest {

    @NotNull(message = "criteriaTypeList-criteriaTypeList must not be null")
    private @Valid List<CustomerAssessmentCriteriaTypeRequest> criteriaTypeList;

}
