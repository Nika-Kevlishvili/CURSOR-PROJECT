package bg.energo.phoenix.model.customAnotations.billing;

import bg.energo.phoenix.model.request.billing.companyDetails.CompanyCommunicationChannelDTO;
import bg.energo.phoenix.model.request.billing.companyDetails.baseDTO.BaseCompanyCommunicationChannelDTO;
import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.List;

import static java.lang.annotation.ElementType.FIELD;

@Target({FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Constraint(validatedBy = {CompanyCommunicationChannelValidator.CompanyCommunicationChannelValidatorImpl.class,
                           CompanyCommunicationChannelValidator.CompanyCommunicationChannelUpdateValidatorImpl.class})
public @interface CompanyCommunicationChannelValidator {
    String fieldPath() default "";

    int minSize() default 1;

    int maxSize() default 512;

    String regexPattern() default "";

    String value() default "";

    String message() default "";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    class CompanyCommunicationChannelValidatorImpl implements ConstraintValidator<CompanyCommunicationChannelValidator, List<BaseCompanyCommunicationChannelDTO>> {
        private String fieldPath = "";
        private String regexPattern = "";
        private int minSize = 1;
        private int maxSize = 512;

        @Override
        public void initialize(CompanyCommunicationChannelValidator constraintAnnotation) {
            this.fieldPath = constraintAnnotation.fieldPath();
            this.regexPattern = constraintAnnotation.regexPattern();
            this.minSize = constraintAnnotation.minSize();
            this.maxSize = constraintAnnotation.maxSize();
            ConstraintValidator.super.initialize(constraintAnnotation);
        }

        @Override
        public boolean isValid(List<BaseCompanyCommunicationChannelDTO> requests, ConstraintValidatorContext context) {
            StringBuilder errors = new StringBuilder();
            for (int i = 0; i < requests.size(); i++) {
                BaseCompanyCommunicationChannelDTO request = requests.get(i);
                if (StringUtils.isBlank(request.getCommunicationChannel())) {
                    errors.append("%s[%s].communicationChannel-communicationChannel is blank;".formatted(fieldPath, i));
                } else {
                    if (request.getCommunicationChannel().length() < minSize) {
                        errors.append("%s[%s].communicationChannel-communicationChannel min length should be %s;".formatted(fieldPath, i, minSize));
                    }
                    if (request.getCommunicationChannel().length() > maxSize) {
                        errors.append("%s[%s].communicationChannel-communicationChannel max length should be %s;".formatted(fieldPath, i, maxSize));
                    }
                    if (!regexPattern.isEmpty() && !request.getCommunicationChannel().matches(regexPattern)) {
                        errors.append("%s[%s].communicationChannel-communicationChannel does not match regex;".formatted(fieldPath, i));
                    }
                }
            }
            if (!errors.isEmpty()) {
                context.buildConstraintViolationWithTemplate(errors.toString()).addConstraintViolation();
                return false;
            }

            return true;
        }
    }

    class CompanyCommunicationChannelUpdateValidatorImpl implements ConstraintValidator<CompanyCommunicationChannelValidator, List<CompanyCommunicationChannelDTO>> {
        private String fieldPath = "";
        private String regexPattern = "";
        private int minSize = 1;
        private int maxSize = 512;

        @Override
        public void initialize(CompanyCommunicationChannelValidator constraintAnnotation) {
            this.fieldPath = constraintAnnotation.fieldPath();
            this.regexPattern = constraintAnnotation.regexPattern();
            this.minSize = constraintAnnotation.minSize();
            this.maxSize = constraintAnnotation.maxSize();
            ConstraintValidator.super.initialize(constraintAnnotation);
        }

        @Override
        public boolean isValid(List<CompanyCommunicationChannelDTO> requests, ConstraintValidatorContext context) {
            StringBuilder errors = new StringBuilder();
            if (CollectionUtils.isEmpty(requests)) {
                errors.append("%s-at least 1 record is required;".formatted(fieldPath));
                return false;
            }
            for (int i = 0; i < requests.size(); i++) {
                BaseCompanyCommunicationChannelDTO request = requests.get(i);
                if (StringUtils.isBlank(request.getCommunicationChannel())) {
                    errors.append("%s[%s].communicationChannel-communicationChannel is blank;".formatted(fieldPath, i));
                } else {
                    if (request.getCommunicationChannel().length() < minSize) {
                        errors.append("%s[%s].communicationChannel-communicationChannel min length should be %s;".formatted(fieldPath, i, minSize));
                    }
                    if (request.getCommunicationChannel().length() > maxSize) {
                        errors.append("%s[%s].communicationChannel-communicationChannel max length should be %s;".formatted(fieldPath, i, maxSize));
                    }
                    if (!regexPattern.isEmpty() && !request.getCommunicationChannel().matches(regexPattern)) {
                        errors.append("%s[%s].communicationChannel-communicationChannel does not match regex;".formatted(fieldPath, i));
                    }
                }
            }
            if (!errors.isEmpty()) {
                context.buildConstraintViolationWithTemplate(errors.toString()).addConstraintViolation();
                return false;
            }

            return true;
        }
    }

}
