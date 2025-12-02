package bg.energo.phoenix.model.request.product.price.priceComponent;

import bg.energo.phoenix.model.enums.product.price.priceComponent.IssuedSeparateInvoice;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = false)
@AllArgsConstructor
@NoArgsConstructor
public class PriceComponentFormulaRequest extends FormulaValidationRequest {
    private String condition;
    private String priceText;
    private IssuedSeparateInvoice issuedSeparateInvoice;

    private List<Long> segmentIds;
    private List<Long> preferenceIds;
    private List<Long> podCountryIds;
    private List<Long> podRegionIds;
    private List<Long> podPopulatedPlaceIds;
    private List<Long> podGridOperatorIds;
    private List<Long> podMeasurementIds;
    private List<Long> riskAssessmentIds;
    private List<Long> contractCampaignIds;
    private List<Long> priceParameterIds;
}
