package bg.energo.phoenix.service.product.price.priceComponent.applicationModel;

import bg.energo.phoenix.exception.ClientException;
import bg.energo.phoenix.exception.DomainEntityNotFoundException;
import bg.energo.phoenix.exception.ErrorCode;
import bg.energo.phoenix.model.entity.nomenclature.pod.Profiles;
import bg.energo.phoenix.model.entity.nomenclature.product.Currency;
import bg.energo.phoenix.model.entity.product.price.priceComponent.PriceComponent;
import bg.energo.phoenix.model.entity.product.price.priceComponent.applicationModel.*;
import bg.energo.phoenix.model.enums.nomenclature.NomenclatureItemStatus;
import bg.energo.phoenix.model.enums.product.price.priceComponent.applicationModel.ApplicationModelSubObjectStatus;
import bg.energo.phoenix.model.enums.product.price.priceComponent.applicationModel.MinuteRange;
import bg.energo.phoenix.model.enums.product.price.priceComponent.applicationModel.Periodicity;
import bg.energo.phoenix.model.request.product.iap.interimAdvancePayment.periodical.edit.EditDateOfMonthRequest;
import bg.energo.phoenix.model.request.product.iap.interimAdvancePayment.periodical.edit.EditDayOfWeekPeriodOfYear;
import bg.energo.phoenix.model.request.product.iap.interimAdvancePayment.periodical.edit.EditDayOfWeekRequest;
import bg.energo.phoenix.model.request.product.iap.interimAdvancePayment.periodical.edit.EditPeriodOfYearRequest;
import bg.energo.phoenix.model.request.product.price.aplicationModel.*;
import bg.energo.phoenix.model.response.nomenclature.product.currency.CurrencyResponse;
import bg.energo.phoenix.model.response.priceComponent.applicationModel.*;
import bg.energo.phoenix.repository.nomenclature.pod.ProfilesRepository;
import bg.energo.phoenix.repository.nomenclature.product.CurrencyRepository;
import bg.energo.phoenix.repository.product.price.applicationModel.*;
import bg.energo.phoenix.repository.product.price.priceComponent.PriceComponentRepository;
import bg.energo.phoenix.util.epb.EPBChainedExceptionTriggerUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Service
@Slf4j
public class VolumesBySettlementPeriodService implements ApplicationModelBaseService {
    private final VolumesBySettlementPeriodRepository volumesBySettlementPeriodRepository;

    private final SettlementPeriodsDateOfMonthsRepository dateOfMonthsRepository;
    private final SettlementPeriodsIssuingPeriodsRepository issuingPeriodsRepository;
    private final SettlementPeriodsDayWeekPeriodYearRepository dayWeekPeriodYearRepository;
    private final SettlementPeriodsKwhRestrictionsRepository kwhRestrictionsRepository;
    private final SettlementPeriodsCcyRestrictionsRepository ccyRestrictionsRepository;
    private final SettlementPeriodsProfileRepository settlementPeriodsProfileRepository;
    private final SettlementPeriodRepository settlementPeriodRepository;
    private final ProfilesRepository profilesRepository;
    private final CurrencyRepository currencyRepository;
    private final ApplicationModelRepository applicationModelRepository;
    private final PriceComponentRepository priceComponentRepository;

    @Override
    public void create(ApplicationModel model, ApplicationModelRequest modelRequest) {
        VolumesBySettlementPeriodsRequest request = modelRequest.getSettlementPeriodsRequest();
        Periodicity periodType = request.getPeriodType();
        VolumesBySettlementPeriod settlementPeriod = new VolumesBySettlementPeriod();
        settlementPeriod.setStatus(ApplicationModelSubObjectStatus.ACTIVE);
        settlementPeriod.setApplicationModel(model);
        settlementPeriod.setPeriodicity(periodType);
        settlementPeriod.setTimeZone(request.getTimeZone());
        settlementPeriod.setIsRestrictedBasedOnVolume(request.isHasVolumeRestriction());
        settlementPeriod.setIsRestrictedBasedOnValue(request.isHasValueRestriction());
        settlementPeriod.setCreateDate(LocalDateTime.now());
        EditDayOfWeekPeriodOfYear dayOfWeekAndPeriodOfYear = request.getDayOfWeekAndPeriodOfYear();
        if (dayOfWeekAndPeriodOfYear != null) {
            settlementPeriod.setYearRound(dayOfWeekAndPeriodOfYear.getYearRound());
        }
        VolumesBySettlementPeriod save = volumesBySettlementPeriodRepository.save(settlementPeriod);
        saveProfiles(save, request.getProfiles());
        switch (periodType) {
            case RRULE_FORMULA -> save.setRruleFormula(request.getFormula());
            case DAY_OF_WEEK_AND_PERIOD_OF_YEAR -> saveDayOfWeekAndPeriodOfYear(save, request);
            case DAY_OF_MONTH -> saveDayOfMonth(save, request);
        }

        if (request.isHasVolumeRestriction()) {
            save.setVolumeRestrictionPercent(request.getKwhRestrictionPercent());
            saveKwhRestrictions(save, request.getKwhRestriction());
        }
        if (request.isHasValueRestriction()) {
            saveCcyRestrictions(save, request.getCcyRestriction());
        }
        saveSettlementPeriods(save, request.getSettlementPeriods());
    }

    private void saveProfiles(VolumesBySettlementPeriod model, List<ProfilesRequest> requests) {
//        List<SettlementPeriodsProfiles> settlementPeriodsProfiles = requests.stream().map(x -> new SettlementPeriodsProfiles(model, x)).toList();
//        profileRepository.saveAll(settlementPeriodsProfiles);
        List<SettlementPeriodsProfiles> settlementPeriodsProfiles = new ArrayList<>();
        List<String> errorMessages = new ArrayList<>();
        for (ProfilesRequest request : requests) {
            Optional<Profiles> profiles = profilesRepository.findByIdAndStatus(request.getProfileId(), List.of(NomenclatureItemStatus.ACTIVE)).stream().findAny();
            if (profiles.isEmpty()) {
                errorMessages.add("applicationModelRequest.settlementPeriodsRequest.profiles.data-" + request.getProfileId() + "~Profile not found!");
                continue;
            }
            //check if it is hardcoded
            settlementPeriodsProfiles.add(new SettlementPeriodsProfiles(model, request));
        }
        EPBChainedExceptionTriggerUtil.throwExceptionIfRequired(errorMessages, log);
        settlementPeriodsProfileRepository.saveAll(settlementPeriodsProfiles);
    }

    private void saveDayOfMonth(VolumesBySettlementPeriod model, VolumesBySettlementPeriodsRequest request) {
        Set<EditDateOfMonthRequest> dateOfMonths = request.getDateOfMonths();
        List<SettlementPeriodsDateOfMonths> settlementPeriodsDateOfMonths = dateOfMonths.stream().map(req -> new SettlementPeriodsDateOfMonths(model, req)).toList();
        dateOfMonthsRepository.saveAll(settlementPeriodsDateOfMonths);
    }

