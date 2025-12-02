package bg.energo.phoenix.model.response.receivable.latePaymentFine;

import bg.energo.phoenix.model.enums.receivable.latePaymentFine.LatePaymentFineReversed;
import bg.energo.phoenix.model.enums.receivable.latePaymentFine.LatePaymentFineType;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class LatePaymentFineListingResponse {

    private String number;
    private LocalDate createDate;
    private LocalDate dueDate;
    private LatePaymentFineType type;
    private String customerIdentifier;
    private LatePaymentFineReversed reversed;
    private String billingGroup;
    private BigDecimal amount;
    private String currency;
    private Long id;
    private LocalDate logicalDate;
    private String customer;

    public LatePaymentFineListingResponse(LatePaymentFineListingMiddleResponse middleResponse) {
        this.number = middleResponse.getNumber();
        this.createDate = middleResponse.getCreateDate();
        this.dueDate = middleResponse.getDueDate();
        this.type = middleResponse.getType();
        this.customer = middleResponse.getCustomer();
        this.reversed = middleResponse.getReversed();
        this.billingGroup = middleResponse.getBillingGroup();
        this.amount = middleResponse.getTotalAmount();
        this.currency = middleResponse.getCurrency();
        this.id = middleResponse.getId();
        this.logicalDate = middleResponse.getLogicalDate();
        this.customerIdentifier = middleResponse.getCustomerIdentifier();
    }
}
