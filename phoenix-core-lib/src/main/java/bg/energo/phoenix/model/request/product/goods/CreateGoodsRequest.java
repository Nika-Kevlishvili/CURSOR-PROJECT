package bg.energo.phoenix.model.request.product.goods;

import bg.energo.phoenix.model.customAnotations.product.goods.GoodsPriceValidator;
import bg.energo.phoenix.model.customAnotations.product.goods.withValidators.GoodsCreateSalesAreaValidator;
import bg.energo.phoenix.model.customAnotations.product.goods.withValidators.GoodsCreateSalesChannelValidator;
import bg.energo.phoenix.model.customAnotations.product.goods.withValidators.GoodsCreateSegmentValidator;
import bg.energo.phoenix.model.customAnotations.product.goods.withValidators.GoodsCreateVatRateValidator;
import bg.energo.phoenix.model.enums.product.goods.GoodsDetailStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.validator.constraints.Length;

import java.math.BigDecimal;
import java.util.List;

/**
 * <h1>CreateGoodsRequest object</h1>
 * {@link #name}
 * {@link #nameTransl}
 * {@link #printingName}
 * {@link #printingNameTransl}
 * {@link #goodsGroupsId}
 * {@link #otherSystemConnectionCode}
 * {@link #goodsSuppliersId}
 * {@link #manufacturerCodeNumber}
 * {@link #goodsDetailStatus}
 * {@link #goodsUnitId}
 * {@link #incomeAccountNumbers}
 * {@link #controllingOrderId}
 * {@link #vatRateId}
 * {@link #salesChannelsIds}
 * {@link #salesAreasIds}
 * {@link #segmentsIds}
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@GoodsCreateVatRateValidator
@GoodsCreateSalesAreaValidator
@GoodsCreateSalesChannelValidator
@GoodsCreateSegmentValidator
public class CreateGoodsRequest {

    @NotBlank(message = "name-name is required;")
    @Length(min = 1, max = 1024, message = "name-name length must be between 1 and 1024;")
    private String name;

    @NotBlank(message = "nameTransl-transliterated name is required;")
    @Length(min = 1, max = 1024, message = "nameTransl-transliterated name length must be between 1 and 1024;")
    private String nameTransl;

    @NotBlank(message = "printingName-printingName is required;")
    @Length(min = 1, max = 1024, message = "printingName-printingName length must be between 1 and 1024;")
    private String printingName;

    @NotBlank(message = "printingNameTransl-Transliterated printing name is required;")
    @Length(min = 1, max = 1024, message = "printingNameTransl-Transliterated printing name length must be between 1 and 1024;")
    private String printingNameTransl;

    @NotNull(message = "goodsGroupsId-group ID is required;")
    private Long goodsGroupsId;

    @Length(min = 1, max = 256, message = "otherSystemConnectionCode-other system connection code length must be between 1 and 256;")
    private String otherSystemConnectionCode;

    @NotNull(message = "goodsSuppliersId-goods suppliers ID is required;")
    private Long goodsSuppliersId;

    @Length(min = 1, max = 512, message = "manufacturerCodeNumber-Manufacturer Code Number length must be between 1 and 512;")
    private String manufacturerCodeNumber;

    @NotNull(message = "goodsDetailStatus-Goods Status is required;")
    private GoodsDetailStatus goodsDetailStatus;

    @NotNull(message = "price-price is required;")
    @GoodsPriceValidator
    private BigDecimal price;

    @NotNull(message = "currencyId-currency is required;")
    private Long currencyId;

    @NotNull(message = "goodsUnitId-Goods units is required;")
    private Long goodsUnitId;

    @Length(min = 1, max = 32, message = "incomeAccountNumbers-Income account numbers length must be between 1 and 32;")
    private String incomeAccountNumbers;

    @Length(min = 1, max = 32, message = "controllingOrderId-controlling order ID length must be between 1 and 32;")
    private String controllingOrderId;

//    @NotNull(message = "vatRateId-VAT rate is required;")
    private Long vatRateId;

    @NotNull(message = "globalVatRate-global vat rate should not be null;")
    private Boolean globalVatRate;
    @NotNull(message = "globalSalesArea-global sales area should not be null;")
    private Boolean globalSalesArea;
    @NotNull(message = "globalSalesChannel-global sales channel should not be null;")
    private Boolean globalSalesChannel;
    @NotNull(message = "globalSegment-global segment should not be null;")
    private Boolean globalSegment;

    private List<Long> salesChannelsIds;

    private List<Long> salesAreasIds;

    private List<Long> segmentsIds;

}
