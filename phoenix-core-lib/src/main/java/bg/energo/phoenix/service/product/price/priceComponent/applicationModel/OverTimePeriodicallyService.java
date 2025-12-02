package bg.energo.phoenix.service.product.price.priceComponent.applicationModel;

import bg.energo.phoenix.exception.ClientException;
import bg.energo.phoenix.exception.DomainEntityNotFoundException;
import bg.energo.phoenix.exception.ErrorCode;
import bg.energo.phoenix.model.entity.product.price.priceComponent.PriceComponent;
import bg.energo.phoenix.model.entity.product.price.priceComponent.applicationModel.*;
import bg.energo.phoenix.model.enums.product.price.priceComponent.applicationModel.*;
import bg.energo.phoenix.model.request.product.iap.interimAdvancePayment.periodical.edit.EditDateOfMonthRequest;
import bg.energo.phoenix.model.request.product.iap.interimAdvancePayment.periodical.edit.EditDayOfWeekPeriodOfYear;
import bg.energo.phoenix.model.request.product.iap.interimAdvancePayment.periodical.edit.EditDayOfWeekRequest;
import bg.energo.phoenix.model.request.product.iap.interimAdvancePayment.periodical.edit.EditPeriodOfYearRequest;
import bg.energo.phoenix.model.request.product.price.aplicationModel.ApplicationModelRequest;
import bg.energo.phoenix.model.request.product.price.aplicationModel.OverTimePeriodicallyRequest;
import bg.energo.phoenix.model.response.priceComponent.applicationModel.*;
import bg.energo.phoenix.repository.product.price.applicationModel.*;
import bg.energo.phoenix.repository.product.price.priceComponent.PriceComponentRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OverTimePeriodicallyService implements ApplicationModelBaseService {

    private final PeriodicallyIssuingPeriodsRepository issuingPeriodsRepository;
    private final PeriodicallyDayWeekPeriodYearRepository dayWeekPeriodYearRepository;
    private final PeriodicallyDateOfMonthsRepository dateOfMonthsRepository;
    private final OverTimePeriodicallyRepository periodicallyRepository;
    private final ApplicationModelRepository applicationModelRepository;
    private final PriceComponentRepository priceComponentRepository;

    @Override
    public void create(ApplicationModel model, ApplicationModelRequest modelRequest) {
        OverTimePeriodicallyRequest request = modelRequest.getOverTimePeriodicallyRequest();
        Periodicity periodType = request.getPeriodType();
        OverTimePeriodically overTimePeriodically = new OverTimePeriodically();
        overTimePeriodically.setStatus(OverTimePeriodicallyStatus.ACTIVE);
        overTimePeriodically.setApplicationModel(model);
        overTimePeriodically.setCreateDate(LocalDateTime.now());
        overTimePeriodically.setPeriodicity(periodType);
        OverTimePeriodically save = periodicallyRepository.save(overTimePeriodically);
        switch (periodType) {
            case RRULE_FORMULA -> save.setRruleFormula(request.getFormula());
            case DAY_OF_MONTH -> saveDayOfMonth(save, request);
            case DAY_OF_WEEK_AND_PERIOD_OF_YEAR -> saveDayOfWeekAndPeriodOfYear(save, request);
        }
        periodicallyRepository.save(save);

    }

    private void saveDayOfWeekAndPeriodOfYear(OverTimePeriodically model, OverTimePeriodicallyRequest requestModel) {
        EditDayOfWeekPeriodOfYear request = requestModel.getDayOfWeekAndPeriodOfYear();
        model.setYearRound(request.getYearRound());
        if (!request.getYearRound()) {
            List<EditPeriodOfYearRequest> issuingPeriodsRequest = request.getPeriodsOfYear();
            List<PeriodicallyIssuingPeriods> issuingPeriods = issuingPeriodsRequest.stream().map(x -> new PeriodicallyIssuingPeriods(model, x)).toList();
            issuingPeriodsRepository.saveAll(issuingPeriods);
        }
        Set<EditDayOfWeekRequest> daysOfWeek = request.getDaysOfWeek();
        List<PeriodicallyDayWeekPeriodYear> periodYears = daysOfWeek.stream().map(x -> new PeriodicallyDayWeekPeriodYear(model, x)).toList();
        dayWeekPeriodYearRepository.saveAll(periodYears);
    }

    private void saveDayOfMonth(OverTimePeriodically model, OverTimePeriodicallyRequest request) {
        Set<EditDateOfMonthRequest> dateOfMonths = request.getDateOfMonths();
        List<PeriodicallyDateOfMonths> periodicallyDateOfMonths = dateOfMonths.stream().map(req -> new PeriodicallyDateOfMonths(model, req)).toList();
        dateOfMonthsRepository.saveAll(periodicallyDateOfMonths);
    }

    @Override
    public void update(ApplicationModel applicationModel, ApplicationModelRequest modelRequest) {
        OverTimePeriodicallyRequest request = modelRequest.getOverTimePeriodicallyRequest();
        OverTimePeriodically overTimePeriodically = periodicallyRepository.findByApplicationModelIdAndStatusIn(applicationModel.getId(), List.of(OverTimePeriodicallyStatus.ACTIVE))
                .orElseThrow(() -> new ClientException("id-Application model not found", ErrorCode.DOMAIN_ENTITY_NOT_FOUND));
        Periodicity oldPeriodicity = overTimePeriodically.getPeriodicity();
        Periodicity newPeriodType = request.getPeriodType();
        if (!newPeriodType.equals(oldPeriodicity)) {
            switch (oldPeriodicity) {
                case RRULE_FORMULA -> overTimePeriodically.setRruleFormula(null);
                case DAY_OF_MONTH -> deleteDayOfMonth(overTimePeriodically);
                case DAY_OF_WEEK_AND_PERIOD_OF_YEAR -> deleteDayOfWeekAndPeriodOfYear(overTimePeriodically);
            }
        }
        switch (newPeriodType) {
            case RRULE_FORMULA -> overTimePeriodically.setRruleFormula(request.getFormula());
            case DAY_OF_MONTH -> updateDateOfMonth(overTimePeriodically, request);
            case DAY_OF_WEEK_AND_PERIOD_OF_YEAR -> updateDayWeekAndPeriodOfYear(overTimePeriodically, request);
        }
        overTimePeriodically.setPeriodicity(newPeriodType);
        periodicallyRepository.save(overTimePeriodically);
    }

    private void updateDayWeekAndPeriodOfYear(OverTimePeriodically model, OverTimePeriodicallyRequest baseRequest) {
        EditDayOfWeekPeriodOfYear request = baseRequest.getDayOfWeekAndPeriodOfYear();
        if (!request.getYearRound()) {
            updateIssuingPeriods(model, baseRequest);
        }
        List<PeriodicallyDayWeekPeriodYear> dayOfYears = dayWeekPeriodYearRepository.findAllByOverTimePeriodicallyIdAndStatusIn(model.getId(), List.of(OverTimePeriodicallyDayWeekYearStatus.ACTIVE));
        Set<EditDayOfWeekRequest> periodOfYearRequests = request.getDaysOfWeek();
        Map<Long, EditDayOfWeekRequest> requestMap = periodOfYearRequests.stream()
                .filter(x -> x.getId() != null)
                .collect(Collectors.toMap(EditDayOfWeekRequest::getId, j -> j));


        for (PeriodicallyDayWeekPeriodYear dayOfYear : dayOfYears) {
            EditDayOfWeekRequest editWeekRequest = requestMap.get(dayOfYear.getId());
            if (editWeekRequest == null) {
                dayOfYear.setStatus(OverTimePeriodicallyDayWeekYearStatus.DELETED);
            } else {
                dayOfYear.setWeek(editWeekRequest.getWeek());
                dayOfYear.setDay(editWeekRequest.getDays().stream().toList());
            }
        }
        List<PeriodicallyDayWeekPeriodYear> newDateOfMonths = periodOfYearRequests.stream().filter(x -> x.getId() == null)
                .map(x -> new PeriodicallyDayWeekPeriodYear(model, x))
                .toList();
        dayOfYears.addAll(newDateOfMonths);
        dayWeekPeriodYearRepository.saveAll(dayOfYears);
        model.setYearRound(request.getYearRound());
    }

    private void updateIssuingPeriods(OverTimePeriodically model, OverTimePeriodicallyRequest request) {
        List<PeriodicallyIssuingPeriods> issuingPeriods = issuingPeriodsRepository.findAllByOverTimePeriodicallyIdAndStatusIn(model.getId(), List.of(PeriodicallyIssuingPeriodsStatus.ACTIVE));
        List<EditPeriodOfYearRequest> periodsOfYear = request.getDayOfWeekAndPeriodOfYear().getPeriodsOfYear();
        Map<Long, EditPeriodOfYearRequest> collect = periodsOfYear.stream().filter(x -> x.getId() != null).collect(Collectors.toMap(EditPeriodOfYearRequest::getId, j -> j));
        for (PeriodicallyIssuingPeriods period : issuingPeriods) {
            EditPeriodOfYearRequest periodEdit = collect.get(period.getId());
            if (periodEdit == null) {
                period.setStatus(PeriodicallyIssuingPeriodsStatus.DELETED);
            } else {
                period.setPeriodFrom(periodEdit.getStartDate());
                period.setPeriodTo(periodEdit.getEndDate());
            }
        }
        List<PeriodicallyIssuingPeriods> newIssuingPeriods = periodsOfYear.stream().filter(x -> x.getId() == null)
                .map(x -> new PeriodicallyIssuingPeriods(model, x)).toList();
        issuingPeriods.addAll(newIssuingPeriods);
        issuingPeriodsRepository.saveAll(issuingPeriods);
    }

    private void updateDateOfMonth(OverTimePeriodically model, OverTimePeriodicallyRequest request) {
        List<PeriodicallyDateOfMonths> dateOfMonths = dateOfMonthsRepository.findAllByOverTimePeriodicallyIdAndStatusIn(model.getId(), List.of(PeriodicallyDateOfMonthsStatus.ACTIVE));
        Set<@Valid EditDateOfMonthRequest> dateOfMonthRequests = request.getDateOfMonths();
        Map<Long, EditDateOfMonthRequest> requestMap = dateOfMonthRequests
                .stream().filter(x -> x.getId() != null).collect(Collectors.toMap(EditDateOfMonthRequest::getId, j -> j));
        for (PeriodicallyDateOfMonths dateOfMonth : dateOfMonths) {
            EditDateOfMonthRequest editDateOfMonthRequest = requestMap.get(dateOfMonth.getId());
            if (editDateOfMonthRequest == null) {
                dateOfMonth.setStatus(PeriodicallyDateOfMonthsStatus.DELETED);
            } else {
                dateOfMonth.setMonth(editDateOfMonthRequest.getMonth());
                dateOfMonth.setMonthNumber(editDateOfMonthRequest.getMonthNumbers().stream().toList());
            }
        }
        List<PeriodicallyDateOfMonths> newDateOfMonths = dateOfMonthRequests.stream().filter(x -> x.getId() == null)
                .map(x -> new PeriodicallyDateOfMonths(model, x))
                .toList();
        dateOfMonths.addAll(newDateOfMonths);
        dateOfMonthsRepository.saveAll(dateOfMonths);
    }

    @Override
    public void delete(Long modelId) {
        OverTimePeriodically overTimePeriodically = periodicallyRepository.findByApplicationModelIdAndStatusIn(modelId, List.of(OverTimePeriodicallyStatus.ACTIVE))
                .orElseThrow(() -> new ClientException("id-Application model not found", ErrorCode.DOMAIN_ENTITY_NOT_FOUND));
        Periodicity periodicity = overTimePeriodically.getPeriodicity();
        switch (periodicity) {
            case DAY_OF_MONTH -> deleteDayOfMonth(overTimePeriodically);
            case DAY_OF_WEEK_AND_PERIOD_OF_YEAR -> deleteDayOfWeekAndPeriodOfYear(overTimePeriodically);
        }
        overTimePeriodically.setStatus(OverTimePeriodicallyStatus.DELETED);
        periodicallyRepository.save(overTimePeriodically);
    }

    private void deleteDayOfWeekAndPeriodOfYear(OverTimePeriodically overTimePeriodically) {
        if (overTimePeriodically.getYearRound() == null || !overTimePeriodically.getYearRound()) {
            List<PeriodicallyIssuingPeriods> issuingPeriods = issuingPeriodsRepository.findAllByOverTimePeriodicallyIdAndStatusIn(overTimePeriodically.getId(), List.of(PeriodicallyIssuingPeriodsStatus.ACTIVE));
            for (PeriodicallyIssuingPeriods issuingPeriod : issuingPeriods) {
                issuingPeriod.setStatus(PeriodicallyIssuingPeriodsStatus.DELETED);

            }
            issuingPeriodsRepository.saveAll(issuingPeriods);
        }
        List<PeriodicallyDayWeekPeriodYear> yearList = dayWeekPeriodYearRepository.findAllByOverTimePeriodicallyIdAndStatusIn(overTimePeriodically.getId(), List.of(OverTimePeriodicallyDayWeekYearStatus.ACTIVE));
        for (PeriodicallyDayWeekPeriodYear periodicallyDayWeekPeriodYear : yearList) {
            periodicallyDayWeekPeriodYear.setStatus(OverTimePeriodicallyDayWeekYearStatus.DELETED);
        }
        dayWeekPeriodYearRepository.saveAll(yearList);
    }

    private void deleteDayOfMonth(OverTimePeriodically overTimePeriodically) {
        List<PeriodicallyDateOfMonths> dateOfMonths = dateOfMonthsRepository.findAllByOverTimePeriodicallyIdAndStatusIn(overTimePeriodically.getId(), List.of(PeriodicallyDateOfMonthsStatus.ACTIVE));
        for (PeriodicallyDateOfMonths dateOfMonth : dateOfMonths) {
            dateOfMonth.setStatus(PeriodicallyDateOfMonthsStatus.DELETED);
        }
        dateOfMonthsRepository.saveAll(dateOfMonths);
    }

    @Override
    public ApplicationModelResponse view(ApplicationModel model) {
        OverTimePeriodically overTimePeriodically = periodicallyRepository.findByApplicationModelIdAndStatusIn(model.getId(), List.of(OverTimePeriodicallyStatus.ACTIVE))
                .orElseThrow(() -> new ClientException("id-Application model not found", ErrorCode.DOMAIN_ENTITY_NOT_FOUND));
        Periodicity periodicity = overTimePeriodically.getPeriodicity();
        OverTimePeriodicallyResponse periodicallyResponse = new OverTimePeriodicallyResponse();
        switch (periodicity) {
            case RRULE_FORMULA -> {
                periodicallyResponse.setPeriodType(periodicity);
                periodicallyResponse.setFormula(overTimePeriodically.getRruleFormula());
            }
            case DAY_OF_MONTH -> createDayOfMonthView(periodicallyResponse, overTimePeriodically);
            case DAY_OF_WEEK_AND_PERIOD_OF_YEAR -> createDayOfWeekView(periodicallyResponse, overTimePeriodically);
        }
        ApplicationModelResponse applicationModelResponse = new ApplicationModelResponse(model.getApplicationModelType(), model.getApplicationType(), model.getApplicationLevel());
        applicationModelResponse.setOverTimePeriodicallyResponse(periodicallyResponse);
        return applicationModelResponse;
    }

    private void createDayOfWeekView(OverTimePeriodicallyResponse response, OverTimePeriodically model) {
        ApplicationModelDayWeekPeriodOfYearResponse weekResponse = new ApplicationModelDayWeekPeriodOfYearResponse();

        if (Boolean.FALSE.equals(model.getYearRound())) {
            List<PeriodicallyIssuingPeriods> issuingPeriods = issuingPeriodsRepository
                    .findAllByOverTimePeriodicallyIdAndStatusIn(model.getId(), List.of(PeriodicallyIssuingPeriodsStatus.ACTIVE));
            List<IssuingPeriodsResponse> periodsResponses = issuingPeriods.stream().map(IssuingPeriodsResponse::new).toList();
            weekResponse.setPeriodsOfYear(periodsResponses);
        }
        List<PeriodicallyDayWeekPeriodYear> weekDayYearPeriod = dayWeekPeriodYearRepository.findAllByOverTimePeriodicallyIdAndStatusIn(model.getId(), List.of(OverTimePeriodicallyDayWeekYearStatus.ACTIVE));
        List<ApplicationModelDayOfWeekResponse> applicationModelDayOfWeekResponses = weekDayYearPeriod.stream().map(ApplicationModelDayOfWeekResponse::new).toList();
        weekResponse.setDayOfWeek(applicationModelDayOfWeekResponses);
        response.setPeriodType(model.getPeriodicity());
        weekResponse.setYearRound(model.getYearRound());
        response.setDayWeekPeriodOfYear(weekResponse);
    }

    private void createDayOfMonthView(OverTimePeriodicallyResponse response, OverTimePeriodically model) {
        List<PeriodicallyDateOfMonths> dateOfMonths = dateOfMonthsRepository.findAllByOverTimePeriodicallyIdAndStatusIn(model.getId(), List.of(PeriodicallyDateOfMonthsStatus.ACTIVE));
        List<ApplicationModelDateOfMonthResponse> responseList = dateOfMonths.stream().map(ApplicationModelDateOfMonthResponse::new).toList();
        response.setPeriodType(model.getPeriodicity());
        response.setDateOfMonths(responseList);
    }

    @Override
    public void clone(ApplicationModel source, ApplicationModel clone) {
        OverTimePeriodically overTimePeriodically = periodicallyRepository
                .findByApplicationModelIdAndStatusIn(source.getId(), List.of(OverTimePeriodicallyStatus.ACTIVE))
                .orElseThrow(() -> new DomainEntityNotFoundException("id-OverTimePeriodically application model not found while cloning application model ID %s;".formatted(source.getId())));

        OverTimePeriodically clonedEntity = cloneOverTimePeriodically(clone, overTimePeriodically);

        periodicallyRepository.save(clonedEntity);
    }


    @Override
    public boolean copy(ApplicationModel source, ApplicationModel copied, PriceComponent priceComponent) {
        priceComponentRepository.saveAndFlush(priceComponent);
        copied.setPriceComponent(priceComponent);
        applicationModelRepository.saveAndFlush(copied);
        OverTimePeriodically overTimePeriodically = periodicallyRepository
                .findByApplicationModelIdAndStatusIn(source.getId(), List.of(OverTimePeriodicallyStatus.ACTIVE))
                .orElseThrow(() -> new DomainEntityNotFoundException("id-OverTimePeriodically application model not found while copying application model ID %s;".formatted(source.getId())));

        OverTimePeriodically clonedEntity = cloneOverTimePeriodically(copied, overTimePeriodically);

        periodicallyRepository.save(clonedEntity);
        return true;
    }

    private OverTimePeriodically cloneOverTimePeriodically(ApplicationModel clone, OverTimePeriodically overTimePeriodically) {
        Periodicity periodicity = overTimePeriodically.getPeriodicity();

        OverTimePeriodically clonedEntity = new OverTimePeriodically();
        clonedEntity.setApplicationModel(clone);
        clonedEntity.setPeriodicity(periodicity);
        clonedEntity.setStatus(OverTimePeriodicallyStatus.ACTIVE);
        periodicallyRepository.saveAndFlush(clonedEntity);

        if (periodicity.equals(Periodicity.RRULE_FORMULA)) {
            clonedEntity.setRruleFormula(overTimePeriodically.getRruleFormula());
        } else if (periodicity.equals(Periodicity.DAY_OF_MONTH)) {
            List<PeriodicallyDateOfMonths> dateOfMonthsList = dateOfMonthsRepository.findAllByOverTimePeriodicallyIdAndStatusIn(
                    overTimePeriodically.getId(),
                    List.of(PeriodicallyDateOfMonthsStatus.ACTIVE)
            );

            for (PeriodicallyDateOfMonths dateOfMonths : dateOfMonthsList) {
                PeriodicallyDateOfMonths clonedDateOfMonths = new PeriodicallyDateOfMonths();
                clonedDateOfMonths.setOverTimePeriodically(clonedEntity);
                clonedDateOfMonths.setStatus(PeriodicallyDateOfMonthsStatus.ACTIVE);
                clonedDateOfMonths.setMonth(dateOfMonths.getMonth());
                clonedDateOfMonths.setMonthNumber(dateOfMonths.getMonthNumber());
                dateOfMonthsRepository.save(clonedDateOfMonths);
            }
        } else if (periodicity.equals(Periodicity.DAY_OF_WEEK_AND_PERIOD_OF_YEAR)) {
            clonedEntity.setYearRound(overTimePeriodically.getYearRound());

            // if year round is false, clone issuing periods
            if (Boolean.FALSE.equals(overTimePeriodically.getYearRound())) {
                List<PeriodicallyIssuingPeriods> issuingPeriods = issuingPeriodsRepository.findAllByOverTimePeriodicallyIdAndStatusIn(
                        overTimePeriodically.getId(),
                        List.of(PeriodicallyIssuingPeriodsStatus.ACTIVE)
                );

                for (PeriodicallyIssuingPeriods periods : issuingPeriods) {
                    PeriodicallyIssuingPeriods clonedPeriodicallyIssuingPeriods = new PeriodicallyIssuingPeriods();
                    clonedPeriodicallyIssuingPeriods.setOverTimePeriodically(clonedEntity);
                    clonedPeriodicallyIssuingPeriods.setStatus(PeriodicallyIssuingPeriodsStatus.ACTIVE);
                    clonedPeriodicallyIssuingPeriods.setPeriodFrom(periods.getPeriodFrom());
                    clonedPeriodicallyIssuingPeriods.setPeriodTo(periods.getPeriodTo());
                    issuingPeriodsRepository.save(clonedPeriodicallyIssuingPeriods);
                }
            }

            // clone day week period year
            List<PeriodicallyDayWeekPeriodYear> dayWeekPeriodYears = dayWeekPeriodYearRepository.findAllByOverTimePeriodicallyIdAndStatusIn(
                    overTimePeriodically.getId(),
                    List.of(OverTimePeriodicallyDayWeekYearStatus.ACTIVE)
            );

            for (PeriodicallyDayWeekPeriodYear dayWeekPeriodYear : dayWeekPeriodYears) {
                PeriodicallyDayWeekPeriodYear clonedDayWeekPeriodYear = new PeriodicallyDayWeekPeriodYear();
                clonedDayWeekPeriodYear.setOverTimePeriodically(clonedEntity);
                clonedDayWeekPeriodYear.setStatus(OverTimePeriodicallyDayWeekYearStatus.ACTIVE);
                clonedDayWeekPeriodYear.setWeek(dayWeekPeriodYear.getWeek());
                clonedDayWeekPeriodYear.setDay(dayWeekPeriodYear.getDay());
                dayWeekPeriodYearRepository.save(clonedDayWeekPeriodYear);
            }
        }
        return clonedEntity;
    }

}
