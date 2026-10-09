package bg.energo.migration.integration.phoenix.customer.service;

import bg.energo.migration.integration.phoenix.customer.model.request.CreateCustomerRequest;
import bg.energo.migration.integration.phoenix.customer.model.response.CustomerResponse;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Service
public class PhoenixCustomerService {
    @Qualifier("phoenixWebClient")
    private final WebClient webClient;

    public PhoenixCustomerService(WebClient webClient) {
        this.webClient = webClient;
    }

    public CustomerResponse phoenixCreateCustomer(CreateCustomerRequest request) {
        return webClient
                .post()
                .uri("/customer")
                .bodyValue(request)
                .retrieve()
                .onStatus(
                        HttpStatusCode::is4xxClientError,
                        response ->
                                response
                                        .bodyToMono(String.class)
                                        .flatMap(errorBody ->
                                                Mono.error(new RuntimeException("Client error: " + errorBody))
                                        )
                )
                .onStatus(
                        HttpStatusCode::is5xxServerError,
                        response ->
                                response
                                        .bodyToMono(String.class)
                                        .flatMap(errorBody ->
                                                Mono.error(new RuntimeException("Server error: " + errorBody))
                                        )
                )
                .bodyToMono(CustomerResponse.class)
                .block();
    }
}
