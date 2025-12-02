package bg.energo.phoenix.model.response.service;

import bg.energo.phoenix.model.enums.product.service.ServiceStatus;
import bg.energo.phoenix.model.response.interimAdvancePayment.InterimAdvancePaymentGroupShortResponse;
import bg.energo.phoenix.model.response.interimAdvancePayment.InterimAdvancePaymentShortResponse;
import bg.energo.phoenix.model.response.penalty.PenaltyShortResponse;
import bg.energo.phoenix.model.response.penaltyGroup.PenaltyGroupShortResponse;
import bg.energo.phoenix.model.response.priceComponent.PriceComponentGroupShortResponse;
import bg.energo.phoenix.model.response.priceComponent.PriceComponentShortResponse;
import bg.energo.phoenix.model.response.product.ProductServiceTemplateShortResponse;
import bg.energo.phoenix.model.response.shared.FileWithStatusesResponse;
import bg.energo.phoenix.model.response.terminationGroup.TerminationGroupShortResponse;
import bg.energo.phoenix.model.response.terminations.TerminationShortResponse;
import bg.energo.phoenix.model.response.terms.TermsShortResponse;
import bg.energo.phoenix.model.response.termsGroup.TermsGroupsShortResponse;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ServiceResponse {

    // general information
    private Long id;
    private Long version;
    private ServiceStatus serviceStatus;
    private List<ServiceVersion> versions;
    private boolean isConnectedToContract; // for permission manipulation purposes in UI

    // service settings
    private ServiceBasicSettingsResponse basicSettings;
    private ServicePriceSettingsResponse priceSettings;
    private ServiceAdditionalSettingsResponse additionalSettings;

    // service subcomponents
    private TermsShortResponse term;
    private TermsGroupsShortResponse termGroup;
    private List<ServiceContractTermShortResponse> contractTerms;
    private List<InterimAdvancePaymentShortResponse> interimAdvancePayments;
    private List<InterimAdvancePaymentGroupShortResponse> interimAdvancePaymentGroups;
    private List<PriceComponentShortResponse> priceComponents;
    private List<PriceComponentGroupShortResponse> priceComponentGroups;
    private List<TerminationShortResponse> terminations;
    private List<TerminationGroupShortResponse> terminationGroups;
    private List<PenaltyShortResponse> penalties;
    private List<PenaltyGroupShortResponse> penaltyGroups;
    private List<ServiceRelatedEntityShortResponse> relatedEntities;
    private List<FileWithStatusesResponse> serviceFiles;
    private List<ProductServiceTemplateShortResponse> templateResponses;
    private boolean isLocked;
}
