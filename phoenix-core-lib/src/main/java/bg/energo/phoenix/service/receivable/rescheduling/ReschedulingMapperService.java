package bg.energo.phoenix.service.receivable.rescheduling;

import bg.energo.phoenix.exception.DomainEntityNotFoundException;
import bg.energo.phoenix.model.entity.EntityStatus;
import bg.energo.phoenix.model.entity.customer.Customer;
import bg.energo.phoenix.model.entity.customer.CustomerDetails;
import bg.energo.phoenix.model.entity.nomenclature.product.Currency;
import bg.energo.phoenix.model.entity.receivable.customerAssessment.CustomerAssessment;
import bg.energo.phoenix.model.entity.receivable.customerLiability.CustomerLiability;
import bg.energo.phoenix.model.entity.receivable.rescheduling.Rescheduling;
import bg.energo.phoenix.model.entity.receivable.rescheduling.ReschedulingLiabilities;
import bg.energo.phoenix.model.enums.contract.InterestRate.InterestRateStatus;
import bg.energo.phoenix.model.enums.customer.CustomerStatus;
import bg.energo.phoenix.model.enums.nomenclature.NomenclatureItemStatus;
import bg.energo.phoenix.model.enums.receivable.rescheduling.ReschedulingStatus;
import bg.energo.phoenix.model.request.receivable.rescheduling.ReschedulingRequest;
import bg.energo.phoenix.model.request.receivable.rescheduling.ReschedulingUpdateRequest;
import bg.energo.phoenix.model.response.contract.InterestRate.InterestRateShortResponse;
import bg.energo.phoenix.model.response.customer.CustomerDetailsShortResponse;
import bg.energo.phoenix.model.response.receivable.rescheduling.ReschedulingLiabilityResponse;
import bg.energo.phoenix.model.response.receivable.rescheduling.ReschedulingPlansResponse;
import bg.energo.phoenix.model.response.receivable.rescheduling.ReschedulingResponse;
import bg.energo.phoenix.model.response.shared.ShortResponse;
import bg.energo.phoenix.repository.customer.CustomerDetailsRepository;
import bg.energo.phoenix.repository.customer.CustomerRepository;
import bg.energo.phoenix.repository.interestRate.InterestRateRepository;
import bg.energo.phoenix.repository.nomenclature.product.CurrencyRepository;
import bg.energo.phoenix.repository.receivable.customerAssessment.CustomerAssessmentRepository;
import bg.energo.phoenix.repository.receivable.customerLiability.CustomerLiabilityRepository;
import bg.energo.phoenix.repository.receivable.rescheduling.ReschedulingDraftLiabilitiesRepository;
import bg.energo.phoenix.repository.receivable.rescheduling.ReschedulingLiabilitiesRepository;
import bg.energo.phoenix.repository.receivable.rescheduling.ReschedulingPlansRepository;
import bg.energo.phoenix.service.receivable.customerLiability.CustomerLiabilityService;
import bg.energo.phoenix.util.epb.EPBDecimalUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReschedulingMapperService {

    private final CustomerAssessmentRepository customerAssessmentRepository;
    private final CustomerRepository customerRepository;
    private final CurrencyRepository currencyRepository;
    private final InterestRateRepository interestRateRepository;
    private final CustomerLiabilityRepository customerLiabilityRepository;
    private final ReschedulingLiabilitiesRepository reschedulingLiabilitiesRepository;
    private final CustomerDetailsRepository customerDetailsRepository;
    private final CustomerLiabilityService customerLiabilityService;
    private final ReschedulingDraftLiabilitiesRepository reschedulingDraftLiabilitiesRepository;
    private final ReschedulingPlansRepository reschedulingPlansRepository;

    /**
     * Maps the parameters from a {@link ReschedulingRequest} to create a new {@link Rescheduling} entity.
     *
     * @param request the request containing the parameters to map
     * @return the newly created Rescheduling entity with the mapped parameters
     */
    public Rescheduling mapParametersForCreate(ReschedulingRequest request) {
        log.info("Mapping parameters for rescheduling create with request %s;".formatted(request));
        Rescheduling rescheduling = new Rescheduling();
        //set temporary number for saving
        rescheduling.setReschedulingNumber("TEMPORARY_NUMBER");
        rescheduling.setReschedulingStatus(request.getReschedulingStatus());
        rescheduling.setStatus(EntityStatus.ACTIVE);
        rescheduling.setCustomerDetailId(request.getCustomerDetailId());
        rescheduling.setNumberOfInstallment(request.getNumberOfInstallment() != null ? request.getNumberOfInstallment()
                                                                                              .intValue() : null);
        rescheduling.setAmountOfTheInstallment(EPBDecimalUtils.roundToTwoDecimalPlaces(request.getAmountOfTheInstallment()));
        rescheduling.setInstallmentDueDay(request.getInstallmentDueDayOfTheMonth()
                                                 .shortValue());
        rescheduling.setInterestType(request.getReschedulingInterestType());
        rescheduling.setCustomerCommunicationId(request.getCustomerCommunicationDataId());
        rescheduling.setCustomerCommunicationIdForContract(request.getCustomerCommunicationDataIdForContract());
        rescheduling.setReversed(false);
        checkAndSetAdditionalParameters(request, rescheduling);

        return rescheduling;
    }

    /**
     * Maps a {@link Rescheduling} entity to a {@link ReschedulingResponse} object.
     *
     * @param rescheduling the {@link Rescheduling} entity to map
     * @return the {@link ReschedulingResponse} object with the mapped data
     */
    public ReschedulingResponse mapToReschedulingResponse(Rescheduling rescheduling) {
        log.info("Mapping rescheduling response for rescheduling view request;");
        ReschedulingResponse response = new ReschedulingResponse();
        response.setId(rescheduling.getId());
        response.setReschedulingNumber(rescheduling.getReschedulingNumber());
        response.setReschedulingStatus(rescheduling.getReschedulingStatus());
        response.setStatus(rescheduling.getStatus());
        response.setCustomerResponse(getCustomerResponse(rescheduling));
        response.setCustomerCommunicationDataId(rescheduling.getCustomerCommunicationId());
        response.setCustomerCommunicationDataIdForContract(rescheduling.getCustomerCommunicationIdForContract());
        response.setNumberOfInstallment(rescheduling.getNumberOfInstallment());
        response.setAmountOfTheInstallment(rescheduling.getAmountOfTheInstallment());
        response.setCurrencyResponse(getCurrencyResponse(rescheduling.getCurrencyId()));
        response.setInterestRateResponse(rescheduling.getInterestRateId() == null ? null : getInterestRate(rescheduling.getInterestRateId()));
        response.setInterestRateForInstallmentsResponse(getInterestRate(rescheduling.getInterestRateIdForInstallments()));
        response.setInstallmentDueDay(rescheduling.getInstallmentDueDay());
        response.setInterestType(rescheduling.getInterestType());
        response.setReversed(rescheduling.getReversed());
        CustomerAssessment customerAssessment = customerAssessmentRepository.findById(rescheduling.getCustomerAssessmentId())
                                                                            .orElseThrow(() -> new DomainEntityNotFoundException(
                                                                                    "Customer Assessment not found by ID %s;".formatted(
                                                                                            rescheduling.getCustomerAssessmentId())));
        response.setCustomerAssessmentId(customerAssessment.getId());
        response.setCustomerAssessmentName(customerAssessment.getAssessmentNumber());
        response.setIsLiabilityListChanged(false);

        List<ReschedulingPlansResponse> reschedulingPlansResponse = null;
        List<Long> checked = reschedulingDraftLiabilitiesRepository.findCustomerLiabilityIdIdByReschedulingId(rescheduling.getId());
        if (rescheduling.getReschedulingStatus()
                        .equals(ReschedulingStatus.DRAFT)) {
            List<ReschedulingLiabilityResponse> reschedulingLiabilityResponses = customerLiabilityService.getReschedulingLiabilitiesByCustomerId(
                    rescheduling.getCustomerId(),
                    null
            );
            Map<Long, ReschedulingLiabilityResponse> map = reschedulingLiabilityResponses
                    .stream()
                    .collect(Collectors.toMap(ReschedulingLiabilityResponse::getLiabilityId, y -> y));
            checked.forEach(check -> {
                ReschedulingLiabilityResponse reschedulingLiabilityResponse = map.get(check);
                if (reschedulingLiabilityResponse != null) {
                    reschedulingLiabilityResponse.setChecked(true);
                } else {
                    response.setIsLiabilityListChanged(true);
                }
            });
            List<ReschedulingLiabilityResponse> sorted = sortByCheckedStatus(map.values());
            response.setReschedulingLiabilityResponses(sorted);
        } else {
            List<ReschedulingLiabilityResponse> list = customerLiabilityService.getReschedulingLiabilitiesByLiabilityIds(checked).
                                                                               stream()
                                                                               .peek(liab -> liab.setChecked(true))
                                                                               .toList();
            response.setReschedulingLiabilityResponses(list);
            reschedulingPlansResponse = reschedulingPlansRepository.findByReschedulingId(rescheduling.getId())
                                                                   .stream()
                                                                   .map(ReschedulingPlansResponse::new)
                                                                   .toList();
            if(!CollectionUtils.isEmpty(reschedulingPlansResponse)) {
                BigDecimal sumOfAmount = BigDecimal.ZERO;
                BigDecimal sumOfPrincipalAmount = BigDecimal.ZERO;
                BigDecimal sumOfInterestAmount = BigDecimal.ZERO;
                BigDecimal sumOfFeeAmount = BigDecimal.ZERO;

                for(ReschedulingPlansResponse plansResponse : reschedulingPlansResponse) {
                    sumOfAmount = sumOfAmount.add(plansResponse.getAmount());
                    sumOfPrincipalAmount = sumOfPrincipalAmount.add(plansResponse.getPrincipalAmount());
                    sumOfInterestAmount = sumOfInterestAmount.add(plansResponse.getInterest());
                    sumOfFeeAmount = sumOfFeeAmount.add(plansResponse.getFee());
                }
                response.setSumOfAmount(sumOfAmount.setScale(2, RoundingMode.DOWN));
                response.setSumOfPrincipalAmount(sumOfPrincipalAmount.setScale(2, RoundingMode.DOWN));
                response.setSumOfInterestAmount(sumOfInterestAmount.setScale(2, RoundingMode.DOWN));
                response.setSumOfFeeAmount(sumOfFeeAmount.setScale(2, RoundingMode.DOWN));
            }
        }
        response.setReschedulingPlans(reschedulingPlansResponse);

        return response;
    }

    private List<ReschedulingLiabilityResponse> sortByCheckedStatus(Collection<ReschedulingLiabilityResponse> responses) {
        List<ReschedulingLiabilityResponse> result = new ArrayList<>(responses.size());

        for (ReschedulingLiabilityResponse response : responses) {
            if (response.isChecked()) {
                result.add(response);
            }
        }

        for (ReschedulingLiabilityResponse response : responses) {
            if (!response.isChecked()) {
                result.add(response);
            }
        }

        return result;
    }

    /**
     * Maps the parameters from the provided {@link ReschedulingUpdateRequest} to the given {@link Rescheduling} entity.
     * If the `saveAndExecute` flag is set, it also creates the rescheduling liabilities and sets the rescheduling status to `EXECUTED`.
     *
     * @param request       the {@link ReschedulingUpdateRequest} containing the updated parameters
     * @param rescheduling  the {@link Rescheduling} entity to update
     * @param errorMessages a list to store any error messages that occur during the mapping
     */
    public void mapParametersForUpdate(ReschedulingUpdateRequest request, Rescheduling rescheduling, List<String> errorMessages) {
        log.info("Mapping parameters for rescheduling update with request: %s;".formatted(request));

        rescheduling.setNumberOfInstallment(request.getNumberOfInstallment() != null ? request.getNumberOfInstallment()
                                                                                              .intValue() : null);
        rescheduling.setAmountOfTheInstallment(EPBDecimalUtils.roundToTwoDecimalPlaces(request.getAmountOfTheInstallment()));
        rescheduling.setInstallmentDueDay(request.getInstallmentDueDayOfTheMonth()
                                                 .shortValue());
        rescheduling.setInterestType(request.getReschedulingInterestType());
        rescheduling.setCustomerDetailId(request.getCustomerDetailId());
        rescheduling.setCustomerCommunicationId(request.getCustomerCommunicationDataId());
        rescheduling.setCustomerCommunicationIdForContract(request.getCustomerCommunicationDataIdForContract());
        checkAndSetAdditionalParametersFroUpdateRequest(request, rescheduling);

        if (request.getReschedulingStatus()
                   .equals(ReschedulingStatus.EXECUTED)) {
            rescheduling.setReschedulingStatus(ReschedulingStatus.EXECUTED);
            createReschedulingLiabilities(request.getLiabilityIds(), rescheduling.getId(), request.getCustomerId(), errorMessages);
        }
    }

    /**
     * Creates rescheduling liabilities for the given liability IDs and rescheduling ID.
     *
     * @param liabilityIds   the list of customer liability IDs to create rescheduling liabilities for
     * @param reschedulingId the ID of the rescheduling entity to associate the liabilities with
     */
    public void createReschedulingLiabilities(
            List<Long> liabilityIds,
            Long reschedulingId,
            Long customerId,
            List<String> errorMessages
    ) {
        log.info("Creating Rescheduling Liabilities with liability ids: %s".formatted(liabilityIds));
        if (liabilityIds != null && !liabilityIds.isEmpty()) {
            liabilityIds.forEach(liabilityId -> {
                CustomerLiability customerLiability = customerLiabilityRepository.findByIdAndStatus(liabilityId, EntityStatus.ACTIVE)
                                                                                 .orElseThrow(() -> new DomainEntityNotFoundException(
                                                                                         "Customer Liability not found by ID %s;".formatted(
                                                                                                 liabilityId)));

                ReschedulingLiabilities reschedulingLiabilities = new ReschedulingLiabilities();
                reschedulingLiabilities.setReschedulingId(reschedulingId);
                reschedulingLiabilities.setCustomerLiabilitieId(customerLiability.getId());
                reschedulingLiabilities.setCurrentAmount(customerLiability.getCurrentAmount());
                reschedulingLiabilities.setInterestsFromDate(customerLiability.getDueDate()
                                                                              .plusDays(1));
                reschedulingLiabilitiesRepository.save(reschedulingLiabilities);

            });
        }
    }

    /**
     * Checks and sets additional parameters for a rescheduling create request.
     * <p>
     * This method performs the following actions:
     * - Verifies that the customer assessment with the provided ID exists and is active.
     * - Sets the customer assessment ID on the rescheduling entity.
     * - Verifies that the customer with the provided ID exists and is active.
     * - Sets the customer ID on the rescheduling entity.
     * - Verifies that the currency with the provided ID exists and is active.
     * - Sets the currency ID on the rescheduling entity.
     * - Verifies that the interest rate for installments with the provided ID exists and is active.
     * - Sets the interest rate ID for installments on the rescheduling entity.
     * - If a replace interest rate for liabilities ID is provided, verifies that the interest rate with the provided ID exists and is active, and sets the interest rate ID on the rescheduling entity.
     *
     * @param request      the {@link ReschedulingRequest} containing the updated parameters
     * @param rescheduling the {@link Rescheduling} entity to update
     */
    private void checkAndSetAdditionalParameters(ReschedulingRequest request, Rescheduling rescheduling) {
        log.info("Checking and setting additional parameters for rescheduling create request;");
        customerAssessmentRepository.findByIdAndStatus(request.getCustomerAssessmentId(), EntityStatus.ACTIVE)
                                    .orElseThrow(() -> new DomainEntityNotFoundException("Customer Assessment not found by ID %s;".formatted(
                                            request.getCustomerAssessmentId())));
        rescheduling.setCustomerAssessmentId(request.getCustomerAssessmentId());

        customerRepository.findByIdAndStatuses(request.getCustomerId(), List.of(CustomerStatus.ACTIVE))
                          .orElseThrow(() -> new DomainEntityNotFoundException("Customer not found by ID %s;".formatted(request.getCustomerId())));
        rescheduling.setCustomerId(request.getCustomerId());

        currencyRepository.findByIdAndStatus(request.getCurrencyId(), List.of(NomenclatureItemStatus.ACTIVE))
                          .orElseThrow(() -> new DomainEntityNotFoundException("Currency not found by ID %s;".formatted(request.getCurrencyId())));
        rescheduling.setCurrencyId(request.getCurrencyId());

        interestRateRepository.findByIdAndStatusIn(request.getInterestRateForInstalmentsId(), List.of(InterestRateStatus.ACTIVE))
                              .orElseThrow(() -> new DomainEntityNotFoundException("Interest Rate not found by ID %s;".formatted(
                                      request.getInterestRateForInstalmentsId())));
        rescheduling.setInterestRateIdForInstallments(request.getInterestRateForInstalmentsId());

        if (request.getReplaceInterestRateForLiabilitiesId() != null) {
            interestRateRepository.findByIdAndStatusIn(
                                          request.getReplaceInterestRateForLiabilitiesId(),
                                          List.of(InterestRateStatus.ACTIVE)
                                  )
                                  .orElseThrow(() -> new DomainEntityNotFoundException("Interest Rate not found by ID %s;".formatted(
                                          request.getReplaceInterestRateForLiabilitiesId())));
            rescheduling.setInterestRateId(request.getReplaceInterestRateForLiabilitiesId());
        }
    }

    /**
     * Gets a short response containing the customer details.
     * <p>
     * This method retrieves the customer and customer details entities based on the provided customer ID, and returns a
     * {@link CustomerDetailsShortResponse} containing the customer ID, customer details ID, customer name, customer type,
     * and business activity.
     *
     * @return a {@link CustomerDetailsShortResponse} containing the customer details
     * @throws DomainEntityNotFoundException if the customer or customer details are not found
     */
    private CustomerDetailsShortResponse getCustomerResponse(Rescheduling rescheduling) {
        Customer customer = customerRepository.findByIdAndStatuses(rescheduling.getCustomerId(), List.of(CustomerStatus.ACTIVE))
                                              .orElseThrow(() -> new DomainEntityNotFoundException(
                                                      "Customer not found with given id: %s".formatted(rescheduling.getCustomerId())));
        CustomerDetails customerDetails = getCustomerDetails(rescheduling.getCustomerDetailId());

        return new CustomerDetailsShortResponse(
                customer.getId(),
                customerDetails.getId(),
                getCustomerName(customer, customerDetails),
                customer.getCustomerType(),
                customerDetails.getBusinessActivity()
        );
    }

    /**
     * Retrieves the {@link CustomerDetails} entity with the given ID.
     *
     * @param customerDetailId the ID of the {@link CustomerDetails} entity to retrieve
     * @return the {@link CustomerDetails} entity with the given ID
     * @throws DomainEntityNotFoundException if the {@link CustomerDetails} entity with the given ID is not found
     */
    private CustomerDetails getCustomerDetails(Long customerDetailId) {
        return customerDetailsRepository.findById(customerDetailId)
                                        .orElseThrow(() -> new DomainEntityNotFoundException("CustomerDetails not found with id:%s".formatted(
                                                customerDetailId)));
    }

    /**
     * Gets the full name of the customer, including the legal form name if available.
     *
     * @param customer        the customer entity
     * @param customerDetails the customer details entity
     * @return the full name of the customer, including the legal form name if available
     */
    private String getCustomerName(Customer customer, CustomerDetails customerDetails) {
        String legalFormName = customerDetailsRepository.getLegalFormName(customerDetails.getId());
        return String.format(
                "%s (%s%s%s%s)", customer.getIdentifier(), customerDetails.getName(),
                customerDetails.getMiddleName() != null ? " " + customerDetails.getMiddleName() : "",
                customerDetails.getLastName() != null ? " " + customerDetails.getLastName() : "",
                StringUtils.isNotEmpty(legalFormName) ? " " + legalFormName : ""
        );
    }

    /**
     * Gets a short response containing the currency details.
     * <p>
     * This method retrieves the currency entity based on the provided currency ID, and returns a
     * {@link ShortResponse} containing the currency ID and name.
     *
     * @param currencyId the ID of the currency
     * @return a {@link ShortResponse} containing the currency details
     * @throws DomainEntityNotFoundException if the currency is not found
     */
    private ShortResponse getCurrencyResponse(Long currencyId) {
        Currency currency = currencyRepository.findById(currencyId)
                                              .orElseThrow(() -> new DomainEntityNotFoundException(
                                                      "Currency not found with given id: %s".formatted(currencyId)));
        return new ShortResponse(currency.getId(), currency.getName());
    }

    /**
     * Retrieves a short response containing the details of an interest rate with the given ID.
     * <p>
     * This method retrieves the interest rate entity with the specified ID and the active status, and returns a
     * {@link InterestRateShortResponse} containing the interest rate ID and name.
     *
     * @param interestRateId the ID of the interest rate to retrieve
     * @return a {@link InterestRateShortResponse} containing the interest rate details
     * @throws DomainEntityNotFoundException if the interest rate with the given ID is not found
     */
    private InterestRateShortResponse getInterestRate(Long interestRateId) {
        return new InterestRateShortResponse(
                interestRateRepository.findByIdAndStatusIn(interestRateId, List.of(InterestRateStatus.ACTIVE))
                                      .orElseThrow(() ->
                                                           new DomainEntityNotFoundException(
                                                                   "Interest rate with given id: %s not found".formatted(interestRateId))
                                      )
        );
    }

    /**
     * Checks and sets additional parameters for a rescheduling update request.
     * <p>
     * This method performs the following checks and updates on the provided `Rescheduling` entity:
     * - Checks if the `customerId` has changed and updates it if necessary.
     * - Checks if the `customerAssessmentId` has changed and updates it if necessary.
     * - Checks if the `currencyId` has changed and updates it if necessary.
     * - Checks if the `interestRateId` for liabilities has changed and updates it if necessary.
     * - Checks if the `interestRateIdForInstallments` has changed and updates it if necessary.
     *
     * @param request      the rescheduling update request
     * @param rescheduling the rescheduling entity to be updated
     */
    private void checkAndSetAdditionalParametersFroUpdateRequest(ReschedulingRequest request, Rescheduling rescheduling) {
        log.info("Checking and setting additional parameters for rescheduling update request;");
        if (!Objects.equals(request.getCustomerId(), rescheduling.getCustomerId())) {
            customerRepository.findByIdAndStatuses(request.getCustomerId(), List.of(CustomerStatus.ACTIVE))
                              .orElseThrow(() -> new DomainEntityNotFoundException("Customer not found by ID %s;".formatted(request.getCustomerId())));
            rescheduling.setCustomerId(request.getCustomerId());
        }

        if (!Objects.equals(request.getCustomerAssessmentId(), rescheduling.getCustomerAssessmentId())) {
            customerAssessmentRepository.findByIdAndStatus(request.getCustomerAssessmentId(), EntityStatus.ACTIVE)
                                        .orElseThrow(() -> new DomainEntityNotFoundException("Customer Assessment not found by ID %s;".formatted(
                                                request.getCustomerAssessmentId())));
            rescheduling.setCustomerAssessmentId(request.getCustomerAssessmentId());
        }

        if (!Objects.equals(request.getCurrencyId(), rescheduling.getCurrencyId())) {
            currencyRepository.findByIdAndStatus(request.getCurrencyId(), List.of(NomenclatureItemStatus.ACTIVE))
                              .orElseThrow(() -> new DomainEntityNotFoundException("Currency not found by ID %s;".formatted(request.getCurrencyId())));
            rescheduling.setCurrencyId(request.getCurrencyId());
        }

        if (!Objects.equals(rescheduling.getInterestRateId(), request.getReplaceInterestRateForLiabilitiesId())) {
            if (request.getReplaceInterestRateForLiabilitiesId() != null) {
                interestRateRepository.findByIdAndStatusIn(
                                              request.getReplaceInterestRateForLiabilitiesId(),
                                              List.of(InterestRateStatus.ACTIVE)
                                      )
                                      .orElseThrow(() -> new DomainEntityNotFoundException("Interest Rate not found by ID %s;".formatted(
                                              request.getReplaceInterestRateForLiabilitiesId())));
            }

            rescheduling.setInterestRateId(request.getReplaceInterestRateForLiabilitiesId());
        }

        if (!Objects.equals(rescheduling.getInterestRateIdForInstallments(), request.getInterestRateForInstalmentsId())) {
            interestRateRepository.findByIdAndStatusIn(request.getInterestRateForInstalmentsId(), List.of(InterestRateStatus.ACTIVE))
                                  .orElseThrow(() -> new DomainEntityNotFoundException("Interest Rate not found by ID %s;".formatted(
                                          request.getInterestRateForInstalmentsId())));
            rescheduling.setInterestRateIdForInstallments(request.getInterestRateForInstalmentsId());
        }
    }
}
