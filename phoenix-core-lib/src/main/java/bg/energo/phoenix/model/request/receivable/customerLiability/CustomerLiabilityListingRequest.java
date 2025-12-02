package bg.energo.phoenix.model.request.receivable.customerLiability;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
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
public class CustomerLiabilityListingRequest {

    @NotNull(message = "page-Page must not be null;")
    private Integer page;

    @NotNull(message = "size-Size must not be null;")
    private Integer size;

    private String prompt;

    private CustomerLiabilitySearchFields searchFields;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate dueDateFrom;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate dueDateTo;

    @Digits(integer = 14, fraction = 5, message = "initialAmountFrom-Initial amount from must have up to 14 digits and 5 digits after decimal point;")
    private BigDecimal initialAmountFrom;

    @Digits(integer = 14, fraction = 5, message = "initialAmountTo-Initial amount to must have up to 14 digits and 5 digits after decimal point;")
    private BigDecimal initialAmountTo;

    @Digits(integer = 14, fraction = 5, message = "currentAmountFrom-Current amount from must have up to 14 digits and 5 digits after decimal point;")
    private BigDecimal currentAmountFrom;

    @Digits(integer = 14, fraction = 5, message = "currentAmountTo-Current amount to must have up to 14 digits and 5 digits after decimal point;")
    private BigDecimal currentAmountTo;

    @Size(min = 14, max = 14, message = "billingGroup-Billing group must have 14 characters;")
    @Pattern(regexp = "^\\d+$", message = "billingGroup-Billing group must contain only digits;")
    private String billingGroup;

    private Boolean blockedForPayments;

    private Boolean blockedForReminderLetters;

    private Boolean blockedForInterestCalculation;

    private Boolean blockedForLiabilityOffsetting;

    private Boolean blockedForSupplyTermination;

    private CustomerLiabilityListColumns columns;

    private Sort.Direction direction;

    private List<Long> currencyIds;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate occurrenceDateFrom;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate occurrenceDateTo;
}
