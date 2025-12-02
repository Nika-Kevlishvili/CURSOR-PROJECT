package bg.energo.phoenix.model.request.receivable.massOperationForBlocking;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ReceivableBlockingListingSearchField {

    ALL("ALL"),

    ID("ID"),

    NAME("NAME"),

    TYPE("TYPE"),

    STATUS("STATUS");

    private final String value;

}
