package bg.energo.phoenix.model.response.receivable.latePaymentFine;

import bg.energo.phoenix.model.entity.receivable.latePaymentFine.LatePaymentFine;
import bg.energo.phoenix.model.enums.receivable.latePaymentFine.LatePaymentFineOutDocType;
import bg.energo.phoenix.model.enums.receivable.latePaymentFine.LatePaymentFineType;
import bg.energo.phoenix.model.response.nomenclature.product.currency.CurrencyShortResponse;
import bg.energo.phoenix.model.response.shared.ShortResponse;
import bg.energo.phoenix.model.response.task.TaskShortResponse;
import bg.energo.phoenix.model.response.template.TemplateFileResponse;
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
@AllArgsConstructor
@NoArgsConstructor
public class LatePaymentFineResponse {
    private Long id;
    private String latePaymentNumber;
    private LatePaymentFineType type;
    private BigDecimal amount;
    private BigDecimal amountInOtherCurrency;
    private LocalDate dueDate;
    private CurrencyShortResponse currencyShortResponse;
    private String incomeAccountNumber;
    private String constCentreControllingOrder;
    private LatePaymentCustomerShortResponse customerShortResponse;
    private ShortResponse contractBillingGroupShortResponse;
    private Long contractBillingGroupId;
    private Long issuerVersionId;
    private String issuerName;
    private String fileUrl;
    private boolean reversed;
    private List<LatePaymentFineInvoiceTableResponse> tableResponse;
    private LocalDateTime createDate;
    private List<Long> contractDetailIds;
    private LatePaymentFineReversalShortResponse latePaymentFineReversalShortResponse;
    private LatePaymentFineReversalShortResponse parentLatePaymentFineShortResponse;
    private List<CommunicationShortResponse> communicationShortResponse;
    private LocalDate logicalDate;
    private ShortResponse parentLiabilityShortResponse;
    private ShortResponse reschedulingShortResponse;
    private LatePaymentFineOutDocType outDocType;
    private List<TemplateFileResponse> fileResponse;
    private ShortResponse templateShortResponse;
    private List<TaskShortResponse> tasks;

    public LatePaymentFineResponse(LatePaymentFine latePaymentFine) {
        this.id = latePaymentFine.getId();
        this.latePaymentNumber = latePaymentFine.getLatePaymentNumber();
        this.type = latePaymentFine.getType();
        this.amount = latePaymentFine.getAmount();
        this.amountInOtherCurrency = latePaymentFine.getAmountInOtherCcy();
        this.dueDate = latePaymentFine.getDueDate();
        this.incomeAccountNumber = latePaymentFine.getIncomeAccountNumber();
        this.constCentreControllingOrder = latePaymentFine.getConstCentreControllingOrder();
        this.fileUrl = latePaymentFine.getFileUrl();
        this.reversed = latePaymentFine.isReversed() ||
                latePaymentFine.getType().equals(LatePaymentFineType.REVERSAL_OF_LATE_PAYMENT_FINE);
        this.createDate = latePaymentFine.getCreateDate();
        this.logicalDate = latePaymentFine.getLogicalDate();

    }
}
