package bg.energo.phoenix.model.request.receivable.objectionWithdrawalToAChangeOfABalancingGroupCoordinator;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Data
public class CreateObjectionWithdrawalToAChangeOfABalancingGroupCoordinatorRequest {

    @NotNull(message = "objectionToChangeOfACbgId-Objection To Change Of A Cbg should not be null;")
    private Long objectionToChangeOfACbgId;



    public List<Long> taskIds;

    @NotNull(message = "emailTemplateId-Email Template should not be null;")
    private Long emailTemplateId;
    @NotEmpty(message = "templateIds-Template ids should be provided!;")
    private Set<Long> templateIds;

    private List<Long> fileIds = new ArrayList<>();
}
