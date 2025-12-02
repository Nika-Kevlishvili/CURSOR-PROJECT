package bg.energo.phoenix.service.customer;

import bg.energo.phoenix.model.entity.customer.Customer;
import bg.energo.phoenix.model.entity.customer.CustomerDetails;
import bg.energo.phoenix.model.entity.customer.CustomerPreference;
import bg.energo.phoenix.model.entity.customer.CustomerSegment;
import bg.energo.phoenix.model.entity.nomenclature.address.Municipality;
import bg.energo.phoenix.model.entity.nomenclature.address.PopulatedPlace;
import bg.energo.phoenix.model.entity.nomenclature.address.ZipCode;
import bg.energo.phoenix.model.entity.nomenclature.customer.Bank;
import bg.energo.phoenix.model.entity.nomenclature.customer.CreditRating;
import bg.energo.phoenix.model.enums.contract.ContractFileType;
import bg.energo.phoenix.model.enums.customer.CustomerDetailStatus;
import bg.energo.phoenix.model.enums.customer.CustomerStatus;
import bg.energo.phoenix.model.enums.nomenclature.NomenclatureItemStatus;
import bg.energo.phoenix.model.request.contract.ProxyEditRequest;
import bg.energo.phoenix.model.request.contract.product.*;
import bg.energo.phoenix.model.request.contract.product.additionalParameters.ProductContractAdditionalParametersRequest;
import bg.energo.phoenix.model.request.contract.product.additionalParameters.ProductContractBankingDetails;
import bg.energo.phoenix.model.request.contract.relatedEntities.RelatedEntityRequest;
import bg.energo.phoenix.model.request.contract.service.ServiceContractAdditionalParametersRequest;
import bg.energo.phoenix.model.request.contract.service.ServiceContractBankingDetails;
import bg.energo.phoenix.model.request.contract.service.ServiceContractBasicParametersResponse;
import bg.energo.phoenix.model.request.contract.service.ServiceContractInterimAdvancePaymentsRequest;
import bg.energo.phoenix.model.request.contract.service.edit.*;
import bg.energo.phoenix.model.request.customer.CustomerVersionsResponse;
import bg.energo.phoenix.model.response.activity.SystemActivityShortResponse;
import bg.energo.phoenix.model.response.contract.ContractFileResponse;
import bg.energo.phoenix.model.response.contract.pods.ContractPodsResponseImpl;
import bg.energo.phoenix.model.response.contract.productContract.*;
import bg.energo.phoenix.model.response.contract.serviceContract.*;
import bg.energo.phoenix.model.response.customer.ConnectedGroupResponse;
import bg.energo.phoenix.model.response.customer.CustomerResponse;
import bg.energo.phoenix.model.response.customer.CustomerShortResponse;
import bg.energo.phoenix.model.response.customer.CustomerViewResponse;
import bg.energo.phoenix.model.response.customer.communicationData.ForeignAddressInfo;
import bg.energo.phoenix.model.response.customer.communicationData.LocalAddressInfo;
import bg.energo.phoenix.model.response.customer.communicationData.detailed.ContactPersonDetailedResponse;
import bg.energo.phoenix.model.response.customer.communicationData.detailed.CustomerAddress;
import bg.energo.phoenix.model.response.customer.communicationData.detailed.CustomerCommunicationsDetailedResponse;
import bg.energo.phoenix.model.response.customer.customerAccountManager.CustomerAccountManagerResponse;
import bg.energo.phoenix.model.response.customer.manager.ManagerResponse;
import bg.energo.phoenix.model.response.customer.owner.CustomerOwnerDetailResponse;
import bg.energo.phoenix.model.response.customer.relatedCustomer.RelatedCustomerResponse;
import bg.energo.phoenix.model.response.nomenclature.address.*;
import bg.energo.phoenix.model.response.nomenclature.contract.ContractVersionTypesResponse;
import bg.energo.phoenix.model.response.nomenclature.customer.*;
import bg.energo.phoenix.model.response.shared.FileWithStatusesResponse;
import bg.energo.phoenix.model.response.task.TaskShortResponse;
import bg.energo.phoenix.repository.customer.CustomerRepository;
import bg.energo.phoenix.repository.nomenclature.address.PopulatedPlaceRepository;
import bg.energo.phoenix.security.PermissionService;
import bg.energo.phoenix.service.nomenclature.address.*;
import bg.energo.phoenix.service.nomenclature.customer.EconomicBranchCIService;
import bg.energo.phoenix.service.nomenclature.customer.EconomicBranchNCEAService;
import bg.energo.phoenix.service.nomenclature.customer.LegalFormService;
import bg.energo.phoenix.service.nomenclature.customer.OwnershipFormService;
import bg.energo.phoenix.service.riskList.model.RiskListDecision;
import bg.energo.phoenix.util.epb.EPBFinalFields;
import lombok.RequiredArgsConstructor;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

import static bg.energo.phoenix.model.enums.customer.CustomerType.LEGAL_ENTITY;
import static bg.energo.phoenix.model.enums.customer.CustomerType.PRIVATE_CUSTOMER;
import static bg.energo.phoenix.permissions.PermissionContextEnum.CUSTOMER;
import static bg.energo.phoenix.permissions.PermissionEnum.CUSTOMER_VIEW_BASIC;
import static bg.energo.phoenix.permissions.PermissionEnum.CUSTOMER_VIEW_BASIC_AM;

@Service
@RequiredArgsConstructor
public class CustomerMapperService {
    private final LegalFormService legalFormService;
    private final DistrictService districtService;
    private final ResidentialAreaService residentialAreaService;
    private final StreetService streetService;
    private final CountryService countryService;
    private final PopulatedPlaceService populatedPlaceService;
    private final EconomicBranchCIService economicBranchCIService;
    private final EconomicBranchNCEAService economicBranchNCEAService;
    private final UnwantedCustomerService unwantedCustomerService;
    private final OwnershipFormService ownershipFormService;
    private final PermissionService permissionService;
    private final CustomerDetailsService customerDetailsService;
    private final CustomerSegmentService customerSegmentService;
    private final CustomerPreferenceService customerPreferenceService;
    private final PopulatedPlaceRepository populatedPlaceRepository;
    private final CustomerRepository customerRepository;

    /**
     * <h1>Modify Customer View Response</h1>
     * function adds GDPR masking based on the authorized user permissions
     *
     * @param responses list of {@link CustomerViewResponse} object
     * @return {@link CustomerViewResponse} object
     */
    public CustomerViewResponse modifyCustomerViewResponse(CustomerViewResponse responses) {
        if (checkGdpr()) {
            return responses;
        }
        List<RelatedCustomerResponse> relatedCustomers = responses.getRelatedCustomers();
        if (relatedCustomers != null) {
            for (RelatedCustomerResponse relatedCustomer : relatedCustomers) {
                relatedCustomer.setName(EPBFinalFields.GDPR);
                relatedCustomer.setIdentifier(EPBFinalFields.GDPR);

            }
        }
        List<ManagerResponse> managers = responses.getManagers();
        if (managers != null) {
            for (ManagerResponse manager : managers) {
                manager.setPersonalNumber(EPBFinalFields.GDPR);
                manager.setBirthDate(EPBFinalFields.GDPR);
            }
        }

        List<CustomerOwnerDetailResponse> owners = responses.getOwner();
        if (CollectionUtils.isNotEmpty(owners)) {
            for (CustomerOwnerDetailResponse owner : owners) {
                if (!owner.getOwnerType().equals(LEGAL_ENTITY)) {
                    owner.setName(EPBFinalFields.GDPR);
                    owner.setPersonalNumber(EPBFinalFields.GDPR);
                }
            }
        }

        if (responses.getCustomerType().equals(PRIVATE_CUSTOMER)) {
            responses.setIdentifier(EPBFinalFields.GDPR);
            responses.setName(EPBFinalFields.GDPR);
            responses.setNameTransl(EPBFinalFields.GDPR);
            responses.setMiddleName(EPBFinalFields.GDPR);
            responses.setMiddleNameTransl(EPBFinalFields.GDPR);
            responses.setLastName(EPBFinalFields.GDPR);
            responses.setLastNameTransl(EPBFinalFields.GDPR);
        }

        List<CustomerCommunicationsDetailedResponse> communicationData = responses.getCommunicationData();
        if (communicationData != null) {
            for (CustomerCommunicationsDetailedResponse comData : communicationData) {
                List<ContactPersonDetailedResponse> contactPersons = comData.getContactPersons();
                if (CollectionUtils.isNotEmpty(contactPersons)) {
                    for (ContactPersonDetailedResponse contact : contactPersons) {
                        contact.setBirthDate(EPBFinalFields.GDPR);
                        contact.setName(EPBFinalFields.GDPR);
                        contact.setMiddleName(EPBFinalFields.GDPR);
                        contact.setSurname(EPBFinalFields.GDPR);
                    }
                }
            }
        }
        return responses;
    }

