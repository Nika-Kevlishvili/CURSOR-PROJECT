package bg.energo.phoenix.service.riskList;

import bg.energo.phoenix.exception.ClientException;
import bg.energo.phoenix.exception.DomainEntityNotFoundException;
import bg.energo.phoenix.exception.ErrorCode;
import bg.energo.phoenix.exception.IllegalArgumentsProvidedException;
import bg.energo.phoenix.model.entity.customer.Customer;
import bg.energo.phoenix.model.entity.customer.CustomerDetails;
import bg.energo.phoenix.model.entity.nomenclature.customer.legalForm.LegalForm;
import bg.energo.phoenix.model.enums.customer.CustomerStatus;
import bg.energo.phoenix.model.enums.customer.CustomerType;
import bg.energo.phoenix.repository.customer.CustomerDetailsRepository;
import bg.energo.phoenix.repository.customer.CustomerRepository;
import bg.energo.phoenix.repository.nomenclature.customer.legalForm.LegalFormRepository;
import bg.energo.phoenix.service.riskList.model.RiskListBasicInfoResponse;
import bg.energo.phoenix.service.riskList.model.RiskListFullInfoResponse;
import bg.energo.phoenix.service.riskList.model.RiskListRequest;
import bg.energo.phoenix.util.epb.EPBValidationUtils;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.netty.resolver.DefaultAddressResolverGroup;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.http.codec.json.Jackson2JsonDecoder;
import org.springframework.http.codec.json.Jackson2JsonEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;

import java.net.URLEncoder;
import java.time.Duration;
import java.util.Collections;
import java.util.List;

import static org.springframework.util.MimeTypeUtils.TEXT_HTML;

@Slf4j
@Service
@Getter
public class RiskListServiceImpl implements RiskListService {
    private final String baseUrl;
    private final Integer timeout;
    private final WebClient webClient;
    private final CustomerRepository customerRepository;
    private final CustomerDetailsRepository customerDetailsRepository;
    private final LegalFormRepository legalFormRepository;

    @Autowired
    public RiskListServiceImpl(WebClient.Builder webClient,
                               CustomerRepository customerRepository,
                               CustomerDetailsRepository customerDetailsRepository,
                               LegalFormRepository legalFormRepository,
                               @Value("${risk.list.api.base.url}") String baseUrl,
                               @Value("${risk.list.api.connection.timeout}") Integer timeout) {
        this.webClient = webClient
                .clientConnector(new ReactorClientHttpConnector(HttpClient.create().resolver(DefaultAddressResolverGroup.INSTANCE)))
                .exchangeStrategies(
                        ExchangeStrategies.builder()
                                .codecs((configurer) -> {
                                    configurer.customCodecs().register(new Jackson2JsonEncoder(new ObjectMapper(), TEXT_HTML));
                                    configurer.customCodecs().register(new Jackson2JsonDecoder(new ObjectMapper(), TEXT_HTML));
                                }).build()).build();
        this.customerRepository = customerRepository;
        this.customerDetailsRepository = customerDetailsRepository;
        this.legalFormRepository = legalFormRepository;
        this.baseUrl = baseUrl;
        this.timeout = timeout;
    }


    /**
     * Checks the provided customer against the risk list for basic information.
     *
     * @param request the request containing the customer identifier, version and consumption
     * @return the basic response containing the decision and recommendations
     */
    @Override
    public RiskListBasicInfoResponse evaluateBasicCustomerRisk(RiskListRequest request) {
        log.debug("Evaluating basic customer risk for request: {}", request);

        List<String> violations = EPBValidationUtils.validateRequest(request);
        if (CollectionUtils.isNotEmpty(violations)) {
            log.error("Invalid request: {}", request);
            throw new IllegalArgumentsProvidedException(String.join("", violations));
        }

        String name = getFormattedName(request.getIdentifier(), request.getVersion());

        return getRiskListBasicInfoResponse(request, name);
    }

    public RiskListBasicInfoResponse evaluateBasicCustomerRisk(RiskListRequest riskListRequest, Customer customer, CustomerDetails customerDetails) {
        return getRiskListBasicInfoResponse(
                riskListRequest,
                getFormattedName(
                        customer.getIdentifier(),
                        customer,
                        customerDetails
                )
        );
    }

    private RiskListBasicInfoResponse getRiskListBasicInfoResponse(RiskListRequest request, String name) {
        // constraint defined by the Risk List API
        if (name.length() > 1024) {
            name = name.substring(0, 1024);
        }

        try {
            String finalName = name;
            Mono<RiskListBasicInfoResponse> responseMono = webClient
                    .get()
                    .uri(("%s/%s".formatted(baseUrl, URLEncoder.encode(request.getIdentifier()))), uriBuilder -> {
                        uriBuilder.queryParam("consumption", request.getConsumption());
                        uriBuilder.queryParam("name", URLEncoder.encode(finalName));
                        return uriBuilder.build();
                    })
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .accept(MediaType.APPLICATION_JSON)
                    .exchangeToMono(response -> {
                        if (response.statusCode().is2xxSuccessful()) {
                            return response.bodyToMono(RiskListBasicInfoResponse.class);
                        } else if (response.statusCode().equals(HttpStatus.BAD_REQUEST)) {
                            ObjectMapper objectMapper = new ObjectMapper();
                            return response.bodyToMono(String.class)
                                    .flatMap(body -> {
                                        try {
                                            RiskListBasicInfoResponse riskListBasicInfoResponse = objectMapper.readValue(body, RiskListBasicInfoResponse.class);
                                            return Mono.just(riskListBasicInfoResponse);
                                        } catch (JsonProcessingException e) {
                                            return Mono.error(new ClientException("Failed to get response from Risk List API;", ErrorCode.APPLICATION_ERROR));
                                        }
                                    });
                        } else {
                            return response.createException().flatMap(Mono::error);
                        }
                    })
                    .timeout(Duration.ofMillis(timeout));

            RiskListBasicInfoResponse response = responseMono.block();
            if (response == null) {
                log.error("Failed to get response from Risk List API");
                throw new ClientException("Failed to get response from Risk List API;", ErrorCode.APPLICATION_ERROR);
            }

            return response;
        } catch (Exception e) {
            log.error("Failed to get response from Risk List API");
            throw new ClientException("Failed to get response from Risk List API;", ErrorCode.APPLICATION_ERROR);
        }
    }


