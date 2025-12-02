package bg.energo.phoenix.model.request.receivable.rescheduling;

import lombok.Getter;

public enum ReschedulingSearchFields {

    ALL("ALL"),
    NUMBER("RESCHEDULING_NUMBER"),
    CUSTOMER("CUSTOMER");

    @Getter
    private String value;

    ReschedulingSearchFields(String value) {
        this.value = value;
    }
}