    public CustomerAddress createCustomerAddressData(CustomerDetails details) {
        CustomerAddress customerAddress = new CustomerAddress();
        if (Boolean.TRUE.equals(details.getForeignAddress())) {
            ForeignAddressInfo foreignAddressData = new ForeignAddressInfo();
            foreignAddressData.setCountryId(details.getCountryId());
            foreignAddressData.setCountryName(countryService.view(details.getCountryId()).getName());
            foreignAddressData.setRegion(details.getRegionForeign());
            foreignAddressData.setMunicipality(details.getMunicipalityForeign());
            foreignAddressData.setPopulatedPlace(details.getPopulatedPlaceForeign());
            foreignAddressData.setZipCode(details.getZipCodeForeign());
            foreignAddressData.setDistrict(details.getDistrictForeign());
            foreignAddressData.setResidentialAreaType(details.getResidentialAreaTypeForeign());
            foreignAddressData.setResidentialArea(details.getResidentialAreaForeign());
            foreignAddressData.setStreetType(details.getStreetTypeForeign());
            foreignAddressData.setStreet(details.getStreetForeign());
            customerAddress.setForeignAddressData(foreignAddressData);
        } else {
            LocalAddressInfo localAddressInfo = new LocalAddressInfo();
            localAddressInfo.setCountryId(details.getCountryId());
            localAddressInfo.setCountryName(countryService.view(details.getCountryId()).getName());

            if (details.getResidentialAreaId() != null) {
                localAddressInfo.setResidentialAreaType(details.getResidentialAreaType());
                localAddressInfo.setResidentialAreaId(details.getResidentialAreaId());
                localAddressInfo.setResidentialAreaName(residentialAreaService.view(details.getResidentialAreaId()).getName());
            }

            localAddressInfo.setPopulatedPlaceId(details.getPopulatedPlaceId());

            localAddressInfo.setPopulatedPlaceName(populatedPlaceService.view(details.getPopulatedPlaceId()).getName());
            localAddressInfo.setZipCodeId(details.getZipCode().getId());
            localAddressInfo.setZipCodeName(details.getZipCode().getName());

            if (details.getDistrictId() != null) {
                localAddressInfo.setDistrictId(details.getDistrictId());
                localAddressInfo.setDistrictName(districtService.view(details.getDistrictId()).getName());
            }

            if (details.getStreetId() != null) {
                localAddressInfo.setStreetType(details.getStreetType());
                localAddressInfo.setStreetId(details.getStreetId());
                localAddressInfo.setStreetName(streetService.view(details.getStreetId()).getName());
            }

            if (details.getPopulatedPlaceId() != null) {
                Optional<PopulatedPlace> populatedPlace = populatedPlaceRepository.findByIdAndStatus(details.getPopulatedPlaceId(), List.of(NomenclatureItemStatus.ACTIVE, NomenclatureItemStatus.INACTIVE));
                if (populatedPlace.isPresent()) {
                    Municipality municipality = populatedPlace.get().getMunicipality();
                    localAddressInfo.setMunicipalityId(municipality.getId());
                    localAddressInfo.setMunicipalityName(municipality.getName());
                    localAddressInfo.setRegionId(municipality.getRegion().getId());
                    localAddressInfo.setRegionName(municipality.getRegion().getName());
                }
            }

            customerAddress.setLocalAddressData(localAddressInfo);
        }
        customerAddress.setForeign(details.getForeignAddress());
        customerAddress.setNumber(details.getStreetNumber());
        customerAddress.setAdditionalInformation(details.getAddressAdditionalInfo());
        customerAddress.setBlock(details.getBlock());
        customerAddress.setEntrance(details.getEntrance());
        customerAddress.setFloor(details.getFloor());
        customerAddress.setApartment(details.getApartment());
        customerAddress.setMailbox(details.getMailbox());

        return customerAddress;
    }

    /**
     * <h1>ModifyCustoemrShortResponse</h1>
     * function that takes {@link CustomerShortResponse} response object
     * adds "GDPR" String instead of name and personalNumber
     *
     * @param response {@link CustomerShortResponse} object
     */
    public void modifyCustomerShortResponse(CustomerShortResponse response) {
        if (checkGdpr() || response.getType().equals(LEGAL_ENTITY)) {
            return;
        }
        response.setName(EPBFinalFields.GDPR);
        response.setPersonalNumber(EPBFinalFields.GDPR);
    }

