package bg.energo.phoenix.service.massImport;

import bg.energo.phoenix.model.CacheObject;
import bg.energo.phoenix.model.CacheObjectForBank;
import bg.energo.phoenix.model.entity.customer.Customer;
import bg.energo.phoenix.model.enums.customer.*;
import bg.energo.phoenix.model.enums.nomenclature.NomenclatureItemStatus;
import bg.energo.phoenix.model.enums.nomenclature.ResidentialAreaType;
import bg.energo.phoenix.model.enums.nomenclature.StreetType;
import bg.energo.phoenix.model.request.customer.*;
import bg.energo.phoenix.model.request.customer.communicationData.*;
import bg.energo.phoenix.model.request.customer.communicationData.communicationContact.CreateCommunicationContactRequest;
import bg.energo.phoenix.model.request.customer.communicationData.communicationContact.EditCommunicationContactRequest;
import bg.energo.phoenix.model.request.customer.communicationData.contactPerson.CreateContactPersonRequest;
import bg.energo.phoenix.model.request.customer.communicationData.contactPerson.EditContactPersonRequest;
import bg.energo.phoenix.model.request.customer.communicationData.contactPurpose.CreateContactPurposeRequest;
import bg.energo.phoenix.model.request.customer.communicationData.contactPurpose.EditContactPurposeRequest;
import bg.energo.phoenix.model.request.customer.customerAccountManager.CreateCustomerAccountManagerRequest;
import bg.energo.phoenix.model.request.customer.customerAccountManager.EditCustomerAccountManagerRequest;
import bg.energo.phoenix.model.request.customer.manager.CreateManagerRequest;
import bg.energo.phoenix.model.request.customer.manager.EditManagerRequest;
import bg.energo.phoenix.model.request.customer.relatedCustomer.CreateRelatedCustomerRequest;
import bg.energo.phoenix.model.request.customer.relatedCustomer.EditRelatedCustomerRequest;
import bg.energo.phoenix.repository.customer.AccountManagerRepository;
import bg.energo.phoenix.repository.customer.CustomerRepository;
import bg.energo.phoenix.repository.nomenclature.address.*;
import bg.energo.phoenix.repository.nomenclature.customer.*;
import bg.energo.phoenix.repository.nomenclature.customer.legalForm.LegalFormRepository;
import bg.energo.phoenix.repository.nomenclature.customer.legalForm.LegalFormTransliteratedRepository;
import bg.energo.phoenix.util.mi.Counter;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.time.LocalDate.from;

@Service
@RequiredArgsConstructor
public class ExcelMapper {

    private final CustomerRepository customerRepository;
    private final PopulatedPlaceRepository populatedPlaceRepository;
    private final OwnershipFormRepository ownershipFormRepository;
    private final EconomicBranchCIRepository economicBranchCIRepository;
    private final EconomicBranchNCEARepository economicBranchNCEARepository;
    private final SegmentRepository segmentRepository;
    private final LegalFormRepository legalFormRepository;
    private final LegalFormTransliteratedRepository legalFormTransliteratedRepository;

    private final CountryRepository countryRepository;
    private final DistrictRepository districtRepository;
    private final MunicipalityRepository municipalityRepository;
    private final RegionRepository regionRepository;
    private final ResidentialAreaRepository residentialAreaRepository;
    private final StreetRepository streetRepository;
    private final ZipCodeRepository zipCodeRepository;
    private final BankRepository bankRepository;
    private final CreditRatingRepository creditRatingRepository;
    private final AccountManagerTypeRepository accountManagerTypeRepository;
    private final BelongingCapitalOwnerRepository belongingCapitalOwnerRepository;
    private final CiConnectionTypeRepository ciConnectionTypeRepository;
    private final ContactPurposeRepository contactPurposeRepository;
    private final PlatformRepository platformRepository;
    private final RepresentationMethodRepository representationMethodRepository;
    private final TitleRepository titleRepository;
    private final PreferencesRepository preferencesRepository;
    private final AccountManagerRepository accountManagerRepository;

    public EditCustomerRequest convertToEditCustomerRequest(
            EditCustomerRequest request, Row row,
            List<String> errorMessages
    ) {

        setEditCustomerRequestFields(request, row, errorMessages);
        setSubObjects(request, row, errorMessages);

        return request;
    }

    private void setSubObjects(EditCustomerRequest request, Row row, List<String> errorMessages) {
        setManagers(request, row, errorMessages);
        setRelatedCustomers(request, row, errorMessages);
        setOwners(request, row, errorMessages);
        setCommunicationData(request, row, errorMessages);
        setAccountManagers(request, row, errorMessages);
    }

    private void setEditCustomerRequestFields(EditCustomerRequest request, Row row, List<String> errorMessages) {
        String type = getStringValue(2, row);
        if (type != null) request.setCustomerType(CustomerType.valueOf(type));

        String businessActivity = getStringValue(3, row);
        if (businessActivity != null) {
            request.setBusinessActivity(getBoolean(businessActivity, errorMessages, "businessActivity"));
        } else {
            request.setBusinessActivity(null);
        }

        String identifier = getStringValue(8, row);
        if (identifier != null) request.setCustomerIdentifier(identifier);

        String foreign = getStringValue(6, row);
        if (foreign != null) request.setForeign(getBoolean(foreign, errorMessages, "foreign"));

        String marketingConsent = getStringValue(5, row);
        if (marketingConsent != null)
            request.setMarketingConsent(getBoolean(marketingConsent, errorMessages, "marketingConsent"));

        String preferCommunicationInEnglish = getStringValue(365, row);
        if (preferCommunicationInEnglish != null)
            request.setPreferCommunicationInEnglish(Boolean.TRUE.equals(getBoolean(
                    preferCommunicationInEnglish,
                    errorMessages,
                    "preferCommunicationInEnglish"
            )));

        String vatNumber = getStringValue(11, row);
        if (vatNumber != null) request.setVatNumber(vatNumber);

        String customerDetails = getStringValue(9, row);
        if (customerDetails != null) request.setCustomerDetailStatus(CustomerDetailStatus.valueOf(customerDetails));

        String ownershipFormName = getStringValue(22, row);
        if (ownershipFormName != null) {
            Optional<CacheObject> optionalOwnershipForm = ownershipFormRepository.findByNameAndStatus(
                    ownershipFormName,
                    NomenclatureItemStatus.ACTIVE
            );
            if (optionalOwnershipForm.isPresent()) request.setOwnershipFormId(optionalOwnershipForm.get().getId());
            else errorMessages.add("ownershipFormName-Not found ownership form with name: " + ownershipFormName + ";");

        }

        String economicBranchCI = getStringValue(23, row);
        if (economicBranchCI != null) {
            Optional<CacheObject> optionalEconomicBranchCI = economicBranchCIRepository.findByNameAndStatus(
                    economicBranchCI,
                    NomenclatureItemStatus.ACTIVE
            );
            if (optionalEconomicBranchCI.isPresent())
                request.setEconomicBranchId(optionalEconomicBranchCI.get().getId());
            else
                errorMessages.add("economicBranchCI-Not found economic branch CI with name: " + economicBranchCI + ";");
        }


        String economicBranchNCEA = getStringValue(24, row);
        if (economicBranchNCEA != null) {
            Optional<CacheObject> optionalEconomicBranchNCEA = economicBranchNCEARepository.findByNameAndStatus(
                    getStringValue(24, row),
                    NomenclatureItemStatus.ACTIVE
            );
            if (optionalEconomicBranchNCEA.isPresent())
                request.setEconomicBranchNCEAId(optionalEconomicBranchNCEA.get().getId());
            else
                errorMessages.add("economicBranchNCEA-Not found economic branch NCEA with name: " + economicBranchNCEA + ";");
        }


        String mainSubjectOfActivity = getStringValue(28, row);
        if (mainSubjectOfActivity != null) request.setMainSubjectOfActivity(mainSubjectOfActivity);

        String oldCustomerNumber = getStringValue(10, row);
        if (oldCustomerNumber != null) request.setOldCustomerNumber(oldCustomerNumber);

        CustomerAddressRequest customerAddress = getCustomerAddressRequestForEditRequest(
                request.getAddress(),
                29,
                row,
                errorMessages
        );
        if (customerAddress != null) request.setAddress(customerAddress);

        CustomerBankingDetails bankingDetails = getCustomerBankingDetailsForEditRequest(
                request.getBankingDetails(),
                54,
                row,
                errorMessages
        );
        if (bankingDetails != null) request.setBankingDetails(bankingDetails);

        setSegmentIds(request, row, errorMessages);
        setCustomerDetails(request, row, errorMessages);
    }

    private CustomerBankingDetails getCustomerBankingDetailsForEditRequest(
            CustomerBankingDetails bankingDetails, int columnNumber, Row row,
            List<String> errorMessages
    ) {
        CustomerBankingDetails temp = Objects.requireNonNullElseGet(bankingDetails, CustomerBankingDetails::new);
        Counter counter = new Counter(0);

        Boolean debitCredit = getBooleanValue(columnNumber, row, counter, errorMessages, "directDebit");
        if (debitCredit != null) temp.setDirectDebit(debitCredit);

        setBankIdAndBankBic(temp, columnNumber, row, counter);

        String iban = getStringValue(columnNumber + 2, row, counter);
        if (iban != null) temp.setIban(iban);

        String declaredConsumption = getStringValueDeclaredConsumption(columnNumber + 3, row, counter);
        if (declaredConsumption != null) temp.setDeclaredConsumption(declaredConsumption);

        List<Long> preferenceIds = getPreferenceIds(columnNumber + 4, row, errorMessages);
        if (!preferenceIds.isEmpty()) {
            if (temp.getPreferenceIds() != null) {
                List<Long> mergedList = Stream.concat(temp.getPreferenceIds().stream(), preferenceIds.stream()).collect(
                        Collectors.toList());
                temp.setPreferenceIds(mergedList);
            } else {
                temp.setPreferenceIds(preferenceIds);
            }
            counter.setCount(counter.getCount() + 1);
        }

        addCreditRating(columnNumber + 7, row, counter, temp, errorMessages);

        return counter.getCount() > 0 ? temp : bankingDetails;
    }

    private void addCreditRating(
            int columnNumber, Row row, Counter counter,
            CustomerBankingDetails customerBankingDetails,
            List<String> errorMessages
    ) {
        String creditRatingName = getStringValue(columnNumber, row, counter);
        if (creditRatingName != null) {
            Optional<CacheObject> optionalCreditRating = creditRatingRepository.getByNameAndStatus(
                    creditRatingName,
                    NomenclatureItemStatus.ACTIVE
            );
            if (optionalCreditRating.isPresent())
                customerBankingDetails.setCreditRatingId(optionalCreditRating.get().getId());
            else errorMessages.add("creditRatingName-Not found credit rating with name: " + creditRatingName + ";");
        }
    }

    private void setAccountManagers(EditCustomerRequest request, Row row, List<String> errorMessages) {
        List<EditCustomerAccountManagerRequest> accountManagers = getEditCustomerAccountManagerRequests(
                110,
                row,
                errorMessages
        );
        if (!accountManagers.isEmpty()) {
            if (request.getAccountManagers() != null) {
                List<EditCustomerAccountManagerRequest> mergedList = Stream.concat(
                        request.getAccountManagers().stream(),
                        accountManagers.stream()
                ).collect(Collectors.toList());
                request.setAccountManagers(mergedList);
            } else request.setAccountManagers(accountManagers);
        }
    }

    private void setCommunicationData(EditCustomerRequest request, Row row, List<String> errorMessages) {
        List<EditCustomerCommunicationsRequest> communicationData = getEditCommunicationDataRequests(
                116,
                row,
                errorMessages
        );
        if (!communicationData.isEmpty()) {
            if (request.getCommunicationData() != null) {
                List<EditCustomerCommunicationsRequest> mergedList = Stream.concat(
                        request.getCommunicationData().stream(),
                        communicationData.stream()
                ).collect(Collectors.toList());
                request.setCommunicationData(mergedList);
            } else request.setCommunicationData(communicationData);
        }
    }

    private void setOwners(EditCustomerRequest request, Row row, List<String> errorMessages) {
        List<CustomerOwnerEditRequest> owners = getCustomerOwnerEditRequests(68, row, errorMessages);
        if (!owners.isEmpty()) {
            if (request.getOwner() != null) {
                List<CustomerOwnerEditRequest> mergedList = Stream.concat(
                        request.getOwner().stream(),
                        owners.stream()
                ).collect(Collectors.toList());
                request.setOwner(mergedList);
            } else request.setOwner(owners);
        }
    }

