package bg.energo.phoenix.model.documentModels.reminder;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.time.LocalDate;

public class LiabilityForReminder {
    @JsonProperty("InitialAmount")
    public String InitialAmount;

    @JsonProperty("CurrentAmount")
    public String CurrentAmount;

    @JsonProperty("Currency")
    public String Currency;

    @JsonProperty("OutgoingDocumentNumber")
    public String OutgoingDocumentNumber;

    @JsonProperty("OutgoingDocumentPrefix")
    public String OutgoingDocumentPrefix;

    @JsonProperty("OutgoingDocumentDate")
    public LocalDate OutgoingDocumentDate;

    @JsonProperty("ContractNumber")
    public String ContractNumber;

    @JsonProperty("BillingGroup")
    public String BillingGroup;

    @JsonProperty("DueDate")
    public LocalDate DueDate;


    public LiabilityForReminder(String documentNumber, String documentPrefix, LocalDate documentDate,
                                BigDecimal initialAmount, BigDecimal currentAmount, String currency, String contractNumber,
                                String billingGroup, LocalDate dueDate) {
        this.OutgoingDocumentNumber = documentNumber;
        this.OutgoingDocumentPrefix = documentPrefix;
        this.OutgoingDocumentDate = documentDate;
        this.InitialAmount = initialAmount != null ? initialAmount.toString() : "";
        this.CurrentAmount = currentAmount != null ? currentAmount.toString() : "";
        this.Currency = currency;
        this.ContractNumber = contractNumber;
        this.BillingGroup = billingGroup;
        this.DueDate = dueDate;
    }
}
