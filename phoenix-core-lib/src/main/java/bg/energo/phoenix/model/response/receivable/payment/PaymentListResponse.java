package bg.energo.phoenix.model.response.receivable.payment;

import bg.energo.phoenix.model.entity.EntityStatus;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class PaymentListResponse {

    private Long id;
    private String paymentNumber;
    private String customer;
    private String billingGroup;
    private String outgoingDocumentType;
    private String collectionChannel;
    private Long paymentPackage;
    private LocalDate paymentDate;
    private BigDecimal initialAmount;
    private BigDecimal currentAmount;
    private EntityStatus status;

    public PaymentListResponse(PaymentListMiddleResponse middleResponse) {
        this.id = middleResponse.getCustomerPaymentId();
        this.paymentNumber = middleResponse.getPaymentNumber();
        this.customer = middleResponse.getCustomer();
        this.billingGroup = middleResponse.getBillingGroup();
        this.outgoingDocumentType = middleResponse.getOutgoingDocumentType();
        this.collectionChannel = middleResponse.getPaymentChannel();
        this.paymentPackage = middleResponse.getPaymentPackage();
        this.paymentDate = middleResponse.getPaymentDate();
        this.initialAmount = middleResponse.getInitialAmount();
        this.currentAmount = middleResponse.getCurrentAmount();
        this.status = middleResponse.getStatus();
    }
}
