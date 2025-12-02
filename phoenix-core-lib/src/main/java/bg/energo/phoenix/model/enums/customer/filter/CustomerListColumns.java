package bg.energo.phoenix.model.enums.customer.filter;

import lombok.Getter;

public enum CustomerListColumns {
    ID("identifier"),
    STATUS("cd.status"),
    TYPE("customerType"),
    NAME("cd.name"),
    ACCOUNT_MANAGER("vwc.displayName"),
    ECONOMIC_BRANCH_BASED_COMMERCIAL_INFO("economic_branch_name"),
    POPULATED_PLACE("populated_place_name"),
    DATE_OF_CREATION("cd.createDate"),
    UNWANTED_CUSTOMER("unwanted_customer_status");

    @Getter
    private String value;

    CustomerListColumns(String value) {
        this.value = value;
    }
}
