package bg.energo.phoenix.model.response.pod.meter;

import bg.energo.phoenix.model.enums.pod.meter.MeterStatus;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class MeterListResponse {

    private Long id;
    private String number;
    private String podIdentifier;
    private String gridOperatorName;
    private LocalDate installmentDate;
    private LocalDate removeDate;
    private MeterStatus status;
    private LocalDateTime dateOfCreation;

}
