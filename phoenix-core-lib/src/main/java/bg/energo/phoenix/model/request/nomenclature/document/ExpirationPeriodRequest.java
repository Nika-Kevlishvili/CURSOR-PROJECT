package bg.energo.phoenix.model.request.nomenclature.document;

import jakarta.validation.constraints.NotNull;
import org.hibernate.validator.constraints.Range;

public record ExpirationPeriodRequest(
        @NotNull(message = "numberOfMonths-Number of Months must not be null;")
        @Range(min = 1, max = 9999, message = "numberOfMonths-Number of months should be between {min} and {max}!;")
        Integer numberOfMonths
) {
}
