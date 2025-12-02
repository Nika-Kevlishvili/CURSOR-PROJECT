package bg.energo.phoenix.service.customer.indicators;

import bg.energo.phoenix.exception.DomainEntityNotFoundException;
import bg.energo.phoenix.model.entity.customer.Customer;
import bg.energo.phoenix.model.entity.customer.CustomerDetails;
import bg.energo.phoenix.model.entity.customer.indicators.CustomerIndicators;
import bg.energo.phoenix.model.response.customer.indicators.CustomerIndicatorsResponse;
import bg.energo.phoenix.repository.customer.CustomerDetailsRepository;
import bg.energo.phoenix.repository.customer.CustomerRepository;
import bg.energo.phoenix.repository.customer.indicators.CustomerIndicatorsRepository;
import bg.energo.phoenix.repository.customer.indicators.EnergoProRepository;
import bg.energo.phoenix.service.riskList.RiskListService;
import bg.energo.phoenix.service.riskList.model.RiskListBasicInfoResponse;
import bg.energo.phoenix.service.riskList.model.RiskListDecision;
import bg.energo.phoenix.service.riskList.model.RiskListRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;


@Slf4j
@Service
@RequiredArgsConstructor
public class CustomerIndicatorsService {

    private final CustomerIndicatorsRepository customerIndicatorsRepository;
    private final CustomerRepository customerRepository;
    private final CustomerDetailsRepository customerDetailsRepository;
    private final RiskListService riskListService;
    private final EnergoProRepository energoProRepository;
    private final CustomerIndicatorsMapperService customerIndicatorsMapperService;

    /**
     * Retrieves the customer indicators associated with the given customer ID,
     * calculating additional metrics such as the number of lawsuits, the current
     * risk assessment, and liabilities.
     *
     * @param customerId the unique identifier of the customer whose indicators are to be retrieved
     * @return a CustomerIndicatorsResponse object containing the processed customer indicators
     * @throws DomainEntityNotFoundException if the customer or the customer indicators entity cannot be found
     */
    public CustomerIndicatorsResponse getCustomerIndicatorsById(Long customerId) {
        // Retrieve CustomerIndicators by customerId
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new DomainEntityNotFoundException("Customer with id %d does not exist".formatted(customerId)));

        CustomerIndicators customerIndicators = customerIndicatorsRepository.findById(customerId)
                .orElseGet(
                        () -> customerIndicatorsRepository.save(
                                CustomerIndicators.builder()
                                        .customerId(customerId)
                                        .numberOfActiveContracts(0L)
                                        .expiringContractsNext3Months(0L)
                                        .overdueLiabilities12MonthWithData(0)
                                        .overdueLiabilities24MonthWithData(0)
                                        .warnings12Month(0)
                                        .warnings24Month(0)
                                        .warnings12MonthWithData(0)
                                        .warnings24MonthWithData(0)
                                        .disconnection12Month(0)
                                        .disconnection24Month(0)
                                        .disconnection12MonthWithData(0)
                                        .disconnection24MonthWithData(0)
                                        .build()
                        )
                );

        customerIndicators.setNumberOfLawsuits(getLawsuitsCount(customer));
        customerIndicators.setCurrentRiskAssessment(getCurrentRiskAssessment(customer));
        customerIndicators.setAssessmentDate(LocalDate.now());
        customerIndicators.setCurrentLiabilities(Objects.requireNonNullElse(customerIndicatorsRepository.getCurrentLiabilities(customerId), BigDecimal.ZERO));

