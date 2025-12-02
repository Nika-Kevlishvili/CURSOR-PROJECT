package bg.energo.phoenix.model.enums.pod.meter;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum MeterTableColumn {
    ID("id"),
    NUMBER("number"),
    POD_IDENTIFIER("pod.identifier"),
    GRID_OPERATOR_NAME("go.name"),
    INSTALLMENT_DATE("installmentDate"),
    REMOVE_DATE("removeDate"),
    STATUS("status"),
    DATE_OF_CREATION("createDate");

    private final String value;
}
