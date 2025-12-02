package bg.energo.phoenix.service.product.service;

import bg.energo.phoenix.model.entity.BaseEntity;
import bg.energo.phoenix.model.entity.nomenclature.product.Currency;
import bg.energo.phoenix.model.entity.nomenclature.product.VatRate;
import bg.energo.phoenix.model.entity.nomenclature.product.service.ServiceGroups;
import bg.energo.phoenix.model.entity.nomenclature.product.service.ServiceType;
import bg.energo.phoenix.model.entity.nomenclature.product.service.ServiceUnit;
import bg.energo.phoenix.model.entity.product.service.*;
import bg.energo.phoenix.model.entity.product.term.terms.Terms;
import bg.energo.phoenix.model.entity.product.term.termsGroups.TermsGroups;
import bg.energo.phoenix.model.enums.nomenclature.NomenclatureItemStatus;
import bg.energo.phoenix.model.enums.product.product.PaymentGuarantee;
import bg.energo.phoenix.model.enums.product.service.*;
import bg.energo.phoenix.model.request.product.service.BaseServiceRequest;
import bg.energo.phoenix.model.request.product.service.ServiceAdditionalSettingsRequest;
import bg.energo.phoenix.model.request.product.service.ServiceBasicSettingsRequest;
import bg.energo.phoenix.model.request.product.service.ServicePriceSettingsRequest;
import bg.energo.phoenix.model.request.product.service.subObject.contractTerm.BaseServiceContractTermRequest;
import bg.energo.phoenix.model.request.product.service.subObject.contractTerm.EditServiceContractTermRequest;
import bg.energo.phoenix.model.response.interimAdvancePayment.InterimAdvancePaymentShortResponse;
import bg.energo.phoenix.model.response.nomenclature.product.GridOperatorResponse;
import bg.energo.phoenix.model.response.penalty.PenaltyShortResponse;
import bg.energo.phoenix.model.response.priceComponent.PriceComponentShortResponse;
import bg.energo.phoenix.model.response.service.*;
import bg.energo.phoenix.model.response.terminations.TerminationShortResponse;
import bg.energo.phoenix.model.response.terms.TermsShortResponse;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;

@Service
public class ServiceMapper {

