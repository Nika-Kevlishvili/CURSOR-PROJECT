package bg.energo.phoenix.model.request.billing.ProcessPeriodicity.listing;

import bg.energo.phoenix.model.entity.EntityStatus;
import bg.energo.phoenix.model.enums.billing.processPeriodicity.ProcessPeriodicityType;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.data.domain.Sort;

import java.util.List;

@Data
public class ProcessPeriodicityListingRequest {
    private String prompt;

    private List<ProcessPeriodicityType> periodicity;

    private ProcessPeriodicityListColumns sortBy;

    @NotNull(message = "page-Page must not be null;")
    private Integer page;

    @NotNull(message = "size-Size must not be null;")
    private Integer size;

    private ProcessPeriodicitySearchByEnums searchBy;

    private Sort.Direction direction;

    private List<EntityStatus> statuses;
}
