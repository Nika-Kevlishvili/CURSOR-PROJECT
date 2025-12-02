package bg.energo.phoenix.model.enums.customer.filter;

import lombok.Getter;

public enum UnwantedCustomerListingStatus {
    ALL("ALL"),
    YES("YES"),
    NO("NO");
    @Getter
    private String value;

    UnwantedCustomerListingStatus(String value) {
        this.value = value;
    }
}
