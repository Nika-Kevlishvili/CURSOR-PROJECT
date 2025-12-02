package bg.energo.phoenix.model.response.contract.serviceContract;

import bg.energo.phoenix.model.response.interimAdvancePayment.InterimAdvancePaymentTermsResponse;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class ServiceContractIAPResponse {
    private Long contractIapId;
    private Long interimAdvancePaymentId;
    private String name;
    private Integer issueDate;
    private BigDecimal value;
    private Integer termValue;
    private InterimAdvancePaymentTermsResponse iapTerms;
    private List<ServiceContractPriceComponentsResponse> formulas;
}
