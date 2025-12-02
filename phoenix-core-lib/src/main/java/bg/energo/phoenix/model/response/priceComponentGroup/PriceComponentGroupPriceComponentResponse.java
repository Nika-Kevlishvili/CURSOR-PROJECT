package bg.energo.phoenix.model.response.priceComponentGroup;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PriceComponentGroupPriceComponentResponse {

    private Long id;

    private Long priceComponentId;

    private String priceComponentName;

}
