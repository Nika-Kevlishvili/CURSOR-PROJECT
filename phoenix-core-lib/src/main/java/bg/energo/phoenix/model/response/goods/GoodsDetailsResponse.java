package bg.energo.phoenix.model.response.goods;

import bg.energo.phoenix.model.entity.product.goods.GoodsDetails;
import bg.energo.phoenix.model.enums.product.goods.GoodsDetailStatus;
import bg.energo.phoenix.model.response.nomenclature.goods.GoodsGroupsResponse;
import bg.energo.phoenix.model.response.nomenclature.goods.GoodsSuppliersResponse;
import bg.energo.phoenix.model.response.nomenclature.goods.GoodsUnitsResponse;
import bg.energo.phoenix.model.response.nomenclature.product.VatRateResponse;
import bg.energo.phoenix.model.response.nomenclature.product.currency.CurrencyResponse;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * <h1>GoodsDetailsResponse object</h1>
 * {@link #id}
 * {@link #name}
 * {@link #nameTransl}
 * {@link #printingName}
 * {@link #printingNameTransl}
 * {@link #otherSystemConnectionCode}
 * {@link #goodsSuppliers}
 * {@link #manufacturerCodeNumber}
 * {@link #status}
 * {@link #price}
 * {@link #currency}
 * {@link #goodsUnits}
 * {@link #vatRate}
 * {@link #incomeAccountNumbers}
 * {@link #controllingOrderId}
 * {@link #versionId}
 * {@link #systemUserId}
 * {@link #createDate}
 * {@link #modifyDate}
 * {@link #modifySystemUserId}
 * {@link #versions}
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class GoodsDetailsResponse {

    private Long id;
    private String name;
    private String nameTransl;
    private String printingName;
    private String printingNameTransl;
    private GoodsGroupsResponse goodsGroups;
    private String otherSystemConnectionCode;
    private GoodsSuppliersResponse goodsSuppliers;
    private String manufacturerCodeNumber;
    private GoodsDetailStatus status;
    private BigDecimal price;
    private CurrencyResponse currency;
    private GoodsUnitsResponse goodsUnits;
    private VatRateResponse vatRate;
    private String incomeAccountNumbers;
    private String controllingOrderId;
    private Long versionId;
    private String systemUserId;
    private LocalDateTime createDate;
    private LocalDateTime modifyDate;
    private String modifySystemUserId;
    private List<GoodsVersionsResponse> versions;
    private Boolean globalVatRate;
    private Boolean globalSalesArea;
    private Boolean globalSalesChannel;
    private Boolean globalSegment;
    public GoodsDetailsResponse(GoodsDetails goodsDetails, List<GoodsVersionsResponse> versions) {
        this.id = goodsDetails.getId();
        this.name = goodsDetails.getName();
        this.nameTransl = goodsDetails.getNameTransl();
        this.printingName = goodsDetails.getPrintingName();
        this.printingNameTransl = goodsDetails.getPrintingNameTransl();
        this.goodsGroups = new GoodsGroupsResponse(goodsDetails.getGoodsGroups());
        this.otherSystemConnectionCode = goodsDetails.getOtherSystemConnectionCode();
        this.goodsSuppliers = new GoodsSuppliersResponse(goodsDetails.getGoodsSuppliers());
        this.manufacturerCodeNumber = goodsDetails.getManufacturerCodeNumber();
        this.status = goodsDetails.getStatus();
        this.price = goodsDetails.getPrice();
        this.currency = new CurrencyResponse(goodsDetails.getCurrency());
        this.goodsUnits = new GoodsUnitsResponse(goodsDetails.getGoodsUnits());
        this.vatRate = goodsDetails.getVatRate() == null ? null : new VatRateResponse(goodsDetails.getVatRate());
        this.incomeAccountNumbers = goodsDetails.getIncomeAccountNumbers();
        this.controllingOrderId = goodsDetails.getControllingOrderId();
        this.versionId = goodsDetails.getVersionId();
        this.systemUserId = goodsDetails.getSystemUserId();
        this.createDate = goodsDetails.getCreateDate();
        this.modifyDate = goodsDetails.getModifyDate();
        this.modifySystemUserId = goodsDetails.getModifySystemUserId();
        this.versions = versions;
        this.globalVatRate = goodsDetails.getGlobalVatRate();
        this.globalSalesArea = goodsDetails.getGlobalSalesArea();
        this.globalSalesChannel = goodsDetails.getGlobalSalesChannel();
        this.globalSegment = goodsDetails.getGlobalSegment();
    }
}
