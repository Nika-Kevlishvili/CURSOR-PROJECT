package phoenix.core.customer.model.customAnotations.customer.withValidators;

import phoenix.core.customer.model.request.CustomerBankingDetails;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

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
            context.buildConstraintViolationWithTemplate("Direct debit is required; ").addConstraintViolation();
            return false;
        }
        boolean correct = true;
        StringBuilder stringBuilder = new StringBuilder();
        if(customerBankingDetails.getDirectDebit()){
            if(customerBankingDetails.getBankId() == null){
                stringBuilder.append("Bank is required; ");
                correct = false;
            }

            if(customerBankingDetails.getBic() == null){
                stringBuilder.append("BIC is required; ");
                correct = false;
            }

            if(customerBankingDetails.getIban() == null){
                stringBuilder.append("IBAN is required; ");
                correct = false;
            }

        }else{

            if(customerBankingDetails.getBankId() != null){
                stringBuilder.append("Bank must not be provided; ");
                correct = false;
            }

            if(customerBankingDetails.getBic() != null){
                stringBuilder.append("BIC must not be provided; ");
                correct = false;
            }

            if(customerBankingDetails.getIban() != null){
                stringBuilder.append("IBAN must not be provided; ");
                correct = false;
            }
        }
        context.buildConstraintViolationWithTemplate(stringBuilder.toString()).addConstraintViolation();
        return correct;
    }
}
