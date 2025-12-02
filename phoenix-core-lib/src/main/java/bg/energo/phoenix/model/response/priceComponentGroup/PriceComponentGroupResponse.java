package bg.energo.phoenix.model.response.priceComponentGroup;

import bg.energo.phoenix.model.enums.product.price.priceComponentGroup.PriceComponentGroupStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PriceComponentGroupResponse {

    private Long groupId;

    private PriceComponentGroupStatus status;

    private Long groupDetailId;

    private Long versionId;

    private String name;

    private List<PriceComponentGroupVersion> versions;

    private List<PriceComponentGroupPriceComponentResponse> priceComponentsList;

    private Boolean isLocked;

}
