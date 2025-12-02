package bg.energo.phoenix.model.customAnotations.customer;

import bg.energo.phoenix.model.request.customer.communicationData.communicationContact.BaseCommunicationContactRequest;
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

import static java.lang.annotation.ElementType.TYPE;

@Target({TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Constraint(validatedBy = BaseCommunicationContractValidator.BaseCommunicationContractValidatorImpl.class)
public @interface BaseCommunicationContractValidator {
    String value() default "";

    String message() default "{value}-Invalid Format or symbols;";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    class BaseCommunicationContractValidatorImpl implements ConstraintValidator<BaseCommunicationContractValidator, BaseCommunicationContactRequest> {
        @Override
        public boolean isValid(BaseCommunicationContactRequest request, ConstraintValidatorContext context) {
            boolean isValid = true;
            StringBuilder validationMessageBuilder = new StringBuilder();

            if (request.getContactType() == null) {
                validationMessageBuilder.append("communicationData.communicationContacts.contactType-Contract Type is null");
                return false;
            }

            switch (request.getContactType()) {
                case MOBILE_NUMBER, LANDLINE_PHONE, CALL_CENTER, FAX -> {
                    if (request.getContactValue() != null) {
                        Pattern pattern = Pattern.compile("^[\\d\\-–+*]{1,32}$");
                        Matcher matcher = pattern.matcher(request.getContactValue());
                        if (!matcher.matches()) {
                            validationMessageBuilder.append(String.format("communicationData.communicationContacts.contactValue-Contract Value invalid format or symbols: [%s];", request.getContactValue()));
                        }
                    }
                }
                case EMAIL -> {
                    if (request.getContactValue() != null) {
                        Pattern pattern = Pattern.compile("^[A-Za-z\\d!@#$%&'*+\\-/=?^_`|{}~.]{1,256}$");
                        Matcher matcher = pattern.matcher(request.getContactValue());
                        if (!matcher.matches()) {
                            validationMessageBuilder.append(String.format("communicationData.communicationContacts.contactValue-Contract Value invalid format or symbols: [%s];", request.getContactValue()));
                        }
                    }
                }
                case WEBSITE, OTHER_PLATFORM -> {
                    if (request.getContactValue() != null) {
                        Pattern pattern = Pattern.compile("^[А-Яа-яA-Za-z\\d!@#$%&'*+\\-/=?^_`|{}~.()]{1,512}$");
                        Matcher matcher = pattern.matcher(request.getContactValue());
                        if (!matcher.matches()) {
                            validationMessageBuilder.append(String.format("communicationData.communicationContacts.contactValue-Contract Value invalid format or symbols: [%s];", request.getContactValue()));
                        }
                    }
                }
            }

            if (!validationMessageBuilder.isEmpty()) {
                context.buildConstraintViolationWithTemplate(validationMessageBuilder.toString()).addConstraintViolation();
                isValid = false;
            }

            return isValid;
        }
    }
}
