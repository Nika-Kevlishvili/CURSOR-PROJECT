package bg.energo.phoenix.service.customer;

import bg.energo.phoenix.exception.ClientException;
import bg.energo.phoenix.exception.DomainEntityNotFoundException;
import bg.energo.phoenix.exception.ErrorCode;
import bg.energo.phoenix.model.entity.customer.Customer;
import bg.energo.phoenix.model.entity.customer.CustomerDetails;
import bg.energo.phoenix.model.entity.nomenclature.address.*;
import bg.energo.phoenix.model.entity.nomenclature.customer.*;
import bg.energo.phoenix.model.entity.nomenclature.customer.legalForm.LegalForm;
import bg.energo.phoenix.model.entity.nomenclature.customer.legalForm.LegalFormTransliterated;
import bg.energo.phoenix.model.enums.customer.CustomerDetailStatus;
import bg.energo.phoenix.model.enums.customer.CustomerStatus;
import bg.energo.phoenix.model.enums.customer.CustomerType;
import bg.energo.phoenix.model.enums.customer.GDPRCustomerFields;
import bg.energo.phoenix.model.enums.nomenclature.NomenclatureItemStatus;
import bg.energo.phoenix.model.request.customer.*;
import bg.energo.phoenix.model.response.customer.CustomerShortResponse;
import bg.energo.phoenix.permissions.PermissionContextEnum;
import bg.energo.phoenix.permissions.PermissionEnum;
import bg.energo.phoenix.repository.customer.CustomerAccountManagerRepository;
import bg.energo.phoenix.repository.customer.CustomerDetailsRepository;
import bg.energo.phoenix.repository.nomenclature.address.*;
import bg.energo.phoenix.repository.nomenclature.customer.*;
import bg.energo.phoenix.repository.nomenclature.customer.legalForm.LegalFormRepository;
import bg.energo.phoenix.repository.nomenclature.customer.legalForm.LegalFormTransliteratedRepository;
import bg.energo.phoenix.security.PermissionService;
import bg.energo.phoenix.util.epb.EPBChainedExceptionTriggerUtil;
import bg.energo.phoenix.util.epb.EPBFinalFields;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;


@Service
@RequiredArgsConstructor
public class CustomerDetailsService {
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
    private final CustomerAccountManagerService accountManagerService;
    private final PermissionService permissionService;

    public CustomerDetails createCustomerdetails(CreateCustomerRequest request,
                                                 Customer customer,
                                                 List<NomenclatureItemStatus> statuses,
                                                 List<String> exceptionMessages) {
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
        customerDetails.setPreferCommunicationInEnglish(request.isPreferCommunicationInEnglish());
        fillBankingDetailsFields(request.getBankingDetails(), customerDetails, statuses, exceptionMessages);
        customerDetails.setForeignAddress(request.getAddress() != null ? request.getAddress().getForeign() : false);
        customerDetails.setStatus(request.getCustomerDetailStatus());

        if (exceptionMessages.isEmpty()) {
            return customerDetailsRepository.save(customerDetails);
        }

        return null;
    }

    /**
     * <h1>Create Customer Details New Version</h1>
     * function finds previous version of the customer , increases it and adds to the new customer object as well as
     * other customer related info.if customer type is not legal entity it sets the customer personal info to the
     * new version of the CustomerDetails object, also sets all the related objects to the new version of
     * customer details and saves it in the database
     *
     * @param request
     * @param customer
     * @param oldDetails
     * @param exceptionMessages
     * @return {@link CustomerDetails} object
     */
    @Transactional
    public CustomerDetails createCustomerDetailsNewVersion(EditCustomerRequest request, Customer customer, CustomerDetails oldDetails, List<String> exceptionMessages) {
        checkDetailsAccess(oldDetails);
        CustomerDetails createdCustomerDetails = null;
        List<NomenclatureItemStatus> statuses = List.of(NomenclatureItemStatus.ACTIVE, NomenclatureItemStatus.INACTIVE);
        Long prevousVersion = getCustomerDetailsLatestVersion(customer, exceptionMessages);//customerDetails.getVersionId();

        //createNewCustomerDetails
        CustomerDetails customerDetailsNewVersion = new CustomerDetails();
        customerDetailsNewVersion.setVersionId(++prevousVersion);
        customerDetailsNewVersion.setOldCustomerNumbers(request.getOldCustomerNumber());
        customerDetailsNewVersion.setVatNumber(request.getVatNumber());
        customerDetailsNewVersion.setMainActivitySubject(request.getMainSubjectOfActivity());
        if (!customer.getCustomerType().equals(CustomerType.LEGAL_ENTITY)) {
            fillCustomerDetailsFieldsForUpdate(customerDetailsNewVersion, oldDetails);
        }
        fillCustomerFieldsByTypesForNewVersion(new CreateCustomerRequest(request), customerDetailsNewVersion, oldDetails, statuses, exceptionMessages);
        fillAddressDataForNewVersion(new CreateCustomerRequest(request), customerDetailsNewVersion, oldDetails, statuses, exceptionMessages);
        customerDetailsNewVersion.setCustomerId(customer.getId());
        customerDetailsNewVersion.setMarketingCommConsent(request.getMarketingConsent());
        if (!oldDetails.getForeignEntityPerson().equals(request.getForeign()))
            exceptionMessages.add("foreign-Foreign Person value should match previos value");
        customerDetailsNewVersion.setForeignEntityPerson(request.getForeign());
        customerDetailsNewVersion.setPreferCommunicationInEnglish(request.isPreferCommunicationInEnglish());
        fillBankingDetailsFieldsForUpdateNewCustomerVersion(request.getBankingDetails(), customerDetailsNewVersion, oldDetails, statuses, exceptionMessages);
        customerDetailsNewVersion.setForeignAddress(request.getAddress().getForeign());
        customerDetailsNewVersion.setStatus(request.getCustomerDetailStatus());
        if (exceptionMessages.isEmpty()) {
            return customerDetailsRepository.save(customerDetailsNewVersion);
        }
        return null;
    }

    /**
     * <h1>Fill Customer Details Fields For Update</h1>
     * sets name,nameTransliterated, middleName,middleNameTransliterated,lastName
     * and lastNameTransliterated to the customerDetails object
     *
     * @param newVersion
     * @param customerDetails
     */
    private void fillCustomerDetailsFieldsForUpdate(CustomerDetails newVersion, CustomerDetails customerDetails) {
        newVersion.setName(customerDetails.getName());
        newVersion.setNameTransl(customerDetails.getNameTransl());
        newVersion.setMiddleName(customerDetails.getMiddleName());
        newVersion.setMiddleNameTransl(customerDetails.getMiddleNameTransl());
        newVersion.setLastName(customerDetails.getLastName());
        newVersion.setLastNameTransl(customerDetails.getLastNameTransl());
    }

    /**
     * <h1>Save Customer Details</h1>
     * function saves {@link CustomerDetails} object to the database
     *
     * @param customerDetails
     * @return {@link CustomerDetails} object
     */
    public CustomerDetails saveCustomerDetails(CustomerDetails customerDetails) {
        return customerDetailsRepository.save(customerDetails);
    }

    /**
     * <h1>Get Customer Details Latest Version</h1>
     * function finds max version value of the customerDetails and returns the id
     *
     * @param customer
     * @param exceptionMessages
     * @return version value
     */
    private Long getCustomerDetailsLatestVersion(Customer customer, List<String> exceptionMessages) {
        Optional<Long> latestCustomerDetails = customerDetailsRepository.findMaxVersionIdByCustomerId(customer.getId());
        if (latestCustomerDetails.isPresent()) {
            return latestCustomerDetails.get();
        } else {
            exceptionMessages.add("Cant find CustomerDetails by latest customer id;");
            return null;
        }
    }

    /**
     * <h1>Fill Customer Fields By Types</h1>
     * if customerType is legal entity function calls
     * {@link #fillBusinessRelatedFields(CreateCustomerRequest, CustomerDetails, List, List)},
     * also sets name and nameTransliterated of customer.
     * else if customer type is private customer with business activity function calls:
     * {@link #fillBusinessRelatedFields(CreateCustomerRequest, CustomerDetails, List, List)}
     * and {@link #fillPrivateCustomerFields(CreateCustomerRequest, CustomerDetails)}
     * else {@link #fillPrivateCustomerFields(CreateCustomerRequest, CustomerDetails)}
     *
     * @param request
     * @param customerDetails
     * @param statuses
     * @param exceptionMessages
     */
    private void fillCustomerFieldsByTypes(CreateCustomerRequest request,
                                           CustomerDetails customerDetails,
                                           List<NomenclatureItemStatus> statuses,
                                           List<String> exceptionMessages) {
        if (request.getCustomerType() == CustomerType.LEGAL_ENTITY) {
            fillBusinessRelatedFields(request, customerDetails, statuses, exceptionMessages);
            customerDetails.setName(request.getBusinessCustomerDetails().getName());
            customerDetails.setNameTransl(request.getBusinessCustomerDetails().getNameTranslated());
            customerDetails.setBusinessActivity(false);
        } else {
            if (request.getBusinessActivity()) {
                fillBusinessRelatedFields(request, customerDetails, statuses, exceptionMessages);
                customerDetails.setBusinessActivityName(request.getBusinessCustomerDetails().getName());
                customerDetails.setBusinessActivityNameTransl(request.getBusinessCustomerDetails().getNameTranslated());
            }
            customerDetails.setBusinessActivity(request.getBusinessActivity());
            fillPrivateCustomerFields(request, customerDetails);
        }
    }

