package bg.energo.phoenix.model.entity.product.penalty.penalty;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum PenaltyTableColumn {
    ID("id"),
    NAME("name"),
    PARTY_RECEIVING("partyReceivingPenalties"),
    AVAILABLE("available"),
    APPLICABILITY("applicability"),
    DATE_OF_CREATION("createDate");

    private final String value;

}
