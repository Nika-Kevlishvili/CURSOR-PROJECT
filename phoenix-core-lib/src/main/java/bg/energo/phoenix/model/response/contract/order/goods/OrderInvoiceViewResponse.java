package bg.energo.phoenix.model.response.contract.order.goods;

import bg.energo.phoenix.model.enums.billing.invoice.InvoiceDocumentType;
import bg.energo.phoenix.model.enums.billing.invoice.InvoiceType;

import java.math.BigDecimal;
import java.time.LocalDate;


public interface OrderInvoiceViewResponse {
    Long getId();

    String getInvoiceNumber();

    InvoiceDocumentType getInvoiceDocumentType();

    InvoiceType getInvoiceType();

    LocalDate getInvoiceDate();

    String getCustomer();

    String getAccountingPeriod();

    String getBasisForIssuing();

    BigDecimal getTotalAmount();

}
