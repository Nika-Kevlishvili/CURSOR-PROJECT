package bg.energo.phoenix.model.request.customer.communicationData.contactPerson;

import bg.energo.phoenix.model.customAnotations.customer.communicationData.ContactPersonInput;
import bg.energo.phoenix.model.customAnotations.customer.communicationData.ValidContactPersonRequest;
import bg.energo.phoenix.model.customAnotations.customer.manager.AdditionalInformation;
import bg.energo.phoenix.model.customAnotations.customer.withValidators.CustomerEditValidators.StringBirthDateValidator;
import bg.energo.phoenix.model.enums.customer.Status;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ValidContactPersonRequest
public class BaseContactPersonRequest {

    @NotNull(message = "communicationData.contactPersons.titleId-contact person title: must not be null;")
    private Long titleId;

    @NotNull(message = "communicationData.contactPersons.name-contact person name: must not be null;")
    @NotBlank(message = "communicationData.contactPersons.name-contact person name: must not be blank;")
    @ContactPersonInput(value = "communicationData.contactPersons.name")
    @Size(min = 1, max = 512, message = "communicationData.contactPersons.name-contact person name: size should be between {min} and {max} symbols;")
    private String name;

    @ContactPersonInput(value = "communicationData.contactPersons.middleName-contact person middleName")
    @Size(min = 1, max = 512, message = "communicationData.contactPersons.middleName-contact person middleName: size should be between {min} and {max} symbols;")
    private String middleName;

    @NotNull(message = "communicationData.contactPersons.surname-contact person surname: must not be null;")
    @NotBlank(message = "communicationData.contactPersons.surname-contact person surname: must not be blank;")
    @ContactPersonInput(value = "communicationData.contactPersons.surname")
    @Size(min = 1, max = 512, message = "communicationData.contactPersons.surname-contact person surname: size should be between {min} and {max} symbols;")
    private String surname;

    @Pattern(regexp = "^[А-Яа-яA-Za-z\\d–&\\-'‘\\s]*$", message = "communicationData.contactPersons.jobPosition job position invalid pattern, valid pattern [{regexp}];")
    @Size(min = 1, max = 512, message = "communicationData.contactPersons.jobPosition-contact person jobPosition: size should be between {min} and {max} symbols;")
    private String jobPosition;

    private LocalDate positionHeldFrom;

    private LocalDate positionHeldTo;

    @StringBirthDateValidator(message = "birthDate-wrong birthdate format")
    private String birthDate;

    @AdditionalInformation(value = "communicationData.contactPersons.additionalInformation-contact person additionalInformation")
    @Size(min = 1, max = 2048, message = "communicationData.contactPersons.additionalInformation-contact person additionalInformation: size should be between {min} and {max} symbols;")
    private String additionalInformation;

    @NotNull(message = "communicationData.contactPersons.status-contact person status: must not be null;")
    private Status status;

}
