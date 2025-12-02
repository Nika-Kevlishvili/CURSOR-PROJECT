package bg.energo.phoenix.service.pod.billingByScales;

import bg.energo.phoenix.exception.ClientException;
import bg.energo.phoenix.exception.DomainEntityNotFoundException;
import bg.energo.phoenix.exception.ErrorCode;
import bg.energo.phoenix.model.entity.nomenclature.product.priceComponent.Scales;
import bg.energo.phoenix.model.entity.pod.billingByScale.BillingByScale;
import bg.energo.phoenix.model.entity.pod.billingByScale.BillingByScaleStatus;
import bg.energo.phoenix.model.entity.pod.billingByScale.BillingDataByScale;
import bg.energo.phoenix.model.entity.pod.meter.Meter;
import bg.energo.phoenix.model.entity.pod.pod.PointOfDelivery;
import bg.energo.phoenix.model.entity.pod.pod.PointOfDeliveryDetails;
import bg.energo.phoenix.model.enums.nomenclature.NomenclatureItemStatus;
import bg.energo.phoenix.model.enums.pod.PodSubObjectStatus;
import bg.energo.phoenix.model.enums.pod.meter.MeterStatus;
import bg.energo.phoenix.model.enums.pod.pod.PodStatus;
import bg.energo.phoenix.model.request.pod.billingByScales.*;
import bg.energo.phoenix.model.response.pod.billingByScales.BillingByScaleListResponse;
import bg.energo.phoenix.model.response.pod.billingByScales.BillingByScalesResponse;
import bg.energo.phoenix.model.response.pod.billingByScales.BillingByScalesTableResponse;
import bg.energo.phoenix.permissions.PermissionContextEnum;
import bg.energo.phoenix.permissions.PermissionEnum;
import bg.energo.phoenix.repository.nomenclature.product.priceComponent.ScalesRepository;
import bg.energo.phoenix.repository.pod.billingByScales.BillingByScaleRepository;
import bg.energo.phoenix.repository.pod.billingByScales.BillingDataByScaleRepository;
import bg.energo.phoenix.repository.pod.meter.MeterRepository;
import bg.energo.phoenix.repository.pod.meter.MeterScaleRepository;
import bg.energo.phoenix.repository.pod.pod.PointOfDeliveryDetailsRepository;
import bg.energo.phoenix.repository.pod.pod.PointOfDeliveryRepository;
import bg.energo.phoenix.security.PermissionService;
import bg.energo.phoenix.util.epb.EPBChainedExceptionTriggerUtil;
import bg.energo.phoenix.util.epb.EPBFinalFields;
import bg.energo.phoenix.util.epb.EPBStringUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static bg.energo.phoenix.permissions.PermissionEnum.BILLING_BY_SCALES_VIEW_BASIC;
import static bg.energo.phoenix.permissions.PermissionEnum.BILLING_BY_SCALES_VIEW_DELETED;

@Service
@Slf4j
@RequiredArgsConstructor
public class BillingByScalesService {
    private final BillingByScaleRepository billingByScaleRepository;
    private final BillingDataByScaleRepository billingDataByScaleRepository;
    private final PointOfDeliveryRepository pointOfDeliveryRepository;
    private final PointOfDeliveryDetailsRepository pointOfDeliveryDetailsRepository;
    private final ScalesRepository scalesRepository;
    private final MeterRepository meterRepository;
    private final MeterScaleRepository meterScaleRepository;
    private final PermissionService permissionService;

