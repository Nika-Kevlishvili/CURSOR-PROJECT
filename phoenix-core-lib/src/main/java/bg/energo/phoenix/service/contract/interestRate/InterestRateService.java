package bg.energo.phoenix.service.contract.interestRate;

import bg.energo.phoenix.exception.ClientException;
import bg.energo.phoenix.exception.DomainEntityNotFoundException;
import bg.energo.phoenix.exception.ErrorCode;
import bg.energo.phoenix.exception.OperationNotAllowedException;
import bg.energo.phoenix.model.entity.contract.InterestRate.InterestRate;
import bg.energo.phoenix.model.entity.contract.InterestRate.InterestRatePaymentTerms;
import bg.energo.phoenix.model.entity.contract.InterestRate.InterestRatePeriods;
import bg.energo.phoenix.model.entity.nomenclature.product.Currency;
import bg.energo.phoenix.model.entity.nomenclature.product.terms.Calendar;
import bg.energo.phoenix.model.enums.contract.InterestRate.*;
import bg.energo.phoenix.model.enums.nomenclature.NomenclatureItemStatus;
import bg.energo.phoenix.model.request.contract.interestRate.*;
import bg.energo.phoenix.model.response.contract.InterestRate.*;
import bg.energo.phoenix.permissions.PermissionContextEnum;
import bg.energo.phoenix.repository.interestRate.InterestRatePaymentTermsRepository;
import bg.energo.phoenix.repository.interestRate.InterestRatePeriodsRepository;
import bg.energo.phoenix.repository.interestRate.InterestRateRepository;
import bg.energo.phoenix.repository.nomenclature.product.CurrencyRepository;
import bg.energo.phoenix.repository.nomenclature.product.terms.CalendarRepository;
import bg.energo.phoenix.security.PermissionService;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import static bg.energo.phoenix.permissions.PermissionEnum.INTEREST_RATES_VIEW_BASIC;
import static bg.energo.phoenix.permissions.PermissionEnum.INTEREST_RATES_VIEW_DELETED;

@Service
@RequiredArgsConstructor
@Slf4j
public class InterestRateService {
    private final InterestRateRepository interestRateRepository;
    private final InterestRatePaymentTermsRepository interestRatePaymentTermsRepository;
    private final InterestRatePeriodsRepository interestRatePeriodsRepository;
    private final CurrencyRepository currencyRepository;
    private final CalendarRepository calendarRepository;
    private final PermissionService permissionService;

    @Transactional
    public Long create(InterestRateCreateRequest request) {
        log.debug("Creating Interest Rate with object: {}", request.toString());
        List<String> errorMessages = new ArrayList<>();
        Optional<InterestRate> defaultInterestRate = interestRateRepository.findByIsDefaultAndStatus(true, InterestRateStatus.ACTIVE);
        if (defaultInterestRate.isPresent() && request.getIsDefault()) {
            errorMessages.add("isDefault-[IsDefault] can't create interest rate, because there is default interest rate with id:%s;".formatted(defaultInterestRate.get().getId()));
            /*throw new ClientException("isDefault-[IsDefault] can't create interest rate, because there is default interest rate with id:%s;"
                    .formatted(defaultInterestRate.get().getId()), ErrorCode.CONFLICT);*/
        }
        Optional<Currency> currency = currencyRepository.findByIdAndStatus(request.getCurrencyId(), List.of(NomenclatureItemStatus.ACTIVE));
        //orElseThrow(() -> new DomainEntityNotFoundException("currencyId-[CurrencyId] can't find active currency with id:%s;".formatted(request.getCurrencyId())));
        if (currency.isEmpty()) {
            errorMessages.add("currencyId-[CurrencyId] can't find active currency with id:%s;".formatted(request.getCurrencyId()));
        }
        checkNameForDublicateNames(request.getName(), errorMessages);
        InterestRate interestRate = mapAndSaveInterestRateFromRequest(request);
        createInterestRateTerm(interestRate, request.getPaymentTerm(), errorMessages);
        saveInterestRatePeriods(request.getInterestRatePeriods(), interestRate, errorMessages);
        throwExceptionIfRequired(errorMessages);
        return interestRate.getId();
    }

    private void checkNameForDublicateNames(String name, List<String> errorMessages) {
        Optional<InterestRate> interestRateOptional = interestRateRepository.findFirstByNameAndStatus(name, InterestRateStatus.ACTIVE);
        if (interestRateOptional.isPresent()) {
            errorMessages.add("name-[name] must be unique;");
        }
    }