    /**
     * <h1>getViewObject</h1>
     * function takes all customer and sub object data and maps them into {@link CustomerViewResponse} object
     *
     * @param customer                        {@link Customer} object
     * @param customerDetails                 {@link CustomerDetails} object
     * @param managers                        list of {@link ManagerResponse} objects
     * @param relatedCustomers                list of {@link RelatedCustomerResponse} objects
     * @param ownerResponses                  list of {@link CustomerOwnerDetailResponse} objects
     * @param customerCommunications          list of {@link CustomerCommunicationsDetailedResponse} objects
     * @param connectedGroupResponses         list of {@link ConnectedGroupResponse} objects
     * @param customerAccountManagerResponses list of {@link CustomerAccountManagerResponse} objects
     * @return {@link CustomerViewResponse} object
     */
    public CustomerViewResponse getCustomerViewResponse(Customer customer,
                                                        CustomerDetails customerDetails,
                                                        List<ManagerResponse> managers,
                                                        List<RelatedCustomerResponse> relatedCustomers,
                                                        List<CustomerOwnerDetailResponse> ownerResponses,
                                                        List<CustomerCommunicationsDetailedResponse> customerCommunications,
                                                        List<ConnectedGroupResponse> connectedGroupResponses,
                                                        List<CustomerAccountManagerResponse> customerAccountManagerResponses,
                                                        List<SystemActivityShortResponse> activities,
                                                        List<TaskShortResponse> tasks) {
        MunicipalityResponse municipalityResponse = null;
        RegionResponse regionResponse = null;

        if (customerDetails.getPopulatedPlaceId() != null) {
            Optional<PopulatedPlace> populatedPlace =
                    populatedPlaceRepository
                            .findByIdAndStatus(
                                    customerDetails.getPopulatedPlaceId(),
                                    List.of(
                                            NomenclatureItemStatus.ACTIVE,
                                            NomenclatureItemStatus.INACTIVE
                                    )
                            );

            if (populatedPlace.isPresent()) {
                Municipality municipality = populatedPlace.get().getMunicipality();
                municipalityResponse = new MunicipalityResponse(municipality);
                regionResponse = new RegionResponse(municipality.getRegion());
            }
        }

        return CustomerViewResponse.
                builder()
                .customerId(customer.getId())
                .isUnwantedCustomer(getUnwantedCustomerStatus(customer.getIdentifier()))
                .customerNumber(customer.getCustomerNumber())
                .identifier(customer.getIdentifier())
                .customerType(customer.getCustomerType())
                .businessActivity(customerDetails.getBusinessActivity())
                .lastCustomerDetailId(customer.getLastCustomerDetailId())
                .customerSystemUserId(customer.getSystemUserId())
                .customerCreateDate(customer.getCreateDate())
                .customerStatus(customer.getStatus())
                .customerDetailsId(customerDetails.getId())
                .oldCustomerNumbers(customerDetails.getOldCustomerNumbers())
                .vatNumber(customerDetails.getVatNumber())
                .name(customerDetails.getName())
                .nameTransl(customerDetails.getNameTransl())
                .middleName(customerDetails.getMiddleName())
                .middleNameTransl(customerDetails.getMiddleNameTransl())
                .lastName(customerDetails.getLastName())
                .lastNameTransl(customerDetails.getLastNameTransl())
                .connectedGroups(connectedGroupResponses)
                .legalForm(getCustomerLegalForms(customerDetails.getLegalFormId()))
                .legalFormTranslId(getCustomerLegalFormsTransId(customerDetails.getLegalFormTranslId()))
                .ownershipFormId(getOwnershipForm(customerDetails.getOwnershipFormId()))
                .economicBranchCiId(getEconomicBranchCi(customerDetails.getEconomicBranchCiId()))
                .economicBranchNceaId(getEconomicBranchNcea(customerDetails.getEconomicBranchNceaId()))
                .mainActivitySubject(customerDetails.getMainActivitySubject())
                .customerDeclaredConsumption(customerDetails.getCustomerDeclaredConsumption())
                .creditRating(mapCreditRating(customerDetails.getCreditRating()))
                .bank(mapBank(customerDetails.getBank()))
                .iban(customerDetails.getIban())
                .zipCode(mapZipCode(customerDetails.getZipCode()))
                .streetNumber(customerDetails.getStreetNumber())
                .addressAdditionalInfo(customerDetails.getAddressAdditionalInfo())
                .block(customerDetails.getBlock())
                .entrance(customerDetails.getEntrance())
                .floor(customerDetails.getFloor())
                .apartment(customerDetails.getApartment())
                .mailbox(customerDetails.getMailbox())
                .streetId(getStreet(customerDetails.getStreetId()))
                .streetType(customerDetails.getStreetType())
                .residentialAreaType(customerDetails.getResidentialAreaType())
                .residentialAreaId(getResidentialArea(customerDetails.getResidentialAreaId()))
                .districtId(getDistrict(customerDetails.getDistrictId()))
                .regionForeign(customerDetails.getRegionForeign())
                .municipalityForeign(customerDetails.getMunicipalityForeign())
                .populatedPlaceForeign(customerDetails.getPopulatedPlaceForeign())
                .zipCodeForeign(customerDetails.getZipCodeForeign())
                .districtForeign(customerDetails.getDistrictForeign())
                .customerDetailsCustomerId(customerDetails.getCustomerId())
                .versionId(customerDetails.getVersionId())
                .publicProcurementLaw(customerDetails.getPublicProcurementLaw())
                .marketingCommConsent(customerDetails.getMarketingCommConsent())
                .foreignEntityPerson(customerDetails.getForeignEntityPerson())
                .directDebit(customerDetails.getDirectDebit())
                .foreignAddress(customerDetails.getForeignAddress())
                .populatedPlaceId(getPopulatedPlace(customerDetails.getPopulatedPlaceId()))
                .countryId(getCountry(customerDetails.getCountryId()))
                .preferCommunicationInEnglish(customerDetails.getPreferCommunicationInEnglish())
                .businessActivityName(customerDetails.getBusinessActivityName())
                .businessActivityNameTransl(customerDetails.getBusinessActivityNameTransl())
                .gdprRegulationConsent(customerDetails.getGdprRegulationConsent())
                .status(customerDetails.getStatus())
                .customerSegments(mapCustomerSegment(customerDetails.getId()))
                .customerPreferences(mapCustomerPreferences(customerDetails.getId()))
                .gdprMasking(!checkGdpr())
                .customerVersions(getCustomerVersions(customerDetails))
                .managers(managers)
                .relatedCustomers(relatedCustomers)
                .owner(ownerResponses)
                .communicationData(customerCommunications)
                .connectedGroups(connectedGroupResponses)
                .customerAccountManagers(customerAccountManagerResponses)
                .streetTypeForeign(customerDetails.getStreetTypeForeign())
                .streetForeign(customerDetails.getStreetForeign())
                .residentialAreaTypeForeign(customerDetails.getResidentialAreaTypeForeign())
                .residentialAreaForeign(customerDetails.getResidentialAreaForeign())
                .activities(activities)
                .tasks(tasks)
                .localMunicipality(municipalityResponse)
                .localRegion(regionResponse)
                .isPodDisconnected(getDisconnectedPodStatus(customer.getId()))
                .customerAdditionalInformation(customer.getAdditionalInfo())
                .build();
    }

