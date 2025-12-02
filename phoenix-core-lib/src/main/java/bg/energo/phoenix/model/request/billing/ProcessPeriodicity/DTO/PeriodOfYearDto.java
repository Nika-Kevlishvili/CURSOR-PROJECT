package bg.energo.phoenix.model.request.billing.ProcessPeriodicity.DTO;

import bg.energo.phoenix.model.request.billing.ProcessPeriodicity.baseDTO.BasePeriodOfYearDto;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = false)
public class PeriodOfYearDto extends BasePeriodOfYearDto {
    private Long id;
}