    private void checkNameForDublicateNamesForEdit(String name, Long id, List<String> errorMessages) {
        Optional<InterestRate> interestRateOptional = interestRateRepository.findFirstByNameAndIdNotInAndStatus(name, List.of(id), InterestRateStatus.ACTIVE);
        if (interestRateOptional.isPresent()) {
            errorMessages.add("name-[name] must be unique;");
        }
    }

    private void throwExceptionIfRequired(List<String> exceptionMessages) {
        if (!exceptionMessages.isEmpty()) {
            log.error(StringUtils.join("; ", exceptionMessages));
            StringBuilder sb = new StringBuilder();

            for (String exceptionMessage : exceptionMessages) {
                if (!exceptionMessage.contains(EPBFinalFields.VALIDATION_MESSAGE_REMOVE_INDICATOR)) {
                    sb.append(exceptionMessage);
                }
            }

            if (sb.isEmpty()) {
                sb.append("Error: Process Failed");
            }

            throw new ClientException(sb.toString().trim(), ErrorCode.CONFLICT);
        }
    }

    private void createInterestRateTerm(InterestRate interestRate, InterestRatePaymentTermBaseRequest request, List<String> errorMessages) {
        InterestRatePaymentTerms interestRatePaymentTerms = new InterestRatePaymentTerms();
        interestRatePaymentTerms.setType(request.getType());
        interestRatePaymentTerms.setValue(request.getValue());
        interestRatePaymentTerms.setValueTo(request.getValueTo());
        interestRatePaymentTerms.setCalendarId(checkCalendarId(request.getCalendarId(), "paymentTerm.calendarId-[CalendarId] Can't find active calendar with id:%s;", errorMessages));
        interestRatePaymentTerms.setDueDateChange(request.getDueDateChange());
        interestRatePaymentTerms.setName(request.getName());
        interestRatePaymentTerms.setInterestRateId(interestRate.getId());
        interestRatePaymentTerms.setStatus(InterestRateSubObjectStatus.ACTIVE);
        interestRatePaymentTerms.setExcludes(request.getExcludes());
        interestRatePaymentTermsRepository.save(interestRatePaymentTerms);
    }

    private Long checkCalendarIdForEdit(InterestRatePaymentTerms interestRatePaymentTerms, Long calendarId, String errorMessage, List<String> errorMessages) {
        if (interestRatePaymentTerms.getCalendarId().equals(calendarId)) {
            return calendarId;
        }
        Optional<Calendar> calendarOptional = calendarRepository.findByIdAndStatusIsIn(calendarId, List.of(NomenclatureItemStatus.ACTIVE));
        if (calendarOptional.isPresent()) {
            return calendarOptional.get().getId();
        } else {
            errorMessages.add(errorMessage.formatted(calendarId));
            return null;
        }
        /*return calendarRepository.findByIdAndStatusIsIn(calendarId, List.of(NomenclatureItemStatus.ACTIVE))
                .orElseThrow(() -> new DomainEntityNotFoundException(errorMessage
                        .formatted(calendarId)));*/
    }

    private Long checkCalendarId(Long calendarId, String errorMessage, List<String> errorMessages) {
        Optional<Calendar> calendarOptional = calendarRepository.findByIdAndStatusIsIn(calendarId, List.of(NomenclatureItemStatus.ACTIVE));
        if (calendarOptional.isPresent()) {
            return calendarOptional.get().getId();
        } else {
            errorMessages.add(errorMessage.formatted(calendarId));
            return null;
        }
        /*return calendarRepository.findByIdAndStatusIsIn(calendarId, List.of(NomenclatureItemStatus.ACTIVE))
                .orElseThrow(() -> new DomainEntityNotFoundException(errorMessage
                        .formatted(calendarId)));*/
    }

    private Calendar checkCalendarIdForView(Long calendarId, String errorMessage) {
        return calendarRepository.findById(calendarId)
                .orElseThrow(() -> new DomainEntityNotFoundException(errorMessage
                        .formatted(calendarId)));
    }

