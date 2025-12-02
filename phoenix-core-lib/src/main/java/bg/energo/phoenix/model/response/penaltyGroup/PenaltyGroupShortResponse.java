package bg.energo.phoenix.model.response.penaltyGroup;

import bg.energo.phoenix.model.entity.product.penalty.penaltyGroups.PenaltyGroupDetails;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class PenaltyGroupShortResponse {

    private Long id;
    private String name;

    public PenaltyGroupShortResponse(PenaltyGroupDetails penaltyGroupDetails) {
        Long penaltyGroupId = penaltyGroupDetails.getPenaltyGroupId();
        this.id = penaltyGroupId;
        this.name = "%s (%s)".formatted(penaltyGroupDetails.getName(), penaltyGroupId);
    }
}
