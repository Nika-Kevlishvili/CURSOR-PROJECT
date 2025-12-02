package bg.energo.phoenix.model.enums.billing.billings;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ApplicationModelType {
    FOR_VOLUMES,
    OVER_TIME_PERIODICAL,
    OVER_TIME_ONE_TIME,
    PER_PIECE,
    INTERIM_AND_ADVANCE_PAYMENT,
    WITH_ELECTRICITY_INVOICE
}