    public ServiceDetails fromRequestToServiceDetailsEntity(BaseServiceRequest request,
                                                            EPService service,
                                                            ServiceGroups serviceGroup,
                                                            ServiceType serviceType,
                                                            VatRate vatRate,
                                                            ServiceUnit serviceUnit,
                                                            Currency cashDepositCurrency,
                                                            Currency bankGuaranteeCurrency,
                                                            Currency priceSettingsCurrency,
                                                            Terms terms,
                                                            TermsGroups termsGroups) {
        ServiceBasicSettingsRequest basicSettings = request.getBasicSettings();
        ServicePriceSettingsRequest priceSettings = request.getPriceSettings();
        ServiceAdditionalSettingsRequest additionalSettings = request.getAdditionalSettings();

        return ServiceDetails.builder()
                // BASIC SETTINGS
                .service(service)
                .name(BooleanUtils.isTrue(basicSettings.getIsIndividual()) ? String.valueOf(service.getId()) : basicSettings.getName())
                .nameTransliterated(BooleanUtils.isTrue(basicSettings.getIsIndividual()) ? String.valueOf(service.getId()) : basicSettings.getNameTransliterated())
                .status(basicSettings.getServiceDetailStatus())
                .availableForSale(basicSettings.getAvailableForSale())
                .availableFrom(basicSettings.getAvailableFrom())
                .availableTo(basicSettings.getAvailableTo())
                .printingName(basicSettings.getPrintingName())
                .printingNameTransliterated(basicSettings.getPrintingNameTransliterated())
                .shortDescription(basicSettings.getShortDescription())
                .fullDescription(basicSettings.getFullDescription())
                .invoiceAndTemplatesText(basicSettings.getInvoiceAndTemplatesText())
                .invoiceAndTemplatesTextTransliterated(basicSettings.getInvoiceAndTemplatesTextTransliterated())
                .serviceGroup(serviceGroup)
                .terms(terms)
                .termsGroups(termsGroups)
                .otherSystemConnectionCode(basicSettings.getOtherSystemConnectionCode())
                .serviceType(serviceType)
                .saleMethods(basicSettings.getSaleMethods().stream().toList()) // mandatory, will be present
                .paymentGuarantees(basicSettings.getPaymentGuarantees().stream().toList()) // mandatory, will be present
                .cashDepositAmount(basicSettings.getCashDepositAmount())
                .cashDepositCurrency(cashDepositCurrency)
                .bankGuaranteeAmount(basicSettings.getBankGuaranteeAmount())
                .bankGuaranteeCurrency(bankGuaranteeCurrency)
                .consumptionPurposes(CollectionUtils.isEmpty(basicSettings.getConsumptionPurposes()) ? null : basicSettings.getConsumptionPurposes().stream().toList())
                .podMeteringTypes(CollectionUtils.isEmpty(basicSettings.getPodMeteringTypes()) ? null : basicSettings.getPodMeteringTypes().stream().toList())
                .voltageLevels(CollectionUtils.isEmpty(basicSettings.getVoltageLevels()) ? null : basicSettings.getVoltageLevels().stream().toList())
                .podTypes(CollectionUtils.isEmpty(basicSettings.getTypePointsOfDelivery()) ? null : basicSettings.getTypePointsOfDelivery().stream().toList())
                .incomeAccountNumber(basicSettings.getIncomeAccountNumber())
                .costCenterControllingOrder(basicSettings.getCostCenterControllingOrder())
                .globalVatRate(basicSettings.getGlobalVatRate())
                .vatRate(vatRate)
                .serviceUnit(serviceUnit)
                .capacityLimitType(basicSettings.getCapacityLimitType())
                .capacityLimitAmount(basicSettings.getCapacityLimitAmount())
                .globalSalesArea(basicSettings.getGlobalSalesAreas())
                .globalSalesChannel(basicSettings.getGlobalSalesChannel())
                .globalSegment(basicSettings.getGlobalSegment())
                .globalGridOperator(basicSettings.getGlobalGridOperator())
                // PRICE SETTINGS
                .equalMonthlyInstallmentsActivation(priceSettings.getEqualMonthlyInstallmentsActivation())
                .installmentNumberFrom(priceSettings.getInstallmentNumberFrom())
                .installmentNumber(priceSettings.getInstallmentNumber())
                .installmentNumberTo(priceSettings.getInstallmentNumberTo())
                .amountFrom(priceSettings.getAmountFrom())
                .amount(priceSettings.getAmount())
                .amountTo(priceSettings.getAmountTo())
                .currency(priceSettingsCurrency)
                // ADDITIONAL SETTINGS
                .servicePeriodicity(additionalSettings.getPeriodicity())
                .paymentMethod(additionalSettings.getPaymentMethod())
                .paymentBeforeExecution(additionalSettings.getPaymentBeforeExecution())
                .executionLevel(additionalSettings.getExecutionLevel())
                .additionalField1(additionalSettings.getAdditionalField1())
                .additionalField2(additionalSettings.getAdditionalField2())
                .additionalField3(additionalSettings.getAdditionalField3())
                .additionalField4(additionalSettings.getAdditionalField4())
                .additionalField5(additionalSettings.getAdditionalField5())
                .additionalField6(additionalSettings.getAdditionalField6())
                .additionalField7(additionalSettings.getAdditionalField7())
                .additionalField8(additionalSettings.getAdditionalField8())
                .additionalField9(additionalSettings.getAdditionalField9())
                .additionalField10(additionalSettings.getAdditionalField10())
                .build();
    }


