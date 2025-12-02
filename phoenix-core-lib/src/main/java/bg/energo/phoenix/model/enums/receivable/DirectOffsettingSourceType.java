package bg.energo.phoenix.model.enums.receivable;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum DirectOffsettingSourceType {
    RECEIVABLE("RECEIVABLE"),
    PAYMENT("PAYMENT"),
    DEPOSIT("DEPOSIT"),
    LIABILITY("LIABILITY"),
    RESCHEDULING("RESCHEDULING");

    private final String value;
}
