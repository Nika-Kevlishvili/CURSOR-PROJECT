package bg.energo.phoenix.model.enums.contract.order.goods;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
public enum GoodsOrderListingSortFields {
    NUMBER("(orderNumber)"),
    CUSTOMER("(customer)"),
    GOODS("(goods)"),
    GOODS_SUPPLIER("(goodsSupplier)"),
    DATE_OF_ORDER_CREATION("(orderCreationDate)"),
    INVOICE_PAYMENT_TERM("(paymentTerm)"),
    INVOICE_MATURITY_DATE("(invoiceMaturityDate)"),
    INVOICE_PAYED("(invoicePaid)"),
    VALUE_OF_THE_ORDER("(orderValue)");

    @Getter
    private final String column;
}
