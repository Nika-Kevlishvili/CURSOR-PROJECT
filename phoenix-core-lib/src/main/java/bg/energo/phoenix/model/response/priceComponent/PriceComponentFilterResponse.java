package bg.energo.phoenix.model.response.priceComponent;

import bg.energo.phoenix.model.enums.product.price.priceComponent.NumberType;
import bg.energo.phoenix.model.enums.product.price.priceComponent.PriceComponentConditions;
import bg.energo.phoenix.model.enums.product.price.priceComponent.PriceComponentStatus;
import lombok.Data;
import org.apache.commons.lang3.math.NumberUtils;

import java.time.LocalDateTime;

@Data
public class PriceComponentFilterResponse {
    private Long id;
    private String name;
    private String priceTypeName;
    private String valueTypeName;
    private NumberType numberType;
    private PriceComponentConditions condition;
    private String availability;
    private String formula;
    private LocalDateTime createDate;
    private PriceComponentStatus status;

    public PriceComponentFilterResponse(PriceComponentMiddleResponse middleResponse) {
        this.id = middleResponse.getId();
        this.name = middleResponse.getName();
        this.priceTypeName = middleResponse.getPrice();
        this.valueTypeName = middleResponse.getValue();
        this.numberType = NumberType.valueOf(middleResponse.getNumber());
        this.condition = PriceComponentConditions.valueOf(middleResponse.getConditions());
        this.availability = middleResponse.getAvailable();
        this.createDate = middleResponse.getCdate();
        if (NumberUtils.isParsable(middleResponse.getFormula())) {
            this.formula = middleResponse.getFormula();
        } else {
            this.formula = "Formula";
        }
        this.status = middleResponse.getStatus();
    }
}
