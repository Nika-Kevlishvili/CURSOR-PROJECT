package bg.energo.phoenix.model.request.contract.product.additionalParameters;

import bg.energo.phoenix.model.customAnotations.contract.products.ValidProductContractBankingDetails;
import bg.energo.phoenix.model.customAnotations.customer.ValidIBAN;
import lombok.Data;

@Data
@ValidProductContractBankingDetails
public class ProductContractBankingDetails {

    private Boolean directDebit;

    private Long bankId;

    @ValidIBAN(errorMessageKey = "additionalParameters.bankingDetails.iban")
    private String iban;

}
