package bg.energo.phoenix.model.response.contract.order.goods;

import bg.energo.phoenix.model.entity.EntityStatus;
import bg.energo.phoenix.model.enums.contract.order.goods.GoodsOrderStatus;
import lombok.Data;

import java.math.RoundingMode;
import java.time.LocalDate;

@Data
public class GoodsOrderListingResponse {
    private Long id;
    private EntityStatus status;
    private GoodsOrderStatus orderStatus;
    private String orderNumber;
    private String customer;
    private String goods;
    private String goodsSupplier;
    private LocalDate orderCreationDate;
    private String paymentTerm;
    private LocalDate invoiceMaturityDate;
    private Boolean invoicePaid;
    private String orderValue;
    private Boolean isLockedByInvoice;

    public GoodsOrderListingResponse(GoodsOrderListingMiddleResponse response) {
        this.id = response.getId();
        this.status = response.getStatus();
        this.orderStatus = response.getOrderStatus();
        this.orderNumber = response.getOrderNumber();
        this.customer = response.getCustomer();
        this.goods = response.getGoods();
        this.isLockedByInvoice = response.getIsLockedByInvoice();
        this.goodsSupplier = response.getGoodsSupplier();
        this.orderCreationDate = response.getOrderCreationDate().toLocalDate();
        this.paymentTerm = response.getPaymentTerm();
        this.invoiceMaturityDate = response.getInvoiceMaturityDate();
        this.invoicePaid = response.getInvoicePaid();
        if (response.getOrderValue() != null) {
            this.orderValue = response.getOrderValue().setScale(2, RoundingMode.HALF_DOWN).toString();
        }
    }
}
