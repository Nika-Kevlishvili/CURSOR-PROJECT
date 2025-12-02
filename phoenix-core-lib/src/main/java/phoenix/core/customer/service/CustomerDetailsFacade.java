package phoenix.core.customer.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;
import phoenix.core.customer.model.entity.customer.Customer;
import phoenix.core.customer.model.entity.customer.CustomerDetails;
import phoenix.core.customer.model.entity.nomenclature.address.*;
import phoenix.core.customer.model.entity.nomenclature.customer.*;
import phoenix.core.customer.model.entity.nomenclature.customer.legalForm.LegalForm;
import phoenix.core.customer.model.entity.nomenclature.customer.legalForm.LegalFormTransliterated;
import phoenix.core.customer.model.enums.customer.CustomerDetailStatus;
import phoenix.core.customer.model.enums.customer.CustomerType;
import phoenix.core.customer.model.enums.customer.GDPRCustomerFields;
import phoenix.core.customer.model.enums.nomenclature.NomenclatureItemStatus;
import phoenix.core.customer.model.request.*;
import phoenix.core.customer.repository.customer.CustomerDetailsRepository;
import phoenix.core.customer.repository.nomenclature.address.*;
import phoenix.core.customer.repository.nomenclature.customer.*;
import phoenix.core.customer.repository.nomenclature.customer.legalForm.LegalFormRepository;
import phoenix.core.customer.repository.nomenclature.customer.legalForm.LegalFormTransliteratedRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;


@Service("coreCustomerDetailsService")
@RequiredArgsConstructor
@Validated
public class CustomerDetailsFacade {
    private final LegalFormRepository legalFormRepository;
    private final LegalFormTransliteratedRepository legalFormTransliteratedRepository;
    private final EconomicBranchCIRepository economicBranchCIRepository;
    private final EconomicBranchNCEARepository economicBranchNCEARepository;
    private final OwnershipFormRepository ownershipFormRepository;
    private final StreetRepository streetRepository;
    private final ResidentialAreaRepository residentialAreaRepository;
    private final DistrictRepository districtRepository;
    private final CountryRepository countryRepository;
    private final PopulatedPlaceRepository populatedPlaceRepository;
    private final CreditRatingRepository creditRatingRepository;
    private final BankRepository bankRepository;
    private final ZipCodeRepository zipCodeRepository;
    private final CustomerDetailsRepository customerDetailsRepository;


    public CustomerDetails createCustomerdetails(CreateCustomerRequest request,
                                                 Customer customer,
                                                 List<NomenclatureItemStatus> statuses,
                                                 List<String> exceptionMessages,
                                                 Boolean editMode) {
        CustomerDetails customerDetails = new CustomerDetails();
        customerDetails.setOldCustomerNumbers(request.getOldCustomerNumber());
        customerDetails.setVatNumber(request.getVatNumber());
        customerDetails.setVersionId(1L);
        customerDetails.setMainActivitySubject(request.getMainSubjectOfActivity());
        fillCustomerFieldsByTypes(request, customerDetails, statuses, exceptionMessages);
        fillAddressData(request, customerDetails, statuses, exceptionMessages);
        if (customer != null) customerDetails.setCustomerId(customer.getId());
        customerDetails.setVersionId(1L);
        customerDetails.setMarketingCommConsent(request.getMarketingConsent());
        customerDetails.setForeignEntityPerson(request.getForeign());
        fillBankingDetailsFields(request.getBankingDetails(), customerDetails, statuses, exceptionMessages);
        customerDetails.setForeignAddress(request.getAddress().getForeign());
        //TODO: assign correct systemUserId
        customerDetails.setSystemUserId("bla");
        customerDetails.setCreateDate(LocalDateTime.now());
        customerDetails.setStatus(request.getCustomerDetailStatus());
        if (editMode) {
            return customerDetails;
        }
        if (exceptionMessages.isEmpty()) return customerDetailsRepository.save(customerDetails);
        return null;
    }

