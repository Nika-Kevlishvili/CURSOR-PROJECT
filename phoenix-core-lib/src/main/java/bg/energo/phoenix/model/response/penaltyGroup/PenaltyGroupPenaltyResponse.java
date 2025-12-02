package bg.energo.phoenix.model.response.penaltyGroup;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PenaltyGroupPenaltyResponse {
    private Long id;
    private Long penaltyId;
    private String name;
}
