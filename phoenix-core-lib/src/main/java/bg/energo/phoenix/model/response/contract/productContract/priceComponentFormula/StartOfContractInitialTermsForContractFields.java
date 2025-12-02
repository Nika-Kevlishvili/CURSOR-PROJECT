package bg.energo.phoenix.model.response.contract.productContract.priceComponentFormula;

import bg.energo.phoenix.model.enums.product.term.terms.StartOfContractInitialTerm;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class StartOfContractInitialTermsForContractFields {
    private Integer startDayOfInitialContractTerm;
    private Integer firstDayOfTheMonthOfInitialContractTerm;
    private List<StartOfContractInitialTerm> startsOfContractInitialTerms;
}
