package bg.energo.phoenix.model.customAnotations.billing;

import bg.energo.phoenix.model.request.billing.companyDetails.CompanyDetailedParameterDTO;
import bg.energo.phoenix.model.request.billing.companyDetails.baseDTO.BaseCompanyDetailedParameterDTO;
import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;
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
@Constraint(validatedBy = {CompanyDetailedParameterValidator.CompanyDetailedParameterValidatorImpl.class,
        CompanyDetailedParameterValidator.CompanyDetailedParameterUpdateValidatorImpl.class})
public @interface CompanyDetailedParameterValidator {
    String fieldPath() default "";

    int minSize() default 1;

    int maxSize() default 1024;

    String regexPattern() default "";

    String value() default "";

    String message() default "";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    class CompanyDetailedParameterValidatorImpl implements ConstraintValidator<CompanyDetailedParameterValidator, List<BaseCompanyDetailedParameterDTO>> {
        private int minSize = 1;
        private int maxSize = 1024;
        private String fieldPath = "";
        private String regexPattern = "";

        @Override
        public void initialize(CompanyDetailedParameterValidator constraintAnnotation) {
            this.minSize = constraintAnnotation.minSize();
            this.maxSize = constraintAnnotation.maxSize();
            this.fieldPath = constraintAnnotation.fieldPath();
            this.regexPattern = constraintAnnotation.regexPattern();
            ConstraintValidator.super.initialize(constraintAnnotation);
        }

        @Override
        public boolean isValid(List<BaseCompanyDetailedParameterDTO> requests, ConstraintValidatorContext context) {
            StringBuilder errors = new StringBuilder();
            for (int i = 0; i < requests.size(); i++) {
                BaseCompanyDetailedParameterDTO request = requests.get(i);
                if (StringUtils.isBlank(request.getParameter())) {
                    errors.append("%s[%s].parameter-parameter is blank;".formatted(fieldPath, i));
                } else {
                    if (request.getParameter().length() < minSize) {
                        errors.append("%s[%s].parameter-parameter min length should be %s;".formatted(fieldPath, i, minSize));
                    }
                    if (request.getParameter().length() > maxSize) {
                        errors.append("%s[%s].parameter-parameter max length should be %s;".formatted(fieldPath, i, maxSize));
                    }
                    if (!regexPattern.isEmpty() && !request.getParameter().matches(regexPattern)) {
                        errors.append("%s[%s].parameter-parameter does not match regex;".formatted(fieldPath, i));
                    }
                }
                if (StringUtils.isBlank(request.getParameterTranslated())) {
                    errors.append("%s[%s].parameterTranslated-parameterTranslated is blank;".formatted(fieldPath, i));
                } else {
                    if (request.getParameterTranslated().length() < minSize) {
                        errors.append("%s[%s].parameterTranslated-parameterTranslated min length should be %s;".formatted(fieldPath, i, minSize));
                    }
                    if (request.getParameterTranslated().length() > maxSize) {
                        errors.append("%s[%s].parameterTranslated-parameterTranslated max length should be %s;".formatted(fieldPath, i, maxSize));
                    }

                    if (!regexPattern.isEmpty() && !request.getParameterTranslated().matches(regexPattern)) {
                        errors.append("%s[%s].parameterTranslated-parameterTranslated does not match regex;".formatted(fieldPath, i));
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
    class CompanyDetailedParameterUpdateValidatorImpl implements ConstraintValidator<CompanyDetailedParameterValidator, List<CompanyDetailedParameterDTO>> {
        private int minSize = 1;
        private int maxSize = 1024;
        private String fieldPath = "";
        private String regexPattern = "";

        @Override
        public void initialize(CompanyDetailedParameterValidator constraintAnnotation) {
            this.minSize = constraintAnnotation.minSize();
            this.maxSize = constraintAnnotation.maxSize();
            this.fieldPath = constraintAnnotation.fieldPath();
            this.regexPattern = constraintAnnotation.regexPattern();
            ConstraintValidator.super.initialize(constraintAnnotation);
        }

        @Override
        public boolean isValid(List<CompanyDetailedParameterDTO> requests, ConstraintValidatorContext context) {
            StringBuilder errors = new StringBuilder();
            for (int i = 0; i < requests.size(); i++) {
                BaseCompanyDetailedParameterDTO request = requests.get(i);
                if (StringUtils.isBlank(request.getParameter())) {
                    errors.append("%s[%s].parameter-parameter is blank;".formatted(fieldPath, i));
                } else {
                    if (request.getParameter().length() < minSize) {
                        errors.append("%s[%s].parameter-parameter min length should be %s;".formatted(fieldPath, i, minSize));
                    }
                    if (request.getParameter().length() > maxSize) {
                        errors.append("%s[%s].parameter-parameter max length should be %s;".formatted(fieldPath, i, maxSize));
                    }
                    if (!regexPattern.isEmpty() && !request.getParameter().matches(regexPattern)) {
                        errors.append("%s[%s].parameter-parameter does not match regex;".formatted(fieldPath, i));
                    }
                }
                if (StringUtils.isBlank(request.getParameterTranslated())) {
                    errors.append("%s[%s].parameterTranslated-parameterTranslated is blank;".formatted(fieldPath, i));
                } else {
                    if (request.getParameterTranslated().length() < minSize) {
                        errors.append("%s[%s].parameterTranslated-parameterTranslated min length should be %s;".formatted(fieldPath, i, minSize));
                    }
                    if (request.getParameterTranslated().length() > maxSize) {
                        errors.append("%s[%s].parameterTranslated-parameterTranslated max length should be %s;".formatted(fieldPath, i, maxSize));
                    }

                    if (!regexPattern.isEmpty() && !request.getParameterTranslated().matches(regexPattern)) {
                        errors.append("%s[%s].parameterTranslated-parameterTranslated does not match regex;".formatted(fieldPath, i));
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
