package bg.energo.phoenix.model.request.contract.product;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

@EqualsAndHashCode(callSuper = true)
@Data
public class ProductContractProductParametersCreateRequest extends ProductParameterBaseRequest{



    // contract third tab information fields
    @DecimalMin(value = "0.01",message = "productParameters.marginalPrice-marginalPrice can not be less than 0.01;")
    @DecimalMax(value = "99999999999.99",message = "productParameters.marginalPrice-marginalPrice can not be greater than 99999999999.99;")
    private BigDecimal marginalPrice;
    @DecimalMin(value = "0.01",message = "productParameters.marginalPriceValidity-marginalPriceValidity can not be less than 0.01;")
    @DecimalMax(value = "99999999999.99",message = "productParameters.marginalPriceValidity-marginalPriceValidity can not be greater than 99999999999.99;")
    private String marginalPriceValidity;
    @DecimalMin(value = "0",message = "productParameters.hourlyLoadProfile-hourlyLoadProfile can not be less than 0;")
    @DecimalMax(value = "99999999999.99999",message = "productParameters.hourlyLoadProfile-hourlyLoadProfile can not be greater than 99999999999.99999;")
    @Digits(integer = 14, fraction = 5, message = "productParameters.hourlyLoadProfile-hourlyLoadProfile can have a maximum of 5 digits after the decimal point;")
    private BigDecimal hourlyLoadProfile;
    @DecimalMin(value = "0.01",message = "productParameters.procurementPrice-procurementPrice can not be less than 0.01;")
    @DecimalMax(value = "99999999999.99",message = "productParameters.procurementPrice-procurementPrice can not be greater than 99999999999.99;")
    private BigDecimal procurementPrice;
    @DecimalMin(value = "0.01",message = "productParameters.imbalancePriceIncrease-imbalancePriceIncrease can not be less than 0.01;")
    @DecimalMax(value = "99999999999.99",message = "productParameters.imbalancePriceIncrease-imbalancePriceIncrease can not be greater than 99999999999.99;")
    private BigDecimal imbalancePriceIncrease;
    @DecimalMin(value = "0.01",message = "productParameters.setMargin-setMargin can not be less than 0.01;")
    @DecimalMax(value = "99999999999.99",message = "productParameters.setMargin-setMargin can not be greater than 99999999999.99;")
    private BigDecimal setMargin;


}
