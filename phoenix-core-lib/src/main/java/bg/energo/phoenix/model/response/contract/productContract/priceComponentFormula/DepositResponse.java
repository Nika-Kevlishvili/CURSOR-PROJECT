package bg.energo.phoenix.model.response.contract.productContract.priceComponentFormula;

import bg.energo.phoenix.model.entity.nomenclature.product.Currency;
import bg.energo.phoenix.model.response.nomenclature.product.currency.CurrencyResponse;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
public class DepositResponse {
    private BigDecimal cashDepositAmount;
    private CurrencyResponse cashDepositCurrency;
    private BigDecimal bankGuaranteeAmount;
    private CurrencyResponse bankGuaranteeCurrency;

    public DepositResponse(BigDecimal cashDepositAmount, Currency cashDepositCurrency, BigDecimal bankGuaranteeAmount, Currency bankGuaranteeCurrency) {
        this.cashDepositAmount = cashDepositAmount;
        this.cashDepositCurrency = cashDepositCurrency == null ? null : new CurrencyResponse(cashDepositCurrency);
        this.bankGuaranteeAmount = bankGuaranteeAmount;
        this.bankGuaranteeCurrency = bankGuaranteeCurrency == null ? null : new CurrencyResponse(bankGuaranteeCurrency);
    }
}