    private void saveDayOfWeekAndPeriodOfYear(VolumesBySettlementPeriod model, VolumesBySettlementPeriodsRequest requestModel) {
        EditDayOfWeekPeriodOfYear request = requestModel.getDayOfWeekAndPeriodOfYear();
        if (!request.getYearRound()) {
            List<EditPeriodOfYearRequest> issuingPeriodsRequest = request.getPeriodsOfYear();
            List<SettlementPeriodsIssuingPeriods> issuingPeriods = issuingPeriodsRequest.stream().map(x -> new SettlementPeriodsIssuingPeriods(model, x)).toList();
            issuingPeriodsRepository.saveAll(issuingPeriods);
        }
        Set<EditDayOfWeekRequest> daysOfWeek = request.getDaysOfWeek();
        List<SettlementPeriodsDayWeekPeriodYear> periodYears = daysOfWeek.stream().map(x -> new SettlementPeriodsDayWeekPeriodYear(model, x)).toList();
        request.setYearRound(request.getYearRound());
        dayWeekPeriodYearRepository.saveAll(periodYears);
    }

    private void saveSettlementPeriods(VolumesBySettlementPeriod model, List<SettlementPeriodRequest> requests) {
        List<SettlementPeriods> collect = requests.stream().map(x -> new SettlementPeriods(model, x)).toList();
        settlementPeriodRepository.saveAll(collect);

    }

    private void saveKwhRestrictions(VolumesBySettlementPeriod model, List<VolumeRanges> ranges) {
        List<SettlementPeriodsKwhRestrictions> kwhRestrictions = ranges.stream().map(x -> new SettlementPeriodsKwhRestrictions(model, x)).toList();
        kwhRestrictionsRepository.saveAll(kwhRestrictions);
    }

    private void saveCcyRestrictions(VolumesBySettlementPeriod model, List<ValueRanges> ranges) {
        List<SettlementPeriodsCcyRestrictions> ccyRestrictions = new ArrayList<>();
        int index = 0;
        for (ValueRanges valueRange : ranges) {
            boolean currencyExists = currencyRepository.existsByIdAndStatus(valueRange.getCurrency(), NomenclatureItemStatus.ACTIVE);
            if (!currencyExists) {
                throw new DomainEntityNotFoundException(String.format("applicationModelRequest.volumesByScaleRequest.ccyRestriction[%s].currencyId-Currency do not exists;", index));
            }
            ccyRestrictions.add(new SettlementPeriodsCcyRestrictions(model, valueRange));
            index++;
        }
        ccyRestrictionsRepository.saveAll(ccyRestrictions);
    }

    @Override
    public void update(ApplicationModel applicationModel, ApplicationModelRequest modelRequest) {
        VolumesBySettlementPeriodsRequest request = modelRequest.getSettlementPeriodsRequest();
        VolumesBySettlementPeriod volumesBySettlementPeriod = volumesBySettlementPeriodRepository.findByApplicationModelIdAndStatusIn(applicationModel.getId(), List.of(ApplicationModelSubObjectStatus.ACTIVE))
                .orElseThrow(() -> {
                    throw new ClientException("priceComponentId-Application model not found", ErrorCode.DOMAIN_ENTITY_NOT_FOUND);
                });
        Periodicity oldPeriodicity = volumesBySettlementPeriod.getPeriodicity();
        Periodicity newPeriodType = request.getPeriodType();
        checkAndDelete(volumesBySettlementPeriod, request);
        if (!newPeriodType.equals(oldPeriodicity)) {
            switch (oldPeriodicity) {
                case RRULE_FORMULA -> volumesBySettlementPeriod.setRruleFormula(null);
                case DAY_OF_MONTH -> deleteDayOfMonth(volumesBySettlementPeriod);
                case DAY_OF_WEEK_AND_PERIOD_OF_YEAR -> deleteDayOfWeekAndPeriodOfYear(volumesBySettlementPeriod);
            }
        }
        switch (newPeriodType) {
            case RRULE_FORMULA -> volumesBySettlementPeriod.setRruleFormula(request.getFormula());
            case DAY_OF_MONTH -> updateDateOfMonth(volumesBySettlementPeriod, request);
            case DAY_OF_WEEK_AND_PERIOD_OF_YEAR -> updateDayWeekAndPeriodOfYear(volumesBySettlementPeriod, request);
        }
        updateSettlementPeriods(volumesBySettlementPeriod, request);
        updateProfiles(volumesBySettlementPeriod, request);
        updateCcyRestrictions(volumesBySettlementPeriod, request);
        if (request.isHasVolumeRestriction()) {
            updateKwhRestrictions(volumesBySettlementPeriod, request);
        }
        if (request.isHasValueRestriction()) {
            updateCcyRestrictions(volumesBySettlementPeriod, request);
        }

        volumesBySettlementPeriod.setPeriodicity(newPeriodType);
        volumesBySettlementPeriod.setTimeZone(request.getTimeZone());
    }

    private void checkAndDelete(VolumesBySettlementPeriod model, VolumesBySettlementPeriodsRequest request) {
        if (request.isYearRound() && Boolean.FALSE.equals(model.getYearRound())) {
            deleteIssuingPeriods(model);
        }
        if (!request.isHasVolumeRestriction() && model.getIsRestrictedBasedOnVolume()) {
            deleteKwhRestrictions(model);
            model.setIsRestrictedBasedOnVolume(request.isHasVolumeRestriction());
        }
        if (!request.isHasValueRestriction() && model.getIsRestrictedBasedOnValue()) {
            deleteCcyRestrictions(model);
            model.setIsRestrictedBasedOnValue(request.isHasValueRestriction());
        }
    }

