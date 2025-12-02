package phoenix.core.customer.model.response.customer;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import phoenix.core.customer.model.enums.customer.Status;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CustomerPreferenceResponse {
    private Long id;
   // private CustomerDetails customerDetail;
    private PreferencesResponse preferences;
    private Status status;
}
