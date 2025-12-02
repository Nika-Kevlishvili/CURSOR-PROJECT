package bg.energo.phoenix.model.entity.product.penalty.penaltyGroups;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum PenaltyGroupTableColumn {
    ID("id"),
    GROUP_DATE_OF_CREATION("dateOfCreation"),
    GROUP_NAME("name"),
    NUM_OF_PENALTIES("numPenalties");

    private final String value;
}