    private void saveInterestRatePeriods(List<InterestRatePeriodsCreateRequest> interestRatePeriodsCreateRequest, InterestRate interestRate, List<String> errorMessages) {
        if (CollectionUtils.isNotEmpty(interestRatePeriodsCreateRequest)) {
            List<InterestRatePeriods> interestRatePeriods = new ArrayList<>();
            for (int i = 0; i < interestRatePeriodsCreateRequest.size(); i++) {
                InterestRatePeriodsCreateRequest request = interestRatePeriodsCreateRequest.get(i);
                InterestRatePeriods interestRatePeriod = new InterestRatePeriods();
                interestRatePeriod.setAmountInPercent(request.getAmountInPercent());
                interestRatePeriod.setBaseInterestRate(request.getBaseInterestRate());
                interestRatePeriod.setApplicableInterestRate(calculateApplicableInterestRate(request.getBaseInterestRate(), request.getAmountInPercent()));
                interestRatePeriod.setFee(request.getFee());
                interestRatePeriod.setCurrencyId(checkCurrencyId(request.getCurrencyId(), i, "interestRatePeriodsCreateRequest[%s].currencyId-[CurrencyId] Can't find active currency with id: %s;",  List.of(NomenclatureItemStatus.ACTIVE),errorMessages));
                interestRatePeriod.setValidFrom(request.getValidFrom());
                interestRatePeriod.setStatus(InterestRatePeriodStatus.ACTIVE);
                interestRatePeriod.setInterestRateId(interestRate.getId());
                interestRatePeriods.add(interestRatePeriod);
            }
            if (CollectionUtils.isNotEmpty(interestRatePeriods)) {
                interestRatePeriodsRepository.saveAll(interestRatePeriods);
            }
        }
    }

    private void createInterestRatePeriods(List<InterestRatePeriodsEditRequest> interestRatePeriodsEditRequests, InterestRate interestRate, List<String> errorMessages) {
        if (CollectionUtils.isNotEmpty(interestRatePeriodsEditRequests)) {
            List<InterestRatePeriods> interestRatePeriods = new ArrayList<>();
            for (int i = 0; i < interestRatePeriodsEditRequests.size(); i++) {
                InterestRatePeriodsEditRequest request = interestRatePeriodsEditRequests.get(i);
                InterestRatePeriods interestRatePeriod = new InterestRatePeriods();
                interestRatePeriod.setAmountInPercent(request.getAmountInPercent());
                interestRatePeriod.setBaseInterestRate(request.getBaseInterestRate());
                interestRatePeriod.setApplicableInterestRate(calculateApplicableInterestRate(request.getBaseInterestRate(), request.getAmountInPercent()));
                interestRatePeriod.setFee(request.getFee());
                interestRatePeriod.setCurrencyId(checkCurrencyId(request.getCurrencyId(), i, "interestRatePeriodsCreateRequest[%s].currencyId-[CurrencyId] Can't find active currency with id: %s;",  List.of(NomenclatureItemStatus.ACTIVE,NomenclatureItemStatus.INACTIVE),errorMessages));
                interestRatePeriod.setCurrencyId(request.getCurrencyId());
                interestRatePeriod.setValidFrom(request.getValidFrom());
                interestRatePeriod.setStatus(InterestRatePeriodStatus.ACTIVE);
                interestRatePeriod.setInterestRateId(interestRate.getId());
                interestRatePeriods.add(interestRatePeriod);
            }
            if (CollectionUtils.isNotEmpty(interestRatePeriods)) {
                interestRatePeriodsRepository.saveAll(interestRatePeriods);
            }
        }

    }

    private Long checkCurrencyId(Long currencyId, int i, String errorMessage,List<NomenclatureItemStatus> statusesToCheck, List<String> errorMessages) {
        Optional<Currency> currencyOptional = currencyRepository.findByIdAndStatus(currencyId, statusesToCheck);
        if (currencyOptional.isPresent()) {
            return currencyOptional.get().getId();
        } else {
            errorMessages.add(errorMessage.formatted(i, currencyId));
            return null;
        }
    }

    private InterestRate mapAndSaveInterestRateFromRequest(InterestRateCreateRequest request) {
        InterestRate interestRate = new InterestRate();
        interestRate.setName(request.getName());
        interestRate.setIsDefault(request.getIsDefault());
        interestRate.setType(request.getType());
        interestRate.setCharging(request.getCharging());
        interestRate.setMinAmountForInterestCharging(request.getMinAmountForInterestCharging());
        interestRate.setMinAmountOfInterest(request.getMinAmountOfInterest());
        interestRate.setMaxAmountOfInterest(request.getMaxAmountOfInterest());
        interestRate.setCurrencyId(request.getCurrencyId());
        interestRate.setMinAmountOfInterestInPercentOfLiability(request.getMinAmountOfInterestInPercentOfLiability());
        interestRate.setMaxAmountOfInterestInPercentOfTheLiability(request.getMaxAmountOfInterestInPercentOfTheLiability());
        interestRate.setGracePeriod(request.getGracePeriod());
        interestRate.setPeriodicity(request.getPeriodicity());
        interestRate.setGrouping(request.getGrouping());
        interestRate.setIncomeAccountNumber(request.getIncomeAccountNumber());
        interestRate.setCostCenterControllingOrder(request.getCostCenterControllingOrder());
        interestRate.setStatus(InterestRateStatus.ACTIVE);
        return interestRateRepository.save(interestRate);
    }

