package bg.energo.phoenix.model.response.receivable.customerLiability;

import bg.energo.phoenix.model.entity.EntityStatus;
import bg.energo.phoenix.model.enums.billing.accountingsPeriods.AccountingPeriodStatus;
import bg.energo.phoenix.model.enums.receivable.CreationType;
import bg.energo.phoenix.model.enums.receivable.CustomerLiabilitiesOutgoingDocType;
import bg.energo.phoenix.model.response.billing.invoice.InvoiceShortResponse;
import bg.energo.phoenix.model.response.contract.InterestRate.InterestRateShortResponse;
import bg.energo.phoenix.model.response.contract.biling.BillingGroupListingResponse;
import bg.energo.phoenix.model.response.customer.CustomerDetailsShortResponse;
import bg.energo.phoenix.model.response.nomenclature.customer.BankResponse;
import bg.energo.phoenix.model.response.nomenclature.product.currency.CurrencyShortResponse;
import bg.energo.phoenix.model.response.receivable.CustomerOffsettingResponse;
import bg.energo.phoenix.model.response.shared.ShortResponse;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CustomerLiabilityResponse {

    private Long id;
    private String number;
    private Long accountPeriodId;
    private String accountingPeriodName;
    private AccountingPeriodStatus accountingPeriodStatus;
    private LocalDate dueDate;
    private LocalDate fullOffsetDate;
    private InterestRateShortResponse applicableInterestRate;
    private LocalDate interestDateFrom;
    private LocalDate interestDateTo;
    private BigDecimal initialAmount;
    private BigDecimal initialAmountInOtherCurrency;
    private BigDecimal currentAmount;
    private BigDecimal currentAmountInOtherCurrency;
    private CurrencyShortResponse currencyResponse;
    private String outgoingDocumentFromExternalSystem;
    private String basisForIssuing;
    private String numberOfIncomeAccount;
    private String costCenterControllingOrder;
    private Boolean directDebit;
    private BankResponse bankResponse;
    private String iban;
    private Boolean blockedForPayment;
    private LocalDate blockedForPaymentFromDate;
    private LocalDate blockedForPaymentToDate;
    private ShortResponse blockedForPaymentReason;
    private String blockedForPaymentAdditionalInfo;
    private Boolean blockedForReminderLetters;
    private LocalDate blockedForReminderLettersFromDate;
    private LocalDate blockedForReminderLettersToDate;
    private ShortResponse blockedForReminderLettersReason;
    private String blockedForReminderLettersAdditionalInfo;
    private Boolean blockedForCalculationOfLatePayment;
    private LocalDate blockedForCalculationOfLatePaymentFromDate;
    private LocalDate blockedForCalculationOfLatePaymentToDate;
    private ShortResponse blockedForCalculationOfLatePaymentReason;
    private String blockedForCalculationOfLatePaymentAdditionalInfo;
    private Boolean blockedForLiabilitiesOffsetting;
    private LocalDate blockedForLiabilitiesOffsettingFromDate;
    private LocalDate blockedForLiabilitiesOffsettingToDate;
    private ShortResponse blockedForLiabilitiesOffsettingReason;
    private String blockedForLiabilitiesOffsettingAdditionalInfo;
    private Boolean blockedForSupplyTermination;
    private LocalDate blockedForSupplyTerminationFromDate;
    private LocalDate blockedForSupplyTerminationToDate;
    private ShortResponse blockedForSupplyTerminationReason;
    private String blockedForSupplyTerminationAdditionalInfo;
    private CustomerDetailsShortResponse customerResponse;
    private InvoiceShortResponse invoiceResponse;
    private ShortResponse reschedulingShortResponse;
    private ShortResponse latePaymentFineShortResponse;
    private ShortResponse depositShortResponse;
    private ShortResponse actionShortResponse;
    private BillingGroupListingResponse billingGroupResponse;
    private CustomerDetailsShortResponse alternativeInvoiceRecipientCustomerResponse;
    private CustomerLiabilitiesOutgoingDocType outgoingDocumentType;
    private EntityStatus status;
    private CreationType creationType;
    private String additionalInfo;
    private List<CustomerOffsettingResponse> customerLiabilityOffsettingReponseList;
    private LocalDate creationDate;
    private BigDecimal amountWithoutInterest;
    private BigDecimal amountWithoutInterestInOtherCurrency;
    private LocalDate occurrenceDate;
}
