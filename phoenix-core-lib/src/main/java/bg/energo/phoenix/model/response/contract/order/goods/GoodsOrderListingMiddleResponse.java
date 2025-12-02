package bg.energo.phoenix.model.response.contract.order.goods;

import bg.energo.phoenix.model.entity.EntityStatus;
import bg.energo.phoenix.model.enums.contract.order.goods.GoodsOrderStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public interface GoodsOrderListingMiddleResponse {
    Long getId();

    EntityStatus getStatus();

    GoodsOrderStatus getOrderStatus();

    String getOrderNumber();

    String getCustomer();

    String getGoods();

    String getGoodsSupplier();
    LocalDateTime getOrderCreationDate();

    String getPaymentTerm();

    LocalDate getInvoiceMaturityDate();

    Boolean getInvoicePaid();

    BigDecimal getOrderValue();

    Boolean getIsLockedByInvoice();

}