    private void setRelatedCustomers(EditCustomerRequest request, Row row, List<String> errorMessages) {
        List<EditRelatedCustomerRequest> relatedCustomers = getEditRelatedCustomerRequests(62, row, errorMessages);
        if (!relatedCustomers.isEmpty()) {
            if (request.getRelatedCustomers() != null) {
                List<EditRelatedCustomerRequest> mergedList = Stream.concat(
                        request.getRelatedCustomers().stream(),
                        relatedCustomers.stream()
                ).collect(Collectors.toList());
                request.setRelatedCustomers(mergedList);
            } else request.setRelatedCustomers(relatedCustomers);
        }
    }

    private void setManagers(EditCustomerRequest request, Row row, List<String> errorMessages) {
        List<EditManagerRequest> managers = getEditManagerRequests(77, row, errorMessages);
        if (!managers.isEmpty()) {
            if (request.getManagers() != null) {
                List<EditManagerRequest> mergedList = Stream.concat(
                        request.getManagers().stream(),
                        managers.stream()
                ).collect(Collectors.toList());
                request.setManagers(mergedList);
            } else request.setManagers(managers);
        }
    }

    private void setCustomerDetails(EditCustomerRequest editCustomerRequest, Row row, List<String> errorMessages) {
        if (editCustomerRequest.getCustomerType().equals(CustomerType.LEGAL_ENTITY)) {
            BusinessCustomerDetails customerDetails = getBusinessCustomerDetails(
                    editCustomerRequest.getBusinessCustomerDetails(),
                    row,
                    errorMessages,
                    editCustomerRequest.getCustomerType(),
                    editCustomerRequest.getBusinessActivity()
            );
            if (customerDetails != null)
                editCustomerRequest.setBusinessCustomerDetails(customerDetails);

            editCustomerRequest.setPrivateCustomerDetails(null);
            editCustomerRequest.setPrivateCustomerDetails(getPrivateCustomerDetails(
                    editCustomerRequest.getPrivateCustomerDetails(),
                    row,
                    editCustomerRequest.getCustomerType(),
                    errorMessages
            ));
        } else {
            if (editCustomerRequest.getBusinessActivity()) {
                PrivateCustomerDetails privateCustomerDetails = getPrivateCustomerDetails(
                        editCustomerRequest.getPrivateCustomerDetails(),
                        row,
                        editCustomerRequest.getCustomerType(),
                        errorMessages
                );
                if (privateCustomerDetails != null)
                    editCustomerRequest.setPrivateCustomerDetails(privateCustomerDetails);
                BusinessCustomerDetails customerDetails = getBusinessCustomerDetails(
                        editCustomerRequest.getBusinessCustomerDetails(),
                        row,
                        errorMessages,
                        editCustomerRequest.getCustomerType(),
                        editCustomerRequest.getBusinessActivity()
                );
                if (customerDetails != null)
                    editCustomerRequest.setBusinessCustomerDetails(customerDetails);
            } else {
                PrivateCustomerDetails privateCustomerDetails = getPrivateCustomerDetails(
                        editCustomerRequest.getPrivateCustomerDetails(),
                        row,
                        editCustomerRequest.getCustomerType(),
                        errorMessages
                );
                if (privateCustomerDetails != null)
                    editCustomerRequest.setPrivateCustomerDetails(privateCustomerDetails);
                editCustomerRequest.setBusinessCustomerDetails(null);
            }
        }
    }

    private PrivateCustomerDetails getPrivateCustomerDetails(
            PrivateCustomerDetails customerDetails,
            Row row,
            CustomerType customerType,
            List<String> errorMessages
    ) {
        PrivateCustomerDetails temp = customerDetails;
        if (customerDetails == null) temp = new PrivateCustomerDetails();
        Counter counter = new Counter(0);

        Boolean gdprRegulationConsent = getBooleanValue(7, row, counter, errorMessages, "gdprRegulationConsent");
        if (gdprRegulationConsent != null) temp.setGdprRegulationConsent(gdprRegulationConsent);

        if (!customerType.equals(CustomerType.LEGAL_ENTITY)) {
            String firstName = getStringValue(12, row, counter);
            if (firstName != null) temp.setFirstName(firstName);

            String firstNameTrans = getStringValue(13, row, counter);
            if (firstNameTrans != null) temp.setFirstNameTranslated(firstNameTrans);
        }

        String middleName = getStringValue(14, row, counter);
        if (middleName != null) temp.setMiddleName(middleName);

        String middleNameTrans = getStringValue(15, row, counter);
        if (middleNameTrans != null) temp.setMiddleNameTranslated(middleNameTrans);

        String lastName = getStringValue(16, row, counter);
        if (lastName != null) temp.setLastName(lastName);

        String lastNameTrans = getStringValue(17, row, counter);
        if (lastNameTrans != null) temp.setLastNameTranslated(lastNameTrans);

        return counter.getCount() > 0 ? temp : customerDetails;
    }

    private BusinessCustomerDetails getBusinessCustomerDetails(
            BusinessCustomerDetails customerDetails,
            Row row,
            List<String> errorMessages,
            CustomerType customerType,
            Boolean businessActivity
    ) {
        if (customerType.equals(CustomerType.LEGAL_ENTITY) || businessActivity) {
            Counter counter = new Counter(0);

            BusinessCustomerDetails temp = Objects.requireNonNullElseGet(customerDetails, BusinessCustomerDetails::new);

            Boolean procurementLaw = getBooleanValue(4, row, counter, errorMessages, "procurementLaw");
            if (procurementLaw != null) {
                temp.setProcurementLaw(procurementLaw);
            }

            String customerName = getStringValue(12, row, counter);
            if (customerName != null) {
                temp.setName(customerName);
            }

            String customerNameTranslated = getStringValue(13, row, counter);
            if (customerNameTranslated != null) {
                temp.setNameTranslated(customerNameTranslated);
            }

            addLegalForm(18, row, counter, temp, errorMessages);
            addLegalFormTransl(19, row, counter, temp, errorMessages);

            //TODO: change excel column index when businessActivityName will be removed from excel
            String businessActivityName = getStringValue(20, row, counter);
            if (businessActivityName != null) {
                temp.setName(businessActivityName);
            }

            //TODO: change excel column index when businessActivityName will be removed from excel
            String businessActivityNameTransliterated = getStringValue(21, row, counter);
            if (businessActivityNameTransliterated != null) {
                temp.setNameTranslated(businessActivityNameTransliterated);
            }

            return counter.getCount() > 0 ? temp : customerDetails;
        } else {
            return null;
        }
    }

    private void addLegalForm(
            int columnNumber,
            Row row,
            Counter counter,
            BusinessCustomerDetails businessCustomerDetails,
            List<String> errorMessages
    ) {
        String legalFormName = getStringValue(columnNumber, row, counter);
        if (legalFormName != null) {
            Optional<CacheObject> optionalLegalForm = legalFormRepository.findByNameAndStatus(
                    legalFormName,
                    NomenclatureItemStatus.ACTIVE
            );
            if (optionalLegalForm.isPresent()) businessCustomerDetails.setLegalFormId(optionalLegalForm.get().getId());
            else errorMessages.add("legalFormName-Not found legal form with name: " + legalFormName + ";");
        }
    }

    private void addLegalFormTransl(
            int columnNumber,
            Row row,
            Counter counter,
            BusinessCustomerDetails businessCustomerDetails,
            List<String> errorMessages
    ) {
        String legalFormTransName = getStringValue(columnNumber, row, counter);
        if (legalFormTransName != null) {
            Optional<CacheObject> optionalLegalFormTrans = legalFormTransliteratedRepository.findByNameAndStatus(
                    legalFormTransName,
                    NomenclatureItemStatus.ACTIVE
            );
            if (optionalLegalFormTrans.isPresent())
                businessCustomerDetails.setLegalFormTransId(optionalLegalFormTrans.get().getId());
            else
                errorMessages.add("legalFormTransName-Not found legal form transliterated with name: " + legalFormTransName + ";");
        }
    }

    private void setSegmentIds(EditCustomerRequest request, Row row, List<String> errorMessages) {
        List<Long> segmentIds = getSegmentIds(25, row, errorMessages);
        if (!segmentIds.isEmpty()) {
            if (request.getSegmentIds() != null) {
                List<Long> mergedList = Stream.concat(request.getSegmentIds().stream(), segmentIds.stream()).collect(
                        Collectors.toList());
                request.setSegmentIds(mergedList);
            } else request.setSegmentIds(segmentIds);
        }
    }

    private List<EditCustomerCommunicationsRequest> getEditCommunicationDataRequests(
            int columnNumber,
            Row row,
            List<String> errorMessages
    ) {
        List<EditCustomerCommunicationsRequest> communicationDataRequests = new ArrayList<>();
        addEditCommunicationDataRequest(columnNumber, row, communicationDataRequests, errorMessages);
        addEditCommunicationDataRequest(columnNumber + 83, row, communicationDataRequests, errorMessages);
        addEditCommunicationDataRequest(columnNumber + 166, row, communicationDataRequests, errorMessages);
        return communicationDataRequests;
    }

    private void addEditCommunicationDataRequest(
            int columnNumber,
            Row row,
            List<EditCustomerCommunicationsRequest> communicationDataRequests,
            List<String> errorMessages
    ) {
        EditCustomerCommunicationsRequest request = new EditCustomerCommunicationsRequest();
        Counter counter = new Counter(0);

        List<EditContactPurposeRequest> contactPurposeRequests = getEditContactPurposeRequests(
                columnNumber + 53,
                row,
                errorMessages
        );
        if (!contactPurposeRequests.isEmpty()) {
            request.setContactPurposes(contactPurposeRequests);
            counter.setCount(counter.getCount() + 1);
        }

        List<EditContactPersonRequest> contactPersonRequests = getEditContactPersonRequest(
                columnNumber + 56,
                row,
                errorMessages
        );
        if (!contactPersonRequests.isEmpty()) {
            request.setContactPersons(contactPersonRequests);
            counter.setCount(counter.getCount() + 1);
        }

        List<EditCommunicationContactRequest> communicationContactRequests = getEditCommunicationContactRequests(
                columnNumber + 26,
                row,
                errorMessages
        );
        if (!communicationContactRequests.isEmpty()) {
            request.setCommunicationContacts(communicationContactRequests);
            counter.setCount(counter.getCount() + 1);
        }


        if (row.getCell(columnNumber) != null && row.getCell(columnNumber).getCellType() != CellType.BLANK) {
            request.setContactTypeName(row.getCell(columnNumber).getStringCellValue());
            counter.setCount(counter.getCount() + 1);
        }


        CustomerCommAddressRequest customerCommAddressRequest = getCustomerCommAddressRequest(
                new CustomerCommAddressRequest(),
                columnNumber + 1,
                row,
                errorMessages
        );
        if (customerCommAddressRequest != null) {
            request.setAddress(customerCommAddressRequest);
            counter.setCount(counter.getCount() + 1);
        }

        if (counter.getCount() > 0) {
            request.setStatus(Status.ACTIVE);
            communicationDataRequests.add(request);
        }
    }

    private CustomerCommAddressRequest getCustomerCommAddressRequest(
            CustomerCommAddressRequest request,
            int columnNumber,
            Row row,
            List<String> errorMessages
    ) {
        Counter counter = new Counter(0);
        request.setForeign(getBooleanValue(columnNumber, row, counter, errorMessages, "foreign"));
        request.setNumber(getStringValue(columnNumber + 9, row, counter));
        request.setAdditionalInformation(getStringValue(columnNumber + 10, row, counter));
        request.setBlock(getStringValue(columnNumber + 11, row, counter));
        request.setEntrance(getStringValue(columnNumber + 12, row, counter));
        request.setFloor(getStringValue(columnNumber + 13, row, counter));
        request.setApartment(getStringValue(columnNumber + 14, row, counter));
        request.setMailbox(getStringValue(columnNumber + 15, row, counter));
        setCommAddress(counter, columnNumber, request, row, errorMessages);
        return counter.getCount() > 0 ? request : null;
    }

    private void setCommAddress(
            Counter counter,
            int columnNumber,
            CustomerCommAddressRequest request,
            Row row,
            List<String> errorMessages
    ) {
        setCommForeignAddress(columnNumber, request, row, counter, errorMessages);
        setCommLocalAddress(columnNumber, request, row, counter, errorMessages);
    }