    private InterestRate updateAndSaveInterestRate(InterestRate interestRate, InterestRateEditRequest request) {
        interestRate.setName(request.getName());
        interestRate.setIsDefault(request.getIsDefault());
        interestRate.setType(request.getType());
        interestRate.setCharging(request.getCharging());
        interestRate.setMinAmountForInterestCharging(request.getMinAmountForInterestCharging());
        interestRate.setMinAmountOfInterest(request.getMinAmountOfInterest());
        interestRate.setMaxAmountOfInterest(request.getMaxAmountOfInterest());
        interestRate.setCurrencyId(request.getCurrencyId());
        interestRate.setMinAmountOfInterestInPercentOfLiability(request.getMinAmountOfInterestInPercentOfLiability());
        interestRate.setMaxAmountOfInterestInPercentOfTheLiability(request.getMaxAmountOfInterestInPercentOfTheLiability());
        interestRate.setGracePeriod(request.getGracePeriod());
        interestRate.setPeriodicity(request.getPeriodicity());
        interestRate.setGrouping(request.getGrouping());
        interestRate.setIncomeAccountNumber(request.getIncomeAccountNumber());
        interestRate.setCostCenterControllingOrder(request.getCostCenterControllingOrder());
        return interestRateRepository.save(interestRate);
    }

    public InterestRateResponse getDefault() {
        Optional<InterestRate> interestRateOptional = interestRateRepository.findByIsDefaultAndStatus(true, InterestRateStatus.ACTIVE);
        return interestRateOptional.map(this::getPreviewObject).orElse(null);

    }

    public InterestRateResponse preview(Long id) {
        InterestRate interestRate = interestRateRepository.findByIdAndStatusIn(id, getStatuses())
                .orElseThrow(() -> new DomainEntityNotFoundException("id-Can't find interest rate with id: %s;".formatted(id)));
        return getPreviewObject(interestRate);
    }

    private InterestRateResponse getPreviewObject(InterestRate interestRate) {
        List<InterestRatePeriodsResponse> interestRatePeriods = getInterestRatePeriods(interestRate);
        InterestRatePaymentTermResponse interestRatePaymentTerms = getInterestPaymentTerms(interestRate);
        return getInterestRateResponse(interestRate, interestRatePeriods, interestRatePaymentTerms);
    }

    private InterestRateResponse getInterestRateResponse(InterestRate interestRate, List<InterestRatePeriodsResponse> interestRatePeriods, InterestRatePaymentTermResponse interestRatePaymentTerms) {
        Currency currency = getCurrencyForView(interestRate.getCurrencyId());
        return InterestRateResponse.builder()
                .id(interestRate.getId())
                .name(interestRate.getName())
                .isDefault(interestRate.getIsDefault())
                .type(interestRate.getType())
                .charging(interestRate.getCharging())
                .minAmountForInterestCharging(interestRate.getMinAmountForInterestCharging())
                .minAmountOfInterest(interestRate.getMinAmountOfInterest())
                .maxAmountOfInterest(interestRate.getMaxAmountOfInterest())
                .currencyId(currency.getId())
                .currencyName(currency.getName())
                .minAmountOfInterestInPercentOfLiability(interestRate.getMinAmountOfInterestInPercentOfLiability())
                .maxAmountOfInterestInPercentOfTheLiability(interestRate.getMaxAmountOfInterestInPercentOfTheLiability())
                .gracePeriod(interestRate.getGracePeriod())
                .periodicity(interestRate.getPeriodicity())
                .grouping(interestRate.getGrouping())
                .incomeAccountNumber(interestRate.getIncomeAccountNumber())
                .costCenterControllingOrder(interestRate.getCostCenterControllingOrder())
                .status(interestRate.getStatus())
                .interestRatePeriodsResponse(interestRatePeriods)
                .paymentTermResponse(interestRatePaymentTerms)
                .build();
    }

