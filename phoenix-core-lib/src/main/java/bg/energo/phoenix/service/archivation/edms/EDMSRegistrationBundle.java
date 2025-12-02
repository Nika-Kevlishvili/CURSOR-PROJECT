package bg.energo.phoenix.service.archivation.edms;

import bg.energo.phoenix.exception.ClientException;
import bg.energo.phoenix.exception.ErrorCode;
import bg.energo.phoenix.model.request.communication.edms.LoginRequest;
import bg.energo.phoenix.model.response.communication.edms.LoginResponse;
import bg.energo.phoenix.model.response.communication.edms.LoginResponseContent;
import bg.energo.phoenix.service.archivation.edms.config.EDMSAPIConfigurationProperties;
import bg.energo.phoenix.service.archivation.edms.config.EDMSCredentialConfigurationProperties;
import bg.energo.phoenix.service.archivation.edms.config.EDMSProcessConfigurationProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.http.codec.json.Jackson2JsonDecoder;
import org.springframework.http.codec.json.Jackson2JsonEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;

import static org.springframework.util.MimeTypeUtils.TEXT_HTML;

@Slf4j
@Data
@Service
public class EDMSRegistrationBundle {
    private final EDMSCredentialConfigurationProperties credentialProperties;
    private final EDMSAPIConfigurationProperties apiConfigurationProperties;
    private final EDMSProcessConfigurationProperties processProperties;

    private String accessToken;

    private final WebClient webClient;

    public EDMSRegistrationBundle(EDMSCredentialConfigurationProperties credentialProperties,
                                  EDMSAPIConfigurationProperties apiConfigurationProperties,
                                  EDMSProcessConfigurationProperties processProperties,
                                  WebClient.Builder webClient) {
        this.credentialProperties = credentialProperties;
        this.apiConfigurationProperties = apiConfigurationProperties;
        this.processProperties = processProperties;
        this.webClient = buildWebClient(webClient);
    }

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void login() throws JsonProcessingException {
        log.debug("Attempting to login to EDMS system");

        if (!StringUtils.isEmpty(accessToken)) {
            this.accessToken = "";
        }

        LoginResponse responseBody = webClient
                .post()
                .uri(("%s%s".formatted(apiConfigurationProperties.getUrl(), apiConfigurationProperties.getLogin())))
                .body(BodyInserters
                        .fromValue(
                                LoginRequest
                                        .builder()
                                        .profileGuid(credentialProperties.getProfileGuid())
                                        .productName(credentialProperties.getProductName())
                                        .userName(credentialProperties.getUserName())
                                        .password(credentialProperties.getPassword())
                                        .build()
                        )
                )
                .headers(httpHeaders -> httpHeaders.add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE))
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .bodyToMono(LoginResponse.class)
                .timeout(Duration.ofSeconds(processProperties.getGlobalTimeout()))
                .onErrorResume(throwable -> {
                    throwable.printStackTrace();
                    log.debug("EDMS Registration Bundle: exception handled on attempt to login");
                    return Mono.error(
                            new ClientException("EDMS Registration Bundle: exception handled on attempt to login", ErrorCode.APPLICATION_ERROR)
                    );
                })
                .block();

        if (responseBody == null) {
            log.debug("EDMS Registration Bundle: response body is null during login");
            throw new ClientException("EDMS Registration Bundle: response body is null during login", ErrorCode.APPLICATION_ERROR);
        }

        LoginResponseContent responseContent = responseBody.getToken();
        if (responseContent == null) {
            log.debug("EDMS Registration Bundle: response body content is null during login");
            throw new ClientException("EDMS Registration Bundle: response body content is null during login", ErrorCode.APPLICATION_ERROR);
        }


        String token = responseContent.getAccessToken();
        if (StringUtils.isBlank(token)) {
            log.debug("EDMS Registration Bundle: access token is null during login");
            throw new ClientException("EDMS Registration Bundle: access token is null during login", ErrorCode.APPLICATION_ERROR);
        }

        this.accessToken = "%s %s".formatted(responseContent.getTokenType(), token);

        log.debug("Logged in EDMS system successfully");
    }

    public void logout() {
        log.debug("Attempting to log out from EDMS system");
        try {
            webClient
                    .post()
                    .uri(("%s%s".formatted(apiConfigurationProperties.getUrl(), apiConfigurationProperties.getLogin())))
                    .header(HttpHeaders.AUTHORIZATION, accessToken)
                    .retrieve()
                    .bodyToMono(Void.class)
                    .timeout(Duration.ofSeconds(processProperties.getGlobalTimeout()))
                    .onErrorComplete()
                    .block();
        } catch (Exception e) {
            log.debug("Exception handled on logout, ignoring");
        }

        this.accessToken = "";
    }

    private WebClient buildWebClient(WebClient.Builder webClient) {
        webClient
                .exchangeStrategies(
                        ExchangeStrategies
                                .builder()
                                .codecs((configurer) -> {
                                    configurer.customCodecs().register(new Jackson2JsonEncoder(new ObjectMapper(), TEXT_HTML));
                                    configurer.customCodecs().register(new Jackson2JsonDecoder(new ObjectMapper(), TEXT_HTML));
                                    configurer.defaultCodecs().maxInMemorySize(-1); // without size restriction
                                })
                                .build());

        try {
            SslContext sslContext = SslContextBuilder
                    .forClient()
                    .trustManager(InsecureTrustManagerFactory.INSTANCE)
                    .build();

            webClient.clientConnector(
                    new ReactorClientHttpConnector(
                            HttpClient.create()
                                    .secure(cert -> cert.sslContext(sslContext))
                    )
            );
        } catch (Exception e) {
            log.error("Unable to create insecure trust manger");

            webClient.clientConnector(
                    new ReactorClientHttpConnector(
                            HttpClient.create()
                    )
            );
        }

        return webClient.build();
    }
}
