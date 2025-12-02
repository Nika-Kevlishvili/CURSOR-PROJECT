package bg.energo.phoenix.service.product.penalty.penaltyGroups;

import bg.energo.phoenix.model.entity.EntityStatus;
import bg.energo.phoenix.model.entity.product.penalty.penaltyGroups.PenaltyGroup;
import bg.energo.phoenix.model.entity.product.penalty.penaltyGroups.PenaltyGroupDetails;
import bg.energo.phoenix.model.entity.product.penalty.penaltyGroups.PenaltyGroupPenalty;
import bg.energo.phoenix.model.entity.product.penalty.penaltyGroups.PenaltyGroupPenaltyQueryResponse;
import bg.energo.phoenix.model.request.product.penalty.penaltyGroup.PenaltyGroupCreateRequest;
import bg.energo.phoenix.model.request.product.penalty.penaltyGroup.PenaltyGroupUpdateRequest;
import bg.energo.phoenix.model.response.penaltyGroup.PenaltyGroupPenaltyResponse;
import bg.energo.phoenix.model.response.penaltyGroup.PenaltyGroupResponse;
import bg.energo.phoenix.model.response.penaltyGroup.PenaltyGroupVersionResponse;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
public class PenaltyGroupMapper {

    public PenaltyGroupResponse toResponse(PenaltyGroup penaltyGroup, PenaltyGroupDetails currentPenaltyGroup, List<? extends PenaltyGroupPenaltyQueryResponse> penaltyGroupPenaltyList,
                                           List<PenaltyGroupDetails> versions) {

        return PenaltyGroupResponse.builder()
                .id(penaltyGroup.getId())
                .name(currentPenaltyGroup.getName())
                .currentVersionId(currentPenaltyGroup.getVersionId())
                .penalties(toPenaltyGroupPenaltyResponse(penaltyGroupPenaltyList))
                .versions(toPenaltyGroupVersionResponse(versions))
                .status(penaltyGroup.getStatus())
                .build();
    }

    private List<PenaltyGroupVersionResponse> toPenaltyGroupVersionResponse(List<PenaltyGroupDetails> versions) {
        var sortedList = versions.stream().sorted(Comparator.comparing(PenaltyGroupDetails::getStartDate)).toList();
        int size = sortedList.size();
        var result = new ArrayList<PenaltyGroupVersionResponse>();
        for (int i = 0; i < size; i++) {
            var penaltyGroup = sortedList.get(i);
            LocalDate endDate = null;
            if(i< size -1) {
                endDate = sortedList.get(i+1).getStartDate();
            }
            result.add(PenaltyGroupVersionResponse.builder()
                    .id(penaltyGroup.getVersionId())
                    .startDate(penaltyGroup.getStartDate())
                    .endDate(endDate)
                    .build());
        }
        return result;
    }

    private List<PenaltyGroupPenaltyResponse> toPenaltyGroupPenaltyResponse(List<? extends PenaltyGroupPenaltyQueryResponse> penaltyGroupPenaltyList) {
        return penaltyGroupPenaltyList.stream().map(penaltyGroupPenalty -> PenaltyGroupPenaltyResponse.builder()
                .id(penaltyGroupPenalty.getId())
                .penaltyId(penaltyGroupPenalty.getPenaltyId())
                .name(penaltyGroupPenalty.getPenaltyName())
                .build()).toList();
    }

    public PenaltyGroupDetails createPenaltyGroupDetails(PenaltyGroupCreateRequest request, PenaltyGroup penaltyGroup) {
        return PenaltyGroupDetails.builder()
                .penaltyGroupId(penaltyGroup.getId())
                .versionId(1)
                .startDate(LocalDate.now())
                .name(request.getName())
                .build();
    }

    public PenaltyGroupPenalty createPenaltyGroupPenalties(PenaltyGroupDetails penaltyGroupDetails, Long penaltyId) {
        return PenaltyGroupPenalty.builder()
                .penaltyGroupDetailId(penaltyGroupDetails.getId())
                .penaltyId(penaltyId)
                .status(EntityStatus.ACTIVE)
                .build();
    }

    public PenaltyGroupDetails createPenaltyGroupDetails(PenaltyGroupUpdateRequest request, PenaltyGroup penaltyGroup, long versionId) {
        return PenaltyGroupDetails.builder()
                .penaltyGroupId(penaltyGroup.getId())
                .versionId((int) versionId)
                .startDate(request.getStartDate())
                .name(request.getName())
                .build();

    }
    public PenaltyGroupDetails fromPenaltyGroupDetails(PenaltyGroupDetails details,Long groupId){
        return PenaltyGroupDetails.builder()
                .penaltyGroupId(groupId)
                .versionId(1)
                .startDate(details.getStartDate())
                .name(details.getName())
                .build();
    }
}
