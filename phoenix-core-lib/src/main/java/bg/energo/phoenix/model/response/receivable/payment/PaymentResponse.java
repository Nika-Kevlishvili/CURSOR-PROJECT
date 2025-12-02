package bg.energo.phoenix.model.response.receivable.payment;

import bg.energo.phoenix.model.entity.EntityStatus;
import bg.energo.phoenix.model.enums.receivable.payment.OutgoingDocumentType;
import bg.energo.phoenix.model.response.billing.accountingPeriods.AccountingPeriodsResponse;
import bg.energo.phoenix.model.response.billing.invoice.InvoiceShortResponse;
import bg.energo.phoenix.model.response.contract.biling.ContractBillingGroupShortResponse;
import bg.energo.phoenix.model.response.customer.CustomerShortResponse;
import bg.energo.phoenix.model.response.nomenclature.product.currency.CurrencyShortResponse;
import bg.energo.phoenix.model.response.penalty.PenaltyShortResponse;
import bg.energo.phoenix.model.response.receivable.CustomerOffsettingResponse;
import bg.energo.phoenix.model.response.receivable.collectionChannel.CollectionChannelShortResponse;
import bg.energo.phoenix.model.response.receivable.deposit.DepositShortResponse;
import bg.energo.phoenix.model.response.receivable.latePaymentFine.LatePaymentFineShortResponse;
import bg.energo.phoenix.model.response.receivable.massOperationForBlocking.BlockingReasonShortResponse;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder(setterPrefix = "with")
public class PaymentResponse {

    private Long id;

    private String paymentNumber;

    private LocalDateTime paymentDate;

    private LocalDateTime fullOffsetDate;

    private BigDecimal initialAmount;

    private BigDecimal currentAmount;

    private CurrencyShortResponse currencyId;

    private CollectionChannelShortResponse collectionChannelId;

    private Long paymentPackageId;

    private AccountingPeriodsResponse accountPeriodId;

    private String paymentPurpose;

    private String paymentInfo;

    private Boolean blockedForOffsetting;

    private LocalDateTime blockedForOffsettingFromDate;

    private LocalDateTime blockedForOffsettingToDate;

    private BlockingReasonShortResponse blockedForOffsettingBlockingReasonId;

    private String blockedForOffsettingAdditionalInfo;

    private CustomerShortResponse customerId;

    private ContractBillingGroupShortResponse contractBillingGroupId;

    private OutgoingDocumentType outgoingDocumentType;

    private InvoiceShortResponse invoiceId;

    private LatePaymentFineShortResponse latePaymentFineId;

    private DepositShortResponse customerDepositId;

    private PenaltyShortResponse penaltyId;

    private List<CustomerOffsettingResponse> offsettingResponseList;

    private EntityStatus status;

    private Boolean isOnlinePayment;
}
