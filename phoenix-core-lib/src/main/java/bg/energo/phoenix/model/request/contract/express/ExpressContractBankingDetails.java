package bg.energo.phoenix.model.request.contract.express;

import bg.energo.phoenix.model.customAnotations.contract.express.ValidExpressContractBankingDetails;
import bg.energo.phoenix.model.customAnotations.customer.ValidIBAN;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
@ValidExpressContractBankingDetails
public class ExpressContractBankingDetails {
    private Boolean directDebit;

    private Long bankId;

    @ValidIBAN(errorMessageKey = "customer.bankingDetails.iban")
    private String iban;
}