    private void setCommLocalAddress(
            int columnNumber,
            CustomerCommAddressRequest request,
            Row row,
            Counter counter,
            List<String> errorMessages
    ) {
        CustomerCommLocalAddressData localAddressData = getCommLocalAddressData(
                null,
                columnNumber + 1,
                row,
                errorMessages
        );
        if (localAddressData != null) {
            request.setLocalAddressData(localAddressData);
            counter.setCount(counter.getCount() + 1);
        } else request.setLocalAddressData(null);
    }

    private CustomerCommLocalAddressData getCommLocalAddressData(
            CustomerCommLocalAddressData localAddressData,
            int columnNumber,
            Row row,
            List<String> errorMessages
    ) {
        CustomerCommLocalAddressData temp = Objects.requireNonNullElseGet(
                localAddressData,
                CustomerCommLocalAddressData::new
        );

        Counter counter = new Counter(0);
        String countryName = getStringValue(columnNumber, row);
        if (countryName != null) {
            Optional<CacheObject> optionalCountry = countryRepository.getCacheObjectByNameAndStatus(
                    countryName,
                    NomenclatureItemStatus.ACTIVE
            );
            if (optionalCountry.isPresent()) temp.setCountryId(optionalCountry.get().getId());
            else errorMessages.add("countryName-Not found country with name: " + countryName + ";");
        }

        String regionName = getStringValue(columnNumber + 1, row, counter);
        if (regionName != null) {
            Optional<CacheObject> optionalRegion = regionRepository.findByNameAndCountryId(
                    regionName,
                    temp.getCountryId(),
                    NomenclatureItemStatus.ACTIVE
            );
            if (optionalRegion.isPresent()) temp.setRegionId(optionalRegion.get().getId());
            else errorMessages.add("regionName-Not found region with name: " + regionName + " in provided country;");
        }

        String municipalityName = getStringValue(columnNumber + 2, row, counter);
        if (municipalityName != null) {
            Optional<CacheObject> optionalMunicipality = municipalityRepository.getByNameAndRegionId(
                    municipalityName,
                    temp.getRegionId(),
                    NomenclatureItemStatus.ACTIVE
            );
            if (optionalMunicipality.isPresent()) temp.setMunicipalityId(optionalMunicipality.get().getId());
            else
                errorMessages.add("municipalityName-Not found municipality with name: " + municipalityName + " in provided region;");
        }

        String populatedPlaceName = getStringValue(columnNumber + 3, row, counter);
        if (populatedPlaceName != null) {
            Optional<CacheObject> optionalPopulatedPlace = populatedPlaceRepository.getByNameAndMunicipalityId(
                    populatedPlaceName,
                    temp.getMunicipalityId(),
                    NomenclatureItemStatus.ACTIVE
            );
            if (optionalPopulatedPlace.isPresent()) temp.setPopulatedPlaceId(optionalPopulatedPlace.get().getId());
            else
                errorMessages.add("populatedPlaceName-Not found populated place with name: " + populatedPlaceName + " in provided municipality;");
        }

        String zipcodeName = getStringValue(columnNumber + 4, row, counter);
        if (zipcodeName != null) {
            Optional<CacheObject> optionalZipCode = zipCodeRepository.getByNameAndPopulatedPlaceId(
                    zipcodeName,
                    temp.getPopulatedPlaceId(),
                    NomenclatureItemStatus.ACTIVE
            );
            if (optionalZipCode.isPresent()) temp.setZipCodeId(optionalZipCode.get().getId());
            else
                errorMessages.add("zipcodeName-Not found zipcode with name: " + zipcodeName + " in provided populated place;");
        }

        String districtName = getStringValue(columnNumber + 5, row, counter);
        if (districtName != null) {
            Optional<CacheObject> optionalDistrict = districtRepository.getByNameAndPopulatedPlaceId(
                    districtName,
                    temp.getPopulatedPlaceId(),
                    NomenclatureItemStatus.ACTIVE
            );
            if (optionalDistrict.isPresent()) temp.setDistrictId(optionalDistrict.get().getId());
            else
                errorMessages.add("districtName-Not found district with name: " + districtName + " in provided populated place;");
        }

        String residentialAreaName = getStringValue(columnNumber + 6, row, counter);
        if (residentialAreaName != null) {
            Optional<CacheObject> optionalResidentialArea = residentialAreaRepository.getByNameAndPopulatedPlaceId(
                    residentialAreaName,
                    temp.getPopulatedPlaceId(),
                    NomenclatureItemStatus.ACTIVE
            );
            if (optionalResidentialArea.isPresent()) temp.setResidentialAreaId(optionalResidentialArea.get().getId());
            else
                errorMessages.add("residentialAreaName-Not found residential area with name: " + residentialAreaName + " in provided populated place;");
        }

        String streetName = getStringValue(columnNumber + 7, row, counter);
        if (streetName != null) {
            Optional<CacheObject> optionalStreet = streetRepository.getByNameAndPopulatedPlaceId(
                    streetName,
                    temp.getPopulatedPlaceId(),
                    NomenclatureItemStatus.ACTIVE
            );
            if (optionalStreet.isPresent()) temp.setStreetId(optionalStreet.get().getId());
            else
                errorMessages.add("streetName-Not found street with name: " + streetName + " in provided populated place;");
        }

        return (counter.getCount() > 0) ? temp : localAddressData;
    }

    private void setCommForeignAddress(
            int columnNumber,
            CustomerCommAddressRequest request,
            Row row,
            Counter counter,
            List<String> errorMessages
    ) {
        CustomerCommForeignAddressData foreignAddressData = getCommForeignAddressData(
                null,
                columnNumber + 1,
                columnNumber + 16,
                row,
                errorMessages
        );
        if (foreignAddressData != null) {
            request.setForeignAddressData(foreignAddressData);
            counter.setCount(counter.getCount() + 1);
        } else request.setForeignAddressData(null);
    }

    private CustomerCommForeignAddressData getCommForeignAddressData(
            CustomerCommForeignAddressData foreignAddressData,
            int countryColumn,
            int columnNumber,
            Row row,
            List<String> errorMessages
    ) {
        CustomerCommForeignAddressData temp = Objects.requireNonNullElseGet(
                foreignAddressData,
                CustomerCommForeignAddressData::new
        );

        Counter counter = new Counter(0);

        String countryName = getStringValue(countryColumn, row);
        if (countryName != null) {
            Optional<CacheObject> optionalCountry = countryRepository.getCacheObjectByNameAndStatus(
                    countryName,
                    NomenclatureItemStatus.ACTIVE
            );
            if (optionalCountry.isPresent()) temp.setCountryId(optionalCountry.get().getId());
            else errorMessages.add("countryName-Not found country with name: " + countryName + ";");
        }

        String region = getStringValue(columnNumber, row, counter);
        if (region != null) temp.setRegion(region);

        String municipality = getStringValue(columnNumber + 1, row, counter);
        if (municipality != null) temp.setMunicipality(municipality);

        String populatedPlace = getStringValue(columnNumber + 2, row, counter);
        if (populatedPlace != null) temp.setPopulatedPlace(populatedPlace);

        String zipCode = getStringValue(columnNumber + 3, row, counter);
        if (zipCode != null) temp.setZipCode(zipCode);

        String district = getStringValue(columnNumber + 4, row, counter);
        if (district != null) temp.setDistrict(district);

        String residentialAreaType = getStringValue(columnNumber + 5, row, counter);
        if (residentialAreaType != null) temp.setResidentialAreaType(ResidentialAreaType.valueOf(residentialAreaType));

        String residentialArea = getStringValue(columnNumber + 6, row, counter);
        if (residentialArea != null) temp.setResidentialArea(residentialArea);

        String streetType = getStringValue(columnNumber + 7, row, counter);
        if (streetType != null) temp.setStreetType(StreetType.valueOf(streetType));

        String street = getStringValue(columnNumber + 8, row, counter);
        if (street != null) temp.setStreet(street);
        return (counter.getCount() > 0) ? temp : foreignAddressData;
    }

    private List<EditCommunicationContactRequest> getEditCommunicationContactRequests(
            int columnNumber,
            Row row,
            List<String> errorMessages
    ) {
        List<EditCommunicationContactRequest> communicationContactRequests = new ArrayList<>();
        addMobileNumbersForEditRequest(
                columnNumber,
                columnNumber + 6,
                row,
                communicationContactRequests,
                errorMessages
        );
        addLandlinePhonesForEditRequest(
                columnNumber,
                columnNumber + 12,
                row,
                communicationContactRequests,
                errorMessages
        );
        addCallCentersForEditRequest(columnNumber, columnNumber + 15, row, communicationContactRequests, errorMessages);
        addFaxesForEditRequest(columnNumber, columnNumber + 18, row, communicationContactRequests, errorMessages);
        addEmailsForEditRequest(columnNumber, columnNumber + 21, row, communicationContactRequests, errorMessages);
        addWebsitesForEditRequest(columnNumber, columnNumber + 24, row, communicationContactRequests, errorMessages);
        return communicationContactRequests;

    }

    private void addWebsitesForEditRequest(
            int columnNumber,
            int i,
            Row row,
            List<EditCommunicationContactRequest> communicationContactRequests,
            List<String> errorMessages
    ) {
        addCommForEditRequest(
                columnNumber,
                i,
                row,
                communicationContactRequests,
                CustomerCommContactTypes.WEBSITE,
                errorMessages
        );
        addCommForEditRequest(
                columnNumber + 2,
                i + 1,
                row,
                communicationContactRequests,
                CustomerCommContactTypes.WEBSITE,
                errorMessages
        );
        addCommForEditRequest(
                columnNumber + 4,
                i + 2,
                row,
                communicationContactRequests,
                CustomerCommContactTypes.WEBSITE,
                errorMessages
        );
    }


    private void addEmailsForEditRequest(
            int columnNumber,
            int i,
            Row row,
            List<EditCommunicationContactRequest> communicationContactRequests,
            List<String> errorMessages
    ) {
        addCommForEditRequest(
                columnNumber,
                i,
                row,
                communicationContactRequests,
                CustomerCommContactTypes.EMAIL,
                errorMessages
        );
        addCommForEditRequest(
                columnNumber + 2,
                i + 1,
                row,
                communicationContactRequests,
                CustomerCommContactTypes.EMAIL,
                errorMessages
        );
        addCommForEditRequest(
                columnNumber + 4,
                i + 2,
                row,
                communicationContactRequests,
                CustomerCommContactTypes.EMAIL,
                errorMessages
        );
    }

    private void addFaxesForEditRequest(
            int columnNumber,
            int i,
            Row row,
            List<EditCommunicationContactRequest> communicationContactRequests,
            List<String> errorMessages
    ) {
        addCommForEditRequest(
                columnNumber,
                i,
                row,
                communicationContactRequests,
                CustomerCommContactTypes.FAX,
                errorMessages
        );
        addCommForEditRequest(
                columnNumber + 2,
                i + 1,
                row,
                communicationContactRequests,
                CustomerCommContactTypes.FAX,
                errorMessages
        );
        addCommForEditRequest(
                columnNumber + 4,
                i + 2,
                row,
                communicationContactRequests,
                CustomerCommContactTypes.FAX,
                errorMessages
        );
    }

    private void addCallCentersForEditRequest(
            int columnNumber, int i, Row row, List<EditCommunicationContactRequest> communicationContactRequests,
            List<String> errorMessages
    ) {
        addCommForEditRequest(
                columnNumber,
                i,
                row,
                communicationContactRequests,
                CustomerCommContactTypes.CALL_CENTER,
                errorMessages
        );
        addCommForEditRequest(
                columnNumber + 2,
                i + 1,
                row,
                communicationContactRequests,
                CustomerCommContactTypes.CALL_CENTER,
                errorMessages
        );
        addCommForEditRequest(
                columnNumber + 4,
                i + 2,
                row,
                communicationContactRequests,
                CustomerCommContactTypes.CALL_CENTER,
                errorMessages
        );
    }

    private void addLandlinePhonesForEditRequest(
            int columnNumber, int i, Row row,
            List<EditCommunicationContactRequest> communicationContactRequests, List<String> errorMessages
    ) {
        addCommForEditRequest(
                columnNumber,
                i,
                row,
                communicationContactRequests,
                CustomerCommContactTypes.LANDLINE_PHONE,
                errorMessages
        );
        addCommForEditRequest(
                columnNumber + 2,
                i + 1,
                row,
                communicationContactRequests,
                CustomerCommContactTypes.LANDLINE_PHONE,
                errorMessages
        );
        addCommForEditRequest(
                columnNumber + 4,
                i + 2,
                row,
                communicationContactRequests,
                CustomerCommContactTypes.LANDLINE_PHONE,
                errorMessages
        );
    }

