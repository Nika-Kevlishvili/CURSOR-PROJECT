package bg.energo.phoenix.model.enums.communication.xEnergie;

import lombok.Getter;

public enum XEnergieRepositoryCreateCustomerResponse {
    CUSTOMER_CREATED(1f),
    CUSTOMER_EXISTS(0f),
    ERROR_IN_DATABASE(-1f),
    TOO_MANY_ROWS(-2f),
    NO_DATA_FOUND(-3f),
    INVALID_CURSOR(-4f),
    MISSING_CUSTOMER_NUMBER(-5f),
    MISSING_GROUP_FLAG(-6f),
    MISSING_CUSTOMER_IDENTIFIER(-7f),
    UNEXPECTED_EXCEPTION_HANDLED(-999f);

    XEnergieRepositoryCreateCustomerResponse(float result) {
        this.result = result;
    }

    public static XEnergieRepositoryCreateCustomerResponse fromResult(float result) {
        for (XEnergieRepositoryCreateCustomerResponse response : values()) {
            if (response.getResult() == result) {
                return response;
            }
        }
        return UNEXPECTED_EXCEPTION_HANDLED;
    }

    @Getter
    private final float result;
}