    public ServiceDetails updateServiceDetail(ServiceDetails serviceDetails,
                                              BaseServiceRequest request,
                                              ServiceGroups serviceGroup,
                                              ServiceType serviceType,
                                              VatRate vatRate,
                                              ServiceUnit serviceUnit,
                                              Currency cashDepositCurrency,
                                              Currency bankGuaranteeCurrency,
                                              Currency priceSettingsCurrency,
                                              Terms terms,
                                              TermsGroups termsGroups) {
        ServiceBasicSettingsRequest basicSettings = request.getBasicSettings();
        ServicePriceSettingsRequest priceSettings = request.getPriceSettings();
        ServiceAdditionalSettingsRequest additionalSettings = request.getAdditionalSettings();

        // BASIC SETTINGS
        serviceDetails.setName(BooleanUtils.isTrue(basicSettings.getIsIndividual()) ? serviceDetails.getName() : basicSettings.getName());
        serviceDetails.setNameTransliterated(BooleanUtils.isTrue(basicSettings.getIsIndividual()) ? serviceDetails.getNameTransliterated() : basicSettings.getNameTransliterated());
        serviceDetails.setStatus(basicSettings.getServiceDetailStatus());
        serviceDetails.setAvailableForSale(basicSettings.getAvailableForSale());
        serviceDetails.setAvailableFrom(basicSettings.getAvailableFrom());
        serviceDetails.setAvailableTo(basicSettings.getAvailableTo());
        serviceDetails.setPrintingName(basicSettings.getPrintingName());
        serviceDetails.setPrintingNameTransliterated(basicSettings.getPrintingNameTransliterated());
        serviceDetails.setShortDescription(basicSettings.getShortDescription());
        serviceDetails.setFullDescription(basicSettings.getFullDescription());
        serviceDetails.setInvoiceAndTemplatesText(basicSettings.getInvoiceAndTemplatesText());
        serviceDetails.setInvoiceAndTemplatesTextTransliterated(basicSettings.getInvoiceAndTemplatesTextTransliterated());
        serviceDetails.setServiceGroup(serviceGroup);
        serviceDetails.setTerms(terms);
        serviceDetails.setTermsGroups(termsGroups);
        serviceDetails.setOtherSystemConnectionCode(basicSettings.getOtherSystemConnectionCode());
        serviceDetails.setServiceType(serviceType);
        serviceDetails.setSaleMethods(basicSettings.getSaleMethods().stream().toList()); // mandatory, will be present
        serviceDetails.setPaymentGuarantees(basicSettings.getPaymentGuarantees().stream().toList()); // mandatory, will be present
        serviceDetails.setCashDepositAmount(basicSettings.getCashDepositAmount());
        serviceDetails.setCashDepositCurrency(cashDepositCurrency);
        serviceDetails.setBankGuaranteeAmount(basicSettings.getBankGuaranteeAmount());
        serviceDetails.setBankGuaranteeCurrency(bankGuaranteeCurrency);
        serviceDetails.setCapacityLimitType(basicSettings.getCapacityLimitType());
        serviceDetails.setCapacityLimitAmount(basicSettings.getCapacityLimitAmount());
        serviceDetails.setConsumptionPurposes(CollectionUtils.isEmpty(basicSettings.getConsumptionPurposes()) ? null : basicSettings.getConsumptionPurposes().stream().toList());
        serviceDetails.setPodMeteringTypes(CollectionUtils.isEmpty(basicSettings.getPodMeteringTypes()) ? null : basicSettings.getPodMeteringTypes().stream().toList());
        serviceDetails.setVoltageLevels(CollectionUtils.isEmpty(basicSettings.getVoltageLevels()) ? null : basicSettings.getVoltageLevels().stream().toList());
        serviceDetails.setPodTypes(CollectionUtils.isEmpty(basicSettings.getTypePointsOfDelivery()) ? null : basicSettings.getTypePointsOfDelivery().stream().toList());
        serviceDetails.setIncomeAccountNumber(basicSettings.getIncomeAccountNumber());
        serviceDetails.setCostCenterControllingOrder(basicSettings.getCostCenterControllingOrder());
        serviceDetails.setGlobalVatRate(basicSettings.getGlobalVatRate());
        serviceDetails.setVatRate(vatRate);
        serviceDetails.setServiceUnit(serviceUnit);
        serviceDetails.setGlobalSalesArea(basicSettings.getGlobalSalesAreas());
        serviceDetails.setGlobalSalesChannel(basicSettings.getGlobalSalesChannel());
        serviceDetails.setGlobalSegment(basicSettings.getGlobalSegment());
        serviceDetails.setGlobalGridOperator(basicSettings.getGlobalGridOperator());

        // PRICE SETTINGS
        serviceDetails.setEqualMonthlyInstallmentsActivation(priceSettings.getEqualMonthlyInstallmentsActivation());
        serviceDetails.setInstallmentNumberFrom(priceSettings.getInstallmentNumberFrom());
        serviceDetails.setInstallmentNumber(priceSettings.getInstallmentNumber());
        serviceDetails.setInstallmentNumberTo(priceSettings.getInstallmentNumberTo());
        serviceDetails.setAmountFrom(priceSettings.getAmountFrom());
        serviceDetails.setAmount(priceSettings.getAmount());
        serviceDetails.setAmountTo(priceSettings.getAmountTo());
        serviceDetails.setCurrency(priceSettingsCurrency);

        // ADDITIONAL SETTINGS
        serviceDetails.setServicePeriodicity(additionalSettings.getPeriodicity());
        serviceDetails.setPaymentMethod(additionalSettings.getPaymentMethod());
        serviceDetails.setPaymentBeforeExecution(additionalSettings.getPaymentBeforeExecution());
        serviceDetails.setExecutionLevel(additionalSettings.getExecutionLevel());
        serviceDetails.setAdditionalField1(additionalSettings.getAdditionalField1());
        serviceDetails.setAdditionalField2(additionalSettings.getAdditionalField2());
        serviceDetails.setAdditionalField3(additionalSettings.getAdditionalField3());
        serviceDetails.setAdditionalField4(additionalSettings.getAdditionalField4());
        serviceDetails.setAdditionalField5(additionalSettings.getAdditionalField5());
        serviceDetails.setAdditionalField6(additionalSettings.getAdditionalField6());
        serviceDetails.setAdditionalField7(additionalSettings.getAdditionalField7());
        serviceDetails.setAdditionalField8(additionalSettings.getAdditionalField8());
        serviceDetails.setAdditionalField9(additionalSettings.getAdditionalField9());
        serviceDetails.setAdditionalField10(additionalSettings.getAdditionalField10());

        return serviceDetails;
    }


