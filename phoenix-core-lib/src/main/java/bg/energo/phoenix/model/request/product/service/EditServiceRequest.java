package bg.energo.phoenix.model.request.product.service;

import bg.energo.phoenix.model.customAnotations.DuplicatedValuesValidator;
import bg.energo.phoenix.model.customAnotations.product.service.ValidServiceTerms;
import bg.energo.phoenix.model.request.product.service.subObject.contractTerm.EditServiceContractTermRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ValidServiceTerms
public class EditServiceRequest extends BaseServiceRequest {

    @NotNull(message = "versionId-Version must not be null;")
    private Long versionId;

    @NotNull(message = "updateExistingVersion-[UpdateExistingVersion] field is required;")
    private Boolean updateExistingVersion;

    private List<@Valid EditServiceContractTermRequest> contractTerms;

    @DuplicatedValuesValidator(fieldPath = "serviceDetailIdsForUpdatingServiceContracts")
    private List<Long> serviceDetailIdsForUpdatingServiceContracts;
}
