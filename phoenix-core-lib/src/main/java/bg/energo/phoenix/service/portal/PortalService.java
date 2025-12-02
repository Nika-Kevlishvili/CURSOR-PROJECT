package bg.energo.phoenix.service.portal;

import bg.energo.common.crypto.PKIUtils;
import bg.energo.common.i18n.I18n;
import bg.energo.common.portal.api.PortalRegistrationBundle;
import bg.energo.common.portal.api.appTag.AppTag;
import bg.energo.common.portal.api.appTag.QueryTagsRequest;
import bg.energo.common.portal.api.appTag.QueryTagsResponse;
import bg.energo.common.portal.api.appUser.AppUserInfoDto;
import bg.energo.common.portal.api.appUser.AppUserStatus;
import bg.energo.common.portal.api.appUser.QueryAppUserRequest;
import bg.energo.common.portal.api.appUser.QueryAppUserResponse;
import bg.energo.common.portal.api.register.AppRegisterResponse;
import bg.energo.common.portal.api.register.RemoteResource;
import bg.energo.common.portal.api.unregister.AppUnregisterRequest;
import bg.energo.common.portal.api.user.PortalUserForApplicationDto;
import bg.energo.common.security.acl.app_permissions.AclContextGroups;
import bg.energo.common.security.acl.app_permissions.AvailableApplicationPermissions;
import bg.energo.common.security.acl.app_permissions.PermissionTranslations;
import bg.energo.common.security.acl.definitions.AclContextDefinition;
import bg.energo.common.security.acl.utils.PermissionUtils;
import bg.energo.phoenix.config.AppConfig;
import bg.energo.phoenix.exception.ClientException;
import bg.energo.phoenix.exception.ErrorCode;
import bg.energo.phoenix.exception.IllegalArgumentsProvidedException;
import bg.energo.phoenix.model.request.communication.portal.customerAccountManager.GetAccountManagerPortalRequest;
import bg.energo.phoenix.model.response.communication.portal.customerAccountManager.GetAccountManagerPortalResponse;
import bg.energo.phoenix.permissions.ContextLogicalGroupEnum;
import bg.energo.phoenix.permissions.PermissionContextEnum;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import jakarta.annotation.PreDestroy;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;

import javax.net.ssl.SSLException;
import java.security.PublicKey;
import java.time.Duration;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.UUID;


@Service
@Slf4j
public class PortalService {


    private final WebClient webClient;
    private final AppConfig appConfig;
    private final PortalRegistrationBundle appRegisterBundle;
    private final PortalManagerService portalManagerService;

    public PortalService(WebClient.Builder webClient, AppConfig appConfig, ApplicationStateService stateService, PortalManagerService portalManagerService) {
        this.appConfig = appConfig;
        this.portalManagerService = portalManagerService;
        this.appRegisterBundle = stateService.getPortalRegistrationBundle();
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
    }

    public AclContextGroups getLogicalGroups(@NonNull final I18n i18n) {
        final AclContextGroups res = new AclContextGroups();
        res.setLang(i18n.getCurrentLanguage());
        res.setLogicalGroups(new LinkedHashMap<>());
        for (ContextLogicalGroupEnum context : ContextLogicalGroupEnum.values()) {
            res.getLogicalGroups().put(context.getKey(), context.getPermissionIds());
        }
        return res;
    }

    public AvailableApplicationPermissions getAvailablePermissions(@NonNull final I18n i18n) {
        AvailableApplicationPermissions res = new AvailableApplicationPermissions();
        res.setApplicationId(appConfig.getApplicationId());
        res.setApplicationModuleId(appConfig.getModuleId());
        res.setLogicalGroups(getLogicalGroups(i18n));
        res.setAvailableContexts(new HashMap<>());
        final PermissionTranslations translations = PermissionUtils.createTranslationsContainer(i18n.getCurrentLanguage());

        for (PermissionContextEnum permissionContext : PermissionContextEnum.values()) {
            AclContextDefinition definition = new AclContextDefinition(
                    permissionContext.getId(),
                    permissionContext.getDescriptionKey(),
                    permissionContext.getAclContextDefinition()
            );
            res.getAvailableContexts().put(permissionContext.getId(),
                    definition);
            PermissionUtils.translateAclContext(i18n, translations, definition);

        }


        PermissionUtils.translateGroups(i18n, translations, res.getLogicalGroups());
        res.setTranslations(translations);


        return res;
    }


