package bg.energo.phoenix.model.response.contract.productContract.priceComponentFormula;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
public class InstallmentForContractFields {
    private Boolean equalMonthlyInstallmentsActivation;
    private Short installmentNumber;
    private Short installmentNumberFrom;
    private Short installmentNumberTo;
    private BigDecimal amount;
    private BigDecimal amountFrom;
    private BigDecimal amountTo;
}
