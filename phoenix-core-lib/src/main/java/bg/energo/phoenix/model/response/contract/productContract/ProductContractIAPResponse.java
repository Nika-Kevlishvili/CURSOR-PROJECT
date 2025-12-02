package bg.energo.phoenix.model.response.contract.productContract;

import bg.energo.phoenix.model.response.interimAdvancePayment.InterimAdvancePaymentTermsResponse;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class ProductContractIAPResponse {
    private Long contractIapId;
    private Long interimAdvancePaymentId;
    private String name;
    private Integer issueDate;
    private BigDecimal value;
    private Integer termValue;
    private InterimAdvancePaymentTermsResponse iapTerms;
    private List<ContractPriceComponentResponse> formulas;
}
