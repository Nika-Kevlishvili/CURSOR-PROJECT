package bg.energo.phoenix.model.request.pod.meter;

import bg.energo.phoenix.model.customAnotations.pod.meter.ValidMeterInstallmentRange;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@ValidMeterInstallmentRange
public class MeterRequest {

    @NotBlank(message = "number-Number should not be blank;")
    @Size(min = 1, max = 32, message = "number-Number should be between {min} and {max} characters;")
    @Pattern(regexp = "^[0-9A-Za-z]+$", message = "number-Allowed symbols in number are: A-Z a-z 0-9;")
    private String number;

    @NotNull(message = "gridOperatorId-Grid operator ID is required;")
    private Long gridOperatorId;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    @NotNull(message = "installmentDate-Installment date is required;")
    private LocalDate installmentDate;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate removeDate;

    @NotNull(message = "podId-Pod ID is required;")
    private Long podId;

    @NotEmpty(message = "meterScaleIds-Meter scales should not be empty;")
    private List<Long> meterScales;

    private boolean warningAcceptedByUser;

}
