package bg.energo.phoenix.model.enums.product.price.priceComponent;

import lombok.Getter;

@Getter
public enum IssuedSeparateInvoice {
    INVOICE_ONE(1),
    INVOICE_TWO(2),
    INVOICE_THREE(3),
    INVOICE_FOUR(4);
    private final Integer numberToAdd;

    public static Integer getMaxValue() {
        return 4;
    }

    IssuedSeparateInvoice(Integer numberToAdd) {
        this.numberToAdd = numberToAdd;
    }
}
