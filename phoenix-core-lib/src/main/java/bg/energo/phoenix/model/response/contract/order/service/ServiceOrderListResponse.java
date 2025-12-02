package bg.energo.phoenix.model.response.contract.order.service;

import bg.energo.phoenix.model.entity.EntityStatus;
import bg.energo.phoenix.model.enums.contract.order.service.ServiceOrderStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public interface ServiceOrderListResponse {
    Long getId();
    String getOrderNumber(); // formatted "number(without prefix)\registration date"
    String getCustomerName();
    String getServiceName();
    LocalDateTime getCreateDate();
    LocalDate getInvoiceMaturityDate();
    Boolean getInvoicePaid();
    String getAccountManager();
    BigDecimal getValueOfTheOrder();
    EntityStatus getStatus();
    ServiceOrderStatus getOrderStatus();
    Boolean getIsLockedByInvoice();
}
