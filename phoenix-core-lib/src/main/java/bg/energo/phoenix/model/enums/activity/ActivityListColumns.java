package bg.energo.phoenix.model.enums.activity;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ActivityListColumns {
    ID("id"),
    ACTIVITY("a2.name"),
    SUB_ACTIVITY("sa.name"),
    CONNECTION_TYPE("connectionType"),
    CREATE_DATE("createDate");

    @Getter
    private final String value;
}
