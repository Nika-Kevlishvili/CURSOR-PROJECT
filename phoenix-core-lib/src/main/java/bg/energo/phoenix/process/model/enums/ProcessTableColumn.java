package bg.energo.phoenix.process.model.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ProcessTableColumn {
    ID("id"),
    NAME("name"),
    STATUS("status"),
    CREATE_DATE("createDate"),
    START_DATE("processStartDate"),
    COMPLETE_DATE("processCompleteDate"),
    START_AFTER_PROCESS("startAfterProcess"),
    POSTPONED_START("postponedStart"),
    TYPE("type");

    @Getter
    private final String value;
}
