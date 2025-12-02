package bg.energo.phoenix.model.request.receivable.objectionWithdrawalToAChangeOfABalancingGroupCoordinator;

import bg.energo.phoenix.model.enums.receivable.balancingGroupCoordinatorObjection.ChangeOfCbgCreateStatus;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ObjectionWithdrawalToAChangeOfCBGBaseRequest {
    @NotNull(message = "objectionToChangeOfACbgId-Objection To Change Of A Cbg should not be null;")
    private Long objectionToChangeOfACbgId;

    @NotNull(message = "emailTemplateId-Email Template should not be null;")
    private Long emailTemplateId;

    @NotEmpty(message = "templateIds-Template ids should be provided!;")
    private Set<Long> templateIds;

    private List<Long> fileIds = new ArrayList<>();

    private ChangeOfCbgCreateStatus saveAs;

}
