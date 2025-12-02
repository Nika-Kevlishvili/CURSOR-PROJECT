package bg.energo.phoenix.apis.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * <h1>CustomerCheckResponse</h1>
 * {@link #customers} List of Apis Api Customer object
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class CustomerCheckResponse {
    List<ApisCustomer> customers;
}