    private void fillCustomerFieldsByTypes(CreateCustomerRequest request,
                                           CustomerDetails customerDetails,
                                           List<NomenclatureItemStatus> statuses,
                                           List<String> exceptionMessages) {
        if (request.getCustomerType() == CustomerType.LEGAL_ENTITY) {
            fillBusinessRelatedFields(request, customerDetails, statuses, exceptionMessages);
            customerDetails.setName(request.getBusinessCustomerDetails().getName());
            customerDetails.setNameTransl(request.getBusinessCustomerDetails().getNameTranslated());
        } else if (request.getCustomerType() == CustomerType.PRIVATE_CUSTOMER_WITH_BUSINESS_ACTIVITY) {
            fillBusinessRelatedFields(request, customerDetails, statuses, exceptionMessages);
            fillPrivateCustomerFields(request, customerDetails);
        } else {
            fillPrivateCustomerFields(request, customerDetails);
        }
    }

    private void fillAddressData(CreateCustomerRequest request,
                                 CustomerDetails customerDetails,
                                 List<NomenclatureItemStatus> statuses,
                                 List<String> exceptionMessages) {
        CustomerAddressRequest customerAddressRequest = request.getAddress();
        if (customerAddressRequest.getForeign()) {
            fillForeignAddressFields(request, customerDetails, statuses, exceptionMessages);
        } else {
            fillLocalAddressFields(request, customerDetails, statuses, exceptionMessages);
        }
        customerDetails.setStreetNumber(customerAddressRequest.getNumber());
        customerDetails.setAddressAdditionalInfo(customerAddressRequest.getAdditionalInformation());
        customerDetails.setBlock(customerAddressRequest.getBlock());
        customerDetails.setEntrance(customerAddressRequest.getEntrance());
        customerDetails.setFloor(customerAddressRequest.getFloor());
        customerDetails.setApartment(customerAddressRequest.getApartment());
        customerDetails.setMailbox(customerAddressRequest.getMailbox());

    }

    private void fillBankingDetailsFields(CustomerBankingDetails customerBankingDetails,
                                          CustomerDetails customerDetails,
                                          List<NomenclatureItemStatus> statuses,
                                          List<String> exceptionMessages) {
        if (customerBankingDetails != null) {
            customerDetails.setCreditRating(
                    getCreditRating(customerBankingDetails.getCreditRatingId(), statuses, exceptionMessages)
            );
            customerDetails.setBank(
                    getBank(customerBankingDetails, statuses, exceptionMessages)
            );
            customerDetails.setDirectDebit(customerBankingDetails.getDirectDebit());
            customerDetails.setCustomerDeclaredConsumption(customerBankingDetails.getDeclaredConsumption());
            customerDetails.setIban(customerBankingDetails.getIban());
        }
    }

    private CreditRating getCreditRating(Long creditRatingId,
                                         List<NomenclatureItemStatus> statuses,
                                         List<String> exceptionMessages) {
        Optional<CreditRating> optionalCreditRating = creditRatingRepository.findByIdAndStatus(creditRatingId, statuses);
        if (optionalCreditRating.isEmpty()) {
            if (creditRatingId != null) {
                exceptionMessages.add("Credit Rating not found; ");
            }
            return null;
        } else {
            return optionalCreditRating.get();
        }
    }

    private Bank getBank(CustomerBankingDetails customerBankingDetails,
                         List<NomenclatureItemStatus> statuses,
                         List<String> exceptionMessages) {
        Optional<Bank> optionalBank = bankRepository
                .findByIdAndStatus(customerBankingDetails.getBankId(), statuses);
        if (optionalBank.isEmpty()) {
            if (customerBankingDetails.getDirectDebit()) {
                exceptionMessages.add("Bank not found; ");
            }
            return null;
        } else {
            return optionalBank.get();
        }
    }

