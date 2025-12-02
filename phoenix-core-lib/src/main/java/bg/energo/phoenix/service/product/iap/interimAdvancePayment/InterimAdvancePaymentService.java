package bg.energo.phoenix.service.product.iap.interimAdvancePayment;

import bg.energo.phoenix.exception.ClientException;
import bg.energo.phoenix.exception.DomainEntityNotFoundException;
import bg.energo.phoenix.exception.ErrorCode;
import bg.energo.phoenix.exception.OperationNotAllowedException;
import bg.energo.phoenix.model.entity.nomenclature.product.Currency;
import bg.energo.phoenix.model.entity.nomenclature.product.terms.Calendar;
import bg.energo.phoenix.model.entity.product.iap.advancedPaymentGroup.AdvancedPaymentGroupAdvancedPayments;
import bg.energo.phoenix.model.entity.product.iap.interimAdvancePayment.*;
import bg.energo.phoenix.model.entity.product.price.priceComponent.PriceComponent;
import bg.energo.phoenix.model.entity.product.price.priceComponent.PriceComponentFormulaVariable;
import bg.energo.phoenix.model.entity.product.price.priceComponent.applicationModel.ApplicationModel;
import bg.energo.phoenix.model.entity.product.product.ProductDetails;
import bg.energo.phoenix.model.entity.product.product.ProductInterimAndAdvancePayments;
import bg.energo.phoenix.model.entity.product.service.ServiceDetails;
import bg.energo.phoenix.model.entity.product.service.ServiceInterimAndAdvancePayment;
import bg.energo.phoenix.model.enums.copy.domain.CopyDomain;
import bg.energo.phoenix.model.enums.nomenclature.NomenclatureItemStatus;
import bg.energo.phoenix.model.enums.product.iap.advancedPaymentGroup.AdvancedPaymentGroupStatus;
import bg.energo.phoenix.model.enums.product.iap.interimAdvancePayment.*;
import bg.energo.phoenix.model.enums.product.iap.interimAdvancePayment.filter.InterimAdvancePaymentSearchField;
import bg.energo.phoenix.model.enums.product.iap.interimAdvancePayment.filter.InterimAdvancePaymentSortField;
import bg.energo.phoenix.model.enums.product.price.priceComponent.PriceComponentStatus;
import bg.energo.phoenix.model.enums.product.price.priceComponent.applicationModel.ApplicationModelStatus;
import bg.energo.phoenix.model.enums.product.product.ProductSubObjectStatus;
import bg.energo.phoenix.model.enums.product.service.ServiceSubobjectStatus;
import bg.energo.phoenix.model.enums.product.termination.terminations.filter.TerminationSearchFields;
import bg.energo.phoenix.model.enums.product.termination.terminations.filter.TerminationSortFields;
import bg.energo.phoenix.model.request.copy.domain.CopyDomainBaseRequest;
import bg.energo.phoenix.model.request.product.iap.advancedPaymentGroup.SearchInterimAdvancePaymentRequest;
import bg.energo.phoenix.model.request.product.iap.interimAdvancePayment.CreateInterimAdvancePaymentRequest;
import bg.energo.phoenix.model.request.product.iap.interimAdvancePayment.EditInterimAdvancePaymentRequest;
import bg.energo.phoenix.model.request.product.iap.interimAdvancePayment.EditInterimAdvancePaymentTermRequest;
import bg.energo.phoenix.model.request.product.iap.interimAdvancePayment.InterimAdvancePaymentListingRequest;
import bg.energo.phoenix.model.request.product.iap.interimAdvancePayment.periodical.edit.EditDayOfWeekAndPeriodOfYearAndDateOfMonthRequest;
import bg.energo.phoenix.model.request.product.price.priceComponent.AvailablePriceComponentSearchRequest;
import bg.energo.phoenix.model.response.AdvancedPaymentGroup.AdvancedPaymentSimpleInfoResponse;
import bg.energo.phoenix.model.response.AdvancedPaymentGroup.InterimAdvancePaymentSearchListResponse;
import bg.energo.phoenix.model.response.copy.domain.CopyDomainListResponse;
import bg.energo.phoenix.model.response.interimAdvancePayment.InterimAdvancePaymentListResponse;
import bg.energo.phoenix.model.response.interimAdvancePayment.InterimAdvancePaymentResponse;
import bg.energo.phoenix.model.response.interimAdvancePayment.InterimAdvancePaymentTermsResponse;
import bg.energo.phoenix.model.response.interimAdvancePayment.copy.InterimAdvancePaymentCopyResponse;
import bg.energo.phoenix.model.response.interimAdvancePayment.periodical.DateOfMonthResponse;
import bg.energo.phoenix.model.response.interimAdvancePayment.periodical.DayOfWeekAndPeriodOfYearAndDateOfMonthResponse;
import bg.energo.phoenix.model.response.interimAdvancePayment.periodical.DayOfWeekResponse;
import bg.energo.phoenix.model.response.interimAdvancePayment.periodical.PeriodOfYearResponse;
import bg.energo.phoenix.model.response.nomenclature.product.currency.CurrencyResponse;
import bg.energo.phoenix.model.response.nomenclature.terms.CalendarResponse;
import bg.energo.phoenix.model.response.priceComponent.AvailablePriceComponentResponse;
import bg.energo.phoenix.model.response.priceComponent.PriceComponentDetailedResponse;
import bg.energo.phoenix.model.response.terms.copy.InvoicePaymentTermsCopyResponse;
import bg.energo.phoenix.permissions.PermissionContextEnum;
import bg.energo.phoenix.repository.nomenclature.product.CurrencyRepository;
import bg.energo.phoenix.repository.nomenclature.product.terms.CalendarRepository;
import bg.energo.phoenix.repository.product.iap.advancedPaymentGroup.AdvancedPaymentGroupAdvancedPaymentsRepository;
import bg.energo.phoenix.repository.product.iap.advancedPaymentGroup.mappers.AdvancePaymentGroupMapper;
import bg.energo.phoenix.repository.product.iap.interimAdvancePayment.InterimAdvancePaymentRepository;
import bg.energo.phoenix.repository.product.iap.interimAdvancePayment.InterimAdvancePaymentTermsRepository;
import bg.energo.phoenix.repository.product.price.applicationModel.ApplicationModelRepository;
import bg.energo.phoenix.repository.product.price.priceComponent.PriceComponentRepository;
import bg.energo.phoenix.repository.product.product.ProductInterimAndAdvancePaymentRepository;
import bg.energo.phoenix.repository.product.service.subObject.ServiceInterimAndAdvancePaymentRepository;
import bg.energo.phoenix.security.PermissionService;
import bg.energo.phoenix.service.copy.domain.CopyDomainBaseService;
import bg.energo.phoenix.service.nomenclature.product.CurrencyService;
import bg.energo.phoenix.service.product.price.priceComponent.PriceComponentService;
import bg.energo.phoenix.util.epb.EPBChainedExceptionTriggerUtil;
import bg.energo.phoenix.util.epb.EPBStringUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import static bg.energo.phoenix.permissions.PermissionEnum.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class InterimAdvancePaymentService implements CopyDomainBaseService {
    private final CalendarRepository calendarRepository;

    private final InterimAdvancePaymentRepository interimAdvancePaymentRepository;
    private final CurrencyRepository currencyRepository;
    private final InterimAdvancePaymentDateOfMonthService interimAdvancePaymentDateOfMonthService;
    private final InterimAdvancePaymentDayWeekPeriodYearService interimAdvancePaymentDayWeekPeriodYearService;
    private final InterimAdvancePaymentIssuingPeriodService interimAdvancePaymentIssuingPeriodService;
    private final InterimAdvancePaymentTermsService interimAdvancePaymentTermsService;

    private final PermissionService permissionService;
    private final PriceComponentRepository priceComponentRepository;
    private final ServiceInterimAndAdvancePaymentRepository serviceInterimAndAdvancePaymentRepository;
    private final AdvancedPaymentGroupAdvancedPaymentsRepository advancedPaymentGroupAdvancedPaymentsRepository;
    private final ProductInterimAndAdvancePaymentRepository productInterimAndAdvancePaymentRepository;

    private final CurrencyService currencyService;
    private final PriceComponentService priceComponentService;
    private final InterimAdvancePaymentTermsRepository interimAdvancePaymentTermsRepository;
    private final AdvancePaymentGroupMapper advancePaymentGroupMapper;
    private final ApplicationModelRepository applicationModelRepository;

    /**
     * Creates Interim Advance Payment based on provided request {@link InterimAdvancePayment}, {@link CreateInterimAdvancePaymentRequest}
     *
     * @param request contains data required to create Interim Advance Payments {@link CreateInterimAdvancePaymentRequest}
     * @return Interim Advance Payment Response which contains created Interim Advance Payment's state {@link InterimAdvancePaymentResponse}
     * @throws ClientException if any error raised during creation
     */
    @Transactional
    public Long create(CreateInterimAdvancePaymentRequest request) {
        StringBuilder errorMessage = new StringBuilder();
        InterimAdvancePayment interimAdvancePayment = createInterimAdvancePayment(request, errorMessage);

        interimAdvancePaymentTermsService.create(request, interimAdvancePayment, errorMessage);
        interimAdvancePaymentDateOfMonthService.createDateOfMonths(request, interimAdvancePayment, errorMessage);
        interimAdvancePaymentDayWeekPeriodYearService.createDaysOfWeek(request, interimAdvancePayment, errorMessage);
        interimAdvancePaymentIssuingPeriodService.createPeriodsOfYear(request, interimAdvancePayment, errorMessage);

        throwExceptionIfRequired(errorMessage);

        return interimAdvancePayment.getId();
    }

    /**
     * Throws exception if provided error message parameter is not empty
     *
     * @param errorMessage - contains all error messages which happened during creating/editing interim advance payment
     * @throws ClientException if error messages is not empty
     */
    private void throwExceptionIfRequired(StringBuilder errorMessage) {
        if (!errorMessage.isEmpty()) {
            throw new ClientException(errorMessage.toString(), ErrorCode.CONFLICT);
        }
    }

    /**
     * Create Interim Advance Payment model class and persist it {@link InterimAdvancePayment}
     *
     * @param request      contains required data to create model class {@link CreateInterimAdvancePaymentRequest}
     * @param errorMessage - used to store messages of errors during creation of model
     * @return Interim Advance Payment model {@link InterimAdvancePayment}
     */
    private InterimAdvancePayment createInterimAdvancePayment(CreateInterimAdvancePaymentRequest request, StringBuilder errorMessage) {
        InterimAdvancePayment interimAdvancePayment = new InterimAdvancePayment();
        interimAdvancePayment.setName(request.getName());
        interimAdvancePayment.setValueType(request.getValueType());
        interimAdvancePayment.setValue(request.getValue());
        interimAdvancePayment.setValueFrom(request.getValueFrom());
        interimAdvancePayment.setValueTo(request.getValueTo());
        interimAdvancePayment.setCurrency(getCurrency(request.getCurrencyId(), List.of(NomenclatureItemStatus.ACTIVE), errorMessage));
        interimAdvancePayment.setDateOfIssueType(request.getDateOfIssueType());
        interimAdvancePayment.setDateOfIssueValue(request.getDateOfIssueValue());
        interimAdvancePayment.setDateOfIssueValueFrom(request.getDateOfIssueValueFrom());
        interimAdvancePayment.setDateOfIssueValueTo(request.getDateOfIssueValueTo());
        interimAdvancePayment.setPriceComponent(getPriceComponent(request.getPriceComponentId(), errorMessage));
        interimAdvancePayment.setPaymentType(request.getPaymentType());
        interimAdvancePayment.setMatchTermOfStandardInvoice(request.getMatchesWithTermOfStandardInvoice());
        interimAdvancePayment.setNoInterestOnOverdueDebts(request.getNoInterestInOverdueDebt());
        setPeriodTypeAndYearRound(request, interimAdvancePayment);
        interimAdvancePayment.setIssuingForTheMonthToCurrent(request.getIssuingForTheMonthToCurrent());
        interimAdvancePayment.setDeductionFrom(request.getDeductionFrom());
        interimAdvancePayment.setStatus(InterimAdvancePaymentStatus.ACTIVE);
        return interimAdvancePaymentRepository.save(interimAdvancePayment);
    }

    /**
     * Find currency by id and return it if found, else add message to error messages {@link Currency}
     *
     * @param id           - id of currency {@link Currency}
     * @param statuses     - nomenclature statuses list
     * @param errorMessage - used to store message of error if Currency can't be found by provided id
     * @return Currency {@link Currency}
     */
    private Currency getCurrency(Long id, List<NomenclatureItemStatus> statuses, StringBuilder errorMessage) {
        if (id != null) {
            Optional<Currency> optionalCurrency = currencyRepository.findByIdAndStatus(id, statuses);
            if (optionalCurrency.isEmpty()) {
                errorMessage.append("currencyId-Currency not found by id: ").append(id).append(";");
                return null;
            } else return optionalCurrency.get();
        }
        return null;
    }

    /**
     * Find Price Component by id and return it if found, else add message to error messages {@link PriceComponent}
     *
     * @param id           - id of Price Component {@link PriceComponent}
     * @param errorMessage - used to store message of error if Currency can't be found by provided id
     * @return Price Component {@link PriceComponent}
     */
    private PriceComponent getPriceComponent(Long id, StringBuilder errorMessage) {
        if (id != null) {
            Optional<PriceComponent> optionalPriceComponent = priceComponentRepository.findByIdAndStatusIn(id, List.of(PriceComponentStatus.ACTIVE));
            if (optionalPriceComponent.isEmpty()) {
                errorMessage.append("priceComponentId-Price Component not found by id: ").append(id).append(";");
                return null;
            } else return optionalPriceComponent.get();
        }
        return null;
    }

    /**
     * Check if request contains Periods data {@link DayOfWeekAndPeriodOfYearAndDateOfMonthResponse}
     * and set period Type to Interim Advance Payment {@link InterimAdvancePayment}.
     * Check if field "Year Round" is provided and set it to Interim Advance Payment {@link InterimAdvancePayment},
     * if it is null set false by default
     *
     * @param request               - used to create Interim Advance Payment {@link InterimAdvancePayment}
     * @param interimAdvancePayment - Current state of Interim Advance Payment model {@link InterimAdvancePayment}
     */
    private void setPeriodTypeAndYearRound(CreateInterimAdvancePaymentRequest request, InterimAdvancePayment interimAdvancePayment) {
        if (request.getDayOfWeekAndPeriodOfYearAndDateOfMonth() != null) {
            interimAdvancePayment.setPeriodType(request.getDayOfWeekAndPeriodOfYearAndDateOfMonth().getPeriodType());
            if (request.getDayOfWeekAndPeriodOfYearAndDateOfMonth().getDayOfWeekAndPeriodOfYear() != null) {
                interimAdvancePayment.setYearRound(request.getDayOfWeekAndPeriodOfYearAndDateOfMonth().getDayOfWeekAndPeriodOfYear().getYearRound());
            } else interimAdvancePayment.setYearRound(false);
        } else interimAdvancePayment.setYearRound(false);
    }

    /**
     * Find Interim Advance Payment by id. Also find its sub-objects and return everything in response model {@link InterimAdvancePayment}, {@link InterimAdvancePaymentResponse}
     *
     * @param id - id of Interim Advance Payment
     * @return Response which contains complete data about interim advance payment {@link InterimAdvancePaymentResponse}
     * @throws DomainEntityNotFoundException if Interim Advance Payment can't be found by provided id
     */
    public InterimAdvancePaymentResponse view(Long id) {
        InterimAdvancePayment interimAdvancePayment = interimAdvancePaymentRepository.findByIdAndStatusIn(id, getStatusesAccordingPermissions())
                .orElseThrow(() -> new DomainEntityNotFoundException("Interim and advance payment not found by id: " + id + ";"));

        List<DayOfWeekResponse> dayOfWeekResponses = interimAdvancePaymentDayWeekPeriodYearService.findByInterimAdvancePaymentIdAndStatusIn(id, List.of(InterimAdvancePaymentSubObjectStatus.ACTIVE));
        List<PeriodOfYearResponse> periodOfYearResponses = interimAdvancePaymentIssuingPeriodService.findByInterimAdvancePaymentIdAndStatusIn(id, List.of(InterimAdvancePaymentSubObjectStatus.ACTIVE));
        List<DateOfMonthResponse> dateOfMonthResponses = interimAdvancePaymentDateOfMonthService.findByInterimAdvancePaymentIdAndStatusIn(id, List.of(InterimAdvancePaymentSubObjectStatus.ACTIVE));
        InterimAdvancePaymentTermsResponse interimAdvancePaymentTermsResponse = interimAdvancePaymentTermsService.findByInterimAdvancePaymentIdAndStatusIn(id, List.of(InterimAdvancePaymentSubObjectStatus.ACTIVE));

        DayOfWeekAndPeriodOfYearAndDateOfMonthResponse dayOfWeekAndPeriodOfYearAndDateOfMonthResponse
                = new DayOfWeekAndPeriodOfYearAndDateOfMonthResponse(interimAdvancePayment.getPeriodType(), dayOfWeekResponses, interimAdvancePayment.getYearRound(), periodOfYearResponses, dateOfMonthResponses);

        InterimAdvancePaymentResponse response = new InterimAdvancePaymentResponse(interimAdvancePayment, dayOfWeekAndPeriodOfYearAndDateOfMonthResponse, interimAdvancePaymentTermsResponse);
        response.setCurrency(getCurrency(interimAdvancePayment.getCurrency()));
        response.setPriceComponent(getPriceComponent(interimAdvancePayment.getPriceComponent()));
        response.setIsLocked(interimAdvancePaymentRepository.hasLockedConnection(id));
        return response;
    }

    private CurrencyResponse getCurrency(Currency currency) {
        if (currency == null) return null;
        return currencyService.view(currency.getId());
    }

    private PriceComponentDetailedResponse getPriceComponent(PriceComponent priceComponent) {
        if (priceComponent == null) return null;
        return priceComponentService.getById(priceComponent.getId());
    }

    /**
     * Find Interim Advance Payment by id and delete it if exists and is available {@link InterimAdvancePayment}
     *
     * @param id - id of Interim advance payment which is being deleted
     * @return Response which contains just id of deleted Interim Advance Payment {@link InterimAdvancePaymentResponse}
     * @throws DomainEntityNotFoundException if ACTIVE Interim Advance Payment can't be found by provided id
     * @throws ClientException               if Interim Advance Payment is connected to group.
     */
    @Transactional
    public Long delete(Long id) {
        InterimAdvancePayment interimAdvancePayment = interimAdvancePaymentRepository.findByIdAndStatusIn(id, List.of(InterimAdvancePaymentStatus.ACTIVE))
                .orElseThrow(() -> new DomainEntityNotFoundException("Interim and advance payment not found by id: " + id + ";"));

        if (interimAdvancePayment.getStatus().equals(InterimAdvancePaymentStatus.DELETED)) {
            log.error("Interim and Advance Payment is already deleted;");
            throw new ClientException("Interim and Advance Payment is already deleted;", ErrorCode.CONFLICT);
        }

        if (interimAdvancePayment.getGroupDetailId() != null) {
            log.error("Interim and Advance Payment is connected to group;");
            throw new ClientException("Interim and Advance Payment is connected to group;", ErrorCode.CONFLICT);
        }

        if (interimAdvancePaymentRepository.hasConnectionToProduct(id)) {
            log.error("id-You can’t delete the interim and advance payment with ID [%s] because it is connected to the product.".formatted(id));
            throw new ClientException("id-You can’t delete the interim and advance payment with ID [%s] because it is connected to the product.".formatted(id), ErrorCode.CONFLICT);
        }

        if (interimAdvancePaymentRepository.hasConnectionToService(id)) {
            log.error("id-You can’t delete the interim and advance payment with ID [%s] because it is connected to the service.".formatted(id));
            throw new ClientException("id-You can’t delete the interim and advance payment with ID [%s] because it is connected to the service.".formatted(id), ErrorCode.CONFLICT);
        }

        interimAdvancePayment.setStatus(InterimAdvancePaymentStatus.DELETED);
        interimAdvancePaymentRepository.save(interimAdvancePayment);

        return interimAdvancePayment.getId();
    }


    /**
     * Edit Interim Advance Payment {@link InterimAdvancePayment} and its sub-objects with provided id
     * according to received data through request {@link EditInterimAdvancePaymentRequest}
     *
     * @param id      - id of Interim Advance payment which is being edited
     * @param request - contains data required to edit Interim Advance Payment {@link EditInterimAdvancePaymentRequest}, {@link InterimAdvancePayment}
     * @return Response which contains complete data about edited Interim Advance Payment {@link InterimAdvancePaymentResponse}
     * @throws DomainEntityNotFoundException if ACTIVE Interim Advance Payment can't be found by provided id
     * @throws ClientException               if currently authenticated user does not have enough permissions to edit locked Interim Advance Payments
     * @throws ClientException               if any error happened during editing
     */
    @Transactional
    public Long edit(Long id, EditInterimAdvancePaymentRequest request) {
        StringBuilder errorMessage = new StringBuilder();

        InterimAdvancePayment interimAdvancePayment = interimAdvancePaymentRepository.findByIdAndStatusIn(id, List.of(InterimAdvancePaymentStatus.ACTIVE))
                .orElseThrow(() -> new DomainEntityNotFoundException("Interim and advance payment not found by id: " + id + ";"));

        checkPermissionsToEdit(interimAdvancePayment);
        checkValidationsForIapBoundToGroup(request, interimAdvancePayment, errorMessage);

        interimAdvancePayment = editInterimAdvancePaymentRequest(interimAdvancePayment, request, errorMessage);

        interimAdvancePaymentTermsService.edit(request, interimAdvancePayment, errorMessage);
        interimAdvancePaymentDateOfMonthService.editDateOfMonths(request, interimAdvancePayment, errorMessage);
        interimAdvancePaymentDayWeekPeriodYearService.editDaysOfWeek(request, interimAdvancePayment, errorMessage);
        interimAdvancePaymentIssuingPeriodService.editPeriodsOfYear(request, interimAdvancePayment, errorMessage);

        throwExceptionIfRequired(errorMessage);

        return interimAdvancePayment.getId();
    }

    private void checkValidationsForIapBoundToGroup(EditInterimAdvancePaymentRequest request, InterimAdvancePayment interimAdvancePayment, StringBuilder errorMessage) {
        if (checkIfAdvancedPaymentIsBoundToTheGroup(interimAdvancePayment.getId())) {
            checkValueTypeForEdit(request, errorMessage);
            checkDateOfIssueTypeForEdit(request, errorMessage);
            checkPaymentTermForEdit(request.getInterimAdvancePaymentTerm(), errorMessage);
            if (request.getPaymentType() == null || !request.getPaymentType().equals(PaymentType.OBLIGATORY)) {
                errorMessage.append("paymentType-paymentType should be OBLIGATORY;");
            }
        }
    }

    private void checkPaymentTermForEdit(EditInterimAdvancePaymentTermRequest request, StringBuilder errorMessage) {
        if (request != null) {
            if (request.getValue() == null) {
                errorMessage.append("interimAdvancePaymentTerm.value-interimAdvancePaymentTerm.value should not be null;");
            }
            if (request.getValueFrom() != null) {
                errorMessage.append("interimAdvancePaymentTerm.valueFrom-valueFrom should be null;");
            }
            if (request.getValueTo() != null) {
                errorMessage.append("interimAdvancePaymentTerm.valueFrom-valueTo should be null;");
            }
        }
    }

    private void checkDateOfIssueTypeForEdit(EditInterimAdvancePaymentRequest request, StringBuilder errorMessage) {
        if (request.getDateOfIssueType().equals(DateOfIssueType.WORKING_DAYS_AFTER_INVOICE_DATE) || request.getDateOfIssueType().equals(DateOfIssueType.DATE_OF_THE_MONTH)) {
            if (request.getDateOfIssueValue() == null) {
                errorMessage.append("dateOfIssueValue-dateOfIssueValue should not be null;");
            }
            if (request.getDateOfIssueValueFrom() != null) {
                errorMessage.append("dateOfIssueValueFrom-dateOfIssueValueFrom should be null;");
            }
            if (request.getDateOfIssueValueTo() != null) {
                errorMessage.append("dateOfIssueValueTo-dateOfIssueValueTo should be null;");
            }
        }
    }

    private void checkValueTypeForEdit(EditInterimAdvancePaymentRequest request, StringBuilder errorMessage) {
        if (request.getValueType().equals(ValueType.EXACT_AMOUNT) || request.getValueType().equals(ValueType.PERCENT_FROM_PREVIOUS_INVOICE_AMOUNT)) {
            if (request.getValue() == null) {
                errorMessage.append("value-value should not be null;");
            }
            if (request.getValueFrom() != null) {
                errorMessage.append("valueFrom-valueFrom should be null;");
            }
            if (request.getValueTo() != null) {
                errorMessage.append("valueTo-valueTo should be null;");
            }
        } else if (request.getValueType().equals(ValueType.PRICE_COMPONENT)) {
            PriceComponent priceComponent = priceComponentRepository.findByIdAndStatusIn(request.getPriceComponentId(), List.of(PriceComponentStatus.ACTIVE))
                    .orElseThrow(() -> new DomainEntityNotFoundException("priceComponentId-Price Component not found by id: " + request.getPriceComponentId()));
            List<PriceComponentFormulaVariable> formulaVariables = priceComponent.getFormulaVariables().stream().filter(priceComponentFormulaVariable -> priceComponentFormulaVariable.getVariable().name().startsWith("X"))
                    .toList();
            if (!formulaVariables.isEmpty()) {
                int index = 0;
                for (PriceComponentFormulaVariable variable : formulaVariables) {
                    if (variable.getValue() == null) {
                        errorMessage.append("formulaVariable[%s]-value should not be null;".formatted(index));
                    }
                    if (variable.getValueFrom() != null) {
                        errorMessage.append("formulaVariable[%s]-valueFrom should be null;".formatted(index));
                    }
                    if (variable.getValueTo() != null) {
                        errorMessage.append("formulaVariable[%s]-valueTo should be null;".formatted(index));
                    }
                    index++;
                }
            }
        }
    }

    public boolean checkIfAdvancedPaymentIsBoundToTheGroup(Long advancedPaymentIds) {
        List<AdvancedPaymentGroupAdvancedPayments> advancedPaymentGroupAdvancedPayments =
                advancedPaymentGroupAdvancedPaymentsRepository.findByAdvancePaymentIdAndStatus(advancedPaymentIds, AdvancedPaymentGroupStatus.ACTIVE);
        return !CollectionUtils.isEmpty(advancedPaymentGroupAdvancedPayments);
    }

    /**
     * Edit and persist provided Interim Advance Payment {@link InterimAdvancePayment} based on received request data {@link EditInterimAdvancePaymentRequest},
     * save message if any error happened
     *
     * @param interimAdvancePayment Interim Advance payment which is being edited {@link InterimAdvancePayment}
     * @param request               - used to edit Interim Advance Payment  {@link EditInterimAdvancePaymentRequest}
     * @param errorMessage          - used to store messages of errors
     * @return Edited Interim Advance Payment {@link InterimAdvancePayment}
     */
    private InterimAdvancePayment editInterimAdvancePaymentRequest(InterimAdvancePayment interimAdvancePayment, EditInterimAdvancePaymentRequest request, StringBuilder errorMessage) {
        interimAdvancePayment.setName(request.getName());
        interimAdvancePayment.setValueType(request.getValueType());
        interimAdvancePayment.setValue(request.getValue());
        interimAdvancePayment.setValueFrom(request.getValueFrom());
        interimAdvancePayment.setValueTo(request.getValueTo());

        Currency requestedCurrency = getCurrency(request.getCurrencyId(), List.of(NomenclatureItemStatus.ACTIVE, NomenclatureItemStatus.INACTIVE), errorMessage);
        if (requestedCurrency != null) {
            Currency currency = interimAdvancePayment.getCurrency();
            if (currency != null) {
                NomenclatureItemStatus requestedCurrencyStatus = requestedCurrency.getStatus();

                if (requestedCurrencyStatus.equals(NomenclatureItemStatus.INACTIVE)) {
                    if (!Objects.equals(requestedCurrency.getId(), currency.getId())) {
                        errorMessage.append("currencyId-You cannot assign INACTIVE currency to interim advance payment;");
                    } else {
                        interimAdvancePayment.setCurrency(requestedCurrency);
                    }
                } else {
                    interimAdvancePayment.setCurrency(requestedCurrency);
                }
            } else {
                interimAdvancePayment.setCurrency(requestedCurrency);
            }
        }

        interimAdvancePayment.setDateOfIssueType(request.getDateOfIssueType());
        interimAdvancePayment.setDateOfIssueValue(request.getDateOfIssueValue());
        interimAdvancePayment.setDateOfIssueValueFrom(request.getDateOfIssueValueFrom());
        interimAdvancePayment.setDateOfIssueValueTo(request.getDateOfIssueValueTo());
        interimAdvancePayment.setPriceComponent(getPriceComponent(request.getPriceComponentId(), errorMessage));
        interimAdvancePayment.setPaymentType(request.getPaymentType());
        interimAdvancePayment.setMatchTermOfStandardInvoice(request.getMatchesWithTermOfStandardInvoice());
        interimAdvancePayment.setNoInterestOnOverdueDebts(request.getNoInterestInOverdueDebt());
        editPeriodTypeAndYearRound(request, interimAdvancePayment);
        interimAdvancePayment.setIssuingForTheMonthToCurrent(request.getIssuingForTheMonthToCurrent());
        interimAdvancePayment.setDeductionFrom(request.getDeductionFrom());
        return interimAdvancePaymentRepository.save(interimAdvancePayment);
    }

    /**
     * Check if request contains Periods data {@link EditDayOfWeekAndPeriodOfYearAndDateOfMonthRequest}
     * and set period Type to Interim Advance Payment {@link InterimAdvancePayment}.
     * Check if field "Year Round" is provided and set it to Interim Advance Payment {@link InterimAdvancePayment},
     * if it is null set false by default
     *
     * @param request               - used to edit Interim Advance Payment {@link InterimAdvancePayment}
     * @param interimAdvancePayment - Current state of Interim Advance Payment model {@link InterimAdvancePayment}
     */
    private void editPeriodTypeAndYearRound(EditInterimAdvancePaymentRequest request, InterimAdvancePayment interimAdvancePayment) {
        if (request.getDayOfWeekAndPeriodOfYearAndDateOfMonth() != null) {
            interimAdvancePayment.setPeriodType(request.getDayOfWeekAndPeriodOfYearAndDateOfMonth().getPeriodType());
            if (request.getDayOfWeekAndPeriodOfYearAndDateOfMonth().getDayOfWeekAndPeriodOfYear() != null) {
                interimAdvancePayment.setYearRound(request.getDayOfWeekAndPeriodOfYearAndDateOfMonth().getDayOfWeekAndPeriodOfYear().getYearRound());
            } else interimAdvancePayment.setYearRound(false);
        } else interimAdvancePayment.setYearRound(false);
    }

    /**
     * Check permissions of currently authenticated user
     * and if Interim Advance Payment is connected to group but user does not have respective permission throw exception
     *
     * @param interimAdvancePayment Interim Advance Payment which is being edited
     * @throws ClientException if model is connected to group and user does not have INTERIM_ADVANCE_PAYMENT_EDIT_LOCKED permission
     */
    private void checkPermissionsToEdit(InterimAdvancePayment interimAdvancePayment) {
        if (interimAdvancePaymentRepository.hasLockedConnection(interimAdvancePayment.getId())) {
            List<String> context = permissionService.getPermissionsFromContext(PermissionContextEnum.INTERIM_ADVANCE_PAYMENT);
            if (!context.contains(INTERIM_ADVANCE_PAYMENT_EDIT_LOCKED.getId()))
                throw new OperationNotAllowedException("You can't edit price component because it is connected to the product contract, service contract or service order!;");
        }
    }

    /**
     * Return Page of Interim Advance Payments Responses {@link InterimAdvancePaymentListResponse}
     * filtered and ordered according to received request {@link InterimAdvancePaymentListingRequest}
     *
     * @param request - used to filter and order interim advance payments {@link InterimAdvancePayment}
     * @return Page of Interim Advance Payments List Responses {@link InterimAdvancePaymentListResponse}
     */
    public Page<InterimAdvancePaymentListResponse> list(InterimAdvancePaymentListingRequest request) {
        Sort.Order order = new Sort.Order(getSortDirection(request.getSortDirection()), getSortField(request.getSortBy()));
        Pageable pageable = PageRequest.of(request.getPage(), request.getSize(), Sort.by(order));
        List<String> valueTypes = request.getValueType() == null || request.getValueType().isEmpty() ? new ArrayList<>() :
                request.getValueType().stream().map(Enum::name).collect(Collectors.toList());
        return interimAdvancePaymentRepository.filter(
                EPBStringUtils.fromPromptToQueryParameter(request.getPrompt()),
                getSearchField(request.getPromptBy()),
                getStatusesAccordingPermissions().stream().map(InterimAdvancePaymentStatus::name).toList(),
                valueTypes,
                request.getDeductionFrom() == null ? null : request.getDeductionFrom().name(),
                request.getAvailability() == InterimAdvancePaymentAvailability.ALL ? null : request.getAvailability().toString(),
                pageable
        );
    }

    /**
     * If Search field is provided return it, else return "ALL" by default
     *
     * @param searchField provided search field
     * @return Name of search field.
     */
    private String getSearchField(InterimAdvancePaymentSearchField searchField) {
        if (searchField == null) return TerminationSearchFields.ALL.getValue();
        return searchField.getValue();
    }

    /**
     * Check currently authenticated users permissions and return List of Interim Advance Payments Statuses accordingly
     *
     * @return List of Interim Advance Payment Statuses
     */
    private List<InterimAdvancePaymentStatus> getStatusesAccordingPermissions() {
        List<InterimAdvancePaymentStatus> statuses = new ArrayList<>();
        List<String> context = permissionService.getPermissionsFromContext(PermissionContextEnum.INTERIM_ADVANCE_PAYMENT);
        if (context.contains(INTERIM_ADVANCE_PAYMENT_VIEW_BASIC.getId()))
            statuses.add(InterimAdvancePaymentStatus.ACTIVE);
        if (context.contains(INTERIM_ADVANCE_PAYMENT_VIEW_DELETED.getId()))
            statuses.add(InterimAdvancePaymentStatus.DELETED);
        return statuses;
    }

    /**
     * If Sord Direction is provided return it, else return "ASC" by default
     *
     * @param sortDirection - provided sort direction
     * @return sort direction
     */
    private Sort.Direction getSortDirection(Sort.Direction sortDirection) {
        if (sortDirection == null) return Sort.Direction.ASC;
        return sortDirection;
    }

    /**
     * If sort field is provided return it, else return "ID" by default
     *
     * @param sortField - provided sort fiels
     * @return Nme of sort field
     */
    private String getSortField(InterimAdvancePaymentSortField sortField) {
        if (sortField == null) return TerminationSortFields.ID.getValue();
        return sortField.getValue();
    }


    private InterimAdvancePayment mapAdvancePayment(InterimAdvancePayment source) {
        return InterimAdvancePayment.builder()
                .name(source.getName())
                .valueType(source.getValueType())
                .value(source.getValue())
                .valueFrom(source.getValueFrom())
                .valueTo(source.getValueTo())
                .currency(checkCurrency(source.getCurrency()))
                .dateOfIssueType(source.getDateOfIssueType())
                .dateOfIssueValue(source.getDateOfIssueValue())
                .dateOfIssueValueFrom(source.getDateOfIssueValueFrom())
                .dateOfIssueValueTo(source.getDateOfIssueValueTo())
                .paymentType(source.getPaymentType())
                .periodType(source.getPeriodType())
                .matchTermOfStandardInvoice(source.getMatchTermOfStandardInvoice())
                .noInterestOnOverdueDebts(source.getNoInterestOnOverdueDebts())
                .yearRound(source.getYearRound())
                .deductionFrom(source.getDeductionFrom())
                .status(InterimAdvancePaymentStatus.ACTIVE)
                .issuingForTheMonthToCurrent(source.getIssuingForTheMonthToCurrent())
                .build();
    }

    private Currency checkCurrency(Currency currency) {
        if (currency == null) {
            return null;
        } else {
            if (!currency.getStatus().equals(NomenclatureItemStatus.DELETED)) {
                return currency;
            } else {
                log.error("Currency is Deleted");
                throw new ClientException("id-Currency is Deleted;", ErrorCode.OPERATION_NOT_ALLOWED);
            }
        }
    }


    /**
     * Find available interim and advance payments for adding to a group of interim and advance payments.
     *
     * @param request request with search parameters
     * @return page with interim and advance payments
     */
    public Page<InterimAdvancePaymentSearchListResponse> findAvailableInterimAndAdvancePayments(SearchInterimAdvancePaymentRequest request) {
        Sort.Order order = new Sort.Order(Sort.Direction.DESC, InterimAdvancePaymentSortField.ID.getValue());
        return interimAdvancePaymentRepository.findAvailableInterimAndAdvancePayments(
                EPBStringUtils.fromPromptToQueryParameter(request.getPrompt()),
                PageRequest.of(request.getPage(), request.getSize(), Sort.by(order))
        );
    }


    /**
     * Retrieves interim and advance payment response for copying after passing validations.
     *
     * @param id id of interim and advance payment to copy
     * @return interim and advance payment response for copying
     */
    @Transactional
    public InterimAdvancePaymentCopyResponse viewForCopy(Long id) {
        log.debug("Viewing for copy interim and advance payment with id: {}", id);

        InterimAdvancePayment source = interimAdvancePaymentRepository
                .findByIdAndStatusIn(id, List.of(InterimAdvancePaymentStatus.ACTIVE))
                .orElseThrow(() -> new DomainEntityNotFoundException("Interim and advance payment not found by id: %s;".formatted(id)));

        // price component
        PriceComponent copiedPriceComponent = null;
        PriceComponent priceComponent = source.getPriceComponent();
        if (priceComponent != null && !priceComponent.getStatus().equals(PriceComponentStatus.DELETED)) {
            // other checks are performed in the service about nomenclatures statuses

            ApplicationModel sourceAppModel = applicationModelRepository
                    .findByPriceComponentIdAndStatusIn(priceComponent.getId(), List.of(ApplicationModelStatus.ACTIVE))
                    .orElseThrow(() -> new DomainEntityNotFoundException("Application model not found for price component id: " + priceComponent.getId()));
            ApplicationModel copiedApplicationModel = priceComponentService.copyApplicationModelWithPriceComponent(sourceAppModel);

            if (copiedApplicationModel != null) {
                // if all "copy" validations are passed, then price component should be copied with all its accompanying entities
                copiedPriceComponent = copiedApplicationModel.getPriceComponent();
            }
        }

        // period configurations
        List<DayOfWeekResponse> dayOfWeekResponses = interimAdvancePaymentDayWeekPeriodYearService
                .findByInterimAdvancePaymentIdAndStatusIn(id, List.of(InterimAdvancePaymentSubObjectStatus.ACTIVE));

        List<PeriodOfYearResponse> periodOfYearResponses = interimAdvancePaymentIssuingPeriodService
                .findByInterimAdvancePaymentIdAndStatusIn(id, List.of(InterimAdvancePaymentSubObjectStatus.ACTIVE));

        List<DateOfMonthResponse> dateOfMonthResponses = interimAdvancePaymentDateOfMonthService
                .findByInterimAdvancePaymentIdAndStatusIn(id, List.of(InterimAdvancePaymentSubObjectStatus.ACTIVE));

        DayOfWeekAndPeriodOfYearAndDateOfMonthResponse periodConfigurationResponse = new DayOfWeekAndPeriodOfYearAndDateOfMonthResponse(
                source.getPeriodType(),
                dayOfWeekResponses,
                source.getYearRound(),
                periodOfYearResponses,
                dateOfMonthResponses
        );

        // payment terms
        InterimAdvancePaymentTermsResponse iapTerm = null;
        Optional<InterimAdvancePaymentTerms> iapTermOptional = interimAdvancePaymentTermsRepository
                .findByInterimAdvancePaymentIdAndStatusInOrderByCreateDate(id, List.of(InterimAdvancePaymentSubObjectStatus.ACTIVE));
        if (iapTermOptional.isPresent()) {
            InterimAdvancePaymentTerms interimAdvancePaymentTerms = iapTermOptional.get();
            if (interimAdvancePaymentTerms.getCalendar().getStatus().equals(NomenclatureItemStatus.ACTIVE)) {
                iapTerm = new InterimAdvancePaymentTermsResponse(interimAdvancePaymentTerms);
                CalendarResponse calendarResponse = iapTerm.getCalendar();
                Optional<Calendar> calendarOptional = calendarRepository.findByIdAndStatusIsIn(calendarResponse.getId(), List.of(NomenclatureItemStatus.ACTIVE));
                if (calendarOptional.isEmpty()) {
                    iapTerm.setCalendar(null);
                }
            }
        }

        return new InterimAdvancePaymentCopyResponse(
                source,
                periodConfigurationResponse,
                iapTerm == null ? null : copyToAdvancePaymentCopyResponse(iapTerm),
                copiedPriceComponent
        );
    }


    private InvoicePaymentTermsCopyResponse copyToAdvancePaymentCopyResponse(InterimAdvancePaymentTermsResponse original) {
        InvoicePaymentTermsCopyResponse build = InvoicePaymentTermsCopyResponse.builder()
                .name(original.getName())
                .calendarType(original.getCalendarType())
                .value(original.getValue())
                .valueFrom(original.getValueFrom())
                .valueTo(original.getValueTo())
                .excludeWeekends(original.getExcludeWeekends())
                .excludeHolidays(original.getExcludeHolidays())
                .dueDateChange(original.getDueDateChange())
                .termId(original.getId())
                .status(original.getStatus())
                .build();
        if (original.getCalendar() != null) {
            build.setCalendar(original.getCalendar());
        }
        return build;
    }


    /**
     * Adds Interim Advance Payment to Service Details
     *
     * @param interimAdvancePaymentIds Interim Advance Payment ids
     * @param serviceDetails           Service Details to add Interim Advance Payment to
     * @param exceptionMessages        List of exception messages to be filled in case of errors
     */
    @Transactional
    public void addInterimAdvancePaymentsToService(List<Long> interimAdvancePaymentIds, ServiceDetails serviceDetails, List<String> exceptionMessages) {
        if (CollectionUtils.isNotEmpty(interimAdvancePaymentIds)) {
            // fetch all available Interim Advance Payments at the moment of adding
            List<Long> availableIAPs = interimAdvancePaymentRepository.findAvailableAdvancePaymentIdsForService(interimAdvancePaymentIds);
            List<ServiceInterimAndAdvancePayment> tempList = new ArrayList<>();

            for (int i = 0; i < interimAdvancePaymentIds.size(); i++) {
                Long iapId = interimAdvancePaymentIds.get(i);
                if (availableIAPs.contains(iapId)) {
                    Optional<InterimAdvancePayment> iapOptional = interimAdvancePaymentRepository.findById(iapId);
                    if (iapOptional.isEmpty()) {
                        log.error("interimAdvancePayments[%s]-can't find Interim Advance Payment with id: %s;".formatted(i, iapId));
                        exceptionMessages.add("basicSettings.interimAdvancePayments[%s]-can't find Interim Advance Payment with id: %s;".formatted(i, iapId));
                        continue;
                    }

                    ServiceInterimAndAdvancePayment siap = new ServiceInterimAndAdvancePayment();
                    siap.setInterimAndAdvancePayment(iapOptional.get());
                    siap.setServiceDetails(serviceDetails);
                    siap.setStatus(ServiceSubobjectStatus.ACTIVE);
                    tempList.add(siap);
                } else {
                    log.error("interimAdvancePayments[%s]-Interim Advance Payment with id: %s is not available for adding;".formatted(i, iapId));
                    exceptionMessages.add("basicSettings.interimAdvancePayments[%s]-Interim Advance Payment with id: %s is not available for adding;".formatted(i, iapId));
                }
            }

            // if there are any errors - return without saving
            if (CollectionUtils.isNotEmpty(exceptionMessages)) {
                return;
            }

            // save all Interim Advance Payments
            serviceInterimAndAdvancePaymentRepository.saveAll(tempList);
        }
    }


    /**
     * Edits Interim Advance Payments for Service Details existing version
     *
     * @param requestIAPs       Interim Advance Payment ids
     * @param serviceDetails    Service Details to add Interim Advance Payment to
     * @param exceptionMessages List of exception messages to be filled in case of errors
     */
    @Transactional
    public void updateServiceIAPsForExistingVersion(List<Long> requestIAPs, ServiceDetails serviceDetails, List<String> exceptionMessages) {
        // fetch all active dbIAPs
        List<ServiceInterimAndAdvancePayment> dbIAPs = serviceInterimAndAdvancePaymentRepository
                .findByServiceDetailsIdAndStatusIn(serviceDetails.getId(), List.of(ServiceSubobjectStatus.ACTIVE));

        if (CollectionUtils.isNotEmpty(requestIAPs)) {
            List<Long> dbIAPIds = dbIAPs.stream().map(siap -> siap.getInterimAndAdvancePayment().getId()).toList();

            // fetch all available Interim Advance Payments at the moment of adding
            List<Long> availableIAPs = interimAdvancePaymentRepository.findAvailableAdvancePaymentIdsForService(requestIAPs);
            List<ServiceInterimAndAdvancePayment> tempList = new ArrayList<>();

            for (int i = 0; i < requestIAPs.size(); i++) {
                Long iapId = requestIAPs.get(i);
                if (!dbIAPIds.contains(iapId)) { // if iap is new, its availability should be checked
                    if (availableIAPs.contains(iapId)) {
                        Optional<InterimAdvancePayment> iapOptional = interimAdvancePaymentRepository.findById(iapId);
                        if (iapOptional.isEmpty()) {
                            log.error("interimAdvancePayments[%s]-can't find Interim Advance Payment with id: %s;".formatted(i, iapId));
                            exceptionMessages.add("basicSettings.interimAdvancePayments[%s]-can't find Interim Advance Payment with id: %s;".formatted(i, iapId));
                            continue;
                        }

                        ServiceInterimAndAdvancePayment siap = new ServiceInterimAndAdvancePayment();
                        siap.setInterimAndAdvancePayment(iapOptional.get());
                        siap.setServiceDetails(serviceDetails);
                        siap.setStatus(ServiceSubobjectStatus.ACTIVE);
                        tempList.add(siap);
                    } else {
                        log.error("interimAdvancePayments[%s]-Interim Advance Payment with id: %s is not available for adding;".formatted(i, iapId));
                        exceptionMessages.add("basicSettings.interimAdvancePayments[%s]-Interim Advance Payment with id: %s is not available for adding;".formatted(i, iapId));
                    }
                }
            }

            // if validations failed, don't save anything
            if (CollectionUtils.isNotEmpty(exceptionMessages)) {
                return;
            }

            // save all new Interim Advance Payments
            serviceInterimAndAdvancePaymentRepository.saveAll(tempList);
        }

        if (CollectionUtils.isNotEmpty(dbIAPs)) {
            for (ServiceInterimAndAdvancePayment siap : dbIAPs) {
                // if user has removed iaps, set DELETED status
                if (!requestIAPs.contains(siap.getInterimAndAdvancePayment().getId())) {
                    siap.setStatus(ServiceSubobjectStatus.DELETED);
                    serviceInterimAndAdvancePaymentRepository.save(siap);
                }
            }
        }
    }


    /**
     * Edits Interim Advance Payments for Service Details new version
     *
     * @param requestIAPs          Interim Advance Payment ids
     * @param updatedServiceDetail Service Details to add Interim Advance Payment to
     * @param sourceServiceDetail  source service details (version from which new version was created)
     * @param exceptionMessages    List of exception messages to be filled in case of errors
     */
    @Transactional
    public void updateServiceIAPsForNewVersion(List<Long> requestIAPs,
                                               ServiceDetails updatedServiceDetail,
                                               ServiceDetails sourceServiceDetail,
                                               List<String> exceptionMessages) {
        if (CollectionUtils.isNotEmpty(requestIAPs)) {
            // fetch all active dbIAPs from source version
            List<ServiceInterimAndAdvancePayment> dbIAPs = serviceInterimAndAdvancePaymentRepository
                    .findByServiceDetailsIdAndStatusIn(sourceServiceDetail.getId(), List.of(ServiceSubobjectStatus.ACTIVE));

            List<Long> dbIAPIds = dbIAPs.stream().map(siap -> siap.getInterimAndAdvancePayment().getId()).toList();

            // fetch all available Interim Advance Payments at the moment of adding
            List<Long> availableIAPs = interimAdvancePaymentRepository.findAvailableAdvancePaymentIdsForService(requestIAPs);
            List<InterimAdvancePayment> tempList = new ArrayList<>();

            for (int i = 0; i < requestIAPs.size(); i++) {
                Long iapId = requestIAPs.get(i);
                if (dbIAPIds.contains(iapId)) { // if iap is from the source version, it should be cloned
                    InterimAdvancePayment sourceIAP = dbIAPs.stream()
                            .filter(iap -> iap.getInterimAndAdvancePayment().getId().equals(iapId))
                            .findFirst().get().getInterimAndAdvancePayment(); // will always be present, as we have collected the list above
                    InterimAdvancePayment cloned = cloneInterimAdvancePayment(sourceIAP.getId());
                    tempList.add(cloned);
                } else {
                    if (availableIAPs.contains(iapId)) { // if iap is new, its availability should be checked
                        Optional<InterimAdvancePayment> iapOptional = interimAdvancePaymentRepository.findById(iapId);
                        if (iapOptional.isEmpty()) {
                            log.error("interimAdvancePayments[%s]-can't find Interim Advance Payment with id: %s;".formatted(i, iapId));
                            exceptionMessages.add("basicSettings.interimAdvancePayments[%s]-can't find Interim Advance Payment with id: %s;".formatted(i, iapId));
                            continue;
                        }

                        tempList.add(iapOptional.get());
                    } else {
                        log.error("interimAdvancePayments[%s]-Interim Advance Payment with id: %s is not available for adding;".formatted(i, iapId));
                        exceptionMessages.add("basicSettings.interimAdvancePayments[%s]-Interim Advance Payment with id: %s is not available for adding;".formatted(i, iapId));
                    }
                }
            }

            // if validations failed, don't save anything
            if (CollectionUtils.isNotEmpty(exceptionMessages)) {
                return;
            }

            // save all new Interim Advance Payments
            for (InterimAdvancePayment item : tempList) {
                ServiceInterimAndAdvancePayment serviceInterimAndAdvancePayment = new ServiceInterimAndAdvancePayment();
                serviceInterimAndAdvancePayment.setInterimAndAdvancePayment(item);
                serviceInterimAndAdvancePayment.setServiceDetails(updatedServiceDetail);
                serviceInterimAndAdvancePayment.setStatus(ServiceSubobjectStatus.ACTIVE);
                serviceInterimAndAdvancePaymentRepository.save(serviceInterimAndAdvancePayment);
            }
        }
    }

    /**
     * Adds Interim Advance Payment to Product Details
     *
     * @param interimAdvancePaymentSet Interim Advance Payment ids
     * @param productDetails           Product Details to add Interim Advance Payment to
     * @param exceptionMessages        List of exception messages to be filled in case of errors
     */
    @Transactional
    public void addInterimAdvancePaymentsToProduct(List<Long> interimAdvancePaymentSet, ProductDetails productDetails, List<String> exceptionMessages) {
        if (CollectionUtils.isNotEmpty(interimAdvancePaymentSet)) {
            List<Long> interimAdvancePaymentIds = new ArrayList<>(interimAdvancePaymentSet); // this is for the sake of getting index of element when handling errors

            // fetch all available Interim Advance Payments at the moment of adding
            List<Long> availableIAPs = interimAdvancePaymentRepository.findAvailableAdvancePaymentIdsForProduct(interimAdvancePaymentIds);
            List<ProductInterimAndAdvancePayments> tempList = new ArrayList<>();

            for (int i = 0; i < interimAdvancePaymentIds.size(); i++) {
                Long iapId = interimAdvancePaymentIds.get(i);
                Optional<InterimAdvancePayment> iapOptional = interimAdvancePaymentRepository.findById(iapId);
                if (iapOptional.isEmpty()) {
                    log.error("interimAdvancePayments[%s]-can't find Interim Advance Payment with id: %s;".formatted(i, iapId));
                    exceptionMessages.add("basicSettings.interimAdvancePayments[%s]-can't find Interim Advance Payment with id: %s;".formatted(i, iapId));
                    continue;
                }
                if (availableIAPs.contains(iapId)) {
                    ProductInterimAndAdvancePayments siap = new ProductInterimAndAdvancePayments();
                    siap.setInterimAdvancePayment(iapOptional.get());
                    siap.setProductDetails(productDetails);
                    siap.setProductSubObjectStatus(ProductSubObjectStatus.ACTIVE);
                    tempList.add(siap);
                } else {
                    log.error("interimAdvancePayments[%s]-Interim Advance Payment with id: %s is not available for adding;".formatted(i, iapId));
                    exceptionMessages.add("basicSettings.interimAdvancePayments[%s]-Interim Advance Payment with id: %s is not available for adding;".formatted(i, iapId));
                }
            }

            // if there are any errors - return without saving
            if (CollectionUtils.isNotEmpty(exceptionMessages)) {
                return;
            }

            // save all Interim Advance Payments
            productInterimAndAdvancePaymentRepository.saveAll(tempList);
        }
    }


    /**
     * Edits Interim Advance Payments for Product Details existing version
     *
     * @param requestIAPSet     Interim Advance Payment ids
     * @param productDetails    Product Details to add Interim Advance Payment to
     * @param exceptionMessages List of exception messages to be filled in case of errors
     */
    @Transactional
    public void updateProductIAPsForExistingVersion(List<Long> requestIAPSet, ProductDetails productDetails, List<String> exceptionMessages) {
        // fetch all active dbIAPs
        List<ProductInterimAndAdvancePayments> dbIAPs = productInterimAndAdvancePaymentRepository
                .findByProductDetailsIdAndProductSubObjectStatusIn(productDetails.getId(), List.of(ProductSubObjectStatus.ACTIVE));

        if (CollectionUtils.isNotEmpty(requestIAPSet)) {
            List<Long> requestIAPs = new ArrayList<>(requestIAPSet); // this is for the sake of getting index of element when handling errors

            List<Long> dbIAPIds = dbIAPs.stream().map(piap -> piap.getInterimAdvancePayment().getId()).toList();

            // fetch all available Interim Advance Payments at the moment of adding
            List<Long> availableIAPs = interimAdvancePaymentRepository.findAvailableAdvancePaymentIdsForProduct(requestIAPs);
            List<ProductInterimAndAdvancePayments> tempList = new ArrayList<>();

            for (int i = 0; i < requestIAPs.size(); i++) {
                Long iapId = requestIAPs.get(i);
                Optional<InterimAdvancePayment> iapOptional = interimAdvancePaymentRepository.findById(iapId);
                if (iapOptional.isEmpty()) {
                    log.error("interimAdvancePayments[%s]-can't find Interim Advance Payment with id: %s;".formatted(i, iapId));
                    exceptionMessages.add("basicSettings.interimAdvancePayments[%s]-can't find Interim Advance Payment with id: %s;".formatted(i, iapId));
                    continue;
                }
                if (!dbIAPIds.contains(iapId)) { // if iap is new, its availability should be checked
                    if (availableIAPs.contains(iapId)) {
                        ProductInterimAndAdvancePayments siap = new ProductInterimAndAdvancePayments();
                        siap.setInterimAdvancePayment(iapOptional.get());
                        siap.setProductDetails(productDetails);
                        siap.setProductSubObjectStatus(ProductSubObjectStatus.ACTIVE);
                        tempList.add(siap);
                    } else {
                        log.error("interimAdvancePayments[%s]-Interim Advance Payment with id: %s is not available for adding;".formatted(i, iapId));
                        exceptionMessages.add("basicSettings.interimAdvancePayments[%s]-Interim Advance Payment with id: %s is not available for adding;".formatted(i, iapId));
                    }
                }
            }

            // if validations failed, don't save anything
            if (CollectionUtils.isNotEmpty(exceptionMessages)) {
                return;
            }

            // save all new Interim Advance Payments
            productInterimAndAdvancePaymentRepository.saveAll(tempList);
        }

        if (CollectionUtils.isNotEmpty(dbIAPs)) {
            for (ProductInterimAndAdvancePayments piap : dbIAPs) {
                // if user has removed iaps, set DELETED status
                if (!requestIAPSet.contains(piap.getInterimAdvancePayment().getId())) {
                    piap.setProductSubObjectStatus(ProductSubObjectStatus.DELETED);
                    productInterimAndAdvancePaymentRepository.save(piap);
                }
            }
        }
    }


    /**
     * Edits Interim Advance Payments for Product Details new version
     *
     * @param requestIAPSet        Interim Advance Payment ids
     * @param updatedProductDetail Product Details to add Interim Advance Payment to
     * @param sourceProductDetail  source product details (version from which new version was created)
     * @param exceptionMessages    List of exception messages to be filled in case of errors
     */
    @Transactional
    public void updateProductIAPsForNewVersion(List<Long> requestIAPSet,
                                               ProductDetails updatedProductDetail,
                                               ProductDetails sourceProductDetail,
                                               List<String> exceptionMessages) {
        if (CollectionUtils.isNotEmpty(requestIAPSet)) {
            List<Long> requestIAPs = new ArrayList<>(requestIAPSet); // this is for the sake of getting index of element when handling errors

            // fetch all active dbIAPs from source version
            List<ProductInterimAndAdvancePayments> dbIAPs = productInterimAndAdvancePaymentRepository
                    .findByProductDetailsIdAndProductSubObjectStatusIn(sourceProductDetail.getId(), List.of(ProductSubObjectStatus.ACTIVE));

            List<Long> dbIAPIds = dbIAPs.stream().map(siap -> siap.getInterimAdvancePayment().getId()).toList();

            // fetch all available Interim Advance Payments at the moment of adding
            List<Long> availableIAPs = interimAdvancePaymentRepository.findAvailableAdvancePaymentIdsForProduct(requestIAPs);
            List<InterimAdvancePayment> tempList = new ArrayList<>();

            for (int i = 0; i < requestIAPs.size(); i++) {
                Long iapId = requestIAPs.get(i);
                if (dbIAPIds.contains(iapId)) { // if iap is from the source version, it should be cloned
                    InterimAdvancePayment sourceIAP = dbIAPs.stream()
                            .filter(iap -> iap.getInterimAdvancePayment().getId().equals(iapId))
                            .findFirst().get().getInterimAdvancePayment(); // will always be present, as we have collected the list above
                    InterimAdvancePayment cloned = cloneInterimAdvancePayment(sourceIAP.getId());
                    tempList.add(cloned);
                } else {
                    Optional<InterimAdvancePayment> iapOptional = interimAdvancePaymentRepository.findById(iapId);
                    if (iapOptional.isEmpty()) {
                        log.error("interimAdvancePayments[%s]-can't find Interim Advance Payment with id: %s;".formatted(i, iapId));
                        exceptionMessages.add("basicSettings.interimAdvancePayments[%s]-can't find Interim Advance Payment with id: %s;".formatted(i, iapId));
                        continue;
                    }
                    if (availableIAPs.contains(iapId)) { // if iap is new, its availability should be checked
                        tempList.add(iapOptional.get());
                    } else {
                        log.error("interimAdvancePayments[%s]-Interim Advance Payment with id: %s is not available for adding;".formatted(i, iapId));
                        exceptionMessages.add("basicSettings.interimAdvancePayments[%s]-Interim Advance Payment with id: %s is not available for adding;".formatted(i, iapId));
                    }
                }
            }

            // if validations failed, don't save anything
            if (CollectionUtils.isNotEmpty(exceptionMessages)) {
                return;
            }

            // save all new Interim Advance Payments
            for (InterimAdvancePayment item : tempList) {
                ProductInterimAndAdvancePayments productInterimAndAdvancePayment = new ProductInterimAndAdvancePayments();
                productInterimAndAdvancePayment.setInterimAdvancePayment(item);
                productInterimAndAdvancePayment.setProductDetails(updatedProductDetail);
                productInterimAndAdvancePayment.setProductSubObjectStatus(ProductSubObjectStatus.ACTIVE);
                productInterimAndAdvancePaymentRepository.save(productInterimAndAdvancePayment);
            }
        }
    }

    public List<InterimAdvancePayment> copyIAPForSubObjects(List<InterimAdvancePayment> interimAdvancePayments) {
        return copyAdvancePayments(interimAdvancePayments);
    }


    /**
     * Clones an interim advance payment and returns the clone. Cloned interim advance payment is always active and available.
     * This is one step of interim advance payment cloning process. Cloning interim advance payment's period settings should be performed separately.
     *
     * @param interimAdvancePaymentId the interim advance payment ID to be cloned
     * @return cloned interim advance payment
     */
    @Transactional
    public InterimAdvancePayment cloneInterimAdvancePayment(Long interimAdvancePaymentId) {
        InterimAdvancePayment source = interimAdvancePaymentRepository
                .findById(interimAdvancePaymentId)
                .orElseThrow(() -> new DomainEntityNotFoundException("id-Unable to find Advanced Payment with Id: %s".formatted(interimAdvancePaymentId)));

        InterimAdvancePayment clonedAdvancePayment = mapAdvancePayment(source);

        // if price component is attached to the source, it should be cloned as well
        if (source.getPriceComponent() != null) {
            clonedAdvancePayment.setPriceComponent(priceComponentService.clonePriceComponent(source.getPriceComponent().getId()));
        }

        clonedAdvancePayment = interimAdvancePaymentRepository.saveAndFlush(clonedAdvancePayment);

        // this term becomes optional, when certain checkbox is checked, otherwise mandatory (and if present, only 1 will be present)
        Optional<InterimAdvancePaymentTerms> optionalPaymentTerm = interimAdvancePaymentTermsRepository
                .findByInterimAdvancePaymentIdAndStatusInOrderByCreateDate(interimAdvancePaymentId, List.of(InterimAdvancePaymentSubObjectStatus.ACTIVE));
        if (optionalPaymentTerm.isPresent()) {
            cloneInterimAdvancePaymentTerm(optionalPaymentTerm.get(), clonedAdvancePayment);
        }

        // clone the rest of the settings
        List<InterimAdvancePaymentDayWeekPeriodYear> dayOfWeek = interimAdvancePaymentDayWeekPeriodYearService
                .findByInterimAdvancePaymentIdAndStatusInEntity(source.getId(), List.of(InterimAdvancePaymentSubObjectStatus.ACTIVE));

        List<InterimAdvancePaymentIssuingPeriod> periodOfYear = interimAdvancePaymentIssuingPeriodService
                .findByInterimAdvancePaymentIdAndStatusInEntity(source.getId(), List.of(InterimAdvancePaymentSubObjectStatus.ACTIVE));

        List<InterimAdvancePaymentDateOfMonth> dateOfMonth = interimAdvancePaymentDateOfMonthService
                .findByInterimAdvancePaymentIdAndStatusInEntity(source.getId(), List.of(InterimAdvancePaymentSubObjectStatus.ACTIVE));

        if (CollectionUtils.isNotEmpty(dayOfWeek)) {
            createDayOfWeek(dayOfWeek, clonedAdvancePayment);
        }

        if (CollectionUtils.isNotEmpty(periodOfYear)) {
            createPeriodOfYear(periodOfYear, clonedAdvancePayment);
        }

        if (CollectionUtils.isNotEmpty(dateOfMonth)) {
            createDateOfMonth(dateOfMonth, clonedAdvancePayment);
        }

        return clonedAdvancePayment;
    }


    /**
     * Clones interim advance payment term
     *
     * @param source term to be cloned
     * @param iap    owner of the cloned term
     */
    private void cloneInterimAdvancePaymentTerm(InterimAdvancePaymentTerms source, InterimAdvancePayment iap) {
        InterimAdvancePaymentTerms term = new InterimAdvancePaymentTerms();
        term.setCalendarType(source.getCalendarType());
        term.setValue(source.getValue());
        term.setValueFrom(source.getValueFrom());
        term.setValueTo(source.getValueTo());
        term.setCalendar(source.getCalendar());
        term.setExcludeWeekends(source.getExcludeWeekends());
        term.setInterimAdvancePayment(iap);
        term.setStatus(InterimAdvancePaymentSubObjectStatus.ACTIVE);
        term.setExcludeHolidays(source.getExcludeHolidays());
        term.setDueDateChange(source.getDueDateChange());
        term.setName(source.getName());
        interimAdvancePaymentTermsRepository.save(term);
    }


    /**
     * Clones interim advance payment date of month settings
     *
     * @param dateOfMonthList      list of date of month settings to be cloned
     * @param clonedAdvancePayment owner of the cloned settings
     */
    private void createDateOfMonth(List<InterimAdvancePaymentDateOfMonth> dateOfMonthList, InterimAdvancePayment clonedAdvancePayment) {
        List<InterimAdvancePaymentDateOfMonth> dateOfMonthsToSave = new ArrayList<>();
        for (InterimAdvancePaymentDateOfMonth item : dateOfMonthList) {
            InterimAdvancePaymentDateOfMonth newItem = InterimAdvancePaymentDateOfMonth.builder()
                    .interimAdvancePayment(clonedAdvancePayment)
                    .month(item.getMonth())
                    .monthNumbers(item.getMonthNumbers())
                    .status(InterimAdvancePaymentSubObjectStatus.ACTIVE)
                    .build();
            dateOfMonthsToSave.add(newItem);
        }
        interimAdvancePaymentDateOfMonthService.saveAll(dateOfMonthsToSave);
    }


    /**
     * Clones interim advance payment period of year settings
     *
     * @param periodOfYearLIst     list of period of year settings to be cloned
     * @param clonedAdvancePayment owner of the cloned settings
     */
    private void createPeriodOfYear(List<InterimAdvancePaymentIssuingPeriod> periodOfYearLIst, InterimAdvancePayment clonedAdvancePayment) {
        List<InterimAdvancePaymentIssuingPeriod> periodOfYearToSave = new ArrayList<>();
        for (InterimAdvancePaymentIssuingPeriod item : periodOfYearLIst) {
            InterimAdvancePaymentIssuingPeriod newItem = InterimAdvancePaymentIssuingPeriod.builder()
                    .interimAdvancePayment(clonedAdvancePayment)
                    .periodFrom(item.getPeriodFrom())
                    .periodTo(item.getPeriodTo())
                    .status(InterimAdvancePaymentSubObjectStatus.ACTIVE)
                    .build();
            periodOfYearToSave.add(newItem);
        }
        interimAdvancePaymentIssuingPeriodService.saveAll(periodOfYearToSave);
    }


    /**
     * Clones interim advance payment day of week settings
     *
     * @param dayOfWeekList        list of day of week settings to be cloned
     * @param clonedAdvancePayment owner of the cloned settings
     */
    private void createDayOfWeek(List<InterimAdvancePaymentDayWeekPeriodYear> dayOfWeekList, InterimAdvancePayment clonedAdvancePayment) {
        List<InterimAdvancePaymentDayWeekPeriodYear> dayOfWeekToSave = new ArrayList<>();
        for (InterimAdvancePaymentDayWeekPeriodYear item : dayOfWeekList) {
            InterimAdvancePaymentDayWeekPeriodYear newItem = InterimAdvancePaymentDayWeekPeriodYear.builder()
                    .interimAdvancePayment(clonedAdvancePayment)
                    .week(item.getWeek())
                    .days(item.getDays())
                    .status(InterimAdvancePaymentSubObjectStatus.ACTIVE)
                    .build();
            dayOfWeekToSave.add(newItem);
        }
        interimAdvancePaymentDayWeekPeriodYearService.saveAll(dayOfWeekToSave);
    }

    public List<AdvancedPaymentSimpleInfoResponse> copyAdvancePaymentsWithResponse(List<InterimAdvancePayment> advancePayments) {
        List<InterimAdvancePayment> saved = copyAdvancePayments(advancePayments);

        List<AdvancedPaymentSimpleInfoResponse> advancePaymentsToReturn = new ArrayList<>();
        for (InterimAdvancePayment item : saved) {
            AdvancedPaymentSimpleInfoResponse advancedPaymentSimpleInfoResponse = new AdvancedPaymentSimpleInfoResponse(item.getId(), item.getName());
            advancePaymentsToReturn.add(advancedPaymentSimpleInfoResponse);
        }

        return advancePaymentsToReturn;
    }

    public List<InterimAdvancePayment> copyAdvancePayments(List<InterimAdvancePayment> advancePayments) {
        List<String> errorMessages = new ArrayList<>();
        List<InterimAdvancePayment> advancePaymentsToCopy = new ArrayList<>();

        for (InterimAdvancePayment item : advancePayments) {
            if (!checkCurrencyForCopy(item.getCurrency())) {
                break;
            }

            PriceComponent priceComponent = null;
            if (item.getPriceComponent() != null && item.getPriceComponent().getStatus().equals(PriceComponentStatus.ACTIVE)) {
                // if price component is present in iap, it should be copied

                PriceComponent sourcePriceComponent = item.getPriceComponent();
                ApplicationModel sourceAppModel = applicationModelRepository
                        .findByPriceComponentIdAndStatusIn(sourcePriceComponent.getId(), List.of(ApplicationModelStatus.ACTIVE))
                        .orElseThrow(() -> new DomainEntityNotFoundException("Application model not found for price component id: " + sourcePriceComponent.getId()));
                ApplicationModel copiedApplicationModel = priceComponentService.copyApplicationModelWithPriceComponent(sourceAppModel);

                // if copying an iap's price component returns with null value, such iap should not be copied
                if (copiedApplicationModel == null) {
                    continue;
                } else {
                    // if "copying" pc returned with a positive result, all entities should be updated with the new pc
                    priceComponent = copiedApplicationModel.getPriceComponent();
                }
            }

            Optional<InterimAdvancePaymentTerms> advancePaymentTermsOptional = interimAdvancePaymentTermsRepository
                    .findByInterimAdvancePaymentIdAndStatusInOrderByCreateDate(item.getId(), List.of(InterimAdvancePaymentSubObjectStatus.ACTIVE));

            if (advancePaymentTermsOptional.isPresent()) {
                InterimAdvancePaymentTerms terms = advancePaymentTermsOptional.get();
                if (terms.getCalendar() != null && terms.getCalendar().getStatus().equals(NomenclatureItemStatus.ACTIVE)) {
                    InterimAdvancePayment interimAdvancePayment = interimAdvancePaymentRepository
                            .saveAndFlush(
                                    advancePaymentGroupMapper.copyInterimAdvancePayment(item, priceComponent)
                            );

                    InterimAdvancePaymentTerms copyTerms = advancePaymentGroupMapper.copyTerms(terms);
                    copyTerms.setInterimAdvancePayment(interimAdvancePayment);

                    interimAdvancePaymentTermsRepository.saveAndFlush(copyTerms);

                    interimAdvancePayment.setGroupDetailId(null);
                    advancePaymentsToCopy.add(interimAdvancePayment);
                }
            } else {
                InterimAdvancePayment interimAdvancePayment = interimAdvancePaymentRepository
                        .saveAndFlush(
                                advancePaymentGroupMapper.copyInterimAdvancePayment(item, priceComponent)
                        );

                advancePaymentsToCopy.add(interimAdvancePayment);
            }

        }
        EPBChainedExceptionTriggerUtil.throwExceptionIfRequired(errorMessages, log);
        return interimAdvancePaymentRepository.saveAll(advancePaymentsToCopy);
    }

    private boolean checkCurrencyForCopy(Currency currency) {
        if (currency != null) {
            return currency.getStatus().equals(NomenclatureItemStatus.ACTIVE);
        }
        return true;
    }

    @Override
    public CopyDomain getDomain() {
        return CopyDomain.INTERIM_ADVANCED_PAYMENT;
    }

    @Override
    public Page<CopyDomainListResponse> filterCopyDomain(CopyDomainBaseRequest request) {
        Sort.Order order = new Sort.Order(Sort.Direction.DESC, "id");
        Pageable pageable = PageRequest.of(request.getPage(), request.getSize(), Sort.by(order));
        return interimAdvancePaymentRepository.filterForCopy(
                EPBStringUtils.fromPromptToQueryParameter(request.getPrompt()),
                List.of(InterimAdvancePaymentStatus.ACTIVE),
                pageable
        );
    }


    /**
     * Returns lists of price components that are "available" for adding to interim advance payments.
     *
     * @param request {@link AvailablePriceComponentSearchRequest} containing search criteria
     * @return page of {@link AvailablePriceComponentResponse} objects
     */
    public Page<AvailablePriceComponentResponse> getAvailablePriceComponents(AvailablePriceComponentSearchRequest request) {
        log.debug("Retrieving available price components by request: {}", request);

        Page<PriceComponent> availablePriceComponents = priceComponentRepository
                .getAvailablePriceComponentsForIap(
                        EPBStringUtils.fromPromptToQueryParameter(request.getPrompt()),
                        PageRequest.of(request.getPage(), request.getSize())
                );

        return availablePriceComponents.map(AvailablePriceComponentResponse::responseFromEntity);
    }
}
