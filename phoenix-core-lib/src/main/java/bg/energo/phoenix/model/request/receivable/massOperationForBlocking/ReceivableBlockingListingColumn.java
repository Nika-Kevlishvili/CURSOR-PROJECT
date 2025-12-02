package bg.energo.phoenix.model.request.receivable.massOperationForBlocking;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ReceivableBlockingListingColumn {
    ID("id"),

    NAME("name"),

    TYPE("type"),

    STATUS("mass_operation_blocking_status");

    private final String value;
}
