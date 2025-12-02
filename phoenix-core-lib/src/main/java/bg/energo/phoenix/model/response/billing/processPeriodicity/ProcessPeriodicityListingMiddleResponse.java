package bg.energo.phoenix.model.response.billing.processPeriodicity;

import bg.energo.phoenix.model.entity.EntityStatus;
import bg.energo.phoenix.model.enums.billing.processPeriodicity.ProcessPeriodicityType;

import java.time.LocalDateTime;

public interface ProcessPeriodicityListingMiddleResponse {
    Long getId();

    String getName();

    EntityStatus getStatus();

    ProcessPeriodicityType getPeriodicity();

    LocalDateTime getCreateDate();
}
