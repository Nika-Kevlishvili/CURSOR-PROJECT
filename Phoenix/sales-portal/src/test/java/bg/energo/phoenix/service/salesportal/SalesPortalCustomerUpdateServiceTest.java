package bg.energo.phoenix.service.salesportal;

import bg.energo.phoenix.exception.ClientException;
import bg.energo.phoenix.exception.ErrorCode;
import bg.energo.phoenix.model.entity.customer.Customer;
import bg.energo.phoenix.model.entity.customer.CustomerDetails;
import bg.energo.phoenix.model.entity.customer.CustomerOwner;
import bg.energo.phoenix.model.entity.customer.Manager;
import bg.energo.phoenix.model.entity.customer.communication.CustomerCommunicationContacts;
import bg.energo.phoenix.model.entity.customer.communication.CustomerCommunications;
import bg.energo.phoenix.model.entity.nomenclature.customer.Bank;
import bg.energo.phoenix.model.enums.customer.*;
import bg.energo.phoenix.repository.customer.CustomerDetailsRepository;
import bg.energo.phoenix.repository.customer.CustomerOwnerRepository;
import bg.energo.phoenix.repository.customer.CustomerRepository;
import bg.energo.phoenix.repository.customer.ManagerRepository;
import bg.energo.phoenix.repository.customer.communicationData.CustomerCommunicationContactsRepository;
import bg.energo.phoenix.repository.customer.communicationData.CustomerCommunicationsRepository;
import bg.energo.phoenix.repository.nomenclature.address.*;
import bg.energo.phoenix.repository.nomenclature.customer.*;
import bg.energo.phoenix.service.customer.ManagerService;
import bg.energo.phoenix.service.salesportal.model.SalesPortalCustomerUpdateResponse;
import bg.energo.phoenix.service.salesportal.requests.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SalesPortalCustomerUpdateServiceTest {

    private static final Long CUSTOMER_ID = 12345L;
    private static final Long VERSION_ID = 47393L;
    private static final String IDENTIFIER = "8889101010";

    @Mock private CustomerRepository customerRepository;
    @Mock private CustomerDetailsRepository customerDetailsRepository;
    @Mock private CustomerCommunicationsRepository customerCommunicationsRepository;
    @Mock private CustomerCommunicationContactsRepository customerCommunicationContactsRepository;
    @Mock private ManagerRepository managerRepository;
    @Mock private CustomerOwnerRepository customerOwnerRepository;
    @Mock private BankRepository bankRepository;
    @Mock private TitleRepository titleRepository;
    @Mock private RepresentationMethodRepository representationMethodRepository;
    @Mock private BelongingCapitalOwnerRepository belongingCapitalOwnerRepository;
    @Mock private EconomicBranchCIRepository economicBranchCIRepository;
    @Mock private EconomicBranchNCEARepository economicBranchNCEARepository;
    @Mock private CountryRepository countryRepository;
    @Mock private DistrictRepository districtRepository;
    @Mock private PopulatedPlaceRepository populatedPlaceRepository;
    @Mock private ResidentialAreaRepository residentialAreaRepository;
    @Mock private StreetRepository streetRepository;
    @Mock private ZipCodeRepository zipCodeRepository;
    @Mock private ManagerService managerService;

    @InjectMocks
    private SalesPortalCustomerUpdateService service;

    private Customer customer;
    private CustomerDetails details;

    @BeforeEach
    void setUp() {
        customer = new Customer();
        customer.setId(CUSTOMER_ID);
        customer.setIdentifier(IDENTIFIER);
        customer.setStatus(CustomerStatus.ACTIVE);
        customer.setCustomerType(CustomerType.PRIVATE_CUSTOMER);

        details = new CustomerDetails();
        details.setId(900L);
        details.setVersionId(VERSION_ID);
        details.setCustomerId(CUSTOMER_ID);

        // Valid address nomenclature IDs used by validRegisteredAddress() resolve (LENIENT:
        // unused in tests that don't touch an address).
        when(countryRepository.existsByIdAndStatusIn(eq(100L), anyList())).thenReturn(true);
        when(populatedPlaceRepository.existsByIdAndStatusIn(eq(200L), anyList())).thenReturn(true);
        when(zipCodeRepository.existsByIdAndStatusIn(eq(300L), anyList())).thenReturn(true);
        // PHN-3427: the valid address is a consistent hierarchy — populated place 200 is in
        // country 100, and zip 300 is in populated place 200.
        when(populatedPlaceRepository.existsByIdAndMunicipalityRegionCountryId(eq(200L), eq(100L), anyList()))
                .thenReturn(true);
        when(zipCodeRepository.existsByIdAndPopulatedPlaceIdAndStatusIn(eq(300L), eq(200L), anyList()))
                .thenReturn(true);
    }

    private SalesPortalCustomerUpdateRequest baseRequest() {
        SalesPortalCustomerUpdateRequest r = new SalesPortalCustomerUpdateRequest();
        r.setCustomerId(CUSTOMER_ID);
        r.setCustomerVersionId(VERSION_ID);
        r.setCustomerPnOrUic(IDENTIFIER);
        return r;
    }

    private void mockResolvedCustomerAndDetails() {
        when(customerRepository.findById(CUSTOMER_ID)).thenReturn(Optional.of(customer));
        when(customerDetailsRepository.findByCustomerIdAndVersionId(CUSTOMER_ID, VERSION_ID))
                .thenReturn(Optional.of(details));
    }

    // ─────────────────────── Customer resolution ───────────────────────

    @Nested
    class CustomerResolution {

        @Test
        void throws_whenCustomerNotFound() {
            when(customerRepository.findById(CUSTOMER_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.updateCustomer(baseRequest()))
                    .isInstanceOf(ClientException.class)
                    .hasMessageContaining("customerId-Customer not found");
        }

        @Test
        void throws_whenCustomerStatusIsDeleted() {
            customer.setStatus(CustomerStatus.DELETED);
            when(customerRepository.findById(CUSTOMER_ID)).thenReturn(Optional.of(customer));

            assertThatThrownBy(() -> service.updateCustomer(baseRequest()))
                    .isInstanceOf(ClientException.class)
                    .hasMessageContaining("Can't update deleted customer")
                    .extracting("errorCode").isEqualTo(ErrorCode.APPLICATION_ERROR);
        }

        @Test
        void throws_whenIdentifierMismatch() {
            when(customerRepository.findById(CUSTOMER_ID)).thenReturn(Optional.of(customer));
            SalesPortalCustomerUpdateRequest req = baseRequest();
            req.setCustomerPnOrUic("WRONG");

            assertThatThrownBy(() -> service.updateCustomer(req))
                    .isInstanceOf(ClientException.class)
                    .hasMessageContaining("Customer identifier does not match");
        }

        @Test
        void throws_whenVersionNotFound() {
            when(customerRepository.findById(CUSTOMER_ID)).thenReturn(Optional.of(customer));
            when(customerDetailsRepository.findByCustomerIdAndVersionId(CUSTOMER_ID, VERSION_ID))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.updateCustomer(baseRequest()))
                    .isInstanceOf(ClientException.class)
                    .hasMessageContaining("Customer version not found");
        }
    }

    // ─────────────────────── Type dispatch ───────────────────────

    @Nested
    class TypeDispatch {

        @Test
        void privateCustomerWithBusinessActivity_routesToIndividualBranch() {
            customer.setCustomerType(CustomerType.PRIVATE_CUSTOMER_WITH_BUSINESS_ACTIVITY);
            mockResolvedCustomerAndDetails();

            SalesPortalCustomerUpdateRequest req = baseRequest();
            req.setName("ИВАН");
            req.setSurname("ГЕОРГИЕВ");
            req.setRegistered(true);
            req.setAddress(validRegisteredAddress());

            SalesPortalCustomerUpdateResponse resp = service.updateCustomer(req);

            assertThat(resp.getStatus()).isEqualTo("success");
            assertThat(details.getName()).isEqualTo("ИВАН");
            assertThat(details.getLastName()).isEqualTo("ГЕОРГИЕВ");
        }
    }

    // ─────────────────────── Individual branch ───────────────────────

    @Nested
    class IndividualBranch {

        @Test
        void happyPath_setsNameSurnameAndAddress() {
            mockResolvedCustomerAndDetails();

            SalesPortalCustomerUpdateRequest req = baseRequest();
            req.setName("ИВАН");
            req.setMiddleName("ПЕТРОВ");
            req.setSurname("ГЕОРГИЕВ");
            req.setRegistered(true);
            req.setAddress(validRegisteredAddress());

            SalesPortalCustomerUpdateResponse resp = service.updateCustomer(req);

            assertThat(resp.getStatus()).isEqualTo("success");
            assertThat(details.getName()).isEqualTo("ИВАН");
            assertThat(details.getMiddleName()).isEqualTo("ПЕТРОВ");
            assertThat(details.getLastName()).isEqualTo("ГЕОРГИЕВ");
            assertThat(details.getCountryId()).isEqualTo(100L);
            assertThat(details.getPopulatedPlaceId()).isEqualTo(200L);
            assertThat(details.getForeignAddress()).isFalse();
        }

        @Test
        void middleName_absent_clearsToNull() {
            details.setMiddleName("Existing");
            mockResolvedCustomerAndDetails();

            SalesPortalCustomerUpdateRequest req = baseRequest();
            req.setName("ИВАН");
            req.setSurname("ГЕОРГИЕВ");
            req.setRegistered(true);
            req.setAddress(validRegisteredAddress());
            // middleName not set ⇒ null in request

            service.updateCustomer(req);

            assertThat(details.getMiddleName()).isNull();
        }

        @Test
        void mandatorySurname_missing_throwsAccumulated() {
            mockResolvedCustomerAndDetails();

            SalesPortalCustomerUpdateRequest req = baseRequest();
            req.setName("ИВАН");
            // surname absent
            req.setRegistered(true);
            req.setAddress(validRegisteredAddress());

            assertThatThrownBy(() -> service.updateCustomer(req))
                    .isInstanceOf(ClientException.class)
                    .hasMessageContaining("surname-Surname is mandatory");
        }

        @Test
        void address_missing_throwsAccumulated() {
            mockResolvedCustomerAndDetails();

            SalesPortalCustomerUpdateRequest req = baseRequest();
            req.setName("ИВАН");
            req.setSurname("ГЕОРГИЕВ");
            // address and registered absent

            assertThatThrownBy(() -> service.updateCustomer(req))
                    .isInstanceOf(ClientException.class)
                    .hasMessageContaining("address-Address is mandatory");
        }
    }

    // ─────────────────────── Address handling ───────────────────────

    @Nested
    class AddressHandling {

        @Test
        void unregistered_writesForeignStringsAndClearsNomenclatureIds() {
            details.setPopulatedPlaceId(999L); // pre-existing ID to verify clearing
            mockResolvedCustomerAndDetails();

            SalesPortalCustomerUpdateRequest req = baseRequest();
            req.setName("ИВАН");
            req.setSurname("ГЕОРГИЕВ");
            req.setRegistered(false);
            SalesPortalAddressRequest addr = new SalesPortalAddressRequest();
            addr.setCountryId(100L);
            addr.setRegion("Sofia Region");
            addr.setMunicipality("Sofia");
            addr.setPopulatedPlace("Sofia City");
            req.setAddress(addr);

            service.updateCustomer(req);

            assertThat(details.getCountryId()).isEqualTo(100L);
            assertThat(details.getPopulatedPlaceId()).isNull();
            assertThat(details.getRegionForeign()).isEqualTo("Sofia Region");
            assertThat(details.getPopulatedPlaceForeign()).isEqualTo("Sofia City");
            assertThat(details.getForeignAddress()).isTrue();
        }

        @Test
        void registered_butStringSentForNomenclatureField_addsError() {
            mockResolvedCustomerAndDetails();

            SalesPortalCustomerUpdateRequest req = baseRequest();
            req.setName("ИВАН");
            req.setSurname("ГЕОРГИЕВ");
            req.setRegistered(true);
            SalesPortalAddressRequest addr = validRegisteredAddress();
            addr.setRegion("Sofia"); // string sent for nomenclature field — invalid
            req.setAddress(addr);

            assertThatThrownBy(() -> service.updateCustomer(req))
                    .isInstanceOf(ClientException.class)
                    .hasMessageContaining("Nomenclature IDs should be provided for region");
        }

        @Test
        void unregistered_butNomenclatureIdSent_addsError() {
            mockResolvedCustomerAndDetails();

            SalesPortalCustomerUpdateRequest req = baseRequest();
            req.setName("ИВАН");
            req.setSurname("ГЕОРГИЕВ");
            req.setRegistered(false);
            SalesPortalAddressRequest addr = new SalesPortalAddressRequest();
            addr.setCountryId(100L);
            addr.setPopulatedPlaceId(200L); // nomenclature ID sent for unregistered — invalid
            req.setAddress(addr);

            assertThatThrownBy(() -> service.updateCustomer(req))
                    .isInstanceOf(ClientException.class)
                    .hasMessageContaining("Strings should be provided for populatedPlace");
        }
    }

    // ─────────── Address geographic-hierarchy validation (PHN-3427) ───────────
    // Mirrors the PUT /customer/{id} flow: populated place must belong to the entered country,
    // and street/district/residentialArea/zipCode must belong to the populated place.

    @Nested
    class AddressHierarchyValidation {

        @Test
        void registered_populatedPlaceNotInCountry_addsError() {
            mockResolvedCustomerAndDetails();
            // Populated place 200 exists and is ACTIVE, but does not belong to country 100.
            when(populatedPlaceRepository.existsByIdAndMunicipalityRegionCountryId(eq(200L), eq(100L), anyList()))
                    .thenReturn(false);

            SalesPortalCustomerUpdateRequest req = baseRequest();
            req.setName("ИВАН");
            req.setSurname("ГЕОРГИЕВ");
            req.setRegistered(true);
            req.setAddress(validRegisteredAddress());

            assertThatThrownBy(() -> service.updateCustomer(req))
                    .isInstanceOf(ClientException.class)
                    .hasMessageContaining("populatedPlaceId-Populated place is not in entered country");
        }

        @Test
        void registered_zipCodeNotInPopulatedPlace_addsError() {
            mockResolvedCustomerAndDetails();
            // Zip 300 exists but does not belong to populated place 200.
            when(zipCodeRepository.existsByIdAndPopulatedPlaceIdAndStatusIn(eq(300L), eq(200L), anyList()))
                    .thenReturn(false);

            SalesPortalCustomerUpdateRequest req = baseRequest();
            req.setName("ИВАН");
            req.setSurname("ГЕОРГИЕВ");
            req.setRegistered(true);
            req.setAddress(validRegisteredAddress());

            assertThatThrownBy(() -> service.updateCustomer(req))
                    .isInstanceOf(ClientException.class)
                    .hasMessageContaining("zipCodeId-Zip code not found in entered populated place");
        }

        @Test
        void registered_districtNotInPopulatedPlace_addsError() {
            mockResolvedCustomerAndDetails();
            when(districtRepository.existsByIdAndPopulatedPlaceIdAndStatusIn(eq(400L), eq(200L), anyList()))
                    .thenReturn(false);

            SalesPortalCustomerUpdateRequest req = baseRequest();
            req.setName("ИВАН");
            req.setSurname("ГЕОРГИЕВ");
            req.setRegistered(true);
            SalesPortalAddressRequest addr = validRegisteredAddress();
            addr.setDistrictId(400L);
            req.setAddress(addr);

            assertThatThrownBy(() -> service.updateCustomer(req))
                    .isInstanceOf(ClientException.class)
                    .hasMessageContaining("districtId-District not found in entered populated place");
        }

        @Test
        void registered_multipleHierarchyMismatches_accumulateAllErrors() {
            mockResolvedCustomerAndDetails();
            // Populated place is valid & in country, but two children don't belong to it —
            // the reporter expects an error for each inconsistent field, not just the first.
            when(districtRepository.existsByIdAndPopulatedPlaceIdAndStatusIn(eq(400L), eq(200L), anyList()))
                    .thenReturn(false);
            when(zipCodeRepository.existsByIdAndPopulatedPlaceIdAndStatusIn(eq(300L), eq(200L), anyList()))
                    .thenReturn(false);

            SalesPortalCustomerUpdateRequest req = baseRequest();
            req.setName("ИВАН");
            req.setSurname("ГЕОРГИЕВ");
            req.setRegistered(true);
            SalesPortalAddressRequest addr = validRegisteredAddress();
            addr.setDistrictId(400L);
            req.setAddress(addr);

            assertThatThrownBy(() -> service.updateCustomer(req))
                    .isInstanceOf(ClientException.class)
                    .hasMessageContaining("districtId-District not found in entered populated place")
                    .hasMessageContaining("zipCodeId-Zip code not found in entered populated place");
        }

        @Test
        void registered_fullConsistentHierarchy_passesAndSetsIds() {
            mockResolvedCustomerAndDetails();
            when(districtRepository.existsByIdAndPopulatedPlaceIdAndStatusIn(eq(400L), eq(200L), anyList()))
                    .thenReturn(true);
            when(residentialAreaRepository.existsByIdAndPopulatedPlaceIdAndStatusIn(eq(500L), eq(200L), anyList()))
                    .thenReturn(true);
            when(streetRepository.existsByIdAndPopulatedPlaceIdAndStatusIn(eq(600L), eq(200L), anyList()))
                    .thenReturn(true);

            SalesPortalCustomerUpdateRequest req = baseRequest();
            req.setName("ИВАН");
            req.setSurname("ГЕОРГИЕВ");
            req.setRegistered(true);
            SalesPortalAddressRequest addr = validRegisteredAddress();
            addr.setDistrictId(400L);
            addr.setResidentialAreaId(500L);
            addr.setStreetId(600L);
            req.setAddress(addr);

            SalesPortalCustomerUpdateResponse resp = service.updateCustomer(req);

            assertThat(resp.getStatus()).isEqualTo("success");
            assertThat(details.getCountryId()).isEqualTo(100L);
            assertThat(details.getPopulatedPlaceId()).isEqualTo(200L);
            assertThat(details.getDistrictId()).isEqualTo(400L);
            assertThat(details.getResidentialAreaId()).isEqualTo(500L);
            assertThat(details.getStreetId()).isEqualTo(600L);
        }

        @Test
        void communicationDataAddress_populatedPlaceNotInCountry_addsScopedError() {
            customer.setCustomerType(CustomerType.LEGAL_ENTITY);
            mockResolvedCustomerAndDetails();
            // The legal-entity comm-data 555 carries validRegisteredAddress() (country 100,
            // populated place 200) — but populated place 200 is not in country 100.
            when(populatedPlaceRepository.existsByIdAndMunicipalityRegionCountryId(eq(200L), eq(100L), anyList()))
                    .thenReturn(false);

            SalesPortalCustomerUpdateRequest req = legalEntityRequestWithMinimumCollections();

            assertThatThrownBy(() -> service.updateCustomer(req))
                    .isInstanceOf(ClientException.class)
                    .hasMessageContaining("communicationData[0].address.populatedPlaceId-Populated place is not in entered country");
        }
    }

    // ─────────────────────── Direct debit ───────────────────────

    @Nested
    class DirectDebitHandling {

        @BeforeEach
        void setUpLegalEntity() {
            customer.setCustomerType(CustomerType.LEGAL_ENTITY);
            mockResolvedCustomerAndDetails();
        }

        @Test
        void absent_leavesExistingValueUnchanged() {
            details.setDirectDebit(true);
            details.setIban("EXISTINGIBAN");

            SalesPortalCustomerUpdateRequest req = legalEntityRequestWithMinimumCollections();
            // directDebit absent

            service.updateCustomer(req);

            assertThat(details.getDirectDebit()).isTrue();
            assertThat(details.getIban()).isEqualTo("EXISTINGIBAN");
        }

        @Test
        void trueWithValidBicAndBank_writesValues() {
            Bank bank = new Bank();
            bank.setId(7L);
            bank.setBic("UBBSBGSF");
            when(bankRepository.findByIdAndStatus(eq(7L), anyList())).thenReturn(Optional.of(bank));

            SalesPortalCustomerUpdateRequest req = legalEntityRequestWithMinimumCollections();
            req.setDirectDebit(true);
            req.setBankId(7L);
            req.setBic("UBBSBGSF");
            req.setIban("BG80BNBG96611020345678");

            service.updateCustomer(req);

            assertThat(details.getDirectDebit()).isTrue();
            assertThat(details.getBank()).isSameAs(bank);
            assertThat(details.getIban()).isEqualTo("BG80BNBG96611020345678");
        }

        @Test
        void trueWithMismatchedBic_throwsAccumulated() {
            Bank bank = new Bank();
            bank.setId(7L);
            bank.setBic("UBBSBGSF");
            when(bankRepository.findByIdAndStatus(eq(7L), anyList())).thenReturn(Optional.of(bank));

            SalesPortalCustomerUpdateRequest req = legalEntityRequestWithMinimumCollections();
            req.setDirectDebit(true);
            req.setBankId(7L);
            req.setBic("WRONGBIC");
            req.setIban("BG80BNBG96611020345678");

            assertThatThrownBy(() -> service.updateCustomer(req))
                    .isInstanceOf(ClientException.class)
                    .hasMessageContaining("bic-BIC does not match");
        }

        @Test
        void false_clearsBankAndIban() {
            details.setDirectDebit(true);
            details.setIban("OLD");

            SalesPortalCustomerUpdateRequest req = legalEntityRequestWithMinimumCollections();
            req.setDirectDebit(false);

            service.updateCustomer(req);

            assertThat(details.getDirectDebit()).isFalse();
            assertThat(details.getBank()).isNull();
            assertThat(details.getIban()).isNull();
        }
    }

    // ─────────────────────── Communication data ───────────────────────

    @Nested
    class CommunicationDataHandling {

        @BeforeEach
        void setUpLegalEntity() {
            customer.setCustomerType(CustomerType.LEGAL_ENTITY);
            mockResolvedCustomerAndDetails();
        }

        @Test
        void communicationDataId_notFound_addsError() {
            SalesPortalCustomerUpdateRequest req = legalEntityRequestWithMinimumCollections();
            req.setCommunicationData(List.of(commDataRequest(555L, "+359888111111", "a@b.bg")));
            // Override the helper's stubbing AFTER it's been set up
            when(customerCommunicationsRepository.findByIdAndStatuses(eq(555L), anyList()))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.updateCustomer(req))
                    .isInstanceOf(ClientException.class)
                    .hasMessageContaining("Communication data not found");
        }

        @Test
        void appendsPhonesAndEmails_withDuplicatesAllowed() {
            CustomerCommunications cc = new CustomerCommunications();
            cc.setId(555L);
            cc.setCustomerDetailsId(details.getId());
            when(customerCommunicationsRepository.findByIdAndStatuses(eq(555L), anyList()))
                    .thenReturn(Optional.of(cc));

            SalesPortalCommunicationDataRequest cd = new SalesPortalCommunicationDataRequest();
            cd.setCommunicationDataId(555L);
            cd.setRegistered(true);
            cd.setAddress(validRegisteredAddress());
            cd.setPhones(List.of("+359888111111", "+359888111111")); // duplicate intentionally
            cd.setEmails(List.of("a@b.bg"));

            SalesPortalCustomerUpdateRequest req = legalEntityRequestWithMinimumCollections();
            req.setCommunicationData(List.of(cd));

            service.updateCustomer(req);

            ArgumentCaptor<List<CustomerCommunicationContacts>> captor =
                    ArgumentCaptor.forClass(List.class);
            verify(customerCommunicationContactsRepository, org.mockito.Mockito.atLeastOnce())
                    .saveAll(captor.capture());

            // Collect all saved rows across all invocations (phones + emails are two saveAll calls)
            List<CustomerCommunicationContacts> phones = captor.getAllValues().stream()
                    .flatMap(List::stream)
                    .filter(c -> c.getContactType() == CustomerCommContactTypes.MOBILE_NUMBER)
                    .toList();
            List<CustomerCommunicationContacts> emails = captor.getAllValues().stream()
                    .flatMap(List::stream)
                    .filter(c -> c.getContactType() == CustomerCommContactTypes.EMAIL)
                    .toList();
            assertThat(phones).hasSize(2); // duplicates kept
            assertThat(emails).hasSize(1);
            assertThat(phones).allMatch(p -> p.getStatus() == Status.ACTIVE);
            assertThat(phones).allMatch(p -> p.getCustomerCommunicationsId().equals(555L));
        }

        @Test
        void communicationData_notBelongingToCustomer_addsError() {
            SalesPortalCustomerUpdateRequest req = legalEntityRequestWithMinimumCollections();
            req.setCommunicationData(List.of(commDataRequest(555L, "+359888111111", "a@b.bg")));
            // Override helper's stubbing AFTER setup: return a comm-data that belongs to a different version
            CustomerCommunications wrongVersionCc = new CustomerCommunications();
            wrongVersionCc.setId(555L);
            wrongVersionCc.setCustomerDetailsId(99999L);
            when(customerCommunicationsRepository.findByIdAndStatuses(eq(555L), anyList()))
                    .thenReturn(Optional.of(wrongVersionCc));

            assertThatThrownBy(() -> service.updateCustomer(req))
                    .isInstanceOf(ClientException.class)
                    .hasMessageContaining("does not belong to this customer version");
        }
    }

    // ─────────────────────── Managers ───────────────────────

    @Nested
    class ManagerHandling {

        @BeforeEach
        void setUpLegalEntity() {
            customer.setCustomerType(CustomerType.LEGAL_ENTITY);
            mockResolvedCustomerAndDetails();
        }

        @Test
        void manager_notFound_addsError() {
            SalesPortalCustomerUpdateRequest req = legalEntityRequestWithMinimumCollections();
            req.setManagers(List.of(managerRequest(77L)));
            // Override the helper's present-manager stub so the lookup misses.
            when(managerRepository.findById(77L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.updateCustomer(req))
                    .isInstanceOf(ClientException.class)
                    .hasMessageContaining("Manager not found");
        }

        @Test
        void manager_appendsContactsViaManagerService() {
            Manager manager = new Manager();
            manager.setId(77L);
            manager.setCustomerDetailId(details.getId());
            manager.setStatus(Status.ACTIVE);
            when(managerRepository.findById(77L)).thenReturn(Optional.of(manager));
            when(representationMethodRepository.existsByIdAndStatusIn(eq(11L), anyList())).thenReturn(true);
            when(titleRepository.existsByIdAndStatusIn(eq(22L), anyList())).thenReturn(true);

            SalesPortalManagerUpdateRequest mgr = managerRequest(77L);
            mgr.setMobileNumbers(List.of("+359888111", "+359888222"));
            mgr.setEmails(List.of("m@co.bg"));

            SalesPortalCustomerUpdateRequest req = legalEntityRequestWithMinimumCollections();
            req.setManagers(List.of(mgr));

            service.updateCustomer(req);

            verify(managerService).appendManagerContacts(77L, ManagerContactType.MOBILE_NUMBER,
                    List.of("+359888111", "+359888222"));
            verify(managerService).appendManagerContacts(77L, ManagerContactType.EMAIL,
                    List.of("m@co.bg"));
        }
    }

    // ─────────────────────── Owners ───────────────────────

    @Nested
    class OwnerHandling {

        @BeforeEach
        void setUpLegalEntity() {
            customer.setCustomerType(CustomerType.LEGAL_ENTITY);
            mockResolvedCustomerAndDetails();
        }

        @Test
        void owner_identifierResolvesToExistingCustomer_repointsFk() {
            // Build request via helper, then override the owner stub AFTER so our owner instance
            // is the one observed by the service (helper creates its own internal owner).
            SalesPortalCustomerUpdateRequest req = legalEntityRequestWithMinimumCollections();
            req.setOwners(List.of(ownerRequest(33L, "OWNER123", "Some Co", 5L)));

            CustomerOwner owner = new CustomerOwner();
            owner.setId(33L);
            owner.setCustomer(customer);
            when(customerOwnerRepository.findByIdAndStatuses(eq(33L), anyList()))
                    .thenReturn(Optional.of(owner));

            service.updateCustomer(req);

            assertThat(owner.getOwnerCustomer()).isNotNull();
            assertThat(owner.getOwnerCustomer().getId()).isEqualTo(999L);
        }

        @Test
        void owner_identifierNotFound_addsError() {
            CustomerOwner owner = new CustomerOwner();
            owner.setId(33L);
            owner.setCustomer(customer);
            when(customerOwnerRepository.findByIdAndStatuses(eq(33L), anyList()))
                    .thenReturn(Optional.of(owner));
            when(belongingCapitalOwnerRepository.existsByIdAndStatusIn(eq(5L), anyList())).thenReturn(true);
            when(customerRepository.findByIdentifierAndStatus("MISSING", CustomerStatus.ACTIVE))
                    .thenReturn(Optional.empty());

            SalesPortalCustomerUpdateRequest req = legalEntityRequestWithMinimumCollections();
            req.setOwners(List.of(ownerRequest(33L, "MISSING", "X", 5L)));

            assertThatThrownBy(() -> service.updateCustomer(req))
                    .isInstanceOf(ClientException.class)
                    .hasMessageContaining("Customer not found by identifier");
        }
    }

    // ─────────────────────── Cross-type silent ignore ───────────────────────

    @Nested
    class CrossTypeIgnore {

        @Test
        void individualFieldsSentForLegalEntity_areIgnored_legalEntityFieldsApplied() {
            customer.setCustomerType(CustomerType.LEGAL_ENTITY);
            mockResolvedCustomerAndDetails();

            SalesPortalCustomerUpdateRequest req = legalEntityRequestWithMinimumCollections();
            req.setName("ShouldBeIgnored");
            req.setSurname("AlsoIgnored");
            req.setAdditionalComment("New VIP comment");

            service.updateCustomer(req);

            assertThat(customer.getAdditionalInfo()).isEqualTo("New VIP comment");
            assertThat(details.getName()).isNull();
            assertThat(details.getLastName()).isNull();
        }

        @Test
        void legalEntityFieldsSentForIndividual_areIgnored_individualFieldsApplied() {
            mockResolvedCustomerAndDetails();

            SalesPortalCustomerUpdateRequest req = baseRequest();
            req.setName("ИВАН");
            req.setSurname("ГЕОРГИЕВ");
            req.setRegistered(true);
            req.setAddress(validRegisteredAddress());
            req.setAdditionalComment("This should be ignored");
            req.setManagers(List.of(managerRequest(77L))); // should be ignored

            service.updateCustomer(req);

            assertThat(details.getName()).isEqualTo("ИВАН");
            // managers list was ignored — managerRepository never called
            verify(managerRepository, never()).findById(any());
        }
    }

    // ─────────────────────── Nomenclature FK validation (PHN-3125) ───────────────────────

    @Nested
    class NomenclatureFkValidation {

        @Test
        void economicBranchCi_notFound_addsCleanError() {
            customer.setCustomerType(CustomerType.LEGAL_ENTITY);
            mockResolvedCustomerAndDetails();
            when(economicBranchCIRepository.existsByIdAndStatusIn(eq(1L), anyList())).thenReturn(false);

            SalesPortalCustomerUpdateRequest req = legalEntityRequestWithMinimumCollections();
            req.setEconomicBranchCiId(1L);

            assertThatThrownBy(() -> service.updateCustomer(req))
                    .isInstanceOf(ClientException.class)
                    .hasMessageContaining("economicBranchCiId-Economic Branch CI not found");
        }

        @Test
        void economicBranchNcea_notFound_addsCleanError() {
            customer.setCustomerType(CustomerType.LEGAL_ENTITY);
            mockResolvedCustomerAndDetails();
            when(economicBranchNCEARepository.existsByIdAndStatusIn(eq(2L), anyList())).thenReturn(false);

            SalesPortalCustomerUpdateRequest req = legalEntityRequestWithMinimumCollections();
            req.setEconomicBranchNceaId(2L);

            assertThatThrownBy(() -> service.updateCustomer(req))
                    .isInstanceOf(ClientException.class)
                    .hasMessageContaining("economicBranchNceaId-Economic Branch NCEA not found");
        }

        @Test
        void economicBranchCi_valid_isApplied() {
            customer.setCustomerType(CustomerType.LEGAL_ENTITY);
            mockResolvedCustomerAndDetails();
            when(economicBranchCIRepository.existsByIdAndStatusIn(eq(10L), anyList())).thenReturn(true);

            SalesPortalCustomerUpdateRequest req = legalEntityRequestWithMinimumCollections();
            req.setEconomicBranchCiId(10L);

            service.updateCustomer(req);

            assertThat(details.getEconomicBranchCiId()).isEqualTo(10L);
        }

        @Test
        void addressCountry_notFound_addsCleanError() {
            mockResolvedCustomerAndDetails();
            when(countryRepository.existsByIdAndStatusIn(eq(100L), anyList())).thenReturn(false);

            SalesPortalCustomerUpdateRequest req = baseRequest();
            req.setName("ИВАН");
            req.setSurname("ГЕОРГИЕВ");
            req.setRegistered(true);
            req.setAddress(validRegisteredAddress());

            assertThatThrownBy(() -> service.updateCustomer(req))
                    .isInstanceOf(ClientException.class)
                    .hasMessageContaining("countryId-Country not found");
        }
    }

    // ─────────────────────── Mandatory managers (PHN-3128) ───────────────────────

    @Nested
    class MandatoryManagers {

        @Test
        void legalEntity_withoutManagers_addsError() {
            customer.setCustomerType(CustomerType.LEGAL_ENTITY);
            mockResolvedCustomerAndDetails();

            SalesPortalCustomerUpdateRequest req = legalEntityRequestWithMinimumCollections();
            req.setManagers(null);

            assertThatThrownBy(() -> service.updateCustomer(req))
                    .isInstanceOf(ClientException.class)
                    .hasMessageContaining("managers-Managers are mandatory for legal entities");
        }
    }

    // ─────────────────────── Cross-type name validation (PHN-3129) ───────────────────────

    @Nested
    class CrossTypeNameValidation {

        @Test
        void legalEntity_withDisallowedSymbolsInName_isIgnored_notRejected() {
            customer.setCustomerType(CustomerType.LEGAL_ENTITY);
            mockResolvedCustomerAndDetails();

            SalesPortalCustomerUpdateRequest req = legalEntityRequestWithMinimumCollections();
            req.setMiddleName("SHOULD_BE_IGNORED");
            req.setSurname("ALSO_IGNORED");

            SalesPortalCustomerUpdateResponse resp = service.updateCustomer(req);

            assertThat(resp.getStatus()).isEqualTo("success");
            assertThat(details.getMiddleName()).isNull();
            assertThat(details.getLastName()).isNull();
        }

        @Test
        void individual_withDisallowedSymbolsInName_isRejected() {
            mockResolvedCustomerAndDetails();

            SalesPortalCustomerUpdateRequest req = baseRequest();
            req.setName("SHOULD_BE_IGNORED"); // underscore is not an allowed symbol for individuals
            req.setSurname("ГЕОРГИЕВ");
            req.setRegistered(true);
            req.setAddress(validRegisteredAddress());

            assertThatThrownBy(() -> service.updateCustomer(req))
                    .isInstanceOf(ClientException.class)
                    .hasMessageContaining("name-Name contains not allowed symbols");
        }
    }

    // ─────────────────────── Test fixtures ───────────────────────

    private SalesPortalAddressRequest validRegisteredAddress() {
        SalesPortalAddressRequest a = new SalesPortalAddressRequest();
        a.setCountryId(100L);
        a.setPopulatedPlaceId(200L);
        a.setZipCodeId(300L);
        return a;
    }

    private SalesPortalCommunicationDataRequest commDataRequest(Long commId, String phone, String email) {
        SalesPortalCommunicationDataRequest cd = new SalesPortalCommunicationDataRequest();
        cd.setCommunicationDataId(commId);
        cd.setRegistered(true);
        cd.setAddress(validRegisteredAddress());
        cd.setPhones(List.of(phone));
        cd.setEmails(List.of(email));
        return cd;
    }

    private SalesPortalManagerUpdateRequest managerRequest(Long managerId) {
        SalesPortalManagerUpdateRequest m = new SalesPortalManagerUpdateRequest();
        m.setManagerId(managerId);
        m.setName("Mgr");
        m.setSurname("Last");
        m.setJobPosition("CEO");
        m.setRepresentationMethodId(11L);
        m.setTitleId(22L);
        return m;
    }

    private SalesPortalOwnerUpdateRequest ownerRequest(Long ownerId, String identifier, String name, Long belongingCapitalOwnerId) {
        SalesPortalOwnerUpdateRequest o = new SalesPortalOwnerUpdateRequest();
        o.setOwnerId(ownerId);
        o.setOwnerIdentifier(identifier);
        o.setOwnerName(name);
        o.setBelongingCapitalOwnerId(belongingCapitalOwnerId);
        return o;
    }

    /**
     * Returns a legal-entity request with the minimum set of mandatory collections populated
     * (communicationData, managers, owners — all three are mandatory for legal entities), so
     * individual sub-area tests don't trigger spurious "… is mandatory" errors.
     */
    private SalesPortalCustomerUpdateRequest legalEntityRequestWithMinimumCollections() {
        CustomerCommunications cc = new CustomerCommunications();
        cc.setId(555L);
        cc.setCustomerDetailsId(details.getId());
        when(customerCommunicationsRepository.findByIdAndStatuses(eq(555L), anyList()))
                .thenReturn(Optional.of(cc));

        Manager manager = new Manager();
        manager.setId(77L);
        manager.setCustomerDetailId(details.getId());
        manager.setStatus(Status.ACTIVE);
        when(managerRepository.findById(77L)).thenReturn(Optional.of(manager));
        when(representationMethodRepository.existsByIdAndStatusIn(eq(11L), anyList())).thenReturn(true);
        when(titleRepository.existsByIdAndStatusIn(eq(22L), anyList())).thenReturn(true);

        CustomerOwner owner = new CustomerOwner();
        owner.setId(33L);
        owner.setCustomer(customer);
        when(customerOwnerRepository.findByIdAndStatuses(eq(33L), anyList()))
                .thenReturn(Optional.of(owner));
        when(belongingCapitalOwnerRepository.existsByIdAndStatusIn(eq(5L), anyList())).thenReturn(true);
        when(economicBranchCIRepository.existsByIdAndStatusIn(eq(50L), anyList())).thenReturn(true);

        Customer ownerCustomer = new Customer();
        ownerCustomer.setId(999L);
        ownerCustomer.setIdentifier("OWNER123");
        ownerCustomer.setStatus(CustomerStatus.ACTIVE);
        when(customerRepository.findByIdentifierAndStatus("OWNER123", CustomerStatus.ACTIVE))
                .thenReturn(Optional.of(ownerCustomer));

        SalesPortalManagerUpdateRequest mgr = managerRequest(77L);
        mgr.setMobileNumbers(List.of("+359888111111"));
        mgr.setEmails(List.of("mgr@co.bg"));

        SalesPortalCustomerUpdateRequest req = baseRequest();
        req.setMainSubjectOfActivity("Energy distribution");
        req.setEconomicBranchCiId(50L);
        req.setCommunicationData(List.of(commDataRequest(555L, "+359888111111", "a@b.bg")));
        req.setManagers(List.of(mgr));
        req.setOwners(List.of(ownerRequest(33L, "OWNER123", "Some Co", 5L)));
        return req;
    }
}
