package phoenix.core.customer.model.enums.customer;

import lombok.Getter;

public enum CustomerListColumns {
    ID("identifier"),
    STATUS("status"),
    TYPE("customerType"),
    NAME("cd.name"),
    //ACCOUNT_MANAGER(""),
    ECONOMIC_BRANCH_BASED_COMMERCIAL_INFO("cd.economicBranchCiId"),
    POPULATED_PLACE("cd.populatedPlaceId"),
    DATE_OF_CREATION("cd.createDate");
    //UNWANTED_CUSTOMER;

    @Getter
    private String value;

    CustomerListColumns(String value) {
        this.value = value;
    }
}
