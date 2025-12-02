package bg.energo.phoenix.model.request.product.service;

import bg.energo.phoenix.model.customAnotations.DuplicatedValuesValidator;
import bg.energo.phoenix.model.customAnotations.product.service.ServiceAdditionalParamsValidator;
import bg.energo.phoenix.model.customAnotations.product.service.ServiceRelatedEntitiesValidator;
import bg.energo.phoenix.model.customAnotations.product.service.ServiceTemplateValidator;
import bg.energo.phoenix.model.request.product.product.TemplateSubObjectRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Set;

@Data
@AllArgsConstructor
@NoArgsConstructor
@ServiceAdditionalParamsValidator
@ServiceTemplateValidator
public class BaseServiceRequest {

    @Valid
    @NotNull(message = "basicSettings-Basic settings must not be null;")
    private ServiceBasicSettingsRequest basicSettings;

    @Valid
    @NotNull(message = "priceSettings-Price settings must not be null;")
    private ServicePriceSettingsRequest priceSettings;

    @Valid
    @NotNull(message = "additionalSettings-Additional settings must no be null;")
    private ServiceAdditionalSettingsRequest additionalSettings;

    private Long term;
    private Long termGroup;

    @DuplicatedValuesValidator(fieldPath = "basicSettings.interimAdvancePayments")
    private List<Long> interimAdvancePayments;
    @DuplicatedValuesValidator(fieldPath = "basicSettings.interimAdvancePaymentGroups")
    private List<Long> interimAdvancePaymentGroups;

    @DuplicatedValuesValidator(fieldPath = "basicSettings.priceComponents")
    private List<Long> priceComponents;
    @DuplicatedValuesValidator(fieldPath = "basicSettings.priceComponentGroups")
    private List<Long> priceComponentGroups;

    @DuplicatedValuesValidator(fieldPath = "additionalSettings.terminations")
    private List<Long> terminations;
    @DuplicatedValuesValidator(fieldPath = "additionalSettings.terminationGroups")
    private List<Long> terminationGroups;

    @DuplicatedValuesValidator(fieldPath = "additionalSettings.penalties")
    private List<Long> penalties;
    @DuplicatedValuesValidator(fieldPath = "additionalSettings.penaltyGroups")
    private List<Long> penaltyGroups;

    @DuplicatedValuesValidator(fieldPath = "basicSettings.serviceFiles")
    private List<Long> serviceFiles;

    @ServiceRelatedEntitiesValidator
    private List<ServiceRelatedEntityRequest> relatedEntities;

    private Set<TemplateSubObjectRequest> templateIds;
}
