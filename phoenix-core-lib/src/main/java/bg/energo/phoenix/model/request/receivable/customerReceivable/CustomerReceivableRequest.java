package bg.energo.phoenix.model.request.receivable.customerReceivable;

import bg.energo.phoenix.model.customAnotations.DateRangeValidator;
import bg.energo.phoenix.model.customAnotations.customer.ValidIBAN;
import bg.energo.phoenix.model.customAnotations.receivable.LocalDateDeserializer;
import bg.energo.phoenix.model.customAnotations.receivable.LocalDateSerializer;
import bg.energo.phoenix.model.customAnotations.receivable.customerLiability.CustomerLiabilityPostRequest;
import bg.energo.phoenix.model.customAnotations.receivable.customerLiability.CustomerLiabilityPutRequest;
import bg.energo.phoenix.model.customAnotations.receivable.customerLiability.ValidBigDecimalDigits;
import bg.energo.phoenix.model.customAnotations.receivable.customerReceivables.CustomerReceivableCreateValidator;
import bg.energo.phoenix.model.customAnotations.receivable.customerReceivables.CustomerReceivablePostGroup;
import bg.energo.phoenix.model.customAnotations.receivable.customerReceivables.CustomerReceivablePutGroup;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@NoArgsConstructor
@Getter
@Setter
@CustomerReceivableCreateValidator(groups = CustomerReceivablePostGroup.class)
public class CustomerReceivableRequest {

    @NotNull(message = "accountingPeriodId-[accountingPeriodId] accounting period id is mandatory;", groups = CustomerReceivablePostGroup.class)
    private Long accountingPeriodId;

    @NotNull(message = "initialAmount-[initialAmount] initial amount is mandatory!;", groups = CustomerReceivablePostGroup.class)
    @ValidBigDecimalDigits(message = "initialAmount total digits must not exceed 32;", groups = {CustomerLiabilityPostRequest.class, CustomerLiabilityPutRequest.class})
    @PositiveOrZero(message = "initialAmount-[initialAmount] initial amount should be positive or zero;", groups = {CustomerReceivablePostGroup.class, CustomerReceivablePutGroup.class})
    private BigDecimal initialAmount;

    @NotNull(message = "currencyId-[currencyId] currency id is mandatory;", groups = CustomerReceivablePostGroup.class)
    private Long currencyId;

    @Pattern(regexp = "^[a-zA-Z0-9]*$", message = "outgoingDocumentForAnExternalSystem-[outgoingDocumentForAnExternalSystem] allowed symbols are : A-Z a-z 0-9;", groups = {CustomerReceivablePostGroup.class, CustomerReceivablePutGroup.class})
    @Size(min = 1, max = 512, message = "outgoingDocumentForAnExternalSystem-[outgoingDocumentForAnExternalSystem] size must be between 1 and 512;", groups = {CustomerReceivablePostGroup.class, CustomerReceivablePutGroup.class})
    private String outgoingDocumentForAnExternalSystem;

    @Pattern(regexp = "^[a-zA-Z0-9]*$", message = "basisForIssuing-[basisForIssuing] allowed symbols are : A-Z a-z 0-9;", groups = {CustomerReceivablePostGroup.class, CustomerReceivablePutGroup.class})
    @Size(min = 1, max = 512, message = "basisForIssuing-[basisForIssuing] size must be between 1 and 512;", groups = {CustomerReceivablePostGroup.class, CustomerReceivablePutGroup.class})
    private String basisForIssuing;

    @Pattern(regexp = "^[a-zA-Z0-9]*$", message = "numberOfIncomeAccount-[numberOfIncomeAccount] allowed symbols are : A-Z a-z 0-9;", groups = {CustomerReceivablePostGroup.class, CustomerReceivablePutGroup.class})
    @Size(min = 1, max = 512, message = "numberOfIncomeAccount-[numberOfIncomeAccount] size must be between 1 and 512;", groups = {CustomerReceivablePostGroup.class, CustomerReceivablePutGroup.class})
    private String numberOfIncomeAccount;

    @Pattern(regexp = "^[a-zA-Z0-9]*$", message = "costCenterControllingOrder-[costCenterControllingOrder] allowed symbols are : A-Z a-z 0-9;", groups = {CustomerReceivablePostGroup.class, CustomerReceivablePutGroup.class})
    @Size(min = 1, max = 512, message = "costCenterControllingOrder-[costCenterControllingOrder] size must be between 1 and 512;", groups = {CustomerReceivablePostGroup.class, CustomerReceivablePutGroup.class})
    private String costCenterControllingOrder;

    private boolean directDebit;

    private Long bankId;

    @ValidIBAN(errorMessageKey = "customerReceivableRequest.iban", groups = {CustomerReceivablePostGroup.class, CustomerReceivablePutGroup.class})
    private String bankAccount;

    @Size(min = 1, max = 2048, message = "additionalReceivableInformation-[additionalReceivableInformation] Size  must be between 1 and 2048;", groups = {CustomerReceivablePostGroup.class, CustomerReceivablePutGroup.class})
    private String additionalReceivableInformation;

    private boolean blockedForOffsetting;

    @DateRangeValidator(fieldPath = "blockedFromDate", fromDate = "1990-01-01", toDate = "2090-12-31", groups = {CustomerReceivablePostGroup.class, CustomerReceivablePutGroup.class})
    @JsonDeserialize(using = LocalDateDeserializer.class)
    @JsonSerialize(using = LocalDateSerializer.class)
    private LocalDate blockedFromDate;

    @DateRangeValidator(fieldPath = "blockedToDate", fromDate = "1990-01-01", toDate = "2090-12-31", groups = {CustomerReceivablePostGroup.class, CustomerReceivablePutGroup.class})
    @JsonDeserialize(using = LocalDateDeserializer.class)
    @JsonSerialize(using = LocalDateSerializer.class)
    private LocalDate blockedToDate;

    private Long reasonId;

    @Size(min = 1, max = 2048, message = "additionalInformation-[additionalInformation] size must be between 1 and 2048;")
    private String additionalInformation;

    @NotNull(message = "customerId-[customerId] customer id is mandatory;", groups = CustomerReceivablePostGroup.class)
    private Long customerId;

    private Long billingGroupId;

    private Long alternativeInvoiceRecipientCustomerId;

    @NotNull(message = "occurrenceDate-[occurrenceDate] occurrence date is mandatory;", groups = CustomerReceivablePostGroup.class)
    @DateRangeValidator(fieldPath = "occurrenceDate", fromDate = "1990-01-01", toDate = "2090-12-31")
    @JsonDeserialize(using = LocalDateDeserializer.class)
    @JsonSerialize(using = LocalDateSerializer.class)
    private LocalDate occurrenceDate;

    @NotNull(message = "dueDate-[dueDate] due date is mandatory;", groups = CustomerReceivablePostGroup.class)
    @DateRangeValidator(fieldPath = "dueDate", fromDate = "1990-01-01", toDate = "2090-12-31")
    @JsonDeserialize(using = LocalDateDeserializer.class)
    @JsonSerialize(using = LocalDateSerializer.class)
    private LocalDate dueDate;

}

