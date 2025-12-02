package bg.energo.phoenix.model.request.receivable.latePaymentFine;

import bg.energo.phoenix.model.customAnotations.DateRangeValidator;
import bg.energo.phoenix.model.enums.receivable.latePaymentFine.LatePaymentFineReversed;
import bg.energo.phoenix.model.enums.receivable.latePaymentFine.LatePaymentFineSearchBy;
import bg.energo.phoenix.model.enums.receivable.latePaymentFine.LatePaymentFineSortingType;
import bg.energo.phoenix.model.enums.receivable.latePaymentFine.LatePaymentFineType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
public class LatePaymentFineListingRequest {

    @NotNull(message = "page-Page must not be null;")
    private Integer page;

    @NotNull(message = "size-Size must not be null;")
    private Integer size;

    private String prompt;

    private LatePaymentFineSearchBy searchBy;

    private List<LatePaymentFineType> type;

    private List<LatePaymentFineReversed> reversed;

    @DecimalMin(value = "0.00", message = "amountFrom-[amountFrom] Minimum value is 0;")
    @Digits(integer = 32, fraction = 5, message = "amountFrom-[amountFrom] Maximum value is 32 numbers;")
    private BigDecimal totalAmountFrom;

    @DecimalMin(value = "0.00", message = "amountTo-[amountTo] Minimum value is 0;")
    @Digits(integer = 32, fraction = 5, message = "amountTo-[amountTo] Maximum value is 32 numbers;")
    private BigDecimal totalAmountTo;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    @DateRangeValidator(fieldPath = "dateFrom", fromDate = "1990-01-01", toDate = "2090-12-31")
    private LocalDate dateFrom;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    @DateRangeValidator(fieldPath = "dateTo", fromDate = "1990-01-01", toDate = "2090-12-31")
    private LocalDate dateTo;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    @DateRangeValidator(fieldPath = "dueDateFrom", fromDate = "1990-01-01", toDate = "2090-12-31")
    private LocalDate dueDateFrom;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    @DateRangeValidator(fieldPath = "dueDateTo", fromDate = "1990-01-01", toDate = "2090-12-31")
    private LocalDate dueDateTo;

    private List<Long> currencyIds;

    private LatePaymentFineSortingType sortingType;

    private Sort.Direction direction;
}
