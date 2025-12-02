package bg.energo.phoenix.model.response.contract.productContract.priceComponentFormula;

import bg.energo.phoenix.model.enums.product.term.terms.SupplyActivation;
import bg.energo.phoenix.model.enums.product.term.terms.WaitForOldContractTermToExpire;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class SupplyActivationsForContractFields {
    private Integer supplyActivationExactDateStartDay;
    private List<SupplyActivation> supplyActivations;
    private  List<WaitForOldContractTermToExpire> waitForOldContractTermToExpires;
}
