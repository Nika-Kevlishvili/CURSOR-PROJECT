package bg.energo.phoenix.model.documentModels;

import bg.energo.phoenix.model.entity.billing.invoice.Invoice;
import bg.energo.phoenix.model.entity.receivable.customerLiability.CustomerLiability;
import bg.energo.phoenix.model.entity.receivable.latePaymentFine.LatePaymentFine;
import bg.energo.phoenix.model.entity.receivable.rescheduling.ReschedulingLiabilities;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Slf4j
public class ReschedulingLiabilityModel {
    public String DocumentNumber;
    public String DocumentPrefix;
    public LocalDate DocumentDate;
    public LocalDate DocumentDueDate;
    public BigDecimal InitialAmount;
    public BigDecimal CurrentAmount;
    public BigDecimal CalculatedInterests;

    public void from(Object[] liabilities) {
        ReschedulingLiabilities reschedulingLiability = (ReschedulingLiabilities) liabilities[0];
        CustomerLiability customerLiability = (CustomerLiability) liabilities[1];
        Invoice invoice = liabilities[2]== null ? null : (Invoice) liabilities[2];
        LatePaymentFine lpf = (LatePaymentFine) liabilities[3];
        String documentNumber = documentNumber = customerLiability.getLiabilityNumber();;
        String documentPrefix = "";
        LocalDate documentDate = null;
        LocalDate dueDate =null;
        documentDate = customerLiability.getCreateDate().toLocalDate();
        dueDate = customerLiability.getDueDate();
        if(invoice!=null) {
            String[] split = invoice.getInvoiceNumber().split("-");
            if (split.length != 2) {
                log.info("Length is not 2 , incorrect data!");
            } else {
                documentPrefix = split[0];
                documentNumber = split[1];
            }
        }
        this.DocumentNumber = documentNumber;
        this.DocumentPrefix = documentPrefix;
        this.DocumentDate = documentDate;
        this.DocumentDueDate = dueDate;
        this.InitialAmount=customerLiability.getInitialAmount();
        this.CurrentAmount = customerLiability.getCurrentAmount();
        this.CalculatedInterests = lpf.getAmount();
    }
}