    private void updateCcyRestrictions(VolumesBySettlementPeriod model, VolumesBySettlementPeriodsRequest request) {
        model.setIsRestrictedBasedOnValue(request.isHasValueRestriction());
        List<SettlementPeriodsCcyRestrictions> ccyRestrictions = ccyRestrictionsRepository.findAllByVolumesBySettlementPeriodIdAndStatusIn(model.getId(), List.of(ApplicationModelSubObjectStatus.ACTIVE));
        List<ValueRanges> rangesEdits = request.getCcyRestriction();
        boolean areCurrenciesDifferent = areCurrencyIdsDifferent(ccyRestrictions, rangesEdits);
        if (!CollectionUtils.isEmpty(rangesEdits)) {
            Map<Long, ValueRanges> collect = rangesEdits.stream().filter(x -> x.getId() != null).collect(Collectors.toMap(ValueRanges::getId, j -> j));

            int index = 0;
            for (SettlementPeriodsCcyRestrictions ccyRestriction : ccyRestrictions) {
                ValueRanges valueRangesEdit = collect.get(ccyRestriction.getId());
                if (valueRangesEdit == null) {
                    ccyRestriction.setStatus(ApplicationModelSubObjectStatus.DELETED);
                } else {
                    ccyRestriction.setValueFrom(valueRangesEdit.getValueFrom());
                    ccyRestriction.setValueTo(valueRangesEdit.getValueTo());
                    boolean currencyExists;
                    if (!ccyRestriction.getCurrencyId().equals(valueRangesEdit.getCurrency())) {
                        currencyExists = currencyRepository.existsByIdAndStatus(valueRangesEdit.getCurrency(), NomenclatureItemStatus.ACTIVE);
                    } else {
                        currencyExists = currencyRepository.existsById(valueRangesEdit.getCurrency());
                    }
                    if (!currencyExists) {
                        throw new DomainEntityNotFoundException(String.format("applicationModelRequest.volumesByScaleRequest.ccyRestriction[%s].currencyId-Currency do not exists;", index));
                    }
                    ccyRestriction.setCurrencyId(valueRangesEdit.getCurrency());
                }
                index++;
            }
            List<SettlementPeriodsCcyRestrictions> newRestrictions = new ArrayList<>();
            index = 0;
            for (ValueRanges valueRange : rangesEdits) {
                if (valueRange.getId() != null) {
                    continue;
                }
                boolean currencyExists;
                if (areCurrenciesDifferent) {
                    currencyExists = currencyRepository.existsByIdAndStatus(valueRange.getCurrency(), NomenclatureItemStatus.ACTIVE);
                } else {
                    currencyExists = currencyRepository.existsById(valueRange.getCurrency());
                }
                if (!currencyExists) {
                    throw new DomainEntityNotFoundException(String.format("applicationModelRequest.volumesByScaleRequest.ccyRestriction[%s].currencyId-Currency do not exists;", index));
                }
                newRestrictions.add(new SettlementPeriodsCcyRestrictions(model, valueRange));
                index++;
            }
            ccyRestrictions.addAll(newRestrictions);
            ccyRestrictionsRepository.saveAll(ccyRestrictions);
        }

    }

    private void updateKwhRestrictions(VolumesBySettlementPeriod model, VolumesBySettlementPeriodsRequest request) {
        model.setIsRestrictedBasedOnVolume(request.isHasVolumeRestriction());
        List<SettlementPeriodsKwhRestrictions> kwhRestrictions = kwhRestrictionsRepository.findAllByVolumesBySettlementPeriodIdAndStatusIn(model.getId(), List.of(ApplicationModelSubObjectStatus.ACTIVE));
        List<VolumeRanges> rangesEdits = request.getKwhRestriction();
        Map<Long, VolumeRanges> collect = rangesEdits.stream().filter(x -> x.getId() != null).collect(Collectors.toMap(VolumeRanges::getId, j -> j));
        for (SettlementPeriodsKwhRestrictions kwhRestriction : kwhRestrictions) {
            VolumeRanges volumeRangesEdit = collect.get(kwhRestriction.getId());
            if (volumeRangesEdit == null) {
                kwhRestriction.setStatus(ApplicationModelSubObjectStatus.DELETED);
            } else {
                kwhRestriction.setValueFrom(volumeRangesEdit.getValueFrom());
                kwhRestriction.setValueTo(volumeRangesEdit.getValueTo());
            }
        }
        List<SettlementPeriodsKwhRestrictions> newRestrictions = rangesEdits.stream().filter(x -> x.getId() == null)
                .map(x -> new SettlementPeriodsKwhRestrictions(model, x)).toList();
        model.setVolumeRestrictionPercent(request.getKwhRestrictionPercent());
        kwhRestrictions.addAll(newRestrictions);
        kwhRestrictionsRepository.saveAll(kwhRestrictions);
    }

    private void updateProfiles(VolumesBySettlementPeriod model, VolumesBySettlementPeriodsRequest request) {
        List<SettlementPeriodsProfiles> profiles = settlementPeriodsProfileRepository.findAllByVolumesBySettlementPeriodIdAndStatusIn(model.getId(), List.of(ApplicationModelSubObjectStatus.ACTIVE));
        Map<Long, ProfilesRequest> profilesRequestMap = request.getProfiles().stream().collect(Collectors.toMap(ProfilesRequest::getProfileId, j -> j));
        for (SettlementPeriodsProfiles profile : profiles) {
            ProfilesRequest profilesRequest = profilesRequestMap.get(profile.getProfileId());
            if (profilesRequest == null) {
                profile.setStatus(ApplicationModelSubObjectStatus.DELETED);
            } else {
                profile.setPercentage(profilesRequest.getPercentage());
                profilesRequestMap.remove(profile.getProfileId());
            }
        }


        Collection<ProfilesRequest> values = profilesRequestMap.values();
        List<Long> profileIds = new ArrayList<>();
        for (ProfilesRequest value : values) {
            profileIds.add(value.getProfileId());
            profiles.add(new SettlementPeriodsProfiles(model, value));
        }
        List<String> profileErrors = new ArrayList<>();
        List<Long> podsDoNotExists = profilesRepository.findProfileIdsByIdInAndStatus(profileIds, List.of(NomenclatureItemStatus.INACTIVE, NomenclatureItemStatus.DELETED));
        if (CollectionUtils.isNotEmpty(podsDoNotExists)) {
            for (Long podsDoNotExist : podsDoNotExists) {
                profileErrors.add("applicationModelRequest.settlementPeriodsRequest.profiles.data-" + podsDoNotExist + "~Profile not found!");
            }
        }
        EPBChainedExceptionTriggerUtil.throwExceptionIfRequired(profileErrors, log);

        settlementPeriodsProfileRepository.saveAll(profiles);
    }

    private void updateSettlementPeriods(VolumesBySettlementPeriod model, VolumesBySettlementPeriodsRequest request) {
        Map<MinuteRange, SettlementPeriodRequest> settlementPeriodRequestMap = request.getSettlementPeriods().stream().collect(Collectors.toMap(SettlementPeriodRequest::getMinuteRange, j -> j));
        List<SettlementPeriods> settlementPeriods = settlementPeriodRepository.findAllByVolumesBySettlementPeriodIdAndStatusIn(model.getId(), List.of(ApplicationModelSubObjectStatus.ACTIVE));
        for (SettlementPeriods settlementPeriod : settlementPeriods) {
            SettlementPeriodRequest settlementPeriodRequest = settlementPeriodRequestMap.get(settlementPeriod.getMinuteRange());
            if (settlementPeriodRequest == null) {
                throw new ClientException("Settlement not found!", ErrorCode.APPLICATION_ERROR);
            }
            settlementPeriod.setHours(settlementPeriodRequest.getHours());
        }
        settlementPeriodRepository.saveAll(settlementPeriods);

    }

