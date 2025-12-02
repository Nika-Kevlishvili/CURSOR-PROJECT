package bg.energo.phoenix.model.entity.product.penalty.penaltyGroups;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum PenaltyGroupSearchField {
    ALL("ALL"),
    PENALTY_GROUP_NAME("PENALTY_GROUP_NAME"),
    PENALTY_NAME("PENALTY_NAME");

    private final String value;
}
