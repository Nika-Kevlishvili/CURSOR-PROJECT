package bg.energo.phoenix.util.epb;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.apache.commons.collections4.CollectionUtils;

import java.util.Collections;
import java.util.List;
import java.util.Set;

public class EPBValidationUtils {

    /**
     * Validates the request object
     *
     * @param request the request object to be validated
     * @param <T>     the type of the request object
     * @return a list of validation errors or empty list if no errors
     */
    public static <T> List<String> validateRequest(T request) {
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            Validator validator = factory.getValidator();
            Set<ConstraintViolation<T>> violations = validator.validate(request);
            if (CollectionUtils.isNotEmpty(violations)) {
                return violations.stream()
                        .map(ConstraintViolation::getMessage)
                        .toList();
            } else {
                return Collections.emptyList();
            }
        }
    }

}
