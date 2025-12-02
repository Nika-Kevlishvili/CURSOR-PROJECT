package bg.energo.phoenix.model.response.penaltyGroup;

import bg.energo.phoenix.model.entity.EntityStatus;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class PenaltyGroupResponse {
    private Long id;
    private String name;
    private Integer currentVersionId;
    private List<PenaltyGroupPenaltyResponse> penalties;
    private List<PenaltyGroupVersionResponse> versions;
    private EntityStatus status;
    private Boolean isLocked;
}