    private void addCommForEditRequest(
            int columnNumber, int i, Row row, List<EditCommunicationContactRequest> communicationContactRequests,
            CustomerCommContactTypes type, List<String> errorMessages
    ) {
        EditCommunicationContactRequest request = new EditCommunicationContactRequest();
        Counter counter = new Counter(0);

        // TODO: change to name instead of type
        String platformName = getStringValue(columnNumber + 1, row, counter);
        if (platformName != null) {
            Optional<CacheObject> optionalPlatform = platformRepository.getByNameAndStatus(
                    platformName,
                    NomenclatureItemStatus.ACTIVE
            );
            if (optionalPlatform.isPresent()) request.setPlatformId(optionalPlatform.get().getId());
            else errorMessages.add("platformName-Not found platform with name: " + platformName + ";");
        }

        request.setContactValue(getStringValue(i, row, counter));

        if (counter.getCount() > 0) {
            request.setSendSms(false);
            request.setStatus(Status.ACTIVE);
            request.setContactType(type);
            communicationContactRequests.add(request);
        }
    }

    private void addMobileNumbersForEditRequest(
            int columnNumber, int i, Row row, List<EditCommunicationContactRequest> communicationContactRequests,
            List<String> errorMessages
    ) {
        addMobileNumberForEditRequest(columnNumber, i, row, communicationContactRequests, errorMessages);
        addMobileNumberForEditRequest(columnNumber + 2, i + 2, row, communicationContactRequests, errorMessages);
        addMobileNumberForEditRequest(columnNumber + 4, i + 4, row, communicationContactRequests, errorMessages);
    }

    private void addMobileNumberForEditRequest(
            int columnNumber, int i, Row row, List<EditCommunicationContactRequest> communicationContactRequests,
            List<String> errorMessages
    ) {
        EditCommunicationContactRequest request = new EditCommunicationContactRequest();
        Counter counter = new Counter(0);

        // TODO: change to name instead of type
        String platformName = getStringValue(columnNumber + 1, row, counter);
        if (platformName != null) {
            Optional<CacheObject> optionalPlatform = platformRepository.getByNameAndStatus(
                    platformName,
                    NomenclatureItemStatus.ACTIVE
            );
            if (optionalPlatform.isPresent()) request.setPlatformId(optionalPlatform.get().getId());
            else errorMessages.add("platformName-Not found platform with name: " + platformName + ";");
        }
        request.setContactValue(getStringValue(i, row, counter));
        request.setSendSms(getBoolean(getStringValue(i + 1, row, counter), errorMessages, "sendSms"));


        if (counter.getCount() > 0) {
            request.setStatus(Status.ACTIVE);
            request.setContactType(CustomerCommContactTypes.MOBILE_NUMBER);
            communicationContactRequests.add(request);
        }
    }


    private List<EditContactPersonRequest> getEditContactPersonRequest(
            int columnNumber,
            Row row,
            List<String> errorMessages
    ) {
        List<EditContactPersonRequest> contactPersonRequests = new ArrayList<>();
        addContactPersonRequestForEditCustomerRequest(columnNumber, row, contactPersonRequests, errorMessages);
        addContactPersonRequestForEditCustomerRequest(columnNumber + 9, row, contactPersonRequests, errorMessages);
        addContactPersonRequestForEditCustomerRequest(columnNumber + 18, row, contactPersonRequests, errorMessages);
        return contactPersonRequests;
    }

    private void addContactPersonRequestForEditCustomerRequest(
            int columnNumber,
            Row row,
            List<EditContactPersonRequest> contactPersonRequests,
            List<String> errorMessages
    ) {
        EditContactPersonRequest request = new EditContactPersonRequest();
        Counter counter = new Counter(0);

        String titleName = getStringValue(columnNumber + 8, row, counter);
        if (titleName != null) {
            Optional<CacheObject> optionalTitle = titleRepository.getByNameAndStatus(
                    titleName,
                    NomenclatureItemStatus.ACTIVE
            );
            if (optionalTitle.isPresent()) request.setTitleId(optionalTitle.get().getId());
            else errorMessages.add("titleName-Not found title with name: " + titleName + ";");
        }


        request.setName(getStringValue(columnNumber, row, counter));
        request.setMiddleName(getStringValue(columnNumber + 1, row, counter));
        request.setSurname(getStringValue(columnNumber + 2, row, counter));
        request.setJobPosition(getStringValue(columnNumber + 3, row, counter));
        request.setPositionHeldFrom((getDateValue(columnNumber + 4, row, counter)));
        request.setPositionHeldTo(getDateValue(columnNumber + 5, row, counter));
        LocalDate dateValue = getDateValue(columnNumber + 6, row, counter);
        request.setBirthDate(dateValue == null ? null : dateValue.toString());
        request.setAdditionalInformation(getStringValue(columnNumber + 7, row, counter));

        if (counter.getCount() > 0) {
            request.setStatus(Status.ACTIVE);
            contactPersonRequests.add(request);
        }
    }

    private List<EditContactPurposeRequest> getEditContactPurposeRequests(
            int columnNumber,
            Row row,
            List<String> errorMessages
    ) {
        List<EditContactPurposeRequest> contactPurposeRequests = new ArrayList<>();
        addContactPurposeRequestForEditCustomerRequest(columnNumber, row, contactPurposeRequests, errorMessages);
        addContactPurposeRequestForEditCustomerRequest(columnNumber + 1, row, contactPurposeRequests, errorMessages);
        addContactPurposeRequestForEditCustomerRequest(columnNumber + 2, row, contactPurposeRequests, errorMessages);
        return contactPurposeRequests;
    }

    private void addContactPurposeRequestForEditCustomerRequest(
            int columnNumber, Row row, List<EditContactPurposeRequest> contactPurposeRequests,
            List<String> errorMessages
    ) {
        EditContactPurposeRequest request = new EditContactPurposeRequest();

        String contactPurposeName = getStringValue(columnNumber, row);
        if (contactPurposeName != null) {
            Optional<CacheObject> optionalContactPurpose = contactPurposeRepository.getByNameAndStatus(
                    contactPurposeName,
                    NomenclatureItemStatus.ACTIVE
            );
            if (optionalContactPurpose.isPresent()) {
                if (contactPurposeRequests.stream().
                        noneMatch(c -> c.getContactPurposeId().equals(optionalContactPurpose.get().getId()))) {
                    request.setContactPurposeId(optionalContactPurpose.get().getId());
                    request.setStatus(Status.ACTIVE);
                    contactPurposeRequests.add(request);
                } else {
                    errorMessages.add("contactPurpose-Cannot add multiple contact purpose with same name: " + contactPurposeName + ";");
                }
            } else errorMessages.add("contactPurpose-Not found contact purpose with name: " + contactPurposeName + ";");
        }
    }

    private List<CustomerOwnerEditRequest> getCustomerOwnerEditRequests(
            int columnNumber,
            Row row,
            List<String> errorMessages
    ) {
        List<CustomerOwnerEditRequest> ownerRequests = new ArrayList<>();
        addCustomerOwnerEditRequest(columnNumber, row, ownerRequests, errorMessages);
        addCustomerOwnerEditRequest(columnNumber + 3, row, ownerRequests, errorMessages);
        addCustomerOwnerEditRequest(columnNumber + 6, row, ownerRequests, errorMessages);
        return ownerRequests;
    }

    private void addCustomerOwnerEditRequest(
            int columnNumber,
            Row row,
            List<CustomerOwnerEditRequest> ownerRequests,
            List<String> errorMessages
    ) {
        Counter counter = new Counter(0);
        CustomerOwnerEditRequest request = new CustomerOwnerEditRequest();

        request.setPersonalNumber(getStringValue(columnNumber, row, counter));

        String belongingCapitalOwnerName = getStringValue(columnNumber + 1, row, counter);
        if (belongingCapitalOwnerName != null) {
            Optional<CacheObject> optionalBelongingCapitalOwner = belongingCapitalOwnerRepository.getByNameAndStatus(
                    belongingCapitalOwnerName,
                    NomenclatureItemStatus.ACTIVE
            );
            if (optionalBelongingCapitalOwner.isPresent())
                request.setBelongingOwnerCapitalId(optionalBelongingCapitalOwner.get().getId());
            else
                errorMessages.add("belongingCapitalOwner-Not found belonging capital owner with name: " + belongingCapitalOwnerName + ";");
        }


        request.setAdditionalInformation(getStringValue(columnNumber + 1, row, counter));

        if (counter.getCount() > 0) ownerRequests.add(request);
    }

    private List<EditRelatedCustomerRequest> getEditRelatedCustomerRequests(
            int columnNumber,
            Row row,
            List<String> errorMessages
    ) {
        List<EditRelatedCustomerRequest> relatedCustomerRequests = new ArrayList<>();
        addEditRelatedCustomerRequest(columnNumber, row, relatedCustomerRequests, errorMessages);
        addEditRelatedCustomerRequest(columnNumber + 2, row, relatedCustomerRequests, errorMessages);
        addEditRelatedCustomerRequest(columnNumber + 4, row, relatedCustomerRequests, errorMessages);
        return relatedCustomerRequests;
    }

    private void addEditRelatedCustomerRequest(
            int columnNumber,
            Row row,
            List<EditRelatedCustomerRequest> relatedCustomerRequests,
            List<String> errorMessages
    ) {
        EditRelatedCustomerRequest request = new EditRelatedCustomerRequest();
        Counter counter = new Counter(0);

        String identifier = getStringValue(columnNumber, row, counter);
        if (identifier != null) {
            Optional<Customer> optionalCustomer = customerRepository.findByIdentifierAndStatus(
                    identifier,
                    CustomerStatus.ACTIVE
            );
            if (optionalCustomer.isPresent()) request.setRelatedCustomerId(optionalCustomer.get().getId());
            else
                errorMessages.add("relatedCustomerIdentifier-Not found related customer with identifier: " + identifier + ";");
        }

        String ciConnectionType = getStringValue(columnNumber + 1, row, counter);
        if (ciConnectionType != null) {
            Optional<CacheObject> optionalCiConnectionType = ciConnectionTypeRepository.getByNameAndStatus(
                    ciConnectionType,
                    NomenclatureItemStatus.ACTIVE
            );
            if (optionalCiConnectionType.isPresent())
                request.setCiConnectionTypeId(optionalCiConnectionType.get().getId());
            else errorMessages.add("connectionType-Not found ci connection type with name: " + ciConnectionType + ";");
        }

        if (counter.getCount() > 0) {
            request.setStatus(Status.ACTIVE);
            relatedCustomerRequests.add(request);
        }
    }

    private List<EditManagerRequest> getEditManagerRequests(int columnNumber, Row row, List<String> errorMessages) {
        List<EditManagerRequest> managerRequests = new ArrayList<>();
        addEditManagerRequest(columnNumber, row, managerRequests, errorMessages);
        addEditManagerRequest(columnNumber + 11, row, managerRequests, errorMessages);
        addEditManagerRequest(columnNumber + 22, row, managerRequests, errorMessages);
        return managerRequests;
    }

    private void addEditManagerRequest(
            int columnNumber,
            Row row,
            List<EditManagerRequest> managerRequests,
            List<String> errorMessages
    ) {
        EditManagerRequest request = new EditManagerRequest();
        Counter counter = new Counter(0);

        String titleName = getStringValue(columnNumber + 9, row, counter);
        if (titleName != null) {
            Optional<CacheObject> optionalTitle = titleRepository.getByNameAndStatus(
                    titleName,
                    NomenclatureItemStatus.ACTIVE
            );
            if (optionalTitle.isPresent()) request.setTitleId(optionalTitle.get().getId());
            else errorMessages.add("titleName-Not found title with name: " + titleName + ";");
        }

        request.setName(getStringValue(columnNumber, row, counter));

        request.setMiddleName(getStringValue(columnNumber + 1, row, counter));
        request.setSurname(getStringValue(columnNumber + 2, row, counter));
        request.setPersonalNumber(getStringValue(columnNumber + 3, row, counter));

        request.setJobPosition(getStringValue(columnNumber + 4, row, counter));
        request.setPositionHeldFrom(getDateValue(columnNumber + 5, row, counter));
        request.setPositionHeldTo(getDateValue(columnNumber + 6, row, counter));
        LocalDate dateValue = getDateValue(columnNumber + 7, row, counter);
        request.setBirthDate(dateValue == null ? null : dateValue.toString());

        String representationMethodName = getStringValue(columnNumber + 8, row, counter);
        if (representationMethodName != null) {
            Optional<CacheObject> optionalRepresentationMethod = representationMethodRepository.getByNameAndStatus(
                    representationMethodName,
                    NomenclatureItemStatus.ACTIVE
            );
            if (optionalRepresentationMethod.isPresent())
                request.setRepresentationMethodId(optionalRepresentationMethod.get().getId());
            else
                errorMessages.add("representationMethodName-Not found representation method with name: " + representationMethodName + ";");

        }

        request.setAdditionalInformation(getStringValue(columnNumber + 10, row, counter));

        if (counter.getCount() > 0) {
            request.setStatus(Status.ACTIVE);
            managerRequests.add(request);
        }
    }

