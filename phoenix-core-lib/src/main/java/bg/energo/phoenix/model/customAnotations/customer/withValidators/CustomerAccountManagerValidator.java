package bg.energo.phoenix.model.customAnotations.customer.withValidators;

import bg.energo.phoenix.model.request.customer.customerAccountManager.BaseCustomerAccountManagerRequest;
import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;
import org.apache.commons.collections4.CollectionUtils;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static java.lang.annotation.ElementType.FIELD;

@Target({FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Constraint(validatedBy = CustomerAccountManagerValidator.CustomerAccountManagerValidatorImpl.class)
public @interface CustomerAccountManagerValidator {
    String message() default "";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    class CustomerAccountManagerValidatorImpl implements ConstraintValidator<CustomerAccountManagerValidator, List<? extends BaseCustomerAccountManagerRequest>> {
        @Override
        public boolean isValid(List<? extends BaseCustomerAccountManagerRequest> accountManagerRequests, ConstraintValidatorContext context) {
            if (CollectionUtils.isEmpty(accountManagerRequests)) {
                return true;
            }

            boolean isValid = true;
            StringBuilder exceptionMessageBuilder = new StringBuilder();

            Set<Long> usedIds = new HashSet<>();
            for (int i = 0; i < accountManagerRequests.size(); i++) {
                BaseCustomerAccountManagerRequest accountManagerRequest = accountManagerRequests.get(i);
                if (usedIds.contains(accountManagerRequest.getAccountManagerId())) {
                    exceptionMessageBuilder.append("accountManagers[%s].accountManagerId-Account Manager ID duplicated;".formatted(i));
                } else {
                    usedIds.add(accountManagerRequest.getAccountManagerId());
                }

                if (accountManagerRequest.getAccountManagerId() == null) {
                    exceptionMessageBuilder.append("accountManagers[%s].accountManagerId-Account Manager ID Must not be null;".formatted(i));
                }

                if (accountManagerRequest.getAccountManagerTypeId() == null) {
                    exceptionMessageBuilder.append("accountManagers[%s].accountManagerTypeId-Account Manager Type ID Must not be null;".formatted(i));
                }
            }

            if (!exceptionMessageBuilder.isEmpty()) {
                isValid = false;
                context.buildConstraintViolationWithTemplate(exceptionMessageBuilder.toString()).addConstraintViolation();
            }

            return isValid;
        }
    }
}
