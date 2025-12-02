package bg.energo.phoenix.model.enums.receivable.balancingGroupCoordinatorObjection;

public enum ObjectionSortingFields {

    NUMBER("number"),
    CHANGE_DATE("changeDate"),
    GRID_OPERATOR("gridOperator"),
    CHANGE_STATUS("changeStatus"),
    COUNT_PODS("numberOfPods"),
    CREATION_DATE("createDate");

    private final String value;

    private ObjectionSortingFields(String value) {
        this.value = value;
    }
    public String getValue() {
        return value;
    }
}