    public CreateCustomerRequest convertToCreateCustomerRequest(
            CreateCustomerRequest request,
            Row row,
            List<String> errorMessages
    ) {
        setFields(request, row, errorMessages);
        setSubObjects(request, row, errorMessages);
        return request;
    }

    private void setFields(CreateCustomerRequest request, Row row, List<String> errorMessages) {
        request.setCustomerType(CustomerType.valueOf(getStringValue(2, row)));
        request.setBusinessActivity(getBoolean(getStringValue(3, row), errorMessages, "businessActivity"));
        request.setCustomerIdentifier(getStringValue(8, row));
        request.setForeign(getBoolean(getStringValue(6, row), errorMessages, "foreign"));
        request.setPreferCommunicationInEnglish(Boolean.TRUE.equals(getBoolean(
                getStringValue(365, row),
                errorMessages,
                "preferCommunicationInEnglish"
        )));
        request.setMarketingConsent(getBoolean(getStringValue(5, row), errorMessages, "marketingConsent"));
        request.setOldCustomerNumber(getStringValue(10, row));
        request.setVatNumber(getStringValue(11, row));
        request.setCustomerDetailStatus(CustomerDetailStatus.valueOf(getStringValue(9, row)));

        String ownershipFormName = getStringValue(22, row);
        if (ownershipFormName != null) {
            Optional<CacheObject> optionalOwnershipForm = ownershipFormRepository.findByNameAndStatus(
                    ownershipFormName,
                    NomenclatureItemStatus.ACTIVE
            );
            if (optionalOwnershipForm.isPresent()) request.setOwnershipFormId(optionalOwnershipForm.get().getId());
            else errorMessages.add("ownershipForm-Not found ownership form  with name: " + ownershipFormName + ";");
        }

        String economicBranchCI = getStringValue(23, row);
        if (economicBranchCI != null) {
            Optional<CacheObject> optionalEconomicBranchCI = economicBranchCIRepository.findByNameAndStatus(
                    economicBranchCI,
                    NomenclatureItemStatus.ACTIVE
            );
            if (optionalEconomicBranchCI.isPresent())
                request.setEconomicBranchId(optionalEconomicBranchCI.get().getId());
            else errorMessages.add("economicBranchCI-Not found economic branch with name: " + economicBranchCI + ";");
        }

        String economicBranchNCEA = getStringValue(24, row);
        if (economicBranchNCEA != null) {
            Optional<CacheObject> optionalEconomicBranchNCEA = economicBranchNCEARepository.findByNameAndStatus(
                    economicBranchNCEA,
                    NomenclatureItemStatus.ACTIVE
            );
            if (optionalEconomicBranchNCEA.isPresent())
                request.setEconomicBranchNCEAId(optionalEconomicBranchNCEA.get().getId());
            else
                errorMessages.add("economicBranchNCEA-Not found economic branch NCEA with name: " + economicBranchNCEA + ";");
        }

        request.setMainSubjectOfActivity(getStringValue(28, row));

        setSegments(request, row, errorMessages);
        setCustomerDetails(request, row, errorMessages);

        CustomerAddressRequest customerAddress = getCustomerAddressRequest(
                new CustomerAddressRequest(),
                29,
                row,
                errorMessages
        );
        if (customerAddress != null) request.setAddress(customerAddress);

        CustomerBankingDetails bankingDetails = getCustomerBankingDetails(54, row, errorMessages);
        if (bankingDetails != null) request.setBankingDetails(bankingDetails);
    }

    private void setSubObjects(CreateCustomerRequest request, Row row, List<String> errorMessages) {
        setManagers(request, row, errorMessages);
        setRelatedCustomers(request, row, errorMessages);

        List<CustomerOwnerRequest> owners = getOwners(68, row, errorMessages);
        if (!owners.isEmpty()) request.setOwner(owners);

        List<CreateCustomerCommunicationsRequest> communicationData = getCommunicationData(116, row, errorMessages);
        if (!communicationData.isEmpty()) request.setCommunicationData(communicationData);

        List<CreateCustomerAccountManagerRequest> accountManagers = getCustomerAccountManagerRequests(
                110,
                row,
                errorMessages
        );
        if (!accountManagers.isEmpty()) request.setAccountManagers(accountManagers);
    }

    private void setRelatedCustomers(CreateCustomerRequest request, Row row, List<String> errorMessages) {
        List<CreateRelatedCustomerRequest> relatedCustomers = getRelatedCustomers(62, row, errorMessages);
        if (!relatedCustomers.isEmpty()) {
            request.setRelatedCustomers(relatedCustomers);
        }
    }

    private void setManagers(CreateCustomerRequest request, Row row, List<String> errorMessages) {
        List<CreateManagerRequest> managers = getManagers(77, row, errorMessages);
        if (!managers.isEmpty()) {
            request.setManagers(managers);
        }
    }

    private void setCustomerDetails(CreateCustomerRequest request, Row row, List<String> errorMessages) {
        request.setPrivateCustomerDetails(getPrivateCustomerDetails(
                null,
                row,
                request.getCustomerType(),
                errorMessages
        ));
        request.setBusinessCustomerDetails(getBusinessCustomerDetails(
                null,
                row,
                errorMessages,
                request.getCustomerType(),
                request.getBusinessActivity()
        ));
    }

    private void setSegments(CreateCustomerRequest request, Row row, List<String> errorMessages) {
        List<Long> segmentIds = getSegmentIds(25, row, errorMessages);
        if (!segmentIds.isEmpty()) {
            request.setSegmentIds(segmentIds);
        }
    }

    private List<CreateCustomerAccountManagerRequest> getCustomerAccountManagerRequests(
            int columnNumber,
            Row row,
            List<String> errorMessages
    ) {
        List<CreateCustomerAccountManagerRequest> customerAccountManagerRequests = new ArrayList<>();
        addCustomerAccountManagerRequest(columnNumber, row, customerAccountManagerRequests, errorMessages);
        addCustomerAccountManagerRequest(columnNumber + 2, row, customerAccountManagerRequests, errorMessages);
        addCustomerAccountManagerRequest(columnNumber + 4, row, customerAccountManagerRequests, errorMessages);
        return customerAccountManagerRequests;
    }

    private List<EditCustomerAccountManagerRequest> getEditCustomerAccountManagerRequests(
            int columnNumber,
            Row row,
            List<String> errorMessages
    ) {
        List<EditCustomerAccountManagerRequest> customerAccountManagerRequests = new ArrayList<>();
        addEditCustomerAccountManagerRequest(columnNumber, row, customerAccountManagerRequests, errorMessages);
        addEditCustomerAccountManagerRequest(columnNumber + 2, row, customerAccountManagerRequests, errorMessages);
        addEditCustomerAccountManagerRequest(columnNumber + 4, row, customerAccountManagerRequests, errorMessages);
        return customerAccountManagerRequests;
    }

    private void addEditCustomerAccountManagerRequest(
            int columnNumber,
            Row row,
            List<EditCustomerAccountManagerRequest> customerAccountManagerRequests,
            List<String> errorMessages
    ) {
        EditCustomerAccountManagerRequest request = new EditCustomerAccountManagerRequest();
        Counter counter = new Counter(0);
        String managerIdentifier = getStringValue(columnNumber, row, counter);
        if (managerIdentifier != null) {
            Optional<CacheObject> optionalAccountManager = accountManagerRepository.getByUsernameAndStatus(
                    managerIdentifier,
                    Status.ACTIVE
            );
            if (optionalAccountManager.isPresent()) request.setAccountManagerId(optionalAccountManager.get().getId());
            else
                errorMessages.add("accountManagerIdentifier-Not found account manager with username: " + managerIdentifier + ";");
        }

        String accountManagerType = getStringValue(columnNumber + 1, row, counter);
        if (accountManagerType != null) {
            Optional<CacheObject> optionalAccountManagerType = accountManagerTypeRepository.getByNameAndStatus(
                    accountManagerType,
                    NomenclatureItemStatus.ACTIVE
            );
            if (optionalAccountManagerType.isPresent())
                request.setAccountManagerTypeId(optionalAccountManagerType.get().getId());
            else
                errorMessages.add("accountManagerType-Not found account manager type  with name: " + accountManagerType + ";");
        }

        if (counter.getCount() > 0) customerAccountManagerRequests.add(request);
    }

    private void addCustomerAccountManagerRequest(
            int columnNumber, Row row, List<CreateCustomerAccountManagerRequest> customerAccountManagerRequests,
            List<String> errorMessages
    ) {
        CreateCustomerAccountManagerRequest request = new CreateCustomerAccountManagerRequest();
        Counter counter = new Counter(0);
        String managerIdentifier = getStringValue(columnNumber, row, counter);
        if (managerIdentifier != null) {
            Optional<CacheObject> optionalAccountManager = accountManagerRepository.getByUsernameAndStatus(
                    managerIdentifier,
                    Status.ACTIVE
            );
            if (optionalAccountManager.isPresent()) request.setAccountManagerId(optionalAccountManager.get().getId());
            else
                errorMessages.add("accountManagerIdentifier-Not found account manager with username: " + managerIdentifier + ";");
        }

        String accountManagerType = getStringValue(columnNumber + 1, row, counter);
        if (accountManagerType != null) {
            Optional<CacheObject> optionalAccountManagerType = accountManagerTypeRepository.getByNameAndStatus(
                    accountManagerType,
                    NomenclatureItemStatus.ACTIVE
            );
            if (optionalAccountManagerType.isPresent())
                request.setAccountManagerTypeId(optionalAccountManagerType.get().getId());
            else
                errorMessages.add("accountManagerType-Not found account manager type  with name: " + accountManagerType + ";");
        }

        if (counter.getCount() > 0) customerAccountManagerRequests.add(request);
    }

    private List<CreateCustomerCommunicationsRequest> getCommunicationData(
            int columnNumber,
            Row row,
            List<String> errorMessages
    ) {
        List<CreateCustomerCommunicationsRequest> communicationDataRequests = new ArrayList<>();
        addCommunicationData(columnNumber, row, communicationDataRequests, errorMessages);
        addCommunicationData(columnNumber + 83, row, communicationDataRequests, errorMessages);
        addCommunicationData(columnNumber + 166, row, communicationDataRequests, errorMessages);
        return communicationDataRequests;
    }

    private void addCommunicationData(
            int columnNumber,
            Row row,
            List<CreateCustomerCommunicationsRequest> communicationDataRequests,
            List<String> errorMessages
    ) {
        Counter counter = new Counter(0);
        CreateCustomerCommunicationsRequest request = new CreateCustomerCommunicationsRequest();

        List<CreateContactPurposeRequest> contactPurposeRequests = getCreateContactPurposeRequests(
                columnNumber + 53,
                row,
                errorMessages
        );
        if (!contactPurposeRequests.isEmpty()) {
            request.setContactPurposes(contactPurposeRequests);
            counter.setCount(counter.getCount() + 1);
        }
        List<CreateContactPersonRequest> contactPersonRequests = getCreateContactPersonRequests(
                columnNumber + 56,
                row,
                errorMessages
        );
        if (!contactPersonRequests.isEmpty()) {
            request.setContactPersons(contactPersonRequests);
            counter.setCount(counter.getCount() + 1);
        }

        List<CreateCommunicationContactRequest> communicationContactRequests = getCreateCommunicationContactRequests(
                columnNumber + 26,
                row,
                errorMessages
        );
        if (!communicationContactRequests.isEmpty()) {
            request.setCommunicationContacts(communicationContactRequests);
            counter.setCount(counter.getCount() + 1);
        }
        request.setContactTypeName(getStringValue(columnNumber, row, counter));

        CustomerCommAddressRequest customerAddressRequest = getCustomerCommAddressRequest(
                new CustomerCommAddressRequest(),
                columnNumber + 1,
                row,
                errorMessages
        );
        if (customerAddressRequest != null) {
            request.setAddress(customerAddressRequest);
            counter.setCount(counter.getCount() + 1);
        }
        if (counter.getCount() > 0) {
            request.setStatus(Status.ACTIVE);
            communicationDataRequests.add(request);
        }
    }

