package bg.energo.phoenix.service.billing.runs.models.restriction;

import bg.energo.phoenix.service.billing.runs.models.BillingDataRestrictionModel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Data
@NoArgsConstructor
@AllArgsConstructor
//object for calculations
public class RestrictionModelDto {
    //values from billing restriction model
    private Long modelId;
    private Integer invoiceId;
    private Long podId;
    private BigDecimal totalVolumes;
    private BigDecimal price;
    private LocalDate calculatedFrom;
    private LocalDate calculatedTo;
    private Long priceComponentId;
    private Long currencyId;
    // restricted values
    private BigDecimal volumesOfPercentageRestriction;
    private BigDecimal amountOfPercentageRestriction;
    private BigDecimal volumesOfKwhRestriction;
    private BigDecimal amountOfKwhRestriction;
    private BigDecimal volumesOfCcyRestriction;
    private BigDecimal amountOfCcyRestriction;
    // calculated total amount for model
    private BigDecimal totalAmount;
    // calculated total percent restriction for whole priceComponent
    private BigDecimal totalPercentRestrictionByPriceComponent;
    //final prioritized restriction values
    private BigDecimal finalRestrictionVolume;
    private BigDecimal finalRestrictionAmount;

    private static RestrictionModelDto fromBillingDataRestrictionModel(BillingDataRestrictionModel billingDataRestrictionModel) {
        RestrictionModelDto restrictionModelDto = new RestrictionModelDto();
        restrictionModelDto.setModelId(billingDataRestrictionModel.getModelId());
        restrictionModelDto.setInvoiceId(billingDataRestrictionModel.getInvoiceId());
        restrictionModelDto.setPodId(billingDataRestrictionModel.getPodId());
        restrictionModelDto.setTotalVolumes(billingDataRestrictionModel.getTotalVolumes());
        restrictionModelDto.setPrice(billingDataRestrictionModel.getPrice());
        restrictionModelDto.setCalculatedFrom(billingDataRestrictionModel.getCalculatedFrom());
        restrictionModelDto.setCalculatedTo(billingDataRestrictionModel.getCalculatedTo());
        restrictionModelDto.setPriceComponentId(billingDataRestrictionModel.getPriceComponentId());
        restrictionModelDto.setCurrencyId(billingDataRestrictionModel.getCurrencyId());
        return restrictionModelDto;
    }

    public static List<RestrictionModelDto> fromBillingDataRestrictionModelList(List<BillingDataRestrictionModel> billingDataRestrictionModelList) {
        return billingDataRestrictionModelList.stream()
                .map(RestrictionModelDto::fromBillingDataRestrictionModel)
                .collect(Collectors.toList());
    }

}
