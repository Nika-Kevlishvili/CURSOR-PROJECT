package bg.energo.phoenix.model.response.receivable.deposit;

import bg.energo.phoenix.model.entity.EntityStatus;
import bg.energo.phoenix.model.entity.receivable.deposit.Deposit;
import bg.energo.phoenix.model.response.nomenclature.product.currency.CurrencyShortResponse;
import bg.energo.phoenix.model.response.receivable.CustomerOffsettingResponse;
import bg.energo.phoenix.model.response.receivable.manualLiabilityOffsetting.ReceivableTemplateResponse;
import bg.energo.phoenix.model.response.shared.FileWithStatusesResponse;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
public class DepositResponse {

    private Long id;
    private String depositNumber;
    private LocalDate paymentDeadline;
    private LocalDate refundDate;
    private BigDecimal initialAmount;
    private BigDecimal initialAmountInOtherCurrency;
    private BigDecimal currentAmount;
    private BigDecimal currentAmountInOtherCurrency;

    private CurrencyShortResponse currencyShortResponse;
    private String incomeAccountNumber;
    private String costCenter;
    private DepositCustomerResponse customerResponse;
    private EntityStatus status;
    private DepositPaymentDdlAftWithdrawalResponse withdrawalResponse;
    private List<DepositProductContractResponse> depositProductContractResponse;
    private List<DepositServiceContractResponse> depositServiceContractResponse;
    private List<ReceivableTemplateResponse> templateResponses;

    private List<CustomerOffsettingResponse> customerDepositOffsettingResponseList;
    private List<FileWithStatusesResponse> files;

    private boolean canDelete;

    public DepositResponse(Deposit deposit) {
        this.id = deposit.getId();
        this.depositNumber = deposit.getDepositNumber();
        this.paymentDeadline = deposit.getPaymentDeadline();
        if (deposit.getRefundDate() != null) {
            this.refundDate = deposit.getRefundDate();
        }
        this.initialAmount = deposit.getInitialAmount();
        this.currentAmount = deposit.getCurrentAmount();
        this.incomeAccountNumber = deposit.getIncomeAccountNumber();
        this.costCenter = deposit.getCostCenter();
        this.status = deposit.getStatus();
    }

}
