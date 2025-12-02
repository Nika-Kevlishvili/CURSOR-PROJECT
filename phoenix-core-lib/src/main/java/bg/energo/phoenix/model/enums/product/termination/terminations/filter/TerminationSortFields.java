package bg.energo.phoenix.model.enums.product.termination.terminations.filter;

import lombok.Getter;

public enum TerminationSortFields {
    ID("id"),
    NAME("name"),
    AUTOMATIC_TERMINATION("autoTermination"),
    NOTICE_DUE("noticeDue"),
    AVAILABLE("available"),
    DATE_OF_CREATION("createDate"),
    STATUS("status");

    @Getter
    private final String value;

    TerminationSortFields(String value){this.value = value;}
}
