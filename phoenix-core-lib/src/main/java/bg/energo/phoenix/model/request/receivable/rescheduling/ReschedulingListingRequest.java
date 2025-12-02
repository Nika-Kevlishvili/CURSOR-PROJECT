package bg.energo.phoenix.model.request.receivable.rescheduling;

import bg.energo.phoenix.model.customAnotations.DateRangeValidator;
import bg.energo.phoenix.model.customAnotations.receivable.rescheduling.ValidReschedulingListingRequest;
import bg.energo.phoenix.model.entity.EntityStatus;
import bg.energo.phoenix.model.enums.receivable.rescheduling.ReschedulingStatus;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@ValidReschedulingListingRequest
public class ReschedulingListingRequest {

    @NotNull(message = "page-Page must not be null;")
    private Integer page;

    @NotNull(message = "size-Size must not be null;")
    private Integer size;

    private String prompt;

    private ReschedulingSearchFields searchFields;

    @Digits(integer = 10, fraction = 0, message = "numberOfInstallmentFrom must be a whole number;")
    @Min(value = 1, message = "numberOfInstallmentFrom must be in the 1-9999 range;")
    @Max(value = 9999, message = "numberOfInstallmentFrom must be in the 1-9999 range;")
    private BigDecimal numberOfInstallmentFrom;

    @Digits(integer = 10, fraction = 0, message = "numberOfInstallmentTo must be a whole number;")
    @Min(value = 1, message = "numberOfInstallmentTo must be in the 1-9999 range;")
    @Max(value = 9999, message = "numberOfInstallmentTo must be in the 1-9999 range;")
    private BigDecimal numberOfInstallmentTo;

    @Digits(integer = 2, fraction = 0, message = "installmentDueDayFrom must be a whole number;")
    @Min(value = 1, message = "installmentDueDayFrom must be between 1 and 31;")
    @Max(value = 31, message = "installmentDueDayFrom must be between 1 and 31;")
    private BigDecimal installmentDueDayFrom;

    @Digits(integer = 2, fraction = 0, message = "installmentDueDayTo must be a whole number;")
    @Min(value = 1, message = "installmentDueDayTo must be between 1 and 31;")
    @Max(value = 31, message = "installmentDueDayTo must be between 1 and 31;")
    private BigDecimal installmentDueDayTo;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    @DateRangeValidator(fieldPath = "createDateFrom", fromDate = "1990-01-01", toDate = "2090-12-31")
    private LocalDate createDateFrom;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    @DateRangeValidator(fieldPath = "createDateTo", fromDate = "1990-01-01", toDate = "2090-12-31")
    private LocalDate createDateTo;

    private ReschedulingListColumns columns;

    private Sort.Direction direction;

    private  List<EntityStatus> statuses;

    private List<ReschedulingStatus> reschedulingStatuses;

    private List<Boolean> reverseStatuses;

}
