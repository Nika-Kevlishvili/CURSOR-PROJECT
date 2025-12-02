package bg.energo.phoenix.model.customAnotations.customer.withValidators;

import bg.energo.phoenix.model.request.customer.CustomerBankingDetails;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.apache.commons.lang3.StringUtils;

/*
    validate banking details based on Direct debit is selected or not.
    If Direct Debit is selected Bank, BIC and IBAN must be provided.
    If Not selected must not be provided
 */
public class CustomerBankingDetailsValidatorImpl
        implements ConstraintValidator<CustomerBankingDetailsValidator, CustomerBankingDetails> {
    @Override
    public boolean isValid(CustomerBankingDetails customerBankingDetails,
                           ConstraintValidatorContext context) {

        context.disableDefaultConstraintViolation();
        if(customerBankingDetails.getDirectDebit() == null){
            context.buildConstraintViolationWithTemplate("bankingDetails.directDebit-Direct debit is required;")
                    .addConstraintViolation();
            return false;
        }
        boolean correct = true;
        StringBuilder stringBuilder = new StringBuilder();
        if(customerBankingDetails.getDirectDebit()){
            if(customerBankingDetails.getBankId() == null){
                stringBuilder.append("bankingDetails.bankId-Bank is required when direct debit is selected;");
                correct = false;
            }

            if(StringUtils.isEmpty(customerBankingDetails.getIban())){
                stringBuilder.append("bankingDetails.iban-IBAN is required when direct debit is selected;");
                correct = false;
            }

        }

        context.buildConstraintViolationWithTemplate(stringBuilder.toString()).addConstraintViolation();
        return correct;
    }
}