    /**
     * <h1>Fill Customer Fields By Types For New Version</h1>
     * if customerType is legal entity function calls
     * {@link #fillBusinessRelatedFieldsForNewVersion(CreateCustomerRequest, CustomerDetails, CustomerDetails, List, List)},
     * also sets name and nameTransliterated of customer.
     * else if customer type is private customer with business activity function calls:
     * {@link #fillBusinessRelatedFieldsForNewVersion(CreateCustomerRequest, CustomerDetails, CustomerDetails, List, List)}
     * and {@link #fillPrivateCustomerFields(CreateCustomerRequest, CustomerDetails)}
     * else {@link #fillPrivateCustomerFields(CreateCustomerRequest, CustomerDetails)}
     *
     * @param request
     * @param customerDetails
     * @param customerDetailsCurrentVersion
     * @param statuses
     * @param exceptionMessages
     */
    private void fillCustomerFieldsByTypesForNewVersion(CreateCustomerRequest request,
                                                        CustomerDetails customerDetails,
                                                        CustomerDetails customerDetailsCurrentVersion,
                                                        List<NomenclatureItemStatus> statuses,
                                                        List<String> exceptionMessages) {
        if (request.getCustomerType() == CustomerType.LEGAL_ENTITY) {
            fillBusinessRelatedFieldsForNewVersion(request, customerDetails, customerDetailsCurrentVersion, statuses, exceptionMessages);
            customerDetails.setName(request.getBusinessCustomerDetails().getName());
            customerDetails.setNameTransl(request.getBusinessCustomerDetails().getNameTranslated());
            customerDetails.setBusinessActivity(false);
        } else {
            if (Boolean.TRUE.equals(request.getBusinessActivity()))
                fillBusinessRelatedFieldsForNewVersion(request, customerDetails, customerDetailsCurrentVersion, statuses, exceptionMessages);

            customerDetails.setBusinessActivity(request.getBusinessActivity());
            fillPrivateCustomerFields(request, customerDetails);
        }
    }

