package phoenix.core.customer.model.request;

import lombok.Data;
import phoenix.core.customer.model.customAnotations.customer.CustomerDeclaredConsumptionValidator;
import phoenix.core.customer.model.customAnotations.customer.IBANValidator;
import phoenix.core.customer.model.customAnotations.customer.withValidators.CustomerBankingDetailsValidator;

import javax.validation.constraints.NotNull;
import java.util.List;

@Data
@CustomerBankingDetailsValidator
public class CustomerBankingDetails {
    @NotNull(message = "Direct Debit is required; ")
    private Boolean directDebit;

    private Long bankId;

    private String bic;

    @IBANValidator
    private String iban;

    @CustomerDeclaredConsumptionValidator
    private String declaredConsumption;

    private List<Long> preferenceIds;

    private Long creditRatingId;

}
