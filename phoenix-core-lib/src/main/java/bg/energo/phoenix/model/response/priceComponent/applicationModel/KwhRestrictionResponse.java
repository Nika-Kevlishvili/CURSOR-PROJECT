package bg.energo.phoenix.model.response.priceComponent.applicationModel;

import bg.energo.phoenix.model.entity.product.price.priceComponent.applicationModel.VolumesByScaleKwhRestriction;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class KwhRestrictionResponse {
    private Long id;
    private Integer valueFrom;
    private Integer valueTo;

    public KwhRestrictionResponse(VolumesByScaleKwhRestriction restriction) {
        this.id = restriction.getId();
        this.valueFrom = restriction.getValueFrom();
        this.valueTo = restriction.getValueTo();
    }
}
