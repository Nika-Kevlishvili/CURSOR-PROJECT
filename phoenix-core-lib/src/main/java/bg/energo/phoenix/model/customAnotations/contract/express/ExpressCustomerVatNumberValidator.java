package bg.energo.phoenix.model.customAnotations.contract.express;

import bg.energo.phoenix.model.request.contract.express.ExpressContractCustomerRequest;
import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.ElementType.TYPE;

@Target({TYPE, PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Constraint(validatedBy = {ExpressCustomerVatNumberValidator.VatNumberValidatorImpl.class})
public @interface ExpressCustomerVatNumberValidator {
    String message() default "{}";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    class VatNumberValidatorImpl
            implements ConstraintValidator<ExpressCustomerVatNumberValidator, ExpressContractCustomerRequest> {
        @Override
        public boolean isValid(ExpressContractCustomerRequest request, ConstraintValidatorContext context) {
            StringBuilder stringBuilder = new StringBuilder();
            if (request.getCustomerType() == null) {
                return false;
            }
            context.disableDefaultConstraintViolation();
            switch (request.getCustomerType()) {
                case LEGAL_ENTITY -> {
                    if (request.getVatNumber() != null) {
                        if (request.isForeign()) {
                            Pattern pattern = Pattern.compile("^[A-Z–\\-./\\d]{5,15}$");
                            Matcher matcher = pattern.matcher(request.getVatNumber());
                            if (!matcher.matches()) {
                                stringBuilder.append("customer.vatNumber-VAT number invalid format or symbols;");
                            }
                        } else {
                            Pattern pattern = Pattern.compile("^(BG\\d{9}|BG\\d{13})$");
                            Matcher matcher = pattern.matcher(request.getVatNumber());
                            if (!matcher.matches()) {
                                stringBuilder.append("customer.vatNumber-VAT number invalid format or symbols;");
                            }
                        }
                    }
                }
                case PRIVATE_CUSTOMER -> {
                    if (request.getBusinessActivity() != null && request.getBusinessActivity()) {
                        if (request.getVatNumber() != null) {
                            if (request.isForeign()) {
                                Pattern pattern = Pattern.compile("^[A-Z–\\-.\\d]{5,15}$");
                                Matcher matcher = pattern.matcher(request.getVatNumber());
                                if (!matcher.matches()) {
                                    stringBuilder.append("customer.vatNumber-VAT number invalid format or symbols;");
                                }
                            } else {
                                Pattern pattern;
                                if (request.isForeign()) {
                                    pattern = Pattern.compile("^[A-Z0-9]{5,15}$");
                                } else {
                                    pattern = Pattern.compile("^(BG\\d{10})$");
                                }

                                Matcher matcher = pattern.matcher(request.getVatNumber());
                                if (!matcher.matches()) {
                                    stringBuilder.append("customer.vatNumber-VAT number invalid format or symbols;");
                                }
                            }
                        }
                    } else if (request.getVatNumber() != null) {
                        stringBuilder.append("customer.vatNumber-Vat number should not be provided;");
                    }
                }
            }

            context.buildConstraintViolationWithTemplate(stringBuilder.toString()).addConstraintViolation();
            return stringBuilder.isEmpty();
        }
    }

}
