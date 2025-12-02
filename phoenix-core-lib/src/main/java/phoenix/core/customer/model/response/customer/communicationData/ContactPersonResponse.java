package phoenix.core.customer.model.response.customer.communicationData;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import phoenix.core.customer.model.enums.customer.Status;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ContactPersonResponse {
    private Long id;
    private Long titleId;
    private String titleName;
    private String name;
    private String middleName;
    private String surname;
    private String jobPosition;
    private LocalDate positionHeldFrom;
    private LocalDate positionHeldTo;
    private LocalDate birthDate;
    private String additionalInfo;
    private Status status;
}
