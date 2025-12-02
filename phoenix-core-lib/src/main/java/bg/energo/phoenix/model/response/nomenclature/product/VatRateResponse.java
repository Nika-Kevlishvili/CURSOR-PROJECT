package bg.energo.phoenix.model.response.nomenclature.product;

import bg.energo.phoenix.model.entity.nomenclature.product.VatRate;
import bg.energo.phoenix.model.enums.nomenclature.NomenclatureItemStatus;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class VatRateResponse {
    private Long id;

    private String name;

    private BigDecimal valueInPercent;

    private Boolean globalVatRate;

    private LocalDate startDate;

    private Long orderingId;

    private NomenclatureItemStatus status;

    private String systemUserId;

    public VatRateResponse(VatRate vatRate){
        this.id = vatRate.getId();
        this.name = vatRate.getName();
        this.valueInPercent = vatRate.getValueInPercent();
        this.globalVatRate = vatRate.getGlobalVatRate();
        this.startDate = vatRate.getStartDate();
        this.status = vatRate.getStatus();
        this.orderingId = vatRate.getOrderingId();
        this.systemUserId = vatRate.getSystemUserId();
    }
}
