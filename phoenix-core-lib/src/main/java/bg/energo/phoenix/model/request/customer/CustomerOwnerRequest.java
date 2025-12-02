package bg.energo.phoenix.model.request.customer;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.Length;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CustomerOwnerRequest {

    @NotBlank(message = "personalNumber-Customer Owner Personal Number is required;")
    private String personalNumber;

    @Length(min = 1,max = 2048,message = "additionalInformation-CustomerOwner additional information length should be 1 and 2048 symbols;")
    @Pattern(regexp = "^[\\dА-Яа-яA-Za-z–@#$&*()_+\\-§?!/\\\\<>:.,'‘€№=\\s]*$",message = "additionalInformation-CustomerOwner Invalid symbols in additionalInformation;")
    private String additionalInformation;

    @Positive(message = "belongingOwnerCapitalId-CustomerOwner belongingOwnerCapitalId should be positive;")
    @NotNull(message = "belongingOwnerCapitalId-CustomerOwner Belonging Owner Capital ID is required;")
    private Long belongingOwnerCapitalId;

    public CustomerOwnerRequest(CustomerOwnerEditRequest request) {
        this.personalNumber=request.getPersonalNumber();
        this.additionalInformation=request.getAdditionalInformation();
        this.belongingOwnerCapitalId= request.getBelongingOwnerCapitalId();
    }

}