        return customerIndicatorsMapperService.toResponse(customerIndicators);
    }

    private int getLawsuitsCount(Customer customer) {
        try {
            return energoProRepository.getCustomerLawsuitsCount(customer.getIdentifier());
        } catch (Exception e) {
            log.error("Error fetching lawsuits count for customer {}: {}", customer.getId(), e.getMessage());
            throw new IllegalStateException("Failed to fetch lawsuits count for customer " + customer.getId(), e);
        }
    }

    /**
     * Evaluates and retrieves the current risk assessment for a customer based on their details and consumption data.
     *
     * @param customer the customer for whom the risk assessment is to be calculated.
     * @return a String representing the current risk assessment of the specified customer.
     * @throws DomainEntityNotFoundException if the specified customer or their related details are not found.
     */
    private String getCurrentRiskAssessment(Customer customer) {
        CustomerDetails customerDetails = customerDetailsRepository
                .findFirstByCustomerId(customer.getId(), Sort.by(Sort.Direction.DESC, "createDate"))
                .orElseThrow(() -> new DomainEntityNotFoundException("Customer details with id %d does not exist".formatted(customer.getId())));

        RiskListRequest request = new RiskListRequest();
        request.setConsumption(BigDecimal.valueOf(4000));
        request.setIdentifier(customer.getIdentifier());
        request.setVersion(customer.getLastCustomerDetailId());

        RiskListBasicInfoResponse response = riskListService.evaluateBasicCustomerRisk(request, customer, customerDetails);

        try {
            return evaluateRiskByConsumption(request, customer, customerDetails, response);
        } catch (Exception e) {
            log.error("Error evaluating risk for customer {}: {}", customer.getId(), e.getMessage());
            return null;
        }
    }

    /**
     * Evaluates and formats the risk assessment logic using stepwise consumption adjustments.
     *
     * @param request  The initial request object (used for adjustments).
     * @param response The raw response from RiskListService.
     * @return A formatted string representing the risk evaluation result.
     */
    private String evaluateRiskByConsumption(RiskListRequest request, Customer customer, CustomerDetails customerDetails, RiskListBasicInfoResponse response) {
        log.debug("Starting risk logic evaluation for customer");

        BigDecimal consumption = request.getConsumption();

        while (true) {
            // Extract decision and recommendations from the response
            RiskListDecision decision = response.getDecision();
            List<String> recommendations = response.getRecommendations();

            // Stepwise logic based on decision
            switch (decision) {
                case PERMIT -> {
                    // Format and return the PERMIT result
                    return formatPermitResponse(consumption, !isEmptyRecommendations(recommendations));
                }
                case DENY -> {
                    // Adjust consumption to 50 and re-evaluate
                    if (consumption.compareTo(BigDecimal.valueOf(50)) != 0) {
                        consumption = BigDecimal.valueOf(50);
                        request.setConsumption(consumption);
                        response = riskListService.evaluateBasicCustomerRisk(request, customer, customerDetails);
                    } else {
                        return "Deny S / Забрана S";
                    }
                }
                case MISSING -> {
                    // Adjust consumption for MISSING decisions
                    consumption = adjustConsumptionForMissing(consumption);
                    if (consumption == null) {
                        return "Missing"; // No further steps
                    }
                    request.setConsumption(consumption);
                    response = riskListService.evaluateBasicCustomerRisk(request, customer, customerDetails);
                }
                default -> {
                    return "Missing";
                }
            }
        }
    }

    /**
     * Adjusts consumption values for MISSING decisions.
     */
    private BigDecimal adjustConsumptionForMissing(BigDecimal currentConsumption) {
        if (currentConsumption.compareTo(BigDecimal.valueOf(4000)) == 0) {
            return BigDecimal.valueOf(2000);
        } else if (currentConsumption.compareTo(BigDecimal.valueOf(2000)) == 0) {
            return BigDecimal.valueOf(500);
        } else if (currentConsumption.compareTo(BigDecimal.valueOf(500)) == 0) {
            return BigDecimal.valueOf(50);
        }
        return null;
    }

    /**
     * Formats a PERMIT response based on conditions.
     */
    private String formatPermitResponse(BigDecimal consumption, boolean withConditions) {
        String permit;
        if (consumption.compareTo(BigDecimal.valueOf(4000)) == 0) {
            permit = "Permit XL / Разрешение XL";
        } else if (consumption.compareTo(BigDecimal.valueOf(2000)) == 0) {
            permit = "Permit L / Разрешение L";
        } else if (consumption.compareTo(BigDecimal.valueOf(500)) == 0) {
            permit = "Permit M / Разрешение M";
        } else if (consumption.compareTo(BigDecimal.valueOf(50)) == 0) {
            permit = "Permit S / Разрешение S";
        } else {
            permit = "Missing";
        }

        if (withConditions) {
            permit += " with conditions / с условия";
        }
        return permit;
    }

    /**
     * Checks if a list of recommendations is empty or null.
     */
    private boolean isEmptyRecommendations(List<String> recommendations) {
        return recommendations == null || recommendations.isEmpty();
    }
}