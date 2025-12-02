package bg.energo.phoenix.model.response.customer.communicationData.detailed;

import bg.energo.phoenix.model.enums.customer.Status;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ContactPersonDetailedResponse {
    private Long id;
    private Long titleId;
    private String titleName;
    private String name;
    private String middleName;
    private String surname;
    private String jobPosition;
    private LocalDate positionHeldFrom;
    private LocalDate positionHeldTo;
    private String birthDate;
    private String additionalInformation;
    private Status status;
    private Long customerCommunicationsId;
}
