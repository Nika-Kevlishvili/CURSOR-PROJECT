package bg.energo.phoenix.model.request.billing.ProcessPeriodicity.DTO;

import bg.energo.phoenix.model.request.billing.ProcessPeriodicity.baseDTO.BaseDateOfMonthDto;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = false)
public class DateOfMonthDto extends BaseDateOfMonthDto {
    private Long id;
}