    private void updateDayWeekAndPeriodOfYear(VolumesBySettlementPeriod model, VolumesBySettlementPeriodsRequest baseRequest) {
        EditDayOfWeekPeriodOfYear request = baseRequest.getDayOfWeekAndPeriodOfYear();
        if (!request.getYearRound()) {
            updateIssuingPeriods(model, baseRequest);
        }
        List<SettlementPeriodsDayWeekPeriodYear> dayOfYears = dayWeekPeriodYearRepository.findAllByVolumesBySettlementPeriodIdAndStatusIn(model.getId(), List.of(ApplicationModelSubObjectStatus.ACTIVE));
        Set<EditDayOfWeekRequest> periodOfYearRequests = request.getDaysOfWeek();
        Map<Long, EditDayOfWeekRequest> requestMap = periodOfYearRequests.stream()
                .filter(x -> x.getId() != null)
                .collect(Collectors.toMap(EditDayOfWeekRequest::getId, j -> j));


        for (SettlementPeriodsDayWeekPeriodYear dayOfYear : dayOfYears) {
            EditDayOfWeekRequest editWeekRequest = requestMap.get(dayOfYear.getId());
            if (editWeekRequest == null) {
                dayOfYear.setStatus(ApplicationModelSubObjectStatus.DELETED);
            } else {
                dayOfYear.setWeek(editWeekRequest.getWeek());
                dayOfYear.setDay(editWeekRequest.getDays().stream().toList());
            }
        }
        List<SettlementPeriodsDayWeekPeriodYear> newDateOfMonths = periodOfYearRequests.stream().filter(x -> x.getId() == null)
                .map(x -> new SettlementPeriodsDayWeekPeriodYear(model, x))
                .toList();
        dayOfYears.addAll(newDateOfMonths);
        model.setYearRound(request.getYearRound());
        dayWeekPeriodYearRepository.saveAll(dayOfYears);

    }

    private void updateIssuingPeriods(VolumesBySettlementPeriod model, VolumesBySettlementPeriodsRequest request) {
        List<SettlementPeriodsIssuingPeriods> issuingPeriods = issuingPeriodsRepository.findAllByVolumesBySettlementPeriodIdAndStatusIn(model.getId(), List.of(ApplicationModelSubObjectStatus.ACTIVE));
        List<EditPeriodOfYearRequest> periodsOfYear = request.getDayOfWeekAndPeriodOfYear().getPeriodsOfYear();
        Map<Long, EditPeriodOfYearRequest> collect = periodsOfYear.stream().filter(x -> x.getId() != null).collect(Collectors.toMap(EditPeriodOfYearRequest::getId, j -> j));
        for (SettlementPeriodsIssuingPeriods period : issuingPeriods) {
            EditPeriodOfYearRequest periodEdit = collect.get(period.getId());
            if (periodEdit == null) {
                period.setStatus(ApplicationModelSubObjectStatus.DELETED);
            } else {
                period.setPeriodFrom(periodEdit.getStartDate());
                period.setPeriodTo(periodEdit.getEndDate());
            }
        }
        List<SettlementPeriodsIssuingPeriods> newIssuingPeriods = periodsOfYear.stream().filter(x -> x.getId() == null)
                .map(x -> new SettlementPeriodsIssuingPeriods(model, x)).toList();
        issuingPeriods.addAll(newIssuingPeriods);
        issuingPeriodsRepository.saveAll(issuingPeriods);
    }

    private void updateDateOfMonth(VolumesBySettlementPeriod model, VolumesBySettlementPeriodsRequest request) {
        List<SettlementPeriodsDateOfMonths> dateOfMonths = dateOfMonthsRepository.findAllByVolumesBySettlementPeriodIdAndStatusIn(model.getId(), List.of(ApplicationModelSubObjectStatus.ACTIVE));
        Set<EditDateOfMonthRequest> dateOfMonthRequests = request.getDateOfMonths();
        Map<Long, EditDateOfMonthRequest> requestMap = dateOfMonthRequests
                .stream().filter(x -> x.getId() != null).collect(Collectors.toMap(EditDateOfMonthRequest::getId, j -> j));
        for (SettlementPeriodsDateOfMonths dateOfMonth : dateOfMonths) {
            EditDateOfMonthRequest editDateOfMonthRequest = requestMap.get(dateOfMonth.getId());
            if (editDateOfMonthRequest == null) {
                dateOfMonth.setStatus(ApplicationModelSubObjectStatus.DELETED);
            } else {
                dateOfMonth.setMonth(editDateOfMonthRequest.getMonth());
                dateOfMonth.setMonthNumber(editDateOfMonthRequest.getMonthNumbers().stream().toList());
            }
        }
        List<SettlementPeriodsDateOfMonths> newDateOfMonths = dateOfMonthRequests.stream().filter(x -> x.getId() == null)
                .map(x -> new SettlementPeriodsDateOfMonths(model, x))
                .toList();
        dateOfMonths.addAll(newDateOfMonths);
        dateOfMonthsRepository.saveAll(dateOfMonths);
    }

    @Override
    public void delete(Long modelId) {
        VolumesBySettlementPeriod volumesBySettlementPeriod = volumesBySettlementPeriodRepository.findByApplicationModelIdAndStatusIn(modelId, List.of(ApplicationModelSubObjectStatus.ACTIVE))
                .orElseThrow(() -> {
                    throw new ClientException("id-Application model not found", ErrorCode.DOMAIN_ENTITY_NOT_FOUND);
                });
        Periodicity periodicity = volumesBySettlementPeriod.getPeriodicity();
        volumesBySettlementPeriod.setStatus(ApplicationModelSubObjectStatus.DELETED);
        switch (periodicity) {
            case DAY_OF_MONTH -> deleteDayOfMonth(volumesBySettlementPeriod);
            case DAY_OF_WEEK_AND_PERIOD_OF_YEAR -> deleteDayOfWeekAndPeriodOfYear(volumesBySettlementPeriod);
        }
        if (volumesBySettlementPeriod.getIsRestrictedBasedOnVolume()) {
            deleteKwhRestrictions(volumesBySettlementPeriod);
        }
        if (volumesBySettlementPeriod.getIsRestrictedBasedOnValue()) {
            deleteCcyRestrictions(volumesBySettlementPeriod);
        }
        deleteProfiles(volumesBySettlementPeriod);
        deleteSettlementPeriods(volumesBySettlementPeriod);
        volumesBySettlementPeriod.setStatus(ApplicationModelSubObjectStatus.DELETED);
        volumesBySettlementPeriodRepository.save(volumesBySettlementPeriod);

    }

    private void deleteSettlementPeriods(VolumesBySettlementPeriod model) {
        List<SettlementPeriods> settlementPeriods = settlementPeriodRepository.findAllByVolumesBySettlementPeriodIdAndStatusIn(model.getId(), List.of(ApplicationModelSubObjectStatus.ACTIVE));
        for (SettlementPeriods settlementPeriod : settlementPeriods) {
            settlementPeriod.setStatus(ApplicationModelSubObjectStatus.DELETED);
        }
        settlementPeriodRepository.saveAll(settlementPeriods);

    }

