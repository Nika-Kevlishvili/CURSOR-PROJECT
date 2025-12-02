package bg.energo.phoenix.billingRun.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class VolumeDateRange {
    private LocalDate from;
    private LocalDate to;
}
