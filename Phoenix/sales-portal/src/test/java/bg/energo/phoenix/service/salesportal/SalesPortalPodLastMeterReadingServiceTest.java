package bg.energo.phoenix.service.salesportal;

import bg.energo.phoenix.exception.DomainEntityNotFoundException;
import bg.energo.phoenix.model.entity.nomenclature.product.priceComponent.Scales;
import bg.energo.phoenix.model.entity.pod.billingByScale.BillingByScale;
import bg.energo.phoenix.model.entity.pod.billingByScale.BillingByScaleStatus;
import bg.energo.phoenix.model.entity.pod.billingByScale.BillingDataByScale;
import bg.energo.phoenix.model.entity.pod.pod.PointOfDelivery;
import bg.energo.phoenix.model.enums.nomenclature.NomenclatureItemStatus;
import bg.energo.phoenix.model.enums.pod.pod.PodStatus;
import bg.energo.phoenix.repository.nomenclature.product.priceComponent.ScalesRepository;
import bg.energo.phoenix.repository.pod.billingByScales.BillingByScaleRepository;
import bg.energo.phoenix.repository.pod.billingByScales.BillingDataByScaleRepository;
import bg.energo.phoenix.repository.pod.meter.MeterRepository;
import bg.energo.phoenix.repository.pod.pod.PointOfDeliveryRepository;
import bg.energo.phoenix.service.salesportal.model.SalesPortalPodLastMeterReadingResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SalesPortalPodLastMeterReadingServiceTest {

    @Mock
    private PointOfDeliveryRepository pointOfDeliveryRepository;
    @Mock
    private BillingByScaleRepository billingByScaleRepository;
    @Mock
    private BillingDataByScaleRepository billingDataByScaleRepository;
    @Mock
    private ScalesRepository scalesRepository;
    @Mock
    private MeterRepository meterRepository;

    @InjectMocks
    private SalesPortalPodLastMeterReadingService service;

    @Test
    void getLastMeterReading_shouldThrow_whenPodNotFound() {
        String identifier = "32XGYKZDRAZPN1058550541";
        when(pointOfDeliveryRepository.findByIdentifierAndStatus(identifier, PodStatus.ACTIVE))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getLastMeterReading(identifier))
                .isInstanceOf(DomainEntityNotFoundException.class)
                .hasMessageContaining("podIdentifier-POD not found with given identifier");
    }

    @Test
    void getLastMeterReading_shouldReturnEmpty_whenNoBillingByScale() {
        String identifier = "32XGYKZDRAZPN1058";
        PointOfDelivery pod = new PointOfDelivery();
        pod.setId(10L);
        pod.setIdentifier(identifier);

        when(pointOfDeliveryRepository.findByIdentifierAndStatus(identifier, PodStatus.ACTIVE))
                .thenReturn(Optional.of(pod));
        when(billingByScaleRepository.findByPodIdAndStatus(10L, BillingByScaleStatus.ACTIVE))
                .thenReturn(List.of());

        assertThat(service.getLastMeterReading(identifier)).isEmpty();
    }

    @Test
    void getLastMeterReading_shouldReturnEmpty_whenScaleRowsEmpty() {
        String identifier = "32XGYKZDRAZPN1058";
        PointOfDelivery pod = new PointOfDelivery();
        pod.setId(10L);
        pod.setIdentifier(identifier);

        BillingByScale billingByScale = billingByScale(3L, LocalDate.of(2026, 5, 1), LocalDate.of(2026, 5, 13),
                false, false, LocalDateTime.of(2026, 5, 13, 12, 0));

        when(pointOfDeliveryRepository.findByIdentifierAndStatus(identifier, PodStatus.ACTIVE))
                .thenReturn(Optional.of(pod));
        when(billingByScaleRepository.findByPodIdAndStatus(10L, BillingByScaleStatus.ACTIVE))
                .thenReturn(List.of(billingByScale));
        when(billingDataByScaleRepository.findByBillingByScaleIdWithoutActiveElectricity(3L))
                .thenReturn(List.of());

        assertThat(service.getLastMeterReading(identifier)).isEmpty();
    }

    @Test
    void getLastMeterReading_shouldSelectLatestPeriodToAndLatestCreateDate() {
        String identifier = "32XGYKZDRAZPN1058";
        PointOfDelivery pod = new PointOfDelivery();
        pod.setId(10L);
        pod.setIdentifier(identifier);

        BillingByScale olderPeriod = billingByScale(1L, LocalDate.of(2026, 4, 1), LocalDate.of(2026, 4, 30),
                false, false, LocalDateTime.of(2026, 5, 1, 10, 0));
        BillingByScale latestPeriodOlderCreate = billingByScale(2L, LocalDate.of(2026, 5, 1), LocalDate.of(2026, 5, 13),
                false, false, LocalDateTime.of(2026, 5, 13, 9, 0));
        BillingByScale latestPeriodLatestCreate = billingByScale(3L, LocalDate.of(2026, 5, 1), LocalDate.of(2026, 5, 13),
                true, true, LocalDateTime.of(2026, 5, 13, 12, 0));
        BillingByScale excludedMixedFlags = billingByScale(4L, LocalDate.of(2026, 5, 1), LocalDate.of(2026, 5, 20),
                true, false, LocalDateTime.of(2026, 5, 14, 12, 0));

        BillingDataByScale tableRow = BillingDataByScale.builder()
                .id(100L)
                .billingByScaleId(3L)
                .scaleId(7L)
                .scaleNumber("251991099")
                .periodFrom(LocalDate.of(2026, 5, 1))
                .periodTo(LocalDate.of(2026, 5, 13))
                .newMeterReading(BigDecimal.valueOf(1500))
                .oldMeterReading(BigDecimal.valueOf(1200))
                .differenceKwh(BigDecimal.valueOf(300))
                .index(0)
                .build();

        Scales scales = Scales.builder()
                .id(7L)
                .scaleCode("SC-01")
                .scaleType("DAY")
                .scaleForActiveElectricity(true)
                .build();

        when(pointOfDeliveryRepository.findByIdentifierAndStatus(identifier, PodStatus.ACTIVE))
                .thenReturn(Optional.of(pod));
        when(billingByScaleRepository.findByPodIdAndStatus(10L, BillingByScaleStatus.ACTIVE))
                .thenReturn(List.of(olderPeriod, latestPeriodOlderCreate, latestPeriodLatestCreate, excludedMixedFlags));
        when(billingDataByScaleRepository.findByBillingByScaleIdWithoutActiveElectricity(3L))
                .thenReturn(List.of(tableRow));
        when(scalesRepository.findByIdAndStatuses(eq(7L), eq(List.of(NomenclatureItemStatus.ACTIVE))))
                .thenReturn(Optional.of(scales));

        SalesPortalPodLastMeterReadingResponse response = service.getLastMeterReading(identifier).orElseThrow();

        assertThat(response.getPodId()).isEqualTo(10L);
        assertThat(response.getDataByScaleId()).isEqualTo(3L);
        assertThat(response.getDateFrom()).isEqualTo("01-05-2026");
        assertThat(response.getDateTo()).isEqualTo("13-05-2026");
        assertThat(response.getCorrection()).isTrue();
        assertThat(response.getOverride()).isTrue();
        assertThat(response.getStatus()).isEqualTo("ACTIVE");
        assertThat(response.getBillingByScalesTableCreateRequests()).hasSize(1);
        assertThat(response.getBillingByScalesTableCreateRequests().get(0).getNewMeterReading())
                .isEqualByComparingTo(BigDecimal.valueOf(1500));
        assertThat(response.getBillingByScalesTableCreateRequests().get(0).getScaleCode()).isEqualTo("SC-01");
        assertThat(response.getBillingByScalesTableCreateRequests().get(0).getPeriodFrom()).isEqualTo("01-05-2026");
    }

    @Test
    void getLastMeterReading_shouldReturnSingleTableRow_withLatestPeriodToAndLatestCreateDate() {
        String identifier = "32XGYKZDRAZPN1058";
        PointOfDelivery pod = new PointOfDelivery();
        pod.setId(10L);
        pod.setIdentifier(identifier);

        BillingByScale billingByScale = billingByScale(3L, LocalDate.of(2026, 5, 1), LocalDate.of(2026, 5, 13),
                false, false, LocalDateTime.of(2026, 5, 13, 12, 0));

        BillingDataByScale olderPeriodRow = BillingDataByScale.builder()
                .id(100L)
                .billingByScaleId(3L)
                .scaleId(7L)
                .scaleNumber("251991099")
                .periodFrom(LocalDate.of(2026, 4, 1))
                .periodTo(LocalDate.of(2026, 4, 30))
                .newMeterReading(BigDecimal.valueOf(1000))
                .index(0)
                .build();
        olderPeriodRow.setCreateDate(LocalDateTime.of(2026, 5, 10, 10, 0));

        BillingDataByScale latestPeriodOlderCreate = BillingDataByScale.builder()
                .id(101L)
                .billingByScaleId(3L)
                .scaleId(7L)
                .scaleNumber("251991100")
                .periodFrom(LocalDate.of(2026, 5, 1))
                .periodTo(LocalDate.of(2026, 5, 13))
                .newMeterReading(BigDecimal.valueOf(1400))
                .index(1)
                .build();
        latestPeriodOlderCreate.setCreateDate(LocalDateTime.of(2026, 5, 13, 9, 0));

        BillingDataByScale latestPeriodLatestCreate = BillingDataByScale.builder()
                .id(102L)
                .billingByScaleId(3L)
                .scaleId(7L)
                .scaleNumber("251991101")
                .periodFrom(LocalDate.of(2026, 5, 1))
                .periodTo(LocalDate.of(2026, 5, 13))
                .newMeterReading(BigDecimal.valueOf(1500))
                .index(2)
                .build();
        latestPeriodLatestCreate.setCreateDate(LocalDateTime.of(2026, 5, 13, 12, 0));

        Scales scales = Scales.builder()
                .id(7L)
                .scaleCode("SC-01")
                .scaleType("DAY")
                .scaleForActiveElectricity(true)
                .build();

        when(pointOfDeliveryRepository.findByIdentifierAndStatus(identifier, PodStatus.ACTIVE))
                .thenReturn(Optional.of(pod));
        when(billingByScaleRepository.findByPodIdAndStatus(10L, BillingByScaleStatus.ACTIVE))
                .thenReturn(List.of(billingByScale));
        when(billingDataByScaleRepository.findByBillingByScaleIdWithoutActiveElectricity(3L))
                .thenReturn(List.of(olderPeriodRow, latestPeriodOlderCreate, latestPeriodLatestCreate));
        when(scalesRepository.findByIdAndStatuses(eq(7L), eq(List.of(NomenclatureItemStatus.ACTIVE))))
                .thenReturn(Optional.of(scales));

        SalesPortalPodLastMeterReadingResponse response = service.getLastMeterReading(identifier).orElseThrow();

        assertThat(response.getBillingByScalesTableCreateRequests()).hasSize(1);
        assertThat(response.getBillingByScalesTableCreateRequests().get(0).getNewMeterReading())
                .isEqualByComparingTo(BigDecimal.valueOf(1500));
        assertThat(response.getBillingByScalesTableCreateRequests().get(0).getScaleNumber()).isEqualTo("251991101");
    }

    private static BillingByScale billingByScale(
            Long id,
            LocalDate dateFrom,
            LocalDate dateTo,
            boolean correction,
            boolean override,
            LocalDateTime createDate
    ) {
        BillingByScale billingByScale = new BillingByScale();
        billingByScale.setId(id);
        billingByScale.setPodId(10L);
        billingByScale.setDateFrom(dateFrom);
        billingByScale.setDateTo(dateTo);
        billingByScale.setCorrection(correction);
        billingByScale.setOverride(override);
        billingByScale.setStatus(BillingByScaleStatus.ACTIVE);
        billingByScale.setCreateDate(createDate);
        return billingByScale;
    }
}