    @Transactional
    public Long create(BillingByScalesCreateRequest request) {
        List<String> errorMessages = new ArrayList<>();
        BillingByScale dbBillingByScale = null;
        PointOfDelivery pointOfDelivery =
                pointOfDeliveryRepository.findByIdentifierAndStatus(request.getIdentifier(), PodStatus.ACTIVE)
                        .orElseThrow(() -> new DomainEntityNotFoundException("identifier -[identifier] Can't find active pod with identifier %s id;".formatted(request.getIdentifier())));
        PointOfDeliveryDetails pointOfDeliveryDetails =
                pointOfDeliveryDetailsRepository.findByPodId(pointOfDelivery.getId())
                        .orElseThrow(() -> new DomainEntityNotFoundException("identifier -[identifier] Can't find pod Details with pod id %s;".formatted(pointOfDelivery.getId())));


        LocalDate dateFrom = request.getDateFrom();
        LocalDate dateTo = request.getDateTo();
        if (!(request.getCorrection() || request.getOverride())) {
            checkBillingByScalesWithPodPeriodFromAndPeriodTo(pointOfDelivery, dateFrom, dateTo, errorMessages);
        }
        request.setBillingByScalesTableCreateRequests(sortBillingByScalesByIndex(request.getBillingByScalesTableCreateRequests()));
        for (int i = 0; i < request.getBillingByScalesTableCreateRequests().size(); i++) {
            BillingByScalesTableCreateRequest item = request.getBillingByScalesTableCreateRequests().get(i);
            if (!(item.getPeriodFrom().isAfter(dateFrom) || item.getPeriodFrom().isEqual(dateFrom)) ||
                    !(item.getPeriodFrom().isBefore(dateTo) || item.getPeriodFrom().isEqual(dateTo))) {
                errorMessages.add("dateFrom -[DateFrom] is not in range: %s;"
                        .formatted(dateFrom));
                /*throw new ClientException("dateFrom -[DateFrom] is not in range: %s;"
                        .formatted(dateFrom), ErrorCode.CONFLICT);*/
            }
            if (!(item.getPeriodTo().isAfter(dateFrom) || item.getPeriodTo().isEqual(dateFrom)) ||
                    !(item.getPeriodTo().isBefore(dateTo) || item.getPeriodTo().isEqual(dateTo))) {
                errorMessages.add("dateTo -[DateTo] is not in range: %s;"
                        .formatted(dateTo));
                /*throw new ClientException("dateTo -[DateTo] is not in range: %s;"
                        .formatted(dateTo), ErrorCode.CONFLICT);*/
            }

        }

        if (!request.getBillingByScalesTableCreateRequests().isEmpty()) {
            List<BillingByScalesTableCreateRequest> billingByScalesTableCreateRequestsList =
                    sortBillingByScalesByIndex(request.getBillingByScalesTableCreateRequests());

            for (int i = 0; i < billingByScalesTableCreateRequestsList.size(); i++) {
                BillingByScalesTableCreateRequest billingByScalesTableCreateRequests =
                        request.getBillingByScalesTableCreateRequests().get(i);
                if (!request.getSaveRecordForIntermediatePeriod()) {
                    if (!checkForIntermediatePeriod(dateFrom, dateTo, billingByScalesTableCreateRequestsList)) {
                        StringBuilder warningMassage = new StringBuilder();
                        warningMassage.append("dateFrom -[DateFrom] there is intermediate period dateFrom: %s;dateTo -[dateTo] there is intermediate period dateTo: %s;"
                                .formatted(dateFrom, dateTo));
                        warningMassage.insert(0, "%s-".formatted(EPBFinalFields.WARNING_MESSAGE_INDICATOR));
                        throw new ClientException(warningMassage.toString(), ErrorCode.CONFLICT);
                    }
                }
                String scaleCode = billingByScalesTableCreateRequests.getScaleCode();
                String scaleType = billingByScalesTableCreateRequests.getScaleType();
                String scaleTariffScale = billingByScalesTableCreateRequests.getTariffScale();
                String meterNumber = billingByScalesTableCreateRequests.getMeterNumber();
                LocalDate periodFrom = billingByScalesTableCreateRequests.getPeriodFrom();
                LocalDate periodTo = billingByScalesTableCreateRequests.getPeriodTo();
                Scales scales = null;
                int finalI = i;
                if (meterNumber != null && !meterNumber.isEmpty()) {
                    checkIfMeterExists(finalI, meterNumber);
                    Meter innerListMeter = meterRepository.findByNumberAndGridOperatorIdAndStatus(periodFrom, periodTo, meterNumber, pointOfDelivery.getGridOperatorId(), MeterStatus.ACTIVE)
                            .orElseThrow(() -> new DomainEntityNotFoundException("billingByScalesTableCreateRequests[%s].meterNumber -[MeterNumber] Meter isn’t installed in this POD in the mentioned period;".formatted(finalI)));
                    if (periodFrom != null && periodTo != null) {

                        if (!(periodFrom.equals(innerListMeter.getInstallmentDate()))) {
                            if (!periodFrom.isAfter(innerListMeter.getInstallmentDate())) {
                                errorMessages.add("billingByScalesTableCreateRequests[%s].periodFrom -[PeriodFrom] should be after meterNumber meter installment date;"
                                        .formatted(billingByScalesTableCreateRequests.getIndex()));
                               /* throw new ClientException("billingByScalesTableCreateRequests[%s].periodFrom -[PeriodFrom] should be after meterNumber meter installment date;"
                                        .formatted(billingByScalesTableCreateRequests.getIndex()), ErrorCode.CONFLICT);*/
                            }
                        }
                        if (innerListMeter.getRemoveDate() != null) {
                            if (!periodFrom.isBefore(innerListMeter.getRemoveDate())) {
                                errorMessages.add("billingByScalesTableCreateRequests[%s].periodFrom -[PeriodFrom] should be before meterNumber meter removal date;"
                                        .formatted(billingByScalesTableCreateRequests.getIndex()));
                                /*throw new ClientException("billingByScalesTableCreateRequests[%s].periodFrom -[PeriodFrom] should be before meterNumber meter removal date;"
                                        .formatted(billingByScalesTableCreateRequests.getIndex()), ErrorCode.CONFLICT);*/
                            }
                        }
                        if (!periodTo.isAfter(innerListMeter.getInstallmentDate())) {
                            errorMessages.add("billingByScalesTableCreateRequests[%s].periodTo -[PeriodTo] should be after meterNumber meter installment date;"
                                    .formatted(billingByScalesTableCreateRequests.getIndex()));
                           /* throw new ClientException("billingByScalesTableCreateRequests[%s].periodTo -[PeriodTo] should be after meterNumber meter installment date;"
                                    .formatted(billingByScalesTableCreateRequests.getIndex()), ErrorCode.CONFLICT);*/
                        }
                        if (innerListMeter.getRemoveDate() != null) {
                            if (!(periodTo.equals(innerListMeter.getRemoveDate()))) {
                                if (!periodTo.isBefore(innerListMeter.getRemoveDate())) {
                                    errorMessages.add("billingByScalesTableCreateRequests[%s].periodTo -[PeriodTo] should be before meterNumber meter removal date;"
                                            .formatted(billingByScalesTableCreateRequests.getIndex()));
                                    /*throw new ClientException("billingByScalesTableCreateRequests[%s].periodTo -[PeriodTo] should be before meterNumber meter removal date;"
                                            .formatted(billingByScalesTableCreateRequests.getIndex()), ErrorCode.CONFLICT);*/
                                }
                            }
                        }
                    }
                    if (scaleCode != null && !scaleCode.isEmpty() &&
                            scaleType != null && !scaleType.isEmpty()) {
                        scales = scalesRepository.findByScaleCodeAndScaleTypeAndStatusAndGridOperatorId(
                                        scaleCode,
                                        scaleType,
                                        NomenclatureItemStatus.ACTIVE,
                                        innerListMeter.getGridOperatorId())
                                .orElseThrow(() -> new DomainEntityNotFoundException(("billingByScalesTableCreateRequests[%s].scaleCode -[scaleCode] Can't find active scales nomenclature with the scaleCode: %s;billingByScalesTableCreateRequests[%s].scaleType -[scaleType] Can't find active scales nomenclature with the scaleType: %s;")
                                        .formatted(billingByScalesTableCreateRequests.getIndex(), billingByScalesTableCreateRequests.getScaleCode(), billingByScalesTableCreateRequests.getIndex(), billingByScalesTableCreateRequests.getScaleCode())));
                    }
                    if (scaleTariffScale != null && !scaleTariffScale.isEmpty() &&
                            scaleType != null && !scaleType.isEmpty()) {
                        scales = scalesRepository.findByTariffScaleAndScaleTypeAndGridOperatorIdAndStatus(
                                scaleTariffScale,
                                scaleType,
                                innerListMeter.getGridOperatorId(),
                                NomenclatureItemStatus.ACTIVE).orElseThrow(()->new DomainEntityNotFoundException("billingByScalesTableCreateRequests[%s].tariffScale -[tariffScale] Can't find active scales nomenclature with the tariffScale: %s;billingByScalesTableCreateRequests[%s].scaleType -[scaleType] Can't find active scales nomenclature with the scaleType: %s;"
                                .formatted(billingByScalesTableCreateRequests.getIndex(), billingByScalesTableCreateRequests.getTariffScale(), billingByScalesTableCreateRequests.getIndex(), billingByScalesTableCreateRequests.getScaleType())));

                    }
                    if (scales == null) {
                        throw new DomainEntityNotFoundException("billingByScalesTableCreateRequests[%s].scaleCode -[scaleCode] Can't find Active Scales nomenclature with scalesCode/ScalesTariff;"
                                .formatted(billingByScalesTableCreateRequests.getIndex()));
                    }
                    Long innerMeterId = innerListMeter.getId();
                    Long scalesId;

                    if (scales != null) {
                        scalesId = scales.getId();
                    } else {
                        scalesId = null;
                    }
                    if (innerMeterId != null && scalesId != null) {
                        meterScaleRepository.findByMeterIdAndScaleIdAndStatus(innerMeterId, scalesId, PodSubObjectStatus.ACTIVE)
                                .orElseThrow(() -> new DomainEntityNotFoundException("billingByScalesTableCreateRequests[%s].periodTo -[periodTo] Can't find meterScales with meterId: %s and ScalesId: %s;"
                                        .formatted(billingByScalesTableCreateRequests.getIndex(), innerMeterId, scalesId)));
                    }
//                    if (!request.getSaveRecordForMeterReadings()) {
//                      /  if (!checkForMeterReadings(billingByScalesTableCreateRequests, scalesId, pointOfDelivery.getId(), innerMeterId)) {
//                            StringBuilder warningMassage = new StringBuilder();
//                            warningMassage.append("newMeterReading -[NewMeterReading] mismatch ;oldMeterReading -[OldMeterReading]  mismatch;");
//                            warningMassage.insert(0, "%s-".formatted(EPBFinalFields.WARNING_MESSAGE_INDICATOR));
//                            throw new ClientException(warningMassage.toString(), ErrorCode.CONFLICT);
//                        }
//                    }
                    if (scalesId != null && innerMeterId != null) {
                        checkBillingByScaleOldAndNewMeterReadings(scalesId, innerMeterId, pointOfDelivery);
                    }
                }


            }
            checkScaleCodeMeterDatesAndMeterReading(billingByScalesTableCreateRequestsList, errorMessages, request.getSaveRecordForMeterReadings());
            dbBillingByScale = createBillingByScales(request, pointOfDelivery);
            List<BillingDataByScale> billingDataByScales = new ArrayList<>();
            for (int i = 0; i < request.getBillingByScalesTableCreateRequests().size(); i++) {
                BillingByScalesTableCreateRequest item = request.getBillingByScalesTableCreateRequests().get(i);
                int finalI = i;
                checkIfMeterExists(finalI, item.getMeterNumber());
                Meter meter = null;
                if (StringUtils.isNotEmpty(item.getMeterNumber())) {
                    meter = meterRepository.findByNumberAndGridOperatorIdAndStatus(item.getPeriodFrom(), item.getPeriodTo(), item.getMeterNumber(), pointOfDelivery.getGridOperatorId(), MeterStatus.ACTIVE)
                            .orElseThrow(() -> new DomainEntityNotFoundException("billingByScalesTableCreateRequests[%s].meterNumber -[MeterNumber] Meter isn’t installed in this POD in the mentioned period;".formatted(item.getIndex())));
                }
                Long scalesId = null;
                if (item.getScaleCode() != null && !item.getScaleCode().isEmpty() &&
                        item.getScaleType() != null && !item.getScaleType().isEmpty()) {
                    Scales scales = scalesRepository.findByScaleCodeAndScaleTypeAndStatusAndGridOperatorId(
                                    item.getScaleCode(),
                                    item.getScaleType(),
                                    NomenclatureItemStatus.ACTIVE,
                                    meter.getGridOperatorId())
                            .orElseThrow(() -> new DomainEntityNotFoundException("billingByScalesTableCreateRequests[%s].scaleCode -[scaleCode] Can't find active scales nomenclature with the scaleCode: %s;"
                                    .formatted(item.getIndex(), item.getScaleCode())));
                    scalesId = scales.getId();
                }
                if (item.getTariffScale() != null && !item.getTariffScale().isEmpty() &&
                        item.getScaleType() != null && !item.getScaleType().isEmpty()) {
                    Scales scales = scalesRepository.findByTariffScaleAndScaleTypeAndGridOperatorIdAndStatus(
                            item.getTariffScale(),
                            item.getScaleType(),
                            pointOfDelivery.getGridOperatorId(),
                            NomenclatureItemStatus.ACTIVE).orElseThrow(() -> new DomainEntityNotFoundException("billingByScalesTableCreateRequests[%s].tariffScale -[tariffScale] Can't find active scales nomenclature with the tariffScale: %s;"
                            .formatted(item.getIndex(), item.getScaleCode())));

                    scalesId = scales.getId();


                }
                if (scalesId == null) {
                    throw new DomainEntityNotFoundException("billingByScalesTableCreateRequests[%s].scaleNumber -[ScaleNumber] Can't find active scales nomenclature with the tariffScale: %s;"
                            .formatted(item.getIndex(), item.getScaleCode()));
                }
                BillingDataByScale billingDataByScale = BillingDataByScale.builder()
                        .periodFrom(item.getPeriodFrom())
                        .periodTo(item.getPeriodTo())
                        .meterId(meter != null ? meter.getId() : null)
                        .scaleId(scalesId)
                        //.timeZone() has been deleted
                        .newMeterReading(item.getNewMeterReading())
                        .oldMeterReading(item.getOldMeterReading())
                        .differenceKwh(item.getDifference())
                        .multiplier(item.getMultiplier())
                        .correctionKwh(item.getCorrection())
                        .deductedKwh(item.getDeducted())
                        .totalVolumesKwh(item.getTotalVolumes())
                        .volumes(item.getVolumes())
                        .unitPrice(item.getUnitPrice())
                        .totalValue(item.getTotalValue())
                        .billingByScaleId(dbBillingByScale.getId())
                        .scaleNumber(item.getScaleNumber())
                        .index(item.getIndex())
                        .build();
                billingDataByScales.add(billingDataByScale);
            }
            billingDataByScaleRepository.saveAll(billingDataByScales);
        } else {
            dbBillingByScale = createBillingByScales(request, pointOfDelivery);
        }
        EPBChainedExceptionTriggerUtil.throwExceptionIfRequired(errorMessages, log);
        return dbBillingByScale.getId();
    }

    private void checkBillingByScalesWithPodPeriodFromAndPeriodTo(PointOfDelivery pointOfDelivery, LocalDate dateFrom, LocalDate dateTo, List<String> errorMessages) {
        List<Long> billingDataByScaleList = billingByScaleRepository.findByPodIdAndDateFromAndDateTo(pointOfDelivery.getId(), dateFrom, dateTo);
        if (!CollectionUtils.isEmpty(billingDataByScaleList)) {
            throw new ClientException("Can't create billing data by scale, because  in system already exist billing data by scale which include same POD and period from and period to has overlap;"
                    , ErrorCode.CONFLICT);
        }
    }

    public List<BillingByScalesTableCreateRequest> sortBillingByScalesByIndex(List<BillingByScalesTableCreateRequest> list) {
        if (CollectionUtils.isNotEmpty(list)) {
            list.sort(Comparator.comparingInt(BillingByScalesTableCreateRequest::getIndex));
            return list;
        }
        return list;
    }

    public List<BillingDataByScale> combinedBillingByScalesSort(List<BillingDataByScale> combinedBillingByScales) {
        if (CollectionUtils.isNotEmpty(combinedBillingByScales)) {
            combinedBillingByScales.sort(Comparator.comparingInt(BillingDataByScale::getIndex));
            return combinedBillingByScales;
        }
        return combinedBillingByScales;
    }

    private Integer calculateIndex(List<BillingDataByScale> combinedBillingByScales) {
        int maxIndex = 0;
        for (int i = 1; i < combinedBillingByScales.size(); i++) {
            // Replace getSomeProperty() with the actual getter for the property you want to use for comparison
            if (combinedBillingByScales.get(i).getIndex() > combinedBillingByScales.get(maxIndex).getIndex()) {
                maxIndex = i;
            }
        }
        return maxIndex + 1;
    }

