package bg.energo.phoenix.model.enums.billing.billings;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum SendingAnInvoice {
    ACCORDING_TO_THE_CONTRACT,
    EMAIL,
    PAPER
}
