package phoenix.core.customer.model.request.communicationData.contactPerson;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import phoenix.core.customer.model.customAnotations.customer.communicationData.ContactPersonInput;
import phoenix.core.customer.model.customAnotations.customer.manager.AdditionalInformation;
import phoenix.core.customer.model.enums.customer.Status;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class BaseContactPersonRequest {

    // TODO: 14.01.23 change managerinput validation for contact person

    @NotNull(message = "contact person title: must not be null.")
    private Long titleId;

    @NotNull(message = "contact person name: must not be null.")
    @NotBlank(message = "contact person name: must not be blank.")
    @ContactPersonInput(value = "contact person name")
    @Size(min = 1, max = 512, message = "contact person name: size should be between {min} and {max} symbols.")
    private String name;

    @ContactPersonInput(value = "contact person middleName")
    @Size(min = 1, max = 512, message = "contact person middleName: size should be between {min} and {max} symbols.")
    private String middleName;

    @NotNull(message = "contact person surname: must not be null.")
    @NotBlank(message = "contact person surname: must not be blank.")
    @ContactPersonInput(value = "contact person surname")
    @Size(min = 1, max = 512, message = "contact person surname: size should be between {min} and {max} symbols.")
    private String surname;

    @NotNull(message = "contact person jobPosition: must not be null.")
    @NotBlank(message = "contact person jobPosition: must not be blank.")
    @ContactPersonInput(value = "contact person jobPosition")
    @Size(min = 1, max = 512, message = "contact person jobPosition: size should be between {min} and {max} symbols.")
    private String jobPosition;

    @JsonFormat(pattern = "dd.MM.yyyy")
    private LocalDate positionHeldFrom;

    @JsonFormat(pattern = "dd.MM.yyyy")
    private LocalDate positionHeldTo;

    @JsonFormat(pattern = "dd.MM.yyyy")
    private LocalDate birthDate;;

    // TODO: 04.01.23 add new line character
    @AdditionalInformation(value = "contact person additionalInformation")
    @Size(min = 1, max = 2048, message = "contact person additionalInformation: size should be between {min} and {max} symbols.")
    private String additionalInformation;

    // TODO: 03.01.23 system user id should be added later

    @NotNull(message = "contact person status: must not be null.")
    private Status status;

}