    private void fillBusinessRelatedFields(CreateCustomerRequest request,
                                           CustomerDetails customerDetails,
                                           List<NomenclatureItemStatus> statuses,
                                           List<String> exceptionMessages) {
        customerDetails.setPublicProcurementLaw(request.getBusinessCustomerDetails().getProcurementLaw());
        customerDetails.setLegalFormId(
                getLegalFormId(request.getBusinessCustomerDetails(), statuses, exceptionMessages)
        );
        customerDetails.setLegalFormTranslId(
                getLegalFormTranslId(request.getBusinessCustomerDetails(), statuses, exceptionMessages)
        );
        customerDetails.setOwnershipFormId(
                getOwnershipFormId(request, statuses, exceptionMessages)
        );
        customerDetails.setEconomicBranchCiId(
                getEconomicBranchCIId(request, statuses, exceptionMessages)
        );
        customerDetails.setEconomicBranchNceaId(
                getEconomicBranchNCEAId(request, statuses, exceptionMessages)
        );

    }

    private Long getLegalFormId(BusinessCustomerDetails businessCustomerDetails,
                                List<NomenclatureItemStatus> statuses,
                                List<String> exceptionMessages) {
        Optional<LegalForm> optionalLegalForm = legalFormRepository
                .findByIdAndStatus(businessCustomerDetails.getLegalFormId(), statuses);
        if (optionalLegalForm.isEmpty()) {
            exceptionMessages.add("Legal form not found; ");
            return null;
        } else {
            return optionalLegalForm.get().getId();
        }
    }

    private Long getLegalFormTranslId(BusinessCustomerDetails businessCustomerDetails,
                                      List<NomenclatureItemStatus> statuses,
                                      List<String> exceptionMessages) {
        Optional<LegalFormTransliterated> optionalLegalFormTransliterated = legalFormTransliteratedRepository
                .findByIdAndStatus(businessCustomerDetails.getLegalFormTransId(), statuses);
        if (optionalLegalFormTransliterated.isEmpty()) {
            exceptionMessages.add("Legal form Transl. not found; ");
            return null;
        } else {
            return optionalLegalFormTransliterated.get().getId();
        }
    }

    private Long getOwnershipFormId(CreateCustomerRequest request,
                                    List<NomenclatureItemStatus> statuses,
                                    List<String> exceptionMessages) {
        Optional<OwnershipForm> optionalOwnershipForm = ownershipFormRepository
                .findByIdAndStatus(request.getOwnershipFormId(), statuses);
        if (optionalOwnershipForm.isEmpty()) {
            checkCustomerTypeAndAddExceptionMessage(request,
                    request.getOwnershipFormId(),
                    "Ownership form not found; ",
                    exceptionMessages);
            return null;
        } else {
            return optionalOwnershipForm.get().getId();
        }
    }

    private Long getEconomicBranchCIId(CreateCustomerRequest request,
                                       List<NomenclatureItemStatus> statuses,
                                       List<String> exceptionMessages) {
        Optional<EconomicBranchCI> optionalEconomicBranchCI = economicBranchCIRepository
                .findByIdAndStatus(request.getEconomicBranchId(), statuses);
        if (optionalEconomicBranchCI.isEmpty()) {
            checkCustomerTypeAndAddExceptionMessage(request,
                    request.getEconomicBranchId(),
                    "Economic Branch CI not found; ",
                    exceptionMessages);
            return null;
        } else {
            return optionalEconomicBranchCI.get().getId();
        }
    }

    private Long getEconomicBranchNCEAId(CreateCustomerRequest request,
                                         List<NomenclatureItemStatus> statuses,
                                         List<String> exceptionMessages) {
        Optional<EconomicBranchNCEA> optionalEconomicBranchNCEA = economicBranchNCEARepository
                .findByIdAndStatus(request.getEconomicBranchNCEAId(), statuses);
        if (optionalEconomicBranchNCEA.isEmpty()) {
            if (request.getEconomicBranchNCEAId() != null) {
                exceptionMessages.add("Economic Branch NCEA not found; ");
            }
            return null;
        } else {
            return optionalEconomicBranchNCEA.get().getId();
        }
    }

