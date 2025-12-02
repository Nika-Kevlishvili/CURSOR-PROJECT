package bg.energo.phoenix.model.enums.product.goods;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum GoodsSearchField {
    ALL("ALL"),
    NAME("NAME"),
    GROUP_NAME("GROUP_NAME"),
    GOODS_SUPPLIER("GOODS_SUPPLIER"),
    CODE_NUMBER_OF_MANUFACTURER("MANUFACTURER_CODE"),
    CODE_FOR_CONNECTION_WITH_OTHER_SYSTEM("OTHER_SYS_CONNECTION_CODE");

    private final String value;
}