    public ServiceContractTerm fromCreateRequestToServiceContractTermEntity(BaseServiceContractTermRequest request,
                                                                            ServiceDetails serviceDetails) {
        return ServiceContractTerm.builder()
                .serviceDetails(serviceDetails)
                .name(request.getName())
                .perpetuityClause(request.getPerpetuityCause())
                .periodType(request.getPeriodType())
                .termType(request.getTermType())
                .value(request.getValue())
                .status(ServiceSubobjectStatus.ACTIVE)
                .automaticRenewal(request.getAutomaticRenewal())
                .numberOfRenewals(request.getNumberOfRenewals())
                .renewalPeriodType(request.getRenewalPeriodType())
                .renewalPeriodValue(request.getRenewalPeriodValue())
                .build();
    }


    public ServiceContractTerm fromEditRequestToServiceContractTermEntity(EditServiceContractTermRequest request,
                                                                          ServiceContractTerm dbServiceContractTerm,
                                                                          ServiceDetails serviceDetails) {
        dbServiceContractTerm.setName(request.getName());
        dbServiceContractTerm.setPerpetuityClause(request.getPerpetuityCause());
        dbServiceContractTerm.setPeriodType(request.getPeriodType());
        dbServiceContractTerm.setTermType(request.getTermType());
        dbServiceContractTerm.setValue(request.getValue());
        dbServiceContractTerm.setStatus(ServiceSubobjectStatus.ACTIVE);
        dbServiceContractTerm.setServiceDetails(serviceDetails);
        dbServiceContractTerm.setAutomaticRenewal(request.getAutomaticRenewal());
        dbServiceContractTerm.setNumberOfRenewals(request.getNumberOfRenewals());
        dbServiceContractTerm.setRenewalPeriodType(request.getRenewalPeriodType());
        dbServiceContractTerm.setRenewalPeriodValue(request.getRenewalPeriodValue());
        return dbServiceContractTerm;
    }


