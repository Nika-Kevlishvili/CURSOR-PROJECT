package bg.energo.phoenix.process.model.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ProcessSearchField {
    ALL("all"),
    NAME("name");

    private final String value;
}