    private List<CreateCommunicationContactRequest> getCreateCommunicationContactRequests(
            int columnNumber,
            Row row,
            List<String> errorMessages
    ) {
        List<CreateCommunicationContactRequest> communicationContactRequests = new ArrayList<>();
        addOtherPlatform(columnNumber, columnNumber, row, communicationContactRequests, errorMessages);
        addMobileNumbers(columnNumber, columnNumber + 6, row, communicationContactRequests, errorMessages);
        addLandlinePhones(columnNumber, columnNumber + 12, row, communicationContactRequests, errorMessages);
        addCallCenters(columnNumber, columnNumber + 15, row, communicationContactRequests, errorMessages);
        addFaxes(columnNumber, columnNumber + 18, row, communicationContactRequests, errorMessages);
        addEmails(columnNumber, columnNumber + 21, row, communicationContactRequests, errorMessages);
        addWebsites(columnNumber, columnNumber + 24, row, communicationContactRequests, errorMessages);
        return communicationContactRequests;
    }

    private void addOtherPlatform(
            int columnNumber,
            int i,
            Row row,
            List<CreateCommunicationContactRequest> communicationContactRequests,
            List<String> errorMessages
    ) {
        addOtherPlatform(
                columnNumber,
                i,
                row,
                communicationContactRequests,
                CustomerCommContactTypes.OTHER_PLATFORM,
                errorMessages
        );
        addOtherPlatform(
                columnNumber + 2,
                i + 1,
                row,
                communicationContactRequests,
                CustomerCommContactTypes.OTHER_PLATFORM,
                errorMessages
        );
        addOtherPlatform(
                columnNumber + 4,
                i + 2,
                row,
                communicationContactRequests,
                CustomerCommContactTypes.OTHER_PLATFORM,
                errorMessages
        );
    }

    private void addOtherPlatform(
            int columnNumber, int i, Row row, List<CreateCommunicationContactRequest> communicationContactRequests,
            CustomerCommContactTypes type, List<String> errorMessages
    ) {
        CreateCommunicationContactRequest request = new CreateCommunicationContactRequest();
        Counter counter = new Counter(0);

        String platformName = getStringValue(columnNumber, row, counter);

        String platformType = getStringValue(columnNumber + 1, row, counter);
        if (platformType != null) {
            Optional<CacheObject> optionalPlatform = platformRepository.getByNameAndStatus(
                    platformType,
                    NomenclatureItemStatus.ACTIVE
            );
            if (optionalPlatform.isPresent()) request.setPlatformId(optionalPlatform.get().getId());
            else errorMessages.add("platformType-Not found platform with name: " + platformType + ";");
        }
        request.setContactValue(platformName);

        if (platformName != null && request.getPlatformId() == null) {
            errorMessages.add("platformType-PlatformType must be provided when name is entered: " + platformName + ";");
        }
        if (platformName == null && request.getPlatformId() != null) {
            errorMessages.add("platformName-platformName must be provided when name is entered: " + platformType + ";");
        }

        if (counter.getCount() > 0) {
            request.setSendSms(false);
            request.setStatus(Status.ACTIVE);
            request.setContactType(type);
            communicationContactRequests.add(request);
        }
    }

    private void addWebsites(
            int columnNumber,
            int i,
            Row row,
            List<CreateCommunicationContactRequest> communicationContactRequests,
            List<String> errorMessages
    ) {
        addContactData(
                columnNumber,
                i,
                row,
                communicationContactRequests,
                CustomerCommContactTypes.WEBSITE,
                errorMessages
        );
        addContactData(
                columnNumber + 2,
                i + 1,
                row,
                communicationContactRequests,
                CustomerCommContactTypes.WEBSITE,
                errorMessages
        );
        addContactData(
                columnNumber + 4,
                i + 2,
                row,
                communicationContactRequests,
                CustomerCommContactTypes.WEBSITE,
                errorMessages
        );
    }

    private void addEmails(
            int columnNumber,
            int i,
            Row row,
            List<CreateCommunicationContactRequest> communicationContactRequests,
            List<String> errorMessages
    ) {
        addContactData(
                columnNumber,
                i,
                row,
                communicationContactRequests,
                CustomerCommContactTypes.EMAIL,
                errorMessages
        );
        addContactData(
                columnNumber + 2,
                i + 1,
                row,
                communicationContactRequests,
                CustomerCommContactTypes.EMAIL,
                errorMessages
        );
        addContactData(
                columnNumber + 4,
                i + 2,
                row,
                communicationContactRequests,
                CustomerCommContactTypes.EMAIL,
                errorMessages
        );
    }

    private void addFaxes(
            int columnNumber, int i, Row row, List<CreateCommunicationContactRequest> communicationContactRequests,
            List<String> errorMessages
    ) {
        addContactData(columnNumber, i, row, communicationContactRequests, CustomerCommContactTypes.FAX, errorMessages);
        addContactData(
                columnNumber + 2,
                i + 1,
                row,
                communicationContactRequests,
                CustomerCommContactTypes.FAX,
                errorMessages
        );
        addContactData(
                columnNumber + 4,
                i + 2,
                row,
                communicationContactRequests,
                CustomerCommContactTypes.FAX,
                errorMessages
        );
    }

    private void addCallCenters(
            int columnNumber, int i, Row row, List<CreateCommunicationContactRequest> communicationContactRequests,
            List<String> errorMessages
    ) {
        addContactData(
                columnNumber,
                i,
                row,
                communicationContactRequests,
                CustomerCommContactTypes.CALL_CENTER,
                errorMessages
        );
        addContactData(
                columnNumber + 2,
                i + 1,
                row,
                communicationContactRequests,
                CustomerCommContactTypes.CALL_CENTER,
                errorMessages
        );
        addContactData(
                columnNumber + 4,
                i + 2,
                row,
                communicationContactRequests,
                CustomerCommContactTypes.CALL_CENTER,
                errorMessages
        );
    }

    private void addLandlinePhones(
            int columnNumber, int i, Row row, List<CreateCommunicationContactRequest> communicationContactRequests,
            List<String> errorMessages
    ) {
        addContactData(
                columnNumber,
                i,
                row,
                communicationContactRequests,
                CustomerCommContactTypes.LANDLINE_PHONE,
                errorMessages
        );
        addContactData(
                columnNumber + 2,
                i + 1,
                row,
                communicationContactRequests,
                CustomerCommContactTypes.LANDLINE_PHONE,
                errorMessages
        );
        addContactData(
                columnNumber + 4,
                i + 2,
                row,
                communicationContactRequests,
                CustomerCommContactTypes.LANDLINE_PHONE,
                errorMessages
        );
    }

    private void addContactData(
            int columnNumber, int i, Row row, List<CreateCommunicationContactRequest> communicationContactRequests,
            CustomerCommContactTypes type, List<String> errorMessages
    ) {
        CreateCommunicationContactRequest request = new CreateCommunicationContactRequest();
        Counter counter = new Counter(0);
        request.setContactValue(getStringValue(i, row, counter));

        if (counter.getCount() > 0) {
            request.setSendSms(false);
            request.setStatus(Status.ACTIVE);
            request.setContactType(type);
            communicationContactRequests.add(request);
        }
    }

    private void addMobileNumbers(
            int columnNumber,
            int i,
            Row row,
            List<CreateCommunicationContactRequest> communicationContactRequests,
            List<String> errorMessages
    ) {
        addMobileNumber(columnNumber, i, row, communicationContactRequests, errorMessages);
        addMobileNumber(columnNumber + 2, i + 2, row, communicationContactRequests, errorMessages);
        addMobileNumber(columnNumber + 4, i + 4, row, communicationContactRequests, errorMessages);
    }

    private void addMobileNumber(
            int columnNumber,
            int i,
            Row row,
            List<CreateCommunicationContactRequest> communicationContactRequests,
            List<String> errorMessages
    ) {
        CreateCommunicationContactRequest request = new CreateCommunicationContactRequest();
        Counter counter = new Counter(0);
        request.setContactValue(getStringValue(i, row, counter));
        request.setSendSms(getBooleanValue(i + 1, row, counter, errorMessages, "sendSms"));

        if (counter.getCount() > 0) {
            request.setStatus(Status.ACTIVE);
            request.setContactType(CustomerCommContactTypes.MOBILE_NUMBER);
            communicationContactRequests.add(request);
        }
    }

    private Boolean getBooleanValue(int columnNumber, Row row, List<String> errorMessages, String fieldName) {
        if (row.getCell(columnNumber) != null && row.getCell(columnNumber).getCellType() != CellType.BLANK) {
            ((XSSFCell) row.getCell(columnNumber)).setCellType(CellType.STRING);
            return getBoolean(row.getCell(columnNumber).getStringCellValue(), errorMessages, fieldName);
        }
        return null;
    }

    private Boolean getBooleanValue(
            int columnNumber,
            Row row,
            Counter counter,
            List<String> errorMessages,
            String fieldName
    ) {
        if (row.getCell(columnNumber) != null && row.getCell(columnNumber).getCellType() != CellType.BLANK) {
            ((XSSFCell) row.getCell(columnNumber)).setCellType(CellType.STRING);
            counter.setCount(counter.getCount() + 1);
            return getBoolean(row.getCell(columnNumber).getStringCellValue(), errorMessages, fieldName);
        }
        return null;
    }

    private Boolean getBoolean(String value, List<String> errorMessages, String fieldName) {
        if (value != null) {
            if (value.equalsIgnoreCase("YES")) return true;
            if (value.equalsIgnoreCase("NO")) return false;
            errorMessages.add(fieldName + "-Must be provided only YES or NO;");
        }
        return null;
    }


    private List<CreateContactPersonRequest> getCreateContactPersonRequests(
            int columnNumber,
            Row row,
            List<String> errorMessages
    ) {
        List<CreateContactPersonRequest> contactPersonRequests = new ArrayList<>();
        addContactPersonRequest(columnNumber, row, contactPersonRequests, errorMessages);
        addContactPersonRequest(columnNumber + 9, row, contactPersonRequests, errorMessages);
        addContactPersonRequest(columnNumber + 18, row, contactPersonRequests, errorMessages);
        return contactPersonRequests;
    }

    private void addContactPersonRequest(
            int columnNumber,
            Row row,
            List<CreateContactPersonRequest> contactPersonRequests,
            List<String> errorMessages
    ) {
        CreateContactPersonRequest request = new CreateContactPersonRequest();
        Counter counter = new Counter(0);

        String titleName = getStringValue(columnNumber + 8, row, counter);
        if (titleName != null) {
            Optional<CacheObject> optionalTitle = titleRepository.getByNameAndStatus(
                    titleName,
                    NomenclatureItemStatus.ACTIVE
            );
            if (optionalTitle.isPresent()) request.setTitleId(optionalTitle.get().getId());
            else errorMessages.add("titleName-Not found title with name: " + titleName + ";");
        }


        request.setName(getStringValue(columnNumber, row, counter));
        request.setMiddleName(getStringValue(columnNumber + 1, row, counter));
        request.setSurname(getStringValue(columnNumber + 2, row, counter));

        request.setJobPosition(getStringValue(columnNumber + 3, row, counter));
        request.setPositionHeldFrom(getDateValue(columnNumber + 4, row, counter));
        request.setPositionHeldTo(getDateValue(columnNumber + 5, row, counter));
        LocalDate dateValue = getDateValue(columnNumber + 6, row, counter);
        request.setBirthDate(dateValue == null ? null : dateValue.toString());

        request.setAdditionalInformation(getStringValue(columnNumber + 7, row, counter));

        if (counter.getCount() > 0) {
            request.setStatus(Status.ACTIVE);
            contactPersonRequests.add(request);
        }
    }

    private String getStringValue(int columnNumber, Row row, Counter counter) {
        if (row.getCell(columnNumber) != null && row.getCell(columnNumber).getCellType() != CellType.BLANK) {
            ((XSSFCell) row.getCell(columnNumber)).setCellType(CellType.STRING);
            counter.setCount(counter.getCount() + 1);
            return row.getCell(columnNumber).getStringCellValue();

        }
        return null;
    }

