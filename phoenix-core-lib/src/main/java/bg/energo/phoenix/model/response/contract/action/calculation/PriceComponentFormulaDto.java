package bg.energo.phoenix.model.response.contract.action.calculation;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PriceComponentFormulaDto {
    private Long id;
    private String formula;
}
