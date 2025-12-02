package bg.energo.phoenix.model.request.pod.pod;

public enum PodSortColumns {
    ID("id"),
    DETAIL_ID("pd2.id"),
//    DISCONNECTION("disconnected"),
    MEASUREMENT_TYPE("pd2.measurementType"),
    CONSUMPTION_TYPE("pd2.consumptionPurpose"),
    PROVIDED_POWER("pd2.providedPower"),
    GRID_OPERATOR("go2.name"),
    CUSTOMER_IDENTIFIER("c.identifier"),
    POD_TYPE("pd2.type"),
    IDENTIFIER("identifier"),
    DISCONNECTION("disconnectionPowerSupply"),
    ;

    private String column;
    PodSortColumns(String column) {
        this.column=column;
    }

    public String getColumn() {
        return column;
    }
}