    public ProductContractUpdateRequest mapProductContractUpdateRequest(ProductContractResponse response, LocalDate currentDate, Long newCustomerDetailsId) {
        ProductContractUpdateRequest productContractUpdateRequest = new ProductContractUpdateRequest();

        ProductContractBasicParametersUpdateRequest basicParametersUpdateRequest = new ProductContractBasicParametersUpdateRequest();
        BasicParametersResponse basicParameters = response.getBasicParameters();

        if (basicParameters != null) {
            basicParametersUpdateRequest.setTerminationDate(basicParameters.getTerminationDate());
            basicParametersUpdateRequest.setPerpetuityDate(basicParameters.getPerpetuityDate());
            basicParametersUpdateRequest.setStatus(basicParameters.getStatus());
            basicParametersUpdateRequest.setSubStatus(basicParameters.getSubStatus());
            basicParametersUpdateRequest.setStatusModifyDate(basicParameters.getStatusModifyDate());
            basicParametersUpdateRequest.setProductId(basicParameters.getProductId());
            basicParametersUpdateRequest.setProductVersionId(basicParameters.getProductVersionId());
            basicParametersUpdateRequest.setType(basicParameters.getType());
            basicParametersUpdateRequest.setHasUntilAmount(basicParameters.getHasUntilAmount() != null && basicParameters.getHasUntilAmount());
            basicParametersUpdateRequest.setHasUntilVolume(basicParameters.getHasUntilVolume() != null && basicParameters.getHasUntilVolume());
            basicParametersUpdateRequest.setProcurementLaw(basicParameters.getProcurementLaw());
            basicParametersUpdateRequest.setUntilAmount(basicParameters.getUntilAmount());
            basicParametersUpdateRequest.setUntilVolume(basicParameters.getUntilVolume());
            basicParametersUpdateRequest.setUntilAmountCurrencyId(basicParameters.getUntilAmountCurrency() != null ? basicParameters.getUntilAmountCurrency().getId() : null);
            basicParametersUpdateRequest.setSigningDate(basicParameters.getSigningDate());
            basicParametersUpdateRequest.setEntryInForceDate(basicParameters.getEntryInForceDate());
            basicParametersUpdateRequest.setStartOfInitialTerm(basicParameters.getStartOfInitialTerm());
            basicParametersUpdateRequest.setCustomerId(basicParameters.getCustomerId());
            basicParametersUpdateRequest.setCustomerVersionId(basicParameters.getCustomerVersionId());
            basicParametersUpdateRequest.setCustomerNewDetailsId(newCustomerDetailsId);
            basicParametersUpdateRequest.setCommunicationDataBillingId(basicParameters.getBillingCommunicationData() != null ? basicParameters.getBillingCommunicationData().getId() : null);
            basicParametersUpdateRequest.setCommunicationDataContractId(basicParameters.getContractCommunicationData() != null ? basicParameters.getContractCommunicationData().getId() : null);
            List<ContractFileResponse> files = basicParameters.getFiles();
            List<Long> fileIds = new ArrayList<>();
            for (ContractFileResponse file : files) {
                if (file.getFileType() != null && file.getFileType() == ContractFileType.UPLOADED_FILE) {
                    fileIds.add(file.getId());
                }
            }
            basicParametersUpdateRequest.setFiles(fileIds);
            basicParametersUpdateRequest.setDocuments(basicParameters.getDocuments() != null ? basicParameters.getDocuments().stream().map(FileWithStatusesResponse::getId).toList() : null);
            basicParametersUpdateRequest.setVersionStatus(basicParameters.getVersionStatus());
            basicParametersUpdateRequest.setVersionTypeIds(basicParameters.getVersionTypesResponse() != null ? basicParameters.getVersionTypesResponse().stream().map(ContractVersionTypesResponse::getId).collect(Collectors.toSet()) : null);

            List<ProxyEditRequest> proxyRequests = Optional.ofNullable(response.getBasicParameters().getProxy())
                    .orElse(new ArrayList<>())
                    .stream()
                    .map(proxyResponse -> {
                        ProxyEditRequest proxyRequest = new ProxyEditRequest();
                        proxyRequest.setId(proxyResponse.getId());
                        proxyRequest.setProxyForeignEntityPerson(proxyResponse.getProxyForeignEntityPerson());
                        proxyRequest.setProxyName(proxyResponse.getProxyName());
                        proxyRequest.setProxyCustomerIdentifier(proxyResponse.getProxyCustomerIdentifier());
                        proxyRequest.setProxyEmail(proxyResponse.getProxyEmail());
                        proxyRequest.setProxyPhone(proxyResponse.getProxyPhone());
                        proxyRequest.setProxyPowerOfAttorneyNumber(proxyResponse.getProxyPowerOfAttorneyNumber());
                        proxyRequest.setProxyData(proxyResponse.getProxyData());
                        proxyRequest.setProxyValidTill(proxyResponse.getProxyValidTill());
                        proxyRequest.setNotaryPublic(proxyResponse.getNotaryPublic());
                        proxyRequest.setAreaOfOperation(proxyResponse.getAreaOfOperation());
                        proxyRequest.setAuthorizedProxyForeignEntityPerson(proxyResponse.getAuthorizedProxyForeignEntityPerson());
                        proxyRequest.setProxyAuthorizedByProxy(proxyResponse.getProxyAuthorizedByProxy());
                        proxyRequest.setAuthorizedProxyCustomerIdentifier(proxyResponse.getAuthorizedProxyCustomerIdentifier());
                        proxyRequest.setAuthorizedProxyEmail(proxyResponse.getAuthorizedProxyEmail());
                        proxyRequest.setAuthorizedProxyPhone(proxyResponse.getAuthorizedProxyPhone());
                        proxyRequest.setAuthorizedProxyPowerOfAttorneyNumber(proxyResponse.getAuthorizedProxyPowerOfAttorneyNumber());
                        proxyRequest.setAuthorizedProxyData(proxyResponse.getAuthorizedProxyData());
                        proxyRequest.setAuthorizedProxyValidTill(proxyResponse.getAuthorizedProxyValidTill());
                        proxyRequest.setAuthorizedProxyNotaryPublic(proxyResponse.getAuthorizedProxyNotaryPublic());
                        proxyRequest.setAuthorizedProxyRegistrationNumber(proxyResponse.getAuthorizedProxyRegistrationNumber());
                        proxyRequest.setAuthorizedProxyAreaOfOperation(proxyResponse.getAuthorizedProxyAreaOfOperation());
                        return proxyRequest;
                    })
                    .toList();
            basicParametersUpdateRequest.setProxy(proxyRequests);

            List<RelatedEntityRequest> relatedEntityRequests = Optional.ofNullable(response.getBasicParameters().getRelatedEntities())
                    .orElse(new ArrayList<>())
                    .stream()
                    .map(relatedEntityResponse -> {
                        RelatedEntityRequest relatedEntityRequest = new RelatedEntityRequest();
                        relatedEntityRequest.setId(relatedEntityResponse.getId());
                        relatedEntityRequest.setEntityId(relatedEntityResponse.getEntityId());
                        relatedEntityRequest.setEntityType(relatedEntityResponse.getEntityType());
                        relatedEntityRequest.setRelatedEntityId(relatedEntityResponse.getRelatedEntityId());
                        relatedEntityRequest.setRelatedEntityType(relatedEntityResponse.getRelatedEntityType());
                        return relatedEntityRequest;
                    })
                    .toList();
            basicParametersUpdateRequest.setRelatedEntities(relatedEntityRequests);
        }


        ProductContractAdditionalParametersRequest productContractAdditionalParametersRequest = new ProductContractAdditionalParametersRequest();
        AdditionalParametersResponse additionalParameters = response.getAdditionalParameters();

        if (additionalParameters != null) {
            productContractAdditionalParametersRequest.setDealNumber(additionalParameters.getDealNumber());
            productContractAdditionalParametersRequest.setEstimatedTotalConsumptionUnderContractKwh(additionalParameters.getEstimatedTotalConsumptionUnderContractKwh());

            ProductContractBankingDetails bankingDetailsRequest = new ProductContractBankingDetails();
            ProductContractBankingDetailsResponse bankingDetails = additionalParameters.getBankingDetails();

            if (bankingDetails != null) {
                bankingDetailsRequest.setDirectDebit(bankingDetails.getDirectDebit());
                bankingDetailsRequest.setBankId(bankingDetails.getBankId());
                bankingDetailsRequest.setIban(bankingDetails.getIban());
            }

            productContractAdditionalParametersRequest.setBankingDetails(bankingDetailsRequest);
            productContractAdditionalParametersRequest.setRiskAssessment(additionalParameters.getRiskAssessment() != null ? RiskListDecision.valueOf(additionalParameters.getRiskAssessment().toUpperCase()) : null);
            productContractAdditionalParametersRequest.setRiskAssessmentAdditionalConditions(additionalParameters.getRiskAssessmentAdditionalConditions());
            productContractAdditionalParametersRequest.setInterestRateId(additionalParameters.getInterestRateId());
            productContractAdditionalParametersRequest.setCampaignId(additionalParameters.getCampaignId());
            productContractAdditionalParametersRequest.setEmployeeId(additionalParameters.getEmployeeId());
            productContractAdditionalParametersRequest.setInternalIntermediaries(additionalParameters.getInternalIntermediaries() != null ? additionalParameters.getInternalIntermediaries().stream().map(ProductContractSubObjectShortResponse::getId).toList() : null);
            productContractAdditionalParametersRequest.setExternalIntermediaries(additionalParameters.getExternalIntermediaries() != null ? additionalParameters.getExternalIntermediaries().stream().map(ProductContractSubObjectShortResponse::getId).toList() : null);
            productContractAdditionalParametersRequest.setAssistingEmployees(additionalParameters.getAssistingEmployees() != null ? additionalParameters.getAssistingEmployees().stream().map(ProductContractSubObjectShortResponse::getId).toList() : null);
        }


        ProductContractProductParametersCreateRequest productContractProductParametersCreateRequest = new ProductContractProductParametersCreateRequest();
        ThirdPagePreview productParameters = response.getProductParameters();

        if (productParameters != null) {
            productContractProductParametersCreateRequest.setContractType(productParameters.getContractType());
            productContractProductParametersCreateRequest.setProductContractTermId(productParameters.getContractTerm() != null ? productParameters.getContractTerm().getId() : null);
            productContractProductParametersCreateRequest.setContractTermEndDate(productParameters.getContractTermDate());
            productContractProductParametersCreateRequest.setPaymentGuarantee(productParameters.getPaymentGuarantee());
            productContractProductParametersCreateRequest.setCashDeposit(productParameters.getCashDeposit());
            productContractProductParametersCreateRequest.setCashDepositCurrencyId(productParameters.getCashDepositCurrency() != null ? productParameters.getCashDepositCurrency().getId() : null);
            productContractProductParametersCreateRequest.setBankGuarantee(productParameters.getBankGuarantee());
            productContractProductParametersCreateRequest.setBankGuaranteeCurrencyId(productParameters.getBankDepositCurrency() != null ? productParameters.getBankDepositCurrency().getId() : null);
            productContractProductParametersCreateRequest.setGuaranteeInformation(productParameters.getGuaranteeInformation());
            productContractProductParametersCreateRequest.setGuaranteeContract(productParameters.isGuaranteeContract());
            productContractProductParametersCreateRequest.setInvoicePaymentTermId(productParameters.getInvoicePaymentTerm() != null ? productParameters.getInvoicePaymentTerm().getId() : null);
            productContractProductParametersCreateRequest.setInvoicePaymentTermValue(productParameters.getInvoicePaymentTermValue());
            productContractProductParametersCreateRequest.setEntryIntoForce(productParameters.getEntryIntoForce());
            productContractProductParametersCreateRequest.setEntryIntoForceValue(productParameters.getEntryIntoForceValue());
            productContractProductParametersCreateRequest.setStartOfContractInitialTerm(productParameters.getStartOfContractInitialTerm());
            productContractProductParametersCreateRequest.setStartOfContractValue(productParameters.getStartOfContractValue());
            productContractProductParametersCreateRequest.setSupplyActivation(productParameters.getSupplyActivation());
            productContractProductParametersCreateRequest.setSupplyActivationValue(productParameters.getSupplyActivationValue());
            productContractProductParametersCreateRequest.setMonthlyInstallmentValue(productParameters.getMonthlyInstallmentValue());
            productContractProductParametersCreateRequest.setMonthlyInstallmentAmount(productParameters.getMonthlyInstallmentAmount());
            productContractProductParametersCreateRequest.setProductContractWaitForOldContractTermToExpires(productParameters.getProductContractWaitForOldContractTermToExpires());
            productContractProductParametersCreateRequest.setMarginalPrice(productParameters.getMarginalPrice());
            productContractProductParametersCreateRequest.setMarginalPriceValidity(productParameters.getMarginalPriceValidity());
            productContractProductParametersCreateRequest.setHourlyLoadProfile(productParameters.getHourlyLoadProfile());
            productContractProductParametersCreateRequest.setProcurementPrice(productParameters.getProcurementPrice());
            productContractProductParametersCreateRequest.setImbalancePriceIncrease(productParameters.getImbalancePriceIncrease());
            productContractProductParametersCreateRequest.setSetMargin(productParameters.getSetMargin());

            productContractProductParametersCreateRequest.setContractFormulas(response.getProductParameters().getPriceComponents().stream()
                    .map(contractPriceComponentResponse -> {
                        PriceComponentContractFormula priceComponentContractFormula = new PriceComponentContractFormula();
                        priceComponentContractFormula.setFormulaVariableId(contractPriceComponentResponse.getFormulaVariableId());
                        priceComponentContractFormula.setValue(contractPriceComponentResponse.getValue());
                        return priceComponentContractFormula;
                    })
                    .toList());

            Set<Long> encounteredInterimAdvancePaymentIds = new HashSet<>();

            List<ContractInterimAdvancePaymentsRequest> interimAdvancePaymentsRequests = response.getProductParameters().getInterimAdvancePayments().stream()
                    .filter(iapResponse -> {
                        if (iapResponse != null && iapResponse.getInterimAdvancePaymentId() != null) {
                            return encounteredInterimAdvancePaymentIds.add(iapResponse.getInterimAdvancePaymentId());
                        }

                        return true;
                    })
                    .map(iapResponse -> {
                        ContractInterimAdvancePaymentsRequest interimAdvancePaymentsRequest = new ContractInterimAdvancePaymentsRequest();

                        if (iapResponse != null) {
                            interimAdvancePaymentsRequest.setIssueDate(iapResponse.getIssueDate());
                            interimAdvancePaymentsRequest.setValue(iapResponse.getValue());
                            interimAdvancePaymentsRequest.setInterimAdvancePaymentId(iapResponse.getInterimAdvancePaymentId());
                            interimAdvancePaymentsRequest.setTermValue(iapResponse.getTermValue());

                            List<PriceComponentContractFormula> formulas = iapResponse.getFormulas() != null ? iapResponse.getFormulas().stream().map(formulaResponse -> {
                                PriceComponentContractFormula formula = new PriceComponentContractFormula();
                                formula.setFormulaVariableId(formulaResponse.getFormulaVariableId());
                                formula.setValue(formulaResponse.getValue());
                                return formula;
                            }).toList() : null;

                            interimAdvancePaymentsRequest.setContractFormulas(formulas);
                        }
                        return interimAdvancePaymentsRequest;
                    }).toList();

            productContractProductParametersCreateRequest.setInterimAdvancePayments(interimAdvancePaymentsRequests);
        }

        Map<Long, ContractPodRequest> podRequestMap = new HashMap<>();

        for (ContractPodsResponseImpl contractPod : Optional.ofNullable(response.getContractPodsResponses()).orElse(new ArrayList<>())) {
            Long billingGroupId = contractPod != null ? contractPod.getBillingGroupId() : null;
            Long podDetailIds = contractPod != null ? contractPod.getPodDetailId() : null;

            if (billingGroupId != null && podDetailIds != null) {
                if (podRequestMap.containsKey(billingGroupId)) {
                    ContractPodRequest existingPodRequest = podRequestMap.get(billingGroupId);
                    List<Long> newPodDetailIds = new ArrayList<>(
                            existingPodRequest
                                    .getProductContractPointOfDeliveries()
                                    .stream()
                                    .map(ProductContractPointOfDeliveryRequest::pointOfDeliveryDetailId)
                                    .toList()
                    );
                    newPodDetailIds.add(podDetailIds);
                    existingPodRequest
                            .setProductContractPointOfDeliveries(
                                    newPodDetailIds
                                            .stream()
                                            .map(id -> new ProductContractPointOfDeliveryRequest(id, ""))
                                            .toList());
                } else {
                    ContractPodRequest newPodRequest = new ContractPodRequest();
                    newPodRequest.setBillingGroupId(billingGroupId);
                    newPodRequest.setProductContractPointOfDeliveries(List.of(new ProductContractPointOfDeliveryRequest(podDetailIds, "")));
                    podRequestMap.put(billingGroupId, newPodRequest);
                }
            }
        }

        productContractUpdateRequest.setPodRequests(new ArrayList<>(podRequestMap.values()));
        productContractUpdateRequest.setProductParameters(productContractProductParametersCreateRequest);
        productContractUpdateRequest.setBasicParameters(basicParametersUpdateRequest);
        productContractUpdateRequest.setAdditionalParameters(productContractAdditionalParametersRequest);
        productContractUpdateRequest.setStartDate(currentDate);
        productContractUpdateRequest.setSavingAsNewVersion(true);

        return productContractUpdateRequest;
    }

