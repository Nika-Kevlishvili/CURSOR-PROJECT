package bg.energo.phoenix.model.enums.receivable.offsetting;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ObjectOffsettingType {

    LIABILITY("Liability"),
    RECEIVABLE("Receivable"),
    PAYMENT("Payment"),
    DEPOSIT("Deposit"),
    RESCHEDULING("Rescheduling");

    private final String value;
}
