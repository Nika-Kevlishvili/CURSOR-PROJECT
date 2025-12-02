package bg.energo.phoenix.model.request.contract.action;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PenaltyFormulaExpressionHolder {
    private String statement;
}
