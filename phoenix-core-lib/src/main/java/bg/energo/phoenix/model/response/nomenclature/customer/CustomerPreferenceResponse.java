package bg.energo.phoenix.model.response.nomenclature.customer;

import bg.energo.phoenix.model.enums.customer.Status;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CustomerPreferenceResponse {
    private Long id;
   // private CustomerDetails customerDetail;
    private PreferencesResponse preferences;
    private Status status;
}