    /**
     * <h1>Fill Address Data</h1>
     * function saves customer address related information
     * if customer is foreigner it saves foreignAddress fields
     * else it saves localAddress fields
     *
     * @param request
     * @param customerDetails
     * @param statuses
     * @param exceptionMessages
     */
    private void fillAddressData(CreateCustomerRequest request,
                                 CustomerDetails customerDetails,
                                 List<NomenclatureItemStatus> statuses,
                                 List<String> exceptionMessages) {
        CustomerAddressRequest customerAddressRequest = request.getAddress();
        if (customerAddressRequest == null)
            return;
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

    /**
     * <h1>Fill Address Data For New Version</h1>
     * function saves customer address related information
     * if customer is foreigner it saves foreignAddress fields
     * else it saves localAddress fields
     *
     * @param request
     * @param customerDetails
     * @param customerDetailsCurrentVersion
     * @param statuses
     * @param exceptionMessages
     */
    private void fillAddressDataForNewVersion(CreateCustomerRequest request,
                                              CustomerDetails customerDetails,
                                              CustomerDetails customerDetailsCurrentVersion,
                                              List<NomenclatureItemStatus> statuses,
                                              List<String> exceptionMessages) {
        CustomerAddressRequest customerAddressRequest = request.getAddress();
        if (customerAddressRequest == null)
            return;
        if (customerAddressRequest.getForeign()) {
            fillForeignAddressFieldsForNewVersion(request, customerDetails, customerDetailsCurrentVersion, statuses, exceptionMessages);
        } else {
            fillLocalAddressFieldsForNewVersion(request, customerDetails, customerDetailsCurrentVersion, statuses, exceptionMessages);
        }
        customerDetails.setStreetNumber(customerAddressRequest.getNumber());
        customerDetails.setAddressAdditionalInfo(customerAddressRequest.getAdditionalInformation());
        customerDetails.setBlock(customerAddressRequest.getBlock());
        customerDetails.setEntrance(customerAddressRequest.getEntrance());
        customerDetails.setFloor(customerAddressRequest.getFloor());
        customerDetails.setApartment(customerAddressRequest.getApartment());
        customerDetails.setMailbox(customerAddressRequest.getMailbox());
    }

    /**
     * <h1>Edit Address Data</h1>
     * function edits customer address related information
     * if customer is foreigner it edits foreignAddress fields
     * else it edits localAddress fields
     *
     * @param request
     * @param customerDetails
     * @param statuses
     * @param exceptionMessages
     */
    private void editAddressData(EditCustomerRequest request,
                                 CustomerDetails customerDetails,
                                 List<NomenclatureItemStatus> statuses,
                                 List<String> exceptionMessages) {
        CustomerAddressRequest customerAddressRequest = request.getAddress();
        if (customerAddressRequest.getForeign()) {
            editForeignAddressFields(request, customerDetails, statuses, exceptionMessages);
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

    /**
     * <h1>Fill Local Address Fields</h1>
     * function adds country id and populatePlaceReleatedFields to the {@link CustomerDetails} object
     *
     * @param request
     * @param customerDetails
     * @param statuses
     * @param exceptionMessages
     */
    private void fillLocalAddressFields(EditCustomerRequest request,
                                        CustomerDetails customerDetails,
                                        List<NomenclatureItemStatus> statuses,
                                        List<String> exceptionMessages) {
        LocalAddressData localAddressData = request.getAddress().getLocalAddressData();
       /* if (localAddressData != null && request.getCustomerDetailStatus().equals(CustomerDetailStatus.POTENTIAL))
            return;*/
        Long countryId = getCountryIdForEdit(request, localAddressData.getCountryId(), customerDetails.getCountryId(), statuses, exceptionMessages);
        if (countryId != null) {
            customerDetails.setCountryId(countryId);
            fillPopulatedPlaceRelatedFields(request, customerDetails, countryId, statuses, exceptionMessages);
        }
        if (localAddressData.getCountryId() == null) {
            customerDetails.setCountryId(null);
        }
        if (localAddressData.getStreetId() == null) {
            customerDetails.setStreetId(null);
        }
        if (localAddressData.getResidentialAreaId() == null) {
            customerDetails.setResidentialAreaId(null);
        }
        if (localAddressData.getDistrictId() == null) {
            customerDetails.setDistrictId(null);
        }
        if (localAddressData.getZipCodeId() == null) {
            customerDetails.setZipCode(null);
        }
        if (localAddressData.getPopulatedPlaceId() == null) {
            customerDetails.setPopulatedPlaceId(null);
        }

        customerDetails.setStreetType(localAddressData.getStreetType());
        customerDetails.setResidentialAreaType(localAddressData.getResidentialAreaType());

        customerDetails.setRegionForeign(null);
        customerDetails.setMunicipalityForeign(null);
        customerDetails.setPopulatedPlaceForeign(null);
        customerDetails.setZipCodeForeign(null);
        customerDetails.setDistrictForeign(null);
        customerDetails.setStreetTypeForeign(null);
        customerDetails.setStreetForeign(null);
        customerDetails.setResidentialAreaTypeForeign(null);
        customerDetails.setResidentialAreaForeign(null);
    }

    /**
     * <h1>Fill Banking Details Fields</h1>
     * if banking details are not empty function sets credit rating and bank to the
     * {@link CustomerDetails} object , as well as directDebit, CustomerDeclaredConsumption and IBan
     *
     * @param customerBankingDetails
     * @param customerDetails
     * @param statuses
     * @param exceptionMessages
     */
    private void fillBankingDetailsFields(CustomerBankingDetails customerBankingDetails,
                                          CustomerDetails customerDetails,
                                          List<NomenclatureItemStatus> statuses,
                                          List<String> exceptionMessages) {
        if (customerBankingDetails != null) {
            customerDetails.setCreditRating(
                    getCreditRating(customerBankingDetails.getCreditRatingId(), customerDetails.getCreditRating(), statuses, exceptionMessages)
            );
            customerDetails.setBank(
                    getBank(customerBankingDetails, customerDetails.getBank(), statuses, exceptionMessages)
            );
            customerDetails.setDirectDebit(customerBankingDetails.getDirectDebit());
            customerDetails.setCustomerDeclaredConsumption(customerBankingDetails.getDeclaredConsumption());
            customerDetails.setIban(customerBankingDetails.getIban());
        }
    }

    /**
     * <h1>Fill Banking Details Fields For Update New Customer Version</h1>
     *
     * @param customerBankingDetails
     * @param customerDetails
     * @param statuses
     * @param exceptionMessages
     */
    private void fillBankingDetailsFieldsForUpdateNewCustomerVersion(CustomerBankingDetails customerBankingDetails,
                                                                     CustomerDetails customerDetails,
                                                                     CustomerDetails customerDetailsCurrentVersion,
                                                                     List<NomenclatureItemStatus> statuses,
                                                                     List<String> exceptionMessages) {
        if (customerBankingDetails != null) {
            customerDetails.setCreditRating(
                    getCreditRating(customerBankingDetails.getCreditRatingId(), customerDetailsCurrentVersion.getCreditRating(), statuses, exceptionMessages)
            );
            customerDetails.setBank(
                    getBank(customerBankingDetails, customerDetailsCurrentVersion.getBank(), statuses, exceptionMessages)
            );
            customerDetails.setDirectDebit(customerBankingDetails.getDirectDebit());
            customerDetails.setCustomerDeclaredConsumption(customerBankingDetails.getDeclaredConsumption());
            customerDetails.setIban(customerBankingDetails.getIban());
        }
    }

    /**
     * <h1>Get Credit Rating For Edit</h1>
     * function selects {@link CreditRating} object from db
     * if object is empty or object is changed and status of new object is INACTIVE
     * it adds exception message to the exceptionMessages list
     * else returns the object
     *
     * @param creditRatingId
     * @param dbCreditRating
     * @param statuses
     * @param exceptionMessages
     * @return
     */
    private CreditRating getCreditRating(Long creditRatingId,
                                         CreditRating dbCreditRating,
                                         List<NomenclatureItemStatus> statuses,
                                         List<String> exceptionMessages) {
        Optional<CreditRating> optionalCreditRating = creditRatingRepository.findByIdAndStatus(creditRatingId, statuses);
        if (optionalCreditRating.isEmpty()) {
            if (creditRatingId != null) {
                exceptionMessages.add("bankingDetails.creditRatingId-Credit Rating not found;");
            }
            return null;
        } else if (optionalCreditRating.get().getStatus().equals(NomenclatureItemStatus.INACTIVE)
                && (dbCreditRating == null || !creditRatingId.equals(dbCreditRating.getId()))) {

            exceptionMessages.add("bankingDetails.creditRatingId-Can't add INACTIVE nomenclature item;");
            return null;
        } else {
            return optionalCreditRating.get();
        }
    }

    /**
     * <h1>Get Bank For Edit</h1>
     * function selects {@link Bank} object from db
     * if object is empty or object is changed and status of new object is INACTIVE
     * it adds exception message to the exceptionMessages list
     * else returns the object
     *
     * @param customerBankingDetails
     * @param dbBank
     * @param statuses
     * @param exceptionMessages
     * @return
     */
    private Bank getBank(CustomerBankingDetails customerBankingDetails,
                         Bank dbBank,
                         List<NomenclatureItemStatus> statuses,
                         List<String> exceptionMessages) {
        Optional<Bank> optionalBank = bankRepository
                .findByIdAndStatus(customerBankingDetails.getBankId(), statuses);
        if (optionalBank.isEmpty()) {
            if (customerBankingDetails.getDirectDebit() != null && customerBankingDetails.getDirectDebit()) {
                exceptionMessages.add("bankingDetails.bankId-Bank not found;");
            }
            Optional<Bank> inactiveBankInfo = bankRepository.findByIdAndStatus(customerBankingDetails.getBankId(), List.of(NomenclatureItemStatus.INACTIVE));
            if (inactiveBankInfo.isPresent() && (dbBank == null || !customerBankingDetails.getBankId().equals(dbBank.getId()))) {
                exceptionMessages.add("bankingDetails.bankId-Can't add INACTIVE nomenclature item;");
                return null;
            }
            return null;
        } else {
            return optionalBank.get();
        }
    }

    /**
     * <h1>Fill Business Related Fields</h1>
     * function sets procurment law ,
     * legalForm nomenclature id,
     * legalForm transliterated nomenclature id,
     * ownership id ,
     * economicbranchId and economicBanchNCEAid
     *
     * @param request
     * @param customerDetails
     * @param statuses
     * @param exceptionMessages
     */
    private void fillBusinessRelatedFields(CreateCustomerRequest request,
                                           CustomerDetails customerDetails,
                                           List<NomenclatureItemStatus> statuses,
                                           List<String> exceptionMessages) {
        BusinessCustomerDetails businessCustomerDetails = request.getBusinessCustomerDetails();
        customerDetails.setPublicProcurementLaw(businessCustomerDetails.getProcurementLaw());
        if (businessCustomerDetails.getLegalFormId() != null) {
            customerDetails.setLegalFormId(
                    getLegalFormId(businessCustomerDetails, statuses, exceptionMessages)
            );
            customerDetails.setLegalFormTranslId(
                    getLegalFormTranslId(businessCustomerDetails, statuses, exceptionMessages)
            );
        }
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

    private void fillBusinessRelatedFieldsForNewVersion(CreateCustomerRequest request,
                                                        CustomerDetails customerDetails,
                                                        CustomerDetails customerDetailsCurrentVersion,
                                                        List<NomenclatureItemStatus> statuses,
                                                        List<String> exceptionMessages) {
        customerDetails.setPublicProcurementLaw(request.getBusinessCustomerDetails().getProcurementLaw());
        customerDetails.setLegalFormId(
                getLegalFormIdForEdit(request.getBusinessCustomerDetails(), customerDetailsCurrentVersion.getLegalFormId(), statuses, exceptionMessages)
        );
        customerDetails.setLegalFormTranslId(
                getLegalFormTranslIdForEdit(request.getBusinessCustomerDetails(), customerDetailsCurrentVersion.getLegalFormTranslId(), statuses, exceptionMessages)
        );
        customerDetails.setOwnershipFormId(
                getOwnershipFormIdForEdit(request, customerDetailsCurrentVersion.getOwnershipFormId(), statuses, exceptionMessages)
        );
        customerDetails.setEconomicBranchCiId(
                getEconomicBranchCIId(request, customerDetailsCurrentVersion.getEconomicBranchCiId(), statuses, exceptionMessages)
        );
        customerDetails.setEconomicBranchNceaId(
                getEconomicBranchNCEAId(request, customerDetailsCurrentVersion.getEconomicBranchNceaId(), statuses, exceptionMessages)
        );
        customerDetails.setBusinessActivityName(request.getBusinessCustomerDetails().getName());
        customerDetails.setBusinessActivityNameTransl(request.getBusinessCustomerDetails().getNameTranslated());
    }

    /**
     * <h1>Get Legal Form Id</h1>
     * function selects {@link LegalForm} from db
     * if record is empty it adds error message
     * else return record
     *
     * @param businessCustomerDetails
     * @param statuses
     * @param exceptionMessages
     * @return long id
     */
    private Long getLegalFormId(BusinessCustomerDetails businessCustomerDetails,
                                List<NomenclatureItemStatus> statuses,
                                List<String> exceptionMessages) {
        Optional<LegalForm> optionalLegalForm = legalFormRepository
                .findByIdAndStatus(businessCustomerDetails.getLegalFormId(), statuses);
        if (optionalLegalForm.isEmpty()) {
            exceptionMessages.add("businessCustomerDetails.legalFormId-Legal form not found;");
            return null;
        } else {
            return optionalLegalForm.get().getId();
        }
    }

    private Long getLegalFormIdForEdit(BusinessCustomerDetails businessCustomerDetails,
                                       Long dbLegalFormId,
                                       List<NomenclatureItemStatus> statuses,
                                       List<String> exceptionMessages) {
        Long legalFormId = businessCustomerDetails.getLegalFormId();
        if (legalFormId != null) {
            Optional<LegalForm> optionalLegalForm = legalFormRepository
                    .findByIdAndStatus(legalFormId, statuses);
            if (optionalLegalForm.isEmpty()) {
                exceptionMessages.add("businessCustomerDetails.legalFormId-Legal form not found;");
                return null;
            } else if (optionalLegalForm.get().getStatus().equals(NomenclatureItemStatus.INACTIVE)
                    && (!legalFormId.equals(dbLegalFormId))) {

                exceptionMessages.add("businessCustomerDetails.legalFormId-Can't add INACTIVE nomenclature item;");
                return null;
            } else {
                return optionalLegalForm.get().getId();
            }
        } else return null;
    }

    /**
     * <h1>Get Legal Form TranslId</h1>
     * function selects {@link LegalFormTransliterated} from db
     * if record is empty it adds error message
     * else return record
     *
     * @param businessCustomerDetails
     * @param statuses
     * @param exceptionMessages
     * @return long id
     */
    private Long getLegalFormTranslId(BusinessCustomerDetails businessCustomerDetails,
                                      List<NomenclatureItemStatus> statuses,
                                      List<String> exceptionMessages) {
        Optional<LegalFormTransliterated> optionalLegalFormTransliterated = legalFormTransliteratedRepository
                .findByIdAndStatus(businessCustomerDetails.getLegalFormTransId(), statuses);
        if (optionalLegalFormTransliterated.isEmpty()) {
            exceptionMessages.add("businessCustomerDetails.legalFormTransId-Legal form Transl. not found;");
            return null;
        } else {
            return optionalLegalFormTransliterated.get().getId();
        }
    }

    private Long getLegalFormTranslIdForEdit(BusinessCustomerDetails businessCustomerDetails,
                                             Long dbLegalFormTransId,
                                             List<NomenclatureItemStatus> statuses,
                                             List<String> exceptionMessages) {
        Long legalFormTransId = businessCustomerDetails.getLegalFormTransId();
        if (legalFormTransId != null) {
            Optional<LegalFormTransliterated> optionalLegalFormTransliterated = legalFormTransliteratedRepository
                    .findByIdAndStatus(legalFormTransId, statuses);
            if (optionalLegalFormTransliterated.isEmpty()) {
                exceptionMessages.add("businessCustomerDetails.legalFormTransId-Legal form Transl. not found;");
                return null;
            } else if (optionalLegalFormTransliterated.get().getStatus().equals(NomenclatureItemStatus.INACTIVE)
                    && (!legalFormTransId.equals(dbLegalFormTransId))) {

                exceptionMessages.add("businessCustomerDetails.legalFormTransId-Can't add INACTIVE nomenclature item;");
                return null;
            } else {
                return optionalLegalFormTransliterated.get().getId();
            }
        } else
            return null;
    }

    /**
     * <h1>Get Ownership Form Id</h1>
     * function selects {@link OwnershipForm} from db
     * if there is no data  and customer status is Potential function adds error message to the array of exceptionMessages
     * else return id
     *
     * @param request
     * @param statuses
     * @param exceptionMessages
     * @return Long id
     */
    private Long getOwnershipFormId(CreateCustomerRequest request,
                                    List<NomenclatureItemStatus> statuses,
                                    List<String> exceptionMessages) {
        Optional<OwnershipForm> optionalOwnershipForm = ownershipFormRepository
                .findByIdAndStatus(request.getOwnershipFormId(), statuses);
        if (optionalOwnershipForm.isEmpty()) {
            checkCustomerTypeAndAddExceptionMessage(request,
                    request.getOwnershipFormId(),
                    "ownershipFormId-Ownership form not found;",
                    exceptionMessages);
            return null;
        } else {
            return optionalOwnershipForm.get().getId();
        }
    }

    /**
     * <h1>Get Economic Branch CI Id</h1>
     * function selects {@link EconomicBranchCI} object from the db according to economicBranchCIId and
     * {@link NomenclatureItemStatus} statuses array.
     * if there is no data  and customer status is Potential function adds error message to the array of exceptionMessages
     * else returns economicBranchCIId
     *
     * @param request
     * @param statuses
     * @param exceptionMessages
     * @return Long economicBranchCIId
     */
    private Long getEconomicBranchCIId(CreateCustomerRequest request,
                                       List<NomenclatureItemStatus> statuses,
                                       List<String> exceptionMessages) {
        Optional<EconomicBranchCI> optionalEconomicBranchCI = economicBranchCIRepository
                .findByIdAndStatus(request.getEconomicBranchId(), statuses);
        if (optionalEconomicBranchCI.isEmpty()) {
            checkCustomerTypeAndAddExceptionMessage(request,
                    request.getEconomicBranchId(),
                    "economicBranchId-Economic Branch CI not found;",
                    exceptionMessages);
            return null;
        } else {
            return optionalEconomicBranchCI.get().getId();
        }
    }

    /**
     * <h1>Get Economic Branch NCEAId</h1>
     * function selects {@link EconomicBranchNCEA} object from the db according to economicBranchNCEAId and
     * {@link NomenclatureItemStatus} statuses array.
     * if there is no data  and customer status is Potential function adds error message to the array of exceptionMessages
     * else returns economicBranchNCEAId
     *
     * @param request
     * @param statuses
     * @param exceptionMessages
     * @return Long economicBranchNCEAId
     */
    private Long getEconomicBranchNCEAId(CreateCustomerRequest request,
                                         List<NomenclatureItemStatus> statuses,
                                         List<String> exceptionMessages) {
        Optional<EconomicBranchNCEA> optionalEconomicBranchNCEA = economicBranchNCEARepository
                .findByIdAndStatus(request.getEconomicBranchNCEAId(), statuses);
        if (optionalEconomicBranchNCEA.isEmpty()) {
            if (request.getEconomicBranchNCEAId() != null) {
                exceptionMessages.add("economicBranchNCEAId-Economic Branch NCEA not found;");
            }
            return null;
        } else {
            return optionalEconomicBranchNCEA.get().getId();
        }
    }

    /**
     * <h1>Fill Private Customer Fields</h1>
     *
     * @param request
     * @param customerDetails
     */
    private void fillPrivateCustomerFields(CreateCustomerRequest request,
                                           CustomerDetails customerDetails) {
        PrivateCustomerDetails privateCustomerDetails = request.getPrivateCustomerDetails();
        if (!StringUtils.equals(privateCustomerDetails.getFirstName(), EPBFinalFields.GDPR)) {
            customerDetails.setName(privateCustomerDetails.getFirstName());
        }
        if (!StringUtils.equals(privateCustomerDetails.getFirstNameTranslated(), EPBFinalFields.GDPR)) {
            customerDetails.setNameTransl(privateCustomerDetails.getFirstNameTranslated());
        }
        if (!StringUtils.equals(privateCustomerDetails.getMiddleName(), EPBFinalFields.GDPR)) {
            customerDetails.setMiddleName(privateCustomerDetails.getMiddleName());
        }
        if (!StringUtils.equals(privateCustomerDetails.getMiddleNameTranslated(), EPBFinalFields.GDPR)) {
            customerDetails.setMiddleNameTransl(privateCustomerDetails.getMiddleNameTranslated());
        }
        if (!StringUtils.equals(privateCustomerDetails.getLastName(), EPBFinalFields.GDPR)) {
            customerDetails.setLastName(privateCustomerDetails.getLastName());
        }
        if (!StringUtils.equals(privateCustomerDetails.getLastNameTranslated(), EPBFinalFields.GDPR)) {
            customerDetails.setLastNameTransl(privateCustomerDetails.getLastNameTranslated());
        }
        customerDetails.setGdprRegulationConsent(privateCustomerDetails.getGdprRegulationConsent());
    }

    /**
     * <h1>Fill Local Address Fields</h1>
     * function adds country id and populatePlaceReleatedFields to the {@link CustomerDetails} object
     *
     * @param request
     * @param customerDetails
     * @param statuses
     * @param exceptionMessages
     */
    private void fillLocalAddressFields(CreateCustomerRequest request,
                                        CustomerDetails customerDetails,
                                        List<NomenclatureItemStatus> statuses,
                                        List<String> exceptionMessages) {
        LocalAddressData localAddressData = request.getAddress().getLocalAddressData();
        if (localAddressData == null && request.getCustomerDetailStatus().equals(CustomerDetailStatus.POTENTIAL))
            return;
        Long countryId = getCountryId(request, localAddressData.getCountryId(), statuses, exceptionMessages);
        if (countryId != null) {
            customerDetails.setCountryId(countryId);
            fillPopulatedPlaceRelatedFields(request, customerDetails, countryId, statuses, exceptionMessages);
        }
        customerDetails.setStreetType(localAddressData.getStreetType());
        customerDetails.setResidentialAreaType(localAddressData.getResidentialAreaType());
    }

    private void fillLocalAddressFieldsForNewVersion(CreateCustomerRequest request,
                                                     CustomerDetails customerDetails,
                                                     CustomerDetails customerDetailsCurrentVersion,
                                                     List<NomenclatureItemStatus> statuses,
                                                     List<String> exceptionMessages) {
        LocalAddressData localAddressData = request.getAddress().getLocalAddressData();
        if (localAddressData == null && request.getCustomerDetailStatus().equals(CustomerDetailStatus.POTENTIAL))
            return;
        Long countryId = getCountryIdForEdit(request, localAddressData.getCountryId(), customerDetailsCurrentVersion.getCountryId(), statuses, exceptionMessages);
        if (countryId != null) {
            customerDetails.setCountryId(countryId);
            fillPopulatedPlaceRelatedFields(request, customerDetails, countryId, statuses, exceptionMessages);
        }
        customerDetails.setStreetType(localAddressData.getStreetType());
        customerDetails.setResidentialAreaType(localAddressData.getResidentialAreaType());
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

    /**
     * <h1>Fill Populated Place Related Fields</h1>
     * function gets populatedPlaceId if its not null sets populatedPlaceId, StreetId, residentialAre, district and zipCode
     * to the {@link CustomerDetails} object
     *
     * @param request
     * @param customerDetails
     * @param countryId
     * @param statuses
     * @param exceptionMessages
     */
    private void fillPopulatedPlaceRelatedFields(EditCustomerRequest request,
                                                 CustomerDetails customerDetails,
                                                 Long countryId,
                                                 List<NomenclatureItemStatus> statuses,
                                                 List<String> exceptionMessages) {
        Long populatedPlaceId = getPopulatedPlaceId(request, customerDetails.getPopulatedPlaceId(), countryId, statuses, exceptionMessages);
        if (populatedPlaceId != null) {
            customerDetails.setPopulatedPlaceId(populatedPlaceId);
            customerDetails.setStreetId(
                    getStreetIdForEdit(request.getAddress().getLocalAddressData(), populatedPlaceId, customerDetails.getStreetId(), statuses, exceptionMessages)
            );
            customerDetails.setResidentialAreaId(
                    getResidentialAreaIdForEdit(request.getAddress().getLocalAddressData(), populatedPlaceId, customerDetails.getResidentialAreaId(), statuses, exceptionMessages)
            );
            customerDetails.setDistrictId(
                    getDistrictId(request, populatedPlaceId, customerDetails.getDistrictId(), statuses, exceptionMessages)
            );
            customerDetails.setZipCode(
                    getZipCode(request, populatedPlaceId, customerDetails.getZipCode(), statuses, exceptionMessages)
            );
        }
    }

    /**
     * <h1>Get Populated Place Id</h1>
     * function select populatedPlace info based on populatedPlaceId ,
     * if record is empty  checks if customer is Potential and adds error message
     * else gets country from db and compares it to the request countryId
     * if they are not the same adds exception
     * else returns populatedPlaceId
     *
     * @param request
     * @param countryId
     * @param statuses
     * @param exceptionMessages
     * @return
     */
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
                    "address.localAddressData.populatedPlaceId-Populated place not found;",
                    exceptionMessages
            );
            return null;
        } else {
            Country populatedPlaceCountry = optionalPopulatedPlace.get().getMunicipality().getRegion().getCountry();
            if (!countryId.equals(populatedPlaceCountry.getId())) {
                exceptionMessages.add("address.localAddressData.populatedPlaceId-Populated place is not in entered country;");
                return null;
            }
            return optionalPopulatedPlace.get().getId();
        }
    }

    /**
     * <h1>Get Populated Place Id</h1>
     * function select populatedPlace info based on populatedPlaceId ,
     * if record is empty  checks if customer is Potential and adds error message
     * if object is changed and status of new item is INACTIVE adds error message
     * else gets country from db and compares it to the request countryId
     * if they are not the same adds exception
     * else returns populatedPlaceId
     *
     * @param request
     * @param dbPopulatedPlaceId
     * @param countryId
     * @param statuses
     * @param exceptionMessages
     * @return
     */
    private Long getPopulatedPlaceId(EditCustomerRequest request,
                                     Long dbPopulatedPlaceId,
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
                    "address.localAddressData.populatedPlaceId-Populated place not found;",
                    exceptionMessages
            );
            return null;
        } else if (optionalPopulatedPlace.get().getStatus().equals(NomenclatureItemStatus.INACTIVE)
                && (!localAddressData.getPopulatedPlaceId().equals(dbPopulatedPlaceId))) {

            exceptionMessages.add("address.localAddressData.populatedPlaceId-Can't add INACTIVE nomenclature item;");
            return null;
        } else {
            Country populatedPlaceCountry = optionalPopulatedPlace.get().getMunicipality().getRegion().getCountry();
            if (!countryId.equals(populatedPlaceCountry.getId())) {
                exceptionMessages.add("address.localAddressData.populatedPlaceId-Populated place is not in entered country;");
                return null;
            }
            return optionalPopulatedPlace.get().getId();
        }
    }

