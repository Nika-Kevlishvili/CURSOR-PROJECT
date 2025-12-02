package bg.energo.phoenix.model.response.priceComponent;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PriceComponentTagResponse {
    private String id;
    private String name;
}