    private void fillPrivateCustomerFields(CreateCustomerRequest request,
                                           CustomerDetails customerDetails) {
        PrivateCustomerDetails privateCustomerDetails = request.getPrivateCustomerDetails();
        customerDetails.setName(request.getPrivateCustomerDetails().getFirstName());
        customerDetails.setNameTransl(request.getPrivateCustomerDetails().getFirstNameTranslated());
        customerDetails.setMiddleName(privateCustomerDetails.getMiddleName());
        customerDetails.setMiddleNameTransl(privateCustomerDetails.getMiddleNameTranslated());
        customerDetails.setLastName(privateCustomerDetails.getLastName());
        customerDetails.setLastNameTransl(privateCustomerDetails.getLastNameTranslated());
        customerDetails.setBusinessActivityName(privateCustomerDetails.getBusinessActivityName());
        customerDetails.setBusinessActivityNameTransl(privateCustomerDetails.getBusinessActivityNameTranslated());
        customerDetails.setGdprRegulationConsent(privateCustomerDetails.getGdprRegulationConsent());
    }

    private void fillLocalAddressFields(CreateCustomerRequest request,
                                        CustomerDetails customerDetails,
                                        List<NomenclatureItemStatus> statuses,
                                        List<String> exceptionMessages) {
        LocalAddressData localAddressData = request.getAddress().getLocalAddressData();
        Long countryId = getCountryId(request, localAddressData.getCountryId(), statuses, exceptionMessages);
        if (countryId != null) {
            customerDetails.setCountryId(countryId);
            fillPopulatedPlaceRelatedFields(request, customerDetails, countryId, statuses, exceptionMessages);
        }
    }

    private void fillPopulatedPlaceRelatedFields(CreateCustomerRequest request,
                                                 CustomerDetails customerDetails,
                                                 Long countryId,
                                                 List<NomenclatureItemStatus> statuses,
                                                 List<String> exceptionMessages) {
        Long populatedPlaceId = getPopulatedPlaceId(request, countryId, statuses, exceptionMessages);
        if (populatedPlaceId != null) {
            customerDetails.setPopulatedPlaceId(populatedPlaceId);
            customerDetails.setStreetId(
                    getStreetId(request.getAddress().getLocalAddressData(), populatedPlaceId, statuses, exceptionMessages)
            );
            customerDetails.setResidentialAreaId(
                    getResidentialAreaId(request.getAddress().getLocalAddressData(), populatedPlaceId, statuses, exceptionMessages)
            );
            customerDetails.setDistrictId(
                    getDistrictId(request, populatedPlaceId, statuses, exceptionMessages)
            );
            customerDetails.setZipCode(
                    getZipCode(request, populatedPlaceId, statuses, exceptionMessages)
            );
        }
    }

    private Long getPopulatedPlaceId(CreateCustomerRequest request,
                                     Long countryId,
                                     List<NomenclatureItemStatus> statuses,
                                     List<String> exceptionMessages) {
        LocalAddressData localAddressData = request.getAddress().getLocalAddressData();
        Optional<PopulatedPlace> optionalPopulatedPlace = populatedPlaceRepository
                .findByIdAndStatus(localAddressData.getPopulatedPlaceId(), statuses);
        if (optionalPopulatedPlace.isEmpty()) {
            checkCustomerTypeAndAddExceptionMessage(
                    request,
                    localAddressData.getPopulatedPlaceId(),
                    "Populated place not found; ",
                    exceptionMessages
            );
            return null;
        } else {
            Country populatedPlaceCountry = optionalPopulatedPlace.get().getMunicipality().getRegion().getCountry();
            if (!countryId.equals(populatedPlaceCountry.getId())) {
                exceptionMessages.add("Populated place is not in entered country; ");
                return null;
            }
            return optionalPopulatedPlace.get().getId();
        }
    }