    /**
     * <h1>Get Street Id</h1>
     * function select {@link Street} object from db
     * if record doesn't exists it adds error message to exceptionMessages list
     * else return street id
     *
     * @param localAddressData
     * @param populatedPlaceId
     * @param statuses
     * @param exceptionMessages
     * @return Long streetId
     */
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
                exceptionMessages.add("address.localAddressData.streetId-Street not found in entered populated place;");
            }
            return null;
        } else {
            return optionalStreet.get().getId();
        }
    }

    /**
     * <h1>Get Street Id For Edit</h1>
     * function select Street info based on streetId and populatedPlaceId
     * if record is empty and id is provided adds error message
     * if object is changed and status of new item is INACTIVE adds error message
     * else returns streetId
     *
     * @param localAddressData
     * @param populatedPlaceId
     * @param dbStreetId
     * @param statuses
     * @param exceptionMessages
     * @return
     */
    private Long getStreetIdForEdit(LocalAddressData localAddressData,
                                    Long populatedPlaceId,
                                    Long dbStreetId,
                                    List<NomenclatureItemStatus> statuses,
                                    List<String> exceptionMessages) {
        Optional<Street> optionalStreet = streetRepository.findByIdAndPopulatedPlaceIdAndStatus(
                localAddressData.getStreetId(),
                populatedPlaceId,
                statuses);
        if (optionalStreet.isEmpty()) {
            if (localAddressData.getStreetId() != null) {
                exceptionMessages.add("address.localAddressData.streetId-Street not found in entered populated place;");
            }
            return null;
        } else if (optionalStreet.get().getStatus().equals(NomenclatureItemStatus.INACTIVE)
                && (!localAddressData.getStreetId().equals(dbStreetId))) {

            exceptionMessages.add("address.localAddressData.streetId-Can't add INACTIVE nomenclature item;");
            return null;
        } else {
            return optionalStreet.get().getId();
        }
    }

    /**
     * <h1>Get Residential Area Id</h1>
     * function select {@link ResidentialArea} object from db
     * if record doesn't exists it adds error message to exceptionMessages list
     * else return residentialArea id
     *
     * @param localAddressData
     * @param populatedPlaceId
     * @param statuses
     * @param exceptionMessages
     * @return Long residentialAreaId
     */
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
                exceptionMessages.add("address.localAddressData.residentialAreaId-Residential Area not found in entered populated place;");
            }
            return null;
        } else {
            return optionalResidentialArea.get().getId();
        }
    }

    /**
     * <h1>Get Residential Area Id For Edit</h1>
     * function select Residential Area info based on residentialAreaId and populatedPlaceId
     * if record is empty and id is provided adds error message
     * if object is changed and status of new item is INACTIVE adds error message
     * else returns residentialAreaId
     *
     * @param localAddressData
     * @param populatedPlaceId
     * @param dbResidentialAreaId
     * @param statuses
     * @param exceptionMessages
     * @return
     */
    private Long getResidentialAreaIdForEdit(LocalAddressData localAddressData,
                                             Long populatedPlaceId,
                                             Long dbResidentialAreaId,
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
                exceptionMessages.add("address.localAddressData.residentialAreaId-Residential Area not found in entered populated place;");
            }
            return null;
        } else if (optionalResidentialArea.get().getStatus().equals(NomenclatureItemStatus.INACTIVE)
                && (!localAddressData.getResidentialAreaId().equals(dbResidentialAreaId))) {

            exceptionMessages.add("address.localAddressData.residentialAreaId-Can't add INACTIVE nomenclature item;");
            return null;
        } else {
            return optionalResidentialArea.get().getId();
        }
    }

    /**
     * <h1>Get District Id</h1>
     * function select district from database based on district id,
     * if record is empty adds error message
     * else returns districtId
     *
     * @param request
     * @param populatedPlaceId
     * @param statuses
     * @param exceptionMessages
     * @return
     */
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
            if (localAddressData.getDistrictId() != null) {
                exceptionMessages.add("address.localAddressData.districtId-District not found in entered populated place;");
            }
