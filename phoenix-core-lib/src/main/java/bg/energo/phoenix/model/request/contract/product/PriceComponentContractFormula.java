package bg.energo.phoenix.model.request.contract.product;

import bg.energo.phoenix.model.request.product.price.priceComponent.PriceComponentProjectionForIap;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PriceComponentContractFormula {
    @NotNull(message = "productParameters.interimAdvancePayments.contractFormulas.serviceParameters.formulaVariableId-formulaVariableId can not be null;")
    private Long formulaVariableId;
    @DecimalMin(value = "0",message = "productParameters.interimAdvancePayments.contractFormulas.serviceParameters.value-[value] can not be less than 0;")
    @DecimalMax(value = "999999999.99999",message = "productParameters.interimAdvancePayments.contractFormulas.serviceParameters.value-[value] can not be greater than 99999999.99;")
    private BigDecimal value;



    public PriceComponentContractFormula(PriceComponentProjectionForIap priceComponentProjectionForIap) {
        this.formulaVariableId = priceComponentProjectionForIap.getId();
        this.value = priceComponentProjectionForIap.getValue();
    }
}
