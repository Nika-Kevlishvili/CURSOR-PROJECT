package bg.energo.migration.integration.phoenix.customer.model.request.managers;

import bg.energo.migration.integration.phoenix.customer.model.enums.Status;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateManagerRequest {
    private Long titleId;
    private String name;
    private String middleName;
    private String surname;
    private String personalNumber;
    private String jobPosition;
    private LocalDate positionHeldFrom;
    private LocalDate positionHeldTo;
    private String birthDate;
    private Long representationMethodId;
    private String additionalInformation;
    private Status status;
}