    public ServiceResponse fromEntityToResponse(EPService service,
                                                ServiceDetails details,
                                                List<ServiceVersion> versions,
                                                ServiceBasicSettingsResponse basicSettings,
                                                ServicePriceSettingsResponse priceSettings,
                                                ServiceAdditionalSettingsResponse additionalSettings) {
        ServiceResponse serviceResponse = new ServiceResponse();
        serviceResponse.setId(service.getId());
        serviceResponse.setVersion(details.getVersion());
        serviceResponse.setServiceStatus(service.getStatus());
        serviceResponse.setVersions(versions);
        serviceResponse.setBasicSettings(basicSettings);
        serviceResponse.setPriceSettings(priceSettings);
        serviceResponse.setAdditionalSettings(additionalSettings);
        return serviceResponse;
    }


    public ServiceBasicSettingsResponse fromDetailsEntityToBasicSettingsResponse(ServiceDetails details, List<NomenclatureItemStatus> statuses, EPService service) {
        ServiceBasicSettingsResponse response = new ServiceBasicSettingsResponse();
        response.setIsIndividual(StringUtils.isNotEmpty(service.getCustomerIdentifier()));
        response.setCustomerIdentifier(service.getCustomerIdentifier());
        response.setName(details.getName());
        response.setNameTransliterated(details.getNameTransliterated());
        response.setPrintingName(details.getPrintingName());
        response.setPrintingNameTransliterated(details.getPrintingNameTransliterated());
        response.setOtherSystemConnectionCode(details.getOtherSystemConnectionCode());

        ServiceGroups serviceGroup = details.getServiceGroup();
        if (serviceGroup != null && statuses.contains(serviceGroup.getStatus())) {
            response.setServiceGroupId(details.getServiceGroup().getId());
            response.setServiceGroupName(details.getServiceGroup().getName());
            response.setServiceGroupNameTransl(details.getServiceGroup().getNameTransliterated());
        }

        ServiceType serviceType = details.getServiceType();
        if (serviceType != null && statuses.contains(serviceType.getStatus())) {
            response.setServiceTypeId(serviceType.getId());
            response.setServiceTypeName(serviceType.getName());
        }

        List<ServiceSaleMethod> saleMethods = details.getSaleMethods();
        if (saleMethods != null) {
            response.setSaleMethods(new HashSet<>(saleMethods));
        }

        List<PaymentGuarantee> paymentGuarantees = details.getPaymentGuarantees();
        if (paymentGuarantees != null) {
            response.setPaymentGuarantees(new HashSet<>(paymentGuarantees));
        }

        List<ServiceConsumptionPurpose> consumptionPurposes = details.getConsumptionPurposes();
        if (consumptionPurposes != null) {
            response.setConsumptionPurposes(new HashSet<>(consumptionPurposes));
        }

        List<ServicePODMeteringType> podMeteringTypes = details.getPodMeteringTypes();
        if (podMeteringTypes != null) {
            response.setPodMeteringTypes(new HashSet<>(podMeteringTypes));
        }

        List<ServiceVoltageLevel> voltageLevels = details.getVoltageLevels();
        if (voltageLevels != null) {
            response.setVoltageLevels(new HashSet<>(voltageLevels));
        }

        List<ServicePodType> podTypes = details.getPodTypes();
        if (podTypes != null) {
            response.setTypePointsOfDelivery(new HashSet<>(podTypes));
        }

        response.setServiceDetailStatus(details.getStatus());
        response.setAvailableForSale(details.getAvailableForSale());
        response.setAvailableTo(details.getAvailableTo());
        response.setAvailableFrom(details.getAvailableFrom());
        response.setShortDescription(details.getShortDescription());
        response.setFullDescription(details.getFullDescription());
        response.setInvoiceAndTemplatesText(details.getInvoiceAndTemplatesText());
        response.setInvoiceAndTemplatesTextTransliterated(details.getInvoiceAndTemplatesTextTransliterated());
        response.setIncomeAccountNumber(details.getIncomeAccountNumber());
        response.setCostCenterControllingOrder(details.getCostCenterControllingOrder());
        response.setGlobalVatRate(details.getGlobalVatRate());

        VatRate vatRate = details.getVatRate();
        if (vatRate != null && !details.getGlobalVatRate() && statuses.contains(vatRate.getStatus())) {
            response.setVatRateName(vatRate.getName());
            response.setVatRateId(vatRate.getId());
        }

        ServiceUnit serviceUnit = details.getServiceUnit();
        if (serviceUnit != null && statuses.contains(serviceUnit.getStatus())) {
            response.setServiceUnitId(serviceUnit.getId());
            response.setServiceUnitName(serviceUnit.getName());
        }

        response.setCashDepositAmount(details.getCashDepositAmount());
        Currency cashDepositCurrency = details.getCashDepositCurrency();
        if (cashDepositCurrency != null && statuses.contains(cashDepositCurrency.getStatus())) {
            response.setCashDepositCurrencyId(cashDepositCurrency.getId());
            response.setCashDepositCurrencyName(cashDepositCurrency.getName());
        }

        response.setBankGuaranteeAmount(details.getBankGuaranteeAmount());
        Currency bankGuaranteeCurrency = details.getBankGuaranteeCurrency();
        if (bankGuaranteeCurrency != null && statuses.contains(bankGuaranteeCurrency.getStatus())) {
            response.setBankGuaranteeCurrencyId(bankGuaranteeCurrency.getId());
            response.setBankGuaranteeCurrencyName(bankGuaranteeCurrency.getName());
        }

        response.setGlobalSalesChannel(details.getGlobalSalesChannel());
        response.setGlobalSalesAreas(details.getGlobalSalesArea());
        response.setGlobalSegment(details.getGlobalSegment());
        response.setGlobalGridOperator(details.getGlobalGridOperator());
        response.setCapacityLimitType(details.getCapacityLimitType());
        response.setCapacityLimitAmount(details.getCapacityLimitAmount());
        return response;
    }

