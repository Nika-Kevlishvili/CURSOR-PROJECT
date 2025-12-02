package phoenix.core.customer.model.request;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.Length;
import phoenix.core.customer.model.customAnotations.UICDefaultValidator;
import phoenix.core.customer.model.customAnotations.nomenclature.NameDefaultValidator;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CustomerOwnerRequest {


    @UICDefaultValidator
    @NotNull(message = "Customer Owner Personal Number is required; ")
    private String personalNumber;

    @NotBlank(message = "Customer Owner Additional Information is required; ")
    @Length(min = 1,max = 2048)
    @NameDefaultValidator
    private String additionalInformation;

    @Positive
    @NotNull(message = "Customer Owner Belonging Owner Capital ID is required; ")
    private Long belongingOwnerCapitalId;

    public CustomerOwnerRequest(CustomerOwnerEditRequest request) {
        this.personalNumber=request.getPersonalNumber();
        this.additionalInformation=request.getAdditionalInformation();
        this.belongingOwnerCapitalId= request.getBelongingOwnerCapitalId();
    }
}
