package bg.energo.phoenix.service.salesportal;

import bg.energo.phoenix.model.enums.pod.pod.PodStatus;
import bg.energo.phoenix.model.enums.translation.Language;
import bg.energo.phoenix.repository.contract.billing.ContractPodRepository;
import bg.energo.phoenix.repository.contract.product.ProductContractDetailsRepository;
import bg.energo.phoenix.repository.contract.product.ProductContractFileRepository;
import bg.energo.phoenix.repository.contract.product.ProductContractRepository;
import bg.energo.phoenix.repository.contract.product.ProductContractSignableDocumentRepository;
import bg.energo.phoenix.repository.contract.proxy.ProductContractProxyRepository;
import bg.energo.phoenix.repository.customer.CustomerDetailsRepository;
import bg.energo.phoenix.repository.customer.CustomerRepository;
import bg.energo.phoenix.repository.customer.ManagerContactRepository;
import bg.energo.phoenix.repository.customer.ManagerRepository;
import bg.energo.phoenix.repository.customer.communicationData.CustomerCommunicationContactsRepository;
import bg.energo.phoenix.repository.customer.communicationData.CustomerCommunicationsRepository;
import bg.energo.phoenix.repository.documents.DocumentsRepository;
import bg.energo.phoenix.repository.nomenclature.customer.BankRepository;
import bg.energo.phoenix.repository.nomenclature.customer.legalForm.LegalFormRepository;
import bg.energo.phoenix.repository.nomenclature.customer.legalForm.LegalFormTransliteratedRepository;
import bg.energo.phoenix.repository.pod.pod.PointOfDeliveryDetailsRepository;
import bg.energo.phoenix.repository.pod.pod.PointOfDeliveryRepository;
import bg.energo.phoenix.repository.product.product.ProductDetailsRepository;
import bg.energo.phoenix.repository.receivable.collectionChannel.CollectionChannelRepository;
import bg.energo.phoenix.repository.receivable.customerLiability.CustomerLiabilityRepository;
import bg.energo.phoenix.repository.receivable.payment.PaymentRepository;
import bg.energo.phoenix.service.archivation.edms.EDMSFileArchivationService;
import bg.energo.phoenix.service.customer.ManagerService;
import bg.energo.phoenix.service.document.ftpService.FileService;
import bg.energo.phoenix.service.lock.LockService;
import bg.energo.phoenix.service.salesportal.model.SalesPortalPodFullProjection;
import bg.energo.phoenix.service.salesportal.model.SalesPortalPodResponse;
import bg.energo.phoenix.service.translation.TranslationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SalesPortalServicePodByIdentifierTest {

    private static final String POD_IDENTIFIER = "32X3134200";

    @Mock
    private PointOfDeliveryRepository pointOfDeliveryRepository;
    @Mock
    private CustomerRepository customerRepository;
    @Mock
    private ProductContractRepository productContractRepository;
    @Mock
    private ProductContractSignableDocumentRepository productContractSignableDocumentRepository;
    @Mock
    private PointOfDeliveryDetailsRepository pointOfDeliveryDetailsRepository;
    @Mock
    private CustomerLiabilityRepository customerLiabilityRepository;
    @Mock
    private PaymentRepository paymentRepository;
    @Mock
    private CollectionChannelRepository collectionChannelRepository;
    @Mock
    private LockService lockService;
    @Mock
    private DocumentsRepository documentsRepository;
    @Mock
    private FileService fileService;
    @Mock
    private EDMSFileArchivationService edmsFileArchivationService;
    @Mock
    private ObjectMapper objectMapper;
    @Mock
    private TranslationService translationService;
    @Mock
    private ProductContractDetailsRepository productContractDetailsRepository;
    @Mock
    private ContractPodRepository contractPodRepository;
    @Mock
    private ProductContractProxyRepository productContractProxyRepository;
    @Mock
    private ProductContractFileRepository productContractFileRepository;
    @Mock
    private CustomerCommunicationContactsRepository customerCommunicationContactsRepository;
    @Mock
    private CustomerCommunicationsRepository customerCommunicationsRepository;
    @Mock
    private ManagerRepository managerRepository;
    @Mock
    private ManagerService managerService;
    @Mock
    private ManagerContactRepository managerContactRepository;
    @Mock
    private CustomerDetailsRepository customerDetailsRepository;
    @Mock
    private BankRepository bankRepository;
    @Mock
    private ProductDetailsRepository productDetailsRepository;
    @Mock
    private LegalFormRepository legalFormRepository;
    @Mock
    private LegalFormTransliteratedRepository legalFormTransliteratedRepository;

    @InjectMocks
    private SalesPortalService salesPortalService;

    @Test
    void getPodByIdentifierByQuery_shouldOmitContractFields_whenPodDeactivationDateIsInPast() {
        when(pointOfDeliveryRepository.existsByIdentifierIgnoreCaseAndStatusIn(
                POD_IDENTIFIER, List.of(PodStatus.ACTIVE))).thenReturn(true);

        SalesPortalPodFullProjection projection = deactivatedPodProjection();
        Page<SalesPortalPodFullProjection> page = new PageImpl<>(List.of(projection));
        when(pointOfDeliveryRepository.findFullPodDataByIdentifier(
                eq(List.of(POD_IDENTIFIER)),
                isNull(),
                eq("en"),
                any(),
                any(Pageable.class)
        )).thenReturn(page);

        SalesPortalPodResponse response = salesPortalService.getPodByIdentifierByQuery(
                POD_IDENTIFIER, Language.ENGLISH);

        assertThat(response.getPodIdentifier()).isEqualTo(POD_IDENTIFIER);
        assertThat(response.getPodParameters().getContractId()).isNull();
        assertThat(response.getPodParameters().getContractVersionId()).isNull();
        assertThat(response.getPodParameters().getContractStatus()).isNull();
        assertThat(response.getPodParameters().getActivationDate()).isNull();
        assertThat(response.getPodParameters().getDeactivationDate()).isNull();
    }

    @Test
    void getPodByIdentifierByQuery_shouldIncludeContractFields_whenPodIsCurrentlyActive() {
        when(pointOfDeliveryRepository.existsByIdentifierIgnoreCaseAndStatusIn(
                POD_IDENTIFIER, List.of(PodStatus.ACTIVE))).thenReturn(true);

        SalesPortalPodFullProjection projection = activePodProjection();
        Page<SalesPortalPodFullProjection> page = new PageImpl<>(List.of(projection));
        when(pointOfDeliveryRepository.findFullPodDataByIdentifier(
                eq(List.of(POD_IDENTIFIER)),
                isNull(),
                eq("en"),
                any(),
                any(Pageable.class)
        )).thenReturn(page);

        SalesPortalPodResponse response = salesPortalService.getPodByIdentifierByQuery(
                POD_IDENTIFIER, Language.ENGLISH);

        assertThat(response.getPodParameters().getContractId()).isEqualTo(20455L);
        assertThat(response.getPodParameters().getContractVersionId()).isEqualTo(9001L);
        assertThat(response.getPodParameters().getContractStatus()).isEqualTo("ACTIVE_IN_TERM");
        assertThat(response.getPodParameters().getActivationDate()).isNull();
        assertThat(response.getPodParameters().getDeactivationDate()).isNull();
    }

    private static SalesPortalPodFullProjection deactivatedPodProjection() {
        LocalDate activationDate = LocalDate.now().minusMonths(6);
        LocalDate deactivationDate = LocalDate.now().minusMonths(1);
        return podProjection(20455L, 9001L, "ACTIVE_IN_TERM", activationDate, deactivationDate);
    }

    private static SalesPortalPodFullProjection activePodProjection() {
        LocalDate activationDate = LocalDate.now().minusMonths(1);
        return podProjection(20455L, 9001L, "ACTIVE_IN_TERM", activationDate, null);
    }

    private static SalesPortalPodFullProjection podProjection(
            Long contractId,
            Long contractVersionId,
            String contractStatus,
            LocalDate activationDate,
            LocalDate deactivationDate
    ) {
        return new SalesPortalPodFullProjection() {
            @Override public Long getPodId() { return 1L; }
            @Override public Long getPodDetailId() { return 11L; }
            @Override public String getPodIdentifier() { return POD_IDENTIFIER; }
            @Override public String getPodStatus() { return "Available"; }
            @Override public Long getCountryId() { return null; }
            @Override public Long getPopulatedPlaceId() { return null; }
            @Override public String getPopulatedPlace() { return null; }
            @Override public Long getMunicipalityId() { return null; }
            @Override public String getMunicipality() { return null; }
            @Override public Long getRegionId() { return null; }
            @Override public String getRegion() { return null; }
            @Override public Long getZipCodeId() { return null; }
            @Override public String getZipCode() { return null; }
            @Override public Long getDistrictId() { return null; }
            @Override public String getDistrict() { return null; }
            @Override public Long getResidentialAreaId() { return null; }
            @Override public String getQuarterResidentialArea() { return null; }
            @Override public Long getStreetId() { return null; }
            @Override public String getStreetBoulevard() { return null; }
            @Override public String getBuildingNumber() { return null; }
            @Override public String getBlock() { return null; }
            @Override public String getEntrance() { return null; }
            @Override public String getApartment() { return null; }
            @Override public String getMailbox() { return null; }
            @Override public String getLatitude() { return null; }
            @Override public String getLongitude() { return null; }
            @Override public Long getGridOperatorId() { return 1L; }
            @Override public String getGridOperator() { return "GO"; }
            @Override public String getTypeOfPod() { return "CONSUMER"; }
            @Override public String getVoltageLevel() { return "LOW"; }
            @Override public String getPurpose() { return "DOMESTIC"; }
            @Override public String getMeasurementType() { return "STANDARD"; }
            @Override public Integer getEstimatedAverageConsumption() { return 1000; }
            @Override public Long getContractId() { return contractId; }
            @Override public String getContractNumber() { return "20455"; }
            @Override public String getContractStatus() { return contractStatus; }
            @Override public LocalDate getActivationDate() { return activationDate; }
            @Override public LocalDate getDeactivationDate() { return deactivationDate; }
            @Override public String getCustomerName() { return null; }
            @Override public String getCustomerMiddleName() { return null; }
            @Override public String getCustomerLastName() { return null; }
            @Override public Long getCustomerLegalFormId() { return null; }
            @Override public String getCustomerClientNumber() { return null; }
            @Override public Boolean getCustomerBlacklist() { return null; }
            @Override public String getListStreetBoulevard() { return null; }
            @Override public Boolean getForeignAddress() { return false; }
            @Override public String getPodName() { return "POD"; }
            @Override public Long getContractVersionId() { return contractVersionId; }
        };
    }
}
