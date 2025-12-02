package bg.energo.phoenix.apis.model;

import bg.energo.phoenix.apis.validation.ApisIdentificationNumberValidator;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * <h1>CustomerCheckRequest object</h1>
 * {@link #identificationNumbers} list of customer personal numbers
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class CustomerCheckRequest {
    @NotNull(message = "identificationNumbers-shouldn't be null")
    @ApisIdentificationNumberValidator(minListSize = 1, maxListSize = 5000,
            actualFirstLength = 9, actualSecondLength = 13)
    public List<String> identificationNumbers;
}
