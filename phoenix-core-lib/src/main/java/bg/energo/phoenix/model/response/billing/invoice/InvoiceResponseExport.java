package bg.energo.phoenix.model.response.billing.invoice;

import bg.energo.phoenix.model.enums.billing.invoice.InvoiceDocumentType;
import bg.energo.phoenix.model.enums.billing.invoice.InvoiceStatus;
import bg.energo.phoenix.model.enums.billing.invoice.InvoiceType;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class InvoiceResponseExport {
    private Long invoiceId;
    private String invoiceNumber;
    private LocalDate creationDate;
    private InvoiceStatus invoiceStatus;
    private LocalDate statusModifyDate;
    private InvoiceDocumentType documentType;
    private String customerIdentifier;
    private String customerName;
    private LocalDate taxEventDate;
    private LocalDate paymentDeadLine;
    private InvoiceType invoiceType;
    private String contractOrder;
    private String billingGroupName;
    private String recipientOfInvoiceNumber;
    private String recipientOfInvoiceName;
    private String commForBilling;
    private String productServiceName;
    private LocalDate meterReadingFrom;
    private LocalDate meterReadingTo;
    private String numberOfIncomeAccount;
    private String basisForIssuing;
    private String costCenterControllingOrder;
    private String interestRate;
    private List<InvoiceDetailedExportModel> vatRateResponses;
    private BigDecimal totalAmountIncludingVat;
    private BigDecimal totalAmountIncludingVatInOtherCurrency;
    private Boolean directDebit;
    private String bank;
    private String bic;
    private String bankAccount;
    private String issuer;
    private String liabilityCustomerReceivable;
    private String currencyName;
    private String accountingPeriodName;
    private String templateName;
    private String compensation;
    private String debitCreditNotes;

    public InvoiceResponseExport(
            Long invoiceId,
            String invoiceNumber,
            LocalDateTime creationDate,
            InvoiceStatus invoiceStatus,
            LocalDateTime statusModifyDate,
            InvoiceDocumentType documentType,
            String customerIdentifier,
            String customerName,
            LocalDate taxEventDate,
            LocalDate paymentDeadLine,
            InvoiceType invoiceType,
            String contractOrder,
            String billingGroupName,
            String recipientOfInvoiceNumber,
            String recipientOfInvoiceName,
            String commsName,
            String productServiceName,
            LocalDate meterReadingFrom,
            LocalDate meterReadingTo,
            String numberOfIncomeAccount,
            String basisForIssuing,
            String costCenterControllingOrder,
            String interestRate,
            Boolean directDebit,
            String bank,
            String bic,
            String bankAccount,
            BigDecimal totalAmountIncludingVat,
            BigDecimal totalAmountIncludingVatInOtherCurrency,
            String currencyName,
            String accountingPeriodName,
            String templateName,
            String govCompensation,
            String debitCreditNotes
    ) {
        this.invoiceId = invoiceId;
        this.invoiceNumber = invoiceNumber;
        this.creationDate = creationDate.toLocalDate();
        this.invoiceStatus = invoiceStatus;
        this.documentType = documentType;
        this.customerIdentifier = customerIdentifier;
        this.customerName = customerName;
        this.taxEventDate = taxEventDate;
        this.paymentDeadLine = paymentDeadLine;
        this.invoiceType = invoiceType;
        this.contractOrder = contractOrder;
        this.billingGroupName = billingGroupName;
        this.recipientOfInvoiceNumber = recipientOfInvoiceNumber;
        this.recipientOfInvoiceName = recipientOfInvoiceName;
        this.commForBilling = commsName;
        this.productServiceName = productServiceName;
        this.meterReadingFrom = meterReadingFrom;
        this.meterReadingTo = meterReadingTo;
        this.numberOfIncomeAccount = numberOfIncomeAccount;
        this.basisForIssuing = basisForIssuing;
        this.costCenterControllingOrder = costCenterControllingOrder;
        this.interestRate = interestRate;
        this.directDebit = directDebit;
        this.bank = bank;
        this.bic = bic;
        this.bankAccount = bankAccount;
        this.totalAmountIncludingVat = totalAmountIncludingVat;
        this.totalAmountIncludingVatInOtherCurrency = totalAmountIncludingVatInOtherCurrency;
        this.currencyName = currencyName;
        this.accountingPeriodName = accountingPeriodName;
        this.templateName = templateName;
        //Todo change this to dynamic
        this.issuer = "ENERGO-PRO Energy Services";
        this.statusModifyDate=statusModifyDate.toLocalDate();
        this.compensation=govCompensation;
        this.debitCreditNotes=debitCreditNotes;
    }
}
