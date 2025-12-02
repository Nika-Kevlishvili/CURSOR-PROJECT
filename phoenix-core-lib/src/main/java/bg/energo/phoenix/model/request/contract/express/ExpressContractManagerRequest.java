package bg.energo.phoenix.model.request.contract.express;

import bg.energo.phoenix.model.customAnotations.customer.manager.ManagerInput;
import bg.energo.phoenix.model.response.customer.manager.ManagerResponse;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.Objects;

@Data
public class ExpressContractManagerRequest {
    private Long id;
    @NotNull(message = "customer.managerRequests.titleId-manager title must not be null;")
    private Long titleId;
    @NotNull(message = "customer.managerRequests.name-manager name must not be null;")
    @NotBlank(message = "customer.managerRequests.name-manager name must not be blank;")
    @ManagerInput(value = "customer.managerRequests.name")
    @Size(min = 1, max = 512, message = "customer.managerRequests.name-manager name size should be between {min} and {max} symbols;")
    private String name;
    @ManagerInput(value = "customer.managerRequests.middleName")
    @Size(min = 1, max = 512, message = "customer.managerRequests.middleName-manager middleName size should be between {min} and {max} symbols;")
    private String middleName;
    @NotNull(message = "customer.managerRequests.surname-manager surname must not be null;")
    @NotBlank(message = "customer.managerRequests.surname-manager surname must not be blank;")
    @ManagerInput(value = "managers.surname")
    @Size(min = 1, max = 512, message = "customer.managerRequests.surname-manager surname size should be between {min} and {max} symbols;")
    private String surname;
    @NotNull(message = "customer.managerRequests.jobPosition-manager jobPosition must not be null;")
    @NotBlank(message = "customer.managerRequests.jobPosition-manager jobPosition must not be blank;")
    @Pattern(regexp = "^[А-Яа-яA-Za-z\\d–&\\-'‘\\s]*$", message = "customer.managerRequests.jobPosition-manager job position invalid pattern, valid pattern [{regexp}];")
    @Size(min = 1, max = 512, message = "customer.managerRequests.jobPosition-manager jobPosition size should be between {min} and {max} symbols;")
    private String jobPosition;
    @NotNull(message = "customer.managerRequests.representationMethodId-Representation method can not be null!;")
    private Long representationMethodId;

    private String personalNumber;

    public boolean equalsResponse(ManagerResponse response) {
        if (!Objects.equals(id, response.getId())) return false;
        if (!Objects.equals(titleId, response.getTitleId())) return false;
        if (!Objects.equals(name, response.getName())) return false;
        if (!Objects.equals(middleName, response.getMiddleName())) return false;
        if (!Objects.equals(surname, response.getSurname())) return false;
        if(!Objects.equals(representationMethodId,response.getRepresentationMethodId())) return false;
        if(!Objects.equals(personalNumber,response.getPersonalNumber())) return false;
        return Objects.equals(jobPosition, response.getJobPosition());
    }
}