    private Long getStreetId(LocalAddressData localAddressData,
                             Long populatedPlaceId,
                             List<NomenclatureItemStatus> statuses,
                             List<String> exceptionMessages) {
        Optional<Street> optionalStreet = streetRepository.findByIdAndPopulatedPlaceIdAndStatus(
                localAddressData.getStreetId(),
                populatedPlaceId,
                statuses);
        if (optionalStreet.isEmpty()) {
            if (localAddressData.getStreetId() != null) {
                exceptionMessages.add("Street not found in entered populated place; ");
            }
            return null;
        } else {
            return optionalStreet.get().getId();
        }
    }

    private Long getResidentialAreaId(LocalAddressData localAddressData,
                                      Long populatedPlaceId,
                                      List<NomenclatureItemStatus> statuses,
                                      List<String> exceptionMessages) {
        Optional<ResidentialArea> optionalResidentialArea = residentialAreaRepository
                .findByIdAndPopulatedPlaceIdAndStatus(
                        localAddressData.getResidentialAreaId(),
                        populatedPlaceId,
                        statuses
                );
        if (optionalResidentialArea.isEmpty()) {
            if (localAddressData.getResidentialAreaId() != null) {
                exceptionMessages.add("Residential Area not found in entered populated place; ");
            }
            return null;
        } else {
            return optionalResidentialArea.get().getId();
        }
    }

    private Long getDistrictId(CreateCustomerRequest request,
                               Long populatedPlaceId,
                               List<NomenclatureItemStatus> statuses,
                               List<String> exceptionMessages) {
        LocalAddressData localAddressData = request.getAddress().getLocalAddressData();
        Optional<District> optionalDistrict = districtRepository
                .findByIdAndPopulatedPlaceIdAndStatus(
                        localAddressData.getDistrictId(),
                        populatedPlaceId,
                        statuses
                );
        if (optionalDistrict.isEmpty()) {
            checkCustomerTypeAndAddExceptionMessage(
                    request,
                    localAddressData.getDistrictId(),
                    "District not found in entered populated place; ",
                    exceptionMessages
            );
            return null;
        } else {
            return optionalDistrict.get().getId();
        }
    }

    private ZipCode getZipCode(CreateCustomerRequest request,
                               Long populatedPlaceId,
                               List<NomenclatureItemStatus> statuses,
                               List<String> exceptionMessages) {
        LocalAddressData localAddressData = request.getAddress().getLocalAddressData();
        Optional<ZipCode> optionalZipCode = zipCodeRepository
                .findByIdAndPopulatedPlaceIdAndStatus(
                        localAddressData.getZipCodeId(),
                        populatedPlaceId,
                        statuses
                );
        if (optionalZipCode.isEmpty()) {
            checkCustomerTypeAndAddExceptionMessage(
                    request,
                    localAddressData.getZipCodeId(),
                    "Zip code not found in entered populated place; ",
                    exceptionMessages
            );
            return null;
        } else {
            return optionalZipCode.get();
        }
    }

    private void fillForeignAddressFields(CreateCustomerRequest request,
                                          CustomerDetails customerDetails,
                                          List<NomenclatureItemStatus> statuses,
                                          List<String> exceptionMessages) {
        ForeignAddressData foreignAddressData = request.getAddress().getForeignAddressData();
        customerDetails.setRegionForeign(foreignAddressData.getRegion());
        customerDetails.setMunicipalityForeign(foreignAddressData.getMunicipality());
        customerDetails.setPopulatedPlaceForeign(foreignAddressData.getPopulatedPlace());
        customerDetails.setZipCodeForeign(foreignAddressData.getZipCode());
        customerDetails.setDistrictForeign(foreignAddressData.getDistrict());
        customerDetails.setStreetForeign(foreignAddressData.getStreet());
        customerDetails.setResidentialAreaForeign(foreignAddressData.getResidentialArea());
        customerDetails.setCountryId(getCountryId(request, foreignAddressData.getCountryId(), statuses, exceptionMessages));
    }

    private Long getCountryId(CreateCustomerRequest request,
                              Long countryId,
                              List<NomenclatureItemStatus> statuses,
                              List<String> exceptionMessages) {
        Optional<Country> optionalCountry = countryRepository.findByIdAndStatus(countryId, statuses);
        if (optionalCountry.isEmpty()) {
            checkCustomerTypeAndAddExceptionMessage(
                    request,
                    countryId,
                    "Country not found; ",
                    exceptionMessages
            );
            return null;
        } else {
            return optionalCountry.get().getId();
        }
    }

