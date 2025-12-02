package bg.energo.phoenix.model.response.penaltyGroup;

import bg.energo.phoenix.model.entity.EntityStatus;

import java.time.LocalDateTime;

public interface PenaltyGroupListResponse {
    Long getId();

    String getName();

    Integer getNumPenalties();

    EntityStatus getStatus();

    LocalDateTime getDateOfCreation();
}
