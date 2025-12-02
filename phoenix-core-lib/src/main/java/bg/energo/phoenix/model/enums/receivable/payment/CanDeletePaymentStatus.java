package bg.energo.phoenix.model.enums.receivable.payment;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum CanDeletePaymentStatus {
    CAN_DELETE("CAN_DELETE"),
    CAN_DELETE_WITH_MESSAGE("CAN_DELETE_WITH_MESSAGE"),
    CANNOT_DELETE("CANNOT_DELETE");

    private final String message;
}
