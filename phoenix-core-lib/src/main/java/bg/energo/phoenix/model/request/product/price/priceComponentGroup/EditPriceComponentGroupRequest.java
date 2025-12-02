package bg.energo.phoenix.model.request.product.price.priceComponentGroup;

import bg.energo.phoenix.model.customAnotations.product.priceComponentGroup.ValidEditPriceComponentGroupRequest;
import bg.energo.phoenix.model.request.product.price.priceComponentGroup.priceComponent.EditPriceComponentGroupPriceComponentRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.LocalDate;
import java.util.List;

@Data
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@ValidEditPriceComponentGroupRequest
public class EditPriceComponentGroupRequest extends BasePriceComponentGroupRequest {

    @NotNull(message = "versionId-[versionId] field must not be null;")
    private Long versionId;

    @NotNull(message = "updateExistingVersion-[updateExistingVersion] field must not be null;")
    private Boolean updateExistingVersion;

    private LocalDate startDate;

    private List<@Valid EditPriceComponentGroupPriceComponentRequest> priceComponentsList;

}
