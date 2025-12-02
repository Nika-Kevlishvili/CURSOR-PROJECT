package bg.energo.phoenix.service.billing.invoice.enums;

import bg.energo.phoenix.model.entity.billing.invoice.Invoice;
import lombok.Getter;

import java.util.Objects;

@Getter
public enum InvoiceObjectType {
    PRODUCT_CONTRACT,
    SERVICE_CONTRACT,
    GOODS_ORDER,
    SERVICE_ORDER,
    ONLY_CUSTOMER;

    public static InvoiceObjectType defineInvoiceObjectType(Invoice invoice) {
        if (Objects.nonNull(invoice.getProductContractId())) {
            return PRODUCT_CONTRACT;
        } else if (Objects.nonNull(invoice.getServiceContractId())) {
            return SERVICE_CONTRACT;
        } else if (Objects.nonNull(invoice.getGoodsOrderId())) {
            return GOODS_ORDER;
        } else if (Objects.nonNull(invoice.getServiceOrderId())) {
            return SERVICE_ORDER;
        }else if (Objects.nonNull(invoice.getCustomerDetailId())) {
            return ONLY_CUSTOMER;
        }

        throw new RuntimeException("Cannot define invoice object type");
    }

}