    public ServiceAdditionalSettingsResponse fromDetailsEntityToAdditionalSettingsResponse(ServiceDetails details) {
        ServiceAdditionalSettingsResponse additionalSettings = new ServiceAdditionalSettingsResponse();
        additionalSettings.setPeriodicity(details.getServicePeriodicity());
        additionalSettings.setPaymentMethod(details.getPaymentMethod());
        additionalSettings.setPaymentBeforeExecution(details.getPaymentBeforeExecution());
        additionalSettings.setExecutionLevel(details.getExecutionLevel());
        additionalSettings.setAdditionalField1(details.getAdditionalField1());
        additionalSettings.setAdditionalField2(details.getAdditionalField2());
        additionalSettings.setAdditionalField3(details.getAdditionalField3());
        additionalSettings.setAdditionalField4(details.getAdditionalField4());
        additionalSettings.setAdditionalField5(details.getAdditionalField5());
        additionalSettings.setAdditionalField6(details.getAdditionalField6());
        additionalSettings.setAdditionalField7(details.getAdditionalField7());
        additionalSettings.setAdditionalField8(details.getAdditionalField8());
        additionalSettings.setAdditionalField9(details.getAdditionalField9());
        additionalSettings.setAdditionalField10(details.getAdditionalField10());
        additionalSettings.setServiceAdditionalParams(mapAdditionalParams(details));

        List<ServiceIneligiblePaymentChannel> ineligiblePaymentChannels = details.getIneligiblePaymentChannels();

        if (ineligiblePaymentChannels != null) {
            additionalSettings.setIneligiblePaymentChannels(new HashSet<>(ineligiblePaymentChannels));
        }

        return additionalSettings;
    }

