package bg.energo.migration.integration.phoenix.customer.model.request.communicationdata.contactpersons;

import bg.energo.migration.integration.phoenix.customer.model.enums.Status;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateContactPersonRequest {
    private Long titleId;
    private String name;
    private String middleName;
    private String surname;
    private String jobPosition;
    private LocalDate positionHeldFrom;
    private LocalDate positionHeldTo;
    private String birthDate;
    private String additionalInformation;
    private Status status;
}
