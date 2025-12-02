package bg.energo.phoenix.model.response.customer.list;

import java.math.BigDecimal;
import java.time.LocalDate;

public interface CustomerRelatedOrderListResponse {

    Long getOrderId();

    String getStatus();

    String getOrderNumber();

    String getOrderType();

    String getOrderStatus();

    LocalDate getCreationDate();

    LocalDate getInvoiceMaturityDate();

    Boolean getInvoicePaid();

    BigDecimal getOrderValue();

    String getInvoicePaymentTerm();

}
