package bg.energo.phoenix.model.response.terminationGroup;

import bg.energo.phoenix.model.enums.product.termination.terminationGroup.TerminationGroupStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TerminationGroupResponse {

    private Long groupId;

    private TerminationGroupStatus status;

    private Long groupDetailId;

    private Long versionId;

    private String name;

    private List<TerminationGroupVersion> versions;

    private List<TerminationGroupTerminationResponse> terminationsList;

    private Boolean isLocked;

}
