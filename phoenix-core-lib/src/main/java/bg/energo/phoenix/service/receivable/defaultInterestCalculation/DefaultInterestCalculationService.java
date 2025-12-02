package bg.energo.phoenix.service.receivable.defaultInterestCalculation;

import bg.energo.phoenix.model.process.latePaymentFIne.InterestCalculationResponseDTO;
import bg.energo.phoenix.model.process.latePaymentFIne.LatePaymentFineDTO;
import bg.energo.phoenix.model.request.receivable.defaultInterestCalculation.DefaultInterestCalculationPreviewRequest;
import bg.energo.phoenix.model.request.receivable.defaultInterestCalculation.DefaultInterestCalculationRequest;
import bg.energo.phoenix.model.response.receivable.defaultInterestCalculation.DefaultInterestCalculationPreviewResponse;
import bg.energo.phoenix.model.response.receivable.defaultInterestCalculation.DefaultInterestRateCalculationProcessResponse;
import bg.energo.phoenix.repository.receivable.customerLiability.CustomerLiabilityRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class DefaultInterestCalculationService {

    private final CustomerLiabilityRepository customerLiabilityRepository;
    private final ObjectMapper objectMapper;

    /**
     * Previews the default interest calculation for the specified customer.
     *
     * @param request the request containing the customer ID for which to preview the default interest calculation
     * @return a list of {@link DefaultInterestCalculationPreviewResponse} objects representing the preview of the default interest calculation
     */
    public Page<DefaultInterestCalculationPreviewResponse> preview(DefaultInterestCalculationPreviewRequest request) {
        log.info("Previewing default interest calculation with request: {}", request);
        return customerLiabilityRepository.getDefaultInterestCalculationPreview(
                request.customerId,
                PageRequest.of(request.getPage(), request.getSize())
        );
    }

    /**
     * Calculates the default interest for the specified liability IDs and virtual payment date.
     *
     * @param request the request containing the liability IDs and virtual payment date for which to calculate the default interest
     * @return a list of {@link LatePaymentFineDTO} objects representing the calculated default interest
     */
    public List<DefaultInterestRateCalculationProcessResponse> calculate(DefaultInterestCalculationRequest request) {
        log.info("Calculating default interest with request: {}", request);

        if (request.getLiabilityIds().isEmpty()) {
            return Collections.emptyList();
        }

        List<Object[]> liabilities = customerLiabilityRepository.calculateDefaultInterest(
                request.getLiabilityIds(),
                request.getVirtualPaymentDate()
        );

        return liabilities.stream()
                .map(this::processLiability)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toList());
    }

    /**
     * Processes a liability item by extracting the calculated interest amount from the provided JSON data.
     *
     * @param item an array containing the liability ID and the JSON data for the interest calculation
     * @return an {@link Optional} of {@link DefaultInterestRateCalculationProcessResponse} containing the liability ID and the calculated interest amount, or an empty {@link Optional} if an error occurs during processing
     */
    private Optional<DefaultInterestRateCalculationProcessResponse> processLiability(Object[] item) {
        try {
            Long id = (Long) item[0];
            String lfpJson = (String) item[1];
            BigDecimal amount = extractAmount(lfpJson);

            return Optional.of(new DefaultInterestRateCalculationProcessResponse(id, amount));
        } catch (Exception e) {
            log.error("Error processing liability: {}", e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * Extracts the calculated interest amount from the provided JSON data.
     *
     * @param lfpJson the JSON data containing the interest calculation response
     * @return the calculated interest amount, or null if the JSON data is null or empty
     * @throws JsonProcessingException if an error occurs while parsing the JSON data
     */
    private BigDecimal extractAmount(String lfpJson) throws JsonProcessingException {
        if (lfpJson == null || lfpJson.isEmpty()) {
            return null;
        }
        InterestCalculationResponseDTO interestCalc = objectMapper.readValue(lfpJson, InterestCalculationResponseDTO.class);
        return interestCalc.getCalculatedInterest();
    }
}