    public AppRegisterResponse doRegisterApp() {
        log.debug("Going to register application...");
        final AppRegisterResponse response = portalManagerService.registerMe(appRegisterBundle.getRegisterUrl(), appRegisterBundle.getRegisterRequest());
        appRegisterBundle.setRegistrationResponse(response);
        log.debug("App registered and got session {}", response.getPortalSessionKey());
        if (response.getPublicKeyBytes() != null) {
            log.debug("Receive public key with the registration");
            try {
                PublicKey publicKey = PKIUtils.loadPublicKey(response.getPublicKeyBytes());
                response.setPublicKey(publicKey);
            } catch (Exception e) {
                log.error("", e);
            }
        }
        appRegisterBundle.setApplicationRegistered(true);
        log.debug("AppRegister end()");
        return response;
    }

    @PreDestroy
    public void unRegisterMe() {
        log.debug("unRegistering");
        try {
            AppUnregisterRequest appUnRegistrerRequest = new AppUnregisterRequest();
            appUnRegistrerRequest.setModuleId(appRegisterBundle.getRegisterRequest().getModuleId());
            appUnRegistrerRequest.setApplicationId(appRegisterBundle.getRegisterRequest().getApplicationId());
            portalManagerService.unregisterMe(appRegisterBundle.getRegisterUrl(), appUnRegistrerRequest);
        } catch (Exception e) {
            //log.error("unregisterMe error", e);
        }
    }

    public PortalUserForApplicationDto getCurrentUser(
            @NonNull final String endpoint,
            @NonNull final String payload,
            @NonNull final String sessionId) {
        Mono<PortalUserForApplicationDto> response = webClient
                .post()
                .uri(appConfig.getPortalBaseUrl() + endpoint)
                .header(HttpHeaders.CONTENT_TYPE, MediaType.TEXT_PLAIN_VALUE)
                .header(appRegisterBundle.getRegistrationResponse().getPortalSessionHeader(), sessionId)
                .body(BodyInserters.fromValue(payload))
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .onStatus(HttpStatus.UNAUTHORIZED::equals, clientResponse ->
                        clientResponse.bodyToMono(String.class).map(x -> {
                            if (x.equals("Invalid session!")) {
                                return new ClientException("Invalid session!", ErrorCode.APPLICATION_ERROR);
                            }
                            return new ClientException("Invalid token", ErrorCode.ACCESS_DENIED);
                        })
                )
                .bodyToMono(new ParameterizedTypeReference<PortalUserForApplicationDto>() {
                })
                .timeout(Duration.ofMillis(appConfig.getRequestTimeout()));
        try {
            return response.block();
        } catch (ClientException e) {
            if (e.getErrorCode().equals(ErrorCode.APPLICATION_ERROR)) {
                doRegisterApp();
            }
            throw e;
        }
    }

