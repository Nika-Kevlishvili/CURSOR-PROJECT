package bg.energo.phoenix.model.request.customer;

import bg.energo.phoenix.model.customAnotations.DuplicatedValuesValidator;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.Length;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ConnectedGroupRequest {

    @Pattern(regexp= "^[А-ЯA-Z0-9@#$&*()+\\-:.,'‘– €№=]*$",message = "groupName-Group name does not match allowed symbols;")
    @NotBlank(message = "groupName-Group name can not be blank;")
    @Length(min = 1,max = 2048,message = "groupName-Group name length should be in range [{min}:{max}] and not be blank;")
    private String groupName;

    @NotNull(message = "connectionTypeId-Connection type ID should not be null;")
    private Long connectionTypeId;

    @Pattern(regexp= "^[А-ЯA-Zа-яa-z0-9@#$&*()–_+\\-§?!\\/\\\\<>:.,'‘€№= \\r\\n\\s\\d]*$", message = "additionalInformation-Additional information does not match allowed symbols;")
    @Length(max = 2048, message =  "additionalInformation-Additional information length should not exceed {max} length;")
    private String additionalInformation;

    @DuplicatedValuesValidator(fieldPath = "customerIds")
    private List<Long> customerIds;

}