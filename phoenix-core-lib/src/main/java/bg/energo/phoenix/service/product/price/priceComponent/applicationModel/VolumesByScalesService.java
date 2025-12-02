package bg.energo.phoenix.service.product.price.priceComponent.applicationModel;

import bg.energo.phoenix.exception.ClientException;
import bg.energo.phoenix.exception.DomainEntityNotFoundException;
import bg.energo.phoenix.exception.ErrorCode;
import bg.energo.phoenix.model.entity.nomenclature.product.Currency;
import bg.energo.phoenix.model.entity.nomenclature.product.priceComponent.Scales;
import bg.energo.phoenix.model.entity.product.price.priceComponent.PriceComponent;
import bg.energo.phoenix.model.entity.product.price.priceComponent.applicationModel.*;
import bg.energo.phoenix.model.enums.nomenclature.NomenclatureItemStatus;
import bg.energo.phoenix.model.enums.product.price.priceComponent.applicationModel.ApplicationModelSubObjectStatus;
import bg.energo.phoenix.model.enums.product.price.priceComponent.applicationModel.VolumesByScaleScalesStatus;
import bg.energo.phoenix.model.enums.product.price.priceComponent.applicationModel.VolumesByScaleStatus;
import bg.energo.phoenix.model.request.product.price.aplicationModel.*;
import bg.energo.phoenix.model.response.nomenclature.priceComponent.ScalesResponse;
import bg.energo.phoenix.model.response.nomenclature.product.currency.CurrencyResponse;
import bg.energo.phoenix.model.response.priceComponent.applicationModel.*;
import bg.energo.phoenix.repository.nomenclature.product.CurrencyRepository;
import bg.energo.phoenix.repository.nomenclature.product.priceComponent.ScalesRepository;
import bg.energo.phoenix.repository.product.price.applicationModel.*;
import bg.energo.phoenix.repository.product.price.priceComponent.PriceComponentRepository;
import bg.energo.phoenix.util.epb.EPBChainedExceptionTriggerUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class VolumesByScalesService implements ApplicationModelBaseService {
    private final VolumesByScaleRepository volumesByScaleRepository;
    private final VolumesByScaleScalesRepository volumesByScaleScalesRepository;
    private final VolumesByScaleIssuingPeriodsRepository volumesByScaleIssuingPeriodsRepository;
    private final VolumesByScaleKwhRestrictionRepository kwhRestrictionRepository;
    private final VolumesByScaleCcyRestrictionRepository ccyRestrictionRepository;
    private final ScalesRepository scalesRepository;
    private final ApplicationModelRepository applicationModelRepository;
    private final PriceComponentRepository priceComponentRepository;
    private final CurrencyRepository currencyRepository;

    @Override
    public void create(ApplicationModel model, ApplicationModelRequest modelRequest) {
        VolumesByScaleRequest request = modelRequest.getVolumesByScaleRequest();
        VolumesByScale newVolumesByScale = new VolumesByScale();
        newVolumesByScale.setYearRound(request.isYearRound());
        newVolumesByScale.setApplicationModel(model);
        newVolumesByScale.setStatus(VolumesByScaleStatus.ACTIVE);
        newVolumesByScale.setRestrictionOfApplicationOnVolume(request.isHasVolumeRestriction());
        newVolumesByScale.setRestrictionOfApplicationOnValues(request.isHasValueRestriction());
        newVolumesByScale.setVolumeRestrictionPercent(request.getKwhRestrictionPercent());
        VolumesByScale volumesByScale = volumesByScaleRepository.save(newVolumesByScale);
        if (!request.isYearRound()) {
            List<APPeriodOfYearRequest> periodsOfYear = request.getPeriodsOfYear();
            List<VolumesByScaleIssuingPeriods> periods = periodsOfYear.stream().map(x -> new VolumesByScaleIssuingPeriods(x, volumesByScale)).toList();
            volumesByScaleIssuingPeriodsRepository.saveAll(periods);
        }
        if (request.isHasVolumeRestriction()) {
            List<VolumeRanges> volumeRanges = request.getKwhRestriction();
            volumesByScale.setVolumeRestrictionPercent(request.getKwhRestrictionPercent());
            List<VolumesByScaleKwhRestriction> kwhRestrictions = volumeRanges.stream().map(x -> new VolumesByScaleKwhRestriction(x, volumesByScale)).toList();
            kwhRestrictionRepository.saveAll(kwhRestrictions);
        }
        if (request.isHasValueRestriction()) {
            List<ValueRanges> valueRanges = request.getCcyRestriction();
            List<VolumesByScaleCcyRestriction> restrictions = new ArrayList<>();
            int index = 0;
            for (ValueRanges valueRange : valueRanges) {
                boolean currencyExists = currencyRepository.existsByIdAndStatus(valueRange.getCurrency(), NomenclatureItemStatus.ACTIVE);
                if (!currencyExists) {
                    throw new DomainEntityNotFoundException(String.format("applicationModelRequest.volumesByScaleRequest.ccyRestriction[%s].currencyId-Currency do not exists;", index));
                }
                restrictions.add(new VolumesByScaleCcyRestriction(valueRange, volumesByScale));
                index++;
            }
            ccyRestrictionRepository.saveAll(restrictions);
        }
        List<Scales> scales = new ArrayList<>();
        Set<Long> scaleIds = request.getScaleIds();
        int index = 0;
        List<String> errorMessages = new ArrayList<>();

        for (Long scaleId : scaleIds) {
            Optional<Scales> scalesOptional = scalesRepository.findByIdAndStatuses(scaleId, List.of(NomenclatureItemStatus.ACTIVE));
            if (scalesOptional.isEmpty()) {
                errorMessages.add(String.format("applicationModelRequest.volumesByScaleRequest.scaleIds.data-%s~Scale not found", scaleId));
                continue;
            }
            scales.add(scalesOptional.get());
            index++;
        }
        EPBChainedExceptionTriggerUtil.throwExceptionIfRequired(errorMessages, log);

        List<VolumesByScaleScales> volumesByScaleScales = scales.stream().map(x -> new VolumesByScaleScales(volumesByScale, x)).toList();
        volumesByScaleScalesRepository.saveAll(volumesByScaleScales);

    }

    @Override
    public void update(ApplicationModel applicationModel, ApplicationModelRequest modelRequest) {
        VolumesByScale volumesByScale = volumesByScaleRepository.findByApplicationModelIdAndStatusIn(applicationModel.getId(), List.of(VolumesByScaleStatus.ACTIVE))
                .orElseThrow(() -> {
                    throw new ClientException("priceComponentId-Application model not found by id", ErrorCode.DOMAIN_ENTITY_NOT_FOUND);
                });
        VolumesByScaleRequest request = modelRequest.getVolumesByScaleRequest();
        checkAndDelete(volumesByScale, request);

        if (request.isHasVolumeRestriction()) {
            updateKwhRestrictions(volumesByScale, request);
        }
        if (request.isHasValueRestriction()) {
            updateCcyRestrictions(volumesByScale, request);
        }
        if (!request.isYearRound()) {
            updateIssuingPeriods(volumesByScale, request);
        }
        createScales(volumesByScale, request);

        volumesByScale.setYearRound(request.isYearRound());
        volumesByScale.setRestrictionOfApplicationOnValues(request.isHasValueRestriction());
        volumesByScale.setRestrictionOfApplicationOnVolume(request.isHasVolumeRestriction());
        volumesByScaleRepository.save(volumesByScale);
    }

    private void createScales(VolumesByScale volumesByScale, VolumesByScaleRequest request) {
        Set<Long> scaleIds = request.getScaleIds();
        List<VolumesByScaleScales> volumesByScalesScales = volumesByScaleScalesRepository.findAllByVolumesByScaleIdAndStatus(volumesByScale.getId(), VolumesByScaleScalesStatus.ACTIVE);

        for (VolumesByScaleScales scaleOfVolume : volumesByScalesScales) {
            if (!scaleIds.remove(scaleOfVolume.getScales().getId())) {
                scaleOfVolume.setStatus(VolumesByScaleScalesStatus.DELETED);
            }
        }
        List<Scales> scales = scalesRepository.findScaleIdsInAndStatuses(scaleIds, List.of(NomenclatureItemStatus.ACTIVE));
        for (Scales scale : scales) {
            volumesByScalesScales.add(new VolumesByScaleScales(volumesByScale, scale));
            scaleIds.remove(scale.getId());
        }
        List<String> errorMessages = new ArrayList<>();
        for (Long scaleId : scaleIds) {
            errorMessages.add("applicationModelRequest.volumesByScaleRequest.scaleIds.data-" + scaleId + "~Scale not found");
        }

        EPBChainedExceptionTriggerUtil.throwExceptionIfRequired(errorMessages, log);

        volumesByScaleScalesRepository.saveAll(volumesByScalesScales);
    }

    private void updateIssuingPeriods(VolumesByScale volumesByScale, VolumesByScaleRequest request) {
        List<VolumesByScaleIssuingPeriods> issuingPeriods = volumesByScaleIssuingPeriodsRepository.findAllByVolumesByScaleIdAndStatus(volumesByScale.getId(), ApplicationModelSubObjectStatus.ACTIVE);
        List<APPeriodOfYearRequest> periodsOfYear = request.getPeriodsOfYear();
        Map<Long, APPeriodOfYearRequest> collect = periodsOfYear.stream().filter(x -> x.getId() != null).collect(Collectors.toMap(APPeriodOfYearRequest::getId, j -> j));
        for (VolumesByScaleIssuingPeriods period : issuingPeriods) {
            APPeriodOfYearRequest periodEdit = collect.get(period.getId());
            if (periodEdit == null) {
                period.setStatus(ApplicationModelSubObjectStatus.DELETED);
            } else {
                period.setPeriodFrom(periodEdit.getStartDate());
                period.setPeriodTo(periodEdit.getEndDate());
            }
        }
        List<VolumesByScaleIssuingPeriods> newIssuingPeriods = periodsOfYear.stream().filter(x -> x.getId() == null)
                .map(x -> new VolumesByScaleIssuingPeriods(x, volumesByScale)).toList();
        issuingPeriods.addAll(newIssuingPeriods);
        volumesByScaleIssuingPeriodsRepository.saveAll(issuingPeriods);
    }

    private void updateCcyRestrictions(VolumesByScale volumesByScale, VolumesByScaleRequest request) {
        List<VolumesByScaleCcyRestriction> ccyRestrictions = ccyRestrictionRepository.findAllByVolumesByScaleIdAndStatus(volumesByScale.getId(), ApplicationModelSubObjectStatus.ACTIVE);
        List<ValueRanges> rangesEdits = request.getCcyRestriction();
        Map<Long, ValueRanges> collect = rangesEdits.stream().filter(x -> x.getId() != null).collect(Collectors.toMap(ValueRanges::getId, j -> j));

        int index = 0;
        for (VolumesByScaleCcyRestriction ccyRestriction : ccyRestrictions) {
            ValueRanges valueRangesEdit = collect.get(ccyRestriction.getId());
            if (valueRangesEdit == null) {
                ccyRestriction.setStatus(ApplicationModelSubObjectStatus.DELETED);
            } else {
                ccyRestriction.setValueFrom(valueRangesEdit.getValueFrom());
                ccyRestriction.setValueTo(valueRangesEdit.getValueTo());
                if (!ccyRestriction.getCurrencyId().equals(valueRangesEdit.getCurrency())) {
                    boolean currencyExists = currencyRepository.existsByIdAndStatus(valueRangesEdit.getCurrency(), NomenclatureItemStatus.ACTIVE);
                    if (!currencyExists) {
                        throw new DomainEntityNotFoundException(String.format("applicationModelRequest.volumesByScaleRequest.ccyRestriction[%s].currencyId-Currency do not exists;", index));
                    }
                    ccyRestriction.setCurrencyId(valueRangesEdit.getCurrency());
                }
            }
            index++;
        }

        List<VolumesByScaleCcyRestriction> newRestrictions = new ArrayList<>();
        index = 0;
        for (ValueRanges valueRange : rangesEdits) {
            if (valueRange.getId() != null) {
                continue;
            }
            boolean currencyExists = currencyRepository.existsByIdAndStatus(valueRange.getCurrency(), NomenclatureItemStatus.ACTIVE);
            if (!currencyExists) {
                throw new DomainEntityNotFoundException(String.format("applicationModelRequest.volumesByScaleRequest.ccyRestriction[%s].currencyId-Currency do not exists;", index));
            }
            newRestrictions.add(new VolumesByScaleCcyRestriction(valueRange, volumesByScale));
            index++;
        }

        ccyRestrictions.addAll(newRestrictions);
        ccyRestrictionRepository.saveAll(ccyRestrictions);
    }

    private void updateKwhRestrictions(VolumesByScale volumesByScale, VolumesByScaleRequest request) {
        List<VolumesByScaleKwhRestriction> kwhRestrictions = kwhRestrictionRepository.findAllByVolumesByScaleIdAndStatus(volumesByScale.getId(), ApplicationModelSubObjectStatus.ACTIVE);
        List<VolumeRanges> rangesEdits = request.getKwhRestriction();
        Map<Long, VolumeRanges> collect = rangesEdits.stream().filter(x -> x.getId() != null).collect(Collectors.toMap(VolumeRanges::getId, j -> j));
        for (VolumesByScaleKwhRestriction kwhRestriction : kwhRestrictions) {
            VolumeRanges volumeRangesEdit = collect.get(kwhRestriction.getId());
            if (volumeRangesEdit == null) {
                kwhRestriction.setStatus(ApplicationModelSubObjectStatus.DELETED);
            } else {
                kwhRestriction.setValueFrom(volumeRangesEdit.getValueFrom());
                kwhRestriction.setValueTo(volumeRangesEdit.getValueTo());
            }
        }
        List<VolumesByScaleKwhRestriction> newRestrictions = rangesEdits.stream().filter(x -> x.getId() == null)
                .map(x -> new VolumesByScaleKwhRestriction(x, volumesByScale)).toList();
        volumesByScale.setVolumeRestrictionPercent(request.getKwhRestrictionPercent());
        kwhRestrictions.addAll(newRestrictions);
        kwhRestrictionRepository.saveAll(kwhRestrictions);
    }

    //Checks if values are changed to boolean and deletes old values.
    private void checkAndDelete(VolumesByScale volumesByScale, VolumesByScaleRequest request) {
        if (request.isYearRound() && !volumesByScale.getYearRound()) {
            List<VolumesByScaleIssuingPeriods> volumesByScaleIssuingPeriods = deleteAllIssuingPeriods(volumesByScale);
            volumesByScaleIssuingPeriodsRepository.saveAll(volumesByScaleIssuingPeriods);
            volumesByScale.setYearRound(request.isYearRound());
        }
        if (!request.isHasVolumeRestriction() && volumesByScale.getRestrictionOfApplicationOnVolume()) {
            List<VolumesByScaleKwhRestriction> kwhRestrictions = deleteKwhRestriction(volumesByScale);
            kwhRestrictionRepository.saveAll(kwhRestrictions);
            volumesByScale.setRestrictionOfApplicationOnVolume(request.isHasVolumeRestriction());
        }
        if (!request.isHasValueRestriction() && volumesByScale.getRestrictionOfApplicationOnValues()) {
            List<VolumesByScaleCcyRestriction> restrictions = deleteCcyRestriction(volumesByScale);
            ccyRestrictionRepository.saveAll(restrictions);
            volumesByScale.setRestrictionOfApplicationOnValues(request.isHasValueRestriction());
        }
    }

    @Override
    public void delete(Long modelId) {
        VolumesByScale volumesByScale = volumesByScaleRepository.findByApplicationModelIdAndStatusIn(modelId, List.of(VolumesByScaleStatus.ACTIVE))
                .orElseThrow(() -> new ClientException("priceComponentId-Application model not found by id", ErrorCode.DOMAIN_ENTITY_NOT_FOUND));
        volumesByScale.setStatus(VolumesByScaleStatus.DELETED);
        List<VolumesByScaleScales> allByVolumesByScaleIdAndStatus = volumesByScaleScalesRepository.findAllByVolumesByScaleIdAndStatus(volumesByScale.getId(), VolumesByScaleScalesStatus.ACTIVE);
        allByVolumesByScaleIdAndStatus.forEach(x -> x.setStatus(VolumesByScaleScalesStatus.DELETED));
        List<VolumesByScaleIssuingPeriods> issuingPeriods = deleteAllIssuingPeriods(volumesByScale);
        List<VolumesByScaleKwhRestriction> kwhRestrictions = deleteKwhRestriction(volumesByScale);
        List<VolumesByScaleCcyRestriction> ccyRestrictions = deleteCcyRestriction(volumesByScale);

        volumesByScaleRepository.save(volumesByScale);
        volumesByScaleScalesRepository.saveAll(allByVolumesByScaleIdAndStatus);
        volumesByScaleIssuingPeriodsRepository.saveAll(issuingPeriods);
        kwhRestrictionRepository.saveAll(kwhRestrictions);
        ccyRestrictionRepository.saveAll(ccyRestrictions);
    }

    private List<VolumesByScaleCcyRestriction> deleteCcyRestriction(VolumesByScale volumesByScale) {
        List<VolumesByScaleCcyRestriction> ccyRestrictions = ccyRestrictionRepository.findAllByVolumesByScaleIdAndStatus(volumesByScale.getId(), ApplicationModelSubObjectStatus.ACTIVE);
        ccyRestrictions.forEach(x -> x.setStatus(ApplicationModelSubObjectStatus.DELETED));
        return ccyRestrictions;
    }

    private List<VolumesByScaleKwhRestriction> deleteKwhRestriction(VolumesByScale volumesByScale) {
        List<VolumesByScaleKwhRestriction> kwhRestrictions = kwhRestrictionRepository.findAllByVolumesByScaleIdAndStatus(volumesByScale.getId(), ApplicationModelSubObjectStatus.ACTIVE);
        kwhRestrictions.forEach(x -> x.setStatus(ApplicationModelSubObjectStatus.DELETED));
        return kwhRestrictions;
    }

    private List<VolumesByScaleIssuingPeriods> deleteAllIssuingPeriods(VolumesByScale volumesByScale) {
        List<VolumesByScaleIssuingPeriods> issuingPeriods = volumesByScaleIssuingPeriodsRepository.findAllByVolumesByScaleIdAndStatus(volumesByScale.getId(), ApplicationModelSubObjectStatus.ACTIVE);
        issuingPeriods.forEach(x -> x.setStatus(ApplicationModelSubObjectStatus.DELETED));
        return issuingPeriods;
    }

    @Override
    public ApplicationModelResponse view(ApplicationModel model) {
        VolumesByScaleResponse volumesByScaleResponse = new VolumesByScaleResponse();
        VolumesByScale volumesByScale = volumesByScaleRepository.findByApplicationModelIdAndStatusIn(model.getId(), List.of(VolumesByScaleStatus.ACTIVE))
                .orElseThrow(() -> new ClientException("priceComponentId-Application model not found by id", ErrorCode.DOMAIN_ENTITY_NOT_FOUND));
        List<ScalesResponse> scales = volumesByScaleScalesRepository.findAllByVolumesByScaleIdAndStatus(volumesByScale.getId(), VolumesByScaleScalesStatus.ACTIVE)
                .stream().map(VolumesByScaleScales::getScales).map(ScalesResponse::new).toList();
        volumesByScaleResponse.setScalesResponse(scales);
        if (Boolean.FALSE.equals(volumesByScale.getYearRound())) {
            List<IssuingPeriodsResponse> issuingPeriodsResponses = volumesByScaleIssuingPeriodsRepository.findAllByVolumesByScaleIdAndStatus(volumesByScale.getId(), ApplicationModelSubObjectStatus.ACTIVE)
                    .stream().map(IssuingPeriodsResponse::new).toList();
            volumesByScaleResponse.setPeriodsOfYear(issuingPeriodsResponses);
        }
        if (Boolean.TRUE.equals(volumesByScale.getRestrictionOfApplicationOnVolume())) {
            List<KwhRestrictionResponse> kwhRestrictionResponses = kwhRestrictionRepository.findAllByVolumesByScaleIdAndStatus(volumesByScale.getId(), ApplicationModelSubObjectStatus.ACTIVE)
                    .stream().map(KwhRestrictionResponse::new).toList();
            volumesByScaleResponse.setKwhRestriction(kwhRestrictionResponses);
            volumesByScaleResponse.setHasVolumeRestriction(volumesByScale.getRestrictionOfApplicationOnVolume());
            volumesByScaleResponse.setVolumeRestrictionPercent(volumesByScale.getVolumeRestrictionPercent());
        }
        if (Boolean.TRUE.equals(volumesByScale.getRestrictionOfApplicationOnValues())) {
            List<VolumesByScaleCcyRestriction> ccyRestrictions = ccyRestrictionRepository.findAllByVolumesByScaleIdAndStatus(volumesByScale.getId(), ApplicationModelSubObjectStatus.ACTIVE);
            List<CcyRestrictionResponse> ccyRestrictionResponses = new ArrayList<>();

            for (VolumesByScaleCcyRestriction ccyRestriction : ccyRestrictions) {
                CcyRestrictionResponse ccyRestrictionResponse = new CcyRestrictionResponse(ccyRestriction);
                Currency currency = currencyRepository.findByIdAndStatus(ccyRestriction.getCurrencyId(), List.of(NomenclatureItemStatus.ACTIVE, NomenclatureItemStatus.INACTIVE))
                        .orElseThrow(() -> new DomainEntityNotFoundException("id-Currency do not exists"));
                ccyRestrictionResponse.setCurrencyResponse(new CurrencyResponse(currency));
                ccyRestrictionResponses.add(ccyRestrictionResponse);
            }
            volumesByScaleResponse.setCcyRestriction(ccyRestrictionResponses);
            volumesByScaleResponse.setHasValueRestriction(volumesByScale.getRestrictionOfApplicationOnValues());
        }


        volumesByScaleResponse.setYearRound(volumesByScale.getYearRound());
        volumesByScaleResponse.setHasVolumeRestriction(volumesByScale.getRestrictionOfApplicationOnVolume());
        volumesByScaleResponse.setHasValueRestriction(volumesByScale.getRestrictionOfApplicationOnValues());


        ApplicationModelResponse applicationModelResponse = new ApplicationModelResponse(model.getApplicationModelType(), model.getApplicationType(), model.getApplicationLevel());
        applicationModelResponse.setVolumesByScaleResponse(volumesByScaleResponse);
        return applicationModelResponse;
    }

    @Override
    public void clone(ApplicationModel source, ApplicationModel clone) {
        VolumesByScale volumesByScale = volumesByScaleRepository
                .findByApplicationModelIdAndStatusIn(source.getId(), List.of(VolumesByScaleStatus.ACTIVE))
                .orElseThrow(() -> new DomainEntityNotFoundException("VolumesByScale not found while cloning application model id %s;".formatted(source.getId())));

        VolumesByScale clonedVolumesByScale = cloneVolumesByScales(source, clone, volumesByScale);

        // clone scales
        List<VolumesByScaleScales> scalesList = volumesByScaleScalesRepository.findAllByVolumesByScaleIdAndStatus(
                volumesByScale.getId(),
                VolumesByScaleScalesStatus.ACTIVE
        );

        for (VolumesByScaleScales scales : scalesList) {
            VolumesByScaleScales clonedScales = new VolumesByScaleScales();
            clonedScales.setVolumesByScale(clonedVolumesByScale);
            clonedScales.setScales(scales.getScales());
            clonedScales.setStatus(VolumesByScaleScalesStatus.ACTIVE);
            volumesByScaleScalesRepository.save(clonedScales);
        }

        List<VolumesByScaleCcyRestriction> ccyRestrictions = ccyRestrictionRepository.findAllByVolumesByScaleIdAndStatus(
                source.getId(),
                ApplicationModelSubObjectStatus.ACTIVE
        );

        if (CollectionUtils.isNotEmpty(ccyRestrictions)) {
            for (VolumesByScaleCcyRestriction ccyRestriction : ccyRestrictions) {
                VolumesByScaleCcyRestriction clonedCcyRestriction = new VolumesByScaleCcyRestriction();
                clonedCcyRestriction.setVolumesByScale(clonedVolumesByScale);
                clonedCcyRestriction.setValueFrom(ccyRestriction.getValueFrom());
                clonedCcyRestriction.setValueTo(ccyRestriction.getValueTo());
                clonedCcyRestriction.setStatus(ApplicationModelSubObjectStatus.ACTIVE);
                clonedCcyRestriction.setCurrencyId(ccyRestriction.getCurrencyId());
                ccyRestrictionRepository.save(clonedCcyRestriction);
            }
        }

        volumesByScaleRepository.save(clonedVolumesByScale);
    }

    @Override
    public boolean copy(ApplicationModel source, ApplicationModel copied, PriceComponent priceComponent) {
        VolumesByScale volumesByScale = volumesByScaleRepository
                .findByApplicationModelIdAndStatusIn(source.getId(), List.of(VolumesByScaleStatus.ACTIVE))
                .orElseThrow(() -> new DomainEntityNotFoundException("VolumesByScale not found while cloning application model id %s;".formatted(source.getId())));


        List<Scales> scalesList = volumesByScaleScalesRepository.findScalesByVolumesByScaleAndStatus(
                volumesByScale.getId(),
                VolumesByScaleScalesStatus.ACTIVE
        );
        for (Scales scales : scalesList) {
            if (!scales.getStatus().equals(NomenclatureItemStatus.ACTIVE)) {
                return false;
            }
        }

        List<VolumesByScaleCcyRestriction> ccyRestrictions = ccyRestrictionRepository.findAllByVolumesByScaleIdAndStatus(
                source.getId(),
                ApplicationModelSubObjectStatus.ACTIVE
        );
        for (VolumesByScaleCcyRestriction ccyRestriction : ccyRestrictions) {
            boolean currencyExists = currencyRepository.existsByIdAndStatus(ccyRestriction.getCurrencyId(), NomenclatureItemStatus.ACTIVE);
            if (!currencyExists) {
                return false;
            }
        }


        priceComponentRepository.saveAndFlush(priceComponent);
        copied.setPriceComponent(priceComponent);

        applicationModelRepository.saveAndFlush(copied);
        VolumesByScale clonedVolumesByScale = cloneVolumesByScales(source, copied, volumesByScale);

        for (Scales scales : scalesList) {
            VolumesByScaleScales clonedScales = new VolumesByScaleScales();
            clonedScales.setVolumesByScale(clonedVolumesByScale);
            clonedScales.setScales(scales);
            clonedScales.setStatus(VolumesByScaleScalesStatus.ACTIVE);
            volumesByScaleScalesRepository.save(clonedScales);
        }
        if (CollectionUtils.isNotEmpty(ccyRestrictions)) {
            for (VolumesByScaleCcyRestriction ccyRestriction : ccyRestrictions) {
                VolumesByScaleCcyRestriction clonedCcyRestriction = new VolumesByScaleCcyRestriction();
                clonedCcyRestriction.setVolumesByScale(clonedVolumesByScale);
                clonedCcyRestriction.setValueFrom(ccyRestriction.getValueFrom());
                clonedCcyRestriction.setValueTo(ccyRestriction.getValueTo());
                clonedCcyRestriction.setStatus(ApplicationModelSubObjectStatus.ACTIVE);
                clonedCcyRestriction.setCurrencyId(ccyRestriction.getCurrencyId());
                ccyRestrictionRepository.save(clonedCcyRestriction);
            }
        }

        volumesByScaleRepository.save(clonedVolumesByScale);
        return true;
    }

    private VolumesByScale cloneVolumesByScales(ApplicationModel source, ApplicationModel clone, VolumesByScale volumesByScale) {
        VolumesByScale clonedVolumesByScale = new VolumesByScale();
        clonedVolumesByScale.setRestrictionOfApplicationOnVolume(volumesByScale.getRestrictionOfApplicationOnVolume());
        clonedVolumesByScale.setRestrictionOfApplicationOnValues(volumesByScale.getRestrictionOfApplicationOnValues());
        clonedVolumesByScale.setYearRound(volumesByScale.getYearRound());
        clonedVolumesByScale.setApplicationModel(clone);
        clonedVolumesByScale.setStatus(VolumesByScaleStatus.ACTIVE);
        volumesByScaleRepository.saveAndFlush(clonedVolumesByScale);

        // clone issuing periods if not year round
        if (Boolean.FALSE.equals(volumesByScale.getYearRound())) {
            List<VolumesByScaleIssuingPeriods> issuingPeriods = volumesByScaleIssuingPeriodsRepository
                    .findAllByVolumesByScaleIdAndStatus(
                            volumesByScale.getId(),
                            ApplicationModelSubObjectStatus.ACTIVE
                    );

            for (VolumesByScaleIssuingPeriods periods : issuingPeriods) {
                VolumesByScaleIssuingPeriods clonedIssuingPeriods = new VolumesByScaleIssuingPeriods();
                clonedIssuingPeriods.setVolumesByScale(clonedVolumesByScale);
                clonedIssuingPeriods.setPeriodFrom(periods.getPeriodFrom());
                clonedIssuingPeriods.setPeriodTo(periods.getPeriodTo());
                clonedIssuingPeriods.setStatus(ApplicationModelSubObjectStatus.ACTIVE);
                volumesByScaleIssuingPeriodsRepository.save(clonedIssuingPeriods);
            }
        }

        // clone kwh restrictions if volume restriction percent is not null
        if (volumesByScale.getVolumeRestrictionPercent() != null) {
            clonedVolumesByScale.setVolumeRestrictionPercent(volumesByScale.getVolumeRestrictionPercent());
            List<VolumesByScaleKwhRestriction> kwhRestrictions = kwhRestrictionRepository.findAllByVolumesByScaleIdAndStatus(
                    volumesByScale.getId(),
                    ApplicationModelSubObjectStatus.ACTIVE
            );

            for (VolumesByScaleKwhRestriction kwhRestriction : kwhRestrictions) {
                VolumesByScaleKwhRestriction clonedKwhRestriction = new VolumesByScaleKwhRestriction();
                clonedKwhRestriction.setVolumesByScale(clonedVolumesByScale);
                clonedKwhRestriction.setValueFrom(kwhRestriction.getValueFrom());
                clonedKwhRestriction.setValueTo(kwhRestriction.getValueTo());
                clonedKwhRestriction.setStatus(ApplicationModelSubObjectStatus.ACTIVE);
                kwhRestrictionRepository.save(clonedKwhRestriction);
            }
        }

        // clone ccy restrictions if restriction of application on values is true

        return clonedVolumesByScale;
    }
}
