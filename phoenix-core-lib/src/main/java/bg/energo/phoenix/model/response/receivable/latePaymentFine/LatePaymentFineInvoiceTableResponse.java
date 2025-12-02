package bg.energo.phoenix.model.response.receivable.latePaymentFine;

import bg.energo.phoenix.model.entity.receivable.latePaymentFine.LatePaymentFineInvoices;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class LatePaymentFineInvoiceTableResponse {
    private Long id;
    private Long invoiceId;
    private String invoiceNumber;
    private BigDecimal latePaidAmount;
    private String overdueStartDate;
    private String overdueEndDate;
    private Long numberOfDays;
    private BigDecimal percentage;
    private BigDecimal totalAmount;
    private Long latePaymentFineId;
    private BigDecimal fee;


    public LatePaymentFineInvoiceTableResponse(LatePaymentFineInvoices latePaymentFineInvoices) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy");
        this.id = latePaymentFineInvoices.getId();
        this.invoiceId = latePaymentFineInvoices.getInvoiceId();
        this.invoiceNumber = latePaymentFineInvoices.getInvoiceNumber();
        this.latePaidAmount = latePaymentFineInvoices.getLatePaidAmount();
        this.overdueStartDate = latePaymentFineInvoices.getOverdueStartDate().format(formatter);
        this.overdueEndDate = latePaymentFineInvoices.getOverdueEndDate().format(formatter);
        this.numberOfDays = latePaymentFineInvoices.getNumberOfDays();
        this.percentage = latePaymentFineInvoices.getPercentage();
        this.totalAmount = latePaymentFineInvoices.getTotalAmount();
        this.latePaymentFineId = latePaymentFineInvoices.getLatePaymentFineId();
        this.fee = latePaymentFineInvoices.getFee();
    }
}