    private String getStringValue(int columnNumber, Row row) {
        if (row.getCell(columnNumber) != null && row.getCell(columnNumber).getCellType() != CellType.BLANK) {
            ((XSSFCell) row.getCell(columnNumber)).setCellType(CellType.STRING);
            return row.getCell(columnNumber).getStringCellValue();
        }
        return null;
    }

    private String getStringValueDeclaredConsumption(int columnNumber, Row row, Counter counter) {

        if (row.getCell(columnNumber) != null && row.getCell(columnNumber).getCellType() != CellType.BLANK) {
            counter.setCount(counter.getCount() + 1);
            String result;
            if (row.getCell(columnNumber).getCellType() == CellType.STRING) {
                result = row.getCell(columnNumber).getStringCellValue();
            } else if (row.getCell(columnNumber).getCellType() == CellType.NUMERIC) {
                result = String.valueOf(row.getCell(columnNumber).getNumericCellValue());
            } else {
                return null;
            }

            if (result.length() > 1
                    && result.charAt(result.length() - 2) == '.'
                    && result.charAt(result.length() - 1) == '0') {

                result = result.split("\\.")[0];
            }
            return result;
        }
        return null;
    }

    private LocalDate getDateValue(int columnNumber, Row row, Counter counter) {
        if (row.getCell(columnNumber) != null) {
            ((XSSFCell) row.getCell(columnNumber)).setCellType(CellType.NUMERIC);
            if (row.getCell(columnNumber).getDateCellValue() != null) {
                counter.setCount(counter.getCount() + 1);
                return from(
                        LocalDate.ofInstant(
                                row.getCell(columnNumber).getDateCellValue().toInstant(), ZoneId.systemDefault()));
            }
        }
        return null;
    }

    private List<CreateContactPurposeRequest> getCreateContactPurposeRequests(
            int columnNumber,
            Row row,
            List<String> errorMessages
    ) {
        List<CreateContactPurposeRequest> contactPurposeRequests = new ArrayList<>();
        addContactPurposeRequest(columnNumber, row, contactPurposeRequests, errorMessages);
        addContactPurposeRequest(columnNumber + 1, row, contactPurposeRequests, errorMessages);
        addContactPurposeRequest(columnNumber + 2, row, contactPurposeRequests, errorMessages);
        return contactPurposeRequests;
    }

    private void addContactPurposeRequest(
            int columnNumber,
            Row row,
            List<CreateContactPurposeRequest> contactPurposeRequests,
            List<String> errorMessages
    ) {
        CreateContactPurposeRequest request = new CreateContactPurposeRequest();
        String contactPurposeName = getStringValue(columnNumber, row);
        if (contactPurposeName != null) {
            Optional<CacheObject> optionalContactPurpose = contactPurposeRepository.getByNameAndStatus(
                    contactPurposeName,
                    NomenclatureItemStatus.ACTIVE
            );
            if (optionalContactPurpose.isPresent()) {
                if (contactPurposeRequests.stream().
                        noneMatch(c -> c.getContactPurposeId().equals(optionalContactPurpose.get().getId())))
                    request.setContactPurposeId(optionalContactPurpose.get().getId());
                else
                    errorMessages.add("contactPurpose-Cannot add multiple contact purpose with same name: " + contactPurposeName + ";");
            } else
                errorMessages.add("contactPurpose-Not 5found contact purpose with name: " + contactPurposeName + ";");
            request.setStatus(Status.ACTIVE);
            contactPurposeRequests.add(request);
        }
    }

    private List<CreateRelatedCustomerRequest> getRelatedCustomers(
            int columnNumber,
            Row row,
            List<String> errorMessages
    ) {
        List<CreateRelatedCustomerRequest> relatedCustomerRequests = new ArrayList<>();
        addRelatedCustomerRequest(columnNumber, row, relatedCustomerRequests, errorMessages);
        addRelatedCustomerRequest(columnNumber + 2, row, relatedCustomerRequests, errorMessages);
        addRelatedCustomerRequest(columnNumber + 4, row, relatedCustomerRequests, errorMessages);
        return relatedCustomerRequests;
    }

    private void addRelatedCustomerRequest(
            int columnNumber,
            Row row,
            List<CreateRelatedCustomerRequest> relatedCustomerRequests,
            List<String> errorMessages
    ) {
        CreateRelatedCustomerRequest request = new CreateRelatedCustomerRequest();
        Counter counter = new Counter(0);

        String identifier = getStringValue(columnNumber, row, counter);
        if (identifier != null) {
            Optional<Customer> optionalCustomer = customerRepository.findByIdentifierAndStatus(
                    identifier,
                    CustomerStatus.ACTIVE
            );
            if (optionalCustomer.isPresent()) request.setRelatedCustomerId(optionalCustomer.get().getId());
            else
                errorMessages.add("relatedCustomerIdentifier-Not found related customer with identifier: " + identifier + ";");
        }

        String ciConnectionType = getStringValue(columnNumber + 1, row, counter);
        if (ciConnectionType != null) {
            Optional<CacheObject> optionalCiConnectionType = ciConnectionTypeRepository.getByNameAndStatus(
                    ciConnectionType,
                    NomenclatureItemStatus.ACTIVE
            );
            if (optionalCiConnectionType.isPresent())
                request.setCiConnectionTypeId(optionalCiConnectionType.get().getId());
            else errorMessages.add("connectionType-Not found ci connection type with name: " + ciConnectionType + ";");
        }

        if (counter.getCount() > 0) {
            request.setStatus(Status.ACTIVE);
            relatedCustomerRequests.add(request);
        }
    }

    private List<CreateManagerRequest> getManagers(int columnNumber, Row row, List<String> errorMessages) {
        List<CreateManagerRequest> managerRequests = new ArrayList<>();
        addManagerRequest(columnNumber, row, managerRequests, errorMessages);
        addManagerRequest(columnNumber + 11, row, managerRequests, errorMessages);
        addManagerRequest(columnNumber + 22, row, managerRequests, errorMessages);
        return managerRequests;
    }

    private void addManagerRequest(
            int columnNumber,
            Row row,
            List<CreateManagerRequest> managerRequests,
            List<String> errorMessages
    ) {
        CreateManagerRequest request = new CreateManagerRequest();
        Counter counter = new Counter(0);

        String titleName = getStringValue(columnNumber + 9, row, counter);
        if (titleName != null) {
            Optional<CacheObject> optionalTitle = titleRepository.getByNameAndStatus(
                    titleName,
                    NomenclatureItemStatus.ACTIVE
            );
            if (optionalTitle.isPresent()) request.setTitleId(optionalTitle.get().getId());
            else errorMessages.add("titleName-Not found title with name: " + titleName + ";");
        }

        request.setName(getStringValue(columnNumber, row, counter));

        request.setMiddleName(getStringValue(columnNumber + 1, row, counter));
        request.setSurname(getStringValue(columnNumber + 2, row, counter));
        request.setPersonalNumber(getStringValue(columnNumber + 3, row, counter));
        request.setJobPosition(getStringValue(columnNumber + 4, row, counter));
        request.setPositionHeldFrom(getDateValue(columnNumber + 5, row, counter));
        request.setPositionHeldTo(getDateValue(columnNumber + 6, row, counter));
        LocalDate dateValue = getDateValue(columnNumber + 7, row, counter);
        request.setBirthDate(dateValue == null ? null : dateValue.toString());


        String representationMethodName = getStringValue(columnNumber + 8, row, counter);
        if (representationMethodName != null) {
            Optional<CacheObject> optionalRepresentationMethod = representationMethodRepository.getByNameAndStatus(
                    representationMethodName,
                    NomenclatureItemStatus.ACTIVE
            );
            if (optionalRepresentationMethod.isPresent())
                request.setRepresentationMethodId(optionalRepresentationMethod.get().getId());
            else
                errorMessages.add("representationMethodName-Not found representation method with name: " + representationMethodName + ";");

        }


        request.setAdditionalInformation(getStringValue(columnNumber + 10, row, counter));

        if (counter.getCount() > 0) {
            request.setStatus(Status.ACTIVE);
            managerRequests.add(request);
        }
    }

    private List<CustomerOwnerRequest> getOwners(int columnNumber, Row row, List<String> errorMessages) {
        List<CustomerOwnerRequest> ownerRequests = new ArrayList<>();
        addOwnerRequest(columnNumber, row, ownerRequests, errorMessages);
        addOwnerRequest(columnNumber + 3, row, ownerRequests, errorMessages);
        addOwnerRequest(columnNumber + 6, row, ownerRequests, errorMessages);
        return ownerRequests;
    }

    private void addOwnerRequest(
            int columnNumber,
            Row row,
            List<CustomerOwnerRequest> ownerRequests,
            List<String> errorMessages
    ) {
        Counter counter = new Counter(0);
        CustomerOwnerRequest request = new CustomerOwnerRequest();
        request.setPersonalNumber(getStringValue(columnNumber, row, counter));

        String belongingCapitalOwnerName = getStringValue(columnNumber + 1, row, counter);
        if (belongingCapitalOwnerName != null) {
            Optional<CacheObject> optionalBelongingCapitalOwner = belongingCapitalOwnerRepository.getByNameAndStatus(
                    belongingCapitalOwnerName,
                    NomenclatureItemStatus.ACTIVE
            );
            if (optionalBelongingCapitalOwner.isPresent())
                request.setBelongingOwnerCapitalId(optionalBelongingCapitalOwner.get().getId());
            else
                errorMessages.add("belongingCapitalOwner-Not found belonging capital owner with name: " + belongingCapitalOwnerName + ";");
        }

        request.setAdditionalInformation(getStringValue(columnNumber + 2, row, counter));
        if (counter.getCount() > 0) ownerRequests.add(request);
    }

    private CustomerBankingDetails getCustomerBankingDetails(int columnNumber, Row row, List<String> errorMessages) {
        CustomerBankingDetails bankingDetails = new CustomerBankingDetails();
        Counter counter = new Counter(0);

        bankingDetails.setDirectDebit(getBooleanValue(columnNumber, row, counter, errorMessages, "directDebit"));
        setBankIdAndBankBic(bankingDetails, columnNumber, row, counter);
        bankingDetails.setIban(getStringValue(columnNumber + 2, row, counter));
        bankingDetails.setDeclaredConsumption(getStringValueDeclaredConsumption(columnNumber + 3, row, counter));

        List<Long> preferenceIds = getPreferenceIds(columnNumber + 4, row, errorMessages);
        if (!preferenceIds.isEmpty()) {
            bankingDetails.setPreferenceIds(preferenceIds);
            counter.setCount(counter.getCount() + 1);
        }
        addCreditRating(columnNumber + 7, row, counter, bankingDetails, errorMessages);

        return counter.getCount() > 0 ? bankingDetails : null;
    }

    private void setBankIdAndBankBic(
            CustomerBankingDetails bankingDetails,
            int columnNumber,
            Row row,
            Counter counter
    ) {
        Optional<CacheObjectForBank> optionalBank = bankRepository.getByNameAndStatus(
                getStringValue(
                        columnNumber + 1,
                        row
                ), NomenclatureItemStatus.ACTIVE
        );
        if (optionalBank.isPresent()) {
            CacheObjectForBank bank = optionalBank.get();
            if (bank.getId() != null) {
                bankingDetails.setBankId(bank.getId());
                counter.setCount(counter.getCount() + 1);
            }
            if (bank.getBic() != null) {
                bankingDetails.setBic(bank.getBic());
                counter.setCount(counter.getCount() + 1);
            }
        }
    }

    private List<Long> getPreferenceIds(int columnNumber, Row row, List<String> errorMessages) {
        List<Long> preferenceIds = new ArrayList<>();
        addPreferenceId(columnNumber, row, preferenceIds, errorMessages);
        addPreferenceId(columnNumber + 1, row, preferenceIds, errorMessages);
        addPreferenceId(columnNumber + 2, row, preferenceIds, errorMessages);
        return preferenceIds;
    }

    private void addPreferenceId(int columnNumber, Row row, List<Long> preferenceIds, List<String> errorMessages) {
        String preferenceName = getStringValue(columnNumber, row);
        if (preferenceName != null) {
            Optional<CacheObject> optionalPreferences = preferencesRepository.findByNameAndStatus(
                    preferenceName,
                    NomenclatureItemStatus.ACTIVE
            );
            if (optionalPreferences.isPresent()) {
                if (!preferenceIds.contains(optionalPreferences.get().getId())) {
                    preferenceIds.add(optionalPreferences.get().getId());
                } else
                    errorMessages.add("preference-Cannot add multiple preference with same name: " + preferenceName + ";");
            } else errorMessages.add("preference-Not found preference with name: " + preferenceName + ";");
        }
    }

