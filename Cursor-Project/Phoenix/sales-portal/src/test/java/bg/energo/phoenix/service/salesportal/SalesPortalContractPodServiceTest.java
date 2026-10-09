package bg.energo.phoenix.service.salesportal;

import bg.energo.phoenix.model.CacheObjectForPod;
import bg.energo.phoenix.model.enums.pod.pod.PODType;
import bg.energo.phoenix.model.request.contract.product.ContractPodRequest;
import bg.energo.phoenix.model.response.contract.pods.ContractPodsResponseImpl;
import bg.energo.phoenix.model.response.contract.productContract.ProductContractResponse;
import bg.energo.phoenix.repository.contract.billing.ContractPodRepository;
import bg.energo.phoenix.repository.contract.product.ProductContractDetailsRepository;
import bg.energo.phoenix.repository.pod.pod.PointOfDeliveryDetailsRepository;
import bg.energo.phoenix.repository.pod.pod.PointOfDeliveryRepository;
import bg.energo.phoenix.repository.product.product.ProductDetailsRepository;
import bg.energo.phoenix.service.contract.product.ProductContractPodService;
import bg.energo.phoenix.service.salesportal.requests.SalesPortalContractUpdateRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SalesPortalContractPodServiceTest {

    private static final Long OLD_POD_DETAIL_ID = 10L;
    private static final Long NEW_POD_DETAIL_ID = 20L;
    private static final String OLD_POD_IDENTIFIER = "OLD-POD";
    private static final String NEW_POD_IDENTIFIER = "NEW-POD";

    @Mock private ProductContractPodService productContractPodService;
    @Mock private ProductContractDetailsRepository productContractDetailsRepository;
    @Mock private ProductDetailsRepository productDetailsRepository;
    @Mock private PointOfDeliveryDetailsRepository pointOfDeliveryDetailsRepository;
    @Mock private PointOfDeliveryRepository pointOfDeliveryRepository;
    @Mock private ContractPodRepository contractPodRepository;

    private SalesPortalContractPodService service;

    @BeforeEach
    void setUp() {
        service = new SalesPortalContractPodService(
                productContractPodService,
                productContractDetailsRepository,
                productDetailsRepository,
                pointOfDeliveryDetailsRepository,
                pointOfDeliveryRepository,
                contractPodRepository
        );
    }

    @Test
    void buildPodRequestsFromIdentifiers_shouldUseRequestedPodsNotOnlyExistingContractPods() {
        SalesPortalContractUpdateRequest request = new SalesPortalContractUpdateRequest();
        request.setPodIdentifiers(List.of(OLD_POD_IDENTIFIER, NEW_POD_IDENTIFIER));

        ProductContractResponse existingContract = new ProductContractResponse();
        existingContract.setContractPodsResponses(List.of(
                contractPodResponse(OLD_POD_IDENTIFIER, OLD_POD_DETAIL_ID, 99L)
        ));

        when(pointOfDeliveryDetailsRepository.findByPodIdentifiers(List.of(OLD_POD_IDENTIFIER, NEW_POD_IDENTIFIER)))
                .thenReturn(List.of(
                        cacheObject(OLD_POD_DETAIL_ID, OLD_POD_IDENTIFIER),
                        cacheObject(NEW_POD_DETAIL_ID, NEW_POD_IDENTIFIER)
                ));

        List<String> errors = new ArrayList<>();
        List<ContractPodRequest> podRequests = service.buildPodRequestsFromIdentifiers(request, existingContract, errors);

        assertThat(errors).isEmpty();
        assertThat(podRequests).hasSize(1);
        assertThat(podRequests.get(0).getBillingGroupId()).isEqualTo(99L);
        assertThat(podRequests.get(0).getProductContractPointOfDeliveries())
                .extracting(pod -> pod.pointOfDeliveryDetailId())
                .containsExactly(OLD_POD_DETAIL_ID, NEW_POD_DETAIL_ID);
    }

    @Test
    void validateEstimatedTotalConsumption_shouldSumRequestedPodsNotOnlyExistingContractPods() {
        SalesPortalContractUpdateRequest request = new SalesPortalContractUpdateRequest();
        request.setPodIdentifiers(List.of(OLD_POD_IDENTIFIER, NEW_POD_IDENTIFIER));
        request.setEstimatedTotalConsumption(BigDecimal.valueOf(3.6));

        when(pointOfDeliveryDetailsRepository.findByPodIdentifiers(List.of(OLD_POD_IDENTIFIER, NEW_POD_IDENTIFIER)))
                .thenReturn(List.of(
                        cacheObject(OLD_POD_DETAIL_ID, OLD_POD_IDENTIFIER),
                        cacheObject(NEW_POD_DETAIL_ID, NEW_POD_IDENTIFIER)
                ));
        when(pointOfDeliveryDetailsRepository.findEstimatedMonthlyAvgConsumptionByIdIn(
                List.of(OLD_POD_DETAIL_ID, NEW_POD_DETAIL_ID)))
                .thenReturn(List.of(100, 200));

        List<String> errors = new ArrayList<>();
        service.validateEstimatedTotalConsumption(request, errors);

        assertThat(errors).isEmpty();
    }

    private static ContractPodsResponseImpl contractPodResponse(String identifier, Long podDetailId, Long billingGroupId) {
        return new ContractPodsResponseImpl(
                identifier,
                "pod-detail",
                podDetailId,
                1,
                1000L,
                PODType.CONSUMER,
                "grid",
                null,
                null,
                "billing-group",
                null,
                null,
                null,
                null,
                1L,
                billingGroupId,
                100,
                null,
                null,
                false
        );
    }

    private static CacheObjectForPod cacheObject(Long id, String identifier) {
        return new CacheObjectForPod(id, identifier, 100, PODType.CONSUMER);
    }
}
