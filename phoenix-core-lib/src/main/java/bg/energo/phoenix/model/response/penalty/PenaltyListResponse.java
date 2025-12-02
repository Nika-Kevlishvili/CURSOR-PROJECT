package bg.energo.phoenix.model.response.penalty;

import bg.energo.phoenix.model.entity.EntityStatus;
import bg.energo.phoenix.model.entity.product.penalty.penalty.PartyReceivingPenalty;
import bg.energo.phoenix.model.entity.product.penalty.penalty.PenaltyApplicability;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
public class PenaltyListResponse {
    private Long id;
    private String name;
    private List<PartyReceivingPenalty> partyReceivingPenaltySet;
    private PenaltyApplicability penaltyApplicability;
    private String available;
    private EntityStatus status;
    private LocalDateTime createDate;

    public PenaltyListResponse(PenaltyListMiddleResponse middleResponse) {
        this.id = middleResponse.getId();
        this.name = middleResponse.getName();
        this.partyReceivingPenaltySet = middleResponse.getPartyReceivingPenalties() == null
                ? null
                : Arrays.stream(
                        middleResponse
                                .getPartyReceivingPenalties()
                                .replace("{", "")
                                .replace("}", "")
                                .split(",")
                )
                .map(PartyReceivingPenalty::valueOf)
                .toList();
        this.penaltyApplicability = middleResponse.getApplicability();
        this.available = middleResponse.getAvailable();
        this.status = middleResponse.getStatus();
        this.createDate = middleResponse.getCreateDate();
    }
}
