package bg.energo.phoenix.model.request.contract.service;

import bg.energo.phoenix.model.customAnotations.customer.ValidIBAN;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ServiceContractBankingDetails {
    private Boolean directDebit;

    private Long bankId;

    @ValidIBAN(errorMessageKey = "additionalParameters.bankingDetails.iban")
    private String iban;
}
