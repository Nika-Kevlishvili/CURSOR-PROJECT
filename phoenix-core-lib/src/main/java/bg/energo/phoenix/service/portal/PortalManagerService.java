package bg.energo.phoenix.service.portal;

import bg.energo.common.backend.app.HeartbeatChecker;
import bg.energo.common.backend.app.RegisterMe;
import bg.energo.common.portal.api.heartbeat.HeartbeatResponse;
import bg.energo.common.portal.api.register.AppRegisterRequest;
import bg.energo.common.portal.api.register.AppRegisterResponse;
import bg.energo.common.portal.api.register.RemoteResource;
import bg.energo.common.portal.api.unregister.AppUnregisterRequest;
import bg.energo.common.portal.api.unregister.AppUnregisterResponse;
import bg.energo.phoenix.config.AppConfig;
import bg.energo.phoenix.exception.ClientException;
import bg.energo.phoenix.exception.ErrorCode;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;

import javax.net.ssl.SSLException;
import java.time.Duration;
@Slf4j
@Service
public class PortalManagerService implements RegisterMe, HeartbeatChecker {

    private final WebClient webClient;
    private final AppConfig appConfig;
    private final ApplicationStateService stateService;

    public PortalManagerService(WebClient.Builder webClient, AppConfig appConfig, ApplicationStateService stateService) {
        this.appConfig = appConfig;
        this.stateService = stateService;
        //TODO REMOVE THIS AFTER CERTIFICATE IS FIXED
        SslContext sslContext = null;
        try {
            sslContext = SslContextBuilder
                    .forClient()
                    .trustManager(InsecureTrustManagerFactory.INSTANCE)
                    .build();
        } catch (SSLException e) {
            e.printStackTrace();
        }
        SslContext finalSslContext = sslContext;
        HttpClient httpClient = HttpClient.create().secure(t -> t.sslContext(finalSslContext));

        this.webClient = webClient
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .build();

//        this.webClient = webClient
//                .clientConnector(new ReactorClientHttpConnector())
//                .build();
    }
    @Override
    public AppRegisterResponse registerMe(String endpoint, AppRegisterRequest appRegisterRequest) {

        log.debug("Portal connection - registerMe()");
        log.debug("registration param {}", appRegisterRequest.toString());
        Mono<AppRegisterResponse> response = webClient
                .post()
                .uri(appConfig.getPortalBaseUrl() + appConfig.getPortalRegisterUrl())
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .body(BodyInserters.fromValue(appRegisterRequest))
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<AppRegisterResponse>() {
                })
                .timeout(Duration.ofMillis(30000)) //TODO MOVE TO CONFIG
                .onErrorResume(throwable -> {
                    throwable.printStackTrace();
                    return Mono.error(
                            new ClientException("Failed to get response from portal registerMe", ErrorCode.APPLICATION_ERROR));
                });
        return response.block();
    }

    @Override
    public AppUnregisterResponse unregisterMe(String endpoint, AppUnregisterRequest appUnRegistrerRequest) {
        log.debug("Portal connection - unregisterMe()");
        AppRegisterResponse registrationResponse = stateService.getPortalRegistrationBundle().getRegistrationResponse();
        RemoteResource unregisterResource = registrationResponse.getResources().get(PortalResources.UNREGISTER_RESOURCE);
        Mono<AppUnregisterResponse> response = webClient
                .post()
                .uri(appConfig.getPortalBaseUrl() + unregisterResource.getUrl())
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .header(registrationResponse.getPortalSessionHeader(), registrationResponse.getPortalSessionKey())
                .body(BodyInserters.fromValue(appUnRegistrerRequest))
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<AppUnregisterResponse>() {
                })
                .timeout(Duration.ofMillis(10000))//TODO MOVE TO CONFIG
                .onErrorResume(throwable -> {
                    throwable.printStackTrace();
                    return Mono.error(
                            new ClientException("Failed to get response from portal unregisterMe", ErrorCode.APPLICATION_ERROR));
                });
        return response.block();
    }

    @Override
    public HeartbeatResponse checkHeartbeat(String heartbeatUrl, String sessionKey) {
        log.debug("Portal connection - checkHeartbeat()");
        AppRegisterResponse registrationResponse = stateService.getPortalRegistrationBundle().getRegistrationResponse();
        Mono<HeartbeatResponse> response;
        response = webClient
                .get()
                .uri(appConfig.getPortalBaseUrl() + heartbeatUrl)
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .header(registrationResponse.getPortalSessionHeader(), sessionKey)
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<HeartbeatResponse>() {
                })
                .timeout(Duration.ofMillis(appConfig.getHeartBeatTimeout()))
                .onErrorResume(throwable -> Mono.error(
                        new ClientException("Failed to get response from portal checkHeartbeat", ErrorCode.APPLICATION_ERROR)));
        return response.block();

    }
}
