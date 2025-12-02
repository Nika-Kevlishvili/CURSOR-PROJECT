package bg.energo.phoenix.model.request.contract.interestRate;

import lombok.Getter;

public enum InterestRateListColumns {
    ID("id"),
    NAME("name"),
    CHARGING("charging"),
    TYPE("type"),
    GROUPING("grouping"),
    IS_DEFAULT("isDefault"),
    CREATE_DATE("createDate"),
    STATUS("status");


    @Getter
    private String value;

    InterestRateListColumns(String value) {
        this.value = value;
    }
}
