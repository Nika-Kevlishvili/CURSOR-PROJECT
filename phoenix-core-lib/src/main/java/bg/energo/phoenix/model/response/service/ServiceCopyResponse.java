package bg.energo.phoenix.model.response.service;

import bg.energo.phoenix.model.response.interimAdvancePayment.InterimAdvancePaymentGroupShortResponse;
import bg.energo.phoenix.model.response.interimAdvancePayment.InterimAdvancePaymentShortResponse;
import bg.energo.phoenix.model.response.penalty.PenaltyShortResponse;
import bg.energo.phoenix.model.response.penaltyGroup.PenaltyGroupShortResponse;
import bg.energo.phoenix.model.response.priceComponent.PriceComponentGroupShortResponse;
import bg.energo.phoenix.model.response.priceComponent.PriceComponentShortResponse;
import bg.energo.phoenix.model.response.product.ProductServiceTemplateShortResponse;
import bg.energo.phoenix.model.response.terminationGroup.TerminationGroupShortResponse;
import bg.energo.phoenix.model.response.terminations.TerminationShortResponse;
import bg.energo.phoenix.model.response.terms.TermsShortResponse;
import bg.energo.phoenix.model.response.termsGroup.TermsGroupsShortResponse;
import lombok.Data;

import java.util.List;

@Data
public class ServiceCopyResponse {

    private ServiceBasicSettingsResponse basicSettings;
    private ServicePriceSettingsResponse priceSettings;
    private ServiceAdditionalSettingsResponse additionalSettings;
    private TermsShortResponse terms;
    private TermsGroupsShortResponse termGroup;

    private List<ServiceContractTermShortResponse> serviceContractTerms;
    private List<TerminationShortResponse> terminations;
    private List<TerminationGroupShortResponse> terminationGroups;
    private List<PenaltyShortResponse> penalties;
    private List<PenaltyGroupShortResponse> penaltyGroups;
    private List<InterimAdvancePaymentShortResponse> interimAdvancePayments;
    private List<InterimAdvancePaymentGroupShortResponse> interimAdvancePaymentGroups;
    private List<PriceComponentShortResponse> priceComponents;
    private List<PriceComponentGroupShortResponse> priceComponentGroups;
    private List<ServiceRelatedEntityShortResponse> relatedEntities;
    private List<ProductServiceTemplateShortResponse> templateResponses;
}