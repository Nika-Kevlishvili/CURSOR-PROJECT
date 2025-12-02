package bg.energo.phoenix.service.billing.billingRunProcess.enums;

import bg.energo.phoenix.model.entity.billing.billingRun.BillingRun;

import java.util.Objects;

public enum BillingRunObjectType {
    ONLY_CUSTOMER,
    PRODUCT_CONTRACT,
    SERVICE_CONTRACT,
    GOODS_ORDER,
    SERVICE_ORDER;

    public static BillingRunObjectType defineInvoiceObjectType(BillingRun billingRun) {
        if (Objects.nonNull(billingRun.getProductContractId())) {
            return PRODUCT_CONTRACT;
        } else if (Objects.nonNull(billingRun.getServiceContractId())) {
            return SERVICE_CONTRACT;
        } else if (Objects.nonNull(billingRun.getGoodsOrderId())) {
            return GOODS_ORDER;
        } else if (Objects.nonNull(billingRun.getServiceOrderId())) {
            return SERVICE_ORDER;
        } else if (Objects.nonNull(billingRun.getCustomerDetailId())) {
            return ONLY_CUSTOMER;
        }

        throw new RuntimeException("Cannot define billingRun object type");
    }
}
