package bg.energo.phoenix.model.response.receivable.customerReceivable;

import bg.energo.phoenix.model.entity.EntityStatus;
import bg.energo.phoenix.model.entity.receivable.CustomerReceivable;
import bg.energo.phoenix.model.enums.receivable.CreationType;
import bg.energo.phoenix.model.enums.receivable.OutgoingDocumentType;
import bg.energo.phoenix.model.response.billing.accountingPeriods.AccountingPeriodsResponse;
import bg.energo.phoenix.model.response.billing.invoice.InvoiceShortResponse;
import bg.energo.phoenix.model.response.contract.biling.BillingGroupListingResponse;
import bg.energo.phoenix.model.response.customer.CustomerDetailsShortResponse;
import bg.energo.phoenix.model.response.nomenclature.customer.BankResponse;
import bg.energo.phoenix.model.response.receivable.CustomerOffsettingResponse;
import bg.energo.phoenix.model.response.shared.ShortResponse;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CustomerReceivableResponse {
    private Long id;
    private String number;
    private AccountingPeriodsResponse accountingPeriodResponse;
    private LocalDateTime creationDate;
    private LocalDate dateOfFullOffset;
    private LocalDate occurrenceDate;
    private LocalDate dueDate;
    private BigDecimal initialAmount;
    private BigDecimal initialAmountInOtherCurrency;
    private BigDecimal currentAmount;
    private BigDecimal currentAmountInOtherCurrency;
    private ShortResponse currencyResponse;
    private String outgoingDocumentFromAnExternalSystem;
    private String basisForIssuing;
    private String numberOfIncomeAccount;
    private String costCenterControllingOrder;
    private Boolean directDebit;
    private BankResponse bankResponse;
    private String bankAccount;
    private Boolean blockedForLiabilitiesOffsetting;
    private LocalDate blockedFromDate;
    private LocalDate blockedToDate;
    private ShortResponse reason;
    private String blockedForPaymentAdditionalInformation;
    private String additionalInformation;
    private CustomerDetailsShortResponse customerResponse;
    private BillingGroupListingResponse billingGroupResponse;
    private CustomerDetailsShortResponse alternativeRecipientOfAnInvoiceResponse;
    private OutgoingDocumentType outgoingDocumentType;
    private EntityStatus status;
    private InvoiceShortResponse invoiceResponse;
    private Long actionId;
    private CreationType creationType;
    private List<CustomerOffsettingResponse> customerOffsettingResponseList;
    private ShortResponse latePaymentFineShortResponse;
    private ShortResponse depositShortResponse;
    private ShortResponse paymentShortResponse;

    public static CustomerReceivableResponse from(CustomerReceivable customerReceivable) {
        CustomerReceivableResponse response = new CustomerReceivableResponse();
        response.id = customerReceivable.getId();
        response.setNumber(customerReceivable.getReceivableNumber());
        response.setCreationDate(customerReceivable.getCreateDate());
        response.setDateOfFullOffset(customerReceivable.getFullOffsetDate());
        response.setDueDate(customerReceivable.getDueDate());
        response.setOccurrenceDate(customerReceivable.getOccurrenceDate());
        response.setInitialAmount(customerReceivable.getInitialAmount());
        response.setInitialAmountInOtherCurrency(customerReceivable.getInitialAmountInOtherCurrency());
        response.setCurrentAmount(customerReceivable.getCurrentAmount());
        response.setCurrentAmountInOtherCurrency(customerReceivable.getCurrentAmountInOtherCurrency());
        response.setOutgoingDocumentFromAnExternalSystem(customerReceivable.getOutgoingDocumentFromExternalSystem());
        response.setBasisForIssuing(customerReceivable.getBasisForIssuing());
        response.setNumberOfIncomeAccount(customerReceivable.getIncomeAccountNumber());
        response.setCostCenterControllingOrder(customerReceivable.getCostCenterControllingOrder());
        response.setDirectDebit(customerReceivable.getDirectDebit());
        response.setBankAccount(customerReceivable.getBankAccount());
        response.setAdditionalInformation(customerReceivable.getAdditionalInformation());
        response.setBlockedForLiabilitiesOffsetting(customerReceivable.getBlockedForPayment());
        response.setBlockedFromDate(customerReceivable.getBlockedForPaymentFromDate());
        response.setBlockedToDate(customerReceivable.getBlockedForPaymentToDate());
        response.setBlockedForPaymentAdditionalInformation(customerReceivable.getBlockedForPaymentAdditionalInformation());
        response.setOutgoingDocumentType(customerReceivable.getOutgoingDocumentType());
        response.setStatus(customerReceivable.getStatus());
        response.setCreationType(customerReceivable.getCreationType());
        return response;
    }
}