    private CustomerAddressRequest getCustomerAddressRequest(
            CustomerAddressRequest request, int columnNumber, Row
                    row, List<String> errorMessages
    ) {
        Counter counter = new Counter(0);
        request.setForeign(getBooleanValue(columnNumber, row, counter, errorMessages, "foreign"));
        request.setNumber(getStringValue(columnNumber + 9, row, counter));
        request.setAdditionalInformation(getStringValue(columnNumber + 10, row, counter));
        request.setBlock(getStringValue(columnNumber + 11, row, counter));
        request.setEntrance(getStringValue(columnNumber + 12, row, counter));
        request.setFloor(getStringValue(columnNumber + 13, row, counter));
        request.setApartment(getStringValue(columnNumber + 14, row, counter));
        request.setMailbox(getStringValue(columnNumber + 15, row, counter));
        setAddress(counter, columnNumber, request, row, errorMessages);
        return counter.getCount() > 0 ? request : null;
    }

    private CustomerAddressRequest getCustomerAddressRequestForEditRequest(
            CustomerAddressRequest request,
            int columnNumber, Row row, List<String> errorMessages
    ) {
        if (request == null) request = new CustomerAddressRequest();
        Boolean foreign = getBooleanValue(columnNumber, row, errorMessages, "foreign");
        if (foreign != null) request.setForeign(foreign);

        String number = getStringValue(columnNumber + 9, row);
        if (number != null) request.setNumber(number);

        String additionalInfo = getStringValue(columnNumber + 10, row);
        if (additionalInfo != null) request.setAdditionalInformation(additionalInfo);

        String block = getStringValue(columnNumber + 11, row);
        if (block != null) request.setBlock(block);

        String entrance = getStringValue(columnNumber + 12, row);
        if (entrance != null) request.setEntrance(entrance);

        String floor = getStringValue(columnNumber + 13, row);
        if (floor != null) request.setFloor(floor);

        String apartment = getStringValue(columnNumber + 14, row);
        if (apartment != null) request.setApartment(apartment);

        String mailbox = getStringValue(columnNumber + 15, row);
        if (mailbox != null) request.setMailbox(mailbox);

        setAddressForEditRequest(columnNumber, request, row, errorMessages);

        return request;
    }

    private void setAddressForEditRequest(
            int columnNumber,
            CustomerAddressRequest request,
            Row row,
            List<String> errorMessages
    ) {
        setLocalAddressForEditRequest(columnNumber, request, row, errorMessages);
        setForeignAddressForEditRequest(columnNumber, request, row, errorMessages);
    }

    private void setAddress(
            Counter counter,
            int columnNumber,
            CustomerAddressRequest request,
            Row row,
            List<String> errorMessages
    ) {
        setForeignAddress(columnNumber, request, row, counter, errorMessages);
        setLocalAddress(columnNumber, request, row, counter, errorMessages);
    }

    private void setForeignAddress(
            int columnNumber,
            CustomerAddressRequest request,
            Row row,
            Counter counter,
            List<String> errorMessages
    ) {
        ForeignAddressData foreignAddressData = getForeignAddressData(
                null,
                columnNumber + 1,
                columnNumber + 16,
                row,
                errorMessages
        );
        if (foreignAddressData != null) {
            request.setForeignAddressData(foreignAddressData);
            counter.setCount(counter.getCount() + 1);
        } else request.setForeignAddressData(null);
    }

    private void setForeignAddressForEditRequest(
            int columnNumber,
            CustomerAddressRequest request,
            Row row,
            List<String> errorMessages
    ) {
        ForeignAddressData foreignAddressData = getForeignAddressData(
                request.getForeignAddressData(), columnNumber + 1,
                columnNumber + 16, row, errorMessages
        );
        request.setForeignAddressData(foreignAddressData);
    }

    private void setLocalAddress(
            int columnNumber,
            CustomerAddressRequest request,
            Row row,
            Counter counter,
            List<String> errorMessages
    ) {
        LocalAddressData localAddressData = getLocalAddressData(null, columnNumber + 1, row, errorMessages);
        if (localAddressData != null) {
            request.setLocalAddressData(localAddressData);
            counter.setCount(counter.getCount() + 1);
        } else request.setLocalAddressData(null);
    }

    private void setLocalAddressForEditRequest(
            int columnNumber,
            CustomerAddressRequest request,
            Row row,
            List<String> errorMessages
    ) {
        LocalAddressData localAddressData = getLocalAddressData(
                request.getLocalAddressData(),
                columnNumber + 1,
                row,
                errorMessages
        );
        request.setLocalAddressData(localAddressData);
    }

    private ForeignAddressData getForeignAddressData(
            ForeignAddressData foreignAddressData,
            int countryColumn, int columnNumber, Row row, List<String> errorMessages
    ) {
        ForeignAddressData temp = Objects.requireNonNullElseGet(foreignAddressData, ForeignAddressData::new);

        Counter counter = new Counter(0);

        String countryName = getStringValue(countryColumn, row);
        if (countryName != null) {
            Optional<CacheObject> optionalCountry = countryRepository.getCacheObjectByNameAndStatus(
                    countryName,
                    NomenclatureItemStatus.ACTIVE
            );
            if (optionalCountry.isPresent()) temp.setCountryId(optionalCountry.get().getId());
            else errorMessages.add("countryName-Not found country with name: " + countryName + ";");
        }

        String region = getStringValue(columnNumber, row, counter);
        if (region != null) temp.setRegion(region);

        String municipality = getStringValue(columnNumber + 1, row, counter);
        if (municipality != null) temp.setMunicipality(municipality);

        String populatedPlace = getStringValue(columnNumber + 2, row, counter);
        if (populatedPlace != null) temp.setPopulatedPlace(populatedPlace);

        String zipCode = getStringValue(columnNumber + 3, row, counter);
        if (zipCode != null) temp.setZipCode(zipCode);

        String district = getStringValue(columnNumber + 4, row, counter);
        if (district != null) temp.setDistrict(district);

        String residentialAreaType = getStringValue(columnNumber + 5, row, counter);
        if (residentialAreaType != null) temp.setResidentialAreaType(ResidentialAreaType.valueOf(residentialAreaType));

        String residentialArea = getStringValue(columnNumber + 6, row, counter);
        if (residentialArea != null) temp.setResidentialArea(residentialArea);

        String streetType = getStringValue(columnNumber + 7, row, counter);
        if (streetType != null) temp.setStreetType(StreetType.valueOf(streetType));

        String street = getStringValue(columnNumber + 8, row, counter);
        if (street != null) temp.setStreet(street);
        return (counter.getCount() > 0) ? temp : foreignAddressData;
    }

    private LocalAddressData getLocalAddressData(
            LocalAddressData localAddressData,
            int columnNumber, Row row, List<String> errorMessages
    ) {
        LocalAddressData temp = Objects.requireNonNullElseGet(localAddressData, LocalAddressData::new);

        Counter counter = new Counter(0);
        String countryName = getStringValue(columnNumber, row);
        if (countryName != null) {
            Optional<CacheObject> optionalCountry = countryRepository.getCacheObjectByNameAndStatus(
                    countryName,
                    NomenclatureItemStatus.ACTIVE
            );
            if (optionalCountry.isPresent()) temp.setCountryId(optionalCountry.get().getId());
            else errorMessages.add("countryName-Not found country with name: " + countryName + ";");
        }

        String regionName = getStringValue(columnNumber + 1, row, counter);
        if (regionName != null) {
            Optional<CacheObject> optionalRegion = regionRepository.findByNameAndCountryId(
                    regionName,
                    temp.getCountryId(),
                    NomenclatureItemStatus.ACTIVE
            );
            if (optionalRegion.isPresent()) temp.setRegionId(optionalRegion.get().getId());
            else errorMessages.add("regionName-Not found region with name: " + regionName + " in provided country;");
        }

        String municipalityName = getStringValue(columnNumber + 2, row, counter);
        if (municipalityName != null) {
            Optional<CacheObject> optionalMunicipality = municipalityRepository.getByNameAndRegionId(
                    municipalityName,
                    temp.getRegionId(),
                    NomenclatureItemStatus.ACTIVE
            );
            if (optionalMunicipality.isPresent()) temp.setMunicipalityId(optionalMunicipality.get().getId());
            else
                errorMessages.add("municipalityName-Not found municipality with name: " + municipalityName + " in provided region;");
        }

        String populatedPlaceName = getStringValue(columnNumber + 3, row, counter);
        if (populatedPlaceName != null) {
            Optional<CacheObject> optionalPopulatedPlace = populatedPlaceRepository.getByNameAndMunicipalityId(
                    populatedPlaceName,
                    temp.getMunicipalityId(),
                    NomenclatureItemStatus.ACTIVE
            );
            if (optionalPopulatedPlace.isPresent()) temp.setPopulatedPlaceId(optionalPopulatedPlace.get().getId());
            else
                errorMessages.add("populatedPlaceName-Not found populated place with name: " + populatedPlaceName + " in provided municipality;");
        }

        String zipcodeName = getStringValue(columnNumber + 4, row, counter);
        if (zipcodeName != null) {
            Optional<CacheObject> optionalZipCode = zipCodeRepository.getByNameAndPopulatedPlaceId(
                    zipcodeName,
                    temp.getPopulatedPlaceId(),
                    NomenclatureItemStatus.ACTIVE
            );
            if (optionalZipCode.isPresent()) temp.setZipCodeId(optionalZipCode.get().getId());
            else
                errorMessages.add("zipcodeName-Not found zipcode with name: " + zipcodeName + " in provided populated place;");
        }

        String districtName = getStringValue(columnNumber + 5, row, counter);
        if (districtName != null) {
            Optional<CacheObject> optionalDistrict = districtRepository.getByNameAndPopulatedPlaceId(
                    districtName,
                    temp.getPopulatedPlaceId(),
                    NomenclatureItemStatus.ACTIVE
            );
            if (optionalDistrict.isPresent()) temp.setDistrictId(optionalDistrict.get().getId());
            else
                errorMessages.add("districtName-Not found district with name: " + districtName + " in provided populated place;");
        }

        String residentialAreaName = getStringValue(columnNumber + 6, row, counter);
        if (residentialAreaName != null) {
            Optional<CacheObject> optionalResidentialArea = residentialAreaRepository.getByNameAndPopulatedPlaceId(
                    residentialAreaName,
                    temp.getPopulatedPlaceId(),
                    NomenclatureItemStatus.ACTIVE
            );
            if (optionalResidentialArea.isPresent()) temp.setResidentialAreaId(optionalResidentialArea.get().getId());
            else
                errorMessages.add("residentialAreaName-Not found residential area with name: " + residentialAreaName + " in provided populated place;");
        }

        String streetName = getStringValue(columnNumber + 7, row, counter);
        if (streetName != null) {
            Optional<CacheObject> optionalStreet = streetRepository.getByNameAndPopulatedPlaceId(
                    streetName,
                    temp.getPopulatedPlaceId(),
                    NomenclatureItemStatus.ACTIVE
            );
            if (optionalStreet.isPresent()) temp.setStreetId(optionalStreet.get().getId());
            else
                errorMessages.add("streetName-Not found street with name: " + streetName + " in provided populated place;");
        }

        return (counter.getCount() > 0) ? temp : localAddressData;
    }

    private List<Long> getSegmentIds(int columnNumber, Row row, List<String> errorMessages) {
        List<Long> segmentIds = new ArrayList<>();
        addSegmentId(columnNumber, row, segmentIds, errorMessages);
        addSegmentId(columnNumber + 1, row, segmentIds, errorMessages);
        addSegmentId(columnNumber + 2, row, segmentIds, errorMessages);
        return segmentIds;
    }

    private void addSegmentId(int columnNumber, Row row, List<Long> segmentIds, List<String> errorMessages) {
        String segmentName = getStringValue(columnNumber, row);
        if (segmentName != null) {
            Optional<CacheObject> optionalSegment = segmentRepository.findByNameAndStatus(
                    segmentName,
                    NomenclatureItemStatus.ACTIVE
            );
            if (optionalSegment.isPresent()) {
                if (!segmentIds.contains(optionalSegment.get().getId()))
                    segmentIds.add(optionalSegment.get().getId());
                else
                    errorMessages.add("segmentName-Cannot add multiple segment with same name: " + segmentName + ";");
            } else errorMessages.add("segmentName-Not found segment with name: " + segmentName + ";");
        }
    }

}