    private void deleteProfiles(VolumesBySettlementPeriod model) {
        List<SettlementPeriodsProfiles> profiles = settlementPeriodsProfileRepository.findAllByVolumesBySettlementPeriodIdAndStatusIn(model.getId(), List.of(ApplicationModelSubObjectStatus.ACTIVE));
        for (SettlementPeriodsProfiles profile : profiles) {
            profile.setStatus(ApplicationModelSubObjectStatus.DELETED);
        }
        settlementPeriodsProfileRepository.saveAll(profiles);

    }

    private void deleteKwhRestrictions(VolumesBySettlementPeriod model) {
        List<SettlementPeriodsKwhRestrictions> kwhRestrictions = kwhRestrictionsRepository.findAllByVolumesBySettlementPeriodIdAndStatusIn(model.getId(), List.of(ApplicationModelSubObjectStatus.ACTIVE));
        for (SettlementPeriodsKwhRestrictions kwhRestriction : kwhRestrictions) {
            kwhRestriction.setStatus(ApplicationModelSubObjectStatus.DELETED);
        }
        kwhRestrictionsRepository.saveAll(kwhRestrictions);
    }

    private void deleteCcyRestrictions(VolumesBySettlementPeriod model) {
        List<SettlementPeriodsCcyRestrictions> ccyRestrictions = ccyRestrictionsRepository.findAllByVolumesBySettlementPeriodIdAndStatusIn(model.getId(), List.of(ApplicationModelSubObjectStatus.ACTIVE));
        for (SettlementPeriodsCcyRestrictions ccyRestriction : ccyRestrictions) {
            ccyRestriction.setStatus(ApplicationModelSubObjectStatus.DELETED);
        }
        ccyRestrictionsRepository.saveAll(ccyRestrictions);
    }

    private void deleteDayOfMonth(VolumesBySettlementPeriod model) {
        List<SettlementPeriodsDateOfMonths> dateOfMonths = dateOfMonthsRepository.findAllByVolumesBySettlementPeriodIdAndStatusIn(model.getId(), List.of(ApplicationModelSubObjectStatus.ACTIVE));
        for (SettlementPeriodsDateOfMonths dateOfMonth : dateOfMonths) {
            dateOfMonth.setStatus(ApplicationModelSubObjectStatus.DELETED);
        }
        dateOfMonthsRepository.saveAll(dateOfMonths);
    }

    private void deleteDayOfWeekAndPeriodOfYear(VolumesBySettlementPeriod model) {
        if (Boolean.FALSE.equals(model.getYearRound())) {
            deleteIssuingPeriods(model);
        }
        List<SettlementPeriodsDayWeekPeriodYear> yearList = dayWeekPeriodYearRepository.findAllByVolumesBySettlementPeriodIdAndStatusIn(model.getId(), List.of(ApplicationModelSubObjectStatus.ACTIVE));
        for (SettlementPeriodsDayWeekPeriodYear periodicallyDayWeekPeriodYear : yearList) {
            periodicallyDayWeekPeriodYear.setStatus(ApplicationModelSubObjectStatus.DELETED);
        }
        dayWeekPeriodYearRepository.saveAll(yearList);
    }

    private void deleteIssuingPeriods(VolumesBySettlementPeriod model) {
        List<SettlementPeriodsIssuingPeriods> issuingPeriods = issuingPeriodsRepository.findAllByVolumesBySettlementPeriodIdAndStatusIn(model.getId(), List.of(ApplicationModelSubObjectStatus.ACTIVE));
        for (SettlementPeriodsIssuingPeriods issuingPeriod : issuingPeriods) {
            issuingPeriod.setStatus(ApplicationModelSubObjectStatus.DELETED);
        }
        issuingPeriodsRepository.saveAll(issuingPeriods);
    }

    @Override
    public ApplicationModelResponse view(ApplicationModel model) {
        VolumesBySettlementPeriodResponse settlementPeriodResponse = new VolumesBySettlementPeriodResponse();
        VolumesBySettlementPeriod volumesBySettlementPeriod = volumesBySettlementPeriodRepository.findByApplicationModelIdAndStatusIn(model.getId(), List.of(ApplicationModelSubObjectStatus.ACTIVE))
                .orElseThrow(() -> new ClientException("id-Application model not found", ErrorCode.DOMAIN_ENTITY_NOT_FOUND));
        Periodicity periodicity = volumesBySettlementPeriod.getPeriodicity();
        switch (periodicity) {
            case RRULE_FORMULA -> {
                settlementPeriodResponse.setPeriodType(periodicity);
                settlementPeriodResponse.setFormula(volumesBySettlementPeriod.getRruleFormula());
            }
            case DAY_OF_MONTH -> createDayOfMonthView(settlementPeriodResponse, volumesBySettlementPeriod);
            case DAY_OF_WEEK_AND_PERIOD_OF_YEAR ->
                    createDayOfWeekView(settlementPeriodResponse, volumesBySettlementPeriod);
        }

        if (Boolean.TRUE.equals(volumesBySettlementPeriod.getIsRestrictedBasedOnVolume())) {
            List<KwhRestrictionResponse> kwhRestrictionResponses = kwhRestrictionsRepository.findAllByVolumesBySettlementPeriodIdAndStatusIn(volumesBySettlementPeriod.getId(), List.of(ApplicationModelSubObjectStatus.ACTIVE))
                    .stream().map(this::mapKwhEntityToResponse).toList();
            settlementPeriodResponse.setKwhRestriction(kwhRestrictionResponses);
            settlementPeriodResponse.setVolumeRestrictionPercent(volumesBySettlementPeriod.getVolumeRestrictionPercent());
            settlementPeriodResponse.setHasVolumeRestriction(volumesBySettlementPeriod.getIsRestrictedBasedOnVolume());
        }
        if (Boolean.TRUE.equals(volumesBySettlementPeriod.getIsRestrictedBasedOnValue())) {
            List<SettlementPeriodsCcyRestrictions> settlementPeriodsCcyRestrictions = ccyRestrictionsRepository.findAllByVolumesBySettlementPeriodIdAndStatusIn(volumesBySettlementPeriod.getId(), List.of(ApplicationModelSubObjectStatus.ACTIVE));
            List<CcyRestrictionResponse> ccyRestrictionResponses = new ArrayList<>();
            for (SettlementPeriodsCcyRestrictions settlementPeriodsCcyRestriction : settlementPeriodsCcyRestrictions) {
                CcyRestrictionResponse ccyRestrictionResponse = mapCcyEntityToResponse(settlementPeriodsCcyRestriction);
                Currency currency = currencyRepository.findByIdAndStatus(settlementPeriodsCcyRestriction.getCurrencyId(), List.of(NomenclatureItemStatus.ACTIVE, NomenclatureItemStatus.INACTIVE))
                        .orElseThrow(() -> new DomainEntityNotFoundException("id-Currency do not exists"));
                ccyRestrictionResponse.setCurrencyResponse(new CurrencyResponse(currency));
                ccyRestrictionResponses.add(ccyRestrictionResponse);
            }
            settlementPeriodResponse.setCcyRestriction(ccyRestrictionResponses);

            settlementPeriodResponse.setHasValueRestriction(volumesBySettlementPeriod.getIsRestrictedBasedOnValue());
        }
        createProfileView(volumesBySettlementPeriod, settlementPeriodResponse);
        createSettlementPeriodView(volumesBySettlementPeriod, settlementPeriodResponse);

        ApplicationModelResponse applicationModelResponse = new ApplicationModelResponse(model.getApplicationModelType(), model.getApplicationType(), model.getApplicationLevel());
        applicationModelResponse.setVolumesBySettlementPeriodResponse(settlementPeriodResponse);

        settlementPeriodResponse.setTimeZone(volumesBySettlementPeriod.getTimeZone());
        return applicationModelResponse;
    }

