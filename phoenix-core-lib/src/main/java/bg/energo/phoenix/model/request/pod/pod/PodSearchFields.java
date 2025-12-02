package bg.energo.phoenix.model.request.pod.pod;

public enum PodSearchFields {
    ALL("ALL"),
    ADDITIONAL_IDENTIFIER("ADDITIONALIDENTIFIER"),
    IDENTIFIER("IDENTIFIER"),
    NAME("NAME"),
    CUSTOMER_IDENTIFIER("CUSTOMERIDENTIFIER"),


    ;

    private String column;
    PodSearchFields(String column) {
        this.column=column;
    }

    public String getColumn() {
        return column;
    }
}
