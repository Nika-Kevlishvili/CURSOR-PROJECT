package bg.energo.phoenix.model.response.penalty;

import bg.energo.phoenix.model.entity.product.penalty.penalty.Penalty;
import lombok.Data;

@Data
public class PenaltyShortResponse {

    private Long id;
    private String name;

    public PenaltyShortResponse(Penalty penalty) {
        this.id = penalty.getId();
        this.name = "%s (%s)".formatted(penalty.getName(), penalty.getId());
    }
}
