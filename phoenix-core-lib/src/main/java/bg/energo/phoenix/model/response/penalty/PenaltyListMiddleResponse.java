package bg.energo.phoenix.model.response.penalty;

import bg.energo.phoenix.model.entity.EntityStatus;
import bg.energo.phoenix.model.entity.product.penalty.penalty.PenaltyApplicability;

import java.time.LocalDateTime;

public interface PenaltyListMiddleResponse {
    Long getId();

    String getName();

    String getPartyReceivingPenalties();

    PenaltyApplicability getApplicability();

    String getAvailable();

    EntityStatus getStatus();

    LocalDateTime getCreateDate();
}