    private void checkCustomerTypeAndAddExceptionMessage(CreateCustomerRequest request,
                                                         Long objectId,
                                                         String message,
                                                         List<String> exceptionMessages) {
        if (request.getCustomerDetailStatus() != CustomerDetailStatus.POTENTIAL
                || objectId != null) {
            exceptionMessages.add(message);
        }
    }


    public CustomerDetails editCustomerDetails(Customer customer, Long id, EditCustomerRequest request, List<String> exceptionMessages) {
        Optional<CustomerDetails> customerDetailsOptional = customerDetailsRepository.findByCustomerId(id);
        if (customerDetailsOptional.isPresent()) {
            CustomerDetails dbCustomerDetails = customerDetailsOptional.get();
            CustomerDetails editCustomerDetails =
                    createCustomerdetails(new CreateCustomerRequest(request), customer, List.of(NomenclatureItemStatus.ACTIVE), exceptionMessages, true);
            if (editCustomerDetails != null) {
                editCustomerDetails = mapCustomerDetails(customer, dbCustomerDetails, editCustomerDetails);
                return customerDetailsRepository.save(editCustomerDetails);
            } else {
                exceptionMessages.add("Customer Details is null; ");
            }
        } else {
            exceptionMessages.add("Customer Details does not exists; ");
        }
        return null;
    }

