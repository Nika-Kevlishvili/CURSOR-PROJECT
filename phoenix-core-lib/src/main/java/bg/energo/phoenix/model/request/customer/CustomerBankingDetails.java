package bg.energo.phoenix.model.request.customer;

import bg.energo.phoenix.model.customAnotations.customer.CustomerDeclaredConsumptionValidator;
import bg.energo.phoenix.model.customAnotations.customer.ValidIBAN;
import bg.energo.phoenix.model.customAnotations.customer.withValidators.CustomerBankingDetailsValidator;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
@CustomerBankingDetailsValidator
public class CustomerBankingDetails {

    @NotNull(message = "bankingDetails.directDebit-Direct Debit is required;")
    private Boolean directDebit;

    private Long bankId;

    private String bic;

    @ValidIBAN(errorMessageKey = "bankingDetails.iban")
    private String iban;

    @CustomerDeclaredConsumptionValidator
    private String declaredConsumption;

    private List<Long> preferenceIds;

    private Long creditRatingId;

}
