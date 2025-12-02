package bg.energo.phoenix.model.request.billing.ProcessPeriodicity.baseDTO;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BaseProcessPeriodicityTimeIntervalDto {

    @DateTimeFormat(pattern = "hh:mm")
    private LocalTime startTime;

    @DateTimeFormat(pattern = "hh:mm")
    private LocalTime endTime;
}