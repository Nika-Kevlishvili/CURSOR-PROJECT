package bg.energo.phoenix.model.request.receivable.deposit;

import bg.energo.phoenix.model.customAnotations.DateRangeValidator;
import bg.energo.phoenix.model.customAnotations.receivable.LocalDateDeserializer;
import bg.energo.phoenix.model.customAnotations.receivable.LocalDateSerializer;
import bg.energo.phoenix.model.customAnotations.receivable.customerLiability.CustomerLiabilityPostRequest;
import bg.energo.phoenix.model.customAnotations.receivable.customerLiability.CustomerLiabilityPutRequest;
import bg.energo.phoenix.model.customAnotations.receivable.customerLiability.ValidBigDecimalDigits;
import bg.energo.phoenix.model.customAnotations.receivable.deposit.DepositRequestValidator;
import bg.energo.phoenix.model.customAnotations.receivable.deposit.DepositTemplateValidator;
import bg.energo.phoenix.model.entity.receivable.manualLiabilityOffsetting.ReceivableTemplateRequest;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

@Data
@NoArgsConstructor
@DepositRequestValidator
@DepositTemplateValidator
public class DepositCreateRequest {

    @NotNull(message = "paymentDeadline-[paymentDeadline] must not be null;")
    @DateRangeValidator(fieldPath = "paymentDeadline", fromDate = "1990-01-01", toDate = "2090-12-31")
    @JsonDeserialize(using = LocalDateDeserializer.class)
    @JsonSerialize(using = LocalDateSerializer.class)
    private LocalDate paymentDeadline;

    @DateRangeValidator(fieldPath = "refundDate", fromDate = "1990-01-01", toDate = "2090-12-31")
    @JsonDeserialize(using = LocalDateDeserializer.class)
    @JsonSerialize(using = LocalDateSerializer.class)
    private LocalDate refundDate;

    @NotNull(message = "initialAmount-[initialAmount] must not be null;")
    @DecimalMin(value = "0.00", message = "initialAmount-[initialAmount] Minimum value is 99999999999;")
    @DecimalMax(value = "99999999999.99", message = "initialAmount-[initialAmount] Maximum value is 99999999999;")
    @ValidBigDecimalDigits(message = "initialAmount total digits must not exceed 32;", groups = {CustomerLiabilityPostRequest.class, CustomerLiabilityPutRequest.class})
    private BigDecimal initialAmount;

    @DecimalMin(value = "0.00", message = "initialAmount-[initialAmount] Minimum value is 99999999999;")
    @DecimalMax(value = "99999999999.99", message = "initialAmount-[initialAmount] Maximum value is 99999999999;")
    @ValidBigDecimalDigits(message = "currentAmount total digits must not exceed 32;", groups = {CustomerLiabilityPostRequest.class, CustomerLiabilityPutRequest.class})
    private BigDecimal currentAmount;

    @NotNull(message = "currencyId-[currencyId] must not be null;")
    private Long currencyId;

    @Pattern(regexp = "^[a-zA-Z0-9]+$", message = "numberOfIncomeAccount-[numberOfIncomeAccount] must be in A-Z a-z 0-9 range")
    @Size(min = 1, max = 512, message = "numberOfIncomeAccount-[numberOfIncomeAccount] must be between 1 and 512 characters")
    @NotNull(message = "numberOfIncomeAccount-[numberOfIncomeAccount] must not be null;")
    private String numberOfIncomeAccount;

    @Pattern(regexp = "^[a-zA-Z0-9]+$", message = "costCentre-[costCentre] must be in A-Z a-z 0-9 range")
    @Size(min = 1, max = 512, message = "costCentre-[costCentre] must be between 1 and 512 characters")
    private String costCentre;

    @NotNull(message = "customerId-[customerId] must not be null")
    private Long customerId;

    private @Valid List<DepositContractRequest> depositContractRequest;

    @NotNull(message = "paymentDeadlineAfterWithdrawalRequest-[paymentDeadlineAfterWithdrawalRequest] must not be null;")
    private @Valid PaymentDeadlineAfterWithdrawalRequest paymentDeadlineAfterWithdrawalRequest;

//    private Long templateForNotification; //TODO

//    private Long emailTemplate; //TODO

    private Set<ReceivableTemplateRequest> templateIds;

}
