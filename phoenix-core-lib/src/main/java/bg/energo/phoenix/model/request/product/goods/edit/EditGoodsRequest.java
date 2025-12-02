package bg.energo.phoenix.model.request.product.goods.edit;

import bg.energo.phoenix.model.customAnotations.product.goods.GoodsPriceValidator;
import bg.energo.phoenix.model.customAnotations.product.goods.withValidators.GoodsEditSalesAreaValidator;
import bg.energo.phoenix.model.customAnotations.product.goods.withValidators.GoodsEditSalesChannelValidator;
import bg.energo.phoenix.model.customAnotations.product.goods.withValidators.GoodsEditSegmentValidator;
import bg.energo.phoenix.model.customAnotations.product.goods.withValidators.GoodsEditVatRateValidator;
import bg.energo.phoenix.model.enums.product.goods.GoodsDetailStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.Length;

import java.math.BigDecimal;
import java.util.List;

/**
 * <h1>EditGoodsRequest object</h1>
 * {@link #updateExistingVersion}
 * {@link #versionId}
 * {@link #name}
 * {@link #nameTransl}
 * {@link #printingName}
 * {@link #printingNameTransl}
 * {@link #goodsGroupsId}
 * {@link #otherSystemConnectionCode}
 * {@link #goodsSuppliersId}
 * {@link #manufacturerCodeNumber}
 * {@link #goodsDetailStatus}
 * {@link #price}
 * {@link #currencyId}
 * {@link #goodsUnitId}
 * {@link #vatRateId}
 * {@link #incomeAccountNumbers}
 * {@link #controllingOrderId}
 * {@link #salesAreasIds}
 * {@link #salesChannelsIds}
 * {@link #segmentsIds}
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@GoodsEditVatRateValidator
@GoodsEditSegmentValidator
@GoodsEditSalesAreaValidator
@GoodsEditSalesChannelValidator
public class EditGoodsRequest {

    @NotNull(message = "updateExistingVersion-updateExistingVersion shouldn't be null;")
    private Boolean updateExistingVersion;

    @NotNull(message = "versionId-versionId shouldn't be null;")
    private Long versionId;

//    @Valid
//    private GoodsDetailsEditRequest goodsDetails;
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
    @Length(min = 1, max = 512, message = "manufacturerCodeNumber-manufacturer code number length must be between 1 and 512;")
    private String manufacturerCodeNumber;
    @NotNull(message = "goodsDetailStatus-Goods Status is required;")
    private GoodsDetailStatus goodsDetailStatus;
    @NotNull(message = "price-price is required;")
    @GoodsPriceValidator
    private BigDecimal price;
    @NotNull(message = "currencyId-currency id is required;")
    private Long currencyId;
    @NotNull(message = "goodsUnitId-Goods units id is required;")
    private Long goodsUnitId;

//    @NotNull(message = "vatRateId-VAT rate id is required;")
    private Long vatRateId;
    @NotNull(message = "globalVatRate-global vat rate should not be null;")
    private Boolean globalVatRate;

    @NotNull(message = "globalSalesArea-global sales area should not be null;")
    private Boolean globalSalesArea;
    @NotNull(message = "globalSalesChannel-global sales channel should not be null;")
    private Boolean globalSalesChannel;
    @NotNull(message = "globalSegment-global segment should not be null;")
    private Boolean globalSegment;

    @Length(min = 1, max = 32, message = "incomeAccountNumbers-Income account numbers length must be between 1 and 32;")
    private String incomeAccountNumbers;
    @Length(min = 1, max = 32, message = "controllingOrderId-controlling order ID length must be between 1 and 32;")
    private String controllingOrderId;

//    @NotNull(message = "salesChannelsIds-Sales channels is required;")
//    @Size(min = 1, message = "salesChannelsIds-Minimum one Sales channel is required;")
    private List<@Valid GoodsSalesAreaEditRequest> salesAreasIds;

//    @NotNull(message = "salesAreasIds-Sales areas is required;")
//    @Size(min = 1, message = "salesAreasIds-Minimum one Sales area is required;")
    private List<@Valid GoodsSalesChannelsEditRequest> salesChannelsIds;

//    @NotNull(message = "segmentsIds-Segments is required;")
//    @Size(min = 1, message = "segmentsIds-Minimum one segment is required;")
    private List<@Valid GoodsSegmentsEditRequest> segmentsIds;

}