    public ServiceContractEditRequest mapServiceContractEditRequest(ServiceContractResponse response, LocalDate currentDate, Long newCustomerDetailsId) {
        ServiceContractEditRequest serviceContractEditRequest = new ServiceContractEditRequest();

        ServiceContractBasicParametersEditRequest basicParameters = new ServiceContractBasicParametersEditRequest();
        ServiceContractBasicParametersResponse basicParametersResponse = response.getBasicParameters();

        if (basicParametersResponse != null) {
            basicParameters.setServiceId(basicParametersResponse.getServiceId());
            basicParameters.setServiceVersionId(basicParametersResponse.getServiceVersionId());
            basicParameters.setContractStatus(basicParametersResponse.getContractStatus());
            basicParameters.setContractStatusModifyDate(basicParametersResponse.getContractStatusModifyDate());
            basicParameters.setContractType(basicParametersResponse.getType());
            basicParameters.setDetailsSubStatus(basicParametersResponse.getSubStatus());
            basicParameters.setSignInDate(basicParametersResponse.getSignInDate());
            basicParameters.setEntryIntoForceDate(basicParametersResponse.getEntryIntoForceDate());
            basicParameters.setContractTermUntilAmountIsReached(basicParametersResponse.getContractTermUntilAmountIsReached());
            basicParameters.setContractTermUntilAmountIsReachedCheckbox(basicParametersResponse.getContractTermUntilAmountIsReachedCheckbox());
            basicParameters.setCustomerId(basicParametersResponse.getCustomerId());
            basicParameters.setCustomerVersionId(basicParametersResponse.getCustomerVersionId());
            basicParameters.setCustomerNewDetailsId(newCustomerDetailsId);
            basicParameters.setCommunicationDataForBilling(basicParametersResponse.getCommunicationDataForBilling());
            basicParameters.setCommunicationDataForContract(basicParametersResponse.getCommunicationDataForContract());
            basicParameters.setContractVersionStatus(basicParametersResponse.getContractVersionStatus());
            basicParameters.setContractVersionTypes(Optional.ofNullable(basicParametersResponse.getVersionTypes())
                    .orElse(new ArrayList<>())
                    .stream()
                    .map(ServiceContractContractVersionTypesResponse::getId)
                    .toList());
            basicParameters.setStartOfTheInitialTermOfTheContract(basicParametersResponse.getContractInitialTermStartDate());
            basicParameters.setTerminationDate(basicParametersResponse.getTerminationDate());
            basicParameters.setPerpetuityDate(basicParametersResponse.getPerpetuityDate());
            basicParameters.setContractTermEndDate(basicParametersResponse.getContractTermEndDate());
            basicParameters.setStartDate(basicParametersResponse.getCreationDate());

            List<ProxyEditRequest> proxyRequests = Optional.ofNullable(basicParametersResponse.getProxyResponse())
                    .orElse(new ArrayList<>())
                    .stream()
                    .map(proxyResponse -> {
                        ProxyEditRequest proxyRequest = new ProxyEditRequest();
                        proxyRequest.setId(proxyResponse.getId());
                        proxyRequest.setProxyForeignEntityPerson(proxyResponse.getProxyForeignEntityPerson());
                        proxyRequest.setProxyName(proxyResponse.getProxyName());
                        proxyRequest.setProxyCustomerIdentifier(proxyResponse.getProxyCustomerIdentifier());
                        proxyRequest.setProxyEmail(proxyResponse.getProxyEmail());
                        proxyRequest.setProxyPhone(proxyResponse.getProxyPhone());
                        proxyRequest.setProxyPowerOfAttorneyNumber(proxyResponse.getProxyPowerOfAttorneyNumber());
                        proxyRequest.setProxyData(proxyResponse.getProxyData());
                        proxyRequest.setProxyValidTill(proxyResponse.getProxyValidTill());
                        proxyRequest.setNotaryPublic(proxyResponse.getNotaryPublic());
                        proxyRequest.setAreaOfOperation(proxyResponse.getAreaOfOperation());
                        proxyRequest.setAuthorizedProxyForeignEntityPerson(proxyResponse.getAuthorizedProxyForeignEntityPerson());
                        proxyRequest.setProxyAuthorizedByProxy(proxyResponse.getProxyAuthorizedByProxy());
                        proxyRequest.setAuthorizedProxyCustomerIdentifier(proxyResponse.getAuthorizedProxyCustomerIdentifier());
                        proxyRequest.setAuthorizedProxyEmail(proxyResponse.getAuthorizedProxyEmail());
                        proxyRequest.setAuthorizedProxyPhone(proxyResponse.getAuthorizedProxyPhone());
                        proxyRequest.setAuthorizedProxyPowerOfAttorneyNumber(proxyResponse.getAuthorizedProxyPowerOfAttorneyNumber());
                        proxyRequest.setAuthorizedProxyData(proxyResponse.getAuthorizedProxyData());
                        proxyRequest.setAuthorizedProxyValidTill(proxyResponse.getAuthorizedProxyValidTill());
                        proxyRequest.setAuthorizedProxyNotaryPublic(proxyResponse.getAuthorizedProxyNotaryPublic());
                        proxyRequest.setAuthorizedProxyRegistrationNumber(proxyResponse.getAuthorizedProxyRegistrationNumber());
                        proxyRequest.setAuthorizedProxyAreaOfOperation(proxyResponse.getAuthorizedProxyAreaOfOperation());
                        return proxyRequest;
                    })
                    .toList();
            basicParameters.setProxy(proxyRequests);

            basicParameters.setFiles(Optional.ofNullable(basicParametersResponse.getContractFiles())
                    .orElse(new ArrayList<>())
                    .stream()
                    .map(FileWithStatusesResponse::getId)
                    .collect(Collectors.toCollection(LinkedHashSet::new)));

            basicParameters.setDocuments(Optional.ofNullable(basicParametersResponse.getAdditionalDocuments())
                    .orElse(new ArrayList<>())
                    .stream()
                    .map(FileWithStatusesResponse::getId)
                    .collect(Collectors.toCollection(LinkedHashSet::new)));

            List<RelatedEntityRequest> relatedEntityRequests = Optional.ofNullable(basicParametersResponse.getRelatedEntities())
                    .orElse(new ArrayList<>())
                    .stream()
                    .map(relatedEntityResponse -> {
                        RelatedEntityRequest relatedEntityRequest = new RelatedEntityRequest();
                        relatedEntityRequest.setId(relatedEntityResponse.getId());
                        relatedEntityRequest.setEntityId(relatedEntityResponse.getEntityId());
                        relatedEntityRequest.setEntityType(relatedEntityResponse.getEntityType());
                        relatedEntityRequest.setRelatedEntityId(relatedEntityResponse.getRelatedEntityId());
                        relatedEntityRequest.setRelatedEntityType(relatedEntityResponse.getRelatedEntityType());
                        return relatedEntityRequest;
                    })
                    .toList();
            basicParameters.setRelatedEntities(relatedEntityRequests);
        }

        ServiceContractAdditionalParametersRequest serviceContractAdditionalParametersRequest = new ServiceContractAdditionalParametersRequest();
        ServiceContractAdditionalParametersResponse additionalParameters = response.getAdditionalParameters();

        if (additionalParameters != null) {
            ServiceContractBankingDetails bankingDetails = null;
            ServiceContractBankingDetailsResponse bankingDetailsResponse = additionalParameters.getBankingDetails();
            if (bankingDetailsResponse != null) {
                bankingDetails = new ServiceContractBankingDetails();
                bankingDetails.setDirectDebit(bankingDetailsResponse.getDirectDebit());
                bankingDetails.setBankId(bankingDetailsResponse.getBankId());
                bankingDetails.setIban(bankingDetailsResponse.getIban());
            }
            serviceContractAdditionalParametersRequest.setBankingDetails(bankingDetails);

            serviceContractAdditionalParametersRequest.setInterestRateId(additionalParameters.getInterestRateId());
            serviceContractAdditionalParametersRequest.setCampaignId(additionalParameters.getCampaignId());

            List<Long> assistingEmployees = new ArrayList<>();
            List<ServiceContractSubObjectShortResponse> assistingEmployeesResponse = additionalParameters.getAssistingEmployees();
            if (assistingEmployeesResponse != null) {
                for (ServiceContractSubObjectShortResponse subObjectShortResponse : assistingEmployeesResponse) {
                    assistingEmployees.add(subObjectShortResponse.getId());
                }
            }
            serviceContractAdditionalParametersRequest.setAssistingEmployees(assistingEmployees);

            List<Long> internalIntermediaries = new ArrayList<>();
            List<ServiceContractSubObjectShortResponse> internalIntermediariesResponse = additionalParameters.getInternalIntermediaries();
            if (internalIntermediariesResponse != null) {
                for (ServiceContractSubObjectShortResponse subObjectShortResponse : internalIntermediariesResponse) {
                    internalIntermediaries.add(subObjectShortResponse.getId());
                }
            }
            serviceContractAdditionalParametersRequest.setInternalIntermediaries(internalIntermediaries);

            List<Long> externalIntermediaries = new ArrayList<>();
            List<ServiceContractSubObjectShortResponse> externalIntermediariesResponse = additionalParameters.getExternalIntermediaries();
            if (externalIntermediariesResponse != null) {
                for (ServiceContractSubObjectShortResponse subObjectShortResponse : externalIntermediariesResponse) {
                    externalIntermediaries.add(subObjectShortResponse.getId());
                }
            }
            serviceContractAdditionalParametersRequest.setExternalIntermediaries(externalIntermediaries);
            serviceContractAdditionalParametersRequest.setEmployeeId(additionalParameters.getEmployeeId());
        }


        // Map ServiceParametersPreview to ServiceContractServiceParametersCreateRequest
        ServiceContractServiceParametersEditRequest serviceContractServiceParametersEditRequest = new ServiceContractServiceParametersEditRequest();

        ServiceParametersPreview serviceParametersResponse = response.getServiceParameters();

        if (serviceParametersResponse != null) {
            serviceContractServiceParametersEditRequest.setPaymentGuarantee(serviceParametersResponse.getPaymentGuarantee());
            serviceContractServiceParametersEditRequest.setCashDepositAmount(serviceParametersResponse.getCashDeposit());
            serviceContractServiceParametersEditRequest.setCashDepositCurrencyId(serviceParametersResponse.getCashDepositCurrency() != null ? serviceParametersResponse.getCashDepositCurrency().getId() : null);
            serviceContractServiceParametersEditRequest.setBankGuaranteeAmount(serviceParametersResponse.getBankGuarantee());
            serviceContractServiceParametersEditRequest.setBankGuaranteeCurrencyId(serviceParametersResponse.getBankDepositCurrency() != null ? serviceParametersResponse.getBankDepositCurrency().getId() : null);
            serviceContractServiceParametersEditRequest.setGuaranteeContractInfo(serviceParametersResponse.getGuaranteeInformation());
            serviceContractServiceParametersEditRequest.setGuaranteeContract(serviceParametersResponse.isGuaranteeContract());
            serviceContractServiceParametersEditRequest.setEntryIntoForce(serviceParametersResponse.getEntryIntoForce());
            serviceContractServiceParametersEditRequest.setEntryIntoForceDate(serviceParametersResponse.getEntryIntoForceValue());
            serviceContractServiceParametersEditRequest.setStartOfContractInitialTerm(serviceParametersResponse.getStartOfContractInitialTerm());
            serviceContractServiceParametersEditRequest.setStartOfContractInitialTermDate(serviceParametersResponse.getStartOfContractInitialTermDate());
            serviceContractServiceParametersEditRequest.setInvoicePaymentTermId(serviceParametersResponse.getInvoicePaymentTerm() != null ? serviceParametersResponse.getInvoicePaymentTerm().getId() : null);
            serviceContractServiceParametersEditRequest.setInvoicePaymentTerm(serviceParametersResponse.getInvoicePaymentTermValue());
            serviceContractServiceParametersEditRequest.setContractTermEndDate(serviceParametersResponse.getContractTermDate());
            serviceContractServiceParametersEditRequest.setContractTermEndDate(serviceParametersResponse.getContractTermDate());
            serviceContractServiceParametersEditRequest.setQuantity(serviceParametersResponse.getQuantity() != null ? new BigDecimal(serviceParametersResponse.getQuantity()) : null);
            serviceContractServiceParametersEditRequest.setMonthlyInstallmentNumber(serviceParametersResponse.getMonthlyInstallmentValue());
            serviceContractServiceParametersEditRequest.setMonthlyInstallmentAmount(serviceParametersResponse.getMonthlyInstallmentAmount());
            serviceContractServiceParametersEditRequest.setContractTermId(serviceParametersResponse.getContractTerm().getId() != null ? serviceParametersResponse.getContractTerm().getId() : null);

            serviceContractServiceParametersEditRequest.setContractNumbers(
                    Optional.ofNullable(serviceParametersResponse.getContractResponseList())
                            .orElse(new ArrayList<>())
                            .stream()
                            .map(SubObjectContractResponse::getId)
                            .map(String::valueOf)
                            .collect(Collectors.toList())
            );

            serviceContractServiceParametersEditRequest.setPodIds(
                    Optional.ofNullable(serviceParametersResponse.getPodsList())
                            .orElse(new ArrayList<>())
                            .stream()
                            .map(SubObjectPodsResponse::getId)
                            .collect(Collectors.toList())
            );

            serviceContractServiceParametersEditRequest.setUnrecognizedPods(
                    Optional.ofNullable(serviceParametersResponse.getUnrecognizablePodsList())
                            .orElse(new ArrayList<>())
                            .stream()
                            .map(SubObjectPodsResponse::getPodIdentifier)
                            .collect(Collectors.toList())
            );

            serviceContractServiceParametersEditRequest.setContractFormulas(
                    Optional.ofNullable(serviceParametersResponse.getPriceComponents())
                            .orElse(new ArrayList<>())
                            .stream()
                            .map(contractPriceComponentResponse -> {
                                PriceComponentContractFormula priceComponentContractFormula = new PriceComponentContractFormula();
                                priceComponentContractFormula.setFormulaVariableId(contractPriceComponentResponse.getFormulaVariableId());
                                priceComponentContractFormula.setValue(contractPriceComponentResponse.getValue());
                                return priceComponentContractFormula;
                            })
                            .toList()
            );

            Set<Long> encounteredInterimAdvancePaymentIds = new HashSet<>();

            serviceContractServiceParametersEditRequest.setInterimAdvancePaymentsRequests(
                    Optional.ofNullable(serviceParametersResponse.getInterimAdvancePayments())
                            .orElse(new ArrayList<>())
                            .stream()
                            .filter(iapResponse -> {
                                if (iapResponse != null && iapResponse.getInterimAdvancePaymentId() != null) {
                                    return encounteredInterimAdvancePaymentIds.add(iapResponse.getInterimAdvancePaymentId());
                                }

                                return true;
                            })
                            .map(iapResponse -> {
                                ServiceContractInterimAdvancePaymentsRequest interimAdvancePaymentsRequest = new ServiceContractInterimAdvancePaymentsRequest();

                                if (iapResponse != null) {
                                    interimAdvancePaymentsRequest.setIssueDate(iapResponse.getIssueDate());
                                    interimAdvancePaymentsRequest.setValue(iapResponse.getValue());
                                    interimAdvancePaymentsRequest.setInterimAdvancePaymentId(iapResponse.getInterimAdvancePaymentId());
                                    interimAdvancePaymentsRequest.setTermValue(iapResponse.getTermValue());

                                    List<PriceComponentContractFormula> formulas = Optional.ofNullable(iapResponse.getFormulas())
                                            .orElse(new ArrayList<>())
                                            .stream()
                                            .map(formulaResponse -> {
                                                PriceComponentContractFormula formula = new PriceComponentContractFormula();
                                                formula.setFormulaVariableId(formulaResponse.getFormulaVariableId());
                                                formula.setValue(formulaResponse.getValues());
                                                return formula;
                                            })
                                            .toList();

                                    interimAdvancePaymentsRequest.setContractFormulas(formulas);
                                }
                                return interimAdvancePaymentsRequest;
                            })
                            .toList()
            );

            serviceContractServiceParametersEditRequest.setContractNumbersEditList(
                    Optional.ofNullable(serviceParametersResponse.getContractResponseList())
                            .orElse(new ArrayList<>())
                            .stream()
                            .map(subObjectContractResponse -> {
                                ServiceContractContractNumbersEditRequest serviceContractContractNumbersEditRequest = new ServiceContractContractNumbersEditRequest();
                                serviceContractContractNumbersEditRequest.setContractNumber(subObjectContractResponse.getContractNumber());
                                return serviceContractContractNumbersEditRequest;
                            })
                            .toList()
            );

            serviceContractServiceParametersEditRequest.setPodsEditList(
                    Optional.ofNullable(serviceParametersResponse.getPodsList())
                            .orElse(new ArrayList<>())
                            .stream()
                            .map(subObjectPodsResponse -> {
                                ServiceContractPodsEditRequest serviceContractPodsEditRequest = new ServiceContractPodsEditRequest();
                                serviceContractPodsEditRequest.setId(subObjectPodsResponse.getId());
                                serviceContractPodsEditRequest.setPodId(subObjectPodsResponse.getPodId());
                                return serviceContractPodsEditRequest;
                            })
                            .toList()
            );

            serviceContractServiceParametersEditRequest.setUnrecognizedPodsEditList(
                    Optional.ofNullable(serviceParametersResponse.getUnrecognizablePodsList())
                            .orElse(new ArrayList<>())
                            .stream()
                            .map(subObjectPodsResponse -> {
                                ServiceContractUnrecognizedPodsEditRequest serviceContractUnrecognizedPodsEditRequest = new ServiceContractUnrecognizedPodsEditRequest();
                                serviceContractUnrecognizedPodsEditRequest.setId(subObjectPodsResponse.getId());
                                serviceContractUnrecognizedPodsEditRequest.setPodName(subObjectPodsResponse.getPodName());
                                return serviceContractUnrecognizedPodsEditRequest;
                            })
                            .toList()
            );

        }


        serviceContractEditRequest.setServiceParameters(serviceContractServiceParametersEditRequest);
        serviceContractEditRequest.setBasicParameters(basicParameters);
        serviceContractEditRequest.setAdditionalParameters(serviceContractAdditionalParametersRequest);
        serviceContractEditRequest.getBasicParameters().setStartDate(currentDate);
        serviceContractEditRequest.setSavingAsNewVersion(true);

        return serviceContractEditRequest;
    }