    /**
     * <h2>Get Account Manager</h2>
     * Sending communication request to <b>Portal</b> to get response of all active Account Managers
     *
     * @param getAccountManagerPortalRequest - {@link GetAccountManagerPortalRequest}
     * @param portalModule                   - portalModule that must be held in {@link PortalRegistrationBundle#getRegisterRequest()}{@link PortalRegistrationBundle#getRegisterRequest().getModuleId()}
     * @param sessionId                      - sessionId that must be held in {@link PortalRegistrationBundle#getRegistrationResponse()}{@link PortalRegistrationBundle#getRegistrationResponse().getSessionId()}
     * @return {@link GetAccountManagerPortalResponse}
     */
    public QueryAppUserResponse getAccountManagers(GetAccountManagerPortalRequest getAccountManagerPortalRequest, String portalModule, String sessionId) {
        RemoteResource queryAppUsersResource = appRegisterBundle.getRegistrationResponse().getResources().get(PortalResources.QUERY_APP_USERS_RESOURCE);

        Mono<QueryAppUserResponse> response = webClient.post()
                .uri(appConfig.getPortalBaseUrl() + queryAppUsersResource.getUrl().replace("{moduleId}", portalModule))
                .headers(httpHeaders -> {
                    httpHeaders.add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
                    httpHeaders.add(appRegisterBundle.getRegistrationResponse().getPortalSessionHeader(), sessionId);
                })
                .body(BodyInserters.fromValue(getAccountManagerPortalRequest))
                .accept(MediaType.ALL)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<QueryAppUserResponse>() {
                })
                .timeout(Duration.ofMillis(appConfig.getRequestTimeout()))
                .onErrorResume(throwable -> {
                    throwable.printStackTrace();
                    return Mono.error(
                            new ClientException("Failed to get response from portal getAccountManagers", ErrorCode.APPLICATION_ERROR));
                });
        return response.block();
    }

    public List<String> getTaggedUsers(String tag) {
        RemoteResource queryAppUsersResource = appRegisterBundle.getRegistrationResponse().getResources().get(PortalResources.QUERY_APP_USERS_RESOURCE);
        QueryAppUserRequest queryAppUserRequest = new QueryAppUserRequest();
        queryAppUserRequest.setLang("en_US");
        queryAppUserRequest.setUserStatus(AppUserStatus.ONLY_ACTIVE);
        queryAppUserRequest.setTagId(UUID.fromString(tag));
        Mono<QueryAppUserResponse> response = webClient.post()
                .uri(appConfig.getPortalBaseUrl() + queryAppUsersResource.getUrl().replace("{moduleId}", appConfig.getModuleId().toString()))
                .headers(httpHeaders -> {
                    httpHeaders.add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
                    httpHeaders.add(appRegisterBundle.getRegistrationResponse().getPortalSessionHeader(), appRegisterBundle.getRegistrationResponse().getPortalSessionKey());
                })
                .body(BodyInserters.fromValue(queryAppUserRequest))
                .accept(MediaType.ALL)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<QueryAppUserResponse>() {
                })
                .timeout(Duration.ofMillis(appConfig.getRequestTimeout()))
                .onErrorResume(throwable -> {
                    throwable.printStackTrace();
                    return Mono.error(
                            new ClientException("Failed to get response from portal getAccountManagers", ErrorCode.APPLICATION_ERROR));
                });
        QueryAppUserResponse portalResponse = response.block();
        if (portalResponse == null) {
            throw new IllegalArgumentsProvidedException("Portal returned null as response!;");
        }
        List<AppUserInfoDto> portalCustomerAccountManagers = portalResponse.getUsers();
        return portalCustomerAccountManagers.stream().map(AppUserInfoDto::getUserName).toList();
    }

    public List<AppTag> getTags(String language){
        AppRegisterResponse registrationResponse = appRegisterBundle.getRegistrationResponse();
        RemoteResource remoteResource = registrationResponse.getResources().get(PortalResources.APP_TAGS_RESOURCE);
        String replace = remoteResource.getUrl().replace("{moduleId}", appConfig.getModuleId().toString());
        QueryTagsRequest queryTagsRequest = new QueryTagsRequest();
        queryTagsRequest.setLang(language);

        Mono<QueryTagsResponse> queryTagsResponseMono = webClient
                .post()
                .uri(appConfig.getPortalBaseUrl()+replace)
                .body(BodyInserters.fromValue(queryTagsRequest))
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .header(registrationResponse.getPortalSessionHeader(), registrationResponse.getPortalSessionKey())
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<QueryTagsResponse>() {
                })
                .timeout(Duration.ofMillis(appConfig.getHeartBeatTimeout()))
                .onErrorResume(throwable -> Mono.error(

                        new ClientException(throwable.getMessage(), ErrorCode.APPLICATION_ERROR)));
        QueryTagsResponse block = queryTagsResponseMono.block();
        if(block==null){
            throw new IllegalArgumentsProvidedException("Tags was not returned from portal!;");
        }
        return block.getTags();

    }
}
