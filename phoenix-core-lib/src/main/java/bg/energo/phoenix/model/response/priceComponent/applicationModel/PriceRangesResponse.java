package bg.energo.phoenix.model.response.priceComponent.applicationModel;

import bg.energo.phoenix.model.entity.product.price.priceComponent.applicationModel.PerPieceRanges;
import lombok.Data;

@Data
public class PriceRangesResponse {
    private Integer from;
    private Integer to;

    public PriceRangesResponse(PerPieceRanges range) {
        this.from = range.getValueFrom();
        this.to = range.getValueTo();
    }
}
