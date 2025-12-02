package bg.energo.phoenix.model.enums.product.product.list;

import lombok.Getter;

public enum ProductListColumns {
    ID("id"),
    NAME("pd.name"),
    GROUP("pg.name"),
    STATUS("productStatus"),
    PRODUCT_TYPE("pt.name"),
    CONTRACT_TERM("pct.name"),
    SALES_CHANNEL("psc.name"),
    CREATE_DATE("createDate"),
    INDIVIDUAL_PRODUCT("customerIdentifier"),
    CONTRACT_TEMPLATE("contractTemplate");//TODO not done yet

    @Getter
    private final String value;

    ProductListColumns(String value) {
        this.value = value;
    }
}
