package bg.energo.phoenix.model.request.customer.manager;

import bg.energo.phoenix.model.customAnotations.customer.manager.AdditionalInformation;
import bg.energo.phoenix.model.customAnotations.customer.manager.ManagerInput;
import bg.energo.phoenix.model.customAnotations.customer.manager.PersonalNumber;
import bg.energo.phoenix.model.customAnotations.customer.manager.ValidManagerRequest;
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
@AllArgsConstructor
@NoArgsConstructor
@ValidManagerRequest
public class BaseManagerRequest {

    @NotNull(message = "managers.titleId-manager title must not be null;")
    private Long titleId;

    @NotNull(message = "managers.name-manager name must not be null;")
    @NotBlank(message = "managers.name-manager name must not be blank;")
    @ManagerInput(value = "managers.name")
    @Size(min = 1, max = 512, message = "name-manager name size should be between {min} and {max} symbols;")
    private String name;

    @ManagerInput(value = "managers.middleName")
    @Size(min = 1, max = 512, message = "managers.middleName-manager middleName size should be between {min} and {max} symbols;")
    private String middleName;

    @NotNull(message = "managers.surname-manager surname must not be null;")
    @NotBlank(message = "managers.surname-manager surname must not be blank;")
    @ManagerInput(value = "managers.surname")
    @Size(min = 1, max = 512, message = "managers.surname-manager surname size should be between {min} and {max} symbols;")
    private String surname;

    @PersonalNumber(message = "managers.personalNumber-manager personalNumber length must be 10 or 12 digits and match valid pattern;")
    private String personalNumber;

    @NotNull(message = "managers.jobPosition-manager jobPosition must not be null;")
    @NotBlank(message = "managers.jobPosition-manager jobPosition must not be blank;")
    @Pattern(regexp = "^[А-Яа-яA-Za-z\\d–&\\-'‘\\s]*$", message = "managers.jobPosition-manager job position invalid pattern, valid pattern [{regexp}];")
    @Size(min = 1, max = 512, message = "managers.jobPosition-manager jobPosition size should be between {min} and {max} symbols;")
    private String jobPosition;

    private LocalDate positionHeldFrom;

    private LocalDate positionHeldTo;
    @StringBirthDateValidator(message = "birthDate-wrong birthdate format")
    private String birthDate;

    @NotNull(message = "managers.representationMethodId-manager representationMethod must not be null;")
    private Long representationMethodId;

    @AdditionalInformation(value = "managers.additionalInformation")
    @Size(min = 1, max = 2048, message = "managers.additionalInformation-manager additionalInformation size should be between {min} and {max} symbols;")
    private String additionalInformation;

    @NotNull(message = "managers.status-manager status must not be null;")
    private Status status;

}