    private BillingByScale createBillingByScales(BillingByScalesCreateRequest request, PointOfDelivery pointOfDelivery) {
        BillingByScale billingByScale = new BillingByScale();
        billingByScale.setPodId(pointOfDelivery.getId());
        billingByScale.setDateFrom(request.getDateFrom());
        billingByScale.setDateTo(request.getDateTo());
        billingByScale.setBillingPowerInKw(request.getBillingPowerInKw());
        billingByScale.setInvoiceNumber(request.getInvoiceNumber());
        billingByScale.setInvoiceDate(request.getInvoiceDate());
        billingByScale.setInvoiceCorrection(request.getInvoiceCorrection());
        billingByScale.setCorrection(request.getCorrection());
        billingByScale.setOverride(request.getOverride());
        billingByScale.setBasisForIssuingInvoice(request.getBasisForIssuingTheInvoice());
        billingByScale.setStatus(BillingByScaleStatus.ACTIVE);
        return billingByScaleRepository.save(billingByScale);
    }

//    private boolean checkForMeterReadings(BillingByScalesTableCreateRequest item, Long scalesId, Long id, Long innerMeterId) {
//        Boolean status = true;
//        BillingByScalesMaxMinReading minMaxReadings = billingByScaleRepository.findByPodIdAndScaleCodaAndMeter(scalesId, id, innerMeterId);
//        if (item.getOldMeterReading() != null) {
//            if (!item.getOldMeterReading().equals(minMaxReadings.getMaxReading())) {
//                status = false;
//            }
//        } else if (minMaxReadings.getMaxReading() != null) {
//            status = false;
//        }
//        if (item.getNewMeterReading() != null) {
//            if (!item.getNewMeterReading().equals(minMaxReadings.getMinReading())) {
//                status = false;
//            }
//        } else if (minMaxReadings.getMinReading() != null) {
//            status = false;
//        }
//
//        return status;
//    }

    private void checkBillingByScaleOldAndNewMeterReadings(Long scaleId, Long meterId, PointOfDelivery pointOfDelivery) {
        //TODO waiting for the logic to find out

        billingByScaleRepository.findByPodIdAndScaleCodaAndMeter(scaleId, meterId, pointOfDelivery.getId());
    }

    private boolean checkForIntermediatePeriod(LocalDate dateFrom, LocalDate dateTo, List<BillingByScalesTableCreateRequest> billingByScalesTableCreateRequestsList) {
        Set<LocalDate> initialDays = getDaysSet(dateFrom, dateTo);
        Set<LocalDate> listDays = new HashSet<>();
        for (BillingByScalesTableCreateRequest item : billingByScalesTableCreateRequestsList) {
            Set<LocalDate> itemDays = getDaysSet(item.getPeriodFrom(), item.getPeriodTo());
            listDays.addAll(itemDays);
        }
        return initialDays.size() == listDays.size();
    }

    private Set<LocalDate> getDaysSet(LocalDate dateFrom, LocalDate dateTo) {
        Set<LocalDate> daysSet = new HashSet<>();
        LocalDate current = dateFrom;

        while (!current.isAfter(dateTo)) {
            daysSet.add(current);
            current = current.plusDays(1);
        }

        return daysSet;
    }

    @Transactional
    public void checkScaleCodeMeterDatesAndMeterReadingForEdit(List<BillingDataByScale> billingByScalesTableEditRequestsList, Boolean saveRecordForMeterReadings) {
        List<List<BillingDataByScale>> findUniqueEntities = findUniqueEntriesForEdit(billingByScalesTableEditRequestsList);
        if (CollectionUtils.isNotEmpty(findUniqueEntities)) {
            int index = -1;
            for (List<BillingDataByScale> list : findUniqueEntities) {
                //if (listHasNewItems(list)) {
                for (BillingDataByScale item : list) {
                    item.setIndex(++index);
                }
                //}
                List<BillingDataByScale> scaleCodeAndMeterNumber = findDuplicatesForScaleCodeAndMeterNumberForEdit(list);
                scaleCodeAndMeterNumber = combinedBillingByScalesSort(scaleCodeAndMeterNumber);
                checkBillingRequestListForDatesForEdit(scaleCodeAndMeterNumber, saveRecordForMeterReadings);
            }
        }
    }

    private boolean listHasNewItems(List<BillingDataByScale> findUniqueEntities) {
        for (BillingDataByScale item : findUniqueEntities) {
            if (item.getId() == null) {
                return true;
            }
        }
        return false;
    }

    public List<List<BillingDataByScale>> findUniqueEntriesForEdit(List<BillingDataByScale> billingDataList) {
        Map<String, List<BillingDataByScale>> uniqueEntriesMap = new HashMap<>();

        for (BillingDataByScale entry : billingDataList) {
            Long scaleNumber = entry.getScaleId();
            Long scaleCode = entry.getMeterId();

            if (scaleNumber != null && scaleCode != null) {
                String key = scaleNumber + "_" + scaleCode;
                if (!uniqueEntriesMap.containsKey(key)) {
                    uniqueEntriesMap.put(key, new ArrayList<>());
                }
                uniqueEntriesMap.get(key).add(entry);
            }
        }

        return new ArrayList<>(uniqueEntriesMap.values());
    }

    private void checkScaleCodeMeterDatesAndMeterReadingInEdit(List<BillingByScalesTableEditRequest> billingByScalesTableEditRequests, List<String> errorMessages, Boolean saveRecordForMeterReadings) {
        List<BillingByScalesTableCreateRequest> billingByScalesTableCreateRequests = getBillingByCreateRequestFromEdit(billingByScalesTableEditRequests, errorMessages);
        checkScaleCodeMeterDatesAndMeterReading(billingByScalesTableCreateRequests, errorMessages, saveRecordForMeterReadings);
    }

    private List<BillingByScalesTableCreateRequest> getBillingByCreateRequestFromEdit(List<BillingByScalesTableEditRequest> billingByScalesTableEditRequests, List<String> errorMessages) {
        List<BillingByScalesTableCreateRequest> list = new ArrayList<>();
        if (!CollectionUtils.isEmpty(billingByScalesTableEditRequests)) {
            for (BillingByScalesTableEditRequest item : billingByScalesTableEditRequests) {
                BillingByScalesTableCreateRequest billingByScalesTableCreateRequest = new BillingByScalesTableCreateRequest();
                billingByScalesTableCreateRequest.setPeriodFrom(item.getPeriodFrom());
                billingByScalesTableCreateRequest.setPeriodTo(item.getPeriodTo());
                billingByScalesTableCreateRequest.setMeterNumber(item.getMeterNumber());
                billingByScalesTableCreateRequest.setScaleCode(item.getScaleCode());
                billingByScalesTableCreateRequest.setScaleType(item.getScaleType());
                billingByScalesTableCreateRequest.setTimeZone(item.getTimeZone());
                billingByScalesTableCreateRequest.setNewMeterReading(item.getNewMeterReading());
                billingByScalesTableCreateRequest.setOldMeterReading(item.getOldMeterReading());
                billingByScalesTableCreateRequest.setDifference(item.getDifference());
                billingByScalesTableCreateRequest.setMultiplier(item.getMultiplier());
                billingByScalesTableCreateRequest.setCorrection(item.getCorrection());
                billingByScalesTableCreateRequest.setDeducted(item.getDeducted());
                billingByScalesTableCreateRequest.setTotalVolumes(item.getTotalVolumes());
                billingByScalesTableCreateRequest.setTariffScale(item.getTariffScale());
                billingByScalesTableCreateRequest.setVolumes(item.getVolumes());
                billingByScalesTableCreateRequest.setUnitPrice(item.getUnitPrice());
                billingByScalesTableCreateRequest.setTotalValue(item.getTotalValue());
                billingByScalesTableCreateRequest.setIndex(item.getIndex());
                list.add(billingByScalesTableCreateRequest);
            }
        }
        return list;
    }

    private void checkScaleCodeMeterDatesAndMeterReading(List<BillingByScalesTableCreateRequest> billingByScalesTableCreateRequestsList, List<String> errorMessages, Boolean saveRecordForMeterReadings) {
        List<List<BillingByScalesTableCreateRequest>> findUniqueEntities = findUniqueEntriesForScaleCode(billingByScalesTableCreateRequestsList);
        if (CollectionUtils.isNotEmpty(findUniqueEntities)) {
            for (List<BillingByScalesTableCreateRequest> list : findUniqueEntities) {
                List<BillingByScalesTableCreateRequest> scaleCodeAndMeterNumber = sortBillingByScalesByIndex(findDuplicatesForScaleCodeAndMeterNumber(list));
                checkBillingRequestListForDates(scaleCodeAndMeterNumber, billingByScalesTableCreateRequestsList, errorMessages, saveRecordForMeterReadings);
            }
        }
        List<List<BillingByScalesTableCreateRequest>> findTariffUniqueEntities = findUniqueEntriesForScaleTariff(billingByScalesTableCreateRequestsList);
        if (CollectionUtils.isNotEmpty(findTariffUniqueEntities)) {
            for (List<BillingByScalesTableCreateRequest> list : findTariffUniqueEntities) {
                List<BillingByScalesTableCreateRequest> tariffScaleAndMeterNumber = sortBillingByScalesByIndex(tariffScaleAndMeterNumber(list));
                checkBillingRequestListForDates(tariffScaleAndMeterNumber, billingByScalesTableCreateRequestsList, errorMessages, saveRecordForMeterReadings);
            }
        }
    }

    public List<List<BillingByScalesTableCreateRequest>> findUniqueEntriesForScaleCode(List<BillingByScalesTableCreateRequest> billingDataList) {
        Map<String, List<BillingByScalesTableCreateRequest>> uniqueEntriesMap = new HashMap<>();

        for (BillingByScalesTableCreateRequest entry : billingDataList) {
            String scaleCode = entry.getScaleCode();
            String meterNumber = entry.getMeterNumber();

            if (scaleCode != null && meterNumber != null) {
                String key = scaleCode + "_" + meterNumber;
                if (!uniqueEntriesMap.containsKey(key)) {
                    uniqueEntriesMap.put(key, new ArrayList<>());
                }
                uniqueEntriesMap.get(key).add(entry);
            }
        }

        return new ArrayList<>(uniqueEntriesMap.values());
    }

    public List<List<BillingByScalesTableEditRequest>> findUniqueEntriesForScaleCodeForEdit(List<BillingByScalesTableEditRequest> billingDataList) {
        Map<String, List<BillingByScalesTableEditRequest>> uniqueEntriesMap = new HashMap<>();

        for (BillingByScalesTableEditRequest entry : billingDataList) {
            String scaleCode = entry.getScaleCode();
            String meterNumber = entry.getMeterNumber();

            if (scaleCode != null && meterNumber != null) {
                String key = scaleCode + "_" + meterNumber;
                if (!uniqueEntriesMap.containsKey(key)) {
                    uniqueEntriesMap.put(key, new ArrayList<>());
                }
                uniqueEntriesMap.get(key).add(entry);
            }
        }

        return new ArrayList<>(uniqueEntriesMap.values());
    }

    public List<List<BillingByScalesTableCreateRequest>> findUniqueEntriesForScaleTariff(List<BillingByScalesTableCreateRequest> billingDataList) {
        Map<String, List<BillingByScalesTableCreateRequest>> uniqueEntriesMap = new HashMap<>();

        for (BillingByScalesTableCreateRequest entry : billingDataList) {
            String scaleCode = entry.getTariffScale();
            String meterNumber = entry.getMeterNumber();

            if (scaleCode != null && meterNumber != null) {
                String key = scaleCode + "_" + meterNumber;
                if (!uniqueEntriesMap.containsKey(key)) {
                    uniqueEntriesMap.put(key, new ArrayList<>());
                }
                uniqueEntriesMap.get(key).add(entry);
            }
        }

        return new ArrayList<>(uniqueEntriesMap.values());
    }

