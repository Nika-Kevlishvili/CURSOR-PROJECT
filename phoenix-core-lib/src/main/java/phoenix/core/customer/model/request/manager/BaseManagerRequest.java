package phoenix.core.customer.model.request.manager;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import phoenix.core.customer.model.customAnotations.customer.manager.AdditionalInformation;
import phoenix.core.customer.model.customAnotations.customer.manager.ManagerInput;
import phoenix.core.customer.model.customAnotations.customer.manager.PersonalNumber;
import phoenix.core.customer.model.customAnotations.customer.manager.ValidManagerRequest;
import phoenix.core.customer.model.enums.customer.Status;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.time.LocalDate;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ValidManagerRequest
public class BaseManagerRequest {

    @NotNull(message = "manager title: must not be null.")
    private Long titleId;

    @NotNull(message = "manager name: must not be null.")
    @NotBlank(message = "manager name: must not be blank.")
    @ManagerInput(value = "manager name")
    @Size(min = 1, max = 512, message = "manager name: size should be between {min} and {max} symbols.")
    private String name;

    @ManagerInput(value = "manager middleName")
    @Size(min = 1, max = 512, message = "manager middleName: size should be between {min} and {max} symbols.")
    private String middleName;

    @NotNull(message = "manager surname: must not be null.")
    @NotBlank(message = "manager surname: must not be blank.")
    @ManagerInput(value = "manager surname")
    @Size(min = 1, max = 512, message = "manager surname: size should be between {min} and {max} symbols.")
    private String surname;

    @PersonalNumber(message = "manager personalNumber: length must be 10 or 12 digits.")
    private String personalNumber;

    @NotNull(message = "manager jobPosition: must not be null.")
    @NotBlank(message = "manager jobPosition: must not be blank.")
    @ManagerInput(value = "manager jobPosition")
    @Size(min = 1, max = 512, message = "manager jobPosition: size should be between {min} and {max} symbols.")
    private String jobPosition;

    @JsonFormat(pattern = "dd.MM.yyyy")
    private LocalDate positionHeldFrom;

    @JsonFormat(pattern = "dd.MM.yyyy")
    private LocalDate positionHeldTo;

    @JsonFormat(pattern = "dd.MM.yyyy")
    private LocalDate birthDate;

    @NotNull(message = "manager representationMethod: must not be null.")
    private Long representationMethodId;

    // TODO: 04.01.23 add new line character
    @AdditionalInformation(value = "manager additionalInformation")
    @Size(min = 1, max = 2048, message = "manager additionalInformation: size should be between {min} and {max} symbols.")
    private String additionalInformation;

    // TODO: 03.01.23 system user id should be added later

    @NotNull(message = "manager status: must not be null.")
    private Status status;

}
