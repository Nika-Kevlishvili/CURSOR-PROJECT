package phoenix.core.customer.apis.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import phoenix.core.customer.apis.validation.ApisIdentificationNumberValidator;

import javax.validation.constraints.NotNull;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CustomerCheckRequest {
    @NotNull
    @ApisIdentificationNumberValidator(minListSize = 1, maxListSize = 5000,
            actualFirstLength = 9, actualSecondLength = 13)
    public List<String> identificationNumbers;
}