    public List<List<BillingByScalesTableEditRequest>> findUniqueEntriesForScaleTariffForEdit(List<BillingByScalesTableEditRequest> billingDataList) {
        Map<String, List<BillingByScalesTableEditRequest>> uniqueEntriesMap = new HashMap<>();

        for (BillingByScalesTableEditRequest entry : billingDataList) {
            String scaleCode = entry.getTariffScale();
            String meterNumber = entry.getMeterNumber();

            if (scaleCode != null && meterNumber != null) {
                String key = scaleCode + "_" + meterNumber;
                if (!uniqueEntriesMap.containsKey(key)) {
                    uniqueEntriesMap.put(key, new ArrayList<>());
                }
                uniqueEntriesMap.get(key).add(entry);
            }
        }

        return new ArrayList<>(uniqueEntriesMap.values());
    }

    private List<BillingByScalesTableEditRequest> findDuplicatesForScaleCodeAndMeterNumberEdit(List<BillingByScalesTableEditRequest> requests) {
        Set<BillingByScalesTableEditRequest> duplicates = new HashSet<>();

        int size = requests.size();
        for (int i = 0; i < size - 1; i++) {
            BillingByScalesTableEditRequest currentRequest = requests.get(i);
            String currentScaleCode = currentRequest.getScaleCode();
            String currentMeterNumber = currentRequest.getMeterNumber();
            if (currentScaleCode != null && currentMeterNumber != null) {
                for (int j = i + 1; j < size; j++) {
                    BillingByScalesTableEditRequest otherRequest = requests.get(j);
                    String otherScaleCode = otherRequest.getScaleCode();
                    String otherMeterNumber = otherRequest.getMeterNumber();
                    if (otherScaleCode != null && otherMeterNumber != null) {
                        // Check for same scaleCode and meterNumber
                        if (currentScaleCode.equals(otherScaleCode) && currentMeterNumber.equals(otherMeterNumber)) {
                            duplicates.add(currentRequest);
                            duplicates.add(otherRequest);
                        }
                    }
                }
            }
        }
        return new ArrayList<>(duplicates);
    }

    private List<BillingByScalesTableCreateRequest> findDuplicatesForScaleCodeAndMeterNumber(List<BillingByScalesTableCreateRequest> requests) {
        Set<BillingByScalesTableCreateRequest> duplicates = new HashSet<>();

        int size = requests.size();
        for (int i = 0; i < size - 1; i++) {
            BillingByScalesTableCreateRequest currentRequest = requests.get(i);
            String currentScaleCode = currentRequest.getScaleCode();
            String currentMeterNumber = currentRequest.getMeterNumber();
            if (currentScaleCode != null && currentMeterNumber != null) {
                for (int j = i + 1; j < size; j++) {
                    BillingByScalesTableCreateRequest otherRequest = requests.get(j);
                    String otherScaleCode = otherRequest.getScaleCode();
                    String otherMeterNumber = otherRequest.getMeterNumber();
                    if (otherScaleCode != null && otherMeterNumber != null) {
                        // Check for same scaleCode and meterNumber
                        if (currentScaleCode.equals(otherScaleCode) && currentMeterNumber.equals(otherMeterNumber)) {
                            duplicates.add(currentRequest);
                            duplicates.add(otherRequest);
                        }
                    }
                }
            }
        }
        return new ArrayList<>(duplicates);
    }

    private List<BillingDataByScale> findDuplicatesForScaleCodeAndMeterNumberForEdit(List<BillingDataByScale> requests) {
        Set<BillingDataByScale> duplicates = new HashSet<>();

        int size = requests.size();
        for (int i = 0; i < size - 1; i++) {
            BillingDataByScale currentRequest = requests.get(i);
            Long currentScaleCode = currentRequest.getScaleId();
            Long currentMeterNumber = currentRequest.getMeterId();
            if (currentScaleCode != null && currentMeterNumber != null) {
                for (int j = i + 1; j < size; j++) {
                    BillingDataByScale otherRequest = requests.get(j);
                    Long otherScaleCode = otherRequest.getScaleId();
                    Long otherMeterNumber = otherRequest.getMeterId();
                    if (otherScaleCode != null && otherMeterNumber != null) {
                        // Check for same scaleCode and meterNumber
                        if (currentScaleCode.equals(otherScaleCode) && currentMeterNumber.equals(otherMeterNumber)) {
                            duplicates.add(currentRequest);
                            duplicates.add(otherRequest);
                        }
                    }
                }
            }
        }
        return new ArrayList<>(duplicates);
    }

    private List<BillingByScalesTableEditRequest> tariffScaleAndMeterNumberEdit(List<BillingByScalesTableEditRequest> requests) {
        Set<BillingByScalesTableEditRequest> duplicates = new HashSet<>();

        int size = requests.size();
        for (int i = 0; i < size - 1; i++) {
            BillingByScalesTableEditRequest currentRequest = requests.get(i);
            String currentTariffScale = currentRequest.getTariffScale();
            String currentMeterNumber = currentRequest.getMeterNumber();
            if (currentTariffScale != null && currentMeterNumber != null) {
                for (int j = i + 1; j < size; j++) {
                    BillingByScalesTableEditRequest otherRequest = requests.get(j);
                    String otherTariffScale = otherRequest.getTariffScale();
                    String otherMeterNumber = otherRequest.getMeterNumber();
                    if (otherTariffScale != null && otherMeterNumber != null) {
                        // Check for same tariffScale and meterNumber
                        if (currentTariffScale.equals(otherTariffScale) && currentMeterNumber.equals(otherMeterNumber)) {
                            duplicates.add(currentRequest);
                            duplicates.add(otherRequest);
                        }
                    }

                }
            }

        }
        return new ArrayList<>(duplicates);
    }

    private List<BillingByScalesTableCreateRequest> tariffScaleAndMeterNumber(List<BillingByScalesTableCreateRequest> requests) {
        Set<BillingByScalesTableCreateRequest> duplicates = new HashSet<>();

        int size = requests.size();
        for (int i = 0; i < size - 1; i++) {
            BillingByScalesTableCreateRequest currentRequest = requests.get(i);
            String currentTariffScale = currentRequest.getTariffScale();
            String currentMeterNumber = currentRequest.getMeterNumber();
            if (currentTariffScale != null && currentMeterNumber != null) {
                for (int j = i + 1; j < size; j++) {
                    BillingByScalesTableCreateRequest otherRequest = requests.get(j);
                    String otherTariffScale = otherRequest.getTariffScale();
                    String otherMeterNumber = otherRequest.getMeterNumber();
                    if (otherTariffScale != null && otherMeterNumber != null) {
                        // Check for same tariffScale and meterNumber
                        if (currentTariffScale.equals(otherTariffScale) && currentMeterNumber.equals(otherMeterNumber)) {
                            duplicates.add(currentRequest);
                            duplicates.add(otherRequest);
                        }
                    }

                }
            }

        }
        return new ArrayList<>(duplicates);
    }

    private void checkBillingRequestListForDatesEdit(List<BillingByScalesTableEditRequest> billingByScalesTableCreateRequestsList) {
        int size = billingByScalesTableCreateRequestsList.size();
        for (int i = 0; i < size - 1; i++) {
            BillingByScalesTableEditRequest currentRequest = billingByScalesTableCreateRequestsList.get(i);
            LocalDate currentPeriodFrom = currentRequest.getPeriodFrom();
            LocalDate currentPeriodTo = currentRequest.getPeriodTo();
            BigDecimal newMeterReading = currentRequest.getNewMeterReading();
            BigDecimal oldMeterReading = currentRequest.getOldMeterReading();

            for (int j = i + 1; j < size; j++) {
                BillingByScalesTableEditRequest otherRequest = billingByScalesTableCreateRequestsList.get(j);
                LocalDate otherPeriodFrom = otherRequest.getPeriodFrom();
                LocalDate otherPeriodTo = otherRequest.getPeriodTo();
                BigDecimal otherNewMeterReading = otherRequest.getNewMeterReading();
                BigDecimal otherRequestOldMeterReading = otherRequest.getOldMeterReading();

                // Check for date overlap
                if (currentPeriodFrom.isBefore(otherPeriodTo) && otherPeriodFrom.isBefore(currentPeriodTo)) {
                    throw new ClientException(("billingByScalesTableCreateRequests[%s].periodFrom -[PeriodFrom] has overlap;" +
                            "billingByScalesTableCreateRequests[%s].periodTo -[PeriodTo] has overlap;")
                            .formatted(currentRequest.getIndex(), currentRequest.getIndex()), ErrorCode.CONFLICT);
                    // Overlap found
                }
                if (oldMeterReading != null && otherRequestOldMeterReading != null) {
                    if (!(oldMeterReading.compareTo(otherNewMeterReading) == 0)) {
                        throw new ClientException(("billingByScalesTableCreateRequests[%s].oldMeterReading -[OldMeterReading] should be equal to new Meter reading;" +
                                "billingByScalesTableCreateRequests[%s].newMeterReading -[newMeterReading] should be equal to old Meter reading;")
                                .formatted(currentRequest.getIndex(), otherRequest.getIndex()), ErrorCode.CONFLICT);
                    }
                }
            }
        }
    }

