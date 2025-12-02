package bg.energo.phoenix.model.request.receivable.customerLiability;

import bg.energo.phoenix.model.customAnotations.DateRangeValidator;
import bg.energo.phoenix.model.customAnotations.customer.ValidIBAN;
import bg.energo.phoenix.model.customAnotations.receivable.LocalDateDeserializer;
import bg.energo.phoenix.model.customAnotations.receivable.LocalDateSerializer;
import bg.energo.phoenix.model.customAnotations.receivable.customerLiability.CustomerLiabilityCreateRequestValidator;
import bg.energo.phoenix.model.customAnotations.receivable.customerLiability.CustomerLiabilityPostRequest;
import bg.energo.phoenix.model.customAnotations.receivable.customerLiability.CustomerLiabilityPutRequest;
import bg.energo.phoenix.model.customAnotations.receivable.customerLiability.ValidBigDecimalDigits;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@CustomerLiabilityCreateRequestValidator(groups = CustomerLiabilityPostRequest.class)
public class CustomerLiabilityRequest {

    @NotNull(message = "accountingPeriodId is mandatory;", groups = CustomerLiabilityPostRequest.class)
    private Long accountingPeriodId;

    @NotNull(message = "dueDate is mandatory;", groups = CustomerLiabilityPostRequest.class)
    @DateRangeValidator(fieldPath = "dueDate", fromDate = "1990-01-01", toDate = "2090-12-31", groups = {CustomerLiabilityPostRequest.class, CustomerLiabilityPutRequest.class})
    @JsonDeserialize(using = LocalDateDeserializer.class)
    @JsonSerialize(using = LocalDateSerializer.class)
    private LocalDate dueDate;

    private Long applicableInterestRateId;

    @DateRangeValidator(fieldPath = "interestDateFrom", fromDate = "1990-01-01", toDate = "2090-12-31", groups = {CustomerLiabilityPostRequest.class, CustomerLiabilityPutRequest.class})
    @JsonDeserialize(using = LocalDateDeserializer.class)
    private LocalDate interestDateFrom;

    @DateRangeValidator(fieldPath = "interestDateTo", fromDate = "1990-01-01", toDate = "2090-12-31", groups = {CustomerLiabilityPostRequest.class, CustomerLiabilityPutRequest.class})
    @JsonDeserialize(using = LocalDateDeserializer.class)
    private LocalDate interestDateTo;

    @NotNull(message = "initialAmount is mandatory;", groups = CustomerLiabilityPostRequest.class)
    @DecimalMin(value = "0", message = "initialAmount must be greater than or equal to 0;", groups = {CustomerLiabilityPostRequest.class, CustomerLiabilityPutRequest.class})
    @ValidBigDecimalDigits(message = "initialAmount total digits must not exceed 32. If decimal is present, must have 1 or 2 decimal places;", groups = {CustomerLiabilityPostRequest.class, CustomerLiabilityPutRequest.class})
    private BigDecimal initialAmount;

    @NotNull(message = "currencyId is mandatory", groups = CustomerLiabilityPostRequest.class)
    private Long currencyId;

    @Size(min = 1, max = 512, message = "outgoingDocumentFromExternalSystem must be between 1 and 512 characters;", groups = {CustomerLiabilityPostRequest.class, CustomerLiabilityPutRequest.class})
    @Pattern(regexp = "^[A-Za-z0-9]*$", message = "outgoingDocumentFromExternalSystem allows only A-Z a-z 0-9;", groups = {CustomerLiabilityPostRequest.class, CustomerLiabilityPutRequest.class})
    private String outgoingDocumentFromExternalSystem;

    @Size(min = 1, max = 512, message = "basisForIssuing must be between 1 and 512 characters;", groups = {CustomerLiabilityPostRequest.class, CustomerLiabilityPutRequest.class})
    @Pattern(regexp = "^[A-Za-z0-9]*$", message = "basisForIssuing allows only A-Z a-z 0-9;", groups = {CustomerLiabilityPostRequest.class, CustomerLiabilityPutRequest.class})
    private String basisForIssuing;

