package bg.energo.phoenix.model.enums.product.termination.terminationGroup;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum TerminationGroupSearchField {
    ALL("ALL"),
    TERMINATION_GROUP_NAME("TERMINATION_GROUP_NAME"),
    TERMINATION_NAME("TERMINATION_NAME");

    private final String value;
}