    /**
     * For legal entity the name should be a combination of name and legal form abbreviation, separated by a space.
     * For private customer the name should be a combination of name, middle name and surname, separated by spaces.
     *
     * @param identifier UIC in case of legal entity or PN in case of private customer
     * @return name of the customer
     */
    private String getFormattedName(String identifier, Long version) {
        log.debug("Getting name for customer with identifier {} and version {} for Risk List request", identifier, version);

        Customer customer = customerRepository
                .findByIdentifierAndStatus(identifier, CustomerStatus.ACTIVE)
                .orElseThrow(() -> new DomainEntityNotFoundException("identifier-Active customer with UIC/PN %s not found;".formatted(identifier)));

        CustomerDetails customerDetails;
        if (version == 0) {
            // fetch latest detail if version is 0 (in case of mass import)
            customerDetails = customerDetailsRepository
                    .findFirstByCustomerId(customer.getId(), Sort.by(Sort.Direction.DESC, "createDate"))
                    .orElseThrow(() -> new DomainEntityNotFoundException("identifier-Latest customer details not found for customer with UIC/PN %s;".formatted(identifier)));
        } else {
            // fetch detail by version
            customerDetails = customerDetailsRepository
                    .findByCustomerIdAndVersionId(customer.getId(), version)
                    .orElseThrow(() -> new DomainEntityNotFoundException("identifier-Customer details not found for customer with UIC/PN %s and version %s;".formatted(identifier, version)));
        }

        return getFormattedName(identifier, customer, customerDetails);
    }

    private String getFormattedName(String identifier, Customer customer, CustomerDetails customerDetails) {
        String name;
        if (customer.getCustomerType().equals(CustomerType.PRIVATE_CUSTOMER)) {
            name = "%s%s %s".formatted(
                    customerDetails.getName(),
                    StringUtils.isEmpty(customerDetails.getMiddleName()) ? "" : " " + customerDetails.getMiddleName(),
                    customerDetails.getLastName()
            );
        } else {
            LegalForm legalForm = legalFormRepository
                    .findById(customerDetails.getLegalFormId())
                    .orElseThrow(() -> new DomainEntityNotFoundException("identifier-Legal form abbreviation not found for customer with UIC/PN %s;".formatted(identifier)));

            name = "%s %s".formatted(customerDetails.getName(), legalForm.getName());
        }

        return name;
    }


    /**
     * Checks the provided customer against the risk list for full information.
     *
     * @param identifier the customer identifier (UIC or PN)
     * @return the full response containing the decision and recommendations
     */
    @Override
    public RiskListFullInfoResponse evaluateFullCustomerRisk(String identifier) {
        log.debug("Evaluating full customer risk for customer with identifier {}", identifier);

        if(!customerRepository.existsByIdentifierAndStatusIn(identifier, List.of(CustomerStatus.ACTIVE))) {
            log.error("identifier-Active customer with UIC/PN %s not found;".formatted(identifier));
            throw new DomainEntityNotFoundException("identifier-Active customer with UIC/PN %s not found;".formatted(identifier));
        }

        try {
            Mono<RiskListFullInfoResponse> responseMono = webClient
                    .get()
                    .uri(("%s/%s".formatted(baseUrl, identifier)))
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .accept(MediaType.APPLICATION_JSON)
                    .exchangeToMono(response -> {
                        if (response.statusCode().is2xxSuccessful()) {
                            return response.bodyToMono(RiskListFullInfoResponse.class);
                        } else if (response.statusCode().equals(HttpStatus.BAD_REQUEST)) {
                            ObjectMapper objectMapper = new ObjectMapper();
                            return response.bodyToMono(String.class)
                                    .flatMap(body -> {
                                        try {
                                            RiskListFullInfoResponse riskListBasicInfoResponse = objectMapper.readValue(body, RiskListFullInfoResponse.class);
                                            return Mono.just(riskListBasicInfoResponse);
                                        } catch (JsonProcessingException e) {
                                            return Mono.error(new ClientException("Failed to get response from Risk List API;", ErrorCode.APPLICATION_ERROR));
                                        }
                                    });
                        } else {
                            return response.createException().flatMap(Mono::error);
                        }
                    })
                    .timeout(Duration.ofMillis(timeout));

            RiskListFullInfoResponse response = responseMono.block();
            if (response == null) {
                log.error("Failed to get response from Risk List API");
                throw new ClientException("Failed to get response from Risk List API;", ErrorCode.APPLICATION_ERROR);
            }

            if (response.getSmallInfo() == null && response.getLargeInfo() == null) {
                log.debug("Risk List API returned empty response for customer with identifier {}", identifier);
                response.setBasicInfo(new RiskListBasicInfoResponse("missing", Collections.emptyList()));
            }

            return response;
        } catch (Exception e) {
            log.error("Failed to get response from Risk List API");
            throw new ClientException("Failed to get response from Risk List API;", ErrorCode.APPLICATION_ERROR);
        }
    }

}
