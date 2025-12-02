package bg.energo.phoenix.model.response.pod.discount;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class DiscountPointOfDeliveryShortResponse {
    private Long id;
    private String name;
    private String identifier;
}
