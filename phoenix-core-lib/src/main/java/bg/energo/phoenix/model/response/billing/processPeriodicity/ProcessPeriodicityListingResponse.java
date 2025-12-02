package bg.energo.phoenix.model.response.billing.processPeriodicity;

import bg.energo.phoenix.model.entity.EntityStatus;
import bg.energo.phoenix.model.enums.billing.processPeriodicity.ProcessPeriodicityType;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ProcessPeriodicityListingResponse {
    private Long id;
    private String name;
    private EntityStatus status;
    private ProcessPeriodicityType periodicity;
    private LocalDateTime createDate;

    public ProcessPeriodicityListingResponse(ProcessPeriodicityListingMiddleResponse processPeriodicityListingMiddleResponse) {
        this.id = processPeriodicityListingMiddleResponse.getId();
        this.name = processPeriodicityListingMiddleResponse.getName();
        this.status = processPeriodicityListingMiddleResponse.getStatus();
        this.periodicity = processPeriodicityListingMiddleResponse.getPeriodicity();
        this.createDate = processPeriodicityListingMiddleResponse.getCreateDate();
    }
}