    @Size(min = 1, max = 512, message = "numberOfIncomeAccount must be between 1 and 512 characters;", groups = {CustomerLiabilityPostRequest.class, CustomerLiabilityPutRequest.class})
    @Pattern(regexp = "^[A-Za-z0-9]*$", message = "numberOfIncomeAccount allows only A-Z a-z 0-9;", groups = {CustomerLiabilityPostRequest.class, CustomerLiabilityPutRequest.class})
    private String numberOfIncomeAccount;

    @Size(min = 1, max = 512, message = "costCenterControllingOrder must be between 1 and 512 characters;", groups = {CustomerLiabilityPostRequest.class, CustomerLiabilityPutRequest.class})
    @Pattern(regexp = "^[A-Za-z0-9]*$", message = "costCenterControllingOrder allows only A-Z a-z 0-9;", groups = {CustomerLiabilityPostRequest.class, CustomerLiabilityPutRequest.class})
    private String costCenterControllingOrder;

    private boolean directDebit;

    private Long bankId;

    @ValidIBAN(errorMessageKey = "customerLiabilityRequest.iban", groups = {CustomerLiabilityPostRequest.class, CustomerLiabilityPutRequest.class})
    private String bankAccount;

    @Size(min = 1, max = 2048, message = "additionalLiabilityInformation must be between 1 and 2048 characters;", groups = {CustomerLiabilityPostRequest.class, CustomerLiabilityPutRequest.class})
    private String additionalLiabilityInformation;

    private boolean blockedForPayment;

    @DateRangeValidator(fieldPath = "blockedForPaymentFromDate", fromDate = "1990-01-01", toDate = "2090-12-31", groups = {CustomerLiabilityPostRequest.class, CustomerLiabilityPutRequest.class})
    @JsonDeserialize(using = LocalDateDeserializer.class)
    private LocalDate blockedForPaymentFromDate;

    @DateRangeValidator(fieldPath = "blockedForPaymentToDate", fromDate = "1990-01-01", toDate = "2090-12-31", groups = {CustomerLiabilityPostRequest.class, CustomerLiabilityPutRequest.class})
    @JsonDeserialize(using = LocalDateDeserializer.class)
    private LocalDate blockedForPaymentToDate;

    private Long blockedForPaymentReasonId;

    @Size(min = 1, max = 2048, message = "blockedForPaymentAdditionalInfo must be between 1 and 2048 characters;", groups = {CustomerLiabilityPostRequest.class, CustomerLiabilityPutRequest.class})
    private String blockedForPaymentAdditionalInfo;

    private boolean blockedForReminderLetters;

    @DateRangeValidator(fieldPath = "blockedForReminderLettersFromDate", fromDate = "1990-01-01", toDate = "2090-12-31", groups = {CustomerLiabilityPostRequest.class, CustomerLiabilityPutRequest.class})
    @JsonDeserialize(using = LocalDateDeserializer.class)
    private LocalDate blockedForReminderLettersFromDate;

    @DateRangeValidator(fieldPath = "blockedForReminderLettersToDate", fromDate = "1990-01-01", toDate = "2090-12-31", groups = {CustomerLiabilityPostRequest.class, CustomerLiabilityPutRequest.class})
    @JsonDeserialize(using = LocalDateDeserializer.class)
    private LocalDate blockedForReminderLettersToDate;

    private Long blockedForReminderLettersReasonId;

    @Size(min = 1, max = 2048, message = "blockedForReminderLettersAdditionalInfo must be between 1 and 2048 characters;", groups = {CustomerLiabilityPostRequest.class, CustomerLiabilityPutRequest.class})
    private String blockedForReminderLettersAdditionalInfo;

    private boolean blockedForCalculationOfLatePayment;

    @DateRangeValidator(fieldPath = "blockedForCalculationOfLatePaymentFromDate", fromDate = "1990-01-01", toDate = "2090-12-31", groups = {CustomerLiabilityPostRequest.class, CustomerLiabilityPutRequest.class})
    @JsonDeserialize(using = LocalDateDeserializer.class)
    private LocalDate blockedForCalculationOfLatePaymentFromDate;