    private void createSettlementPeriodView(VolumesBySettlementPeriod model, VolumesBySettlementPeriodResponse response) {
        List<SettlementPeriodsResponse> settlementPeriodsResponses = settlementPeriodRepository
                .findAllByVolumesBySettlementPeriodIdAndStatusIn(model.getId(), List.of(ApplicationModelSubObjectStatus.ACTIVE))
                .stream().map(SettlementPeriodsResponse::new).toList();
        response.setSettlementPeriods(settlementPeriodsResponses);
    }

    private void createProfileView(VolumesBySettlementPeriod model, VolumesBySettlementPeriodResponse response) {
        List<ProfileResponse> profileResponses = new ArrayList<>();
        List<SettlementPeriodsProfiles> periodsProfiles = settlementPeriodsProfileRepository
                .findAllByVolumesBySettlementPeriodIdAndStatusIn(model.getId(), List.of(ApplicationModelSubObjectStatus.ACTIVE));
        for (SettlementPeriodsProfiles periodsProfile : periodsProfiles) {
            Profiles profiles = profilesRepository.findByIdAndStatus(periodsProfile.getProfileId(), List.of(NomenclatureItemStatus.ACTIVE, NomenclatureItemStatus.INACTIVE))
                    .orElseThrow(() -> new DomainEntityNotFoundException("id-Profile not found;"));
            profileResponses.add(new ProfileResponse(periodsProfile, profiles));
        }
        response.setProfileResponses(profileResponses);

    }


    private CcyRestrictionResponse mapCcyEntityToResponse(SettlementPeriodsCcyRestrictions ccyRestrictions) {
        CcyRestrictionResponse ccyRestrictionResponse = new CcyRestrictionResponse();
        ccyRestrictionResponse.setId(ccyRestrictions.getId());
        ccyRestrictionResponse.setValueFrom(ccyRestrictions.getValueFrom());
        ccyRestrictionResponse.setValueTo(ccyRestrictions.getValueTo());
        return ccyRestrictionResponse;
    }

    private KwhRestrictionResponse mapKwhEntityToResponse(SettlementPeriodsKwhRestrictions kwhRestrictions) {
        KwhRestrictionResponse response = new KwhRestrictionResponse();
        response.setId(kwhRestrictions.getId());
        response.setValueFrom(kwhRestrictions.getValueFrom());
        response.setValueTo(kwhRestrictions.getValueTo());
        return response;
    }

    private void createDayOfWeekView(VolumesBySettlementPeriodResponse response, VolumesBySettlementPeriod model) {
        ApplicationModelDayWeekPeriodOfYearResponse weekResponse = new ApplicationModelDayWeekPeriodOfYearResponse();

        if (Boolean.FALSE.equals(model.getYearRound())) {
            List<SettlementPeriodsIssuingPeriods> issuingPeriods = issuingPeriodsRepository
                    .findAllByVolumesBySettlementPeriodIdAndStatusIn(model.getId(), List.of(ApplicationModelSubObjectStatus.ACTIVE));
            List<IssuingPeriodsResponse> periodsResponses = issuingPeriods.stream().map(this::mapIssuingPeriodsEntityToResponse).toList();
            weekResponse.setPeriodsOfYear(periodsResponses);
        }
        List<SettlementPeriodsDayWeekPeriodYear> weekDayYearPeriod = dayWeekPeriodYearRepository.findAllByVolumesBySettlementPeriodIdAndStatusIn(model.getId(), List.of(ApplicationModelSubObjectStatus.ACTIVE));
        List<ApplicationModelDayOfWeekResponse> applicationModelDayOfWeekResponses = weekDayYearPeriod.stream().map(this::mapDayOfWeekEntityToResponse).toList();
        weekResponse.setDayOfWeek(applicationModelDayOfWeekResponses);
        response.setPeriodType(model.getPeriodicity());
        weekResponse.setYearRound(model.getYearRound());
        response.setDayWeekPeriodOfYear(weekResponse);
    }


    private void createDayOfMonthView(VolumesBySettlementPeriodResponse response, VolumesBySettlementPeriod model) {
        List<SettlementPeriodsDateOfMonths> dateOfMonths = dateOfMonthsRepository.findAllByVolumesBySettlementPeriodIdAndStatusIn(model.getId(), List.of(ApplicationModelSubObjectStatus.ACTIVE));
        List<ApplicationModelDateOfMonthResponse> responseList = dateOfMonths.stream().map(this::mapDateOfMonthEntityToResponse).toList();
        response.setPeriodType(model.getPeriodicity());
        response.setYearRound(model.getYearRound());
        response.setDateOfMonths(responseList);
    }

    private ApplicationModelDayOfWeekResponse mapDayOfWeekEntityToResponse(SettlementPeriodsDayWeekPeriodYear entity) {
        ApplicationModelDayOfWeekResponse response = new ApplicationModelDayOfWeekResponse();
        response.setId(entity.getId());
        response.setDay(entity.getDay().stream().sorted(Comparator.comparingInt(Enum::ordinal)).toList());
        response.setWeek(entity.getWeek());
        return response;
    }

    private IssuingPeriodsResponse mapIssuingPeriodsEntityToResponse(SettlementPeriodsIssuingPeriods entity) {
        IssuingPeriodsResponse issuingPeriodsResponse = new IssuingPeriodsResponse();
        issuingPeriodsResponse.setId(entity.getId());
        issuingPeriodsResponse.setStartDate(entity.getPeriodFrom());
        issuingPeriodsResponse.setEndDate(entity.getPeriodTo());
        return issuingPeriodsResponse;
    }

    private ApplicationModelDateOfMonthResponse mapDateOfMonthEntityToResponse(SettlementPeriodsDateOfMonths entity) {
        ApplicationModelDateOfMonthResponse response = new ApplicationModelDateOfMonthResponse();
        response.setId(entity.getId());
        response.setMonth(entity.getMonth());
        response.setMonthNumbers(entity.getMonthNumber().stream().sorted(Comparator.comparingInt(Enum::ordinal)).toList());
        return response;
    }

