package bg.energo.phoenix.model.response.priceComponent.applicationModel;

import bg.energo.phoenix.model.entity.product.price.priceComponent.applicationModel.OverTimeOneTime;
import bg.energo.phoenix.model.enums.product.price.priceComponent.applicationModel.OverTimeOneTimeType;
import lombok.Data;

@Data
public class OverTimeOneTimeResponse {

    private OverTimeOneTimeType type;

    public OverTimeOneTimeResponse(OverTimeOneTime oneTime) {
        this.type = oneTime.getType();
    }
}
