package bg.energo.phoenix.model.enums.product.termination.terminations.filter;

import lombok.Getter;

public enum TerminationSearchFields {
    ALL("ALL"),
    NAME("NAME");

    @Getter
    private final String value;

    TerminationSearchFields(String value){this.value = value;}
}
