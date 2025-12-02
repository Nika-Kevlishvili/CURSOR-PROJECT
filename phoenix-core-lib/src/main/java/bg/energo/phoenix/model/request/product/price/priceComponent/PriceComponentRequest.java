package bg.energo.phoenix.model.request.product.price.priceComponent;

import bg.energo.phoenix.model.customAnotations.product.priceComponent.PriceComponentVatRateValidator;
import bg.energo.phoenix.model.customAnotations.product.priceComponent.XenergieApplicationValidator;
import bg.energo.phoenix.model.enums.product.price.priceComponent.NumberType;
import bg.energo.phoenix.model.request.product.price.aplicationModel.ApplicationModelRequest;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.Objects;


@Data
@XenergieApplicationValidator
@PriceComponentVatRateValidator
public class PriceComponentRequest {

    @NotNull(message = "name-name can not be null;")
    @Size(min = 1, max = 1024, message = "name-name should be between 1 and 1024;")
    private String name;

    @NotNull(message = "displayName-displayName can not be null;")
    @Size(min = 1, max = 1024, message = "displayName-displayName should be between 1 and 1024;")
    private String displayName;

    @NotNull(message = "priceComponentValueTypeId-priceComponentValueTypeId can not be null;")
    @Positive(message = "priceComponentValueTypeId-priceComponentValueTypeId should be positive;")
    private Long priceComponentValueTypeId;

    @NotNull(message = "priceComponentPriceTypeId-priceComponentPriceTypeId can not be null;")
    @Positive(message = "priceComponentPriceTypeId-priceComponentPriceTypeId should be positive;")
    private Long priceComponentPriceTypeId;

    @NotNull(message = "currencyId-currencyId can not be null;")
    @Positive(message = "currencyId-currencyId should be positive;")
    private Long currencyId;

    @NotNull(message = "numberType-numberType can not be null;")
    private NumberType numberType;

    private String numberOfIncomeAccount;

    private String controllingOrder;

    private String tagForContractTemplate;

    @Valid
    @NotNull(message = "applicationModelRequest-application model can not be null;")
    private ApplicationModelRequest applicationModelRequest;

    @Positive(message = "vatRateId-vatRateId should be positive;")
    private Long vatRateId;

    private Boolean globalVatRate;

    private Boolean discount;

    private Boolean doNotIncludeVatBase;
    @JsonProperty("customerDetailId")
    private Long alternativeRecipientCustomerDetailId;

    private Boolean consumer;
    private Boolean generator;

    @Valid
    @NotNull(message = "formulaRequest- Formula data must not be null;")
    private PriceComponentFormulaRequest formulaRequest;

    @JsonIgnore
    @AssertTrue(message = "alternativeRecipientCustomerDetailId-Alternative Recipient customer detail id must not be null when doNotIncludeVatBase is checked")
    public boolean isAlternativeRecipientCustomerDetailIdValid() {
        if (Boolean.TRUE.equals(doNotIncludeVatBase)) {
            return !Objects.isNull(alternativeRecipientCustomerDetailId);
        }
        return true;
    }
}
