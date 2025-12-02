package bg.energo.phoenix.model.response.pod.discount;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class DiscountCustomerShortResponse {
    private Long id;
    private String identifier;
    private String displayName;
}
