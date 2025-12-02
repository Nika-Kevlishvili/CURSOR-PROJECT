package bg.energo.phoenix.model.request.pod.billingByScales;

import lombok.Getter;

public enum BillingByScaleListSearchBy {
    ALL("ALL"),
    ID("ID"),
    POD_IDENTIFIER("POD_IDENTIFIER");

    @Getter
    private String value;

    BillingByScaleListSearchBy(String value) {
        this.value = value;
    }
}
