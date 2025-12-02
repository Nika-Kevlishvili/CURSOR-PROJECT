package bg.energo.phoenix.model.response.billing.accountingPeriods;

import java.math.BigDecimal;
import java.time.LocalDate;

public interface AccountPeriodSapResponse {
    LocalDate getLastDay();
    String getIncomeAccountName();
    BigDecimal getTotalAmount();
    BigDecimal getTotalVolumes();
    String getIncomeAccountNumber();
    BigDecimal getTotalAmountOfVat();
}