    private InterestRatePaymentTermResponse getInterestPaymentTerms(InterestRate interestRate) {
        Optional<InterestRatePaymentTerms> interestRatePaymentTerms =
                interestRatePaymentTermsRepository.findByInterestRateIdAndStatus(interestRate.getId(), InterestRateSubObjectStatus.ACTIVE);
        if (interestRatePaymentTerms.isPresent()) {
            return mapInterestRatePaymentTerm(interestRatePaymentTerms.get());
        }
        return new InterestRatePaymentTermResponse();
    }

    private InterestRatePaymentTermResponse mapInterestRatePaymentTerm(InterestRatePaymentTerms interestRatePaymentTerms) {
        Calendar calendar = checkCalendarIdForView(interestRatePaymentTerms.getCalendarId(), "paymentTerm.calendarId-[CalendarId] Can't find active calendar with id:%s;");
        return InterestRatePaymentTermResponse.builder()
                .id(interestRatePaymentTerms.getId())
                .type(interestRatePaymentTerms.getType())
                .name(interestRatePaymentTerms.getName())
                .value(interestRatePaymentTerms.getValue())
                .valueFrom(interestRatePaymentTerms.getValueFrom())
                .valueTo(interestRatePaymentTerms.getValueTo())
                .calendarId(calendar.getId())
                .calendarName(calendar.getName())
                .dueDateChange(interestRatePaymentTerms.getDueDateChange())
                .excludes(interestRatePaymentTerms.getExcludes())
                .build();
    }

    private void updateInterestRatePaymentTerm(InterestRatePaymentTerms interestRatePaymentTerms, InterestRatePaymentTermEditRequest paymentTermEditRequest, List<String> errorMessages) {
        interestRatePaymentTerms.setType(paymentTermEditRequest.getType());
        interestRatePaymentTerms.setName(paymentTermEditRequest.getName());
        interestRatePaymentTerms.setValue(paymentTermEditRequest.getValue());
        interestRatePaymentTerms.setValueFrom(paymentTermEditRequest.getValueFrom());
        interestRatePaymentTerms.setValueTo(paymentTermEditRequest.getValueTo());
        interestRatePaymentTerms.setCalendarId(checkCalendarIdForEdit(interestRatePaymentTerms, paymentTermEditRequest.getCalendarId(), "paymentTermEditRequest.calendarId-[CalendarId] Can't find active calendar with id:%s;", errorMessages));
        interestRatePaymentTerms.setDueDateChange(paymentTermEditRequest.getDueDateChange());
        interestRatePaymentTerms.setExcludes(paymentTermEditRequest.getExcludes());
        throwExceptionIfRequired(errorMessages);
        interestRatePaymentTermsRepository.save(interestRatePaymentTerms);
    }

    private List<InterestRatePeriodsResponse> getInterestRatePeriods(InterestRate interestRate) {
        List<InterestRatePeriods> interestRatePeriodsList =
                interestRatePeriodsRepository.findByInterestRateIdAndStatus(interestRate.getId(), InterestRatePeriodStatus.ACTIVE);
        return mapInterestRatePeriodsToResponse(interestRatePeriodsList);
    }

