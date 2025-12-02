package bg.energo.phoenix.service.product.termination.terminationGroup;

import bg.energo.phoenix.model.entity.product.termination.terminationGroup.TerminationGroup;
import bg.energo.phoenix.model.entity.product.termination.terminationGroup.TerminationGroupDetails;
import bg.energo.phoenix.model.response.terminationGroup.TerminationGroupResponse;
import bg.energo.phoenix.model.response.terminationGroup.TerminationGroupTerminationResponse;
import bg.energo.phoenix.model.response.terminationGroup.TerminationGroupVersion;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TerminationGroupMapper {

    public TerminationGroupResponse terminationGroupResponseFromEntity(TerminationGroup group,
                                                                       TerminationGroupDetails details,
                                                                       List<TerminationGroupVersion> versions,
                                                                       List<TerminationGroupTerminationResponse> terminationsList) {
        return TerminationGroupResponse.builder()
                .groupId(details.getTerminationGroupId())
                .groupDetailId(details.getId())
                .name(details.getName())
                .versionId(details.getVersionId())
                .status(group.getStatus())
                .versions(versions)
                .terminationsList(terminationsList)
                .build();
    }

    public TerminationGroupDetails fromTerminationGroupDetails(TerminationGroupDetails details, Long groupId) {
        TerminationGroupDetails terminationGroupDetails = new TerminationGroupDetails();
        terminationGroupDetails.setTerminationGroupId(groupId);
        terminationGroupDetails.setVersionId(1L);
        terminationGroupDetails.setStartDate(details.getStartDate());
        terminationGroupDetails.setName(details.getName());
        return terminationGroupDetails;

    }

}