    private void checkBillingRequestListForDates(List<BillingByScalesTableCreateRequest> billingByScalesTableCreateRequestsList,
                                                 List<BillingByScalesTableCreateRequest> requestList, List<String> errorMessages,
                                                 Boolean saveRecordForMeterReadings) {
        int size = billingByScalesTableCreateRequestsList.size();
        for (int i = 0; i < size - 1; i++) {
            BillingByScalesTableCreateRequest currentRequest = billingByScalesTableCreateRequestsList.get(i);
            LocalDate currentPeriodFrom = currentRequest.getPeriodFrom();
            LocalDate currentPeriodTo = currentRequest.getPeriodTo();

            for (int j = i + 1; j < size; j++) {
                BillingByScalesTableCreateRequest otherRequest = billingByScalesTableCreateRequestsList.get(j);
                LocalDate otherPeriodFrom = otherRequest.getPeriodFrom();
                LocalDate otherPeriodTo = otherRequest.getPeriodTo();

                // Check for date overlap
                if (currentPeriodFrom.isBefore(otherPeriodTo) && otherPeriodFrom.isBefore(currentPeriodTo)) {
                    errorMessages.add(("billingByScalesTableCreateRequests[%s].periodFrom - [PeriodFrom] has overlap;" +
                            "billingByScalesTableCreateRequests[%s].periodTo - [PeriodTo] has overlap;" +
                            "billingByScalesTableCreateRequests[%s].periodFrom - [PeriodFrom] has overlap;" +
                            "billingByScalesTableCreateRequests[%s].periodTo - [PeriodTo] has overlap;")
                            .formatted(otherRequest.getIndex(), otherRequest.getIndex(), currentRequest.getIndex(), currentRequest.getIndex()));
              /*      throw new ClientException(("billingByScalesTableCreateRequests[%s].periodFrom - [PeriodFrom] has overlap;" +
                            "billingByScalesTableCreateRequests[%s].periodTo - [PeriodTo] has overlap;")
                            .formatted(otherRequest.getIndex()*//*getIndexOfTheObject(otherRequest, requestList)*//*, otherRequest.getIndex()*//*getIndexOfTheObject(otherRequest, requestList)*//*), ErrorCode.CONFLICT);
                    // Overlap found*/
                }
            }
        }
        if (!saveRecordForMeterReadings) {
            for (int i = 0; i < billingByScalesTableCreateRequestsList.size(); i++) {
                BillingByScalesTableCreateRequest item = billingByScalesTableCreateRequestsList.get(i);
                BillingByScalesTableCreateRequest nextItem = getNextItemForCreate(i, billingByScalesTableCreateRequestsList);
                if (item != null && nextItem != null) {
                    if (item.getOldMeterReading() != null && nextItem.getOldMeterReading() != null && item.getNewMeterReading() != null && nextItem.getNewMeterReading() != null) {
                        if (nextItem != null && !(item.getNewMeterReading().compareTo(nextItem.getOldMeterReading()) == 0)) {
                            StringBuilder warningMassage = new StringBuilder();
                            warningMassage.append(("billingByScalesTableCreateRequests[%s].newMeterReading - [newMeterReading] should be equal to new Meter reading;" +
                                    "billingByScalesTableCreateRequests[%s].oldMeterReading - [OldMeterReading] should be equal to old Meter reading;")
                                    .formatted(item.getIndex(), nextItem.getIndex()));
                            warningMassage.insert(0, "%s-".formatted(EPBFinalFields.WARNING_MESSAGE_INDICATOR));
                            throw new ClientException(warningMassage.toString(), ErrorCode.CONFLICT);
//                            errorMessages.add(("billingByScalesTableCreateRequests[%s].newMeterReading - [newMeterReading] should be equal to new Meter reading;" +
//                                    "billingByScalesTableCreateRequests[%s].oldMeterReading - [OldMeterReading] should be equal to old Meter reading;")
//                                    .formatted(item.getIndex(), nextItem.getIndex()));
                      /*  throw new ClientException(("billingByScalesTableCreateRequests[%s].oldMeterReading - [OldMeterReading] should be equal to new Meter reading;" +
                                "billingByScalesTableCreateRequests[%s].newMeterReading - [newMeterReading] should be equal to old Meter reading;")
                                .formatted(item.getIndex(), nextItem.getIndex()), ErrorCode.CONFLICT);*/
                        }
                    }
                }
            }
        }
        for (int i = 0; i < billingByScalesTableCreateRequestsList.size(); i++) {
            for (int j = i + 1; j < billingByScalesTableCreateRequestsList.size(); j++) {
                BillingByScalesTableCreateRequest item1 = billingByScalesTableCreateRequestsList.get(i);
                BillingByScalesTableCreateRequest item2 = billingByScalesTableCreateRequestsList.get(j);
                if (item1.getPeriodFrom() != null && item1.getPeriodTo() != null && item2.getPeriodFrom() != null && item2.getPeriodTo() != null) {
                    if (item1.getPeriodFrom().equals(item2.getPeriodFrom())) {
                        throw new ClientException("billingByScalesTableCreateRequests[%s].periodFrom -[periodFrom] is equal to billingByScalesTableCreateRequests[%s].periodFrom;billingByScalesTableCreateRequests[%s].periodFrom -[periodFrom] is equal to billingByScalesTableCreateRequests[%s].periodFrom;"
                                .formatted(item1.getIndex(), item2.getIndex(), item2.getIndex(), item1.getIndex()), ErrorCode.CONFLICT);
                    }
                    if (item1.getPeriodTo().equals(item2.getPeriodTo())) {
                        throw new ClientException("billingByScalesTableCreateRequests[%s].periodTo -[periodTo] is equal to billingByScalesTableCreateRequests[%s].periodTo;billingByScalesTableCreateRequests[%s].periodTo -[periodTo] is equal to billingByScalesTableCreateRequests[%s].periodTo;"
                                .formatted(item1.getIndex(), item2.getIndex(), item2.getIndex(), item1.getIndex()), ErrorCode.CONFLICT);
                    }
                    if (item1.getPeriodFrom().equals(item2.getPeriodTo())) {
                        throw new ClientException("billingByScalesTableCreateRequests[%s].periodFrom -[periodFrom] is equal to billingByScalesTableCreateRequests[%s].periodTo;billingByScalesTableCreateRequests[%s].periodTo -[periodFrom] is equal to billingByScalesTableCreateRequests[%s].periodFrom;"
                                .formatted(item1.getIndex(), item2.getIndex(), item2.getIndex(), item1.getIndex()), ErrorCode.CONFLICT);
                    }
                    if (item1.getPeriodTo().equals(item2.getPeriodFrom())) {
                        throw new ClientException("billingByScalesTableCreateRequests[%s].periodTo -[periodTo] is equal to billingByScalesTableCreateRequests[%s].periodFrom;billingByScalesTableCreateRequests[%s].periodFrom -[periodFrom] is equal to billingByScalesTableCreateRequests[%s].periodTo;"
                                .formatted(item1.getIndex(), item2.getIndex(), item2.getIndex(), item1.getIndex()), ErrorCode.CONFLICT);
                    }
                }
            }
        }
    }

    private Integer getIndexOfTheObject(BillingByScalesTableCreateRequest otherRequest, List<BillingByScalesTableCreateRequest> requestList) {
        for (int i = 0; i < requestList.size(); i++) {
            if (requestList.get(i).equals(otherRequest)) {
                return i;
            }
        }
        return null;
    }

    public BillingByScalesResponse view(Long id) {
        BillingByScale billingByScale = billingByScaleRepository.findById(id)
                .orElseThrow(() -> new DomainEntityNotFoundException("id -[id] Can't find Billing By scale with id: %s".formatted(id)));
        PointOfDelivery pointOfDelivery =
                pointOfDeliveryRepository.findByIdAndStatusIn(billingByScale.getPodId(), List.of(PodStatus.ACTIVE))
                        .orElseThrow(() -> new DomainEntityNotFoundException("id -[id] Can't find active pod with %s id;".formatted(billingByScale.getPodId())));
        PointOfDeliveryDetails pointOfDeliveryDetails =
                pointOfDeliveryDetailsRepository.findByPodId(pointOfDelivery.getId())
                        .orElseThrow(() -> new DomainEntityNotFoundException("id -[id] Can't find pod Details with pod id %s;".formatted(pointOfDelivery.getId())));
        List<Meter> metersList = meterRepository.findByPodIdAndGridOperatorIdAndStatus(pointOfDelivery.getId(), pointOfDelivery.getGridOperatorId(), MeterStatus.ACTIVE)
                .orElseThrow(() -> new DomainEntityNotFoundException("id -[id] Can't find active meter with pod id %s;".formatted(pointOfDelivery.getId())));

        List<BillingDataByScale> billingDataByScale = billingDataByScaleRepository.findByBillingByScaleIdOrderByIndexAsc(billingByScale.getId());

        BillingByScalesResponse billingByScalesResponse = mapVideObject(billingByScale, pointOfDelivery, pointOfDeliveryDetails, metersList, billingDataByScale);
        billingByScalesResponse.setIsLocked(billingByScale.getInvoiced() != null && billingByScale.getInvoiced());

        return billingByScalesResponse;
    }

    private BillingByScalesResponse mapVideObject(BillingByScale billingByScale, PointOfDelivery pointOfDelivery, PointOfDeliveryDetails pointOfDeliveryDetails, List<Meter> metersList, List<BillingDataByScale> billingDataByScaleList) {
        List<BillingByScalesTableResponse> billingByScalesTable = new ArrayList<>();
        for (BillingDataByScale item : billingDataByScaleList) {
            Scales scales = scalesRepository.findByIdAndStatuses(item.getScaleId(), List.of(NomenclatureItemStatus.ACTIVE))
                    .orElseThrow(() -> new DomainEntityNotFoundException("id -[id] Can't find Active Scale Nomenclature with id: %s".formatted(item.getScaleId())));
            BillingByScalesTableResponse billingDataByScale = BillingByScalesTableResponse.builder()
                    .periodFrom(item.getPeriodFrom())
                    .periodTo(item.getPeriodTo())
                    .meterNumber(getMeterNumber(item.getMeterId()))
                    .scaleId(scales.getId())
                    .scaleNumber(item.getScaleNumber())
                    .scaleCode(item.getScaleNumber() == null ? null : scales.getScaleCode())
                    .scaleType(scales.getScaleType())
                    .tariffScale(item.getScaleNumber() == null ? scales.getTariffScale() : null)
                    .newMeterReading(item.getNewMeterReading())
                    .oldMeterReading(item.getOldMeterReading())
                    .difference(item.getDifferenceKwh())
                    .multiplier(item.getMultiplier())
                    .correction(item.getCorrectionKwh())
                    .deducted(item.getDeductedKwh())
                    .totalVolumes(item.getTotalVolumesKwh())
                    .volumes(item.getVolumes())
                    .unitPrice(item.getUnitPrice())
                    .totalValue(item.getTotalValue())
                    .index(item.getIndex())
                    .build();
            billingByScalesTable.add(billingDataByScale);
        }

        return BillingByScalesResponse.builder()
                .identifier(pointOfDelivery.getIdentifier())
                .dateFrom(billingByScale.getDateFrom())
                .dateTo(billingByScale.getDateTo())
                .billingPowerInKw(billingByScale.getBillingPowerInKw())
                //.numberOfDays(billingByScale)
                .invoiceNumber(billingByScale.getInvoiceNumber())
                .invoiceDate(billingByScale.getInvoiceDate())
                .invoiceCorrection(billingByScale.getInvoiceCorrection())
                .correction(billingByScale.getCorrection())
                .override(billingByScale.getOverride())
                .basisForIssuingTheInvoice(billingByScale.getBasisForIssuingInvoice())
                .status(billingByScale.getStatus())
                .billingByScalesTableCreateRequests(billingByScalesTable)
                .build();
    }

    private String getMeterNumber(Long meterId) {
        if (meterId == null) {
            return null;
        }
        Meter meter = meterRepository.findByIdAndStatusIn(meterId, List.of(MeterStatus.ACTIVE))
                .orElseThrow(() -> new DomainEntityNotFoundException("id -[id] Can't find Active Meter with id: %s".formatted(meterId)));
        return meter.getNumber();
    }

