package bg.energo.phoenix.model.request.receivable.deposit;

import bg.energo.phoenix.model.customAnotations.DateRangeValidator;
import bg.energo.phoenix.model.customAnotations.receivable.LocalDateDeserializer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
public class DepositUpdateRequest {

    @NotNull(message = "paymentDeadline-[paymentDeadline] must not be null;")
    @DateRangeValidator(fieldPath = "paymentDeadline", fromDate = "1990-01-01", toDate = "2090-12-31")
    @JsonDeserialize(using = LocalDateDeserializer.class)
    private LocalDate paymentDeadline; //TODO after adding offsetting objects

    @NotNull(message = "refundDate-[refundDate] must not be null;")
    @DateRangeValidator(fieldPath = "refundDate", fromDate = "1990-01-01", toDate = "2090-12-31")
    @JsonDeserialize(using = LocalDateDeserializer.class)
    private LocalDate refundDate;

    @NotNull(message = "initialAmount-[initialAmount] must not be null;")
    @DecimalMin(value = "0.00", message = "initialAmount-[initialAmount] Minimum value is 99999999999;")
    @DecimalMax(value = "99999999999.99", message = "initialAmount-[initialAmount] Maximum value is 99999999999;")
    @Digits(integer = 11, fraction = 2, message = "initialAmount-[initialAmount] Maximum value is 99999999999;")
    private BigDecimal initialAmount; //TODO after adding offsetting objects

    @NotNull(message = "currencyId-[currencyId] must not be null;")
    private Long currencyId; //TODO after adding offsetting objects

    @Pattern(regexp = "^[a-zA-Z0-9]+$", message = "numberOfIncomeAccount-[numberOfIncomeAccount] must be in A-Z a-z 0-9 range")
    @Size(min = 1, max = 512, message = "numberOfIncomeAccount-[numberOfIncomeAccount] must be between 1 and 512 characters")
    private String numberOfIncomeAccount;

    @Pattern(regexp = "^[a-zA-Z0-9]+$", message = "costCentre-[costCentre] must be in A-Z a-z 0-9 range")
    @Size(min = 1, max = 512, message = "costCentre-[costCentre] must be between 1 and 512 characters")
    private String costCentre;

    private @Valid List<DepositContractRequest> depositContractRequest;

    @NotNull(message = "paymentDeadlineAfterWithdrawalRequest-[paymentDeadlineAfterWithdrawalRequest] must not be null;")
    private @Valid PaymentDeadlineAfterWithdrawalRequest paymentDeadlineAfterWithdrawalRequest;

//    private Long templateForNotification; //TODO

//    private Long emailTemplate; //TODO



}