//            checkCustomerTypeAndAddExceptionMessage(
//                    request,
//                    localAddressData.getDistrictId(),
//                    "District not found in entered populated place; ",
//                    exceptionMessages
//            );
            return null;
        } else {
            return optionalDistrict.get().getId();
        }
    }

    /**
     * <h1>Get District Id</h1>
     * function select district from database based on district id,
     * if record is empty adds error message
     * else returns districtId
     *
     * @param request
     * @param populatedPlaceId
     * @param dbDistrictId
     * @param statuses
     * @param exceptionMessages
     * @return
     */
    private Long getDistrictId(EditCustomerRequest request,
                               Long populatedPlaceId,
                               Long dbDistrictId,
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
            if (localAddressData.getDistrictId() != null) {
                exceptionMessages.add("address.localAddressData.districtId-District not found in entered populated place;");
            }
            return null;
        } else if (optionalDistrict.get().getStatus().equals(NomenclatureItemStatus.INACTIVE)
                && (!localAddressData.getDistrictId().equals(dbDistrictId))) {

            exceptionMessages.add("address.localAddressData.districtId-Can't add INACTIVE nomenclature item;");
            return null;
        } else {
            return optionalDistrict.get().getId();
        }
    }

    /**
     * <h1>Get Zip Code Id</h1>
     * function select zipCode info based on zip code id and populatedPlaceId ,
     * if record is empty  checks if customer is Potential and adds error message
     * else gets zipcode from db and returns it
     *
     * @param request
     * @param populatedPlaceId
     * @param statuses
     * @param exceptionMessages
     * @return
     */
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
                    "address.localAddressData.zipCodeId-Zip code not found in entered populated place;",
                    exceptionMessages
            );
            return null;
        } else {
            return optionalZipCode.get();
        }
    }

    /**
     * <h1>Get Zip Code Id For edit customer</h1>
     * function select zipCode info based on zip code id and populatedPlaceId ,
     * if record is empty  checks if customer is Potential and adds error message
     * else gets zipcode from db and returns it
     *
     * @param request
     * @param populatedPlaceId
     * @param dbZipCode
     * @param statuses
     * @param exceptionMessages
     * @return
     */
    private ZipCode getZipCode(EditCustomerRequest request,
                               Long populatedPlaceId,
                               ZipCode dbZipCode,
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
                    "address.localAddressData.zipCodeId-Zip code not found in entered populated place;",
                    exceptionMessages
            );
            return null;
        } else if (optionalZipCode.get().getStatus().equals(NomenclatureItemStatus.INACTIVE)
                && (dbZipCode == null || !localAddressData.getZipCodeId().equals(dbZipCode.getId()))) {

            exceptionMessages.add("address.localAddressData.zipCodeId-Can't add INACTIVE nomenclature item;");
            return null;
        } else {
            return optionalZipCode.get();
        }
    }

    /**
     * <h1>Fill Foreign Address Fields</h1>
     * function fills {@link ForeignAddressData} object
     *
     * @param request
     * @param customerDetails
     * @param statuses
     * @param exceptionMessages
     */
    private void fillForeignAddressFields(CreateCustomerRequest request,
                                          CustomerDetails customerDetails,
                                          List<NomenclatureItemStatus> statuses,
                                          List<String> exceptionMessages) {
        ForeignAddressData foreignAddressData = request.getAddress().getForeignAddressData();
        if (foreignAddressData == null && request.getCustomerDetailStatus().equals(CustomerDetailStatus.POTENTIAL))
            return;
        customerDetails.setRegionForeign(foreignAddressData.getRegion());
        customerDetails.setMunicipalityForeign(foreignAddressData.getMunicipality());
        customerDetails.setPopulatedPlaceForeign(foreignAddressData.getPopulatedPlace());
        customerDetails.setZipCodeForeign(foreignAddressData.getZipCode());
        customerDetails.setDistrictForeign(foreignAddressData.getDistrict());
        customerDetails.setStreetTypeForeign(foreignAddressData.getStreetType());
        customerDetails.setStreetForeign(foreignAddressData.getStreet());
        customerDetails.setResidentialAreaTypeForeign(foreignAddressData.getResidentialAreaType());
        customerDetails.setResidentialAreaForeign(foreignAddressData.getResidentialArea());
        customerDetails.setCountryId(getCountryId(request, foreignAddressData.getCountryId(), statuses, exceptionMessages));
    }

    private void fillForeignAddressFieldsForNewVersion(CreateCustomerRequest request,
                                                       CustomerDetails customerDetails,
                                                       CustomerDetails customerDetailsCurrentVersion,
                                                       List<NomenclatureItemStatus> statuses,
                                                       List<String> exceptionMessages) {
        ForeignAddressData foreignAddressData = request.getAddress().getForeignAddressData();
        if (foreignAddressData == null && request.getCustomerDetailStatus().equals(CustomerDetailStatus.POTENTIAL))
            return;
        customerDetails.setRegionForeign(foreignAddressData.getRegion());
        customerDetails.setMunicipalityForeign(foreignAddressData.getMunicipality());
        customerDetails.setPopulatedPlaceForeign(foreignAddressData.getPopulatedPlace());
        customerDetails.setZipCodeForeign(foreignAddressData.getZipCode());
        customerDetails.setDistrictForeign(foreignAddressData.getDistrict());
        customerDetails.setStreetTypeForeign(foreignAddressData.getStreetType());
        customerDetails.setStreetForeign(foreignAddressData.getStreet());
        customerDetails.setResidentialAreaTypeForeign(foreignAddressData.getResidentialAreaType());
        customerDetails.setResidentialAreaForeign(foreignAddressData.getResidentialArea());
        customerDetails.setCountryId(getCountryIdForEdit(request, foreignAddressData.getCountryId(), customerDetailsCurrentVersion.getCountryId(), statuses, exceptionMessages));
    }

    /**
     * <h1>Edit Foreign Address Fields</h1>
     * function edits {@link ForeignAddressData} object
     *
     * @param request
     * @param customerDetails
     * @param statuses
     * @param exceptionMessages
     */
    private void editForeignAddressFields(EditCustomerRequest request,
                                          CustomerDetails customerDetails,
                                          List<NomenclatureItemStatus> statuses,
                                          List<String> exceptionMessages) {
        ForeignAddressData foreignAddressData = request.getAddress().getForeignAddressData();
        /*if (foreignAddressData == null && request.getCustomerDetailStatus().equals(CustomerDetailStatus.POTENTIAL))
            return;*/
        customerDetails.setRegionForeign(foreignAddressData.getRegion());
        customerDetails.setMunicipalityForeign(foreignAddressData.getMunicipality());
        customerDetails.setPopulatedPlaceForeign(foreignAddressData.getPopulatedPlace());
        customerDetails.setZipCodeForeign(foreignAddressData.getZipCode());
        customerDetails.setDistrictForeign(foreignAddressData.getDistrict());
        customerDetails.setStreetTypeForeign(foreignAddressData.getStreetType());
        customerDetails.setStreetForeign(foreignAddressData.getStreet());
        customerDetails.setResidentialAreaTypeForeign(foreignAddressData.getResidentialAreaType());
        customerDetails.setResidentialAreaForeign(foreignAddressData.getResidentialArea());
        customerDetails.setCountryId(getCountryIdForEdit(request, foreignAddressData.getCountryId(), customerDetails.getCountryId(), statuses, exceptionMessages));

        customerDetails.setStreetId(null);
        customerDetails.setResidentialAreaId(null);
        customerDetails.setDistrictId(null);
        customerDetails.setZipCode(null);
        customerDetails.setPopulatedPlaceId(null);
    }

    /**
     * <h1>Get Country Id</h1>
     * function selects {@link Country} info according to the {@link NomenclatureItemStatus} list
     * if record is empty  checks if customer is Potential and adds error message
     * else return countryId
     *
     * @param request
     * @param countryId
     * @param statuses
     * @param exceptionMessages
     * @return Long country id
     */
    private Long getCountryId(EditCustomerRequest request,
                              Long countryId,
                              List<NomenclatureItemStatus> statuses,
                              List<String> exceptionMessages) {
        Optional<Country> optionalCountry = countryRepository.findByIdAndStatus(countryId, statuses);
        if (optionalCountry.isEmpty()) {
            checkCustomerTypeAndAddExceptionMessage(
                    request,
                    countryId,
                    "countryId-Country not found;",
                    exceptionMessages
            );
            return null;
        } else {
            return optionalCountry.get().getId();
        }
    }

    /**
     * <h1>Get Country For Edit</h1>
     * function selects {@link Country} object from db
     * if object is empty or object is changed and status of new object is INACTIVE
     * it adds exception message to the exceptionMessages list
     * else returns the object id
     *
     * @param request
     * @param countryId
     * @param dbCountryId
     * @param statuses
     * @param exceptionMessages
     * @return
     */
    private Long getCountryIdForEdit(EditCustomerRequest request,
                                     Long countryId,
                                     Long dbCountryId,
                                     List<NomenclatureItemStatus> statuses,
                                     List<String> exceptionMessages) {
        Optional<Country> optionalCountry = countryRepository.findByIdAndStatus(countryId, statuses);
        if (optionalCountry.isEmpty()) {
            checkCustomerTypeAndAddExceptionMessage(
                    request,
                    countryId,
                    "countryId-Country not found;",
                    exceptionMessages
            );
            return null;
        } else if (optionalCountry.get().getStatus().equals(NomenclatureItemStatus.INACTIVE)
                && (!countryId.equals(dbCountryId))) {

            exceptionMessages.add("countryId-Can't add INACTIVE nomenclature item;");
            return null;
        } else {
            return optionalCountry.get().getId();
        }
    }

    private Long getCountryIdForEdit(CreateCustomerRequest request,
                                     Long countryId,
                                     Long dbCountryId,
                                     List<NomenclatureItemStatus> statuses,
                                     List<String> exceptionMessages) {
        Optional<Country> optionalCountry = countryRepository.findByIdAndStatus(countryId, statuses);
        if (optionalCountry.isEmpty()) {
            checkCustomerTypeAndAddExceptionMessage(
                    request,
                    countryId,
                    "countryId-Country not found;",
                    exceptionMessages
            );
            return null;
        } else if (optionalCountry.get().getStatus().equals(NomenclatureItemStatus.INACTIVE)
                && (!countryId.equals(dbCountryId))) {

            exceptionMessages.add("countryId-Can't add INACTIVE nomenclature item;");
            return null;
        } else {
            return optionalCountry.get().getId();
        }
    }

    /**
     * <h1>Get Country Id</h1>
     * function selects {@link Country} info according to the {@link NomenclatureItemStatus} list
     * if record is empty  checks if customer is Potential and adds error message
     * else return countryId
     *
     * @param request
     * @param countryId
     * @param statuses
     * @param exceptionMessages
     * @return Long country id
     */
    private Long getCountryId(CreateCustomerRequest request,
                              Long countryId,
                              List<NomenclatureItemStatus> statuses,
                              List<String> exceptionMessages) {
        Optional<Country> optionalCountry = countryRepository.findByIdAndStatus(countryId, statuses);
        if (optionalCountry.isEmpty()) {
            checkCustomerTypeAndAddExceptionMessage(
                    request,
                    countryId,
                    "countryId-Country not found;",
                    exceptionMessages
            );
            return null;
        } else {
            return optionalCountry.get().getId();
        }
    }

    /**
     * <h1>checkCustomerTypeAndAddExceptionMessage</h1>
     * if customer status is Potential function adds error message to the array of exceptionMessages
     *
     * @param request
     * @param objectId
     * @param message
     * @param exceptionMessages
     * @return Long country id
     */
    private void checkCustomerTypeAndAddExceptionMessage(CreateCustomerRequest request,
                                                         Long objectId,
                                                         String message,
                                                         List<String> exceptionMessages) {
        if (request.getCustomerDetailStatus() != CustomerDetailStatus.POTENTIAL
                || objectId != null) {
            exceptionMessages.add(message);
        }
    }

    /**
     * <h1>Customer editCustomerDetails</h1>
     * function will select customer details by customer id and version of the customer details
     * if this kind of version exists function will map request and db objects and save customer details to the database and return the object
     * else it will add error message to the exception massages
     *
     * @param customer
     * @param id
     * @param request
     * @param exceptionMessages
     * @return
     */
    public CustomerDetails editCustomerDetails(Customer customer, Long id, EditCustomerRequest request, List<String> exceptionMessages) {
        Optional<CustomerDetails> customerDetailsOptional = customerDetailsRepository.findByCustomerIdAndVersionId(id, request.getCustomerDetailsVersion());
        if (customerDetailsOptional.isPresent()) {
            CustomerDetails dbCustomerDetails = customerDetailsOptional.get();
            checkCustomerStatusChange(dbCustomerDetails.getStatus(), request.getCustomerDetailStatus(), exceptionMessages);
            isValidCustomerStatus(dbCustomerDetails, request.getCustomerDetailStatus(), exceptionMessages);
            checkForIfLocked(dbCustomerDetails);
            checkDetailsAccess(dbCustomerDetails);
            List<NomenclatureItemStatus> statuses = List.of(NomenclatureItemStatus.ACTIVE, NomenclatureItemStatus.INACTIVE);
            dbCustomerDetails.setOldCustomerNumbers(request.getOldCustomerNumber());
            dbCustomerDetails.setVatNumber(request.getVatNumber());
            dbCustomerDetails.setMainActivitySubject(request.getMainSubjectOfActivity());
            editCustomerFieldsByTypes(request, dbCustomerDetails, statuses, exceptionMessages);
            editAddressData(request, dbCustomerDetails, statuses, exceptionMessages);//start
            if (customer != null) dbCustomerDetails.setCustomerId(customer.getId());
            dbCustomerDetails.setVersionId(dbCustomerDetails.getVersionId());
            dbCustomerDetails.setMarketingCommConsent(request.getMarketingConsent());

            if (!dbCustomerDetails.getForeignEntityPerson().equals(request.getForeign()))
                exceptionMessages.add("foreign-Foreign Person value should match previos value");

            dbCustomerDetails.setForeignEntityPerson(request.getForeign());
            dbCustomerDetails.setPreferCommunicationInEnglish(request.isPreferCommunicationInEnglish());
            fillBankingDetailsFields(request.getBankingDetails(), dbCustomerDetails, statuses, exceptionMessages);
            dbCustomerDetails.setForeignAddress(request.getAddress().getForeign());
            dbCustomerDetails.setStatus(request.getCustomerDetailStatus());
            return dbCustomerDetails;
        } else {
            exceptionMessages.add("Customer Details does not exists;");
        }
        return null;
    }

    private void checkForIfLocked(CustomerDetails dbCustomerDetails) {
        Boolean isBound = customerDetailsRepository.checkForBoundObjects(dbCustomerDetails.getId());

        if (Boolean.TRUE.equals(isBound)) {
            if (!checkIfHasLockedPermission()) {
                throw new ClientException("You can't edit Customer because it is connected to the object is system;", ErrorCode.OPERATION_NOT_ALLOWED);
            }
        }
    }

    public boolean checkForIfLockedForPreview(CustomerDetails dbCustomerDetails) {
        Boolean isBound = customerDetailsRepository.checkForBoundObjects(dbCustomerDetails.getId());
        return Boolean.TRUE.equals(isBound);
    }

    private boolean checkIfHasLockedPermission() {
        List<String> customerContext = permissionService.getPermissionsFromContext(PermissionContextEnum.CUSTOMER);
        return customerContext.contains(PermissionEnum.CUSTOMER_EDIT_LOCKED.getId());
    }

    private void checkDetailsAccess(CustomerDetails customerDetails) {
        List<String> customerContext = permissionService.getPermissionsFromContext(PermissionContextEnum.CUSTOMER);
        if (customerContext.contains(PermissionEnum.CUSTOMER_EDIT_AM.getId()) && !customerContext.contains(PermissionEnum.CUSTOMER_EDIT.getId())) {
            if (!accountManagerService.checkCustomerDetailsAccess(customerDetails, permissionService.getLoggedInUserId())) {
                throw new ClientException("id-You need to be account manager for this customer to be able to edit;", ErrorCode.ACCESS_DENIED);
            }
        }
    }

    /**
     * <h1>Edit Customer Fields By Types</h1>
     * if customerType is legal entity function calls
     * {@link #editBusinessRelatedFields(EditCustomerRequest, CustomerDetails, List, List)},
     * also sets name and nameTransliterated of customer.
     * else if customer type is private customer with business activity function calls:
     * {@link #editBusinessRelatedFields(EditCustomerRequest, CustomerDetails, List, List)}
     * and {@link #editPrivateCustomerFields(EditCustomerRequest, CustomerDetails)}
     * else {@link #editPrivateCustomerFields(EditCustomerRequest, CustomerDetails)}
     *
     * @param request
     * @param customerDetails
     * @param statuses
     * @param exceptionMessages
     */
    private void editCustomerFieldsByTypes(EditCustomerRequest request, CustomerDetails customerDetails, List<NomenclatureItemStatus> statuses, List<String> exceptionMessages) {
        if (request.getCustomerType() == CustomerType.LEGAL_ENTITY) {
            editBusinessRelatedFields(request, customerDetails, statuses, exceptionMessages);
            customerDetails.setName(request.getBusinessCustomerDetails().getName());
            customerDetails.setNameTransl(request.getBusinessCustomerDetails().getNameTranslated());
            customerDetails.setBusinessActivity(false);

        } else {
            if (request.getBusinessActivity()) {
                editBusinessRelatedFields(request, customerDetails, statuses, exceptionMessages);
                customerDetails.setBusinessActivityName(request.getBusinessCustomerDetails().getName());
                customerDetails.setBusinessActivityNameTransl(request.getBusinessCustomerDetails().getNameTranslated());
            } else setNullsToBusinessRelatedFields(customerDetails);
            customerDetails.setBusinessActivity(request.getBusinessActivity());
            editPrivateCustomerFields(request, customerDetails);
        }
    }

    private void setNullsToBusinessRelatedFields(CustomerDetails customerDetails) {
        customerDetails.setPublicProcurementLaw(false);
        customerDetails.setLegalFormId(null);
        customerDetails.setLegalFormTranslId(null);
        customerDetails.setOwnershipFormId(null);
        customerDetails.setEconomicBranchCiId(null);
        customerDetails.setEconomicBranchNceaId(null);
    }

    /**
     * <h1>Edit Private Customer Fields</h1>
     * before editing private customer fields function checks if field is under GDPR permission
     * if not changes the field value
     *
     * @param request
     * @param customerDetails
     */
    private void editPrivateCustomerFields(EditCustomerRequest request,
                                           CustomerDetails customerDetails) {
        PrivateCustomerDetails privateCustomerDetails = request.getPrivateCustomerDetails();
        if (!StringUtils.equals(privateCustomerDetails.getFirstName(), EPBFinalFields.GDPR)) {
            customerDetails.setName(privateCustomerDetails.getFirstName());
        }
        if (!StringUtils.equals(privateCustomerDetails.getFirstNameTranslated(), EPBFinalFields.GDPR)) {
            customerDetails.setNameTransl(privateCustomerDetails.getFirstNameTranslated());
        }
        if (!StringUtils.equals(privateCustomerDetails.getMiddleName(), EPBFinalFields.GDPR)) {
            customerDetails.setMiddleName(privateCustomerDetails.getMiddleName());
        }
        if (!StringUtils.equals(privateCustomerDetails.getMiddleNameTranslated(), EPBFinalFields.GDPR)) {
            customerDetails.setMiddleNameTransl(privateCustomerDetails.getMiddleNameTranslated());
        }
        if (!StringUtils.equals(privateCustomerDetails.getLastName(), EPBFinalFields.GDPR)) {
            customerDetails.setLastName(privateCustomerDetails.getLastName());
        }
        if (!StringUtils.equals(privateCustomerDetails.getLastNameTranslated(), EPBFinalFields.GDPR)) {
            customerDetails.setLastNameTransl(privateCustomerDetails.getLastNameTranslated());
        }
        customerDetails.setGdprRegulationConsent(privateCustomerDetails.getGdprRegulationConsent());
    }

    /**
     * <h1>Edit Business Related Fields</h1>
     * function sets procurment law ,
     * legalForm nomenclature id,
     * legalForm transliterated nomenclature id,
     * ownership id ,
     * economicbranchId and economicBanchNCEAid
     *
     * @param request
     * @param customerDetails
     * @param statuses
     * @param exceptionMessages
     */
    private void editBusinessRelatedFields(EditCustomerRequest request,
                                           CustomerDetails customerDetails,
                                           List<NomenclatureItemStatus> statuses,
                                           List<String> exceptionMessages) {
        BusinessCustomerDetails businessCustomerDetails = request.getBusinessCustomerDetails();
        customerDetails.setPublicProcurementLaw(businessCustomerDetails.getProcurementLaw());
        if (businessCustomerDetails.getLegalFormId() != null) {
            customerDetails.setLegalFormId(
                    getLegalFormIdForEdit(businessCustomerDetails, customerDetails.getLegalFormId(), statuses, exceptionMessages)
            );
            customerDetails.setLegalFormTranslId(
                    getLegalFormTranslIdForEdit(businessCustomerDetails, customerDetails.getLegalFormTranslId(), statuses, exceptionMessages)
            );
        } else {
            customerDetails.setLegalFormId(null);
            customerDetails.setLegalFormTranslId(null);
        }
        customerDetails.setOwnershipFormId(
                getOwnershipFormIdForEdit(request, customerDetails.getOwnershipFormId(), statuses, exceptionMessages)
        );
        customerDetails.setEconomicBranchCiId(
                getEconomicBranchCIId(request, customerDetails.getEconomicBranchCiId(), statuses, exceptionMessages)
        );
        customerDetails.setEconomicBranchNceaId(
                getEconomicBranchNCEAId(request, customerDetails.getEconomicBranchNceaId(), statuses, exceptionMessages)
        );

    }

    /**
     * <h1>Get Economic Branch NCEAId</h1>
     * function selects {@link EconomicBranchNCEA} object from the db according to economicBranchNCEAId and
     * {@link NomenclatureItemStatus} statuses array.
     * if there is no data  and customer status is Potential function adds error message to the array of exceptionMessages
     * else returns economicBranchNCEAId
     *
     * @param request
     * @param statuses
     * @param exceptionMessages
     * @return Long economicBranchNCEAId
     */
    private Long getEconomicBranchNCEAId(EditCustomerRequest request,
                                         Long dbEconomicBranchNCEAId,
                                         List<NomenclatureItemStatus> statuses,
                                         List<String> exceptionMessages) {
        Optional<EconomicBranchNCEA> optionalEconomicBranchNCEA = economicBranchNCEARepository
                .findByIdAndStatus(request.getEconomicBranchNCEAId(), statuses);
        if (optionalEconomicBranchNCEA.isEmpty()) {
            if (request.getEconomicBranchNCEAId() != null) {
                exceptionMessages.add("economicBranchNCEAId-Economic Branch NCEA not found;");
            }
            return null;
        } else if (optionalEconomicBranchNCEA.get().getStatus().equals(NomenclatureItemStatus.INACTIVE)
                && (!request.getEconomicBranchNCEAId().equals(dbEconomicBranchNCEAId))) {

            exceptionMessages.add("economicBranchNCEAId-Can't add INACTIVE nomenclature item;");
            return null;
        } else {
            return optionalEconomicBranchNCEA.get().getId();
        }
    }

    private Long getEconomicBranchNCEAId(CreateCustomerRequest request,
                                         Long dbEconomicBranchNCEAId,
                                         List<NomenclatureItemStatus> statuses,
                                         List<String> exceptionMessages) {
        Optional<EconomicBranchNCEA> optionalEconomicBranchNCEA = economicBranchNCEARepository
                .findByIdAndStatus(request.getEconomicBranchNCEAId(), statuses);
        if (optionalEconomicBranchNCEA.isEmpty()) {
            if (request.getEconomicBranchNCEAId() != null) {
                exceptionMessages.add("economicBranchNCEAId-Economic Branch NCEA not found;");
            }
            return null;
        } else if (optionalEconomicBranchNCEA.get().getStatus().equals(NomenclatureItemStatus.INACTIVE)
                && (!request.getEconomicBranchNCEAId().equals(dbEconomicBranchNCEAId))) {

            exceptionMessages.add("economicBranchNCEAId-Can't add INACTIVE nomenclature item;");
            return null;
        } else {
            return optionalEconomicBranchNCEA.get().getId();
        }
    }

    /**
     * <h1>Get Economic Branch CI Id</h1>
     * function selects {@link EconomicBranchCI} object from the db according to economicBranchCIId and
     * {@link NomenclatureItemStatus} statuses array.
     * if there is no data  and customer status is Potential function adds error message to the array of exceptionMessages
     * else returns economicBranchCIId
     *
     * @param request
     * @param statuses
     * @param exceptionMessages
     * @return Long economicBranchCIId
     */
    private Long getEconomicBranchCIId(EditCustomerRequest request,
                                       Long dbEconomicBranchCIId,
                                       List<NomenclatureItemStatus> statuses,
                                       List<String> exceptionMessages) {
        Optional<EconomicBranchCI> optionalEconomicBranchCI = economicBranchCIRepository
                .findByIdAndStatus(request.getEconomicBranchId(), statuses);
        if (optionalEconomicBranchCI.isEmpty()) {
            checkCustomerTypeAndAddExceptionMessage(request,
                    request.getEconomicBranchId(),
                    "economicBranchId-Economic Branch CI not found;",
                    exceptionMessages);
            return null;
        } else if (optionalEconomicBranchCI.get().getStatus().equals(NomenclatureItemStatus.INACTIVE)
                && (!request.getEconomicBranchId().equals(dbEconomicBranchCIId))) {

            exceptionMessages.add("economicBranchId-Can't add INACTIVE nomenclature item;");
            return null;
        } else {
            return optionalEconomicBranchCI.get().getId();
        }
    }

    private Long getEconomicBranchCIId(CreateCustomerRequest request,
                                       Long dbEconomicBranchCIId,
                                       List<NomenclatureItemStatus> statuses,
                                       List<String> exceptionMessages) {
        Optional<EconomicBranchCI> optionalEconomicBranchCI = economicBranchCIRepository
                .findByIdAndStatus(request.getEconomicBranchId(), statuses);
        if (optionalEconomicBranchCI.isEmpty()) {
            checkCustomerTypeAndAddExceptionMessage(request,
                    request.getEconomicBranchId(),
                    "economicBranchId-Economic Branch CI not found;",
                    exceptionMessages);
            return null;
        } else if (optionalEconomicBranchCI.get().getStatus().equals(NomenclatureItemStatus.INACTIVE)
                && (!request.getEconomicBranchId().equals(dbEconomicBranchCIId))) {

            exceptionMessages.add("economicBranchId-Can't add INACTIVE nomenclature item;");
            return null;
        } else {
            return optionalEconomicBranchCI.get().getId();
        }
    }

    /**
     * <h1>checkCustomerTypeAndAddExceptionMessage</h1>
     * if customer status is Potential function adds error message to the array of exceptionMessages
     *
     * @param request
     * @param objectId
     * @param message
     * @param exceptionMessages
     */
    private void checkCustomerTypeAndAddExceptionMessage(EditCustomerRequest request,
                                                         Long objectId,
                                                         String message,
                                                         List<String> exceptionMessages) {
        if (request.getCustomerDetailStatus() != CustomerDetailStatus.POTENTIAL
                || objectId != null) {
            exceptionMessages.add(message);
        }
    }

    /**
     * <h1>Get Ownership Form Id For Edit</h1>
     * function selects {@link OwnershipForm} from db
     * if there is no data  and customer status is Potential function adds error message to the array of exceptionMessages
     * else return id
     *
     * @param request
     * @param statuses
     * @param exceptionMessages
     * @return Long id
     */
    private Long getOwnershipFormIdForEdit(EditCustomerRequest request,
                                           Long dbOwnershipFormId,
                                           List<NomenclatureItemStatus> statuses,
                                           List<String> exceptionMessages) {
        Optional<OwnershipForm> optionalOwnershipForm = ownershipFormRepository
                .findByIdAndStatus(request.getOwnershipFormId(), statuses);
        if (optionalOwnershipForm.isEmpty()) {
            checkEditCustomerTypeAndAddExceptionMessage(request,
                    request.getOwnershipFormId(),
                    "ownershipFormId-Ownership form not found;",
                    exceptionMessages);
            return null;
        } else if (optionalOwnershipForm.get().getStatus().equals(NomenclatureItemStatus.INACTIVE)
                && (!request.getOwnershipFormId().equals(dbOwnershipFormId))) {

            exceptionMessages.add("ownershipFormId-Can't add INACTIVE nomenclature item;");
            return null;
        } else {
            return optionalOwnershipForm.get().getId();
        }
    }

    private Long getOwnershipFormIdForEdit(CreateCustomerRequest request,
                                           Long dbOwnershipFormId,
                                           List<NomenclatureItemStatus> statuses,
                                           List<String> exceptionMessages) {
        Optional<OwnershipForm> optionalOwnershipForm = ownershipFormRepository
                .findByIdAndStatus(request.getOwnershipFormId(), statuses);
        if (optionalOwnershipForm.isEmpty()) {
            checkCustomerTypeAndAddExceptionMessage(request,
                    request.getOwnershipFormId(),
                    "ownershipFormId-Ownership form not found;",
                    exceptionMessages);
            return null;
        } else if (optionalOwnershipForm.get().getStatus().equals(NomenclatureItemStatus.INACTIVE)
                && (!request.getOwnershipFormId().equals(dbOwnershipFormId))) {

            exceptionMessages.add("ownershipFormId-Can't add INACTIVE nomenclature item;");
            return null;
        } else {
            return optionalOwnershipForm.get().getId();
        }
    }

    /**
     * <h1>checkEditCustomerTypeAndAddExceptionMessage</h1>
     * if customer status is Potential function adds error message to the array of exceptionMessages
     *
     * @param request
     * @param objectId
     * @param message
     * @param exceptionMessages
     */
    private void checkEditCustomerTypeAndAddExceptionMessage(EditCustomerRequest request,
                                                             Long objectId,
                                                             String message,
                                                             List<String> exceptionMessages) {
        if (request.getCustomerDetailStatus() != CustomerDetailStatus.POTENTIAL
                || objectId != null) {
            exceptionMessages.add(message);
        }
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
        dbCustomerDetails.setStreetType(editCustomerDetails.getStreetType());
        dbCustomerDetails.setResidentialAreaId(editCustomerDetails.getResidentialAreaId());
        dbCustomerDetails.setResidentialAreaType(editCustomerDetails.getResidentialAreaType());
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
        return dbCustomerDetails;
    }

    private String checkGDPRName(String text, CustomerDetails dbCustomerDetails, Customer customer, GDPRCustomerFields gdprCustomerFields) {
        if (text == null) {
            return "";
        }
        if (text.equals(EPBFinalFields.GDPR)) {
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

    /**
     * <h1>Get Versions</h1>
     *
     * @param customerId
     * @param customerDetailStatuses
     * @return {@link CustomerVersionsResponse}
     */
    public List<CustomerVersionsResponse> getVersions(Long customerId, List<CustomerDetailStatus> customerDetailStatuses) {
        return customerDetailsRepository.getVersions(customerId, customerDetailStatuses);
    }

    /**
     * <h1>Find By Customer Id And VersionId</h1>
     * function selects {@link CustomerDetails} object from db with customer id and version
     *
     * @param id
     * @param version
     * @return {@link CustomerDetails}
     */
    public Optional<CustomerDetails> findByCustomerIdAndVersionId(Long id, Long version) {
        return customerDetailsRepository.findByCustomerIdAndVersionId(id, version);
    }

    /**
     * <h1>Find First By Customer Id</h1>
     *
     * @param id
     * @param createDate
     * @return {@link CustomerDetails}
     */
    public Optional<CustomerDetails> findFirstByCustomerId(Long id, Sort createDate) {
        return customerDetailsRepository.findFirstByCustomerId(id, createDate);
    }

    /**
     * <h1>findCustomerInfo</h1>
     *
     * @param identifier
     * @param types
     * @return {@link CustomerShortResponse}
     */
    public CustomerShortResponse findCustomerInfo(String identifier, List<CustomerType> types) {
        return customerDetailsRepository
                .findFirstByCustomerIdentifierAndStatus(
                        identifier,
                        List.of(CustomerStatus.ACTIVE),
                        types,
                        PageRequest.of(0, 1)
                )
                .orElseThrow(() -> new DomainEntityNotFoundException("identifier-Customer details not found;"));
    }

    public void isValidCustomerStatus(CustomerDetails oldCustomerDetails, CustomerDetailStatus status, List<String> errorMessages) {
        CustomerDetailStatus oldStatus = oldCustomerDetails.getStatus();
        if (status.equals(oldStatus)) {
            return;
        }

        if (oldStatus.equals(CustomerDetailStatus.ENDED) && !List.of(CustomerDetailStatus.POTENTIAL, CustomerDetailStatus.NEW).contains(status)) {
            if (customerDetailsRepository.customerHasActiveContractsOrOrders(oldCustomerDetails.getId())) {
                errorMessages.add("customerDetailStatus-Customer status can not be changed from ended!;");
            }
        }

    }

    public void checkCustomerStatusChange(CustomerDetailStatus oldStatus, CustomerDetailStatus status, List<String> errorMessages) {
        if (oldStatus.equals(status)) {
            return;
        }
        if (List.of(CustomerDetailStatus.ACTIVE, CustomerDetailStatus.LOST).contains(oldStatus) && !status.equals(CustomerDetailStatus.ENDED)) {
            errorMessages.add("customerDetailStatus-Customer details status can only changed to ENDED!;");
            return;
        }

        if (oldStatus.equals(CustomerDetailStatus.ENDED) && !List.of(CustomerDetailStatus.NEW, CustomerDetailStatus.POTENTIAL).contains(status)) {
            errorMessages.add("customerDetailStatus-Customer details status can only changed to NEW or POTENTIAL!;");
        }

    }


}
