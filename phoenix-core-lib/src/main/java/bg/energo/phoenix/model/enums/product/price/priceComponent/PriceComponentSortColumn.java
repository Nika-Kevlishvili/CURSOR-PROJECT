package bg.energo.phoenix.model.enums.product.price.priceComponent;

public enum PriceComponentSortColumn {

    ID("id"),
    NAME("name"),
    VALUE_TYPE("value"),
    PRICE_TYPE("price"),
    NUMBER_TYPE("number"),
    CONDITIONS("conditions"),
    AVAILABILITY("available"),
    FORMULA("formula"),
    CREATE_DATE("cdate"),

    ;

    private String column;
    PriceComponentSortColumn(String column) {
        this.column=column;
    }

    public String getColumn() {
        return column;
    }
}