    @Transactional
    public Long delete(Long id) {
        BillingByScale billingByScale = billingByScaleRepository.findById(id)
                .orElseThrow(() -> new DomainEntityNotFoundException("id -[id] Can't find Billing By scale with id: %s".formatted(id)));

        if (Boolean.TRUE.equals(billingByScale.getInvoiced())) {
            throw new ClientException("id -[id] Can't delete because it is connected to invoice", ErrorCode.OPERATION_NOT_ALLOWED);
        }

        if (canDeleteBillingByScales()) {
            if (!billingByScale.getStatus().equals(BillingByScaleStatus.DELETED)) {
                billingByScale.setStatus(BillingByScaleStatus.DELETED);
                billingByScaleRepository.save(billingByScale);
            } else
                throw new ClientException("id -[id] billing by scales with id: %s is already deleted".formatted(id), ErrorCode.CONFLICT);
        }
        return billingByScale.getId();
    }

    private boolean canDeleteBillingByScales() {
        //TODO if BillingByScales is connected to the Billing should delete
        return true;
    }

    public Page<BillingByScaleListResponse> list(BillingByScalesListingRequest request) {
        Sort.Order order = new Sort.Order(request.getDirection(), checkSortField(request));
        return billingByScaleRepository.filter(
                EPBStringUtils.fromPromptToQueryParameter(request.getPrompt()),
                getSearchBy(request.getSearchBy()),
                request.getInvoiced() == null ? null : request.getInvoiced().toString(),
                request.getDateFrom(),
                request.getDateFromTo(),
                request.getDateToFrom(),
                request.getDateTo(),
                getStatuses(),
                PageRequest.of(request.getPage(), request.getSize(), Sort.by(order))
        );
    }

    private String checkSortField(BillingByScalesListingRequest request) {
        if (request.getSortBy() == null) {
            return BillingByScaleListSortBy.ID.getValue();
        } else return request.getSortBy().getValue();
    }


    private List<BillingByScaleStatus> getStatuses() {
        List<BillingByScaleStatus> statuses = new ArrayList<>();
        List<String> context = permissionService.getPermissionsFromContext(PermissionContextEnum.BILLING_BY_SCALES);
        Set<String> viewPermissions = Set.of(BILLING_BY_SCALES_VIEW_BASIC.getId());
        if (context.contains(BILLING_BY_SCALES_VIEW_DELETED.getId())) {
            statuses.add(BillingByScaleStatus.DELETED);
        }
        if (context.stream().anyMatch(viewPermissions::contains)) {
            statuses.add(BillingByScaleStatus.ACTIVE);
        }
        return statuses;
    }

    private String getSearchBy(BillingByScaleListSearchBy searchBy) {
        return Objects.requireNonNullElse(searchBy, BillingByScaleListSearchBy.ALL).getValue();
    }

    @Transactional
    public void checkBillingRequestListForDatesForEdit(List<BillingDataByScale> billingByScalesTableEditRequestsList, Boolean saveRecordForMeterReadings) {
        int size = billingByScalesTableEditRequestsList.size();
        for (int i = 0; i < size - 1; i++) {
            BillingDataByScale currentRequest = billingByScalesTableEditRequestsList.get(i);
            LocalDate currentPeriodFrom = currentRequest.getPeriodFrom();
            LocalDate currentPeriodTo = currentRequest.getPeriodTo();
            BigDecimal newMeterReading = currentRequest.getNewMeterReading();
            BigDecimal oldMeterReading = currentRequest.getOldMeterReading();

            for (int j = i + 1; j < size; j++) {
                BillingDataByScale otherRequest = billingByScalesTableEditRequestsList.get(j);
                LocalDate otherPeriodFrom = otherRequest.getPeriodFrom();
                LocalDate otherPeriodTo = otherRequest.getPeriodTo();
                BigDecimal otherNewMeterReading = otherRequest.getNewMeterReading();
                BigDecimal otherRequestOldMeterReading = otherRequest.getOldMeterReading();

                // Check for date overlap
                if (currentPeriodFrom.isBefore(otherPeriodTo) && otherPeriodFrom.isBefore(currentPeriodTo)) {
                    throw new ClientException(("BillingByScalesTableEditRequest[%s].periodFrom - [PeriodFrom] has overlap;" +
                            "BillingByScalesTableEditRequest[%s].periodTo - [PeriodTo] has overlap;" +
                            "billingByScalesTableCreateRequests[%s].periodFrom - [PeriodFrom] has overlap;" +
                            "billingByScalesTableCreateRequests[%s].periodTo - [PeriodTo] has overlap;"
                    )
                            .formatted(currentRequest.getIndex(), currentRequest.getIndex(), otherRequest.getIndex(), otherRequest.getIndex()), ErrorCode.CONFLICT);
                    // Overlap found
                }
                /*if (oldMeterReading != null) {
                    if (!(oldMeterReading.compareTo(otherNewMeterReading) == 0)) {
                        throw new ClientException(("BillingByScalesTableEditRequest[%s].oldMeterReading- [OldMeterReading] should be equal to new Meter reading;" +
                                "BillingByScalesTableEditRequest[%s].newMeterReading- [newMeterReading] should be equal to old Meter reading;")
                                .formatted(i, j), ErrorCode.CONFLICT);
                    }
                }*/
            }
        }
        if (!saveRecordForMeterReadings) {
            for (int i = 0; i < billingByScalesTableEditRequestsList.size(); i++) {
                BillingDataByScale item = billingByScalesTableEditRequestsList.get(i);
                BillingDataByScale nextItem = getNextItem(i, item, billingByScalesTableEditRequestsList);
                if (item != null && nextItem != null) {
                    if (item.getNewMeterReading() != null && nextItem.getOldMeterReading() != null) {
                        if (nextItem != null && !(item.getNewMeterReading().compareTo(nextItem.getOldMeterReading()) == 0)) {
                            StringBuilder warningMassage = new StringBuilder();
                            warningMassage.append(("billingByScalesTableCreateRequests[%s].newMeterReading - [newMeterReading] should be equal to new Meter reading;" +
                                    "billingByScalesTableCreateRequests[%s].oldMeterReading - [OldMeterReading] should be equal to old Meter reading;")
                                    .formatted(item.getIndex(), nextItem.getIndex()));
                            warningMassage.insert(0, "%s-".formatted(EPBFinalFields.WARNING_MESSAGE_INDICATOR));
                            throw new ClientException(warningMassage.toString(), ErrorCode.CONFLICT);
//                            throw new ClientException(("billingByScalesTableCreateRequests[%s].newMeterReading - [newMeterReading] should be equal to new Meter reading;" +
//                                    "billingByScalesTableCreateRequests[%s].oldMeterReading - [OldMeterReading] should be equal to old Meter reading;")
//                                    .formatted(item.getIndex(), nextItem.getIndex()), ErrorCode.CONFLICT);
                        }
                    }
                }
            }
        }
        for (int i = 0; i < billingByScalesTableEditRequestsList.size(); i++) {
            for (int j = i + 1; j < billingByScalesTableEditRequestsList.size(); j++) {
                BillingDataByScale item1 = billingByScalesTableEditRequestsList.get(i);
                BillingDataByScale item2 = billingByScalesTableEditRequestsList.get(j);
                if (item1.getPeriodFrom() != null && item1.getPeriodTo() != null && item2.getPeriodFrom() != null && item2.getPeriodTo() != null) {
                    if (item1.getPeriodFrom().equals(item2.getPeriodFrom())) {
                        throw new ClientException("billingByScalesTableCreateRequests[%s].periodFrom -[periodFrom] is equal to billingByScalesTableCreateRequests[%s].periodFrom"
                                .formatted(item1.getIndex(), item2.getIndex()), ErrorCode.CONFLICT);
                    }
                    if (item1.getPeriodTo().equals(item2.getPeriodTo())) {
                        throw new ClientException("billingByScalesTableCreateRequests[%s].periodTo -[periodTo] is equal to billingByScalesTableCreateRequests[%s].periodTo"
                                .formatted(item1.getIndex(), item2.getIndex()), ErrorCode.CONFLICT);
                    }
                    if (item1.getPeriodFrom().equals(item2.getPeriodTo())) {
                        throw new ClientException("billingByScalesTableCreateRequests[%s].periodFrom -[periodFrom] is equal to billingByScalesTableCreateRequests[%s].periodTo"
                                .formatted(item1.getIndex(), item2.getIndex()), ErrorCode.CONFLICT);
                    }
                    if (item1.getPeriodTo().equals(item2.getPeriodFrom())) {
                        throw new ClientException("billingByScalesTableCreateRequests[%s].periodTo -[periodTo] is equal to billingByScalesTableCreateRequests[%s].periodFrom"
                                .formatted(item1.getIndex(), item2.getIndex()), ErrorCode.CONFLICT);
                    }
                }
            }
        }
    }

    private BillingDataByScale getNextItem(int i, BillingDataByScale item, List<BillingDataByScale> billingByScalesTableEditRequestsList) {
        if (i + 1 < billingByScalesTableEditRequestsList.size()) {
            return billingByScalesTableEditRequestsList.get(i + 1);
        } else {
            return null;
        }
    }

    private BillingByScalesTableCreateRequest getNextItemForCreate(int i, List<BillingByScalesTableCreateRequest> billingByScalesTableEditRequestsList) {
        if (i + 1 < billingByScalesTableEditRequestsList.size()) {
            return billingByScalesTableEditRequestsList.get(i + 1);
        } else {
            return null;
        }
    }

