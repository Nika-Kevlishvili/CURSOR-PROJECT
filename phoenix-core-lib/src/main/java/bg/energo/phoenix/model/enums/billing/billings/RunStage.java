package bg.energo.phoenix.model.enums.billing.billings;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum RunStage {
    GENERATE_AND_SIGN,
    AUTOMATICALLY_ACCOUNTING
}
