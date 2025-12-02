package bg.energo.phoenix.model.documentModels.mlo;

import java.math.BigDecimal;
import java.time.LocalDate;


public class ReceivableOrLiability {
    public String DocumentNumber;
    public String DocumentPrefix;
    public LocalDate DocumentDate;
    public BigDecimal InitialAmount;
    public BigDecimal CurrentAmount;
    public BigDecimal AmountAfter;

    public ReceivableOrLiability(String documentNumber, String documentPrefix, LocalDate documentDate, BigDecimal initialAmount, BigDecimal currentAmount, BigDecimal amountAfter) {
        this.DocumentNumber = documentNumber;
        this.DocumentPrefix = documentPrefix;
        this.DocumentDate = documentDate;
        this.InitialAmount = initialAmount;
        this.CurrentAmount = currentAmount;
        this.AmountAfter = amountAfter;
    }
}
