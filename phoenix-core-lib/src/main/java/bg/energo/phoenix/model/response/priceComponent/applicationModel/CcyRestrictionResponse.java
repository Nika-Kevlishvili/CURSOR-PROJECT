package bg.energo.phoenix.model.response.priceComponent.applicationModel;

import bg.energo.phoenix.model.entity.product.price.priceComponent.applicationModel.VolumesByScaleCcyRestriction;
import bg.energo.phoenix.model.response.nomenclature.product.currency.CurrencyResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CcyRestrictionResponse {
    private Long id;
    private Integer valueFrom;
    private Integer valueTo;
    private CurrencyResponse currencyResponse;

    public CcyRestrictionResponse(VolumesByScaleCcyRestriction restriction) {
        this.id = restriction.getId();
        this.valueFrom = restriction.getValueFrom();
        this.valueTo = restriction.getValueTo();
    }
}
