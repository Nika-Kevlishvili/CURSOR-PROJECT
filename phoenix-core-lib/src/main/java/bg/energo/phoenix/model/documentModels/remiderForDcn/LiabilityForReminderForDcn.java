package bg.energo.phoenix.model.documentModels.remiderForDcn;

import java.math.BigDecimal;
import java.time.LocalDate;

public class LiabilityForReminderForDcn {
    public String InitialAmount;
    public String CurrentAmount;
    public String Currency;
    public String OutgoingDocumentNumber;
    public String OutgoingDocumentPrefix;
    public LocalDate OutgoingDocumentDate;
    public String ContractNumber;
    public String BillingGroup;
    public LocalDate DueDate;



    public LiabilityForReminderForDcn(String documentNumber, String documentPrefix, LocalDate documentDate,
                                      BigDecimal initialAmount, BigDecimal currentAmount, String currency, String contractNumber,
                                      String billingGroup, LocalDate dueDate) {
        this.OutgoingDocumentNumber = documentNumber;
        this.OutgoingDocumentPrefix = documentPrefix;
        this.OutgoingDocumentDate = documentDate;
        this.InitialAmount = initialAmount != null ? initialAmount.toString() : "";
        this.CurrentAmount = currentAmount != null ? currentAmount.toString() : "";
        this.Currency = currency;
        this.ContractNumber =contractNumber;
        this.BillingGroup = billingGroup;
        this.DueDate = dueDate;
    }
}
