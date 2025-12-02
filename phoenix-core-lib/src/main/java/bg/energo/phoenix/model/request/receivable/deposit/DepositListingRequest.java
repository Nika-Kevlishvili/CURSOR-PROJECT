package bg.energo.phoenix.model.request.receivable.deposit;

import bg.energo.phoenix.model.customAnotations.DateRangeValidator;
import bg.energo.phoenix.model.enums.receivable.deposit.DepositListingSortingType;
import bg.energo.phoenix.model.enums.receivable.deposit.DepositListingType;
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
public class DepositListingRequest {

    @NotNull(message = "page-Page must not be null;")
    private Integer page;

    @NotNull(message = "size-Size must not be null;")
    private Integer size;

    private String prompt;

    private DepositListingType depositListingType;

    private List<Long> currencyIds;


    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    @DateRangeValidator(fieldPath = "paymentDeadlineFrom", fromDate = "1990-01-01", toDate = "2090-12-31")
    private LocalDate paymentDeadlineFrom;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    @DateRangeValidator(fieldPath = "paymentDeadlineTo", fromDate = "1990-01-01", toDate = "2090-12-31")
    private LocalDate paymentDeadlineTo;

    @DecimalMin(value = "0.00", message = "initialAmountFrom-[initialAmountFrom] Minimum value is 0;")
    @Digits(integer = 32, fraction = 5, message = "initialAmountFrom-[initialAmountFrom] Maximum value is 32 numbers;")
    private BigDecimal initialAmountFrom;

    @DecimalMin(value = "0.00", message = "initialAmount-[initialAmount] Minimum value is 0;")
    @Digits(integer = 32, fraction = 5, message = "initialAmount-[initialAmount] Maximum value is 32 numbers;")
    private BigDecimal initialAmountTo;

    @DecimalMin(value = "0.00", message = "currentAmountFrom-[currentAmountFrom] Minimum value is 0;")
    @Digits(integer = 32, fraction = 5, message = "currentAmountFrom-[currentAmountFrom] Maximum value is 32 numbers;")
    private BigDecimal currentAmountFrom;

    @DecimalMin(value = "0.00", message = "currentAmountTo-[currentAmountTo] Minimum value is 0;")
    @Digits(integer = 32, fraction = 5, message = "currentAmountTo-[currentAmountTo] Maximum value is 32 numbers;")
    private BigDecimal currentAmountTo;

    private DepositListingSortingType sortBy;

    private Sort.Direction direction;

}