    public CustomerResponse createCustomerResponse(Customer customer, CustomerDetails details) {
        CustomerResponse customerResponse = new CustomerResponse();
        customerResponse.setCustomerType(customer.getCustomerType());
        customerResponse.setCustomerNumber(customer.getCustomerNumber());
        customerResponse.setLastCustomerDetailId(customer.getLastCustomerDetailId());
        customerResponse.setIdentifier(customer.getIdentifier());
        customerResponse.setId(customer.getId());
        customerResponse.setIsDeleted(CustomerStatus.ACTIVE);
        customerResponse.setVersion(details.getVersionId());
        //TODO: assign customer owners
        return customerResponse;
    }

    public List<CustomerSegmentResponse> mapCustomerSegment(Long customerDetailId) {
        List<CustomerSegment> customerSegments = customerSegmentService.findCustomerSegmentsForCustomer(customerDetailId);

        if (CollectionUtils.isEmpty(customerSegments)) {
            return null;
        }

        return customerSegments
                .stream()
                .map(cs -> new CustomerSegmentResponse(cs.getId(), new SegmentResponse(cs.getSegment()), cs.getStatus()))
                .toList();
    }

    public List<CustomerPreferenceResponse> mapCustomerPreferences(Long customerDetailId) {
        List<CustomerPreference> customerPreferences = customerPreferenceService.findCustomerPreferencesForCustomer(customerDetailId);

        if (CollectionUtils.isEmpty(customerPreferences)) {
            return null;
        }

        return customerPreferences
                .stream()
                .map(cp -> new CustomerPreferenceResponse(cp.getId(), new PreferencesResponse(cp.getPreferences()), cp.getStatus()))
                .toList();
    }

