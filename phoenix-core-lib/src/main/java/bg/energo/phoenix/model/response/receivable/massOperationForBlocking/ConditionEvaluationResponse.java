package bg.energo.phoenix.model.response.receivable.massOperationForBlocking;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ConditionEvaluationResponse {
    private List<Long> paymentIds;

    private List<Long> receivableIds;

    private List<Long> liabilitiesIds;
}