    @Transactional
    public Long edit(BillingByScalesEditRequest request) {
        List<String> errorMessages = new ArrayList<>();
        BillingByScale billingByScale = billingByScaleRepository.findById(request.getId())
                .orElseThrow(() -> new DomainEntityNotFoundException("id -[id] Can't find BillingByScale with id: %s".formatted(request.getId())));

        if (billingByScale.getStatus().equals(BillingByScaleStatus.DELETED)) {
            throw new ClientException("id -[id] Can't edit deleted billing by scale", ErrorCode.OPERATION_NOT_ALLOWED);
        }

        if (Boolean.TRUE.equals(billingByScale.getInvoiced())) {
            if (!permissionService.permissionContextContainsPermissions(PermissionContextEnum.BILLING_BY_SCALES, List.of(PermissionEnum.BILLING_BY_SCALES_EDIT_LOCKED))) {
                throw new ClientException("id -[id] Can't edit because it is connected to invoice", ErrorCode.OPERATION_NOT_ALLOWED);
            }
        }

        request.setBillingByScalesTableEditRequests(sortBillingByScalesByIndexForEdit(request.getBillingByScalesTableEditRequests()));
        PointOfDelivery pointOfDelivery = pointOfDeliveryRepository.findByIdAndStatusIn(billingByScale.getPodId(), List.of(PodStatus.ACTIVE))
                .orElseThrow(() -> new DomainEntityNotFoundException("id -[id] Can't find active point of delivery with id: %s".formatted(billingByScale.getPodId())));

        PointOfDeliveryDetails pointOfDeliveryDetails = pointOfDeliveryDetailsRepository.findById(pointOfDelivery.getLastPodDetailId()).
                orElseThrow(() -> new DomainEntityNotFoundException("id -[id] Can't find point ot delivery details with id: %s".formatted(pointOfDelivery.getLastPodDetailId())));

        LocalDate dateFrom = billingByScale.getDateFrom();
        LocalDate dateTo = billingByScale.getDateTo();

        List<BillingDataByScale> dbBillingDataByScales = billingDataByScaleRepository.findByBillingByScaleId(billingByScale.getId());
        List<BillingDataByScale> updateBillingDataByScales = new ArrayList<>();
        List<BillingDataByScale> createBillingDataByScales = new ArrayList<>();
        List<BillingDataByScale> combinedBillingByScales;
        List<BillingDataByScale> deleteBillingByScales = new ArrayList<>();
        List<BillingByScalesTableEditRequest> billingByScalesTableEditRequestsList = request.getBillingByScalesTableEditRequests();
        checkScaleCodeMeterDatesAndMeterReadingInEdit(billingByScalesTableEditRequestsList, errorMessages, request.getSaveRecordForMeterReadings());
        for (BillingByScalesTableEditRequest item : billingByScalesTableEditRequestsList) {
            if (!(item.getPeriodFrom().isAfter(dateFrom) || item.getPeriodFrom().isEqual(dateFrom)) ||
                    !(item.getPeriodFrom().isBefore(dateTo) || item.getPeriodFrom().isEqual(dateTo))) {
                throw new ClientException("billingByScalesTableCreateRequests[%s].periodFrom -[periodFrom] is not in range: %s;"
                        .formatted(item.getIndex(), dateFrom), ErrorCode.CONFLICT);
            }
            if (!(item.getPeriodTo().isAfter(dateFrom) || item.getPeriodTo().isEqual(dateFrom)) ||
                    !(item.getPeriodTo().isBefore(dateTo) || item.getPeriodTo().isEqual(dateTo))) {
                throw new ClientException("billingByScalesTableCreateRequests[%s].periodTo -[periodTo] is not in range: %s;"
                        .formatted(item.getIndex(), dateTo), ErrorCode.CONFLICT);
            }
            if (!request.getSaveRecordForIntermediatePeriod()) {
                if (!checkForIntermediatePeriodForEdit(dateFrom, dateTo, request.getBillingByScalesTableEditRequests())) {
                    throw new ClientException("billingByScalesTableCreateRequests[%s].periodFrom - [periodFrom] there is intermediate period dateFrom: %s;dateTo - [dateTo] there is intermediate period dateTo: %s;"
                            .formatted(item.getIndex(), dateFrom, dateTo), ErrorCode.CONFLICT);
                }
            }
            Long scaleId = null;
            BillingDataByScale billingDataByScaleOptional = null;
            if (item.getScaleCode() != null) {
                billingDataByScaleOptional = billingDataByScaleRepository.findByPeriodFromAndBillingByScaleIdAndMeterId(item.getPeriodFrom(), billingByScale.getId(), getMeterId(item.getIndex(), item.getPeriodFrom(), item.getPeriodTo(), item.getMeterNumber()).getId(), item.getScaleCode());
            } else if (item.getTariffScale() != null) {
                Meter meterId = getMeterId(item.getIndex(), item.getPeriodFrom(), item.getPeriodTo(), item.getMeterNumber());
                billingDataByScaleOptional = billingDataByScaleRepository.findByPeriodFromAndBillingByScaleIdAndMeterIdTariffScale(item.getPeriodFrom(), billingByScale.getId(), meterId == null ? null : meterId.getId(), item.getScaleCode());
            }
            if (billingDataByScaleOptional != null) {
                updateBillingDataByScales.add(mapBillingByScales(billingDataByScaleOptional, item, pointOfDelivery.getGridOperatorId(),errorMessages));
            } else {
                checkIfMeterExists(item.getIndex(), item.getMeterNumber());
                Meter meter = null;
                if (StringUtils.isNotEmpty(item.getMeterNumber())) {
                    meter = meterRepository.findByNumberAndGridOperatorIdAndStatus(item.getPeriodFrom(), item.getPeriodTo(), item.getMeterNumber(), pointOfDelivery.getGridOperatorId(), MeterStatus.ACTIVE)
                            .orElseThrow(() -> new DomainEntityNotFoundException("billingByScalesTableCreateRequests[%s].meterNumber -[meterNumber] Meter isn’t installed in this POD in the mentioned period;".formatted(item.getIndex())));
                }
                Scales scale = getScale(item, pointOfDelivery.getGridOperatorId(), errorMessages);
                BillingDataByScale billingDataByScale = BillingDataByScale.builder()
                        .periodFrom(item.getPeriodFrom())
                        .periodTo(item.getPeriodTo())
                        .meterId(meter != null ? meter.getId() : null)
                        //.scaleId(scale.getId())
                        .timeZone(item.getTimeZone())
                        .newMeterReading(item.getNewMeterReading())
                        .oldMeterReading(item.getOldMeterReading())
                        .differenceKwh(item.getDifference())
                        .multiplier(item.getMultiplier())
                        .correctionKwh(item.getCorrection())
                        .deductedKwh(item.getDeducted())
                        .totalVolumesKwh(item.getTotalVolumes())
                        .volumes(item.getVolumes())
                        .unitPrice(item.getUnitPrice())
                        .totalValue(item.getTotalValue())
                        .billingByScaleId(billingByScale.getId())
                        .scaleNumber(item.getScaleNumber())
                        .index(item.getIndex())
                        .build();
                if (scale != null) {
                    billingDataByScale.setScaleId(scale.getId());
                }
                createBillingDataByScales.add(billingDataByScale);
                if (!CollectionUtils.isEmpty(dbBillingDataByScales)) {
                    for (BillingDataByScale dbBillingByScale : dbBillingDataByScales) {
                        if (item.getPeriodFrom().equals(dbBillingByScale.getPeriodFrom()) &&
                                item.getPeriodTo().equals(dbBillingByScale.getPeriodTo())) {
                            deleteBillingByScales.add(dbBillingByScale);
                        }
                    }
                }
            }
        }
        combinedBillingByScales = Stream.of(updateBillingDataByScales, createBillingDataByScales)
                .flatMap(Collection::stream)
                .collect(Collectors.toList());

        if (CollectionUtils.isEmpty(combinedBillingByScales)) {
            billingDataByScaleRepository.deleteAll(dbBillingDataByScales);
            return billingByScale.getId();
        } else {
            for (BillingDataByScale combinedItem : combinedBillingByScales) {
                LocalDate fromDate = combinedItem.getPeriodFrom();
                LocalDate toDate = combinedItem.getPeriodTo();
                for (BillingDataByScale dbItem : dbBillingDataByScales) {
                    if (fromDate.equals(dbItem.getPeriodFrom()) && toDate.equals(dbItem.getPeriodTo())) {
                        break;
                    } else {
                        deleteBillingByScales.add(dbItem);
                    }
                }
            }
        }
        if (!CollectionUtils.isEmpty(deleteBillingByScales)) {
            billingDataByScaleRepository.deleteAll(deleteBillingByScales);
        }
        if (!CollectionUtils.isEmpty(combinedBillingByScales)) {
            for (int i = 0; i < combinedBillingByScales.size(); i++) {
                BillingByScalesTableEditRequest billingByScalesTableEditRequests =
                        request.getBillingByScalesTableEditRequests().get(i);

                String scaleCode = billingByScalesTableEditRequests.getScaleCode();
                String scaleType = billingByScalesTableEditRequests.getScaleType();
                String scaleTariffScale = billingByScalesTableEditRequests.getTariffScale();
                String meterNumber = billingByScalesTableEditRequests.getMeterNumber();
                LocalDate periodFrom = billingByScalesTableEditRequests.getPeriodFrom();
                LocalDate periodTo = billingByScalesTableEditRequests.getPeriodTo();
                Scales scales = null;
                checkIfMeterExists(billingByScalesTableEditRequests.getIndex(), meterNumber);
                Meter meter = null;

                if (StringUtils.isNotEmpty(meterNumber)) {
                    meter = meterRepository.findByNumberAndGridOperatorIdAndStatus(periodFrom, periodTo, meterNumber, pointOfDelivery.getGridOperatorId(), MeterStatus.ACTIVE)
                            .orElseThrow(() -> new DomainEntityNotFoundException("billingByScalesTableCreateRequests[%s].meterNumber -[meterNumber] Meter isn’t installed in this POD in the mentioned period;".formatted(billingByScalesTableEditRequests.getIndex())));

                }
                if (scaleCode != null && !scaleCode.isEmpty() &&
                        scaleType != null && !scaleType.isEmpty()) {

                    scales = scalesRepository.findByScaleCodeAndScaleTypeAndStatusAndGridOperatorId(
                                    scaleCode,
                                    scaleType,
                                    NomenclatureItemStatus.ACTIVE,
                                    meter.getGridOperatorId()
                            )
                            .orElseThrow(() -> new DomainEntityNotFoundException("billingByScalesTableCreateRequests[%s].scaleCode -[scaleCode] Can't find active scales nomenclature with the scaleCode: %s;"
                                    .formatted(billingByScalesTableEditRequests.getIndex(), billingByScalesTableEditRequests.getScaleCode())));

                }
                if (scaleTariffScale != null && !scaleTariffScale.isEmpty() &&
                        scaleType != null && !scaleType.isEmpty()) {
                    scales = scalesRepository.findByTariffScaleAndScaleTypeAndGridOperatorIdAndStatus(
                            scaleTariffScale,
                            scaleType,
                            pointOfDelivery.getGridOperatorId(),
                            NomenclatureItemStatus.ACTIVE).orElseThrow(()->new DomainEntityNotFoundException("billingByScalesTableCreateRequests[%s].tariffScale -[tariffScale] Can't find active scales nomenclature with the TariffScale: %s;"
                            .formatted(billingByScalesTableEditRequests.getIndex(), billingByScalesTableEditRequests.getTariffScale())));

                }
                if (scales == null) {
                    throw new DomainEntityNotFoundException("billingByScalesTableEditRequests[%s].scaleCode -[scaleCode] Can't find Active Scales nomenclature with scalesCode/ScalesTariff;"
                            .formatted(billingByScalesTableEditRequests.getIndex()));
                }
                if (meterNumber != null && !meterNumber.isEmpty()) {
                    checkIfMeterExists(billingByScalesTableEditRequests.getIndex(), meterNumber);
                    Meter innerListMeter = meterRepository.findByNumberAndGridOperatorIdAndStatus(periodFrom, periodTo, meterNumber, pointOfDelivery.getGridOperatorId(), MeterStatus.ACTIVE)
                            .orElseThrow(() -> new DomainEntityNotFoundException("billingByScalesTableCreateRequests[%s].meterNumber -[MeterNumber] Meter isn’t installed in this POD in the mentioned period;".formatted(billingByScalesTableEditRequests.getIndex())));
                    if (periodFrom != null && periodTo != null) {
                        if (!periodFrom.equals(innerListMeter.getInstallmentDate())) {
                            if (!periodFrom.isAfter(innerListMeter.getInstallmentDate())) {
                                throw new ClientException("billingByScalesTableCreateRequests[%s].periodFrom - [PeriodFrom] should be after meterNumber meter installment date;"
                                        .formatted(billingByScalesTableEditRequests.getIndex()), ErrorCode.CONFLICT);
                            }
                        }
                        if (innerListMeter.getRemoveDate() != null) {
                            if (!periodFrom.isBefore(innerListMeter.getRemoveDate())) {
                                throw new ClientException("billingByScalesTableCreateRequests[%s].periodFrom - [PeriodFrom] should be before meterNumber meter removal date;"
                                        .formatted(billingByScalesTableEditRequests.getIndex()), ErrorCode.CONFLICT);
                            }
                        }
                        if (!periodTo.isAfter(innerListMeter.getInstallmentDate())) {
                            throw new ClientException("billingByScalesTableCreateRequests[%s].periodTo - [PeriodTo] should be after meterNumber meter installment date;"
                                    .formatted(billingByScalesTableEditRequests.getIndex()), ErrorCode.CONFLICT);
                        }
                        if (innerListMeter.getRemoveDate() != null) {
                            if (!periodTo.equals(innerListMeter.getRemoveDate())) {
                                if (!periodTo.isBefore(innerListMeter.getRemoveDate())) {
                                    throw new ClientException("billingByScalesTableCreateRequests[%s].periodTo - [PeriodTo] should be before meterNumber meter removal date;"
                                            .formatted(billingByScalesTableEditRequests.getIndex()), ErrorCode.CONFLICT);
                                }
                            }
                        }
                    }
                    Long innerMeterId = innerListMeter.getId();
                    Long scalesId = scales.getId();
                    if (innerMeterId != null && scalesId != null) {
                        meterScaleRepository.findByMeterIdAndScaleIdAndStatus(innerMeterId, scalesId, PodSubObjectStatus.ACTIVE)
                                .orElseThrow(() -> new DomainEntityNotFoundException("billingByScalesTableCreateRequests[%s].periodTo -[periodTo] Can't find meterScales with meterId: %s and ScalesId: %s;"
                                        .formatted(billingByScalesTableEditRequests.getIndex(), innerMeterId, scalesId)));
                    }
//                    if (!request.getSaveRecordForMeterReadings()) {
//                        if (!checkForMeterReadingsForEdit(billingByScalesTableEditRequests, scalesId, pointOfDelivery.getId(), innerMeterId)) {
//                            throw new ClientException("billingByScalesTableCreateRequests[%s].newMeterReading - [NewMeterReading] mismatch ;oldMeterReading - [OldMeterReading]  mismatch;".formatted(billingByScalesTableEditRequests.getIndex()), ErrorCode.CONFLICT);
//                        }
//                    }
                    if (scalesId != null && innerMeterId != null) {
                        checkBillingByScaleOldAndNewMeterReadingsForEdit(scalesId, innerMeterId, pointOfDelivery);
                    }
                }
            }
        }
        combinedBillingByScales = combinedBillingByScalesSort(combinedBillingByScales);
        checkScaleCodeMeterDatesAndMeterReadingForEdit(combinedBillingByScales, request.getSaveRecordForMeterReadings());
        billingDataByScaleRepository.deleteByIdNotIn(billingByScale.getId(), combinedBillingByScales.stream().map(BillingDataByScale::getId).collect(Collectors.toList()));
        if (!CollectionUtils.isEmpty(updateBillingDataByScales)) {
            billingDataByScaleRepository.saveAll(updateBillingDataByScales);
        }
        if (!CollectionUtils.isEmpty(createBillingDataByScales)) {
            billingDataByScaleRepository.saveAll(createBillingDataByScales);
        }
        return billingByScale.getId();
    }