    @Override
    public void clone(ApplicationModel source, ApplicationModel clone) {
        VolumesBySettlementPeriod volumesBySettlementPeriod = volumesBySettlementPeriodRepository
                .findByApplicationModelIdAndStatusIn(source.getId(), List.of(ApplicationModelSubObjectStatus.ACTIVE))
                .orElseThrow(() -> new DomainEntityNotFoundException("Volumes by settlement period not found for application model id: %s;".formatted(source.getId())));

        Periodicity periodicity = volumesBySettlementPeriod.getPeriodicity();

        VolumesBySettlementPeriod clonedVolumesBySettlementPeriod = getClonedVolumesBySettlementPeriod(clone, volumesBySettlementPeriod, periodicity);
        List<SettlementPeriodsProfiles> settlementPeriodsProfiles = settlementPeriodsProfileRepository
                .findAllByVolumesBySettlementPeriodIdAndStatusIn(volumesBySettlementPeriod.getId(), List.of(ApplicationModelSubObjectStatus.ACTIVE));
        cloneProfiles(settlementPeriodsProfiles, clonedVolumesBySettlementPeriod);
        // ccy
        List<SettlementPeriodsCcyRestrictions> ccyRestrictions = ccyRestrictionsRepository
                .findAllByVolumesBySettlementPeriodIdAndStatusIn(volumesBySettlementPeriod.getId(), List.of(ApplicationModelSubObjectStatus.ACTIVE));

        ccyRestrictions.forEach(ccyRestriction -> {
            SettlementPeriodsCcyRestrictions clonedCcyRestriction = new SettlementPeriodsCcyRestrictions();
            clonedCcyRestriction.setValueFrom(ccyRestriction.getValueFrom());
            clonedCcyRestriction.setValueTo(ccyRestriction.getValueTo());
            clonedCcyRestriction.setVolumesBySettlementPeriod(clonedVolumesBySettlementPeriod);
            clonedCcyRestriction.setCurrencyId(ccyRestriction.getCurrencyId());
            clonedCcyRestriction.setStatus(ApplicationModelSubObjectStatus.ACTIVE);
            ccyRestrictionsRepository.save(clonedCcyRestriction);
        });

        volumesBySettlementPeriodRepository.save(clonedVolumesBySettlementPeriod);
    }

    @Override
    public boolean copy(ApplicationModel source, ApplicationModel copied, PriceComponent priceComponent) {
        VolumesBySettlementPeriod volumesBySettlementPeriod = volumesBySettlementPeriodRepository
                .findByApplicationModelIdAndStatusIn(source.getId(), List.of(ApplicationModelSubObjectStatus.ACTIVE))
                .orElseThrow(() -> new DomainEntityNotFoundException("Volumes by settlement period not found for application model id: %s;".formatted(source.getId())));
        List<SettlementPeriodsProfiles> periodsProfiles = settlementPeriodsProfileRepository
                .findAllByVolumesBySettlementPeriodIdAndStatusIn(volumesBySettlementPeriod.getId(), List.of(ApplicationModelSubObjectStatus.ACTIVE));
        for (SettlementPeriodsProfiles periodsProfile : periodsProfiles) {
            if (!profilesRepository.existsByIdAndStatusIn(periodsProfile.getProfileId(), List.of(NomenclatureItemStatus.ACTIVE))) {
                return false;
            }
        }
        // ccy
        List<SettlementPeriodsCcyRestrictions> ccyRestrictions = ccyRestrictionsRepository
                .findAllByVolumesBySettlementPeriodIdAndStatusIn(volumesBySettlementPeriod.getId(), List.of(ApplicationModelSubObjectStatus.ACTIVE));
        for (SettlementPeriodsCcyRestrictions ccyRestriction : ccyRestrictions) {
            boolean currencyExists = currencyRepository.existsByIdAndStatus(ccyRestriction.getCurrencyId(), NomenclatureItemStatus.ACTIVE);
            if (!currencyExists) {
                return false;
            }
        }

        priceComponentRepository.saveAndFlush(priceComponent);
        copied.setPriceComponent(priceComponent);
        applicationModelRepository.saveAndFlush(copied);

        Periodicity periodicity = volumesBySettlementPeriod.getPeriodicity();

        VolumesBySettlementPeriod clonedVolumesBySettlementPeriod = getClonedVolumesBySettlementPeriod(copied, volumesBySettlementPeriod, periodicity);
        cloneProfiles(periodsProfiles, clonedVolumesBySettlementPeriod);
        ccyRestrictions.forEach(ccyRestriction -> {
            SettlementPeriodsCcyRestrictions clonedCcyRestriction = new SettlementPeriodsCcyRestrictions();
            clonedCcyRestriction.setValueFrom(ccyRestriction.getValueFrom());
            clonedCcyRestriction.setValueTo(ccyRestriction.getValueTo());
            clonedCcyRestriction.setVolumesBySettlementPeriod(clonedVolumesBySettlementPeriod);
            clonedCcyRestriction.setCurrencyId(ccyRestriction.getCurrencyId());
            clonedCcyRestriction.setStatus(ApplicationModelSubObjectStatus.ACTIVE);
            ccyRestrictionsRepository.save(clonedCcyRestriction);
        });

        volumesBySettlementPeriodRepository.save(clonedVolumesBySettlementPeriod);
        return true;
    }