    private List<InterestRatePeriodsResponse> mapInterestRatePeriodsToResponse(List<InterestRatePeriods> interestRatePeriodsList) {
        List<InterestRatePeriodsResponse> returnList = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(interestRatePeriodsList)) {
            for (InterestRatePeriods item : interestRatePeriodsList) {
                Currency currency = getCurrencyForView(item.getCurrencyId());
                InterestRatePeriodsResponse interestRatePeriods = InterestRatePeriodsResponse.builder()
                        .id(item.getId())
                        .amountInPercent(item.getAmountInPercent())
                        .baseInterestRate(item.getBaseInterestRate())
                        .applicableInterestRate(item.getApplicableInterestRate())
                        .fee(item.getFee())
                        .currencyId(currency.getId())
                        .currencyName(currency.getName())
                        .validFrom(item.getValidFrom())
                        .build();
                returnList.add(interestRatePeriods);
            }
        } else return new ArrayList<>();
        return returnList;
    }

    private Long getCurrency(Long currencyId) {
        Currency currency = currencyRepository.findByIdAndStatus(currencyId, List.of(NomenclatureItemStatus.ACTIVE))
                .orElseThrow(() -> new DomainEntityNotFoundException("id-[id] Can't find active currency with id:%s;"
                        .formatted(currencyId)));
        return currency.getId();
    }

    private Currency getCurrencyForView(Long currencyId) {
        Currency currency = currencyRepository.findByIdAndStatus(currencyId, List.of(NomenclatureItemStatus.ACTIVE, NomenclatureItemStatus.INACTIVE, NomenclatureItemStatus.DELETED))
                .orElseThrow(() -> new DomainEntityNotFoundException("id-[id] Can't find active currency with id:%s;"
                        .formatted(currencyId)));
        return currency;
    }

    private List<InterestRateStatus> getStatuses() {
        List<InterestRateStatus> statuses = new ArrayList<>();
        List<String> context = permissionService.getPermissionsFromContext(PermissionContextEnum.INTEREST_RATES);
        if (context.contains(INTEREST_RATES_VIEW_DELETED.getId())) {
            statuses.add(InterestRateStatus.DELETED);
        }
        if (context.contains(INTEREST_RATES_VIEW_BASIC.getId())) {
            statuses.add(InterestRateStatus.ACTIVE);
        }
        return statuses;
    }

    public Long delete(Long id) {
        log.debug("Delete Interest Rate with id: {}", id);
        InterestRate interestRate = interestRateRepository.findByIdAndStatusIn(id, List.of(InterestRateStatus.ACTIVE))
                .orElseThrow(() -> new DomainEntityNotFoundException("Can't find active interest rate with id:%s".formatted(id)));

        if(interestRate.getIsDefault()) {
            throw new OperationNotAllowedException("Can't delete defult interest rate with id:%s".formatted(id));
        }

        if (interestRateRepository.hasConnectionWithProductContract(id)) {
            log.error("id-You can’t delete the Interest Rate with ID %s because it is connected to the Product Contract;".formatted(id));
            throw new OperationNotAllowedException("id-You can’t delete the Interest Rate with ID %s because it is connected to the Product Contract;".formatted(id));
        }

        if (interestRateRepository.hasConnectionWithServiceContract(id)) {
            log.error("id-You can’t delete the Interest Rate with ID %s because it is connected to the Service Contract;".formatted(id));
            throw new OperationNotAllowedException("id-You can’t delete the Interest Rate with ID %s because it is connected to the Service Contract;".formatted(id));
        }

        if (interestRateRepository.hasConnectionWithServiceOrder(id)) {
            log.error("id-You can’t delete the Interest Rate with ID %s because it is connected to the Service Order;".formatted(id));
            throw new OperationNotAllowedException("id-You can’t delete the Interest Rate with ID %s because it is connected to the Service Order;".formatted(id));
        }

        if (interestRateRepository.hasConnectionWithGoodsOrder(id)) {
            log.error("id-You can’t delete the Interest Rate with ID %s because it is connected to the Goods Order;".formatted(id));
            throw new OperationNotAllowedException("id-You can’t delete the Interest Rate with ID %s because it is connected to the Goods Order;".formatted(id));
        }

        interestRate.setStatus(InterestRateStatus.DELETED);
        interestRateRepository.save(interestRate);
        return interestRate.getId();
    }

    @Transactional
    public Long update(Long id, InterestRateEditRequest request) {
        log.debug("Update Interest Rate: {} , {}", id, request.toString());
        List<String> errorMessages = new ArrayList<>();
        InterestRate interestRateToEdit = interestRateRepository.findByIdAndStatusIn(id, List.of(InterestRateStatus.ACTIVE))
                .orElseThrow(() -> new DomainEntityNotFoundException("id-[Id] Can't find Active interest rate;"));
        Optional<InterestRate> defaultInterestRateOptional = interestRateRepository.findByIsDefaultAndStatus(true, InterestRateStatus.ACTIVE);
        if (defaultInterestRateOptional.isPresent()) {
            InterestRate defaultInterestRate = defaultInterestRateOptional.get();
            if (!defaultInterestRate.getId().equals(interestRateToEdit.getId()) && request.getIsDefault()) {
                throw new ClientException("isDefault-[IsDefault] can't Edit interest rate, because there is default interest rate with id:%s;"
                        .formatted(defaultInterestRate.getId()), ErrorCode.CONFLICT);
            }
        }
        Long requestCurrencyId = request.getCurrencyId();
        List<NomenclatureItemStatus> listOfStatuses;
        if(interestRateToEdit.getCurrencyId().equals(requestCurrencyId)){
            listOfStatuses = List.of(NomenclatureItemStatus.ACTIVE,NomenclatureItemStatus.INACTIVE);
        } else {
            listOfStatuses = List.of(NomenclatureItemStatus.ACTIVE);
        }
        Optional<Currency> currency = currencyRepository.findByIdAndStatus(requestCurrencyId, listOfStatuses);
        if (currency.isEmpty()) {
            errorMessages.add("currencyId-[CurrencyId] can't find active currency with id:%s;".formatted(request.getCurrencyId()));
        }
        checkNameForDublicateNamesForEdit(request.getName(), interestRateToEdit.getId(), errorMessages);
        InterestRate interestRate = updateAndSaveInterestRate(interestRateToEdit, request);
        //updatePaymentTerm
        InterestRatePaymentTermEditRequest paymentTermEditRequest = request.getPaymentTerm();
        if (paymentTermEditRequest.getId() != null) {
            InterestRatePaymentTerms interestRatePaymentTerms = interestRatePaymentTermsRepository.findByIdAndStatus(paymentTermEditRequest.getId(), InterestRateSubObjectStatus.ACTIVE)
                    .orElseThrow(() -> new DomainEntityNotFoundException("Can't find Active InterestRatePaymentTerms with id:%s;".formatted(paymentTermEditRequest.getId())));
            updateInterestRatePaymentTerm(interestRatePaymentTerms, paymentTermEditRequest, errorMessages);
        } else {
            Optional<InterestRatePaymentTerms> interestRatePaymentTermOptional =
                    interestRatePaymentTermsRepository.findByInterestRateIdAndStatus(interestRate.getId(), InterestRateSubObjectStatus.ACTIVE);

            if (interestRatePaymentTermOptional.isPresent()) {
                InterestRatePaymentTerms interestRatePaymentTerms = interestRatePaymentTermOptional.get();
                interestRatePaymentTerms.setStatus(InterestRateSubObjectStatus.DELETED);

                interestRatePaymentTermsRepository.save(interestRatePaymentTerms);
            }

            createInterestRateTerm(interestRate, request.getPaymentTerm(), errorMessages);
        }
        //updateInterestRatePeriods
        List<InterestRatePeriodsEditRequest> interestRatePeriodsEditRequests = request.getInterestRatePeriods();
        if (CollectionUtils.isNotEmpty(interestRatePeriodsEditRequests)) {
            List<InterestRatePeriods> interestRatePeriodsToDelete = new ArrayList<>();
            List<InterestRatePeriods> interestRatePeriodsToEdit = new ArrayList<>();
            List<InterestRatePeriodsEditRequest> interestRatePeriodsToCreateRequest = new ArrayList<>();
            List<Long> editInterestRateIds = interestRatePeriodsEditRequests.stream()
                    .map(InterestRatePeriodsEditRequest::getId)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
            if (CollectionUtils.isEmpty(editInterestRateIds)) {
                List<InterestRatePeriods> dbInterestRatesToDelete = interestRatePeriodsRepository.findByInterestRateIdAndStatus(
                        interestRate.getId(), InterestRatePeriodStatus.ACTIVE);
                if (!CollectionUtils.isEmpty(dbInterestRatesToDelete)) {
                    for (InterestRatePeriods item : dbInterestRatesToDelete) {
                        item.setStatus(InterestRatePeriodStatus.DELETED);
                        interestRatePeriodsRepository.save(item);
                    }
                }
            }
            List<InterestRatePeriods> dbInterestRates = interestRatePeriodsRepository.findByIdNotInAndInterestRateIds(
                    interestRate.getId(), editInterestRateIds);
            if (CollectionUtils.isNotEmpty(dbInterestRates)) {
                for (InterestRatePeriods item : dbInterestRates) {
                    item.setStatus(InterestRatePeriodStatus.DELETED);
                    interestRatePeriodsToDelete.add(item);
                }
                interestRatePeriodsRepository.saveAll(interestRatePeriodsToDelete);
            }
            for (int i = 0; i < interestRatePeriodsEditRequests.size(); i++) {
                InterestRatePeriodsEditRequest item = interestRatePeriodsEditRequests.get(i);
                if (item.getId() != null) {
                    InterestRatePeriods interestRatePeriods = interestRatePeriodsRepository.findByIdAndStatusAndInterestRateId(item.getId(), InterestRatePeriodStatus.ACTIVE, interestRateToEdit.getId())
                            .orElseThrow(() -> new DomainEntityNotFoundException("Can't find active interest rate period with id:%s;".formatted(item.getId())));
                    interestRatePeriods.setAmountInPercent(item.getAmountInPercent());
                    interestRatePeriods.setBaseInterestRate(item.getBaseInterestRate());
                    interestRatePeriods.setApplicableInterestRate(calculateApplicableInterestRate(item.getBaseInterestRate(), item.getAmountInPercent()));
                    interestRatePeriods.setFee(item.getFee());
                    interestRatePeriods.setCurrencyId(checkCurrencyId(item.getCurrencyId(), i,"interestRatePeriodsEditRequest[%s].currencyId-[CurrencyId] Can't find active currency with id: %s;", List.of(NomenclatureItemStatus.ACTIVE,NomenclatureItemStatus.INACTIVE), errorMessages));
                    interestRatePeriods.setValidFrom(item.getValidFrom());
                    interestRatePeriodsToEdit.add(interestRatePeriods);
                } else {
                    interestRatePeriodsToCreateRequest.add(item);
                }
            }
            interestRatePeriodsRepository.saveAll(interestRatePeriodsToEdit);
            createInterestRatePeriods(interestRatePeriodsToCreateRequest, interestRate, errorMessages);
        } else {
            List<InterestRatePeriods> interestRatePeriodsList = interestRatePeriodsRepository.findByInterestRateIdAndStatus(interestRate.getId(), InterestRatePeriodStatus.ACTIVE);
            List<InterestRatePeriods> interestRatePeriodsToDelete = new ArrayList<>();
            if (CollectionUtils.isNotEmpty(interestRatePeriodsList)) {
                for (InterestRatePeriods item : interestRatePeriodsList) {
                    item.setStatus(InterestRatePeriodStatus.DELETED);
                    interestRatePeriodsToDelete.add(item);
                }
            }
            interestRatePeriodsRepository.saveAll(interestRatePeriodsToDelete);
        }
        throwExceptionIfRequired(errorMessages);
        return interestRate.getId();
    }

    public Page<InterestRateListResponse> list(InterestRateListRequest request) {
        log.debug("List Interest Rate : {}", request.toString());
        Sort.Order order = new Sort.Order(request.getDirection(), checkSortField(request));
        return interestRateRepository.list(
                EPBStringUtils.fromPromptToQueryParameter(request.getPrompt()),
                getSearchField(request),
                getStatuses(),
                getCharging(request),
                getGrouping(request),
                getInterestRateType(request),
                PageRequest.of(request.getPage(), request.getSize(), Sort.by(order)));
    }

    private List<InterestRateType> getInterestRateType(InterestRateListRequest request) {
        if (CollectionUtils.isEmpty(request.getType())) {
            return null;
        } else return request.getType();
    }

    private List<Boolean> getGrouping(InterestRateListRequest request) {
        InterestRateGrouping grouping = request.getGrouping();
        List<Boolean> result = new ArrayList<>();
        if (grouping == null) {
            result = List.of(true, false);
        } else if (grouping.equals(InterestRateGrouping.ALL)) {
            result = List.of(true, false);
        } else if (grouping.equals(InterestRateGrouping.YES)) {
            result = List.of(true);
        } else if (grouping.equals(InterestRateGrouping.NO)) {
            result = List.of(false);
        }
        return result;
    }

    private List<InterestRateCharging> getCharging(InterestRateListRequest request) {
        if (CollectionUtils.isEmpty(request.getCharging())) {
            return null;
        } else return request.getCharging();
    }

    private String getSearchField(InterestRateListRequest request) {
        String searchField = null;
        if (request.getSearchBy() != null) {
            searchField = request.getSearchBy().getValue();
        } else searchField = InterestRateSearchFields.ALL.getValue();
        return searchField;
    }

    private String checkSortField(InterestRateListRequest request) {
        if (request.getSortBy() == null) {
            return InterestRateListColumns.ID.getValue();
        } else return request.getSortBy().getValue();
    }


    /**
     * Intended for the interest rate dropdowns in product/service contract and service/goods order create/edit forms.
     *
     * @return active interest rates optionally filtered by the prompt.
     */
    public Page<InterestRateShortResponse> getAvailableInterestRatesForContracts(String prompt, int page, int size) {
        log.debug("Get available interest rates for contracts");

        Page<InterestRate> interestRates = interestRateRepository.findAvailableInterestRatesForContracts(
                EPBStringUtils.fromPromptToQueryParameter(prompt),
                PageRequest.of(page, size)
        );

        return interestRates.map(ir -> new InterestRateShortResponse(ir.getId(), ir.getName(), ir.getStatus(), ir.getIsDefault()));
    }

    private BigDecimal calculateApplicableInterestRate(BigDecimal baseInterestRate, BigDecimal amountInterestRate) {
        if (baseInterestRate != null) {
            if (baseInterestRate.compareTo(BigDecimal.ZERO) > 0) {
                return baseInterestRate.add(amountInterestRate);
            }
        }

        return amountInterestRate;
    }
}