    @DateRangeValidator(fieldPath = "blockedForCalculationOfLatePaymentToDate", fromDate = "1990-01-01", toDate = "2090-12-31", groups = {CustomerLiabilityPostRequest.class, CustomerLiabilityPutRequest.class})
    @JsonDeserialize(using = LocalDateDeserializer.class)
    private LocalDate blockedForCalculationOfLatePaymentToDate;

    private Long blockedForCalculationOfLatePaymentReasonId;

    @Size(max = 2048, message = "blockedForCalculationOfLatePaymentAdditionalInfo must not exceed 2048 characters;", groups = {CustomerLiabilityPostRequest.class, CustomerLiabilityPutRequest.class})
    private String blockedForCalculationOfLatePaymentAdditionalInfo;

    private boolean blockedForLiabilitiesOffsetting;

    @DateRangeValidator(fieldPath = "blockedForLiabilitiesOffsettingFromDate", fromDate = "1990-01-01", toDate = "2090-12-31", groups = {CustomerLiabilityPostRequest.class, CustomerLiabilityPutRequest.class})
    @JsonDeserialize(using = LocalDateDeserializer.class)
    private LocalDate blockedForLiabilitiesOffsettingFromDate;

    @DateRangeValidator(fieldPath = "blockedForLiabilitiesOffsettingToDate", fromDate = "1990-01-01", toDate = "2090-12-31", groups = {CustomerLiabilityPostRequest.class, CustomerLiabilityPutRequest.class})
    @JsonDeserialize(using = LocalDateDeserializer.class)
    private LocalDate blockedForLiabilitiesOffsettingToDate;

    private Long blockedForLiabilitiesOffsettingReasonId;

    @Size(min = 1, max = 2048, message = "blockedForCalculationOfLatePaymentAdditionalInfo must be between 1 and 2048 characters;", groups = {CustomerLiabilityPostRequest.class, CustomerLiabilityPutRequest.class})
    private String blockedForLiabilitiesOffsettingAdditionalInfo;

    private boolean blockedForSupplyTermination;

    @DateRangeValidator(fieldPath = "blockedForSupplyTerminationFromDate", fromDate = "1990-01-01", toDate = "2090-12-31", groups = {CustomerLiabilityPostRequest.class, CustomerLiabilityPutRequest.class})
    @JsonDeserialize(using = LocalDateDeserializer.class)
    private LocalDate blockedForSupplyTerminationFromDate;

    @DateRangeValidator(fieldPath = "blockedForSupplyTerminationToDate", fromDate = "1990-01-01", toDate = "2090-12-31", groups = {CustomerLiabilityPostRequest.class, CustomerLiabilityPutRequest.class})
    @JsonDeserialize(using = LocalDateDeserializer.class)
    private LocalDate blockedForSupplyTerminationToDate;

    private Long blockedForSupplyTerminationReasonId;

    @Size(min = 1, max = 2048, message = "blockedForLiabilitiesOffsettingAdditionalInfo must be between 1 and 2048 characters;", groups = {CustomerLiabilityPostRequest.class, CustomerLiabilityPutRequest.class})
    private String blockedForSupplyTerminationAdditionalInfo;

    @NotNull(message = "customerId is mandatory", groups = CustomerLiabilityPostRequest.class)
    private Long customerId;

    private Long billingGroupId;

    private Long alternativeInvoiceRecipientCustomerId;

    @DecimalMin(value = "0", message = "amountWithoutInterest must be greater than or equal to 0;", groups = {CustomerLiabilityPostRequest.class, CustomerLiabilityPutRequest.class})
    @ValidBigDecimalDigits(message = "amountWithoutInterest total digits must not exceed 32. If decimal is present, must have 1 or 2 decimal places;", groups = {CustomerLiabilityPostRequest.class, CustomerLiabilityPutRequest.class})
    private BigDecimal amountWithoutInterest;

    @DateRangeValidator(fieldPath = "occurrenceDate", fromDate = "1990-01-01", toDate = "2090-12-31", groups = {CustomerLiabilityPostRequest.class, CustomerLiabilityPutRequest.class})
    @JsonDeserialize(using = LocalDateDeserializer.class)
    @JsonSerialize(using = LocalDateSerializer.class)
    @NotNull(message = "occurrence date is mandatory!;",groups = {CustomerLiabilityPostRequest.class})
    private LocalDate occurrenceDate;

}
