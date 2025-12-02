package bg.energo.phoenix.model.response.contract.productContract.priceComponentFormula;

import bg.energo.phoenix.model.enums.product.term.terms.ContractEntryIntoForce;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class ContractEntryIntoForceForContractFields {
    private Integer contractEntryIntoForceFromExactDayOfMonthStartDay;
    private List<ContractEntryIntoForce> contractEntryIntoForces;
}
