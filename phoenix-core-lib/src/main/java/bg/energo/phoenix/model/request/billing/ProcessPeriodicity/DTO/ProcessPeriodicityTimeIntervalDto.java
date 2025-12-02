package bg.energo.phoenix.model.request.billing.ProcessPeriodicity.DTO;

import bg.energo.phoenix.model.request.billing.ProcessPeriodicity.baseDTO.BaseProcessPeriodicityTimeIntervalDto;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = false)
public class ProcessPeriodicityTimeIntervalDto extends BaseProcessPeriodicityTimeIntervalDto {
    private Long id;
}
