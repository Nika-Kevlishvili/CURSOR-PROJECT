package bg.energo.phoenix.util.versionDates;

import bg.energo.phoenix.model.entity.contract.product.ProductContractDetails;
import bg.energo.phoenix.model.entity.contract.service.ServiceContractDetails;
import bg.energo.phoenix.model.entity.product.iap.advancedPaymentGroup.AdvancedPaymentGroupDetails;
import bg.energo.phoenix.model.entity.product.price.priceComponentGroup.PriceComponentGroupDetails;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VersionWithDatesModel {
    private Integer versionId;
    private LocalDate startDate;
    private LocalDate endDate;

    public VersionWithDatesModel(ProductContractDetails details) {
        this.versionId = details.getVersionId();
        this.startDate = details.getStartDate();
        this.endDate = details.getEndDate();
    }

    public VersionWithDatesModel(ServiceContractDetails details) {
        this.versionId = Math.toIntExact(details.getVersionId());
        this.startDate = details.getStartDate();
        this.endDate = details.getEndDate();
    }

    public VersionWithDatesModel(PriceComponentGroupDetails details) {
        this.versionId = Math.toIntExact(details.getVersionId());
        this.startDate = details.getStartDate();
        this.endDate = details.getEndDate();
    }

    public VersionWithDatesModel(AdvancedPaymentGroupDetails details) {
        this.versionId = Math.toIntExact(details.getVersionId());
        this.startDate = details.getStartDate();
        this.endDate = details.getEndDate();
    }
}