    private CustomerDetails mapCustomerDetails(Customer customer, CustomerDetails dbCustomerDetails, CustomerDetails editCustomerDetails) {
        dbCustomerDetails.setName(checkGDPRName(editCustomerDetails.getName(),
                dbCustomerDetails,
                customer,
                GDPRCustomerFields.NAME));
        dbCustomerDetails.setNameTransl(checkGDPRName(editCustomerDetails.getNameTransl(),
                dbCustomerDetails,
                customer,
                GDPRCustomerFields.NAME_TRANS));
        dbCustomerDetails.setMiddleName(checkGDPRName(editCustomerDetails.getMiddleName(),
                dbCustomerDetails,
                customer,
                GDPRCustomerFields.MIDDLE_NAME));
        dbCustomerDetails.setMiddleNameTransl(checkGDPRName(editCustomerDetails.getMiddleNameTransl(),
                dbCustomerDetails,
                customer,
                GDPRCustomerFields.MIDDLE_NAME_TRANS));
        dbCustomerDetails.setLastName(checkGDPRName(editCustomerDetails.getLastName(),
                dbCustomerDetails,
                customer,
                GDPRCustomerFields.LAST_NAME));
        dbCustomerDetails.setLastName(checkGDPRName(editCustomerDetails.getLastNameTransl(),
                dbCustomerDetails,
                customer,
                GDPRCustomerFields.LAST_NAME_TRANS));
        dbCustomerDetails.setOldCustomerNumbers(editCustomerDetails.getOldCustomerNumbers());
        dbCustomerDetails.setVatNumber(editCustomerDetails.getVatNumber());
        dbCustomerDetails.setLegalFormId(editCustomerDetails.getLegalFormId());
        dbCustomerDetails.setLegalFormTranslId(editCustomerDetails.getLegalFormTranslId());
        dbCustomerDetails.setOwnershipFormId(editCustomerDetails.getOwnershipFormId());
        dbCustomerDetails.setEconomicBranchCiId(editCustomerDetails.getEconomicBranchCiId());
        dbCustomerDetails.setEconomicBranchNceaId(editCustomerDetails.getEconomicBranchNceaId());
        dbCustomerDetails.setMainActivitySubject(editCustomerDetails.getMainActivitySubject());
        dbCustomerDetails.setCreditRating(editCustomerDetails.getCreditRating());
        dbCustomerDetails.setBank(editCustomerDetails.getBank());
        dbCustomerDetails.setIban(editCustomerDetails.getIban());
        dbCustomerDetails.setZipCode(editCustomerDetails.getZipCode());
        dbCustomerDetails.setStreetNumber(editCustomerDetails.getStreetNumber());
        dbCustomerDetails.setAddressAdditionalInfo(editCustomerDetails.getAddressAdditionalInfo());
        dbCustomerDetails.setBlock(editCustomerDetails.getBlock());
        dbCustomerDetails.setEntrance(editCustomerDetails.getEntrance());
        dbCustomerDetails.setFloor(editCustomerDetails.getFloor());
        dbCustomerDetails.setApartment(editCustomerDetails.getApartment());
        dbCustomerDetails.setMailbox(editCustomerDetails.getMailbox());
        dbCustomerDetails.setStreetId(editCustomerDetails.getStreetId());
        dbCustomerDetails.setResidentialAreaId(editCustomerDetails.getResidentialAreaId());
        dbCustomerDetails.setDistrictId(editCustomerDetails.getDistrictId());
        dbCustomerDetails.setRegionForeign(editCustomerDetails.getRegionForeign());
        dbCustomerDetails.setMunicipalityForeign(editCustomerDetails.getMunicipalityForeign());
        dbCustomerDetails.setPopulatedPlaceForeign(editCustomerDetails.getPopulatedPlaceForeign());
        dbCustomerDetails.setZipCodeForeign(editCustomerDetails.getZipCodeForeign());
        dbCustomerDetails.setDistrictForeign(editCustomerDetails.getDistrictForeign());
        dbCustomerDetails.setPublicProcurementLaw(editCustomerDetails.getPublicProcurementLaw());
        dbCustomerDetails.setMarketingCommConsent(editCustomerDetails.getMarketingCommConsent());
        dbCustomerDetails.setForeignEntityPerson(editCustomerDetails.getForeignEntityPerson());
        dbCustomerDetails.setDirectDebit(editCustomerDetails.getDirectDebit());
        dbCustomerDetails.setForeignAddress(editCustomerDetails.getForeignAddress());
        dbCustomerDetails.setPopulatedPlaceId(editCustomerDetails.getPopulatedPlaceId());
        dbCustomerDetails.setCountryId(editCustomerDetails.getCountryId());
        dbCustomerDetails.setStatus(editCustomerDetails.getStatus());
        dbCustomerDetails.setModifySystemUserId("user");//TODO sysUser add
        dbCustomerDetails.setModifyDate(LocalDateTime.now());
        return dbCustomerDetails;
    }

    //TODO: WHAT IS THIS
    private String checkGDPRName(String text, CustomerDetails dbCustomerDetails, Customer customer, GDPRCustomerFields gdprCustomerFields) {
        if (text == null) {
            return "";
        }
        if (text.equals("GDPR")) {
            switch (gdprCustomerFields) {
                case IDENTIFIER:
                    return customer.getIdentifier();
                case NAME:
                    return dbCustomerDetails.getName();
                case NAME_TRANS:
                    return dbCustomerDetails.getNameTransl();
                case MIDDLE_NAME:
                    return dbCustomerDetails.getMiddleName();
                case MIDDLE_NAME_TRANS:
                    return dbCustomerDetails.getMiddleNameTransl();
                case LAST_NAME:
                    return dbCustomerDetails.getLastName();
                case LAST_NAME_TRANS:
                    return dbCustomerDetails.getLastNameTransl();
            }
        }
        return text;
    }

    public List<CustomerVersionsResponse> getVersions(Long customerId, List<CustomerDetailStatus> customerDetailStatuses) {
        return customerDetailsRepository.getVersions(customerId,customerDetailStatuses);
    }

    public Optional<CustomerDetails> findByCustomerIdAndVersionId(Long id, Long version) {
        return customerDetailsRepository.findByCustomerIdAndVersionId(id,version);
    }

    public Optional<CustomerDetails> findFirstByCustomerId(Long id, Sort createDate) {
        return customerDetailsRepository.findFirstByCustomerId(id,createDate);
    }
}