    public ServicePriceSettingsResponse fromDetailsEntityToPriceSettingsResponse(ServiceDetails details, List<NomenclatureItemStatus> statuses) {
        ServicePriceSettingsResponse priceSettings = new ServicePriceSettingsResponse();
        priceSettings.setEqualMonthlyInstallmentsActivation(details.getEqualMonthlyInstallmentsActivation());
        priceSettings.setInstallmentNumber(details.getInstallmentNumber());
        priceSettings.setInstallmentNumberFrom(details.getInstallmentNumberFrom());
        priceSettings.setInstallmentNumberTo(details.getInstallmentNumberTo());
        priceSettings.setAmount(details.getAmount());
        priceSettings.setAmountFrom(details.getAmountFrom());
        priceSettings.setAmountTo(details.getAmountTo());

        Currency currency = details.getCurrency();
        if (currency != null && statuses.contains(currency.getStatus())) {
            priceSettings.setCurrencyId(currency.getId());
            priceSettings.setCurrencyName(currency.getName());
        }

        return priceSettings;
    }

    public List<PriceComponentShortResponse> createPriceComponentResponse(ServiceDetails details) {
        List<ServicePriceComponent> priceComponents = details.getPriceComponents();
        return priceComponents.stream().filter(x -> x.getStatus().equals(ServiceSubobjectStatus.ACTIVE))
                .sorted(Comparator.comparing(BaseEntity::getCreateDate))
                .map(ServicePriceComponent::getPriceComponent)
                .map(PriceComponentShortResponse::new)
                .toList();
    }

    public List<TerminationShortResponse> createTerminationResponse(ServiceDetails details) {
        List<ServiceTermination> terminations = details.getTerminations();
        return terminations.stream().filter(x -> x.getStatus().equals(ServiceSubobjectStatus.ACTIVE))
                .sorted(Comparator.comparing(BaseEntity::getCreateDate))
                .map(ServiceTermination::getTermination)
                .map(TerminationShortResponse::new)
                .toList();
    }

    public List<PenaltyShortResponse> createPenaltyResponse(ServiceDetails details) {
        List<ServicePenalty> penalties = details.getPenalties();
        return penalties.stream().filter(x -> x.getStatus().equals(ServiceSubobjectStatus.ACTIVE))
                .sorted(Comparator.comparing(BaseEntity::getCreateDate))
                .map(ServicePenalty::getPenalty)
                .map(PenaltyShortResponse::new)
                .toList();
    }

    public List<ServiceContractTermShortResponse> createServiceContractTermsResponse(ServiceDetails productDetails) {
        return productDetails
                .getContractTerms()
                .stream().filter(x -> x.getStatus().equals(ServiceSubobjectStatus.ACTIVE))
                .sorted(Comparator.comparing(BaseEntity::getCreateDate))
                .map(ServiceContractTermShortResponse::new)
                .toList();
    }

