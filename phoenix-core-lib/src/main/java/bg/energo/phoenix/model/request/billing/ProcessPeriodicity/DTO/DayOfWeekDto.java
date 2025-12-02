package bg.energo.phoenix.model.request.billing.ProcessPeriodicity.DTO;

import bg.energo.phoenix.model.request.billing.ProcessPeriodicity.baseDTO.BaseDayOfWeekDto;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = false)
public class DayOfWeekDto extends BaseDayOfWeekDto {
    private Long id;
}
