package bg.energo.phoenix.billingRun.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ScaleSplitDate {
    private LocalDate date;
    private Boolean isStart;
}
