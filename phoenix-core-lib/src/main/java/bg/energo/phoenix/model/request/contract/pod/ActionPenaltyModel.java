package bg.energo.phoenix.model.request.contract.pod;

import lombok.AllArgsConstructor;
import lombok.Data;

@AllArgsConstructor
@Data
public class ActionPenaltyModel {

    private Long actionId;
    private Long penaltyId;
    private Long podId;
}
