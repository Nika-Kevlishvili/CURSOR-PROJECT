package bg.energo.phoenix.model.request.nomenclature.receivable;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CustomerAssessmentTypeFilterRequest {

    @NotNull(message = "typeId-typeId must not be null")
    private Long typeId;

}
