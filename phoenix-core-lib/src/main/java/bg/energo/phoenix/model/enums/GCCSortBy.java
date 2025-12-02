package bg.energo.phoenix.model.enums;

import lombok.Getter;

public enum GCCSortBy {
    ID("id"),
    GROUP_NAME("name"),
    CONNECTION_TYPE("type"),
    NUMBER_CONNECTIONS("quantity"),
    MANAGERS("managers"),
    CREATION_DATE("created");

    @Getter
    private String value;

    GCCSortBy(String value) {
        this.value = value;
    }
}