    public ZipCodeResponse mapZipCode(ZipCode zipCode) {
        if (zipCode == null) {
            return null;
        }
        return new ZipCodeResponse(zipCode);
    }

    public BankResponse mapBank(Bank bank) {
        if (bank == null) {
            return null;
        }
        return new BankResponse(bank);
    }

    public CreditRatingResponse mapCreditRating(CreditRating creditRating) {
        if (creditRating == null) {
            return null;
        }
        CreditRatingResponse creditRatingResponse = new CreditRatingResponse();
        creditRatingResponse.setId(creditRating.getId());
        creditRatingResponse.setName(creditRating.getName());
        creditRatingResponse.setStatus(creditRating.getStatus());
        creditRatingResponse.setDefaultSelection(creditRating.getIsDefault());
        creditRatingResponse.setOrderingId(creditRating.getOrderingId());
        return creditRatingResponse;
    }

    /**
     * <h1>GetCustomerLegalFormsTransId</h1>
     * function returns legalFormTranslierated nomenclature item base on legalFormTranslId
     *
     * @param legalFormTranslId ID of legalFormTransliterated
     * @return {@link LegalFormTranResponse}
     */
    public LegalFormTranResponse getCustomerLegalFormsTransId(Long legalFormTranslId) {
        if (legalFormTranslId == null) return null;
        return legalFormService.getLegalFormTransliterated(legalFormTranslId);
    }

