package bg.energo.phoenix.model.enums.billing.billings;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ExecutionType {
    MANUAL,
    IMMEDIATELY,
    EXACT_DATE
}
