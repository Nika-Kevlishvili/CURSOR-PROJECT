package phoenix.core.customer.apis.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.http.codec.json.Jackson2JsonDecoder;
import org.springframework.http.codec.json.Jackson2JsonEncoder;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import phoenix.core.customer.apis.model.ApisCustomer;
import phoenix.core.customer.apis.model.CustomerCheckRequest;
import phoenix.core.customer.apis.model.CustomerCheckResponse;
import phoenix.core.customer.apis.service.ApisService;
import phoenix.core.exception.ClientException;
import phoenix.core.exception.ErrorCode;
import reactor.core.publisher.Mono;

import javax.validation.Validator;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import static org.springframework.util.MimeTypeUtils.TEXT_HTML;

@Service
@Validated
public class ApisServiceImpl implements ApisService {

    @Value("${apis.api.token}")
    String apisApiKey;

    @Value("${apis.base.url}")
    String apisBaseUrl;

    @Value("${apis.connection.timout}")
    Integer timeout;

    private final Validator validator;

    private static final String APIS_URL = "/eik";

    private final WebClient webClient;

    public ApisServiceImpl(WebClient.Builder webClient, Validator validator) {
        this.webClient = webClient
                .clientConnector(new ReactorClientHttpConnector())
                .exchangeStrategies(ExchangeStrategies.builder().codecs((configurer) -> {
                    configurer.customCodecs().register(new Jackson2JsonEncoder(new ObjectMapper(), TEXT_HTML));
                    configurer.customCodecs().register(new Jackson2JsonDecoder(new ObjectMapper(), TEXT_HTML));
                }).build())
                .build();
        this.validator = validator;
    }

    @Override
    public CustomerCheckResponse checkApisCustomersInfo(CustomerCheckRequest customerCheckRequest) {
        try {
            Mono<List<ApisCustomer>> response = webClient
                    .post()
                    .uri(apisBaseUrl + APIS_URL)
                    .header("Authorization", "Bearer " + apisApiKey)
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .body(BodyInserters.fromValue(customerCheckRequest.getIdentificationNumbers()))
                    .accept(MediaType.TEXT_HTML)
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<List<ApisCustomer>>() {
                    })
                    .timeout(Duration.ofMillis(timeout))
                    .onErrorResume(throwable -> {
                        throwable.printStackTrace();
                        return Mono.error(
                                new ClientException("Failed to get response from Apis", ErrorCode.APPLICATION_ERROR));
                    });
            List<ApisCustomer> customers = response.block();
            if (customers != null) {
                return new CustomerCheckResponse(customers);
            } else throw new ClientException("Response Object From Apis is null", ErrorCode.APPLICATION_ERROR);

        } catch (Exception e) {
            e.printStackTrace();
            throw new ClientException("Something went wrong while communicating Apis Api", ErrorCode.APPLICATION_ERROR);
        }
    }

    @Override
    public CustomerCheckResponse checkApisCustomerInfoWithSingleIdentificationNumber(String number) {
        List<String> list = new ArrayList<>();
        list.add(number);
        return checkApisCustomersInfo(new CustomerCheckRequest(list));
    }
}