    private VolumesBySettlementPeriod getClonedVolumesBySettlementPeriod(ApplicationModel clone, VolumesBySettlementPeriod volumesBySettlementPeriod, Periodicity periodicity) {
        VolumesBySettlementPeriod clonedVolumesBySettlementPeriod = new VolumesBySettlementPeriod();
        clonedVolumesBySettlementPeriod.setPeriodicity(periodicity);
        clonedVolumesBySettlementPeriod.setTimeZone(volumesBySettlementPeriod.getTimeZone());
        clonedVolumesBySettlementPeriod.setYearRound(volumesBySettlementPeriod.getYearRound());
        clonedVolumesBySettlementPeriod.setApplicationModel(clone);
        clonedVolumesBySettlementPeriod.setIsRestrictedBasedOnVolume(volumesBySettlementPeriod.getIsRestrictedBasedOnVolume());
        clonedVolumesBySettlementPeriod.setIsRestrictedBasedOnValue(volumesBySettlementPeriod.getIsRestrictedBasedOnValue());
        clonedVolumesBySettlementPeriod.setStatus(ApplicationModelSubObjectStatus.ACTIVE);
        volumesBySettlementPeriodRepository.saveAndFlush(clonedVolumesBySettlementPeriod);
        clonedVolumesBySettlementPeriod.setYearRound(volumesBySettlementPeriod.getYearRound());
        if (periodicity.equals(Periodicity.RRULE_FORMULA)) {
            volumesBySettlementPeriod.setRruleFormula(volumesBySettlementPeriod.getRruleFormula());
        } else if (periodicity.equals(Periodicity.DAY_OF_WEEK_AND_PERIOD_OF_YEAR)) {
            // clone issuing periods if not year round
            if (Boolean.FALSE.equals(volumesBySettlementPeriod.getYearRound())) {
                List<SettlementPeriodsIssuingPeriods> issuingPeriods = issuingPeriodsRepository
                        .findAllByVolumesBySettlementPeriodIdAndStatusIn(volumesBySettlementPeriod.getId(), List.of(ApplicationModelSubObjectStatus.ACTIVE));

                issuingPeriods.forEach(issuingPeriod -> {
                    SettlementPeriodsIssuingPeriods clonedIssuingPeriod = new SettlementPeriodsIssuingPeriods();
                    clonedIssuingPeriod.setPeriodFrom(issuingPeriod.getPeriodFrom());
                    clonedIssuingPeriod.setPeriodTo(issuingPeriod.getPeriodTo());

                    clonedIssuingPeriod.setVolumesBySettlementPeriod(clonedVolumesBySettlementPeriod);
                    clonedIssuingPeriod.setStatus(ApplicationModelSubObjectStatus.ACTIVE);
                    issuingPeriodsRepository.save(clonedIssuingPeriod);
                });
            }

            // clone day week period year
            List<SettlementPeriodsDayWeekPeriodYear> weekDayYearPeriod = dayWeekPeriodYearRepository
                    .findAllByVolumesBySettlementPeriodIdAndStatusIn(volumesBySettlementPeriod.getId(), List.of(ApplicationModelSubObjectStatus.ACTIVE));

            weekDayYearPeriod.forEach(dayWeekPeriodYear -> {
                SettlementPeriodsDayWeekPeriodYear clonedDayWeekPeriodYear = new SettlementPeriodsDayWeekPeriodYear();
                clonedDayWeekPeriodYear.setDay(dayWeekPeriodYear.getDay());
                clonedDayWeekPeriodYear.setWeek(dayWeekPeriodYear.getWeek());
                clonedDayWeekPeriodYear.setVolumesBySettlementPeriod(clonedVolumesBySettlementPeriod);
                clonedDayWeekPeriodYear.setStatus(ApplicationModelSubObjectStatus.ACTIVE);
                dayWeekPeriodYearRepository.save(clonedDayWeekPeriodYear);
            });
        } else if (periodicity.equals(Periodicity.DAY_OF_MONTH)) {
            List<SettlementPeriodsDateOfMonths> dateOfMonths = dateOfMonthsRepository
                    .findAllByVolumesBySettlementPeriodIdAndStatusIn(volumesBySettlementPeriod.getId(), List.of(ApplicationModelSubObjectStatus.ACTIVE));

            dateOfMonths.forEach(dateOfMonth -> {
                SettlementPeriodsDateOfMonths clonedDateOfMonth = new SettlementPeriodsDateOfMonths();
                clonedDateOfMonth.setMonth(dateOfMonth.getMonth());
                clonedDateOfMonth.setMonthNumber(dateOfMonth.getMonthNumber());
                clonedDateOfMonth.setVolumesBySettlementPeriod(clonedVolumesBySettlementPeriod);
                clonedDateOfMonth.setStatus(ApplicationModelSubObjectStatus.ACTIVE);
                dateOfMonthsRepository.save(clonedDateOfMonth);
            });
        }

        // clone kwh restrictions if volume restriction percent is not null
        if (volumesBySettlementPeriod.getVolumeRestrictionPercent() != null) {
            clonedVolumesBySettlementPeriod.setVolumeRestrictionPercent(volumesBySettlementPeriod.getVolumeRestrictionPercent());
            List<SettlementPeriodsKwhRestrictions> kwhRestrictions = kwhRestrictionsRepository
                    .findAllByVolumesBySettlementPeriodIdAndStatusIn(volumesBySettlementPeriod.getId(), List.of(ApplicationModelSubObjectStatus.ACTIVE));

            kwhRestrictions.forEach(kwhRestriction -> {
                SettlementPeriodsKwhRestrictions clonedKwhRestriction = new SettlementPeriodsKwhRestrictions();
                clonedKwhRestriction.setValueFrom(kwhRestriction.getValueFrom());
                clonedKwhRestriction.setValueTo(kwhRestriction.getValueTo());
                clonedKwhRestriction.setVolumesBySettlementPeriod(clonedVolumesBySettlementPeriod);
                clonedKwhRestriction.setStatus(ApplicationModelSubObjectStatus.ACTIVE);
                kwhRestrictionsRepository.save(clonedKwhRestriction);
            });
        }

        List<SettlementPeriods> settlementPeriods = settlementPeriodRepository
                .findAllByVolumesBySettlementPeriodIdAndStatusIn(volumesBySettlementPeriod.getId(), List.of(ApplicationModelSubObjectStatus.ACTIVE));
        List<SettlementPeriods> clonedSettlementPeriods = new ArrayList<>();
        for (SettlementPeriods settlementPeriod : settlementPeriods) {
            SettlementPeriods periods = new SettlementPeriods();
            periods.setHours(settlementPeriod.getHours());
            periods.setMinuteRange(settlementPeriod.getMinuteRange());
            periods.setStatus(ApplicationModelSubObjectStatus.ACTIVE);
            periods.setVolumesBySettlementPeriod(clonedVolumesBySettlementPeriod);
            clonedSettlementPeriods.add(periods);
        }
        settlementPeriodRepository.saveAll(clonedSettlementPeriods);
        return clonedVolumesBySettlementPeriod;
    }


    void cloneProfiles(List<SettlementPeriodsProfiles> settlementPeriodsProfiles, VolumesBySettlementPeriod clone) {
        settlementPeriodsProfiles
                .forEach(
                        sourcePeriodProfile -> {
                            SettlementPeriodsProfiles clonedPeriodsProfile = new SettlementPeriodsProfiles();
                            clonedPeriodsProfile.setPercentage(sourcePeriodProfile.getPercentage());
                            clonedPeriodsProfile.setVolumesBySettlementPeriod(clone);
                            clonedPeriodsProfile.setProfileId(sourcePeriodProfile.getProfileId());
                            clonedPeriodsProfile.setStatus(ApplicationModelSubObjectStatus.ACTIVE);
                            settlementPeriodsProfileRepository.save(clonedPeriodsProfile);
                        }
                );
    }

    private boolean areCurrencyIdsDifferent(List<SettlementPeriodsCcyRestrictions> list1, List<ValueRanges> list2) {
        if(!CollectionUtils.isEmpty(list1) && !CollectionUtils.isEmpty(list2)){
            if (list1.size() != list2.size()) {
                return false;
            }

            boolean foundDifference = false;
            for (int i = 0; i < list1.size(); i++) {
                SettlementPeriodsCcyRestrictions item1 = list1.get(i);
                ValueRanges item2 = list2.get(i);
                if (!item1.getCurrencyId().equals(item2.getCurrency())) {
                    foundDifference = true;
                    break;
                }
            }
            return foundDifference;
        }
       return false;
    }

}
