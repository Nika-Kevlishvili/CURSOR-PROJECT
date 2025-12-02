package bg.energo.phoenix.model.enums.product.termination.terminationGroup;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum TerminationGroupTableColumn {
    TG_ID("groupId"),
    TG_DATE_OF_CREATION("dateOfCreation"),
    TG_NAME("name"),
    NUM_OF_TERMINATIONS("numberOfTerminations");

    private final String value;
}