    private List<BillingByScalesTableEditRequest> sortBillingByScalesByIndexForEdit(List<BillingByScalesTableEditRequest> list) {
        if (CollectionUtils.isNotEmpty(list)) {
            list.sort(Comparator.comparingInt(BillingByScalesTableCreateRequest::getIndex));
            return list;
        }
        return list;
    }

    private BillingDataByScale mapBillingByScales(BillingDataByScale billingDataByScale, BillingByScalesTableEditRequest item, Long gridOperatorId, List<String> errorMessages) {
        Meter meter = getMeterId(item.getIndex(), item.getPeriodFrom(), item.getPeriodTo(), item.getMeterNumber());
        billingDataByScale.setPeriodFrom(item.getPeriodFrom());
        billingDataByScale.setPeriodTo(item.getPeriodTo());
        billingDataByScale.setMeterId(meter.getId());
        billingDataByScale.setScaleNumber(item.getScaleNumber());
        Scales scale = getScale(item, gridOperatorId, errorMessages);
        if (scale != null) {
            billingDataByScale.setScaleId(scale.getId());
        } else {
            EPBChainedExceptionTriggerUtil.throwExceptionIfRequired(errorMessages, log);
        }
        billingDataByScale.setNewMeterReading(item.getNewMeterReading());
        billingDataByScale.setOldMeterReading(item.getOldMeterReading());
        billingDataByScale.setDifferenceKwh(item.getDifference());
        billingDataByScale.setMultiplier(item.getMultiplier());
        billingDataByScale.setCorrectionKwh(item.getCorrection());
        billingDataByScale.setDeductedKwh(item.getDeducted());
        billingDataByScale.setTotalVolumesKwh(item.getTotalVolumes());
        billingDataByScale.setVolumes(item.getVolumes());
        billingDataByScale.setUnitPrice(item.getUnitPrice());
        billingDataByScale.setTotalValue(item.getTotalValue());
        return billingDataByScale;
    }

    private Meter getMeterId(int index, LocalDate periodFrom, LocalDate periodTo, String meterNumber) {
        if (StringUtils.isEmpty(meterNumber)) {
            return null;
        }
        checkIfMeterExists(index, meterNumber);
        return meterRepository.findByNumberAndStatus(periodFrom, periodTo, meterNumber, MeterStatus.ACTIVE)
                .orElseThrow(() -> new DomainEntityNotFoundException("billingByScalesTableCreateRequests[%s].meterNumber -[meterNumber] Meter isn’t installed in this POD in the mentioned period;".formatted(index)));
    }


    private void checkBillingByScaleOldAndNewMeterReadingsForEdit(Long scalesId, Long innerMeterId, PointOfDelivery pointOfDelivery) {
        //TODO waiting for the logic to find out
    }

//    private boolean checkForMeterReadingsForEdit(BillingByScalesTableEditRequest item, Long scalesId, Long id, Long innerMeterId) {
//        Boolean status = true;
//        BillingByScalesMaxMinReading minMaxReadings = billingByScaleRepository.findByPodIdAndScaleCodaAndMeter(scalesId, id, innerMeterId);
//        if (item.getOldMeterReading() != null && item.getNewMeterReading() != null) {
//            if (!item.getOldMeterReading().equals(minMaxReadings.getMaxReading())) {
//                status = false;
//            }
//            if (!item.getNewMeterReading().equals(minMaxReadings.getMinReading())) {
//                status = false;
//            }
//        }
//        return status;
//    }

    private boolean checkForIntermediatePeriodForEdit(LocalDate dateFrom, LocalDate dateTo, List<BillingByScalesTableEditRequest> billingByScalesTableEditRequests) {
        Set<LocalDate> initialDays = getDaysSet(dateFrom, dateTo);
        Set<LocalDate> listDays = new HashSet<>();
        for (BillingByScalesTableEditRequest item : billingByScalesTableEditRequests) {
            Set<LocalDate> itemDays = getDaysSet(item.getPeriodFrom(), item.getPeriodTo());
            listDays.addAll(itemDays);
        }
        return initialDays.size() == listDays.size();
    }

    private Scales getScale(BillingByScalesTableEditRequest item, Long gridOperatorId, List<String> errorMessages) {
        Scales scales = null;
        if (item.getScaleCode() != null && !item.getScaleCode().isEmpty() &&
                item.getScaleType() != null && !item.getScaleType().isEmpty()) {
            Optional<Scales> dbScalse = scalesRepository.findByScaleCodeAndScaleTypeAndStatusAndGridOperatorId(
                    item.getScaleCode(),
                    item.getScaleType(),
                    NomenclatureItemStatus.ACTIVE,
                    gridOperatorId
            );
            if (dbScalse.isPresent()) {
                scales = dbScalse.get();
            } else
                errorMessages.add("billingByScalesTableCreateRequests[%s].scaleCode -[scaleCode] Can't find active scales nomenclature with the scaleCode: %s;"
                        .formatted(item.getIndex(), item.getScaleCode()));
            /*
                    .orElseThrow(() -> new DomainEntityNotFoundException("billingByScalesTableCreateRequests[%s].scaleCode -[scaleCode] Can't find active scales nomenclature with the scaleCode: %s;"
                            .formatted(item.getIndex(), item.getScaleCode())));*/

        }
        if (item.getTariffScale() != null && !item.getTariffScale().isEmpty() &&
                item.getScaleType() != null && !item.getScaleType().isEmpty()) {
            Optional<Scales> dbScales = scalesRepository.findByTariffScaleAndScaleTypeAndGridOperatorIdAndStatus(
                    item.getTariffScale(),
                    item.getScaleType(),
                    gridOperatorId,
                    NomenclatureItemStatus.ACTIVE);
             if (dbScales.isEmpty()) {
                errorMessages.add("billingByScalesTableCreateRequests[%s].tariffScale -[tariffScale] Can't find active scales nomenclature with the tariffScale: %s;"
                        .formatted(item.getIndex(), item.getTariffScale()));
            }else{
                 scales = dbScales.get();
             }

  /*
                    .orElseThrow(() -> new DomainEntityNotFoundException("billingByScalesTableCreateRequests[%s].tariffScale -[tariffScale] Can't find active scales nomenclature with the tariffScale: %s;"
                            .formatted(item.getIndex(), item.getTariffScale())));*/

        }
        return scales;
    }

    public void checkIfMeterExists(Integer index, String meterNumber) {
        if (StringUtils.isEmpty(meterNumber)) {
            return;
        }
        List<Meter> innerListMeter = meterRepository.findByNumberAndStatus(meterNumber, MeterStatus.ACTIVE);
        if (CollectionUtils.isEmpty(innerListMeter)) {
            throw new DomainEntityNotFoundException("billingByScalesTableCreateRequests[%s].meterNumber -[MeterNumber]Can't find Meter By meter number: %s;".formatted(index, meterNumber));
        }

    }
}