    public List<ServiceContractTermShortResponse> createServiceContractTermsResponseForCopy(ServiceDetails productDetails) {
        List<ServiceContractTerm> contractTerms = productDetails.getContractTerms().stream().filter(serviceContractTerm ->
                serviceContractTerm.getStatus() == ServiceSubobjectStatus.ACTIVE).toList();
        List<ServiceContractTermShortResponse> responses = new ArrayList<>();
        for (ServiceContractTerm contractTerm : contractTerms) {
            ServiceContractTermShortResponse response = new ServiceContractTermShortResponse();
            response.setName(contractTerm.getName());
            response.setPerpetuityCause(contractTerm.getPerpetuityClause());
            response.setPeriodType(contractTerm.getPeriodType());
            response.setTermType(contractTerm.getTermType());
            response.setValue(contractTerm.getValue());
            response.setAutomaticRenewal(contractTerm.getAutomaticRenewal());
            response.setNumberOfRenewals(contractTerm.getNumberOfRenewals());
            response.setRenewalPeriodType(contractTerm.getRenewalPeriodType());
            response.setRenewalPeriodValue(contractTerm.getRenewalPeriodValue());
            responses.add(response);
        }
        return responses;
    }

    public List<InterimAdvancePaymentShortResponse> createIAPShortResponse(ServiceDetails details) {
        List<ServiceInterimAndAdvancePayment> interimAndAdvancePayments = details.getInterimAndAdvancePayments();
        return interimAndAdvancePayments.stream()
                .filter(x -> x.getStatus().equals(ServiceSubobjectStatus.ACTIVE))
                .sorted(Comparator.comparing(BaseEntity::getCreateDate))
                .map(ServiceInterimAndAdvancePayment::getInterimAndAdvancePayment)
                .map(InterimAdvancePaymentShortResponse::new).toList();
    }

    public List<GridOperatorResponse> createGridOperatorResponse(ServiceDetails details, List<NomenclatureItemStatus> statuses) {
        List<ServiceGridOperator> gridOperator = details.getGridOperator();
        return gridOperator
                .stream()
                .filter(x -> x.getStatus().equals(ServiceSubobjectStatus.ACTIVE))
                .map(ServiceGridOperator::getGridOperator)
                .filter(x -> statuses.contains(x.getStatus()))
                .map(GridOperatorResponse::new).toList();
    }

    public TermsShortResponse createTermsShortResponse(ServiceDetails serviceDetails) {
        if (serviceDetails.getTerms() == null) {
            return null;
        }
        return new TermsShortResponse(serviceDetails.getTerms());
    }

    public ServiceCopyResponse fromEntityToCopyResponse(EPService service, ServiceDetails details) {
        ServiceCopyResponse serviceCopyResponse = new ServiceCopyResponse();
        serviceCopyResponse.setBasicSettings(fromDetailsEntityToBasicSettingsResponse(details, List.of(NomenclatureItemStatus.ACTIVE), service));
        serviceCopyResponse.setPriceSettings(fromDetailsEntityToPriceSettingsResponse(details, List.of(NomenclatureItemStatus.ACTIVE)));
        serviceCopyResponse.setAdditionalSettings(fromDetailsEntityToAdditionalSettingsResponse(details));
        return serviceCopyResponse;
    }

    public List<ServiceAdditionalParamsResponse> mapAdditionalParams(ServiceDetails details) {
        List<ServiceAdditionalParamsResponse> serviceAdditionalParams = new ArrayList<>();
        details.getServiceAdditionalParams()
                .stream()
                .filter(adPar -> adPar.getLabel() != null)
                .forEach(
                it -> {
                    ServiceAdditionalParamsResponse serviceAdditionalParam = new ServiceAdditionalParamsResponse(
                            it.getOrderingId(),
                            it.getServiceDetailId(),
                            it.getLabel(),
                            it.getValue()
                    );
                    serviceAdditionalParams.add(serviceAdditionalParam);
                }
        );
        return serviceAdditionalParams;
    }

}