    /**
     * <h1>GetDistrict</h1>
     * function returns District nomenclature item base on districtId
     *
     * @param districtId ID of district
     * @return {@link DistrictResponse}
     */
    public DistrictResponse getDistrict(Long districtId) {
        if (districtId == null) return null;
        return districtService.view(districtId);
    }

    /**
     * <h1>GetResidentialArea</h1>
     * function returns ResidentialArea nomenclature item base on residentialAreaId
     *
     * @param residentialAreaId ID of residentialArea
     * @return {@link ResidentialAreaResponse}
     */
    public ResidentialAreaResponse getResidentialArea(Long residentialAreaId) {
        if (residentialAreaId == null) return null;
        return residentialAreaService.view(residentialAreaId);
    }

    /**
     * <h1>GetStreet</h1>
     * function returns Street nomenclature item base on streetId
     *
     * @param streetId ID of street
     * @return {@link StreetsResponse}
     */
    public StreetsResponse getStreet(Long streetId) {
        if (streetId == null) return null;
        return streetService.view(streetId);
    }

    /**
     * <h1>getCountry</h1>
     * function returns Country nomenclature item base on countryId
     *
     * @param countryId ID of country
     * @return {@link StreetsResponse}
     */
    public CountryResponse getCountry(Long countryId) {
        if (countryId == null) return null;
        return countryService.view(countryId);
    }

    /**
     * <h1>GetPopulatedPlace</h1>
     * function returns PopulatedPlace nomenclature item base on populatedPlaceId
     *
     * @param populatedPlaceId ID of populatedPlace
     * @return {@link PopulatedPlaceDetailedResponse}
     */
    public PopulatedPlaceDetailedResponse getPopulatedPlace(Long populatedPlaceId) {
        if (populatedPlaceId == null) return null;
        return populatedPlaceService.detailedView(populatedPlaceId);
    }

    /**
     * <h1>GetEconomicBranchNcea</h1>
     * function returns EconomicBranchNCEA nomenclature item base on economicBranchNceaId
     *
     * @param economicBranchNceaId ID of economicBranchNcea
     * @return {@link EconomicBranchNCEAResponse}
     */
    public EconomicBranchNCEAResponse getEconomicBranchNcea(Long economicBranchNceaId) {
        if (economicBranchNceaId == null) return null;
        return economicBranchNCEAService.view(economicBranchNceaId);
    }

    /**
     * <h1>GetEconomicBranchCi</h1>
     * function returns EconomicBranchCi nomenclature item base on economicBranchCiId
     *
     * @param economicBranchCiId ID of economicBranchCi
     * @return {@link EconomicBranchCIResponse}
     */
    public EconomicBranchCIResponse getEconomicBranchCi(Long economicBranchCiId) {
        if (economicBranchCiId == null) return null;
        return economicBranchCIService.view(economicBranchCiId);
    }

    /**
     * <h1>GetOwnershipForm</h1>
     * function returns OwnershipForm nomenclature item base on ownershipFormId
     *
     * @param ownershipFormId ID of ownershipForm
     * @return {@link OwnershipFormResponse}
     */
    public OwnershipFormResponse getOwnershipForm(Long ownershipFormId) {
        if (ownershipFormId == null) return null;
        return ownershipFormService.view(ownershipFormId);
    }

    /**
     * <h1>GetCustomerLegalForms</h1>
     * function returns LegalForm nomenclature item base on legalFormId
     *
     * @param legalFormId ID of legalForm
     * @return {@link LegalFormResponse}
     */
    public LegalFormResponse getCustomerLegalForms(Long legalFormId) {
        if (legalFormId == null) return null;
        return legalFormService.view(legalFormId);
    }

    /**
     * <h1>GetUnwantedCustomerStatus</h1>
     * function returns unwantedCustomerObject based on identifier
     *
     * @param identifier identifier of customer
     * @return boolean value
     */
    public Boolean getUnwantedCustomerStatus(String identifier) {
        return unwantedCustomerService.checkUnwantedCustomer(identifier) != null;
    }


    public Boolean getDisconnectedPodStatus(Long customerId) {
        return customerRepository.checkCustomerPodDisconnectionStatus(customerId);
    }

    /**
     * function checks if system user has gdpr permission
     *
     * @return boolean value
     */
    public boolean checkGdpr() {
        List<String> context = permissionService.getPermissionsFromContext(CUSTOMER);

        return context
                .stream()
                .anyMatch(x -> List.of(CUSTOMER_VIEW_BASIC.getId(), CUSTOMER_VIEW_BASIC_AM.getId()).contains(x));
    }

    /**
     * <h1>GetCustomerVersions</h1>
     * function returns customer versions array
     *
     * @param customerDetails {@link CustomerDetails} object
     * @return list of {@link CustomerVersionsResponse} objects
     */
    public List<CustomerVersionsResponse> getCustomerVersions(CustomerDetails customerDetails) {
        List<CustomerDetailStatus> customerDetailStatuses = new ArrayList<>();
        customerDetailStatuses.add(CustomerDetailStatus.POTENTIAL);
        customerDetailStatuses.add(CustomerDetailStatus.NEW);
        customerDetailStatuses.add(CustomerDetailStatus.ACTIVE);
        customerDetailStatuses.add(CustomerDetailStatus.LOST);
        customerDetailStatuses.add(CustomerDetailStatus.ENDED);
        return customerDetailsService.getVersions(customerDetails.getCustomerId(), customerDetailStatuses);
    }
}
