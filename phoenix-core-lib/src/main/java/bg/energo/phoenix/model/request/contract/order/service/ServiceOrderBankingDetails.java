package bg.energo.phoenix.model.request.contract.order.service;

import bg.energo.phoenix.model.customAnotations.contract.order.service.ValidServiceOrderBankingDetails;
import bg.energo.phoenix.model.customAnotations.customer.ValidIBAN;
import lombok.Data;

@Data
@ValidServiceOrderBankingDetails
public class ServiceOrderBankingDetails {

    private Boolean directDebit;

    private Long bankId;

    @ValidIBAN(errorMessageKey = "basicParameters.bankingDetails.iban")
    private String iban;

}
