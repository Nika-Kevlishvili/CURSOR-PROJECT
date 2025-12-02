package bg.energo.phoenix.model.response.receivable.customerReceivable;

import bg.energo.phoenix.model.entity.EntityStatus;
import bg.energo.phoenix.model.enums.billing.accountingsPeriods.AccountingPeriodStatus;
import bg.energo.phoenix.model.enums.receivable.CreationType;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
public class CustomerReceivableListingResponse {
    private Long id;
    private String customer;
    private String billingGroup;
    private String alternativeRecipient;
    private BigDecimal initialAmount;
    private BigDecimal currentAmount;
    private String receivableNumber;
    private EntityStatus status;
    private CreationType creationType;
    private AccountingPeriodStatus accountingPeriodStatus;
    private LocalDate occurrenceDate;
    private LocalDate dueDate;

    public static CustomerReceivableListingResponse from(CustomerReceivableMiddleResponse middleResponse) {
        CustomerReceivableListingResponse response = new CustomerReceivableListingResponse();
        response.id = middleResponse.getId();
        response.customer = middleResponse.getCustomer();
        response.billingGroup = middleResponse.getBillingGroup();
        response.alternativeRecipient = middleResponse.getAlternativeRecipient();
        response.initialAmount = middleResponse.getInitialAmount();
        response.currentAmount = middleResponse.getCurrentAmount();
        response.receivableNumber = middleResponse.getReceivableNumber();
        response.status = middleResponse.getStatus();
        response.occurrenceDate = middleResponse.getOccurrenceDate();
        response.dueDate = middleResponse.getDueDate();
        response.setCreationType(middleResponse.getCreationType());
        response.setAccountingPeriodStatus(middleResponse.getAccountingPeriodStatus());
        return response;
    }
}
